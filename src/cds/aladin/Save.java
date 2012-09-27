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

import cds.tools.Util;
import cds.tools.pixtools.CDSHealpix;
import cds.xml.*;
import cds.fits.Fits;
import cds.fits.HeaderFits;
import cds.image.*;
import cds.moc.HealpixMoc;

import healpix.core.HealpixIndex;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.zip.CRC32;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;

/**
 * Gestion de la fenetre des sauvegardes
 * RQ: Uniquement necessaire pour le standalone
 *
 * @author Pierre Fernique [CDS]
 * @version 1.6 : mars 2007 - sauvegarde EPS de la vue
 * @version 1.5 : f�vrier 2006 - sauvegarde des images FITS couleurs
 * @version 1.4 : octobre 2005 - prise en compte d'un InputStream pour supporter VOApp.getFITS()
 * @version 1.3 : juillet 2005 - support pour la sauvegarde des images r��chantillonn�es
 * @version 1.2 : mars 2002 - get links ajoute
 * @version 1.1 : 22 fevrier 2002 - Cas des backup des plans RGB
 * @version 1.1 : 6 fevrier 2001 - BMP a la place de FITS pour le saveView
 * @version 1.1 : 11 avril 2000 - contournement "out of memory"
 * @version 1.0 : (26 nov 99) creation
 */
public final class Save extends JFrame implements ActionListener {

   static final String CR = Util.CR;

   static final String FORMAT[] = { "BMP","EPS","JPEG","PNG","PNG+LINK" };

   static final int BMP =1;
   static final int EPS=2;
   static final int JPEG=4;
   static final int PNG=8;
   static final int LK=16;
   static final int LK_FLEX=32;

   /** Retourne la liste des formats support�s pour le save View */
   static protected String [] getFormat() { return FORMAT; }

   /** Retourne la liste des formats supportes (except� EPS)
    * en pr�fixant par '%' pour pouvoir �tre int�gr� dans le menu
    * g�n�ral d'Aladin (voir Aladin.createJBar()).
    */
   static protected String[] getFormatMenu() {
      String [] res = new String[FORMAT.length-1];
      for( int i=0,j=0; i<FORMAT.length; i++ ) {
         if( FORMAT[i].equals("EPS") ) continue;   // Un menu � part
         res[j++] = "%"+FORMAT[i];
      }
      return res;
   }

   /** Retourne le code binaire du format correspondant au menu d�roulant de choix */
   static protected int getCodedFormat(int n) {
      if( n<4 ) return (int)Math.pow(2,n);
      return PNG|LK;
   }

   static final int CURRENTVIEW = 0;
   static final int ALLVIEWS = 1;
   static final int ALLROIS = 2;

   // Les references aux objets
   Aladin aladin;
   JPanel p;

   static String [] INFOCHOICE;
   static String [] CHOICE;

   static String TITLE,INFO,FISTINFO,SECONDINFO,CLOSE,EXPORT,DIR,
                 CANNOT,CANNOT1,INFOIMG,SAVEIN,SAVERGBIN,ENCODER;

   // Memorisation temporaire
   JTextField directory;
   JTextField [] fileSavePlan;
   Plan [] listPlan;
   int nbSavePlan;
   JComboBox format;
   JCheckBox [] cbPlan;
   JRadioButton tsvCb, votCb;
   JRadioButton fitsCb, jpgCb, pngCb;
   String errorFile=null;

   boolean first=true;

   protected void createChaine() {
      TITLE = aladin.chaine.getString("SFTITLE");
      INFO = aladin.chaine.getString("SFINFO");
      SECONDINFO = aladin.chaine.getString("SFSECONDINFO");
      CLOSE = aladin.chaine.getString("CLOSE");
      EXPORT = aladin.chaine.getString("SFEXPORT");
      DIR = aladin.chaine.getString("SFDIR");
      CANNOT = aladin.chaine.getString("SFCANNOT");
      CANNOT1 = aladin.chaine.getString("SFCANNOT1");
      INFOIMG = aladin.chaine.getString("SFINFOIMG");
      SAVEIN = aladin.chaine.getString("SFSAVEIN");
      SAVERGBIN = aladin.chaine.getString("SFSAVERGBIN");
      ENCODER = aladin.chaine.getString("SFJPEGENCODER");

      CHOICE = new String[] {
            aladin.chaine.getString("SFCH5"),
            aladin.chaine.getString("SFCH3"),
            aladin.chaine.getString("SFCH1"),
            aladin.chaine.getString("SFCH2"),
            aladin.chaine.getString("SFCH4"),
      };

      INFOCHOICE = new String[] {
            aladin.chaine.getString("SFHCH5"),
            aladin.chaine.getString("SFHCH3"),
            aladin.chaine.getString("SFHCH1"),
            aladin.chaine.getString("SFHCH2"),
            aladin.chaine.getString("SFHCH4"),
      };

   }


  /** Creation du Frame donnant les proprietes du plan.
   * @param aladin Reference
   * @param show true s'il faut afficher le Canvas
   */
   protected Save(Aladin aladin) { this(aladin,false); }
   protected Save(Aladin aladin,boolean show) {
      super();
      this.aladin = aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(TITLE);
      Aladin.makeAdd(this,p=getFirstPanel(),"Center");
      ((JPanel)getContentPane()).setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      Util.setCloseShortcut(this, false, aladin);

      setLocation( Aladin.computeLocation(this) );

      pack();
      if( show ) show();
   }


   static final int SAVEVIEW = 1;


  /** Donne le panel de la fenetre de choix (save, export planes,
   * ou export current view
   */
   private JPanel getFirstPanel() {

      // JPanel des boutons de choix de sauvegarde et d'exportation
      JPanel b = new JPanel();
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.BOTH;
      c.anchor = GridBagConstraints.EAST;

      b.setLayout(g);
      b.setFont(Aladin.BOLD);
      for( int i=0; i<CHOICE.length; i++ ) {
         MyButton ml = new MyButton(aladin,CHOICE[i]);
         ml.setRond();
         c.gridwidth = GridBagConstraints.RELATIVE;
         c.insets = new Insets(4,5,0,3);
         g.setConstraints(ml,c);
         b.add(ml);

         if( i==SAVEVIEW ) {
            JPanel p = new JPanel();
            MyLabel l = new MyLabel(INFOCHOICE[i],Label.LEFT,Aladin.PLAIN);
            p.add(l);
            format =new JComboBox();
            for( int j=0; j<FORMAT.length; j++ ) format.addItem(FORMAT[j]+" format");
            format.setSelectedIndex(1);   /* EPS */
            p.add(format);
            c.gridwidth = GridBagConstraints.REMAINDER;
            g.setConstraints(p,c);
            b.add(p);
         } else {
            MyLabel l = new MyLabel(INFOCHOICE[i],Label.LEFT,Aladin.PLAIN);
            c.gridwidth = GridBagConstraints.REMAINDER;
            g.setConstraints(l,c);
            b.add(l);
         }
      }


      // JPanel qui contient les infos, les boutons et le cancel
      JPanel p = new JPanel();
      p.setLayout( new BorderLayout(5,5));
      MyLabel info = new MyLabel(INFO,Label.CENTER,Aladin.BOLD);
      Aladin.makeAdd(p,info,"North");
      Aladin.makeAdd(p,b,"Center");
      JPanel c1 = new JPanel(); c1.setLayout( new BorderLayout());
      JButton bt = new JButton(CLOSE);
      bt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { hide(); }
      });
      Aladin.makeAdd(c1, bt,"East");
      Aladin.makeAdd(p,c1,"South");


      return p;
   }


   private JFrame frameExport;

  /** Sauvegarde des plans individuellement */
   protected void exportPlans() {
      frameExport = new JFrame();

      Aladin.makeAdd(frameExport,new MyLabel(SECONDINFO,Label.CENTER,Aladin.BOLD),"North");

      // J'ajoute une scrollbar
      JPanel p = new JPanel();
      p.setLayout( new GridLayout(1,1));
      JPanel p1 = getPlanPanel();
      if( p1==null ) return;
      JScrollPane scroll = new JScrollPane(p1);
      int h = Math.min(400,aladin.calque.getNbUsedPlans()*50+30);
      scroll.setMaximumSize(new Dimension(200,h));
      p.add(scroll);
      Aladin.makeAdd(frameExport,p,"Center");

      // Le panel contenant le nom du repertoire et les boutons
      JPanel bas = new JPanel();
      bas.setLayout( new BorderLayout(5,5));
      JPanel basg = new JPanel();

      // Le r�pertoire par d�faut
      directory = new JTextField(aladin.getDefaultDirectory(),30);
      basg.add(new JLabel(DIR));
      basg.add(directory);

      Aladin.makeAdd(bas,basg,"West");
      JPanel basd = new JPanel();
      basd.setLayout( new FlowLayout(FlowLayout.CENTER));
      JButton bt = new JButton(EXPORT);
      bt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { actionExportPlans(); }
      });
      basd.add(bt);
      bt = new JButton(CLOSE);
      bt.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { frameExportClose(); }
      });
      basd.add(bt);
      Aladin.makeAdd(bas,basd,"Center");
      Aladin.makeAdd(frameExport,bas,"South");
      frameExport.pack();
      Point pos = getLocation(); pos.translate(20,40);
      frameExport.setLocation(pos);
      frameExport.setVisible(true);
   }

   /** Ferme la fen�tre pour l'exportation des plans */
   private void frameExportClose() { frameExport.dispose(); }

   // Afin de n'afficher qu'une seule fois le message de warning
   // sur les sauvegardes FITS
   boolean firstFlagFits=true;

  /** Aiguillage de sauvegarde */
   protected void actionExportPlans() {
      boolean res=true;	// Resultat final des sauvegardes
      boolean flagFits=false;	// True si on a sauvegarde au-moins 1 FITS
      File f;
      String s;
      errorFile="";

      for( int i=0; i<nbSavePlan; i++ ) {
         if( !cbPlan[i].isSelected() ) continue;
         Plan p = listPlan[i];
         f = new File(directory.getText(),fileSavePlan[i].getText());
         aladin.console.setCommand("export "+Tok.quote(p.label)+" "+f.getAbsolutePath());
         switch( p.type ) {
            case Plan.ALLSKYMOC:
               s = directory.getText()+Util.FS+fileSavePlan[i].getText();
               res &= saveMoc(s,(PlanMoc)p);
               break;
            case Plan.TOOL:
               res&= (tsvCb!=null && tsvCb.isSelected()) || !p.isCatalog() ? saveToolTSV(f,p) : saveCatVOTable(f,p,false);
               break;
            case Plan.ALLSKYCAT:
            case Plan.CATALOG:
               res&=saveCatalog(f,p,tsvCb.isSelected());
               break;
            case Plan.IMAGE:
            case Plan.IMAGERSP:
            case Plan.IMAGEALGO:
            case Plan.IMAGERGB:
            case Plan.IMAGEMOSAIC:
               s = directory.getText()+Util.FS+fileSavePlan[i].getText();
               res&=saveImage(s,p,pngCb!=null && pngCb.isSelected() ? 3 :
                                  jpgCb!=null && jpgCb.isSelected() ? 2 : 0 );
               break;
         }
      }

      if( !res ) {
         Aladin.warning(this,CANNOT+"\n "+errorFile,1);
      } else {
         setVisible(false);
         if( flagFits && firstFlagFits ) { firstFlagFits=false; Aladin.info(this,INFOIMG); }
         frameExportClose();
      }

      aladin.memoDefaultDirectory(directory.getText());
   }

  /** Construction du panel de sauvegarde des plans */
   protected JPanel getPlanPanel() {
      int i,j=0;
      GridBagConstraints c = new GridBagConstraints();
      GridBagLayout g =  new GridBagLayout();
      c.fill = GridBagConstraints.NONE;    // J'agrandirai les composantes
      c.anchor = GridBagConstraints.WEST;  // Ancres a gauche
      c.insets = new Insets(0,2,0,2);

      JPanel p = new JPanel();
      p.setLayout(g);

      int nb = aladin.calque.getNbUsedPlans();
      fileSavePlan = new JTextField[nb];
      cbPlan = new JCheckBox[nb];
      listPlan = new Plan [nb];
      boolean noCatalog = true;
      boolean noImage = true;

      Plan [] allPlan = aladin.calque.getPlans();
      for( i=0; i<allPlan.length; i++ ) {
         Plan pl =allPlan[i];
         if( pl.type==Plan.NO || pl.type==Plan.APERTURE  || pl.type==Plan.FOLDER || !pl.flagOk ) continue;
         if( pl instanceof PlanBG && pl.type!=Plan.ALLSKYMOC ) continue;
         if( pl.isSimpleCatalog() && noCatalog ) noCatalog = false;
         if( pl.isImage() && noImage) noImage = false;
         listPlan[j]=pl;
         cbPlan[j] = new JCheckBox(j+".- ",pl.selected);
//         String s = pl.label+((pl.objet!=null)?"-"+pl.objet:"");
         String s = pl.label;
         JLabel name = new JLabel(s);
         name.setForeground(pl.c);
         JLabel type = new JLabel(Plan.Tp[pl.type]);
         String file = ServerAladin.blankToUnderline(s);
         while( file.charAt(0)=='.' ) file=file.substring(1); // J'enl�ve les "." en pr�fixe
         file=file.replace('\\','-');
         file=file.replace('/','-');
         file=file.replace(':','-');
         file=file.replace('[','-');
         file=file.replace(']','-');
         if( pl.isImage() || pl.type==Plan.ALLSKYMOC ) {
            if( !file.endsWith(".fits") ) file=file+".fits";
         } else file=file+".txt";
         fileSavePlan[j] = new JTextField(file,20);
         c.gridwidth = 2;
         c.anchor = GridBagConstraints.EAST;
         g.setConstraints(cbPlan[j],c); p.add(cbPlan[j]);
         c.gridwidth = 1;
         c.anchor = GridBagConstraints.WEST;
         g.setConstraints(name,c); p.add(name);
         c.gridwidth = 1;
         g.setConstraints(fileSavePlan[j],c); p.add(fileSavePlan[j]);
         c.gridwidth = GridBagConstraints.REMAINDER;
         g.setConstraints(type,c); p.add(type);
         j++;
      }
      
      if( j==0 ) {
         aladin.warning(this,"There is no available plan to export !");
         return null;
      }

      // s'il y a au moins un plan catalogue
      if( !noCatalog ) {
          c.gridwidth = 2;
          c.anchor = GridBagConstraints.EAST;
          JLabel nil = new JLabel("");
          g.setConstraints(nil,c); p.add(nil);

          c.gridwidth = 1;
          c.anchor = GridBagConstraints.WEST;
          JLabel l = new JLabel(SAVEIN);
          l.setFont(Aladin.BOLD);
          g.setConstraints(l,c); p.add(l);

          JPanel pFormat = new JPanel();
          pFormat.setLayout(new FlowLayout(FlowLayout.LEFT));
          ButtonGroup cg = new ButtonGroup();
          tsvCb = new JRadioButton("TSV");      tsvCb.setActionCommand("TSV");
          votCb = new JRadioButton("VOTABLE");  votCb.setActionCommand("VOTABLE");
          cg.add(tsvCb); cg.add(votCb); tsvCb.setSelected(true);
          pFormat.add(tsvCb);
          pFormat.add(votCb);
          tsvCb.addActionListener(this);
          votCb.addActionListener(this);
          c.gridwidth = 1;
          if( !noImage ) c.gridwidth = GridBagConstraints.REMAINDER;
          g.setConstraints(pFormat,c); p.add(pFormat);
      }

      // s'il y a au moins une image
      if( !noImage ) {
          c.gridwidth = 2;
          c.anchor = GridBagConstraints.EAST;
          JLabel nil = new JLabel("");
          g.setConstraints(nil,c); p.add(nil);

          c.gridwidth = 1;
          c.anchor = GridBagConstraints.WEST;
          JLabel l = new JLabel(SAVERGBIN);
          l.setFont(Aladin.BOLD);
          g.setConstraints(l,c); p.add(l);

          JPanel pFormat = new JPanel();
          pFormat.setLayout(new FlowLayout(FlowLayout.LEFT));
          ButtonGroup cg = new ButtonGroup();
          fitsCb = new JRadioButton("FITS"); fitsCb.setActionCommand("FITS");
          jpgCb = new JRadioButton("JPEG");  jpgCb.setActionCommand("JPEG");
          pngCb = new JRadioButton("PNG");   pngCb.setActionCommand("PNG");
          cg.add(fitsCb); cg.add(jpgCb); cg.add(pngCb); fitsCb.setSelected(true);
          pFormat.add(fitsCb);
          pFormat.add(jpgCb);
          pFormat.add(pngCb);
          fitsCb.addActionListener(this);
          jpgCb.addActionListener(this);
          pngCb.addActionListener(this);
          c.gridwidth = 1;
          c.insets.bottom = c.insets.top = 2;
          g.setConstraints(pFormat,c); p.add(pFormat);
      }

      nbSavePlan=j;

      return p;
   }

  /** Backup ou Save view
   * @param mode 0-backup, 1-view
   * @param format BMP, JPEG, PNG, EPS, PNG|LK (ignor� si mode=0)
   */
   protected void saveFile(int mode) { saveFile(mode,BMP,-1); }
   protected void saveFile(int mode,int format,float qual) {
      boolean res;
      FileDialog fd = new FileDialog(this,"",FileDialog.SAVE);
      aladin.setDefaultDirectory(fd);
      fd.setVisible(true);
      aladin.memoDefaultDirectory(fd);
      String dir = fd.getDirectory();
      String name =  fd.getFile();
      if( name==null ) return;
      String s = (dir==null?"":dir)+(name==null?"":name);
      if( mode==0 ) {
         if( !Util.toLower(s).endsWith(".aj") ) s=s+".aj";
         aladin.console.setCommand("backup "+s);
         res=saveAJ(s);
         if( res ) aladin.log("backup","");
      } else {
         String ext = format==BMP ? ".bmp" : format==JPEG ? ".jpg" :
                     (format&PNG)==PNG ? ".png" : ".eps";
         if( !(Util.toLower(s).endsWith(".jpeg") && format==JPEG) &&
             !Util.toLower(s).endsWith(ext) ) s=s+ext;

         String lk = (format&LK)==LK ? " -lk":"";
         aladin.console.setCommand("save "+lk+s);
         res=saveView(s,0,0,format,qual);
         if( res ) aladin.log("save",Util.toUpper(ext.substring(1)+lk));
      }
      if( !res ) {
         Aladin.warning(this,CANNOT+"\n"+s+"\n"+CANNOT1,1);
      } else setVisible(false);
   }

   // Elements pour gerer le buffer de sauvegarde
   static private int MAXBUF=8192;
   static private byte buf[] = new byte[MAXBUF];
   static private int nbuf=0;
   static private FileOutputStream f;

  /** Prepare le buffer de sauvegarde */
   static private void open(File file) throws java.io.IOException {
      f = new FileOutputStream(file);
      nbuf=0;
   }

  /** flush de la sauvegarde courante */
   static private void flush() throws java.io.IOException {
      f.write(buf,0,nbuf);
      nbuf=0;
   }

  /** Prepare le buffer de sauvegarde */
   static private void close() throws java.io.IOException {
      flush();
      f.close();
   }

  /** Sauvegarde avec bufferisation minimale */
   static private void append(String s) throws java.io.IOException {
      append(s.toCharArray());
   }

  /** Sauvegarde avec bufferisation minimale */
   static private void append(char a[]) throws java.io.IOException {
      for( int i=0; i<a.length; i++ ) {
         buf[nbuf++]=(byte) a[i];
         if( nbuf==MAXBUF ) flush();
      }
   }

  /** Sauvegarde d'Aladin */
   protected boolean saveAJ(String filename) {
      filename = aladin.getFullFileName(filename);
      File file = new File(filename);
      Plan [] allPlan = aladin.calque.getPlans();
      int i;


      try {

         file.delete();
         open(file);	// Creation du buffer de sauvegarde

         // Entete du fichier de sauvegarde
         append("<?xml version = \"1.0\"?>\n"+
                "<!-- This file has been produced by the Aladin Java interface,"+CR+
                "     Please do not modify it -->"+CR+CR+
                "<ALADINJAVA vers=\"1.0\">"+CR);


         // Generation XML pour chaque plan
         for( i=allPlan.length-1; i>=0; i-- ) {
            Plan p = allPlan[i];
            if( !p.isReady() ) continue;

            switch(p.type) {
               case Plan.FILTER:  appendPlanFilterXML(p);  break;
               case Plan.FOLDER:  appendPlanFolderXML(p);  break;
               case Plan.CATALOG: appendPlanCatalogXML(p); break;
               case Plan.TOOL:    appendPlanToolXML(p);    break;
               case Plan.IMAGERSP:
               case Plan.IMAGEALGO:
               case Plan.IMAGEMOSAIC:
               case Plan.IMAGE:   appendPlanImageXML(p);   break;
               case Plan.IMAGERGB:appendPlanImageRGBXML(p);break;
            }

         }

         // Le positionnement des vues
         // @todo A POURSUIVRE DES QUE J'AURAIS LE TEMPS (voir le complement dans LocalServer
         aladin.view.sauvegarde();
         append("  <MODEVIEW");
         append(CR+"    overlays=\""+aladin.calque.getOverlayList()+"\"");
         append(CR+"    overlay=\""+aladin.calque.flagOverlay+"\"");
//         append(CR+"    grid=\""+aladin.calque.hasGrid()+"\"");
//         append(CR+"    target=\""+aladin.calque.hasTarget()+"\"");
         append(CR+"    mode=\""+aladin.view.getModeView()+"\"");
         append(CR+"    position=\""+aladin.view.getScrollValue()+"\">"+CR);

         for( int j=0; j<aladin.view.viewMemo.size(); j++) {
            if( aladin.view.viewMemo.memo[j]==null ) continue;
            if( aladin.view.viewMemo.memo[j].pref==null ) continue;
            append("    <VIEW n=\""+j+"\"");
            appendView(aladin.view.viewMemo.memo[j]);
            append(">"+CR+"    </VIEW>"+CR);
         }

         append("  </MODEVIEW>"+CR);

         // Pied du fichier de sauvegarde
         append("</ALADINJAVA>"+CR);

         // fermeture
         close();

      }catch(Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         aladin.warning(this,CANNOT+"\n"+file+"\n--> "+e,1);
         return false;
      }
      return true;

   }


   private void appendView(ViewMemoItem m) {
      try {
         append(CR+"       zoom=\""+m.zoom+"\"");
         append(CR+"       xzoomView=\""+m.xzoomView+"\"");
         append(CR+"       yzoomView=\""+m.yzoomView+"\"");
         append(CR+"       rzoomWidth=\""+m.rzoomWidth+"\"");
         append(CR+"       rzoomHeight=\""+m.rzoomHeight+"\"");
         append(CR+"       rvWidth=\""+m.rvWidth+"\"");
         append(CR+"       rvHeight=\""+m.rvHeight+"\"");
         append(CR+"       pref=\""+m.pref.label+"\"");
         append(CR+"       locked=\""+m.locked+"\"");
         append(CR+"       northUp=\""+m.northUp+"\"");
//         append(CR+"       sync=\""+m.sync+"\"");
      } catch( Exception e ) {}
   }

   /** appel� lorsque l'utilisateur a modifie le format de sauvegarde des cats :
    * on modifie le suffixe des noms de fichiers
    */
   private void changeCatFormat() {
      boolean tsv = tsvCb.isSelected();
      String newSuffix = tsv?".txt":".xml";
      String oldSuffix = tsv?".xml":".txt";

      for( int i=0; i<listPlan.length; i++ ) {
         Plan p =listPlan[i];
         if( p==null || !p.isCatalog() ) continue;
         String label = fileSavePlan[i].getText();
         if( !label.endsWith(oldSuffix) ) continue;
         fileSavePlan[i].setText(label.substring(0, label.length()-4)+newSuffix);
      }
   }
   
   // les extensions � tester dans le cas d'une substitution */
   static final private String [] EXTENSIONS = { ".png",".jpg",".jpeg",".fits",".fit" };

   /** appel� lorsque l'utilisateur a modifie le format de sauvegarde des image :
    * on modifie le suffixe des noms de fichiers */
   private void changeImgFormat() {
      String newSuffix = fitsCb.isSelected()?".fits": jpgCb.isSelected()?".jpg" : ".png";
      for( int i=0; i<listPlan.length; i++ ) {
         Plan p =listPlan[i];
         if( p==null || !p.isImage() ) continue;
         String label = fileSavePlan[i].getText();
         int offset = label.lastIndexOf('.');
         if( offset>0 ) {
            String oldSuffix = label.substring(offset);
            if( Util.indexInArrayOf(oldSuffix, EXTENSIONS, false)>=0 ) label= label.substring(0,offset);
         }
         fileSavePlan[i].setText(label+newSuffix);
      }
   }

   /** Sauvegarde du plan Image RGB p sous forme XML (utilise le buffer f) */
   private void appendPlanImageRGBXML(Plan p) throws java.io.IOException {
      appendXMLHeadPlan(p);         // Entete du plan courant
      append("    <VALUE><![CDATA["+CR);    // Debut de l'image
      append64( ((PlanImageRGB)p).getByteRGB() );
      append("]]></VALUE>"+CR+"  </PLANE>"+CR); // Fin de l'image et du plan
   }

   /** Sauvegarde du plan Image p sous forme XML (utilise le buffer f) */
   private void appendPlanImageXML(Plan p) throws java.io.IOException {
      appendXMLHeadPlan(p);         // Entete du plan courant
      append("    <VALUE><![CDATA["+CR);    // Debut de l'image
      append64( ((PlanImage)p).getBufPixels8() );
      append("]]></VALUE>"+CR+"  </PLANE>"+CR); // Fin de l'image et du plan
   }

   static private String B64 =
    "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

   static int [] b642a=null;

  /** Decodage d'une image en base 64. - Merci Fox pour le code
   *  Le traitement peut etre en fait en plusieurs fois.
   * @param b[] le tableau des pixels en sortie
   * @param k   l'offset dans le tableau b[]
   * @param a[] les caracteres base 64 a traiter
   * @param start la position de depart dans a[]
   * @param length le nombre de caracteres a traiter dans a[]
   * @return la prochaine position a remplir dans b[]
   */
   public static int get64(byte [] b, int k,
         char [] a, int start, int length) {
      char [] tab = B64.toCharArray();
      int  c, c3, i,j, colno, lineno;
      boolean skip_line;
      int size=b.length;

      /* Initilize b642a */
      if( b642a==null ) {
         b642a = new int[256];
         for( i=0; i<b642a.length; i++ ) b642a[i]=0x40;
         for (i=0; i<tab.length; i++) b642a[tab[i]] = i ;
         b642a['='] = 0xff ;
      }

      colno = 0;
      skip_line = false;
      lineno = 1;
      j=start;
      while( j<length ) {
         c = a[j++];
         colno++;
         if( skip_line ) System.err.print(c);
         if( c==' ' || c=='\t' || c=='\n' || c=='\r') {
            if( c=='\n' || c=='\r') { lineno++; colno=0; skip_line=false; }
            continue;
         }
         if( skip_line ) continue;
         c3 = b642a[c&0xff];
         if( (c3&0x40)!=0 ) {
            if( colno==1 ) {
               skip_line = true;
               System.err.println("++++Ignore line: "+c);
               continue;
            }
            System.err.println("****Bad input char "+((char)c)+
                  " line "+lineno+", col "+colno);
            continue;
         }
         c3 <<= 6;

         c = (a[j++]) & 0xff;
         colno++ ;
         i = b642a[c&0xff];
         if( (i&0x40)!=0 ) {
            System.err.println("****Bad input char "+((char)c)+
                  " line "+lineno+", col "+colno);
            c3 >>= 4;

         if( k>=size ) return k;
         b[k++]=(byte)c3;
         continue;
         }
         c3 |= i;
         c3 <<= 6;

         c = (a[j++]) & 0xff;
         colno++ ;
         i = b642a[c&0xff];
         if( (i&0x40)!=0 ) {		/* 2 characters to issue */
            if( i!=0xff ) System.err.println("****Bad input char "+((char)c)+
                  " line "+lineno+", col "+colno);
            c3 >>= 2;
         if( k>=size ) return k;
         b[k++]=(byte)(c3>>8);
         if( k>=size ) return k;
         b[k++]=(byte)c3;
         continue ;
         }
         c3 |= i;
         c3 <<= 6;

         c = (a[j++]) & 0xff;
         colno++ ;
         i = b642a[c&0xff] ;
         if( (i&0x40)!=0 && i!=0xff ) System.err.println("****Bad input char "+((char)c)+
               " line "+lineno+", col "+colno);
         else c3 |= i;
         if( k>=size ) return k;
         b[k++]=(byte)(c3>>16);
         if( k>=size ) return k;
         b[k++]=(byte)(c3>>8);
         if( k>=size ) return k;
         b[k++]=(byte)c3;
      }
      return k;
   }

   /** Sauvegarde en base 64 */
   private void append64(byte [] p) throws java.io.IOException {

      char [] tab = B64.toCharArray();
      char [] b4 = new char[4];
      int c, c3, nb;
      int i=0;

      nb = 0;
      while( i<p.length ) {
         c = p[i++]&0xff;
         c3 = c<<16;
         b4[2] = b4[3] = '=';

         if( i<p.length ) {
            c = p[i++]&0xff;
            c3 |= (c<<8);
            b4[2]=0;
            if( i<p.length ) {
               c = p[i++]&0xff;
               c3 |=  c;
               b4[3]=0;
            }
         }
         if( b4[3]==0 ) b4[3] = tab[c3&63];
         c3 >>= 6;
            if( b4[2]==0 ) b4[2] = tab[c3&63];
            c3 >>= 6;
            b4[1] = tab[c3&63];
            c3 >>= 6;
         b4[0] = tab[c3&63];
         append(b4);
         nb += 4;
         if( (nb%76)==0 ) append(CR);
      }
   }

  /** Sauvegarde du plan Catalog p sous forme XML (utilise le buffer f) */
   private void appendPlanCatalogXML(Plan p) throws java.io.IOException {
      appendXMLHeadPlan(p);
      appendPCatXML(p.pcat); 
      append("  </PLANE>"+CR);
   }
   
   /** Sauvegarde du contenu d'un plan Catalog ou des sources d'un paln Tool sous forme XML (utilise le buffer f) */
   private void appendPCatXML(Pcat pcat) throws java.io.IOException {
      Legende leg=null;
      
      // Parcours des objets
      Iterator<Obj> it = pcat.iterator();
      while( it.hasNext() ) {
         Obj o1 = it.next();
         if( !(o1 instanceof Source) ) continue;
         Source o = (Source)o1;

         // Nouvelle table dans le plan courant
         if( o.leg!=leg ) {
            if( leg!=null ) append( getXMLTailTable()); // fin de la table precedente
            append( getXMLHeadTable(o.leg) );           // Nouvelle table
            leg=o.leg;
         }

         // Ajout de la position RA,DEC en J2000 et de l'identificateur
         Position po = o;
         append(po.raj+"\t"+po.dej+"\t"+po.id);

         // Ajout des infos TSV
         append("\t"+o.info+CR);

      }

      // Fin de la derniere table
      if( pcat.hasObj() ) append(getXMLTailTable());
   }

   /** Sauvegarde du plan Filter p sous forme XML (utilise le buffer f) */
   protected void appendPlanFilterXML(Plan p) throws java.io.IOException {
      appendXMLHeadPlan(p);
      append("    <SCRIPT>"+CR);

      // thomas, 22/11/06 : je suis oblig� d'escaper les sauts de ligne
      // car XMLParser me les mange lorsqu'on recharge un filtre, et j'en ai besoin !
      // Je vais donc devoir les d�coder � ce moment l�
      append( XMLParser.XMLEncode(((PlanFilter)p).script.replaceAll("\n", "\\\\n"))+CR);
      append("    </SCRIPT>"+CR);
      append("  </PLANE>"+CR);
   }


   /** Sauvegarde du plan Folder p sous forme XML (utilise le buffer f) */
   protected void appendPlanFolderXML(Plan p) throws java.io.IOException {
      appendXMLHeadPlan(p);
      append("  </PLANE>"+CR);
   }


  /** Sauvegarde du plan tool p sous forme XML (utilise le buffer f)*/
   protected void appendPlanToolXML(Plan p) throws java.io.IOException {
      boolean flagSource=false;
      
      // Entete du plan courant
      appendXMLHeadPlan(p);

      // Debut de la table
      append("    <TABLE><VALUE><![CDATA["+CR);

      // Parcours des objets tools classiques
      Iterator<Obj> it = p.pcat.iterator();
      while( it.hasNext() ) {
         Position o = (Position)it.next();

         if( o instanceof Source ) { flagSource=true; continue; }
         
         // Beurk - PF nov 2010
         if (o instanceof Cercle) {
             Cercle c = (Cercle)o;
             append(getInstance(c)+"\t"+"."+"\t"+c.o[0].raj+"\t"+c.o[0].dej+
                    "\t"+c.o[0].x+"\t"+c.o[0].y+"\t"+c.isWithLabel()+
                    "\t"+c.getSpecificAJInfo()+CR);
             append(getInstance(c)+"\t"+"+"+"\t"+c.o[1].raj+"\t"+c.o[1].dej+
                     "\t"+c.o[1].x+"\t"+c.o[1].y+"\t"+c.isWithLabel()+
                     "\t"+c.getSpecificAJInfo()+CR);
             
         } else append(getInstance(o)+"\t"+suite(o)+
                  "\t"+o.raj+"\t"+o.dej+"\t"+o.x+"\t"+o.y+"\t"+o.isWithLabel()+
                  "\t"+o.getSpecificAJInfo()+CR);
      }

      // Fin de la table concernant les objets classques
      append(getXMLTailTable());
      
      // Dans le cas o� il y a des Source (outil PHOT)
      if( flagSource) appendPCatXML(p.pcat);

      // Fin du plan
      append("  </PLANE>"+CR);

   }

  /** Retourne le nom de l'objet graphique */
   protected String getToolName(Obj o) { return "Undefined"; }

  /** Retourne la fin XML d'une table */
   protected String getXMLTailTable() {
      return "]]></VALUE></TABLE>"+CR;
   }

  /** retourne l'entete XML d'une table */
   protected String getXMLHeadTable(Legende leg) {
      int i;
      StringBuffer s = new StringBuffer();

      // Entete de la table
      s.append("    <TABLE>"+CR);

      // Legende
      for( i=0; i<leg.field.length; i++ ) {
         Field f=leg.field[i];
         s.append("      <COLUMN");
         if( f.name!=null )        s.append(CR+"         name=\""+XMLParser.XMLEncode(f.name)+"\"");
         if( f.description!=null ) s.append(CR+"         description=\""+XMLParser.XMLEncode(f.description)+"\"");
         if( f.href!=null )        s.append(CR+"         href=\""+XMLParser.XMLEncode(f.href)+"\"");
         if( f.gref!=null )        s.append(CR+"         gref=\""+XMLParser.XMLEncode(f.gref)+"\"");
         if( f.ucd!=null )         s.append(CR+"         ucd=\""+XMLParser.XMLEncode(f.ucd)+"\"");
         if( f.unit!=null )        s.append(CR+"         unit=\""+XMLParser.XMLEncode(f.unit)+"\"");
         if( f.width!=null )       s.append(CR+"         width=\""+XMLParser.XMLEncode(f.width)+"\"");
         if( f.arraysize!=null )   s.append(CR+"         arraysize=\""+XMLParser.XMLEncode(f.arraysize)+"\"");
         if( f.precision!=null )   s.append(CR+"         precision=\""+XMLParser.XMLEncode(f.precision)+"\"");
         if( f.utype!=null )       s.append(CR+"         arraysize=\""+XMLParser.XMLEncode(f.utype)+"\"");
         if( f.type!=null )        s.append(CR+"         type=\""+XMLParser.XMLEncode(f.type)+"\"");
         if( f.datatype!=null )    s.append(CR+"         datatype=\""+XMLParser.XMLEncode(f.datatype)+"\"");
         if( f.refText!=null )     s.append(CR+"         refText=\""+XMLParser.XMLEncode(f.refText)+"\"");
         if( f.refValue!=null )    s.append(CR+"         refValue=\""+XMLParser.XMLEncode(f.refValue)+"\"");
         s.append("/>"+CR);
      }

      // Debut des donnees
      s.append("      <VALUE><![CDATA["+CR);

      return s.toString();
   }

  /** retourne l'entete XML d'un plan */
   private void appendXMLHeadPlan(Plan p) throws java.io.IOException {
       int i;

      append("  <PLANE");
      append(CR+"     type=\""+XMLParser.XMLEncode(Plan.Tp[p.type])+"\"");
      append(CR+"     depth=\""+p.folder+"\"");
      append(CR+"     activated=\""+p.active+"\"");
      if( p.label!=null )      append(CR+"     label=\""+XMLParser.XMLEncode(p.label)+"\"");
      if( p.objet!=null )      append(CR+"     object=\""+XMLParser.XMLEncode(p.objet)+"\"");
      if( p.param!=null )      append(CR+"     param=\""+XMLParser.XMLEncode(p.param)+"\"");
      if( p.type==Plan.FILTER )
         if( ((PlanFilter)p).plan!=null ) append(CR+"     dedicatedto=\""+((PlanFilter)p).plan.label+"\"");
      if( p.type==Plan.FOLDER )
         append(CR+"     localscope=\""+((PlanFolder)p).localScope+"\"");
      if( !p.isImage() && p.type!=Plan.FOLDER && p.type!=Plan.FILTER ) {
         append(CR+"     color=\""+Action.findColorName(p.c)+"\"");
      }
      if( p.type==Plan.TOOL && ((PlanTool)p).withSource() ) append(CR+"     withsource=\"true\"");
      if( !p.isSelectable() )   append(CR+"     selectable=\"false\"");
      if( p.isImage() ) {
         PlanImage pi = (PlanImage)p;
         String fmt = PlanImage.getFormat(pi.fmt);
         String res = PlanImage.getResolution(pi.res);
         append(CR+"     fmt=\""+XMLParser.XMLEncode(fmt)+"\"");
         append(CR+"     resolution=\""+XMLParser.XMLEncode(res)+"\"");
      }
      if( p.from!=null ) append(CR+"     from=\""+XMLParser.XMLEncode(p.from)+"\"");
      if( p.u!=null )    append(CR+"     url=\""+XMLParser.XMLEncode(p.u+"")+"\"");
      if( Projection.isOk(p.projd) ) {
         append(CR+"     RA=\""+p.projd.raj+"\"");
         append(CR+"     DE=\""+p.projd.dej+"\"");
         append(CR+"     radius=\""+(p.projd.rm/2)+"\"");
         append(CR+"     proj=\""+p.projd.modeCalib+"\"");

         if( Projection.isOk(p.projd) ) {
            Calib c = p.projd.c;
            append(CR+"     calib=\""+c.aladin
                     +","+c.epoch
                     +","+c.alpha
                     +","+c.delta
                     +","+c.yz
                     +","+c.xz
                     +","+c.focale
                     +","+c.Xorg
                     +","+c.Yorg
                     +","+c.incX
                     +","+c.incY
                     +","+c.alphai
                     +","+c.deltai
                     +","+c.incA
                     +","+c.incD
                     +","+c.Xcen
                     +","+c.Ycen
                     +","+c.widtha
                     +","+c.widthd
                     +","+c.xnpix
                     +","+c.ynpix
                     +","+c.rota
                     +","+c.cdelz
                     +","+c.sdelz
                     +","+c.type1
                     +","+c.type2
                     +","+c.equinox
                     +","+c.proj
            +"\"");
            append(CR+"     projection=\""+Calib.getProjName(c.proj)+"\"");
            append(CR+"     system=\""+c.system+"\"");
            append(CR+"     flagepoch=\""+c.flagepoc+"\"");
            append(CR+"     adxpoly=\""+c.adxpoly[0]);
            for( i=1; i<10; i++ ) append(","+c.adxpoly[i]);
            append("\"");
            append(CR+"     adypoly=\""+c.adypoly[0]);
            for( i=1; i<10; i++ ) append(","+c.adypoly[i]);
            append("\"");
            append(CR+"     xyapoly=\""+c.xyapoly[0]);
            for( i=1; i<10; i++ ) append(","+c.xyapoly[i]);
            append("\"");
            append(CR+"     xydpoly=\""+c.xydpoly[0]);
            for( i=1; i<10; i++ ) append(","+c.xydpoly[i]);
            append("\"");
            append(CR+"     CD=\""+c.CD[0][0]+","+c.CD[0][1]+","
                                    +c.CD[1][0]+","+c.CD[1][1]+"\"");
            append(CR+"     ID=\""+c.ID[0][0]+","+c.ID[0][1]+","
                                    +c.ID[1][0]+","+c.ID[1][1]+"\"");

         }
      }

      if( p instanceof PlanImage ) {
         PlanImage pi = (PlanImage)p;
         append(CR+"     width=\""+pi.width+"\"");
         append(CR+"     height=\""+pi.height+"\"");
         append(CR+"     video=\""+pi.video+"\"");
         append(CR+"     transfertFct=\""+pi.transfertFct+"\"");
         append(CR+"     minPix=\""+pi.dataMin+"\"");
         append(CR+"     maxPix=\""+pi.dataMax+"\"");
         append(CR+"     minPixCut=\""+pi.pixelMin+"\"");
         append(CR+"     maxPixCut=\""+pi.pixelMax+"\"");
         append(CR+"     bZero=\""+pi.bZero+"\"");
         append(CR+"     bScale=\""+pi.bScale+"\"");
         append(CR+"     cm=\""+pi.typeCM+"\"");
         append(CR+"     colormap1=\""+pi.cmControl[0]+"\"");
         append(CR+"     colormap2=\""+pi.cmControl[1]+"\"");
         append(CR+"     colormap3=\""+pi.cmControl[2]+"\"");
         append(CR+"     bitpix=\""+pi.bitpix+"\"");
         append(CR+"     opacity=\""+pi.getOpacityLevel()+"\"");
         if( pi.cacheID!=null && pi.cacheOffset!=0L ) {
            append(CR+"     cacheID=\""+XMLParser.XMLEncode(pi.cacheID)+"\"");
            append(CR+"     cacheOffset=\""+pi.cacheOffset+"\"");
         }
         if( p instanceof PlanImageRGB ) {
            PlanImageRGB prgb = (PlanImageRGB)pi;
            for( int j=0; j<9; j++ ) {
               append(CR+"     RGBControl"+(j+1)+"=\""+prgb.RGBControl[j]+"\"");
            }
            if( prgb.planRed!=null && prgb.planRed.type!=Plan.NO )     append(CR+"     RGBRed=\""+prgb.planRed.label+"\"");
            if( prgb.planGreen!=null && prgb.planGreen.type!=Plan.NO ) append(CR+"     RGBGreen=\""+prgb.planGreen.label+"\"");
            if( prgb.planBlue!=null && prgb.planBlue.type!=Plan.NO )   append(CR+"     RGBBlue=\""+prgb.planBlue.label+"\"");
         }
      }
      append(" >"+CR);
      
      // Filtres pr�d�finis ?
      if( p.filters!=null ) {
         append("    <FILTERS filterIndex=\""+p.filterIndex+"\" nFilter=\""+p.filters.length+"\">"+CR);
         for( int j=0; j<p.filters.length; j++ ) {
            append("       <FILTER><![CDATA[");
            append(p.filters[j]+CR);
            append("]]></FILTER>"+CR);
         }
         append("    </FILTERS>"+CR);
      }

      if( p instanceof PlanImage && ((PlanImage)p).hasFitsHeader() ) {
         append("    <ORIRIGINALHEADERFITS>"+CR);
         append("    <![CDATA[");
         append( ((PlanImage)p).headerFits.getOriginalHeaderFits() );
         append("]]>"+CR);
         append("    </ORIRIGINALHEADERFITS>"+CR);
      }
   }

   protected boolean saveCatalog(String s,Plan p, boolean tsv, boolean addXY) {
      s=aladin.getFullFileName(s);
      File f = new File(s);
      if( tsv ) return saveCatTSV(f,p);
      else return saveCatVOTable(f,p,addXY);
   }


   protected boolean saveCatalog(File file, Plan p ,boolean tsv) {
       if( tsv ) return saveCatTSV(file,p);
       else return saveCatVOTable(file,p,false);
   }

   /** Ajout d'un suffixe en cas de multi-table en TSV */
   private File nextSuffixFile(File file,int n) {
      String f = file.getAbsolutePath();
      int i = f.lastIndexOf('.');
      if( i>=0 ) {
         int j = f.lastIndexOf('-',i);
         if( j>0 ) f = f.substring(0,j)+"-"+n+f.substring(i);
         else f = f.substring(0,i)+"-"+n+f.substring(i);
      } else f=f+"-"+n;
      return new File(f);
   }

   private String getInstance(Object o) {
      if( o instanceof Tag )  return "taglabel";
      if( o instanceof Cote )   return "arrow";
      if( o instanceof Ligne )  return "line";
      if( o instanceof Repere ) return "phot";
      if( o instanceof Source ) return "source";
      // if( o instanceof Pickle ) return "pickle";
      // if( o instanceof Arc )    return "arc";
      if( o instanceof Cercle ) return "circle";
      return "unknown";
   }

   private String suite(Obj o) {
      if( o instanceof Ligne ) {
         if( ((Ligne)o).finligne==null ) return ( ((Ligne)o).bout==3 ? "*" : ".");
         return "+";
      }
      return ".";
   }

   /**
    * Sauvegarde TSV d'un plan Tool
    * @param file le fichier dans lequel sauvegarder
    * @param p plan catalogue a sauvegarder en TSV
    * @�eturn boolean true si sauvegarde s'est deroulee correctement, false sinon
    */
   protected boolean saveToolTSV(File file,Plan p) {
      StringBuffer s = new StringBuffer(MAXBUF);
      Pcat pcat = p.pcat;
      FileOutputStream f=null;

      try{
         s.append("RAJ2000\tDEJ2000\tObject\tCont_Flag\tInfo"+CR);

         Iterator<Obj> it = pcat.iterator();
         int nb = pcat.getCounts();
         for( int i=0; i<=nb; i++ ) {
            if( i<nb ) {
               Position o = (Position)it.next();
               String info = "";
               if( o instanceof Source ) info = getInfo( (Source)o );
               else if( o.id!=null ) info=o.id;
               s.append(Util.myRound(o.raj+"",13)+"\t"+Util.myRound(o.dej+"",13)
                     +"\t"+getInstance(o)+"\t"+suite(o)+"\t"+info+CR);
            }
            // flush interm�diaire si n�cessaire, et final
            if( s.length()>MAXBUF-100 || i==nb) {
               f=writeByteTSV(f,file,0,s);
               s = new StringBuffer(MAXBUF);
            }
         }
      }catch(Exception e) {
         errorFile=errorFile+"\n"+file;
         return false;
      }

      aladin.log("export","tool TSV");

      return true;
   }


   /**
    * Sauvegarde TSV d'un plan catalogue
    * @param file le fichier dans lequel sauvegarder
    * @param p plan catalogue a sauvegarder en TSV
    * @param verbose true pour m�moriser dans le pad la commande script correspondante
    * @�eturn boolean true si sauvegarde s'est deroulee correctement, false sinon
    */
   protected boolean saveCatTSV(File file,Plan p) {
      StringBuffer s = new StringBuffer(MAXBUF);
      Pcat pcat = p.pcat;
      int nbTable=0;
      FileOutputStream f=null;

      try{
         Legende leg = ((PlanCatalog)p).getFirstLegende();
         s.append(getShortHeader(leg));

         int nb = pcat.getCounts();
         Iterator<Obj> it = pcat.iterator();
         for( int i=0; i<=nb; i++ ) {
            Source o = (Source)(i<nb?it.next():null);

            // Ecriture de la table courante (ou de sa fin)
            if( o==null || o.leg!=leg ) {
               f=writeByteTSV(f,file,nbTable,s);
               if( o==null ) {
                  f.close();
                  f=null;
               }
               nbTable++;

               // Recherche de la la l�gende de la prochaine table
               if( o!=null ) {
                  s = new StringBuffer(MAXBUF);
                  leg = (o).leg;
                  s.append(getShortHeader(leg));
               }
            }
            if( o!=null ) s.append(getTSV(o)+CR);

            // flush interm�diaire si n�cessaire
            if( s.length()>MAXBUF ) {
               f=writeByteTSV(f,file,nbTable,s);
               s = new StringBuffer(MAXBUF);
            }
         }
      }catch(Exception e) {
         errorFile=errorFile+"\n"+file;
         return false;
      }

      aladin.log("export","catalog TSV");

      return true;
   }

   /** Ecriture de s dans le fichier d�j� ouvert f ou qu'il faut ouvrir via
    * file et nbTable. Retourne le descripteur du fichier
    * @param f Descripteur de fichier si d�j� ouvert, sinon null
    * @param file File d�crivant le fichier � cr�er
    * @param nbTable num�ro de la table pour un �ventuel suffixe
    * @param s Le buffer � �crire
    * @return le descripteur du fichier
    * @throws Exception
    */
   private FileOutputStream writeByteTSV(FileOutputStream f, File file,
                 int nbTable, StringBuffer s) throws Exception {
      if( f==null ) {
         if( nbTable>0 ) file=nextSuffixFile(file,nbTable);
         file.delete();
         f = new FileOutputStream(file);
      }
      char [] a = s.toString().toCharArray();
      byte [] b = new byte[a.length];
      for( int j=0; j<a.length; j++ ) b[j] = (byte) a[j];
      f.write(b);
      return f;
   }

   /**
    * Sauvegarde VOTable d'un plan catalogue dans un fichier donne
    * @param file le fichier dans lequel on sauvegarde le plan
    * @param p le plan catalogue a sauvegarder en VOTable
    * @param addXY ajout (ou non) des positions (X,Y) -> utilis� par SimPlay g�n�rique
    * @return boolean true si la sauvegarde s'est bien effectuee, false sinon
    */
   protected boolean saveCatVOTable(File file, Plan p, boolean addXY ) {
       try {
          DataOutputStream out = new DataOutputStream(
             new BufferedOutputStream(new FileOutputStream(file)));
          aladin.writePlaneInVOTable(p, out, addXY);
          out.close();
       }
       catch(IOException ioe) {errorFile=errorFile+"\n"+file;return false;}
       aladin.log("export","catalog VOTABLE");
       return true;
   }

//   protected void JPEGwriter(Image img,OutputStream o) throws Exception {
//      BufferedImage bufferedImage = new BufferedImage(
//            img.getWidth(null),
//            img.getHeight(null),
//            BufferedImage.TYPE_INT_RGB );
//      Graphics g = bufferedImage.createGraphics();
//      g.drawImage(img,0,0,null);
//      g.dispose();
//
//      try {
//         com.sun.image.codec.jpeg.JPEGImageEncoder code
//               = com.sun.image.codec.jpeg.JPEGCodec.createJPEGEncoder(o);
//         JPEGEncodeParam p = code.getDefaultJPEGEncodeParam(bufferedImage);
//         p.setQuality((float)0.8,true);
//         code.setJPEGEncodeParam(p);
//         code.encode(bufferedImage);
//   } catch( Exception e ) {
//         throw new Exception(ENCODER);
//      }
//   }

   /** Il semblerait que le writer JPEG de java ne sache pas faire du GRAY !!
    * En clair, l'option !RGB est inutile pour le moment
    */
   protected void ImageWriter(Image img,String format, float qual, boolean RGB,OutputStream o) throws Exception {
      try {

         // Pour pouvoir g�rer la qualit� en JPEG
         if( format.equals("jpg") || format.equals("jpeg") ) writeJPEG(img,qual,RGB,o);

         // sinon m�thode classique par d�faut
         else {
            BufferedImage bufferedImage;
            if( img instanceof BufferedImage ) bufferedImage=(BufferedImage)img;
            else {
               bufferedImage = new BufferedImage(
                     img.getWidth(null),
                     img.getHeight(null),
                     RGB ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_BYTE_GRAY );
               Graphics g = bufferedImage.createGraphics();
               g.drawImage(img,0,0,aladin);
               g.dispose();
            }
            aladin.waitImage(bufferedImage);
            ImageIO.write(bufferedImage, format, o);
         }
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         throw new Exception(ENCODER);
      }
   }

   /** Ecriture d'une image en JPEG en g�rant la qualit�
    * @param img Image � �crire
    * @param RGB true s'il s'agit d'une image RGB
    * @param os Stream output
    * @param qual qualit� 0.9 moyen, 0.95 pas mal, 0.97 excellent
    * @throws Exception
    */
   public void writeJPEG(Image img,float qual,boolean RGB,OutputStream os) throws Exception {
      BufferedImage bufferedImage;
      if( img instanceof BufferedImage ) bufferedImage = (BufferedImage)img;
      else {
         bufferedImage = new BufferedImage(
               img.getWidth(null),
               img.getHeight(null),
               RGB ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_BYTE_GRAY );
         Graphics g = bufferedImage.createGraphics();
         g.drawImage(img,0,0,aladin);
         g.dispose();
      }
      aladin.waitImage(img);
      if( qual<0 || qual>1 ) qual=0.95f;

      ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
      ImageWriteParam iwp = writer.getDefaultWriteParam();
      iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
      iwp.setCompressionQuality(qual);

      ImageOutputStream out = ImageIO.createImageOutputStream(os);
      writer.setOutput(out);
      writer.write(null, new IIOImage(bufferedImage,null,null), iwp);
      writer.dispose();
   }




  /** Sauvegarde de vues
   * Sauvegarde en BMP 24 bits ou JPEG ou EPS
   * @param file Descripteur du fichier destination, ou null pour sortie standard
   * @param w la largeur de l'image de destination (uniquement mode nogui)
   * @param h la hauteur de l'image de destination (uniquement mode nogui)
   * @param format JPEG, PNG ou BMP ou EPS
   * @param qual qualit� (pour JPEG uniquement) [0..1]
   * @param mode CURRENTVIEW, ALLVIEWS, ALLROIS
   * @return True si Ok false sinon
   *
   * REMARQUE : ACTUELLEMENT SEUL LE MODE CURRENTVIEW EST SUPPORTE. LE PROBLEME
   * SE POSE POUR LES VIEWSIMPLES NON CREEES
   */
   protected boolean saveView(String filename,int w, int h,int format,float qual) {
      return saveView(filename,w,h,format,qual,CURRENTVIEW);
   }
   protected boolean saveView(String filename,int w, int h,int format,float qual,int mode) {
      if( mode!=CURRENTVIEW ) {
         System.err.println("Presently, only the current view can be saved");
         return false;
      }
      return saveOneView(filename,w,h,format,qual,aladin.view.getCurrentView());
   }

   
   protected boolean saveOneView(String filename,int w, int h,int format,float qual,ViewSimple v) {
      boolean rep=false;
//      try {
//         // Un peu compliqu� mais indispensable car sinon risque de DeadLock
//         v.waitLockRepaint("saveOneView");
//         if( !v.isSync() ) {
//            v.unlockRepaint("saveOneView");
//            System.out.println(Thread.currentThread().getName()+": Save.saveOneView() waiting isSync() on "+v);
//            Util.pause(50);
//            v.waitLockRepaint("saveOneView");
//         }
//         System.out.println(Thread.currentThread().getName()+": Save.saveOneView() ready "+v);
         try { rep=saveOneView1(filename,w,h,format,qual,v); }
         catch( Exception e) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
         return rep;
//      } finally { v.unlockRepaint("saveOneView"); }
   }

    /** Sauvegarde d'une vue
    * Sauvegarde en BMP 24 bits ou JPEG ou EPS
    * @param file Descripteur du fichier destination, ou null pour sortie standard
    * @param w la largeur de l'image de destination (uniquement mode nogui)
    * @param h la hauteur de l'image de destination (uniquement mode nogui)
    * @param format JPEG, BMP, EPS
    * @param qual qualit� (pour JPEG uniquement) [0..1]
    * @param v la vue � sauvegarder
    * @return True si Ok false sinon
    *
    */
   private boolean saveOneView1(String filename,int w, int h,int format,float qual,ViewSimple v) throws Exception {
     
//      System.out.println("saneOneView1 "+filename+" avant ...");
      boolean rep=false;
//      v.setLockRepaint(true);
      
      OutputStream o=null;
      try {
          o = filename==null ?
               (OutputStream)System.out :
               (OutputStream)new FileOutputStream(aladin.getFullFileName(filename));
        if( (format&EPS)==EPS  ) saveEPS(v,o);
        else {
            Image img = v.getImage(w,h);
            if( (format&JPEG)==JPEG )  {
               String s = generateFitsHeaderString(v);
               ImageWriter(img,"jpg",qual,true,new JpegOutputFilter(o,s));
            }
            else if( (format&PNG)==PNG )  {
               String s = generateFitsHeaderString(v);
               ImageWriter(img,"png",-1,true,new PNGOutputFilter(o,s));
            }
//            else if( (format&PNG)==PNG ) ImageWriter( img ,"png",-1,true,o);
            else if( (format&BMP)==BMP  ) BMPWriter.write( img ,o);
            else throw new Exception("Unsupported output image format !");
         }
         rep=true;
      } catch(Exception e) {
         if( filename!=null ) System.out.println("!!! image error ["+filename+"]");
         System.err.println(e.getMessage());
         if( aladin.levelTrace>=3 ) e.printStackTrace();
      } finally { if( o!=null && o!=System.out ) o.close(); }
      Aladin.trace(3,"Current view saved successfully "+aladin.getFullFileName(filename));

      //G�n�ration d'un fichier de liens pour faire une carte cliquable HTML
      if( ( (format&LK)==LK || (format&LK_FLEX)==LK_FLEX ) && filename!=null ) {
         boolean lkFlex = (format&LK_FLEX)==LK_FLEX;
         try {
            int i=filename.lastIndexOf('.');
            if( i>=0 ) filename = filename.substring(0,i);
            String linkFilename = filename+(lkFlex?".lkflex":".lk");
            o = new FileOutputStream(aladin.getFullFileName(linkFilename));
            linkWriter(v,o,lkFlex);

            // �criture position des 4 coins de la vue
            if( lkFlex ) {
                String cornersFilename = filename+".corners";

                OutputStream oCorners = new FileOutputStream(aladin.getFullFileName(cornersFilename));
                PrintStream out = new PrintStream(oCorners);

                PointD pp =  v.getPosition(0.0, 0.0);
                Coord coo1 = new Coord();
                coo1.x = pp.x;
                coo1.y = pp.y;
                v.pref.projd.getCoord(coo1);

                pp = v.getPosition(0.0, v.rv.height);
                Coord coo2 = new Coord();
                coo2.x = pp.x;
                coo2.y = pp.y;
                v.pref.projd.getCoord(coo2);

                pp = v.getPosition((double)v.rv.width, (double)v.rv.height);
                Coord coo3 = new Coord();
                coo3.x = pp.x;
                coo3.y = pp.y;
                v.pref.projd.getCoord(coo3);

                pp = v.getPosition(v.rv.width, 0.0);
                Coord coo4 = new Coord();
                coo4.x = pp.x;
                coo4.y = pp.y;
                v.pref.projd.getCoord(coo4);

                out.print("# C0 "+"0.0"+" "+"0.0"+" "+coo1.al+" "+coo1.del+"\n");
                out.print("# C1 "+"0.0"+" "+v.rv.height+" "+coo2.al+" "+coo2.del+"\n");
                out.print("# C2 "+v.rv.width+" "+v.rv.height+" "+coo3.al+" "+coo3.del+"\n");
                out.print("# C3 "+v.rv.width+" "+"0.0"+" "+coo4.al+" "+coo4.del+"\n");

                out.flush();
                out.close();
            }

            Aladin.trace(3, "HTTP link file generated ["+linkFilename+"]");
         } catch(Exception e) {
            System.err.println(e.getMessage());
            if( aladin.levelTrace>=3 ) e.printStackTrace();
         }
         finally { o.close(); }
      }

//      v.setLockRepaint(false);
//      v.repaint();

      return rep;
   }

   /** G�n�re un fichier d�crivant les liens HTTP associ�es � une image sauvegard�e.
    * Le format de chaque ligne est le suivant
    * NomDuPlan <TAB> x <TAB> y <TAB> id <TAB> url
    * @param v la vue concern�e
    * @param o le flux de sortie
    * @throws Exception
    */
   protected void linkWriter(ViewSimple v,OutputStream o, boolean flex) throws Exception {

      PrintStream out = new PrintStream(o);
      out.print("#PLANE\tID\tX\tY\tURL\n");
      Plan [] plan = aladin.calque.getPlans();
      Plan folder = aladin.calque.getMyScopeFolder(plan,v.pref);

      for( int i=plan.length-1; i>=0; i--) {
         Plan p = plan[i];
         if( !p.isSimpleCatalog() || !p.flagOk ) continue;
         boolean flagDraw = i>=v.n;

         // M�me scope que le plan de r�f�rence ?
         flagDraw = flagDraw && aladin.calque.getMyScopeFolder(plan,p)==folder;

         if( p.pcat==null || (!p.active && !flex) ) continue;
         if( !flex) p.pcat.writeLink(out,v,flagDraw);
         else p.pcat.writeLinkFlex(out,v,flagDraw);
      }
      out.flush();
      out.close();
   }

   /** Sauvegarde de la vue sp�cifi�e en EPS
    * @param v la vue � sauvegarder
    * @param o le flux de sortie
    */
   protected void saveEPS(ViewSimple v,OutputStream o) throws Exception {
      PrintStream out = new PrintStream(o);
      EPSGraphics epsg = new EPSGraphics(out,"Aladin-chart",null,0,0,v.rv.width,v.rv.height);

      // Affichage de l'image
      if( v.imgprep!=null && v.pref.active ) epsg.drawImage(v.imgprep,v.dx,v.dy,v.aladin);
      // test pour sortie EPS des footprints avec transparence
//      if( v.imgprep!=null && v.pref.active ) epsg.drawImage(v.getImage(0,0),0,0,v.aladin);


      // Affichage des overlays
      if( Projection.isOk(v.pref.projd ) ) {
         v.paintOverlays(epsg,null,0,0,true);
         v.drawRepere(epsg,0,0);
         v.drawCredit(epsg,0,0);
      }
      epsg.end();
      out.close();
   }


  /** Sauvegarde de la vue courante.
   * Pour l'instant, la sauvegarde se fait en FITS (8 bits => niveaux de gris)
   * Il serait souhaitable de trouver un format plus adequat a une eventuelle
   * importation dans un logiciel graphique (genre GIF sans pour autant payer
   * les droits)
   * @param file Descripteur du fichier destination
   * @return True si Okm false sinon
   protected boolean saveView(String filename) {
      File file = new File(filename);

      // Recup des infos sur la vue courante
      Image img = aladin.view.getImage();
      int w = img.getWidth(aladin);
      int h = img.getHeight(aladin);

      // Extraction des pixels
      int[] pixels = new int[w * h];
      PixelGrabber pg = new PixelGrabber(img, 0,0, w, h, pixels, 0, w);
      try { pg.grabPixels(); } catch ( Exception e) { return false; }

      // Passage en niveaux de gris
      byte [] a = new byte[w*h];
      for( int j=0; j<w*h; j++ ) {
         int pixel = pixels[j];
         int red   = (pixel >> 16) & 0xff;
         int green = (pixel >>  8) & 0xff;
         int blue  = (pixel      ) & 0xff;
         a[j] = (byte)( (red+green+blue)/3 );
      }

      // Sauvegarde Fits
      return saveImageFITS(file,w,h,a,null);
   }
   */
   
   protected boolean saveMoc(String filename, PlanMoc p) {
      try {
         HealpixMoc moc = p.getMoc();
         moc.write(filename, HealpixMoc.FITS);
      } catch( Exception e ) {
         if( aladin.levelTrace>3 ) e.printStackTrace();
         return false;
      }
      return true;
   }

   protected boolean saveImageBMP(String filename,Plan p1) {
      PlanImage p = (PlanImage)p1;
      try { BMPWriter.write24BitBMP(((PlanRGBInterface)p).getPixelsRGB(),p.width,p.height,new FileOutputStream(filename)); }
      catch(Exception e) {
         System.out.println("!!! BMP image failed for \""+filename+"\"");
         System.err.println(e+"");
         return false;
      }
      aladin.log("export","BMP");
      return true;
   }

  /** Sauvegarde d'un plan image.
   * @param mode 0 - FIts classique, 1 - FITS HEALPIX, 2 - JPEG,  3 - PNG
   */
   protected boolean saveImage(String s,Plan p,int mode) {
      s=aladin.getFullFileName(s);
      File f = new File(s);
      if( mode>=2 ) return saveImageColor(s,(PlanImage)p,mode);
      return saveImageFITS(f,(PlanImage)p,mode);
   }

   /** Sauvegarde sous forme JPEG ou PNG + header FITS d'un plan - mode peut �tre 3-"png" ou 2-"jpg" */
   public boolean saveImageColor(String filename,PlanImage p,int mode) {
      try {
         OutputStream o = new FileOutputStream(filename);
         boolean rep=saveImageColor(o,p,mode);
         aladin.log("export",mode==3 ? "PNG" : "JPEG");
         return rep;

      } catch( Exception e ) {
         System.out.println("!!! "+(mode==3?"PNG":"JPEG")+" image failed");
         System.err.println(e+"");
         return false;
      }
   }

   /** Sauvegarde sous forme JPEG ou PNG + header FITS d'un plan - mode peut �tre 3-"png" ou 2-"jpg" */
   protected boolean saveImageColor(OutputStream o,PlanImage p, int mode) {
      MemoryImageSource img;
      try {
         if( p.type==Plan.IMAGERGB ) {
            img = new MemoryImageSource(p.width,p.height,p.cm,
                  ((PlanRGBInterface)p).getPixelsRGB(), 0,p.width);
         } else img = new MemoryImageSource(p.width,p.height,p.cm, p.pixels, 0,p.width);

         String s = "Created by Aladin";
         if( !p.hasNoReduction() ) s = generateFitsHeaderStringForNativeImage(p);
         ImageWriter(getToolkit().createImage(img),mode==3 ? "png":"jpg",-1,
               p.type==Plan.IMAGERGB, mode==3 ? new PNGOutputFilter(o,s) : new JpegOutputFilter(o,s));
      } catch(Exception e) {
         System.out.println("!!! JPEG image failed");
         System.err.println(e+"");
         return false;
      }
      return true;
   }
   

   /**
    * Sauvegarde d'un plan image dans un fichier FITS. Prend en compte le fait que l'image
    * ait pu �tre recalibr�e ou r��chantillonn��, que le plan dispose ou non des vraies pixels,
    * et d'une ent�te FITS originale
    * @param file Fichier FITS � cr�er
    * @param p Plan de l'image � sauvegarder
    * @param mode 0 - Fits classique, 1 - FITS HEALPIX
    * @return true si Ok, false sinon
    */
   protected boolean saveImageFITS(File file,PlanImage p) { return saveImageFITS(file,p,0); }
   protected boolean saveImageFITS(File file,PlanImage p,int mode) {
      boolean rep=false;

      try {
         file.delete();

         FileOutputStream f = new FileOutputStream(file);
         if( mode==0 ) saveImageFITS(f,p);
         else saveImageHPX(f,p);
         f.close();
         rep=true;
      } catch( Exception e) {
         if( aladin.levelTrace>=3 ) e.printStackTrace();
         errorFile=errorFile+"\n"+file;
      }
      aladin.log("export","image FITS");
      return rep;
   }


   /** G�n�ration de l'ent�te FITS (mode strings) obtenu par generateFitsHeader(PlanImage p) 
    * pour une image JPEG ou PNG */
   protected String generateFitsHeaderStringForNativeImage(PlanImage p) {
      Vector v =  generateFitsHeader1(p.projInit,p.projd,
            p.headerFits,
            false,
            p.hasSpecificCalib(),
            false,
            false,
            8,p.bZero,p.bScale,p.width,p.height);

      return fitsHeaderVtoStrings( v );
   }


   /** G�n�ration de l'ent�te FITS (mode strings) obtenu par generateFitsHeader(PlanImage p) */
   protected String generateFitsHeaderString(PlanImage p) {
      return fitsHeaderVtoStrings( generateFitsHeader(p) );
   }

   /** G�n�ration de l'ent�te FITS (mode strings) obtenu par generateFitsHeader(ViewSimple v) */
   protected String generateFitsHeaderString(ViewSimple v) {
      return fitsHeaderVtoStrings( generateFitsHeader(v) );
   }

   /** Conversion d'une ent�te Fits du mode Vector vers le mode Strings */
   private String fitsHeaderVtoStrings(Vector v) {
      StringBuffer s = new StringBuffer(v.size()*80);
      Enumeration e = v.elements();
      while( e.hasMoreElements() ) {
         byte b [] = (byte[])e.nextElement();
         s.append((new String(b)).trim());
         s.append('\n');
      }
      return s.toString();
   }

   /** Generation d'une ent�te Fits pour une vue */
   protected Vector generateFitsHeader(ViewSimple v) {
      Plan p = v.pref;
      Projection proj = Projection.isOk(p.projd) ? p.projd.copy() : null;
      int x = (int)Math.floor(v.rzoom.x);
      int y = (int)Math.floor(v.rzoom.y);
      int width = (int)Math.ceil(v.rzoom.width);
      int height = (int)Math.ceil(v.rzoom.height);
      double Zzoom = v.zoom ;

      if( proj!=null ) proj.cropAndZoom(x, y, width, height, Zzoom) ;
      width = (int)Math.round(Zzoom*width) ;
      height = (int)Math.round(Zzoom*height);
      if( p instanceof PlanImage ) {
         PlanImage pi = (PlanImage)p;
         return generateFitsHeader1(pi.projInit,proj, pi.headerFits, false, true, pi instanceof PlanImageRGB, pi instanceof PlanImageAlgo,
               pi.bitpix,pi.bZero,pi.bScale, width, height);
      }
      return generateFitsHeader1(p.projInit,proj, p.headerFits, false, true, p instanceof PlanImageRGB, p instanceof PlanImageAlgo,
            8, 0., 1., width, height);
   }

   /** G�n�ration d'une ent�te FITS reprenant toutes les infos originales
    * de l'image, mises � jour en fonction des modifications de l'utilisateur
    * (cropping, r��chantillonnage, recalibration...)
    * @param p le plan image concern�
    * @return un vector contenant les lignes de l'ent�te FITS align�es sur 80 bytes
    */
   protected Vector generateFitsHeader(PlanImage p) {
      return generateFitsHeader1(p.projInit,p.projd,
            p.headerFits,
            p.hasOriginalPixels(),
            p.hasSpecificCalib(),
            p instanceof PlanImageRGB,
            p instanceof PlanImageAlgo,
            p.bitpix,p.bZero,p.bScale,p.width,p.height);
   }

   /** Idem mais avec les param�tres individuels pour pouvoir l'appliquer �
    * la vue */
   private Vector generateFitsHeader1(Projection projInit,Projection projd,
         FrameHeaderFits headerFits, boolean hasFitsPixels, boolean hasSpecificWCS,
         boolean imageRGB,boolean imageAlgo,int bitpix,double bZero,double bScale,int width,int height) {
      int i;
      Vector v = new Vector(100);
      Vector key   = null;
      Vector value = null;
      Hashtable qKey = null;

      boolean hasFitsHeader = headerFits!=null;
      boolean hasNAXIS3 = imageRGB && hasFitsHeader && headerFits.hasKey("NAXIS3");
      boolean hasCTYPE3 = imageRGB && hasFitsHeader && headerFits.hasKey("CTYPE3");

      // R�cup�ration des champs WCS
      if( hasSpecificWCS ) {
         key   = new Vector(20);
         value = new Vector(20);
         try { projd.getWCS(key,value); }
         catch( Exception e ) { }

         // Recherche des champs � supprimer ds l'ent�te FITS initiale
         qKey = new Hashtable(30);
         if( projInit!=null ) {
            String keyOmit[] = projInit.c.getWCSKeys();
            for( i=0; i<keyOmit.length; i++ ) {
               String k = keyOmit[i].trim();
               // On ne prend pas en compte certains mots cl�s qui vont
               // faire d�sordre si on les met � la fin du fichier
               if( k.equals("NAXIS1") || k.equals("NAXIS2") || k.equals("EPOCH") ) continue;
               qKey.put(k,"");
//               aladin.trace(3,"remove FITS key ["+k+"]");
            }
         }
         qKey.put("END","");
      }

      // Je n'ai pas d'entete FITS, je la g�n�re de toutes pieces
      if( !hasFitsHeader ) {
         v.addElement( getFitsLine("SIMPLE","T","Generated by Aladin (CDS)") );
         v.addElement( getFitsLine("BITPIX",bitpix+"","Bits per pixel") );

         if( imageRGB ) {
            v.addElement( getFitsLine("NAXIS","3","Number of dimensions") );
            v.addElement( getFitsLine("NAXIS1",""+width,"Length of x axis") );
            v.addElement( getFitsLine("NAXIS2",""+height,"Length of y axis") );
            v.addElement( getFitsLine("NAXIS3","3","Number of colors") );
//            v.addElement( getFitsLine("CTYPE3","RGB","Red Green Blue planes") );
        } else {
            v.addElement( getFitsLine("NAXIS","2","Number of dimensions") );
            v.addElement( getFitsLine("NAXIS1",""+width,"Length of x axis") );
            v.addElement( getFitsLine("NAXIS2",""+height,"Length of y axis") );

         }

      // J'ai une entete FITS, je la copie en virant les champs WCS si besoin est
      } else {

         int naxis=2;
         try { naxis = headerFits.getIntFromHeader("NAXIS"); } 
         catch( Exception e1 ) { }
         boolean flagModif=false;

         Hashtable origKeys = new Hashtable(100);

         // R�cup�ration de l'ancienne entete
         StringTokenizer st = new StringTokenizer(headerFits.getOriginalHeaderFits(),"\n");
         while( st.hasMoreTokens() ) {
            String s = st.nextToken();
            String k = HeaderFits.getKey(s);

            if( k!=null ){

               // On m�morise les keys pour pouvoir rep�rer celles qui auraient �t� ajout�es
               // directement dans la hashTable header via un plugin
               origKeys.put(k,"");

               // On ne remet pas le END qui sera de toutes fa�ons ajout�
               if( k.equals("END") ) continue;

               // Dans le cas d'une sauvegarde d'une image d'un MEF
               if( k.equals("XTENSION") ) {
                  v.addElement( getFitsLine("SIMPLE","T","Generated by Aladin (CDS)") );
                  continue;
               }

               // Je dois virer CTYPE3 = 'RGB' si l'image a �t� pass�e en niveaux de gris
               if( !imageRGB && k.equals("CTYPE3")
                     && headerFits.getStringFromHeader(k).equals("RGB") )  continue;

               // Si je n'ai pas les pixels originaux ou qu'il s'agit d'une image couleur,
               // il faut passer en 8 bits
               if( !hasFitsPixels || imageRGB ) {
                  if( k.equals("SIMPLE") ) {
                     v.addElement( getFitsLine("SIMPLE","T","Generated by Aladin (CDS)") );
                     continue;
                  } else if( k.equals("BITPIX") ) {
                     v.addElement( getFitsLine("BITPIX","8","Bits per pixel") );
                     continue;
                  }
               }

               // S'il s'agit d'une image obtenu par Algo
               if( imageAlgo ) {
                  if( k.equals("BZERO") ) {
                     v.addElement( getFitsLine("BZERO",bZero+"","Generated by Aladin (CDS)") );
                     continue;
                  } else if( k.equals("BSCALE") ) {
                     v.addElement( getFitsLine("BSCALE",bScale+"","Generated by Aladin (CDS)") );
                     continue;
                  } else if( k.equals("BITPIX") ) {
                     v.addElement( getFitsLine("BITPIX",bitpix+"","Bits per pixel") );
                     continue;
                  }
               }

               // On omet le champ EXTEND (Aladin ne sauvegarde pas les extensions)
               if( k.equals("EXTEND")) { flagModif=true; continue; }

               // On omet les champs NAXISn si n>2 (et non imageRGB)
//               if( k.startsWith("NAXIS")
//                     && !k.equals("NAXIS")
//                     && !k.equals("NAXIS1")
//                     && !k.equals("NAXIS2") && !imageRGB ) { flagModif=true; continue; }

               // On force NAXIS � 3 si imageRGB
               if( imageRGB && naxis!=3 && k.equals("NAXIS") ) {
                  flagModif=true;
                  v.addElement( getFitsLine("NAXIS","3" ,"") );
                  continue;
               }

               // On force NAXIS � 2 (ou 3 si imageRGB)
//               if( naxis>2 && k.equals("NAXIS") ) {
//                  flagModif=true;
//                  v.addElement( getFitsLine("NAXIS",imageRGB ? "3":"2" ,"") );
//                  continue;
//               }

               // Si on a du WCS specifique (recalibr�, resampl�, ou cropp� )
               if( hasSpecificWCS  ) {

                  // S'il s'agit d'une image resampl�e ou cropp�e, il faut forcer le NAXIS1,NAXIS2
                  // en fonction de la nouvelle taille
//                  if( p instanceof PlanImageResamp || p.) {
                     if( k.equals("NAXIS1") && width!=headerFits.getIntFromHeader(k) ) {
                        v.addElement( getFitsLine("NAXIS1",width+"","Length of x axis") );
                        flagModif=true;
                        continue;
                     }
                     if( k.equals("NAXIS2") && height!=headerFits.getIntFromHeader(k)) {
                        v.addElement( getFitsLine("NAXIS2",height+"","Length of y axis") );
                        flagModif=true;
                        continue;
                     }
//                  }

                  // On omet les champs WCS de l'image initiale.
                  if( hasSpecificWCS && qKey.get(k)!=null ) continue;
               }

               // S'il s'agit d'une image RGB, il faut ajouter NAXIS3=3 et CTYPE3=RGB
               // si ce n'est d�j� fait
               if( imageRGB && (!hasNAXIS3 || !hasCTYPE3) ) {
                  if( !hasNAXIS3 && k.equals("NAXIS2") ) {
                     v.addElement( getFullFitsLine(s)  );
                     v.addElement( getFitsLine("NAXIS3","3","Number of colors") );
                     flagModif=true;
                     continue;
                  }
                  if( !hasCTYPE3 && k.equals("CTYPE2") ) {
                     v.addElement( getFullFitsLine(s) );
                     v.addElement( getFitsLine("CTYPE3","RGB","Red Green Blue planes") );
                     flagModif=true;
                     hasCTYPE3=true;
                     continue;
                  }
               }
            }

            String valOrig = HeaderFits.getValue(s);
            String valC = (String)headerFits.getHeaderFits().getHashHeader().get(k);
            if( valC==null ) continue;      // La cl� a �t� supprim�
            if( Tok.unQuote(valC).trim().equals(valOrig) ) v.addElement( getFullFitsLine(s));  // La cl� n'a pas �t� touch�e
            else v.addElement( getFitsLine(k,valC,"Aladin modif") );   // La cl� a �t� modifi�e


//            // On m�morise la ligne courante
//            v.addElement( getFullFitsLine(s));
         }

         // On v�rifie qu'on a rien oubli�
         if( hasFitsHeader ) {
            Enumeration e = headerFits.getHeaderFits().getHashHeader().keys();
            while( e.hasMoreElements() ) {
               String k = (String)e.nextElement();
               if( origKeys.get(k)==null ) {
                  v.addElement( getFitsLine(k,(String)headerFits.getHeaderFits().getHashHeader().get(k),"Aladin add") );
               }
            }
         }

         if( flagModif ) v.addElement( getFitsLine("Generated by Aladin (CDS)") );;
      }

      // Ajout de la calibration WCS specifique
      if( hasSpecificWCS ) {
         v.addElement( getFitsLine("This astrometrical calibration was computed via Aladin") );
         Enumeration ekey   = key.elements();
         Enumeration evalue = value.elements();
         while( ekey.hasMoreElements() ) {
            String skey   = (String)ekey.nextElement();
            String svalue = (String)evalue.nextElement();
            if( skey.trim().equals("NAXIS1")
                  || skey.trim().equals("NAXIS2")
                  || skey.trim().equals("NAXIS3") ) continue;
//            aladin.trace(3,"add FITS key ["+skey+"]");
            v.addElement( getFitsLine(skey,svalue,"") );

            if( imageRGB && !hasCTYPE3 && skey.trim().equals("CTYPE2") ) {
               v.addElement( getFitsLine("CTYPE3","RGB","Red Green Blue planes") );
               hasCTYPE3=true;
               continue;
            }

         }
      }

      // LE CTYPE3 pour RGB n'a pas �t� mis car il n'y avait aucun CTYPE2 => je l'ajoute ici
      if( imageRGB && !hasCTYPE3 ) v.addElement( getFitsLine("CTYPE3","RGB","Red Green Blue planes") );

      return v;
   }

   /**
    * Sauvegarde d'un plan image dans stream en format FITS. Prend en compte le fait que l'image
    * ait pu �tre recalibr�e ou r��chantillonn��, que le plan dispose ou non des vraies pixels,
    * et d'une ent�te FITS originale
    * @param os le stream de sortie ou null si cr�ation d'un inputStream de retour
    * @param p le plan image � sauvegarder
    * @return l'inputstream de la sauvegarde ou null si on passe par outputStream
    * @throws IOException
    */
   protected InputStream saveImageFITS(OutputStream os,PlanImage p) throws Exception {
      MyByteArrayStream bas = null;
      OutputStream f;
      int size=0;
      int i;
      byte [] bb;

      if( os!=null ) f = os;
      else f = bas = new MyByteArrayStream(10000);

      boolean hasFitsPixels = p.hasOriginalPixels();
      boolean hasFitsHeader = p.hasFitsHeader();
      boolean hasSpecificWCS = p.hasSpecificCalib();
      boolean imageRGB = p instanceof PlanImageRGB;

Aladin.trace(3,"Export "+p.label+" (orig. Fits header:"+hasFitsHeader+
                               ", orig. pixels:"+hasFitsPixels+
                               ", specif.calib:"+hasSpecificWCS+
                               ", RGB:"+imageRGB+")");

      // Generation de l'ent�te FITS
      Vector v = generateFitsHeader(p);
      size = writeFitsLines(f, v, size);
      byte [] end = getEndBourrage(size);
      f.write(end);
      size += end.length;

      // Sauvegarde des pixels d'une image RGB (ou native)
      if( imageRGB ) {
         size=0;
         PlanImageRGB pRGB = (PlanImageRGB)p;
         int n = pRGB.width*pRGB.height;
         byte buf[] = new byte[n];
         for(i=0; i<3; i++ ) {
            pRGB.getColor(buf,i);
            pRGB.invImageLine(pRGB.width,pRGB.height,buf);
            f.write(buf);
            size+=n;
         }

      // Sauvegarde des pixels d'une image simple (soit les vrais, soit les 8 bits)
      } else {
         if( hasFitsPixels ) bb = p.getFitsPixels(); // p.invImageLine(p.width,p.height,bb,p.npix); }
         else bb = p.getFits8Pixels();

         f.write(bb);
         size=bb.length;
      }

      // Bourrage
      if( size%2880!=0 ) {
         int bourrage = 2880-size%2880;
         bb = new byte[bourrage];
         f.write(bb);
      }

      // retour d'un InputStream ?
      if( bas==null ) return null;
      else return bas.getInputStream();
   }

   // g�n�ration de la premi�re HDU d'un fichier FITS Healpix
   private Vector generateHealpixHDU0(boolean flagColor) {
      Vector v = new Vector(100);
      v.addElement( getFitsLine("SIMPLE","T","conforms to FITS standard") );
      v.addElement( getFitsLine("BITPIX","8","array data type") );
      v.addElement( getFitsLine("NAXIS","0","number of array dimensions") );
      v.addElement( getFitsLine("EXTEND","T",null) );
      if( flagColor ) v.addElement( getFitsLine("COLORMOD","ARGB",null) );
      return v;
   }

   // G�n�ration de la deuxi�me HDU d'un fichier FITS Healpix
   private Vector generateHealpixHDU1(int norder,int bitpix,boolean ring, int width) {
      Vector v = new Vector(100);
      long nside = CDSHealpix.pow2(norder);
      long nbPix = 12*nside*nside;
      int npix = Math.abs(bitpix)/8;
width=1;
      String tForm = bitpix==8 ? "XX" : bitpix==16 ? "I" : bitpix==32 ? "J" :
                     bitpix==-32 ? "E" : "D";
      v.addElement( getFitsLine("XTENSION","BINTABLE","binary table extension") );
      v.addElement( getFitsLine("BITPIX","8","array data type") );
      v.addElement( getFitsLine("NAXIS","2","2-dimensional binary table") );
      v.addElement( getFitsLine("NAXIS1",(width*npix)+"","width of table") );
      v.addElement( getFitsLine("NAXIS2",(nbPix/width)+"","number of rows in table") );
      v.addElement( getFitsLine("PCOUNT","0","number of group parameters") );
      v.addElement( getFitsLine("GCOUNT","1","number of groups") );
      v.addElement( getFitsLine("TFIELDS","1","number of table fields") );
      v.addElement( getFitsLine("TTYPE1","PIXVAL","label for field   1") );
      v.addElement( getFitsLine("TFORM1",width+tForm,"data format of field") );
      v.addElement( getFitsLine("PIXTYPE","HEALPIX","Pixel algorithm") );
      v.addElement( getFitsLine("ORDERING",ring?"RING":"NESTED","Ordering scheme") );
      v.addElement( getFitsLine("NSIDE",nside+"","Resolution parameter") );
      v.addElement( getFitsLine("FIRSTPIX","0","First pixel (0 based)") );
      v.addElement( getFitsLine("LASTPIX",(nbPix-1)+"","Last pixel (0 based)") );
//      v.addElement( getFitsLine("INDXSCHM","IMPLICIT","Indexing: IMPLICIT or EXPLICIT") );
      return v;
   }


   /** Ecriture d'un ensemble de lignes FITS d�j� pr�format�es dans un Vector de byte[80]
    * @param taille du flux avant l'�criture
    * @return nouvelle taille du flux apr�s �criture
    */
   private int writeFitsLines(OutputStream f,Vector v,int size) throws Exception {
      Enumeration e = v.elements();
      while( e.hasMoreElements() ) {
         byte [] b = (byte[])e.nextElement();
         f.write(b);
         size+=b.length;
      }
      return size;
   }

   byte[] nan ;
   
   /**
    * Sauvegarde d'un plan image dans stream en format HEALPIX FITS.
    * @param os le stream de sortie ou null si cr�ation d'un inputStream de retour
    * @param p le plan image � sauvegarder
    * @return l'inputstream de la sauvegarde ou null si on passe par outputStream
    * @throws IOException
    */
   protected InputStream saveImageHPX(OutputStream os,PlanBG p) throws Exception {
      MyByteArrayStream bas = null;
      OutputStream f;
      int size=0;

      try {
      if( os!=null ) f = os;
      else f = bas = new MyByteArrayStream(10000);

      int N = 9; // 9 a priori puisqu'on est en 512x512
      int losangeWidth = cds.tools.pixtools.Util.nside(N); 
      int order=N+3;
      int bitpix= p.bitpix;
     
      int nbits=Math.abs(bitpix)/8;

      long nside = CDSHealpix.pow2(order);
//      long nbPix = 12*nside*nside;
      long nbPix3 = 12*8*8;// 12 x 2^3 x 2^3 // � l'ordre 3
      boolean ring = false;
      int lenLine=1024;
      Aladin.trace(3,"Export "+p.label+"\" in healpix NSIDE="+nside+" ["+(ring?"RING":"NESTED")+"] bitpix="+
            bitpix);

     // Generation de la premi�re HDU FITS
      Vector v = generateHealpixHDU0(false);
      size=writeFitsLines(f,v,size);
      byte [] end = getEndBourrage(size);
      f.write(end);
      size += end.length;

      // Generation de la deuxi�me HDU FITS
      v = generateHealpixHDU1(order,bitpix,ring,lenLine);
      size=writeFitsLines(f,v,size);
      end = getEndBourrage(size);
      f.write(end);
      size += end.length;

      // Sauvegarde des pixels (on parcourt les pixels Healpix dans l'ordre)
      // et on �crit ligne par ligne (lenLigne valeurs � chaque fois)
      Projection proj = p.projd;
      Coord c = new Coord();
      byte [] buf = new byte[lenLine*nbits];
      int pos=0;
      // nb pixels par losange
      int nbPix = losangeWidth*losangeWidth;
      nan = new byte[nbPix*nbits];
      for (int i = 0 ; i < nbPix ; i++) {
    	  PlanImage.setPixVal(nan, bitpix, i, Double.NaN);
      }
      int[] hpx2xy = cds.tools.pixtools.Util.createHpx2xy(N);
//      for( long ipix=0; ipix<nbPix; ipix++) {
      for (int i = 0 ; i < nbPix3 ; i++) {
    	  boolean found = true;
    	  double val;
    	  // r�cup�re le losange de niveau 3
    	  String filename = cds.tools.pixtools.Util.getFilePath(p.url,order-N, i);
    	  Fits los = new Fits();
    	  try {
    		  los.loadFITS(filename+".fits");
    	  } catch (FileNotFoundException e) {
    		  // ne rien dire, il va y en avoir plein si c'est partiel !
    		  found=false;
    	  }
    	  if (!found) {
    		  // on finit d'�crire ce qu'il restait dans le buffer
    		  f.write(buf,0,pos); size+=pos; pos=0;
    		  // on ajoute tout le losange en nan
    		  f.write(nan); pos=0; size+=nan.length; p.pourcent=(100.*i)/nbPix3; 
    	  }
    	  else {
    		  for( int ipix = 0 ; ipix < nbPix ; ipix++) {
//    			  int[] xy = cds.tools.pixtools.Util.hpx2XY(ipix+1,N);
//    			  val = los.getPixelDouble(xy[0],losangeWidth-1-xy[1]);
    			  int idx = hpx2xy[ipix];
    	          int yy = idx/losangeWidth;
    	          int xx = idx-yy*losangeWidth;
    	          val = los.getPixelDouble(xx,yy);
    			  PlanImage.setPixVal(buf, bitpix, pos++, val);

    			  if( pos==lenLine ) { f.write(buf); pos=0; size+=buf.length; p.pourcent=(100.*i)/nbPix3; }
    		  }
    	  }
      }
      if( pos>0 ) { f.write(buf,0,pos); size+=pos; }

      // Bourrage final
      if( size%2880!=0 ) {
         int bourrage = 2880-size%2880;
         byte [] bb = new byte[bourrage];
         f.write(bb);
      }

      } catch( Exception e ) { e.printStackTrace(); }
      p.pourcent=-1;
      p.setLockCacheFree(false);

      // retour d'un InputStream ?
      if( bas==null ) return null;
      else return bas.getInputStream();
   }

   /**
    * Sauvegarde d'un plan image dans stream en format HEALPIX FITS. Prend en compte le fait que l'image
    * ait pu �tre recalibr�e ou r��chantillonn��, que le plan dispose ou non des vraies pixels,
    * et d'une ent�te FITS originale
    * @param os le stream de sortie ou null si cr�ation d'un inputStream de retour
    * @param p le plan image � sauvegarder
    * @return l'inputstream de la sauvegarde ou null si on passe par outputStream
    * @throws IOException
    */
   protected InputStream saveImageHPX(OutputStream os,PlanImage p) throws Exception {
	   if (p instanceof PlanBG)
		   return saveImageHPX(os, (PlanBG)p);
	   
      MyByteArrayStream bas = null;
      OutputStream f;
      int size=0;
      boolean flagColor = p instanceof PlanImageRGB;

      try {
      if( os!=null ) f = os;
      else f = bas = new MyByteArrayStream(10000);

// A modifier par la suite
      int order=9;
//      int order=13; // pour mellinger
      int bitpix=flagColor ? 32 : p.bitpix==8 ? 16 : p.bitpix;    // Pour le moment les bytes de sont pas support�s
      int npix=Math.abs(bitpix)/8;

      long nside = CDSHealpix.pow2(order);
      long nbPix = 12*nside*nside;
      boolean ring = false;
      int lenLine=1024;

      Aladin.trace(3,"Export "+p.label+"\" in healpix NSIDE="+nside+" ["+(ring?"RING":"NESTED")+"] bitpix="+
            bitpix+(flagColor?" ARGB":""));

     // Generation de la premi�re HDU FITS
      Vector v = generateHealpixHDU0(flagColor);
      size=writeFitsLines(f,v,size);
      byte [] end = getEndBourrage(size);
      f.write(end);
      size += end.length;

      // Generation de la deuxi�me HDU FITS
      v = generateHealpixHDU1(order,bitpix,ring,lenLine);
      size=writeFitsLines(f,v,size);
      end = getEndBourrage(size);
      f.write(end);
      size += end.length;

      // Sauvegarde des pixels (on parcourt les pixels Healpix dans l'ordre)
      // et on �crit ligne par ligne (lenLigne valeurs � chaque fois)
      Projection proj = p.projd;
      Coord c = new Coord();
      byte [] buf = new byte[lenLine*npix];
      int pos=0;
      p.pourcent=1;
      p.setLockCacheFree(true);
      p.pixelsOriginFromCache();
      for( long ipix=0; ipix<nbPix; ipix++) {
//         double [] polar = ring ? CDSHealpix.pix2ang_ring(nside,ipix) : CDSHealpix.pix2ang_nest(nside,ipix);
         double [] polar = CDSHealpix.pix2ang_nest(nside,ipix);
         double [] radec = CDSHealpix.polarToRadec(polar);
         c.al = radec[0];
         c.del = radec[1];
         Localisation.frameToFrame(c, Localisation.GAL, Localisation.ICRS);
         proj.getXY(c);

         if( !flagColor ) {
            double val;
            if( Double.isNaN(c.x) || c.x<0 || c.x>p.width || c.y<0 || c.y>p.height ) val = Double.NaN;
//            else  val = p.getPixelInDouble((int)c.x, (int)(p.height-c.y));
            else  val = p.getPixelInDouble((int)c.x, (int)c.y);
            PlanImage.setPixVal(buf, bitpix, pos++, val);
         } else {
            int val;
            if( Double.isNaN(c.x) || c.x<0 || c.x>p.width || c.y<0 || c.y>p.height ) val = 0;
            else  val = p.getPixel8((int)c.x, (int)(c.y));
            PlanImage.setInt(buf, pos*4, val);
            pos++;
         }

         if( pos==lenLine ) { f.write(buf); pos=0; size+=buf.length; p.pourcent=(100.*ipix)/nbPix; }
      }
      if( pos>0 ) { f.write(buf,0,pos); size+=pos; }

      // Bourrage final
      if( size%2880!=0 ) {
         int bourrage = 2880-size%2880;
         byte [] bb = new byte[bourrage];
         f.write(bb);
      }

      } catch( Exception e ) { e.printStackTrace(); }
      p.pourcent=-1;
      p.setLockCacheFree(false);

      // retour d'un InputStream ?
      if( bas==null ) return null;
      else return bas.getInputStream();
   }

   static public byte [] getFitsLine(String comment) {
      return getFitsLine("COMMENT", null, comment);
   }

   /**
    * Mise en forme d'une chaine pour une ent�te FITS en suivant la r�gle suivante:
    * si mot plus petit que 8 lettres, bourrage de blancs
    * utilisation de quotes simples + double quote simple � l'int�rieur
    * @param a la chaine a mettre en forme. Elle peut �tre d�j� quot�e
    * @return la chaine mise en forme
    */
   static private char [] formatFitsString(char [] a) {
      if( a.length==0 ) return a;
      StringBuffer s = new StringBuffer();
      int i;
      boolean flagQuote = a[0]=='\''; // Chaine d�j� quot�e ?

      s.append('\'');

      // recopie sans les quotes
      for( i= flagQuote ? 1:0; i<a.length- (flagQuote ? 1:0); i++ ) {
         if( !flagQuote && a[i]=='\'' ) s.append('\'');  // Double quotage
         s.append(a[i]);
      }

      // bourrage de blanc si <8 caract�res + 1�re quote
      for( ; i< (flagQuote ? 9:8); i++ ) s.append(' ');

      // ajout de la derni�re quote
      s.append('\'');

      return s.toString().toCharArray();

   }

   /**
    * Test si c'est une chaine � la FITS (ni num�rique, ni bool�en)
    * @param s la chaine � tester
    * @return true si s est une chaine ni num�rique, ni bool�enne
    * ATTENTION: NE PREND PAS EN COMPTE LES NOMBRES IMAGINAIRES
    */
   static private boolean isFitsString(String s) {
      if( s.length()==0 ) return true;
      char c = s.charAt(0);
      if( s.length()==1 && (c=='T' || c=='F') ) return false;	// boolean
      if( !Character.isDigit(c) && c!='.' && c!='-' && c!='+' ) return true;
      try {
         Double.valueOf(s);
         return false;
      } catch( Exception e ) { return true; }
   }

   /** G�n�ration de la fin de l'ent�te FITS, c�d le END et le byte de bourrage
    * pour que cela fasse un multiple de 2880.
    * @param headSize taille actuelle de l'ent�te
    */
   static protected byte [] getEndBourrage(int headSize) {
      int size = 2880 - headSize%2880;
      if( size<3 ) size+=2880;
      byte [] b = new byte[size];
      b[0]=(byte)'E'; b[1]=(byte)'N';b[2]=(byte)'D';
      for( int i=3; i<b.length; i++ ) b[i]=(byte)' ';
      return b;
   }

   /**
    * Mise en forme d'une ligne pour une ent�te FITS. Prends en compte si la valeur
    * est num�rique, String et m�me �ventuellement String d�j� quot� � la FITS
    * @param key La cl�
    * @param value La valeur
    * @param comment Un �ventuel commentaire, sinon ""
    * @return la chaine de 80 caract�res au format FITS
    */
   static public byte [] getFitsLine(String key, String value, String comment) {
      int i=0,j;
      char [] a;
      byte [] b = new byte[80];

      // Le mot cle
      a = key.toCharArray();
      for( j=0; i<8; j++,i++) b[i]=(byte)( (j<a.length)?a[j]:' ' );

      // La valeur associee
      if( value!=null ) {
         b[i++]=(byte)'='; b[i++]=(byte)' ';

         a = value.toCharArray();

         // Valeur num�rique => alignement � droite
         if( !isFitsString(value) ) {
            for( j=0; j<20-a.length; j++)  b[i++]=(byte)' ';
            for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];

         // Chaine de caract�res => formatage
         } else {
            a = formatFitsString(a);
            for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];
            while( i<30 ) b[i++]=(byte)' ';
         }
      }

      // Le commentaire
      if( comment!=null && comment.length()>0 && i<80-3 ) {
         if( value!=null ) { b[i++]=(byte)' ';b[i++]=(byte)'/'; b[i++]=(byte)' '; }
         a = comment.toCharArray();
         for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte) a[j];
      }

      // Bourrage
      while( i<80 ) b[i++]=(byte)' ';

      return b;
   }

   /** Retourne la chaine au format FITS 80 caract�res */
   protected byte[] getFullFitsLine(String s) {
      byte[] b = new byte[80];
      char[] a = s.toCharArray();
      int i;
      for( i=0; i<a.length && i<80; i++ ) b[i]=(byte)a[i];
      while( i<80 ) b[i++]=(byte)' ';

      return b;

   }

   /** Recuperation des infos liees a une source sous forme TSV */
   static protected String getTSV(Source o) { return getSourceInfo(o,"\t"); }

   /** Recuperation des infos liees a une source sous forme TSV */
   static protected String getInfo(Source o) { return getSourceInfo(o," / "); }

   /** Recuperation des infos liees a une source sous forme d'une chaine d'infos s�par�es
    * par "sep" (deballage des marques GLU) */
    static protected String getSourceInfo(Source o,String sep) {
       StringBuffer s = new StringBuffer();

       StringTokenizer st = new StringTokenizer(o.info,"\t");
       st.nextElement();     // On saute le triangle
       while( st.hasMoreTokens() ) {
          Words w = new Words(st.nextToken());
          if( s.length()!=0 ) s.append(sep);
          s.append(w.getText());
       }

       return s.toString();
    }

  /** Construction d'un header TSV A a partir d'une legende
    */
   protected String getShortHeader(Legende leg) {
      int i;
      StringBuffer s = new StringBuffer();

      for( i=0; i<leg.field.length; i++ ) {
         if( i>0 ) s.append("\t");
         s.append(leg.field[i].name);
      }
      s.append(CR);
      for( i=0; i<leg.field.length; i++ ) {
         int j;
         if( i>0 ) s.append("\t");
         try { j = Integer.parseInt(leg.field[i].width); }
         catch(Exception e) { j=0; }
         if( j==0 ) j=10;
         for( int k=0; k<j; k++ ) s.append("-");
      }
      s.append(CR);

      return s.toString();
   }

   // Gestion des evenement
   @Override
public boolean action(Event evt, Object what) {

      if( CHOICE[0].equals(what) ) { aladin.printer(); hide(); }
      else if( CHOICE[1].equals(what) ) saveFile(1,getCodedFormat(format.getSelectedIndex()),-1);
      else if( CHOICE[2].equals(what) ) saveFile(0);
      else if( CHOICE[3].equals(what) ) exportPlans();
      else if( CHOICE[4].equals(what) ) aladin.saveHTML();
      else if( evt.target instanceof Checkbox && tsvCb!=null ) changeCatFormat();
      else if( evt.target instanceof Checkbox && fitsCb!=null ) changeImgFormat();
      return true;
   }

   public void actionPerformed(ActionEvent e) {
      if( e.getSource() instanceof JRadioButton ) {
         if( tsvCb!=null ) changeCatFormat();
         if( fitsCb!=null ) changeImgFormat();
      }
   }

   // Gestion des evenement
   @Override
public boolean handleEvent(Event e) {

      // On supprime le frame
      if( e.id==Event.WINDOW_DESTROY ) hide();

      return super.handleEvent(e);
   }

   /**
    * Classe filtrant une sortie JPEG afin d'ins�rer � la vol�e un segment commentaire
    * juste en d�but de fichier
    */
   class JpegOutputFilter extends OutputStream {
      OutputStream out;
      String comment;
      boolean first;

      JpegOutputFilter(OutputStream out,String comment) {
         this.out=out;
         this.comment=comment;
         first=true;
      }

      /** Insertion du commentaire */
      private void insertComment() throws IOException {

         // Marker de commentaire 0xFFFE
         buf[0] = (byte)0xFF; buf[1]=(byte)0xFE;

         // suivi de la taille du commentaire +2
         int len = comment.length();
         buf[2] = (byte)( (len+2)>>8 & 0xFF ); buf[3] = (byte)( (len+2) & 0xFF );

         // Ecriture
         out.write(buf,0,4);
         out.write(comment.getBytes(),0,len);
      }

      private final byte buf[] = new byte[4];

      @Override
      public void write(byte b[], int off, int len) throws IOException {

         // Pour �viter de planter si le magic code est pass� en deux fois (pas grand risque)
         // dans ces cas-l� on laisse tomber pour le moment
         if( first && len<2 ) first=false;

         // Insertion d'un commentaire
         if( first ) {
            out.write(b,off,2);   // marker de d�but => 0xFFD8
            insertComment();
            first=false;
            if( len-2>0 ) out.write(b,off+2,len-2);  // Le reste du bloc (s'il y a)

            // Puis simple recopie
         } else out.write(b,off,len);
      }

      @Override
      public void write(int b) throws IOException {
         buf[0] = (byte)( b>>24 & 0xFF ); buf[1] = (byte)( b>>16 & 0xFF );
         buf[2] = (byte)( b>>8 & 0xFF );  buf[3] = (byte)( b & 0xFF );
         write(buf,0,4);
      }

      @Override
      public void flush() throws IOException {
         out.flush();
      }

      @Override
      public void close() throws IOException {
         super.close();
         out.close();
      }
   }
   
   final String COMPNG = "tEXtComment";
   
   /**
    * Classe filtrant une sortie PNG afin d'ins�rer � la vol�e un segment commentaire
    * juste en d�but de fichier
    */
   class PNGOutputFilter extends OutputStream {
      OutputStream out;
      byte [] comment;
      boolean writeComment=false;
      CRC32 crc = new CRC32();

      PNGOutputFilter(OutputStream out,String s) {
         this.out=out;
         int n = s.length();
         comment=new byte[n+12];  // 4 tEXt + 7 comment + 1 \0
         for( int i=0; i<COMPNG.length(); i++ ) comment[i] = (byte)COMPNG.charAt(i);
         comment[11]=0;
         System.arraycopy(s.getBytes(),0,comment,12,n);
      }

      /** Insertion du commentaire */
      private void insertComment() throws IOException {

         // Taille du commentaire
         int len = comment.length-4; // -' parce qu'il ne faut pas compter "tEXt"
         buf[0] = (byte)( len>>24 & 0xFF ); buf[1] = (byte)( len>>16 & 0xFF );
         buf[2] = (byte)( len>>8 & 0xFF );  buf[3] = (byte)( len & 0xFF );

         // Ecriture
         out.write(buf,0,4);
         out.write(comment,0,comment.length);
         
         //Calcul du CRC
         crc.reset();
         crc.update(comment);
         long c = crc.getValue();
//         System.out.println("CRC=["+c+"]");
         buf[0] = (byte)( c>>24 & 0xFF ); buf[1] = (byte)( c>>16 & 0xFF );
         buf[2] = (byte)( c>>8 & 0xFF );  buf[3] = (byte)( c & 0xFF );
         out.write(buf,0,4);
      }

      private final byte buf[] = new byte[8];
      
      private int mode=0;  // Etat courant de la machine � �tat rep�rant l'emplacement de l'insertion du comment
      private int offset=0; // offset courant depuis le d�but du fichier
      private int sizeHeader=0; // Taille de l'ent�te PNG
      private int header=0; // emplacement courant dans le header
      
      // Pour trouver le premier emplacement o� l'on peut ins�rer le commentaire
      // m�me si les bytes sont fournis en plusieurs fois
      // @return l'emplacement de l'insertion en comptant depuis "off". -1 si toujours pas bon
      private int endOfHeader(byte b[], int off, int len) {
         for( int i=0; i<len; i++,offset++ ) {
            switch(mode) {
               case 0: // Dans le magic code
                       if( offset==7 ) mode=1;
                       break;
               case 1: // Dans la taille du segment IHDR
                       sizeHeader = (sizeHeader<<8) | (int)b[off+i] & 0xFF;
                       if( offset==11 ) {
                          mode=2; 
//                          System.out.println("Segment HEADER size="+sizeHeader);
                       }
                       break;
               case 2: // Dans la signature du segment, le segment et le CTR
                       header++;
                       if( header==sizeHeader+8 ) return i+1; // On a fini l'ent�te, on pourra ins�rer le commentaire
                       break;
                     
            }
         }
         return -1;
      }

      @Override
      public void write(byte b[], int off, int len) throws IOException {

         // Jusqu'� avoir ins�rer le commentaire
         if( !writeComment ) {
            int fold = endOfHeader(b,off,len);
            if( fold>=0 ) {
               out.write(b,off,fold);
               insertComment();
               out.write(b,off+fold,len-fold);
               writeComment=true;
               
            } else out.write(b,off,len);

         // Puis simple recopie
         } else out.write(b,off,len);
      }

      @Override
      public void write(int b) throws IOException {
         buf[0] = (byte)( b>>24 & 0xFF ); buf[1] = (byte)( b>>16 & 0xFF );
         buf[2] = (byte)( b>>8 & 0xFF );  buf[3] = (byte)( b & 0xFF );
         write(buf,0,4);
      }

      @Override
      public void flush() throws IOException {
         out.flush();
      }

      @Override
      public void close() throws IOException {
         out.close();
      }
   }
}
