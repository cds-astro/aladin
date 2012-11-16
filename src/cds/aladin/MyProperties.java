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
   
   // Contient les propri�t�s (ConfigurationItem)
   private Vector<ConfigurationItem>          prop;
   
   private ConfigurationItem getItem(String key) {
      for( ConfigurationItem item : prop ) {
         if( item.key.equals(key) ) return item;
      }
      return null;
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

      // Je lis les propri�t�s de la configuration
      String s;
      int line = 0;
      while( (s = br.readLine()) != null ) {
         line++;
         if( s.trim().length() == 0 ) {
            prop.addElement(new ConfigurationItem(" ", null));
            continue;
         }
         if( s.charAt(0) == '#' ) {
            prop.addElement(new ConfigurationItem("#", s));
            continue;
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
         else if( item.key.trim().length() > 0 ) bw.write(Util.align(item.key, 20) +" = "+ item.value); // Propri�t�s
         bw.newLine();
      }
      bw.flush();
   }

   
    /**
     * Classe permettant la m�morisation d'un propri�t�, c'est-�-dire un couple
     * (cl�,valeur)
     */
    private class ConfigurationItem {
       protected String key;  // Cl� associ�e � la propri�t�
       protected String value; // valeur associ�e � la propri�t�

       private ConfigurationItem(String key, String value) {
          this.key = key;
          this.value = value;
       }
       
    }
    

}