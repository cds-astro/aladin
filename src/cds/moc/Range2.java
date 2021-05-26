// Copyright 1999-2020 - Universit� de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Donn�es
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

package cds.moc;

/**
 * Adaptation & extension of RangeSet from Healpix.essentials lib (GNU General Public License)
 * from Martin Reinecke [Max-Planck-Society] built from Jan Kotek's "LongRange" class
 * Allow to associate an object to each range in order to build a 2 dimensional MOC
 * @version 1.0 - april 2019
 * @author P.Fernique [CDS]
 */
public class Range2 extends Range {

   // ranges spatiaux associ�s � un range temporel (contient 2x moins d'�l�ments r[i]..r[i+1] associ� � smoc[i/2])
   public Range [] rr;

   public Range2() { this(4); }

   public Range2(int cap) {
      super(cap);
      rr = new Range[cap];
   }

   public Range2(Range2 other) {
      super(other);
      int n = other.sz>>>1;
      rr = new Range[n];
      for( int i=0; i<n; i++ ) {
         rr[i] = new Range( other.rr[i] );
      }
   }


   public void resize(int newsize) {
      if( newsize==r.length ) return;
      super.resize(newsize);
      Range[] nSpaceRangeArray = new Range[newsize];
      System.arraycopy(rr,0,nSpaceRangeArray,0,sz/2);
      rr = nSpaceRangeArray;
   }
   
   public void trimSize() {
      int n=sz/2;
      for( int i=0; i<n; i++ ) { 
         if( i>0 && rr[i].equals(rr[i-1]) ) rr[i] = rr[i-1];
         else rr[i].trimSize(); 
      }
      super.trimSize();
   }
   
   
   /** Retourne un range dont la pr�cision des intervalles est d�grad�e en fonction d'un nombre de bits
    * Aggr�ge les intervalles si n�cessaires et ajuste l'occupation m�moire
    * @param shift1 Nombre de bits d�grad�s - premi�re dimension (1 => d�gradation d'un facteur 2, 2 => d'un facteur 4...)
    * @param shift2 Nombre de bits d�grad�s - deuxi�me dimension (1 => d�gradation d'un facteur 2, 2 => d'un facteur 4...)
    * @return un nouveau Range d�grad�
    */
   public Range2 degrade(int shift1, int shift2) {
      if( shift1==0 && shift2==0 ) return new Range2( this );
      Range2 r1 = new Range2(sz);
      long mask = (~0L)<<shift1;   // Mask qui va bien sur les bits de poids faibles
      for( int i=0; i<sz; i+=2 ) {
         long a =  r[i] & mask;
         long b = (((r[i+1]-1)>>>shift1)+1 ) << shift1;
         Range r = rr[i>>>1].degrade(shift2);
         r1.add(a, b, r );
      }
      r1.trimSize();
      return r1;
   }
   
   /** Retourne un range dont la pr�cision des intervalles est d�grad�e en fonction d'un nombre de bits
    * Aggr�ge les intervalles si n�cessaires et ajuste l'occupation m�moire
    * @param shift Nombre de bits d�grad�s (1 => d�gradation d'un facteur 2, 2 => d'un facteur 4...)
    * @return un nouveau Range d�grad�
    */
   public Range2 degrade(int shift) { return degrade(shift,0); }

  /** Append a range to the object.
    * @param a first long in range
    * @param b one-after-last long in range
    * @param m Spatial MOC associated to the range 
    */
  public void append (long a, long b, Range m) {
     if( a>=b ) return;
     if( sz>0 && a<=r[sz-1] ) {
        if (a<r[sz-2]) throw new IllegalArgumentException("bad append operation");
        
        // J'agrandis l'intervalle pr�c�dent uniquement si les MOCs sont �gaux
        if( b>r[sz-1] && (sz<2 || sz>=2 && mocEquals( rr[(sz>>>1)-1],m)) ) {
           r[sz-1]=b;
           return;
        }
     }
     ensureCapacity(sz+2);

// A FAIRE EN AMONT
//     int sz2 = sz>>>1;
//     rr[sz2] = sz2>1 && m!=null && m.equals( rr[sz2-1]) ? rr[sz2-1] : m;   // En cas d'�galit�, on utilise le MOC spatial de l'intervalle temps pr�c�dent.
     rr[sz>>>1] = m;
     
     r[sz++] = a;
     r[sz++] = b;
  }

  /** Append an entire range set to the object. */
  public void append (Range2 other) {
     for (int i=0; i<other.sz; i+=2) append(other.r[i],other.r[i+1], other.rr[i>>>1]);
  }
  
  /** Push two entries at the end of the entry vector (no check)  */
  public void push(long min, long max, Range m) {
     
     // Si le dernier intervalle temporel est identique, on ajoute le range spacial directement
     // sans cr�er un nouvel intervall temporel
     if( sz>=2 && r[sz-2]==min && r[sz-1]==max ) {
        rr[(sz-2)>>>1].add(m);
        
     } else {
        ensureCapacity(sz+2);
        r[sz]=min;
        r[sz+1]=max;
        if( m!=null ) rr[sz>>>1] = m;
        sz+=2;
     }
  }


  /** Push a single entry at the end of the entry vector (no check)*/
  private void push(long v, Range m) {
     ensureCapacity(sz+1);
     r[sz]=v;
     if( m!=null ) rr[sz>>>1] = m;
     sz++;
  }
  
  
  /** Sort and remove useless ranges and trim the buffer  => Warning: not thread safe */
  public void sortAndFix() { System.err.println("No yet implemented"); }
//
//     // On remplit un tableau externe pour pouvoir le trier
//     // par intervalles croissants, et le plus grand en premier si �galit� sur le d�but de l'intervalle
//     ArrayList<Ran> list = new ArrayList<>( sz );
//     for( int j=0; j<sz; j+=2 ) list.add( new Ran( r[j], r[j+1] ) );
//     Collections.sort(list);
//
//     // On recopie les intervalles en enlevant tout ce qui n'est pas n�cessaire
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
     Range2 res = new Range2();  // Tableau r�sultat
     Range oldm =null;           // Pour comparer avec le pr�c�dent Range spatial
     int ia,ib;                  // Indices de parcours des tableaux
     boolean ina,inb;  // flag d'�tat pour chaque tableau pour d�terminer si je suis en-dehors ou � l'int�rieur d'une intervalle
     
     // Pour INTER, petite acc�l�ration pour trouver le premier indice concern� pour chaque tableau
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
     
     // Je parcours les deux tableaux en parall�les
     boolean runa = ia!=a.sz;
     boolean runb = ib!=b.sz;
     
     boolean outRun =false; // true si j'ai fini le parcours sur l'un ou l'autre des tableaux
     
     while( runa || runb ) {
     
        // Si je n'avance plus sur un des tableaux, j'initialise la valeur � 0, sinon � la valeur courante
        long va = runa ? a.r[ia] : 0L;
        long vb = runb ? b.r[ib] : 0L;
        
        // Idem pour les SMOC associ� (uniquement sur les indices paires (d�but des intervalles)
        Range ma = runa ? a.rr[ia>>>1] : null;
        Range mb = runb ? b.rr[ib>>>1] : null;
        
        // Dois-je avancer sur l'un, l'autre ou les deux tableaux en m�me temps
        // en fonction de la valeur courante (ou choisit l'�l�ment le plus petit en premier     
        boolean adv_a = runa && (!runb || (va<=vb));
        boolean adv_b = runb && (!runa || (vb<=va));
        
        // Si j'avance en tableau A, je passe de l'interieur d'un intervalle � son exterieur (ou le contraire)
        // puis j'avance l'index, et v�rifie que je ne suis pas au bout        
        if( adv_a ) { 
           // si je suis sur une fin d'intervalle, et qu'il n'y a pas d'inter-intervalle derri�re,
           // je saute l'�l�ment suivant
           if( (ia&1)==1 && ia<a.sz-1 && a.r[ia]==a.r[ia+1] ) { ia++; ina=!ina; ma = a.rr[ia>>>1]; }
           ina=!ina; ++ia; runa = ia!=a.sz;
        }
        
        // Idem pour le deuxi�me tableau
        if( adv_b ) {
           // si je suis sur une fin d'interalle, et qu'il n'y a pas d'inter-intervalle derri�re,
           // je saute l'�l�ment suivant
           if( (ib&1)==1 && ib<b.sz-1 && b.r[ib]==b.r[ib+1] ) { ib++; inb=!inb; mb = b.rr[ib>>>1]; }
           inb=!inb; ++ib; runb = ib!=b.sz;
        }
        
        // En d�but et en fin de parcours, lorsque les deux tableaux sont "s�par�s",
        // je n'ai pas besoin de v�rifier l'�galit� des Mocs
        boolean testEqual = ia!=0 && ib!=0 || ((!runa ||!runb) && outRun);
        outRun=!runa || !runa;
        
        // Op�ration � faire sur les MOCs associ�s
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
           
           // Je clos �ventuellement le pr�c�dent intervalle
           if( (res.sz&1)!=0 ) res.push(adv_a ? va : vb, null );
           
           // Je d�marre le suivant ?
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
     
     // flag d'�tat pour chaque tableau pour d�terminer si je suis en-dehors ou � l'int�rieur d'une intervalle
     boolean ina=false;
     boolean inb=false;
     
     // Indices de parcours des tableaux
     long [] inter = new long[]{ a,b };
     int pos=indexOf(a);
     
     while(pos>=0 && r[pos]==a ) pos--;      // En d�but d'intervalle => on prend avec le pr�c�dent
     if( pos<0 ) pos=0;                      // avant tout
     if( pos>0  && (pos&1)==1 ) pos++;       // dans un inter-intervalle => on d�marre sur le suivant
     
     int ia=pos;
     int ib=0;
     
     // Je parcours les deux tableaux en parall�les
     boolean runa = ia!=sz;
     boolean runb = ib!=inter.length;
     
     while( runa || runb ) {
     
        // Si je n'avance plus sur un des tableaux, j'initialise la valeur � 0, sinon � la valeur courante
        long va = runa ? r[ia] : 0L;
        long vb = runb ? inter[ib] : 0L;
        
        // Idem pour les SMOC associ� (uniquement sur les indices paires (d�but des intervalles)
        Range ma = runa ? rr[ia>>>1] : null;
        Range mb = m1;
        
        // Dois-je avancer sur l'un, l'autre ou les deux tableaux en m�me temps
        // en fonction de la valeur courante (ou choisit l'�l�ment le plus petit en premier     
        boolean adv_a = runa && (!runb || va<=vb);
        boolean adv_b = runb && (!runa || vb<=va);
        
        // Si j'avance en tableau A, je passe de l'interieur d'un intervalle � son exterieur (ou le contraire)
        // puis j'avance l'index, et v�rifie que je ne suis pas au bout        
        if( adv_a ) { 
           // si je suis sur une fin d'intervalle, et qu'il n'y a pas d'inter-intervalle derri�re,
           // je saute l'�l�ment suivant
           if( (ia&1)==1 && ia<sz-1 && r[ia]==r[ia+1] ) { ia++; ina=!ina; ma = rr[ia>>>1]; }
           ina=!ina; ++ia; runa = ia<sz && (r[ia]<=b || r[ia]>b && (ia&1)==1); 
        }
        
        // Idem pour le deuxi�me tableau
        if( adv_b ) { inb=!inb; ++ib; runb = ib!=inter.length; }
        
        // Op�ration � faire sur les MOCs associ�s
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
           
           // Je clos �ventuellement le pr�c�dent intervalle
           if( (ajout.sz&1)!=0 ) ajout.push(v, null );
           
           // Je d�marre le suivant ?
           if( m!=null && !m.isEmpty() ) {
              ajout.push(v, m );
           }
           oldm = m;
        }
    }

     int diff = ajout.sz - (ia-pos);

     // D�callage � droite ?
     if( diff>0 ) {
        ensureCapacity(sz+diff);
        for( int j=sz-1; j>=pos; j--) {
           r[j+diff] = r[j];
           if( (j&1)==1 ) rr[(j+diff)>>>1] = rr[j>>>1];
        }
     }

     // Insertion par �crasement dans la zone laiss�e libre des modifs m�moris�es
     for( int j=0; j<ajout.sz; j++ ) {
        r[pos+j] = ajout.r[j];
        if( (j&1)==0 ) rr[(pos+j)>>>1] = ajout.rr[j>>>1];
     }

     // D�calage � gauche ?
     if( diff<0 ) {
        int j;
        for( j=pos+ajout.sz; j<sz+diff; j++) {
           r[j] = r[j-diff] ;
           if( (j&1)==0 ) rr[j>>>1] = rr[(j-diff)>>>1];
        }
        
        // Pour pouvoir lib�rer la m�moire
        for( ;j<sz; j+=2 ) rr[j>>>1] = null;
     }
     sz+=diff;
  }
  
  public boolean equals( Object obj ) {
     if( this==obj ) return true;
     if( obj==null || !(obj instanceof Range2) ) return false;
     Range2 other = (Range2) obj;
     if( other.sz!=sz ) return false;
     for (int i=0; i<sz; ++i) {
        if (other.r[i]!=r[i]) return false;
        if( i%2==0 && other.rr[i/2]!=null && !other.rr[i/2].equals(rr[i/2]) ) return false;
     }
     return true;
  }
  
  public int hashCode() {
     if (sz == 0)  return 0;
     int result = 1;
     for( int i=0; i<sz; i++) {
        long element = r[i];
        result = 31 * result + (int)(element ^ (element >>> 32));
        if( i%2==0 ) result = 31 * result + rr[i/2].hashCode();
     }
     return result;
  }

  public boolean check() {
     Range oldm=null;
     
     for( int i=0; i<sz-1; i+=2 ) {
        if( r[i]>=r[i+1] ) {
           System.out.println("J'ai un probl�me i="+i+" tmin="+r[i]+">=tmax="+r[i+1]);
          return false;
        }
        if( i>1 && r[i]<r[i-1] ) {
           System.out.println("J'ai un probl�me i="+i+" r[i]="+r[i]+" < r[i-1]="+r[i-1]);
           return false;
        }
        if( rr[i>>>1]==null ) {
           System.out.println("J'ai un probl�me i="+i+" smoc=null");
           return false;
        }
        if( i>0 && r[i]==r[i-1] && mocEquals( rr[i>>>1], oldm) ) {
           System.out.println("J'ai un probl�me i="+i+" smoc["+(i>>>1)+"]=oldm="+rr[i>>>1]);
           return false;
          
        }
        oldm=rr[i>>>1];
     }
     return true;
  }
  
   /** RAM usage (in bytes) */
   public long getMem() {
      if( r==null ) return 0L;
      long mem =  super.getMem();
      int n=sz/2;
      for( int i=0;i<n; i++ ) {
         if( i<n-1 && rr[i]==rr[i+1] ) continue;   // Si pointe sur le m�me coverage, inutile de le compter
         mem += rr[i].getMem();
      }
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
