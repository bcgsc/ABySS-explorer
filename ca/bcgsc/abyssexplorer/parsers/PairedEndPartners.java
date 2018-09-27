package ca.bcgsc.abyssexplorer.parsers;

/**
 * Class to capture the paired-end (PE) partner contigs
 * of a given single-end (SE) contig.
 * 
 * @author Cydney Nielsen
 *
 */
public class PairedEndPartners {
	
	protected int sId;  // source
	protected byte sStrand;
	protected int dId;  // destination
	protected byte dStrand;
	protected Integer dist; // distance estimate
	protected Float error;      // standard error in the distance estimate
	protected Integer numPairs; // number of pairs supporting this connection

	// Our dot format does not currently store number of pairs or error
	public PairedEndPartners(int s, byte ss, int d, byte ds, int de) {
		init(s, ss, d, ds, de);
	}

	public PairedEndPartners(int s, byte ss, int d, byte ds, int de, int n, float e) {
		init(s, ss, d, ds, de);
		numPairs = n;
		error = e;
	}
	
	private void init(int s, byte ss, int d, byte ds, int de) {
		sId = s;
		sStrand = ss;
		dId = d;
		dStrand = ds;
		dist = de;
	}
	
	public String getSourceLabel() {
		String label = "";
		if (sStrand == 0) {
			label = sId + "+";
		} else {
			label = sId + "-";
		}
		return label;
	}
	
	public String getDestLabel() {
		String label = "";
		if (dStrand == 0) {
			label = dId + "+";
		} else {
			label = dId + "-";
		}
		return label;
	}
	
	public Integer getDis() {
		return dist;
	}
	
	public Float getError() {
		return error;
	}
	
	public Integer getNumPairs() {
		return numPairs;
	}
	
	public String toString() {
		return (getSourceLabel() + " " + getDestLabel() + " " + dist);
	}

}
