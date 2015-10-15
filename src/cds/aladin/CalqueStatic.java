package cds.aladin;

public class CalqueStatic extends Calque {
   
   private ViewStatic view;
   
   protected CalqueStatic(Aladin aladin,ViewStatic view) {
      this.aladin = aladin;
      this.view = view;
      zoom = new ZoomStatic(aladin);
      overlayFlag=0xFFFF & ~Calque.HPXGRID;
   }
   
   public Plan getPlanBase() { return view.getCurrentView().pref; }

}
