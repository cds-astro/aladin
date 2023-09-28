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

package cds.aladin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
	
public static final List<String> SODAHINTPARAMS = new ArrayList<>();
public static final List<String> SODAINPUTPARAMS_POS = new ArrayList<>();
public static final List<String> SODAINPUTPARAMS_BAND = new ArrayList<>();
public static final List<String> SODAINPUTPARAMS_TIME = new ArrayList<>();

public static final String ID = "ID";
public static final String TARGET = "Target";
public static final String RA_STRING = "Right Ascension";
public static final String DEC_STRING	 = "Declination";
public static final String DIMENSIONS = "Dimensions";
public static final String TIME = "Time";
public static final String DateTimeMJD = "Date(ParseToMJD)";
public static final String BandSODAForm = "Band(SODA)";

public static final String BAND = "Band";
public static final String POL = "Pol";
public static final String SODAPOL_DATATYPE = "char(Multiselect)";
public static final String POL_STATES = "pol_states";

public static final String EMPTYSTRING = "";
public static final String STANDARDID = "standardID";
public static final String T_MIN = "t_min";
public static final String T_MAX = "t_max";
public static final String T_XEL = "t_xel";
public static final String EM_MIN = "em_min";
public static final String EM_MAX = "em_max";
public static final String SETFORMVALUES = "SETFORMVALUES";
public static final String SODAINPUTPARAMS_POL = "pol_states";
public static final String SODA_STANDARDID = "ivo://ivoa.net/std/SODA#sync-1.0";
public static final String SODAASYNC_STANDARDID = "ivo://ivoa.net/std/SODA#async-1.0";
public static final int SODA_IDINDEX = 7;
public static final int SODA_POSINDEX1 = 1;
public static final int SODA_POSINDEX2 = 2;
public static final int SODA_POSINDEX3 = 3;
public static final int SODA_TIMEINDEX = 4;
public static final int SODA_BANDINDEX = 5;
public static final int SODA_POLINDEX = 6;
public static final String RESULTS_RESOURCE_NAME = "results";
public static final String INPUTPARAM_NAME = "inputParams";
public static final String RANGE_DELIMITER = " > ";
//DALI specs says shape names are casesensitive.. an they are accepted as all caps
//so poS = "CIRCLE.. is ok and pOS = "Circle is not
public static final String SODA_URL_PARAM = "?POS=CIRCLE+$1+$2+$3&TIME=$4&BAND=$5&POL=$6*&ID=$7";
public static final String SODA_CIRCLE_URL_PARAM = "?POS=CIRCLE+$1+$2+$3&TIME=$4&BAND=$5&POL=$6*&ID=$7";
public static final String SODA_POLY_URL_PARAM = "?POS=POLYGON+$1&TIME=$2&BAND=$3&POL=$4*&ID=$5";
public static final String DATALINK_FORM = "Datalink_Form";
public static final String DATALINK_FORM_SYNC = "Datalink_Form_sync";
public static final String DATALINK_FORM_ASYNC = "Datalink_Form_async";
public static final String DATALINK_SODACIRCLE_FORM = "Datalink_SodaCircle_Form";
public static final String DATALINK_SODAPOLY_FORM = "Datalink_SodaPoly_Form";
public static final String DOT_CHAR = ".";
public static final String QUESTIONMARK_CHAR = "?"; 
public static final String AMPERSAND_CHAR = "&";
public static final String DOLLAR_CHAR = "$";
public static final String EQUALS_CHAR = "=";
public static final String COMMA_CHAR = ",";
public static final String COMMA_SPACECHAR = ", ";
public static final String DOTREGEX = "\\.";
public static final String UTF8 = "utf-8";
public static final String PLUS_CHAR = "+";
public static final String LIMIT_UNKNOWN_MESSAGE = "Field limit unknown...";
public static final String POL_DEFAULT_VALUES = "I\tQ\tU\tV\tRR\tLL\tRL\tLR\tXX\tYY\tXY\tYX\tPOLI\tPOLA";
public static final int NUMBEROFOPTIONS = 14;
public static final String REGEX_ARRAY_PRINT = "[\\[\\]null(,$)]";
public static final String NEWLINE_CHAR = "\n";

public static final Map<String, String> DATE_FORMATS = new HashMap<>();
//wont allow .0
//if allowing exponent:: REGEX_NUMBER = "^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$";
public static final String REGEX_NUMBER = "[-+]?\\d+(\\.\\d+)?";//integer plus decimal
public static final String REGEX_DIGITS = "^\\d*[1-9]\\d*$";
public static final String REGEX_NUMBERNOEXP = "^"+REGEX_NUMBER+"$";// = "^[-+]?[0-9]\\d*(\\.\\d+)?$";//^\\d{1,5}$"; //\\s?-?[0-9]*\\s?";
public static final String SERVICE_DEF = "service_def";
public static final String DESCRIPTION = "description";
public static final String SEMANTICS = "semantics";
public static final String CONTENTTYPE = "content_type";
public static final String CONTENTQUAL = "content_qualifier";
public static final String CONTENTLENGTH = "content_length";
public static final String CONTENTLENGTH_DISPLAY = "content_length_display";
public static final String DEFAULT_CONTENTLENGTH_UNITS = "bytes";
public static final String ACCESSURL = "access_url";
public static final String CONTENT_TYPE_TEXTXML = "text/xml";
public static final String CONTENT_TYPE_TEXTHTML = "text/html";
public static final String CONTENT_TYPE_TEXTPLAIN = "text/plain";
public static final String CONTENT_TYPE_PDF = "application/pdf";
public static final String CONTENT_TYPE_HIPS = "content=hips";
public static final String CONTENT_TYPE_VOTABLE = "application/x-votable+xml";
public static final String UNKNOWN = "UNKNOWN";
public static final String SPACESTRING = " ";
public static final String S_REGION = "s_region";
//public static final String DATATYPE_DATALINK = "content=datalink";//"application/x-votable+xml;content=datalink"; we'l just check for content=datalink
public static final String SEMANTIC_CUTOUT = "#cutout";
public static final String SEMANTIC_ACCESS = "#access";
public static final String SEMANTIC_PROC = "#proc";
public static final String SEMANTIC_PREVIEW = "#preview";
public static final String SEMANTIC_PREVIEWPLOT = "#preview-plot";
public static final String REGEX_TABLENAME_SPECIALCHAR = "[$&+,:;=?@#/\\\\|]";
public static final String REGEX_VALIDTABLENAMECONSTRUCT = "(?<tableName>[\\p{L}_][\\p{L}\\p{N}@$#_]{0,127})";
public static final String REGEX_VALIDTABLEPREFIX = "^(?<prefix>"+REGEX_VALIDTABLENAMECONSTRUCT+"?\\.)";
public static final String REGEX_VALIDTABLENAME = "^"+REGEX_VALIDTABLENAMECONSTRUCT+"$";//msdn site
public static final String REGEX_TABLENAMEALREADYQUOTED = "^\"[^\"]+\"$";

public static final String REGEX_ONLYALPHANUM = "[^A-Za-z0-9]";
public static final String REGEX_ALPHA = "[A-Za-z]";

public static final String REGEX_OPTIONNALEXPONENT = "([eE]{1}[-+]?\\d+)?";
public static final String REGEX_RANGEDELIMITERS = "\\.\\.|,|and|AND|\\s+";

/** REGEX_OPNUM: compare operator(1 or 0) +or-(1 or 0) decimal or whole number(exactly 1): 
-valid matches: >=233, <34, !=-65, >=-434.05
-invalid matches: =>64, >>344, asf, >=-434.05asd, >=-434.05 434.05
*/
//public static final String REGEX_OPNUM = "^\\s*((?<operator>[><=]|>=|<=|!=)\\s*)?(?<value>[-+]?[0-9]\\d*(\\.\\d+)?){1}$";
//public static final String REGEX_OPNUM = "^\\s*((?<operator>[><=]|>=|<=|!=)\\s*)?(?<value>[-+]?[0-9]\\d*(\\.\\d+)?){1}$";
public static final String REGEX_OP = "^\\s*((?<operator>>=|<=|!=|[><=])\\s*)";
public static final String REGEX_OPNUM = REGEX_OP+"?(?<value>"+REGEX_NUMBER+REGEX_OPTIONNALEXPONENT+"){1}$";
public static final String REGEX_OPANYVAL = REGEX_OP+"?(?<value>.*)$";//get valid operator , followed by 'any' value



//integer plus decimal plsu exponent
public static final String REGEX_NUMBERWITHEXP = "\\A\\s*("+REGEX_NUMBER+REGEX_OPTIONNALEXPONENT+"){1}\\s*\\z";//matches one number
/** REGEX_RANGEINPUT: min(exactly1) ..(exactly1) max(exactly1): 
-valid matches: 2..34, -523232323..+23423423424
-invalid matches: 2.., ..35345
the delimiter between the 2 values can be .. or , or and/AND
*/
public static final String REGEX_RANGENUMBERINPUT = "^(?<range1>" + REGEX_NUMBER + REGEX_OPTIONNALEXPONENT
		+ "){1}\\s*(?<operator>" + REGEX_RANGEDELIMITERS + ")\\s*(?<range2>" + REGEX_NUMBER
		+ REGEX_OPTIONNALEXPONENT + "){1}\\s*$";

public static final String REGEX_RANGEINPUT = "^(?<range1>.*){1}\\s*(?<operator>" + REGEX_RANGEDELIMITERS + "){1}\\s*(?<range2>.*{1}){1}\\s*$";

//public static final String REGEX_RANGEINPUT = "^(?<range1>"+REGEX_NUMBER+"){1}\\s*((?<operator>\\.\\.|,|and|AND|\\s+)\\s*(?<range2>"+REGEX_NUMBER+")_{1}";
//public static final String REGEX_RANGEINPUT = "^(?<range1>"+REGEX_NUMBER+"){1}\\s*(?<operator>\\.\\.|,|and|AND|\\s+)\\s*(?<range2>"+REGEX_NUMBER+"){1}\\s*$";

/** REGEX_OPNUM: ^\\s* - 0 or more space
 * ((?<operator>=|!=|LIKE|NOT LIKE|IS NOT|IS)\\s+{0,1}) compare operator for string (1 or 0 times followed mandatorily by space)
 * (?<value>[\\w\\s]+)$ - ending one or more words
-valid matches: like liek, hubbel232
-invalid matches: like, like3 like
*/
public static final String REGEX_OPALPHANUMOLD = "^\\s*((?<operator>=|!=|LIKE|NOT LIKE|IS NOT|IS)\\s+)?(?<value>[\\w\\s]+)$";
public static final String REGEX_OPALPHANUM = "^\\s*((?<operator>=|!=|LIKE|NOT LIKE|IS NOT|IS)\\s+)?(?<value>(.*)(?!\\s))$";

public static final String PROCESSED_REGEX_OPALPHANUM = "\'${value}\'";
public static final String RANGE_DEFAULT_SPACEDDISPLAY = "${range1} ${range2}";
public static final String ADQLVALUE_FORBETWEEN = "BETWEEN ${range1} AND ${range2}";
public static final String REGEX_TIME_RANGEINPUT = "\\s*(?<delimiter>,|\\.\\.|\\s+\\band\\b\\s+|\\s+\\bAND\\b\\s+)\\s*";
public static final String REGEX_BAND_RANGEINPUT = "\\s*(?<delimiter>,|\\.\\.|\\s+\\band\\b\\s+|\\s+\\bAND\\b\\s+|\\s+)\\s*";

public static final String DATALINK_CUTOUT_FORMLABEL = "Cutout";
public static final String SODA_FORM = "SODA_FORM";
public static final String TAP_MAIN_FORM ="TAP_MAIN_FORM";
public static final String STANDARDGLUs = "StandardForms.dic";
public static final String TAPSERVERS = "TapServices.txt";
public static final String STDFORM_IDENTIFIER = "Aladin.StdForm";
public static final String INFOGUI = "INFOGUI";

//tap client related constants
public static final String TAP = "TAP";
public static final String TABLENAME = "table_name";
public static final String COLUMNNAME = "column_name";
public static final String UNIT = "unit";
public static final String UCD = "ucd";
public static final String UTYPE = "utype";
public static final String DATATYPE = "datatype";
public static final String SIZE = "size";
public static final String PRINCIPAL = "principal";
public static final String INDEXED = "indexed";
public static final String STD = "std";
public static final String REMOVEWHERECONSTRAINT = "REMOVEWHERECONSTRAINT";
public static final String CHANGESERVER = "CHANGESERVER";
public static final String WRITEQUERY = "WRITEQUERY";
public static final String ADDWHERECONSTRAINT = "ADDWHERECONSTRAINT";
public static final String TABLECHANGED = "TABLECHANGED";
public static final String ADDPOSCONSTRAINT = "ADDPOSCONSTRAINT";
public static final String POSCONSTRAINTSHAPECHANGED = "POSCONSTRAINTSHAPECHANGED";
public static final String CHECKQUERY = "CHECKQUERY";
public static final String UPLOAD = "UPLOAD";
public static final String SELECTALL = "SELECTALL";
public static final String OPEN_SET_RADEC = "OPEN_SET_RADEC";
public static final String JOIN_TABLE = "JOIN_TABLE";
public static final String GETRESULTPARAMS = "REQUEST=doQuery&LANG=ADQL&MAXREC="+TapManager.MAXTAPROWS+"&QUERY="; //As per TAP spec
//public static final String SYNCGETRESULT = "%1$s/sync?REQUEST=doQuery&LANG=ADQL&QUERY=%2$s";
public static final String POSQuery = "CONTAINS(POINT('ICRS', %1$s%2$s, %1$s%3$s), CIRCLE('ICRS', %4$s, %5$s, %6$s)) = 1";
//public static final String UCD_RA_PATTERN1 = "pos.eq.ra;meta.main";//wont explicitely check for this. pos.eq.ra is allowed for now.
public static final String UCD_RA_PATTERN2 = "pos.eq.ra";
public static final String UCD_RA_PATTERN3 = "pos_eq_ra_main";//"POS_EQ_RA_MAIN";
//public static final String UCD_DEC_PATTERN1 = "pos.eq.dec;meta.main";
public static final String UCD_DEC_PATTERN2 = "pos.eq.dec";
public static final String UCD_DEC_PATTERN3 = "pos_eq_dec_main";//"POS_EQ_DEC_MAIN";
public static final String[] CIRCLEORSQUARE = { "CIRCLE"/*, "SQUARE"*/ };//TODO:: cirlce or sqaure dev. add sq when...
public static final String[] SYNC_ASYNC = { "SYNC","ASYNC" };
public static final String TAP_REC_LIMIT_UNLIMITED = "unlimited";
public static final String[] TAP_REC_LIMIT = { "10","100","200","500","1000","2000", "9999","99999","999999", TAP_REC_LIMIT_UNLIMITED};
public static final String AND = "AND";
public static final String TAPv1 = "TAPv1";
public static final String V1 = "V1";
public static final String COUNT = "COUNT";
public static final String TABLETYPE = "table_type";
public static final String SCHEMANAME = "schema_name";
public static final String LASTPANEL = "LASTPANEL";
public static final String GENERAL = "GENERAL";
public static final String RETRYACTION = "RETRYACTION";
public static final String SUBMITACTION = "SUBMIT";
public static final String RESETACTION = "RESET";
public static final String CLEARACTION = "CLEAR";
public static final String EDITUPLOADTABLENAMEACTION = "EDITUPLOADTABLENAMEACTION";
public static final String STANDARD_TAPRESULTFORMAT = "votable";// "application/x-votable+xml";//TODO:: either
public static final String TABLEGUINAME = "table";

/** Liste des caractère définissant une chaine de carac @author Mallory Marcot*/
public static final String LISTE_CARACTERE_STRING = "AZRTYUIOPQSDFGHJKLMWXCVBN%_[]^";

	static {
		SODAHINTPARAMS.add(STANDARDID);
		SODAINPUTPARAMS_POS.add("s_ra");
		SODAINPUTPARAMS_POS.add("s_dec");
		SODAHINTPARAMS.addAll(SODAINPUTPARAMS_POS);
		
		SODAINPUTPARAMS_TIME.add(T_MIN);
		SODAINPUTPARAMS_TIME.add(T_MAX);
		SODAHINTPARAMS.addAll(SODAINPUTPARAMS_TIME);
		
		SODAINPUTPARAMS_BAND.add(EM_MIN);
		SODAINPUTPARAMS_BAND.add(EM_MAX);
		SODAHINTPARAMS.addAll(SODAINPUTPARAMS_BAND);
		
		SODAHINTPARAMS.add(SODAINPUTPARAMS_POL);
		
		DATE_FORMATS.put("^\\d{1,2}-\\d{1,2}-\\d{4}", "dd-MM-yyyy");
		DATE_FORMATS.put("^\\d{1,2}-[a-zA-Z]{3}-\\d{4}", "dd-MMM-yyyy");
		DATE_FORMATS.put("^\\d{4}-\\d{1,2}-\\d{1,2}", "yyyy-MM-dd");
		DATE_FORMATS.put("^\\d{4}-[a-zA-Z]{3}-\\d{1,2}", "yyyy-MMM-dd");
		
	}
	
//For tap glu form
public static final String TAPPROTOCOL = "Aladin.TAP";
public static final String GLU_DESCRIPTION = "Description";
public static final String GLU_VALUE = "Value";
public static final String GLU_TYPE = "Type";
public static final String GLU_HINT = "VerboseDescription";
public static final String GLU_ADQL = "ADQL";
public static final String GLU_SELECT = "Select";
public static final String GLU_FROM = "From";
public static final String GLU_WHERE = "Where";
public static final String GLU_TABLES = "Tables";
public static final String GLU_ALLORDISTINCT = "ALLORDISTINCT";
public static final String GLU_ADQL_TEMPLATE1 = "SELECT %1$s %2$s %3$s FROM %4$s %5$s";
public static final String TOP = "TOP";
public static final String COLUMNS = "COLUMNS";
public static final String FROM = "FROM";
public static final String SELECT = "SELECT";
public static final String ALL = "ALL";
public static final String DISTINCT = "DISTINCT";
public static final String WHERE = "WHERE";
public static final String TARGETNAN = "--   --";

public static final String REGISTRYPANEL = "REGISTRYPANEL";
public static final String SUBMITPANEL = "SubmitPanel";
public static final String UPLOADTABLEPREFIX = "TAP_UPLOAD.";
public static final String ALADINTABLEPREFIX = "TAP_UPLOAD.AladinTable";
public static final String SHOWAYNCJOBS = "SHOWAYNCJOBS";
public static final String DELETEONEXIT = "DELETEONEXIT";
public static final String GETPREVIOUSSESSIONJOB = "GETPREVIOUSSESSIONJOB";
public static final String LOADJOBRESULT = "LOADJOBRESULT";
public static final String LOADDEFAULTTAPRESULT = "LOADDEFAULTTAPRESULT";
public static final String DELETEJOB = "DELETEJOB";
public static final String ABORTJOB = "ABORTJOB";
public static final String OLDJOBSELECTED = "OLDJOBSELECTED";
/*public static final String RUNJOB = "RUNJOB";*/

public static final int TAPFORM_STATUS_NOTLOADED = 0;//have metadata..not created gui
public static final int TAPFORM_STATUS_LOADING = 1;//metadata and gui in place
public static final int TAPFORM_STATUS_LOADED = 2;//metadata and gui in place
public static final int TAPFORM_STATUS_ERROR = -1;//error

//modes correspond to the frame that host these servers
public static enum TapClientMode {//Dialog is serverselector
	DIALOG, TREEPANEL, @Deprecated/**
	 * Upload mode is not used anymore. All tables synced to Aladin stack
	 */
	UPLOAD, STANDALONE;
}
public static final String GLU = "Basic";
public static final String NODE = "NODE";
public static final String GENERIC = "Generic";
public static final String TEMPLATES = "Templates";
public static final String OBSCORE = "Obscore";
public static final String ACCESSALL = "Access all";

public static final String TAPMODECHANGETOOLTIP = "TAPMODECHANGETOOLTIP";
public static final String LOADCLIENTTAPURL = "LOADCLIENTTAPURL";
public static final String RELOAD = "RELOAD";

//some votable tag names
public static final String STR_RESULTS = "RESULTS";
public static final String STR_QUERY_STATUS = "QUERY_STATUS";

public static final String STCPREFIX_POLYGON = "POLYGON ICRS ";
public static final String STCPREFIX_CIRCLE = "CIRCLE ICRS ";

public static final String RADECBUTTON = "RADECBUTTON";

public static final String CHANGESETTINGS = "CHANGESETTINGS";

//Actions obstap client
public static final String ADD_DATAPRODUCTTYPE = "ADD_DATAPRODUCTTYPE";
public static final String ADD_SPATIALCONSTRAINT = "ADD_SPATIALCONSTRAINT";
public static final String ADD_TIMECONSTRAINT = "ADD_TIMECONSTRAINT";
public static final String ADD_SPECTRALCONSTRAINT = "ADD_SPECTRALCONSTRAINT";
public static final String ADD_FREECONSTRAINT = "ADD_FREECONSTRAINT";
public static final String TAP_EMPTYINPUT = "ALATAP_EMPTYINPUT";

public static final String UCD_MAINIDQUALIFIER = "meta.main";

//flagged columns
public static final String RA = "ra", DEC = "dec";
public static final String PARALLAX = "parallax", RADIALVELOCITY = "radialvelocity";
public static final String BIBCODE = "bibCode", JOURNAL = "journal", DOCTITLE = "title";
public static final String PMRA = "pmra", PMDEC = "pmdec";
public static final String REDSHIFT = "redshift", MAG = "mag";
public static final String MAINID = "id", SRCCLASS = "srcclass";

//obscore columns
public static final String DATAPRODUCT_TYPE = "dataproduct_type", OBSID = "obs_id";
public static final String BIB_REFERENCE = "bib_reference", ACCESS_FORMAT = "access_format", ACCESS_ESTSIZE = "access_estsize";

//tap schema ids strings for locating schema tables
//hate using regex as performance is abysmal for simple string searches comapred to String.contains
//looking for tables or just keys might exclude some imp tables?
public static final String REGEX_TAPSCHEMATABLES = "schemas|TAP_SCHEMA.tables|TAP_SCHEMA.keys|key_columns|columns";
public static final String PATHSYNC = "sync";
public static final String PATHASYNC = "async";
public static final String PATHPHASE = "phase";
public static final String PATHRESULTS = "/results/result";

public static final String DIRQUERY_GETALLTAPSERVERS = "*";

//join
public static final String SERVERJOINTABLESELECTED = "SERVERJOINTABLESELECTED";
public static final String UPLOADJOINTABLESELECTED = "UPLOADJOINTABLESELECTED";
//public static final String KEY_ID = "key_id";
public static final String FROM_TABLE = "from_table";
public static final String TARGET_TABLE = "target_table";
public static final String FROM_COLUMN = "from_column";
public static final String TARGET_COLUMN = "target_column";

public static enum DBColumnType {
	SMALLINT, INTEGER, BIGINT, REAL, DOUBLE, VARBINARYnum, 
	CHAR, VARCHAR, VARCHARn, CHARn, VARBINARY, VARBINARYn, 
	BINARYn, BLOB, CLOB, TIMESTAMP, POINT, REGION;
}

public static final String TABLESLABEL = "TABLESLABEL";

}
