package ca.bcgsc.abyssexplorer.graph;

/**
 * Class to store a contigs id and strand.
 * Designed to be more efficient than using 
 * a String (e.g. "5+").
 * 
 * @author Cydney Nielsen
 * 
 */
public class ContigLabel implements Comparable<ContigLabel> {

    public final static byte PLUS = 0;
    public final static byte MINUS = 1;
    
	/**
	 * The contig id
	 */
	int id;
	
	/**
	 * The contig strand
	 * (0 = "+", 1 = "-")
	 */
	byte strand;
	
	public ContigLabel(int i, byte s) {
		id = i;
		if (s != PLUS && s != MINUS) {
			throw new IllegalArgumentException("strand must be one of 0 (for '+') or 1 (for '-')");
		}
		strand = s;
	}
	
	public ContigLabel(String s) {
		if (s == null) {
			throw new IllegalArgumentException("Invalid contig label format: " + s);
		}
		if (s.endsWith("+")) {
			id = Integer.parseInt(s.replace("+",""));
			strand = PLUS;
		} else if (s.endsWith("-")){
			id = Integer.parseInt(s.replace("-", ""));
			strand = MINUS;
		} else {
			throw new IllegalArgumentException("Invalid contig label format: '" + s + "'");
		}
	}
	
	public String getLabel() {
		String label = "";
		if (strand == PLUS) {
			label = id + "+";
		} else {
			label = id + "-";
		}
		return label;
	}
	
	public String getRevCompLabel() {
		String rLabel = "";
		if (strand == PLUS) {
			rLabel = id + "-";
		} else {
			rLabel = id + "+";
		}
		return rLabel;
	}
	
	public byte getRevCompStrand() {
		if (strand == PLUS) {
			return MINUS;
		} else {
			return PLUS;
		}
	}
	
	public int getId() {
		return id;
	}
	
	public byte getStrand() {
		return strand;
	}
	
	public void setStrand(byte s) {
		strand = s;
	}
	
	public int compareTo(ContigLabel o) {
		int c = 0;
		if (o.getId() > getId()) {
			c = -1;
		} else if (o.getId() < getId()) {
			c = 1;
		} else if (o.getId() == getId()) {
			if (o.getStrand() == getStrand()) {
				c = 0;
			} else if (o.getStrand() == 1) {
				c = -1;
			} else {
				c = +1;
			}
		}
		return c;
	}
	
	public boolean equals(Object o) {
		if (o instanceof ContigLabel && this.compareTo((ContigLabel) o) == 0) {
			return true;
		} else {
			return false;
		}
	}
}
