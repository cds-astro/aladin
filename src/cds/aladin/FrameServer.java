// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import org.kxml2.io.KXmlParser;
import org.xmlpull.v1.XmlPullParserException;

import cds.tools.TwoColorJTable;
import cds.tools.Util;

/**
 * Gestion de la fenetre associee à la sélection des servers VO pour AllVO
 *
 * @author Pierre Fernique [CDS]
 * @version 1.0 : (28 nov 2005) Creation
 */
public final class FrameServer extends JFrame implements ActionListener,KeyListener {
   static String TITLE,INFO = " ? ",CHECK,UNCHECK,SUBMIT,CLOSE,
                 CHECKUNCHECK,IMGSRV,CATSRC,SPECSRV,
                 TIPSUBMIT,TIPCLOSE,FILTER,RESET,GO;

   // Les references aux objets
   Aladin aladin;
   ServerAllVO discoveryServer;
   JTextField filter=null;

   protected void createChaine() {
      TITLE = aladin.chaine.getString("FSTITLE");
      CHECK = aladin.chaine.getString("FSCHECK");
      UNCHECK = aladin.chaine.getString("FSUNCHECK");
      GO = aladin.chaine.getString("FSGO");
      RESET = aladin.chaine.getString("RESET");
      FILTER = aladin.chaine.getString("FSFILTER");
      SUBMIT = aladin.chaine.getString("FSSUBMIT");
      CLOSE = aladin.chaine.getString("CLOSE");
      CHECKUNCHECK = aladin.chaine.getString("FSCHECKUNCHECK");
      IMGSRV = aladin.chaine.getString("FSIMGSRV");
      CATSRC = aladin.chaine.getString("FSCATSRC");
      SPECSRV = aladin.chaine.getString("FSSPECSRV");
      TIPSUBMIT = aladin.chaine.getString("TIPSUBMIT");
      TIPCLOSE = aladin.chaine.getString("TIPCLOSE");
   }

  /** Creation du Frame gerant la liste des serveurs IVOA + locaux
   * (boutons "DETAILS" du formulaire AllVO)
   */
   protected FrameServer(Aladin aladin,ServerAllVO discoveryServer) {
      super();
      this.aladin=aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(TITLE);
      this.discoveryServer=discoveryServer;
      this.aladin = aladin;
      setLocation(Aladin.computeLocation(this));

      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false, aladin);

      getContentPane().add(createPanel(),"Center");
      pack();
      setVisible(true);
      discoveryServer.clearStepLabel();
   }

   private JPanel panelScroll;
   private static final String QUERY_NVO = "Query NVO registry";
   private static final String QUERY_WORKSHOP_REGISTRY = "Query workshop registry";
   private static final String REFRESH = "Refresh";
   private static final String MODIFY_ENDPOINT = "Modify registry endpoint";
   private ButtonGroup registryCbg;

   public Dimension getPreferredSize() { return new Dimension(600,800); }

   GridBagLayout g;
   GridBagConstraints c;

   // Le panel de la liste des serveurs
   private JPanel createPanel() {
      JPanel p = new JPanel();
      p.setLayout( new BorderLayout(0,0) );

      int scrollWidth = 650;
      int scrollHeight = 500;

      JPanel check = new JPanel();
      JButton b;
      check.add(b=new JButton(CHECK));    b.addActionListener(this);
      check.add(b=new JButton(UNCHECK));  b.addActionListener(this);
      check.add(new JLabel("      "+FILTER+": "));
      filter = new JTextField(15);
      check.add(filter); filter.addKeyListener(this);
      check.add(b=new JButton(GO));     b.addActionListener(this);
      check.add(b=new JButton(RESET));  b.addActionListener(this);

      JPanel header = new JPanel( new BorderLayout());
      header.add(new JLabel(CHECKUNCHECK,JLabel.CENTER),"North");
      header.add(check,"Center");
      JPanel foo = new JPanel(new FlowLayout());
      registryCbg = new ButtonGroup();
      JRadioButton r;
      foo.add(r=new JRadioButton(QUERY_NVO));
      registryCbg.add(r);
      r.setSelected(true);
      r.setActionCommand(QUERY_NVO);
      foo.add(r=new JRadioButton(QUERY_WORKSHOP_REGISTRY));
      registryCbg.add(r);
      r.setActionCommand(QUERY_WORKSHOP_REGISTRY);
      foo.add(b=new JButton(REFRESH));           b.addActionListener(this);
      foo.add(b=new JButton(MODIFY_ENDPOINT));   b.addActionListener(this);
      if( Aladin.PROTO ) header.add(foo, "South");

      p.add(header,"North");


      g = new GridBagLayout();
      c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;

      panelScroll = new JPanel(g);
//      panelScroll.setLayout(new GridLayout(0,1,0,0));
      JScrollPane scroll = new JScrollPane(panelScroll);
      scroll.setSize(scrollWidth,scrollHeight);
      scroll.setBackground(Color.white);
      scroll.getVerticalScrollBar().setUnitIncrement(70);

      fillWithServers();

      Aladin.makeAdd(p,scroll,"Center");

      JPanel submit = new JPanel();
      submit.add(b=new JButton(SUBMIT));
      b.addActionListener(this);
      b.setToolTipText(TIPSUBMIT);
      submit.add(b=new JButton(CLOSE));
      b.addActionListener(this);
      b.setToolTipText(TIPCLOSE);
      p.add(submit,"South");

      return p;
   }

   private void fillWithServers() {

      panelScroll.removeAll();

      for( int j=0; j<3; j++ ) {
         int type = j==0 ? Server.IMAGE : j==1 ? Server.CATALOG : Server.SPECTRUM;
         c.gridx=0;
         c.gridwidth=4;
         c.gridy++;
         c.weightx=1;
         c.weighty=0.1;
         int gapy = c.insets.top;
         c.insets.top=15;
//         if( j>0 ) { Filet f = new Filet(); g.setConstraints(f, c); panelScroll.add( f); c.gridy++; }
         JLabel t = new JLabel( j==0 ? IMGSRV : j==1 ? CATSRC : SPECSRV);
         t.setFont(Aladin.LITALIC);
         JPanel p = new JPanel(new BorderLayout(0,0));
         p.add(t);
         p.setBackground(j==0 ? MetaDataTree.LABEL_COL[ResourceNode.IMAGE] :
            j==1 ? MetaDataTree.LABEL_COL[ResourceNode.CAT] :
               MetaDataTree.LABEL_COL[ResourceNode.SPECTRUM]);
         g.setConstraints(p, c);
         panelScroll.add(p);
         c.insets.top=gapy;
         int h=0;
         int height;

         // Génération du masque de filtrage
         String mask = filter.getText().trim();
         if( mask.length()==0 ) mask=null;

         for( int i=0; i<aladin.dialog.server.length; i++ ) {
            Server s = aladin.dialog.server[i];
            if( !s.isDiscovery() ) continue;
            if( s.type!=type ) continue;
            if( mask!=null ) if( Util.indexOfIgnoreCase(s.description, mask)<0 ) { s.filterAllVO=false; continue; }
            s.filterAllVO=true;
            h++;
            if( s.cbAllVO==null ) {
               s.cbAllVO = new JCheckBox(s.description);
               s.cbAllVO.setSelected(true);
               s.cbAllVO.setOpaque(true);
            }
            height = s.cbAllVO.getPreferredSize().height;
            s.cbAllVO.setPreferredSize(new Dimension(330,height));

            s.statusAllVO.setFont(Aladin.ITALIC);
            s.statusAllVO.setOpaque(true);
            s.statusAllVO.setPreferredSize(new Dimension(50,height));

            Color bg = h%2==0?TwoColorJTable.DEFAULT_ALTERNATE_COLOR:getBackground();

            if( s.statusReport==null ) {
               s.statusReport = new JButton(INFO);
               s.statusReport.setMargin(new Insets(0,0,0,0) );
               s.statusReport.setPreferredSize(new Dimension(20,height));
               s.statusReport.addActionListener(this);
               s.statusReport.setOpaque(true);
            }

            c.gridx=0;
            c.gridy++;
            c.gridwidth=1;
            c.weightx=0.1;
            JLabel l = new JLabel((h<10?"  ":"")+h+")");
            l.setOpaque(true);
            l.setBackground(bg);
            l.setPreferredSize(new Dimension(l.getPreferredSize().width,height));
            g.setConstraints(l, c);
            panelScroll.add(l);

            c.gridx++;
            c.weightx=0.6;
            s.cbAllVO.setBackground(bg);
            g.setConstraints(s.cbAllVO, c);
            panelScroll.add(s.cbAllVO);

            c.weightx=0.2;
            c.gridx++;
            s.statusAllVO.setBackground(bg);
            g.setConstraints(s.statusAllVO, c);
            panelScroll.add(s.statusAllVO);

            c.weightx=0.05;
            c.gridx++;
            int gap = c.insets.left;
            c.insets.left=5;
            if( s.statusReport==null ) {
               l = new JLabel("");
               l.setOpaque(true);
               l.setBackground(bg);
               g.setConstraints(l, c);
               panelScroll.add(l);
            } else {
               s.statusReport.setBackground(bg);
               g.setConstraints(s.statusReport, c);
               panelScroll.add(s.statusReport);
            }
            c.insets.left=gap;

            // mise en relief des serveurs provenant réellement du registry VO
            if( ! (s instanceof ServerGlu && ((ServerGlu)s).gluTag.startsWith("IVOA")) ) {
               s.cbAllVO.setFont(s.cbAllVO.getFont().deriveFont(Font.BOLD));
            }
         }

      }
      // Bourrage
      c.gridy++;
      c.gridwidth=4;
      c.weighty=0.9;
      JLabel l1 = new JLabel(" ");
      g.setConstraints(l1, c);
      panelScroll.add(l1);

      panelScroll.invalidate();
      panelScroll.repaint();
   }


   public void actionPerformed(ActionEvent e) {
      Object o = e.getSource();

      if( !(o instanceof JButton) ) return;

      String s = ((JButton)o).getActionCommand();

      // Affichage du selecteur de fichiers
      if( s.equals(INFO )) {
         for( int i=0; i<aladin.dialog.server.length; i++ ) {
            if( o!=aladin.dialog.server[i].statusReport) continue;
            aladin.dialog.server[i].showStatusReport();
         }
      } else if( s.equals(CHECK) || s.equals(UNCHECK)) {
         boolean flag = s.equals(CHECK);
         check(-1,flag);
      } else if( s.equals(CLOSE) )  { setVisible(false);
      } else if( s.equals(GO) )     { go();
      } else if( s.equals(RESET) )  { reset();
      } else if( s.equals(REFRESH) ){ refresh();
      } else if( s.equals(SUBMIT) ) {
//         aladin.dialog.setCurrent(discoveryServer.nom);
         discoveryServer.tree.clear();
         discoveryServer.submit();
      } else if( s.equals(MODIFY_ENDPOINT) ) { modifyEndpoint();
      }
   }

   private void go() {
      fillWithServers();
      pack();
   }

   private void reset() {
      filter.setText("");
      for( int i=0; i<aladin.dialog.server.length; i++ ) {
         aladin.dialog.server[i].statusError=null;
         if( aladin.dialog.server[i].statusAllVO!=null ) aladin.dialog.server[i].statusAllVO.setText(" ");
      }
      fillWithServers();
      check(-1,true);
   }

   /** Coche ou non (flag) tous les serveurs d'un type donnée (Server.CATALOG... (-1 pour tous)) */
   protected void check(int type,boolean flag) {
      for( int i=0; i<aladin.dialog.server.length; i++ ) {
         if( flag && !aladin.dialog.server[i].filterAllVO ) continue;
         if( aladin.dialog.server[i].cbAllVO==null ) continue;
         if( type!=-1 && aladin.dialog.server[i].type!=type ) continue;
         aladin.dialog.server[i].cbAllVO.setSelected(flag);
      }
   }

   private void modifyEndpoint() {
	   String endpoint = (String)JOptionPane.showInputDialog(this, "Enter OAI endpoint (eg http://myserver.org/oai.pl) :", "Registry OAI endpoint", JOptionPane.QUESTION_MESSAGE, null, null, REGISTRY_BASE_URL);

	   if( endpoint!=null ) REGISTRY_BASE_URL = endpoint;
   }

   private void refresh() {
      Aladin.makeCursor(this, Aladin.WAITCURSOR);

      panelScroll.removeAll();
      removeIVOA();

      if( registryCbg.getSelection().getActionCommand().equals(QUERY_NVO) ) {
         aladin.dialog.ivoaServersLoaded = false;
         aladin.dialog.appendIVOAServer();
      } else {
         appendWorkshopRegistryServers(REGISTRY_BASE_URL, null);
      }

      fillWithServers();

      Aladin.makeCursor(this, Aladin.DEFAULTCURSOR);
   }

   // set=ivo_managed permet de ne récupérer que les enregistrements gérés par ce registry
//   static protected String REGISTRY_BASE_URL = "http://vops1.hq.eso.org:8080/registry/OAIHandlerv1_0?set=ivo_managed";
   static protected String REGISTRY_BASE_URL = "http://manaslu.star.le.ac.uk/astrogrid-registry/OAIHandlerv1_0?set=ivo_managed";
   private void appendWorkshopRegistryServers(String registry, String resToken) {
	   URL url;
	   try {
		   String suffix;
		   if( resToken!=null ) suffix = "resumptionToken="+URLEncoder.encode(resToken);
		   else suffix = "metadataPrefix=ivo_vor";

		   url = new URL(registry+"&verb=ListRecords&"+suffix);

//		   url = new URL(REGISTRY_BASE_URL+"?verb=ListRecords&metadataPrefix=ivo_vor");
	   }
	   catch(MalformedURLException e) {
		   e.printStackTrace();
		   return;
	   }
	   DataInputStream dis = null;
	   try {

		   dis = new DataInputStream(new BufferedInputStream(url.openStream()));
		   String data;

		   MyByteArrayStream bas = new MyByteArrayStream();
		   String type = null;
		   String actionName = null;
		   String institute = null;
		   String baseUrl = null;
		   String desc = null;
		   String identifier = null;
		   String resumptionToken = null;
		   String status = null;

		   KXmlParser parser = new KXmlParser();
		   parser.setInput(new InputStreamReader(dis));
		   int event = KXmlParser.START_DOCUMENT;
		   String tagName;
		   boolean startTag;
		   boolean endTag;

		   while( (event=parser.next()) != KXmlParser.END_DOCUMENT ) {

//			   System.out.println(parser.getText());


			   if( event!=KXmlParser.START_TAG && event!=KXmlParser.END_TAG ) continue;
//
			   tagName = parser.getName();
			   startTag = event==KXmlParser.START_TAG;
			   endTag = event==KXmlParser.END_TAG;


			   if( startTag && ( tagName.indexOf("ri:Resource")>=0 || tagName.indexOf("vr:Resource")>=0 || tagName.equalsIgnoreCase("resource") ) ) {
				   status = parser.getAttributeValue(null, "status");

				   // on ne conserve pas les services dont le statut est "inactive" ou "deleted"
				   if( status==null || ! status.equals("active") ) continue;

				   type = parser.getAttributeValue(null, "xsi:type");
				   if( type!=null ) type = type.trim();
//				   System.out.println("type : "+type);
			   }
			   else if( startTag && tagName.equalsIgnoreCase("capability")  ) {
				   if( (type==null || supportType(type)==null) && (status==null || status.equals("active") ) ) {
					   type = parser.getAttributeValue(null, "xsi:type");
					   if( type!=null ) type = type.trim();
				   }
			   }
			   else if( startTag && ( tagName.indexOf("vr:shortName")>=0 || tagName.equalsIgnoreCase("shortName") ) ) {
				   parser.next();
				   actionName = parser.getText();
				   if( actionName!=null ) actionName = actionName.trim();
//				   System.out.println("action name : "+actionName);
			   }
			   else if( startTag && ( tagName.indexOf("vr:publisher")>=0 || tagName.equalsIgnoreCase("publisher") ) ) {
				   parser.next();
				   institute = parser.getText();
				   if( institute!=null ) institute = institute.trim();
//				   System.out.println("institute : "+institute);
			   }
			   else if( startTag && tagName.indexOf("vr:accessURL")>=0 ) {
				   parser.next();
				   baseUrl = parser.getText();
				   if( baseUrl!=null ) baseUrl = baseUrl.trim();
//				   System.out.println("Base Url : "+baseUrl);
			   }
			   else if( startTag && tagName.toLowerCase().indexOf("accessurl")>=0 && baseUrl==null ) {
				   parser.next();
                   baseUrl = parser.getText();
                   if( baseUrl!=null ) baseUrl = baseUrl.trim();
			   }
			   else if( startTag && ( tagName.indexOf("vr:title")>=0 || tagName.equalsIgnoreCase("title") ) ) {
				   parser.next();
				   desc = parser.getText();
				   if( desc!=null ) desc = desc.trim();
//				   System.out.println("Desc : "+desc);
			   }
			   else if( startTag && ( tagName.indexOf("vr:identifier")>=0 || tagName.equalsIgnoreCase("identifier") ) ) {
				   parser.next();
				   identifier = parser.getText();
				   if( identifier!=null ) identifier = identifier.trim();
//				   System.out.println("Identifier : "+identifier);
			   }
			   // création éventuelle d'un nouvel enregistrement GLU
			   else if( endTag && ( tagName.indexOf("oai:record")>=0 || tagName.equalsIgnoreCase("resource") ) ) {
				   String t;
				   // on vérifie s'il s'agit d'un record ConeSearch, SIA ou SSA
				   if( (t=supportType(type))!=null ) {
					   createRecord(bas, t, actionName, institute, baseUrl, desc, identifier);
				   }

				   status = type = actionName = institute = baseUrl = desc = identifier = null;
			   }
			   // resumptionToken
			   else if( startTag && tagName.indexOf("resumptionToken")>=0 ) {
				   parser.next();
				   resumptionToken = parser.getText();
			   }
		   }

		   /*
		   while( (data=dis.readLine())!=null ) {
			   System.out.println(data);

			   if( data.indexOf("<ri:Resource")>=0 || data.indexOf("<vr:Resource")>=0 ) {
				   type = getServiceType(data);
				   System.out.println("type : "+type);
			   }
			   else if( data.indexOf("<vr:shortName")>=0 ) {
				   actionName = getActionName(data);
//				   System.out.println("action name : "+actionName);
			   }
			   else if( data.indexOf("<vr:publisher")>=0 ) {
				   institute = getInstitute(data);
//				   System.out.println("institute : "+institute);
			   }
			   else if( data.indexOf("<vr:accessURL")>=0 ) {
				   baseUrl = getBaseUrl(data);
//				   System.out.println("Base Url : "+baseUrl);
			   }
			   else if( data.indexOf("<vr:title>")>=0 ) {
				   desc = getDesc(data);
//				   System.out.println("Desc : "+desc);
			   }
			   else if( data.indexOf("<vr:identifier>")>=0 ) {
				   identifier = getIdentifier(data);
//				   System.out.println("Identifier : "+identifier);
			   }
			   // création éventuelle d'un nouvel enregistrement GLU
			   else if( data.indexOf("</oai:record>")>=0 ) {
				   String t;
				   // on vérifie s'il s'agit d'un record ConeSearch, SIA ou SSA
				   if( (t=supportType(type))!=null ) {
					   createRecord(bas, t, actionName, institute, baseUrl, desc, identifier);
				   }

				   type = actionName = institute = baseUrl = desc = identifier = null;
			   }
			   // resumptionToken
			   else if( data.indexOf("<resumptionToken")>=0 ) {
				   resumptionToken = getResumptionToken(data);
			   }
		   }
		   */

//		   System.out.println("Resumption token : "+resumptionToken);

		   if( resumptionToken!=null ) {
			   appendWorkshopRegistryServers(registry, resumptionToken);
		   }

		   aladin.dialog.appendServersFromStream(bas.getInputStream());
	   }
	   catch(IOException ioe) {
		   ioe.printStackTrace();
	   }
	   catch(XmlPullParserException xppe) {
		   xppe.printStackTrace();
	   }
	   catch(Exception e) {
		   e.printStackTrace();
	   }
	   finally {
		   try {
			   dis.close();
		   }
		   catch(IOException ioe) {System.out.println("can't close");}
	   }

   }

   // TODO : refactoring nécessaire !!
//   private static final String URL_RESOLVE_IVORN = "http://nvo.stsci.edu/VORegistry/registry.asmx/QueryVOResource?predicate=identifier%3D%27";
   private static final String URL_RESOLVE_IVORN = "http://esavo.esa.int/registry/GetResource.jsp?identifier=";
//   private static final String URL_RESOLVE_IVORN = "http://vizier.u-strasbg.fr/viz-bin/registry/vizier/oai_v0.10.pl?verb=GetRecord&metadataPrefix=ivo_vor&identifier=";

   /**
    * Interroge le registry pour retourner les objets IVORN correspondant aux identifiants passés en paramètre
    * @param uri
    */
   static protected VOResource[] getGluStreamFromIvornList(String[] uri) {
       ArrayList l = new ArrayList();
       VOResource ivorn;
       for( int i=0; i<uri.length; i++ ) {
           ivorn = getIvorn(uri[i]);
           if( ivorn!=null ) System.out.println(ivorn.desc);
       }
       return (VOResource[])l.toArray(new VOResource[l.size()]);
   }

   static protected VOResource getIvorn(String uri) {
       URL url;
       try {
           String urlSuffix = "";
//           urlSsuffix = "%27"; // pour stsci
           url = new URL(URL_RESOLVE_IVORN+URLEncoder.encode(uri)+urlSuffix);
//           System.out.println(url);
       }
       catch(MalformedURLException e) {
           e.printStackTrace();
           return null;
       }
       DataInputStream dis = null;
       try {

           dis = new DataInputStream(new BufferedInputStream(url.openStream()));
           String data;



           String type = null;
           String actionName = null;
           String institute = null;
           String baseUrl = null;
           String desc = null;
           String identifier = null;
           String resumptionToken = null;
           String status = null;

           KXmlParser parser = new KXmlParser();
           parser.setInput(new InputStreamReader(dis));
           int event = KXmlParser.START_DOCUMENT;
           String tagName;
           boolean startTag;
           boolean endTag;


           while( (event=parser.next()) != KXmlParser.END_DOCUMENT ) {


//             System.out.println(parser.getText());


               if( event!=KXmlParser.START_TAG && event!=KXmlParser.END_TAG ) continue;
//
               tagName = parser.getName();
               startTag = event==KXmlParser.START_TAG;
               endTag = event==KXmlParser.END_TAG;

//               System.out.println(tagName);

               if( startTag && ( tagName.indexOf("ri:Resource")>=0 || tagName.indexOf("vr:Resource")>=0 || tagName.equalsIgnoreCase("resource") ) ) {
                   status = parser.getAttributeValue(null, "status");

                   // on ne conserve pas les services dont le statut est "inactive" ou "deleted"
                   if( status==null || ! status.equals("active") ) continue;

                   type = parser.getAttributeValue(null, "xsi:type");
                   if( type!=null ) type = type.trim();
//                 System.out.println("type : "+type);
               }
               else if( startTag && tagName.equalsIgnoreCase("capability")  ) {
                   if( (type==null || supportType(type)==null) && (status==null || status.equals("active") ) ) {
                       type = parser.getAttributeValue(null, "xsi:type");
                       if( type!=null ) type = type.trim();
                   }
               }
               else if( startTag && ( tagName.indexOf("vr:shortName")>=0 || tagName.equalsIgnoreCase("shortName") ) ) {
                   parser.next();
                   actionName = parser.getText();
                   if( actionName!=null ) actionName = actionName.trim();
//                 System.out.println("action name : "+actionName);
               }
               else if( startTag && ( tagName.indexOf("vr:publisher")>=0 || tagName.equalsIgnoreCase("publisher") ) ) {
                   parser.next();
                   institute = parser.getText();
                   if( institute!=null ) institute = institute.trim();
//                 System.out.println("institute : "+institute);
               }
               else if( (startTag && tagName.indexOf("vr:accessURL")>=0 ) && (baseUrl==null || baseUrl.length()==0 ) ) {
                   parser.next();
                   baseUrl = parser.getText();
                   if( baseUrl!=null ) baseUrl = baseUrl.trim();
//                   System.out.println("BASEURL : "+baseUrl);
               }
               else if( startTag && (tagName.toLowerCase().indexOf("accessurl")>=0  ) && (baseUrl==null || baseUrl.length()==0 ) ) {
                   parser.next();
                   baseUrl = parser.getText();
                   if( baseUrl!=null ) baseUrl = baseUrl.trim();
//                   System.out.println("BASEURL : "+baseUrl);
               }
               else if( startTag && ( tagName.indexOf("vr:title")>=0 || tagName.equalsIgnoreCase("title") ) ) {
                   parser.next();
                   desc = parser.getText();
                   if( desc!=null ) desc = desc.trim();
//                 System.out.println("Desc : "+desc);
               }
               else if( startTag && ( tagName.indexOf("vr:identifier")>=0 || tagName.equalsIgnoreCase("identifier") ) ) {
                   parser.next();
                   identifier = parser.getText();
                   if( identifier!=null ) identifier = identifier.trim();
//                 System.out.println("Identifier : "+identifier);
               }
               // création éventuelle d'un nouvel enregistrement GLU
               else if( endTag && ( tagName.indexOf("oai:record")>=0 || (tagName.indexOf("Resource")>=0 && tagName.toLowerCase().indexOf("related")<0) ) ) {
                   String t = supportType(type);
                   // on vérifie s'il s'agit d'un record ConeSearch, SIA ou SSA
                   VOResource ivorn = new VOResource();
                   ivorn.type = t!=null?t:type;
                   ivorn.actionName = actionName;
                   ivorn.baseUrl = baseUrl;
                   ivorn.desc = desc;
                   ivorn.identifier = identifier;
                   return ivorn;

               }
               // resumptionToken
               else if( startTag && tagName.indexOf("resumptionToken")>=0 ) {
                   parser.next();
                   resumptionToken = parser.getText();
               }
           }




       }
       catch(IOException ioe) {
           ioe.printStackTrace();
       }
       catch(XmlPullParserException xppe) {
           xppe.printStackTrace();
       }
       catch(Exception e) {
           e.printStackTrace();
       }
       finally {
           try {
               dis.close();
           }
           catch(IOException ioe) {System.out.println("can't close");}
       }
       return null;
   }


   static protected void createRecord(MyByteArrayStream bas, String type, String actionName,
           String institute, String baseUrl, String desc, String identifier) {

       createRecord(bas, type, actionName, institute, baseUrl, desc, identifier, "IVOA");
   }

   static protected void createRecord(MyByteArrayStream bas, String type, String actionName,
		                     String institute, String baseUrl, String desc, String identifier, String prefix) {

//	   System.out.println(actionName);
//	   System.out.println(baseUrl);
//	   System.out.println("*"+type+"*");
//
//	   if( actionName==null ) actionName = identifier;
//
//	   if( actionName!=null ) actionName = actionName.replace(' ', '-');

	   bas.write("%ActionName"+"\t"+prefix+"-"+type+"-"+identifier);
	   bas.write("\n");

	   bas.write("%Identifier"+"\t"+identifier);
	   bas.write("\n");

	   bas.write("%Institute"+"\t"+institute);
	   bas.write("\n");

	   bas.write("%Description"+"\t"+desc.substring(0, Math.min(60,desc.length())));
	   bas.write("\n");

	   bas.write("%Owner\tCDS'aladin");
	   bas.write("\n");

       if (baseUrl != null) {
            baseUrl = baseUrl.trim();
            if (baseUrl.indexOf('?') < 0) {
                baseUrl += "?";
            }
            if (!baseUrl.endsWith("?") && !baseUrl.endsWith("/") && !baseUrl.endsWith("&")) {
                baseUrl += "&";
            }
        }

       // ajout 'REQUEST=queryData' pour URL de base SSAP
       if( baseUrl!=null && type.equals("ssap") ) {
    	   baseUrl += "REQUEST=queryData&";
       }

	   if( type.equals("siap") ) {
           // TODO : il faudra penser à trier, et à virer les items sans aucune calib
//		   bas.write("%Url"+"\t"+baseUrl+"POS=$1,$2&SIZE=$3&FORMAT=image/fits");
           bas.write("%Url"+"\t"+baseUrl+"POS=$1,$2&SIZE=$3");
           bas.write("\n");
	   }
	   else if( type.equals("ssap") ) {
		   bas.write("%Url"+"\t"+baseUrl+"POS=$1,$2&SIZE=$3");
           bas.write("\n");
	   }
	   else if( type.equals("cs") ) {
		   bas.write("%Url"+"\t"+baseUrl+"RA=$1&DEC=$2&SR=$3&VERB=2");
           bas.write("\n");
	   }


	   bas.write("%Param.Description\t$1=Right Ascension");
	   bas.write("\n");

	   bas.write("%Param.Description\t$2=Declination");
	   bas.write("\n");

	   bas.write("%Param.Description\t$3=Radius in deg");
	   bas.write("\n");

	   bas.write("%Param.DataType\t$1=Target(RAd)");
	   bas.write("\n");

	   bas.write("%Param.DataType\t$2=Target(DEd)");
	   bas.write("\n");

	   bas.write("%Param.DataType\t$3=Field(RADIUSd)");
	   bas.write("\n");

	   bas.write("%Param.Value\t$3=0.17");
	   bas.write("\n");

	   if( type.equals("cs") ) {
		   bas.write("%ResultDataType\tMime(text/xml)");
           bas.write("\n");
	   }
	   else if( type.equals("ssap") ) {
		   bas.write("%ResultDataType\tMime(ssa/xml)");
           bas.write("\n");
	   }
	   else if( type.equals("siap") ) {
		   bas.write("%ResultDataType\tMime(sia/xml)");
           bas.write("\n");
	   }


	   bas.write("%Aladin.Label\t"+desc.substring(0, Math.min(60,desc.length())));
	   bas.write("\n");

	   bas.write("%Aladin.Menu\tIVOA...");
	   bas.write("\n");

	   bas.write("%Aladin.LabelPlane\t"+type+actionName+" $1");
	   bas.write("\n");

	   bas.write("%DistribDomain\tALADIN");
	   bas.write("\n");
   }

   private static String supportType(String type) {
	   if( type==null ) return null;

	   type = type.toLowerCase();

	   if( type.indexOf("simpleimageaccess")>=0 ) return "siap";
	   if( type.indexOf("simplespec")>=0 )return "ssap";
	   if( type.indexOf("conesearch")>=0 || type.indexOf("tabularskyservice")>=0 ) return "cs";

	   return null;
   }

   private String getServiceType(String s) {
	   // on ne conserve que les ressources "active"
	   if( s.indexOf("\"active\"")<0 ) return null;

	   int i = s.indexOf("xsi:type=");
	   if( i<0 ) return null;

	   s = s.substring(i+10);
	   i = s.indexOf("\"");
	   if( i>=0 ) s = s.substring(0, i);

	   return s;
   }

   private String getActionName(String s) {
	   int i = s.indexOf("<vr:shortName>");
	   if( i<0 ) return null;

	   s = s.substring(i+14);
	   int j;
	   i = s.indexOf(' ');
	   j = s.indexOf('<');

	   if( i<0 ) return s.substring(0,j);
	   if( j<0 ) return s.substring(0,i);

	   return s.substring(0,Math.min(i,j));
   }

   private String getInstitute(String s) {
	   int i = s.indexOf("<vr:publisher>");
	   if( i<0 ) return null;

	   s = s.substring(i+14);
	   int j;
	   j = s.indexOf('<');

	   if( j>=0 ) return s.substring(0,j);

	   return s;
   }

   private String getBaseUrl(String s) {
	   int i = s.indexOf(">");
	   if( i<0 ) return null;
	   s = s.substring(i+1);

	   i = s.indexOf('<');
	   if( i<0 ) return s;

	   return s.substring(0,i);
   }

   private String getDesc(String s) {
	   int i = s.indexOf(">");
	   if( i<0 ) return null;
	   s = s.substring(i+1);

	   i = s.indexOf('<');
	   if( i<0 ) return s;

	   return s.substring(0,i);

   }

   private String getIdentifier(String s) {
	   int i = s.indexOf(">");
	   if( i<0 ) return null;
	   s = s.substring(i+1);

	   i = s.indexOf('<');
	   if( i<0 ) return s;

	   return s.substring(0,i);
   }

   private String getResumptionToken(String s) {
	   int i = s.indexOf(">");
	   if( i<0 ) return null;
	   s = s.substring(i+1);

	   i = s.indexOf('<');
	   if( i<0 ) return s;

	   return s.substring(0,i);
   }

   private void removeIVOA() {
	   // on cherche le premier serveur "IVOA"
	   Server[] servers = aladin.dialog.server;

	   int i;
	   for( i=0; i<servers.length; i++ ) {
		   if( ! (servers[i] instanceof ServerGlu) ) continue;
		   if( ((ServerGlu)servers[i]).gluTag.startsWith("IVOA") ) break;
	   }

	   Server[] newServers = new Server[i];
	   System.arraycopy(servers, 0, newServers, 0, i);

	   aladin.dialog.server = newServers;
   }



   // Gestion des evenement
//   public boolean handleEvent(Event e) {
//
//      if( e.id==Event.WINDOW_DESTROY ) {
//         hide();
//      }
//
//      return super.handleEvent(e);
//   }

   public void keyPressed(KeyEvent e) { }
   public void keyTyped(KeyEvent e) { }

   public void keyReleased(KeyEvent e) {
      if( e.getKeyCode()==KeyEvent.VK_ENTER ) go();
   }

}
