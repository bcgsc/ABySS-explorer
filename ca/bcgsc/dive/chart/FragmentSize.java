package ca.bcgsc.dive.chart;

/**
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 * Updated on June 22, 2010
 */

import java.awt.BasicStroke;
import ca.bcgsc.dive.stat.HistogramStats;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.util.UnitType;

import ca.bcgsc.dive.util.*;
import java.awt.Color;
import java.awt.GridBagLayout;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.ui.Layer;



public class FragmentSize {
	
	private HashMap<String, String> nameToPathMap = new HashMap<String, String>();
	private XYSeriesCollection dataset = null;
	private HashMap<String, HistogramStats> statsMap = new HashMap<String, HistogramStats>();
	
	private Range lastXAxisZoom = null;
	private Range lastYAxisZoom = null;
	private Range currXAxisZoom = null;
	private Range currYAxisZoom = null;
	private NumberAxis xAxis = null;
	private NumberAxis yAxis = null;
        private JFreeChart chartDisplayed = null;
	
	//private DefaultTableModel model = null;
	
	public FragmentSize()
	{
		dataset = new XYSeriesCollection();
	}
        
	/**
	 * @pre the file at the path specified must exist
	 */
	public void readHistFile(String path) throws IOException
	{
//            String name = Utilities.getAssemblyNameFromPath(path);
            String name = null;
            File f = new File(path);
            name = f.getName().substring(0, f.getName().length()-7); // trim away "-3.hist" at the end

            if(name == null)
            {
                    System.out.println("ERROR: The canonical path of the file must be in the format \"/.../k???/???.hist\"\nThis file was not read: " + path);
                    return;
            }

            // Check whether the abbreviation (name) already exist...
            if(nameToPathMap.containsValue(path))
            {
                    return; // this file has already been read; don't do anything
            }
            else if(nameToPathMap.containsKey(name))
            {
                    //get the path that links to the existing abbreviation
                    String thePathInMap = nameToPathMap.get(name);

                    //rename the keys in all maps with the path
                    nameToPathMap.put(thePathInMap, thePathInMap);

                    //remove the pairs that used abbreviation in all maps
                    nameToPathMap.remove(name);

                    // use path as name for the current file
                    nameToPathMap.put(path, path);
                    name = path;
            }
            else
            {
                    // use abbreviation as name
                    nameToPathMap.put(name, path);
            }

            ArrayList<Long> counts = new ArrayList<Long>();
            ArrayList<Long> bins = new ArrayList<Long>();

            try{
		    BufferedReader br = new BufferedReader(new FileReader(path));
	        String line = null;
	        
	        while (( line = br.readLine()) != null)
	        {        	
	        	String[] arr = line.split("\t");
	        	Long x = new Long(arr[0]);
	        	Long y = new Long(arr[1]);
	        	//s.add(x, y);
	        	
	        	bins.add(x);
	        	counts.add(y);       	
	        }
	        
	        HistogramStats stats = new HistogramStats(bins, counts, true);
                bins = stats.getBins();
                counts = stats.getCounts();
	        statsMap.put(path, stats);
	        
	        int lowerBoundIndex = stats.getLowerBoundIndex();
	        int upperBoundIndex = stats.getUpperBoundIndex();
	        XYSeries s = new XYSeries(name, false, true);
	        
	        for(int i= lowerBoundIndex; i<=upperBoundIndex; i++)
	        {
	        	s.add(bins.get(i),counts.get(i));
	        }
	        dataset.addSeries(s);
                
                HistogramStats statsForNegative = stats.getStatsForNegativeData();
                if(statsForNegative != null){                    

                    XYSeries s2 = new XYSeries(name+"(RF)", false, true);
                    s.setKey(name+"(FR)");

                    int indexOfSmallestPositiveSize = stats.getIndexOfSmallestPositiveSize();
                    int start = indexOfSmallestPositiveSize - statsForNegative.getUpperBoundIndex()-1;
                    int end = indexOfSmallestPositiveSize - statsForNegative.getLowerBoundIndex()-1;

//                    if(lowerBoundIndex > 0){
//                        if(bins.get(lowerBoundIndex-1) == 0L){
//                            // need to shift one bin
//                            start--;
//                            end--;
//                        }
//                    }

                    for(int i= start; i<=end; i++)
                    {
                            s2.add(bins.get(i),counts.get(i));
                    }
                    dataset.addSeries(s2);
                }
	        
	        br.close();
		}
            catch (IOException ex) {
                    throw ex;
            }
	}
	
	private DefaultTableModel fillStatsTable()
	{
            final int numSeries = nameToPathMap.size();
            final String[] columnNames = new String[]{"Assembly",
                                                      "Min",
                                                      "Q1",
                                                      "Median",
                                                      "Q3",
                                                      "Max",
                                                      "Mean",
                                                      //"Variance",
                                                      "Stdev",
                                                      "Stdev/Mean",
                                                      "Q factor"};
            DefaultTableModel model = new DefaultTableModel(columnNames,numSeries)
            {
                private static final long serialVersionUID = 1L;

                @Override
                public Class<?> getColumnClass(int column) {
                    // this is done so numeric columns can be sorted correctly (by numerical value instead of alphabetical value)
                    if (column > 0 && column <5)
                        return Long.class;
                    else if(column >=5)
                            return Double.class;

                    return super.getColumnClass(column);
                }

                @Override
                    public boolean isCellEditable(int rowIndex, int columnIndex)
                    {
                        return false;
                    }
            };

            Set<String> keys = nameToPathMap.keySet();
            Iterator<String> itr = keys.iterator();
            int rowIndex = 0;
            while(itr.hasNext())
            {
                    String name = itr.next();
                    HistogramStats stats = statsMap.get(nameToPathMap.get(name));

                    model.setValueAt(name,rowIndex,0); //assembly
                    model.setValueAt(stats.getMin(),rowIndex,1); //min
                    model.setValueAt(stats.getQ1(),rowIndex,2); //Q1
                    model.setValueAt(stats.getMedian(),rowIndex,3); //median
                    model.setValueAt(stats.getQ3(),rowIndex,4); //Q3
                    model.setValueAt(stats.getMax(),rowIndex,5); //max
                    double mean = stats.getMean();
                    model.setValueAt(mean,rowIndex,6); //mean
                    //model.setValueAt(stats.getVariance(),rowIndex,7); //variance
                    double stdev = stats.getStdev();
                    model.setValueAt(stdev,rowIndex,7); //stdev
                    model.setValueAt(stdev/mean, rowIndex, 8); // stdev/mean
                    model.setValueAt(stats.getQFactor(), rowIndex, 9);
                    rowIndex++;
            }

            return model;
	}
	
	private final class MyDoubleCellRenderer extends DefaultTableCellRenderer
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
	
	private final class MyPercentCellRenderer extends DefaultTableCellRenderer
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
	
        private JFreeChart createChart()
        {
            double GAP_THRESHOLD = 1.0;

            String title = null;
            String xAxisLabel = "Fragment Size";
            String yAxisLabel = "Count";

            yAxis = new NumberAxis(yAxisLabel);
            yAxis.addChangeListener(new AxisChangeListener(){
                    public void axisChanged(AxisChangeEvent e) {
                            lastYAxisZoom = currYAxisZoom;
                            currYAxisZoom = yAxis.getRange();				
                    }			
            });
            xAxis = new NumberAxis(xAxisLabel);
            xAxis.addChangeListener(new AxisChangeListener(){
                    public void axisChanged(AxisChangeEvent e) {
                            lastXAxisZoom = currXAxisZoom;
                            currXAxisZoom = xAxis.getRange();				
                    }			
            });

            yAxis.setAutoRangeIncludesZero(false);
            xAxis.setAutoRangeIncludesZero(false); 
        
            MyFastXYPlot plot = new MyFastXYPlot(dataset, xAxis, yAxis, null);
            plot.setDomainPannable(true);
            plot.setRangePannable(true);
            StandardXYItemRenderer renderer = new StandardXYItemRenderer(StandardXYItemRenderer.LINES, new StandardXYToolTipGenerator());        
            int numSeries = dataset.getSeriesCount();
            for(int i=0; i<numSeries; i++)
            {
                renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            }

            renderer.setPlotDiscontinuous(true);
            renderer.setGapThresholdType(UnitType.ABSOLUTE);
            renderer.setGapThreshold(GAP_THRESHOLD);
            plot.setRenderer(renderer);
            plot.setDrawingSupplier(new MyDrawingSupplier());

            lastYAxisZoom = yAxis.getRange();
            lastXAxisZoom = xAxis.getRange();
            currYAxisZoom = lastYAxisZoom;
            currXAxisZoom = lastXAxisZoom;

            JFreeChart chart = new JFreeChart(title,
            JFreeChart.DEFAULT_TITLE_FONT, plot, true);
            chart.setAntiAlias(false);
            chart.setTextAntiAlias(true);
            
            return chart;
        }

	public void displayChart()
	{
            chartDisplayed = createChart();
        //chartDisplayed.setBackgroundPaint(Color.WHITE);
        
		ChartPanel chartPanel = new ChartPanel(chartDisplayed);
		chartPanel.setZoomAroundAnchor(true);
		chartPanel.setMouseWheelEnabled(true);
		
        // This is done so the labels do not stretch on resize
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
      
        JPanel outerPanel = new JPanel(new BorderLayout());
        outerPanel.add(chartPanel, BorderLayout.CENTER);        
        
        DefaultTableModel model = fillStatsTable();
        JTable table = new JTable(model);
        table.setDefaultRenderer(Double.class, new MyDoubleCellRenderer());
        table.getColumnModel().getColumn(8).setCellRenderer(new MyPercentCellRenderer());
        table.getTableHeader().setReorderingAllowed(false);
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
        table.setRowSorter(sorter);        
        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(chartPanel.getWidth(),100));
        outerPanel.add(sp, BorderLayout.SOUTH);        
        
		JFrame frame = new JFrame("FragmentSize");
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(outerPanel);		
        frame.pack();
        frame.setFocusable(true);
        frame.addKeyListener(createKeyListenerForFrame());
        frame.setVisible(true);        
	}
    
    private void previousZoom()
    {
        if(lastXAxisZoom != null)
                xAxis.setRange(lastXAxisZoom);
        if(lastYAxisZoom != null)
                yAxis.setRange(lastYAxisZoom);
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

    public void help() throws IOException
    {
        InputStream is = getClass().getResourceAsStream("/help/help.txt");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) 
        {
        	System.out.println(line);
        }
        br.close();
        isr.close();
        is.close();
        System.exit(0);
    }
    
    public void version() throws IOException
    {
        InputStream is = getClass().getResourceAsStream("/help/version.txt");
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);
        String line;
        while ((line = br.readLine()) != null) 
        {
        	System.out.println(line);
        }
        br.close();
        isr.close();
        is.close();
        System.exit(0);
    }

    public JPanel drawPlot(String path, Color c)
    {
//        Iterator<String> itr = paths.iterator();
//        while(itr.hasNext())
//        {
//            String path = itr.next();
            File f = new File(path);
            if(f.exists())
            {
                try {
                    path = f.getCanonicalPath();
                    System.out.println("Reading file: " + path);
                    readHistFile(path);
                } catch (IOException e) {
                    //e.printStackTrace();
                    //numErrors++;
                    System.out.println(e.getMessage());
                }
            }
            else
            {
                //numErrors++;
                System.out.println("This file does not exist: " + path);
            }
//        }

        chartDisplayed = createChart();

        XYItemRenderer renderer = chartDisplayed.getXYPlot().getRenderer();
        renderer.setSeriesPaint(0, c);
        renderer.setSeriesPaint(1, Color.GRAY); // for negative data if it exists

        HistogramStats stats = statsMap.get(path);
        XYPlot plot = chartDisplayed.getXYPlot();
        ValueMarker medianMarker = new ValueMarker(stats.getMedian());
        Color medianColor = new Color(236, 112, 20);
        medianMarker.setPaint(medianColor);
        medianMarker.setStroke(new BasicStroke(2.0F));
        plot.addDomainMarker(medianMarker, Layer.FOREGROUND);

        IntervalMarker iqrMarker = new IntervalMarker(stats.getQ1(), stats.getQ3());
        Color iqrColor = new Color(158,202,225);
        iqrMarker.setOutlinePaint(iqrColor);
        iqrMarker.setPaint(iqrColor);
        plot.addDomainMarker(iqrMarker, Layer.BACKGROUND);

        HistogramStats stats2 = stats.getStatsForNegativeData();
        if(stats2 != null){
            ValueMarker medianMarker2 = new ValueMarker(stats2.getMedian()*(-1L));
            medianMarker2.setPaint(medianColor);
            medianMarker2.setStroke(new BasicStroke(2.0F));
            plot.addDomainMarker(medianMarker2, Layer.FOREGROUND);

            IntervalMarker iqrMarker2 = new IntervalMarker(stats2.getQ3()*(-1L), stats2.getQ1()*(-1L));
            iqrMarker2.setOutlinePaint(iqrColor);
            iqrMarker2.setPaint(iqrColor);
            plot.addDomainMarker(iqrMarker2, Layer.BACKGROUND);
        }

        final MyChartPanel chartPanel = new MyChartPanel(chartDisplayed);
        chartPanel.setZoomAroundAnchor(true);
        chartPanel.setMouseWheelEnabled(true);

        // This is done so the labels do not stretch on resize
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);

        LegendTitle legend = chartDisplayed.getLegend();
        legend.setVisible(false);

//        JPanel outerPanel = new JPanel(new BorderLayout());
//        outerPanel.add(chartPanel, BorderLayout.CENTER);
        JPanel outerPanel = new JPanel(new GridBagLayout());
        java.awt.GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        outerPanel.add(chartPanel, gridBagConstraints);

//        DefaultTableModel model = fillStatsTable();
//        JTable table = new JTable(model);
//        table.setDefaultRenderer(Double.class, new MyDoubleCellRenderer());
//        table.getColumnModel().getColumn(8).setCellRenderer(new MyPercentCellRenderer());
//        table.getTableHeader().setReorderingAllowed(false);
//
//        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
//        table.setRowSorter(sorter);
//        JScrollPane sp = new JScrollPane(table);
//        sp.setMinimumSize (new Dimension(0, table.getRowHeight()*2+table.getRowMargin()) );
////        outerPanel.add(sp, BorderLayout.SOUTH);
//        gridBagConstraints = new java.awt.GridBagConstraints();
//        gridBagConstraints.gridx = 0;
//        gridBagConstraints.gridy = 1;
//        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
//        gridBagConstraints.weightx = 1.0;
//        gridBagConstraints.weighty = 0.0;
//        outerPanel.add(sp, gridBagConstraints);

        chartPanel.setFocusable(true);
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

        return outerPanel;
    }


//    public JInternalFrame plot(ArrayList<String> paths)
//    {
//        Iterator<String> itr = paths.iterator();
//        while(itr.hasNext())
//        {
//            String path = itr.next();
//            File f = new File(path);
//            if(f.exists())
//            {
//                try {
//                    String cpath = f.getCanonicalPath();
//                    System.out.println("Reading file: " + cpath);
//                    readHistFile(cpath);
//                } catch (IOException e) {
//                    //e.printStackTrace();
//                    //numErrors++;
//                    System.out.println(e.getMessage());
//                }
//            }
//            else
//            {
//                //numErrors++;
//                System.out.println("This file does not exist: " + path);
//            }
//        }
//
//        chartDisplayed = createChart();
//
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
//        JPanel outerPanel = new JPanel(new BorderLayout());
//        outerPanel.add(chartPanel, BorderLayout.CENTER);
//
//        DefaultTableModel model = fillStatsTable();
//        JTable table = new JTable(model);
//        table.setDefaultRenderer(Double.class, new MyDoubleCellRenderer());
//        table.getColumnModel().getColumn(8).setCellRenderer(new MyPercentCellRenderer());
//        table.getTableHeader().setReorderingAllowed(false);
//
//        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
//        table.setRowSorter(sorter);
//        JScrollPane sp = new JScrollPane(table);
//        sp.setPreferredSize(new Dimension(chartPanel.getWidth(),100));
//        outerPanel.add(sp, BorderLayout.SOUTH);
//
//        JInternalFrame frame = new JInternalFrame("Fragment Size Distribution");
//
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.getContentPane().add(outerPanel);
//        frame.pack();
//        frame.setFocusable(true);
//        frame.addKeyListener(createKeyListenerForFrame());
//        frame.setVisible(true);
//
//        return frame;
//    }

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int numArgs = args.length;
		
        if (args.length <= 0 || args[0].equals("--help"))
        {
        	FragmentSize f = new FragmentSize();
        	try {
                        f.help();
                } catch (IOException e) {
                        System.out.println(e.getMessage());
                        System.exit(0);
                }
        }
        else if (args[0].equals("--version"))
        {
        	FragmentSize f = new FragmentSize();
        	try {
                        f.version();
                } catch (IOException e) {
                        System.out.println(e.getMessage());
                        System.exit(0);
                }
        }		
		
		FragmentSize fsd = new FragmentSize();
		int numErrors = 0;
		for(int i=0; i<numArgs; i++)
		{			
			File f = new File(args[i]);
			if(f.exists())
			{			
				try {
					String path = f.getCanonicalPath();				
					System.out.println("Reading file: " + path);					
					fsd.readHistFile(path);
				} catch (IOException e) {				
					//e.printStackTrace();
					numErrors++;
					System.out.println(e.getMessage());
				}
			}
			else
			{
				numErrors++;
				System.out.println("This file does not exist: " + args[i]);
			}
		}
		
		if(numErrors < numArgs)
			fsd.displayChart();
		else
			System.out.println("ERROR: No files were read. FragmentSize is terminated.");
	}

}
