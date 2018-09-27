package ca.bcgsc.dive.chart;

import java.awt.geom.Point2D;
import java.util.Iterator;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.chart.plot.XYPlot;

/**
 * Class for fixing the bug in CombinedDomainXYPlot of JFreeChart.
 * @author kmnip
 */
public class MyCombinedDomainXYPlot  extends CombinedDomainXYPlot{

        public MyCombinedDomainXYPlot(ValueAxis domainAxis) {
            super(domainAxis);
        }

        @Override
        public void zoomRangeAxes(double factor, PlotRenderingInfo state,
                                  Point2D source, boolean useAnchor) {
            // delegate 'state' and 'source' argument checks...
            XYPlot subplot = findSubplot(state, source);
            if (subplot != null) {
                //subplot.zoomRangeAxes(factor, state, source, useAnchor);
                subplot.zoomRangeAxes(factor, state.getSubplotInfo(state.getSubplotIndex(source)), source, useAnchor);
            }
            else {
                // if the source point doesn't fall within a subplot, we do the
               // zoom on all subplots...
                Iterator iterator = getSubplots().iterator();
                while (iterator.hasNext()) {
                    subplot = (XYPlot) iterator.next();
                    subplot.zoomRangeAxes(factor, state, source, useAnchor);
                }
            }
        }
    

}
