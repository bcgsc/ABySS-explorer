package ca.bcgsc.abyssexplorer.gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.event.ChangeListener;

import org.apache.commons.collections15.Transformer;

import ca.bcgsc.abyssexplorer.graph.AbyssGraph2;
import ca.bcgsc.abyssexplorer.graph.ContigLabel;
import ca.bcgsc.abyssexplorer.graph.DistanceEstimate;
import ca.bcgsc.abyssexplorer.graph.Edge;
import ca.bcgsc.abyssexplorer.graph.Vertex;
import ca.bcgsc.abyssexplorer.visualization.control.AbyssGraphMouse;
import ca.bcgsc.abyssexplorer.visualization.decorators.AbyssEdgeShape;
import ca.bcgsc.abyssexplorer.visualization.picking.AbyssShapePickSupport;
import ca.bcgsc.abyssexplorer.visualization.picking.PairedEndPathState;
import ca.bcgsc.abyssexplorer.visualization.picking.PartnerEdgeState;
import ca.bcgsc.abyssexplorer.visualization.renderers.AbyssEdgeRenderer;
import edu.uci.ics.jung.algorithms.layout.Layout;

import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.Context;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.visualization.Layer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.decorators.EdgeShape;
import edu.uci.ics.jung.visualization.layout.PersistentLayoutImpl;
import edu.uci.ics.jung.visualization.renderers.DefaultEdgeLabelRenderer;
import edu.uci.ics.jung.visualization.renderers.Renderer;
import edu.uci.ics.jung.visualization.transform.MutableTransformer;
import java.text.DecimalFormat;
import java.util.Collection;

/**
 * Extends edu.uci.ics.jung.visualization.VisualizationViewer
 * (authors: Joshua O'Madadhain, Tom Nelson, Danyel Fisher).
 * 
 * Controls the ABySS-Explorer graph display panel (maintains
 * the graph layout and graph selection states).
 * 
 * @author Cydney Nielsen
 *
 */
public class AbyssVisualizationViewer extends VisualizationViewer<Vertex,Edge> {
	
	AbyssGraph2 g; // parsed input data
	
	/**
	 * holds the paired end partner edges for the currently
	 * selected (picked) edge
	 */
	protected PartnerEdgeState partnerEdgeState;
	
	/**
	 * holds the paired end contigs for which the currently
	 * selected (picked) edge is a member
	 */
	protected PairedEndPathState pairedEndPathState;

        protected Integer seedId = null;
        protected Collection<Integer> path;
        
	public AbyssVisualizationViewer(PersistentLayoutImpl<Vertex,Edge> layout) {
		super(layout);
		setGraphMouse(new AbyssGraphMouse());
		partnerEdgeState = new PartnerEdgeState();
		pairedEndPathState = new PairedEndPathState();
		setBackground(Color.WHITE);
		setTransformers();
		setRenderers();
		setPickSupport();
	}
	
	public void activatePartnerDisplay() {
		partnerEdgeState.setDisplayActive(true);
	}
	
	public void activatePePathDisplay() {
		pairedEndPathState.setDisplayActive(true);
	}
	
	public void deactivatePartnerDisplay() {
		partnerEdgeState.setDisplayActive(false);
	}
	
	public void deactivatePePathDisplay() {
		pairedEndPathState.setDisplayActive(false);
	}
	
	public void setGraph(AbyssGraph2 g) {
		this.g = g;
	}
	
	public AbyssGraph2 getGraph() {
		return g;
	}
	
	public Graph<Vertex,Edge> getDisplayedGraph() {
		return getGraphLayout().getGraph();
	}
	
	public PairedEndPathState getPairedEndPathState() {
		return pairedEndPathState;
	}
	
	public PartnerEdgeState getPartnerEdgePickState() {
		return partnerEdgeState;
	}
	
	public List<ContigLabel> getPossiblePathLabels() {
		return pairedEndPathState.getPossibleLabels();
	}
	
        public List<Object[]> getInboundPartnerData(){
            List<DistanceEstimate> iDist = partnerEdgeState.getInbound();
            List<Object[]> data = null;
            if (iDist != null) {
                data = new ArrayList<Object[]>(iDist.size());
                for(DistanceEstimate d: iDist){
                    Object[] deData = new Object[]{d.getLabel(),d.getDist(),d.getError(),d.getNumPairs()};
                    data.add(deData);
                }
            }

            return data;
        }

        public List<Object[]> getOutboundPartnerData(){
            List<DistanceEstimate> oDist = partnerEdgeState.getOutbound();
            List<Object[]> data = null;
            if (oDist != null) {
                data = new ArrayList<Object[]>(oDist.size());
                for(DistanceEstimate d: oDist){
                    Object[] deData = new Object[]{d.getLabel(),d.getDist(),d.getError(),d.getNumPairs()};
                    data.add(deData);
                }
            }

            return data;
        }

//	public List<String> getInboundPartnerLabels() {
//		List<String> iLabels = null;
//		List<DistanceEstimate> iDist = partnerEdgeState.getInbound();
//		if (iDist != null) {
//
//                    if(iDist.size() > 0){
//                        List<DistanceEstimate> sortedList = new ArrayList<DistanceEstimate>(iDist.size());
//                        sortedList.add(iDist.get(0));
//                        for(int i=1; i<iDist.size(); i++){
//                            DistanceEstimate thisDE = iDist.get(i);
//
//                            int numSorted = sortedList.size();
//                            for(int j=0; j<numSorted; j++){
//                                if(sortedList.get(j).getDist() >= thisDE.getDist()){
//                                    sortedList.add(j, thisDE);
//                                    break;
//                                }
//                            }
//                            if(sortedList.size()-1 < i){
//                                sortedList.add(thisDE);
//                            }
//                        }
//
//                        //System.out.println(iDist.size() == sortedList.size());
//                        iDist = sortedList;
//                    }
//
//                        iLabels = new ArrayList<String>(iDist.size());
//			for (DistanceEstimate d: iDist) {
//                                iLabels.add(d.toString());
//			}
//		}
//		return iLabels;
//	}
//
//	public List<String> getOutboundPartnerLabels() {
//		List<String> oLabels = null;
//		List<DistanceEstimate> oDist = partnerEdgeState.getOutbound();
//		if (oDist != null) {
//
//                    if(oDist.size() > 0){
//                        List<DistanceEstimate> sortedList = new ArrayList<DistanceEstimate>(oDist.size());
//                        sortedList.add(oDist.get(0));
//                        for(int i=1; i<oDist.size(); i++){
//                            DistanceEstimate thisDE = oDist.get(i);
//
//                            int numSorted = sortedList.size();
//                            for(int j=0; j<numSorted; j++){
//                                if(sortedList.get(j).getDist() >= thisDE.getDist()){
//                                    sortedList.add(j, thisDE);
//                                    break;
//                                }
//                            }
//                            if(sortedList.size()-1 < i){
//                                sortedList.add(thisDE);
//                            }
//                        }
//
//                        //System.out.println(oDist.size() == sortedList.size());
//                        oDist = sortedList;
//                    }
//
//			oLabels = new ArrayList<String>(oDist.size());
//			for (DistanceEstimate d: oDist) {
//				oLabels.add(d.toString());
//			}
//		}
//		return oLabels;
//	}
	
	public Edge getInHighlightedPartner() {
		return partnerEdgeState.getInHighlighted();
	}
	
	public Edge getOutHighlightedPartner() {
		return partnerEdgeState.getOutHighlighted();
	}
	        
	public Edge getSelectedEdge() {
		Edge sEdge = null;
		Set<Edge> s = pickedEdgeState.getPicked();
		if (s != null) {
			if (s.size() > 1) {
				throw new IllegalArgumentException("Only one edge can be selected at a time (found " + s.size() + ")");
			} else if (s.size() == 1) {
				sEdge = (Edge) s.toArray()[0];
			}
		}
		return sEdge;
	}
	
	public String getSelectedEdgeString(int k) {
		Edge sEdge = getSelectedEdge();
		if (sEdge == null) {
			return null;
		}

                String s = sEdge.getLabel() + " ,";
		Integer sLen = sEdge.getLen();
		Float sCov = sEdge.getCoverage();
		if (sLen != null && sCov != null) {
			s += "(" + sLen + " bp";

                        if(sCov >= 0){
                            DecimalFormat formatter = new DecimalFormat("####.#");
                            String sCovFormatted = formatter.format(sCov);
                            if (k > 0){
                                s += "; " + sCovFormatted + " kmer cov";
                            }
                            else{
                                s += "; " + sCovFormatted + " cov";
                            }
                        }

                        s += ")";
		} else if (sLen != null) {
			s += "(" + sLen + " bp)"; 
		} else if (sCov != null && sCov >= 0) {

		      DecimalFormat formatter = new DecimalFormat("####.#");
		      String sCovFormatted = formatter.format(sCov);
                      if (k > 0){
		          s += "(" + sCovFormatted + " kmer cov)";
                      }
                      else{
                          s += "(" + sCovFormatted + " cov)";
                      }
		}
		s += "\n";
		
		return s;
	}
	
	public String getSelectedEdgeFormats() {
		String formats = "bold,selected,regular";
		return formats;
	}
	
	public String getSelectedPathLabel() {
		String sLabel = null;
		ContigLabel s = pairedEndPathState.getSelectedLabel();
		if (s != null) {
			sLabel = s.getLabel();
		}
		return sLabel;
	}
	
	public String getSelectedPathString() {
		String path = "Contig id: ,"; 
		path += pairedEndPathState.getSelectedLabel().getLabel() + "\n,";
		path += "Unitig members: ,";
		for (Object edge: pairedEndPathState.getSelectedMembers()) {
                        if(edge instanceof ContigLabel){
                            path += ((ContigLabel)edge).getLabel() + " ,";
                        }
                        else{
                            path += edge + " ,";
                        }
		}
		return path;
	}
	
	public String getSelectedPathStringFormats() {
		String formats = "bold,regular,bold,";
		for (Object label: pairedEndPathState.getSelectedMembers()) {
                    if(label instanceof ContigLabel){
                        Edge e = g.getEdge(((ContigLabel)label).getId());
			if (pickedEdgeState.isPicked(e)) {
				formats += "selected,";
			} else if (!getDisplayedGraph().containsEdge(e)){
				formats += "missing,";
			} else {
				formats += "regular,";
			}
                    }
                    else{
                        formats += "missing,";
                    }
		}
		return formats;
	}
	
	public void clear() {
		clearEdgeState();
		pickedVertexState.clear();
	}

	public void clearEdgeState() {
		pickedEdgeState.clear();
		partnerEdgeState.clear();
		pairedEndPathState.clear();
		setToolTipText(null);
	}
	
	/**
	 * Select the input Edge. Includes selecting
	 * input edge's paired-end partner edges and
	 * possible paired-end contigs (paths).
	 * 
	 * @param edge
	 */
	public void selectEdge(Edge edge) {
		clearEdgeState();
		setToolTipText(null);
		pickedEdgeState.pick(edge, true);
		selectPath(edge);
		selectPartners(edge);
	}
	
	/**
	 * Select the input paired-end contig (path). Includes
	 * selecting the paired-end partner edges of the path's
	 * terminal edges.
	 * 
	 * @param peLabel
	 */
	public void selectQueryPath(ContigLabel peLabel) {
		clear();
	
		// select partners of the terminal edges
		/*List<Edge> members = g.getPairedEndContigMembers(peLabel);
		Edge firstEdge = members.get(0);
		Edge lastEdge = members.get(members.size()-1);
		System.out.println("first label: " + firstEdge.getLabel() + " last label: " + lastEdge.getLabel());
		System.out.println("selecting partners for first edge: " + firstEdge.getLabel());
		selectPartners(firstEdge);
		System.out.println("selecting partners for last edge: " + lastEdge.getLabel());
		addPartnersToSelection(lastEdge);*/
		
		pairedEndPathState.setPossibleLabels(peLabel);
		selectPath(peLabel);
	}
	
	public void selectVertex(Vertex vertex) {
            if (pickedVertexState.isPicked(vertex) == false) {
                    pickedVertexState.clear();
                    pickedVertexState.pick(vertex, true);
            }
	}
	
	public void selectPartners(Edge edge) {
            // if PE partners are rendered, hightlight the partners and orient them if needed
            // otherwise, do nothing
            if(partnerEdgeState.isDisplayActive()){
		setToolTipText(null);
		partnerEdgeState.clear();
		addPartnersToSelection(edge);
            }
	}
	
	public void addPartnersToSelection(Edge edge) {
		List<DistanceEstimate> in = edge.getInboundPartners();
		if (in != null) {
			// ensure graph has up-to-date edge orientations
			for (DistanceEstimate iDis: in) {
				g.getEdge(iDis.getLabelObject());
			}
			partnerEdgeState.pick(in, "in");
		}
		List<DistanceEstimate> out = edge.getOutboundPartners();
		if (out != null) {
			// ensure graph has up-to-date edge orientations
			for (DistanceEstimate oDis: out) {
				g.getEdge(oDis.getLabelObject());
			}
			partnerEdgeState.pick(out, "out");
		}
	}
	
	/**
	 * Selects the first of all paired-end paths 
	 * for which the input single-end contig is 
	 * a member. Respects the strand of the input 
	 * edge. 
	 * 
	 * @param edge
	 */
	public void selectPath(Edge edge) {
            // if paths are rendered, then highlight the path and orient the contigs if needed
            // otherwise, do nothing
            if(pairedEndPathState.isDisplayActive()){

		List<ContigLabel> peLabels = edge.getPairedEndLabels(); // ids of PE contigs
		if (peLabels == null) { return; }
		
		// determine if this edge is on the currently selected path
		ContigLabel peSelected = pairedEndPathState.getSelectedLabel(); // currently selected PE contig id
		ContigLabel peToSelect = null;
		if (peSelected != null) {
			for (ContigLabel possiblePe: peLabels) {
				if (peSelected.getId() == possiblePe.getId()) {
					if (peSelected.getStrand() != possiblePe.getStrand()) {
						// correct the orientation if needed
						if (peSelected.getStrand() == 0) {
							peToSelect = new ContigLabel(peSelected.getId(), (byte) 1);
						} else {
							peToSelect = new ContigLabel(peSelected.getId(), (byte) 0);
						}
					} else {
						peToSelect = peSelected;
					}
					break;
				}
			}
		} 	
		if (peToSelect == null) {
                        // select the first possible pe
                        Collections.sort(peLabels);
                        peToSelect = peLabels.get(0);
		}
		pairedEndPathState.setPossibleLabels(peLabels);
		selectPath(peToSelect);
            }
	}
	
	public void selectPath(String s) {
		selectPath(new ContigLabel(s));
	}
	
	/**
	 * Select all single-end contigs that are members
	 * of the input paired-end contig considering the
	 * input strand of the paired-end contig.
	 * 
	 * @param peId
	 */
	public void selectPath(ContigLabel peLabel) {
		List<Object> members = g.getPairedEndContigMembers(peLabel);

                int sid = -1;
                
                // re-orient all contigs within the path to match the labels except the selected contig
                Edge theEdge = getSelectedEdge();              
                
                if(theEdge != null){
                    sid = theEdge.getId();                    
                }
                else{
                    sid = seedId;
                }
                
                if(sid >= 0){                    
                    for(Object l : members){
                        if(l instanceof ContigLabel){
                            ContigLabel lCL = (ContigLabel)l;
                            int id = lCL.getId();
                            if(id != sid){
                                Edge e = g.getEdge(id);
                                if(e.getStrand() != lCL.getStrand()){
                                    e.reverseComplement();
                                }
                            }
                        }
                    }
                }

		pairedEndPathState.setSelection(peLabel, members);
	}
	
	public boolean isEdgeSelected() {
		if (pickedEdgeState.getSelectedObjects().length == 0) {
			return false;
		} else {
			return true;
		}
	}
	
	public boolean isPairedEndSelected() {
		boolean selected = false;
		if (pairedEndPathState.getNumSelectedMembers() != 0) {
			selected = true;
		}
		return selected;
	}
	
	public void reverseComplementSelection(Edge edge) {
		
		edge.reverseComplement();

                //TODO: figure out whether this needs to be done whenever an edge is reverse complemented
                Graph<Vertex, Edge> dg = getDisplayedGraph();
                Transformer<Context<Graph<Vertex, Edge>,Edge>,Shape> tformer = getRenderContext().getEdgeShapeTransformer();

                // This is only a temporary work around
                dg.removeEdge(edge);
                dg.addEdge(edge, edge.getSourceVertex(), edge.getDestVertex(), EdgeType.DIRECTED);

                if(tformer instanceof EdgeShape.QuadCurve){
                    ((EdgeShape.QuadCurve) tformer).getEdgeIndexFunction().reset(dg, edge);
                }
                else if(tformer instanceof AbyssEdgeShape.Squiggle){
                    ((AbyssEdgeShape.Squiggle) tformer).getEdgeIndexFunction().reset(dg, edge);
                }

		selectPath(edge);
		selectPartners(edge);
	}
	
	/**
	 * Overrides some of the default Transformers in JUNG's
	 * edu.uci.ics.jung.visualization.PluggableRenderContex
	 */
	protected void setTransformers() {
	    
		setVertexColorTransformer();
		setVertexShapeTransformer();
                setVertexStrokeTransformer();
		setEdgeColorTransformer();
        
        setEdgeThicknessTransformer();
		turnContigLabelsOn();
		setEdgeLabelColor();
		setEdgeFontTransformer();
		
		turnContigLenOn();
		
	}
	
	protected void setVertexColorTransformer() {
		Transformer<Vertex, Paint> vertexPaint = new Transformer<Vertex, Paint>() {
			public Paint transform(Vertex v) {
                                if (getPickedVertexState().isPicked(v)) {
					return Color.darkGray;
                                } else if (missingEdges(v)) {
					return Color.white;
                                } else if (v.getNumAberrantOverlaps() > 0){
                                    return new Color(236, 112, 20); // orange
                                } else if (v.getNumInferredOverlaps() > 0){
                                    return new Color(254,196,79); // yellow
				} else {
					return Color.lightGray;
				}
			}
		};
		Transformer<Vertex, Paint> vertexEdgePaint = new Transformer<Vertex, Paint>() {
			public Paint transform(Vertex v) {
				if (getPickedVertexState().isPicked(v)) {
					return Color.darkGray;
				} else if (v.getNumAberrantOverlaps() > 0){
                                    return new Color(236, 112, 20); // orange
                                } else if (v.getNumInferredOverlaps() > 0){
                                    return new Color(254,196,79); // yellow
//				} else if (missingEdges(v)) {
//					return new Color(150,150,150);
				} else {
					return Color.lightGray;
				}
			}
		};
		getRenderContext().setVertexFillPaintTransformer(vertexPaint);
		getRenderContext().setVertexDrawPaintTransformer(vertexEdgePaint);
	}

        protected void setVertexStrokeTransformer(){
            Transformer<Vertex, Stroke> vertexStroke = new Transformer<Vertex, Stroke>() {
                public Stroke transform(Vertex v){
                    return new BasicStroke(3);
                }
            };
            getRenderContext().setVertexStrokeTransformer(vertexStroke);
        }
	
	public boolean missingEdges(Vertex v) {
		boolean missing = false;
		Graph<Vertex,Edge> dGraph = getDisplayedGraph();
		for (Edge oEdge: v.getOutgoing()) {
			if (!dGraph.containsEdge(oEdge)) {
				//missing = true;
                            return true;
			}
		}
		for (Edge iEdge: v.getIncoming()) {
			if (!dGraph.containsEdge(iEdge)) {
				//missing = true;
                            return true;
			}
		}
		return missing;
	}

        public Edge getLargestMissingEdge(Vertex v){
            Edge largestMissingEdge = null;
		Graph<Vertex,Edge> dGraph = getDisplayedGraph();
		for (Edge oEdge: v.getOutgoing()) {
			if (!dGraph.containsEdge(oEdge)) {
                            if(largestMissingEdge == null){
                                largestMissingEdge = oEdge;
                            }
                            else{
                                if(largestMissingEdge.getLen() < oEdge.getLen()){
                                    largestMissingEdge = oEdge;
                                }
                            }
			}
		}
		for (Edge iEdge: v.getIncoming()) {
			if (!dGraph.containsEdge(iEdge)) {
                            if(largestMissingEdge == null){
                                largestMissingEdge = iEdge;
                            }
                            else{
                                if(largestMissingEdge.getLen() < iEdge.getLen()){
                                    largestMissingEdge = iEdge;
                                }
                            }
			}
		}
                return largestMissingEdge;
        }
	
	protected void setVertexShapeTransformer() {
		Transformer<Vertex, Shape> vertexShape = new Transformer<Vertex, Shape>() {
			final int VERTEX_HEIGHT = 25;
			final int VERTEX_WIDTH = 10;
			final int VERTEX_POINT = 10;
			public Shape transform(Vertex v) {

                            Graph<Vertex,Edge> graph = getDisplayedGraph();
				if (v.hasConsistentPoles(graph)) {
					return new Ellipse2D.Float(-VERTEX_POINT / 2, -VERTEX_POINT / 2,
							VERTEX_POINT, VERTEX_POINT);
				} else {
					return new Ellipse2D.Float(-VERTEX_WIDTH / 2,
							-VERTEX_HEIGHT / 2, VERTEX_WIDTH, VERTEX_HEIGHT);
				}
			}
		};
		getRenderContext().setVertexShapeTransformer(vertexShape);
	}
	
	protected void setEdgeColorTransformer() {
		Transformer<Edge, Paint> edgePaint = new Transformer<Edge, Paint>() {
			public Paint transform(Edge e) {
				if (pickedEdgeState.isPicked(e)) {
					// current selection is medium orange (takes priority)
					//return new Color(236, 112, 20);
                                    return new Color(51,160,44); // green
				} else if (partnerEdgeState.isHighlighted(e) && partnerEdgeState.isDisplayActive()) {
					// use same medium orange as for selection
					//return new Color(236, 112, 20);
                                    return new Color(51,160,44); // green
				} else if (pairedEndPathState.isPicked(e.getLabelObject()) && pairedEndPathState.isDisplayActive()) {
					// Paired-end path takes priority over partner distance estimates
					float index = ((Integer) pairedEndPathState.getPosition(e.getLabelObject())).floatValue();
					float rIndex = index/pairedEndPathState.getNumSelectedMembers();
					return getBlue(rIndex);
				} else if (partnerEdgeState.isInbound(e) && partnerEdgeState.isDisplayActive()) {
					// inbound are light orange
					//return new Color(254, 196, 79);
                                        return new Color(194,165,207); // light purple
				} else if (partnerEdgeState.isOutbound(e) && partnerEdgeState.isDisplayActive()) {
					// outbound are dark orange
					//return new Color(153, 52, 4);
                                        return new Color(118,42,131); // dark purple
                                } else if ((seedId != null && e.getId() == seedId)
                                        || (path != null && path.contains(e.getId()))){
                                    return new Color(50,205,50); // light green
				} else {
					return Color.gray;
				}
			}
		};
		getRenderContext().setEdgeDrawPaintTransformer(edgePaint);
		getRenderContext().setArrowDrawPaintTransformer(edgePaint);
		getRenderContext().setArrowFillPaintTransformer(edgePaint);
	}
	
	protected Color getBlue(float percent) {
		// float rDark = 44; float gDark = 127; float bDark = 184;
		float rDark = 8; float gDark = 81; float bDark = 156;
		float rLight = 158; float gLight = 202; float bLight = 225;
		
		float rNew = (rLight - (percent * (rLight - rDark)))/255;
		float gNew = (gLight - (percent * (gLight - gDark)))/255;
		float bNew = (bLight - (percent * (bLight - bDark)))/255;
		
		return new Color(rNew, gNew, bNew);
	}
	
	protected void setEdgeThicknessTransformer() {
		Transformer<Edge, Stroke> edgeThickness = new Transformer<Edge, Stroke>() {
			public Stroke transform(Edge e) {
				if (pickedEdgeState.isPicked(e)) {
					return new BasicStroke(4);
				} else if (pairedEndPathState.isPicked(e.getLabelObject()) && pairedEndPathState.isDisplayActive()) {
					return new BasicStroke(4);
				} else if ((partnerEdgeState.isInbound(e) || partnerEdgeState.isOutbound(e)) && partnerEdgeState.isDisplayActive()) {
					return new BasicStroke(4);
				} else {
					return new BasicStroke(2);
				}
			}
		};
		Transformer<Edge, Stroke> arrowThickness = new Transformer<Edge, Stroke>() {
			public Stroke transform(Edge e) { return new BasicStroke(2); }
		};
		getRenderContext().setEdgeStrokeTransformer(edgeThickness);
		// getRenderContext().setEdgeArrowStrokeTransformer(edgeThickness);
		getRenderContext().setEdgeArrowStrokeTransformer(arrowThickness);
	}
	
	protected void setEdgeLabelColor() {
		getRenderContext().setEdgeLabelRenderer(new DefaultEdgeLabelRenderer(Color.black));
	}
	
	protected void setEdgeFontTransformer() {
		Transformer<Edge,Font> edgeFont = new Transformer<Edge,Font>() {
			public Font transform(Edge e) {
				return new Font("Helvetica", Font.PLAIN, 14);
			}
		};
		getRenderContext().setEdgeFontTransformer(edgeFont);
	}
	
	public void setLenScale(int s) {
		if (getRenderContext().getEdgeShapeTransformer() instanceof AbyssEdgeShape.Squiggle) {
			((AbyssEdgeShape.Squiggle<Vertex,Edge>) getRenderContext().getEdgeShapeTransformer()).setLenScale(s);
		}
	}
	
	public void setInHighlightedPartner(String label) {
		Edge e = null;
		if (label != null) {
			e = getGraph().getEdge(label);
		} 
		partnerEdgeState.setInHighlighted(e);
	}
	
	public void setOutHighlightedPartner(String label) {
		Edge e = null;
		if (label != null) {
			e = getGraph().getEdge(label);
		} 
		partnerEdgeState.setOutHighlighted(e);
	}
	
	protected void setRenderers() {
		getRenderer().getVertexLabelRenderer().setPosition(
				Renderer.VertexLabel.Position.CNTR);
		getRenderer().setEdgeRenderer(new AbyssEdgeRenderer());
	}
	
	public void turnContigLenOn() {
		AbyssEdgeShape.Squiggle<Vertex,Edge> edgeShapeTransformer = new AbyssEdgeShape.Squiggle<Vertex,Edge>();
                edgeShapeTransformer.setControlOffsetIncrement(200.0f);
//                edgeShapeTransformer.setEdgeIndexFunction(MyDefaultParallelEdgeIndexFunction.<Vertex, Edge>getInstance());
                getRenderContext().setEdgeShapeTransformer(edgeShapeTransformer);
	}
	
	public void turnContigLenOff() {
		EdgeShape.QuadCurve<Vertex, Edge> edgeShapeTransformer = new EdgeShape.QuadCurve<Vertex, Edge>();
//                edgeShapeTransformer.setEdgeIndexFunction(MyDefaultParallelEdgeIndexFunction.<Vertex, Edge>getInstance());
		getRenderContext().setEdgeShapeTransformer(edgeShapeTransformer);
	}
	
	public void turnContigLabelsOn() {
		Transformer<Edge,String> edgeLabel = new Transformer<Edge,String>() {
			public String transform(Edge e) {
				return e.getLabel();
			}
		};
		getRenderContext().setEdgeLabelTransformer(edgeLabel);
	}
	
	public void turnContigLabelsOff() {
		Transformer<Edge,String> edgeLabel = new Transformer<Edge,String>() {
			public String transform(Edge e) {
				return "";
			}
		};
		getRenderContext().setEdgeLabelTransformer(edgeLabel);
	}
	
	/**
	 * Ensures correct shapes are returned for selected items
	 */
	protected void setPickSupport() {
		getRenderContext().setPickSupport(
				new AbyssShapePickSupport(this));
	}
	
	public void addModelChangeListener(ChangeListener c) {
		getModel().addChangeListener(c);
	}

	public void addGraphMouseChangeListener(ChangeListener c) {
		((AbyssGraphMouse) getGraphMouse()).addChangeListener(c);
	}
	
	public void addEdgeItemListener(ItemListener l) {
		pickedEdgeState.addItemListener(l);
		
	}
	
	public void addPartnerChangeListener(ChangeListener c) {
		partnerEdgeState.addChangeListener(c);
	}
	
	public void addPathChangeListener(ChangeListener c) {
		pairedEndPathState.addChangeListener(c);
	}
	
	public void addPartnerEdgeChangeListener(ChangeListener c) {
		partnerEdgeState.addChangeListener(c);
	}

    @Override
        public void setGraphLayout(Layout<Vertex, Edge> layout){
            super.setGraphLayout(layout);

            centerGraph();
        }

        public void centerGraph(){
            Layout<Vertex, Edge> layout = getGraphLayout();
            layout.setSize(this.getSize());

            MutableTransformer modelTransformer = getRenderContext().getMultiLayerTransformer().getTransformer(Layer.LAYOUT);
            modelTransformer.setToIdentity();
        }

        public void setSeed(Integer seedId){
            this.seedId = seedId;
            path = null;
        }

        public void setPath(Collection<Integer> path){
            this.path = path;
            seedId = null;
        }

        public ArrayList<Edge> getPath(){
            if(path == null){
                return null;
            }

            ArrayList<Edge> edgePath = new ArrayList<Edge>(path.size());
            for(Integer id : path){
                edgePath.add(g.getEdge(id));
            }
            return edgePath;
        }

	/**
	 * 
	 */
	private static final long serialVersionUID = 484495586168643969L;

}
