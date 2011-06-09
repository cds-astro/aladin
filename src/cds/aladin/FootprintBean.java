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

/*
 * Created on 02-Sep-2005
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.util.Hashtable;
import java.util.Vector;

/** Objet représentant un footprint instrumental rattaché à une position
 * @author Thomas Boch [CDS]
 */
public class FootprintBean {

	static private Hashtable hash;

	static {
		hash = new Hashtable();
	}

//	private double[][] raOffset;
//	private double[][] decOffset;


	private double posAngle=0.0;
	private String ra;
	private String de;
	private String raRot;
	private String deRot;

    private boolean raIsSet = false;
    private boolean decIsSet = false;
    private boolean raRotIsSet = false;
    private boolean decRotIsSet = false;

	private boolean movable = false;
	private boolean rollable = false;

    private String instrumentName;
    private String telescopeName;
    private String instrumentDesc;

    private boolean displayInFovList = true;

    // origine de la description du FoV (==auteur)
    private String origin;

	// ensemble des sous-footprints beans composant le footprint
	Vector<SubFootprintBean> subFootprints;

    FootprintBean() {
		subFootprints = new Vector<SubFootprintBean>();
	}

	protected void addSubFootprintBean(SubFootprintBean sFpBean) {
		subFootprints.add(sFpBean);
	}

	   public boolean isDisplayInFovList() {
	        return displayInFovList;
	    }

	    public void setDisplayInFovList(boolean displayInFovList) {
	        this.displayInFovList = displayInFovList;
	    }

	/** Retourne l'ensemble des beans représentant les sous-parties des FoV !
	 *
	 * SubFootPrintBean[]
	 */
	protected SubFootprintBean[] getBeans() {
		SubFootprintBean[] beans = new SubFootprintBean[subFootprints.size()];
		subFootprints.copyInto(beans);


		return beans;

	}




	static void registerNewTemplate(String id, FootprintBean footprint) {
		if( hash.get(id)!=null ) return;
		hash.put(id, footprint);
	}

	static FootprintBean getTemplate(String id) {
		return (FootprintBean)hash.get(id);
	}





	/**
	 * @return Returns the decOffset array
	 */
//	protected double[][] getDecOffset() {
//		return decOffset;
//	}
	/**
	 * @return Returns the raOffset array
	 */
//	protected double[][] getRaOffset() {
//		return raOffset;
//	}
	/**
	 * @return Returns the movable.
	 */
	protected boolean isMovable() {
		return movable;
	}
	/**
	 * @param movable The movable to set.
	 */
	protected void setMovable(boolean movable) {
		this.movable = movable;
	}
/**
 * @return Returns the posAngle.
 */
protected double getPosAngle() {
	return posAngle;
}
/**
 * @param posAngle The posAngle to set.
 */
protected void setPosAngle(double posAngle) {
	this.posAngle = posAngle;
}
	/**
	 * @return Returns the rollable.
	 */
	protected boolean isRollable() {
		return rollable;
	}
	/**
	 * @param rollable The rollable to set.
	 */
	protected void setRollable(boolean rollable) {
		this.rollable = rollable;
	}

    /**
     * @param ra The ra to set.
     */
    protected void setRa(String ra) {
        this.ra = ra;
        raIsSet = true;
    }

	/**
	 * @param de The de to set.
	 */
	protected void setDe(String de) {
		this.de = de;
		decIsSet = true;
	}

	// PF Jan 09
    protected void setRaRot(String ra) {
        this.raRot = ra;
        raRotIsSet = true;
    }

 // PF Jan 09
    protected void setDeRot(String de) {
        this.deRot = de;
        decRotIsSet = true;
    }

	protected double getRa() throws Exception {
		Coord c = new Coord(ra+" "+de);
		return c.al;
	}

	protected double getDe() throws Exception {
		Coord c = new Coord(ra+" "+de);
		return c.del;
	}

	// PF Jan 09
    protected double getRaRot() throws Exception {
       Coord c = new Coord(raRot+" "+deRot);
       return c.al;
   }

    // PF Jan 09
   protected double getDeRot() throws Exception {
       Coord c = new Coord(raRot+" "+deRot);
       return c.del;
   }

   // PF Jan 09
   protected boolean coordsAreSet() {
      return raIsSet && decIsSet;
   }

   // PF Jan 09
   protected boolean rotAreSet() {
      return raRotIsSet && decRotIsSet;
   }

    protected String getInstrumentDesc() {
        return instrumentDesc==null?"":instrumentDesc;
    }

    protected void setInstrumentDesc(String instrumentDesc) {
        this.instrumentDesc = instrumentDesc;
    }

    protected String getInstrumentName() {
        return instrumentName==null?"":instrumentName;
    }

    protected void setInstrumentName(String instrumentName) {
        this.instrumentName = instrumentName;
    }

    protected String getTelescopeName() {
        return telescopeName==null?"":telescopeName;
    }

    protected void setTelescopeName(String telescopeName) {
        this.telescopeName = telescopeName;
    }

    public String getOrigin() {
        return origin==null?"":origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }
}
