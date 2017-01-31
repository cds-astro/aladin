package cds.aladin;

import static cds.aladin.Constants.*;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import cds.tools.Util;


/**
 * This class is to show the upload frame for Tap servers
 *
 */
public class FrameUploadServer extends JFrame implements ActionListener, PlaneLoadListener {

   /**
	 * 
	 */
	private static final long serialVersionUID = 399753558953437543L;

	static String TITLE, CLOSE, ERRORMSG, IDENTIFIER, TAPTABLEUPLOADTIP, UPFILEINFO, BROWSE, TAPDELETEALLTIP;
	public static final String UPLOADFILEPREFIX = "file-";
	private Aladin aladin;
	protected Map<String, String> uploadingPlanCatalogs;
	protected JComboBox<String> uploadOptions;
	protected Map<String, File> uploadedTableFiles = new HashMap<String, File>();
	protected ServerTap uploadServer;
	GridBagConstraints c;
	JTextField tableName;
	JLabel infoLabel;
	protected int selectedFile = -1;
	JTextField systemFile;
	ButtonGroup radioGroup;

	protected void createChaine() {
		TITLE = aladin.chaine.getString("UPTITLE");
		CLOSE = aladin.chaine.getString("CLOSE");
		IDENTIFIER = aladin.chaine.getString("ISIDENTIFIER");
		ERRORMSG = aladin.chaine.getString("ERROR");
		TAPTABLEUPLOADTIP = aladin.chaine.getString("TAPTABLEUPLOADTIP");
		UPFILEINFO = aladin.chaine.getString("UPFILEINFO");
		BROWSE = aladin.chaine.getString("FILEBROWSE");
		TAPDELETEALLTIP = aladin.chaine.getString("TAPDELETEALLTIP");
	}

	/**
	 * @param url 
	 * @wbp.parser.constructor
	 */
	protected FrameUploadServer(Aladin aladin, String mainServerUrl) {
		super();
		this.aladin = aladin;
		Aladin.setIcon(this);
		enableEvents(AWTEvent.WINDOW_EVENT_MASK);
		Util.setCloseShortcut(this, false, aladin);
		
		uploadServer = new ServerTap(aladin);
		uploadServer.primaryColor = Aladin.BACKGROUND;
		uploadServer.secondColor = Aladin.COLOR_CONTROL_BACKGROUND;
//		newServer.aladinLabel = this.selectedServerLabel.getLabel();
		uploadServer.isUploadServer = true;
		uploadServer.setName("LocalResources");
		uploadServer.setUrl(mainServerUrl);
		uploadServer.tablesMetaData = new HashMap<String, TapTable>();
		createChaine();
		setTitle(TITLE);
		getContentPane().setLayout(new GridBagLayout());
		
		setLocation(Aladin.computeLocation(this));
		uploadingPlanCatalogs = new HashMap<String,String>();
		setUploadFileForm();
		
	}
	
	/** Affichage des infos associées à un serveur */
	protected void show(Server s) {
		if (s instanceof ServerTap) {
			setTitle("Upload to "+s.getName());
			uploadServer.setUrl(((ServerTap)s).getUrl());
		}
		setFont(Aladin.PLAIN);
		pack();
		setVisible(true);
	}
	
	/**
	 * Creates the first form for loading upload data
	 */
	public void setUploadFileForm() {
		this.getContentPane().setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		c = new GridBagConstraints();
		c.gridx = 0;
	    c.gridy = 0;
	    c.gridwidth = 3; 
	    c.fill = GridBagConstraints.NONE;
	    c.anchor = GridBagConstraints.LINE_START;
	    c.insets = new Insets(10, 10, 1, 5);
	 // Premiere indication
		JLabel l = new JLabel(UPFILEINFO);
		this.getContentPane().add(l, c);

		c.gridy++;
		c.gridwidth = 1;
//		this.getContentPane().add(file, c);
		c.insets = new Insets(1, 10, 1, 1);
		radioGroup = new ButtonGroup();
		JRadioButton radio1= new JRadioButton();
		radio1.setSelected(true);
		selectedFile = 0;
		radioGroup.add(radio1);
		c.weightx = 0.01;
		this.getContentPane().add(radio1, c);
		
		c.gridx = 1;
		c.insets = new Insets(1,1,1,1);
		c.weightx = 0.06;
		this.getContentPane().add(new JLabel("Local file"), c);
		
		c.gridx = 2;
		c.weightx = 0.90;
		systemFile = new JTextField();
		systemFile.setPreferredSize(new Dimension(240, Server.HAUT));
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		this.getContentPane().add(systemFile, c);
		
		radio1.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				selectedFile = 0;
			}
		});

		// Pour s'aider d'une boite de recherche
		if (Aladin.STANDALONE) {
			JButton browse = new JButton(BROWSE);
			browse.addActionListener(this);
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.WEST;
			c.insets = new Insets(1, 1, 1, 5);
			c.weightx = 0.03;
			c.gridx = 3;
			this.getContentPane().add(browse, c);
		}
//	    this.getContentPane().add(new ServerFile(aladin, 45), c);
		
		Vector<String> canUploadVOTablesNames = getUploadablePlaneNames();
		updateAllUploadableVoTables(this.getContentPane(), canUploadVOTablesNames, radioGroup);
		
		makeBottomPanel(this.getContentPane());
		
	}
	
	public void makeBottomPanel(Container containerPanel) {
		c.gridy++;
		c.gridx = 1;
		c.weightx = 0.06;
		c.insets = new Insets(1,1,1,1);
		c.gridwidth = 1;
		containerPanel.add(new JLabel("Table name suffix:"), c);
		
		c.gridx = 2;
		c.weightx = 0.94;
		this.tableName = new JTextField();
		this.tableName.setPreferredSize(new Dimension(240, Server.HAUT));
		containerPanel.add(this.tableName, c);
		this.tableName.setText(this.generateSuffix());
		
		c.gridx = 0;
		c.gridwidth = 4;
		c.gridy++;
//		c.insets = new Insets(2, 35, 1, 5);
		c.fill = GridBagConstraints.HORIZONTAL;
		
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
		JButton button = new JButton("Upload");
		button.setActionCommand(UPLOAD);
		button.addActionListener(this);
		button.setPreferredSize(new Dimension(80, Server.HAUT));
		button.setToolTipText(TAPTABLEUPLOADTIP); 
		buttonsPanel.add(button);
		
//		button = new JButton("Delete all");
//		button.setActionCommand(DELETEALL);
//		button.addActionListener(this);
//		button.setPreferredSize(new Dimension(80, Server.HAUT));
//		button.setToolTipText(TAPDELETEALLTIP); 
//		buttonsPanel.add(button);
		containerPanel.add(buttonsPanel, c);
		
		c.gridx = 0;
		c.gridy++;
		infoLabel = new JLabel();
		infoLabel.setFont(Aladin.ITALIC);
		c.insets = new Insets(20, 10, 5, 2);
		containerPanel.add(infoLabel, c);
	}
	
	/**
	 * Method adds the gui for selection of files already loaded in Aladin.
	 * @param containerPanel
	 * @param canUploadVOTablesNames
	 * @param radioGroup
	 */
	public void updateAllUploadableVoTables(Container containerPanel, Vector<String> canUploadVOTablesNames, ButtonGroup radioGroup) {
//		containerPanel = new JPanel(new GridBagLayout());
	    c.fill = GridBagConstraints.NONE;
	    c.anchor = GridBagConstraints.LINE_START;
	    c.gridx = 0;
	    c.gridy++;
		
		JRadioButton radio2= new JRadioButton();
		radio2.setName("loadedFileRadio");
		c.weightx = 0.01;
		radio2.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				selectedFile = 1;
			}
		});
		c.insets = new Insets(1, 10, 1, 1);
		containerPanel.add(radio2, c);
		radioGroup.add(radio2);
		
		c.gridx = 1;
		c.insets = new Insets(1,1,1,1);
		c.weightx = 0.06;
		containerPanel.add(new JLabel("Already loaded file:"), c);
		
		c.gridx = 2;
		c.weightx = 0.93;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.WEST;
		if (canUploadVOTablesNames == null || canUploadVOTablesNames.isEmpty()) {
			uploadOptions  = new JComboBox<String>();
			uploadOptions.setEnabled(false);
		} else {
			uploadOptions  = new JComboBox<String>(canUploadVOTablesNames);
		}
		 
		uploadOptions.setOpaque(false);
		containerPanel.add(uploadOptions, c);
		pack();
	}
	
	/**
	 * Gets all the plane names that can be used for upload queries (votables). 
	 * @return
	 */
	public Vector<String> getUploadablePlaneNames() {
		Vector<String> canUploadVOTablesNames = new Vector<String>();
		Plan[] plan = this.aladin.calque.plan;
		for (int i = 0; i < plan.length; i++) {
			if (plan[i].pcat!=null && plan[i].pcat.flagVOTable) {
				canUploadVOTablesNames.add(plan[i].label);
			}
		}
		return canUploadVOTablesNames;
	}

	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		
		if (command.equals("SUBMIT")) {
			Map<String, Object> requestParams = new HashMap<String, Object>();
			if (uploadedTableFiles.get(uploadServer.selectedTableName) == null) {
				Aladin.warning(this.getContentPane(), "Unable to submit " + uploadServer.selectedTableName + " data!");
				return;
			}
			String uploadFileName = UPLOADFILEPREFIX+uploadServer.selectedTableName;
			requestParams.put(uploadFileName, uploadedTableFiles.get(uploadServer.selectedTableName));
			requestParams.put("upload", getUploadParam(uploadServer.selectedTableName, uploadFileName));
			
			uploadServer.submit(requestParams);
		} else if (command.equals(UPLOAD)) {
			//Just parse the selected table's metadata to create gui and store file version of it
			if (checkInputs()) {
				return;
			}
			TapManager tapManager = TapManager.getInstance(aladin);
			uploadServer.showloading();
			String uploadTableName = UPLOADTABLEPREFIX.concat(tableName.getText());
			String fileName = EMPTYSTRING;
			switch (selectedFile) {
			case 0:
				Plan loadingPlan = aladin.calque.createPlan(systemFile.getText().trim(),"localTableData",null,uploadServer);
				loadingPlan.addPlaneLoadListener(this);
				uploadingPlanCatalogs.put(loadingPlan.label, uploadTableName);
				break;
			case 1:
				fileName = uploadOptions.getSelectedItem().toString().trim();
				Plan planToLoad = tapManager.getPlan(fileName);
				try {
					saveUploadFile(uploadTableName, planToLoad);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if(Aladin.levelTrace >= 3) e.printStackTrace();
					Aladin.warning(this.getContentPane(), "Unable upload " + planToLoad.label + " data!");
				}
				break;
			default:
				Aladin.warning(this.getContentPane(), "Please select your upload data!");
				return;
			}
			pack();
		} else if (command.equals(BROWSE)) {
			 browseFile();
	         return;
		} else if (command.equals(CLOSE))
			setVisible(false);
	}
	
	protected String getUploadParam(String tableName, String fileName) {
		return tableName.replace(UPLOADTABLEPREFIX, EMPTYSTRING).concat(",param:").concat(fileName);//TODO:: tintin when doing join need to send more files
	}
	
	@Override
	public void planeLoaded(PlaneLoadEvent ple) {
		if (ple.status == PlaneLoadEvent.SUCCESS) {
			String uploadTableName = uploadingPlanCatalogs.get(ple.plane.label);
			try {
				saveUploadFile(uploadTableName, ple.plane);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				if(Aladin.levelTrace >= 3) e.printStackTrace();
				Aladin.warning(this.getContentPane(), "Unable upload " + ple.plane.label + " data!");
			}
		} else {
			Aladin.warning(this.getContentPane(), "Cannot upload " + ple.plane.label + " data. Error!");
			uploadingPlanCatalogs.remove(uploadingPlanCatalogs.get(ple.plane.label));
		}
		
	}
	
	/**
	 * Method adds request param file(created again from PlanCatalog) into cache for the upload table
	 * File name = UPLOADFILEPREFIX+uploadTableName.xml
	 * @param uploadTableName
	 * @param planCatalog
	 * @throws Exception
	 */
	private void saveUploadFile(String uploadTableName, Plan plan) throws Exception {
		if (!(plan instanceof PlanCatalog)) {
			throw new Exception("Cannot upload " + plan.label + " data. Please select a catalog!");
		}
		PlanCatalog planCatalog = (PlanCatalog) plan;
		final File tmpFile;
		if ((tmpFile = aladin.createTempFile(UPLOADFILEPREFIX+uploadTableName, ".xml")) == null) {
			// TODO:: tintin when doing join need to send more files
			System.err.println("ERROR in aladin.createTempFile for "+uploadTableName);
			throw new Exception("Unable upload " + planCatalog.label + " data!");
		}
		if (aladin.save == null)
			aladin.save = new Save(aladin);
		aladin.save.saveCatVOTable(tmpFile, planCatalog, false);
		uploadedTableFiles.put(uploadTableName, tmpFile);
		
		TapManager tapManager = TapManager.getInstance(aladin);
		tapManager.createTapServerFromAladinPlan(planCatalog, uploadTableName);
		
		this.tableName.setText(this.generateSuffix());
		this.infoLabel.setText("New table(Name: "+uploadTableName+") from "+planCatalog.label+" is parsed in Aladin!");
		tapManager.eraseNotification(this.infoLabel);
	}
	
	protected void createUploadServer() {
//		tapManager.loadTapColumnSchemas("http://www.cadc-ccda.hia-iha.nrc-cnrc.gc.ca/tap");
		uploadServer.setOpaque(true);
		uploadServer.createFormDefault(); //default choice is the first table
		
		c.insets = new Insets(5, 10, 10, 5);
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 4;
		c.weighty = 0.94;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTH;
		this.getContentPane().add(uploadServer, c);
		
		c.gridy++;
		c.weighty = 0.01;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.SOUTH;
		JButton submit = new JButton("Submit");
		submit.addActionListener(this);
		submit.setActionCommand("SUBMIT");
		this.getContentPane().add(submit, c);
		
		uploadServer.revalidate();
		uploadServer.repaint();
		
	}
	
	protected boolean checkInputs() {
		boolean hasError = true;
		if (selectedFile == -1) {
			Aladin.warning(this.getContentPane(), "Please select your upload data!");
		} else if (tableName.getText().isEmpty()  ||  !isTableNameValid(tableName.getText())) {
			Aladin.warning(this.getContentPane(), "Please input a valid table name (with no special characters)!");
		} else if (tableAlreadyExists(tableName.getText())) {
			Aladin.warning(this.getContentPane(), "This table name is already submitted for upload. Please edit the table name!");
		} else if (selectedFile == 0 && systemFile.getText().trim().isEmpty() ) {
			Aladin.warning(this.getContentPane(), "Please choose a file!");
		} else if (selectedFile == 1 && uploadOptions.getSelectedItem()==null ) {
			Aladin.warning(this.getContentPane(), "Please choose a file!");
		} else {
			hasError = false;
		}
		return hasError;
	}
	
	/** Ouverture de la fenêtre de sélection d'un fichier */
	   protected void browseFile() {
	      String path = Util.dirBrowser("Choose file/URL to upload", aladin.getDefaultDirectory(),systemFile,2);
	      if( path == null ) return;

	      String dir = path;
	      int offset = path.indexOf(" ");
	      if( offset > 0 ) {
	         Tok tok = new Tok(path," ");
	         dir = tok.nextToken();
	      }
	      
	      File f = new File(dir);
	      if( !f.isDirectory() ) dir = f.getParent();
	      aladin.memoDefaultDirectory(dir);
	   }
	
   /**
    * Method checks if table name is already used
    * @param input
    * @return
    */
	public boolean tableAlreadyExists(String input) {
		String userInput = UPLOADTABLEPREFIX.concat(input);
		Set<String> existingUploadedTables = this.uploadServer.tablesMetaData.keySet();
		Set<String> existingUploadingTables = this.uploadingPlanCatalogs.keySet();
		return (existingUploadedTables.contains(userInput) || existingUploadingTables.contains(userInput));
	}
	
	/**
	 * Method checks if the tablename is valid.
	 * @param input
	 * @return
	 */
	public boolean isTableNameValid(String input) {
		boolean result = false;
		Pattern regex = Pattern.compile(REGEX_ONLYALPHANUM);//find no special chars
		Matcher matcher = regex.matcher(input);
		if (!matcher.find()){
			regex = Pattern.compile(REGEX_ALPHA);//find atleast one alphabet
			matcher = regex.matcher(input);
			if (matcher.find()){
				result = true;
			}			
		}
		
		return result;
	}
	
	/**
	 * Method to generate a unique <i>upload</i> table name
	 * @param uploadFrame
	 * @return
	 */
	public String generateUploadTableName() {
		String uploadTableName = ALADINTABLEPREFIX + new Random().nextInt(Integer.SIZE - 1);
		if (!tableAlreadyExists(uploadTableName)) {
			return uploadTableName;
		}
		return generateUploadTableName();
	}
	
	/**
	 * Method to generate a unique suffix for an <i>upload</i> table name
	 * @param uploadFrame
	 * @return
	 */
	public String generateSuffix() {
		String uploadTableSuffix = "AladinTable" + new Random().nextInt(Integer.SIZE - 1);
		String uploadTableName = "TAP_UPLOAD".concat(uploadTableSuffix);
		if (!tableAlreadyExists(uploadTableName)) {
			return uploadTableSuffix;
		}
		return generateSuffix();
	}


}
