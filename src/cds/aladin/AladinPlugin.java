// Copyright 1999-2022 - Universite de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donnees
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

/**
 * An Aladin plugin mechanism allows you to extend Aladin for you own purpose.
 * See <A HREF="http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=plugins">
 * http://aladin.u-strasbg.fr/java/nph-aladin.pl?frame=plugins</A> page for examples.
 * <P>
 * <B>How create your Aladin plugin:</B>
 * <OL>
 * <LI> Write your plugin by extending this abstract class. Notice that you can use your
 *    own java package (instead of cds.aladin package), or no package definition at all.
 *    <UL>
 *      <LI><UL><B>REQUIRED</B> : you must implement these 2 methods:
 *             <LI>String menu() =>   return your menu label
 *             <LI>void exec()   =>   will be called by Aladin for executing your plugin
 *          </UL>   
 *      <LI><UL><B>OPTIONAL</B> : you can extend these methods:
 *             <LI>String author()        => return the author list
 *             <LI>String version()       => return the version number and/or version date
 *             <LI>String category()      => return the plugin category (Image, Catalog, Overlays...)
 *             <LI>String url()           => return an URL to a web site providing the source code, the new releases...
 *             <LI>String description()   => return a short paragraph describing your plugin
 *             <LI>String scriptCommand() => return the command word for allowing script usage of your plugin
 *             <LI>String scriptHelp()    => return the script help of your plugin (syntax specific)
 *             <LI>
 *             <LI>boolean inSeparatedThread() => return true if your plugin has to be executed in a separated thread (false by default)
 *             <LI>boolean isRunning() => => return true when the plugin is activated (see javadoc)
 *             <LI>void cleanup()         => call by Aladin before it shuts down
 *             <LI>String execScriptCommand(String []param) => will be called by Aladin for executing
 *                                       your plugin by script. By default calls process() without
 *                                       parameter  
 *           </UL> 
 *    </UL>                                    
 * <LI> Put the corresponding YourPlugin.class file into the Aladin plugin directory<BR>
 *       => $HOME/.aladin/Plugins
 *       
 * <LI> Restart Aladin and find your plugin into the Plugins menu.
 * </OL>
 * 
 * <B>Material for plugins:</B>
 * <OL>
 * <LI> An Aladin plugin has a reference to the instance of Aladin session: called "aladin"
 * 
 * <LI> An Aladin plugin can access directly to the Aladin stack data via a dedicated
 *    class called AladinData (see the corresponding class javadoc)
 *    <UL>These objects must be created by one of these 4 aladin methods
 *       <LI>aladin.getAladinData("planeLabel")   => provides an AladinData associated to the specified plane
 *       <LI>aladin.getAladinData()               => same for the first selected stack plane
 *       <LI>aladin.getAladinImage()              => same for the background image
 *       <LI>aladin.createAladinData("planeLabel")=> create a new stack plane and return the associated AladinData
 *    </UL>
 *    <EM>example:</EM><BR>
 *         AladinData ad = aladin.getAladinData("PlaneLabel");<BR>
 *         double [] pixels = ad.getPixels();<BR>
 *         ....
 *        
 * <LI> An Aladin plugin can know the Aladin stack plane labels via the dedicated
 *    aladin method :<BR>
 *        String [] labels = aladin.getAladinStack();
 *        
 * <LI> An Aladin plugin can control Aladin via the VOApp interface implemented by Aladin
 * => see documentation at http://aladin.u-strasbg.fr/java/FAQ.htx#VOApp<BR>
 *        <EM>example:</EM><BR>
 *            aladin.execCommand("get Vizier(GSC1.2) m1");<BR>
 *            aladin.execCommand("copy DSS2 CopyOfDSS2");<BR>
 *            aladin.putVOTable(InputStream in,"label");<BR>
 *            ...
 * 
 * <LI> An Aladin plugin can be registered itself as a VOObserver listener to be
 *    called by Aladin when the user click somewhere<BR>
 * => see documentation at http://aladin.u-strasbg.fr/java/FAQ.htx#VOObserver<BR>
 *        ex: aladin.addVOObserver(this)
 * </OL>       
 * <EM>WARNING:</EM> Presently, the Aladin.createAladinData(String) can create Image plane only.
 *          For creating Catalog or Graphical plane you can use VOapp interface
 *          by passing a VOTable stream or executing some script commands.
 * 
 * @author Pierre Fernique
 * @version 1.1 jan 2007 - creation
 */
public abstract class AladinPlugin implements Runnable {
   
   private Thread thread=null;
   private boolean isSuspended=false;
   
   /** Aladin reference : automatically set when the plugin is loaded */
   public Aladin aladin;
   
   /** Return the string identifying this plugin in the Aladin menu.
    * This string will be used as the plugin identifier. So it has to be unique
    * If you want to insert this plugin in a sub-menu, use the category() method */
   public abstract String menu();
   
   /** Method called by Aladin when the user launchs the plugin */
   public abstract void exec() throws AladinException;
   
   /** OPTIONAL: return the author list with or without their affiliations... 
    * Ex : P.Fernique [CDS], T. Boch [CDS] */
   public String author() { return null; };

   /** OPTIONAL: return the version number and/or version date...
    * And the Aladin minimal version number
    * Example: v1.0 - 9 dec 2006 - Aladin v5.426 */
   public String version() { return null; };

   /** OPTIONAL: return a short paragraph describing the plugin...*/
   public String description() { return null; };

   /** OPTIONAL: return the script command (1 word) for allowing the usage
    * of the plugin via script */
   public String scriptCommand() { return null; }
   
   /** OPTIONAL: return the script help associated to the script plugin command
    * The syntax has to follow the Aladin help script syntax. See the example below.
    * "#X:" : Field descriptor - "@cmd" : hyper-link to another command - not folded lines
    * Example: #n:rot - Image rotation              <= name + short description
    *          #s:rot [planeLabel] [angle]          <= synopsys
    *          #d:Image rotation using...           <= full description
    *          #e:rot Image1 35                     <= example
    *          #t:90 and 180° rotations are faster  <= tip
    *          #g:@create,@mview                    <= see also
    */
   public String scriptHelp() { return null; }
   
   /** OPTIONAL: return the plugin category as a path (see examples). Used by Aladin
    * to build the menu and sub-menu according to this path.
    * Prefer the 1 level category.
    * 1 level examples : Image , Catalog, Cube, Overlay, ...
    * 2 level examples : Image/Astrometry, Catalog/Source extraction, ...
    */
   public String category() { return null; };
   
   /** OPTIONAL: return an URL allowing to access to the original site.
    * Generally used to download the source code and/or the new versions
    * Ex: http://aladin.u-strasbg.fr/java/Plugins/RotationImage.java
    * Ex: http://my.beautiful.site/myplugins/index.html
    */
   public String url() { return null; }

   
   /** Optional: return true if the plugin must be executed in a separated thread */
   public boolean inSeparatedThread() { return false; }
   
   /** Optional: return true if the plugin has achieved its current task */
   public boolean isSync() { return true; }
     
   /** Optional: Method called by Aladin to execute the Plugin by script
    * @param param list of params
    * @return "" if ok, otherwise an error message.
    */
   public String execScriptCommand(String [] param) { 
      try { start(); } catch( Exception e ) { return e.getMessage(); }
      return "";
   }
   
   /** OPTIONAL: Method called by Aladin when shutting down and when the user is stopping
    * manually the plugin
    * Override this method if you have to dispose a JFrame when your plugin is stopping.
    * Override this method if your plugin needs to do some clean-up before Aladin shuts down,
    * eg disconnect from a PLASTIC hub
    */
   public void cleanup() {}
   
   /** Use by Aladin to know the plugin state. Can be overided for plugins
    * not running in a separated thread (notably if they use their own window..)
    * @return true if the plugin is active (or running for separated thread); false otherwise
    */
   public boolean isRunning() { return thread!=null && thread.isAlive(); }      
 
   /** Use by Aladin to know the plugin state
    * DO NOT OVERRIDE IT
    */
    protected boolean isSuspended() { return thread!=null && thread.isAlive() && isSuspended; }      
   
   /** Use by Aladin to launch the plugin execution
    * DO NOT OVERRIDE IT
    */
   protected void start() throws AladinException {
      if( isRunning() ) throw new AladinException(AladinData.ERR013);
      error=null;
      if( inSeparatedThread() ) (thread = new Thread(this,"AladinPlugin")).start();
      else exec();
   }
   
   /** Use by Aladin to stop the plugin execution
    * DO NOT OVERRIDE IT
    */
   protected void stop() {
      if( !isRunning() ) return;
      if( thread!=null ) thread.stop();
      cleanup();
   }
   
   /** Use by Aladin to suspend the plugin execution
    * DO NOT OVERRIDE IT
    */
   protected void suspend() {
      if( !isRunning() || thread==null || isSuspended ) return;
      thread.suspend();
      isSuspended=true;
   }
   
   /** Use by Aladin to resume the plugin execution
    * DO NOT OVERRIDE IT
    */
   protected void resume() {
      if( thread==null || !isRunning() && !isSuspended ) return;
      thread.resume();
      isSuspended=false;
   }
   
   private String error=null;
   
   /** Returns the plugin error message, or null if there is not.
    * DO NOT OVERRIDE IT
    */
   protected String getError() { return error; }
   
   /** Use by Aladin to pass the reference to itself 
    * DO NOT OVERIDE IT
    */
   public void setAladin(Aladin aladin) { this.aladin = aladin; }
   
   /** Thread processing
    * DO NOT OVERRIDE IT
    */
   public void run() { 
      hasBeenStarted=true;
      try {
         exec();
      } catch( AladinException e) { 
         e.printStackTrace();
         error=e.getMessage();
      }
   }
   
   private boolean hasBeenStarted=false;
   
   /** Returns true if the plugin has been launched
    * DO NOT OVERRIDE IT
    */
   public boolean hasBeenStarted() { return hasBeenStarted; }
}
