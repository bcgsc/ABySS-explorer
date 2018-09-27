package ca.bcgsc.dive.dive;

import ca.bcgsc.abyssexplorer.gui.AbyssExplorer;
import ca.bcgsc.dive.chart.*;
import ca.bcgsc.dive.dive.Processor.SearchResults;
import ca.bcgsc.dive.stat.*;
import ca.bcgsc.dive.util.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.plaf.metal.MetalTabbedPaneUI;
import javax.swing.table.*;

/**
 *
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */
public class Dive extends javax.swing.JFrame {

    private static final long serialVersionUID = 201809271100L;
    private static final String version = "ABySS-Explorer 2.1.0";

    private String sharedAncestorPath = null; // the path shared by the assemblies loaded
    private HashMap<String, Integer> kValues = null; //key: absolute path of the assembly; value: k
    private HashMap<String, String> loadedAssembliesMap = new HashMap<>(); // key: canonical path of the assembly; value: abs. path

    private String bestInSelection = null; // the name of the assembly in the selection with the largest contiguity

    private static final DefaultComboBoxModel UNITS_FOR_FA = new DefaultComboBoxModel(new String[] { "bp", "k-mer"}); // only *.fa files have the nucleotide sequences to calculate Scaffold Lengths
//    private static final DefaultComboBoxModel UNITS_FOR_ADJ = new DefaultComboBoxModel(new String[] { "bp", "nol-bp" }); // only *.adj files have the data needed to calculate length in non-overlapping base-pairs

    private static final Color BEST_COLOR = new Color(231, 41, 138); // a pinkish red color for highlighting the largest value in the stats tables
    private static final DefaultTableCellRenderer maxRenderer = new Utilities.CellRenderer(BEST_COLOR, null, SwingConstants.RIGHT, Font.BOLD); // renderer for cells that have the largest value
    private static final DefaultTableCellRenderer disabledRenderer = new Utilities.CellRenderer(null, Color.LIGHT_GRAY, SwingConstants.LEFT, Font.PLAIN); // render for cells that have null entries (ie. when the file needed is not found)

    private ArrayList<Integer> disabledRowIndexesForSelectedFile = new ArrayList<>();
    private ArrayList<Integer> disabledRowIndexesForCoverageFile = new ArrayList<>();
    private boolean ready = false; // flag whether the current settings needs to be applied

    private int maxN50Index = -1; // row index of largest "N50" in scaffold sizes stats table
    private int maxContiguityIndex = -1; // row index of largest "contiguity" in scaffold sizes stats table
    private int maxReconstructionIndex = -1; // row index of largest "reconstruction" in scaffold sizes stats table
    private int maxSpanIndex = -1; // row index of the largest "span" in scaffold sizesstats table

    private static final int MAX_LENGTH_PER_LINE_IN_DIALOG = 50; // maximum number of characters per line. Commonly used for calls to Utilities.formatDialogMessage
    private static final String DEFAULT_TITLE = "ABySS-Explorer"; // default title for the program
    private static final String TITLE_FOR_ADDING_ASSEMBLIES = "Add one or more assemblies"; // title for the file choooser when the user atttempts to add assemblie directories
    private static final String TITLE_FOR_LOADING_FILE = "Load a DOT or ADJ file"; // title for the file chooser when the user attempts to load a DOT/ADJ file

    private AbyssExplorer explorer = null; 
    private N50plot n50plot = null; // the current N50plot
    private CoverageHistogram covplot = null; // the current K-mer Coverage Distribution plot
    private JSplitPane plotsSplitPane = null; // splitpane to hold N50plot (top/left) and Coverage Plot (bottom/right)
    private boolean plotsForNavigator = false; // flag to differentiate between the N50-plot generated using Scaffold Span (for Navigator) or Scaffold Length.
    private String lastAssemblySearched = null;

    private JPanel navigatorPanel = null; // panel for Navigator
    private PlotSettings lastPlotSettingsUsed = null;
    private BlatInterface blatInterface = null;
    private ArrayList<SwingWorker> workers = new ArrayList<>(); // list of currently active SwingWorkers

    private javax.swing.event.ListSelectionListener selectionListener = null; // selection listener for "Assemblies" list table
    private TableColumn hiddenColumn = null; // this would be the "Span" column if it were hidden

    private ArrayList<String> fsdFileNames = new ArrayList<>(); // list of names of files for Fragment Size statistics (ie. *-3.hist)
    private ArrayList<JCheckBox> libraryCheckBoxes = new ArrayList<>(); // list of checkboxes for selecting libraries in Settings

    private MouseListener statsComponentsListener = null; // mouse listener for clicks in all components within the Statistics pane
    private MouseListener plotsComponentsListener = null; // mouse listener for clicks in all components within the Plots pane
    private MouseListener navigatorComponentsListener = null; // mouse listener for clisk in all components within the Navigator pane

    private boolean showTabsHeader = false; // flag whetther the tabs are show in the Plots pane
    private String lastQuery = null; // last query for Navigator search
    private String currentQuery = null; // current query for Navigator search
    private final static int[] STEP_SIZES = new int[]{1,2,3,4,5,10,20,50,100}; // sizes for the step size slider in Navigator settings

    private static final String SEQUENCE_SEARCH_QUERY_FORMAT = "assembly/filename:contig-ID1,ID2,etc.";
    private static final String NAVIGATOR_SEARCH_QUERY_FORMAT = "ID1 or ID1,ID2";
    private static final String FIND_CONTIG_SEQ_TOOLTIP = "Find contig sequences";
    private static final String FIND_CONTIG_NAV_TOOLTIP = "Find contig(s) in Navigator";
    private final ImageIcon seqSearchIcon = new ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/magnifier_seq_arrow.png")); // icon for the search button when in sequence search mode
    private final ImageIcon navSearchIcon = new ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/magnifier_nav_arrow.png")); // icon for the search button when in navigtor search mode

    private static final Border RAISED_BORDER = BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED); // border for the view buttons when it is not pressed
    private static final Border LOWERED_BORDER = BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.LOWERED); // border for the view buttons when it is pressed

    private static final Border GRAY_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY); // border for the view panes that does not have focus
    private static final Border BLACK_BORDER = BorderFactory.createLineBorder(Color.BLACK); // border for the view pane that current has focus

    private final JLabel statsLabel = new JLabel("STATISTICS", new ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/stats_mini.png")), SwingConstants.LEFT); // label for the Statistics pane
    private final JLabel plotsLabel = new JLabel("PLOTS", new ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/plots_mini.png")), SwingConstants.LEFT); // label for the Plots pane
    private final JLabel navLabel = new JLabel("NAVIGATOR", new ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/explorer_mini.png")), SwingConstants.LEFT); // label for the Navigator pane

    private final CompTitledBorder statsBorder = new CompTitledBorder(GRAY_BORDER, statsLabel); // border for the Statistics pane
    private final CompTitledBorder plotsBorder = new CompTitledBorder(GRAY_BORDER, plotsLabel); // border for the Plots pane
    private final CompTitledBorder navBorder = new CompTitledBorder(GRAY_BORDER, navLabel); // border for the Navigator pane

    private final javax.swing.filechooser.FileNameExtensionFilter adjDotFileNameExtensionFilter = new javax.swing.filechooser.FileNameExtensionFilter("ADJ or DOT files", "adj", "dot");
    
    public static final int UNITIGS = 3;
    public static final int CONTIGS = 6;
    public static final int SCAFFOLDS = 8; 
    
    public Dive(){
        super();
        init();
    }

    /** Initializes the program */
    //@Override
    public void init() {
        try {
            java.awt.EventQueue.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    setUpInternalComponentsListeners();
                    initComponents();
                    initCustomComponents();
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** This method is called from within the init() method to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        lengthTypeButtonGroup = new javax.swing.ButtonGroup();
        contigSequenceFrame = new javax.swing.JFrame();
        jScrollPane3 = new javax.swing.JScrollPane();
        contigSequenceTextArea = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        copyToClipboardButton = new javax.swing.JButton();
        blatButton = new javax.swing.JButton();
        noteLabel = new javax.swing.JLabel();
        contigIdsComboBox = new javax.swing.JComboBox();
        exploreNeighborhoodButton = new javax.swing.JButton();
        genomeComboBox = new javax.swing.JComboBox();
        assemblyComboBox = new javax.swing.JComboBox();
        genomeLabel = new javax.swing.JLabel();
        assemblyLabel = new javax.swing.JLabel();
        sequencesTooLongWarningLabel = new javax.swing.JLabel();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        lineWrapCheckBoxMenuItem = new javax.swing.JCheckBoxMenuItem();
        searchTypePopupMenu = new javax.swing.JPopupMenu();
        sequenceSearchRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        navigatorSearchRadioButtonMenuItem = new javax.swing.JRadioButtonMenuItem();
        searchTypeButtonGroup = new javax.swing.ButtonGroup();
        fc = new javax.swing.JFileChooser();
        assemblyModeButtonGroup = new javax.swing.ButtonGroup();
        stepSizeButtonGroup = new javax.swing.ButtonGroup();
        scaffoldStatsPopupMenu = new javax.swing.JPopupMenu();
        copyScaffoldStatsMenuItem = new javax.swing.JMenuItem();
        fragmentStatsPopupMenu = new javax.swing.JPopupMenu();
        copyFragmentStatsMenuItem = new javax.swing.JMenuItem();
        libsPopupMenu = new javax.swing.JPopupMenu();
        assembliesPopupMenu = new javax.swing.JPopupMenu();
        removeSelectedMenuItem = new javax.swing.JMenuItem();
        topPanel = new javax.swing.JPanel();
        addressbarPanel = new javax.swing.JPanel();
        addAssembliesButton = new javax.swing.JButton();
        removeAssembliesButton = new javax.swing.JButton();
        loadAdjDotButton = new javax.swing.JButton();
        jSeparator5 = new javax.swing.JSeparator();
        contigSearchPanel = new javax.swing.JPanel();
        searchButton = new javax.swing.JButton();
        contigSearchTextField = new javax.swing.JTextField();
        backButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        assembliesAndSettingsSplitPane = new javax.swing.JSplitPane();
        kDirPanel = new javax.swing.JPanel();
        kDirScrollPane = new javax.swing.JScrollPane();
        assembliesAndColorsTable = new javax.swing.JTable(){
            public String getToolTipText(MouseEvent e){
                String tip = null;
                Point p = e.getPoint();
                int c = columnAtPoint(p);
                int r = rowAtPoint(p);
                if(c == 1 && r >= 0){
                    TableModel model = getModel();
                    if(model != null){
                        tip = (String)model.getValueAt(r,c);
                        if(sharedAncestorPath != null){
                            tip = sharedAncestorPath + File.separator + tip;
                        }

                        int k = kValues.get(tip);

                        tip = "k=" + k + "; path: " + tip;
                    }
                }
                else{
                    tip = super.getToolTipText(e);
                }
                return tip;
            }
        };
        showStatsToggleButton = new javax.swing.JToggleButton();
        showPlotsToggleButton = new javax.swing.JToggleButton();
        showNavigatorToggleButton = new javax.swing.JToggleButton();
        settingsPanel = new javax.swing.JPanel();
        settingsTabbedPane = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        mclLabel = new javax.swing.JLabel();
        javax.swing.text.InternationalFormatter formatter = new javax.swing.text.InternationalFormatter();
        formatter.setMinimum(new Long(0));
        //formatter.setAllowsInvalid(false);
        minContigLengthFormattedTextField = new JFormattedTextField(formatter);
        unitLabel = new javax.swing.JLabel();
        yAxisUnitComboBox = new javax.swing.JComboBox();
        scaleLabel = new javax.swing.JLabel();
        xAxisScaleComboBox = new javax.swing.JComboBox();
        drawN50plotCheckBox = new javax.swing.JCheckBox();
        drawFragSizeDistCheckBox = new javax.swing.JCheckBox();
        selectLibrariesButton = new javax.swing.JButton();
        drawCovPlotCheckBox = new javax.swing.JCheckBox();
        explorerSettingsPanel = new javax.swing.JPanel();
        showLengthCheckBox = new javax.swing.JCheckBox();
        showLabelsCheckBox = new javax.swing.JCheckBox();
        lengthSlider = new javax.swing.JSlider();
        peRadioButton = new javax.swing.JRadioButton();
        seRadioButton = new javax.swing.JRadioButton();
        stepSizeSlider = new javax.swing.JSlider();
        showAllRadioButton = new javax.swing.JRadioButton();
        showNeighborsRadioButton = new javax.swing.JRadioButton();
        stepSizeLabel = new javax.swing.JLabel();
        scaffoldRadioButton = new javax.swing.JRadioButton();
        jPanel2 = new javax.swing.JPanel();
        fileLabel = new javax.swing.JLabel();
        filesComboBox = new javax.swing.JComboBox();
        lengthLabel = new javax.swing.JLabel();
        unitOfLengthComboBox = new javax.swing.JComboBox();
        useFastaIndexCheckBox = new javax.swing.JCheckBox();
        applyButton = new javax.swing.JButton();
        displayPanel = new javax.swing.JPanel();
        statsSplitPane = new javax.swing.JSplitPane();
        assembliesPanel = new javax.swing.JPanel();
        atScrollPane = new javax.swing.JScrollPane();
        scaffoldStatsTable = new ScaffoldTable()
        ;
        fragmentSizePanel = new javax.swing.JPanel();
        fsScrollPane = new javax.swing.JScrollPane();
        fragmentStatsTable = new javax.swing.JTable();
        plotsPanel = new javax.swing.JPanel();
        plotsTabbedPane = new javax.swing.JTabbedPane();
        showHideButton = new javax.swing.JButton();
        statusPanel = new javax.swing.JPanel();
        stopButton = new javax.swing.JButton();
        statusLabel = new javax.swing.JLabel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        addAssembliesMenuItem = new javax.swing.JMenuItem();
        removeAssembliesMenuItem = new javax.swing.JMenuItem();
        loadAdjDotMenuItem = new javax.swing.JMenuItem();
        exitMenuItem = new javax.swing.JMenuItem();
        viewMenu = new javax.swing.JMenu();
        clearStatsMenuItem = new javax.swing.JMenuItem();
        clearPlotsMenuItem = new javax.swing.JMenuItem();
        clearNavigatorMenuItem = new javax.swing.JMenuItem();
        resetMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        aboutMenuItem = new javax.swing.JMenuItem();

        contigSequenceFrame.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                contigSequenceFrameComponentShown(evt);
            }
        });

        contigSequenceTextArea.setEditable(false);
        contigSequenceTextArea.setFont(new java.awt.Font("Courier", 0, 13)); // NOI18N
        contigSequenceTextArea.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        jScrollPane3.setViewportView(contigSequenceTextArea);

        contigSequenceFrame.getContentPane().add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel1.setLayout(new java.awt.GridBagLayout());

        copyToClipboardButton.setText("Copy to clipboard");
        copyToClipboardButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyToClipboardButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(copyToClipboardButton, gridBagConstraints);

        blatButton.setText("BLAT");
        blatButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                blatButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(blatButton, gridBagConstraints);

        noteLabel.setText("<html><br>NOTE: To conform with UCSC's BLAT use restrictions: <ul> <li>Each batch query is limited to the smaller of 8000 characters or 25 sequences or less</li> <li>Maximum of one query every 15 seconds</li> </ul> </html>");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        jPanel1.add(noteLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel1.add(contigIdsComboBox, gridBagConstraints);

        exploreNeighborhoodButton.setText("Explore the neighborhood of contig:");
        exploreNeighborhoodButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exploreNeighborhoodButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        jPanel1.add(exploreNeighborhoodButton, gridBagConstraints);

        genomeComboBox.setAutoscrolls(true);
        genomeComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                genomeComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(genomeComboBox, gridBagConstraints);

        assemblyComboBox.setAutoscrolls(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(assemblyComboBox, gridBagConstraints);

        genomeLabel.setText("Genome");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        jPanel1.add(genomeLabel, gridBagConstraints);

        assemblyLabel.setText("Assembly");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        jPanel1.add(assemblyLabel, gridBagConstraints);

        sequencesTooLongWarningLabel.setForeground(java.awt.Color.red);
        sequencesTooLongWarningLabel.setText("All sequences are too long. Please BLAT manually.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        jPanel1.add(sequencesTooLongWarningLabel, gridBagConstraints);

        contigSequenceFrame.getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);

        jMenu1.setText("View");

        lineWrapCheckBoxMenuItem.setText("line wrap");
        lineWrapCheckBoxMenuItem.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                lineWrapCheckBoxMenuItemItemStateChanged(evt);
            }
        });
        jMenu1.add(lineWrapCheckBoxMenuItem);

        jMenuBar1.add(jMenu1);

        contigSequenceFrame.setJMenuBar(jMenuBar1);

        searchTypeButtonGroup.add(sequenceSearchRadioButtonMenuItem);
        sequenceSearchRadioButtonMenuItem.setMnemonic('S');
        sequenceSearchRadioButtonMenuItem.setText("Find contig sequences");
        sequenceSearchRadioButtonMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/magnifier_seq.png"))); // NOI18N
        sequenceSearchRadioButtonMenuItem.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                sequenceSearchRadioButtonMenuItemItemStateChanged(evt);
            }
        });
        searchTypePopupMenu.add(sequenceSearchRadioButtonMenuItem);

        searchTypeButtonGroup.add(navigatorSearchRadioButtonMenuItem);
        navigatorSearchRadioButtonMenuItem.setMnemonic('A');
        navigatorSearchRadioButtonMenuItem.setSelected(true);
        navigatorSearchRadioButtonMenuItem.setText("Find contig in Navigator");
        navigatorSearchRadioButtonMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/magnifier_nav.png"))); // NOI18N
        navigatorSearchRadioButtonMenuItem.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                navigatorSearchRadioButtonMenuItemItemStateChanged(evt);
            }
        });
        searchTypePopupMenu.add(navigatorSearchRadioButtonMenuItem);

        copyScaffoldStatsMenuItem.setText("Copy table to clipboard");
        copyScaffoldStatsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyScaffoldStatsMenuItemActionPerformed(evt);
            }
        });
        scaffoldStatsPopupMenu.add(copyScaffoldStatsMenuItem);

        copyFragmentStatsMenuItem.setText("Copy table to clipboard");
        copyFragmentStatsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyFragmentStatsMenuItemActionPerformed(evt);
            }
        });
        fragmentStatsPopupMenu.add(copyFragmentStatsMenuItem);

        removeSelectedMenuItem.setText("remove selected");
        removeSelectedMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeSelectedMenuItemActionPerformed(evt);
            }
        });
        assembliesPopupMenu.add(removeSelectedMenuItem);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle(this.DEFAULT_TITLE);
        setMinimumSize(new java.awt.Dimension(640, 480));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        topPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        topPanel.setMaximumSize(new java.awt.Dimension(2147483647, 30));
        topPanel.setPreferredSize(new java.awt.Dimension(1024, 30));
        topPanel.setLayout(new java.awt.GridBagLayout());

        addressbarPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,26));
        addressbarPanel.setPreferredSize(new java.awt.Dimension(100, 26));
        addressbarPanel.setLayout(new java.awt.GridBagLayout());

        addAssembliesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/menu_plus.png"))); // NOI18N
        addAssembliesButton.setToolTipText("Add one or more assemblies");
        addAssembliesButton.setMaximumSize(new java.awt.Dimension(26, 26));
        addAssembliesButton.setMinimumSize(new java.awt.Dimension(26, 26));
        addAssembliesButton.setPreferredSize(new java.awt.Dimension(26, 26));
        addAssembliesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAssembliesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        addressbarPanel.add(addAssembliesButton, gridBagConstraints);

        removeAssembliesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/menu_minus.png"))); // NOI18N
        removeAssembliesButton.setToolTipText("Remove the selected assemblies");
        removeAssembliesButton.setEnabled(false);
        removeAssembliesButton.setMaximumSize(new java.awt.Dimension(26, 26));
        removeAssembliesButton.setMinimumSize(new java.awt.Dimension(26, 26));
        removeAssembliesButton.setPreferredSize(new java.awt.Dimension(26, 26));
        removeAssembliesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAssembliesButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        addressbarPanel.add(removeAssembliesButton, gridBagConstraints);

        loadAdjDotButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/adj_dot.png"))); // NOI18N
        loadAdjDotButton.setToolTipText("Load a DOT or ADJ file");
        loadAdjDotButton.setMaximumSize(new java.awt.Dimension(26, 26));
        loadAdjDotButton.setMinimumSize(new java.awt.Dimension(26, 26));
        loadAdjDotButton.setPreferredSize(new java.awt.Dimension(26, 26));
        loadAdjDotButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadAdjDotButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 0, 2);
        addressbarPanel.add(loadAdjDotButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        topPanel.add(addressbarPanel, gridBagConstraints);

        jSeparator5.setOrientation(javax.swing.SwingConstants.VERTICAL);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 0, 6);
        topPanel.add(jSeparator5, gridBagConstraints);

        contigSearchPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,26));
        contigSearchPanel.setLayout(new java.awt.BorderLayout());

        searchButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/magnifier_nav_arrow.png"))); // NOI18N
        searchButton.putClientProperty("JButton.buttonType", "square");
        searchButton.setToolTipText(this.FIND_CONTIG_NAV_TOOLTIP);
        searchButton.setMaximumSize(new java.awt.Dimension(26, 26));
        searchButton.setMinimumSize(new java.awt.Dimension(26, 26));
        searchButton.setPreferredSize(new java.awt.Dimension(26, 26));
        searchButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                searchButtonMousePressed(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                searchButtonMouseReleased(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                searchButtonMouseExited(evt);
            }
        });
        contigSearchPanel.add(searchButton, java.awt.BorderLayout.EAST);

        contigSearchTextField.setForeground(java.awt.Color.gray);
        contigSearchTextField.setText(NAVIGATOR_SEARCH_QUERY_FORMAT);
        contigSearchTextField.setToolTipText(NAVIGATOR_SEARCH_QUERY_FORMAT);
        contigSearchTextField.setMaximumSize(new java.awt.Dimension(250, 23));
        contigSearchTextField.setMinimumSize(new java.awt.Dimension(250, 23));
        contigSearchTextField.setPreferredSize(new java.awt.Dimension(250, 23));
        contigSearchTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                contigSearchTextFieldFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                contigSearchTextFieldFocusLost(evt);
            }
        });
        contigSearchTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                contigSearchTextFieldActionPerformed(evt);
            }
        });
        contigSearchPanel.add(contigSearchTextField, java.awt.BorderLayout.CENTER);

        backButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/back_arrow.png"))); // NOI18N
        backButton.putClientProperty("JButton.buttonType", "square");
        backButton.setToolTipText("Go to last contig searched in Navigator");
        backButton.setEnabled(false);
        backButton.setMaximumSize(new java.awt.Dimension(26, 26));
        backButton.setMinimumSize(new java.awt.Dimension(26, 26));
        backButton.setPreferredSize(new java.awt.Dimension(26, 26));
        backButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backButtonActionPerformed(evt);
            }
        });
        contigSearchPanel.add(backButton, java.awt.BorderLayout.WEST);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 5;
        gridBagConstraints.gridy = 0;
        topPanel.add(contigSearchPanel, gridBagConstraints);

        getContentPane().add(topPanel, java.awt.BorderLayout.NORTH);

        mainPanel.setLayout(new java.awt.GridBagLayout());

        assembliesAndSettingsSplitPane.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        assembliesAndSettingsSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        assembliesAndSettingsSplitPane.setResizeWeight(1.0);
        assembliesAndSettingsSplitPane.setContinuousLayout(true);
        assembliesAndSettingsSplitPane.setOneTouchExpandable(true);

        kDirPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0), "ASSEMBLIES"));
        kDirPanel.setLayout(new java.awt.GridBagLayout());

        kDirScrollPane.setOpaque(false);
        kDirScrollPane.setPreferredSize(new java.awt.Dimension(200, 200));

        assembliesAndColorsTable.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        assembliesAndColorsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", ""
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        assembliesAndColorsTable.setComponentPopupMenu(assembliesPopupMenu);
        assembliesAndColorsTable.setFillsViewportHeight(true);
        assembliesAndColorsTable.setFocusable(false);
        assembliesAndColorsTable.setIntercellSpacing(new java.awt.Dimension(0, 0));
        assembliesAndColorsTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        assembliesAndColorsTable.setShowHorizontalLines(false);
        assembliesAndColorsTable.setShowVerticalLines(false);
        assembliesAndColorsTable.getTableHeader().setReorderingAllowed(false);
        assembliesAndColorsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                assembliesAndColorsTableMouseClicked(evt);
            }
        });
        kDirScrollPane.setViewportView(assembliesAndColorsTable);
        if (assembliesAndColorsTable.getColumnModel().getColumnCount() > 0) {
            assembliesAndColorsTable.getColumnModel().getColumn(0).setMinWidth(22);
            assembliesAndColorsTable.getColumnModel().getColumn(0).setPreferredWidth(22);
            assembliesAndColorsTable.getColumnModel().getColumn(0).setMaxWidth(22);
            assembliesAndColorsTable.getColumnModel().getColumn(0).setCellRenderer(new ColorRenderer(true));
        }

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        kDirPanel.add(kDirScrollPane, gridBagConstraints);

        showStatsToggleButton.setBackground(new java.awt.Color(178, 223, 138));
        showStatsToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/stats3.png"))); // NOI18N
        showStatsToggleButton.setToolTipText("Show \"Statistics\" pane");
        showStatsToggleButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        showStatsToggleButton.setEnabled(false);
        showStatsToggleButton.setFocusPainted(false);
        showStatsToggleButton.setFocusable(false);
        showStatsToggleButton.setMaximumSize(new java.awt.Dimension(66, 56));
        showStatsToggleButton.setMinimumSize(new java.awt.Dimension(66, 56));
        showStatsToggleButton.setPreferredSize(new java.awt.Dimension(66, 56));
        showStatsToggleButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showStatsToggleButtonItemStateChanged(evt);
            }
        });
        showStatsToggleButton.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                showStatsToggleButtonPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        kDirPanel.add(showStatsToggleButton, gridBagConstraints);

        showPlotsToggleButton.setBackground(new java.awt.Color(178, 223, 138));
        showPlotsToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/plots3.png"))); // NOI18N
        showPlotsToggleButton.setToolTipText("Show \"Plots\" pane");
        showPlotsToggleButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        showPlotsToggleButton.setEnabled(false);
        showPlotsToggleButton.setFocusPainted(false);
        showPlotsToggleButton.setFocusable(false);
        showPlotsToggleButton.setMaximumSize(new java.awt.Dimension(66, 56));
        showPlotsToggleButton.setMinimumSize(new java.awt.Dimension(66, 56));
        showPlotsToggleButton.setPreferredSize(new java.awt.Dimension(66, 56));
        showPlotsToggleButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showPlotsToggleButtonItemStateChanged(evt);
            }
        });
        showPlotsToggleButton.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                showPlotsToggleButtonPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        kDirPanel.add(showPlotsToggleButton, gridBagConstraints);

        showNavigatorToggleButton.setBackground(new java.awt.Color(178, 223, 138));
        showNavigatorToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/navigator3.png"))); // NOI18N
        showNavigatorToggleButton.setToolTipText("Show \"Navigator\" pane");
        showNavigatorToggleButton.setBorder(javax.swing.BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
        showNavigatorToggleButton.setEnabled(false);
        showNavigatorToggleButton.setFocusPainted(false);
        showNavigatorToggleButton.setFocusable(false);
        showNavigatorToggleButton.setMaximumSize(new java.awt.Dimension(66, 56));
        showNavigatorToggleButton.setMinimumSize(new java.awt.Dimension(66, 56));
        showNavigatorToggleButton.setPreferredSize(new java.awt.Dimension(66, 56));
        showNavigatorToggleButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showNavigatorToggleButtonItemStateChanged(evt);
            }
        });
        showNavigatorToggleButton.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                showNavigatorToggleButtonPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1.0;
        kDirPanel.add(showNavigatorToggleButton, gridBagConstraints);

        assembliesAndSettingsSplitPane.setTopComponent(kDirPanel);

        settingsPanel.setLayout(new java.awt.GridBagLayout());

        settingsTabbedPane.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0), "SETTINGS"));

        jPanel3.setLayout(new java.awt.GridBagLayout());

        mclLabel.setLabelFor(minContigLengthFormattedTextField);
        mclLabel.setText("min. contig length (bp):");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(mclLabel, gridBagConstraints);

        minContigLengthFormattedTextField.setBackground(java.awt.Color.white);
        minContigLengthFormattedTextField.setForeground(Color.BLACK);
        minContigLengthFormattedTextField.setToolTipText("Enter a positive integer here");
        minContigLengthFormattedTextField.setEnabled(false);
        minContigLengthFormattedTextField.setMinimumSize(new java.awt.Dimension(130, 23));
        minContigLengthFormattedTextField.setPreferredSize(new java.awt.Dimension(130, 23));
        minContigLengthFormattedTextField.setValue(new Long(200));
        minContigLengthFormattedTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                minContigLengthFormattedTextFieldFocusGained(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 6, 0);
        jPanel3.add(minContigLengthFormattedTextField, gridBagConstraints);

        unitLabel.setLabelFor(yAxisUnitComboBox);
        unitLabel.setText("N50-plot Y-axis unit:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel3.add(unitLabel, gridBagConstraints);

        yAxisUnitComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "percentile", "reconstruction" }));
        yAxisUnitComboBox.setEnabled(false);
        yAxisUnitComboBox.setMinimumSize(new java.awt.Dimension(130, 23));
        yAxisUnitComboBox.setPreferredSize(new java.awt.Dimension(130, 23));
        yAxisUnitComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                yAxisUnitComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 6, 0);
        jPanel3.add(yAxisUnitComboBox, gridBagConstraints);

        scaleLabel.setLabelFor(xAxisScaleComboBox);
        scaleLabel.setText("N50-plot X-axis scale:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel3.add(scaleLabel, gridBagConstraints);

        xAxisScaleComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "linear", "logarithmic" }));
        xAxisScaleComboBox.setEnabled(false);
        xAxisScaleComboBox.setMinimumSize(new java.awt.Dimension(130, 23));
        xAxisScaleComboBox.setPreferredSize(new java.awt.Dimension(130, 23));
        xAxisScaleComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                xAxisScaleComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 6, 0);
        jPanel3.add(xAxisScaleComboBox, gridBagConstraints);

        drawN50plotCheckBox.setSelected(true);
        drawN50plotCheckBox.setText("N50-plot");
        drawN50plotCheckBox.setEnabled(false);
        drawN50plotCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                drawN50plotCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(drawN50plotCheckBox, gridBagConstraints);

        drawFragSizeDistCheckBox.setText("Fragment Size Distribution");
        drawFragSizeDistCheckBox.setEnabled(false);
        drawFragSizeDistCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                drawFragSizeDistCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(drawFragSizeDistCheckBox, gridBagConstraints);

        selectLibrariesButton.setBackground(new java.awt.Color(239, 59, 44));
        selectLibrariesButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/right_menu.png"))); // NOI18N
        selectLibrariesButton.setText("select libraries");
        selectLibrariesButton.setEnabled(false);
        selectLibrariesButton.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        selectLibrariesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                selectLibrariesButtonActionPerformed(evt);
            }
        });
        selectLibrariesButton.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                selectLibrariesButtonPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 20, 0, 0);
        jPanel3.add(selectLibrariesButton, gridBagConstraints);

        drawCovPlotCheckBox.setText("Coverage Plot");
        drawCovPlotCheckBox.setEnabled(false);
        drawCovPlotCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                drawCovPlotCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 3;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        jPanel3.add(drawCovPlotCheckBox, gridBagConstraints);

        settingsTabbedPane.addTab("Stats/Plots", null, jPanel3, "Settings for Statistics and Plots");

        explorerSettingsPanel.setLayout(new java.awt.GridBagLayout());

        showLengthCheckBox.setSelected(true);
        showLengthCheckBox.setText("length");
        showLengthCheckBox.setEnabled(false);
        showLengthCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showLengthCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(showLengthCheckBox, gridBagConstraints);

        showLabelsCheckBox.setSelected(true);
        showLabelsCheckBox.setText("labels");
        showLabelsCheckBox.setEnabled(false);
        showLabelsCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showLabelsCheckBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(showLabelsCheckBox, gridBagConstraints);

        lengthSlider.setMajorTickSpacing(100);
        lengthSlider.setMinorTickSpacing(1);
        lengthSlider.setPaintLabels(true);
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(0, new JLabel("<html>10<sup>2</sup></html>"));
        labelTable.put(100, new JLabel("<html>10<sup>3</sup></html>"));
        lengthSlider.setLabelTable(labelTable);
        lengthSlider.setEnabled(false);
        lengthSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                lengthSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(lengthSlider, gridBagConstraints);

        assemblyModeButtonGroup.add(peRadioButton);
        peRadioButton.setSelected(true);
        peRadioButton.setText("contigs");
        peRadioButton.setEnabled(false);
        peRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                peRadioButtonItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        explorerSettingsPanel.add(peRadioButton, gridBagConstraints);

        assemblyModeButtonGroup.add(seRadioButton);
        seRadioButton.setText("unitigs");
        seRadioButton.setEnabled(false);
        seRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                seRadioButtonItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.gridwidth = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(seRadioButton, gridBagConstraints);

        stepSizeSlider.setMajorTickSpacing(1);
        stepSizeSlider.setMaximum(STEP_SIZES.length-1);
        stepSizeSlider.setPaintLabels(true);
        stepSizeSlider.setPaintTicks(true);
        stepSizeSlider.setSnapToTicks(true);
        stepSizeSlider.setValue(0);
        stepSizeSlider.setEnabled(false);
        stepSizeSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                stepSizeSliderStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(stepSizeSlider, gridBagConstraints);

        stepSizeButtonGroup.add(showAllRadioButton);
        showAllRadioButton.setText("show neighborhood");
        showAllRadioButton.setEnabled(false);
        showAllRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showAllRadioButtonItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(showAllRadioButton, gridBagConstraints);

        stepSizeButtonGroup.add(showNeighborsRadioButton);
        showNeighborsRadioButton.setSelected(true);
        showNeighborsRadioButton.setText("show extensions");
        showNeighborsRadioButton.setEnabled(false);
        showNeighborsRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                showNeighborsRadioButtonItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(showNeighborsRadioButton, gridBagConstraints);

        stepSizeLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        stepSizeLabel.setText("depth");
        stepSizeLabel.setEnabled(false);
        stepSizeLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTH;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(stepSizeLabel, gridBagConstraints);

        assemblyModeButtonGroup.add(scaffoldRadioButton);
        scaffoldRadioButton.setText("scaffolds");
        scaffoldRadioButton.setEnabled(false);
        scaffoldRadioButton.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                scaffoldRadioButtonItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        explorerSettingsPanel.add(scaffoldRadioButton, gridBagConstraints);

        settingsTabbedPane.addTab("Navigator", null, explorerSettingsPanel, "Settings for Navigator");

        jPanel2.setLayout(new java.awt.GridBagLayout());

        fileLabel.setLabelFor(filesComboBox);
        fileLabel.setText("file to compare: ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(fileLabel, gridBagConstraints);

        filesComboBox.setEnabled(false);
        filesComboBox.setMinimumSize(new java.awt.Dimension(200, 23));
        filesComboBox.setPreferredSize(new java.awt.Dimension(200, 23));
        filesComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                filesComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 6, 0);
        jPanel2.add(filesComboBox, gridBagConstraints);

        lengthLabel.setLabelFor(unitOfLengthComboBox);
        lengthLabel.setText("unit of length:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.SOUTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(lengthLabel, gridBagConstraints);

        unitOfLengthComboBox.setEnabled(false);
        unitOfLengthComboBox.setMinimumSize(new java.awt.Dimension(200, 23));
        unitOfLengthComboBox.setPreferredSize(new java.awt.Dimension(200, 23));
        unitOfLengthComboBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                unitOfLengthComboBoxItemStateChanged(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 2, 6, 0);
        jPanel2.add(unitOfLengthComboBox, gridBagConstraints);

        useFastaIndexCheckBox.setSelected(true);
        useFastaIndexCheckBox.setText("use FASTA index");
        useFastaIndexCheckBox.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weighty = 1.0;
        jPanel2.add(useFastaIndexCheckBox, gridBagConstraints);

        settingsTabbedPane.addTab("Advanced Options", null, jPanel2, "Advanced Options");

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        settingsPanel.add(settingsTabbedPane, gridBagConstraints);

        applyButton.setBackground(new java.awt.Color(178, 223, 138));
        applyButton.setText("Apply");
        applyButton.setEnabled(false);
        applyButton.putClientProperty("JButton.buttonType", "square");
        applyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                applyButtonActionPerformed(evt);
            }
        });
        applyButton.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                applyButtonPropertyChange(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(2, 2, 2, 2);
        settingsPanel.add(applyButton, gridBagConstraints);

        assembliesAndSettingsSplitPane.setBottomComponent(settingsPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(assembliesAndSettingsSplitPane, gridBagConstraints);

        displayPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        displayPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                displayPanelComponentResized(evt);
            }
        });
        displayPanel.setLayout(new java.awt.GridBagLayout());

        statsSplitPane.setBorder(statsBorder);
        statsSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        statsSplitPane.setContinuousLayout(true);
        statsSplitPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        statsSplitPane.setMinimumSize(new java.awt.Dimension(1, 0));
        statsSplitPane.setOneTouchExpandable(true);
        statsSplitPane.setPreferredSize(new java.awt.Dimension(1, 0));
        statsSplitPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                statsSplitPaneMouseClicked(evt);
            }
        });
        statsSplitPane.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                statsSplitPaneComponentShown(evt);
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                statsSplitPaneComponentHidden(evt);
            }
        });

        assembliesPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0), "Scaffold Sizes"));
        assembliesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        assembliesPanel.setMinimumSize(new java.awt.Dimension(1, 0));
        assembliesPanel.setPreferredSize(new java.awt.Dimension(1, 0));
        assembliesPanel.setLayout(new java.awt.GridBagLayout());

        atScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        atScrollPane.setMinimumSize(new java.awt.Dimension(1, 0));
        atScrollPane.setPreferredSize(new java.awt.Dimension(1, 0));

        scaffoldStatsTable.setAutoCreateRowSorter(true);
        scaffoldStatsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Assembly", "N50", "Contiguity", "Reconstruction", "Span", "Median k-mer coverage", "n", "n (l>=MCL)", "n (l>=N50)"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        scaffoldStatsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        scaffoldStatsTable.setComponentPopupMenu(scaffoldStatsPopupMenu);
        scaffoldStatsTable.getTableHeader().setReorderingAllowed(false);
        atScrollPane.setViewportView(scaffoldStatsTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        assembliesPanel.add(atScrollPane, gridBagConstraints);

        statsSplitPane.setBottomComponent(assembliesPanel);

        fragmentSizePanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0), "Fragment Sizes"));
        fragmentSizePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        fragmentSizePanel.setMinimumSize(new java.awt.Dimension(1, 0));
        fragmentSizePanel.setPreferredSize(new java.awt.Dimension(1, 0));
        fragmentSizePanel.setLayout(new java.awt.GridBagLayout());

        fsScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE,Integer.MAX_VALUE));
        fsScrollPane.setMinimumSize(new java.awt.Dimension(1, 0));
        fsScrollPane.setPreferredSize(new java.awt.Dimension(1, 0));

        fragmentStatsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Library", "Orientation", "Min", "Q1", "Median", "Q3", "Max", "Mean", "Stdev", "Stdev/Mean", "Q factor"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Long.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class, java.lang.Double.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        fragmentStatsTable.setDefaultRenderer(Double.class, new MyDoubleCellRenderer());
        fragmentStatsTable.getColumnModel().getColumn(9).setCellRenderer(new MyPercentCellRenderer());
        fragmentStatsTable.setToolTipText("double-click to to plot");
        fragmentStatsTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        fragmentStatsTable.setComponentPopupMenu(fragmentStatsPopupMenu);
        fragmentStatsTable.getTableHeader().setReorderingAllowed(false);
        TableRowSorter<DefaultTableModel> fragmentSizeTableSorter = new TableRowSorter<DefaultTableModel>((DefaultTableModel)fragmentStatsTable.getModel());
        fragmentStatsTable.setRowSorter(fragmentSizeTableSorter);
        fragmentSizeTableSorter.setSortsOnUpdates(true);
        fragmentStatsTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fragmentStatsTableMouseClicked(evt);
            }
        });
        fsScrollPane.setViewportView(fragmentStatsTable);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        fragmentSizePanel.add(fsScrollPane, gridBagConstraints);

        statsSplitPane.setTopComponent(fragmentSizePanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.001;
        displayPanel.add(statsSplitPane, gridBagConstraints);

        plotsPanel.setBorder(plotsBorder);
        plotsPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                plotsPanelMouseClicked(evt);
            }
        });
        plotsPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                plotsPanelComponentShown(evt);
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                plotsPanelComponentHidden(evt);
            }
        });
        plotsPanel.setLayout(new java.awt.GridBagLayout());

        plotsTabbedPane.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        plotsTabbedPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        plotsTabbedPane.setMinimumSize(new java.awt.Dimension(1, 1));
        plotsTabbedPane.setPreferredSize(new java.awt.Dimension(10, 10));
        plotsTabbedPane.setUI(new MyTabbedPaneUI());
        plotsTabbedPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                plotsTabbedPaneMousePressed(evt);
            }
        });
        plotsTabbedPane.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent evt) {
                plotsTabbedPaneComponentAdded(evt);
            }
            public void componentRemoved(java.awt.event.ContainerEvent evt) {
                plotsTabbedPaneComponentRemoved(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        plotsPanel.add(plotsTabbedPane, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.33;
        gridBagConstraints.weighty = 1.0;
        displayPanel.add(plotsPanel, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        mainPanel.add(displayPanel, gridBagConstraints);

        showHideButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/left.png"))); // NOI18N
        showHideButton.setBorderPainted(false);
        showHideButton.setContentAreaFilled(false);
        showHideButton.setFocusPainted(false);
        showHideButton.setMaximumSize(new java.awt.Dimension(8, 15));
        showHideButton.setMinimumSize(new java.awt.Dimension(8, 15));
        showHideButton.setPreferredSize(new java.awt.Dimension(8, 15));
        showHideButton.setPressedIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/left_over.png"))); // NOI18N
        showHideButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/left_over.png"))); // NOI18N
        showHideButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showHideButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 0, 1);
        mainPanel.add(showHideButton, gridBagConstraints);

        getContentPane().add(mainPanel, java.awt.BorderLayout.CENTER);

        statusPanel.setLayout(new java.awt.GridBagLayout());

        stopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/stop.png"))); // NOI18N
        stopButton.setToolTipText("Stop");
        stopButton.setEnabled(false);
        stopButton.setMaximumSize(new java.awt.Dimension(25, 25));
        stopButton.setMinimumSize(new java.awt.Dimension(25, 25));
        stopButton.setPreferredSize(new java.awt.Dimension(25, 25));
        stopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                stopButtonActionPerformed(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        statusPanel.add(stopButton, gridBagConstraints);

        statusLabel.setText("Add one or more assemblies or load a DOT/ADJ file");
        statusLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        statusLabel.setMaximumSize(new java.awt.Dimension(100000, 25));
        statusLabel.setMinimumSize(new java.awt.Dimension(0, 25));
        statusLabel.setPreferredSize(new java.awt.Dimension(4, 25));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        statusPanel.add(statusLabel, gridBagConstraints);

        getContentPane().add(statusPanel, java.awt.BorderLayout.SOUTH);

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        addAssembliesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/menu_plus.png"))); // NOI18N
        addAssembliesMenuItem.setMnemonic('A');
        addAssembliesMenuItem.setText("Add assemblies");
        addAssembliesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addAssembliesMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(addAssembliesMenuItem);

        removeAssembliesMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/menu_minus.png"))); // NOI18N
        removeAssembliesMenuItem.setMnemonic('R');
        removeAssembliesMenuItem.setText("Remove selected assemblies");
        removeAssembliesMenuItem.setEnabled(false);
        removeAssembliesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeAssembliesMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(removeAssembliesMenuItem);

        loadAdjDotMenuItem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/menu_adj_dot.png"))); // NOI18N
        loadAdjDotMenuItem.setMnemonic('L');
        loadAdjDotMenuItem.setText("Load ADJ/DOT file");
        loadAdjDotMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadAdjDotMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(loadAdjDotMenuItem);

        exitMenuItem.setMnemonic('X');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        viewMenu.setMnemonic('V');
        viewMenu.setText("View");

        clearStatsMenuItem.setMnemonic('S');
        clearStatsMenuItem.setText("Clear Statistics pane");
        clearStatsMenuItem.setEnabled(false);
        clearStatsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearStatsMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(clearStatsMenuItem);

        clearPlotsMenuItem.setMnemonic('P');
        clearPlotsMenuItem.setText("Clear Plots pane");
        clearPlotsMenuItem.setEnabled(false);
        clearPlotsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearPlotsMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(clearPlotsMenuItem);

        clearNavigatorMenuItem.setMnemonic('N');
        clearNavigatorMenuItem.setText("Clear Navigator pane");
        clearNavigatorMenuItem.setEnabled(false);
        clearNavigatorMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearNavigatorMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(clearNavigatorMenuItem);

        resetMenuItem.setMnemonic('R');
        resetMenuItem.setText("Reset");
        resetMenuItem.setEnabled(false);
        resetMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetMenuItemActionPerformed(evt);
            }
        });
        viewMenu.add(resetMenuItem);

        menuBar.add(viewMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }// </editor-fold>//GEN-END:initComponents

    private void setUpInternalComponentsListeners(){
        statsComponentsListener = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {}
            @Override
            public void mousePressed(MouseEvent me) {
                statsHasFocus();
            }
            @Override
            public void mouseReleased(MouseEvent me) {}
            @Override
            public void mouseEntered(MouseEvent me) {}
            @Override
            public void mouseExited(MouseEvent me) {}
        };

        plotsComponentsListener = new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent me) {}
            @Override
            public void mousePressed(MouseEvent me) {
                plotsHasFocus();
            }
            @Override
            public void mouseReleased(MouseEvent me) {}
            @Override
            public void mouseEntered(MouseEvent me) {}
            @Override
            public void mouseExited(MouseEvent me) {}
        };

        navigatorComponentsListener = new MouseListener(){
            @Override
            public void mouseClicked(MouseEvent me) {}
            @Override
            public void mousePressed(MouseEvent me) {
                navigatorHasFocus();
            }
            @Override
            public void mouseReleased(MouseEvent me) {}
            @Override
            public void mouseEntered(MouseEvent me) {}
            @Override
            public void mouseExited(MouseEvent me) {}
        };
    }

    private void addComponentsMouseListener(Component c, MouseListener l){
        c.addMouseListener(l);
        if(c instanceof JComponent){
            for(Component ci : ((JComponent)c).getComponents()){
                addComponentsMouseListener(ci, l);
            }
        }
    }


    private void filesComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_filesComboBoxItemStateChanged
//        String selectedFile = (String)filesComboBox.getSelectedItem();
        String selectedUnit = (String)unitOfLengthComboBox.getSelectedItem();
        int selectedUnitIndex = -1;
//        if(selectedFile.endsWith(".fa")) {
            selectedUnitIndex = UNITS_FOR_FA.getIndexOf(selectedUnit);
            unitOfLengthComboBox.setModel(UNITS_FOR_FA);
//        }
//        else {
//            selectedUnitIndex = UNITS_FOR_ADJ.getIndexOf(selectedUnit);
//            unitOfLengthComboBox.setModel(UNITS_FOR_ADJ);
//        }

        if(selectedUnitIndex >= 0) {
            unitOfLengthComboBox.setSelectedItem(selectedUnit);
        }
        else {
            unitOfLengthComboBox.setSelectedIndex(0);
        }

        if(filesComboBox.isEnabled()){
            validateSettings();
        }
    }//GEN-LAST:event_filesComboBoxItemStateChanged

    private void unitOfLengthComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_unitOfLengthComboBoxItemStateChanged
        if(unitOfLengthComboBox.isEnabled()){
            validateSettings();
        }
    }//GEN-LAST:event_unitOfLengthComboBoxItemStateChanged

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.out.println("ABySS-Explorer was terminated.");
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void addAssembliesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAssembliesMenuItemActionPerformed
        try{
            loadDirectory();
        }
        catch(InterruptedException ex){
            String doneText = "Stopped by user.";
            setStatus(doneText, true, ERROR_STATUS);
        }
    }//GEN-LAST:event_addAssembliesMenuItemActionPerformed

    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
//        JTextArea textArea = new JTextArea(about());
//        textArea.setEditable(false);
//        textArea.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
//        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "About", JOptionPane.PLAIN_MESSAGE);
        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setPreferredSize(new Dimension(500,200));
        editorPane.setContentType("text/html");
        editorPane.setText(abouthtml());
        if(Desktop.isDesktopSupported()){
            editorPane.addHyperlinkListener(new HyperlinkListener(){
                @Override
                public void hyperlinkUpdate(HyperlinkEvent e) {
                    if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED){
                        //System.out.println("Link clicked: " + e.getURL().toString());
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.browse(e.getURL().toURI());
                        } catch (URISyntaxException ex) {
                            System.out.println(ex.getMessage());
                        } catch (IOException ex) {
                            System.out.println(ex.getMessage());
                        }
                    }
                }            
            });
        }
        else{
            System.out.println("Java's Desktop API is not supported on your machine. Copy the URL and view it in your browser.");
        }
        JOptionPane.showMessageDialog(this, new JScrollPane(editorPane), "About", JOptionPane.PLAIN_MESSAGE);
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void contigSearchTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_contigSearchTextFieldActionPerformed
        SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                try{
                    contigSequenceFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    performSearch();
                } finally{
                    contigSequenceFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                return null;
            }
        };
        worker.execute();
    }//GEN-LAST:event_contigSearchTextFieldActionPerformed

    private void copyToClipboardButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyToClipboardButtonActionPerformed

        java.awt.datatransfer.StringSelection seq = new java.awt.datatransfer.StringSelection( contigSequenceTextArea.getText() );
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(seq, seq);
        JOptionPane.showMessageDialog(contigSequenceFrame, "The sequence has been copied to the system clipboard.");
    }//GEN-LAST:event_copyToClipboardButtonActionPerformed

    private void blatButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_blatButtonActionPerformed
        SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                try{
                    contigSequenceFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    openBlatPageInBrowser();
                } finally{
                    contigSequenceFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }
                return null;
            }
        };
        worker.execute();
    }//GEN-LAST:event_blatButtonActionPerformed

    private void contigSearchTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_contigSearchTextFieldFocusGained
        String query = contigSearchTextField.getText();
        contigSearchTextField.setForeground(Color.BLACK);

        if(query.equals(SEQUENCE_SEARCH_QUERY_FORMAT) ||
                query.equals(NAVIGATOR_SEARCH_QUERY_FORMAT)){
            contigSearchTextField.setText("");
        }
    }//GEN-LAST:event_contigSearchTextFieldFocusGained

    private void contigSearchTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_contigSearchTextFieldFocusLost
        String query = contigSearchTextField.getText();

        if(sequenceSearchRadioButtonMenuItem.isSelected()) {
            if(query.equals(SEQUENCE_SEARCH_QUERY_FORMAT)) {
                contigSearchTextField.setForeground(Color.GRAY);
            }
            else if(query.equals("")) {
                contigSearchTextField.setText(SEQUENCE_SEARCH_QUERY_FORMAT);
                contigSearchTextField.setForeground(Color.GRAY);
            }
        }
        else if(navigatorSearchRadioButtonMenuItem.isSelected()){
            if(query.equals(NAVIGATOR_SEARCH_QUERY_FORMAT)) {
                contigSearchTextField.setForeground(Color.GRAY);
            }
            else if(query.equals("")) {
                contigSearchTextField.setText(NAVIGATOR_SEARCH_QUERY_FORMAT);
                contigSearchTextField.setForeground(Color.GRAY);
            }
        }

    }//GEN-LAST:event_contigSearchTextFieldFocusLost

    private void clearPlotsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearPlotsMenuItemActionPerformed
        int numTabs = plotsTabbedPane.getTabCount();
        if(numTabs > 0) {
            String msg = "All plots would be removed.\nWould you like to continue?";
            int value = JOptionPane.showConfirmDialog(plotsTabbedPane.getTopLevelAncestor(), msg, "Clear Plots", JOptionPane.YES_NO_OPTION);

            if(value == JOptionPane.YES_OPTION) {
                plotsTabbedPane.removeAll();
                showPlotsToggleButton.setSelected(false);
                plotsForNavigator = false;
                plotsSplitPane = null;
                n50plot = null;
                covplot = null;

                setStatus("All plots cleared", false, NORMAL_STATUS);
            }
        }
        else{
            if(plotsPanel.isVisible()){
                plotsPanel.setVisible(false);
            }
            if(showPlotsToggleButton.isSelected()){
                showPlotsToggleButton.setSelected(false);
            }
        }
    }//GEN-LAST:event_clearPlotsMenuItemActionPerformed

    private void exploreNeighborhoodButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exploreNeighborhoodButtonActionPerformed
        launchExplorerAndAnchorAtContig();

    }//GEN-LAST:event_exploreNeighborhoodButtonActionPerformed

    private void sequenceSearchRadioButtonMenuItemItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_sequenceSearchRadioButtonMenuItemItemStateChanged
        if(evt.getStateChange() == ItemEvent.SELECTED){
            contigSearchTextField.setToolTipText(SEQUENCE_SEARCH_QUERY_FORMAT);

            String query = contigSearchTextField.getText().trim();

            if(query.equals(NAVIGATOR_SEARCH_QUERY_FORMAT) || query.equals("")) {
                contigSearchTextField.setText(SEQUENCE_SEARCH_QUERY_FORMAT);
            }

            searchButton.setToolTipText(FIND_CONTIG_SEQ_TOOLTIP);
            searchButton.setIcon(seqSearchIcon);
            backButton.setEnabled(false);
        }
    }//GEN-LAST:event_sequenceSearchRadioButtonMenuItemItemStateChanged

    private void navigatorSearchRadioButtonMenuItemItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_navigatorSearchRadioButtonMenuItemItemStateChanged
        if(evt.getStateChange() == ItemEvent.SELECTED){
            contigSearchTextField.setToolTipText(NAVIGATOR_SEARCH_QUERY_FORMAT);

            String query = contigSearchTextField.getText().trim();

            if(query.equals(SEQUENCE_SEARCH_QUERY_FORMAT) || query.equals("")) {
                contigSearchTextField.setText(NAVIGATOR_SEARCH_QUERY_FORMAT);
            }

            searchButton.setToolTipText(FIND_CONTIG_NAV_TOOLTIP);
            searchButton.setIcon(navSearchIcon);
            backButton.setEnabled(lastQuery != null);
        }
    }//GEN-LAST:event_navigatorSearchRadioButtonMenuItemItemStateChanged

    private void fragmentStatsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fragmentStatsTableMouseClicked
        if(evt.getClickCount() == 2){
            drawFragmentSizeDistInNewThread();
        }
    }//GEN-LAST:event_fragmentStatsTableMouseClicked

    private void statsSplitPaneMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_statsSplitPaneMouseClicked
        //statsHasFocus();

        if(evt.getClickCount() == 2){
            showPlotsToggleButton.setSelected(false);
            showNavigatorToggleButton.setSelected(false);
            displayPanel.repaint();
        }
    }//GEN-LAST:event_statsSplitPaneMouseClicked

    private void genomeComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_genomeComboBoxItemStateChanged
        Set<String> dbNames = blatInterface.selectOrg((String) genomeComboBox.getSelectedItem());
        DefaultComboBoxModel assemblyModel = new DefaultComboBoxModel();
        for(String db: dbNames){
            assemblyModel.addElement(db);
        }
        assemblyComboBox.setModel(assemblyModel);
    }//GEN-LAST:event_genomeComboBoxItemStateChanged

    private void showStatsToggleButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showStatsToggleButtonItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;

        if(selected){
            showStatsToggleButton.setBorder(LOWERED_BORDER);
            showStatsToggleButton.setToolTipText("Hide \"Statistics\" pane");
        }
        else{
            showStatsToggleButton.setBorder(RAISED_BORDER);
            showStatsToggleButton.setToolTipText("Show \"Statistics\" pane");
        }

        /* If the state of the button is same as the visibility of of the pane, don't do anything.
         * ie. If statsSplitPane is shown and the button is pushed, return.
         *     If statsSplitPane is not shown and the button is pushed, continue.
         */
        if(statsSplitPane.isVisible() == selected){
            return;
        }

        statsSplitPane.setVisible(selected);
        displayPanel.validate();
        validateSettings();

        if(selected){
            if(ready){
                displayPanel.validate();
                applySettingsInNewThread();
            }
            else if(scaffoldStatsTable.getRowCount() <= 0){
//                if(filesComboBox.getSelectedItem() != null) {
                    final Map<String, Color> selectedAssemblies = getSelectedAssemblies();

                    if(selectedAssemblies.size() <= 0){
                        String msg = "Please select one or more assemblies and try again.";
                        JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                        //System.out.println("no selected k-dirs");
                        showStatsToggleButton.setSelected(false);
                        return;
                    }

                    SwingWorker worker = new SwingWorker<Void, Void>() {
                        public Void doInBackground(){
                            long start = System.currentTimeMillis();
                            try{
                                statusLabel.setForeground(Color.BLACK);
                                showStatsToggleButton.setEnabled(false);
                                busyCursor();
                                stopButton.setEnabled(true);

                                Set<String> ks = selectedAssemblies.keySet();
                                boolean getStatsFromPlot = false;
                                PlotSettings settings = getPlotSettings();

                                if(showNavigatorToggleButton.isSelected()){
    //                                statsForNavigator = true;
                                    //ks.add(Utilities.getAssemblyNameFromPath(explorer.getFileOpened().getAbsolutePath()));
                                }
                                else{
    //                                statsForNavigator = false;
                                    if(showPlotsToggleButton.isSelected() && n50plot != null && lastPlotSettingsUsed != null && lastPlotSettingsUsed.equals(settings)){
                                        getStatsFromPlot = true;
                                    }
                                }

                                fillScaffoldStatsTable(ks, getStatsFromPlot);
                                fillFragmentSizeTable();

                                lastPlotSettingsUsed = settings;
                                packStatsSplitPane();
                                String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                                setStatus(doneText, true, NORMAL_STATUS);
                            }
                            catch(InterruptedException ex){
                                String doneText = "Stopped by user.";
                                setStatus(doneText, true, ERROR_STATUS);
                            }
                            catch (Exception ex){
                                ex.printStackTrace();
                                String doneText = "ERROR: " + ex.getMessage();
                                setStatus(doneText, true, ERROR_STATUS);
                            }
                            finally{
                                Processor.packColumns(fragmentStatsTable, 2);
                                stopButton.setEnabled(false);
                                defaultCursor();
                                showStatsToggleButton.setEnabled(true);
                                return null;
                            }
                        }
                    };
                    workers.add(worker);
                    worker.execute();
                    if(worker.isDone()){
                        workers.remove(worker);
                    }
//                }
            }
        }

        if(selected){
            packStatsSplitPane();
        }

        if(plotsSplitPane != null){
            if(showNavigatorToggleButton.isSelected()){
                if(plotsSplitPane.getOrientation() != JSplitPane.VERTICAL_SPLIT){
                    plotsSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                }
                if(plotsSplitPane.getTopComponent() != null && plotsSplitPane.getBottomComponent() != null){
                    plotsSplitPane.setDividerLocation(0.5);
                }
            }
            else{
                if(plotsSplitPane.getOrientation() != JSplitPane.HORIZONTAL_SPLIT){
                    plotsSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                }
                if(plotsSplitPane.getLeftComponent() != null && plotsSplitPane.getRightComponent() != null){
                    plotsSplitPane.setDividerLocation(0.5);
                }
            }
        }

//        clearStatsMenuItem.setEnabled(selected);

        if(selected){
            //lastPlotSettingsUsed = getPlotSettings();
            if(lastPlotSettingsUsed != null){
                applyButton.setEnabled(!lastPlotSettingsUsed.equals(getPlotSettings()));
            }            
            statsHasFocus();
        }
        else{
            if(!showPlotsToggleButton.isSelected() && !showNavigatorToggleButton.isSelected()){
                applyButton.setEnabled(false);
            }
            
            if(plotsPanel.isVisible()){
                plotsHasFocus();
            }
            else if(navigatorPanel != null && navigatorPanel.isVisible()){
                navigatorHasFocus();
            }
            else{
                clearFocus();
            }
        }

//        boolean explorerShown = showExplorerToggleButton.isSelected();
//        filesComboBox.setEnabled(!explorerShown);
//        unitOfLengthComboBox.setEnabled(!explorerShown);
    }//GEN-LAST:event_showStatsToggleButtonItemStateChanged

    private void showPlotsToggleButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showPlotsToggleButtonItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;

        if(selected){
            showPlotsToggleButton.setBorder(LOWERED_BORDER);
            showPlotsToggleButton.setToolTipText("Hide \"Plots\" pane");
        }
        else{
            showPlotsToggleButton.setBorder(RAISED_BORDER);
            showPlotsToggleButton.setToolTipText("Show \"Plots\" pane");
        }

        if(plotsPanel.isVisible() == selected){
            return;
        }

        plotsPanel.setVisible(selected);
        displayPanel.validate();
        validateSettings();
        if(selected){
            if(ready){
                applySettingsInNewThread();
            }
            else if(plotsTabbedPane.getTabCount() <= 0){
//                if(filesComboBox.getSelectedItem() != null) {
                    final Map<String, Color> selectedKsList = getSelectedAssemblies();

                    if(selectedKsList.size() <= 0){
                        String msg = "Please select one or more assemblies and try again.";
                        JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                        //System.out.println("no selected k-dirs");
                        showPlotsToggleButton.setSelected(false);
                        return;
                    }

                    if(drawFragSizeDistCheckBox.isSelected() && !librariesSelected()){
                        String msg = "Please select one or more libraries and try again.";
                        JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                        showPlotsToggleButton.setSelected(false);
                        return;
                    }

                    SwingWorker worker = new SwingWorker<Void, Void>() {
                        public Void doInBackground(){
                            long start = System.currentTimeMillis();
                            try{
                                statusLabel.setForeground(Color.BLACK);
                                showPlotsToggleButton.setEnabled(false);
                                busyCursor();
                                stopButton.setEnabled(true);
                                String doneText = null;
                                int status = NORMAL_STATUS;

                                if(drawN50plotCheckBox.isSelected() || drawCovPlotCheckBox.isSelected()){
                                    if(showNavigatorToggleButton.isSelected()){
                                        doneText = drawPlotsForNavigator();
                                        if(doneText == null){
                                            doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                                        }
                                        else{
                                            status = ERROR_STATUS;
                                        }
                                    }
                                    else{
                                        doneText = drawPlots(selectedKsList);
                                        plotsForNavigator = false;
                                        if(doneText == null){
                                            doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                                        }
                                        else{
                                            status = ERROR_STATUS;
                                        }
                                    }
                                }

                                if(drawFragSizeDistCheckBox.isSelected()){
                                    drawFragmentSizeDist(getSelectedLibraries());
                                }

                                setStatus(doneText, true, status);
                            }
                            catch (InterruptedException ex){
                                String doneText = "Stopped by user.";
                                setStatus(doneText, true, ERROR_STATUS);
                            }
                            catch (Exception ex) {
                                ex.printStackTrace();
                                String doneText = "ERROR: " + ex.getMessage();
                                setStatus(doneText, true, ERROR_STATUS);
                            }
                            finally{
                                stopButton.setEnabled(false);
                                defaultCursor();
                                showPlotsToggleButton.setEnabled(true);
                                return null;
                            }
                        }
                    };
                    workers.add(worker);
                    worker.execute();
                    if(worker.isDone()){
                        workers.remove(worker);
                    }

//                }
            }
        }

        if(plotsSplitPane != null){
            if(showNavigatorToggleButton.isSelected()){
                if(plotsSplitPane.getOrientation() != JSplitPane.VERTICAL_SPLIT){
                    plotsSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                }
                if(plotsSplitPane.getTopComponent() != null && plotsSplitPane.getBottomComponent() != null){
                    plotsSplitPane.setDividerLocation(0.5);
                }
            }
            else{
                if(plotsSplitPane.getOrientation() != JSplitPane.HORIZONTAL_SPLIT){
                    plotsSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                }
                if(plotsSplitPane.getLeftComponent() != null && plotsSplitPane.getRightComponent() != null){
                    plotsSplitPane.setDividerLocation(0.5);
                }
            }
        }

//        clearPlotsMenuItem.setEnabled(selected);

        if(selected){
            //lastPlotSettingsUsed = getPlotSettings();
            if(lastPlotSettingsUsed != null){
                applyButton.setEnabled(!lastPlotSettingsUsed.equals(getPlotSettings()));
            }
            
            plotsHasFocus();
        }
        else{
            if(!showStatsToggleButton.isSelected() && !showNavigatorToggleButton.isSelected()){
                applyButton.setEnabled(false);
            }
            
            if(statsSplitPane.isVisible()){
                statsHasFocus();
            }
            else if(navigatorPanel != null && navigatorPanel.isVisible()){
                navigatorHasFocus();
            }
            else{
                clearFocus();
            }
        }

        if(showStatsToggleButton.isSelected()){
             packStatsSplitPane();
        }
    }//GEN-LAST:event_showPlotsToggleButtonItemStateChanged

    private void showNavigatorToggleButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showNavigatorToggleButtonItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;

        if(selected){
            showNavigatorToggleButton.setBorder(LOWERED_BORDER);
            showNavigatorToggleButton.setToolTipText("Hide \"Navigator\" pane");
        }
        else{
            showNavigatorToggleButton.setBorder(RAISED_BORDER);
            showNavigatorToggleButton.setToolTipText("Show \"Navigator\" pane");
        }

        if(navigatorPanel != null){
            if(navigatorPanel.isVisible() == selected){
                switchAssembliesSelectionMode(selected);
                return;
            }


//            if(selected && explorer.isClear()){
//                try{
//                    explore();
//                    lastPlotSettingsUsed = getPlotSettings();
//                }
//                catch(Exception e){
//                    JOptionPane.showMessageDialog(this, e.getMessage(), "Warning", JOptionPane.WARNING_MESSAGE);
//                    showExplorerToggleButton.setSelected(false);
//                    return;
//                }
//            }

            navigatorPanel.setVisible(selected);
            displayPanel.validate();
            validateSettings();
            if(selected){
                if(ready){
                    applySettingsInNewThread();
                }
                else if(explorer.isClear()){

                    if(assembliesAndColorsTable.getSelectedRowCount() != 1) {
                        JOptionPane.showMessageDialog(this, "Please select one assembly and try again", "Warning", JOptionPane.WARNING_MESSAGE);
                        showNavigatorToggleButton.setSelected(false);
                        return;
                    }

                    SwingWorker worker = new SwingWorker<Void, Void>() {
                        public Void doInBackground(){
                            long start = System.currentTimeMillis();
                            try{
                                setStatus("Please wait...", true, NORMAL_STATUS);

                                showNavigatorToggleButton.setEnabled(false);
                                busyCursor();
                                stopButton.setEnabled(true);

                                int rowId = assembliesAndColorsTable.getSelectedRow();
                                String assembly = (String) assembliesAndColorsTable.getValueAt(rowId, 1);

                                if(sharedAncestorPath != null){
                                    assembly = sharedAncestorPath + File.separator + assembly;
                                }

                                explore(assembly);

                                if(navigatorSearchRadioButtonMenuItem.isSelected()){
                                    backButton.setEnabled(lastQuery != null);
                                }

                                navigatorHasFocus();

    //                            if(showStatsToggleButton.isSelected() && ){//!statsForNavigator){
    //                                Set<String> ks = new HashSet<String>();
    //                                ks.add(assembly);
    //                                fillScaffoldStatsTable(ks, false);
    //                                packStatsSplitPane();
    //                                statsForNavigator = true;
    //                            }

                                String doneText = null;
                                int status = NORMAL_STATUS;
                                if(showPlotsToggleButton.isSelected() && !plotsForNavigator){
                                    doneText = drawPlotsForNavigator();
                                    if(doneText != null){
                                        status = ERROR_STATUS;
                                    }
                                }

                                if(doneText == null){
                                    doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                                }

                                setStatus(doneText, true, status);
                            }
    //                        catch (InterruptedException ex){
    //                            String doneText = "Stopped by user.";
    //                            setStatus(doneText, true, ERROR_STATUS);
    //                            showNavigatorToggleButton.setSelected(false);
    //                        }
                            catch (IOException ex){
                                //ex.printStackTrace();
                                String msg = ex.getMessage();
                                JOptionPane.showMessageDialog(self(), Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
                                String doneText = "ERROR: " + msg;
                                setStatus(doneText, true, ERROR_STATUS);
                                showNavigatorToggleButton.setSelected(false);
                                explorer.clear();
                            }
                            catch (Exception ex){
                                ex.printStackTrace();
                                String msg = ex.getMessage();
                                JOptionPane.showMessageDialog(self(), Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
                                String doneText = "ERROR: " + msg;
                                setStatus(doneText, true, ERROR_STATUS);
                                showNavigatorToggleButton.setSelected(false);
                                explorer.clear();
                            }
                            finally{
                                stopButton.setEnabled(false);
                                defaultCursor();
                                showNavigatorToggleButton.setEnabled(true);
                                return null;
                            }
                        }
                    };
                    workers.add(worker);
                    worker.execute();
                    if(worker.isDone()){
                        workers.remove(worker);
                    }
                }
            }

            if(showPlotsToggleButton.isSelected()){
                if(plotsSplitPane != null){
                    if(showNavigatorToggleButton.isSelected()){
                        if(plotsSplitPane.getOrientation() != JSplitPane.VERTICAL_SPLIT){
                            plotsSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
                        }
                        if(plotsSplitPane.getTopComponent() != null && plotsSplitPane.getBottomComponent() != null){
                            plotsSplitPane.setDividerLocation(0.5);
                        }
                    }
                    else{
                        if(plotsSplitPane.getOrientation() != JSplitPane.HORIZONTAL_SPLIT){
                            plotsSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                        }
                        if(plotsSplitPane.getLeftComponent() != null && plotsSplitPane.getRightComponent() != null){
                            plotsSplitPane.setDividerLocation(0.5);
                        }
                    }
                }
            }

            switchAssembliesSelectionMode(selected);
            if(selected){
                if(lastPlotSettingsUsed != null){
                    File f = explorer.getFileOpened();
                    if(f != null){
                        String assembly = null;
                        if(f.isDirectory()){
                            assembly = f.getName();
                        }
                        else{
                            assembly = f.getParentFile().getName();
                        }
                        int indexUsed = -1;
                        for(int i=assembliesAndColorsTable.getRowCount()-1; i>=0; i--){
                            if(assembliesAndColorsTable.getValueAt(i, 1).equals(assembly)){
                                indexUsed = i;
                                break;
                            }
                        }
                        lastPlotSettingsUsed.selectedKs = new int[]{indexUsed};
                        //applyButton.setEnabled(!lastPlotSettingsUsed.equals(getPlotSettings()));
                        applyButton.setEnabled(false);
                    }
                }

                navigatorHasFocus();
//                explorer.enableSettings();
                //lastPlotSettingsUsed = getPlotSettings();
            }
            else{
//                explorerPanel.setVisible(false);
//                displayPanel.validate();

                if(!showStatsToggleButton.isSelected() && !showPlotsToggleButton.isSelected() ){
                    applyButton.setEnabled(false);
                }                

                if(plotsPanel.isVisible()){
                    plotsHasFocus();
                }
                else if(statsSplitPane.isVisible()){
                    statsHasFocus();
                }
                else{
                    clearFocus();
                }

                backButton.setEnabled(false);
//                explorer.disableSettings();
            }

            displayPanel.validate();

//            filesComboBox.setEnabled(!selected);
//            unitOfLengthComboBox.setEnabled(!selected);

            if(showStatsToggleButton.isSelected()){
//                SwingUtilities.invokeLater(new Runnable(){
//                    public void run(){
                        packStatsSplitPane();
//                    }
//                });
            }

//            clearExplorerMenuItem.setEnabled(explorerButtonSelected);
        }
    }//GEN-LAST:event_showNavigatorToggleButtonItemStateChanged


    private void switchAssembliesSelectionMode(boolean navigatorButtonSelected){
        if(navigatorButtonSelected){
            if(assembliesAndColorsTable.getSelectionModel().getSelectionMode() != ListSelectionModel.SINGLE_SELECTION){
                int index = -1;
                int rowCount = assembliesAndColorsTable.getSelectedRowCount();
                if(rowCount == 1){
                    index = assembliesAndColorsTable.getSelectedRow();
                }
                else if(rowCount >1){
                    index = assembliesAndColorsTable.getSelectedRows()[0];
                }

                assembliesAndColorsTable.getSelectionModel().removeListSelectionListener(selectionListener);
                assembliesAndColorsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

                if(index != -1){
                    assembliesAndColorsTable.getSelectionModel().setLeadSelectionIndex(index);
                }
                assembliesAndColorsTable.getSelectionModel().addListSelectionListener(selectionListener);
            }
        }
        else{
            if(assembliesAndColorsTable.getSelectionModel().getSelectionMode() != ListSelectionModel.MULTIPLE_INTERVAL_SELECTION){
                int selectedRow = assembliesAndColorsTable.getSelectedRow();
                assembliesAndColorsTable.getSelectionModel().removeListSelectionListener(selectionListener);
                assembliesAndColorsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

                if(selectedRow >= 0){
                    ListSelectionModel model = assembliesAndColorsTable.getSelectionModel();
                    model.addSelectionInterval(selectedRow, selectedRow);
                }
                assembliesAndColorsTable.getSelectionModel().addListSelectionListener(selectionListener);
            }
        }
    }

    private Dive self(){
        return this;
    }

    private void showHideButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showHideButtonActionPerformed
        if(assembliesAndSettingsSplitPane.isVisible()){
            assembliesAndSettingsSplitPane.setVisible(false);
            //showSettingsToggleButton.setSelected(false);
            showHideButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/right.png")));
            javax.swing.ImageIcon pressedIcon = new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/right_over.png"));
            showHideButton.setRolloverIcon(pressedIcon);
            showHideButton.setPressedIcon(pressedIcon);

        }
        else{
            assembliesAndSettingsSplitPane.setVisible(true);
            //showSettingsToggleButton.setSelected(true);
            showHideButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/left.png")));
            javax.swing.ImageIcon pressedIcon = new javax.swing.ImageIcon(getClass().getResource("/ca/bcgsc/dive/img/left_over.png"));
            showHideButton.setRolloverIcon(pressedIcon);
            showHideButton.setPressedIcon(pressedIcon);
        }
        mainPanel.validate();
    }//GEN-LAST:event_showHideButtonActionPerformed

    private long mousePressTime = 0L;

    private void searchButtonMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchButtonMousePressed
        if(evt.getButton() == MouseEvent.BUTTON1){
            mousePressTime = evt.getWhen();

            SwingWorker worker = new SwingWorker<Void, Void>() {
                public Void doInBackground() throws Exception {
                    Thread.sleep(400);
                    if(mousePressTime > 0L){
                        searchTypePopupMenu.show(searchButton,0,searchButton.getHeight());
                    }

                    return null;
                }
            };
            worker.execute();
        }
    }//GEN-LAST:event_searchButtonMousePressed

    private void searchButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchButtonMouseReleased
        if(evt.getButton() == MouseEvent.BUTTON1){
            if(evt.getWhen() - mousePressTime < 400){
                mousePressTime = 0L;
                performSearch();
            }
            else{
                mousePressTime = 0L;
            }
        }
    }//GEN-LAST:event_searchButtonMouseReleased

    private void searchButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_searchButtonMouseExited
        if(evt.getButton() == MouseEvent.BUTTON1){
            mousePressTime = 0L;
        }
    }//GEN-LAST:event_searchButtonMouseExited

    private void stopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_stopButtonActionPerformed
        for(SwingWorker worker : workers){
            if(!worker.isDone() && !worker.isCancelled()){
                worker.cancel(true);
            }
        }
        workers.clear();
        stopButton.setEnabled(false);
    }//GEN-LAST:event_stopButtonActionPerformed

    private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_applyButtonActionPerformed
        try {
            applySettingsInNewThread();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }//GEN-LAST:event_applyButtonActionPerformed

    private void yAxisUnitComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_yAxisUnitComboBoxItemStateChanged
        validateSettings();
    }//GEN-LAST:event_yAxisUnitComboBoxItemStateChanged

    private void xAxisScaleComboBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_xAxisScaleComboBoxItemStateChanged
        validateSettings();
    }//GEN-LAST:event_xAxisScaleComboBoxItemStateChanged

    private void clearNavigatorMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearNavigatorMenuItemActionPerformed
        if(navigatorPanel != null){
            String msg = "All contents displayed in Navigator would be cleared.\nWould you like to continue?";
            int response = JOptionPane.showConfirmDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Clear Navigator", JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.YES_OPTION){
                if(explorer != null){
                    explorer.clear();
                }
                navLabel.setText("NAVIGATOR");
                navigatorPanel.setVisible(false);
                showNavigatorToggleButton.setSelected(false);

                displayPanel.validate();
                if(plotsSplitPane != null){
                    if(plotsSplitPane.getOrientation() != JSplitPane.HORIZONTAL_SPLIT){
                        plotsSplitPane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
                    }
                    if(plotsSplitPane.getLeftComponent() != null && plotsSplitPane.getRightComponent() != null){
                        plotsSplitPane.setDividerLocation(0.5);
                    }

                    removePlotMarkers();
                }

                setStatus("All contents in Navigator cleared", false, NORMAL_STATUS);
            }
        }
    }//GEN-LAST:event_clearNavigatorMenuItemActionPerformed

    private void clearStatsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearStatsMenuItemActionPerformed
        int fstRowCount = fragmentStatsTable.getRowCount();
        int atRowCount = scaffoldStatsTable.getRowCount();

        if(fstRowCount > 0 || atRowCount > 0){
            String msg = "All statistics would be cleared.\nWould you like to continue?";
            int response = JOptionPane.showConfirmDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Clear Statistics", JOptionPane.YES_NO_OPTION);
            if(response == JOptionPane.YES_OPTION){
                showStatsToggleButton.setSelected(false);

                DefaultTableModel modelF = (DefaultTableModel) fragmentStatsTable.getModel();
                for(int i=fstRowCount-1; i>=0; i--){
                    modelF.removeRow(i);
                }

                disabledRowIndexesForSelectedFile.clear();
                disabledRowIndexesForCoverageFile.clear();

                DefaultTableModel modelA = (DefaultTableModel) scaffoldStatsTable.getModel();
                for(int i=atRowCount-1; i>=0; i--){
                    modelA.removeRow(i);
                }

                if(hiddenColumn != null){
                    TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
                    tcm.addColumn(hiddenColumn);
                    tcm.moveColumn(8, 4);
                    hiddenColumn = null;

                    Border border = assembliesPanel.getBorder();
                    if(border instanceof TitledBorder) {
                        TitledBorder tborder = (TitledBorder) border;
                        tborder.setTitle("Scaffold Sizes");
                        assembliesPanel.repaint();
                    }
                }

                setStatus("All statistics cleared", false, NORMAL_STATUS);
            }
        }
        else{
            if(statsSplitPane.isVisible()){
                statsSplitPane.setVisible(false);
            }
            if(showStatsToggleButton.isSelected()){
                showStatsToggleButton.setSelected(false);
            }
        }

        bestInSelection = null;
//        statsForNavigator = false;
    }//GEN-LAST:event_clearStatsMenuItemActionPerformed

    private void applyButtonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_applyButtonPropertyChange
        if(applyButton.isEnabled()){
            applyButton.setOpaque(true);
        }
        else{
            applyButton.setOpaque(false);
        }
    }//GEN-LAST:event_applyButtonPropertyChange

    private void minContigLengthFormattedTextFieldFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_minContigLengthFormattedTextFieldFocusGained
        SwingUtilities.invokeLater(new Runnable(){
            public void run(){
                minContigLengthFormattedTextField.selectAll();
            }
        });
}//GEN-LAST:event_minContigLengthFormattedTextFieldFocusGained

    private void plotsTabbedPaneComponentAdded(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_plotsTabbedPaneComponentAdded
        showTabsHeader = plotsTabbedPane.getTabCount() > 1;
        setTabComponentsVisible(showTabsHeader);
    }//GEN-LAST:event_plotsTabbedPaneComponentAdded

    private void plotsTabbedPaneComponentRemoved(java.awt.event.ContainerEvent evt) {//GEN-FIRST:event_plotsTabbedPaneComponentRemoved
        showTabsHeader = plotsTabbedPane.getTabCount() > 1;
        setTabComponentsVisible(showTabsHeader);
    }//GEN-LAST:event_plotsTabbedPaneComponentRemoved

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        System.out.println("ABySS-Explorer was terminated.");
    }//GEN-LAST:event_formWindowClosing

    private void lineWrapCheckBoxMenuItemItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_lineWrapCheckBoxMenuItemItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;
        contigSequenceTextArea.setLineWrap(selected);
    }//GEN-LAST:event_lineWrapCheckBoxMenuItemItemStateChanged

    private void backButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backButtonActionPerformed
        final File f = explorer.getFileOpened();
        if(f != null && lastQuery != null){
            contigSearchTextField.setForeground(Color.BLACK);
            contigSearchTextField.setText(lastQuery);
            performSearch();
        }
    }//GEN-LAST:event_backButtonActionPerformed

    private void displayPanelComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_displayPanelComponentResized
        displayPanel.validate();
        packStatsSplitPane();
    }//GEN-LAST:event_displayPanelComponentResized

    private void assembliesAndColorsTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_assembliesAndColorsTableMouseClicked
        if(!evt.isPopupTrigger()){
            if(assembliesAndColorsTable.rowAtPoint(evt.getPoint()) < 0){
                assembliesAndColorsTable.clearSelection();
            }
            else if(evt.getClickCount() == 2 && assembliesAndColorsTable.getSelectedRowCount() == 1){
                boolean showNavigator = navigatorPanel.isVisible();
                boolean showPlots = plotsPanel.isVisible();
                boolean showStats = statsSplitPane.isVisible();

                if(!showNavigator ||
                       !showPlots ||
                       !showStats ||
                       ready){

                    navigatorPanel.setVisible(true);
                    plotsPanel.setVisible(true);
                    statsSplitPane.setVisible(true);

                    if(!showNavigatorToggleButton.isSelected()){
                        showNavigatorToggleButton.setSelected(true);
                    }
                    if(!showPlotsToggleButton.isSelected()){
                        showPlotsToggleButton.setSelected(true);
                    }
                    if(!showStatsToggleButton.isSelected()){
                        showStatsToggleButton.setSelected(true);
                    }

                    applySettingsInNewThread();
                }
            }
        }
    }//GEN-LAST:event_assembliesAndColorsTableMouseClicked

    private void statsSplitPaneComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_statsSplitPaneComponentShown
        clearStatsMenuItem.setEnabled(true);
    }//GEN-LAST:event_statsSplitPaneComponentShown

    private void statsSplitPaneComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_statsSplitPaneComponentHidden
        clearStatsMenuItem.setEnabled(false);
    }//GEN-LAST:event_statsSplitPaneComponentHidden

    private void showLengthCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showLengthCheckBoxItemStateChanged
        lengthSlider.setEnabled(evt.getStateChange() == ItemEvent.SELECTED);
        validateSettings();
    }//GEN-LAST:event_showLengthCheckBoxItemStateChanged

    private void showLabelsCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showLabelsCheckBoxItemStateChanged
        validateSettings();
    }//GEN-LAST:event_showLabelsCheckBoxItemStateChanged

    private void lengthSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_lengthSliderStateChanged
        if(!lengthSlider.getValueIsAdjusting()){
            validateSettings();
        }
    }//GEN-LAST:event_lengthSliderStateChanged

    private void showNeighborsRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showNeighborsRadioButtonItemStateChanged
        if(evt.getStateChange() == ItemEvent.SELECTED){
            //stepSizeSlider.setEnabled(true);
            stepSizeLabel.setText("number of extensions:");
            validateSettings();
        }
    }//GEN-LAST:event_showNeighborsRadioButtonItemStateChanged

    private void showAllRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_showAllRadioButtonItemStateChanged
        if(evt.getStateChange() == ItemEvent.SELECTED){
            //stepSizeSlider.setEnabled(false);
            stepSizeLabel.setText("size of neighorhood:");
            validateSettings();
        }
    }//GEN-LAST:event_showAllRadioButtonItemStateChanged

    private void stepSizeSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_stepSizeSliderStateChanged
        if(!stepSizeSlider.getValueIsAdjusting()){
            validateSettings();
        }
    }//GEN-LAST:event_stepSizeSliderStateChanged

    private void contigSequenceFrameComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_contigSequenceFrameComponentShown
        /* Creating an object of BlatInterface would require loading the BLAT page once.
         * Here, we create an object of BlatInterface only when it is needed (only when
         * the first sequence search is performed, instead of every time the program is launched).
         */
        if(blatInterface == null){
            blatInterface = new BlatInterface(); // requires internet connection
            Set<String> orgs = blatInterface.listAllOrgs();
            Vector<String> orgsList = new Vector<String>(orgs.size());
            orgsList.addAll(orgs);
            Collections.sort(orgsList);
            DefaultComboBoxModel genomeModel = new DefaultComboBoxModel(orgsList);
            genomeComboBox.setModel(genomeModel);
            genomeComboBox.setSelectedItem("Human");
        }
    }//GEN-LAST:event_contigSequenceFrameComponentShown

    private void resetMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetMenuItemActionPerformed
        String msg = "All contents displayed would be removed. Would you like to continue?";
        int r = JOptionPane.showConfirmDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Confirm removing all assemblies", JOptionPane.YES_NO_OPTION);
        if(r == JOptionPane.YES_OPTION){
            clearAll();
        }
    }//GEN-LAST:event_resetMenuItemActionPerformed

    private void loadAdjDotMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadAdjDotMenuItemActionPerformed
        loadAdjDotFile();
    }//GEN-LAST:event_loadAdjDotMenuItemActionPerformed

    private void addAssembliesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addAssembliesButtonActionPerformed
        addAssembliesMenuItemActionPerformed(evt);
    }//GEN-LAST:event_addAssembliesButtonActionPerformed

    private void loadAdjDotButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadAdjDotButtonActionPerformed
        loadAdjDotMenuItemActionPerformed(evt);
    }//GEN-LAST:event_loadAdjDotButtonActionPerformed

    private void removeAssembliesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAssembliesMenuItemActionPerformed
        int[] selectedRowsInView = assembliesAndColorsTable.getSelectedRows();
        int numSelected = selectedRowsInView.length;
        String[] selectedAssemblies = new String[numSelected];
        ArrayList<Integer> selectedRowsInModel = new ArrayList<Integer>(numSelected);

        for(int i=0; i<numSelected; i++){
            int viewIndex = selectedRowsInView[i];
            selectedRowsInModel.add(assembliesAndColorsTable.convertRowIndexToModel(viewIndex));
            selectedAssemblies[i] = (String)assembliesAndColorsTable.getValueAt(viewIndex, 1);
        }

        boolean useWorkingDir = sharedAncestorPath != null;

        for(String assembly : selectedAssemblies){
            String path = null;
            if(useWorkingDir){
                path = sharedAncestorPath + File.separator + assembly;
            }
            else{
                path = assembly;
            }

            //remove the assembly in k-map
            kValues.remove(path);
            
            //remove the assembly in loaded map
            loadedAssembliesMap.remove(Utilities.getCanonicalPath(path));

            //remove the assemby in color-map
            chartColors.remove(path);

//            //reomve the assembly from the plots
//            if(n50plot != null){
//                int numSeriesLeft = n50plot.removeSeries(path);
//                if(numSeriesLeft == 0){
//                    //clear plots
//                }
//            }
//            if(covplot != null){
//                int numSeriesLeft = covplot.removeSeries(path);
//                if(numSeriesLeft == 0){
//                    //clear plots
//                }
//            }
//
//            //fragmentsizes (stats/plots)
//            if(bestInSelection.equals(path)){
//                //TODO: remove the statistics
//                //TODO: get the statistics from another assembly!
//                //TODO: remove all FSD plots
//            }
//
//            //statistics
//            DefaultTableModel model = (DefaultTableModel)assembliesTable.getModel();
//            int numRows = model.getRowCount();
//            if(numRows > 0){
//                for(int i=numRows-1; i>=0; i--){
//                   String name = (String) model.getValueAt(0, i) ;
//                   if(workingDir != null){
//                       name = workingDir + File.separator + name;
//                   }
//                   if(name.equals(path)){
//                       model.removeRow(i);
//                   }
//                }
//
//                //TODO: update the indexes for the cell renderers
//            }
//
//            //navigator
//            File navFile = explorer.getFileOpened();
//            if(path.equals(navFile.getPath())){
//                //TODO: clear explorer
//            }
        }

        Collections.sort(selectedRowsInModel);

        if(lastPlotSettingsUsed != null){
            lastPlotSettingsUsed.selectedKs = new int[0];
        }

        DefaultTableModel model = (DefaultTableModel) assembliesAndColorsTable.getModel();
        for(int i=numSelected-1; i>=0; i--){
            model.removeRow(selectedRowsInModel.get(i));
        }

        if(model.getRowCount() > 0){
            fillAssembliesAndColorsTable(null);
        }

        removeUselessLibraries();

        String msg = null;
        if(numSelected == 1){
            msg = numSelected + " assembly was removed.";
        }
        else{
            msg = numSelected + " assemblies were removed.";
        }

        this.setStatus(msg, true, NORMAL_STATUS);
    }//GEN-LAST:event_removeAssembliesMenuItemActionPerformed

    private void removeAssembliesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeAssembliesButtonActionPerformed
        this.removeAssembliesMenuItemActionPerformed(evt);
    }//GEN-LAST:event_removeAssembliesButtonActionPerformed

    private void showStatsToggleButtonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_showStatsToggleButtonPropertyChange
        if(showStatsToggleButton.isEnabled()){
            showStatsToggleButton.setOpaque(true);
        }
        else{
            showStatsToggleButton.setOpaque(false);
        }
    }//GEN-LAST:event_showStatsToggleButtonPropertyChange

    private void showPlotsToggleButtonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_showPlotsToggleButtonPropertyChange
        if(showPlotsToggleButton.isEnabled()){
            showPlotsToggleButton.setOpaque(true);
        }
        else{
            showPlotsToggleButton.setOpaque(false);
        }
    }//GEN-LAST:event_showPlotsToggleButtonPropertyChange

    private void showNavigatorToggleButtonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_showNavigatorToggleButtonPropertyChange
        if(showNavigatorToggleButton.isEnabled()){
            showNavigatorToggleButton.setOpaque(true);
        }
        else{
            showNavigatorToggleButton.setOpaque(false);
        }
    }//GEN-LAST:event_showNavigatorToggleButtonPropertyChange

    private void copyScaffoldStatsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyScaffoldStatsMenuItemActionPerformed
        java.awt.datatransfer.StringSelection stats = new java.awt.datatransfer.StringSelection(Processor.getAllValuesFromTable(scaffoldStatsTable));
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stats, stats);
    }//GEN-LAST:event_copyScaffoldStatsMenuItemActionPerformed

    private void copyFragmentStatsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_copyFragmentStatsMenuItemActionPerformed
        java.awt.datatransfer.StringSelection stats = new java.awt.datatransfer.StringSelection(Processor.getAllValuesFromTable(fragmentStatsTable));
        java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stats, stats);
    }//GEN-LAST:event_copyFragmentStatsMenuItemActionPerformed

    private void plotsPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotsPanelMouseClicked
        plotsHasFocus();

        if(evt.getClickCount() == 2){
            showStatsToggleButton.setSelected(false);
            showNavigatorToggleButton.setSelected(false);
            displayPanel.repaint();
        }
    }//GEN-LAST:event_plotsPanelMouseClicked

    private void plotsPanelComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_plotsPanelComponentHidden
        clearPlotsMenuItem.setEnabled(false);
    }//GEN-LAST:event_plotsPanelComponentHidden

    private void plotsPanelComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_plotsPanelComponentShown
        clearPlotsMenuItem.setEnabled(true);
    }//GEN-LAST:event_plotsPanelComponentShown

    private void selectLibrariesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_selectLibrariesButtonActionPerformed
        libsPopupMenu.show(selectLibrariesButton.getParent(), selectLibrariesButton.getX()+selectLibrariesButton.getWidth(), selectLibrariesButton.getY()+selectLibrariesButton.getHeight()-libsPopupMenu.getPreferredSize().height);
    }//GEN-LAST:event_selectLibrariesButtonActionPerformed

    private void drawFragSizeDistCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_drawFragSizeDistCheckBoxItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;
        selectLibrariesButton.setEnabled(selected);
        validateSettings();
        validateViewButtonsStatus();
    }//GEN-LAST:event_drawFragSizeDistCheckBoxItemStateChanged

    private void drawN50plotCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_drawN50plotCheckBoxItemStateChanged
        boolean selected = evt.getStateChange() == ItemEvent.SELECTED;
        yAxisUnitComboBox.setEnabled(selected);
        xAxisScaleComboBox.setEnabled(selected);
        validateSettings();
        validateViewButtonsStatus();
    }//GEN-LAST:event_drawN50plotCheckBoxItemStateChanged

    private void selectLibrariesButtonPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_selectLibrariesButtonPropertyChange
       boolean enabled = selectLibrariesButton.isEnabled();
       if(!enabled){
           selectLibrariesButton.setOpaque(false);
           selectLibrariesButton.repaint();
       }
       else{
           selectLibrariesButton.setOpaque(!librariesSelected());
           selectLibrariesButton.repaint();
       }
    }//GEN-LAST:event_selectLibrariesButtonPropertyChange

    private void plotsTabbedPaneMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotsTabbedPaneMousePressed
        plotsHasFocus();
    }//GEN-LAST:event_plotsTabbedPaneMousePressed

    private void removeSelectedMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeSelectedMenuItemActionPerformed
        this.removeAssembliesMenuItemActionPerformed(evt);
    }//GEN-LAST:event_removeSelectedMenuItemActionPerformed

    private void scaffoldRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_scaffoldRadioButtonItemStateChanged
        this.validateSettings();
    }//GEN-LAST:event_scaffoldRadioButtonItemStateChanged

    private void seRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_seRadioButtonItemStateChanged
        this.validateSettings();
    }//GEN-LAST:event_seRadioButtonItemStateChanged

    private void peRadioButtonItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_peRadioButtonItemStateChanged
        this.validateSettings();
    }//GEN-LAST:event_peRadioButtonItemStateChanged

    private void drawCovPlotCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_drawCovPlotCheckBoxItemStateChanged
        validateSettings();
        validateViewButtonsStatus();
    }//GEN-LAST:event_drawCovPlotCheckBoxItemStateChanged

    private void applySettings() throws InterruptedException, Exception{
        Exception e = null;
        boolean statsShown = statsSplitPane.isVisible();
        boolean plotsShown = plotsPanel.isVisible();
        boolean navigatorShown = navigatorPanel.isVisible();
        PlotSettings currentSettings = getPlotSettings();

        if(singleFileLoaded){
            exploreSingleFile(explorer.getFileOpened(), explorer.getK());
        }
        else /*if (!singleFileLoaded && filesComboBox.getSelectedItem() != null)*/ {
            Map<String, Color> map = getSelectedAssemblies();

            if(map.size() <= 0){
                //TODO: warn the user

                String msg = "No assemblies were selected.";
                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                //System.out.println("no selected k-dirs");
                return;
            }

            boolean sameAssembliesSelected = false;
            if(lastPlotSettingsUsed != null){
                sameAssembliesSelected = currentSettings.haveSameAssembliesSelected(lastPlotSettingsUsed);
            }

            boolean clearAllStats = lastPlotSettingsUsed == null
                    || !sameAssembliesSelected
                    || !currentSettings.haveSameStatsSettings(lastPlotSettingsUsed);
            boolean clearAllPlots = lastPlotSettingsUsed == null
                    || !sameAssembliesSelected
                    || !currentSettings.haveSamePlotsSettings(lastPlotSettingsUsed)
                    || !currentSettings.drawFragmentSizeDistribution && !currentSettings.drawN50plot && !currentSettings.drawCoveragePlot
                    || currentSettings.assemblyMode != lastPlotSettingsUsed.assemblyMode;
//            boolean newN50plot = !currentSettings.haveSameN50plotSettings(lastPlotSettingsUsed);
//            boolean newCovPlot = !currentSettings.haveSameCoveragePlotSettings(lastPlotSettingsUsed);
//            boolean newFSDplot = !currentSettings.haveSameFragmentSizeDistributionSettings(lastPlotSettingsUsed);

            if(!sameAssembliesSelected){
                bestInSelection = null;
            }

            if(clearAllStats){
                maxN50Index = -1;
                maxContiguityIndex = -1;
                maxReconstructionIndex = -1;
                maxSpanIndex = -1;

                disabledRowIndexesForSelectedFile.clear();
                disabledRowIndexesForCoverageFile.clear();

                DefaultTableModel fsTableModel = (DefaultTableModel) fragmentStatsTable.getModel();
                for(int i=fsTableModel.getRowCount()-1; i>=0; i--){
                    fsTableModel.removeRow(i);
                }

                DefaultTableModel aTableModel = (DefaultTableModel) scaffoldStatsTable.getModel();
                for(int i=aTableModel.getRowCount()-1; i>=0; i--){
                    aTableModel.removeRow(i);
                }
            }
            else{
                if(!currentSettings.haveSameStatsSettings(lastPlotSettingsUsed)){
                    DefaultTableModel aTableModel = (DefaultTableModel) scaffoldStatsTable.getModel();
                    for(int i=aTableModel.getRowCount()-1; i>=0; i--){
                        aTableModel.removeRow(i);
                    }
                }
            }

            if(clearAllPlots){
                plotsTabbedPane.removeAll();
                plotsSplitPane = null;
                plotsForNavigator = false;
                n50plot = null;
                covplot = null;
                if(!currentSettings.drawFragmentSizeDistribution && !currentSettings.drawN50plot && !currentSettings.drawCoveragePlot){
                    plotsShown = false;
                    plotsPanel.setVisible(false);
                    showPlotsToggleButton.setSelected(false);
                }
            }
            else{
                if(!currentSettings.drawN50plot && !currentSettings.drawCoveragePlot){
                    if(plotsSplitPane != null){
                        plotsTabbedPane.remove(plotsSplitPane);
                        plotsSplitPane = null;
                    }
                    plotsForNavigator = false;
                    n50plot = null;
                    covplot = null;
                }
                else{
                    if(!sameAssembliesSelected
                            || currentSettings.minContigLength != lastPlotSettingsUsed.minContigLength
                            || currentSettings.xAxisInUnitOfLength != lastPlotSettingsUsed.xAxisInUnitOfLength
                            || !currentSettings.fileName.equals(lastPlotSettingsUsed.fileName)
                            || currentSettings.unit != lastPlotSettingsUsed.unit){
                        if(plotsSplitPane != null){
                            plotsSplitPane.setLeftComponent(null);
                        }
                        plotsForNavigator = false;
                        n50plot = null;
                    }
                    else{
                        if(currentSettings.logY != lastPlotSettingsUsed.logY){
                            if(n50plot != null){
                                n50plot.useLogYAxis(currentSettings.logY);
                            }
                        }
                    }

                    if(!sameAssembliesSelected){
                        if(plotsSplitPane != null){
                            plotsSplitPane.setRightComponent(null);
                        }
                        covplot = null;
                    }
                }

                if(!currentSettings.drawFragmentSizeDistribution){
                    // remove all FSD
                    int tabCount = plotsTabbedPane.getTabCount();
                    for(int i=tabCount-1; i>=0; i--){
                        Component c = plotsTabbedPane.getComponentAt(i);
                        if(c != null && c != plotsSplitPane){
                            plotsTabbedPane.remove(i);
                        }
                    }
                }
                else{
                    // remove individual FSD
                    for(JCheckBox cb : libraryCheckBoxes){
                        if(plotsTabbedPane.getTabCount() == 0){
                            break;
                        }

                        if(!cb.isSelected()){
                            int index = plotsTabbedPane.indexOfTab(cb.getText());
                            if(index >= 0){
                                plotsTabbedPane.remove(index);
                            }
                        }
                    }
                }
            }

            if(navigatorShown){
                int rowId = assembliesAndColorsTable.getSelectedRow();
                String assembly = (String) assembliesAndColorsTable.getValueAt(rowId, 1);
                if(sharedAncestorPath != null){
                    assembly = sharedAncestorPath + File.separator + assembly;
                }

                try{
                    String fileName = null;
                    if(explorer != null
                            && lastPlotSettingsUsed != null
                            && Arrays.equals(lastPlotSettingsUsed.selectedKs, currentSettings.selectedKs) // no change in assembly selected
                            && lastPlotSettingsUsed.assemblyMode == currentSettings.assemblyMode){ // no change from PE to SE or vice versa
                        File fileOpened = explorer.getFileOpened();
                        if(fileOpened != null){
                            File selectedAssembly = new File(assembly);
                            if(selectedAssembly.exists() && selectedAssembly.equals(fileOpened.getParentFile())){
                                fileName = fileOpened.getName();
                            }
                        }
                    }

                    explore(assembly, fileName);

                    if(plotsShown && (currentSettings.drawN50plot || currentSettings.drawCoveragePlot)){
                        String errMsg = drawPlotsForNavigator();
                        setStatus(errMsg, true, ERROR_STATUS);
                    }
                }
                catch(IOException ex){
//                    ex.printStackTrace();
                    navigatorShown = false;
                    plotsForNavigator = false;
                    navigatorPanel.setVisible(false);
                    explorer.clear();
                    showNavigatorToggleButton.setSelected(false);
//                    setStatus(ex.getMessage(), true, ERROR_STATUS);
                    e = ex;
                }
                catch(Exception ex){
                    e = ex;
//                    ex.printStackTrace();
                }
            }

            if(plotsShown && !navigatorShown && (currentSettings.drawN50plot || currentSettings.drawCoveragePlot)){
                plotsForNavigator = false;
                String errMsg = drawPlots(map);
                if(errMsg != null){
                    setStatus(errMsg, true, ERROR_STATUS);
                }
            }

            if(statsShown){
//                statsForNavigator = navigatorShown;
                if(scaffoldStatsTable.getRowCount() == 0){
                    fillScaffoldStatsTable(map.keySet(), plotsShown && !navigatorShown);
                }

                if(fragmentStatsTable.getRowCount() == 0){
                    fillFragmentSizeTable();
                }
                packStatsSplitPane();
            }

            if(plotsShown && currentSettings.drawFragmentSizeDistribution){
                String[] libs = getSelectedLibraries();
                if(drawFragSizeDistCheckBox.isSelected() && libs.length <= 0){
                    String msg = "No libraries were selected.";
                    setStatus(msg, true, ERROR_STATUS);
                }
                drawFragmentSizeDist(libs);
            }

            validateViewButtonsStatus();
            //JOptionPane.showMessageDialog(self(), Utilities.formatDialogMessage(ex.getMessage(), Dive.MAX_CHARS_PER_LINE), "Error", JOptionPane.ERROR_MESSAGE);
        }

        if(navigatorShown || plotsShown || statsShown){
            lastPlotSettingsUsed = currentSettings;
        }

        ready = false;
        applyButton.setEnabled(false);

        if(e != null){
            throw e;
        }
    }

    private void validateSettings(){
        PlotSettings ps = getPlotSettings();
            boolean statsShown = statsSplitPane.isVisible();
            boolean plotsShown = plotsPanel.isVisible();
            boolean explorerShown = navigatorPanel.isVisible();

        if((!singleFileLoaded && ps.selectedKs.length <= 0) ||
                    (!statsShown && !plotsShown && !explorerShown)){
            ready = false;
            applyButton.setEnabled(false);
            return;
        }

        if(lastPlotSettingsUsed == null){
            ready = true;
            applyButton.setEnabled(true);
            return;
        }

        ready = false;

        boolean selectionEqual = Arrays.equals(lastPlotSettingsUsed.selectedKs, ps.selectedKs);

        boolean mclEqual = lastPlotSettingsUsed.minContigLength == ps.minContigLength;
        boolean unitEqual = lastPlotSettingsUsed.unit == ps.unit;
        boolean fileNameEqual = true;
        if(lastPlotSettingsUsed.fileName != null && ps.fileName != null){
            fileNameEqual = lastPlotSettingsUsed.fileName.equals(ps.fileName);
        }

//        boolean scaleEqual = lastPlotSettingsUsed.logY == ps.logY;
//        boolean unitOfLengthEqual = lastPlotSettingsUsed.xAxisInUnitOfLength == ps.xAxisInUnitOfLength;

                        boolean isPairedEndEqual = lastPlotSettingsUsed.assemblyMode == ps.assemblyMode;
                        boolean showSquigglesEqual = lastPlotSettingsUsed.showSquiggles == ps.showSquiggles;
                        boolean lengthScaleEqual = lastPlotSettingsUsed.lengthScale == ps.lengthScale;
                        boolean showLabelsEqual = lastPlotSettingsUsed.showLabels == ps.showLabels;
                        boolean showPEContigsEqual = lastPlotSettingsUsed.showPEContigs == ps.showPEContigs;
//                        boolean showPEPartnersEqual = lastPlotSettingsUsed.showPEPartners == ps.showPEPartners;
                        boolean useStepSizeEqual = lastPlotSettingsUsed.useStepSize == ps.useStepSize;
                        boolean stepSizeEqual = lastPlotSettingsUsed.stepSize == ps.stepSize;


        boolean explorerReady = !selectionEqual || !isPairedEndEqual || !showSquigglesEqual || !lengthScaleEqual || !showLabelsEqual || !showPEContigsEqual || !useStepSizeEqual || !stepSizeEqual;
        if(explorerShown){
            ready = explorerReady || ready;
        }

        if(!singleFileLoaded){
            boolean statsReady = !selectionEqual || !mclEqual || !unitEqual || !fileNameEqual;
            boolean plotsReady = statsReady || !lastPlotSettingsUsed.haveSamePlotsSettings(ps);
            if(ps.drawFragmentSizeDistribution && ps.selectedLibs.length == 0){
                plotsReady = false;
            }

            if(plotsShown){
                ready = plotsReady || ready;
            }

            if(statsShown){
                ready = statsReady || ready;
            }
        }

        applyButton.setEnabled(ready);

    }

    private void validateViewButtonsStatus(){
//        if(assembliesAndColorsTable.getSelectedRowCount()<=0){
//                showPlotsToggleButton.setEnabled(false);
//                showStatsToggleButton.setEnabled(false);
//                showNavigatorToggleButton.setEnabled(false);
//        }
//        else
        if(!drawFragSizeDistCheckBox.isSelected() && !drawN50plotCheckBox.isSelected() && !drawCovPlotCheckBox.isSelected()){
            if(!showPlotsToggleButton.isSelected()){
                showPlotsToggleButton.setEnabled(false);
            }
        }
        else if(drawFragSizeDistCheckBox.isSelected() && !librariesSelected()){
                showPlotsToggleButton.setEnabled(false);
                showStatsToggleButton.setEnabled(false);
                showNavigatorToggleButton.setEnabled(false);
        }
        else{
                showPlotsToggleButton.setEnabled(true);
                showStatsToggleButton.setEnabled(true);
                showNavigatorToggleButton.setEnabled(true);
        }
    }

//    private void settingsApplyButtonAction(boolean applyToPlots){
//        if(workingDir != null && filesComboBox.getSelectedItem() != null) {
//            HashMap<String, Color> selectedKsList = getSelectedKs();
//
//            if(selectedKsList.size() <= 0){
//                //TODO: warn the user
//
//                String msg = "No assemblies were selected. Please select assemblies by checking off the checkboxes.";
//                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_CHARS_PER_LINE), "Warning", JOptionPane.WARNING_MESSAGE);
//                //System.out.println("no selected k-dirs");
//                return;
//            }
//
//            showExplorerToggleButton.setSelected(false);
//            showStatsToggleButton.setSelected(false);
//            showPlotsToggleButton.setSelected(false);
//
//            try{
//                if(applyToPlots){
//                    plotsTabbedPane.removeAll();
//                    drawPlots(selectedKsList);
//                }
//            } catch(java.lang.InterruptedException e){
//                return;
//            } catch(Exception e){
//                return;
//            }
//
//            fillAssembliesTable3(selectedKsList.keySet(), applyToPlots);
//
//            fillFragmentSizeTable();
//
//            showStatsToggleButton.setSelected(true);
//            showPlotsToggleButton.setSelected(applyToPlots);
//        }
//    }

    private String drawPlots(Map<String, Color> selectedKsList) throws InterruptedException, Exception{
//                plotsHasFocus();

//                plotsTabbedPane.removeAll();

                JSplitPane splitPane = plotsSplitPane;

                if(splitPane == null){
                    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, null, null);
                    splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
                    splitPane.setOneTouchExpandable(true);
                    addComponentsMouseListener(splitPane, plotsComponentsListener);
                }

                boolean errDrawingN50plot = false;
                boolean errDrawingCovPlot = false;

                if(drawN50plotCheckBox.isSelected() && splitPane.getLeftComponent() == null && filesComboBox.getSelectedItem() != null){
                    String read1 = "Reading files for N50plot...";
                    setStatus(read1, true, NORMAL_STATUS);
                    JPanel n50plotPanel = drawN50plot(selectedKsList);

                    if(n50plotPanel != null){
                        splitPane.setLeftComponent(n50plotPanel);
                        addComponentsMouseListener(n50plotPanel, plotsComponentsListener);
                    }
                    else{
                        n50plot = null;
                        errDrawingN50plot = true;
                    }
                }

                if(drawCovPlotCheckBox.isSelected() && splitPane.getRightComponent() == null){
                    String read2 = "Reading files for k-mer coverage plot...";
                    setStatus(read2, true, NORMAL_STATUS);
                    JPanel covPlotPanel = drawKmerCoveragePlot(selectedKsList);

                    if(covPlotPanel != null){
                        splitPane.setRightComponent(covPlotPanel);
                        addComponentsMouseListener(covPlotPanel, plotsComponentsListener);
                    }
                    else{
                        covplot = null;
                        errDrawingCovPlot = true;
                    }
                }

                String errMsg = null;
                int indexOfTab = plotsTabbedPane.indexOfComponent(splitPane);

//                if(errDrawingN50plot && errDrawingCovPlot){
//                    errMsg = "ERROR: Cannot draw N50 plot and k-mer coverage plot.";
//                }
//                else if(errDrawingN50plot && !errDrawingCovPlot){
//                    if(indexOfTab < 0){
//                        plotsTabbedPane.add("Cov. Plot", splitPane);
//                    }
//                    else{
//                        plotsTabbedPane.setTitleAt(indexOfTab, "Cov. Plot");
//                    }
//                    splitPane.setResizeWeight(0.0);
//                    splitPane.setDividerLocation(0.0);
//                    plotsSplitPane = splitPane;
//
//                    errMsg = "ERROR: Cannot draw N50plot.";
//                }
//                else if(!errDrawingN50plot && errDrawingCovPlot){
//                    if(indexOfTab < 0){
//                        plotsTabbedPane.add("N50-Plot", splitPane);
//                    }
//                    else{
//                        plotsTabbedPane.setTitleAt(indexOfTab, "N50-Plot");
//                    }
//                    splitPane.setResizeWeight(1.0);
//                    splitPane.setDividerLocation(1.0);
//                    plotsSplitPane = splitPane;
//
//                    errMsg = "ERROR: Cannot draw k-mer coverage plot.";
//                }
//                else{
//                    if(indexOfTab < 0){
//                        plotsTabbedPane.add("N50-Plot & Cov. Plot", splitPane);
//                    }
//                    else{
//                        plotsTabbedPane.setTitleAt(indexOfTab, "N50-Plot & Cov. Plot");
//                    }
//                    splitPane.setResizeWeight(0.5);
//                    splitPane.setDividerLocation(0.5);
//                    plotsSplitPane = splitPane;
//                }
                if(errDrawingN50plot && errDrawingCovPlot){
                    errMsg = "ERROR: Cannot draw N50-plot and k-mer coverage plot.";
                }
                else if(errDrawingN50plot && !errDrawingCovPlot){
                    errMsg = "ERROR: Cannot draw N50-plot.";
                }
                else if(!errDrawingN50plot && errDrawingCovPlot){
                    errMsg = "ERROR: Cannot draw k-mer coverage plot.";
                }
                
                if (n50plot == null && covplot != null){
                    if(indexOfTab < 0){
                        plotsTabbedPane.add("Cov. Plot", splitPane);
                    }
                    else{
                        plotsTabbedPane.setTitleAt(indexOfTab, "Cov. Plot");
                    }
                    splitPane.setResizeWeight(0.0);
                    splitPane.setDividerLocation(0.0);
                    plotsSplitPane = splitPane;
                }
                else if (n50plot != null && covplot == null){
                    if(indexOfTab < 0){
                        plotsTabbedPane.add("N50-Plot", splitPane);
                    }
                    else{
                        plotsTabbedPane.setTitleAt(indexOfTab, "N50-Plot");
                    }
                    splitPane.setResizeWeight(1.0);
                    splitPane.setDividerLocation(1.0);
                    plotsSplitPane = splitPane;
                }
                else if (n50plot != null && covplot != null){
                    if(indexOfTab < 0){
                        plotsTabbedPane.add("N50-Plot & Cov. Plot", splitPane);
                    }
                    else{
                        plotsTabbedPane.setTitleAt(indexOfTab, "N50-Plot & Cov. Plot");
                    }
                    splitPane.setResizeWeight(0.5);
                    splitPane.setDividerLocation(0.5);
                    plotsSplitPane = splitPane;
                }                

                return errMsg;
    }

    private void applySettingsInNewThread() {
        SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                Long start = System.currentTimeMillis();
                try{
                    statusLabel.setForeground(Color.BLACK);
                    stopButton.setEnabled(true);
                    busyCursor();
                    enableLocateDirComponents(false);
                    enableSettingsComponents(false);
                    applyButton.setEnabled(false);
                    applySettings();
                    String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                    setStatus(doneText, true, NORMAL_STATUS);
                }
                catch (InterruptedException ex){
                    String doneText = "Stopped by user.";
                    setStatus(doneText, true, ERROR_STATUS);
                }
                catch (IOException ex){
                    //ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                catch (Exception ex){
                    ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                finally{
                    Processor.packColumns(fragmentStatsTable, 2);
                    stopButton.setEnabled(false);
                    enableLocateDirComponents(true);
                    if(singleFileLoaded){
                        enableNavigatorSettingsComponents(true);
                    }
                    else{
                        enableSettingsComponents(true);
                    }
                    defaultCursor();
                }
                return null;
            }
        };
        workers.add(worker);
        worker.execute();
        if(worker.isDone()){
            workers.remove(worker);
        }
    }

    public String drawPlotsForNavigator(){

//        plotsHasFocus();

            File fileOpened = explorer.getFileOpened();
            if(fileOpened == null || !fileOpened.exists()){
                return null;
            }

            PlotSettings plotSettings = getPlotSettings();
//            if(lastPlotSettingsUsed != null && lastPlotSettingsUsed.equals(plotSettings) && plotsForNavigator){
//                return;
//            }

            //filesComboBox.setEnabled(false);
            String fileName = plotSettings.fileName;

            String[] suffixes = null;
            if(peRadioButton.isSelected()){
                suffixes = new String[]{"-contigs.fa", "-" + CONTIGS + ".fa"};
            }
            else if(scaffoldRadioButton.isSelected()){
                suffixes = new String[]{"-scaffolds.fa", "-" + SCAFFOLDS + ".fa"};                
            }
            else{
                suffixes = new String[]{"-8.fa","-6.fa","-3.fa","-1.fa"};
            }

            String absPath = fileOpened.getAbsolutePath();
            String fileNameForN50plot = absPath + File.separator + fileName;
            File fileForN50plot = new File(fileNameForN50plot);
            for(String suffix: suffixes){
                DefaultComboBoxModel model = (DefaultComboBoxModel) filesComboBox.getModel();
                for(int i=0; i< model.getSize(); i++){
                    String name = (String) model.getElementAt(i);
                    if(name.endsWith(suffix)){
                        String potentialPathForN50plot = absPath + File.separator + name;
                        fileForN50plot = new File(potentialPathForN50plot);
                        if(fileForN50plot.exists()){
                            fileNameForN50plot = potentialPathForN50plot;
                            break;
                        }
                    }
                }
            }

//            plotsTabbedPane.removeAll();
            if(!plotsForNavigator && plotsSplitPane != null){
                plotsSplitPane.setLeftComponent(null);
                removePlotMarkers();
            }
//            plotsPanel.removeAll();


            String assemblyName = fileOpened.getAbsolutePath();

            Color c = chartColors.get(assemblyName);
            if(c == null){
                c = MyDrawingSupplier.PAINT_SEQ_FOR_WHITE_BG[0];
            }

            HashMap<String, Color> pathInList = new HashMap<String, Color>();
            pathInList.put(fileNameForN50plot, c);

            HashMap<String, Integer> ks = new HashMap<String, Integer>();
            ks.put(fileNameForN50plot, kValues.get(assemblyName));

            HashMap<String, Color> pathInList2 = new HashMap<String, Color>();
            pathInList2.put(absPath + File.separator + "coverage.hist", c);


            
            JSplitPane splitPane = plotsSplitPane;
            if(splitPane == null){
                splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, null, null);
                splitPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
                splitPane.setOneTouchExpandable(true);
                addComponentsMouseListener(splitPane, plotsComponentsListener);
            }

            boolean errDrawingN50plot = false;
            boolean errDrawingCovPlot = false;

            if(drawN50plotCheckBox.isSelected()){
                if (splitPane.getLeftComponent() == null){
                    n50plot = new N50plot(plotSettings.minContigLength);
                    JPanel n50plotPanel = null;
                    try {
                        String read1 = "Reading files for N50plot...";
                        setStatus(read1, true, NORMAL_STATUS);
                        n50plotPanel = n50plot.drawPlot(pathInList, ks, plotSettings.logY, plotSettings.xAxisInUnitOfLength, N50stats.BP, useFastaIndexCheckBox.isSelected()); // // Scaffold span
                        splitPane.setLeftComponent(n50plotPanel);
                        addComponentsMouseListener(n50plotPanel, plotsComponentsListener);
                    } catch (Exception ex) {
                        errDrawingN50plot = true;
        //                ex.printStackTrace();
                    }
                    if(n50plotPanel == null){
                        errDrawingN50plot = true;
                    }
                }
            }

            if(drawCovPlotCheckBox.isSelected()){
                if (splitPane.getRightComponent() == null){                    
                    covplot = new CoverageHistogram();
                    JPanel covplotPanel = null;
                    try {
                        String read2 = "Reading files for k-mer coverage plot...";
                        setStatus(read2, true, NORMAL_STATUS);
                        covplotPanel = covplot.drawPlot(pathInList2);
                        splitPane.setRightComponent(covplotPanel);
                        addComponentsMouseListener(covplotPanel, plotsComponentsListener);
                    } catch (Exception ex) {
                        errDrawingCovPlot = true;
        //                ex.printStackTrace();
                    }
                    if(covplotPanel == null){
                        errDrawingCovPlot = true;
                    }
                }
            }

            //TODO: notify user if plots couldn't be drawn

                String errMsg = null;
                int indexOfTab = plotsTabbedPane.indexOfComponent(splitPane);

                if(errDrawingN50plot && errDrawingCovPlot){
                    errMsg = "ERROR: Cannot draw N50-plot and k-mer coverage plot.";
                }
                else if(errDrawingN50plot && !errDrawingCovPlot){
                    errMsg = "ERROR: Cannot draw N50-plot.";
                }
                else if(!errDrawingN50plot && errDrawingCovPlot){
                    errMsg = "ERROR: Cannot draw k-mer coverage plot.";
                }
                
                if (n50plot == null && covplot != null){
                    if(indexOfTab < 0){
                        plotsTabbedPane.add("Cov. Plot", splitPane);
                    }
                    else{
                        plotsTabbedPane.setTitleAt(indexOfTab, "Cov. Plot");
                    }
                    splitPane.setResizeWeight(0.0);
                    splitPane.setDividerLocation(0.0);
                    plotsSplitPane = splitPane;
                }
                else if (n50plot != null && covplot == null){
                    if(indexOfTab < 0){
                        plotsTabbedPane.add("N50-Plot", splitPane);
                    }
                    else{
                        plotsTabbedPane.setTitleAt(indexOfTab, "N50-Plot");
                    }
                    splitPane.setResizeWeight(1.0);
                    splitPane.setDividerLocation(1.0);
                    plotsSplitPane = splitPane;
                }
                else if (n50plot != null && covplot != null){
                    if(indexOfTab < 0){
                        plotsTabbedPane.add("N50-Plot & Cov. Plot", splitPane);
                    }
                    else{
                        plotsTabbedPane.setTitleAt(indexOfTab, "N50-Plot & Cov. Plot");
                    }
                    splitPane.setResizeWeight(0.5);
                    splitPane.setDividerLocation(0.5);
                    plotsSplitPane = splitPane;
                }

            plotsForNavigator = true;

            showPlotsToggleButton.setSelected(true);

            lastPlotSettingsUsed = plotSettings;

            explorer.addPlotMarkers();

            return errMsg;
    }

    public void addPlotMarkers(String id, Integer lenInBP, Float cov, int k){
        if(lastPlotSettingsUsed != null){
            int lenInKmer = lenInBP - k + 1;
            int unit = lastPlotSettingsUsed.unit;

            int len = lenInBP;
            switch(unit){
                case N50plot.KMER:
                    len = lenInKmer;
                    break;
                case N50plot.NR_KMER:
                    len = lenInBP - 2*k + 2;
                    break;
                case N50plot.NOLBP:
                    len = Utilities.getAdjustedLength(explorer.getFileOpened().getAbsolutePath()+File.separator+lastPlotSettingsUsed.fileName,  id, k);
                    //System.out.println(len);
                    break;
                default:
                    break;
            }

            if(n50plot != null){
                n50plot.addRangeMarker(len);
            }

            if(cov != null && lenInKmer > 0 && covplot != null){
                //int meanCoverage = cov/lenInKmer;

                covplot.addDomainMarker(cov);
            }
        }
    }

    public void removePlotMarkers(){
        if(n50plot != null){
            n50plot.removeAllRangeMarkers();
        }
        if(covplot != null){
            covplot.removeAllDomainMarkers();
        }
    }

//    private void pressSplitPaneLeftButton(JSplitPane pane){
//        BasicSplitPaneUI ui = (BasicSplitPaneUI) pane.getUI();
//        BasicSplitPaneDivider divider = ui.getDivider();
//        JButton button = (JButton) divider.getComponent(0);
//        button.doClick();
//    }
//
//    private void pressSplitPaneRightButton(JSplitPane pane){
//        BasicSplitPaneUI ui = (BasicSplitPaneUI) pane.getUI();
//        BasicSplitPaneDivider divider = ui.getDivider();
//        JButton button = (JButton) divider.getComponent(1);
//        button.doClick();
//    }

    private void performSearch(){
        
        if(sequenceSearchRadioButtonMenuItem.isSelected()){
            validateQueryAndShowSequences();
        }
        else if(navigatorSearchRadioButtonMenuItem.isSelected()){
            if(singleFileLoaded){
                searchContigInExplorerForSingleFileMode();
            }
            else{
                searchContigInExplorerForAssembliesMode();
            }
        }
        else{
            if(singleFileLoaded){
                findShortestPathForSingleFileMode();
            }
            else{
                findShortestPathForAssembliesMode();
            }
        }
    }

    private void searchContigInExplorerForSingleFileMode(){
        String id = contigSearchTextField.getText().trim();

        if(id.equals("") || id.equals(SEQUENCE_SEARCH_QUERY_FORMAT)){
            // nothing was entered.
            String msg = "Please enter one or two contig IDs.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(Processor.isContigID(id)){
            try{
                int r = explorer.loadFileAndAnchorAtContig(explorer.getFileOpened(), explorer.getK(), id);
                if(r > -1){
//                    if(currentQuery != null){
//                        lastQuery = currentQuery;
//                        backButton.setEnabled(true);
//                    }
//                    currentQuery = id;
                }
                else{
                    JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage("Could not find contig '" + id + "' in Navigator.", MAX_LENGTH_PER_LINE_IN_DIALOG), "Not found", JOptionPane.ERROR_MESSAGE);
                }
            }
            catch(InterruptedException ex){
                String doneText = "Stopped by user.";
                setStatus(doneText, true, ERROR_STATUS);
            }
            catch(IOException e){
                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(e.getMessage(), MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        else if(id.split(",").length == 2){
            findShortestPathForSingleFileMode();
            return;
        }
        else {
            String msg = "The contig id must be an integer and may end with either '+' or '-'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }
    }

    private void searchContigInExplorerForAssembliesMode(){
        if(assembliesAndColorsTable.getRowCount() == 0){
            JOptionPane.showMessageDialog(this, "Please either add one or more assemblies or load a DOT or ADJ file.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(assembliesAndColorsTable.getSelectedRowCount() != 1){
            String msg = "Please select one row in the 'Assemblies' table.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int rowId = assembliesAndColorsTable.getSelectedRow();
        //int modelRowId = assembliesTable.convertRowIndexToModel(rowId);
        final String assembly = (String) assembliesAndColorsTable.getValueAt(rowId, 1);

        final String assemblyPath;
        if(sharedAncestorPath != null){
            assemblyPath = sharedAncestorPath + File.separator + assembly;
        }
        else{
            assemblyPath = assembly;
        }

        final File kdir = new File(assemblyPath);

        if(!kdir.exists()){
            String msg = "Could not find '" + assemblyPath + "'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String id = contigSearchTextField.getText().trim();

        if(id.equals("") || id.equals(SEQUENCE_SEARCH_QUERY_FORMAT)){
            // nothing was entered.
            String msg = "Please enter one or two contig IDs.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(id.split(",").length == 2){
//            sequenceSearchRadioButtonMenuItem.setSelected(true);
//            validateQueryAndShowSequences();
            findShortestPathForAssembliesMode();
            return;
        }

        if(!Processor.isContigID(id)){
            String msg = "The contig id must be an integer and may end with either '+' or '-'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                Long start = System.currentTimeMillis();
                try{
                    statusLabel.setForeground(Color.BLACK);
                    stopButton.setEnabled(true);
                    busyCursor();
                    enableLocateDirComponents(false);
                    enableSettingsComponents(false);
                    applyButton.setEnabled(false);

                    
                    int k = kValues.get(assemblyPath);
                    navLabel.setText("NAVIGATOR (" + assembly + ")");
                    navigatorPanel.setVisible(true);
                    int r = explorer.loadFileAndAnchorAtContig(kdir, k, id);
                    if(r == -1){
                        showNonModalDialog("ERROR", "Could not find contig '" + id + "' in Navigator.");
                    }

//                    navLabel.setText("NAVIGATOR (" + assembly + ")");
//                    navigatorPanel.setVisible(true);
                    showNavigatorToggleButton.setSelected(true);
                    applySettings();
                    if(r > -1){
//                        if(currentQuery != null){
//                            lastQuery = currentQuery;
//                            backButton.setEnabled(true);
//                        }
//                        currentQuery = id;
                        String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                        setStatus(doneText, true, NORMAL_STATUS);
                    }
                    else{
                        String doneText = "Could not find contig '" + id + "' in Navigator.";
                        setStatus(doneText, true, ERROR_STATUS);
                    }

                }
                catch (InterruptedException ex){
                    String doneText = "Stopped by user.";
                    setStatus(doneText, true, ERROR_STATUS);
                }
                catch (IOException ex){
                    //ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                catch (Exception ex){
                    ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                finally{
                    stopButton.setEnabled(false);
                    enableLocateDirComponents(true);
                    if(singleFileLoaded){
                        enableNavigatorSettingsComponents(true);
                    }
                    else{
                        enableSettingsComponents(true);
                    }
                    defaultCursor();
                }
                return null;
            }
        };
        workers.add(worker);
        worker.execute();
        if(worker.isDone()){
            workers.remove(worker);
        }

//        try{
//            int k = kValues.get(assemblyPath);
//            int r = -1;
//
//            r = explorer.loadFileAndAnchorAtContig(kdir, k, id);
//
//            navLabel.setText("NAVIGATOR (" + assembly + ")");
//
//            if(r > -1){
//                if(currentQuery != null){
//                    lastQuery = currentQuery;
//                    backButton.setEnabled(true);
//                }
//                currentQuery = id;
//                showNavigatorToggleButton.setSelected(true);
//
//                applySettings();
//            }
//            else{
//                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage("Could not find contig '" + id + "' in Navigator.", MAX_LENGTH_PER_LINE_IN_DIALOG), "Not found", JOptionPane.ERROR_MESSAGE);
//            }
//        }
//        catch(IOException e){
//            String msg = e.getMessage();
//            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
//            explorer.clear();
//            navLabel.setText("NAVIGATOR");
//            //explorerPanel.setVisible(false);
//            showNavigatorToggleButton.setSelected(false);
//
//            setStatus(msg, true, ERROR_STATUS);
//        }
    }

    private void findShortestPathForAssembliesMode(){
        if(assembliesAndColorsTable.getRowCount() == 0){
            JOptionPane.showMessageDialog(this, "Please either add one or more assemblies or load a DOT or ADJ file.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(assembliesAndColorsTable.getSelectedRowCount() != 1){
            String msg = "Please select one row in the 'Assemblies' table.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int rowId = assembliesAndColorsTable.getSelectedRow();
        final String assembly = (String) assembliesAndColorsTable.getValueAt(rowId, 1);

        final String assemblyPath;
        if(sharedAncestorPath != null){
            assemblyPath = sharedAncestorPath + File.separator + assembly;
        }
        else{
            assemblyPath = assembly;
        }

        final File kdir = new File(assemblyPath);

        if(!kdir.exists()){
            String msg = "Could not find '" + assemblyPath + "'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String q = contigSearchTextField.getText().trim();
        final String[] ids = q.split(",");

        if(ids.length != 2){
            String msg = "For shortest path search, please enter 2 contig IDs, separated by a comma. Otherwise, enter 1 contig ID only.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(!Processor.isContigID(ids[0]) || !Processor.isContigID(ids[1])){
            String msg = "The contig ID must be an integer and may end with either '+' or '-'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

       SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                Long start = System.currentTimeMillis();
                try{
                    statusLabel.setForeground(Color.BLACK);
                    stopButton.setEnabled(true);
                    busyCursor();
                    enableLocateDirComponents(false);
                    enableSettingsComponents(false);
                    applyButton.setEnabled(false);


                    navLabel.setText("NAVIGATOR (" + assembly + ")");
                    int k = kValues.get(assemblyPath);
                    String errMsg = null;
                    try{
                        navigatorPanel.setVisible(true);
                        explorer.loadFileAndFindShortestPath(kdir, k, ids[0].trim(), ids[1].trim());
                    }
                    catch(Exception ex){
                        errMsg = "ERROR: " + ex.getMessage();

                        showNonModalDialog("ERROR", errMsg);
                    }
//                    navigatorPanel.setVisible(true);
                    showNavigatorToggleButton.setSelected(true);
                    applySettings();

                    if(errMsg != null){
                        setStatus(errMsg, true, ERROR_STATUS);
                    }
                    else{
                        String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                        setStatus(doneText, true, NORMAL_STATUS);
                    }
                }
                catch (InterruptedException ex){
                    String doneText = "Stopped by user.";
                    setStatus(doneText, true, ERROR_STATUS);
                }
                catch (IOException ex){
                    //ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                catch (Exception ex){
                    ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                finally{
                    stopButton.setEnabled(false);
                    enableLocateDirComponents(true);
                    if(singleFileLoaded){
                        enableNavigatorSettingsComponents(true);
                    }
                    else{
                        enableSettingsComponents(true);
                    }
                    defaultCursor();
                }
                return null;
            }
        };
        workers.add(worker);
        worker.execute();
        if(worker.isDone()){
            workers.remove(worker);
        }

//        navLabel.setText("NAVIGATOR (" + assembly + ")");
//        int k = kValues.get(assemblyPath);
//        try{
//            explorer.loadFileAndFindShortestPath(kdir, k, ids[0], ids[1]);
//            showNavigatorToggleButton.setSelected(true);
//        }
//        catch(Exception e){
//            String msg = e.getMessage();
//            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
    }

    private void findShortestPathForSingleFileMode(){
        String q = contigSearchTextField.getText().trim();
        String[] ids = q.split(",");

        if(ids.length != 2){
            String msg = "For shortest path search, please enter 2 contig IDs, separated by a comma. Otherwise, enter 1 contig ID only.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if(!Processor.isContigID(ids[0]) || !Processor.isContigID(ids[1])){
            String msg = "The contig ID must be an integer and may end with either '+' or '-'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try{
            explorer.findShortestPath(ids[0].trim(), ids[1].trim());
        }
        catch(Exception e){
            String msg = e.getMessage();
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
    }


    public void setQuery(String newQuery){
        if(!newQuery.equals(currentQuery)){
            lastQuery = currentQuery;
            currentQuery = newQuery;
        }
        backButton.setEnabled(lastQuery != null);
    }
    
    public void setQuery(String newQuery, String oldQuery){
        lastQuery = oldQuery;
        currentQuery = newQuery;
        backButton.setEnabled(lastQuery != null);
    }

    private void explore(String assembly) throws IOException, InterruptedException{
        explore(assembly, null);
    }

    private void explore(String assembly, String theFile) throws IOException, InterruptedException{
//        if(workingDir != null && kDirTable.getSelectedRowCount() == 1) {
        String path = assembly;

            int k = kValues.get(assembly);

            if(theFile != null){
                path += File.separator + theFile;
            }

            File f = new File(path);

            int r = explorer.loadFile(f, k);
            //##
                String name = null;
                if(sharedAncestorPath != null){
                    name = assembly.substring(sharedAncestorPath.length()+1);
                }
                else{
                    name = assembly;
                }

                navLabel.setText("NAVIGATOR (" + name + ")");

                this.navigatorPanel.repaint();
//            explorerHasFocus();
//        }
//        else{
//            throw new Exception("Please select one assembly.");
//        }
    }

    private JPanel launchExplorer(){
        if(explorer == null) {
            explorer = new AbyssExplorer(this);
        }
        
        JPanel panel = explorer.launchInPanel(showLabelsCheckBox,
                        showLengthCheckBox,
                        lengthSlider,
//                        showPEPartnersCheckBox,
//                        showPEContigsCheckBox,
                        peRadioButton,
                        seRadioButton,
                        showAllRadioButton,
                        showNeighborsRadioButton,
                        stepSizeSlider,
                        scaffoldRadioButton);
        return panel;
    }

    private void launchExplorerAndAnchorAtContig(){
        final File kdir = new File(lastAssemblySearched);
        final String id = (String) contigIdsComboBox.getSelectedItem();

        try{
            int k = kValues.get(lastAssemblySearched);

            //need to figure out whether the id is from PE/SE
            if(scaffoldIds.contains(id)){
                scaffoldRadioButton.setSelected(true);
            }
            else if(contigIds.contains(id)){
                peRadioButton.setSelected(true);
            }
            else if(unitigIds.contains(id)){
                seRadioButton.setSelected(true);
            }

            int indexToCut = 0;
            if(sharedAncestorPath != null){
                indexToCut = sharedAncestorPath.length()+1;
            }
            navLabel.setText("NAVIGATOR (" + lastAssemblySearched.substring(indexToCut) + ")");
            navigatorPanel.setVisible(true);
            
            explorer.loadFileAndAnchorAtContig(kdir, k, id);

//            navLabel.setText("NAVIGATOR (" + lastAssemblySearched.substring(indexToCut) + ")");
//            navigatorPanel.setVisible(true);

            showNavigatorToggleButton.setSelected(true);
        }
        catch(InterruptedException ex){
            String doneText = "Stopped by user.";
            setStatus(doneText, true, ERROR_STATUS);
        }
        catch(IOException e){
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(e.getMessage(), MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private PlotSettings getPlotSettings(){
        String fileName = (String) filesComboBox.getSelectedItem();

        int mcl = ((Long) minContigLengthFormattedTextField.getValue()).intValue();
        boolean logY = false;
        boolean xAxisInUnitOfLength = false;
        int unit = -1;

        String yScale = (String)xAxisScaleComboBox.getSelectedItem();
        if(yScale.equals("logarithmic")) {
            logY= true;
        }

        String xUnit = (String)yAxisUnitComboBox.getSelectedItem();
        if(xUnit.equals("reconstruction")) {
            xAxisInUnitOfLength = true;
        }

        String unitStr = (String)unitOfLengthComboBox.getSelectedItem();
        if(unitStr != null){
            if(unitStr.equals("k-mer")) {
                unit = N50plot.KMER;
            }
            else if(unitStr.equals("nr-k-mer")) {
                unit = N50plot.NR_KMER;
            }
            else if(unitStr.equals("nol-bp")) {
                unit = N50plot.NOLBP;
            }
            else {
                unit = N50plot.BP;
            }
        }
        else{
            unit = N50plot.BP;
        }

        int[] selectedKs = assembliesAndColorsTable.getSelectedRows();
        String[] selectedLibraries = getSelectedLibraries();
        
        int assemblyMode = -1;
        if(seRadioButton.isSelected()){
            assemblyMode = UNITIGS;
        }
        else if(peRadioButton.isSelected()){
            assemblyMode = CONTIGS;
        }
        else if(scaffoldRadioButton.isSelected()){
            assemblyMode = SCAFFOLDS;
        }
        boolean showPEContigs = seRadioButton.isSelected();

        return new PlotSettings(fileName,
                            mcl,
                            logY,
                            xAxisInUnitOfLength,
                            unit,
                            selectedKs,
//                            peRadioButton.isSelected(),
                            assemblyMode,
                            showLengthCheckBox.isSelected(),
                            lengthSlider.getValue(),
                            showLabelsCheckBox.isSelected(),
                            showPEContigs,
//                            showPEContigsCheckBox.isSelected(),
//                            showPEPartnersCheckBox.isSelected(),
                            showNeighborsRadioButton.isSelected(),
                            stepSizeSlider.getValue(),
                            drawN50plotCheckBox.isSelected(),
                            drawFragSizeDistCheckBox.isSelected(),
                            selectedLibraries,
                            drawCovPlotCheckBox.isSelected()
                );
    }

    private String[] getSelectedLibraries(){
        ArrayList<String> libs = new ArrayList<String>();

        for(JCheckBox cb : libraryCheckBoxes){
            if(cb.isSelected()){
                libs.add(cb.getText());
            }
        }

        int numSelectedLibs = libs.size();
        if(numSelectedLibs == 0){
            return new String[0];
        }

        String[] selectedLibs = new String[numSelectedLibs];
        libs.toArray(selectedLibs);
        return selectedLibs;
    }

//    private void selectLibrary(String libraryName){
//        if(!drawFragSizeDistCheckBox.isSelected()){
//            drawFragSizeDistCheckBox.setSelected(true);
//        }
//        for(JCheckBox cb : libraryCheckBoxes){
//            if(cb.getText().equals(libraryName)){
//                cb.setSelected(true);
//                return;
//            }
//        }
//    }

    private boolean librariesSelected(){
        for(JCheckBox cb : libraryCheckBoxes){
            if(cb.isSelected()){
                return true;
            }
        }
        return false;
    }

    private void addListenerForLibraryCheckBox(JCheckBox cb){
        cb.addItemListener(new ItemListener(){
            public void itemStateChanged(ItemEvent ie) {
                if(ie.getStateChange() == ItemEvent.SELECTED){
                    selectLibrariesButton.setOpaque(false);
                    selectLibrariesButton.repaint();
                }
                else{
                    selectLibrariesButton.setOpaque(!librariesSelected());
                    selectLibrariesButton.repaint();
                }
                validateSettings();
                validateViewButtonsStatus();
            }            
        });
    }

    private void removeUselessLibraries(){
        int numCB = libraryCheckBoxes.size();

        if(kValues != null){
            Set<String> assembliesAdded = kValues.keySet();

            for(int i=numCB-1; i>=0; i--){
                JCheckBox cb = libraryCheckBoxes.get(i);
                String libName = cb.getText();
                boolean libOK = false;
                for(String path: assembliesAdded){
                    File f = new File(path + File.separator + libName + "-3.hist");                    
                    if(f.exists()){
                        libOK = true;
                        break;
                    }
                }
                if(!libOK){
                    fsdFileNames.remove(libName + "-3.hist");
                    this.libsPopupMenu.remove(cb);
                    this.libraryCheckBoxes.remove(i);
                }
            }
        }
        else{
            //this.libsPopupMenu.removeAll();
            for(JCheckBox c :libraryCheckBoxes){
                libsPopupMenu.remove(c);
            }

            this.libraryCheckBoxes.clear();
            this.fsdFileNames.clear();
        }
    }

    private void openBlatPageInBrowser() {
        try {
            Desktop desktop = Desktop.getDesktop();
            String genome = java.net.URLEncoder.encode((String) genomeComboBox.getSelectedItem(), "UTF-8");
            String assembly = blatInterface.getDatabaseName((String) assemblyComboBox.getSelectedItem());

            String blatAddress = "http://genome.ucsc.edu/cgi-bin/hgBlat?org=" + genome;
            if(assembly != null){
                blatAddress += "&db=" + java.net.URLEncoder.encode(assembly, "UTF-8");
            }
            blatAddress += "&type=submit&userSeq=";
            String query = contigSequenceTextArea.getText();

            //need to split query into 25-sequence or 8000-character (whichever is smaller) chunks

            ArrayList<String> list = Processor.splitIntoChunks(query);
            Iterator<String> itr = list.iterator();

            while(itr.hasNext()) {
                String sequenceInURL = java.net.URLEncoder.encode(itr.next(), "UTF-8");
                String url = blatAddress + sequenceInURL;
                //System.out.println(url);
                URI address = new URI(url);
                desktop.browse(address);

                if(itr.hasNext()) {
                    /*
                     * Sleep for 15 seconds.
                     * According to BLAT use restrictions at http://genome.ucsc.edu/FAQ/FAQblat.html#blat2
                     * "Program-driven use of Blat is limited to a maximum of one hit every 15 seconds"
                     */
                    System.out.println("Wait for 15 seconds...");
                    try {
                        Thread.sleep(15000L);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } catch (IOException ex) {
            //do nothing for now
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "IOException", JOptionPane.ERROR_MESSAGE);
        } catch (java.net.URISyntaxException ex) {
            //do nothing for now
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, ex.getMessage(), "URISyntaxException", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void validateQueryAndShowSequences() {
        if(assembliesAndColorsTable.getRowCount() == 0){
            JOptionPane.showMessageDialog(this, "Please either add one or more assemblies or load a DOT or ADJ file.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String query = contigSearchTextField.getText().trim();

        if(query.equals("") || query.equals(SEQUENCE_SEARCH_QUERY_FORMAT)) {
            // nothing was entered.
            String msg = "Please enter the a query term for each of the following: assembly, file name, and contig id";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int indexOfColon= query.indexOf(":");

        String assembly = null;
        String contigIDsStr = null;
        if(indexOfColon < 0) {// no assemlby, contig id(s) only
            if(assembliesAndColorsTable.getSelectedRowCount() != 1) {
                String msg = "You must provide the assembly name or select a row in the 'Assemblies' table.";
                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int rowId = assembliesAndColorsTable.getSelectedRow();
            assembly = (String) assembliesAndColorsTable.getValueAt(rowId, 1);
            contigIDsStr = query;
        }
        else {// have assembly
            if(indexOfColon == query.length()-1) {
                // no contig id
                String msg = "You did not enter a contig id.";
                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }

            assembly = query.substring(0, indexOfColon).replace("/", File.separator);
            contigIDsStr = query.substring(indexOfColon+1);
        }

        String path = null;
        if(sharedAncestorPath != null){
            path = sharedAncestorPath + File.separator + assembly;
        }
        else{
            path = assembly;
        }

        File f = new File(path);
        if(f.exists()){
            if(f.isFile()){
                lastAssemblySearched = f.getParentFile().getAbsolutePath();
            }
            else{
                lastAssemblySearched = f.getAbsolutePath();
            }
        }
        else{            
            String msg = "Could not find '"+ path + "'.\nPlease check the spelling and try again.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Not found", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] contigIDs = null;
        if(query.indexOf(",") > 0){
            contigIDs = contigIDsStr.split(",");
        }
        else{
            contigIDs = contigIDsStr.split(" ");
        }

        for(String contigID : contigIDs) {
            if(!Processor.isContigID(contigID)) {
                String msg = "Your query was not in the correct format.";
                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Warning", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        showSequences(path, contigIDs);
    }

    private Set<String> scaffoldIds = new HashSet<String>();
    private Set<String> contigIds = new HashSet<String>();
    private Set<String> unitigIds = new HashSet<String>();
 
    public void showSequences(String path, String[] contigIDs){
        scaffoldIds = new HashSet<String>();
        contigIds = new HashSet<String>();
        unitigIds = new HashSet<String>();

        contigIdsComboBox.setModel(new DefaultComboBoxModel(contigIDs));
        contigIdsComboBox.setSelectedIndex(0);
        Set<SearchResults> results = Processor.findContigsInPath(path, contigIDs);

        String contigIDAsString = contigIDs[0];
        int len = contigIDs.length;
        String s = "";
        if(len > 1){
            s = "s";
        }

        for(int i=1; i<len; i++){
            contigIDAsString += "," + contigIDs[i];
        }

        if(results == null || results.size() <= 0) {
            // contig not found
            String msg = "Could not find contig" + s + " '"+ contigIDAsString + "' in '" + path + "'.";
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String concat = "";
        int numLongerThanLimit = 0;
        int numSeqs = 0;
        for(SearchResults r : results){
            for(String seq : r.seqs){
                numSeqs++;
                if(seq.length() >= 8000){
                    numLongerThanLimit++;
                }
                concat += seq + "\n";
            }

            String fileName = r.fileName;
            if(fileName.endsWith("-"+SCAFFOLDS+".fa") || fileName.endsWith("-scaffolds.fa")){
                scaffoldIds.addAll(r.contigIds);
            }
            else if(fileName.endsWith("-contigs.fa") || fileName.endsWith("-"+CONTIGS+".fa")){
                contigIds.addAll(r.contigIds);
            }
            else if(fileName.endsWith("-5.fa") || fileName.endsWith("-4.fa") || fileName.endsWith("-3.fa")){
                unitigIds.addAll(r.contigIds);
            }
        }

        //if found, pop up a window displaying sequence
        contigSequenceFrame.setVisible(false); // this line is needed for case when the frame is already visible
        contigSequenceTextArea.setText(concat);
        contigSequenceTextArea.setCaretPosition(0);
        int width = 500;
        int height = 500;

        int indexToCut = 0;
        if(sharedAncestorPath != null){
            indexToCut = sharedAncestorPath.length()+1;
        }

        contigSequenceFrame.setTitle("Contig" + s + " " +  contigIDAsString + " in " + path.substring(indexToCut));
        //System.out.println(this.getWidth() + "," + this.getHeight() );

        Point pt = this.getLocation();
        //SwingUtilities.convertPointToScreen(pt, this);
        contigSequenceFrame.setLocation(pt.x + this.getWidth()/2-width/2,
                                        pt.y + this.getHeight()/2-height/2);
        contigSequenceFrame.setSize(width, height);

        contigSequenceFrame.setVisible(true);
        contigSequenceFrame.setState(java.awt.Frame.NORMAL);
        contigSequenceFrame.toFront();

        boolean enableBlat = numLongerThanLimit < numSeqs;
        sequencesTooLongWarningLabel.setVisible(!enableBlat);
        blatButton.setEnabled(enableBlat);
        assemblyComboBox.setEnabled(enableBlat);
        genomeComboBox.setEnabled(enableBlat);
        noteLabel.setVisible(enableBlat);
    }

//    private void displaySubDirectoryHint() {
//        String path = pathTextField.getText();
//        int lastSlashIndex = path.lastIndexOf(File.separator);
//        if(lastSlashIndex >=0 && lastSlashIndex < path.length() -1) {
//            String parentDirStr = path.substring(0, lastSlashIndex);
//            if(lastSlashIndex == 0) { // if the parent dir is '/'
//                parentDirStr = File.separator;
//            }
//
//            File parentDir = new File(parentDirStr);
//            if(parentDir.exists() && parentDir.isDirectory()) {
//                String guess = path.substring(lastSlashIndex+1);
//
//                String[] list = parentDir.list(new DirNamePrefixFilter(guess));
//
//                if(list.length > 0) {
//                    String newPath = null;
//                    if(lastSlashIndex == 0) {
//                        newPath = parentDirStr + list[0];
//                    }
//                    else {
//                        newPath = parentDirStr + File.separator + list[0];
//                    }
//
//                    pathTextField.setText(newPath);
//
//                    //select the hint
//                    pathTextField.setCaretPosition(newPath.length());
//                    pathTextField.moveCaretPosition(path.length());
//                }
//            }// don't do anything if parent dir does not exist
//
//        }// don't do anything if the path ends with a separator
//
//    }


    private HistogramStats getHistogramStats(String path) {
        ArrayList<Long> counts = new ArrayList<Long>();
        ArrayList<Long> bins = new ArrayList<Long>();

        try{
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = null;

            while (( line = br.readLine()) != null) {
                String[] arr = line.split("\t");
                bins.add(new Long(arr[0])); // x
                counts.add(new Long(arr[1])); // y
            }

            br.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return new HistogramStats(bins, counts, true, true);
    }

    private void fillFragmentSizeTable() {
        DefaultTableModel model = (DefaultTableModel) fragmentStatsTable.getModel();

        int rowCount = model.getRowCount();
        for(int i=rowCount-1; i>=0; i--) {
            model.removeRow(i);
        }
        fragmentStatsTable.repaint();

        if(bestInSelection == null){
            return; // do nothing
        }

//        String suffix = "-3.hist";

//        String trueBestAssemblyPath = null;

//        if(bestAssembly == null) {
//            statusLabel.setText("Finding best assembly...");
//            System.out.println("Finding best assembly...");
//            // 'best' directory does not exist. Need to find the best assembly by comparing contiguity
//            trueBestAssemblyPath = Processor.getBestAssemblyPath(workingDir);
//
//            if(trueBestAssemblyPath == null){
//                // cannot determine best assembly
//                String msg = "Cannot find best assembly. Fragment sizes table cannot be filled out.";
//                JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_CHARS_PER_LINE), "Error", JOptionPane.ERROR_MESSAGE);
//                return;
//            }
//
//            File f = new File(trueBestAssemblyPath);
//            if(f.isDirectory()){
//                bestAssembly = f.getName();
//            }
//            else{
//                bestAssembly = f.getParentFile().getName();
//            }

//            //create the symbolic link using shell cmd
//            String source = trueBestAssemblyPath;
//            String link = workingDir + File.separator + "best";
//
//            try {
//                String[] cmd = null;
//
//                //linux and mac os x
//                final String[] linuxCmd = new String[]{"ln", "-s", source, link};
//
//                //Windows Vista and 7
//                final String[] windowsCmd = new String[]{"mklink", "/d", link, source};
//
//                String osName = System.getProperty("os.name");
//
//                if(osName.equals("Linux") || osName.equals("Mac OS X")){
//                    cmd = linuxCmd;
//                }
//                else if(osName.equals("Windows Vista") || osName.equals("Windows 7")){
//                    cmd = windowsCmd;
//                }
//                else{
//                    cmd = linuxCmd;
//                }
//
//                Process result = Runtime.getRuntime().exec(cmd);
//                BufferedReader br = new BufferedReader(new InputStreamReader(result.getInputStream()));
//                String line = null;
//                while((line = br.readLine() ) != null){
//                    System.out.println(line);
//                }
//
//                br = new BufferedReader(new InputStreamReader(result.getErrorStream()));
//                line = null;
//                while((line = br.readLine() ) != null){
//                    System.out.println(line);
//                }
//
//                br.close();
//                result.destroy();
//
//            } catch (IOException ex) {
//                //JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//                System.out.println("ERROR: " + ex.getMessage());
//            }
//        }
//        else{
//            trueBestAssemblyPath = bestInSelection;
//            if(workingDir != null){
//                trueBestAssemblyPath = workingDir + File.separator + trueBestAssemblyPath;
//            }
//        }
        int indexToCut = 0;
        if(sharedAncestorPath != null){
            indexToCut = sharedAncestorPath.length()+1;
        }
        Border border = fragmentSizePanel.getBorder();
        if(border instanceof TitledBorder) {
            TitledBorder tborder = (TitledBorder) border;
            tborder.setTitle("Fragment Sizes (" + bestInSelection.substring(indexToCut) + ")");
            fragmentSizePanel.repaint();
        }
        //fragmentSizePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Fragment Sizes (" + bestAssembly + ")"));

        File kDir = new File(bestInSelection);
        String[] fList = kDir.list(new FileNameRegexFilter(".*-3\\.hist"));
        for(String fName : fList) {
            String fPath = bestInSelection + File.separator + fName;
            setStatus("Reading file: " + fPath, true, NORMAL_STATUS);

            HistogramStats stats = getHistogramStats(fPath);
            if(stats != null) {

                HistogramStats stats2 = stats.getStatsForNegativeData();
                String library = fName.substring(0, fName.lastIndexOf("-3.hist"));

                double mean = stats.getMean();
                double stdev = stats.getStdev();

                int percentPlus = stats.getPercentageOfPostiveSizes();
                int percentMinus = 100 - percentPlus;
                String percentPlusStr = Integer.toString(percentPlus);
                String percentMinusStr = Integer.toString(percentMinus);

                if(stats.isFlipped()){
                    percentPlusStr += "% RF";
                    percentMinusStr += "% FR";
                }
                else{
                    percentPlusStr += "% FR";
                    percentMinusStr += "% RF";
                }

                model.addRow(new Object[]{library,
                                        percentPlusStr,
                                        stats.getMin(), //min
                                        stats.getQ1(), //Q1
                                        stats.getMedian(), //median
                                        stats.getQ3(), //Q3
                                        stats.getMax(), //max
                                        mean, //mean
                                        stdev, //stdev
                                        stdev/mean, // stdev/mean
                                        stats.getQFactor()
                });

                if(stats2 != null){

                    double mean2 = stats2.getMean();
                    double stdev2 = stats2.getStdev();

                    model.addRow(new Object[]{library,
                                            percentMinusStr,
                                            stats2.getMin(), //min
                                            stats2.getQ1(), //Q1
                                            stats2.getMedian(), //median
                                            stats2.getQ3(), //Q3
                                            stats2.getMax(), //max
                                            mean2, //mean
                                            stdev2, //stdev
                                            stdev2/mean2, // stdev/mean
                                            stats2.getQFactor()
                    });
                }
            }
        }

//        Iterator<String> itr = kDirPathList.iterator();
//        while(itr.hasNext())
//        {
//            String path = itr.next();
//            String assembly = Utilities.getAssemblyNameFromPath(path); //note: 'path' does not include the file name
//
//            File kDir = new File(path);
//            String[] fList = kDir.list(new FragmentSizeFileNameFilter());
//            for(int i=0; i<fList.length; i++)
//            {
//                String fName = fList[i];
//                String fPath = path + File.separator + fName;
//                statusLabel.setText("Reading file: " + fPath);
//                HistogramStats stats = getHistogramStats(fPath);
//                if(stats != null)
//                {
//                    double q = stats.getQFactor();
//                    long m = stats.getMedian();
//                    String library = fName.substring(0, fName.lastIndexOf(suffix));
//
//                    model.addRow(new Object[]{assembly, library, new Long(m), new Double(q)});
//                }
//            }
//        }

        Processor.packColumns(fragmentStatsTable, 2);
    }

//    private void fillFragmentSizeTableInNewThread() {
//        SwingWorker worker = new SwingWorker<Void, Void>() {
//            public Void doInBackground() throws Exception {
//                long start = System.currentTimeMillis();
//                try{
//                    busyCursor();
//                    enableLocateDirComponents(false);
//                    fillFragmentSizeTable();
//                    //fillAssembliesColumnInAssembliesTable((String)filesComboBox.getSelectedItem());
//                    //fillAssembliesColumnInAssembliesTable();
//                } finally{
//                    enableLocateDirComponents(true);
//                    defaultCursor();
//                    String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis() - start)/1000.0 + " seconds.";
//                    System.out.println(doneText);
//                    statusLabel.setText(doneText);
//                }
//                return null;
//            }
//        };
//        worker.execute();
//    }

    private static class MyPercentCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        MyPercentCellRenderer(){
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public void setValue(Object aValue) {
            Object result = aValue;
            if (( aValue != null) && (aValue instanceof Double)) {
              Double numberValue = (Double)aValue;
              DecimalFormat formatter = new DecimalFormat("####.##%");
              result = formatter.format(numberValue.doubleValue());
            }
            super.setValue(result);
        }
    }

    private static class MyDoubleCellRenderer extends DefaultTableCellRenderer
    {
        private static final long serialVersionUID = 1L;

        MyDoubleCellRenderer(){
            setHorizontalAlignment(SwingConstants.RIGHT);
        }

        @Override
        public void setValue(Object aValue) {
            Object result = aValue;
            if (( aValue != null) && (aValue instanceof Double)) {
              Double numberValue = (Double)aValue;
              DecimalFormat formatter = new DecimalFormat("####.##");
              result = formatter.format(numberValue.doubleValue());
            }
            super.setValue(result);
        }
    }

    /* This method is called when the user double-clicks on a library in the Fragment Sizes statistics table
     */
    private void drawFragmentSizeDistInNewThread() {
        SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                long start = System.currentTimeMillis();
                try{
                    setStatus("Drawing chart, please wait...", true, NORMAL_STATUS);
                    busyCursor();
                    
                    int selectedRow = fragmentStatsTable.getSelectedRow();
                    String libraryName = (String) fragmentStatsTable.getValueAt(selectedRow, 0);
                    if(libraryName.endsWith("(FR)")){
                        libraryName = libraryName.substring(0, libraryName.length()-4);
                    }
                    else if(libraryName.endsWith("(RF)")){
                        libraryName = libraryName.substring(0, libraryName.length()-4);
                    }

                    // About to draw a FSD of the library double-clicked, select the FSD checkbox
                    lastPlotSettingsUsed.drawFragmentSizeDistribution = true;
                    drawFragSizeDistCheckBox.setSelected(true);                    

                    int numTabs = plotsTabbedPane.getTabCount();
                    if(numTabs <= 0){ // tabbed pane has no tabs
                        // no N50-plot/Coverage plot, so deselect the corresponding check box
                        lastPlotSettingsUsed.drawN50plot = false;
                        drawN50plotCheckBox.setSelected(false);                        

                        //select only the checkbox for the library double-clicked
                        for(JCheckBox cb : libraryCheckBoxes){
                            cb.setSelected(cb.getText().equals(libraryName));
                            applyButton.setEnabled(false);
                        }
                    }
                    else{ // tabbed pane has one or more tabs
                        // get all tab titles
                        HashSet<String> tabTitles = new HashSet<String>();
                        for(int i=0; i<numTabs; i++){
                            tabTitles.add(plotsTabbedPane.getTitleAt(i));
                        }

                        if(!tabTitles.contains("N50-Plot") && !tabTitles.contains("N50-Plot & Cov. Plot") && !tabTitles.contains("Cov. Plot")){
                            lastPlotSettingsUsed.drawN50plot = false;
                            drawN50plotCheckBox.setSelected(false);                            
                        }

                        for(JCheckBox cb : libraryCheckBoxes){
                            // select only the libraries that are plotted or double-clicked
                            cb.setSelected(tabTitles.contains(cb.getText()) || cb.getText().equals(libraryName));
                            applyButton.setEnabled(false);
                        }
                    }
                    
                    lastPlotSettingsUsed.selectedLibs = getSelectedLibraries();                    

                    drawFragmentSizeDist(new String[]{libraryName});                    
                    plotsHasFocus();

                    String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis() - start)/1000.0 + " seconds.";
                    setStatus(doneText, true, NORMAL_STATUS);
                }
                catch(Exception ex){
                    ex.printStackTrace();
                    String doneText = "ERROR: " + ex.getMessage();
                    setStatus(doneText, true, ERROR_STATUS);
                }
                finally{
                    defaultCursor();
                    validateSettings();
                }
                return null;
            }
        };
        worker.execute();
    }

    private void navigatorHasFocus(){
        settingsTabbedPane.setSelectedIndex(1);

        navBorder.setBorder(BLACK_BORDER);

        statsBorder.setBorder(GRAY_BORDER);

        plotsBorder.setBorder(GRAY_BORDER);

        displayPanel.repaint();
    }

    private void plotsHasFocus(){
        settingsTabbedPane.setSelectedIndex(0);

        navBorder.setBorder(GRAY_BORDER);

        statsBorder.setBorder(GRAY_BORDER);

        plotsBorder.setBorder(BLACK_BORDER);

        displayPanel.repaint();
    }

    private void statsHasFocus(){
        settingsTabbedPane.setSelectedIndex(0);

        navBorder.setBorder(GRAY_BORDER);

        statsBorder.setBorder(BLACK_BORDER);

        plotsBorder.setBorder(GRAY_BORDER);

        displayPanel.repaint();
    }

    private void clearFocus(){
        navBorder.setBorder(GRAY_BORDER);

        statsBorder.setBorder(GRAY_BORDER);

        plotsBorder.setBorder(GRAY_BORDER);

        displayPanel.repaint();
    }

//    private void drawKmerCoveragePlotInNewThread() {
//        SwingWorker worker = new SwingWorker<Void, Void>() {
//            public Void doInBackground() throws Exception {
//                long start = System.currentTimeMillis();
//                try{
//                    statusLabel.setText("Drawing chart, please wait...");
//                    busyCursor();
//                    drawKmerCoveragePlot();
//                } catch(Exception e) {
//                    e.printStackTrace();
//                } finally{
//                    defaultCursor();
//                    String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis() - start)/1000.0 + " seconds.";
//                    System.out.println(doneText);
//                    statusLabel.setText(doneText);
//                }
//                return null;
//            }
//        };
//        worker.execute();
//    }

//    private void drawN50plotInNewThread() {
//        SwingWorker worker = new SwingWorker<Void, Void>() {
//            public Void doInBackground() throws Exception {
//                long start = System.currentTimeMillis();
//                try{
//                    statusLabel.setText("Drawing chart, please wait...");
//                    busyCursor();
//                    drawN50plot();
//                } finally{
//                    defaultCursor();
//                    String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis() - start)/1000.0 + " seconds.";
//                    System.out.println(doneText);
//                    statusLabel.setText(doneText);
//                }
//                return null;
//            }
//        };
//        worker.execute();
//    }

    private void drawFragmentSizeDist(String[] libraryNames) {

        if(bestInSelection == null){
            Map<String, Color> selectedKs = getSelectedAssemblies();
            Set<String> ks = selectedKs.keySet();
            if(ks.size() == 1){
                bestInSelection = ks.iterator().next();
            }
            else{
                long largestContiguity = 0;

                PlotSettings ps = getPlotSettings();
                long mcl = ps.minContigLength;
                int unit = ps.unit;

                for(String assembly : ks){
                    int k = kValues.get(assembly);
                    long contiguity = 0;
                    try{
                        contiguity = Processor.calculateContiguity2(assembly, mcl, k, unit);
                        if(contiguity > largestContiguity){
                            largestContiguity = contiguity;
                            bestInSelection = assembly;
                        }
                    }
                    catch (Exception e){
                        String msg = "ERROR: Could not calculate contiguity for '" + assembly + "'";
                        setStatus(msg, true, ERROR_STATUS);
                    }
                }
            }
        }

        // prompt err msg if bestInSelection == null at this point.

        for(String libraryName : libraryNames){
            int index = plotsTabbedPane.indexOfTab(libraryName);
            if(index < 0){
                String path = bestInSelection + File.separator + libraryName + "-3.hist";
                FragmentSize fs = new FragmentSize();
                JPanel panel = fs.drawPlot(path, chartColors.get(bestInSelection));

                plotsTabbedPane.add(libraryName, panel);
                index = plotsTabbedPane.indexOfComponent(panel);
                setTabComponentsVisible(showTabsHeader);
            }
            plotsTabbedPane.setSelectedIndex(index);
        }

        showPlotsToggleButton.setSelected(true);
    }

//    private void displayNoAssembliesSelectedMessage() {
//        Border border = assembliesPanel.getBorder();
//        String title = null;
//        if(border instanceof TitledBorder) {
//            title = ((TitledBorder) border).getTitle();
//        }
//        else {
//            title = "Assemblies";
//        }
//
//        String msg = "No assemblies were selected.\nSelect one or more assemblies by clicking the checkboxes in the leftmost column of the \"" + title + "\" panel.";
//        JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_CHARS_PER_LINE), "Error", JOptionPane.ERROR_MESSAGE);
//    }

    private JPanel drawKmerCoveragePlot(Map<String, Color> selectedKsList) throws Exception {
        Map<String, Color> map = new TreeMap<String, Color>();

        Set<String> keys = selectedKsList.keySet();
        for(String assemblyName : keys) {
            String path = assemblyName + File.separator + "coverage.hist";
            String assemblyPath = assemblyName;
            Color c = selectedKsList.get(assemblyPath);
            map.put(path, c);
        }

        if(map.size() > 0) {
            covplot = new CoverageHistogram();
            return covplot.drawPlot(map);
        }

        return null;
    }

    private JPanel drawN50plot(Map<String, Color> selectedKsList) throws Exception {
        lastPlotSettingsUsed = getPlotSettings();
        Map<String, Color> map = new TreeMap<String, Color>();
        Map<String, Integer> ks = new HashMap<String, Integer>();

        Set<String> keys = selectedKsList.keySet();
        for(String assemblyName : keys) {
            String path = assemblyName + File.separator + lastPlotSettingsUsed.fileName;
            String assemblyPath = assemblyName;
            Color c = selectedKsList.get(assemblyPath);
            map.put(path, c);
            ks.put(path, kValues.get(assemblyPath));
        }

        if(map.size() > 0) {
            n50plot = new N50plot(lastPlotSettingsUsed.minContigLength);
            return n50plot.drawPlot(map, ks, lastPlotSettingsUsed.logY, lastPlotSettingsUsed.xAxisInUnitOfLength, lastPlotSettingsUsed.unit, useFastaIndexCheckBox.isSelected()); // Scaffold length
        }

        return null;
    }

    private void packStatsSplitPane(){
        if(statsSplitPane.isVisible()){
            int fNumRows = fragmentStatsTable.getRowCount();
            int fRowHeight = fragmentStatsTable.getRowHeight();
            //int fRowMargin = fragmentSizeTable.getRowMargin();
            Insets fsInsets = fsScrollPane.getInsets();
            Insets fInsets = fragmentSizePanel.getInsets();

            int aNumRows = scaffoldStatsTable.getRowCount();
            int aRowHeight = scaffoldStatsTable.getRowHeight();
            //int aRowMargin = assembliesTable.getRowMargin();
            Insets asInsets = atScrollPane.getInsets();
            Insets aInsets = assembliesPanel.getInsets();

            Insets sInsets = statsSplitPane.getInsets();
            Dimension d = statsSplitPane.getMinimumSize();
            int dividerSize = statsSplitPane.getDividerSize();
            int maxHeightAvailable = assembliesAndSettingsSplitPane.getHeight();
            if(showPlotsToggleButton.isSelected()){
                maxHeightAvailable = maxHeightAvailable * 2/5;
            }

            int heightForFST = fRowHeight*fNumRows;
            if(fNumRows > 0){
                heightForFST += fsInsets.top + fsInsets.bottom + fInsets.top + fInsets.bottom + fragmentStatsTable.getTableHeader().getHeight();
            }
            int heightForAT = aRowHeight*aNumRows + asInsets.top + asInsets.bottom + aInsets.top + aInsets.bottom + scaffoldStatsTable.getTableHeader().getHeight();
            if(atScrollPane.getHorizontalScrollBar().isVisible()
                    || scaffoldStatsTable.getTableHeader().getWidth() >= statsSplitPane.getWidth()-atScrollPane.getVerticalScrollBar().getWidth()){
                heightForAT += atScrollPane.getHorizontalScrollBar().getHeight();
            }

            int proposedHeight = heightForFST + heightForAT + sInsets.top + sInsets.bottom + dividerSize;

            if(proposedHeight <= maxHeightAvailable){
                Dimension d2 = new Dimension(d.width,proposedHeight);
                statsSplitPane.setMinimumSize(d2);
                statsSplitPane.setPreferredSize(d2);
                displayPanel.validate();
                statsSplitPane.setDividerLocation(sInsets.top + heightForFST);
            }
            else{
                Dimension d2 = new Dimension(d.width,maxHeightAvailable);
                statsSplitPane.setMinimumSize(d2);
                statsSplitPane.setPreferredSize(d2);
                displayPanel.validate();
                statsSplitPane.setDividerLocation(Math.min(sInsets.top + heightForFST, maxHeightAvailable/2));
            }
            
            packFSTable();
            packATable();
        }
    }

    private void packFSTable(){
        statsSplitPane.validate();
        Insets fsInsets = fsScrollPane.getInsets();
        int fPad = 0;
        if(fsScrollPane.getVerticalScrollBar().isVisible()){
            fPad = fsScrollPane.getVerticalScrollBar().getWidth();
        }
        DefaultTableColumnModel colModel = (DefaultTableColumnModel)fragmentStatsTable.getColumnModel();
        int width = 0;
        int count = colModel.getColumnCount();
        for(int i=0; i< count; i++){
            TableColumn c = colModel.getColumn(i);
            width += c.getWidth();
        }

        fsScrollPane.setMinimumSize(new Dimension(width+fsInsets.left+fsInsets.right+fPad,1));
        fsScrollPane.setPreferredSize(new Dimension(width+fsInsets.left+fsInsets.right+fPad,1));
        statsSplitPane.validate();
//        Processor.packColumns(fragmentStatsTable, 2);
//        statsSplitPane.validate();
    }

    private void packATable(){
        statsSplitPane.validate();
        Insets asInsets = atScrollPane.getInsets();
        int aPad = 0;
        if(atScrollPane.getVerticalScrollBar().isVisible()){
            aPad = atScrollPane.getVerticalScrollBar().getWidth();
        }
        DefaultTableColumnModel colModel = (DefaultTableColumnModel)scaffoldStatsTable.getColumnModel();
        int width = 0;
        int count = colModel.getColumnCount();
        for(int i=0; i< count; i++){
            TableColumn c = colModel.getColumn(i);
            width += c.getWidth();
        }
        atScrollPane.setMinimumSize(new Dimension(width+asInsets.left+asInsets.right+aPad,1));
        atScrollPane.setPreferredSize(new Dimension(width+asInsets.left+asInsets.right+aPad,1));
        statsSplitPane.validate();
//        Processor.packColumns(scaffoldStatsTable, 2);
//        statsSplitPane.validate();
    }

    @Override
    public void validate(){
        super.validate();
        packStatsSplitPane();
    }

    private void initCustomComponents() {
        javax.swing.ToolTipManager.sharedInstance().setDismissDelay(10000);

        DefaultTableModel model = (DefaultTableModel) assembliesAndColorsTable.getModel();
        model.addTableModelListener(new TableModelListener(){
            private int lastRowCount = 0;
            
            public void tableChanged(TableModelEvent e){
                int currentRowCount = assembliesAndColorsTable.getRowCount();
                int type = e.getType();
                if(type == TableModelEvent.DELETE && currentRowCount == 0){
                    removeAssembliesMenuItem.setEnabled(false);
                    removeAssembliesButton.setEnabled(false);
//                    showPlotsToggleButton.setEnabled(false);
//                    showStatsToggleButton.setEnabled(false);
//                    showNavigatorToggleButton.setEnabled(false);
                }
                else if(type == TableModelEvent.INSERT && lastRowCount == 0){
                    removeAssembliesMenuItem.setEnabled(true);
                    removeAssembliesButton.setEnabled(true);
//                    showPlotsToggleButton.setEnabled(true);
//                    showStatsToggleButton.setEnabled(true);
//                    showNavigatorToggleButton.setEnabled(true);
                }
                lastRowCount = currentRowCount;
            }
        });

        Hashtable<Integer, JLabel> stepSizesDictionary = new Hashtable<Integer, JLabel>();
        for(int c=0;c<STEP_SIZES.length; c++){
            stepSizesDictionary.put(c, new JLabel(Integer.toString(STEP_SIZES[c])));
        }
        stepSizeSlider.setLabelTable(stepSizesDictionary);

        minContigLengthFormattedTextField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent de) {
                validateMCLTextField();
            }

            public void removeUpdate(DocumentEvent de) {
                validateMCLTextField();
            }

            public void changedUpdate(DocumentEvent de) {}
        });

        addSelectionListenerToAssembliesAndColorsTable();

        navigatorPanel = launchExplorer();
        navigatorPanel.setMinimumSize(new Dimension(1,1));
        navigatorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        navigatorPanel.setPreferredSize(new Dimension(10,10));
        navigatorPanel.setBorder(navBorder);

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = 1;
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 0.66;
        gridBagConstraints.weighty = 1.0;
        displayPanel.add(navigatorPanel, gridBagConstraints);

        statsSplitPane.setVisible(false);
        plotsPanel.setVisible(false);
        navigatorPanel.setVisible(false);

        navigatorPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                //explorerHasFocus();

                if(evt.getClickCount() == 2){
                    showPlotsToggleButton.setSelected(false);
                    showStatsToggleButton.setSelected(false);
                    displayPanel.repaint();
                }
            }
        });
        navigatorPanel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                clearNavigatorMenuItem.setEnabled(true);
            }
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                clearNavigatorMenuItem.setEnabled(false);
            }
        });

        addComponentsMouseListener(statsSplitPane, statsComponentsListener);
        addComponentsMouseListener(navigatorPanel, navigatorComponentsListener);
        explorer.addMouseListener(navigatorComponentsListener);

        navigatorPanel.validate();
        navigatorPanel.invalidate();



//        Set<String> orgs = blatLister.listAllOrg();
//        Vector<String> orgsList = new Vector<String>(orgs.size());
//        orgsList.addAll(orgs);
//        Collections.sort(orgsList);
//        DefaultComboBoxModel genomeModel = new DefaultComboBoxModel(orgsList);
//        genomeComboBox.setModel(genomeModel);
//        genomeComboBox.setSelectedItem("Human");



        scaffoldStatsTable.getColumnModel().addColumnModelListener(new TableColumnModelListener(){
            public void columnMarginChanged(ChangeEvent e)
            {
                packATable();
            }

            public void columnSelectionChanged(ListSelectionEvent e){}
            public void columnAdded(TableColumnModelEvent e){}
            public void columnMoved(TableColumnModelEvent e){}
            public void columnRemoved(TableColumnModelEvent e){}
        });

        fragmentStatsTable.getColumnModel().addColumnModelListener(new TableColumnModelListener(){
            public void columnMarginChanged(ChangeEvent e)
            {
                packFSTable();
            }

            public void columnSelectionChanged(ListSelectionEvent e){}
            public void columnAdded(TableColumnModelEvent e){}
            public void columnMoved(TableColumnModelEvent e){}
            public void columnRemoved(TableColumnModelEvent e){}
        });
    }

    public int[] getStepSizeDictionary(){
        return STEP_SIZES;
    }

    private void validateMCLTextField(){

        if(minContigLengthFormattedTextField.getText().equals(minContigLengthFormattedTextField.getValue().toString())){
            minContigLengthFormattedTextField.setBackground(Color.WHITE);
        }
        else if(minContigLengthFormattedTextField.isEditValid()){
            try{
                minContigLengthFormattedTextField.setBackground(Color.WHITE);
                minContigLengthFormattedTextField.commitEdit();
                validateSettings();
            }
            catch(java.text.ParseException e){
                minContigLengthFormattedTextField.setBackground(new Color(251, 154, 153));
            }
        }
    }

    public void checkCurrentDirectory() throws InterruptedException {
        String dir = System.getProperty("user.dir");
        File userDir = new File(dir);
        fc.setCurrentDirectory(userDir);

        if(Utilities.isAbyssAssemblyDirectory(userDir) > 0){
            addAssemblies(new File[]{userDir});
        }
    }


    private void addSelectionListenerToAssembliesAndColorsTable() {
        selectionListener = new javax.swing.event.ListSelectionListener(){
            public void valueChanged(javax.swing.event.ListSelectionEvent lse) {
                validateSettings();
                validateViewButtonsStatus();
            }
        };
        assembliesAndColorsTable.getSelectionModel().addListSelectionListener(selectionListener);
    }


    private void loadDirectory() throws InterruptedException {
        if(singleFileLoaded && explorer != null && !explorer.isClear()){
            String msg = "File would be unloaded from Navigator. Would you like to continue?";
            int response = JOptionPane.showConfirmDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Confirm unloading file", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(response == JOptionPane.YES_OPTION){
                singleFileLoaded = false;
                assembliesAndColorsTable.setEnabled(true);
                sequenceSearchRadioButtonMenuItem.setEnabled(true);

                explorer.clear();
                navLabel.setText("NAVIGATOR");
                navigatorPanel.setVisible(false);
            }
            else{
                return;
            }
        }

        fc.setFileSelectionMode(javax.swing.JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(true);
        fc.setDialogTitle(Dive.TITLE_FOR_ADDING_ASSEMBLIES);
        int returnVal = fc.showOpenDialog(this);

        if(returnVal == JFileChooser.APPROVE_OPTION) {
            addAssemblies(fc.getSelectedFiles());
        }
    }

//    public void validateDirectory2(File theFile){
//        kValues = new HashMap<String, Integer>();
//        File abyssDir = null;
//        String dirToSelect = null;
//        String fileToExplore = null;
//
//        if(theFile.isDirectory()){
//            abyssDir = theFile;
//        }
//        else{ // a file
//            abyssDir = theFile.getParentFile();
//            fileToExplore = theFile.getName();
//        }
//
//        if(Utilities.isAbyssAssemblyDirectory(abyssDir) > 0){
//            dirToSelect = abyssDir.getName();
//            abyssDir = abyssDir.getParentFile();
//            // select this dir in the k dir list later
//        }
//
//        File[] allFiles = abyssDir.listFiles();
//        if(allFiles != null && allFiles.length > 0) {
//            LinkedList<File> assemblies = new LinkedList<File>();
//
//            for(File aFile : allFiles){
//                if(aFile.isDirectory()){
//                    String aFileName = aFile.getName();
//                    if(aFileName.charAt(0) != '.' && !aFileName.equals("best")){
//                        int k = Utilities.isAbyssAssemblyDirectory(aFile);
//
//                        if(k > 0){ // found k from the dot file
//                            assemblies.add(aFile);
//                            kValues.put(aFileName, k);
//                        }
//                    }
//                }
//            }
//
//            workingDir = abyssDir.getAbsolutePath();
//            fc.setCurrentDirectory(abyssDir);
//
//                String regex = File.separator;
//                if(regex.equals("\\")){
//                    regex += "\\";
//                }
//                String[] dirs = workingDir.split(regex);
//
//                String title = DEFAULT_TITLE;
//                if(dirs.length >= 2){
//                    title = dirs[dirs.length-2] + '/' + dirs[dirs.length-1] + " - " + title;
//                }
//
//                setTitle(title);
//
//
//                DefaultComboBoxModel model = new DefaultComboBoxModel();
//                filesComboBox.setModel(model);
//
//                for(File f : assemblies) {
//                    File[] files = f.listFiles(new FastaAndAdjFileFilter());
//                    for(File file : files) {
//                        String name = file.getName();
//                        if(model.getIndexOf(name) < 0) {
//                            model.addElement(name);
//                            if(name.endsWith("-contigs.fa")) {
//                                model.setSelectedItem(name);
//                            }
//                        }
//                    }
//                }
//
//                if(filesComboBox.getSelectedIndex() < 0 && model.getSize() > 0)
//                    filesComboBox.setSelectedIndex(0);
//
//            fillKDirTableInNewThread(dirToSelect); // TODO
//            if(dirToSelect != null && dirToSelect.equals("best")){
//                dirToSelect = bestAssembly;
//            }
//
//            if(fileToExplore != null && (fileToExplore.endsWith(".adj") || fileToExplore.endsWith(".dot"))){
//                try {
//                    explore(dirToSelect, fileToExplore);
//                    explorerPanel.setVisible(true);
//                    lastPlotSettingsUsed = this.getPlotSettings();
//                    showExplorerToggleButton.setSelected(true);
//                    explorerHasFocus();
//                } catch (Exception ex) {
//                    ex.printStackTrace();
//                }
//            }
//            else{
//                showExplorerToggleButton.setSelected(false);
//                lastPlotSettingsUsed = null;
//            }
//
//            ready = false;
//            showStatsToggleButton.setSelected(false);
//            showPlotsToggleButton.setSelected(false);
//            showStatsToggleButton.setEnabled(true);
//            showPlotsToggleButton.setEnabled(true);
//            showExplorerToggleButton.setEnabled(true);
//
//            applyButton.setEnabled(false);
//            enableSettingsComponents(true);
//        }
//    }

    private void fillAssembliesAndColorsTable(String kName){// throws InterruptedException{
//        statsSplitPane.setVisible(false);
//        plotsTabbedPane.setVisible(false);
//
//        maxN50Index = -1;
//        maxContiguityIndex = -1;
//        maxReconstructionIndex = -1;
//        maxSpanIndex = -1;
//
//        disabledRowIndexesForSelectedFile.clear();
//        disabledRowIndexesForCoverageFile.clear();

//        DefaultTableModel fsTableModel = (DefaultTableModel) fragmentSizeTable.getModel();
//        for(int i=fsTableModel.getRowCount()-1; i>=0; i--){
//            fsTableModel.removeRow(i);
//        }
//
//        DefaultTableModel aTableModel = (DefaultTableModel) assembliesTable.getModel();
//        for(int i=aTableModel.getRowCount()-1; i>=0; i--){
//            aTableModel.removeRow(i);
//        }
//
//        plotsTabbedPane.removeAll();
//
        DefaultTableModel model = (DefaultTableModel) assembliesAndColorsTable.getModel();
        for(int i=model.getRowCount()-1; i>=0; i--){
            model.removeRow(i);
        }

        if(lastPlotSettingsUsed != null){
            lastPlotSettingsUsed.selectedKs = new int[0];
        }
//
//        if(explorerPanel != null){
//            explorerPanel.setVisible(false);
//        }
//
//        if(explorer!=null){
//            explorer.clear();
//        }


//        ArrayList<String> assemblies = new ArrayList<String>();

        Set<String> paths = kValues.keySet();
        sharedAncestorPath = null;
        String longestCommonPrefix = Utilities.getLongestCommonPrefix(paths);

        int trim = longestCommonPrefix.lastIndexOf(File.separator);
        if(trim > 0){
            sharedAncestorPath = longestCommonPrefix.substring(0, trim);
        }

        int indexToCut = 0;
        if(sharedAncestorPath != null){
            System.out.println("Path shared by assemblies loaded: " + sharedAncestorPath);
            indexToCut = sharedAncestorPath.length()+1;
            setTitle(sharedAncestorPath + " - " + DEFAULT_TITLE);
        }
        else{
            System.out.println("No path shared by assemblies loaded.");
            setTitle(DEFAULT_TITLE);
        }

        if(kName != null){
           kName =  kName.substring(indexToCut);
        }

        ArrayList<String> assemblies = new ArrayList<String>(kValues.keySet());
        Collections.sort(assemblies);

        for(String assembly : assemblies){
            Object[] row = new Object[2];

            if(chartColors.containsKey(assembly)){
                row[0] = chartColors.get(assembly);
            }
            else{
                Color c = colorFactory.getNextColor();
                chartColors.put(assembly, c);
                row[0] = c;
            }

            row[1] = assembly.substring(indexToCut);
            model.addRow(row);
        }

        removeAssembliesMenuItem.setEnabled(true);
        removeAssembliesButton.setEnabled(true);

        if(kName != null){
            //select that k
            ListSelectionModel sModel = assembliesAndColorsTable.getSelectionModel();
            int rCount = assembliesAndColorsTable.getRowCount();
            for(int i=0; i<rCount; i++){
                Object row = assembliesAndColorsTable.getValueAt(i, 1);
                if(row != null){
                    if(kName.equals((String)row)){
                        sModel.setSelectionInterval(i, i);
                        break;
                    }
                }
            }
        }
    }

    private HashMap<String, Color> chartColors = new HashMap<String, Color>();
    private ColorFactory colorFactory = new ColorFactory();
    private class ColorFactory{
        private int i = 0;
        private Color[] colors = MyDrawingSupplier.PAINT_SEQ_FOR_WHITE_BG;

        public Color getNextColor(){
            Color color = colors[i];
            if(i >= colors.length-1){
                i = 0;
            }
            else{
                i++;
            }
            return color;
        }

        public void reset(){
            i = 0;
        }
    }

//    private String getBestAssemblyPath() throws InterruptedException{
////        Set<String> keys = new HashSet<String>(kDirPathList.size());
////        for(String path : kDirPathList){
////            keys.add( Utilities.getAssemblyNameFromPath(path) );
////        }
//        Set<String> keys = kValues.keySet();
//
//        Long defaultMCL = 100L;
//        if(!minContigLengthFormattedTextField.getValue().equals(defaultMCL)){
//            minContigLengthFormattedTextField.setValue(defaultMCL);
//        }
//
//        if(!((String)filesComboBox.getSelectedItem()).endsWith("-contigs.fa")){
//            DefaultComboBoxModel model = (DefaultComboBoxModel) filesComboBox.getModel();
//            int numItems = model.getSize();
//            for(int i=0; i< numItems; i++){
//                String item = (String)model.getElementAt(i);
//                if(item.endsWith("-contigs.fa")){
//                    model.setSelectedItem(item);
//                }
//            }
//        }
//
//        if(!unitOfLengthComboBox.getSelectedItem().equals("bp")){
//            unitOfLengthComboBox.setSelectedItem("bp");
//        }
//
//        fillAssembliesTable3(keys, false);
//
//        bestAssembly = (String) assembliesTable.getValueAt(assembliesTable.convertRowIndexToView(maxContiguityIndex), 0);
//
//        fillFragmentSizeTable();
//
//        packStatsSplitPane();
//
//        showStatsToggleButton.setSelected(true);
//
//        return workingDir + File.separator + bestAssembly;
//    }

    private void fillAssembliesAndColorsTableInNewThread(final String kName) {
        SwingWorker worker = new SwingWorker<Void, Void>() {
            public Void doInBackground() throws Exception {
                Long start = System.currentTimeMillis();
                try{
                    statusLabel.setForeground(Color.BLACK);
                    stopButton.setEnabled(true);
                    busyCursor();
                    enableLocateDirComponents(false);
                    applyButton.setEnabled(false);
                    fillAssembliesAndColorsTable(kName);
                    String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                    setStatus(doneText, true, NORMAL_STATUS);
                }
//                catch(InterruptedException ex){
//                    String doneText = "Stopped by user.";
//                    System.out.println(doneText);
//                    statusLabel.setForeground(Color.RED);
//                    statusLabel.setText(doneText);
//                }
                catch(Exception ex){
                    ex.printStackTrace();
                }
                finally{
                    stopButton.setEnabled(false);
                    enableLocateDirComponents(true);
                    defaultCursor();
                }
                return null;
            }
        };
        workers.add(worker);
        worker.execute();
        if(worker.isDone()){
            workers.remove(worker);
        }
    }

    private void enableLocateDirComponents(boolean enabled) {
        addAssembliesMenuItem.setEnabled(enabled);
        loadAdjDotButton.setEnabled(enabled);
    }

    private void enableSettingsComponents(boolean enabled) {
        enableAdvancedSettingsComponents(enabled);
        enableStatsPlotsSettingsComponents(enabled);
        enableNavigatorSettingsComponents(enabled);
    }

    private void enableAdvancedSettingsComponents(boolean enabled) {
        useFastaIndexCheckBox.setEnabled(enabled);
        filesComboBox.setEnabled(enabled);
        unitOfLengthComboBox.setEnabled(enabled);
    }

    private void enableStatsPlotsSettingsComponents(boolean enabled){
        minContigLengthFormattedTextField.setEnabled(enabled);
        drawN50plotCheckBox.setEnabled(enabled);
        drawCovPlotCheckBox.setEnabled(enabled);
        drawFragSizeDistCheckBox.setEnabled(enabled);
        if(enabled){
            boolean drawN50plot = drawN50plotCheckBox.isSelected();
            yAxisUnitComboBox.setEnabled(drawN50plot);
            xAxisScaleComboBox.setEnabled(drawN50plot);            
            boolean drawFragSizeDist = drawFragSizeDistCheckBox.isSelected();
            selectLibrariesButton.setEnabled(drawFragSizeDist);
        }
        else{
            yAxisUnitComboBox.setEnabled(false);
            xAxisScaleComboBox.setEnabled(false);
            selectLibrariesButton.setEnabled(false);
        }
    }

    private void enableNavigatorSettingsComponents(boolean enabled){
        showLengthCheckBox.setEnabled(enabled);
        lengthSlider.setEnabled(enabled);
        showLabelsCheckBox.setEnabled(enabled);
        showAllRadioButton.setEnabled(enabled);
        showNeighborsRadioButton.setEnabled(enabled);

        stepSizeLabel.setEnabled(enabled);
        stepSizeSlider.setEnabled(enabled);

//        if(!singleFileLoaded){
            scaffoldRadioButton.setEnabled(enabled);
            peRadioButton.setEnabled(enabled);
            seRadioButton.setEnabled(enabled);

//            if(enabled && seRadioButton.isSelected()){
//                showPEContigsCheckBox.setEnabled(true);
//                showPEPartnersCheckBox.setEnabled(true);
//            }
//            else{
//                showPEContigsCheckBox.setEnabled(false);
//                showPEPartnersCheckBox.setEnabled(false);
//            }
//        }
    }


    private void fillScaffoldStatsTable(Set<String> selectedKsSet, boolean getStatsFromPlots) throws InterruptedException{
        //assembly | n50 | contiguity | reconstruction | span | median k-mer coverage | n | n(l>=MCL) | n (l>=N50)
        DefaultTableModel model = (DefaultTableModel) scaffoldStatsTable.getModel();

        if(hiddenColumn != null){
            TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
            tcm.addColumn(hiddenColumn);
            tcm.moveColumn(8, 4);
            hiddenColumn = null;

            Border border = assembliesPanel.getBorder();
            if(border instanceof TitledBorder) {
                TitledBorder tborder = (TitledBorder) border;
                tborder.setTitle("Scaffold Sizes");
                assembliesPanel.repaint();
            }
        }

        maxN50Index = -1;
        maxContiguityIndex = -1;
        maxReconstructionIndex = -1;
        maxSpanIndex = -1;

        disabledRowIndexesForSelectedFile.clear();
        disabledRowIndexesForCoverageFile.clear();

        for(int i=model.getRowCount()-1; i>=0; i--){
            model.removeRow(i);
        }

        Long mcl = (Long) minContigLengthFormattedTextField.getValue();
        scaffoldStatsTable.getColumnModel().getColumn(7).setHeaderValue("n (l>="+mcl.toString()+" bp)");

        String unitStr = (String) unitOfLengthComboBox.getSelectedItem();
        int unit = -1;
        if(unitStr != null){
            if(unitStr.equals("k-mer")) {
                unit = N50stats.KMER;
                TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
                tcm.getColumn(1).setHeaderValue("N50 (k-mer)");
                tcm.getColumn(2).setHeaderValue("Contiguity (k-mer)");
                tcm.getColumn(3).setHeaderValue("Reconstruction (k-mer)");
                tcm.getColumn(4).setHeaderValue("Span (k-mer)");
            }
//            else if(unitStr.equals("nr-k-mer")) {
//                unit = N50stats.NR_KMER;
//                TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
//                tcm.getColumn(1).setHeaderValue("N50 (nr-k-mer)");
//                tcm.getColumn(2).setHeaderValue("Contiguity (nr-k-mer)");
//                tcm.getColumn(3).setHeaderValue("Reconstruction (nr-k-mer)");
//                tcm.getColumn(4).setHeaderValue("Span (nr-k-mer)");
//            }
            else if(unitStr.equals("nol-bp")) {
                unit = N50stats.NOLBP;
                TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
                tcm.getColumn(1).setHeaderValue("N50 (nol-bp)");
                tcm.getColumn(2).setHeaderValue("Contiguity (nol-bp)");
                tcm.getColumn(3).setHeaderValue("Reconstruction (nol-bp)");
                tcm.getColumn(4).setHeaderValue("Span (nol-bp)");
            }
            else {
                unit = N50stats.BP;
                TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
                tcm.getColumn(1).setHeaderValue("N50 (bp)");
                tcm.getColumn(2).setHeaderValue("Contiguity (bp)");
                tcm.getColumn(3).setHeaderValue("Reconstruction (bp)");
                tcm.getColumn(4).setHeaderValue("Span (bp)");
            }
            scaffoldStatsTable.getTableHeader().repaint();
        }


        long maxN50 = -1;
        long maxContiguity = -1;
        long maxReconstruction = -1;
        long maxSpan = -1;

        String fileName = (String) filesComboBox.getSelectedItem();
        //boolean scaffold = false; //scaffoldLengthRadioButton.isSelected();

        boolean spanRedundant = true;

        int indexToCut = 0;
        if(sharedAncestorPath != null){
            indexToCut = sharedAncestorPath.length()+1;
        }

        int index = 0;
        for(String assembly : selectedKsSet){
            try {

                Object[] row = new Object[9];
                row[0] = assembly.substring(indexToCut);

                    String fastaFile = assembly + File.separator + fileName;
                    if(fileName != null && Utilities.fileExist(fastaFile) ) {
                        N50stats stats = null;

                        if(getStatsFromPlots && n50plot != null){
                            stats = n50plot.getStats(fastaFile);
                        }
                        else{
                            stats = new N50stats(mcl);
                            setStatus("Reading File: " + fastaFile, true, NORMAL_STATUS);
                            stats.readFile(fastaFile, kValues.get(assembly), unit, useFastaIndexCheckBox.isSelected());
                        }

                        long n50 = stats.getN50();
                        long contiguity = stats.getContiguity();
                        long reconstruction = stats.getReconstruction();
                        long span = stats.getSpan();
                        if(span <= 0){
                            span = reconstruction;
                        }

                        if(span != reconstruction){
                            spanRedundant = false;
                        }

                        row[1] = n50;
                        row[2] = contiguity;
                        row[3] = reconstruction;
                        row[4] = span;
                        row[6] = stats.getNumContigs();
                        row[7] = stats.getNumContigsLongerThanOrEqualToMinContigLength();
                        row[8] = stats.getNumContigsLongerThanOrEqualToN50();

                        if(n50>maxN50)
                        {
                            maxN50 = n50;
                            maxN50Index = index;
                            scaffoldStatsTable.repaint();
                        }
                        if(contiguity > maxContiguity)
                        {
                            bestInSelection = assembly;
                            maxContiguity = contiguity;
                            maxContiguityIndex = index;
                            scaffoldStatsTable.repaint();
                        }
                        if(reconstruction > maxReconstruction)
                        {
                            maxReconstruction = reconstruction;
                            maxReconstructionIndex = index;
                            scaffoldStatsTable.repaint();
                        }
                        if(span > maxSpan)
                        {
                            maxSpan = span;
                            maxSpanIndex = index;
                            scaffoldStatsTable.repaint();
                        }
                    }
                    else{
                        if(fileName != null){ // a file name was seleted in the combobox but it does not exist in the assembly directory
                            setStatus("Could not find '" + fastaFile + "'", true, ERROR_STATUS);
                        }
                        disabledRowIndexesForSelectedFile.add(index);
                    }


                String coverageHistogramFile = assembly + File.separator + "coverage.hist";
                    if(Utilities.fileExist(coverageHistogramFile)) {
                        if(getStatsFromPlots && covplot != null){
                            row[5] = covplot.getMedianCoverage(coverageHistogramFile);
                        }
                        else {
                            setStatus("Reading File: " + coverageHistogramFile, true, NORMAL_STATUS);
                            row[5] = CoverageStats.getMedianKmerCoverage(coverageHistogramFile);
                        }
                    }
                    else{
                        disabledRowIndexesForCoverageFile.add(index);
                    }

                model.addRow(row);
                index++;

            } catch (InterruptedException ex){
                throw ex;
            } catch (FileNotFoundException ex) {
                System.out.println("FileNotFoundException: " + ex.getMessage());
            } catch (IOException ex) {
                System.out.println("IOException: " + ex.getMessage());
            } catch (Exception ex){
                System.out.println("Exception: " + ex.getMessage());
            }
        }

        if(spanRedundant){
            //System.out.println("Span column is redundant.");
            TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
            hiddenColumn = tcm.getColumn(4);
            tcm.removeColumn(hiddenColumn);

            Border border = assembliesPanel.getBorder();
            if(border instanceof TitledBorder) {
                TitledBorder tborder = (TitledBorder) border;
                tborder.setTitle("Contig Sizes");
                //assembliesPanel.repaint();
            }
        }

        //TODO: Figure out why the program freezes here occaisionally.
        Processor.packColumns(scaffoldStatsTable, 2);

//        System.out.println(Processor.getAllValuesFromTable(assembliesTable));
        //lastPlotSettingsUsed = getPlotSettings();
    }

    private Map<String, Color> getSelectedAssemblies(){
        int[] rows = assembliesAndColorsTable.getSelectedRows();
        Map<String, Color> map = new TreeMap<String, Color>();

        String prefix = "";
        if(sharedAncestorPath != null){
            prefix = sharedAncestorPath + File.separator;
        }

        for(int r : rows){
            map.put(prefix + (String)assembliesAndColorsTable.getValueAt(r, 1),(Color)assembliesAndColorsTable.getValueAt(r, 0));
        }
        return map;
    }

    public void busyCursor() {
        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void defaultCursor() {
        this.setCursor(Cursor.getDefaultCursor());
    }

    private static String about() {
        String aboutText = version + " (Build " + serialVersionUID + ")"
                +"\nWritten by Ka Ming Nip and Cydney Nielsen.\nReport bugs to <kmnip@bcgsc.ca>\nCopyright 2011 Canada's Michael Smith Genome Sciences Centre, BC Cancer\n\n"
                + "Java: " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + "\n"
                + "System: " + System.getProperty("os.name") + " version " + System.getProperty("os.version") + " running on " + System.getProperty("os.arch") + "\n";

        // may want to print the user's home directory here if an external file is needed for user preferences and a history of recently opened files

        return aboutText;
    }
    
    private static String abouthtml() {
        String aboutText = 
                "<html>"
                + "<p>"
                + "<font size=\"3\" face=\"verdana\">"
                + version + "<br />"
                + "<br />"
                + "Written by Ka Ming Nip and Cydney Nielsen.<br />"
                + "Please report bugs to <a href=\"mailto:kmnip@bcgsc.ca\">kmnip@bcgsc.ca</a>.<br />"
                + "Copyright 2011 Canada's Michael Smith Genome Sciences Centre, BC Cancer.<br />"
                + "<br />"
                + "</font>"
                + "</p>"
                + "<hr />"
                + "<p>"
                + "<font size=\"3\" face=\"verdana\">"
                + "<b>Product Version:</b> " + version + " (Build " + serialVersionUID + ")<br />"
                + "<b>Java:</b> " + System.getProperty("java.vm.name") + " " + System.getProperty("java.vm.version") + "<br />"
                + "<b>System:</b> " + System.getProperty("os.name") + " version " + System.getProperty("os.version") + " running on " + System.getProperty("os.arch") + "<br />"
                + "</font>"
                + "</p>"
                + "</html>";

        // may want to print the user's home directory here if an external file is needed for user preferences and a history of recently opened files

        return aboutText;
    }     

    public void help() throws IOException {
        InputStream is = getClass().getResourceAsStream("/ca/bcgsc/dive/help/help.txt");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
        br.close();
        isr.close();
        is.close();
        System.exit(0);
    }

    class MyTabbedPaneUI extends MetalTabbedPaneUI{
        @Override
        protected int calculateTabAreaHeight(int tabPlacement, int horizRunCount, int maxTabHeight) {
            if (showTabsHeader) {
                 return super.calculateTabAreaHeight(tabPlacement, horizRunCount, maxTabHeight);
            }
            else {
                 return 0;
            }
        }
        @Override
        protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex){
            if (showTabsHeader) {
                super.paintContentBorder(g, tabPlacement, selectedIndex);
            }
        }
        @Override
        protected void paintTabArea(Graphics g, int tabPlacement, int selectedIndex){
            if (showTabsHeader) {
                super.paintTabArea(g, tabPlacement, selectedIndex);
            }
        }
    }

    private void setTabComponentsVisible(boolean show){
        int tabCount = plotsTabbedPane.getTabCount();
        for(int i=0; i< tabCount; i++){
            Component c = plotsTabbedPane.getTabComponentAt(i);
            if(c != null){
                c.setVisible(show);
            }
        }
    }

    private void addAssemblies(File[] selectedFiles) throws InterruptedException{
        if(kValues == null){
            kValues = new HashMap<String, Integer>();
        }

        ArrayList<File> selectedDirectories = new ArrayList<File>();
        for(File f : selectedFiles){
            if(f.exists()){
                selectedDirectories.add(f);
            }
            else{
                System.out.println("ERROR: Could not find '" + f.getPath() + "'");
            }
        }

        HashMap<String, Integer> validDirs = null;
        if(selectedDirectories.isEmpty() /*&& fileForNavigator == null*/){
            String msg = "The directories selected do not exist. No assemblies were added.";
            System.out.println("ERROR: " + msg);
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        validDirs = validateSelectedDirectories(selectedDirectories);
        if(validDirs == null || validDirs.isEmpty() /*&& fileForNavigator == null*/){
            String msg = "The directories selected are either not valid or have been loaded previously. No assemblies were added.";
            System.out.println("ERROR: " + msg);
            JOptionPane.showMessageDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        kValues.putAll(validDirs);

        int numAssemblies = validDirs.size();
        String msg = null;
        if(numAssemblies == 1){
            msg = numAssemblies + " assembly was added.";
        }
        else{        
            msg = numAssemblies + " assemblies were added.";
        }
        setStatus(msg, true, NORMAL_STATUS);

        Set<String> paths = kValues.keySet();

        fillAssembliesAndColorsTable(null);

        if(!resetMenuItem.isEnabled()){
            resetMenuItem.setEnabled(true);
        }

        DefaultComboBoxModel model = new DefaultComboBoxModel();
        filesComboBox.setModel(model);
        String[] suffixes = {"-scaffolds.fa",
                             "-" + SCAFFOLDS + ".fa",
                             "-" + SCAFFOLDS + ".dot",
                             "-" + SCAFFOLDS + ".adj",
                             "-contigs.fa",
                             "-" + CONTIGS + ".fa",
                             "-" + CONTIGS + ".dot",
                             "-" + CONTIGS + ".adj",
                             "-unitigs.fa",
                             "-" + UNITIGS +".fa",
                             "-" + UNITIGS +".dot",
                             "-" + UNITIGS +".adj"
                            };
        
        for(String p : paths) {
            File f = new File(p);
            File[] files = f.listFiles(new FastaAndAdjFileFilter());
            for(String suffix : suffixes){
                for(File file : files) {
                    String name = file.getName();
                    if(name.endsWith(suffix) && model.getIndexOf(name) < 0) {
                        model.addElement(name);
                        model.setSelectedItem(name);
                    }
                }
            }

            File[] fsdFiles = f.listFiles(new FileNameRegexFilter(".*-" + UNITIGS + "\\.hist"));
            for(File file : fsdFiles){
                String name = file.getName();
                if(!fsdFileNames.contains(name)){
                    fsdFileNames.add(name);
                    JCheckBox cb = new JCheckBox(name.substring(0, name.length()-7)); // library name is file name with "-3.hist" trimmed
                    cb.setSelected(true);
                    addListenerForLibraryCheckBox(cb);
                    libsPopupMenu.add(cb); // JCheckBox was used instead of JCheckBoxMenuItem because mouse clicks on JCheckBox do not hide the popupmenu. Therefore, multiple checkboxes can be (un)checked.
                    libraryCheckBoxes.add(cb);
                }
            }
        }
        if(filesComboBox.getSelectedIndex() < 0 && model.getSize() > 0)
            filesComboBox.setSelectedIndex(0);

        ready = false;
//        showStatsToggleButton.setEnabled(true);
//        showPlotsToggleButton.setEnabled(true);
//        showNavigatorToggleButton.setEnabled(true);
        this.validateViewButtonsStatus();
        applyButton.setEnabled(false);
        enableSettingsComponents(true);
        fresh = false;
    }

    private HashMap<String, Integer> validateSelectedDirectories(ArrayList<File> selectedFiles) throws InterruptedException{
        HashMap<String, Integer> selectedAssembliesAndK = new HashMap<String, Integer>();
        for(File f : selectedFiles){
            int k = Utilities.isAbyssAssemblyDirectory(f);
            if(k == -1){
                HashMap<File, Integer> abyssAssembliesKMap = new HashMap<File, Integer>();
                for(File child : f.listFiles()){
                    int childK = Utilities.isAbyssAssemblyDirectory(child);
                    if(childK > 0){
                        abyssAssembliesKMap.put(child, childK);
//                        if(!isLoaded(child)){
//                            //store this k
//                            selectedAssembliesAndK.put(child.getAbsolutePath(), childK);
//                        }
                    }
                }

                Set<File> abyssAssemblies = abyssAssembliesKMap.keySet();
                for(File aa : abyssAssemblies){
                    if(Utilities.isSymbolicLink(aa)){
                        File ef = Utilities.equivalentFile(aa, abyssAssemblies);
                        if(ef != null){
                            System.out.println("WARNING: '" + aa.getAbsolutePath() + "' is not loaded because '" + ef.getAbsolutePath() + "' is an equivalent assembly.");
                            continue;
                        }
                    }
                    else if(!isLoaded(aa)){
                            //store this k
                        selectedAssembliesAndK.put(aa.getAbsolutePath(), abyssAssembliesKMap.get(aa));
                    }
                }
            }
            else{
                if(!isLoaded(f)){
                    //store this k
                    selectedAssembliesAndK.put(f.getAbsolutePath(), k);
                }
            }
        }
        return selectedAssembliesAndK;
    }

    private boolean isLoaded(File f){
        if(loadedAssembliesMap == null){
            loadedAssembliesMap = new HashMap<String, String>();
        }

        Set<String> cpathsOfLoadedAssemblies = loadedAssembliesMap.keySet();
        try {
            String cpath = f.getCanonicalPath();
            boolean isLoaded = cpathsOfLoadedAssemblies.contains(cpath);
            String abspath = f.getAbsolutePath();
            if(!isLoaded){
                loadedAssembliesMap.put(cpath, abspath);
            }
            else{
                System.out.println("WARNING: '" + f.getName() + "' was not loaded because the same assembly at '" + loadedAssembliesMap.get(cpath) + "' was loaded previously." );
            }

            return isLoaded;
        } catch (IOException ex) {
            return false;
        }
    }

    private void clearAll(){
            // remove all assemblies loaded
            DefaultTableModel model = (DefaultTableModel) assembliesAndColorsTable.getModel();
            for(int i=model.getRowCount()-1; i>=0; i--){
                model.removeRow(i);
            }
            chartColors.clear();
            colorFactory.reset();
            resetMenuItem.setEnabled(false);
            sharedAncestorPath = null;
            kValues = null;
            loadedAssembliesMap = new HashMap<String, String>();
            bestInSelection = null;
            this.setTitle(DEFAULT_TITLE);

            // remove all library choices
            removeUselessLibraries();
            this.drawN50plotCheckBox.setSelected(true);
            this.drawCovPlotCheckBox.setSelected(true);
            this.drawFragSizeDistCheckBox.setSelected(false);

            // clear Navigator
            explorer.clear();
            navLabel.setText("NAVIGATOR");
            navigatorPanel.setVisible(false);
            showNavigatorToggleButton.setSelected(false);
            showNavigatorToggleButton.setEnabled(false);

            // clear Plots
            plotsTabbedPane.removeAll();
            showPlotsToggleButton.setSelected(false);
            showPlotsToggleButton.setEnabled(false);
            plotsForNavigator = false;
            plotsSplitPane = null;
            n50plot = null;
            covplot = null;

            // clear stats
            int fstRowCount = fragmentStatsTable.getRowCount();
            int atRowCount = scaffoldStatsTable.getRowCount();
//            if(fstRowCount > 0 || atRowCount > 0){
                showStatsToggleButton.setSelected(false);
                showStatsToggleButton.setEnabled(false);

                DefaultTableModel modelF = (DefaultTableModel) fragmentStatsTable.getModel();
                for(int i=fstRowCount-1; i>=0; i--){
                    modelF.removeRow(i);
                }

                disabledRowIndexesForSelectedFile.clear();
                disabledRowIndexesForCoverageFile.clear();

                DefaultTableModel modelA = (DefaultTableModel) scaffoldStatsTable.getModel();
                for(int i=atRowCount-1; i>=0; i--){
                    modelA.removeRow(i);
                }

                if(hiddenColumn != null){
                    TableColumnModel tcm = scaffoldStatsTable.getColumnModel();
                    tcm.addColumn(hiddenColumn);
                    tcm.moveColumn(8, 4);
                    hiddenColumn = null;

                    Border border = assembliesPanel.getBorder();
                    if(border instanceof TitledBorder) {
                        TitledBorder tborder = (TitledBorder) border;
                        tborder.setTitle("Scaffold Sizes");
                        assembliesPanel.repaint();
                    }
                }
//            }
//            statsForNavigator = false;
            maxN50Index = -1;
            maxContiguityIndex = -1;
            maxReconstructionIndex = -1;
            maxSpanIndex = -1;

            //clear search
            contigIds = new HashSet<String>();
            unitigIds = new HashSet<String>();
            lastQuery = null;
            currentQuery = null;
            if(navigatorSearchRadioButtonMenuItem.isSelected()){
                contigSearchTextField.setText(NAVIGATOR_SEARCH_QUERY_FORMAT);
            }
            else{
                contigSearchTextField.setText(SEQUENCE_SEARCH_QUERY_FORMAT);
            }
            backButton.setEnabled(false);

            //clear settings
            lastPlotSettingsUsed = null;
            ready = false;
            enableSettingsComponents(false);
            applyButton.setEnabled(false);
            fresh = true;

            //clear status bar
            setStatus(null, false, NORMAL_STATUS);
    }

    private void loadAdjDotFile(){
        if(!fresh){
            //prompt user

            String msg = "All contents displayed would be cleared. Would you like to continue?";
            int response = JOptionPane.showConfirmDialog(this, Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG), "Confirm unloading file", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if(response == JOptionPane.YES_OPTION){
                clearAll();
            }
            else{
                return;
            }
        }

        fc.setFileSelectionMode(javax.swing.JFileChooser.FILES_ONLY);
        fc.setFileFilter(adjDotFileNameExtensionFilter);
        fc.setMultiSelectionEnabled(false);
        fc.setDialogTitle(Dive.TITLE_FOR_LOADING_FILE);
        int returnVal = fc.showOpenDialog(this);
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            SwingWorker worker = new SwingWorker<Void, Void>() {
                public Void doInBackground(){
                    long start = System.currentTimeMillis();
                    statusLabel.setForeground(Color.BLACK);
                    stopButton.setEnabled(true);
                    busyCursor();
                    enableLocateDirComponents(false);
                    enableSettingsComponents(false);
                    try{

                        validateSelectedFile(fc.getSelectedFile());

                        String doneText = "Done. Elapsed Time: " + (System.currentTimeMillis()-start)/1000.0 + " seconds.";
                        setStatus(doneText, true, NORMAL_STATUS);
                    }
                    catch(InterruptedException ex){
                        String doneText = "Stopped by user.";
                        setStatus(doneText, true, ERROR_STATUS);
                    }
                    catch(Exception ex){
                        ex.printStackTrace();
                        setStatus(ex.getMessage(), true, ERROR_STATUS);
                    }
                    finally{
                        stopButton.setEnabled(false);
                        enableLocateDirComponents(true);
                        if(singleFileLoaded){
                            enableNavigatorSettingsComponents(true);
                        }
                        else{
                            enableSettingsComponents(true);
                        }
                        defaultCursor();
                    }
                    return null;
                }
            };
            workers.add(worker);
            worker.execute();
            if(worker.isDone()){
                workers.remove(worker);
            }
        }

    }

    public void validateSelectedFile(File f) throws InterruptedException{
        String name = f.getName();
        boolean isAdj = name.endsWith(".adj");
        boolean isDot = name.endsWith(".dot");

        if(isDot){
            int k = Utilities.isAbyssDotFile(f);
            if(k < 0){
                String msg = "Could not find the value of k for '" + name + "'.";
                System.out.println("WARNING: " + msg);
            }
            exploreSingleFile(f, k);
        }
        else if(isAdj){
            File parent = f.getParentFile();
            int k = Utilities.isAbyssAssemblyDirectory(parent);
            if(k < 0){
                String msg = "Could not find the value of k for '" + name + "'.";
                System.out.println("ERROR: " + msg);
            }
            exploreSingleFile(f, k);
        }
        else{
            String msg = "'" + name + "' is not an ADJ or DOT file.";
            System.out.println("WARNING: " + msg);
        }
    }

    private boolean singleFileLoaded = false;
    private boolean fresh = true;

    private void exploreSingleFile(File f, int k){
        try{
            navigatorPanel.setVisible(true);
//            System.out.println("Reading file: " + f.getAbsolutePath());
            explorer.loadFile(f, k);
            navigatorHasFocus();

            enableNavigatorSettingsComponents(true);
            enableStatsPlotsSettingsComponents(false);
            enableAdvancedSettingsComponents(false);
            assembliesAndColorsTable.setEnabled(false);
            scaffoldRadioButton.setEnabled(false);
            peRadioButton.setEnabled(false);
            seRadioButton.setEnabled(false);
            showStatsToggleButton.setEnabled(false);
            showPlotsToggleButton.setEnabled(false);
            showNavigatorToggleButton.setEnabled(false);
            sequenceSearchRadioButtonMenuItem.setEnabled(false);

            singleFileLoaded = true;
            navigatorSearchRadioButtonMenuItem.setSelected(true);

            fresh = true;

            navLabel.setText("NAVIGATOR (" + f.getName() + ")");
            setTitle(f.getParent() + " - " + DEFAULT_TITLE);
            
//            String msg = f.getName() + " was loaded.";
//            setStatus(msg, true, NORMAL_STATUS);
        }
        catch(IOException e){
            e.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    public static final int ERROR_STATUS = 0;
    public static final int NORMAL_STATUS = 1;

    public void setStatus(String msg, boolean printToStdout, int status){
        if(printToStdout){
            System.out.println(msg);
        }
        switch(status){
            case NORMAL_STATUS:
                statusLabel.setForeground(Color.BLACK);
                break;
            case ERROR_STATUS:
                statusLabel.setForeground(Color.RED);
                break;
            default:
                statusLabel.setForeground(Color.BLACK);
        }
        statusLabel.setText(msg);
    }

    private class ScaffoldTable extends JTable{
        @Override
        public javax.swing.table.TableCellRenderer getCellRenderer(int row, int column) {
            int rowCount = getRowCount();
            int covColumn = 5;
            boolean spanHidden = hiddenColumn != null;
            if(spanHidden){
                covColumn = 4;
            }

            if(column > 0
                && ( (disabledRowIndexesForSelectedFile.contains(convertRowIndexToModel(row)) && column != covColumn)
                    || (disabledRowIndexesForCoverageFile.contains(convertRowIndexToModel(row)) && column == covColumn)) )
            {
                return disabledRenderer;
            }
            else if(rowCount > 1
                && maxN50Index>=0 && maxN50Index<rowCount
                && maxContiguityIndex>=0 && maxContiguityIndex<rowCount
                && maxReconstructionIndex >= 0 && maxReconstructionIndex<rowCount
                && maxSpanIndex >= 0 && maxSpanIndex<rowCount
                && ((convertRowIndexToView(maxN50Index) == row && column == 1)
                    || (convertRowIndexToView(maxContiguityIndex) == row && column == 2)
                    || (convertRowIndexToView(maxReconstructionIndex) == row && column == 3)
                    || (convertRowIndexToView(maxSpanIndex) == row && column == 4 && !spanHidden)))
            {
                return maxRenderer;
            }

            return super.getCellRenderer(row, column);
        }
    }

    public void showNonModalDialog(String title, String msg){
        String formattedMsg = Utilities.formatDialogMessage(msg, MAX_LENGTH_PER_LINE_IN_DIALOG);
        JLabel label = new JLabel(formattedMsg);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        Dimension d = label.getPreferredSize();

        javax.swing.JDialog errDialog = new javax.swing.JDialog(this, title, false);
        errDialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        errDialog.getContentPane().add(label);
        int w = Math.max(d.width + 10, 300);
        int h = d.height + 100;
        errDialog.setSize(w, h);

        // move dialog to the middle of the main frame
        Rectangle r = this.getBounds();
        int x = r.x + r.width/2 - w/2;
        int y = r.y + r.height/2 - h/2;
        errDialog.setLocation(x, y);
        
        errDialog.setVisible(true);
    }

    public static void main(String[] args) {
        ArrayList<File> selectedFiles = new ArrayList<File>();

        int numFile = 0;
        int numDir = 0;

        for(String argument : args) {
            if(argument.equals("--version")) {
                System.out.println(about());
                System.exit(0);
            }
            else if(argument.equals("--help")) {
                try {
                    Dive d = new Dive();
                    d.help();
                } catch (IOException ex) {
                    System.out.println(ex.getMessage());
                } finally{
                    System.exit(0);
                }
            }
            else {
                File f = new File(argument);
                if(f.exists()){
                    f = f.getAbsoluteFile();
                    if(f.isDirectory()){
                        numDir++;
                    }
                    else{
                        numFile++;
                    }

                    if((numFile>0 && numDir>0) || // user provided a combination of file and directories
                            numFile > 1){ // user provided more than 1 file
                        System.out.println("Please either provide the path of one DOT or ADJ file or the paths of one ore more ABySS assembly directories.");
                        System.exit(-1);
                    }

                    selectedFiles.add(f);
                }
                else{
                    System.out.println("'" + argument + "' is not a valid argument. Please check the spelling and try again.");
                    System.exit(-1);
                }
            }
        }

        //Toolkit.getDefaultToolkit().setDynamicLayout(true);
        System.setProperty("sun.awt.noerasebackground", "true");
        final Dive frame = new Dive();

        // set frame to be 75% size of screen resolution
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Dimension scrnsize = toolkit.getScreenSize();
        Dimension framesize = new Dimension(scrnsize.width*3/4, scrnsize.height*3/4);
        //System.out.println(framesize.width + "," + framesize.height );

        // move frame to the middle of the screen
        int x = scrnsize.width/2 - framesize.width/2;
        int y = scrnsize.height/2 - framesize.height/2;
        frame.setLocation(x, y);
        frame.setPreferredSize(framesize);
        frame.pack();
        frame.setVisible(true);

        try{
            if(selectedFiles.size() > 0){
                if(selectedFiles.size() == 1 && selectedFiles.get(0).isFile()){
                    frame.validateSelectedFile(selectedFiles.get(0));
                }
                else{
                    File[] selectedFilesArr = new File[selectedFiles.size()];
                    selectedFilesArr = selectedFiles.toArray(selectedFilesArr);
                    frame.addAssemblies(selectedFilesArr);
                }
            }
            else{
                frame.checkCurrentDirectory();
            }
        }
        catch(InterruptedException ex){
            String doneText = "Stopped by user.";
            frame.setStatus(doneText, true, ERROR_STATUS);
        }
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JButton addAssembliesButton;
    private javax.swing.JMenuItem addAssembliesMenuItem;
    private javax.swing.JPanel addressbarPanel;
    private javax.swing.JButton applyButton;
    private javax.swing.JTable assembliesAndColorsTable;
    private javax.swing.JSplitPane assembliesAndSettingsSplitPane;
    private javax.swing.JPanel assembliesPanel;
    private javax.swing.JPopupMenu assembliesPopupMenu;
    private javax.swing.JComboBox assemblyComboBox;
    private javax.swing.JLabel assemblyLabel;
    private javax.swing.ButtonGroup assemblyModeButtonGroup;
    private javax.swing.JScrollPane atScrollPane;
    private javax.swing.JButton backButton;
    private javax.swing.JButton blatButton;
    private javax.swing.JMenuItem clearNavigatorMenuItem;
    private javax.swing.JMenuItem clearPlotsMenuItem;
    private javax.swing.JMenuItem clearStatsMenuItem;
    private javax.swing.JComboBox contigIdsComboBox;
    private javax.swing.JPanel contigSearchPanel;
    private javax.swing.JTextField contigSearchTextField;
    private javax.swing.JFrame contigSequenceFrame;
    private javax.swing.JTextArea contigSequenceTextArea;
    private javax.swing.JMenuItem copyFragmentStatsMenuItem;
    private javax.swing.JMenuItem copyScaffoldStatsMenuItem;
    private javax.swing.JButton copyToClipboardButton;
    private javax.swing.JPanel displayPanel;
    private javax.swing.JCheckBox drawCovPlotCheckBox;
    private javax.swing.JCheckBox drawFragSizeDistCheckBox;
    private javax.swing.JCheckBox drawN50plotCheckBox;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JButton exploreNeighborhoodButton;
    private javax.swing.JPanel explorerSettingsPanel;
    private javax.swing.JFileChooser fc;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JComboBox filesComboBox;
    private javax.swing.JPanel fragmentSizePanel;
    private javax.swing.JPopupMenu fragmentStatsPopupMenu;
    private javax.swing.JTable fragmentStatsTable;
    private javax.swing.JScrollPane fsScrollPane;
    private javax.swing.JComboBox genomeComboBox;
    private javax.swing.JLabel genomeLabel;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSeparator jSeparator5;
    private javax.swing.JPanel kDirPanel;
    private javax.swing.JScrollPane kDirScrollPane;
    private javax.swing.JLabel lengthLabel;
    private javax.swing.JSlider lengthSlider;
    private javax.swing.ButtonGroup lengthTypeButtonGroup;
    private javax.swing.JPopupMenu libsPopupMenu;
    private javax.swing.JCheckBoxMenuItem lineWrapCheckBoxMenuItem;
    private javax.swing.JButton loadAdjDotButton;
    private javax.swing.JMenuItem loadAdjDotMenuItem;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JLabel mclLabel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JFormattedTextField minContigLengthFormattedTextField;
    private javax.swing.JRadioButtonMenuItem navigatorSearchRadioButtonMenuItem;
    private javax.swing.JLabel noteLabel;
    private javax.swing.JRadioButton peRadioButton;
    private javax.swing.JPanel plotsPanel;
    private javax.swing.JTabbedPane plotsTabbedPane;
    private javax.swing.JButton removeAssembliesButton;
    private javax.swing.JMenuItem removeAssembliesMenuItem;
    private javax.swing.JMenuItem removeSelectedMenuItem;
    private javax.swing.JMenuItem resetMenuItem;
    private javax.swing.JRadioButton scaffoldRadioButton;
    private javax.swing.JPopupMenu scaffoldStatsPopupMenu;
    private javax.swing.JTable scaffoldStatsTable;
    private javax.swing.JLabel scaleLabel;
    private javax.swing.JRadioButton seRadioButton;
    private javax.swing.JButton searchButton;
    private javax.swing.ButtonGroup searchTypeButtonGroup;
    private javax.swing.JPopupMenu searchTypePopupMenu;
    private javax.swing.JButton selectLibrariesButton;
    private javax.swing.JRadioButtonMenuItem sequenceSearchRadioButtonMenuItem;
    private javax.swing.JLabel sequencesTooLongWarningLabel;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JTabbedPane settingsTabbedPane;
    private javax.swing.JRadioButton showAllRadioButton;
    private javax.swing.JButton showHideButton;
    private javax.swing.JCheckBox showLabelsCheckBox;
    private javax.swing.JCheckBox showLengthCheckBox;
    private javax.swing.JToggleButton showNavigatorToggleButton;
    private javax.swing.JRadioButton showNeighborsRadioButton;
    private javax.swing.JToggleButton showPlotsToggleButton;
    private javax.swing.JToggleButton showStatsToggleButton;
    private javax.swing.JSplitPane statsSplitPane;
    private javax.swing.JLabel statusLabel;
    private javax.swing.JPanel statusPanel;
    private javax.swing.ButtonGroup stepSizeButtonGroup;
    private javax.swing.JLabel stepSizeLabel;
    private javax.swing.JSlider stepSizeSlider;
    private javax.swing.JButton stopButton;
    private javax.swing.JPanel topPanel;
    private javax.swing.JLabel unitLabel;
    private javax.swing.JComboBox unitOfLengthComboBox;
    private javax.swing.JCheckBox useFastaIndexCheckBox;
    private javax.swing.JMenu viewMenu;
    private javax.swing.JComboBox xAxisScaleComboBox;
    private javax.swing.JComboBox yAxisUnitComboBox;
    // End of variables declaration//GEN-END:variables

}
