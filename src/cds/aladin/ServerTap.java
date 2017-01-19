package cds.aladin;

import static cds.aladin.Constants.ADDPOSCONSTRAINT;
import static cds.aladin.Constants.ADDWHERECONSTRAINT;
import static cds.aladin.Constants.CHANGESERVER;
import static cds.aladin.Constants.CHECKQUERY;
import static cds.aladin.Constants.CIRCLEORSQUARE;
import static cds.aladin.Constants.COMMA_SPACECHAR;
import static cds.aladin.Constants.EMPTYSTRING;
import static cds.aladin.Constants.OPEN_SET_RADEC;
import static cds.aladin.Constants.REGEX_TABLENAME_SPECIALCHAR;
import static cds.aladin.Constants.REMOVEWHERECONSTRAINT;
import static cds.aladin.Constants.SELECTALL;
import static cds.aladin.Constants.SHOWAYNCJOBS;
import static cds.aladin.Constants.SPACESTRING;
import static cds.aladin.Constants.SYNC_ASYNC;
import static cds.aladin.Constants.TABLECHANGED;
import static cds.aladin.Constants.TAP_REC_LIMIT;
import static cds.aladin.Constants.TARGETNAN;
import static cds.aladin.Constants.UPLOAD;
import static cds.aladin.Constants.WRITEQUERY;
import static cds.tools.CDSConstants.BOLD;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import adql.db.DBChecker;
import adql.db.DBType;
import adql.db.DBType.DBDatatype;
import adql.db.DefaultDBColumn;
import adql.db.DefaultDBTable;
import adql.parser.ADQLParser;
import adql.parser.QueryChecker;
import adql.query.ADQLQuery;
import cds.tools.Util;
import cds.xml.VOSICapabilitiesReader;

public class ServerTap extends Server implements MouseListener{

	private static final long serialVersionUID = 1L;
	String ERR,LOAD,TAPTABLEJOINTIP, TAPTABLEUPLOADTIP, TARGETERROR, TIPCLICKTOADD, TAPTABLENOUPLOADTIP;
	private String name;
	private String url;
	protected TapManager tapManager = null;
	
	public Future<JPanel> infoPanel;
	static final int DEFAULT_INFO_TABLE_HEIGHT = 115;
	JComboBox tablesGui;
	JList selectList;
	JCheckBox selectAll;
	JPanel whereClausesPanel;
	List<ColumnConstraint> whereClauses;
	Map<String, TapTable> tablesMetaData;
	String selectedTableName;
	JComboBox<Integer> limit;
	JComboBox<String> sync_async;
	JComboBox<String> circleOrSquare;
	JPanel queryComponentsGui;
	private String raColumnName;
	private String decColumnName;
	JPanel targetPanel;
	public static Map<String, DBDatatype> DBDATATYPES = new HashMap();
	Hashtable adqlWhere = new Hashtable();
    Map<String, String> adqlElements;
    List<DefaultDBTable> queryCheckerTables;
    protected boolean isUploadServer;
    Color primaryColor = Aladin.BLUE;
    Color secondColor = Aladin.BACKGROUND;
    protected boolean isNotLoaded;
    protected boolean dummyInstantiation;
    Future<VOSICapabilitiesReader> capabilities;
    JFrame setRaDecFrame;
    
	static{
		for (DBDatatype dbDatatype : DBDatatype.values()) {
			DBDATATYPES.put(dbDatatype.name(), dbDatatype);
		}
	}

	protected ServerTap(Aladin aladin) {
	  this.aladin = aladin;
	  isNotLoaded = true;
      createChaine();
      type = CATALOG;
      aladinLabel="TAP";
      this.adqlParser = new ADQLParser();
      this.tapManager = TapManager.getInstance(aladin);
   }
	
	protected ServerTap(Aladin aladin, boolean dummyInstantiation){
		this(aladin);
		this.dummyInstantiation = true;
	}
	
	protected void createFormDefault() {
		this.createForm(0);
	}
	
	/**
	 * Creation of the tap query panel
	 * @param tablesMetaData
	 * @param tableChoice
	 */
	protected void createForm(int tableChoice) {
		Vector<String> tables = new Vector<String>(this.tablesMetaData.keySet().size());
		tables.addAll(this.tablesMetaData.keySet());
		selectedTableName = tables.get(tableChoice);
		Vector<TapTableColumn> columnNames = this.tablesMetaData.get(selectedTableName).getColumns();
		if (columnNames == null) {
			if (isUploadServer) {
				Aladin.warning("Error in uploaded data");
				return;
			}
			try {
				columnNames = tapManager.updateTableColumnSchemas(selectedTableName, this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(e.getMessage());
				return;
			}
			if (columnNames == null) {
				Aladin.warning("Error in updating the metadata for :"+selectedTableName);
				return;
			}
		}
		setQueryChecker(this.selectedTableName);
		
		JButton button;
		type = CATALOG;

		setLayout(new BorderLayout());
		setOpaque(true);
		this.setBackground(this.primaryColor);
		setFont(Aladin.PLAIN);
		
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		
		JPanel containerPanel = new JPanel(new GridBagLayout());
		containerPanel.setBackground(this.primaryColor);
	    
		GridBagConstraints c = new GridBagConstraints();
		c.gridy = 0;
		
		JPanel titlePanel = new JPanel();
		titlePanel.setBackground(this.primaryColor);
		titlePanel.setAlignmentY(SwingConstants.CENTER);
		if (!isUploadServer) {
			makeTitle(titlePanel, this.name);
//			this.aladinLabel = this.name;
		} else {
			makeTitle(titlePanel, "Upload server");
		}
		
	    c.fill = GridBagConstraints.HORIZONTAL;
	    c.anchor = GridBagConstraints.CENTER;
	    c.gridx = 0;
	    c.weighty = 0.02;
		containerPanel.add(titlePanel, c);

		if (!isUploadServer) {
		button = new JButton("Change server");
		button.setActionCommand(CHANGESERVER);
		button.addActionListener(this);
		titlePanel.add(button);
		}
		
		// Premiere indication
		JLabel info1 = new JLabel(description);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		c.gridy++;
	    c.weighty = 0.02;
	    info1.setHorizontalAlignment(SwingConstants.CENTER);
	    containerPanel.add(info1, c);
	    c.gridy++;
	    
	    
		JPanel tablesPanel = getTablesPanel(tableChoice, tables);
		tablesPanel.setBackground(this.primaryColor);
		tablesPanel.setFont(BOLD);
		c.weighty = 0.02;
        c.anchor = GridBagConstraints.LINE_START;
        c.weightx = 0.10;
	    c.fill = GridBagConstraints.NONE;
	    c.gridx = 0;
	    c.insets = new Insets(0, 0, 0, 0);
	    containerPanel.add(tablesPanel, c);		
		
	    this.queryComponentsGui = new JPanel();
		setWhereAddConstraintsGui(columnNames);
		this.queryComponentsGui.setBackground(this.primaryColor);
		this.queryComponentsGui.setFont(BOLD);
		c.weighty = 0.51;
		c.weightx = 0.40;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(this.queryComponentsGui, c);	
		
		JPanel linePanel = getBottomPanel();
		c.weightx = 0.10;
		c.weighty = 0.02;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(linePanel, c);
	    
	    
	    JScrollPane scrolley = null;
		tap = new JTextArea(8, 100);//"", 8, 50
		tap.setFont(Aladin.ITALIC);
		tap.setWrapStyleWord(true);
		tap.setLineWrap(true);
		tap.setEditable(true);
		scrolley = new JScrollPane(tap);
		c.weightx = 0.35;
		c.weighty = 0.35;
	    c.fill = GridBagConstraints.BOTH;
	    c.gridy++;
	    containerPanel.add(scrolley, c);	
	    this.removeAll();
	    add(containerPanel);
	    
	    isNotLoaded = false;
	    writeQuery();
	}
	
	
	protected void changeTableSelection(int tableChoice) {
		Vector<String> tables = new Vector<String>(this.tablesMetaData.keySet().size());
		tables.addAll(this.tablesMetaData.keySet());
		selectedTableName = tables.get(tableChoice);
		Vector<TapTableColumn> columnNames = this.tablesMetaData.get(selectedTableName).getColumns();
		if (columnNames == null) {
			if (isUploadServer) {
				Aladin.warning("Error in uploaded data");
				return;
			}
			try {
				columnNames = tapManager.updateTableColumnSchemas(selectedTableName, this);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				Aladin.warning(e.getMessage());
				return;
			}
			if (columnNames == null) {
				Aladin.warning("Error in updating the metadata for :"+selectedTableName);
				return;
			}
		}
		setQueryChecker(this.selectedTableName);
		
		
		boolean targetPanelPresent = false;
		if (this.raColumnName!=null && this.decColumnName!=null) {
			targetPanelPresent = true;
		}

		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		
		if (targetPanelPresent) {
			if (this.raColumnName==null || this.decColumnName==null) {
				targetPanel.setVisible(false);
			}
		} else {
			if (this.raColumnName!=null && this.decColumnName!=null) {
				createTargetPanel();
			}
		}
		
		if (this.selectAll!=null) {
			this.selectAll.setSelected(true);
		}
		this.selectList.removeAll();
		this.selectList.setListData(columnNames);
		
	    
		resetFields();
		ball.setMode(Ball.UNKNOWN);
	    aladin.dialog.setDefaultParameters(aladin.dialog.getCurrent(),5);
	    isNotLoaded = false;
	    writeQuery();
	    
		this.revalidate();
		this.repaint();
		
	}
	
	/**
	 * Tap client gui in case when it is still loading
	 */
	protected void showloading() {
		this.setBackground(this.primaryColor);
		JLabel planeLabel = new JLabel("loading "+this.name+"...");
		planeLabel.setFont(Aladin.ITALIC);
		add(planeLabel,"Center");
		JButton button = new JButton("Open tap server list");
		button.setActionCommand(CHANGESERVER);
		button.addActionListener(this);
		add(button);
		isNotLoaded = true;
		revalidate();
		repaint();
	}
	
	/**
	 * Tap client gui in case of loading error
	 */
	protected void showLoadingError() {
		this.setBackground(this.primaryColor);
		this.removeAll();
		JLabel planeLabel = new JLabel("Error: unable to load "+this.name);
		planeLabel.setFont(Aladin.ITALIC);
		add(planeLabel,"Center");
		JButton button = new JButton("Open tap server list");
		button.setActionCommand(CHANGESERVER);
		button.addActionListener(this);
		isNotLoaded = true;
		add(button);
		revalidate();
		repaint();
	}
	
	/**
	 * Creates the table selection panel with tables drop-down, upload button etc..
	 * @param tableChoice
	 * @param tables
	 * @return
	 */
	public JPanel getTablesPanel(int tableChoice, Vector<String> tables) {
    	JPanel tablesPanel = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		tablesPanel.setLayout(gridbag);
		tablesPanel.setFont(BOLD);
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.insets = new Insets(1, 2, 1, 3);
		c.anchor = GridBagConstraints.WEST;
		c.fill = GridBagConstraints.WEST;
		c.weightx = 0.05;
		JLabel label = new JLabel();
		label.setText("Table:");
		label.setFont(BOLD);
		tablesPanel.add(label, c);

		tablesGui = new JComboBox(tables);
		tablesGui.setOpaque(false);
		tablesGui.setName("table");
		tablesGui.setActionCommand(TABLECHANGED);
		tablesGui.setSelectedIndex(tableChoice);
		tablesGui.addActionListener(this);
		tablesGui.setAlignmentY(SwingConstants.CENTER);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx++;
		c.weightx = 0.75;
		tablesPanel.add(tablesGui, c);
		
		JButton button = new JButton("Set ra, dec");
		button.setActionCommand(OPEN_SET_RADEC);
		button.addActionListener(this);
		c.weightx = 0.10;
		c.gridx++;
		tablesPanel.add(button);

		button = new JButton("Join");
		c.weightx = 0.05;
		c.gridx++;
		tablesPanel.add(button, c);
		button.setEnabled(false);
		button.addActionListener(this);
		button.setToolTipText(TAPTABLEJOINTIP);
		
		if (!isUploadServer) {
			String uploadTipText = TAPTABLEUPLOADTIP;
			button = new JButton("Upload");
			button.setActionCommand(UPLOAD);
			if (this.capabilities!=null) {
				try {
					VOSICapabilitiesReader meta = this.capabilities.get();
					button.setEnabled(meta.isUploadAllowed());
					c.weightx = 0.05;
					c.gridx++;
					button.addActionListener(this);
					if (meta.isUploadAllowed() && meta.getUploadHardLimit()!=0L) {
						String tip= String.format("Hard limit =%1$s rows", meta.getUploadHardLimit());
						uploadTipText = uploadTipText.concat(tip);
					} else if (!meta.isUploadAllowed()) {
						uploadTipText = TAPTABLENOUPLOADTIP;
					}
					button.setToolTipText(uploadTipText);
					tablesPanel.add(button, c);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if( Aladin.levelTrace >= 3 ) e.printStackTrace();//Do nothing, no upload button will be added
				}
			}
		}
		
		return tablesPanel;
	}
	
	/**
	 * Creates where constraints panel. Displays columns, where constraints addition interface(Column and position constraints)
	 * @param columnNames
	 */
	public void setWhereAddConstraintsGui(Vector<TapTableColumn> columnNames) {
		// TODO Auto-generated method stub
		this.queryComponentsGui.removeAll();
		GridBagLayout gridbag = new GridBagLayout();
		this.queryComponentsGui.setLayout(gridbag);
		this.queryComponentsGui.setBackground(this.primaryColor);
		
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(Aladin.BACKGROUND);
		JLabel label = new JLabel("Select:");
		label.setFont(BOLD);
		panel.add(label);
		
		this.selectAll = new JCheckBox("All", true);
		this.selectAll.setActionCommand(SELECTALL);
		this.selectAll.addActionListener(this);
		panel.add(selectAll);
		
		if (columnNames != null) {
			this.selectList = new JList(columnNames);
			this.selectList.setSelectionInterval(0, columnNames.size()-1);
			this.selectList.setCellRenderer(new TapTableColumnRenderer());
			this.selectList.addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					// TODO Auto-generated method stub
					if (selectList.getSelectedValuesList().size() != selectList.getModel().getSize()) {
						selectAll.setSelected(false);
					} else {
						selectAll.setSelected(true);
					}
					writeQuery();
				}
			});
		}
		
		
		JScrollPane scrolley = new JScrollPane(this.selectList);
		this.selectList.setVisibleRowCount(4);
		this.selectList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.weightx = 0.20;
		c.weighty = 0.02;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(0, 0, 0, 0);
		this.queryComponentsGui.add(panel, c);
		
		c.gridy = 1;
		c.weighty = 0.98;
		c.gridheight = 2;
		this.queryComponentsGui.add(scrolley, c);
		
		c.gridy = 0;
		c.gridx = 1;
		c.weightx = 0.80;
		c.weighty = 0.02;
		c.gridheight = 1;
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.setBackground(this.secondColor);
		label = new JLabel("Constraints:");
		label.setFont(BOLD);
		panel.add(label);
		
		JButton button = new JButton("Add new");
		button.setActionCommand(ADDWHERECONSTRAINT);
		button.addActionListener(this);
		panel.add(button);
		
		panel.add(Box.createHorizontalStrut(15));
		
		label = new JLabel("Max rows:");
		label.setFont(BOLD);
		panel.add(label);
		
		this.limit = new JComboBox<Integer>(TAP_REC_LIMIT);
		this.limit.setOpaque(false);
		this.limit.addItemListener(new ItemListener() {
	         public void itemStateChanged(ItemEvent e) {
	        	 writeQuery();
	          }
	       });
		panel.add(this.limit);
		
		this.queryComponentsGui.add(panel, c);
		
		c.gridy = 1;
		c.weighty = 0.98;
		
		targetPanel = new JPanel();
		targetPanel.setBackground(this.primaryColor);
		if (this.raColumnName!=null && this.decColumnName!=null) {
			createTargetPanel();// TAPALADIN
		} else {
			targetPanel.setVisible(false);
		}
		c.weighty = 0.20;
		c.gridheight = 1;
		this.queryComponentsGui.add(targetPanel, c);
		c.gridy = 2;
		c.weighty = 0.78;
		
		this.whereClausesPanel = new JPanel();
		this.whereClausesPanel.setLayout(new BoxLayout(this.whereClausesPanel, BoxLayout.Y_AXIS));
		this.whereClausesPanel.setBackground(this.secondColor);
		this.whereClausesPanel.setName("WHERECLAUSES");
		
		JScrollPane constraintsScroller = new JScrollPane();
		constraintsScroller.setViewportView(this.whereClausesPanel);
		constraintsScroller.getVerticalScrollBar().setUnitIncrement(4);
		
		this.queryComponentsGui.add(constraintsScroller, c);
	}
	
	/**
	 * Lower buttons panel, just above the tap query text area
	 * @return
	 */
	public JPanel getBottomPanel() {
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		bottomPanel.setBackground(this.primaryColor);
		JButton button = new JButton("Generate query");
		button.setActionCommand("WRITEQUERY");
		button.addActionListener(this);
		bottomPanel.add(button);
		
		button = new JButton("Check..");
		button.setActionCommand("CHECKQUERY");
		button.addActionListener(this);
		bottomPanel.add(button);
		
		this.sync_async = new JComboBox<String>(SYNC_ASYNC);
		this.sync_async.setOpaque(false);
		bottomPanel.add(this.sync_async);
		
		button = new JButton("Async jobs>>");
		button.setActionCommand(SHOWAYNCJOBS);
		button.addActionListener(this);
		bottomPanel.add(button);
		
		return bottomPanel;
	}
	
	/**
	 * Creates the 
	 * @param targetPanel
	 */
	protected void createTargetPanel(){
		GridBagLayout gridbag = new GridBagLayout();
		this.targetPanel.setLayout(gridbag);
		this.targetPanel.setFont(BOLD);
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.insets = new Insets(1, 1, 1, 1);
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0.10;
		
		JLabel label;
//		targetLabel = label= new JLabel("");
//        resumeTargetLabel();
		label= new JLabel("Target");
        label.setFont(BOLD);
//        label.setSize(20, HAUT);
		gridbag.setConstraints(label, c);
		this.targetPanel.add(label);

		target = new JTextField(40);
        target.addKeyListener(this);
        target.addActionListener(this);
		c.gridx = 1;
		c.gridwidth = 2;
		c.weightx = 0.80;
		gridbag.setConstraints(target, c);
		this.targetPanel.add(target);

		grab = new JToggleButton("Grab");
		Insets m = grab.getMargin();
        grab.setMargin(new Insets(m.top,2,m.bottom,2));
        grab.setOpaque(false);
        grab.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
              aladin.dialog.startGrabIt();
              if (aladin.additionalServiceDialog!=null) {
            	  aladin.additionalServiceDialog.startGrabIt();
			}
              
           }
        });
        grab.setFont(Aladin.SBOLD);
        updateWidgets(aladin.dialog);
        if (this.aladinLabel.equalsIgnoreCase(Constants.DATALINK_CUTOUT_FORMLABEL)) {//TODO:tintin change this logic?
        	grab.setEnabled(true);//Default true for datalink forms
		}
        
        c.gridwidth = 1;
		c.weightx = 0.05;
		c.gridx = 3;
		gridbag.setConstraints(grab, c);
		this.targetPanel.add(grab);

		String radText=RAD;
		label= new JLabel(addDot(radText));
		label.setFont(Aladin.BOLD);
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridx = 0;
		c.weightx = 0.10;
		gridbag.setConstraints(label, c);
		this.targetPanel.add(label);
		
		radius = new JTextField(50);
		radius.addKeyListener(this);
		radius.addActionListener(this);
		c.gridx = 1;
		c.weightx = 0.80;
		gridbag.setConstraints(radius, c);
		this.targetPanel.add(radius);
		
		this.circleOrSquare = new JComboBox<String>(CIRCLEORSQUARE);
		this.circleOrSquare.setOpaque(false);
		this.circleOrSquare.setName("posConstraintShape");
//		this.circleOrSquare.setActionCommand(POSCONSTRAINTSHAPECHANGED);
		this.circleOrSquare.setSelectedIndex(0);
//		this.circleOrSquare.addActionListener(this);
		c.weightx = 0.05;
		c.gridx = 2;
		gridbag.setConstraints(this.circleOrSquare, c);
		this.targetPanel.add(this.circleOrSquare);
		
		JButton button = new JButton("Add");
		button.setActionCommand(ADDPOSCONSTRAINT);
		button.addActionListener(this);
		button.setToolTipText(TIPCLICKTOADD);
		c.weightx = 0.05;
		c.gridx = 3;
		gridbag.setConstraints(button, c);
		this.targetPanel.add(button);
		
		Util.toolTip(label, RADIUS_EX);
	    Util.toolTip(radius, RADIUS_EX);
	    
		modeCoo=RADEd; //just ra and dec
		modeRad=STRINGd;
		if( coo==null ) {
			coo = new JTextField[2];
	        coo[0] = new JTextField();
	        coo[1] = new JTextField();
		}
		if (rad==null) {
			rad = new JTextField[2];
	        rad[0] = new JTextField();
	        rad[1] = new JTextField();
		}
		this.targetPanel.setVisible(true);
	}

	/**
	 * Method that set the gui of a where contraint
	 * @param whereClause
	 * @param firstGridEle
	 * @param secondGridEle
	 * @param thirdGridEle
	 */
	public void addWhereConstraint(WhereGridConstraint whereConstraintPanel) {
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints gridBagConstraints = new GridBagConstraints();
		gridBagConstraints.gridwidth = 1;
		gridBagConstraints.weighty = 1;
		gridBagConstraints.anchor = GridBagConstraints.NORTH;
		gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
		gridBagConstraints.gridy = 0;
		
		//Col-- start
				
		//and/or option
		gridBagConstraints.gridx = 0;
		gridBagConstraints.weightx = 0.05;
		gridbag.setConstraints(whereConstraintPanel.andOrOperator, gridBagConstraints);

		//column names drop box or ra query
		gridBagConstraints.gridx = 1;
		gridBagConstraints.weightx = 0.45;
		gridbag.setConstraints(whereConstraintPanel.firstGridComponent, gridBagConstraints);
		
		//where operators or dec query
		gridBagConstraints.gridx = 2;
		gridBagConstraints.weightx = 0.05;
		gridbag.setConstraints(whereConstraintPanel.secondGridComponent, gridBagConstraints);
		
		//value for constraint or radius query
		gridBagConstraints.gridx = 3;
		gridBagConstraints.weightx = 0.42;
		gridbag.setConstraints(whereConstraintPanel.thirdGridComponent , gridBagConstraints);
		//whereClausesPanel.add(constraintValue);
		
		//remove button
		gridBagConstraints.gridx = 4;
		gridBagConstraints.weightx = 0.03;
		whereConstraintPanel.removeButton.addActionListener(this);
		gridbag.setConstraints(whereConstraintPanel.removeButton, gridBagConstraints);
		
		
		// Col-- end
		whereConstraintPanel.setBackground(Aladin.BACKGROUND);
		whereConstraintPanel.addWhereConstraints();
		this.whereClausesPanel.add(whereConstraintPanel, Component.TOP_ALIGNMENT);
		ColumnConstraint.removeFirstAndOrOperator(this.whereClausesPanel);
		
//		this.queryComponentsGui.setBounds(XTAB1, addConstraintY, XWIDTH, 250);
		revalidate();
		repaint();
	}
	
	@Override
	protected void showStatusReport() {
		if (aladin.frameInfoServer == null || !aladin.frameInfoServer.isOfTapServerType() || !aladin.frameInfoServer.getServer().equals(this)) {
			if (aladin.frameInfoServer != null) {
				aladin.frameInfoServer.dispose();
			}
			if (this.infoPanel != null) {// new server
				aladin.frameInfoServer = new FrameInfoServer(aladin, this.infoPanel);
			} else {// incase the table info is not populated or some issues..
				aladin.frameInfoServer = new FrameInfoServer(aladin);
			}
		} else if (aladin.frameInfoServer.isFlagUpdate()) {
				try {
					aladin.frameInfoServer.updateInfoPanel();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					if( Aladin.levelTrace >= 3 ) e.printStackTrace();
				}
			}
		aladin.frameInfoServer.show(this);
	}
	
	/**
	 * Method to update the frame info panel.
	 * 	if the old frame is visible the method reloads it.
	 * @param newInfoPanel
	 */
	protected void tackleFrameInfoServerUpdate(Future<JPanel> newInfoPanel) {
		try {
			if (this.infoPanel!=null) {
				if (this.infoPanel.isDone()) {
					JPanel infoPanel = this.infoPanel.get();
					FrameInfoServer frameInfoServer = (FrameInfoServer) SwingUtilities.getRoot(infoPanel);
					if (frameInfoServer!=null) {
						frameInfoServer.setAdditionalComponent(newInfoPanel);
						if (frameInfoServer.isVisible()) {
							frameInfoServer.updateInfoPanel();
							frameInfoServer.revalidate();
							frameInfoServer.repaint();
						} else {
							frameInfoServer.setFlagUpdate(true);
						}
					}
				} else {
					this.infoPanel.cancel(true);
				}
			}
			this.infoPanel = newInfoPanel;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			if( Aladin.levelTrace >= 3 ) e.printStackTrace();
		}

	}

	/**
	 * Method assembles the query from all the front end components.
	 */
	public void writeQuery() {
		try {
			StringBuffer queryFromGui = new StringBuffer();
			queryFromGui.append("SELECT TOP ").append(this.limit.getSelectedItem());
			if (this.selectAll.isSelected()) {
				queryFromGui.append(" *");
			} else {
				List<TapTableColumn> selectedColumns = (this.selectList.getSelectedValuesList());
				if (selectedColumns == null || selectedColumns.isEmpty()) {
					queryFromGui.append(" *");
				} else {
					queryFromGui.append(SPACESTRING);
					for (TapTableColumn selectedColumn : selectedColumns) {
						queryFromGui.append(selectedColumn.getColumn_name()).append(COMMA_SPACECHAR);
					}
				}
				
			}
			//queryFromGui.append(((List<TapTableColumn>) this.selectList.getSelectedValuesList()).toString().replaceAll("[\\[\\]]", ""))
			queryFromGui = new StringBuffer(queryFromGui.toString().trim().replaceAll(",$", EMPTYSTRING));
			queryFromGui.append(" FROM ")
			.append(getQueryPart(selectedTableName)).append(SPACESTRING);
			
			Component[] whereConstraints = this.whereClausesPanel.getComponents();
			if ( this.whereClausesPanel.getComponentCount()>0) {
				WhereGridConstraint whereConstraint;
				queryFromGui.append("WHERE ");
				for (int i = 0; i < whereConstraints.length; i++) {
					whereConstraint = (WhereGridConstraint) whereConstraints[i];
					queryFromGui.append(whereConstraint.getAdqlString());
				}
			}
			
			tap.setText(queryFromGui.toString());
			ball.setMode(Ball.OK);
		} catch (Exception e) {
			// TODO: handle exception
			Aladin.warning(this, e.getMessage());
            ball.setMode(Ball.NOK);
		}
		
	}
	
	/**
	 * Method only used for cases of table names with special chars.
	 * Adds double quote to the names
	 * @param queryPartInput
	 * @return
	 */
	public static String getQueryPart(String queryPartInput) {
		Pattern regex = Pattern.compile(REGEX_TABLENAME_SPECIALCHAR);
		Matcher matcher = regex.matcher(queryPartInput);
		if (matcher.find()){
			queryPartInput = Glu.doubleQuote(queryPartInput);
		}
		return queryPartInput;
	}
	
	@Override
	public ADQLQuery checkQuery(List<String> unrecognisedParams) {
		// TODO Auto-generated method stub
		ADQLQuery query = super.checkQuery(unrecognisedParams);
		if (query == null && unrecognisedParams!=null && !unrecognisedParams.isEmpty()) {
			for (String tableName : unrecognisedParams) {
				setQueryChecker(tableName);
			}
		}
		return super.checkQuery(new ArrayList<String>());
	}
	
	/**
	 * Method adds the database metadata to the parser
	 */
	public void setQueryChecker(String tableName) {
		DefaultDBTable table = new DefaultDBTable(tableName);
		DefaultDBTable queryCheckerTable = null;
		boolean updateQueryChecker = true;
		
		if (this.queryCheckerTables == null) {//initialise for the first time
			this.queryCheckerTables = new ArrayList<DefaultDBTable>();
		} else {
			for (DefaultDBTable defaultDBTable : this.queryCheckerTables) {//Check if table is existing
				if (defaultDBTable.getADQLName()!=null && defaultDBTable.getADQLName().equalsIgnoreCase(table.getADQLName())) {
					queryCheckerTable = defaultDBTable;
					break;
				}
			}
		}
		
		if (tablesMetaData.containsKey(tableName)) {//Get table metadata if we have 
			Vector<TapTableColumn> columns = tablesMetaData.get(tableName).getColumns();
			if (columns != null) {
				for (TapTableColumn tapTableColumn : columns) {
					DefaultDBColumn columnForParser = null;
					if (queryCheckerTable!=null) {
						columnForParser = (DefaultDBColumn) queryCheckerTable.getColumn(tapTableColumn.getColumn_name(), true);//check if queryparser has column info
						if (columnForParser!=null) {
							updateQueryChecker = false;// or just that columnForParser!=null..for all columns
							break;//all other column would be populated
						}
					} 
					columnForParser = new DefaultDBColumn(tapTableColumn.getColumn_name(), table); //if column info is not there then you do the below, populate checker from metainfo of column..
					if (tapTableColumn.getDatatype()!=null && !tapTableColumn.getDatatype().isEmpty()) {
						int offset = tapTableColumn.getDatatype().indexOf("adql:");
						if (offset!=-1 && offset+5<tapTableColumn.getDatatype().length()) {
							String datatype = tapTableColumn.getDatatype().substring(offset+5);
							if (DBDATATYPES.containsKey(datatype)) {
								DBDatatype dbDataType = DBDATATYPES.get(datatype);
								DBType type = null;
								if (tapTableColumn.getSize()!=0) {
									type = new DBType(dbDataType, tapTableColumn.getSize());
								} else {
									type = new DBType(dbDataType);
								}
								columnForParser.setDatatype(type);
							}
						}
					}
					table.addColumn(columnForParser);
				}
			}
			
			if (updateQueryChecker) {
				if (queryCheckerTable!=null) {
					this.queryCheckerTables.remove(queryCheckerTable);
				}
				this.queryCheckerTables.add(table);
				QueryChecker checker = new DBChecker(this.queryCheckerTables);
				this.adqlParser.setQueryChecker(checker);
			}
		}
		
	}
	
	@Override
	protected void clear() {
		if (this.sync_async!=null) {
			this.sync_async.setSelectedIndex(0);
		}
		if (this.limit!=null) {
			this.limit.setSelectedIndex(0);
		}
		if (this.selectAll!=null) {
			this.selectAll.setSelected(false);
		}
		
		boolean resetTargetPanel = false;
		if (this.raColumnName!=null && this.decColumnName!=null) {
			resetTargetPanel = true;
		}
		this.raColumnName = tablesMetaData.get(selectedTableName).getRaColumnName();
		this.decColumnName = tablesMetaData.get(selectedTableName).getDecColumnName();
		if (resetTargetPanel && (this.raColumnName!=null && this.decColumnName!=null)) {
			resetTargetPanel = false;
		}
		if (resetTargetPanel) {
			Vector<TapTableColumn> columnNames = this.tablesMetaData.get(this.selectedTableName).getColumns();
			setWhereAddConstraintsGui(columnNames);
			this.queryComponentsGui.revalidate();
			this.queryComponentsGui.repaint();
		}
		
		resetColumnSelection();
		resetFields();
		super.clear();
		this.revalidate();
		this.repaint();
	}
	
	/**
	 * Convenience method
	 */
	protected void resetFields() {
		if (this.whereClausesPanel != null) {
			this.whereClausesPanel.removeAll();
		}
		if (this.circleOrSquare != null) {
			this.circleOrSquare.setSelectedIndex(0);
		}
		if (this.tap != null) {
			this.tap.setText(EMPTYSTRING);
		}
		resetColumnSelection();
	};
	
	@Override
	protected void reset() {
		this.createFormDefault();
		if (this.selectAll!=null) {
			this.selectAll.setSelected(true);
		}
		resetFields();
		super.reset();
		this.revalidate();
		this.repaint();
	};
	
	/**
	 * Method sets/resets column selection based on selectAll selection
	 * repaint is <b>not</b> called implicitly
	 */
	protected void resetColumnSelection() {
		if (this.selectAll!=null) {
			if (this.selectAll.isSelected()) {
				int start = 0;
				int end = selectList.getModel().getSize() - 1;
				if (end >= 0) {
					selectList.setSelectionInterval(start, end);
				}
			} else {
				selectList.clearSelection();
			}
		}
	}
	
	/**
	 * Method fires sync or async submissions based on what is selected by user
	 * Incase of upload as file is posted, diff sync methods are followed
	 * @param requestParams
	 */
	public void submit(Map<String, Object> requestParams) {
	      //check again
		if (this.isNotLoaded) {
			return;
		}
		boolean sync = this.sync_async.getSelectedItem().equals("SYNC");
  	  	this.submitTapServerRequest(sync, requestParams, this.name, url);
	}
	
	@Override
	public void submit() {
		submit(null);//no request params
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {
		// TODO Auto-generated method stub
		super.actionPerformed(arg0);
		Object source = arg0.getSource();
		if (source instanceof JButton) {
			String action = ((JButton) source).getActionCommand();
			if (action.equals(CHANGESERVER)) {
				this.tapManager.showTapRegistryForm();
			} else if (action.equals(WRITEQUERY)) {
				this.writeQuery();
			} else if (action.equals(ADDWHERECONSTRAINT)) {
				Vector<TapTableColumn> columnMetaData = this.tablesMetaData.get(this.selectedTableName).getColumns();
				WhereGridConstraint columnConstraint = new ColumnConstraint(this, columnMetaData);
				addWhereConstraint(columnConstraint);
				writeQuery();
			} else if (action.equals(REMOVEWHERECONSTRAINT)) {
				JButton button = ((JButton) source);
		    	JPanel thisComponent = (JPanel) button.getParent();
		    	JPanel compUnderWorks = (JPanel) thisComponent.getParent();
		    	compUnderWorks.remove(thisComponent);
		    	ColumnConstraint.removeFirstAndOrOperator(this.whereClausesPanel);
		    	writeQuery();
		    	this.whereClausesPanel.revalidate();
		    	this.whereClausesPanel.repaint();
			} else if (action.equals(ADDPOSCONSTRAINT)) {
				String objet = null;
				if( target != null ) {
			         try {
			        	objet = resolveQueryField();
			            if( objet == null) throw new Exception(UNKNOWNOBJ);
			            ball.setMode(Ball.OK);
			         } catch( Exception e1 ) {
			            Aladin.warning(this, e1.getMessage());
			            ball.setMode(Ball.NOK);
			            return;
			         }
			      }
				if (objet.trim().equals(TARGETNAN)) {//resolve obj for empty field
					Aladin.warning(this, TARGETERROR);
		            ball.setMode(Ball.NOK);
		            return;
				}
				WhereGridConstraint positionConstraint = new PositionConstraint(this, coo[0].getText(), coo[1].getText(), rad[0].getText(), this.raColumnName, this.decColumnName);
				addWhereConstraint(positionConstraint);
				writeQuery();
			} else if (action.equals(CHECKQUERY)) {
				checkQueryFlagMessage(new ArrayList<String>());
			} else if (action.equals(UPLOAD)) {
				//disabled based on capability and if user has not created a table
				if (tapManager.uploadFrame == null) {
					tapManager.uploadFrame = new FrameUploadServer(aladin, this.url);
				}
				tapManager.uploadFrame.show(this);
				
			} else if (action.equals(OPEN_SET_RADEC)) {
	        	this.tapManager.setRaDecForTapServer(this, selectedTableName);
			} else if (action.equals(SHOWAYNCJOBS)) {
				this.tapManager.showAsyncPanel();
			}
		} else if (source instanceof JComboBox) {
			this.createForm(((JComboBox) source).getSelectedIndex());
//			this.changeTableSelection(((JComboBox) source).getSelectedIndex());
			//TODO:: tintin changeTableSelection will be the method to use. Work in progress!!
			revalidate();
			repaint();
			
		} else if(source instanceof JCheckBox){// check command- SELECTALL
			JCheckBox selectAll = (JCheckBox)source;
			String action = selectAll.getActionCommand();
			if (action.equals(SELECTALL)) {
				resetColumnSelection();
			}
			writeQuery();
			selectList.revalidate();
			selectList.repaint();
		}
	}
	
	protected void createChaine() {
		super.createChaine();
		aladinLabel = aladin.chaine.getString("TAPFORMNAME");
		description = aladin.chaine.getString("TAPFORMINFO");
		title = aladin.chaine.getString("TAPFORMTITLE");
		verboseDescr = aladin.chaine.getString("TAPFORMDESC");
		ERR = aladin.chaine.getString("TAPFORMERR");
		LOAD = aladin.chaine.getString("FSLOAD");
		TAPTABLEJOINTIP = aladin.chaine.getString("TAPTABLEJOINTIP");
		TAPTABLEUPLOADTIP = aladin.chaine.getString("TAPTABLEUPLOADTIP");
		TARGETERROR = aladin.chaine.getString("TARGETERROR");
		TIPCLICKTOADD = aladin.chaine.getString("TIPCLICKTOADD");
		TAPTABLENOUPLOADTIP = aladin.chaine.getString("TAPTABLENOUPLOADTIP");
	}
	
	public void setData(Map<String, TapTable> tablesMetaData) {
		// TODO Auto-generated method stub
		this.tablesMetaData = tablesMetaData;
	}

	public String getRaColumnName() {
		return raColumnName;
	}

	public void setRaColumnName(String raColumnName) {
		this.raColumnName = raColumnName;
	}

	public String getDecColumnName() {
		return decColumnName;
	}

	public void setDecColumnName(String decColumnName) {
		this.decColumnName = decColumnName;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setCapalilities() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		this.writeQuery();
	}
	
}
