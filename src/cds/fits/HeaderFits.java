// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.fits;

import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import cds.aladin.Aladin;
import cds.aladin.FrameHeaderFits;
import cds.aladin.MyInputStream;
import cds.aladin.Save;

/**
 * Classe dediee a la gestion d'un header FITS.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.6 : Déc 2007 : Possibilité de surcharger les mots clés
 * @version 1.5 : 20 aout 2002 methode readFreeHeader
 * @version 1.4 : 19 juin 00 Utilisation du PushbackInputStream et
 *                implantation de isHCOMP()
 * @version 1.3 : (6 juin 2000) format HCOMPRESS
 * @version 1.2 : (20 mars 2000) prise en compte du champ EQUINOX enquote
 * @version 1.1 : (14 jan 99) affichage du header fits dans un frame
 * @version 0.9 : (18 mai 99) Creation
 */
public final class HeaderFits {

   private StringBuffer   memoHeaderFits = null;  // Memorisation de l'entete FITS telle quelle (en Strings)
   
  /** Les elements de l'entete */
   protected Hashtable header;
   protected Vector<String> keysOrder;

   /** La taille de l'entete FITS (en octets) */
    private int sizeHeader=0;

    /** Création du header Fits à partir de rien */
    public HeaderFits() {
        alloc();
    }

  /** Creation du header.
   */
    public HeaderFits(MyInputStream dis) throws Exception {
      readHeader(dis);
   }

    public HeaderFits(String s) throws Exception {
       readFreeHeader(s);
    }

    public HeaderFits(String s,FrameHeaderFits frameHeaderFits) throws Exception {
       readFreeHeader(s,false,frameHeaderFits);
    }

    public HeaderFits(MyInputStream dis,FrameHeaderFits frameHeaderFits) throws Exception {
       readHeader(dis,frameHeaderFits);
    }
    
    /** Retourne le header FITS original (en Strings) */
    public String getOriginalHeaderFits() { return memoHeaderFits.toString(); }
    
    /** Mémorise le header FITS original (en Strings) */
    public void setOriginalHeaderFits(String s) { memoHeaderFits= new StringBuffer(s); }

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
   public int getSizeHeader() { return sizeHeader; }
   
   /** retourne la table de hash des mots clés */
   public Hashtable<String,String> getHashHeader() { return header; }
   
   /** Retourne un énumerateur sur la liste des mots clés (ordonnés) */
   public Enumeration<String> getKeys() { return keysOrder.elements(); }

  /** Extraction de la valeur d'un champ FITS. Si on commence par une quote, va jusqu'à la
   * prochaine quote, sinon jusqu'au commentaire, ou sinon la fin de la ligne
   * @param buffer La ligne
   * @return La valeur
   */
   static public String getValue(String s) {
      byte [] a = new byte[80];
      int i;
      for( i=0; i<s.length(); i++ ) a[i]=(byte)s.charAt(i);
      while( i<80 ) a[i++]=(byte)' ';
      return getValue(a);
   }
   
   /** Extraction de la valeur d'un champ FITS. Si on commence par une quote, va jusqu'à la
    * prochaine quote, sinon jusqu'au commentaire, ou sinon la fin de la ligne
    * @param buffer La ligne
    * @return La valeur
    */
   static public String getValue(byte [] buffer) {
       int i;
       boolean quote = false;
       boolean blanc=true;
       int offset = 9;

       for( i=offset ; i<80; i++ ) {
          if( !quote ) {
             if( buffer[i]==(byte)'/' ) break;   // on a atteint le commentaire
          } else {
             if( buffer[i]==(byte)'\'') break;   // on a atteint la prochaine quote
          }

          if( blanc ) {
             if( buffer[i]!=(byte)' ' ) blanc=false;
             if( buffer[i]==(byte)'\'' ) { quote=true; offset=i+1; }
          }
       }
       return (new String(buffer, 0, offset, i-offset)).trim();
   }

   /** retourne la clé d'une ligne d'entete FITS */
   static public  String getKey(byte [] buffer) {
      return new String(buffer, 0, 0, 8).trim();
   }

   /** retourne la clé d'une ligne d'entete FITS */
   static public String getKey(String s) {
      if( s.length()<8 ) return s;
      return s.substring(0,8).trim();
   }
   
   /** Lecture d'une entete PDS */
   public boolean readHeaderPDS(MyInputStream dis,FrameHeaderFits frameHeaderFits) throws Exception {
      
      int linesRead=0;
      alloc();
      try {
         while( true ) {
            String s = dis.readLine();
            linesRead++;
            if( s.length()==0 ) continue;
//            System.out.println("["+s+"]");
            if( s.trim().equals("END") ) return true;
            if( frameHeaderFits!=null ) frameHeaderFits.appendMHF(s);
            int i = s.indexOf('=');
            if( i<0 ) continue;
            String key = s.substring(0,i-1).trim();
            String value = s.substring(i+1).trim();
            header.put(key, value);
            keysOrder.addElement(key);
         }
      } catch( Exception e ) {
         Aladin.error="PDS header error (line "+(linesRead+1)+")";
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
         throw new Exception();
      }
   }

  /** Lecture de l'entete FITS.
   * Mise a jour de tableau associatif header.
   * @param dis Flux de donnees
   * @return true si OK, false sinon
   */
   public boolean readHeader(MyInputStream dis) throws Exception { return readHeader(dis,null); }
   public boolean readHeader(MyInputStream dis,FrameHeaderFits frameHeaderFits) throws Exception {
      int blocksize = 2880;
      int fieldsize = 80;
      String key, value;
      int linesRead = 0;
      sizeHeader=0;
      boolean firstLine=true;

//Aladin.trace(3,"Reading FITS header");
      byte[] buffer = new byte[fieldsize];

      alloc();
      try {
         while (true) {
            dis.readFully(buffer);
//System.out.println(Thread.currentThread().getName()+":"+linesRead+":["+new String(buffer,0)+"]");
            key =  getKey(buffer);
            if( linesRead==0 && !key.equals("SIMPLE") && !key.equals("XTENSION") ) {
//               System.out.println("pb: key="+key+" s="+new String(buffer,0));
               throw new Exception("probably not a FITS file");
            }
            sizeHeader+=fieldsize;
            linesRead++;
            if( key.equals("END" ) ) break;
            appendMHF(new String(buffer,0));
            if( buffer[8] != '=' ) continue;
            value=getValue(buffer);
//Aladin.trace(3,key+" ["+value+"]");
            header.put(key, value);
            keysOrder.addElement(key);
         }

        // Test s'il s'agit de FITS Hcompresse 
        if( dis.isHCOMP() ) return true;
        
         // On passe le bourrage eventuel
         int bourrage = blocksize - sizeHeader%blocksize;
         if( bourrage!=blocksize ) {
            byte [] tmp = new byte[bourrage];
            dis.readFully(tmp);
            sizeHeader+=bourrage;
         }
      } catch( Exception e ) {
//System.out.println("lig="+(linesRead+1)+" "+new String(buffer,0));
// CETTE VARIABLE error AURAIT DU ETRE NON STATIC ET PASSE VIA LA REFERENCE A ALADIN
// SI ON VEUT POUVOIR UTILISER CORRECTEMENT PLUSIEURS INSTANCES D'ALADIN
         if( linesRead==0 ) Aladin.error="Remote server message:\n"+new String(buffer,0);
         else {
            Aladin.error="Fits header error (line "+(linesRead+1)+")";
//            if( Aladin.levelTrace>=3 ) e.printStackTrace();
         }
         throw e;
      }

      return true;
   }

   /** Recherche d'un caractère c dans buf à partir de la position from. Retourne
    * la position courante si on trouve un \n avant et que l'on atteint la limite finLigne
    * ou buf.length. Dans le cas de recherche du caractère '/' (pour les commentaires FITS),
    * il est ignoré s'il se trouve dans une chaine quotée par '
    * @param buf buffer de recherche
    * @param from offset de départ
    * @param finLigne offset de fin
    * @param c caractère à rechercher
    * @return position de c, ou de \n ou de finLigne
    */
   private int getPos(char buf[],int from,int finLigne,char c) {
      int max = buf.length;
      boolean inQuote= from+2<buf.length && buf[from+2]=='\'' && c=='/';
      int deb=from;
      while( from<max && from<finLigne && (buf[from]!=c || buf[from]==c && inQuote)
            && buf[from]!='\n' ) {
         if( from>deb+2 && buf[from]=='\'' && buf[from-1]!='\'' ) inQuote=false;
         from++;
      }
      return from;
   }

   /** Lecture d'une entête Fits quelque soit sa structure. Soit des lignes ASCII séparées
    * par des \n, soit des lignes de 80 caractères sans \n. Le = n'est pas obligatoirement
    * en 8ème position.
    * Mise a jour de tableau associatif header
    * @param s La chaine contenant le header Fits
    * @return true si OK, false sinon
    */
   public boolean readFreeHeader(String s) { return readFreeHeader(s,false,null); }
   public boolean readFreeHeader(String s,boolean specialDSS,FrameHeaderFits frameHeaderFits) {
      alloc();
      int len=79;
      char buf [] = s.toCharArray();
      int i=0;
      String key,value,com;
      boolean first=true;
      int a,b,c;
      while( i<buf.length ) {
         
         // Si on ne commence pas par SIMPLE, on l'ajoute sinon ça posera souci au cas
         // où l'on sauvegarde en FITS par la suite une image PNG ou JPEG avec entête par .hhh
         if( first ) {
            first=false;
            if( buf.length>i+7 && !(new String(buf,i,6)).equals("SIMPLE") ) {
               appendMHF((new String(Save.getFitsLine("SIMPLE","T",null))).trim());
            }
         }
         
         // Cas particulier d'une ligne vide
         c=getPos(buf,i,i+len,'\n');
         if( (new String(buf,i,c-i)).trim().length()==0 ) {
            appendMHF("");
            

         // Cas particulier pour COMMENT XXXX
         } else if( buf.length>i+7 && (new String(buf,i,7)).equals("COMMENT") ) {
            a=i+7;
            c = getPos(buf,a,i+len,'\n');
            com = (c-a>0) ? (new String(buf,a+1,c-a-1)).trim() : "";
            appendMHF((new String(Save.getFitsLineComment(com))).trim());

         // Cas particulier pour HISTORY XXXX
         } else if( buf.length>i+7 && (new String(buf,i,7)).equals("HISTORY") ) {
               a=i+7;
               c = getPos(buf,a,i+len,'\n');
               com = (c-a>0) ? (new String(buf,a+1,c-a-1)).trim() : "";
               appendMHF((new String(Save.getFitsLineHistory(com))).trim());

            // Cas général
         } else {
            a = getPos(buf,i,i+len,'=');
            b = getPos(buf,a,i+len,'/');
            c = getPos(buf,b,i+len,'\n');
            if( i!=a || i!=b || i!=c ) {
               key = new String(buf,i,a-i).trim();
               value = (b-a>0 ) ? (new String(buf,a+1,b-a-1)).trim() : "";
               com = (c-b>0) ? (new String(buf,b+1,c-b-1)).trim() : "";
               //System.out.println(i+":"+a+"["+key+"]="+b+"["+value+"]/"+c+"["+com+"]");
               if( key.equals("END") ) {
                  value=com=null;
                  break;
               }
               
               // Dans le cas d'une entête DSS dans un fichier ".hhh" il ne faut pas retenir les mots
               // clés concernant l'astrométrie de la plaque entière
               //            if( !( specialDSS && (key.startsWith("AMD") || key.startsWith("PLT"))) ) {
               header.put(key, value);
               keysOrder.addElement(key);
               //            }
               appendMHF((new String(Save.getFitsLine(key, value, com))).trim());
            }
         }
         i=c+1;
      }
      
      // NORMALEMENT C'EST CORRIGE PAR BOF - PF fév 2011
//      if( specialDSS ) purgeAMDifRequired();
      
      return true;
   }
   
   // HORRIBLE PATCH (Pierre)
   // Dans le cas des entêtes .hhh associées aux imagettes DSS, il y a souvent deux calibrations, non compatibles
   // dans ce cas, je supprime celle de la plaque et je ne garde que celle de l'imagette.
//   private void purgeAMDifRequired() {
//      if( header.get("CRPIX1")==null ) return; 
//      
//      System.err.println("*** Double calibration on DSS image => remove AMD/PLT one");
//      Vector<String> nKeysOrder = new Vector<String>();
//      for( String key : keysOrder ) {
//         if( key.startsWith("AMD") || key.startsWith("PLT") ) header.remove(key);
//         else nKeysOrder.addElement(key);
//      }
//      keysOrder = nKeysOrder;
//   }

   /**
    * Teste si un mot clé est présent dans l'entête
    * @param key la clé à tester
    * @return true si la clé est présente
    */
   public boolean hasKey(String key) {
      return header.get(key.trim())!=null;
   }


  /** Recherche d'un element entier par son mot cle
   * @param key le mot cle  (inutile de l'aligner en 8 caractères)
   * @return la valeur recherchee
   */
   public int getIntFromHeader(String key)
                 throws NumberFormatException,NullPointerException {
      String s;
      int result;

      s = (String) header.get(key.trim());
      result = (int)Double.parseDouble(s.trim());
      return result;
   }

  /** Extrait les elements d'un floattant.
   * Purge d'eventuels ' et blancs avant et apres
   */
   private String trimDouble(String s) {
      char [] a = s.toCharArray();
      int i;				// offset du debut
      int j;				// offset de fin
      char ch;				// tmp

      // On cherche le signe ou le premier chiffre
      for( i=0; i<a.length; i++ ) {
         ch = a[i];
         if( ch=='+' || ch=='-' || ch=='.' || (ch>='0' && ch<='9' ) ) break;
      }

      // on cherche le dernier chiffre ou un '.'
      for( j=a.length-1; j>=i; j-- ) {
         ch=a[j];
         if( (ch>='0' && ch<='9' ) || ch=='.' ) { j++; break; }
      }

      return new String(a,i,j-i);
   }

   /** Surcharge ou ajout d'un mot clé */
   public void setKeyword(String key,String value) {
      header.put(key,value);
   }

  /** Recherche d'un element double par son mot cle
   * @param key le mot cle (inutile de l'aligner en 8 caractères)
   * @return la valeur recherchee
   */
   public double getDoubleFromHeader(String key)
                 throws NumberFormatException,NullPointerException {
      String s;
      double result;

      s = (String) header.get(key.trim());
      result = Double.valueOf(trimDouble(s)).doubleValue();
      return result;
   }

  /** Recherche d'une chaine par son mot cle
   * @param key le mot cle  (inutile de l'aligner en 8 caractères)
   * @return la valeur recherchee
   */
   public String getStringFromHeader(String key)
                 throws NullPointerException {
      String s = (String) header.get(key.trim());
      if( s==null || s.length()==0 ) return s;
      if( s.charAt(0)=='\'' ) return s.substring(1,s.length()-1).trim();
      return s;
//      return (String) header.get(key.trim());
   }

   /** Ajout, surcharge ou suppression d'un mot cle
    * @param key le mot clé (inutile de l'aligner en 8 caractères)
    * @param value la valeur à positionner, null si suppression
    */
   public void setToHeader(String key,String value) {
      if( value==null ) header.remove(key);
      else header.put(key.trim(),value);
   }

   /** Ajoute/remplace/supprime un couple (MOTCLE,VALEUR) - l'ordre des mots clés
    * est mémorisé dans keysOrder, et les valeurs sont stockées dans header
    * Ra : VALEUR=null signifie une suppression */
   public void setKeyValue(String key, String value) {

      // Suppression ?
      if( value==null ) {
         if( !hasKey(key) ) return;
         header.remove(key);
         keysOrder.remove(key);
         return;
      }

      // Ajout
      if( !hasKey(key) ) keysOrder.addElement(key);
      header.put(key, value);
   }

   /** Ecriture de l'entête FITS des mots clés mémorisés. L'ordre est conservé
    * comme à l'origine - les commentaires ne sont pas restitués 
    * @return le nombre d'octets écrits */
   public int writeHeader(OutputStream os ) throws Exception {
      int n=keysOrder.size()*80;
      byte [] b= getEndBourrage(n);
      byte buf [] = new byte[n + b.length];
      
      int m=0;
      Enumeration e = keysOrder.elements();
      while( e.hasMoreElements() ) {
         String key = (String)e.nextElement();
         String value = (String) header.get(key);
         if( value==null ) continue;
         System.arraycopy(getFitsLine(key,value),0,buf,m,80 );
         m+=80;
      }
      System.arraycopy(b,0,buf,m,b.length);
      n+=b.length;
      os.write(buf);
      return n;
      
//      int n=0;
//      Enumeration e = keysOrder.elements();
//      while( e.hasMoreElements() ) {
//         String key = (String)e.nextElement();
//         String value = (String) header.get(key);
//         if( value==null ) continue;
//         os.write( getFitsLine(key,value) );
//         n+=80;
//      }
//      byte [] b= getEndBourrage(n);
//      n+=b.length;
//      os.write(b);
//      return n;

   }

   /** Génération de la fin de l'entête FITS, càd le END et le byte de bourrage
    * pour que cela fasse un multiple de 2880.
    * @param headSize taille actuelle de l'entête
    */
  static public byte [] getEndBourrage(int headSize) {
      int size = 2880 - headSize%2880;
      if( size<3 ) size+=2880;
      byte [] b = new byte[size];
      b[0]=(byte)'E'; b[1]=(byte)'N';b[2]=(byte)'D';
      for( int i=3; i<b.length; i++ ) b[i]=(byte)' ';
      return b;
   }

  /**
   * Mise en forme d'une ligne pour une entête FITS. Prends en compte si la valeur
   * est numérique, String et même éventuellement String déjà quoté à la FITS
   * @param key La clé
   * @param value La valeur
   * @param comment Un éventuel commentaire, sinon ""
   * @return la chaine de 80 caractères au format FITS
   */
  static public byte [] getFitsLine(String key, String value) {
     return getFitsLine(key,value,null);
  }
  static public  byte [] getFitsLine(String key, String value, String comment) {
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

        // Valeur numérique => alignement à droite
        if( !isFitsString(value) ) {
           for( j=0; j<20-a.length; j++)  b[i++]=(byte)' ';
           for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];

        // Chaine de caractères => formatage
        } else {
           a = formatFitsString(a);
           for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte)a[j];
           while( i<30 ) b[i++]=(byte)' ';
        }
     }

     // Le commentaire
     if( comment!=null && comment.length()>0 ) {
        if( value!=null ) { b[i++]=(byte)' ';b[i++]=(byte)'/'; b[i++]=(byte)' '; }
        a = comment.toCharArray();
        for( j=0; i<80 && j<a.length; j++,i++) b[i]=(byte) a[j];
     }

     // Bourrage
     while( i<80 ) b[i++]=(byte)' ';

     return b;
  }

  /**
   * Test si c'est une chaine à la FITS (ni numérique, ni booléen)
   * @param s la chaine à tester
   * @return true si s est une chaine ni numérique, ni booléenne
   * ATTENTION: NE PREND PAS EN COMPTE LES NOMBRES IMAGINAIRES
   */
  static private boolean isFitsString(String s) {
     if( s.length()==0 ) return true;
     char c = s.charAt(0);
     if( s.length()==1 && (c=='T' || c=='F') ) return false;   // boolean
     if( !Character.isDigit(c) && c!='.' && c!='-' && c!='+' ) return true;
     try {
        Double.valueOf(s);
        return false;
     } catch( Exception e ) { return true; }
  }

  /**
   * Mise en forme d'une chaine pour une entête FITS en suivant la règle suivante:
   * si mot plus petit que 8 lettres, bourrage de blancs
   * utilisation de quotes simples + double quote simple à l'intérieur
   * @param a la chaine a mettre en forme. Elle peut être déjà quotée
   * @return la chaine mise en forme
   */
  static private char [] formatFitsString(char [] a) {
     if( a.length==0 ) return a;
     StringBuffer s = new StringBuffer();
     int i;
     boolean flagQuote = a[0]=='\''; // Chaine déjà quotée ?

     s.append('\'');

     // recopie sans les quotes
     for( i= flagQuote ? 1:0; i<a.length- (flagQuote ? 1:0); i++ ) {
        if( !flagQuote && a[i]=='\'' ) s.append('\'');  // Double quotage
        s.append(a[i]);
     }

     // bourrage de blanc si <8 caractères + 1ère quote
     for( ; i< (flagQuote ? 9:8); i++ ) s.append(' ');

     // ajout de la dernière quote
     s.append('\'');

     return s.toString().toCharArray();
  }

  /** Allocation ou réallocation des structures de mémorisation */
  protected void alloc() {
//     if( header!=null && keysOrder!=null ) return;
     header = new Hashtable(200);
     keysOrder = new Vector(200);
  }
  
  /** Retourne la taille mémoire approximative */
  public long getMem() { return 16+(keysOrder==null?0:keysOrder.size()*50); }



}
