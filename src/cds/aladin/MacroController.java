// Copyright 2010 - UDS/CNRS
// The Aladin program is distributed under the terms
// of the GNU General Public License version 3.
//
//This file is part of Aladin.
//
//    Aladin is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, version 3 of the License.
//
//    Aladin is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    The GNU General Public License is available in COPYING file
//    along with Aladin.
//


package cds.aladin;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.Document;

import cds.aladin.MacroModel.ParamTableModel;
import cds.tools.Util;





/**
* Controleur (lien entre vue et modèle) pour les macros
* (utilisation des scripts avec liste de paramètres)
*
* @author Thomas Boch [CDS]
*
* @version 0.91 : March 2008 : ajout logs d'utilisation 
*          0.9  : sept. 2006 - création
* @see cds.aladin.FrameMacro
* @see cds.aladin.MacroModel
*/
public class MacroController implements ActionListener, MouseMotionListener,
                                        MouseListener {
	// objet gérant le modèle de données représenté par la vue
	private MacroModel macroModel;
	
	private FrameMacro frameMacro;
	private Aladin a;
	
	public MacroController(FrameMacro frameMacro, Aladin a) {
		this.frameMacro = frameMacro;
		this.a = a;
		
		macroModel = new MacroModel(a);
	}
    
    /** Lit un fichier script et retourne la chaine correspodante */
    private String getScriptFromReader(BufferedReader reader) throws IOException {
        String script = "";
        String line;
        while( (line = reader.readLine()) != null ) script += line+"\n";
        reader.close();
        
        return script;
    }
	
    /** Charge des parametres provenant d'un BufferedReader */
    private void loadParams(BufferedReader reader) throws IOException {
        String line;
        StringTokenizer st;
        Vector list;
        String[] values;
        while( (line = reader.readLine() ) != null ) {
            list = new Vector();
            
            // ignore comments lines and blank lines
            if( line.trim().startsWith("#") || line.trim().length()==0 ) continue;
            
            st = new StringTokenizer(line, "\t,|");
            while( st.hasMoreTokens() ) list.add(st.nextElement());
            
            values = new String[list.size()];
            list.copyInto(values);
            macroModel.getParamTableModel().addRecord(values);
        }
        reader.close();
        macroModel.getParamTableModel().initTable();
    }
    
    /** Importation des paramètres à partir des coordonnées des objets sélectionnés
     * @author P.Fernique - sept 2010
     */
    private void importParams() {
       ParamTableModel ptm = macroModel.getParamTableModel();
       Vector<Plan> p = a.calque.getSelectedPlanes();
       if( p!=null && p.size()>0 ) {
          Vector<Plan> pa = new Vector<Plan>();
          for( int i=0; i<p.size(); i++ ) {
             Plan p2 = p.get(i);
             if( p2.isReady() && p2.isSimpleCatalog() ) pa.add(p2);
          }
          p = pa;
       }
          
       if( p==null || p.size()==0 ) {
          Plan p1 = a.calque.getFirstCatalog();
          if( p1!=null ) { p = new Vector<Plan>(); p.add(p1); }
       }
       if( p.size()==0 ) a.warning(frameMacro,a.chaine.getString("NEEDCAT"));

       ptm.reset();
       int n=0;
       Enumeration<Plan> e = p.elements();
       while( e.hasMoreElements() ) {
          Plan p1 = e.nextElement();
          if( !p1.isReady() || !p1.isCatalog() ) continue;
          Iterator<Obj> it = p1.iterator();
          while( it.hasNext() ) {
             Obj o = it.next();
             if( !(o instanceof Source) ) continue;
             Source s = (Source)o;
             ptm.addRecord( new String[] { (++n)+"" , s.id, a.localisation.J2000ToString(s.raj,s.dej), s.plan.label } );
          }
       }
       ptm.initTable();
    }
    
	// implementation de ActionListener
	public void actionPerformed(ActionEvent ae) {
		String command = ae.getActionCommand();
		
		// traitement du chargement d'un script
		if( command.equals(FrameMacro.LOAD_SCRIPT) ) {
			JFileChooser fChooser = getJFileChooser(FrameMacro.LOAD_SCRIPT);
			
			int retval = fChooser.showDialog(frameMacro, command);
			
			File file;
			if( retval==JFileChooser.APPROVE_OPTION && (file=fChooser.getSelectedFile())!=null ) {
                String script = "";
			    try {
			        script = getScriptFromReader(new BufferedReader(new FileReader(file)));
			  	}
			  	catch(IOException e) {a.warning(frameMacro,a.chaine.getString("FTIOERR")+" : "+e,1);return;}
			  	
				frameMacro.setScript(script);
			}
			
        }
        // traitement de la sauvegarde d'un filtre
        else if( command.equals(FrameMacro.SAVE_SCRIPT) ) {
			JFileChooser fChooser = getJFileChooser(FrameMacro.SAVE_SCRIPT);

			int retval = fChooser.showDialog(frameMacro, command);
			
			File file;
			if( retval==JFileChooser.APPROVE_OPTION && (file=fChooser.getSelectedFile())!=null ) {
				try {
					DataOutputStream out = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(file)));
						String script = frameMacro.getScriptText();
						if( !script.startsWith("#AJS") ) out.writeBytes("#AJS"+Util.CR);
		                out.writeBytes(script.replaceAll("\\n", Util.CR));
						out.close();
				}
				catch(IOException e) {a.warning(frameMacro,a.chaine.getString("FTIOERR")+" : "+e);}
			}
		}
		
		// traitement du chargement d'une liste de paramètres
		else if( command.equals(FrameMacro.LOAD_PARAMS) ) {
			JFileChooser fChooser = getJFileChooser(FrameMacro.LOAD_PARAMS);
			
			int retval = fChooser.showDialog(frameMacro, command);
			
			File file;
			if( retval==JFileChooser.APPROVE_OPTION && (file=fChooser.getSelectedFile())!=null ) {
				macroModel.getParamTableModel().reset();
				try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
					loadParams(reader);
			  	}
			  	catch(IOException e) {a.warning(frameMacro,a.chaine.getString("FTIOERR")+" : "+e,1);return;}
			}
        }
        
        // Traitement de l'importation des coordonnées des objets sélectionnés en tant que paramètres - PF sept 2010
        else if( command.equals(FrameMacro.IMPORT_PARAMS) ) {
           importParams();
        }
        
        // traitement de la sauvegarde d'une liste de paramètres
        else if( command.equals(FrameMacro.SAVE_PARAMS) ) {
			JFileChooser fChooser = getJFileChooser(FrameMacro.SAVE_PARAMS);
			
			int retval = fChooser.showDialog(frameMacro, command);
			
			File file;
			if( retval==JFileChooser.APPROVE_OPTION && (file=fChooser.getSelectedFile())!=null ) {
				try {
					DataOutputStream out = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(file)));
						Object[] records = macroModel.getParamTableModel().getRecords();
						String s;
						for( int i=0; i<records.length; i++ ) {
							s = records[i].toString();
							if( s.trim().length()>0 ) {
								out.writeBytes(records[i].toString());
								if( i<records.length-1 ) out.writeBytes(Util.CR);
							}
						}
						out.close();
				}
				catch(IOException e) {a.warning(frameMacro,a.chaine.getString("FTIOERR")+" : "+e);}
			}
		}
		
		// visualisation de l'aide
		else if( command.equals(FrameMacro.SEEHELP) ) {
			frameMacro.showHelp();
		}
        
        // chargement d'un exemple
        else if( command.equals(FrameMacro.LOADEX) ) {
            
            // on charge le script exemple
            frameMacro.setScript(a.chaine.getString("FMSCRIPTEX"));
            // on charge les paramètres exemple
            macroModel.getParamTableModel().reset();
            try {
                loadParams(new BufferedReader(new InputStreamReader(
                           new ByteArrayInputStream(a.chaine.getString("FMPARAMSEX").getBytes()))
                ));
            }
            catch(IOException ioe) {
                ioe.printStackTrace();
                a.warning("Can't load example parameters !");
            }
        }
		
		// ajout d'une colonne
		else if( command.equals(FrameMacro.ADD_COL) ) {
			macroModel.getParamTableModel().addEmptyCol();
		}
		
		// reset de la liste des params
		else if( command.equals(FrameMacro.CLEAR_PARAMS) ) {
			macroModel.getParamTableModel().reset();
			macroModel.getParamTableModel().initTable();
			macroModel.getParamTableModel().addEmptyCol();
		}
		
		// exécution du script, avec les paramètres courant (sélectionnés)
		else if( command.equals(FrameMacro.EXEC_CURRENT) ) {
			execCurrentParams();
		}
		
		// exécution du script, avec les prochains paramètres courants
		else if( command.equals(FrameMacro.EXEC_NEXT) ) {
			execNextParams();
		}
		
		// exécution du script, avec toute la liste des paramètres
		else if( command.equals(FrameMacro.EXEC_ALL) ) {
			execAllParams(0);
		}
		
		// exécution du script, avec toute la liste des paramètres
		else if( command.equals(FrameMacro.EXEC_ALL_FROM_CURRENT) ) {
			int row = frameMacro.getParamTable().getSelectedRow();
			execAllParams(row);
		}
		
		// arrêt de l'exécution en cours
		else if( command.equals(FrameMacro.STOP) ) {
			stopCurrentExec();
		}
		
		// suppression d'une ligne de paramètres
		else if( command.equals(FrameMacro.DELETE) ) {
			deleteSelectedRow();
		}
		
		// fermeture de la frame
		else if( command.equals(FrameMacro.CLOSE) ) {
			frameMacro.setVisible(false);
		}
		
	}
	
	/*** implementation de MouseMotionListener (pour le JTextPane contenant le script) ***/ 
	public void mouseDragged(MouseEvent e) {}
	
	public void mouseMoved(MouseEvent e) {
		JTextPane tp = (JTextPane)e.getSource();
		int idx = tp.viewToModel(e.getPoint());
//		frameMacro.mousePosInScript = idx;
		
		frameMacro.testIfColorLink(idx);
	}
	/*************************************************************************************/
	
	/*** implémentation de MouseListener (pour gestion du click dans JTextPane avec script) et du click droit dans la table ***/
	public void mouseClicked(MouseEvent e) {
	}
	public void mouseEntered(MouseEvent e) {}
	
	// pour forcer la fin de la mise en valeur des liens
	public void mouseExited(MouseEvent e) {
		frameMacro.testIfColorLink(-1);
	}
	
	public void mousePressed(MouseEvent e) {
		if( e.getSource() instanceof JTable && 
			    frameMacro.getParamTable().getSelectedRow()>=0 &&
			    e.isPopupTrigger() &&
				frameMacro.getParamTable().getRowCount()>1 ) {
			frameMacro.getPopup().show(e.getComponent(), e.getX(), e.getY());
		}
	}
	
	// affichage de l'aide correspondante
	public void mouseReleased(MouseEvent e) {
		String word = frameMacro.getCmdWordUnderMouse();
		if( word==null ) return;
		
		a.command.execHelpCmd(word);
	}
	
	/******************************************************************************************/
	
	/** interrompt l'exécution en cours
	 */
	private void stopCurrentExec() {
		if( execThread!=null && execThread.isAlive() ) {
			// tue un éventuel "sync" en cours
			a.command.killSync();
			stopThread = true;
			frameMacro.setEnabledStopBtn(false);
		}
	}
	
	/**
	 * supprime la ligne sélectionnée
	 *
	 */
	private void deleteSelectedRow() {
		int idx = frameMacro.getParamTable().getSelectedRow();
		if( idx<0 ) return;
		macroModel.getParamTableModel().deleteRecord(idx);
	}
	
	private Thread execThread;
	private boolean stopThread = false;
	/**
	 * lance l'exécution du script chargé, avec les params sélectionnés (et seulement ceux là)
	 *
	 */
	private synchronized void execCurrentParams() {
		if( execThread!=null && execThread.isAlive() ) {
			a.warning(frameMacro, "Previous execution not finished yet !", 1);
			return;
		}
        execThread = new Thread("AladinMacroExeParam") {
            public void run() {
            	frameMacro.setEnabledStopBtn(true);
            	stopThread = false;
            	
                frameMacro.hilightScriptLine(0);
            	a.command.execHelpCmd("off");
				// TODO : bloquer l'édition de la table pendant l'exécution ?
				String[] commands = getScriptCommands();
				Map map = getParamMapForRow(frameMacro.getParamTable().getSelectedRow());
				if( map==null ) return;
		
				execScript(commands, new Map[] {map}, null);
				
				frameMacro.hilightScriptLine(-1);
				frameMacro.setEnabledStopBtn(false);
		    }
		};
		execThread.start();
        a.log("macroController", "execCurrent");
	}
	
	private synchronized void execNextParams() {
		// de cette façon, si aucune ligne n'est sélectionnée, on prend la première
		int row = frameMacro.getParamTable().getSelectedRow() + 1;
		if( row<macroModel.getParamTableModel().getRowCount() ) {
			frameMacro.getParamTable().setRowSelectionInterval(row, row);
		}
		else {
			a.warning(frameMacro, "No next params row to execute, stop execution !", 1);
			return;
		}
		
		execCurrentParams();
	}
	
	/**
	 * lance l'exécution du script chargé, pour l'ensemble des params à partir de la ligne row
	 * @param row la ligne de params à partir de laquelle on commence
	 */
	private synchronized void execAllParams(final int row) {
		if( execThread!=null && execThread.isAlive() ) {
			
			// TODO : passer ces msgs en français aussi !
			a.warning(frameMacro, "Previous execution not finished yet !", 1);
			return;
		}
		// vérification de la validité de row
		if( row==-1 ) {
			a.warning(frameMacro, "No parameter row selected, stop execution !", 1);
			return;
		}
		
		frameMacro.getParamTable().setRowSelectionInterval(row, row);
		
        execThread = new Thread("AladinMacroExe") {
            public void run() {
            	frameMacro.setEnabledStopBtn(true);
            	stopThread = false;
            	
                frameMacro.hilightScriptLine(0);
            	a.command.execHelpCmd("off");
				// TODO : bloquer l'édition de la table pendant l'exécution ?
				String[] commands = getScriptCommands();
				Map[] maps = new Map[macroModel.getParamTableModel().getRowCount()-row];
				for( int i=0; i<maps.length; i++ ) {
					maps[i] = getParamMapForRow(i+row);
				}
				
				int[] idx = new int[maps.length];
				for( int i=0; i<idx.length; i++ ) idx[i] = i+row;
				
				execScript(commands, maps, idx);

				frameMacro.hilightScriptLine(-1);
				frameMacro.setEnabledStopBtn(false);
				
		    }
		};
		execThread.start();
        a.log("macroController", "execAll");
		
	}
	
	/**
	 * Exécute l'ensemble des commandes pour chaque ensemble de paramètres contenu dans paramMaps
	 * @param commands
	 * @param paramMaps
	 */
	private void execScript(String[] commands, Map[] paramMaps, int[] idx) {
		for( int i=0; i<paramMaps.length; i++ ) {
			if( idx!=null ) {
				if( idx[i]<macroModel.getParamTableModel().getRowCount() ) {
					frameMacro.getParamTable().setRowSelectionInterval(idx[i],idx[i]);
				}
			}
			// pas d'exécution si le paramMap courant est null
			if( paramMaps[i]==null ) continue;
			for( int j=0; j<commands.length; j++ ) {
				if( stopThread ) {
					stopThread = false;
					frameMacro.hilightScriptLine(-1);
					a.warning(frameMacro, "Execution interrupted !", 1);
					return;
				}
				frameMacro.hilightScriptLine(j);
				macroModel.executeScript(commands[j], paramMaps[i]);
			}
		}
	}
	
	/**
	 * retourne la map "nom du param" --> valeur pour la ligne row
	 * @return
	 */
	private Map getParamMapForRow(int row) {
		HashMap map = new HashMap();
		
		int nbCol = macroModel.getParamTableModel().getColumnCount();
		
		if( row==-1 ) {
			Aladin.warning(frameMacro, "No parameter row selected, stop execution !", 1);
			return null;
		}
		
		String curCol, curVal;
		boolean returnNull = true;
		for( int i=0; i<nbCol; i++ ) {
			curCol = macroModel.getParamTableModel().getColumnName(i);
			curVal = (String)macroModel.getParamTableModel().getValueAt(row, i);
			if( returnNull && curVal!=null && curVal.trim().length()>0 ) returnNull = false;
			map.put(curCol, curVal);
		}
		
		// on retourne null si toutes les valeurs sont vides
		return returnNull?null:map;
	}
	
	/** 
	 * Récupère sous forme de tableau l'ensemble des commandes contenues dans le champ texte script
	 * @return
	 */
	private String[] getScriptCommands() {
		String s = frameMacro.getScriptText();
        String sep = "\n";
		StringTokenizer st = new StringTokenizer(s, sep, true);
		Vector v = new Vector();
		String cmd, ocmd;
		ocmd = sep;
		while( st.hasMoreTokens() ) {
			cmd = st.nextToken();
            
			if( sep.equals(cmd)  ) {
				if( !sep.equals(ocmd) ) {
					ocmd = cmd;
					continue;
				}
                
				ocmd = cmd;
				cmd = "";
			}
			else {
				ocmd = cmd;
			}
			
			v.addElement(cmd);
		}
		
		String[] commands = new String[v.size()];
		v.copyInto(commands);
		v = null;
		
		return commands;
	}
	
	private JFileChooser fChooser;
	/**
	 * @param label
	 * @return Renvoie un JFileChooser à utiliser par la classe
	 */
	private JFileChooser getJFileChooser(String label) {
		if( fChooser==null ) {
			fChooser = new JFileChooser(a.getDefaultDirectory());
		}
		
		if( label.equals(FrameMacro.LOAD_SCRIPT) ) {
			fChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fChooser.resetChoosableFileFilters();
			fChooser.addChoosableFileFilter(new FileNameExtensionFilter(new String[] {"ajs", "txt"}, "Aladin script files (*.ajs, *.txt)"));
			fChooser.setFileFilter(fChooser.getAcceptAllFileFilter());
		}
		else if( label.equals(FrameMacro.LOAD_PARAMS) ) {
			fChooser.setDialogType(JFileChooser.OPEN_DIALOG);
			fChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			fChooser.resetChoosableFileFilters();
			fChooser.addChoosableFileFilter(new FileNameExtensionFilter(new String[] {"txt", "tsv", "csv"}, "CSV Files (*.txt, *.tsv, *.csv)"));
			fChooser.setFileFilter(fChooser.getAcceptAllFileFilter());
		}
		else if( label.equals(FrameMacro.SAVE_SCRIPT) || label.equals(FrameMacro.LOAD_SCRIPT) ) {
			fChooser.setDialogType(JFileChooser.SAVE_DIALOG);
			fChooser.resetChoosableFileFilters();
		}
		
		
		return fChooser;
	}
	
	class FileNameExtensionFilter extends FileFilter {
		
		String[] extensions;
		String desc;
		
		FileNameExtensionFilter(String[] extensions, String desc) {
			this.extensions = extensions;
			this.desc = desc;
		}
		
		// accept only files with extension being one of those listed in extensions array
		public boolean accept(File f) {
		    if( f.isDirectory()) {
		    	return true;
		    }

		    String ext = getExtension(f);
		    
		    return Util.indexInArrayOf(ext, extensions)>=0;
		}
		
		public String getDescription() {
			return desc;
		}
		
		
		public String getExtension(File f) {
			if (f == null) return null;
			
			String ext = null;
			String s = f.getName();
			int i = s.lastIndexOf('.');

			if (i > 0 && i < s.length() - 1) {
				ext = s.substring(i + 1).toLowerCase();
			}
			return ext;
		}
		
	} // end of inner class FileNameExtensionFilter
	
	/**
	 * @return Returns the macroModel.
	 */
	protected MacroModel getMacroModel() {
		return macroModel;
	}
	/**
	 * @param macroModel The macroModel to set.
	 */
	protected void setMacroModel(MacroModel macroModel) {
		this.macroModel = macroModel;
	}

}
