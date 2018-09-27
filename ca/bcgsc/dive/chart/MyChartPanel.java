/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.dive.chart;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.Pannable;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.Zoomable;

/**
 *
 * @author kmnip
 */
public class MyChartPanel extends ChartPanel{

    private static int ZOOM_MASK = InputEvent.SHIFT_MASK;
    private transient Rectangle2D zoomRectangle = null;
    private double panW, panH;
    private Point panLast;
    private Point2D zoomPoint = null;
    private boolean useBuffer = ChartPanel.DEFAULT_BUFFER_USED;



    public MyChartPanel(JFreeChart chart){
        super(chart, ChartPanel.DEFAULT_BUFFER_USED);
    }

        /**
         * Handles a 'mouse pressed' event.
         * <P>
         * This event is the popup trigger on Unix/Linux.  For Windows, the popup
         * trigger is the 'mouse released' event.
         *
         * @param e  The mouse event.
         */
        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                if (this.getPopupMenu() != null) {
                    displayPopupMenu(e.getX(), e.getY());
                    return;
                }
            }
            
            Plot plot = this.getChart().getPlot();
            int mods = e.getModifiers();
            if ((mods & ZOOM_MASK) == ZOOM_MASK && this.zoomRectangle == null) {
                Rectangle2D screenDataArea = getScreenDataArea(e.getX(), e.getY());
                if (screenDataArea != null) {
                    this.zoomPoint = this.getPointInRectangle(e.getX(), e.getY(),
                            screenDataArea);
                }
                else {
                    this.zoomPoint = null;
                }
            }
            else{
                // can we pan this plot?
                if (plot instanceof Pannable) {
                    Pannable pannable = (Pannable) plot;
                    if (pannable.isDomainPannable() || pannable.isRangePannable()) {
                        Rectangle2D screenDataArea = getScreenDataArea(e.getX(),
                                e.getY());
                        if (screenDataArea != null && screenDataArea.contains(
                                e.getPoint())) {
                            this.panW = screenDataArea.getWidth();
                            this.panH = screenDataArea.getHeight();
                            this.panLast = e.getPoint();
                            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                        }
                    }
                    // the actual panning occurs later in the mouseDragged()
                    // method
                }
            }
        }

        private Point2D getPointInRectangle(int x, int y, Rectangle2D area) {
            double xx = Math.max(area.getMinX(), Math.min(x, area.getMaxX()));
            double yy = Math.max(area.getMinY(), Math.min(y, area.getMaxY()));
            return new Point2D.Double(xx, yy);
        }


    /**
     * Handles a 'mouse dragged' event.
     *
     * @param e  the mouse event.
     */
    @Override
    public void mouseDragged(MouseEvent e) {

        // if the popup menu has already been triggered, then ignore dragging...
        if (this.getPopupMenu() != null && this.getPopupMenu().isShowing()) {
            return;
        }

        // handle panning if we have a start point
        if (this.panLast != null) {
            double dx = e.getX() - this.panLast.getX();
            double dy = e.getY() - this.panLast.getY();
            if (dx == 0.0 && dy == 0.0) {
                return;
            }
            double wPercent = -dx / this.panW;
            double hPercent = dy / this.panH;
            Plot plot = this.getChart().getPlot();
            boolean old = plot.isNotify();
            plot.setNotify(false);
            Pannable p = (Pannable) plot;
            if (p.getOrientation() == PlotOrientation.VERTICAL) {
                p.panDomainAxes(wPercent, this.getChartRenderingInfo().getPlotInfo(),
                        this.panLast);
                p.panRangeAxes(hPercent, this.getChartRenderingInfo().getPlotInfo(),
                        this.panLast);
            }
            else {
                p.panDomainAxes(hPercent, this.getChartRenderingInfo().getPlotInfo(),
                        this.panLast);
                p.panRangeAxes(wPercent, this.getChartRenderingInfo().getPlotInfo(),
                        this.panLast);
            }
            this.panLast = e.getPoint();
            plot.setNotify(old);
            return;
        }

        // if no initial zoom point was set, ignore dragging...
        if (this.zoomPoint == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) getGraphics();

        // erase the previous zoom rectangle (if any).  We only need to do
        // this is we are using XOR mode, which we do when we're not using
        // the buffer (if there is a buffer, then at the end of this method we
        // just trigger a repaint)
        if (!this.useBuffer) {
            drawZoomRectangle(g2, true);
        }

        boolean hZoom = false;
        boolean vZoom = false;
        Plot plot = this.getChart().getPlot();
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        if(plot instanceof Zoomable){
            orientation = ((Zoomable)plot).getOrientation();
        }
        if (orientation == PlotOrientation.HORIZONTAL) {
            hZoom = this.isRangeZoomable();
            vZoom = this.isDomainZoomable();
        }
        else {
            hZoom = this.isDomainZoomable();
            vZoom = this.isRangeZoomable();
        }
        Rectangle2D screenDataArea = getScreenDataArea();
        if (hZoom && vZoom) {
            double x, y, w, h;
            x = Math.min(this.zoomPoint.getX(), e.getX());
            y = Math.min(this.zoomPoint.getY(), e.getY());
            w = Math.abs(e.getX()-this.zoomPoint.getX());
            h = Math.abs(e.getY()-this.zoomPoint.getY());
            this.zoomRectangle = intersect(new Rectangle2D.Double(x, y, w, h), screenDataArea);
        }
        else if (hZoom) {
            double x, y, w, h;
            x = Math.min(this.zoomPoint.getX(), e.getX());
            y = screenDataArea.getMinY();
            w = Math.abs(e.getX()-this.zoomPoint.getX());
            h = screenDataArea.getHeight();
            this.zoomRectangle = intersect(new Rectangle2D.Double(x, y, w, h), screenDataArea);
        }
        else if (vZoom) {
            double x, y, w, h;
            x = screenDataArea.getMinX();
            y = Math.min(this.zoomPoint.getY(), e.getY());
            w = screenDataArea.getWidth();
            h = Math.abs(e.getY()-this.zoomPoint.getY());
            this.zoomRectangle = intersect(new Rectangle2D.Double(x, y, w, h), screenDataArea);
        }

        // Draw the new zoom rectangle...
        if (this.useBuffer) {
            repaint();
        }
        else {
            // with no buffer, we use XOR to draw the rectangle "over" the
            // chart...
            drawZoomRectangle(g2, true);
        }
        g2.dispose();

    }

    private void drawZoomRectangle(Graphics2D g2, boolean xor) {
        if (this.zoomRectangle != null) {
            if (xor) {
                 // Set XOR mode to draw the zoom rectangle
                g2.setXORMode(Color.gray);
            }
            if (this.getFillZoomRectangle()) {
                g2.setPaint(this.getZoomFillPaint());
                g2.fill(this.zoomRectangle);
            }
            else {
                g2.setPaint(this.getZoomFillPaint());
                g2.draw(this.zoomRectangle);
            }
            if (xor) {
                // Reset to the default 'overwrite' mode
                g2.setPaintMode();
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

        // if we've been panning, we need to reset now that the mouse is
        // released...
        if (this.panLast != null) {
            this.panLast = null;
            setCursor(Cursor.getDefaultCursor());
        }

        else if (this.zoomRectangle != null) {
            boolean hZoom = false;
            boolean vZoom = false;
            if (((Zoomable)this.getChart().getPlot()).getOrientation() == PlotOrientation.HORIZONTAL) {
                hZoom = this.isRangeZoomable();
                vZoom = this.isDomainZoomable();
            }
            else {
                hZoom = this.isDomainZoomable();
                vZoom = this.isRangeZoomable();
            }

            boolean zoomTrigger1 = hZoom && Math.abs(e.getX()
                - this.zoomPoint.getX()) >= this.getZoomTriggerDistance();
            boolean zoomTrigger2 = vZoom && Math.abs(e.getY()
                - this.zoomPoint.getY()) >= this.getZoomTriggerDistance();
            if (zoomTrigger1 || zoomTrigger2) {

                    double x, y, w, h;
                    Rectangle2D screenDataArea = getScreenDataArea();

                    // for mouseReleased event, (horizontalZoom || verticalZoom)
                    // will be true, so we can just test for either being false;
                    // otherwise both are true
                    if (!vZoom) {
                        x = Math.min(this.zoomPoint.getX(), e.getX());
                        y = screenDataArea.getMinY();
                        w = Math.abs(e.getX()-this.zoomPoint.getX());
                        h = screenDataArea.getHeight();
                    }
                    else if (!hZoom) {
                        x = screenDataArea.getMinX();
                        y = Math.min(this.zoomPoint.getY(), e.getY());
                        w = screenDataArea.getWidth();
                        h = Math.abs(e.getY()-this.zoomPoint.getY());
                    }
                    else {
                        x = Math.min(this.zoomPoint.getX(), e.getX());
                        y = Math.min(this.zoomPoint.getY(), e.getY());
                        w = Math.abs(e.getX()-this.zoomPoint.getX());
                        h = Math.abs(e.getY()-this.zoomPoint.getY());
                    }
                    Rectangle2D zoomArea = intersect(new Rectangle2D.Double(x, y, w, h), screenDataArea);
                    zoom(zoomArea);

                this.zoomPoint = null;
                this.zoomRectangle = null;
            }
            else {
                // erase the zoom rectangle
                Graphics2D g2 = (Graphics2D) getGraphics();
                if (this.useBuffer) {
                    repaint();
                }
                else {
                    drawZoomRectangle(g2, true);
                }
                g2.dispose();
                this.zoomPoint = null;
                this.zoomRectangle = null;
            }

        }

        else if (e.isPopupTrigger()) {
            if (this.getPopupMenu() != null) {
                displayPopupMenu(e.getX(), e.getY());
            }
        }

    }

    private Rectangle2D intersect(Rectangle2D inner, Rectangle2D outer){
        double x = Math.max(inner.getMinX(), outer.getMinX());
        double y = Math.max(inner.getMinY(), outer.getMinY());
        double x2 = Math.min(inner.getMaxX(), outer.getMaxX());
        double y2 = Math.min(inner.getMaxY(), outer.getMaxY());
        return new Rectangle2D.Double(x, y, x2-x, y2-y);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (this.getChart() == null) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        drawZoomRectangle(g2, !this.useBuffer);
        g2.dispose();
    }

}
