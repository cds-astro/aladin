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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.io.OutputStream;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import cds.fits.HeaderFits;
import cds.tools.Util;

/**
 * Classe dediee a la gestion d'un header FITS.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.6 : D�c 2007 : Possibilit� de surcharger les mots cl�s
 * @version 1.5 : 20 aout 2002 methode readFreeHeader
 * @version 1.4 : 19 juin 00 Utilisation du PushbackInputStream et
 *                implantation de isHCOMP()
 * @version 1.3 : (6 juin 2000) format HCOMPRESS
 * @version 1.2 : (20 mars 2000) prise en compte du champ EQUINOX enquote
 * @version 1.1 : (14 jan 99) affichage du header fits dans un frame
 * @version 0.9 : (18 mai 99) Creation
 */
public class FrameHeaderFits extends JFrame {
   private StringBuffer	memoHeaderFits = null;	// Memorisation de l'entete FITS telle quelle (en Strings)
   private JTextPane ta;
   private JTextField ts;
   static String CLOSE,CLEAR,SAVE,CANCEL,SAVEINFO;
   private DefaultStyledDocument df;
   private JButton clear,cancel,save;
   private static final boolean SAVABLE=true;
   private Plan plan;

   protected HeaderFits headerFits;

   static void createChaine(Chaine chaine) {
      SAVE = chaine.getString("HSAVE");
      CANCEL = chaine.getString("HUNDO");
      CLOSE = chaine.getString("CLOSE");
      CLEAR = chaine.getString("CLEAR");
      SAVEINFO = chaine.getString("HSAVEINFO");
   }

   /** Cr�ation du header Fits � partir de rien */
   public FrameHeaderFits() {
      headerFits = new HeaderFits();
   }

   public FrameHeaderFits(HeaderFits headerFits) {
      this.headerFits = headerFits;
   }

  /** Creation du header.
   * @param dis le flux en entree
   */
   protected FrameHeaderFits(Plan plan,MyInputStream dis) throws Exception {
      super("FITS header");
      this.plan=plan;
      Aladin.setIcon(this);
      makeTA();
      headerFits = new HeaderFits();
      headerFits.readHeader(dis,this);
   }

  /** Creation du header a partir d'un chaine de caracteres. Celle-ci
   * peut etre une entete FITS "valide" (bloc de 2880, 80 colonnes...)
   * ou un simple paragraphe KEY = VALUE /COMMENT\n...
   */
   protected FrameHeaderFits(Plan plan,String s) { this(plan,s,false); }
   protected FrameHeaderFits(Plan plan,String s,boolean specialHHH) {
      Aladin.setIcon(this);
      this.plan=plan;
      makeTA();
      headerFits = new HeaderFits();
      headerFits.readFreeHeader(s,specialHHH,this);
   }
   
   protected void free() { dispose(); }
   
   /** Retourne l'objet de manipulation de l'ent�te Fits */
   protected HeaderFits getHeaderFits() { return headerFits; }

   static private SimpleAttributeSet atKey=null,atValue,atComment,atCom,atHist,atYellow,atWhite;
   private boolean first=true;  // Pour ne faire la mise en forme compl�te qu'une seule fois

   /** Mise en forme du texte de l'ent�te fits avec surlignage �ventuel d'un mot */
   private void search(String key) {
      if( atKey==null ) {
         atKey = new SimpleAttributeSet();
         atKey.addAttribute(StyleConstants.CharacterConstants.Foreground,Color.blue);
         atKey.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
         atValue = new SimpleAttributeSet();
         atValue.addAttribute(StyleConstants.CharacterConstants.Foreground,Color.black);
         atValue.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
         atComment = new SimpleAttributeSet();
         atComment.addAttribute(StyleConstants.CharacterConstants.Foreground,Aladin.DARKBLUE);
         atComment.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
         atCom = new SimpleAttributeSet();
         atCom.addAttribute(StyleConstants.CharacterConstants.Foreground,Aladin.GREEN);
         atCom.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
         atHist = new SimpleAttributeSet();
         atHist.addAttribute(StyleConstants.CharacterConstants.Foreground,new Color(127,0,85));
         atHist.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
         atWhite = new SimpleAttributeSet();
         atWhite.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
         atYellow = new SimpleAttributeSet();
         atYellow.addAttribute(StyleConstants.CharacterConstants.Background,Color.yellow);
      }
      String s = ta.getText();
      int pos;

      // Mise en forme de base (uniquement sur les couleurs des lettres)
      if( first && s.length()<64*1024 ) {
         first =false;
         int opos=0;
         while( (pos=s.indexOf("\n",opos))>=0 ) {
            String k="";
            if( opos+7<s.length() ) k=s.substring(opos,opos+8).trim();
            if(  k.equals("HISTORY")  ) df.setCharacterAttributes(opos,pos,atHist,true);
            else if( k.startsWith("/")  || k.equals("COMMENT") ) df.setCharacterAttributes(opos,pos,atComment,true);
            else {
               df.setCharacterAttributes(opos,opos+8,atKey,true);
               df.setCharacterAttributes(opos+9,pos,atValue,true);
               int com = s.indexOf('/',opos+30);
               if( com>=0 ) df.setCharacterAttributes(com,pos,atCom,true);
            }
            opos=pos+1;
         }

      // Sinon simple remise en blanc du fond
      } else {
         df.setCharacterAttributes(0,s.length()-1,atWhite,false);
      }
      if( key.length()==0 ) { clear.setEnabled(false); return; }
      clear.setEnabled(true);

      // Surlignage en jaune de la chaine recherch�e
      pos = -1;
      while( (pos=s.indexOf(key,pos+1))>=0 ) {
         df.setCharacterAttributes(pos,key.length(),atYellow,false);
      }

      // Surlignage en jaune de la chaine recherch�e
      // et positionnement � la premi�re occurence
      pos = -1;
      int firstPos=-1;
      while( (pos=s.indexOf(key,pos+1))>=0 ) {
         df.setCharacterAttributes(pos,key.length(),atYellow,false);
         if( firstPos==-1 ) firstPos=pos;
      }
      if( firstPos!=-1 ) ta.setCaretPosition(firstPos);

   }
   
   boolean isEdited=false;
   
   private String originalHeader = null;
   protected void save() {
      String s = ta.getText();
      try {
         headerFits = new HeaderFits(s);
         if( plan!=null ) {
            Calib c = new Calib(headerFits);
            plan.projd=new Projection(Projection.WCS, c);
            plan.setHasSpecificCalib();
         }
      } catch( Exception e ) {
         Aladin.warning(this,"Not a valid FITS header: "+e.getMessage());
         return;
      }
      
      memoHeaderFits = new StringBuffer( s );
      makeTA();
      if( !Aladin.confirmation(this,SAVEINFO ) ) return;
      plan.aladin.save(plan.aladin.EXPORT);
   }
   
   protected void cancel() {
      memoHeaderFits = new StringBuffer( originalHeader );
      isEdited=false;
      ta.setText(getOriginalHeaderFits());
   }
   
   private void updateWidgets() {
      if( originalHeader==null ) return;
      isEdited=true;
      cancel.setEnabled(!originalHeader.equals(ta.getText()));
      save.setEnabled( !originalHeader.equals(ta.getText()));
   }

  /** Construction du Frame de visualisation du Header FITS */
   public void makeTA() {
      JButton b;

      df=new DefaultStyledDocument() ;
      ta = new JTextPane(df);
      ta.getDocument().addDocumentListener(new DocumentListener() {
         public void removeUpdate(DocumentEvent e)  { updateWidgets(); }
         public void insertUpdate(DocumentEvent e)  { updateWidgets(); }
         public void changedUpdate(DocumentEvent e) { updateWidgets(); }
      });

      ta.setFont( Aladin.COURIER );
      ta.setEditable(SAVABLE); 

      JScrollPane sc = new JScrollPane(ta);
      sc.setPreferredSize(new Dimension(600,600));
      JPanel p = new JPanel();
      ts = new JTextField(10);
      ts.addKeyListener(new KeyAdapter() {
         public void keyReleased(KeyEvent e) {
            String s=((JTextField)e.getSource()).getText();
            search(s);
         }
      });
      try {
         p.add(Aladin.createLabel(Aladin.aladin.chaine.getString("MFSEARCHL")));
      } catch( Exception e1 ) {
         p.add(Aladin.createLabel("Search"));
      }
      p.add(ts);
      clear = b =  new JButton(CLEAR);
      b.setEnabled(false);
      p.add(b);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { ts.setText("");  search(""); }
      });
      p.add(new JLabel(" - "));
      save = b =  new JButton(SAVE);
      b.setEnabled(false);
      if( SAVABLE ) p.add(b);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { save(); }
      });
      cancel = b =  new JButton(CANCEL);
      b.setEnabled(false);
      if( SAVABLE ) p.add(b);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { cancel(); }
      });

      b =  new JButton(CLOSE);
      p.add(b);
      b.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) { setVisible(false); }
      });
      Aladin.makeAdd(this,sc,"Center");
      Aladin.makeAdd(this,p,"South");
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false, null);
      setLocation(Aladin.computeLocation(this));
   }

   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) setVisible(false);
      super.processWindowEvent(e);
   }

//  /** Retourne Vrai s'il s'agit d'un FITS Hcompresse */
//   protected boolean isHCOMP() { return flagHCOMP; }

  /** Visualise le header FITS */
   protected void seeHeaderFits() {
      ta.setText(getOriginalHeaderFits());
      originalHeader = ta.getText();  // Pour pouvoir annuler une �dition
      isEdited=false;
      ta.setCaretPosition(0); // on se positionne au d�but du header
      ts.requestFocusInWindow();
      first=true;
      search("");
      pack();
      setVisible(true);
   }


  /** Retourne le header FITS original (en Strings) */
   protected String getOriginalHeaderFits() { return memoHeaderFits.toString(); }
   
   /** M�morise le header FITS original (en Strings) */
   protected void setOriginalHeaderFits(String s) { memoHeaderFits= new StringBuffer(s); }

  /** Ajoute la ligne courante a la memorisation du header FITS
   * en supprimant les blancs en fin de ligne
   * @param s la chaine a ajouter
   */
   public void appendMHF(String s) {
      if( memoHeaderFits==null ) memoHeaderFits=new StringBuffer();
      memoHeaderFits.append(s.trim()+"\n");
   }

  /** Taille en octets de l'entete FITS.
   * Uniquemenent mis a jour apres readHeader()
   * @return La taille de l'entete
   */
   protected int getSizeHeader() { return headerFits.getSizeHeader(); }

   /**
    * Teste si un mot cl� est pr�sent dans l'ent�te
    * @param key la cl� � tester
    * @return true si la cl� est pr�sente
    */
   protected boolean hasKey(String key) { return headerFits.hasKey(key); }


  /** Recherche d'un element entier par son mot cle
   * @param key le mot cle  (inutile de l'aligner en 8 caract�res)
   * @return la valeur recherchee
   */
   protected int getIntFromHeader(String key)
                 throws NumberFormatException,NullPointerException {
      return headerFits.getIntFromHeader(key);
   }


   /** Surcharge ou ajout d'un mot cl� */
   protected void setKeyword(String key,String value) {
      headerFits.setKeyword(key,value);
   }

  /** Recherche d'un element double par son mot cle
   * @param key le mot cle (inutile de l'aligner en 8 caract�res)
   * @return la valeur recherchee
   */
   protected double getDoubleFromHeader(String key)
                 throws NumberFormatException,NullPointerException {
      return headerFits.getDoubleFromHeader(key);
   }

  /** Recherche d'une chaine par son mot cle
   * @param key le mot cle  (inutile de l'aligner en 8 caract�res)
   * @return la valeur recherchee
   */
   protected String getStringFromHeader(String key)
                 throws NullPointerException {
      return headerFits.getStringFromHeader(key);
   }

   /** Ajout, surcharge ou suppression d'un mot cle
    * @param key le mot cl� (inutile de l'aligner en 8 caract�res)
    * @param value la valeur � positionner, null si suppression
    */
   protected void setToHeader(String key,String value) {
      headerFits.setToHeader(key,value);
   }

   /** Ajoute/remplace/supprime un couple (MOTCLE,VALEUR) - l'ordre des mots cl�s
    * est m�moris� dans keysOrder, et les valeurs sont stock�es dans header
    * Ra : VALEUR=null signifie une suppression */
   protected void setKeyValue(String key, String value) {
      headerFits.setKeyValue(key,value);
   }

   /** Ecriture de l'ent�te FITS des mots cl�s m�moris�s. L'ordre est conserv�
    * comme � l'origine - les commentaires ne sont pas restitu�s */
   protected void writeHeader(OutputStream os ) throws Exception {
      headerFits.writeHeader(os);
   }
}
