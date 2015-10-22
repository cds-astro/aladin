package cds.aladin;

import java.util.Hashtable;


public class PlanBGStatic extends PlanBG {
   
   PlanBGStatic(Aladin aladin, String label, String url) {
      super(aladin);
      
      this.url = url;
      this.label = label;
      
      dataMin=pixelMin=0;
      dataMax=pixelMax=255;
      isOldPlan=false;

      pixList = new Hashtable<String,HealpixKey>(1000);

      RGBControl = new int[RGBCONTROL.length];
      for( int i=0; i<RGBCONTROL.length; i++) RGBControl[i] = RGBCONTROL[i];
      creatDefaultCM();
      
      type = ALLSKYIMG;
      inFits = false;
      inJPEG = true;
      inPNG = false;
      color = true;
      frameOrigin = Localisation.ICRS;

      scanProperties();
      
//      int defaultProjType = Projection.getProjType(sProj);
      int defaultProjType = Calib.SIN;
      
      projd = new Projection("allsky",Projection.WCS,0,0,60*4,60*4,250,250,500,500,0,false, defaultProjType,Calib.FK5);
      projd.frame = getCurrentFrameDrawing();
      
      typeCM = aladin.configuration.getCMMap();
      transfertFct = aladin.configuration.getCMFct();
      video = aladin.configuration.getCMVideo();
      
   }
   
   protected void askForRepaint() { }
}
