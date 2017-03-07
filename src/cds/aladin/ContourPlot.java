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

public final class ContourPlot extends ContourAlgorithm {
	
	// Below, data members which store the grid steps,
	// the z values, the interpolation flag, the dimensions
	// of the contour plot and the increments in the grid:
	int		xSteps, ySteps;
	private short		z[][];
	
	private int compteur;	    	// pour les thread

	// flag mis a true lorsque l'on debute un nouveau contour
	private boolean newContour; 

	// variables de travail
	private PointD p1,p2,pTemp,firstPoint;

	// Below, data members, most of which are adapted from
	// Fortran variables in Snyder's code:
	int	l1[] = new int[4];
	int	l2[] = new int[4];
	int	ij[] = new int[2];
	int	i1[] = new int[2];
	int	i2[] = new int[2];
	int	i3[] = new int[6];
	int	ibkey,icur,jcur,ii,jj,elle,ix,iedge,iflag,ni,ks;
	int	idir,nxidir,k;
	int	z1,z2;
	double  cval;
	double	intersect[]	= new double[4];
	double	xy[]		= new double[2];
	double	prevXY[]	= new double[2];
	float	lev;
	boolean	workSpace[];
	boolean	jump;
	
	private double	prevU,prevV,u,v;
	
	// variable servant a la construction des contours
	private PointD[] Contours;
	int index;
	int nb=0;
	

	//-------------------------------------------------------
	// A constructor method.
	//-------------------------------------------------------
	public ContourPlot() {
		super();
		init();
	}

	//-------------------------------------------------------
	private int sign(int a, int b) {
		a = Math.abs(a);
		if (b < 0)	return -a;
		else		return  a;
	}

	
	//-------------------------------------------------------
	// "DrawKernel" is the guts of drawing and is called
	// directly or indirectly by "ContourPlotKernel" in order
	// to draw a segment of a contour or to set the pen
	// position "prevXY". Its action depends on "iflag":
	//
	// iflag == 1 means Continue a contour
	// iflag == 2 means Start a contour at a boundary
	// iflag == 3 means Start a contour not at a boundary
	// iflag == 4 means Finish contour at a boundary
	// iflag == 5 means Finish closed contour not at boundary
	// iflag == 6 means Set pen position
	//
	//-------------------------------------------------------
	void DrawKernel() {
		

		if ((iflag == 1) || (iflag == 4) || (iflag == 5)) {
			
			prevU = (prevXY[0] - 1.0);
			prevV = (prevXY[1] - 1.0);
			u = (xy[0] - 1.0);
			v = (xy[1] - 1.0);

			// Interchange horizontal & vertical
			
		        //System.out.println(prevU +" " +prevV);
		        //System.out.println(u+" "+v+"\n \n");
		        p1 = new PointD(prevU,prevV);
		        //PointD p1 = new PointD(Math.round(prevU),Math.round(prevV));
		        p2 = new PointD(u,v);
		        //PointD p2 = new PointD(Math.round(u),Math.round(v));

				if(newContour) {
					newContour = false;
					firstPoint = p1;
				}
				pTemp = new PointD(-1.0,-1.0);
				if(index>0) pTemp = Contours[index-1]; 
		        //if( index==0 || ! p1.equals(Contours[index-1]) )// a remettre
				if( index==0 || pTemp==null || ! ( (int)p1.x==(int)pTemp.x && (int)p1.y==(int)pTemp.y ) )
		        {check();Contours[index]=p1;index++;}

				pTemp = Contours[index-1];
		        //if( ! p2.equals(Contours[index-1]) )// a remettre
				if( ! ( (int)p2.x==(int)pTemp.x && (int)p2.y==(int)pTemp.y ) )
		        {check();Contours[index]=p2;index++;}
		        
		        nb+=2;
		        
		}
		prevXY[0] = xy[0];
		prevXY[1] = xy[1];
	}

	//-------------------------------------------------------
	// "DetectBoundary"
	//-------------------------------------------------------
	void DetectBoundary() {
		ix = 1;
		if (ij[1-elle] != 1) {
			ii = ij[0] - i1[1-elle];
			jj = ij[1] - i1[elle];
			ii = ij[0] + i2[elle];
			jj = ij[1] + i2[1-elle];
			ix = 0;
			
			if (ij[1-elle] >= l1[1-elle]) {
				ix = ix + 2;
				return;
			}
		}
		ii = ij[0] + i1[1-elle];
		jj = ij[1] + i1[elle];
		
	}

	//-------------------------------------------------------
	// "Routine_label_020" corresponds to a block of code
	// starting at label 20 in Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	boolean Routine_label_020() {
		l2[0] =  ij[0];
		l2[1] =  ij[1];
		l2[2] = -ij[0];
		l2[3] = -ij[1];
		idir = 0;
		nxidir = 1;
		k = 1;
		ij[0] = Math.abs(ij[0]);
		ij[1] = Math.abs(ij[1]);
		
		elle = 0;
		return false;
	}

	//-------------------------------------------------------
	// "Routine_label_050" corresponds to a block of code
	// starting at label 50 in Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	boolean Routine_label_050() {
		while (true) {
			if (ij[elle] >= l1[elle]) {
				if (++elle <= 1) continue;
				elle = idir % 2;
				ij[elle] = sign(ij[elle],l1[k-1]);
				if (Routine_label_150()) return true;
				continue;
			}
			
			ii = ij[0] + i1[elle];
			jj = ij[1] + i1[1-elle];
			
			break;
		}
		jump = false;
		return false;
	}

	//-------------------------------------------------------
	// "Routine_label_150" corresponds to a block of code
	// starting at label 150 in Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	boolean Routine_label_150() {
	        compteur++;
		while (true) {
			//------------------------------------------------
			// Lines from z[ij[0]-1][ij[1]-1]
			//	   to z[ij[0]  ][ij[1]-1]
			//	  and z[ij[0]-1][ij[1]]
			// are not satisfactory. Continue the spiral.
			//------------------------------------------------
			if (ij[elle] < l1[k-1]) {
				ij[elle]++;
				if (ij[elle] > l2[k-1]) {
					l2[k-1] = ij[elle];
					idir = nxidir;
					nxidir = idir + 1;
					k = nxidir;
					if (nxidir > 3) nxidir = 0;
				}
				ij[0] = Math.abs(ij[0]);
				ij[1] = Math.abs(ij[1]);
				
				elle = 0;
				return false;
			}
			if (idir != nxidir) {
				nxidir++;
				ij[elle] = l1[k-1];
				k = nxidir;
				elle = 1 - elle;
				ij[elle] = l2[k-1];
				if (nxidir > 3) nxidir = 0;
				continue;
			}

			if (ibkey != 0) return true;
			ibkey = 1;
			ij[0] = icur;
			ij[1] = jcur;
			if (Routine_label_020()) continue;
			return false;
		}
	}

	//-------------------------------------------------------
	// "Routine_label_200" corresponds to a block of code
	// starting at label 200 in Synder's subroutine "GCONTR".
	// It has return values 0, 1 or 2.
	//-------------------------------------------------------
	short Routine_label_200()
	{
		while (true) {
			xy[elle] = ij[elle] + intersect[iedge-1];
			xy[1-elle] = ij[1-elle];
			workSpace[2*(xSteps*(ij[1]-1)
				+ij[0]-1) + elle] = true;
			DrawKernel();
			if (iflag >= 4) {
				icur = ij[0];
				jcur = ij[1];
				return 1;
			}
			ContinueContour();
			if (!workSpace[2*(xSteps*(ij[1]-1)+ij[0]-1)+elle]) return 2;
			iflag = 5;		// 5. Finish a closed contour
			iedge = ks + 2;
			if (iedge > 4) iedge = iedge - 4;
			intersect[iedge-1] = intersect[ks-1];
		}
	}

	//-------------------------------------------------------
	// "CrossedByContour" is true iff the current segment in
	// the grid is crossed by one of the contour values and
	// has not already been processed for that value.
	//-------------------------------------------------------
	boolean CrossedByContour() {
		ii = ij[0] + i1[elle];
		jj = ij[1] + i1[1-elle];
		z1 = z[ij[0]-1][ij[1]-1];
		z2 = z[ii-1][jj-1];
		int i = 2*(xSteps*(ij[1]-1) + ij[0]-1) + elle;

		if (!workSpace[i]) {
			if ((lev>Math.min(z1,z2)) && (lev<=Math.max(z1,z2))) {
				workSpace[i] = true;
				return true;
			}
		}
		return false;
	}

	//-------------------------------------------------------
	// "ContinueContour" continues tracing a contour. Edges
	// are numbered clockwise, the bottom edge being # 1.
	//-------------------------------------------------------
	void ContinueContour() {
		short local_k;

		ni = 1;
		if (iedge >= 3) {
			ij[0] = ij[0] - i3[iedge-1];
			ij[1] = ij[1] - i3[iedge+1];
		}
		for (local_k = 1; local_k < 5; local_k++)
			if (local_k != iedge) {
				ii = ij[0] + i3[local_k-1];
				jj = ij[1] + i3[local_k];
				z1 = z[ii-1][jj-1];
				ii = ij[0] + i3[local_k];
				jj = ij[1] + i3[local_k+1];
				z2 = z[ii-1][jj-1];
				if ((cval > Math.min(z1,z2) && (cval <= Math.max(z1,z2)))) {
					if ((local_k == 1) || (local_k == 4)) {
						int zz = z2;

						z2 = z1;
						z1 = zz;
					}
					intersect[local_k-1] = (cval - z1)/(z2 - z1);
					
					ni++;
					ks = local_k;
				}
			}
		if (ni != 2) {
			//-------------------------------------------------
			// The contour crosses all 4 edges of cell being
			// examined. Choose lines top-to-left & bottom-to-
			// right if interpolation point on top edge is
			// less than interpolation point on bottom edge.
			// Otherwise, choose the other pair. This method
			// produces the same results if axes are reversed.
			// The contour may close at any edge, but must not
			// cross itself inside any cell.
			//-------------------------------------------------
			ks = 5 - iedge;
			if (intersect[2] >= intersect[0]) {
				ks = 3 - iedge;
				if (ks <= 0) ks = ks + 4;
			}
		}
		//----------------------------------------------------
		// Determine whether the contour will close or run
		// into a boundary at edge ks of the current cell.
		//----------------------------------------------------
		elle = ks - 1;
		iflag = 1;		// 1. Continue a contour
		jump = true;
		if (ks >= 3) {
			ij[0] = ij[0] + i3[ks-1];
			ij[1] = ij[1] + i3[ks+1];
			elle = ks - 3;
		}
	}

	//-------------------------------------------------------
	// "ContourPlotKernel" is the guts of this class and
	// corresponds to Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	void ContourPlotKernel()
	{
		short val_label_200;

		l1[0] = xSteps;	l1[1] = ySteps;
		l1[2] = -1;l1[3] = -1;
		i1[0] =	1; i1[1] =  0;
		i2[0] =	1; i2[1] = -1;
		i3[0] =	1; i3[1] =  0; i3[2] = 0;
		i3[3] =	1; i3[4] =  1; i3[5] = 0;
		prevXY[0] = 0.0; prevXY[1] = 0.0;
		xy[0] = 1.0; xy[1] = 1.0;
		iflag = 6;
		DrawKernel();
		icur = Math.max(1, Math.min((int)Math.floor(xy[0]), xSteps));
		jcur = Math.max(1, Math.min((int)Math.floor(xy[1]), ySteps));
		ibkey = 0;
		ij[0] = icur;
		ij[1] = jcur;
		if (Routine_label_020() &&
			 Routine_label_150()) return;
		if (Routine_label_050()) return;
		while (true) {
			DetectBoundary();
			if (jump) {
				if (ix != 0) iflag = 4; // Finish contour at boundary
				iedge = ks + 2;
				if (iedge > 4) iedge = iedge - 4;
				intersect[iedge-1] = intersect[ks-1];
				val_label_200 = Routine_label_200();
				if (val_label_200 == 1) {
					if (Routine_label_020() && Routine_label_150()) return;
					if (Routine_label_050()) return;
					continue;
				}
				if (val_label_200 == 2) continue;
				return;
			}
			if ((ix != 3) && (ix+ibkey != 0) && CrossedByContour()) {
				newContour = true;
				// nouveau contour pour ce niveau
				if(index>0 && p2!=null ) {
					if( !p2.equals(Contours[index-1]) && distance(p2,firstPoint)<2 ) {
						check();
						Contours[index]=p2;
						index++;
					}
				}
				check();
				Contours[index]=null;
				index++;
				
				// An acceptable line segment has been found.
				// Follow contour until it hits a
				// boundary or closes.
				//
				//System.out.println("BEGIN CONTOUR");
				iedge = elle + 1;
				cval = lev;
				if (ix != 1) iedge = iedge + 2;
				iflag = 2 + ibkey;
				intersect[iedge-1] = (cval - z1) / (z2 - z1);
				val_label_200 = Routine_label_200();
				if (val_label_200 == 1) {
					if (Routine_label_020() && Routine_label_150()) return;
					if (Routine_label_050()) return;
					continue;
				}
				if (val_label_200 == 2) continue;
				return;
			}
			if (++elle > 1) {
				elle = idir % 2;
				ij[elle] = sign(ij[elle],l1[k-1]);
				if (Routine_label_150()) return;
			}
			if (Routine_label_050()) return;
		}
	}

    protected PointD[] getContours() {
                init();
                
                int a;
                
                // z a initialiser a partir de data
                z = new short[width][height];
                for(int j=0;j<height;j++) {
                   a=j*width;
                   for(int i=0;i<width;i++) {
                      z[i][j] = data[a+i];
                   }
                }
                
		xSteps = width-1;
		ySteps = height-1;
		int workLength = 2 * xSteps * ySteps;
    	 
		
		workSpace = new boolean[workLength];
		ContourPlotKernel();
       
       // on enleve les elts null a la fin du tableau
       PointD[] tmp = new PointD[index];
       System.arraycopy(Contours,0,tmp,0,index);
       Contours = tmp;
       
	   //System.out.println("NB points avant cleanContours : "+Contours.length);
       // nettoyage des contours (ne sert plus a rien depuis passage en double)
       //Contours = cleanContours(Contours);
	   //System.out.println("NB points apres cleanContours : "+Contours.length);
       
       pc.updatePourcent( 1.0 );
       
       return Contours;
    }
    
    // initialise certaines variables (important lorsqu'on relance un calcul avec un niveau different)
    protected void init() {
        compteur=0;
        index=0;
        nb=0;
    	Contours = new PointD[5000];
    }
    
    /** renvoie un tableau agrandi */
    private PointD[] realloc(PointD[] tab) {
        PointD[] tmp = new PointD[tab.length+5000];
        System.arraycopy(tab,0,tmp,0,tab.length);
        return tmp;
    }
    
    /** verifie que le tableau est assez grand, appelle la fct de reallocation sinon */
    private void check() {
        if(index==Contours.length) Contours = realloc(Contours);
    }

    protected void setLevel(double level) {
        this.level = level;
        lev = (float)level;
    }

	/** retourne la distance euclidienne entre les points a et b */
	private double distance(PointD a, PointD b) {
		return Math.sqrt( Math.pow(a.x-b.x,2) + Math.pow(a.y-b.y,2) );
	}
  
}
