package cds.allsky;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;

import cds.aladin.Aladin;
import cds.aladin.Chaine;
import cds.aladin.Plan;
import cds.aladin.PlanBG;
import cds.tools.Util;

public class TabRgb extends JPanel implements ActionListener {

	private static String B;
	private static String G;
	private static String R;
	
	private static final String OK = "Create RGB";
	
	private static final String txt = "Choose 2 or 3 opened Allsky views to combine and create a colored one.";

	private String REP_DEST;
	private String RGBALLSKY;
	private String BROWSE;
	private String HELP;

	private JLabel		dirLabel = new JLabel();
	private JButton 	browse = new JButton();
	private JTextField 	dir = new JTextField(30);
	
	JProgressBar progressBar = new JProgressBar(0,100);

	private JComboBox[] ch;
	private JButton bOk = new JButton(OK);
	private JButton bHelp = new JButton();
	private final Aladin aladin;
	private String titlehelp;

	public TabRgb(Aladin aladin) {
		super(new BorderLayout());
		this.aladin = aladin;
		createChaine(Aladin.getChaine());
		bHelp = Util.getHelpButton(this,HELP);
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill=GridBagConstraints.BOTH;
		c.insets = new Insets(2,2,2,2);

		JPanel pCenter = new JPanel();
		pCenter.setLayout(new GridBagLayout());
		pCenter.setBorder(BorderFactory.createEmptyBorder(5, 55, 5,55));
		
		JLabel info = new JLabel(Util.fold(RGBALLSKY,80,true));
		info.setFont(info.getFont().deriveFont(Font.ITALIC));
		c.gridwidth=GridBagConstraints.REMAINDER;
		c.insets.bottom=20;
		pCenter.add(info,c);
		
		// Cr�ation des lignes pour choisir les plans
		c.insets.bottom=2;
		int n=3;
		ch=new JComboBox[n];
		for (int i=0; i<n; i++) {
			ch[i]=new JComboBox();
			ch[i].setPreferredSize(new Dimension(200,20));

			JLabel ll=new JLabel(getLabelSelector(i));
			ll.setForeground(getColorLabel(i));

			c.gridwidth=GridBagConstraints.RELATIVE;
			c.weightx=0.0;
			pCenter.add(ll,c);
			c.gridwidth=GridBagConstraints.REMAINDER;
//			c.weightx=10.0;
			pCenter.add(ch[i],c);
			
			ch[i].addFocusListener(new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent e) {
					init();
				}
			});
		}
				
		init();
		c.gridwidth=GridBagConstraints.RELATIVE;
		c.weightx=0;
		
		// S�lection du r�pertoire destination
		JPanel dirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));
		dirLabel = new JLabel(REP_DEST);
		pCenter.add(dirLabel,c);
   		browse.setText(BROWSE);
		browse.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { dirBrowser(dir); }
		});
		dirPanel.add(dir);
		dirPanel.add(browse);
		c.gridwidth=GridBagConstraints.REMAINDER;
		pCenter.add(dirPanel,c);
		
//		c.gridx=GridBagConstraints.RELATIVE; c.gridy=GridBagConstraints.RELATIVE;
		c.gridwidth=GridBagConstraints.REMAINDER;
		c.fill=GridBagConstraints.HORIZONTAL;
		
		// bouton OK
		bOk.setEnabled(false);
		bOk.addActionListener(this);
		pCenter.add(bOk,c);
		
		// barre de progression
		progressBar.setStringPainted(true);
		c.insets.top=70;
		pCenter.add(progressBar,c);

		JPanel fin = new JPanel(new BorderLayout());
		JPanel pBtn = new JPanel();
		pBtn.setLayout(new BoxLayout(pBtn, BoxLayout.X_AXIS));
		pBtn.add(Box.createHorizontalGlue());
		pBtn.add(bOk);
		pBtn.add(Box.createRigidArea(new Dimension(10,0)));
		pBtn.add(Box.createHorizontalGlue());
		fin.add(pBtn, BorderLayout.CENTER);
		fin.add(bHelp, BorderLayout.EAST);
		
		// composition du panel principal
		add(pCenter, BorderLayout.CENTER);
		add(fin, BorderLayout.SOUTH);
        setBorder( BorderFactory.createEmptyBorder(5, 5, 5, 5));
	}
	protected String getLabelSelector(int i) {
		return i == 0?R:i == 1?G:i==2?B:"";
	}

	protected Color getColorLabel(int i) {
		return i == 0?Color.red:i == 1?Color.green:i==2?Color.blue:Color.black;
	}
	

	private void createChaine(Chaine chaine) {
		BROWSE = chaine.getString("FILEBROWSE");
		REP_DEST = chaine.getString("REPDALLSKY");
		RGBALLSKY = chaine.getString("RGBALLSKY");
//		HELP = txt;//chaine.getString("ALLSKYRGBHELP");
		HELP = chaine.getString("HELPRGBALLSKY");
		titlehelp = chaine.getString("HHELP");
        R = chaine.getString("RGBRED");
        G = chaine.getString("RGBGREEN");
        B = chaine.getString("RGBBLUE");
	}
	
	/** Recupere la liste des plans Allsky valides */
	protected PlanBG[] getPlan() {
		try {
         Vector<Plan> v = aladin.calque.getPlanBG();
         if( v==null ) return new PlanBG[0];
         // enl�ve les plans d�j� couleur
         for (Iterator<Plan> iterator = v.iterator(); iterator.hasNext();) {
         	PlanBG plan = (PlanBG) iterator.next();
         	
         	if (plan.isColored())
         		v.remove(plan);
         }
         PlanBG pi [] = new PlanBG[v.size()];
         v.copyInto(pi);
         return pi;
      } catch( Exception e ) {
        return new PlanBG[]{};
      }
	}

	public void setProgress(int value) {
		progressBar.setValue(value);
	}
		
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand() == OK) {
			// r�cup�re les 2 ou 3 plans s�lectionn�s
			Object[] plans = new Object[3];
			for (int i=0; i<3; i++) {
				plans[i] = ch[i].getSelectedItem();
			}
			
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			bOk.setEnabled(false);
			BuilderRgb builderRgb = new BuilderRgb(aladin,plans,dir.getText());
			builderRgb.start();
			(new ThreadProgressBar(builderRgb)).start();
		}
	}

	/** Ouverture de la fen�tre de s�lection d'un fichier */
	private void dirBrowser(JTextField dir) {
	   String path = dir.getText().trim();
	   if( path.length()==0 ) path = aladin.getDefaultDirectory();
	   String t = Util.dirBrowser(this,path);
	   if( t==null ) return;
	   dir.setText(t);
	   actionPerformed(new ActionEvent(dir,-1, "dirBrowser Action"));
	}

	void init() {
		// sauvegarde les anciennes selections
		Object[] save = new Object[]{ch[0].getSelectedItem(),
		ch[1].getSelectedItem(),
		ch[2].getSelectedItem()};
		// rachaichit les combo box avec la liste des plans allsky
		PlanBG[] plans = getPlan();
		for (int i=0; i<3; i++) {
			ch[i].removeAllItems();
			ch[i].addItem(null);
		}
		for (PlanBG planBG : plans) {
			for (int i=0; i<3; i++) {
				ch[i].addItem(planBG);
				// remet l'ancienne selection
				if (save[i]!=null && planBG==save[i])
					ch[i].setSelectedItem(planBG);
					
			}
		}
		if (save[0]!=null || save[1]!=null || save[2]!=null)
			bOk.setEnabled(true);
	}
	
	public void setStartEnabled(boolean b) {
		bOk.setEnabled(b);
	}

	public void help() {
		JOptionPane.showMessageDialog(this, HELP, titlehelp,
				JOptionPane.INFORMATION_MESSAGE);
	}
	
	class ThreadProgressBar implements Runnable {
		Object rgbBuilder;
		public ThreadProgressBar(Object source) {
			rgbBuilder = source;
		}

		public synchronized void start(){
			// lance en arri�re plan le travail
			(new Thread(this)).start();
		}
		public void run() {
			int value = 0;
			while(rgbBuilder != null && value < 99) {
				value = (int)((BuilderRgb)rgbBuilder).getProgress();
				setProgress(value);
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
				}
			}
			setProgress(100);
			bOk.setEnabled(false);
			setCursor(null);
			
		}
	}
}