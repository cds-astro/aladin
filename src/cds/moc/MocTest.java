package cds.moc;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Iterator;

public class MocTest {
   
   // Juste pour tester le parsing d'un flux
   private static boolean testStream() throws Exception {
      HealpixMoc moc = new HealpixMoc();
      moc.read("/Documents and Settings/Standard/Mes Documents/Fits et XML/sdss7_nside128.txt",HealpixMoc.ASCII);
      System.out.println(moc);
      return true;
   }
   
   // Tests pour le mode compressé
   private static boolean testCompressed() throws Exception {
      String filename = "/Documents and Settings/Standard/Bureau/Compressed.fits";
      String ref = " 3/1 2 4 7 8 9 10";
      HealpixMoc moc = new HealpixMoc();
      moc.add(ref);
      File f = new File(filename);
      if( f.exists() ) f.delete();
      FileOutputStream fo = new FileOutputStream(f);
      moc.writeFITS(fo,true);
      moc.read(filename,HealpixMoc.FITS);
      System.out.println("testCompressed: ref="+ref+"]\n result => "+moc);
      return true;
   }

   
   // Juste pour tester quelques méthodes */
   private static boolean testBasic() {
      String ref = " 3/10 4/12 4/13 4/14 4/15 4/18 4/22 4/16 4/17 5/19 5/20";
      HealpixMoc moc = new HealpixMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.setCheckUnicity(true);
      moc.add("4/13-18 5/19 20");
      Iterator<long []> it = moc.iterator();
      StringBuffer s = new StringBuffer();
      while( it.hasNext() ) {
         long[] p = it.next();
         s.append(" "+p[0]+"/"+p[1]);
      }
      boolean rep = s.toString().equals(ref);
      if( !rep ) {
         System.out.println("HpixList.testBasic ERROR: \n.get ["+s+"]\n.ref ["+ref+"]\n");
      } else System.out.println("HpixList.testBasic OK");
      return rep;
   }
   
   private static boolean testHierarchy() {
      String ref = "3/10-12 5/128";
      HealpixMoc moc = new HealpixMoc(ref);
      boolean b;
      boolean rep=true;
      System.out.println("REF: "+ref);
      System.out.println("MOC:\n"+moc);
      System.out.println("- 3/11 [asserting true] isIn()="+(b=moc.isIn(3,11))); rep &= b;
      System.out.println("- 3/12 [asserting true] isIn()="+(b=moc.isIn(3,11))); rep &= b;
      System.out.println("- 2/0 [asserting false] isAscendant()="+(b=moc.isAscendant(2,0))); rep &= !b;
      System.out.println("- 1/0 [asserting true] isAscendant()="+(b=moc.isAscendant(1,0))); rep &= b;
      System.out.println("- 6/340000 [asserting false] isDescendant()="+(b=moc.isDescendant(6,340000)));  rep &=!b;
      System.out.println("- 6/515 [asserting true] isDescendant()="+(b=moc.isDescendant(6,514))); rep &=b;
      if( !rep ) System.out.println("HpixList.testContains ERROR:");
      else System.out.println("HpixList.testContains OK");
      return rep;
   }
   
   private static boolean testContains() {
      HealpixMoc moc = new HealpixMoc("2/0 3/10 4/35");
      System.out.println("MOC:\n"+moc);
      boolean b;
      boolean rep=true;
      try { 
         System.out.println("- contains(028.93342,+18.18931) [asserting IN]    => "+(b=moc.contains(028.93342,18.18931)==HealpixMoc.IN)); rep &= b;
         System.out.println("- contains(057.23564,+15.34922) [asserting OUT]   => "+(b=moc.contains(057.23564,15.34922)==HealpixMoc.OUT)); rep &= b;
         System.out.println("- contains(031.89266,+17.07820) [asserting MAYBE] => "+(b=moc.contains(031.89266,17.07820)==HealpixMoc.INBORDER)); rep &= b;
      } catch( Exception e ) {
         e.printStackTrace();
         rep=false;
      }
      if( !rep ) System.out.println("HpixList.testContains ERROR:");
      else System.out.println("HpixList.testContains OK");
      return rep;
  }
   
   private static boolean testFITS() throws Exception {
      HealpixMoc moc = new HealpixMoc();
      moc.add("3/10 4/12-15 18 22");
      moc.setCheckUnicity(true);
      moc.add("4/13-18 5/19 20");
      int mode= HealpixMoc.FITS;
      String ext = (mode==HealpixMoc.FITS?"fits":"txt");
      String file = "/Documents and Settings/Standard/Bureau/HEALPixMOCM."+ext;
      moc.write(file,mode);
      System.out.println("test write ("+ext+") seems OK");
      moc = new HealpixMoc();
      moc.read(file,mode);
      System.out.println("test read seems OK");
      System.out.println("Result:\n"+moc);
      return true;
   }
   
   // Juste pour convertir d'un format à un autre
   private static boolean testConvert() throws Exception {
      HealpixMoc moc = new HealpixMoc();
//      moc.read("C:/Documents and Settings/Standard/Mes documents/Fits et XML/sdss7_nside256.txt",HealpixMoc.ASCII);
      moc.read("C:/Documents and Settings/Standard/Bureau/coverage-II_294_sdss7-128.txt",HealpixMoc.ASCII);
      String s = "/Documents and Settings/Standard/Bureau/SDSS_HpxMOCM.fits";
      int mode = HealpixMoc.FITS;
      moc.write(s,mode);
      System.out.println(s+" generated");
      moc = new HealpixMoc();
      moc.read(s,mode);
      System.out.println("test read seems OK");
      System.out.println("Result:\n"+moc);
      return true;
   }
   
// Juste pour tester
   public static void main(String[] args) {
      try {
//         testCompressed();
//       testFITS();
       testConvert();
//         testContains();
//         testBasic();
//       testStream();
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
   


}
