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

package cds.aladin;

/**
 *
 * @author Thomas Boch [CDS]
 * @version 0.9 (Mai 2010)
 *
 */
public class PlanHealpixAlgo extends PlanHealpix {

    private PlanHealpix p1; // Le plan Healpix première opérande (ou null)
    private PlanHealpix p2; // Le plan Image deuxième opérande (ou null)
    private int fct; // la fonction +,-,*,/ (0,1,2 ou 3)
    private double coef; // Le coefficent ou NaN si inutilisé

    public PlanHealpixAlgo(Aladin aladin,String label,
                           PlanHealpix p1,PlanHealpix p2,int fct,double coef) {
        super(aladin);

        this.p1 = p1;
        this.p2 = p2;
        this.fct = fct;
        this.coef = coef;


        doInit();
    }

    private void doInit() {
//        this.idxTFormToRead = idxTFormToRead;

        video = aladin.configuration.getCMVideo();
        flagOk = false;
        isOldPlan = false;
        type = ALLSKYIMG;
        frameOrigin = Localisation.GAL;
//        this.filename = file;
//        cacheID = survey = file;
//        this.originalPath = file;

//        int i = file.lastIndexOf(Util.FS);
//        if (i > 0)
//            survey = survey.substring(i + 1);
//        setLabel(label==null ? survey : label);
//
//        this.dirName = getDirname() + Util.FS + dirNameForIdx(idxTFormToRead);
//
//        this.survey = this.dirName;
    }

}
