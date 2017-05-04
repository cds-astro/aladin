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

import java.awt.event.ActionEvent;
import java.io.DataInputStream;
import java.net.URL;
import java.util.StringTokenizer;

import javax.swing.*;

/** Spécialisation de la classe GluServer pour le cas particulier de SKYBOT
 * @version 1.0 - 8 dec 2005 - création
 */
public class ServerSkybot extends ServerGlu {

   final static private String GLUSKYBOT = "SkyBot.resolver";

   static String RESOLVIT,ERR;


   private JButton resolv;

   static protected void createChaine(Chaine chaine) {
      RESOLVIT = chaine.getString("SBRESOLVIT");
      ERR = chaine.getString("SBERR");
   }


   protected ServerSkybot(Aladin aladin, String A, String D, String MV,
         String MP, String ML, String LP, String PP, String FU,
         String [] PD, String [] PK, String [] PV, String R, String MI,
		 String [] AF, String AL,StringBuffer record) {

      super(aladin,A,D,MV,MP,ML,LP,PP,FU,PD,PK,PV,null, R,MI,AF,AL,null,null,record,null,null, null, null);
      aladinLogo = "SkyBotLogo.gif";

      // Resolver d'astéroid
      JButton b = new JButton(RESOLVIT);
      b.setOpaque(false);
      b.addActionListener(this);
      b.setFont(Aladin.BOLD);
      b.setBounds(10,lastY,250,HAUT);
      add(b);

   }


   // Pas utilisable dans le bouton "ALL VO"
   protected boolean isDiscovery() { return false; }

   /**
    * Interrogation du resolver SKYBOT pour obetnir la position d'un objet à une époque donnée
    * Utilise le format de retour suivant (-mime=text):
    *  # flag: 1
    *  # ticket: 11....
    *  # Num, Name, RA(h), DE(deg), Class, Mv, Err(arcsec), dRA(arcsec/h), dDEC(arcsec/h), Dg(ua), Dh(ua)
    * 1683 'Castafiore' 9.43673062924 22.0643431708 'MB IIb' 17.0 -0.106 -10.1351 1.6969   2.50547787954793 3.20897973581
    */
   protected Coord skybotResolver(String target,String epoch) throws Exception {
      URL u = aladin.glu.getURL(GLUSKYBOT,Glu.quote(target)+" "+Glu.quote(epoch));
      DataInputStream dis = new DataInputStream(u.openStream());
      String data;
      while( (data=dis.readLine())!=null ) {
         data=data.trim();
         if( data.length()>0 && data.charAt(0)!='#' ) break;
      }
      try {
         StringTokenizer tok = new StringTokenizer(data,"|");
         Coord c = new Coord();
         tok.nextToken(); tok.nextToken();
         String rah = tok.nextToken().trim();
         String de = tok.nextToken().trim();
//       System.out.println("Resultat: "+rah+" "+de);
         c.al = Double.valueOf(rah).doubleValue()*15;
         c.del = Double.valueOf(de).doubleValue();
         return c;
      } catch( Exception e ) {
          e.printStackTrace();
         throw new Exception(data);
      }
   }

   public void actionPerformed(ActionEvent e) {
      Object o = e.getSource();
      if( o instanceof JButton && ((JButton)o).getActionCommand().equals(RESOLVIT) ) {
         String target = getTarget(true);
         if( target==null ) return;
         String epoch = getDate(true);
         if( epoch==null ) return;
         try { setTarget(skybotResolver(target,epoch).getSexa(":")); }
         catch( Exception e1) {
            aladin.warning(this,ERR+":\n["+e1.getMessage()+"]");
            if( aladin.levelTrace>=3 ) e1.printStackTrace();
         }
         return;
      }

      super.actionPerformed(e);
   }

}
