package ca.bcgsc.abyssexplorer.parsers;

import java.util.ArrayList;
import java.util.List;

/**
 * Class to hold a contig's adjacent contigs.
 * Inbound and outbound contig neighbours 
 * are defined relative to this contig's 
 * positive strand orientation. 
 * 
 * @author Cydney Nielsen
 *
 */
public class AdjacentContigs {

	protected int id; 
	protected byte strand; // '+' = 0; '-' = 1
	
	// optional data
	Integer len;
	Float cov;
        Integer distance;
	protected List<String> inbound;
	protected List<String> outbound;
	
	public AdjacentContigs(String l) {
		setLabel(l);
	}
	
	public AdjacentContigs(String l, int len) {
		setLabel(l);
		this.len = len;
	}
	
	public AdjacentContigs(String l, int len, float cov) {
		setLabel(l);
		this.len = len;
		this.cov = cov;
	}
	
	protected void setLabel(String l) {
		if (!l.endsWith("+") && !l.endsWith("-")) {
			throw new IllegalArgumentException("Contig label must end with '+' or '-': " + l);
		}
		if (l.endsWith("+")) {
			id = Integer.parseInt(l.replace("+", ""));
			strand = 0;
		} else {
			id = Integer.parseInt(l.replace("-", ""));
			strand = 1;
		}
	}
	
	public void addInbound(List<String> iLabels) {
		for (String iLabel: iLabels) {
			addInbound(iLabel);
		}
	}
	
	public void addInbound(String iLabel) {
		if (!iLabel.endsWith("+") && !iLabel.endsWith("-")) {
			throw new IllegalArgumentException("Contig label must end with '+' or '-': '" + iLabel + "'");
		}
		if (strand == 0) {
			if (inbound == null) {
				inbound = new ArrayList<String>();
			}
			inbound.add(iLabel);
		} else {
			if (outbound == null) {
				outbound = new ArrayList<String>();
			}
			outbound.add(iLabel);//flipStrand(iLabel, "-"));
		}
	}
	
	public void addOutbound(List<String> oLabels) {
		for (String oLabel: oLabels) {
			addOutbound(oLabel);
		}
	}
	
	public void addOutbound(String oLabel) {
		if (!oLabel.endsWith("+") && !oLabel.endsWith("-")) {
			throw new IllegalArgumentException("Contig label must end with '+' or '-': " + oLabel);
		}
		if (strand == 0) {
			if (outbound == null) {
				outbound = new ArrayList<String>();
			}
			outbound.add(oLabel);
		} else {
			if (inbound == null) {
				inbound = new ArrayList<String>();
			}
			inbound.add(oLabel);//flipStrand(oLabel, "-"));
		}
	}
	
	protected String flipStrand(String l, String s) {
		if (s.equals("+")) {
			return l.replace("+","") + "-";
		} else {
			return l.replace("-","") + "+";
		}
	}

	public Float getCov() {
		return cov;
	}
	
	public String getLabel() {
		String label = "";
		if (strand == 0) {
			label = id + "+";
		} else {
			label = id + "-";
		}
		return label;
	}
	
	public Integer getLen() {
		return len;
	}
	
	public List<String> getInbound() {
		if (strand == 0) {
			return inbound;
		} else {
			return outbound;
		}
	}
	
	public List<String> getOutbound() {
		if (strand == 0) {
			return outbound;
		} else {
			return inbound;
		}
	}
	
	public boolean hasInbound() {
		if (strand == 0) {
                    if (inbound == null || inbound.size() == 0) {
                            return false;
                    }
                }
                else{
                    if (outbound == null || outbound.size() == 0) {
                            return false;
                    }
                }
                return true;
	}
	
	public boolean hasOutbound() {
		if (strand == 0) {
                    if (outbound == null || outbound.size() == 0) {
                            return false;
                    }
                }
                else{
                    if (inbound == null || inbound.size() == 0) {
                            return false;
                    }
                }
                return true;
	}

        public void setDistance(Integer d){
            distance = d;
        }

        public Integer getDistance(){
            return distance;
        }
	
}
