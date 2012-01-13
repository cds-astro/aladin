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
import java.awt.FileDialog;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.image.ColorModel;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.*;

import cds.tools.Util;
import cds.xml.Field;
import cds.xml.XMLConsumer;
import cds.xml.XMLParser;

/**
 * Le formulaire d'interrogation des donnees et images
 * sur le disque local
 * <P>
 * <B>Rq :</B> Utilise uniquement par la version Standalone
 *
 * @author Pierre Fernique [CDS]
 * @version 2.1 : dec 03 - ajustement du scroll du select apres chargement AJ
 * @version 2.0 : jan 03 - Suppression du Layout Manager et toilettage
 * @version 1.5 : 12 mars 02 readFully deplace dans la classe PlanImage
 * @version 1.4 : 14 jan 02 Local(http:) pour les catalogues
 * @version 1.3 : 19 juin 00 Utilisation du PushbackInputStream
 *                pour readFully()
 * @version 1.2 : 8 juin 2001 - correction readFully()
 * @version 1.1 : 11 avril 2000 - contournement "out of memory"
 * @version 1.0 : 9 dec 1999 - Creation
 */
public class ServerFile extends Server implements XMLConsumer {

   String BROWSE;

   JTextField file;		// Le champ de saisie du nom de fichier
   static String loadError;	// Le message d'erreur courant
   String titre,info1,info2,info3;

   // variable de travail pour le parsing des "circle"
   private Coord circleCenter;

   @Override
   protected void createChaine() {
      super.createChaine();
      description   = aladin.chaine.getString("FILEINFO");
      titre  = aladin.chaine.getString("FILETITLE");
      verboseDescr   = aladin.chaine.getString("FILEDESC");
      info1  = aladin.chaine.getString("FILEINFO1");
      info2  = aladin.chaine.getString("FILEINFO2");
      info3  = aladin.chaine.getString("FILEINFO3");
      BROWSE = aladin.chaine.getString("FILEBROWSE");
   }

   /** Initialisation des variables propres */
   protected void init() {
      type = APPLI;
      aladinLogo = "MyDataLogo.gif";
   }


   /** Creation du formulaire d'interrogation des images sur le disque
    * @param aladin reference
    */
   protected ServerFile(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
      init();

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      int y=45;

      // Le  titre
      JPanel tp = new JPanel();
      tp.setBackground(Aladin.BLUE);
      Dimension d = makeTitle(tp,titre);
      tp.setBounds(470/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);

      // Premiere indication
      JLabel l = new JLabel(info1+" "+(Aladin.STANDALONE?info2+" ":"")+info3);
      l.setBounds(20,y,400, 20); y+=20;
      add(l);

      // Le nom du fichier a charger
      file = new JTextField(50);
      file.addKeyListener(this);
      int wpos = 60;
      int xpos = XWIDTH-XTAB1-wpos;
      file.setBounds(XTAB1,y,xpos-15,HAUT);
      add(file);

      // Pour s'aider d'une boite de recherche
      if( Aladin.STANDALONE ) {
         JButton browse = new JButton(BROWSE);
         browse.setMargin(new Insets(0,0,0,0) );
         browse.addActionListener(this);
         browse.setBounds(xpos,y+5,wpos,20); y+=40;
         add(browse);
      }
      else y+=40;

      tree = new MetaDataTree(aladin, null);
      JScrollPane sc = new JScrollPane(tree);
      tree.setScroll(sc);
      sc.setBackground(tree.bkgColor);
      sc.setBounds(XTAB1,y,XWIDTH-XTAB1*2,230); y+=230;
      add(sc);

      // Indication du component à maximiser
      setMaxComp(sc);
   }

   /** Retourne true si le serveur correspond a la chaine
    * passee en parametre. Il a fallu deriver cette methode
    * pour garder la compatibilite avec "local" et "MyData"
    * @return true: Ok c'est ce serveur.
    */
   @Override
   protected boolean is(String s) {
      if( s.equalsIgnoreCase("Local") || s.equalsIgnoreCase("MyData") || s.equalsIgnoreCase("File")) return true;
      return super.is(s);
   }

   /** Creation d'un plan de maniere generique
    * Le target et radius sont ignores, criteria contient
    * le nom du fichier ou l'URL
    */
   @Override
   protected int createPlane(String target,String radius,String criteria,
         String label, String origin) {
      String f=criteria;

      Tok st = new Tok(criteria," ,");
      f = st.nextToken();
      if( st.hasMoreTokens() ) label = st.nextToken();
      if( st.hasMoreTokens() ) origin = st.nextToken();

      // le fichier (ou url) peut etre suivi d'un mime type
      // avec un blanc comme separateur, celui-ci sera simplement ignore
      // puisque Aladin reconnait desormais automatiquement le contenu
      int i=f.lastIndexOf(' ');
      if( i>0 ) {
         String m = f.substring(i+1).toLowerCase();
         if( m.startsWith("image/") || m.startsWith("text/") ) {
            f=f.substring(0,i);
         }
      }
      return creatLocalPlane(f,label,origin,null,null,null,this);
   }

   /** Creation d'un plan issu d'un chargement d'un fichier AJ, fits ou autre
    * Le choix se fait en fonction de l'extension du nom de fichier
    * 	.aj	-> AJ
    *	.fits	-> FITS
    *	autre	-> XML + TSV
    * @param f path du fichier
    */
   //    protected int creatLocalPlane(String f,String label,String origin) {
   //    	return creatLocalPlane(f,label,origin,null,null,null);
   //    }
   //
   //    protected int creatLocalPlane(InputStream is, String label,String origin) {
   //    	return creatLocalPlane(null,label,origin,null,null,is);
   //    }

   /** Retourne le nom du fichier ou de l'url en enlevant un éventuel suffixe
    * [nnn] indiquant une extension particulière d'un MEF */
   private String getNameWithoutBrackets(String s) {
      if( s==null ) return null;
      int n = s.length();
      if( n<4 || s.charAt(n-1)!=']' ) return s;
      int pos = s.lastIndexOf('[');
      if( pos<0 ) return s;
      try {
         n = Integer.parseInt(s.substring(pos+1,n-1));
         return s.substring(0,pos);
      } catch(Exception e) { }
      return s;
   }

   /** Creation d'un plan issu d'un chargement d'un fichier AJ, fits ou autre
    * @param f path du fichier
    * @param resNode noeud décrivant le fichier à charger, peut être <i>null</i>
    */
   protected int creatLocalPlane(String f,String label,String origin, Obj o, ResourceNode resNode,InputStream is,Server server) {
      String serverTaskId = aladin.synchroServer.start("ServerFile.creatLocalPlane/"+label);
      try {
//         setSync(false);
         int n=0;
         MyInputStream in;
         long type;
         URL u=null;
         boolean localFile=false;

         if( f!=null ) f=aladin.getFullFileName(f);

         waitCursor();
         try {
            if( label==null ) {
               int i = f.lastIndexOf(f.startsWith("http:")||f.startsWith("https:")||f.startsWith("ftp:") ? "/"
                     : Util.FS);
               label=(i>=0)?f.substring(i+1):f;

               // Suppression d'une extension éventuelle
               i = label.lastIndexOf('.');
               if( i>0 && label.length()-i<=5 ) label = label.substring(0,i);
            }

            // Analyse du contenu d'un répertoire local
            if( is==null && !(f.startsWith("http:") || f.startsWith("https:"))) {
               try {
                  final File x = new File(f);
                  if( x.isDirectory() ) {
//                     setSync(true);
                     Aladin.trace(4,"ServerFile.creatLocalPlane("+f+"...) => detect: DIR");
                     if( PlanBG.isPlanBG(f) ) {

                        // Catalogue ?
                        if( (new File(f+"/Norder3/Allsky.xml")).exists() ) {
                           TreeNodeAllsky gSky;
                           try { gSky = new TreeNodeAllsky(aladin, f); }
                           catch( Exception e ) {
                              aladin.trace(4, "ServerFile.creatLocalPlane(...) Allsky properties file not found, assume default params");
                              gSky = new TreeNodeAllsky(aladin, null, null, null, null, null, null, null, null, null, f, "15 cat");
                           }
                           n=aladin.calque.newPlanBG(gSky,label,null,null);

                           // ou Image
                        } else n=aladin.calque.newPlanBG(f,label,null,null);
                     }
                     else {
                        final ServerFile th = this;
                        (new Thread(){
                           public void run() {
                              try {
                                 aladin.log("load", "dir");
                                 MyInputStream mi = new MyInputStream((new IDHAGenerator()).getStream(x,th)); 
                                 updateMetaData(mi,th,"",null);
                                 mi.close();
                              } catch( IOException e ) {
                                 e.printStackTrace();
                              }
                              defaultCursor();
                           }
                        }).start();
                     }
                     return n;
                  }
               } catch( Exception e) {
                  defaultCursor();
                  e.printStackTrace();
                  return n;
               }
            }

            if( origin==null ) origin=f;

            // Pour loguer
            String mode= is==null ? "file" : "stream";

            // Pas de verification de la redondance pour les fichiers locaux
            // mais tout de meme affichage des messages divers
            flagVerif=false;
            verif(0,null,null,null);

            // gestion des URL file:... sous Windows
            if (f!=null && f.startsWith("file:")) {
               f = f.replaceAll("\\\\", "/");
            }

            //Obtention du stream (donnee locale ou distante)
            if( is==null && (f.startsWith("http:")||f.startsWith("https:")) ) {
               u = aladin.glu.getURL("Http",getNameWithoutBrackets(f),true,true);
               in = Util.openStream(u);
               mode="http";
            }

            // support FTP --> ça fonctionne avec par exemple une URL du type ftp://user:passwd@server/....
            else if( is==null && f.startsWith("ftp://") ) {
               u = new URL(getNameWithoutBrackets(f));
               in = Util.openStream(u);
               mode="ftp";
            }
            // URL du type file://...
            else if( is==null && f.startsWith("file:/") ) {
               localFile=true;
               u = new URL(getNameWithoutBrackets(f));
               in = Util.openStream(u);
               mode="file";
            }

            else {
               if( is==null ) {
                  localFile=true;
                  Aladin.trace(3,"Opening "+getNameWithoutBrackets(f));
                  in = new MyInputStream(new FileInputStream( getNameWithoutBrackets(f) ));
               }
               else {
                  // Dans le cas d'une continuation (FITS EXTENSION)
                  if( is instanceof MyInputStream ) in = (MyInputStream)is;

                  // Dans le cas normal
                  else in = new MyInputStream(is);
               }
            }
            in = in.startRead();
            type = in.getType();

            // Petit rajouti pour reconnaitre l'extension AJS pour les scripts Aladin
            if( f!=null && f.endsWith(".ajs") ) type |= MyInputStream.AJS;

            // Petit rajouti pour reconnaitre l'extension REG pour les regions DS9
            if( f!=null && f.endsWith(".reg") ) type |= MyInputStream.AJS;

            String t = in.decodeType(type);
            Aladin.trace(3,(f==null?"stream":f)+" => detect: "+t);
            aladin.log("load",mode+t);

            if( (type & MyInputStream.AJS|type & MyInputStream.AJSx|MyInputStream.UNKNOWN)!=0) aladin.command.readFromStream(in);
            else if( (type & MyInputStream.AJ)!=0) loadAJ(in);
            else if( (type & MyInputStream.IDHA)!=0) updateMetaData(in,server,"",null);
            else if( (type & MyInputStream.SIA_SSA)!=0)  updateMetaData(in,server,"",null);

            else if( (type & MyInputStream.HPXMOC)!=0 ) {
               n=aladin.calque.newPlanHpxMOCM(in,label);
            }
            else if( (type & MyInputStream.HPX)!=0 ) {
               n=aladin.calque.newPlanHealpix(f,in,label,PlanBG.DRAWPIXEL,0, false);
            }
            else if( (type & MyInputStream.FITS)!=0 && (type & MyInputStream.RGB)!=0 ) {
               if( u!=null ) {
                  n=aladin.calque.newPlanImageRGB(u,in,PlanImage.OTHER,
                        label,null,f, origin,
                        PlanImage.UNKNOWN,PlanImage.UNDEF,
                        null,resNode);
               } else n=aladin.calque.newPlanImageRGB(f,null,in,resNode);
            }
            else if( (type & MyInputStream.XFITS)!=0) {
               aladin.calque.newFitsExt(f,in,label,o);
               n=1;
            }
            else if( (type & (MyInputStream.FITS|MyInputStream.PDS))!=0) {
               if( u!=null ) {
                  n=aladin.calque.newPlanImage(u,in,PlanImage.OTHER,
                        label,null,f, origin,
                        PlanImage.UNKNOWN,PlanImage.UNDEF,
                        o,resNode);
               } else 
                  n=aladin.calque.newPlanImage(f,in,label,origin,o,resNode);
            }
            else if( (type & MyInputStream.FOV_ONLY) != 0 ) {
               // un nouveau plan sera créé sur la pile si la description contient les PARAM de position
               boolean newPlane = (n=aladin.processFovVOTable(in,null,true))>=0;
               // si on a juste ajouté un FOV à la liste des FOV chargés,
               // on se place sur cet onglet et on sélectionne le FOV en question
               if( !newPlane ) {
                  n=-2;  // Pour éviter une erreur via VOAPP (--> c'est du propre !)
                  aladin.dialog.setCurrent(ServerDialog.FIELD);
                  ((ServerFoV)aladin.dialog.server[ServerDialog.FIELD]).selectFOV(ServerFoV.idLastRegistered);
               }
            }
            else if( (type & (MyInputStream.ASTRORES|MyInputStream.VOTABLE|
                  MyInputStream.CSV|MyInputStream.BSV|MyInputStream.IPAC))!=0 ) {
               if( u!=null ) n=aladin.calque.newPlanCatalog(u,in,label,"",f,null,server);
               else if( f!=null) n=aladin.calque.newPlanCatalog(f,in);
               else n=aladin.calque.newPlanCatalog(in,label,origin);

               // C'est peut être une image native ?
            } else if( (type & MyInputStream.NativeImage())!=0 ) {
               if( u!=null ) {
                  n=aladin.calque.newPlanImageColor(u,in,PlanImage.OTHER,
                        label,null,f, origin,
                        PlanImage.UNKNOWN,PlanImage.UNDEF,
                        o,resNode);
               } else n=aladin.calque.newPlanImageColor(f,null,in,resNode);

               // C'est peut être un dico GLU ?
            } else if( (type & MyInputStream.GLU)!=0 ) {
               if( aladin.glu.loadGluDic(new DataInputStream(in), false,localFile) ) {
                  aladin.glu.reload(false);
               }

               // C'est peut être un planBG via HTTP
            } else if( mode.equals("http") && f!=null && f.indexOf('?')<0 ) {

               // images ?
               if( Util.isUrlResponding(new URL(f+"/Norder3/Allsky.jpg"))
                     || Util.isUrlResponding(new URL(f+"/Norder3/Allsky.fits")) ) n=aladin.calque.newPlanBG(new URL(f),label,null,null);

               // ou catalogue ?
               else if( Util.isUrlResponding(new URL(f+"/Norder3/Allsky.xml")) ) {
                  TreeNodeAllsky gSky;
                  try { gSky = new TreeNodeAllsky(aladin, f); }
                  catch( Exception e ) {
                     aladin.trace(4, "ServerFile.creatLocalPlane(...) Allsky properties file not found, assume default params");
                     gSky = new TreeNodeAllsky(aladin, null, null, f, null, null, null, null, null, null, null, "15 cat");
                  }
                  n=aladin.calque.newPlanBG(gSky,label,null,null);
               }

               else throw new Exception("Data format not recognized");

            } else {
               throw new Exception("Data format not recognized");
            }
            aladin.endMsg();

            // Dans le cas de Meta-donnee (SIA ou IDHA) on va automatiquement ouvrir
            // et positionner la fenetre des formulaires toFront
            if( (type & (MyInputStream.SIA_SSA|MyInputStream.IDHA))!=0 ) {
               aladin.dialog.show();
               aladin.dialog.setCurrent(aladinLabel);
            }
         } catch(Exception e) {
            e.printStackTrace();
            Aladin.warning(this,""+e,1);
            defaultCursor();
            ball.setMode(Ball.NOK);
//            setSync(true);
            return -1;
         }
         defaultCursor();
//         setSync(true);
         return n;
      } finally { aladin.synchroServer.stop(serverTaskId); }
   }

   String of="";   // Precedente chaine dans le champ de saisie

   @Override
   protected void setInitialFocus() {
      file.requestFocus();
      file.setCaretPosition(file.getText().length());
   }

   /** Chargement de l'image ou des donnees */
   @Override
   public void submit() {
      waitCursor();
      String f = file.getText().trim();
      f=aladin.getFullFileName(f);
      if( !f.equals(of) ) tree.clear();
      of=f;
      if( tree!=null && !tree.isEmpty() ) {
         if( tree.nbSelected()>0 ) {
            tree.loadSelected();
            tree.resetCb();
         } else Aladin.warning(this,WNEEDCHECK);
         defaultCursor();
      } else creatLocalPlane(f,null,null,null,null,null,this);
   }

   /** Nettoyage du formulaire */
   @Override
   protected void clear() {
      file.setText("");
      tree.clear();
      super.clear();
   }

   /** Reset du formulaire */
   @Override
   protected void reset() {
      if( tree!=null ) tree.resetCb();
   }


   private static final String DEFAULT_FILENAME = "-";

   /** Gestion des evenements.
    * Le bouton BROWSE permet d'afficher la fenetre de recherche des
    * fichiers
    */
   @Override
   public void actionPerformed(ActionEvent e) {
      Object s = e.getSource();

      // Affichage du selecteur de fichiers
      if( s instanceof JButton
            && ((JButton)s).getActionCommand().equals(BROWSE )) {
         browseFile();
         return;
      }

      super.actionPerformed(e);
   }

   /** Ouverture de la fenêtre de sélection d'un fichier */
   protected void browseFile() {
      FileDialog fd = new FileDialog(aladin.dialog,description);
      aladin.setDefaultDirectory(fd);

      // (thomas) astuce pour permettre la selection d'un repertoire
      // (c'est pas l'ideal, mais je n'ai pas trouve de moyen plus propre en AWT)
      fd.setFile(DEFAULT_FILENAME);
      fd.setVisible(true);
      aladin.memoDefaultDirectory(fd);
      String dir = fd.getDirectory();
      String name =  fd.getFile();
      // si on n'a pas changé le nom, on a selectionne un repertoire
      boolean isDir = false;
      if( name!=null && name.equals(DEFAULT_FILENAME) ) {
         name = "";
         isDir = true;
      }
      String t = (dir==null?"":dir)+(name==null?"":name);
      file.setText(t);
      if( (name!=null && name.length()>0) || isDir ) submit();
   }


   /** Chargement d'un fichier au format AJ
    * @param in l'inputStream
    * @return true si ok, false sinon
    */
   protected boolean loadAJ(MyInputStream in) {
      boolean rep;

      XMLParser xmlParser = new XMLParser(this);
      loadError=null;

      try {
         rep=xmlParser.parse(in);
      } catch( Exception e ) {
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         loadError=""+e; return false;
      }
      if( !rep ) loadError=xmlParser.getError();
      aladin.view.findBestDefault();
      //      aladin.calque.activateAll();
      aladin.view.setDefaultRepere();
      aladin.calque.repaintAll();
      return (rep && loadError==null );
   }

   /* Variables temporaires associees au parsing du XML */
   private boolean inValue,inFilterScript,inFitsHeader,inFilter;  // Etat
   private int nFilter=0;   // Indice du filtre en cours d'insertion
   private Vector vField;	// Vecteur contenant les FIELDS de la table courante
   private Legende leg=null;	// Legende de la table courante
   private Plan plan=null;	// Plan courant
   private ViewMemoItem vmi;  // ViewMemoItem courant
   private int firstView;   //indice de la première vue à afficher
   private double ra=0;		// RA du centre du plan courant
   private double de=0;		// DE du centre du plan courant
   private double rm=0;		// SIZE du plan courant
   private int proj=0;		// Type de la projection courante (cf Projection.NAME[])
   private int typePlan=-1;	// Type du plan courant (cf Plan.Type)
   private boolean flagCatalogSource=false; // true si le plan courant est TOOL et que celui-ci contient également un catalogue de sources
   private String rec;		// Enregistrement courant dans l'analyse des lignes d'une table
   private Obj prevO=null;			// Dernier objet tool inserer dans la table courante
   private boolean prevFlagSuite=false;		// Dernier flag de suivi de ligne


   /** Creation d'un ViewMemoItem (tag <VIEW>)
    * @param atts les attributs XML du tag VIEW
    * @return le viewMemoItem cree
    */
   private ViewMemoItem creatViewMemoItemByAJ(Hashtable atts) {
      ViewMemoItem vmi = new ViewMemoItem();
      String s;
      if( (s=(String)atts.get("zoom"))!=null )       vmi.zoom=Double.valueOf(s).doubleValue();
      if( (s=(String)atts.get("xzoomView"))!=null )  vmi.xzoomView=Double.valueOf(s).doubleValue();
      if( (s=(String)atts.get("yzoomView"))!=null )  vmi.yzoomView=Double.valueOf(s).doubleValue();
      if( (s=(String)atts.get("rzoomWidth"))!=null ) vmi.rzoomWidth=Double.valueOf(s).doubleValue();
      if( (s=(String)atts.get("rzoomHeight"))!=null )vmi.rzoomHeight=Double.valueOf(s).doubleValue();
      if( (s=(String)atts.get("rvWidth"))!=null )    vmi.rvWidth=Integer.parseInt(s);
      if( (s=(String)atts.get("rvHeight"))!=null )   vmi.rvHeight=Integer.parseInt(s);
      if( (s=(String)atts.get("pref"))!=null )       vmi.pref=aladin.calque.getPlan(s,1);
      if( (s=(String)atts.get("roi"))!=null
            ||(s=(String)atts.get("locked"))!=null )     vmi.locked=(new Boolean(s)).booleanValue();
      if( (s=(String)atts.get("northUp"))!=null )    vmi.northUp=(new Boolean(s)).booleanValue();
      //       if( (s=(String)atts.get("sync"))!=null )       vmi.sync=(new Boolean(s)).booleanValue();
      return vmi;
   }


   /** Creation d'un plan courant (tag <PLANE>)
    * @param atts les attributs XML du tag PLANE
    * @return le plan cree
    */
   private Plan creatPlaneByAJ(Hashtable atts) {
      String s;
      String type = (String)atts.get("type");
      typePlan= Util.indexInArrayOf(type, Plan.Tp);

      inFilter = inFitsHeader = inFilterScript = false;
      switch(typePlan) {
         case Plan.FILTER:
            PlanCatalog p = null;
            if( (s=(String)atts.get("dedicatedto"))!=null ) p=(PlanCatalog)aladin.calque.getPlan(s,1);
            plan = ( new PlanFilter(aladin,"",null,p));
            break;
         case Plan.FOLDER:
            plan = ( new PlanFolder(aladin));
            if( (s=(String)atts.get("localscope"))!=null ) ((PlanFolder)plan).localScope=(new Boolean(s)).booleanValue();
            break;
         case Plan.CATALOG:
            plan = new PlanCatalog(aladin);
            if( (s=(String)atts.get("object"))!=null ) plan.objet = s;
            if( (s=(String)atts.get("param"))!=null )  plan.param = s+".";
            if( (s=(String)atts.get("from"))!=null )   plan.from = s;
            if( (s=(String)atts.get("RA"))!=null )     ra=Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("DE"))!=null )     de=Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("radius"))!=null ) rm=Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("color"))!=null )  plan.c=Action.getColor(s);;
            break;
         case Plan.TOOL:
            plan = ( new PlanTool(aladin));
            if( (s=(String)atts.get("color"))!=null )  plan.c=Action.getColor(s);
            if( (s=(String)atts.get("withsource"))!=null )  flagCatalogSource=true;
            if( (s=(String)atts.get("xylock"))!=null ) {
               ((PlanTool)plan).hasXYorig = (new Boolean(s)).booleanValue();
            }
            break;
         case Plan.IMAGEMOSAIC:
         case Plan.IMAGERSP:
         case Plan.IMAGEALGO:
         case Plan.IMAGERGB:
         case Plan.IMAGE:
            boolean hasOrigPixel = atts.get("cacheID")!=null;
            if( typePlan==Plan.IMAGE ) plan = ( new PlanImage(aladin));
            else if( typePlan==Plan.IMAGEMOSAIC ) plan = ( new PlanImageMosaic(aladin));
            else if( typePlan==Plan.IMAGERSP )    plan = ( new PlanImageResamp(aladin));
            else if( typePlan==Plan.IMAGEALGO )   plan = ( new PlanImageAlgo(aladin));
            else if( typePlan==Plan.IMAGERGB )    {
               plan = ( new PlanImageRGB(aladin));
               ((PlanImageRGB)plan).RGBControl = new int[9];
            }
            if( (s=(String)atts.get("object"))!=null )     plan.objet = s;
            PlanImage pi = (PlanImage)plan;
            if( (s=(String)atts.get("param"))!=null )      plan.param = s+".";
            if( (s=(String)atts.get("fmt"))!=null )        pi.fmt = PlanImage.getFmt(s);
            if( (s=(String)atts.get("resolution"))!=null ) pi.res = PlanImage.getRes(s);
            if( (s=(String)atts.get("from"))!=null )       plan.from = s;
            if( (s=(String)atts.get("cacheID"))!=null )    pi.cacheID = s;
            if( (s=(String)atts.get("cacheOffset"))!=null )pi.cacheOffset = Long.parseLong(s);
            if( (s=(String)atts.get("url"))!=null ) {
               try { plan.u = new URL(s); } catch(Exception e) {};
            }
            if( (s=(String)atts.get("RA"))!=null )     ra=Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("DE"))!=null )     de=Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("radius"))!=null ) rm=Double.valueOf(s).doubleValue();
            pi.transfertFct=PlanImage.LINEAR;
            pi.bScale=1.0;
            pi.bitpix=8;
            pi.npix=1;
            pi.typeCM=0;
            if( (s=(String)atts.get("width"))!=null )      pi.naxis1=pi.width = Integer.parseInt(s);
            if( (s=(String)atts.get("height"))!=null )     pi.naxis2=pi.height = Integer.parseInt(s);
            if( (s=(String)atts.get("video"))!=null )      pi.video = Integer.parseInt(s);
            if( /* hasOrigPixel &&*/ (s=(String)atts.get("transfertFct"))!=null )pi.transfertFct = Integer.parseInt(s);
            if( (s=(String)atts.get("minPix"))!=null )     pi.dataMin = Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("maxPix"))!=null )     pi.dataMax = Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("minPixCut"))!=null )  pi.pixelMin = Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("maxPixCut"))!=null )  pi.pixelMax = Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("bZero"))!=null )      pi.bZero = Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("bScale"))!=null )     pi.bScale = Double.valueOf(s).doubleValue();
            if( (s=(String)atts.get("cm"))!=null )         pi.typeCM = Integer.parseInt(s);
            if( (s=(String)atts.get("colormap1"))!=null )  pi.cmControl[0] = Integer.parseInt(s);
            if( (s=(String)atts.get("colormap2"))!=null )  pi.cmControl[1] = Integer.parseInt(s);
            if( (s=(String)atts.get("colormap3"))!=null )  pi.cmControl[2] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl1"))!=null)((PlanImageRGB)pi).RGBControl[0] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl2"))!=null)((PlanImageRGB)pi).RGBControl[1] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl3"))!=null)((PlanImageRGB)pi).RGBControl[2] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl4"))!=null)((PlanImageRGB)pi).RGBControl[3] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl5"))!=null)((PlanImageRGB)pi).RGBControl[4] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl6"))!=null)((PlanImageRGB)pi).RGBControl[5] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl7"))!=null)((PlanImageRGB)pi).RGBControl[6] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl8"))!=null)((PlanImageRGB)pi).RGBControl[7] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBControl9"))!=null)((PlanImageRGB)pi).RGBControl[8] = Integer.parseInt(s);
            if( (s=(String)atts.get("RGBRed"))!=null)     ((PlanImageRGB)pi).labelRed=s;
            if( (s=(String)atts.get("RGBGreen"))!=null)   ((PlanImageRGB)pi).labelGreen=s;
            if( (s=(String)atts.get("RGBBlue"))!=null)    ((PlanImageRGB)pi).labelBlue=s;
            if( (s=(String)atts.get("opacity"))!=null )    pi.setOpacityLevel(Float.valueOf(s).floatValue());
            if( hasOrigPixel && (s=(String)atts.get("bitpix"))!=null )     {
               pi.bitpix = Integer.parseInt(s);
               pi.npix = Math.abs(pi.bitpix)/8;
            }

            break;
         default: plan=null;
      }
      // Traitements génériques
      if( plan!=null ) {
         if( (s=(String)atts.get("label"))!=null )      plan.label = s;
         if( (s=(String)atts.get("depth"))!=null ) plan.folder = Integer.parseInt(s);
         if( (s=(String)atts.get("activated"))!=null ) plan.active=plan.askActive = (new Boolean(s)).booleanValue();
         if( (s=(String)atts.get("selectable"))!=null ) plan.setSelectable( (new Boolean(s)).booleanValue() );
         if( plan instanceof PlanImage ) ((PlanImage)plan).orig = PlanImage.LOCAL;

         // Mise en place de la calib et de la projection associee
         if( (s=(String)atts.get("proj"))!=null )   proj=Integer.parseInt(s);
         if( (s=(String)atts.get("calib"))!=null ) {
            Calib c = new Calib();
            parseCalib(c,s);
            c.adxpoly=parsePoly((String)atts.get("adxpoly"));
            c.adypoly=parsePoly((String)atts.get("adypoly"));
            c.xyapoly=parsePoly((String)atts.get("xyapoly"));
            c.xydpoly=parsePoly((String)atts.get("xydpoly"));
            c.CD=parseMat((String)atts.get("CD"));
            c.ID=parseMat((String)atts.get("ID"));
            plan.setNewProjD(new Projection(proj,c));
            if( plan instanceof PlanImage ) ((PlanImage)plan).setHasSpecificCalib();
         }
         if( (s=(String)atts.get("flagepoch"))!=null ) {
            if( plan.projd!=null && plan.projd.c!=null ) {
               plan.projd.c.flagepoc=Integer.parseInt(s);
            }
         }
         if( (s=(String)atts.get("system"))!=null ) {
            if( plan.projd!=null && plan.projd.c!=null ) {
               plan.projd.c.system=Integer.parseInt(s);
            }
         }
         if( (s=(String)atts.get("projection"))!=null ) {
            if( plan.projd!=null && plan.projd.c!=null ) {
               plan.projd.c.proj=Calib.getProjType(s);
            }
         }

      }
      return plan;
   }

   /** Parse l'attribut XML de description de la CALIB Aladin et remplit
    * les champs correspondants. Chaque valeur est separee par une simple ","
    * Voir Save.java -> getXMLHeadPlan()
    */
   private void parseCalib(Calib c, String s) {
      StringTokenizer st = new StringTokenizer(s,",");

      c.aladin =Integer.parseInt(st.nextToken());
      c.epoch = Double.valueOf(st.nextToken()).doubleValue();
      c.alpha = Double.valueOf(st.nextToken()).doubleValue();
      c.delta = Double.valueOf(st.nextToken()).doubleValue();
      c.yz = Double.valueOf(st.nextToken()).doubleValue();
      c.xz  = Double.valueOf(st.nextToken()).doubleValue();
      c.focale = Double.valueOf(st.nextToken()).doubleValue();
      c.Xorg = Double.valueOf(st.nextToken()).doubleValue();
      c.Yorg = Double.valueOf(st.nextToken()).doubleValue();
      c.incX = Double.valueOf(st.nextToken()).doubleValue();
      c.incY = Double.valueOf(st.nextToken()).doubleValue();
      c.alphai = Double.valueOf(st.nextToken()).doubleValue();
      c.deltai = Double.valueOf(st.nextToken()).doubleValue();
      c.incA = Double.valueOf(st.nextToken()).doubleValue();
      c.incD = Double.valueOf(st.nextToken()).doubleValue();
      c.Xcen = Double.valueOf(st.nextToken()).doubleValue();
      c.Ycen = Double.valueOf(st.nextToken()).doubleValue();
      c.widtha = Double.valueOf(st.nextToken()).doubleValue();
      c.widthd = Double.valueOf(st.nextToken()).doubleValue();
      c.xnpix = Integer.parseInt(st.nextToken());
      c.ynpix = Integer.parseInt(st.nextToken());
      c.rota = Double.valueOf(st.nextToken()).doubleValue();
      c.cdelz = Double.valueOf(st.nextToken()).doubleValue();
      c.sdelz = Double.valueOf(st.nextToken()).doubleValue();
      c.type1 = st.nextToken();
      c.type2 = st.nextToken();
      try { c.equinox = Double.valueOf(st.nextToken()).doubleValue(); }
      catch( Exception e1 ) { c.equinox=2000.0; }
      try { c.proj = Integer.parseInt(st.nextToken()); }
      catch( Exception e1 ) { c.proj=Calib.SIN; }
   }

   /** Parse un Polynome dans un attribut xml et retourne les coefficients
    * sous la forme d'un tableau de double.
    * Chaque valeur est separee par une simple ","
    * Voir Save.java -> getXMLHeadPlan()
    */
   private double[] parsePoly(String s) {
      StringTokenizer st = new StringTokenizer(s,",");
      double [] x = new double[st.countTokens()];
      for( int i=0; i<x.length; i++ ) {
         x[i] = Double.valueOf(st.nextToken()).doubleValue();
      }
      return x;
   }


   /** Parse une matrice 2x2 dans un attribut xml et retourne les coefficients
    * sous la forme d'un tableau de double.
    * Chaque valeur est separee par une simple ","
    * Voir Save.java -> getXMLHeadPlan()
    */
   private double[][] parseMat(String s) {
      StringTokenizer st = new StringTokenizer(s,",");
      double [][] x = new double[2][2];
      x[0][0] = Double.valueOf(st.nextToken()).doubleValue();
      x[0][1] = Double.valueOf(st.nextToken()).doubleValue();
      x[1][0] = Double.valueOf(st.nextToken()).doubleValue();
      x[1][1] = Double.valueOf(st.nextToken()).doubleValue();
      return x;
   }

   /** Interface XMLConsumer */
   public void startElement(String name, Hashtable atts) {
      if( name.equals("ALADINJAVA") ) aladin.calque.FreeAll();
      else if( name.equals("PLANE") ) plan = creatPlaneByAJ(atts);
      else if( name.equals("TABLE") )  vField = new Vector(10);
      else if( name.equals("SCRIPT") ) inFilterScript =true;
      else if( name.equals("ORIRIGINALHEADERFITS") ) inFitsHeader=true;
      else if( name.equals("FILTER") ) inFilter=true;

      else if( name.equals("FILTERS") ) {
         try {
            nFilter=0;
            plan.filters = new String[Integer.parseInt( (String)atts.get("nFilter"))];
            plan.filterIndex = Integer.parseInt( (String)atts.get("filterIndex"));
         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

      } else if( name.equals("MODEVIEW") ) {
         firstView=0;
         String s;
         try {
            if( (s=(String)atts.get("mode"))!=null )     aladin.view.setModeView(Integer.parseInt(s));
            if( (s=(String)atts.get("position"))!=null ) firstView=Integer.parseInt(s);

            // Pour compatibilité version <7
            if( (s=(String)atts.get("grid"))!=null )     aladin.calque.setOverlayFlag("grid", (new Boolean(s)).booleanValue() );
            if( (s=(String)atts.get("target"))!=null )   aladin.calque.setOverlayFlag("target", (new Boolean(s)).booleanValue() );

            if( (s=(String)atts.get("overlay"))!=null )  aladin.calque.flagOverlay = (new Boolean(s)).booleanValue();
            if( (s=(String)atts.get("overlays"))!=null ) aladin.calque.setOverlayList(s);
         } catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

      } else if( name.equals("VIEW") ) {
         vmi = creatViewMemoItemByAJ(atts);
         String s = (String)atts.get("n");
         try { aladin.view.viewMemo.set(Integer.parseInt(s),vmi); }
         catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }

      } else if( name.equals("VALUE") ) {
         inValue=true;
         if( vField!=null && vField.size()>0 ) leg = new Legende(vField);

         // Description de colonnes
      } else if( name.equals("COLUMN") ) {
         Field f = new Field(atts);
         Enumeration e = atts.keys();
         while( e.hasMoreElements() ) {
            String key = (String)e.nextElement();
            f.addInfo(key,(String)atts.get(key));
         }
         vField.addElement(f);
      }
   }

   private Vector pdf = null;

   /** Interface XMLConsumer */
   public void endElement(String name){
      if( name.equals("PLANE") ) {

         if( plan!=null ) {

            // pour prendre en compte pas la suite les filtres dédiés
            if( plan.filters!=null && plan.filterIndex>=0 ) {
               if( pdf==null ) pdf = new Vector();
               pdf.addElement(plan);
            }

            // Post-traitement sur le plan CATALOG
            //            if( typePlan==Plan.CATALOG || typePlan==CATALOGTOOL ) plan.pcat.postJob(ra,de,rm,true);

            // Post-traitement sur le plan IMAGE
            if( typePlan==Plan.IMAGE || typePlan==Plan.IMAGEMOSAIC
                  || typePlan==Plan.IMAGERSP || typePlan==Plan.IMAGEALGO) {
               // Creation de la table des couleurs
               PlanImage pi = (PlanImage)plan;
               pi.cm=ColorMap.getCM(pi.cmControl[0],pi.cmControl[1],pi.cmControl[2],
                     pi.video==PlanImage.VIDEO_INVERSE,
                     pi.typeCM,
                     pi.transfertFct);
               pi.calculPixelsZoom();
               pi.changeImgID();
            }

            // validation et memorisation
            plan.flagOk = true;
            aladin.calque.addOnStack(plan);
            plan=null;
            flagCatalogSource=false;
         }

      } else if( name.equals("MODEVIEW") )  {
         aladin.view.scrollOn(firstView,0,1);
      } else if( name.equals("SCRIPT") )  { inFilterScript =false;
      } else if( name.equals("TABLE") )   { leg=null; vField=null; if( flagCatalogSource ) typePlan=CATALOGTOOL;
      } else if( name.equals("VALUE") )   { inValue=false;
      } else if( name.equals("ALADINJAVA") ) {

         // mise à jour des différents filtres mémorisés
         if( pFilter!= null ) {
            Enumeration<PlanFilter> filters = pFilter.keys();
            PlanFilter pf;
            while( filters.hasMoreElements() ) {
               pf = filters.nextElement();
               pf.updateDefinition((String)pFilter.get(pf), pf.label, null);
               pf.updateState();
            }

            // un peu bourrin, mais c'était le plus facile
            new Thread("AladinFilterUpdate") {
               @Override
               public void run() {
                  try { Thread.currentThread().sleep(1000); }
                  catch(Exception e) {e.printStackTrace();}
                  PlanFilter.updateAllFilters(aladin);
                  final Enumeration<PlanFilter> filters = pFilter.keys();
                  while( filters.hasMoreElements() ) {
                     final PlanFilter pf = filters.nextElement();
                     pf.setMustUpdate();
                     pf.doApplyFilter();

                  }
                  aladin.calque.select.repaint();
                  pFilter.clear();
               }
            }.start();
         }

         // mise à jour des différents filtres dédiés mémorisés
         if( pdf!=null ) {
            new Thread("AladinFilterUpdate2") {
               @Override
               public void run() {
                  try { Thread.currentThread().sleep(1200); }
                  catch(Exception e) {e.printStackTrace();}
                  Enumeration<Plan> e = pdf.elements();
                  while( e.hasMoreElements() ) {
                     Plan p = e.nextElement();
                     p.setFilter(p.filterIndex);
                  }
                  aladin.calque.select.repaint();
                  pdf=null;
               }
            }.start();
         }

      }
   }

   /** Remplit le champ courant de la source courante
    * Met a jour la variable de classe rec
    * @param ch le tableau de caracteres a analyser
    * @param cur l'indice du caractere courant
    * @param end le dernier indice valide
    * @return la nouvelle position dans ch[]
    */
   private int getSourceField(char [] ch, int cur, int end) {
      int start=cur;
      while( cur<end && ch[cur]!='\t' && ch[cur]!='\n' ) cur++;
      rec = new String(ch,start,cur-start).trim();

      return ch[cur]=='\t'?cur+1:cur;
   }

   /** Cherche les infos de la source courante et ajoute l'objet correspondant
    * au plan courant
    * utilise la variable de classe rec
    * @param ch le tableau de caracteres a analyser
    * @param cur l'indice du caractere courant
    * @param end le dernier indice valide
    * @return la nouvelle position dans ch[]
    */
   private int getSource(char [] ch, int cur, int end) {
      double ra,de;
      String id;

      // On skippe un eventuel \r
      while( cur<end && (ch[cur]=='\r' || ch[cur]=='\n') ) cur++;

      // Recuperation des positions et de l'ID
      cur=getSourceField(ch,cur,end);	ra=Double.valueOf(rec).doubleValue();
      cur=getSourceField(ch,cur,end);	de=Double.valueOf(rec).doubleValue();
      cur=getSourceField(ch,cur,end);	id=rec;

      // Recuperation des infos
      int start=cur;
      while( cur<end && ch[cur]!='\n'  ) cur++;
      // modif Thomas (le trim() supprimait la valeur de certains champs "blancs" situés en bout de ligne)
      rec = new String(ch,start,cur-start);
      //rec = new String(ch,start,cur-start).trim();

      // Ajout de l'objet dans le plan courant
      Source o = (leg!=null)?new Source(plan,ra,de,id,rec,leg):
         new Source(plan,ra,de,id,rec);

      // Cas particulier de sources dans un plan tool
      if( leg!=null && typePlan==CATALOGTOOL && ((PlanTool)plan).legPhot==null ) ((PlanTool)plan).legPhot=leg;

      plan.pcat.setObjetFast(o);

      return cur;
   }

   /** Cherche les infos du tool courante et ajoute l'objet correspondant
    * au plan courant. Gere les suivis de tool (genre polylignes)
    * Utilise la variable de classe rec
    * @param ch le tableau de caracteres a analyser
    * @param cur l'indice du caractere courant
    * @param end le dernier indice valide
    * @return la nouvelle position dans ch[]
    */
   private int getTool(char [] ch, int cur, int end) {
      double ra,de;
      int x,y;
      boolean flagSuite=false;
      boolean flagSpecial=false;
      boolean withlabel=false;
      String typeTool=null;
      String id;

      // On skippe un eventuel \r
      while( cur<end && (ch[cur]=='\r' || ch[cur]=='\n') ) cur++;

      // Recuperation du type, de l'info sur le suivant, de la position
      // en ra,de et en x,y ainsi que l'identificateur
      cur=getSourceField(ch,cur,end);	typeTool=rec;
      cur=getSourceField(ch,cur,end);	flagSuite=rec.equals("+"); flagSpecial=rec.equals("*");
      cur=getSourceField(ch,cur,end);	try{ ra=Double.valueOf(rec).doubleValue(); } catch( Exception e) { ra=0; }
      cur=getSourceField(ch,cur,end);	try{ de=Double.valueOf(rec).doubleValue(); } catch( Exception e) { de=0; }
      cur=getSourceField(ch,cur,end);	try{ x=Integer.parseInt(rec); } catch( Exception e) { x=0; }
      cur=getSourceField(ch,cur,end);	try{ y=Integer.parseInt(rec); } catch( Exception e) { y=0; }
      cur=getSourceField(ch,cur,end);	withlabel=(new Boolean(rec)).booleanValue();
      cur=getSourceField(ch,cur,end);	id=rec;

      // Ajout de l'objet dans le plan courant
      Position o=null;
      if( typeTool.equals("tag") )       o = ( new Repere(plan) );   // Pour compatibilité avec les versions <7
      else if( typeTool.equals("text") )      o = ( new Tag(plan) );      // Pour compatibilité avec les versions <7

      else if( typeTool.equals("phot") )      o = ( new Repere(plan) );
      else if( typeTool.equals("taglabel") )  o = ( new Tag(plan) );
      else if( typeTool.equals("line") )      o = ( new Ligne(plan) );
      else if( typeTool.equals("arrow") )     o = ( new Cote(plan) );
      else if( typeTool.equals("circle") ) {
         if( circleCenter!=null ) {
            double r = de-circleCenter.del;
            o = new Cercle(plan, circleCenter, r);
            circleCenter = null;
         }
         else {
            circleCenter = new Coord(ra, de);
         }
      }
      if( o!=null ) {
         o.raj=ra; o.dej=de;
         o.x=x;    o.y=y;
         o.setWithLabel(withlabel);
         o.setSpecificAJInfo(id);

         // Suivi de ligne
         try {
            if( prevFlagSuite ) {
               ((Ligne)prevO).finligne = (Ligne)o;
               ((Ligne)o).debligne = (Ligne)prevO;
            }
         } catch( Exception e ) { }

         // Fin d'un polygone
         try {
            if( flagSpecial ) ((Ligne)o).bout=3;
         } catch( Exception e ) { }

         // Enregistrement de l'objet dans le plan
         plan.pcat.setObjetFast(o);

         // memorisation en cas de suivi
         prevO = o;
         prevFlagSuite = flagSuite;
      }

      return cur;
   }

   /** Cherche les pixels RGB de l'image et l'associe au plan courant.
    * Cette procedure peut etre appelee plusieurs fois en sequence.
    * @param ch le tableau de caracteres a analyser
    * @param cur l'indice du caractere courant
    * @param end le dernier indice valide
    */
   private void getImageRGB(char [] ch, int start, int length) {
      PlanImageRGB p = (PlanImageRGB)plan;

      // Premier bloc de pixels => initialisations
      if( rgb==null ) {
         rgb = new byte[p.width * p.height * 3];
         pOffset=0;
      }

      // Traitement du bloc courant
      pOffset=Save.get64(rgb,pOffset,ch,start,length);

      // C'est fini ?
      if( pOffset==p.width*p.height*3 ) {
         p.setByteRGB(rgb);
         p.cm = ColorModel.getRGBdefault();
         for( int i=0; i<3; i++ ) p.filterRGB(p.cmControl, i);
         p.flagRed=p.flagGreen=p.flagBlue=true;
         rgb=null;
         pOffset=0;
      }
   }

   byte [] rgb=null;
   int pOffset=0;   // offset courant dans p.pixels (bloc/bloc)

   /** Cherche les pixels de l'image et l'associe au plan courant.
    * Cette procedure peut etre appelee plusieurs fois en sequence.
    * @param ch le tableau de caracteres a analyser
    * @param cur l'indice du caractere courant
    * @param end le dernier indice valide
    */
   private void getImage(char [] ch, int start, int length) {
      PlanImage p = (PlanImage)plan;

      // Premier bloc de pixels => initialisations
      if( p.getBufPixels8()==null ) {
         p.setBufPixels8(new byte[p.width * p.height]);
         pOffset=0;
      }

      // Traitement du bloc courant
      pOffset=Save.get64(p.getBufPixels8(),pOffset,ch,start,length);
   }

   // mémoire des PlanFilter à mettre à jour et des définitions correspondantes (clé : PlanFilter, value : définition)
   Hashtable pFilter;
   /** Mise en place du script sur le plan filtre en cours de construction */
   private void setScript(String script) {
      // décodage des entités spéciales
      // PF SEPT 07 - JE COMMENTE PARCE QUE CELA A DEJA ETE APPLIQUE EN AMONT
      //   	  script = XMLParser.XMLDecode(script);

      if( pFilter==null ) {
         pFilter = new Hashtable();
      }

      plan.setActivated();

      // mémorisation du PlanFilter et de la déf. associée
      pFilter.put(plan, script);

   }

   // Type de plan modifié pour prendre en compte les catalogues inclues dans les tools
   static final int CATALOGTOOL = 1000;

   /** Interface XMLConsumer */
   public void characters(char ch[], int start, int length){
      int cur = start;		// Current character
      int end = start+length;	// Last character

      if( inValue ) {
         switch( typePlan ) {
            case Plan.CATALOG:
            case CATALOGTOOL:
               while( cur<end ) {
                  cur = getSource(ch,cur,end);
                  cur++;
               }
               break;
            case Plan.TOOL:
               while( cur<end ) {
                  cur = getTool(ch,cur,end);
                  cur++;
               }
               prevFlagSuite=false;	// pour ne pas accoler deux objets
               break;
            case Plan.IMAGE:
            case Plan.IMAGEHUGE:
            case Plan.IMAGEMOSAIC:
            case Plan.IMAGERSP:
            case Plan.IMAGEALGO:
               getImage(ch,start,length);
               break;
            case Plan.IMAGERGB:
               getImageRGB(ch,start,length);
               break;
         }
      } else if( inFilterScript ) {
         // thomas, 22/11/06 : les sauts de ligne ont été remplacés par un "\n" dans le .aj
         //Il faut donc les décoder
         setScript(new String(ch,start,length).replaceAll("\\\\n", "\n"));
      } else if( inFitsHeader ) {
         if( ((PlanImage)plan).headerFits==null ) {
            ((PlanImage)plan).headerFits = new FrameHeaderFits(new String(ch,start,length));
         } else ((PlanImage)plan).headerFits.setOriginalHeaderFits(new String(ch,start,length));
      } else if( inFilter ) {
         if( plan.filters==null ) return;
         plan.filters[nFilter++] = new String(ch,start,length);
      }
   }
}
