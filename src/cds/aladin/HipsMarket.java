package cds.aladin;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;

import cds.tools.Util;


/**
 * Classe qui gère l'arbre HiPS apparaissant à gauche de la vue
 * @version 1.0 décembre 2016 - création
 * @author Pierre Fernique [CDS]
 */
public class HipsMarket extends JPanel {
   
   private Aladin aladin;                  // Référence
   private HipsFilter hipsFilter=null;     // Formulaire de filtrage de l'arbre HiPS
   private boolean mocServerLoading=false; // true = requête de génération de l'arbre en cours (appel à MocServer)
   
   // Composantes de l'interface
   private MyTree tree;
   private JButton filter;
   private Elague prune;
   
   // Paramètres d'appel initial du MocServer (construction de l'arbre)
   private static String  MOCSERVER_PARAMS 
        = "client_application=AladinDesktop"+(Aladin.BETA && !Aladin.PROTO?"*":"")
         +"&hips_service_url=*&fmt=glu&get=record";
   
   public HipsMarket(Aladin aladin) {
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
      prune = new Elague(aladin);
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
      setBorder( BorderFactory.createEmptyBorder(25,0,5,0));

      // Actions sur le clic d'un noeud de l'arbre
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
      
      // Chargement de l'arbre initial
      initTree();
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

      TreeNodeAllsky gSky = aladin.glu.getGluSky(j);

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
      repaint();
      (new Thread("initTree") {
         public void run() {
            try {
               loadRemoteTree();
               tree.populateTree(aladin.glu.vGluSky.elements());
               tree.defaultExpand();
               aladin.gluSkyReload();
            } finally { postTreeProcess(); }
         }
      }).start();
   }
   
   private boolean lastPrune = false;
   
   /** Regénération de l'arbre HiPS en fonction des flags "isIn()" de chaque noeud */
   protected void pruneTree() {
      boolean activated = prune.isActivated();
      if( lastPrune || activated ) tree.populateTree(aladin.glu.vGluSky.elements());
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
               tree.populateTree(aladin.glu.vGluSky.elements());
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
      DataInputStream dis=null;

      // Recherche sur le site principal, et sinon dans le cache local
      try {
         mocServerLoading = true;

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
      catch( Exception e1 ) {
         if( Aladin.levelTrace>=3 ) e1.printStackTrace();
      }
      finally {
         mocServerLoading=false;
         if( dis!=null ) { try { dis.close(); } catch( Exception e) {} }
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
            for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) gSky.setHidden(true);

            // Positionnement des datasets dans le champ
            for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) {
               gSky.setHidden( !set.contains(gSky.internalId) );
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
               
               Aladin.trace(4,"HipsMarket.hipsUpdate: Contacting MocServer : "+u);
               in= new BufferedReader( new InputStreamReader( Util.openStream(u) ));
               String s;

               // récupération de chaque ID concernée (1 par ligne)
               HashSet<String> set = new HashSet<String>();
               while( (s=in.readLine())!=null ) set.add( getId(s) );

               // Nettoyage préalable de l'arbre
               for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) gSky.isIn=false;

               // Positionnement des datasets dans le champ
               for( TreeNodeAllsky gSky : aladin.glu.vGluSky ) {
                  gSky.isIn = set.contains(gSky.internalId);
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

   public void paintComponent(Graphics g) {
      super.paintComponent(g);
      
      // Petit message pour avertir l'utilisateur
      if( mocServerLoading ) {
         g.setColor(Color.red);
         String s = "Loading...";
         int x = getWidth()/2 - g.getFontMetrics().stringWidth(s)/2;
         g.drawString(s, x, 25);
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

