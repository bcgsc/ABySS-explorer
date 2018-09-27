package ca.bcgsc.abyssexplorer.graph;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Class to capture a sequence contig edge.
 * 
 * @author Cydney Nielsen
 * 
 */
public class Edge implements Comparable<Edge> {

    public final static byte PLUS = ContigLabel.PLUS;
    public final static byte MINUS = ContigLabel.MINUS;
    
	protected ContigLabel eLabel;
	protected Integer len;
	protected Float coverage;
	protected boolean hasInvertedRepeats;
	
	// two vertex objects; first = source, second = destination
	protected Vertex[] vertices;
	
	// partner edges linked by paired end reads relative to this edge's positive strand
	public List<DistanceEstimate> iPartners;
	public List<DistanceEstimate> oPartners;
	
	// parent paired-end contig ids relative to this edge's positive strand
	protected List<ContigLabel> peLabels;
	
	public Edge(String eLabel) {
		processLabel(eLabel);
		hasInvertedRepeats = false;
		len = -1; coverage = -1F;
		vertices = new Vertex[2];
	}

	/**
	 * Corrects for current orientation of this Edge.
	 * @param e
	 * @param d
	 * @param error
	 * @param numPairs
	 */
	public void addInboundPartner(Edge e, Integer d, Float error, Integer numPairs) {
		if (e == null) {
			throw new IllegalArgumentException("inbound partner edge cannot be null");
		} else {
			assert e.getId() != this.getId(): "Did not expect inbound partner to have same id: " + this.getId();
			initializeDistInfo();
			// partners are stored for this edge's positive strand orientation
			if (eLabel.getStrand() == ContigLabel.PLUS) {
				// copy edge's label for storage in a DistanceEstimate object
				ContigLabel iLabel = new ContigLabel(e.getId(), e.getStrand());
				iPartners.add(new DistanceEstimate(iLabel, d, error, numPairs));
			} else {
				// store as outbound; e.g. 2+ into 1- == 2- out of 1+
				oPartners.add(new DistanceEstimate(e.getRevCompLabelObject(), d, error, numPairs));
			}
		}
	}
	
	public void addOutboundPartner(Edge e, Integer d, Float error, Integer numPairs) {
		if (e == null) {
			throw new IllegalArgumentException("outbound partner edge cannot be null");
		} else if (d == null) {
			throw new IllegalArgumentException("outbound partner distance cannot be null");
		} else {
			assert e.getId() != this.getId(): "Did not expect outbound partner to have same id: " + this.getId();
			initializeDistInfo();
			// partners are stored for this edge's positive strand orientation
			if (eLabel.getStrand() == ContigLabel.PLUS) {
				// copy edge's label for storage in DistanceEsimate object
				ContigLabel oLabel = new ContigLabel(e.getId(), e.getStrand());
				oPartners.add(new DistanceEstimate(oLabel, d, error, numPairs));
			} else {
				// store as inbound; e.g. 2+ out of 1- == 2- into 1+
				iPartners.add(new DistanceEstimate(e.getRevCompLabelObject(), d, error, numPairs));
			}
		}
	}
	
	protected void initializeDistInfo() {
		if (iPartners == null) {
			iPartners = new LinkedList<DistanceEstimate>();
			oPartners = new LinkedList<DistanceEstimate>();
		}
	}
	
	/** 
	 * Store a parent paired-end (pe) id for this single-end (se) contig.
	 * Orientation of this se contig relative to the pe contig is not
	 * stored here, but rather in the graph itself.
	 * 
	 * @param peId
	 */
	public void addPairedEndId(ContigLabel l) {
		if (peLabels == null) {
			peLabels = new LinkedList<ContigLabel>();
		}
		// only add a peLabel once
		for (ContigLabel pe: peLabels) {
			if (l.getId() == pe.getId() && l.getStrand() == pe.getStrand()) {
				return;
			}
		}
		peLabels.add(l);
	}
	
	public int compareTo(Edge other) {
		/**
		 * Compares edges based on ids
		 */
		if (eLabel.getId() == other.getId()) {
			return 0;
		} else if (eLabel.getId() > other.getId()) {
			return 1;
		} else {
			return -1;
		}
	} 
	
	public Float getCoverage() {
		return coverage;
	}

	public Vertex getDestVertex() {
		return getDestVertex(eLabel.getStrand());
	}
        
	public Vertex getDestVertex(byte strand) {
		if (strand == PLUS) {
			return vertices[1];
		} else {
			return vertices[0];
		}
	}
        
        public List<ContigLabel> getTrueOutgoingEdgeLabels(byte strand){
            List<ContigLabel> labels = new LinkedList<ContigLabel>();
            
            Vertex dv = this.getDestVertex(strand);
            byte pole = dv.getIncomingPole(this, strand);
            
            //find all outgoing edges from this pole
            if(pole == 0 || pole == 1){
                HashSet<Edge> dvalledges = new HashSet<Edge>();
                dvalledges.addAll(dv.incoming.values());
                dvalledges.addAll(dv.outgoing.values());
                
                for (Edge e : dvalledges){                    
                    if (pole == dv.getOutgoingPole(e, ContigLabel.PLUS)){
                        labels.add(new ContigLabel(e.getId(), ContigLabel.PLUS));
                        continue;
                    }
                    else if(pole == dv.getOutgoingPole(e, ContigLabel.MINUS)){
                        labels.add(new ContigLabel(e.getId(), ContigLabel.MINUS));
                    }
                }
            }
            
            return labels;
        }
	
        public List<ContigLabel> getTrueIncomingEdgeLabels(byte strand){
            List<ContigLabel> labels = new LinkedList<ContigLabel>();
            
            Vertex dv = this.getSourceVertex(strand);
            byte pole = dv.getOutgoingPole(this, strand);
            
            //find all outgoing edges from this pole
            if(pole == 0 || pole == 1){
                HashSet<Edge> dvalledges = new HashSet<Edge>();
                dvalledges.addAll(dv.incoming.values());
                dvalledges.addAll(dv.outgoing.values());
                
                for (Edge e : dvalledges){                    
                    if (pole == dv.getIncomingPole(e, ContigLabel.PLUS)){
                        labels.add(new ContigLabel(e.getId(), ContigLabel.PLUS));
                        continue;
                    }
                    else if(pole == dv.getIncomingPole(e, ContigLabel.MINUS)){
                        labels.add(new ContigLabel(e.getId(), ContigLabel.MINUS));
                    }
                }
            }
            
            return labels;
        }
        
	public String getLabel() {
		return eLabel.getLabel();
	}
	
	public ContigLabel getLabelObject() {
		return eLabel;
	}
	
	public Integer getLen() {
		return len;
	}
	
	/**
	 * @return the id
	 */
	public int getId() {
		return eLabel.getId();
	}
	
	public DistanceEstimate getInboundDistEstimate(ContigLabel l) {
		DistanceEstimate iDist = null;
		if (eLabel.getStrand() == ContigLabel.PLUS) {
			for (DistanceEstimate d: iPartners) {
				if (d.getLabelObject().equals(l)) {
					iDist = d;
					break;
				}
			}
		} else {
			// compare to rc of outbound partners
			// e.g. if 43+ is outbound of 23+, 43- is inbound of 23-
			for (DistanceEstimate d: oPartners) {
				ContigLabel oLabel = d.getLabelObject();
				if (oLabel.getId() == l.getId() && oLabel.getStrand() == l.getRevCompStrand()) {
					ContigLabel rcLabel = new ContigLabel(l.getId(), l.getStrand());
					iDist = new DistanceEstimate(rcLabel, d.getDist(), d.getError(), d.getNumPairs());
					break;
				}
			}
		}
		return iDist;
	}
	
	public DistanceEstimate getOutboundDistEstimate(ContigLabel l) {
		DistanceEstimate oDist = null;
		if (eLabel.getStrand() == ContigLabel.PLUS) {
			for (DistanceEstimate d: oPartners) {
				if (d.getLabelObject().equals(l)) {
					oDist = d;
					break;
				}
			}
		} else {
			// compare to rc of inbount partners
			// e.g. if 43+ is inbound of 23+, 43- is outbound of 23-
			for (DistanceEstimate d: iPartners) {
				ContigLabel iLabel = d.getLabelObject();
				if (iLabel.getId() == l.getId() && iLabel.getStrand() == l.getRevCompStrand()) {
					ContigLabel rcLabel = new ContigLabel(l.getId(), l.getStrand());
					oDist = new DistanceEstimate(rcLabel, d.getDist(), d.getError(), d.getNumPairs());
					break;
				}
			}
		}
		return oDist;
	}
	
	/**
	 * Returns correctly oriented inbound partner edges 
	 * for the edge's current strand.
	 * 
	 * @return
	 */
	public List<DistanceEstimate> getInboundPartners() {
		if (getStrand() == ContigLabel.PLUS) {
			return iPartners;
		} else {
			// System.out.println("returning oPartners copy");
			if (oPartners == null) { return null; }
			// return reverse-complemented copies of outbound partners
			List<DistanceEstimate> rcPartners = new ArrayList<DistanceEstimate>(oPartners.size());
			for (DistanceEstimate oDis: oPartners) {
				ContigLabel oLabel = oDis.getLabelObject();
				ContigLabel rcoLabel = new ContigLabel(oLabel.getId(), oLabel.getRevCompStrand());
				rcPartners.add(new DistanceEstimate(rcoLabel, oDis.getDist(), oDis.getError(), oDis.getNumPairs()));
			}
			return rcPartners;
		}
	}
	
	/**
	 * Returns correctly oriented outbound partners edges
	 * for the edge's current strand.
	 * 
	 * @return
	 */
	public List<DistanceEstimate> getOutboundPartners() {
		if (getStrand() == ContigLabel.PLUS) {
			return oPartners;
		} else {
			// System.out.println("returning iPartners copy");
			if (iPartners == null) { return null; }
			// return reverse-complemented copies of inbound partners
			List<DistanceEstimate> rcPartners = new ArrayList<DistanceEstimate>(iPartners.size());
			for (DistanceEstimate iDis: iPartners) {
				ContigLabel iLabel = iDis.getLabelObject();
				ContigLabel rciLabel = new ContigLabel(iLabel.getId(), iLabel.getRevCompStrand());
				rcPartners.add(new DistanceEstimate(rciLabel, iDis.getDist(), iDis.getError(), iDis.getNumPairs()));
			}
			return rcPartners;
		}
	}
	
	public List<ContigLabel> getPairedEndLabels() {
		if (peLabels == null) { return null; }
		if (getStrand() == ContigLabel.PLUS) {
			// return as stored
			return peLabels;
		} else {
			// return reverse-complemented copies
			List<ContigLabel> rcLabels = new ArrayList<ContigLabel>(peLabels.size());
			for (ContigLabel peLabel: peLabels) {
				rcLabels.add(new ContigLabel(peLabel.getId(), peLabel.getRevCompStrand()));
			}
			return rcLabels;
		}
	}
	
	public String getRevCompLabel() {
		return eLabel.getRevCompLabel();
	}
	
	public ContigLabel getRevCompLabelObject() {
		return new ContigLabel(eLabel.getId(), eLabel.getRevCompStrand());
	}

	public Vertex getSourceVertex() {
		return getSourceVertex(eLabel.getStrand());
	}
        
        public Vertex getSourceVertex(byte strand){
		if (strand == PLUS) {
			return vertices[0];
		} else {
			return vertices[1];
		}           
        }
	
	/**
	 * @return the strand
	 */
	public byte getStrand() {
		return eLabel.getStrand();
	}
	
	public Vertex[] getVertices() {
		if (eLabel.getStrand() == ContigLabel.PLUS) {
			return vertices;
		} else {
			// vertices are stored for edge's positive strand orientation
			if (vertices[0] == vertices[1]) {
				// covers both palindrome and inverted repeat cases
				return vertices;
			} else {
				Vertex[] tmpVertices = new Vertex[2];
				tmpVertices[0] = vertices[1];
				tmpVertices[1] = vertices[0];
				return tmpVertices;
			}
		}
	}
	
	/**
	 * @return boolean as to whether terminates with inverted repeats
	 */
	public boolean hasInvertedRepeats() {
		return hasInvertedRepeats;
	}
	
	protected void processLabel(String l) {
		int id;
		if (l.endsWith("+")) {
			id = Integer.parseInt(l.replace("+",""));
			eLabel = new ContigLabel(id, ContigLabel.PLUS);
		} else if (l.endsWith("-")) {
			id = Integer.parseInt(l.replace("-",""));
			eLabel = new ContigLabel(id, ContigLabel.MINUS);
		} else {
			throw new IllegalArgumentException("Invalid eLabel: '" + l + "'");
		}
	}
	
	public void setLen(Integer len) {
		this.len = len;
	}
	
	public void setCoverage(Float coverage) {
		this.coverage = coverage;
	}
	
	/**
	 * @param vertices the vertices to set
	 */
	public void setVertices(Vertex[] vertices) {
		setVertices(vertices[0], vertices[1]);
	}
	
	public void setVertices(Vertex v1, Vertex v2) {
		setFirstVertex(v1);
		setSecondVertex(v2);
		/*if (eLabel.getStrand() == 0) {
			vertices[0] = v1;
			vertices[1] = v2;
		} else {
			if (v1 == v2) {
				// covers both palindrome and inverted repeat cases
				vertices[0] = v1;
				vertices[1] = v2;
			} else {
				// want the rc of stored values
				vertices[0] = v2;
				vertices[1] = v1;
			}
		}*/
	}
	
	public void setFirstVertex(Vertex v1) {
		if (eLabel.getStrand() == ContigLabel.PLUS) { // || v1.isPalindrome) { 
			this.vertices[0] = v1;
		} else {
			// want the rc of stored values
			this.vertices[1] = v1;
		}
	}
	
	/**
	 * @param b as to whether terminates with inverted repeats
	 */
	public void setHasInvertedRepeats(boolean b) {
		hasInvertedRepeats = b;
	}
	
	public void setSecondVertex(Vertex v2) {
		if (eLabel.getStrand() == ContigLabel.PLUS) { // || v2.isPalindrome) {
			this.vertices[1] = v2;
		} else {
			// want the rc of stored values
			this.vertices[0] = v2;
		}
	}
	
	public void reverseComplement() {
		if (eLabel.getStrand() == ContigLabel.PLUS) {
			eLabel.setStrand(ContigLabel.MINUS);
		} else {
			eLabel.setStrand(ContigLabel.PLUS);
		}
	}
	

	public boolean isIncident(Edge e){
            return getDestVertex().isIncident(e) || getSourceVertex().isIncident(e);
        }
}
