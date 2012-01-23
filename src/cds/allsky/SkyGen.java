package cds.allsky;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.ParseException;
import java.util.StringTokenizer;

import cds.aladin.PlanImage;
import cds.fits.Fits;
import cds.moc.HealpixMoc;
import cds.tools.pixtools.Util;

public class SkyGen {

	private File file;
	private Context context;

	int order = -1;
	private Action action;

	public SkyGen() {
		this.context = new Context();
	}


	/**
	 * Analyse le fichier contenant les paramètres de config de la construction
	 * du allsky sous le format : option = valeur
	 * 
	 * @throws Exception
	 *             si l'erreur dans le parsing des options nécessite une
	 *             interrption du programme
	 */
	private void parseConfig() throws Exception {

		// Extrait toutes les options du fichier
		// pour construire le contexte

		// Ouverture et lecture du fichier
		BufferedReader inputStream = null;
		inputStream = new BufferedReader(new FileReader(file));

		String line = null;
		while ((line = inputStream.readLine()) != null) {
			StringTokenizer st = new StringTokenizer(line, "=");
			if (st.countTokens() != 2) {
				System.err.println("Error reading line : "+line);
				continue;
			}
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
	 * Lance quelques vérifications de cohérence entre les options données
	 * 
	 * @throws Exception
	 *             si une incohérence des options nécessite une interrption du
	 *             programme
	 */
	private void validateContext() throws Exception {
		// ---- Qq vérifications

		// arguments des répertoires de départ
		if (context.getInputPath() == null) {
			throw new Exception("Argument inputdir is missing");
			// Par défaut le répertoire courant
			//context.setInputPath(System.getProperty("user.dir"));
		}
		if (context.getOutputPath() == null)
			// Par défaut le répertoire courant
			context.setOutputPath(context.getInputPath()
					+ /* Util.FS+ */Constante.ALLSKY);

		// données déjà présentes ?
		if (!context.isExistingDir()) {
			throw new Exception("Input dir does NOT exists : "
					+ context.getInputPath());
		}
		if (context.isExistingAllskyDir()) {
			context.warning("Output dir already exists");
			if (context.getCoAddMode() == null) {
				context.warning("Default behaviour for computing pixels already computed : "
						+ CoAddMode.getDefault());
				context.setCoAddMode(CoAddMode.getDefault());
			}
		}
		// à l'inverse, si il y a l'option "pixel"
		// ca laisse sous entendre que l'utilisateur pensait avoir dejà des
		// données
		else if (context.getCoAddMode() != null) {
			context.warning("There is NO already computed tiles, so option "
					+ context.getCoAddMode() + " ignored");
		}

		// si on n'a pas d'image etalon, on la cherche + initialise avec
		if ( (action!=Action.JPEG&&action!=Action.MOC&&action!=Action.ALLSKY) 
				&& (context.getImgEtalon()==null) ) {
			boolean found = context.findImgEtalon(context.getInputPath());
			if (!found) {
				String msg = "There is no available images in source directory : "
						+ context.getInputPath();
				context.warning(msg);
				throw new Exception(msg);
			}

			Fits file = new Fits();
			try {
				file.loadHeaderFITS(context.getImgEtalon());
				// calcule le meilleur nside/norder
				long nside = healpix.core.HealpixIndex.calculateNSide(file
						.getCalib().GetResol()[0] * 3600.);
				order = ((int) Util.order((int) nside) - Constante.ORDER);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// si le numéro d'order donné est différent de celui calculé
		// attention n'utilise pas la méthode context.getOrder car elle a un default à 3
		if (order != context.order && -1 != context.order) {
			context.warning("Order given (" + context.getOrder()
					+ ") != auto (" + order + ")");
		}
		else {
			context.setOrder(order);
		}
		// si le bitpix donné est différent de celui calculé
		if (context.getBitpix() != context.getBitpixOrig()) {
			context.warning("Bitpix given (" + context.getBitpix()
					+ ") != auto (" + context.getBitpixOrig() + ")");
		}
		
		// il faut au moins un cut (ou img) pour construire des JPEG
		if (context.getCut()==null && action==Action.JPEG)
			throw new Exception("You hav to give at least option img= or cut= for Jpeg construction");
	}

	/**
	 * Affecte à un objet Context l'option de configuration donnée
	 * 
	 * @param opt
	 *            nom de l'option
	 * @param val
	 *            valeur de l'option
	 * @throws Exception
	 *             si l'interprétation de la valeur nécessite une interrption du
	 *             programme
	 */
	private void setContextFromOptions(String opt, String val) throws Exception {
		// enlève des éventuels apostrophes ou guillemets
		val = val.replace("\'", "");
		val = val.replace("\"", "");
		System.out.println(opt + " = " + val);

		// System.out.println(opt +" === " +val);
		if(opt.equalsIgnoreCase("h"))
			usage();
		else if (opt.equalsIgnoreCase("verbose"))
			Context.setVerbose(Integer.parseInt(val));
		else if (opt.equalsIgnoreCase("input"))
			context.setInputPath(val);
		else if (opt.equalsIgnoreCase("output"))
			context.setOutputPath(val);
		else if (opt.equalsIgnoreCase("blank"))
			context.setBlankOrig(Double.parseDouble(val));
		else if (opt.equalsIgnoreCase("order"))
			context.setOrder(Integer.parseInt(val));
		else if (opt.equalsIgnoreCase("pixel"))
			context.setCoAddMode(CoAddMode.valueOf(val.toUpperCase()));
		else if (opt.equalsIgnoreCase("bitpix"))
			context.setBitpix(Integer.parseInt(val));
		else if (opt.equalsIgnoreCase("region")) {
			if (val.endsWith("fits")) {
				HealpixMoc moc = new HealpixMoc();
				moc.read(val);
				context.setMoc(moc);
			}
			else context.setMoc(val);
		}
		else if (opt.equalsIgnoreCase("frame"))
			context.setFrameName(val);
		else if (opt.equalsIgnoreCase("skyval"))
			context.setSkyval(val);
		else if (opt.equalsIgnoreCase("border"))
			try {
				context.setBorderSize(val);
			} catch (ParseException e) {
				System.err.println(e.getMessage());
			}
		else if (opt.equalsIgnoreCase("pixelCut"))
			context.setPixelCut(val);
		else if (opt.equalsIgnoreCase("dataCut")) {
			context.setDataCut(val);
		}
		else if (opt.equalsIgnoreCase("color"))
			context.setColor(Boolean.parseBoolean(val));
		else if (opt.equalsIgnoreCase("img")) {
			context.setImgEtalon(val);
		}
		else System.err.println("Error : unknown option " + opt);

	}

	enum Action {
		FINDER, TILES, JPEG, MOC, ALLSKY
	}

	public static void main(String[] args) {
		SkyGen generator = new SkyGen();
		int length = args.length;
		if (length == 0) {
			usage();
			return;
		}
		// extrait les options en ligne de commande, et les analyse
		for (String arg : args) {
			// si c'est dans un fichier
			String param = "-param=";
			if (arg.startsWith(param)) {
				try {
					generator.setConfigFile(arg.substring(param.length()));
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				continue;
			}
			else if (arg.equalsIgnoreCase("-h") || arg.equalsIgnoreCase("-help")) {
				SkyGen.usage();
				return;
			}
			// toutes les autres options écrasent les précédentes
			else if (arg.contains("=")) {
				String[] opts = arg.split("=");
				try {
					// si il y a un - on l'enlève
					opts[0] = opts[0].substring(opts[0].indexOf('-') + 1);

					generator.setContextFromOptions(opts[0], opts[1]);
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			// les autres mots sont supposées des actions (si +ieurs, seule la
			// dernière est gardée)
			else {
				try {
					generator.action = Action.valueOf(arg.toUpperCase());
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}

		}

		try {
			generator.validateContext();
		} catch (Exception e) {
			System.err.println(e);
//			e.printStackTrace();
			return;
		}
		// lance les calculs
		generator.start();
	}

	private void start() {
		context.setIsRunning(true);
		if (action==null) {
			// aucune action définie -> on fait la totale (sauf jpg)
			action = Action.FINDER;
			start();
			action = Action.TILES;
			start();
			action = Action.MOC;
			start();
			return;
		}
		switch (action) {
		case FINDER : { 
			System.out.println("*** Create local index :");
			BuilderIndex builder = new BuilderIndex(context);
			ThreadProgressBar progressBar = new ThreadProgressBar(builder);
			(new Thread(progressBar)).start();
			// laisse le temps au thread de se lancer
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			File f = new File(context.getHpxFinderPath()+Util.FS+"Norder"+order);
			if (f.exists())
				context.warning("  Using previous");
			else
				builder.build();
			progressBar.stop();
			break;
		}
		case JPEG : {
			System.out.println("*** Create Jpeg output :");
			BuilderJpg builder = new BuilderJpg(null, PlanImage.LINEAR, context);
			ThreadProgressBar progressBar = new ThreadProgressBar(builder);
			(new Thread(progressBar)).start();
			// laisse le temps au thread de se lancer
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			builder.run();
			progressBar.stop();
			break;
		}
		case MOC : {
			System.out.println("*** Create MultiOrderCoverage map"); 
			BuilderMoc builder = new BuilderMoc();
			builder.createMoc(context.outputPath);
			break;
		}
		case TILES : {
			System.out.println("*** Create healpix tiles : ");
			BuilderController builder = new BuilderController(context);
			ThreadProgressBar progressBar = new ThreadProgressBar(builder);
			(new Thread(progressBar)).start();
			// laisse le temps au thread de se lancer
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
			}
			try {
				builder.build();
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			} finally {
				progressBar.stop();
			}
			// + lance un allsky à la fin
//			action = Action.ALLSKY;
//			start();
			break;
		}
		case ALLSKY : {
			System.out.println("*** Create a final Allsky view : ");
			BuilderAllsky builder = new BuilderAllsky(context, -1);
			try {
				builder.createAllSky(3, 64);
				if (context.getCut()!=null)
					builder.createAllSkyJpgColor(3,64,false);
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(0);
			}
			break;
		}
		}
		context.setIsRunning(false);
	}

	private static void usage() {
		System.out.println("SkyGen -param=configfile\n");
		System.out.println("This configfile must contains these following options, or use them in comand line :");
		System.out.println(
				"input     Directory of original images (fits or jpg+hhh)" + "\n" +
				"output    Target directory (default $PWD+\"ALLSKY\")" + "\n" +
				"pixel     keep|keepall|overwrite|average|replaceall - in case of already computed values (default overwrite)" + "\n" +
				"region    Healpix region to compute (ex: 3/34-38 50 53) or Moc.fits file (nothing means all the sky)" + "\n" +
				"blank     BLANK value alternative (use of FITS header by default)" + "\n" +
				"border    Margins to ignore in the original images (N W S E or constant)" + "\n" +
				"frame     Healpix frame (C or G - default C for ICRS)" + "\n" +
				"skyval    Fits key to use for removing sky background" + "\n" +
				"bitpix    Target bitpix (default is original one)" + "\n" +
				"order     Number of Healpix Order (default computed from the original resolution)" + "\n" +
				"pixelCut  Display range cut (BSCALE,BZERO applied)(required JPEG 8 bits conversion - ex: \"120 140\")" + "\n" +
				"dataCut   Range for pixel vals (BSCALE,BZERO applied)(required for bitpix conversion - ex: \"-32000 +32000\")" + "\n" +
				"color     True if your input images are colored jpeg" + "\n" +
				"img       Image path to use for initialization" + "\n" +
				"verbose   Show live statistics : tracelevel from -1 (nothing) to 4 (a lot)" + "\n");
		System.out.println("\nUse one of these actions at end of command line :" + "\n" +
				"finder    Build finder index" + "\n" +
				"tiles     Build Healpix tiles" + "\n" +
				"jpeg      Build JPEG tiles from original tiles" + "\n" +
				"moc       Build MOC from finder Index or from target directory" + "\n" +
				"allsky    Build Allsky.fits and Allsky.jpg fits pixelCut exists (even if not used)");
	}

	private void setConfigFile(String configfile) throws Exception {
		this.file = new File(configfile);
		parseConfig();
	}

	class ThreadProgressBar implements Runnable {
		Progressive builder;
		boolean isRunning = false;
		public ThreadProgressBar(Progressive builder) {
			this.builder = builder;
		}

		@Override
		public void run() {
			isRunning=true;
			while (isRunning) {
				context.setProgress((int) this.builder.getProgress());
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
			}

		}
		public void stop() {
			context.setProgress((int) this.builder.getProgress());
			System.out.println("END");
			isRunning=false;
		}
	}
}
