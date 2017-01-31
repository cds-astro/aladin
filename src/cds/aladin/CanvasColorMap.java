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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JPanel;

import cds.tools.Util;


/**
 * Gestion du graphique de la dynamique d'une image
 *
 * @author Pierre Fernique [CDS]
 * @author Anais Oberto [CDS]
 * @version 1.8 : sept 2010 - Correction bug du changement de couleur sur les fonctions de transfert
 * @version 1.7 : juin 2006 - Fonctions de transfert gérées dans la colormap
 * @version 1.6 : avril 2006 Possibilité d'ajouter sa propre color map (Thomas)
 * @version 1.5 : décembre 2004 Incorporation de la palette Stern de Robin Wetzel
 * @version 1.4 : (26 sept 2003) Prise en compte des vraies valeurs pixels
 * @version 1.3 : (15 mars 2002) petite modif pour l'histogramme Chandra
 * @version 1.2 : (dec 2001) incorporation image RGB
 * @version 1.1 : (15 juin 2000) Recuperation memoire libre
 * @version 1.0 : (5 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class CanvasColorMap extends JPanel  implements
MouseMotionListener, MouseListener, KeyListener
{

   // Les constantes representants les 3 triangles de controles
   static final private   int MIN = 0;      // Seuil minimal
   static final private   int MIL = 1;      // Seuil moyen
   static final private   int MAX = 2;      // Seuil maximal
   static final private   int NO =  3;      // Aucun seuil

   // Demi largeur de la palette BAND
   static private final int SIZEBAND = 16;

   // Les constantes d'affichage
   static final private   int mX = 5;      // Marge en abscisse
   static final private   int mY = 0  ;     // Marge en ordonnee
   static final protected int Hp = 120;     // Hauteur de l'historgramme
   static final private   int cmH = 25;     // Hauteur de la colormap
   static final private   int cmY = Hp+10+cmH;  // Ordonnée de la colormap
   static final private   int W = 256+mX+10; // Largeur totale du graphique
   static final private   int H = Hp+cmH+mY+35; // Hauteur totale du graphique

   // Ordonnees (cas normal ou cas Inverse) de la courbe de
   // reponse des gris
   static final private   int Y[]  = {mY+Hp,mY+Hp,mY+Hp/2,mY+1, mY+1 };
   static final private   int Yr[] = {mY+1, mY+1, mY+Hp/2,mY+Hp,mY+Hp};

   // les references
   protected PlanImage pimg;                // le plan image concerne
   private boolean isPlanImageRGB=false;
   private boolean isPlanBGRGB=false;

   // Les valeurs a memoriser
   private int currentTriangle=NO;        // Le triangle en cours de selection
   protected int [] triangle = new int[3];// Positions courantes des 3 triangles
   private Image imgHist=null;            // Image contenant l'histogramme
   private Graphics gHist=null;
   private int greyLevel=-1;              // la valeur du niveau de gris sous la souris
   private boolean isDragging=false;      // true si on est en train de faire un dragging des triangles
   private boolean flagLosange = false;   // true si MAj est appuyé pour dessiner un losange à la palce du curseur du milieu

   static private Vector customCM;        // mémorisation des color maps personnalisées
   static protected Vector customCMName;  // mémorisation des noms des color maps personnalisés

   int rgb = -1;      // couleur, utilise pour le PlanImageRGB
   // 0: rouge, 1: vert, 2: bleu

   // tableau des composantes pour Stern
   static final private int[] SR={0,18,36,54,72,90,108,127,145,163,199,217,235,
      254,249,244,239,234,229,223,218,213,208,203,197,192,187,182,177,172,
      161,156,151,146,140,135,130,125,120,115,109,104,99,94,89,83,78,73,68,
      63,52,47,42,37,32,26,21,16,11,6,64,65,66,67,68,69,70,71,72,73,75,76,
      77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,96,97,98,99,100,
      101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,117,118,
      119,120,121,122,123,124,125,126,128,129,130,131,132,133,134,135,136,
      137,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,
      155,156,157,158,160,161,162,163,164,165,166,167,168,169,170,171,172,
      173,174,175,176,177,178,179,181,182,183,184,185,186,187,188,189,190,
      192,193,194,195,196,197,198,199,200,201,203,204,205,206,207,208,209,
      210,211,212,213,214,215,216,217,218,219,220,221,222,224,225,226,227,
      228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,245,
      246,247,248,249,250,251,252,253,254};

   static final private int[] SG={0,1,2,3,4,5,6,7,8,9,11,12,13,14,15,16,17,18,
      19,20,21,22,23,24,25,26,27,28,29,30,32,33,34,35,36,37,38,39,40,41,42,
      43,44,45,46,47,48,49,50,51,53,54,55,56,57,58,59,60,61,62,64,65,66,67,
      68,69,70,71,72,73,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,
      92,93,94,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,
      112,113,114,115,117,118,119,120,121,122,123,124,125,126,128,129,130,
      131,132,133,134,135,136,137,139,140,141,142,143,144,145,146,147,148,
      149,150,151,152,153,154,155,156,157,158,160,161,162,163,164,165,166,
      167,168,169,170,171,172,173,174,175,176,177,178,179,181,182,183,184,
      185,186,187,188,189,190,192,193,194,195,196,197,198,199,200,201,203,
      204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,
      221,222,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,
      239,240,241,242,243,245,246,247,248,249,250,251,252,253,254};

   static final private int[] SB={0,1,3,5,7,9,11,13,15,17,21,23,25,27,29,31,33,
      35,37,39,41,43,45,47,49,51,53,55,57,59,63,65,67,69,71,73,75,77,79,81,
      83,85,87,89,91,93,95,97,99,101,105,107,109,111,113,115,117,119,121,
      123,127,129,131,133,135,137,139,141,143,145,149,151,153,155,157,159,
      161,163,165,167,169,171,173,175,177,179,181,183,185,187,191,193,195,
      197,199,201,203,205,207,209,211,213,215,217,219,221,223,225,227,229,
      233,235,237,239,241,243,245,247,249,251,255,251,247,243,238,234,230,
      226,221,217,209,204,200,196,192,187,183,179,175,170,166,162,158,153,
      149,145,141,136,132,128,119,115,111,107,102,98,94,90,85,81,77,73,68,
      64,60,56,51,47,43,39,30,26,22,17,13,9,5,0,3,7,15,19,22,26,30,34,38,41,
      45,49,57,60,64,68,72,76,79,83,87,91,95,98,102,106,110,114,117,121,125,
      129,137,140,144,148,152,156,159,163,167,171,175,178,182,186,190,194,
      197,201,205,209,216,220,224,228,232,235,239,243,247,251};

   // composantes de la table rainbow (IDL color table 13)
   static protected final int[] RAINBOW_R = {0,4,9,13,18,22,27,31,36,40,45,50,54,
      58,61,64,68,69,72,74,77,79,80,82,83,85,84,86,87,88,86,87,87,87,85,84,84,
      84,83,79,78,77,76,71,70,68,66,60,58,55,53,46,43,40,36,33,25,21,16,12,4,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,4,8,12,21,25,29,33,42,
      46,51,55,63,67,72,76,80,89,93,97,101,110,114,119,123,131,135,140,144,153,
      157,161,165,169,178,182,187,191,199,203,208,212,221,225,229,233,242,246,
      250,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255};

   static protected final int[] RAINBOW_G = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,4,8,16,21,25,29,38,42,46,51,55,63,67,72,76,84,89,93,97,
      106,110,114,119,127,131,135,140,144,152,157,161,165,174,178,182,187,195,
      199,203,208,216,220,225,229,233,242,246,250,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,250,242,238,233,229,221,216,212,208,199,195,191,187,178,174,170,165,
      161,153,148,144,140,131,127,123,119,110,106,102,97,89,85,80,76,72,63,59,
      55,51,42,38,34,29,21,17,12,8,0};

   static protected final int[] RAINBOW_B = {0,3,7,10,14,19,23,28,32,38,43,48,53,
      59,63,68,72,77,81,86,91,95,100,104,109,113,118,122,127,132,136,141,145,
      150,154,159,163,168,173,177,182,186,191,195,200,204,209,214,218,223,227,
      232,236,241,245,250,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,246,242,238,233,225,220,216,212,203,199,195,191,
      187,178,174,170,165,157,152,148,144,135,131,127,123,114,110,106,102,97,
      89,84,80,76,67,63,59,55,46,42,38,34,25,21,16,12,8,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

   // composantes de la table EOSB (IDL color table 27)
   static private final int[] EOSB_R = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,9,18,27,36,45,49,57,72,81,91,100,109,118,127,
      136,131,139,163,173,182,191,200,209,218,227,213,221,255,255,255,255,255,
      255,255,255,229,229,255,255,255,255,255,255,255,255,229,229,255,255,255,
      255,255,255,255,255,229,229,255,255,255,255,255,255,255,255,229,229,255,
      255,255,255,255,255,255,255,229,229,255,255,255,255,255,255,255,255,229,
      229,255,255,255,255,255,255,255,255,229,229,255,255,255,255,255,255,255,
      255,229,229,255,255,255,255,255,255,255,255,229,229,255,253,251,249,247,
      245,243,241,215,214,235,234,232,230,228,226,224,222,198,196,216,215,213,
      211,209,207,205,203,181,179,197,196,194,192,190,188,186,184,164,162,178,
      176,175,173,171,169,167,165,147,145,159,157,156,154,152,150,148,146,130,
      128,140,138,137,135,133,131,129,127,113,111,121,119,117,117};

   static private final int[] EOSB_G = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,15,23,31,39,47,55,57,64,79,87,95,
      103,111,119,127,135,129,136,159,167,175,183,191,199,207,215,200,207,239,
      247,255,255,255,255,255,255,229,229,255,255,255,255,255,255,255,255,229,
      229,255,255,255,255,255,255,255,255,229,229,255,250,246,242,238,233,229,
      225,198,195,212,208,204,199,195,191,187,182,160,156,169,165,161,157,153,
      148,144,140,122,118,127,125,123,121,119,116,114,112,99,97,106,104,102,
      99,97,95,93,91,80,78,84,82,80,78,76,74,72,70,61,59,63,61,59,57,55,53,50,
      48,42,40,42,40,38,36,33,31,29,27,22,21,21,19,16,14,12,13,8,6,3,1,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

   static private final int[] EOSB_B = {116,121,127,131,136,140,144,148,153,
      157,145,149,170,174,178,182,187,191,195,199,183,187,212,216,221,225,229,
      233,238,242,221,225,255,247,239,231,223,215,207,199,172,164,175,167,159,
      151,143,135,127,119,100,93,95,87,79,71,63,55,47,39,28,21,15,7,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0};

   // composantes de la table 'Fire' (ImageJ)
   static private final int[] FIRE_R = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1,4,7,
      10,13,16,19,22,25,28,31,34,37,40,43,46,49,52,55,58,61,64,67,70,73,76,79,
      82,85,88,91,94,98,101,104,107,110,113,116,119,122,125,128,131,134,137,
      140,143,146,148,150,152,154,156,158,160,162,163,164,166,167,168,170,171,
      173,174,175,177,178,179,181,182,184,185,186,188,189,190,192,193,195,196,
      198,199,201,202,204,205,207,208,209,210,212,213,214,215,217,218,220,221,
      223,224,226,227,229,230,231,233,234,235,237,238,240,241,243,244,246,247,
      249,250,252,252,252,253,253,253,254,254,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255};

   static private final int[] FIRE_G = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,1,3,5,7,8,10,12,14,16,19,21,24,27,29,32,35,37,40,43,46,48,
      51,54,57,59,62,65,68,70,73,76,79,81,84,87,90,92,95,98,101,103,105,107,
      109,111,113,115,117,119,121,123,125,127,129,131,133,134,136,138,140,141,
      143,145,147,148,150,152,154,155,157,159,161,162,164,166,168,169,171,173,
      175,176,178,180,182,184,186,188,190,191,193,195,197,199,201,203,205,206,
      208,210,212,213,215,217,219,220,222,224,226,228,230,232,234,235,237,239,
      241,242,244,246,248,248,249,250,251,252,253,254,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
      255,255,255,255,255,255,255,255};

   static private final int[] FIRE_B = {0,7,15,22,30,38,45,53,61,65,69,74,78,
      82,87,91,96,100,104,108,113,117,121,125,130,134,138,143,147,151,156,160,
      165,168,171,175,178,181,185,188,192,195,199,202,206,209,213,216,220,220,
      221,222,223,224,225,226,227,224,222,220,218,216,214,212,210,206,202,199,
      195,191,188,184,181,177,173,169,166,162,158,154,151,147,143,140,136,132,
      129,125,122,118,114,111,107,103,100,96,93,89,85,82,78,74,71,67,64,60,56,
      53,49,45,42,38,35,31,27,23,20,16,12,8,5,4,3,3,2,1,1,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,
      4,8,13,17,21,26,30,35,42,50,58,66,74,82,90,98,105,113,121,129,136,144,
      152,160,167,175,183,191,199,207,215,223,227,231,235,239,243,247,251,255,
      255,255,255,255,255,255,255};

   // composantes de la table 'Cube Helix' (cf. http://arxiv.org/abs/1108.5083 )
   static private final int[] CUBEHELIX_R = {0,1,3,4,6,8,9,10,12,13,14,15,17,18,
      19,20,20,21,22,23,23,24,24,25,25,25,26,26,26,26,26,26,26,26,26,26,26,25,
      25,25,25,24,24,24,23,23,23,23,22,22,22,21,21,21,21,21,21,20,20,20,21,21,
      21,21,21,22,22,22,23,23,24,25,26,27,27,28,30,31,32,33,35,36,38,39,41,43,
      45,47,49,51,53,55,57,60,62,65,67,70,72,75,78,81,83,86,89,92,95,98,101,104,
      107,110,113,116,120,123,126,129,132,135,138,141,144,147,150,153,155,158,
      161,164,166,169,171,174,176,178,181,183,185,187,189,191,193,194,196,198,
      199,201,202,203,204,205,206,207,208,209,209,210,211,211,211,212,212,212,
      212,212,212,212,212,211,211,211,210,210,210,209,208,208,207,207,206,205,
      205,204,203,203,202,201,201,200,199,199,198,197,197,196,196,195,195,194,
      194,194,193,193,193,193,193,193,193,193,193,193,194,194,195,195,196,196,
      197,198,199,200,200,202,203,204,205,206,208,209,210,212,213,215,217,218,
      220,222,223,225,227,229,231,232,234,236,238,240,242,244,245,247,249,251,
      253,255};

   static private final int[] CUBEHELIX_G = {0,0,1,1,2,2,3,4,4,5,6,6,7,8,9,10,
      11,11,12,13,14,15,17,18,19,20,21,22,24,25,26,28,29,31,32,34,35,37,38,40,
      41,43,45,46,48,50,52,53,55,57,58,60,62,64,66,67,69,71,73,74,76,78,79,81,
      83,84,86,88,89,91,92,94,95,97,98,99,101,102,103,104,106,107,108,109,110,
      111,112,113,114,114,115,116,116,117,118,118,119,119,120,120,120,121,121,
      121,121,122,122,122,122,122,122,122,122,122,122,122,122,122,122,122,121,
      121,121,121,121,121,121,121,121,120,120,120,120,120,120,120,120,120,120,
      121,121,121,121,121,122,122,122,123,123,124,124,125,125,126,127,127,128,
      129,130,131,131,132,133,135,136,137,138,139,140,142,143,144,146,147,149,
      150,152,154,155,157,158,160,162,164,165,167,169,171,172,174,176,178,180,
      182,183,185,187,189,191,193,194,196,198,200,202,203,205,207,208,210,212,
      213,215,216,218,219,221,222,224,225,226,228,229,230,231,232,233,235,236,
      237,238,239,240,240,241,242,243,244,244,245,246,247,247,248,248,249,250,
      250,251,251,252,252,253,253,254,255};

   static private final int[] CUBEHELIX_B = {0,1,3,4,6,8,9,11,13,15,17,19,21,23,
      25,27,29,31,33,35,37,39,41,43,45,47,48,50,52,54,56,57,59,60,62,63,65,66,
      67,69,70,71,72,73,74,74,75,76,76,77,77,77,78,78,78,78,78,78,78,77,77,77,
      76,76,75,75,74,73,73,72,71,70,69,68,67,66,66,65,64,63,61,60,59,58,58,57,
      56,55,54,53,52,51,51,50,49,49,48,48,47,47,47,46,46,46,46,46,47,47,47,48,
      48,49,50,50,51,52,53,55,56,57,59,60,62,64,65,67,69,71,74,76,78,81,83,86,
      88,91,94,96,99,102,105,108,111,114,117,120,124,127,130,133,136,140,143,
      146,149,153,156,159,162,165,169,172,175,178,181,184,186,189,192,195,197,
      200,203,205,207,210,212,214,216,218,220,222,224,226,227,229,230,231,233,
      234,235,236,237,238,239,239,240,241,241,242,242,242,243,243,243,243,243,
      243,243,243,243,243,242,242,242,242,241,241,241,241,240,240,240,239,239,
      239,239,239,238,238,238,238,238,238,238,238,239,239,239,240,240,240,241,
      242,242,243,244,245,246,247,248,249,250,252,253,255};



   // initialisation statique : création des color maps supplémentaires
   static {
      addCustomCM(new MyColorMap("rainbow", RAINBOW_R, RAINBOW_G, RAINBOW_B));
      addCustomCM(new MyColorMap("eosb", EOSB_R, EOSB_G, EOSB_B));
      addCustomCM(new MyColorMap("fire", FIRE_R, FIRE_G, FIRE_B));
      addCustomCM(new MyColorMap("cubehlx", CUBEHELIX_R, CUBEHELIX_G, CUBEHELIX_B));
      // création d'une color map  pour la polarisation
      int[] r, g, b;
      r = new int[256];
      g = new int[256];
      b = new int[256];
      for (int i = 0; i < 256; i++) {
         // TODO : je ne comprends plus pourquoi je devrais boucler, alors qu'on ne se trouve qu'entre -90 et 90

         // le +0.5 permet de commencer avec du bleu plutot que du rouge
         float hue = (1f*i/256f+0.5f)%1f;
         //	      hue = hue>1 ? hue-1 : hue;
         Color color = Color.getHSBColor(hue, 1f, 1f);
         r[i] = color.getRed();
         g[i] = color.getGreen();
         b[i] = color.getBlue();
      }
      addCustomCM(new MyColorMap("polar", r, g, b));
   }

   private Color BKGD = null;

   private FrameColorMap frameColorMap;

   /** Creation du graphique de la table de reponse des gris
    * @param pimg le Planimage associe
    */
   protected CanvasColorMap(FrameColorMap parent,PlanImage pimg) { this(parent,pimg,-1); }
   protected CanvasColorMap(FrameColorMap parent,PlanImage pimg,  int color) {
      int i;

      frameColorMap = parent;
      addMouseMotionListener(this);
      addMouseListener(this);
      addKeyListener(this);

      this.pimg = pimg;
      isPlanImageRGB = pimg instanceof PlanImageRGB;
      isPlanBGRGB = pimg instanceof PlanBG && ((PlanBG)pimg).isColored();

      BKGD = pimg.aladin.frameCM.getBackground();

      // Positionnement des triangles
      if( isPlanImageRGB ) {
         for( i=0; i<3; i++ ) triangle[i]=((PlanImageRGB)pimg).RGBControl[color*3+i];

      } else if( isPlanBGRGB ) {
         for( i=0; i<3; i++ ) triangle[i]=((PlanBG)pimg).RGBControl[color*3+i];

      } else {
         for( i=0; i<3; i++ ) triangle[i]=pimg.cmControl[i];
      }

      rgb=color;
   }

   private CanvasColorMap cm1=null,cm2=null;
   protected void setOtherColorMap(CanvasColorMap cm1, CanvasColorMap cm2 ) {
      this.cm1=cm1;
      this.cm2=cm2;
   }


   /** Retourne la liste des noms des colormaps */
   public static String [] getCMList() {
      String res [] = new String[ FrameColorMap.CM.length +
                                  (customCMName==null ? 0 : customCMName.size())];
      // ajout des CM par défaut
      int i=0;
      for( ; i<FrameColorMap.CM.length; i++ ) res[i] = FrameColorMap.CM[i];

      // ajout des CM "custom"
      if( CanvasColorMap.customCMName!=null ) {
         Enumeration e = CanvasColorMap.customCMName.elements();
         while( e.hasMoreElements() ) {
            res[i++] = (String) e.nextElement();
         }
      }
      return res;
   }

   /** Retourne le nom d'une colormap particulière */
   public static String getCMName(int i) { return getCMList()[i]; }



   /** Positionnement des triangles par defaut */
   protected void reset() {
      triangle[MIN]=0;
      triangle[MIL]=128;
      triangle[MAX]=255;
      repaint();
   }

   /** Construction de la table des couleurs en fonctions des triangles
    * @return  la table des couleurs generee
    */
   protected IndexColorModel getCM() {
      flagCMBand=false;
      return getCM(triangle[0],triangle[1],triangle[2],
            pimg.video==PlanImage.VIDEO_INVERSE,
            pimg.typeCM,pimg.transfertFct,pimg.isTransparent());
   }

   static byte [] r = new byte[256];
   static byte [] g = new byte[256];
   static byte [] b = new byte[256];

   /**
    * Effectue l'interpolation linéaire d'une palette RGB déterminée.
    * La palette résultante (interpolée) est stockée dans les tableaux statiques r,g et b de l'objet ColorMap
    *
    * @param Sr La composante R de la palette à interpoler
    * @param Sg La composante G de la palette à interpoler
    * @param Sb La composante B de la palette à interpoler
    * @param inverse	à true si l'on veut les couleurs inversées; à false pour la palette normale
    * @param transp     true si la palette a sa première case utilisée pour la transparence
    * @param tr0 l'abscisse du 1er point de contrôle de la courbe en deux parties (cf. FrameCM)
    * @param tr1 l'abscisse du 2e point de contrôle de la courbe en deux parties (cf. FrameCM)
    * @param tr2 l'abscisse du 3e point de contrôle de la courbe en deux parties (cf. FrameCM)
    * @param fct fonction de transfert
    *
    * @author Pierre Fernique - 27/3/2009 réécriture complète parce que compliqué et bugué
    * @author Robin -- 09/07/2004
    */
   static void interpolPalette(int[] Sr, int[] Sg, int[] Sb, boolean inverse, boolean transp,
         int tr0, int tr1, int tr2,int fct) {

      double range = 256;
      int gap=0;
      if( transp ) { range=255; gap=1; }
      
      double mid = range/2;
      double pas1 = mid/(tr1-tr0);
      double pas2 = mid/(tr2-tr1);

//      int max = Sr.length-1;
      int max = (int)range-1;

      int [] fctGap = computeTransfertFct(fct,transp);

      for( int i=0; i<range; i++ ) {
         int j= i<tr0 ? 0 :
            i<tr1 ? (int)Math.round((i-tr0)*pas1) :
               i<tr2 ? (int)(Math.round( mid + (i-tr1)*pas2)) :
                  max;

               j = fctGap[j];

               if( j>=Sr.length ) j=Sr.length-1;
               else if( j<0 ) j=0;
               if( inverse ) j=Sr.length-j-1;

               r[i+gap]=(byte) (0xFF & Sr[ j ]);
               g[i+gap]=(byte) (0xFF & Sg[ j ]);
               b[i+gap]=(byte) (0xFF & Sb[ j ]);
      }
   }


   static public IndexColorModel getRainbowCM(boolean transp) {
      byte[] red   = new byte[256];
      byte[] green = new byte[256];
      byte[] blue  = new byte[256];
      for (int i=0; i<256; i++) {
         red[i]   = (byte) (0xFF & RAINBOW_R[i]);
         green[i] = (byte) (0xFF & RAINBOW_G[i]);
         blue[i]  = (byte) (0xFF & RAINBOW_B[i]);
      }

      return transp ? new IndexColorModel(8, 256, red, green, blue,0)
      : new IndexColorModel(8, 256, red, green, blue);
   }

   // index de la dernière color map "par défaut" (par distinction avec celles ajoutées par l'utilisateur)
   static final int LAST_DEFAULT_CM_IDX = PlanImage.CMSTERN;

   // Pour la table de couleur d'une bande entre minb et maxb
   static private byte rb[] = new byte[256];
   static private byte gb[] = new byte[256];
   static private byte bb[] = new byte[256];

   // Valeur min et max de la bande (voir getCMBand())
   static private int minb,maxb;

   // Retour le niveau de rouge en fonction de la position dans la bande
   static private int getRBandColor(int x) {
      return (255-Math.abs(x-(maxb+minb)/2)*10);
   }

   static public ColorModel getCMBand(int greyLevel, boolean inverse,boolean background,boolean transp) {
      return getCMBand(greyLevel-SIZEBAND,greyLevel+SIZEBAND, inverse,background,transp);
   }

   /** Génère une colormap temporaire ne montrant qu'une bande entre min et max */
   static private IndexColorModel getCMBand(int min,int max,boolean inverse,boolean background,boolean transp) {
      minb=min; maxb=max;
      int milieu = (min+max)/2;
      for( int i=0; i<256; i++ ) {
         if( i<min || i> max ) rb[i] = gb[i] = bb[i] = (byte)(!background ? (inverse?255:0)
               : inverse ? 255-i : i);
         else if( i==milieu ) { rb[i]=(byte)0xFF; gb[i]=bb[i]=0; }
         else {
            byte c = (byte)getRBandColor(i);
            rb[i]=0;
            gb[i]=c;
            bb[i]=c;
         }
      }
      return transp ? new IndexColorModel(8,256,rb,gb,bb,0) : new IndexColorModel(8,256,rb,gb,bb);
   }

   /** Creation de la table des couleurs en fonction des parametres
    * @param tr0 cut min
    * @param tr1 1/2 position
    * @param tr2 cut max
    * @param fct fonction de transfert (Log...)
    * @param inverse true s'il y a inversion
    * @param typeCM Type de ColorMap
    * @return la table des couleurs generee
    */
   // des triangles
   public static IndexColorModel getCM(int tr0,int tr1,int tr2, boolean inverse,int typeCM,int fct) {
      return getCM(tr0,tr1,tr2,inverse,typeCM,fct,false);
   }
   public static IndexColorModel getCM(int tr0,int tr1,int tr2, boolean inverse,int typeCM,int fct,boolean transp) {
      int i,n;

      int [] rd = new int[256];
      int [] gn = new int[256];
      int [] bl = new int[256];

      if( typeCM == PlanImage.CMBB ) {
         for( i=0; i<256; i++ ) {
            rd[i] = i<<1; if( rd[i]>255 ) rd[i]=255;
            gn[i] = (i-64)<<1;  if( gn[i]<0 ) gn[i]=0; else if( gn[i]>255 ) gn[i]=255;
            bl[i] = (i-128)<<1; if( bl[i]<0 ) bl[i]=0;
         }
         interpolPalette(rd,gn,bl,inverse,transp,tr0,tr1,tr2,fct);

         // S'il s'agit d'une table des couleurs A
      } else if( typeCM == PlanImage.CMA ) {
         for( i=0; i<256; i++ ) {
            rd[i] = (i<64)?0:(i<128)?(i-64)<<2:255;
            gn[i] = (i<64)?i<<2:(i<128)?255-((i-64)<<2):(i<192)?0:(i-192)<<2;
            bl[i] = (i<32)?0:(i<128)?((i-32)<<3)/3:(i<192)?255-((i-128)<<2):0;
         }
         interpolPalette(rd,gn,bl,inverse,transp,tr0,tr1,tr2,fct);

         // S'il s'agit d'une table des couleurs STERN SPECIAL -- #Robin - 02/07/2004
      } else if( typeCM == PlanImage.CMSTERN ) {
         interpolPalette(SR,SG,SB,!(inverse),transp,tr0,tr1,tr2,fct);

         // S'il s'agit d'une table de couleurs "custom"
      } else if( typeCM>LAST_DEFAULT_CM_IDX ) {
         int idx = typeCM-LAST_DEFAULT_CM_IDX-1;
         MyColorMap myCM = (MyColorMap)customCM.elementAt(idx);
         interpolPalette(myCM.getRed(),myCM.getGreen(),myCM.getBlue(),!(inverse),transp,tr0,tr1,tr2,fct);

         // Sinon table de niveaux de gris, en cas de trasnparence, on décale de 1
      } else {
         int gap = transp ? 1 : 0;
         for( i=gap; i<256; i++ ) rd[i]=gn[i]=bl[i]=i-gap;
         interpolPalette(rd,gn,bl,inverse,transp,tr0,tr1,tr2,fct);
      }

      return transp ? new IndexColorModel(8,256,r,g,b,0) : new IndexColorModel(8,256,r,g,b);
   }


   /** Modifie les indices des 256 entrées (resp 255) d'une table des couleurs pour tenir
    * compte d'une fonction de transfert
    * @param fct le code de la fonction de transfert (PlanImage.LOG, .SQRT, .SQR)
    */
   static private int [] computeTransfertFct(int fct,boolean transp) {
      int range = transp ? 255 : 256;
      
      int [] r = new int[range];

      if( fct==PlanImage.LINEAR ) {
         for( int i=0; i<range; i++ ) r[i]=i;
         return r;
      }

      double val[] = new double[range];
      double min=Double.MAX_VALUE;
      double max=-min;
      double v;
      for( int i=0; i<range; i++ ) {
         v = i;
         switch(fct) {
            case PlanImage.ASINH: val[i] = v = Math.log(v + Math.sqrt(Math.pow(v, 2.)+1)); break;
            case PlanImage.LOG:   val[i] = v = Math.log(v/10+1);  break;
            case PlanImage.SQRT:  val[i] = v = Math.sqrt(v/10.);  break;
            case PlanImage.SQR:   val[i] = v  = v*v;              break;
         }
         if( v<min ) min=v;
         if( v>max ) max=v;
      }
      double x = range/(max-min);
      for( int i=0; i<range; i++ ) {
         v = x*val[i] - min;
         if( v>=range ) v=range-1;
         else if( v<0 ) v=0;
         r[i] = (int)v;
      }

      return r;
   }

   static final private Dimension DIM = new Dimension(W,H);
   static final private Dimension DIMRGB = new Dimension(W,H-cmH);

   @Override
   public Dimension getMinimumSize() { return getPreferredSize(); }
   public Dimension getPreferredSize() { return isPlanBGRGB || isPlanImageRGB ? DIMRGB : DIM; }

   /** Dessin d'un triangle.
    * @param g le contexte graphique
    * @param x l'abcisse du triangle a dessinner
    */
   protected void drawTriangle(Graphics g,int x) {
      int [] tx = new int[4];
      int [] ty = new int[4];

      tx[0] = tx[3] = x+mX;
      tx[1] = tx[0]-7;
      tx[2] = tx[0]+7;

      ty[0] = ty[3] = Hp+4+mY;
      ty[1] = ty[2] = ty[0]+10;

      g.fillPolygon(tx,ty,tx.length);
   }

   /** Ajustement des valeurs des triangles pour concerver un ordre coherent MIN<=MIL<=MAX */
   protected void ajustTriangle() {
      int i;

      for( i=0; i<2; i++ ) {
         if( triangle[i]>triangle[i+1]-5 ) triangle[i]=triangle[i+1]-5;
      }
      for( i=2; i>0; i-- ) {
         if( triangle[i]<triangle[i-1] ) triangle[i-1]=triangle[i]-5;
      }
      for( i=2; i>0; i-- ) {
         if( triangle[i]<0 ) triangle[i]=0;
         if( triangle[i]>255 ) triangle[i]=255;
      }
   }

   /** Tracage de la courbe de reponse des gris
    * @param g le contexte graphique
    */
   protected void drawLinear(Graphics g) {
      int i;
      int y [] = (pimg.video==PlanImage.VIDEO_NORMAL)?Y:Yr;
      int [] x = new int[5];
      x[0]=mX;
      x[1]=mX+triangle[MIN]; x[2]=mX+triangle[MIL];  x[3]=mX+triangle[MAX];
      x[4]=mX+256;

      g.setColor( Color.black );
      for( i=0; i<4; i++ ) {
         g.drawLine(x[i],y[i], x[i+1],y[i+1]);
         g.drawLine(x[i],y[i]-1, x[i+1],y[i+1]-1);
      }

      for( i=1; i<4; i++ ) g.drawLine(x[i],mY+Hp+3, x[i],y[i]);
   }

   static int ogreyLevel=-1;
   private boolean memoGreyLevel(int x) {
      greyLevel = (x<0)?0:(x>255)?255:x;

      // Reaffichage si necessaire
      if( ogreyLevel!=greyLevel ) {
         ogreyLevel=greyLevel;

         // On en profite pour mettre à jour la boite à outils
         resumePixelTool();

         return true;
      }

      return false;
   }


   // Mise à jour des informations de la pixelToolBox
   protected void resumePixelTool() {
      if( pimg.aladin.framePixelTool==null || !pimg.aladin.framePixelTool.isVisible() ) return;
      pimg.aladin.framePixelTool.setParams(pimg, getLastPixel());
   }


   boolean flagCMBand=false;

   public void mouseMoved(MouseEvent e) {
      int x = e.getX();
      int y = e.getY();
      x-=mX;
      if( !memoGreyLevel(x) ) return;

      if( pimg.type!=PlanImage.IMAGERGB &&  y>=cmY && y<=cmY+cmH ) {
         //         System.out.println("Pimg.video = "+pimg.video);
         pimg.setCM(getCMBand(greyLevel, pimg.video==PlanImage.VIDEO_INVERSE,
               !e.isShiftDown(),pimg.isTransparent()));
         isDragging=flagCMBand=true;
         pimg.aladin.view.getCurrentView().repaint();
         repaint();

      } else stopBand();

      if( !flagCMBand ) {
         //         nearTriangle(x,y);
         repaint();
      }
   }

   protected void stopBand() {
      if( flagCMBand ) {
         pimg.setCM(getCM());
         pimg.aladin.view.getCurrentView().repaint();
      }
      flagCMBand=isDragging=false;
   }

   /** Reperage du triangle le plus proche du clic souris */
   public void mousePressed(MouseEvent e) {
      int x = e.getX();
      x-=mX;
      oy=0;

      repaint();

      // Reperage du triangle le plus proche
      for( int i=0; i<3; i++ ) {
         if( x>triangle[i]-7 && x<triangle[i]+7 ) {
            if( i<MIL && triangle[MIL]==triangle[MAX] ) i=MAX;
            currentTriangle=i;
            Aladin.makeCursor(this, i==1 ? Aladin.MOVECURSOR : Aladin.STRECHCURSOR);
            isDragging=true;
            return;
         }
      }
      if( currentTriangle==NO && x>triangle[MIN]+10 && x<triangle[MAX]-10 ) {
         currentTriangle=MIL;
         Aladin.makeCursor(this, Aladin.MOVECURSOR);
         isDragging=true;
         return;
      }
      currentTriangle = NO;
      Aladin.makeCursor(this, Aladin.HANDCURSOR);
   }


   private int oy=-1;
   private int trMin=0;
   private int trMax=0;
   private int t[] = new int[3];

   /** Deplacement du triangle precedemment selectionne avec mise a jour
    *  d'une portion de l'image
    */
   public void mouseDragged(MouseEvent e) {
      if( currentTriangle==NO ) return;
      int x = e.getX();
      int y = e.getY();
      x-=mX;
      memoGreyLevel(x);
      System.arraycopy(triangle, 0, t, 0, 3);

      flagLosange = e.isControlDown() || (e.getModifiers() & java.awt.event.InputEvent.BUTTON3_MASK) !=0;

      if( x<0 ) x=0;
      else if( x>255 ) x=255;

      int ox = triangle[1];
      triangle[currentTriangle] = x;
      if( triangle[1]<5 ) triangle[1]=5;
      if( triangle[1]>250 ) triangle[1]=250;

      // Pour pouvoir également controler les cuts haut et bas par clic&drag à ls DS9
      if( currentTriangle==1 && flagLosange ) {
         int fct = 256/Hp;
         triangle[0]-=ox-triangle[1];
         triangle[2]-=ox-triangle[1];
         int deltaY = y-oy;
         if( oy!=-1 && deltaY!=0 ) {
            triangle[0]+=deltaY*fct;
            if( triangle[0]>triangle[1]-5 ) triangle[0]=triangle[1]-5;
            triangle[2]-= deltaY*fct ;
            if( triangle[2]<triangle[1]+5 ) triangle[2]=triangle[1]+5;
         }
         oy=y;
      }
      if( trMin>0 ) { triangle[0]-=trMin; trMin=0; }
      if( triangle[0]<0 ) { trMin-= triangle[0]; triangle[0]=0; }

      if( trMax>0 ) { triangle[2]+=trMax; trMax=0; }
      if( triangle[2]>255 ) { trMax+=(triangle[2]-255); triangle[2]=255; }

      ajustTriangle();

      // répercussion sur les autres colormaps si SHIFT appuyé
      if( e.isShiftDown() &&(cm1!=null || cm2!=null)  ) {
         for( int i=0; i<3; i++ ) {
            if( t[i]!=triangle[i] ) {
               if( cm1!=null ) cm1.triangle[i]=triangle[i];
               if( cm2!=null ) cm2.triangle[i]=triangle[i];
            }
         }
         if( cm1!=null ) cm1.repaint();
         if( cm2!=null ) cm2.repaint();
      }

      if( isPlanImageRGB ) {
         pimg.aladin.view.getCurrentView().setFilter(triangle,rgb);
         pimg.aladin.calque.zoom.zoomView.repaint();
      } else if( isPlanBGRGB ){
         ((PlanBG)pimg).filterRGB(triangle,rgb);
      } else pimg.setCM(getCM());

      pimg.aladin.view.getCurrentView().repaint();

      repaint();
   }


   /** Indique si on est en train de fair un drag de la table des couleurs */
   final protected boolean isDragging() { return isDragging; }

   /** Fin de deplacement du triangle precedemment selectionne */
   public void mouseReleased(MouseEvent e) {
      isDragging=false;
      if( currentTriangle==NO ) { repaint(); return; }
      int x = e.getX();
      oy=-1;
      x-=mX;
      if( x<0 ) x=0;
      else if( x>255 ) x=255;

      Aladin.makeCursor(this, Aladin.HANDCURSOR);

      triangle[currentTriangle] = x;
      ajustTriangle();
      repaint();

      pimg.aladin.view.recreateMemoryBufferFor(pimg);  // Pour palier au bug linux

      if( isPlanImageRGB ) {
         pimg.aladin.view.getCurrentView().setFilter(triangle,rgb);
         pimg.aladin.calque.zoom.zoomView.repaint();
      } else if( isPlanBGRGB ){
         ((PlanBG)pimg).filterRGB(triangle,rgb);
      } else pimg.setCM(getCM());

      frameColorMap.resumeWidgets();
   }

   public void mouseEntered(MouseEvent e) {
      Aladin.makeCursor(this,Aladin.HANDCURSOR);
      requestFocusInWindow();
   }

   public void mouseExited(MouseEvent e) {
      stopBand();
      greyLevel=-1;
      Aladin.makeCursor(this,Aladin.DEFAULTCURSOR);
      repaint();
   }

   private long lastHistID=-1;

   public void updateImageHist() {
      int i;
      if( imgHist!=null && pimg.getHistID()==lastHistID ) return;
//      System.out.println("updateImageHist lastHistID="+lastHistID+" pimg.getHistID="+pimg.getHistID());
      lastHistID=pimg.getHistID();
      if( imgHist==null ) {
         imgHist = pimg.aladin.createImage(257,Hp+20);
         gHist=imgHist.getGraphics();
      }

      gHist.setColor( BKGD );
      gHist.fillRect(0,Hp,256,Hp);
      gHist.setColor( Color.white );
      gHist.fillRect(0,0,256,Hp);

      double hist[] = pimg.getHist(rgb);

      // S'il s'agit d'un des histogrammes couleurs
      // on affecte une couleur correspondante
      if( rgb>-1 ) {
         // Dans le cas de la video inverse, on inverse la couleur des histogrammes
         if( pimg.video==PlanImage.VIDEO_INVERSE ) {
            gHist.setColor( (rgb==0)?Color.cyan:
               ( (rgb==1)?Color.magenta: Color.yellow ) );
         } else { // sauf dans le cas de 2 couleurs
            gHist.setColor( (rgb==0)?Color.red:
               ( (rgb==1)?Color.green: Color.blue ) );
         }
      } else {
         gHist.setColor( Color.cyan );
      }
      for( i=0; i<256; i++ ) gHist.drawLine( i,Hp, i,(int)(Hp-hist[i]) );

      gHist.setColor( Color.black );
      if( isPlanImageRGB && ((PlanImageRGB)pimg).labels[rgb]!=null )
         gHist.drawString(((PlanImageRGB)pimg).labels[rgb],100,20);

      gHist.drawRect(0,0,256,Hp);
      gHist.setFont(Aladin.SPLAIN);
      String c = "color";
      for( i=0; i<c.length(); i++ ) {
         gHist.drawString(c.substring(i,i+1),mX+256+20,50+12*i);
      }

//      System.out.println("updateImageHist rgb="+rgb);
   }

   @Override
   public void paintComponent(Graphics gd) {
      int i;
      super.paintComponent(gd);

      updateImageHist();
      gd.setColor(BKGD);
      gd.fillRect(0,0,W,H);

      gd.drawImage(imgHist,mX,mY,this);

      for( i=0; i<3; i++ ) {
         gd.setColor( i==currentTriangle ? Color.red : Color.black);
         drawTriangle(gd,triangle[i]);
      }

      if( isPlanImageRGB || isPlanBGRGB ) drawLinear(gd);
      else drawFct(gd);
   }
   
   
   /** Méthode static pour le tracer de n'importe quelle CM */
   static protected void drawColorMap(Graphics gr,int dx,int dy,int width,int height,
         int typeCM,boolean reverse) {
      IndexColorModel idm = getCM(0,128,255,reverse,typeCM,PlanImage.LINEAR);
      double gapx = width/256.;
      double x=0;
      for( int i=0,xc=0; i<256; i++, x+=gapx ) {
         int red = idm.getRed(i);
         int green = idm.getGreen(i);
         int blue = idm.getBlue(i);
         gr.setColor( new Color(red,green,blue) );
         for( ;xc<x; xc++) gr.drawLine(dx+xc,dy,dx+xc,dy+height);
      }
   }
   
   protected void drawColorMap(Graphics gr,int dx,int dy,int width,int height) {
      double gapx = width/256.;
      double x=0;
      for( int i=0,xc=0; i<256; i++, x+=gapx ) {
         gr.setColor( getColor(i) );
         for( ;xc<x; xc++) gr.drawLine(dx+xc,dy,dx+xc,dy+height);
      }
   }

   protected Color getColor(int i) {
      Color c;
      if( !flagCMBand ) c=new Color(r[i]&0xFF,g[i]&0xFF,b[i]&0xFF);
      else c = new Color(rb[i]&0xFF,gb[i]&0xFF,bb[i]&0xFF);
      return c;
   }

   /** retourne la valeur du dernier pixel sous la souris (valeur physique) */
   protected double getLastPixel() {
      try {
         return Double.parseDouble( pimg.getPixelInfoFromGrey(greyLevel) );
      } catch( Exception e ) {}
      return Double.NaN;
   }


   /** Trace les colormaps pour les trois composantes et divers infos lié à la position
    * de la souris sur le graphique */
   protected void drawFct(Graphics gr) {
      double f = Hp/256.;
      int x,y,ox=0,oy=0;

      // Table de couleur résultante (en haut)
      drawColorMap(gr,mX,cmY,256,cmH);

      // Le titre
      gr.setColor( Aladin.MYBLUE );
      gr.drawString(frameColorMap.CMCM,85,cmY-2);

      // Tracé des 3 courbes de la colormap en superposition de l'histogramme
      if( !flagCMBand ) {
         for( int j=0; j<3; j++ ) {
            gr.setColor(j==0?Color.red:j==1?Aladin.COLOR_GREEN:Color.blue);
            byte t[] = j==0?r:j==1?g:b;
            for( int i=0; i<256; i++ ) {
               y = (int)( (256-(t[i]&0xFF))*f-0.5);
               x = i+mX;
               if( i>0 ) gr.drawLine(ox,mY+oy,x,mY+y);
               ox=x; oy=y;
            }
         }
      }

      // Affichage de la valeur du pixel courant (sous la souris) en bas
      if( greyLevel>0 && pimg.hasAvailablePixels() ) {
         String s = pimg.getPixelInfoFromGrey(greyLevel);
         x = greyLevel+mX;

         // traits rouges du pixel courant
         gr.setColor(Color.red);
         gr.drawLine(x,mY+Hp, x, mY+Hp - (int)(pimg.hist[greyLevel]) );
         gr.drawLine(x,cmY,x,cmY+cmH);

         // Valeur du pixel sous la souris
         int len = gr.getFontMetrics().stringWidth(s);
         x = W/2-len/2;
         y = mY+14;
         int h=15;
         Util.drawCartouche(gr, x, y-h/2-5, len, h, 0.8f,  null, Color.white);
         gr.drawString(s,x,y);
      }

   }

   /** ajout d'une colormap personnalisée à la liste des CM disponibles
    *
    * @param cm la colormap à ajouter
    * @param nom un nom associé à la colormap
    *
    * @return un nom <b>unique</b> associé à la colormap
    */
   static public synchronized String addCustomCM(MyColorMap cm) {

      if( customCM==null ) {
         customCM = new Vector();
         customCMName = new Vector();
      }


      String name = checkCMNameUnicity(cm.getName());
      cm.setName(name);
      customCM.addElement(cm);
      customCMName.addElement(name);

      return name;
   }

   static private String checkCMNameUnicity(String s) {
      String newName = new String(s);
      int n=1;
      while( customCMName.contains(newName) ) newName=newName+(n++);
      return newName;
   }
   public void mouseClicked(MouseEvent e) {
      // TODO Auto-generated method stub

   }

   /** Pour dessiner le losange du curseur central lors de l'appui
    * de la touche CTRL */
   public void keyPressed(KeyEvent e) {
      boolean control = e.isControlDown();
      if( control!=flagLosange ) repaint();
      flagLosange = control;

   }
   public void keyReleased(KeyEvent e) {
      keyPressed(e);
   }
   public void keyTyped(KeyEvent e) {
      // TODO Auto-generated method stub

   }

}

