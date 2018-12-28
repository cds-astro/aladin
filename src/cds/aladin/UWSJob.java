// Copyright 1999-2018 - Université de Strasbourg/CNRS
// The Aladin Desktop program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
// The Aladin Desktop program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
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
//    along with Aladin.
//

package cds.aladin;

import static cds.aladin.Constants.LOADJOBRESULT;
import static cds.aladin.Constants.PATHPHASE;
import static cds.aladin.Constants.PATHRESULTS;
import static cds.aladin.Constants.UTF8;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextAttribute;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
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

/**
 * @author chaitra
 *
 */
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
	public int requestNumber = -1;
	public Server server;//just to setStatus. May be just convert to Ball.java?
	
	//job info: jobInfoXml not sure how it looks
	
	public UWSJob(UWSFacade uwsFacade, String serverLabel, URL location) {
		// TODO Auto-generated constructor stub
		this.serverLabel = serverLabel;
		this.location = location;
		this.uwsFacade = uwsFacade;
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
	 * Method checks if job phase is not okay to start
	 */
	public boolean canRun() {
		boolean result = false;
		if (this.currentPhase.equals(HELD) || this.currentPhase.equals(PENDING)) {
			result = true;
		} else {
			result = false;
		}
		return result;
	}
	
	/**
	 * Method aborts an executing job
	 * @throws Exception 
	 * */
	public void abortJob() throws Exception {
		HttpURLConnection httpClient = null;
		try {
			httpClient = createWritePostData(this.location.toString(), PATHPHASE, "PHASE", PHASEACTION_ABORT);
			httpClient.setConnectTimeout(7000);
			String previousPhase = this.currentPhase;
			if (httpClient.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
				String locationString = httpClient.getHeaderField("Location");
				this.location = new URL(locationString);
				httpClient.disconnect();
				UWSFacade.populateJob(this.location.openStream(), this);
			} else {
				StringBuffer errorMessage = new StringBuffer("Error when aborting job:\n");
				errorMessage.append("phase: ").append(this.currentPhase);
				if (httpClient.getResponseCode() >= 400) {
					errorMessage.append("\n")
							.append(Util.handleErrorResponseForTapAndDL(this.location, httpClient));
		        }
				httpClient.disconnect();
				notificationText = UWSFacade.CANTABORTJOB;
				throw new IOException(errorMessage.toString());
			}
			this.updateGui(previousPhase);
		} catch (IOException e) {
			if (Aladin.levelTrace >= 3) e.printStackTrace();
			throw e;
		}
		finally {
			if (httpClient != null) {
				httpClient.disconnect();
			}
		}
	}
	
	/**
	 * Method to move the job to a queued or executing phase from a pending/held phase.
	 * @throws Exception 
	 */
	public void run() throws Exception {
		Aladin.trace(3,"In run. Job phase is:"+this.currentPhase);
		if (this.currentPhase.equals(PENDING) || this.currentPhase.equals(HELD)) {
			HttpURLConnection httpClient = null;
			try {
				httpClient = createWritePostData(this.location.toString(), PATHPHASE, "PHASE", PHASEACTION_RUN);
				String previousPhase = this.currentPhase;
				if (httpClient.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
					String locationString = httpClient.getHeaderField("Location");
					this.location = new URL(locationString);
					httpClient.disconnect();
					UWSFacade.populateJob(this.location.openStream(), this);
				} else {
					this.currentPhase = UNEXPECTEDERROR+ "-Phase: "+this.currentPhase;//Unexpected Error(phase)
					notificationText = UWSFacade.CANTSTARTJOB;
					StringBuffer errorMessage = new StringBuffer("Error when starting job:\n");
					errorMessage.append("\n phase: ").append(this.currentPhase);
					if (httpClient.getResponseCode() >= 400) {
						errorMessage.append("\n ")
								.append(Util.handleErrorResponseForTapAndDL(this.location, httpClient));
			        }
					httpClient.disconnect();
					throw new IOException(errorMessage.toString());
				}
				this.updateGui(previousPhase);
			} catch (IOException e) {
				if (Aladin.levelTrace >= 3) e.printStackTrace();
				throw e;
			} finally {
				if (httpClient != null) {
					httpClient.disconnect();
				}
			}
		}
		Aladin.trace(3,"run finished. Job phase is:"+this.currentPhase);
	}
	
	public HttpURLConnection createWritePostData(String baseUrl, String path, String paramName, String paramValue)
			throws IOException, URISyntaxException {
		HttpURLConnection httpClient = null;
		URL writeUrl = TapManager.getUrl(baseUrl, null, path);
		URLConnection urlConn = MultiPartPostOutputStream.createConnection(writeUrl);
		urlConn.setRequestProperty("Accept", "*/*");
		urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		// set some other request headers...
		urlConn.setRequestProperty("Cache-Control", "no-cache");
		String encodedPhaseRunParam = URLEncoder.encode(paramName, UTF8) + "=" + URLEncoder.encode(paramValue, UTF8);
		DataOutputStream os = new DataOutputStream(urlConn.getOutputStream());
		os.writeBytes(encodedPhaseRunParam);
		os.close();
		if (urlConn instanceof HttpURLConnection) {
			httpClient = (HttpURLConnection) urlConn;
			httpClient.setInstanceFollowRedirects(false);
		} else {
			throw new IOException("Error with job url. Please try again later!\nURL: " + this.location.toString());
		}
		return httpClient;
	}

	/**
	 * Method polls every second for phase change of job
	 * In case of UWS 1.1, blocking behavior can be used: blocks indefinitely until phase change
	 * @param server 
	 * @param useBlocking
	 * @param uwsFacade 
	 * @param requestNumber 
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void pollForCompletion(Server server, boolean useBlocking, UWSFacade uwsFacade, int requestNumber) throws IOException, InterruptedException, Exception {
		try {
			if (Aladin.levelTrace >= 3) System.out.println("pollForCompletion. Job phase is: "+this.currentPhase);
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
					uwsFacade.loadResults(this, null, requestNumber, server);
					break;
				} else if (this.currentPhase.equals(ERROR)) {
					this.showAsErroneous();
					this.uwsFacade.showAsyncPanel();
					Aladin.error(uwsFacade.asyncPanel, UWSFacade.JOBERRORTOOLTIP);
					if (server != null) {
						server.setStatusForCurrentRequest(requestNumber, Ball.NOK);
					}
					break;
				} else if (this.currentPhase.equals(PENDING) || this.currentPhase.equals(HELD)) {
					this.run();
				} else { //ARCHIVED, ABORTED and other stuff
					break;
				}
				updateGui(previousPhase);
				
				if (!useBlocking) {
					try {
						Thread.sleep(UWSFacade.POLLINGDELAY);
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
	 * @throws Exception 
	 */
	public void handleJobHttpInterface(URLConnection urlConn, int expectedHttpResponseCode, String genericErrorMessage, boolean setPhaseOnly) throws Exception {
		HttpURLConnection httpConn = null;
		try {
			urlConn = this.location.openConnection();
			if (urlConn instanceof HttpURLConnection) {
				httpConn = (HttpURLConnection) urlConn;
				httpConn.setInstanceFollowRedirects(false);
				if (httpConn.getResponseCode() == expectedHttpResponseCode) {
					/*if (setPhaseOnly) {
						UWSFacade.getsetPhase(this); //not to open conn again to check just phase, we have the response anyway
					} else {
						UWSFacade.populateJob(httpConn.getInputStream(), this);
					}*/
					UWSFacade.populateJob(httpConn.getInputStream(), this);
				} else if(httpConn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND){
					Aladin.trace(3, "Job is not found, user probably asked for delete: \n"+this.location.toString());
					this.currentPhase = JOBNOTFOUND;//Not found Error_404
					notificationText = UWSFacade.JOBNOTFOUNDMESSAGE;
					throw new IOException(UWSFacade.JOBNOTFOUNDMESSAGE);
				} else {
					notificationText = genericErrorMessage+this.location.toString()+"\n phase: "+this.currentPhase;
					this.currentPhase = UNEXPECTEDERROR+ "-Phase: "+this.currentPhase;//Unexpected Error(phase)
					StringBuffer errorMessage = new StringBuffer(genericErrorMessage);
					errorMessage.append("\n phase: ").append(this.currentPhase);
					if (httpConn.getResponseCode() >= 400) {
						errorMessage.append("\n")
								.append(Util.handleErrorResponseForTapAndDL(this.location, httpConn));
			        }
					httpConn.disconnect();
					throw new IOException(errorMessage.toString());
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
	
	/**
	 * As per spec- better to have result at /base async url/results/result
	 * @return
	 * @throws MalformedURLException
	 * @throws URISyntaxException 
	 */
	public String getDefaultResultsUrl() throws MalformedURLException, URISyntaxException {
		String result = null;
		URL defaultUrl = TapManager.getUrl(this.getLocation().toString(), null, PATHRESULTS);
		if (defaultUrl != null) {
			result = defaultUrl.toString();
		}
		return result;
	}
	
	public String getIfSingleResult() {
		String singleResultUrl = null;
		if (this.results != null && this.results.size() == 1) {
			singleResultUrl = results.values().iterator().next();
		}
		return singleResultUrl;
	}
	
	public void setInitialGui() {
//		this.gui = new JRadioButton(this.serverLabel+", Job: "+this.location+"     "+this.currentPhase);
		this.gui = new JRadioButton(getJobLabel());
		this.gui.setMinimumSize(new Dimension(0, Server.HAUT));
		this.gui.addActionListener(this);
		this.gui.setToolTipText(UWSFacade.UWSJOBRADIOTOOLTIP);
	}
	
	/**
	 * Gets the front end radio label display string
	 * @return
	 */
	public String getJobLabel() {
		StringBuffer radioLabel =  new StringBuffer("<html><p width=\"1600\">").append(this.currentPhase);
		radioLabel.append(" , Start time: ").append(this.startTime);
		if (this.query != null) {
			radioLabel.append(" , Query: ").append(this.query.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;"));
		}
		radioLabel.append(" ( server: ").append(this.serverLabel).append(")</p></html>");
		return radioLabel.toString();
	}
	
	/**
	 * Updates job information from the progress of the asyn thread on phase change
	 * Updates both the radio gui and if that is selected the job summary gui
	 */
	public void updateGui(String oldPhase) {
		// TODO Auto-generated method stub
		if (((oldPhase != null && !oldPhase.equals(this.currentPhase)) || oldPhase == null) && this.gui != null) {
			this.gui.setText(getJobLabel());
			this.gui.revalidate();
			this.gui.repaint();
			if (this.gui.isSelected()) {// update job details panel also if that is selected
				setJobDetailsPanel();
			}
			uwsFacade.loadResultsbutton.setVisible(UWSJob.COMPLETED.equals(this.currentPhase));
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
		
		if (this.results!=null && !this.results.isEmpty()) {
			JPanel resultsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			resultsPanel.add(new JLabel("Load on Aladin: "));
			displayResults = new JComboBox(this.results.values().toArray());
			resultsPanel.add(displayResults);
			resultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
			jobDetails.add(resultsPanel);

			JButton loadbutton = new JButton("LOAD");
			loadbutton.setActionCommand(LOADJOBRESULT);
			loadbutton.addActionListener(this);
			resultsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
//			resultsPanel.add(loadbutton);
			jobDetails.add(loadbutton);
			
			/*JButton loadDefault = new JButton(UWSFacade.STANDARDRESULTSLOAD);
			loadDefault.setToolTipText(UWSFacade.STANDARDRESULTSLOADTIP);
			loadDefault.addActionListener(this);
			loadDefault.setActionCommand(LOADDEFAULTTAPRESULT);
			jobDetails.add(loadDefault);*/
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
		
		JTextPane summary = new JTextPane();
		summary.setContentType("text/html");
		summary.setText(this.getResponsetoDisplay());
		summary.setAlignmentX(Component.LEFT_ALIGNMENT);
		summary.setCaretPosition(0);
		jobDetails.add(summary);
		
		if (!jobDetails.isVisible()) {
			jobDetails.setVisible(true);
		}
		jobDetails.revalidate();
		jobDetails.repaint();
	}
	

	public void showErrorOnServer() {
		// TODO Auto-generated method stub
		if (server != null) {
			server.setStatusForCurrentRequest(requestNumber, Ball.NOK);
		}
	}
	
	public void resetStatusOnServer() {
		// TODO Auto-generated method stub
		if (server != null) {
			server.setStatusForCurrentRequest(requestNumber, Ball.UNKNOWN);
		}
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
		Font font = new Font("PLAIN", Font.PLAIN, 15);
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
			uwsFacade.loadResultsbutton.setVisible(UWSJob.COMPLETED.equals(this.currentPhase));
		} else if (o instanceof JButton) {
			String action = ((JButton)o).getActionCommand();
			if (action.equals(LOADJOBRESULT)) {
				try {
					uwsFacade.loadResults(this, (String)displayResults.getSelectedItem(), -1, null);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					Aladin.error(uwsFacade.asyncPanel, "Error in processing results url! Please try with the default tap results url also");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					Aladin.error(uwsFacade.asyncPanel, "Unable to get the job information, please try again!");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.error(uwsFacade.asyncPanel, e1.getMessage());
				}
			}
		}
	}
	
	public String getResponsetoDisplay() {
		// TODO Auto-generated method stub
		responseBody = new StringBuffer("<html><p>");
		if (query != null) {
			responseBody.append("Job created to execute query: <b>").append(query);
		}
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
			responseBody.append("<br>Results: ").append(this.results.toString().replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;"));
		}
		if (errorType != null) {
			responseBody.append("<br>Error type: ").append(this.errorType).append("<br>Error message: ")
					.append(this.errorMessage.replaceAll("&", "&amp;").replaceAll(">", "&gt;").replaceAll("<", "&lt;"));
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
