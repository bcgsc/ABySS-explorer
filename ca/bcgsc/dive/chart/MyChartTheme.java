/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.chart;

import java.awt.Color;
import org.jfree.chart.StandardChartTheme;

/**
 *
 * @author kmnip
 */
public final class MyChartTheme extends StandardChartTheme{

    public MyChartTheme()
    {
        super("light");
        this.setChartBackgroundPaint(Color.LIGHT_GRAY);
        this.setPlotBackgroundPaint(Color.WHITE);
        this.setDomainGridlinePaint(Color.LIGHT_GRAY);
        this.setRangeGridlinePaint(Color.LIGHT_GRAY);
        this.setDrawingSupplier(new MyDrawingSupplier());
    }
}
