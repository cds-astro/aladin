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

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import cds.moc.HealpixMoc;
import cds.tools.MultiPartPostOutputStream;

/**
 * Le formulaire pour interroger par MOC
 *
 * @author Thomas Boch [CDS]
 * @version 0.9 : 20 juillet 2012
 */
public class ServerMocQuery extends Server  {

    static private String[] LARGE_CATS = {"2MASS", "CMC14", "UCAC3", "UCAC4", "2MASS6X", "SDSS8", "USNOB1", "WISE_ALLSKY",
                                          "DENIS", "GLIMPSE", "GSC23", "NOMAD", "PPMX", "PPMXL", "TYCHO2", "SIMBAD",
                                          "UKIDSS_DR8_LAS", "UKIDSS_DR6_GPS"};

    private String baseUrl = "http://cdsxmatch.u-strasbg.fr/QueryCat/QueryCat";

    private JComboBox comboMoc;
    private JComboBox comboCat;
    private JTextField textCat;
    private JComboBox comboMaxNbRows;



  /** Initialisation des variables propres à MocQuery */
   protected void init() {
      type    = CATALOG;
      title = "MOC query";
      aladinLabel     = "MOC";
   }


   protected void createChaine() {
      // TODO : localisation
       description = "Query by MOC";
      super.createChaine();
   }

 /** Creation du formulaire d'interrogation par MOC
   * @param aladin reference
   */
   protected ServerMocQuery(Aladin aladin) {
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

      // combo box pour choix du MOC
      JLabel pTitre = new JLabel("Choose a MOC");
      pTitre.setFont(Aladin.BOLD);
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);
      comboMoc = new JComboBox();
      comboMoc.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
      add(comboMoc);

      // combo box pour choix du catalogue à interroger
      pTitre = new JLabel("Catalog to query");
      pTitre.setFont(Aladin.BOLD);
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);
      y+=HAUT+MARGE;

      pTitre = new JLabel("Choose in list");
      pTitre.setFont(Aladin.ITALIC);
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);
      comboCat = new JComboBox();
      comboCat.addItem("---");
      Arrays.sort(LARGE_CATS);
      for (String cat: LARGE_CATS ) {
          comboCat.addItem(cat);
      }

      comboCat.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
      add(comboCat);

      // champ texte pour entrer nom catalogue
      pTitre = new JLabel("Or enter a VizieR table ID");
      pTitre.setFont(Aladin.ITALIC);
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);

      textCat = new JTextField();
      textCat.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
      add(textCat);

      // listener on comboCat
      comboCat.addItemListener(new ItemListener() {

          public void itemStateChanged(ItemEvent e) {
              if (e.getItem().equals("---")) {
                  textCat.setEnabled(true);
              }
              else {
                  textCat.setEnabled(false);
                  textCat.setText("");
              }
          }
      });

      // combo box pour limiter le nombre de sources
      pTitre = new JLabel("Max nb of rows");
      pTitre.setFont(Aladin.BOLD);
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);
      comboMaxNbRows = new JComboBox();
      comboMaxNbRows.addItem("10,000");
      comboMaxNbRows.addItem("50,000");
      comboMaxNbRows.addItem("100,000");
      comboMaxNbRows.addItem("unlimited");
      comboMaxNbRows.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
      add(comboMaxNbRows);

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

       // Sélection de l'item désigné
       if (defaut>0) c.setSelectedIndex(defaut);

       // Premier item, ou deuxième item
       else if (s==null || s.equals(NOINPUTITEM)) c.setSelectedIndex(0);

       // Précédent item sélectionné
       else c.setSelectedItem(s);
    }

    @Override
    public void setVisible(boolean flag) {
        // update list of MOCs
        if (flag) {
            Vector<Plan> mocs = aladin.calque.getPlans(PlanMoc.class);
            comboMoc.removeAllItems();
            if (mocs != null) {
                for (Iterator it = mocs.iterator(); it.hasNext();) {
                    comboMoc.addItem(it.next());
                }
            }
        }

        super.setVisible(flag);
    }

   protected boolean isDiscovery() { return false; }

   private void submitThread() {
       waitCursor();


       URL url;
       try {
           url = new URL(baseUrl);
       }
       catch(MalformedURLException mue) {
           defaultCursor();
           mue.printStackTrace();
           return;
       }

       PlanMoc selectedMoc = (PlanMoc)comboMoc.getSelectedItem();
       if (selectedMoc==null) {
           Aladin.warning("No MOC selected !");
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

           String catName;
           if (comboCat.getSelectedItem().equals("---")) {
               catName = textCat.getText().trim();
           }
           else {
               catName = comboCat.getSelectedItem().toString();
           }
           out.writeField("catName", catName);
           out.writeField("mode", "mocfile");
           out.writeField("format", "votable");
           String limit = comboMaxNbRows.getSelectedItem().toString();
           if ( ! limit.equals("unlimited")) {
               limit = limit.replaceAll(",", "");
               out.writeField("limit", limit);
           }


           HealpixMoc hpxMoc = selectedMoc.getMoc();
           File tmpMoc = File.createTempFile("moc", "fits");
           tmpMoc.deleteOnExit();
           FileOutputStream fo = new FileOutputStream(tmpMoc);
           try {
               hpxMoc.writeFits(fo);
           }
           finally {
               try {
                   fo.close();
               }
               catch(Exception e) {}
           }
           out.writeFile("mocfile", null, tmpMoc, false);

           out.close();

           aladin.calque.newPlanCatalog(new MyInputStream(urlConn.getInputStream()), catName + " MOC query");
       }
       catch(Exception ioe) {
           defaultCursor();
           ioe.printStackTrace();
           Aladin.warning("An error occured while contacting the QueryCat service");
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
