package ca.bcgsc.abyssexplorer.visualization.picking;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeListener;

import ca.bcgsc.abyssexplorer.graph.ContigLabel;
import ca.bcgsc.abyssexplorer.graph.Edge;

import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import edu.uci.ics.jung.visualization.util.DefaultChangeEventSupport;

/**
 * Stores the state of paired-end contigs for
 * the currently selected single-end contig.
 * Only supports selection of one paired-end
 * contig at a time. 
 * 
 * @author Cydney Nielsen
 *
 */
public class PairedEndPathState implements ChangeEventSupport {
	
	/**
	 * Indicates whether this state should influence the graph renderer
	 */
	protected boolean displayActive = true;
	
	/**
	 * paired-end contig label
	 */
	protected ContigLabel selectedPeLabel;
	
	/**
	 * single-end contig members 
	 */
//	protected List<Edge> selectedMembers;
        protected List<Object> selectedMembers;
	
	/**
	 * possible paired-end contig labels
	 */
	protected List<ContigLabel> possiblePeLabels;
	
	/**
	 * Defines how to communicate with registered listeners
	 */
    protected ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);
	
	public int getNumSelectedMembers() {
		if (selectedMembers == null) {
			return 0;
		} else {
			return selectedMembers.size();
		}
	}
	
	/**
	 * Returns the position of the first occurrence
	 * of the input edge in the currently selected
	 * path.
	 * 
	 * @param e
	 * @return
	 */
	public int getPosition(ContigLabel e) {
		if (selectedMembers == null) {
			throw new IndexOutOfBoundsException("Cannot get index position of edge " + e.getLabel() + ". No selected members");
		}
		int index = 0;
		for (int i=0; i<selectedMembers.size(); i++) {
			// Edge mEdge = members.get(i);
			if (selectedMembers.get(i) instanceof ContigLabel && selectedMembers.get(i).equals(e)) {
				index = i;
				break;
			}
		}
		return index;
	}
	
	public List<ContigLabel> getPossibleLabels() {
		return possiblePeLabels;
	}
	
	public ContigLabel getSelectedLabel() {
		return selectedPeLabel;
	}
	
	public List<Object> getSelectedMembers() {
		return selectedMembers;
	}
	
	protected void pick(Object e) {
		if (selectedMembers == null) {
			selectedMembers = new ArrayList<Object>();
		}
		selectedMembers.add(e);
	}
	
	protected void pick(List<Object> edges) {
		if (selectedMembers == null) {
			selectedMembers = new ArrayList<Object>(edges.size());
		}
		for (Object e: edges) {
			pick(e);
		}
	}
	
	public void clear() {
            selectedPeLabel = null;
		if (selectedMembers != null) {
			selectedMembers.clear();
		}
	}
	
	public boolean isDisplayActive() {
		return displayActive;
	}
	
	public boolean isPicked(ContigLabel e) {
		if (selectedMembers == null) {
			return false;
		} else {
			return selectedMembers.contains(e);
		}
	}
	
	public void setDisplayActive(boolean b) {
		displayActive = b;
	}
	
	public void setPossibleLabels(ContigLabel p) {
		if (possiblePeLabels == null) {
			possiblePeLabels = new ArrayList<ContigLabel>(1);
		} else {
			possiblePeLabels.clear();
		}
		possiblePeLabels.add(p);
	}
	
	public void setPossibleLabels(List<ContigLabel> p) {
		possiblePeLabels = p;
	}
	
	public void setSelection(ContigLabel l, List<Object> m) {
            clear();
            selectedPeLabel = l;
            pick(m);
            fireStateChanged();
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
