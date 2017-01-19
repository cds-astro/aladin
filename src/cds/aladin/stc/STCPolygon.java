package cds.aladin.stc;

import java.util.ArrayList;
import java.util.Collections;

public class STCPolygon extends STCObj {
    private STCFrame frame;
    private ArrayList<Double> xCorners = new ArrayList<Double>();
    private ArrayList<Double> yCorners = new ArrayList<Double>();


    public STCPolygon() {}

    public STCFrame getFrame() {
        return frame;
    }

    public void setFrame(STCFrame frame) {
        this.frame = frame;
    }

    public void addCorner(double x, double y) {
        xCorners.add(x);
        yCorners.add(y);
    }

    @Override
    public ShapeType getShapeType() {
        return STCObj.ShapeType.POLYGON;
    }

    public ArrayList<Double> getxCorners() {
        return xCorners;
    }

    public void setxCorners(ArrayList<Double> xCorners) {
        this.xCorners = xCorners;
    }

    public ArrayList<Double> getyCorners() {
        return yCorners;
    }

    public void setyCorners(ArrayList<Double> yCorners) {
        this.yCorners = yCorners;
    }
    
    public void reverseDrawDirection() {
		Collections.reverse(xCorners);
		Collections.reverse(yCorners);
	}
    
    @Override
    public boolean isIn(double lon,double lat) {
       return true;
    }


}
