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

/*
 * Created on 01-Sep-2005
 *
 * To change this generated comment go to
 * Window>Preferences>Java>Code Generation>Code Template
 */
package cds.aladin;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Hashtable;

import cds.savot.model.FieldSet;
import cds.savot.model.MarkupComment;
import cds.savot.model.ParamSet;
import cds.savot.model.ResourceSet;
import cds.savot.model.SavotField;
import cds.savot.model.SavotParam;
import cds.savot.model.SavotResource;
import cds.savot.model.SavotTR;
import cds.savot.model.SavotTable;
import cds.savot.model.SavotVOTable;
import cds.savot.model.TDSet;
import cds.savot.model.TRSet;
import cds.savot.model.TableSet;
import cds.savot.pull.SavotPullEngine;
import cds.savot.pull.SavotPullParser;
import cds.tools.Util;

/** Classe parsant les footprint preview
 * @author Thomas Boch [CDS]
 * @kickoff 01/09/2005
 */
public class FootprintParser {

    static private final String SPHERICAL_COORDS = "stc:AstroCoordSystem.CoordFrame.SPHERICAL";

	private MyInputStream mis;
	private byte[] beginStream;

	// variable de travail pour le parsing
	private SavotVOTable votable;
	private Hashtable<String, FootprintBean> hash;

	// ensemble des resources à traiter
	private SavotResource[] resources;

	boolean sphericalCoordinates = false;


	static private Hashtable<String, FootprintBean> footprintHash; // conserve la mémoire des footbeans créés

	static {
		footprintHash = new Hashtable<String, FootprintBean>();
	}

	/**
	 *
	 * @param mis stream partiellement entamé par le parsing des objets de base
	 */
	public FootprintParser(MyInputStream mis, byte[] beginStream) {
		this.mis = mis;
		this.beginStream = beginStream;
	}

	/**
	 *
	 * @param resources tableau des RESOURCE à parser
	 */
	public FootprintParser(SavotResource[] resources) {
		this.resources = resources;
	}

	/**
	 *
	 * @return la Hashtable donnant un objet Footprint d'après son nom
	 */
	public Hashtable<String, FootprintBean> getFooprintHash() {
		InputStream is=null;
		ResourceSet resSet = new ResourceSet();
		try {
			// cas où on a passé un inputstream comme constructeur
			if( mis!=null ) {
				is = buildInputStream();
				SavotPullParser parser = new SavotPullParser(is, SavotPullEngine.FULL,null,false);
				votable = parser.getVOTable();

				resSet = votable.getResources();
			}
			// cas où on a passé un tableau de SavotResource
			else {
				for( int i=0; i<resources.length; i++ ) resSet.addItem(resources[i]);
			}
		}
		catch(Exception e) {
			e.printStackTrace();
			Aladin.error("Problem during parsing of footprints !");
			return null;
		}
		finally{ if( is!=null ) try { is.close(); } catch( Exception e) {} };

		int nbRes = resSet.getItemCount();
//		System.out.println(nbRes);
		hash = new Hashtable<String, FootprintBean>();

		for( int i=0; i<nbRes; i++ ) {
			Aladin.trace(3, "Footprint: Processing resource: "+i);
			processFovResource(resSet.getItemAt(i));
		}
//		new Footprint(raOffset,deOffset,)

		return hash;
	}

	// conserve les correspondances ID-->objet VOTable
	private Hashtable<String, MarkupComment> refMem;

	int counter=0;
	int tabIndex = 0;

	/**
	 * construit le footprint associé à une RESOURCE
	 *
	 * @param res la RESOURCE "racine", pouvant contenir d'autres RESOURCE
	 */
	private void processFovResource(SavotResource res) {
		FootprintBean fpBean = new FootprintBean();

		sphericalCoordinates = false;

		tabIndex = 0;

		SubFootprintBean sub;

//	    System.out.println("Processing resource "+res);
		refMem = new Hashtable<String, MarkupComment>();
		String id = res.getId();
        // par défaut
        fpBean.setInstrumentName(id);

		// dangereux, car c'est sur cet ID qu'on va référencer un FoV
//		if( id==null || id.length()==0 ) id = res.getName();


		// traitement des params au niveau RESOURCE
		ParamSet params = res.getParams();
		int nbParam = params.getItemCount();
		SavotParam param;
		for( int i=0; i<nbParam; i++ ) {
			param = params.getItemAt(i);

			if ( param.getUtype().trim().equalsIgnoreCase(SPHERICAL_COORDS)) {
			    sphericalCoordinates = true;
			}

			// position RA du FoV
			if( param.getUcd().equalsIgnoreCase("pos.eq.ra;meta.main") ) {
				fpBean.setRa(param.getValue());
			}

			// position DE du FoV
			else if( param.getUcd().equalsIgnoreCase("pos.eq.dec;meta.main") ) {
				fpBean.setDe(param.getValue());
			}

            // position RA du centre de rotation du FoV  (PF jan 09)
            if( param.getUcd().equalsIgnoreCase("pos.eq.ra") ) {
                fpBean.setRaRot(param.getValue());
            }

            // position DE du centre de rotation du FoV  (PF jan 09)
            else if( param.getUcd().equalsIgnoreCase("pos.eq.dec") ) {
                fpBean.setDeRot(param.getValue());
            }
			// angle position du FoV
			else if( param.getUcd().equalsIgnoreCase("pos.posAng") || param.getUtype().equals(SIAPExtBuilder.FOV_POS_ANGLE) ) {
				double d;
				try {
					d = Double.valueOf(param.getValue()).doubleValue();
				}
				catch( NumberFormatException e ) {
				    continue;
//				    d = 0.;
				}
				fpBean.setPosAngle(d);
			}

			// caractère "movable" du FoV
			else if( param.getName().equalsIgnoreCase("Movable") ) {
				boolean b;
				try {
					b = Boolean.valueOf(param.getValue()).booleanValue();
				}
				catch( NumberFormatException e ) {continue;}
				fpBean.setMovable(b);
			}

			// caractère "rollable" du FoV
			else if( param.getName().equalsIgnoreCase("Rollable") ) {
				boolean b;
				try {
					b = Boolean.valueOf(param.getValue()).booleanValue();
				}
				catch( NumberFormatException e ) {continue;}
				fpBean.setRollable(b);
			}

            // convention interne à Aladin pour affichage dans JTable
            else if( param.getId().equals("InstrumentDescription") ) {
                fpBean.setInstrumentDesc(param.getValue());
            }
            // convention interne à Aladin pour affichage dans JTable
            else if( param.getId().equals("InstrumentName") ) {
                fpBean.setInstrumentName(param.getValue());
            }
            // convention interne à Aladin pour affichage dans JTable
            else if( param.getId().equals("TelescopeName") ) {
                fpBean.setTelescopeName(param.getValue());
            }
			// convention interne à Aladin pour affichage dans JTable
            else if( param.getId().equals("Origin") ) {
                fpBean.setOrigin(param.getValue());
            }

		}

		// traitement de la RESOURCE racine
		sub = processResource(res);
		if( sub!=null && sub.getNbOfSubParts()>0 ) fpBean.addSubFootprintBean(sub);

		/*
		for( int i=0; i<nbTab; i++ ) {
			processTable((SavotTable)tables.getItemAt(i), i);
		}
		*/

		// traitement des RESOURCEs dans RESOURCE (Tom Donaldson)
		ResourceSet resources = res.getResources();
		for( int i=0; i<resources.getItemCount(); i++ ) {
		    sub = processResource(resources.getItemAt(i));
		    if( sub!=null ) {
		    	fpBean.addSubFootprintBean(sub);
		    	// on garde en mémoire les sous-parties d'un FoV --> on les place pour cela dans un container
		    	String subfpId = resources.getItemAt(i).getId();
		    	if (subfpId!=null && subfpId.length()>0) {
		    	    FootprintBean container = new FootprintBean();
		    	    container.addSubFootprintBean(sub);
		    	    container.setDisplayInFovList(false);
		    	    // on évite d'écraser un bean existant par un sub-bean
		    	    if ( ! hash.contains(subfpId) ) {
		    	        hash.put(subfpId, container);
		    	    }
		    	}
		    }
		}


		hash.put(id, fpBean);


		// on ne vérifie plus l'existence d'un bean avec le meme nom, on écrase
		if( footprintHash.get(id)!=null ) {
			Aladin.trace(1, "Footprint with ID "+id +"already exists ...\n Existing definition will be erased");
		}
		
		Aladin.trace(3, "Footprint : add to footprintHash footprint with key "+id+"**");
		footprintHash.put(id, fpBean);
	}

	/** traite une RESOURCE en la considérant comme sous-partie d'un FOV
	 *
	 * @param res
	 */
	private SubFootprintBean processResource(SavotResource res) {
		SubFootprintBean subFpBean = new SubFootprintBean();
		subFpBean.setInSphericalCoords(sphericalCoordinates);

		TableSet tables = res.getTables();
		int nbTab = tables.getItemCount();
		SubFootprintBean sub;

		for( int i=0; i<nbTab; i++ ) {
			// TODO : prendre en compte couleur éventuelle au niveau de la TABLE (pour les types STRING notamment)
			sub = processTable(tables.getItemAt(i));
			if( sub!=null ) {
				subFpBean.addSubFootprintBean(sub);
			}
//			System.out.println("sub : "+sub);
		}

		String id = res.getName();
		subFpBean.setName(id);
		subFpBean.setColor(getColor(res));

		return subFpBean;
	}

	/*
	static protected PlanField getPlanFieldFromID(String id) {
	    if( footprintHash==null ) return null;
		FootprintBean fpBean = (FootprintBean)footprintHash.get(id);
		if( fpBean==null ) return null;
//		return new PlanField(Aladin.aladin, fpBean.getRaOffset(), fpBean.getDecOffset(), fpBean.boundary, fpBean.names, id);
		return new PlanField(Aladin.aladin, fpBean, id);
	}
	*/

	static protected FootprintBean getBeanFromID(String id) {
		if( footprintHash==null ) return null;
		// on ne prend pas en compte la casse pour rechercher le bean correspondant
		Enumeration<String> e = footprintHash.keys();
		String key;
		while( e.hasMoreElements() ) {
			key = e.nextElement();
			if( key.equalsIgnoreCase(id) ) return footprintHash.get(key);
		}

		return null;
	}

	/*
	private void processTable(SavotTable table, int index) {
		String type = getRegionType(table);
		if( type==null || !( type.equals("Box") || type.equals("Polygon") )  ) return;

		String id = table.getId();
		names[index] = id;

		if( type.equals("Polygon") ) {
			FieldSet fields = table.getFields();
			int nbFields = fields.getItemCount();

			int idxRAOffset = -1;
			int idxDEOffset = -1;
			SavotField field;
			String utype;
			// boucle sur les champs
			for( int i=0; i<nbFields; i++ ) {
				field = (SavotField)fields.getItemAt(i);
				field = getField(field);
				utype = field.getUtype();
				if( utype.equals("stc:AstroCoordArea/Region/reg:Polygon/Vertex/Position[1]")) {
					idxRAOffset = i;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Polygon/Vertex/Position[2]") ) {
					idxDEOffset = i;

					continue;
				}
			}
			// pas de traitement possible dans un tel cas
			if( idxRAOffset<0 || idxDEOffset<0 ) return;

			TRSet trs = table.getData().getTableData().getTRs();
			int nbRows = trs.getItemCount();
			raOffset[index] = new double[nbRows];
			deOffset[index] = new double[nbRows];
			SavotTR tr;
			double raOff,deOff;
			raOff = deOff = 0.;
			String tmp;
			TDSet tds;
			counter += nbRows;
			boundary[index] = counter;
			for( int i=0; i<nbRows; i++ ) {
				tr = (SavotTR)trs.getItemAt(i);
				tds = tr.getTDs();
				try {

					tmp = tds.getContent(idxRAOffset);
//					System.out.println(tmp);
					raOff = Double.valueOf(tmp).doubleValue();

					tmp = tds.getContent(idxDEOffset);
//					System.out.println(tmp+"\n");
					deOff = Double.valueOf(tmp).doubleValue();
				}
				catch(NumberFormatException e) {e.printStackTrace();}
				// TODO : vérifier unité et faire conversion en degrés
				raOffset[index][i] = raOff/3600.;
				deOffset[index][i] = deOff/3600.;
			}

		}

		else if( type.equals("Box") ) {
			ParamSet params= table.getParams();
			int nbParams = params.getItemCount();

			SavotParam ctrRAOffsetParam = null;
			SavotParam ctrDEOffsetParam = null;
			SavotParam sizeRAParam = null;
			SavotParam sizeDEParam = null;
			SavotParam param;
			String utype;
			// boucle sur les params
			for( int i=0; i<nbParams; i++ ) {
				param = (SavotParam)params.getItemAt(i);
				param = getParam(param);
				utype = param.getUtype();
				if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Center[1]")) {
					ctrRAOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Center[2]") ) {
					ctrDEOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Size[1]") ) {
					sizeRAParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Size[2]") ) {
					sizeDEParam = param;

					continue;
				}

			}
			// pas de traitement possible dans un tel cas
			if( ctrRAOffsetParam==null || ctrDEOffsetParam==null || sizeRAParam==null || sizeDEParam==null ) {
				return;
			}

			raOffset[index] = new double[4];
			deOffset[index] = new double[4];

			counter += 4;
			boundary[index] = counter;

			double ctrRAOffset,ctrDEOffset,sizeRA,sizeDE;
			ctrRAOffset = ctrDEOffset = sizeRA = sizeDE = -1;
			try {
				ctrRAOffset = Double.valueOf(ctrRAOffsetParam.getValue()).doubleValue();
				ctrDEOffset = Double.valueOf(ctrDEOffsetParam.getValue()).doubleValue();
				sizeRA = Double.valueOf(sizeRAParam.getValue()).doubleValue();
				sizeDE = Double.valueOf(sizeDEParam.getValue()).doubleValue();
			}
			catch(NumberFormatException e) {e.printStackTrace();}

			int signRA, signDE;
			for( int i=0; i<4; i++ ) {
				signRA = (i==1||i==2)?1:-1;
				signDE = (i==0||i==1)?1:-1;

				// TODO : conversion en degrees en tenant compte des unités dans le VOTable!!
				raOffset[index][i] = (ctrRAOffset+signRA*sizeRA*0.5)/3600.;
				deOffset[index][i] = (ctrDEOffset+signDE*sizeDE*0.5)/3600.;
			}

		}

	}
	*/

	private SubFootprintBean processTable(SavotTable table) {
		String type = getRegionType(table);

		// si le type n'est pas un des types supportés ...
		if( type==null ||  !( type.equals("Box") || type.equals("Polygon")
			           || type.equals("Circle")  || type.equals("Pickle") || type.equals("String") )  ) {
			return null;
		}



		String id = table.getId();
		SubFootprintBean subFpBean = null;

		if( type.equals("Polygon") ) {
			FieldSet fields = table.getFields();
			int nbFields = fields.getItemCount();

			int idxRAOffset = -1;
			int idxDEOffset = -1;
			SavotField field;
			String utype;
			// boucle sur les champs
			for( int i=0; i<nbFields; i++ ) {
				field = fields.getItemAt(i);
				field = getField(field);
				utype = field.getUtype();
				if( utype.equals("stc:AstroCoordArea/Region/reg:Polygon/Vertex/Position[1]") ||
                    utype.equalsIgnoreCase("stc:AstroCoordArea.Polygon.Vertex.Position.C1")    ) {
					idxRAOffset = i;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Polygon/Vertex/Position[2]") ||
                         utype.equalsIgnoreCase("stc:AstroCoordArea.Polygon.Vertex.Position.C2")) {
					idxDEOffset = i;

					continue;
				}
			}
			// pas de traitement possible dans un tel cas
			if( idxRAOffset<0 || idxDEOffset<0 ) return null;

			TRSet trs = table.getData().getTableData().getTRs();
			int nbRows = trs.getItemCount();
			double[] raOffset = new double[nbRows];
			double[] deOffset = new double[nbRows];
			SavotTR tr;
			double raOff,deOff;
			raOff = deOff = 0.;
			String tmp;
			TDSet tds;
			counter += nbRows;
			for( int i=0; i<nbRows; i++ ) {
				tr = trs.getItemAt(i);
				tds = tr.getTDs();
				try {

					tmp = tds.getContent(idxRAOffset);
//					System.out.println(tmp);
					raOff = Double.valueOf(tmp).doubleValue();

					tmp = tds.getContent(idxDEOffset);
//					System.out.println(tmp+"\n");
					deOff = Double.valueOf(tmp).doubleValue();
				}
				catch(NumberFormatException e) {e.printStackTrace();}
				// TODO : vérifier unité et faire conversion en degrés
				raOffset[i] = raOff/3600.;
				deOffset[i] = deOff/3600.;

			}

			subFpBean = new SubFootprintBean(raOffset, deOffset, id);
			subFpBean.setInSphericalCoords(sphericalCoordinates);

			return subFpBean;
		}

        // TODO : nouveau format
		else if( type.equals("Box") ) {
			ParamSet params= table.getParams();
			int nbParams = params.getItemCount();

			SavotParam ctrRAOffsetParam = null;
			SavotParam ctrDEOffsetParam = null;
			SavotParam sizeRAParam = null;
			SavotParam sizeDEParam = null;
			SavotParam param;
			String utype;
			// boucle sur les params
			for( int i=0; i<nbParams; i++ ) {
				param = params.getItemAt(i);
				param = getParam(param);
				utype = param.getUtype();
				if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Center[1]")) {
					ctrRAOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Center[2]") ) {
					ctrDEOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Size[1]") ) {
					sizeRAParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Box/Size[2]") ) {
					sizeDEParam = param;

					continue;
				}

			}
			// pas de traitement possible dans un tel cas
			if( ctrRAOffsetParam==null || ctrDEOffsetParam==null || sizeRAParam==null || sizeDEParam==null ) {
				return null;
			}

			double[] raOffset = new double[4];
			double[] deOffset = new double[4];

			counter += 4;

			double ctrRAOffset,ctrDEOffset,sizeRA,sizeDE;
			ctrRAOffset = ctrDEOffset = sizeRA = sizeDE = -1;
			try {
				ctrRAOffset = Double.valueOf(ctrRAOffsetParam.getValue()).doubleValue();
				ctrDEOffset = Double.valueOf(ctrDEOffsetParam.getValue()).doubleValue();
				sizeRA = Double.valueOf(sizeRAParam.getValue()).doubleValue();
				sizeDE = Double.valueOf(sizeDEParam.getValue()).doubleValue();
			}
			catch(NumberFormatException e) {e.printStackTrace();return null;}

			int signRA, signDE;
			for( int i=0; i<4; i++ ) {
				signRA = (i==1||i==2)?1:-1;
				signDE = (i==0||i==1)?1:-1;

				// TODO : conversion en degrees en tenant compte des unités dans le VOTable!!
				raOffset[i] = (ctrRAOffset+signRA*sizeRA*0.5)/3600.;
				deOffset[i] = (ctrDEOffset+signDE*sizeDE*0.5)/3600.;
			}

			subFpBean = new SubFootprintBean(raOffset, deOffset, id);
			subFpBean.setInSphericalCoords(sphericalCoordinates);
			return subFpBean;

		}

		else if( type.equals("Circle") ) {
			ParamSet params= table.getParams();
			int nbParams = params.getItemCount();

			SavotParam ctrXOffsetParam = null;
			SavotParam ctrYOffsetParam = null;
			SavotParam radiusParam = null;
			SavotParam param;
			String utype;
			// boucle sur les params
			for( int i=0; i<nbParams; i++ ) {
				param = params.getItemAt(i);
				param = getParam(param);
				utype = param.getUtype();
//				System.out.println(utype);
				if( utype.equals("stc:AstroCoordArea/Region/reg:Sector/Center[1]") ||
						utype.equals("stc:AstroCoordArea.Circle.Center.C1") ) {
					ctrXOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Sector/Center[2]") ||
				         	utype.equals("stc:AstroCoordArea.Circle.Center.C2") ) {
					ctrYOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Circle/radius") ||
				         	utype.equals("stc:AstroCoordArea.Circle.radius") ) {
					radiusParam = param;

					continue;
				}
			}
			// pas de traitement possible dans un tel cas
			if( ctrXOffsetParam==null || ctrYOffsetParam==null || radiusParam==null ) {
				return null;
			}

			double ctrXOffset, ctrYOffset, radius;

			try {
				ctrXOffset = Double.valueOf(ctrXOffsetParam.getValue()).doubleValue();
				ctrYOffset = Double.valueOf(ctrYOffsetParam.getValue()).doubleValue();
				radius = Double.valueOf(radiusParam.getValue()).doubleValue();
			}
			catch(Exception e) {e.printStackTrace();return null;}

			// TODO : conversion selon l'unité indiqué dans les params!!
			ctrXOffset = ctrXOffset/3600.0;
			ctrYOffset = ctrYOffset/3600.0;
			radius = radius/3600.0;

			subFpBean = new SubFootprintBean(ctrXOffset, ctrYOffset, radius, id);
			subFpBean.setInSphericalCoords(sphericalCoordinates);
			return subFpBean;
		}

		else if( type.equals("Pickle") ) {
			ParamSet params= table.getParams();
			int nbParams = params.getItemCount();

			SavotParam ctrXOffsetParam = null;
			SavotParam ctrYOffsetParam = null;
			SavotParam internalRadParam = null;
			SavotParam externalRadParam = null;
			SavotParam startAngleParam = null;
			SavotParam endAngleParam = null;

			SavotParam param;
			String utype;
			// boucle sur les params
			for( int i=0; i<nbParams; i++ ) {
				param = params.getItemAt(i);
				param = getParam(param);
				utype = param.getUtype();
				if( utype.equals("stc:AstroCoordArea/Region/reg:Sector/Center[1]")) {
					ctrXOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Sector/Center[2]") ) {
					ctrYOffsetParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Sector/angle1") ) {
					startAngleParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Sector/angle2") ) {
					endAngleParam = param;

					continue;
				}
				// petite subtilité, car on a le meme utype pour internalRad et externalRad !!
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Circle/radius") && internalRadParam==null ) {
					internalRadParam = param;

					continue;
				}
				else if( utype.equals("stc:AstroCoordArea/Region/reg:Circle/radius") ) {
					externalRadParam = param;

					continue;
				}
			}
			// pas de traitement possible dans un tel cas
			if( ctrXOffsetParam==null || ctrYOffsetParam==null || startAngleParam==null || endAngleParam==null ||
				internalRadParam==null || externalRadParam==null ) {
				return null;
			}

			double ctrXOffset, ctrYOffset, startAngle, endAngle, internalRad, externalRad;

			try {
				ctrXOffset = Double.valueOf(ctrXOffsetParam.getValue()).doubleValue();
				ctrYOffset = Double.valueOf(ctrYOffsetParam.getValue()).doubleValue();
				startAngle = Double.valueOf(startAngleParam.getValue()).doubleValue();
				endAngle = Double.valueOf(endAngleParam.getValue()).doubleValue();
				internalRad = Double.valueOf(internalRadParam.getValue()).doubleValue();
				externalRad = Double.valueOf(externalRadParam.getValue()).doubleValue();
			}
			catch(Exception e) {e.printStackTrace();return null;}

			// petite subtilité pour que le rayon interne soit le plus petit
			double tmp = internalRad;
			if( internalRad>externalRad ) {
				internalRad = externalRad;
				externalRad = tmp;
			}

			// TODO : conversion selon l'unité indiqué dans les params!!
			ctrXOffset = ctrXOffset/3600.0;
			ctrYOffset = ctrYOffset/3600.0;
			double angle = endAngle-startAngle;
			internalRad = internalRad/3600.0;
			externalRad = externalRad/3600.0;

			subFpBean = new SubFootprintBean(ctrXOffset, ctrYOffset, startAngle, angle, internalRad, externalRad, id);
			subFpBean.setInSphericalCoords(sphericalCoordinates);
			return subFpBean;
		}
		else if( type.equals("String") ) {
			ParamSet params= table.getParams();
			int nbParams = params.getItemCount();

			SavotParam raParam=null;
			SavotParam decParam=null;
			SavotParam contentParam=null;
			SavotParam param;
			String utype;
			// boucle sur les params
			for( int i=0; i<nbParams; i++ ) {
				param = params.getItemAt(i);
				param = getParam(param);
				utype = param.getUtype();
				if( utype.equals("stc:AstroCoord.Position2D.Value2.C1") ) {
					raParam = param;
				}
				else if( utype.equals("stc:AstroCoord.Position2D.Value2.C2") ) {
					decParam = param;
				}
				else if( utype.equals("app:footprint.render.overlay.string.content") ) {
					contentParam = param;
				}
			}

			// pas de traitement possible dans un tel cas
			if( raParam==null || decParam==null || contentParam==null ) {
				System.err.println("something is missing");
				return null;
			}

			double ra, dec;

			try {
				ra = Double.valueOf(raParam.getValue()).doubleValue();
				dec = Double.valueOf(decParam.getValue()).doubleValue();
			}
			catch(NumberFormatException nfe) {nfe.printStackTrace();return null;}

			ra = ra/3600.0;
			dec = dec/3600.0;

			subFpBean = new SubFootprintBean(ra, dec, "center", contentParam.getValue());
			subFpBean.setInSphericalCoords(sphericalCoordinates);
			return subFpBean;
		}

		return null;

	}

	private SavotField getField(SavotField field) {
		String ref = field.getRef();
		if( ref!=null && ref.length()>0 ) return (SavotField)refMem.get(ref);
		String id = field.getId();
		if( id!=null && id.length()>0 ) {
			// register field
			refMem.put(id, field);
			return field;
		}
		return field;
	}

	private SavotParam getParam(SavotParam param) {
		String ref = param.getRef();
		if( ref!=null && ref.length()>0 ) return (SavotParam)refMem.get(ref);
		String id = param.getId();
		if( id!=null && id.length()>0 ) {
			// register field
			refMem.put(id, param);
			return param;
		}
		return param;
	}

	/**
	 * Retourne le type de région, null si non trouvé
	 * @return
	 */
	private String getRegionType(SavotTable table) {
		ParamSet params = table.getParams();
		int nbParam = params.getItemCount();
		SavotParam param;
		for( int i=0; i<nbParam; i++ ) {
			param = params.getItemAt(i);
			if( param.getName().equals("Region") ||
                param.getUtype().equalsIgnoreCase("dal:footprint.geom.segment.shape")) return param.getValue();

			if( param.getUtype().equalsIgnoreCase("app:footprint.render.overlay.string") ) return "String";
		}

		return null;
	}

	/*
	public void parse() {
		InputStream is;
		try {
			is = buildInputStream();
		}
		catch(Exception e) {
			e.printStackTrace();
			Aladin.warning("Problem during parsing of footprints !");
			return;
		}

		new SavotPullParser(is, SavotPullEngine.FULL,null,false);
	}
	*/

	static private int BUF_LENGTH = 10000;

	private InputStream buildInputStream() throws IOException {
		MyByteArrayStream is = new MyByteArrayStream();
		// écriture préambule VOTable
//      is.write("<?xml version=\"1.0\" ?>\n<VOTABLE>".getBytes());
		// on écrit d'abord le contenu du buffer qui correspond au début du stream à créer
		if( beginStream!=null ) is.write(beginStream);

		// on écrit la suite
		byte[] buf = new byte[BUF_LENGTH];
		int len;
		while( mis.available()>0 ) {
			len = mis.read(buf);
			is.write(buf,0,len);
		}

		return is.getInputStream();
	}

	private Color getColor(SavotResource res) {
		ParamSet params = res.getParams();
		int nbParams = params.getItemCount();

		SavotParam param;
		for( int i=0; i<nbParams; i++ ) {
			param = params.getItemAt(i);
			param = getParam(param);
			if( param!=null && param.getName().equalsIgnoreCase("color") ) {
				String val = param.getValue();
				int idx = Util.indexInArrayOf(val, Action.COLORNAME);
				if( idx>=0 ) return Action.MYCOLORS[idx];
				else return Action.decodeStaticRGB(val);
			}
		}

		return null;
	}

}
