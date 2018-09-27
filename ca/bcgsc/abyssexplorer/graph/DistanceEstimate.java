package ca.bcgsc.abyssexplorer.graph;

/**
 * Stores information for a paired-end distance estimate.
 * 
 * @author Cydney Nielsen
 * 
 */
public class DistanceEstimate implements Comparable<DistanceEstimate> {
	
	/**
	 * Paired-end partner contig label
	 */
	protected ContigLabel dLabel; 

	/**
	 * Optional distance estimate
	 */
	protected Integer dist;
	
	/**
	 * Optional standard error in the distance estimate
	 */
	protected Float error;
	
	/**
	 * Optional number of supporting pairs
	 */
	protected Integer numPairs;
	
	public DistanceEstimate(ContigLabel l) {
		dLabel = l;
	}
	
	public DistanceEstimate(ContigLabel l, Integer d) {
		dLabel = l;
		dist = d;
	}
	
	public DistanceEstimate(ContigLabel l, Integer d, Float e) {
		dLabel = l;
		dist = d;
		error = e;
	}
	
	public DistanceEstimate(ContigLabel l, Integer d, Float e, Integer n) {
		dLabel = l;
		dist = d;
		error = e;
		numPairs = n;
	}
	
	public Integer getDist() {
		return dist;
	}
	
	public Float getError() {
		return error;
	}
	
	public String getLabel() {
		return dLabel.getLabel();
	}
	
	public ContigLabel getLabelObject() {
		return dLabel;
	}
	
	public Integer getNumPairs() {
		return numPairs;
	}
	
	public String toString() {
		String s = dLabel.getLabel();
		if (dist == null && numPairs == null) { return s; }
		s += "\t[";
		if (dist != null) {
			s += "d = " + dist + " bp";
			if (error != null) {
				s += "; e = " + error + " bp";
			}
			s+= ";";
		}
		if (numPairs != null) {
			s += "\tn = " + numPairs;
		}
		s += "]";
		return s;
	}
	
	/**
	 * Compare based on contig labels
	 */
	public int compareTo(DistanceEstimate other) {
		return (getLabel().compareTo(other.getLabel()));
	}

}
