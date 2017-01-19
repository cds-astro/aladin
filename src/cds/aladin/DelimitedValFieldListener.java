package cds.aladin;

import static cds.aladin.Constants.EMPTYSTRING;

import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTextField;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

/**
 * This FocusListener is very specific listener to have a textField with inputs such as "value1 and value2"
 * -here 'and' would be the delimiter. 
 * Class provides error highlighting whenever the constraint is violated(no delimiter/only 1 value)
 * The validity of input to the text field is processed at FocusLost. This is available as isValid.
 *
 */
public class DelimitedValFieldListener{
	
	private String delimiter = EMPTYSTRING;
	public String REGEX_BETWEENINPUT = "([A-Za-z0-9]+)\\s+%1$s\\s+([A-Za-z0-9]+)";
	private String toolTipText = "Format should be as: value1 %1$s value2";
	private boolean isValid = true;
	
	public DelimitedValFieldListener() {
		// TODO Auto-generated constructor stub
	}
	
	public DelimitedValFieldListener(String delimiter) {
		// TODO Auto-generated constructor stub
		if (delimiter!=null) {
			this.delimiter = delimiter;
		}
		REGEX_BETWEENINPUT = String.format(REGEX_BETWEENINPUT, this.delimiter);
		toolTipText = String.format(toolTipText, this.delimiter);
	}

	/*@Override
	public void focusGained(FocusEvent e) {
		// TODO Auto-generated method stub

	}

	@Override
	public void focusLost(FocusEvent e) {
		// TODO Auto-generated method stub
		setValid(andConstraint(((JTextField) e.getSource()), REGEX_BETWEENINPUT, toolTipText));
	}*/
	
	public static boolean andConstraint(JTextField constraint, String checkPattern, String toolTipText) {
		String selectedText = constraint.getText();
		Highlighter highlighter = constraint.getHighlighter();
		Pattern re = Pattern.compile(checkPattern, Pattern.CASE_INSENSITIVE);
		Matcher m = re.matcher(selectedText);
		boolean result = false;
		if (!m.find()) {
			constraint.setToolTipText(toolTipText);
			HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Aladin.LIGHTORANGE);
			try {
				highlighter.addHighlight(0,selectedText.length(),painter);
			} catch (BadLocationException e1) {
				e1.printStackTrace();
				//Don't do anything if this feature fails
			}
//			constraint.grabFocus();
		} else {//reset error decorations
			result = true;
			highlighter.removeAllHighlights();
			constraint.setToolTipText(EMPTYSTRING);
		}
		return result;
	}
	
	public String getDelimiter() {
		return delimiter;
	}

	public void setDelimiter(String delimiter) {
		this.delimiter = delimiter;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

	public String getToolTipText() {
		return toolTipText;
	}

	public void setToolTipText(String toolTipText) {
		this.toolTipText = toolTipText;
	}
	
	 // Pour le traitement de l'expression en cours
	   class Expr {
	      char [] s;   // La chaine à traiter
	      int pos;     // La position courante
	      
	      // Initialisation de l'expression à mémoriser
	      Expr(String expr) { s=expr.toCharArray(); pos=0; }
	      
	      // Retourne true si la position courante est en fin de chaine
	      boolean isEnd() { return pos>=s.length; }
	      
	      // Retourne le caractère courant ou 0 si fin de chaine
	      char ch() { return isEnd() ? 0 : s[pos]; }
	      
	      // Retourne la sous-chaine indiquées
	      String get(int deb,int count) { return new String(s,deb,count); }
	      
	      // Affiche la chaine en indiquant l'emplacement courant
	      String error() { return new String(s,0,pos)+" ?? "+new String(s,pos,s.length-pos); }
	      
	      // Passe les blancs
	      void skipBlank() {
	         while( pos<s.length && Character.isSpaceChar( s[pos] ) ) pos++;
	      }
	   }
	
}
