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

import java.awt.Color;
import java.awt.Label;
import java.net.URL;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import cds.tools.Util;

/**
 * Objet de creation des objets en asynchrone
 * pour ameliorer le temps de demarrage du programme
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (20 mars 2000) Creation
 */
public final class CreatObj implements Runnable {

   Thread thread;               // Pour creer les objets en asynchrone
   Aladin a;
   private long t;              // Mémorise le temps courant pour calculer la vitesse de création des objets

   protected CreatObj(Aladin aladin) {
      a=aladin;


      initTimer();
      a.console = new Console(a);
      a.console.printInfo("Aladin "+a.VERSION+" under JVM "+aladin.javaVersion+" with "+a.MAXMEM+"MB");
      trace(1,"Creating Console window");

      a.synchroServer = new Synchro(10000);
      a.synchroPlan = new Synchro(60000);
      a.command=new Command(a);
      trace(1,"Creating Command interface");

      a.vButton = new Vector(10);
//      trace(1,"Creating Button menu");

      a.glu = new Glu(a);
      trace(1,"Creating Glu gateway");
      
      a.logo = new Logo(a);
//      trace(1,"Creating Aladin logo");

      a.status = new Status(a,a.WELCOME);
//      trace(1,"Creating Status object");

      a.match = new Match(a);
      a.grid = new Grid(a);
      a.oeil = new Oeil(a);
      a.northup = new Northup(a);
      a.pix = new Hdr(a);
//      trace(1,"Creating Sync, Split, Grid, Wink logo");

      a.viewControl = new ViewControl(a);
      trace(1,"Creating View control widget");

      a.urlStatus = new Tips(a);
//      trace(1,"Creating URL info line");

      a.memStatus = new MyLabel("",Label.RIGHT,a.SPLAIN);
      a.memStatus.setForeground(Color.darkGray);
//      trace(1,"Creating Memory status line");

      a.mesure = new Mesure(a);
      a.search = new Search(a,true);
      trace(1,"Creating Measurement panel");

      a.toolBox = new ToolBox(a);
//      trace(1,"Creating Toolbar panel");

      a.calque = new Calque(a);
      trace(1,"Creating Calque object");

      a.treeView = new TreeView(a);
      trace(1,"Creating Treeview window");

      a.localisation = new Localisation(a);
      trace(1,"Creating Localisation widget");

//      a.pixel = new Pixel(a);
//      trace(1,"Creating Pixel widget");

      a.view = new View(a,a.calque);
      trace(1,"Creating Multiview Panel");

      a.help = new Help(a);
//      trace(1,"Creating Help");

      
      a.save = new Save(a);
      trace(1,"Creating Save");
      
      a.setMemory();


   }

   // Initialisation du timer
   private void initTimer() {
      t = System.currentTimeMillis();
   }

   /** Affichage du message de trace + temps nécessaire à l'exécution depuis le dernier message */
   private void trace(int n,String s) {
      long t1 = System.currentTimeMillis();
      s += " ("+(t1-t)+"ms)";
      t=t1;
      a.trace(n,s);
   }

 /** Creation des derniers objets de l'interface Aladin et traitement
   * d'eventuels plans a charger immediatement
   */
   protected boolean creatLastObj() {

      // Creation asynchrone...
      thread = new Thread(this,"AladinCreatObj");
      thread.start();

      // Y a-t-il des plans a charger par les parametres
      if( Aladin.isApplet() && a.extApplet==null )
                             return a.getParameter("img")!=null
                                 || a.getParameter("-c")!=null
                                 || a.getParameter("script")!=null;

      return false;
   }

  /** Creation des objets en asynchrone */
   public void run() {

       // launch try to autoconnect to PLASTIC/SAMP (and might launch a hub according to user prefs and Aladin.NOHUB)
       // and launch timer which will periodically check PLASTIC/SAMP status
       if( Aladin.PLASTIC_SUPPORT ) {
           a.setPlastic(a.plasticPrefs.getBooleanValue(PlasticPreferences.PREF_LAUNCHHUB) && !Aladin.NOHUB);
           Timer timer = new Timer();
           TimerTask timerTask = new TimerTask() {
               public void run() {
                   a.setPlastic(false);
               }
           };
           timer.schedule(timerTask, 5000, 5000);
       }

      // Creation des derniers objets
      initTimer();
      a.dialog = new ServerDialog(a);
      trace(1,"Creating Server window");

      // Positionnement du frame par défaut
      if( a.NOGUI ) {
         a.localisation.setFrame(a.localisation.ICRS);
         a.trace(3,"Default frame: "+a.localisation.getFrameName(a.localisation.ICRS));

      } else {
         a.localisation.setFrame(a.configuration.getFrame());
         a.trace(3,"Default frame: "+a.localisation.getFrameName(a.configuration.getFrame()));
      }
      a.searchData.setEnabled(true);
      
      if( a.BOOKMARKS ) {
         trace(1,"CreateObj.run(): initializing bookmarks...");
         a.bookmarks.init(false);
      }

      // Traitement des parametres en mode applet
      if( Aladin.isApplet() && a.extApplet==null ) processParameter();

      // Exécution d'un launch script (voir main())
      else {
         String script = a.getLaunchScript();
         if( script.length()>0 ) {
            a.trace(4,"CreateObj.run(): exec launch script => "+script);
//            String s = a.execCommand(script);
            String s="";
            a.command.execScriptAsStream(script);
            if( a.quitAfterLaunchScript() ) {
               while( a.command.hasCommand() ) {
//                  a.trace(3,"Command stream processing...");
                  Util.pause(100);
               }
               a.trace(4,"CreateObj.run(): command stream achieved => quit!");
               a.quit(s==null ? 0:1);
            }
         }
      }

   }

   private String getParam(Hashtable t,String key) {
      return (t==null)?a.getParameter(key):(String)t.get(key);
   }

   /** Traite les parametres pour charger des plans des le demarrage.
     * @param t si t est != null, les parametres seront pris de cette table
     * @return <I>true</I> si des donnees vont etre chargee, sinon <I>false</I>
     *
     * @RQ: Uniquement via l'applet
     */
    protected void processParameter() { processParameter(null); }
    protected void processParameter(Hashtable t) {
       URL u;
       String param;
       boolean rep=false;
       StringTokenizer st;

       a.trace(1,"Analyzing the parameters...");
       a.waitDialog();

       try {
          // Chargement d'un script
          // Chargement d'eventuelles images par le parametre
          // "img=url[ label],..."
          String load = getParam(t,"img");
          if( load!=null ) {
             a.trace(3,"loading...");
             st = new StringTokenizer(load,",");
             while( st.hasMoreTokens() ) {
                StringTokenizer st1 = new StringTokenizer(st.nextToken()," ");
                try {
                   String adr=st1.nextToken();
                   String label=st1.hasMoreTokens()?st1.nextToken():null;
                   String origin=st1.hasMoreTokens()?st1.nextToken():null;
                   a.dialog.server[ServerDialog.LOCAL].createPlane(null,null,adr,label,origin);
                } catch( Exception ef ) { System.err.print("Cannot build url: "+ef); };
             }
          }

          // Compatibilite avec l'ancienne methode des parametres
          String c = getParam(t,"-c");
          if( c!=null ) {
             a.setBannerWait();
             // Recuperation des differents parametres
             String rm     = getParam(t,"-rm");
             String servers = getParam(t,"-server");
             String source = getParam(t,"-source");
             String qualifier = getParam(t,"-aladin.qualifier");
             String resol = getParam(t,"-aladin.resolution");
             String fmt = getParam(t,"-aladin.format");
             String fov = getParam(t,"-fov");

             // les valeurs par defaut
             if( servers==null ) servers="Aladin";
             if( resol==null ) resol="FULL";
             if( fmt==null ) fmt="JPEG";

             st = new StringTokenizer(servers,",");
             while( st.hasMoreTokens() ) {
                try {
                   String server = st.nextToken();

                   // Aladin
                   if( server.indexOf("Aladin")>=0 ) {
                      String criteria = fmt+" "+resol;
                      if( qualifier!=null ) criteria = criteria+" "+qualifier;
                      a.dialog.server[ServerDialog.ALADIN].createPlane(c,rm,criteria,null,null);
                   }

                   // VizieR
                   if( server.indexOf("VizieR")>=0 && source!=null ) {
                      a.dialog.server[ServerDialog.VIZIER].createPlane(c,rm,source,null,null);
                   }

                   // Simbad
                   if( server.indexOf("Simbad")>=0 ) {
                      a.dialog.server[ServerDialog.SIMBAD].createPlane(c,rm,null,null,null);
                   }

                   // NED
                   if( server.indexOf("NED")>=0 ) {
                      a.dialog.server[ServerDialog.NED].createPlane(c,rm,null,null,null);
                   }
                } catch( Exception e1 ) { System.out.println("Pb: "+e1); e1.printStackTrace(); }
             }

             // Pour les FoV
             st = new StringTokenizer(fov,",");
             while( st.hasMoreTokens() ) {
                a.dialog.server[ServerDialog.FIELD].createPlane(c,null,st.nextToken(),null,null);
             }
          }

          String script = getParam(t,"script");
          if( script==null ) script=getParam(t,"-script");
          if( script!=null ) {
             a.setBannerWait();
             a.command.execScriptAsStream(script);
//             a.command.execScript(script);
          }

       } catch( Exception e ) {}
    }
}

