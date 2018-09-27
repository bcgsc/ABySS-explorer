package ca.bcgsc.abyssexplorer.gui;

import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 * The ABySS-Explorer menu class.
 * 
 * @author Cydney Nielsen
 *
 */
public class Menu {
	
	protected JMenuBar menuBar;
	
	protected JMenu fileMenu;
	protected JMenuItem openItem;
	// protected JMenuItem exportItem;
	protected JMenuItem quitItem;
	
	protected File fileToOpen = null;
	// protected File fileToExport;
	
	public Menu() {
		menuBar = new JMenuBar();
		addFileMenu();
	}

	public void addOpenListener(ActionListener l) {
		openItem.addActionListener(l);
	}
	
	// public void addExportListener(ActionListener l) {
	// 	exportItem.addActionListener(l);
	// }
	
	public void addQuitListener(ActionListener l) {
		quitItem.addActionListener(l);
	}
	
	public JMenuBar getMenuBar() {
		return menuBar;
	}
	
	protected void addFileMenu() {
    	fileMenu = new JMenu("File");
    	
    	openItem = new JMenuItem("Open...");
        fileMenu.add(openItem);
        
		// exportItem = new JMenuItem("Export EPS...");
		// fileMenu.add(exportItem);
		
		quitItem = new JMenuItem("Quit");
		fileMenu.add(quitItem);
        
    	menuBar.add(fileMenu);
	}
	
	public File chooseFileToOpen() {
		JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		JFrame frame = new JFrame();
		Integer returnVal = chooser.showOpenDialog(frame);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			fileToOpen = new File(chooser.getSelectedFile().getAbsolutePath());
		}
		return fileToOpen;
	}

	// public File chooseExportFile() {
	// 	JFileChooser chooser = new JFileChooser();
	// 	JFrame frame = new JFrame();
    //     int returnVal = chooser.showSaveDialog(frame);
    //     if (returnVal == JFileChooser.APPROVE_OPTION) {
    //      	fileToExport = new File(chooser.getSelectedFile().getAbsolutePath());
	//	}
    //    return fileToExport;
	//}
	
}
