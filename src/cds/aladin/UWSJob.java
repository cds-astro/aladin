package cds.aladin;

import static cds.aladin.Constants.LOADJOBRESULT;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;

import cds.tools.MultiPartPostOutputStream;
import cds.tools.Util;

public class UWSJob implements ActionListener{
	
	public static final String PHASEACTION_RUN = "RUN";
	public static final String PHASEACTION_ABORT = "ABORT";
	
	public static final String PENDING = "PENDING";
	public static final String HELD = "HELD";
	public static final String EXECUTING = "EXECUTING";
	public static final String QUEUED = "QUEUED";
	public static final String UNKNOWN = "UNKNOWN";
	public static final String SUSPENDED = "SUSPENDED";
	public static final String COMPLETED = "COMPLETED";
	public static final String ARCHIVED = "ARCHIVED";
	public static final String ERROR = "ERROR";
	public static final String ABORTED = "ABORTED";
	
	//server's attris
	private String serverLabel;
	private double version;
	
	//job attri as per spec
	private URL location;
	private String jobId;
	private String runId;
	private String ownerId;
	private String currentPhase;
	private String quote;
	private String creationTime;
	private String startTime;
	private String endTime;
	private long executionDuration;
	private String destructionTime;
	private Map<String, String> parameters;
	private Map<String, String> results;
	private String errorType; //transient or fatal
	private boolean hasErrorDetail; //true or false;
	private String errorMessage;
	private StringBuffer errorMessageDetailed;
	private StringBuffer jobInfoXml;//for now.
	public StringBuffer responseBody;
	
	//front-end things
	public JRadioButton gui;
	public JLabel notificationLabel;
	public JPanel jobDetails;
	private String query;
	private boolean deleteOnExit;
	String notificationText = "";
	public JComboBox displayResults;
	
	//front-end display messages. don't change.
	public static final String JOBNOTFOUND = "Not found ;ERROR_404";
	public static final String UNEXPECTEDERROR = "Unexpected Error ";
	public static final String INCORRECTPROTOCOL = "Error incorrect protocol ";
		
	public UWSFacade uwsFacade;
	
	//job info: jobInfoXml not sure how it looks
	
	
	public UWSJob(UWSFacade uwsFacade, String serverLabel, URL location) {
		// TODO Auto-generated constructor stub
		this.serverLabel = serverLabel;
		this.location = location;
		this.uwsFacade = uwsFacade;
	}
	
	/**
	 * Method to move the job to a queued or executing phase from a pending/held phase.
	 * @throws IOException 
	 */
	
	public void run() throws IOException {
		if (this.currentPhase.equals(PENDING) || this.currentPhase.equals(HELD)) {
			try {
				MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
				String boundary = MultiPartPostOutputStream.createBoundary();
				URL phaseChangeUrl = new URL(this.location.toString()+"/phase");
				URLConnection urlConn = MultiPartPostOutputStream.createConnection(phaseChangeUrl);
				urlConn.setRequestProperty("Accept", "*/*");
				urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
				// set some other request headers...
				urlConn.setRequestProperty("Connection", "Keep-Alive");
				urlConn.setRequestProperty("Cache-Control", "no-cache");
				MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);
				out.writeField("PHASE", "run");
				out.close();
				String previousPhase = this.currentPhase;
				handleJobHttpInterface(urlConn, HttpURLConnection.HTTP_SEE_OTHER, "Error with running job :\n", true);
				this.updateGui(previousPhase);
			} catch (IOException e) {
				// TODO Auto-generated catch block tintin
				if (Aladin.levelTrace >= 3) e.printStackTrace();
				throw e;
			}
		}
	}
	

	/**
	 * Method checks if job phase is not okay to abort
	 */
	public boolean canAbort() {
		boolean result = false;
		if (this.currentPhase.equals(COMPLETED) || this.currentPhase.equals(ABORTED) || this.currentPhase.equals(ERROR)
				|| this.currentPhase.equals(ARCHIVED)) {
			result = false;
		} else {
			result = true;
		}
		return result;
	}
	
	/**
	 * Method aborts an executing job
	 * */
	public void abortJob() throws IOException {
		HttpURLConnection httpClient;
		try {
			MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
			String boundary = MultiPartPostOutputStream.createBoundary();
			URL phaseChangeUrl = new URL(this.location.toString()+"/phase");
			URLConnection urlConn = MultiPartPostOutputStream.createConnection(phaseChangeUrl);
			urlConn.setRequestProperty("Accept", "*/*");
			urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
			// set some other request headers...
			urlConn.setRequestProperty("Connection", "Keep-Alive");
			urlConn.setRequestProperty("Cache-Control", "no-cache");
			MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);
			out.writeField("PHASE", PHASEACTION_ABORT);
			out.close();
			httpClient = (HttpURLConnection) urlConn;
			httpClient.setInstanceFollowRedirects(false);
			String previousPhase = this.currentPhase;
			if (httpClient.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
				String locationString = httpClient.getHeaderField("Location");
				this.location = new URL(locationString);
				httpClient.disconnect();
				UWSFacade.populateJob(this.location.openStream(), this);
			} else {
				throw new IOException("Error when aborting job:\n"+this.location.toString()+"\n httpcode:"+httpClient.getResponseCode()+"\n phase: "+this.currentPhase);
			}
			httpClient.disconnect();
			this.updateGui(previousPhase);
		} catch (IOException e) {
			// TODO Auto-generated catch block tintin
			e.printStackTrace();
			throw e;
		}
	}
	
	
	/**
	 * Method polls every second for phase change of job
	 * In case of UWS 1.1, blocking behavior can be used: blocks indefinitely until phase change
	 * @param useBlocking
	 * @param uwsFacade 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void pollForCompletion(boolean useBlocking, UWSFacade uwsFacade) throws IOException, InterruptedException, Exception {
		try {
			URL jobInProgressUrl = this.location;
			String previousPhase = this.currentPhase;
			while (true) {
				if (useBlocking) {
					useBlocking = this.version != 0.0d && this.version == 1.1;
					if (useBlocking) {
						jobInProgressUrl = new URL(getBlockingEndPoint());
					}
				}
				if (this.currentPhase.equals(EXECUTING) || this.currentPhase.equals(QUEUED) 
						|| this.currentPhase.equals(SUSPENDED) || this.currentPhase.equals(UNKNOWN)) {
					URLConnection conn = jobInProgressUrl.openConnection();
					handleJobHttpInterface(conn, HttpURLConnection.HTTP_OK, "Error for job: \n", false);
				} else if (this.currentPhase.equals(COMPLETED)) {
					uwsFacade.loadResults(this, this.results.entrySet().iterator().next().getValue());
					break;
				} else if (this.currentPhase.equals(ERROR)) {
					this.showAsErroneous();
					break;
				} else if (this.currentPhase.equals(PENDING) || this.currentPhase.equals(HELD)) {
					this.run();
				} else { //ARCHIVED, ABORTED and other stuff
					break;
				}
				updateGui(previousPhase);
				
				if (!useBlocking) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO: handle exception
						if (Aladin.levelTrace >= 3) e.printStackTrace();//do -not much- if poll fails
					}
				}
			}
			updateGui(previousPhase);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	/**
	 * For convenience. 
	 * Method sets phase/populate uwsjob based on a expected response code
	 * If not found -throws exception
	 * If expected code/in case of error- throws exception with generic message
	 */
	public void handleJobHttpInterface(URLConnection urlConn, int expectedHttpResponseCode, String genericErrorMessage, boolean setPhaseOnly) throws IOException {
		HttpURLConnection httpConn = null;
		try {
			if (urlConn instanceof HttpURLConnection) {
				httpConn = (HttpURLConnection) urlConn;
				httpConn.setInstanceFollowRedirects(false);
				if (httpConn.getResponseCode() == expectedHttpResponseCode) {
					if (setPhaseOnly) {
						UWSFacade.getsetPhase(this); //not to open conn again to check just phase, we have the response anyway
					} else {
						UWSFacade.populateJob(httpConn.getInputStream(), this);
					}
				} else if(httpConn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND){
					System.out.println("Job is not found, user probably asked for delete: \n"+this.location.toString());
					this.currentPhase = JOBNOTFOUND;//Not found Error_404
					notificationText = UWSFacade.JOBNOTFOUNDMESSAGE;
					throw new IOException(UWSFacade.JOBNOTFOUNDMESSAGE);
				} else {
					notificationText = genericErrorMessage+this.location.toString()+"\n phase: "+this.currentPhase;
					this.currentPhase = UNEXPECTEDERROR+ "-Phase: "+this.currentPhase;//Unexpected Error(phase)
					throw new IOException(notificationText);
				}
				httpConn.disconnect();
			} else {
				notificationText = UWSFacade.ERROR_INCORRECTPROTOCOL;
				this.currentPhase = INCORRECTPROTOCOL+ "-Phase: "+this.currentPhase;//Unexpected Error(phase)
				throw new IOException(UWSFacade.ERROR_INCORRECTPROTOCOL);//Error incorrect job url(phase)
			}
		} catch (IOException e) {
			// TODO: handle exception
			throw e;
		} finally {
			if (httpConn!=null) {
				httpConn.disconnect();
			}
		}
		
	}
	
	/**
	 * Gets block-indefinite-until-phase-change endpoint
	 * @return
	 */
	public String getBlockingEndPoint() {
		StringBuffer urlInProgress = new StringBuffer(this.location.toString());
		urlInProgress.append("?WAIT=-1&PHASE="+this.currentPhase);
		return urlInProgress.toString();
	}
	
	public String getResultsPath(String resultToLoad) throws MalformedURLException {
		String resultsUrl = null;
		if (resultToLoad != null) {
			if (resultToLoad.startsWith("http") || resultToLoad.startsWith("https")) {
				resultsUrl = resultToLoad;
			} else {
				resultsUrl = getDefaultResultsUrl();
				//parse if relative path :://TODO:: tintin
				//server : http://heasarc.gsfc.nasa.gov/xamin/vo/tap/async/1479376281691_5
				//results path : /xamin/vo/tap/async/1479376281691_5/results/result
				//server base url : http://heasarc.gsfc.nasa.gov/xamin/vo/tap/
			}
		} else {
			resultsUrl = getDefaultResultsUrl();
		}
		return resultsUrl;
	}
	
	/**
	 * As per spec- better to have result at /base async url/results/result
	 * @return
	 * @throws MalformedURLException
	 */
	public String getDefaultResultsUrl() throws MalformedURLException {
		return this.getLocation().toString()+"/results/result";
	}
	
	
	public void setInitialGui() {
//		this.gui = new JRadioButton(this.serverLabel+", Job: "+this.location+"     "+this.currentPhase);
		StringBuffer radioLabel =  new StringBuffer(this.currentPhase);
		radioLabel.append(" , Query: ").append(this.query).append(" ( server: ").append(this.serverLabel).append(")");
		this.gui = new JRadioButton(radioLabel.toString());
		this.gui.setMinimumSize(new Dimension(0, Server.HAUT));
		this.gui.addActionListener(this);
	}
	
	/**
	 * Updates job information from the progress of the asyn thread on phase change
	 * Updates both the radio gui and if that is selected the job summary gui
	 */
	public void updateGui(String oldPhase) {
		// TODO Auto-generated method stub
		if (((oldPhase != null && !oldPhase.equals(this.currentPhase)) || oldPhase == null) && this.gui != null) {
			StringBuffer radioLabel = new StringBuffer(this.currentPhase);
			radioLabel.append(" , Query: ").append(this.query)
			.append(" ( server: ").append(this.serverLabel).append(")");
			this.gui.setText(radioLabel.toString());
			this.gui.revalidate();
			this.gui.repaint();
			if (this.gui.isSelected()) {// update job details panel also if that is selected
				setJobDetailsPanel();
			}
		}
	}

	/**
	 * Shows an error gui: right now only a tooltip is added to job. 
	 * Job summary and notification text anyway shows the issue
	 */
	public synchronized void showAsErroneous() {
		if (this.gui != null) {
			this.gui.setToolTipText(UWSFacade.JOBERRORTOOLTIP);
			updateGui(null);
//			this.gui.setForeground(Color.red);
//			this.gui.setFont(getErrorFont());
		}
	}

	public synchronized void setJobDetailsPanel(){
		JPanel jobDetails = uwsFacade.jobDetails;
		jobDetails.removeAll();
		jobDetails.setLayout(new BoxLayout(jobDetails, BoxLayout.Y_AXIS));
		/*JEditorPane jobSummary = new JEditorPane();
		jobSummary.setEditable(false);
		try {
			jobSummary.setPage(this.location);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			jobSummary.setText("Unable to get job summary");
			e.printStackTrace();
		}
		
		JScrollPane scroller = new JScrollPane(jobSummary);
		scroller.getVerticalScrollBar().setUnitIncrement(4);
		scroller.setPreferredSize(new Dimension(250, 175));	
		scroller.setMinimumSize(new Dimension(20, 20));
		jobDetails.add(scroller);*/
		
		//error text if there then will be displayed.
		this.notificationLabel = new JLabel();
		this.notificationLabel.setText(notificationText);
		jobDetails.add(this.notificationLabel);
		
		
		/*jobDetails.add(new JLabel("Job ID: "+this.jobId));
		JLabel boldedText = new JLabel("URL: "+this.location);
		boldedText.setFont(CDSConstants.LBOLD);
		jobDetails.add(boldedText);
		jobDetails.add(new JLabel("Run ID: "+this.runId));
		jobDetails.add(new JLabel("Owner ID: "+this.ownerId));
		this.phaseLabel = new JLabel("phase: "+this.currentPhase);
		jobDetails.add(this.phaseLabel);
		jobDetails.add(new JLabel("Start time: "+this.startTime));
		jobDetails.add(new JLabel("End time: "+this.endTime));
		jobDetails.add(new JLabel("Execution duration: "+this.executionDuration));
		jobDetails.add(new JLabel("Destruction: "+this.destructionTime));
		jobDetails.add(new JLabel("Parameters: "+this.parameters));*/
		
		JTextPane summary = new JTextPane();
		summary.setContentType("text/html");
		summary.setText(this.getResponsetoDisplay());
		jobDetails.add(summary);
		
		if (this.results!=null && !this.results.isEmpty()) {
			JPanel resultsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			resultsPanel.add(new JLabel("Load on Aladin: "));
			displayResults = new JComboBox(this.results.values().toArray());
			resultsPanel.add(displayResults);

			JButton loadbutton = new JButton("LOAD");
			loadbutton.setActionCommand(LOADJOBRESULT);
			loadbutton.addActionListener(this);
			resultsPanel.add(loadbutton);
			jobDetails.add(resultsPanel);
		}
		
		if (errorType != null) {
			JButton button = new JButton("Show error details>>");
			button.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					if (!hasErrorDetail) {
						errorMessageDetailed = new StringBuffer("No additional error details available.");
					} else if (hasErrorDetail && (errorMessageDetailed == null || errorMessageDetailed.length() <= 0)) {
						populateDetailedErrorMessage();
					}
					Aladin.info(uwsFacade.asyncPanel, errorMessageDetailed.toString());
				}
			});
			jobDetails.add(button);
		}
		if (!jobDetails.isVisible()) {
			jobDetails.setVisible(true);
		}
		jobDetails.revalidate();
		jobDetails.repaint();
	}
	
	private void populateDetailedErrorMessage() {
		// TODO Auto-generated method stub
		BufferedReader buffReader = null;
		try {
			URL errorUrl = new URL(location.toString() + "/error");
			buffReader = new BufferedReader(new InputStreamReader(Util.openStream(errorUrl)));
			String response;
			errorMessageDetailed = new StringBuffer("Type: ");
			errorMessageDetailed.append(errorType)
			.append("\nError message: ")
			.append(errorMessage)
			.append("\n Details: \n");
			while ((response = buffReader.readLine()) != null) {
				errorMessageDetailed.append(response).append("\n");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace>=3) e.printStackTrace();
			errorMessageDetailed = new StringBuffer("No error details available");
		} finally {
			if (buffReader!=null) {
				try {
					buffReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					if (Aladin.levelTrace>=3) e.printStackTrace();
				}
			}
		}
		
	}

	public void updateParameters(HashMap<String, String> parameters, String method) {
		//403 illegal request for current job state
	}
	
	public void update(Map<String, String> parameters) {
		
	}
	
	public static Font getErrorFont() {
		Font font = new Font("PLAIN",Font.PLAIN,15);
		Map fontAttri = font.getAttributes();
		fontAttri.put(TextAttribute.STRIKETHROUGH, TextAttribute.STRIKETHROUGH_ON);
		return new Font(fontAttri);
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		// TODO Auto-generated method stub
		Object o = event.getSource();

	      // Affichage du selecteur de fichiers
		if (o instanceof JRadioButton && o.equals(gui)) {
			setJobDetailsPanel();
			uwsFacade.deleteOnExit.setVisible(true);
			uwsFacade.deleteOnExit.setSelected(isDeleteOnExit());
		} else if (o instanceof JButton) {
			String action = ((JButton)o).getActionCommand();
			if (action.equals(LOADJOBRESULT)) {
				try {
					uwsFacade.loadResults(this, (String)displayResults.getSelectedItem());
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					Aladin.warning(uwsFacade.asyncPanel, "Please enter a valid job url!");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					Aladin.warning(uwsFacade.asyncPanel, "Unable to get the job information, please try again!");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.warning(uwsFacade.asyncPanel, e1.getMessage());
				}
				
			}
		}
	}
	
	public String getResponsetoDisplay() {
		// TODO Auto-generated method stub
		responseBody = new StringBuffer("<html><p>");
		responseBody.append("Job created to execute query: <b>").append(query);
		responseBody.append("</b><br>Job ID: ").append(this.jobId).append("<br>Run ID: ").append(this.runId)
				.append("<br>URL: <b>").append(this.location.toString()).append("<br></b>Owner ID: ")
				.append(this.ownerId).append("<br>Phase: ").append(this.currentPhase).append("<br>Quote: ")
				.append(this.quote).append("<br>Creation time: ").append(this.creationTime).append("<br>Start time: ")
				.append(this.startTime).append("<br>End time: ").append(this.endTime).append("<br>Execution duration: ")
				.append(this.executionDuration).append("<br>Destruction time: ").append(this.destructionTime);
		if (this.parameters != null && !this.parameters.isEmpty()) {
			responseBody.append("<br>Parameters: ").append(this.parameters);
		}
		if (this.results != null && !this.results.isEmpty()) {
			responseBody.append("<br>Results: ").append(this.results);
		}
		if (errorType != null) {
			responseBody.append("<br>Error type: ").append(this.errorType).append("<br>Error message: ")
					.append(this.errorMessage);
		}
		if (jobInfoXml != null) {
			responseBody.append("<br>Job info: ").append(this.jobInfoXml);
		}
		return responseBody.toString();
	}
	
	public double getVersion() {
		return version;
	}
	public void setVersion(double version) {
		this.version = version;
	}
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public String getRunId() {
		return runId;
	}
	public void setRunId(String runId) {
		this.runId = runId;
	}
	public String getOwnerId() {
		return ownerId;
	}
	public void setOwnerId(String ownerId) {
		this.ownerId = ownerId;
	}
	public String getStartTime() {
		return startTime;
	}
	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}
	public String getEndTime() {
		return endTime;
	}
	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}
	public long getExecutionDuration() {
		return executionDuration;
	}
	public void setExecutionDuration(long executionDuration) {
		this.executionDuration = executionDuration;
	}
	public String getDestructionTime() {
		return destructionTime;
	}
	public void setDestructionTime(String destructionTime) {
		this.destructionTime = destructionTime;
	}
	public Map<String, String> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	public Map<String, String> getResults() {
		return results;
	}
	public void setResults(Map<String, String> results) {
		this.results = results;
	}

	public String getCurrentPhase() {
		return currentPhase;
	}

	public void setCurrentPhase(String currentPhase) {
		this.currentPhase = currentPhase;
	}
	
	public String getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(String creationTime) {
		this.creationTime = creationTime;
	}

	public URL getLocation() {
		return location;
	}

	public void setLocation(URL location) {
		this.location = location;
	}

	public String getServerLabel() {
		return serverLabel;
	}

	public void setServerLabel(String serverLabel) {
		this.serverLabel = serverLabel;
	}

	public String getQuote() {
		return quote;
	}

	public void setQuote(String quote) {
		this.quote = quote;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public boolean isDeleteOnExit() {
		return deleteOnExit;
	}

	public void setDeleteOnExit(boolean deleteOnExit) {
		this.deleteOnExit = deleteOnExit;
	}

	public String getErrorType() {
		return errorType;
	}

	public void setErrorType(String errorType) {
		this.errorType = errorType;
	}

	public boolean isHasErrorDetail() {
		return hasErrorDetail;
	}

	public void setHasErrorDetail(boolean hasErrorDetail) {
		this.hasErrorDetail = hasErrorDetail;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public StringBuffer getJobInfoXml() {
		return jobInfoXml;
	}

	public void setJobInfoXml(StringBuffer jobInfoXml) {
		this.jobInfoXml = jobInfoXml;
	}

}
