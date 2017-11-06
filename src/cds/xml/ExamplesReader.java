package cds.xml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;
import java.util.Stack;

import cds.aladin.Aladin;


/**
 * Reader for an xhtml stream.
 * We are only interested reading "example"
 * For now, closing tag of parent tag with vocab is not detected, only the starting of this tag is detected.
 * //discussion with Markus Demleitner at the ADASS/IVOA interop Santiago 2017: no need to check for vocab. ( we were not able to see gavo examples without it.)
 * 
 * <div vocab="ivo://ivoa.net/std/DALI-examples"> - not checked
 * 		<div typeof="example"> - this is what we look for
 * 			<div property="name"> </div>
 * 			<div property="query"> </div>
 * 		</div>
 * </div>
 */
public class ExamplesReader {
	
     public static void main (String[] args){
             try {
//         		URL address = new URL("http://130.79.129.54:8080/mySimple.xhtml");//works
//         		URL address = new URL("http://130.79.129.54:8080/simbadExamples.xhtml");//works
         		
//         		URL address = new URL("http://130.79.129.54:8080/vizierExamples.xml");//works
         		// Open the address and create a BufferedReader with the source code.
         		URL address = new URL("http://130.79.129.54:8080/griTapExamples.1");
				new ExamplesReader().parse(address.openStream());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
     }
     
     public char prevChar;  
     public boolean isInTag = false;
     public boolean isInEndTag = false;
     public boolean isInAttri = false;
     public boolean isAttriStart = false;
     public boolean isAttributeValue = false;
     public char attributeEndChar;
     public boolean doReadContent = false;
     public String attriName;
     public String attriValue;
     public StringBuffer content = new StringBuffer();
     public StringBuffer tag = new StringBuffer();
     public Map attributes = new HashMap();
     private Stack tagStack;	
     
	public Map<String, String> parse(InputStream is) throws IOException {
		int charNext;
		
		InputStreamReader pageInput = new InputStreamReader(is);
		BufferedReader source = new BufferedReader(pageInput);
		DaliExamplesReader reader = new DaliExamplesReader();
		tagStack = new Stack<Object>();
		
		while ((charNext = source.read()) != -1) {
			char characterRead = (char) charNext;
			if (characterRead == '<') {
				if (content.length() > 0) {
					reader.characters(content);
					content = new StringBuffer();
				}
				isInTag = true;
				tag = new StringBuffer();
			} else if (characterRead == '!' && prevChar == '<') {
				isInTag = false;
				isInAttri = false;
				tag = new StringBuffer();
				doReadContent = false;
				isInEndTag = false;
			} else if ((isInTag || isInAttri) && (characterRead == '>' && prevChar == '/')) {
				isInTag = false;
				isInAttri = false;
				reader.startElement(tag.toString(), attributes);
				attributes.clear();
				reader.endElement(tag.toString());
				tag = new StringBuffer();
			} else if ((isInTag || isInAttri) && characterRead == '>') {
				isInAttri = false;
				isInTag = false;
				tagStack.push(tag.toString());
				reader.startElement(tag.toString(), attributes);
				attributes.clear();
				content = new StringBuffer();
				doReadContent = true;
			} else if (characterRead == '/' && prevChar == '<') {
				tag = new StringBuffer();
				isInTag = false;
				isInEndTag = true;
				isInAttri = false;
				doReadContent = false;
				if (!tagStack.isEmpty()) {
					if (content.length() > 0) {
						reader.characters(content);
						content = new StringBuffer();
					}
				}
				
			} else if (isInEndTag && characterRead == '>') {
				isInEndTag = false;
				if (!tagStack.isEmpty()) {
//					if (!tag.toString().equalsIgnoreCase(tagStack.peek().toString())) {
//						System.err.println("yikes it is not same");
//					}
					tagStack.pop();
					reader.endElement(tag.toString());
				}
				doReadContent = true;
			} else if (isInTag && characterRead == ' ') {
				isInAttri = true;
				isAttriStart = true;
				content = new StringBuffer();
				isInTag = false;
			}  else {
				if (isInTag || isInEndTag) {
					if (characterRead != '>' && characterRead != '/') {
						tag.append(characterRead);
					}
				} else if (isInAttri) {
					if (isAttriStart && (characterRead == ' ' || characterRead == '=') && content.toString().trim().length() > 0) {
						attriName = content.toString();
						attriName = attriName.replaceAll("\\s", "");
						content = new StringBuffer();
						attributeEndChar = ' ';
						isAttriStart = false;
						isAttributeValue = true;
					} else if (isAttributeValue && (characterRead == '=')) {
						isAttributeValue = true; // do nothing
					} else if (isAttributeValue) {
						if (attriName != null && characterRead == attributeEndChar && content.length() > 0){
							attriValue = content.toString();
							attributes.put(attriName, attriValue);
							isAttributeValue = false;
							isAttriStart = true;
							content = new StringBuffer();
							attriName = null;
							attriValue = null;
						} else if (content.length() == 0 && (characterRead == '\'' || characterRead == '"')) {
							attributeEndChar = characterRead;
						} else {
							content.append(characterRead);
						}
						
					} else {
						content.append(characterRead);
					}
					
				} else if (doReadContent) {
					content.append(characterRead);
				}
			}
			prevChar = characterRead;
		}
//		System.out.println(attributes);
//		System.out.println(content);
//		for (String object : reader.examples.keySet()) {
//			System.out.println(object+":::: "+reader.examples.get(object));
//			System.out.println("==========================");
//		}
		pageInput.close();
		source.close();
		Aladin.trace(3, "Read these many examples "+reader.examples.size());
//		System.out.println("Total size:" +reader.examples.size());
		return reader.examples;
	}
	
	class DaliExamplesReader{
		//tag flags
		private boolean inExamplesTag = false;
		private boolean inExampleTag = false;
		private boolean inNameTag = false;
		private boolean inQueryTag = false;
		private String exampleTag = null;
		private String nameTag = null;
		private String queryTag = null;
		
		private String exampleLabel = null;
		
		private boolean wait = false; 
		private Stack tagStack;	
		private StringBuffer content = null;
		
		public DaliExamplesReader() {
			// TODO Auto-generated constructor stub
			inExamplesTag = true;
			examples = new Hashtable<String, String>();
		}
		
		//data
		private Map<String, String> examples;
		
		public void startElement(String qName, Map<String, String> attributes){
			// TODO Auto-generated method stub
//			super.startElement(uri, localName, qName, attributes);
			//discussion with Markus Demleitner at the ADASS/IVOA interop Santiago 2017: no need to check for vocab. ( we were not able to see gavo examples without it.)
			/*if (attributes.get("vocab") != null && attributes.get("vocab").equals("ivo://ivoa.net/std/DALI-examples")) {
				inExamplesTag = true;
				examples = new Hashtable<String, String>();
			} else */if (inExamplesTag) {
				if (inExampleTag) {
					tagStack.push(qName);
					if (attributes.get("property") != null) {
						String propertyValue = (String) attributes.get("property");
						if (propertyValue.equalsIgnoreCase("name") || propertyValue.equalsIgnoreCase("name")) {
							inNameTag = true;
							nameTag = qName;
						} else if (propertyValue.equals("query")) {
							inQueryTag = true;
							queryTag = qName;
						}
					}
				} else if (attributes.get("typeof") != null && attributes.get("typeof").equals("example")) {
					inExampleTag = true;
					tagStack = new Stack<String>();
					tagStack.push(qName);
					exampleTag = qName;
					content = new StringBuffer();
				}
			}
		}

		public void endElement(String qName){
			
			if (inExampleTag) {
				String tagLeft = (String) tagStack.pop();
				if (tagStack.isEmpty() && tagLeft.equals(exampleTag)) {
					inExampleTag = false;
				} else if (inNameTag && tagLeft.equals(nameTag)) {
					inNameTag = false;
				} else if (inQueryTag && tagLeft.equals(queryTag)) {
					if (exampleLabel == null) {
						exampleLabel = "Service provided example ";
						exampleLabel = generateQueryName(exampleLabel);
					}
					String value = XMLParser.XMLDecode(content.toString());
					if (value != null) {
						examples.put(exampleLabel, value);
					}
					inQueryTag = false;
					resetInExampleFlags();
				}
			}
		}
		

		public void characters(StringBuffer ch) {
			// TODO Auto-generated method stub
			if (ch.length() > 0) {
				String data = ch.toString();
				if (inExamplesTag) {
					if (inExampleTag) {
						if (inNameTag) {
							exampleLabel = data;
							inNameTag = false;
						} else if (inQueryTag) {
							content.append(data);
//							resetFlags();//this is because we assume this reading order. for now we do not look for anything other than this order
						}
					}
				}
			}
		}
		
		/**
		 * Method to generate a unique <i>upload</i> table name
		 * 
		 * @param uploadFrame
		 * @return
		 */
		public String generateQueryName(String prefix) {
			String uploadTableName = prefix + new Random().nextInt(Integer.SIZE - 1);
			if (!examples.containsKey(uploadTableName)) {
				return uploadTableName;
			}
			return generateQueryName(prefix);
		}
		
		/**
		 * Method to resetFlags
		 */
		private void resetInExampleFlags() {
			inExampleTag = false;
			inNameTag = false;
			inQueryTag = false;
			exampleLabel = null;
			content = new StringBuffer();
		}

		public Map<String, String> getExamples() {
			if (examples != null && examples.isEmpty()) {
				examples = null;
			}
			return examples;
		}

	} 
}