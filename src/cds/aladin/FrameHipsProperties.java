// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
//
//    Aladin Desktop is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin Desktop is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin Desktop.
//

package cds.aladin;



/**
 * Classe dediee � l'affichage d'un fichier properties HiPS
 *
 * @author Pierre Fernique [CDS]
* @version 1.0 : nov 2015 - Creation
* @version 1.1 : sept 2017 - Ajout de la cr�ation par simple chaine
 */
public class FrameHipsProperties extends FrameHeaderFits {
    private PlanBG plan=null;
    private String prop=null;
   
    /**
     * Visualisation des propri�t�s HiPS � partir de son plan
     * @param plan
     * @throws Exception
     */
    protected FrameHipsProperties(PlanBG plan) throws Exception {
       this();
       this.plan = plan;
    }
    
    /**
     * Visualisation des propri�t�s HiPS directement pass�s en param�tre
     * @param prop
     * @throws Exception
     */
    protected FrameHipsProperties(String title,String prop) throws Exception {
       this();
       setTitle(title);
       this.prop = prop;
       seeHeaderFits();
    }
    
    private FrameHipsProperties() throws Exception {
       super(null,"Properties");
       Aladin.setIcon(this);
       makeTA(false);
    }
    
    protected String getOriginalHeaderFits() {
       if( prop!=null ) return prop;
       return plan.prop.getPropOriginal();
    }

}
