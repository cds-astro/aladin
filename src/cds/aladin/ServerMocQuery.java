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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
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

    protected String   baseUrl;
    protected String title1,title2;

    private JComboBox  comboLocalPlane;
    private JComboBox  comboCat;
    private JTextField textCat;
    private JComboBox  comboMaxNbRows;

  /** Initialisation des variables propres à MocQuery */
   protected void init() {
      type        = CATALOG;
      title       = "MOC query";
      title1      = "Choose a MOC";
      title2      = "Catalog to query";
      aladinLabel = "MOC";
      baseUrl      = "http://cdsxmatch.u-strasbg.fr/QueryCat/QueryCat";
   }

   protected void createChaine() {
      super.createChaine();
      description = "Query by MOC";
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
      JLabel pTitre = new JLabel(title1);
      pTitre.setFont(Aladin.BOLD);
      pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(pTitre);
      comboLocalPlane = new JComboBox();
      comboLocalPlane.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
      add(comboLocalPlane);

      // combo box pour choix du catalogue à interroger
      pTitre = new JLabel(title2);
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
      for (String cat: LARGE_CATS ) comboCat.addItem(cat);

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
              if (e.getItem().equals("---")) textCat.setEnabled(true);
              else { textCat.setEnabled(false); textCat.setText(""); }
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
   
   protected Vector<Plan> getPlans() {
      return aladin.calque.getPlans(PlanCatalog.class);
   }

    @Override
    public void setVisible(boolean flag) {
        // update list of local planes
        if (flag) {
            Vector<Plan> plans = getPlans();
            comboLocalPlane.removeAllItems();
            if (plans != null) {
                for (Iterator it = plans.iterator(); it.hasNext(); ) comboLocalPlane.addItem(it.next());
            }
        }
        super.setVisible(flag);
    }

   protected boolean isDiscovery() { return false; }
   
   private Plan dedicatedLocalPlane=null;
   protected void setPlan(Plan plan) { dedicatedLocalPlane = plan; }
   protected Plan getPlan() {
      if( dedicatedLocalPlane!=null ) return dedicatedLocalPlane;
      return (Plan)comboLocalPlane.getSelectedItem();
   }
   
   private String catName=null;
   protected void setCatName(String cat) { catName=cat; }
   protected String getCatName() {
      if( catName!=null ) return catName;
      if (comboCat.getSelectedItem().equals("---")) return textCat.getText().trim();
      return comboCat.getSelectedItem().toString();
   }
   
   protected String planName=null;
   protected void setPlanName(String label) { planName=label; }
   protected String getPlanName() {
      String s = planName==null ? getCatName() : planName;
      return s + " by MOC";
   }
   
   private String limit=null;
   protected void setLimit(String limit) { this.limit=limit; }
   protected String getLimit() {
      if( limit!=null ) return limit;
      return comboMaxNbRows.getSelectedItem().toString();
   }

   private void submitThread() {
       waitCursor();

       URL url;
       try {
           url = new URL(baseUrl);
           aladin.trace(4,"ServerXQuery.submitThread: url="+url);
       } catch(MalformedURLException e) {
           defaultCursor();
           e.printStackTrace();
           return;
       }

       Plan localPlan = getPlan();
       if (localPlan==null) {
           Aladin.warning("No local plane selected !");
           return;
       }
       
       aladin.trace(4,"Sending local data...");
       try {
           log();
           MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
           String boundary = MultiPartPostOutputStream.createBoundary();
           HttpURLConnection urlConn = (HttpURLConnection)MultiPartPostOutputStream.createConnection(url);
           urlConn.setRequestProperty("Accept", "*/*");
           urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
           urlConn.setRequestProperty("Connection", "Keep-Alive");
           urlConn.setRequestProperty("Cache-Control", "no-cache");
           MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);
           addParameter(out);
           addUpload(out,localPlan);
           out.close();
           aladin.trace(4,"Local data file sent");
           aladin.calque.newPlanCatalog( urlConn, getPlanName());

       } catch(Exception e) {
           defaultCursor();
           e.printStackTrace();
           Aladin.warning("An error occured while contacting the remote service");
           return;
       }
       defaultCursor();
   }
   
   protected void log() {
      aladin.log("MocQuery",getCatName());
   }
   
   protected void addParameter( MultiPartPostOutputStream out ) throws Exception {
      String catName = getCatName();
      out.writeField("catName", catName);
      out.writeField("mode", "mocfile");
      out.writeField("format", "votable");
      String limit = getLimit();
      if ( ! limit.equals("unlimited")) {
          limit = limit.replaceAll(",", "");
          out.writeField("limit", limit);
      }
   }
   
   protected void addUpload( MultiPartPostOutputStream out, Plan plan ) throws Exception {
      HealpixMoc hpxMoc = ((PlanMoc)plan).getMoc();
      File tmp = File.createTempFile("tmp", "fits");
      tmp.deleteOnExit();
      FileOutputStream fo = new FileOutputStream(tmp);
      try { hpxMoc.writeFits(fo); }
      finally { try { fo.close(); } catch(Exception e) {} }
      out.writeFile("mocfile", null, tmp, false);
   }
   
  /** Soumission du formulaire */
   public void submit() {
      new Thread() {
         public void run() { submitThread(); }
      }.start();
   }
}
