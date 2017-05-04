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

import static cds.aladin.Constants.ADQLVALUE_FORBETWEEN;
import static cds.aladin.Constants.CHANGESERVER;
import static cds.aladin.Constants.CHECKQUERY;
import static cds.aladin.Constants.COMMA_CHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.LASTPANEL;
import static cds.aladin.Constants.LIMIT_UNKNOWN_MESSAGE;
import static cds.aladin.Constants.NUMBEROFOPTIONS;
import static cds.aladin.Constants.RANGE_DELIMITER;
import static cds.aladin.Constants.REGEX_ARRAY_PRINT;
import static cds.aladin.Constants.REGEX_OPNUM;
import static cds.aladin.Constants.REGEX_RANGEINPUT;
import static cds.aladin.Constants.SHOWAYNCJOBS;
import static cds.aladin.Constants.SODA_IDINDEX;
import static cds.aladin.Constants.SODA_POLINDEX;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.SYNC_ASYNC;
import static cds.aladin.Constants.TABLECHANGED;
import static cds.aladin.Constants.UTF8;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import adql.parser.ADQLParser;
import cds.moc.HealpixMoc;
import cds.tools.Util;

/**
 * Le formulaire d'interrogation d'un serveur defini par enregistrement GLU
 *
 * @author Pierre Fernique [CDS]
 * @version 2.1 : avr 07 - Prise en compte du type MOC
 * @version 2.0 : jan 03 - Suppression du Layout Manager et toilettage
 * @version 1.0 : (23 oct 2000) Creation
 */
public class ServerGlu extends Server implements Runnable {
   String HELP,ERR;
   private boolean flagSIAIDHA=false;
   private boolean flagTAP=false;
   private boolean flagTAPV2=false;
   int fmt;		// Format de retour (PlanImage.fmt)
	String actionName, info1, /* info2, */filter, PARSEMJDFIELDHINT, GENERICERROR, CHECKQUERYTOOLTIP, SYNCASYNCTOOLTIP,
			SHOWASYNCTOOLTIP;
   String system;       // appel système dans le cas d'un enregistrement concernant une application locale, null sinon
   String dir;          // Répertoire d'exécution du system, null sinon
   StringBuffer record;
   Vector vc;		    // Liste des componenents associes aux parametres du tagGlu d'interrogation
   String planeLabel;	// Format pour le label du plan ou null sinon
   String [] vD=null;	// Contient eventuellement la liste des valeurs
  			            // par defaut pour les champs a saisir. L'index
                        // correspond a l'indice du champ. le tableau est null
                        // si aucune valeur sinon seulement les elements sans
                        // valeur par defaut sont null.
   int baseUrlIndex;    // Indice du paramètre dont le type est BaseUrl(URL|MOCID)
   Thread thread;       // pour interrogation asynchrone des serveurs SIA/SSA

   protected int lastY;
   
   HashMap<Integer, String[]> rangeValues = new HashMap<Integer, String[]>();
   private HealpixMoc posBounds = null;
   private Map<String, Vector> tapTableMapping = new HashMap<String, Vector>();
   private Map<String, GluAdqlTemplate> gluAdqlQueryTemplates;
   private String currentSelectedTapTable;
   private Hashtable<String, String> adqlFunc = null;
   private Hashtable<String, String> adqlFuncParams = null;
   JComboBox sync_async;
   String LISTDELIMITER = SPACESTRING;

   protected void createChaine() {
      super.createChaine();
      info1 = aladin.chaine.getString("GLUINFO1");
//      info2 = aladin.chaine.getString("GLUINFO2");
      filter= aladin.chaine.getString("SMBFILTER");
      HELP  = aladin.chaine.getString("GLUHELP");
      ERR   = aladin.chaine.getString("GLUERR");
      PARSEMJDFIELDHINT = Aladin.chaine.getString("PARSEMJDFIELDHINT");
      GENERICERROR = Aladin.chaine.getString("GENERICERROR");
      CHECKQUERYTOOLTIP = Aladin.chaine.getString("CHECKQUERYTOOLTIP");
      SYNCASYNCTOOLTIP = Aladin.chaine.getString("SYNCASYNCTOOLTIP");
      SHOWASYNCTOOLTIP = Aladin.chaine.getString("SHOWASYNCTOOLTIP");
   }

   /**
    * Creation du formulaire d'interrogation du serveur decrit
    * par les champs GLU
    * @param aladin reference
    * @param Les champs GLU
    * @param record simple copie de l'enregistrement GLU original
    * @param tapTables
    * @param gluAdqlTemplate
 * @param tapClient 
    */
	protected ServerGlu(Aladin aladin, String actionName, String description, String verboseDescr, String aladinMenu,
			String aladinMenuNumber, String aladinLabel, String planeLabel, String docUser, String[] paramDescription,
			String[] paramDataType, String[] paramValue, String[][] paramRange, String resultDataType, String institute,
			String[] aladinFilter, String aladinLogo, String dir, String system, StringBuffer record,
			String aladinProtocol, String[] tapTables, GluAdqlTemplate gluAdqlTemplate, TapClient tapClient) {

      this.aladin = aladin;
      createChaine();
      setBackground(Aladin.BLUE);
      
      this.record      = record;
      this.actionName  = actionName;
      this.aladinLogo  = aladinLogo;
      this.aladinLabel = (aladinLabel!=null)?aladinLabel:(description!=null)?description:actionName;
      this.description = description;
      this.verboseDescr= verboseDescr;
      this.aladinMenu  = aladinMenu;
      this.institute   = institute;
      this.planeLabel  = planeLabel;
      this.docUser     = docUser;
      this.ordre       = aladinMenuNumber;
      this.system      = system;
      this.dir         = dir;
      JScrollPane sc=null;
//      try { this.aladinMenuNumber=Double.parseDouble(aladinMenuNumber); } catch( Exception e) { }

      // Dans le cas où le Popup associé est IVOA..., le serveur sera
      // considéré comme caché (accessible uniquement en Discovery mode)
      if( aladinMenu!=null && aladinMenu.equals("IVOA...") ) HIDDEN=true;

      type=IMAGE;
      if( resultDataType!=null ) {
         if( resultDataType.indexOf("ssa")>=0 ) type=SPECTRUM;
         else if( resultDataType.indexOf("moc")>=0 ) type=MOC;
         else if( resultDataType.indexOf("x-votable+xml")>=0 ) type=CATALOG;
         else if( resultDataType.indexOf("application")>=0 ) {
            if( resultDataType.indexOf("image")>0 ) type=APPLIIMG;
            else type=APPLI;
         }
         else if( resultDataType.indexOf("text")>=0 || resultDataType.indexOf("txt")>=0 
               || resultDataType.indexOf("tsv")>=0 || resultDataType.indexOf("csv")>=0 
               || resultDataType.indexOf("xml")>=0) type=CATALOG;
      }
      flagSIAIDHA = resultDataType!=null && (resultDataType.indexOf("sia")>=0 
            || resultDataType.indexOf("idha")>=0  || resultDataType.indexOf("ssa")>=0 );
      flagTAP = aladinProtocol!=null && Util.indexOfIgnoreCase(aladinProtocol, "tap")==0; 
      flagTAPV2 = aladinProtocol!=null && Util.indexOfIgnoreCase(aladinProtocol, "tapv1")==0 && Aladin.PROTO;//TODO:: tintinproto

      if( flagSIAIDHA && type!=SPECTRUM ) type=IMAGE;
      DISCOVERY=flagSIAIDHA || type==SPECTRUM || type==CATALOG;

      fmt = getFmt(resultDataType);

      setLayout(null);
      setFont(Aladin.PLAIN);

      boolean flagInfo=true;
      int y=HEIGHT/2-(paramDescription.length*35+50+60+(docUser!=null?30:0)+(flagSIAIDHA?180:0))/2;
      if( Aladin.OUTREACH ) y=YOUTREACH;
      if( y<10 ) { y=5; flagInfo=false; }

		if (flagTAPV2) {
			LISTDELIMITER = COMMA_CHAR;
			this.tapClient = tapClient;
			this.tapClient.serverGlu = this;
			this.gluAdqlQueryTemplates = new HashMap<String, GluAdqlTemplate>();
			for (int i = 0; i < tapTables.length; i++) {
				this.gluAdqlQueryTemplates.put(tapTables[i], new GluAdqlTemplate());
			}
			// No setQueryChecker for ServerGlu. We only check syntax.
		}
      // Le titre
      JPanel tp = new JPanel();
      Dimension d = makeTitle(tp,description);
      int x = XWIDTH/2-d.width/2;
      if( x<0 ) x=5;
      tp.setBackground(Aladin.BLUE);
      tp.setBounds(x,y,d.width,d.height); y+=d.height+10;
      tp.setName("titrePanel");
      if (flagTAPV2) {
      	 tapTableMapping.put("GENERAL",new Vector());
      	 tapTableMapping.get("GENERAL").add(tp);
      	/*if (this.tapClient.mode == TapClientMode.DIALOG) {
      		JButton button = ServerTap.getChangeServerButton();
    		button.addActionListener(this);
//    		button.setBounds(x+d.width,y-d.height,85,HAUT);
//    		button.setBounds(x+d.width+65, y-d.height, 30, 30);
    		button.setBounds(XWIDTH-2*XTAB1-60, y-d.height+3, 35, 25);
    		tapTableMapping.get("GENERAL").add(button);
    		add(button);
		}
      		
      	if (mode != TapServerMode.UPLOAD) {
      		modeChoice = tapClient.getGluModeButton();
      		modeChoice.setSelected(true);
//      		modeChoice.setBounds(x+d.width+95,y-d.height+3,35,25);
      		modeChoice.setBounds(XWIDTH-2*XTAB1-25, y-d.height+3, 35, 25);
      		tapClient.enableModes();
			add(modeChoice);
		}*/
      	
       }
      add(tp);

      // Indication (que s'il y a de la place)
      if( flagInfo ) {
         JLabel l = new JLabel(info1);
         l.setName("info");
         l.setBounds(110,y,400, 20); y+=20;
         if (flagTAPV2) {
        	 tapTableMapping.get("GENERAL").add(l);
         }
         add(l);
//         l = new JLabel(info2);
//         l.setBounds(128,y,300, 20); y+=20;
//         add(l);
      }
      
      if (flagTAPV2) {
    	  JPanel optionsPanel = ServerTap.getOptionsPanel(this);
      	optionsPanel.setName("optionsPanel");
      	 if (modeChoice!=null) {
      		modeChoice.setSelected(true);
  		}
      	optionsPanel.setBackground(Aladin.BLUE);
//      	optionsPanel.setBounds(XWIDTH-2*XTAB1-60, y-d.height-5, 78, 30);
      	optionsPanel.setBounds(x+d.width, y-d.height-5, 78, 30);
      	 add(optionsPanel);
      	tapTableMapping.get("GENERAL").add(optionsPanel);
	}

      // Y a-t-il un champ dont le type est baseUrl(URL|MOCID)
      baseUrlIndex = getBaseUrlParam(paramDataType);
      
      // Creation du JPanel du target
      boolean targetRequired = targetRequired(paramDataType);
      boolean radiusRequired = radiusRequired(paramDataType);
      if( targetRequired || radiusRequired ) {
         JPanel tPanel = new JPanel();
         int mode = 0;
         if( !targetRequired ) mode |= NOTARGET;
         if( !radiusRequired ) mode |= NORADIUS;
         int h = makeTargetPanel(tPanel,mode);
         tPanel.setBackground(Aladin.BLUE);
         tPanel.setBounds(0,y,XWIDTH,h); y+=h;
         tPanel.setName("tPanel");
         if (flagTAPV2) {
        	 int targetIndex = getKeyWordRequiredIndex(paramDataType, "Target");
        	 if (targetIndex >= 0) {
        		 for (String tapTable : tapTables) {
        				if (isParamSpecified(paramDataType[targetIndex],tapTable)) {
        					Vector components = this.tapTableMapping.get(tapTable);
    	  					if (components == null) {
    	  							components = new Vector();
    		  						this.tapTableMapping.put(tapTable, components);
    		 				}
    	  					components.add(tPanel);
        				}
             	 }
			}
         }
         add(tPanel);
      }

      if (flagTAPV2 && tapTables!=null) {
         this.currentSelectedTapTable = tapTables[0];
      }

      // Creation des champs de saisie
      vc = new Vector(5);
      int nc=0;
      for( int i=0; i<paramDescription.length; i++ ) {
         JComponent co;
         JList<String> list = null;
         boolean flagShow=true;
         boolean flagShowBackUp=true;
         StringTokenizer pv = (paramValue[i]!=null)?new StringTokenizer(paramValue[i],"\t"):null;

         if (checkIfMultiSelect(paramDataType[i])) {
        	 list = new JList<String>(paramValue[i].split("\t"));
             list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
             list.setVisibleRowCount(5);
             if (flagTAPV2) {
					if (paramDataType != null && paramDataType[i] != null){
						setDedicatedFields(i, paramDataType[i], list, gluAdqlTemplate);
						list.addListSelectionListener(new ListSelectionListener() {
							@Override
							public void valueChanged(ListSelectionEvent e) {
								// TODO Auto-generated method stub
								updateWidgets();
							}
						});
					}
			}
             sc = new JScrollPane(list);
             co = list;
         } else if( pv!=null && pv.countTokens()>1 ) {
            JComboBox ch = new JComboBox();
            ch.setOpaque(false);
            while( pv.hasMoreTokens() ) ch.addItem(pv.nextToken());
            if( flagTAP ) ch.addActionListener(this);
            if (flagTAPV2) {
					if (paramDataType != null && paramDataType[i] != null){
						setDedicatedFields(i, paramDataType[i], ch, gluAdqlTemplate);
						if(paramDataType[i].contains("Tables")) {
						ch.setActionCommand(TABLECHANGED);
					}}
			}
            co = ch;

         // Construction d'un champ de texte libre
         } else {
            JTextField t;
            if( pv!=null ) {

               // memorisation de la valeur par defaut
               if( vD==null ) vD = new String[paramDescription.length];
               vD[nc]=paramValue[i];

               t = new JTextField(vD[nc],40); // Une seule valeur possible => valeur par defaut
               

            } else t = new JTextField(40);
            t.addKeyListener(this);
            if( flagTAP ) t.addActionListener(this);

            co = t;

            // Type de donnees ? (pour target et radius, date et input)
            if( paramDataType!=null && paramDataType[i]!=null ) {
               co=setDedicatedFields(i, paramDataType[i], co, gluAdqlTemplate);

               // Certain component ne doivent pas être affiché (tel le target)
               flagShow = isShownField(paramDataType[i]);
             }
          }

         if (paramRange!=null && paramRange[i]!=null) {
         	rangeValues.put(i, paramRange[i]);
         	Util.toolTip(co, getSODAParamHint(co, i, paramRange[i]));
		}
         
         if (flagTAPV2) {//for show hide of default table columns
        	 flagShowBackUp = flagShow;
        	 if (flagShow) {
        		 flagShow = isParamSpecified(paramDataType[i], this.currentSelectedTapTable);
        		 //flagShow is flagShowFor current selected table incase of tapv2
			}
		}
         
         JLabel pTitre = null;
         if( flagShow ) {
            String s = paramDescription[i]==null?("Parameter "+(i+1)):(paramDescription[i]+" ");
            pTitre = new JLabel(addDot(s));
            pTitre.setFont(Aladin.BOLD);
            pTitre.setBounds(XTAB1,y,XTAB2-10,HAUT);
            add(pTitre);
            if (list!=null) {
            	sc.setBounds(XTAB2,y,XWIDTH-XTAB2,LIST_HAUT); y+=LIST_HAUT+MARGE;
            	add(sc);
            	list = null;
			} else {
				co.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
				add(co);
			}
            
            co.setName(s);
            nc++;
         }
         
         if (flagTAPV2 && paramDataType[i] != null && tapTables != null) {
  			for (String tapTable : tapTables) {
  				if (isParamSpecified(paramDataType[i],tapTable)) {
  					Vector components = this.tapTableMapping.get(tapTable);
  					if (components == null) {
	  						this.tapTableMapping.put(tapTable, new Vector());
	  						components = this.tapTableMapping.get(tapTable);
	 				}
  					//components: classify as per table
  					if (flagShowBackUp){//flagShowBackUp not really needed but its an extra putty
  						if (pTitre == null) {
  	 						pTitre = createDescriptiveLabel(paramDescription, i);
  						}
  	  					components.add(pTitre);
  	  					components.add(co);
					}
 				}
  			}
  		}
         
         vc.addElement(co);
         
      }
      
      
      // Pour IDHA ou SIA
      if( flagSIAIDHA ) {
         tree = new MetaDataTree(aladin, null);
         sc = new JScrollPane(tree);
         tree.setScroll(sc);
         sc.setBackground(tree.bkgColor);
         sc.setBounds(XTAB1,y,XWIDTH-2*XTAB1,180); y+=180;
         add(sc);
      }

      // Pour TAP
      else if( flagTAP ) {
    	 if (flagTAPV2) {
    		this.adqlParser = new ADQLParser();
    		JPanel linePanel = new JPanel();
			linePanel.setBackground(Aladin.BLUE);
			/*JButton button = new JButton("Refresh query");
			button.setActionCommand("WRITEQUERY");
			button.addActionListener(this);
			linePanel.add(button);*/
			
			JButton button = new JButton("Check..");
			button.setToolTipText(CHECKQUERYTOOLTIP);
			button.setActionCommand("CHECKQUERY");
			button.addActionListener(this);
			linePanel.add(button);
			
			this.sync_async = new JComboBox<String>(SYNC_ASYNC);
			this.sync_async.setToolTipText(SYNCASYNCTOOLTIP);
			this.sync_async.setOpaque(false);
			linePanel.add(sync_async);
			linePanel.setBounds(XTAB1,y,XWIDTH-2*XTAB1,HAUT+10); y+=HAUT+10;
			
			button = new JButton("Async jobs>>");
			button.setToolTipText(SHOWASYNCTOOLTIP);
			button.setActionCommand(SHOWAYNCJOBS);
			button.addActionListener(this);
			linePanel.add(button);
			
			this.tapTableMapping.put(LASTPANEL, new Vector());
			this.tapTableMapping.get(LASTPANEL).add(linePanel);
		    add(linePanel);
		}
    	  
         tap = new JTextArea("",8,50);
         tap.setFont(Aladin.ITALIC);
         tap.setWrapStyleWord(true);
         tap.setLineWrap(true);
//         tap.setEditable(false);
         sc = new JScrollPane(tap);
         
         y+=15;
         sc.setBounds(XTAB1,y,XWIDTH-2*XTAB1,180); y+=180;
         add(sc);
      }

      // Gestion des filtres prédéfinis s'il y a lieu
      if( aladinFilter!=null ) {
         filters=aladinFilter;
//         y=addFilterComment(10,y+10);

         JComboBox filtersChoice = createFilterChoice();
         filtersChoice.setOpaque(false);
         y+=10;
         JLabel pFilter = new JLabel(addDot(filter));
         pFilter.setFont(Aladin.BOLD);
         pFilter.setBounds(XTAB1,y,XTAB2-10,HAUT);
         add(pFilter);
         filtersChoice.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE;
         add(filtersChoice);
      }

      // Vers de la doc associee
      if( docUser!=null ) {
         JButton b = new JButton(HELP);
         b.setOpaque(false);
         b.addActionListener(this);
//         b.setFont(Aladin.BOLD);
         y+=10;
         b.setBounds(290,y,140,HAUT);  lastY=y; y+=HAUT+MARGE;
         add(b);
      }

      if( flagSIAIDHA || flagTAP ) setMaxComp(sc);

   }
   
  /**
   * Method for TAP : change table: to reset fields and columns as per the first table
   * @param tableSelected
   */
	protected void changeTapTable(String tableSelected) {
		int y = 5;
		JScrollPane sc = null;
		int j=0;
		if( (j=tableSelected.indexOf(" - "))>0 ) {
			this.currentSelectedTapTable = tableSelected.substring(0,j).trim();
		}
		else if( (j=tableSelected.trim().indexOf("- "))==0 ) {
        	this.currentSelectedTapTable = tableSelected.substring(0,j).trim();}
		else {
			this.currentSelectedTapTable = tableSelected;
		}
		removeAll();
		
		if (tapTableMapping.get("GENERAL") != null) {
			for (Object topPanelComponent : tapTableMapping.get("GENERAL")) {
				add((Component) topPanelComponent);
				if ("optionsPanel".equals(((Component) topPanelComponent).getName())) {
					y += 20;
				}
			}
		}
		y += 20;
		Vector<Component> components = new Vector<Component>();
//		components.addAll(tapTableMapping.get("GENERAL"));
		components.addAll(tapTableMapping.get(this.currentSelectedTapTable));
		for (Component co : components) {
			if (co instanceof JPanel) {
				add(co);
				y += co.getHeight();
				/*if ("optionsPanel".equals(co.getName())) {
					y += 10;
				}*/
				
				/*y += co.getHeight();
				if ("optionsPanel".equals(co.getName())) {
					y += 20;
				} else if ("tPanel".equals(co.getName())) {
					y += 10;
				}*/
				
			} else if (co instanceof JButton) {
				add(co);
			} else if (co instanceof JLabel) {
//				if (co.getName()!= null && !co.getName().equals("info")) {				}
				co.setBounds(XTAB1, y, XTAB2 - 10, HAUT);
				add(co);
			} else if (co instanceof JList) {
				sc = new JScrollPane();
				sc.setViewport((JViewport) co.getParent());
				sc.setBounds(XTAB2, y, XWIDTH - XTAB2, LIST_HAUT); y += LIST_HAUT + MARGE;
				add(sc);
			} else {
				co.setBounds(XTAB2, y, XWIDTH - XTAB2, HAUT);		y += HAUT + MARGE;
				add(co);
			}
		}
		
		Aladin.trace(3,"before addition of lastpanel y"+y);
		JPanel lastPanel = (JPanel) tapTableMapping.get(LASTPANEL).get(0);
		lastPanel.setBounds(XTAB1,y,XWIDTH-2*XTAB1,HAUT+10); y+=HAUT+10;
		add(lastPanel);
		Aladin.trace(3,"before addition of tap y"+y);
		 
		sc = new JScrollPane(tap);
        y+=15;
        sc.setBounds(XTAB1,y,XWIDTH-2*XTAB1,180); y+=180;
        add(sc);
        
        if( flagTAP ) setMaxComp(sc);
        
        this.majChoiceSize();
		revalidate();
		repaint();
	}
	
	private JLabel createDescriptiveLabel(String[] paramDescription, int i) {
		JLabel pTitre = new JLabel();
		String s = paramDescription[i]==null?("Parameter "+(i+1)):(paramDescription[i]+" ");
        pTitre = new JLabel(addDot(s));
        pTitre.setFont(Aladin.BOLD);
		return pTitre;
	}
    
   /**
    * Method to set some glu params
    * @param ranges
    * @param paramValues
    */
	public void setAdditionalGluParams(HashMap<String, String[]> ranges, Hashtable<String, String> paramValues) {
		String[] paramRanges = new String[NUMBEROFOPTIONS];
		
		String formIndex = null;

		JTextField field = null;
		JList<String> list = null;
		if (ranges != null) {
			for (int i = 0; i < vc.size(); i++) {
				JComponent gluComponent = (JComponent) vc.elementAt(i);
				formIndex  = String.valueOf(1+i);
				if (paramValues.containsKey(formIndex)) {
					String paramValue = paramValues.get(formIndex);
					if (paramValue != null) {
						if (gluComponent instanceof JTextField) {
							field = (JTextField) gluComponent;
							field.setText(paramValue);
						} else if (gluComponent instanceof JList<?>) {
							list = (JList<String>) gluComponent;
							list.setListData(paramValue.split("\t"));
						}
					}
				}
				
				if (ranges.containsKey(formIndex)) {
					paramRanges = ranges.get(formIndex);
					if (paramRanges!=null) {
						rangeValues.put(i, ranges.get(formIndex));
						Util.toolTip(gluComponent, getSODAParamHint(gluComponent, i, paramRanges));
					}
				} else if (!formIndex.equalsIgnoreCase(SODA_IDINDEX+"")) {
					rangeValues.put(i, null);
					Util.toolTip(gluComponent, LIMIT_UNKNOWN_MESSAGE);
				}

			}
		}
	}
   
   /**
    * Method to set a form paramter hint
    * @param co 
    * @param paramFormIndex
    * @param paramRange
    * @return hintString
    */
	public String getSODAParamHint(JComponent co, int paramFormIndex, String[] paramRange) {
		String result = LIMIT_UNKNOWN_MESSAGE;
		StringBuffer sodaHint = null;
		if (paramFormIndex==SODA_POLINDEX-1 && paramRange!=null && paramRange[0]!=null) {// [0] check if there is atleast one value
			sodaHint = new StringBuffer("Valid options are: ").append(Arrays.toString(paramRange).replaceAll(REGEX_ARRAY_PRINT, EMPTYSTRING).trim().replaceAll("\\s", " ,"));
		} else {
			if (paramRange != null) {
				String lowerLimitString = (paramRange[0] == null || paramRange[0].isEmpty()) ? EMPTYSTRING: paramRange[0];
				String upperLimitString = (paramRange[1] == null || paramRange[1].isEmpty()) ? EMPTYSTRING: paramRange[1];
				if (!(lowerLimitString.isEmpty() && upperLimitString.isEmpty())) {
					sodaHint = new StringBuffer("Valid Range: ").append(lowerLimitString);
					sodaHint.append(RANGE_DELIMITER).append(upperLimitString);
				}
			}
		}
		
		if (co==date) {
			if (sodaHint!=null) {
				sodaHint.append("  . ").append(PARSEMJDFIELDHINT);
			} else {
				sodaHint = new StringBuffer(result).append("  . ").append(PARSEMJDFIELDHINT);
			}
			result = sodaHint.toString();
		} else if ((paramFormIndex+1) == SODA_IDINDEX){
			result = EMPTYSTRING;
		} else if (sodaHint!=null){
			result = sodaHint.toString();
		}
		return result;
	}
	

  /** voir keyWordRequired */
   private boolean targetRequired(String PK[]) { return keyWordRequired(PK,"Target"); }
   private boolean radiusRequired(String PK[]) { return keyWordRequired(PK,"Field"); }
   private boolean checkIfMultiSelect(String paramDataType) { return isParamSpecified(paramDataType,"MultiSelect"); }
   private int getBaseUrlParam(String PK[]) { return getKeyWordRequiredIndex(PK,"BaseUrl"); }

   /** Retourne vrai si dans la liste des types de données PK[] ont a au-moins une
    * fois un type donné en paramètre
    * @param PK[] liste des types de données
    * @param keyWord le type de données recherché
    */
    private boolean keyWordRequired(String PK[],String keyWord) {
       return getKeyWordRequiredIndex(PK,keyWord)>=0;
       
//       for( int i=0; i<PK.length; i++ ) {
//          if( PK[i]==null ) continue;
//          int j=PK[i].indexOf('(');
//          if( j<0 ) continue;
//          if( PK[i].substring(0,j).equalsIgnoreCase(keyWord) ) return true;
//       }
//       return false;
    }
    
    /** Retourne le premier indice dans la liste des types de données PK[] qui a le type
     * de paramèter indiqué
     * @param PK[] liste des types de données
     * @param keyWord le type de données recherché
     */
    private int getKeyWordRequiredIndex(String PK[],String keyWord) {
       for( int i=0; i<PK.length; i++ ) {
          if( PK[i]==null ) continue;
          int j=PK[i].indexOf('(');
          if( j<0 ) j=PK[i].length();
          if( PK[i].substring(0,j).equalsIgnoreCase(keyWord) ) return i;
       }
       return -1;
    }
    
   /**
     * Method to check if an additionl param is specified in the PK.
     * Example: 
     * returns true for additionalParam2 in case PK[n]= ParamName(additionalParam1,additionalParam2)
     * @param paramDataType
     * @param param
     * @return
     */
    private boolean isParamSpecified(String paramDataType,String param) {
    	if( paramDataType==null ) return false;
        int j=paramDataType.indexOf('(');
        if( j<0 ) return false;
        else {
     	   String[] params = paramDataType.substring(j+1,paramDataType.indexOf(')')).split(",");
     	   for (int k = 0; k < params.length; k++) {
     		   if( params[k].equalsIgnoreCase(param) ) {
     			   return true;
     		   } 
			}
        }
        return false;
     }


  /** Dans le cas d'un serveur d'image, retourne le format (PlanImage.fmt)
   * qui sera retourne par le serveur en fonction du champ %R, 0 sinon */
   private int getFmt(String R) {
      if( R==null || R.indexOf("image")<0 ) return 0;
      return R.indexOf("/gfits")>0?PlanImage.GFITS:
             R.indexOf("/hfits")>0?PlanImage.HFITS:
             R.indexOf("/mrcomp")>0?PlanImage.MRCOMP:
             R.indexOf("/fits")>0?PlanImage.FITS:PlanImage.NATIVE;
   }

  /** Extrait le prochain mode d'un type de donnees "a la GLU" qui precise
   * pour un Target, un Field ou une Date ce qui est accepte par le serveur
   * @param mode Contiendra le prochain mode (ou chaine vide si termine)
   * @param a tableau de caracteres
   * @param i offset dans a[]
   * @return prochain offset a traiter
   */
   private int getMode(StringBuffer mode,char a[],int i) {
       while( i<a.length && a[i]!=')' && a[i]!='|' && a[i]!=',' && a[i]!=' ') {
          mode.append(a[i]);
          i++;
       }
       return i;
   }

   /** Repérage des champs à afficher dans le formualire
    * @param PK type de donnees a la GLU du genre Target(SIMBAD|COO)
    * @return true s'il s'agit d'un champ qui doit etre affiche dans le formulaire, cad qui ne
    *              concerne pas le target
    */
    private boolean isShownField(String PK) {
       char a[] = PK.toCharArray();
       int i=0;

       for( i=0; i<a.length && a[i]!='('; i++);
       String prefixe = new String(a,0,i);
       return !( prefixe.equalsIgnoreCase("Target") || prefixe.equalsIgnoreCase("Field") );
    }
    
    //Change this if isShownField(String PK) changes it logic from identifying target panel components
    private boolean isTargetPanelComponent(String PK) {
        return !isShownField(PK);
     }

  /** Positionnement des indicateurs modeCoo, modeRad et modeDate, modeInput[] ainsi que
   * coo[], rad[] et date et input[] associes
   * pour connaitre les Components concernes par le Target, le Radius et par la Date.
   * Voir la classe Server pour connaitre les modes supportes
 * @param i2 
   * @param PK type de donnees a la GLU du genre Target(SIMBAD|COO) ou Field(RADIUS)
   *           ou  Date(JD|MJD|YEARd) ou Input(IMG[s]|CAT[s])
   * @param sourceGluAdqlTemplate - All adql elements from glu dic
   * @return true s'il s'agit d'un champ qui doit etre affiche dans le formulaire, cad qui ne
   *              concerne pas le target
   */
   private JComponent setDedicatedFields(int gluIndex, String PK,JComponent f, GluAdqlTemplate sourceGluAdqlTemplate) {
      char a[] = PK.toCharArray();
      int i=0;
      StringBuffer mode;	// Pour recuperer les modes RA|DE|SIMBAD...
      int nbField=1;		// Nombre de champ TextField pour decrire target, radius, date ou input
      int ind=0;		    // Indice concerne pour coo[] et rad[]
      String s;

      //Recuperation du prefixe : Target(... ou Field(... ou Date(... ou Input(...
      for( i=0; i<a.length && a[i]!='('; i++);
      String prefixe = new String(a,0,i);

//System.out.println("setDedicatedFields -> "+PK+": prefixe=["+prefixe+"]:");
      // Pour le target
      if( prefixe.equalsIgnoreCase("Target") ) {
//System.out.println("   Target...");
         while(true) {
           mode = new StringBuffer();
           i=getMode(mode,a,i+1);
           if( mode.length()==0 ) break;
           s=mode.toString();
//System.out.println("      mode="+s);

                if( s.equalsIgnoreCase("RA") )     { nbField=2; modeCoo |= RADEC; }
           else if( s.equalsIgnoreCase("DE") )     { nbField=2; ind=1; modeCoo |= RADEC; }
           else if( s.equalsIgnoreCase("RAb") )    { nbField=2; modeCoo |= RADEb; }
           else if( s.equalsIgnoreCase("DEb") )    { nbField=2; ind=1; modeCoo |= RADEb; }
           else if( s.equalsIgnoreCase("RAh") )    {
              nbField=6; modeCoo |= RADE6; }
           else if( s.equalsIgnoreCase("RAm") )    { nbField=6; ind=1; modeCoo |= RADE6; }
           else if( s.equalsIgnoreCase("RAs") )    { nbField=6; ind=2; modeCoo |= RADE6; }
           else if( s.equalsIgnoreCase("DEdg") )   { nbField=6; ind=3; modeCoo |= RADE6; }
           else if( s.equalsIgnoreCase("DEm") )    { nbField=6; ind=4; modeCoo |= RADE6; }
           else if( s.equalsIgnoreCase("DEs") )    { nbField=6; ind=5; modeCoo |= RADE6; }
           else if( s.equalsIgnoreCase("SIMBAD") ) { modeCoo |= SIMBAD; }
           else if( s.equalsIgnoreCase("NED") )    { modeCoo |= NED; }
           else if( s.equalsIgnoreCase("COO") )    { modeCoo |= COO; }
           else if( s.equalsIgnoreCase("COOd") )   { modeCoo |= COOd; }
           else if( s.equalsIgnoreCase("COOb") )   { modeCoo |= COOb; }
           else if( s.equalsIgnoreCase("RAd") )    { nbField=2; modeCoo |= RADEd; }
           else if( s.equalsIgnoreCase("DEd") )    { nbField=2; ind=1; modeCoo |= RADEd; }
           else if( flagTAPV2 && this.gluAdqlQueryTemplates.keySet().contains(s)) {
        	   classifyAsPerTapTables(prefixe, s, gluIndex+1, sourceGluAdqlTemplate);
           }
           else if(modeCoo==0){//to avoid reset incase of multiple parameters . if modeCoo is already set-don't reset
              System.err.println("Server ["+aladinLabel+"]; unknown Target code ["+s+"] => assume COO");
              modeCoo |= COO;
           }
         }

         if( coo==null ) coo = new JTextField[nbField];
         coo[ind] = (JTextField)f;
//System.out.println("      modeCoo="+modeCoo);

      // Pour le Field
      } else if( prefixe.equalsIgnoreCase("Field") ) {
//System.out.println("   Field...");
         while(true) {
           mode = new StringBuffer();
           i=getMode(mode,a,i+1);
           if( mode.length()==0 ) break;
           s=mode.toString();
//System.out.println("      mode="+s);

                if( s.equalsIgnoreCase("RA") )     { nbField=2; modeRad |= RADBOX; }
           else if( s.equalsIgnoreCase("DE") )     { nbField=2; ind=1; modeRad |= RADBOX; }
           else if( s.equalsIgnoreCase("RAd") )    { nbField=2; modeRad |= RADBOXd; }
           else if( s.equalsIgnoreCase("DEd") )    { nbField=2; ind=1; modeRad |= RADBOXd; }
           else if( s.equalsIgnoreCase("RAs") )    { nbField=2; modeRad |= RADBOXs; }
           else if( s.equalsIgnoreCase("DEs") )    { nbField=2; ind=1; modeRad |= RADBOXs; }
           else if( s.equalsIgnoreCase("SQR") )    { modeRad |= RADSQR; }
           else if( s.equalsIgnoreCase("SQRd") )   { modeRad |= RADSQRd; }
           else if( s.equalsIgnoreCase("SQRs") )   { modeRad |= RADSQRs; }
           else if( s.equalsIgnoreCase("RADIUS") ) { modeRad |= RADIUS; }
           else if( s.equalsIgnoreCase("RADIUSd") ){ modeRad |= RADIUSd; }
           else if( s.equalsIgnoreCase("RADIUSs") ){ modeRad |= RADIUSs; }
           else if( s.equalsIgnoreCase("STRINGd") ){ modeRad |= STRINGd; }
           else if( flagTAPV2 && this.gluAdqlQueryTemplates.keySet().contains(s)) {
        	   classifyAsPerTapTables(prefixe, s, gluIndex+1, sourceGluAdqlTemplate);
           } else if( flagTAPV2 && s.equalsIgnoreCase("OP") && f instanceof JTextField) {
        	   if( adqlOpInputs==null ) {
    			   adqlOpInputs = new Vector<JTextField>();
                }
    		   adqlOpInputs.addElement((JTextField) f);
        	   
           }/*
           else if (!(flagTAPV2 && tapTableMapping.keySet().contains(s))) {
//    				tapTableMapping.get(s).add(f);
				}*/
           else if(modeRad==0){
              System.err.println("Server ["+aladinLabel+"]; unknown Field code ["+s+"] => assume RADIUS");
              modeRad |= RADIUS;
           }
         }

         if( rad==null ) rad = new JTextField[nbField];
         rad[ind] = (JTextField)f;
//System.out.println("      modeRad="+modeRad);

      // Pour la date
      } else if( prefixe.equalsIgnoreCase("Date") ) {
         // System.out.println("Date...");
         while(true) {
            mode = new StringBuffer();
            i=getMode(mode,a,i+1);
            if( mode.length()==0 ) break;
            s=mode.toString();
            // System.out.println("mode="+s);

            if( s.equalsIgnoreCase("MJD") )   { modeDate |= MJD; }
            else if( s.equalsIgnoreCase("JD") )    { modeDate |= JD; }
            else if( s.equalsIgnoreCase("YEARd") ) { modeDate |= YEARd; }
            else if( s.equalsIgnoreCase("ParseToMJD") ) { modeDate |= ParseToMJD; }
            else if( flagTAPV2 && this.gluAdqlQueryTemplates.keySet().contains(s)) {
               classifyAsPerTapTables(prefixe, s, gluIndex+1, sourceGluAdqlTemplate);
            } else if( flagTAPV2 && s.equalsIgnoreCase("OP") && f instanceof JTextField) {
               if( adqlOpInputs==null ) {
                  adqlOpInputs = new Vector<JTextField>();
               }
               adqlOpInputs.addElement((JTextField) f);

            }
            else if(modeDate==0){
               if( Aladin.levelTrace>=3 ) System.err.println("Server ["+aladinLabel+"]; unknown Date code ["+s+"] => assume JD");
               modeDate |= JD;
            }
         }

         date = (JTextField)f;
         // System.out.println("modeDate="+modeDate);

         // Pour le input
      } else if( prefixe.equalsIgnoreCase("Input") ) {
//System.out.println("Input...");
         while(true) {
            mode = new StringBuffer();
            i=getMode(mode,a,i+1);
            if( mode.length()==0 ) break;
            s=mode.toString();
//System.out.println("mode="+s);

            // Initialisation des inputs
            if( input==null ) {
               input = new JComponent[MAXINPUT];
               modeInput = new int[MAXINPUT];
               nbInput=0;
            }

                 if( s.equalsIgnoreCase("IMG") )   { modeInput[nbInput] |= IMG; }
            else if( s.equalsIgnoreCase("ALLIMG") ){ modeInput[nbInput] |= ALLIMG; }
            else if( s.equalsIgnoreCase("IMGs") )  { modeInput[nbInput] |= IMGs; }
            else if( s.equalsIgnoreCase("CAT") )   { modeInput[nbInput] |= CAT; }
            else if( s.equalsIgnoreCase("CATs") )  { modeInput[nbInput] |= CATs; }
            else if( flagTAPV2 && this.gluAdqlQueryTemplates.keySet().contains(s)) {
         	   classifyAsPerTapTables(prefixe, s, gluIndex+1, sourceGluAdqlTemplate);
            } else if( flagTAPV2 && s.equalsIgnoreCase("OP") && f instanceof JTextField) {
          	   if( adqlOpInputs==null ) {
      			   adqlOpInputs = new Vector<JTextField>();
                  }
      		   adqlOpInputs.addElement((JTextField) f);
          	   
             }
            else if(modeInput[nbInput]==0){
               System.err.println("Server ["+aladinLabel+"]; unknown Input code ["+s+"] => assume IMG");
               modeInput[nbInput] |= IMG;
            }
         }

         // input 1 plan => Choice à la place de TextField
         if( (modeInput[nbInput] & (ALLIMG|IMG|CAT))!=0 ) {
            JComboBox c = new JComboBox();
            c.addItem(NOINPUTITEM);
            f=c;
         }
//System.out.println("modeInput["+nbInput+"]="+modeInput[nbInput]);
         input[nbInput++] = f;
         DISCOVERY=false;		// PAs de mode AllVO pour ce type de formulaire
      } else if( prefixe.equalsIgnoreCase("Band") ) {
          while(true) {
              mode = new StringBuffer();
              i=getMode(mode,a,i+1);
              if( mode.length()==0 ) break;
              s=mode.toString();

              if( s.equalsIgnoreCase("SODA") || s.equalsIgnoreCase("m") )   { modeBand |= BANDINMETERS; }
              else if( flagTAPV2 && this.gluAdqlQueryTemplates.keySet().contains(s)) {
           	   classifyAsPerTapTables(prefixe, s, gluIndex+1, sourceGluAdqlTemplate);
              } else if( flagTAPV2 && s.equalsIgnoreCase("OP") && f instanceof JTextField) {
           	   if( adqlOpInputs==null ) {
       			   adqlOpInputs = new Vector<JTextField>();
                   }
       		   adqlOpInputs.addElement((JTextField) f);
           	   
              }
              /*else if(modeBand==0){
                 System.err.println("Server ["+aladinLabel+"]; unknown band code ["+s+"] => assume NoMode");
                 modeBand |= NOMODE;
              }*/
           }

           band = (JTextField)f;
        } else if( flagTAPV2) {
        	setGluAdqlForAllTapTables(prefixe, i, a, gluIndex+1, sourceGluAdqlTemplate, f);
        }

      return f;
   }
   
   private void setGluAdqlForAllTapTables(String prefixe, int i, char[] a, int gluIndex, GluAdqlTemplate sourceGluAdqlTemplate, JComponent f) {
	   StringBuffer mode;
	   while (true) {
   		mode = new StringBuffer();
           i=getMode(mode,a,i+1);
           if( mode.length()==0 ) break;
           String s = mode.toString();
           if(s.equalsIgnoreCase("OP") && f instanceof JTextField) {
        	   if( adqlOpInputs==null ) {
    			   adqlOpInputs = new Vector<JTextField>();
                }
    		   adqlOpInputs.addElement((JTextField) f);
        	   
           } else {
        	   classifyAsPerTapTables(prefixe, s, gluIndex, sourceGluAdqlTemplate);
           }
           
		}
   }
   
   private void classifyAsPerTapTables(String prefixe, String s, int gluIndex, GluAdqlTemplate sourceGluAdqlTemplate) {
	   for (String tapTable : this.gluAdqlQueryTemplates.keySet()) {
          	if( s.equalsIgnoreCase(tapTable))   { 
          		GluAdqlTemplate tableQuery = this.gluAdqlQueryTemplates.get(tapTable);
          		GluAdqlTemplate.copy(prefixe, String.valueOf(gluIndex), sourceGluAdqlTemplate, tableQuery);
          	}
		}
   }
   
   public static String extractFromQuotes(String input) {
		String demiliter = EMPTYSTRING;
		try {
			StringTokenizer stk = new StringTokenizer(input, "\"");
			demiliter = stk.nextToken();
		} catch (NoSuchElementException e) {
			// do nothing. Just return default. Uses space.
		}
		return demiliter;
	}

   /** Pour un serveur GLU on teste en premier si le nom du serveur passé
    * en paramètre dans la commande script ne serait pas directement
    * l'identificateur de l'enregistrement GLU. Sinon on va regarder
    * en détail les chaines de descriptions
    */
   protected boolean is(String s) {
      return s.equalsIgnoreCase(actionName) || super.is(s);
   }
   

   /** Retourne l'Item d'une JcomboBox qui contient la chaine "cr" (ou egalité stricte
    * dans le cas d'un numérique)
    * RA: "cr" est supposé être en majuscules
    * Rq: traite le cas particulier "nn - chaine", où le retour sera "nn" au lieu de "chaine"
    * Retourn null si non trouvé */
   private String getComboItem(JComboBox c, String cr) {
      String s,s1;
      int m;
      boolean flagNumeric=true;
      try { Double.parseDouble(cr); }
      catch( Exception e ) { flagNumeric=false; }

      int n=c.getItemCount();
      for( int i=0; i<n; i++ ) {
         s1=s = ((String)c.getItemAt(i)).toUpperCase();
         if( (m=s.indexOf(" - "))>0 ) {
            s1=s.substring(m+1).trim();
            s=s.substring(0,m).trim();
         }
         if( (!flagNumeric && (s1!=s && (s.equals(cr) || s1.indexOf(cr)>=0) 
               || (s1==s && s.indexOf(cr)>=0))) 
            || flagNumeric && s.equals(cr) ) {
//         if( !flagNumeric && (s.indexOf(cr)>=0 || s1.indexOf(cr)>=0) || flagNumeric && s.equals(cr) ) {
            s = (String)c.getItemAt(i);   // Pour ne pas rester en majuscules
            if( (m=s.indexOf(" - "))>0 ) s=s.substring(0,m).trim();
            return s;

         }
      }
      return null;
   }
   
   /** Creation d'un plan de maniere generique */
   protected int createPlane(String target,String radius,String criteria,
   				 String label, String origin) {
      String s,objet="";
      Enumeration e;
      int i,j,k,m;
      
      String serverTaskId = aladin.synchroServer.start("ServerGlu.createPlane/"+target);
      try {

         // Resolution par Simbad necessaire ?,
         // et remplissage des champs adequats
         try {
            objet=resolveTarget(target);
            if( objet==null ) throw new Exception(UNKNOWNOBJ);
         } catch( Exception e1 ) {
            Aladin.warning(this,e1.getMessage(),1);
            return -1;
         }

         // Pre-remplissage des champs concernant le radius
         if( radius!=null && radius.length()>0 ) resolveRadius(radius,true);

         // Pré-remplissage du champ concernant la date
         if( date!=null && date.getText().trim().length()==0 ) resolveDate(getDefaultDate());
         
         // Découpage des critères dans un tableau
         Tok st = new Tok(criteria.trim(),",");
         String crit[] = new String[st.countTokens()];
         for( i=0; st.hasMoreTokens(); i++ ) {
            crit[i] = st.nextToken();
            //System.out.println("Critère "+(i+1)+" ["+crit[i]+"]");
         }

         Vector v = new Vector(10);    // Liste des critères finaux
         Vector vbis = new Vector(10);	// Juste pour etablir le label du plan

         // Initialisation à null;
         for( e = vc.elements(); e.hasMoreElements(); e.nextElement() ) {
            v.addElement(null);
            vbis.addElement(null);
         }

         // Placement des critères labelés (ex: Survey=DSS1)
         for( i=0; i<crit.length; i++ ) {
            String cr=crit[i];
            int posEgal=cr.indexOf('=');
            if( posEgal>0 && cr.charAt(posEgal-1)!='\\' ) {
               String cName=cr.substring(0,posEgal).trim().toUpperCase();
               s = cr.substring(posEgal+1).trim();
               s = Tok.unQuote(s);
               
               //System.out.print(".Recherche initiale pour "+cName+"="+s);

               // Recherche du paramètre correspondant
               e = vc.elements();
               for( j=0; e.hasMoreElements(); j++ ) {
                  JComponent c = (JComponent)e.nextElement();
                  if( isFieldInput(c) || isFieldTargetOrRadius(c)) continue;
                  String coName=c.getName();
                  if( coName==null ) continue;
                  coName=coName.trim().toUpperCase();
                  //System.out.print(" ["+coName+"]");
                  if( coName.indexOf(cName)<0 ) continue;
                  if( c instanceof JComboBox ) s=getComboItem((JComboBox)c,s.toUpperCase());
                  if( s!=null ) {
                     //System.out.print(" Bingo("+s+"):"+j);
                     v.setElementAt(s,j);
                     vbis.setElementAt(s,j);
                     crit[i]=null; // je le mange
                  }
                  break;
               }

               //System.out.println();
            }
         }

         // Placement des critères non labelés, mais dont la valeur va être
         // trouvée dans la liste de la JComboBox
         e = vc.elements();
         for( j=0; e.hasMoreElements(); j++ ) {
            JComponent c = (JComponent)e.nextElement();
            //System.out.print(".Recherche pour "+c.getName()+" :");
            boolean trouve=false;
            for( k=0; !trouve && k<crit.length; k++ ) {
               String cr=crit[k];
               if( cr==null ) continue;
               cr = cr.toUpperCase();

               //System.out.print(" ["+cr+"]");
               if( c instanceof JComboBox && !isFieldInput(c) ) {
                  s = getComboItem((JComboBox)c,cr);
                  if( s!=null ) {
                     //System.out.print(" Bingo("+s+"):"+j);
                     if( (m=s.indexOf(" - "))>0 ) s=s.substring(0,m).trim();
                     trouve=true;

                     v.setElementAt(s,j);
                     vbis.setElementAt(s,j);
                     crit[k]=null;	// je le mange
                  }
               }
            }
            //System.out.println();
         }


         // Passage en revue des critères input non encore utilisés et je les place
         // postionnellement dans les critères output en sautant les cases déjà renseignés
         e = vc.elements();
         JComponent c=(JComponent)e.nextElement();
         for( j=i=0; i<crit.length; i++ ) {
            if( crit[i]==null ) continue;		// déjà utilisé

            try {
               for(;v.elementAt(j)!=null || isFieldTargetOrRadius(c); j++ ) c=(JComponent)e.nextElement();
            }
            catch(NoSuchElementException nsee) {continue;}

            // Si le component est tagué en INPUT, on va remplacer le nom du plan saisie
            // par son URL (si possible)
            if( isFieldInput(c) ) {
               setSelectedItem(c,crit[i]);		// Je positionne le Choice à la main sinon
               crit[i]=getInputUrl(c);         // le getInputUrl merdouille
               if( crit[i]==null ) {
//                  setSync(true);
                  return -1;
               }
            }

            //System.out.println(".Position("+crit[i]+"):"+j);
            v.setElementAt(crit[i],j);
            vbis.setElementAt(crit[i],j);
         }

         // Passage en revue des critères manquants en output et positionnement de la valeur
         // par défaut correspondante si elle peut être déterminée, sinon ""
         e = vc.elements();
         for( j=0; e.hasMoreElements(); j++ ) {
            c = (JComponent)e.nextElement();
            if( v.elementAt(j)!=null ) continue; // déjà renseigné
            //System.out.print(".Default pour "+c.getName()+" :");
            s=""; // le défaut

            if( (c instanceof JTextField) ){
               s = ((JTextField)c).getText();
               //System.out.print(" Default_Textfield("+s+"):"+j);

            } else if( c instanceof JComboBox ) {
               s = (String)((JComboBox)c).getSelectedItem();
               if( (m=s.indexOf(" - "))>0 ) vbis.addElement(s.substring(m+3));
               else vbis.addElement(s);
               if( (m=s.indexOf(" - "))>0 ) s=s.substring(0,m).trim();
               else if( s.equals("?") || s.startsWith("-") && s.endsWith("-")) s="";
               //System.out.print(" Default_Choice("+s+"):"+j);
               v.addElement(s);
            }
            v.setElementAt(s,j);
            vbis.setElementAt(s,j);
            //System.out.println();
         }
         
         // Résolution d'un paramètre de type BaseUrl(URL|MOCID)
         if( baseUrlIndex>=0 ) {
            s = (String) v.get( baseUrlIndex );
            if( !s.startsWith("http://") && !s.startsWith("https://") ) {
              s = aladin.directory.resolveServiceUrl(actionName,s);
              if( s!=null ) v.setElementAt( s, baseUrlIndex);
            }
         }

         e = v.elements();
         StringBuffer p=null;
         StringBuffer p1=null;
         while( e.hasMoreElements() ) {
            s = (String)e.nextElement();
            //System.out.println("Param ["+s+"]");
            if( p==null ) { p=new StringBuffer(Glu.quote(s)); p1=new StringBuffer(s); }
            else { p.append(" "+Glu.quote(s)); p1.append("/"+s); }
         }

         // Generation de l'URL par appel au GLU
         URL u = aladin.glu.getURL(actionName,p.toString());


         // S'agit-il d'une commande script provenant d'un serveur en 2 temps ? (SIAP/SSAP)
         if( flagSIAIDHA && ( type==IMAGE || type==SPECTRUM ) ) {

            //          System.out.println(u);
            //          for( int pff=0; pff<crit.length; pff++ ) System.out.println(crit[pff]);
            TreeBuilder tb = new TreeBuilder(aladin, u, -1, null,  objet);
            try {
               ResourceNode rootNode = tb.build();
               Vector leaves = new Vector();
               BasicTree.getAllLeaves(rootNode, leaves);
               SIAPruner pruner = new SIAPruner((ResourceNode[])leaves.toArray(new ResourceNode[leaves.size()]), crit);
               ResourceNode[] nodesToLoad = pruner.prune();
               if( nodesToLoad==null ) {
                  return -1;
               }
               ResourceNode node;
               for( int idx=0; idx<nodesToLoad.length; idx++ ) {
                  node = nodesToLoad[idx];
                  if( type==IMAGE ) {
                     // load image in aladin
                     aladin.dialog.localServer.tree.load(node,label);
                  }
                  else if( type==SPECTRUM ) {
                     // broadcast spectrum to whatever spectrum app is there
                     aladin.getMessagingMgr().sendMessageLoadSpectrum(node.location, node.location, node.name, node.getMetadata(), null);
                  }
               }
            }
            catch(Exception e2) {
               aladin.command.println("error : "+e2.getMessage());
               e2.printStackTrace();
            }
            return 1;
         }


         if( label==null ) {
            if( planeLabel==null ) label=getDefaultLabelIfRequired(label);
            else {
               String [] param = new String[vbis.size()];
               for( i=0; i<param.length; i++ ) {
                  s = (String)vbis.elementAt(i);
                  if( s.equals("-") ) s="";
                  param[i] = s;
               }
               planeLabel = dollarQuerySet(planeLabel);
               label = getDefaultLabelIfRequired(label, aladin.glu.dollarSet(planeLabel,param,Glu.NOURL).trim());
            }
         }

         String param = p1!=null ? p1.toString() : "";

         // S'agit-il d'un serveur d'images
         if( type==IMAGE ) {
            if( !verif(Plan.IMAGE,objet,param) ) {
               return -1;
            }
            if( fmt==PlanImage.NATIVE ) {
               int n=aladin.calque.newPlanImageColor(u,null,PlanImage.OTHER,label,objet,param, "provided by "+institute,
                     fmt,PlanImage.UNDEF,null,null);
               return n;
            } else {
               int n = aladin.calque.newPlanImage(u,PlanImage.OTHER,
                     label,objet,param, "provided by "+institute, fmt,PlanImage.UNDEF, null);
               return n;
            }
            
         // Ou d'un serveur de MOC (on prend on compte les mirroirs)
         } else if( type==MOC ) {
            MyInputStream in=null;
            try {
               try { in = Util.openStream(u); }
               catch( Exception e1 ) {
                  
                  // Peut être un miroir ?
                  if( aladin.glu.checkIndirection(actionName, null) ) {
                     u=aladin.glu.getURL(actionName,p.toString());
                     in = Util.openStream(u);
                  } else throw e1;
               }
               return aladin.calque.newPlanMOC( in, label);
               
            } catch( Exception e1 ) {
               if( aladin.levelTrace>=3 ) e1.printStackTrace(); 
               return -1;
            }

            // Ou d'un serveur de donnees
         } else {
            if( !verif(Plan.CATALOG,objet,param) ) { 
               return -1;
            }
            int n = aladin.calque.newPlanCatalog(u,label,objet,param,"provided by "+institute,this);
            return n;
         }

      } finally { aladin.synchroServer.stop(serverTaskId); }
   }

   public void submit() {
      (new Thread(this,"GluServer submit") {
         public void run() { submit1(true, false); }
      }).start();
   }

   /** Interrogation
    * IL FAUDRAIT GREFFER CETTE FONCTION A createPlane CI-DESSUS...
    */
   private void submit1(boolean flagDoIt, boolean userReady) {
      String s,objet="";
      Enumeration e;
      String code=null;
      boolean flagScriptEquiv=true;	// Par défaut, il existe tjs une commande script équivalent
      List<Coord> rectVertices = new ArrayList<Coord>();//code for poly addition --in progress 1 line
      
      // Resolution par Simbad necessaire ?
      if(target!=null) {
         try {
            objet=resolveQueryField();
            if( objet==null ) throw new Exception(UNKNOWNOBJ);
            if (this.posBounds!=null) { //current config to check target limits
               String error = isWithinBounds(this.posBounds, rectVertices);
               if( error!=null ) throw new Exception(error);
            }
            /*int i = getDelimiterIndex(radius.getText().trim()); //code for poly shape addition --in progress 6 lines
	            if (i>=0) {
	            	if (rectVertices.isEmpty()) {
	       	 			rectVertices= getRectVertices();
					}
				}*/

         } catch( Exception e1 ) {
            if( !flagDoIt ) return;
            Aladin.warning(this,e1.getMessage());
            ball.setMode(Ball.NOK);
            return;
         }

      }

      Vector v = new Vector(10);
      Vector vbis = new Vector(10);           	// Juste pour etablir le label du plan
      StringBuffer criteres = new StringBuffer();
      e = vc.elements();
      int index = 0;
      StringBuffer limitViolation = null;
      while( e.hasMoreElements() ) {
         JComponent c = (JComponent)e.nextElement();
         String crit=null;
         if( isFieldInput(c) ) {
            v.addElement( getInputUrl(c) );
            vbis.addElement( getInputPlaneName(c) );
            flagScriptEquiv=false;
         } else if( c instanceof JTextField ) {
            s = ((JTextField)c).getText();
            try {
                StringBuffer processedText = processCustomFields(c, (flagDoIt || userReady));
                if (processedText!=null && processedText.length()>0 && !processedText.toString().trim().isEmpty()) {
                   s = processedText.toString();
                }
             } catch (Exception e1) {
                if( !flagDoIt ) return;
                Aladin.warning(this, e1.getMessage());
                ball.setMode(Ball.NOK);
                return;
             }
             if (flagTAPV2 && !s.isEmpty() && adqlOpInputs!=null && adqlOpInputs.contains(c)) {
                String processedInput = getRangeInput(s);
                if (processedInput.isEmpty()) {
                   processedInput = isInValidOperatorNumber(s, userReady || flagDoIt);
                   if (processedInput==null) {
                      return;
                   } else {
                      s = processedInput;
                   }
                } else {
                   s = processedInput;
                }
                /*for (FocusListener focusListener : c.getFocusListeners()) {
					if (focusListener instanceof DelimitedValFieldListener) {
						DelimitedValFieldListener constraint2Val = ((DelimitedValFieldListener)focusListener);
						if (!constraint2Val.isValid()) {
							ball.setMode(Ball.NOK);
							Aladin.warning(this, constraint2Val.getToolTipText());
							return;
						}
					}
				}*/
             }
            v.addElement(s);
            vbis.addElement(s);
            if( !isFieldTargetOrRadius(c) ) crit=s;
            limitViolation = isValueWithinLimits(s, rangeValues.get(index), null);
         } else if( c instanceof JComboBox ) {
            int j;
            String t=null;
            crit=s = (String)((JComboBox)c).getSelectedItem();
            // Si la valeur est précédée d'un "XXX - valeur", c'est XXX qui sera utilisé
            // en tant que valeur.
            if( (j=s.indexOf(" - "))>0 ) vbis.addElement(crit=s.substring(j+3));
            if( (j=s.trim().indexOf("- "))==0 ) vbis.addElement(crit=s.substring(j+2));
            else vbis.addElement(s);
            if( j>=0 ) s=s.substring(0,j).trim();
            else if( s.equals("?") || s.startsWith("-") && s.endsWith("-")) crit=s="";
            v.addElement(s);
         }//TODO:: add in case limitViolation for the combobox
         else if( c instanceof JList ) {
            int j;
            StringBuffer listString = new StringBuffer();
            List<String> selectedValues= ((JList<String>)c).getSelectedValuesList();
            for (String selectedValue : selectedValues) {
               crit=s = selectedValue;
               if( (j=s.indexOf(" - "))>0 ) vbis.addElement(crit=s.substring(j+3));
               if( (j=s.trim().indexOf("- "))==0 ) vbis.addElement(crit=s.substring(j+2));
               else vbis.addElement(s);
               if( j>=0 ) s=s.substring(0,j).trim();
               else if( s.equals("?") || s.startsWith("-") && s.endsWith("-")) crit=s="";
               listString.append(s).append(LISTDELIMITER);
            }

            v.addElement(listString.toString().trim().replaceAll(LISTDELIMITER+"$", EMPTYSTRING));
            limitViolation = isValueWithinGivenOptions(listString.toString(), rangeValues.get(index), null);
         }

         // Mise à jour des critères de la commande script équivalente
         if( crit!=null && crit.length()>0 ) {
            if( criteres.length()>0 ) criteres.append(",");

            criteres.append(Tok.quote(crit));
         }
         
         if (limitViolation!=null && limitViolation.length()!= 0 && !isFieldDate(c) && !isFieldBand(c)) {//Skip in case of date and band.
        	 Aladin.warning(c, limitViolation.toString());
			return;
		}
         
         index++;
      }

      e = v.elements();
      StringBuffer p=null;
      StringBuffer p1=null;
      while( e.hasMoreElements() ) {
         s = (String)e.nextElement();
         if( p==null ) { p=new StringBuffer(Glu.quote(s)); p1=new StringBuffer(s); }
         else { p.append(" "+Glu.quote(s)); p1.append("/"+s); }
      }

      // Generation du label du plan
      String label=null;
      if( planeLabel==null ) label=aladinLabel;
      else {
         String [] param = new String[vbis.size()];
         for( int i=0; i<param.length; i++ ) {
            s = (String)vbis.elementAt(i);
            if( s.equals("-") ) s="";
            param[i] = s;
         }
         label = aladin.glu.dollarSet(dollarQuerySet(planeLabel),param,Glu.NOURL).trim();
      }
      
      URL u = null;
      if (flagTAPV2) {
//    	  e = v.elements();
    	  GluAdqlTemplate gluQueryTemplate = this.gluAdqlQueryTemplates.get(this.currentSelectedTapTable);
  		
    	  String queryString = EMPTYSTRING;
    	  if (gluQueryTemplate!=null) {
    		  queryString = gluQueryTemplate.getGluQuery(v, this.currentSelectedTapTable, this.adqlFunc, this.adqlFuncParams);
    	  }
    	  String url = (String) aladin.glu.aladinDic.get(actionName);
    	  Aladin.trace(3, "URL for "+actionName+" is: "+url);
    	  url = TapManager.getInstance(aladin).getSyncUrlUnEncoded(url, queryString);
    	  Aladin.trace(3, "getSyncUrlUnEncoded: "+url);
    	  aladin.glu.aladinDic.put(actionName+"v1", url);// for setting all glu params
    	  u = aladin.glu.getURL(actionName+"v1",p==null?"" : p.toString(), true);
    	  Aladin.trace(3, "getURL u: "+u);
    	  try {
			u = addQueryEncodedUrl(u.toString());
			aladin.glu.aladinDic.put(actionName+"v1", u.toString());//update the properly encoded query
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			if( !flagDoIt ) return;
            Aladin.warning(this,e1.getMessage());
            ball.setMode(Ball.NOK);
            return;
		}
      } else {
    	// Generation de l'URL par appel au GLU
    	  u = aladin.glu.getURL(actionName,p==null?"" : p.toString());
      }
      
      // C'est juste pour avoir le texte ADQL
      if( !flagDoIt && flagTAP ) { setInTap(u); return; }
      
      if (flagTAPV2) {
    	  setInTap(u);
      }
      
      // Il s'agit d'un appel système ?
      if( system!=null ) { exec(label, p==null?"" : p.toString() ); return; }
      
      // Génération de la commande script équivalent
      if( flagScriptEquiv && !flagTAPV2 ) {
         String r = getRadius(false);
         if( r==null ) r="";
         else r = " "+Coord.getUnit(getRM(r)/60.);
         code = "get "+actionName + (criteres.length()==0?" ":"("+criteres+") ");
         aladin.console.printCommand(code+this.getTarget()+r);
      }

      
      // S'agit-il d'un serveur SIA || IDHA
      if( flagSIAIDHA ) {
          try{

            // Traitement des images par lot
             if( tree!=null && !tree.isEmpty() ) {
                if( tree.nbSelected()>0 ) {
                   if( !tooManyChecked() ) { 
                      tree.loadSelected();
                      tree.resetCb();
                   }
               } else Aladin.warning(this,WNEEDCHECK);

			   // Chargement des descriptions des images disponibles dans thread séparé
			   } else {
				   waitCursor();
				   uT = u;
				   posT = new Coord(objet).toString();
				   // variable pour thread
				   thread= new Thread(this,"AladinGluServerMetaData");
				   Util.decreasePriority(Thread.currentThread(), thread);
				   thread.start();

				   return;
			   }
          } catch( Exception e1) {
 		    String message = e1.getMessage();
		    if( message==null ) message="unknown error";
            Aladin.warning(this,ERR+"\n["+message+"]");
			if (Aladin.levelTrace>=3) e1.printStackTrace();
			defaultCursor();
            ball.setMode(Ball.HS);
          }
          return;

      } else defaultCursor();

      if (flagTAPV2) {
    	  String url = (String) aladin.glu.aladinDic.get(actionName);
    	  boolean sync = this.sync_async.getSelectedItem().equals("SYNC");
    	  this.submitTapServerRequest(sync, null, label, url);
	  } else {
		  lastPlan = aladin.calque.createPlan(u+"",label,"provided by "+institute,this);
	  }
      
      if( code!=null && lastPlan!=null ) lastPlan.setBookmarkCode(code+" $TARGET $RADIUS");
    }
   
   /**
    * Method to process date and band fields
    * @param c
    * @param takeAction
    * @return
    * @throws Exception
    */
	public StringBuffer processCustomFields(JComponent c, boolean takeAction) throws Exception {
		StringBuffer processedText = null;
		if (isFieldDate(c)) {
			if (date != null) {
				try {
					processedText = setDateInMJDFormat(takeAction, date.getText().trim(),
							rangeValues.get(Constants.SODA_TIMEINDEX - 1));
				} catch (Exception e1) {
					throw e1;
				}
			}
		} else if (isFieldBand(c)) {
			if (band != null) {
				try {
					processedText = processSpectralBand(takeAction, band.getText().trim(),
							rangeValues.get(Constants.SODA_BANDINDEX - 1));
				} catch (NumberFormatException e1) {
					throw new Exception("Error.."+e1.getMessage());
				} catch (Exception e1) {
					throw e1;
				}
			}
		}
		return processedText;
	}	
   
   /**
    * Method checks of operators are correctly used in the input
    * @param input
    * @return
    */
   private String isInValidOperatorNumber(String input, boolean showError) {
	// TODO Auto-generated method stub
	   String inputInProgress = null;
		Pattern regex = Pattern.compile(REGEX_OPNUM);//find no special chars
		Matcher matcher = regex.matcher(input);
		if (matcher.find()){
			if (matcher.group("operator") == null) {
				inputInProgress = "=".concat(input);
			} else {
				inputInProgress = input;
			}
		} else if (showError) {
			Aladin.warning(this, input+" is incorrect! Please rectify. Valid examples are: >3, 10..12, <=-788 etc..");
			ball.setMode(Ball.NOK);
		}
		return inputInProgress;
   }
   
   /**
    * Method retrives the adql value for between operator from a range
    * input 1..3 or 1 3 or 1,3
    * output BETWEEN 1 and 3
    * @param input
    * @return
    */
   private String getRangeInput(String input) {
	   String result = EMPTYSTRING;
		Pattern regex = Pattern.compile(REGEX_RANGEINPUT);
		Matcher matcher = regex.matcher(input);
		if (matcher.find()){
			result = matcher.replaceAll(ADQLVALUE_FORBETWEEN);
		}
		return result;
   }

/**
    * Very specific method in place to ensure that the query is encoded.
    * Assumtion: ADQL query in AlaGlu.dic are not in encoded form.
    * If they will be then this method will no longer be needed.
    * @param url
    * @return
 * @throws UnsupportedEncodingException 
 * @throws MalformedURLException 
    */
   private URL addQueryEncodedUrl(String url) throws UnsupportedEncodingException, MalformedURLException {
	   URL u = null;
	   try {
 		  int offset = url.indexOf("QUERY=");
 	      String adqlQueryUnEncoded = url.substring(offset+6);
 	      String adqlQueryEncoded = URLEncoder.encode(adqlQueryUnEncoded, UTF8);
 	     
 		  url = url.replace(adqlQueryUnEncoded, adqlQueryEncoded);
 		  if(Aladin.levelTrace >= 3){
 			  System.out.println("adqlQueryUnEncoded: "+adqlQueryUnEncoded);//TODO:: tintin sysouts
 			  System.out.println("adqlQueryEncoded: "+adqlQueryEncoded);
 			  System.out.println("Tryin to decode again: "+URLDecoder.decode(adqlQueryEncoded, UTF8));
 		  }
 		  aladin.glu.aladinDic.put(actionName+"v1", url);
 		  u = new URL(url);
		} catch (UnsupportedEncodingException e1) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace>=3) e1.printStackTrace();
			throw e1;
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace>=3) e1.printStackTrace();
			throw e1;
		}
	   return u;
   }
   
   private void setInTap(URL u) {
      String s = u.toString();
      int offset = s.indexOf("QUERY=");
      s = s.substring(offset+6);
      s = URLDecoder.decode(s);
      s=s.replace("FROM", "\n   FROM");
      s=s.replace("WHERE","\n   WHERE");
      tap.setText(s);
   }
   
   /** Lance l'éxécution de l'application system */
   private boolean exec(final String label,String params) {
      aladin.log("exec",actionName);
      final String dir = this.dir;
      final String command = Util.concatDir(dir,getCommand(params));
      Aladin.trace(1,"Exec: "+(dir!=null?"cd "+dir+";":"") +command);
      try {
           new Thread(actionName){
            public void run() {
               try { 
                  Process p = Runtime.getRuntime().exec(command,null,dir==null?null : new File(dir));
                  InputStream stdout = p.getInputStream ();
                  lastPlan = aladin.calque.createPlan(stdout,label,"provided by "+institute);
               }catch( Exception e1) { 
                  String message = e1.getMessage();
                  if( message==null ) message="unknown error";
                  Aladin.warning(ERR+"\n["+message+"]");
                  if (Aladin.levelTrace>=3) e1.printStackTrace();
                  defaultCursor();
                  ball.setMode(Ball.HS);
               }
            }
         }.start(); 
      } catch( Exception e ) { e.printStackTrace(); return false; }
      return true;
   }
   
   private String getCommand(String params) { return aladin.glu.gluSystem(system, params); }


   /** Remplacement dans une chaine de $query par d */
   private String dollarQuerySet(String template) {
      int i=template.indexOf("$query");
      if( target==null || i<0 ) return template;
      String d = target.getText().trim();
      if( d.length()==0 ) d=getDefaultTarget();
      return template.substring(0,i)+d+template.substring(i+6);
   }

   protected Plan lastPlan=null; 	// Dernier plan créé

	URL uT; // url à passer au thread
	String posT; // position à passer au thread
	// appel aux serveurs SIA/SSA/IDHA en asynchrone
	public void run() {
		URL u = uT;

		try {
//			MyInputStream in = new MyInputStream(u.openStream());
			MyInputStream in = Util.openStream(u);
			updateMetaData(in, this, "", posT);

			aladin.dialog.show(this);
			defaultCursor();
            if( tree.isEmpty() ) ball.setMode(Ball.NOK);
		}
		catch (Exception e1) {
		    String message = e1.getMessage();
		    if( message==null ) message="unknown error";
            Aladin.warning(this,ERR+"\n["+message+"]");
			if (Aladin.levelTrace>=3) e1.printStackTrace();
		defaultCursor();
            ball.setMode(Ball.HS);
	}
	}

   /** Interrogation en mode discovery */
	protected MyInputStream getMetaData(String target,String radius,StringBuffer infoUrl) throws Exception {
      String s;
      Enumeration e;

      // Positionnement des nécessaires au target et radius pour le formulaire
      if( resolveTarget(target)==null ) return null;
      resolveRadius(radius,true);
//      setRadius(radius);

      // Mise en place des paramètres
      Vector<String> v = new Vector<String>(10);
      e = vc.elements();
      while( e.hasMoreElements() ) {
         Component c = (Component)e.nextElement();
         if( c instanceof JTextField ) {
            s = ((JTextField)c).getText();
            v.addElement(s);
         } else if( c instanceof JComboBox ) {
            int j;
            s = (String)((JComboBox)c).getSelectedItem();
            if( (j=s.indexOf(" - "))>0 ) s=s.substring(0,j).trim();
            else if( s.equals("?") || s.startsWith("-") && s.endsWith("-")) s="";
            v.addElement(s);
         }
      }
      e = v.elements();
      StringBuffer p=null;
      while( e.hasMoreElements() ) {
         s = (String)e.nextElement();
         if( p==null ) p=new StringBuffer(Glu.quote(s));
         else p.append(" "+Glu.quote(s));
      }

      // Generation de l'URL par appel au GLU, et ouverture du Stream
      URL u = aladin.glu.getURL(actionName,p.toString());
      infoUrl.append(u+"");
      if( type==CATALOG ) return getMetaDataForCat(u);
//      return new MyInputStream(u.openStream());
      return Util.openStream(u);
    }

   private boolean majChoiceSize = true;
   private int maxChoiceWidth = XWIDTH-100;
   /** met à jour la taille des Choice
    *  pour qu'ils ne soient pas trop longs
    *
    */
   protected void majChoiceSize() {
   	if( !majChoiceSize ) return;
    Enumeration e = vc.elements();
    JComboBox choice;
    while( e.hasMoreElements() ) {
       Component c = (Component)e.nextElement();
       if( c instanceof JComboBox ) {
       	  choice = (JComboBox)c;
       	  int orgWidth = choice.getSize().width;
       	  if( orgWidth>maxChoiceWidth ) {
       	  	choice.setSize(maxChoiceWidth, 30);
       	  }
       }
    }
    majChoiceSize = false;
   }

   /** Reset du formulaire */
   protected void reset() {
      if( tree!=null ) tree.resetCb();
      super.reset();
   }

  /** Nettoyage du formulaire */
   protected void clear() {
      int j=0;		// Compteur des champs a remplir
      Component [] c = getComponents();

      // Repositionnement des valeurs par defaut
      for( int i=0; i<c.length; i++ ) {
         if( c[i] instanceof JTextField ) {
            if( vD!=null && vD[j]!=null ) ((JTextField)c[i]).setText(vD[j]);
            else ((JTextField)c[i]).setText("");
            j++;

         // On prend la première valeur au cas où
         } else if( c[i] instanceof JComboBox ) {
            ((JComboBox)c[i]).setSelectedIndex(0) ;
            j++;
         }
      }

      if( tree!=null ) tree.clear();

      super.clear();
   }
   
   protected boolean updateWidgets() {
      if( flagTAP ) {
         System.out.println("Je dois rééditer la chaine ADQL");
         submit1(false, false);
      }
      return super.updateWidgets();
   }

   public void actionPerformed(ActionEvent e) {
      Object o = e.getSource();
      if( o instanceof JButton) {
    	  String action = ((JButton)o).getActionCommand();
    	  if (action.equals(HELP)) {
    		  if( docUser.startsWith("http://") ) aladin.glu.showDocument("Http",docUser,true);
    	         else aladin.glu.showDocument(docUser,"");
    	         return;
    	  }
      }
      if (flagTAPV2) {
    	  if (o instanceof JButton) {
    		TapManager tapManager = TapManager.getInstance(aladin);
  			String action = ((JButton) o).getActionCommand();
  			if (action.equals(CHECKQUERY)) {
  				this.submit1(false, true);
  				if (!ball.isRed()) {
  					checkQueryFlagMessage();
				}
  			}/* else if (action.equals(WRITEQUERY)) {
  				this.submit1(false, true);
  			}*/ else if (action.equals(CHANGESERVER)) {
  				try {
					tapManager.showTapRegistryForm();
				} catch (Exception e1) {
					Aladin.warning(this, GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			} else if (action.equals(SHOWAYNCJOBS)) {
				try {
					tapManager.showAsyncPanel();
				} catch (Exception e1) {
					Aladin.warning(this, GENERICERROR);
		            ball.setMode(Ball.NOK);
				}
			}

  		} else if (o instanceof JComboBox) {
  			String action = ((JComboBox) o).getActionCommand();
  			if (action.equals(TABLECHANGED)) {
  				String tableSelected = (String) ((JComboBox) o).getSelectedItem();
  				this.changeTapTable(tableSelected);
  			}

  		}
      	} else if( flagTAP) updateWidgets();

      super.actionPerformed(e);
   }

   public HealpixMoc getPosBounds() {
      return posBounds;
   }

   public void setPosBounds(HealpixMoc posBounds) {
      this.posBounds = posBounds;
   }

   public Map<String, GluAdqlTemplate> getGluAdqlQueryTemplates() {
      return gluAdqlQueryTemplates;
   }

   public void setGluAdqlQueryTemplates(Map<String, GluAdqlTemplate> gluAdqlQueryTemplates) {
      this.gluAdqlQueryTemplates = gluAdqlQueryTemplates;
   }

   public String getCurrentSelectedTapTable() {
      return currentSelectedTapTable;
   }

   public void setCurrentSelectedTapTable(String currentSelectedTapTable) {
      this.currentSelectedTapTable = currentSelectedTapTable;
   }

   public Hashtable<String, String> getAdqlFunc() {
      return adqlFunc;
   }

   public void setAdqlFunc(Hashtable<String, String> adqlFunc) {
      this.adqlFunc = adqlFunc;
   }

   public Hashtable<String, String> getAdqlFuncParams() {
      return adqlFuncParams;
   }

   public void setAdqlFuncParams(Hashtable<String, String> adqlFuncParams) {
      this.adqlFuncParams = adqlFuncParams;
   }
}
