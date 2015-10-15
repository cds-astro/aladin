package cds.aladin;

import java.awt.Color;

public class ViewSimpleStatic extends ViewSimple {

   
   protected ViewSimpleStatic(Aladin aladin) {
      super(aladin);

      setBackground(Color.white);
      setOpaque(true);
      setDoubleBuffered(false);
   }
   
   protected void setViewParam(PlanBG p,String label,int width, int height, Coord c, double radius) {
      pref=p;
      pref.label=label;
      setDimension(width,height);
      p.projd.setProjCenter(c.al, c.del);
      p.setDefaultZoom( c, radius, width);
      setZoomXY(p.initZoom,-1,-1,true);
      System.out.println("c="+c+" zoom="+zoom+" radius="+Coord.getUnit(radius)+" rzoom="+rzoom);
   }
   
}
