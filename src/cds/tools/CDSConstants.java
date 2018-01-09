// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.tools;

import java.awt.Font;
import java.awt.Color;
import java.awt.Toolkit;

/**
 * CDS design and static values interface
 *
 * @author Andr‰ Schaaff [CDS]
 *
 * @version 1.0 beta :  (july 2002), xml format (Astrores or VOTable), glu and url values
 *                      (june 2002), STREAM and FRAME values
 *                      renamed to CDSConstants
 * @version 0.9 : (january 2002), extraction from AladinJava
 *
 *
 */

public interface CDSConstants {

    static final boolean LSCREEN= Toolkit.getDefaultToolkit().getScreenSize().width > 1000;

    /** fonts average size */
    static final int  SIZE   = LSCREEN?12:10;

    static String s = "Helvetica";
    static Font BOLD   = new Font(s,Font.BOLD,  SIZE);
    static Font PLAIN  = new Font(s,Font.PLAIN, SIZE);
    static Font ITALIC = new Font(s,Font.ITALIC,SIZE);
    static int SSIZE  = SIZE-2;
    static Font SBOLD  = new Font(s,Font.BOLD,  SSIZE);
    static Font SPLAIN = new Font(s,Font.PLAIN, SSIZE);
    static Font SITALIC= new Font(s,Font.ITALIC,SSIZE);
    static int LSIZE  = SIZE+2;
    static Font LPLAIN = new Font(s,Font.PLAIN, LSIZE);
    static Font LBOLD  = new Font(s,Font.BOLD,  LSIZE);
    static Font LITALIC= new Font(s,Font.ITALIC,LSIZE);
    static Font LLITALIC= new Font(s,Font.BOLD,18);
    static Font COURIER= new Font("Courier",Font.PLAIN,SIZE);
    static Font BCOURIER= new Font("Courier",Font.PLAIN+Font.BOLD,SIZE);

    static final int DEFAULT 	= 0;
    static final int WAIT 	= 1;
    static final int HAND 	= 2;
    static final int CROSSHAIR	= 3;
    static final int MOVE 	= 4;
    static final int RESIZE 	= 5;
    static final int TEXT 	= 6;

    // background color
    static final Color BKGD   = Color.lightGray;
    static final Color BKGDCOPYRIGHT   = Color.white;
    static Color LGRAY = new Color(229,229,229);

    static final String COPYRIGHT = "(c) ULP/CNRS 1999-2002 - Centre de Données astronomiques de Strasbourg";

    // Les textes associes aux differentes possibilites du menu
    static final int GETHEIGHT  = 17; // Cochonnerie de getHeight()

    // True if standalone mode
    static boolean STANDALONE = true;

    // units
    static final String DEGREE = "degree";
    static final String ARCMIN = "arcmin";
    static final String ARCSEC = "arcsec";

    // output mode, stream or frame
    static final int STREAM = 0;
    static final int FRAME = 1;

    // modal or non modal dialog
    static final boolean MODAL = true;
    static final boolean NONMODAL = false;

    // XML specific format
    static final int ASTRORES = 0;
    static final int VOTABLE = 1;

    // url or glu tag values
    static String VIZIERMETAGLU = "VizieR.Meta";
    static String VIZIERMETACATGLU = "VizieR.MetaCat";

    static String ASTROVIZIERMETA = "http://vizier.u-strasbg.fr/cgi-bin/asu-xml?-meta.aladin=all";
    static String ASTROVIZIERMETACAT = "http://vizier.u-strasbg.fr/cgi-bin/asu-xml?-meta&";

    static String VOVIZIERMETA = "http://vizier.u-strasbg.fr/viz-bin/nph-metaladin";
//    static String VOVIZIERMETA = "http://vizier.u-strasbg.fr/cgi-bin/votable?-meta.aladin=all";
    static String VOVIZIERMETACAT = "http://vizier.u-strasbg.fr/cgi-bin/votable?-meta&";

    // default VizieR query url or glu values
    static String VIZIERMETACAT = VOVIZIERMETACAT;
    static String VIZIERMETA = VOVIZIERMETA;

}
