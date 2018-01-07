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

package cds.aladin;

import cds.tools.*;

import java.awt.*;
import java.awt.event.*;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.*;

/**
 * Gestion de la fenêtre d'affichage des infos sur les tables d'un plan catalogue
 * @date jan 2009 - création
 * @author P. Fernique
 */
public class FramePixelExtraction extends JFrame {

	private Aladin aladin;
	private PlanImage pimg;

	protected FramePixelExtraction(Aladin aladin) {
	   super();
	   this.aladin = aladin;
       Aladin.setIcon(this);
       setTitle("Pixel extraction");

       enableEvents(AWTEvent.WINDOW_EVENT_MASK);
       Util.setCloseShortcut(this, false, aladin);
       
       JPanel pgen = (JPanel)getContentPane();
       pgen.setBorder( BorderFactory.createEmptyBorder(10,10,15,10));
       pgen.setLayout( new BorderLayout());
       pgen.add( createPanel(), BorderLayout.CENTER );
	   
       setLocation(aladin.computeLocation(this));
       pack();
       setVisible(true);
	}
	
	private JPanel createPanel() {
	   JPanel p = new JPanel( new BorderLayout(5,5));
	   String s;
	   
	   pimg = (PlanImage)aladin.calque.getPlanBase();
	   s =  "<center>"+aladin.chaine.getString("PIXEXTRINFO1")
	           +"<br> <b>Reference image : "+pimg.label+"\nNumber of pixels: "+pimg.naxis1*pimg.naxis2+"</b></center>";
	   JLabel img = new JLabel(Util.fold(s,40,true));
	   p.add(img, BorderLayout.NORTH );
	   
	   s = aladin.chaine.getString("PIXEXTRINFO2");
	   JLabel info = new JLabel(Util.fold(s,40,true),JLabel.CENTER);
	   p.add(info, BorderLayout.CENTER);
	   
	   JPanel confirm = new JPanel();
	   JButton b;
       b = new JButton(aladin.chaine.getString("PIXELEXTRSUBMIT"));
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { pixelExtraction(); dispose(); }
       });

       confirm.add(b);
       b = new JButton(aladin.chaine.getString("SFCANCEL"));
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { dispose(); }
       });
       confirm.add(b);
	   
	   p.add(confirm,BorderLayout.SOUTH); 
	   return p;
	}
	
	
//	static final Dimension DIM = new Dimension(300,200);
//	public Dimension getPreferredSize() { return DIM; }
	
    /** Extraction des pixels des plans images sélectionnés */
    protected void pixelExtraction() {
       
//       if( !confirmation(aladin.chaine.getString("PIXEXTRINFO") ) ) return;
       try {
          Vector v = aladin.calque.getPlanImg();
          int naxis1=pimg.naxis1;
          int naxis2=pimg.naxis2;
          PlanImage p[]=null;
          int n=0;
          int j=0;
          
          // 2 tours, un pour compter, l'autre pour copier
          for( int tour=0; tour<2 ;tour++ ) {
             if( tour==1 ) {
                p = new PlanImage[n+1];
                p[j++]=pimg;
             }
             for( int i=v.size()-1; i>=0; i--) {
                PlanImage p1 = (PlanImage)v.elementAt(i);
                if( p1==pimg || !p1.selected || !p1.hasAvailablePixels() || p1 instanceof PlanBG ) continue;
                if( tour==0 ) n++;
                else p[j++] = p1;
             }
          }
          final PlanImage [] p1 = p;
          final AladinData cat = aladin.createAladinCatalog("Pixels");
          cat.plan.flagProcessing=true;
          aladin.calque.repaintAll();
          (new Thread(){
             public void run() { 
                try { pixelExtraction(cat, p1); }
                catch( Exception e1 ) {
                   if( aladin.levelTrace>=3 ) e1.printStackTrace();
                   cat.plan.error="Pixel extraction error !";
                   aladin.error(cat.plan.error);
                } finally { 
                   cat.plan.flagProcessing = false; 
                }
             }
          }).start();
       } catch( Exception e2 ) {
          if( aladin.levelTrace>=3 ) e2.printStackTrace();
          aladin.error("Pixel extraction error !");
       }
    }
    
    /** Extraction des pixels sous forme de table */
    protected void pixelExtraction(AladinData cat,PlanImage p[]) throws Exception  {
       
       // Création de l'entête du catalogue (1 colonne par image + 2 colonnes pour X,Y);
       int n=p.length+4;
       String [] name = new String[n];
       String [] dataType = new String[n];
       for( int i=0; i<p.length; i++ ) {
          name[i] = p[i].label;
          dataType[i] = "double";
       }
       name[n-4] = "X";    dataType[n-4]="integer";
       name[n-3] = "Y";    dataType[n-3]="integer";
       name[n-2] = "RAJ2000";   dataType[n-2]="double";
       name[n-1] = "DEJ2000";   dataType[n-1]="double";
      
       cat.setName(name);
       cat.setDatatype(dataType);
       
       for( int i=0; i<p.length; i++ ) {
          p[i].setLockCacheFree(true);
          p[i].pixelsOriginFromCache();
       }
       
       try {
          double ra=Double.NaN,dec=Double.NaN;
          Coord coo = new Coord();
          String [] row = new String[n];
          int height = p[0].naxis2;
          int width = p[0].naxis1;
          int shape = width*height>10000 ? Source.DOT : Source.POINT;
          int m=1;
          boolean flagCoo = Projection.isOk(p[0].projd);
          for( int y=0; y<height; y++) {
             for( int x=0; x<width; x++ ) {
                if( flagCoo ) {
                   coo.x=x+0.5; coo.y=y+0.5;
                   p[0].projd.getCoord(coo);
                   ra=coo.al; dec=coo.del;
                }
                row[n-2]=ra+"";
                row[n-1]=dec+"";
                
                coo.x=x; coo.y=y;
                if( flagCoo ) p[0].projd.getCoord(coo);
                
                for( int i=0; i<p.length; i++) {
                   row[i]=" ";
                   if( flagCoo && i>0 ) {
                      if( Projection.isOk(p[i].projd) ) p[i].projd.getXY(coo);
                      else continue;
                   }
                   int x1 = (int)Math.round(coo.x);
                   int y1 = p[i].naxis2-(int)Math.round(coo.y)-1 ;
                   if( p[i].isInside(x1,y1) ) row[i]= p[i].getPixel(x1,y1)+"";
                }
                row[n-4]=(x+1)+"";
                row[n-3]=(height-y)+"";
                cat.addSource("Pix_"+(m++), ra,dec, row).setShape(shape);
             }
          }
          cat.objModified();
          cat.plan.flagProcessing=false;
       } finally {
          for( int i=0; i<p.length; i++ )  p[i].setLockCacheFree(false);
       }
    }
}
