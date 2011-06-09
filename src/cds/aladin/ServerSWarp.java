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

import java.awt.*;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import javax.swing.*;

import cds.tools.MultiPartPostOutputStream;

/**
 * Le formulaire pour interroger SWarp � distance
 *
 * @author Thomas Boch [CDS]
 * @version 0.9 : 10 mars 2009 - Cr�ation
 */
public class ServerSWarp extends Server  {

    private String outputProj, atLeastOne;

    private JComboBox comboProj;

    private String baseUrl = "http://alasky.u-strasbg.fr/cgi/SWarp/nph-SWarp.py";

    // TODO : liste compl�te � valider avec Caro et Mark
    private String[] availableProjections = {"TAN", "SIN", "ARC", "AIT",
                                             "ZEA", "ZPN", "STG"};

  /** Initialisation des variables propres a Simbad */
   protected void init() {
      type    = IMAGE;
      aladinLabel     = "SWarp image co-adding service";
      aladinLogo    = "SWarp.gif";
      docUser = "http://terapix.iap.fr/rubrique.php?id_rubrique=49";
   }


   protected void createChaine() {
      // TODO : localisation
      title = aladin.chaine.getString("SWTITLE");
      description = aladin.chaine.getString("SWINFO");
      outputProj = aladin.chaine.getString("SWOUTPUTPROJ");
      atLeastOne = aladin.chaine.getString("SWATLEASTONE");
      super.createChaine();
   }

 /** Creation du formulaire d'interrogation de Simbad.
   * @param aladin reference
   * @see ServerSimbad.SimbadServer#createGluSky()
   */
   protected ServerSWarp(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
      init();

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=Aladin.OUTREACH ? YOUTREACH : 50;
      int X=150;

      // Le titre
      JPanel tp = new JPanel();
      Dimension d = makeTitle(tp,title);
      tp.setBackground(Aladin.BLUE);
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);

      // Un texte d'aide pour remplit le formulaire
      JLabel l = new JLabel(description);
      l.setBounds(90,y,400, 20); y+=20;
      add(l);

      // combo box pour choix des images � mosa�quer
      int nbCombo = 8;
      input = new JComponent[nbCombo];
      modeInput = new int[nbCombo];
      nbInput = nbCombo;
      for( int i=0; i<nbCombo; i++ ) {
          JLabel pTitre = new JLabel(addDot("Image "+(i+1)));
          pTitre.setFont(Aladin.BOLD);
          pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
          add(pTitre);
          JComboBox combo = new JComboBox();
          combo.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
          add(combo);
          input[i] = combo;
          modeInput[i] = Server.IMG;
      }

      // combo box pour choix projection image resultat
      y += 1.3*(HAUT+MARGE);
      JLabel pTitre = new JLabel(addDot(outputProj));
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);
      comboProj = new JComboBox();
      comboProj.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
      add(comboProj);
      for( int i=0; i<availableProjections.length; i++ ) {
          comboProj.addItem(availableProjections[i]);
      }
   }

   protected void adjustInputChoice(JComboBox c, Vector v,int defaut) {
       int i=c.getSelectedIndex();
       String s=(i>=0) ? (String)c.getItemAt(i) : null;
       c.removeAllItems();

       c.addItem(NOINPUTITEM);
       if( v!=null ) {
          Enumeration e = v.elements();
          while( e.hasMoreElements() ) c.addItem( ((Plan)e.nextElement()).label );
       }

       // S�lection de l'item d�sign�
       if (defaut>0) c.setSelectedIndex(defaut);

       // Premier item, ou deuxi�me item
       else if (s==null || s.equals(NOINPUTITEM)) c.setSelectedIndex(0);

       // Pr�c�dent item s�lectionn�
       else c.setSelectedItem(s);
    }

   protected boolean isDiscovery() { return false; }

   private void submitThread() {
       waitCursor();
       Set selectedImages = new HashSet();
       Plan[] planes;
       for( int i=0; i<input.length; i++ ) {
           planes = getInputPlane(input[i]);
           if( planes==null || planes.length==0 ) {
               continue;
           }
           selectedImages.add(planes[0]);
       }
       if( selectedImages.size()==0 ) {
           Aladin.warning(atLeastOne);
           return;
       }
       Iterator it = selectedImages.iterator();
       Plan p;

       // POST parameters and launch SWarp processing
       URL url;
       try {
           url = new URL(baseUrl);
       }
       catch(MalformedURLException mue) {
           defaultCursor();
           mue.printStackTrace();
           return;
       }

       MyInputStream mis;
       try {
           MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
           String boundary = MultiPartPostOutputStream.createBoundary();
           URLConnection urlConn = MultiPartPostOutputStream.createConnection(url);
           urlConn.setRequestProperty("Accept", "*/*");
           urlConn.setRequestProperty("Content-Type",
               MultiPartPostOutputStream.getContentType(boundary));
           // set some other request headers...
           urlConn.setRequestProperty("Connection", "Keep-Alive");
           urlConn.setRequestProperty("Cache-Control", "no-cache");
           MultiPartPostOutputStream out =
               new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

           // type de projection
           out.writeField("projection", comboProj.getSelectedItem().toString());

           if( aladin.save==null ) {
               aladin.save = new Save(aladin);
           }

           int imgIdx = 1;
           String paramName;
           while( it.hasNext() ) {
               paramName = "img"+imgIdx;
               p = (Plan)it.next();
               // if remote URL : just pass the URL value
               if( p.hasRemoteUrl()) {
                   out.writeField(paramName, p.getUrl());
               }
               // if local file : post the whole file content
               else {
                   out.writeFile(paramName, "image/fits", paramName+".fits",
                                 aladin.save.saveImageFITS((OutputStream)null,
                                 (PlanImage)p), true);
               }
               imgIdx++;
           }
           out.close();

           aladin.calque.newPlanImage(urlConn.getInputStream(), "SWarp", "SWarp");
       }
       catch(Exception ioe) {
           defaultCursor();
           ioe.printStackTrace();
           Aladin.warning("An error occured while contacting the SWarp service");
           return;
       }
       defaultCursor();
   }

  /** Soumission du formulaire */
   public void submit() {
       new Thread() {
           public void run() {
               submitThread();
           }
       }.start();
   }
}
