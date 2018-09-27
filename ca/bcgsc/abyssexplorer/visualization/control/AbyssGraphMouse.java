package ca.bcgsc.abyssexplorer.visualization.control;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.List;

import javax.swing.event.ChangeListener;

import ca.bcgsc.abyssexplorer.graph.DistanceEstimate;
import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;
import ca.bcgsc.abyssexplorer.gui.AbyssVisualizationViewer;
import ca.bcgsc.abyssexplorer.visualization.picking.PartnerEdgeState;


import edu.uci.ics.jung.algorithms.layout.GraphElementAccessor;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.VisualizationServer.Paintable;
import edu.uci.ics.jung.visualization.control.CrossoverScalingControl;
import edu.uci.ics.jung.visualization.control.PluggableGraphMouse;
import edu.uci.ics.jung.visualization.control.ScalingControl;
import edu.uci.ics.jung.visualization.layout.PersistentLayoutImpl;
import edu.uci.ics.jung.visualization.picking.PickedState;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import edu.uci.ics.jung.visualization.util.DefaultChangeEventSupport;

/**
 * AbyssGraphMouse supports the picking of graph elements
 * with the mouse, and uses a MouseButtonOne press and
 * drag gesture to translate the graph display in the x and y
 * direction. 
 * 
 * Integrates functionality from Tom Nelson's JUNG classes:
 *  TranslatingGraphMousePlugin
 *  PickingGraphMousePlugin
 *  ScalingGraphMousePlugin
 *  RotatingGraphMousePlugin
 * 
 * @author Cydney Nielsen
 *
 */
public class AbyssGraphMouse extends PluggableGraphMouse implements ChangeEventSupport {
	
	/**
	 * Defines how to communicate with registered listeners
	 */
    protected ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);
	
	/**
	 * the picked Vertex, if any
	 */
    protected Vertex vertex;
    
    /**
     * the picked Edge, if any
     */
    public Edge edge;
    
    /**
     * the location in the View where the mouse was pressed
     */
    protected Point down;
	
	/**
	 * modifiers to compare against mouse event modifiers
	 */
    protected int modifiers;
    
    /**
     * modifiers for the action of adding to an existing selection
     */
    protected int addToSelectionModifiers;
    
    /**
     * modifiers for the action of rotating the graph display
     */
    protected int rotationModifiers;
    
    protected AbyssVisualizationViewer vv;
    protected GraphElementAccessor<Vertex,Edge> pickSupport;
    protected PickedState<Vertex> pickedVertexState;
    protected PickedState<Edge> pickedEdgeState;
    protected PartnerEdgeState partnerEdgePickState;
    protected PersistentLayoutImpl<Vertex,Edge> layout;
    
    /**
     * the x distance from the picked vertex center to the mouse point
     */
    protected double offsetx;
    
    /**
     * the y distance from the picked vertex center to the mouse point
     */
    protected double offsety;
    
    /**
     * controls whether the Vertices may be moved with the mouse
     */
    protected boolean locked;
    
    /**
     * used to draw a rectangle to contain picked vertices
     */
    protected Rectangle2D rect;
    
    /**
     * the Paintable for the lens picking rectangle
     */
    protected Paintable lensPaintable;
    
    /**
     * color for the picking rectangle
     */
    protected Color lensColor = Color.black;
    
    /**
     * the amount to zoom in by
     */
	protected float in = 1.1f;
	/**
	 * the amount to zoom out by
	 */
	protected float out = 1/1.1f;
	
	/**
	 * whether to center the zoom at the current mouse position
	 */
	protected boolean zoomAtMouse = false;
    
    /**
     * controls scaling operations
     */
    protected ScalingControl scaler;

    /**
     * a Paintable to draw the rectangle used to pick multiple
     * Vertices
     * @author Tom Nelson
     */
    class LensPaintable implements Paintable {

        public void paint(Graphics g) {
            Color oldColor = g.getColor();
            g.setColor(lensColor);
            if(rect.getWidth() > 0 && rect.getHeight() > 0){ // this check is needed to bypass java.lang.ArithmeticException: / by zero  at sun.java2d.pisces.Stroker.finish(Stroker.java:698)
                ((Graphics2D)g).draw(rect);
            }
            g.setColor(oldColor);
        }

        public boolean useTransform() {
            return false;
        }
    }
    
    public AbyssGraphMouse() {
    	modifiers = InputEvent.BUTTON1_MASK; // left mouse button
    	addToSelectionModifiers = InputEvent.BUTTON1_MASK | InputEvent.SHIFT_MASK;
    	int mask = MouseEvent.CTRL_MASK;
        if (System.getProperty("os.name").startsWith("Mac")) {
            mask = MouseEvent.META_MASK;
        }
    	rotationModifiers = MouseEvent.BUTTON1_MASK | mask;
    	rect = new Rectangle2D.Float();
    	lensPaintable = new LensPaintable();
    	scaler = new CrossoverScalingControl();
    }
    
    @SuppressWarnings("unchecked")
	protected void updateState(MouseEvent e) {
        down = e.getPoint();
        vv = (AbyssVisualizationViewer) e.getSource();
        layout = (PersistentLayoutImpl) vv.getGraphLayout();
        pickSupport = vv.getPickSupport();
        pickedVertexState = vv.getPickedVertexState();
        pickedEdgeState = vv.getPickedEdgeState();
        partnerEdgePickState = vv.getPartnerEdgePickState();
    }
    
    /**
     * MousePressed events trigger vertex selections.
     * This is done to enable vertex movement during
     * mouseDragged events without requiring an initial
     * mouseClicked event.
     */
    public void mousePressed(MouseEvent e) {
    	updateState(e); 
    	Point2D ip = e.getPoint();
        vertex = pickSupport.getVertex(layout, ip.getX(), ip.getY());
        if (vertex != null) {
        	if (e.getModifiers() == modifiers) {
        		vv.selectVertex(vertex);
        		// layout.getLocation applies the layout transformer so
        		// q is transformed by the layout transformer only
        		Point2D q = layout.transform(vertex);
        		// transform the mouse point to graph coordinate system
        		Point2D gp = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, ip);
        		offsetx = (float) (gp.getX()-q.getX());
        		offsety = (float) (gp.getY()-q.getY());
        	} else if (e.getModifiers() == addToSelectionModifiers) {
            	partnerEdgePickState.clear();
            	vv.setToolTipText(null);
                vv.addPostRenderPaintable(lensPaintable);
                rect.setFrameFromDiagonal(down,down);
                boolean wasThere = pickedVertexState.pick(vertex, !pickedVertexState.isPicked(vertex));
                if (wasThere) {
                    vertex = null;
                } else {
                    // layout.getLocation applies the layout transformer so
                    // q is transformed by the layout transformer only
                    Point2D q = layout.transform(vertex);
                    // translate mouse point to graph coord system
                    Point2D gp = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.LAYOUT, ip);
                    offsetx = (float) (gp.getX()-q.getX());
                    offsety = (float) (gp.getY()-q.getY());
                }
        	}
        	e.consume();
        } else {
        	// make the selection rectange visible
        	vv.addPostRenderPaintable(lensPaintable);
        }
    }
    
    /**
     * MouseClicked events trigger edge selections. 
     */
    public void mouseClicked(MouseEvent e) {
    	updateState(e);
    	Point2D ip = e.getPoint();
    	vertex = pickSupport.getVertex(layout, ip.getX(), ip.getY());
    	edge = pickSupport.getEdge(layout, ip.getX(), ip.getY());
    	if (vertex == null) 
    		// only select an edge if not interfering with a vertex
    		if (edge != null) {
    			// treat events with 'addToSelectionModifiers' as only having 'modifiers'
    			if (e.getModifiers() == modifiers && edge != null && e.getClickCount() == 2) {
    				vv.reverseComplementSelection(edge);
    			}
    			if (e.getModifiers() == modifiers || e.getModifiers() == addToSelectionModifiers) {
    				vv.selectEdge(edge);
    			} 
    		} else {
    			// only clear if click occurs outside of an edge or vertex
    			vv.clear();
    		}
    	fireStateChanged();
    }
    
    /**
	 * If the mouse drag is initiated over a vertex, then that
	 * vertex will be selected in response to the initial mousePressed
	 * event, and move it and all other picked vertices with the mouse.
	 * 
	 * If the mouse is not over a vertex, then drag the display.
	 * 
	 * All of the above only applies to unlocked displays.
	 * 
	 */
    public void mouseDragged(MouseEvent e) {
        if (locked == false) {
        	// initiation point 'down' is set in mousePressed()
            vertex = pickSupport.getVertex(layout, down.getX(), down.getY());
            if (vertex != null) {
            	// vertex selected in mousePressed()
            	moveSelectedVertices(e);
            } else {
            	// 'out' indicates current mouse position in the mouseDragged event
                Point2D out = e.getPoint();
                if (e.getModifiers() == modifiers) {
                	translateDisplay(e);
                } else if (e.getModifiers() == addToSelectionModifiers) {
                	rect.setFrameFromDiagonal(down,out);
                } else if (e.getModifiers() == rotationModifiers) {
                	rotateDisplay(e);
                }
            }
            if (vertex != null) e.consume();
            vv.repaint();
        }
    }
    
    /**
	 * If the mouse is dragging a rectangle, pick the
	 * Vertices contained in that rectangle
	 * 
	 * clean up settings from mousePressed
	 */
    public void mouseReleased(MouseEvent e) {
        if(e.getModifiers() == modifiers) {
            if (down != null) {
                Point2D out = e.getPoint();

                if (vertex == null && heyThatsTooClose(down, out, 5) == false) {
                    pickContainedVertices(vv, down, out, true);
                }
            }
        } else if (e.getModifiers() == this.addToSelectionModifiers) {
            if (down != null) {
                Point2D out = e.getPoint();

                if (vertex == null && heyThatsTooClose(down,out,5) == false) {
                    pickContainedVertices(vv, down, out, false);
                }
            }
        }
        down = null;
        vertex = null;
        edge = null;
        rect.setFrame(0,0,0,0);
        vv.removePostRenderPaintable(lensPaintable);
        vv.repaint();
    }
    
    /**
     * If mouse moves over a paired-end partner edge, 
     * display the distance estimate in the tooltip.
     */
    public void mouseMoved(MouseEvent e) {
    	updateState(e); // entry point
    	Point2D ip = e.getPoint();
    	edge = pickSupport.getEdge(layout, ip.getX(), ip.getY());
    	if (edge != null) {
    		if (!pickedEdgeState.isPicked(edge)) {
    			// determine whether edge is a paired-end partner
    			Collection<Edge> picked = pickedEdgeState.getPicked();
    			assert picked.size() < 2: "Only one edge can be picked at a time";
    			if (picked.size() == 1) {
    				Edge pEdge = (Edge) picked.toArray()[0];
    				List<DistanceEstimate> in = pEdge.getInboundPartners();
    				boolean found = false;
    				if (in != null) {
    					for (DistanceEstimate iDis: in) {
    						if (edge.getLabelObject().equals(iDis.getLabelObject())) {
    							Integer d = iDis.getDist();
    							vv.setToolTipText(d.toString());
    							found = true;
    						}
    					}
    				}
    				if (!found) {
    					List<DistanceEstimate> out = pEdge.getOutboundPartners();
    					if (out != null) {
    						for (DistanceEstimate oDis: out) {
    							if (edge.getLabelObject().equals(oDis.getLabelObject())) {
    								Integer d = oDis.getDist();
    								vv.setToolTipText(d.toString());
    							}
    						}
    					}
    				}
    			}
    		}
    	} else {
    		vv.setToolTipText(null);
    	}
    }
 
    protected void moveSelectedVertices(MouseEvent e) {
        Point p = e.getPoint();
        Point2D graphPoint = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(p);
        Point2D graphDown = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(down);
//        Layout<Vertex,Edge> layout = vv.getGraphLayout();
        double dx = graphPoint.getX()-graphDown.getX();
        double dy = graphPoint.getY()-graphDown.getY();
        PickedState<Vertex> ps = vv.getPickedVertexState();
        
        for(Vertex v : ps.getPicked()) {
            Point2D vp = layout.transform(v);
            vp.setLocation(vp.getX()+dx, vp.getY()+dy);
            layout.setLocation(v, vp);
        }
        down = p;
    }
    
    public void translateDisplay(MouseEvent e) {
        MutableTransformer modelTransformer = vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        try {
            Point2D q = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(down);
            Point2D p = vv.getRenderContext().getMultiLayerTransformer().inverseTransform(e.getPoint());
            float dx = (float) (p.getX()-q.getX());
            float dy = (float) (p.getY()-q.getY());
            
            modelTransformer.translate(dx, dy);
            down.x = e.getX();
            down.y = e.getY();
        } catch (RuntimeException ex) {
            System.err.println("down = "+down+", e = "+e);
            throw ex;
        }
        e.consume();
        vv.repaint();
    }
    
    public void rotateDisplay(MouseEvent e) {
        MutableTransformer modelTransformer =
            vv.getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
        // rotate
        Point2D center = vv.getCenter();
        Point2D q = down;
        Point2D p = e.getPoint();
        Point2D v1 = new Point2D.Double(center.getX()-p.getX(), center.getY()-p.getY());
        Point2D v2 = new Point2D.Double(center.getX()-q.getX(), center.getY()-q.getY());
        double theta = angleBetween(v1, v2);
        modelTransformer.rotate(theta, vv.getRenderContext().getMultiLayerTransformer().inverseTransform(Layer.VIEW, center));
        down.x = e.getX();
        down.y = e.getY();
        e.consume();
    }
    
    /**
     * Returns the angle between two vectors from the origin
     * to points v1 and v2.
     * @param v1
     * @param v2
     * @return
     */
    protected double angleBetween(Point2D v1, Point2D v2) {
        double x1 = v1.getX();
        double y1 = v1.getY();
        double x2 = v2.getX();
        double y2 = v2.getY();
        // cross product for direction
        double cross = x1*y2 - x2*y1;
        int cw = 1;
        if(cross > 0) {
            cw = -1;
        } 
        // dot product for angle
        double angle = 
            cw*Math.acos( ( x1*x2 + y1*y2 ) / 
                ( Math.sqrt( x1*x1 + y1*y1 ) * 
                        Math.sqrt( x2*x2 + y2*y2 ) ) );
        if(Double.isNaN(angle)) {
            angle = 0;
        }
        return angle;
    }

    
    /**
     * rejects picking if the rectangle is too small, like
     * if the user meant to select one vertex but moved the
     * mouse slightly
     * @param p
     * @param q
     * @param min
     * @return
     */
    private boolean heyThatsTooClose(Point2D p, Point2D q, double min) {
        return Math.abs(p.getX()-q.getX()) < min &&
                Math.abs(p.getY()-q.getY()) < min;
    }
    
    /**
     * pick the vertices inside the rectangle created from points
     * 'down' and 'out'
     *
     */
    protected void pickContainedVertices(VisualizationViewer<Vertex,Edge> vv, Point2D down, Point2D out, boolean clear) {
        
        Layout<Vertex,Edge> layout = vv.getGraphLayout();
        PickedState<Vertex> pickedVertexState = vv.getPickedVertexState();
        
        Rectangle2D pickRectangle = new Rectangle2D.Double();
        pickRectangle.setFrameFromDiagonal(down,out);
         
        if(pickedVertexState != null) {
            if(clear) {
            	pickedVertexState.clear();
            }
            GraphElementAccessor<Vertex,Edge> pickSupport = vv.getPickSupport();

            Collection<Vertex> picked = pickSupport.getVertices(layout, pickRectangle);
            for(Vertex v : picked) {
            	pickedVertexState.pick(v, true);
            }
        }
    }
    
    /**
	 * zoom the display in or out, depending on the direction of the
	 * mouse wheel motion.
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		updateState(e); // entry point
		Point2D mouse = e.getPoint();
		Point2D center = vv.getCenter();
		int amount = e.getWheelRotation();
		if(zoomAtMouse) {
			if (amount < 0) {
				scaler.scale(vv, in, mouse);
			} else if(amount > 0) {
				scaler.scale(vv, out, mouse);
			}
		} else {
			if (amount < 0) {
				scaler.scale(vv, in, center);
			} else if(amount > 0) {
				scaler.scale(vv, out, center);
			}
		}
		e.consume();
		vv.repaint();
	}

    // TODO: clean-up: none of this being used
    
    public void mouseEntered(MouseEvent e) {
        // JComponent c = (JComponent) e.getSource();
    }

    public void mouseExited(MouseEvent e) {
        // JComponent c = (JComponent) e.getSource();
    }

    /**
     * @return Returns the locked.
     */
    public boolean isLocked() {
        return locked;
    }

    /**
     * @param locked The locked to set.
     */
    public void setLocked(boolean locked) {
        this.locked = locked;
    }
    
	public void addChangeListener(ChangeListener l) {
		changeSupport.addChangeListener(l);
	}

	public void fireStateChanged() {
		changeSupport.fireStateChanged();	
	}

	public ChangeListener[] getChangeListeners() {
		return changeSupport.getChangeListeners();
	}

	public void removeChangeListener(ChangeListener l) {
		changeSupport.removeChangeListener(l);
	}  
}