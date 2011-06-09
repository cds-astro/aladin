// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//


package cds.aladin;


/**
 * Les Items propres à chaque tranche du blink (Rien de plus qu'une structure)
 */
public class PlanImageBlinkItem {
   protected String label;
   protected byte [] pixels;
   protected byte [] pixelsOrigin;
   protected int [] pixelsRGB;
   protected boolean cacheFromOriginalFile;
   protected String cacheID;
   protected long cacheOffset;
   
   protected PlanImageBlinkItem(PlanImage p) {
      this(p.label,p.getLinearPixels8(),p.pixelsOrigin, p.cacheFromOriginalFile,p.cacheID,p.cacheOffset);
      cacheID=null;
   }
   
   protected PlanImageBlinkItem(String label, byte [] pixels, byte [] pixelsOrigin, boolean cacheFromOriginalFile, 
         String cacheID, long cacheOffset ) {
      this.label=label;
      this.pixels=pixels;
      this.pixelsOrigin=pixelsOrigin;
      this.cacheFromOriginalFile=cacheFromOriginalFile;
      this.cacheID=cacheID;
      this.cacheOffset=cacheOffset;         
   }
   
   protected PlanImageBlinkItem(String label, int [] pixelsRGB ) {
      this.label=label;
      this.pixelsRGB=pixelsRGB;
   }

}
