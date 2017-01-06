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
import javax.swing.ButtonGroup;
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
 * Classe qui g�re l'arbre HiPS apparaissant � gauche de la vue
 * @version 1.0 d�cembre 2016 - cr�ation
 * @author Pierre Fernique [CDS]
 */
public class RegStore extends JPanel implements Iterable<MocItem>{
   
   private Aladin aladin;                  // R�f�rence
   private MultiMoc multiProp;             // Le multimoc de stockage des properties des collections
   private RegFilter regFilter=null;       // Formulaire de filtrage de l'arbre des collections
   private boolean mocServerLoading=false; // true = requ�te de g�n�ration de l'arbre en cours (appel � MocServer)
   
   private RegTree regTree;               // Le JPanel de l'arbre des collections
   private ArrayList<TreeObjReg> listReg;  // Liste des noeuds potentiels de l'arbre
   
   // Composantes de l'interface
   private JButton filter;    // Bouton d'ouverture du formulaire de filtrage
   protected Prune prune;     // L'icone d'activation du mode "�lagage"
   private Collapse collapse; // L'icone pour d�velopper/r�duire l'arbre
   private Timer timer = null;// Timer pour le r�affichage lors du chargement
   
   // Param�tres d'appel initial du MocServer (construction de l'arbre)
//   private static String  MOCSERVER_INIT = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")+"&hips_service_url=*&get=record"; //&fmt=glu";
   private static String  MOCSERVER_INIT = "*&fields=!hipsgen*&get=record&fmt=asciic";
   
   // Param�tres de maj par le MocServer (update de l'arbre)
   private static String MOCSERVER_UPDATE = "fmt=asciic";
//   private static String MOCSERVER_UPDATE = "client_application=AladinDesktop"+(Aladin.BETA?"*":"")+"&hips_service_url=*&";

   public RegStore(Aladin aladin) {
      this.aladin = aladin;
      
      // POUR LES TESTS => Surcharge de l'URL du MocServer
//      aladin.glu.aladinDic.put("MocServer","http://localhost:8080/MocServer/query?$1");
      
      multiProp = new MultiMoc();
      
      // L'arbre avec sa scrollbar
      regTree = new RegTree(aladin);
      setBackground(aladin.getBackground());
      regTree.setBackground(aladin.getBackground());
      
      JScrollPane scrollTree = new JScrollPane(regTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
      regTree.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            TreePath tp = regTree.getPathForLocation(e.getX(), e.getY());
            if( tp==null ) hideInfo();
            else {
               ArrayList<TreeObjReg> treeObjs = getSelectedTreeObjHips();

               // Double-clic, on effectue l'action par d�faut du noeud
               if (e.getClickCount() == 2) loadMultiHips( treeObjs );

               // Simple clic => on montre les informations associ�es au noeud
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
   
   /** R�cup�ration de la liste des TreeObj s�lectionn�es */
   private ArrayList<TreeObjReg> getSelectedTreeObjHips() {
      TreePath [] tps = regTree.getSelectionPaths();
      ArrayList<TreeObjReg> treeObjs = new ArrayList<TreeObjReg>();
      if( tps!=null ) {
         for( int i=0; i<tps.length; i++ ) {
            Object obj = ((DefaultMutableTreeNode)tps[i].getLastPathComponent()).getUserObject();
            if( obj instanceof TreeObjReg ) treeObjs.add( (TreeObjReg)obj );
         }
      }
      return treeObjs;
   }
   
   // La frame d'affichage des informations du HiPS cliqu� � la souris
   private FrameInfo frameInfo = null;
   
   /** Cache la fen�tre des infos du HiPS */
   private void hideInfo() { showInfo(null,null); }
   
   private void loadMultiHips(ArrayList<TreeObjReg> treeObjs) {
      if( treeObjs.size()==0 ) return;
      hideInfo();
      treeObjs.get(0).load();
   }

   
   /** Affiche la fenetre des infos des Collections pass�es en param�tre
    * @param treeObjs les noeuds correspondant aux collections s�lectionn�es, ou null si effacement de la fen�tre
    * @param e �v�nement souris pour r�cup�rer la position absolue o� il faut afficher la fen�tre d'info
    */
   private void showInfo(ArrayList<TreeObjReg> treeObjs, MouseEvent e) {
      if( treeObjs==null || treeObjs.size()==0 ) {
         if( frameInfo==null ) return;
         frameInfo.setVisible(false);
         return;
      }
      if( frameInfo==null ) frameInfo = new FrameInfo();
      Point p = e.getLocationOnScreen();
      frameInfo.setCollections( treeObjs );
      
      int w=450;
      int h=120;
      int x=p.x+50;
      int y=p.y-30;
      
      if( y<0 ) y=0;
      if( y+h > aladin.SCREENSIZE.height ) y = aladin.SCREENSIZE.height-h;
      if( x+w > aladin.SCREENSIZE.width )  x = aladin.SCREENSIZE.width -w;
      frameInfo.setBounds(x,y,w,h);
      frameInfo.setVisible(true);
   }
   
   /** Cr�ation/ouverture du formulaire de filtrage de l'arbre des Collections */
   private void filtre() {
      if( regFilter==null ) regFilter = new RegFilter(aladin);
      regFilter.showFilter();
   }
   
   /**
    * Retourne le node correspondant � une identiciation
    * @param A l'identificateur de la collection � chercher
    * @param flagSubstring true si on prend en compte le cas d'une sous-chaine
    * @param mode 0 - match exact
    *             1 - substring sur label
    *             2 - match exact puis substring sur l'IVOID (ex: Simbad ok pour CDS/Simbad)
    *                 puis du menu  (ex DssColored ok pour Optical/DSS/DssColored)
    * @return le Hips node trouv�, null sinon
    */
   protected TreeObjReg getTreeObjReg(String A) { return getTreeObjReg(A,0); }
   protected TreeObjReg getTreeObjReg(String A,int mode) {
      for( TreeObjReg to : listReg ) {
         if( A.equals(to.id) || A.equals(to.label) || A.equals(to.internalId) ) return to;
         if( mode==1 && Util.indexOfIgnoreCase(to.label,A)>=0 ) return to;
         if( mode==2 ) {
            if( to.internalId!=null && to.internalId.endsWith(A) ) return to;

            int offset = to.label.lastIndexOf('/');
            if( A.equals(to.label.substring(offset+1)) ) return to;
         }
      }

      if( mode==2 ) {
         for( TreeObjReg to : listReg ) {
            int offset = to.label.lastIndexOf('/');
            if( Util.indexOfIgnoreCase(to.label.substring(offset+1),A)>=0 ) return to;
         }
      }
      return null;
   }
   
   /** Cr�ation du plan via script - m�thode identique � celle de la s�rie des classes ServerXXX
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
      
      TreeObjReg to = getTreeObjReg(survey,2);
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
   
   // False lorsque la premi�re initialisation de l'arbre est faite
   private boolean init=true;
   
   /** G�n�ration initiale de l'arbre - et maj des menus Aladin correspondants */
   protected void initTree() {
      try {
         initMultiProp();
         rebuildTree();
         regTree.defaultExpand();
      } finally { postTreeProcess(); init=false;}
   }
   
   boolean refCount = true;
   private void setRefCount(boolean flag) { refCount=flag; }

   /** (Re)construction de l'arbre en fonction de l'�tat pr�c�dent de l'arbre
    * et de la valeur des diff�rents flags associ�s aux noeuds
    * @param refCount  true si on doit faire les d�comptages des noeuds en tant que r�f�rence sinon �tat courant
    * @return true s'il y a eu au-moins une modif
    */
   private boolean rebuildTree() {
      boolean pruneActivated = prune.isActivated();
      boolean modif = false;
      try {
         regTree.setLockExpand(true);
         for( TreeObjReg to : listReg ) { 
            boolean mustBeActivated = !to.isHidden() && (!pruneActivated || pruneActivated && to.getIsIn()!=0 );
            modif |= setActivated( to, regTree, mustBeActivated );
         }
         if( modif || refCount ) {
            regTree.countDescendance( refCount );
            refCount=false;
         }
         if( prune.isAvailable() && !pruneActivated ) regTree.populateFlagIn();
         
      } finally { regTree.setLockExpand(false); }
      return modif;
   }
   
   /** Activation/d�sactivation d'un noeud de l'arbre */
   protected boolean setActivated(TreeObjReg to, RegTree regTree, boolean activated) {
      if( to.isActivated() == activated ) return false;
      to.setActivated(activated);
      if( activated ) regTree.createTreeBranch( to );
      else regTree.removeTreeBranch( to );
      return true;
   }
   
   private boolean treeReady=true;
   private Object lock = new Object();
   protected void setTreeReady(boolean flag) { synchronized( lock ) { treeReady=flag; } }
   protected boolean isTreeReady()           { synchronized( lock ) { return treeReady; } }
   
   /** Demande de reg�n�ration de l'arbre => sera fait par l'EDT dans le paintComponent() */
   protected void askForResumeTree() {
      setTreeReady(false);
      repaint();
   }
      
   /** R�affichage de l'arbre en fonction des flags courants */
   protected void resumeTree() {
      try {
         long t0 = System.currentTimeMillis();
         if( !rebuildTree() ) return;
         validate();
         postTreeProcess();
         System.out.println("resumeTree done in "+(System.currentTimeMillis()-t0)+"ms");
      } finally { setTreeReady( true ); }
      
      // Pour permettre le changement du curseur d'attente de la fen�tre de filtrage
      if( regFilter!=null ) aladin.makeCursor(regFilter, Aladin.DEFAULTCURSOR);
   }
   
   /** Filtrage et r�affichage de l'arbre en fonction des contraintes indiqu�es dans params
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

   /** R�affichage de l'arbre en fonction des Hips in/out de la vue courante */
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
      ArrayList<String> ids = multiProp.scan( (HealpixMoc)null, expr, false, -1);
      System.out.println("Filter: "+ids.size()+"/"+multiProp.size()+" in "+(System.currentTimeMillis()-t0)+"ms");
      
      // Positionnement des flags isHidden() en fonction du filtrage
      HashSet<String> set = new HashSet<String>( ids.size() );
      for( String s : ids ) set.add(s);      
      for( TreeObjReg to : listReg ) to.setHidden( !set.contains(to.internalId) );
   }
   
   // Dernier champs interrog� sur le MocServer
   private Coord oc=null;
   private double osize=-1;
   private boolean flagCheckIn=false;

   /** Interroge le MocServer pour conna�tre les Collections disponibles dans le champ.
    * Met � jour l'arbre en cons�quence */
   private boolean checkIn() {
      if( !dialogOk() ) return false; 

      // Le champ est trop grand ou que la vue n'a pas de r�f spatiale ?
      // => on suppose que tous les HiPS sont a priori visibles
      ViewSimple v = aladin.view.getCurrentView();
      if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
         boolean modif=false;
         for( TreeObjReg to : listReg ) {
            if( !modif && to.getIsIn()!=-1 ) modif=true;
            to.setIn(-1);
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

            // Pour �viter de faire 2x la m�me chose de suite
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

            // r�cup�ration de chaque ID concern�e (1 par ligne)
            HashSet<String> set = new HashSet<String>();
            while( (s=in.readLine())!=null ) set.add( getId(s) );

            // Positionnement des flags correspondants
            for( TreeObjReg to : listReg ) to.setIn( set.contains(to.internalId) ? 1 : 0 );
         
         } catch( EOFException e ) {}
         finally{ flagCheckIn=false; if( in!=null ) in.close(); }
         
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

      return true;
   }

   /** Retourne true si l'arbre est d�velopp� selon le d�faut pr�vu */
   protected boolean isDefaultExpand() { return regTree.isDefaultExpand(); }
   
   /** Collapse l'arbre sauf le noeud courant */
   protected void collapseAllExceptCurrent() {
      if( isDefaultExpand() ) regTree.allExpand();
      else regTree.defaultExpand();
   }
   
   /** Retourne true s'il n'y a pas d'arbre HiPS */
   protected boolean isFree() {
      return regTree==null || regTree.root==null ;
   }

   /** Traitement � applique apr�s la g�n�ration ou la r�g�n�ration de l'arbre */
   private void postTreeProcess() {
      
      filter.setEnabled( dialogOk() );
      
      // Mise en route ou arr�t du thread de coloration de l'arbre en fonction des Collections
      // pr�sentes ou non dans la vue courante
      if( !isFree() ) startHipsUpdater();
      else stopHipsUpdater();
   }
   
   private int nbRecInProgress=0;
   private boolean interruptServerReading;
   
   /** (Re)g�n�ration du Multiprop en fonction d'un stream d'enregistrements properties */
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
            if( n%1000==0 ) aladin.trace(4,"RegStore.loadMultiProp(..) "+n+" prop loaded...");
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
   
   /** G�n�ration des noeuds de l'arbre HiPS en fonction du contenu du MultiProp
    * Les noeuds seront tri�s avant d'�tre m�moris�s
    * Les URLs HiPS seront m�moris�es dans le Glu afin de pouvoir g�rer les sites miroirs
    */
   private void populateMultiProp(boolean localFile) {
      
      if( listReg==null ) listReg = new ArrayList<TreeObjReg>(20000);
      else listReg.clear();
      
      // On force le recomptage des HiPS
      setRefCount(true);
      
      for( MocItem mi : this ) populateProp(mi.prop,localFile);
      
      Comparator c = TreeObj.getComparator();
      Collections.sort(listReg,c);
      
   }
   
   /** G�n�ration du noeud de l'arbre correspondant � un enregistrement prop, ainsi que des
    * entr�es GLU associ�es
    * @param prop       Enregistrement des propri�t�s
    * @param localFile  true si cet enregistrement a �t� charg� localement par l'utilisateur (ACTUELLEMENT NON UTILISE)
    */
   private void populateProp(MyProperties prop, boolean localFile) {
      
      // D�termination de l'identificateur
      String id = MultiMoc.getID(prop);
      
      // Ajustement local des propri�t�s
      propAdjust( id, prop );
      
      String HIPSU = Constante.KEY_HIPS_SERVICE_URL;
      
      // Dans le cas o� il n'y a pas de mirroir, m�morisation de l'URL directement
      if( prop.getProperty(HIPSU+"_1")==null ) {
         String url =  prop.getProperty(HIPSU);
         if( url!=null ) aladin.glu.aladinDic.put(id,url);
         
      // Dans le cas o� il y a des mirroirs => m�morisation des indirections
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
         
         // M�morisation des indirections possibles sous la forme %I id0\tid1\t...
         aladin.glu.aladinDic.put(id,indirection.toString());
      }
      
      // Ajout dans la liste des noeuds d'arbre
      listReg.add( new TreeObjReg(aladin,id,localFile,prop) );
   }
   
   /** Ajustement des propri�t�s, notamment pour ajouter le bon client_category
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
         
         // D�termination de la cat�gorie
         String code = getCatCode(id);
         if( code==null ) return;
         
         if( code.equals("J") ) {
            category = "Journal table";
         }
         else {
            int c = Util.indexInArrayOf(code, CAT_CODE);
            if( c==-1 ) category = "Catalog/"+code;   // Cat�gorie inconnue
            else {
               category = "Catalog/"+CAT_LIB[c];
               
               String sortKey = "Catalog/"+c+"/"+getCatSuffix(id);
               prop.replaceValue(Constante.KEY_CLIENT_SORT_KEY,sortKey);
           }
         }
         
         // D�termination du suffixe (on ne va pas cr�er un folder par un �l�ment unique)
         String parent = getCatParent(id);
         boolean hasMultiple = hasMultiple(parent);
         if( !hasMultiple ) parent = getCatParent( parent );
         else {
            // Je remplace le num�ro par l'obs_collection
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
   
   // M�morise les paths de l'arbre qui ont de multiples feuilles
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
   
   /** Retourne le code de la cat�gorie des catalogues, null sinon
    * (ex: CDS/I/246/out => I) */
   private String getCatCode(String id) {
      int i=id.indexOf('/');
      int j=id.indexOf('/',i+1);
      if( i>0 && j>i ) return id.substring(i+1,j);
      return null;
   }
   
   /** Retourne le suffixe de l'identificateur d'un catalogue => tous ce qui suit le code
    * de cat�gorie
    * (ex: CDS/I/246/out => 246/out) */
   private String getCatSuffix(String id) {
      int i=id.indexOf('/');
      int j=id.indexOf('/',i+1);
      if( i>0 && j>i ) return id.substring(j+1);
      return null;
   }
   
   /** Retourne le pr�fixe parent d'un identificateur de catalgoue => tout ce qui pr�c�de
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
   
   // G�n�ration du MultiMoc depuis la sauvegarde binaire du cache
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
   
   /** Retourne true si le HipsStore est pr�t */
   protected boolean dialogOk() {
      return !mocServerLoading && multiProp.size()>0;
   }
   
   /** Chargement des descriptions de l'arbre par le MocServer */
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
   
   /** Demande de maj des enr. via le MocServer. La date d'estampillage de r�f�rence sera
    * la plus tardive de l'ensemble des enr. d�j� copi�s. Et si aucun, ce sera la date
    * du fichier de cache (avec un risque de d�calage des horloges entre le MocServer et
    * la machine locale
    * @return true si l'update a fonctionn�.
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

            // Peut �tre un site esclave actif ?
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
   
   /** R�affichage du Panel tous les demi-secondes afin de faire clignoter le message d'attente */
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
      
      // La reg�n�ration de l'arbre doit �tre faite par l'EDT sinon il y a de temps
      // en temps des conflits d'acc�s sur la structure de l'arbre
      // SI CA S'AVERE TROP LONG IL FAUDRAIT EXPLORER L'UTILISATION D'UN SWINGWORKER...
      if( !init && !isTreeReady() ) { resumeTree(); }
      
      super.paintComponent(g);
      
      g.setFont( Aladin.BOLD );
      
      // Petit message pour avertir l'utilisateur que l'on charge des d�finitions
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
         threadUpdater = new Updater("RegUpdater");
         threadUpdater.start();
      } else encore=true;
   }

   private void stopHipsUpdater() { encore=false; }

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

   
   /********* Classe g�rant une fen�tre d'information associ�e au HiPS cliqu� dans l'arbre des HiPS ***************/
   
   private class FrameInfo extends JFrame {
      
      ArrayList<TreeObjReg> treeObjs=null;     // hips dont il faut afficher les informations
      JPanel panelInfo=null;                // le panel qui contient les infos (sera remplac� � chaque nouveau hips)
      JCheckBox hipsBx=null,mocBx=null,progBx=null, dmBx=null, csBx=null, msBx=null, allBx=null;
      
      FrameInfo() {
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         JPanel contentPane = (JPanel)getContentPane();
         contentPane.setLayout( new BorderLayout(5,5)) ;
         contentPane.setBackground( new Color(240,240,250));
         contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
         setUndecorated(true);
         setAlwaysOnTop(true);
      }
      
      boolean isSame(ArrayList<TreeObjReg> a1, ArrayList<TreeObjReg> a2) {
         if( a1==null && a2==null ) return true;
         if( a1==null || a2==null ) return false;
         if( a1.size() != a2.size() ) return false;
         for( TreeObjReg ti : a1) {
            if( !a2.contains(ti) ) return false;
         }
         return true;
      }
      
      /** Positionne les collections concern�es, et reg�n�re le panel en fonction */
      void setCollections(ArrayList<TreeObjReg> treeObjs) {
         if( isSame(treeObjs,this.treeObjs) ) return;
         this.treeObjs = treeObjs;
         resumePanel();
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
         
         TreeObjReg to=null;
         
         if( treeObjs.size()>1 )  {
            a = new MyAnchor(aladin,treeObjs.size()+" collections selected",50,null,null);
            PropPanel.addCouple(p,null, a, g,c);
            
         } else {
         
            to = treeObjs.get(0);

            if( to.verboseDescr!=null || to.description!=null ) {
               s = to.verboseDescr==null ? "":to.verboseDescr;
               s =  s+"\n \n"+to.prop.getRecord(null);
               a = new MyAnchor(aladin,to.description,200,s,null);
               a.setFont(a.getFont().deriveFont(Font.PLAIN));
               PropPanel.addCouple(p,null, a, g,c);
            }
            String provenance = to.copyright==null ? to.copyrightUrl : to.copyright;
            if( provenance!=null ) {
               s = ".Provenance: "+provenance;
               a = new MyAnchor(aladin,s,50,null,null);
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
            hipsBx = mocBx = progBx = dmBx = csBx = allBx = null;
            if( to.getUrl()!=null ) {
               hipsBx = bx = new JCheckBox("Progr. survey");
               mocAndMore.add(bx);
               bx.setSelected(true);
               bx.setToolTipText("Hierarchical Progressive Survey (HiPS)");
            }
            if( to.isCatalog() ) {
               boolean allCat = nbRows<2000;
               ButtonGroup bg = new ButtonGroup();
               if( nbRows!=-1 && nbRows<10000 ) {
                  allBx = bx = new JCheckBox("All sources");
                  mocAndMore.add(bx);
                  bx.setSelected(to.getUrl()==null && allCat );
                  bx.setToolTipText("Load all sources (small catalog/table <10000)");
                  bg.add(bx);
               } 

               csBx = bx = new JCheckBox("Cone search");
               mocAndMore.add(bx);
               bx.setSelected(to.getUrl()==null && !allCat);
               bx.setToolTipText("Cone search on the current view");
               bg.add(bx);
               
               msBx = bx = new JCheckBox("Query by MOC");
               mocAndMore.add(bx);
               bx.setSelected(false);
               bx.setToolTipText("Load all sources inside the selected MOC in the stack");
               bg.add(bx);
            
            }
            mocBx = bx = new JCheckBox("Coverage"); 
            mocAndMore.add(bx); 
            bx.setToolTipText("MultiOrder Coverage map (MOC)");
            if( to.isCatalog() ) {
               dmBx = bx = new JCheckBox("Density map");
               mocAndMore.add(bx);
               Util.toolTip(bx,"Progressive view (HiPS) of the density map associated to the catalog",true);
            } else {
               if( to.getProgenitorsUrl()!=null ) {
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
         
         if( to!=null && to.internalId!=null ) {
            JLabel x = new JLabel(to.internalId);
            x.setForeground(Aladin.GREEN);
            bas.add(x,BorderLayout.WEST);
         }
         
         contentPane.add(panelInfo,BorderLayout.CENTER);
         panelInfo.add(bas,BorderLayout.SOUTH);
         contentPane.validate();
      }
      
      void submit() {
         if( treeObjs.size()==1 ) {
            TreeObjReg to = treeObjs.get(0);
            if( allBx!=null  && allBx.isSelected() )   to.loadAll();
            if( csBx!=null   && csBx.isSelected() )    to.loadCS();
            if( msBx!=null   && msBx.isSelected() )    to.queryByMoc();
            if( hipsBx!=null && hipsBx.isSelected() )  to.loadHips();
            if( mocBx!=null  && mocBx.isSelected() )   to.loadMoc();
            if( progBx!=null && progBx.isSelected() )  to.loadProgenitors();
            if( dmBx!=null   && dmBx.isSelected() )    to.loadDensityMap();
         } else {
            System.out.println("Je dois charger "+treeObjs.size()+" data sets...");
         }
      }
   }
   
}

