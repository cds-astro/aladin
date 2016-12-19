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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import cds.aladin.prop.PropPanel;
import cds.tools.Util;


/**
 * Classe qui gère l'arbre HiPS apparaissant à gauche de la vue
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public class HipsStore extends JPanel {
   
   private Aladin aladin;                  // Référence
   private HipsFilter hipsFilter=null;     // Formulaire de filtrage de l'arbre HiPS
   private boolean mocServerLoading=false; // true = requête de génération de l'arbre en cours (appel à MocServer)
   private PlanMoc planMoc=null;           // Un Moc à afficher temporairement (passage de la souris sur l'arbre HiPS)
   
   // Composantes de l'interface
   private MyTree tree;
   private JButton filter;
   private Prune prune;
   
   // Paramètres d'appel initial du MocServer (construction de l'arbre)
   private static String  MOCSERVER_PARAMS 
        = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
         +"&hips_service_url=*&get=record"; //&fmt=glu";
   
   public HipsStore(Aladin aladin) {
      this.aladin = aladin;
      
      // L'arbre avec sa scrollbar
      tree = new MyTree(aladin);
      setBackground(aladin.getBackground());
      tree.setBackground(aladin.getBackground());
      JScrollPane scrollTree = new JScrollPane(tree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setBorder( BorderFactory.createEmptyBorder(0,0,0,0));
      
      // Les boutons de controle
      JPanel p1 = new JPanel(new FlowLayout(FlowLayout.LEFT,1,1));
      p1.setBackground(aladin.getBackground());
      
      final Collapse collapse = new Collapse(aladin);
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
      tree.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
            DefaultMutableTreeNode node = null;
            if( tp!=null ) node=(DefaultMutableTreeNode)tp.getLastPathComponent();
            
            if (e.getClickCount() == 2) {
               if (node == null) return;
               hideInfo();
               TreeNodeHips nodeInfo = (TreeNodeHips) node.getUserObject();
               nodeInfo.submit();
            } else {
               showInfo(node,e);
            }
            collapse.repaint();
         }
      });
      
//      // Action à faire lorsque la souris reste plus de 3s sur un noeud terminal
//      tree.addMouseMotionListener(new MouseMotionAdapter() {
//         public void mouseMoved(MouseEvent e) {
//            TreePath tp = tree.getPathForLocation(e.getX(), e.getY());
//            DefaultMutableTreeNode node = null;
//            if( tp!=null ) node=(DefaultMutableTreeNode)tp.getLastPathComponent();
//               
//            if( node==null || !node.isLeaf() ) stopMocTimer();
//            else  if( node.isLeaf() && (lastTp==null || !tp.equals(lastTp)) ) {
//               lastTp=tp;
//               startMocTimer(tp);
//            }
//         }
//      });
      
      // Chargement de l'arbre initial
      initTree();
   }
   
// CHARGEMENT D'UN MOC DE L'ITEM SOUS LA SOURIS AU BOUT DE 3 SECONDES... EN FAIT PAS PRATIQUE
//   private TreePath lastTp=null;
//   private MocTimer mocTimer=null;
//   
//   private void startMocTimer(TreePath tp) {
//      System.out.println("Je dois démarrer un timer sur "+tp);
//      if( mocTimer==null ) {
//         mocTimer = new MocTimer();
//         mocTimer.start();
//      }
//      mocTimer.reset(tp);
//   }
//   
//   private void stopMocTimer() { 
//      if( mocTimer!=null && mocTimer.isWaiting() ) {
//         System.out.println("Je dois reseter le timer");
//         mocTimer.reset(null);
//         lastTp=null;
//         planMoc=null;
//         aladin.view.repaintAll();
//      }
//   }
//   
//   protected void drawMoc(Graphics g, ViewSimple v) {
//      if( planMoc==null || !planMoc.isReady() ) return;
//      planMoc.draw(g, v);
//   }
//   
//   class MocTimer extends Thread {
//      private long debut=-1;
//      private TreePath tp;
//      
//      void reset(TreePath tp) {
//         debut = System.currentTimeMillis();
//         this.tp = tp;
//      }
//      
//      boolean isWaiting() { return tp!=null; }
//      
//      public void run() {
//         System.out.println("Je lance le mocTimer...");
//         while( true ) {
//            long t = System.currentTimeMillis();
//            
//            // Action à faire ?
//            if( debut!=-1 && t-debut>3000 && tp!=null ) {
//               System.out.println("Je dois afficher le MOC de "+tp);
//               if( planMoc==null ) loadMoc();
//               
//            // Arrêt du Timer ?
//            } else if( debut!=-1 && t-debut>10000 && tp==null ) {
//               break;
//            }
//            try{ Util.pause(500); } catch( Exception e ) { }
//         }
//         mocTimer=null;
//         System.out.println("J'arrête le mocTimer !");
//      }
//      
//      void loadMoc() {
//         TreeNodeAllsky hips = (TreeNodeAllsky) ((DefaultMutableTreeNode)tp.getLastPathComponent()).getUserObject();
//         MyInputStream in = null;
//         try {
//            in = Util.openStream(hips.getUrl()+"/Moc.fits");
//            planMoc = new PlanMoc(aladin, in, null, "moc", null, 0);
//            planMoc.c = Color.red;
//         } catch( Exception e ) {}
//
//      }
//   }
   
   // La frame d'affichage des informations du HiPS cliqué à la souris
   private FrameInfo frameInfo = null;
   
   /** Cache la fenêtre des infos du HiPS */
   private void hideInfo() { showInfo(null,null); }
   
   /** Affiche la fenetre des infos du HiPS correspondant au noeud passé en paramètre
    * en face de la position de la souris
    * @param node le noeud correspondant au HiPS sous la souris, ou null si effacement de la fenêtre
    * @param e évènement souris pour récupérer la position absolue où il faut afficher la fenêtre d'info
    */
   private void showInfo(DefaultMutableTreeNode node, MouseEvent e) {
      if( node==null || !node.isLeaf() ) {
         if( frameInfo==null ) return;
         frameInfo.setVisible(false);
         return;
      }
      if( frameInfo==null ) frameInfo = new FrameInfo();
      Point p = e.getLocationOnScreen();
      frameInfo.setHips( (TreeNodeHips) node.getUserObject());
      
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
   
   /** Classe gérant une fenêtre d'information associée au HiPS cliqué dans l'arbre des HiPS */
   class FrameInfo extends JFrame {
      
      TreeNodeHips hips=null;  // hips dont il faut afficher les informations
      JPanel panelInfo=null;         // le panel qui contient les infos (sera remplacé à chaque nouveau hips)
      JCheckBox dataBx=null,mocBx=null;
      
      FrameInfo() {
         setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
         JPanel contentPane = (JPanel)getContentPane();
         contentPane.setLayout( new BorderLayout(5,5)) ;
         contentPane.setBackground( new Color(240,240,250));
         contentPane.setBorder( BorderFactory.createLineBorder(Color.black));
         setUndecorated(true);
      }
      
      /** Positionne le hips concerné, et regénère le panel en fonction */
      void setHips(TreeNodeHips hips) {
         if( hips==this.hips ) return;
         this.hips = hips;
         resumePanel();
      }
      
      /** Reconstruit le panel des informations en fonction du hips courant */
      void resumePanel() {
         JPanel contentPane = (JPanel)getContentPane();
         if( panelInfo!=null ) contentPane.remove(panelInfo);
         
         panelInfo = new JPanel( new BorderLayout() );
         panelInfo.setBorder( BorderFactory.createEmptyBorder(5, 5, 2, 5));
         
         MyAnchor a;
         GridBagConstraints c = new GridBagConstraints();
         GridBagLayout g =  new GridBagLayout();
         c.fill = GridBagConstraints.BOTH;            // J'agrandirai les composantes
         c.insets = new Insets(2,2,0,5);
         JPanel p = new JPanel(g);
         
         if( hips.verboseDescr!=null || hips.description!=null ) {
            String s = hips.verboseDescr==null ? "":hips.verboseDescr;
            if( hips.internalId!=null )s=s+"\n \n=> HiPS identifier: "+hips.internalId;
            a = new MyAnchor(aladin,hips.description,200,s,null);
            a.setFont(a.getFont().deriveFont(Font.PLAIN));
            PropPanel.addCouple(p,null, a, g,c);
         }
         String provenance = hips.copyright==null ? hips.copyrightUrl : hips.copyright;
         if( provenance!=null ) {
            String s = "Provenance: "+provenance;
            a = new MyAnchor(aladin,s,50,null,null);
            a.setForeground(Color.gray);
            PropPanel.addCouple(p,null, a, g,c);
         }
        
         JPanel mocAndMore = new JPanel( new FlowLayout());
         JCheckBox bx;
         dataBx = bx = new JCheckBox("Dataset"); mocAndMore.add(bx); bx.setSelected(true); // bx.setForeground(Color.gray);
         mocBx = bx = new JCheckBox("Coverage (MOC)");  mocAndMore.add(bx); // bx.setForeground(Color.gray);
         if( hips.isCatalog() ) {
            bx = new JCheckBox("Density map"); mocAndMore.add(bx); // bx.setForeground(Color.gray);
         } else {
            bx = new JCheckBox("Progenitors"); mocAndMore.add(bx); // bx.setForeground(Color.gray);
         }
         PropPanel.addCouple(p,"", mocAndMore, g,c); // bx.setForeground(Color.gray);
         
         panelInfo.add(p,BorderLayout.CENTER);

         JPanel control = new JPanel( new FlowLayout(FlowLayout.CENTER,6,2) );
         control.setBackground( contentPane.getBackground() );
         JButton b = new JButton("Load"); b.setMargin( new Insets(2,4,2,4));
         b.setFont(b.getFont().deriveFont(Font.BOLD));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
               submit(hips);
               hideInfo();
            }
         });
         b = new JButton("Close"); b.setMargin( new Insets(2,4,2,4));
         control.add(b);
         b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { hideInfo(); }
         });
         panelInfo.add(control,BorderLayout.SOUTH);
         
         contentPane.add(panelInfo,BorderLayout.CENTER);
         contentPane.validate();
      }
      
      void submit(TreeNodeHips hips) {
         if( dataBx.isSelected() ) hips.submit();
         if( mocBx.isSelected() ) hips.loadMoc();
      }
   }
   
   
   /** Création/ouverture du formulaire de filtrage de l'arbre HiPS */
   private void filtre() {
      if( hipsFilter==null ) hipsFilter = new HipsFilter(aladin);
      hipsFilter.show();
   }
   
   /** Création du plan via script - méthode identique à celle de la série des classes ServerXXX
    * (de fait, reprise de ServerHips de la version Aladin v9)
    * TODO: elle pourrait être décrite via une interface */
   protected int createPlane(String target,String radius,String criteria, String label, String origin) {
      String survey;
      int defaultMode=PlanBG.UNKNOWN;

      if( criteria==null || criteria.trim().length()==0 ) survey="DSS colored";
      else {
         Tok tok = new Tok(criteria,", ");
         survey = tok.nextToken();

         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( s.equalsIgnoreCase("Fits") ) defaultMode=PlanBG.FITS;
            else if( s.equalsIgnoreCase("Jpeg") || s.equalsIgnoreCase("jpg") || s.equalsIgnoreCase("png") ) defaultMode=PlanBG.JPEG;
         }
      }

      int j = aladin.glu.findGluSky(survey,2);
      if( j<0 ) {
         Aladin.warning(this,"Progressive survey (HiPS) unknown ["+survey+"]",1);
         return -1;
      }

      TreeNodeHips gSky = aladin.glu.getGluSky(j);

      try {
         if( defaultMode!=PlanBG.UNKNOWN ) gSky.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.hips(gSky,label,target,radius);
   }
   
   
   /** Génération initiale de l'arbre HiPS - et maj des menus Aladin correspondants */
   private void initTree() {
      if( mocServerLoading ) return;
      startTimer();
      (new Thread("initTree") {
         public void run() {
            try {
               loadRemoteTree();
               rebuildTree();
            } finally { postTreeProcess(); }
         }
      }).start();
   }
   
   /** (Re)construction de l'arbre des HiPS (après un ajout d'une description de HiPS par exemple) */
   protected void rebuildTree() {
      tree.freeTree();
      tree.populateTree(aladin.glu.vHips.elements());
      tree.defaultExpand();
      aladin.hipsReload();
   }
   
   protected boolean isDefaultExpand() { return tree.isDefaultExpand(); }
   
   /** Collapse l'arbre sauf le noeud courant */
   protected void collapseAllExceptCurrent() {
      if( isDefaultExpand() ) tree.allExpand();
      else tree.defaultExpand();
   }
   
   private boolean lastPrune = false;
   
   /** Regénération de l'arbre HiPS en fonction des flags "isIn()" de chaque noeud */
   protected void pruneTree() {
      boolean activated = prune.isActivated();
      if( lastPrune || activated ) tree.populateTree(aladin.glu.vHips.elements());
      tree.setInTree(tree.root);
      lastPrune = activated;
      if( !activated ) return;
      try { tree.elagueOut(); } finally { postTreeProcess(); }
   }
   
   /** Regénération de l'arbre HiPS en fonction des flags "isHidden()" de chaque noeud */
   protected void resumeTree(final String params) {
      if( mocServerLoading ) return;
     (new Thread("resumeTree") {
         public void run() {
            try {
               tree.populateTree(aladin.glu.vHips.elements());
               profileUpdate(params);
               pruneTree();
               // TODO: Faire un expand du root si nécessaire
            } finally { postTreeProcess(); }
         }
      }).start();
   }
   
   /** Retourne true s'il n'y a pas d'arbre HiPS */
   protected boolean isFree() {
      return tree==null || tree.root==null || tree.root.getChildCount()==0;
   }

   /** Traitement à applique après la génération ou la régénération de l'arbre HiPS */
   private void postTreeProcess() {
      
      boolean treeOk = !isFree();
      filter.setEnabled(treeOk);
      
      // Mise en route ou arrêt du thread de coloration de l'arbre en fonction des HiPS
      // présents ou non dans la vue courante
      if( treeOk ) startHipsUpdater();
      else stopHipsUpdater();
   }
   
   /** Chargement des descriptions de l'arbre par le MocServer */
   private void loadRemoteTree() { loadRemoteTree(MOCSERVER_PARAMS); }
   private void loadRemoteTree(String params) {
//      DataInputStream dis=null;
      InputStream in=null;

      // Recherche sur le site principal, et sinon dans le cache local
      try {
         mocServerLoading = true;

         Aladin.trace(3,"Loading HiPS Tree definitions...");
         
         String u = aladin.glu.getURL("MocServer", params, true).toString();
         try {
            in = aladin.cache.getWithBackup(u);

            // Peut être un site esclave actif ?
         } catch( Exception e) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            in = Util.openStream(u);
         }
//         dis = new DataInputStream(in);
//         aladin.glu.loadGluDic(dis,0,false,true,false,false);
         aladin.glu.loadProperties(in, false);
         aladin.glu.tri();

      }
      catch( Exception e1 ) {
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
      }
      finally {
         mocServerLoading=false;
//         if( dis!=null ) { try { dis.close(); } catch( Exception e) {} }
         if( in!=null ) { try { in.close(); } catch( Exception e) {} }
      }
   }
   
   /** Interroge le MocServer pour connaître les HiPS correspondants au filtre courant
    * Met à jour l'arbre en conséquence */
   private void profileUpdate(String params) {
      try {
         
         BufferedReader in=null;
         try {
            URL u = aladin.glu.getURL("MocServer", params, true);

            // récupération de chaque ID concernée (1 par ligne)
            HashSet<String> set = new HashSet<String>();
            
            Aladin.trace(4,"HipsMarket.profileUpdate: Contacting MocServer : "+u);
            try {
               String s;
               in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
               while( (s=in.readLine())!=null ) set.add( getId(s) );
            } catch( EOFException e ) { }

            // Nettoyage préalable de l'arbre
            for( TreeNodeHips hips : aladin.glu.vHips ) hips.setHidden(true);

            // Positionnement des datasets dans le champ
            for( TreeNodeHips hips : aladin.glu.vHips ) {
               if( hips.isLocal() ) continue;
               hips.setHidden( !set.contains(hips.internalId) );
            }

            // Mise à jour des branches de l'arbre
            DefaultMutableTreeNode root = tree.getRoot();
            tree.setHiddenTree(root);
            
            // Suppression des noeuds et branches cachés
            tree.elagueHidden();

            validate();
            repaint();

         } finally{ if( in!=null ) in.close(); }
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

   }

   
   // Dernier champs interrogé sur le MocServer
   private Coord oc=null;
   private double osize=-1;

   /** Interroge le MocServer pour connaître les HiPS disponibles dans le champ.
    * Met à jour l'arbre en conséquence */
   private void hipsUpdate() {
      if( mocServerLoading ) return; 
      try {
         BufferedReader in=null;
         try {
            
            ViewSimple v = aladin.view.getCurrentView();
            if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
               for( TreeNodeHips gSky : aladin.glu.vHips ) gSky.isIn=true;

            } else {
               String params;

               // Pour éviter de faire 2x la même chose
               Coord c = v.getCooCentre();
               double size = v.getTaille();
               if( c.equals(oc) && size==osize ) return;
               oc=c;
               osize=size;

               // Interrogation par cercle
               if( v.getTaille()>45 ) {
                  params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&RA="+c.al+"&DEC="+c.del+"&SR="+size*Math.sqrt(2);

                  // Interrogation par rectangle
               } else {
                  StringBuilder s1 = new StringBuilder("Polygon");
                  for( Coord c1: v.getCooCorners())  s1.append(" "+c1.al+" "+c1.del);
                  params = "client_application=AladinDesktop"+(aladin.BETA?"*":"")+"&hips_service_url=*&stc="+URLEncoder.encode(s1.toString());
               }

               URL u = aladin.glu.getURL("MocServer", params, true);
               
               Aladin.trace(4,"HipsMarket.hipsUpdate: Contacting MocServer : "+u);
               in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
               String s;

               // récupération de chaque ID concernée (1 par ligne)
               HashSet<String> set = new HashSet<String>();
               while( (s=in.readLine())!=null ) set.add( getId(s) );

               // Nettoyage préalable de l'arbre
               for( TreeNodeHips hips : aladin.glu.vHips ) hips.isIn=false;

               // Positionnement des datasets dans le champ
               for( TreeNodeHips hips : aladin.glu.vHips ) {
                  if( hips.isLocal() ) continue;   // On ne travaille pas sur les ajouts locaux
                  hips.isIn = set.contains(hips.internalId);
               }
            }

               // Mise à jour des branches de l'arbre
               DefaultMutableTreeNode root = tree.getRoot();
               pruneTree();
            
            validate();
            repaint();

         } finally{ if( in!=null ) in.close(); }
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

   }

   // Extraction de l'obs_id de l'IVOID pour rester compatible avec la nomenclature interne de l'arbre (TreeNodeAllsky.internalId)
//   private String getId(String ivoid) {
//      int start = ivoid.startsWith("ivo://") ? 6 : 0;
//      int offset = ivoid.indexOf("/",start);
//      int offset1 = ivoid.indexOf("?",start);
//      if( offset1>0 ) offset = Math.min(offset,offset1);
//      String id = ivoid.substring(offset+1);
//      return id;
//   }
//   

   private String getId(String ivoid) {
      if( ivoid.startsWith("ivo://") ) return ivoid.substring(6);
      return ivoid;
   }
   

   /** true si on peut raisonnablement faire un updating des HiPS visibles dans la vue */
   private boolean isReadyForUpdating() {
      return !mocServerLoading && isVisible() && getWidth()>0;
   }

   private int patience=0;
   
   Timer timer = null;
   
   public void startTimer() {
      if( timer==null ) {
         timer = new Timer(500, new ActionListener() {
            public void actionPerformed(ActionEvent e) { repaint(); }
         });
      }
      timer.start();
   }

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      
      g.setFont( Aladin.BOLD );
      
      // Petit message pour avertir l'utilisateur
      if( mocServerLoading ) {
         String dot="";
         for( int i = 0; i<patience; i++ ) dot+=".";
         patience++;
         if( patience>10 ) patience=0;
         g.setColor(Aladin.BKGD);
         String s = "Opening HiPS store "+dot;
         g.drawString(s, 10, 25);
      } else {
         if( timer!=null ) { timer.stop(); }
         g.setColor( Aladin.DARKBLUE );
         g.drawString("HiPS store",10,25);
      }
   }
   
   /************** Pour faire la maj en continue des HiPS visibles dans la vue *******************/
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
               if( isReadyForUpdating() ) hipsUpdate();
               Thread.currentThread().sleep(1000);
            } catch( Exception e ) { }
         }
         //         System.out.println("Hips updater stopped");
         threadUpdater=null;
      }
   }



}

