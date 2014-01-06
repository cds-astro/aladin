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

import java.lang.reflect.Constructor;
import java.awt.Color;
import java.io.*;
import java.text.ParseException;
import java.util.*;

import javax.imageio.stream.FileImageInputStream;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import cds.allsky.Context;
import cds.allsky.HipsGen;
import cds.astro.AstroMath;
import cds.astro.Astrocoo;
import cds.astro.Unit;
import cds.moc.HealpixMoc;
import cds.savot.model.SavotField;
import cds.tools.Computer;
import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.xml.Field;


/**
 * Gestion de commandes en ligne d'Aladin java
 * @version 1.7 (dec 2004) ajout des commandes des vues, du blink...
 *                         et modif des helps
 * @version 1.6 (mars 2004) ajout commande xmatch, commande robot
 * @version 1.5 (16 aout 2002) ajout command draw
 * @version 1.4 (24 juin 2002) ajout command flipflop
 * @version 1.3 (17 mai 2002) separateur ":" toleres dans commande get
 * @version 1.2 (22 fevrier 2002) Ajout de la commande RGB
 * @version 1.1 (12 fevrier 2001) Ajout d'un time out sur la synchro
 * @version 1.0 (5 fevrier 2001) Creation
 * @author  P.Fernique [CDS]
 */
public final class Command implements Runnable {

   static final long TIMEOUT = 15L;	 // 15 minutes de chien de garde
                                                 // pour la commande "sync"
   static long timeout = TIMEOUT*60000;

   // Gestion du systeme de coordonnees pour la commande "draw"
   static int DRAWXY = 0;
   static int DRAWRADEC = 1;
   static int drawMode = DRAWRADEC;	// mode de coord courant

   Aladin a;
   Calque c;
   private CommandDS9 ds9;      // Pour gérer les conversions de commandes DS9
   private boolean stop;        // passe à true pour tuer les threads de lecture

   private String lastCmd="";		// Dernière commande exécutée
   
//   protected boolean syncNeedSave=false;  // True si on doit attendre la fin d'un Save,Export ou Backup pour quitter
   protected Synchro syncSave = new Synchro();
   
   private Thread thread;   // Le thread de lecture des commandes
   JFrame robotInfo;
   String curTuto;
   JTextArea infoTxt;
   
   HashMap<String, String> var  = new HashMap<String, String>(100);

   MyRobot robot;
   
   public boolean robotMode = false;      // pour robot : vrai si en mode "robot"
   boolean filterMode = false;            // vrai si on est en mode "definition de filtre"
   StringBuffer filterDef = null;         // objet contenant la def. du filtre

   PlanImage planRGBRef;   // Particularite pour traiter la commande RGB

   static final String execHelp =
      "\n" +
      "#PLANE:#                              #VIEW:#\n" +
      "   @get servers [target] [radius]      @mview [1|2|4|9|16] [n]\n" +
      "   @load filename                      @cview [-plot] [[x] v]\n" +
      "   @select x1 [x2..]                   @select v1 [v2..]\n" +
      "   @set [x1] [x2..] prop=value         @zoom ...\n" +
      "   @hide|@show [x1] [x2..]              @northup|@unnorthup [v1] [v2..]\n" +
      "   @mv|@copy x1 x2                      @lock|@unlock [v1] [v2..]\n" +
      "   @rm [x1] [x2..] | -all              @match [-scale] [v|x|off]\n" +
      "   @export [-fmt] x filename           @mv|@copy v1 v2\n" +
      "                                      @rm [v1] [v2..] | -lock\n" +
      "#IMAGE:#                                @save [-fmt] [-lk] [WxH] [filename]\n" +
      "   @cm [x1|v1...] [colorMap...]        @coord|@object\n" +
      "   @RGB|@RGBdiff [x1|v1...]\n" +
      "   @blink|@mosaic [x1] [x2...]          #CATALOG:#\n" +
      "   @+ | @- | @* | @/ ...                  @filter ...\n" +
      "   @norm [-cut] [x]                    @addcol ...\n" +
      "   @conv [x] ...                       @xmatch x1 x2 [dist] ...\n" +
      "   @kernel ...                         @cplane [name]\n" +
      "   @resamp x1 x2 ...                   @search {expr|+|-}\n" +
      "   @crop [x|v] [[X,Y] WxH]             @tag|@untag\n\n" +
      "   @flipflop [x|v] [V|H]               @select -tag\n" +
      "   @contour [nn] [nosmooth] [zoom]\n" +
      "   @grey|@bitpix [-cut] [x] BITPIX\n" +
      "  \n" +
      "#GRAPHIC# #TOOL:#                       #FOLDER:#\n" +
      "   @draw [color] fct(param)            @md [-localscope] [name]\n" +
      "   @grid [on|off]                      @mv|@rm [name]\n" +
      "   @reticle [on|off]                   @collapse|@expand [name]\n" +
      "   @overlay [on|off]                   @show|@hide [name]\n" +
      " \n" +
      "#MISCELLANEOUS:#\n" +
      "   @backup filename     @status       @sync       @demo [on|off|end]  @pause [nn]\n" +
      "   @help ...            @trace        @mem        @info msg\n" +
      "   @macro script param  @call fct     @list [fct] @reset\n" +
      "   @setconf prop=value  @function ... @= ...      @convert      @quit" +
      "";
 ;

   private String execHelp() {
      if( Aladin.levelTrace>0 ) {
         return execHelp+"@test ...\n";
      } else return execHelp+"\n";
   }

   // Liste des commandes scripts documentés
   static final String CMD[] = {
      "addcol","backup","bitpix","blink","call","cm","collapse","conv","contour","coord","copy",
      "cplane","cview","crop","demo","draw","expand","export","filter","moreonfilter","function",
      "flipflop","get","grey","grid","help","hide","hist","info","kernel","list","load","lock",
      "macro","md","mem","northup","match",
      "mosaic","mv","norm","overlay","pause","print","quit","resamp","reset","reticle",
      "RGB","RGBdiff","rm","save","scale","search","select","set","setconf","show",
      "status",/*"stick",*/"sync","tag","thumbnail","trace","unlock",/* "unstick",*/
      "untag","xmatch","moreonxmatch","zoom","+","-","*","/","=",
   };
   
   // Liste des commandes qui ne requierent pas un sync() avant d'être exécutée
   static final private String NOSYNCCMD[] = {
      "call","collapse","demo","expand","function","=",
      "get","grid","help","hist","info","list","kernel","load","lock","md","mem",
      "pause",/*"reset",*/"reticle",
      "scale",/*"setconf",*/
      "status","stick","sync","timeout","trace","unlock","unstick",
   };
   
   /** retourne true si la commande requiert un sync() avant d'être exécutée */
   private boolean needSync(String cmd) {
      return Util.indexInArrayOf(cmd, NOSYNCCMD)== -1;
   }

  /** Creation d'un module de commande.
   * @param aladin la reference habituelle
   */
   Command(Aladin aladin) {
      a=aladin;
      ds9 = new CommandDS9(aladin);
      testRobot();
      if( Aladin.ROBOTSUPPORT ) {
          robot = new MyRobot(a);
          Aladin.trace(3, "Creating robot");
      }
   }

   /** Arrêt forcer du thread de lecture des commandes */
   protected void stop() { stop=true; }
   
   public void printConsole(String s) {
      print(s);
      console(s);
   }
   
   public void console(String s) {
      if( !s.startsWith("!!!") ) a.console.printInfo(s);
      else a.console.printError(s);

   }

   /** Affichage sur le standard output si possible */
   public void println(String s) { print(s+"\n"); }
   public void print(String s) {
      if( Aladin.NOGUI && !s.startsWith("!!!")  ) return;
      if( !Aladin.isApplet() ) System.out.print(s);
   }
   
  /** Lance la lecture asynchrone du standard input */
   protected void readStandardInput() {
      thread = new Thread(this,"AladinReadStdin");
      Util.decreasePriority(Thread.currentThread(), thread);
//      thread.setPriority( Thread.NORM_PRIORITY -1);
      thread.start();
   }

   private InputStream stream=System.in;
   Stack<InputStream> stackStream = new Stack<InputStream>();
   synchronized void setStream(InputStream in ) {
      if( in==null ) {
      	// thomas 02/02/2005
//      	in=(InputStream)(stackStream.empty()?System.in:stackStream.pop()); // original, commenté

      	if( stackStream.empty() ) in = System.in;
      	// thomas 02/02/2005 : le pb du pop() est que dans certains cas, on dépile le stream qu'on venait de consommer entièrement
      	// ce qui provoquait un dépilage en série non désiré (du coup, on perdait certaines commandes !!)
      	else {
      		in = stackStream.pop();
      		if( in==stream && in!=System.in && !stackStream.empty() ) {
      			in = stackStream.peek();
      		}
      	}
      }
      else stackStream.push(in);
      stream=in;
   }

  /** Lance la lecture asynchrone du standard input */
   protected void readFromStream(InputStream in) { setStream(in); }

   /** Module de controle des commandes asynchrones.
    * Il s'agit d'un thread qui lit l'entree standard et execute
    * les commandes qu'il recoit.
    * D'autre part, cette méthode affiche automatique le formulaire des serveurs
    * le cas échéant.
    */
   public void run() {
      a.waitDialog();
//      if( Aladin.BANNER && !Aladin.NOGUI && !a.isLoading() && !a.flagLaunch ) {
//         try { Thread.currentThread().sleep(3000); } catch( Exception e ) {}
//         if( !a.pad.hasHistoryCmd() && a.msgOn ) {
//            if( !a.dialog.isShowing() ) a.dialog.show();
//         }
//      }
//      println("Aladin is waiting commands...");
      scriptFromInputStream();
      a.trace(2,"Command interpreter stopped !");
   }

   private boolean sleepFlag=false;
   synchronized protected void readNow() {

//      // Contournement Bug Windows JAVA 1.1.8 sur le read bloquant après available()==1
//      if( Aladin.BUGPARAD118 ) { execScript(a.pad.popCmd()); return; }

      if( sleepFlag )thread.interrupt();
   }

   synchronized private void setFlagSleep(boolean flag) { sleepFlag=flag; }

   int X = 0;
   
   /** Retourne true s'il y a encore une commande dans le flux d'entrée */
   protected boolean hasCommand() {
      try {
//         System.out.println("stream="+stream+" available="+stream.available()+" isSyncServer="+isSyncServer());
         return stream!=System.in && stream.available()>-1 || stream.available()>0 || !isSyncServer();
      } catch( Exception e ) { e.printStackTrace(); }
      return false;
   }

   // Procedure un peu tordue pour lire une commande provenant
   // d'un flux (STDIN ou autre) ou éventuellement de la console Aladin (Pad)
   private String readLine() {
      StringBuffer s = new StringBuffer();
      boolean encore=true;
      int b=0;
      int acc=0;  // Profondeur de crochets pour éviter les fausses détections de ';' au sein d'une UCD  

      do {
         // Une commande qui provient du pad et prioritaire sur stdin
         if( (stream==null || stream==System.in) &&
               a.console!=null && a.console.hasWaitingCmd() ) return a.console.popCmd();

         // Commandes provenant d'un stream (STDIN et/ou autres)
         try {

            // Petite garantie pour éviter une boucle bloquante en cas de
            // problème sur stream.available (ex. sous Tomcat)
//            if( X>100000 ) Util.pause(1000);
//            X++;

            if( stream==null || stream==System.in && stream.available()==0 ) {
               setFlagSleep(true);
               Util.pause(500);
               setFlagSleep(false);
               continue;
            }
         }catch( Exception e ) { stream=null; continue; }

         try {
            b=stream.read();
//            System.out.println("Read b="+b+"=>"+((char)b)+" from "+stream+" available="+stream.available());
         } catch( Exception e ) {stream=null; continue; }

         // Fin de lecture d'un stream alternatif
         if( stream!=System.in && b==-1 ) {
	        setStream(null);
	        encore=false;
	        
	     // Ajout a la commande courante
         } else {
            s.append((char)b);
            if( b=='[' ) acc++;
            else if( b==']' ) acc--;
         }

      } while( encore && !stop && b!=10 && !(b==';' && acc==0) );
      return s.toString();
   }

   protected String getPrompt() {
//      return filterMode ? "Command - Filter def.> ":
//         "Command> ";
      return filterMode ?  "Filter> ":
             fonct!=null ? "Function> ":
                           "Command> ";
   }
   
   //** Retourne true si Aladin attend la suite de la commande (filtre, fonction....) */
   protected boolean waitingMore() { return !filterMode && fonct==null; }

  /** Lecture d'un script
   * @param dis InputStream
   */
   protected void scriptFromInputStream() {
      String s=null;
      boolean prompt=(stream==System.in);

      if( prompt ) print(getPrompt());
      while(!stop /* && true */) {
         try {
            if( robotMode ) robotSync();
            s = readLine();
            //         System.out.println("===> ["+s+"]");
            if( s==null )  return;

            if( s.trim().length()!=0 ) {
               // thomas : quand on definit un filtre, les lignes de commentaires ne doivent pas être ignorées
               //            if( s.charAt(0)=='#' && !filterMode ) continue;
               execScript(s);
            }
            if( prompt ) print(getPrompt());
         } catch( Exception e ) {
            if( Aladin.levelTrace>=3 ) e.printStackTrace();
            println("!!! "+e);
            e.printStackTrace();
         }
      }
   }

   /**
    * Retourne le numéro de la vue correspondant à son identificateur
    * qui doit suivre la syntaxe A1... D4. La lettre représente la colonne
    * et le chiffre la ligne
    * @param vID identificateur d'une vue
    * @param verbose true si on accepte les messages d'erreur
    * @return numéro de la vue (de 0 à ViewControl.MAXVIEW-1)
    *         ou -1 si problème ou non visible
    */
   protected int getViewNumber(String vID) { return getViewNumber(vID,true); }
   protected int getViewNumber(String vID,boolean verbose) {
      int nview = a.view.getNViewFromID(vID);
      if( verbose && nview<0 ) {
         printConsole("!!! Not a valid view identifier [ex: B2]");
         return -1;
      }
      if( nview>= a.view.getModeView() ) {
         if( verbose ) printConsole("!!! View \""+vID+"\" not visible "+nview);
         return -1;
      }
      return nview;
   }
   
   /** Extraction des indices des colonnes pour un paramètre
    * suivant la syntaxe suivante :  NomPlan(Column1,Column2).
    * S'il n'y a pas de colonne mentionnée, retourne 0,1   
    * @return : le nom de la table sans les parenthèses
    */
   protected String parseColumnIndex(String [] col, String s) {
      col[0]=""; col[1]="";
      int offset = s.indexOf('(');
      int offset1 = s.lastIndexOf(',');
      if( offset==-1 || offset1==-1 || s.charAt(s.length()-1)!=')' ) return s;
      String plane = s.substring(0,offset);
      col[0] = s.substring(offset+1,offset1);
      col[1] = s.substring(offset1+1,s.length()-1);
//      System.out.println("Table=["+plane+"] col1=["+col[0]+"] col2=["+col[1]+"]");
      return plane;
   }
   
   /** Retourne le plan en fonction de son numero, ou de son label
    *  ou d'un masque (jokers).
    * dans la pile (1 etant celui tout en bas).
    * @param s la chaine qui doit contenir le numero
    * @param methode 0 si le s ne peut etre qu'un numero (éventuellement précédé de @)
    *                1 si s peut etre egalement un nom de plan (éventuellement précédé de @),
    *                  ou un masque de nom de plan
    * @param atStrict true si seul le @n est autorisé et non le "n" simple
    * @param verbose
    * @return le plan, ou null si non trouvé
    */
   protected Plan getNumber(String s ) { return getNumber(s,1); }
   protected Plan getNumber(String s,int methode) { return getNumber(s,methode,false,true); }
   protected Plan getNumber(String s,int methode,boolean atStrict,boolean verbose) {
      int n=0;
      boolean at = s.length()>1 && s.charAt(0)=='@'; // préfixe @ sur le numéro du plan

      try{ n = Integer.parseInt(atStrict ? s.substring(1) : at? s.substring(1) : s); }
      catch(Exception e) {}
      Plan [] allPlans = a.calque.getPlans();
      if( n<1 || n>allPlans.length ) {
         if( methode==1 && !at ) {
            n=a.calque.getIndexPlan(s);
            if( n<0 ) {
               if( verbose ) printConsole("!!! Plane \"" + s + "\" not found !");
               return null;
            }
            return allPlans[n];
         }
         if( verbose ) printConsole("!!! Plane number error ("+s+")");
         return null;
      }
      n=allPlans.length-n;
      if( allPlans[n].type==Plan.NO ) {
         if( verbose ) printConsole("!!! Plane number "+s+" not assigned");
         return null;
      }
      return allPlans[n];
   }

   /** Retourne le plan indiqué dans le paramètre, soit par son nom, soit
    * par son numéro dans le stack, soit par l'identificateur d'une vue
    * (sauf si  method==1.
    * Retourne null si problème */
   private Plan getPlanFromParam(String s) { return getPlanFromParam(s,0,false); }
   private Plan getPlanFromParam(String s,int method) { return getPlanFromParam(s,method,false); }
   private Plan getPlanFromParam(String s,int method,boolean atStrict) {
      Plan p=null;

      // Un plan via un identificateur de vue ?
      if( method == 0 ) {
         int nview=getViewNumber(s,false);
         if( nview >= 0 ) {
            ViewSimple vtmp=a.view.viewSimple[nview];
            if( !vtmp.isFree() ) p=vtmp.pref;
         }
      }

      // Un plan par son nom ou son numéro ?
      if( p == null ) {
         p = getNumber(s,1,atStrict,false);
//         int n=getNumber(s,1,atStrict,false);
//         if( n >= 0 ) p=a.calque.plan[n];
      }
      return p;
   }


   private PlanImage[] getPlanImage(String param) {
      Plan p[] = getPlan(param,1);
      PlanImage pi[] = new PlanImage[p.length];
      System.arraycopy(p,0,pi,0,p.length);
      return pi;
   }

   /** Retourne le premier plan qui correspond à l'identificateur (label, wildcard, id de vue)
    * @param planID désignation du plan
    * @return le plan, ou null si aucun qui correspond.
    */
   protected Plan getFirstPlan(String planID) {
      if( planID.indexOf(' ')>=0 && planID.indexOf('"')<0 ) planID= '"'+planID+'"';
//System.out.println("planID=["+planID+"]");
      Plan p[] = getPlan(planID);
      if( p.length==0) return null;
      return p[0];
   }

   /** Construit un tableau des plans spécifiés dans param. si
    * param est "", prend les plans sélectionnés
    * @param method 0: tous les plans prêts
    *               1: tous les plans images simples prêts
    *               2: tous les plans prêts sans prendre en compte les
    *                  identificateur de vue (ex: B2)
    *               3: tous les plans Folder
    *               4: Idem que 2 mais également les plans non prêts
    */
   protected Plan[] getPlan(String param) { return getPlan(param,0); }
   protected Plan[] getPlan(String param,int method) {
      Vector<Plan> v = new Vector<Plan>();

      param=param.trim();

      // Les plans images sélectionnés dans la pile
      Plan [] allPlan = a.calque.getPlans();
      if( param.length()==0 ) {
         for( int i =0; i<allPlan.length; i++ ) {
            Plan p = allPlan[i];
            if( method==1 && ! p.hasAvailablePixels()) continue;
            if( p.flagOk && p.selected ) v.addElement(p);
         }

      // Les plans images spécifiés par le paramètre
      } else {
         Tok st = new Tok(param);
         while( st.hasMoreTokens() ) {
            String s = st.nextToken();
            Plan p = getPlanFromParam(s,method==2?1:0);
            if( p==null ) continue;
            if( method==1 && !p.hasAvailablePixels()) continue;
            if( method==3 && p.type!=Plan.FOLDER ) continue;
            if( p!=null ) {
               if( p.flagOk || method==4 ) v.addElement(p);
            }
         }
      }

      return v.toArray(new Plan[v.size()]);
   }

//   protected boolean isSyncServer1() {
//      for( int i=0; i<a.dialog.server.length; i++ ) {
//         if( !a.dialog.server[i].isSync() ) {
//            Aladin.trace(4,"Command.isSyncServer() : waiting server["+i+"] \""+a.dialog.server[i].aladinLabel+"\"...");
//            return false;
//         }
//      }
//      return true;
//   }
   /** Retourne true si tous les serveurs sont syncrhonisés */
   protected boolean isSyncServer() {
      if( a.synchroServer.isReady() ) return true;
      Aladin.trace(4,"Command.isSyncServer() : waiting server...\n" +
      		         "==> "+a.synchroServer);
      return false;
   }
   
   /** Retourne true si tous les plugins sont syncrhonisés */
   protected boolean isSyncPlugin() { 
      if( a.plugins==null ) return true;
      boolean rep=a.plugins.isSync();
      if( !rep ) Aladin.trace(4,"Command.isSyncPlugin() : waiting a plugin...\n");
      return rep;
   }
   
   /** Retourne true si tous les plans sont syncrhonisés */
   protected boolean isSyncPlan() {
      if( a.synchroPlan.isReady() ) return true;
      Aladin.trace(4,"Command.isSyncPlan() : waiting plane...\n" +
                     "==> "+a.synchroPlan);
      return false;
   }
   
   /** Retourne true si tous les serveurs sont syncrhonisés */
   protected boolean isSyncSave() {
      if( syncSave.isReady() ) return true;
      Aladin.trace(4,"Command.isSyncSave() : waiting save process...\n" +
                     "==> "+syncSave);
      return false;
   }
   


   protected boolean syncNeedRepaint=false;
   protected boolean syncNeedSesame=false;
   
  /** Retourne vrai si tous les plans sont flagOk et que tous les serveurs
   * sont ok */
   protected boolean isSync() {
      if( a.dialog==null || a.calque==null ) {
         a.trace(4,"Command.isSync() : waiting (Aladin.dialog not ready)...");
         return false;
      }
      
      if( syncNeedSesame && a.view.isSesameInProgress() ) {
         a.trace(4,"Command.isSync() : waiting sesame...\n" +
         		   "==> "+a.view.sesameSynchro);
         return false;
      }
      syncNeedSesame=false;
      
      if( syncNeedRepaint ) {
         a.trace(4,"Command.isSync() : waiting viewSimple.paintComponent()...");
         a.view.repaintAll();
         return false;
      }
      
      if( !isSyncServer() ) return false;
      if( !isSyncPlan() ) return false;
      if( !isSyncPlugin() ) return false;
      if( !isSyncSave() ) return false;
      
//      if( !a.calque.isPlanBGSync() ) return false;
      
      Plan [] plan = a.calque.getPlans();
      for( int i=plan.length-1; i>=0; i-- ) {
         if( plan[i].type!=Plan.NO && !plan[i].isSync()  ) {
            a.trace(4,"Command.isSync() : waiting plane \""+plan[i]+"\"...");
            if( plan[i].label==null ) {
               System.err.println("isSync label==null : type="+plan[i].type+" state="+plan[i].getDebugFlag());
            }
//            if( Aladin.NOGUI && plan[i] instanceof PlanBG && !((PlanBG)plan[i]).isFullyDrawn() ) {
//               syncNeedRepaint=true;
//            }
            return false;
         }
      }
      
//      System.out.println("All is sync");
      return true;
   }

   private boolean killSync = false; // true si on doit tuer le sync courant
   /**
    * tue le "sync" en cours
    * Ne fait rien s'il n'y a pas de synchronisation en cours
    *
    */
   protected void killSync() {
      if( !inSync ) return;
      killSync = true;
   }

   private boolean inSync = false; // true si un sync est en cours
   
   
   // Les différents modes de synchronisation des plans
   final protected static int SYNCOFF = 0;
   final protected static int SYNCON  = 1;
   protected int syncMode = SYNCON;   // mode de synchronisation courant
   
   /** Positionnement du mode de synchronisation, SYNCON = automatique,
    * SYNCOFF = explicite par la commande sync */
   protected void setSyncMode(int mode) { syncMode=mode; }

  /** Attend que tous les plans soient Ok */
   public void sync() {
   	  inSync = true;
      if( robotMode ) {
          robotSync();
          Util.pause(1000);
      }
      long d = System.currentTimeMillis();
      while( !isSync() ) {
      	 if( killSync ) {
      	 	killSync = false;
            println("!!! Command sync has been killed !");
            inSync = false;
            return;
      	 }
         if( timeout>0 && System.currentTimeMillis()-d>timeout ) {
            println("!!! Time out error ("+(timeout/60000)+" minutes).");
            inSync = false;
            return;
         }
         Util.pause(100);
      }
      inSync = false;
   }

   private void syncServer() { 
      try {
         a.synchroServer.waitUntil(timeout);
      } catch( Exception e ) {
         println("!!! Time out error ("+(timeout/60000)+" minutes).");
      }
   }

   /** teste si robot est supporté, et fixe la valeur de Aladin.ROBOTSUPPORT */
   private void testRobot() {
       try {
            Class<?> x = Class.forName("java.awt.Robot");
            Constructor<?> cons = x.getConstructor(new Class[]{});
            cons.newInstance(new Object[] {});
           // robot supporte !
           Aladin.trace(3, "Robot supported");
           Aladin.ROBOTSUPPORT = true;
       }
       catch(Exception e) {
           // robot non supporte
           Aladin.trace(3, "Robot NOT supported");
           Aladin.ROBOTSUPPORT = false;
       }
   }

   /** Attends que l'action soit exécutée */
    private void robotSync() {
       long d = System.currentTimeMillis();
       while( !ActionExecutor.ready || ActionExecutor.interruptAction ) {
          if( timeout>0 && System.currentTimeMillis()-d>timeout ) {
             println("!!! Time out error ("+(timeout/60000)+" minutes).");
             return;
          }
          Util.pause(500);
       }
       Util.pause(1000);
    }

   static String[] UNIT = { "m","arcmin","'",
                            "s","sec","\"",
                            "d","deg","°" };

  /** Affichage du status */
   private String execStatusCmd(String param) {
      String status = getStatus(param);
      println(status);
      return status;
   }
   
   /** Retourne le status */
   protected String getStatus(String param) {
      StringBuffer res =new StringBuffer();
      Plan p[];
      if( param==null || param.equals("") || param.equals("*") || param.equals("stack")) p = a.calque.getPlans();
      else p = getPlan(param,2);
      for( int i=0; i<p.length; i++ ) {
         Plan plan = p[i];
         if( plan.type==Plan.NO ) continue;
         if( res.length()>0 ) res.append("\n");
         res.append("PlaneID "+Tok.quote(plan.label)+"\n"
               + "PlaneNb "+(p.length-a.calque.getIndex(plan))+"\n"
               + "Type    "+Plan.Tp[plan.type]+"\n"
               + (plan.verboseDescr!=null ? "Info    "+plan.verboseDescr+"\n" : "")
               + (plan.ack!=null ? "Ack     "+plan.ack+"\n" : "")
               + "Status  "+(plan.active ? "shown":"hidden")+" "
                           +(plan.selected ? "selected":"")
                           +(!plan.flagOk && plan.error!=null ? " error":"")
                           +"\n"
               + (plan.u!=null  ? "Url     "+plan.getUrl()+"\n" : "")
               + (plan.co!=null ? "Target  "+plan.getTarget()+"\n" : "")
         );
         if( plan.colorBackground!=null )  res.append("Background "+(plan.colorBackground==Color.white ? "white": "black")+"\n");
         if( plan.type==Plan.APERTURE ) {
            res.append("Roll    "+((PlanField)plan).getRoll()+"\n");
            res.append("Rollable "+((PlanField)plan).isRollable()+"\n");
            res.append("Movable  "+((PlanField)plan).isMovable()+"\n");
            String fov = ((PlanField)plan).getStatusSubFov();
            if( fov!=null ) res.append(fov);
         } else if( plan.isCatalog() ) {
            res.append("NbObj   "+plan.getCounts()+"\n");
            res.append("Shape   "+Source.getShape(plan.sourceType)+"\n");
            res.append("Filter  "+((PlanCatalog)plan).getFilterDescription()+"\n");
            res.append("Color   "+Action.findColorName(plan.c)+"\n");
         } else if( plan.type==Plan.TOOL ) {
            res.append("Color   "+Action.findColorName(plan.c)+"\n");
            res.append("Movable "+(plan.isMovable()?"on":"off")+"\n");
         } else if( plan.type==Plan.IMAGE || plan.type==Plan.IMAGEHUGE ) {
            res.append("Width   "+((PlanImage)plan).naxis1+"\n"
                     + "Height  "+((PlanImage)plan).naxis2+"\n"
            );
         } else if( plan.type==Plan.FOLDER ) {
            res.append("Scope   "+(((PlanFolder)plan).localScope?"local":"global")+"\n");
            String item = ((PlanFolder)plan).getStatusItems();
            if( item!=null ) res.append(item);
         }

        if( a.calque.planeTypeCanBeTrans(plan) ) {
            res.append("Opacity "+Util.myRound(plan.getOpacityLevel()*100+"",0)+"\n");
        }
      }
      
      // statut des vues (soit par leur ID, soit toutes si aucune spécification)
      StringBuffer x = new StringBuffer();
      if( param==null || param.equals("") || param.equals("*")|| param.equals("views") ) x = a.view.getStatus();
      else {
         Tok tok = new Tok(param);
         int n;
         while( tok.hasMoreTokens() ) {
            String s = tok.nextToken();
            if( (n=getViewNumber(s,false))!=-1 && !a.view.viewSimple[n].isFree() ) {
               x.append( a.view.viewSimple[n].getStatus()+"\n");
            }
         }
      }
      if( x.length()>0 ) res.append("\n"+x);

      return res.toString();
   }

  /** Extraction d'un radius donne en dernier champ d'un target.
   * Exemple de syntaxe: "10 20 30 +40 50 60 12arcmin"
   * @param radius le radius qui sera trouve
   * @param tX le target qui en sera deduit
   * @param t la chaine a analyser
   * @return true si on trouve effectivement un radius indique
   */
  private boolean extractRadius(StringBuffer radius,StringBuffer tX,String t) {
     char a[] = t.toCharArray();
     int i,j;


     // Extraction du dernier mot
     for( i=a.length-1; i>0 && (a[i]<'0' || a[i]>'9'); i--);
     String unit = new String(a,i+1,a.length-i-1);

     // Il ne s'agit pas d'une unite
     for( j=0; j<UNIT.length && !unit.equalsIgnoreCase(UNIT[j]); j++ );
     if( j==UNIT.length ) return false;

     // Recuperation de la valeur du rayon
     for( j=i; j>0 && a[j]!=' '; j--);
     if( j==0 ) return false;
     String s = new String(a,j+1,a.length-j-1);

     // Petit ajustement
     if( s.endsWith("'") ) s = s.substring(0,s.length()-1)+"m";
     else if( s.endsWith("\"") ) s = s.substring(0,s.length()-1)+"s";

     radius.append(""+Server.getAngle(s,Server.RADIUS));
     tX.append( new String(a,0,j));
     return true;
  }

  /** Retourne true s'il s'agit d'une commande script */
  protected boolean isCommand(String c) {
     return Util.indexInArrayOf(c, CMD, true)>=0;
  }

  static final private String OP = "+-*/";

  /** Cherche un éventuel opérateur arithmétique
   * en vérifiant que les opérandes potentielles sont bien des noms de plans
   * ou des scalaires.
   * @return la position de l'opérateur, 0 si unaire, -1 si pas trouvé
   */
  private int findPosAlgebre(String cmd) {
     for( int i=0; i<OP.length(); i++ ) {
        char op = OP.charAt(i);
        int pos=-1;
        while( (pos=cmd.indexOf(op,pos+1))!=-1) {
           double n1=Double.NaN,n2=Double.NaN;

           String p2 = cmd.substring(pos+1).trim();
           try { n2 = Double.parseDouble(p2); } catch( Exception e) {}
           boolean f2 = getNumber(Tok.unQuote(p2),1,true,false)!=null;
           if( pos==0 && (!Double.isNaN(n2) || f2) ) {
//System.out.println("Trouvé algèbre unaire pos="+pos+" ["+op+p2+"]");
              return pos;  // Cas unaire
           }

           String p1 = cmd.substring(0,pos).trim();
           try { n1 = Double.parseDouble(p1); } catch( Exception e) {}
           boolean f1 = getNumber(Tok.unQuote(p1),1,true,false)!=null;
           if( (!Double.isNaN(n1) || f1) && (!Double.isNaN(n2) || f2)
                 && !(!Double.isNaN(n1) && !Double.isNaN(n2)) ) {
//System.out.println("Trouvé algèbre binaire pos="+pos+" ["+p1+op+p2+"]");
              return pos;
           }
        }
     }
     return -1;
  }

  /** Extrait le nom d'un plan qui aurait été spécifié en préfixe d'une commande.
   * Retourne la commande sans le préfixe. Le nom du plan peut être éventuellement
   * quoté, il sera automatiquement "déquoté".
   * De plus, en profite pour ajouter des blancs autour d'un opérateur arithmétique
   * éventuel
   * Ex : toto = get aladin(dss1) m1
   * => targetPlane = toto
   * => return : get aladin(dss1) m1
   * @param targetPlane Le nom du plan, ou inchangé si non spécifié
   * @param cmd la commande avec un éventuel préfixe "nomplan = "
   * @return la commande sans son préfixe, avec des blancs autour d'opérateur arith.
   */
   protected String getTargetPlane(StringBuffer targetPlane,String cmd) {
      String s;
      int pos = cmd.indexOf('=');
      if( pos==-1 ) s=cmd;
      else s = cmd.substring(pos+1).trim();

      // S'il y a des blancs dans le nom de plan et pas de quotes au début
      // c'est qu'il s'agit d'une commande du genre: get File(http://xxx?toto=bidule)
      String name = pos==-1 ? "" : cmd.substring(0,pos).trim();
      if( name.indexOf(' ')>0 && name.charAt(0)!='"' && name.charAt(0)!='\'' ) {
         return cmd;
      }

      // Peut être faut-il mettre des blancs autour d'un opérateur arithmétique
      int i;
      if( findAlgebre(s)<0 && (i=findPosAlgebre(s))>=0 ) {
         if( i==0 ) s = s.charAt(i)+" "+s.substring(i+1);
         else s = s.substring(0,i)+" "+s.charAt(i)+" "+s.substring(i+1);
      }

      // On continue sur la recherche d'un nom de plan destination "toto=..."
      if( pos==-1 ) return s;
      String c = (new Tok(s)).nextToken();
      if( !isCommand(c) && findAlgebre(s)<0
                        && getNumber(s,1,false,false)==null ) return cmd;
      targetPlane.append(Tok.unQuote(cmd.substring(0,pos).trim()));
      return s;
   }

  /** Decoupage des champs d'une commande "get [serveur...] target [radius]" */
   protected boolean splitGetCmd(StringBuffer servers, StringBuffer target,
                               StringBuffer radius,String cmd,boolean withServer) {
       char b[]=cmd.toCharArray();
       int d,i;
       int inPar;	// Niveau de parenthesage

       // chargement des records GLU additionels
       if( withServer && (servers.indexOf("IVOA")>=0 || cmd.indexOf("IVOA")>=0)
               && ! a.dialog.ivoaServersLoaded ) {
           a.dialog.appendIVOAServer();
       }
       
       // On passe les espaces en debut de chaine
       for( d=0; d<b.length && b[d]==' '; d++);

       // Parcours de tous les serveurs
       for( inPar=0, i=d; i<b.length; i++ ) {
          if( inPar==0 ) {
             if( b[i]=='(' ) inPar++;
             else if( b[i]==' ' ) break;
          } else if( b[i]==')' ) inPar--;
       }

       // Memorisation temporaire
       String s = new String(b,d,i-d);

       // On passe les espaces apres les serveurs
       for( d=i; d<b.length && b[d]==' '; d++);

       // On prend tout le reste pour le target
       String t = (new String(b,d,b.length-d)).trim();

       // Si le premier serveur n'existe pas, il s'agit sans
       // doute uniquement d'un target
       StringTokenizer st = new StringTokenizer(s,",(");
       if( !withServer || a.dialog.getServer(st.nextToken())<0 ) {

          // Si la vue courante est vide il faut prendre
          // la liste des serveurs par defaut
          if( a.view.getCurrentView().isFree() /* || a.isFullScreen() */ ) {
             t=cmd;
             if( Aladin.OUTREACH || a.isFullScreen() ) s="allsky(\"DSS colored\")";
             else {
                s=a.configuration.getServer();
                if( s==null || s.trim().length()==0 ) s="allsky";
                String p = a.configuration.getSurvey();
                if( p!=null && p.trim().length()>0 ) s=s+"("+p+")";
             }
             
             String rep = a.view.sesameResolve(cmd,true);
             if( rep==null ) {
                a.warning(a.dialog.server[0].UNKNOWNOBJ);
                return false;
             } else t=rep;
             a.console.printCommand("get "+s+" "+cmd);

          // sinon il s'agit d'un simple deplacement du repere
          } else {
             syncNeedRepaint=true;
             syncNeedSesame=true;
             
             // Via une adresse healpix norder/npi
             if( execHpxCmd(cmd) ) return false;
             
             // ou via une position ou une target
             a.view.sesameResolve(cmd);
             a.dialog.setDefaultTarget(cmd);
             
             return false;
          }
       }

       // On memorise les serveurs
       servers.append(s);

//       // Si le target n'est pas mentionne,
//       // on prend la position du dernier repere dans View
//       if( t.length()==0 ) {
//          waitingPlanInProgress();
//          t=a.dialog.getDefaultTarget();
//
//       // Sinon on regarde si le dernier champ ne serait pas un radius
//       } else {
          
       if( t.length()!=0 ) {
          StringBuffer tX = new StringBuffer();
          if( extractRadius(radius,tX,t) ) {
             t=tX.toString();
             a.dialog.setDefaultTaille(radius.toString());
          }

          // On mémorise le target comme nouveau target par défaut
          a.dialog.setDefaultTarget(t);
       }

       target.append(t);

       // On recherche le rayon par défaut si nécessaire
       if( radius.length()==0 ) {
          waitingPlanInProgress();
          radius.append(Server.getRM(a.dialog.getDefaultTaille())+"'");
       }

       return true;
   }

   /** Attend que les plans en cours de chargement aient pu positionner le target
    * par défaut afin d'éviter un sync() */
   private void waitingPlanInProgress() {
      return;

      // POUR LE MOMENT JE NE LE METS PAS EN PLACE CAR J'AI PEUR DE MAUVAISES SURPRISES
//      boolean encore;
//      long t = System.currentTimeMillis();
//      synchronized( a.calque ) {
//         do {
//            encore=false;
//            Plan p=null;
//            for( int i=0; i<a.calque.plan.length; i++ ) {
//               p = a.calque.plan[i];
//               if( !p.flagOk && p.flagWaitTarget ) {
//                  encore=true;
//                  break;
//               }
//            }
//            if( encore ) {
//               // Au cas où ça tarde trop, je sors quand même
//               //TODO IL FAUDRA QUE J'AUGMENTE LE TIMEOUT A 1MN
//               if( t-System.currentTimeMillis()>30000 ) {
//                  System.err.println("Command.waitingFileInProgress timeout");
//                  return;
//               }
//               System.out.println("J'attends le plan "+(p!=null ? p.label : "null"));
//               Util.pause(100);
//            }
//         } while( encore);
//      }
   }

   /** Decoupage du serveur courant et de ses criteres eventuels */
    protected int getServerInfo(StringBuffer server,StringBuffer criteria,
                              char a[],int i) {
      int inPar;	// Niveau de parenthesage
      int d;

      for( d=i, inPar=0; i<a.length; i++ ) {
//System.out.println("a["+i+"]="+a[i]+" inPar="+inPar);
          if( inPar==0 ) {
             if( a[i]=='(' ) {
                inPar++;
                server.append(new String(a,d,i-d));
                d=i+1;
             } else if( a[i]==',' ) break;
          } else if( a[i]==')' ) {
                    inPar--;
                    criteria.append(new String(a,d,i-d));
                 }
       }
       if( server.length()==0 ) server.append(new String(a,d,i-d));
       if( i<a.length && a[i]==',' ) i++;
       return i;
    }

   /** retourne false si la ligne designant les serveurs ne contient
    * que le serveur Local(...) ou MyData(...) ou File(...) ou Aladin(allsky),
    * càd n'a pas besoin de désignation de target
    */
    private boolean isTargetRequired(StringBuffer s) {
      char b[]=s.toString().toCharArray();
      StringBuffer serverX= new StringBuffer();
      StringBuffer criteriaX= new StringBuffer();
      int i=0;
      i=getServerInfo(serverX,criteriaX,b,i);
      String server = serverX.toString().trim();
      return ! (server.equalsIgnoreCase("Local")
            || server.equalsIgnoreCase("MyData")
         || server.equalsIgnoreCase("VizieRX")
         || (server.equalsIgnoreCase("VizieR")
                && Util.indexOfIgnoreCase(criteriaX.toString(),"allsky")>=0)
         || server.equalsIgnoreCase("allsky")
         || (server.equalsIgnoreCase("Aladin")
                && Util.indexOfIgnoreCase(criteriaX.toString(),"allsky")>=0)
		 || server.equalsIgnoreCase("File") ) && i==b.length;
    }

  /** Execution d'une commande get */
   protected String execGetCmd(String cmd,String label,boolean withServer) {
      StringBuffer targetX=new StringBuffer();
      StringBuffer radiusX=new StringBuffer();
      StringBuffer serversX=new StringBuffer();
      StringBuffer erreur=new StringBuffer();	 // Liste des erreurs
      
      // Extraction des trois champs de la commande
      if( !splitGetCmd(serversX,targetX,radiusX,cmd,withServer) ) return null;
      String target = a.localisation.getICRSCoord(targetX.toString().trim());
      String radius = radiusX.toString();

      if( isTargetRequired(serversX) ) {
         if( target.length()==0 ) {
            if( syncMode==SYNCON ) sync();
            target=a.dialog.getDefaultTargetJ2000();
         }
         if( target.length()==0 ) {
            Aladin.warning(a.chaine.getString("WNEEDOBJ"),1);
            return null;
         } else a.dialog.setDefaultTarget(target);

         // On verifie immediatement que l'identificateur est bien
         // reconnu par Simbad
         if( View.notCoord(target) ) {
            int csr = Aladin.WAITCURSOR;
            Aladin.makeCursor(a,csr);
            Coord coo=null;
            try {
               coo = a.view.sesame(target);
               csr = Aladin.DEFAULTCURSOR;
               Aladin.makeCursor(a,csr);
               if( coo==null ) Aladin.warning("\""+target+"\": "+a.chaine.getString("OBJUNKNOWN"),1);
            } catch( Exception e ) { Aladin.warning(e.getMessage(),1); }
            if( coo==null )  return null;
         }
      }

      // Traitement des serveurs un par un
      char b[]=serversX.toString().toCharArray();
      int i=0,j;
      do {
         StringBuffer serverX = new StringBuffer();
         StringBuffer criteriaX = new StringBuffer();
         i=getServerInfo(serverX,criteriaX,b,i);

         String server = serverX.toString();
         String criteria = criteriaX.toString();
Aladin.trace(4,"Command.execGetCmd("+cmd+","+label+") => server=["+server+"] criteria=["+criteria+"] target=["+target+"] radius=["+radius+"])");
         if( server.equalsIgnoreCase("VizierX") ) server="VizieR";   // Pour charger tout un catalogue sans poser un problème de compatibilité


         if( (j=a.dialog.getServer(server))>=0 ) {
            a.dialog.server[j].flagToFront=false;	// Pour eviter le toFront d'Aladin
            int n=a.dialog.server[j].createPlane(target,radius,criteria,label,a.dialog.server[j].institute);
            if( n!=-1 ) {
               a.calque.getPlan(n).setBookmarkCode("get "+server+(criteria.length()>0?"("+criteria+")":"")+" $TARGET $RADIUS");
            }
            if( a.isFullScreen() ) a.fullScreen.repaint();
         } else {
            if( erreur.length()>0 ) erreur.append(", ");
            erreur.append(server);
         }

      } while( i<b.length );

      // Affichage des erreurs
      if( erreur.length()>0 ) {
         if( !Aladin.NETWORK ) Aladin.warning("No network for: "+erreur,1);
         else Aladin.warning(a.chaine.getString("WERROR")+" "+erreur,1);
      }

      return "";
   }
   
   protected HipsGen hipsgen=null;            // pour la génération des allskys via commande script
   
   /** Lancement via une commande script de la génération d'un allsky */
//   protected void execSkyGen(String param)  {
//      try {
//         Tok tok = new Tok(param);
//         String [] arg = new String[ tok.countTokens() ];
//         for( int i=0; i<arg.length; i++ ) arg[i] = tok.nextToken();
//         
//         
//         // Interruption d'une exécution précédente en cours
//         if( Util.indexOfIgnoreCase(param, "abort")>=0 || Util.indexOfIgnoreCase(param, "pause")>=0 
//               || Util.indexOfIgnoreCase(param, "resume")>=0) {
//            Context context = skygen!=null && skygen.context!=null && skygen.context.isTaskRunning() ? skygen.context : null;
//            if( context==null ) throw new Exception("There is no running skygen task");
//            if( Aladin.NOGUI )  skygen.execute(arg);
//            else skygen.executeAsync(arg);
//            return;
//         }
//         
//         if( skygen!=null && skygen.context!=null && skygen.context.isTaskRunning() ) {
//            throw new Exception("There is already a running skygen task !");
//         }
//         skygen = new SkyGen();
//         if( Aladin.NOGUI )  skygen.execute(arg);
//         else skygen.executeAsync(arg);
//      } catch( Exception e ) {
//         if( a.levelTrace>=3 ) e.printStackTrace();
//         a.warning("skygen error !"+e.getMessage()+"\n",1);
//      }
//   }
   
   /** Lancement d'une macro par script */
   protected void execMacro(String param) {
      MyInputStream scriptStream = null;
      MyInputStream paramStream = null;
      try {
         Tok tok = new Tok(param);
         
         // Récupération des lignes de commandes de la macro
         String scriptFile = a.getFullFileName(tok.nextToken());
         scriptStream = (new MyInputStream(Util.openAnyStream(scriptFile))).startRead();
         String s;
         Vector<String> v = new Vector<String>(100);
         while( (s=scriptStream.readLine())!=null ) {
            String s1 = s.trim();
            v.addElement(s1);
         }
         Object [] cmd = v.toArray();
          
         // Instanciation du controler de macro
         MacroModel macro = new MacroModel(a);
         
         // Récupération des paramètres de la macro
         String paramFile = a.getFullFileName(tok.nextToken());
         paramStream = (new MyInputStream(Util.openAnyStream(paramFile))).startRead();
         HashMap params = new HashMap();
         while( (s=paramStream.readLine())!=null ) {
            int offset=-1;
            int i,deb;
            for( deb=0,i=1; (offset=s.indexOf('\t',offset+1))!=-1; i++, deb=offset ) {
               String s1 = s.substring(deb,offset).trim();
               params.put("$"+i,s1);
            }
            params.put("$"+i,s.substring(deb).trim());
            
            for( i=0; i<cmd.length; i++ ) macro.executeScript((String)cmd[i], params);
         }
      } catch( Exception e ) {
         if( a.levelTrace>=3 ) e.printStackTrace();
         a.warning("macro error !"+e.getMessage()+"\n",1);
      } 
      finally {
         if( scriptStream!=null ) try { scriptStream.close(); } catch( Exception e) {}
         if( paramStream!=null )  try { paramStream.close(); }  catch( Exception e) {}
      }
   }
   
   /** Execution d'une commande setconfig propertie = value*/
   protected String execSetconfCmd(String param) {

      int egaleOffset = param.lastIndexOf('=');
      if( egaleOffset==-1 ) return null;
      String propertie  = param.substring(0,egaleOffset).trim();
      String value = param.substring(egaleOffset+1).trim();


Aladin.trace(4,"Command.execSetconfCmd("+param+") => prop=["+propertie+"] value=["+value+"])");

      if( propertie.equalsIgnoreCase("bookmarks") ) {
         a.bookmarks.setBookmarkList(value);
         return "";
      }
      
      else if( propertie.equalsIgnoreCase("timeout") ) {
         int n;
         if( param.equals("off") ) n=0;
         else n=Integer.parseInt(param);
         timeout=n*60000;
         return "";
      }

      else if( propertie.equalsIgnoreCase("overlays") ) {
         a.calque.setOverlayList(value);
         a.calque.repaintAll();
         return "";
      }

      try { a.configuration.setconf(propertie,value); }
      catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         printConsole("!!! setconf error: "+e.getMessage());
         return e.getMessage();
      }
      
      // On met automatiquement le mode de drawing correspondant au frame courant
      if( propertie.equalsIgnoreCase("frame") ) {
         if( Util.indexOfIgnoreCase(value, "XY")>=0 ) setDrawMode(DRAWXY);
         else setDrawMode(DRAWRADEC);
      }

      return "";
   }


   /** Execution d'une commande set [PlaneNames*][(specifications)] propertie = value*/
   protected String execSetCmd(String param) {

      // Recupération de la valeur (NE DOIT PAS CONTENIR LE SIGNE =)
      int egaleOffset = param.lastIndexOf('=');
      if( egaleOffset==-1 ) return null;
      String value = param.substring(egaleOffset+1).trim();

      // Récupération de la Propertie
      char b [] = param.toCharArray();
      int i;
      for( i=egaleOffset-1; i>0 && b[i]==' '; i--);	// on skip les blancs avant le "="
      for( ; i>0 && b[i]!=' '; i--);	// on recule sur Propertie
      String propertie = param.substring(i,egaleOffset).trim();

      // Dans le cas d'un plan APERTURE, il peut y avoir une specification de FoV
      // ex: set "HST 32/FGS2" Status=shown (on ne supporte alors qu'un seul nom de plan)
      String specif=null;
      int j;
      for( j=i; j>0 && b[j]!='/'; j--);
      if( b[j]=='/' )  {

         // Bidouillage en cas de quotes englobant "PLAN X/SPECIF" pour obtenir "PLAN X"
         if( b[i-1]=='"' ) {
            specif=param.substring(j+1,i-1);
            b[j]='"';
            i=j+1;
         } else {
            specif=param.substring(j+1,i);
            i=j;
         }
      }

      // Récupération de la désignation des plans concernés
      String plans = (new String(b,0,i)).trim();
//      String plans = param.substring(0,i).trim();

Aladin.trace(4,"Command.execSetCmd("+param+") =>plans=["+plans+"] "
      +(specif!=null ? "specif=["+specif+"]":"")
      +"prop=["+propertie+"] value=["+value+"]");

      Plan [] p = getPlan(plans,2);

      // Test qu'il n'y a qu'un plan concerné dans le cas d'une spécification "/xxx"
      if( specif!=null && p.length>1 ) {
         String s = "set error: only suppport one plane with \"/\" specification";
         printConsole("!!! set error: "+s);
         return s;
      }

      for( i=0; i<p.length; i++ ) {
         try { p[i].setPropertie(propertie,specif,value);}
         catch( Exception e ) {
            printConsole("!!! set error: "+e.getMessage());
            return e.getMessage();
         }
      }
      a.calque.repaintAll();

      return "";
   }

   /** Exécution des paramètres de changement de la palette et/ou de l'autocut
    * @param param [PlanID] [cmParam...]
    * @return "" si ok, sinon le message d'erreur
    */
   protected String execCM(String param) {
      Plan p;
      boolean defaultPlan=true;
      Vector<Plan> v = new Vector<Plan>(10);
      String s=null;
      boolean ok=false;

      // Analyse des plans passés en paramètre
      Tok tok = new Tok(param);
      while( tok.hasMoreTokens() ) {
         s = tok.nextToken();
         p = getPlanFromParam(s);
         if( p==null ) break;
         if( !p.hasAvailablePixels() ) { defaultPlan=false; continue; }
         v.add(p);
      }

      // Si aucun plan indiqué, on essaye le plan de base
      if( v.size()==0 && defaultPlan ) {
         p = a.calque.getPlanBase();
         if( p!=null ) v.add(p);
      }

      // Aucun plan
      if( v.size()==0 ) return "No image plane";

      // On ré-empile tous les paramètres qui suivent le noms des plans
      StringBuffer par = new StringBuffer();
      while( s!=null ) {
         par.append(" "+s);
         if( tok.hasMoreTokens() ) s=tok.nextToken();
         else s=null;
      }
      s=par.toString();

      // Action par défaut
      if( s.length()==0 ) {
         s = a.configuration.get(Configuration.CM);
         if( s==null ) s="gray reverse autocut";
         s = s+ " all";
      }

      // On applique chaque paramètre pour chaque plan
      Enumeration<Plan> e = v.elements();
      while( e.hasMoreElements() ) {
         PlanImage pi = (PlanImage)e.nextElement();
         ok |= pi.setCmParam(s);
      }

      if( ok ) a.calque.repaintAll();

      return "";
   }

//  // voir getDrawParam();
//   private int getOneDrawParam(StringBuffer p,char a[],int i) {
//      boolean flagText,bslash;
//      boolean first=true;
//
//      for( bslash=flagText=false; i<a.length; i++) {
//         if( !flagText) {
//            if( a[i]==',' || a[i]==' ' || a[i]==')' ) { i++; break; }
//            else if( first && a[i]=='"' ) { flagText=true; continue; }
//         } else {
//            if( a[i]=='"' ) {
//               if( !bslash ) { i++; break; }
//            }
//            if( a[i]=='\\' ) { bslash=true; continue; }
//            else bslash=false;
//         }
//         p.append(a[i]);
//         first=false;
//      }
//      return i;
//   }

  /** Decoupe dans un tableau de String une ligne de parametres separes
   * par des , ou blancs. Les parametres peuvent etre
   * des chaines de caracteres qui, si elles ont des blancs, doivent
   * etre enquotees par ", (avec \" si exception)
   * Utilise getOneDrawParam().
   */
//   private String [] getDrawParam(String param) {
//      if( param.trim().length()==0 ) return new String[0];
//      char a[] = param.toCharArray();
//      int i=0;
//      StringBuffer p=null;
//      Vector<StringBuffer> v = new Vector<StringBuffer>(5);
//
//      while( i<a.length ) {
//         while( i<a.length && (a[i]==' ' || a[i]==',') ) i++;	// passe les separateurs
//         p = new StringBuffer();
//         i = getOneDrawParam(p,a,i);
//         v.addElement(p);
//      }
//
//      Enumeration<StringBuffer> e = v.elements();
//      String s[] = new String[v.size()];
//      for( i=0; e.hasMoreElements(); i++ ) {
//         s[i] = new String(e.nextElement());
//      }
//      if( Aladin.levelTrace>=4 ) {
//         System.out.print("Command.getDrawParam: ");
//         for( i=0; s!=null && i<s.length; i++ ) {
//            System.out.print(" p["+i+"]=["+s[i]+"]");
//         }
//         System.out.println();
//      }
//
//      return s;
//   }

   /** Supprime les marques de liens dans une chaine de caractères */
   protected String removeLinks(String help) {
      if( help.indexOf('@')<0 ) return help;
      StringBuffer s = new StringBuffer();
      char a[]=help.toCharArray();
      for( int i=0; i<a.length; i++ ) if( a[i]!='@' ) s.append(a[i]);
      return s.toString();
   }
   
   private static final String OTHERS = "+-*/";
   private boolean isLetter(char c) {
      return Character.isLetterOrDigit(c) || OTHERS.indexOf(c)>=0;
   }
   
   /** Transforme les marques des liens en équivalent HTML */
   protected String translateLinks(String help) {
      StringBuffer s = new StringBuffer();
      StringTokenizer tok = new StringTokenizer(help," |\t\n\r\f", true);
      while( tok.hasMoreTokens() ) {
         String w = tok.nextToken();
         if( w.startsWith("@") ) {
            int offset=1;
            while( offset<w.length() && isLetter( w.charAt(offset) )) offset++;
            String w1 = w.substring(1,offset);
            String w2 = w.substring(offset,w.length());
            s.append("<A HREF=\"#"+w1+"\">"+w1+"</A>"+w2);
         } else if( w.startsWith("_") && w.endsWith("_")) {
            String w1 = w.substring(1,w.length()-1);
            s.append("<B><FONT SIZE=\"+2\" COLOR=\"darkgreen\">"+w1+"</FONT></B>");
          
         } else if( w.startsWith("#") && w.endsWith("#")) {
            String w1 = w.substring(1,w.length()-1);
            s.append("<B>"+w1+"</B>");
          
         } else if( w.startsWith("\\") ) {
            String w1 = w.substring(1,w.length());
            s.append(w1);
          
         } else s.append(w);

      }
      return s.toString();
   }

   

   /** Execution de la commande createROI */
   protected void execROICmd(String param) {
      try {
         if( param.length()==0 ) a.view.createROI();

         // Spécification en secondes d'arc
         else if( param.charAt(param.length()-1)=='"' ) {
            double radius = Double.valueOf(param.substring(0,param.length()-1)).doubleValue();
            a.view.createROI(radius/3600);

         // Spécification en pixels
         } else {
            int pixels = Integer.parseInt(param);
            a.view.createROI(pixels);
         }
      } catch( Exception e ) { 
         if( a.levelTrace>=3 ) e.printStackTrace();
         printConsole("!!! thumbnail error: "+e.getMessage());
      }
   }

   /** Execution de la commande xmatch */
   protected String execXMatchCmd(String s,String label) {
      String[] tokens = Util.split( s, " ", '(', ')' );
      int nbTokens = tokens.length;
//   	System.out.println("tokens.length : "+tokens.length);
//   	for( int i=0; i<tokens.length; i++ ) System.out.println("tokens["+i+"] : "+tokens[i]);
      int curIdx = 1; // on commence à 1 car tokens[0] == "xmatch"
      double dist=-1;
      int mode = CDSXMatch.BESTMATCH; // mode de xmatch par défaut : best match
      Plan pCat1 = null;
      Plan pCat2 = null;

      int k1,k2;
      String tmp;
      String p1Cols, p2Cols;
      p1Cols = p2Cols = null;

      if( curIdx<nbTokens ) {
        tmp = tokens[curIdx++];
      	k1 = tmp.indexOf('(');
      	k2 = tmp.lastIndexOf(')');

        if( k1>=0 && k2>=0 ) {
        	p1Cols = tmp.substring(k1+1,k2);
//        	System.out.println("p1Cols : "+p1Cols);
        	pCat1 = getNumber(tmp.substring(0,k1));
        }
        else pCat1 = getNumber(tmp);

      }

      if( curIdx<nbTokens ) {
        tmp = tokens[curIdx++];
      	k1 = tmp.indexOf('(');
      	k2 = tmp.lastIndexOf(')');

        if( k1>=0 && k2>=0 ) {
        	p2Cols = tmp.substring(k1+1,k2);
//        	System.out.println("p2Cols : "+p2Cols);
        	pCat2 = getNumber(tmp.substring(0,k1));
        }
        else pCat2 = getNumber(tmp);
      }

      if( curIdx<nbTokens ) {
         try {
            dist = Double.valueOf(tokens[curIdx++]).doubleValue();
         }
         catch( NumberFormatException e ) {dist=-1;}
      }
      // si dist n'est pas donné, on prend 4 arcsec comme distance par défaut
      else dist = 4.0;

      if( curIdx<nbTokens ) {
         String modeStr = tokens[curIdx++];
         if( modeStr.equalsIgnoreCase("allmatch") ) mode = CDSXMatch.ALLMATCH;
         else if( modeStr.equalsIgnoreCase("bestmatch") ) mode = CDSXMatch.BESTMATCH;
         else if( modeStr.equalsIgnoreCase("nomatch") ) mode = CDSXMatch.NOMATCH;
         else {
            printConsole("!!! xmatch error: parameter "+modeStr+" was not recognized !");
            println("Allowed modes are : allmatch|bestmatch|nomatch");
            return "";
         }
      }

      if( pCat1==null || pCat2==null || dist<0. ) printConsole("!!! xmatch error: missing or incorrect parameters");
      else {
        if( !(pCat1 instanceof PlanCatalog) || !(pCat2 instanceof PlanCatalog) ) {
            printConsole("!!! xmatch error: can only be performed on catalog planes !");
            return "";
        }
        CDSXMatch xMatch = new CDSXMatch(a);

        // une fois qu'on a les indices des plans, et qu'on s'est assuré qu'il s'agit
        // de PlanCatalog, on peut analyser les noms des col. de coordonnées (si elles ont été fournies)
        int[] p1CoordIdx, p2CoordIdx;
        p1CoordIdx = p2CoordIdx = null;
        if( p1Cols!=null ) {
        	p1CoordIdx = getCoordIdx( (PlanCatalog)pCat1, p1Cols);
        }
        if( p2Cols!=null ) {
        	p2CoordIdx = getCoordIdx( (PlanCatalog)pCat2, p2Cols);
        }

        xMatch.setColFilter(null);
        xMatch.posXMatch(pCat1, pCat2,label,
                         p1CoordIdx, p2CoordIdx, new double[]{0.0,dist}, mode, a, true);
      }
      return "";
   }

   /**
    * Retourne le tableau des index des coordonnées
    * @param pc
    * @param colStr
    * @return
    */
   private int[] getCoordIdx(PlanCatalog pc, String colStr) {
      	String[] coord = Util.split(colStr, ",", ' ', ' ', true);
      	if( coord.length<2 ) {
      		printConsole("!!! xmatch error: there should be 2 coordinate columns !");
      		return null;
      	}
      	int idxRa, idxDe;
      	idxRa = idxDe = -1;

      	try {
      		Field[] fields = pc.getFirstLegende().field;
      		for( int i=0; i<fields.length && (idxRa<0 || idxDe<0 ); i++ ) {
      			if( fields[i].name.equals(coord[0]) ) {
      				idxRa = i;
      				continue;
      			}
      			if( fields[i].name.equals(coord[1]) ) {
      				idxDe = i;
      				continue;
      			}
      		}
      	}
		catch( Exception e ) {e.printStackTrace(); return null;}

		if( idxRa<0 || idxDe<0 ) {
		    printConsole("!!! xmatch error: could not find coordinate columns "+colStr+" for plane "+pc.getLabel());
			println("Cross-match will be performed using coordinate columns found by UCD !");
			return null;
		}
		return new int[]{idxRa, idxDe};

   }

   /** Execution d'une commande help */
   protected void execHelpCmd(String param) { execHelpCmd(param,true); }
   protected void execHelpCmd(String param,boolean inAladinFrame) {
      if( param.length()==0 ) {
         if( inAladinFrame ) {
            a.log("Help","script command");
            a.cardView.show(a.bigView,"Help");
            a.inScriptHelp=true;
            if( a.msgOn ) a.msgOn = false;
            a.help.setHyperText("","|!Aladin script commands."+execHelp());
         }
         println(removeLinks(execHelp()));
         return;
      }
      
      if( param.equals("off")) { a.cardView.show(a.bigView,"View"); a.inScriptHelp=false; }
      else if( param.equals("all")) execAllHelp();
      else if( param.equals("allhtml")) execHTMLHelp();
      else {
         String s=getHelpString(param);
         if( s.length() == 0 ) return;
         println(removeLinks(s));
         if( inAladinFrame ) {
            a.inScriptHelp=true;
            a.cardView.show(a.bigView,"Help");
            a.help.setHyperText(param.charAt(0)=='#' ? null : param,"|" + s);
         }
      }
   }

   /** Affiche le Help des commandes à la queue leuleu */
   private void execAllHelp() {
      println(removeLinks(execHelp()));
      for( int i=0; i<CMD.length; i++ ) {
         println("----");
         println(removeLinks(getHelpString(CMD[i])));
      }
   }
   
   /** Affiche le Help des commandes à la queue leuleu en HTML */
   private void execHTMLHelp() {
      println("<PRE>\n"+translateLinks(execHelp())+"</PRE>\n");
      for( int i=0; i<CMD.length; i++ ) {
         println("<P><HR><A NAME=\""+CMD[i]+"\"></A>");
         println(translateLinks(getHelpStringHTML(CMD[i])));
      }
   }


   /** Mise en forme d'un texte de help avec éventuellement
    * récupération au préalable du paragraphe du help associé à une commande
    * @param p chaine du help (commence par #) ou nom de commande
    * @return le paragraphe du help mis en forme
    */
   private String getHelpString(String p) {
      String s;

      // Il s'agit déjà du texte de help et non pas d'une commande
      if( p.charAt(0)=='#' ) s=p;

      // Recherche du Help via le fichier Aladin.String
      else {
         String s1 = "Help."+Util.toLower(p);
         s = a.chaine.getString(s1);
         if( s.length()==0 || s.equals("["+s1+"]") ) {
            printConsole("!!! help error: command \""+p+"\" unknown !");
            return "";
         }
      }

      // Découpage par section. La syntaxe est la suivante :
      // #s:synopsys#d:texte\ntexte#e:exemple1#e:exemple2
      // # séparateur de champ
      // s:synopys, n:description 1 ligne, d:description, e:exemple,
      // t:note, g:see also
      // Le champ peut contenir des \n, l'indentation et le repli des lignes
      // trop longue sera automatique
      StringTokenizer st = new StringTokenizer(s,"#");
      String indent = "   ";
      StringBuffer res = new StringBuffer();
      char oc=' ';
      while( st.hasMoreTokens() ) {
         s=st.nextToken();
         if( s.length()<2 ) continue;
         char c = s.charAt(0);
         s = s.substring(2);

         // Détermination d'un éventuel titre de section
         if( c!=oc ) {
            res.append(" \n"+(c=='n'?" \n \n \n#NAME#":c=='s'?"#SYNOPSIS#":c=='d'?"#DESCRIPTION#":
               c=='e'?"#EXAMPLE#":c=='t'?"#NOTES#":c=='g'?"#SEE# #ALSO#":"")+"\n");
         }
         oc=c;

         // Découpage du texte par ligne avec éventuellement repli des lignes
         StringTokenizer st1 = new StringTokenizer(s,"\n");
         while( st1.hasMoreTokens() ) {
            StringBuffer line = new StringBuffer(indent.toString());
            StringTokenizer st2= new StringTokenizer(st1.nextToken()," ",true);
            String mot=null;
            while( mot!=null || st2.hasMoreTokens() ) {
               if( mot==null ) mot = st2.nextToken();
               if( line.length() + mot.length() >78 ) {
                  res.append(line+"\n");
                  line = new StringBuffer(indent.toString());
               }
               line.append(mot); mot=null;
            }
            if( line.length()>indent.length() ) res.append(line+"\n");
         }
      }
      return res.toString();
   }
  
   /** Mise en forme HTML d'un texte de help avec éventuellement
    * récupération au préalable du paragraphe du help associé à une commande
    * @param p chaine du help (commence par #) ou nom de commande
    * @return le paragraphe du help mis en forme
    */
   private String getHelpStringHTML(String p) {
      String s;

      // Il s'agit déjà du texte de help et non pas d'une commande
      if( p.charAt(0)=='#' ) s=p;

      // Recherche du Help via le fichier Aladin.String
      else {
         String s1 = "Help."+Util.toLower(p);
         s = a.chaine.getString(s1);
         if( s.length()==0 || s.equals("["+s1+"]") ) {
            printConsole("!!! help error: command \""+p+"\" unknown !");
            return "";
         }
      }

      // Découpage par section. La syntaxe est la suivante :
      // #s:synopsys#d:texte\ntexte#e:exemple1#e:exemple2
      // # séparateur de champ
      // s:synopys, n:description 1 ligne, d:description, e:exemple,
      // t:note, g:see also
      // Le champ peut contenir des \n, l'indentation et le repli des lignes
      // trop longue sera automatique
      StringTokenizer st = new StringTokenizer(s,"#");
      String indent = "   ";
      StringBuffer res = new StringBuffer();
      char oc=' ';
      while( st.hasMoreTokens() ) {
         s=st.nextToken();
         if( s.length()<2 ) continue;
         char c = s.charAt(0);
         s = s.substring(2);

         // Détermination d'un éventuel titre de section
         if( c!=oc ) {
            res.append("<P>\n"+(c=='n'?"":
                                c=='s'?"<P><I><B>Synopsis</B></I>:<BR>":
                                c=='d'?"<P><I><B>Description</B></I><BR>":
                                c=='e'?"<P><I><B>Example</B></I>:<BR>":
                                c=='t'?"<P><I><B>Note</B></I> - ":
                                c=='g'?"<P><I><B>See also</B></I>: "
                                      :"")+"\n");
         } else res.append("<BR>\n");
         oc=c;
         
         res.append(s);

//         // Découpage du texte par ligne avec éventuellement repli des lignes
//         StringTokenizer st1 = new StringTokenizer(s,"\n");
//         while( st1.hasMoreTokens() ) {
//            StringBuffer line = new StringBuffer(indent.toString());
//            StringTokenizer st2= new StringTokenizer(st1.nextToken()," ",true);
//            String mot=null;
//            while( mot!=null || st2.hasMoreTokens() ) {
//               if( mot==null ) mot = st2.nextToken();
//               if( line.length() + mot.length() >70 ) {
//                  res.append(line+"\n");
//                  line = new StringBuffer(indent.toString());
//               }
//               line.append(mot); mot=null;
//            }
//            if( line.length()>indent.length() ) res.append(line+"\n");
//         }
      }
      return res.toString();
   }
   
 
   /** Interprétation d'une position healpix donnée par norder/npix */
   protected boolean execHpxCmd(String param) {
      try { 
         int offset = param.indexOf('/');
         if( offset==-1 ) return false;
         int order = Integer.parseInt(param.substring(0,offset).trim());
         int npix = Integer.parseInt(param.substring(offset+1).trim());
         
         long nSide = CDSHealpix.pow2(order);
         double x[];
         x = CDSHealpix.polarToRadec(CDSHealpix.pix2ang_nest(nSide, npix));
         Coord c = new Coord(x[0], x[1]);
         ViewSimple v = a.view.getCurrentView();
         Plan pref = null;
         if( v!=null && !v.isFree() ) pref=v.pref;
         if( pref!=null && pref instanceof PlanBG ) {
            a.localisation.frameToFrame(c, ((PlanBG)pref).frameOrigin, Localisation.ICRS);
         }
         a.view.setRepere(c);
//         printConsole("Healpix "+order+"/"+npix+" => "+c);
         return true;
      } catch( Exception e ) { }
      return false;
   }

   /** Exécution de la commande select */
   protected void execSelectCmd(String param) {

      // Sélection des sources taguées
      if( param.trim().equals("-tag") ) {
         a.selecttag();
         return;
      }

      int defaultView = a.view.getCurrentNumView();
      int nview=-1;
      int n=-1;
      boolean first=true;
//      a.view.unSelectAllView();
//      a.view.deSelect();
//      a.calque.unSelectAllPlan();
      Tok st = new Tok(param);
      while( st.hasMoreTokens() ) {
         String x = st.nextToken();
         
         // Indication d'une frame particulière dans le cas d'un cube
         if( Util.indexOfIgnoreCase(x, "frame=")==0 ) {
            try {
               // Récupération du numéro de frame (peut être un nombre non-entier
               double frame = Double.parseDouble(x.substring(6))-1;
               
               // Un plan particulier => toutes les vues dont c'est la référence
               if( n>=0) {
                  int np[] = a.view.getNumView(a.calque.getPlan(n));
                  for( int i=0; i<np.length; i++ ) {
                     if( a.view.viewSimple[np[i]].blinkControl==null ) continue;
                     a.view.viewSimple[np[i]].blinkControl.setFrameLevel(frame);
                     a.view.viewSimple[np[i]].repaint();
                  }
                  
               // Une vue particulière
               } else if( nview>=0 ) {
                  if( a.view.viewSimple[nview].blinkControl==null ) continue;
                  a.view.viewSimple[nview].blinkControl.setFrameLevel(frame);
                  a.view.viewSimple[nview].repaint();
                 
               // Toutes les vues sélectionnées
               } else {
                  ViewSimple v[] = a.view.getSelectedView();
                  for( int i=0; i<v.length; i++ ) {
                     if( v[i].blinkControl==null ) continue;
                     v[i].blinkControl.setFrameLevel(frame);
                     v[i].repaint();
                  }
               }
            } catch( Exception e ) {
               printConsole("!!! select cube frame error: "+e.getMessage()); 
            }
            return;         
         
         // On ne le fait que maintenant dans le cas d'une sélection d'une frame particulière
         // pour l'ensemble des vues précédemment sélectionnées
         } else {
            if( first ) {
               first=false;
               a.view.unSelectAllView();
               a.view.deSelect();
               a.calque.unSelectAllPlan();
            }
         }
         Plan p = getNumber(x,1,false,false);
//         n=getNumber(x,1,false,false);
         nview=getViewNumber(x,false);
         if( nview >= 0 ) {
           a.view.selectView(nview);
           a.view.setCurrentNumView(nview);
           defaultView=-1;
         } else {
           if( p!=null ) {
              p.selected=true;
              nview = a.view.selectView(p);
              if( defaultView!=-1 && nview!=-1 ) {
                 a.view.setCurrentNumView(nview);
                 defaultView=-1;
              }
              if( p.isCatalog() ) {
                 a.view.selectAllInPlanWithoutFree(p,0);
                 a.mesure.mcanvas.repaint();
              }
           }
         }
      }
      // Si aucune view sélectionnée, on remet la dernière
      if( defaultView>=0 ) {
         a.view.selectView(defaultView);
         a.view.setCurrentNumView(defaultView);
      }
      a.calque.repaintAll();
   }

   /** Exécution d'un flipflop = Symétrie verticale ou horizontale */
   protected void execFlipFlop(String param, String label) {
      char a[] = param.toCharArray();
      int i=0,j;
      String arg=null;
      int methode=0;

      // Récup de l'argument éventuel (H ou V ou HV)
      if( a.length>0 ) {
         for( i=a.length-1; i>0 && Character.isSpaceChar(a[i]); i--);
         j = i+1;
         while( i>0 && !Character.isSpaceChar(a[i])) i--;
         if( Character.isSpaceChar(a[i]) ) i++;
         arg = param.substring(i,j);
      }

      if( arg!=null && arg.length()!=0 ) {
         if( arg.equalsIgnoreCase("H") ) methode=1;
         else if( arg.equalsIgnoreCase("VH") || arg.equalsIgnoreCase("HV") ) methode=2;
      } else i=a.length;  // pas d'argument mentionné

      // Détermination du plan concerné
      Plan [] p = getPlan(param.substring(0,i).trim(),1);
      if( p.length==0 ) p = new Plan[]{ this.a.calque.getPlanBase() };

      // Plan de destination
      if( label!=null ) {
         try {
            p[0] = this.a.calque.dupPlan((PlanImage)p[0],label,p[0].type,false);
         } catch( Exception e ) { printConsole("!!! fliflop error: "+e.getMessage()); return; }
      }
      try {
         this.a.view.flip((PlanImage)p[0],methode);
      } catch( Exception e ) {
         printConsole("!!! fliflop error: "+e.getMessage());
      }
   }
   
   /** Execution de la commande match */
   protected void execMatchCmd(String param) {
      StringTokenizer tok = new StringTokenizer(param);
      int mode=3;
      String p="";
      while( tok.hasMoreTokens() ) {
         String s = tok.nextToken();
         if( s.equalsIgnoreCase("-scale") ) mode=2;
         else if( s.equalsIgnoreCase("off") ) mode=0;
         else { p=s; break; }
      }
      if( p.length()>0 ) execSelectCmd(p);
      if( mode==0 ) {
         if( a.view.isSelectCompatibleViews() ) a.view.unselectViewsPartial();
         a.match(0);
         
      } else a.switchMatch(mode==3);
   }

   /** réduction à une portion de l'image
    * Dans le cas d'une copie préalable, retourne le nouveau Plan
    * */
   protected Plan execCropCmd(String param,String label) {
      char b[] = param.toCharArray();
      int i,j;
      String size,pos=null;
      double x=0,y=0,w=50,h=50;
      Coord c1=null;

      // recup de la taille wxh
      boolean flagDim=false;
      i=b.length-1;
      try {
         for( ; i>0 && Character.isSpaceChar(b[i]); i--);
         j = i+1;
         while( i>0 && !Character.isSpaceChar(b[i])) i--;
         size = param.substring(Character.isSpaceChar(b[i]) ? i+1:i , j);
         j = size.indexOf('x');
         if( j<0 ) throw new Exception();
         w = Double.parseDouble(size.substring(0,j));
         h = Double.parseDouble(size.substring(j+1));
         flagDim=true;
      } catch( Exception e ) { i=b.length-1; }

      // recup de la position x,y si presente
      while( i>0 && Character.isSpaceChar(b[i])) i--;
      j=i>0 && !Character.isSpaceChar(b[i]) ? i+1 : i;
      boolean flagPos=false;
      while( i>0 && !Character.isSpaceChar(b[i])) { if( b[i]==',' ) flagPos=true;   i--; }
      if( flagPos ) {
         pos = param.substring(Character.isSpaceChar(b[i]) ? i+1:i , j);
         j = pos.indexOf(',');
         x = Double.parseDouble(pos.substring(0,j));
         y = Double.parseDouble(pos.substring(j+1));
         j = i;
      }

      // Récupération du plan concerné (on ne supporte plus la possibilité de mentionner plusieurs plans)
      // Attention, cette possibilité n'est pas offerte dans le cas d'un PlanBG (allsky) non visible
      PlanImage pi=null;
      try {
         pi = (PlanImage)getPlan(param.substring(0,j),1)[0];
         if( pi==null ) throw new Exception();
      } catch( Exception e1 ) {
         pi = (PlanImage)a.calque.getPlanBase();
      }
      ViewSimple v = a.view.getView(pi);
      if( pi instanceof PlanBG && (!pi.active || !pi.ref && pi.getOpacityLevel()==0f) ) {
         Aladin.warning("crop error: allsky plane ["+pi.label+"] must be visible to be cropped!",1);
         System.err.println("crop error: allsky plane ["+pi.label+"] must be visible to be cropped!");
         return null;
      }

      // On détermine la taille si non précisée ?
      if( !flagDim ) {
         try {
            w=v.rzoom.width;
            h=v.rzoom.height;
         } catch( Exception e ) { }
         
      }
      else if( pi instanceof PlanBG ) { w /=v.zoom; h/=v.zoom; }

    // On essaye la position du repere, sinon le centre de la vue, si nécessaire
      if( !flagPos ) {
         try {
            c1 = pi instanceof PlanBG ? v.getCooCentre()
                  : new Coord(a.view.repere.raj,a.view.repere.dej);
            pi.projd.getXY(c1);
//            x = c1.x;
//            y = c1.y;
            x = c1.x-w/2.;
            y = c1.y-h/2.;
            y = pi.naxis2-(y+h);
         } catch( Exception e1 ) {
            e1.printStackTrace();
            x=v.rzoom.x;
            y=v.rzoom.y; 
         }
      }

      a.trace(4,"Command.crop: on "+v+" param=["+param+"] label="+label+" "+x+","+y+(flagPos?"(provided) ":" ("+c1+" on reticle) ")+w+"x"+h+(flagDim?"( provided)":"(view size)"));

      if( v.cropArea(new RectangleD(x,pi.naxis2-(y+h),w,h), label, v.zoom, 1,true,false)==null ) {
         Aladin.warning("crop error: view ["+v+"] not usable!",1);;
         System.err.println("crop error: view ["+v+"] not usable!");
         return null;
      }
      syncNeedRepaint=true;
      a.view.repaintAll();
      return pi;
   }
   
   
   protected void setDrawMode(int mode) { drawMode=mode; }
   
   /** Recupération d'une couleur spécifique, et recalage du Tok si nécessaire
    * dans le cas d'un rgb(r,g,b) qui nécessite de lire 3 paramètres
    * @param s le nom de la couleur ou de la fonction de couleur
    * @param tok le parser des paramètres calés sur le prochain
    * @return la couleur, ou null s'il ne s'agit pas d'un nom de couleur ou d'une fonction de couleur
    */
   private Color getSpecifColor(String s,Tok tok) {
      Color c;
      if( s.equalsIgnoreCase("rgb") ) {
         String r = tok.nextToken();
         String g = tok.nextToken();
         String b = tok.nextToken();
         s = s+"("+r+","+g+","+b+")";
         c = Action.getColor(s);
      } else c = Action.getColor(s);
      return c;
   }
   
   private boolean flagFoV=false;   // Une commande de création de FoV a été passée au préalable
   private Color globalColor=null;  // Dernière couleur demandée
   private Plan oPlan=null;         // Dernier plan Tool ou FoV utilisé
   
  /** Execution d'une commande get */
   protected boolean execDrawCmd(String cmd,String param) {
      Plan plan=null;	// Plan ou il faudra dessiner
      int height;		// On va compter XY à partir du bas
      Obj newobj=null;  // Nouvelle objet a inserer
      Coord c=null;	    // Position de l'objet si drawMode==DRAWRADEC;
      double x=0,y=0;	// Position de l'objet si drawMode==DRAWXY;
      Color specifColor=null;  // Couleur spécifique à l'objet
      
      
//      StringTokenizer st = new StringTokenizer(param,"(");
//      String fct = st.nextToken();
//      String parameter =  fct.length()<param.length() ? param.substring(fct.length()+1,param.length()-1) : "";
//      String p [] = getDrawParam(parameter);
      
      Tok tok = new Tok(param,"(, )");
      String fct = tok.nextToken();
      
      // Couleur spécifique ? => on la traite, et on se recale
      specifColor=getSpecifColor(fct,tok);
      if( specifColor!=null )  fct = tok.nextToken();
      
      // Recupération des paramètres de la fonction
      String p [] = new String[ tok.countTokens() ];
      for( int i=0; i<p.length; i++ ) p[i] = tok.nextToken();
      
      // Détermination de la hauteur de l'image de base,
      // sinon on prendra 500 par défaut
      height = 500;
      PlanImage pi = (PlanImage)a.calque.getPlanBase();
      if( pi!=null ) height=pi.naxis2;

      // Gestion du mode de tracage (en XY ou en RADEC)
      if( fct.equalsIgnoreCase("mode") ) {
         if( p[0].equalsIgnoreCase("xy") ) drawMode = DRAWXY;
         else if( p[0].equalsIgnoreCase("radec") ) drawMode = DRAWRADEC;
         else {
            printConsole("!!! draw error: mode param ("+p[0]+") unknown");
            return false;
         }
         console("Draw mode: "+(drawMode==DRAWXY ? "XY coordinates":"celestial coordinates"));
         
         return true;
      }
      
      // Commande global(prop=value,prop=value...)
      if( fct.equalsIgnoreCase("global") ) {
         memoGlobal(p);
         return true;
      }

      // Création d'un plan tool => draw newtool(mytool)
      if( fct.equalsIgnoreCase("newtool") ) {
         String name=null;
         if( p.length>0 ) name=p[0];
         oPlan=plan = a.calque.createPlanTool(name);
         if( globalColor!=null ) plan.c=globalColor;
         return true;
      }
      
      // Création d'un plan FoV => draw newFOV(xc,yc[,angle,mytool])
      try {
         if( fct.equalsIgnoreCase("newfov") ) {
            if( drawMode==DRAWRADEC ) {
               c = new Coord(p[0]+" "+p[1]);
               c=a.localisation.frameToICRS(c);
            } else {
               c = new Coord();
               c.x = parseDouble(p[0])-0.5;
               c.y = height-parseDouble(p[1])+0.5;
               a.view.getCurrentView().getProj().getCoord(c);
            }
            double angle = p.length<3 ? 0 : parseDouble(p[2]);
            String name = p.length>=4 ? p[3] : null;
            oPlan=plan = a.calque.createPlanField(name,c,angle,true,true);
            if( globalColor!=null ) plan.c=globalColor;
            flagFoV=true;
            return true;
         }
      } catch( Exception e ) {
         printConsole("!!! draw error: "+e.getMessage());
         if( Aladin.levelTrace!=0 ) e.printStackTrace();
         return false;
      }
      
      // Determination du plan TOOL, ou creation si necessaire
      // On essaye de reprendre le précédent si possible
      if( oPlan!=null && oPlan.type!=Plan.APERTURE && flagFoV ) oPlan=null; // Il faut passer à un plan FoV
      if( oPlan!=null && a.calque.planToolOk(oPlan,flagFoV) ) plan=oPlan;   // On reprend le précédent
      else plan = flagFoV ? a.calque.selectPlanToolOrFoV() : a.calque.selectPlanTool();
      oPlan=plan;
      
      // Positionnement des variables globales au plan
      if( globalColor!=null ) {
         if( globalColor!=plan.c && plan.type==Plan.TOOL && plan.getCounts()>0 ) plan=a.calque.createPlanTool(null); // Création automatique au changement de couleur
         plan.c=globalColor;
      }
      if( drawMode==DRAWRADEC ) plan.setXYorig(false);
      
      try {

         // Recuperation de la position (toujours les 2 premiers parametres)
         if( drawMode==DRAWRADEC ) {
            c = new Coord(p[0]+" "+p[1]);
            c=a.localisation.frameToICRS(c);
         } else {
            x = parseDouble(p[0])-0.5;
            y = height-parseDouble(p[1])+0.5;
         }

         // Commande string(x,y,text)
         if( fct.equalsIgnoreCase("string") ) {
            Tag tag;
            if( drawMode==DRAWRADEC ) newobj = tag = new Tag(plan,c,p[2]);
            else newobj = tag = new Tag(plan,a.view.getCurrentView(),x,y,p[2]);
            tag.setDist(5); tag.setAngle(0); tag.setPole("nopole");

            // Commande tag(x,y...)
         } else if( fct.equalsIgnoreCase("tag") || fct.equalsIgnoreCase("string")) {
            Tag tag;
            String id = p.length<3 ? null : p[2]; 
            if( drawMode==DRAWRADEC ) newobj = tag = new Tag(plan,c,id);
            else newobj = tag = new Tag(plan,a.view.getCurrentView(),x,y,id);
            try {
               if( p.length>3 ) tag.setDist( (int)parseDouble(p[3]) );
               if( p.length>4 ) tag.setAngle( (int)parseDouble(p[4]) );
               if( p.length>5 ) tag.setPole( p[5] );
               if( p.length>6 ) tag.setFontSize( (int)parseDouble(p[6]) );
            } catch( Exception e ) {
               printConsole("!!! draw tag error: usage: draw tag(x,y[,label,dist,angle,pole,fontSize])");
               return false;
            }

         // Commande phot(x,y,r)
         } else if( fct.equalsIgnoreCase("phot") ) {
            Repere phot;
            ViewSimple v = a.view.getCurrentView();
            try {
               if( drawMode==DRAWRADEC ) {
                  newobj = phot = new Repere(plan,c);
                  phot.setRadius( p[2] );
               } else {
                  newobj = phot = new Repere(plan,v,x,y);
                  phot.setRayon( v,parseDouble(p[2]) );
               }
            } catch( Exception e ) {
               printConsole("!!! draw phot error: usage: draw phot(x,y,radius)");
               return false;
            }

            // Commande circle(x,y,r)
         } else if( fct.equalsIgnoreCase("circle") ) {

            if( drawMode==DRAWRADEC ) {
               double r = Server.getAngle(p[2],Server.RADIUSs)/60.;
               newobj = new Cercle(plan,c,r);
            } else {
               double r = parseDouble(p[2]);
               newobj = new Cercle(plan,a.view.getCurrentView(),x,y,r);
            }

            // Commande ellipse(x,y,semiMA,semiMI,angle)
         } else if( fct.equalsIgnoreCase("ellipse") ) {
            double angle   = parseDouble(p[4]);
            if( drawMode==DRAWRADEC ) {
               double semiMA = Server.getAngle(p[2],Server.RADIUSs)/60.;
               double semiMI = Server.getAngle(p[3],Server.RADIUSs)/60.;
               newobj = new Ellipse(plan,c,semiMA,semiMI,angle);
            } else {
               double semiMA = parseDouble(p[2]);
               double semiMI = parseDouble(p[3]);
               newobj = new Ellipse(plan,a.view.getCurrentView(),x,y,semiMA,semiMI,angle);
            }

            // Commande box(x,y,w,h[,angle])
         } else if( fct.equalsIgnoreCase("box") ) {
            double angle=0;
            try { angle = parseDouble(p[4]); } catch( Exception e ) { }
            if( drawMode==DRAWRADEC ) {
               double w = Server.getAngle(p[2],Server.RADIUSs)/60.;
               double h = Server.getAngle(p[3],Server.RADIUSs)/60.;
               newobj = new Box(plan,c,w,h,angle);
            } else {
               double w = parseDouble(p[2]);
               double h = parseDouble(p[3]);
               newobj = new Box(plan,a.view.getCurrentView(),x,y,w,h,angle);
            }

            // Commande vector(x,y,w,angle)
         } else if( fct.equalsIgnoreCase("vector") ) {
            double angle = parseDouble(p[3]);
            if( drawMode==DRAWRADEC ) {
               double w = Server.getAngle(p[2],Server.RADIUSs)/60.;
               newobj = new Vecteur(plan,c,w,angle);
            } else {
               double w = parseDouble(p[2]);
               newobj = new Vecteur(plan,a.view.getCurrentView(),x,y,w,angle);
            }

            // Commande arc(x,y,r,startAngle,endAngle)
         } else if( fct.equalsIgnoreCase("arc") ) {
            double startAngle = parseDouble(p[3]);
            double angle   = parseDouble(p[4]);
            if( drawMode==DRAWRADEC ) {
               double r = Server.getAngle(p[2],Server.RADIUSs)/60.;
               newobj = new Arc(plan,c,r,startAngle,angle);
            } else {
               double r = parseDouble(p[2]);
               newobj = new Arc(plan,a.view.getCurrentView(),x,y,r,startAngle,angle);
            }

            // Commande pickle(x,y,r1,r2,startAngle,endAngle)
         } else if( fct.equalsIgnoreCase("pickle") ) {
            double startAngle = parseDouble(p[4]);
            double angle   = parseDouble(p[5]);
            if( drawMode==DRAWRADEC ) {
               double r1 = Server.getAngle(p[2],Server.RADIUSs)/60.;
               double r2 = Server.getAngle(p[3],Server.RADIUSs)/60.;
               newobj = new Pickle(plan,c,r1,r2,startAngle,angle);
            } else {
               double r1 = parseDouble(p[2]);
               double r2 = parseDouble(p[3]);
               newobj = new Pickle(plan,a.view.getCurrentView(),x,y,r1,r2,startAngle,angle);
            }
            
         } else if( fct.equalsIgnoreCase("line") 
                 || fct.equalsIgnoreCase("polygon") ) {
            newobj=null;
            Ligne p1,op1 = null;
            ViewSimple v = a.view.getCurrentView();
            // Y a-t-il un label en dernier paramètre ?
            String id=null;
            int n = p.length;
            if( n%2==1 ) {
               id = p[n-1];
               n--;
            }
            for( int i=0; i<n; i+=2) {
               if( drawMode==DRAWRADEC ) {
                  c = new Coord(p[i]+" "+p[i+1]);
                  c=a.localisation.frameToICRS(c);
                  p1  = new Ligne(c.al,c.del, plan, v,id,op1);
               } else {
                  x = parseDouble(p[i])-0.5;
                  y = height-parseDouble(p[i+1])+0.5;
                  p1 = new Ligne(plan,v,x,y,id,op1);
               }
               if( specifColor!=null ) p1.setColor(specifColor);
               addObj(plan,p1);
               op1=p1;
            }
            // bouclage
            if( fct.equalsIgnoreCase("polygon") ) {
               newobj = p1 = new Ligne(0,0,plan,v,id,op1);
               p1.makeLastLigneForClose(v);
            }

         } else if( fct.equalsIgnoreCase("dist") ) {
            newobj=null;
            Cote p1,op1 = null;
            ViewSimple v = a.view.getCurrentView();
            int n = p.length;
            for( int i=0; i<n; i+=2) {
               if( drawMode==DRAWRADEC ) {
                  c = new Coord(p[i]+" "+p[i+1]);
                  c=a.localisation.frameToICRS(c);
                  p1  = new Cote(c.al,c.del, plan, v,op1);
               } else {
                  x = parseDouble(p[i])-0.5;
                  y = height-parseDouble(p[i+1])+0.5;
                  p1 = new Cote(plan,v,x,y,op1);
               }
               if( specifColor!=null ) p1.setColor(specifColor);
               addObj(plan,p1);
               op1=p1;
            }
            
        // Commande draw inconnue
         } else {
            printConsole("!!! draw error: function unknown ("+fct+")");
            return false;
         }
      } catch( Exception e ) {
         printConsole("!!! draw error: "+e.getMessage());
         if( Aladin.levelTrace!=0 ) e.printStackTrace();
         return false;
      }

      // Couleur spécifique + Tracage
      if( newobj!=null ) {
         if( specifColor!=null ) newobj.setColor(specifColor);
         addObj(plan,newobj);
      }
      
      plan.resetProj();
      a.view.repaintAll();
      return true;
   }
   
   // Parsing d'un double avec prise en compte d'un éventuel format
   // en suffixe (à la IRAF, ex: 23.7686d)
   // prend également en compte le signe '+' en préfixe
   private double parseDouble(String s) throws Exception {
      s = s.trim();
      int fin;
      for( fin=s.length()-1; fin>0 && !Character.isDigit(s.charAt(fin)); fin--);
      int deb= s.length()>0 && s.charAt(0)=='+' ? 1 : 0;
      return Double.parseDouble(s.substring(deb,fin+1));
   }
   
   // Ajout d'un objet graphique => dans le cas d'un ajout dans un plan FoV (PlanField)
   // Il est nécessaire de calculer également les (x,y) tangentiels
   private void addObj(Plan plan,Obj newobj) {
      plan.pcat.setObjetFast(newobj);
      
      // Il faut encore calculer les tangentes par rapport au centre de la projection
      if( plan.type!=Plan.APERTURE ) return;
      ((Position)newobj).setXYTan(plan.co);
   }
   
   private void memoGlobal(String [] p) {
      for( int i=0; i<p.length; i++ ) {
         if( p[i].startsWith("color=") ) {
            globalColor=Action.getColor(p[i].substring(6));
//            System.out.println("globalColor found="+p[i].substring(6)+" c="+globalColor);
         }
      }
   }

   /** Execution d'une commande info
    *
    * @param param texte a afficher
    */
   private void execInfo(String param) {
       a.status.setText(param);
   }
   
   /** Execute une chaine contenant un script comme un flux afin de garantir l'ordre des commandes
    * lorsqu'il y a des "load" ou "get" de scripts et filtres "emboités" */
   protected void execScriptAsStream(String s) {
      MyByteArrayStream bis=null;
      try {
         bis = new MyByteArrayStream(2000);
         bis.write(s);
         readFromStream(bis.getInputStream());
      } finally {
         try { if( bis!=null ) bis.close(); } catch( IOException e ) { }
      }
   }
   
  /** Traitement d'une ligne de script eventuellement avec des ";"
   * @param s la ligne a traiter
   * @param verbose true si on baratine
   * @return null si la premiere commande n'est pas trouvee
   */
   public String execScript(String s) { return execScript(s,true,false); }
   synchronized public String execScript(String s,boolean verbose,boolean flagOnlyFunction) {
//      StringTokenizer st = new StringTokenizer(s,";\n\r");
      // thomas, 16/11/06 : permet de ne pas couper la déf. des filtres (pb des ';' dans les UCD !)
      String[] commands = Util.split(s, ";\n\r", '[', ']');
      int i=0;		// Compteur de commandes
      StringBuffer rep = new StringBuffer();
      String s1=null;

//      while( st.hasMoreTokens() ) {
      for( int k=0; k<commands.length; k++ ) {
         try {
            String cmd = commands[k].trim();
            if( cmd.length()>0 ) {
               if( i==0 ) { if( (s1=exec(cmd,verbose,flagOnlyFunction))==null ) return null; }
               else s1=exec(cmd,verbose,flagOnlyFunction);
            }
            
            if( s1!=null && s1.length()>0 ) rep.append(s1);
            i++;
         } catch( Exception e ) {
            if( a.levelTrace==3 ) e.printStackTrace();
         }
      }
      return rep.toString();
   }

  /** Retourne la dernière commande exécutée */
   protected String getLastCmd() { return lastCmd; }

   /** Retourne le code du calcul algébrique ou -1 si non trouvé */
   private int findAlgebre(String s) {
      if( s.startsWith("+") ) return PlanImageAlgo.ADD;
      if( s.startsWith("-") ) return PlanImageAlgo.SUB;
      if( s.startsWith("* ") ) return PlanImageAlgo.MUL;
      if( s.startsWith("/") ) return PlanImageAlgo.DIV;
      
      if( s.indexOf(" + ")>0 ) return PlanImageAlgo.ADD;
      if( s.indexOf(" - ")>0 ) return PlanImageAlgo.SUB;
      if( s.indexOf(" * ")>0 ) return PlanImageAlgo.MUL;
      if( s.indexOf(" / ")>0 ) return PlanImageAlgo.DIV;
      
      return -1;
   }
      
   StringBuffer comment=null;           // Last comment
   Function fonct=null;
   
  /** Traitement d'une commande aladin
   * @param s la commande a traiter
   * @param verbose true si ca doit etre bavard
   * @return null si la commande n'existe pas
   */
   
   protected void execLater(String s) {
      if( SwingUtilities.isEventDispatchThread() ) {
         exec(s);
      } else {
         final String [] param = new String[1];
         param[0]=s;
         SwingUtilities.invokeLater(new Runnable() {
            public void run() { exec(param[0]); }
         });
      }
   }
   protected String exec(String s) { return exec(s,true,false); }
   protected String exec(String s1,boolean verbose,boolean flagOnlyFunction) {
      if( a.isFullScreen() && !a.fullScreen.isVisible() ) a.fullScreen.setVisible(true);
      
      // mémorisation du dernier commentaire pour une éventuelle définition de fonction
      if( s1.trim().charAt(0)=='#' ) {
         if( comment==null ) comment = new StringBuffer(s1.trim().substring(1));
         else comment.append(" "+s1.trim().substring(1));
         return "";
      } else if( !s1.startsWith("function") ) comment=null;
      
      
      // Attente que les serveurs soient OK
      syncServer();
      
      if( !filterMode && fonct==null ) {
         // Petit ajustement éventuelle pour une commande "=expression" ou "expression="
         s1 = evalAdjust(s1.trim());

         // Petit adjustement éventuelle pour une commande " val Unit1 in Unit2"
         s1 = convertAdjust(s1);
      }
      
      // Compatibilité pour les commandes "region" de DS9
      try { 
         String s2 = ds9.translate(s1);
         if( s2!=null ) {
            if( s2.length()==0 ) return ""; // Commande jugée inutile (par exemple changement de frame)
            return execScript(s2, verbose, flagOnlyFunction);
         }
      } catch( Exception e) { printConsole(e.getMessage()); return "";}
      
      // Extraction d'un éventuel préfixe désignant le plan target
      // ex: toto = get Simbad m1
      StringBuffer tp = new StringBuffer();
      String s = getTargetPlane(tp, s1);
      String label = tp.length()==0 ? null : "="+tp.toString();
//System.out.println("TargetPlane=["+tp+"] => s="+s+" label="+label);
      
      
      Tok st = new Tok(s);
      String cmd = st.nextToken();
      String param;

      // Petit raccourci pour remplacer "toto=@1" par "toto=copy @1"
      if( getNumber(s,1,false,false)!=null ) { param=s; cmd="copy"; }

      // Sinon, simple récupération des paramètres
      else param = s.substring(cmd.length()).trim();
      int n,fct;

      lastCmd=cmd;
      
      // Faut-il faire écho de la commande ?
      boolean echo = verbose && (a.getInstanceId()>0 || a.getInstanceId()==0 && !a.flagLaunch);

      // Echo sur la sortie standard
      if( echo  ) println("["+s1+"]...");
      
      // sync automatique pour les commandes concernées
      if( syncMode==SYNCON && needSync(cmd) ) {
         if( !isSync() ) a.trace(4,"Command.exec() : command \""+cmd+"\" needs sync...");
         sync();
      }
      
      // est-ce le debut d'une nouvelle definition de fonction ?
      if( s1.trim().startsWith("function") ) {
         fonct = new Function();
         if( comment!=null ) {
            fonct.setDescription(comment.toString());
            comment=null;
         }
      }
      if( fonct!=null ) {
         try {
            if( fonct.parseFunction(s1) ) { addFunction(fonct); fonct=null; }
         } catch( Exception e ) { printConsole("!!! "+e.getMessage()); fonct=null; }
         return "";
      }
      
      // Ne fait que charger les fonctions
      if( flagOnlyFunction ) return "";

      //thomas
	  // est-ce le debut d'une nouvelle definition de filtre ?
      if( s.toLowerCase().startsWith("filter") && s.indexOf("{")>=0 ) {
      	println("Enter the constraints for the new filter");
      	filterMode = true;
      	filterDef = new StringBuffer();
      }

      if (filterMode) {
         filterDef.append("\n" + s);

         // a-t-on atteint la fin de la definition ?
         if (Action.countNbOcc('{', filterDef.toString()) <=
            Action.countNbOcc('}', filterDef.toString())) {
            filterMode = false;
            // robot
            if( robotMode &&  Aladin.ROBOTSUPPORT ) {
               // on force le mode "Advanced"
               FilterProperties.setCurrentMode(FilterProperties.ADVANCED);
               robot.executeCommand("filter", filterDef.toString());
            }
            else {
               if( createFilter(filterDef.toString())!=null) a.calque.select.repaint();
            }
            return "";
         }

         if (Action.countNbOcc('\n', filterDef.toString()) > 1) println("Enter other constraints for the new filter");
         return "";
      }
      
      // Echo sur la console
      if( echo ) a.console.printCommand(s1);
      
      // Commentaire
      if( s1.length()>0 && s1.trim().charAt(0)=='#' ) return "";
      
      if( robotMode &&  Aladin.ROBOTSUPPORT ) {
          if( robot.executeCommand(cmd, param) ) return "";
      }
      
      a.trace(4,"Command.exec() : execute now \""+cmd+" "+param+"\"...");
      
           if( cmd.equalsIgnoreCase("taquin") ) a.view.taquin(param);
//      else if( cmd.equalsIgnoreCase("skygen") ) execSkyGen(param);
      else if( cmd.equalsIgnoreCase("macro") )  execMacro(param);
//      else if( cmd.equalsIgnoreCase("createRGB") ) testCreateRGB(param);
      else if( cmd.equalsIgnoreCase("test") )   hop();
      else if( cmd.equalsIgnoreCase("testlang") ) a.chaine.testLanguage(param);
      else if( cmd.equalsIgnoreCase("testimg") )testCalib(label,param,0);
      else if( cmd.equalsIgnoreCase("testcat") )testCalib(label,param,1);
      else if( cmd.equalsIgnoreCase("testscript"))testscript(param);
      else if( cmd.equalsIgnoreCase("testperf"))testperf(param);
      else if( cmd.equalsIgnoreCase("call"))    execFunction(param);
      else if( cmd.equalsIgnoreCase("="))       execEval(param);
      else if( cmd.equalsIgnoreCase("convert")) execConvert(param);
      else if( cmd.equalsIgnoreCase("list"))    return listFunction(param);
      else if( s.trim().startsWith("addcol") ) { execAddCol(s); return ""; }
      else if( cmd.equalsIgnoreCase("select") ) execSelectCmd(param);
      else if( cmd.equalsIgnoreCase("tag") )    a.tagselect();
      else if( cmd.equalsIgnoreCase("untag") )  a.untag();
      else if( cmd.equalsIgnoreCase("reloadglu") )  a.glu = new Glu(a);
      else if( cmd.equalsIgnoreCase("goto") )   goTo(param);
      else if( cmd.equalsIgnoreCase("crop") )   execCropCmd(param,label);
      else if( cmd.equalsIgnoreCase("match") )   execMatchCmd(param);
      else if( cmd.equalsIgnoreCase("stick") )  execViewCmd(param,STICKVIEW);
      else if( cmd.equalsIgnoreCase("unstick") )execViewCmd(param,UNSTICKVIEW);
      else if( cmd.equalsIgnoreCase("lock") )   execViewCmd(param,LOCKVIEW);
      else if( cmd.equalsIgnoreCase("unlock") ) execViewCmd(param,UNLOCKVIEW);
      else if( cmd.equalsIgnoreCase("northup") )   execViewCmd(param,NORTHUP);
      else if( cmd.equalsIgnoreCase("unnorthup") ) execViewCmd(param,UNNORTHUP);
      else if( cmd.equalsIgnoreCase("grey")
            || cmd.equalsIgnoreCase("gray") )   a.grey();
      else if( cmd.equalsIgnoreCase("quit") )   a.quit(0);
      else if( cmd.equalsIgnoreCase("get") )    return execGetCmd(param,label,true);
      else if( cmd.equalsIgnoreCase("set") )    return execSetCmd(param);
      else if( cmd.equalsIgnoreCase("setconf") )return execSetconfCmd(param);
      else if( cmd.equalsIgnoreCase("status") ) return execStatusCmd(param);
      else if( cmd.equalsIgnoreCase("info") )   execInfo(param);//a.status.setText(param);
      else if( cmd.equalsIgnoreCase("help") )   execHelpCmd(param,false);
      else if( cmd.equalsIgnoreCase("reset") )  a.reset();
      else if( cmd.equalsIgnoreCase("new") )    a.windows();
      else if( cmd.equalsIgnoreCase("search") ) a.search.execute(param);
      else if( cmd.equalsIgnoreCase("createplane") || cmd.equalsIgnoreCase("cplane")
            || cmd.equalsIgnoreCase("plane") )  a.calque.newPlanCatalogBySelectedObjet(label!=null?label:param,false);
      else if( cmd.equalsIgnoreCase("thumbnail")
            || cmd.equalsIgnoreCase("createROI")
            || cmd.equalsIgnoreCase("ROI") )    execROICmd(param);
      else if( cmd.equalsIgnoreCase("stc") )    execDrawCmd("draw",param);
      else if( cmd.equalsIgnoreCase("draw") )   execDrawCmd(cmd,param);
      else if( cmd.equalsIgnoreCase("rename") || cmd.equalsIgnoreCase("ren") ) {  // For compatibility
         try {
            Plan p=null;
            st = new Tok(param);
            String name = st.nextToken();
            String nameDst = st.nextToken();
            if( nameDst.length()>0 ) {
               p = getNumber(name);
            }
            else {
               p=a.calque.getFirstSelectedPlan();
               nameDst=name;
            }
            a.calque.rename(p,nameDst);
         } catch ( Exception e ) {
            printConsole("!!! rename error: "+e.getMessage());
            return "";
         }
      }
      else if( cmd.equalsIgnoreCase("grid") ) {
              boolean flag= !param.equals("off");
              a.calque.setGrid(flag,false);
              a.calque.repaintAll();
           }
//      else if( cmd.equalsIgnoreCase("hist") || cmd.equals("h") )   {
//            try { n=Integer.parseInt(param); }
//            catch( Exception e ) { n=10; }
//            a.pad.hist(n);
//           }
      else if( cmd.equalsIgnoreCase("pause") ) {
               double m;
               try { m=Double.parseDouble(param); if( m<=0 ) m=1; }
               catch( Exception e ) { m=1; }
               Util.pause((int)(Math.round(m*1000)));
           }
      else if( cmd.equalsIgnoreCase("timeout") ) {   // pour compatibilité
               return execSetconfCmd("timeout="+param);
           }
      else if( cmd.equalsIgnoreCase("reticle")) {
              int mode = param.equals("off") ? 0 : param.equals("large") ? 2:1;
              a.calque.setReticle(mode);
              a.calque.repaintAll();
           }
      else if( cmd.equalsIgnoreCase("target")) {
              boolean flag= !param.equals("off");
              a.calque.setOverlayFlag("target", flag);
              a.calque.repaintAll();
           }
      else if( cmd.equalsIgnoreCase("scale") || cmd.equalsIgnoreCase("overlay") ) {
              boolean flag= !param.equals("off");
              a.calque.setOverlay(flag);
              a.calque.repaintAll();
           }
      else if( cmd.equalsIgnoreCase("flipflop") ) {
            execFlipFlop(param,label);
      }
      else if( cmd.equalsIgnoreCase("reverse") ) {    // Pour compatibilité
              boolean flag= !param.equals("off");
              execCM(flag?"reverse":"noreverse");
            }
      else if( cmd.equalsIgnoreCase("blink") ) {
              PlanImage p[] = getPlanImage(param);
              if( p.length<2 ) {
                 printConsole("!!! blink error: 2 images are required for blinking");
                 return "";
              }
              a.calque.newPlanImageBlink(p,label,800);
              syncNeedRepaint=true;
           }
      else if( cmd.equalsIgnoreCase("mosaic") ) {
         PlanImage p[] = getPlanImage(param);
         if( p.length<2 ) {
            printConsole("!!! mosaic error: 2 images are required for mosaic");
            return "";
         }
         a.calque.newPlanImageMosaic(p,label,null);
         syncNeedRepaint=true;
      }
      else if( cmd.equalsIgnoreCase("resamp") || cmd.equalsIgnoreCase("rsamp")) {
             try {
                boolean fullPixel=false;
                int methode=PlanImageResamp.BILINEAIRE;
                st = new Tok(param);
                PlanImage p1 = (PlanImage)getPlanFromParam(st.nextToken());
                PlanImage p2 = (PlanImage)getPlanFromParam(st.nextToken());
                while( st.hasMoreTokens() ) {
                   char c = st.nextToken().charAt(0);
                   if( c=='8' ) fullPixel=false;
                   if( c=='F' || c=='f' ) fullPixel=true;
                   if( c=='C' || c=='c' ) methode=PlanImageResamp.PPV;
                   if( c=='B' || c=='b' ) methode=PlanImageResamp.BILINEAIRE;
                }
                a.calque.newPlanImageResamp(p1,p2,label,methode,fullPixel,true);
                syncNeedRepaint=true;
            } catch( Exception e ) {
                printConsole("Resamp error: "+e.getMessage());
                return "";
             }
           }
      else if( (fct=findAlgebre(s))>=0 ) {
         if( syncMode==SYNCON ) sync();
         try {
            st = new Tok(s);
            PlanImage p1=null,p2=null;
            String v1,v2;
            double coef=Double.NaN;
            n = st.countTokens();
            if( n<=2 ) {
               v2 = s.substring(1).trim();
               p2 = (PlanImage)getPlanFromParam(v2,0,true);
               if( p2==null ) try { coef = Double.parseDouble(v2); } catch( Exception e ) {}
               if( p1==null && p2==null && Double.isNaN(coef)) throw new Exception();
               a.calque.newPlanImageAlgo(label,p1,p2,fct,coef,null,PlanImageAlgo.BILINEAIRE);
           } else if( n==3 ) {
               v1 = st.nextToken();
               st.nextToken();
               v2 = st.nextToken();
               p1 = (PlanImage)getPlanFromParam(v1,0,true);
               if( p1==null ) try { coef = Double.parseDouble(v1); } catch( Exception e ) {}
               p2 = (PlanImage)getPlanFromParam(v2,0,true);
               if( p2==null ) try { coef = Double.parseDouble(v2); } catch( Exception e ) {}
               if( p1==null && p2==null && Double.isNaN(coef)) throw new Exception();
               a.calque.newPlanImageAlgo(label,p1,p2,fct,coef,null,PlanImageAlgo.BILINEAIRE);
            } else throw new Exception();
         } catch( Exception e ) { printConsole("!!! Arithmetic expression syntax error: "+e.getMessage()); return "error"; }
      }
      else if( cmd.equalsIgnoreCase("norm") ) {
         try {
            fct = PlanImageAlgo.NORM;
            st = new Tok(param);
            String v1 = st.nextToken();
            String v2 = st.hasMoreTokens() ? st.nextToken() : null;
            PlanImage p1=null;
            if( v1!=null && v1.equals("-cut") ) { fct=PlanImageAlgo.NORMCUT; v1=v2; }
            if( v1!=null ) p1 = (PlanImage)getPlanFromParam(v1);
            a.calque.newPlanImageAlgo(label,p1,null,fct,0,null,0);
            syncNeedRepaint=true;
         } catch( Exception e ) { printConsole("!!! norm error: "+e.getMessage()); return "error"; }
       }
      else if( cmd.equalsIgnoreCase("kernel") ) {
         String s2="";
         
         // Pas de paramètre => retourne la liste des noms de kernels
         if( param.trim().length()==0 ) s2 = a.kernelList.getKernelList();
         
         else {
            // tente d'ajouter une définition suivant la syntaxe toto=1 1 1 1 1 1 1 1 1
            if( param.indexOf('=')>0 ) {
               double pixRes=1/3600.; // par défaut 1" par pixel
               try { pixRes = a.view.getCurrentView().pref.projd.getPixResDelta(); }
               catch( Exception e ) {}
               try { a.kernelList.addKernel(param,pixRes);
               } catch( Exception e ) {
                  printConsole("!!! conv error: kernel definition error"); 
                  return "error";
               }
            // Affichage les kernels qui correspondent au masque passé en paramètre
            } else s2 = a.kernelList.getKernelDef(param);
         }
         print(s2);
         a.console.printInPad(s2);
         return s2;
      }
      else if( cmd.equalsIgnoreCase("conv") ) {
         try {
            st = new Tok(param);
            String v1 = st.nextToken();
            String conv=null;
            PlanImage p1 = (PlanImage)getPlanFromParam(v1,0,true);
            if( p1!=null ) conv = param.substring(v1.length()).trim();
            else conv=param;
            a.calque.newPlanImageAlgo(label,p1,null,PlanImageAlgo.CONV,0,conv,0);
            syncNeedRepaint=true;
        } catch( Exception e ) { printConsole("!!! conv error: "+e.getMessage()); return "error"; }
      }
      else if( cmd.equalsIgnoreCase("bitpix") ) {
         try {
            fct = PlanImageAlgo.BITPIX;
            st = new Tok(param);
            String v1 = st.nextToken();
            String v2 = st.hasMoreTokens() ? st.nextToken() : null;
            String v3 = st.hasMoreTokens() ? st.nextToken() : null;
            PlanImage p1=null;
            if( v1!=null && v1.equals("-cut") ) { fct=PlanImageAlgo.BITPIXCUT; v1=v2; v2=v3;}
            p1 = (PlanImage)getPlanFromParam(v1,0,true);
            if( p1!=null ) v1=v2;
            String bitpix=v1;
            
            if( p1!=null && !p1.isSimpleImage() 
             || p1==null && !a.calque.getPlanBase().isSimpleImage() ) { throw new Exception("Uncompatible image");  }

            a.calque.newPlanImageAlgo(label,p1,null,fct,0,bitpix,0);
            syncNeedRepaint=true;
           
         } catch( Exception e ) { printConsole("!!! bitpix error: "+e.getMessage()); return "error"; }
      }
      else if( cmd.equalsIgnoreCase("RGB") ) {
               PlanImage p[] = getPlanImage(param);
               if( p.length<2 ) {
                  printConsole("!!! RGB error: not enough images");
                  return "";
               }
               a.calque.newPlanImageRGB(p[0],p.length>2?p[1]:null,
                                        p.length>2?p[2]:p[1],p[0],label,false);
               syncNeedRepaint=true;
           }
      else if( cmd.equalsIgnoreCase("RGBdiff") ) {
               PlanImage p[] = getPlanImage(param);
               if( p.length!=2 ) {
                  printConsole("!!! RGBdiff error: requires two images");
                  return "";
               }
               a.calque.newPlanImageRGB(p[0],p[1],null,p[0],label,true);
               syncNeedRepaint=true;
      }
      else if (cmd.equalsIgnoreCase("cm") ) execCM(param);
      else if( cmd.equalsIgnoreCase("sync") ) sync();
      else if( cmd.equalsIgnoreCase("md") ) {
               boolean local=false;
               if( param.length()>11 && param.substring(0,11).equalsIgnoreCase("-localscope")) {
                  local=true;
                  param = param.substring(11).trim();
               }
               if( param.length()==0 ) a.calque.newFolder(null,0,local);
               else a.calque.newFolder(param,0,local);
               a.calque.select.repaint();
           }
      else if( cmd.equalsIgnoreCase("collapse") ) {
              Plan p[] = getPlan(param,3);
              for( int i=0; i<p.length; i++ ) {
                 if( a.calque.isCollapsed(p[i]) ) continue;
                 a.calque.select.switchCollapseFolder(p[i]);
              }
              a.calque.repaintAll();
          }
      else if( cmd.equalsIgnoreCase("expand") ) {
              Plan p[] = getPlan(param,3);
              for( int i=0; i<p.length; i++ ) {
                 if( !a.calque.isCollapsed(p[i]) ) continue;
                 a.calque.select.switchCollapseFolder(p[i]);
              }
              a.calque.repaintAll();
          }
      else if( cmd.equalsIgnoreCase("copy") ) {
           st = new Tok(param);
           String p1,p2;
           int mview1 = getViewNumber(p1=st.nextToken(),false);
           int mview2 = getViewNumber(p2=st.nextToken(),false);
           if( mview1>=0 && mview2>=0 ) a.view.copyView(mview1,mview2);

           // Il doit s'agir d'un plan
           else {
              Plan p=null;
              Plan plan = getNumber(p1,1,false,false);
              if( plan==null || p2.length()==0 ) {   // plan par défaut (
                 p2=p1;
                 p = a.calque.getFirstSelectedPlan();
              } else p = plan;
              if( label!=null ) p2=label;
              try { 
                 if( p instanceof PlanImageBlink ) a.calque.newPlanImageFromBlink( (PlanImageBlink)p, -1);
                 else a.calque.dupPlan((PlanImage)p,p2.trim().length()==0 ? null:p2,p.type,true);
                 syncNeedRepaint=true;
              } catch( Exception e ) {
                 printConsole("!!! copy error: "+e.getMessage());
                 return "";
              }
           }
         }
      else if( cmd.equalsIgnoreCase("mv") || cmd.equalsIgnoreCase("move") ) {
               // Pour les vues
               st = new Tok(param);
               if( st.countTokens()==2 ) {
                  int mview1 = getViewNumber(st.nextToken(),false);
                  int mview2 = getViewNumber(st.nextToken(),false);
                  if( mview1>=0 && mview2>=0 ) {
                     a.view.moveView(mview1,mview2);
                     return "";
                  }
               }
               // Pour les plans
               st = new Tok(param);
               try {
                  Vector<Plan> vp = new Vector<Plan>(10);
                  while( st.hasMoreTokens() ) {
                     vp.addElement( getNumber(st.nextToken()) );
                  }
                  int j=vp.size();
                  Plan target=vp.elementAt(j-1);
                  for( int i=j-2; i>=0; i--) {
                     a.calque.permute(vp.elementAt(i),target);
                  }
                  syncNeedRepaint=true;
                  a.view.newView(1);
                  a.calque.repaintAll();
               } catch( Exception eMv ) {
                  printConsole("!!! mv error: "+eMv.getMessage());
                  return "";
               }
           }
      else if( cmd.equalsIgnoreCase("rm") || cmd.equalsIgnoreCase("free") ) {
              if( param.equals("lock") || param.equals("-lock")
                    ||param.equals("ROI") || param.equals("-ROI")) a.view.freeLock();
              else if( param.equals("all") || param.equals("-all")) a.calque.FreeAll();

               // Suppression par la sélection
               else if( param.length()==0 ) {
                  a.delete();

               // Suppression par les paramètres
               } else {
                  // Les vues éventuelles
                  ViewSimple v[] = getViews(param);
                  if( v.length>0 ) a.view.free(v);

                  // Les plans
                  a.calque.unSelectAllPlan();
                  Plan p[] = getPlan(param,4);
                  if( p.length>0 ) {
                     for( int i=0; i<p.length; i++ ) p[i].selected=true;
                     a.calque.FreeSet(false);
                  }
                  a.view.setSelectFromView(true);
               }

               a.gc();
               a.calque.repaintAll();
           }
      else if( cmd.equalsIgnoreCase("modeview") || cmd.equalsIgnoreCase("mview")) {
               try { n=Integer.parseInt(st.nextToken());
               if( n!=1 && n!=2 && n!=4 && n!=9 && n!=16 ) n=1; }
               catch( Exception e ) { n=1; }
               a.view.setModeView(n);
               try{
                  n=Integer.parseInt(st.nextToken());
                  a.view.scrollOn(n-1);
               } catch( Exception e ) { }
           }
      else if( cmd.equalsIgnoreCase("createview") || cmd.equalsIgnoreCase("cview") ) {
               boolean plot=false;
               String [] col = new String[2];
               Plan p=null;
               if( param.length()==0 ) p=a.calque.getFirstSelectedPlan();
               else {
                  String p1 = st.nextToken();
                  if( p1.equals("-plot") ) {
                     plot=true;
                     if( !st.hasMoreTokens() ) p=a.calque.getFirstSelectedPlan();
                     else p1 = st.nextToken();
                  }
                  if( plot ) p1 = parseColumnIndex(col, p1);
                  if( p==null ) p = getNumber(p1);
               }
               if( p==null ) return "";
               
               int nview=-1;
               if( st.hasMoreTokens()) {
                  nview = getViewNumber(st.nextToken());
                  a.calque.setPlanRef(p,nview);
               } else nview = a.view.getLastNumView(p);
               if( nview<0 ) return "";
               
               if( !a.view.viewSimple[nview].isPlotView() ) a.view.setPlanRef(nview, p);
               if( plot ) {
                  try {
                     a.view.viewSimple[nview].addPlotTable(p, col[0], col[1] ,false);
                  } catch( Exception e ) {
                     printConsole("!!! cview -plot error: "+e.getMessage());
                  }
               }
               a.calque.repaintAll();
           }
      else if( cmd.equalsIgnoreCase("hide") ) {
               Plan p[] = getPlan(param,2);
               for( int i=0; i<p.length; i++ ) {
                  if( p[i].type==Plan.FOLDER ) a.calque.setActiveFolder(p[i],false);
                  else p[i].setActivated(false);
               }
               a.calque.repaintAll();
           }
      else if( cmd.equalsIgnoreCase("show")
            || cmd.equalsIgnoreCase("ref") ) {
               Plan p[] = getPlan(param,2);
               for( int i=0; i<p.length; i++ ) {
                  if( p[i].type==Plan.FOLDER ) a.calque.setActiveFolder(p[i],true);
                  else a.calque.showPlan(p[i]);
               }
           }
      else if( cmd.equalsIgnoreCase("zoom") ) {
               syncNeedRepaint=true;
               if( !a.calque.zoom.setZoom(param) ) {
                  printConsole("!!! zoom error: factor \""+param+"\" unknown !");
               }
      }
      else if( cmd.equalsIgnoreCase("backup") ) {
         String syncId = syncSave.start("Command.backup");
         try {
            if( a.save==null ) a.save = new Save(a);
            (a.save).saveAJ(param);
         }
         catch( Exception e ) {}
         finally {
            syncSave.stop(syncId);
         }
      }
      else if( cmd.equalsIgnoreCase("save") ) {
         String syncId = syncSave.start("Command.save");
         try {
              if( a.save==null ) a.save = new Save(a);

              String tmp=null;
              String file=null;
              int w=500,h=500;
              boolean flagDim=false;
              boolean flagROI=false;
              int mode=0;
              int posFile=cmd.length();   // Position du nom du fichier (cochonnerie de blancs)
              float qual=-1;

              if( st.hasMoreTokens() ) tmp = st.nextToken();
              if( tmp!=null ) {

                 // Les ROI ?
                 if( tmp.equals("-ROI" ) ) {
                    flagROI=true;
                    posFile = s.indexOf(tmp)+tmp.length()+1;
                    tmp = st.nextToken();
                 }

                 // Un format indiqué ?
                 if( tmp.startsWith("-jpeg") || tmp.startsWith("-jpg") ) {
                    mode=Save.JPEG;
                    try {
                       qual = Float.parseFloat(tmp.substring(tmp.indexOf('g')+1,tmp.length()));
                       if( qual>1 ) qual/=100;
                    } catch( Exception e ) { qual=-1; }
                    
                    posFile = s.indexOf(tmp)+tmp.length()+1;
                    if( st.hasMoreTokens() ) tmp = st.nextToken();
                    else tmp=null;

                 } else if( tmp.equals("-eps")  ) {
                    mode=Save.EPS;
                    posFile = s.indexOf(tmp)+tmp.length()+1;
                    if( st.hasMoreTokens() ) tmp = st.nextToken();
                    else tmp=null;

                 } else if( tmp.equals("-png")  ) {
                    mode=Save.PNG;
                    posFile = s.indexOf(tmp)+tmp.length()+1;
                    if( st.hasMoreTokens() ) tmp = st.nextToken();
                    else tmp=null;
                 }

                 // Faut-il un fichier de links ?
                 if( tmp!=null && tmp.equals("-lk")  ) {
                    mode|=Save.LK;
                    posFile = s.indexOf(tmp)+tmp.length()+1;
                    if( st.hasMoreTokens() ) tmp = st.nextToken();
                    else tmp=null;
                 }
                 // Fichier de links pour viewer Flex
                 else if( tmp!=null && tmp.equals("-lkflex")  ) {
                     mode|=Save.LK_FLEX;
                     posFile = s.indexOf(tmp)+tmp.length()+1;
                     if( st.hasMoreTokens() ) tmp = st.nextToken();
                 }

              }

              // Une Dimension ?
              if( tmp!=null ) {
                 try {
                    int x = tmp.indexOf('x');
                    w = Integer.parseInt(tmp.substring(0,x));
                    h = Integer.parseInt(tmp.substring(x+1));
                    flagDim=true;
                    if( st.hasMoreTokens() ) {
                       posFile = s.indexOf(tmp)+tmp.length()+1;
                       file=st.nextToken();
                    }
                 } catch( Exception e ) { w=h=View.INITW; file=tmp; }
              }

              // Pour s'assurer que l'on n'a pas que le premier mot du nom du fichier
              // en cas de blancs
              if( file!=null ) file = s.substring(posFile).trim();

//System.out.println("save mode="+mode+" w="+w+" h="+h+" file="+(file==null?"null":file));

              if( flagDim && !a.NOGUI) {
                 tmp="dimension specification required NOGUI mode (-nogui parameter), assume window size";
                 a.warning("save error: "+tmp,1);
                 w=h=View.INITW;
              }

              if( file==null && !a.NOGUI) {
                 tmp="saving on standard output required NOGUI mode (-nogui parameter)";
                 a.warning("save error: "+tmp,1);
                 return tmp;
              } else file = Tok.unQuote(file);

              // Ajustement de la taille dans le mode NOGUI & attente en conséquence (PlanBG)
              if( a.NOGUI ) {
                 ViewSimple v = a.view.getCurrentView();
                 if( w==-1 || h==-1 ) {
                    v.setDimension(((PlanImage)v.pref).width,((PlanImage)v.pref).height);
                    v.setZoomXY(1, -1, -1);
                 } else v.setDimension(w,h);
                 v.paintComponent(null);
              }
              
             // Mode Image non précisé ?
              if( mode==0 || mode==Save.LK || mode==Save.LK_FLEX ) {
                 if( file!=null && (file.endsWith(".jpg") || file.endsWith(".jpeg"))) mode|=Save.JPEG;
                 else if( file!=null && file.endsWith(".eps")) mode|=Save.EPS;
                 else if( file!=null && file.endsWith(".png")) mode|=Save.PNG;
                 else if( file!=null && file.endsWith(".lk")) mode|=Save.LK;
                 else mode|=Save.BMP;
              }

              if( flagROI ) a.view.saveROI(file,w,h,mode);
              else (a.save).saveView(file,w,h,mode,qual);
         }
         catch( Exception e ) { e.printStackTrace(); }
         finally {
            syncSave.stop(syncId);
         }
      }
      else if( cmd.equalsIgnoreCase("export") ) {
         String syncId = syncSave.start("Command.export");
         try {
            if( param!=null && param.startsWith("-ROI") ) {
               String prefix = null;
               try {
                  st.nextToken();
                  prefix = st.nextToken();
               } catch( Exception e ) { prefix=null; }
               a.view.exportROI(prefix);
            } else {

               if( a.save==null ) a.save = new Save(a);
               int posFile=0;
               boolean vot=false;
               boolean fits=false;
               boolean hpx=false;
               int finFile = -1;

               String planID=st.nextToken();

               boolean addXY = false;
               // Paramètre -votable ou -fits ?
               if( planID.charAt(0)=='-' ) {
                  if( planID.indexOf("votable")>0 ) {
                     vot=true;
                     if( planID.indexOf("flex")>0 ) {
                        addXY = true;
                     }
                  }
                  else if( planID.indexOf("fits")>0 ) fits=true;
                  else if( planID.indexOf("hpx")>0 ) hpx=true;
                  planID = st.nextToken();
               }

               posFile=st.getPos();

               Plan p=getNumber(planID);
               if( p==null ) {
                  String tmp="nothing to export";
                  printConsole("!!! export error: "+tmp);
                  return tmp;
               }

               // Pour compatibilité ou format par l'extension
               if( param.endsWith(" votable") || param.endsWith(" VOTABLE")
                     || param.endsWith(".xml") || param.endsWith(".XML")) {
                  vot=true;
                  if( param.endsWith(" votable") || param.endsWith(" VOTABLE")) {
                     finFile = param.length()-" votable".length();
                  }
               } else if( param.endsWith("fits") || param.endsWith("FITS")) {
                  fits=true;
                  if( param.endsWith(" fits") || param.endsWith(" FITS")) {
                     finFile = param.length()-" fits".length();
                  }
               } else if( param.endsWith("hpx") || param.endsWith("HPX")) {
                  hpx=true;
                  if( param.endsWith(" hpx") || param.endsWith(" HPX")) {
                     finFile = param.length()-" hpx".length();
                  }
               }
               String file = finFile==-1 ? s.substring(posFile) : s.substring(posFile,finFile);
               file = Tok.unQuote(file.trim()).trim();
               if( file.endsWith("fits") || file.endsWith("FITS")) {
                  fits=true;
               } else if( file.endsWith("hpx") || file.endsWith("HPX")) {
                  hpx=true;
               }

               if( p instanceof PlanMoc ) (a.save).saveMoc(file, (PlanMoc)p, HealpixMoc.FITS);
               else if( p.isCatalog() ) (a.save).saveCatalog(file,p,!vot,addXY);
               else if( p.isImage() && !(p instanceof PlanImageBlink) ) (a.save).saveImage(file,p,hpx?1:fits?0:2);
               else {
                  String tmp="plane type ["+Plan.Tp[p.type]+"] not supported";
                  printConsole("!!! export error: "+tmp);
                  return tmp;
               }
            }
         } catch( Exception e ) { if( a.levelTrace>=3 ) e.printStackTrace(); }
           finally {
              syncSave.stop(syncId);
         }
      }
      else if( cmd.equalsIgnoreCase("trace") ) {
               if( param.equals("off") || param.equals("0")) {
                   a.setTraceLevel(0);
                   return "";
               }
               try { n=Integer.parseInt(param);
                     if( n>Aladin.MAXLEVELTRACE || n<0 ) n=1;
               } catch( Exception e ) { n=1; }
               a.setTraceLevel(n);
            }
      else if( cmd.equalsIgnoreCase("mem") ) {
               a.gc();
               long total = Runtime.getRuntime().totalMemory()/(1024*1024);
               long free = Runtime.getRuntime().freeMemory()/(1024*1024);
               long max = Runtime.getRuntime().maxMemory()/(1024*1024);
               printConsole("Total used memory: "+
                 (int)(total - free)+ "Mb (total="+total+"Mb free="+free+"Mb max="+max+"Mb)\n");
            }
      else if( cmd.equalsIgnoreCase("gc") ) {
              if( param.equals("off") ) a.gc=false;
              else a.gc=true;
            }
      else if( cmd.equalsIgnoreCase("load") ) {
         a.load(Tok.unQuote(param),label);
      }

      // Pour CFHT-QSO: Renaud Savalle
      // Creation d'un nouveau plan tool
      // arguments; nom du plan
      else if( cmd.equalsIgnoreCase("ptool") )  {
               String nom = st.nextToken();
               printConsole("Creating new PlanTool "+nom);
               a.calque.newPlanTool(nom);
               a.calque.repaintAll();
             }

      // Pour CFHT-QSO: Renaud Savalle
      // Creation d'un repere (tag) pour une position en pixels
      // arguments: numero du plan de reference (image), coordonnees du repere en pixels
      else if( cmd.equalsIgnoreCase("rep") )  {
               String p = st.nextToken();
               Plan plan = getNumber(p);
               if( plan==null ) return "";
               // Coordonnees en pixels
               int x = Integer.parseInt(st.nextToken());
               int y = Integer.parseInt(st.nextToken());
               // Creation du repere
               printConsole("Creating repere ("+x+","+y+") on plane "+p);
               Repere repere = new Repere(plan,a.view.getCurrentView(),x,y);
               // Ajoute le repere sur le plan courant, il doit etre du type PlanTool sinon rien ne se passe
               a.calque.setObjet(repere);
               a.calque.repaintAll();
             }

      // pour mise on/off du robot
      else if( cmd.equalsIgnoreCase("demo") || cmd.equalsIgnoreCase("robot") ) {
          if( !Aladin.ROBOTSUPPORT ) {
              Aladin.warning(a.chaine.getString("NOROBOT"), 1);
              return "";
          }
          if( param.trim().equals("end") && robotInfo!=null ) {
             MyRobot.info("\n\n         T H E     E N D\n\n\n", a);
             // TODO : thomas : pourquoi ? la fin est un peu abrupte ...
//             robotInfo.dispose();
             robotInfo=null;
             robotMode = false;
             if( robot!=null ) robot.reset();

          } else if( param.trim().equals("off") ) {
              robotMode = false;
//              if( infoTxt!=null ) FilterProperties.insertInTA(infoTxt,"\n\n",infoTxt.getText().length());
          }
          else { robotMode = true; }
          return "";
      }

      // thomas : cross-match (les colonnes de coordonnées sont automatiquement reconnues grace aux UCDs)
      else if( cmd.equals("xmatch") )  return execXMatchCmd(s,label);

      // thomas
      else if( cmd.equals("contour") ) {
               PlanImage p = (PlanImage)a.calque.getPlanBase();
               if( p==null || !p.flagOk ) {
                  printConsole("!!! contour error: no image ready !");
                  return "";
               }
               if( p.type==Plan.IMAGERGB || p instanceof PlanImageBlink ) {
                  printConsole("!!! contour error: can't produce contours on this image");
                 return "";
               }

			   // 4 niveaux par defaut
      	       int nbContours=4;
               if( st.hasMoreTokens() ) {
			     String p1 = st.nextToken();
                 try{ nbContours = Integer.parseInt(p1);}
                 catch (NumberFormatException e) { printConsole("!!! contour error: incorrect or missing parameter");return "";}
               }

               int tmp[] = new int[nbContours];
               tmp = FrameContour.generateLevels(nbContours);

               double levels[] = new double[nbContours];

               for(int i=0;i<levels.length;i++) levels[i] = tmp[i];

               boolean useSmoothing = true; // vrai par defaut

               boolean currentZoomOnly = false; // faux par defaut

               try{

                 String p2 = st.nextToken();


                 if (p2.equals("smooth") || p2.equals("nosmooth")) {
                   useSmoothing = p2.equals("smooth")?true:false;
                   String p3 = st.nextToken();
                   currentZoomOnly = p3.equals("zoom")?true:false;
                 }
                 else currentZoomOnly = p2.equals("zoom")?true:false;
               }
               catch(Exception e) {}

               a.calque.newPlanContour(label!=null?label:"Contours",null,levels,new ContourPlot(),useSmoothing,2,currentZoomOnly,true,null);
             }

      // thomas
      // activation/desactivation d'un filtre
      else if( cmd.equals("filter") ) {
      	    String correctSyntax = "Syntax is : filter [filter name] on|off";
      	    int nbParam = st.countTokens();
      	    // dans ce cas, on active tous les filtres
      	    if(nbParam == 1) {
      	    	String onOff = st.nextToken().toLowerCase();

      	    	if(onOff.equals("on")) {
                    PlanFilter.activateAllFilters();
      	    	}
      	    	else if(onOff.equals("off")) {
                    PlanFilter.desactivateAllFilters();
                    PlanCatalog.desactivateAllDedicatedFilters(a);
      	    	}
      	    	else {
      	    	    printConsole("!!! filter error: incorrect parameter \""+onOff+"\"");
      	    	    println(correctSyntax);
      	    	}
      	    }
      	    else if(nbParam==2) {
      	       String fName = st.nextToken();
      	       String onOff = st.nextToken().toLowerCase();

      	       PlanFilter pf = null;
      	       if( (pf=PlanFilter.getFilterByName(fName,a))!=null) {


      	          if(onOff.equals("on")) {

      	             pf.setActivated(true);
      	             pf.updateState();
      	             a.calque.select.repaint();

      	             //if(a.frameConstraint != null && a.frameConstraint.isShowing()) a.frameConstraint.majFrameConstraint();
      	          }
      	          else if(onOff.equals("off")) {
      	             pf.setActivated(false);
      	             pf.updateState();
      	             a.calque.select.repaint();
      	          }
      	          else {
      	             printConsole("!!! filter error: incorrect parameter \""+onOff+"\"");
      	             println(correctSyntax);
      	          }

      	       }

      	       else {
      	          printConsole("!!! filter error: the filter "+fName+" does not exist");
      	       }
      	    }
      	    else {
      	       printConsole("!!! filter error: incorrect number of parameters");
      	       println(correctSyntax);
      	    }
      }

      // Peut être une commande associée à un plugin ?
      else if( a.plugins!=null && a.plugins.findScript(cmd)!=null ) {
         st = new Tok(param);
         String p [] = st.getStrings();
         return a.plugins.execPluginByScript(cmd,p);
      }
           
      // S'agit-il d'un traitement d'une variable
      else if( execVar(s) ) return "";

      // Bon on va donc simplement activer Sesame et déplacer le repere
      else { 
         return execGetCmd(s,label,false);
      }
      return "";
   }
   
   // Traitement d'une commande propre a une variable (genre A = A+1)
   // PAS ENCORE IMPLANTE
   private boolean execVar(String s) {
      return false;
   }

   /** Retourne un tableau de ViewSimple correspondant aux identificateurs
    * de vues passés en paramètre (ex: A2 B3 C1...). (Uniquement les vues
    * visibles) */
   private ViewSimple[] getViews(String param) {
      StringTokenizer st = new StringTokenizer(param);
      Vector<ViewSimple> tmp = new Vector<ViewSimple>();
      while( st.hasMoreTokens() ) {
         int nview = getViewNumber(st.nextToken(),false);
         if( nview>=0 ) tmp.addElement(a.view.viewSimple[nview]);
      }
      
      return tmp.toArray(new ViewSimple[tmp.size()]);
   }

   static final private int STICKVIEW   = 0;
   static final private int UNSTICKVIEW = 1;
   static final private int LOCKVIEW    = 2;
   static final private int UNLOCKVIEW  = 3;
   static final private int NORTHUP     = 4;
   static final private int UNNORTHUP   = 5;

   /** Gestion des commandes traitant plusieurs vues simultanément
    *  ou toutes les vues sélectionnées.
    * @param param la liste des identificateurs de vues ou "" si les
    *              vues sélectionnées
    * @param cmd LOCKVIEW,UNLOCKVIEW,ATTACHVIEW,DETACHVIEW
    */
   private void execViewCmd(String param,int cmd) {
      ViewSimple v[];
      if( param.length()==0 ) {
         if( cmd==UNSTICKVIEW || cmd==UNLOCKVIEW || cmd==UNNORTHUP ) v=a.view.viewSimple;
         else v=a.view.getSelectedView();
      }
      else v=getViews(param);
      for( int i=0; i<v.length; i++ ) {
         if( v[i]==null || v[i].isFree() ) continue;
         switch(cmd) {
         case STICKVIEW:    v[i].sticked=true;  break;
         case UNSTICKVIEW:  v[i].sticked=false; break;
         case LOCKVIEW:     v[i].locked=true;      break;
         case UNLOCKVIEW:   v[i].locked=false;     break;
         case NORTHUP:      v[i].northUp=true;      break;
         case UNNORTHUP:    v[i].northUp=false;     break;
         }
      }
      a.view.repaintAll();
   }


   /** Cree un nouveau filtre en se basant
    *  sur la definition contenue dans def
	*  @param def - definition complete du filtre a creer
    *  @return retourne le planFilter ou null si problème
	*/
   protected PlanFilter createFilter(String def) {

        // lorsque le label est null, le nom du filtre est dans la definition
        PlanFilter pf = (PlanFilter)a.calque.newPlanFilter(null, def);

      	if( pf!=null && pf.isValid() ) {

      	    printConsole("Filter "+pf.label+" created");
            pf.setActivated(true);   // PF
            pf.updateState();        // PF
      	}
      	else {
      	 printConsole("!!! Bad filter syntax !");
           pf=null;
      	// FAIRE QQCH DU GENRE F.CHECKSYNTAX()
   }

        return pf;
   }

   /** Traitement de la commande "addcol"
    *
    * @param s commande complète
    */
   private void execAddCol(String s) {
       s = s.trim();
       /*
       int begin = s.indexOf('(');
       if( begin<0 ) {
           toConsoleln("addcol : syntax is addcol(plane,name,ucd,unit,expr)");
           return;
       }
       int end = s.lastIndexOf(')');
       if( end<0 ) end = s.length()-1;
       s = s.substring(begin+1, end);
       //toConsole(s);
*/
       s = s.substring(6).trim();

       String syntax = "addcol : syntax is addcol plane,name,expr,unit,ucd,nb decimals";

       String param[] = new String[6];
       for( int i=0; i<param.length; i++ ) param[i] = "";

       StringTokenizer st = new StringTokenizer(s, ",", true);
       String curToken, oldToken;
       oldToken = "";
       int i = 0;
       while( st.hasMoreTokens() ) {
           curToken = st.nextToken();
           if( curToken.equals(",") ) {
               if( oldToken.equals(",") ) param[i++] = "";
           }
           else {
               param[i++] = curToken;
           }
           oldToken = curToken;
       }
       String plane, name, ucd, unit, expr;
       plane = param[0].trim();
       name = param[1].trim();
       expr = param[2].trim();
       unit = param[3].trim();
       ucd = param[4].trim();
       int nbDec;
       try {
       	nbDec = Integer.parseInt(param[5].trim());
       }
       catch( NumberFormatException e ) {nbDec = 4; }

       Aladin.trace(3,"expr: "+expr);
       // on cherche le plan en question
       Plan plan = getNumber(plane);
       if( plan==null ) {
          printConsole("!!! addcol error : plane "+plane+" is not in current stack");
          println(syntax);
          return;
       }
       if( !plan.isSimpleCatalog() ) {
          printConsole("!!! addcol error : plane "+plane+" is not a catalogue plane");
          println(syntax);
          return;
       }

       PlanCatalog pc = (PlanCatalog)plan;

       // on vérifie que le nom de la nouvelle colonne n'est pas déja utilisé !
       if( FrameColumnCalculator.colExist(name, pc) ) {
          printConsole("!!! addcol error : A column with label \""+name+"\"already exists in this plane !");
          println(syntax);
          return;
       }

       SavotField f = new SavotField();
       f.setName(name);
       f.setUcd(ucd);
       f.setUnit(unit);

       ColumnCalculator cc = new ColumnCalculator(new SavotField[] {f}, new String[] {expr}, pc, nbDec, a);
       if( !cc.createParser() ) {
          printConsole("!!! addcol error : "+cc.getError());
          println(syntax);
          return;
       }
       cc.compute();
   }

   /** efface le contenu de la frame d'info en mode robot
    */
   protected void reset() {
      if( robotInfo!=null && infoTxt!=null ) infoTxt.setText("");
   }

   protected void goTo(String param) {
      StringTokenizer st = new StringTokenizer(param);
      a.view.gotoThere(param);
//      try {
//         int x = Integer.parseInt(st.nextToken());
//         int y = Integer.parseInt(st.nextToken());
//         a.view.getCurrentView().goTo(x,y);
//      } catch( Exception e ) { e.printStackTrace(); }
   }
   
   
   /************************************* Gestion des fonctions *************************************************/
   
   private ArrayList<Function> function=new ArrayList<Function>();
   private boolean functionLocalDefinition = false;
   
   public void setFunctionLocalDefinition(boolean flag) { functionLocalDefinition=flag; }
   public boolean getFunctionLocalDefinition() { return functionLocalDefinition; }
   
   /** Retourne le nombre de fonctions */
   public int getNbFunctions() {
      return function.size();
   }
   
   /** Retourne la fonction à l'indice indiqué */
   public Function getFunction(int i) {
      if( function==null || i<0 || i>=function.size() ) return null;
      return function.get(i);
   }
   
   /** Retourne la fonction repérée par son nom, ou null si introuvable */
   public Function getFunction(String name) {
      int i = findFunction(name);
      return i<0 ? null : getFunction(i);
   }
   
   private int findFunction(String name) {
      for( int i=0; i<function.size(); i++ ) {
         if( (function.get(i)).getName().equals(name) ) return i;
      }
      return -1;
   }
   
   public void addFunction(Function f) {
      String name = f.getName();
      f.setLocalDefinition(functionLocalDefinition);
      int i = findFunction(name);
      if( i>=0 ) function.set(i,f);
      else function.add(f);
      functionModif=true;
   }
   
   public void removeFunction(Function f) {
      int i = findFunction(f.getName());
      if( i<0 ) return;
      function.remove(i);
      functionModif=true;
   }
   
   public void setFunctionModif(boolean flag) {
      functionModif=flag;
      Iterator it = function.iterator();
      while( it.hasNext() ) {
         Function f = (Function)it.next();
         if( !f.isLocalDefinition() ) continue;
         f.setModif(false);
      }
   }
   
   private boolean functionModif=false;
   public boolean functionModif() {
      if( functionModif ) return true;
      Iterator<Function> it = function.iterator();
      while( it.hasNext() ) {
         Function f = it.next();
         if( !f.isLocalDefinition() ) continue;
         if( f.hasBeenModif() ) return true;
      }
      return false;
   }
   
   // Ajustement d'une syntaxe partielle d'une commande convto où seul
   // le mot clé " to " est repéré => insertion de la commande "convert" en préfixe
   private String convertAdjust(String s) {
      if( s.indexOf("convert")==0 ) return s;
      int n = s.indexOf(" to ");
      if( n<=0 ) return s;
      return "convert "+s.substring(0,n)+" to "+s.substring(n+4);
   }
   
   // Traitement d'une commande de conversion d'unité
   private String execConvert(String s) {
      String res;
      if( s.trim().length()==0 ) {
         StringBuffer s1 = new StringBuffer();
         Enumeration e1 = Unit.symbols();
         while( e1.hasMoreElements() ) {
            String k = (String)e1.nextElement();
            String d = Unit.explainUnit(k);
            s1.append(k+" - "+d+"\n");
         }
         res = s1.toString();
         print(res);
         a.console.printInPad(res);
      } else {
         int n = s.indexOf(" to ");
         String from = s.substring(0,n);
         char c;
         int m=from.length()-1;
         while( m>0 && !Character.isDigit(c=from.charAt(m)) 
               && c!=')'/*  && !Character.isSpaceChar(c) */) m--;
         String to = s.substring(n+4);
         try { 
            from = Computer.compute( from.substring(0,m+1) )+from.substring(m+1);
            Unit m1 = new Unit(from);
            Unit m2 = new Unit();
            m2.setUnit(to);
            m1.convertTo(m2);
            res = m1.getValue()+" "+m1.getUnit();
         } catch( Exception e ) {
            res="!!! Conversion error ["+e.getMessage()+"]";
         }
         a.localisation.setTextSaisie(res);
         printConsole(res);
      }
      return res;
   }
   
   // Traitement de l'évaluation d'une expression arithmétique
   private String execEval(String p) {
      String res;
      if( p.trim().length()==0 ) {
         res=Computer.help();
         print(res);
         a.console.printInPad(res);
      } else {
         try {
            res = Computer.compute(p)+"";
         } catch( Exception e ) {
            res="!!! Eval error ["+e.getMessage()+"]";
         }
         a.localisation.setTextSaisie(res);
         printConsole(res);
      }
      return res;
   }
   
   // Petit ajustement éventuel pour une commande "=expression" ou "expression="
   // afin de retourner la chaine "= expression"
   private String evalAdjust(String s) {
      int n=s.length();
      if( n==0 || (s.charAt(0)!='=' && s.charAt(n-1)!='=') ) return s;
      if( s.charAt(0)=='=' && !Character.isSpace( s.charAt(1) ) ) return "= "+s.substring(1);
      if( s.charAt(n-1)=='=' ) return "= "+s.substring(0,n-1).trim();
      return s;
   }
   
   private String execFunction(String p) {
      try {
         String name=p;
         String param="";
         int i = p.indexOf('(');
         if( i>0 ) {
            name = p.substring(0,i);
            int j = p.lastIndexOf(')');
            param = p.substring(i+1,j);
         }
         Function f = getFunction(name);
         if( f==null ) return "Function unknown ["+name+"]";
         return f.exec(a,param,false);
      } catch( Exception e ) {
         return "Function syntax error ["+p+"]";
      }
   }
   
   private String listFunction(String mask) {
      try {
         boolean verbose=false;
         if( mask!=null && mask.length()>0 ) verbose=true;
         else mask=null;
         StringBuffer s = new StringBuffer(1000);
         Iterator<Function> e = function.iterator();
         while( e.hasNext() ) {
            Function f = e.next();
            String name = f.getName();
            if( mask!=null && !Util.matchMask(mask, f.getName()) ) continue;
            if( verbose ) s.append(f+"\n");
            else s.append(name+(f.getDescription().length()>0?" - "+f.getDescription():"")+"\n");
         }
         print(s.toString());
         a.console.printInPad(s.toString());
         return s.toString();
         
      } catch( Exception e ) {
         e.printStackTrace();
         return e.getMessage();
      }
   }
   
   public void resetBookmarks() {
      if( function==null ) return;
      Iterator<Function> e = function.iterator();
      while( e.hasNext() ) (e.next()).setBookmark(false);
   }
   
   public Vector<Function> getBookmarkFunctions() { return getFunctions(0); }
   protected Vector<Function> getLocalFunctions() { return getFunctions(1); }
   
   /** Récupération d'une liste de fonctions
    * @param mode  0 - les fonctions bookmarkées,  1- Les fonctions locales
    */
   private Vector<Function> getFunctions(int mode) {
      Vector<Function> v=new Vector<Function>(10);
      if( getNbFunctions()==0 ) return v;
      Iterator<Function> e = function.iterator();
      while( e.hasNext() ) {
         Function f = e.next();
         if( mode==0 && f.isBookmark() ) v.addElement(f);
         else if( mode==1 && f.isLocalDefinition() ) v.addElement(f);
      }
      return v;
   }
   
   
   /**************************************  Test de non régression du code **************************************/

   private String TEST =
      "info Aladin test script in progress...;" +
      "reset;" +
      "setconf frame=ICRS;" +
      "setconf timeout=1;" +
      "mview 4;" +
      "get ESO(dss1,25,25),Aladin(DSS2) M1;" +
      "get skyview(Surveys=2MASS,400,Sin) m1;" +
      "cview DSS* A1;" +
      "cview Sk* B1;" +
      "cview ESO* A2;" +
      "set ESO* planeID=ESO.DSS1;" +
      "cm A2 noreverse 2200..13000 log;" +
      "mv A2 B2;" +
      "mv B1 A2;" +
      "mv B2 B1;" +
      "select Sk*;" +
      "contour;" +
      "select DSS*;" +
      "contour;" +
      "select Sk*;" +
      "draw mode(radec);" +
      "draw tag(05:34:30.87,+22:01:02.1,\"Crab nebulae\",50,-30,circle,14);" +
      "set Draw* color=yellow;" +
      "draw mode(xy);" +
      "draw phot(63 57 15);" +
      "get LEDA M1;" +
      "hide Contours;" +
      "select ESO*;" +
      "zoom 1x;" +
      "backup back.aj;" +
      "reset;" +
      "pause 2;" +
      "load back.aj;" +
      "RGB = RGB ESO* DSS*;" +
      "zoom 1x;" +
      "rm DSS*;" +
      "rm Contours~1;" +
      "show Contours;" +
      "Gauss = conv Skw* gauss(fwhm=10\",radius=12);" +
      "Gauss1 = Gauss;" +
      "set Gauss1 FITS:CRPIX1=203;" +
      "Gauss = Gauss / Gauss1;" +
      "rm Gauss1;" +
      "setconf frame=Gal;"+
      "184.57316 -05.83741;" +
      "flipflop Gauss V;" +
      "Crop = crop Gauss 100x100;" +
      "rm Gauss;" +
      "rm A2;" +
      "Cube=blink Sk* Crop ESO*;" +
      "rm Sk*;" +
      "PR=get Press;" +
      "set PR opacity=65;" +
      "rm A2;" +
      "pause 2;" +
      "get Simbad M1 14';" +
      "call NED(M1,\"10'\");" +
      "get Vizier(USNO);" +
      "md Fold;" +
      "mv RGB Fold;" +
      "sync;" +
      "mv I/284 Fold;" +
      "filter Magn {;" +
      "$[phot.mag*]<15 {draw rainbow(${Imag}) fillcircle(-$[phot.mag*]) };" +
      "};" +
      "mv Magn Fold;" +
      "set Fold scope=local;" +
      "rm USNO;" +
      "XMatch = xmatch Simbad NED 45;" +
      "addcol XMatch,B-V,${B_tab1}-${V_tab1};" +
      "select XMatch;" +
      "search -B-V=\"\";" +
      "tag;" +
      "cplane B-V;" +
      "rm XMatch;" +
      "get Fov(HST);" +
      "mv HST Fold;" +
      "export Simbad Cat.xml;" +
      "export NED Cat1.tsv;" +
      "export Crop Img.jpg;" +
      "rm Simbad NED Crop;" +
      "load Img.jpg;" +
      "grey Img.jpg;" +
      "rm A2;" +
      "load Cat.xml;" +
      "load Cat1.tsv;" +
      "mv Img.jpg Fold;" +
      "mv Cat.xml Fold;" +
      "mv Cat1.tsv Fold;" +
      "set Img.jpg opacity=20;" +
      "collapse Fold;" +
      "get allsky(SHASSA);" +
      "set proj=AITOFF;" +
      "zoom 180°;" +
      "rm B1;" +
      "get allsky(Mellinger);" +
      "m1;" +
      "zoom 15';" +
      "rm B1;" +
      "set Melling* opacity=30;" +
      "get allsky(\"Simbad density\");" +
      "set proj=CARTESIAN;" +
      "cm eosb reverse log;" +
      "M1;" +
      "zoom 30°;" +
      "set opacity=30;" +
      "rm B1;" +
      "cview -plot I/284(Imag,R2mag) B1;" +
      "sync;" +
      "select B-V;" +
      "grid on;" +
      "setconf overlays=-label;" +
      "info The end !;"
      ;

   /** Test des vues et des opérations arithmétiques sur les images
    * 1) Je crée une image test
    */
   protected void testscript(String param) {
      a.console.setVisible(true);
      a.console.clearPad();
      a.console.printInPad(TEST.replace(';','\n') );
      execScript(TEST);
      a.glu.showDocument("Http","http://aladin.u-strasbg.fr/java/Testscript.jpg",true);
   }
   
   /** Test de lecture et d'écriture du disque */
   private void testperf(final String param) {
      final long GB = 1024*1024*1024;
      (new Thread() {
         public void run() {
            String filename = param==null || param.trim().length()==0 ? "AladinTestDisk.tmp" : param;
            File file = new File(filename);
            try {
               if( file.exists() ) {
                  if( param==null || param.trim().length()==0 ) file.delete();
                  else {
                     if( !file.isDirectory() ) {
                        printConsole("!!! testperf error: test file is already existing ["+param+"]");
                        return;
                     }
                     filename= Util.concatDir(filename,"AladinTestDisk.tmp");
                     file = new File(filename);
                     if( file.exists() ) file.delete();
                  }
               }
               byte [] buf = new byte[512];
               for( int i=0; i<buf.length; i++ ) buf[i] = (byte)i;
               long size = GB;
               int cpt=0;

               print("testperf disk writing...");
               long t = System.currentTimeMillis();
               RandomAccessFile f = new RandomAccessFile(filename, "rw");
               while( size>0 ) {
                  f.write(buf);
                  size-= (long) buf.length;
                  cpt+= buf.length;
                  if( cpt>=1024*1024*10 ) { print("."); cpt=0; }
               }
               println("");
               f.close();
               long msw = System.currentTimeMillis() -t;
               double debitw = (GB/(msw/1000.))/(1024*1024);
               
//               println("testperf cache flush...");
//               t = System.currentTimeMillis();
//               try {
//                  testDiskFlush(new File("/"),GB/8);
//               } catch( Exception e1 ) {
//                  e1.printStackTrace();
//               }
//               long msf = System.currentTimeMillis() -t;
//               toStdoutln(" reading 1GB tree file in "+Util.getTemps(msf));
              
               print("testperf disk reading...");
               t = System.currentTimeMillis();
               f = new RandomAccessFile(filename, "rw");
               size = f.length();
               while( size>0 ) {
                  f.read(buf);
                  size-= (long) buf.length;
                  cpt+= buf.length;
                  if( cpt>=1024*1024*10 ) { print("."); cpt=0; }
               }
               println("");
               f.close();
               long msr = System.currentTimeMillis() -t;
               double debitr = (GB/(msr/1000.))/(1024*1024);
               
               double debitmw=0;
               try {
                  println("testperf memory...");
                  long MEM = GB/4;
                  int NBTEST=10;
                  buf = new byte[(int)MEM];
                  for( int j=0; j<buf.length; j++ ) buf[j] = (byte)(j);
                  t = System.currentTimeMillis();
                  for( int i=0; i<NBTEST; i++ ) {
                     for( int j=0; j<buf.length; j++ ) buf[j] = (byte)(buf[j] + j);
                  }
                  size=0;
                  for( int j=0; j<buf.length; j++ ) size += buf[j];
                  println("Optimiser obfuscator...("+size+")...");
                  
                  long msmw = System.currentTimeMillis() -t;
                  debitmw = ((MEM/(msmw/1000.))/(1024*1024*1024) )*NBTEST;
               } catch( Exception e ) { }
               
               
               printConsole("testperf: Disk: w="+ Util.myRound(debitw)+"MB/s r="+Util.myRound(debitr)+"MB/s" +
               		" - Memory: r/w="+ Util.myRound(debitmw)+"GB/s"
               		                        );

               file.delete();

            } catch( Exception e ) {
               e.printStackTrace();
               file.delete();
            }
         } }).start();
   }
   
   /** Méthode pour saturer les caches mémoires systèmes en lisant le début de l'arborescence du disque */
   private long testDiskFlush(File dir,long size) {
      if( size<=0 ) return size;
      
      File f[] = dir.listFiles();
      for( int i=0; f!=null && i<f.length; i++ ) {
         if( f[i].isDirectory() ) {
            size = testDiskFlush(f[i],size);
         } else {
            if( f[i].length()<1024*1024 ) continue; 
            try {
               RandomAccessFile a = new RandomAccessFile(f[i], "r");
               byte [] buf = new byte[512];
               long length = a.length();
               while( length>0 ) {
                  a.read(buf,0, length>buf.length ? buf.length : (int)length); 
                  length-=buf.length;
               }               
               size-=a.length();
               a.close();
            } catch(Exception e ) { e.printStackTrace(); }
         }
         if( size<=0 ) return size;
//         System.out.println(f[i].getAbsolutePath()+" => "+size/(1024*1024)+"MB");
      }
      return size;
   }

   /**
    * Test format et calibration des images et des catalogues
    * @param le nom des plans créés
    * @param param WIDTH HEIGHT RAJ DEJ PIXELSIXE SIN|TAN|... BITPIX
    * @param mode 0:image, 1:catalogue, 2:les deux
    */
   protected void testCalib(String label,String param,int mode) {
      int w=50;
      int h=30;
      int bitpix=16;
      Coord coo =new Coord();
      double szPixel=1/60.;
      int type=Calib.TAN;
      boolean flagCat=(mode==1 || mode==2);
      boolean flagImg=(mode==0 || mode==2);
      if( label==null ) label="Test";

      try {
         StringTokenizer st = new StringTokenizer(param);
         w = Integer.parseInt(st.nextToken());
         h = Integer.parseInt(st.nextToken());
         String s=st.nextToken()+" "+st.nextToken();
         coo = new Coord(s);
         szPixel=Double.parseDouble(st.nextToken())/60.;
         type = Calib.getProjType(st.nextToken())-1;
         if( type<0 ) type=Calib.TAN;
         bitpix = Integer.parseInt(st.nextToken());
      } catch( Exception e ) { }

      String fileImg="Test.fits";
      String fileCat="Test.txt";

      String s = "Calibration grid test:\n"+
        (flagCat?"- Catalog file: "+fileCat+"\n"+
                 "   .catalog size        : "+(w*h/25)+" objects\n"+
                 "   .Dist. between ojects: "+szPixel*300+"\"\n":"")+
         (flagImg?"- Image file  : "+fileImg+"\n"+
                 "   .bitpix              : "+bitpix+"\n"+
                 "   .Image size          : "+w+"x"+h+"\n"+
                 "   .Central coord       : "+coo+"\n"+
                 "   .Pixel size          : "+szPixel*60+"\"\n"+
                 "   .Projection          : "+Calib.projType[type]:"");
      System.out.println(s);
//      a.info(s);

      if( flagImg ) {
         createFitsTest(fileImg,w,h,bitpix,coo.al,coo.del,szPixel,type);
         a.load(fileImg,label);
      }

      if( flagCat ) {
         createTSVTest(fileCat,w,h,coo.al,coo.del,szPixel);
         a.load(fileCat,label+(mode==2 ? " cat":""));
      }
   }
   
//   // Juste pour montrer à Anaïs
//   private void testCreateRGB(String param) {
//      try {
//         StringTokenizer st = new StringTokenizer(param);
//         String rgbFile = st.nextToken();
//         PlanImageRGB rgb = new PlanImageRGB(a,st.nextToken(),null, st.nextToken(), null, st.nextToken(),null);
//         a.save.saveImageColor(rgbFile, rgb, 2);
//      } catch( Exception e ) {
//         e.printStackTrace();
//      }
//   }
   
   /**
    * Création d'un fichier TSV de test */
   protected void createTSVTest(String file,int width,int height,double raj,double dej,double pxSize) {
      try {

         int cx = width/2-1;
         int cy = height/2-1;
         double pxDegSize = pxSize/60.;
         Astrocoo c = new Astrocoo();
         int nl=1;

         File g = new File(a.getDefaultDirectory()+Util.FS+file);
         g.delete();
         RandomAccessFile gf = new RandomAccessFile(g,"rw");
         StringBuffer s = new StringBuffer();
         s.append("RA\tDEC\tX Fits\tY Fits\tSexa\n");

         for( int lig=0; lig<height; lig++ ) {
            double del=dej+(lig-cy)*pxDegSize;
            double cosd = Math.cos(del*Math.PI/180);
            for( int col=0; col<width; col++ ) {
               if( (col-cx)%5!=0 || (lig-cy)%5!=0 ) continue;
               double al=(raj+(col-cx)*pxDegSize) /cosd;
               if( del>90 )  { del = 180-del;  al+=180; }
               if( del<-90 ) { del = -180-del; al+=180 ; }
               if( al<0 ) al+=360.;
               else if( al>360) al-=360;
               c.set(al,del);
               s.append(c.getLon()+"\t"+c.getLat()+"\t"+(col+1)+"\t"+(lig+1)+"\t"+c.toString("2s")+"\n");
               nl++;
               if( nl==1000 ) { writeString(gf,s.toString()); nl=0; s = new StringBuffer(); }
            }
         }
         if( nl!=0 )writeString(gf,s.toString());
         gf.close();

      }catch( Exception e ) { e.printStackTrace();  }
   }

   // Ecriture d'une ligne dans le fichier
   private void writeString(RandomAccessFile gf,String s) throws Exception {
      char [] a = s.toString().toCharArray();
      byte [] b = new byte[a.length];
      for( int j=0; j<a.length; j++ ) b[j] = (byte) a[j];
      gf.write(b);
   }

   /** Création d'une image de test */
   protected void createFitsTest(String file,int width,int height,int bitpix,double raj,double dej,double pxSize,int type) {
      try {

         double rm = pxSize*width;
         double rm1 = pxSize*height;
         int cx = width/2;
         int cy = height/2;
         Projection p = new Projection("Test",Projection.SIMPLE,raj,dej,rm,rm1,
                                              cx-0.5,cy-0.5,width,height,0,true,type,Calib.FK5);

         File g = new File(a.getDefaultDirectory()+Util.FS+file);
         g.delete();
         RandomAccessFile gf = new RandomAccessFile(g,"rw");

         Vector<String> key   = new Vector<String>(20);
         Vector<String> value = new Vector<String>(20);
         try { p.getWCS(key,value); }
         catch( Exception e ) { System.err.println("GetWCS error"); }

         Vector<byte[]> v = new Vector<byte[]>();
         v.addElement( Save.getFitsLine("SIMPLE","T","Aladin image test") );
         v.addElement( Save.getFitsLine("BITPIX",bitpix+"",null) );
         v.addElement( Save.getFitsLine("NAXIS",2+"",null) );
         Enumeration<String> ekey   = key.elements();
         Enumeration<String> evalue = value.elements();
         while( ekey.hasMoreElements() ) {
            String skey   = ekey.nextElement();
            String svalue = evalue.nextElement();
            v.addElement( Save.getFitsLine(skey,svalue,"") );
         }

         // Entete FITS
         long size=0L;
         Enumeration<byte[]> e = v.elements();
         while( e.hasMoreElements() ) {
            byte [] b = e.nextElement();
            gf.write(b);
            size+=b.length;
         }

         // END + bourrage
         byte [] end = new byte[3];
         end[0]=(byte)'E'; end[1]=(byte)'N';end[2]=(byte)'D';
         gf.write(end);
         size+=3;
         byte [] bb = new byte[2880-(int)size%2880];
         for( int i=0; i<bb.length; i++ ) bb[i]=(byte)' ';
         gf.write(bb);

         // Si image HUGE, on travaille ligne par ligne
         // sinon d'un bloc avec génération des étoiles tests
         int n = Math.abs(bitpix)/8;
         boolean flagHuge = width*height*n>Aladin.LIMIT_HUGEFILE;

         // Générations des pixels
         byte out[] = new byte[width*n * (flagHuge?1:height)];
         cx--; cy--;   // on compte à partir de 0 et non de 1 comme en FITS

         double fct = (width+height)/1600.;
         int pos=0;

         // Génération d'une grille de coordonnées
         for( int lig=0; lig<height; lig++ ) {
            for( int col=0; col<width; col++ ) {
               double c = lig==cy && col==cx ? 900 :
                      lig==cy || col==cx ? 700 :
                      lig>cy-100 && lig<cy && col>cx && col<cx+100? -100 :
                     (lig-cy)%100==0 || (col-cx)%100==0 ? 500 :
                     (lig-cy)%10==0 || (col-cx)%10==0 ? 300 :
                     (lig-cy)%5==0 || (col-cx)%5==0 ? 200 :
                      lig<cy && col<cx ? (lig+col)/fct -100 :
                      lig%2==0 || col%2==0 ? 0:-100;
               if( bitpix==8) c = (c+100)/4;
               PlanImage.setPixVal(out,bitpix,pos++,c);
            }
            if( flagHuge ) { gf.write(out); pos=0; }
         }

         // Génération d'étoiles gaussiennes
         if( !flagHuge ) {
            int sens=1;
            int ik=1;
            for( int col=10; col<100; col+=10 ) {
               int lig = col;
               double k[][] = a.kernelList.getKernel(ik);
               ik+=sens;
               if( ik==6 ) {sens=-1; ik=5; }
               if( ik==0 ) break;
               int m = k[0].length;
               for(int lk=0; lk<m; lk++ ) {
                  for(int ck=0; ck<m; ck++ ) {
                     double c= k[lk][ck]*1000 -100;
                     if( bitpix==8) c = (c+100)/4;
                     int x= cx+(col-m/2+ck);
                     int y= cy-(lig-m/2+lk);
                     if( y<height && y>=0 && x<width && x>=0 ) {
                        PlanImage.setPixVal(out,bitpix,y*width+x,c);
                     }
                     x= (int)cx+(col-m/2+ck);
                     y= (int)cy-(100-(lig-m/2+lk) );
                     if( y<height && y>=0 && x<width && x>=0 ) {
                        PlanImage.setPixVal(out,bitpix,y*width+x,c);
                     }
                  }
               }
            }
            gf.write(out);
         }
         gf.close();

      }catch( Exception e ) { e.printStackTrace();  }
   }
   
   private void hop() {
      try {

         // reference slits from LMS
         AladinData adRef = Aladin.aladin.getAladinData("toto");
         // centroids slits
         Iterator<Obj> itRef = adRef.iteratorObj();

         while (itRef.hasNext() ) {
           Obj objRef = itRef.next();
           double ra  = objRef.getRa();
           double dec = objRef.getDec();
           System.out.println(ra+" | "+dec);
           objRef.setRaDec(ra+0.1, dec+0.1);
         }
         
         adRef.repaint();

       } catch (AladinException e) {
         e.printStackTrace();
       }

   }

}

