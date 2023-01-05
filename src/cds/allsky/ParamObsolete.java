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

package cds.allsky;

import java.util.ArrayList;

import cds.aladin.Tok;

public enum ParamObsolete {
   
   // Alias
   creator_did (Param.id,false),
   hips_order (Param.order,false),
   hips_min_order (Param.minOrder,false),
   hips_frame (Param.frame,false),
   hips_tile_width (Param.tileWidth,false),
   hips_pixel_bitpix (Param.bitpix,false),
   hips_data_range (Param.dataRange,false),
   hips_pixel_cut (Param.pixelCut,false),
   hips_title (Param.title,false),
   hips_creator (Param.creator,false),
   hips_status (Param.status,false),
   hips_moc_order (Param.mocOrder,false),
   
   tileFormat (Param.mirrorFormat,false),
   format (Param.mirrorFormat,false),
   
   // Réellement obsolètes
   split (Param.mirrorSplit,true),
   nside (Param.mapNside,true),
   fading (Param.mode,true),
   method (Param.mode,true),
   ivorn (Param.id,true),
   input (Param.in,true),
   output (Param.out,true),
   pixel (Param.mode,true),
   blocking (Param.partitioning,true),
   cutting (Param.partitioning,true),
   polygon (Param.fov,true),
   jpegMethod (Param.mode,true),
   dataCut (Param.dataRange,true),
   pixelRange (Param.dataRange,true),
   goodPixel (Param.validRange,true),
   histoPercent (Param.skyVal,true),
   label (Param.title,true),
   publisher (Param.creator,true),
   tileTypes (Param.mirrorFormat,true),
   mixing (Param.mode,true),
   
   filter((Param)null,true),

   // valeurs de paramètres obsolètes
   average (ModeMerge.mergeMean,true),
   replacetile (ModeMerge.mergeOverwriteTile,true),

   // Options alias et obsolètes
   debug (ParamOption.d,false),
   help (ParamOption.h,false),
   f (ParamOption.clean,true),
   live (Param.incremental,true),
   check (Param.fastCheck,true),
   nocheck (Param.fastCheck,true),
   ;
   
  String alias;
  boolean warning;
  
  ParamObsolete(Param alias,boolean warning) { 
     this.alias=alias==null ? null : alias.toString(); 
     this.warning=warning; 
  }
  
  ParamObsolete(ModeMerge paramAlias,boolean warning) { 
     this.alias=paramAlias.toString(); 
     this.warning=warning; 
   }
  
  ParamObsolete(ParamOption paramOption,boolean warning) { 
     this.alias=paramOption.toString(); 
     this.warning=warning; 
  }
  
  /** Retourne la liste des alias valides pour un paramètre ou une option */
  static String[] aliases(String s) {
     ArrayList<String> rep = new ArrayList<>();
     for( ParamObsolete p : ParamObsolete.values() ) {
        if( p.warning ) continue;   // on ne prend pas en compte les "deprecated"
        if( p.alias.equalsIgnoreCase(s) ) {
//           rep.add(p.toString()+ (p.warning?" (deprecated)":""));
           rep.add(p.toString());
        }
     }
     if( rep.size()==0 ) return null;
     String [] p = new String[rep.size()];
     rep.toArray(p);
     return p;
  }
  
  /** Retourne le paramètre dans la dernière syntaxe autorisée.
   * @param context
   * @param s le paramètre, éventuellement suivi de sa valeur (ex: hips_pixel_bitpix=16)
   * @return le paramètre (éventuellement suivi de sa valeur) inchangé ou non.
   *         null si simplement oublié
   */
  static String getLastSyntax(Context context,String s) {
     
     s = Tok.unQuote(s);
     String param, value=null;
     int i = s.indexOf('=');
     if( i<0 ) param=s;
     else {
        param = s.substring(0,i).trim();
        value = Tok.unQuote( s.substring(i+1).trim() );
     }
     
     // Une option commençant par un tiret ?
     boolean tiret=false;
     if( param.startsWith("-") ) { tiret=true; param=param.substring(1); }
     
     String nParam=null;
     String nValue=null;
     
     // Alias pour le paramètre ?
     s = param.toUpperCase();
     for( ParamObsolete p : ParamObsolete.values() ) {
       if( p.toString().toUpperCase().equals(s) ) {
          nParam = p.alias;
          
          // Cas particuliers ?
          if( p==live || p==nocheck ) value="true";
          if( p==check ) value="false";
          if( p==mixing ) nValue= value.toLowerCase().equals("true") 
                ? ModeOverlay.overlayMean.toString() : ModeOverlay.overlayNone.toString();
          if( p==fading ) nValue= value.toLowerCase().equals("true") 
                ? ModeOverlay.overlayFading.toString() : ModeOverlay.overlayMean.toString(); 
          if( p.warning ) {
             String t = tiret ? "option [-" : "parameter [";
             String v = value!=null ? "="+value : "";
             context.warning("Deprecated "+t+p+"] => "
                     +(nParam!=null ? "assuming ["+nParam+v+"]" : "ignored!"));
          }
          if( p==live ) tiret=false;
         if( nParam==null ) return null;
          break;
       }
     }
     
     // Alias pour la valeur du paramètre ?
     if( nValue==null && value!=null ) {
        s = value.toUpperCase();
        for( ParamObsolete p : ParamObsolete.values() ) {
           if( p.toString().toUpperCase().equals(s) ) {
              nValue = p.alias;
              if( p.warning ) context.warning("Parameter value deprecated ["+p+"] => assuming ["+nValue+"]");
              break;
           }
        }
     }
     
     String rep = (tiret?"-":"") 
           + (nParam==null ? param : nParam) 
           + (value!=null ? ("="+ (nValue==null ? value : nValue)):"");
     
     return rep;
  }
}
