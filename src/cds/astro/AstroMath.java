package cds.astro;

/**
 * Trigonometric and a few other functions used in the astronomical context.
 * This class includes also 3x3 matrix manipulation.
 * Extracted from Class Coo
 * @author Francois Ochsenbein
 * @version 1.0: 20-Apr-2004
 * @version 1.1: 15-Jun-2008: elliptical integral of 1st kind.
 * @version 1.2: 03-Jun-2016: all 3 kinds of complete elliptical integrals
 *               (see also the "ellipse.pdf" documentation)
 * @version 2.0: 03-Feb-2019: 1-D matrices (20% faster)
 */

public class AstroMath {
  public static final double[] powers = { 1., 1.e1, 1.e2, 1.e3,
      1.e4, 1.e5, 1.e6, 1.e7, 1.e8, 1.e9 };

  /** Number of degrees in 1 radian */
  public static final double DEG = 180.0/Math.PI ;	// #Degrees in 1 radian
  /** Number of square degrees in 1 steradian */
  public static final double DEG2 = DEG*DEG ;		// #sq.deg in 1 steradian
  /** Number of arcsecs in 1 radian */
  public static final double ARCSEC = 3600.*DEG ;	// #Arcsec in 1 radian
  /** Number of milli-arcsecs in 1 radian */
  public static final double MAS = 1000.*ARCSEC ;	// #mas in 1 radian
  /** Natural log(10), used for dexp */
  public static final double ln10 = Math.log(10.) ;	// log10 to natural log
  /** Accuracy (ε) of computation in double, about 1.11e-16 */
  public static double eps = Math.ulp(0.5);		// precision ε (1.11e-16)
  /** Accuracy used in the computation of elliptical integrals */
  static final double ellEPS = 4.e-16;			// Precision for ellptical integrals
  /** 3x3 unity matrix, also default rotation */
  public static final double[] U3matrix = {            // Unity 3x3 matrix
      1.0, 0.0, 0.0,  0.0, 1.0, 0.0,  0.0, 0.0, 1.0 };
  /** rotspin unity 2x3x3 matrix (zero rotation and zero spin) */
  public static final double[] Urotspin = {            // Unity rotspin matrix
      1.0, 0.0, 0.0,  0.0, 1.0, 0.0,  0.0, 0.0, 1.0,
      0.0, 0.0, 0.0,  0.0, 0.0, 0.0,  0.0, 0.0, 0.0 };
  /** 6x6 unity matrix */
  public static final double[] U6matrix = {            // Unity 6x6 matrix
      1.0, 0.0, 0.0,  0.0, 0.0, 0.0,  
      0.0, 1.0, 0.0,  0.0, 0.0, 0.0,  
      0.0, 0.0, 1.0,  0.0, 0.0, 0.0,  
      0.0, 0.0, 0.0,  1.0, 0.0, 0.0,  
      0.0, 0.0, 0.0,  0.0, 1.0, 0.0,  
      0.0, 0.0, 0.0,  0.0, 0.0, 1.0 };
  /** For tests: eps (ε) is the relative accuracy.
   **/
  //public static boolean DEBUG=true;

  //  ===========================================================
  //		Trigonometry in Degrees
  //  ===========================================================

  /*  Static methods (functions) in Java are very close to C ones;
    	 they do not require any object instanciation.
    	 Typical example of static methods are in the Math class
    	 Note that the functions toDegrees and toRadians can be used
    	 in JDK1.2 -- we stick here strictly to JDK1.1
   */

  /**
   * Cosine when argument in degrees
   * @param x angle in degrees
   * @return	the cosine
   */
  public static final double cosd(double x) {
    return Math.cos( x/DEG );
  }

  /**
   * Sine  when argument in degrees
   * @param x angle in degrees
   * @return	the sine
   */
  public static final double sind(double x) {
    return Math.sin( x/DEG );
  }

  /**
   * Tangent  when argument in degrees
   * @param x angle in degrees
   * @return	the tan
   */
  public static final double tand(double x) {
    return Math.tan( x/DEG );
  }

  /**
   * sin-1 (inverse function of sine), gives argument in degrees
   * @param	x argument
   * @return	y value such that sin(y) = x
   */
  public static final double asind(double x) {
    return Math.asin(x)*DEG;
  }

  /**
   * tan-1 (inverse function of tangent), gives argument in degrees
   * @param x argument
   * @return	angle in degrees
   */
  public static final double atand(double x) {
    return Math.atan(x)*DEG;
  }

  /**
   * get the polar angle from 2-D cartesian coordinates
   * @param y cartesian y coordinate
   * @param x cartesian x coordinate
   * @return	polar angle in degrees
   */
  public static final double atan2d(double y,double x) {
    return Math.atan2(y,x)*DEG;
  }

  //  ===========================================================
  //		Hyperbolic Functions (in Math >=1.5)
  //  ===========================================================

  /**
   * Hyperbolic cosine cosh = (exp(x) + exp(-x))/2
   * @param  x argument
   * @return	corresponding hyperbolic cosine (>= 1)
   */
  /*---
    public static final double cosh (double x) {
    	 double ex ;
    		  ex = Math.exp(x) ;
    		return 0.5 * (ex + 1./ex) ;
    }
    ---*/

  /**
   * Hyperbolic tangent = (exp(x)-exp(-x))/(exp(x)+exp(-x))
   * @param x argument
   * @return	corresponding hyperbolic tangent (in range ]-1, 1[)
   */
  /*---
    public static final double tanh (double x) {
    	 double ex, ex1 ;
    ex = Math.exp(x) ;
    ex1 = 1./ex ;
    return (ex - ex1) / (ex + ex1) ;
    }
    ---*/

  /**
   * tanh-1 (inverse function of tanh)
   * @param x argument, in range ]-1, 1[ (NaN returned otherwise)
   * @return	corresponding hyperbolic inverse tangent
   */
  public static final double atanh (double x) {
    return (0.5*Math.log((1.+(x))/(1.-(x))));
  }

  //  ===========================================================
  //		sin(x)/x and Inverse
  //  ===========================================================

  /**
   * Function sinc(x) = sin(x)/x
   * @param x argument (radians)
   * @return	corresponding value
   */
  public static final double sinc(double x) {
    double ax = Math.abs(x);
    double y;
    if (ax <= 1.e-4) {
      ax *= ax;
      y = 1 - ax*(1.0-ax/20.0)/6.0;
    }
    else y = Math.sin(ax)/ax;
    return y;
  }

  /** Reciprocal */
  /**
   * Function asinc(x), inverse function of sinc
   * @param	x argument
   * @return	y such that sinc(y) = x
   */
  public static final double asinc(double x) {
    double ax = Math.abs(x);
    double y;
    if( ax <= 1.e-4) {
      ax *= ax;
      y = 1.0 + ax*(1.0 + ax*(9.0/20.0))/6.0;
    }
    else y = Math.asin(ax)/ax;
    return y;
  }

  //  ===========================================================
  //		Exponential/Logarithm base 10
  //  ===========================================================

  /**
   * Compute just 10<sup>n</sup>
   * @param	n Power to which to compute the value
   * @return	10<sup>n</sup>
   */
  public static final double dexp(int n) {
    int i = n;
    int m = powers.length-1;
    double x = 1;
    boolean inv = false;
    if (n < 0) { inv = true; i = -n; }
    while (i > m) { x *= powers[m]; i -= m; }
    x *= powers[i];
    if (inv) x = 1./x ;
    return(x);
  }


  /**
   * Compute just 10<sup>x</sup>
   * @param	x Power to which to compute the value
   * @return	10<sup>x</sup>
   */
  public static final double dexp(double x) {
    return(Math.exp(x*ln10));
  }

  /**
   * Compute the log base 10
   * @param	x Number (positive)
   * @return	log<sub>10</sub>(x)
   */
  public static final double log(double x) {
    return(Math.log(x)/ln10);
  }

  //  ===========================================================
  //		Elliptical Integrals
  //		See http://dlmf.nist.gov/19.8
  //  ===========================================================

  /**
   * Computation of complete elliptic integral of first kind.
   * K(a,b) = Integral{0,&pi;/2} du/sqrt(a<sup>2</sup>cos<sup>2</sup>u+b<sup>2</sup>sin<sup>2</sup>u).
   * <br>Computed with arithmetico-geometrical mean M(a,b) = common limit of
   * a<sub>n+1</sub>=(a<sub>n</sub>+b<sub>n</sub>)/2,
   * b<sub>n+1</sub>=sqrt(a<sub>n</sub>*b<sub>n</sub>).<br>
   * The arithmetico-geometrical mean M(a,b) is given by (Gauss):
   * 1/M(a,b) = (2/&pi;) K(a,b)<br>
   * M(a,b) being the common limit of suites 
   * a<sub>n+1</sub>=(a<sub>n</sub>+b<sub>n</sub>)/2
   * b<sub>n+1</sub>=sqrt(a<sub>n</sub>.b<sub>n</sub>)
   * @param a (positive)
   * @param b (positive)
   * @return value of the elliptic integral function
   **/
  public static final double ell1(double a, double b) {
    double a0=a, b0=b, a1, b1; int i;
    if(a<b) { a0=b; b0=a; }	// a_n suite decreasing
    for(i=50; (--i>=0) && ((a0-b0)>ellEPS); a0=a1, b0=b1) {
      a1 = (a0+b0)/2.;
      b1 = Math.sqrt(a0*b0);
    }
    /* System.err.println("#...AstroMath.ell1(" + a + "," + b + "): " 
	                  + (50-i) + " iterations"); */
    if(i<0) System.err.println(
        "#+++AstroMath.ell1(" + a + "," + b + ") did not converge!");
    /* if (DEBUG) System.out.println("#...ell1(" + a + "," + b + " => "
		+ (Math.PI/(a0+b0))); */
    return(Math.PI/(a0+b0));
  }
  public static final double ell1(double m) {
    /**
     * 1-argument complete elliptical K(m) = Integral{0,&pi;/2} du/sqrt(1-m.sin<sup>2</sup>u)
     * @param m argument, in range [0,1[.
     * @return value of the elliptic integral function K(m)
     **/
    return(ell1(1., Math.sqrt(1.-m)));
  }

  /**
   * Computation of complete elliptic integral of second kind.
   * J(a,b) = Integral{0,&pi;/2} du.sqrt(a<sup>2</sup>cos<sup>2</sup>u+b<sup>2</sup>sin<sup>2</sup>u)
   * <br>Computed with property that 
   * E(a,b)/K(a,b) = (a<sup>2</sup>+a<sup>2</sup>)/2 
   *      - Sum[n=1^Infinity] 2<sup>n-1</sup>c<sub>n</sub><sup>2</sup> where
   * <br> c<sub>n+1</sub>=(a<sub>n</sub>-b<sub>n</sub>)/2.
   * @param a (positive)
   * @param b (positive)
   * @return the integral
   **/
  public static final double ell2(double a, double b) {
    double a0=a, b0=b, a1, b1, S, d2, f2=1.; int i;
    if(a<b) { a0=b; b0=a; }	// a_n suite decreasing
    S = (a0*a0+b0*b0)/2.;
    for(i=50; (--i>=0) && ((a0-b0)>ellEPS); a0=a1, b0=b1) {
      d2 = (a0-b0)/2.; S -= f2*d2*d2; f2 *= 2.;
      a1 = (a0+b0)/2.; 
      b1 = Math.sqrt(a0*b0);
    }
    /* System.err.println("#...AstroMath.ell2(" + a + "," + b + "): " 
	                  + (50-i) + " iterations"); */
    if(i<0) System.err.println(
        "#+++AstroMath.ell2(" + a + "," + b + ") did not converge!");
    return(Math.PI*S/(a0+b0));
  }
  /**
   * Computation of complete elliptic integral of second kind.
   * E(m) = Integral{0,&pi;/2} du.sqrt(1-m.sin<sup>2</sup>u)
   * @param m argument, in range [0,1[
   * @return value of the elliptic integral function E(m)
   **/
  public static final double ell2(double m) {
    return(ell2(1., Math.sqrt(1.-m))); /*
	double a0, b0, a1, b1, S, d2, f2; int i;
	a0 = 1.; b0 = Math.sqrt(1.-m);
	S = 1. - m/2.; f2 = 0.25;
	for(i=50; (--i>=0) && (Math.abs(a0-b0)>ellEPS); a0=a1, b0=b1) {
	    S -= f2*(a0-b0)*(a0-b0); f2 *= 2.;
	    a1 = (a0+b0)/2.; 
	    b1 = Math.sqrt(a0*b0);
	}
	if(i<0) System.err.println(
	    "#+++AstroMath.ell1(" + m + ") did not converge!");
	return(Math.PI*S/(a0+b0)); */
  }

  /**
   * Computation of complete elliptic integral of third kind.
   * Pi(n,m) = Integral{0,&pi;/2} du(1-n.sin<sup>2</sup>u).sqrt(1-m.sin<sup>2</sup>u)]
   * See http://functions.wolfram.com/EllipticIntegrals/
   * http://scitation.aip.org/content/aip/journal/jap/34/9/10.1063/1.1729771
   * @param n [0,1[
   * @param m [0,1[
   * @return the integral
   **/
  public static final double ell3(double n, double m) {
    double a0, b0, a1, b1, zeta, delta, eps0, eps1; int i;
    a0 = 1.; b0 = Math.sqrt(1.-m);
    delta = (1.-n)/b0;
    eps0  = n/(1.-n);
    zeta  = 0;
    for(i=50; (--i>=0) && ((a0-b0)>ellEPS||Math.abs(delta-1.)>ellEPS); 
        a0=a1, b0=b1, eps0=eps1) {
      eps1 = (delta*eps0 + zeta)/(1.+delta);
      zeta = (eps0+zeta)/2.;
      a1 = (a0+b0)/2.; b1 = Math.sqrt(a0*b0);
      delta = 0.25*b1/a1*(2.+delta+1./delta);
    }
    /* System.err.println("#...AstroMath.ell3(" + n + "," + m + "): " 
	                  + (50-i) + " iterations"); */
    if(i<0) System.err.println(
        "#+++AstroMath.ell3(" + n + "," + m + ") did not converge!");
    return(Math.PI*(1.+zeta)/(a0+b0));
  }

  //  ===========================================================
  //		Matrices and Vectors 3-D and 6-D
  //		(stored as 1-D arrays)
  //  ===========================================================

  /**
   * Find the largest absolute value of an array
   * @param A  array of values
   * @return	the highest absolute value
   */
  public static final double amax(double[] A) {
    double max=0;
    for(int i=0; i<A.length; i++) {
      if(A[i]>max)          max=A[i]; 
      else if((A[i]+max)<0) max=0-A[i];
    }
    return(max);
  }

  /** 
   * Dimension of a matrix (square or symmetrically stored)
   * @param  size Size of the vector
   * @return the size of the corresponding square matrix;
   *         negative for a symmetrically stored matrix, 0 if error
   *         Accepts a double-matrix (e.g. 2x3x3 returns 3).
   */
  public static final int dim(int size) {
    double m=Math.sqrt(size); 
    if(m==Math.floor(m)) return((int)m);
    // double-matrix?
    m = Math.sqrt(0.5*size);
    if(m==Math.floor(m)) return((int)m);
    // symmetrically stored matrix?
    m = 0.5*(Math.sqrt(1.0+8.0*size)-1.0);
    if(m!=Math.floor(m)) return(0);
    m = -m;
    return((int)m);
  }

  /** 
   * Index in a symmetrically stored matrix.
   * A symmetrically stored matrix contains:
   * <ol>
   * <li> the diagonal, as elements [0..n[
   * <li> the non-diagonal terms, order line-wise
   *      (upper triangular matrix)
   * </ol>
   * As an example for a 4x4 matrix:
   * <pre>
   * | 0  4  5  6 |
   * |    1  7  8 |
   * |       2  9 |
   * |          3 |
   * </pre>
   * @param  n Size of the matrix
   * @param  i line index, range [0..n[
   * @param  j column index, range [0..n[
   * @return the index in the symmetrically stored  matrix.
   */
  public static final int symIndex(int n, int i, int j) {
    if(i==j) return(i);
    return(i<j ?
        (2*n-i)*(i+1)/2 + j - (i+1) :
          (2*n-j)*(j+1)/2 + i - (j+1));
  }

  /** 
   * Convert a matrix into square matrix.
   * There are 2 types of non-square matrices:
   * <ol>
   * <li> a double-matrix of n columns and 2n lines,
   *      representing a linear transformation and its
   *      derivative (A,&Adot;)
   * <li> A symmetrically stored matrix contains:
   * <ol>
   * <li> the diagonal, as elements [0..n[
   * <li> the non-diagonal terms, order line-wise
   * </ol>.
   * As an example for a 4x4 matrix:
   * <pre>
   * | 0  4  5  6 |
   * |    1  7  8 |
   * |       2  9 |
   * |          3 |
   * </pre>
   * </ol>
   * @param  A   matrix in symmetrically stored
   * @return converted matrix
   */
  public static final double[] msq(double[] A) {
    int n = dim(A.length);
    if(n==0) return(null);	// impossible
    double[] R;
    int i,j,m;
    if(n<0) {		// symmetrically stored
      n = -n;
      R = new double[n*n];
      // Diagonal:
      for(i=m=0; i<n; i++, m+=n+1) R[m]=A[i];
      // Non-diagonal:
      for(i=0, m=n; i<n; i++) for(j=i+1; j<n; j++) 
        R[n*i+j] = R[n*j+i] = A[m++];
      return(R);
    }
    if(A.length==(n*n))	// already square
      return(A);
    // Create from (A,Adot):
    // |  A    0 |
    // | Adot  A |
    R = new double[2*A.length];
    // Left (A,Adot)
    for(i=m=0; i<A.length; i+=n) { 
      for(j=0;j<n;j++) R[m++]=A[i+j];
      for(j=0;j<n;j++) R[m++]=0;
    }
    // Bottom-right
    m = n*(2*n+1);
    for(i=0; m<R.length; m+=n) {
      for(j=0; j<n; j++) R[m++] = A[i++];
    }
    return(R);
  }

  /** 
   * Transposed of a 3x3 Matrix
   * @param  A input matrix
   * @return R  = <sup>t</sup>(A)
   */
  public static final double[] m3t(double[] A) {
    double[] R = new double[9];
    R[0] = A[0]; R[1] = A[3]; R[2] = A[6];
    R[3] = A[1]; R[4] = A[4]; R[5] = A[7];
    R[6] = A[2]; R[7] = A[5]; R[8] = A[8];
    return (R);
  }

  /** 
   * Transposed of a square Matrix
   * @param  A input matrix, any dimension
   * @return R  = <sup>t</sup>(A)
   */
  public static final double[] transposed(double[] A) {
    int size=A.length;
    if(size==9) return(m3t(A));
    int n = dim(size); 
    if(n<0)	// symmetrically stored
      return(A);
    if(n==0) {
      System.err.println("#***AstroMath.transposed: " + size
          + "-vector not a square matrix");
      return(null);
    }
    double[] T = new double[size];
    int i,j,m;
    for(i=0, m=0; i<n; i++) for(j=0; j<size; j+=n) 
      T[m++] = A[i+j];
    return(T);
  }

  /**
   * Convert 3x3 or 2x3x3 matrix into 6x6
   * @param  A 3x3 matrix (9-double array) or 2x3x3 (18-double array)
   *         or 2x6x6
   * @return 6x6 matrix defined as:<ul>
   *     <li> 3x3 matrix: the returned matrix is<pre>
   *          | R 0 |
   *          | 0 R |
   *          </pre>
   *     <li> 2x3x3 matrix (R,r): the returned matrix is<pre>
   *          | R 0 |
   *          | r R |
   *          </pre>
   * </ul>
   */
  public static final double[] m6(double[] A) {
    if(A.length==36) return(A);
    if((A.length!=9)&&(A.length!=18)) {
      return(null);
    }
    double[] R = new double[36];
    if(A==U3matrix) System.arraycopy(U6matrix, 0, R, 0, 36);
    else for(int i=0; i<18; i+=6) {
      int a = i/2;
      for(int j=0; j<3; j++) {
        R[i+j] = A[a+j];
        R[i+j+21] = A[a+j];
        R[i+j+18] = 0;
        R[i+j+3]  = 0;
      }
      if(A.length==18) for(int j=0; j<3; j++) 
        R[i+j+18] = A[a+j+9];
    }
    return(R);
  }

  /**
   * 3x3 Matrix Product.
   * The operands may be 2x3x3 arrays, in which case 
   * only the first 3x3 matrix is considered.
   * @param  A 3x3 matrix (9- or 18-double array)
   * @param  B 3x3 matrix (9- or 18-double array)
   * @return R    = A * B (9-double array)
   */
  public static final double[] m3p(double[] A, double[] B) {
    if(A==U3matrix) return(B);
    if(B==U3matrix) return(A);
    if((A.length>18)||(B.length>18)) {
      System.err.println("#***AstroMath.m3p(" + A.length + "x" + B.length 
          + ") not compatible with 3x3 matrices.");
      return(null);
    }
    double[] R = new double[9];
    for(int i=0; i<9; i+=3) for(int j=0; j<3; j++)
      R[i+j] = A[i]*B[j] + A[i+1]*B[j+3] + A[i+2]*B[j+6];
    return(R);
  }

  /**
   * Multiple 3x3 Matrix Product. 
   * 3x3 matrices are 1-D vectors (9 numbers).
   * @param  A 3x3 matrix (9-double array)
   * @param  M... 3x3 matrices (9-double array)
   * @return R    = A * B *...
   */
  public static final double[] m3pm(double[] A, double[]... M) {
    if(M.length<1) return(A);
    double[] R = m3p(A, M[0]);
    double[] T = null;
    for(int o=1; o<M.length; o++) {
      double[] B = M[o];
      if(B==U3matrix) continue;
      if(T==null) { T=R; R=new double[9]; }
      else System.arraycopy(R, 0, T, 0, 9);
      for(int i=0; i<9; i+=3) for(int j=0; j<3; j++)
        R[i+j] = T[i]*B[j] + T[i+1]*B[j+3] + T[i+2]*B[j+6];
    }
    return(R);
  }

  /** 
   * Square Matrix Product.
   * @param  A square matrix; may have a size double of B.
   * @param  B square matrix, same dimension as A or half size.
   * @return R = A B
   */
  public static final double[] mp(double[] A, double[] B) {
    int na = A.length;
    int nb = B.length;
    if((na==3)&&(nb==3)) return(m3p(A, B));
    int n = dim(A.length);
    if(n<=0) {	// impossible
      System.err.println("#***AstroMath.mp(" + A.length + " x " 
          + B.length + ") requires 2 square matrices of same dim.");
      return(null);
    }
    int i, j, k;
    double[] C = new double[A.length];
    for(i=0; i<A.length; i+=n) for(j=0; j<n; j++) {
      double w=0;
      for(k=0; k<n; k++)
        w += A[i+k]*B[k*n+j];
      C[i+j] = w;
    }
    return(C);
  }

  /**
   * Multiple nxn Matrix Product. 
   * @param  A nxn matrix (n²-double array)
   * @param  M 3x3 matrices (n²-double array)
   * @return R    = A * M *...
   */
  public static final double[] mpm(double[] A, double[]... M) {
    if(M.length<1) return(A);
    int n = dim(A.length);
    if(n<=0) return(null);
    double[] R = mp(A, M[0]);
    double[] T = null;
    //System.out.println("#...mpm(" + n + "x" + n + ")...");
    //if(DEBUG)printMatrix("#...(A):\n", A);
    //if(DEBUG)printMatrix("#.*.(1):\n", M[0]);
    //if(DEBUG)printMatrix("#...==>:\n", R);
    for(int o=1; (o<M.length)&&(R!=null); o++) {
      double[] B = M[o];
      //printMatrix("#.*.(" + (o+1) + "):\n", B);
      if(T==null) { T=R; R=new double[A.length]; }
      else System.arraycopy(R, 0, T, 0, A.length);
      for(int i=0; i<A.length; i+=n) for(int j=0; j<n; j++) {
        double w=0;
        for(int k=0; k<n; k++)
          w += T[i+k]*B[k*n+j];
        R[i+j] = w;
      }
      //printMatrix("#...==>:\n", R);
    }
    return(R);
  }

  /**
   * Product of a 3x3, 2x3x3 or 6x6 Matrix with a vector.
   * The dimensions may differ: a 6-vector is considered as 2 3-vectors, and
   * a 6x6 matrix as 4 sub-matrices.
   * @param  A 3x3, 2x3x6 (Rot+Spin) or 6x6 matrix
   * @param  B 3- or 6-vector
   * @param  R Result of the matrix product
   * @return  status: true (ok) or false (impossible)
   */
  public static final boolean m36v(double[] A, double[] B, double[] R) {
    int nv = B.length;
    if(A==U3matrix) {
      if(R!=B) System.arraycopy(B, 0, R, 0, nv);
      return(true);
    }
    int n, i, m;
    // Verify compatibility: n = size of A matrix
    if(A.length>=36) n=6;
    else if((A.length==9)||(A.length==18)) n=3;
    else return(false);
    double[] V;
    if(B==R) {      // vectors at same location
      V = new double[nv];
      System.arraycopy(B, 0, V, 0, nv);
    }
    else V = B;
    // Compute the (x,y,z)
    if(nv==3) for(i=0; i<3; i++) {
      // Computation of 3-vector
      m = i*n; 
      R[i] = A[m+0]*V[0] + A[m+1]*V[1] + A[m+2]*V[2];
    }
    else if(n==6) for(i=0; i<6; i++) {
      // 6x6 matrix, 6-vector
      m = i*n; 
      R[i] = A[m+0]*V[0] + A[m+1]*V[1] + A[m+2]*V[2]
          + A[m+3]*V[3] + A[m+4]*V[4] + A[m+5]*V[5];
    }
    else for(i=0; i<3; i++) {
      // 3x3 matrix, 6-vector
      m = 3*i; 
      R[i]   = A[m]*V[0] + A[m+1]*V[1] + A[m+2]*V[2];
      R[i+3] = A[m]*V[3] + A[m+1]*V[4] + A[m+2]*V[5];
      if(A.length==9) continue;
      // 2x3x3 matrix, 6-vector, add spin*pos
      m += 9;
      R[i+3] += A[m]*V[0] + A[m+1]*V[1] + A[m+2]*V[2];
    }
    return(true);
  }

  /**
   * Product of a 3x3 or 6x6 Matrix with a vector.
   * The dimensions may differ: a 6-vector is considered as 2 3-vectors, and
   * a 6x6 matrix as 4 sub-matrices.
   * @param  A 3x3 or 6x6 matrix
   * @param  V 3- or 6-vector
   * @return R    = A * V
   */
  public static final double[] m36v(double[] A, double[] V) {
    if(A==U3matrix) return(V);
    double[] R = new double[V.length];
    m36v(A, V, R);
    return(R);
  }

  /** 
   * Product of 2 matrices, or a matrix by a vector, in 3- or 6-D space.
   * The dimensions of the two operands may differ.
   * @param  A 3x3, 2x3x6 (Rot+Spin) or 6x6 or 2x6x6 operand
   * @param  B vector or matrix operand (3x1, 6x1, 3x3, 2x3x6 or 6x6)
   * @return R = A B
   */
  public static final double[] m36p(double[] A, double[] B) {
    if(B.length<=6)		// matrix * vector
      return(m36v(A, B));
    // Here A and B are 2 matrices of 9, 18 or 36 elements.
    //System.out.println("#...m36p: " + A.length + " x " + B.length);
    int na = A.length;
    int nb = B.length;
    int nx = na^nb;		// 0 if identical, odd if 3x3 implied
    int n = Math.max(na, nb);
    int i, j;
    double[] R = null;	// Result matrix

    if(nx==0) {	// Product of 2 nxn Matrices
      if(n==9)  return(m3p(A, B));
      if(n==18) {
        R = new double[n];
        // Product of rot/spin:  (A,Ad) * (B,Bd) = (A.B, Ad.B+A.Bd)
        for(i=0;i<3;i++) {
          int om = i*3;	// Main matrix
          int od = om+9;	// Dot  matrix
          for(j=0; j<3; j++) {
            R[om+j] = A[om+0]*B[j+0] + A[om+1]*B[j+3]  + A[om+2]*B[j+6];
            R[od+j] = A[od+0]*B[j+0] + A[od+1]*B[j+3]  + A[od+2]*B[j+6]
                + A[om+0]*B[j+9] + A[om+1]*B[j+12] + A[om+2]*B[j+15];
          }
        }
        return(R);
      }
      if(n==36) return(mp(A,B));
    }
    else if(nx==(9^18)) {
      // Product 9x18 or 18x9
      // 9x18 : (A,0) * (B,Bd) = (AB,A.Bd)
      // 18x9 : (A,Ad) * (B,0) = (AB,Ad.B)
      R = new double[n];
      for(i=0;i<3;i++) {
        int om = i*3;	// Main matrix
        int od = om+9;	// Dot  matrix
        for(j=0; j<3; j++) 
          R[om+j] = A[om+0]*B[j+0] + A[om+1]*B[j+3]  + A[om+2]*B[j+6];
        if(na>9) for(j=0; j<3; j++)  // Ad.B
          R[od+j] = A[od+0]*B[j+0] + A[od+1]*B[j+3]  + A[od+2]*B[j+6];
        if(nb>9) for(j=0; j<3; j++)  // A.Bd
          R[od+j] = A[om+0]*B[j+9] + A[om+1]*B[j+12] + A[om+2]*B[j+15];
      }
      return(R);
    }
    else if(n==36) {
      // Product 9x36, 18x36, 36x9 or 36x18
      //if(nb==36) return(mp(m6(A), B));
      //if(na==36) return(mp(A, m6(B)));
      R = new double[n];
    }
    if(R==null) {
      System.err.println("#***AstroMath.m36p(" + na + "," + nb
          + ") not valid: matrix product restricted to 3x3 or 6x6");
      return(null);
    }

    if(na==36) 
      for(i=0; i<9; i+=3) for(j=0; j<3; j++) for(int o=0; o<36; o+=(o&1)==0?3:15) {
        // A₀ A₁  B 0     A₀B A₁B
        // A₂ A₃  0 B     A₂B A₃B
        //             or
        // A₀ A₁  B  0     A₀B+A₁Bd A₁B
        // A₂ A₃  Bd B     A₂B+A₃Bd A₃B
        int oi = o+2*i;
        R[oi+j] = A[oi]*B[j] + A[oi+1]*B[j+3] + A[oi+2]*B[j+6];
        if(nb==9) continue;
        if((o&1)!=0) continue;
        // Add derivative
        R[oi+j] += A[oi+3]*B[j+9] + A[oi+4]*B[j+12] + A[oi+5]*B[j+15];
      }
    else if(nb==36) 
      for(i=0; i<9; i+=3) for(j=0; j<3; j++) for(int o=0; o<36; o+=(o&1)==0?3:15) {
        // A 0    B₀ B₁  : AB₀ AB₁
        // 0 A    B₂ B₃  : AB₂ AB₃
        //             or
        // A  0   B₀ B₁  : AB₀      AB₁
        // Ad A   B₂ B₃  : AB₂+AdB₀ AB₃+AdB₁
        int oj = o+j;
        R[oj+2*i] = A[i]*B[oj] + A[i+1]*B[oj+6] + A[i+2]*B[oj+12];
        //System.out.print(" (o=" + o + ")");
        if(na==9) continue;
        if(o <18) continue;
        R[oj+2*i] += A[i+9]*B[oj-18] + A[i+10]*B[oj-12] + A[i+11]*B[oj-6];
      }
    return(R);
  }

  //  ===========================================================
  //		Covariance matrices
  //		(stored as 1-D arrays)
  //  ===========================================================

  /** 
   * Product (T).(V).t(T), used for covariance matrix of a linear transformation.
   * The (V) matrix can be 3x3 or 6x6, symmetrically stored (9 or 21 elements),
   * or squared stored (9 or 36 values). 
   * The formula :  W(i,j) = Σ(k)Σ(l) T(i,k).V(k,l).T(j,l).
   * In the case where (T) is 3x3 and V is (6x6), the result is made of 4 sub-matrices:
   * <pre>
   * | T V₀ T'   T V₁ T' |
   * | T V₂ T'   T V₃ T' |
   * </pre>
   * @param  T 3x3 (9 elements) or 6x6 (36 elements) transformation matrix
   * @param  V 3x3 or 6x6 covariance matrix, squared or symmetrically stored.
   *         May be 6, 9, 21 or 36 elements.
   * @param  W Resulting matrix = T . V . <sup>t</sup>T.
   *         Must be of same dimension as V.
   *         May be at the same location as V if T is 3x3.
   * @return true if operation correct, false when impossible
    public static final boolean m36pt(double[] T, double[] V, double[] W) {
        if(T.length!=9) return(mpt(T, V, W));
        boolean sym = false;
        if(V.length==21) sym=true;
        else if(V.length!=36) return(mpt(T, V, W));
        if(V.length!=W.length) return(false); 
        double[] Vt = new double[9];	// Temporary
        //System.out.println("#...AstroMath.m36pt(" + T.length + ", " + V.length + " => " + W.length
        //        + "): sym=" + sym);
        //printMatrix("#...Variance 6x6 matrix (sym=" + sym + "):\n", V);
        // Faster method: use an array with indices in the right order:
        // -- for symmetrically stored:
        int[] ovq = { 0,1,2,    6,7,8,   12,13,14,
                      3,4,5,    9,10,11, 15,16,17,
                     21,22,23, 27,28,29, 33,34,35};
        // -- for squared stored:
        int[] ovs = { 0,6,7,    6,1,11,   7,11,2,
                      8,9,10,  12,13,14, 15,16,17,
                      3,18,19, 18,4,20,  19,20,5 };
        int[] ov = sym ? ovs : ovq;
        int i,j,k,l,q;
        int iq=0;
        for(q=0; q<3; q++) {
            // Explore the sub-matries upper-left, upper-right, bottom-right
            // Copy elements of V into Vt
            int oq = q*9;	// Offset in ovq/ovs
            for(i=0; i<9; i++) Vt[i] = V[ov[oq+i]];
            //printMatrix("#...q=" + q + ": ", Vt);
            // Compute elements and place them into W
            boolean diag = (q&1)==0;	// Sub-matrix along diagonal
            int iw=0;
            for(i=0; i<9; i+=3) for(j=0; j<9; j+=3, iw++) {
                // Compute element(i,j) = Σ(kl) T(i,k).T(j,l).V(k,l)
                if(diag && (j<i)) continue;
                double w=0;
                int it=0;
                for(k=0;k<3;k++) for(l=0;l<3;l++)
                    w += T[i+k]*T[j+l]*+Vt[it++];
                    //System.out.println("#...q=" + q + ", i=" + i + ", j=" + j 
                    //        + " => W[" + (sym?ovs[iq+iw]:iv+j) + "]="+ w);
                W[ov[oq+iw]] = w;
            }
        }
        //printMatrix("#...Combined matrix (bottom-left not filled):\n", W);
        if(sym) return(true);
        // Complete the lower triangle
        for(i=1; i<6; i++) for(j=0; j<i; j++) 
            W[i*6+j] = W[j*6+i];
        return(true);
    }
   */

  /** 
   * Product (T).(V).t(T), used for covariance matrix of a linear transformation.
   * The (V) matrix can be 3x3 or 6x6, symmetrically stored (9 or 21 elements),
   * or squared stored (9 or 36 values). 
   * @param  T 3x3 (9 elements) or 6x6 (36 elements) transformation matrix
   * @param  V 3x3 or 6x6 covariance matrix, squared or symmetrically stored.
   *         May be 6, 9, 21 or 36 elements.
   * @param  W Result = T . V . <sup>t</sup>T.
   *         W is symmetrically stored if V is symmetrically stored.
   * @return the resylt
    public static final double[] m36pt(double[] T, double[] V) {
        double[] W = new double[V.length];
        if(m36pt(T, V, W)) return(W);
        return(null);
    }
   */

  /** 
   * Product (T).(V).t(T), used for covariance matrix of a linear transformation.
   * The formula :  W(i,j) = Σ(k)Σ(l) T(i,k).V(k,l).T(j,l).
   * @param  T nxn or (n/2)*(n/2) transformation matrix
   * @param  V nxn covariance matrix, squared of symmetrically stored.
   * @param  W Result = T . V . <sup>t</sup>T.
   * @return true if operation correct, false when impossible
   */
  public static final boolean mpt(double[] T, double[] V, double[] W) {
    if(V==W) return(false); // same location...
    if(V.length!=W.length) return(false); 
    int nt = dim(T.length);
    int nv = dim(V.length);
    boolean sym = false;	// indicates symmetrically stored.
    if(nv<0) { nv = -nv; sym = true; }
    if(nt!=nv) {
      System.err.println("#***AstroMath.mpt(" + T.length + ","
          + V.length + "): incompatible dim. for T and V.");
      return(false);
    }
    int i, j, k, l;
    int iw = sym ? nv : 0;	// index in computed matrix, non-diagonal terms for sym.
    for (i=0; i<nv; i++) {
      int it = nv*i;
      if(!sym) iw += i;	// 1st element to compute (skip non-computed terms)
      for (j=i; j<nv; j++) {
        int jt = nv*j;
        int iv = sym ? nv : 0;
        double w=0 ;
        // Symmetrically stored: group T[i,k].T[j,l] with T[i,l].T[j,k]
        if(sym) for (k=0; k<nv; k++) {
          w += T[it+k]*T[jt+k]*V[k];  // diagonal term
          for(l=k+1; l<nv; l++) 
            w += (T[it+k]*T[jt+l]+T[it+l]*T[jt+k])*V[iv++];
        }
        // Standard square matrix
        else for (k=0; k<nv; k++) for (l=0; l<nv; l++) 
          w += T[it+k]*T[jt+l]*V[iv++];
        if(sym && i==j) W[i] = w;	// diagonal term
        else W[iw++] = w ;
      }
    }
    if(sym) return(true);
    /* Complete the lower part (j<i) */
    for(i=1; i<nv; i++) for(j=0; j<i; j++) 
      W[i*nv+j] = W[j*nv+i];
    return(true);
  }

  /** 
   * Product (T).(V).t(T), used for covariance matrix of a linear transformation.
   * The input matrix can be symmetrically stored (n(n+1)/2 values), 
   * or squared stored (n² values).
   * The formula :  W(i,j) = Σ(k)Σ(l) T(i,k).V(k,l).T(j,l).
   * @param  T nxn transformation matrix
   * @param  V nxn covariance matrix, squared of symmetrically stored.
   * @return V' = T . V . <sup>t</sup>T, symmetrically stored if V is symmetrically stored.
   */
  public static final double[] mpt(double[] T, double[] V) {
    double[] W = new double[V.length];
    if(mpt(T, V, W)) return(W);
    return(null);
  }

  /** (too generic? not yet used...)
   * Generalized product (T).(V).t(T), used to compute the covariance matrix of a linear transformation.
   * The (V) matrix can be symmetrically stored [n(n+1)/2 values], 
   * or squared stored [n² values], and the size of the (V) matrix can be
   * a multiple of the size of (T).
   * The formula :  W(i,j) = Σ(k)Σ(l) T(i,k).T(j,l).V(k,l)
   * In the case T[n/2,n/2], V is made of 4 sub-matrices, the result is
   * <pre>
   * | T V₀ T'   T V₁ T' |
   * | T V₂ T'   T V₃ T' |
   * </pre>
   * @param  T square transformation matrix
   * @param  V nxn covariance matrix, squared or symmetrically stored.
   * @param  W Result = T . V . <sup>t</sup>T.
   * @return true if operation correct, false when impossible
    public static final boolean mpt_gen(double[] T, double[] V, double[] W) {
        if(V==W) return(false); // same location...
        if(V.length!=W.length) return(false); 
        int nt = dim(T.length);
        int nv = dim(V.length);
        boolean sym = false;
        if(nv<0) { nv = -nv; sym = true; }
        int np=0;
        if(nt>0) {
            np = nv/nt;
            if(nt*np != nv) np=0;
        }
        if(np<1) {
            System.err.println("#***AstroMath.mpt_gen(" + T.length + ","
                + V.length + "): dim. incompatibles for T and V.");
            return(false);
        }
        // T can be made of submatrices; when np==1, identical with mpt(T, V, W).
        int i,j,k,l;
        if(DEBUG) System.out.println("#...AstroMath.mpt_gen(" + T.length + ", " + V.length + " => " + W.length
                + "): nt=" + nt + ", nv=" + nv + ", np=" + np + ", sym=" + sym);
        for(int ip=0; ip<np; ip++) for(int jp=ip; jp<np; jp++) {
            int ib=(ip*nt);	// Line number of upper left block
            int jb=(jp*nt);	// Column number of upper left block
            int ob=ib*nv+jb;	// Offset of sub-matrix in V, if V square
            boolean diag = (ip==jp);
            boolean dsym = diag&sym;
            // if(DEBUG) System.out.println("#...ip=" + ip + ", jp=" + jp + ", ob=" + ob + ", diag=" + diag);
            // Explore the sub-matrix # (ip, jp)
            for(i=0;i<nt;i++) {
                int iv = ib+i;	// Line number in V
                int iw =	// Offset of first number to compute in V
                   sym ? symIndex(nv, iv, diag ? iv+1 : jb) :
                         iv*nv + jb    + (diag ? i    : 0 );
                int it = i*nt;	// Offset of line i in T
                for(j=diag?i:0; j<nt; j++) {
                    int jt = j*nt;
                    double w=0;
                    if(dsym) for(k=0; k<nt; k++) {	
                        int kv = symIndex(nv, ib+k, ib+k+1);
                        w += T[it+k]*T[jt+k]*V[k+ib];	// diagonal
                        if(DEBUG) System.out.println("#...adding_diagonal (k=" + k + "): kv=" + kv + ", diag_V[" + (k+ip) + "]");
                        for(l=k+1; l<nt; l++)
                            w += (T[it+k]*T[jt+l]+T[it+l]*T[jt+k])*V[kv++];
                    }
                    else if(sym) for(k=0; k<nt; k++) {	
                        int kv = symIndex(nv, ib+k, jb);
                        if(DEBUG) System.out.println("#...adding_sym..... (k=" + k + "): kv=" + kv + ", diag_V[" + (k+ip) + "]");
                        for(l=0; l<nt; l++)
                            w += T[it+k]*T[jt+l] * V[kv++];
                    }
                    else for(k=0; k<nt; k++) {
                        int kv = ob + nv*k;
                        for(l=0; l<nt; l++) 
                            w += T[it+k]*T[jt+l] * V[kv+l];
                    }
                    if(DEBUG) System.out.println("#...ip=" + ip + ", jp=" + jp + ", i=" + i + ", j=" + j + ", dsym=" + dsym
                            + ", fill: W[" + (dsym&&(i==j)?ib+i:iw) + "]=" + w);
                    if(dsym&&(i==j)) W[ib+i] = w;
                    else             W[iw++] = w;
                }
            }
        }
        if(sym) return(true);
        // Complete the lower part (j<i) 
        for(i=1; i<nv; i++) {
            for(j=0; j<i; j++) W[i*nv+j] = W[j*nv+i];
        }
        return(true);
    }
     ---*/

  //  ===========================================================
  //		Rotation and Rotation+Spin matrices
  //		(stored as 1-D arrays)
  //  ===========================================================

  /** 
   * Generate the 3x3 rotation matrix from angles around the main axes.
   * @param axes       the text naming the axes of rotation, e.g. "zyz". 
   *                   The text may also specify units for angles:
   *                   r=radians, d=degrees, " or s=arcsec, m=milli-arcsec;
   *                   default is degrees. The unit specification must 
   *                   <em>precede</em> the list of axes.
   * @param angles	the angles in degrees (or as specified in axes).
   *                   As many arguments as the number of specified axes must be given.
   * @return R[9]	the 3x3 rotation matrix product of R(1).R(2)...
   * where R(1) is the rotation around the axis specified in the first char in "axes", 
   * etc.<br>
   * The rotation matrices around the axes are defined by:
   * <pre>
   *            R_x(θ)              R_y(θ)              R_z(θ)
   *     | 1    0     0  |   | cosθ  0  sinθ |   | cosθ -sinθ  0 |
   *     | 0  cosθ -sinθ |   |   0   1   0   |   | sinθ  cosθ  0 |
   *     | 0  sinθ  cosθ |   |-sinθ  0  cosθ |   |   0     0   1 |
   * </pre>
   */
  public static double[] rotation(String axes, double... angles) {
    if(axes.length()<1) return(U3matrix);
    double[] R = new double[9];
    double unit = DEG;
    char[] ax = axes.toCharArray();
    int nr = 0;
    System.arraycopy(U3matrix, 0, R, 0, 9);
    for(int i=0; i<ax.length; i++) {
      int i0,i1,i2,j0,j1,j2;
      char axis = Character.toLowerCase(axes.charAt(i));
      /* j0 is the axis, j1 adjacent, j2 third axis */
      switch(Character.toLowerCase(ax[i])) {
      case 'z': j0=2; j1=0; j2=1; break;
      case 'y': j0=1; j1=2; j2=0; break;
      case 'x': j0=0; j1=1; j2=2; break;
      case 'r':  unit=1;       continue;
      case '"':
      case 's':  unit=ARCSEC;  continue;
      case 'm':  unit=MAS;     continue;
      case '\u00B0':
      case 'd':  unit=DEG;     continue;
      default:
        System.err.println("#+++AstroMath.rotation(" + axes + "): char#" + i 
            + " <" + ax[i] + "> stops the computation.");
        return(R);
      }
      //System.out.println("#...rotation(" + axis + "): value=" + angles[nr] + ", unit=" + unit);
      double a = angles[nr++]/unit; if(a==0) continue;
      double c = Math.cos(a);
      double s = Math.sin(a);
      double a01,a11,a21,a02,a12,a22;	// saved columns
      i0=j0*3; i1=j1*3; i2=j2*3;
      a01 = R[i0+j1]; a11 = R[i1+j1]; a21 = R[i2+j1];	// col.j1
      a02 = R[i0+j2]; a12 = R[i1+j2]; a22 = R[i2+j2];	// col.j2
      R[i0+j1] = a01*c + a02*s;
      R[i1+j1] = a11*c + a12*s;
      R[i2+j1] = a21*c + a22*s;
      R[i0+j2] = a02*c - a01*s;
      R[i1+j2] = a12*c - a11*s;
      R[i2+j2] = a22*c - a21*s;
    }
    return(R) ;
  }

  /** 
   * Generate the 2x3x3 rotation + spin matrix from angles and spins around the main axes.
   * The number of axes is limited to 3. Example of usage:<br>
   * <em>AstroMath.rotspin(<tt>"sxyzm"</tt>, rot_x, rot_y, rot_z, spin_x, spin_y, spin_z)</em><br>
   * where <em>rot_x</em>, <em>rot_y</em>, and <em>rot_z</em> are the rotation angles in
   * <em>arcsec</em> (the initial <tt>s</tt> indicates this unit), and the spin angles
   * <em>spin_x</em>, <em>spin_y</em>, and <em>spin_z</em> are in <em>mas/yr</em>
   * (the final <tt>m</tt> indicates this unit). 
   * Default units are degrees, and degrees/year.
   * @param axes       the text naming the axes used in rotations and spins;
   *                   it may also specify units, as in {@link #rotation}
   * @param angles_spins	the angles for rotations as in {@link #rotation}, 
   *                   followed by the same number of spins.
   * @return R[18]	a set of 2 3x3 matrices containing (rotation, spin)
   * <pre>
   *     | R  |
   *     | Rd |
   * </pre>
   * where <b>R</b> is the (orthogonal) 3x3 rotation matrix, 
   * and <b>Rd</b> its derivative.
   */
  public static double[] rotspin(String axes, double... angles_spins) {
    if(axes.length()<1) return(Urotspin);
    //double[] R = new double[18];
    // A temporary array made of (nr+1)*9 doubles is created, with
    // [0]=combined rotation, 
    // [1]=combined derivative relative to first axis
    // etc...
    char[] ax = axes.toCharArray();
    double[] R = new double[9*(ax.length+1)];   // Rot+Spins
    double unit = DEG;
    int i0,i1,i2,j0,j1,j2,o;
    int nr=0;	// number of rotation axes
    System.arraycopy(U3matrix, 0, R, 0, 9);
    for(int i=9; i<18; i++) R[i]=0; // No spin
    for(int i=0; i<axes.length(); i++) {
      /* j0 is the axis, j1 adjacent, j2 third axis */
      //if(DEBUG) System.out.println("#rotspin: nr=" + nr + ", i=" + i + ", axis=" + axis + ":");
      switch(Character.toLowerCase(ax[i])) {
      case 'z': j0=2; j1=0; j2=1; break;
      case 'y': j0=1; j1=2; j2=0; break;
      case 'x': j0=0; j1=1; j2=2; break;
      case 'r':  unit=1;       continue;
      case '"':
      case 's':  unit=ARCSEC;  continue;
      case 'm':  unit=MAS;     continue;
      case '\u00B0':
      case 'd':  unit=DEG;     continue;
      default:
        System.err.println("#***AstroMath.rotspin(" + axes + "): char#" + i 
            + " <" + ax[i] + "> not understood!");
        return(null);
      }
      double a = angles_spins[nr++]/unit; 
      double c = Math.cos(a);
      double s = Math.sin(a);
      double a01,a11,a21,a02,a12,a22;	// saved columns
      System.arraycopy(R, 0, R, nr*9, 9); // For next derivative
      // Rotate the matrices. not necessary for zero angle
      i0=j0*3; i1=j1*3; i2=j2*3;
      if(a!=0) for(int k=0; k<nr; k++) {
        o=9*k;
        a01=R[o+i0+j1]; a11=R[o+i1+j1]; a21=R[o+i2+j1];	// col.j1
        a02=R[o+i0+j2]; a12=R[o+i1+j2]; a22=R[o+i2+j2];	// col.j2
        R[o+i0+j1] = a01*c + a02*s;
        R[o+i1+j1] = a11*c + a12*s;
        R[o+i2+j1] = a21*c + a22*s;
        R[o+i0+j2] = a02*c - a01*s;
        R[o+i1+j2] = a12*c - a11*s;
        R[o+i2+j2] = a22*c - a21*s;
      }
      //if(DEBUG) System.out.println("#rotspin: nr=" + nr + ", i=" + i + ", rotated " + (a*AstroMath.MAS) + "mas around axis=" + axis + ":");
      //if(DEBUG) double[] m = new double[18];
      //if(DEBUG) System.arraycopy(R,  0, m, 0, 18); System.out.print(toString("(0+1) ", m));
      //if(DEBUG) System.arraycopy(R, 18, m, 0, 18); System.out.print(toString("(2+3) ", m));
      //if(DEBUG) System.arraycopy(R,18, m, 0, 9); System.out.print(toString("(2) ", m));
      //if(DEBUG) System.arraycopy(R,27, m, 0, 9); System.out.print(toString("(3) ", m));
      // Multiply the last matrix by derivative (c->-s, s->c)
      o=nr*9;
      a01=R[o+i0+j1]; a11=R[o+i1+j1]; a21=R[o+i2+j1];	// col.j1
      a02=R[o+i0+j2]; a12=R[o+i1+j2]; a22=R[o+i2+j2];	// col.j2
      R[o+i0+j0] = 0; R[o+i1+j0] = 0; R[o+i2+j0] = 0;	// col.j0
      R[o+i0+j1] = -a01*s + a02*c;
      R[o+i1+j1] = -a11*s + a12*c;
      R[o+i2+j1] = -a21*s + a22*c;
      R[o+i0+j2] = -a02*s - a01*c;
      R[o+i1+j2] = -a12*s - a11*c;
      R[o+i2+j2] = -a22*s - a21*c;
      //if(DEBUG) System.out.println("#rotspin: nr=" + nr + ", i=" + i + ", computed derived matrix:");
      //if(DEBUG) System.arraycopy(R,  0, m, 0, 18); System.out.print(toString("(0+1) ", m));
      //if(DEBUG) System.arraycopy(R, 18, m, 0, 18); System.out.print(toString("(2+3) ", m));
      //if(DEBUG) System.arraycopy(R,18, m, 0, 9); System.out.print(toString("(2) ", m));
      //if(DEBUG) System.arraycopy(R,27, m, 0, 9); System.out.print(toString("(3) ", m));
      //if(DEBUG) System.out.print("\n");
      //System.out.println(toString("#rotspin: nr=" + nr + ", i=" + i 
      //    + ", rotated " + (a*AstroMath.MAS) + "mas around axis=" + axis + ":\n", R));
    }
    if(nr==0) return(U3matrix);
    // Sum the partial derivatives
    for(int i=0; i<nr; i++) {
      double a = angles_spins[nr+i]/unit; 
      o=(i+1)*9;
      if(i==0) for(int k=0; k<9; k++) R[9+k] *= a;
      else if(a==0) continue;
      else     for(int k=0; k<9; k++) R[9+k] += a*R[o+k];
      //if(DEBUG) System.out.println("#..._spin: nr=" + nr + ", i=" + i + ", spin=" + (a*MAS));
      //if(DEBUG) System.arraycopy(R, 0, m, 0, 9); System.out.print(toString("(0) ", m));
      //if(DEBUG) System.arraycopy(R, 9, m, 0, 9); System.out.print(toString("(1) ", m));
    }
    // Copy into 2x3x6
    double[] S = new double[18];
    System.arraycopy(R,0,S,0,18);
    return(S) ;
  }

  /** 
   * Inverse a rotation or rotspin matrix.
   * @param  A input matrix, represents a rotation (orthogonal 3x3 matrix),
   *           or a rotation+spin matrix (2x3x3 matrix).
   * @return Inverse matrix A<sup>-1</sup>
   */
  public static final double[] rot_inv(double[] A) {
    int size=A.length;
    if(size==9) return(m3t(A));
    if(size!=18) {
      System.err.println("#***AstroMath.rot_inv: " + size
          + "-vector not rotation or rotspin matrix");
      return(null);
    }
    // Inverse of rotspin matrix (A,Ad):
    // inverse is (t(A), -t(A)(Ad)t(A)
    double[] T = new double[size];
    int i,j,m;
    for(i=0, m=0; i<3; i++) for(j=0; j<9; j+=3) 
      T[m++] = A[i+j];
    for(i=0; i<9; i+=3) for(j=0; j<3; j++) {
      double w = 0;
      m = j*3;
      for(int k=0; k<3; k++) w += A[m+k]
          * (T[i]*A[k+9] + T[i+1]*A[k+12] + T[i+2]*A[k+15]);
      T[9+i+j] = -w;
    } 
    return(T);
  }

  //  ===========================================================
  //		6D-Propagation (Rotion) matrices
  //		(stored as 1-D arrays)
  //  ===========================================================

  /** 
   * Generate 6-D motion (propagation) matrix (for use in stellar motion movement)
   * @param t      the time interval of the motion.
   * @return T[36] the 6x6 translation matrix <br><pre>
   *        | [1]  t.[1] |
   *        | [0]    [1] |
   * where [1] is the 3x3 unity matrix,</pre>.
   */
  public static double[] motionMatrix(double t) {
    double[] T = new double[36];
    System.arraycopy(U6matrix, 0, T, 0, 36);
    T[3]=t; T[10]=t; T[17]=t;
    return(T);
  }

  /** (not useful, could be problematic)
   * Apply a motion on a 6x6 matrix, left-side.
   * @param t  the time interval of the motion.
   * @param M  the 6x6 matrix, modified as  <tt>motionMatrix</tt>(t)*M
   * @return   status: false if not possible
    public static boolean motion(double t, double[] M) {
        if(M.length!=36) return(false);
        if(t==0) return(true);
        if(M==U6matrix) {
            System.err.println("#***AstroMath.motion(" + t 
                    + ") applied on read-only U6matrix");
            return(false);
        }
        for(int i=0; i<18; i+=6) for(int j=0; j<6; j++) 
            M[i+j] += t*M[i+j+18];
            // Result is | M₀+M₂t  M₁+M₃t |
            //           |    M₂      M₃  |
        return(true);
    }
   */

  /** (not useful, could be problematic)
   * Apply a motion on a 6x6 matrix, right-side.
   * @param M  the 6x6 matrix, modified as  M*<tt>motionMatrix</tt>(t)
   * @param t  the time interval of the motion.
   * @return   status: false if not possible
    public static boolean motion(double[] M, double t) {
        if(M.length!=36) return(false);
        if(t==0) return(true);
        if(M==U6matrix) return(motion(t, M));
        for(int i=0; i<36; i+=6) for(int j=0; j<3; j++) 
            M[i+j+3] += t*M[i+j];
            // Result is | M₀  M₁+M₀t |
            //           | M₂  M₃+M₂t |
        return(true);
    }
   */

  /** 
   * Combine a motion with a rotation/spin matrix
   * @param t   	the time interval of the motion.
   * @param M   	a 3x3 (rotation) or 2x3x3 (rotspin) or 6x6 (rotation+spin) matrix.
   * @return T[36]	the product   motionMatrix(t) . M, generally 6x6 matrix
   */
  public static double[] motionMatrix(double t, double[] M) {
    //if(DEBUG)System.out.print(toString("#...motionMatrix(dt=" + t + ") with matrix:\n", M));
    if(t==0) {
      if((M.length==9)||(M.length==36)) return(M);
    }
    double[] T; 
    if(M.length == 36) {
      T = new double[36];
      System.arraycopy(M, 0, T, 0, 36);
    }
    else T = m6(M);
    for(int i=0; i<18; i+=6) for(int j=0; j<6; j++) 
      T[i+j] += t*T[i+j+18];
    // Result is | M₀+M₂t  M₁+M₃t |
    //           |    M₂      M₃  |
    return(T);
  }

  /** 
   * Combine a rotation/spin matrix with a motion.
   * @param M   	a 3x3 (rotation) or 6x6 (rotation+spin) matrix.
   * @param t   	the time interval of the motion.
   * @return T[36]	the product   M . motionMatrix(t).
   **/
  public static double[] motionMatrix(double[] M, double t) {
    if(t==0) {
      if((M.length==9)||(M.length==36)) return(M);
    }
    double[] T; 
    if(M.length == 36) {
      T = new double[36];
      System.arraycopy(M, 0, T, 0, 36);
    }
    else T = m6(M);
    for(int i=0; i<36; i+=6) for(int j=0; j<3; j++) 
      T[i+j+3] += t*T[i+j];
    // Result is | M₀  M₁+M₀t |
    //           | M₂  M₃+M₂t |
    return(T);
  }

  /** 
   * Combine a motion with 2 rotation/spin matrices.
   * @param A   	a 3x3 (rotation) or 2x3x3 (rotspin) or 6x6 (rotation+spin) matrix.
   * @param t   	the time interval of the motion.
   * @param B   	a 3x3 (rotation) or 2x3x3 (rotspin) or 6x6 (rotation+spin) matrix.
   * @return T[36]	the product   A . motionMatrix(t) . B, generally 6x6 matrix
   */
  public static double[] motionMatrix(double[] A, double t, double[] B) {
    if(t==0) return(m36p(A, B));
    return(m36p(A, motionMatrix(t,B)));
  }

  //  ===========================================================
  //		Edition of vectors and matrices
  //		(stored as 1-D arrays)
  //  ===========================================================

  /**
   * Edition of the 3 components of a vector.
   * @param	u the 3-vector
   * @return	the equivalent string (edited vector)
   */
  protected static final String toString(double u[]) {
    return(toString("", u));
  }

  /**
   * Edition of the components of a vector (3 or 6 components) 
   * or of a matrix (3x3, 2x3x3 or 6x6).
   * @param	title text prefixing each line, if not ending by 
   *          a newline (<tt>'\n'</tt>).
   * @param	u the 3-vector or matrix to edit.
   *          A vector can be 3- or 6-D, and the matrix can
   *          be 3x3 (9 doubles), 2x3x3 (18 doubles) or 6x6 (36 doubles).
   * @return	the equivalent string (edited vector)
   */
  protected static final String toString(String title, double u[]) {
    String b24 = "                        ";
    StringBuilder b = new StringBuilder(title);
    StringBuilder blanks = null;
    boolean add_nl = title.endsWith("\n");
    if(u==null) {
      if(add_nl) b.deleteCharAt(b.length()-1);
      b.append(" (null)");
      if(add_nl) b.append('\n');
      return (b.toString());
    }
    boolean is_rotspin = u.length==18;
    boolean is_sym = false;
    int i, j, len;
    int nj = u.length; 	// Number of elements per line
    if(add_nl) { blanks = new StringBuilder("    "); b.append(blanks); }
    else if(nj>6) {         // Is a matrix
      add_nl = true;      // Terminate with a newline
      int nt = title.length();
      blanks = new StringBuilder(nt);
      for(i=0; i<nt; i++) blanks.append(' ');
    }
    if(nj>6) {
      if((len = dim(nj))<0) { is_sym = true; nj = -len; }
      else nj = nj>18 ? 6 : 3;
    }
    //b.append("[nj=" + nj + ", is_sym=" + is_sym + ", is_rotspin=" + is_rotspin + "]\n"); System.out.print(b);
    boolean add_sep = nj>3;
    int i_max = u.length>6 ? nj : 1;
    //if(is_rotspin) System.out.println("#...Edit spin matrix: " + u.length + ", nj=" + nj);
    for(i=0; i<i_max; i++) {
      int m = is_sym ? (i+1)*(2*nj-i)/2 : i*nj; //i*(i+1)/2 : i*nj;
      if(i>0) { 
        len = b.length()-1; 
        // remove trailing blanks:
        while(b.charAt(len)==' ') b.setLength(len--);
        b.append('\n'); 
        b.append(blanks); 
      }
      boolean edit_dot = is_rotspin;
      while(true) {
        //if(is_rotspin) System.out.println("#...edit_dot=" + edit_dot + ", i=" + i + ", m=" + m + ", nj=" + nj);
        if(edit_dot^is_rotspin) b.append("; ");
        for(j=0; j<nj; j++) {
          double v, av;
          if(is_sym) {
            if(j<i) { b.append(b24); continue; }
            if(j==i) v = u[i];
            else     v = u[m++];
          }
          else v = u[m+j];
          len = b.length()+23;
          //if(is_sym && (j>i)) continue;
          // v = u[m+j];
          av = Math.abs(v);
          //if(av>=100.0) ed.editDouble(b, v, 12, 3, 0);
          b.append(v>=0 ? ' ' : '-'); b.append(Math.abs(v)); 
          for(len-=b.length(); len>0; len--) b.append(' ');
          b.append(' ');
        }
        if(edit_dot) { m += 9; edit_dot ^= true; }
        else break;
      }
    }
    if(add_nl) b.append('\n');
    return (b.toString()); // Buffer converted to String
  }

  //  ===========================================================
  //		Routine to check the consistency of arrays
  //  ===========================================================

  /**
   * Difference (Distance) between 2 arrays, defined as max(|V[i]-R[i]) (largest difference)
   * @param	val   array of values to compare
   * @param	ref   array of reference values
   * @return	distance between the 2 arrays.
   *          Negative large value is returned in case of incompatibility.
   */
  static public final double diffArray(double[] val, double[] ref) {
    int nv = val==null ? 0 : val.length;
    int nr = ref==null ? 0 : ref.length;
    if((nr==0)||(nv==0)||(nr!=nv)) {
      //System.out.println("#+++diffArray: attempting to compare " 
      //        + nv + "- and " + nr + "-vectors");
      return(-1./eps);
    }
    double dist=0;
    for(int i=0; i<nr; i++) {
      double adif = Math.abs(val[i]-ref[i]); 
      if(adif>dist) dist=adif;
    } 
    return(dist);
  }

  /**
   * Verification of an array vs a known array.
   * @param	title to issue
   * @param	val   array of values to compare
   * @param	ref   array of reference values
   * @param	dmax   maximum acceptable (zero or negative for ε)
   * @return	true if difference always smaller than dmax.
   */
  static public boolean checkArray(String title, double[] val, double[] ref, double dmax) {
    System.out.print(title + "(dmax=" + dmax + ")"); 
    int nv = val==null ? 0 : val.length;
    int nr = ref==null ? 0 : ref.length;
    if(dmax<0)  dmax = -dmax*eps;
    if(dmax==0) dmax = eps;
    double diff = diffArray(val, ref);
    if(diff<0) { 
      System.out.println("#+++checkArray: attempting to compare " 
          + nv + "- and " + nr + "-arrays");
      return(false);
    }
    boolean ok=true;
    if(diff>dmax) {	// List the various extrama
      for(int i=0; i<nr; i++) {
        double dv = (val[i]-ref[i]);
        if(Math.abs(dv)<=dmax) continue;
        dv = Math.rint(10.*dv/dmax)/10.0;
        System.out.print(" [" + i + (dv>0? "]+" : "]") + dv);
        if(Math.abs(dv)>=4.0) System.out.print("**");
      }
      ok=false;
    }
    diff = Math.rint(10.*diff/dmax)/10.0;
    // Not too many decimals:
    String amax = Double.toString(dmax);
    if(amax.length()>8) {
      int i = amax.indexOf("E");
      if(i>0) {
        double x = Double.valueOf(amax.substring(0,i));
        amax =  Double.toString(Math.rint(1000.*x)/1000.) + amax.substring(i);
      }
      else amax = Double.toString(Math.rint(1000.0*dmax)/1000.);
    }
    double m = Math.log10(dmax);
    double e = Math.floor(m);
    dmax = dexp(e)*Math.rint(1000.*dexp(m-e))/1000.;
    System.out.print("; max=(" + amax + ")*" + diff);
    if(ok) System.out.println(" (ok)");
    else System.out.println(" (" + diff + ")" + (diff>8.0 ? "**" : "++"));
    return(ok);
  }
  /**
   * Verification of an array vs a known array.
   * @param	title to issue
   * @param	val   array of values to compare
   * @param	ref   array of reference values
   * @return	true if difference always smaller than dmax.
   */
  static public boolean checkArray(String title, double[] val, double[] ref) {
    return(checkArray(title, val, ref, 4.*eps));
  }

  /**
   * Verification of a unit matrix.
   * @param	title to issue
   * @param	val   array of (9, 18 or 36) values to compare 
   *                to unity matrix.
   * @return	true if ma
   */
  static public boolean checkUnity(String title, double[] val) {
    double[] ref = U3matrix;
    if(val.length==36) ref = U6matrix;
    if(val.length==18) ref = Urotspin;
    return(checkArray(title, val, ref, 4.0*eps));
  }

  /**
   * Print a Matrix
   * @param	title to issue; terminate the title with a <i>newline</i> to issue a line with title.
   * @param	matrix   the matrix to edit (can be a simple vector)
   */
  static final public void printMatrix(String title, double[] matrix) {
    System.out.print(toString(title, matrix));
    if(!title.endsWith("\n")) System.out.println("");
  }

  /**
   * Print an Array
   * @param	title to issue
   * @param	matrix   the matrix to edit (can be a simple vector)
   */
  static final public void printArray(String title, double[] matrix) {
    System.out.print(toString(title, matrix));
  }

  /**
   * Testing the functions stored here.
   * or of a matrix (3x3, 2x3x3 or 6x6).
   * @param	verbose a verbosity level, number with 0 = less verbose, 1 = more verbose, ...
   * @param	level   a depth level (unused)
   * @return	true if ok.
   */
  public static boolean test(int verbose, int level) {
    boolean ok=true;
    System.out.println("#===AstroMath.test: verbosity=" + verbose
        + ", level=" + level + "; ε=" + eps);
    double[] Rx = rotation("x", 30.0);
    double[] Ry = rotation("y", 45.0);
    double[] Rz = rotation("z", 60.0);
    double[] Rp = m3pm(Rx, Ry, Rz);
    double[] Rc = rotation("xyz", 30., 45., 60.0);
    if(verbose>0) {
      printMatrix("# Rx(30°): ", Rx);
      printMatrix("# Ry(45°): ", Ry);
      printMatrix("# Rz(60°): ", Rz);
      printMatrix("# Product: ", Rp);
      printMatrix("# combin.: ", Rc);
    }
    if(verbose<=0) ok &= diffArray(Rp, Rc)>2.0*eps;
    else ok &= checkArray("#---Combined rotation vs matrix product", Rp, Rc);
    if(!ok) System.out.println("#+++ok=false (1)");
    double[] rotspin  = rotspin("xyz", 10., -20., 30., -1.0, 2.0, -3.0);
    double[] rotspinr = rotspin("zyx", -30., 20., -10., 3.0, -2.0, 1.0);
    double[] rotspin1 = rot_inv(rotspin);
    double[] rotspinx = m36p(Rx, rotspin);
    double[] rotspini = m36p(rot_inv(rotspin), rot_inv(Rx));
    if(verbose>0) {
      System.out.println("");
      printMatrix("#[0]zero rotspin matrix\n", Urotspin);
      printMatrix("#[1]rotspin(xyz, 10, -20, 30, -1, 2, -3)\n", rotspin);
      printMatrix("#[2]...Inverse of this rotspin matrix..:\n", rotspin1);
      printMatrix("#[3]rotspin(zyx, -30, 20, -10, 3, -2, 1)\n", rotspinr);
      printMatrix("#[4].....................Rx*rotspin.....\n", rotspinx);
      printMatrix("#[5]................Reverse*Rx-1........\n", rotspini);
    }
    if(verbose>0) {
      printMatrix("#---Use msq(rotspin):\n", msq(rotspin));
      printMatrix("#....or .m6(rotspin):\n", m6(rotspin));
      ok &= checkArray("#---check     [2]=[3]", rotspinr, rotspin1);
      ok &= checkArray("#---check [1]x[2]=[0]", m36p(rotspin, rotspin1), Urotspin);
      ok &= checkArray("#---check inv.[4]=[5]", rot_inv(rotspinx), rotspini);
      ok &= checkUnity("#---check [1]x[2]=.1.", m36p(rotspin, rotspin1));
    } 
    else ok &= diffArray( rotspinr, rotspin1) > 2.5*eps
        && diffArray( m36p(rotspin, rotspin1), Urotspin) > 2.5*eps
        && diffArray( rot_inv(rotspinx), rotspini) > 2.5*eps
        && diffArray( m36p(rotspin, rotspin1), Urotspin) > 2.5*eps;
        if(!ok) System.out.println("#+++ok=false (2)");
        // Verify rot/rot_inv
        int i,j,m=level*100;
        double[] saved_rotspin = rotspin.clone();
        System.out.print("#...Verify " + m + "x matrix product:");
        for(i=0; i<m; i++) {
          if(i<5) System.out.print(" " + i); 
          else if(i==5) System.out.print(" ...");
          else if(i>m-3) System.out.print(" " + i);
          rotspin = mpm(rotspin, rotspin1, saved_rotspin);
        }
        checkArray("", rotspin, saved_rotspin);
        ok &= diffArray(rotspin, saved_rotspin)<m*eps;
        rotspin = saved_rotspin;
        // Verify index
        //if(verbose>0) {
        //    System.out.println("#...Check symIndex:");
        //    for(i=0;i<3;i++) for(j=0;j<3;j++) 
        //        System.out.println("#...(" + i + "," + j + ") =>" + symIndex(3,i,j));
        //}
        // Checking mpt with 3x3 matrix, Rx as transformation
        double[] s = { 1.0, 2.0, 4.0,  1.0, 2.0,  3.0}; //{ 1.0,  1.0, 2.0,  2.0, 3.0, 4.0 };
        double[] v = msq(s);
        if(verbose>0) {
          printMatrix("#===symmetric 3x3:\n", s);
          printMatrix("#===full      3x3:\n", v);
          printMatrix("#===Transorm  3x3:\n", Rx);
          printMatrix("#---Symmetric mpt:\n", mpt(Rx, s));
          printMatrix("#---full      mpt:\n", mpt(Rx, v));
        }
        // Checking mpt with 6x6 matrix
        if(verbose>0) System.out.println("#---Verify covariance matrix");
        double[] T = motionMatrix(1.0, rotspin); 
        double[] S = new double[21];	// Symmetric matrix
        for(i=0, m=6; i<6; i++) {
          S[i] = Math.rint(100.*Math.random())/50.;
          for(j=i+1; j<6; j++) 
            S[m++] = Math.rint(2000.*Math.random()-1000.)/1000.;
        }
        double[] V = msq(S);
        if(verbose>0) {
          printMatrix("#---Error+Correlation Matrix:\n", S);
          printMatrix("#---Error+Correlation Matrix [6x6]:\n", V);
        }
        // Convert correlation into covariances
        for(i=0, m=6; i<6; i++) for(j=i+1; j<6; j++) 
          S[m++] *= S[i]*S[j];
        // Variances:
        for(i=0; i<6; i++) 
          S[i] *= S[i];
        if(verbose>0) printMatrix("#---Covariance Matrix:\n", S);
        for(i=0; i<6; i++) for(j=0; j<6; j++) {
          if(i!=j) V[i*6+j] *= V[i*(6+1)]*V[j*(6+1)];
        }
        for(i=0; i<6; i++) { m = (6+1)*i; V[m] *= V[m]; }
        if(verbose>0) printMatrix("#---Covariance Matrix [6x6]:\n", V);
        // Verification propagation error
        double[] Saved = S.clone();
        //propagate_error(S, 2.0); printMatrix("#---propagate_error(2):\n", S);
        //printMatrix("#---computed via mpt:\n", mpt(motionMatrix(2.0), Saved));
        // Set 20 values of T to zero
        for(i=m=0; i<6; i++) for(j=0; j<6; j++, m++) {
          if((i!=j)&&((m&1)!=0)) T[m]=0;
        }
        double[] Vt = mpt(T, V);
        double[] V3 = mpm(T, V, transposed(T));
        double[] Vz = mpt(m6(Rz), V);
        //double[] Vz36 = m36pt(m6(Rz), V);
        double[] WS = mpt(m6(Rz), S); 
        if(verbose>0) {
          printMatrix("#---Linear T:\n", T);
          printMatrix("#===Transformed Covariance [6x6]:\n", Vt);
          printMatrix("#===computed via .mpm()... [6x6]:\n", V3);
          checkArray("#===Diff. for the above .. [6x6]:", V3, Vt);
          printMatrix("#===Transformed Covariance as Symmetric:\n", mpt(T, S));
          printMatrix("#....................(as.6x6)...........\n", msq(mpt(T, S)));
          printMatrix("#===Linear 3x3 transformation with Rz:\n", Rz);
          printMatrix("#         (6x6) .....................:\n", m6(Rz));
          printMatrix("#...compute cov(Rz.V) via 6x6:\n", Vz);
          //printMatrix("#...compute cov(Rz.V) via m36pt\n", Vz36);
          //checkArray("#===Diff. cov(Rz.V):", Vz, Vz36);
          System.out.println("#---With symmetric matrix:");
          printMatrix("#...Compute cov(Rz.Sym) via mpt(m6(Rz)):\n", WS);
          //printMatrix("#...compute cov(Rz.Sym) with m36pt ....:\n", m36pt(Rz, S));
          //checkArray("#===Diff. the 2 symmetric matrices:", m36pt(Rz, S), WS);
          System.out.println("#===Compare the 36 covariances:   ");
        }
        else ok &= diffArray(V3, Vt) > 2.0*eps
            //&& diffArray(Vz, Vz36) > 2.0*eps
            && diffArray(mpt(Rz, S), WS) > 2.0*eps;
            if(!ok) System.out.println("#+++ok=false (3)");
            if(ok) System.out.println("#---End of AstroMath.tests: ok");
            else   System.out.println("#***Bad ** AstroMath.tests?");
            return(ok);
  }

}
