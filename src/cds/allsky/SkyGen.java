package cds.allsky;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.StringTokenizer;

import cds.aladin.Aladin;
import cds.allsky.SkyGen.Action;
import cds.fits.Fits;
import cds.tools.pixtools.Util;

public class SkyGen {

	private File file;
	private Context context;

	int order = -1;
	private Action action;

	
	public SkyGen() {
		this.context = new Context();
	}

	public SkyGen(String configfile) throws Exception {
		
		this.file = new File(configfile);
		this.context = new Context();
		parseConfig();

	}
	
	private void parseConfig() throws Exception {
		
		// Extrait toutes les options du fichier
		// pour construire le contexte

		// Ouverture et lecture du fichier
		BufferedReader inputStream = null; 
		inputStream = new BufferedReader(new FileReader(file));

		String line = null;
		while ((line = inputStream.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line,"=");
			if (st.countTokens()!=2)
				continue;
			// extrait le nom de l'option
			String opt = st.nextToken().trim();
			// extrait la/les valeurs
			String val = st.nextToken().trim();
			
			try {
				setContextFromOptions(opt, val);
			} catch (Exception e) {
				e.printStackTrace();
				break;
			}

		}

		inputStream.close();
		
	}

	/**
	 * @throws Exception
	 */
	private void validateContext() throws Exception {
		// ---- Qq vérifications
		
		// arguments des répertoires de départ
		if (context.getInputPath()==null) {
//			throw new Exception("Args inputdir/outputdir sont obligatoires");
			// Par défaut le répertoire courant
			context.setInputPath(System.getProperty("user.dir"));
		}
		if (context.getOutputPath()==null)
			// Par défaut le répertoire courant
			context.setOutputPath(context.getInputPath()+/*Util.FS+*/Constante.ALLSKY);
			
		// données déjà présentes ?
		if (!context.isExistingDir()) {
			throw new Exception("Le répertoire d'entrée n'existe pas");
		}
		if (context.isExistingAllskyDir()) {
			context.warning("Le répertoire de sortie existe déjà");
			if (context.getCoAddMode()==null) {
				context.warning("Comportement par défaut pour les pixels déjà présents "+CoAddMode.getDefault());
				context.setCoAddMode(CoAddMode.getDefault());
			}
		}
		// à l'inverse, si il y a l'option "pixel" 
		// ca laisse sous entendre que l'utilisateur pensait avoir dejà des données
		else if (context.getCoAddMode()!=null) {
			context.warning("Il n'y a pas de données pré-existantes, option "+
					context.getCoAddMode()+" ignorée");
		}
		
		// si on n'a pas d'image etalon, on la cherche + initialise avec
		if (context.getImgEtalon()==null) {
			boolean found = context.findImgEtalon(context.getInputPath());
			if( !found ) {
				String msg = "There is no available images in source directory !\n"+context.getInputPath();
				context.warning(msg);
				throw new Exception(msg);
			}

			Fits file = new Fits();
			try { file.loadHeaderFITS(context.getImgEtalon()); } 
			catch( Exception e ) { e.printStackTrace(); }
			// calcule le meilleur nside
			long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
			order = ((int) Util.order((int)nside) - Constante.ORDER);
		}
		
		// si le numéro d'order donné est différent de celui calculé
		// attenion n'utilise pas la méthode getOrder car elle a un default à 3
		if (order != context.order && -1 != context.order ) {
			context.warning("Order lu (" + context.getOrder() +") != auto (" + order + ")");
		}
		// si le bitpix donné est différent de celui calculé
		if (context.getBitpix() != context.getBitpixOrig() ) {
			context.warning("Bitpix lu (" + context.getBitpix() +") != auto (" + context.getBitpixOrig() + ")");
		}
	}

	/**
	 * @param order
	 * @param opt
	 * @param val
	 * @return
	 * @throws Exception 
	 */
	private void setContextFromOptions(String opt, String val) throws Exception {
		//System.out.println(opt +" === " +val);
		if (opt.equalsIgnoreCase("input"))
			context.setInputPath(val);
		else if (opt.equalsIgnoreCase("output"))
			context.setOutputPath(val);
		else if (opt.equalsIgnoreCase("regex"))
			context.setRegex(val);
		else if (opt.equalsIgnoreCase("order"))
			context.setOrder(Integer.parseInt(val));
		else if (opt.equalsIgnoreCase("pixel"))
			context.setCoAddMode(CoAddMode.valueOf(val.toUpperCase()));
		else if (opt.equalsIgnoreCase("bitpix"))
			context.setBitpix(Integer.parseInt(val));
		else if (opt.equalsIgnoreCase("region"))
			context.setRegion(val);
		else if (opt.equalsIgnoreCase("frame"))
			context.setFrameName(val);
		else if (opt.equalsIgnoreCase("skyval"))
			context.setSkyval(val);
		else if (opt.equalsIgnoreCase("borders"))
			try {
				context.setBorderSize(val);
			} catch (ParseException e) {System.err.println(e.getMessage());}
		else if (opt.equalsIgnoreCase("pixelCut"))
			context.setCut(val);
		else if (opt.equalsIgnoreCase("dataCut")) {
			context.setCutData(val);
		}
		else if (opt.equalsIgnoreCase("color"))
			context.setColor(Boolean.parseBoolean(val));
		else if (opt.equalsIgnoreCase("fading"))
			context.setFading(Boolean.parseBoolean(val));
		else if (opt.equalsIgnoreCase("img")) {
			context.setImgEtalon(val);
		}

	}

	enum Action {FINDER,TILES,JPEG,MOC}
	
	public static void main(String[] args) {
		Aladin a = new Aladin();
		SkyGen generator = new SkyGen();
		int length = args.length;
		if (length==0) {
			usage();
			return;
		}
		// extrait les options en ligne de commande, et les analyse
		for (String arg : args) {
			System.out.println(arg+" ");
			// si c'est dans un fichier 
			String param = "-param=";
			if (arg.startsWith(param)) {
				generator.setConfigFile(arg.substring(param.length()));
				continue;
			}
			param = "-action=";
			if (arg.startsWith(param)) {
				generator.action = Action.valueOf((arg.substring(param.length())).toUpperCase());
				continue;
			}
			// toutes les autres options écrasent les précédentes
			if (arg.contains("=")) {
				String[] opts = arg.split("=");
				try {
					// si il y a un - on l'enlève
					opts[0] = opts[0].substring(opts[0].indexOf('-')+1);
					
					generator.setContextFromOptions(opts[0], opts[1]);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			
		}

		try {
			generator.validateContext();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		// lance les calculs
		generator.start();
	}

	private void start() {
		if (action==null) {
			// aucun action définis -> on fait la totale
			action = Action.FINDER;
			start();
			action = Action.TILES;
			start();
			action = Action.JPEG;
			start();
			action = Action.MOC;
			start();
			return;
		}
		// TODO 
		switch (action) {
		case FINDER : System.out.println("lancement du HpxFinder"); break;
		case JPEG : System.out.println("lancement des jpg"); break;
		case MOC : System.out.println("lancement du MOC"); break;
		case TILES : System.out.println("lancement du HpxBuilder"); break;
		}
	}

	private static void usage() {
		System.out.println("SkyGen -param=configfile");
		
	}

	private void setConfigFile(String configfile) {
		this.file = new File(configfile);
	}
}
