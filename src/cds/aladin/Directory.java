package cds.aladin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import cds.aladin.prop.PropPanel;
import cds.allsky.Constante;
import cds.moc.HealpixMoc;
import cds.mocmulti.BinaryDump;
import cds.mocmulti.MocItem;
import cds.mocmulti.MultiMoc;
import cds.tools.MultiPartPostOutputStream;
import cds.tools.Util;


/**
 * Classe qui gère l'arbre du Directory des collections
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public class Directory extends JPanel implements Iterable<MocItem>{
   
   private Aladin aladin;                  // Référence
   private MultiMoc multiProp;             // Le multimoc de stockage des properties des collections
   private DirectoryFilter dirFilter=null; // Formulaire de filtrage de l'arbre des collections
   protected boolean mocServerLoading=false;  // true si on est en train de charger le directory initial
   protected boolean mocServerUpdating=false; // true si on est en train de mettre à jour le directory
   
   private DirectoryTree dirTree;          // Le JTree du directory
   private ArrayList<TreeObjDir> dirList;  // La liste des collections connues
   
   // Composantes de l'interface
   private QuickFilterField quickFilter; // Champ de filtrage rapide
   private JButton filter;         // Bouton d'ouverture du formulaire de filtrage
   protected Inside inside;          // L'icone d'activation du mode "élagage"
   private Collapse collapse;      // L'icone pour développer/réduire l'arbre
   private Timer timer = null;     // Timer pour le réaffichage lors du chargement
   
   // Paramètres d'appel initial du MocServer (construction de l'arbre)
//   private static String  MOCSERVER_INIT = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")+"&hips_service_url=*&get=record"; //&fmt=glu";
   private static String  MOCSERVER_INIT = "*&fields=!hipsgen*&get=record&fmt=asciic";
   
   // Paramètres de maj par le MocServer (update de l'arbre)
   private String mocUrl = null; //préfixe de l'URL une fois connue par le GLU
   private static String MOCSERVER_UPDATE = "fmt=asciic";
//   private static String MOCSERVER_UPDATE = "client_application=AladinDesktop"+(Aladin.BETA?"*":"")+"&hips_service_url=*&";

   private JScrollPane scrollTree = null;
   
   public Directory(Aladin aladin, Color cbg) {
      this.aladin = aladin;
      setBackground(cbg);
      
      // POUR LES TESTS => Surcharge de l'URL du MocServer
//      aladin.glu.aladinDic.put("MocServer","http://localhost:8080/MocServer/query?$1");
      
      multiProp = new MultiMoc();
      
      // L'arbre avec sa scrollbar
      dirTree = new DirectoryTree(aladin, cbg);
      scrollTree = new JScrollPane(dirTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setBorder( BorderFactory.createEmptyBorder(10,0,0,0));
      scrollTree.setBackground(getBackground());
      
      // Le bandeau en haut
      JLabel titre = new JLabel("Directory");
      titre.setFont(Aladin.BOLD);
      titre.setForeground(Aladin.DARKBLUE);
      
      JPanel panelFilter = new JPanel( new BorderLayout(5,5) );
      panelFilter.setBackground( getBackground() );
      quickFilter = new QuickFilterField(6);
      quickFilter.setToolTipText(Util.fold("Quick filter by free keywords (comma separated) "
            + "or by any advanced filter expression (ex: nb_rows<1000)",40,true));
      quickFilter.addKeyListener(new KeyListener() {
         public void keyTyped(KeyEvent e) { }
         public void keyPressed(KeyEvent e) { }
         public void keyReleased(KeyEvent e) {
            if( e.getKeyCode()==KeyEvent.VK_ENTER ) quickFiltre();
         }
      });
      panelFilter.add(quickFilter, BorderLayout.CENTER );
      
      filter = new JButton("...");
      filter.setMargin(new Insets(2,2,3,2));
      filter.setToolTipText("Advanced directory filter....");
      filter.setEnabled(false);
      filter.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { filtre(); }
      });
      panelFilter.add(filter, BorderLayout.EAST );
      
      JPanel header = new JPanel( new BorderLayout(5,5) );
      header.setBackground( getBackground() );
      header.setBorder( BorderFactory.createEmptyBorder(0, 0, 0, 10));
      header.add( titre, BorderLayout.WEST );
      header.add( panelFilter, BorderLayout.CENTER );
      
      // Les boutons de controle
      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,1,1));
      p1.setBackground(getBackground());
      collapse = new Collapse(aladin);
      p1.add(collapse);
      inside = new Inside(aladin);
      p1.add(inside);
      
      JPanel p2 = new JPanel( new FlowLayout());
      p2.setBackground(getBackground());
      
      JPanel control = new JPanel(new BorderLayout());
      control.setBackground(getBackground());
      control.setBorder( BorderFactory.createEmptyBorder(0,5,0,0));
      control.add(p1,BorderLayout.WEST);
      control.add(p2,BorderLayout.CENTER);
      
      setLayout(new BorderLayout() );
      add(header,BorderLayout.NORTH);
      add(scrollTree,BorderLayout.CENTER);
      add(control,BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(5,3,10,0));

      // Actions sur le clic d'un noeud de l'arbre
      dirTree.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            TreePath tp = dirTree.getPathForLocation(e.getX(), e.getY());
            if( tp==null ) hideInfo();
            else {
               ArrayList<TreeObjDir> treeObjs = getSelectedTreeObjDir();

               // Double-clic, on effectue l'action par défaut du noeud
               if (e.getClickCount() == 2) loadMulti( treeObjs );

               // Simple clic => on montre les informations associées au noeud
               else showInfo(treeObjs,e);
            }
            
            collapse.repaint();
         }
      });
      
//       Chargement de l'arbre initial
//      (new Thread(){
//         public void run() { initTree(); }
//      }).start();
      
      initTree();
   }
   
   /** Classe pour un JTextField avec reset en bout de champ (petite croix rouge) */
   class QuickFilterField extends JTextField implements MouseListener /*,MouseMotionListener*/ {
      private Rectangle cross=null;

      QuickFilterField(int nChar) { super(nChar); addMouseListener(this); }
      
      boolean in(int x,int y) {
         if( cross==null || getText().length()==0) return false;
         return x>=cross.x;
      }

      public void paintComponent(Graphics g) {
         try {
            super.paintComponent(g);
            int x = getWidth()-X-8;
            int y = getHeight()/2-X/2;
            if( mocServerLoading || mocServerUpdating ) Slide.drawBall(g, x, y, blinkState ? Color.white : Color.green );
            else drawCross(g,x,y);
         } catch( Exception e ) { }
      }

      static private final int X = 6;
      private void drawCross(Graphics g, int x, int y) {
         cross = new Rectangle(x,y,X,X);
         if( dirFilter==null || !dirFilter.hasFilter() ) return;
         g.setColor(Color.white);
         g.fillOval(x-3, y-3, X+7, X+7);
//         g.setColor( dirFilter.hasFilter() ? Color.red.darker() : Color.gray );
         g.setColor( Color.red.darker() );
         g.drawLine(x,y,x+X,y+X);
         g.drawLine(x+1,y,x+X+1,y+X);
         g.drawLine(x+2,y,x+X+2,y+X);
         g.drawLine(x+X,y,x,y+X);
         g.drawLine(x+X+1,y,x+1,y+X);
         g.drawLine(x+X+2,y,x+2,y+X);
      }

      public void mouseClicked(MouseEvent e) { }
      public void mousePressed(MouseEvent e) { }
      public void mouseEntered(MouseEvent e) { }
      public void mouseExited(MouseEvent e) { }

      public void mouseReleased(MouseEvent e) {
         if( in(e.getX(),e.getY())  ) dirFilter.reset();
      }
      
//      public void mouseDragged(MouseEvent e) { }
//      public void mouseMoved(MouseEvent e) {
//         Cursor nc,c =getCursor();
//         if( in(e.getX(),e.getY()) )  nc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
//         else nc = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
//         if( nc.equals(c) ) return;
//         setCursor(nc);
//      }


    }

   
   /** Récupération de la liste des TreeObj sélectionnées */
   private ArrayList<TreeObjDir> getSelectedTreeObjDir() {
      TreePath [] tps = dirTree.getSelectionPaths();
      ArrayList<TreeObjDir> treeObjs = new ArrayList<TreeObjDir>();
      if( tps!=null ) {
         for( int i=0; i<tps.length; i++ ) {
            Object obj = ((DefaultMutableTreeNode)tps[i].getLastPathComponent()).getUserObject();
            if( obj instanceof TreeObjDir ) treeObjs.add( (TreeObjDir)obj );
         }
      }
      return treeObjs;
   }
   
   // La frame d'affichage des informations du HiPS cliqué à la souris
   private FrameInfo frameInfo = null;
   
   /** Cache la fenêtre des infos du HiPS */
   private void hideInfo() { showInfo(null,null); }
   
   /** Chargements simultanés de toutes les collections sélectionnées */
   private void loadMulti(ArrayList<TreeObjDir> treeObjs) {
      if( treeObjs.size()==0 ) return;
      hideInfo();
      for( TreeObjDir to : treeObjs ) to.load();
   }

   
   /** Affiche la fenetre des infos des Collections passées en paramètre
    * @param treeObjs les noeuds correspondant aux collections sélectionnées, ou null si effacement de la fenêtre
    * @param e évènement souris pour récupérer la position absolue où il faut afficher la fenêtre d'info
    */
   private void showInfo(ArrayList<TreeObjDir> treeObjs, MouseEvent e) {
      if( treeObjs==null || treeObjs.size()==0 ) {
         if( frameInfo==null ) return;
         frameInfo.setVisible(false);
         return;
      }
      if( frameInfo==null ) frameInfo = new FrameInfo();
      Point p = e.getLocationOnScreen();
      frameInfo.setCollections( treeObjs );
      
      int w=350;
      int h=120;
      int x=p.x+50;
      int y=p.y-30;
      
      if( y<0 ) y=0;
//      if( y+h > aladin.SCREENSIZE.height ) y = aladin.SCREENSIZE.height-h;
//      if( x+w > aladin.SCREENSIZE.width )  x = aladin.SCREENSIZE.width -w;
      frameInfo.setBounds(x,y,Math.max(w,frameInfo.getWidth()),Math.max(h, frameInfo.getHeight()));
//    frameInfo.setLocation(x, y);
      frameInfo.setVisible(true);
   }
   
   /** Création/ouverture/fermeture du formulaire de filtrage de l'arbre des Collections */
   private void filtre() {
      if( dirFilter==null ) dirFilter = new DirectoryFilter(aladin);
      if( dirFilter.isVisible() ) dirFilter.setVisible(false);
      else dirFilter.showFilter();
//      setFilterButtonColor();
   }
   
   /** Filtrage basique de l'arbre des Collections */
   private void quickFiltre() {
      if( dirFilter==null ) dirFilter = new DirectoryFilter(aladin);
      dirFilter.setFreeText( quickFilter.getText() );
      dirFilter.submit();
      setFilterButtonColor();
   }
   
   /** Retourne true si la chaine est une clé de propriété d'au moins un enregistrement */
   protected boolean isFieldName(String s) { return multiProp.isFieldName(s); }
   
   /** Initialise le texte dans le champ du filtre rapide */
   protected void quickFilterSetText(String t) {
      quickFilter.setText(t);
      setFilterButtonColor();
   }
   
   // Affichage en rouge le bouton du filtre s'il y en a un actif
   private void setFilterButtonColor() {
      filter.setForeground( dirFilter.hasFilter() ? Color.red : Color.black ) ;
   }
   
   /**
    * Retourne le node correspondant à une identiciation
    * @param A l'identificateur de la collection à chercher
    * @param flagSubstring true si on prend en compte le cas d'une sous-chaine
    * @param mode 0 - match exact
    *             1 - substring sur label
    *             2 - match exact puis substring sur l'IVOID (ex: Simbad ok pour CDS/Simbad)
    *                 puis du menu  (ex DssColored ok pour Optical/DSS/DssColored)
    * @return le Hips node trouvé, null sinon
    * 
    * NECESSAIRE POUR get Hips(XXX) MAIS IL FAUDRA CHANGER CELA
    */
   protected TreeObjDir getTreeObjDir(String A) { return getTreeObjDir(A,0); }
   protected TreeObjDir getTreeObjDir(String A,int mode) {
      for( TreeObjDir to : dirList ) {
         if( !to.isHiPS() ) continue;
         if( A.equals(to.id) || A.equals(to.label) || A.equals(to.internalId) ) return to;
         if( mode==1 && Util.indexOfIgnoreCase(to.label,A)>=0 ) return to;
         if( mode==2 ) {
            if( to.internalId!=null && to.internalId.endsWith(A) ) return to;

            int offset = to.label.lastIndexOf('/');
            if( A.equals(to.label.substring(offset+1)) ) return to;
         }
      }

      if( mode==2 ) {
         for( TreeObjDir to : dirList ) {
            if( !to.isHiPS() ) continue;
            int offset = to.label.lastIndexOf('/');
            if( Util.indexOfIgnoreCase(to.label.substring(offset+1),A)>=0 ) return to;
         }
      }
      return null;
   }
   
   /** Création du plan via script - méthode identique à celle de la série des classes ServerXXX
    * (de fait, reprise de ServerHips de la version Aladin v9) */
   protected int createPlane(String target,String radius,String criteria, String label, String origin) {
      String survey;
      int defaultMode=PlanBG.UNKNOWN;

      if( criteria==null || criteria.trim().length()==0 ) survey="CDS/P/DSS2/color";
      else {
         Tok tok = new Tok(criteria,", ");
         survey = tok.nextToken();

         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equalsIgnoreCase("Fits") ) defaultMode=PlanBG.FITS;
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") || s.equalsIgnoreCase("png") ) defaultMode=PlanBG.JPEG;
         }
      }
      
      TreeObjDir to = getTreeObjDir(survey,2);
      if( to==null ) {
         Aladin.warning(this,"Progressive survey (HiPS) unknown ["+survey+"]",1);
         return -1;
      }

      try {
         if( defaultMode!=PlanBG.UNKNOWN ) to.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.hips(to,label,target,radius);
   }
   

   @Override
   public Iterator<MocItem> iterator() {
      return multiProp.iterator();
   }
   
   // False lorsque la première initialisation de l'arbre est faite
   private boolean init=true;
   
   /** Génération initiale de l'arbre - et maj des menus Aladin correspondants */
   protected void initTree() {
      try {
         initMultiProp();
         buildTree();
         dirTree.defaultExpand();
      } finally { postTreeProcess(); init=false;}
   }
   
   /** (Re)construction de l'arbre en fonction de l'état précédent de l'arbre
    * et de la valeur des différents flags associés aux noeuds
    * @param refCount  true si on doit faire les décomptages des noeuds en tant que référence sinon état courant
    * @return true s'il y a eu au-moins une modif
    */
   private void buildTree() {
      DirectoryModel model = (DirectoryModel) dirTree.getModel();
      for( TreeObjDir to : dirList ) model.createTreeBranch( to ); 
      initCounter( model );
   }
   
   // Initialisation du compteur de référence en fonction d'un TreeModel
   private void initCounter( DirectoryModel model ) {
      // Initialisation du compteur de référence
      HashMap<String,Integer> hs = new HashMap<String,Integer>();
      model.countDescendance( hs );
      counter = hs;
   }
   
   // Compteurs des noeuds
   private HashMap<String,Integer> counter = null;
   
   /** Reconstruction de l'arbre en utilisant à chaque fois un nouveau model
    * pour éviter les conflits d'accès et éviter la lenteur des évènements
    * de maj des état de l'arbre (après de très nombreux tests, c'est de loin
    * la meilleure méthode que j'ai trouvée même si elle peut paraitre étonnante
    * à première vue.)
    * 
    * @param tmpDirList
    */
   private void rebuildTree(ArrayList<TreeObjDir> tmpDirList, boolean defaultExpand, boolean initCounter ) {
      boolean insideActivated = inside.isActivated();
      
      // Mémorisation temporaire des états expanded/collapsed
      HashSet<String> wasExpanded = new HashSet<String>();
      backupState(new TreePath(dirTree.root), wasExpanded, dirTree);

      // Génération d'un nouveau model prenant en compte les filtres
      DirectoryModel model = new DirectoryModel(aladin);
      for( TreeObjDir to : tmpDirList ) {
         boolean mustBeActivated = !to.isHidden() && (!insideActivated || insideActivated && to.getIsIn()!=0 );
         if( mustBeActivated ) model.createTreeBranch( to );
      }
      
      if( initCounter ) initCounter( model );
      else model.countDescendance();

//      // Comptage de la descendance de chaque branche
//      if( !defaultExpand ) model.countDescendance();
//      
//      // Dans le cas où l'on remet l'expand par défaut, on sait qu'on a ajouté
//      // des collections et qu'il faut donc refaire le comptage de référence
//      else initCounter( model );
      
      // Répercussion des états des feuilles sur les branches
      if( inside.isAvailable() && !insideActivated ) model.populateFlagIn();
      
      // Remplacement du model dans l'arbre affiché
      dirTree.setModel( model );
      
      // Ouverture minimal des noeuds
      if( defaultExpand ) dirTree.defaultExpand();
         
      // Restauration des états expanded/collapses + compteurs de référence
      restoreState( new TreePath(model.root), defaultExpand ? null : wasExpanded, counter, dirTree);
   }
   
   /** Retourne le path sous forme de chaine - sans le premier "/" et "" pour la racine
    * ex => Image/Optical/DSS
    */
   private String getPathString(TreePath p) {
      boolean first=true;
      StringBuilder path = null;
      for( Object n : p.getPath() ) {
         if( first ) { first=false; continue; }  // on ne prend pas la racine
         if( path==null ) path = new StringBuilder(n+"");
         else path.append("/"+n);
      }
      return path==null ? "" : path.toString();
   }
   
   /** Mémorise dans une HashSet les branches qui sont expanded */
   private void backupState(TreePath parent, HashSet<String> wasExpanded, DirectoryTree tree) {
      DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parent.getLastPathComponent();
      TreeObj to =  (TreeObj)lastNode.getUserObject();
      if( tree.isExpanded(parent) ) wasExpanded.add(to.path );
      
      if( lastNode.getChildCount() >= 0 ) {
         for( Enumeration e = lastNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            backupState( parent.pathByAddingChild(node) , wasExpanded, tree);
         }
      }
   }
   
   /** Restaure l'état des branches en fonction d'une hashSet des branches
    * qui étaient expanded, ainsi que d'une hashmap donnant les décomptes
    * de référence pour chaque branche. */
   private void restoreState(TreePath parent, HashSet<String> wasExpanded,  HashMap<String,Integer> counter, JTree tree) {
      String path = getPathString(parent);
      
      // Récupération de l'état expanded/collapsed
      if( wasExpanded!=null && wasExpanded.contains( path ) ) tree.expandPath( parent );
      
      // Récupération du compteur de référence
      DefaultMutableTreeNode lastNode = (DefaultMutableTreeNode) parent.getLastPathComponent();
      Integer nbRef = counter.get( path );
      if( nbRef!=null )  {
         TreeObj to =  (TreeObj)lastNode.getUserObject();
         to.nbRef = nbRef.intValue();
      }

      if( lastNode.getChildCount() >= 0 ) {
         for( Enumeration e = lastNode.children(); e.hasMoreElements(); ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            restoreState( parent.pathByAddingChild(node) , wasExpanded, counter, tree );
         }
      }
   }
   
   /** Réaffichage de l'arbre en fonction des flags courants */
   protected void resumeTree() { resumeTree(dirList,false,false); }
   
   /** Remplacement et réaffichage de l'arbre avec une nouvelle liste de noeuds */
   protected void replaceTree(ArrayList<TreeObjDir> tmpDirList) {
      resumeTree(tmpDirList,true,true);
      dirList = tmpDirList;
   }
   
   /** Réaffichage de l'arbre */
   private void resumeTree(ArrayList<TreeObjDir> tmpDirList, boolean defaultExpand, boolean initCounter) {
      try {
         long t0 = System.currentTimeMillis();
         rebuildTree(tmpDirList,defaultExpand,initCounter);
         validate();
         postTreeProcess();
         System.out.println("resumeTree done in "+(System.currentTimeMillis()-t0)+"ms");
      } finally {

         // Pour permettre le changement du curseur d'attente de la fenêtre de filtrage
         if( dirFilter!=null ) aladin.makeCursor(dirFilter, Aladin.DEFAULTCURSOR);
      }

   }
   
   /** Filtrage et réaffichage de l'arbre en fonction des contraintes indiquées dans params
    *  @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    */
   protected void resumeFilter(String expr) {
      try {
         checkFilter(expr);
         resumeTree();
      } catch( Exception e ) {
        if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }

   /** Réaffichage de l'arbre en fonction des Hips in/out de la vue courante */
   protected void resumeIn() {
      if( !checkIn() ) return;
      if( inside.isActivated() ) resumeTree();
      else {
         ((DirectoryModel)dirTree.getModel()).populateFlagIn();
         repaint();
      }
   }

   /** Positionnement des flags isHidden() de l'arbre en fonction des contraintes de filtrage
    * @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    */
   private void checkFilter(String expr) throws Exception {
      
      // Filtrage
      long t0 = System.currentTimeMillis();
      ArrayList<String> ids = multiProp.scan( (HealpixMoc)null, expr, false, -1);
      System.out.println("Filter: "+ids.size()+"/"+multiProp.size()+" in "+(System.currentTimeMillis()-t0)+"ms");
      
      // Positionnement des flags isHidden() en fonction du filtrage
      HashSet<String> set = new HashSet<String>( ids.size() );
      for( String s : ids ) set.add(s);      
      for( TreeObjDir to : dirList ) to.setHidden( !set.contains(to.internalId) );
   }
   
   // Dernier champs interrogé sur le MocServer
   private Coord oc=null;
   private double osize=-1;

   /** Interroge le MocServer pour connaître les Collections disponibles dans le champ.
    * Met à jour l'arbre en conséquence */
   private boolean checkIn() { return checkIn(false); }
   private boolean checkIn(boolean force) {
      if( !dialogOk() ) return false; 

      // Le champ est trop grand ou que la vue n'a pas de réf spatiale ?
      // => on suppose que tous les HiPS sont a priori visibles
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
         boolean modif=false;
         for( TreeObjDir to : dirList ) {
            if( !modif && to.getIsIn()!=-1 ) modif=true;
            to.setIn(-1);
         }
         return modif;
      }

      // Interrogation du MocServer distant...
      try {
         
         // Pour activer le voyant d'attente
         repaint();
         
         BufferedReader in=null;
         try {
            String params;

            // Pour éviter de faire 2x la même chose de suite
            Coord c = v.getCooCentre();
            double size = v.getTaille();
            if( !force && c.equals(oc) && size==osize ) return false;
            oc=c;
            osize=size;

            // Interrogation par cercle
            if( v.getTaille()>45 ) {
               params = MOCSERVER_UPDATE+"&RA="+c.al+"&DEC="+c.del+"&SR="+size*Math.sqrt(2);

               // Interrogation par rectangle
            } else {
               StringBuilder s1 = new StringBuilder("Polygon");
               for( Coord c1: v.getCooCorners())  s1.append(" "+c1.al+" "+c1.del);
               params = MOCSERVER_UPDATE+"&stc="+URLEncoder.encode(s1.toString());
            }

            URL u = aladin.glu.getURL("MocServer", params, true);

            Aladin.trace(4,"HipsMarket.hipsUpdate: Contacting MocServer : "+u);
            in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
            String s;

            // récupération de chaque ID concernée (1 par ligne)
            HashSet<String> set = new HashSet<String>();
            while( (s=in.readLine())!=null ) set.add( getId(s) );

            // Positionnement des flags correspondants
            for( TreeObjDir to : dirList ) {
               if( !to.hasMoc() ) to.setIn( -1 );
               else to.setIn( set.contains(to.internalId) ? 1 : 0 );
            }
         
         } catch( EOFException e ) {}
         finally{ if( in!=null ) in.close(); }
         
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

      return true;
   }

   /** Retourne true si l'arbre est développé selon le défaut prévu */
   protected boolean isDefaultExpand() { return dirTree.isDefaultExpand(); }
   
   /** Collapse l'arbre sauf le noeud courant */
   protected void collapseAllExceptCurrent() {
      
      // Si j'ai une ou plusieurs branches sélectionnées
      TreePath [] tps = dirTree.getSelectionPaths();
      if( tps!=null ) {

         int mode=0;  // -1 collapse, 0-pas encore initialisé,  1 expand
         
         for( TreePath tp: tps ) {
            
            // cas particulier de la racine => on ne referme pas tout
            if( tp.getPathCount()==1 ) {
               if( mode==1 || isDefaultExpand() ) { mode=1; dirTree.allExpand(); }
               else { mode=-1; dirTree.defaultExpand(); }

               // Cas de la sélection d'une branche
            } else {

               // S'il n'est pas totalement ouvert, je le fais
               if( mode==1 || dirTree.isCollapsed(tp) ) { mode=1; dirTree.allExpand(tp); }
               else  { mode=-1; dirTree.collapseRec(tp); }
            }
         }

         // Rien n'est sélectionné => comme si on avait sélectionné la racine
      } else {
         if( isDefaultExpand() ) dirTree.allExpand();
         else dirTree.defaultExpand();
      }
   }
   
   /** Retourne true s'il n'y a pas d'arbre HiPS */
   protected boolean isFree() {
      return dirTree==null || dirTree.root==null ;
   }

   /** Traitement à applique après la génération ou la régénération de l'arbre */
   private void postTreeProcess() {
      
      filter.setEnabled( dialogOk() );
      dirTree.minimalExpand();
      
      // Mise en route ou arrêt du thread de coloration de l'arbre en fonction des Collections
      // présentes ou non dans la vue courante
      if( !isFree() ) startInsideUpdater();
      else stopInsideUpdater();
   }
   
   private boolean interruptServerReading;
   
   /** (Re)génération du Multiprop en fonction d'un stream d'enregistrements properties */
   protected int loadMultiProp(InputStreamReader in) throws Exception {
      MyProperties prop;
      
      boolean mocServerReading=true;
      interruptServerReading=false;
      int n=0;
      int rm=0;
      String memo="";
      
      try {
         memo = quickFilter.getText();
         quickFilter.setEditable(false);
         quickFilter.setForeground( Color.gray);
         
         while( mocServerReading && !interruptServerReading ) {
            prop = new MyProperties();
            mocServerReading = prop.loadRecord(in);
            if( prop.size()==0 || MultiMoc.getID(prop)==null ) continue;
            
            try {
               if( prop.getProperty("MOCSERVER_REMOVE")!=null ) {
                  multiProp.remove( MultiMoc.getID(prop));
                  rm++;
               }
               else multiProp.add( prop );
               
               quickFilter.setText("  updating... ("+multiProp.size()+")");
            } catch( Exception e ) {
               if( Aladin.levelTrace>=3 ) e.printStackTrace();
            }
            
            n++;
            if( n%1000==0 && n>0) aladin.trace(4,"Directory.loadMultiProp(..) "+(n-rm)+" prop loaded "+(rm>0?" - "+rm+" removed":"")+"...");
         }
         if( interruptServerReading ) aladin.trace(3,"MocServer update interrupted !");
      } finally{ 
         try { in.close(); } catch( Exception e) {}
         quickFilter.setEditable(true);
         quickFilter.setForeground( Color.black);
         quickFilter.setText(memo);
      }
      
      return n;
   }
   
   /** Interruption du chargement des infos du MocServer */
   protected void interruptMocServerReading() { interruptServerReading=true; }
   
   /** Génération de la liste des collections en fonction du contenu du MultiProp
    * La liste est triée
    * Les URLs HiPS seront mémorisées dans le Glu afin de pouvoir gérer les sites miroirs
    */
   private ArrayList<TreeObjDir> populateMultiProp(boolean localFile) {
      ArrayList<TreeObjDir> listReg = new ArrayList<TreeObjDir>(20000);
      for( MocItem mi : this ) populateProp(listReg, mi.prop,localFile);
      Collections.sort(listReg, TreeObj.getComparator() );
      return listReg;
   }
   
   /** Ajout d'une collection correspondant à un enregistrement prop, ainsi que des
    * entrées GLU associées
    * @param prop       Enregistrement des propriétés
    * @param localFile  true si cet enregistrement a été chargé localement par l'utilisateur (ACTUELLEMENT NON UTILISE)
    */
   private void populateProp(ArrayList<TreeObjDir> listReg, MyProperties prop, boolean localFile) {
      
      // Détermination de l'identificateur
      String id = MultiMoc.getID(prop);
      if( id==null ) {
         System.err.println("Directory.populateProp error - getID returns null => ignored ["+prop.toString().replace("\n"," ")+"]");
         return;
      }
      
      // Ajustement local des propriétés
      propAdjust( id, prop );
      
      String HIPSU = Constante.KEY_HIPS_SERVICE_URL;
      
      // Dans le cas où il n'y a pas de mirroir, mémorisation de l'URL directement
      if( prop.getProperty(HIPSU+"_1")==null ) {
         String url =  prop.getProperty(HIPSU);
         if( url!=null ) aladin.glu.aladinDic.put(id,url);
         
      // Dans le cas où il y a des mirroirs => mémorisation des indirections
      } else {

         // Enregistrement de tous les mirroirs comme des indirections
         StringBuilder indirection = null;
         for( int i=0; true; i++ ) {
            String url = prop.getProperty(HIPSU+(i==0?"":"_"+i));
            if( url==null ) break;
            
            String subid = id+"_"+i;
            aladin.glu.aladinDic.put(subid,url);

            if( indirection==null ) indirection = new StringBuilder("%I "+subid);
            else indirection.append("\t"+subid);
         }
         
         // Mémorisation des indirections possibles sous la forme %I id0\tid1\t...
         aladin.glu.aladinDic.put(id,indirection.toString());
      }
      
      // Ajout dans la liste des noeuds d'arbre
      listReg.add( new TreeObjDir(aladin,id,localFile,prop) );
   }
   
   /** Ajustement des propriétés, notamment pour ajouter le bon client_category
    * s'il s'agit d'un catalogue */
   private void propAdjust(String id, MyProperties prop) {
      propAdjust1(id,prop);
      String category = prop.getProperty(Constante.KEY_CLIENT_CATEGORY);
      
      if( id.equals("CDS/Model.SED/sed") 
            || id.equals("CDS/METAobj")
            || id.equals("CDS/ReadMeObj") ) category=null;
      
      if( category==null ) {
         prop.setProperty(Constante.KEY_CLIENT_CATEGORY,"Miscellaneous/"+Util.getSubpath(id, 0,1));
      }
      
      // Tri de la catégorie générale
      String key = prop.get(Constante.KEY_CLIENT_SORT_KEY);
      if( key==null ) key="Z";
      String cat = prop.get(Constante.KEY_CLIENT_CATEGORY);
      int c = Util.indexInArrayOf( Util.getSubpath(cat, 0), CAT);
      if( c==-1 ) c=CAT.length;
      key = String.format("%02d",c)+"/"+key;
      prop.replaceValue(Constante.KEY_CLIENT_SORT_KEY, key);
      
      // Insertion de la date de publication de l'article de référence
      String bib = prop.get("bib_reference");
      if( bib!=null ) {
         try { 
            int year = Integer.parseInt( bib.substring(0,4) );
            prop.put("bib_year",""+year);
         } catch( Exception e) {}
      }
   }
   
   private void propAdjust1(String id, MyProperties prop) {

      String type = prop.getProperty(Constante.KEY_DATAPRODUCT_TYPE);
      if( type==null || type.indexOf("catalog")<0 || !id.startsWith("CDS/") ) return;
      
      String category=null;
      
      if( id.equals("CDS/Simbad") ) category = "Data base";
      else {
         
         // Détermination de la catégorie
         String code = getCatCode(id);
         if( code==null ) return;
         
         String sortPrefix = "";
         boolean flagJournal = code.equals("J");
         if( flagJournal ) {
            String journal = getJournalCode(id);
            category = "Catalog/CDS VizieR/Journal table/"+journal;
            sortPrefix = journal;
            
         } else {
            int c = Util.indexInArrayOf(code, CAT_CODE);
            if( c==-1 ) category = "Catalog/CDS VizieR/"+code;   // Catégorie inconnue
            
            else {
               category = "Catalog/CDS VizieR/"+CAT_LIB[c];
               sortPrefix=String.format("%02d",c);
               
           }
         }
         
         // Tri par popularité et catégorie
         String popularity = prop.get("vizier_popularity");
         if( popularity!=null ) {
            popularity = String.format("%08d", 10000000 -Long.parseLong(popularity));
         } else popularity=getCatSuffix(id);
         String sortKey = sortPrefix+"/"+popularity;
         prop.replaceValue(Constante.KEY_CLIENT_SORT_KEY,sortKey);
         
         // Détermination du suffixe (on ne va pas créer un folder pour un élément unique)
         String parent = getCatParent(id);
         boolean hasMultiple = hasMultiple(parent);
         if( !hasMultiple ) {
            parent = getCatParent( parent );
            
            if( !flagJournal ) {
               //Je vire le nom de la table a la fin du titre
               String titre = prop.get(Constante.KEY_OBS_TITLE);
               int i;
               if( titre!=null && (i=titre.lastIndexOf('('))>0 ) {
                  titre = titre.substring(0,i-1);
                  prop.replaceValue(Constante.KEY_OBS_TITLE, titre);
               }
            }

         } else {
            String titre = prop.get(Constante.KEY_OBS_TITLE);
            if( titre!=null ) {

               // Je remonte le nom du catalog sur la branche
               int i= titre.lastIndexOf('(');
               int j = titre.indexOf(')',i);
               if( i>0 && j>i ) {
                  int k = parent.lastIndexOf('/');
                  parent = ( k==-1 ? "" : parent.substring(0,k+1) )+ titre.substring(0,i-1);
                  
                  String desc = prop.get(Constante.KEY_OBS_DESCRIPTION);
                  if( desc!=null ) {
                     String newTitre = desc + titre.substring(i-1,j+1);
                     prop.replaceValue(Constante.KEY_OBS_TITLE, newTitre);
                  }
               }
            }

         }
         if( !flagJournal ) {
            String suffix = getCatSuffix( parent );
            suffix = suffix==null ? "" : "/"+suffix;
            category += suffix;
         }
      }
      
      prop.replaceValue(Constante.KEY_CLIENT_CATEGORY, category );
   }
   
   // Mémorise les paths de l'arbre qui ont de multiples feuilles
   private HashSet<String> multiple = null;
   
   /** Retourne true si le path contient plusieurs feuilles */
   private boolean hasMultiple(String path) {
      if( multiple==null ) {
         multiple = new HashSet<String>(multiProp.size());
         HashSet<String> un = new HashSet<String>(multiProp.size());
         for( MocItem mi : multiProp ) {
            String parent = getCatParent( mi.mocId );
            if( un.contains(parent) ) multiple.add(parent);
            else un.add(parent);
         }
      }
      return multiple.contains(path);
   }
   
   /** Retourne le code de la catégorie des catalogues, null sinon (ex: CDS/I/246/out => I) */
   private String getCatCode(String id) { return Util.getSubpath(id,1); }
   
   /** retourne l'abbréviation du journal (ex: CDS/J/A+A/171/261/table1 => A+A) */
   private String getJournalCode(String id) { return Util.getSubpath(id,2); }
   
   /** Retourne le suffixe de l'identificateur d'un catalogue => tous ce qui suit le code
    * de catégorie (ex: CDS/I/246/out => 246/out) */
   private String getCatSuffix(String id) { return Util.getSubpath(id,2,2); }
   
   /** Retourne le préfixe parent d'un identificateur de catalgoue => tout ce qui précède
    * le dernier mot
    * (ex: CDS/I/246/out => CDS/I/246) */
   private String getCatParent(String id) {
      int i=id.lastIndexOf('/');
      if( i>0 ) return id.substring(0,i);
      return null;
   }
   
   private final String [] CAT = {"Image","Data base","Catalog","Cube","Miscellaneous","Outreach" };
   
   private final String [] CAT_CODE = { "I","II","III","IV","V","VI","VII","VIII","IX","B" };
   private final String [] CAT_LIB = {
         "I-Astrometric Data",
         "II-Photometric Data",
         "III-Spectroscopic Data",
         "IV-Cross-Identifications",
         "V-Combined data",
         "VI-Miscellaneous",
         "VII-Non-stellar Objects",
         "VIII-Radio and Far-IR data",
         "IX-High-Energy data",
         "B-External databases, regularly updated"
   };
   
   static private final String MMOC = "Multiprop.bin";
   
   // Sauvegarde dans le cache du MultiMoc sous forme binaire
   private boolean cacheWrite() {
      try {
         String s = aladin.cache.getCacheDir()+Util.FS+MMOC;
         (new BinaryDump()).save(multiProp, s);
         aladin.trace(3,"Multiprop stored in cache ["+s+"]");
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   // Génération du MultiMoc depuis la sauvegarde binaire du cache
   private boolean cacheRead() {
      try {
         long t0 = System.currentTimeMillis();
         String s = aladin.cache.getCacheDir()+Util.FS+MMOC;
         multiProp = (new BinaryDump()).load(s);
         aladin.trace(3,"Multiprop loaded ("+multiProp.size()+" rec.) from cache ["+s+"] in "+(System.currentTimeMillis()-t0)+"ms...");
      } catch( Exception e ) {
         return false;
      }
      return true;
   }
   
   /** Retourne la date d'estampillage la plus tardive de l'ensemble des enregistrements
    * du multiProp. 0 si aucun */
   private long getMultiPropTimeStamp() {
      long max=0;
      for( MocItem mi : multiProp ) {
         long ts = mi.getPropTimeStamp();
         if( ts>max ) max=ts;
      }
      return max;
   }
   
   // Retourne la date du cache
   private long getCacheTimeStamp() {
      String s = aladin.cache.getCacheDir()+Util.FS+MMOC;
      return (new File(s)).lastModified();
   }
   
   /** Retourne true si le HipsStore est prêt */
   protected boolean dialogOk() {
      return !mocServerLoading && multiProp.size()>0;
   }
   
   /** Chargement des descriptions de l'arbre par le MocServer */
   private void initMultiProp() {

      // Tentative de rechargement depuis le cache
      if( cacheRead()  ) {
         startTimer();
         while( !init ) Util.pause(100);
         (new Thread("updateFromMocServer"){
            public void run() {
               if( updateFromMocServer()>0  ) {
                  cacheWrite();
                  final ArrayList<TreeObjDir> tmpListReg = populateMultiProp(false);
                  SwingUtilities.invokeLater(new Runnable() {
                     public void run() { replaceTree(tmpListReg); }
                  });
               }
            }
         }).start();


         // Le cache est vide => il faut charger depuis le MocServer
      } else {

         // L'initialisation se fait en deux temps pour pouvoir laisser
         // l'utilisateur travailler plus rapidement
         String s = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
               +"&hips_service_url=*&fields=!obs_description,!hipsgen*&fmt=gzip-ascii&get=record";
         loadFromMocServer(s);
         
         startTimer();
         while( !init ) Util.pause(100);
         (new Thread("updateFromMocServer"){
            public void run() { 
               if( loadFromMocServer(MOCSERVER_INIT)>0 ) {
                  cacheWrite();
                  final ArrayList<TreeObjDir> tmpListReg = populateMultiProp(false);
                  SwingUtilities.invokeLater(new Runnable() {
                     public void run() { replaceTree(tmpListReg); }
                  });
               }
            }
         }).start();
      }

      this.dirList = populateMultiProp(false);
   }

   /** Ajout de nouvelles collections */
   protected boolean addHipsProp(InputStreamReader in, boolean localFile) {
      try {
         loadMultiProp(in);
         dirList = populateMultiProp(localFile);
//         replaceTree(listReg);
         checkIn(true);
         resumeTree(dirList, false, true);
      } catch( Exception e ) {
         if( aladin.levelTrace>3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   /** Demande de maj des enr. via le MocServer en passant en POST la liste des IDs des collections
    * que l'on connait déjà + une date d'estampillage pour chacune d'elles. Si ça ne fonctionne
    * pas, on tentera une maj plus basique
    * @return le nombre de records chargés
    */
   private int updateFromMocServer() {
      long t0 = System.currentTimeMillis();
      URL url;
      try {
         url  = aladin.glu.getURL("MocServer");
      } catch(Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         return -1;
      }

      try {
         mocServerUpdating=true;
         MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
         String boundary = MultiPartPostOutputStream.createBoundary();
         URLConnection urlConn = MultiPartPostOutputStream.createConnection(url);
         urlConn.setRequestProperty("Accept", "*/*");
         urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
         // set some other request headers...
         urlConn.setRequestProperty("Connection", "Keep-Alive");
         urlConn.setRequestProperty("Cache-Control", "no-cache");
         MultiPartPostOutputStream out =  new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

         File tmpMoc = File.createTempFile("moc", ".txt");
         tmpMoc.deleteOnExit();

         BufferedWriter fo = new BufferedWriter( new FileWriter(tmpMoc));
         try {
            for( MocItem mi : this ) {
               String s = mi.mocId+"="+mi.getPropTimeStamp()+"\n";
               fo.write( s.toCharArray() );
            }
         } finally { try { fo.close(); } catch(Exception e) {} }

         out.writeField("fmt", "asciic");
         out.writeFile("maj", null, tmpMoc, true);
         out.close();
         aladin.trace(4,"ID list sent");

         int n=loadMultiProp( new InputStreamReader( urlConn.getInputStream() ) );
         Aladin.trace(3,"Multiprop updated in "+(System.currentTimeMillis()-t0)+"ms => "+n+" record"+(n>1?"s":""));
         return n;

      } catch(Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         System.err.println("An error occured while updating the MocServer service => try the basic mode...");
         return updateFromMocServerBasic();
      } finally { mocServerUpdating=false; }
   }
   
//   /** Gunzippe le flux si nécessaire */
//   private InputStream gunzipIfRequired(InputStream in) throws Exception {
//      PushbackInputStream pb = new PushbackInputStream( in, 2 );
//      byte[] signature = new byte[2];
//      pb.read( signature );
//      pb.unread( signature );
//      int head = ( signature[0] & 0xff ) | ( ( signature[1] << 8 ) & 0xff00 );
//      boolean isGziped = GZIPInputStream.GZIP_MAGIC == head;
//      if( !isGziped ) return pb;
//      return new GZIPInputStream(pb);
//   }   

   /** Demande de maj des enr. via le MocServer en fonction d'une seule date spécifique.
    * La date d'estampillage de référence sera la plus tardive de l'ensemble des enr. déjà copiés.
    * Et si aucun, ce sera la date du fichier de cache (avec un risque de décalage des horloges
    * entre le MocServer et la machine locale.
    * Cette méthode de maj ne permet pas la suppression des rec obsolètes, ni le complément de maj
    * si la précédente maj a été interrompue en cours.
    * @return  le nombre de records chargés
    */
   private int updateFromMocServerBasic() {
      long ts = getMultiPropTimeStamp();
      if( ts==0L ) ts = getCacheTimeStamp();
      return loadFromMocServer("TIMESTAMP=>"+ts+"&fmt=asciic&get=record");
   }
   
   private int loadFromMocServer(String params) {
      InputStreamReader in=null;
      boolean eof=false;
      int n=0;
      
      String text = params.indexOf("TIMESTAMP")>=0 ? "updat":"load";

      // Recherche sur le site principal, et sinon sur un site miroir
      try {
         mocServerLoading = true;

         long t0 = System.currentTimeMillis();
         Aladin.trace(3,text+"ing Multiprop definitions from MocServer...");

         String u = aladin.glu.getURL("MocServer", params, true).toString();
         try {
            in = new InputStreamReader( Util.openStream(u,false,3000) );
            if( in==null ) throw new Exception("cache openStream error");

            // Peut être un site esclave actif ?
         } catch( EOFException e1 ) {
            eof=true;
         } catch( Exception e) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            try {
               in = new InputStreamReader( Util.openStream(u,false,-1) );
            } catch( EOFException e1 ) { eof=true; }
         }
         
         if( !eof ) n=loadMultiProp(in);
         Aladin.trace(3,"Multiprop "+text+"ed in "+(System.currentTimeMillis()-t0)+"ms => "+n+" record"+(n>1?"s":""));
         
      } catch( Exception e1 ) {
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
         return -1;
         
      } finally {
         mocServerLoading=false;
         if( in!=null ) { try { in.close(); } catch( Exception e) {} }
      }
      return n;
   }
   
   private String getId(String ivoid) {
      if( ivoid.startsWith("ivo://") ) return ivoid.substring(6);
      return ivoid;
   }
   
   
   /** Retourne le nombre max (en K) du nombre de lignes des catalogues et tables */
   protected int getNbKRowMax() {
      long max = 0L;
      for( MocItem mi : this ) {
         try {
            long n = Long.parseLong( mi.prop.get("nb_rows") );
            if( n>max ) max=n;
         } catch( Exception e) {}
      }
      return (int)( max/1000L );
   }
   
   /** Retourne le nombre d'items matchant la contrainte */
   protected int getNumber(String contrainte) {
      try {
         return multiProp.scan( contrainte ).size();
      } catch( Exception e ) { } 
      return -1;
   }
   

   /** true si on peut raisonnablement faire un updating des HiPS visibles dans la vue */
   private boolean isReadyForUpdating() {
      return !mocServerLoading && isVisible() && getWidth()>0;
   }

   private int blinkDot=0;
   private boolean blinkState=true;
   
   /** Réaffichage du Panel tous les demi-secondes afin de faire clignoter le message d'attente */
   private void startTimer() {
      if( timer==null ) {
         timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) { blinkState=!blinkState; repaint(); }
         });
      }
      if( !timer.isRunning() ) timer.start();
   }
   
   private void stopTimer() {
      if( timer!=null && timer.isRunning() ) timer.stop();
   }
   
   
//   public void paintComponent(Graphics g) {
//      
//      super.paintComponent(g);
//      
//      g.setFont( Aladin.BOLD );
//      
//      // Petit message pour avertir l'utilisateur que l'on charge des définitions
//      if( mocServerLoading ) {
//         String dot="";
//         for( int i = 0; i<blinkDot; i++ ) dot+=".";
//         blinkDot++;
//         if( blinkDot>10 ) blinkDot=0;
//         g.setColor(Aladin.BKGD);
//         String s = "Updating directory ("+nbRecInProgress+")"+dot;
//         g.drawString(s, 16, 25);
//         Slide.drawBall(g, 2, 16, blinkState ? Color.white : Color.orange);
//         
//      } else {
//         
//         g.setColor( Aladin.DARKBLUE );
//         int n=multiProp.size();
//         g.drawString("Directory : "+n+" collection"+(n>1?"s":""),16,25);
//         
//         // On fait clignoter le voyant d'attente d'info in/out
//         if( flagCheckIn ) {
//            startTimer();
//            Slide.drawBall(g, 2, 16, blinkState ? Color.white : Color.green );
//            
//         } else {
//            stopTimer();
//            Slide.drawBall(g, 2, 16, Color.green );
//         }
//      }
//   }
   
   
   /************** Pour faire la maj en continue des HiPS visibles dans la vue *******************************/
   
   private Thread threadUpdater=null;
   private boolean encore=true;

   private void startInsideUpdater() {
      if( threadUpdater==null ) {
         threadUpdater = new Updater("RegUpdater");
         threadUpdater.start();
      } else encore=true;
   }

   private void stopInsideUpdater() { encore=false; }

   class Updater extends Thread {
      public Updater(String s) { super(s); }

      public void run() {
         encore=true;
         //         System.out.println("Registry Tree updater running");
         while( encore ) {
            try {
               //               System.out.println("Hips updater checking...");
               if( isReadyForUpdating() ) resumeIn();
               Thread.currentThread().sleep(1000);
            } catch( Exception e ) { }
         }
         //         System.out.println("Registry Tree updater stopped");
         threadUpdater=null;
      }
   }

   
   /********* Classe gérant une fenêtre d'information associée au HiPS cliqué dans l'arbre des HiPS ***************/
   
   private class FrameInfo extends JFrame {
      
      ArrayList<TreeObjDir> treeObjs=null;     // hips dont il faut afficher les informations
      JPanel panelInfo=null;                // le panel qui contient les infos (sera remplacé à chaque nouveau hips)
      JCheckBox hipsBx=null,mocBx=null,mociBx=null,progBx=null,
                dmBx=null, siaBx=null, csBx=null, msBx=null, allBx=null;
      
      FrameInfo() {
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         JPanel contentPane = (JPanel)getContentPane();
         contentPane.setLayout( new BorderLayout(5,5)) ;
         contentPane.setBackground( new Color(240,240,250));
         contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
         setUndecorated(true);
         setAlwaysOnTop(true);
         pack();
      }
      
//      boolean isSame(ArrayList<TreeObjDir> a1, ArrayList<TreeObjDir> a2) {
//         if( a1==null && a2==null ) return true;
//         if( a1==null || a2==null ) return false;
//         if( a1.size() != a2.size() ) return false;
//         for( TreeObjDir ti : a1) {
//            if( !a2.contains(ti) ) return false;
//         }
//         return true;
//      }
      
      
      /** Positionne les collections concernées, et regénère le panel en fonction */
      void setCollections(ArrayList<TreeObjDir> treeObjs) {
//         if( isSame(treeObjs,this.treeObjs) ) return;
         this.treeObjs = treeObjs;
         resumePanel();
         validate();
         SwingUtilities.invokeLater( new Runnable() {
            public void run() { pack(); repaint(); }
         });
      }
      
      /** Reconstruit le panel des informations en fonction des collections courantes */
      void resumePanel() {
         JPanel contentPane = (JPanel)getContentPane();
         if( panelInfo!=null ) contentPane.remove(panelInfo);
         
         panelInfo = new JPanel( new BorderLayout() );
         panelInfo.setBorder( BorderFactory.createEmptyBorder(5, 5, 2, 5));
         
         String s;
         long nbRows=-1;
         MyAnchor a;
         GridBagConstraints c = new GridBagConstraints();
         GridBagLayout g =  new GridBagLayout();
         c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
         c.insets = new Insets(2,2,0,5);
         JPanel p = new JPanel(g);
         
         TreeObjDir to=null;
         boolean hasView = !aladin.view.isFree();
         
         if( treeObjs.size()>1 )  {
            a = new MyAnchor(aladin,treeObjs.size()+" collections selected",50,null,null);
            a.setFont(a.getFont().deriveFont(Font.PLAIN));
            PropPanel.addCouple(p,null, a, g,c);
            StringBuilder list = null;
            String sList=null,more=null;
            for( TreeObjDir to1 : treeObjs ) {
               if( list==null ) list = new StringBuilder(to1.internalId);
               else list.append(", "+to1.internalId);
               if( sList==null && list.length()>80 ) sList = list+"...";
            }
            if( sList!=null ) more = list.toString();
            else sList=list.toString();
            
            JPanel mocAndMore = new JPanel( new FlowLayout(FlowLayout.CENTER,5,0));
            JCheckBox bx;
            mociBx = mocBx = csBx = null;

            csBx = bx = new JCheckBox("Multiple cone search");
            mocAndMore.add(bx);
            bx.setSelected(true);
            bx.setToolTipText("Cone search on the current view of the selected collections");
            bx.setEnabled( hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
            
            JLabel labelPlus = new JLabel(" + ");
            labelPlus.setForeground(Color.lightGray);
            mocAndMore.add(labelPlus);
            
            mocBx = bx = new JCheckBox("MOC union"); 
            mocAndMore.add(bx); 
            bx.setToolTipText("Load the union of MOCs of the selected collections");
          
            mociBx = bx = new JCheckBox("MOC intersection"); 
            mocAndMore.add(bx); 
            bx.setToolTipText("Load the intersection of MOCs of the selected collections");
          
            PropPanel.addCouple(p,"", mocAndMore, g,c);
            
            a = new MyAnchor(aladin,sList,100,more,null);
            a.setForeground(Aladin.GREEN);
            PropPanel.addCouple(p,null, a, g,c);
            
         } else {
         
            to = treeObjs.get(0);

            if( to.verboseDescr!=null || to.description!=null ) {
               s = to.verboseDescr==null ? "":to.verboseDescr;
               a = new MyAnchor(aladin,to.description,200,s,null);
               a.setFont(a.getFont().deriveFont(Font.PLAIN));
               PropPanel.addCouple(p,null, a, g,c);
            }
            String provenance = to.copyright==null ? to.copyrightUrl : to.copyright;
            if( provenance!=null ) {
               s = ".Provenance: "+provenance;
               a = new MyAnchor(aladin,null,50,s,null);
               a.setForeground(Color.gray);
               PropPanel.addCouple(p,null, a, g,c);
            }

            JPanel p1 = new JPanel(new BorderLayout(15,0));
            s  = to.getProperty(Constante.KEY_MOC_SKY_FRACTION);
            if( s!=null ) {
               try { s = Util.myRound( Double.parseDouble(s)*100); } catch( Exception e) {}
               s = ".Sky coverage: "+s+"%";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a,BorderLayout.WEST);
            }
            s  = to.getProperty(Constante.KEY_HIPS_PIXEL_SCALE);
            if( s!=null ) {
               try { s = Coord.getUnit( Double.parseDouble(s)); } catch( Exception e) {}
               s = "    .HiPS pixel scale: "+s;
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a,BorderLayout.CENTER);
            }
            s  = to.getProperty(Constante.KEY_NB_ROWS);
            if( s!=null ) {
               try { nbRows = Long.parseLong(s); } catch( Exception e) {}
               s = "    .nb row: "+s;
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a,BorderLayout.CENTER);
            }

            if( p1.getComponentCount()>0 ) PropPanel.addCouple(p,null, p1, g,c);

            JPanel mocAndMore = new JPanel( new FlowLayout(FlowLayout.CENTER,5,0));
            JCheckBox bx;
            hipsBx = mocBx = mociBx = progBx = dmBx = csBx = siaBx = allBx = null;
            if( to.getUrl()!=null ) {
               hipsBx = bx = new JCheckBox("HiPS");
               mocAndMore.add(bx);
               bx.setSelected(true);
               bx.setToolTipText("Hierarchical Progressive Survey access");
            }
             
            if( to.hasSIA() ) {
               siaBx = bx = new JCheckBox("SIA");
               mocAndMore.add(bx);
               bx.setEnabled(hasView);
               bx.setSelected(to.getUrl()==null );
               bx.setToolTipText("Simple Image Access => load the image list available in the current view");
           }
            
            if( to.isCDSCatalog() ) {
               boolean allCat = nbRows<2000;
               NoneSelectedButtonGroup bg = new NoneSelectedButtonGroup();
               if( hipsBx!=null ) bg.add(hipsBx);
               
               boolean hasMoc = aladin.calque.getNbPlanMoc()>0;
               
               if( nbRows!=-1 && nbRows<100000 ) {
                  allBx = bx = new JCheckBox("All sources");
                  mocAndMore.add(bx);
                  bx.setSelected(to.getUrl()==null && allCat );
                  bx.setToolTipText("Load all sources (small catalog/table <100000)");
                  bg.add(bx);
               } 

               csBx = bx = new JCheckBox("Cone search");
               mocAndMore.add(bx);
               bx.setSelected(to.getUrl()==null && !allCat);
               bx.setToolTipText("Cone search on the current view");
               bx.setEnabled( hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
               bg.add(bx);
               
               msBx = bx = new JCheckBox("MOC search");
               mocAndMore.add(bx);
               bx.setToolTipText("Load all sources inside the selected MOC in the stack");
               bx.setEnabled( hasMoc );
               bg.add(bx);
               
               // Positionnement de la sélection par défaut
               boolean onMoc = aladin.calque.getFirstSelectedPlan() instanceof PlanMoc;
               if( onMoc && msBx!=null ) msBx.setSelected(true);
               else if( allBx!=null && nbRows<10000 ) allBx.setSelected(true);
               else if( csBx.isEnabled() ) csBx.setSelected(true); 
               
            } else if( to.getCSUrl()!=null ) {
               csBx = bx = new JCheckBox("Cone search");
               mocAndMore.add(bx);
               bx.setSelected(true);
               bx.setToolTipText("Cone search on the current view");
               bx.setEnabled( hasView && Projection.isOk( aladin.view.getCurrentView().getProj()) );
            }
            
            JLabel labelPlus = new JLabel(" + ");
            labelPlus.setForeground(Color.lightGray);
            mocAndMore.add(labelPlus);
            
            if( to.hasMoc() ) {
               mocBx = bx = new JCheckBox("MOC"); 
               mocAndMore.add(bx); 
               bx.setToolTipText("Load the MultiOrder Coverage map (MOC) associated to the collection");
            }
            
            if( to.isCDSCatalog() ) {
               dmBx = bx = new JCheckBox("Density map");
               mocAndMore.add(bx);
               Util.toolTip(bx,"Progressive view (HiPS) of the density map associated to the catalog",true);
            } else {
               if( to.getProgenitorsUrl()!=null ) {
                  progBx = bx = new JCheckBox("Progenitors");
                  mocAndMore.add(bx);
                  Util.toolTip(bx,"Meta data and links to the original data images",true);
               }
            }
            PropPanel.addCouple(p,"", mocAndMore, g,c);
            
            // On supprime le label car il n'y a aucun produit annexe
            if( mocBx==null && dmBx==null && progBx==null ) labelPlus.setText(" ");

         }
         
         panelInfo.add(p,BorderLayout.CENTER);

         JPanel control = new JPanel( new FlowLayout(FlowLayout.CENTER,6,2) );
         control.setBackground( contentPane.getBackground() );
         
         
         
         JButton b = new JButton(treeObjs.size()>1?"Load all":"Load"); b.setMargin( new Insets(2,4,2,4));
         b.setFont(b.getFont().deriveFont(Font.BOLD));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               submit();
               hideInfo();
            }
         });
         
//         if( treeObjs.size()==1 ) {
//            b = new JButton("Bookmark"); b.setMargin( new Insets(2,4,2,4));
//            control.add(b);
//            b.addActionListener(new ActionListener() {
//               public void actionPerformed(ActionEvent e) { bookmark(); }
//            });
//         }

         
         b = new JButton("Close"); b.setMargin( new Insets(2,4,2,4));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { hideInfo(); }
         });
         
         JPanel bas = new JPanel( new BorderLayout(0,0));
         bas.add(control,BorderLayout.CENTER);
         
         if( to!=null && to.internalId!=null ) {
            MyAnchor x = new MyAnchor(aladin,to.internalId,100,to.prop.getRecord(null),null);
            x.setForeground(Aladin.GREEN);
            bas.add(x,BorderLayout.WEST);
         }
         
         contentPane.add(panelInfo,BorderLayout.CENTER);
         panelInfo.add(bas,BorderLayout.SOUTH);
         contentPane.validate();
      }
      
      private class NoneSelectedButtonGroup extends ButtonGroup {
         public void setSelected(ButtonModel model, boolean selected) {
           if (selected)  super.setSelected(model, selected);
           else clearSelection();
         }
       }
      
      void bookmark() {
         TreeObjDir to = treeObjs.get(0);
         aladin.info("Pas encore implanté\nIl faudrait ajouté automatiquement un Bookmark sur cette collection");
      }
      
      void submit() {
         if( treeObjs.size()==0 ) return;
         
         // Accès à une collection
         if( treeObjs.size()==1 ) {
            TreeObjDir to = treeObjs.get(0);
            if( allBx!=null  && allBx.isSelected() )   to.loadAll();
            if( siaBx!=null  && siaBx.isSelected() )   to.loadSIA();
            if( csBx!=null   && csBx.isSelected() )    to.loadCS();
            if( msBx!=null   && msBx.isSelected() )    to.queryByMoc();
            if( hipsBx!=null && hipsBx.isSelected() )  to.loadHips();
            if( mocBx!=null  && mocBx.isSelected() )   to.loadMoc();
            if( progBx!=null && progBx.isSelected() )  to.loadProgenitors();
            if( dmBx!=null   && dmBx.isSelected() )    to.loadDensityMap();
            
         // Accès à plusieurs collections simultanément
         } else {
            
            // CS
            if( csBx!=null   && csBx.isSelected() ) {
               for( TreeObjDir to : treeObjs ) {
                  if( to.hasSIA() ) to.loadSIA();
                  else to.loadCS();
               }
            }
            
            // Union des MOCs
            if( mocBx!=null  && mocBx.isSelected() ) multiMocLoad(treeObjs,true);
            if( mociBx!=null  && mociBx.isSelected() ) multiMocLoad(treeObjs,false);
         }
      }
      
      /** Chargement de l'union ou intersection des Mocs */
      void multiMocLoad(ArrayList<TreeObjDir> treeObjs, boolean union ) {
         
         // Liste des ID
         StringBuilder params = null;
         for( TreeObjDir to : treeObjs ) {
            if( params==null ) params = new StringBuilder(to.internalId);
            else params.append(","+to.internalId);
         }
         
         String label;
         if( union ) { params.append("&get=moc"); label="MOCs"; }
         else { params.append("&get=imoc"); label="iMOCs"; }
         
         String u = aladin.glu.getURL("MocServer", params.toString(),true).toString();
         aladin.execAsyncCommand(label+"=load "+u);
      }
   }
   
}

