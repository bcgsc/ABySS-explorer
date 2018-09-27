
package ca.bcgsc.dive.chart;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 */

import ca.bcgsc.abyssexplorer.parsers.DotParser;
import ca.bcgsc.dive.stat.N50stats;
import java.awt.BasicStroke;
//import java.awt.BorderLayout;
import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.event.ActionEvent;
//import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
//import java.awt.event.WindowAdapter;
//import java.awt.event.WindowEvent;
import java.io.*;
import java.math.BigDecimal;
import java.math.MathContext;
//import java.text.DecimalFormat;
import java.util.*;
import javax.swing.*;
//import javax.swing.table.DefaultTableModel;
//import javax.swing.table.TableColumnModel;
//import javax.swing.table.TableRowSorter;
//import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
//import org.jfree.chart.event.ChartProgressEvent;
//import org.jfree.chart.event.ChartProgressListener;
//import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
//import org.jfree.chart.renderer.xy.XYStepRenderer;
//import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYStepRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.urls.StandardXYURLGenerator;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import ca.bcgsc.dive.util.*;
//import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;


public class N50plot implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final int DEFAULT_MIN_CONTIG_LENGTH = 1;
//    private static final String TEMP_FILE_NAME = "N50plot.tempdata";
    private int minContigLength = DEFAULT_MIN_CONTIG_LENGTH;
//    private int minContigLengthForSettings = 1;
//    private boolean firstRender = false;
//    private boolean isADJFile = false;
//    private ArrayList<String> fileNames = null;
    private HashMap<String, Integer> numContigsMap = null;
    private HashMap<String, Integer> numContigsLargerThanThresholdMap = null;
    private HashMap<String, Long> reconstructionMap = null;
    private HashMap<String, Long> contiguityMap = null;
    private HashMap<String, Long> n50map = null;
    private HashMap<String, Integer> numContigsLargerThanN50Map = null;
//    private HashMap<String, String> nameMap = null;
    private Map<String, Color> colorMap = null;
    private HashMap<String, Long> spanMap = null;
    private HashSet<String> pathsSet = null;
//    private JFrame frame = null;
//    private JRadioButton linearYAxisRadioButton = null;
//    private JRadioButton logYAxisRadioButton = null;
//    private JRadioButton bpLengthRadioButton = null;
//    private JRadioButton kmerLengthRadioButton = null;
//    private JRadioButton nrkmerLengthRadioButton = null;
//    private JRadioButton nolbpLengthRadioButton = null;
//    private JRadioButton percentileRadioButton = null;
//    private JRadioButton unitLengthRadioButton = null;
//    private JRadioButton optForRedrawRadioButton = null;
//    private JRadioButton optForFirstDrawRadioButton = null;
//    private JRadioButton contigLengthRadioButton = null;
//    private JRadioButton scaffoldLengthRadioButton = null;
//    private JTable table = null;
    private NumberAxis xAxis = null;
    private NumberAxis yAxis = null;
    private Range lastXAxisZoom = null;
    private Range lastYAxisZoom = null;
    private Range currXAxisZoom = null;
    private Range currYAxisZoom = null;
    private String homeDir = null;
//    private String tempDir = null;
//    private String[] tempFileNames = null;
    private XYSeriesCollection stepChartDataset = null;
//    private boolean scaffoldLengthInTemp = false;
    private JFreeChart chartDisplayed = null;

    public final static int BP = 0;
    public final static int KMER = 1;
    public final static int NR_KMER = 2;
    public final static int NOLBP = 3;

    private ArrayList<Integer> tempScaffoldSpans = null;
    private int tempNumContigs = 0;

    /**
     *
     *
     */
    public N50plot() {
        this(DEFAULT_MIN_CONTIG_LENGTH);
    }

    /**
     *
     * @param minContigLength
     */
    public N50plot(int minContigLength) {
        this.minContigLength = minContigLength;
//        this.minContigLengthForSettings = minContigLength;
//        tempDir = System.getProperty("java.io.tmpdir");
        homeDir = System.getProperty("user.home");
    }

    /**
     * Set the minimum contig length threshold
     * @param minContigLength  the minumn contig length threshold
     */
    public void setMinContigLength(int minContigLength) {
        this.minContigLength = minContigLength;
//        this.minContigLengthForSettings = minContigLength;
    }

    /**
     * Read FASTA/ADJ file and extract contig lengths from it
     * @param fileName  canonical path of the file
     * @param useKmer  use 'k-mer' as unit of length for contigs
     * @throws IOException
     */
    private ArrayList<Integer> readFile(String fileName, int k, boolean useKmer, boolean useFai) throws IOException, InterruptedException {
        ArrayList<Integer> lengthArr = new ArrayList<Integer>();

        try {
            tempNumContigs = 0;
            if(fileName.endsWith(".fa") || fileName.endsWith(".fasta")){
                if (useFai && Utilities.fileExist(fileName + ".fai")){
                    System.out.println("Reading FASTA index ...");
                    BufferedReader br = new BufferedReader(new FileReader(fileName + ".fai"));
                    String line = null;

                    // get contig lengths from rest of the file
                    while ((line = br.readLine()) != null) {
                        if(Thread.interrupted()){
                            throw new java.lang.InterruptedException();
                        }
                        int length = Utilities.getLengthFromLineInFAIFile(line);
                        if(length >= minContigLength) {                 
                            lengthArr.add(length);                            
                        }
                        tempNumContigs++;
                    }
                    br.close();
                }
                else{
                    BufferedReader br = new BufferedReader(new FileReader(fileName));
                    tempScaffoldSpans = new ArrayList<Integer>();                    
                    String line = null;
                    int scaffoldLength = 0;
                    int numNs = 0;                    
                    while((line = br.readLine()) != null) {
                        if(Thread.interrupted()){
                            throw new java.lang.InterruptedException();
                        }
                            
                        if(line.startsWith(">")) {
                            tempNumContigs++;
                            if(scaffoldLength >= minContigLength){                                
                                lengthArr.add(scaffoldLength);
                                tempScaffoldSpans.add(new Integer(scaffoldLength + numNs));
                            }
                            scaffoldLength = 0;
                            numNs = 0;
                            continue;
                        }

                        int[] arr = Utilities.getScaffoldLength(line);
                        scaffoldLength += arr[0];
                        numNs += arr[1];                        
                    }
                    br.close();
                }
            }
            else if(fileName.endsWith(".adj")){
                BufferedReader br = new BufferedReader(new FileReader(fileName));
                String line = null;

                // get contig lengths from rest of the file
                while ((line = br.readLine()) != null) {
                    if(Thread.interrupted()){
                        throw new java.lang.InterruptedException();
                    }
                    int length = Utilities.getLengthFromLineInADJFile(line);
                    if(length >= minContigLength) {
                        lengthArr.add(length);                            
                    }
                    tempNumContigs++;
                }
                br.close();
            }
            else if (fileName.endsWith(".dot")) {
                DotParser dotParser = new DotParser();
                dotParser.open(new File(fileName));
                while (true) {
                    if(Thread.interrupted()){
                        throw new java.lang.InterruptedException();
                    }
                    
                    int length = dotParser.getNextContigLength();
                    if (length < 0) { break; } // end of file
                    if(length >= minContigLength) {
                        lengthArr.add(length);                            
                    }
                    tempNumContigs++;
                }
                dotParser.close();
            }
//            BufferedReader br = new BufferedReader(new FileReader(fileName));
//            String line = null;
//
//            // read the first line
//            if ((line = br.readLine()) != null) {                
//                if ( line.startsWith(">") ) { // a FASTA file (*.fa)
//                        tempScaffoldSpans = new ArrayList<Integer>();
//                        tempNumContigs = 0;
//                        while(line != null) {
//                            if(Thread.interrupted()){
//                                throw new java.lang.InterruptedException();
//                            }
//
//                            int scaffoldLength = 0;
//                            int numNs = 0;
//                            while((line = br.readLine()) != null) {
//                                if(Thread.interrupted()){
//                                    throw new java.lang.InterruptedException();
//                                }
//
//                                if(line.startsWith(">")) {
//                                    break;
//                                }
//                                int[] arr = Utilities.getScaffoldLength(line);
//                                scaffoldLength += arr[0];
//                                numNs += arr[1];
//                            }
//                            tempNumContigs++;
//                            if(scaffoldLength >= minContigLength){
//                                lengthArr.add(scaffoldLength);
//                                tempScaffoldSpans.add(new Integer(scaffoldLength + numNs));
//                            }
//                        }
////                    }
//                }
//                else if (line.contains(";")){ //an ADJ file (*.adj)
//                    isADJFile = true;
//
//                    if (useKmer) {
//                        int length = Utilities.getLengthFromLineInADJFile(line);                        
//                        if(length >= minContigLength) {
//                            lengthArr.add(length - k + 1);                            
//                        }
//
//                        // get contig lengths from rest of the file
//                        while ((line = br.readLine()) != null) {
//                            if(Thread.interrupted()){
//                                throw new java.lang.InterruptedException();
//                            }
//                            length = Utilities.getLengthFromLineInADJFile(line);
//                            if(length >= minContigLength) {
//                                lengthArr.add(length - k + 1);                            
//                            }
//                        }
//                    }
//                    else {
//                        int length = Utilities.getLengthFromLineInADJFile(line);                        
//                        if(length >= minContigLength) {
//                            lengthArr.add(length);                            
//                        }
//
//                        // get contig lengths from rest of the file
//                        while ((line = br.readLine()) != null) {
//                            if(Thread.interrupted()){
//                                throw new java.lang.InterruptedException();
//                            }
//                            length = Utilities.getLengthFromLineInADJFile(line);
//                            if(length >= minContigLength) {
//                                lengthArr.add(length);                            
//                            }
//                        }
//                    }                    
//                }
//            }
//
//            br.close();
        }
        catch (IOException ex) {
            throw ex;
        }

        if (lengthArr.size() <= 0) {
            throw new IOException("ERROR: The file '" + fileName + "' does not contain any relevant data.");
        }

        return lengthArr;
    }

    /*
     * Return an array of contig lengths that are larger than the minimum contig length
     * @pre lengthArr must be sorted
     */
    private ArrayList<Integer> getBigContigs(ArrayList<Integer> lengthArr) {
        int lastIndexToCut = 0;
        int len = lengthArr.size();
        while (lastIndexToCut < len) {
            if (lengthArr.get(lastIndexToCut) >= minContigLength) {
                break;
            }
            lastIndexToCut++;
        }

        ArrayList<Integer> arr = new ArrayList<Integer>();
        for (int i = lastIndexToCut; i < len; i++) {
            arr.add(lengthArr.get(i));
        }

        return arr;
    }


    /*
     * Return a sum of the array of integers with an adjustment of minus k-1 for each integer.
     * Only integers >= k would be added.
     */
    private long getSum(ArrayList<Integer> arr, int k, boolean useNRKmer) {
        long sum = 0;
        long adjustmentForKmer = k - 1;

        if (useNRKmer) {
            adjustmentForKmer = adjustmentForKmer * 2;
        }

        int count = 0;
        int len = arr.size();

        for (int i = 0; i < len; i++) {
            long contigLength = arr.get(i) - adjustmentForKmer;
            if (contigLength >= 1) { // make sure that contigs < k are not added
                sum += contigLength;
            }
            else {
                count++;
            }
        }

        if (count > 0 && !useNRKmer) {
            System.out.println("WARNING: " + count + " contigs < k(=" + k + "). These contigs were not included.");
        }
        else if (count > 0 && useNRKmer) {
            System.out.println("WARNING: " + count + " contigs < 2k-1, k=" + k + ". These contigs were not included.");
        }

        return sum;
    }

    /**
     * Add the array of contig lengths as a new series to the dataset.
     * @param lengthArr  the array of contig lengths
     * @param name  name of the series
     * @param useKmer  true if using 'k-mer' or 'nolbp' as unit of length for contigs; false if using 'bp'
     * @param xAxisInBp  true if x-axis to be displayed in unit of length; false if displayed in percentile
     *
     * @pre lengthArr must be sorted in ascending order
     */
    private void addSeriesToDataset(ArrayList<Integer> lengthArr, int k, String name, boolean useKmer, boolean xAxisInBp) {
//        int k = Utilities.getKValueFromAssemblyName(name);
        long adjustment = 0; //default: no adjustment

        ArrayList<Integer> arr = null;

        long sum = -1;
        long span = -1;
        if (useKmer){
            arr = lengthArr;
            adjustment = k - 1;
            if (!xAxisInBp){
                sum = getSum(arr, k, false); // this method will output a warning message if any contigs are < k
            }
            if(tempScaffoldSpans != null && !tempScaffoldSpans.isEmpty()){
                span = getSum(tempScaffoldSpans, k, false);
            }
            else{
                span = sum;
            }
        }
        else {
            arr = lengthArr;
            sum = Utilities.getSum(arr);

            if(tempScaffoldSpans != null && !tempScaffoldSpans.isEmpty()){
                span = Utilities.getSum(tempScaffoldSpans);
            }
            else{
                span = sum;
            }
        }

        //System.out.println(sum);
        int len = arr.size();
        long currentSum = 0;

        numContigsLargerThanThresholdMap.put(name, new Integer(len));
        spanMap.put(name, new Long(span));

        XYSeries s = new XYSeries(name, false, true);

        long lastContigLength = -1;
        long currentContigLength = 0;
        long sos = 0;
        long count = 0;
        long halfReconstruction = sum/2;
        int nLargerThanOrEqualToN50 = 0;
        long n50 = -1;
        boolean n50NotFound = true;
        for (int i = len -1; i >=0 ; i--) {
            long contigLength = arr.get(i);
            //long contigLengthInKmer = contigLength - k + 1;
            currentContigLength = contigLength - adjustment;

            // make sure that contigs < k are not added
            if (currentContigLength > 0 /* && ((!isADJFile && contigLengthInKmer > 0 ) || isADJFile)*/) {
                // add a new data point to plot only when there is a new contig length
                if (lastContigLength != currentContigLength) {
                    if (xAxisInBp) { //x-axis in unit of length
                        s.add(currentSum, currentContigLength);
                    }
                    else { //x-axis in percentile
                        s.add(currentSum * 100.0 / (double) sum, currentContigLength);
                    }

                    lastContigLength = currentContigLength;
                }

                sos += currentContigLength * currentContigLength;

                currentSum += currentContigLength;

                if(n50NotFound){
                    nLargerThanOrEqualToN50++;
                    if(currentSum > halfReconstruction)
                    {
                        n50 = currentContigLength;
                        n50NotFound = false;
                    }
                }
            }
            else {
                count++;
            }
        }

        reconstructionMap.put(name, currentSum);
        n50map.put(name, n50);
        numContigsLargerThanN50Map.put(name, nLargerThanOrEqualToN50);
        
        //System.out.println("WARNING: " + count);
//        if(count >0 && !useNRKmer)
//        	System.out.println("WARNING: " + count );//+ " contigs < k(=" + k + "). These contigs were not included.");
//        else if(count >0 && useNRKmer)
//        	System.out.println("WARNING: " + count );//+ " contigs < 2k-1, k(=" + k + "). These contigs were not included.");

        long contiguity = (long) Math.rint(Math.sqrt(sos));
        //sSystem.out.println(contiguity);
        contiguityMap.put(name, contiguity);

        // add the last item on the plot
        if (xAxisInBp) {
            s.add(currentSum, currentContigLength);
        }
        else {
            s.add(100.0, currentContigLength);
        }

        if (stepChartDataset == null) {
            stepChartDataset = new XYSeriesCollection();
        }
        stepChartDataset.addSeries(s);
    }

//    public JInternalFrame plot(ArrayList<String> paths, boolean logY, boolean xAxisInUnitOfLength, int unitOfLength) throws Exception {
//        boolean useKmer = false;
//        boolean useNRKmer = false;
//        String kmerUnit = "";
//        String xAxisLabel = null;
//
//        switch(unitOfLength) {
//            case KMER:
//                kmerUnit = " (k-mer)";
//                useKmer = true;
//                break;
//            case NR_KMER:
//                kmerUnit = " (nr-k-mer)";
//                useNRKmer = true;
//                break;
//            case NOLBP:
//                kmerUnit = " (nol-bp)";
//                useNRKmer = true;
//                break;
//            default:
//                kmerUnit = " (bp)";
//                useKmer = false;
//                useNRKmer = false;
//        }
//
//        if(xAxisInUnitOfLength) {
//            switch(unitOfLength) {
//                case KMER:
//                    xAxisLabel = "Reconstruction (k-mer)";
//                    break;
//                case NR_KMER:
//                    xAxisLabel = "Reconstruction (nr-k-mer)";
//                    break;
//                case NOLBP:
//                    xAxisLabel = "Reconstruction (nol-bp)";
//                    break;
//                default:
//                    xAxisLabel = "Reconstruction (bp)";
//            }
//        }
//        else {
//            xAxisLabel = "Weighted Median Percentile";
//        }
//
//        // read the files and create the dataset
//        createDataset(paths, useKmer, xAxisInUnitOfLength, false, useNRKmer, true);
//
//        String title = null;//"Weighted Median of Contig Length";
//
//        String yAxisLabel = "Contig Length" + kmerUnit;
//
//        chartDisplayed = createChart(logY, xAxisInUnitOfLength, xAxisLabel, yAxisLabel, title);
//
//        ChartPanel chartPanel = new ChartPanel(chartDisplayed);
//        chartPanel.setMouseWheelEnabled(true);
//
//        // Set the max and min dimensions so that the labels do not stretch on resize when window size is between these dimension
//        chartPanel.setMinimumDrawWidth(0);
//        chartPanel.setMinimumDrawHeight(0);
//        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
//        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
//
//        JInternalFrame iframe = new JInternalFrame("N50 Plot");
//        iframe.getContentPane().add(chartPanel);
//        iframe.pack();
//        iframe.setFocusable(true);
//        iframe.addKeyListener(createKeyListenerForFrame());
//        return iframe;
//    }

    /*
     * NOTE:
     * The Y-axis in the chart is the axis for Weighted Percentile or Reconstruction
     * while the X-axis is the axis for Contig Lengths.
     *
     * The chart is oriented such that the Y-axis is the horizontal axis
     * while the X-axis is the vertical axis.
     */
    public JPanel drawPlot(Map<String, Color> map, Map<String, Integer> kValues, boolean logY, boolean xAxisInUnitOfLength, int unitOfLength, boolean useFai) throws InterruptedException {
        boolean useKmer = false;
        boolean useNRKmer = false;
        String kmerUnit = "";
        String xAxisLabel = null;

        switch(unitOfLength) {
            case KMER:
                kmerUnit = " (k-mer)";
                useKmer = true;
                break;
            case NR_KMER:
                kmerUnit = " (nr-k-mer)";
                useNRKmer = true;
                break;
            case NOLBP:
                kmerUnit = " (nol-bp)";
                useNRKmer = true;
                break;
            default:
                kmerUnit = " (bp)";
                useKmer = false;
                useNRKmer = false;
        }

        if(xAxisInUnitOfLength) {
            switch(unitOfLength) {
                case KMER:
                    xAxisLabel = "Reconstruction (k-mer)";
                    break;
                case NR_KMER:
                    xAxisLabel = "Reconstruction (nr-k-mer)";
                    break;
                case NOLBP:
                    xAxisLabel = "Reconstruction (nol-bp)";
                    break;
                default:
                    xAxisLabel = "Reconstruction (bp)";
            }
        }
        else {
            xAxisLabel = "Weighted Percentile (NXX)";
        }

        // read the files and create the dataset
        Set<String> set = map.keySet(); 
        try {
            createDataset(set, kValues, useKmer, xAxisInUnitOfLength, false, useNRKmer, useFai);
        } catch (InterruptedException ex) {
            throw ex;
        } catch (Exception ex) {
            return null;
        }


        String title = null;//"Weighted Median of Contig Length";

        String yAxisLabel = "Contig Length" + kmerUnit;

        chartDisplayed = createChart(logY, xAxisInUnitOfLength, xAxisLabel, yAxisLabel, title);

        XYPlot plot = chartDisplayed.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        int count = stepChartDataset.getSeriesCount();
        for(int i=0; i< count; i++){
            String path = (String) stepChartDataset.getSeriesKey(i);
            if(path != null){
                Color c = map.get(path);
                if(c != null){
                    renderer.setSeriesPaint(i, c);
                }
            }
        }
        colorMap = map;

        final MyChartPanel chartPanel = new MyChartPanel(chartDisplayed);
        chartPanel.setMouseWheelEnabled(true);

        // Set the max and min dimensions so that the labels do not stretch on resize when window size is between these dimension
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        
        chartPanel.addMouseListener(new MouseListener(){
            public void mouseClicked(MouseEvent me) {
            }
            public void mousePressed(MouseEvent me) {
            }
            public void mouseReleased(MouseEvent me) {
            }
            public void mouseEntered(MouseEvent me) {
                chartPanel.setFocusable(true);
                chartPanel.requestFocusInWindow();
            }

            public void mouseExited(MouseEvent me) {
                chartPanel.setFocusable(false);
            }
        });
        chartPanel.addKeyListener(createKeyListenerForFrame());

        LegendTitle legend = chartDisplayed.getLegend();
        legend.setVisible(false);
               
        return chartPanel;
    }

    public void addRangeMarker(double yVal){
        if(chartDisplayed != null){
            XYPlot plot = chartDisplayed.getXYPlot();
            plot.clearRangeMarkers();

            if(yVal > 0){
                ValueMarker marker = new ValueMarker(yVal);
                marker.setPaint(new Color(51,160,44));//new Color(236, 112, 20));
                marker.setStroke(new BasicStroke(2.0F));
                plot.addRangeMarker(marker);
            }
        }
    }

    public void removeAllRangeMarkers(){
        if(chartDisplayed != null){
            XYPlot plot = chartDisplayed.getXYPlot();
            plot.clearRangeMarkers();
        }
    }


    public void useLogYAxis(boolean useLog){
        if(useLog){
            if(yAxis instanceof LogarithmicAxis){
                return;
            }
            else{
                String label = yAxis.getLabel();
                yAxis = new LogarithmicAxis(label);
            }
        }
        else{
            if(yAxis instanceof LogarithmicAxis){
                String label = yAxis.getLabel();
                yAxis = new NumberAxis(label);
            }
            else{
                return;
            }
        }

        XYPlot plot = chartDisplayed.getXYPlot();
        plot.setRangeAxis(yAxis);
        yAxis.addChangeListener(new AxisChangeListener() {
            public void axisChanged(AxisChangeEvent e) {
                lastYAxisZoom = currYAxisZoom;
                currYAxisZoom = yAxis.getRange();
            }
        });
        
        lastYAxisZoom = yAxis.getRange();
        currYAxisZoom = lastYAxisZoom;
    }

    /*
     * NOTE:
     * The Y-axis in the chart is the axis for Weighted Percentile or Reconstruction
     * while the X-axis is the axis for Contig Lengths.
     *
     * The chart is oriented such that the Y-axis is the horizontal axis
     * while the X-axis is the vertical axis.
     */
    private JFreeChart createChart(boolean logY, boolean xAxisInBp, String xAxisLabel, String yAxisLabel, String title){
        xAxis = new NumberAxis(xAxisLabel);
        if (!xAxisInBp) {
            xAxis.setRange(0, 100);
            //xAxis.setInverted(true);
        }
        xAxis.setLowerMargin(0);
        xAxis.setUpperMargin(0);

        yAxis = null;
        if (logY) {
            yAxis = new LogarithmicAxis(yAxisLabel);
        }
        else {
            yAxis = new NumberAxis(yAxisLabel);
        }

        yAxis.addChangeListener(new AxisChangeListener() {
            public void axisChanged(AxisChangeEvent e) {
                lastYAxisZoom = currYAxisZoom;
                currYAxisZoom = yAxis.getRange();
            }
        });
        xAxis.addChangeListener(new AxisChangeListener() {
            public void axisChanged(AxisChangeEvent e) {
                lastXAxisZoom = currXAxisZoom;
                currXAxisZoom = xAxis.getRange();
            }
        });

        XYItemRenderer renderer = new XYStepRenderer(new StandardXYToolTipGenerator(), new StandardXYURLGenerator());

        final int numSeries = stepChartDataset.getSeriesCount();
        for(int i=0; i<numSeries; i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
        }

        final MyFastXYPlot plot = new MyFastXYPlot(stepChartDataset, xAxis, yAxis, renderer);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setDrawingSupplier(new MyDrawingSupplier());
        plot.setOrientation(PlotOrientation.HORIZONTAL);

        lastYAxisZoom = yAxis.getRange();
        lastXAxisZoom = xAxis.getRange();
        currYAxisZoom = lastYAxisZoom;
        currXAxisZoom = lastXAxisZoom;

        JFreeChart chart = new JFreeChart(title, plot);
        chart.setAntiAlias(false);
        chart.setTextAntiAlias(true);

        return chart;
    }


    public int removeSeries(String prefix){
        List<XYSeries> series = stepChartDataset.getSeries();
        List<XYSeries> seriesToRemove = new ArrayList<XYSeries>();
        for(XYSeries s : series){
//            Comparable key = s.getKey();
//            if(key instanceof String){
                String keyStr = (String) s.getKey();
                if(keyStr.startsWith(prefix)){
                    seriesToRemove.add(s);
                    
                    numContigsMap.remove(keyStr);
                    numContigsLargerThanThresholdMap.remove(keyStr);
                    reconstructionMap.remove(keyStr);
                    contiguityMap.remove(keyStr);
                    n50map.remove(keyStr);
                    numContigsLargerThanN50Map.remove(keyStr);
                    colorMap.remove(keyStr);
                    spanMap.remove(keyStr);
                    pathsSet.remove(keyStr);
                }
//            }
        }

        for(XYSeries s : seriesToRemove){
            stepChartDataset.removeSeries(s);
        }

        //correct the series colors
        XYPlot plot = chartDisplayed.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        int count = stepChartDataset.getSeriesCount();
        for(int i=0; i< count; i++){
            String path = (String) stepChartDataset.getSeriesKey(i);
            if(path != null){
                Color c = colorMap.get(path);
                if(c != null){
                    renderer.setSeriesPaint(i, c);
                }
            }
        }
        
        return stepChartDataset.getSeriesCount();
    }

//    /**
//     * Display the chart in a window.
//     * @param logY  use log Y axis
//     * @param useKmer  use k-mers as unit of length
//     * @param xAxisInBp  display x-axis in units of length
//     * @param useHddCache  use harddisk cache to optimize for redraw time
//     */
//    private void displayChart(boolean logY, final boolean useKmer, final boolean xAxisInBp, boolean useHddCache, final boolean useNRKmer, boolean useScaffold) {
//        frame = new JFrame("N50plot");
//        frame.addWindowListener(new WindowAdapter() {
//
//            @Override
//            public void windowClosing(WindowEvent e) {
//                System.out.println("N50plot is terminated.");
//            }
//        });
//
//        String kmerUnit = "";
//        if (!useKmer) {
//            kmerUnit = " (bp)";
//        }
//        else if (useKmer ) {
//            kmerUnit = " (k-mer)";
//        }
//
//        String title = null; //"Weighted Median of Contig Length";
//        String xAxisLabel = null;
//        if (xAxisInBp && !useKmer) {
//            xAxisLabel = "Reconstruction (bp)";
//        }
//        else if (xAxisInBp && useKmer) {
//            xAxisLabel = "Reconstruction (k-mer)";
//        }
//        else {
//            xAxisLabel = "Weighted Percentile (N50)";
//        }
//
//        String yAxisLabel = "Contig Length" + kmerUnit;
//
////        xAxis = new NumberAxis(xAxisLabel);
////        if (!xAxisInBp) {
////            xAxis.setRange(0, 100);
////            xAxis.setInverted(true);
////        }
////        xAxis.setLowerMargin(0);
////        xAxis.setUpperMargin(0);
////
////        yAxis = null;
////        if (logY) {
////            yAxis = new LogarithmicAxis(yAxisLabel);
////        } else {
////            yAxis = new NumberAxis(yAxisLabel);
////        }
////
////        yAxis.addChangeListener(new AxisChangeListener() {
////
////            public void axisChanged(AxisChangeEvent e) {
////                lastYAxisZoom = currYAxisZoom;
////                currYAxisZoom = yAxis.getRange();
////            }
////        });
////        xAxis.addChangeListener(new AxisChangeListener() {
////
////            public void axisChanged(AxisChangeEvent e) {
////                lastXAxisZoom = currXAxisZoom;
////                currXAxisZoom = xAxis.getRange();
////            }
////        });
////
////        XYItemRenderer renderer = new XYStepRenderer(new StandardXYToolTipGenerator(), new StandardXYURLGenerator());
////
////        final int numSeries = stepChartDataset.getSeriesCount();
////        for(int i=0; i<numSeries; i++)
////        {
////            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
////        }
////
////        final FastXYPlot plot = new FastXYPlot(stepChartDataset, xAxis, yAxis, renderer);
////        plot.setDomainPannable(true);
////        plot.setRangePannable(true);
////        plot.setBackgroundPaint(Color.WHITE);
////        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
////        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
////        plot.setDrawingSupplier(new MyDrawingSupplier());
////
////        lastYAxisZoom = yAxis.getRange();
////        lastXAxisZoom = xAxis.getRange();
////        currYAxisZoom = lastYAxisZoom;
////        currXAxisZoom = lastXAxisZoom;
////
////        chartDisplayed = new JFreeChart(title, plot);
////        chartDisplayed.setAntiAlias(false);
////        chartDisplayed.setTextAntiAlias(true);
////
////        plot.setDomainCrosshairVisible(true);
////        plot.setDomainCrosshairLockedOnData(false);
////        plot.setRangeCrosshairVisible(false);
//
//        chartDisplayed = createChart(logY, xAxisInBp, xAxisLabel, yAxisLabel, title);
//
//        ChartPanel chartPanel = new ChartPanel(chartDisplayed);
//
//        // Set the max and min dimensions so that the labels do not stretch on resize when window size is between these dimension
//        chartPanel.setMinimumDrawWidth(0);
//        chartPanel.setMinimumDrawHeight(0);
//        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
//        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
//
//        JPanel outerPanel = new JPanel(new BorderLayout());
//        outerPanel.add(chartPanel, BorderLayout.CENTER);
//        chartPanel.setMouseWheelEnabled(true);
//
//        final int numSeries = stepChartDataset.getSeriesCount();
//        final String[] columnNames = new String[]{"Assembly",
//            "N50" + kmerUnit, // contig length at crosshair
//            /*"Percentile",                      // weighted median percentile*/
//            "n", // total number of contigs
//            "n (l>=" + minContigLength + "bp)", // number of contigs larger than min contig length
//            "n (l>=N50)", // number of contigs larger than the crosshair
//            "Reconstruction" + kmerUnit, // reconstruction (sum of contig lengths those longer than min contig length)
//            "Contiguity" + kmerUnit};
//        final DefaultTableModel model = new DefaultTableModel(columnNames, numSeries) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public Class<?> getColumnClass(int column) {
//                // this is done so numeric columns can be sorted correctly (by numerical value instead of alphabetical value)
//                if (column != 0) {
//                    return Long.class;
//                }
//
//                return super.getColumnClass(column);
//            }
//
//            @Override
//            public boolean isCellEditable(int rowIndex, int columnIndex) {
//                return false;
//            }
//        };
//
//        table = new JTable(model);
//        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
//        table.setRowSorter(sorter);
//        sorter.setSortsOnUpdates(true);
//        sorter.toggleSortOrder(6); //sort by Contiguity in ascending order
//        sorter.toggleSortOrder(6); //sort in descending order
//
//        JScrollPane sp = new JScrollPane(table);
//        sp.setPreferredSize(new Dimension(chartPanel.getWidth(), 200));
//        outerPanel.add(sp, BorderLayout.SOUTH);
//
//        for (int i = 0; i < numSeries; i++) {
//            String name = (String) stepChartDataset.getSeriesKey(i);
//            model.setValueAt(name, i, 0); // series name
//            model.setValueAt(numContigsMap.get(name), i, 2);// total number of contigs
//            model.setValueAt(numContigsLargerThanThresholdMap.get(name), i, 3); // number of contigs larger than min contig length
//            model.setValueAt(reconstructionMap.get(name), i, 5); // reconstruction (sum of contig lengths those longer than min contig length)
//            model.setValueAt(contiguityMap.get(name), i, 6); // contiguity = sqrt of SOS
//        }
//        
//        final XYPlot plot = chartDisplayed.getXYPlot();
//        plot.setDomainCrosshairVisible(true);
//        plot.setDomainCrosshairLockedOnData(false);
//        plot.setRangeCrosshairVisible(false);
//
//        if (xAxisInBp) {
//            plot.setDomainCrosshairValue(plot.getDomainAxis().getUpperBound() / 2);
//        }
//        else {
//            // default the domain crossHair at N50
//            plot.setDomainCrosshairValue(50);
//        }
//
//        chartDisplayed.addProgressListener(new ChartProgressListener() {
//
//            public void chartProgress(ChartProgressEvent event) {
//                if (event.getType() != ChartProgressEvent.DRAWING_FINISHED) {
//                    return;
//                }
//
//                chartProgressHelper(plot, model, useKmer, xAxisInBp, numSeries, useNRKmer);
//            }
//        });
//
//        frame.setJMenuBar(setUpMenuBar(logY, useKmer, xAxisInBp, useHddCache, useNRKmer, useScaffold));
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(outerPanel);
//        frame.pack();
//        frame.setFocusable(true);
//        frame.addKeyListener(createKeyListenerForFrame());
//        frame.setVisible(true);
//    }

//    private void chartProgressHelper(XYPlot plot, DefaultTableModel model, boolean useKmer, boolean xAxisInBp, int numSeries, boolean useNRKmer) {
//        DecimalFormat oneDecimalPlace = new DecimalFormat("###0.#");
//        oneDecimalPlace.setDecimalSeparatorAlwaysShown(false);
//
//        String kmerUnit = "";
//        if (useNRKmer) {
//            kmerUnit = " (nr-k-mer)";
//        }
//        else if (!useKmer) {
//            kmerUnit = " (bp)";
//        }
//        else if (!isADJFile) {
//            kmerUnit = " (k-mer)";
//        }
//        else { //isADJFile
//            kmerUnit = " (nol-bp)";
//        }
//
//        //double N = plot.getDomainCrosshairValue();
//        double N = Math.rint(plot.getDomainCrosshairValue());
//
//        if (xAxisInBp) {
//            TableColumnModel cmodel = table.getColumnModel();
//            cmodel.getColumn(1).setHeaderValue("Weighted Median" + kmerUnit);
//            cmodel.getColumn(4).setHeaderValue("n (l>= Weighted Median)");
//        }
//        else {
//            //String nxxStr = oneDecimalPlace.format(N);
//        	long nxxStr = (long) N;
//            String nxxDisplay = "N" + nxxStr;
//            TableColumnModel cmodel = table.getColumnModel();
//            cmodel.getColumn(1).setHeaderValue(nxxDisplay + kmerUnit);
//            cmodel.getColumn(4).setHeaderValue("n (l>=" + nxxDisplay + ")");
//        }
//
//        table.getTableHeader().updateUI();//.repaint();
//
//        for (int i = 0; i < numSeries; i++) {
//            int indexOfItemToGet = 0;
//            int numItems = stepChartDataset.getItemCount(i);
//
//            if (xAxisInBp) {
//                //less iterations if dataset traversed backward
//                for (int j = numItems - 1; j >= 0; j--) {
//                    indexOfItemToGet = j;
//                    if (stepChartDataset.getXValue(i, j) <= N) {
//                        break;
//                    }
//                }
//            }
//            else {
//                //less iterations if dataset traversed backward
//                for (int j = numItems - 1; j >= 0; j--) {
//                    indexOfItemToGet = j;
//                    if (stepChartDataset.getXValue(i, j) >= N) {
//                        break;
//                    }
//                }
//            }
//
//            Long yVal = (long) stepChartDataset.getYValue(i, indexOfItemToGet);
//            model.setValueAt(yVal, i, 1); // contig length at crosshair
//            model.setValueAt(getNumContigsToTheRightOfCrosshair(i, N, xAxisInBp), i, 4);
//        }
//
//        table.updateUI();//.repaint();
//
////        if (firstRender) {
////            printJIRATable();
////            firstRender = false;
////        }
//    }

    /**
     * Find the best assembly based on square root of sum of squares of contig length in number of k-mers
     */
    public String findBestAssembly() {
        String best = null;
        Long bestScore = null;
        Set<String> keys = contiguityMap.keySet();
        Iterator<String> itr = keys.iterator();

        while (itr.hasNext()) {
            String name = itr.next();
            Long score = contiguityMap.get(name);

            //String path = nameMap.get(name);
            //System.out.println("Score for k" + getKFromPath(path) + ": " + score);

            if (best == null || bestScore < score) {
                best = name;
                bestScore = score;
            }
        }

        return best;
    }

//    /*
//     *
//     * @param logY
//     * @param useKmer
//     * @param xAxisInBp
//     * @param useHddCache
//     * @return
//     */
//    private JMenuBar setUpMenuBar(boolean logY, boolean useKmer, boolean xAxisInBp, final boolean useHddCache, boolean useNRKmer, boolean useScaffold) {
//
//        linearYAxisRadioButton = new JRadioButton("Use linear Y-axis");
//        logYAxisRadioButton = new JRadioButton("Use logarithmic Y-axis");
//        bpLengthRadioButton = new JRadioButton("Use 'bp' as unit of length for contigs");
//        kmerLengthRadioButton = new JRadioButton("Use 'k-mer' as unit of length for contigs");
//        nrkmerLengthRadioButton = new JRadioButton("Use 'non-redundant k-mer' as unit of length for contigs");
//        nolbpLengthRadioButton = new JRadioButton("Use 'non-overlapping bp' as unit of length for contigs");
//        percentileRadioButton = new JRadioButton("Use percentile for X-axis");
//        unitLengthRadioButton = new JRadioButton("Use units of length for X-axis");
//        optForRedrawRadioButton = new JRadioButton("Optimize for redraw time");
//        optForFirstDrawRadioButton = new JRadioButton("Optimize for first draw time");
//        contigLengthRadioButton = new JRadioButton("Contig Length");
//        scaffoldLengthRadioButton = new JRadioButton("Scaffold Length");
//
//        JMenuBar menuBar = new JMenuBar();
//        JMenu menu = new JMenu("Preferences");
//        JMenuItem item = new JMenuItem("Show saved settings");
//        item.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent arg0) {
//                displaySettingsSaved();
//            }
//        });
//        menu.add(item);
//
//        item = new JMenuItem("Save current settings");
//        item.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent arg0) {
//                setUserPreferences(minContigLengthForSettings,
//                        logYAxisRadioButton.isSelected(),
//                        kmerLengthRadioButton.isSelected() || nolbpLengthRadioButton.isSelected(),
//                        unitLengthRadioButton.isSelected(),
//                        optForRedrawRadioButton.isSelected(),
//                        nrkmerLengthRadioButton.isSelected(),
//                        scaffoldLengthRadioButton.isSelected());
//                displaySettingsSaved();
//            }
//        });
//        menu.add(item);
//
//        item = new JMenuItem("Redraw plot with current settings");
//        item.addActionListener(new ActionListener() {
//            public void actionPerformed(ActionEvent arg0) {
//                try {
//                    redrawPlot();
//                } catch (Exception ex) {
//                    System.out.println("ERROR: " + ex.getMessage());
//                }
//            }
//        });
//        menu.add(item);
//
//        menu.addSeparator();
//
//        item = new JMenuItem("Set minimum contig length");
//        item.addActionListener(new ActionListener() {
//
//            public void actionPerformed(ActionEvent arg0) {
//                // ask for min
//                String response = JOptionPane.showInputDialog(frame,
//                        "Enter a non-negative integer threshold for contig length",
//                        minContigLengthForSettings);
//                while (response != null) {
//                    if (Utilities.isNonNegativeInteger(response)) {
//                        minContigLengthForSettings = new Integer(response);
//                        break;
//                    } else {
//                        response = JOptionPane.showInputDialog(frame,
//                                "Enter an integer threshold for contig length",
//                                minContigLengthForSettings);
//                    }
//                }
//            }
//        });
//        menu.add(item);
//
//        menu.addSeparator();
//
//        ButtonGroup yAxisSettingGroup = new ButtonGroup();
//
//        yAxisSettingGroup.add(linearYAxisRadioButton);
//        yAxisSettingGroup.add(logYAxisRadioButton);
//        menu.add(linearYAxisRadioButton);
//        menu.add(logYAxisRadioButton);
//
//        linearYAxisRadioButton.setSelected(!logY);
//        logYAxisRadioButton.setSelected(logY);
//
//        menu.addSeparator();
//
//        ButtonGroup lengthUnitSettingGroup = new ButtonGroup();
//
//        lengthUnitSettingGroup.add(bpLengthRadioButton);
//        lengthUnitSettingGroup.add(kmerLengthRadioButton);
//        lengthUnitSettingGroup.add(nrkmerLengthRadioButton);
//        lengthUnitSettingGroup.add(nolbpLengthRadioButton);
//        menu.add(bpLengthRadioButton);
//        menu.add(kmerLengthRadioButton);
//        menu.add(nrkmerLengthRadioButton);
//        menu.add(nolbpLengthRadioButton);
//
//        bpLengthRadioButton.setSelected(!useKmer && !useNRKmer); // default
//        kmerLengthRadioButton.setSelected(useKmer && !isADJFile);
//        nolbpLengthRadioButton.setSelected(useKmer && isADJFile);
//        nrkmerLengthRadioButton.setSelected(useNRKmer);
//
//        kmerLengthRadioButton.setEnabled(!isADJFile); // never in k-mers if ADJ file
//        nrkmerLengthRadioButton.setEnabled(!isADJFile);
//        nolbpLengthRadioButton.setEnabled(isADJFile);
//
//        menu.addSeparator();
//
//        ButtonGroup xAxisSettingGroup = new ButtonGroup();
//
//        xAxisSettingGroup.add(percentileRadioButton);
//        xAxisSettingGroup.add(unitLengthRadioButton);
//        menu.add(percentileRadioButton);
//        menu.add(unitLengthRadioButton);
//        percentileRadioButton.setSelected(!xAxisInBp);
//        unitLengthRadioButton.setSelected(xAxisInBp);
//
//        menu.addSeparator();
//
//        ButtonGroup drawSettingGroup = new ButtonGroup();
//        drawSettingGroup.add(optForRedrawRadioButton);
//        drawSettingGroup.add(optForFirstDrawRadioButton);
//        menu.add(optForFirstDrawRadioButton);
//        menu.add(optForRedrawRadioButton);
//        optForRedrawRadioButton.setSelected(useHddCache);
//        optForFirstDrawRadioButton.setSelected(!useHddCache);
//
//        // no optimization options for ADJ files not in k-mer mode
//        optForRedrawRadioButton.setEnabled(!(isADJFile && !useKmer));
//        optForFirstDrawRadioButton.setEnabled(!(isADJFile && !useKmer));
//
//        menu.addSeparator();
//
//        ButtonGroup contigLengthTypeGroup = new ButtonGroup();
//        contigLengthTypeGroup.add(contigLengthRadioButton);
//        contigLengthTypeGroup.add(scaffoldLengthRadioButton);
//        menu.add(contigLengthRadioButton);
//        menu.add(scaffoldLengthRadioButton);
//        contigLengthRadioButton.setSelected(!useScaffold);
//        scaffoldLengthRadioButton.setSelected(useScaffold);
//
//        menuBar.add(menu);
//        return menuBar;
//    }

    private void redrawPlot() throws Exception {
//        frame.dispose();
//        System.out.println("The plot is being redrawn. Please wait...");
//        long start = System.currentTimeMillis();
//
//        minContigLength = minContigLengthForSettings;
//        boolean logY = logYAxisRadioButton.isSelected();
//        boolean useKmer = kmerLengthRadioButton.isSelected() || nolbpLengthRadioButton.isSelected();
//        boolean useNRKmer = nrkmerLengthRadioButton.isSelected();
//        boolean xAxisInBp = unitLengthRadioButton.isSelected();
//        boolean opForRedraw = optForRedrawRadioButton.isSelected();
//        boolean useScaffold = scaffoldLengthRadioButton.isSelected();
//
//        // if temp files exist and the current conditions allow reading (and writing) temp files
//        if (tempFileNames != null // temp file exists
//        		&& !(isADJFile && !useKmer) // unit of length is not 'nol-bp' (not dealing with ADJ file in k-mer)
//        		&& scaffoldLengthInTemp==useScaffold) { //type of length of contigs in the temp files is same as the one selected
//            System.out.println("Reading temp files...");
//            createDatasetFromTempFiles(useKmer, xAxisInBp, useNRKmer);
//        }
//        else {
//            createDataset(fileNames, useKmer, xAxisInBp, opForRedraw, useNRKmer, useScaffold);
//        }
//
//        displayChart(logY, useKmer, xAxisInBp, opForRedraw, useNRKmer, useScaffold);
//
//        System.out.println("Done. Elapsed time: " + (System.currentTimeMillis() - start) / 1000.0 + " seconds.");
    }

    /**
     *
     * @param fileNames
     * @param useKmer
     * @param xAxisInBp
     * @param useHddCache
     */
    public void createDataset(Collection<String> fileNames,
                Map<String, Integer> kValues,
    		boolean useKmer,
    		boolean xAxisInBp,
    		boolean useHddCache,
    		boolean useNRKmer,
    		boolean useFai) throws InterruptedException, IOException, Exception {// throws IOException {
//        firstRender = true;
        contiguityMap = new HashMap<String, Long>();
        numContigsMap = new HashMap<String, Integer>();
        numContigsLargerThanThresholdMap = new HashMap<String, Integer>();
        reconstructionMap = new HashMap<String, Long>();
        n50map = new HashMap<String, Long>();
        numContigsLargerThanN50Map = new HashMap<String, Integer>();
        spanMap = new HashMap<String, Long>();

//        nameMap = new HashMap<String, String>();
        pathsSet = new HashSet<String>();
        stepChartDataset = null;
//        this.fileNames = fileNames;
        int len = fileNames.size();
        // Add a new series to the plot for each file

//        if (useHddCache && tempFileNames == null) { // initialize if optimized for redraw time and there are no existing file names stored
//            tempFileNames = new String[len];
//        }

        int countNumFilesHaveError = 0;
        for (String path : fileNames){
            if(Thread.interrupted()){
                throw new java.lang.InterruptedException();
            }

            if (!Utilities.fileExist(path)) {// if file does not exist
                //fileNames.remove(path);
                countNumFilesHaveError++;
                System.out.println("ERROR: The file '" + path + "' does not exist.");
            }
            else if (pathsSet.contains(path)) { //if file exists but the same file has been read previously
                //fileNames.remove(path);
                countNumFilesHaveError++;
                System.out.println("ERROR: The file '" + path + "' has been read previously.");
            }
            else { //if file exists and it has not been read previously
                int k = kValues.get(path);

                try {
                    System.out.println("Reading file: " + path);
                    ArrayList<Integer> lengthArr = readFile(path, k, useKmer, useFai);
                    Collections.sort(lengthArr);

//                    // !(isADJFile && !useKmer) because it is the only case where no optimization allowed
//                    if (useHddCache && !(isADJFile && !useKmer)) {
//                        tempFileNames[i] = tempDir + File.separator + TEMP_FILE_NAME + i;
//
//                        File f = new File(tempDir + File.separator + TEMP_FILE_NAME + i);
//                        f.deleteOnExit();
//                        FileWriter fw = new FileWriter(f, false);
//                        fw.write(path + "\n");
//                        int arrayLength = lengthArr.size();
//                        for (int index = 0; index < arrayLength; index++) {
//                            fw.write(lengthArr.get(index) + "\n");
//                        }
//                        scaffoldLengthInTemp = useScaffold;
//                        fw.close();
//                    }

//                    String name = path;//f.getParentFile().getName() + File.separator + f.getName();
//                    if (nameMap.containsKey(name)) { // the abbreviation already exists!
//                        // rename all the keys in other maps and the dataset
//                        String existingPath = nameMap.get(name);
//                        nameMap.put(existingPath, existingPath);
//                        nameMap.remove(name);
//
//                        XYSeries s = stepChartDataset.getSeries(name);
//                        s.setKey(existingPath);
//
//                        numContigsMap.put(existingPath, numContigsMap.get(name));
//                        numContigsMap.remove(name);
//
//                        numContigsLargerThanThresholdMap.put(existingPath, numContigsLargerThanThresholdMap.get(name));
//                        numContigsLargerThanThresholdMap.remove(name);
//
//                        reconstructionMap.put(existingPath, reconstructionMap.get(name));
//                        reconstructionMap.remove(name);
//
//                        contiguityMap.put(existingPath, contiguityMap.get(name));
//                        contiguityMap.remove(name);
//
//                        spanMap.put(existingPath, spanMap.get(name));
//                        spanMap.remove(name);
//
//                        name = path;
//                    }

                    // for the current file, use path as name (no abbreviation)
//                    nameMap.put(name, path);
                    pathsSet.add(path);
                    numContigsMap.put(path, new Integer(tempNumContigs));
                    addSeriesToDataset(lengthArr, k, path, useKmer, xAxisInBp);
                    tempScaffoldSpans = null;

                } catch (IOException e) {
                    countNumFilesHaveError++;
                    System.out.println(e.getMessage());
                }
            }
        }

//        if (countNumFilesHaveError == len) { // all files have error; no file were read correctly
//            System.out.println("ERROR: No files were read correctly. N50plot is terminated.");
//            System.exit(-1);
//        }

        if (countNumFilesHaveError == len) { // all files have error; no file were read correctly            
            throw new Exception("No files could be read correctly. N50plot cannot be drawn.");
        }

    }

//    /*
//     * Create dataset by reading temporary data files (which were generated earlier).
//     * @param useKmer
//     * @param xAxisInBp
//     * @pre There exist files called "N50plot.tempdataX" where X is an integer
//     */
//    private void createDatasetFromTempFiles(boolean useKmer, boolean xAxisInBp, boolean useNRKmer) {
////        firstRender = true;
////
////        numContigsLargerThanThresholdMap = new HashMap<String, Integer>();
////        reconstructionMap = new HashMap<String, Long>();
////        n50map = new HashMap<String, Long>();
////        numContigsLargerThanN50Map = new HashMap<String, Integer>();
////
////        stepChartDataset = null;
////
////        int len = tempFileNames.length;
////        for (int i = 0; i < len; i++) {
////            ArrayList<Integer> lengthArr = new ArrayList<Integer>(); // this array was sorted before it was written to file
////            String path = null;
////            try {
////                BufferedReader br = new BufferedReader(new FileReader(tempFileNames[i]));
////                String line = null;
////
////                // read the first line
////                if ((line = br.readLine()) != null) {
////                    path = line;
////                }
////
////                while ((line = br.readLine()) != null) {
////                    lengthArr.add(Integer.parseInt(line));
////                }
////
////                br.close();
////            }
////            catch (IOException ex) {
////                System.out.println(ex.getMessage());
////            }
////
////            String name = Utilities.getAssemblyNameFromPath(path);
////
////            if (nameMap.containsKey(path)) {
////                addSeriesToDataset(lengthArr, path, useKmer, xAxisInBp, useNRKmer);
////            }
////            else {
////                addSeriesToDataset(lengthArr, name, useKmer, xAxisInBp, useNRKmer);
////            }
////        }
//    }
//
//    /*
//     *
//     * @param i
//     * @param crosshair
//     * @param xAxisInBp
//     * @return
//     */
//    private long getNumContigsToTheRightOfCrosshair(int i, double crosshair, boolean xAxisInBp) {
//        String key = (String) stepChartDataset.getSeriesKey(i);
//        long total = reconstructionMap.get(key);
//        double constant = ((double) total) / 100.0;
//
//        int numItems = stepChartDataset.getItemCount(i);
//        double sumLengthUpToThisPoint = stepChartDataset.getXValue(i, numItems - 1);
//
//        int count = 0;
//        double sumToStop = 0;
//        double currSum = 0;
//
//        if (xAxisInBp) {
//            sumToStop = crosshair;
//            currSum = sumLengthUpToThisPoint;
//        }
//        else {
//            sumToStop = 100.0 - crosshair * constant;
//            currSum = 100.0 - sumLengthUpToThisPoint * constant;
//        }
//
//        for (int j = numItems - 1; j > 0; j--) {
//            long contigLength = (long) stepChartDataset.getYValue(i, j - 1);
//            double sumLengthUpToNextPoint = stepChartDataset.getXValue(i, j - 1);
//            double difference = sumLengthUpToThisPoint - sumLengthUpToNextPoint;
//
//            if (!xAxisInBp) {
//                difference = Math.abs(difference * constant);
//            }
//
//            int numSameLength = (int) Math.rint(difference / contigLength);
//
//            while (numSameLength > 0) {
//                currSum -= contigLength;
//                count++;
//                if (currSum <= sumToStop) {
//                    break;
//                }
//                numSameLength--;
//            }
//
//            if (currSum <= sumToStop) {
//                break;
//            }
//
//            sumLengthUpToThisPoint = sumLengthUpToNextPoint;
//        }
//
//        return count;
//    }
//
//    private void displaySettingsSaved() {
//        String[] params = getUserPreferences();
//        String settingsStr = "";
//        String contigLength = Integer.toString(DEFAULT_MIN_CONTIG_LENGTH);
//        String yAxisScale = "linear";
//        String unitOfLength = "bp";
//        String unitForXAxis = "percentile";
//        String drawOptimization = "first draw time";
//        String lengthType = "contig length";
//        if (params != null) {
//            int cursor = 0;
//            if (params[cursor] != null && params[cursor].equals("-cl")) {
//                contigLength = params[1];
//                cursor = 2;
//            }
//            if (params[cursor] != null && params[cursor].equals("-ly")) {
//                yAxisScale = "logarithmic";
//                cursor++;
//            }
//            if (params[cursor] != null && params[cursor].equals("-nrkm")) {
//                unitOfLength = "nr-k-mer";
//                cursor++;
//            } else if (params[cursor] != null && params[cursor].equals("-km")) {
//                unitOfLength = "k-mer (or nol-bp if ADJ file)";
//                cursor++;
//            }
//            if (params[cursor] != null && params[cursor].equals("-ulx")) {
//                unitForXAxis = "units of length";
//                cursor++;
//            }
//            if (params[cursor] != null && params[cursor].equals("-hdc")) {
//                drawOptimization = "redraw time";
//                cursor++;
//            }
//            if (params[cursor] != null && params[cursor].equals("-scfd")) {
//                lengthType = "scaffold length";
//                cursor++;
//            }
//
//
//            settingsStr = "Minimum contig length: " + contigLength
//                    + "\nY-axis scale: " + yAxisScale
//                    + "\nUnit of length for contigs: " + unitOfLength
//                    + "\nX-axis displayed in: " + unitForXAxis
//                    + "\nOptimized for: " + drawOptimization
//                    + "\nLength displayed: " + lengthType;
//            JOptionPane.showMessageDialog(frame, settingsStr, "Saved Settings", JOptionPane.INFORMATION_MESSAGE);
//        }
//        else {
//            JOptionPane.showMessageDialog(frame, "You do not have any saved settings!", "Saved Settings", JOptionPane.WARNING_MESSAGE);
//        }
//    }

    private String[] getUserPreferences() {
        String pathToUserpref = homeDir + File.separator + "N50plot.userpref";
        if (!Utilities.fileExist(pathToUserpref)) {
            System.out.println("The file '" + pathToUserpref + "' does not exist. No settings were loaded.");
            return null;
        }

        System.out.println("Reading parameters from '" + pathToUserpref + "'...");
        int paramLength = 6; // there will be at most 6 lines.
        String[] params = new String[paramLength];
        try {
            BufferedReader br = new BufferedReader(new FileReader(pathToUserpref));
            String line = null;
            int i = 0;
            while (((line = br.readLine()) != null)
                    && i < paramLength) {
                params[i] = line.replaceAll(" ", "");
                i++;
            }

            br.close();
        }
        catch (IOException ex) {
            //ex.printStackTrace();
            System.out.println("The file '" + pathToUserpref + "' could not be read. No settings were loaded.");
            return null;
        }
        return params;
    }

    /**
     *
     * @param threshold
     * @param logY
     * @param useKmer
     * @param useBpx
     * @param useHddCache
     * @return
     */
    public boolean setUserPreferences(Integer threshold, boolean logY, boolean useKmer, boolean useBpx, boolean useHddCache, boolean useNRKmer, boolean useScaffold) {
        String pathToUserpref = homeDir + File.separator + "N50plot.userpref";

        try {
            FileWriter fw = new FileWriter(pathToUserpref, false);
            if (threshold != null) {
                fw.write("-cl\n" + threshold + "\n");
            }
            if (logY) {
                fw.write("-ly\n");
            }
            if (useNRKmer) {
                fw.write("-nrkm\n");
            } else if (useKmer) {
                fw.write("-km\n");
            }
            if (useBpx) {
                fw.write("-ulx\n");
            }
            if (useHddCache) {
                fw.write("-hdc\n");
            }
            if (useScaffold) {
            	fw.write("-scfd\n");
            }
            fw.close();
            System.out.println("Your settings were stored in '" + pathToUserpref + "'");
        }
        catch (IOException e) {
            //e.printStackTrace();
            System.out.println("ERROR: The settings could not be stored in '" + pathToUserpref + "'.");
            return false;
        }
        return true;
    }

    /**
     * Revert to the previous zoom state.
     */
    private void previousZoom() {
        if (lastXAxisZoom != null) {
            xAxis.setRange(lastXAxisZoom);
        }
        if (lastYAxisZoom != null) {
            yAxis.setRange(lastYAxisZoom);
        }
    }

    private void autoRange()
    {
        xAxis.setAutoRange(true);
        yAxis.setAutoRange(true);
    }

    private void toggleLegendVisibility()
    {
        LegendTitle legend = chartDisplayed.getLegend();
        if(legend != null)
        {
            legend.setVisible(!legend.isVisible());
        }
    }

    private KeyListener createKeyListenerForFrame()
    {
        KeyListener listener = new KeyListener(){
            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(final KeyEvent e) {
//                SwingWorker worker = new SwingWorker<Void, Void>() {
//                    public Void doInBackground() throws Exception {
//                        Component cmpnt = (Component)e.getSource();
//                        cmpnt.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        char c = e.getKeyChar();
                        switch(c) {
                            case 'p':
                                previousZoom();
                                break;
                            case 'a':
                                autoRange();
                                break;
                            case 'l':
                                toggleLegendVisibility();
                                break;
                            default:
                                break;
                        }

//                        cmpnt.setCursor(Cursor.getDefaultCursor());
//                        return null;
//                    }
//                };
//                worker.execute();
            }

            public void keyTyped(KeyEvent e) {
            }
        };
        return listener;
    }

    public N50stats getStats(String key){
        return new N50stats(n50map.get(key), contiguityMap.get(key), reconstructionMap.get(key), numContigsMap.get(key), numContigsLargerThanThresholdMap.get(key), numContigsLargerThanN50Map.get(key), spanMap.get(key));
    }

//    /**
//     * Print a table of results in JIRA format
//     */
//    public void printJIRATable() {
//        System.out.println("Preparing JIRA table...\n");
//        //model: Assembly,N50,n,n (l>=100bp),n (l>=N50),Reconstruction,Contiguity
//
//        DefaultTableModel model = (DefaultTableModel) table.getModel();
//        int numRowsInModel = model.getRowCount();
//
//        int numRows = numRowsInModel + 1; // one more row for header
//        int numCols = model.getColumnCount() + 1; // one more col for path
//
//        //find the largest N50, contiguity, reconstruction
//        int indexOfLargestN50 = 0;
//        int indexOfLargestContiguity = 0;
//        int indexOfLargestReconstruction = 0;
//        for (int i = 1; i < numRowsInModel; i++) {
//            if ((Long) model.getValueAt(i, 1) > (Long) model.getValueAt(indexOfLargestN50, 1)) {
//                indexOfLargestN50 = i;
//            }
//
//            if ((Long) model.getValueAt(i, 6) > (Long) model.getValueAt(indexOfLargestContiguity, 6)) {
//                indexOfLargestContiguity = i;
//            }
//
//            if ((Long) model.getValueAt(i, 5) > (Long) model.getValueAt(indexOfLargestReconstruction, 5)) {
//                indexOfLargestReconstruction = i;
//            }
//        }
//
//        // put data in a table
//        String[][] strTable = new String[numRows][numCols];
//
//        TableColumnModel cmodel = table.getColumnModel();
//        strTable[0] = new String[]{(String) cmodel.getColumn(0).getHeaderValue(), // Assembly
//                    (String) cmodel.getColumn(1).getHeaderValue(), // N50
//                    (String) cmodel.getColumn(6).getHeaderValue(), // contiguity
//                    (String) cmodel.getColumn(5).getHeaderValue(), // reconstruction
//                    (String) cmodel.getColumn(2).getHeaderValue(), // n
//                    (String) cmodel.getColumn(3).getHeaderValue(), // n (l>=100bp)
//                    (String) cmodel.getColumn(4).getHeaderValue(), // n (l>=N50)
//                    "Path"};
//
//        String name0 = (String) model.getValueAt(0, 0);
//        String path0 = nameMap.get(name0);
//        String parentdir = path0.substring(0, path0.indexOf(name0));
//        boolean shareParent = true;
//
//        for (int r = 0; r < numRowsInModel; r++) {
//            String name = (String) model.getValueAt(r, 0);
//            String path = nameMap.get(name);
//            File f = new File(path);
//            String assemblyNameAndFileName = f.getParent() + File.separator + f.getName();
//
//            if (!path.equals(parentdir + assemblyNameAndFileName)) {
//                shareParent = false;
//            }
//
//            int indexOfSlash = assemblyNameAndFileName.indexOf(File.separator);
//
//            String[] row = strTable[r + 1];
//            row[0] = assemblyNameAndFileName.substring(0, indexOfSlash);
//
//            if (r == indexOfLargestN50) {
//                row[1] = "*" + engineeringNotation((Long) model.getValueAt(r, 1)) + "*";
//            } else {
//                row[1] = engineeringNotation((Long) model.getValueAt(r, 1));
//            }
//
//            if (r == indexOfLargestContiguity) {
//                row[2] = "*" + engineeringNotation((Long) model.getValueAt(r, 6)) + "*";
//            } else {
//                row[2] = engineeringNotation((Long) model.getValueAt(r, 6));
//            }
//
//            if (r == indexOfLargestReconstruction) {
//                row[3] = "*" + engineeringNotation((Long) model.getValueAt(r, 5)) + "*";
//            } else {
//                row[3] = engineeringNotation((Long) model.getValueAt(r, 5));
//            }
//
//            row[4] = engineeringNotation((Long) model.getValueAt(r, 2));
//            row[5] = engineeringNotation((Long) model.getValueAt(r, 3));
//            row[6] = engineeringNotation((Long) model.getValueAt(r, 4));
//            row[7] = path;//assemblyNameAndFileName;
//        }
//
//        // if all paths share the same parent directory, then shorten the paths
//        if (shareParent) {
//            for (int r = 0; r < numRowsInModel; r++) {
//                strTable[r + 1][7] = Utilities.getAssemblyNameFromPath(strTable[r + 1][7]);
//            }
//        }
//
//        // format the table
//        int[] longestLengthForEachCol = new int[numCols];
//        // first, find the longest length for each column
//        for (int c = 0; c < numCols; c++) {
//            int longestLength = 0;
//            for (int r = 0; r < numRows; r++) {
//                int len = strTable[r][c].length();
//                if (len > longestLength) {
//                    longestLength = len;
//                }
//            }
//            longestLengthForEachCol[c] = longestLength;
//        }
//
//        //print this table with formatting
//        String line = "||";
//
//        // header
//        for (int c = 0; c < numCols; c++) {
//            // add the filler spaces
//            int difference = longestLengthForEachCol[c] - strTable[0][c].length();
//            String spaces = "";
//            while (difference > 0) {
//                spaces = spaces + " ";
//                difference--;
//            }
//
//            line = line + strTable[0][c] + spaces + " ||";
//        }
//        System.out.println(line); // print the header
//        // rest of the table
//        for (int r = 1; r < numRows; r++) {
//            line = "|";
//            for (int c = 0; c < numCols; c++) {
//                // add the filler spaces
//                int difference = longestLengthForEachCol[c] - strTable[r][c].length();
//                String spaces = "";
//                while (difference > 0) {
//                    spaces = spaces + " ";
//                    difference--;
//                }
//
//                line = line + " " + strTable[r][c] + spaces + " |";
//            }
//            System.out.println(line); // print row
//        }
//
////    	//print this table without formatting
////    	String line = "||";
////    	boolean isLabelDisplayingN50 = strTable[0][1].startsWith("N");
////    	int indexOfPercentileColumn = 2;
////    	for(int c=0; c<numCols; c++)
////    	{
////    		if(!(c == indexOfPercentileColumn && isLabelDisplayingN50))
////    			line = line + strTable[0][c] + "||";
////    	}
////    	System.out.println(line);
////
////    	for(int r=1; r<numRows; r++)
////    	{
////    		line = "|";
////        	for(int c=0; c<numCols; c++)
////        	{
////        		if(!(c == indexOfPercentileColumn && isLabelDisplayingN50))
////        			line = line + strTable[r][c] + "|";
////        	}
////        	System.out.println(line);
////    	}
//
//        // if all paths share the same parent directory, then display the shared parent directory
//        if (shareParent) {
//            System.out.println(parentdir + "\n");
//        }
//    }

//    /**
//     * Returns the enginnering notation of the double if it is larger than or equal to 1 million
//     */
//    private String engineeringNotation(double num) {
//        if (num < 1000000) // less than 1 million
//        {
//            return Double.toString(num);
//        }
//
//        // greater than 1 million
//        BigDecimal bd = new BigDecimal(num, new MathContext(4)); // 4 sig figs
//        return bd.toEngineeringString();
//    }

    /**
     * Returns the enginnering notation of the long if it is larger than or equal to 1 million
     */
    private String engineeringNotation(long num) {
        if (num < 1000000) { // less than 1 million
            return Long.toString(num);
        }

        // greater than 1 million
        BigDecimal bd = new BigDecimal(num, new MathContext(4)); // 4 sig figs
        return bd.toEngineeringString();
    }

//    public void help() throws IOException {
//        InputStream is = getClass().getResourceAsStream("/help/help.txt");
//        InputStreamReader isr = new InputStreamReader(is);
//        BufferedReader br = new BufferedReader(isr);
//        String line;
//        while ((line = br.readLine()) != null) {
//        	System.out.println(line);
//        }
//        br.close();
//        isr.close();
//        is.close();
//        System.exit(0);
//    }
//
//    public void version() throws IOException {
//        InputStream is = getClass().getResourceAsStream("/help/version.txt");
//        InputStreamReader isr = new InputStreamReader(is);
//        BufferedReader br = new BufferedReader(isr);
//        String line;
//        while ((line = br.readLine()) != null)
//        {
//        	System.out.println(line);
//        }
//        br.close();
//        isr.close();
//        is.close();
//        System.exit(0);
//    }


//    /**
//     * java -jar N50plot.jar
//     *      -cl  <integer threshold of the smallest contig length in base pairs>
//     *      -ly  (include this tag only when the y-axis needs to be in logarithmic scale)
//     *      -km  (include this tag only when lengths are displayed in number of k-mers instead of bp)
//     *      -ulx (include this tag only when the x-axis needs to be displayed in units of length instead of percentile)
//     *      -hdc (include this tag only when optimizing for redraw time)
//     *      <complete path to files separated by spaces>
//     */
//    public static void main(String[] args) {
//        long start = System.currentTimeMillis();
//        boolean isLogY = false;
//        boolean useKmer = false;
//        boolean useNRKmer = false;
//        boolean xAxisInBp = false;
//        boolean useHddCache = false;
//        boolean useScaffold = false;
//        int minContigLength = 1;
//        ArrayList<String> fileNames = new ArrayList<String>();
//
//
//        if (args.length <= 0 || args[0].equals("--help")) {
//            N50plot np = new N50plot();
//            try {
//                np.help();
//            }
//            catch (IOException e) {
//                System.out.println(e.getMessage());
//                System.exit(0);
//            }
//        }
//        else if (args[0].equals("--version")) {
//            N50plot np = new N50plot();
//            try {
//                np.version();
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//                System.exit(0);
//            }
//        }
//
//        System.out.println("N50plot was launched.");
//
//        int cursor=0;
//        while(cursor<args.length) {
//            String argument = args[cursor];
//            if(argument.equals("-cl") && cursor+1<args.length) {
//                String cl = args[++cursor];
//                if (!Utilities.isNonNegativeInteger(cl)) { // accept 0
//                    System.out.println("ERROR: The minimum contig length must be a positive integer! Your input was: " + cl + " N50plot is terminated.");
//                    System.exit(-1);
//                }
//                minContigLength = Integer.parseInt(cl);
//                System.out.println("Smallest Contig Length Allowed: " + cl);
//            }
//            else if (argument.equals("-ly")) {
//                isLogY = true;
//                System.out.println("Use log Y-axis");
//            }
//            else if (argument.equals("-km")) {
//                useKmer = true;
//                System.out.println("Use 'k-mer' (or 'nol-bp' if ADJ file) as unit of length");
//            }
//            else if (argument.equals("-nrkm")) {
//                useKmer = false;
//                useNRKmer = true;
//                System.out.println("Use 'non-redundant k-mer' as unit of length");
//            }
//            else if (argument.equals("-ulx")) {
//                xAxisInBp = true;
//                System.out.println("Display X-axis in units of length");
//            }
//            else if (argument.equals("-hdc")) {
//                System.out.println("Optimized for redraw time");
//                useHddCache = true;
//            }
//            else if(argument.equals("-scfd")) {
//                System.out.println("Display length as scaffold length");
//                useScaffold = true;
//            }
//            else {
//                break;
//            }
//            cursor++;
//        }
//
//        N50plot p = new N50plot();
//
//        // cursor did not move; no params were entered.
//        if (cursor == 0) {
//            //read params from "N50plot.userpref"
//            String[] params = p.getUserPreferences();
//            if (params != null) {
//                int paramsCursor = 0;
//                if (params[0] != null && params[0].equals("-cl")) {
//                    String mcl = params[1];
//                    if (!Utilities.isNonNegativeInteger(mcl)) { // accept 0
//                        System.out.println("ERROR: The minimum contig length must be a positive integer! Your input was: " + mcl + "N50plot is terminated.");
//                        System.exit(-1);
//                    }
//                    minContigLength = Integer.parseInt(mcl);
//                    System.out.println("Smallest Contig Length Allowed: " + mcl);
//                    paramsCursor = 2;
//                }
//                if (params[paramsCursor] != null && params[paramsCursor].equals("-ly")) {
//                    isLogY = true;
//                    System.out.println("Use log Y-axis");
//                    paramsCursor++;
//                }
//                if (params[paramsCursor] != null && params[paramsCursor].equals("-km")) {
//                    useKmer = true;
//                    System.out.println("Use 'k-mer' (or 'nol-bp' if ADJ file) as unit of length for contigs");
//                    paramsCursor++;
//                }
//                if (params[paramsCursor] != null && params[paramsCursor].equals("-nrkm")) {
//                    useKmer = false;
//                    useNRKmer = true;
//                    System.out.println("Use 'non-redundant k-mer' (or 'nol-bp' if ADJ file) as unit of length for contigs");
//                    paramsCursor++;
//                }
//                if (params[paramsCursor] != null && params[paramsCursor].equals("-ulx")) {
//                    xAxisInBp = true;
//                    System.out.println("Display X-axis in units of length");
//                    paramsCursor++;
//                }
//                if (params[paramsCursor] != null && params[paramsCursor].equals("-hdc")) {
//                    useHddCache = true;
//                    System.out.println("Optimize for redraw time");
//                    paramsCursor++;
//                }
//                if (params[paramsCursor] != null && params[paramsCursor].equals("-scfd")) {
//                    useScaffold = true;
//                    System.out.println("Display length as scaffold length");
//                    paramsCursor++;
//                }
//
//                if (paramsCursor == 0) {
//                    System.out.println("The file 'N50plot.userpref' is either empty or not in the correct format. No settings were loaded.");
//                }
//            }
//        }
//
//        // read the file names
//        for (int i = cursor; i < args.length; i++) {
//            File f = new File(args[i]);
//            String path = null;
//            try {
//                path = f.getCanonicalPath();
//                fileNames.add(path);
//            }
//            catch (IOException e) {
//                System.out.println(e.getMessage());
//            }
//        }
////    	}
//
//        //N50plot p = new N50plot(minContigLength);
//        p.setMinContigLength(minContigLength);
//        //p.setFileNames(fileNames);
//
//        if (fileNames.size() > 0) {
//            try {
//                // read the files and create the dataset
//                p.createDataset(fileNames, useKmer, xAxisInBp, useHddCache, useNRKmer, useScaffold);
//            } catch (Exception ex) {
//                // no files could be read correctly
//                System.out.println("ERROR: " + ex.getMessage());
//                System.out.println("N50plot terminates.");
//                System.exit(-1);
//            }
//
//            // display the chart in a window
//            p.displayChart(isLogY, useKmer, xAxisInBp, useHddCache, useNRKmer, useScaffold);
//
//            // print the best assembly in standard output
//            System.out.println("Best Assembly: " + p.findBestAssembly());
//
//            // done
//            System.out.println("Done. Elapsed time: " + (System.currentTimeMillis() - start) / 1000.0 + " seconds.");
//        }
//        else {
//            System.out.println("ERROR: No files were read. N50plot is terminated.");
//        }
//    }
}

