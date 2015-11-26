package cds.aladin;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.Vector;

import cds.tools.Util;


public class MyProperties extends Properties {
   
   private StringBuilder propOriginal = null;   // String des properties originales (telles que)

   public MyProperties() {
      super();
      prop = new Vector<ConfigurationItem>();
   }

   public String [] getKeys() {
      String [] keys = new String[ prop.size() ];
      int i=0;
      for( ConfigurationItem item : prop ) {
         keys[i++] = item.key;
      }
      return keys;
   }

   // Contient les propriétés (ConfigurationItem)
   private Vector<ConfigurationItem>          prop;

   private ConfigurationItem getItem(String key) {
      for( ConfigurationItem item : prop ) {
         if( item.key.equals(key) ) return item;
      }
      return null;
   }

   public void replaceValue(String key, String value) {
      for( ConfigurationItem item : prop ) {
         if( item.key.equals(key) ) { item.value=value; break; }
      }
   }
   

   public void replaceKey(String oldKey, String key) {
      for( ConfigurationItem item : prop ) {
         if( item.key.equals(oldKey) ) {
            item.key=key;
            remove(oldKey);
            put(key,item.value);
         }
      }
   }

   public void remove( String key ) {
      int i;
      for( i=0 ;i<prop.size(); i++ ) {
         ConfigurationItem item = prop.get(i);
         if( item.key.equals(key)) break;
      }
      if( i<prop.size() ) prop.remove(i);
      super.remove(key);
   }


   public synchronized Object put(Object key, Object value) { return put(key,value,false); }
   public synchronized Object put(Object key, Object value,boolean flagAdd) {
      if( ((String)key).equals("!")) key = "!"+System.currentTimeMillis();
      ConfigurationItem item = getItem((String)key);
      if( item == null ) {
         item = new ConfigurationItem((String)key, (String)value);
         prop.addElement(item);
      } else {
         if( flagAdd ) item.value += "\t" + (String)value;
         else item.value = (String)value;
      }
      return super.put(key,value);
   }
   
   /** Ajout/remplacement en fin */
   public Object add(String key, String value) {
      remove(key);
      return put(key,value);
   }

   /** Retourne le flux des propriétés originales */
   public String getPropOriginal() { return propOriginal!=null ? propOriginal.toString() : null; }
   
   public synchronized void load(InputStream in) throws IOException { load(in,false); }
   public synchronized void load(InputStream in,boolean flagKeepOriginal) throws IOException {
      
      // Pour conserver le flux original
      if( flagKeepOriginal ) propOriginal = new StringBuilder();

      prop = new Vector<ConfigurationItem>();
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      // Je lis les propriétés de la configuration
      String s;
      int line = 0;
      while( (s = br.readLine()) != null ) {
         if( flagKeepOriginal ) propOriginal.append(s+"\n");
         line++;
         if( s.trim().length() == 0 ) {
            prop.addElement(new ConfigurationItem(" ", null));
            continue;
         }
         // Simple commentaire ou proposition de clé "#Cle  = valeur"
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

            if( simpleComment ) {
               prop.addElement(new ConfigurationItem("#", s));
               continue;
            }
         }

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
            System.err.println("MyProperties reader error line "+line+" => "+e.getMessage());
            prop.addElement(new ConfigurationItem("#", "#Error: "+s));
            continue;
         }

         //         Aladin.trace(4, "MyProperties.load() [" + key + "] = [" + value + "]");
         put(key, value,true);
      }
      br.close();

   }

   public void store(OutputStream out, String comments) throws IOException {
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));

      for( ConfigurationItem item : prop ) {
         //         if( item.key.equals("#") ) bw.write(item.value); // Commentaires
         //         else if( item.key.trim().length() > 0 ) bw.write(Util.align(item.key, 20) +" = "+ item.value); // Propriétés
         bw.write(item.toString());
         bw.newLine();
      }
      bw.flush();
   }


   /**
    * Classe permettant la mémorisation d'un propriété, c'est-à-dire un couple
    * (clé,valeur)
    */
   private class ConfigurationItem {
      protected String key;  // Clé associée à la propriété
      protected String value; // valeur associée à la propriété

      private ConfigurationItem(String key, String value) {
         this.key = key;
         this.value = value;
      }

//      public String toString() {
//         if( key.equals("#")) return value; // Commentaire unique (pour compatibilité)
//         return Util.align(key, 20) +" = "+ value; // Propriétés
//      }
      
      public String toString() {
         if( key.equals("#")) return value; // Commentaire unique (pour compatibilité)
         if( key==null || value==null ) return "";
         if( value.indexOf('\t')==-1 ) return  Util.align(key, 20) +" = "+ value; // Propriété simple
         StringBuilder s = new StringBuilder();
         Tok tok = new Tok(value,"\t");
         while( tok.hasMoreTokens() ) {
            if( s.length()>0 ) s.append(Util.CR);
            s.append( Util.align(key, 20) +" = "+ tok.nextToken());
         }
         return s.toString();
      }

   }


}