package cds.allsky;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.StringTokenizer;

import cds.fits.Fits;
import cds.tools.pixtools.Util;

public class SkyGen {

	private File file;
	private Context context;

	protected String[] PIXELS = {"keep","overwrite","average"};
	protected int PIXELS_DEFAULT = 1;

	protected String[] FRAMES = {"C","G"};
	protected int FRAMES_DEFAULT = 0;


	public SkyGen(String configfile) throws IOException {
		
		this.file = new File(configfile);
		this.context = new Context();
		parseConfig();
	}
	
	private void parseConfig() throws IOException {
		int order = -1;
		
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
			
			//System.out.println(opt +" === " +val);
			if (opt.equalsIgnoreCase("inputdir"))
				context.setInputPath(val);
			else if (opt.equalsIgnoreCase("outputdir"))
				context.setOutputPath(val);
			else if (opt.equalsIgnoreCase("regex"))
				context.setRegex(val);
			else if (opt.equalsIgnoreCase("norder"))
				context.setOrder(Integer.parseInt(val));
			else if (opt.equalsIgnoreCase("pixel"))
				context.setCoAddMode(CoAddMode.valueOf(val));
			else if (opt.equalsIgnoreCase("bitpix"))
				context.setBitpix(Integer.parseInt(val));
			else if (opt.equalsIgnoreCase("borders"))
				try {
					context.setBorderSize(val);
				} catch (ParseException e) {System.err.println(e.getMessage()); continue;}
			else if (opt.equalsIgnoreCase("pixelCut"))
				context.setCut(val);
			else if (opt.equalsIgnoreCase("dataCut")) {
				context.setCutData(val);
			}
			else if (opt.equalsIgnoreCase("color"))
				context.setColor(Boolean.parseBoolean(val));
			else if (opt.equalsIgnoreCase("fading"))
				context.setFading(Boolean.parseBoolean(val));

			// si il y a un fichier étalon
			else if (opt.equalsIgnoreCase("img")) {
				context.setImgEtalon(val);
			}
			// sinon, on va en chercher un
			else {
				boolean found = context.findImgEtalon(context.getInputPath());
			      if( !found ) {
			         context.warning("There is no available images in source directory !\n"+context.getInputPath());
			         return;
			      }
			      String filename = context.getImgEtalon();

			      Fits file = new Fits();
			      try { file.loadHeaderFITS(context.getImgEtalon()); } 
			      catch( Exception e ) { e.printStackTrace(); }
			      // calcule le meilleur nside
			      long nside = healpix.core.HealpixIndex.calculateNSide(file.getCalib().GetResol()[0] * 3600.);
			      order = ((int) Util.order((int)nside) - Constante.ORDER);
			}

		}

		inputStream.close();
		
		// ---- Qq vérifications
		
		// arguments obligatoires
		if (context.getInputPath()==null || context.getOutputPath()==null)
			context.warning("Args inputdir/outputdir sont obligatoires");
		// données déjà présentes ?
		if (context.isExistingDir()) {
			context.warning("Le répertoire d'entrée n'existe pas");
			return;
		}
		if (context.isExistingAllskyDir()) {
			context.warning("Le répertoire de sortie existe déjà");
			if (context.getCoAddMode()!=CoAddMode.getDefault())
				context.warning("Comportement par défaut pour les pixels déjà présents "+CoAddMode.getDefault());
		}
		
		// si le numéro d'order donné est différent de celui calculé
		if (order != context.getOrder() && -1 != context.getOrder() ) {
			context.warning("Order lu (" + context.getOrder() +") != auto (" + order + ")");
		}
		// si le bitpix donné est différent de celui calculé
		if (context.getBitpix() != context.getBitpixOrig() ) {
			context.warning("Bitpix lu (" + context.getBitpix() +") != auto (" + context.getBitpixOrig() + ")");
		}
	}

	public SkyGen() {
		// TODO
	}

	public static void main(String[] args) {
		SkyGen generator = new SkyGen();
		if (args[0].equalsIgnoreCase("-param="))
			generator.setConfigFile(args[1]);
	}

	private void setConfigFile(String configfile) {
		this.file = new File(configfile);
	}
}
