package cds.allsky;

import java.io.File;

import cds.moc.Healpix;
import cds.tools.pixtools.Util;

public class Cleaner implements Progressive {
   public Context context;

   public Cleaner(Context context) {
      this.context = context;
   }
   
   public void clean(String extension) {
      int order;
      // Parcours de tous les répertoire Norder?? trouvés
      String path = context.getOutputPath();
      for( File nOrder : (new File(path)).listFiles() ) {
         String name = nOrder.getName();
         if( !name.startsWith("Norder") ) continue;
         if( !nOrder.isDirectory() ) continue;
         try { order = Integer.parseInt(name.substring(6)); }
         catch( Exception e ) { continue; }
         
         // Traitement de toutes les tuiles du niveau
         long maxNpix = Healpix.pow2(context.order);
         maxNpix = 12*maxNpix*maxNpix;
         for( long npix=0; npix<maxNpix; npix++ ) {
            String filename = Util.getFilePath(path, order, npix)+extension;
            File fitsfile = new File(filename);
            if( fitsfile.exists() ) {
               try {
                  if (!fitsfile.delete())
                     context.error("Could not remove file " + filename);
               } catch (SecurityException e) {
                  context.error("Could not remove file " + filename + "\n\t"+e.getMessage());
               }  
            }
         }
      }
   }

   @Override
   public int getProgress() {
      return 0;
   }
}