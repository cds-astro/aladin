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

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import cds.tools.Util;

/**
 * Gestion de la fenetre de la console Aladin
 *
 * @author Pierre Fernique [CDS]
 * @version 3.0 : juil 10 refonte de la console sous forme d'une table
 * @version 2.0 : fév 05  Ajout de la gestion d'une console
 * @version 1.1 : 29 oct 04  Ajout du clonage du contenu de la measurement frame
 * @version 1.0 : 4 juin 99  Creation
 */
public final class Console extends JFrame implements ActionListener,KeyListener,MouseListener {
   
   // Nombre de commandes à sauvegarder
   // -1 toutes; 0 aucune, 100=> les 100 dernières
   static final private int NBHISTORYCMD = 1000;

   private static String EXEC,CLOSE,CLEAR,DELETE,HELP,COPY,DUMP;

   // Les references aux objets
   private Aladin aladin;

   // Les composantes de l'interface
   private JTable table;               // L'historique des commandes et info 
   private JTextArea fieldCmd;         // La zone de texte
   private JTextArea fieldPad;         // La zone du notepad
   private JButton exec,clear,delete,dump,clearPad;  // Qq boutons

   private Vector<String> cmd = new Vector<String>();              // Les commandes non encore traitées
   private  Vector<Command> cmdHistory = new Vector<Command>();  // L'historique des commandes traitées
   
   private String currentCmd = null;  // La dernière commande en cours de saisie 

   // Les différents types de "commandes" mémorisées
   static final private int CMD = 0;
   static final private int INFO = 1;
   static final private int ERROR = 2;
   
   static final String [] TYPES = { "Cmd","Info","Error" };
      
   // Gestion des dates
   static private SimpleDateFormat SDF;
   static {
      SDF = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
      SDF.setTimeZone(TimeZone.getDefault());
   }
   
   // Nom du fichier d'historique des commandes
   static private final String HISTORYNAME = "History.ajs";
   
   public void createChaine() {
      CLOSE  = aladin.chaine.getString("CLOSE");
      CLEAR  = aladin.chaine.getString("CLEAR");
      DELETE = aladin.chaine.getString("RESETHISTORY");
      HELP   = aladin.chaine.getString("HELPSCRIPT");
      EXEC   = aladin.chaine.getString("EXEC");
      DUMP   = aladin.chaine.getString("NPCLONE");
   }
   
   public Console(Aladin aladin) {
      super();
      Aladin.setIcon(this);
      this.aladin = aladin;
      createChaine();
      setTitle( aladin.chaine.getString("NPTITLE") );
      enableEvents(AWTEvent.WINDOW_EVENT_MASK);
      Util.setCloseShortcut(this, false, aladin);
      addMouseListener(this);
      JPanel pane = (JPanel) getContentPane();
      pane.setBorder( BorderFactory.createEmptyBorder(10, 10, 10, 10));

      pane.add(getHistoryPanel(),BorderLayout.NORTH);
      
      JPanel p2 = new JPanel(new BorderLayout(5,5));
      p2.add(getCommandPanel(),BorderLayout.NORTH);
      p2.add(getPadPanel(),BorderLayout.CENTER);
      
      pane.add(p2,BorderLayout.CENTER);
      
//      setLocation(Aladin.computeLocation(this));
      
      if( aladin.STANDALONE && !aladin.NOGUI ) {
         try { loadHistory(); }
         catch( Exception e ) { if( aladin.levelTrace>=3 ) e.printStackTrace(); }
      }
      
      pack();
      resumeButton();
   }
   
   /** Mémorisation d'une commande */
   public void printCommand(String cmd) {
      if( cmd==null || cmd.trim().length()==0 ) return;
      cmdHistory.addElement( new Command(cmd) );
      resetArrowHistory();

      resumeTable();
   }

   /** Mémorisation d'une info */
   public void printInfo(String info) {
      cmdHistory.addElement( new Command(info,INFO) );
      resumeTable();
   }
   
   /** Mémorisation d'une erreur */
   public void printError(String error) {
      cmdHistory.addElement( new Command(error,ERROR) );
      resumeTable();
   }
   
   /** Ajout dans le pad et scrolling à la fin si nécessaire */
   public void printInPad(String s) {
      boolean scroll = fieldPad.getCaretPosition()==fieldPad.getText().length();
      fieldPad.append(s);
      if( scroll ) fieldPad.setCaretPosition(fieldPad.getText().length());
      clearPad.setEnabled(true);
    }
   
   /** Ajout dans le pad des measurements des objets sélectionnés */
   public void dumpMeasurements() {
      printInPad( aladin.mesure.getText() );
      aladin.log("DumpMeasurements","");
   }
   
   // Nettoyage du pad
   public void clearPad() {
      fieldPad.setText("");
   }
   
   // Nettoyage du champ de saisie
   private void clear() {
      fieldCmd.setText("");
      resumeTable();
   }
   
   /** Suppression de l'historique */
   private void deleteHistory() {
      if( !aladin.confirmation(this, aladin.chaine.getString("NPCONF")) ) return;
      cmdHistory.clear();
      resumeTable();
   }

   /** Retourne true si une commande est en attente de traitement */
   synchronized public boolean hasWaitingCmd() { return cmd.size()>0; }

   /** Empile la prochaine commande à traiter et réveille command pour la traiter */
   synchronized public void pushCmd(String s) {
      // thomas, 16/11/06 : permet de ne pas couper la déf. des filtres (pb des ';' dans les UCD !)
      String[] commands = Util.split(s, "\n;", '[', ']');
      for( int i=0; i<commands.length; i++ ) {
         cmd.addElement(commands[i]);
      }
      aladin.command.readNow();
   }

   /** Dépile la prochaine commande à traiter */
   synchronized public String popCmd() {
      if( cmd.size()==0 ) return "";
      String s = cmd.elementAt(0);
      cmd.removeElementAt(0);
      return s;
   }
   
   // Execution de la commande en cours de saisie
   private void execute() {
      String cmd = fieldCmd.getText();
      if( (cmd=isCmdComplete(cmd))!=null ) {
         pushCmd(cmd);
         fieldCmd.setText("");
      }
   }
   
   public void show() {
      super.show();
      fieldCmd.requestFocusInWindow();
   }
   
   // Retourne null si la commande en cours de saisie n'est pas complète
   // typiquement pour la définition d'un filtre ou d'une fonction.
   // (Se base sur le nombre d'occurences des accolades)
   // retourne la commande mise en forme
   private String isCmdComplete(String cmd) {
      cmd = cmd.trim();
      if( !cmd.startsWith("filter") && !cmd.startsWith("function") ) return cmd;
      
      int n=cmd.length();
      int acc=0;
      boolean findacc=false;
      
      StringBuffer s = new StringBuffer();
      for( int i=0; i<n; i++ ) {
         char c = cmd.charAt(i);
         /*if( c=='\n' ) c=';';
         else */if( c=='{' ) { findacc=true; acc++; }
         else if( c=='}' ) acc--;
         s.append(c);
      }
      if( acc==0 || !findacc ) return s.toString();
      return null;
   }

   
   /** Sauvegarde l'historique des commandes.
    * Les commandes sont sauvegardées sous la forme de deux lignes
    * consécutives, la première sous la forme d'un commentaire qui donne la date, la deuxième donne
    * la commande, où éventuellement le message d'erreur/l'info sous la forme d'un commentaire
    * ex: #10/10/09 15:32:10
    *     grid on
    *     #10/10/09 15:33:00
    *     #Aladin stopped
    * @throws Exception
    */
   public void saveHistory() throws Exception {
      
      // Existe-il déjà un répertoire générique .aladin sinon je le crée ?
      String configDir = System.getProperty("user.home") + Util.FS + aladin.CACHE;
      File f = new File(configDir);
      if( !f.isDirectory() ) if( !f.mkdir() ) throw new Exception(
            "Cannot create " + aladin.CACHE + " directory");

      // Je vais (re)créer le fichier
      String name = configDir + Util.FS + HISTORYNAME;
      f = new File(name);
      f.delete();
      
      int n = cmdHistory.size();
      if( NBHISTORYCMD==0 ) return;
      if( NBHISTORYCMD==-1 ) n=0;
      else n-=NBHISTORYCMD;
      
      aladin.trace(2,"Saving command history ["+name+"]...");
      BufferedWriter bw = new BufferedWriter(new FileWriter(f));
      
      if( n<0 ) n=0;
      for( int i=n; i<cmdHistory.size(); i++ ) {
         Command c = cmdHistory.elementAt(i);
         bw.write(c+"\n");
      }
      bw.close();
   }
   
   /** Chargement de l'historique des commandes (voir format ci-dessus) */
   public void loadHistory() throws Exception {
      
      // Ouverture du fichier d'historique
      String name = System.getProperty("user.home") + Util.FS + aladin.CACHE + Util.FS + HISTORYNAME;
      File f = new File(name);
      if( !f.exists() ) return;
      aladin.trace(2,"Loading command history ["+name+"]...");
      BufferedReader br = new BufferedReader(new FileReader(f));
      String s;
      
      while ( (s=br.readLine())!=null ) {
         
         
         // Lecture de la date de la commande  ==> #10/12/10 12:30:12
         s = s.trim();
         if( s.length()==0 ) continue;
         
         Command c = new Command();
         
         if( s.charAt(0)=='#' ) {
            s=s.substring(1);
            
            // Si ce n'est pas une date, ce doit être un commentaire ou une erreur
            if( !c.setDate(s) ) {
               c.cmd=s;
               c.type= s.startsWith("!!!") ? ERROR : INFO;
               cmdHistory.addElement(c);
               continue;
            }
            
            // Lecture de la commande qui suit
            s=br.readLine();
            if( s==null ) break;
            s = s.trim();
            if( s.length()==0 ) continue;
         }
         
         // Analyse de la commande
         if( s.charAt(0)=='#' ) {
            s=s.substring(1);
            c.cmd=s;
            c.type= s.startsWith("!!!") ? ERROR : INFO;
         } else {
            c.cmd=s;
            c.type=CMD;
         }
         
         cmdHistory.addElement(c);
      }
   }
   
   /********* History pour l'affichage dans le champ Location (controle avec les flèches) ************/
   
   private int indexArrowHistory=-1;   // -1 signifie dernière commande, sinon indice dans cmdHistory 
   
   /** Reset car une nouvelle commande a été ajoutée */
   private void resetArrowHistory() { indexArrowHistory=-1; }
   
   public int getIndexArrowHistory() { return indexArrowHistory; }
   
   /** Visualisation de la commande suivante ou précédente dans l'historiques */
   public String getNextArrowHistory(int sens) {
      
      // On se recale tout en haut
      if( sens==2 ) resetArrowHistory();
      
      if( indexArrowHistory==-1 && cmdHistory.size()>0 ) indexArrowHistory = cmdHistory.size();
      
      Command cmd=null;
      
      
      try {
         for( indexArrowHistory+=sens; indexArrowHistory>=0 || indexArrowHistory<=cmdHistory.size(); indexArrowHistory+=sens ) {
            cmd = cmdHistory.get(indexArrowHistory);
            if( cmd.isCmd() ) break;
         }
      } catch( Exception e ) { }
      
      if( indexArrowHistory<0 ) indexArrowHistory=0;
      if( indexArrowHistory>=cmdHistory.size() ) indexArrowHistory=-1;
      if( indexArrowHistory==-1 ) return null;
      
      return cmd==null || !cmd.isCmd() ? null : cmd.getCommand();
   }
   
   
   /**************************************** Gestion de l'interface graphique *************************/
   
   // Remet à jour l'état des boutons
   public void resumeButton() {
      try {
         dump.setEnabled( aladin.mesure.getNbSrc()>0 );
         clearPad.setEnabled( fieldPad.getText().trim().length()>0 );
         
         boolean qq = fieldCmd.getText().trim().length()>0;
         clear.setEnabled( qq );
         exec.setEnabled( qq && isCmdComplete(fieldCmd.getText())!=null );
         delete.setEnabled( cmdHistory.size()>0 );
      } catch( Exception e ) { }
      
   }
   
   public void setEnabledDumpButton(boolean flag) { dump.setEnabled(flag); }

   // Remet à jour la table de l'historique des commandes
   private void resumeTable() {
      
      // On a atteint la limite ?
      while( NBHISTORYCMD!=-1 && cmdHistory.size()>NBHISTORYCMD ) cmdHistory.removeElementAt(0);
      
      ( (HistoryTable) table.getModel()).fireTableDataChanged();
      SwingUtilities.invokeLater(new Runnable() {
         public void run() {
            table.scrollRectToVisible( table.getCellRect(cmdHistory.size()-1,0,true) );
            resumeButton();
      }});
      
   }

   // Création du Panel du champ de saisie d'une commande
   private JPanel getCommandPanel() {
      JScrollPane sc = new JScrollPane( fieldCmd = new JTextArea(4,60) );
      fieldCmd.setFont(Aladin.COURIER);
      fieldCmd.addKeyListener(this);
      
      // Le champ de saisie d'une commande
      JPanel p1 = new JPanel( new BorderLayout());
      p1.add(new JLabel("Command:"),BorderLayout.NORTH );
      p1.add(sc,BorderLayout.SOUTH);
      
      // Les boutons de commandes
      JButton b;
      JPanel p2 = new JPanel();
      p2.add( exec=b=new JButton(EXEC)); b.addActionListener(this);
      b.setFont( b.getFont().deriveFont(Font.BOLD));
      p2.add( clear=b=new JButton(CLEAR)); b.addActionListener(this);
      p2.add( delete=b=new JButton(DELETE)); b.addActionListener(this);
      p2.add( b=new JButton(HELP)); b.addActionListener(this);
      
      JPanel p = new JPanel( new BorderLayout());
      p.add(p1,BorderLayout.NORTH);
      p.add(p2,BorderLayout.SOUTH);

      return p;
   }
   
   // Création du Panel du notepad
   private JPanel getPadPanel() {
      JScrollPane sc = new JScrollPane( fieldPad = new JTextArea(8,60) );
      fieldPad.setFont(Aladin.COURIER);
      fieldPad.addKeyListener(this);
      
      JPanel p1 = new JPanel( new BorderLayout());
      p1.add(new JLabel("Notepad:"),BorderLayout.NORTH );
      p1.add(sc,BorderLayout.CENTER);
      
      // Les boutons de commandes
      JButton b;
      JPanel p2 = new JPanel();
      p2.add( dump=b=new JButton(DUMP)); b.addActionListener(this);
      b.setEnabled(false);
      p2.add( clearPad=b=new JButton(CLEAR)); b.addActionListener(this);
      b.setEnabled(false);
      p2.add( new JLabel("                 "));
      p2.add( b=new JButton(CLOSE)); b.addActionListener(this);

      
      JPanel p = new JPanel( new BorderLayout());
      p.add(p1,BorderLayout.CENTER);
      p.add(p2,BorderLayout.SOUTH);
      
      return p;
   }
   // Création du Panel de la table de l'historique des commandes
   private JPanel getHistoryPanel() {
      JPanel p = new JPanel( new BorderLayout());
      JScrollPane sc = new JScrollPane(createHistoryTable());
      p.add(sc,BorderLayout.CENTER);
      return p;
   }
      
   // Création de la table de l'historique des commandes
   private JTable createHistoryTable() {
      table=new JTable( new HistoryTable() );
      table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      table.getColumnModel().getColumn(TYPE).setMinWidth(40);
      table.getColumnModel().getColumn(TYPE).setMaxWidth(40);
      table.getColumnModel().getColumn(DATE).setMinWidth(100);
      table.getColumnModel().getColumn(DATE).setMaxWidth(100);
      table.setPreferredScrollableViewportSize(new Dimension(420,16*12));
      
      MyRenderer cr = new MyRenderer();
      table.getColumnModel().getColumn(TYPE).setCellRenderer(cr);
      table.getColumnModel().getColumn(DATE).setCellRenderer(cr);
      table.getColumnModel().getColumn(DESC).setCellRenderer(cr);

      table.addMouseListener(new MouseAdapter() {
         public void mouseReleased(MouseEvent e) { showSelectedCommands(); }
      });

      return table;
   }
   
   // Le renderer des cellules de la table (juste pour gérer la couleur en fonction du type de commande
   class MyRenderer extends DefaultTableCellRenderer {

      public Component getTableCellRendererComponent(JTable table,Object value,
            boolean isSelected,boolean hasFocus, int row, int col ) {
         Component cell = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,col);
         
         Command c = cmdHistory.elementAt(row);
         Color color = Color.red;
         if( c.type==CMD ) color=Color.black;
         else if( c.type==INFO ) color=Aladin.COLOR_GREEN;
         
         cell.setForeground(color);
         cell.setFont(cell.getFont().deriveFont( col==DESC ? Font.BOLD : Font.PLAIN) );
         return cell;
      }
   }

   // les 3 colonnes de la table
   static final private int DATE  = 0;
   static final private int TYPE  = 1;
   static final private int DESC  = 2;
   
   // La classe gérant la table
   private class HistoryTable extends AbstractTableModel {
      
      public int getColumnCount() { return 3; }
      public int getRowCount() { return cmdHistory.size(); }
      
      public String getColumnName(int col) { 
         return col==TYPE ? "Type" :
                col==DATE ? "Date" : 
                            "Description" ;
      }
      
      public Object getValueAt(int row, int col) {
         Command c = cmdHistory.elementAt(row);
         switch( col ) {
            case TYPE : return c.getType();
            case DATE : return c.getDate(); 
            default   : return c.getCommand();
         }
      }
      
      public boolean isCellEditable(int row, int col) {
         return col==DESC;
      }
   }
   
   // Visualisation dans le champ de saisie de toutes les commandes sélectionnées
   // dans la table
   private void showSelectedCommands() {
      int [] row = table.getSelectedRows();
      StringBuffer s = new StringBuffer();
      for( int i=0; i<row.length; i++ ) {
         Command c = cmdHistory.elementAt(row[i]);
         if( c.type!=CMD ) continue;
         s.append(c.cmd+"\n");
      }
      fieldCmd.setText( s.toString() );
      if( row.length>0 ) setIndexCmd(row[0]);
      resumeButton();
   }
   
   private int oIndexCmd = -1;  // Dernière commande en cours de pointage
   private int indexCmd = -1;   // Commande en cours de pointage
   
   // Positionnement de la commande surlignée
   private void setIndexCmd(int n) { indexCmd=n; }
   
   // Affiche dans le champ de saisie la commande précédente, resp. suivante
   private void showCmd(int sens) {
      if( sens==-1 && !isFirstLine() ) return;
      if( sens==+1 && !isLastLine()  ) return;
      
      // Conserve la commande en cours de saisie
      if( indexCmd==-1 ) currentCmd = fieldCmd.getText();
      
      int index = indexCmd==-1 ? cmdHistory.size() : indexCmd;
      index += sens;
      
      // Incrémente/Décrémente
      if( index<0 ) return;
      if( index>=cmdHistory.size() ) indexCmd=-1;
      else indexCmd=index;
      if( oIndexCmd==indexCmd ) return;
      oIndexCmd=indexCmd;
      String s;
      
      // On est revenu au bout, on reprend la commande en cours d'édition
      if( indexCmd==-1 ) s = currentCmd;
      else {
         Command c = cmdHistory.elementAt(indexCmd);
         
         // On saute tout ce qui n'est pas une vraie commande
         if( c.type!=CMD ) { showCmd(sens); return; }
         s = c.getCommand();
      }
      
      // On remplit le champ de saisie
      fieldCmd.setText(s);
      
      // On surligne dans la table la commande correspondante (s'il y a lieu)
      if( indexCmd==-1 ) table.getSelectionModel().clearSelection();
      else table.getSelectionModel().setSelectionInterval(indexCmd, indexCmd);
      table.scrollRectToVisible(table.getCellRect(indexCmd==-1 ? cmdHistory.size()-1 : indexCmd,0,true));
      
//      System.out.println("ShowCmd("+sens+") indexCmd="+indexCmd);
   }
   
   // retourne true si on est dans la première ligne du champ de saisie
   private boolean isFirstLine() {
      int caret = fieldCmd.getCaretPosition();
      int pos = fieldCmd.getText().indexOf('\n');
      return pos==-1 || caret<pos; 
   }
   
   // retourne true si on est dans la première ligne du champ de saisie
   private boolean isLastLine() {
      int caret = fieldCmd.getCaretPosition();
      int pos = fieldCmd.getText().lastIndexOf('\n');
      return pos==-1 || caret>pos; 
   }

   /** Affichage du formulaire avec demande du focus pour le champ de saisie */
   public void setVisible(boolean flag) {
      super.setVisible(flag);
      if( flag ) {
         fieldCmd.requestFocusInWindow();
         resumeTable();
         resumeButton();
      }
   }

   public void keyPressed(KeyEvent e) {
      int key = e.getKeyCode();
      if( key==KeyEvent.VK_ENTER ) execute();
      else if( key==KeyEvent.VK_UP ) showCmd(-1);
      else if( key==KeyEvent.VK_DOWN ) showCmd(+1);
      else setIndexCmd(-1);
   }
   public void keyTyped(KeyEvent e) { }

   // Validation de la commande en cours
   public void keyReleased(KeyEvent e) { 
      resumeButton();
   }
   
   // Action sur les boutons
   public void actionPerformed(ActionEvent evt) {
      Object src = evt.getSource();
      
      if( src==clearPad ) { clearPad(); return; }
      
      String what = src instanceof JButton ? ((JButton)src).getActionCommand() : "";

           if( CLOSE.equals(what) ) setVisible(false);
      else if( CLEAR.equals(what) ) clear();
      else if( DELETE.equals(what) ) deleteHistory();
      else if( EXEC.equals(what) ) execute();
      else if( DUMP.equals(what) ) dumpMeasurements();
      else if( HELP.equals(what) )  aladin.command.execHelpCmd("",true);
   }

   protected void processWindowEvent(WindowEvent e) {
      if( e.getID()==WindowEvent.WINDOW_CLOSING ) setVisible(false);
      super.processWindowEvent(e);
   }

   public void mouseClicked(MouseEvent e) { }
   public void mouseEntered(MouseEvent e) { }
   public void mouseExited(MouseEvent e) { }
   public void mousePressed(MouseEvent e) { }
   public void mouseReleased(MouseEvent e) { resumeButton(); }
   
   
   /**********************************************  CLASSE/STRUCTURE POUR UNE COMMANDE  *********************************************/
   
   /** Classe interne décrivant une commande */
   private class Command {
      private int type;   // type de la commande (CMD, ERROR ou INFO) 
      private long date;  // date de la commande
      private String cmd; // contenu de la commande, de l'erreur ou de l'info
      
      private Command() {}
      private Command(String s) { this(s,CMD); }
      private Command(String s,int t) {
         cmd=s;
         type=t;
         date = System.currentTimeMillis();
      }
      
      private String getType()    { return TYPES[type]; }
      private String getDate()    { return date==0 ? " ? " : SDF.format(date); }
      private String getCommand() { return cmd; }
      
      // Retourne true s'il s'agit d'une vraie commande (pas une erreur, ni une info)
      public boolean isCmd() { return type==CMD; }
      
      // Postionnement de la date exprimée sous la forme JJ/MM/AA HH/MM/SS
      private boolean setDate(String s) { 
         try { 
            date = SDF.parse(s).getTime();
            return true;
         } catch( ParseException e ) { }
         return false;
      }
      
      public String toString() {
         String d = date==0 ? "" : "#"+getDate()+"\n";
         return d + (type!=CMD ? "#":"") + getCommand();
      }
   }
}
