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

package cds.astro;

public class Test {

  public static void test() {
    /*
    Astropos astropos = new Astropos(ICRS.create());
    System.out.println("@@ SET POS");
    astropos.set(12.3455,5.6789);
    System.out.println("@@ SET EPOCH");
    if (!astropos.setEpoch(2015.5)) {
      System.out.println("Error setting epoch");
      return;
    }
    //System.out.println("@@ SET PREC");
    astropos.setPrecision(9);
    //System.out.println("@@ SET EDIT");
    astropos.setEditing(Astrocoo.editingOptions("s"));
    // System.out.println("@@ PRINT");
    // System.out.println(astropos.toString());
    System.out.println("--------");
    System.out.println("@@ SET ERR ELLIPSE");
    // astropos.setErrorEllipse(Double.NaN, Double.NaN, Double.NaN);
    if (!astropos.setErrorEllipse(1.0, 1.0, 0.5, 2015.5)) {
      System.out.println("Error setting error ellipse");
      return;
    }
    System.out.println("@@ CONVERT");
    if (!astropos.convertTo(new FK5(2000.D, 2000.D))) {
      System.out.println("Error converting");
    };

    System.out.println(astropos.toString());
  }*/
    Astropos astropos = new Astropos(ICRS.create());

    astropos.set(12.3455,5.6789);
    // astropos.setPrecision(9);
    astropos.setEditing(Astrocoo.editingOptions("s"));
    // System.out.println(astropos.toString());

    //astropos.setErrorEllipse(Double.NaN, Double.NaN, Double.NaN);
    astropos.convertTo(new FK5(2000.D, 2000.D));

    System.out.println(astropos.toString());
  }

  public static void test2() {


    String coo ="83.29664986501 +82.77205709857";
    try {
      Astroframe new_icrs = new ICRS(2015.5);
      new_icrs.precision=16;
      Astroframe my_icrs = ICRS.create();
      my_icrs.precision=16;

      Astropos orig = new Astropos(new_icrs);
      orig.set(coo);

      Astropos o = new Astropos(new_icrs);
      o.set(coo);
      o.setProperMotion(2038.341, -1663.726);
      System.out.println("in(J2015.5)= "+o.toString("d"));
      o.convertTo(my_icrs);
      System.out.println("out(J2000)= "+o.toString("d"));

      Astropos p = new Astropos(my_icrs);
      p.set(o.getLon(),o.getLat());
      p.setProperMotion(o.getProperMotionLon(),o.getProperMotionLat());
      p.convertTo(new_icrs);
      System.out.println(">>out(J2015.5)= "+p.toString("d"));
      System.out.println(">>should be>> "+orig.toString("d"));
      System.out.println(">>distance mas>> "+Astrocoo.distance(p.getLon(),p.getLat(),orig.getLon(),orig.getLat())*60*60*1000);
      System.out.println(">>distance mas>> "+Astrocoo.distance(p.getLon(),p.getLat(),o.getLon(),o.getLat())*60*60*1000);

      
      System.out.println("P PM: " + p.getProperMotionLon() + " " + p.getProperMotionLat());
      System.out.println("O PM: " + o.getProperMotionLon() + " " + o.getProperMotionLat());

      
      
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  public static void test3() {
    // Create frames
    final Astroframe icrs2000 = new ICRS(2000.0);
    final Astroframe icrs2015 = new ICRS(2015.5);
    // Set pos params
    Astropos astropos = new Astropos(icrs2015);
    astropos.setPrecision(14);
    // - position
    astropos.set(12.3455,5.6789);
    // - positional error
    if (!astropos.setErrorEllipse(1.0, 1.0, 0.5, 2015.5)) {
      System.out.println("Error setting error ellipse");
      return;
    }
    // - parallax
    if (!astropos.setParallax(0.2439, Double.NaN)) {
    // if (!astropos.setParallax(0.2439, 0.5)) {
      System.out.println("Error setting plx (and its error");
      return;
    }
    // Change epoch
    astropos.convertTo(icrs2000);
    // Print result
    astropos.setEditing(Astrocoo.editingOptions("s"));
    System.out.println(astropos.toString());
  }
  
  
  public static void test4() {
    /*System.out.println("Test equality on clone ?"
    		+ astropos.equals(astropos.clone()));*/
	System.out.println("Test equality with only coordinates ?"
    		+(new Astropos(ICRS.create(), 12.3455,5.6789)).equals(
    		  new Astropos(ICRS.create(), 12.3455,5.6789)));

  }
  
  public static void test5() {
	    Astropos posinit = new Astropos(ICRS.create(), 12.3455,5.6789);
	    boolean isset=false;
	    // => isset = ok
	    // isset = posinit.setProperMotion(1000,1000);
	    // System.out.println("Set PM after toString? "+isset);

	    System.out.println("------ A ------");
	    Astropos astropos = (Astropos)posinit.clone();
	    System.out.println(astropos.toString());
	    astropos.convertTo(new FK5(2000.D, 2020.D));
	    System.out.println(astropos.toString()+"\n");
	    
	    System.out.println("------ B ------");
	    astropos = (Astropos)posinit.clone();
	    // If toString used => computation made => setPM will fail => PK
	    // System.out.println(astropos.toString());
	    isset = astropos.setProperMotion(1000,1000);
	    System.out.println("Set PM after toString? "+isset); // False (because 2 string performs computations!
	    System.out.println(astropos.toString()+"\n");
	    if (isset) System.out.println("\tShould be with PM 1000,1000"+"\n");
	    
	    System.out.println("------ C ------");
	    astropos = (Astropos)posinit.clone();
	    System.out.println(astropos.toString());
	    astropos = new Astropos(astropos);
	    isset = astropos.setProperMotion(1000,1000);
	    System.out.println("Set PM after construct copy? "+isset);
	    System.out.println(astropos.toString()+"\n");
	    if (isset) System.out.println("\tShould be with PM 1000,1000"+"\n");

	    System.out.println("------ D ------");
	    astropos = (Astropos)posinit.clone();
	    System.out.println(astropos.toString());
	    astropos = (Astropos)astropos.clone();
	    isset = astropos.setProperMotion(1000,1000);
	    System.out.println("Set PM after clone of old Astropos ? "+isset);
	    System.out.println(astropos.toString()+"\n");
	    if (isset) System.out.println("\tShould be with PM 1000,1000"+"\n");

	    System.out.println("------ E ------");
	    astropos = (Astropos)posinit.clone();
	    isset = astropos.setProperMotion(1000,1000);
	    System.out.println("Set PM after clone of fresh Astropos ? "+isset);
	    System.out.println(astropos.toString());
	    // convert is possible :
	    astropos.convertTo(new FK5(2000.D, 2020.D));
	    System.out.println(astropos.toString());
	    if (isset) System.out.println("\tShould be with PM 1000,1000"+"\n");
  }
  
  public static void main(String[] args) {
    // test();
    // test2();
    // test3();
	// test4();
	  test5();
  }

}
