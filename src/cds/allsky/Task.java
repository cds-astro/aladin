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
import cds.aladin.Plan;
import cds.tools.pixtools.HpixTree;

public class Task implements Runnable{

	
	MainPanel mainPanel;
	BuilderIndex builderIndex;
	BuilderAllsky builderAllsky;
	BuilderController builder;
    int mode = -1;
    boolean allskyOk;  // true si le allsky généré l'a bien été à la fin du processus de construction et pas en cours de ce processus
	
	private volatile Thread runner = null;

	private int order;
	private String input;
	private String output;

	private int bitpix;
	private boolean keepBB = false;
	private HpixTree hpixTree = null;
	private int coaddMode = TabDesc.REPLACETILE;
//	private double[] cut;
//	private int fct; // fonction de transfert
	private ThreadProgressBar progressBar;

	private double blank;
	private int [] borderSize;
	private double bzero;
	private double bscale;

//	private Plan planPreview;
	
    public Task(MainPanel mainPanel) {
       this.mainPanel = mainPanel;
       builderIndex = new BuilderIndex(mainPanel);
       builderAllsky = new BuilderAllsky(mainPanel);
       builder = new BuilderController(mainPanel);
    }


	public Plan getPlanPreview() {
	   return mainPanel.aladin.calque.getPlan(mainPanel.tabDesc.getLabel());
	}

	public synchronized void startThread(){
	   if(runner == null){
	      runner = new Thread(this);
	      try {
	         runner.start(); // --> appelle run()
	      } catch (Exception e) {
	         // TODO Auto-generated catch block
	         e.printStackTrace();
	      }
	   }
	}

	public synchronized void stopThread(){

	   if(runner != null){
	      runner = null;
	      Aladin.trace(2,"STOP ON "+mode);
	      switch (mode) {
	         case INDEX : builderIndex.stop();
	         case TESS : builder.stop();
	      }
	      progressBar.stop();
	   }
	}
	
	public void run() {
	   mainPanel.setIsRunning(true);
	   try {
	      mode = INDEX;
	      //			if (allsky.toReset()) builder.reset(output);
	      boolean fading = mainPanel.toFading();

	      // Créée un répertoire HpxFinder avec l'indexation des fichiers source pour l'ordre demandé
	      // (garde l'ancien s'il existe déjà)
	      if (mode<=INDEX) {
	         Aladin.trace(2,"Launch Index (frame="+mainPanel.getFrameName()+")");
	         followProgress(mode,builderIndex);
	         boolean init = builderIndex.build(input,output,order);
	         // si le thread a été interrompu, on sort direct
	         if (runner != Thread.currentThread()) {
	            progressBar.stop();
	            return;
	         }
	         if (init) Aladin.trace(2,"Allsky... => Index built");
	         else Aladin.trace(2,"Allsky... => Use previous Index");
	         setProgress(mode,100);
	         progressBar.stop();
	         mainPanel.enableProgress(false,INDEX);
	      }
	      // Création des fichiers healpix fits et jpg
	      if (mode <= TESS) {
	         mode = TESS;
	         Aladin.trace(2,"Launch Tess (frame="+mainPanel.getFrameName()+") "+(!fading ? "(nofading)":""));
	         //				builder.setThread(runner);
	         followProgress(mode, builder);
	         try {
	            //					if (cut == null)
	            //						cut = new double[] {0,0,0,0};
	            //					builder.setAutoCut(cut, fct);
	            builder.setBlank(blank);
	            builder.setBorderSize(borderSize);
	            builder.setBScaleBZero(bscale,bzero);
	            builder.setHpixTree(hpixTree);
	            builder.setCoadd(coaddMode);
	            builder.build(order, output, bitpix, fading, keepBB);
	            // si le thread a été interrompu, on sort direct
	            if (runner != Thread.currentThread()) {
	               progressBar.stop();
	               return;
	            }
	         } catch (Exception e) {
	            e.printStackTrace();
	         }
	         Aladin.trace(2,"Allsky... => Hpx files built");
	         setProgress(mode,100);
	         progressBar.stop();
	         mainPanel.enableProgress(false,TESS);
	      }
	      // création du fichier allsky
	      if (mode <= JPG) mode = JPG;
	      createAllSky();
	      allskyOk=true;
	      mainPanel.setIsRunning(false);

	      mainPanel.done();
	      runner = null;
	      mode = -1;
	      Aladin.trace(2,"Allsky... done!");
	   } catch (Exception e) {
	      e.printStackTrace();
	   }
	}
	
	private long lastCreatedAllSky=-1L;

	// création des fichiers allsky
    public boolean  createAllSky() { return createAllSky(true); }
    public boolean  createAllSky(boolean force) {
//	   if( allskyOk ) return;      // déjà fait
	   
	   if( !force ) {
	      long now = System.currentTimeMillis();
	      if( now-lastCreatedAllSky<15000 ) return false;  // déjà fait il n'y a pas bien longtemps
	      lastCreatedAllSky = now;
	   }

	   //          followProgress(mode,sg);
	   //          String path = Util.concatDir(output,AllskyConst.SURVEY);
	   try {
	      if( bitpix==0 ) builderAllsky.createAllSkyJpgColor(output,3,64);
	      else {
	         double[] cut = mainPanel.getCut();
	         if( cut==null ) builderAllsky.createAllSky(output,3,64,0,0, keepBB);
	         else builderAllsky.createAllSky(output,3,64,cut[0],cut[1],keepBB);
	      }

	   } catch (Exception e) {
	      e.printStackTrace();
	   }
	   return true;
	}

	void setInitDir(String txt) {
	   mainPanel.setInitDir(txt);		
	}

	void setProgress(int stepMode, int i) {
	   mainPanel.setProgress(stepMode, i);
	}

	private void followProgress(int stepMode, Object o) {
	   progressBar = new ThreadProgressBar(stepMode,o, this);
	   progressBar.start();
	}

	public void doInBackground() throws Exception {
	   order = mainPanel.getOrder();
//	   System.out.println("doInBackGround => order="+order);
	   if (order==-1) order = 3;
	   output = mainPanel.getOutputPath();
	   input = mainPanel.getInputPath();

	   if (input == null || input.equals("")) {
	      System.err.println("No input directory given");
	      mainPanel.stop();
	      return ;
	   }
	   // récupère le bitpix dans le formulaire
	   bitpix = mainPanel.getBitpix();
	   keepBB = mainPanel.isKeepBB();
	   hpixTree = mainPanel.getHpixTree();
	   coaddMode = mainPanel.getCoAddMode();
	   double bb[] = mainPanel.getBScaleBZero();
	   bscale = bb[0];
	   bzero = bb[1];
	   blank = mainPanel.getBlank();
	   borderSize = mainPanel.getBorderSize();
	   // si le bitpix change
	   if (mainPanel.getOriginalBitpix() != bitpix)
	      // on change aussi les bornes
	      mainPanel.convertCut(bitpix);
	   //		cut = allsky.getCut();
	   //		fct = allsky.getMethod();

	   if (mode == -1)
	      mode = INDEX;

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

	public void setLastN3(int lastN3) {
	   mainPanel.setLastN3(lastN3);
	}
}

class ThreadProgressBar implements Runnable {

   public static final int INDEX = Constante.INDEX;
   public static final int TESS = Constante.TESS;
   //	public static final int JPG = AllskyConst.JPG;

   //	private volatile Thread th_progress = null;
   private boolean stopped = false;
   int last = -1;
   Object builder;
   Task tasks;

   int mode ;
   public ThreadProgressBar(int stepMode, Object source, Task allskyTask) {
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
      //		th_progress = null;
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
               value = (int)((BuilderIndex)builder).getProgress();
               txt = ((BuilderIndex)builder).getCurrentpath();
               tasks.setInitDir(txt);
               break;
            case TESS : 
               value = (int)((BuilderController)builder).getProgress();
               int n3 = ((BuilderController)builder).getLastN3();
               if (n3!=-1 && last!=n3) {
                  if( tasks.createAllSky(false) ) tasks.mainPanel.preview(n3);
                  tasks.mainPanel.setLastN3(n3);
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
      switch (mode) {
         case INDEX :
            value = (int)((BuilderIndex)builder).getProgress();
            break;
         case TESS : 
            value = (int)((BuilderController)builder).getProgress();
      }
      tasks.setProgress(mode,value);
   }

}
