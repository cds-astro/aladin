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

   public MyProperties() {
      super();
      prop = new Vector<ConfigurationItem>();
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

   public synchronized Object put(Object key, Object value) {
      ConfigurationItem item = getItem((String)key);
      if( item == null ) {
         item = new ConfigurationItem((String)key, (String)value);
         prop.addElement(item);
      } else item.value = (String)value;
      return super.put(key,value);
   }

   public synchronized void load(InputStream in) throws IOException {

      prop = new Vector<ConfigurationItem>();
      BufferedReader br = new BufferedReader(new InputStreamReader(in));

      // Je lis les propriétés de la configuration
      String s;
      int line = 0;
      while( (s = br.readLine()) != null ) {
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
         put(key, value);
      }
      br.close();

   }

   public void store(OutputStream out, String comments) throws IOException {
      BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out));

      for( ConfigurationItem item : prop ) {
         if( item.key.equals("#") ) bw.write(item.value); // Commentaires
         else if( item.key.trim().length() > 0 ) bw.write(Util.align(item.key, 20) +" = "+ item.value); // Propriétés
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

   }


}