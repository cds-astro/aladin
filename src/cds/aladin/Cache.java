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

import java.lang.reflect.Method;

import cds.tools.Util;

/**
 * G�re un cache de m�tadonn�es dans $HOME/.aladin/Cache
 * Les ressources cach�es sont rep�r�es par leurs URLs un peu encod�es (voir encodage())
 * pour pouvoir servir de nom de fichier. Les ressources vieilles sont automatiquement
 * recharg�es au cours de la session Aladin.
 * Une seule m�thode � retenir: 
 *               ==>  InputStream get(String|URL url).
 * 
 * IL FAUDRAIT AJOUTER UN NETTOYAGE POUR LES VIEILLES RESSOURCES QUI N'ONT PAS ETE DEMANDE
 * PENDANT LA SESSION
 * @author Pierre Fernique [CDS]
 *
 */
public final class Cache implements Runnable {
   private Aladin aladin;
   private String dir=null;   // reste � null si cache pas possible
   
   static final String CACHE = "Cache";
   static final int WAIT = 10000;            // Temps avant de lancer l'updater de cache
   static final int OUTOFDATE = 86400000;          // D�lai d'obsolescence du cache
   private Vector todo;                      // Liste des urls � updater
   boolean cacheLock=false;                  // Blocage temporaire du cache (pendant un clear par exemple)
   
   
   protected Cache(Aladin aladin) {
      this.aladin = aladin;
      
      // Cr�ation de $HOME/.aladin/Cache si ce n'est d�j� fait
      if( !aladin.createCache() ) return;
      dir = getCacheDir();
      File f = new File(dir);
      if( !f.isDirectory() && !f.mkdir() ) { dir=null; return; }
      
      todo = new Vector();
   }
   
   /** Retourne le nom du r�pertoire servant de cache */
   static String getCacheDir() {
      return System.getProperty("user.home")+Util.FS+Aladin.CACHE+Util.FS+CACHE;
   }
   
   /** Nettoie le cache */
   void clear() {
      if( !isAvailable() ) return;
      try {
         cacheLock=true;
         File cache = new File(dir);
         String f[] = cache.list();
         for( int i=0; f!=null && i<f.length; i++ ) {
//System.out.println("f="+decodage(f[i]));
            try { (new File(dir+Util.FS+f[i])).delete(); }
            catch( Exception e1 ) { e1.printStackTrace(); }
         }
      } catch( Exception e ) { e.printStackTrace(); }
      cacheLock=false;
Aladin.trace(3,"Clear cache");
   }
   
   /** Retourne true si le cache est actif */
   protected boolean isAvailable() {
      while( cacheLock ) {
         try { Util.pause(100); } catch( Exception e ) {}
      }
      return dir!=null;
   }
   
   /** Retourne le Stream associ� � une URL */
   public InputStream get(URL url) throws Exception { return get(url.toString()); }
   
   /** Retourne le Stream associ� � une URL */
   public InputStream get(String url) throws Exception {
      // Cache non accessible, on retourne directement l'appel distant
      if( !isAvailable() ) return Util.openStream(url);
      
      String id = codage(url);
      File f = new File(dir+Util.FS+id);
      
      // Pr�sent dans le cache, g�nial !
      if( f.isFile() && f.canRead() && f.length()>0 ) {
         
         // Devra �tre mise � jour ?
         if( outOfDate(f) ) add(url);
         aladin.trace(3,"["+url+"] read in cache !");         
         return new FileInputStream(f);
      }
      
      // Absent du cache, on va l'y mettre et appeler � nouveau la m�thode
      if( putInCache(url) ) return get(url);
      
      // Bon, je n'y arrive pas
      System.err.println("Caching not available for ["+url+"] !!!");
//      return (new URL(url)).openStream();
      return Util.openStream(url);
   }
   
   /** Retourne le stream associ� � une URL, d'abord teste le r�seau et sinon le cache */
   public InputStream getWithBackup(String url) throws Exception {
      // Cache non accessible, on retourne directement l'appel distant
      if( !isAvailable() ) return Util.openStream(url);
      
      Exception eBackup=null;
      InputStream is = null;
      
      // Tentative d'acc�s direct par le r�seau
      try {
         is = Util.openStream(url);
         if( is==null ) throw new Exception("cache openStream error");
      } catch( Exception e ) {
         is=null;
         eBackup=e;
      }
      
      String id = codage(url);
      File f = new File(dir+Util.FS+id);
      
      // Ca a march� en direct !
      if( is!=null ) {
         // Devrais-je tenter de remettre � jour le cache
         if( !f.isFile() || outOfDate(f) ) add(url);
         return is;
      }
      
      // Ca n'a pas march� en direct, peut �tre pr�sent dans le cache ?
      if( f.isFile() && f.canRead() && f.length()>0 ) {
         aladin.trace(3,"["+url+"] backup from cache !");         
         return new FileInputStream(f);
      }
      
      // Bon, pas d'autre solution
      throw eBackup;
   }
   
   /** Met dans le cache le r�sultat de l'url indiqu�e */
   public boolean putInCache(String url) throws Exception {
      
      // Lecture
      try  {
//         MyInputStream in = new MyInputStream( (new URL(url)).openStream() );
//         in = in.startRead();
         MyInputStream in = Util.openStream(url);
         byte buf[] = in.readFully();
         
         // Nettoyage et Ecriture
         String id = codage(url);
         File g = new File(dir+Util.FS+id);
         g.delete();
         RandomAccessFile f = new RandomAccessFile(dir+Util.FS+id,"rw");
         f.write(buf);
         f.close();
         aladin.trace(3,"Put in cache ["+url+"]");         
         return true;
      } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
      return false;
   }
   
   /** Met � jour le cache en fonction de la liste todo */
   private void updateCache() {
      aladin.trace(2,"Start cache updater...");
      Vector v;
      synchronized( this ) {
         v = (Vector)todo.clone();
         todo.clear();
      }
      Enumeration e = v.elements();
      while( e.hasMoreElements() ) {
         String url = (String)e.nextElement();
         try { putInCache(url); } catch( Exception e1 ) {}
         Util.pause(1000);   // Pose entre deux
      }
      fin();
   }
   
   /** Ajoute une url dans la liste des ressources qu'il faut mettre � jour dans le cache */
   synchronized private void add(String url) {
      aladin.trace(3,"Add ["+url+"] for cache upgrade");      
      todo.addElement(url);
      startThread();
   }
   
   private Thread thread = null;
   
   synchronized private void fin() { thread=null; }
   
   synchronized private void startThread() {
      if( thread!=null ) return;
      thread = new Thread(this,"Cache updater");
      Util.decreasePriority(Thread.currentThread(), thread);
//      thread.setPriority( Thread.NORM_PRIORITY -2);
      thread.start();
   }
   
   /** Retourne true si le fichier indiqu� date de plus de 24h */
   private boolean outOfDate(File f) {
      return System.currentTimeMillis() - f.lastModified() > OUTOFDATE;
   }
   
   static private char PREFIX = '_';
   
   /** Codage d'une chaine pour servir en tant que nom de fichier.
    * Remplace tous ce qui n'est ni lettre ni chiffre en code hexa
    * pr�fix� par _ */
   static private String codage(String s) {
      StringBuffer r = new StringBuffer();
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) {
         char c = a[i];
         if( !Character.isLetterOrDigit(c) ) r.append(PREFIX+Util.hex(c));
         else r.append(c);
      }
      return r.toString();
   }
   
   /** R�ciproque � la fonction codage() */
   static private String decodage(String s) {
      StringBuffer r = new StringBuffer();
      char a[] = s.toCharArray();
      for( int i=0; i<a.length; i++ ) {
         char c = a[i];
         if( c!=PREFIX ) r.append(c);
         else {
            c = (char)( code(a[i+1])*16 + code(a[i+2]) );
            r.append(c);
            i+=2;
         }
      }
      return r.toString();
   }
   
   static final String HEX = "0123456789ABCDEF";
   /** Retourne la valeur d�cimale corresondant � un digit hexad�cimal */
   static private int code(char c) {
      return HEX.indexOf( Character.toUpperCase(c) );
   }

   public void run() {
      try { thread.sleep(WAIT); } catch( Exception e) {};      
      updateCache();
      thread=null;
   }
}
