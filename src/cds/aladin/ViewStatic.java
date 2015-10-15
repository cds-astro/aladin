package cds.aladin;


public class ViewStatic extends View {
   
   ViewSimpleStatic vs;
   
   ViewStatic(Aladin aladin) {
      super(aladin);
      modeView=1;
      currentView=0;
      viewSimple = new ViewSimple[1];
   }
   
   protected void setViewSimple(ViewSimple vs) { vs.view=this; viewSimple[0] = vs; }
}
