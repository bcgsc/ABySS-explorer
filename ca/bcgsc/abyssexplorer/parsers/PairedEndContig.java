package ca.bcgsc.abyssexplorer.parsers;

import java.util.ArrayList;
import java.util.List;

import ca.bcgsc.abyssexplorer.graph.ContigLabel;

/**
 * Class to capture the paired-end (PE) contig information.
 * Member list is ordered relative to this PE contig's
 * positive strand orientation.
 * 
 * @author Cydney Nielsen
 *
 */
public class PairedEndContig {

	protected int id; // no strand; positive only
	protected int coverage;
	protected List<Object> mLabels;
	
	public PairedEndContig(int i) {
		id = i;
		coverage = -1;
		mLabels = new ArrayList<Object>();
	}
	
	public void addMember(String cLabel) {
		int mId;
		if (cLabel.endsWith("+")) {
			mId = Integer.parseInt(cLabel.replace("+", ""));
			mLabels.add(new ContigLabel(mId, (byte) 0));
		} else if (cLabel.endsWith("-")) {
			mId = Integer.parseInt(cLabel.replace("-", ""));
			mLabels.add(new ContigLabel(mId, (byte) 1));
		} else {
			//throw new IllegalArgumentException("Paired-end contig label must end in '+' or '-'");

                    //does not end in +/-, most likely scaffolding ie. "74N"
                    mLabels.add(cLabel);
		}
	}
	
	public int getCoverage() {
		return coverage;
	}
	
	public List<Object> getMembers() {
		return mLabels;
	}
	
	public int getNumMembers() {
		return mLabels.size();
	}
	
	public String getLabel() {
		String label = id + "+";
		return label;
	}
	
	public int getId() {
		return id;
	}
	
	public void setCoverage(int c) {
		coverage = c;
	}
}
