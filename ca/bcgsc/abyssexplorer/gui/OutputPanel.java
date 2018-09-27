package ca.bcgsc.abyssexplorer.gui;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

/**
 * The ABySS-Explorer text output panel class.
 * 
 * @author Cydney Nielsen
 *
 */
public class OutputPanel {
	
    protected JScrollPane scrollPane;
    protected JTextPane textPane;   
    protected StyledDocument doc;
    
    public OutputPanel() {
    	textPane = new JTextPane();
    	scrollPane = new JScrollPane(textPane);
    	scrollPane.setVerticalScrollBarPolicy(
    			JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    	scrollPane.setPreferredSize(new Dimension(250, 155));
    	scrollPane.setMinimumSize(new Dimension(10, 10));
    	
    	doc = textPane.getStyledDocument();
    	Style def = StyleContext.getDefaultStyleContext().
    	getStyle(StyleContext.DEFAULT_STYLE);

    	Style regular = doc.addStyle("regular", def);
    	StyleConstants.setFontFamily(def, "SansSerif");
    	
    	// Style s = doc.addStyle("italic", regular);
    	// StyleConstants.setItalic(s, true);

    	Style bold = doc.addStyle("bold", regular);
    	StyleConstants.setBold(bold, true);
    	
    	Style selected = doc.addStyle("selected", bold);
    	StyleConstants.setForeground(selected, new Color(51,160,44));//new Color(217, 95, 14));
    	
    	Style missing = doc.addStyle("missing", bold);
    	StyleConstants.setForeground(missing, new Color(150, 150, 150));
    	
    }
    
    public JScrollPane getScrollPane() {
    	return scrollPane;
    }
    
    public void addText(String v, String f) {
    	String[] values = v.split(",");
    	String[] formats = f.split(",");
    	if (values.length != formats.length) {
    		throw new IllegalArgumentException("Problem formating path string: Different number of values and formats.");
    	}
    	try {
    		for (int i=0; i < values.length; i++) {
    			if (i == values.length-1) {
    				doc.insertString(doc.getLength(), values[i]+"\n", doc.getStyle(formats[i]));
    			} else {
    				doc.insertString(doc.getLength(), values[i], doc.getStyle(formats[i]));
    			}
    		}
                textPane.setCaretPosition(0);
    	} catch (BadLocationException ble) {
    		throw new IllegalArgumentException("Problem formating path string");
    	}
    }
    
    public void setText(String v, String f) {
    	clear();
    	addText(v,f);
    }
    
    protected void clear() {
    	try {
    		doc.remove(0, doc.getLength());
    	} catch (BadLocationException ble) {
    		throw new IllegalArgumentException("Problem clearing text output");
    	}
    }

    public String getText(){
        try {
            return doc.getText(0, doc.getLength());
    	} catch (BadLocationException ble) {
            return null;
    	}
    }
}
