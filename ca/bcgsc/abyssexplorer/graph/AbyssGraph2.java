/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package ca.bcgsc.abyssexplorer.graph;

import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.DirectedSparseMultigraph;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.util.EdgeType;
import edu.uci.ics.jung.graph.util.Pair;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;

/**
 *
 * @author kmnip
 */
public class AbyssGraph2 implements Graph<Vertex, Edge>{

    protected String name;
    protected boolean pairedEndAssembly = false;

    protected HashMap<Integer,Object[]> peMemberLabels; // map paired-end contig ids to member labels
    
    protected HashMap<Integer, Vertex> vertices = new HashMap<Integer, Vertex>();
    protected HashMap<Integer, Edge> edges = new HashMap<Integer, Edge>();


    public AbyssGraph2 () {
    }
    
    public void setName(String name) {
            this.name = name;
    }

    public String getName() {
            return name;
    }
    



    public Collection<Edge> getInEdges(Vertex v) {
        System.out.println("getInEdges not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Edge> getOutEdges(Vertex v) {
        return getIncidentEdges(v);
//        return v.getOutgoing();
    }

    public Collection<Vertex> getPredecessors(Vertex v) {
        System.out.println("getPredecessors not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Vertex> getSuccessors(Vertex v) {
        System.out.println("getSuccessors not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int inDegree(Vertex v) {
        System.out.println("inDegree not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int outDegree(Vertex v) {
        System.out.println("outDegree not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isPredecessor(Vertex v, Vertex v1) {
        System.out.println("isPredecessor not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isSuccessor(Vertex v, Vertex v1) {
        System.out.println("isSuccessor not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getPredecessorCount(Vertex v) {
        System.out.println("getPredecessorCount not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getSuccessorCount(Vertex v) {
        System.out.println("getSuccessorCount not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vertex getSource(Edge e) {
        System.out.println("getSource not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Vertex getDest(Edge e) {
        System.out.println("getDest not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isSource(Vertex v, Edge e) {
        System.out.println("isSource not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isDest(Vertex v, Edge e) {
        System.out.println("isDest not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean addEdge(Edge e, Vertex v, Vertex v1) {
        System.out.println("addEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean addEdge(Edge e, Vertex v, Vertex v1, EdgeType et) {
        System.out.println("addEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Pair<Vertex> getEndpoints(Edge e) {
        return new Pair<Vertex>(e.getSourceVertex(), e.getDestVertex());
    }

    public Vertex getOpposite(Vertex v, Edge e) {
        Vertex[] vs = e.getVertices();
        if(vs[0] == v){
            return vs[1];
        }
        else {
            return vs[0];
        }
    }

    public Collection<Edge> getEdges() {
        return edges.values();
    }
    
    public Edge getRandomEdge(){
        Collection<Edge> edges_only = edges.values();
        int i = (new Random()).nextInt(edges_only.size());
        int counter = 0;
        for (Edge e : edges_only){
            counter++;
            if (counter == i){
                return e;
            }
        }
        return null;
    }

    public Collection<Vertex> getVertices() {
        return vertices.values();
    }

    public Vertex getVertex(int id){
        return vertices.get(id);
    }

    public boolean containsVertex(Vertex v) {
        return vertices.containsValue(v);
    }

    public boolean containsEdge(Edge e) {
        return edges.containsValue(e);
    }

    public int getEdgeCount() {
        return edges.size();
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public Collection<Vertex> getNeighbors(Vertex v) {
        System.out.println("getNeighbors not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Edge> getIncidentEdges(Vertex v) {
        List<Edge> ie = new LinkedList<Edge>();
        ie.addAll(v.incoming.values());
        ie.addAll(v.outgoing.values());
        return ie;
    }

    public Collection<Edge> getIncidentEdges(Edge e) {
            Collection<Edge> incident = new HashSet<Edge>();
            for (Vertex v: e.getVertices()) {
                    Collection<Edge> v_incident = getIncidentEdges(v);
                    if (v_incident != null) { incident.addAll(v_incident); }
            }
            return incident;
    }

    public Collection<Vertex> getIncidentVertices(Edge e) {
        List<Vertex> ie = new LinkedList<Vertex>();
        Vertex[] vs = e.vertices;
        ie.add(vs[0]);
        ie.add(vs[1]);
        
        return ie;
    }

    public Edge findEdge(Vertex v, Vertex v1) {
        System.out.println("findEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Edge> findEdgeSet(Vertex v, Vertex v1) {
        System.out.println("findEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean addVertex(Vertex v) {
        int id = v.getId();
        if(id < 0){
            return false;
        }

        vertices.put(id, v);
        return true;
    }

    public boolean addEdge(Edge e) {
        int id = e.getId();
        if(id < 0){
            return false;
        }

        edges.put(id, e);
        return true;
    }

    public boolean addEdge(Edge e, Collection<? extends Vertex> clctn) {
        System.out.println("addEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean addEdge(Edge e, Collection<? extends Vertex> clctn, EdgeType et) {
        System.out.println("addEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean removeVertex(Vertex v) {
        Vertex r = vertices.remove(v.getId());
        return r == null;
    }

    public boolean removeEdge(int id){
        Edge e = edges.remove(new Integer(id));
        return (e != null);
    }
    
    public boolean removeEdge(Edge e) {
        System.out.println("removeEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isNeighbor(Vertex v, Vertex v1) {
        System.out.println("isNeighbor not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isIncident(Vertex v, Edge e) {
        System.out.println("isIncident not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int degree(Vertex v) {
        System.out.println("degree not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getNeighborCount(Vertex v) {
        System.out.println("getNeighborCount not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getIncidentCount(Edge e) {
        System.out.println("getIncidentCount not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public EdgeType getEdgeType(Edge e) {
        System.out.println("getEdgeType not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Collection<Edge> getEdges(EdgeType et) {
        System.out.println("getEdge not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getEdgeCount(EdgeType et) {
        System.out.println("getEdgeCount not supported");
        throw new UnsupportedOperationException("Not supported yet.");
    }


	public boolean hasEdge(String label) {
		if (getEdge(label) != null) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Retrieves the edge with the corresponding label.
	 *
	 * @param eLabel
	 * @return
	 * @throws IndexOutOfBoundsException
	 * @throws NumberFormatException
	 */
	public Edge getEdge(String eLabel) {

		int index = -1; int strand = 0;
		if (eLabel.endsWith("+")) {
			index = Integer.parseInt(eLabel.replace("+",""));
			strand = 0;
		} else if (eLabel.endsWith("-")) {
			index = Integer.parseInt(eLabel.replace("-",""));
			strand = 1;
		} else {
			throw new IllegalArgumentException("Invalid eLabel: '" + eLabel + "'");
		}
		Edge e = getEdge(index, strand);

		return e;
	}

	public Edge getEdge(ContigLabel l) {
		if (l == null) { return null; }
		return getEdge(l.getId(), l.getStrand());
	}

	/**
	 * Retrieves the edge with the corresponding id and strand.
	 * More efficient than getEdge(String eLabel) if already have
	 * edge info in this form.
	 *
	 * @param id
	 * @param strand
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public Edge getEdge(int id, int strand) throws IndexOutOfBoundsException {
		Edge e = getEdge(id);
		// ensure the edge is oriented as requested
		if (e != null && e.getStrand() != strand) {
			e.reverseComplement();
		}
		return e;
	}

	/**
	 * Retrieves the edge with the corresponding id.
	 *
	 * @param id
	 * @return
	 * @throws IndexOutOfBoundsException
	 */
	public Edge getEdge(int id) throws IndexOutOfBoundsException {
		return edges.get(id);
	}


	public boolean hasPeContig(String peLabel) {
		ContigLabel l = new ContigLabel(peLabel);
                if (peMemberLabels == null || peMemberLabels.isEmpty()){
                    return false;
                }
		return peMemberLabels.containsKey(l.getId());
	}


	public List<Object> getPairedEndContigMembers(ContigLabel peLabel) {
		if (peMemberLabels == null) { return null; }
		Object[] mLabels;
		try {
			mLabels = peMemberLabels.get(peLabel.getId());
		} catch (NullPointerException e) {
			throw new IllegalArgumentException("No paired-end contig with id " + peLabel.getId());
		}
		List<Object> members = new ArrayList<Object>(mLabels.length);
		if (peLabel.getStrand() == 0) {
                    members.addAll(Arrays.asList(mLabels));
		} else {
			// return reverse order and opposite strands
			for (int i=mLabels.length-1; i>-1; i--) {
                            Object l = mLabels[i];
                            if(l instanceof ContigLabel){
                                ContigLabel lCL = (ContigLabel) l;
				ContigLabel lr = new ContigLabel(lCL.id, lCL.getRevCompStrand());
                                members.add(lr);
                            }
                            else{
                                members.add(l);
                            }
			}
		}
		return members;
	}

        public Object[] getSEMembers(ContigLabel peLabel){
		if (peMemberLabels == null) { return null; }
                return peMemberLabels.get(peLabel.getId());
        }

	public void setPairedEndContig(int peId, List<Object> members) throws IllegalArgumentException {

            if(pairedEndAssembly){
		if (peMemberLabels == null) {
			peMemberLabels = new HashMap<Integer,Object[]>();
		}

                Object[] mLabels = new Object[members.size()];
                peMemberLabels.put(peId, members.toArray(mLabels));
            }
            else {
                // ensure no id clash with single-end contigs
		if (edges.containsKey(peId)) {
//                    System.out.println("Single-end/Paired-end contig id clash: " + peId);                                        
			throw new IllegalArgumentException("Single-end/Paired-end contig id clash: " + peId);
		}
		if (peMemberLabels == null) {
			peMemberLabels = new HashMap<Integer,Object[]>();
		}
		// store this peId within each member edge and collect an array of members for storage
		Object m; Edge mEdge;
		Object[] mLabels = new Object[members.size()]; // array form
		for (int i=0; i<members.size(); i++) {

			m = members.get(i);
                        if(m instanceof ContigLabel){
                            ContigLabel mCL = (ContigLabel)m;
                            mEdge = getEdge(mCL.getId(), mCL.getStrand());
                            if (mEdge == null) {
//                                System.out.println("Unknown single-end contig: " + mCL.getId());                                
                                    throw new IllegalArgumentException("Unknown single-end contig (" + mCL + ")");                                
                            }
                            else{
                                // store pe orientation relative to mEdge's positive strand
                                if (mEdge.getStrand() == Edge.PLUS) {
                                        mEdge.addPairedEndId(new ContigLabel(peId,ContigLabel.PLUS));
                                } else {
                                        // store rc of pe
                                        mEdge.addPairedEndId(new ContigLabel(peId,ContigLabel.MINUS));
                                }
                            }
                        }
			mLabels[i] = m;
		}
		// override any existing values for this peId
		peMemberLabels.put(peId, mLabels);
            }
	}

    public DirectedSparseMultigraph<Vertex,Edge> getQuerySubgraph(Edge e, int maxEdges) {
        byte strand = e.getStrand();
    	DirectedSparseMultigraph<Vertex,Edge> subGraph = new DirectedSparseMultigraph<Vertex,Edge>();
//        {
//            @Override
//            public Collection<Edge> findEdgeSet(Vertex v1, Vertex v2) {
//                Collection<Edge> set1 = super.findEdgeSet(v1, v2);
//                Collection<Edge> set2 = super.findEdgeSet(v2, v1);
//                if(set1 != null){
//                    set1.addAll(set2);
//                }
//                else{
//                    return set2;
//                }
//
//                return set1;
//            }
//        };
    	Queue<Edge> queue = new LinkedList<Edge>();
    	Map<Integer,Integer> inQueue = new TreeMap<Integer,Integer>();
    	queue.offer(e);
    	inQueue.put(e.getId(), 1);

    	int totalEdges = 0;
    	while (queue.peek() != null) {
    		Edge cEdge = queue.poll();
    		subGraph.addEdge(cEdge, cEdge.getSourceVertex(), cEdge.getDestVertex(), EdgeType.DIRECTED);
    		totalEdges++;
    		if (totalEdges > maxEdges) { break; }

    		// add this neighbors to the queue
    		Collection<Edge> incident = getIncidentEdges(cEdge);
    		for (Edge iEdge: incident) {
    			if (iEdge.getId() == cEdge.getId()) { continue; } // skip self
    			if (inQueue.containsKey(iEdge.getId())) { continue; }
    			inQueue.put(iEdge.getId(), 1);
    			queue.offer(iEdge);
    		}
    	}

        if(strand != e.getStrand()){
            e.reverseComplement();
        }

    	return subGraph;
    }

    public DirectedSparseMultigraph<Vertex,Edge> getQuerySubgraphBySteps(Edge e, int steps, boolean trueNeighborsOnly) {
        byte strand = e.getStrand();
        DirectedSparseMultigraph<Vertex,Edge> subGraph = new DirectedSparseMultigraph<Vertex,Edge>();

        HashSet<Edge> set = null;
        if(trueNeighborsOnly){
            set = getTrueNeighborEdges(e, steps);
        }
        else{
            set = getAllNeighborEdges(e, steps);
        }
        set.add(e);
        if(strand != e.getStrand()){
            e.reverseComplement();
        }

        for(Edge cEdge : set){
            subGraph.addEdge(cEdge, cEdge.getSourceVertex(), cEdge.getDestVertex(), EdgeType.DIRECTED);
        }

        return subGraph;
    }

    private HashSet<Edge> getTrueNeighborEdges(Edge e, int steps){
        HashSet<String> idsOfContigsAdded = new HashSet<String>();
        idsOfContigsAdded.add(e.getId() + "+");
        idsOfContigsAdded.add(e.getId() + "-");

        return getTrueNeighborEdgesHelper(e, steps, 2, idsOfContigsAdded);
    }

    /**
     * @param side 0 outgoing
     *             1 incoming
     *             2 both
     */
    private HashSet<Edge> getTrueNeighborEdgesHelper(Edge e, int steps, int side, HashSet<String> idsOfContigsAdded){
        idsOfContigsAdded.add(e.getLabel());
        HashSet<Edge> outgoing = new HashSet<Edge>();
        HashSet<Edge> incoming = new HashSet<Edge>();

        if(steps <= 0){
//            outgoing.add(e);
            return outgoing;
        }

        if(side == 1 || side == 2){
            Vertex v1 = e.getSourceVertex();
            Byte pole = v1.getOutgoingPole(e);
            HashSet<Edge> set = v1.getTrueNeighborEdges(e);
            for(Edge edge : set){
                if(edge.getDestVertex() == edge.getSourceVertex()){ // a self loop
                    if(v1.getOutgoingPole(edge) != pole){
                        edge.reverseComplement();
                    }
                }
                else if(!edge.getDestVertex().equals(v1)){
                    edge.reverseComplement();
                }
            }
            set.remove(e);
            incoming.addAll(set);
        }

        if(side == 0 || side == 2){
            Vertex v0 = e.getDestVertex();
            Byte pole = v0.getIncomingPole(e);
            HashSet<Edge> set = v0.getTrueNeighborEdges(e);
            for(Edge edge : set){
                if(edge.getDestVertex() == edge.getSourceVertex()){ // a self loop
                    if(v0.getIncomingPole(edge) != pole){
                        edge.reverseComplement();
                    }
                }
                else if(!edge.getSourceVertex().equals(v0)){
                    edge.reverseComplement();
                }
            }
            set.remove(e);
            outgoing.addAll(set);
        }

        steps--;
        if(steps <= 0){
            outgoing.addAll(incoming);
//            outgoing.add(e);
            return outgoing;
        }

        HashSet<Edge> neighborEdges = new HashSet<Edge>();

        HashSet<Edge> redundantOutEdges = new HashSet<Edge>();
        HashSet<Edge> redundantInEdges = new HashSet<Edge>();
        for(Edge edge : outgoing){
            String thisLabel = edge.getLabel();
            if(idsOfContigsAdded.contains(thisLabel)){
                redundantOutEdges.add(edge);
            }
            else{
                idsOfContigsAdded.add(thisLabel);
            }
        }
        for(Edge edge : incoming){
            String thisLabel = edge.getLabel();
            if(idsOfContigsAdded.contains(thisLabel)){
                redundantInEdges.add(edge);
            }
            else{
                idsOfContigsAdded.add(thisLabel);
            }
        }
        for(Edge edge : redundantOutEdges){
            outgoing.remove(edge);
        }
        redundantOutEdges.clear();
        for(Edge edge : redundantInEdges){
            incoming.remove(edge);
        }
        redundantInEdges.clear();

        for(Edge edge : outgoing){
            if(edge.getDestVertex() != edge.getSourceVertex()){ //not a self loop
                HashSet<Edge> s = getTrueNeighborEdgesHelper(edge, steps, 0, idsOfContigsAdded);
                for(Edge thisEdge : s){
                    String thisLabel = thisEdge.getLabel();
                    idsOfContigsAdded.add(thisLabel);
                }
                neighborEdges.addAll(s);
            }
        }

        for(Edge edge : incoming){
            if(edge.getDestVertex() != edge.getSourceVertex()){ //not a self loop
                HashSet<Edge> s = getTrueNeighborEdgesHelper(edge, steps, 1, idsOfContigsAdded);
                for(Edge thisEdge : s){
                    String thisLabel = thisEdge.getLabel();
                    idsOfContigsAdded.add(thisLabel);
                }
                neighborEdges.addAll(s);
            }
        }

        outgoing.addAll(incoming);
        outgoing.addAll(neighborEdges);
//        outgoing.add(e);

        return outgoing;
    }

    private HashSet<Edge> getAllNeighborEdges(Edge e, int steps){
        HashSet<String> idsOfContigsAdded = new HashSet<String>();
        idsOfContigsAdded.add(e.getId() + "+");
        idsOfContigsAdded.add(e.getId() + "-");

        return getAllNeighborEdgesHelper(e, steps, 2, idsOfContigsAdded);
    }

    /**
     * @param side 0 outgoing
     *             1 incoming
     *             2 both
     */
    private HashSet<Edge> getAllNeighborEdgesHelper(Edge e, int steps, int side, HashSet<String> idsOfContigsAdded){
        idsOfContigsAdded.add(e.getLabel());
        HashSet<Edge> outgoing = new HashSet<Edge>();
        HashSet<Edge> incoming = new HashSet<Edge>();

        if(steps <= 0){
//            outgoing.add(e);
            return outgoing;
        }

        if(side == 1 || side == 2){
            Vertex v1 = e.getSourceVertex();
            Byte pole = v1.getOutgoingPole(e);
            incoming.addAll(v1.incoming.values());
            incoming.addAll(v1.outgoing.values());
            incoming.remove(e);
            for(Edge edge : incoming){
                if(edge.getDestVertex() == edge.getSourceVertex()){ // a self loop
                    if(v1.getOutgoingPole(edge) != pole){
                        edge.reverseComplement();
                    }
                }
                else if(!edge.getDestVertex().equals(v1)){
                    edge.reverseComplement();
                }
            }
        }

        if(side == 0 || side == 2){
            Vertex v0 = e.getDestVertex();
            Byte pole = v0.getIncomingPole(e);
            outgoing.addAll(v0.incoming.values());
            outgoing.addAll(v0.outgoing.values());
            outgoing.remove(e);
            for(Edge edge : outgoing){
                if(edge.getDestVertex() == edge.getSourceVertex()){ // a self loop
                    if(v0.getIncomingPole(edge) != pole){
                        edge.reverseComplement();
                    }
                }
                else if(!edge.getSourceVertex().equals(v0)){
                    edge.reverseComplement();
                }
            }
        }

        steps--;
        if(steps <= 0){
            outgoing.addAll(incoming);
//            outgoing.add(e);
            return outgoing;
        }

        HashSet<Edge> neighborEdges = new HashSet<Edge>();
        HashSet<Edge> redundantOutEdges = new HashSet<Edge>();
        HashSet<Edge> redundantInEdges = new HashSet<Edge>();

        for(Edge edge : outgoing){
            String thisLabel = edge.getLabel();
            if(idsOfContigsAdded.contains(thisLabel)){
                redundantOutEdges.add(edge);
            }
            else{
                idsOfContigsAdded.add(thisLabel);
            }
        }
        for(Edge edge : incoming){
            String thisLabel = edge.getLabel();
            if(idsOfContigsAdded.contains(thisLabel)){
                redundantInEdges.add(edge);
            }
            else{
                idsOfContigsAdded.add(thisLabel);
            }
        }
        for(Edge edge : redundantOutEdges){
            outgoing.remove(edge);
        }
        redundantOutEdges.clear();
        for(Edge edge : redundantInEdges){
            incoming.remove(edge);
        }
        redundantInEdges.clear();


        for(Edge edge : outgoing){
            if(edge.getDestVertex() != edge.getSourceVertex()){ //not a self loop
                HashSet<Edge> s = getAllNeighborEdgesHelper(edge, steps, 0, idsOfContigsAdded);
                for(Edge thisEdge : s){
                    String thisLabel = thisEdge.getLabel();
                    idsOfContigsAdded.add(thisLabel);
                }
                neighborEdges.addAll(s);
            }
        }

        for(Edge edge : incoming){
            if(edge.getDestVertex() != edge.getSourceVertex()){ //not a self loop
                HashSet<Edge> s = getAllNeighborEdgesHelper(edge, steps, 1, idsOfContigsAdded);
                for(Edge thisEdge : s){
                    String thisLabel = thisEdge.getLabel();
                    idsOfContigsAdded.add(thisLabel);
                }
                neighborEdges.addAll(s);
            }
        }

        outgoing.addAll(incoming);
        outgoing.addAll(neighborEdges);
//        outgoing.add(e);

        return outgoing;
    }

    public boolean isPairedEndAssembly(){
        return pairedEndAssembly;
    }

    // All graphs initialized with the DotParser are paired-end assemblies
    public void setPairedEndAssembly(boolean is){
        pairedEndAssembly = is;
    }

    public void parsedActions(Integer thresholdDistance){
        for(Vertex v : vertices.values()){
            if(v != null){
                v.findAberrantOverlaps(thresholdDistance);
                v.findInferredOverlaps();
                v.clear();
            }
        }
    }

    public DirectedSparseMultigraph<Vertex,Edge> getShortestPathOnlySubgraph(Edge e1, Edge e2) throws Exception {
        DijkstraShortestPath<Vertex,Edge> dsp = new DijkstraShortestPath<Vertex,Edge>(this);
        List<Edge> edgesInPath = dsp.getPath(e1.getSourceVertex(), e2.getDestVertex());

        if(edgesInPath == null || edgesInPath.isEmpty()){
            throw new Exception("Could not find a path between contigs \"" + e1.getLabel() + "\" and \"" + e2.getLabel());
        }

        DirectedSparseMultigraph<Vertex,Edge> subGraph = new DirectedSparseMultigraph<Vertex,Edge>();

        if(!edgesInPath.contains(e1)){
            subGraph.addEdge(e1, e1.getSourceVertex(), e1.getDestVertex(), EdgeType.DIRECTED);
        }

        for(Edge e : edgesInPath){
            subGraph.addEdge(e, e.getSourceVertex(), e.getDestVertex(), EdgeType.DIRECTED);
        }

        if(!edgesInPath.contains(e2)){
            subGraph.addEdge(e2, e2.getSourceVertex(), e2.getDestVertex(), EdgeType.DIRECTED);
        }

    	return subGraph;
    }

    public ShortestPathNeighborhood getShortestPathWithNeighborhoodSubgraph(Edge e1, Edge e2, int steps, boolean trueNeighborsOnly) throws Exception {

        //find the shortest path between the 2 edges
        DijkstraShortestPath<Vertex,Edge> dsp = new DijkstraShortestPath<Vertex,Edge>(this);
        List<Edge> edgesInPath = dsp.getPath(e1.getDestVertex(), e2.getSourceVertex());

        //check whether a path exists
        if(edgesInPath == null || edgesInPath.isEmpty()){
            throw new Exception("Could not find a path between contigs \"" + e1.getLabel() + "\" and \"" + e2.getLabel());
        }

        if(!edgesInPath.contains(e1)){
            if(edgesInPath.get(0).isIncident(e1)){
                edgesInPath.add(0, e1);
            }
            else{
                edgesInPath.add(e1);
            }
        }
        if(!edgesInPath.contains(e2)){
            if(edgesInPath.get(0).isIncident(e2)){
                edgesInPath.add(0, e2);
            }
            else{
                edgesInPath.add(e2);
            }
        }

        //find the neighborhood of edge 1
        byte strand1 = e1.getStrand();
        HashSet<Edge> set = null;
        if(trueNeighborsOnly){
            set = getTrueNeighborEdges(e1, steps);
        }
        else{
            set = getAllNeighborEdges(e1, steps);
        }
        set.add(e1);

        //find the neighborhood of edge 2
        byte strand2 = e2.getStrand();
        HashSet<Edge> set2 = null;
        if(trueNeighborsOnly){
            set2 = getTrueNeighborEdges(e2, steps);
        }
        else{
            set2 = getAllNeighborEdges(e2, steps);
        }
        set.add(e2);

        // combine the 2 neighborhoods; removes duplicates at the same time
        set.addAll(set2);

        // combine with the path; removes duplicates at the same time
        set.addAll(edgesInPath);

        DirectedSparseMultigraph<Vertex,Edge> subGraph = new DirectedSparseMultigraph<Vertex,Edge>();

        for(Edge e : set){
            subGraph.addEdge(e, e.getSourceVertex(), e.getDestVertex(), EdgeType.DIRECTED);
        }

        if(strand1 != e1.getStrand()){
            e1.reverseComplement();
        }

        if(strand2 != e2.getStrand()){
            e2.reverseComplement();
        }

        ArrayList<Integer> pathIds = new ArrayList<Integer>(edgesInPath.size());
        for(Edge e : edgesInPath){
            pathIds.add(e.getId());
        }

        return new ShortestPathNeighborhood(pathIds, subGraph);
    }

    @Override
    public EdgeType getDefaultEdgeType() {
        return EdgeType.DIRECTED;
    }

    public class ShortestPathNeighborhood{
        public Graph<Vertex, Edge> graph;
        public Collection<Integer> path;

        public ShortestPathNeighborhood(Collection<Integer> path, Graph<Vertex, Edge> graph){
            this.path = path;
            this.graph = graph;
        }
    }
}
