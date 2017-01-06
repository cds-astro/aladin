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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.TimeZone;

/**
 * Gestion avanc�e d'une liste de propri�t�s
 * - conserve les lignes blanches et les commentaires si n�cessaire
 * - prend en compte l'ordre (insertion en d�but, en fin...)
 * - peut g�rer plusieurs valeurs pour une cl�
 * - supporte plusieurs formats en sortie (ASCII, ASCIIC, JSON, HTML, GLU )
 * - accepte en entr�e, une liste de cl� = valeur, sous forme d'un, voire de plusieurs enregistrements
 * 
 * Rq: ne supporte que l'ASCII basique
 * 
 * @author Pierre Fernique [CDS]
 * @version 2.0 d�cembre 2016 - fusion de la version Aladin et MultiMoc
 */
public class MyProperties {
   
   // Contient les propri�t�s (ConfigurationItem)
   private ArrayList<PropItem>         prop;  // Liste s�quentielle des propri�t�s
   private HashMap<String, PropItem>   hash;  // Acc�s direct � la valeur d'une propri�t�
   
   private StringBuilder propOriginal = null;   // Strings des properties originales (telles que) si demand� dans load()

   public MyProperties() {
      prop = new ArrayList<PropItem>();
      hash = new HashMap<String, PropItem>();
   }
   
   /** Retourne la liste ordonn�e des cl�s */
   public ArrayList<String> getKeys() {
      ArrayList<String> a = new ArrayList<String>();
      for( PropItem ci : prop ) a.add(ci.key);
      return a;
   }
   
   /** Teste l'�galit�. 
    * G�re les valeurs multiples possibles pour une m�me cl�
    * Ne prend pas en compte une �ventuelle cl� TIMESTAMP
    */
   public boolean equals(MyProperties p) {
      
      // Tests rapides
      if( this==p ) return true;
      if( p==null ) return false;
      if( Math.abs( size()-p.size() )>1 ) return false;   // peut y avoir le timestamp en plus

      // On compare les cl�s une � une
      for( String k : getKeys() ) {
         if( k.equals(" ") || k.equals("#") ) continue; // On ne compare les commentaires
         if( k.equals("TIMESTAMP") ) continue;          // On ne compare pas sur l'estampillage
         String v1 = p.get(k);
         String v = get(k);
         if( v1==v ) continue;
         if( v1==null && v!=null || v1!=null && v==null ) return false;
         
         // Deux valeurs simples ?
         if( v1.indexOf('\t')<0 && v.indexOf('\t')<0 ) {
            if( !v1.equals(v) ) return false;
            
         // Des valeurs multiples => il faut comparer chaque possibilit� de valeur
         } else if( !v1.equals(v) ) {
            int n=0,n1=0;
            Tok tok = new Tok(v,"\t");
            HashMap<String, String> hash = new HashMap<String, String>(100);
            while( tok.hasMoreTokens() ) { hash.put(tok.nextToken(),""); n++; }

            tok = new Tok(v1,"\t");
            while( tok.hasMoreTokens() ) { if( hash.get(tok.nextToken())==null ) return false; n1++; }
            if( n!=n1 ) return false;
         }
      }
      return true;
   }
   
   /**
    * Compare les propri�t�s et retourne la liste de celles qui ont �t� modifi�es
    * dans le MyProperties pass� en param�tre
    * @param p La r�f�rence � comparer
    * @return Une liste de chaines indiquant les modifications des propri�t�s
    */
   public ArrayList<String> getModVal(MyProperties p) {
      ArrayList<String> a = new ArrayList<String>();
      if( p==null ) return a;
      for( String k : getKeys() ) {
         if( k.equals(" ") || k.equals("#") ) continue;
         String v = get(k);
         String v1 = p.get(k);
         if( v1==null ) continue;
         if( v1==v ) continue;
         
         if( v1.indexOf('\t')<0 && v.indexOf('\t')<0 ) {
            if( !v1.equals(v) ) { a.add(k+" = "+v+" => "+v1); continue; }
            
         } else if( !v1.equals(v) ){

            int n=0,n1=0;
            Tok tok = new Tok(v,"\t");
            HashMap<String, String> hash = new HashMap<String, String>(100);
            while( tok.hasMoreTokens() ) { hash.put(tok.nextToken(),""); n++; }

            tok = new Tok(v1,"\t");
            while( tok.hasMoreTokens() ) { 
               if( hash.get(tok.nextToken())==null ) { n=-1; break; }
               n1++; 
            }
            if( n!=n1 ) {
               v=v.replace("\t",", ");
               v1=v1.replace("\t",", ");
               a.add(k+" = "+v+" => "+v1);
            }
         }
      }
      return a;
   }
   
   /**
    * Compare les propri�t�s et retourne la liste de celles qui ont �t� supprim�es
    * dans le MyProperties pass� en param�tre
    * @param p La r�f�rence � comparer
    * @return Une liste des cl�s supprim�es
    */
   public ArrayList<String> getDelKey(MyProperties p) {
      ArrayList<String> a = new ArrayList<String>();

      for( String k : getKeys() ) {
         if( k.equals(" ") || k.equals("#") ) continue;
         if( p==null )  a.add(k);
         else {
            String v1 = p.get(k);
            if( v1==null ) a.add(k);
         }
      }
      return a;
   }
   
   /**
    * Compare les propri�t�s et retourne la liste de celles qui ont �t� ajout�es
    * dans le MyProperties pass� en param�tre
    * @param p La r�f�rence � comparer
    * @return Une liste des cl�s ajout�es
    */
   public ArrayList<String> getAddKey(MyProperties p) {
      ArrayList<String> a = new ArrayList<String>();
      if( p==null ) return a;
      for( String k1 : getKeys() ) {
         if( k1.equals(" ") || k1.equals("#") ) continue;
         String v = get(k1);
         if( v==null ) a.add(k1);
      }
      return a;
   }
   
   /** Retourne directement une propri�t� particuli�re
    * @param key la cl� de la propri�t� recherch�e
    * @return la ConfigurationItem associ�e � la cl�, null si asbsente
    */
   private PropItem getItem(String key) {
      return hash.get(key);
   }
   
   /** Retourne le nombre de cl�s utilis�es */
   public int size() { return prop.size(); }
   
   /** Retourne une estimation du nombre d'octets n�cessaires � la m�morisation */
   public long getMem() {
      long mem=0L;
      for( PropItem item : prop ) mem += item.getMem();
      return mem;
   }
  
   /** Retourne la valeur associ�e � une cl�, et si elle est absente, retourne
    * la valeur indiqu�e en d�faut
    * @param key la cl� de la propri�t� recherch�e
    * @param defaut la valeur de la cl� en cas de d�faut
    */
   public String getProperty(String key,String defaut) { 
      String s = get(key); 
      return s==null ? defaut : s;
   }
   
   /** Retourne la valeur associ�e � une cl�, ou null si absente (identique � get(key))*/
   public String getProperty(String key) { return get(key); }

   /** Retourne la valeur associ�e � une cl�, ou null si absente */
   public String get(String key) {
      PropItem item = getItem(key);
      if( item!=null ) return item.value;
      return null;
   }
   
   /** Retourne la liste des valeurs pour une cl� (d�coupage des xxx\txxx\txxx...
    * null si la cl� est inconnue */
   public Iterator<String> getIteratorValues(String key) {
      final String value = get(key);
      if( value==null ) return null;
      return new Iterator<String>() {
         int opos=-1,pos=0;
         public boolean hasNext() { return pos!=-1; }
         public String next() {
            pos = value.indexOf('\t',opos+1);
            String rep = pos==-1 ? value.substring(opos+1) : value.substring(opos+1,pos);
            opos=pos;
            return rep;
         }
      };
   }
   
   /** Ajoute une propri�t� en fin de liste. 
    * Suppression de l'ancienne valeur si n�cessaire
    * @param key
    * @param value
    */
   public void add(String key, String value) {
      remove(key);
      PropItem item = new PropItem(key, value);
      prop.add(item);
      hash.put(key,item);
   }

   /** Insertion de la cl� et de la valeur au d�but de liste.
    * Suppression de l'ancienne valeur si n�cessaire
    * @param key
    * @param value
    */
   public void insert(String key, String value) {
      remove(key);
      PropItem item = new PropItem(key, value);
      prop.add(0,item);
      hash.put(key,item);
   }
   
   /** Suppression d'une propri�t� */
   public void remove(String key) {
      prop.remove( getItem(key) );
      hash.remove(key);
   }
   
   /** Ajout d'une nouvelle propri�t�.
    * Si la cl� existe d�j�, remplace sa valeur
    * @param key
    * @param value
    */
   public void setProperty(String key, String value) {
      replaceValue(key, value);
   }
   
   /** Remplacement de la valeur associ�e � une cl�
    * Ajout simple en fin de liste si inexistant au pr�alable
    * @param key
    * @param value
    */
   public void replaceValue(String key, String value) {
      PropItem item = getItem(key);
      if( item == null ) {
         item = new PropItem(key, value);
         prop.add(item);
         hash.put(key,item);
      } else item.value = value;
   }
   
   /** Ajout d'une valeur � une propri�t�. 
    * Si d�j� existant, ajoute cette valeur � la pr�c�dente (multi-valeurs)
    * @param key
    * @param value
    */
   public void put(String key, String value) {
      PropItem item = getItem(key);
      if( item == null ) {
         item = new PropItem(key, value);
         prop.add(item);
         hash.put(key,item);
      } else item.value += "\t" + value;
   }
   
   
   /** Substitution d'une cl� par une autre */
   public void replaceKey(String oldKey, String key) {
      for( PropItem item : prop ) {
         if( item.key.equals(oldKey) ) {
            item.key=key; 
            hash.remove(key);
            hash.put(key, item);
            return;
         }
      }
   }
   
   /** Retourne le flux des propri�t�s originales (uniquement s'il a �t� m�moris� losr du load */
   public String getPropOriginal() { return propOriginal!=null ? propOriginal.toString() : null; }
   
   // Lecture d'une ligne dans un flux basique InputStream
   // REM: ne supporte que l'ASCII basique
   // La ligne retourn�e ne contient ni le CR ni un �ventuellement LF
   private String readLine( InputStream in ) throws IOException {
      StringBuilder s = new StringBuilder(256);
      byte [] c = new byte[1];
      boolean eof = false;
      while( true ) {
         if( in.read(c)==-1 ) { eof=true; break; }   // Fin de flux
         char ch = (char)c[0];
         if( ch=='\n' ) break;         // Fin de la ligne
         if( ch!='\r' ) s.append(ch);  // on ne prend pas en compte le LF
      }
      
      // Fin du flux et rien lu => retourn null
      if( eof && s.length()==0 ) return null;
      
      return s.toString();
   }
   
   /** Charge les propri�t�s de l'enregistrement courant dans le flux. S'arr�te � la premi�re
    * ligne vide qui suit l'enregistrement (sans fermer le flux)
    * @param in
    * @return false si on a atteint la fin du flux (l'enregistrement courant a tout de m�me �t� charg�
    * @throws IOException
    */
   public boolean loadRecord(InputStream in) throws IOException { return load( in,false,true); }
   
   /**
    * Charge les propri�t�s depuis le flux courant. Consid�re qu'il n'y a qu'un seul
    * enregistrement pour tout le flux
    */
   public void load(InputStream in) throws IOException { load( in,false,false); }
   
   /**
    * Charge les propri�t�s � partir du flux courant
    * @param in
    * @param flagKeepOriginal Conserve une copie de l'original
    * @param flagBlankLinestop S'arr�te � la premi�re ligne vide (pour un flux multi-records)
    * @return false si on a atteint la fin du flux, sinon true
    * @throws IOException
    */
   public synchronized boolean load(InputStream in,boolean flagKeepOriginal,boolean flagBlankLinestop) throws IOException {
      
      // Pour conserver le flux original
      if( flagKeepOriginal ) propOriginal = new StringBuilder();

      prop = new ArrayList<PropItem>();
      hash = new HashMap<String, PropItem>();

      // Je lis les propri�t�s de la configuration
      String s;
      while( (s = readLine(in)) != null ) {
         if( flagKeepOriginal ) propOriginal.append(s+"\n");
         
         
         // Cas d'une ligne blanche...
         if( s.trim().length() == 0 ) {
            
            // Dans le cas o� l'on doit s'arr�ter � la premi�re ligne vide apr�s l'enregistrement
            // (flux avec plusieurs enregistrements cons�cutifs)
            if( flagBlankLinestop ) {
               
               // Fin de l'enregistrement
               if( prop.size()>0 ) break;
               
               // L'enregistrement n'ayant pas commenc�, on ne m�morise pas les lignes vides
               else if( prop.size()==0 ) continue;
            }
            
            // On m�morise cette ligne blanche pour pouvoir la restituer
            prop.add(new PropItem(" ", null));
            continue;
         }
         // Simple commentaire (ou proposition de cl� "#Cle  = valeur")
         if( s.charAt(0) == '#' ) {
            boolean simpleComment = true;
            int egal = s.indexOf('=');
            if( egal>1 ) {
               int blanc = s.indexOf(' ');
               if( blanc<0 ) blanc = s.indexOf('\t');
               if( blanc>0 && blanc<egal ) {
                  while( Character.isSpace( s.charAt(blanc)) && blanc<egal) blanc++;
                  simpleComment = blanc!=egal;
               } else simpleComment=false;
            }

            // M�morisation s'il s'agit d'un commentaire classique
            if( simpleComment ) {
               prop.add(new PropItem("#", s));
               continue;
            }
         }

         // ajout normal de la propri�t�
         add(s);
      }
      
      return s!=null;  // s==null si le flux est termin�e => return false

   }
   
   /** Ajout d'une ligne d�crivant une propri�t� par "cl� = valeur".
    * Le "=" est facultatif.
    * G�re le codage �ventuel des propri�t�s � la java.
    */
   public void add(String s) {
      String key;
      String value;
      try {
         int offset = s.indexOf('=');
         key = s.substring(0,offset).trim();
         value = s.substring(offset+1,s.length()).trim();

         if( value.indexOf("\\:")>=0 ) {
            char [] a = value.toCharArray();
            char [] b = new char[a.length];
            int j=0;
            boolean backSlash=false;
            for( int i=0; i<a.length; i++ ) {
               if( !backSlash && a[i]=='\\' ) { backSlash=true; continue; }
               backSlash=false;
               b[j++]=a[i];
            }
            value = new String(b,0,j);
         }
      } catch( Exception e ) {
         System.err.println("MyProperties reader error => "+e.getMessage());
         prop.add(new PropItem("#", "#Error: "+s));
         return;
      }

      put(key, value);
   }
   
   /** Gestion d'une modification des propri�t�s par MyProperties d'exceptions.
    * @param except
    * @param id
    */
   public void exceptions(MyProperties except, String id) {
      
      // D�termine si l'enregistrement correspond
      for( PropItem item : except.prop ) {
         if( item.key.startsWith("#") ) continue;  // Commentaire => on ignore
         if( item.key.startsWith(">") ) continue;  // Les substitutions seront appliqu�es dans la deuxi�me passe
         String key = item.key;
         String value = item.value;
         boolean test=false;
         char c=0;
         if( item.value.startsWith("!")) { value=item.value.substring(1); test=true; }
         else if( item.value.startsWith(">")) { value=item.value.substring(1); c='>'; }
         else if( item.value.startsWith("<")) { value=item.value.substring(1); c='<'; }
         
         String v = key.equals("ID") ? id : get(key);
//         String v = get(key);
         if( v==null ) return;      // La propri�t� n'y est pas => rien � faire
         if( c!=0 ) {
            boolean strict=true;
            if( value.startsWith("=") ) { strict=false; value=value.substring(1); }
            if( !testInequality(c, strict, value, v) ) return; // La propri�t� ne correspond pas => rien � faire
         } else { if( matchMask(value, v)==test ) return; } // La propri�t� ne correspond pas => rien � faire
      }
      
      // Applique les r�gles de substitution
      for( PropItem item : except.prop ) {
         if( item.key.startsWith("#") ) continue;  // Commentaire => on ignore
         if( !item.key.startsWith(">") ) continue; // on ne retient que les r�gles de substitution
         String key = item.key.trim().substring(1);
         String value = item.value.trim();
         int mode=0;
         if( item.value.startsWith("+")) { value=value.substring(1); mode=1; }
         if( item.value.startsWith("-")) { value=value.substring(1); mode=-1; }
         
         PropItem itemProp = getItem(key);
         if( itemProp==null ) {
            if( value.length()>0 && (mode==1 || mode==0) ) {
               put(key,value); 
            }
         } else {
            if( mode==0 ) {
               if( value.length()==0 ) remove(key);
               else itemProp.value=value;
            } else {
               boolean trouve=false;
               Tok tok = new Tok(itemProp.value,"\t");
               StringBuilder v1 = new StringBuilder();
               while( tok.hasMoreTokens() ) {
                  String v = tok.nextToken();
                  trouve |= v.equals(value);
                  if( mode==-1 && trouve ) continue;
                  if( v1.length()>0 ) v1.append("\t");
                  v1.append(v);
               }
               if( mode==1 && !trouve) v1.append("\t"+value);
               itemProp.value = v1.toString();
               if( itemProp.value.length()==0 ) remove(key); 
            }
         }
      }
   }

   /** M�morisation des propri�t�s sur la forme ASCII simple
    * @param out
    * @param comments
    * @throws IOException
    */
   public void store(OutputStream out, String comments) throws IOException {
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));

      for( PropItem item : prop ) {
         bw.write(item.toString());
         bw.newLine();
      }
      bw.flush();
   }
   
   /** Affichage des propri�t�s sur la forme d'une liste de chaines classiques "cl� valeur\n" */
   public String toString() {
      StringBuilder s = new StringBuilder(2048);
      for( PropItem item : prop ) {
         s.append(item.toString());
         s.append('\n');
      }
      return s.toString();
   }
   
   /** Retourne l'enregistrement dans la syntaxe GLU */
   public String getRecordGlu() { return getRecordGlu1(true); }
   
   /** Retourne l'enregistrement dans la syntaxe GLU, mais les �ventuels sites miroirs alternatifs ne seront
    * indiqu�s que pour les HiPS pr�vus pour Aladin Lite
    */
   public String getRecordGluX() {return getRecordGlu1(false); }
   
   private String getRecordGlu1(boolean flagMirror) {
      StringBuilder s = new StringBuilder();
      
      String s1 = get("ID");
      int index=s1.indexOf('/');
      
      String id = s1.substring(index+1);
      String gluId = id.replace('/','-');
      String origin = s1.substring(0,index);
      
      s.append(align("%ActionName", 20) +" "+ gluId+".hpx\n");
      s1 = get("obs_title");
      if( s1==null ) get("obs_collection");
      if( s1!=null ) s.append(align("%Description", 20) +" "+ s1+"\n");
      s.append(align("%Owner", 20) +" aladin\n");
      s.append(align("%DistribDomain", 20) +" ALADIN\n");
      
      // Dans le cas du !flagMirror, les mirroirs ne seront utilis�s que pour les HiPS pr�vus pour AladinLite 
      String s2 = get("client_application");
      boolean flagLite = s2!=null && s2.indexOf("AladinLite")>=0;
      
      int n=1;
      if( flagMirror || !flagMirror && flagLite ) {
         while( get("hips_service_url_"+n)!=null ) n++;
      }
      if( n==1 ) {
         s1 = get("hips_service_url"); if( s1!=null ) s.append(align("%Url", 20) +" "+ s1+"\n");
      } else {
         for( int i=0; i<n; i++ ) {
            s1 = get("hips_service_url"+(i==0?"":"_"+i));
            s2 = get("hips_status"+(i==0?"":"_"+i));
            if( s2!=null && s2.indexOf("partial")>0 ) s.append("#");
            if( s1!=null ) s.append(align("%SeeAction", 20) +" "+ gluId+"_"+i+".hpx\n");
         }
      }
      s1 = get("obs_description"); if( s1!=null ) s.append(align("%VerboseDescr", 20) +" "+ s1+"\n");
      if( origin.length()>0 ) s.append(align("%Origin", 20) +" "+ origin+"\n");
      s.append(align("%Id", 20) +" "+ id+"\n");
      s1 = get("client_application"); 
      if( s1!=null ) s1 = s1.indexOf("AladinDesktopBeta")>=0 ? " beta":"";
      else s1="";
      s.append(align("%Aladin.Profile", 20) +" >6.1"+s1+"\n");
      s1 = get("obs_copyright"); 
      if( s1==null ) s1 = get("prov_progenitor");
      if( s1!=null ) s.append(align("%Copyright", 20) +" "+ s1+"\n");
      s1 = get("obs_copyright_url"); if( s1!=null ) s.append(align("%Copyright.Url", 20) +" "+ s1+"\n");
      s1 = get("moc_sky_fraction"); if( s1!=null ) s.append(align("%SkyFraction", 20) +" "+ s1+"\n");
      s1 = get("obs_collection"); if( s1==null ) s1 = id;
      s.append(align("%Aladin.XLabel", 20) +" "+ s1+"\n");
      s1 = get("client_category"); if( s1!=null ) s.append(align("%Aladin.Tree", 20) +" "+ s1+"\n");
      s1 = get("client_sort_key");
      if( flagLite ) s1 = s1!=null? s1+" lite" : "lite";
      if( s1!=null ) s.append(align("%Aladin.MenuNumber", 20) +" "+ s1+"\n");
      
      if( get("hips_service_url")!=null ) {
         s.append(align("%Aladin.HpxParam",20));
         s1= get("hips_order"); if( s1!=null) s.append(" "+s1);
         s1= get("dataproduct_type");if( s1!=null) s.append(" "+s1);
         s1= get("dataproduct_subtype");if( s1!=null) s.append(" "+s1);
         s1= get("hips_frame"); if( s1!=null) s.append(" "+s1);
         s1= get("hips_tile_format");if( s1!=null) s.append(" "+s1);
         s.append("\n");
      }
      s1 = get("hips_release_date"); if( s1!=null ) s.append(align("%Aladin.Date", 20) +" "+ s1+"\n");
      
      // Plusieurs indirections ?
      if( n>1 )  {
         for( int i=0; i<n; i++ ) {
            s1 = get("hips_service_url"+(i==0?"":"_"+i));
            if( s1!=null ) {
               s.append("\n");
               s.append(align("%ActionName", 20) +" "+ gluId+"_"+i+".hpx\n");
               s.append(align("%Owner", 20) +" CDS'aladin\n");
               s.append(align("%DistribDomain", 20) +" ALADIN\n");
               s.append(align("%Url", 20) +" "+ s1+"\n");
            }
         }
      }
      
      return s.toString();
   }

   /** Retourne la premi�re URL matchant les filtres sur les fields */
   public String getFirstUrl(HashSet<String> fields) {
      StringBuilder s = new StringBuilder();

      for( PropItem item : prop ) {
         if( !isMappingField(item.key, fields) ) continue;
         if( item.key.equals("#") ) s.append(item.value+"\n"); // Commentaires
         else if( item.key.trim().length() > 0 ) {
            int pos=-1;
            pos = item.value.indexOf('\t',pos+1);
            if( pos==-1 ) pos=item.value.length();
            return item.value.substring(0,pos); // Trouv� !
         }
       }
      return null;
   }

   /**
    * Retourne l'enregistrement sous la forme ASCII (cle = valeur), �ventuellement compact�e
    * en enlevant les espaces de part et d'autre du signe �gal. La liste des propri�t�s retenues
    * ou non peut �tre control�e par le param�tre fields
    * @param fields controle des champs en sortie, null si tous
    */
   public String getRecord(HashSet<String> fields) { return getRecord(fields,false); }
   public String getRecord(HashSet<String> fields,boolean flagCompact) {
      StringBuilder s = new StringBuilder();

      for( PropItem item : prop ) {
         if( !isMappingField(item.key, fields) ) continue;
         if( item.key.equals("#") ) s.append(item.value+"\n"); // Commentaires
         else if( item.key.trim().length() > 0 ) {
            int pos=-1;
            do {
               int opos=pos;
               pos = item.value.indexOf('\t',pos+1);
               if( pos==-1 ) pos=item.value.length();
               s.append( getAsciiLine( item.key, item.value.substring(opos+1,pos),flagCompact) ); // Propri�t�s
            } while( pos<item.value.length() );
         }
      }
      return s.toString();
   }
   
   /** Retourne dans la bonne mise en forme une ligne ASCII key=value */
   static public String getAsciiLine( String key, String value,boolean flagCompact) {
      return flagCompact ? key+"="+value+"\n" : align(key, 20) +" = "+ value+"\n";
   }
   
   /**
    * Retourne l'enregistrement sous la forme HTML (coloris�e). La liste des propri�t�s retenues
    * ou non peut �tre control�e par le param�tre fields
    * @param fields controle des champs en sortie, null si tous
    */
   public String getRecordHTML(HashSet<String> fields) {
      StringBuilder s = new StringBuilder();
      s.append("<PRE>\n");
      String c;
      for( PropItem item : prop ) {
         if( !isMappingField(item.key, fields) ) continue;
         if( item.key.equals("#") ) s.append(item.value+"\n"); // Commentaires
         else if( item.key.trim().length() > 0 ) {
            int pos=-1;
            do {
               int opos=pos;
               pos = item.value.indexOf('\t',pos+1);
               if( pos==-1 ) pos=item.value.length();
               String v = item.value.substring(opos+1,pos);
               if( v.startsWith("http://")) v = "<A HREF=\""+v+"\" target=\"_top\">"+v+"</A>";
               String k = item.key;
               if( k.startsWith("hips_") ) c="grey";
               else if( k.startsWith("obs_") ) c="blue";
               else if( k.startsWith("data") ) c="magenta";
               else if( k.startsWith("moc_") ) c="green";
               else if( k.startsWith("client") ) c="orange";
               else c="black";
               if( k.equals("ID")) { c="red"; v = "<font size=\"+1\" color=\""+c+"\"><b>"+v+"</b></font>"; }
               s.append("<font color=\""+c+"\"><b>"+align(k, 20) +"</b></font> = "+ v+"\n"); // Propri�t�s
            } while( pos<item.value.length() );
         }
      }
      s.append("</PRE>\n");
      return s.toString();
   }
   
   /**
    * Teste si une cl� correspond ou non � un nom de champ particulier
    * @param key
    * @param fields
    * @return
    */
   private boolean isMappingField(String key, HashSet<String> fields) {
      if( fields==null ) return true;
      
      boolean deuxTours=false;
      boolean trouve=false;
      boolean onlyRemove=true;
      
      // Le champ doit-il �tre retenu ?
      for( String mask : fields ) {
         if( mask.charAt(0)=='!' ) { deuxTours=true; continue; }
         onlyRemove=false;
         if( matchMask(mask, key)) { trouve=true; break; }
      }
      if( onlyRemove ) trouve=true;
      if( !trouve || !deuxTours ) return trouve;
      
      // Le champ retenu serait-il en fin de compte exclu ?
      for( String mask : fields ) {
         if( mask.charAt(0)!='!' ) continue;
         mask = mask.substring(1);
         if( matchMask(mask, key)) return false;
      }
   
      return true;
   }

   /**
    * Retourne l'enregistrement sous la forme JSON. La liste des propri�t�s retenues
    * ou non peut �tre control�e par le param�tre fields
    * @param fields controle des champs en sortie, null si tous
    */
   public String getRecordJson(HashSet<String> fields) {
      StringBuilder s = new StringBuilder("{");

      for( PropItem item : prop ) {
         if( !isMappingField(item.key, fields) ) continue;
         if( item.key.trim().length() > 0 ) {
            s.append(" \""+item.key+"\":");
            String value = escapeJson(item.value);
            if( item.value.indexOf('\t')==-1 ) s.append("\""+ value+"\","); // Propri�t�s
            else {
               int pos=-1;
               s.append("[ ");
               int len=value.length();
               do {
                  int opos=pos;
                  pos = value.indexOf('\t',pos+1);
                  if( pos==-1 ) pos=len;
                  if( opos!=-1 ) s.append(", ");
                  s.append("\""+value.substring(opos+1,pos)+"\"");
               } while( pos<len );
//               s.append("],\n");
               s.append("],");
               
            }
         }
      }
      s.replace(s.length()-1, s.length(), "}");
      return s.toString();
   }
   
   /** Retourne une ligne key = value en format JSON */
   static public String getJsonLine(String key, String value ) {
      return " \""+key+"\":\""+ escapeJson(value)+"\"";
   }
   
   /** Ins�re les caract�res d'�chappement qu'il faut pour une chaine JSON */
   public static String escapeJson( String s ) {
      if( s.indexOf('"')<0 && s.indexOf('\\')<0 ) return s;
      char [] a = s.toCharArray();
      StringBuilder s1 = new StringBuilder(a.length);
      for( int i=0; i<a.length; i++ ) {
         char ch = a[i];
         if( ch=='"' ) s1.append('\\');
         else if( ch=='\\' && i<a.length-1 ) {
            char ch1=a[i+1];
            if( ch1!='n' && ch1!='t' ) s1.append('\\');
         }
         s1.append(ch);
      }
      return s1.toString();
   }
  
    /**
     * Classe permettant la m�morisation d'un propri�t�, c'est-�-dire un couple
     * (cl�,valeur)
     */
    private class PropItem {
       protected String key;   // Cl� associ�e � la propri�t�
       protected String value; // Valeur associ�e � la propri�t�

       private PropItem(String key, String value) {
          this.key = key;
          this.value = value;
       }

       public String toString() {
          if( key.equals("#")) return value; // Commentaire unique (pour compatibilit�)
          if( key==null || value==null ) return "";
          if( value.indexOf('\t')==-1 ) return align(key, 20) +" = "+ value; // Propri�t� simple
          StringBuilder s = new StringBuilder();
          Tok tok = new Tok(value,"\t");
          while( tok.hasMoreTokens() ) {
             if( s.length()>0 ) s.append(CR);
             s.append( align(key, 20) +" = "+ tok.nextToken());
          }
          return s.toString();
       }
       
       private long getMem() { return 2*(key==null? 0 : key.length()) + (value==null ? 0 : value.length()); }
    }
    
    static final String CR = System.getProperty("line.separator");

    /** Utilitaire pour ajouter des blancs apr�s un mot afin de lui donner une taille particuli�re
     * @param key le mot � aligner
     * @param n le nombre de caract�res souhait�s
     * @return le mot align�, ou si trop grand, avec juste un espace derri�re
     */
    static public String align(String key,int n) { return align(key,n,""); }
    static public String align(String key,int n,String suffixe) {
       int i=key.length();
       if( i>=n ) return key+ suffixe +" ";
       StringBuffer s = new StringBuffer();
       for( int j=0; j<n-i; j++ ) s.append(' ');
       return key+suffixe+s;
    }
    
    static final String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm";
    static final SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
    static {
       TimeZone utc = TimeZone.getTimeZone("UTC");
       sdf.setTimeZone(utc);
    }
    
    public static final String getDate(long ms) {
       return sdf.format(new Date(ms));
    }
    
    /** Effectue un test de grandeur (c='>' ou '<') soit num�rique, soit calendaire, soit alphanum�rique
     * en d�terminant automatiquement le type de donn�es
     * @param c    comparateur
     * @param strict true s'il s'agit d'un test d'in�galit� strict, sinon �galit� incluse
     * @param ref  valeur � tester
     * @param value valeur de r�f�rence
     * @return r�sultat du test (  value c ref  )
     */
    public static boolean testInequality(char c, boolean strict, String ref, String value) {
       try {
          // Probablement une date ISO
          if( value.indexOf('T')>0 ) {
             if( ref.indexOf('T')<0) ref=ref+"T00:00";
             else if( ref.charAt(ref.length()-1)=='Z' ) ref=ref.substring(0,ref.length()-1);
             if( value.charAt(value.length()-1)=='Z' ) value=value.substring(0,value.length()-1);
             Date dNum = sdf.parse(ref);
             Date dProp = sdf.parse(value);
             if( c=='<' ) return dProp.compareTo(dNum)<0;
             return dProp.compareTo(dNum)>0;
          }
          
          // Probablement une valeur num�rique
          double vNum = Double.parseDouble(ref.trim());
          double vProp = Double.parseDouble(value.trim());
          if( c=='>' ) return strict ? vProp>vNum : vProp>=vNum;
          return strict ? vProp<vNum : vProp<=vNum;

       // Bon, on va faire une comparaison alphanum�rique
       } catch( Exception e ) {
          if( c=='<' ) return strict ? value.compareTo(ref)<0 :  value.compareTo(ref)<=0;
          return strict ? value.compareTo(ref)>0 : value.compareTo(ref)>0;
       }
    }
    
    /** Adapted from a C-algorithm from P. Fernique
     * checks whether word matches mask
     * @param mask a string which may contain '?' and '*' wildcards
     * @param word the string to check
     * @return boolean true if word matches mask, false otherwise
     */
    static public final boolean matchMask(String mask, String word) {
       if( word==null || mask==null ) return false;
       
       // Quelques cas particuliers
       int n = mask.length();
       
       // * => true
       if( n==1 && mask.charAt(0)=='*' ) return true;
       
       // xxx
       if( mask.indexOf('*')<0 && mask.indexOf('?')<0 ) return word.equals(mask);
       
       // *xxx*
       if( n>2 && mask.charAt(0)=='*' && mask.charAt(n-1)=='*' ) {
          String s = mask.substring(1,n-1);
          if( s.indexOf('*')<0 && s.indexOf('?')<0 ) { 
             return word.indexOf(s)>=0;
          }
       }
       
       // *xxx
       if( n>2 && mask.charAt(0)=='*' && mask.indexOf('*',1)<0 && mask.indexOf('?',1)<0) {
          return word.endsWith( mask.substring(1) );
       }
       
       // xxx*
       if( n>2 && mask.charAt(n-1)=='*' && mask.lastIndexOf('*',n-1)<0 && mask.lastIndexOf('?',n-1)<0) {
          return word.startsWith( mask.substring(0,n-1) );
       }
       
       // Cas g�n�ral
       String m = mask+'\0';
       String a = word+'\0';
       int im=0,ia=0,ib=-1,ic=-1;
       char cm,ca;

       while( (cm=m.charAt(im))!=0 || a.charAt(ia)!=0 ) {
          if( cm=='\\' ) { im++; continue; }
          if( cm=='*' && (im==0 || m.charAt(im-1)!='\\') ) { im++; ib=im; continue; }
          ca=a.charAt(ia);
          if( ib!=-1 && ib!=im && ca==cm ) ic=ia;
          if( cm==ca || cm=='?' ) {
             if( cm==0 ) {
                if( ib==-1 ) return false;
             } else im++;
             if( ca==0 ) return false;
             else ia++;
          } else {
             if( ib!=-1 ) {
                im=ib;
                if( ic!=-1 ) { ia=ic; ic=-1; }
                else {
                   if( m.charAt(ib)!=ca || ca=='\\' ) {
                      if( ca==0 ) return false;
                      else ia++;
                   }
                }
             } else return false;
          }
       }
       return true;
    }
}