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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import cds.savot.model.CoosysSet;
import cds.tools.Util;
import cds.xml.Field;

/**
 * Gestion des legendes des plans catalogues
 * Une legende peut avoir plusieurs lignes.
 *<P>
 * Utilise un tableau qui associe des noms (ex: unite) aux legendes
 * (ex: mm   minarc   Spect). Chaque element de la ligne-legende est separe
 * par un tab comme dans le format TSV.
 *
 * @author Pierre Fernique [CDS]
 * @version 1.3 : (19 fev 2004) Petite modif sur getWidth()
 * @version 1.2 : (25 juillet 2002) VOTable s'ajoute a Astrores
 * @version 1.1 : (12 dec 00) developpement de getWidth()
 * @version 1.0 : (10 mai 99) Toilettage du code
 * @version 0.9 : (??) creation
 */
public final class Legende extends AbstractTableModel  {
   String name;     // Nom de la table
   Field [] field;
   Vector<String> group=null;   // liste des GROUPs associées à la table
   boolean[] computed; // computed[i] true if field[i] is a computed column
   private int firstLink=-2;  // position du premier champ qui a un lien
   private boolean sorted=false;  // true s'il il y a tri posé sur un champ

   protected Legende() {
      field = new Field[0];
   }

   protected Legende(Vector<Field> vField) {
      field = new Field[ vField.size() ];
      computed = new boolean[ vField.size() ];
      Enumeration<Field> e = vField.elements();
      for( int i=0; e.hasMoreElements(); i++ ) {
          field[i]=e.nextElement();
          computed[i] = false;
      }
   }
   
   /** Construction d'une légende générique en fonction d'un tableau de légendes */
   protected Legende(ArrayList<Legende> leg) {
      ArrayList<Field> f = new ArrayList<Field>(100);
      for(int i=0; i<leg.size(); i++ ) {
         Legende lg = (Legende)leg.get(i);
         for( int j=0; j<lg.field.length; j++) {
            int k=0;
            for( ; k<f.size(); k++ ) if( f.get(k).equals(lg.field[j]) ) break; 
            if( k==f.size() ) f.add( new Field(lg.field[j]) );
            
         }
      }
      field = new Field[f.size()];
      computed = new boolean[field.length];
      name = "[Concatenated]";
      for( int i=0; i<field.length; i++ ) field[i] = f.get(i);
   }
   
   // constructeur par recopie (Thomas, 19/01/2005)
   // Modif pour Pierre jan 2009 pour copier également le name et computed[]
   protected Legende(Legende l) {
      this.field = new Field[l.field.length];
      for( int i=0; i<l.field.length; i++ ) {
         field[i] = l.field[i];
      }
      this.name=l.name;
      this.group = l.group;
      if( l.computed!=null ) {
         this.computed = new boolean[l.computed.length];
         System.arraycopy(l.computed, 0, this.computed, 0, l.computed.length);
      } else this.computed=null;
   }

   /** Ajustement ou création d'une légende via un tableau de chaines, et un type de d'info
    * @param leg La légende initiale, ou null si non encore créée
    * @param type 0-name, 1-datatype, 2-unit, 3-ucd, 4-width, 5-arraysize, 6-precision
    * @param val la liste des valeurs
    * @return la légende renseignée, ou adaptée.
    */
   static public Legende  adjustDefaultLegende(Legende leg,int type,String [] val) {
      if( leg==null || leg.field.length!=val.length ) {
         leg = new Legende();
         leg.field = new Field[val.length];
         leg.computed = new boolean[val.length];
      }
      for( int i=0; i<val.length; i++ ) {
         if( val[i]==null ) continue;
         if( leg.field[i]==null ) {
            leg.field[i] = new Field("Col_"+i);
            leg.computed[i] = false;
         }
         Field f = leg.field[i];
         switch(type) {
            case NAME:      f.name     = val[i].length()==0 ? "" : val[i]; break;
            case DESCRIPTION:f.description= val[i].length()==0 ? "" : val[i]; break;
            case DATATYPE:  f.datatype = val[i].length()==0 ? "" : f.typeVOTable2Fits(val[i]); break;
            case UNIT:      f.unit     = val[i].length()==0 ? "" : val[i]; break;
            case UCD:       f.ucd      = val[i].length()==0 ? "" : val[i]; break;
            case WIDTH:     f.width    = val[i].length()==0 ? "" : val[i]; f.computeColumnSize(); break;
            case ARRAYSIZE: f.arraysize= val[i].length()==0 ? "" : val[i]; f.computeColumnSize(); break;
            case PRECISION: f.precision= val[i].length()==0 ? "" : val[i]; break;
         }
      }
      return leg;
   }
   
   /** Associe une liste de GROUPs à la légende */
   public void setGroup(Vector<String> group) { 
      this.group = group;
//      Aladin.trace(4,"Legende.setGroup: ==> ["+getGroupDef()+"]");
   }
   
   /** retourne true si la légende dispose de GROUPs de définition */
   public boolean hasGroup() { return group!=null; }
   
   /** retourne sous la forme d'une chaine les définitions GROUPs associées à la table */
   public String getGroup() {
      if( group==null ) return "";
      Enumeration<String> e = group.elements();
      StringBuffer s = new StringBuffer();
      while( e.hasMoreElements() ) s.append(e.nextElement());
      return s.toString();
   }

   /** Retourne l'indice du champ (test sur similitude nom,unit,ucd,datatype) */
   protected int find(Field f) {
      for( int i=0; i<field.length; i++ ) if( field[i].equals(f) ) return i;
      return -1;
   }
   
   /** Retourne l'indice du champ (test sur le nom de colonne uniquement) */
   protected int find(String name) {
      for( int i=0; i<field.length; i++ ) if( name.equals(field[i].name) ) return i;
      return -1;
   }
   
   /** Retourne l'indice du champ RA, sinon -1 */
   protected int getRa() {
      for( int i=0; i<field.length; i++ ) if( field[i].isRa() ) return i;
      return -1;
   }
   
   /** Retourne l'indice du champ DE, sinon -1 */
   protected int getDe() {
      for( int i=0; i<field.length; i++ ) if( field[i].isDe() ) return i;
      return -1;
   }
   
   /** Retourne l'indice du champ PMDE, sinon -1 */
  protected int getPmRa() {
      for( int i=0; i<field.length; i++ ) if( field[i].isPmRa() ) return i;
      return -1;
   }
   
  /** Retourne l'indice du champ PMDE, sinon -1 */
   protected int getPmDe() {
      for( int i=0; i<field.length; i++ ) if( field[i].isPmDe() ) return i;
      return -1;
   }
   
   /** Retourne l'indice du champ X, sinon -1 */
  protected int getX() {
      for( int i=0; i<field.length; i++ ) if( field[i].isX() ) return i;
      return -1;
   }
   
  /** Retourne l'indice du champ Y, sinon -1 */
   protected int getY() {
      for( int i=0; i<field.length; i++ ) if( field[i].isY() ) return i;
      return -1;
   }
   
   /** Retourne true si le champ est visible */
   protected boolean isVisible(int index) {
      try { return field[index].visible; }
      catch( Exception e ) { }
      return true;
   }

   /** Ajout d'un champ (utilisé par les plugins) */
   protected void addField(Field f) {
      int n=field.length;
      Field nField[] = new Field[n+1];
      System.arraycopy(field,0,nField,0,n);
      nField[n]=f;
      field=nField;
   }
   
   /** Modification du nom, unité, ucd ou taille d'affichage d'une colonne.
    * Les valeurs null ou <0 ne sont pas modifiées.
    * Si l'index de position est supérieur au nombre de colonnes, les nouvelles
    * colonnes sont automatiquement créées
    * @param index position de la légende dans la table (0=1ère colonne)
    * @param name le nom
    * @param unit l'unité ou null si aucune
    * @param ucd l'ucd ou null si aucun
    * @param width le nombre de digit d'affichage, 0 si utilisation valeur par défaut.
    * @return le nombre de colonnes qu'il a fallu ajouter
    */
   protected int setField(int index, String name,String datatype, String unit,String ucd,int width) {
      Field f;
      int res=0;

      // Ajout des colonnes nécessaires et création des nouvelles légendes
      int n = field.length;
      if( index>=n ) {
         res = index-n;
         Field nField[] = new Field[index+1];
         boolean nComputed[] = new boolean[index+1];
         System.arraycopy(field,0,nField,0,n);

         if( computed!=null ) System.arraycopy(computed,0,nComputed,0,n);
         else for( int i=0; i<n; i++ ) nComputed[i]=false;

         for(int i=n; i<index+1; i++ ) {
            nField[i] = new Field("Col_"+(i+1));
            nComputed[i]=true;
         }
         computed = nComputed;
         field=nField;
      }

      // Modification de la légende concernée
      f = field[index];
      if( name!=null ) f.name = name;
      if( datatype!=null ) f.datatype=f.typeFits2VOTable(datatype);
      if( unit!=null ) f.unit=unit;
      if( ucd!=null ) f.ucd=ucd;
      if( width>=0 ) f.width= width==0? null : width+"";
      f.computeColumnSize();
      computed[index] = true;

      return res;
   }
   
   /** J'ajuste le numéro du champ dans le cas où il y aurait des champs non visibles avant */
   protected int getRealFieldNumber(int nField) {
      if( nField==-1 ) return nField;
      int nVisible=0;
      int nInvisible=0;
      
      if( nField==0 ) {
         for( int i=0; !field[i].visible; i++ ) nInvisible++;
      } else {
         for( int i=0; nVisible<nField; i++ ) {
            if( !field[i].visible ) nInvisible++;
            else nVisible++;
         }
      }
      return nField+nInvisible;
   }

   /** Change le champ qui porte le tri */
   protected boolean switchSort(int nField) {
      
     // Quel était l'ancien tri posé sur ce champ ?
      int sort = field[nField].sort;

      // Je supprime un éventuel tri précédemment posé sur un autre champ
      clearSort();

      // Je positionne le nouveau tri sur le champ et je le retourne
      field[nField].sort = sort==Field.SORT_ASCENDING ?
                                    Field.SORT_DESCENDING : Field.SORT_ASCENDING;

      sorted=true;
      return field[nField].sort==Field.SORT_ASCENDING;
   }

   /** Positionnement d'un tri particulier sur le champ spécifié */
   protected void setSort(int nField,int sort) {
      clearSort();
      if( nField>=0 ) field[nField].sort=sort;
      sorted=sort!=Field.UNSORT;
   }

   /** Suppression des flags de tri sur tous les champs */
   protected void clearSort() {
      for( int i=0; i<field.length; i++ ) field[i].sort=Field.UNSORT;
      sorted=false;
   }

   
   /** retourne true s'il s'agit d'une légende comportant un point de SED */
   protected boolean isSED() {
      for( Field f : field ) {
         if( f.sed!=0 ) return true;
      }
      return false;
   }

   /** Retourne true s'il y a un champ trié */
   protected boolean isSorted() { return sorted; }
   

   /** Retourne le nombre de champs de la légende */
   public int getSize() { return field.length; }

   /** Retourne l'index du champ "matchant" le nom name, sinon -1 */
   protected int matchColIndex(String name) {
      for( int i=0; i<field.length; i++ ) {
         if( field[i].name!=null && Util.matchMask(name, field[i].name) ) return i;
      }
      return -1;
   }

   /** Retourne l'index du champ "matchant" le nom name, sinon -1
    * ==> Ne tient pas compte des majuscules/minuscules */
   protected int matchIgnoreCaseColIndex(String name) {
      for( int i=0; i<field.length; i++ ) {
         if( field[i].name!=null && Util.matchMaskIgnoreCase(name, field[i].name) ) return i;
      }
      return -1;
   }
   
   
   /** Retourne true si le champ correspond à une valeur mémorisée comme NULL
    * pour le champ indiqué
    */
   protected boolean isNullValue(String text,int i) {
      if( i>=field.length ) return false;
      Field f = field[i];
      return  f.nullValue!=null && f.nullValue.equals(text.trim());

   }

   /** Retourne l'UCD associee au champ.
    * @param i numero du champ
    * @return l'UCD, "" si erreur ou non decrit
    */
    protected String getUCD(int i) {
       if( i>=field.length ) return null;
       Field f = field[i];
       return   (f.ucd!=null?f.ucd:"");
    }
    
    protected int getPrecision(int i) {
       if( i>=field.length ) return -1;
       Field f = field[i];
       try { return Integer.parseInt(f.precision); }
       catch( Exception e ) {}
       return -1;
    }

  /** Retourne la desc
  /** Retourne le nombre de caracteres associes au champ.
   * @param i numero du champ
   * @return le nombre de caracteres, 10 si non specifie, -1 si erreur
   *
   * DETAIL PAS DROLE: Pour le cas des boutons (associes a l'acces aux archives)
   * la taille est en fait le nombre de caractere du label du bouton +1
   * Malheureusement ce label peut se confondre avec le texte pour un lien.
   * J'ai pris comme element discriminant la presence d'une variable ${..}
   * en supposant que le nom des boutons est constant sur toute la colonne
   * ... je sens que ca me jouera des tours tot ou tard.
   */
   protected int getWidth(int i) {
      if( i>=field.length ) return -1;
      Field f = field[i];
      
      if( f.refText!=null && f.refText.indexOf("${")<0 ) return f.refText.length()+1;
      return f.columnSize;

//      // VOTable compatibility
//      if( f.width !=null ) {
//        if( f.refText!=null && f.refText.indexOf("${")<0 ) return f.refText.length()+1;
//        if( f.width==null ) return 10;
//        try { j=Integer.parseInt(f.width); }
//        catch( Exception e ) { return 10; }
//      }
//      else {
//         if( f.arraysize != null && f.arraysize.trim().length()>0 ) {
//            try {
//               // (thomas) to support arraysize="*"
//               if( f.arraysize.equals("*") ) return 0;
//               if( f.arraysize.endsWith("*") == false ) j = Integer.parseInt(f.arraysize);
//               else j = Integer.parseInt((f.arraysize).substring(0, (f.arraysize).length()-1));
//            } catch( Exception e ) { return -1; }
////        System.out.println("and now the arraysize " + j);
//         }
//      }
//      return j;
   }

  /** Retourne le nombre de caractere (+1) de la chaine refText
   * @param i numero du champ
   * @return 0 si non defini, -1 si erreur
   */
   protected int getRefTextLength(int i) {
      if( i>=field.length ) return -1;
      if( field[i].refText==null ) return 0;
      return field[i].refText.length()+1;
   }

   protected String getName(int i)     { return i>=field.length?null:field[i].name;     }
   protected String getHref(int i)     { return i>=field.length?null:field[i].href;     }
   protected String getGref(int i)     { return i>=field.length?null:field[i].gref;     }
   protected String getRefText(int i)  { return i>=field.length?null:field[i].refText;  }
   protected String getRefValue(int i) { return i>=field.length?null:field[i].refValue; }
   protected String getDataType(int i) { return i>=field.length?null:field[i].datatype; }
   protected boolean hasInfo(int i)    { return i>=field.length?false:i<field.length;   }
   protected boolean isNumField(int i) { return i<0 ? false : i>=field.length ? true : field[i].isNumDataType(); }

   /** Retourne l'indice du premier champ qui a un lien GLU ou HREF, -1 si aucun */
   protected int getFirstLink() {
      if( firstLink!=-2 ) return firstLink;
      for( int i=0; i<field.length; i++ ) {
         if( getHref(i)==null && getGref(i)==null ) continue;
         firstLink=i;
         return firstLink;
      }
      firstLink=-1;
      return firstLink;
   }
   
   /*******************************  Structure de la JTable *******************************/
   
   static final private String [] HEAD = {
      "","Visible","Coo","Name","Description","Unit","Datatype","UCD","Utype","Width","Arraysize","Precision"
   };
   
   static final private int [] WHEAD = { 30, 50, 30,100, 190, 50, 70, 110, -1, 40,40,40 };
   
   static final private int N=0;
   static final private int VISIBLE=1;
   static final private int COO=2;
   static final public int NAME=3;
   static final public int DESCRIPTION=4;
   static final public int UNIT=5;
   static final public int DATATYPE=6;
   static final public int UCD=7;
   static final public int UTYPE=8;
   static final public int WIDTH=9;
   static final public int ARRAYSIZE=10;
   static final public int PRECISION=11;
   
   private JTable table;
   private Aladin aladin;
   public Plan plan=null;
   
   /** Génère le JPanel de la table */
   protected JPanel getTablePanel(Aladin aladin,Plan plan) {
      this.aladin=aladin;
      this.plan=plan;
      JPanel p = new JPanel( new BorderLayout());
      JScrollPane sc = new JScrollPane(createTable());
      p.add(sc,BorderLayout.CENTER);
      return p;
   }
   
   protected JComboBox createCombo() {
      JComboBox combo = new JComboBox();
      combo.setMaximumRowCount(15);
      for( int i=0; i<field.length; i++ ) combo.addItem(field[i].name);
      return combo;
   }

   protected JTable getTable() {
      if( table==null ) createTable();
      return table;
   }
   
   /** Creation de la JTable des rubriques */
   private JTable createTable() {
      table=new JTable(this);
      table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      MyRenderer mr = new MyRenderer();
      MyComboBoxEditor mc = new MyComboBoxEditor();
      int width=0;
      for( int i=0; i<HEAD.length; i++ ) {
         TableColumn tc = table.getColumnModel().getColumn(i);
         if( WHEAD[i]!=-1 ) tc.setMinWidth(WHEAD[i]);
         if( i==COO ) tc.setCellEditor(mc);
         if( i!=VISIBLE ) tc.setCellRenderer(mr);
         width+=WHEAD[i];
      }
      int height = Math.min(getRowCount(),15)*16;
      table.setPreferredScrollableViewportSize(new Dimension(width+50,height));
      return table;
   }
   
   class MyComboBoxEditor extends DefaultCellEditor {
      public MyComboBoxEditor() {
         super(new JComboBox(Field.COOSIGN) );
      }
   }  

   public String getColumnName(int col) { return HEAD[col]; }
   public int getColumnCount() { return HEAD.length; }
   public int getRowCount() { return field.length; }
   
   public Class<?> getColumnClass(int col) {
      if( col==VISIBLE ) return (new Boolean(true)).getClass();
      return super.getColumnClass(col);
   }

   public Object getValueAt(int row, int col) {
      switch(col) {
         case N:           return (row+1)+"";
         case COO:         return field[row].getCooSignature();
         case VISIBLE:     return new Boolean(field[row].visible);
         case NAME:        return field[row].name;
         case UNIT:        return field[row].unit;
         case DESCRIPTION: return field[row].description;
         case UCD:         return field[row].ucd;
         case UTYPE:       return field[row].utype;
         case DATATYPE:    return Field.typeFits2VOTable(field[row].datatype);
         case WIDTH:       return field[row].width;
         case ARRAYSIZE:   return field[row].arraysize;
         case PRECISION:   return field[row].precision;
      }
      return "";
   }
   
   public boolean isCellEditable(int row, int col) { return col>0; }
   public void setValueAt(Object value,int row, int col) {
      switch(col) {
         case NAME:        field[row].name = (String)value; break;
         case COO:         String s = (String) value; 
                           if( !s.equals( field[row].getCooSignature() ) ) {
                              int coo = Util.indexInArrayOf(s, Field.COOSIGN);
                              modifyRaDecXYField(row,coo);
                           }
                           break;
         case VISIBLE:     field[row].visible = ((Boolean)value).booleanValue(); break;
         case UNIT:        field[row].unit = (String)value; break;
         case DESCRIPTION: field[row].description = (String)value; break;
         case UCD:         field[row].ucd = (String)value; break;
         case UTYPE:       field[row].utype = (String)value; break;
         case DATATYPE:    field[row].datatype = Field.typeVOTable2Fits( (String)value); break;
         case WIDTH:       field[row].width = (String)value; field[row].computeColumnSize(); break;
         case ARRAYSIZE:   field[row].arraysize = (String)value; field[row].computeColumnSize(); break;
         case PRECISION:   field[row].precision = (String)value; break;
      }
      aladin.mesure.redisplay();
   }
   
   class MyRenderer extends DefaultTableCellRenderer {

      public Component getTableCellRendererComponent(JTable table,Object value,
            boolean isSelected,boolean hasFocus, int row, int col ) {
         Component cell = super.getTableCellRendererComponent(table,value,isSelected,hasFocus,row,col);
         if( !(cell instanceof JLabel ) ) return cell;
         
         JLabel j = (JLabel)cell;
         
         if( col==N ) j.setFont(j.getFont().deriveFont(Font.ITALIC));
         else if( col==NAME ) j.setFont(j.getFont().deriveFont(Font.BOLD));
         else j.setFont(j.getFont().deriveFont(Font.PLAIN));

         if( col==N || col==DATATYPE  || col==WIDTH || col==ARRAYSIZE ) j.setHorizontalAlignment(JLabel.CENTER);
         else j.setHorizontalAlignment(JLabel.LEFT);
         
         return cell;
      }
   }
   
   /** Modification des champs utilisés pour la position céleste ou cartésienne */
   public void modifyRaDecXYField(int index, int coo) {
      if( plan==null || plan.pcat==null ) return;
      plan.hasPM=-1;
      
      // Pour les coordonnées célestes
      if( coo==Field.RA || coo==Field.DE || coo==Field.PMRA || coo==Field.PMDE ) {
         int nra=-1,ndec=-1,npmra=-1,npmde=-1;
              if( coo==Field.RA )   nra=index;
         else if( coo==Field.DE )   ndec=index;
         else if( coo==Field.PMRA ) npmra=index;
         else if( coo==Field.PMDE ) npmde=index;

         for( int i=0; i<field.length; i++ ) {
            Field f = field[i];
            if( f.coo==Field.RA ) {
               if( coo==Field.RA ) f.coo=0;
               if( nra==-1 )  nra=i;
            }
            if( f.coo==Field.DE ) { 
               if( coo==Field.DE ) f.coo=0;
               if( ndec==-1 ) ndec=i;
            }
            if( f.coo==Field.PMRA ) { 
               if( coo==Field.PMRA ) f.coo=0;
               if( npmra==-1 ) npmra=i;
            }
            if( f.coo==Field.PMDE ) { 
               if( coo==Field.PMDE ) f.coo=0;
               if( npmde==-1 ) npmde=i;
            }
            if( f.coo==Field.X || f.coo==Field.Y ) f.coo=0;
         }
         field[index].coo = coo;
//         System.out.println("nra="+nra+" ndec="+ndec);
         if( nra>=0 && ndec>=0 && coo!=0 ) plan.modifyRaDecField(this, nra, ndec,npmra,npmde);
         
      // Pour les coordonnées cartésiennes
      } else {
         int nx=-1,ny=-1;
         if( coo==Field.X ) nx=index;
         else if( coo==Field.Y ) ny=index;

         for( int i=0; i<field.length; i++ ) {
            Field f = field[i];
            if( f.coo==Field.X ) {
               if( coo==Field.X ) f.coo=0;
               if( nx==-1 )  nx=i;
            }
            if( f.coo==Field.Y ) { 
               if( coo==Field.Y ) f.coo=0;
               if( ny==-1 ) ny=i;
            }
            if( f.coo==Field.RA || f.coo==Field.DE || f.coo==Field.PMRA || f.coo==Field.PMDE) f.coo=0;
         }
         field[index].coo = coo;
//         System.out.println("nx="+nx+" ny="+ny);
         if( nx>=0 && ny>=0 && coo!=0 ) plan.modifyXYField(this, nx, ny);
         
      }
      
      fireTableDataChanged();
   }
   
   // Juste pour de debuging
   public String toString() { return field[0].name+" "+field[0].ucd+" ..."; }
}
