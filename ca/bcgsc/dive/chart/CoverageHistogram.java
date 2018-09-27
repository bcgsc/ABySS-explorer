
package ca.bcgsc.dive.chart;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 * Updated on July 15, 2010
 */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.io.*;
import java.util.*;
import javax.swing.*;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.UnitType;


public class CoverageHistogram {

    //private int numBinsToSkip = 0;
    //private int upperLimit = -1;
    private XYSeriesCollection primaryDataset = null;
    private XYSeriesCollection secondaryDataset = null;
//    private HashMap<String, String> nameToPathMap = new HashMap<String, String>();
    private HashSet<String> pathsSet = new HashSet<String>();
    private HashMap<String, Long[]> thresholdAndMedianMap = new HashMap<String, Long[]>();
    private HashMap<String, XYPointerAnnotation[]> annotationsMap = new HashMap<String, XYPointerAnnotation[]>();
    private Map<String, Color> colorMap = null;
    private long maxMedian = 0;
    private long maxCount = 0;
    private long maxReconstruction = 0;
    private Range lastXAxisZoom = null;
    private Range lastYAxis1Zoom = null;
    private Range lastYAxis2Zoom = null;
    private Range currXAxisZoom = null;
    private Range currYAxis1Zoom = null;
    private Range currYAxis2Zoom = null;
    private JFreeChart chartDisplayed = null;
    private XYPlot coveragePlot = null;
    private XYPlot reconstructionPlot = null;
    private NumberAxis xAxis = null;
    private NumberAxis yAxis1 = null;
    private NumberAxis yAxis2 = null;

    public CoverageHistogram() {
        primaryDataset = new XYSeriesCollection();
        secondaryDataset = new XYSeriesCollection();
    }

    /**
     * @pre the file at the path specified must exist
     */
    private void readHistFile(String path) throws IOException {
//        String name = Utilities.getAssemblyNameFromPath(path);
//        String name = null;
        File f = new File(path);
//        name = path;//f.getParentFile().getName() + File.separator + f.getName();

//        // Check whether the abbreviation (name) already exist...
//        if (nameToPathMap.containsValue(path)) {
//            return; // this file has already been read; don't do anything
//        } else if (nameToPathMap.containsKey(name)) {
//            //get the path that links to the existing abbreviation
//            String thePathInMap = nameToPathMap.get(name);
//
//            //rename the keys in all maps with the path
//            nameToPathMap.put(thePathInMap, thePathInMap);
//            thresholdAndMedianMap.put(thePathInMap, thresholdAndMedianMap.get(name));
//
//            //remove the pairs that used abbreviation in all maps
//            nameToPathMap.remove(name);
//            thresholdAndMedianMap.remove(name);
//
//            // use path as name for the current file
//            nameToPathMap.put(path, path);
//            name = path;
//        } else {
//            // use abbreviation as name
//            nameToPathMap.put(name, path);
//        }

        if(!f.exists()){
            System.out.println("ERROR: The file '" + path + "' does not exist.");
            return;
        }
        else if(pathsSet.contains(path)){
            System.out.println("ERROR: The file '" + path + "' has been read previously.");
            return;
        }
        else{
            pathsSet.add(path);
        }

        XYSeries s = new XYSeries(path, false, true);
        primaryDataset.addSeries(s);
        ArrayList<Long> histogram = new ArrayList<Long>();
        ArrayList<Long> kmerCoverage = new ArrayList<Long>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(path));
            String line = null;

            while ((line = br.readLine()) != null) {
                String[] arr = line.split("\t");
                Long x = new Long(arr[0]);
                Long y = new Long(arr[1]);
                s.add(x, y);

                kmerCoverage.add(x);
                histogram.add(y);

//	        	if(upperLimit>0 && x >= upperLimit)
//	        		break;
            }
            br.close();
        } catch (IOException ex) {
            //ex.printStackTrace();
            throw ex;
        }

        // create the dataset for the integral plot
        XYSeries s2 = new XYSeries(path, false, true);
        secondaryDataset.addSeries(s2);

        Long sum = 0L;
        int len = histogram.size();

        //traverse backwards because integral is computed from X_i to X_max
        for (int i = len - 1; i >= 0; i--) {
            sum += histogram.get(i);

            s2.add(kmerCoverage.get(i), sum);
        }

        Long[] results = getCoverageThresholdAndMedianKmerCoverage(path);

        System.out.println("Coverage Threshold: " + results[0]);
        System.out.println("Reconstruction @ Threshold: " + results[1]);
        System.out.println("Median k-mer Coverage: " + results[2]);
        System.out.println("Reconstruction @ Median: " + results[3]);
    }

    public long getMedianCoverage(String key){
        Long[] results = thresholdAndMedianMap.get(key);
        if(results != null){
            return results[2];
        }

        return -1;
    }

    /*
    public void displayChart()
    {
    double maxY1 = maxFrequency*1.1;
    double maxX = maxMedian*2;
    double maxY2 = maxReconstruction*1.1;

    String title1 = null;
    String xAxisLabel1 = "K-mer Coverage";
    String yAxisLabel1 = "Count";

    JPanel outerPanel = new JPanel(new GridBagLayout());


    GridBagConstraints c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 0;


    final JFreeChart chart1 = ChartFactory.createScatterPlot(
    title1,
    xAxisLabel1, yAxisLabel1,
    primaryDataset,
    PlotOrientation.VERTICAL,
    true,   // legend
    true,   // tooltips
    false   // urls
    );

    chart1.getLegend().setBackgroundPaint(chart1.getXYPlot().getBackgroundPaint());

    ChartPanel chartPanel1 = new ChartPanel(chart1);
    chartPanel1.setMouseWheelEnabled(true);
    outerPanel.add(chartPanel1, c);

    c = new GridBagConstraints();
    c.fill = GridBagConstraints.BOTH;
    c.gridx = 0;
    c.gridy = 1;


    String title2 = null;
    String xAxisLabel2 = "K-mer Coverage";
    String yAxisLabel2 = "Reconstruction";

    final JFreeChart chart2 = ChartFactory.createScatterPlot(
    title2,
    xAxisLabel2, yAxisLabel2,
    secondaryDataset,
    PlotOrientation.VERTICAL,
    true,   // legend
    true,   // tooltips
    false   // urls
    );

    chart2.getLegend().setBackgroundPaint(chart2.getXYPlot().getBackgroundPaint());

    if(maxMedian > 0 && maxFrequency > 0 && maxReconstruction > 0)
    {
    ValueAxis yAxis1 = chart1.getXYPlot().getRangeAxis();
    yAxis1.setUpperBound(maxY1);
    ValueAxis xAxis1 = chart1.getXYPlot().getDomainAxis();
    xAxis1.setUpperBound(maxX);
    ValueAxis yAxis2 = chart2.getXYPlot().getRangeAxis();
    yAxis2.setUpperBound(maxY2);
    ValueAxis xAxis2 = chart2.getXYPlot().getDomainAxis();
    xAxis2.setUpperBound(maxX);
    }

    ChartPanel chartPanel2 = new ChartPanel(chart2);
    chartPanel2.setMouseWheelEnabled(true);
    outerPanel.add(chartPanel2, c);

    //JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, chartPanel1, chartPanel2);
    //sp.setOneTouchExpandable(true);

    JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(outerPanel);

    frame.pack();
    frame.setVisible(true);
    }
     */

    private void toggleLegendVisibility()
    {
        LegendTitle legend = chartDisplayed.getLegend();
        if(legend != null)
        {
            legend.setVisible(!legend.isVisible());
        }
    }

//    private void busyCursor()
//    {
//        this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
//    }
//
//    private void defaultCursor()
//    {
//        this.setCursor(Cursor.getDefaultCursor());
//    }

    private KeyListener createKeyListenerForFrame()
    {
    	return new KeyListener() {
            public void keyPressed(KeyEvent e) {
            }

            public void keyReleased(final KeyEvent e) {
//                SwingWorker worker = new SwingWorker<Void, Void>() {
//                    public Void doInBackground() throws Exception {
//                        Component cmpnt = (Component)e.getSource();
//                        cmpnt.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

                        char c = e.getKeyChar();
                        switch(c){
                            case 'p':
                                    previousZoom();
                                    break;
                            case 's':
                                    zoomSpecial();
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
    }

//    private JInternalFrame displayChartInInternalFrame()
//    {
//        chartDisplayed = createChart();
//        ChartPanel chartPanel = new ChartPanel(chartDisplayed);
//        chartPanel.setZoomAroundAnchor(true);
//        chartPanel.setMouseWheelEnabled(true);
//
//        // This is done so the labels do not stretch on resize
//        chartPanel.setMinimumDrawWidth(0);
//        chartPanel.setMinimumDrawHeight(0);
//        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
//        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
//
//        JInternalFrame frame = new JInternalFrame("K-mer Coverage");
//        //frame.setJMenuBar(setUpMenuBar());
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(chartPanel);
//        frame.pack();
//        frame.setFocusable(true);
//        frame.addKeyListener(createKeyListenerForFrame());
//        frame.setVisible(true);
//
//        return frame;
//    }

    public JFrame displayChartInFrame()
    {
        chartDisplayed = createChart();
        ChartPanel chartPanel = new ChartPanel(chartDisplayed);
        chartPanel.setZoomAroundAnchor(true);
        chartPanel.setMouseWheelEnabled(true);

        // This is done so the labels do not stretch on resize
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        JFrame frame = new JFrame("K-mer Coverage");
        frame.setJMenuBar(setUpMenuBar());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(chartPanel);
        frame.pack();
        frame.setFocusable(true);
        frame.addKeyListener(createKeyListenerForFrame());
        frame.setVisible(true);

        return frame;
    }

    private JFreeChart createChart() {
        double GAP_THRESHOLD = 1.0;

        double maxY1 = maxCount * 1.3;
        double maxX = maxMedian * 2;
        double maxY2 = maxReconstruction * 1.3;

        String title = null;
        String xAxisLabel = "K-mer Coverage";
        String yAxisLabel1 = "Count";
        String yAxisLabel2 = "Reconstruction";

        yAxis1 = new NumberAxis(yAxisLabel1);
        yAxis1.addChangeListener(new AxisChangeListener() {

            public void axisChanged(AxisChangeEvent e) {
                lastYAxis1Zoom = currYAxis1Zoom;
                currYAxis1Zoom = yAxis1.getRange();
            }
        });
        xAxis = new NumberAxis(xAxisLabel);
        xAxis.addChangeListener(new AxisChangeListener() {

            public void axisChanged(AxisChangeEvent e) {
                lastXAxisZoom = currXAxisZoom;
                currXAxisZoom = xAxis.getRange();
            }
        });
        yAxis2 = new NumberAxis(yAxisLabel2);
        yAxis2.addChangeListener(new AxisChangeListener() {

            public void axisChanged(AxisChangeEvent e) {
                lastYAxis2Zoom = currYAxis2Zoom;
                currYAxis2Zoom = yAxis2.getRange();
            }
        });

        if (maxMedian > 0 && maxCount > 0 && maxReconstruction > 0) {
            yAxis1.setUpperBound(maxY1);
            yAxis2.setUpperBound(maxY2);
            xAxis.setUpperBound(maxX);
        }

        lastXAxisZoom = xAxis.getRange();
        lastYAxis1Zoom = yAxis1.getRange();
        lastYAxis2Zoom = yAxis2.getRange();


        MyFastXYPlot subplot1 = new MyFastXYPlot(primaryDataset, null, yAxis1, null);
        subplot1.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);

        MyFastXYPlot subplot2 = new MyFastXYPlot(secondaryDataset, null, yAxis2, null);
        subplot2.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);

        Set<String> keys = thresholdAndMedianMap.keySet();
        //double angle = 5.5;
        for(String name : keys){
            //String name = getAssemblyNameFromPath(path);
            Long[] arr = thresholdAndMedianMap.get(name);

            double labelOffset = XYPointerAnnotation.DEFAULT_LABEL_OFFSET * 3;
            double tipRadius = XYPointerAnnotation.DEFAULT_TIP_RADIUS / 2;

            //add annotation for median in plot 1
            XYPointerAnnotation anno1 = new XYPointerAnnotation(arr[2].toString(), arr[2], arr[5], 5.5);
            anno1.setLabelOffset(labelOffset);
            anno1.setTipRadius(tipRadius);
            anno1.setToolTipText(name + ": (Median: " + arr[2] + ", Count: " + arr[5] + ")");
            subplot1.addAnnotation(anno1);

            //add annotation for threshold in plot 1
            XYPointerAnnotation anno2 = new XYPointerAnnotation(arr[0].toString(), arr[0], arr[4], 5.5);
            anno2.setLabelOffset(labelOffset);
            anno2.setTipRadius(tipRadius);
            anno2.setToolTipText(name + ": (Threshold: " + arr[0] + ", Count: " + arr[4] + ")");
            subplot1.addAnnotation(anno2);

            //add annotation for median in plot 2
            XYPointerAnnotation anno3 = new XYPointerAnnotation(arr[3].toString(), arr[2], arr[3], 5.5);
            anno3.setLabelOffset(labelOffset);
            anno3.setTipRadius(tipRadius);
            anno3.setToolTipText(name + ": (Median: " + arr[2] + ", Reconstruction: " + arr[3] + ")");
            subplot2.addAnnotation(anno3);

            //add annotation for threshold in plot 2
            XYPointerAnnotation anno4 = new XYPointerAnnotation(arr[1].toString(), arr[0], arr[1], 5.5);
            anno4.setLabelOffset(labelOffset);
            anno4.setTipRadius(tipRadius);
            anno4.setToolTipText(name + ": (Threshold: " + arr[0] + ", Reconstruction: " + arr[1] + ")");
            subplot2.addAnnotation(anno4);

            annotationsMap.put(name, new XYPointerAnnotation[]{anno1, anno2, anno3, anno4});
        }

        MyCombinedDomainXYPlot plot = new MyCombinedDomainXYPlot(xAxis) {

            private static final long serialVersionUID = 1L;

            @Override
            public void panRangeAxes(double percent, PlotRenderingInfo info, Point2D source) {
                if (!isRangePannable()) {
                    return;
                }
                XYPlot subplot = findSubplot(info, source);
                if (subplot != null) {
                    for (int i = 0; i < subplot.getRangeAxisCount(); i++) {
                        ValueAxis rangeAxis = subplot.getRangeAxis(i);
                        rangeAxis.pan(percent);
                    }
                }
            }
        };
        plot.setGap(10);
        plot.add(subplot1, 1);
        plot.add(subplot2, 1);
        plot.setOrientation(PlotOrientation.VERTICAL);
        plot.setDomainPannable(true);
        plot.setRangePannable(true);
        plot.setDrawingSupplier(new MyDrawingSupplier());

        StandardXYItemRenderer renderer1 = new StandardXYItemRenderer(StandardXYItemRenderer.LINES, new StandardXYToolTipGenerator());
        int numSeries = primaryDataset.getSeriesCount();
        for(int i=0; i<numSeries; i++)
        {
            renderer1.setSeriesStroke(i, new BasicStroke(2.0f));
        }

        renderer1.setPlotDiscontinuous(true);
        renderer1.setGapThresholdType(UnitType.ABSOLUTE);
        renderer1.setGapThreshold(GAP_THRESHOLD);
        plot.setRenderer(renderer1); // use same renderer for all subplots so colors/shapes/strokes are the consistent for each series


        //SamplingXYLineAndShapeRenderer rend = new SamplingXYLineAndShapeRenderer(true, true);
        //rend.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
        //plot.setRenderer(rend);

        JFreeChart chart = new JFreeChart(title,
                JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        chart.setAntiAlias(false);
        chart.setTextAntiAlias(true);

        //chartDisplayed.setBackgroundPaint(Color.WHITE);
        LegendTitle legend = chart.getLegend();
        //legend.setBackgroundPaint(Color.LIGHT_GRAY);
        legend.setSources(new LegendItemSource[]{renderer1});// This is done to remove duplicated legend items.
        //legend.setSources(new LegendItemSource[]{rend});// This is done to remove duplicated legend items.

        coveragePlot = subplot1;
        reconstructionPlot = subplot2;
        return chart;
    }

    public int removeSeries(String prefix){
        List<XYSeries> series1 = primaryDataset.getSeries();
        List<XYSeries> series2 = secondaryDataset.getSeries();
        List<XYSeries> seriesToRemove1 = new ArrayList<XYSeries>();
        List<XYSeries> seriesToRemove2 = new ArrayList<XYSeries>();

        for(XYSeries s : series1){
//            Comparable key = s.getKey();
//            if(key instanceof String){
                String keyStr = (String) s.getKey();
                if(keyStr.startsWith(prefix)){
                    seriesToRemove1.add(s);

                    thresholdAndMedianMap.remove(keyStr);
                    colorMap.remove(keyStr);
                    pathsSet.remove(keyStr);

                    XYPointerAnnotation[] annos = annotationsMap.get(keyStr);
                    coveragePlot.removeAnnotation(annos[0], true);
                    coveragePlot.removeAnnotation(annos[1], true);
                    reconstructionPlot.removeAnnotation(annos[2], true);
                    reconstructionPlot.removeAnnotation(annos[3], true);
                    annotationsMap.remove(keyStr);
                }
//            }
        }

        for(XYSeries s : series2){
//            Comparable key = s.getKey();
//            if(key instanceof String){
                String keyStr = (String) s.getKey();
                if(keyStr.startsWith(prefix)){
                    seriesToRemove2.add(s);
                }
//            }
        }

        for(XYSeries s : seriesToRemove1){
            primaryDataset.removeSeries(s);
        }

        for(XYSeries s : seriesToRemove2){
            secondaryDataset.removeSeries(s);
        }

        //correct the series colors
        XYPlot plot = chartDisplayed.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        int numSeries = primaryDataset.getSeriesCount();
        for(int i=0; i< numSeries; i++){
            String path = (String) primaryDataset.getSeriesKey(i);
            if(path != null){
                Color c = colorMap.get(path);
                if(c != null){
                    renderer.setSeriesPaint(i, c);
                }
            }
        }        
        
        return primaryDataset.getSeriesCount();
    }

    private void zoomSpecial() {
        if (maxMedian > 0 && maxCount > 0 && maxReconstruction > 0) {
            double maxY1 = maxCount * 1.3;
            double maxX = maxMedian * 2;
            double maxY2 = maxReconstruction * 1.3;

            xAxis.setRange(0, maxX);
            yAxis1.setRange(0, maxY1);
            yAxis2.setRange(0, maxY2);
        }
    }

    private void autoRange() {
        xAxis.setAutoRange(true);
        yAxis1.setAutoRange(true);
        yAxis2.setAutoRange(true);
    }

    private void previousZoom() {
        if (lastXAxisZoom != null) {
            xAxis.setRange(lastXAxisZoom);
        }
        if (lastYAxis1Zoom != null) {
            yAxis1.setRange(lastYAxis1Zoom);
        }
        if (lastYAxis2Zoom != null) {
            yAxis2.setRange(lastYAxis2Zoom);
        }
    }

    private JMenuBar setUpMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu menu = new JMenu("Zoom");

        JMenuItem item = new JMenuItem("Previous Zoom");
        //item.setMnemonic(KeyEvent.VK_P); // won't work unless the mouse focus is in the menu; need to use the keylistener in the frame
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                previousZoom();
            }
        });
        menu.add(item);

        item = new JMenuItem("Zoom Special");
        //item.setMnemonic(KeyEvent.VK_S);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                zoomSpecial();
            }
        });
        menu.add(item);

        item = new JMenuItem("Auto Range");
        //item.setMnemonic(KeyEvent.VK_A);
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                autoRange();
            }
        });
        menu.add(item);
        bar.add(menu);

        return bar;
    }

    /*
     * Calculate the threshold and median k-mer coverage and their corresponding reconstruction and count.
     *
     * @param name  the assembly name eg. "k57"
     * @return results in an array:
     * 			[0]: threshold
     *          [1]: reconstruction at threshold
     *          [2]: median
     *          [3]: reconstuction at median
     *          [4]: count at threshold
     *          [5]: count at median
     */
    private Long[] getCoverageThresholdAndMedianKmerCoverage(String name) {
        //String name = getAssemblyNameFromPath(path);
        int seriesIndex = primaryDataset.indexOf(name);

        int iFLM = getIndexOfMinKmerCoverage(seriesIndex);
        System.out.println("Minimum k-mer Coverage: " + primaryDataset.getXValue(seriesIndex, iFLM));
        int startIndex = iFLM;

        int endIndex = primaryDataset.getItemCount(seriesIndex) - 1;

        long oldThreshold = (long) primaryDataset.getXValue(seriesIndex, iFLM);

        int repeat = 100;
        int iMedian = -1;
        long sumFromThreshold = 0;
        //int sumFromMedian = 0;
        Long median = null;
        while (repeat > 0) {
            //sumFromThreshold = getSum(seriesIndex, startIndex, endIndex);
            //System.out.println("Coverage:" + oldThreshold + ", Reconstruction: " + sumFromThreshold);

            iMedian = getIndexOfMedian(seriesIndex, startIndex, endIndex) - 1;
            median = primaryDataset.getX(seriesIndex, iMedian).longValue();
            long newThreshold = (long) Math.rint(Math.sqrt(median));

            if (oldThreshold == newThreshold) {
                //converged

                //System.out.println("Coverage Threshold: " + Math.rint(oldThreshold));
                //System.out.println("Median k-mer coverage: " + median);
                //System.out.println("Reconstruction: " + sumFromThreshold);
                break;
            }

            oldThreshold = newThreshold;
            startIndex = getIndexOfElementLargerThanThreshold(seriesIndex, oldThreshold);

            repeat--;
        }

        long threshold = (long) Math.rint(oldThreshold);
        long thresholdCount = 0;

        for (int i = 0; i <= iMedian; i++) {
            long x = primaryDataset.getX(seriesIndex, i).longValue();
            if (x >= threshold) {
                thresholdCount = primaryDataset.getY(seriesIndex, i).longValue();

                // this is done because the reconstruction values were added backwards (opposite direction of the way k-mer coverage data was added)
                int index = secondaryDataset.getSeries(seriesIndex).getItemCount() - i - 1;

                sumFromThreshold = secondaryDataset.getY(seriesIndex, index).longValue();
                break;
            }
        }

        long sumFromMedian = getSum(seriesIndex, iMedian, endIndex);
        long medianCount = primaryDataset.getY(seriesIndex, iMedian).longValue();

        Long[] results = new Long[]{threshold, sumFromThreshold, median, sumFromMedian, thresholdCount, medianCount};
        thresholdAndMedianMap.put(name, results);

        if (median > maxMedian) {
            maxMedian = median;
        }

        long maxCountInThisSeries = Math.max(medianCount, thresholdCount);
        if (maxCountInThisSeries > maxCount) {
            maxCount = maxCountInThisSeries;
        }
        if (sumFromThreshold > maxReconstruction) {
            maxReconstruction = sumFromThreshold;
        }

        return results;
    }

    private int getIndexOfElementLargerThanThreshold(int seriesIndex, double threshold) {
        int len = primaryDataset.getItemCount(seriesIndex);
        for (int i = 0; i <= len; i++) {
            if (primaryDataset.getXValue(seriesIndex, i) >= threshold) {
                return i;
            }
        }
        return -1;
    }

    private int getIndexOfMinKmerCoverage(int seriesIndex) {
        int len = primaryDataset.getItemCount(seriesIndex);
        double min = primaryDataset.getYValue(seriesIndex, 0);

        int theIndex = 0;
        int count = 0;
        for (int i = 1; i < len; i++) {
            double curr = primaryDataset.getYValue(seriesIndex, i);
            if (Double.compare(curr, min) < 0) {
                theIndex = i;
                min = curr;
                count = 0;
            } else if (++count >= 4) {
                break;
            }
        }
        return theIndex;
    }

    private int getIndexOfMedian(int seriesIndex, int firstIndex, int lastIndex) {
        long currSum = 0;
        long half = (getSum(seriesIndex, firstIndex, lastIndex)) / 2;
        int i = 0;
        for (i = firstIndex; i < lastIndex && currSum < half; i++) {
            currSum += primaryDataset.getYValue(seriesIndex, i);
        }
        return i;
    }

    private long getSum(int seriesIndex, int firstIndex, int lastIndex) {
        long sum = 0;
        for (int i = firstIndex; i <= lastIndex; i++) {
            sum += primaryDataset.getYValue(seriesIndex, i);
        }
        return sum;
    }

    public JPanel drawPlot(Map<String, Color> map) throws InterruptedException {
        int len = map.size();
        int count = 0;
        Set<String> keys = map.keySet();
        for (String name : keys){
            if(Thread.interrupted()){
                throw new java.lang.InterruptedException();
            }
            
            File f = new File(name);
            if (f.exists()) {
                try {
                    String path = name;//f.getCanonicalPath();
                    System.out.println("Reading file: " + path);
                    readHistFile(path);
                } catch (IOException e) {
                    //e.printStackTrace();
                    System.out.println(e.getMessage());
                    count++;
                }
            } else {
                System.out.println("ERROR: The file '" + name + "' does not exist.");
                count++;
            }
        }

        if(len == count)
        {
            //throw new Exception("No files could be read correctly. Chart cannot be drawn.");
            return null;
        }

        chartDisplayed = createChart();
        XYPlot plot = chartDisplayed.getXYPlot();
        XYItemRenderer renderer = plot.getRenderer();
        int numSeries = primaryDataset.getSeriesCount();
        for(int i=0; i< numSeries; i++){
            String path = (String) primaryDataset.getSeriesKey(i);
            if(path != null){
                Color c = map.get(path);
                if(c != null){
                    renderer.setSeriesPaint(i, c);
                }
            }
        }

        colorMap = map;

        final MyChartPanel chartPanel = new MyChartPanel(chartDisplayed);
        chartPanel.setZoomAroundAnchor(true);
        chartPanel.setMouseWheelEnabled(true);

        // This is done so the labels do not stretch on resize
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

    public void addDomainMarker(double xVal){
        if(chartDisplayed != null){
            MyCombinedDomainXYPlot plot = (MyCombinedDomainXYPlot) chartDisplayed.getXYPlot();
            List<XYPlot> plots = plot.getSubplots();
            for(XYPlot p : plots){
                p.clearDomainMarkers();
            }

            if(xVal >= 0){ // zero coverage is possible
                ValueMarker marker = new ValueMarker(xVal);
                marker.setPaint(new Color(51,160,44));//new Color(236, 112, 20));
                marker.setStroke(new BasicStroke(2.0F));
                for(XYPlot p : plots){
                    p.addDomainMarker(marker);
                }

                double maxX = maxMedian * 2;
//                xAxis.setAutoRange(true);
//                double autoMax = xAxis.getUpperBound();

                if(xVal <= maxX){
                    xAxis.setUpperBound(maxX);
                }
                else{
                    xAxis.setUpperBound(xVal/0.9);
                }

//                else if(xVal <= autoMax){
//                    xAxis.setUpperBound(autoMax);
//                }
//                else {
//                    xAxis.setUpperBound(xVal);
//                }
            }
        }
    }

    public void removeAllDomainMarkers(){
        if(chartDisplayed != null){
            MyCombinedDomainXYPlot plot = (MyCombinedDomainXYPlot) chartDisplayed.getXYPlot();
            List<XYPlot> plots = plot.getSubplots();
            for(XYPlot p : plots){
                p.clearDomainMarkers();
            }
        }
    }

//    public JInternalFrame plot(ArrayList<String> filenames) throws Exception {
//        int len = filenames.size();
//        int count = 0;
//        for (int i = 0; i < len; i++) {
//            String name = filenames.get(i);
//            File f = new File(name);
//            if (f.exists()) {
//                try {
//                    String path = f.getCanonicalPath();
//                    System.out.println("Reading file: " + path);
//                    readHistFile(path);
//                } catch (IOException e) {
//                    //e.printStackTrace();
//                    System.out.println(e.getMessage());
//                    count++;
//                }
//            } else {
//                System.out.println("ERROR: The file '" + name + "' does not exist.");
//                count++;
//            }
//        }
//
//        if(len == count)
//        {
//            throw new Exception("No files could be read correctly. Chart cannot be drawn.");
//        }
//
//        return displayChartInInternalFrame();
//    }

//    public void help() throws IOException
//    {
//        InputStream is = getClass().getResourceAsStream("/help/help.txt");
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
//
//    public void version() throws IOException
//    {
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
//
//    /**
//     * @param args
//     */
//
//    public static void main(String[] args) {
//        int cursor = 0;
//        int numArgs = args.length;
//
//        if (args.length <= 0 || args[0].equals("--help"))
//        {
//            CoverageHistogram c = new CoverageHistogram();
//            try {
//                c.help();
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//                System.exit(0);
//            }
//        }
//        else if (args[0].equals("--version"))
//        {
//            CoverageHistogram c = new CoverageHistogram();
//            try {
//                c.version();
//            } catch (IOException e) {
//                System.out.println(e.getMessage());
//                System.exit(0);
//            }
//        }
//
//        CoverageHistogram c = new CoverageHistogram();
//
//        int numErrors = 0;
//        for(int i=cursor; i<numArgs; i++)
//        {
//            File f = new File(args[i]);
//            if(f.exists())
//            {
//                try {
//                    String path = f.getCanonicalPath();
//                    System.out.println("Reading file: " + path);
//                    c.readHistFile(path);
//                } catch (IOException e) {
//                    //e.printStackTrace();
//                    System.out.println(e.getMessage());
//                    numErrors++;
//                }
//            }
//            else
//            {
//                System.out.println("This file does not exist: " + args[i]);
//                numErrors++;
//            }
//        }
//
//        if(numErrors == numArgs)
//        {
//             System.out.println("ERROR: No files were read. CoverageHistogram is terminated.");
//             System.exit(-1);
//        }
//
//        c.displayChartInFrame();
//
//    }

}
