// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.aladin;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Event;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;

import cds.tools.Util;

/**
 * Gestion de l'interface de choix du serveur de donnees ou d'images
 *
 * @author Pierre Fernique [CDS]
 * @version 1.3 : (5 décembre 05) Tri des serveurs pour que les Popups soient en fin de liste
 * @version 1.2 : (14 sep 00) Ajout du serveur de champs de vue
 * @version 1.1 : (23 nov 99) Chargement des archives, des surveys et des mots
 *          cles par VizieR
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class ServerDialog extends JFrame
implements SwingWidgetFinder, Runnable, ActionListener,
DropTargetListener, DragSourceListener, DragGestureListener, GrabItFrame {
   static final int MAXSERVER = 10;

   // Les indices des serveurs
   static int ALADIN = 0;
   static int SIMBAD = 0;
   static int NED = 0;
   static int VIZIER = 0;
   static int FIELD = 0;
   static int LOCAL = 0;

   static final String VO_RESOURCES_BY_PLASTIC = "VO res.";

   static final String DEFAULTTAILLE = "14";
   protected String defaultTarget="";            // Le target par defaut
   protected String defaultTaille=DEFAULTTAILLE; // La taille par defaut (rayon arcmin)
   protected String defaultDate;                 // La date par défaut

   // Les chaines
   String TITLE,/*HISTORY,*/SUBMIT,RESET,CLEAR,HELP,CLOSE,IMG,CAT,OTHER,MESSAGE,
   TIPRESET,TIPCLEAR,TIPSUBMIT,TIPCLOSE,GENERICERROR;

   // Les composantes de l'objet
   Server[] server;
   JPanel mp; // le panel multiple des formulaires
   CardLayout card; // et son layout associe
   SelectDialog selectDialog; // L'entourage du panel multiple des formulaires
   Label status; // Le status
   MyButton buttons[]; // Liste des boutons

   // popup d'accès aux serveurs ajoutés par l'utilisateur via PLASTIC
   ServerFolder voResPopup;
   
   TapManager tapManager = null;

   // Les infos a memoriser
   int current = 0; // le formulaire courant
   int bcurrent = 0; // le bouton courant

   protected boolean flagSetPos = false; // true si la position de la fenêtre a
   // déjà été calculée

   // pour robot
   Server curServer, localServer, vizierServer,/* vizierArchives,vizierSurveys,
   vizierBestof,discoveryServer, */aladinServer, fovServer, almaFovServer, vizierSED, /* hipsServer, */tapServer;
   JButton submit;

   // Les references aux autres objets
   Aladin aladin;
   Calque calque;

   /** Retourne le target par défaut en J2000 */
   protected String getDefaultTargetJ2000(){
      return defaultTarget.trim();
   }

   /** Retourne le target par défaut dans le frame courant */
   protected String getDefaultTarget(){
      return aladin.localisation.getFrameCoord(defaultTarget.trim());
   }

   /** Retourne la taille par défaut */
   protected String getDefaultTaille(){ return defaultTaille.trim(); }

   /** Retourne la date par défaut */
   protected String getDefaultDate() { return defaultDate; }

   /** Mémorisation de la dernière date  */
   protected void setDefaultDate(String s) { defaultDate=s; }

   /** Memorisation du dernier target saisie par la saisie rapide */
   protected void setDefaultTarget(String s) {
      if( s.equals(" --   --") ) return;
      //       System.out.println("setDefaultTarget("+s+")");
      defaultTarget=aladin.localisation.getICRSCoord(s);
   }

   /** Memorisation de la taille du plan passé en paramètre */
   protected void setDefaultTaille(Plan p) {
      if( !Projection.isOk(p.projd) ) return;
      double rm = Math.max(p.projd.rm,p.projd.rm1);
      defaultTaille = (rm>0. ? 1.4142*(rm/2)+"" : DEFAULTTAILLE);
      //	   System.out.println("SetDefaultTaille => "+defaultTaille);
   }

   /** Memorisation de la dernière taille par la saisie rapide */
   protected void setDefaultTaille(String s) {
      if( s==null ) s=DEFAULTTAILLE;
      //	   System.out.println("SetDefaultTaille("+s+")");
      defaultTaille=s;
   }

   /**
    * Ajoute au Vecteur sv les serveurs decrits par le GLU (Glu.vGluServer). Le
    * serveur peut se trouver dans un sous-menu Popup (myPopup!=null) ou avoir
    * un bouton a part entiere
    * @param type Server.IMAGE, Server.CATALOG, Server.OTHERS suivant que l'on traite les
    *           serveurs Images, Donnees, ou applications distantes
    */
   private void addGluServer(Vector<Server> sv, Vector<Server> sourceServer, int type) {
      //      Enumeration e = Glu.vGluServer.elements();
      Server sTmp;
      int i;

      for( int j = sourceServer.size() - 1; j >= 0; j-- ) {
         sTmp = sourceServer.elementAt(j);
         if( (sTmp.type & type) ==0 ) continue;

         // Correction du bug multi-instances d'Aladin dans le cas de l'applet
         // Les fomulaires GLU faisaient référence à la première instance d'Aladin
         //         sTmp.aladin = aladin;

         sv.addElement(sTmp);

         // Niveau bouton
         if( sTmp.aladinMenu != null && !sTmp.isHidden() ) {

            // Est-ce que ce Popup existe deja
            for( i = sv.size() - 1; i >= 0; i-- ) {
               Server s = sv.elementAt(i);
               if( s.aladinLabel.equals(sTmp.aladinMenu) && s instanceof ServerFolder ) break;
            }

            // S'il n'existe pas encore, on le cree
            if( i < 0 ) {
               i = sv.size(); // Indice de son emplacement
               sv.addElement(new ServerFolder(aladin, sTmp.aladinMenu, i,
                     sTmp.type == Server.IMAGE ? ServerFolder.LEFT :
                        sTmp.type == Server.CATALOG || sTmp.type == Server.MOC ? ServerFolder.RIGHT : ServerFolder.TOP ));
            }
            ((ServerFolder) sv.elementAt(i)).addItem(sTmp.aladinLabel);
         }
      }
   }

   /** Retourne l'indice du formulaire du dernier Serveur GLU chargé
    * => afin de pouvoir le rendre visible immédiatement le cas échéant */
   protected int getLastGluServerIndice() {
      return findIndiceServer(aladin.glu.lastGluServer);
   }
   
   protected int getTapServerIndex() {
	      return findIndiceServer(tapServer);
   }


   /**
    * Tri des Servers afin que les Popup se situent en fin de liste, Popup-DSS
    * en début de liste des Folders et les Popup-Other tout à la fin.
    * Les servers qui font partie d'un popup vont être trié en toute fin de liste
    * en respectant le même ordre que leur popup respectif
    * @param sv la liste des servers à trier
    * @return la liste triée
    */
   private Vector triServer(Vector sv) {

      // En mode Outreach, simple Tri sur les noms
      //      if( Aladin.OUTREACH ) {
      //         Collections.sort(sv,new Comparator() {
      //
      //            public int compare(Object arg0, Object arg1) {
      //               Server v0 = (Server)arg0;
      //               Server v1 = (Server)arg1;
      //               if( v0.type!=Server.IMAGE || v1.type!=Server.IMAGE ) return 0;
      //               return v0.nom.compareTo(v1.nom);
      //            }
      //
      //         });
      //         return sv;
      //      }

      Vector v = new Vector(sv.size());
      Vector vFirst = new Vector(10);
      Vector vPop = new Vector(10);
      Vector vOther = new Vector(10);
      Vector vEnd1 = new Vector(10);
      Vector vEnd2 = new Vector(10);
      Vector vEnd3 = new Vector(10);

      // Je sépare les serveurs simples,
      // les MyPopup-DSS, les MyPopup et les MyPopup-Other
      Enumeration e = sv.elements();
      while( e.hasMoreElements() ) {
         Server s = (Server) e.nextElement();
         if( s instanceof ServerFolder ) {
            if( s.aladinLabel.startsWith("Other") ) vOther.addElement(s);
            else if( s.aladinLabel.startsWith("Optical") || s.aladinLabel.startsWith("DSS") ) vFirst.addElement(s);
            else vPop.addElement(s);
         } else {
            if( s.aladinMenu!=null ) {
               if( s.aladinMenu.startsWith("Other")) vEnd2.addElement(s);
               else if( s.aladinMenu.startsWith("Optical")) vEnd3.addElement(s);
               else vEnd1.addElement(s);
            } else v.addElement(s);
         }
      }

      // Je place en fin de liste les MyPopup, puis les MyPopup-Other
      // sans oublier d'ajuster le champ "numButton" du MyPopup afin que la
      // position
      // du Popup corresponde à la position du bouton dans le formulaire des
      // serveurs
      int j = v.size();
      for( int i = 0; i < 6; i++ ) {
         e = i == 0 ? vFirst.elements() : i==1 ? vPop.elements() : i==2 ? vOther.elements() :
            i==4 ? vEnd1.elements() : i==5 ? vEnd2.elements() : vEnd3.elements();
            while( e.hasMoreElements() ) {
               Server s = (Server) e.nextElement();
               if( s instanceof ServerFolder ) ((ServerFolder)s).numButton = j++;
               v.addElement(s);
            }
      }

      return v;
   }

   private final JPanel buttonImg;

   /** Retourne la taille de la marge de gauche (prise par les boutons des images */
   protected int getMargeGauche() { return buttonImg.size().width; }

   protected void createChaine() {
      TITLE   = aladin.chaine.getString("SERVERTITLE");
      //      HISTORY = aladin.chaine.getString("HISTORY");
      SUBMIT  = aladin.chaine.getString("SUBMIT");
      RESET   = aladin.chaine.getString("RESET");
      CLEAR   = aladin.chaine.getString("CLEAR");
      CLOSE   = aladin.chaine.getString("CLOSE");
      IMG     = aladin.chaine.getString("IMG");
      CAT     = aladin.chaine.getString("CAT");
      OTHER   = aladin.chaine.getString("OTHER");
      MESSAGE = aladin.chaine.getString("SSHELP");
      TIPRESET = aladin.chaine.getString("TIPRESET");
      TIPCLEAR = aladin.chaine.getString("TIPCLEAR");
      TIPSUBMIT = aladin.chaine.getString("TIPSUBMIT");
      TIPCLOSE = aladin.chaine.getString("TIPCLOSE");
      GENERICERROR = Aladin.getChaine().getString("GENERICERROR");
   }


   long t1,t;
   /**
    * Creation de l'interface et de tous les formulaires necessaires a l'acces
    * aux bases
    * @param aladin Reference
    */
   protected ServerDialog(Aladin aladin) {
      this.aladin = aladin;
      Aladin.setIcon(this);

      int i;
      Vector<Server> sv = new Vector<>(100); // Temporaire pour la creation de serveur[]
      JPanel actions = new JPanel();
      createChaine();
      setTitle(TITLE);
      calque = aladin.calque;

      setFont(Aladin.BOLD);
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);

      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { trapESC(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );
      getRootPane().registerKeyboardAction(new ActionListener() {
         public void actionPerformed(ActionEvent e) { trapESC(); }
      },
      KeyStroke.getKeyStroke(KeyEvent.VK_W,Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()),
      JComponent.WHEN_IN_FOCUSED_WINDOW
            );

      JLabel status = new JLabel(""); status.setFont( Aladin.LITALIC );
      status.setForeground( Color.blue );

      // Les serveurs Images par programme
//      if( Aladin.NETWORK ) { 
//         aladinServer = new ServerAladin(aladin);
//         if( Aladin.OLD ) sv.addElement(aladinServer);
//      }

      // Les serveurs Images via GLU
      if( Aladin.NETWORK ) addGluServer(sv, Glu.vGluServer, Server.IMAGE);


      // Les serveurs Data par programme
      if( Aladin.NETWORK ) {
         Server svizier;
         vizierServer = svizier = new ServerVizieR(aladin, this, actions);
//         if( !Aladin.OUTREACH ) {
            sv.addElement(svizier);
            if( !Aladin.OLD ) svizier.HIDDEN=true; 
            
            
//               sv.addElement(vizierSurveys = new ServerVizieRSurvey(aladin,
//                     ((ServerVizieR) svizier).vSurveys));
//               sv.addElement(vizierArchives = new ServerVizieRMission(aladin,
//                     ((ServerVizieR) svizier).vArchives));
            
            if( aladin.glu.get("Simbad")==null ) sv.addElement(new ServerSimbad(aladin));
//            if( aladin.glu.get("Ned")==null ) sv.addElement(new ServerNED(aladin));
            
//               sv.addElement(new ServerSWarp(aladin));
//               sv.addElement(new ServerMocQuery(aladin));
//            if( Aladin.PROTO) sv.addElement(new ServerXmatch(aladin));
            
            this.tapManager = TapManager.getInstance(aladin);
        	
        	tapServer = new ServerTap(aladin);
			sv.addElement(tapServer);
			
			/*if (aladin.glu.lastTapGluServer != null) {//drag drop of new server take priority
        	tapServer = aladin.glu.lastTapGluServer;
        	tapServer.HIDDEN = false;
        	if( !tapServer.isVisible() ) tapServer.setVisible(true);
			} else{//initial aladin load
				tapServer = new ServerTap(aladin);
				sv.addElement(tapServer);
			}*/
            
//         } else {
//            sv.addElement(new ServerSimbad(aladin));
////            sv.addElement(vizierSurveys = new ServerVizieRSurvey(aladin,
////                  ((ServerVizieR) svizier).vSurveys));
////            sv.addElement(vizierArchives = new ServerVizieRMission(aladin,
////                  ((ServerVizieR) svizier).vArchives));
////            sv.addElement(svizier);
//         }
      }

       // Les serveurs Catalog via GLU
      if( Aladin.NETWORK ) addGluServer(sv, Glu.vGluServer, Server.CATALOG | Server.MOC);

      // Tri des serveurs pour mettre ceux qui sont sous "others" en fin de
      // liste
      sv = triServer(sv);

     // L'arbre des HiPS
//      sv.addElement(hipsServer = new ServerHips(aladin));

      // L'acces local/url
      sv.addElement(localServer = new ServerFile(aladin));

      // Juste pour savoir s'il y a un discoveryServer
//      discoveryServer = null;
//
//      // Le mode discovery
//         discoveryServer = new ServerAllVO(aladin);
//         sv.addElement(discoveryServer);

      // L'arbre des catégories
//      sv.addElement(new ServerWatch(aladin));

      // Les serveurs Spectra via GLU
      if( Aladin.NETWORK ) addGluServer(sv, Glu.vGluServer, Server.SPECTRUM);

      // Les FoV
      sv.addElement(fovServer = new ServerFoV(aladin));
      int n = sv.size();
      ServerFolder fovFolder = new ServerFolder(aladin, fovServer.aladinMenu, n, ServerFolder.TOP );
      sv.addElement( fovFolder );
      fovFolder.addItem(fovServer.aladinLabel);

      sv.addElement(almaFovServer = new ServerAlmaFootprint(aladin));
      fovFolder.addItem(almaFovServer.aladinLabel);

      // Les serveurs d'application via GLU
      if( Aladin.NETWORK ) addGluServer(sv, Glu.vGluServer, Server.APPLI | Server.APPLIIMG);

      // Serveurs obtenus via PLASTIC
      //      if( !Aladin.OUTREACH && Aladin.PROTO && Aladin.PLASTIC_SUPPORT ) {
      //         sv.addElement(voResPopup = new ServerFolder(aladin, VO_RESOURCES_BY_PLASTIC, sv.size()-1, ServerFolder.TOP));
      //      }

      // Construction des panel des boutons
      JPanel buttonTop = new JPanel();
      buttonImg = new JPanel();
      JPanel buttonData = new JPanel();
      buttonTop.setOpaque(false);
      buttonImg.setOpaque(false);
      buttonData.setOpaque(false);

      GridBagLayout gbtop = new GridBagLayout();
      GridBagConstraints gctop = new GridBagConstraints();
      buttonTop.setLayout(gbtop);

      GridBagLayout gbimg = new GridBagLayout();
      GridBagConstraints gcimg = new GridBagConstraints();
      gcimg.gridx = GridBagConstraints.REMAINDER;
      gcimg.weighty = 0.;
      buttonImg.setLayout(gbimg);

      GridBagLayout gbdat = new GridBagLayout();
      GridBagConstraints gcdat = new GridBagConstraints();
      gcdat.gridx = GridBagConstraints.REMAINDER;
      buttonData.setLayout(gbdat);

      gcimg.insets.bottom=6;
      gcimg.insets.top=12;
      JLabel l,limg,ldat;
      limg = l = new JLabel(Util.fold("<center><i>"+IMG+"</i></center>",80,true));
      l.setFont(Aladin.BOLD);
      l.setForeground(Aladin.COLOR_LABEL);
      gbimg.setConstraints(l,gcimg);
      buttonImg.add(l);
      gcimg.insets.bottom=0;
      gcimg.insets.top=0;

      gcdat.insets.bottom=6;
      gcdat.insets.top=12;
      ldat = l = new JLabel(Util.fold("<center><i>"+CAT+"</i></center>",80,true));
      l.setFont(Aladin.BOLD);
      l.setForeground(Aladin.COLOR_LABEL);
      gbdat.setConstraints(l,gcdat);
      buttonData.add(l);
      gcdat.insets.bottom=0;
      gcdat.insets.top=0;

      gctop.insets.right=12;
      l = new JLabel(Util.fold("<center><i>"+OTHER+"</i></center>",80,true));
      l.setFont(Aladin.BOLD);
      l.setForeground(Aladin.COLOR_LABEL);
      gbtop.setConstraints(l,gctop);
      buttonTop.add(l);
      gctop.insets.right=0;

      // Construction du tableau des serveurs et des boutons
      // et remplissage des panels adequats
      server = new Server[sv.size()];
      buttons = new MyButton[sv.size()];
      MyButton lastTop=null, lastLeft=null, lastRight=null;

      for( i = 0; i < server.length; i++ ) {
         gcimg.insets.top=0;
         server[i] = sv.elementAt(i);

         // Certains serveurs n'auront pas leur propre bouton et formulaire
         if( server[i].isHidden() ) {
            //System.out.println("Le serveur ["+server[i].getTitle()+"] n'a
            // pas son propre formulaire");
            continue;
         }

         if( server[i] instanceof ServerFile )  LOCAL = i;
         else if( server[i] instanceof ServerFoV )  FIELD = i;
         else if( server[i] instanceof ServerVizieR ) {
            VIZIER = i;
//            if( Aladin.OUTREACH ) {
//               gcdat.insets.top=45;
//               lastRight.setLastInColumn();
//            }
         }
         else if( server[i] instanceof ServerAladin ) {
            ALADIN = i;
//            if( Aladin.OUTREACH && lastLeft!=null ) {
//               lastLeft.setLastInColumn();
//            }
         }
         else if( server[i] instanceof ServerNED )    NED = i;
         else if( server[i] instanceof ServerSimbad ) SIMBAD = i;

//         else if( Aladin.OUTREACH && server[i].aladinLabel.startsWith("Hubble" ) ) {
//            gcimg.insets.top=45;
//         }

         // Un bouton a part entiere
         if( server[i].aladinMenu == null ) {
            if( server[i].type == Server.IMAGE ) {
               lastLeft=buttons[i] = new MyButton(aladin, status, MyButton.LEFT,
                     server[i].aladinLabel, server[i].description);
               gbimg.setConstraints(buttons[i],gcimg);
               buttonImg.add(buttons[i]);
            } else if( server[i].type == Server.CATALOG ) {
               lastRight=buttons[i] = new MyButton(aladin, status, MyButton.RIGHT,
                     server[i].aladinLabel, server[i].description);
               gbdat.setConstraints(buttons[i],gcdat);
               buttonData.add(buttons[i]);
            } else {
               lastTop=buttons[i] = new MyButton(aladin, status, MyButton.TOP,
                     server[i].aladinLabel, server[i].description);
               gbtop.setConstraints(buttons[i],gctop);
               buttonTop.add(buttons[i]);
            }
            buttons[i].setAlwaysUp(false);
            buttons[i].setModeMenu(true);
            if( server[i].aladinLogo!=null ) {
               try {
                  buttons[i].setBackGroundLogo(aladin.getImagette(server[i].aladinLogo),
                        server[i] instanceof ServerFolder ? MyButton.WITHLABEL:
                           MyButton.NOLABEL );
               } catch( Exception e ) {
                  System.err.println(e.getMessage());
               }
            }
         }
      }

      l = new JLabel();
      gcimg.weighty=1.;
      gcimg.fill = GridBagConstraints.BOTH;
      gbimg.setConstraints(l,gcimg);
      buttonImg.add(l);

      l = new JLabel();
      gcdat.weighty=1.;
      gcdat.fill = GridBagConstraints.BOTH;
      gbdat.setConstraints(l,gcdat);
      buttonData.add(l);

      l = new JLabel();
      gctop.fill = GridBagConstraints.BOTH;
      gbtop.setConstraints(l,gctop);
      buttonTop.add(l);

      // Rectification s'il n'y a aucun serveur d'images, resp de catalogues
      if( lastLeft==null )  limg.setText(Util.fold("<center><i>No image\nservers\nfound !</i></center>",80,true));
      if( lastRight==null ) ldat.setText(Util.fold("<center><i>No catalog\nservers\nfound !</i></center>",80,true));

      if( lastTop!=null )   lastTop.setLastInColumn();
      if( lastRight!=null ) lastRight.setLastInColumn();
      if( lastLeft!=null )  lastLeft.setLastInColumn();

      sv = null;
      try { buttons[bcurrent].push(); } catch( Exception e1 ) { }
      setFont(Aladin.PLAIN);

      // Construction des panels associees a chaque serveur ou Popup
      mp = new JPanel();
      mp.setOpaque(true);
      card = new CardLayout();
      mp.setLayout(card);
      for( i = 0; i < server.length; i++ ) {
         if( server[i] == null || server[i].isHidden() ) continue;
         server[i].setOpaque(true);
         mp.add(server[i].aladinLabel, server[i]);
      }
      selectDialog = new SelectDialog(this, mp);

      // Construction du panel central (boutons images - forms - boutons data)
      JPanel milieu = new JPanel();
      milieu.setOpaque(false);
      milieu.setLayout(new BorderLayout());
      Aladin.makeAdd(milieu, buttonImg, "West");
      Aladin.makeAdd(milieu, selectDialog, "Center");
      Aladin.makeAdd(milieu, buttonData, "East");

      // Construction des boutons d'actions
      JButton m;
      actions.add((m=new JButton(RESET)));    // m.setModeMenu(true);
      m.addActionListener(this); m.setOpaque(false);
      m.setToolTipText(TIPRESET);
      actions.add((m=new JButton(CLEAR)));    // m.setModeMenu(true);
      m.addActionListener(this); m.setOpaque(false);
      m.setToolTipText(TIPCLEAR);
      //      actions.add((m=new JButton(HISTORY)));  // m.setModeMenu(true);
      //      m.addActionListener(this);
      actions.add(new JLabel("           "));
      m=submit = new JButton(SUBMIT);         // m.setModeMenu(true);
      m.addActionListener(this); m.setOpaque(false);
      m.setToolTipText(TIPSUBMIT);
      m.setForeground(Aladin.COLOR_GREEN);
      m.setFont(m.getFont().deriveFont(Font.BOLD));
      actions.add(submit);
      actions.add(m=new JButton(CLOSE));   // m.setModeMenu(true);
      m.addActionListener(this); m.setOpaque(false);
      m.setToolTipText(TIPCLOSE);
      m.setForeground(Color.red);
      m.setFont(m.getFont().deriveFont(Font.BOLD));
      actions.add(m= Util.getHelpButton(this,MESSAGE));  // m.setModeMenu(true);
      actions.setFont(Aladin.BOLD);

      // Construction de la barre du bas (status, et boutons d'action)
      JPanel bas = new JPanel();
      bas.setOpaque(false);
      bas.setLayout(new BorderLayout(0,0));
      Aladin.makeAdd(bas, status,"Center");
      Aladin.makeAdd(bas, actions, "South");
      actions.setOpaque(false);

      // Le panel de la frame elle-meme
      JPanel ct = (JPanel)getContentPane();
      ct.setOpaque(true);
      ct.setLayout(new BorderLayout(0,0));
      //      ct.setBackground(new Color(250,249,245));
      ct.setBorder(BorderFactory.createEmptyBorder(3,3,0,3));
      Aladin.makeAdd(ct, buttonTop, "North");
      Aladin.makeAdd(ct, milieu, "Center");
      Aladin.makeAdd(ct, bas, "South");

      aladin.manageDrop();

//      if( !Aladin.BETA ) setCurrent("hips");
//      else 
         setCurrent("file");

      // INUTILE, C'EST MAINTENANT ASSEZ RAPIDE !
      //      Thread th = new Thread(this,"AladinServerPack");
      //      th.start();
      run();
   }
   
   public void dragGestureRecognized(DragGestureEvent dragGestureEvent) { }
   public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
      dropTargetDragEvent.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
   }
   public void dragExit (DropTargetEvent dropTargetEvent) {}
   public void dragOver (DropTargetDragEvent dropTargetDragEvent) {}
   public void dropActionChanged (DropTargetDragEvent dropTargetDragEvent){}
   public void dragDropEnd(DragSourceDropEvent DragSourceDropEvent){}
   public void dragEnter(DragSourceDragEvent DragSourceDragEvent){}
   public void dragExit(DragSourceEvent DragSourceEvent){}
   public void dragOver(DragSourceDragEvent DragSourceDragEvent){}
   public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent){}
   public synchronized void drop(DropTargetDropEvent dropTargetDropEvent) {
      aladin.drop(dropTargetDropEvent);
   }


   // Juste pour gagner qq secondes
   public void run() {
      pack();
      try { aladin.addServerMenu(this); } catch( Exception e ) { e.printStackTrace(); }
      setLock(true);
   }

   private boolean lock;
   synchronized private void setLock(boolean flag) { lock=flag; }

   /** Retourne true si le pack() est achevé */
   synchronized private boolean packed() { return lock; }

   protected boolean ivoaServersLoaded = false;
   /**
    * Création des servers associés aux enregistrements GLU que l'on récupère
    * depuis le registry IVOA (gateway GLU). Pour pouvoir les charger dans un
    * deuxième temps, on suppose qu'aucun d'entre eux ne sera visible
    * (isHidden()==true) car les formulaires ont déjà tous été créés. Cela ne
    * pourra servir que pour AllVO
    */
   synchronized protected boolean appendIVOAServer() {
      if( ivoaServersLoaded ) return true;

      DataInputStream dis;
      URL url = null;

      try {

         url = aladin.glu.getURL("IVOAdic");
         //         String s = Aladin.STANDALONE ? "http://aladin.u-strasbg.fr/java"
         //               : Aladin.CGIPATH;
         //         url = new URL(s + "/nph-aladin.pl?frame=ivoadic");

         Aladin.trace(1, "Loading the remote IVOA glu dictionary");
         Aladin.trace(3, "  => " + url);
         dis = new DataInputStream(aladin.cache.getWithBackup(url.toString()));

         aladin.glu.vGluServer = new Vector(50);
         aladin.glu.loadGluDic(dis,true,false);

         int n = aladin.glu.vGluServer.size();
         if( n == 0 ) {
            ivoaServersLoaded = false;
            return false;
         }

         Server newServer[] = new Server[server.length + n];
         System.arraycopy(server, 0, newServer, 0, server.length);

         for( int i = 0; i < n; i++ ) {
            newServer[server.length + i] = (Server) aladin.glu.vGluServer
                  .elementAt(i);
         }

         server = newServer;
         newServer = null;

      } catch( Exception e ) {
         e.printStackTrace();
         System.err.println("Remote IVOA Glu dictionary not reached");
         ivoaServersLoaded = false;
         return false;
      }
      ivoaServersLoaded = true;
      return true;

   }

   // pour workshop Euro-VO
   synchronized protected void appendServersFromStream(InputStream is) {

      try {

         DataInputStream dis = new DataInputStream(is);
         aladin.glu.vGluServer = new Vector(50);
         aladin.glu.loadGluDic(dis,true,false);

         int n = aladin.glu.vGluServer.size();
         if( n == 0 ) return;

         Server newServer[] = new Server[server.length + n];
         System.arraycopy(server, 0, newServer, 0, server.length);

         for( int i = 0; i < n; i++ ) {
            newServer[server.length + i] = (Server) aladin.glu.vGluServer
                  .elementAt(i);
         }

         server = newServer;
         newServer = null;

      } catch( Exception e ) {
         e.printStackTrace();
         System.err.println("Problem while adding servers");
      }
   }

   /**
    * Retourne l'indice du serveur correspondant a un nom de serveur, sinon -1
    */
   protected int getServer(String s) {
      int j;
      for( j = 0; j < server.length; j++ ) {
         if( server[j].is(s) ) return j;
      }
      //      if( s.equalsIgnoreCase("allsky") ) return Integer.MAX_VALUE;
      return -1;
   }

   /*
    * // Creation des formulaires pour le Standalone, histoire de ne pas charger //
    * les classes pour rien private void createFormSA() { server[LOCAL] =
    * (Server)(new LocalServer(aladin,status)); }
    */


   // Gestion des evenement
   //   public boolean handleEvent(Event e) {
   //      // pour sortir du mode Robot
   //      if( e.id == Event.KEY_PRESS && e.key == java.awt.event.KeyEvent.VK_ESCAPE
   //            && aladin.command.robotMode ) {
   //         aladin.stopRobot(this);
   //         return true;
   //      }
   //      if( (e.id == Event.WINDOW_DESTROY) ) cache();
   //      return super.handleEvent(e);
   //   }

   /** Action sur une ESC */
   private void trapESC() {
      if( aladin.command.robotMode ) aladin.stopRobot(this);
      else cache();
   }

   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) cache();
      super.processWindowEvent(e);
   }

   // On cache egalement la sous fenetre de VizieR, FrameInfo, et FrameServer
   // si necessaire
   protected void cache() {
      try {
         if( Aladin.NETWORK ) ((ServerVizieR) server[VIZIER]).hideSFrame();
         FrameInfo f = aladin.getFrameInfo();
         if( f != null ) f.setVisible(false);
//         if( discoveryServer != null && ((ServerAllVO) discoveryServer).frameServer != null ) {
//            ((ServerAllVO) discoveryServer).frameServer.setVisible(false);
//         }
      } catch( Exception e ) { }
      setVisible(false);
   }

   // Fait sortir les autres boutons
   void popOtherButton() {
      int i;
      for( i = 0; i < buttons.length; i++ ) {
         if( i != bcurrent && buttons[i] != null ) buttons[i].pop();
      }
   }

   /**
    * Mise en place du target en calculant la position courante dans la Vue en
    * fonction du x,y de la souris
    * @param x,y Position dans la vue
    */
   public void setGrabItCoord(Coord c) { //double x, double y) {
//      ViewSimple v = aladin.view.getCurrentView();
//      Plan pr = v.pref;
//      if( pr == null ) return;
//      Projection proj = pr.projd;
//      if( proj == null ) return;
//      PointD p = v.getPosition(x, y);
//      Coord c = new Coord();
//      c.x = p.x;
//      c.y = p.y;
//      proj.getCoord(c);
      if(  Double.isNaN(c.al) ) return;
      server[current].setTarget(aladin.localisation.getFrameCoord(c.getSexa()));
   }

   /**
    * Arrete le GrabIt
    */
   public void stopGrabIt() {
      JToggleButton grab = server[current].grab;
      if( grab != null ) {
         Plan pref = aladin.calque.getPlanRef();
         grab.getModel().setSelected(false);
         grab.setEnabled(pref != null && Projection.isOk(pref.projd));
         Server s = server[current];
         if( s.tree!=null && !s.tree.isEmpty() ) s.tree.clear();
         if (s instanceof ServerTapExamples) {
				((ServerTapExamples)s).targetSettingsChangedAction();
		 }
      }
      toFront();
   }

   /**
    * Démarrage d'une séquence de GrabIT
    */
   public void startGrabIt() {
      if( server[current].grab == null
            || !server[current].grab.getModel().isSelected() ) return;
      
      if( aladin.firstGrab && aladin.configuration.isHelp() && aladin.configuration.showHelpIfOk("GRABINFO") ) {
         aladin.firstGrab=false;
      }

      aladin.f.toFront();
   }

   /**
    * Retourne true si le bouton grabit du formulaire existe et qu'il est
    * enfoncé
    */
   public boolean isGrabIt() {
      return (server[current].modeCoo != Server.NOMODE
            && server[current].grab != null && server[current].grab.getModel().isSelected());
   }

   /**
    * Mise en place du radius en calculant la position courante dans la Vue en
    * fonction du x,y de la souris
    * @param x,y Position dans la vue
    */
   public void setGrabItRadius(double x1, double y1, double x2, double y2) {
      if( server[current].modeRad == Server.NOMODE ) return;
      if( Math.abs(x1 - x2) < 3 && Math.abs(y1 - y2) < 3 ) return;
      ViewSimple v = aladin.view.getCurrentView();
      Plan pr = v.pref;
      if( pr == null ) return;
      Projection proj = pr.projd;
      if( proj == null ) return;
      PointD p1 = v.getPosition(x1, y1);
      PointD p2 = v.getPosition(x2, y2);
      Coord c1 = new Coord();
      c1.x = p1.x;
      c1.y = p1.y;
      proj.getCoord(c1);
      if( Double.isNaN(c1.al) ) return;
      Coord c2 = new Coord();
      c2.x = p2.x;
      c2.y = p2.y;
      proj.getCoord(c2);
      if( Double.isNaN(c2.al) ) return;
      server[current].resolveRadius(Coord.getUnit( /*Math.sqrt(2)* */
            Coord.getDist(c1, c2)), true);
   }

   /** Ajuste les champs de saisie en fonction du repere courant et de la taille du champ */
   protected void adjustParameters() {
      setDefaultParameters(current,3);
   }

   /**
    * Mise en place du target/radius/epoch par defaut. Le choix se fait en fonction de
    * la derniere saisie et du plan de reference courant
    * @param i le numero du formulaire, si -1 utilise le repère
    * @param mode 0 - sans tenir compte de la chaine en cours de saisie 1 - en
    *           tenant compte de la chaine en cours de saisie 2 - en tenant
    *           compte de la chaine en cours de saisie et du repere 3 - en
    *           tenant compte du repere 4 - Utilise l'objet du plan courant 5 -
    *           Idem 2 mais la priorité sur le rayon est le champ courant (pour
    *           Server.reset())
    */
   synchronized protected void setDefaultParameters(int i, int mode) { setDefaultParameters(i,mode,null); }
   synchronized protected void setDefaultParameters(int i, int mode, String internalId) {
      String lastTarget = null;
      String lastTaille = null;
      String radec = null; // pour le formulaire des instruments
      String taille = null; // idem
      String epoch = null;
      String objet = null;

      // BUG [issue154] SI JE NE COMMENTE PAS CETTE LIGNE DE TOUTE FACON INUTILE,
      // IL N'Y AURA PAS MISE A JOUR DES TARGET/RADIUS/DATE SI LE FORMULAIRE
      // COURANT ET UN CHARGEMENT DE FICHIER, OR SI ON CHARGE UN SCRIPT AJS VIA CE FORMULAIRE
      // LES COMMANDES TELLES QUE SKYBOT NE PRENDRONT PAS LA DATE PAR DEFAUT
      // pas de target pour les plans de type LOCAL
      //      if( i == LOCAL || server[i] instanceof MyPopup ) return;

      // Recuperation de la chaine en cours de saisie
      if( mode == 1 || mode == 2 || mode == 5 ) {
         lastTarget = getDefaultTarget();
         //System.out.println("Derniere target saisie : ["+lastTarget+"]");
         if( lastTarget.length() == 0 ) lastTarget = null;
         lastTaille = getDefaultTaille();
         //System.out.println("Derniere taille saisie : ["+lastTaille+"]");
         if( lastTaille.length() == 0 ) lastTaille = null;
      }

      boolean tooLarge=false;
      ViewSimple v = aladin.view.getCurrentView();
      Projection proj;
      if( v != null && !v.isFree() && v.pref != null && (proj=v.getProj()) != null ) {
         if( mode == 1 || mode == 2 || mode == 4 || mode == 5 )  objet = v.pref.objet;

         // Récupération de la taille à partir du plan courant
         if( mode != 2 && mode != 3 ) {
            if( Projection.isOk(proj) ) {
               tooLarge=proj.rm>180*60;
               if( v.pref.isImage() ) {
                  Calib c = proj.c;
                  taille = Coord.getUnit(c.getImgWidth()) + " x "
                        + Coord.getUnit(c.getImgHeight());
               } else {
                  taille = Coord.getUnit(proj.rm / 120.);
               }
            }
         }

         if( mode==3 ) taille = v.getTaille(2);

         // Récupération de la position du repère
         if( mode == 2 || mode == 3 || mode == 5 || v.pref instanceof PlanBG )  {
            radec = Coord.getSexa(aladin.view.repere.raj, aladin.view.repere.dej, ":");
         }

         // Recuperation de l'objet central et des coord du plan de ref
         else radec = v.getCentre();

         if( tooLarge ) taille=radec=null;

      } else {
         lastTarget = getDefaultTarget();
         lastTaille = getDefaultTaille();
      }

      String defTarget = lastTarget != null ? lastTarget
            : objet != null ? aladin.localisation.getFrameCoord(objet) : radec != null ? aladin.localisation.getFrameCoord(radec) : "";
            
      String defTaille;
      if( mode == 5 ) defTaille = lastTaille != null ? lastTaille : taille;
      else defTaille = taille != null ? taille : lastTaille;

      setDefaultTarget(defTarget);
      if( server[i].modeRad != Server.NOMODE ) setDefaultTaille(taille);

      // Positionnement de l'epoque d'observation
      if( v!=null ) {
         epoch = v.getEpoch();
         if( epoch != null ) server[i].setDate(epoch);
         else if( v.pref instanceof PlanImage ) {
            epoch = ((PlanImage)v.pref).getDateObs();
            if( epoch!=null ) server[i].setDate(epoch);
         }
         setDefaultDate(epoch);
      }
      
      // Positionnement de baseUrl
      if( internalId!=null ) server[i].setBaseUrl(internalId);

      // Si le formulaire a un arbre de métadata non vide, on ne met pas
      // à jour le target à moins que le target soit vide
      if( server[i].tree != null && !server[i].tree.isEmpty()
            && (server[i].target==null || server[i].target.getText().trim().length()!=0 ) ) return;

      server[i].setTarget(defTarget);
      try {
         if( defTaille != null && defTaille.trim().length()!=0
               && server[i].modeRad != Server.NOMODE ) server[i].resolveRadius(defTaille, true);
      } catch( Exception e ) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      }

   }
   
   /**
    * activation ou non du bouton grab
    * @param flag true ou false
    */
   protected void setGrab() {
      Plan pref = aladin.calque.getPlanRef();
      boolean flag = (pref != null && pref.projd != null);
      for( int i = 0; i < server.length; i++ ) {
         if( server[i] != null && server[i].grab != null ) server[i].grab.setEnabled(flag);
      }
   }

   //   protected String getDefaultRadius() {
   //      Plan p;
   //
   //      if( (p = aladin.calque.getPlanRef()) == null || !Projection.isOk(p.projd) ) return "14";
   //      return "" + (Math.floor((p.projd.rm / 2) * 1.4142));
   //   }

   /** Affiche la fenêtre. Attend éventuellement la fin d'un pack en cours */
   void showNow() {
      while( !packed() ) { Util.pause(100); }
      setVisible(true);
   }

   /** Affichage de l'interface */
   public void setVisible(boolean flag) {

      if( !flag ) { super.setVisible(false); return; }

      // Vérifie que le pack() est terminé
      if( !packed() ) { return; }

      if( current>=0 ) server[current].resumeTargetLabel();

      if( !flagSetPos ) {
         setLocation(Aladin.computeLocation(this));
         if( current>=0 ) {
            server[current].setInitialFocus();
            server[current].initServer();
         }
         flagSetPos = true;
      }
      setDefaultParameters(current, 2);
      try {
         super.setVisible(true);
      } catch( Exception e ) {
         e.printStackTrace();
      }

   }

   /**
    * Mise à jour dynamique de certain éléments des formulaires - appelé par
    * Plan.planReady()
    */
   protected void resume() {
      setGrab();
      server[current].resumeTargetLabel();
      server[current].resumeInputChoice();
   }

   /** Si le nom est un serveur "outreach", il va y avoir appel immédiat à ce serveur
    * avec la position par défaut. S'il n'y a pas de position par défaut, une
    * fenêtre Popup va la demander
    * Ne fonctionne que dans le mode outreach (undergraduate)
    * @param nom nom du serveur (ou nom du catalogue pour les cas VizieR)
    * @return true si trouvé, false sinon
    */
   protected boolean submitServer(String nom) {
      int i = findIndiceServer(nom);

      // Les cas particuliers VizieR
//      if( vizierSurveys!=null ) {
//         if( i<0 && vizierSurveys.setParam(nom) )  i=findIndiceServer(vizierSurveys);
//      }
//      if( vizierSurveys!=null ) {
//         if( i<0 && vizierArchives.setParam(nom) ) i=findIndiceServer(vizierArchives);
//      }

      if( i<0 ) return false;
      setCurrent(i);
      if( /* !Aladin.OUTREACH || */ getDefaultTarget().length()==0 ) toFront();
      else server[i].submit();
      return true;
   }
   
   /** Montre le formulaire server dont le tagGlu (ActionName) est passé en paramètre */
   public boolean showByGlutag(String gluTag, String internalId) {
      int i = findIndiceServerByGluTag(gluTag);
      if( i<0 ) return false;
      setCurrent(i,internalId);
      toFront();
      return true;
   }

   /** Montre le formulaire server dont le nom est passé en paramètre */
   public boolean show(String nom) {
      int i = findIndiceServer(nom);
      if( i<0 ) return false;
      
      if ("TAP".equals(nom)) {
    	  if (!tapManager.checkDummyInitForServerDialog(tapServer)) {
  			return false;
      	  };
    	  /*if (aladin.glu.lastTapGluServer == null && tapManager.checkDummyTapServer(tapServer)) {
    		  try {
				this.tapManager.showTapRegistryForm();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(this, Aladin.getChaine().getString("GENERICERROR"));
				e.printStackTrace();
			}
    		  return false;
		  }*/
      }
      
      setCurrent(i);
      toFront();
      return true;
   }

   /** Montre le formulaire du serveur passé en paramètre */
   protected void show(Server x) {
      if( !isVisible() ) setVisible(true);
      setCurrent(findIndiceServer(x));
      toFront();
   }
   
	/**
	 * Method to replace old server entries on server dialog
	 * @param oldServerRef
	 * @param newServer
	 */
	protected void findReplaceServer(Server oldServerRef, Server newServer) {
		int oldServerIndex = findIndiceServer(oldServerRef);
		server[oldServerIndex] = newServer;
		mp.remove(oldServerRef);
		mp.add(newServer.aladinLabel, newServer);
	}

   /** Montre le formulaire du serveur passé en paramètre */
   protected boolean show(Server x,String param1) {
      if( x==null || !x.setParam(param1) ) return false;
      setCurrent(findIndiceServer(x));
      toFront();
      return true;
   }

   public void toFront() {
      setExtendedState(JFrame.NORMAL);
      if( !isVisible() ) setVisible(true);
      super.toFront();
   }



   /** retourne l'indice d'un serveur */
   private int findIndiceServer(Server x) {
      for( int i = 0; i < server.length; i++ ) {
         if( server[i] == x ) return i;
      }
      return -1;
   }

   /**
    * retourne l'indice d'un serveur en fonction de son nom
    * @return l'indice du serveur s,sinon -1
    */
   protected int findIndiceServer(String s) {
      for( int i = 0; i < server.length; i++ ) {
         if( server[i].sameNom(s) ) return i;
      }
      return -1;
   }
   
   /**
    * retourne l'indice d'un serveur en fonction de son nom
    * @return l'indice du serveur s,sinon -1
    */
   protected int findIndiceServerByGluTag(String s) {
      for( int i = 0; i < server.length; i++ ) {
         if( s.equals(server[i].gluTag) ) return i;
      }
      return -1;
   }

   /**
    * Retourne l'indice du formulaire courant
    */
   protected int getCurrent() {
      return current;
   }

   /** Retourne la liste des noms de servers d'un certain type
    * On fait deux tours pour compter au préalable le nombre d'élus.*/
   protected String [] getServerNames(int type,boolean all) {
      int n=0;
      String [] res=null;

      for( int j=0; j<2; j++ ) {
         if( j==1 ) res = new String[n];
         n=0;
         for( int i = 0; i < server.length; i++ ) {
            if( server[i] == null || server[i].isHidden()
                  || server[i] instanceof ServerFolder) continue;
            if( (server[i].type & type)==0 ) continue;
            if( !all && !(server[i] instanceof ServerGlu) ) continue;
            String s[] = server[i].getNomPaths();
            if( j==1 ) {
               for( int k=0; k<s.length; k++ ) res[n+k] = s[k];
            }
            n+=s.length;
         }
      }
      return res;
   }

   // TODO : a finir, ne fonctionne pas pour le moment
   /** Retourne la liste des noms de servers d'un certain type
    * On fait deux tours pour compter au préalable le nombre d'élus.*/
   protected String [] getServerLogos(int type) {
      int n=0;
      String [] res=null;

      for( int j=0; j<2; j++ ) {
         if( j==1 ) res = new String[n];
         n=0;
         for( int i = 0; i < server.length; i++ ) {
            if( server[i] == null || server[i].isHidden()
                  || server[i] instanceof ServerFolder) continue;
            if( server[i].type!=type ) continue;
            String s[] = {server[i].aladinLogo};
            if( j==1 ) {
               for( int k=0; k<s.length; k++ ) res[n+k] = s[k];
            }
            n+=s.length;
         }
      }
      return res;
   }

   /**
    * Positionne le formulaire courant
    * @param s le nom du formulaire
    */
   protected void setCurrent(String s) { setCurrent(s,null); }
   protected void setCurrent(String s, String internalId) {
      setCurrent(findIndiceServer(s),internalId);
   }

   /**
    * Positionne le formulaire courant
    * @param i indice du formulaire
    */
   protected void setCurrent(int i) { setCurrent(i,null); }
   protected void setCurrent(int i, String internalId) {
      if( i < 0 || i >= server.length ) return;


      if( buttons[i] != null ) buttons[i].push();
      setDefaultParameters(i, 2, internalId);
      current = i;
      // robot
      curServer = server[current];
      if( server[i].aladinMenu != null ){
         bcurrent = findIndiceServer(server[i].aladinMenu);
      }
      else bcurrent = i;
      selectDialog.invalidate();
      selectDialog.repaint();
      popOtherButton();

      // maj de la taille des Choice (thomas pour randy)
      // TODO : ce n'est certainement plus necessaire
      if( server[i] instanceof ServerGlu ) ((ServerGlu) server[i]).majChoiceSize();

      card.show(mp, server[i].aladinLabel);


      //Dans le cas ou il faut effacer la sous-fenetre VizieR
      //SANS DOUTE DESORMAIS INUTILE/ERRONE DEPUIS LA V10 - JE METS EN TRY/CATCH AU CAS OU
      try {
         if( server[i] instanceof ServerVizieR ) ((ServerVizieR) server[VIZIER]).hideSFrame();
      } catch( Exception e ) { }

      // initialisation du focus (Thomas, 16/03/06, à montrer à Pierre)
      // TODO : à vérifier depuis passage à swing
      server[i].setInitialFocus();

      // TODO : temporaire, à supprimer après refonte de MetadataTree/BasicTree
      server[i].initServer();
      invalidate();
   }

   // Gestion des evenement
   public void actionPerformed(ActionEvent e) {
      if( !(e.getSource() instanceof JButton) ) return;

      String s = ((JButton)e.getSource()).getActionCommand();

      // On cache, on reset, on submit
      if( CLOSE.equals(s) ) {
         server[current].memTarget(); // Memorisation du precédent target
         if (server[current] instanceof ServerGlu) {
        	 ((ServerGlu)server[current]).cleanUpFOV();
         }
         
         //         myClose.normal();
         cache();
         if (tapManager != null) {
        	 tapManager.hideTapRegistryForm();
		 }
         return;
      }
      if( CLEAR.equals(s) ) {
         server[current].clear();
         return;
      }
      if( SUBMIT.equals(s) ) {
         if( (e.getModifiers() & InputEvent.SHIFT_MASK)!=0 ) server[current].flagVerif=false;
         server[current].submit();
         return;
      }

      // Re-initialisation des defauts
      if( RESET.equals(s) ) {
         server[current].reset();
         return;
      }
   }
   
   public boolean showGenericTapServer() {
      if (!tapManager.checkDummyInitForServerDialog(tapServer))  return false;
       setCurrent("TAP");
       return true;
   }

   // Gestion des evenement
   public boolean action(Event evt, Object what) {
      if( !(what instanceof String) ) return false;


      // Changement du formulaire
      server[current].memTarget(); // Memorisation du precedent target
      
      // Cas spécifique pour TAP
      if ("TAP".equals(what)) showGenericTapServer();
      
      // Cas général
      else setCurrent((String) what);

      return false;
   }

   // implémentation de WidgetFinder
   public boolean findWidget(String name) {
      if( name.equalsIgnoreCase("local") || name.equalsIgnoreCase("simbad")
            || name.equalsIgnoreCase("ned") || name.equals("vizier")
            || name.equals("aladin") ) return true;

      return false;
   }

   public Point getWidgetLocation(String name) {
      ComponentLocator cl = new ComponentLocator();
      if( name.equalsIgnoreCase("local") || name.equalsIgnoreCase("mydata") ) return cl.getLocation( buttons[LOCAL], this);
      if( name.equalsIgnoreCase("simbad") ) return cl.getLocation( buttons[SIMBAD], this);
      if( name.equalsIgnoreCase("ned") ) return cl.getLocation(buttons[NED], this);
      if( name.equalsIgnoreCase("vizier") ) return cl.getLocation( buttons[VIZIER], this);
      if( name.equalsIgnoreCase("aladin") ) return cl.getLocation( buttons[ALADIN], this);
      return null;
   }
}
