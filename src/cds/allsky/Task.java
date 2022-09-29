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

package cds.allsky;

import java.util.Vector;

import cds.aladin.Aladin;

/** Gère le lancemetn des tâches nécéssaires à la génération d'un survey HEALPix.
 * Les tâches doivent hériter de la classe Builder. Elles peuvent être agencées consécutivement
 * (via un Vector de tâches). Elles seront exécutées soit en synchrone, soit en asynchrone dans un Thread
 * indépendant. Quel que soit le mode de lancement, un thread parallèle (ThreadProgressBar) sera également exécuté afin
 * de gérer les affichages éventuelles (stats, info, erreurs).
 * 
 * L'éxécution peut être temporairement suspendue, voire interrompue en utilisant le Context
 * (respectivement Context.taskAbort() et Context.setTaskPause(boolean))
 * 
 * @author anaïs Oberto & Pierre Fernique
 *
 */
public class Task extends Thread {

	private Context context;
	private Vector<Action> actions;
	private Builder builder=null;   // Builder en cours d'exécution
	
	/** Lancement d'une tâche unique
	 * @param context
	 * @param action
	 * @param now  true pour une exécution asynchrone, false pour un thread indépendant
	 */
    public Task(Context context,Action action,boolean now) throws Exception {
       this.context=context;
       actions = new Vector<Action>();
       actions.add(action);
       
       perform(now);
    }
    
    /**
     * Lancement d'un groupe de tâches exécutées consécutivement.
     * @param context
     * @param actions
     * @param now true pour une exécution asynchrone, false pour un thread indépendant
     */
    public Task(Context context,Vector<Action> actions,boolean now) throws Exception {
       this.context=context;
       this.actions=actions;
       perform(now);
    }
    
    // Exécution des tâches
    private void perform(boolean now) throws Exception {
       if( context.isTaskRunning() ) throw new Exception("There is already a running task ("+context.getAction()+")");
       
       // Départ immédiat, ou en Thread à part
       if( now ) run();
       else start();
    }
    
    public void run() {
       ThreadProgressBar progressBar=null;
       
       progressBar = new ThreadProgressBar(context);
       progressBar.start();
       

       
//       System.out.println("Check actions:");
//       for( Action a : actions ) { System.out.println(" ==> "+a); }

       try { 
          context.setTaskRunning(true);
          for( Action a : actions ) {
             if( context.isTaskAborting() ) break;
             context.running(a+"");
             builder = Builder.createBuilder(context,a);
             builder.validateContext();
             if( builder.isAlreadyDone() ) {
                context.endAction();
                continue;
             }
             
             context.startAction(a);
             try {
                if( !builder.isFake() ) {
                   builder.run();
                   builder.showStatistics();
                }
             } catch( Exception e ) {
                context.taskAbort();
                /* String s = e.getMessage();
                if( s!=null && s.length()>0 ) context.warning(e.getMessage());
                else */ e.printStackTrace();
             } 
             context.endAction();
          }
          context.setTaskRunning(false);
       }
       catch( Exception e) { 
          if( Aladin.levelTrace>=3 ) e.printStackTrace();
          context.error(e.getMessage());
          context.taskAbort();
       }
       finally{ context.setTaskRunning(false); if( progressBar!=null ) progressBar.end(); }
    }
    
    /** Thread de "suivi" de l'exécution => gère les affichages (stats, infos, erreurs) */
    class ThreadProgressBar extends Thread {
       private Context context;
       boolean isRunning = true;
       long lastStat=-1;            // date d'affichage des dernières stats.
       long tempo;                  // Tempo entre deux affichages de statistiques
       long lastGC=-1;              // date du dernier GC
//       long tempoGC=10000;          // Tempo entre deux GC
       
       public ThreadProgressBar(Context context) {
          this.context = context;
          tempo = context instanceof ContextGui ? 1000 : 30000;
        }

       public void run() {
          while( isRunning ) {
             try {
                try { Thread.sleep(1000); } catch( Exception e ) { }
                if( isRunning && !context.isTaskPause() ) {
                   context.progressStatus();
                   long now = System.currentTimeMillis();
                   if( now-lastStat>tempo && builder!=null ) { builder.showStatistics(); lastStat=now; }
//                   if( now-lastGC>tempoGC ) { System.gc(); lastGC=now; }
                }
             } catch(Exception e) { e.printStackTrace();  }

          }
          context.resumeWidgets();
       }
       
       public void end() { isRunning=false; }
    }


}
