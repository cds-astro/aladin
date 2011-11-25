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

import static cds.allsky.Constante.INDEX;
import static cds.allsky.Constante.JPG;
import static cds.allsky.Constante.TESS;
import cds.aladin.Aladin;

public class Task implements Runnable {

	private Context context;
	private BuilderIndex builderIndex;
	private BuilderAllsky builderAllsky;
	private BuilderController builder;
	
	private int mode = -1;                 // Mode courant (INDEX,TESS ou JPG)
	
	private volatile Thread runner = null;

	private ThreadProgressBar progressBar;
	
    public Task(Context context) {
       this.context = context;
       builderIndex = new BuilderIndex(context);
       builderAllsky = new BuilderAllsky(context,-1);
       builder = new BuilderController(context);
    }
    
	public synchronized void startThread(){
	   if(runner == null){
	      runner = new Thread(this);
	      try {
	         runner.start(); // --> appelle run()
	      } catch (Exception e) {
	         e.printStackTrace();
	      }
	   }
	}

	public synchronized void stopThread(){

	   if(runner != null){
	      runner = null;
	      Aladin.trace(2,"STOP ON "+mode);
	      switch (mode) {
	         case INDEX : builderIndex.stop();break;
	         case TESS : builder.stop();break;
	      }
	      progressBar.stop();
	   }
	}
	
	private void abort(Exception e) {
       e.printStackTrace();
       if( progressBar!=null ) progressBar.stop();
       context.setAbort();
       runner=null;
       Aladin.trace(2,"Allsky... aborted");
  
	}
	
	public void run() {
	   context.setIsRunning(true);
	   try {
	      mode = INDEX;
	      //			if (allsky.toReset()) builder.reset(output);

	      // Créée un répertoire HpxFinder avec l'indexation des fichiers source pour l'ordre demandé
	      // (garde l'ancien s'il existe déjà)
	      if (mode<=INDEX) {

	         boolean init=false;
	         try {
	            // Initialisation des parametres pour vérifier la cohérence immédiatement
	            context.initParameters();
	            if( !context.verifCoherence() ) throw new Exception("Uncompatible pre-existing survey");

	            Aladin.trace(2,"Launch Index (frame="+context.getFrameName()+")");
	            followProgress(mode,builderIndex);

	            init = builderIndex.build();

	            // si le thread a été interrompu, on sort direct
	            if (runner != Thread.currentThread()) throw new Exception("Interrupted by user");

	         } catch( Exception e ) { abort(e); return; }

	         if (init) Aladin.trace(2,"Allsky... => Index built");
	         else Aladin.trace(2,"Allsky... => Use previous Index");
	         setProgress(mode,100);
	         progressBar.stop();
	         context.enableProgress(false,INDEX);
	      }
	      // Création des fichiers healpix fits et jpg
	      if (mode <= TESS) {
	         mode = TESS;
	         Aladin.trace(2,"Launch Tess (frame="+context.getFrameName()+")");
	         //				builder.setThread(runner);
	         followProgress(mode, builder);
	         
	         try {
	            builder.build();
	            
	            // si le thread a été interrompu, on sort direct
	            if (runner != Thread.currentThread()) throw new Exception("Interrupted by user");
	            
	         } catch (Exception e) { abort(e); return; }
	         
	         Aladin.trace(2,"Allsky... => Hpx files built");
	         setProgress(mode,100);
	         progressBar.stop();
	         context.enableProgress(false,TESS);
	      }
	      // création du fichier allsky
	      if (mode <= JPG) mode = JPG;
	      createAllSky();
	      createMoc();
	      context.preview(0);
	      context.setIsRunning(false);

	      runner = null;
	      mode = -1;
	      Aladin.trace(2,"Allsky... done!");
	   } catch (Exception e) {
	      e.printStackTrace();
	   }
	}
	
	private long lastCreatedAllSky=-1L;
	
	private void createMoc() {
	   (new BuilderMoc()).createMoc(context.getOutputPath());
	}

	// création des fichiers allsky
    public boolean  createAllSky() { return createAllSky(true); }
    public boolean  createAllSky(boolean force) {
	   
	   if( !force ) {
	      long now = System.currentTimeMillis();
	      if( now-lastCreatedAllSky<15000 ) return false;  // déjà fait il n'y a pas bien longtemps
	      lastCreatedAllSky = now;
	   }

	   try {
	      builderAllsky.createAllSky(3,64);
	   } catch (Exception e) {
	      e.printStackTrace();
	   }
	   return true;
	}

	void setInitDir(String txt) {
	   context.setInitDir(txt);		
	}

	void setProgress(int stepMode, int i) {
	   context.setProgress(stepMode, i);
	}

	private void followProgress(int stepMode, Progressive o) {
	   progressBar = new ThreadProgressBar(stepMode,o, this);
	   progressBar.start();
	}

	public void doInBackground() throws Exception {
	   
	   // petite vérification avant de lancer
       String input = context.getInputPath();
	   if (input == null || input.equals("")) {
	      System.err.println("No input directory given");
	      context.stop();
	      return ;
	   }

	   if (mode == -1) mode = INDEX;

	   startThread();
	}


	/**
	 * Invoked when task's progress property changes.
	 */
	//    public void propertyChange(PropertyChangeEvent evt) {
	//        if ("progress" == evt.getPropertyName()) {
	//            int progress = (Integer) evt.getNewValue();
	//            //allskyPanel.setProgress(mode,progress);
	//        } 
	//    }

	/**
	 * Teste s'il n'y a plus de tache en cours (arret normal ou interrompu)
	 */
	public boolean isDone() {
	   return runner == null;
	   //		return runner != Thread.currentThread();
	}

//	public void setLastN3(int lastN3) {
//	   config.setLastN3(lastN3);
//	}


	class ThreadProgressBar implements Runnable {

	   private boolean stopped = false;
	   int last = -1;
	   Progressive builder;
	   Task tasks;

	   int mode ;
	   public ThreadProgressBar(int stepMode, Progressive source, Task allskyTask) {
	      mode=stepMode;
	      builder = source;
	      tasks = allskyTask;
	   }
	   public synchronized void start(){
	      stopped=false;
	      // lance en arrière plan le travail
	      (new Thread(this)).start();
	   }
	   public synchronized void stop() {
	      stopped=true;
	   }

	   /**
	    * Va chercher la dernière valeur de progression
	    */
	   public void run() {
	      int value = 0;
	      String txt = "";
	      while(builder != null && !stopped) {// && value < 99) {
	         switch (mode) {
	            case INDEX :
	               value = builder.getProgress();
	               txt = ((BuilderIndex)builder).getCurrentpath();
	               tasks.setInitDir(txt);
	               break;
	            case TESS : 
	               value = builder.getProgress();
	               int n3 = ((BuilderController)builder).getLastN3();
	               if (n3!=-1 && last!=n3) {
	                  if( tasks.createAllSky(false) ) context.preview(n3);
	                  //                  tasks.config.setLastN3(n3);
	                  last = n3;
	               }
	               break;
	               //			case JPG : 
	               //				value = (int)((SkyGenerator)thread).getProgress();
	               //				break;
	         }
	         tasks.setProgress(mode,value);
	         try {
	            Thread.sleep(200);
	         } catch (InterruptedException e) {
	         }
	      }
	      stopped = true;
	      value = builder.getProgress();
	      tasks.setProgress(mode,value);
	   }

	}
}
