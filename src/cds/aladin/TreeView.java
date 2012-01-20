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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.StringTokenizer;

import javax.swing.*;
import javax.swing.border.TitledBorder;


/**
 * Le frame de visualisation hi�rarchique (arbre) des donn�es
 *
 * @author Thomas Boch [CDS]
 *
 * @version 0.6 : Octobre 2003 --> nettoyage du code
 *                Juin 2003 --> d�couplage TreeView/MetaDataTree
 *                avril 2003 refonte profonde pour int�gration dans Aladin
 *                novembre 2002 (prototype pour avatar GOODS)
 */
public final class TreeView extends JFrame implements WindowListener, ActionListener {

   static String NOM,SUBMIT,RESET,CLOSE,CLEAR,WERR,UNKNOWNOBJ,NODATA;

   //private static final String SAVE = "Save";

   MetaDataTree mTree;		// l'arbre des resources

   JScrollPane scrollTree;   // ScrollPane contenant l'arbre

   // dimensions initiales du ScrollPane pour l'arbre
   private static final int SCROLL_TREE_WIDTH = 320;
   private static final int SCROLL_INFO_WIDTH = 360;

   String targetFound; // variable de travail
   String error; // pour stocker un message d'erreur de TreeBuilder

   // r�f�rence � aladin
   Aladin aladin;

   protected void createChaine() {
      NOM = aladin.chaine.getString("TREETITLE");
      UNKNOWNOBJ = aladin.chaine.getString("UNKNOWNOBJ");
      SUBMIT = aladin.chaine.getString("SUBMIT");
      RESET = aladin.chaine.getString("RESET");
      CLOSE = aladin.chaine.getString("CLOSE");
      CLEAR = aladin.chaine.getString("CLEAR");
      WERR = aladin.chaine.getString("ERROR");
      NODATA = aladin.chaine.getString("NODATA");
   }


 /** Creation du tree view
   * @param aladin reference
   */
   protected TreeView(Aladin aladin) {
      this.aladin = aladin;
      Aladin.setIcon(this);
      createChaine();
      setTitle(NOM);
	  setSize(450,500);
	  getContentPane().setLayout(new BorderLayout(5,5));


	  JPanel p = new JPanel(new BorderLayout());
      // bordure et titre g�n�ral
      p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4), BorderFactory.createCompoundBorder(
              BorderFactory.createTitledBorder(null, NOM, TitledBorder.LEADING, TitledBorder.TOP, Aladin.BOLD),
              BorderFactory.createEmptyBorder(4,4,4,4))));

	  // on ajoute un component pour g�rer le click pour fermer la fen�tre
	  addWindowListener(this);




      // Creation noeud racine
      ResourceNode root = new ResourceNode(aladin, "root");
	  root.hide = true;
	  root.isOpen = true;
      mTree = new MetaDataTree(root,aladin,null,true);
      mTree.setAllowSortByFields(false);
      mTree.setFullExpandAtStart(false);

      // Le panel de l'arbre hierarchique
      scrollTree = new JScrollPane(mTree,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      scrollTree.setSize(SCROLL_TREE_WIDTH,getSize().height-70);
      mTree.setScroll(scrollTree);
      scrollTree.setBackground(mTree.bkgColor);
      p.add(scrollTree, BorderLayout.CENTER);

      getContentPane().add(p,BorderLayout.CENTER);
      getContentPane().add(buttonPanel(),BorderLayout.SOUTH);

   }

   /** Construction du panel des boutons Submit, Reset
    * @return Le panel contenant les boutons Submit/Reset
    */
    private JPanel buttonPanel() {
       JPanel p = new JPanel();
       p.setLayout( new FlowLayout(FlowLayout.CENTER));
       p.setFont( Aladin.LBOLD );
       p.add(createButton(RESET));
       p.add(createButton(CLEAR));
       JButton b;
       p.add(b = createButton(SUBMIT));
       b.setFont(Aladin.BOLD);
       p.add(createButton(CLOSE));
       return p;
    }

    private JButton createButton(String s) {
    	JButton b = new JButton(s);
    	b.addActionListener(this);

    	return b;
    }

	// M�thodes impl�mentant l'interface WindowListener
	public void windowActivated(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {setVisible(false);}
	public void windowDeactivated(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	// FIN m�thodes impl�mentant l'interface WindowListener

	/** M�thode appel�e par les classes sp�cialisant Server
	 * 	Ajoute la branche correspondante dans l'arbre rt (historique)
	 * 	Met � jour l'arbre pass� en param�tre
	 */
	public boolean updateTree(InputStream is, MetaDataTree tree, Server server, String target, String requestedPos) {
        if( server!=null && server.target!=null && (target==null || target.length()==0) ) {
            target = server.target.getText();
        }
		ResourceNode newBranch = createNewBranch(null,is,target,null,server,null,requestedPos);

        // on affiche une eventuelle erreur
        if( error!=null ) {
            Aladin.warning(tree, error);
            return false;
        //  patch pour Pierre, au cas o� on a z�ro resource retourn�e
        } else if( newBranch.nbChildren<=0 ) {
            Aladin.warning(tree, NODATA);
            return false;
        }

	    // si on n'a pas le target, TreeBuilder l'a p-e trouv� dans le stream
        if( (target==null || target.length()==0) && targetFound!=null ) {
            target = targetFound;
        }

	    if( target!=null && target.length()!=0 ) {
            mTree.addNode(getTargetNode(target, mTree), newBranch);
        }
        else {
            mTree.addNode((ResourceNode)mTree.getRootNode(), newBranch);
        }

        // clonage
        ResourceNode clone = (ResourceNode)newBranch.clone();
        // on cache le niveau avec le nom du serveur
        clone.hide = true;
        tree.setRoot(clone);

        // on demande le focus pour pouvoir utiliser les raccourcis clavier !
        tree.requestFocus();

        // RAZ
        targetFound = null;
        error = null;

        return true;
	}

	/** ajoute une nouvelle branche � tree (utilis� par DiscoveryServer)
	 *  En cas d'erreur ou de z�ro r�sultat retourn�, on �crit le message uniquement
	 *  sur STDIN, le popup n'est pas affich�
	 */
	// TODO : cette histoire de requestedPos, c'est pas tres beau, il faudrait rationaliser en se servant de target
    public boolean appendToTree(InputStream is, MetaDataTree tree, Server server, String target, String requestedPos) {
        if( server!=null && server.target!=null && (target==null || target.length()==0) ) {
            target = server.target.getText();
        }

        ResourceNode newBranch = createNewBranch(null,is,target,null,server,null,requestedPos);

        // on affiche une eventuelle erreur
        if( error!=null ) {
            aladin.command.println("!!! "+error);
            return false;
        }
        //  patch pour Pierre, au cas o� on a z�ro resource retourn�e
        else if( newBranch!=null && newBranch.nbChildren<=0 ) { // test null: PF - 18 nov 05
//            aladin.command.toConsoleln("!!! The query on "+server.nom+" returned no data for this position");
            return false;
        }


        // si on n'a pas le target, TreeBuilder l'a p-e trouv� dans le stream
        if( (target==null || target.length()==0) && targetFound!=null ) {
            target = targetFound;
        }

        if( target!=null && target.length()!=0 ) {
            mTree.addNode(getTargetNode(target, mTree), newBranch);
        }
        else {
            mTree.addNode((ResourceNode)mTree.getRootNode(), newBranch);
        }

        // clonage
        ResourceNode clone = (ResourceNode)newBranch.clone();

        clone.hide = false;
        tree.addNode((ResourceNode)tree.getRootNode(), clone);

        // RAZ
        targetFound = null;
        error = null;

        return true;
    }

	/** Recherche un noeud du nom target. Retourne un nouveau noeud si non trouv� */
	private ResourceNode getTargetNode(String target, MetaDataTree mdt) {
		ResourceNode targetNode = (ResourceNode)mdt.searchNodeByName(target);
    	// if it does not exist yet, we first create this node
   		if( targetNode==null ) {
        	targetNode = new ResourceNode(aladin, target);
        	mdt.getRootNode().addChild(targetNode);
        	mdt.traverseTree();
    	}
    	targetNode.isOpen = true;
    	return targetNode;
	}

	protected static String resolveTarget(String s, Aladin a) {
		Coord c;
		try {
			if( !View.notCoord(s) )
				c = new Coord(s);
			else
				c = a.view.sesame(s);
		}
		catch(Exception e) {return null;}

		return c.getSexa(":");
	}

	   protected static Coord resolveTargetCoo(String s, Aladin a) {
	        Coord c=null;
	        try {
	            if( !View.notCoord(s) )
	                c = new Coord(s);
	            else
	                c = a.view.sesame(s);
	        }
	        catch(Exception e) {return null;}

	        return c;
	    }




	/** Construit une nouvelle branche de l'arbre dans un contexte thread� ou non
	 *
	 * @param url url d'acc�s au fichier de description des resources (peut �tre null)
	 * @param is stream de description des resources (peut �tre null)
	 * @param target coordon�es ou nom de l'objet (si url!=null, peut �tre null)
	 * @param radius rayon de recherche
	 * @param server serveur d'ou provient
	 * @param serverName nom du serveur
	 * @return ResourceNode r�f�rence vers la "t�te" de la branche que l'on a cr��
	 */
    private ResourceNode createNewBranch(URL url, InputStream is, String target, String radius, Server server, String serverName, String requestedPos) {
        // r�solution du target
        String targetCoo=null;
        if( url==null && is==null ) {
          targetCoo = resolveTarget(target,aladin);
          if( targetCoo==null ) {
              Aladin.warning(UNKNOWNOBJ+" "+target,1);
              return null;
          }
        }

        ResourceNode newBranch;
        if( url==null && is==null )
          newBranch = createNode(targetCoo,radius,server,requestedPos);
        else newBranch = createNode(url,is,server,requestedPos,target);

        // si newBranch null --> pb
        if( newBranch==null ) {
            Aladin.trace(3,"The new branch of the tree could not be built");
            // fin du thread
            return null;
        }


        // ajout de cette nouvelle branche dans l'arbre
        if( serverName==null ) {
          if( server instanceof ServerAladin )
              serverName = "Aladin";
          else if( server instanceof ServerVizieR )
              serverName = "VizieR";
          else {
              if( url==null ) {
                  if (server==null) {
                      serverName = null;
                  }
                  else {
                      serverName=server.aladinLabel;
                  }
              }
              else {
                  serverName = url.toString();
                  serverName = "..."+serverName.substring(Math.max(0,serverName.length()-20));
              }

          }
        }

        newBranch.name = serverName;

        return newBranch;
	}

   /** construit l'URL qui renverra le XML permettant la construction d'une partie de l'arbre */
   private URL buildURL(String target, String radius, Server server) {
      // on utilisera peut-etre une marque GLU
      URL url;
      // radius en degr�s
      double radiusDeg = Server.getAngle(radius,Server.RADIUS)/60.0;
      try {
        if( server instanceof ServerAladin ) {
			url = new URL("http://aladin.u-strasbg.fr/cgi-bin/nph-HTTP.cgi?out=qualifier&position="+URLEncoder.encode(getDeciCoord(target))+"&radius="+radiusDeg+"&mode=xml_votable");

		}
		else if( server instanceof ServerVizieR) {
			url = new URL("http://vizier.u-strasbg.fr/cgi-bin/votable/-w?-meta&-c="+URLEncoder.encode(target)+"&-c.r="+radiusDeg+"&-c.u=degree&-c.eq=J2000");
		}

		else
			return null;
      }
      catch(MalformedURLException e) {
		  e.printStackTrace();
      	  aladin.trace(1,"URL could not be built");
		  return null;
	  }
	  aladin.trace(3,"Building url "+url);
	  return url;
   }


    /** Creation d'un plan Catalogue
        Code repris de VizieRServer
    */
    protected int creatCatPlane(String cat, String target, String label) {

        URL u;
        //String sz = new Double(Server.getRadius(radius.getText())).toString();
        // � modifier
        String sz = "10";
		return ((ServerVizieR)aladin.dialog.server[ServerDialog.VIZIER]).creatVizieRPlane(target,sz,cat,label,"",false);
    }

    /** @param coo chaine contenant la coordonn�e en sexa
        @return chaine avec coordonn�es en d�cimal
     */
    static String getDeciCoord(String coo) {
        Coord c;
        try {c = new Coord(coo);}
        catch( Exception e ) {return "";}
        return c.al+" "+(c.del>=0.0?"+":"")+c.del;
    }

    static double[] getDeciCoord(String ra, String dec) {
        Coord c;
        try {c = new Coord(ra+" "+dec);}
        catch( Exception e ) {return null;}
        return new double[] {c.al , c.del};
    }

    /** cr�e un noeud(et tous les subnodes) pour un target, un radius et un serveur donn�s
     *  @param target target demand� (r�solu)
     *  @param radius rayon entr� (sous forme de chaine)
	 *  @param server serveur d'ou provient la requete
	 *  @return le noeud cr��
     */
    private ResourceNode createNode( String target, String radius, Server server, String requestedPos) {
		URL url = buildURL(target,radius,server);
		return createNode(url,null,server,requestedPos,target);
    }

	/** cr�e un noeud et les sous-noeuds pour une URL ou un InputStream donn�
	 *
	 * @param url pointeur vers le fichier d�crivant les resources
	 * @param is stream contenant la description des resources
	 * @param server le serveur (Aladin, VizieR, ...)
	 * @return le noeud cr��
	 */
	private ResourceNode createNode(URL url, InputStream is, Server server,
	                                String requestedPos, String target) {
        // si url nulle --> pb
        if( url==null && is==null ) {
            return null;
        }

        int type = -1;
        if( server instanceof ServerAladin )
            type = TreeBuilder.VOTABLE_IDHA;
        // TB - 21/11/2005 - pour VizieR, on passe pour le moment par un faux SIAP, il ne faut donc pas overrider le type !
//        else if( server instanceof VizieRServer )
//            type = TreeBuilder.VIZIER;

        Aladin.trace(1,"Updating the data tree view, using "+(url!=null?"URL "+url:"input stream"));
        ResourceNode ret = null;
        try {
            TreeBuilder tb;
            if( url!=null ) {
                // TODO : lancer une exception dans certains cas pour faire remonter les erreurs
                tb = new TreeBuilder(aladin, url, type, server, target);
            }
            else {
                tb = new TreeBuilder(aladin, is, type, server, target);
            }
            if( requestedPos!=null ) tb.setRequestedPos(requestedPos);
        	ret = tb.build();
        	targetFound = tb.getTarget();
            error = tb.getError();

            if( ret!=null ) ret.hide = false;  // test null: PF - 18 nov 05
        }
        catch(Exception e) {
            System.out.println("An error occured while building the tree :");
            e.printStackTrace();
            if( ret==null ) error = "Could not build the tree for server "+server.aladinLabel;
            else error = e.getMessage();
        }

        return ret;
	}


  /** On charge toutes les feuilles s�lectionn�es ET visibles
   */
   public void submit() {
      mTree.loadSelected();
      mTree.resetCb();
   }

  /** Construit le label du plan en fonction du qualifier et de la resolution
   * Exemple: PLATE SERC J STScI => Pl-SERC.J.DSS1
   */
  static protected String getPlanLabel(String resol, String qual) {
     return getPlanLabel( PlanImage.getRes(resol),qual);
  }
  static protected String getPlanLabel(int resol, String qual) {
     StringBuffer s = new StringBuffer();
     String [] f = new String[3];

     // On decoupe les trois champs SURVEY COLOR SCAN
     StringTokenizer st = new StringTokenizer(qual);
     for( int i=0; i<3; i++ ) f[i] = st.nextToken();

     if( f[0].indexOf("2MASS")>=0 ) { f[2]=f[0]; f[0]=null; }

     // Et on les assemble dans le sens inverse eventuellement
     // precedes par l'indication Pl- pour Plate
     if( resol==PlanImage.PLATE ) s.append("Pl-");
     else if( resol==PlanImage.LOW ) s.append("Lw-");
     if( f[2].equals("STScI") || f[2].equals("STSCI") ) s.append("DSS1");
      else if( !f[2].startsWith("___") ) s.append(f[2]+".");
     s.append(f[1]);
     if( f[0]!=null ) s.append("."+(f[0].startsWith("GOODS-")?f[0].substring(6):f[0]));

     return s.toString();
  }


  /** reset : tous les checkboxes sont d�selectionn�s */
   protected void reset() {
      mTree.resetCb();
   }

   public void actionPerformed(ActionEvent ae) {
	   String o = ae.getActionCommand();

	   if( o.equals(SUBMIT) ) submit();
	   else if( o.equals(RESET) ) reset();
	   else if( o.equals(CLEAR) ) mTree.clear();
	   else if( o.equals(CLOSE) ) setVisible(false);
	   //else if( o.equals(SAVE) ) rt.saveAsIDHA(null);

   }

}
