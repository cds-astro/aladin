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

import java.awt.Dimension;

public class SliderEpochTool extends SliderPlusMoins {
   private FrameCooToolbox frameCooTool;
   
   public SliderEpochTool(FrameCooToolbox f) {
      super(f.aladin,null,1650,2500,1,100);
      setEnabled(true);
      frameCooTool = f;
   }
   
   private Dimension DIM = new Dimension(170,20);
   public Dimension getPreferredSize() { return DIM; }
   
   void submit(int inc) {
      double epoch = getValue();
      frameCooTool.setEpoch(epoch+"");
   }
}
