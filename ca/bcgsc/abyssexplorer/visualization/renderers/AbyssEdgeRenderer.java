package ca.bcgsc.abyssexplorer.visualization.renderers;

import java.awt.Dimension;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import javax.swing.JComponent;

import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;

import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeIndexFunction;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.RenderContext;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.decorators.EdgeShape.IndexedRendering;
import edu.uci.ics.jung.visualization.renderers.BasicEdgeRenderer;
import edu.uci.ics.jung.visualization.transform.LensTransformer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.transform.shape.GraphicsDecorator;

/**
 * Extends the JUNG project's
 * edu.uci.ics.jung.visualization.renderers.BasicEdgeRenderer.
 * 
 * Adds custom functionality to handle features of an AbyssGraph, in particular,
 * how to render edges from polar vertices.
 * 
 * @author Cydney Nielsen
 * 
 */
public class AbyssEdgeRenderer extends BasicEdgeRenderer<Vertex,Edge> {

    public void paintEdge(RenderContext<Vertex,Edge> rc, Layout<Vertex,Edge> layout, Edge e) {
        GraphicsDecorator g2d = rc.getGraphicsContext();
        DirectedSparseMultigraph<Vertex,Edge> graph = (DirectedSparseMultigraph<Vertex,Edge>) layout.getGraph();
        if (!rc.getEdgeIncludePredicate().evaluate(Context.<Graph<Vertex,Edge>,Edge>getInstance(graph,e)))
            return;
        
        // don't draw edge if either incident vertex is not drawn
        Vertex v1 = e.getSourceVertex();
        Vertex v2 = e.getDestVertex();
        if (!rc.getVertexIncludePredicate().evaluate(Context.<Graph<Vertex,Edge>,Vertex>getInstance(graph,v1)) || 
             !rc.getVertexIncludePredicate().evaluate(Context.<Graph<Vertex,Edge>,Vertex>getInstance(graph,v2)))
            return;
        
        Stroke new_stroke = rc.getEdgeStrokeTransformer().transform(e);
        Stroke old_stroke = g2d.getStroke();
        if (new_stroke != null)
            g2d.setStroke(new_stroke);
        
        // specify the edge drawing method
        drawSquiggleEdge(rc, layout, e);

        // restore paint and stroke
        if (new_stroke != null)
            g2d.setStroke(old_stroke);
    }

    protected void drawSquiggleEdge(RenderContext<Vertex,Edge> rc, Layout<Vertex,Edge> layout, Edge e) {

    	GraphicsDecorator g = rc.getGraphicsContext();
    	DirectedSparseMultigraph<Vertex,Edge> graph = (DirectedSparseMultigraph<Vertex,Edge>) layout.getGraph();
    	Vertex v1 = e.getSourceVertex();
    	Vertex v2 = e.getDestVertex();
    	Point2D p1 = layout.transform(v1);
    	Point2D p2 = layout.transform(v2);
    	p1 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p1);
    	p2 = rc.getMultiLayerTransformer().transform(Layer.LAYOUT, p2);
    	Shape s1 = rc.getVertexShapeTransformer().transform(v1);
    	Shape s2 = rc.getVertexShapeTransformer().transform(v2);

    	float x1 = (float) p1.getX();
    	float y1 = (float) p1.getY();
    	float x2 = (float) p2.getX();
    	float y2 = (float) p2.getY();

    	// adjust y coordinates of polar vertices
    	// retrieve the vertex height from its shape transformer
    	if (!v1.hasConsistentPoles(graph)) {
        	// adjust y1
    		Integer vHeight = s1.getBounds().height;
    		int st1 = v1.getOutgoingPole(e);
    		if (st1 == 0) {
    			y1 -= vHeight/2;
    		} else if (st1 == 1) {
    			y1 += vHeight/2;
    		} else {
    			System.err.println("Error: Strand must be 0 or 1 (" + st1 + ")");
    			System.exit(0);
    		}
    	}
    	if (!v2.hasConsistentPoles(graph)) {
    		// adjust y2
    		Integer vHeight = s2.getBounds().height;
    		int st2 = v2.getIncomingPole(e);
    		if (st2 == 0) {
    			y2 -= vHeight/2;
    		} else if (st2 == 1) {
    			y2 += vHeight/2;
    		} else {
    			System.err.println("Error: Strand must be 0 or 1 (" + st2 + ")");
    			System.exit(0);
    		}
    	}
    	
    	boolean isLoop = v1.equals(v2);
    	Shape edgeShape = rc.getEdgeShapeTransformer().transform(Context.<Graph<Vertex,Edge>,Edge>getInstance(graph, e));

    	boolean edgeHit = true;
    	boolean arrowHit = true;
    	Rectangle deviceRectangle = null;
    	JComponent vv = rc.getScreenDevice();
    	if(vv != null) {
    		Dimension d = vv.getSize();
    		deviceRectangle = new Rectangle(0,0,d.width,d.height);
    	}

    	AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

    	if(isLoop) {
    		// this is a self-loop. scale it is larger than the vertex
    		// it decorates and translate it so that its nadir is
    		// at the center of the vertex.
    		Rectangle2D s2Bounds = s2.getBounds2D();
    		xform.scale(s2Bounds.getWidth()*2,s2Bounds.getWidth()*4);
                int st2 = v2.getIncomingPole(e);

                int poleOut = v2.getOutgoingPole(e);
                if(st2 != poleOut){
                    //translate the shape for palindromic contig
                    if(st2 == 0){
                        xform.translate(-edgeShape.getBounds2D().getHeight()/2, -edgeShape.getBounds2D().getWidth()/4);
                    }
                    else if (st2 == 1){
                        xform.translate(edgeShape.getBounds2D().getHeight()/2, edgeShape.getBounds2D().getWidth()/4);
                    }
                }
                else{
                    if (st2 == 0) {
                        xform.translate(0, -edgeShape.getBounds2D().getWidth()/2);
                    }
                    else if (st2 == 1) {
                        xform.translate(0, edgeShape.getBounds2D().getWidth()/2);
                    }
                }
    	} else if(rc.getEdgeShapeTransformer() instanceof EdgeShape.Orthogonal) {
    		float dx = x2-x1;
    		float dy = y2-y1;
    		int index = 0;    		
    		if (rc.getEdgeShapeTransformer() instanceof IndexedRendering) {
    			EdgeIndexFunction<Vertex,Edge> peif = 
    				((IndexedRendering<Vertex,Edge>)rc.getEdgeShapeTransformer()).getEdgeIndexFunction();
    			index = peif.getIndex(graph, e);
    			index *= 20;
    		}
    		GeneralPath gp = new GeneralPath();
    		gp.moveTo(0,0);// the xform will do the translation to x1,y1
    		if(x1 > x2) {
    			if(y1 > y2) {
    				gp.lineTo(0, index);
    				gp.lineTo(dx-index, index);
    				gp.lineTo(dx-index, dy);
    				gp.lineTo(dx, dy);
    			} else {
    				gp.lineTo(0, -index);
    				gp.lineTo(dx-index, -index);
    				gp.lineTo(dx-index, dy);
    				gp.lineTo(dx, dy);
    			}

    		} else {
    			if(y1 > y2) {
    				gp.lineTo(0, index);
    				gp.lineTo(dx+index, index);
    				gp.lineTo(dx+index, dy);
    				gp.lineTo(dx, dy);

    			} else {
    				gp.lineTo(0, -index);
    				gp.lineTo(dx+index, -index);
    				gp.lineTo(dx+index, dy);
    				gp.lineTo(dx, dy);

    			}

    		}
    		edgeShape = gp;

    	} else {
    		// this is a normal edge. Rotate it to the angle between
    		// vertex endpoints, then scale it to the distance between
    		// the vertices
    		float dx = x2-x1;
    		float dy = y2-y1;
    		float thetaRadians = (float) Math.atan2(dy, dx);
    		xform.rotate(thetaRadians);
    		float dist = (float) Math.sqrt(dx*dx + dy*dy);
    		xform.scale(dist, 1.0);
    	}

    	edgeShape = xform.createTransformedShape(edgeShape);

    	MutableTransformer vt = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW);
    	if(vt instanceof LensTransformer) {
    		vt = ((LensTransformer)vt).getDelegate();
    	}
    	edgeHit = vt.transform(edgeShape).intersects(deviceRectangle);

    	if(edgeHit == true) {

    		Paint oldPaint = g.getPaint();

    		// get Paints for filling and drawing
    		// (filling is done first so that drawing and label use same Paint)
    		Paint fill_paint = rc.getEdgeFillPaintTransformer().transform(e); 
    		if (fill_paint != null)
    		{
    			g.setPaint(fill_paint);
    			g.fill(edgeShape);
    		}
    		Paint draw_paint = rc.getEdgeDrawPaintTransformer().transform(e);
    		if (draw_paint != null)
    		{
    			g.setPaint(draw_paint);
    			g.draw(edgeShape);
    		}

    		float scalex = (float)g.getTransform().getScaleX();
    		float scaley = (float)g.getTransform().getScaleY();
    		// see if arrows are too small to bother drawing
    		if(scalex < .3 || scaley < .3) return;

    		if (rc.getEdgeArrowPredicate().evaluate(Context.<Graph<Vertex,Edge>,Edge>getInstance(graph, e))) {

    			Stroke new_stroke = rc.getEdgeArrowStrokeTransformer().transform(e);
    			Stroke old_stroke = g.getStroke();
    			if (new_stroke != null)
    				g.setStroke(new_stroke);
    			Shape destVertexShape = 
    				rc.getVertexShapeTransformer().transform(graph.getEndpoints(e).getSecond());

    			AffineTransform xf = AffineTransform.getTranslateInstance(x2, y2);
    			destVertexShape = xf.createTransformedShape(destVertexShape);

    			arrowHit = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW).transform(destVertexShape).intersects(deviceRectangle);
    			if(arrowHit) {

    				AffineTransform at = this.getEdgeArrowRenderingSupport(). //for jung2-2_0_1
    					getArrowTransform(rc, new GeneralPath(edgeShape), destVertexShape);
    				if(at == null) return;
    				Shape arrow = rc.getEdgeArrowTransformer().transform(Context.<Graph<Vertex,Edge>,Edge>getInstance(graph, e));
    				arrow = at.createTransformedShape(arrow);
    				g.setPaint(rc.getArrowFillPaintTransformer().transform(e));
    				g.fill(arrow);
    				g.setPaint(rc.getArrowDrawPaintTransformer().transform(e));
    				g.draw(arrow);
    			}
    			if (graph.getEdgeType(e) == EdgeType.UNDIRECTED) {
    				Shape vertexShape = 
    					rc.getVertexShapeTransformer().transform(graph.getEndpoints(e).getFirst());
    				xf = AffineTransform.getTranslateInstance(x1, y1);
    				vertexShape = xf.createTransformedShape(vertexShape);

    				arrowHit = rc.getMultiLayerTransformer().getTransformer(Layer.VIEW).transform(vertexShape).intersects(deviceRectangle);

    				if(arrowHit) {
    					AffineTransform at = this.getEdgeArrowRenderingSupport(). //for jung2-2_0_1
                                                getReverseArrowTransform(rc, new GeneralPath(edgeShape), vertexShape, !isLoop);
    					if(at == null) return;
    					Shape arrow = rc.getEdgeArrowTransformer().transform(Context.<Graph<Vertex,Edge>,Edge>getInstance(graph, e));
    					arrow = at.createTransformedShape(arrow);
    					g.setPaint(rc.getArrowFillPaintTransformer().transform(e));
    					g.fill(arrow);
    					g.setPaint(rc.getArrowDrawPaintTransformer().transform(e));
    					g.draw(arrow);
    				}
    			}
    			// restore paint and stroke
    			if (new_stroke != null)
    				g.setStroke(old_stroke);

    		}

    		// restore old paint
    		g.setPaint(oldPaint);
    	}
    }

}
