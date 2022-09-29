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

/**
 * 
 */
package cds.aladin;

import static cds.aladin.Constants.ABORTJOB;
import static cds.aladin.Constants.DELETEJOB;
import static cds.aladin.Constants.DELETEONEXIT;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.GETPREVIOUSSESSIONJOB;
import static cds.aladin.Constants.LOADDEFAULTTAPRESULT;
import static cds.aladin.Constants.NEWLINE_CHAR;
import static cds.aladin.Constants.PATHPHASE;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import cds.tools.MultiPartPostOutputStream;
import cds.tools.Util;
import cds.xml.UWSReader;

/**
 * @author chaitra
 *
 */
public class UWSFacade implements ActionListener{
	
	private static UWSFacade instance = null;
	public Aladin aladin;
	public FrameSimple jobControllerGui;
	public MySplitPane asyncPanel;
	private JPanel inSessionAsyncJobsPanel;
	public JPanel jobDetails;
	private ButtonGroup radioGroup;
	private JRadioButton prevJobRadio;
	private List<UWSJob> sessionUWSJobs;
	private JTextField previousSessionJob;
	public JCheckBox deleteOnExit;
	public JButton loadResultsbutton;
	
	public static String JOBNOTFOUNDMESSAGE, JOBERRORTOOLTIP, UWSNOJOBMESSAGE, CANTSTARTJOB, GENERICERROR1LINE,
			STANDARDRESULTSLOAD, STANDARDRESULTSLOADTIP, UWSASKLOADDEFAULTRESULTS, CANTABORTJOB, UWSJOBRADIOTOOLTIP,
			JOBCONTROLLERTITLE, UWSPANELCURRECTSESSIONTITLE, UWSPANELPREVIOUSSESSIONTITLE, JOBNOTSELECTED, JOBNOTFOUNDGIVENURL,
			NOJOBURLMESSAGE, JOBDELETEERRORMESSAGE, DELETEONCLOSEBUTTONLABEL, UWSMULTIJOBLOADMESSAGE, UWSASKTOREMOVENOTFOUNDJOBS, 
			UWSASKTOREMOVENOTFOUNDJOBSTITLE ;
	public static String ERROR_INCORRECTPROTOCOL = "IOException. Job url not http protocol!";
	public static final int POLLINGDELAY = 4000; //increasing the polling delay to 4secs after consulting Markus Demleitner and Mark Taylor (at Asterics TechForum4)
	public static int DELETETIMEOUTTIME = 7000;
	
	static {
		JOBNOTFOUNDMESSAGE = Aladin.chaine.getString("JOBNOTFOUNDMESSAGE");
		JOBERRORTOOLTIP = Aladin.chaine.getString("JOBERRORTOOLTIP");
		UWSNOJOBMESSAGE = Aladin.chaine.getString("UWSNOJOBMESSAGE");
		CANTSTARTJOB = Aladin.chaine.getString("CANTSTARTJOB");
		GENERICERROR1LINE = Aladin.getChaine().getString("GENERICERROR")+"\n";
		STANDARDRESULTSLOAD = Aladin.getChaine().getString("STANDARDRESULTSLOAD");
		STANDARDRESULTSLOADTIP = Aladin.getChaine().getString("STANDARDRESULTSLOADTIP");
		UWSASKLOADDEFAULTRESULTS = Aladin.getChaine().getString("UWSASKLOADDEFAULTRESULTS");
		CANTABORTJOB= Aladin.getChaine().getString("CANTABORTJOB");
		UWSJOBRADIOTOOLTIP = Aladin.getChaine().getString("UWSJOBRADIOTOOLTIP");
		JOBCONTROLLERTITLE = Aladin.getChaine().getString("JOBCONTROLLERTITLE");
		UWSPANELCURRECTSESSIONTITLE = Aladin.getChaine().getString("UWSPANELCURRECTSESSIONTITLE");
		UWSPANELPREVIOUSSESSIONTITLE = Aladin.getChaine().getString("UWSPANELPREVIOUSSESSIONTITLE");
		JOBNOTSELECTED = Aladin.getChaine().getString("JOBNOTSELECTED");
		JOBNOTFOUNDGIVENURL = Aladin.getChaine().getString("JOBNOTFOUNDGIVENURL");
		NOJOBURLMESSAGE = Aladin.getChaine().getString("NOJOBURLMESSAGE");
		JOBDELETEERRORMESSAGE = Aladin.getChaine().getString("JOBDELETEERRORMESSAGE");
		DELETEONCLOSEBUTTONLABEL = Aladin.getChaine().getString("DELETEONCLOSEBUTTONLABEL");
		UWSMULTIJOBLOADMESSAGE = Aladin.getChaine().getString("UWSMULTIJOBLOADMESSAGE");
		UWSASKTOREMOVENOTFOUNDJOBS = Aladin.getChaine().getString("UWSASKTOREMOVENOTFOUNDJOBS");
		UWSASKTOREMOVENOTFOUNDJOBSTITLE = Aladin.getChaine().getString("UWSASKTOREMOVENOTFOUNDJOBSTITLE");
	}
	
	public UWSFacade() {
		// TODO Auto-generated constructor stub
	}
	
	public UWSFacade(Aladin aladin) {
		// TODO Auto-generated constructor stub
		this();
		this.aladin = aladin;
	}
	
	public static synchronized UWSFacade getInstance(Aladin aladin) {
		if (instance == null) {
			instance = new UWSFacade(aladin);
		}
		return instance;
	}
	
	/**
	 * Handles async request for server panels
	 * @param aladin
	 * @param server
	 * @param label
	 * @param url
	 * @param requestParams
	 * @param requestNumber
	 */
	public static synchronized void fireAsync(Aladin aladin, Server server, String label, String url,
			Map<String, Object> requestParams, int requestNumber) {
		UWSFacade instance = getInstance(aladin);
		instance.instantiateGui();
		try {
			URL requestUrl = TapManager.getUrl(url, null, null);
			instance.handleJob(server, label, requestUrl, null, requestParams, false, requestNumber); //gavo gives error with PHASE = RUN on create job
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Method where an asyn job is created and handled 
	 * @param server 
	 * @param query 
	 * @param queryString 
	 * @param requestParams 
	 * @param requestNumber 
	 */
	public void handleJob(Server server, String label, URL url, String queryString,
			Map<String, Object> requestParams, boolean doRun, int requestNumber) {
		UWSJob job = null;
		try {
			job = createJob(server, label, url, queryString, requestParams, doRun, requestNumber);
			addNewJobToGui(job);
			refreshGui();
			job.run();
			job.pollForCompletion(server, true, this, requestNumber);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			StringBuffer errorMessageToUser = new StringBuffer(GENERICERROR1LINE);
			if (job == null || job.gui == null) {
				errorMessageToUser.append("\n Unable to create job");
			}
			if (queryString != null) {
				errorMessageToUser.append("\n For query: ").append(queryString).append(NEWLINE_CHAR).append(e.getMessage());
			} else {
				errorMessageToUser.append("\n at: ").append(url.toString()).append(NEWLINE_CHAR).append(e.getMessage());
			}
			
			if (job != null && UWSJob.JOBNOTFOUND.equals(job.getCurrentPhase())) {//specific case of job not found
				if (checkIfJobInCache(job)) {
					Aladin.trace(3, "Job is not found, user did not ask for delete: \n"+job.getLocation().toString());
//					removeJobUpdateGui(job); Not removing deleted jobs(deleted elsewhere) from gui. maybe user needs to view ? 
					Aladin.error(asyncPanel, errorMessageToUser.toString());
					if (job != null) {
						job.showAsErroneous();
					} 
				} else {
					if (job != null) {
						job.resetStatusOnServer();
					} 
				}
			} else {
				Aladin.error(asyncPanel, errorMessageToUser.toString());
				if (job != null) {
					job.showAsErroneous();
				} 
			}
			if (server != null) {
				server.setStatusForCurrentRequest(requestNumber, Ball.NOK);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if (job != null) {
				job.showAsErroneous();
			}
			if (server != null) {
				server.setStatusForCurrentRequest(requestNumber, Ball.NOK);
			}
			Aladin.error(asyncPanel, "Error with async job! "+e.getMessage());
		}
	}
	
	/**
	 * Method creates an async job
	 * @param query
	 * @param queryString 
	 * @param newJobCreationUrl
	 * @param postParams 
	 * @param requestNumber 
	 * @return
	 * @throws Exception
	 */
	public UWSJob createJob(Server server, String jobLabel, URL requestUrl, String queryString,
			Map<String, Object> postParams, boolean doRun, int requestNumber) throws Exception {
		UWSJob job = null;
		HttpURLConnection httpClient = null;
		try {
			Aladin.trace(3,"trying to createJob() uws for:: "+requestUrl.toString());
//			URL tapServerRequestUrl = new URL("http://cdsportal.u-strasbg.fr/uwstuto/basic/timers");
			MultiPartPostOutputStream.setTmpDir(Aladin.CACHEDIR);
			String boundary = MultiPartPostOutputStream.createBoundary();
			URLConnection urlConn = MultiPartPostOutputStream.createConnection(requestUrl);
			urlConn.setRequestProperty("Accept", "*/*");
			urlConn.setRequestProperty("Content-Type", MultiPartPostOutputStream.getContentType(boundary));
			// set some other request headers...
			urlConn.setRequestProperty("Connection", "Keep-Alive");
			urlConn.setRequestProperty("Cache-Control", "no-cache");
			MultiPartPostOutputStream out = new MultiPartPostOutputStream(urlConn.getOutputStream(), boundary);

			//standard request parameters
//			out.writeField("request", "doQuery");
//			out.writeField("lang", "ADQL-2.0");
//			out.writeField("version", "1.0");
//			out.writeField("format", "votable");
			
			
//			out.writeField( "REQUEST", "doQuery" );
//			out.writeField( "LANG", "ADQL" );
//			
//			Aladin.trace(3,"createJob() REQUEST :: doQuery");
//			Aladin.trace(3,"createJob() LANG :: ADQL");
//			
//			out.writeField("QUERY", queryString);
//			Aladin.trace(3,"createJob() QUERY :: "+queryString);
			
			
			if (postParams != null) {//this part only for upload as of now
				for (Entry<String, Object> postParam : postParams.entrySet()) {
					if (postParam.getValue() instanceof String) {
						out.writeField(postParam.getKey(), String.valueOf(postParam.getValue()));
						Aladin.trace(3,"createJob() "+postParam.getKey()+" :: "+String.valueOf(postParam.getValue()));
					} else if (postParam.getValue() instanceof File) {
						out.writeFile(postParam.getKey(), null, (File) postParam.getValue(), false);
						Aladin.trace(3,"createJob() "+postParam.getKey()+" :: "+(File) postParam.getValue());
					}
				}
			}
			
			if (doRun) { //gavo won't allow in case of async soda service.
				out.writeField("PHASE", "RUN"); // remove this if we start comparing quotes
				Aladin.trace(3,"createJob() PHASE :: "+"RUN");
			}

//			out.writeField("time", "10");
//			out.writeField("name", "ti");
			
			out.close();
			if (!(urlConn instanceof HttpURLConnection)) {
				throw new Exception("Error url is not http!");
			}
			
			httpClient = (HttpURLConnection) urlConn;
			httpClient.setInstanceFollowRedirects(false);
			
			if (httpClient.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {// is accepted
				String location = httpClient.getHeaderField("Location");
				job = new UWSJob(this, jobLabel, new URL(location));
				populateJob(job.getLocation().openStream(), job);
				if (postParams != null && postParams.containsKey("QUERY")) {
					job.setQuery((String) postParams.get("QUERY"));
				}
				
				job.setDeleteOnExit(true);
//				getsetPhase(job);
				job.setInitialGui();
			} else {
				String errorMessage = Util.handleErrorResponseForTapAndDL(requestUrl, httpClient);
				//also for erroneous scenarios where error code is <400
				Aladin.trace(3,"createJob() ERROR !! did not get a url redirect. \n"+errorMessage);
				throw new Exception(errorMessage);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		} finally {
			if (httpClient != null) {
				httpClient.disconnect();
			}
		}
		Aladin.trace(3,"In createJob. Job phase is:"+job.getCurrentPhase());
		job.server = server;
		job.requestNumber = requestNumber;
		return job;
	}
	
	
	/**
	 * Adds newly created job into front-end and model
	 * @param job
	 */
	public synchronized void addNewJobToGui(UWSJob job) {
		if (sessionUWSJobs == null) {
			sessionUWSJobs = new ArrayList();
			inSessionAsyncJobsPanel.removeAll();
		}
		sessionUWSJobs.add(job);
		inSessionAsyncJobsPanel.add(job.gui);
		radioGroup.add(job.gui);
//		job.gui.setActionCommand(job.getLocation().toString());
//		job.gui.addActionListener(this);
	}
	
	/**
	 * Method deletes model instances and updates gui for a deleted job
	 * @param job
	 */
	public void removeJobUpdateGui(UWSJob job) {
		if (sessionUWSJobs!=null && sessionUWSJobs.contains(job)) {
			sessionUWSJobs.remove(job);
			if (job.gui.isSelected()) {
				if (sessionUWSJobs.size() > 0) {
					sessionUWSJobs.get(0).gui.setEnabled(true);
					sessionUWSJobs.get(0).gui.doClick();
				} else {
					clearJobsummary();
					deleteOnExit.setVisible(false);
				}
			}
			inSessionAsyncJobsPanel.remove(job.gui);
			inSessionAsyncJobsPanel.revalidate();
			inSessionAsyncJobsPanel.repaint();
		}
	}
	
	/**
	 * Checks if the job is stored in cache
	 * @param job
	 * @return
	 */
	public boolean checkIfJobInCache(UWSJob job) {
		boolean result = false;
		if (sessionUWSJobs != null && sessionUWSJobs.contains(job)) {
			result = true;
		} else {
			result = false;
		}
		return result;
	}
	
	public synchronized void refreshGui() {
		asyncPanel.revalidate();
		asyncPanel.repaint();
	}
	
	public void clearJobsummary() {
		jobDetails.removeAll();
		jobDetails.revalidate();
		jobDetails.repaint();
	}
	
	/**
	 * Basic buffered reader for http get
	 * @param url
	 * @return response
	 */
	public static StringBuffer getResponse(InputStream in) {
		BufferedReader buffReader = null;
		StringBuffer result = null;
		try {
			buffReader = new BufferedReader(new InputStreamReader(in));
			String response;
			result = new StringBuffer();
			while ((response = buffReader.readLine()) != null) {
				result.append(response);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = null;
		} finally {
			if (buffReader!=null) {
				try {
					buffReader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return result;
	}
	
	/**
	 * Basic buffered reader for http get
	 * @param url
	 * @return response
	 */
	public static StringBuffer getResponse(URL url) {
		StringBuffer result = null;
		try {
			result = getResponse(Util.openStream(url));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			result = null;
		}
		return result;
	}
	
	public void checkPhase(UWSJob job) {
		//404 for non-exixtant job id
		//500 Internal ser err
		//200 normally inspite of error- need to check the error part
	}
	
	/**
	 * Method updates job based on the new uws status
	 * UWS spec 1.1, Section 2.2.2: the default behavior is to return XML, "accept" not set explicitly.
	 * @param inputStream
	 * @param job
	 * @throws Exception 
	 */
	public static void populateJob(InputStream inputStream, UWSJob job) throws Exception {
//		SavotPullParser savotPullParser = new SavotPullParser(httpClient.getInputStream(), SavotPullEngine.FULL, null);
		UWSReader uwsReader = new UWSReader();
		synchronized (job) {
			uwsReader.load(inputStream, job);
			inputStream.close();
			Aladin.trace(3, "in populateJob phase is:"+job.getCurrentPhase()+" results"+job.getResults());
		}
//		try (Scanner scanner = new Scanner(httpClient.getInputStream())) {
	}
	
	/**
	 * Sets the phase of uws job
	 * @param uwsJob
	 * @throws IOException
	 * @throws URISyntaxException 
	 */
	public static void getsetPhase(UWSJob uwsJob) throws IOException, URISyntaxException {
		URL phaseUrl = TapManager.getUrl(uwsJob.getLocation().toString(), null, PATHPHASE);
//		new URL(uwsJob.getLocation().toString() + "/phase");
		StringBuffer result = getResponse(phaseUrl);
		if (result != null) {
			uwsJob.setCurrentPhase(result.toString().toUpperCase());
		}
	}
	
	/**
	 * Method deletes UWS job.
	 * @param job
	 * @throws IOException, Exception
	 */
	public void deleteJob(UWSJob job, boolean updateModelAndGui) throws IOException, Exception {
		HttpURLConnection httpConn = null;
		try {
			URLConnection conn = job.getLocation().openConnection();
			if (conn instanceof HttpURLConnection) {
				httpConn = (HttpURLConnection) conn;
				httpConn.setInstanceFollowRedirects(false);
				httpConn.setRequestMethod("DELETE");
				httpConn.setConnectTimeout(DELETETIMEOUTTIME);
				httpConn.connect();
				if (httpConn.getResponseCode() == HttpURLConnection.HTTP_SEE_OTHER) {
					if (updateModelAndGui) {
						removeJobUpdateGui(job);
					}
				} else {
					if (httpConn.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND) {
						int option = JOptionPane.showConfirmDialog(asyncPanel, UWSASKTOREMOVENOTFOUNDJOBS,
								UWSASKTOREMOVENOTFOUNDJOBSTITLE, JOptionPane.OK_CANCEL_OPTION);
						if (option == JOptionPane.OK_OPTION) {
							removeJobUpdateGui(job);
							return;
						}
					}
					StringBuffer errorMessage = new StringBuffer(JOBDELETEERRORMESSAGE);
					errorMessage.append("\n")
					.append(Util.handleErrorResponseForTapAndDL(job.getLocation(), httpConn));
					Aladin.trace(3, "Error when deleting job!"+errorMessage.toString());
					throw new IOException(errorMessage.toString());
				}
			}
		} catch (ProtocolException e) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace >=3 ) e.printStackTrace();
			throw e;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace >=3 ) e.printStackTrace();
			throw e;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if(Aladin.levelTrace >=3 ) e.printStackTrace();
			throw e;
		} finally {
			if (httpConn != null) {
				httpConn.disconnect();
			}
		}
	}
	
	/**
	 * sets the Uws job panel
	 * @return
	 */
	public MySplitPane instantiateGui() {
		if (this.jobControllerGui == null) {
			this.jobControllerGui = new FrameSimple(aladin);
		}
		if (asyncPanel == null) {
			JPanel topPanel = new JPanel(new GridBagLayout());
			
			if (inSessionAsyncJobsPanel == null) {
				inSessionAsyncJobsPanel = new JPanel();
				if (sessionUWSJobs == null) {
					JLabel infoLabel = new JLabel(UWSNOJOBMESSAGE);
					infoLabel.setFont(Aladin.LITALIC);
					inSessionAsyncJobsPanel.add(infoLabel);
				}
				inSessionAsyncJobsPanel.setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
				inSessionAsyncJobsPanel.setLayout(new BoxLayout(inSessionAsyncJobsPanel, BoxLayout.Y_AXIS));
			}
			radioGroup = new ButtonGroup();
			GridBagConstraints c = new GridBagConstraints();
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 1;
			c.weightx = 1;
			
			JScrollPane jobsScroll = new JScrollPane(inSessionAsyncJobsPanel);
			jobsScroll.setBackground(Aladin.BLUE);
			jobsScroll.getVerticalScrollBar().setUnitIncrement(5);
			jobsScroll.setMinimumSize(new Dimension(200, 100));
			c.weighty = 0.48;
//			c.anchor = GridBagConstraints.NONE;
			c.fill = GridBagConstraints.BOTH;
			c.insets = new Insets(4, 7, 0, 4);
			jobsScroll.setBorder(BorderFactory.createTitledBorder(UWSPANELCURRECTSESSIONTITLE));
			topPanel.add(jobsScroll,c);
			
			JPanel searchPanel = new JPanel();
			searchPanel.setBackground(Aladin.BLUE);
			searchPanel.setName("searchPanel");
			prevJobRadio = new JRadioButton("Job URL");
			searchPanel.setLayout(new BoxLayout(searchPanel, BoxLayout.X_AXIS));
			radioGroup.add(prevJobRadio);
			searchPanel.add(prevJobRadio);
			prevJobRadio.addActionListener(this);
			previousSessionJob = new JTextField(25);
			searchPanel.add(previousSessionJob);
			JButton button = new JButton("GO");
			button.setActionCommand(GETPREVIOUSSESSIONJOB);
			button.addActionListener(this);
			searchPanel.add(button);
			c.gridy++;
			c.weighty = 0.01;
			c.insets = new Insets(7, 7, 0, 4);
			c.fill = GridBagConstraints.HORIZONTAL;
//			c.anchor = GridBagConstraints.BASELINE;
			searchPanel.setBorder(BorderFactory.createTitledBorder(UWSPANELPREVIOUSSESSIONTITLE));
			topPanel.add(searchPanel,c);
			
			JPanel actionPanel = new JPanel();
			actionPanel.setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
			/*button = new JButton("START");
			button.setActionCommand(RUNJOB);
			button.addActionListener(this);
			actionPanel.add(button);*/
			loadResultsbutton = new JButton(STANDARDRESULTSLOAD);
			loadResultsbutton.setToolTipText(STANDARDRESULTSLOADTIP);
			loadResultsbutton.addActionListener(this);
			loadResultsbutton.setActionCommand(LOADDEFAULTTAPRESULT);
			loadResultsbutton.setVisible(false);
			actionPanel.add(loadResultsbutton);
			
			button = new JButton("ABORT");
			button.setActionCommand(ABORTJOB);
			button.addActionListener(this);
			actionPanel.add(button);
			button = new JButton("DELETE");
			button.setActionCommand(DELETEJOB);
			button.addActionListener(this);
			actionPanel.add(button);
			deleteOnExit = new JCheckBox(DELETEONCLOSEBUTTONLABEL);
			deleteOnExit.setActionCommand(DELETEONEXIT);
			deleteOnExit.addActionListener(this);
			deleteOnExit.setSelected(true);
			deleteOnExit.setVisible(false);
			actionPanel.add(deleteOnExit);
			c.gridy++;
			topPanel.add(actionPanel,c);
			
			this.jobDetails = new JPanel();
			jobsScroll = new JScrollPane(this.jobDetails); 
//			c.insets = new Insets(4, 7, 7, 4);
			jobsScroll.setBackground(Aladin.BLUE);
			this.jobDetails.setVisible(false);
			this.jobDetails.setBorder(BorderFactory.createTitledBorder("Job details:"));
//			c.gridy++;
//			c.weighty = 0.50;
//			c.fill = GridBagConstraints.BOTH;
//			asyncPanel.add(jobsScroll,c);
			
			asyncPanel = new MySplitPane(aladin, JSplitPane.VERTICAL_SPLIT, topPanel, jobsScroll, 1);
//			asyncPanel = new JPanel(new GridBagLayout());
			asyncPanel.setBackground(Aladin.COLOR_CONTROL_BACKGROUND);
			asyncPanel.setFont(Aladin.PLAIN);
			
		}
		return asyncPanel;
	}
	
	//TODO:: impl
	public UWSJob getQuote(URL url) {
		return null;
	}
	
	public void authenticate(){
		
	}
	
	public void getResults() {
		//cursor impl
	}
	
	/**
	 * Method gets the job with specified url
	 */
	public UWSJob getJobFromCache(String url) {
		UWSJob result = null;
		if (sessionUWSJobs!=null) {
			for (UWSJob uwsJob : sessionUWSJobs) {
				if (uwsJob.getLocation().toString().equalsIgnoreCase(url)) {
					result = uwsJob;
					break;
				}
			}
		}
		return result;
	}
	
	/**
	 * Method gets the selected job on frame
	 * @return
	 */
	public UWSJob getSelectedInSessionJob() {
		UWSJob selectedJob = null;
		for (UWSJob uwsJob : sessionUWSJobs) {
			if (uwsJob.gui.isSelected()) {
				selectedJob = uwsJob;
			}
		}
		return selectedJob;
	}
	
	/**
	 * Method usually called on exit to delete all asyn jobs set to delete by user
	 * Clean up method: won't bother with checking if job is still executing
	 */
	public void deleteAllSetToDeleteJobs() {
		// TODO Auto-generated method stub
		if (this.sessionUWSJobs != null) {
			for (UWSJob uwsJob : this.sessionUWSJobs) {
				if (uwsJob.isDeleteOnExit()) {
					try {
						deleteJob(uwsJob, false);
					} catch (Exception e) {
						// We do nothing on quit
					}
				}
			}
		}
	}
	
	/**
	 * loads the selected result(from the drop down by the user) of the job
	 * @param requestNumber 
	 * @param server 
	 * @param uwsJob
	 * @throws MalformedURLException
	 * @throws IOException
	 * @throws Exception
	 */
	public void loadResults(UWSJob uwsJob, String chosen, int requestNumber, Server server) throws MalformedURLException, IOException, Exception {
		if (UWSJob.COMPLETED.equals(uwsJob.getCurrentPhase())) {
			String resultsUrl = null;
			if (chosen != null) {
				resultsUrl = getResultsPath(uwsJob, chosen); //way to many looseends in trying to get the correct results path. But anyway the spec says it is mandatory to have /results/result
				if (resultsUrl == null ) {
					int option = JOptionPane.showConfirmDialog(asyncPanel, UWSASKLOADDEFAULTRESULTS);
					if (option == JOptionPane.OK_OPTION) {
						resultsUrl = uwsJob.getDefaultResultsUrl();
					}
				}
			} else {
				//try to load a single result 
				//if multiple ask user to choose..show message
				resultsUrl = uwsJob.getIfSingleResult();
				if (resultsUrl == null) {
					showAsyncPanel();
					Aladin.info(asyncPanel, UWSMULTIJOBLOADMESSAGE);
					return;
				} else {
					resultsUrl = getResultsPath(uwsJob, resultsUrl);
				}
			}
			URL urlToLoad = TapManager.getUrl(resultsUrl, null, null);
			try {
				TapManager.handleResponse(aladin, urlToLoad, null, uwsJob.getServerLabel(), server, uwsJob.getQuery(), requestNumber);
			} catch (IOException e) {
				// TODO: handle exception
				showAsyncPanel();
				throw e;
			}
//			aladin.calque.newPlan(resultsUrl, uwsJob.getServerLabel(), null);
		}
	}
	
	public String getResultsPath(UWSJob uwsJob, String resultToLoad) {
		String resultsUrlString = null;
		URL resultsUrl = null;
		if (resultToLoad.startsWith("http") || resultToLoad.startsWith("https")) {
			resultsUrl = validateUrlSimple(resultToLoad);
		} else {//if just path is provided and not full url: its path to be constructed from original url. we will assume located on the same server
			try {
				resultsUrl = new URL(uwsJob.getLocation().getProtocol(), uwsJob.getLocation().getHost(), uwsJob.getLocation().getPort(), URLDecoder.decode(resultToLoad, Constants.UTF8));
				resultsUrl = validateUrlSimple(resultsUrl);
				if (resultsUrl == null) {
					resultsUrl = new URL(uwsJob.getLocation().getProtocol(), uwsJob.getLocation().getHost(), uwsJob.getLocation().getPort(), resultToLoad);
					resultsUrl = validateUrlSimple(resultsUrl);
				}
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				Aladin.trace(3, e.getMessage());
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				Aladin.trace(3, e.getMessage());
			}
			//parse if relative path ::
			//server : http://heasarc.gsfc.nasa.gov/xamin/vo/tap/async/1479376281691_5
			//results path : /xamin/vo/tap/async/1479376281691_5/results/result
			//server base url : http://heasarc.gsfc.nasa.gov/xamin/vo/tap/
			
			//some server results arrive un encoded example:
			//result0=/skynode/do/tap/spcam/async/181112212721+0900_1184_107_9922775895291997506/results/result
		}
		if (resultsUrl != null) {
			resultsUrlString = resultsUrl.toString();
		}
		return resultsUrlString;
	}
	
	public URL validateUrlSimple(String resultToLoad) {
		URL resultsUrl = validateUrlSimple(resultToLoad, true);
		if (resultsUrl == null) {
			resultsUrl = validateUrlSimple(resultToLoad, false);
		}
		return resultsUrl;
	}
	
	public URL validateUrlSimple(String resultToLoad, boolean decode) {
		URL resultsUrl = null;
		try {
			if (decode) {
				resultsUrl = new URL(URLDecoder.decode(resultToLoad, Constants.UTF8));
			} else {
				resultsUrl = new URL(resultToLoad);
			}
			//if in application/x-www-form-urlencoded form
			// as in case of csirohttp%3A%2F%2Fatoavo.atnf.csiro.au%2Ftap%2Fasync%2F1488450523217A%2Fresults%2Fresult
			resultsUrl = validateUrlSimple(resultsUrl);
			if (resultsUrl != null && resultsUrl.getAuthority() == null) {
				Aladin.trace(3, "no authority " + resultsUrl);
				resultsUrl = null;
			}
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return resultsUrl;
	}
	
	public URL validateUrlSimple(URL resultsUrl) {
		try {
			if (resultsUrl != null) {
				resultsUrl.toURI();
				if (resultsUrl.getAuthority() == null) {
					Aladin.trace(3, "no authority " + resultsUrl);
					resultsUrl = null;
				}
			}
		} catch (URISyntaxException e) {
			Aladin.trace(3, "URISyntaxException when trying to decode"+e.getMessage());
			resultsUrl = null;
		}
		return resultsUrl;
	}
	
	
	public UWSJob processJobSelection(boolean loadJobSummary) throws Exception {
		UWSJob selectedJob = null;
		try {
			if (prevJobRadio.isSelected()) {
				if (previousSessionJob.getText().isEmpty()) {
					throw new Exception(NOJOBURLMESSAGE);
				} else {
					URL jobUrl = new URL(previousSessionJob.getText());
					selectedJob = getJobFromCache(jobUrl.toString());
					if (selectedJob == null) {
						selectedJob = new UWSJob(this, EMPTYSTRING, jobUrl);
						populateJob(Util.openStreamForTapAndDL(selectedJob.getLocation(), null, true, 10000), selectedJob);
					}
					if (loadJobSummary) {
						selectedJob.setJobDetailsPanel();
						if (sessionUWSJobs == null) {
							asyncPanel.revalidate();
							asyncPanel.repaint();
						}
					}
				}
			} else {
				selectedJob = getSelectedInSessionJob();
			}
			if (selectedJob == null) {
				throw new Exception(JOBNOTSELECTED);
			}
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3) e1.printStackTrace();
			throw e1;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			if (Aladin.levelTrace >= 3) e1.printStackTrace();
			throw new IOException(JOBNOTFOUNDGIVENURL+ " \n"+e1.getMessage());
		}
		return selectedJob;
	}
	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		// TODO Auto-generated method stub
		Object o = e.getSource();

	      // Affichage du selecteur de fichiers
		if (o instanceof JRadioButton) {
			//if it is a valid job which was previously parsed- load it.
			//if use asks to load then make it a job- by parsing the job
			if (prevJobRadio.isSelected()) {
				if (loadResultsbutton != null) {
					loadResultsbutton.setVisible(false);
				}
				if (!previousSessionJob.getText().isEmpty()) {
					try {
						URL jobUrl = new URL(previousSessionJob.getText());
						UWSJob selectedJob = getJobFromCache(jobUrl.toString());
						if (selectedJob != null) {
							selectedJob.setJobDetailsPanel();
							if (UWSJob.COMPLETED.equals(selectedJob.getCurrentPhase())) {
								loadResultsbutton.setVisible(true);
							}
							if (sessionUWSJobs == null) {
								asyncPanel.revalidate();
								asyncPanel.repaint();
							}
						}
					} catch (Exception e1) {
						//all suppressed if user just clicks on previous job button
					}
				}
				deleteOnExit.setVisible(false);
			}
		}	
		else if(o instanceof JButton) {
	    	  String action = ((JButton)o).getActionCommand();
	    	  if (action.equals(GETPREVIOUSSESSIONJOB)) {
	    		//this go button is for getting prev job. behavior : won't check if corresponding radio is selected or not, but directly selects it
				prevJobRadio.setSelected(true);
				deleteOnExit.setVisible(false);
				if (loadResultsbutton != null) {
					loadResultsbutton.setVisible(false);
				}
				try {
					UWSJob selectedJob = processJobSelection(true);
					if (selectedJob != null && UWSJob.COMPLETED.equals(selectedJob.getCurrentPhase())) {
						loadResultsbutton.setVisible(true);
					}
				} catch (Exception e1) {
					Aladin.error(asyncPanel, e1.getMessage());
				}
			} else if (action.equals(LOADDEFAULTTAPRESULT)) {
				try {
					UWSJob selectedJob = processJobSelection(true);
					String resultsUrl = selectedJob.getDefaultResultsUrl();
					loadResults(selectedJob, resultsUrl, -1, null);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					Aladin.error(asyncPanel, "Error in processing results url!");
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					Aladin.error(asyncPanel, "Unable to get the job information, please try again!");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.error(asyncPanel, e1.getMessage());
				}
			} else if (action.equals(DELETEJOB)) {
				UWSJob selectedJob = null;
				try {
					selectedJob = processJobSelection(false);
					deleteJob(selectedJob, true);
					if (prevJobRadio.isSelected()) {
						Aladin.info(asyncPanel, selectedJob.getJobId()+" -job sucessfully deleted");
					}
					selectedJob.resetStatusOnServer();
				} catch (ProtocolException e1) {
					Aladin.error(asyncPanel, e1.getMessage());
					if (selectedJob != null) {
						selectedJob.showErrorOnServer();
					}
				} catch (IOException e1) {
					Aladin.error(asyncPanel, e1.getMessage());
					if (selectedJob != null) {
						selectedJob.showErrorOnServer();
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.error(asyncPanel, e1.getMessage());
					if (selectedJob != null) {
						selectedJob.showErrorOnServer();
					}
				}
			}
			 else if (action.equals(ABORTJOB)) {
				UWSJob selectedJob = null;
				try {
					selectedJob = processJobSelection(false);
					if (selectedJob.canAbort()) {
						selectedJob.abortJob();
						selectedJob.resetStatusOnServer();
						if (prevJobRadio.isSelected()) {
							selectedJob.updateGui(null);
							Aladin.info(asyncPanel, selectedJob.getJobId()+" -job sucessfully aborted");
						}
					} else {
						Aladin.error(asyncPanel, "Cannot abort job when phase is: "+selectedJob.getCurrentPhase());
						return;
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					if (selectedJob != null) {
						selectedJob.showErrorOnServer();
					} 
					Aladin.trace(3, e1.getMessage());
					Aladin.error(asyncPanel, e1.getMessage());
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.error(asyncPanel, e1.getMessage());
					if (selectedJob != null) {
						selectedJob.showErrorOnServer();
					}
				}
			} /*else if (action.equals(RUNJOB)) {
				UWSJob selectedJob = null;
				try {
					selectedJob = processJobSelection(false);
					if (selectedJob.canRun()) {
						selectedJob.run();
						if (prevJobRadio.isSelected()) {
							selectedJob.updateGui(null);
							if (selectedJob.getCurrentPhase().equalsIgnoreCase("")) {
								Aladin.info(asyncPanel, selectedJob.getJobId()+" -job sucessfully started");
							}
						}
					} else {
						Aladin.warning(asyncPanel, "Cannot start job when phase is: "+selectedJob.getCurrentPhase());
						return;
					}
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
					if (selectedJob != null) {
						Aladin.warning(asyncPanel, "Please try again. Error when aborting job: "+selectedJob.getJobId());
					} else {
						Aladin.warning(asyncPanel, e1.getMessage());
					}
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					Aladin.warning(asyncPanel, e1.getMessage());
				}
			}*/	    	  
		} else if (o instanceof JCheckBox) {//DELETEONEXIT action command not checked for now
			try {
				UWSJob selectedJob = processJobSelection(false);
				selectedJob.setDeleteOnExit(((JCheckBox)o).isSelected());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				Aladin.error(asyncPanel, e1.getMessage());
			}
		}
	}
	
	/**
	 * Method shows the async panel
	 * @throws Exception 
	 */
	public void showAsyncPanel(){
		MySplitPane asyncPanel = this.instantiateGui();
		this.jobControllerGui.show(asyncPanel, JOBCONTROLLERTITLE);
//		tapFrameServer.tabbedTapThings.setSelectedIndex(1);
//		tapFrameServer.setVisible(true);
		this.jobControllerGui.toFront();
	}
	
}
