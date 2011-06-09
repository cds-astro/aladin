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

import java.net.*;
import java.io.*;
import java.util.*;

import cds.tools.Util;

/**
* Gestion de l'exportation des plans via un mini-serveur HTTP.
*
* Lorsque Aladin veut exporter un plan ayant des donn�es locales (via un fichier et non
* un serveur Web) - par exemple vers un programme de traitement � distance type S-extractor
* sur le cluster du CDS, il va d�marrer un mini-serveur HTTPD et attendre que le client
* distant lui demande les donn�es du plan en question.
*
* En pratique, il suffit d'appeler :  String Export.export(Plan p) qui retourne l'URL
* temporaire permettant de r�cup�rer les donn�es.
*
* Lorsque tous les plans en cours d'exportation ont �t� effectivement demand�s, le serveur
* HTTPD peut s'arr�ter (d�lai de 10 secondes)
*
* S'il existe plusieurs sessions simultan�es d'Aladin, elles auront chacune un port distinct
*
*
* @author Pierre Fernique [CDS]
* @version 1.0 : avril 2006 - cr�ation
*
*/
public class Export implements Runnable {

   static final int DEFAULT_PORT= 4128;   // Le premier port par d�faut
   static final int MAX=5;                // Nombre de serveurs HTTPD simultan�s possibles

   static final int SERVER = 0;   // Pour cr�er un thread serveur HTTPD
   static final int SOCKET = 1;   // Pour cr�er un thread g�rant le socket courant

   static private Export export = null;   // R�f�rence sur le singleton
   static private boolean serverRunning;  // true si un serveur HTTP est roulant

   private ServerSocket server; // Le serveur HTTPD
   private int port;	        // Port TCP courant
   private int nbExport;	    // Nombre de clients potentiellement en attente
   private Socket _sk;          // Socket courant pour le transmettre au thread fils
   private int _modeRun;        // Pour passage au thread fils (choix de l'aiguillage)
   private boolean lock;        // g�re le verrou du passage des valeurs aux threads fils
   private boolean isStopping;	// true si on a demand� l'arr�t du serveur HTTPD


   // autres r�f�rences
   private Aladin aladin;

   protected Export(Aladin aladin) {
      this.aladin = aladin;
      nbExport=0;
   }


   /**
    * Initialise l'exportation d'un plan par mini-serveur HTTP
    * => Cr�ation du singleton g�rant le mini-serveur HTTP si n�cessaire en cherchant
    * le premier port libre � partir de 4128 et suivants
    * @param plan
    * @return l'url � utiliser pour r�cup�rer les donn�es du plan
    */
   static protected String export(Plan plan) {
      Aladin aladin = plan.aladin;
      try {
         if( export==null ) {
            if( !aladin.confirmation(aladin.dialog,aladin.chaine.getString("EXPORTWARNING")) ) {
               return null;
            }
            export = new Export(aladin);
         }
         export.setStopping(false);
         if( !export.isServerRunning() ) {
            export.port = DEFAULT_PORT;
            int i;
            for( i=0; i<MAX && !export.launchServer(export.port); i++, export.port++);
            if( i==MAX ) return null;
         }
         export.addNbExport(1);

         String ip = InetAddress.getLocalHost().getHostAddress();
         String url = "http://"+ip+":"+export.port+"/"+URLEncoder.encode(plan.label);
export.aladin.trace(1,"export("+plan+") via url="+url);
         return url;
      } catch( Exception e ) {
         e.printStackTrace();
         return null;
      }
   }
   /**
    * Incr�mente ou d�cr�mente le compteur du nombre de plans en cours d'exportation
    * afin de pouvir d�terminer si le serveur httpd peut �tre arr�t� ou non
    * @param n 1 ou -1 pour ajouter un retrancher un plan
    */
   synchronized void addNbExport(int n) { nbExport+=n; }

   /** Positionne le verrou permettant le passage des valeurs aux threads fils */
   synchronized void unlock() { lock=false; }

   /** Attend puis prend le verrou pour le passage des valeurs aux threads fils */
   private void waitLock() {
      while( !getLock() ) Util.pause(100);
   }

   /** Essaye de prendre le verrou */
   synchronized private boolean getLock() {
      if( lock ) return false;
      lock=true;
      return true;
   }

   /** Positionne le flag indiquant que le serveur HTTPD est roulant */
   synchronized void setServerRunning(boolean flag) { serverRunning = flag; }

   /** Retourne true s'il y a un serveur HTTP qui roule */
   synchronized boolean isServerRunning() { return serverRunning; }

   /** Positionne le flag de demande d'arr�t du serveur HTTPD */
   synchronized void setStopping(boolean flag) { isStopping = flag; }

   /** Retourne true si on a demand� l'arr�t du serveur HTTPD */
   synchronized boolean isStopping() { return isStopping; }


   /**
    * D�marrage du serveur HTTPD
    * @param port
    * @return true si ok, false si �a n'a pas march� (ex: port d�j� utilis�)
    * @throws Exception
    */
   private boolean  launchServer(int port) throws Exception {
      try { server = new ServerSocket(port); }
      catch( Exception e ) { return false; }

      waitLock();
      setServerRunning(true);
      _modeRun = SERVER;
      (new Thread(this,"AladinHttpd")).start();;
      return true;
   }

   /**
    * Thread du serveur HTTPD.
    * Attend le client et cr�e un thread s�par� le cas �ch�ant.
    * Lib�re le accept toutes les 10 secondes pour
    * s'assurer que personne n'a demand� sa mort (isStopping())
    *
    */
   private void runServer() {
aladin.trace(2,"Aladin tiny httpd is starting on port "+port);
      unlock();
      try {
         while( !isStopping() ) {
            server.setSoTimeout(10000);
//            server.setReuseAddress(true);
            try {
               Socket sk = server.accept();
               waitLock();
               _sk = sk;
               _modeRun = SOCKET;
               new Thread(this,"AladinHttpdBis").start();
            }catch( Exception e1 ) { }   // Boucle serveur pour pouvoir mourir proprement
         }
         server.close();
      } catch( Exception e ) { }
aladin.trace(2,"Aladin tiny httpd is stopped !");
      setServerRunning(false);
      server=null;
   }

   /** Aiguillage pour thread fils (soit serveur HTTPD soit g�rant d'un socket) */
   public void run() {
      switch( _modeRun ) {
         case SERVER: runServer(); break;
         case SOCKET: runSocket(); break;
      }
   }

   /** Ex�cution du thread fils g�rant un socket pour l'exportation
    * des donn�es d'un plan particulier qui sera connu via la ligne GET /toto HTTP/1.1
    */
   public void runSocket() {
      Socket sk = _sk;
      unlock();
      try {
         InputStream in = sk.getInputStream();
         BufferedOutputStream out = new BufferedOutputStream(sk.getOutputStream());

         // On doit lire tous le flux entrant sinon il va y avoir un "reset by peer"
         // sur le close() du out cot� client
         DataInputStream dis = new DataInputStream(in);
         String s = dis.readLine();
         String s1;
         while( (s1=dis.readLine())!=null && s1.length()>0 );


         StringTokenizer st = new StringTokenizer(s);
         if( st.nextToken().equals("GET") ) {
//            String planID = Util.myDecode(st.nextToken().substring(1));
            String planID = URLDecoder.decode(st.nextToken().substring(1));
aladin.trace(2,"Sending plane ["+planID+"] to "+sk.getInetAddress().getHostAddress()+"...");

            send(out,planID);
         }

         out.flush();
         in.close();
         out.close();

         sk.close();
         addNbExport(-1);
         if( nbExport<=0 ) setStopping(true);
      } catch( Exception e) { e.printStackTrace(); }
   }

   /** Envoi d'un string dans le socket */
   private void sendString(OutputStream out,String s) throws Exception {
      char a[] = s.toCharArray();
      byte b[] = new byte[a.length];
      for( int i=0; i<a.length; i++ ) b[i] = (byte)a[i];
      out.write(b);
   }

   /** Envoi d'une erreur HTTP dans le socket */
   private void sendError(OutputStream out,String msg) throws Exception {
      sendString(out,"HTTP/1.0 404 OK\r\nContent-type: text/plain\r\n\r\n"+msg);
   }

   /** Envoi des donn�es d'un plan image ou catalogue dans le socket */
   private void send(OutputStream out,String planID) throws Exception {
      Plan p=aladin.command.getNumber(planID);
      if( p==null ) { sendError(out,"Plane ["+planID+"] no longer existing"); return; }
      if( p.isSimpleImage() && p.flagOk ) {
         sendString(out,"HTTP/1.0 200 OK\r\nContent-type: image/x-fits\r\n\r\n");
         if( aladin.save==null ) aladin.save = new Save(aladin);
         (aladin.save).saveImageFITS(out,(PlanImage)p);
      } else if( p.isSimpleCatalog() && p.flagOk ) {
         sendString(out,"HTTP/1.0 200 OK\r\nContent-type: text/xml\r\n\r\n");
         aladin.writePlaneInVOTable((PlanCatalog)p, out, false);
      } else sendError(out,"Plan ["+planID+"] is not a valid Aladin plane");
   }

}
