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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import cds.aladin.prop.PropPanel;
import cds.allsky.Constante;
import cds.moc.HealpixMoc;
import cds.mocmulti.BinaryDump;
import cds.mocmulti.MocItem;
import cds.mocmulti.MultiMoc;
import cds.tools.Util;


/**
 * Classe qui gère l'arbre HiPS apparaissant à gauche de la vue
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public class HipsStore extends JPanel implements Iterable<MocItem>{
   
   private Aladin aladin;                  // Référence
   private MultiMoc multiProp;             // Le multimoc de stockage des properties HiPS
   private HipsFilter hipsFilter=null;     // Formulaire de filtrage de l'arbre HiPS
   private boolean mocServerLoading=false; // true = requête de génération de l'arbre en cours (appel à MocServer)
   
   private HipsTree hipsTree;                 // Le JPanel de l'arbre HiPS
   private ArrayList<TreeObjHips> listHips;   // Liste des noeuds potentiels de l'arbre
   
   // Composantes de l'interface
   private JButton filter;    // Bouton d'ouverture du formulaire de filtrage
   protected Prune prune;     // L'icone d'activation du mode "élagage"
   private Collapse collapse; // L'icone pour développer/réduire l'arbre
   private Timer timer = null;// Timer pour le réaffichage lors du chargement
   
   // Paramètres d'appel initial du MocServer (construction de l'arbre)
//   private static String  MOCSERVER_INIT = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")+"&hips_service_url=*&get=record"; //&fmt=glu";
   private static String  MOCSERVER_INIT = "*&fields=!hipsgen*&get=record&fmt=asciic";
   
   // Paramètres de maj par le MocServer (update de l'arbre)
   private static String MOCSERVER_UPDATE = "fmt=asciic";
//   private static String MOCSERVER_UPDATE = "client_application=AladinDesktop"+(Aladin.BETA?"*":"")+"&hips_service_url=*&";

   public HipsStore(Aladin aladin) {
      this.aladin = aladin;
      
      // POUR LES TESTS => Surcharge de l'URL du MocServer
//      aladin.glu.aladinDic.put("MocServer","http://localhost:8080/MocServer/query?$1");
      
      multiProp = new MultiMoc();
      
      // L'arbre avec sa scrollbar
      hipsTree = new HipsTree(aladin);
      setBackground(aladin.getBackground());
      hipsTree.setBackground(aladin.getBackground());
      
      JScrollPane scrollTree = new JScrollPane(hipsTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setBorder( BorderFactory.createEmptyBorder(0,0,0,0));
      
      // Les boutons de controle
      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,1,1));
      p1.setBackground(aladin.getBackground());
      collapse = new Collapse(aladin);
      p1.add(collapse);
      prune = new Prune(aladin);
      p1.add(prune);
      
      JPanel p2 = new JPanel( new FlowLayout());
      p2.setBackground(aladin.getBackground());
      filter = new JButton("filter...");
      filter.setEnabled(false);
      filter.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { filtre(); }
      });
      p2.add(filter);
      
      JPanel control = new JPanel(new BorderLayout());
      control.setBackground(aladin.getBackground());
      control.setBorder( BorderFactory.createEmptyBorder(0,5,0,0));
      control.add(p1,BorderLayout.WEST);
      control.add(p2,BorderLayout.CENTER);
      
      setLayout(new BorderLayout() );
      add(scrollTree,BorderLayout.CENTER);
      add(control,BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(30,0,5,0));

      // Actions sur le clic d'un noeud de l'arbre
      hipsTree.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            TreePath tp = hipsTree.getPathForLocation(e.getX(), e.getY());
            if( tp==null ) hideInfo();
            else {
               ArrayList<TreeObjHips> treeObjs = getSelectedTreeObjHips();

               // Double-clic, on effectue l'action par défaut du noeud
               if (e.getClickCount() == 2) loadMultiHips( treeObjs );

               // Simple clic => on montre les informations associées au noeud
               else showInfo(treeObjs,e);
            }
            
            collapse.repaint();
         }
      });
      
      // Chargement de l'arbre initial
//      (new Thread(){
//         public void run() { initTree(); }
//      }).start();
      
      initTree();
   }
   
   /** Récupération de la liste des TreeObj sélectionnées */
   private ArrayList<TreeObjHips> getSelectedTreeObjHips() {
      TreePath [] tps = hipsTree.getSelectionPaths();
      ArrayList<TreeObjHips> treeObjs = new ArrayList<TreeObjHips>();
      if( tps!=null ) {
         for( int i=0; i<tps.length; i++ ) {
            Object obj = ((DefaultMutableTreeNode)tps[i].getLastPathComponent()).getUserObject();
            if( obj instanceof TreeObjHips ) treeObjs.add( (TreeObjHips)obj );
         }
      }
      return treeObjs;
   }
   
   // La frame d'affichage des informations du HiPS cliqué à la souris
   private FrameInfo frameInfo = null;
   
   /** Cache la fenêtre des infos du HiPS */
   private void hideInfo() { showInfo(null,null); }
   
   private void loadMultiHips(ArrayList<TreeObjHips> treeObjs) {
      if( treeObjs.size()==0 ) return;
      hideInfo();
      treeObjs.get(0).load();
   }

   
   /** Affiche la fenetre des infos des HiPS passés en paramètre
    * @param node le noeud correspondant au HiPS sous la souris, ou null si effacement de la fenêtre
    * @param e évènement souris pour récupérer la position absolue où il faut afficher la fenêtre d'info
    */
   private void showInfo(ArrayList<TreeObjHips> treeObjs, MouseEvent e) {
      if( treeObjs==null || treeObjs.size()==0 ) {
         if( frameInfo==null ) return;
         frameInfo.setVisible(false);
         return;
      }
      if( frameInfo==null ) frameInfo = new FrameInfo();
      Point p = e.getLocationOnScreen();
      frameInfo.setHips( treeObjs );
      
      int w=350;
      int h=120;
      int x=p.x+50;
      int y=p.y-30;
      
      if( y<0 ) y=0;
      if( y+h > aladin.SCREENSIZE.height ) y = aladin.SCREENSIZE.height-h;
      if( x+w > aladin.SCREENSIZE.width )  x = aladin.SCREENSIZE.width -w;
      frameInfo.setBounds(x,y,w,h);
      frameInfo.setVisible(true);
   }
   
   /** Création/ouverture du formulaire de filtrage de l'arbre HiPS */
   private void filtre() {
      if( hipsFilter==null ) hipsFilter = new HipsFilter(aladin);
      hipsFilter.showFilter();
   }
   
   /**
    * Retourne le Hips node correspondant à une identiciation
    * @param A l'identificateur du HiPS à chercher
    * @param flagSubstring true si on prend en compte le cas d'une sous-chaine
    * @param mode 0 - match exact
    *             1 - substring sur label
    *             2 - match exact puis substring sur l'IVORN (ex: Simbad ok pour CDS/Simbad)
    *                 puis du menu  (ex DssColored ok pour Optical/DSS/DssColored)
    * @return le Hips node trouvé, null sinon
    */
   protected TreeObjHips getHips(String A) { return getHips(A,0); }
   protected TreeObjHips getHips(String A,int mode) {
      for( TreeObjHips hips : listHips ) {
         if( A.equals(hips.id) || A.equals(hips.label) || A.equals(hips.internalId) ) return hips;
         if( mode==1 && Util.indexOfIgnoreCase(hips.label,A)>=0 ) return hips;
         if( mode==2 ) {
            if( hips.internalId!=null && hips.internalId.endsWith(A) ) return hips;

            int offset = hips.label.lastIndexOf('/');
            if( A.equals(hips.label.substring(offset+1)) ) return hips;
         }
      }

      if( mode==2 ) {
         for( TreeObjHips gs : listHips ) {
            int offset = gs.label.lastIndexOf('/');
            if( Util.indexOfIgnoreCase(gs.label.substring(offset+1),A)>=0 ) return gs;
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
      
      TreeObjHips hips = getHips(survey,2);
      if( hips==null ) {
         Aladin.warning(this,"Progressive survey (HiPS) unknown ["+survey+"]",1);
         return -1;
      }

      try {
         if( defaultMode!=PlanBG.UNKNOWN ) hips.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.hips(hips,label,target,radius);
   }
   

   @Override
   public Iterator<MocItem> iterator() {
      return multiProp.iterator();
   }
   
   // False lorsque la première initialisation de l'arbre est faite
   private boolean init=true;
   
   /** Génération initiale de l'arbre HiPS - et maj des menus Aladin correspondants */
   protected void initTree() {
      try {
         initMultiProp();
         rebuildTree();
         hipsTree.defaultExpand();
      } finally { postTreeProcess(); init=false;}
   }
   
   boolean refCount = true;
   private void setRefCount(boolean flag) { refCount=flag; }

   /** (Re)construction de l'arbre des HiPS en fonction de l'état précédent de l'arbre
    * et de la valeur des différents flags associés aux noeuds
    * @param refCount  true si on doit faire les décomptages des noeuds en tant que référence sinon état courant
    * @return true s'il y a eu au-moins une modif
    */
   private boolean rebuildTree() {
      boolean pruneActivated = prune.isActivated();
      boolean modif = false;
      try {
         hipsTree.setLockExpand(true);
         for( TreeObjHips hips : listHips ) { 
            boolean mustBeActivated = !hips.isHidden() && (!pruneActivated || pruneActivated && hips.getIsIn()!=0 );
            modif |= setActivated( hips, hipsTree, mustBeActivated );
         }
         if( modif || refCount ) {
            hipsTree.countDescendance( refCount );
            refCount=false;
         }
         if( prune.isAvailable() && !pruneActivated ) hipsTree.populateFlagIn();
         
      } finally { hipsTree.setLockExpand(false); }
      return modif;
   }
   
   /** Activation/désactivation d'un noeud de l'arbre */
   protected boolean setActivated(TreeObjHips hips, HipsTree hipsTree, boolean activated) {
      if( hips.isActivated() == activated ) return false;
      hips.setActivated(activated);
      if( activated ) hipsTree.createTreeBranch( hips );
      else hipsTree.removeTreeBranch( hips );
      return true;
   }
   
   private boolean treeReady=true;
   private Object lock = new Object();
   protected void setTreeReady(boolean flag) { synchronized( lock ) { treeReady=flag; } }
   protected boolean isTreeReady()           { synchronized( lock ) { return treeReady; } }
   
   /** Demande de regénération de l'arbre => sera fait par l'EDT dans le paintComponent() */
   protected void askForResumeTree() {
      setTreeReady(false);
      repaint();
   }
      
   /** Réaffichage de l'arbre en fonction des flags courants */
   protected void resumeTree() {
      try {
         long t0 = System.currentTimeMillis();
         if( !rebuildTree() ) return;
         validate();
         postTreeProcess();
         System.out.println("resumeTree done in "+(System.currentTimeMillis()-t0)+"ms");
      } finally { setTreeReady( true ); }
      
      // Pour permettre le changement du curseur d'attente de la fenêtre de filtrage
      if( hipsFilter!=null ) aladin.makeCursor(hipsFilter, Aladin.DEFAULTCURSOR);
   }
   
   /** Filtrage et réaffichage de l'arbre en fonction des contraintes indiquées dans params
    *  @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    */
   protected void resumeFilter(String expr) {
      try {
         checkFilter(expr);
         askForResumeTree();
      } catch( Exception e ) {
        if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
   }

   /** Réaffichage de l'arbre en fonction des Hips in/out de la vue courante */
   protected void resumeIn() {
      if( !checkIn() ) return;
      askForResumeTree();
   }

   /** Positionnement des flags isHidden() de l'arbre en fonction des contraintes de filtrage
    * @param expr expression ensembliste de filtrage voir doc multimoc.scan(...)
    */
   private void checkFilter(String expr) throws Exception {
      
      // Filtrage
      long t0 = System.currentTimeMillis();
      ArrayList<String> mocIds = multiProp.scan( (HealpixMoc)null, expr, false, -1);
      System.out.println("Filter: "+mocIds.size()+"/"+multiProp.size()+" in "+(System.currentTimeMillis()-t0)+"ms");
      
      // Positionnement des flags isHidden() en fonction du filtrage
      HashSet<String> set = new HashSet<String>( mocIds.size() );
      for( String s : mocIds ) set.add(s);      
      for( TreeObjHips hips : listHips ) hips.setHidden( !set.contains(hips.internalId) );
   }
   
   // Dernier champs interrogé sur le MocServer
   private Coord oc=null;
   private double osize=-1;
   private boolean flagCheckIn=false;

   /** Interroge le MocServer pour connaître les HiPS disponibles dans le champ.
    * Met à jour l'arbre en conséquence */
   private boolean checkIn() {
      if( !dialogOk() ) return false; 
      

      // Le champ est trop grand ou que la vue n'a pas de réf spatiale ?
      // => on suppose que tous les HiPS sont a priori visibles
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
         boolean modif=false;
         for( TreeObjHips hips : listHips ) {
            if( !modif && hips.getIsIn()!=-1 ) modif=true;
            hips.setIn(-1);
         }
         return modif;
      }

      // Interrogation du MocServer distant...
      try {
         
         // Pour activer le voyant d'attente
         flagCheckIn=true; repaint();
         
         BufferedReader in=null;
         try {
            String params;

            // Pour éviter de faire 2x la même chose de suite
            Coord c = v.getCooCentre();
            double size = v.getTaille();
            if( c.equals(oc) && size==osize ) return false;
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
            for( TreeObjHips hips : listHips ) hips.setIn( set.contains(hips.internalId) ? 1 : 0 );
         
         } catch( EOFException e ) {}
         finally{ flagCheckIn=false; if( in!=null ) in.close(); }
         
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

      return true;
   }

   /** Retourne true si l'arbre est développé selon le défaut prévu */
   protected boolean isDefaultExpand() { return hipsTree.isDefaultExpand(); }
   
   /** Collapse l'arbre sauf le noeud courant */
   protected void collapseAllExceptCurrent() {
      if( isDefaultExpand() ) hipsTree.allExpand();
      else hipsTree.defaultExpand();
   }
   
   /** Retourne true s'il n'y a pas d'arbre HiPS */
   protected boolean isFree() {
      return hipsTree==null || hipsTree.root==null ;
   }

   /** Traitement à applique après la génération ou la régénération de l'arbre HiPS */
   private void postTreeProcess() {
      
      filter.setEnabled( dialogOk() );
      
      // Mise en route ou arrêt du thread de coloration de l'arbre en fonction des HiPS
      // présents ou non dans la vue courante
      if( !isFree() ) startHipsUpdater();
      else stopHipsUpdater();
   }
   
   private int nbRecInProgress=0;
   private boolean interruptServerReading;
   
   /** (Re)génération du Multiprop en fonction d'un stream d'enregistrements properties */
   protected int loadMultiProp(InputStream in) throws Exception {
      MyProperties prop;
      
      boolean mocServerReading=true;
      interruptServerReading=false;
      int n=0;
      
      try {
         while( mocServerReading && !interruptServerReading ) {
            prop = new MyProperties();
            mocServerReading = prop.loadRecord(in);
            if( prop.size()==0 ) continue;
            if( n%1000==0 ) aladin.trace(4,"HipsStore.loadMultiProp(..) "+n+" prop loaded...");
            n++;
            nbRecInProgress=n;
            try {  multiProp.add( prop ); } 
            catch( Exception e ) {
               if( Aladin.levelTrace>=3 ) e.printStackTrace();
            }
         }
         if( interruptServerReading ) aladin.trace(3,"MocServer update interrupted !");
      } finally{ try { in.close(); } catch( Exception e) {} }
      
      return n;
   }
   
   /** Interruption du chargement des infos du MocServer */
   protected void interruptMocServerReading() { interruptServerReading=true; }
   
   /** Génération des noeuds de l'arbre HiPS en fonction du contenu du MultiProp
    * Les noeuds seront triés avant d'être mémorisés
    * Les URLs HiPS seront mémorisées dans le Glu afin de pouvoir gérer les sites miroirs
    */
   private void populateMultiProp(boolean localFile) {
      
      if( listHips==null ) listHips = new ArrayList<TreeObjHips>(20000);
      else listHips.clear();
      
      // On force le recomptage des HiPS
      setRefCount(true);
      
      for( MocItem mi : this ) populateProp(mi.prop,localFile);
      
      Comparator c = TreeObj.getComparator();
      Collections.sort(listHips,c);
      
   }
   
   /** Génération du noeud de l'arbre correspondant à un enregistrement prop, ainsi que des
    * entrées GLU associées
    * @param prop       Enregistrement des propriétés
    * @param localFile  true si cet enregistrement a été chargé localement par l'utilisateur (ACTUELLEMENT NON UTILISE)
    */
   private void populateProp(MyProperties prop, boolean localFile) {
      
      // Détermination de l'identificateur
      String id = MultiMoc.getID(prop);
      
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
      listHips.add( new TreeObjHips(aladin,id,localFile,prop) );
   }
   
   /** Ajustement des propriétés, notamment pour ajouter le bon client_category
    * s'il s'agit d'un catalogue */
   private void propAdjust(String id, MyProperties prop) {
      propAdjust1(id,prop);
      String category = prop.getProperty(Constante.KEY_CLIENT_CATEGORY);
      if( category==null ) prop.setProperty(Constante.KEY_CLIENT_CATEGORY,"Miscellaneous");
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
         
         if( code.equals("J") ) {
            category = "Journal table";
         }
         else {
            int c = Util.indexInArrayOf(code, CAT_CODE);
            if( c==-1 ) category = "Catalog/"+code;   // Catégorie inconnue
            else {
               category = "Catalog/"+CAT_LIB[c];
               
               String sortKey = "Catalog/"+c+"/"+getCatSuffix(id);
               prop.replaceValue(Constante.KEY_CLIENT_SORT_KEY,sortKey);
           }
         }
         
         // Détermination du suffixe (on ne va pas créer un folder par un élément unique)
         String parent = getCatParent(id);
         boolean hasMultiple = hasMultiple(parent);
         if( !hasMultiple ) parent = getCatParent( parent );
         else {
            // Je remplace le numéro par l'obs_collection
            String collection = prop.get(Constante.KEY_OBS_COLLECTION);
            if( collection!=null ) {
               int i = parent.lastIndexOf('/');
               parent = ( i==-1 ? "" : parent.substring(0,i+1) )+ collection;
            }
         }
         String suffix = getCatSuffix( parent );
         suffix = suffix==null ? "" : "/"+suffix;
         category += suffix;
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
   
   /** Retourne le code de la catégorie des catalogues, null sinon
    * (ex: CDS/I/246/out => I) */
   private String getCatCode(String id) {
      int i=id.indexOf('/');
      int j=id.indexOf('/',i+1);
      if( i>0 && j>i ) return id.substring(i+1,j);
      return null;
   }
   
   /** Retourne le suffixe de l'identificateur d'un catalogue => tous ce qui suit le code
    * de catégorie
    * (ex: CDS/I/246/out => 246/out) */
   private String getCatSuffix(String id) {
      int i=id.indexOf('/');
      int j=id.indexOf('/',i+1);
      if( i>0 && j>i ) return id.substring(j+1);
      return null;
   }
   
   /** Retourne le préfixe parent d'un identificateur de catalgoue => tout ce qui précède
    * le dernier mot
    * (ex: CDS/I/246/out => CDS/I/246) */
   private String getCatParent(String id) {
      int i=id.lastIndexOf('/');
      if( i>0 ) return id.substring(0,i);
      return null;
   }
   
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
   // DANS LE CAS D'UN SCRIPT, IL FAUDRAIT QUE LA MISE A JOUR SOIT IMMEDIATE (waitingDialog)
   private void initMultiProp() {

      // Tentative de rechargement depuis le cache
      if( cacheRead() ) {
         startTimer();
         (new Thread("updateFromMocServer"){
            public void run() { updateFromMocServer(); }
         }).start();


         // Le cache est vide => il faut charger depuis le MocServer
      } else {

         // L'initialisation se fait en deux temps pour pouvoir laisser
         // l'utilisateur travailler plus rapidement
         String s = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
               +"&hips_service_url=*&fields=!obs_description,!hipsgen*&get=record";
         loadFromMocServer(s);
         startTimer();

         (new Thread("updateFromMocServer"){
            public void run() { 
               if( loadFromMocServer(MOCSERVER_INIT) ) {
                  cacheWrite();
                  populateMultiProp(false);
                  askForResumeTree();
               }
            }
         }).start();
      }

      populateMultiProp(false);
      askForResumeTree();
   }

   // IL FAUT PREVOIR LE CAS D'UN SIMPLE AJOUT, ET NON D'UN REMPLACEMENT COMPLET
   protected boolean addHipsProp(InputStream in, boolean localFile) {
      try {
         loadMultiProp(in);
         populateMultiProp(localFile);
      } catch( Exception e ) {
         if( aladin.levelTrace>3 ) e.printStackTrace();
         return false;
      }
      return true;
   }
   
   /** Demande de maj des enr. via le MocServer. La date d'estampillage de référence sera
    * la plus tardive de l'ensemble des enr. déjà copiés. Et si aucun, ce sera la date
    * du fichier de cache (avec un risque de décalage des horloges entre le MocServer et
    * la machine locale
    * @return true si l'update a fonctionné.
    */
   private boolean updateFromMocServer() {
      long ts = getMultiPropTimeStamp();
      if( ts==0L ) ts = getCacheTimeStamp();
//      ts = -1L;
//      return loadFromMocServer("obs_regime=Infrared&get=record");
      return loadFromMocServer("TIMESTAMP=>"+ts+"&get=record");
   }
   
   private boolean loadFromMocServer(String params) {
      InputStream in=null;
      boolean eof=false;
      
      String text = params.indexOf("TIMESTAMP")>=0 ? "updat":"load";

      // Recherche sur le site principal, et sinon sur un site miroir
      try {
         mocServerLoading = true;

         long t0 = System.currentTimeMillis();
         Aladin.trace(3,text+"ing Multiprop definitions from MocServer...");

         String u = aladin.glu.getURL("MocServer", params, true).toString();
         try {
            in = Util.openStream(u,false,3000);
            if( in==null ) throw new Exception("cache openStream error");

            // Peut être un site esclave actif ?
         } catch( EOFException e1 ) {
            eof=true;
         } catch( Exception e) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            in = Util.openStream(u,false,-1);
         }
         
         int n = 0;
         if( !eof ) n=loadMultiProp(in);
         Aladin.trace(3,"Multiprop "+text+"ed in "+(System.currentTimeMillis()-t0)+"ms => "+n+" record"+(n>1?"s":""));
         
      } catch( Exception e1 ) {
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
         return false;
         
      } finally {
         mocServerLoading=false;
         if( in!=null ) { try { in.close(); } catch( Exception e) {} }
      }
      return true;
   }

   private String getId(String ivoid) {
      if( ivoid.startsWith("ivo://") ) return ivoid.substring(6);
      return ivoid;
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
   
   
   public void paintComponent(Graphics g) {
      
      // La regénération de l'arbre doit être faite par l'EDT sinon il y a de temps
      // en temps des conflits d'accès sur la structure de l'arbre
      // SI CA S'AVERE TROP LONG IL FAUDRAIT EXPLORER L'UTILISATION D'UN SWINGWORKER...
      if( !init && !isTreeReady() ) { resumeTree(); }
      
      super.paintComponent(g);
      
      g.setFont( Aladin.BOLD );
      
      // Petit message pour avertir l'utilisateur que l'on charge des définitions
      if( mocServerLoading ) {
         String dot="";
         for( int i = 0; i<blinkDot; i++ ) dot+=".";
         blinkDot++;
         if( blinkDot>10 ) blinkDot=0;
         g.setColor(Aladin.BKGD);
         String s = "Updating registry ("+nbRecInProgress+")"+dot;
         g.drawString(s, 16, 25);
         Slide.drawBall(g, 2, 16, blinkState ? Color.white : Color.orange);
         
      } else {
         
         g.setColor( Aladin.DARKBLUE );
         g.drawString("Collection registry",16,25);
         
         // On fait clignoter le voyant d'attente d'info in/out
         if( flagCheckIn ) {
            startTimer();
            Slide.drawBall(g, 2, 16, blinkState ? Color.white : Color.green );
            
         } else {
            stopTimer();
            Slide.drawBall(g, 2, 16, Color.green );
         }
      }
   }
   
   
   /************** Pour faire la maj en continue des HiPS visibles dans la vue *******************************/
   
   private Thread threadUpdater=null;
   private boolean encore=true;

   private void startHipsUpdater() {
      if( threadUpdater==null ) {
         threadUpdater = new Updater("HipsUpdater");
         threadUpdater.start();
      } else encore=true;
   }

   private void stopHipsUpdater() { encore=false; }

   class Updater extends Thread {
      public Updater(String s) { super(s); }

      public void run() {
         encore=true;
         //         System.out.println("Hips updater running");
         while( encore ) {
            try {
               //               System.out.println("Hips updater checking...");
               if( isReadyForUpdating() ) resumeIn();
               Thread.currentThread().sleep(1000);
            } catch( Exception e ) { }
         }
         //         System.out.println("Hips updater stopped");
         threadUpdater=null;
      }
   }

   
   /********* Classe gérant une fenêtre d'information associée au HiPS cliqué dans l'arbre des HiPS ***************/
   
   private class FrameInfo extends JFrame {
      
      ArrayList<TreeObjHips> treeObjs=null;     // hips dont il faut afficher les informations
      JPanel panelInfo=null;                // le panel qui contient les infos (sera remplacé à chaque nouveau hips)
      JCheckBox hipsBx=null,mocBx=null,progBx=null, dmBx=null, csBx=null, allBx=null;
      
      FrameInfo() {
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         JPanel contentPane = (JPanel)getContentPane();
         contentPane.setLayout( new BorderLayout(5,5)) ;
         contentPane.setBackground( new Color(240,240,250));
         contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
         setUndecorated(true);
         setAlwaysOnTop(true);
      }
      
      boolean isSame(ArrayList<TreeObjHips> a1, ArrayList<TreeObjHips> a2) {
         if( a1==null && a2==null ) return true;
         if( a1==null || a2==null ) return false;
         if( a1.size() != a2.size() ) return false;
         for( TreeObjHips ti : a1) {
            if( !a2.contains(ti) ) return false;
         }
         return true;
      }
      
      /** Positionne le hips concerné, et regénère le panel en fonction */
      void setHips(ArrayList<TreeObjHips> treeObjs) {
         if( isSame(treeObjs,this.treeObjs) ) return;
         this.treeObjs = treeObjs;
         resumePanel();
      }
      
      /** Reconstruit le panel des informations en fonction du hips courant */
      void resumePanel() {
         JPanel contentPane = (JPanel)getContentPane();
         if( panelInfo!=null ) contentPane.remove(panelInfo);
         
         panelInfo = new JPanel( new BorderLayout() );
         panelInfo.setBorder( BorderFactory.createEmptyBorder(5, 5, 2, 5));
         
         String s;
         long nbRows=0;
         MyAnchor a;
         GridBagConstraints c = new GridBagConstraints();
         GridBagLayout g =  new GridBagLayout();
         c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
         c.insets = new Insets(2,2,0,5);
         JPanel p = new JPanel(g);
         
         TreeObjHips hips=null;
         
         if( treeObjs.size()>1 )  {
            a = new MyAnchor(aladin,treeObjs.size()+" data sets selected",50,null,null);
            PropPanel.addCouple(p,null, a, g,c);
            
         } else {
         
            hips = treeObjs.get(0);

            if( hips.verboseDescr!=null || hips.description!=null ) {
               s = hips.verboseDescr==null ? "":hips.verboseDescr;
               s =  s+"\n \n"+hips.prop.getRecord(null);
               a = new MyAnchor(aladin,hips.description,200,s,null);
               a.setFont(a.getFont().deriveFont(Font.PLAIN));
               PropPanel.addCouple(p,null, a, g,c);
            }
            String provenance = hips.copyright==null ? hips.copyrightUrl : hips.copyright;
            if( provenance!=null ) {
               s = ".Provenance: "+provenance;
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               PropPanel.addCouple(p,null, a, g,c);
            }

            JPanel p1 = new JPanel(new BorderLayout(15,0));
            s  = hips.getProperty(Constante.KEY_MOC_SKY_FRACTION);
            if( s!=null ) {
               try { s = Util.myRound( Double.parseDouble(s)*100); } catch( Exception e) {}
               s = ".Sky coverage: "+s+"%";
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a,BorderLayout.WEST);
            }
            s  = hips.getProperty(Constante.KEY_HIPS_PIXEL_SCALE);
            if( s!=null ) {
               try { s = Coord.getUnit( Double.parseDouble(s)); } catch( Exception e) {}
               s = "    .HiPS pixel scale: "+s;
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a,BorderLayout.CENTER);
            }
            s  = hips.getProperty(Constante.KEY_NB_ROWS);
            if( s!=null ) {
               try { nbRows = Long.parseLong(s); } catch( Exception e) {}
               s = "    .nb_row: "+s;
               a = new MyAnchor(aladin,s,50,null,null);
               a.setForeground(Color.gray);
               p1.add(a,BorderLayout.CENTER);
            }

            if( p1.getComponentCount()>0 ) PropPanel.addCouple(p,null, p1, g,c);

            JPanel mocAndMore = new JPanel( new FlowLayout(FlowLayout.CENTER,5,0));
            JCheckBox bx;
            hipsBx = mocBx = progBx = dmBx = csBx = allBx = null;
            if( hips.getUrl()!=null ) {
               hipsBx = bx = new JCheckBox("Progr. survey");
               mocAndMore.add(bx);
               bx.setSelected(true);
               bx.setToolTipText("Hierarchical Progressive Survey (HiPS)");
            }
            if( hips.isCatalog() ) {
               boolean allCat = nbRows<2000;
               if( nbRows<10000 ) {
                  allBx = bx = new JCheckBox("All sources");
                  mocAndMore.add(bx);
                  bx.setSelected(hips.getUrl()==null && allCat );
                  bx.setToolTipText("Load all sources (small catalog/table)");

               } 

               csBx = bx = new JCheckBox("Cone search");
               mocAndMore.add(bx);
               bx.setSelected(hips.getUrl()==null && !allCat);
               bx.setToolTipText("Cone search on the current view");
            }
            mocBx = bx = new JCheckBox("Coverage"); 
            mocAndMore.add(bx); 
            bx.setToolTipText("MultiOrder Coverage map (MOC)");
            if( hips.isCatalog() ) {
               dmBx = bx = new JCheckBox("Density map");
               mocAndMore.add(bx);
               Util.toolTip(bx,"Progressive view (HiPS) of the density map associated to the catalog",true);
            } else {
               if( hips.getProgenitorsUrl()!=null ) {
                  progBx = bx = new JCheckBox("Orig.data links");
                  mocAndMore.add(bx);
                  Util.toolTip(bx,"Meta data and links to original data sets (progenitors access)",true);
               }
            }
            PropPanel.addCouple(p,"", mocAndMore, g,c);

         }
         
         panelInfo.add(p,BorderLayout.CENTER);

         JPanel control = new JPanel( new FlowLayout(FlowLayout.CENTER,6,2) );
         control.setBackground( contentPane.getBackground() );
         JButton b = new JButton("Load"); b.setMargin( new Insets(2,4,2,4));
         b.setFont(b.getFont().deriveFont(Font.BOLD));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               submit();
               hideInfo();
            }
         });
         b = new JButton("Close"); b.setMargin( new Insets(2,4,2,4));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { hideInfo(); }
         });
         
         JPanel bas = new JPanel( new BorderLayout(0,0));
         bas.add(control,BorderLayout.CENTER);
         
         if( hips!=null && hips.internalId!=null ) {
            JLabel x = new JLabel(hips.internalId);
            x.setForeground(Aladin.GREEN);
            bas.add(x,BorderLayout.WEST);
         }
         
         contentPane.add(panelInfo,BorderLayout.CENTER);
         panelInfo.add(bas,BorderLayout.SOUTH);
         contentPane.validate();
      }
      
      void submit() {
         if( treeObjs.size()==1 ) {
            TreeObjHips hips = treeObjs.get(0);
            if( allBx!=null  && allBx.isSelected() )   hips.loadAll();
            if( csBx!=null   && csBx.isSelected() )    hips.loadCS();
            if( hipsBx!=null && hipsBx.isSelected() )  hips.loadHips();
            if( mocBx!=null  && mocBx.isSelected() )   hips.loadMoc();
            if( progBx!=null && progBx.isSelected() )  hips.loadProgenitors();
            if( dmBx!=null   && dmBx.isSelected() )    hips.loadDensityMap();
         } else {
            System.out.println("Je dois charger "+treeObjs.size()+" data sets...");
         }
      }
   }
   
}

