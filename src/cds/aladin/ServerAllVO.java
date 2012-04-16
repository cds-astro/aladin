// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//


package cds.aladin;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;

import javax.swing.*;

import cds.tools.Util;

/**
 * Le formulaire d'interrogation de toutes les ressources disponibles dans le VO
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : sept 04 - Creation
 */
public class ServerAllVO extends Server implements Runnable,MyListener {
   static final String BLANCS = "                                                               ";
   static final int MULTIPLEX_LIMIT = 8;
   String TITRE,STOP,MORE,CAT,IMG,SPEC,SERV,HSTOP,WAIT,WAIT1,WAIT2,ERR;

   Thread thread;
   JTextField radius;          // le champ de saisie du radius
   ButtonGroup listTree;
   JRadioButton b1,b2;
   JLabel step;
   protected FrameServer frameServer=null;

   protected void createChaine() {
      super.createChaine();
      aladinLabel   = aladin.chaine.getString("VONAME");
      description  = aladin.chaine.getString("VOINFO");
      TITRE = aladin.chaine.getString("VOTITLE");
      verboseDescr  = aladin.chaine.getString("VODESC");
      STOP  = aladin.chaine.getString("VOSTOP");
      HSTOP = aladin.chaine.getString("VOHSTOP");
      MORE  = aladin.chaine.getString("VOMORE");
      CAT   = aladin.chaine.getString("VOCAT");
      IMG   = aladin.chaine.getString("VOIMG");
      SPEC  = aladin.chaine.getString("VOSPECT");
      SERV  = aladin.chaine.getString("VOSERV");
      WAIT  = aladin.chaine.getString("VOWAIT");
      WAIT1 = aladin.chaine.getString("VOWAIT1");
      WAIT2 = aladin.chaine.getString("VOWAIT2");
      ERR   = aladin.chaine.getString("VOERR");
   }

   public ServerAllVO(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
      type = APPLI;
      aladinLogo = "VOLogo.gif";

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=2;

      // Le titre
      JPanel tp = new JPanel();
      tp.setBackground(Aladin.BLUE);
      Dimension d = makeTitle(tp,TITRE);
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);

      // JPanel pour la memorisationdu target (+bouton DRAG)
      JPanel tPanel = new JPanel();
      tPanel.setBackground(Aladin.BLUE);
      int h = makeTargetPanel(tPanel,0);
      tPanel.setBounds(0,y,XWIDTH,h); y+=h;
      add(tPanel);

      modeCoo=COO|SIMBAD;
      modeRad=RADIUS;

      // choix des types de données demandés
      JPanel dataTypePanel = new JPanel();
      dataTypePanel.setBackground(Aladin.BLUE);;
      dataTypePanel.setBounds(0,y-5,XWIDTH,30); y+=30;
      dataTypePanel.setLayout( new FlowLayout(FlowLayout.LEFT));
      dataTypePanel.setFont(Aladin.PLAIN);
      JLabel t = new JLabel(SERV);
      t.setFont(Aladin.BOLD);
      dataTypePanel.add(t);
      JCheckBox c;
      // on met les JCheckbox dans des panels pour que les couleurs de fond
      // soient visibles
      JPanel panelCbImg = new JPanel(new BorderLayout(0,0));
      panelCbImg.add(cbImg = c= new JCheckBox(IMG,true));
      cbImg.setOpaque(false);
      dataTypePanel.add(panelCbImg);
      c.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if( frameServer!=null && frameServer.isVisible() ) {
               frameServer.check(Server.IMAGE,   cbImg.isSelected());
            }
         }
      });
      JPanel panelCbCat = new JPanel(new BorderLayout(0,0));
      panelCbCat.add(cbCat = c=new JCheckBox(CAT,true));
      cbCat.setOpaque(false);
      dataTypePanel.add(panelCbCat);
      c.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if( frameServer!=null && frameServer.isVisible() ) {
               frameServer.check(Server.CATALOG,   cbCat.isSelected());
            }
         }
      });
      JPanel panelCbSpec = new JPanel(new BorderLayout(0,0));
      panelCbSpec.add(cbSpec = c= new JCheckBox(SPEC,true));
      cbSpec.setOpaque(false);
      dataTypePanel.add(panelCbSpec);
      c.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            if( frameServer!=null && frameServer.isVisible() ) {
               frameServer.check(Server.SPECTRUM,   cbSpec.isSelected());
            }
         }
      });
      // couleurs de fond pour les checkbox
      panelCbImg.setBackground(MetaDataTree.LABEL_COL[ResourceNode.IMAGE]);
      panelCbCat.setBackground(MetaDataTree.LABEL_COL[ResourceNode.CAT]);
      panelCbSpec.setBackground(MetaDataTree.LABEL_COL[ResourceNode.SPECTRUM]);

      JButton more = new JButton(MORE);
      more.addActionListener(this);
      more.setFont(Aladin.BOLD);
      more.setOpaque(false);
      dataTypePanel.add(more);
      add(dataTypePanel);

     // Deuxième indication
      step = new JLabel(BLANCS);
      step.setFont(Aladin.ITALIC);
      step.setBounds(15,y,310, 19);  y+=20;
      add(step);

      // L'arbre de Thomas
      ResourceNode root = new ResourceNode(aladin, "root");
      root.hide = true;
      root.isOpen = true;
      tree = new MetaDataTree(root,aladin,null);
      tree.setAllowSortByFields(false);
      tree.setFullExpandAtStart(false);
      tree.setColorLabel(true);
      tree.setStateChangedListener(this);
      JScrollPane scrollTree = new JScrollPane(tree);
      tree.setScroll(scrollTree);
      scrollTree.setBackground(tree.bkgColor);
      scrollTree.setBounds(XTAB1,y,XWIDTH,217); y+=215;
      add(scrollTree);

      // boutons radio pour choix AllVO/Aladin servers
      JPanel stopItPanel = new JPanel();
      stopItPanel.setBackground(Aladin.BLUE);
      stopItPanel.setBounds(50,y,XWIDTH,30); y+=35;
      stopItPanel.setLayout( new FlowLayout(FlowLayout.LEFT));
      stopItPanel.setFont(Aladin.PLAIN);
      stopItPanel.add( new JLabel(HSTOP));
      stopItPanel.add( stop=new JButton(STOP) );
      stop.addActionListener(this);
      stop.setEnabled(false);
      stop.setOpaque(false);
      add(stopItPanel);

      setMaxComp(scrollTree);
   }

//   private JRadioButton VOServers,AladinServers;
   private JCheckBox cbImg,cbCat,cbSpec;
   private JButton stop;

   private boolean olistView=true;

   /** change le mode d evisualisation : arbre ou a plat */
   private void changeViewMode() {
       boolean listView = listTree.getSelection().getActionCommand().indexOf("list")>=0;

       if( olistView!=listView ) {
          tree.setFlat(listView);
          olistView=listView;
       }
   }

   /** Implémentation de l'interface MyListener */
   public void fireStateChange(String state) {
       if( state.equals(MetaDataTree.FLAT_VIEW) ) {
       	   if( b1==null ) return;
           b1.setSelected(true);
//           listTree.setSelected(b1.getModel(), true);
           changeViewMode();
       }
       else if( state.equals(MetaDataTree.HIER_VIEW) ) {
       	   if( b2==null ) return;
           b2.setSelected(true);
//           listTree.setSelected(b2.getModel(), true);
           changeViewMode();
       }
   }

   public void fireStateChange(int i) {}

  static String otarget=null;
  boolean flagClear=true;	// Une horreur pour eviter un clear()
                                // intempestif lors d'un reset

  /** Memorisation du target de la derniere interrogation aladin */
  protected void memoTarget(String s) { otarget=s; }

  /** Pre-remplissage du champ target
   * Si il ne s'agit pas de la meme chaine que la derneire intero
   * le formulaire est resete (pour effacer les images disponibles qui ne
   * correspondront plus)
   * @param s La chaine a mettre dans le champ target
   */
   protected void setTarget(String s) {
//      System.out.println("setTarget("+s+")");
      if( flagClear && otarget!=null && !otarget.equals(s) ) {
//return;
// 9 oct - JE NE COMPRENDS PAS POURQUOI J'AVAIS COMMENTE CE CLEAR. JE LE REMETS
         clear();
      }
      flagClear=true;
      super.setTarget(s);
   }

  /** Reset du formulaire */
   protected void reset() {
      tree.resetCb();
      flagClear=false;
      super.reset();
   }

  /** Clear du formulaire - en 2 coups */
   protected void clear() {
      seeJeton();
      boolean keepTarget = tree.isEmpty();
      tree.clear();
      stop.setEnabled(false);
      String t = target.getText();
      super.clear();
      if( keepTarget ) target.setText(t);
   }

   private boolean flagMultiplex = false;
   private boolean flagCreatPlane = false;

   public void run() {
      if( !loadIVOAdic() ) {
         defaultCursor();
         ball.setMode(Ball.HS);
         return;
      }
      if( flagMultiplex ) { flagMultiplex=false; multiplex(); }
      else if( flagCreatPlane ) { flagCreatPlane=false; creatPlaneThread(); }
      else if( lockContact ) contact();
      finMultiplex();
   }

   /** Effacement du texte indiquant l'étape courante */
   protected void clearStepLabel() { step.setText(""); }

   /** Chargement du dico GLU décrivant les resources IVOA additionnelles */
   protected boolean loadIVOAdic() {
      if( aladin.dialog.ivoaServersLoaded ) return true ;
      step.setText(WAIT);
      return aladin.dialog.appendIVOAServer();
   }

   // Recherche des meilleurs parametres en fonction des criteres
   // Dans un thread separe pour rendre assez vite la main
   private void creatPlaneThread() {
      aladin.info("Un peu de patience, c'est pas encore implémenté");
   }

  /** Interrogation */
   public void submit() {

      // Recuperation et memorisation du target
      String obj = getTarget();
      if( obj==null ) return;
      memoTarget(obj);

      // Traitement des images par lot
      if( tree!=null && !tree.isEmpty() ) {
         if( tree.nbSelected()>0 ) {
            tree.loadSelected();
            tree.resetCb();
         } else {
            // Si aucune ligne n'a ete cochee et si la Frameinfo est ouverte, je charge
            // l'image de cette derniere
            FrameInfo fi = aladin.getFrameInfo();
            if(  fi.isVisible() ) fi.load();
            else Aladin.warning(this,WNEEDCHECK);
         }
         return;
      }

      // Je cache la FrameInfo pour ne pas la prendre a tord par defaut
      // si l'utilisateur clique sur SUBMIT sans avoir coche qq chose
      // (Tu me suis ?) --> (ca va !)
      FrameInfo fi = aladin.getFrameInfo();
      if( fi.isVisible() ) fi.setVisible(false);

      // Chargement des descriptions des images disponibles (threade)
      thread= new Thread(this,"AladinDiscoveryServer");
      flagMultiplex=true;
      Util.decreasePriority(Thread.currentThread(), thread);
      thread.start();
   }

   public void actionPerformed(ActionEvent e) {
      Object s = e.getSource();

      if( s instanceof JRadioButton ) {
         JRadioButton cb = (JRadioButton)s;
         if( cb.equals(b1) || cb.equals(b2) ) changeViewMode();
         return;
      }

      if( s instanceof JButton
            && ((JButton)s).getActionCommand().equals(MORE)) {
         loadIVOAdic();
         if( frameServer==null ) frameServer = new FrameServer(aladin,this);
         else frameServer.setVisible(true);
         frameServer.check(Server.IMAGE,   cbImg.isSelected());
         frameServer.check(Server.CATALOG, cbCat.isSelected());
         frameServer.check(Server.SPECTRUM,cbSpec.isSelected());
      }

      else if( s instanceof JButton
            && ((JButton)s).getActionCommand().equals(STOP)) stopMultiplex();

      super.actionPerformed(e);
   }

   private Vector jeton;
   private Server memoServer;
   private String memoTarget;
   private String memoRadius;
   private boolean stopMultiplex;

   protected void stopMultiplex() {
      stop.setEnabled(false);

//System.out.println("Arrêt du multiplexage !!");

      // On attend la mort du multiplexeur
      stopMultiplex=true;
      Util.pause(2000);

      // On tue tous les threads qui sont en train de contacter des serveurs
      // Méthode non recommandée mais je ne vois
      // pas comment arrêter le thread proprement s'il est bloqué sur le socket
      Enumeration e = jeton.elements();
      while( e.hasMoreElements() ) {
         Thread t = (Thread)e.nextElement();
//         System.out.println("J'interromps "+t);
//         t.stop();
         jeton=null;
         step.setText(BLANCS);
         defaultCursor();
      }
      ball.setMode(Ball.PARTIAL);
   }

   private void finMultiplex() {
      if( jeton==null ) return;
      int n;
      if( (n=jeton.size())==0 ) {
         defaultCursor();
         step.setText(BLANCS);
      } else step.setText(WAIT1+" "+n+" "+WAIT2+(n>1?"s":"")+"...");
   }

   private Hashtable infoJeton;
   
   /** On ne change pas la frame en ICRS, ce sera fait plus tard */
   protected String getTarget(boolean confirm) {
      if( target==null ) return null;
      String s = target.getText().trim();
      if( confirm && s.length()==0 ) {
         if( ball!=null ) ball.setMode(Ball.NOK);
         Aladin.warning(this,WNEEDOBJ);
         return null;
      }
      return s;
   }

   protected void multiplex() {
      stop.setEnabled(true);
      jeton=new Vector(10);
      infoJeton = new Hashtable(10);

      jeton = new Vector(10);
      stopMultiplex = false;

      try {
         String target = getTarget();
         String radius = this.getRadius();
         
         int j=0;

         // Balayage de tous les serveurs interrogeables en mode discovery
         for( int i=0; i<aladin.dialog.server.length; i++ ) {
            Server server = aladin.dialog.server[i];
            if( !server.isDiscovery() ) continue;
            if( !server.isAllVOChecked() ) continue;
            if( frameServer==null || !frameServer.isVisible() ) {
               if( !cbImg.isSelected() && server.type==IMAGE ) continue;
               if( !cbCat.isSelected() && server.type==CATALOG ) continue;
               if( !cbSpec.isSelected() && server.type==SPECTRUM ) continue;
            }
            while( jeton.size()>MULTIPLEX_LIMIT && !stopMultiplex ) {
//               System.out.println("Multiplexeur attend un jeton");
               Util.pause(1000);
            }
            // Demande d'arrêt du multiplexeur
            if( stopMultiplex ) break;

            thread = new Thread(this,"AladinDiscovery"+j);
            Util.decreasePriority(Thread.currentThread(), thread);
//            thread.setPriority( Thread.NORM_PRIORITY -1);
            jeton.addElement(thread);
            infoJeton.put(thread,server.aladinLabel);
            step.setText("Querying "+server.aladinLabel+"...");
            while( !getLockContact() ) Util.pause(100);
//seeJeton();
            memoServer=server;
            memoTarget=target;
            memoRadius=radius;
            j++;
//System.out.println("Contact "+j+"ieme serveur...");
            thread.start();
         }

      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         Aladin.warning(this,ERR,1);
         aladin.log("Error","Discovery tool error.submit()");
      }

System.out.println("C'est terminé pour le multiplexeur");
//seeJeton();
      finMultiplex();
      defaultCursor();
   }

   private void seeJeton() {
      if( jeton==null ) return;
      Enumeration e = jeton.elements();
      int i=0;
      while( e.hasMoreElements() ) {
         Thread t = (Thread)e.nextElement();
         System.out.println("   "+i+":"+t+" "+(String)infoJeton.get(t));
         i++;
      }
   }

   private boolean lockContact=false;
   synchronized private void unlockContact() { lockContact=false; }
   synchronized private boolean getLockContact() {
      if( lockContact ) return false;  // Deja pris
      lockContact=true;
      return true;
   }

//   public void stop() {
//      Thread t = Thread.currentThread();
//System.out.println("Le Thread "+t+" va être tué");
//      t.stop();
//   }

   private MyInputStream inContact;


   protected void contact() {
      Server server = memoServer;
      String target = memoTarget;
      String radius = memoRadius;
      StringBuffer infoUrl = new StringBuffer();
      String error=null;
      unlockContact();
//System.out.println("contact serveur : "+server.getTitle());
      waitCursor();
      try {
         server.setStatusAllVO(Server.STATUS_QUERYING,infoUrl+"");
//         if( server instanceof ServerGlu ) aladin.console.setCommand("get "+((ServerGlu)server).actionName+" "+target);
         inContact = server.getMetaData(target,radius,infoUrl);
         if( inContact==null || !updateMetaData(inContact,server,target,null,true)) {
            throw new Exception("NORESULT");
         }
      } catch( Exception e ) {
           error=e.getMessage();
//         System.out.println(Thread.currentThread()+" Exception ("+e+")");
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }
      Thread t = Thread.currentThread();

      // Gestion du log
      if( error==null ) server.setStatusAllVO(Server.STATUS_OK,infoUrl+"");
      else if( error.equals("NORESULT") ) server.setStatusAllVO(Server.STATUS_NORESULT,infoUrl+"");
      else {
         server.setStatusAllVO(Server.STATUS_ERROR,infoUrl+"");
         server.statusError=error;
      }

//System.out.println("Le Thread "+t+" se termine ("+(String)infoJeton.get(t)+")");
      if( jeton!=null ) jeton.removeElement(Thread.currentThread());
      infoJeton.remove(t);
      finMultiplex();
   }

}
