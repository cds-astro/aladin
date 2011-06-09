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

import java.net.*;

import javax.swing.JOptionPane;

/**
 * Pour g�rer les acc�s HTTP prot�g�s
 */
public class MyAuthenticator extends Authenticator {
   
   protected PasswordAuthentication getPasswordAuthentication() {
      
       // Get information about the request
       String promptString = getRequestingPrompt();
       String hostname;
       try { hostname = getRequestingHost(); }
       catch( Exception e ) { hostname=getRequestingSite().toString(); }
       int port = getRequestingPort();

       String s="Authentication required for accesssing data ["+promptString+"]\n"+      
             "   on machine "+hostname+" : \n \n";    
       
       StringBuffer user= new StringBuffer();
       StringBuffer passwd = new StringBuffer();
       if( Message.showPassword(s,user,passwd)==Message.NON ) return null;
             
       return new PasswordAuthentication(user.toString(), passwd.toString().toCharArray());
   }
}

