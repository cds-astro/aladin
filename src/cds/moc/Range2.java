// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.moc;

/**
 * Adaptation & extension of RangeSet from Healpix.essentials lib (GNU General Public License)
 * from Martin Reinecke [Max-Planck-Society] built from Jan Kotek's "LongRange" class
 * Allow to associate an object to each range in order to build a 2 dimensional MOC
 * @version 1.0 - april 2019
 * @author P.Fernique [CDS]
 */
public class Range2 extends Range {

   // ranges spatiaux associés à un range temporel (contient 2x moins d'éléments r[i]..r[i+1] associé à smoc[i/2])
   public Range [] rangeArray;

   public Range2() { this(4); }

   public Range2(int cap) {
      super(cap);
      rangeArray = new Range[cap];
   }

   public Range2(Range2 other) {
      super(other);
      int n = other.sz>>>1;
      rangeArray = new Range[n];
      for( int i=0; i<n; i++ ) {
         rangeArray[i] = new Range( other.rangeArray[i] );
      }
   }


   public void resize(int newsize) {
      super.resize(newsize);
      Range[] nSpaceRangeArray = new Range[newsize];
      System.arraycopy(rangeArray,0,nSpaceRangeArray,0,sz/2);
      rangeArray = nSpaceRangeArray;
   }
   

  /** Append a range to the object.
    * @param a first long in range
    * @param b one-after-last long in range
    * @param m Spatial MOC associated to the range 
    */
  public void append (long a, long b, Range m) {
     if( a>=b ) return;
     if( sz>0 && a<=r[sz-1] ) {
        if (a<r[sz-2]) throw new IllegalArgumentException("bad append operation");
        
        // J'agrandis l'intervalle précédent uniquement si les MOCs sont égaux
        if( b>r[sz-1] && (sz<2 || sz>=2 && mocEquals( rangeArray[(sz>>>1)-1],m)) ) {
           r[sz-1]=b;
           return;
        }
     }
     ensureCapacity(sz+2);

     rangeArray[sz>>>1] = m;
     r[sz] = a;
     r[sz+1] = b;
     sz+=2;
  }

  /** Append an entire range set to the object. */
  public void append (Range2 other) {
     for (int i=0; i<other.sz; i+=2) append(other.r[i],other.r[i+1], other.rangeArray[i>>>1]);
  }
  
  /** Push two entries at the end of the entry vector (no check)  */
  public void push(long min, long max, Range m) {
     
     // Si le dernier intervalle temporel est identique, on ajoute le range spacial directement
     // sans créer un nouvel intervall temporel
     if( sz>=2 && r[sz-2]==min && r[sz-1]==max ) {
        rangeArray[(sz-2)>>>1].add(m);
        
     } else {
        ensureCapacity(sz+2);
        r[sz]=min;
        r[sz+1]=max;
        if( m!=null ) rangeArray[sz>>>1] = m;
        sz+=2;
     }
  }


  /** Push a single entry at the end of the entry vector (no check)*/
  private void push(long v, Range m) {
     ensureCapacity(sz+1);
     r[sz]=v;
     if( m!=null ) rangeArray[sz>>>1] = m;
     sz++;
  }
  
  
  /** Sort and remove useless ranges and trim the buffer  => Warning: not thread safe */
  public void sortAndFix() { System.err.println("No yet implemented"); }
//
//     // On remplit un tableau externe pour pouvoir le trier
//     // par intervalles croissants, et le plus grand en premier si égalité sur le début de l'intervalle
//     ArrayList<Ran> list = new ArrayList<>( sz );
//     for( int j=0; j<sz; j+=2 ) list.add( new Ran( r[j], r[j+1] ) );
//     Collections.sort(list);
//
//     // On recopie les intervalles en enlevant tout ce qui n'est pas nécessaire
//     long r1[] = new long[ list.size()*2 ];
//     long min=-1,max=-1;
//     int n=0;
//     for( Ran ran : list ) {
//        if( ran.min>max ) {
//           if( max!=-1 ) { r1[n++]=min; r1[n++]=max; }
//           min = ran.min;
//           max = ran.max;
//        } else {
//           if( ran.max>max ) max=ran.max;
//        }
//     }
//     if( max!=-1 ) { r1[n++]=min; r1[n++]=max; }
//
//     // On remplace le vecteur original
//     r=r1;
//     sz=n;
//  }
//
//  private class Ran implements Comparable<Ran> {
//     long min,max;
//     Range r;
//     Ran(long min,long max,Range r) { this.min=min; this.max=max; this.r = r; }
//     public int compareTo(Ran o) {
//        return o.min<min ? 2 : o.min>min ? -2 : o.max>max ? 1 : o.max<max ? -1 : 0;
//     }
//  }
  
  static final private int REMOVE = 0;
  static final private int UNION  = 1;
  static final private int INTER  = 2;
  static final private int SUBTR  = 3;
  
  private static Range2 operation(Range2 a, Range2 b, int op) {
     Range2 res = new Range2();  // Tableau résultat
     Range oldm =null;           // Pour comparer avec le précédent Range spatial
     int ia,ib;                  // Indices de parcours des tableaux
     boolean ina,inb;  // flag d'état pour chaque tableau pour déterminer si je suis en-dehors ou à l'intérieur d'une intervalle
     
     // Pour INTER, petite accélération pour trouver le premier indice concerné pour chaque tableau
     if( op==INTER && a.sz>0 && b.sz>0 ) {
        
        ia = a.indexOf( b.r[0] );
        while( ia>0 && a.r[ia]==a.r[ia-1] ) ia--;
        if( ia<0 ) ia=0;
        
        ib = b.indexOf( a.r[0] );
        while( ib>0 && b.r[ib]==b.r[ib-1] ) ib--;
        if( ib<0 ) ib=0;
        
        ina = (ia&1)!=0 || ia>=a.sz;
        inb = (ib&1)!=0 || ib>=b.sz;

     } else {
        ia = ib = 0;
        ina = inb = false;
     }
     
     // Je parcours les deux tableaux en parallèles
     boolean runa = ia!=a.sz;
     boolean runb = ib!=b.sz;
     
     boolean outRun =false; // true si j'ai fini le parcours sur l'un ou l'autre des tableaux
     
     while( runa || runb ) {
     
        // Si je n'avance plus sur un des tableaux, j'initialise la valeur à 0, sinon à la valeur courante
        long va = runa ? a.r[ia] : 0L;
        long vb = runb ? b.r[ib] : 0L;
        
        // Idem pour les SMOC associé (uniquement sur les indices paires (début des intervalles)
        Range ma = runa ? a.rangeArray[ia>>>1] : null;
        Range mb = runb ? b.rangeArray[ib>>>1] : null;
        
        // Dois-je avancer sur l'un, l'autre ou les deux tableaux en même temps
        // en fonction de la valeur courante (ou choisit l'élément le plus petit en premier     
        boolean adv_a = runa && (!runb || (va<=vb));
        boolean adv_b = runb && (!runa || (vb<=va));
        
        // Si j'avance en tableau A, je passe de l'interieur d'un intervalle à son exterieur (ou le contraire)
        // puis j'avance l'index, et vérifie que je ne suis pas au bout        
        if( adv_a ) { 
           // si je suis sur une fin d'intervalle, et qu'il n'y a pas d'inter-intervalle derrière,
           // je saute l'élément suivant
           if( (ia&1)==1 && ia<a.sz-1 && a.r[ia]==a.r[ia+1] ) { ia++; ina=!ina; ma = a.rangeArray[ia>>>1]; }
           ina=!ina; ++ia; runa = ia!=a.sz;
        }
        
        // Idem pour le deuxième tableau
        if( adv_b ) {
           // si je suis sur une fin d'interalle, et qu'il n'y a pas d'inter-intervalle derrière,
           // je saute l'élément suivant
           if( (ib&1)==1 && ib<b.sz-1 && b.r[ib]==b.r[ib+1] ) { ib++; inb=!inb; mb = b.rangeArray[ib>>>1]; }
           inb=!inb; ++ib; runb = ib!=b.sz;
        }
        
        // En début et en fin de parcours, lorsque les deux tableaux sont "séparés",
        // je n'ai pas besoin de vérifier l'égalité des Mocs
        boolean testEqual = ia!=0 && ib!=0 || ((!runa ||!runb) && outRun);
        outRun=!runa || !runa;
        
        // Opération à faire sur les MOCs associés
        Range m = null;
        switch( op ) {
           case UNION : m = ina && !inb ? ma  
                         :  ina &&  inb ? ma.union(mb)  
                         : !ina &&  inb ? mb  
                         : null;
                        break;
           case INTER : m = ina &&  inb ? ma.intersection(mb) 
                         : null;
                        break;
           case SUBTR : m = ina && !inb ? ma 
                         :  ina &&  inb ? ma.difference(mb) 
                         : null;
                        break;
        }
         
        if( !testEqual || !mocEquals(m,oldm) ) {
           
           // Je clos éventuellement le précédent intervalle
           if( (res.sz&1)!=0 ) res.push(adv_a ? va : vb, null );
           
           // Je démarre le suivant ?
           if( m!=null && !m.isEmpty() ) res.push(adv_a ? va : vb, m );
           oldm = m;
        }
        
        // Pour INTER, inutile de finir le parcours du tableau le plus long
        if( op==INTER && outRun ) break;
    }
    return res;
  }
  
  static private boolean mocEquals(Range m1, Range m2) {
     if( m1==m2 ) return true;
     if( m1==null && m2!=null ) return m2.equals(m1);
     return m1.equals(m2);
  }

  // J'EN SUIS ICI - PF - 25 MARS 2019


  
  /** Internal helper function for constructing unions, intersections
      and differences of two Ranges. */
//  private static Range2 generalUnion2 (Range2 a, Range2 b, boolean flip_a, boolean flip_b) {
//     Range2 res = new Range2();
//     int iva = flip_a ? 0 : -1;
//     while( iva<a.sz ) {
//        int ivb = (iva==-1) ? -1 : b.iiv(a.r[iva]);
//        boolean state_b = flip_b^((ivb&1)==0);
//        if( iva>-1 && !state_b ) res.pushv(a.r[iva]);
//        while( ivb<b.sz-1 && (iva==a.sz-1 || b.r[ivb+1]<a.r[iva+1]) ) { 
//           ++ivb; 
//           state_b=!state_b; 
//           res.pushv(b.r[ivb]); 
//        }
//        if( iva<a.sz-1 && !state_b ) res.pushv(a.r[iva+1]);
//        iva+=2;
//     }
//     return res;
//  }
//  
//  private static Range2 generalUnion (Range2 a, Range2 b, boolean flip_a, boolean flip_b) {
//    if (a.isEmpty()) return flip_a ? new Range2() : new Range2(b);
//    if (b.isEmpty()) return flip_b ? new Range2() : new Range2(a);
//    
//    int strat = strategy (a.nranges(), b.nranges());
//    return (strat==1) ? generalUnion1(a,b,flip_a,flip_b) :
//             ((strat==2) ? generalUnion2(a,b,flip_a,flip_b)
//                         : generalUnion2(b,a,flip_b,flip_a));
//    }

  /** Return the union of this Range and other. */
  public Range2 union(Range2 other) { 
     if( isEmpty() ) return new Range2(other);
     if( other.isEmpty() ) return new Range2(this);
     return operation(this,other,UNION);
  }
  
  /** Return the intersection of this Range and other. */
  public Range2 intersection(Range2 other) {
     if( isEmpty() || other.isEmpty() ) return new Range2();
     return operation(this,other,INTER);
  }
  
  /** Return the difference of this Range and other. */
  public Range2 difference(Range2 other) {
     if( isEmpty() ) return new Range2();
     if( other.isEmpty() ) return new Range2(this);
     return operation(this,other,SUBTR);
  }
  
  private void add1(long a, long b, Range m1) {
     int op=UNION;
     Range2 ajout = new Range2(10);
     Range oldm =null;
     
     // flag d'état pour chaque tableau pour déterminer si je suis en-dehors ou à l'intérieur d'une intervalle
     boolean ina=false;
     boolean inb=false;
     
     // Indices de parcours des tableaux
     long [] inter = new long[]{ a,b };
     int pos=indexOf(a);
     
     while(pos>=0 && r[pos]==a ) pos--;      // En début d'intervalle => on prend avec le précédent
     if( pos<0 ) pos=0;                      // avant tout
     if( pos>0  && (pos&1)==1 ) pos++;       // dans un inter-intervalle => on démarre sur le suivant
     
     int ia=pos;
     int ib=0;
     
     // Je parcours les deux tableaux en parallèles
     boolean runa = ia!=sz;
     boolean runb = ib!=inter.length;
     
     while( runa || runb ) {
     
        // Si je n'avance plus sur un des tableaux, j'initialise la valeur à 0, sinon à la valeur courante
        long va = runa ? r[ia] : 0L;
        long vb = runb ? inter[ib] : 0L;
        
        // Idem pour les SMOC associé (uniquement sur les indices paires (début des intervalles)
        Range ma = runa ? rangeArray[ia>>>1] : null;
        Range mb = m1;
        
        // Dois-je avancer sur l'un, l'autre ou les deux tableaux en même temps
        // en fonction de la valeur courante (ou choisit l'élément le plus petit en premier     
        boolean adv_a = runa && (!runb || va<=vb);
        boolean adv_b = runb && (!runa || vb<=va);
        
        // Si j'avance en tableau A, je passe de l'interieur d'un intervalle à son exterieur (ou le contraire)
        // puis j'avance l'index, et vérifie que je ne suis pas au bout        
        if( adv_a ) { 
           // si je suis sur une fin d'intervalle, et qu'il n'y a pas d'inter-intervalle derrière,
           // je saute l'élément suivant
           if( (ia&1)==1 && ia<sz-1 && r[ia]==r[ia+1] ) { ia++; ina=!ina; ma = rangeArray[ia>>>1]; }
           ina=!ina; ++ia; runa = ia<sz && (r[ia]<=b || r[ia]>b && (ia&1)==1); 
        }
        
        // Idem pour le deuxième tableau
        if( adv_b ) { inb=!inb; ++ib; runb = ib!=inter.length; }
        
        // Opération à faire sur les MOCs associés
        Range m = null;
        switch( op ) {
           case UNION : m = ina && !inb ? ma  
                         :  ina &&  inb ? ma.union(mb)  
                         : !ina &&  inb ? mb  
                         : null;
                        break;
           case INTER : m = ina &&  inb ? ma.intersection(mb) 
                         : null;
                        break;
           case SUBTR : m = ina && !inb ? ma 
                         :  ina &&  inb ? ma.difference(mb) 
                         : null;
                        break;
        }
         
        if( !mocEquals(m,oldm) ) {
           
           long v = adv_a ? va : vb;
           
           // Je clos éventuellement le précédent intervalle
           if( (ajout.sz&1)!=0 ) ajout.push(v, null );
           
           // Je démarre le suivant ?
           if( m!=null && !m.isEmpty() ) {
              ajout.push(v, m );
           }
           oldm = m;
        }
    }

     int diff = ajout.sz - (ia-pos);

     // Décallage à droite ?
     if( diff>0 ) {
        ensureCapacity(sz+diff);
        for( int j=sz-1; j>=pos; j--) {
           r[j+diff] = r[j];
           if( (j&1)==1 ) rangeArray[(j+diff)>>>1] = rangeArray[j>>>1];
        }
     }

     // Insertion par écrasement dans la zone laissée libre des modifs mémorisées
     for( int j=0; j<ajout.sz; j++ ) {
        r[pos+j] = ajout.r[j];
        if( (j&1)==0 ) rangeArray[(pos+j)>>>1] = ajout.rangeArray[j>>>1];
     }

     // Décallage à gauche ?
     if( diff<0 ) {
        int j;
        for( j=pos+ajout.sz; j<sz+diff; j++) {
           r[j] = r[j-diff] ;
           if( (j&1)==0 ) rangeArray[j>>>1] = rangeArray[(j-diff)>>>1];
        }
        
        // Pour pouvoir libérer la mémoire
        for( ;j<sz; j+=2 ) rangeArray[j>>>1] = null;
     }
     sz+=diff;
  }
  
  public boolean check() {
     Range oldm=null;
     
     for( int i=0; i<sz-1; i+=2 ) {
        if( r[i]>=r[i+1] ) {
           System.out.println("J'ai un problème i="+i+" tmin="+r[i]+">=tmax="+r[i+1]);
          return false;
        }
        if( i>1 && r[i]<r[i-1] ) {
           System.out.println("J'ai un problème i="+i+" r[i]="+r[i]+" < r[i-1]="+r[i-1]);
           return false;
        }
        if( rangeArray[i>>>1]==null ) {
           System.out.println("J'ai un problème i="+i+" smoc=null");
           return false;
        }
        if( i>0 && r[i]==r[i-1] && mocEquals( rangeArray[i>>>1], oldm) ) {
           System.out.println("J'ai un problème i="+i+" smoc["+(i>>>1)+"]=oldm="+rangeArray[i>>>1]);
           return false;
          
        }
        oldm=rangeArray[i>>>1];
     }
     return true;
  }
  
   /** RAM usage (in bytes) */
   public long getMem() {
      if( r==null ) return 0L;
      long mem =  super.getMem();
      for( int i=0;i<sz/2; i++ ) mem += rangeArray[i].getMem();
      return mem;
   }


   public void intersect (long a, long b) {
      throw new IllegalArgumentException("not implemented yet");
   }

   public void add(long a, long b, Range m) { 
      if( sz==0 || a>r[sz-1] ) append(a,b,m);
      else add1(a,b,m);
   }

   public void add (long a) { add( a, a+1, null); }

   public void remove (long a, long b) {
      throw new IllegalArgumentException("not implemented yet");
   }

   public void remove (long a) { remove(a,a+1); }

}

