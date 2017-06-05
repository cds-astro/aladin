// Copyright 1999-2017 - Université de Strasbourg/CNRS
// The Aladin program is developped by the Centre de Données
// astronomiques de Strasbourgs (CDS).
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import cds.tools.TwoColorJTable;
import cds.tools.Util;

/**
 * Le formulaire d'interrogation du serveur de champ de vue
 *
 * @author Pierre Fernique [CDS]
 * @version 2.0 : jan 03 - Suppression du Layout Manager et toilettage
 * @version 1.0 : (nov 00) Creation
 */
public final class ServerFoV extends Server implements TableModel {
   static final int MAXLINE = 7;	// Nbre de lignes de la liste des images dispo

   String LOAD;

   Vector<FoVItem> fovList;  // la liste des FoVs
   JTable table;
   TableModelListener tableListener;
   JTextField roll;
   String info1,info2,angle,angle1,edit,fovedit;
   int idxSortedCol = 1; // indice de la colonne sur laquelle on trie
   boolean ascSort;

   protected void createChaine() {
      super.createChaine();
      description  = aladin.chaine.getString("FOVINFO");
      info1 = aladin.chaine.getString("FOVINFO1");
      info2 = aladin.chaine.getString("FOVINFO2");
      angle = aladin.chaine.getString("FOVANGLE");
      angle1= aladin.chaine.getString("FOVANGLE1");
      angle = angle+" ("+angle1+")";
      edit = aladin.chaine.getString("FOVEDIT");
      LOAD = aladin.chaine.getString("FOVLOAD");
      fovedit = "FovEditor";
   }

 /** Creation du formulaire d'interrogation d'Aladin.
   * @param aladin reference
   * @param status le label qui affichera l'etat courant
   */
   protected ServerFoV(Aladin aladin) {
      this.aladin = aladin;
      createChaine();
//      aladinLabel = "FoV";
      aladinLabel = "Instrument Field of Views (FoV)";
      aladinMenu = "FoV...";
      type = APPLI;
      aladinLogo = "FoVLogo.gif";
      grab=null;

      setBackground(Aladin.BLUE);
      setLayout(null);
      setFont(Aladin.PLAIN);
      setBounds(0,0,WIDTH,HEIGHT);
      int y=20;

      // Le titre
      JPanel tp = new JPanel();
      tp.setBackground(Aladin.BLUE);
      Dimension d = makeTitle(tp,description);
      tp.setBounds(XWIDTH/2-d.width/2,y,d.width,d.height); y+=d.height+10;
      add(tp);


      // Premiere indication
      JLabel l = new JLabel(info1);
      l.setBounds(182,y,400, 20); y+=15;
      add(l);
      l = new JLabel(info2);
      l.setBounds(92,y,300, 20); y+=20;
      add(l);

      // JPanel pour la memorisation du target (+bouton DRAG)
      JPanel tPanel = new JPanel();
      tPanel.setBackground(Aladin.BLUE);
      int h = makeTargetPanel(tPanel, NORADIUS);
      tPanel.setBounds(0,y,XWIDTH,h); y+=h;
      add(tPanel);

      modeCoo=COO|SIMBAD;
      modeRad=NOMODE;

      // Radius
      JLabel label = new JLabel(addDot(angle));
      label.setFont(Aladin.BOLD);
      label.setBounds(XTAB1,y,XTAB2-10,HAUT);
      add(label);
      roll = new JTextField("0.0");
      roll.setBounds(XTAB2,y,XWIDTH-XTAB2,HAUT); y+=HAUT+MARGE+10;
      add(roll);
//      JLabel ex = new JLabel(angle1);
//      ex.setFont(Aladin.SITALIC);
//      ex.setBounds(XTAB2,y,XWIDTH-XTAB2,20); y+=HAUT+MARGE;
//      add(ex);

      table = new TwoColorJTable(this);
      table.setGridColor(Color.lightGray);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      table.getTableHeader().setDefaultRenderer(new TableHeaderRenderer(table.getTableHeader().getDefaultRenderer()));
      table.getTableHeader().addMouseListener(new TableHeaderListener());
      // on fixe la taille de chaque colonne
      table.getColumnModel().getColumn(0).setPreferredWidth(110);
      table.getColumnModel().getColumn(1).setPreferredWidth(80);
      table.getColumnModel().getColumn(2).setPreferredWidth(300);
      table.getColumnModel().getColumn(3).setPreferredWidth(80);

      JScrollPane sc = new JScrollPane(table);
      sc.setBounds(XTAB1,y,XWIDTH,150); y+=150;
      add(sc);

      // Bouton pour accéder à l'éditeur de FoV
      y+=10;
      JButton b = new JButton(edit);
      b.setOpaque(false);
      b.addActionListener(this);
      b.setBounds(190,y,150,HAUT);
      add(b);
      b = new JButton(LOAD);
      b.setOpaque(false);
      b.addActionListener(this);
      b.setBounds(190+150+10,y,100,HAUT);
      add(b);

      // Indique le component à maximiser (seulement en hauteur)
      setMaxComp(sc);

      initStaticFoV();

   }

   /** Creation d'un plan de maniere generique
    * La syntaxe du critere est "Instrument[,roll]"
    */
   protected int createPlane(String target,String radius,String criteria,
   				 String label, String origin) {
      String instrument=null;
      double roll=0.;

      Tok st = new Tok(criteria," ,");
      instrument = st.nextToken();
      if( instrument!=null ) instrument = instrument.toUpperCase();
      if( st.hasMoreTokens() ) {
         try { roll=Double.valueOf(st.nextToken()).doubleValue(); }
         catch( Exception e ) { };
      }

      initDynamicFoV();

      return creatFieldPlane(target,roll,instrument,label);
   }


   /** Creation d'un plan de maniere specifique
    * @param target
    * @param roll Angle de rotation du champ de vue
    * @param instrument Instrument
    * @return numero de plan utilise, -1 si erreur
    */
   protected int creatFieldPlane(String target,double roll,String instrument,String label) {
      if( instrument==null || instrument.equals("") ) {
         Aladin.warning(WNEEDCHECK,1);
         return -1;
      }

      loadRemoteFoV();
//      PlanField pf = getFovByID(instrument);
//      if( pf!=null ) return aladin.calque.newPlanField(pf,target,roll,instrument);
      FootprintBean fpBean = getFovBeanByID(instrument);
      // TODO : refactor, on pourrait peut etre remonter tout ça au niveau de PlanField, avec un seul constructeur qui chercherait les beans selon l'id

//      if( label==null ) label=instrument;
      label = getDefaultLabelIfRequired(label,instrument);
      if( fpBean!=null ) return aladin.calque.newPlanField(fpBean, target, label,roll);
      else return aladin.calque.newPlanField(target,roll,instrument,label);
   }

  /** Interrogation  */
   public void submit() {
      double r;

      String t = getTarget();
      String s = roll.getText().trim();
      if( s.length()==0 ) r=0.;
      else r = Double.valueOf(s).doubleValue();

      int idx = table.getSelectedRow();
      if( idx>=0 ) {
         FoVItem fov = fovList.elementAt(idx);
         s = fov.id;
      } else s=null;
      
      String code = "get FoV("+Tok.quote(s)+")";
      aladin.console.printCommand(code+" "+t);

      int n= creatFieldPlane(t,r,s,null);
      if( n!=-1 ) {
         ball.setMode(Ball.OK);
         aladin.calque.getPlan(n).setBookmarkCode(code+" $TARGET");
      } else ball.setMode(Ball.NOK);
   }

   // on conserve l'id du dernier FOV enregistré
   static protected String idLastRegistered;
   /**
    * Record a new fov
    * @param id ID of the new fov
    * @param pf
    * @return true if registration successful, false otherwise
    */
   public boolean registerNewFovTemplate(String id, PlanField pf) {
      if( findFoVIndex(id)>=0 ) {
      	Aladin.trace(3, "PlanField "+id+" is already registered !! Existing definition will be erased");
      }
      else {
    	  addFoV(id,pf);
      }

      idLastRegistered = id;

      return true;
   }

   /**
    * Returns a previously registered PlanField, given its id
    * @param id
    * @return PlanField object with

   public PlanField getFovByID(String id) {
   	PlanField template = FootprintParser.getPlanFieldFromID(id);
   	if( template == null ) return null;

   	return template;
   }
   */

   public FootprintBean getFovBeanByID(String id) {
   	  return FootprintParser.getBeanFromID(id);
   }

   /** sélectionne un FOV donné
    *
    * @param id id fu FOV à sélectionner
    */
   public void selectFOV(String id) {
      int i = findFoVIndex(id);
      table.getSelectionModel().setSelectionInterval(i,i);
   }

   public Class getColumnClass(int columnIndex) { return String.class; }
   public int getColumnCount() { return 4; }

   public String getColumnName(int columnIndex) {
      switch(columnIndex) {
         case 0: return "Instrument";
         case 1: return "Telescope";
         case 2: return "Description";
         case 3: return "Author";
      }
      return "";
   }

   /** Recherche de l'indice d'un FoV dans la table */
   int findFoVIndex(String id) {
      for( int i=0; i<fovList.size(); i++ ) {
         FoVItem fov = fovList.elementAt(i);
         if( fov.id.equals(id) ) return i;
      }
      return -1;
   }

   boolean addFoV(String id, PlanField pf) {
      if( findFoVIndex(id)!=-1 ) return false;
      FoVItem fov = new FoVItem(id,pf);
      fovList.addElement(fov);
      idxSortedCol = 1;
      ascSort = true;
      fireTable();
      return true;
   }

   boolean addFoV(String instr,String telesc, String descr, String orig) {
      String id=makeID(instr,telesc);
      if( findFoVIndex(id)!=-1 ) return false;
      FoVItem fov = new FoVItem(instr,telesc,descr,orig);
      fovList.addElement(fov);
      idxSortedCol = 1;
      ascSort = true;
      fireTable();
      return true;
   }

   private void notifyTableChanged() {
       int n=fovList.size();
       tableListener.tableChanged(new TableModelEvent(this, n, n,
             TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
   }

   private void fireTable() {
      defaultSortFoV();
      notifyTableChanged();
   }

   private void defaultSortFoV() {
      Collections.sort(fovList,new Comparator<FoVItem>() {

        public int compare(FoVItem f1, FoVItem f2) {
            int n = f1.telescope.compareTo(f2.telescope);
            if( n!=0 ) return n;
            return  f1.instrument.compareTo(f2.instrument);
        }
      });
      if( ! ascSort) Collections.reverse(fovList);
      ascSort = !ascSort;
   }

   public int getRowCount() { return fovList==null?-1:fovList.size(); }

   public Object getValueAt(int rowIndex, int columnIndex) {
      FoVItem fov = fovList.elementAt(rowIndex);
      switch(columnIndex) {
         case 0: return fov.instrument;
         case 1: return fov.telescope;
         case 2: return fov.description;
         case 3: return fov.origine==null ? "?" : fov.origine;
      }
      return "";
   }

   public boolean isCellEditable(int rowIndex, int columnIndex) { return false; }
   public void setValueAt(Object aValue, int rowIndex, int columnIndex) { }
   public void addTableModelListener(TableModelListener l) {
      tableListener=l;
   }
   public void removeTableModelListener(TableModelListener l) { }


   static final String FOV[][] = {
//      { "WFPC2",    "HST", "Wide Field and Planetary Camera",    "CDS"},
      { "MEGACAM",  "CFHT","Wide field imaging camera",          "CFH" },
      { "MEGAPRIME","CFHT","Wide field imaging camera + guiders","CFH" },
      { "CFH12K",   "CFHT","Large field camera",                 "CFH" },
      { "WIRCAM",   "CFHT","Wide field IR camera",               "CFH" },
      { "ESPADONS", "CFHT","Echelle Spectropolarimetric device", "CFH" },
      { "EPICMOS",  "XMM", "Sensitive imaging (0.1 to 15 keV)",  "CDS" },
      { "EPICpn",   "XMM", "High resolution (<0.03ms)",          "CDS" },
   };

   private void initStaticFoV() {
      fovList = new Vector<FoVItem>();
      for( int i=0; i<FOV.length; i++ ) {
         FoVItem fov = new FoVItem(FOV[i][0],FOV[i][1],FOV[i][2],FOV[i][3]);
         fovList.add(fov);
      }
   }

   public void show() {
      initDynamicFoV();
      super.show();
   }

   private boolean dynFoV=false;
   private void initDynamicFoV() {
      if( dynFoV ) return;
      (new Thread("initFov") {
         public void run() { loadRemoteFoV(); }
      }).start();
   }

   /** Chargement des description des FoV distants */
   protected void loadRemoteFoV() {
      if( dynFoV ) return;
      try {
         dynFoV=true;
         Aladin.trace(3,"Loading FoV definitions...");
         MyInputStream in = new MyInputStream(aladin.cache.get(Aladin.FOVURL));
         aladin.processFovVOTable(in,null,false);
      } catch( Exception e1 ) { if( Aladin.levelTrace>=3 ) e1.printStackTrace(); }
   }

   static String makeID(String instrument,String telescope) {
      return instrument+"."+telescope;
   }

   /** Gère la liste des FoV disponibles */
   class FoVItem {

       String instrument;
       String telescope;
       String description;
       String origine;
       String id;
       PlanField pf;  // JE NE SUIS PAS VRAIMENT SUR QU'IL FAILLE LE MEMORISER ?
       // A DEMANDER A THOMAS

       FoVItem(String instr,String telesc, String desc,String orig) {
           instrument=instr;
           telescope=telesc;
           description=desc;
           origine=orig==null?"":orig;
           id = instrument;
           pf=null;
       }

       FoVItem(String id,PlanField pf) {
           this.id=id;
           this.pf=pf;
           instrument=pf.getInstrumentName();
           telescope=pf.getTelescopeName();
           description = pf.getInstrumentDesc();
           origine = pf.getOrigine();

           if( instrument==null || instrument.length()==0 ) instrument = id;
       }
   }

   /** Renderer pour le header de la JTable
    *  Permet d'afficher les triangles de tri
    */
   class TableHeaderRenderer extends DefaultTableCellRenderer {

       TableCellRenderer renderer;

       public TableHeaderRenderer(TableCellRenderer defaultRenderer) {
                   renderer = defaultRenderer;
       }

       /**
        * Overwrites DefaultTableCellRenderer.
        */
       public Component getTableCellRendererComponent(JTable table, Object
               value, boolean isSelected,
               boolean hasFocus, int row,
               int column) {

           Component comp = renderer.getTableCellRendererComponent(
                   table, value, isSelected, hasFocus, row, column);

           int idxSortedColView = table.convertColumnIndexToView(idxSortedCol);
           // if column col has been clicked, add a small arrow to the column header
           if( comp instanceof JLabel ) {
               if( column==idxSortedColView ) {
                   ImageIcon icon = ascSort?Util.getAscSortIcon():Util.getDescSortIcon();
                   ((JLabel)comp).setIcon(icon);
                   ((JLabel)comp).setHorizontalTextPosition(SwingConstants.LEADING);
               }
               else ((JLabel)comp).setIcon(null);
           }
           return comp;
       }
   }

   /** Classe interne pour le header de la table
    *  Permet le tri lorsqu'on clique sur un des bandeaux
    */
   class TableHeaderListener extends MouseAdapter {

       public void mouseClicked(MouseEvent e) {
           TableColumnModel columnModel = table.getColumnModel();
           int viewColumn = columnModel.getColumnIndexAtX(e.getX());
           final int column = table.convertColumnIndexToModel(viewColumn);

           idxSortedCol = column;

           if( e.getClickCount() == 1 && column != -1 ) {
               // colonne d'indice 1 : on applique le tri par défaut
               if( column==1) {
                   int idx = table.getSelectedRow();
                   final Object selected = idx>=0?fovList.get(idx):null;
                   defaultSortFoV();
                   notifyTableChanged();
                   selectItem(selected);
                   return;
               }

               Comparator comp = new Comparator() {
                   public final int compare (Object a, Object b) {
                       Object val1, val2;
                       val1 = val2 = null;
                       switch(column) {
                               case 0 : val1 = ((FoVItem)a).instrument;
                               val2 = ((FoVItem)b).instrument;
                               break;

                               case 2 : val1 = ((FoVItem)a).description;
                               val2 = ((FoVItem)b).description;
                               break;

                               case 3 : val1 = ((FoVItem)a).origine;
                               val2 = ((FoVItem)b).origine;
                               break;
                           }
                       return ((Comparable)val1).compareTo(val2);
                   }
               };

               int idx = table.getSelectedRow();
               final Object selected = idx>=0?fovList.get(idx):null;

               Collections.sort(fovList, comp);
               if( ! ascSort ) Collections.reverse(fovList);
               ascSort = !ascSort;
               notifyTableChanged();

               // update selection so to keep initial selection after sorting is done
               selectItem(selected);
           }
       }

       private void selectItem(final Object o) {
           SwingUtilities.invokeLater(new Runnable() {
               public void run() {
                   table.clearSelection();
                   if( o ==null ) return;
                   int idxSelected = fovList.indexOf(o);
                   if( idxSelected>=0 ) table.addRowSelectionInterval(idxSelected, idxSelected);
               }
           });
       }

   } // end of inner class TableHeaderListener

   public void actionPerformed(ActionEvent e) {
      Object o = e.getSource();
      if( o instanceof JButton ) {
         String menu = ((JButton)o).getActionCommand();
         if( menu.equals(edit) )  aladin.buildFoV();
         else if( menu.equals(LOAD) )  ((ServerFile)aladin.dialog.localServer).browseFile();
      }

      super.actionPerformed(e);
   }

}
