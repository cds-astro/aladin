// Copyright 2011 - UDS/CNRS
// The MOC API project is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of MOC API java project.
//
//    MOC API java project is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    MOC API java project is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with MOC API java project.
//

package cds.moc.examples;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;

import cds.moc.Healpix;
import cds.moc.Moc;
import cds.moc.MocCell;
import cds.moc.SMoc;

public class MocExample {
   
    static public void main(String[] args) throws Exception {
       
       try {
         // Creation by Stream
          String u = "http://alasky.unistra.fr/MocServer/query?ID=CDS/II/311/wise&get=moc";
          System.out.println("Loading this remote MOC: "+u+"...");
          URL url = new URL(u);
          BufferedInputStream bis = new BufferedInputStream(url.openStream(), 32*1024);
          SMoc mocA = new SMoc();
          mocA.read(bis);

          System.out.println("Moc sky coverage : "+pourcent(mocA.getCoverage()));
          System.out.println("Moc order        : "+mocA.getMocOrder());
          System.out.println("Number of cells  : "+mocA.getNbCells());
          System.out.println("Number of ranges : "+mocA.getNbRanges());
          System.out.println("In memory        :~"+mocA.getMem()+" bytes");
          System.out.println("Information      :"+
                " MOCTOOL="+mocA.getProperty("MOCTOOL")+
                " MOCTYPE="+mocA.getProperty("MOCTYPE")+
                " MOCID="+mocA.getProperty("MOCID")+
                " DATE="+mocA.getProperty("DATE")+
                " ORIGIN="+mocA.getProperty("ORIGIN")+
                " EXTNAME="+mocA.getProperty("EXTNAME"));
          System.out.print("Contents         : "); display("MocA",mocA);
//        for( MocCell item : moc ) System.out.println(" "+item);
          
          // Creation by list of cells (JSON format or basic ASCII format), write and reread it.
          SMoc mocB = new SMoc();
          mocB.add("3/2 53 55 4/20-22 25 28 30 50 60 5/456 567 836 9/123456");
//          mocB.add("{ \"3\":[2,53,55], \"4\":[20,21,22,25,28,30,50,60], \"5\":[456,567,836], \"9\":[123456] }");
          
          // Addition of some meta data
          mocB.setProperty("MOCID","ivo://CDS/0001");    // MOC unique identifier
          mocB.write("Moc.fits");
          mocB.read("Moc.fits");
          System.out.print("\nAnother Moc created by string: "); display("MocB",mocB);
          
          // Creation by a list of spherical coordinates
          SMoc mocD = new SMoc();
          mocD.setMocOrder(13);
          mocD.bufferOn();            // Activation of the buffering for speed up the multiple insertions
          Healpix hpx = new Healpix();
          for( int i=0; i<100000; i++ ) {
             double lon = Math.random()*360;
             double lat = Math.random()*180 -90;
             mocD.add( hpx, lon, lat);
          }
          mocD.bufferOff();          // Stop and release the buffering
          System.out.print("\nAnother Moc created by spherical positions: "); display("MocD",mocD);
         
        
          // Intersection, union, clone
          
          // Operation logic for preserving surfaces (LOGIC_MIN for preserving observation coverage) -> see IVOA MOC doc
          Moc.setMocOrderLogic( Moc.LOGIC_MAX);
          SMoc clone = mocA.clone(); 
          SMoc union = clone.union(mocB);
          SMoc inter = mocA.intersection(mocB);
          System.out.println("\nMocA coverage      : "+pourcent(mocA.getCoverage()));
          System.out.println("MocB coverage        : "+pourcent(mocB.getCoverage()));
          System.out.println("Moc union coverage   : "+pourcent(union.getCoverage()));
          System.out.println("Moc inter coverage   : "+pourcent(inter.getCoverage()));
          
          // Writing in FITS format
          File f;
          f = new File("Moc.fits");
          System.out.println("\nWriting MocA in FITS file "+f.getAbsolutePath());
          OutputStream outFits = (new FileOutputStream( f ));
          mocA.writeFITS(outFits);
          outFits.close();
          
          // Writing in ASCII format
          f = new File("Moc.txt");
          System.out.println("Writing MocA in ASCII file "+f.getAbsolutePath());
          OutputStream outAscii = (new FileOutputStream( f ));
          mocA.writeASCII(outAscii);
          outAscii.close();
          
          // HEALPix cell queries
          long npix;
          int order;
          order=5; npix=849;
          System.out.println("\nHEALPix cell "+order+"/"+npix+" => inside mocA : "+mocA.isIncluding(order, npix) );
          order=7; npix=14103;
          System.out.println("HEALPix cell "+order+"/"+npix+" => inside mocA : "+mocA.isIncluding(order, npix) );
          System.out.println("MocA => intersects mocB : "+mocA.isIntersecting(mocB) );
          
          
          // Coordinate queries
          hpx = new Healpix();
          double al,del,radius;
          al = 095.73267; del = 69.55885;
          System.out.println("Coordinate ("+al+","+del+") => inside MocA : "+mocA.contains(hpx,al,del));
          al = 095.60671; del = 69.57092;
          System.out.println("Coordinate ("+al+","+del+") => inside MocA : "+mocA.contains(hpx,al,del));
          
          
          // circle queries
          mocA.setMocOrder(13);
          al = 282.81215; del =  -70.20608; radius = 0.5;
          SMoc circle = mocA.queryDisc(hpx, al, del, radius);
          display("MocA intersection with circle("+al+","+del+","+radius+")", circle);
          circle.setMocOrder(6);
          display("Same result for limit order 6", circle);
          
          
      } catch( Exception e ) {
         e.printStackTrace();
      }

    }
    
    static String pourcent(double d) { return (int)(1000*d)/100.+"%"; }

    // Just for displaying a few cells
    static public void display(String title,SMoc moc) {
       System.out.print(title+":");
       int i=0;
       int oOrder=-1;
       for( MocCell item : moc ) {
          if( oOrder!=item.order ) {
             System.out.print("\n   "+item.order+"/");
             oOrder=item.order;
             i=0;
          }

          if( i!=0 && i<20) System.out.print(",");
          if( i<20 )  System.out.print(item.start);
          if( i==21 ) System.out.print("...");
          i++;
       }
       System.out.println();
    }
}
