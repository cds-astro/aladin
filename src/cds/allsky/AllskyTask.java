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

import static cds.allsky.AllskyConst.INDEX;
import static cds.allsky.AllskyConst.JPG;
import static cds.allsky.AllskyConst.TESS;
import cds.aladin.Aladin;
import cds.aladin.Plan;
import cds.tools.pixtools.HpixTree;

public class AllskyTask implements Runnable{

	
	AllskyPanel allsky;
	InitLocalAccess initializer = new InitLocalAccess();
	SkyGenerator sg = new SkyGenerator();
	DBBuilder builder = new DBBuilder();
    int mode = -1;
    boolean allskyOk;  // true si le allsky généré l'a bien été à la fin du processus de construction et pas en cours de ce processus
	
    public AllskyTask(AllskyPanel allskyPanel) {
       allsky = allskyPanel;
    }

	private volatile Thread runner = null;

	private int order;
	private String input;
	private String output;

	private int bitpix;
	private boolean keepBB = false;
	private HpixTree hpixTree = null;
	private int coaddMode = DescPanel.REPLACETILE;
//	private double[] cut;
//	private int fct; // fonction de transfert
	private ThreadProgressBar progressBar;

	private double blank;
	private double bzero;
	private double bscale;

//	private Plan planPreview;

	public Plan getPlanPreview() {
	   return allsky.aladin.calque.getPlan(allsky.pDesc.getLabel());
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
	         case INDEX : initializer.stop();
	         case TESS : builder.stop();
	      }
	      progressBar.stop();
	   }
	}
	
	public void run() {
	   allsky.setIsRunning(true);
	   try {
	      mode = INDEX;
	      //			if (allsky.toReset()) builder.reset(output);
	      boolean fast = allsky.toFast();
	      boolean fading = allsky.toFading();

	      // Créée un répertoire HpxFinder avec l'indexation des fichiers source pour l'ordre demandé
	      // (garde l'ancien s'il existe déjà)
	      if (mode<=INDEX) {
	         Aladin.trace(2,"Launch Index");
	         followProgress(mode,initializer);
	         boolean init = initializer.build(input,output,order);
	         // si le thread a été interrompu, on sort direct
	         if (runner != Thread.currentThread()) {
	            progressBar.stop();
	            return;
	         }
	         if (init) Aladin.trace(2,"Allsky... => Index built");
	         else Aladin.trace(2,"Allsky... => Use previous Index");
	         setProgress(mode,100);
	         progressBar.stop();
	         allsky.enableProgress(false,INDEX);
	      }
	      // Création des fichiers healpix fits et jpg
	      if (mode <= TESS) {
	         mode = TESS;
	         Aladin.trace(2,"Launch Tess ("+(fast?"fast":"best")+(!fast && !fading ? "-nofading":"")+" method)");
	         //				builder.setThread(runner);
	         followProgress(mode, builder);
	         try {
	            //					if (cut == null)
	            //						cut = new double[] {0,0,0,0};
	            //					builder.setAutoCut(cut, fct);
	            builder.setBlank(blank);
	            builder.setBScaleBZero(bscale,bzero);
	            builder.setHpixTree(hpixTree);
	            builder.setCoadd(coaddMode);
	            builder.build(order, output, bitpix, fast, fading, keepBB);
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
	         allsky.enableProgress(false,TESS);
	      }
	      // création du fichier allsky
	      if (mode <= JPG) {
	         mode = JPG;
	         createAllSky();
	      }
	      allskyOk=true;
	      allsky.setIsRunning(false);

	      allsky.done();
	      runner = null;
	      mode = -1;
	      Aladin.trace(2,"DONE");
	   } catch (Exception e) {
	      e.printStackTrace();
	   }
	}

	// création des fichiers allsky
	public void createAllSky() {
	   if( allskyOk ) return;      // déjà fait

	   //          followProgress(mode,sg);
	   //          String path = Util.concatDir(output,AllskyConst.SURVEY);
	   try {
	      if( bitpix==0 ) sg.createAllSkyJpgColor(output,3,64);
	      else {
	         double[] cut = allsky.getCut();
	         if( cut==null ) sg.createAllSky(output,3,64,0,0, keepBB);
	         else sg.createAllSky(output,3,64,cut[0],cut[1],keepBB);
	      }

	   } catch (Exception e) {
	      e.printStackTrace();
	   }
	}

	void setInitDir(String txt) {
	   allsky.setInitDir(txt);		
	}

	void setProgress(int stepMode, int i) {
	   allsky.setProgress(stepMode, i);
	}

	private void followProgress(int stepMode, Object o) {
	   progressBar = new ThreadProgressBar(stepMode,o, this);
	   progressBar.start();
	}

	public void doInBackground() throws Exception {
	   order = allsky.getOrder();
//	   System.out.println("doInBackGround => order="+order);
	   if (order==-1) order = 3;
	   output = allsky.getOutputPath();
	   input = allsky.getInputPath();

	   if (input == null || input.equals("")) {
	      System.err.println("No input directory given");
	      allsky.stop();
	      return ;
	   }
	   // récupère le bitpix dans le formulaire
	   bitpix = allsky.getBitpix();
	   keepBB = allsky.isKeepBB();
	   hpixTree = allsky.getHpixTree();
	   coaddMode = allsky.getCoAddMode();
	   double bb[] = allsky.getBScaleBZero();
	   bscale = bb[0];
	   bzero = bb[1];
	   blank = allsky.getBlank();
	   // si le bitpix change
	   if (allsky.getOriginalBitpix() != bitpix)
	      // on change aussi les bornes
	      allsky.convertCut(bitpix);
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
	   allsky.setLastN3(lastN3);
	}

	public int getLastN3() {
	   return allsky.getLastN3();
	}



}

class ThreadProgressBar implements Runnable {

   public static final int INDEX = AllskyConst.INDEX;
   public static final int TESS = AllskyConst.TESS;
   //	public static final int JPG = AllskyConst.JPG;

   //	private volatile Thread th_progress = null;
   private boolean stopped = false;
   int last = -1;
   Object thread;
   AllskyTask tasks;

   int mode ;
   public ThreadProgressBar(int stepMode, Object source, AllskyTask allskyTask) {
      mode=stepMode;
      thread = source;
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
      while(thread != null && !stopped) {// && value < 99) {
         switch (mode) {
            case INDEX :
               value = (int)((InitLocalAccess)thread).getProgress();
               txt = ((InitLocalAccess)thread).getCurrentpath();
               tasks.setInitDir(txt);
               break;
            case TESS : 
               value = (int)((DBBuilder)thread).getProgress();
               int n3 = ((DBBuilder)thread).getLastN3();
               if (n3!=-1 && last!=n3) {
                  tasks.createAllSky();
                  tasks.allsky.preview(n3);
                  tasks.setLastN3(n3);
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
            value = (int)((InitLocalAccess)thread).getProgress();
            break;
         case TESS : 
            value = (int)((DBBuilder)thread).getProgress();
      }
      tasks.setProgress(mode,value);
   }

}
