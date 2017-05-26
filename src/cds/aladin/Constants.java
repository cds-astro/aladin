package cds.aladin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {
	
public static final List<String> SODAHINTPARAMS = new ArrayList<String>();
public static final List<String> SODAINPUTPARAMS_POS = new ArrayList<String>();
public static final List<String> SODAINPUTPARAMS_BAND = new ArrayList<String>();
public static final List<String> SODAINPUTPARAMS_TIME = new ArrayList<String>();

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
public static final String EM_MIN = "em_min";
public static final String EM_MAX = "em_max";
public static final String SETFORMVALUES = "SETFORMVALUES";
public static final String SODAINPUTPARAMS_POL = "pol_states";
public static final String SODA_STANDARDID = "ivo://ivoa.net/std/SODA#sync-1.0";
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
public static final String SODA_URL_PARAM = "?POS=circle+$1+$2+$3&TIME=$4&BAND=$5&POL=$6*&ID=$7";
public static final String SODA_CIRCLE_URL_PARAM = "?POS=circle+$1+$2+$3&TIME=$4&BAND=$5&POL=$6*&ID=$7";
public static final String SODA_POLY_URL_PARAM = "?POS=polygon+$1&TIME=$2&BAND=$3&POL=$4*&ID=$5";
public static final String DATALINK_FORM = "Datalink_Form";
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

public static final Map<String, String> DATE_FORMATS = new HashMap<String, String>();
//wont allow .0
//if allowing exponent:: REGEX_NUMBER = "^[-+]?\\d+(\\.\\d+)?([eE][-+]?\\d+)?$";
public static final String REGEX_NUMBER = "[-+]?\\d+(\\.\\d+)?";
public static final String REGEX_NUMBERNOEXP = "^"+REGEX_NUMBER+"$";// = "^[-+]?[0-9]\\d*(\\.\\d+)?$";//^\\d{1,5}$"; //\\s?-?[0-9]*\\s?";
public static final String SERVICE_DEF = "service_def";
public static final String DESCRIPTION = "description";
public static final String SEMANTICS = "semantics";
public static final String CONTENTTYPE = "content_type";
public static final String CONTENTLENGTH = "content_length";
public static final String ACCESSURL = "access_url";
public static final String CONTENT_TYPE_TEXTXML = "text/xml";
public static final String CONTENT_TYPE_TEXTHTML = "text/html";
public static final String CONTENT_TYPE_APPLICATIONIMG = "application/image;content=hips";
public static final String UNKNOWN = "UNKNOWN";
public static final String SPACESTRING = " ";
public static final String S_REGION = "s_region";
public static final String DATATYPE_DATALINK = "content=datalink";//"application/x-votable+xml;content=datalink"; we'l just check for content=datalink
public static final String ACCESSFORMAT_UCD = "obscore:Access.Format";
public static final String SEMANTIC_CUTOUT = "#cutout";
public static final String SEMANTIC_ACCESS = "#access";
public static final String SEMANTIC_PROC = "#proc";
public static final String REGEX_TABLENAME_SPECIALCHAR = "[$&+,:;=?@#/\\\\|]";
public static final String REGEX_ONLYALPHANUM = "[^A-Za-z0-9]";
public static final String REGEX_ALPHA = "[A-Za-z]";
/** REGEX_OPNUM: compare operator(1 or 0) +or-(1 or 0) decimal or whole number(exactly 1): 
-valid matches: >=233, <34, !=-65, >=-434.05
-invalid matches: =>64, >>344, asf, >=-434.05asd, >=-434.05 434.05
*/
public static final String REGEX_OPNUM = "^\\s*(?<operator>[><=]|>=|<=|!=)?(?<value>[-+]?[0-9]\\d*(\\.\\d+)?){1}$";
/** REGEX_RANGEINPUT: min(exactly1) ..(exactly1) max(exactly1): 
-valid matches: 2..34, -523232323..+23423423424
-invalid matches: 2.., ..35345
the delimiter between the 2 values can be .. or , or and/AND
*/
public static final String REGEX_RANGEINPUT = "(?<range1>"+REGEX_NUMBER+"){1}\\s*(\\.\\.|,|and|AND|\\s+)\\s*(?<range2>"+REGEX_NUMBER+"){1}";
public static final String ADQLVALUE_FORBETWEEN = "BETWEEN ${range1} AND ${range2}";
public static final String REGEX_TIME_RANGEINPUT = "\\s*(?<delimiter>,|\\.\\.|\\s+\\band\\b\\s+|\\s+\\bAND\\b\\s+)\\s*";
public static final String REGEX_BAND_RANGEINPUT = "\\s*(?<delimiter>,|\\.\\.|\\s+\\band\\b\\s+|\\s+\\bAND\\b\\s+|\\s+)\\s*";

public static final String DATALINK_CUTOUT_FORMLABEL = "Cutout";
public static final String SODA_SYNC_FORM = "SODA_SYNC_FORM";
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
public static final String SYNCGETRESULT = "%1$s/sync?REQUEST=doQuery&LANG=ADQL&QUERY=%2$s"; //As per TAP spec
//public static final String UCD_RA_PATTERN1 = "pos.eq.ra;meta.main";//wont explicitely check for this. pos.eq.ra is allowed for now.
public static final String UCD_RA_PATTERN2 = "pos.eq.ra";
public static final String UCD_RA_PATTERN3 = "POS_EQ_RA_MAIN";
//public static final String UCD_DEC_PATTERN1 = "pos.eq.dec;meta.main";
public static final String UCD_DEC_PATTERN2 = "pos.eq.dec";
public static final String UCD_DEC_PATTERN3 = "POS_EQ_DEC_MAIN";
public static final String[] CIRCLEORSQUARE = { "CIRCLE"/*, "SQUARE"*/ };//TODO:: cirlce or sqaure dev. add sq when...
public static final String[] SYNC_ASYNC = { "SYNC","ASYNC" };
public static final Integer[] TAP_REC_LIMIT = { 100,200,500,1000,2000 };
public static final String AND = "AND";
public static final String TAPv1 = "TAPv1";
public static final String V1 = "V1";
public static final String COUNT = "COUNT";
public static final String TABLETYPE = "table_type";
public static final String SCHEMANAME = "schema_name";
public static final String LASTPANEL = "LASTPANEL";
public static final String GENERAL = "GENERAL";
public static final String DISCARDACTION = "DISCARD";
public static final String DISCARDALLACTION = "DISCARDALL";
public static final String RETRYACTION = "RETRYACTION";
public static final String STANDARD_TAPRESULTFORMAT = "votable";// "application/x-votable+xml";//TODO:: tintin

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

public static final int TAPFORM_STATUS_NOTCREATEDGUI = 1;//have metadata..not created gui
public static final int TAPFORM_STATUS_NOTLOADED = 0;//no metadata
public static final int TAPFORM_STATUS_LOADED = 2;//metadata and gui in place
public static final int TAPFORM_STATUS_ERROR = -1;//error

public static enum TapServerMode {//generic client modes
	UPLOAD;
}

//modes correspond to the frame that host these servers
public static enum TapClientMode {//Dialog is serverselector
	DIALOG, TREEPANEL;
}
public static final String GLU = "GLU";
public static final String GENERIC = "GENERIC";
public static final String[] TAPSERVERMODES = { GENERIC , GLU };
public static final String TAPMODECHANGETOOLTIP = "TAPMODECHANGETOOLTIP";
public static final String LOADCLIENTTAPURL = "LOADCLIENTTAPURL";
public static final String RELOAD = "RELOAD";

//some votable tag names
public static final String STR_RESULTS = "RESULTS";
public static final String STR_QUERY_STATUS = "QUERY_STATUS";

}
