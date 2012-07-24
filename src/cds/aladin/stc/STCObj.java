package cds.aladin.stc;

public abstract class STCObj {

    public enum ShapeType {
        POLYGON
    }


    public abstract ShapeType getShapeType();
    
    public abstract boolean isIn(double lon,double lat);
}
