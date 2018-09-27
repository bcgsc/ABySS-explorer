package ca.bcgsc.abyssexplorer.gui;

import java.awt.BorderLayout;
import java.awt.event.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;
import ca.bcgsc.abyssexplorer.graph.AbyssGraph2.ShortestPathNeighborhood;
import ca.bcgsc.abyssexplorer.graph.ContigLabel;
import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;
import ca.bcgsc.abyssexplorer.parsers.GraphLoader;
import ca.bcgsc.dive.dive.Dive;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.algorithms.layout.util.Relaxer;
import edu.uci.ics.jung.algorithms.layout.util.VisRunner;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.layout.PersistentLayoutImpl;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Hashtable;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import org.apache.batik.dom.GenericDOMImplementation;
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.svggen.SVGGraphics2DIOException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;

/**
 * Main class for the ABySS-Explorer application.
 * Controls communication between all GUI 
 * components and the underlying graph 
 * data structure.
 * 
 * @author Cydney Nielsen
 * @author Jason Chang
 *
 */
public class AbyssExplorer {
	
//	protected JFrame rootFrame;
//	protected final int minWidth = 800;
//	protected final int minHeight = 600;
	
	// GUI components
	protected AbyssVisualizationViewer vv;
//	protected Menu menu;
	//protected ControlPanel panel;
	protected OutputPanel output;
	
        protected Controller controller;

	// Data file handling
	protected GraphLoader graphLoader;
	
	// Keep track of the graph display
	protected boolean activeDisplay = false;//true; //for JUNG 2.0.1
	
        protected JPanel rootPanel = null;
        protected File fileOpened = null;

        protected Dive dive = null;
        protected int k = -1;
        protected int minExpForSlider = 2;
        protected int maxExpForSlider = 3;
        
        protected JLabel inPartnerListLabel = null;
        protected JLabel outPartnerListLabel = null;
        protected JTable inPartnerTable = null;
        protected JTable outPartnerTable = null;

        protected JLabel partnerListTitle = null;
        protected JComboBox peComboBox = null;
        protected JLabel peLabel = null;
        protected JSplitPane bottomSplitPane = null;

        protected JPopupMenu viewerMenu = null;
        protected MouseListener listener = null;
//        protected JCheckBox pePartnerToggle = null;
//        protected JCheckBox peListToggle = null;
        protected JSlider slider = null;

        protected JCheckBox labelsToggle = null;
        protected JCheckBox lenToggle = null;
        protected JSlider lenSlider = null;
        protected JRadioButton peRadioButton = null;
        protected JRadioButton seRadioButton = null;
        protected JRadioButton scaffoldRadioButton = null;
        protected JRadioButton showAllRadioButton = null;
        protected JRadioButton showNeighborsRadioButton = null;
        protected JSlider stepSizeSlider = null;

        protected boolean isClear = true;
        protected JPanel topPanel = null;
//        protected Boolean wasPairedEndMode = null;
        protected Boolean trueNeighborsOnly = null;
        protected Integer lastStepSize = null;
        protected Integer seedId = null;
        protected String[] pathTerminalEdges = null;
        
        protected final static DisabledRenderer disabledRenderer = new DisabledRenderer();
        protected List<Integer> indexesOfInPartnersNotShown = new ArrayList<Integer>();
        protected List<Integer> indexesOfOutPartnersNotShown = new ArrayList<Integer>();
        protected boolean layoutPaused = false; // flag whether the layout is paused

        private final JFileChooser fc = new JFileChooser();

        protected int[] stepSizes;
        
        protected int lastAssemblyMode = -1;
        protected int currentAssemblyMode = -1;
        
//        public void launch() {
//            launch(false);
//        }

//	public void launch(boolean fromExternalSource) {
//                this.fromExternalSource = fromExternalSource;
//                if(rootFrame != null){
//                    rootFrame.setVisible(true);
//                    return;
//                }
//
//		String title = "ABySS-Explorer";
//		rootFrame = new JFrame(title);
//
//		Toolkit tk = Toolkit.getDefaultToolkit();
//		Dimension screen = tk.getScreenSize();
//		screen.height = Math.max(0, screen.height);
//		screen.width = Math.max(0, screen.width);
//		rootFrame.setPreferredSize(new Dimension(screen.width, screen.height));
//
//                java.awt.Container contentPane = rootFrame.getContentPane();
//		addGraphLoader();
//		addMenu();
//                //contentPane.add(addControlPanel(), BorderLayout.LINE_END);
//                contentPane.add(addOutputPanel(), BorderLayout.PAGE_END);
//
//		// to control frame resizing
//		rootFrame.addComponentListener(new ComponentListener() {
//			public void componentHidden(ComponentEvent e) { }
//			public void componentMoved(ComponentEvent e) { }
//			public void componentResized(ComponentEvent e) {
//				Dimension d = rootFrame.getSize();
//				if (d.getWidth() < minWidth || d.getHeight() < minHeight) {
//					rootFrame.setSize(new Dimension(minWidth,minHeight));
//				}
//			}
//			public void componentShown(ComponentEvent e) {	}
//		});
//		rootFrame.pack();
//
//                if(fromExternalSource){
//                    // Since we are launching Explorer from DIVE, we don't want to terminate DIVE
//                    // as well when the user closes the window for Explorer
//                    rootFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
//                }
//                else {
//                    rootFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//                }
//		rootFrame.setVisible(true);
//
//	}

        public AbyssExplorer(){
            this(null);
        }

        public AbyssExplorer(Dive dive){
            this.dive = dive;
            fc.setAcceptAllFileFilterUsed(false);
            fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG image", "png"));
            fc.addChoosableFileFilter(new FileNameExtensionFilter("SVG image", "svg"));

        }

        public JPanel launchInPanel(JCheckBox labelsToggle,
            JCheckBox lenToggle,
            JSlider lenSlider,
//            JCheckBox pePartnerToggle,
//            JCheckBox peListToggle,
            JRadioButton peRadioButton,
            JRadioButton seRadioButton,
            JRadioButton showAllRadioButton,
            JRadioButton stepSizeRadioButton,
            JSlider stepSizeSlider,
            JRadioButton scaffoldRadioButton) {

            this.labelsToggle = labelsToggle;
            this.lenToggle = lenToggle;
            slider = lenSlider;
//            this.pePartnerToggle = pePartnerToggle;
//            this.peListToggle = peListToggle;
            this.peRadioButton = peRadioButton;
            this.seRadioButton = seRadioButton;
            this.showAllRadioButton = showAllRadioButton;
            this.showNeighborsRadioButton = stepSizeRadioButton;
            this.stepSizeSlider = stepSizeSlider;
            this.scaffoldRadioButton = scaffoldRadioButton;

            stepSizes = dive.getStepSizeDictionary();

            if(rootPanel == null){
                rootPanel = new JPanel();
                rootPanel.setLayout(new BorderLayout());

                addGraphLoader();
                topPanel = addTopPanel();
                JSplitPane p = addBottomPanel();

                rootPanel.add(topPanel, BorderLayout.PAGE_START);
                rootPanel.add(p, BorderLayout.PAGE_END);

                controller = new Controller(labelsToggle,
                        lenToggle,
                        lenSlider,
//                        pePartnerToggle,
                        inPartnerListLabel,
                        inPartnerTable,
                        outPartnerListLabel,
                        outPartnerTable,
//                        peListToggle,
                        peComboBox,
                        partnerListTitle,
                        peLabel,
                        peRadioButton,
                        seRadioButton,
                        scaffoldRadioButton);
                setUpController();

                viewerMenu = createViewerPopupMenu();
                
                //rootPanel.add(splitPane);
            }
            
            return rootPanel;
        }

        public void addMouseListener(MouseListener l){
            listener = l;
        }

	/**
	 * Initialize graph loader and hook-up necessary
	 * listeners
	 */
	protected void addGraphLoader() {
		graphLoader = new GraphLoader();
                graphLoader.setK(k);
		graphLoader.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				// update the panel text with graph loader status
				//output.setText(graphLoader.getStatus(), "regular");
				if (graphLoader.getStatus().equals("complete")) {
					AbyssGraph2 g = graphLoader.getParsedGraph();
					try {
						initializeGraph(g);
						// set the parser to null
					} catch (IllegalArgumentException ex) {
                                            JFrame owner = dive;
                                            if(dive == null){
                                                owner = new JFrame();
                                            }
						JOptionPane.showMessageDialog(owner, ex,
								"ERROR", JOptionPane.ERROR_MESSAGE);
					}
				}
                                else{
                                    dive.setStatus(graphLoader.getStatus(), false, Dive.NORMAL_STATUS);
                                }
			}
		});
	}

        private void setUpLengthSlider(){
            AbyssGraph2 g = graphLoader.getParsedGraph();
            int mid = graphLoader.getIdOfLongestContig();
            int max = g.getEdge(mid).getLen();
            int sid = graphLoader.getIdOfShortestContig();
            int min = g.getEdge(sid).getLen();

            minExpForSlider = Integer.toString(min).length();
            maxExpForSlider = Integer.toString(max).length();

            if(maxExpForSlider == minExpForSlider){
                maxExpForSlider = minExpForSlider + 1;
            }

            slider.setMinimum(0); //0%
            slider.setMaximum(100); //100%
            Hashtable<Integer, JLabel> table = new Hashtable<Integer, JLabel>();
            table.put(0, new JLabel("<html>10<sup>"+ minExpForSlider+"</sup></html>"));
            table.put(100, new JLabel("<html>10<sup>"+ maxExpForSlider+"</sup></html>"));
            slider.setLabelTable(table);

            vv.setLenScale((int)Math.rint(Math.pow(10, controller.getLenSliderValue()/100.0*(maxExpForSlider-minExpForSlider)+minExpForSlider) ));
            //slider.repaint();
        }


	
//	/**
//	 * Builds the Menu component and hooks up all
//	 * necessary listeners
//	 */
//	protected void addMenu() {
//		menu = new Menu();
//		// listeners for menu items
//		menu.addOpenListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				File f = menu.chooseFileToOpen();
//				if (f == null) { return; }
//				try {
//					graphLoader.load(f);;
//				} catch (IOException ex) {
//					JOptionPane.showMessageDialog(new JFrame(), ex,
//							"ERROR", JOptionPane.ERROR_MESSAGE);
//				}
//			}
//		});
//		/*menu.addExportListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//				if (vv == null) { return; }
//				File f = menu.chooseExportFile();
//				if (f == null) { return; }
//				try {
//					FileOutputStream outputStream = new FileOutputStream(f);
//					// Create a new document with bounding box 0 <= x <= width and
//					// 0 <= y <= height.
//					EpsGraphics2D g = new EpsGraphics2D("Abyss-Explorer",
//							outputStream, 0, 0, vv.getWidth(),vv.getHeight());
//					vv.paint(g);
//					// Flush and close the document (don't forget to do this!)
//					g.flush();
//					g.close();
//				} catch (FileNotFoundException e1) {
//					JOptionPane.showMessageDialog(new JFrame(), e1,
//							"ERROR", JOptionPane.ERROR_MESSAGE);
//				} catch (IOException e2) {
//					JOptionPane.showMessageDialog(new JFrame(), e2,
//							"ERROR", JOptionPane.ERROR_MESSAGE);
//				}
//			}
//		});*/
//		menu.addQuitListener(new ActionListener() {
//			public void actionPerformed(ActionEvent e) {
//                            if(fromExternalSource){
//                                // Since we are launching Explorer from DIVE, we don't want to terminate DIVE
//                                // as well when the user closes the window for Explorer
//                                rootFrame.setVisible(false);
//                            }
//                            else {
//                                System.exit(0);
//                            }
//			}
//		});
//		rootFrame.getContentPane().add(menu.getMenuBar(), BorderLayout.PAGE_START);
//	}

        protected void applySettings(){
            boolean pListOn = getCurrentAssemblyMode() == Dive.UNITIGS ; //|| getCurrentAssemblyMode() == Dive.CONTIGS;
//            topPanel.setVisible(pListOn);
            if(vv != null){
                if (pListOn) {
                        vv.activatePePathDisplay();
                } else {
                        vv.deactivatePePathDisplay();
                }
            }
            controller.setPeList(pListOn);
            
            switch(getCurrentAssemblyMode()){
                case(Dive.SCAFFOLDS):
                    controller.setPePartnerList(false);
                    break;
                case(Dive.CONTIGS):
                    controller.setPePartnerList(true);
                    break;
                case(Dive.UNITIGS):
                    controller.setPePartnerList(true);
                    break;
                default:
                    controller.setPePartnerList(false);
            }

//            if (pePartnerToggle.isSelected()) {
//                if(vv != null){
//                    vv.activatePartnerDisplay();
//                }
//                controller.setPePartnerList(true);
//                bottomSplitPane.setDividerLocation(0.5);
//            } else {
//                if(vv != null){
//                    vv.deactivatePartnerDisplay();
//                }
//                controller.setPePartnerList(false);
//                bottomSplitPane.setDividerLocation(1.0);
//                BasicSplitPaneUI ui = (BasicSplitPaneUI) bottomSplitPane.getUI();
//                BasicSplitPaneDivider divider = ui.getDivider();
//                JButton button = (JButton) divider.getComponent(1);
//                button.doClick();
//            }

            if (vv != null) {
                updatePathInfo();
                updatePartnerInfo();

                if (lenToggle.isSelected()) {
                        vv.turnContigLenOn();
                } else {
                        vv.turnContigLenOff();
                }

                vv.setLenScale((int)Math.rint(Math.pow(10, slider.getValue()/100.0*(maxExpForSlider-minExpForSlider)+minExpForSlider) ));

                if (labelsToggle.isSelected()) {
                        vv.turnContigLabelsOn();
                } else {
                        vv.turnContigLabelsOff();
                }

                vv.repaint();
            }
        }

	/**
	 * Builds the Control Panel component and hooks up all
	 * necessary listeners
	 */
	protected void setUpController() {
		// listeners for mouse-over in the inbound paired-end partner list
		controller.addInPartnerMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {
				String pePartner = controller.selectInPartnerListItem(e);
				if (activeDisplay) {
					vv.setInHighlightedPartner(pePartner);
					vv.repaint();
				}
			}
			public void mouseDragged(MouseEvent e) {}
		});
		controller.addInPartnerListener(new MouseAdapter() {
                        public void mouseExited(MouseEvent e) {
                                controller.clearInSelectedPartner();
                                if (activeDisplay) {
                                        vv.setInHighlightedPartner(null);
                                        vv.repaint();
                                }
                        }
		});
		// listeners for mouse-over in the outbound paired-end partner list
		controller.addOutPartnerMotionListener(new MouseMotionListener() {
			public void mouseMoved(MouseEvent e) {
				String pePartner = controller.selectOutPartnerListItem(e);
				if (activeDisplay) {
					vv.setOutHighlightedPartner(pePartner);
					vv.repaint();
				}
			}
			public void mouseDragged(MouseEvent e) {}
		});
		controller.addOutPartnerListener(new MouseAdapter() {
			public void mouseExited(MouseEvent e) {
				controller.clearOutSelectedPartner();
				if (activeDisplay) {
					vv.setOutHighlightedPartner(null);
					vv.repaint();
				}
			}
		});
		// listeners for paired-end contig ID list
		controller.addPeListListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String peLabel = controller.selectPeListItem();
				if (peLabel != null) {
					vv.selectPath(peLabel);
				}
			}
		});               
	}

        public boolean isClear(){
            return isClear;
        }

        public int getK(){
            return k;
        }

        public int loadFile(File fileToOpen, int k) throws IOException, InterruptedException {
            return loadFileAndAnchorAtContig(fileToOpen, k, null);
        }

        private int getCurrentAssemblyMode(){
            int assemblyMode = -1;
            if(seRadioButton.isSelected()){
                assemblyMode = Dive.UNITIGS;
            }
            else if(peRadioButton.isSelected()){
                assemblyMode = Dive.CONTIGS;
            }
            else if(scaffoldRadioButton.isSelected()){
                assemblyMode = Dive.SCAFFOLDS;
            }
            return assemblyMode;
        }
        
        /*
         * @return 1 for new file loaded
         *         0 for same file
         *         -1 for errors
         *
         */
        public int loadFileAndAnchorAtContig(File fileToOpen, int k, String id) throws IOException, InterruptedException{
            if (fileToOpen == null) { return -1; }

            layoutPaused = false;

            int returnVal = 0;
            try {                    
//                boolean isPairedEndMode = peRadioButton.isSelected();
                    currentAssemblyMode = getCurrentAssemblyMode();
                
                    boolean newFile = fileOpened == null ||
                                        !fileOpened.equals(fileToOpen) ||
                                        lastAssemblyMode != currentAssemblyMode;
                    boolean newGraphOnly = (trueNeighborsOnly != null && trueNeighborsOnly.booleanValue() != showNeighborsRadioButton.isSelected()) ||
                                                (lastStepSize != null && lastStepSize != stepSizes[stepSizeSlider.getValue()]);

                    String selectedEdgeLabel = null;
                    if(vv != null){
                        Edge selectedEdge = vv.getSelectedEdge();
                        if(selectedEdge != null){
                            selectedEdgeLabel = selectedEdge.getLabel();

                            //if(wasPairedEndMode != null && wasPairedEndMode.booleanValue() && !isPairedEndMode && id == null){
                            if(lastAssemblyMode != -1 && lastAssemblyMode != Dive.UNITIGS && currentAssemblyMode != Dive.UNITIGS && id == null){
                                // PE -> SE
                                id = selectedEdgeLabel;
                            }
                        }
                        
                        //if(id == null && wasPairedEndMode != null && !wasPairedEndMode.booleanValue() && isPairedEndMode){
                        if(id == null && lastAssemblyMode != -1 && lastAssemblyMode == Dive.UNITIGS && currentAssemblyMode != Dive.UNITIGS){
                            // SE -> PE
                            id = vv.getSelectedPathLabel();
                        }
                    }

                    if(newFile){
                        clear();
                        returnVal = 1;
                    }
                    
                    applySettings();                    

                    if(newFile) {                        
                        graphLoader.setK(k);                        
                        graphLoader.load(fileToOpen, currentAssemblyMode);
                        if(fileToOpen.isFile() &&
                                !graphLoader.isPairedEndAssembly() &&
                                graphLoader.getAdjFileType().equals("dot")){  // SE DOT file
                            seRadioButton.setSelected(true);
                            applySettings();
                        }


                        setUpLengthSlider();
                        fileOpened = fileToOpen;
                        lastAssemblyMode = currentAssemblyMode;
                        trueNeighborsOnly = showNeighborsRadioButton.isSelected();
                        lastStepSize = stepSizes[stepSizeSlider.getValue()];
                    }
                    else if(newGraphOnly){
                        trueNeighborsOnly = showNeighborsRadioButton.isSelected();
                        lastStepSize = stepSizes[stepSizeSlider.getValue()];
                        this.initializeGraph(vv.getGraph());
                    }

                    if(id != null) {
                        returnVal = searchContig(id);
                    }
                    else if (!newFile && selectedEdgeLabel != null){
                        vv.clearEdgeState();
                        if(vv != null){
                            AbyssGraph2 g = vv.getGraph();
                            if(g != null){
                                Edge edgeToSelect = g.getEdge(selectedEdgeLabel);
                                if(edgeToSelect != null && vv.getDisplayedGraph().containsEdge(edgeToSelect)){
                                    vv.selectEdge(edgeToSelect);
                                }
                            }
                        }
                    }

                    updatePathInfo();
                    updatePartnerInfo();

                    isClear = false;
                    activeDisplay = true;


                    if(seedId != null){
                        if(newFile){
                            dive.setQuery(vv.getGraph().getEdge(seedId).getLabel(), null);
                        }
                        else{
                            dive.setQuery(vv.getGraph().getEdge(seedId).getLabel());
                        }
                    }
            } catch (IOException ex) {
                throw ex;
//                ex.printStackTrace();
//                    JOptionPane.showMessageDialog(new JFrame(), ex,
//                                    "ERROR", JOptionPane.ERROR_MESSAGE);
//
//                returnVal = -1;
            }

            return returnVal;
        }

        public File getFileOpened(){
            return fileOpened;
        }

        public void clear(){
            graphLoader.clear();
            seedId = null;
            pathTerminalEdges = null;
            isClear = true;
            controller.clear();
//            controller.disableComponents();
            output.clear();
            if(vv != null){
                rootPanel.remove(vv);
                vv = null;
            }
            fileOpened = null;
            progressBar.setValue(progressBar.getMinimum());
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(false);
            k = -1;
            graphLoader.setK(k);
            layoutPaused = false;
            rootPanel.repaint();
        }

//        public void disableSettings(){
//            controller.disableComponents();
//        }
//
//        public void enableSettings(){
//            controller.enableComponenents();
//        }


	private String processQueryId(String id) {
		if (id.trim().length() == 0) { return null; }
		if (id.contains("+") || id.contains("-")) {
			id = id.trim();
		} else {
			id = id.trim() + "+";
		}
		return id;
	}

        public int searchContig(String id) {
            int returnVal = 0;
            try {
//                    if (activeDisplay == false) {
//                            output.setText("Please first open a DOT file.", "regular");
//                    } else {
                            String eLabel = processQueryId(id);
                            AbyssGraph2 g = vv.getGraph();
                            try {
                                    if (g.hasEdge(eLabel)) {
                                            Edge edge = g.getEdge(eLabel);
                                            vv.setSeed(seedId);
                                            PersistentLayoutImpl<Vertex,Edge> pLayout = getQueryLayout(g,edge);
//                                            vv.setVisible(true);
                                            vv.setGraphLayout(pLayout);                                            
                                            vv.selectEdge(edge);
                                    } else  if (g.hasPeContig(eLabel)) {
//                                            vv.setVisible(true);
                                            vv.setSeed(seedId);
                                            ContigLabel peLabel2 = new ContigLabel(eLabel);
                                            // seed the graph layout on the first single-end contig in this path
                                            
                                            ContigLabel firstEdge = null;
                                            for(Object o : g.getPairedEndContigMembers(peLabel2)){
                                                if(o instanceof ContigLabel){
                                                    firstEdge = (ContigLabel) o;
                                                    break;
                                                }
                                            }
                                            //n50plot.addRangeMarker(firstEdge.getLen());
                                            PersistentLayoutImpl<Vertex,Edge> pLayout = getQueryLayout(g,g.getEdge(firstEdge));
                                            vv.setGraphLayout(pLayout);                                            
                                            // only list this paired-end contig in the Control Panel list
                                            List<ContigLabel> mLabels = new ArrayList<ContigLabel>(1);
                                            mLabels.add(peLabel2);
                                            controller.clear();
//                                            controller.setPeListToggleOn();
                                            controller.setPeList(mLabels);
                                            controller.setSelectedPeId(eLabel);
                                            vv.selectQueryPath(peLabel2);
                                    } else {
//                                            output.setText("No contig with id: '" + eLabel + "'", "regular");
//                                            vv.setVisible(false);
                                            returnVal = -1;
                                            return returnVal;
                                    }
                            } catch (IllegalArgumentException e1) {
                                e1.printStackTrace();
                                            JFrame owner = dive;
                                            if(dive == null){
                                                owner = new JFrame();
                                            }
                                    JOptionPane.showMessageDialog(owner, e1,
                                                    "ERROR", JOptionPane.ERROR_MESSAGE);
                            }
//                    }
            } catch (Exception ex) {
                ex.printStackTrace();
                                            JFrame owner = dive;
                                            if(dive == null){
                                                owner = new JFrame();
                                            }
                    JOptionPane.showMessageDialog(owner, ex,
                                    "ERROR", JOptionPane.ERROR_MESSAGE);
            }

//            stopLayout();
            
            return returnVal;
        }

        public void loadFileAndFindShortestPath(File fileToOpen, int k, String id1, String id2) throws IOException, Exception{
            layoutPaused = false;
            currentAssemblyMode = getCurrentAssemblyMode();

//                boolean isPairedEndMode = peRadioButton.isSelected();
                    boolean newFile = fileOpened == null ||
                                        !fileOpened.equals(fileToOpen) ||
                                        lastAssemblyMode != currentAssemblyMode;
//                                        wasPairedEndMode.booleanValue() != isPairedEndMode;
//                    boolean reloadFile = (trueNeighborsOnly != null && trueNeighborsOnly.booleanValue() != showNeighborsRadioButton.isSelected()) ||
//                                                (lastStepSize != null && lastStepSize != stepSizes[stepSizeSlider.getValue()]);

                    if(newFile){
                        clear();
                    }

                    applySettings();

                    if(newFile) {
                        graphLoader.setK(k);
                        graphLoader.load(fileToOpen, currentAssemblyMode);

                        setUpLengthSlider();
                        fileOpened = fileToOpen;
//                        wasPairedEndMode = isPairedEndMode;
                        lastAssemblyMode = currentAssemblyMode;
                        trueNeighborsOnly = showNeighborsRadioButton.isSelected();
                        lastStepSize = stepSizes[stepSizeSlider.getValue()];
                    }

                    findShortestPath(id1, id2);

                    if(newFile){
                        dive.setQuery(id1+","+id2, null);
                    }
                    else{
                        dive.setQuery(id1+","+id2);
                    }

                    isClear = false;
                    activeDisplay = true;
        }

        public void findShortestPath(String id1, String id2) throws Exception{
            vv.clear();
            vv.setSeed(null);
            controller.clear();
            AbyssGraph2 g = vv.getGraph();
            String eLabel1 = processQueryId(id1);
            String eLabel2 = processQueryId(id2);


            PersistentLayoutImpl<Vertex,Edge> pLayout = getShortestPathLayout(g, eLabel1, eLabel2);
            vv.setGraphLayout(pLayout);
        }

	/**
	 * Builds the Output Panel component
	 */
	public JScrollPane addOutputPanel() {
		output = new OutputPanel();
		//output.getScrollPane().setMinimumSize(new Dimension(500,200));
                return output.getScrollPane();
		//rootFrame.getContentPane().add(output.getScrollPane(), BorderLayout.PAGE_END);
	}

        protected static class DisabledRenderer extends DefaultTableCellRenderer{
            public DisabledRenderer(){
                super();
                setForeground(Color.GRAY);
            }
        }

        public static class PartnerTableModel extends DefaultTableModel{
            protected Class[] types = new Class [] {
                String.class, Integer.class, Float.class, Integer.class
            };

            protected boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public PartnerTableModel(){
                super(new Object [][] {}, new String [] {"Contig", "d", "e", "n"});
            }

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        }

        private JPanel addPePartnerTables(){
            JPanel partnerListsPanel = new JPanel();
            partnerListsPanel.setLayout(new GridBagLayout());

            inPartnerTable = new JTable(new PartnerTableModel()){
                protected String[] toolTips = {"Contig id",
                                                     "d (bp)",
                                                     "e (bp)",
                                                     "n (number of mates)"};
                @Override
                public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
                    if(indexesOfInPartnersNotShown.contains(inPartnerTable.convertRowIndexToModel(row))){
                        if(column > 0){ // numbers
                            disabledRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
                        }
                        else{ // string
                            disabledRenderer.setHorizontalAlignment(SwingConstants.LEFT);
                        }
                        return disabledRenderer;
                    }

                    return super.getCellRenderer(row, column);
                }

                @Override
                protected JTableHeader createDefaultTableHeader() {
                    return new JTableHeader(columnModel) {
                        public String getToolTipText(MouseEvent e) {
                            java.awt.Point p = e.getPoint();
                            int index = columnModel.getColumnIndexAtX(p.x);
                            int modelIndex = columnModel.getColumn(index).getModelIndex();
                            return toolTips[modelIndex];
                        }
                    };
                }
            };
            inPartnerTable.setAutoCreateRowSorter(true);
            inPartnerTable.getTableHeader().setReorderingAllowed(false);
            inPartnerTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
            
            outPartnerTable = new JTable(new PartnerTableModel()){
                protected String[] toolTips = {"Contig id",
                                                     "d (bp)",
                                                     "e (bp)",
                                                     "n (number of mates)"};
                @Override
                public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
                    if(indexesOfOutPartnersNotShown.contains(outPartnerTable.convertRowIndexToModel(row))){
                        if(column > 0){ // numbers
                            disabledRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
                        }
                        else{ // string
                            disabledRenderer.setHorizontalAlignment(SwingConstants.LEFT);
                        }
                        return disabledRenderer;
                    }

                    return super.getCellRenderer(row, column);
                }

                @Override
                protected JTableHeader createDefaultTableHeader() {
                    return new JTableHeader(columnModel) {
                        public String getToolTipText(MouseEvent e) {
                            java.awt.Point p = e.getPoint();
                            int index = columnModel.getColumnIndexAtX(p.x);
                            int modelIndex = columnModel.getColumn(index).getModelIndex();
                            return toolTips[modelIndex];
                        }
                    };
                }
            };
            outPartnerTable.setAutoCreateRowSorter(true);
            outPartnerTable.getTableHeader().setReorderingAllowed(false);
            outPartnerTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

            inPartnerListLabel = new JLabel("inbound");
            inPartnerListLabel.setEnabled(false);
            outPartnerListLabel = new JLabel("outbound");
            outPartnerListLabel.setEnabled(false);
            partnerListTitle = new JLabel("Distance Estimates");
            partnerListTitle.setEnabled(false);

            JScrollPane inScrollPane = new JScrollPane(inPartnerTable);
            inScrollPane.setMinimumSize(new Dimension(10,10));
            inScrollPane.setPreferredSize(new Dimension(10,10));
            inScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
            JScrollPane outScrollPane = new JScrollPane(outPartnerTable);
            outScrollPane.setMinimumSize(new Dimension(10,10));
            outScrollPane.setPreferredSize(new Dimension(10,10));
            outScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));

            int y = 0;

            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridwidth = 2;
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.weightx = 0;
            gridBagConstraints.weighty = 0;
            partnerListsPanel.add(partnerListTitle, gridBagConstraints);

            y++;

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.weightx = 0;
            gridBagConstraints.weighty = 0;
            partnerListsPanel.add(inPartnerListLabel, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.weightx = 0;
            gridBagConstraints.weighty = 0;
            partnerListsPanel.add(outPartnerListLabel, gridBagConstraints);

            y++;

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 0;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1;
            gridBagConstraints.weighty = 1;
            partnerListsPanel.add(inScrollPane, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.gridx = 1;
            gridBagConstraints.gridy = y;
            gridBagConstraints.fill = GridBagConstraints.BOTH;
            gridBagConstraints.weightx = 1;
            gridBagConstraints.weighty = 1;
            partnerListsPanel.add(outScrollPane, gridBagConstraints);

            return partnerListsPanel;
        }

//        private JPanel addPePartnerLists() {
//            JPanel partnerListsPanel = new JPanel();
//
//            inPartnerList = new JList();
//            inPartnerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//            outPartnerList = new JList();
//            outPartnerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//            inPartnerListLabel = new JLabel("inbound");
//            inPartnerListLabel.setEnabled(false);
//            outPartnerListLabel = new JLabel("outbound");
//            outPartnerListLabel.setEnabled(false);
//            partnerListTitle = new JLabel("Paired-end Partners");
//            partnerListTitle.setEnabled(false);
//
//            JScrollPane inPartnerListPane = new JScrollPane(inPartnerList);
//            JScrollPane outPartnerListPane = new JScrollPane(outPartnerList);
//            partnerListsPanel.setLayout(new GridBagLayout());
//
//            int y = 0;
//
//            GridBagConstraints gridBagConstraints = new GridBagConstraints();
//            gridBagConstraints.gridwidth = 2;
//            gridBagConstraints.gridx = 0;
//            gridBagConstraints.gridy = y;
//            gridBagConstraints.fill = GridBagConstraints.NONE;
//            gridBagConstraints.weightx = 0;
//            gridBagConstraints.weighty = 0;
//            partnerListsPanel.add(partnerListTitle, gridBagConstraints);
//
//            y++;
//
//            gridBagConstraints = new GridBagConstraints();
//            gridBagConstraints.gridx = 0;
//            gridBagConstraints.gridy = y;
//            gridBagConstraints.fill = GridBagConstraints.NONE;
//            gridBagConstraints.weightx = 0;
//            gridBagConstraints.weighty = 0;
//            partnerListsPanel.add(inPartnerListLabel, gridBagConstraints);
//
//            gridBagConstraints = new GridBagConstraints();
//            gridBagConstraints.gridx = 1;
//            gridBagConstraints.gridy = y;
//            gridBagConstraints.fill = GridBagConstraints.NONE;
//            gridBagConstraints.weightx = 0;
//            gridBagConstraints.weighty = 0;
//            partnerListsPanel.add(outPartnerListLabel, gridBagConstraints);
//
//            y++;
//
//            gridBagConstraints = new GridBagConstraints();
//            gridBagConstraints.gridx = 0;
//            gridBagConstraints.gridy = y;
//            gridBagConstraints.fill = GridBagConstraints.BOTH;
//            gridBagConstraints.weightx = 1;
//            gridBagConstraints.weighty = 1;
//            partnerListsPanel.add(inPartnerListPane, gridBagConstraints);
//
//            gridBagConstraints = new GridBagConstraints();
//            gridBagConstraints.gridx = 1;
//            gridBagConstraints.gridy = y;
//            gridBagConstraints.fill = GridBagConstraints.BOTH;
//            gridBagConstraints.weightx = 1;
//            gridBagConstraints.weighty = 1;
//            partnerListsPanel.add(outPartnerListPane, gridBagConstraints);
//
//            return partnerListsPanel;
//        }

        private JSplitPane addBottomPanel(){
            bottomSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, false);
            bottomSplitPane.setResizeWeight(0.5);
            bottomSplitPane.setOneTouchExpandable(true);
            JPanel partnerListPanel = addPePartnerTables();
            JScrollPane outputScrollPane = addOutputPanel();
            bottomSplitPane.setLeftComponent(outputScrollPane);
            bottomSplitPane.setRightComponent(partnerListPanel);
            bottomSplitPane.setDividerLocation(0.5);
            bottomSplitPane.setContinuousLayout(true);
            return bottomSplitPane;
        }

        protected JButton pauseButton = null;
        protected JButton resumeButton = null;
//        protected JButton resetButton = null;
        protected JProgressBar progressBar = null;
        protected JCheckBox minimizeEnergyCheckBox = null;

        private JPanel addTopPanel(){
            int x = 0;

            JPanel p = new JPanel();
            p.setLayout(new GridBagLayout());

            pauseButton = new JButton();
            pauseButton.setEnabled(false);
            pauseButton.setToolTipText("pause layout of graph");
            pauseButton.setIcon(new javax.swing.ImageIcon(dive.getClass().getResource("/ca/bcgsc/dive/img/pause.png")));
            pauseButton.setMinimumSize(new Dimension(26,26));
            pauseButton.setMaximumSize(new Dimension(26,26));
            pauseButton.setPreferredSize(new Dimension(26,26));
            pauseButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    pauseLayout();
                    dive.setStatus("Graph layout paused by user at " + progressBar.getString() + ".", false, Dive.NORMAL_STATUS);
                    vv.repaint();
                    vv.setVisible(true);
                    vv.centerGraph();
                }
            });

            resumeButton = new JButton();
            resumeButton.setEnabled(false);
            resumeButton.setToolTipText("resume layout of graph");
            resumeButton.setIcon(new javax.swing.ImageIcon(dive.getClass().getResource("/ca/bcgsc/dive/img/forward.png")));
            resumeButton.setMinimumSize(new Dimension(26,26));
            resumeButton.setMaximumSize(new Dimension(26,26));
            resumeButton.setPreferredSize(new Dimension(26,26));
            resumeButton.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    vv.setVisible(false);
                    resumeLayout();
                    dive.setStatus("Graph layout resumed by user.", false, Dive.NORMAL_STATUS);
                }
            });

            progressBar = new JProgressBar(JProgressBar.HORIZONTAL,0,100);
            progressBar.setStringPainted(true);
            progressBar.setMinimumSize(new Dimension(100, 25));
            progressBar.setPreferredSize(new Dimension(100, 25));
            progressBar.setToolTipText("progress of layout algorithm");

            peLabel = new JLabel("Contigs:");
            peComboBox = new JComboBox();
            peComboBox.setMinimumSize(new Dimension(120, 25));
            peComboBox.setPreferredSize(new Dimension(120, 25));

            minimizeEnergyCheckBox = new JCheckBox("minimize energy");
            minimizeEnergyCheckBox.setSelected(true);

            GridBagConstraints gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weighty = 0;
            p.add(new JLabel("layout:"), gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weighty = 0;
            p.add(progressBar, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weighty = 0;
            p.add(pauseButton, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weighty = 0;
            p.add(resumeButton, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.WEST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weightx = 1;
            gridBagConstraints.weighty = 0;
            p.add(minimizeEnergyCheckBox, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weightx = 1;
            gridBagConstraints.weighty = 0;
            p.add(peLabel, gridBagConstraints);

            gridBagConstraints = new GridBagConstraints();
            gridBagConstraints.anchor = GridBagConstraints.EAST;
            gridBagConstraints.gridx = x;
            x++;
            gridBagConstraints.gridy = 0;
            gridBagConstraints.fill = GridBagConstraints.NONE;
            gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
            gridBagConstraints.weightx = 0;
            gridBagConstraints.weighty = 0;
            p.add(peComboBox, gridBagConstraints);

            return p;
        }

        public void addPlotMarkers(){
            if(vv != null){
                Edge edge = vv.getSelectedEdge();
                if(edge != null){
                    Integer lenInBP = edge.getLen();
                    Float cov = edge.getCoverage();
                    dive.addPlotMarkers(Integer.toString(edge.getId()), lenInBP, cov, k);
                }
            }
        }
	
	protected PersistentLayoutImpl<Vertex,Edge> getRandomQueryLayout(AbyssGraph2 g) {
		return getQueryLayout(g, g.getRandomEdge());
	}
	
	protected PersistentLayoutImpl<Vertex,Edge> getQueryLayout(AbyssGraph2 g, int i) {
		return getQueryLayout(g, g.getEdge(i));
	}
	
	protected PersistentLayoutImpl<Vertex,Edge> getQueryLayout(AbyssGraph2 g, Edge e) {
            System.out.println("seed contig: " + e.getLabel());
            seedId = e.getId();

            DirectedSparseMultigraph<Vertex,Edge> subGraph = getSubGraph(g,e);
            MyKKLayout<Vertex,Edge> layout = new MyKKLayout<Vertex,Edge>(subGraph);
            layout.setMinDeltaEnergyNeededToContinue(0.1D);
            layout.setMaxIterations(1000);
//            layout.setDiameter(0.5D);
//            layout.initialize();
//            layout.setLengthFactor(0.1);            
            PersistentLayoutImpl<Vertex,Edge> pLayout = new PersistentLayoutImpl<Vertex,Edge>(layout);
            return pLayout;
	}
	
	protected DirectedSparseMultigraph<Vertex,Edge> getSubGraph(AbyssGraph2 g, Edge e) {
            DirectedSparseMultigraph<Vertex,Edge> subGraph = null;
            
//            if(stepSizeRadioButton.isSelected()){
//                subGraph = g.getQuerySubgraphBySteps(e, stepSizeSlider.getValue());
//            }
//            else{
//                subGraph = g.getQuerySubgraph(e, 60);
//            }

            subGraph = g.getQuerySubgraphBySteps(e, stepSizes[stepSizeSlider.getValue()], showNeighborsRadioButton.isSelected());

            return subGraph;
	}

        protected PersistentLayoutImpl<Vertex,Edge> getShortestPathLayout(AbyssGraph2 g, String eLabel1, String eLabel2) throws Exception {
            Edge e1 = g.getEdge(eLabel1);

            if(e1 == null){
                throw new Exception("Could not find contig \"" + eLabel1 + "\".");
            }

            Edge e2 = g.getEdge(eLabel2);

            if(e2 == null){
                throw new Exception("Could not find contig \"" + eLabel2 + "\".");
            }

//            DirectedSparseMultigraph<Vertex,Edge> subGraph = g.getShortestPathOnlySubgraph(e1, e2);

            ShortestPathNeighborhood spn = g.getShortestPathWithNeighborhoodSubgraph(e1, e2, stepSizes[stepSizeSlider.getValue()], showNeighborsRadioButton.isSelected());
            vv.setPath(spn.path);
            pathTerminalEdges = new String[]{e1.getLabel(), e2.getLabel()};
            seedId = null;

            MyKKLayout<Vertex,Edge> layout = new MyKKLayout<Vertex,Edge>(spn.graph);
            layout.setMinDeltaEnergyNeededToContinue(0.1D);
            layout.setMaxIterations(1000);
            PersistentLayoutImpl<Vertex,Edge> pLayout = new PersistentLayoutImpl<Vertex,Edge>(layout);
            return pLayout;
        }

        protected void updateVertexInfo(Vertex v){
            output.clear();

            Graph<Vertex, Edge> displayedGraph = vv.getDisplayedGraph();

            List<Edge> outgoing = v.getOutgoing();
            List<Edge> incoming = v.getIncoming();

            // print all incoming edges
            String incomingEdges = "incoming edges: ";
            String incomingEdgesFormat = "bold";

            for(Edge e : incoming){
                incomingEdges += ", " + e.getLabel();
                if(displayedGraph.containsEdge(e)){
                    incomingEdgesFormat += ",regular";
                }
                else{
                    incomingEdgesFormat += ",missing";
                }
            }

            if(incoming.size() == 0){
                incomingEdges += ", none";
                incomingEdgesFormat += ",missing";
            }

            output.addText(incomingEdges, incomingEdgesFormat);

            // print all outgoing edges
            String outgoingEdges = "outgoing edges: ";
            String outgoingEdgesFormat = "bold";

            for(Edge e : outgoing){
                outgoingEdges += ", " + e.getLabel();
                if(displayedGraph.containsEdge(e)){
                    outgoingEdgesFormat += ",regular";
                }
                else{
                    outgoingEdgesFormat += ",missing";
                }
            }

            if(outgoing.isEmpty()){
                outgoingEdges += ", none";
                outgoingEdgesFormat += ",missing";
            }

            output.addText(outgoingEdges+"\n", outgoingEdgesFormat);

            // print all inferred overlaps
            if(v.getNumInferredOverlaps() > 0){
                String[] inferredOverlaps = v.getInferredOverlaps();
                String str = "inferred overlaps: ";
                String format = "bold";

                for(String overlap : inferredOverlaps){
                    str += ", " + overlap;
                    format += ",regular";
                }

                output.addText(str, format);
            }

            // aberrant overlaps
            if(v.getNumAberrantOverlaps() > 0){
                String[] aberrantOverlaps = v.getAberrantOverlaps();
                String str = "large overlaps: ";
                String format = "bold";

                for(String overlap : aberrantOverlaps){
                    str += ", " + overlap;
                    format += ",regular";
                }

                output.addText(str, format);
            }
        }

	/**
	 * Use edge selection state from the VisualizationViewer to update 
	 * GUI component displays.
	 */
	protected void updateEdgeInfo() {          
		output.clear();
		String selectedEdge = vv.getSelectedEdgeString(k);
		if (selectedEdge == null) {
                    dive.removePlotMarkers();
                    if(vv.getSelectedPathLabel() == null){
                        printSeedContigs();
                    }
                    return;
                }

                if(fileOpened.isFile()){
                    selectedEdge = "id: ," + selectedEdge;
                }
                else{
                    if(scaffoldRadioButton.isSelected()){
                        selectedEdge = "Scaffold id: ," + selectedEdge;
                    }
                    else if(peRadioButton.isSelected()){
                        selectedEdge = "Contig id: ," + selectedEdge;
                    }
                    else if(seRadioButton.isSelected()){
                        selectedEdge = "Unitig id: ," + selectedEdge;
                    }
                    else {
                        selectedEdge = "id: ," + selectedEdge;
                    }
                }

		output.setText(selectedEdge, vv.getSelectedEdgeFormats());
		output.getScrollPane().repaint();

                //addPlotMarkers();
	}
	
	protected void updatePartnerInfo() {
		// only modify the partner list if toggle is on
//		if (controller.partnerListOn()) {
			List<Object[]> iLabels = vv.getInboundPartnerData();
			if (iLabels != null) {
                                indexesOfInPartnersNotShown.clear();
				controller.setInboundTable(iLabels);
                                AbyssGraph2 abyssGraph = vv.getGraph();
                                DirectedSparseMultigraph<Vertex,Edge> graph = (DirectedSparseMultigraph<Vertex,Edge>) vv.getDisplayedGraph();
                                DefaultTableModel model = (DefaultTableModel) inPartnerTable.getModel();
                                for(Object[] rowData : iLabels){
                                    Edge e = abyssGraph.getEdge((String)rowData[0]);
                                    if(!graph.containsEdge(e)){
                                        for(int i=0; i<model.getRowCount(); i++){
                                            String id = (String)model.getValueAt(i, 0);
                                            if(id.startsWith(Integer.toString(e.getId()))){
                                                indexesOfInPartnersNotShown.add(i);
                                            }
                                        }
                                    }
                                }
                                inPartnerTable.repaint();
			} else {
                                indexesOfInPartnersNotShown.clear();
				controller.clearInboundList();
			}
			List<Object[]> oLabels = vv.getOutboundPartnerData();
			if (oLabels != null) {
                                indexesOfOutPartnersNotShown.clear();
				controller.setOutboundTable(oLabels);
                                AbyssGraph2 abyssGraph = vv.getGraph();
                                DirectedSparseMultigraph<Vertex,Edge> graph = (DirectedSparseMultigraph<Vertex,Edge>) vv.getDisplayedGraph();
                                DefaultTableModel model = (DefaultTableModel) outPartnerTable.getModel();
                                for(Object[] rowData : oLabels){
                                    Edge e = abyssGraph.getEdge((String)rowData[0]);
                                    if(e != null && !graph.containsEdge(e)){
                                        for(int i=0; i<model.getRowCount(); i++){
                                            String id = (String)model.getValueAt(i, 0);
                                            if(id.startsWith(Integer.toString(e.getId()))){
                                                indexesOfOutPartnersNotShown.add(i);
                                            }
                                        }
                                    }
                                }
                                outPartnerTable.repaint();
			} else {
                                indexesOfOutPartnersNotShown.clear();
				controller.clearOutboundList();
			}

                        //panel.getControlPanel().repaint();
//		}
		vv.repaint();
	}
	
	protected void updatePathInfo() {
                output.clear();
		updateEdgeInfo();
                if(graphLoader.isPairedEndAssembly()){
                    Edge e = vv.getSelectedEdge();
                    if(e != null){
                        ContigLabel cl = e.getLabelObject();
                        Object[] members = vv.getGraph().getSEMembers(cl);

                        if(members != null){
                            String memberString = "Unitig members: ,";
                            
                            if(currentAssemblyMode == Dive.SCAFFOLDS){
                                 memberString = "Contig members: ,";
                            }
                            
                            String formatString = "bold,";

                            if(cl.getStrand() == 0){
                                for (int i=0; i<members.length; i++){
                                    if(members[i] instanceof ContigLabel){
                                        memberString += ((ContigLabel)members[i]).getLabel() + " ,";
                                    }
                                    else{
                                        memberString += members[i] + " ,";
                                    }
                                    formatString += "regular,";
                                }
                            }
                            else {
                                for (int i=members.length-1; i>=0; i--){
                                    if(members[i] instanceof ContigLabel){
                                        memberString += ((ContigLabel)members[i]).getLabel() + " ,";
                                    }
                                    else{
                                        memberString += members[i] + " ,";
                                    }
                                    formatString += "regular,";
                                }
                            }
                            output.addText(memberString,formatString);
                        }
                    }
                }
                else if (currentAssemblyMode == Dive.UNITIGS) {
			String selectedPath = vv.getSelectedPathLabel();
			if (selectedPath == null) { 
				controller.clearPeList();
				vv.repaint();
				return; 
			}
			controller.setPeList(vv.getPossiblePathLabels(), selectedPath);
//			controller.setSelectedPeId(selectedPath);                        
			output.addText(vv.getSelectedPathString(), vv.getSelectedPathStringFormats());
		}
		vv.repaint();
		output.getScrollPane().repaint();
	}

        protected void printSeedContigs(){
            if(vv == null){
                return;
            }

            ArrayList<Edge> path = vv.getPath();
            String memberString = "";
            String formatString = "";
            
            if(path != null && path.size() > 0){
                memberString = "Seed contigs: ,";
                formatString = "bold,";
                for (Edge e : path){
                    memberString += e.getLabel() + " ,";
                    formatString += "regular,";
                }                
            }
            else if(seedId == null || vv.getGraph() == null){
                return;
            }
            else{
                memberString = "Seed contig: ,";
                formatString = "bold,";
                memberString += vv.getGraph().getEdge(seedId).getLabel();
                formatString += "regular,";
            }
            output.addText(memberString,formatString);
        }

	/**
	 * Called once the graph parse is complete. Generates
	 * a layout for a randomly seeded subgraph, and sets
	 * a graph layout and mouse listeners
	 * @param g
	 */
	protected void initializeGraph(AbyssGraph2 g) {

            PersistentLayoutImpl<Vertex,Edge> pLayout = null;

            if(this.pathTerminalEdges != null){
                try{
                    pLayout = getShortestPathLayout(g, pathTerminalEdges[0], pathTerminalEdges[1]);
                }
                catch(Exception e){
                    pLayout = null;
                }
            }

            if(pLayout == null){
                Integer id = null;
                if(seedId == null){
                    id = graphLoader.getIdOfLongestContig();
                }
                else{
                    id = seedId;
                }

                Edge e = g.getEdge(id);
                if(e != null){
                    pLayout = getQueryLayout(g, e);
                }
                else{
                    pLayout = getRandomQueryLayout(g);
                }
            }
            
		if (vv == null) {
			vv = new AbyssVisualizationViewer(pLayout);
                        applySettings();
                        vv.setVisible(true); // VV must be visible when it was added to the rootPanel. This is required for the first graph to be centered.
                        rootPanel.add(vv);
                        rootPanel.validate();
                        vv.centerGraph();
//                        vv.setVisible(false);
			vv.setGraph(g);
                        vv.setSeed(seedId);
//                        vv.repaint();
                        vv.addMouseListener(listener);
			vv.addGraphMouseChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					if (!vv.isEdgeSelected()) {
						controller.clear();
						output.clear();
                                                printSeedContigs();
					}
				}
			});
			// edge selection can be changed via mouse or search box, so listen directly to state class
			vv.addEdgeItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
                                    if(graphLoader.isPairedEndAssembly()){
                                        updatePathInfo();
                                    }
                                    else{
					updateEdgeInfo();
                                    }
                                    addPlotMarkers();
                                    controller.clear();
				}
			});
			// partners can be changed via mouse or search box, so listen directly to state class
			vv.addPartnerChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updatePartnerInfo();
				}
			});
			// path can be changed via mouse or search box, so listen directly to state class
			vv.addPathChangeListener(new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					updatePathInfo();
				}
			});
                        
			vv.addModelChangeListener(new ChangeListener() {
                            private boolean inprogress = false;

				public void stateChanged(ChangeEvent e) {
                                    if(layoutPaused){
                                        pauseLayout();
                                        return;
                                    }

                                    if(vv != null){
                                        Layout<Vertex, Edge> layout = vv.getGraphLayout();
                                        if(layout != null){
                                            PersistentLayoutImpl<Vertex,Edge> l = (PersistentLayoutImpl<Vertex,Edge>) layout;
                                            MyKKLayout<Vertex,Edge> trueLayout = (MyKKLayout<Vertex,Edge>) l.getDelegate();
//                                            System.out.println(trueLayout.getStatus());                                     
                                            
                                            if(minimizeEnergyCheckBox.isSelected()){
                                                if( trueLayout.energyMinimized() ){
                                                    pauseLayout();                                                    
                                                    progressBar.setValue(progressBar.getMaximum());
                                                    if(inprogress){
                                                        dive.setStatus("Graph layout completed.", false, Dive.NORMAL_STATUS);
                                                    }
                                                    if(!activeDisplay){
                                                        activeDisplay = true;
                                                    }
                                                    inprogress = false;
                                                    vv.repaint();
                                                    vv.setVisible(true);
                                                    vv.centerGraph();
                                                }
                                                else if(!layoutPaused){
                                                    vv.setVisible(false);
                                                    pauseButton.setEnabled(true);
                                                    resumeButton.setEnabled(false);

                                                    if(trueLayout.done()){
                                                        trueLayout.reset();
                                                    }
                                                    else{
                                                        inprogress = true;
                                                        int progress = trueLayout.getEnergyMinimizationProgress();
                                                        if(progress < 0){
                                                            progress = 0;
                                                        }
                                                        progressBar.setValue(progress);
                                                        dive.setStatus("Graph layout in progress..." + progress + "%", false, Dive.NORMAL_STATUS);
                                                    }
                                                }
                                            }
                                            else{
                                                if( trueLayout.done() ) {
                                                    pauseLayout();
                                                    progressBar.setValue(progressBar.getMaximum());
                                                    if(inprogress){
                                                        dive.setStatus("Graph layout completed.", false, Dive.NORMAL_STATUS);
                                                    }
                                                    if(!activeDisplay){
                                                        activeDisplay = true;
                                                    }                                                    
                                                    vv.repaint();
                                                    vv.setVisible(true); // only display vv if layout calculation is complete
                                                    vv.centerGraph();
                                                }
                                                else if(!layoutPaused){
                                                    vv.setVisible(false);
                                                    pauseButton.setEnabled(true);
                                                    resumeButton.setEnabled(false);

                                                    inprogress = true;
                                                    int progress = trueLayout.getIterationProgress();
                                                    progressBar.setValue(progress);
                                                    dive.setStatus("Graph layout in progress..." + progress + "%", false, Dive.NORMAL_STATUS);
                                                }
                                            }
                                        }
                                    }
                                }
			});
                        vv.addMouseListener(new MouseListener(){

                            public void mouseClicked(MouseEvent me) {
                                if(me.getClickCount() == 1){
                                    Vertex v = vv.getPickSupport().getVertex(vv.getGraphLayout(), me.getX(), me.getY());
                                    if(v != null){
                                        updateVertexInfo(v);
                                    }
                                }
                                else if(!me.isPopupTrigger() && me.getClickCount() == 2){
                                    Vertex v = vv.getPickSupport().getVertex(vv.getGraphLayout(), me.getX(), me.getY());
                                    // if an edge is not shown
                                    if(v != null && vv.missingEdges(v)){
                                        //get the largest edge that is not shown
                                        Edge largestMissingEdge = vv.getLargestMissingEdge(v);
                                        seedId = largestMissingEdge.getId();
                                        pathTerminalEdges = null;
                                        vv.setSeed(seedId);

                                        dive.setQuery(largestMissingEdge.getLabel());
                                        //refocus on this edge
                                        PersistentLayoutImpl<Vertex,Edge> pLayout = getQueryLayout(vv.getGraph(),largestMissingEdge);
                                        pLayout.setSize(vv.getSize());
                                        vv.setGraphLayout(pLayout);
                                        vv.selectEdge(largestMissingEdge);
                                    }
                                }
                            }
                            public void mousePressed(MouseEvent me) {
                                if(me.getClickCount() == 1){
                                    Vertex v = vv.getPickSupport().getVertex(vv.getGraphLayout(), me.getX(), me.getY());
                                    if(v != null){
                                        updateVertexInfo(v);
                                    }
                                }

                                if(me.isPopupTrigger()){
                                    Edge e = vv.getPickSupport().getEdge(vv.getGraphLayout(), me.getX(), me.getY());
                                    if(e == null){
                                        refocusMenuItem.setEnabled(false);
                                        showSeqMenuItem.setEnabled(false);
                                    }
                                    else{
                                        vv.selectEdge(e);
                                        refocusMenuItem.setEnabled(true);
                                        showSeqMenuItem.setEnabled(true);
                                    }
                                    viewerMenu.show(vv, me.getX(), me.getY());
                                }
                            }
                            public void mouseReleased(MouseEvent me) {
                                if(me.isPopupTrigger()){
                                    Edge e = vv.getPickSupport().getEdge(vv.getGraphLayout(), me.getX(), me.getY());
                                    if(e == null){
                                        refocusMenuItem.setEnabled(false);
                                        showSeqMenuItem.setEnabled(false);
                                    }
                                    else{
                                        vv.selectEdge(e);
                                        refocusMenuItem.setEnabled(true);
                                        showSeqMenuItem.setEnabled(true);
                                    }
                                    viewerMenu.show(vv, me.getX(), me.getY());
                                }
                            }
                            public void mouseEntered(MouseEvent me) {
                            }
                            public void mouseExited(MouseEvent me) {
                            }

                        });
		} else {
                        vv.setLenScale((int)Math.rint(Math.pow(10, controller.getLenSliderValue()/100.0*(maxExpForSlider-minExpForSlider)+minExpForSlider) ));
			Dimension d = vv.getSize();
			pLayout.setSize(d);
                        vv.setVisible(false);
			vv.setGraphLayout(pLayout);
//                        vv.setVisible(true);
                        vv.setGraph(g);
//                        MutableTransformer modelTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
//                        modelTransformer.setToIdentity();
		}

//            stopLayout();
	}

        protected JMenuItem refocusMenuItem;
        protected JMenuItem showSeqMenuItem;
        protected JMenuItem screenCaptureMenuItem;
        protected JMenuItem centerGraphMenuItem;
//        protected JMenuItem showCorrespondingPEContigMenuItem;
//        protected JMenuItem showCorrespondingSEContigMenuItem;

        protected JPopupMenu createViewerPopupMenu(){
            JPopupMenu menu = new JPopupMenu();
            refocusMenuItem = new JMenuItem("Refocus on this contig");
            refocusMenuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    if(vv != null){
                        Edge edge = vv.getSelectedEdge();
                        dive.setQuery(edge.getLabel());
                        seedId = edge.getId();
                        pathTerminalEdges = null;
                        vv.setSeed(seedId);
                        PersistentLayoutImpl<Vertex,Edge> pLayout = getQueryLayout(vv.getGraph(),edge);
                        pLayout.setSize(vv.getSize());
                        vv.setGraphLayout(pLayout);
                    }
                }
            });
            menu.add(refocusMenuItem);

//            showCorrespondingSEContigMenuItem = new JMenuItem("Switch to single-end contig");
//            showCorrespondingSEContigMenuItem.addActionListener(new ActionListener(){
//                public void actionPerformed(ActionEvent ae) {
//                    seRadioButton.setSelected(true);
//                    loadFile(fileOpened, k);
//                }
//            });
//            menu.add(showCorrespondingSEContigMenuItem);
//
//            showCorrespondingPEContigMenuItem = new JMenuItem("Switch to paired-end contig");
//            showCorrespondingPEContigMenuItem.addActionListener(new ActionListener(){
//                public void actionPerformed(ActionEvent ae) {
//                    peRadioButton.setSelected(true);
//                    loadFile(fileOpened, k);
//                }
//            });
//            menu.add(showCorrespondingPEContigMenuItem);


            showSeqMenuItem = new JMenuItem("Show nucleotide sequence");
            showSeqMenuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    if(vv != null){
                        Edge edge = vv.getSelectedEdge();
                        //dive.showSequences(fileOpened.getAbsolutePath(), new String[]{edge.getLabel()});
                        String directoryPath = fileOpened.getAbsolutePath();
                        File directory = new File(directoryPath);
                        if (!directory.isDirectory()){
                            directory = directory.getParentFile();
                        }
                        //String[] faFiles = directory.list(new NameSuffixFilenameFilter(".fa"));
                        String path = directory.getAbsolutePath();
                        switch (currentAssemblyMode){
                            case Dive.SCAFFOLDS:
                                break;
                            case Dive.CONTIGS:
                                break;
                            case Dive.UNITIGS:
                                break;                            
                        }
                        dive.showSequences(path, new String[]{edge.getLabel()});
                    }
                }
            });
            menu.add(showSeqMenuItem);

            screenCaptureMenuItem = new JMenuItem("Screen capture");
            screenCaptureMenuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    if(vv != null){
                        confirmTakingSnapShot();
                    }
                    else{
                        JFrame owner = dive;
                        if(dive == null){
                            owner = new JFrame();
                        }
                        JOptionPane.showMessageDialog(owner, "Visualization Viewer is not ready.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            });
            menu.add(screenCaptureMenuItem);

            centerGraphMenuItem = new JMenuItem("Center graph");
            centerGraphMenuItem.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent ae) {
                    vv.centerGraph();
                }
            });
            menu.add(centerGraphMenuItem);

            return menu;
        }


        protected void confirmTakingSnapShot(){
                                            JFrame owner = dive;
                                            if(dive == null){
                                                owner = new JFrame();
                                            }

            int re = fc.showSaveDialog(owner);
            if(re == JFileChooser.APPROVE_OPTION){
                FileFilter filter = fc.getFileFilter();
                String desc = filter.getDescription().toLowerCase();
                File f = fc.getSelectedFile();
                if(desc.contains("png")){                    
                    if(!f.getName().toLowerCase().endsWith(".png")){
                        f = new File(f.getParentFile().getPath() + File.separator + f.getName() + ".png");
                    }
                    if(f.exists()){
                        int confirm = JOptionPane.showConfirmDialog(owner, "'" + f.getName() + "' already exists. Overwrite the file?", "Confirm Overwriting File", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if(confirm == JOptionPane.NO_OPTION){
                            return;
                        }
                    }

                    try {
                        boolean ok = ImageIO.write(getRasterSnapShotOfVV(), "png", f);
                        if(!ok){
                            JOptionPane.showMessageDialog(owner, "No appropriate image writer is found.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(owner, "Cannot write image to file.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else if(desc.contains("svg")){
                    String path = f.getPath();
                    if(!f.getName().toLowerCase().endsWith(".svg")){
                        path += ".svg";
                        f = new File(path);
                    }

                    if(f.exists()){
                        int confirm = JOptionPane.showConfirmDialog(owner, "'" + f.getName() + "' already exists. Overwrite the file?", "Confirm Overwriting File", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                        if(confirm == JOptionPane.NO_OPTION){
                            return;
                        }
                    }

                    DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
                    Document document = domImpl.createDocument(path, "svg", null);
                    SVGGraphics2D svgGenerator = new SVGGraphics2D(document);
                    vv.paint(svgGenerator);
                    
                    
                    try {
                        boolean useCSS = true;
                        OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(f), "UTF-8");
                        svgGenerator.stream(out, useCSS);
                    } catch (SVGGraphics2DIOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(owner, "Cannot write image to file.", "SVGGraphics2DIOException", JOptionPane.ERROR_MESSAGE);
                    } catch (UnsupportedEncodingException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(owner, "Cannot write image to file.", "UnsupportedEncodingException", JOptionPane.ERROR_MESSAGE);
                    } catch (FileNotFoundException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(owner, "Cannot write image to file.", "FileNotFoundException", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        }

        protected BufferedImage getRasterSnapShotOfVV(){
            if(vv != null){
                Dimension d = vv.getSize();
                BufferedImage img = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = img.createGraphics();
                vv.paint(g2);
                g2.dispose();
                return img;
            }

            return null;
        }

	/**
	 * turn off the layout thread
	 */
	protected void stopLayout() {
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(true);
            
            Relaxer relaxer = vv.getModel().getRelaxer();
            if(relaxer != null){
                ((VisRunner)relaxer).stop();
            }
	}

	protected void runLayout() {
            Relaxer relaxer = vv.getModel().getRelaxer();
            if(relaxer != null){
                ((VisRunner)relaxer).run();
            }
	}
	
        protected void pauseLayout(){
            layoutPaused = true;
            pauseButton.setEnabled(false);
            resumeButton.setEnabled(true);

            Relaxer relaxer = vv.getModel().getRelaxer();
            if(relaxer != null){
                ((VisRunner)relaxer).pause();
            }
        }

        protected void resumeLayout(){
            layoutPaused = false;
            pauseButton.setEnabled(true);
            resumeButton.setEnabled(false);

            Relaxer relaxer = vv.getModel().getRelaxer();
            if(relaxer != null){
                ((VisRunner)relaxer).resume();
            }
        }

        protected void resetIteration(){
            Layout<Vertex, Edge> layout = vv.getGraphLayout();
            if(layout != null){
                PersistentLayoutImpl<Vertex,Edge> l = (PersistentLayoutImpl<Vertex,Edge>) layout;
                MyKKLayout<Vertex,Edge> trueLayout = (MyKKLayout<Vertex,Edge>) l.getDelegate();
                trueLayout.reset();
            }
        }

	protected void checkJavaVersion() {
		String version = System.getProperty("java.version");
		if (version.compareTo("1.5") < 0) {
			String m = "It is recommended that you upgrade to Java 1.5 ";
			m += "(you are running Java " + version + ")";

                                            JFrame owner = dive;
                                            if(dive == null){
                                                owner = new JFrame();
                                            }
			JOptionPane.showMessageDialog(owner, m, "Warning",
					JOptionPane.WARNING_MESSAGE);
		}
	}
	
//	public static void main(String[] args) throws IOException {
//
//	    try {
//		    // Set cross-platform Java L&F (also called "Metal")
//	        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//	    } catch (UnsupportedLookAndFeelException e) {
//	       // handle exception
//	    } catch (ClassNotFoundException e) {
//	       // handle exception
//	    } catch (InstantiationException e) {
//	       // handle exception
//	    } catch (IllegalAccessException e) {
//	       // handle exception
//	    }
//
//		AbyssExplorer e = new AbyssExplorer();
//		e.checkJavaVersion();
//		e.launch();
//
//	}
	
}
