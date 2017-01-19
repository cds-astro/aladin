package cds.aladin.stc;

public abstract class STCObj {

    public enum ShapeType {
        POLYGON,
        CIRCLE
    }


    public abstract ShapeType getShapeType();
    
    public abstract boolean isIn(double lon,double lat);
}
