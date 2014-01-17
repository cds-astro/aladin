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
import java.awt.event.*;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

import javax.swing.*;

import cds.tools.Util;


/**	Classe MetaDataTree
 *  Arbre utilisé pour afficher les ressources sous forme hiérarchique
 * 	@author Thomas Boch [CDS]
 * 	@version 0.7 Mars 2006 - Ajout des étiquettes colorées pour les répertoires
 *           0.6 5 Juin 2003 - Découplement de cette classe avec TreeView
 * 		     0.5 Février 2003 - Création
 */
public class MetaDataTree extends BasicTree implements WidgetFinder, KeyListener /*, Runnable*/ {

	static final String OPENWITHALADIN = "Aladin";
//	static final String OPENWITHVOSPEC = "VOSpec";

	// chaines nécessaires
	static String NOLOC,NOIMG,IMAGE,NOSPEC,NOSPEC1,BADURL,ERRSPEC, OPENWITH, PLASTICAPPS;

	protected static final Color[] LABEL_COL = {Aladin.BKGD, new Color(249,188,97), new Color(208,160,224), new Color(192,229,107), new Color(255,124,116), new Color(127,183,255)};

	protected static Color[] LABEL_COL1;
	protected static Color[] LABEL_COL2;
	protected static Color[] LABEL_COL3;
	protected static Color[] LABEL_COL4;


	private boolean colorLabel = false; // colorie-t-on les noeuds non terminaux en fonction de leur type

	// message quand aucun cutout n'est dispo
	private static final String NOIMAGE_WARNING = "can not be loaded : \nno cutout available at this position !\nClick on the region you are interested in\nAvailable images will be marked with a red tick";
	private static final String SORTBY = "Sort by ...";

    private boolean enableKdShortcut = true; // activation ou non des raccourcis clavier

	// pour avertir un objet d'un passage vue à plat <--> vue hiérarchique
	MyListener stateChangerLstr;

	// ouvre-t-on tous les noeuds au départ ?
	private boolean fullExpandAtStart = true;


	private static SortableColumn[] columns = new SortableColumn[6];
	// Initialisation statique
	static {

		createLabelColors();

		// Nom du survey
		columns[0] = new SortableColumn() {
			public String getColName(ResourceNode n) {
				return n.survey!=null?n.survey:"";
			}

			public int compare(Object o1, Object o2, boolean goOn) {
				ResourceNode n1 = (ResourceNode)o1;
				ResourceNode n2 = (ResourceNode)o2;

				// comparaison primaire
				int ret = getColName(n1).compareTo(getColName(n2));

				if( ret!=0 || !goOn ) return ret;

				ret = columns[1].compare(n1,n2,false);
				if( ret!=0 ) return ret;
				ret = columns[2].compare(n1,n2,false);
				return ret;
			}
		};

		// Couleur (bande)
		columns[1] = new SortableColumn() {
			public String getColName(ResourceNode n) {
				String s="";
				if( n.bandPass!=null ) {
					s += n.bandPass;
					if( n.wavelengthExpla!=null ) s += "("+n.wavelengthExpla+")";
					else if( n.wavelength!=null ) s += "("+n.wavelength+")";
				}
				return s;
			}

			public int compare(Object o1, Object o2, boolean goOn) {
				ResourceNode n1 = (ResourceNode)o1;
				ResourceNode n2 = (ResourceNode)o2;

				// comparaison primaire
				int ret = getColName(n1).compareTo(getColName(n2));

				if( ret!=0 || !goOn ) return ret;

				ret = columns[0].compare(n1,n2,false);
				if( ret!=0 ) return ret;
				ret = columns[2].compare(n1,n2,false);
				return ret;
			}
		};

		// Taille de l'image
		columns[2] = new SortableColumn() {
			public String getColName(ResourceNode n) {
				if( n.getFov()==null ) return "";
				return n.getFov().getSizeStr(n.cutout);
			}

			public int compare(Object o1, Object o2, boolean goOn) {
				ResourceNode n1 = (ResourceNode)o1;
				ResourceNode n2 = (ResourceNode)o2;

				// comparaison primaire
				int ret;
				if( n1.getFov()==null ) {
					if( n2.getFov()==null ) ret=0;
					else ret = -1;
				}
				else if( n2.getFov()==null ) {
					ret = 1;
				}
				else {
					double size1 = n1.cutout?n1.getFov().cutout_x*n1.getFov().cutout_y:n1.getFov().x*n1.getFov().y;
					double size2 = n2.cutout?n2.getFov().cutout_x*n2.getFov().cutout_y:n2.getFov().x*n2.getFov().y;
					if( size1==size2 ) ret = 0;
					else ret = size1>size2?1:-1;
				}

				if( ret!=0 || !goOn ) return ret;

				ret = columns[0].compare(n1,n2,false);
				if( ret!=0 ) return ret;
				ret = columns[1].compare(n1,n2,false);
				return ret;
			}
		};

		// Date d'observation
		columns[5] = new SortableColumn() {
			public String getColName(ResourceNode n) {
				return n.obsDate!=null?n.obsDate:"";
			}

			public int compare(Object o1, Object o2, boolean goOn) {
				ResourceNode n1 = (ResourceNode)o1;
				ResourceNode n2 = (ResourceNode)o2;

				return getColName(n1).compareTo(getColName(n2));
			}
		};

		// Taille du pixel
		columns[4] = new SortableColumn(){
			public String getColName(ResourceNode n) {
				return n.getPixSize()!=null?n.getPixSize():"";
			}

			public int compare(Object o1, Object o2, boolean goOn) {
				ResourceNode n1 = (ResourceNode)o1;
				ResourceNode n2 = (ResourceNode)o2;

				int ret;
				if( n1.getPixSizeDeg()==0. ) {
					if( n2.getPixSizeDeg()==0. ) ret=0;
					else ret = -1;
				}
				else if( n2.getPixSizeDeg()==0. ) {
					ret = 1;
				}
				else {
					if( n1.getPixSizeDeg()==n2.getPixSizeDeg() ) ret = 0;
					else ret = n1.getPixSizeDeg()>n2.getPixSizeDeg()?1:-1;
				}

				return ret;
			}
		};

		// Obs ID
		columns[3] = new SortableColumn(){
			public String getColName(ResourceNode n) {
				String ret="";
				if( n.machine!=null && n.machine.length()!=0 ) ret += n.machine+".";
				ret += n.name;
				return ret;
			}

			public int compare(Object o1, Object o2, boolean goOn) {
				ResourceNode n1 = (ResourceNode)o1;
				ResourceNode n2 = (ResourceNode)o2;
				return getColName(n1).compareTo(getColName(n2));
			}
		};
	}


	/** creation des couleurs pour les étiquettes des répertoires */
	static private void createLabelColors() {
		int nbCol = LABEL_COL.length;
		LABEL_COL1 = new Color[nbCol];
		LABEL_COL2 = new Color[nbCol];
		LABEL_COL3 = new Color[nbCol];
		LABEL_COL4 = new Color[nbCol];

		double increment = 0.1;

		for( int i=0; i<LABEL_COL.length; i++ ) {
//			LABEL_COL2[i] = LABEL_COL[i].brighter();
//			LABEL_COL1[i] = LABEL_COL2[i].brighter();
//			LABEL_COL3[i] = LABEL_COL[i].darker();
//			LABEL_COL4[i] = LABEL_COL3[i].darker();

			LABEL_COL1[i] = new Color( (int)(LABEL_COL[i].getRed()+(255-LABEL_COL[i].getRed())*3*increment), (int)(LABEL_COL[i].getGreen()+(255-LABEL_COL[i].getGreen())*3*increment), (int)(LABEL_COL[i].getBlue()+(255-LABEL_COL[i].getBlue())*3*increment) );
			LABEL_COL2[i] = new Color( (int)(LABEL_COL[i].getRed()+(255-LABEL_COL[i].getRed())*increment), (int)(LABEL_COL[i].getGreen()+(255-LABEL_COL[i].getGreen())*increment), (int)(LABEL_COL[i].getBlue()+(255-LABEL_COL[i].getBlue())*increment) );
			LABEL_COL3[i] = new Color( (int)(LABEL_COL[i].getRed()-(255-LABEL_COL[i].getRed())*increment), (int)(LABEL_COL[i].getGreen()-(255-LABEL_COL[i].getGreen())*increment), (int)(LABEL_COL[i].getBlue()-(255-LABEL_COL[i].getBlue())*increment) );
			LABEL_COL4[i] = new Color( (int)(LABEL_COL[i].getRed()-(255-LABEL_COL[i].getRed())*3*increment), (int)(LABEL_COL[i].getGreen()-(255-LABEL_COL[i].getGreen())*3*increment), (int)(LABEL_COL[i].getBlue()-(255-LABEL_COL[i].getBlue())*3*increment) );
		}
	}

	protected synchronized void createChaine() {
		// pour éviter des créations en cascade
		if( NOLOC!=null ) return;
		NOLOC = aladin.chaine.getString("MTNOLOC");
		NOIMG = aladin.chaine.getString("MTNOIMG");
		IMAGE = aladin.chaine.getString("IMAGE");
		NOSPEC = aladin.chaine.getString("MTNOSPEC");
		NOSPEC1 = aladin.chaine.getString("MTNOSPEC1");
		BADURL = aladin.chaine.getString("MTBADURL");
		OPENWITH = aladin.chaine.getString("MTOPENWITH");
		String appMsgProtocolName = aladin.getMessagingMgr().getProtocolName();
		PLASTICAPPS = aladin.chaine.getString("MTPLASTICAPPS").replaceAll("SAMP", appMsgProtocolName);
	}

	// Constructeurs
	MetaDataTree(BasicNode rootNode, Aladin aladin, JScrollPane scroll) {
		super(aladin, rootNode, scroll);
		createChaine();
        if( enableKdShortcut ) addKeyListener(this);
	}

	MetaDataTree(Aladin aladin, JScrollPane scroll) {
    	this(new ResourceNode(aladin), aladin, scroll);
	}

	MetaDataTree(BasicNode rootNode, Aladin aladin, JScrollPane scroll,boolean isHistoryTree) {
		super(aladin, rootNode, scroll, isHistoryTree);
		createChaine();
        if( enableKdShortcut ) addKeyListener(this);
	}

	//// Fin constructeurs ////

	/** Ajoute newNode comme sous-noeud de parent
	 *  Met l'arbre à jour pour que newNode apparaisse
	 */
	void addNode(ResourceNode parent, ResourceNode newNode) {
		parent.addChild(newNode);
		traverseTree();
		getStartPosition(newNode);
//		init();
		// nécessaire ?
		//if( scroll!=null ) scroll.doLayout();
		repaint();
	}

	/** Change la racine de l'arbre et reconstruit le tout */
	void setRoot(ResourceNode newRoot) {
		setRootNode(newRoot);
		traverseTree();
		getStartPosition(getRootNode());
//		init();
		repaint();
	}



	// eteint tous les noeuds couramment affiches
	protected void turnOffAllNodes() {
		ResourceNode curNode;
		if( nodeTab==null ) return;

		for( int i=0; i<nodeTab.length; i++ ) {
			curNode = (ResourceNode)nodeTab[i];
			if( curNode.isLeaf && curNode.type == ResourceNode.IMAGE ) turnOffNode(curNode);
		}
//		repaint();
	}

	// "Eteint" le noeud
	protected void turnOffNode(ResourceNode node) {
		// pas besoin d'eteindre si le noeud n'etait pas allume
		if( !node.litup ) return;
		node.litup = false;

        Graphics h = getGraphics();

		// enlève la checkbox évidée
		drawPreCheckBox(h,node.x-XSPACE, node.y, node, true);

		int lineLength = h.getFontMetrics().stringWidth(getName(node));
		h.setColor(Aladin.LBLUE);
		h.fillRect( node.x-1,(node.y/YSPACE)*YSPACE,
		            lineLength+2,YSPACE);
		if( node.equals(lastInfoNode) ) h.setFont(boldNameFont);
		else h.setFont(nameFont);
		h.setColor(Color.black);
		if( node.isLeaf && node.equals(lastInfoNode) ) drawBorder(h,node, lineLength);
		h.drawString(getName(node),node.x,node.y+YSPACE/2+4);
	}

	// Allume le noeud
	protected void litUpNode(BasicNode node) {
		// pas besoin d'allumer si le noeud n'etait pas eteint
		if( node.litup ) return;
		node.litup = true;

        Graphics h = getGraphics();

		// dessin de la checkbox évidée
		drawPreCheckBox(h,node.x-XSPACE, node.y, node, false);

		int lineLength = h.getFontMetrics().stringWidth(getName(node));
		h.setColor(LITBGCOLOR);
		h.fillRect( node.x-1,(node.y/YSPACE)*YSPACE,
	                lineLength+2,YSPACE);
		h.setColor(LITFGCOLOR);
		if( node.equals(lastInfoNode) ) h.setFont(boldNameFont);
		else h.setFont(nameFont);
		if( node.isLeaf && node.equals(lastInfoNode) ) drawBorder(h,node, lineLength);
		h.drawString(getName(node),node.x,node.y+YSPACE/2+4);
	}

	// positionne l'arbre en position de depart, ie noeuds de valeurs de critères fermés
	private void getStartPosition(BasicNode[] tab) {
		BasicNode node;
		for( int i=0;i<tab.length;i++ ) {
			node = tab[i];
			if( node.isLeaf ) continue;
			if( !fullExpandAtStart && node.nbChildren>0 && ( node.getChildrenAt(0).isLeaf || ((ResourceNode)node).valueCriteria!=null ) ) node.isOpen = false;
			else node.isOpen = true;
			// le noeud racine est toujours ouvert
			if( node.equals(getRootNode()) ) node.isOpen = true;
		}
	}

	protected void getStartPosition() {
		getStartPosition(nodeFullTab);
	}

	protected void getStartPosition(BasicNode myNode) {
		Vector v = new Vector();
       getAllSubnodes(myNode,v);
       v.addElement(myNode);
       BasicNode[] tab = new BasicNode[v.size()];
       v.copyInto(tab);
       v=null;
       getStartPosition(tab);
   }

       // Override de certaines méthodes

      public void setFlat(boolean b) {
        indexSort = -1;

        super.setFlat(b);
      }

      BasicNode[] getLeavesForFlatView(BasicNode node) {
          BasicNode[] nodes = super.getLeavesForFlatView(node);
          ResourceNode[] ret= new ResourceNode[nodes.length];
          for( int i=0; i<ret.length; i++ ) ret[i] = (ResourceNode)nodes[i];
          if( flatView && sortable && indexSort>=0 ) {
              sort(ret, columns[indexSort], descSort);
          }
          return ret;
      }

      int indexSort = -1; // index de la colonne sur laquelle on demande le tri
      boolean descSort = true; // si vrai tri descendant, ascendant sinon
      public void mousePressed(MouseEvent evt) {
         int x = evt.getX();
         int y = evt.getY();
          // si cette condition est vraie, on a p-e cliqué sur un onglet de tri
          if( flatView && sortable && nodeFullTab.length>1 && y<YSPACE
                && (evt.getModifiers() & InputEvent.BUTTON3_MASK)==0 ) {
              for( int i=0; i<colBounds.length; i++ ) {
                  if( x<colBounds[i][0] ) return;
                  // on a trouvé l'onglet sur lequel on a cliqué
                  if( x<=colBounds[i][1] ) {
                      if( indexSort==i ) descSort = !descSort;
                      else descSort = true;
                      indexSort = i;
//                      doDisplay();
                      repaint();
                  }
              }
          }
          super.mousePressed(evt);
      }

       protected void onNodeExpanded(BasicNode node) {}
       protected void onNodeCollapsed(BasicNode node) {}
       protected void onNodeSelected(BasicNode node) {
           showInfo((ResourceNode)node);
       }
       protected void onNodeSelectedDbleClick(BasicNode node) {
           load((ResourceNode)node,null);
       }
       protected void onNodeRemoved(BasicNode node) {
           // si le noeud supprimé ou un de ses sous-noeuds est le noeud lastInfoNode
           Vector vec = new Vector();
           getAllSubnodes(node,vec);
           if( node.equals(lastInfoNode) || vec.contains(lastInfoNode) ) {
               lastInfoNode = null;
           }
       }
       protected void onMouseMoved(BasicNode node, boolean inNodeName) {
           ResourceNode curNode = (ResourceNode)node;
           // we show the field of view of the image or spectrum
			if( (curNode.type == ResourceNode.IMAGE || curNode.type==ResourceNode.SPECTRUM ) && inNodeName ) {
        	   showFov(curNode);
               showCutoutFov(curNode);
           }
           else deactivateCutoutFov();

           Plan pref = aladin.calque.getPlanRef();

           // we set the localization textfield to the center of the ressource
           if( curNode.isLeaf && curNode.type==ResourceNode.IMAGE && inNodeName && pref==null ) {
               Coord coo = getPos(curNode);
               if( coo!=null ) aladin.localisation.setTextSaisie(aladin.localisation.J2000ToString(coo.al, coo.del));
               else resetLocalisation();
           }
           else if( pref==null && aladin.localisation.getTextSaisie().length()>0 ) {
               resetLocalisation();
           }

		   if( !inNodeName || (curNode.type!=ResourceNode.IMAGE && curNode.type!=ResourceNode.SPECTRUM) ) {
               hideFov();
           }
       }

       private void resetLocalisation() {
           aladin.localisation.setTextSaisie("");
       }

       /** Remplit le Menu "Open with ..." selon le type de noeud et
        * l'état des connexions PLASTIC
        * @param m le menu à remplir
        * @param node le noeud concerné
        */
       private void fillOpenWith(JMenu m, ResourceNode node) {
       	// d'abord, on cherche un visualiseur "local"
       	   if( node.type==ResourceNode.IMAGE || node.type==ResourceNode.CAT ) {
       	       m.add(createItem(OPENWITHALADIN));
       	       m.addSeparator();
       	   }
       	   // à supprimer TODO
//       	   else if( node.type==ResourceNode.SPECTRUM ) {
//       	       m.add(OPENWITHVOSPEC);
//       	   }

       	   if( Aladin.PLASTIC_SUPPORT ) {
       	   	   JMenuItem mi;

       	       mi = createItem(PLASTICAPPS);
       	       mi.setFont(Aladin.BOLD);
       	       m.add(mi);
       	       JMenuItem plasticMenu = mi;

       	       AppMessagingInterface.AbstractMessage msg = node.getPlasticMsg();

       	       ArrayList<String> apps = aladin.getMessagingMgr().getAppsSupporting(msg);
       	       System.out.println(apps.size());

       	       if( apps!=null ) {
//				   m.addSeparator();
       	           for (String app: apps ) {
       	           	   mi = createItem(app);
       	           	   mi.setActionCommand(PLASTICAPPS);
       	               m.add(mi);
       	           }
       	       }

       	       // pour catcher un bug de Java !!!
       	       try {
       	       	  plasticMenu.setEnabled(apps!=null && apps.size()>0
       	       	                         && aladin.getMessagingMgr().isRegistered());
       	       }
       	       catch(NullPointerException npe) {}
       	   }
       }


       /** ajoute à pop les combinaisons possibles d'éléments de sort */
       private void createMenuItem(JComponent pop, String[] sort) {


           // cas terminal : il ne reste plus que 2 éléments
           if( sort.length==1 ) {
               JMenuItem item = new JMenuItem(sort[0]);
               String cmd = pop instanceof JMenu ? ((JMenu)pop).getActionCommand() : "";
               item.setActionCommand(cmd+";"+sort[0]);
               pop.add(item);
               return;
           }

           // récursion
           for( int i=0; i<sort.length; i++ ) {
               JMenu menu = new JMenu(sort[i]);
               String cmd = pop instanceof JMenu ? ((JMenu)pop).getActionCommand() : "";
               menu.setActionCommand(cmd+";"+sort[i]);
               pop.add(menu);
               String[] newSort = new String[sort.length-1];
               int start=0;
               // on copie ce qui se trouve avant i
               if( i>0 ) {
                   System.arraycopy(sort,0,newSort,0,i);
                   start = i;
               }

               // on copie ce qui se trouve après i
               if( i<sort.length-1 ) {
                   System.arraycopy(sort,i+1,newSort,start,sort.length-(i+1));
               }

               // appel récursif
               createMenuItem(menu, newSort);
           }
       }

       protected void onRightClickInNode(BasicNode node, int x, int y) {
           ResourceNode resNode = (ResourceNode)node;
           popup.removeAll();

           // si criteriaVal n'est pas null, on ajoute des éléments au popupmenu
           if( resNode.sortCriteria!=null && resNode.sortCriteria.length>1 ) {
               // ligne de séparation
               popup.addSeparator();
               popup.add(createItem(SORTBY));
               createMenuItem(popup,resNode.sortCriteria);
           }
           // ajout de l'option "Open with"
           if( resNode.isLeaf ) {
               JMenu openWith = new JMenu(OPENWITH);
               popup.add(openWith);
               fillOpenWith(openWith, resNode);
           }

           popup.addSeparator();
           popup.add(createItem(COLLAPSE_ALL));
           popup.add(createItem(EXPAND_ALL));

           if( !isHistoryTree && hasSpectraOrImages ) {
               popup.addSeparator();
               popup.add(createItem(CREATE_CATPLANE));
           }


           // ligne de séparation
           popup.addSeparator();
           popup.add(flatView?createItem(HIER_VIEW):createItem(FLAT_VIEW));

		if( !flatView && resNode.isSIAPEvol ) {
              // ligne de séparation
              popup.addSeparator();
              popup.add(createItem(SIAP_EVOL_SORT));
           }

           popup.show(this,x,y);
       }

       public void actionPerformed(ActionEvent ae) {
           super.actionPerformed(ae);

           Object src = ae.getSource();
           String o = ae.getActionCommand();

           ResourceNode n = (ResourceNode)selectedNode;

           // action trappée dans onNodeRemoved
           if( o.equals(DELETE) ) return;

           // traité dans BasicTree.action()
           else if( o.equals(COLLAPSE_SUBTREE) || o.equals(EXPAND_SUBTREE) ) return;


           else if( src instanceof JMenuItem ) {


               // open with Aladin
               if( o.equals(OPENWITHALADIN) ) {
                   load(n,null);
                   return;
               }

               String text = ((JMenuItem)src).getText();

               // do nothing
               if( text.equals(PLASTICAPPS) ) return;

               // ouverture avec une appli PLASTIC
               if( ((JMenuItem)src).getActionCommand().equals(PLASTICAPPS) ) {
                   loadNodeWithPlastic(n, text);
                   return;
               }

               // quelle horreur, je me demande si je ne ferai pas mieux de coller des ActionListener
               if( o.equals(SORTBY) || o.equals(COLLAPSE_ALL)
                       || o.equals(EXPAND_ALL) || o.equals(FLAT_VIEW) || o.equals(HIER_VIEW) ) {
                   if( stateChangerLstr!=null ) stateChangerLstr.fireStateChange(o);
                   return;
               }

               if( o.equals(SIAP_EVOL_SORT) ) {
                   sortSiapEvol();
                   return;
               }

               else if( o.equals(CREATE_CATPLANE) ) {
                   creatCatPlaneForSpectra((ResourceNode)getRootNode());
                   return;
               }

               sortNode(n, ((JMenuItem)src).getActionCommand());
               //System.out.println(((MenuItem)e.target).getActionCommand());
           }
       }


      private void creatCatPlaneForSpectra(ResourceNode beginNode) {
      	// first, get all spectra leaves in the (sub)tree
      	Vector leaves = new Vector();
  		getAllLeaves(beginNode, leaves);

  		if( beginNode.isLeaf ) leaves.addElement(beginNode);

  		Vector spectra = new Vector();
  		ResourceNode node;
  		for( int i=0; i<leaves.size(); i++) {
  			node = (ResourceNode)leaves.elementAt(i);
  			if( node.type==ResourceNode.SPECTRUM || node.type==ResourceNode.IMAGE ) spectra.addElement(leaves.elementAt(i));
  		}

  		if( spectra.size()==0 ) {
  			Aladin.warning(this, "No spectrum/image found in this tree !");
  		}
  		else {
  			ResourceNode[] spectraTab = new ResourceNode[spectra.size()];
  			spectra.copyInto(spectraTab);
  			spectra = null;

  			StringBuffer sb = new StringBuffer();
  			sb.append("<?xml version=\"1.0\"?>\n"+
  			"<!DOCTYPE VOTABLE SYSTEM \"http://us-vo.org/xml/VOTable.dtd\">\n"+
  			"<VOTABLE version=\"1.0\" xmlns=\"http://vizier.u-strasbg.fr/VOTable\">\n"+
  			"  <DESCRIPTION>Object selection from Aladin</DESCRIPTION>\n"+
  			"  <DEFINITIONS>\n"+
  			"    <COOSYS ID=\"J2000\" equinox=\"2000.\" epoch=\"2000\" system=\"eq_FK5\"/>\n"+
  			"  </DEFINITIONS>\n");

  			sb.append("<RESOURCE>\n");
  			sb.append("<TABLE>\n");

//  			sb.append("<FIELD name=\"Sp\" width=\"1\">\n"+
//  			"<LINK content-type=\"spectrumavo/fits\" title=\"Spectrum\" gref=\"Http ${url_spectrum}\"/>\n"+
//  		  	"</FIELD>\n");
  			sb.append("<FIELD name=\"name\" datatype=\"char\" arraysize=\"*\" />\n");
  		  	sb.append("<FIELD name=\"RA\" ucd=\"pos.eq.ra;meta.main\" unit=\"deg\" datatype=\"float\" />\n");
  			sb.append("<FIELD name=\"DE\" ucd=\"pos.eq.dec;meta.main\" unit=\"deg\" datatype=\"float\" />\n");
  			sb.append("<FIELD name=\"url_data\" ucd=\"meta.ref.url\"  datatype=\"char\" arraysize=\"*\"  type=\"hidden\" >\n");
  			sb.append("</FIELD>\n");

  			sb.append("<DATA><TABLEDATA>\n");

              String target = "";

  			for( int i=0; i<spectraTab.length; i++ ) {

  				sb.append("<TR>");
//  				sb.append("<TD>S</TD>");
  				sb.append("<TD>"+spectraTab[i].name+"</TD>");
  				if( spectraTab[i].getFov()!=null ) {
                      // recherche du target
                      if( target!=null && target.length()==0 && spectraTab[i].objet!=null )
                          target = spectraTab[i].objet;

  					sb.append("<TD>"+spectraTab[i].getFov().alpha+"</TD>");
  					sb.append("<TD>"+spectraTab[i].getFov().delta+"</TD>");
  				}
  				/*
  				else if( spectraTab[i].ra>=0 && spectraTab[i].de>=0 ) {
  					sb.append("<TD>"+spectraTab[i].explanation[spectraTab[i].ra]+"</TD>");
  					sb.append("<TD>"+spectraTab[i].explanation[spectraTab[i].de]+"</TD>");
  				}
  				*/
  				else {
  					sb.append("<TD></TD>");
  					sb.append("<TD></TD>");
  				}
  				String location = spectraTab[i].location!=null?spectraTab[i].location:"";
  				sb.append("<TD>"+URLEncoder.encode(location)+"</TD>");
  				sb.append("</TR>\n");
  			}


  			sb.append("</TABLEDATA></DATA>\n");

  			sb.append("</TABLE>\n");
  			sb.append("</RESOURCE>\n");

  			sb.append("</VOTABLE>");


  			try {
//  			System.out.println(sb.toString());
  			    int n = aladin.calque.newPlanCatalog( new MyInputStream(
  				    new BufferedInputStream(new ByteArrayInputStream(
  				    sb.toString().getBytes()))), "toto");
                  if( n>=0 ) {
                      String orgLabel = "Spectra."+target;
                      String newLabel = new String(orgLabel);
                      int count = 0;
                      // unicité du label
                      while( getPlaneByName(newLabel, aladin)!=null  ) {
                          count++;
                          newLabel = orgLabel+"_"+count;
                      }

                      //aladin.calque.plan[n].setLabel("Spectra."+target);
                      aladin.calque.plan[n].setLabel(newLabel);
                  }
  			}
  			catch( Exception e) {e.printStackTrace();}
  		}
      }

      static protected Plan getPlaneByName(String s, Aladin a) {
          Plan p;

          for( int i=a.calque.plan.length-1;i>=0;i-- ) {
             p = a.calque.plan[i];
             if( p.label!=null && p.label.equals(s)) {
                return p;
             }
          }

         return null;
       }


      /**
       * Charge un noeud dans l'appli PLASTIC/SAMP plasticApp
       *
       * @param n
       * @param plasticApp
       */
      protected void loadNodeWithPlastic(ResourceNode n, String plasticApp) {
          if( n==null || plasticApp==null ) return;

          AppMessagingInterface mMgr = aladin.getMessagingMgr();
          Object msg = n.getPlasticMsg();
          if( msg==null ) {
              Aladin.trace(3, "Could not find a "+mMgr.getProtocolName()+" message to load node, aborting !");
              return;
          }

          java.util.List argsList = new ArrayList();

          String url = resolveLocation(n.location, n, null).toString();
          if( url==null ) {
              Aladin.trace(3, "Could not resolve location of the resource to load");
              return;
          }

          java.util.List recipientsList = new ArrayList();
          recipientsList.add(mMgr.getAppWithName(plasticApp));

          // une petite animation pour informer l'utilisateur que qqch se passe
          mMgr.getPlasticWidget().animateWidgetSend();


          // envoi message chargement spectre
          if( msg.equals(AppMessagingInterface.ABSTRACT_MSG_LOAD_SPECTRUM_FROM_URL) ) {
              mMgr.sendMessageLoadSpectrum(url, url, n.name, n.getMetadata(), recipientsList);
          }
          // envoi message CHARAC
          else if( msg.equals(AppMessagingInterface.ABSTRACT_MSG_LOAD_CHARAC_FROM_URL) ) {
              mMgr.sendMessageLoadCharac(url, n.name, recipientsList);
          }
          else if( msg.equals(AppMessagingInterface.ABSTRACT_MSG_LOAD_FITS) ) {
              mMgr.sendMessageLoadImage(url, n.name, recipientsList);
          }

          Aladin.trace(3, "Sending data or spectrum with url "+url);
          aladin.glu.log(mMgr.getProtocolName(), "sending data or spectrum URL");
      }

      SortFrame sortFrame;
      private void sortSiapEvol() {
          if( sortFrame==null ) sortFrame = new SortFrame("Choose sort fields");
          sortFrame.updateList();
          sortFrame.pack();
          sortFrame.show();
          sortFrame.toFront();
      }

      static protected void doSortSiapEvol(String[] items, BasicNode rootNode) {
        String curItem, curValue;
        Vector v = new Vector();

        getAllObs(rootNode, v);

        ResourceNode[] obs = new ResourceNode[v.size()];
        v.copyInto(obs);
        v = null;

        for( int i=items.length-1; i>=0; i-- ) {
        	genericSort(obs, items[i], true);
        }

        ResourceNode curNode;
        ResourceNode fatherNode, curParent, node;

        rootNode.removeAllChild();

        for( int i=0; i<obs.length; i++ ) {
            curNode = obs[i];

            curParent = (ResourceNode)rootNode;

            for( int j=0; j<items.length; j++ ) {
                curItem = items[j];
                curValue = curItem + "::: " + (String)curNode.properties.get(curItem)+" ///"+(String)curNode.propertiesUnits.get(curItem);

                node = (ResourceNode)curParent.getChild(curValue);
                if( node==null ) {
                    ResourceNode tmpNode = null;
                    if( curNode.links!=null ) {
                    	tmpNode = (ResourceNode)curNode.links.get(curItem);
                    }

                    if( tmpNode==null ) node = new ResourceNode(rootNode.aladin, curValue);
                    else node = new ResourceNode(rootNode.aladin, tmpNode);

                    node.type = curNode.type;

                    curParent.addChild(node);
                }
                // le nouveau parent est le noeud créé ou trouvé
                curParent = node;

            }

            curParent.addChild(curNode);
        }

      }

      /**
       * Effectue le tri pour SIAP evolution, SIAP Extensions
       * Met à jour l'arbre
       * @param items tableau des champs sur lesquel trier
       */
      private void doSortSiapEvol(String[] items) {
      	  doSortSiapEvol(items, getRootNode());

          traverseTree();
//          init();
//          doDisplay();
          repaint();
      }

      /** trie les sous-noeuds de node en appliquant l'ordre des critères définis dans critStr */
      private void sortNode(ResourceNode node, String critStr) {
          // récupération de l'ordre de tri
           StringTokenizer st = new StringTokenizer(critStr,";");
           String[] crit = new String[st.countTokens()];
           for( int i=0;i<crit.length;i++) {
               crit[i] = st.nextToken();
           }
           // récupération de toutes les feuilles
           Vector vec = new Vector();
           getAllLeaves(node,vec);
           ResourceNode[] allLeaves = new ResourceNode[vec.size()];
           vec.copyInto(allLeaves);
           vec = null;

           vec = new Vector();
           getAllNonLeaves(node,vec);
           Enumeration e = vec.elements();
           Hashtable hash = new Hashtable();
           ResourceNode bn;
           while(e.hasMoreElements()) {
               bn = (ResourceNode)e.nextElement();
               bn.removeAllChild();
               hash.put(bn.name,bn);
           }

           node.removeAllChild();
           // tri effectif
           sortAndCreate(allLeaves,node,crit,hash);

           traverseTree();
//           doDisplay();
           repaint();
      }
      /** "Trie" des noeuds en fonction de critères
       *
       * @param tab tableau des noeuds à trier
       * @param parent noeud auquel on rattache le tout
       * @param criteria tableau des noms de critères
       * @param nonLeaves association "nom du noeud" (== valeur de critère) ==> noeud
       */
      private void sortAndCreate(ResourceNode[] tab, ResourceNode parent, String[] criteria, Hashtable nonLeaves) {
              if( tab.length==0 ) return;
              ResourceNode curLeaf;

              for( int i=0; i<tab.length; i++ ) {
                   curLeaf = tab[i];
                   Hashtable value = curLeaf.criteriaVal;
                   //System.out.println(curLeaf.name+" thomas");
                   // parent auquel raccrocher le nouveau noeud créé
                   ResourceNode curParent=parent;
                   ResourceNode node;
                   String name = null;
                   // on crée les noeuds dans l'ordre imposé par criteria
                   for( int j=0; j<criteria.length; j++ ) {
                       //System.out.println("nom de critère : "+criteria[j]);
                       name = (String)value.get(criteria[j]);
                       //System.out.println("valeur de critère : "+name);
                       node = (ResourceNode)curParent.getChild(name);
                       if( node==null ) {
                           // faut faire un new
                           node = new ResourceNode(aladin, (ResourceNode)nonLeaves.get(name));
                           curParent.addChild(node);
                       }
                       // le nouveau parent est le noeud créé ou trouvé
                       curParent = node;
                   }

                   // fin de la boucle : on ajoute curLeaf au dernier noeud créé ou trouvé
                   //System.out.println(curLeaf+" feuille de "+curParent);
                   curParent.addChild(curLeaf);
                   if( curParent.col==null ) {
                       //System.out.println(curLeaf.col);
                       curParent.col = curLeaf.col;
                   }
              }
          }

      public void mouseExited(MouseEvent evt) {
           // delete FoVs
           aladin.calque.curFov = null;
           aladin.calque.cutoutFov = null;
           // reset localisation
           resetLocalisation();
           // on efface les noeuds "selectionnes" quand on sort de l'arbre
           clearSelected();

           aladin.calque.repaintAll();
       }

       // TEST pour différencier les types de noeuds avec une couleur --> fonctionne
       /*
      protected void drawCheckBox(int x, int y, BasicNode node, boolean efface) {
          ResourceNode n = (ResourceNode)node;
          int cote = YSPACE-6; // dimension d'un cote du carre
          int xBegin = x;
          int yBegin = y+2;
          if( efface ) {
              h.setColor(grayFill);
              if( n.type==ResourceNode.IMAGE ) h.setColor(Color.cyan);
              else if( n.type==ResourceNode.SPECTRUM ) h.setColor(Color.green);
              h.fillRect(xBegin,yBegin,cote,cote);
          }
          h.setColor(Color.white);
          h.drawRect(xBegin+1,yBegin+1,cote,cote);
          h.setColor(grayCB);
          h.drawRect(xBegin,yBegin,cote,cote);
          // si selectionnee, on dessine l'espece de "V"
          if( node.isSelected() ) {
              h.setColor(tickColor);
              h.fillRect(xBegin+3,yBegin+cote/2-1,2,cote/2);
              int x1 = xBegin+5;
              int y1 = yBegin+cote-3;
              int l = cote/2-1;
              h.drawLine(x1,y1,x1+l,y1-l);
              h.drawLine(x1,y1-1,x1+l,y1-l-1);
          }
      }
      */

   /** Shows the information of a given node in the info frame
       @param node The resource node
    */
   private void showInfo(ResourceNode node) {
       FrameInfo curInst = aladin.getFrameInfo();
       if( node.server!=null && node.server instanceof ServerAladin ) {
           String defFormat = ((ServerAladin)aladin.dialog.server[ServerDialog.ALADIN]).getDefaultFormat();
           if( node.isAvailableFormat(defFormat) ) node.curFormat = defFormat;
       }
       if( dontShow(node) ) return;
       curInst.update(node,this);
       curInst.toFront();
       curInst.show();
   }

   // pour éviter l'ouverture d'un noeud vide !
   private boolean dontShow(ResourceNode n) {
	if( n!=null && aladin.calque.curFov!=null ) return false;
   	if( n==null || n.description==null || n.explanation==null ||
   		(n.description.length==0 && n.links==null && (n.filterDesc==null || n.filterExpla==null )) ) {
   		return true;
   	}
   	return false;
   }


	protected void drawNodeName(Graphics h,BasicNode node, boolean redrawAll) {
		ResourceNode resNode = (ResourceNode)node;
		int type;
		boolean withSortField = (getName(node).indexOf("::: "))>=0 && !node.isLeaf;
		if( !useColorLabel() || resNode.nbChildren==0 || withSortField ||
			(type=((ResourceNode)resNode.getChildrenAt(0)).type)==ResourceNode.VOID) {
			super.drawNodeName(h,node, redrawAll);
		}
		else {
			h.setFont(nameFont);
			if( node==getRootNode() && flatView ) return;

			// dessin d'un rectangle par dessus le label
			// Pour eviter bug ds IE6 Win XP (fontes degueulasses)
			String name = getName(node);
			h.setColor(LABEL_COL2[type]);
			h.fillRect( node.x,node.y+1,h.getFontMetrics().stringWidth(name)+1,YSPACE-2);
			h.fillArc( node.x-8, node.y+1, 16,YSPACE-2+1,90,90);
			h.fillArc( node.x+h.getFontMetrics().stringWidth(name)+1-8, node.y+1, 16,YSPACE-2+1,0,90);
			h.setColor(LABEL_COL3[type]);
			h.fillRect( node.x,node.y+1+YSPACE/2,h.getFontMetrics().stringWidth(name)+1,YSPACE-2-YSPACE/2+1);
			h.fillArc( node.x-8, node.y+1, 16,YSPACE-2+1,180,90);
			h.fillArc( node.x+h.getFontMetrics().stringWidth(name)+1-8, node.y+1, 16,YSPACE-2+1,270,90);
			h.setColor(LABEL_COL1[type]);
			h.drawLine( node.x, node.y+1, node.x+h.getFontMetrics().stringWidth(name), node.y+1);
			h.setColor(LABEL_COL4[type]);
			h.drawLine( node.x, node.y+1+YSPACE-2, node.x+h.getFontMetrics().stringWidth(name), node.y+1+YSPACE-2);

			boolean mouseOver = false;
			if( oHilightNode>=0 && oHilightNode<nodeTab.length && nodeTab!=null ) {

				if( node.equals(nodeTab[oHilightNode]) ) {
					mouseOver = true;

					if(node.isLeaf) {
						drawSelectedLeafNodeName(h,node, name);
	                }
				}
			}
			h.setColor(mouseOver?getMouseOverColor(node):Color.black);
			h.setFont(node.equals(lastInfoNode)?boldNameFont:nameFont);
			h.drawString(name,node.x,node.y+YSPACE/2+4);

		}
	}


// toutes ces méthodes là, ainsi que load() devraient se trouver dans ResourceNode


	static boolean firstShow = true;
	/** Montre les fovs de node */
	void showFov(ResourceNode node) {
        // pour enlever l'affichage du Help
		if( firstShow ) {
			aladin.cardView.show(aladin.bigView,"View");
			firstShow = false;
		}
    	aladin.calque.curFov = getFovs(node, false);
        // on force la projection  si node est une feuille
        ResourceNode parent = (ResourceNode)node.getParent();
        // si on doit calculer une projection (sinon, on ne voit à chaque fois qu'un carré au milieu de l'écran, pas intéressant)
        if( node.isLeaf && parent!=null && parent.type==ResourceNode.IMAGE && node.getFov()!=null ) {
            aladin.calque.fovProj = PlanFov.getProjection(getFovs(parent, false));
        }
        else aladin.calque.fovProj = null;

        aladin.view.repaintAll();
	}

	/** Retournes les fovs d'un noeud
	 *
	 * @param node le noeud à partir duquel on va chercher tous les fovs
     * @param cutout true si on ne veut que les cutout
	 * @return Fov[] tableau des fovs trouvés
	 */
	protected Fov[] getFovs(ResourceNode node, boolean cutout, boolean cutoutIfNeeded) {
        Vector leavesV = new Vector();
        getAllLeaves(node, leavesV);
        if( node.isLeaf && (!cutout || (node.cutout && node.getFov()!=null) ) ) leavesV.addElement(node);
        Enumeration e = leavesV.elements();
        Fov[] fovs = new Fov[leavesV.size()];
		int i=0;
        while( e.hasMoreElements() ) {
        	if( cutout ) fovs[i] = getCutoutFov((ResourceNode)e.nextElement());
            else if( cutoutIfNeeded ) {
                if( node.cutout && node.getFov()!=null ) fovs[i] = getCutoutFov((ResourceNode)e.nextElement());
                else fovs[i] = ((ResourceNode)e.nextElement()).getFov();
            }
            else fovs[i] = ((ResourceNode)e.nextElement()).getFov();
        	i++;
        }
        return fovs;
	}

    protected Fov[] getFovs(ResourceNode node, boolean cutout) {
        return getFovs(node,cutout,false);
    }



   void showCutoutFov(ResourceNode node) {
       //if( node.fov == null) return;

       aladin.calque.cutoutFov = getFovs(node, true);

       aladin.view.repaintAll();
   }

   private Fov getCutoutFov(ResourceNode node) {
       Fov fov;
       try {
           fov = new Fov(resolveTarget(node.getCutoutTarget(), aladin), node.getFov().cutout_x, node.getFov().cutout_y, node.getFov().angle);
       }
       catch(Exception e) {return null;}
       return fov;
   }


   static String resolveTarget(String s, Aladin aladinInst) {
       Coord c;
       try {
           if( !View.notCoord(s) ) c = new Coord(s);
           else  c=aladinInst.view.sesame(s);
       } catch( Exception e ) { return null; }

       return c.getSexa(":");
   }

   void deactivateCutoutFov() {
       aladin.calque.cutoutFov = null;
       aladin.view.repaintAll();
   }

   void hideFov() {
       aladin.calque.curFov = null;
       aladin.view.repaintAll();
   }

   // tri par défaut : sur la résolution (colonne #4)
   private int defaultIndexSort = 4;
   // ordre de tri par défaut (descendant)
   private boolean defaultDescSort = true;
   private boolean sortable=false;
   void setSortable(boolean b) {
       sortable = b;
       if( sortable ) {
           indexSort = defaultIndexSort;
           descSort = defaultDescSort;
       }
   }

   /** Fixe l'aspect initial de la vue hiérarchique
    * (tous les noeuds ouverts, ou derniers noeuds fermés)
    * @param b si true, les noeuds sont tous ouverts initialement
    */
   void setFullExpandAtStart(boolean b) {
       fullExpandAtStart = b;
   }

   protected int initMaxWidth() {
       if( flatView && sortable ) {
           int max = 2*XSPACE;
           // calcul des tailles max des colonnes
           if( recomputeColSize ) {
               computeColSize();
               recomputeColSize = false;
           }
           for( int i=0; i<colSize.length; i++ ) max += (colSize[i]+1)*wChar;
           return max;
       }
       else return super.initMaxWidth();
   }

   static String[] sortName = {"SURVEY", "COLOR", "SIZE", "OBS ID", "RESOL", "DATE"};
   int[][] colBounds; // limites de chaque colonne en x
   void fullDisplay(Graphics h) {
       // ajout des "onglets" permettant de trier les ressources
       if( flatView && sortable && nodeFullTab.length>1 ) {
           colBounds = new int[colSize.length][2];
           int x = 2*XSPACE;
           int height = YSPACE-4;
           int width;
           h.setColor(Color.blue);

           for( int i=0; i<colSize.length; i++ ) {
               width = wChar*colSize[i]-1;
               colBounds[i][0] = x;
               colBounds[i][1] = x+width;
               x += width+wChar;
           }
           for( int i=0; i<colBounds.length; i++ ) drawOnglet(h,i);
       }
       super.fullDisplay(h);
   }

    static int wChar = fm.stringWidth("A");
    private void drawOnglet(Graphics h,int index) {
        int width = colBounds[index][1]-colBounds[index][0];
        int height = YSPACE-4;
        int xBegin = colBounds[index][0];
        h.setColor(grayFill);
        h.fillRect(xBegin+1,2+1,width,height);
        h.setColor(Color.white);
        h.drawRect(xBegin+1,2+1,width,height);
        h.setColor(grayCB);
        h.drawRect(xBegin,2,width,height);
        h.setColor(Color.black);
        h.setFont(nameFont);
        h.drawString(sortName[index],wChar+xBegin,height);
        // dessin des flèches pour indiquer l'ordre du tri
        if( index==indexSort ) {

            if( descSort ) {
                xBegin = colBounds[index][1]-wChar-3+wChar/2;
                h.setColor(Color.white);
                h.drawLine(xBegin,YSPACE-4,xBegin+wChar/2,5);

                xBegin = colBounds[index][1]-wChar-3;
                h.setColor(Color.black);
                h.drawLine(xBegin,5,xBegin+wChar/2,YSPACE-4);

                xBegin = colBounds[index][1]-wChar-3;
                h.setColor(Color.black);
                h.drawLine(xBegin,5,xBegin+wChar-1,5);
            }
            else {
                xBegin = colBounds[index][1]-wChar-3+wChar/2;
                h.setColor(Color.white);
                h.drawLine(xBegin,5,xBegin+wChar/2,YSPACE-4);

                xBegin = colBounds[index][1]-wChar-3;
                h.setColor(Color.black);
                h.drawLine(xBegin,YSPACE-4,xBegin+wChar/2,5);

                xBegin = colBounds[index][1]-wChar-3;
                h.setColor(Color.white);
                h.drawLine(xBegin,YSPACE-4,xBegin+wChar-1,YSPACE-4);
            }
        }
   }

   boolean recomputeColSize=true;
   void traverseTree() {
       recomputeColSize=true;
       super.traverseTree();
   }


   int[] colSize; // taille des col. en nb de caractères
   /*
    * Calcule la taille max des colonnes
    */
   private void computeColSize() {
       colSize = new int[columns.length];

       if( sortable ) {
           // initialisation des valeurs de colSize
           // (la colonne doit être au moins aussi grande que son label)
           for( int i=0; i<colSize.length; i++ ) {
               colSize[i] = sortName[i].length()+3;
           }
       }
       //System.out.println("valeur : "+colSize[0]);
       Vector v = new Vector();
       getAllLeaves(getRootNode(),v);
       ResourceNode[] res = new ResourceNode[v.size()];
       v.copyInto(res);
       v=null;
       ResourceNode curNode;
       String tmp;
       // loop on all leaves
       for( int i=0; i<res.length; i++ ) {
           curNode = res[i];
           for( int j=0; j<columns.length; j++) {
               colSize[j] = Math.max(colSize[j], columns[j].getColName(curNode).length());
           }
       }
   }



   static final private String DEFAULT_NAME = "Info frame";

   /** Retourne le nom du noeud tel qu'on le veut dans l'arbre
    *  @param node le noeud dont on veut connaitre le nom
    *  @param flat pour préciser si l'on se trouve en flatMode ou non
    */
   // TODO : à améliorer pour remonter aux niveaux n et n+1 pour noeuds autre que AladinServer
   // faire p-e un cas spécial pour AladinServer ou on prend color survey etc
   protected String getName(BasicNode node, boolean flat) {
      if( node.name==null ) node.name = "NULL";
      if( !node.isLeaf ) return node.name.length()>0?node.name:DEFAULT_NAME;
       ResourceNode n = (ResourceNode)node;
       if( !flat ) {
           if( n.isSIAPEvol && n.altName!=null && n.getParent().isObs ) return n.altName;
           // bidouille pour virer le -EPOCHx
           int indexEp;
           if( ( indexEp=n.name.indexOf("-EPOCH") ) > 0 ) {
               return n.name.substring(0,indexEp);
           }
           StringBuffer ret = new StringBuffer();
           if( n.machine!=null && n.machine.length()>0 ) ret.append(n.machine+".");
           ret.append( n.name.length()>0?n.name:DEFAULT_NAME );
           if( n.getFov()!=null ) ret.append(" "+n.getFov().getSizeStr(n.cutout));
           if( n.obsDate!=null ) ret.append(" "+n.obsDate);


           return ret.toString().trim();
       }

       // en flatView
       if( !n.isLeaf || n.survey==null ) {
           String newName = getName(n,false);
           ResourceNode parent= (ResourceNode)n.getParent();
           String parentName;
           int idx;
           while( parent!=null && parent.type==ResourceNode.IMAGE ) {
           	   parentName = parent.name;
           	   // nettoyage de parentName
           	   if( (idx=parentName.indexOf("::: "))>=0 ) {
           	      parentName = parentName.substring(idx+4);
           	   }
           	   parentName = MetaDataTree.replace(parentName, " ///", "", 1);

               newName += " "+parentName;
               parent = (ResourceNode)parent.getParent();
           }
           newName += (n.getPixSize()!=null?" "+n.getPixSize():"");
           newName += (n.obsDate!=null?" "+n.obsDate:"");
           return newName;
       }

       // calcul des tailles max des colonnes
       if( recomputeColSize ) {
           computeColSize();
           recomputeColSize = false;
       }

       String retName = "";
       for( int i=0; i<columns.length; i++ ) {
           retName += Util.fillWithBlank(columns[i].getColName(n),colSize[i]+1);
       }
       return retName.trim();
   }

   protected String getName(BasicNode node) {
       return getName(node,flatView);
   }

   // verifie qu on trouve bien un cutout a cet endroit donne
   protected boolean checkCutoutAvailability(BasicNode node, double alpha, double delta, Aladin al) {
       return true;
       // Ma méthode ne fonctionne pas bien pour le moment
       // A REVOIR

       /*
       Coord coo = new Coord(alpha, delta);

       Plan p = al.calque.getPlanRef();
       // ce cas m'emmerde un peu, je sais pas quoi faire
       // je renvoie true, normalement, et le serveur doit dire "No answer available"
       if( p==null || !Projection.isOk(p.projd) ) return true;
       coo = p.projd.getXY(coo);
       Point point = al.view.zoomview.getViewCoord(coo.x,coo.y);
       //System.out.println(point.x);
       //System.out.println(point.y);
       // A SUPPRIMER
       if( ((ResourceNode)node).fov==null ) return true;
       return ((ResourceNode)node).fov.contains(point.x,point.y,p,al.view.zoomview);
       */
   }

	private URL resolveLocation(String location, ResourceNode node, String imgParam) {
		if( node.type!=ResourceNode.IMAGE ) {
			try {
				return new URL(location);
			}
			catch(MalformedURLException mue) {
				// 2 cas particuliers, si la location pointe vers des fichiers locaux
				if( location!=null && ( location.startsWith("/") || location.toLowerCase().startsWith("c:/") ) ) {
					return resolveLocation("file://"+location, node, imgParam);
				}

				Aladin.warning("Can't interpret location "+location);
				return null;}
		}

		String gluLink = node.gluLink;
		boolean useGluLink = gluLink!=null;

	    Coord coo=null;
	    coo = getPos(node);
	       if( coo!=null ) {
	            if( node.cutout ) {
	                if( !checkCutoutAvailability(node,coo.al,coo.del,aladin) ) {
	                    Aladin.warning(IMAGE+" "+node.name+" "+NOIMAGE_WARNING,1);
	                    return null;
	                }
	            }
	            String pos = TreeView.getDeciCoord(coo.getSexa());

	            // pour les DSS, on accède à l'ancien serveur qui veut les position en sexa
	            // je teste sur le contenu de l'URL
	            if( location.indexOf("alapre")>=0 ) {
	               pos = coo.getSexa();
	            }
	            if( useGluLink ) gluLink = replace(gluLink,"$POS", URLEncoder.encode(pos), -1);
	            else location = replace(location,"$POS", URLEncoder.encode(pos), -1);
	       }


	       String curFormat = node.curFormat;
	       if( imgParam!=null && node.isAvailableFormat(imgParam) ) curFormat = imgParam;

	       if( curFormat!=null ) {
	           if( useGluLink ) gluLink = replace(gluLink,"$COMPRESSION",
	                                              URLEncoder.encode(curFormat), -1);
	           else location = replace(location,"$COMPRESSION", URLEncoder.encode(curFormat), -1);
	       }
	       if( node.curMode!=null ){
	           if( useGluLink ) gluLink = replace(gluLink,"$MODE", URLEncoder.encode(node.curMode), -1);
	           else location = replace(location,"$MODE", URLEncoder.encode(node.curMode), -1);
	       }
	       if( useGluLink ) gluLink = replace(gluLink,"$RESOLUTION","FULL",-1);
	       else location = replace(location,"$RESOLUTION","FULL",-1);

	       if( useGluLink ) gluLink = replace(gluLink,"$NumberOfPatches",node.curImgNumber,-1);
	       else location = replace(location,"$NumberOfPatches",node.curImgNumber,-1);

	       // $Xpix, $Ypix
	       String[] imagePos = node.getImagePosTarget();
	       String xPix = imagePos[0];
	       String yPix = imagePos[1];
	       if( useGluLink ) {
	          gluLink = replace(gluLink,"$Xpix",xPix,-1);
	          gluLink = replace(gluLink,"$Ypix",yPix,-1);
	       }
	       else {
	          location = replace(location,"$Xpix",xPix,-1);
	          location = replace(location,"$Ypix",yPix,-1);
	       }

	       URL url;
	       // résolution marque GLU
	       if( useGluLink ) {
	           // pour analyse du tag glu
	           Words w = new Words("");
	           w.tagGlu(gluLink.toCharArray());
	           //System.out.println(w.id);
	           //System.out.println(w.param);
	           url = aladin.glu.getURL(w.id,w.param,true);
	       }
	       else {
	           url = aladin.glu.getURL("Http",location,true);
	       }

	       return url;
	}

   //

   /** charge l'image decrite par le noeud
    * @param node noeud derivant l'image
    * @param imgParam parametre supplementaire, le format en l'occurence. Si null, on prend le format courant
    * @param label le label ou null si à définir
    */
   private void loadImage(ResourceNode node, String imgParam, String label) {

       String location = node.getLocation();
       String gluLink = node.gluLink;

       if( location==null && gluLink==null ) {
           Aladin.warning(NOLOC);
           return;
       }
       if( location==null ) location="";
       if( location.equalsIgnoreCase("NoData") && gluLink==null ) {
           Aladin.warning(NOIMG);
           return;
       }

       // ouverture d'une page HTML dans browser
       if( node.indexing!=null && node.indexing.equals("HTML") ) {
           aladin.glu.showDocument("Http",location,true);
           return;
       }

       boolean useGluLink = gluLink!=null;

       // les URL du type file:/... ne sont pas supportés par creatPlane, je bidouille
       if( location.startsWith("file:") ) {
           location = location.substring(5);
           ((ServerFile)aladin.dialog.server[ServerDialog.LOCAL]).creatLocalPlane(location,node.name,"",null,node,null,null,null,null);
           return;
       }

       Coord coo=null;
       coo = getPos(node);
       if( coo!=null ) {
            if( node.cutout ) {
                if( !checkCutoutAvailability(node,coo.al,coo.del,aladin) ) {
                    Aladin.warning(IMAGE+" "+node.name+" "+NOIMAGE_WARNING,1);
                    return;
                }
            }
            String pos = TreeView.getDeciCoord(coo.getSexa());

            // pour les DSS, on accède à l'ancien serveur qui veut les position en sexa
            // je teste sur le contenu de l'URL
            if( location.indexOf("alapre")>=0 ) {
               pos = coo.getSexa();
            }
            if( useGluLink ) gluLink = replace(gluLink,"$POS", URLEncoder.encode(pos), -1);
            else location = replace(location,"$POS", URLEncoder.encode(pos), -1);
       }


       String curFormat = node.curFormat;
       if( imgParam!=null && node.isAvailableFormat(imgParam) ) curFormat = imgParam;

       if( curFormat!=null ) {
           if( useGluLink ) gluLink = replace(gluLink,"$COMPRESSION",
                                              URLEncoder.encode(curFormat), -1);
           else location = replace(location,"$COMPRESSION", URLEncoder.encode(curFormat), -1);
       }
       if( node.curMode!=null ){
           if( useGluLink ) gluLink = replace(gluLink,"$MODE", URLEncoder.encode(node.curMode), -1);
           else location = replace(location,"$MODE", URLEncoder.encode(node.curMode), -1);
       }
       if( useGluLink ) gluLink = replace(gluLink,"$RESOLUTION","FULL",-1);
       else location = replace(location,"$RESOLUTION","FULL",-1);

       if( useGluLink ) gluLink = replace(gluLink,"$NumberOfPatches",node.curImgNumber,-1);
       else location = replace(location,"$NumberOfPatches",node.curImgNumber,-1);

       // $Xpix, $Ypix
       String[] imagePos = node.getImagePosTarget();
       String xPix = imagePos[0];
       String yPix = imagePos[1];
       if( useGluLink ) {
          gluLink = replace(gluLink,"$Xpix",xPix,-1);
          gluLink = replace(gluLink,"$Ypix",yPix,-1);
       }
       else {
          location = replace(location,"$Xpix",xPix,-1);
          location = replace(location,"$Ypix",yPix,-1);
       }

       URL url;
       // résolution marque GLU
       if( useGluLink ) {
           // pour analyse du tag glu
           Words w = new Words("");
           w.tagGlu(gluLink.toCharArray());
           //System.out.println(w.id);
           //System.out.println(w.param);
           url = aladin.glu.getURL(w.id,w.param,true);
       }
       else {
           url = aladin.glu.getURL("Http",location,true);
       }

       if( label==null ) label = buildStackLabel(node);
       String param = buildQual(node);
       int format = curFormat!=null?PlanImage.getFmt(curFormat):PlanImage.UNKNOWN;

       String objet = coo!=null?coo.getSexa(":"):"";
       if( objet.length()==0 || (node.objet!=null && ( (!node.cutout) && ( (node.modes==null || node.modes.length<=1 || node.curMode==null || node.curMode.equals("ORIGIN") ) ) || (node.targetObjet!=null &&  objet.equals(node.targetObjet) ) ) ) )
           objet = node.objet;

       //System.out.println(objet);
       // Verification de non-redondance
       if( node.server!=null && node.server instanceof ServerAladin &&
             !aladin.dialog.server[aladin.dialog.current].verif(Plan.IMAGE,objet,param,
             format+"/"+PlanImage.UNDEF ) ) {
                 return;
       }

       // écriture de la commande script équivalente
       String cmd = node.getScriptCommand();
       if( cmd!=null ) {
    	   aladin.console.printCommand(cmd);
       }


       // Chargement des données de manière plus générique afin de pouvoir
       // loader n'importe quel type de données et notamment des Fits avec extensions
       // pour Tom Donalson (PF - mai 2009)

       // Cas server Aladin particulier car JPEG en N&B non pris en compte dans la procédure générique
       if( node.server instanceof ServerAladin ) {
          aladin.calque.newPlanImage(url,PlanImage.ALADIN,label,
                objet,param,node.origin!=null&&node.origin.length()>0?node.origin:(node.server!=null?node.server.institute:null),
                      format,
                      PlanImage.UNDEF,
                      null,node);

       // Cas général
       } else {
          ((ServerFile)aladin.dialog.localServer).creatLocalPlane(url.toString(), label,
                node.origin!=null&&node.origin.length()>0?node.origin:(node.server!=null?node.server.institute:null),
                      null, node, (InputStream)null,node.server!=null?node.server:null,null,null );
       }

//       // chargement de l'image
//       if( node.isColorImage() ) {
//           aladin.calque.newPlanImageColor(url,null,PlanImage.ALADIN,label,
//                objet,param,node.origin!=null&&node.origin.length()>0?node.origin:(node.server!=null?node.server.from:null),
//                format,
//                PlanImage.UNDEF,
//                null,node);
//       }
//       else if( node.type==ResourceNode.CUBE) {
//           aladin.calque.newPlanImageCube(url,null,PlanImage.ALADIN,label,
//                   objet,param,node.origin!=null&&node.origin.length()>0?node.origin:(node.server!=null?node.server.from:null),
//                   format,
//                   PlanImage.UNDEF,
//                   null,node);
//       }
//       else {
//           aladin.calque.newPlanImage(url,PlanImage.ALADIN,label,
//               objet,param,node.origin!=null&&node.origin.length()>0?node.origin:(node.server!=null?node.server.from:null),
//               format,
//               PlanImage.UNDEF,
//               null,node);
//       }


   }

   /**
    * Builds the label which appears in the stack, upon basis of the properties of the node
    * @param node node to label
    * @return String the label of the node
    */
   private String buildStackLabel(ResourceNode node) {
       StringBuffer ret = new StringBuffer();
       // on ajoute en préfixe : la résolution ("Pl", "Lw")
       if( node.resol!=null && node.survey!=null ) {
           if( node.resol.equals("PLATE") ) ret.append("Pl-");
           else if( node.resol.equals("LOW") ) ret.append("Lw-");
       }
       if( node.survey != null ) ret.append(node.survey);
       if( node.epoch != null ) ret.append("."+node.epoch);
       if( node.bandPass != null ) ret.append("."+node.bandPass);
       if( node.machine != null ) ret.append("."+node.machine);
       ret.append("."+node.name);

       // demo AVO
       if( !node.curImgNumber.equals("1")||!node.maxImgNumber.equals("1")) {
           ret.append(node.curImgNumber);
           if( node.velStep!=0.0 ) {
               String velocity;
               try {
                   double val = Slider.round(node.beginVel+(Integer.parseInt(node.curImgNumber)-1)*node.velStep, 2);
                   velocity = "_" + val + "km/s";
               }
               catch(NumberFormatException nfe) {velocity = "";}
               ret.append(velocity);
           }
       }

       String stackLabel = ret.toString().trim();
       // demo AVO
       //if( retStr.startsWith("GOODS-")) retStr = retStr.substring(6);

       // fix the name :
       // - no starting dot
       while( stackLabel.length()>0 && stackLabel.charAt(0)=='.' )
           stackLabel = stackLabel.substring(1);
       // - no consecutive dots
       stackLabel = replace(stackLabel, "..", ".", -1);
       // - no final dot
       while( stackLabel.length()>0 && stackLabel.charAt(stackLabel.length()-1)=='.' )
           stackLabel = stackLabel.substring(0, stackLabel.length()-2);

       return stackLabel;
   }

    /**
     * Builds the label which appears in the stack, upon basis of the properties of the node
     * @param node node to label
     * @return String the label of the node
     */
    private String buildQual(ResourceNode node) {
        StringBuffer ret = new StringBuffer();
        if( node.survey!=null ) ret.append(node.survey+" ");
        if( node.bandPass!=null ) ret.append(node.bandPass+" ");
        if( node.curMode!=null ) ret.append(node.curMode+" ");
        ret.append(node.name+" ");

        return ret.toString();
    }

    private Coord getPos(ResourceNode node) {


        if( node.cutout ) {
            Coord coo=null;
            try {
                coo = new Coord(node.getCutoutTarget());
            }
            catch( Exception e ) {
                // si erreur lors de l'interprétation en coordonnées, tentative de résolution via Sesame
                try { coo=aladin.view.sesame(node.getCutoutTarget()); }
                catch( Exception e1 ) {}
            }
            return coo;
        }

        if( node.modes!=null && node.modes.length>1 && !(node.curMode.equals("ORIGIN")) ) {
            Coord coo=null;
            try {
                coo = new Coord(node.getMosaicTarget());
            }
            catch( Exception e ) {
                // si erreur lors de l'interprétation en coordonnées, tentative de résolution via Sesame
               try { coo=aladin.view.sesame(node.getMosaicTarget()); }
               catch( Exception e1 ) {}
            }
            return coo;
        }

        if( node.getFov()==null ) return null;

        return new Coord(node.getFov().alpha, node.getFov().delta);
    }

	/** Loads all selected nodes */
	void loadSelected(String param) {
		Vector v = getSelectedLeaves();
   		Enumeration e = v.elements();

   		while(e.hasMoreElements()) {
       		ResourceNode node = (ResourceNode)e.nextElement();
			load(node, param, null, this);
   		}
	}

    void loadSelected() {
        loadSelected(null);
    }

	/** Get the number of resources that are checked and visible */
	int nbSelected() {
	    return getSelectedLeaves().size();
	}

   /** Clears the tree */
   void clear() {
       ResourceNode node = new ResourceNode(aladin);
       node.hide = true;
       setRoot(node);
       if( sortable ) {
           indexSort = defaultIndexSort;
           descSort = defaultDescSort;
       }
   }

//   // Gestion d'un verrou pour la synchronisation script. (PF - mars 2010)
//   private boolean sync=true;
//   synchronized protected boolean isSync() { return sync; }
//   synchronized protected void setSync(boolean sync) { this.sync=sync; }

	// devrait être ramené au niveau du noeud !
    /** charge la ressource décrite par node dans un thread séparé
     * @param node noeud dont on veut charger la ressource qu'il decrit
     * @param param parametre supplementaire (eg format pour les images)
     * @param label le nom du plan à créer, ou null si non fourni (PF mars 2010)
     */
   protected void load(final ResourceNode node, final String param, final String label, final Component c) {
      final String treeTaskId = aladin.synchroServer.start("MetaDataTree.load");
      //	    setSync(false);
      new Thread("LoadResourceNode") {
         public void run() {
            try {
               if( /*!node.isLeaf &&*/ (node.location==null || node.location.length()==0) ) {
//	              setSync(true); 
                  return;
               }


               if( node.type == ResourceNode.IMAGE || node.type == ResourceNode.CUBE ) {
                  loadImage(node, param, label);
               }
               else if( node.type == ResourceNode.CAT ) {
                  loadCat(node,label);
               }
               else if( node.type == ResourceNode.SPECTRUM ) {
                  loadSpectrum(node, c);
               }
               // on tente de le charger par LocalServer.creatLocalPlane
               else if( node.type == ResourceNode.OTHER ) {
                  loadOther(node,label);
               }
               // tentative d'ouverture d'une page HTML dans browser
               else if( node.indexing!=null && node.indexing.equals("HTML") ) {
                  aladin.glu.showDocument("Http",node.location,true);
               }
//	           setSync(true);
            } finally { aladin.synchroServer.stop(treeTaskId); }
         }
      }.start();
      Util.pause(100);
   }

    protected void load(ResourceNode node, String label) {
        load(node, null, label, null);
    }

    private void loadOther(BasicNode node,String label) {
       if( label==null ) label=node.name;
       aladin.calque.newPlan(((ResourceNode)node).location,label,null,null,null);
    }

	/** chargement d'un spectre dans une appli PLASTIC
	 *
	 * Si non connecté à PLASTIC, ou si pas d'appli trouvé, on affiche un message
	 *
	 * @param node
	 * @param c
	 */
	private void loadSpectrum(ResourceNode node, Component c) {
//		if( node.location!=null && node.location.indexOf("Xpix")>=0 ) {
//			loadSpectrumInSpecview(node, c);
//			return;
//		}


		boolean cantDisplay = false;
		if( !aladin.getMessagingMgr().isRegistered() ) cantDisplay = true;
		ArrayList<String> plasticApps = aladin.getMessagingMgr().getAppsSupporting(node.getPlasticMsg());
		if( plasticApps==null || plasticApps.size()==0 ) cantDisplay = true;

		if( cantDisplay ) {
			System.out.println("Can't load spectrum "+node.name+" : couldn't find any PLASTIC-compatible spectrum viewer");
		}
		// on charge le spectre avec la première appli compatible PLASTIC qui a été trouvée
		else {
			loadNodeWithPlastic(node, plasticApps.get(0));
		}

	}

//   // charge dans le plugin VOSpec le spectre décrit par le noeud
//   private void loadSpectrumInSpecview(ResourceNode node, Component c) {
//   		lock();
//
//		nodesT = new ResourceNode[] {node};
//		cT = c;
//
//		// lancement du thread
//		thread = new Thread(this);
//		thread.setPriority(Thread.NORM_PRIORITY - 1);
//		thread.start();
//   }

   // charge le catalogue decrit par le noeud
   private void loadCat(BasicNode node,String label) {
   	   loadOther(node,label);

   	   // partie abandonnée : on estime désormais que un noeud CAT décrit une zone prédéterminée
   	   /*
       String target = ((ResourceNode)node).getCutoutTarget();
       if( target.length()==0 ) {
//         certainement à changer car l'arbre donne une vue pour une région précise
           Aladin.warning("Please enter a target to load the catalogue",1);
           return;
       }
	   aladin.treeView.creatCatPlane(node.name, target, node.name);
	   */
   }



	/** Save the content of the tree into a file (IDHA/XML format)
	 *
	 * @param file file object where we write
	 */
    /*
	protected void saveAsIDHA(File file) {
		// first, we create and fill the SavotVOTable object corresponding to the tree
		SavotVOTable savotVOT = new SavotVOTable();
		ResourceSet resSet = new ResourceSet();
		savotVOT.setResources(resSet);

		// loop on first level nodes
		Enumeration e = rootNode.getChildren();
		while( e.hasMoreElements() ) {
			ResourceNode node = (ResourceNode)e.nextElement();
			SavotResource res = new SavotResource();
			res.setId(node.name);
			resSet.addItem(res);
			processResource(res, node);
		}

		// then we save the SavotVOTable object as XML in file
		cds.savot.samples.WriteDocument.generateDocument(savotVOT);
	}
    */

	/** Recursive method used by saveAsIDHA to build the SavotVOTable object
	 *
	 * @param res resource to process
	 * @param node corresponding node
	 */
    /*
	private void processResource(SavotResource res, ResourceNode node) {
		ResourceSet resSet = new ResourceSet();
		res.setResources(resSet);
		Enumeration e = node.getChildren();

		while( e.hasMoreElements() ) {
			ResourceNode n = (ResourceNode)e.nextElement();
			SavotResource r = new SavotResource();
			r.setId(n.name);
			resSet.addItem(r);
			processResource(r, n);
		}
	}
    */

   // pour démo Victoria 2006
   Object spv; // objet Specview

	/** charge le spectre décrit par le noeud dans le plugin VOSpec
	 *
	 */
//	private void loadInVOSpecThread(ResourceNode[] nodes, Component c) {

//		 ******** NE PAS COMMITER CECI !! *********
   		// ******** démo Victoria et Workshop ESAC : chargement dans Specview ********
		// je garde le code sous le coude
//		if( nodes[0].location.indexOf("Xpix")>=0 && nodes[0].axes==null ) {
//
//			ResourceNode node = nodes[0];
//			String location = node.location;
//
//			if( location==null ) {
//				Aladin.warning(NOSPEC);
//				return;
//			}
//			if( location.equalsIgnoreCase("NoData") ) {
//				Aladin.warning(NOSPEC1);
//				return;
//			}
//
//
//		    String[] imagePos = node.getImagePosTarget();
//		    String xPix = imagePos[0];
//		    String yPix = imagePos[1];
//		    location = replace(location,"$Xpix",xPix,-1);
//            location = replace(location,"$Ypix",yPix,-1);
//
//
//			URL url;
//			try {
//				url = new URL(location);
//			}
//			catch( MalformedURLException e ) {
//				Aladin.warning(BADURL);
//				return;
//			}
//
//			try {
//
//				// je dois écrire le contenu de l'URL dans un fichier ".txt", sinon Specview rale !!
//				File f = PlasticManager.createTempFile("specview", ".txt");
//				FileOutputStream out = new FileOutputStream(f);
//
//				try {
//					InputStream in = url.openStream();
//					int d;
//					while((d = in.read()) != -1){
//					    out.write(d);
//					}
//				}
//				catch(Exception e) {
//					out.close();
//					Aladin.warning("Error while loading a spectrum in Specview", 1);
//					e.printStackTrace();
//					return;
//				}
//
//				out.close();
//
//	   			Class specviewClass = Class.forName("spv.Specview");
//	   			// lazy instantiation
//	   			if( spv==null ){
//	   				Constructor ct = specviewClass.getConstructor(new Class[] {});
//	   				spv = ct.newInstance(new Object[] {});
//	   			}
//	   			Method m = specviewClass.getMethod("overplotWithSelection", new Class[]{Class.forName("java.net.URL")});
//	   			m.invoke(spv, new Object[] {f.toURL()});
//
//	   			// toFront et show
//	   			Object oTmp;
//	   			Class cTmp;
//	   			Method mTmp;
//	   			cTmp = Class.forName("spv.controller.Controller");
//	   			mTmp = specviewClass.getMethod("getController", null);
//	   			oTmp = mTmp.invoke(spv, null);
//
//	   			mTmp = cTmp.getMethod("getComponent", null);
//	   			oTmp = mTmp.invoke(oTmp, null);
//
//	   			((Frame)oTmp).show();
//	   			((Frame)oTmp).toFront();
//			}
//			catch(Exception e) {
//				Aladin.warning("Error while loading a spectrum in Specview", 1);
//				e.printStackTrace();
//				}
//			return;
//	   		}
	   		// ******************************************************
	   		// ******************************************************



//		try {
//			aladin.createVOSpec();
//
//
//
//			Class specSetCls = Class.forName("esavo.vospec.spectrum.SpectrumSet");
//			Constructor ct = specSetCls.getConstructor(null);
//			Object spectrumSet = ct.newInstance(null);
//
//			Class stringCls = Class.forName("java.lang.String");
//
//			ResourceNode node = null;
//			int k=0;
//			for( int i=0; i<nodes.length; i++) {
//				node = nodes[i];
//				String location = node.location;
//
//				if( location==null ) {
//					Aladin.warning(NOSPEC);
//					continue;
//				}
//				if( location.equalsIgnoreCase("NoData") ) {
//					Aladin.warning(NOSPEC1);
//					continue;
//				}
//
//
//			    String[] imagePos = node.getImagePosTarget();
//			    String xPix = imagePos[0];
//			    String yPix = imagePos[1];
// 		        location = replace(location,"$Xpix",xPix,-1);
//                location = replace(location,"$Ypix",yPix,-1);
//
//
//				URL url;
//				try {
//					url = new URL(location);
//				}
//				catch( MalformedURLException e ) {
//					Aladin.warning(BADURL);
//					continue;
//				}
//
//
//
//				Aladin.trace(1,"Loading spectrum in VOSpec from URL "+url);
//
//				Class specClass = Class.forName("esavo.vospec.spectrum.Spectrum");
//				ct = specClass.getConstructor(null);
//				Object spectrum = ct.newInstance(null);
//
//				// action réalisée : spectrum.setUrl(location)
//				Method m = specClass.getMethod("setUrl", new Class[] {stringCls});
//				m.invoke(spectrum, new Object[] {location});
////				System.out.println("setUrl("+location+")");
//
//				// action réalisée : spectrum.setTitle(node.name);
//				m = specClass.getMethod("setTitle", new Class[] {stringCls});
//				m.invoke(spectrum, new Object[] {node.name});
////				System.out.println("setTitle("+node.name+")");
//
//				if( node.axes!=null && node.dimeq!=null && node.scaleq!=null ) {
//
//					// action réalisée : spectrum.setWaveLengthColumnName(node.axes[0]);
//					m = specClass.getMethod("setWaveLengthColumnName", new Class[] {stringCls});
//					m.invoke(spectrum, new Object[] {node.axes[0]});
////					System.out.println("setWaveLengthColumnName("+node.axes[0]+")");
//
//					// action réalisée : spectrum.setFluxColumnName(node.axes[1])
//					m = specClass.getMethod("setFluxColumnName", new Class[] {stringCls});
//					m.invoke(spectrum, new Object[] {node.axes[1]});
////					System.out.println("setFluxColumnName("+node.axes[1]+")");
//
//					// action réalisée : spectrum.setUnits(new Unit(node.dimeq[0],node.scaleq[0],node.dimeq[1],node.scaleq[1]));
//					Class unitCls = Class.forName("esavo.vospec.spectrum.Unit");
//					m = specClass.getMethod("setUnits", new Class[] {unitCls});
//					ct = unitCls.getConstructor(new Class[] {stringCls, stringCls, stringCls, stringCls});
//					m.invoke(spectrum, new Object[] {ct.newInstance(new Object[] {node.dimeq[0],node.scaleq[0],node.dimeq[1],node.scaleq[1]})});
////					System.out.println("setUnit(new Unit("+node.dimeq[0]+", "+node.scaleq[0]+", "+node.dimeq[1]+", "+node.scaleq[1]+"))");
//
//				}
//
//
//				String ra = node.ra>=0?node.explanation[node.ra]:"";
//				String de = node.de>=0?node.explanation[node.de]:"";
//				// action réalisée : spectrum.setRa(ra)
//				m = specClass.getMethod("setRa", new Class[] {stringCls});
//				m.invoke(spectrum, new Object[] {ra});
////				System.out.println("setRa("+ra+")");
//
//				// action réalisée : spectrum.setDec(de)
//				m = specClass.getMethod("setDec", new Class[] {stringCls});
//				m.invoke(spectrum, new Object[] {de});
////				System.out.println("setDec("+de+")");
//
//				if( node.format!=null ) {
//					// action réalisée : spectrum.setFormat(node.format)
//					m = specClass.getMethod("setFormat", new Class[] {stringCls});
//					m.invoke(spectrum, new Object[] {node.format});
//					// System.out.println("setFormat("+node.format+")");
//				}
//
////				System.out.println();
//
//				// action réalisée : spectrumSet.addSpectrum(k++, spectrum);
//				m = specSetCls.getMethod("addSpectrum", new Class[] {Integer.TYPE, specClass});
//				m.invoke(spectrumSet, new Object[] {new Integer(k++), spectrum});
//
//			}
//
//			// according to Jesus Salgado, toFront and show methods should be called before loadSpectrumSet
//			// this should prevent from freezing totally VOSpec, as we experienced earlier
//			aladin.showVOSpec();
//
//			// action réalisée : aladin.vospec.loadSpectrumSet("",spectrumSet);
//			Method m = Class.forName("esavo.vospec.standalone.VoSpec").getMethod("loadSpectrumSet", new Class[] {stringCls, specSetCls});
//			m.invoke(aladin.vospec, new Object[] {"", spectrumSet});
//
//		}
//		catch( Exception e ) {
//			Aladin.warning(ERRSPEC, 1);
//			e.printStackTrace();
//		}
//	}

//   public void run() {
//	// Copie des variables critiques
//	ResourceNode[] nodes = nodesT;
//	Component c = cT;
//	unlock();				// liberation du verrou
//
//	// chargement dans VOSpec
//	loadInVOSpecThread(nodes, c);
//   }
//
//   // variables pour thread
//	ResourceNode[] nodesT;
//	Component cT;
//
//      // thread pour gérer le chargement des spectres en asynchrone
//      private Thread thread;
//
//      boolean lock=false;
//	  /** Demande le lock */
//		void lock() {
//		   try { while( lock ) { thread.sleep(100); } }
//		   catch( Exception e) {}
//		   setlock(true);
//		}
//
//	  /** Libere le lock */
//		void unlock() { setlock(false); }
//		synchronized void setlock(boolean lock) { this.lock=lock; };

    /**
     * Swaps x[a] with x[b].
     */
    private static void swap(Object[] x, int a, int b) {
        Object tmp = x[a];
        x[a] = x[b];
        x[b] = tmp;
    }

    /**
     * Sort the array of nodes
     * @param nodes array to be sorted
     * @param c the sort we use
     * @param descSort true if the sort must be descendent
     */
    public static void sort(Object[] nodes, SortableColumn c, boolean descSort) {
        Object aux[] = nodes.clone();
        if( c==null ) c = new SortableColumn() {
            public String getColName(ResourceNode n) {
                return "";
            }

            public int compare(Object o1, Object o2, boolean goOn) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        };

        mergeSort(aux, nodes, 0, nodes.length, c, descSort);
    }

    /** sort according to the value of the field fieldName */
    public static void genericSort(Object[] nodes, final String sortField, boolean descSort) {
        Object aux[] = nodes.clone();
        SortableColumn c = new SortableColumn() {
        	public String getColName(ResourceNode node) {
        		String val = (String)node.properties.get(sortField);
        		return val==null?"":val;
        	}

        	public int compare(Object o1, Object o2, boolean goOn) {
        		ResourceNode n1 = (ResourceNode)o1;
    			ResourceNode n2 = (ResourceNode)o2;

    			String val1 = getColName(n1).toLowerCase();
    			String val2 = getColName(n2).toLowerCase();
    			Double d1, d2;
    			// on essaye d'en faire des double
    			try {
    				d1 = new Double(val1);
    				d2 = new Double(val2);
    				return d1.compareTo(d2);
    			}
    			catch( NumberFormatException nfe) {
    				return val1.compareTo(val2);
    			}
//    			return getColName(n1).toLowerCase().compareTo(getColName(n2).toLowerCase());
        	}
        };

        mergeSort(aux, nodes, 0, nodes.length, c, descSort);
    }


    /** Fonction copiée de Arrays.mergeSort */
    private static void mergeSort(Object src[], Object dest[],
                                  int low, int high, SortableColumn c, boolean desc) {
    int length = high - low;
    int fact = desc?1:-1;

    // Insertion sort on smallest arrays
    if (length < 7) {
        for (int i=low; i<high; i++)
            for (int j=i; j>low && fact*c.compare(dest[j-1], dest[j], true)>0; j--)
                swap(dest, j, j-1);

        return;
    }

        // Recursively sort halves of dest into src
        int mid = (low + high) >> 1;
        mergeSort(dest, src, low, mid, c, desc);
        mergeSort(dest, src, mid, high, c, desc);

        // If list is already sorted, just copy from src to dest.  This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if (fact*c.compare(src[mid-1], src[mid], true) <= 0) {
           System.arraycopy(src, low, dest, low, length);
           return;
        }

        // Merge sorted halves (now in src) into dest
        for(int i = low, p = low, q = mid; i < high; i++) {
            if (q>=high || p<mid && fact*c.compare(src[p], src[q], true) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }

    /**
     *
     * @param text text to search and replace in
     * @param repl String to search for
     * @param with String to replace with
     * @param max Maximum number of values to replace. If -1, replace all occurences of repl
     * @return String the string with replacements processed
     */
    public static String replace(String text, String repl, String with, int max) {
        if (text == null || repl == null || with == null || repl.length() == 0 || max == 0)
            return text;

        // Petites accélérations
        if( text.indexOf(repl)<0 ) return text;
        int n = repl.length();

        StringBuffer buf = new StringBuffer(text.length());
        int start = 0, end = 0;
        while ((end = text.indexOf(repl, start)) != -1) {
            buf.append(text.substring(start, end)).append(with);
            start = end + n;

            if (--max == 0)
                break;
        }
        buf.append(text.substring(start));
        return buf.toString();
    }

    void setStateChangedListener(MyListener listener) {
        stateChangerLstr = listener;
    }

    public BasicNode searchNodeByName(String name) {
        for(int i=0;i<nodeFullTab.length;i++) {
            if(nodeFullTab[i].name.equals(name)) return nodeFullTab[i];
        }

        return null;
    }

    /** searchNodeByName utilisé uniquement par getWidgetLocation */
    private ResourceNode searchNodeByName(String name, String band) {
        boolean nameOK;
        boolean bandOK;
        ResourceNode n;
        for(int i=0;i<nodeFullTab.length;i++) {
            n = (ResourceNode)nodeFullTab[i];
            nameOK = n.isLeaf && ( n.machine.indexOf(name)>=0 || n.survey.indexOf(name)>=0 );
            bandOK = band==null || (n.bandPass!=null && n.bandPass.equalsIgnoreCase(band));
            if( nameOK && bandOK ) {
                // move the scrollbar so that the node is visible
//                Point curPos = scroll.getScrollPosition();
//                Dimension vPort = scroll.getViewportSize();
//                if( curPos.y>n.y || (curPos.y+vPort.height-10)<n.y ) {
//                    scroll.setScrollPosition(0, Math.max(0, n.y-vPort.height/2));
//                    scroll.repaint();
//                }
                /*
                System.out.println("scrollpos : "+curPos);
                System.out.println("viewport : "+vPort);
                System.out.println(n.y);
                */
                return n;
            }
            //if( joker && getName(nodeFullTab[i]).indexOf(name)>=0 && nodeFullTab[i].isLeaf ) return nodeFullTab[i];
        }

        return null;
    }

    // implémentation de WidgetFinder

    public boolean findWidget(String name) {
        return true;
    }

   /** ici, le parametre name sera un peu special : debut du nom + "/" + nom bande */
    public Point getWidgetLocation(String name) {
        StringTokenizer st = new StringTokenizer(name, "/ ");
        name = st.nextToken();
        String band = null;
        if( st.hasMoreTokens() ) band = st.nextToken();

        BasicNode bn =  searchNodeByName(name, band);
        if( bn!=null ) return new Point(bn.x-LOGOSIZE*3/2, bn.y+LOGOSIZE/2);
        return null;
    }


	/**
	 * @return Returns the colorLabel.
	 */
	public boolean useColorLabel() {
		return colorLabel;
	}
	/**
	 * @param colorLabel The colorLabel to set.
	 */
	public void setColorLabel(boolean colorLabel) {
		this.colorLabel = colorLabel;
	}

    /** Interface allowing us to have sortable columns in the flatview of the tree */
    public interface SortableColumn {
        /**
         * Returns the label of the column for a given leaf of the tree
         * @param node a leaf node
         * @return String
         */
        String getColName(ResourceNode node);

       /** This method compares 2 nodes upon the basis of their columns
        * Returns a negative, zero or a positive integer if n1 is less, equals,
        * or greater than n2
        * @param n1 first node to be compared
        * @param n2 second node to be compared
        * @param goOn if false, we only do a primary comparison
        * @return int
        */
       int compare(Object n1, Object n2, boolean goOn);
    }


    // pour SIAP evolution
    class SortFrame extends JFrame {
        java.awt.List leftList, rightList;
        Button arrow;
        Button go;

        public SortFrame(String s) {
            super(s);
            Aladin.setIcon(this);

            setBackground(Aladin.BLUE);
            setLayout(new BorderLayout());
            leftList = new java.awt.List(15, true);
            rightList =  new java.awt.List(15, true);
            arrow = new Button("-->");
            go = new Button("OK");

            JPanel p = new JPanel();
            p.setLayout(new FlowLayout());

            p.add(leftList);
            p.add(arrow);
            p.add(rightList);

            Aladin.makeAdd(this, p, "North");

            Aladin.makeAdd(this, go, "South");
        }

        public void updateList() {
            leftList.removeAll();
            rightList.removeAll();

            ResourceNode n = null;
            Vector v = new Vector();
            getAllLeaves(getRootNode(), v);
            n = (ResourceNode) v.elementAt(0);


            for( int i=0; i<n.description.length; i++ ) {
                leftList.add(n.description[i]);
            }
        }

        public boolean action(Event e, Object o) {
            if( e.target.equals(arrow) ) {
                String[] selected = leftList.getSelectedItems();
                for( int i=0; i<selected.length; i++ ) {
                    rightList.add(selected[i]);
                    leftList.remove(selected[i]);
                }
            }

            else if( e.target.equals(go) ) {
                doSortSiapEvol(rightList.getItems());
                hide();
            }

            return true;
        }

        public boolean handleEvent(Event e) {

           if( e.id==Event.WINDOW_DESTROY ) {hide();}
           return super.handleEvent(e);
        }
    } // end of inner class SortFrame

    // implementation de l'interface KeyListener (pour raccourcis clavier)
    public void keyPressed(KeyEvent keyEvent) {
        char key = keyEvent.getKeyChar();
        Event e = null;

        // 'c' to collapse the whole tree
        if( key=='c' ) {
            super.action(e, COLLAPSE_ALL);
        }
        // 'e' to expand the whole tree
        else if( key=='e' ) {
            super.action(e, EXPAND_ALL);
        }
        // 't' to switch to the tree view
        if( key=='t' ) {
            super.action(e, HIER_VIEW);
            if( stateChangerLstr!=null ) stateChangerLstr.fireStateChange(HIER_VIEW);
        }
        // 'l' to switch to the list view
        else if( key=='l' ) {
            super.action(e, FLAT_VIEW);
            if( stateChangerLstr!=null ) stateChangerLstr.fireStateChange(FLAT_VIEW);
        }
    }

    public void keyReleased(KeyEvent keyEvent) {

    }

    public void keyTyped(KeyEvent keyEvent) {

    }

}
