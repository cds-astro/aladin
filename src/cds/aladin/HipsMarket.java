package cds.aladin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;

import cds.aladin.MyTree.NoeudEditor;
import cds.tools.Util;


public class HipsMarket extends JPanel {
   MyTree tree;
   Aladin aladin;
   HipsFilter hipsFilter=null;
   
   public HipsMarket(Aladin aladin) {
      this.aladin = aladin;
      tree = new MyTree(aladin);
      tree.setBackground(getBackground());
      JScrollPane scrollTree = new JScrollPane(tree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setBorder( BorderFactory.createEmptyBorder(0,0,0,0));
      
      JButton profiler = new JButton("Preferences");
      profiler.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            filtre();
         }
      });
      JPanel control = new JPanel(new FlowLayout());
      control.add(profiler);
      
      setLayout(new BorderLayout() );
      add(scrollTree,BorderLayout.CENTER);
      add(control,BorderLayout.SOUTH);
      setBorder( BorderFactory.createEmptyBorder(20,0,5,0));

      tree.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent e) {
            System.out.println("mouseClicked");
            if (e.getClickCount() == 2) {
               DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                     tree.getLastSelectedPathComponent();
               if (node == null) return;
               TreeNodeAllsky nodeInfo = (TreeNodeAllsky) node.getUserObject();
               nodeInfo.submit();
            }
         }
      });
      
      initTree();
   }
   
   private void filtre() {
      System.out.println("Je dois filtrer le HiPS tree...");
      if( hipsFilter==null ) hipsFilter = new HipsFilter(aladin);
      hipsFilter.show();
   }
   
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

      TreeNodeAllsky gSky = aladin.glu.getGluSky(j);

      try {
         if( defaultMode!=PlanBG.UNKNOWN ) gSky.setDefaultMode(defaultMode);
      } catch( Exception e ) {
         aladin.command.printConsole("!!! "+e.getMessage());
      }

      return aladin.hips(gSky,label,target,radius);
   }
   
   protected void resumeTree(final String params) {
      
      // On cache tous les noeuds déjà chargés
      Enumeration<TreeNodeAllsky> e = aladin.glu.vGluSky.elements();
      while( e.hasMoreElements() ) e.nextElement().setHidden(true);
      
      // Pour le moment on récupère la nouvelle liste
      // Par la suite on pourrait juste cacher les noeuds qui ne sont plus retenus
      (new Thread("resumeTree") {
         public void run() {
            loadRemoteTree(params);
            tree.freeTree();
            tree.populateTree(aladin.glu.vGluSky.elements());
         }
      }).start();
   }


   private boolean dynTree=false;
   protected void initTree() {
      if( dynTree ) return;
      (new Thread("initTree") {
         public void run() {
            loadRemoteTree();
            tree.populateTree(aladin.glu.vGluSky.elements());
            aladin.gluSkyReload();
            show();
         }
      }).start();
   }
   
   private static String  DEFAULT_PARAMS = "client_application=AladinDesktop"+(Aladin.BETA?"*":"")
                                          +"&hips_service_url=*&fmt=glu&get=record";

   /** Chargement des descriptions de l'arbre */
   protected void loadRemoteTree() { loadRemoteTree(DEFAULT_PARAMS); }
   protected void loadRemoteTree(String params) {
      if( dynTree ) return;
      DataInputStream dis=null;

      // Recherche sur le site principal, et sinon dans le cache local
      try {
         dynTree=true;
         Aladin.trace(3,"Loading HiPS Tree definitions...");
         
         String u = aladin.glu.getURL("MocServer", params, true).toString();
         InputStream in;
         try {
            in = aladin.cache.getWithBackup(u);

            // Peut être un site esclave actif ?
         } catch( Exception e) {
            if( !aladin.glu.checkIndirection("MocServer", null) ) throw e;
            u = aladin.glu.getURL("MocServer", params, true).toString();
            in = Util.openStream(u);
         }
         dis = new DataInputStream(in);
         aladin.glu.loadGluDic(dis,0,false,true,false,false);
         aladin.glu.tri();

      }
      catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }
      finally {
         dynTree=false;
         if( dis!=null ) { try { dis.close(); } catch( Exception e) {} }
      }
   }
   
   // Dernier champs interrogé sur le MocServer
   private Coord oc=null;
   private double osize=-1;

   /** Interroge le MocServer pour connaître les HiPS disponibles dans le champ.
    * Met à jour l'arbre en conséquence
    */
   protected void hipsUpdate() {
      try {
         
         BufferedReader in=null;
         try {
            
//            if( flagElague ) ((DefaultTreeModel)tree.getModel()).reload();

            ViewSimple v = aladin.view.getCurrentView();
//            if( v.isFree() ) return;

            if( v.isFree() || v.isAllSky() || !Projection.isOk(v.getProj()) ) {
               for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) gSky.isIn=true;

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
               
               Aladin.trace(4,"ServerHips.hipsUpdate: Contacting MocServer : "+u);
               in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
               String s;

               // récupération de chaque IVORN concernée (1 par ligne)
               HashSet<String> set = new HashSet<String>();
               while( (s=in.readLine())!=null ) set.add( getId(s) );

               // Nettoyage préalable de l'arbre
               for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) gSky.isIn=false;

               // Positionnement des datasets dans le champ
               for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) {
                  gSky.isIn = set.contains(gSky.internalId);
//                  if( !gSky.ok ) System.out.println(gSky.internalId+" is out");
               }
            }

            // Mise à jour de la cellule de l'arbre en cours d'édition
            try {
               NoeudEditor c = (NoeudEditor)tree.getCellEditor();
               if( c!=null ) {
                  TreeNode n = (TreeNode)c.getCellEditorValue();
                  if( n!=null &&  n.hasCheckBox() ) {
                     if( n.isIn() ) n.checkbox.setForeground(Color.black);
                     else n.checkbox.setForeground(Color.lightGray);
                  }
               }
            } catch( Exception e ) {
               if( Aladin.levelTrace>=3 ) e.printStackTrace();
            }
            
            // Elagage éventuel
//            if( flagElague ) tree.elague();
//            else {

               // Mise à jour des branches de l'arbre
               DefaultMutableTreeNode root = tree.getRoot();
               tree.setOkTree(root);
//            }
            
            validate();
            repaint();

         } finally{ if( in!=null ) in.close(); }
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }

   }

   // Extraction de l'obs_id de l'IVOID pour rester compatible avec la nomenclature interne de l'arbre (TreeNodeAllsky.internalId)
   private String getId(String ivoid) {
      int start = ivoid.startsWith("ivo://") ? 6 : 0;
      int offset = ivoid.indexOf("/",start);
      int offset1 = ivoid.indexOf("?",start);
      if( offset1>0 ) offset = Math.min(offset,offset1);
      String id = ivoid.substring(offset+1);
      return id;
   }
   
   public void show() {
      super.show();
      startHipsUpdater();
   }

   public void hide() {
      stopHipsUpdater();
      super.hide();
   }

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
               if( isOpened() ) hipsUpdate();
               Thread.currentThread().sleep(1000);
            } catch( Exception e ) { }
         }
         //         System.out.println("Hips updater stopped");
         threadUpdater=null;
      }
   }

   private boolean isOpened() {
//      Window window = SwingUtilities.windowForComponent(this);
      return isVisible();
   }




}

