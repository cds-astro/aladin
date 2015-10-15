package cds.aladin;

public class ZoomStatic extends Zoom {
   
   protected ZoomStatic(Aladin aladin) {
      this.aladin = aladin;
      zoomView = new ZoomView(aladin);
   }

}
