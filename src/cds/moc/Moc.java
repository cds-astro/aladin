package cds.moc;

/**
 * MOC examples
 * @author Pierre Fernique [CDS]
 */
public class Moc {
   
   static void loadAndTest(String[] args) {
      try {
         HealpixMoc moc = new HealpixMoc();
         String file = args[0];
         moc.read(file,HealpixMoc.FITS);
         
         double lon = Double.parseDouble(args[1]);
         double lat = Double.parseDouble(args[2]);
         int inside = moc.contains(lon,lat);
         switch(inside) {
            case HealpixMoc.IN: System.out.println("This MOC contains ("+lon+","+lat+")"); break; 
            case HealpixMoc.OUT: System.out.println("This MOC do not contains ("+lon+","+lat+")"); break; 
            case HealpixMoc.INBORDER: System.out.println("This MOC may be contains ("+lon+","+lat+") (in the border)"); break; 
         }
         
      } catch( Exception e ) {
         e.printStackTrace();
         System.out.println("Usage: moc MocFile.fits lon lat");
      }
   }
   
   static void loadASCIIWriteFITS(String[] args) {
      try {
         HealpixMoc moc = new HealpixMoc();
         
         String fileIn = args[0];
         moc.read(fileIn,HealpixMoc.ASCII);
         
         String fileOut = args[1];
         moc.write(fileOut,HealpixMoc.FITS);
         
      } catch( Exception e ) {
         e.printStackTrace();
         System.out.println("Usage: moc InputFile.txt OutputFile.fits");
      }
   }
   
   public static void main(String[] args) {
      loadAndTest(args);
//      loadASCIIWriteFITS(args);
   }
}
