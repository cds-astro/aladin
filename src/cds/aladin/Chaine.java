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

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import cds.tools.Util;

/** Classe gerant les chaines de caract�res qui sont relativement peu utilisees
 *
 * Le but est d'all�ger la taille de l'appli. (surtout de l'applet)
 * et de supporter facilement les traductions
 *
 * On utilisera Chaine.getString("key") pour r�cup�rer la chaine dont la cl� est key
 * Au premier appel de cette m�thode, le fichier (zipp�) contenant la liste des chaines est charg�
 * et sauvegard� dans une Hashtable. Ce fichier soit se trouver dans le PATH de l'appli/applet
 *
 * @author T. Boch [CDS] + P. Fernque [CDS]
 * @version 1.3 (f�vrier 2007 ) support fichiers additionnels
 * @version 1.2 (f�vrier 2006 ) support multilangues
 * @version 1.1 (3 decembre 2003) Prise en compte des NL
 *                                et de l'alternative GZIP ou non (P.FERNIQUE)
 * @version 1.0 (4 novembre 2003) Creation
 */
public class Chaine {
    // nom des fichiers contenant laes listes des key<tab>value
   private static String STRINGFILE   = "Aladin.string";     // Chaines rares
   private static String STRINGFILE0  = "Aladin0.string";   // Chaines fr�quentes
//   private static String STRINGFILE0u = "Aladin0.string.utf";   // Chaines fr�quentes en unicode
   
   private boolean flagAll=false;		// true si toutes les chaines ont �t� charg�es

    static protected Hashtable map;
    private Aladin aladin;
    
    private String lastFileName=null;  // Dernier fichier de traduction install�    
    
    Chaine(Aladin aladin) {
       this.aladin = aladin;   
       if( map==null ) {
          map = new Hashtable(4000);
          // Pour v�rifier la compl�tude de la traduction
          loadDefList=true;
          defList = new ArrayList(1500);
          loadFile(STRINGFILE0);
          loadDefList=false;
//          try { loadFile(STRINGFILE0u); } catch( Exception e ) {}
          loadAddFiles();
       }

       createChaine();
    }
    
    /** Pour cr�er les chaines de certains objets qui posent probl�me */
    protected void createChaine() {
       Pcat.createChaine(this);
       Plan.createChaine(this);
       FrameInfo.createChaine(this);
       PlanImage.createChaine(this);
       ServerSkybot.createChaine(this);
       FrameHeaderFits.createChaine(this);
    }
    
    /**
     * Remplace les sequence '\' 'n' par '\n'
     */
    static private String replaceNL(String s) {
       char a[] = s.toCharArray();
       int i,j;
       boolean slash=false;

       for( j=i=0; i<a.length; i++,j++ ) {
          char c = a[i];
          if( slash && c=='n' ) { j--; c='\n'; }
          slash=c=='\\';
          if( i!=j ) a[j]=c;
       }

       return i==j?s:new String(a,0,j);
    }
    
    /**
     * Remplace '\n' par les sequence  '\' 'n'
     */
   static private String reverseNL(String s) {
      int n = s.length();
      StringBuffer b = new StringBuffer(n+10);
      for( int i=0; i<n; i++ ) {
         char c = s.charAt(i);
         if( c=='\n' ) { b.append('\\'); b.append('n'); }
         else b.append(c);
      }
      return b.toString();
    }
    
    protected void loadFile(String s) {
       long t = System.currentTimeMillis();
       MyInputStream is=null;
       try{
          InputStream in = getClass().getResourceAsStream("/"+s);
          if( in==null ) throw new Exception();
          is = new MyInputStream(in);
          is = is.startRead();
        } catch( Exception e ) {
          try {
             InputStream in =new FileInputStream(new File(s));
             if( in==null ) throw new Exception();
             is = new MyInputStream( in);
          } catch ( Exception e1 ) {
             try {
                String x = Aladin.aladin.getCodeBase()+s;
//                InputStream in =new java.net.URL(x).openStream();
//                if( in==null ) throw new Exception();
//                is = new MyInputStream( in);
                is = Util.openStream(x);
             } catch (Exception e2 ) {
                e1.printStackTrace();
                e2.printStackTrace();
                return;
             }
          }
       }
       try {
           BufferedReader dis = s.indexOf(".utf")>=0 ?
                         new BufferedReader(new InputStreamReader(is,"UTF8"))
                        :new BufferedReader(new InputStreamReader(is,"iso-8859-1"));
                         
           parseStringFile(dis);
           dis.close();
           Aladin.trace(1,"String file "+s+" loaded ("+(System.currentTimeMillis()-t)+"ms)");
       }
       
       // Si on n'a pas pu trouver le fichier UCD la premi�re fois, on n'insistera pas
       catch( Exception e) {
           if( aladin.levelTrace>=3 ) e.printStackTrace();
           Aladin.trace(1,"Could not load strings file "+s);
           return ;
       }      
    }
    
    /** Retourne l'indice du premier caract�re de la chaine de texte suivant
     * le format:
     * KEYWORD  texte...
     * @param s la chaine pr�c�d� de son mot cl�
     * @return l'indice sinon -1
     */
    static private int getStringIndex(String s) {
       int i=0;
       int n = s.length();
       while( i<n && !Character.isSpace(s.charAt(i)) ) i++;
       while( i<n && Character.isSpace(s.charAt(i)) ) i++;
       return i>0 && i<n?i:-1;
    }
    
    /** Parsing d'un flux contenant des chaines pour une langue support�e
     * par Aladin */
    private void parseStringFile(BufferedReader dis) throws IOException {
       String line;
       int end,i;
       while( (line = dis.readLine()) != null ) {
           if( line.trim().length()>0 && line.charAt(0)=='#' ) continue;
           end = getStringIndex(line);
           if( end<0 ) continue;
           String key = line.substring(0,end-1).trim();
           String valeur = replaceNL(line.substring(end));
           if( key.equals("LANGUAGE") ) {
              aladin.configuration.setLanguage(valeur);
              continue;
           }
           
           if( key.length()==0 ) continue;      
           map.put(key, valeur);
           
           // Si on doit v�rifier une compl�tude de traduction,
           // je charge les mots cl�s anglais de Aladin0.string
           // pour en conserver l'ordre
           if( loadDefList && key.indexOf(".")<0 ) {              
              defList.add(key);
           }
       }
    }
    
    private ArrayList defList = null;
    private boolean loadDefList=false;
    
    /** Proc�dure pour v�rifier la compl�tude d'une traduction */
    protected void testLanguage(String param) {
       String suf = "";
       String lang=null;
       
       if( param==null || param.trim().length()==0 ) {
          suf = aladin.configuration.getLang();
          if( suf.length()>0 ) lang=aladin.configuration.getLanguage(suf.substring(1));
          
       } else {
          try {
             int offset = param.indexOf(' ');
             if( offset<0 ) offset=param.length();
             suf = param.substring(0,offset).trim();
             if( suf.length()==2 ) suf="."+suf;
             else suf="";
             if( offset!=param.length() ) lang = param.substring(offset).trim();
          } catch( Exception e) { suf=""; }
       }
       
       if( suf.length()==0 ) {
          aladin.warning("Missing parameters !");
          return;
       }
       testLanguage(suf,lang);
    }
    
    
    /** Mise en forme du texte de l'ent�te fits avec surlignage �ventuel d'un mot */
    private void search(String key) {
       if( atKey==null ) {
          atKey = new SimpleAttributeSet();
          atKey.addAttribute(StyleConstants.CharacterConstants.Foreground,Color.blue);
          atKey.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
          atValue = new SimpleAttributeSet();
          atValue.addAttribute(StyleConstants.CharacterConstants.Foreground,Color.black);
          atValue.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
          atCom = new SimpleAttributeSet();
          atCom.addAttribute(StyleConstants.CharacterConstants.Foreground,Aladin.GREEN);
          atCom.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
          atLang = new SimpleAttributeSet();
          atLang.addAttribute(StyleConstants.CharacterConstants.Foreground,new Color(127,0,85));
          atLang.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
          atHist = new SimpleAttributeSet();
          atHist.addAttribute(StyleConstants.CharacterConstants.Foreground,Color.red);
          atHist.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
          atWhite = new SimpleAttributeSet();
          atWhite.addAttribute(StyleConstants.CharacterConstants.Background,Color.white);
          atYellow = new SimpleAttributeSet();
          atYellow.addAttribute(StyleConstants.CharacterConstants.Background,Color.yellow);
       }
       String s = ta.getText();
       int pos;
       
       // Mise en forme de base (uniquement sur les couleurs des lettres)
       if( first ) {
          first =false;
          int opos=0;
          while( (pos=s.indexOf("\n",opos))>=0 ) {
             if( s.charAt(opos)=='#' ) {
                df.setCharacterAttributes(opos,pos-opos,atCom,true);
             } else {
                int valOffset = s.indexOf(' ',opos+1);
                if( s.substring(opos,valOffset).equals("LANGUAGE") ) {
                   df.setCharacterAttributes(opos,pos-opos,atLang,true);
                } else {
                   df.setCharacterAttributes(opos,valOffset-opos,atKey,true);
                   if( s.charAt(valOffset+1)=='[' && s.charAt(pos-1)==']' ) {
                      df.setCharacterAttributes(valOffset,pos-valOffset,atHist,true);
                   } else  df.setCharacterAttributes(valOffset,pos-valOffset,atValue,true);
                }
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
       // et positionnement � la premi�re occurence
       pos = -1;
       int firstPos=-1;
       while( (pos=s.indexOf(key,pos+1))>=0 ) {
          df.setCharacterAttributes(pos,key.length(),atYellow,false);
          if( firstPos==-1 ) firstPos=pos;
       }
       if( firstPos!=-1 ) ta.setCaretPosition(firstPos);
    }
    
    static private SimpleAttributeSet atKey=null,atValue,atLang,atCom,atHist,atYellow,atWhite;
    private boolean first=true;  // Pour ne faire la mise en forme compl�te qu'une seule fois
    private JButton clear;
    private JFrame frame;
    
    private DefaultStyledDocument df;
    JTextPane ta;
    JTextField ts;
    
    /** Construction du Panel du texte des traductions */
    private JPanel makePanelTranslation() {
       JButton b;
       
       df=new DefaultStyledDocument() ;
       ta = new JTextPane(df);
       ta.setFont( Aladin.COURIER );
      
       JScrollPane sc = new JScrollPane(ta);
       sc.setPreferredSize(new Dimension(700,800));
       JPanel pa = new JPanel(new BorderLayout());
       pa.add(sc,BorderLayout.CENTER);
       JPanel p = new JPanel();
       ts = new JTextField(10);
       ts.addKeyListener(new KeyAdapter() {
          public void keyReleased(KeyEvent e) {
             String s=((JTextField)e.getSource()).getText();
             search(s);
          }
       });
       p.add(Aladin.createLabel("Search"));
       p.add(ts);
       clear = b =  new JButton("Clear");
       b.setEnabled(false);
       p.add(b);
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { ts.setText("");  search(""); }
       });
       p.add(new JLabel(""));
       b = new JButton("Install");
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { 
             installTranslation();
          }
       });
       p.add(b);
       
       b = new JButton("Send to Aladin team...");
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { 
             try {
             aladin.info(frame,"If you agree for submitting your translation file " +
                    "for an official distribution, please " +
                    "after installing and testing your work, attach the following file " +
                    "to a mail address to:\n \n" +
                    "Email: language@aladin.u-strasbg.fr\n" + 
                    "File : "+getTranslationFullName()+"\n \n" +
                    "Please, do not forget to specify in this mail " +
                    "your name and quality\n" +
                    "Thanks for your contribution !");
             } catch( Exception e1 ) { e1.printStackTrace(); }
          }
       });
       p.add(b);

       b =  new JButton("Close");
       p.add(b);
       b.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) { frame.dispose(); }
       });
       pa.add(p,BorderLayout.SOUTH);
       return pa;
    }
    
    /** R�cup�re le nom complet du fichier de traduction perso */
    private String getTranslationFullName() throws Exception {
       String s = ta.getText();
       int pos = s.indexOf("\nLANGUAGE");
       String langue = s.substring(pos+10,s.indexOf('(',pos)-1).trim();
       boolean flagUTF = testUTF(s);
       String filename=getTranslationFileName(langue,flagUTF);
       String dir = System.getProperty("user.home")+Util.FS+Aladin.CACHE;
       String fullName = dir+Util.FS+filename;
       return fullName;
    }
    
    /** Construit le nom du fichier de traduction perso (sans le path) */
    private String getTranslationFileName(String langue,boolean flagUTF) {
       
       // On enl�ve les blancs et on ne met en majuscules que la premi�re
       // lettre de chaque mot
       StringBuffer s = new StringBuffer(50);
       char a[] = langue.toCharArray();
       boolean first=true;
       for( int i=0; i<a.length; i++ ) {
          char c = a[i];
          if( Character.isSpaceChar(c) ) { first=true; continue; }
          if( first ) c = Character.toUpperCase(c);
          else c= Character.toLowerCase(c);
          first=false;
          s.append(c);
       }
       
       return "Aladin-"+s.toString()+"-"+Aladin.VERSION.substring(1)
               +"-perso.string" + (flagUTF ? ".utf" : "");

    }
    
    /** G�n�ration d'une fen�tre pour �diter une traduction
     * @param suf code deux lettre pr�c�d�e d'un '.' de la langue
     * @param lang Nom de la langue en anglais
     */
   protected void testLanguage(String suf,String lang) {
       if( defList==null ) return;
       boolean first1=true,first2=true,first3=true;
       if( suf.length()>0 ) {
          
          frame = new JFrame("Translation");
          Aladin.setIcon(frame);
          StringBuffer text = new StringBuffer(8000);
          frame.getContentPane().add(makePanelTranslation());
          
          if( lang==null || lang.length()==0 ) lang="Unknown";
          String author=aladin.configuration.getLanguageAuthor(suf.substring(1));
          if( author==null ) author="YourName";
          text.append(
                "# TRANSLATION PROCEDURE:\n" +
                "# 0) Complete LANGUAGE line if necessary [in purple] (language name + author name)\n" +
                "# 1) Translate missing sentences [in red] (do not forget to remove the brackets);\n" +
                "# 2) Remove translated sentences no longer required [in green]\n" +
                "# 3) Check translated sentences already done [in black]\n" +
                "# *) Do not modify keywords (first word of each line);\n" +
                "#    Do not remove control characters (*,!,%) at the beginning of some words;\n" +
                "# 4) Install your translation, restart Aladin and check the result\n\n" +
                "");
          text.append("\nLANGUAGE "+lang+" ("+suf.substring(1)+") "
                +Aladin.VERSION.substring(1)+" "+author+"\n");
          
          // Les �l�ments non traduits
          Iterator it = defList.iterator();
          while( it.hasNext() ) {
             String key = (String)it.next();          
             if( map.get(key+suf)==null ) {
                if( first1 ) { first1=false; text.append("\n### MISSING TRANSLATION:\n"); }
                text.append(key+suf+" ["+reverseNL( (String)map.get(key) )+"]\n" );
             }
          }
          
          // Les �l�ments inutiles
          Enumeration e = map.keys();
          while( e.hasMoreElements() ) {
             String key = (String)e.nextElement();
             if( !key.endsWith(suf) ) continue;
             String k = key.substring(0,key.lastIndexOf(suf));
             if( map.get(k)!=null ) continue;
             if( key.startsWith("Tool.") ) continue;
             if( first2 ) { first2=false; text.append("\n### TRANSLATION NO LONGER REQUIRED:\n"); }
             text.append("#"+key+" "+reverseNL( (String)map.get(key) )+"\n" );
          }
          
          // Les �l�ments d�j� traduits
          it = defList.iterator();
          while( it.hasNext() ) {
             String key = (String)it.next();          
             if( map.get(key+suf)!=null ) {
                if( first3 ) { first3=false; text.append("\n### TRANSLATION DONE:\n"); }
                text.append(key+suf+" "+reverseNL( (String)map.get(key+suf) )+"\n" );
             }
          }
          
          if( first1 && first2 ) {
             aladin.info(frame,"The translation ("+suf+") seems complete!");
          }
          
          frame.pack();
          frame.setVisible(true);
          
          ta.setText(text.toString());
          ta.setCaretPosition(0);
          first=true;
//          search("");
          (new Thread("Translation") {
             public void run() { Util.pause(100); search(""); }
          }).start();
       }
    }
   
   /** Retourne true si au-moins un caract�re doit �tre encod�
    * sur plus de 8 bits */
   private boolean testUTF(String s) {
      int n = s.length();
      for( int i=0; i<n; i++ ) {
         if( (( (int)s.charAt(i) ) & 0xFF00 )!=0 ) return true;
      }
      return false;
   }
   
   /** Installation dans le cache de la traduction locale en cours
    * sous la forme d'un fichier Aladin-LANGUE-NNNN-perso.string[.utf]
    */
   private void installTranslation() {
      try {
         boolean flagUTF=false;
         String langue="";
         String s = ta.getText();
         StringBuffer text = new StringBuffer(8000);                  
         StringTokenizer st = new StringTokenizer(s,"\n");                 
         while( st.hasMoreTokens() ) {
            String line = st.nextToken().trim();
            if( line.length()==0 ) continue;
            if( line.charAt(0)=='#' ) continue;
            
            int pos = line.indexOf(' ');
            if( pos<0 ) continue;
            if( line.charAt(pos+1)=='[' && line.charAt(line.length()-1)==']' ) continue;
            
            text.append(line+Util.CR);
            
            if( langue.length()==0 && line.startsWith("LANGUAGE" ) ) {
               int end = line.indexOf("(");
               if( end>8 ) {
                  langue= line.substring(8,end-1).trim();
                  text.append(Util.CR);
               }
            }
         }
         
         flagUTF = testUTF(s);
         byte buf[];
         s = text.toString();
         if( !flagUTF ) buf = s.getBytes("ISO-8859-1");
         else {
            buf = s.getBytes("UTF-8");
            byte buf1 [] = new byte[buf.length+3];
            System.arraycopy(buf, 0, buf1, 3, buf.length);
            buf1[0]=(byte)0xEF;
            buf1[1]=(byte)0xBB;
            buf1[2]=(byte)0xBF;
            buf=buf1;
         }

         String fileName=getTranslationFileName(langue,flagUTF);
         aladin.configuration.installLanguage(langue, fileName, buf);
         Aladin.info(aladin,aladin.chaine.getString("RESTART")); 
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   
    /** Ajout d'�ventuels fichiers locaux. Parcours la variable Aladin.STRINGFILE qui contient
     * la liste des filenames additionnels. Le filename peut �tre un nom de fichier
     * complet ou un nom de fichier qui doit se trouver dans le CLASSPATH */
    private void loadAddFiles() {
       addAltFiles();
       if( Aladin.STRINGFILE==null ) return;
       StringTokenizer st = new StringTokenizer(Aladin.STRINGFILE,";");
       while( st.hasMoreTokens()) {
          String filename = st.nextToken();
          loadFile(filename);
       }
    }
    
    /** Ajout en pr�fixe du fichier Aladin*.string[.utf] trouv�
     * dans .aladin */
    private void addAltFiles() {
       try {
          String dir = System.getProperty("user.home") +Util.FS+Aladin.CACHE;
          FilenameFilter filter = new FilenameFilter() {
             public boolean accept(File dir, String name) {
                return Util.matchMaskIgnoreCase("Aladin*.string*", name);
             }
          };
          File fdir = new File(dir);
          File [] list = fdir.listFiles(filter);
          
          // Recherche des langues ajout�es localement (extension -perso.string...)
          for( int i=0; i<list.length; i++ ) {
             String name = list[i].getName();
             if( !name.endsWith("-perso.string.utf") && !name.endsWith("-perso.string") ) continue;
             if( Aladin.STRINGFILE==null ) Aladin.STRINGFILE="";
             else Aladin.STRINGFILE=";"+Aladin.STRINGFILE;
             Aladin.STRINGFILE=list[i].getAbsolutePath()+Aladin.STRINGFILE;
          }
          
          // Recherche des langues install�es � distance (normalement une seule)
          for( int i=0; i<list.length; i++ ) {
             String name = list[i].getName();
             if( !name.endsWith(".utf") && !name.endsWith(".string") ) continue;
             if( name.endsWith("-perso.string.utf") || name.endsWith("-perso.string") ) continue;
             if( Aladin.STRINGFILE==null ) Aladin.STRINGFILE="";
             else Aladin.STRINGFILE=";"+Aladin.STRINGFILE;
             Aladin.STRINGFILE=list[i].getAbsolutePath()+Aladin.STRINGFILE;
          }
       } catch( Exception e ) {
          if( Aladin.levelTrace>=3 ) e.printStackTrace();
       }
    }
    
    /** Retourne la chaine dont la cl� est key
     *
     * @param key cl� de la chaine recherch�e
     * @return String la chaine correspondant � la cl� key, <em>une chaine vide</em> si non trouv�e
     */
    public String getString(String key) {
       String suf = aladin.configuration.getLang();
       
        // si map est null, on charge le fichier et on conserver les chaines 
        // utilis�es fr�quemment dans map
//       synchronized(this) {
//        }

        Object o = map.get(key+suf);
        
        // Je charge le deuxi�me fichier des chaines peu fr�quentes
        if( o==null && !flagAll ) {
           try { loadFile(STRINGFILE); } catch( Exception e ) {}
           o = map.get(key+suf);
           flagAll=true;
        }
        
        if( o==null && suf.length()>0 ) {
           if( key.startsWith("TIP") ) return null;
//           if( Aladin.levelTrace>=3 ) System.err.println("No string for ["+(key+suf)+"]");
           o = map.get(key);
        }
        
        if( o==null ) {
           if( key.startsWith("TIP") ) return null;
           System.err.println("No default string for ["+key+"]");
           return "["+key+"]";
        }
        return (String)o;
    }

}
