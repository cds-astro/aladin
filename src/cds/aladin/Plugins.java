// Copyright 1999-2020 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin Desktop.
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
//    along with Aladin Desktop.
//

package cds.aladin;

import java.awt.BorderLayout;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import cds.tools.Util;

/**
 * Permet le chargement des plugins Aladin.
 * Parcours récursivement le répertoire $HOME/.aladin/Plugins (le crée si nécessaire)
 * Toutes les classes qui étendent cds.aladin.AladinPlugin sont loadées.
 * 
 * Elles peuvent se trouver dans un fichier jar et/ou dans un package.
 * Elles peuvent utiliser des librairies jar ou des classes externes qui doivent
 * se trouver également dans le répertoire des plugins.
 * 
 * Dans le cas d'un fichier jar, le nom du fichier jar doit reprendre le nom
 * de la classe du plugin pour pouvoir être supprimé via le bouton "remove" du
 * "plugin controller". Si plusieurs plugins sont regroupés dans un fichier jar, ils
 * seront supprimés simultanément.
 */
public class Plugins extends ClassLoader implements Runnable,ListModel,Comparator
                       ,DropTargetListener, DragSourceListener, DragGestureListener{
       
    private Aladin aladin;      // reference à Aladin
    protected Vector plugs;     // Liste des plugins trouvés
//    private String path=null;
    private boolean flagErrorDir=false;  // true s'il y a un problème pour le répertoire des plugins
    
    static private char FS = Util.FS.charAt(0);
    
    static JFrame controleur=null;        // Le frame du controleur de plugin
    private PluginDescription plugDesc;           // la description courante
    private JButton removePlug;                   // Le bouton pour supprimer des plugins
    private JList listPlugs;                       // Liste des plugins
    
    private final MyClassloader classLoader;  // Mon propre classLoader qui peut ajouter des URL/Path
    
    public Plugins(Aladin aladin) {
       this.aladin = aladin;
       plugs = new Vector();
       
       classLoader = new MyClassloader(new URL[0], this.getClass().getClassLoader());
       
       scanDir(getPlugPath());
       
//        Recherche des plugins dans mon répertoire de développement
//       System.out.println("Chargement des plugins locaux");
//       scanDir("C:\\Users\\Pierre\\Documents\\Développements\\AladinGit\\bin");


//        (juste pour me simplifier la vie)
//       if( aladin.PROTO && aladin.levelTrace>=3 ) {
//          try {
//             String dir = System.getProperty("user.home")
//             +FS+"Mes Documents"
//             +FS+"Workspace"
//             +FS+"aladin"
//             +FS+"bin";
//
//             scan(new File(dir));
//
//          } catch (Throwable t) {
//             t.printStackTrace();
//          }
//       }     
    }
    
    public void dragGestureRecognized(DragGestureEvent dragGestureEvent) { }
    public void dragEnter(DropTargetDragEvent dropTargetDragEvent) {
       dropTargetDragEvent.acceptDrag (DnDConstants.ACTION_COPY_OR_MOVE);
    }
    public void dragExit (DropTargetEvent dropTargetEvent) {}
    public void dragOver (DropTargetDragEvent dropTargetDragEvent) {}
    public void dropActionChanged (DropTargetDragEvent dropTargetDragEvent){}
    public void dragDropEnd(DragSourceDropEvent DragSourceDropEvent){}
    public void dragEnter(DragSourceDragEvent DragSourceDragEvent){}
    public void dragExit(DragSourceEvent DragSourceEvent){}
    public void dragOver(DragSourceDragEvent DragSourceDragEvent){}
    public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent){}

    public synchronized void drop(DropTargetDropEvent dropTargetDropEvent) {
       try {
          Transferable tr = dropTargetDropEvent.getTransferable();
          if( tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {
             dropTargetDropEvent.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
             java.util.List fileList = (java.util.List) tr.getTransferData(DataFlavor.javaFileListFlavor);
             Iterator iterator = fileList.iterator();
             while( iterator.hasNext() ) {
                File file = (File) iterator.next();
                copyInPlug(file);
 //System.out.println(file.getAbsolutePath());
             }
             dropTargetDropEvent.getDropTargetContext().dropComplete(true);
             
          } else dropTargetDropEvent.rejectDrop();
          reload();
          
       } catch( Exception e ) {
          dropTargetDropEvent.rejectDrop();
       }
    }
    
    /** Recopie du plugin dans le répertoire adéquat (suite à un Drag&Drop) */
    private void copyInPlug(File f) throws Exception {
       String name = f.getName();
       File outf = new File(getPlugPath()+FS+name);
       RandomAccessFile in = new RandomAccessFile(f,"r");
       RandomAccessFile out = new RandomAccessFile(outf,"rw");
       byte buf[] = new byte[512];
       long size = in.length();
       while( size>0 ) {
          int n = in.read(buf);
          out.write(buf,0,n);
          size-=n;
       }
       in.close();
       out.close();
    }
    
    /** Scanning des plugins présent dans le répertoire indiqué */
    private void scanDir(String dir) {
       try {
          addInClassPath(dir);
          scan(new File(dir));
                                       
       } catch (Throwable t) {
          if( Aladin.levelTrace>=3) t.printStackTrace();
       }
    }
    
    /** Nécessaire à partir de Jvm 1.0 */
    public class MyClassloader extends URLClassLoader {
       public MyClassloader(URL[] urls, ClassLoader parent) {
           super(urls, parent);
       }

       public void addURL(URL url) {
           super.addURL(url);
       }
   }
    
    /** PEUT ETRE INUTILE: LE CLASSLOADER CONSERVE LE PATH VIA byteOfClass()
     * Ajout du dir dans le CLASSPATH du classLoader
     */
    private void addInClassPath(String dir) throws Exception {
//System.out.println("J'ajoute ["+dir+"] au CLASSPATH");          
       try {
         URL u = new File(dir).toURL();
         classLoader.addURL(u);
         
// CETTE METHODE N'EST PLUS SUPPORTEE A PARTIR DE JVM1.9 => REMPLACER PAR MyClassLoader
//          ClassLoader apploader = ClassLoader.getSystemClassLoader();
//         URLClassLoader sysloader = (URLClassLoader)apploader;
//          Class sysclass = URLClassLoader.class;
//          Method method = sysclass.getDeclaredMethod("addURL",new Class[]{URL.class});
//          method.setAccessible(true);
//          method.invoke(sysloader,new Object[]{ u });
      } catch( Throwable e ) { 
         if( Aladin.levelTrace>=3 ) e.printStackTrace();
      }
    }
    
    /** Construit le répertoire des plugins et le crée si nécessaire */
    protected String getPlugPath() {
       String dir = System.getProperty("user.home")
       +FS+aladin.CACHE
       +FS+"Plugins";
       try {
          File f = new File(dir);
          if( !f.isDirectory() ) if( !f.mkdir() ) throw new Exception();
       } catch( Exception e ) {
          flagErrorDir=true;
//          aladin.warning("Your plugin directory can not be created !\n["+dir+"]");
          if( Aladin.levelTrace>=3 ) e.printStackTrace();
       }
       return dir;
    }
    
    /** Génère et affiche la fenêtre du controleur des plugins
     * Lance également un thread pour vérifier l'état des plugins
     */
    protected void showFrame() {
       if( controleur==null ) {
          JTextArea t;
          JTextField g;
          JButton b;
          controleur = new JFrame(aladin.chaine.getString("PLUGINFO"));
          Aladin.setIcon(controleur);
          controleur.setLocation(50,100);
          controleur.addWindowListener(new WindowAdapter() {
             public void windowClosing(WindowEvent e) { controleur.setVisible(false); }
          });
          JPanel p = new JPanel(new BorderLayout(5,5));
          controleur.getContentPane().add(p);
          listPlugs = new JList(this);
          listPlugs.setVisibleRowCount(10);
          listPlugs.setFixedCellWidth(200);
          listPlugs.addMouseListener(new MouseAdapter() {
             public void mouseReleased(MouseEvent e) { selectionne(); }
          });
          p.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
          JPanel p1 = new JPanel(new BorderLayout());
          p1.add(t=new JTextArea(aladin.chaine.getString("PLUGWARNING")),BorderLayout.NORTH);
          t.setWrapStyleWord(true);
          t.setLineWrap(true);
          t.setEditable(false);
          t.setFont(t.getFont().deriveFont(Font.ITALIC));
          t.setBackground(controleur.getContentPane().getBackground());
          
          JPanel p2 = new JPanel();
          p2.add(new JLabel(aladin.chaine.getString("PLUGLOC")));
          p2.add(g = new JTextField(getPlugPath(),35));
          g.setEditable(false);
          p2.add(b=new JButton(aladin.chaine.getString("PLUGRELOAD")));
          b.setToolTipText(aladin.chaine.getString("PLUGRELOADTIP"));
          b.setMargin(new Insets(0,0,0,0));
          b.setFont(b.getFont().deriveFont(Font.PLAIN));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { reload(); }
          });
          p1.add(p2,BorderLayout.SOUTH);
          
          
          p.add(p1,BorderLayout.NORTH);
          p.add(new JScrollPane(listPlugs),BorderLayout.WEST);
          p.add(plugDesc=new PluginDescription(aladin),BorderLayout.CENTER);
          
          p1 = new JPanel();
          p1.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
          p1.add(b=new JButton(aladin.chaine.getString("PLUGDOWNLOAD")));
          b.setToolTipText(aladin.chaine.getString("PLUGDOWNLOADTIP"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                aladin.glu.showDocument("Http", "http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=plugRep", true);
             }
          });
          p1.add(b=new JButton(aladin.chaine.getString("PLUGUPLOAD")));
          b.setToolTipText(aladin.chaine.getString("PLUGUPLOADTIP"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                aladin.glu.showDocument("mailto:plugin@aladin.u-strasbg.fr?" +
                      "Subject=Aladin plugin&Body=%0A%0ADear Aladin team,%0A%0A" +
                      "You will find, joined to this mail, my Aladin plugin source code%0A%0A" +
                      "By this letter, I certify that this code does not content any spyware " +
                      "or other  malicious code. I certify the content of the author lists " +
                      "and other descriptions in the source code.%0A%0A" +
                      "By this letter, I accept that the CDS team compiles this plugin " +
                      "and distributes the resulting compiled code [with/without] " +
                      "the source version.%0A%0A[***DO NOT FORGET TO ATTACH YOUR PLUGIN SOURCE CODE ***]");
             }
          });
          p1.add(b=new JButton(aladin.chaine.getString("PLUGADD")));
          b.setToolTipText(aladin.chaine.getString("PLUGADDTIP"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { addPlugin(); }
          });
          p1.add(b=removePlug=new JButton(aladin.chaine.getString("PLUGREMOVE")));
          b.setToolTipText(aladin.chaine.getString("PLUGREMOVETIP"));
          b.setEnabled(false);
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { removePlugin(); }
          });
          p1.add(b=new JButton(aladin.chaine.getString("PLUGMOREINFO")));
          b.setToolTipText(aladin.chaine.getString("PLUGMOREINFOTIP"));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 aladin.glu.showDocument("Http", "http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=plugins", true);
             }
          });
          p1.add(b=new JButton(aladin.chaine.getString("CLOSE")));
          b.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) { controleur.setVisible(false); }
          });
          p.add(p1,BorderLayout.SOUTH);
          
          // Pour gérer le DnD des plugins
          new DropTarget(listPlugs, this);
          DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                listPlugs, DnDConstants.ACTION_COPY_OR_MOVE, this);
          
          // Un petit log
          aladin.log("PluginControl",getSize()+" plugin"+(getSize()>1?"s":""));
       }
       
       controleur.pack();
       controleur.setVisible(true);
       if( thread==null ) ( setThread(new Thread(this,"AladinPluginControl"))).start();
    }
    
    /** Thread de vérification de l'état des plugins. S'arrête lorsque la fenêtre
     * du controleur des plugins est fermée */
    public void run() {
       Util.pause(1000);
       while(controleur!=null && controleur.isVisible()) {
          plugDesc.resume();
          Util.pause(1000);
       }
       setThread(null);
    }
  
    /** Gestion du thread de controle de l'état des plugins */
    private Thread thread=null;
    synchronized private Thread setThread(Thread t) { return thread=t; }  
    
    /** Montre la description du plugin correspondant à la sélection dans la liste */
    private void selectionne() {                
       String name = (String)listPlugs.getSelectedValue();
       AladinPlugin ap = find(name);
       plugDesc.setPlugin(ap);
       removePlug.setEnabled( listPlugs.getSelectedValue()!=null ); 
    }
    
    /** Suppression de tous les plugins sélectionnées dans la liste */
    private void removePlugin() {
       boolean ok;
       if( !aladin.confirmation(controleur,aladin.chaine.getString("PLUGCONF")) ) return;
       Object s[] = listPlugs.getSelectedValues();
       for( int i=0; i<s.length; i++ ) {
          try {
             AladinPlugin ap = find((String)s[i]);
             String s1 = ap.getClass().getName();
             File f = new File(getPlugPath()+FS+s1+".class");
             if( !(ok=f.delete()) ) {
                int j = s1.indexOf('.');
                if( j>0 ) s1 = s1.substring(j+1);
                f=new File(getPlugPath()+FS+s1+".jar");
                ok=f.delete();
             }
if( ok ) Aladin.trace(3,"Remove "+f.getCanonicalPath());
else aladin.error("Cannot arrive to remove ["+f.getCanonicalPath()+"]");
          } catch( Exception e ) { e.printStackTrace(); }
       }
       reload();
    }
    
    /** Ajout d'un plugin par boite de dialogue */
    private void addPlugin() {
       try {
          FileDialog f = new FileDialog(controleur,"Plugin");
          f.setVisible(true);
          if( f.getFile()==null ) return;
          File file = new File(f.getDirectory()+f.getFile());
          copyInPlug(file); 
Aladin.trace(3,"Copy "+file.getCanonicalPath());
          reload();
       } catch( Exception e ) { e.printStackTrace(); }
    }
    
    /** Recharge les plugins, remet à jour la liste dans le controleur des plugins
     * et remet à jour le sous-menu plugin */
    private void reload() {
       if( flagErrorDir ) {
          Aladin.error(controleur,"No access to the Aladin plugin directory !");
          return;
       }
       plugDesc.setPlugin(null);
       plugs.clear();
       scanDir(getPlugPath());
       listListener.contentsChanged(
             new ListDataEvent(this,ListDataEvent.CONTENTS_CHANGED,0,plugs.size()));
       if( plugs.size()>0 ) {
          listPlugs.setSelectedIndex(0);
          selectionne();
       }
       aladin.pluginReload();
    }
    
    /**
     * Charge en tant que tableau de bytes une classe particulière
     * @param path répertoire de la classe ou fichier jar
     * @param name nom de la classe sans l'extension (ex: cds.aladin.Aladin)
     * @return
     */
//    private byte [] bytesOfClass(String path, String name) {
//        String filename;
////System.out.println("bytesOfClass ["+path+"] ["+name+"]");
//        try { 
//           if( path.endsWith(".jar") ) {
//              ZipFile zipFile = new ZipFile(path);
//              filename = name.replace('.','/')+".class"; 
//              ZipEntry  z = new ZipEntry(filename);
//              MyInputStream f = new MyInputStream(zipFile.getInputStream(z));
//              byte buf[] = f.readFully();
//              f.close();
//              return buf;
//           } else {
//              filename = name.replace('.',FS)+".class";
//              RandomAccessFile f = new RandomAccessFile(new File(path,filename),"r");
//              byte buf[] = new byte[ (int)f.length() ];
//              f.readFully(buf);
//              f.close();
//              return buf;
//           }
//        } catch (Exception e) { e.printStackTrace(); return null; }
//    }

    /**
     * Surcharge du findClass pour permettre de charger des classes
     * même si Aladin est packagé en jar. De fait je ne sais pas trop
     * pourquoi ça marche ;-)
     */
//    protected Class findClass(String name) throws ClassNotFoundException {
//       byte[]  classBytes;
//       try { classBytes = bytesOfClass(path,name); }
//       catch( Exception e ) {
//          if( Aladin.levelTrace>=3 ) e.printStackTrace();
//          throw new ClassNotFoundException(name);
//       }
//       return defineClass(name, classBytes, 0, classBytes.length);
//    }
    
    /**
     * Scan d'un répertoire à la recherche de plugins compatible Aladin.
     * Ils doivent nécessairement étendre la classe cds.aladin.AladinPlugins.
     * Ajoute au Vector plugs, les plugins trouvés après les avoir instanciés
     * @param dir Le répertoire
     */
    private void scan(File dir) {
Aladin.trace(3,"Scanning plugs in ["+dir+"]");       
       scan(dir,0);
       Collections.sort(plugs,this);
    }
    
    public int compare(Object a1,Object b1) {
       AladinPlugin a = (AladinPlugin)a1;
       AladinPlugin b = (AladinPlugin)b1;
       int ap = a.category()==null ? 0 : 1;
       int bp = b.category()==null ? 0 : 1;       
       if( ap==bp ) return a.menu().compareTo(b.menu());
       else return bp-ap;
    }
    
    // Méthode récursive (voir scan())
    private void scan(File dir,int level) {
       if( !dir.isDirectory() ) return;
       File f[] = dir.listFiles();
       for( int i=0; i<f.length; i++ ) {
          if( f[i].isDirectory() ) scan(f[i],level+1);
          else {
             String name = f[i].getAbsolutePath();
             if( name.endsWith(".jar") ) { scanJar(name); continue; }
             if( !name.endsWith(".class") ) continue;
             if( name.indexOf('$')>=0 ) continue;
             int pos = name.length()+1;
             for( int j=level; j>=0; j-- ) pos = name.lastIndexOf(FS,pos-1);
             String n = name.substring(pos>=0 ? pos+1 : 0 , name.length()-6).replace(FS, '.');
//             path = name.substring(0,pos);
             try {
//                Class plugin = ClassLoader.getSystemClassLoader().loadClass(n);
                Class plugin = classLoader.loadClass(n);
                
                tryToAdd(n,plugin);
             } catch( Throwable e ) {
                System.err.println("Cannot load plugin "+name+"\n ==> "+e.getMessage());
             }
          }
       }
    }
    
    // Retourne false si le plugin indique une version d'Aladin et que celle-ci est supérieure
    // à la version d'Aladin qui exécute le plugin
    private boolean testVersion(AladinPlugin ap) {
       String version = ap.version();
       if( version==null ) return true;
       int offset = Util.indexOfIgnoreCase(version, "Aladin v", 0);
       if( offset<0 ) return true;
       if( aladin.realNumVersion(Aladin.VERSION)<aladin.realNumVersion(version.substring(offset+7)) ) return false;
       return true;
    }
    
    /** Essaye de loader le plugin passé en paramètre
     * @param name nom du plugin (sans suffixe mais avec les packages en préfixe)
     * @param plugin class du plugin
     * @return true si le plugin a été ajouté
     * @throws Throwable
     */
    private boolean tryToAdd(String name,Class plugin) throws Throwable {
       if( !name.equals("cds.aladin.AladinPlugin") 
             && cds.aladin.AladinPlugin.class.isAssignableFrom( plugin ) ) {        
          Constructor ct = plugin.getConstructor(new Class[] {});
          AladinPlugin ap = (AladinPlugin) ct.newInstance(new Object[] {});
          if( !testVersion(ap) ) throw new Exception("Too old Aladin version for this plugin ["+ap.menu()+"=> "+ap.version()+"]");
          ap.setAladin(aladin);
          if( find(ap.menu())!=null ) throw new Exception("Duplicated plugin ["+ap.menu()+"]");
          Aladin.trace(3,"Plugin ["+name+"] loaded => "+ap.menu());              
          plugs.add(ap);
          return true;
       }
       return false;
    }
        
    /** Examine les plugins à partir d'un JAR **/
    private void scanJar(String jarName) {
//        path=jarName;
        boolean trouve=false;
        try {
           addInClassPath(jarName);
           ZipFile zipFile = new ZipFile(jarName);
           Enumeration e1 = zipFile.entries();
           while( e1.hasMoreElements() ) {
            ZipEntry entry = (ZipEntry)e1.nextElement();
         
            String name = entry.getName();
            if( !name.endsWith(".class") ) continue;
            if( name.indexOf('$')>=0 ) continue;
            
            name = name.substring(0, name.length() - 6 ); 
            name = name.replace('/','.');
            
            try {
//               Class plugin = ClassLoader.getSystemClassLoader().loadClass(name);
               Class plugin = classLoader.loadClass(name);
               trouve= trouve || tryToAdd(name,plugin);
            } catch( Throwable e ) {
               e.printStackTrace();
               System.err.println("Cannot load plugin "+name+"\n ==> "+e.getMessage());
            }
            
          }
          zipFile.close();
          
          // S'il sagit d'un jar file qui ne contient pas de plugin, il faut
          // que je l'ajoute au classpath car le classloader ne le reconnaitrait pas
//          if( !trouve ) addInClassPath(jarName);
        }
        catch(Exception e){ e.printStackTrace(); }
    }


    /**
     * Retourne la liste des noms des plugins en ordre alphabétique suffixés
     * par leur catégorie respectives. Exemple "Image/Rotation d'image"
     * @return Liste des noms des plugins
     */
    protected String [] getNames() {
       String [] names = new String[plugs.size()];
       Enumeration e = plugs.elements();
       for(int i=0; e.hasMoreElements(); i++ ) {
          AladinPlugin ap = (AladinPlugin)e.nextElement();
          names[i] = (ap.category()!=null ? ap.category()+"/":"")+ap.menu();
       }
       return names;
    }
    
    /**
     * Retourne le plugin correspondant au nom passé en paramètre 
     * @param name nom du plugin recherché (ou sa commande script)
     * @return plugin Aladin
     */
    protected AladinPlugin find(String name) {
       Enumeration e = plugs.elements();
       while( e.hasMoreElements() ) {
          AladinPlugin ap = (AladinPlugin) e.nextElement();
          if( ap.menu().equalsIgnoreCase(name) ) return ap;
          if( name.equalsIgnoreCase( ap.scriptCommand() ) ) return ap;
       }
       return null;
    }
    
    /**
     * Retourne le plugin correspondant à la commande script passée en paramètre
     * @param cmd commande associée au plugin recherché
     * @return plugin Aladin
     */
    protected AladinPlugin findScript(String cmd) {
       Enumeration e = plugs.elements();
       while( e.hasMoreElements() ) {
          AladinPlugin ap = (AladinPlugin) e.nextElement();
          String s = ap.scriptCommand();
          if( s!=null && s.equals(cmd) ) return ap;
       }
       return null;
    }
    
    /**
     * Exécution du plugin repéré par son nom
     * @param name nom du plugin à exécuter
     * @return true si le plugin a pu être lancé, false sinon
     */
    protected boolean execPlugin(String name) throws AladinException {
       AladinPlugin ap = find(name);
       if( ap==null ) return false;
       boolean inThread = ap.inSeparatedThread();
Aladin.trace(1,"Exec plugin "+(inThread?"asynchroneously ":"")+" ["+name+"]...");       
       if( inThread ) ap.start();
       else ap.exec();
       return true;
    }
    
    /**
     * Exécution du plugin repéré par sa commande script
     * @param cmd commande script du plugin
     * @param param liste des paramètres
     * @return true si le plugin a pu être lancé, false sinon
     */
    protected String execPluginByScript(String cmd,String param[]) {
       AladinPlugin ap = findScript(cmd);
       if( ap==null ) return "Plugin "+cmd+" not found";
Aladin.trace(1,"Exec plugin by script ["+cmd+"]...");       
       return ap.execScriptCommand(param);
    }
    
    /**
     * Appelle la méthode cleanup() pour chaque plugin 
     * Permet de faire du ménage avant la fermeture d'Aladin
     */
    protected void cleanup() {
    	Enumeration e = plugs.elements();
    	while( e.hasMoreElements() ) {
    		AladinPlugin ap = (AladinPlugin) e.nextElement();
    		try { ap.cleanup(); } catch( Exception e1 ) {}
    	}
    }

    /**
     * Appelle la méthode isSync() pour chaque plugin 
     * Retourne true si tous les plugins sont synchronisés
     */
    protected boolean isSync() {
        Enumeration e = plugs.elements();
        while( e.hasMoreElements() ) {
            AladinPlugin ap = (AladinPlugin) e.nextElement();
            if( !ap.hasBeenStarted() ) continue;
            if( !ap.isSync() ) {
               return false;
            }
        }
        return true;
    }


   public Object getElementAt(int index) {
      return ((AladinPlugin)plugs.elementAt(index)).menu();
   }

   public int getSize() { return plugs.size(); }

   public void removeListDataListener(ListDataListener l) { }
   
   private ListDataListener listListener;
   public void addListDataListener(ListDataListener l) {
      listListener=l;
   }
    
}
