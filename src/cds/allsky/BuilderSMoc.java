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

package cds.allsky;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

import cds.aladin.Aladin;
import cds.aladin.HealpixProgen;
import cds.aladin.Localisation;
import cds.aladin.SourceFootprint;
import cds.aladin.stc.STCObj;
import cds.moc.Moc;
import cds.moc.SMoc;
import cds.moc.TMoc;
import cds.tools.pixtools.Util;

/** Construction d'un MOC à partir des données HpxFinder
 * Cf BuilderTMoc et BuilderSTMoc
 *
 * @author Pierre Fernique
 */
public class BuilderSMoc extends Builder {

   protected int hipsOrder;              // L'ordre du HiPS
   protected int spaceOrder,timeOrder;   // Les orders du MOC à générer
   protected long maxSize = -1L;         // La limite de taille du MOC à générer (-1 si aucune)
   protected String ruleSize = null;     // la règle de dégradation de la résolution du MOC à générer (ex: ttts), null si aucune
   protected long lastAdjustTime=-1L;    // Dernière date d'ajustement de la taille du MOC
   
   protected int statNbFile;
   protected long startTime,totalTime;

   private SMoc smoc = null;       
   
   /**
    * Création du générateur de l'arbre des index.
    * @param context
    */
   public BuilderSMoc(Context context) {
      super(context);
   }

   // Pour le moment on utilise toujours BuilderMoc - donc jamais appelé directement
   public Action getAction() { return Action.SMOC; }

   public void run() throws Exception {
      build();
   }

   // Valide la cohérence des paramètres pour la création des tuiles JPEG
   public void validateContext() throws Exception {
      validateOutput();
      validateIndex();
      
      int frame = context.getFrame();
      if( frame!=Localisation.ICRS ) {
         throw new Exception("Only applicable for equatorial HiPS => abort");
      }

      hipsOrder = context.getOrder();
      if( hipsOrder==-1 ) {
         hipsOrder = Util.getMaxOrderByPath( context.getHpxFinderPath() );
         if( hipsOrder==-1 ) throw new Exception("HpxFinder seems to be not yet ready ! (order=-1)");
         context.info("HiPS order retrieved from HpxFinder => "+hipsOrder);
         context.setOrder(hipsOrder); // juste pour que les statistiques de progression s'affichent correctement
      }

      context.mocIndex=null;
      context.initRegion();
      
      // spaceOrder explicitement fourni par l'utilisateur ?
      if( context.getMocOrder()!=-1 ) {
         spaceOrder = context.getMocOrder();
         
         // On ne peut prendre un MOC order supérieur à la résolution nomimale
         int tileOrder = context.getTileOrder();
         if( spaceOrder>tileOrder+hipsOrder ) {
            context.warning("Too high mocOrder ("+spaceOrder+") => assume "+(tileOrder+hipsOrder));
            spaceOrder=tileOrder+hipsOrder;
         }
      }
      
      // Sinon order des tuiles
      else spaceOrder = hipsOrder;

      // Order temporel
      timeOrder = context.getTMocOrder();
      if( timeOrder==-1 ) timeOrder=41;   //Par défaut 17mn
      
      // Limite en taille
      maxSize = context.getMocMaxSize();
      ruleSize = context.getMocRuleSize();
   }

   /** Demande d'affichage des stats via Task() */
   public void showStatistics() {
      context.showJpgStat(statNbFile, totalTime,0,0);
      info();
   }
   
   /** Parcours des tuiles HpxFinder via le Moc HpxFinder. Pour chacun d'elles
    * extraction des informations temporelles et spatiales. Génération du MOC
    * correspondant.
    * Utilisation d'un cache des derniers progéniteurs traités pour éviter de
    * reprendre le même progéniteur couvrant plusieurs tuiles 
    */
   public void build() throws Exception {
      initStat();

      String hpxFinder = context.getHpxFinderPath();
      
      SMoc mocFinder = new SMoc();
      mocFinder.read(hpxFinder+Util.FS+"Moc.fits");
      mocFinder.setMocOrder(hipsOrder);
      
      long progress=0L;
      context.setProgressMax(mocFinder.getNbValues());
      
      cleanIt();
      initIt();   
      info();
      
      int maxCache = 200;
      ArrayDeque<String> cache = new ArrayDeque<>(maxCache);
      
      Iterator<Long> it = mocFinder.valIterator();
      while( it.hasNext() ) {
         long npix = it.next();
         String file = Util.getFilePath(hpxFinder, hipsOrder, npix);
         HealpixProgen out = createLeave(file);
         if( out==null ) {
            context.warning("Missing HpxFinder tile "+hipsOrder+"/"+npix+" => ignored ("+file+")");
            continue;
         }
         for( String key : out ) {
            String json = out.get(key).getJson();
            
            try {
               // On teste si jamais on a déjà traité ce progéniteur sur les maxCache derniers
               String path = cds.tools.Util.extractJSON("path", json);
               if( cache.contains(path) ) continue;
               if( cache.size()>maxCache ) cache.removeFirst();
               cache.add(path);

               TMoc tmoc = getTMoc(timeOrder, json);
               SMoc smoc = getSMoc(spaceOrder, json);
               addIt(tmoc,smoc);
            
            } catch( MocParsingException e1 ) {
               throw new Exception();
            } catch( Exception e ) {
               context.warning("parsing error => "+json);
               continue;
            }
         }
         context.setProgress( progress++ );
      }
      writeIt();
   }
   
   class MocParsingException extends Exception { }
   
   // retourne true s'il est temps de tester un ajustement de taille
   protected boolean mustAdjustSize(Moc m, boolean force) {
      if( force ) m.flush();
      return m.bufferSize()==0;
   }
   
   protected void reduction(Moc m) throws Exception { m.reduction(maxSize); }
   
   protected void adjustSize(Moc m, boolean force) throws Exception {
      if( maxSize==-1 ) return;
      if( !mustAdjustSize(m,force) ) return;
      if( m.getMem() <= maxSize ) return;
      
      reduction(m);
      
      int o = m.getTimeOrder();
      if( o!=-1 ) timeOrder = o;
      o = m.getSpaceOrder();
      if( o!=-1 ) spaceOrder = o;
   }

   protected void initIt() throws Exception {
      smoc = new SMoc(spaceOrder);
      smoc.bufferOn();
    }
   
   protected void info() {
      String s = maxSize>0 ? " maxSize="+cds.tools.Util.getUnitDisk(maxSize):"";
      String s1 = smoc.getMem()>0 ? " currentSize="+cds.tools.Util.getUnitDisk(smoc.getMem()):"";
      context.info("SMOC generation (spaceOrder="+smoc.getSpaceOrder()+s+s1+")...");
   }
   
   /** Extraction du TMOC à partir des informations temporelles dans les propriétés JSON de la tuile */
   protected TMoc getTMoc(int order, String json) throws Exception { return null; }
   
   /** Extraction du SMOC qui couvre le STC indiqué dans les propriétés JSON de la tuile */
   protected SMoc getSMoc(int order,String json) throws Exception {
      String stc = cds.tools.Util.extractJSON("stc", json);
      SourceFootprint sf = new SourceFootprint();
      sf.setStcs(Double.NaN,Double.NaN, stc);
      List<STCObj> listStcs = sf.getStcObjects();
      if( listStcs==null ) throw new Exception();
      return  Aladin.createMocRegion(listStcs,order,true);
   }
   
   protected void addIt(TMoc tmoc1, SMoc smoc1) throws Exception {
      smoc.add(smoc1);
      adjustSize(smoc,false);
   }
   
   protected void cleanIt() throws Exception {
      String file = context.getOutputPath()+Util.FS+"SMoc.fits";
      (new File(file)).delete();      
   }
   
   protected void writeIt() throws Exception {
      adjustSize(smoc,true);
      if( smoc.isEmpty() ) throw new Exception("Empty MOC => not generated");
      String file = context.getOutputPath()+Util.FS+"SMoc.fits";
      smoc.write(file);
   }

   protected void initStat() { statNbFile=0; startTime = System.currentTimeMillis(); }

   // Mise à jour des stats
   protected void updateStat() {
      statNbFile++;
      totalTime = System.currentTimeMillis()-startTime;
   }
   
   /** Construction d'une tuile HpxFinder. Lit le fichier est map les entrées de l'index
    * dans une TreeMap */
   protected HealpixProgen createLeave(String file) throws Exception {
      File f = new File(file);
      if( !f.exists() ) return null;
      HealpixProgen out = new HealpixProgen();
      out.loadStream( new FileInputStream(f));
      updateStat();
      return out;
   }
}
