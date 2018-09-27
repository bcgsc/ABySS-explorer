package ca.bcgsc.abyssexplorer.visualization.picking;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.ChangeListener;

import ca.bcgsc.abyssexplorer.graph.DistanceEstimate;
import ca.bcgsc.abyssexplorer.graph.Edge;

import edu.uci.ics.jung.visualization.util.ChangeEventSupport;
import edu.uci.ics.jung.visualization.util.DefaultChangeEventSupport;

/**
 * Maintains the state of paired end partners edges for the
 * currently 'picked' edge in the graph.
 * 
 * @author Cydney Nielsen
 *
 */
public class PartnerEdgeState implements ChangeEventSupport {

	/**
	 * Indicates whether this state should influence the graph renderer
	 */
	protected boolean displayActive;
	
	/**
	 * The inbound paired end partner edges
	 */
	protected List<DistanceEstimate> inbound;
	
	/**
	 * The outbound paired end partner edges
	 */
	protected List<DistanceEstimate> outbound;
	
	/**
	 * The currently highlighted inbound paired end partner edge
	 */
	protected Edge inHighlighted;
	
	/**
	 * The currently highlighted outbound paired end partner edge
	 */
	protected Edge outHighlighted;
	
	/**
	 * Defines how to communicate with registered listeners
	 */
    protected ChangeEventSupport changeSupport =
        new DefaultChangeEventSupport(this);
	
	public PartnerEdgeState() {
		displayActive = true;
	}
	
	public Edge getInHighlighted() {
		return inHighlighted;
	}
	
	public Edge getOutHighlighted() {
		return outHighlighted;
	}
	
	public List<DistanceEstimate> getInbound() {
		return inbound;
	}
	
	public List<DistanceEstimate> getOutbound() {
		return outbound;
	}
	
	public boolean isDisplayActive() {
		return displayActive;
	}
	
	public void pick(List<DistanceEstimate> dists, String direction) {
		if (dists.size() == 0) { return; }
		for (DistanceEstimate d: dists) {
			pick(d, direction);
		}
		fireStateChanged();
	}
	
	public void pick(DistanceEstimate d, String direction) {
		if (direction.equals("in")) {
			if (inbound == null) {
				inbound = new ArrayList<DistanceEstimate>();
			}
			inbound.add(d);
		} else if (direction.equals("out")) {
			if (outbound == null) {
				outbound = new ArrayList<DistanceEstimate>();
			}
			outbound.add(d);
		} else {
			throw new IllegalArgumentException("edge direction must be 'in' or 'out'");
		}
	}
	
	public void clear() {
		if (inbound != null) { inbound.clear(); }
		if (outbound != null) { outbound.clear(); }
		inHighlighted = null;
		outHighlighted = null;
	}
	
	public boolean isHighlighted(Edge e) {
		boolean h = false;
		if (inHighlighted != null) {
			h = inHighlighted.getLabel().equals(e.getLabel());
		} else if (outHighlighted != null) {
			h = outHighlighted.getLabel().equals(e.getLabel());
		}
		return h;
	}
	
	public boolean isInbound(Edge e) {
		if (inbound == null) { return false; }
		for (DistanceEstimate d: inbound) {
			if (d.getLabelObject().equals(e.getLabelObject())) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isOutbound(Edge e) {
		if (outbound == null) { return false; }
		for (DistanceEstimate d: outbound) {
			if (d.getLabelObject().equals(e.getLabelObject())) {
				return true;
			}
		}
		return false;
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

	public void setDisplayActive(boolean b) {
		displayActive = b;
	}
	
	public void setInHighlighted(Edge e) {
		inHighlighted = e;
	}
	
	public void setOutHighlighted(Edge e) {
		outHighlighted = e;
	}
	
}
