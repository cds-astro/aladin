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

import java.util.Vector;

import cds.tools.Util;


/**
 * Gestionnaire de synchronisation. 
 * Permet d'attendre jusqu'� ce qu'une s�rie de t�ches aient �t� accomplie
 * 
 * Utilisation :
 *    Thread a : String id = start("maTache",5000);     // Indique qu'une tache est en ex�cution au max pour 5s
 *    Thread b : waitUntil();                           // Bloque jusqu'� qu'il n'y ait plus de taches en cours
 *    Thread a : stop(id);                          
 *    Thread c : reset();                               
 *                      
 * @version 1.0 Jan 2011 - cr�ation
 * @author  P.Fernique [CDS]
 */
public final class Synchro {
   
   static final boolean VERBOSE = false;  // juste pour faire �ventuellement du deboguing
   
   // Classe d'une t�che
   class Task {
      String taskId;        // identificateur unique de la t�che
      long endTime;         // date max de fin de t�che ou 0 si pas de borne
      
      public String toString() { return taskId+(endTime==0 ? "" : " (watchdog in "+(endTime-System.currentTimeMillis())/1000+"s)"); }
   }
 
   private Vector<Task> taskList;   // Liste des t�ches en cours
   private long defaultDelay;       // Delai max d'une t�che par d�faut, 0 pour illimit�

   public Synchro() { this(0); }
   public Synchro(long defaultDelay) {
      this.defaultDelay=defaultDelay;
      taskList = new Vector<Task>();
   }
   
   /** Indique que plus aucune t�che n'est en attente */
   public boolean isReady() {
      try {
         waitLock();
         checkTask();
         return taskList.size()==0;
      } finally { unlock(); }
   }

   /** Indique qu'une t�che est en cours d'ex�cution
    * @param proposedTaskId Nom de la tache propos�
    * @param delay dur�e max de la t�che en millisecondes (<=0 pour infini)
    * @return taskId retenue (proposedTaskId + un �ventuel suffixe)
    */
   public String start(String proposedTaskId) { return start(proposedTaskId,0); }
   public String start(String proposedTaskId,long delay) {
      try {
         waitLock();
         Task t = new Task();
//         sleep(300);
         t.taskId = getUniqueTaskId(proposedTaskId);
         t.endTime = delay>0 ? System.currentTimeMillis()+delay 
                   : defaultDelay>0 ? System.currentTimeMillis()+defaultDelay 
                   : 0;
         taskList.addElement(t);
         return t.taskId;
      } finally { unlock(); }
   }

   /** Indique que la tache indiqu�e vient de s'achever
    * @param taskId identificateur de la tache (fourni par setTask()
    */
   public void stop(String taskId) {
      try {
         waitLock();
         checkTask(taskId);
      } finally { unlock(); }
   }

   /** Retourne la liste des taches en attente */
   public String waitingTasks() {
      try {
         waitLock();
         return this.toString();
      } finally { unlock(); }
   }

   /** Supprime toutes les taches en attente */
   public void reset() {
      unlock();
      taskList.clear();
   }

   /** Attend jusqu'� ce qu'il n'y ait plus aucune tache en attente */
   public void waitUntil() { try { waitUntil(0); } catch( Exception e) {}  }
   public void waitUntil(long delay) throws Exception {
      long t = System.currentTimeMillis();
      while( !isReady() ) {
         if( delay>0 && System.currentTimeMillis()-t>delay ) throw new Exception("Synchro.waitUntil() time out");
         if( VERBOSE ) System.out.println(Thread.currentThread().getName()+" is waiting for ready...");
         sleep(25);
      }
   }
   
   // Mise en pause 
   private void sleep(int delay) {
      try { Thread.currentThread().sleep(delay); }
      catch( Exception e) { e.printStackTrace(); }
   }
   
   // Retourne un nom de tache unique
   // soit celui propos� s'il n'est pas encore pris, sinon
   // le nom propos� suffix� comme il convient
   private String getUniqueTaskId(String proposedTaskId) {
      int n=0;
      for( Task t : taskList ) {
         if( t.taskId.equals(proposedTaskId) ) n++;
      }
      return n==0 ? proposedTaskId : proposedTaskId+"~"+n;
   }
   
   // Supprime toutes les taches hors d�lais
   private void checkTask() { checkTask(null); }
   
   // Supprime la tache indiqu�e ainsi que celles hors d�lais
   private void checkTask(String taskId) {
      long time = System.currentTimeMillis();
//      sleep(30);
      for( int i=0; i<taskList.size(); i++ ) {
         Task t = taskList.get(i);
         if( t.endTime>0 && t.endTime<time 
               || t.taskId.equals(taskId) ) { 
            taskList.removeElementAt(i); 
            i--; 
         }
      }
   }
   
   public String toString() {
      if( taskList.size()==0 ) return "Synchro: no task";
      StringBuffer s = new StringBuffer("Waiting "+taskList.size()+" tasks:\n");
      for( Task t : taskList ) s.append("."+t+"\n");
      return s.toString();
   }
   
   
   // Gestion d'un lock pour passer les arguments au thread
   transient private boolean lock;
   private final Object lockObj= new Object();
   private void waitLock() {
      while( !getLock() ) {
         sleep(100);
         if( VERBOSE ) System.out.println(Thread.currentThread().getName()+" => waiting lock");
      }
      if( VERBOSE ) System.out.println(Thread.currentThread().getName()+" => get lock");
//      String name = Thread.currentThread().getName();
//      if( name.startsWith("AWT-EventQueue") ) {
//         try { throw new Exception("bidon"); }
//         catch( Exception e) { e.printStackTrace(); }
//      }
   }
   private void unlock() { 
      lock=false; 
      if( VERBOSE ) System.out.println(Thread.currentThread().getName()+" => free lock");
   }
   private boolean getLock() {
      synchronized( lockObj ) {
         if( lock ) return false;
         lock=true;
         return true;
      }
   }
      
   static private boolean testThread() {
      final Synchro synchro = new Synchro();
      Thread a = new Thread("Thread A") {
         public void run() {
            System.out.println("Starting thread A");
            String un = synchro.start("bidule");
            System.out.println("Thread A adds "+un);
            String deux = synchro.start("truc");
            System.out.println("Thread A adds "+deux);
            synchro.stop(un);
            System.out.println("Thread A removes "+un);
            System.out.println("Endind thread A");
         }
      };
      Thread b = new Thread("Thread B") {
         public void run() {
            System.out.println("Starting thread B");
            String un = synchro.start("bidule");
            System.out.println("Thread B adds "+un);
            String deux = synchro.start("truc");
            System.out.println("Thread B adds "+deux);
            synchro.stop(deux);
            System.out.println("Thread B removes "+deux);
            System.out.println("Endind thread B");
         }
      };
      Thread c = new Thread("Thread c") {
         public void run() {
            System.out.println("Starting thread C");
            synchro.waitUntil();
            System.out.println("Endind thread C");
         }
      };

      a.start();
      try { Thread.currentThread().sleep(15); }
      catch( Exception e) { e.printStackTrace(); }
      b.start();
      c.start();
      try { Thread.currentThread().sleep(1000); }
      catch( Exception e) { e.printStackTrace(); }
      System.out.println( "*******************\n"+synchro.waitingTasks() +"*******************\n");
      try { Thread.currentThread().sleep(3000); }
      catch( Exception e) { e.printStackTrace(); }
      synchro.reset();
      System.out.println("Termin�");
      return true;
   }
   
   static private boolean testBasic() {
      Synchro synchro = new Synchro(6000);
      String un = synchro.start("TacheUne", 2000);
      String deux = synchro.start("TacheDeux");
      String trois = synchro.start("TacheUne",3000);
      System.out.println("Les taches indiqu�es : "+un+", "+deux+", "+trois);
      System.out.println( synchro.waitingTasks() );
      Util.pause(2500);
      System.out.println( synchro.waitingTasks() );
      synchro.stop(deux);
      System.out.println( synchro.waitingTasks() );
      synchro.waitUntil();
      System.out.println( synchro.waitingTasks() );
      return true;
   }
   
   public static void main(String [] args) {
      try {
         testBasic();
         testThread();
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}
