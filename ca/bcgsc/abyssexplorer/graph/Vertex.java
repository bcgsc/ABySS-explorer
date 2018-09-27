package ca.bcgsc.abyssexplorer.graph;

import edu.uci.ics.jung.graph.Graph;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

/**
 * Class to capture a k-mer overlap vertex.
 *
 * @author Cydney Nielsen
 *
 */
public class Vertex {
    
	protected int id;
	// list of in and out edges
	// NOTE: strand in Edge can change, so store relative to positive strand
//	protected List<Edge> incoming;
//	protected List<Edge> outgoing;
        
        protected HashMap<Integer, Edge> incoming;
        protected HashMap<Integer, Edge> outgoing;
        
//	// need two lists - edge can connect to both poles (inverted repeats)
//	protected List<Byte> incomingPoles;
//	protected List<Byte> outgoingPoles;
        protected HashMap<Integer, Byte> incomingPoles;
	protected HashMap<Integer, Byte> outgoingPoles;
                
	protected boolean isPalindrome;

        protected HashMap<String, Integer> distanceMap;
        protected HashSet<String> inferredOverlaps;
        protected HashSet<String> aberrantOverlaps;

	public Vertex(int id) {
		this.id = id;
//		incoming = new LinkedList<Edge>();
//		outgoing = new LinkedList<Edge>();
                incoming = new HashMap<Integer, Edge>();
                outgoing = new HashMap<Integer, Edge>();
                
//		incomingPoles = new LinkedList<Byte>();
//		outgoingPoles = new LinkedList<Byte>();
                incomingPoles = new HashMap<Integer, Byte>();
                outgoingPoles = new HashMap<Integer, Byte>();
		isPalindrome = false;
	}

	public int getId() {
		return id;
	}

	public void setIsPalindrome(boolean b) {
		if (b==true) {
//			// System.out.println("\nv: " + id + " is palindrome");
//			// collapse poles for the palindrome
//			for (int i=0; i<incomingPoles.size(); i++) {
//				// System.out.println("incoming: " + incoming.get(i).getLabel());
//				if (incomingPoles.get(i) == 1) {
//					incomingPoles.set(i, (byte) 0);
//				}
//			}
//			for (int i=0; i<outgoingPoles.size(); i++) {
//				// System.out.println("outcoming: " + outgoing.get(i).getLabel());
//				if (outgoingPoles.get(i) == 1) {
//					outgoingPoles.set(i, (byte) 0);
//				}
//			}
                    HashMap<Integer, Byte> newInPoles = new HashMap<Integer, Byte>();
                    HashMap<Integer, Byte> newOutPoles = new HashMap<Integer, Byte>();
                    for(Entry<Integer, Byte> ent : incomingPoles.entrySet()){
                        if(ent.getValue() == 1){
                            newInPoles.put(id, (byte) 0);
                        }
                    }
                    for(Entry<Integer, Byte> ent : outgoingPoles.entrySet()){
                        if(ent.getValue() == 1){
                            newOutPoles.put(id, (byte) 0);
                        }
                    }
                    
                    incomingPoles.putAll(newInPoles);
                    outgoingPoles.putAll(newOutPoles);
		}
		isPalindrome = b;
	}

	public boolean isPalindrome() {
		return isPalindrome;
	}

        public boolean isIncident(Edge e){
            //return outgoing.contains(e) || incoming.contains(e);
            return outgoing.containsValue(e) || incoming.containsValue(e);
        }

        public boolean hasConsistentPoles(Graph<Vertex,Edge> g) {
            if(isPalindrome){
                return true;
            }

            List<Edge> outs = getOutgoing();
            Byte representative = null;

//            int numOuts = 0;
            if(outs.size() > 0){
                for(Edge e : outs){
                    if(g.containsEdge(e)){
//                        numOuts++;
                        if(representative == null){
                            representative = getOutgoingPole(e);
                        }
                        else if(getOutgoingPole(e) != representative.byteValue()){
                            return false;
                        }
                    }
                }
            }
//            int numIns = 0;
            List<Edge> ins = getIncoming();
            if(ins.size() > 0){
                for(Edge e : ins){
                    if(g.containsEdge(e)){
//                        numIns++;
                        if(representative == null){
                            representative = getIncomingPole(e);
                        }
                        else if(getIncomingPole(e) != representative.byteValue()){
                            return false;
                        }
                    }
                }
            }

//            if(numOuts + numIns <= 1){
//                return false;
//            }

            return true;
        }

//	public boolean hasConsistentPoles() {
//
//		// Under development...
//
//		/*boolean cPoles = true;
//		if (incomingPoles.size() == 0 && outgoingPoles.size() == 0) {
//			return true;
//		}
//		byte p;
//		if (incomingPoles.size() != 0) {
//			p = incomingPoles.get(0);
//		} else {
//			p = outgoingPoles.get(0);
//		}
//		// check for pole consistency
//		for (byte b: incomingPoles) {
//			if (b != p) {
//				cPoles = false;
//				break;
//			}
//		}
//		if (cPoles) {
//			for (byte b: outgoingPoles) {
//				if (b != p) {
//					cPoles = false;
//					break;
//				}
//			}
//		}
//		return cPoles;*/
//		return false;
//	}

	/**
	 * Returns currently inbound edges
	 * @return
	 */
	public List<Edge> getIncoming() {
		// if (isPalindrome) {
		// 	return incoming;
		// } else {
			List<Edge> tmpIncoming = new LinkedList<Edge>();
			for (Edge iEdge: incoming.values()) {
				if (iEdge.getStrand() == Edge.PLUS) {
					tmpIncoming.add(iEdge);
				}
			}
			for (Edge oEdge: outgoing.values()) {
				// stored relative to positive strand
				// negative strand indicates must be in inbound orientation
				if (oEdge.getStrand() == Edge.MINUS) {
					tmpIncoming.add(oEdge);
				}
			}
			return tmpIncoming;
		// }
	}

	/**
	 * Returns currently outbound edges
	 * @return
	 */
	public List<Edge> getOutgoing() {
		// if (isPalindrome) {
		// 	return outgoing;
		// } else {
			List<Edge> tmpOutgoing = new LinkedList<Edge>();
			for (Edge oEdge: outgoing.values()) {
				if (oEdge.getStrand() == Edge.PLUS) {
					tmpOutgoing.add(oEdge);
				}
			}
			for (Edge iEdge: incoming.values()) {
				// stored relative to positive strand
				// negative strand indicates must be in outbound orientation
				if (iEdge.getStrand() == Edge.MINUS) {
					tmpOutgoing.add(iEdge);
				}
			}
			return tmpOutgoing;
		// }
	}
        
	public void addIncoming(Edge e, Byte pole) {
		if (e.getStrand() == Edge.PLUS) {
			if (!incoming.containsValue(e)) {
				incoming.put(e.getId(), e);
				if (isPalindrome) {
					// pole is irrelevant
					pole = 0;
				}
//				incomingPoles.add(pole);
                                incomingPoles.put(e.getId(), pole);
			}
		} else {
			// store info for the edge's positive strand orientation
			if (!outgoing.containsValue(e)) {
				outgoing.put(e.getId(), e);
				if (isPalindrome) {
					// pole is irrelevant
//					outgoingPoles.add((byte) 0);
                                    outgoingPoles.put(e.getId(), (byte) 0);
				} else {
					outgoingPoles.put(e.getId(), getFlippedPole(pole));
				}
			}
		}
	}

	public void addOutgoing(Edge e, Byte pole) {
		if (e.getStrand() == Edge.PLUS) {
			if (!outgoing.containsValue(e)) {
				outgoing.put(e.getId(), e);
				if (isPalindrome) {
					// pole is irrelevant
					pole = 0;
				}
				outgoingPoles.put(e.getId(), pole);
			}
		} else {
			// store info for the edge's positive strand orientation
			if (!incoming.containsValue(e)) {
				incoming.put(e.getId(), e);
				if (isPalindrome) {
					incomingPoles.put(e.getId(), (byte) 0);
				} else {
					incomingPoles.put(e.getId(), getFlippedPole(pole));
				}
			}
		}
	}
       
	public byte getIncomingPole(Edge e) {
		return getIncomingPole(e, e.getStrand());
	}
        
        public byte getIncomingPole(Edge e, byte strand){
		byte pole = -1;
		if (strand == Edge.PLUS) {
			if (incoming.containsValue(e)) {
				// e is present in the incoming list
				if (isPalindrome) {
					// guarantee pole=0 if palindrome
					pole = 0;
				} else {
					pole = incomingPoles.get(e.getId());
				}
			}
		} else {
			// want the rc of stored values
			if (outgoing.containsValue(e)) {
				// e is present in the outgoing list
				if (isPalindrome) {
					// guarantee pole=0 if palindrome
					pole = 0;
				} else {
					pole = outgoingPoles.get(e.getId());
					// adjust for opposite direction
                                        pole = getFlippedPole(pole);
				}
			}
		}
		return pole;            
        }        
                
        public byte getOutgoingPole(Edge e){
            return getOutgoingPole(e, e.getStrand());
        }
        
        public byte getOutgoingPole(Edge e, byte strand) {
		byte pole = -1;
		if (strand == Edge.PLUS) {
			if (outgoing.containsValue(e)) {
				// e is present in the outgoing list
				if (isPalindrome) {
					// guarantee pole=0 if palindrome
					pole = 0;
				} else {
					pole = outgoingPoles.get(e.getId());
				}
			}
		} else {
			// want the rc of stored values
			if (incoming.containsValue(e)) {
				// e is present in the incoming list
				if (isPalindrome) {
					// guarantee pole=0 if palindrome
					pole = 0;
				} else {
					pole = incomingPoles.get(e.getId());
					// adjust for opposite direction                                        
                                        pole = getFlippedPole(pole);                                        
				}
			}
		}
		return pole;
	}

	public byte getFlippedPole(byte pole) {
		if (isPalindrome) {
			return 0;
		} else if (pole == 0) {
			return 1;
		} else {
			return 0;
		}
	}

        public void addDistance(String iid, String oid, Integer distance){
            if(distanceMap == null){
                distanceMap = new HashMap<String, Integer>();
            }

            distanceMap.put(iid+"/"+oid, distance);
        }

        public HashMap<String, Integer> getDistanceMappings(){
            return distanceMap;
        }

        public void addDistanceMappings(HashMap<String, Integer> mappings){
            if(distanceMap == null){
                distanceMap = new HashMap<String, Integer>();
            }
            
            distanceMap.putAll(mappings);            
        }

        public void clear(){
            distanceMap = null;
        }

        public void findInferredOverlaps(){
            if(distanceMap != null){
//                List<Edge> in = incoming.values();
//                List<Edge> out = outgoing.values();
                
                if(inferredOverlaps == null){
                    inferredOverlaps = new HashSet<String>();
                }                
                
                for(Edge inEdge : incoming.values()){
                    for(Edge outEdge : outgoing.values()){
                        if(inEdge.equals(outEdge)){
                            continue;
                        }
                        int ieid = inEdge.getId();
                        int oeid = outEdge.getId();

                        String fkey = Integer.toString(ieid) + "/" + Integer.toString(oeid);
                        String rkey = Integer.toString(oeid) + "/" + Integer.toString(ieid);
                        if(!distanceMap.containsKey(fkey) && !distanceMap.containsKey(rkey)){                            
                            if(ieid < oeid){
                                inferredOverlaps.add(fkey);                                
                            }
                            else{
                                inferredOverlaps.add(rkey);                                
                            }
                            //System.out.println("Inferred Overlap in " + key);
                        }
                    }
                }
            }
        }

        public void findAberrantOverlaps(Integer thresholdDistance){
            if(distanceMap != null){                
                if(aberrantOverlaps == null){
                    aberrantOverlaps = new HashSet<String>();
                }

                for(Entry<String, Integer> ent : distanceMap.entrySet()){
                    String key = ent.getKey();
                    Integer d = ent.getValue();
                    //System.out.println("key:" + key + ";d=" + d);

                    if(d != null){
                        String[] ids = key.split("/");

                        String rkey = ids[1]+"/"+ids[0];

                        int id1 = Integer.parseInt(ids[0]);
                        int id2 = Integer.parseInt(ids[1]);
//                        ArrayList<Integer> idsInt = new ArrayList<Integer>();
//                        idsInt.add(id1);
//                        idsInt.add(id2);
//                        ArrayList<Integer> lengths = new ArrayList<Integer>();
//
//                        // get the length of the two edges with id = id1 or id2
//                        for(Edge e : incoming){
//                            int cid = e.getId();
//                            for(Integer i : idsInt){
//                                if(i.intValue() == cid){
//                                    lengths.add(e.getLen());
//                                    //idsInt.remove(i);
//                                    break;
//                                }
//                            }
//
//                            if(lengths.size() == 2){
//                                break;
//                            }
//                        }
//
//                        // if less than 2 lengths are found
//                        if(lengths.size() < 2){
//                            for(Edge e : outgoing){
//                                int cid = e.getId();
//                                for(Integer i : idsInt){
//                                    if(i.intValue() == cid){
//                                        lengths.add(e.getLen());
//                                        //idsInt.remove(i);
//                                        break;
//                                    }
//                                }
//
//                                if(lengths.size() == 2){
//                                    break;
//                                }
//                            }
//                        }
//
//                        if(lengths.size() == 2){
//                            int longerLength = Math.max(lengths.get(0), lengths.get(1));

                        Edge e1 = null;
                        Edge e2 = null;
                        
                        e1 = incoming.get(id1);
                        e2 = incoming.get(id2);
                        
                        if(e1 == null){
                            e1 = outgoing.get(id1);
                        }

                        if(e2 == null){
                            e2 = outgoing.get(id2);
                        }
                        
                        if(e1 != null && e2 != null){
                            int longerLength = Math.max(e1.len, e2.len);
                        
                            // if the distance is more negative than the threshold and the distance is larger than half the length of the longer contig
                            if(d < thresholdDistance && Math.abs(d) > longerLength*0.5){
                                if(!aberrantOverlaps.contains(key) && !aberrantOverlaps.contains(rkey)){
                                    aberrantOverlaps.add(key);
                                    System.out.println("Large Overlap of " + d + " in " + key);
                                }
                            }
                        }
                    }
                }
            }
        }

        public int getNumInferredOverlaps(){
            if(inferredOverlaps == null){
                return -1;
            }

            return inferredOverlaps.size();
        }

        public int getNumAberrantOverlaps(){
            if(aberrantOverlaps == null){
                return -1;
            }

            return aberrantOverlaps.size();
        }


        public String[] getInferredOverlaps(){
            if(inferredOverlaps == null){
                return null;
            }

            return inferredOverlaps.toArray(new String[inferredOverlaps.size()]);
        }

        public String[] getAberrantOverlaps(){
            if(aberrantOverlaps == null){
                return null;
            }

            return aberrantOverlaps.toArray(new String[aberrantOverlaps.size()]);
        }
        
//        public void findAberrantOverlaps(Integer thresholdDistance){
//
//            if(distanceMap != null){
//                Set<String> keys = distanceMap.keySet();
//
//                for(String key : keys){
//                    Integer d = distanceMap.get(key);
//                    if(d != null){
//                        if(d < thresholdDistance){
//                            if(aberrantOverlaps == null){
//                                aberrantOverlaps = new LinkedList<String>();
//                            }
//                            aberrantOverlaps.add(key);
//                            System.out.println("Large Overlap of " + d + " in " + key);
//                        }
//                    }
//                }
//            }
//        }


        public HashSet<Edge> getTrueNeighborEdges2(Edge e){
            HashSet<Edge> set = new HashSet<Edge>();

            byte strand = e.getStrand();
            Byte pole = -1;
            
//            int i = incoming.indexOf(e);
            if(incoming.containsValue(e)){
                pole = incomingPoles.get(e.getId());
                if(strand == Edge.PLUS){
                    List<Edge> list = getOutgoing();
                    for(Edge edge : list){
                        if(pole.equals(getOutgoingPole(edge))){
                            set.add(edge);
                        }
                    }

                    list = getIncoming();
                    for(Edge edge : list){
                        if(pole.equals(getIncomingPole(edge))){
                            set.add(edge);
                        }
                    }
                }
                else{
                    List<Edge> list = getOutgoing();
                    for(Edge edge : list){
                        if(pole.equals(getOutgoingPole(edge))){
                            set.add(edge);
                        }
                    }

                    pole = getFlippedPole(pole);
                    list = getIncoming();
                    for(Edge edge : list){
                        if(pole.equals(getIncomingPole(edge))){
                            set.add(edge);
                        }
                    }
                }
            }
            else if(outgoing.containsValue(e)){
                    pole = outgoingPoles.get(e.getId());
                    if(strand == Edge.PLUS){
                        List<Edge> list = getIncoming();
                        for(Edge edge : list){
                            if(pole.equals(getIncomingPole(edge))){
                                set.add(edge);
                            }
                        }
                    }
                    else{
                        pole = getFlippedPole(pole);
                        List<Edge> list = getOutgoing();
                        for(Edge edge : list){
                            if(pole.equals(getOutgoingPole(edge))){
                                set.add(edge);
                            }
                        }
                    }
            }
            
            return set;
        }


        public HashSet<Edge> getTrueNeighborEdges(Edge e){
            HashSet<Edge> set = new HashSet<Edge>();
            Byte pole = -1;

            if(incoming.containsValue(e)){
                pole = incomingPoles.get(e.getId());
                List<Edge> olist = getOutgoing();
                for(Edge nEdge : olist){
                    if(pole.byteValue() == getOutgoingPole(nEdge)){
                        set.add(nEdge);
                    }
                }

                pole = getFlippedPole(pole);

                List<Edge> ilist = getIncoming();
                for(Edge nEdge : ilist){
                    if(pole.byteValue() == getIncomingPole(nEdge)){
                        set.add(nEdge);
                    }
                }
            }
            else if(outgoing.containsValue(e)){
                    pole = outgoingPoles.get(e.getId());
                    List<Edge> ilist = getIncoming();
                    for(Edge nEdge : ilist){
                        if(pole.byteValue() == getIncomingPole(nEdge)){
                            set.add(nEdge);
                        }
                    }

                    pole = getFlippedPole(pole);

                    List<Edge> olist = getOutgoing();
                    for(Edge nEdge : olist){
                        if(pole.byteValue() == getOutgoingPole(nEdge)){
                            set.add(nEdge);
                        }
                    }
            }

            return set;
        }

}
