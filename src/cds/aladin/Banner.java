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

import java.awt.*;
import cds.tools.Util;

/**
 * Gestion du Banner de démarrage d'Aladin
 *
 * Copyright: 2004, Pierre Fernique
 */
final public class Banner extends Window implements Runnable {
  Image im=null;
  int delay = 4000;
  String number;
  static Font F = new Font("Times New Roman",Font.BOLD+Font.ITALIC,52);
  static Font F1 = new Font("Times New Roman",Font.BOLD+Font.ITALIC,32);

  public Banner(Aladin a) {
     super(a.f);
     number= Aladin.BETA && !Aladin.OUTREACH ? "Beta" : Aladin.VERSION.substring(0,Aladin.VERSION.indexOf('.'));
     try {
        MyInputStream is = new MyInputStream(a.getClass().getResourceAsStream("/AladinBanner.jpg"));
        byte buf[] = is.readFully();
        if( buf.length==0 ) throw new Exception();
        im = Toolkit.getDefaultToolkit().createImage(buf);
        MediaTracker mt = new MediaTracker(this);
        mt.addImage(im,0);
        mt.waitForAll();
        setSize(im.getWidth(this),im.getHeight(this));
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = getSize();
        setLocation((screenSize.width - frameSize.width) / 2,
                      (screenSize.height - frameSize.height) / 2);
        setVisible(true);
        (new Thread(this,"Banner")).start();
        Util.pause(50);
     } catch( Exception e) { }
  }

  public void run() {
     try {
        Util.pause(delay);
        dispose();
        im=null;
     }catch( Exception e ) { }
  }

  public void paint(Graphics g) {
     if( im==null ) return;
     g.drawImage(im,0,0,this);
     g.setFont(F);
     g.setColor(Color.yellow);
     try {
        ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
              RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
     } catch( Exception e ) {}
     g.drawString(number,320,250);
     if( Aladin.BETA && !Aladin.OUTREACH ) {
        g.setFont(F1);
        g.drawString(Aladin.VERSION,320,290);
     }
  }
}