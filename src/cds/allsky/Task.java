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

package cds.allsky;

import java.util.Vector;

/** G�re le lancemetn des t�ches n�c�ssaires � la g�n�ration d'un survey HEALPix.
 * Les t�ches doivent h�riter de la classe Builder. Elles peuvent �tre agenc�es cons�cutivement
 * (via un Vector de t�ches). Elles seront ex�cut�es soit en synchrone, soit en asynchrone dans un Thread
 * ind�pendant. Quel que soit le mode de lancement, un thread parall�le (ThreadProgressBar) sera �galement ex�cut� afin
 * de g�rer les affichages �ventuelles (stats, info, erreurs).
 * 
 * L'�x�cution peut �tre temporairement suspendue, voire interrompue en utilisant le Context
 * (respectivement Context.taskAbort() et Context.setTaskPause(boolean))
 * 
 * @author ana�s Oberto & Pierre Fernique
 *
 */
public class Task extends Thread {

	private Context context;
	private Vector<Action> actions;
	private Builder builder=null;   // Builder en cours d'ex�cution
	
	/** Lancement d'une t�che unique
	 * @param context
	 * @param action
	 * @param now  true pour une ex�cution asynchrone, false pour un thread ind�pendant
	 */
    public Task(Context context,Action action,boolean now) throws Exception {
       this.context=context;
       actions = new Vector<Action>();
       actions.add(action);
       
       perform(now);
    }
    
    /**
     * Lancement d'un groupe de t�ches ex�cut�es cons�cutivement.
     * @param context
     * @param actions
     * @param now true pour une ex�cution asynchrone, false pour un thread ind�pendant
     */
    public Task(Context context,Vector<Action> actions,boolean now) throws Exception {
       this.context=context;
       this.actions=actions;
       perform(now);
    }
    
    // Ex�cution des t�ches
    private void perform(boolean now) throws Exception {
       if( context.isTaskRunning() ) throw new Exception("There is already a running task ("+context.getAction()+")");
       
       // D�part imm�diat, ou en Thread � part
       if( now ) run();
       else start();
    }
    
    public void run() {
       ThreadProgressBar progressBar=null;
       
       progressBar = new ThreadProgressBar(context);
       progressBar.start();

       try { 
          context.setTaskRunning(true);
          for( Action a : actions ) {
             if( context.isTaskAborting() ) break;
             builder = Builder.createBuilder(context,a);
             builder.validateContext();
             if( builder.isAlreadyDone() ) {
                context.endAction();
                continue;
             }

             context.startAction(a);
             try {
                builder.run();
                builder.showStatistics();
             } catch( Exception e ) {
                e.printStackTrace();
                context.taskAbort();
             } 
             context.endAction();
          }
          context.setTaskRunning(false);
       }
       catch( Exception e) {  e.printStackTrace(); context.warning(e.getMessage()); }
       finally{ context.setTaskRunning(false); if( progressBar!=null ) progressBar.end(); }
    }
    
    /** Thread de "suivi" de l'ex�cution => g�re les affichages (stats, infos, erreurs) */
    class ThreadProgressBar extends Thread {
       private Context context;
       boolean isRunning = true;
       long lastStat=-1;            // date d'affichage des derni�res stats.
       long tempo;                  // Tempo entre deux affichages de statistiques
       long lastGC=-1;              // date du dernier GC
       long tempoGC=30000;          // Tempo entre deux GC
       
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
                   if( now-lastGC>tempoGC ) { System.gc(); lastGC=now; }
                }
             } catch(Exception e) { }

          }
          context.resumeWidgets();
       }
       
       public void end() { isRunning=false; }
    }


}
