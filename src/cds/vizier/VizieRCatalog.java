// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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
 * Created on 4 Sep 2007
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.vizier;


/** 
 * Class representing a VizieR catalogue
 * 
 * @author Thomas Boch
 * @version 1.0 September 2007
 */
public class VizieRCatalog {

    private String name;
    private String desc;
    private String category; // eg : Radio, Optical
    private int density = -1;
    private int nbKRow;      // number of KRows in the catalogue

    public VizieRCatalog(String name, String desc, String category, int density, int nbKRow) {
        this.name = name;
        this.desc = desc;
        this.category = category;
        this.density = density;
        this.nbKRow = nbKRow;
    }


    public String getCategory() {
        return category==null?"":category;
    }

    public int getDensity() {
        return density;
    }

    public String getDesc() {
        return desc==null?"":desc;
    }

    public String getName() {
        return name==null?"":name;
    }

    public int getNbKRow() {
        return nbKRow;
    }

}
