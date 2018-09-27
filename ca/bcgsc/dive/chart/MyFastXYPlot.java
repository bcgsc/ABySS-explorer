package ca.bcgsc.dive.chart;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CrosshairState;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.SeriesRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.RendererUtilities;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRendererState;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleEdge;

/**
 * Based on FastXYPlot written by: Lindsay Pender (original author)
 *                                 nonlinear5 (last known contributor 1)
 *                                 CameO73 (last known contributor 2)
 *                     taken from: http://www.jfree.org/phpBB2/viewtopic.php?f=3&t=18592&hilit=fast+classes&start=45
 *                     January 2010
 *
 * FastXYPlot is good for cases when the plot is a simple scatter plot.
 * However, multiple bugs arise when the data points are connected with lines.
 *
 * MyFastXYPlot contains these fixes and updates:
 * 1. An item just outside of the plotting area is included to the list of
 *    "live items". This ensures that lines are drawn from the edge item to the
 *    edge of the plot if such lines exists.
 * 2. First, all items in every series are compared with the items in their
 *    layers and the layers above. Then, all items are rendered accordingly.
 * 3. The comparison is done on each item and its previous item in the series.
 *    This ensures that the line connecting the item and the previous item will
 *    be drawn if the item would be covered by another item in the layers above.
 *
 * @author Ka Ming Nip
 * Canada's Michael Smith Genome Sciences Centre
 * Updated on Mon 29 Mar 2010 11:46 PM
 */
public class MyFastXYPlot extends XYPlot {

    private static final long serialVersionUID = 201003291134L;
    
    //KEY: series number
    //VALUE: set of pixels to be rendered for the items in the series
    private final HashMap<Integer, HashSet<Integer>> renderedPixelsMap = new HashMap<Integer, HashSet<Integer>>();

    public MyFastXYPlot(XYDataset dataset, ValueAxis domainAxis, ValueAxis rangeAxis, XYItemRenderer renderer) {
        super(dataset, domainAxis, rangeAxis, renderer);
    }

    /*
     * Determines if the item is to be rendered.
     */
    private boolean toBeRendered(XYDataset dataset, ValueAxis xAxis, ValueAxis yAxis, RectangleEdge domainEdge,
            RectangleEdge rangeEdge, Rectangle2D dataArea, int series, int item) {
        boolean toBeRendered = false;

        int width = (int) dataArea.getWidth();

        double xValue = dataset.getXValue(series, item);
        double yValue = dataset.getYValue(series, item);
        int x = (int) xAxis.valueToJava2D(xValue, dataArea, domainEdge);
        int y = (int) yAxis.valueToJava2D(yValue, dataArea, rangeEdge);
        int itemKey = x + width * y;

        if (renderedPixelsMap.get(series).contains(itemKey) ){
            toBeRendered = true;
        }

        return toBeRendered;
    }

    /*
     * Determines if the item is to be rendered in the location of the plot where another
     * item in the same layer or the layer above has already been rendered. If pixel is not
     * occupied, then reserve this pixel for this item.
     */
    private void checkPixelForItem(XYDataset dataset, ValueAxis xAxis, ValueAxis yAxis, RectangleEdge domainEdge,
            RectangleEdge rangeEdge, Rectangle2D dataArea, int series, int item){
        int width = (int) dataArea.getWidth();

        double xValue = dataset.getXValue(series, item);
        double yValue = dataset.getYValue(series, item);
        int x = (int) xAxis.valueToJava2D(xValue, dataArea, domainEdge);
        int y = (int) yAxis.valueToJava2D(yValue, dataArea, rangeEdge);
        int itemKey = x + width * y;

        int itemKey2 = Integer.MIN_VALUE;
        if(item>0)
        {
            double xValue2 = dataset.getXValue(series, item-1);
            double yValue2 = dataset.getYValue(series, item-1);
            int x2 = (int) xAxis.valueToJava2D(xValue2, dataArea, domainEdge);
            int y2 = (int) yAxis.valueToJava2D(yValue2, dataArea, rangeEdge);
            itemKey2 = x2 + width * y2;
        }

        // if the map does not have a set of pixels for this series, then put an empty set for this series in the map
        if (!renderedPixelsMap.containsKey(series))
        {
            renderedPixelsMap.put(series, new HashSet<Integer>());
        }

        SeriesRenderingOrder seriesOrder = getSeriesRenderingOrder();
        int seriesCount = dataset.getSeriesCount();
        if (seriesOrder == SeriesRenderingOrder.REVERSE) {

            // smaller series # is on a higher layer
            for (int s = 0; s <= series; s++) {
                HashSet set = renderedPixelsMap.get(s);

                /* If the set of pixels for the series exists and
                 * the pixels for the current and previous items are reserved
                 * then don't do anything
                 */
                if(set != null && set.contains(itemKey) && set.contains(itemKey2)) {
                    return;
                }
            }
        }
        else
        {
            // bigger series # is on a higehr layer
            for (int s = seriesCount - 1; s >= series; s--) {
                HashSet set = renderedPixelsMap.get(s);
                if(set != null && set.contains(itemKey) && set.contains(itemKey2)) {
                    return;
                }
            }
        }
        
        HashSet<Integer> seriesSet = renderedPixelsMap.get(series);
        // reserve the pixel for the current item
        seriesSet.add(itemKey);

        // reserve the pixel for the previous item if it exists
        if(item>0){
            seriesSet.add(itemKey2);
        }

        // reserve the pixel for the next item if it exists
        if(item+1 < dataset.getItemCount(series))
        {
            double xValue3 = dataset.getXValue(series, item+1);
            double yValue3 = dataset.getYValue(series, item+1);
            int x3 = (int) xAxis.valueToJava2D(xValue3, dataArea, domainEdge);
            int y3 = (int) yAxis.valueToJava2D(yValue3, dataArea, rangeEdge);
            int itemKey3 = x3 + width * y3;
            seriesSet.add(itemKey3);
        }
    }

    /*
     * 
     */
    private void checkPixelForSeries(XYDataset dataset, ValueAxis xAxis, ValueAxis yAxis, RectangleEdge domainEdge,
            RectangleEdge rangeEdge, Rectangle2D dataArea, int series, XYItemRendererState state){
        int firstItem = 0;
        int lastItem = dataset.getItemCount(series) - 1;
        if (lastItem == -1) {
            return;
        }
        if (state.getProcessVisibleItemsOnly()) {
            int[] itemBounds = RendererUtilities.findLiveItems(
                    dataset, series, xAxis.getLowerBound(),
                    xAxis.getUpperBound());
            firstItem = itemBounds[0];
            lastItem = itemBounds[1];

            /*Adds an extra item at the end so lines continue to the edge of plotting area*/
            if (lastItem < dataset.getItemCount(series) - 1) {
                lastItem++;
            }
        }

        for (int item = firstItem; item <= lastItem; item++) {
            checkPixelForItem(dataset, xAxis, yAxis, domainEdge, rangeEdge, dataArea, series, item);
        }
    }

    @Override
    public boolean render(Graphics2D g2,
            Rectangle2D dataArea,
            int index,
            PlotRenderingInfo info,
            CrosshairState crosshairState) {


        boolean foundData = false;
        boolean disableOptimization = false;

        XYDataset dataset = getDataset(index);

        if (!DatasetUtilities.isEmptyOrNull(dataset)) {
            foundData = true;
            ValueAxis xAxis = getDomainAxisForDataset(index);
            ValueAxis yAxis = getRangeAxisForDataset(index);
            XYItemRenderer renderer = getRenderer(index);
            if (renderer == null) {
                renderer = getRenderer();
                if (renderer == null) { // no default renderer available
                    return foundData;
                }
            }

            XYItemRendererState state = renderer.initialise(g2, dataArea, this,
                    dataset, info);
            int passCount = renderer.getPassCount();

            if (renderer instanceof XYLineAndShapeRenderer) {
                disableOptimization = ((XYLineAndShapeRenderer) renderer).getDrawSeriesLineAsPath();   // in case of e.g. splines
            }

            RectangleEdge domainEdge = getDomainAxisEdge();
            RectangleEdge rangeEdge = getDomainAxisEdge();

            SeriesRenderingOrder seriesOrder = getSeriesRenderingOrder();
            if (seriesOrder == SeriesRenderingOrder.REVERSE) {
                //render series in reverse order
                for (int pass = 0; pass < passCount; pass++) {
                    renderedPixelsMap.clear();                     // need to clear every pass or else shapes won't be drawn correctly
                    int seriesCount = dataset.getSeriesCount();

                    if(!disableOptimization) {
                        for (int series = 0; series < seriesCount; series++) {
                            checkPixelForSeries(dataset, xAxis, yAxis, domainEdge, rangeEdge, dataArea, series, state);
                        }
                    }
                    
                    for (int series = seriesCount - 1; series >= 0; series--) {
                        int firstItem = 0;
                        int lastItem = dataset.getItemCount(series) - 1;
                        if (lastItem == -1) {
                            continue;
                        }
                        if (state.getProcessVisibleItemsOnly()) {
                            int[] itemBounds = RendererUtilities.findLiveItems(
                                    dataset, series, xAxis.getLowerBound(),
                                    xAxis.getUpperBound());
                            firstItem = itemBounds[0];
                            lastItem = itemBounds[1];

                            /* Adds an extra item at the end so lines continue to the edge of plotting area*/
                            if (lastItem < dataset.getItemCount(series) - 1) {
                                lastItem++;
                            }
                        }
                        for (int item = firstItem; item <= lastItem; item++) {
                            if (disableOptimization || toBeRendered(dataset, xAxis, yAxis, domainEdge, rangeEdge, dataArea, series, item)) {
                                renderer.drawItem(g2, state, dataArea, info,
                                        this, xAxis, yAxis, dataset, series, item,
                                        crosshairState, pass);
                            }
                        }
                    }
                }
            }
            else {
                //render series in forward order
                for (int pass = 0; pass < passCount; pass++) {
                    renderedPixelsMap.clear();                     // need to clear every pass or else shapes won't be drawn correctly
                    int seriesCount = dataset.getSeriesCount();

                    if(!disableOptimization) {
                        for (int series = seriesCount - 1; series >= 0; series--) {
                            checkPixelForSeries(dataset, xAxis, yAxis, domainEdge, rangeEdge, dataArea, series, state);
                        }
                    }
                    
                    for (int series = 0; series < seriesCount; series++) {
                        int firstItem = 0;
                        int lastItem = dataset.getItemCount(series) - 1;
                        if (lastItem == -1) {
                            continue;
                        }
                        if (state.getProcessVisibleItemsOnly()) {
                            int[] itemBounds = RendererUtilities.findLiveItems(
                                    dataset, series, xAxis.getLowerBound(),
                                    xAxis.getUpperBound());
                            firstItem = itemBounds[0];
                            lastItem = itemBounds[1];

                            /*Adds an extra item at the end so lines continue to the edge of plotting area*/
                            if (lastItem < dataset.getItemCount(series) - 1) {
                                lastItem++;
                            }
                        }
                        for (int item = firstItem; item <= lastItem; item++) {
                            if (disableOptimization || toBeRendered(dataset, xAxis, yAxis, domainEdge, rangeEdge, dataArea, series, item)) {
                                renderer.drawItem(g2, state, dataArea, info,
                                        this, xAxis, yAxis, dataset, series, item,
                                        crosshairState, pass);
                            }
                        }
                    }
                }
            }
        }
        return foundData;
    }
}
