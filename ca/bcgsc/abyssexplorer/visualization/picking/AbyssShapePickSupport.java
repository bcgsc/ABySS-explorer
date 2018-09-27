package ca.bcgsc.abyssexplorer.visualization.picking;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ConcurrentModificationException;

import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;


import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationServer;
import edu.uci.ics.jung.visualization.picking.ShapePickSupport;

/**
 * Extends Tom Nelson's edu.uci.ics.jung.visualization.picking.ShapePickSupport<V,E> 
 * 
 * This class overrides the getEdge() method to adjust for shifted edge positions 
 * due to ABySS-Explorer's polar vertices.
 * 
 * @author Cydney Nielsen
 * 
 */
public class AbyssShapePickSupport extends ShapePickSupport<Vertex,Edge> {

	/**
     * Create a ShapePickSupport for the specified VisualizationServer.
     */
	public AbyssShapePickSupport(VisualizationServer<Vertex,Edge> vv) {
		super(vv);
	}

	/**
	 * Returns an edge whose shape intersects the 'pickArea' footprint of the
	 * passed x,y, coordinates.
	 * 
	 * @param layout the layout
	 * @param x the x
	 * @param y the y
	 * 
	 * @return the edge
	 */
	public Edge getEdge(Layout<Vertex, Edge> layout, double x, double y) {

		Point2D ip = vv.getRenderContext().getMultiLayerTransformer()
			.inverseTransform(Layer.VIEW, new Point2D.Double(x, y));
		x = ip.getX();
		y = ip.getY();

		// as a Line has no area, we can't always use edgeshape.contains(point)
		// so we make a small rectangular pickArea around the point and check if the
		// edgeshape.intersects(pickArea)
		Rectangle2D pickArea = new Rectangle2D.Float((float) x - pickSize,
			(float) y - pickSize, 2 * pickSize, 2 * pickSize);
		Edge closest = null;
		double minDistance = Double.MAX_VALUE;
		while (true) {
			try {
				for (Edge e : getFilteredEdges(layout)) {
					DirectedSparseMultigraph<Vertex,Edge> graph = (DirectedSparseMultigraph<Vertex,Edge>) (vv.
						getModel().getGraphLayout().getGraph());
					if (graph.containsEdge(e)) {
						Shape edgeShape = getTransformedEdgeShape(layout, e);
						if (edgeShape == null)
							continue;
						// because of the transform, the edgeShape is now a
						// GeneralPath
						// see if this edge is the closest of any that intersect
						if (edgeShape.intersects(pickArea)) {
							float cx = 0;
							float cy = 0;
							float[] f = new float[6];
							PathIterator pi = new GeneralPath(edgeShape)
									.getPathIterator(null);
							if (pi.isDone() == false) {
								pi.next();
								pi.currentSegment(f);
								cx = f[0];
								cy = f[1];
								if (pi.isDone() == false) {
									pi.currentSegment(f);
									cx = f[0];
									cy = f[1];
								}
							}
							float dx = (float) (cx - x);
							float dy = (float) (cy - y);
							float dist = dx * dx + dy * dy;
							if (dist < minDistance) {
								minDistance = dist;
								closest = e;
							}
						}
					}
				}
				break;
			} catch (ConcurrentModificationException cme) {
			}
		}
		return closest;
	}

	/**
	 * Retrieves the shape template for e and transforms it according to the
	 * positions of its endpoints in layout. Endpoints are adjusted to account 
	 * for vertex polarity.
	 * 
	 * @param layout the Layout which specifies e's endpoint positions
	 * @param e the edge whose shape is to be returned
	 * 
	 * @return the transformed edge shape
	 */
	/**
	 * @param layout
	 * @param e
	 * @return
	 */
	private Shape getTransformedEdgeShape(Layout<Vertex,Edge> layout, Edge e) {
		
		Vertex v1 = e.getSourceVertex();
		Vertex v2 = e.getDestVertex();
		int s1 = v1.getOutgoingPole(e);
		int s2 = v2.getIncomingPole(e);
		
		boolean isLoop = v1.equals(v2);
		Point2D p1 = vv.getRenderContext().getMultiLayerTransformer()
				.transform(Layer.LAYOUT, layout.transform(v1));
		Point2D p2 = vv.getRenderContext().getMultiLayerTransformer()
				.transform(Layer.LAYOUT, layout.transform(v2));
		if (p1 == null || p2 == null) { return null; }

		float x1 = (float) p1.getX();
		float x2 = (float) p2.getX();
		float y1 = (float) p1.getY();
		float y2 = (float) p2.getY();

		// retrieve the vertex height from its shape transformer
		Integer vHeight = vv.getRenderContext().getVertexShapeTransformer()
		.transform(v1).getBounds().height;

                DirectedSparseMultigraph<Vertex,Edge> graph = (DirectedSparseMultigraph<Vertex,Edge>) layout.getGraph();
		// adjust y1
                if (!v1.hasConsistentPoles(graph)) {
                    if (s1 == 0) {
                            y1 -= vHeight / 2;
                    } else if (s1 == 1) {
                            y1 += vHeight / 2;
                    } else {
                            System.err.println("Error: Strand must be 0 or 1 (" + s1 + ")");
                            System.exit(0);
                    }
                }
		// adjust y2
                if (!v2.hasConsistentPoles(graph)) {
                    if (s2 == 0) {
                            y2 -= vHeight / 2;
                    } else if (s2 == 1) {
                            y2 += vHeight / 2;
                    } else {
                            System.err.println("Error: Strand must be 0 or 1 (" + s2 + ")");
                            System.exit(0);
                    }
                }

		// translate the edge to the starting vertex
		AffineTransform xform = AffineTransform.getTranslateInstance(x1, y1);

		Shape edgeShape = vv.getRenderContext().getEdgeShapeTransformer()
			.transform(Context.<Graph<Vertex,Edge>,Edge> getInstance(vv
					.getGraphLayout().getGraph(), e));
		if (isLoop) {
			// make the loops proportional to the size of the vertex
			Shape shape = vv.getRenderContext().getVertexShapeTransformer()
				.transform(v2);
			Rectangle2D s2Bounds = shape.getBounds2D();
			xform.scale(s2Bounds.getWidth()*2, s2Bounds.getWidth()*4);
			// move the loop so that the nadir is centered in the vertex

                        int poleOut = v2.getOutgoingPole(e);
                        if(s2 != poleOut){
                            //translate the shape for palindromic contig
                            if(s2 == 0){
                                xform.translate(-edgeShape.getBounds2D().getHeight()/2, -edgeShape.getBounds2D().getWidth()/4);
                            }
                            else if (s2 == 1){
                                xform.translate(edgeShape.getBounds2D().getHeight()/2, edgeShape.getBounds2D().getWidth()/4);
                            }
                        }
                        else{
                            if (s2 == 0) {
                                xform.translate(0, -edgeShape.getBounds2D().getWidth()/2);
                            }
                            else if (s2 == 1) {
                                xform.translate(0, edgeShape.getBounds2D().getWidth()/2);
                            }
                        }

//                        if (s2 == 0) {
//                            xform.translate(0, -edgeShape.getBounds2D().getWidth()/2);
//                        }
//                        else if (s2 == 1) {
//                            xform.translate(0, edgeShape.getBounds2D().getWidth()/2);
//                        }
		} else {
			float dx = x2 - x1;
			float dy = y2 - y1;
			// rotate the edge to the angle between the vertices
			double theta = Math.atan2(dy, dx);
			xform.rotate(theta);
			// stretch the edge to span the distance between the vertices
			float dist = (float) Math.sqrt(dx * dx + dy * dy);
			xform.scale(dist, 1.0f);
		}

		// transform the edge to its location and dimensions
		edgeShape = xform.createTransformedShape(edgeShape);
		return edgeShape;
	}
}
