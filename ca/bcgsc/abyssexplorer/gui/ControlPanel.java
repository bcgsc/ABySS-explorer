package ca.bcgsc.abyssexplorer.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Hashtable;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeListener;

import ca.bcgsc.abyssexplorer.graph.ContigLabel;

/**
 * The ABySS-Explorer control panel class.
 * 
 * @author Cydney Nielsen
 *
 */
public class ControlPanel {
	
	protected JPanel panel;
	
	protected JTextField searchField;	
	protected JSlider graphSizeSlider;
	protected JCheckBox labelsToggle;
	protected JCheckBox lenToggle;
	protected JSlider lenSlider;
	
	// protected JCheckBox covToggle;
	// protected JCheckBox layoutToggle;
	
	protected JCheckBox pePartnerToggle;
	protected JScrollPane peInPartnerListPane;
	protected JLabel inPartnerLabel;
	protected DefaultListModel peInPartnerListModel;
	protected JList peInPartnerList;
	protected JScrollPane peOutPartnerListPane;
	protected JLabel outPartnerLabel;
	protected DefaultListModel peOutPartnerListModel;
	protected JList peOutPartnerList;
	
	protected JCheckBox peListToggle;
	protected JScrollPane peListPane;
	protected JLabel peListLabel;
	protected DefaultListModel peListModel;
	protected JList peList;
	
	public ControlPanel() {
		
		panel = new JPanel();
		GridBagLayout gridbag = new GridBagLayout();
		panel.setLayout(gridbag);
		
		initializeLists();
		
		int row = 0;
		//row = addSearchComponents(row);
		// row = addGraphSizeComponents(row);
		row = addEdgeLenComponents(row);
		row = addLabelComponents(row);
		row = addPeListComponents(row);
		row = addPePartnerListComponents(row);
		
		panel.setBorder(
        BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(""),
                        BorderFactory.createEmptyBorder(0,0,0,0)));
		
		disableComponents();
	}
	
	public void addSearchListener(ActionListener l) {
		searchField.addActionListener(l);
	}
	
	public void addSearchMouseListener(MouseListener l) {
		searchField.addMouseListener(l);
	}
	
	public void addPeListListener(MouseListener l) {
		peList.addMouseListener(l);
	}
	
	public void addLabelToggleListener(ItemListener l) {
		labelsToggle.addItemListener(l);
	}
	
	public void addLenToggleListener(ItemListener l) {
		lenToggle.addItemListener(l);
	}
	
	public void addLenSliderListener(ChangeListener c) {
		lenSlider.addChangeListener(c);
	}

	//NOTE: MouseAdaptor implements BOTH MouseListener and MouseMotionListener in Java 1.6
	public void addInPartnerListener(MouseListener l) {
		peInPartnerList.addMouseListener(l);
	}

	public void addInPartnerMotionListener(MouseMotionListener l) {
		peInPartnerList.addMouseMotionListener(l);
	}

	public void addOutPartnerListener(MouseListener l) {
		peOutPartnerList.addMouseListener(l);
	}

	public void addOutPartnerMotionListener(MouseMotionListener l) {
		peOutPartnerList.addMouseMotionListener(l);
	}
	
	public void addPartnerToggleListener(ItemListener l) {
		pePartnerToggle.addItemListener(l);
	}
	
	public void addPeListToggleListener(ItemListener l) {
		peListToggle.addItemListener(l);
	}
	
	protected int addSearchComponents(int row) {
		searchField = new JTextField(15);
		readyForSearch();
		GridBagConstraints cSearch = new GridBagConstraints();
		cSearch.gridwidth = 4; cSearch.gridheight = 1;
		cSearch.gridx = 0; cSearch.gridy = row;
		cSearch.fill = GridBagConstraints.BOTH;
		cSearch.anchor = GridBagConstraints.LINE_START;
		cSearch.weightx = 1.0;
        panel.add(searchField, cSearch);
        row++;
        return row;
	}
	
	protected int addGraphSizeComponents(int row) {
		// graph size label
		JLabel graphSizeLabel = new JLabel("graph size");
		GridBagConstraints cGraphSizeLabel = new GridBagConstraints();
		cGraphSizeLabel.gridwidth = 2; cGraphSizeLabel.gridheight = 1;
		cGraphSizeLabel.gridx = 0; cGraphSizeLabel.gridy = row;
		cGraphSizeLabel.fill = GridBagConstraints.BOTH;
		cGraphSizeLabel.anchor = GridBagConstraints.LINE_START;
		cGraphSizeLabel.weighty = 1.0; // results in even spacing
		panel.add(graphSizeLabel, cGraphSizeLabel);
		
		// graph size slider
		Integer gMin = 40; Integer gMax = 150;
		graphSizeSlider = new JSlider(gMin, gMax, gMin);
		Hashtable<Integer,JLabel> graphSizeSliderLabels = new Hashtable<Integer,JLabel>();
		graphSizeSliderLabels.put(gMin, new JLabel(gMin.toString()));
		graphSizeSliderLabels.put(gMax, new JLabel(gMax.toString()));
		graphSizeSlider.setLabelTable(graphSizeSliderLabels);
		graphSizeSlider.setPaintLabels(true);
		GridBagConstraints cGraphSizeSlider = new GridBagConstraints();
		cGraphSizeSlider.gridwidth = 2; cGraphSizeSlider.gridheight = 1;
		cGraphSizeSlider.gridx = 2; cGraphSizeSlider.gridy = row;
		cGraphSizeSlider.fill = GridBagConstraints.BOTH;
		cGraphSizeSlider.anchor = GridBagConstraints.LINE_START;
		cGraphSizeSlider.weighty = 1.0; // results in even spacing
		panel.add(graphSizeSlider, cGraphSizeSlider);
		row++;
		return row;
	}
	
	protected int addEdgeLenComponents(int row) {
		// edge length label
		JLabel lenLabel = new JLabel("length");
		GridBagConstraints cLenLabel = new GridBagConstraints();
		cLenLabel.gridwidth = 1; cLenLabel.gridheight = 1;
		cLenLabel.gridx = 0; cLenLabel.gridy = row;
		cLenLabel.fill = GridBagConstraints.BOTH;
		cLenLabel.anchor = GridBagConstraints.LINE_START;
		cLenLabel.weighty = 0;//1.0; // results in even spacing
		panel.add(lenLabel, cLenLabel);
		
		// edge length toggle
		lenToggle = new JCheckBox();
		lenToggle.setSelected(true);
		GridBagConstraints cLenToggle = new GridBagConstraints();
		cLenToggle.gridwidth = 1; cLenToggle.gridheight = 1;
		cLenToggle.gridx = 1; cLenToggle.gridy = row;
		cLenToggle.fill = GridBagConstraints.BOTH;
		cLenToggle.anchor = GridBagConstraints.LINE_START;
		cLenToggle.weighty = 0;//1.0; // results in even spacing
		panel.add(lenToggle, cLenToggle);
		
		// edge length slider
		Integer lMin = 100; Integer lMax = 1000;
		lenSlider = new JSlider(lMin, lMax, lMin);
		Hashtable<Integer,JLabel> lenSliderLabels = new Hashtable<Integer,JLabel>();
		lenSliderLabels.put(lMin, new JLabel(lMin.toString()));
		lenSliderLabels.put(lMax, new JLabel(lMax.toString()));
		lenSlider.setLabelTable(lenSliderLabels);
		lenSlider.setPaintLabels(true);
		GridBagConstraints cLenSlider = new GridBagConstraints();
		cLenSlider.gridwidth = 2; cLenSlider.gridheight = 1;
		cLenSlider.gridx = 2; cLenSlider.gridy = row;
		cLenSlider.fill = GridBagConstraints.HORIZONTAL;
		cLenSlider.anchor = GridBagConstraints.LINE_START;
		cLenSlider.weighty = 0;//1.0; // results in even spacing
		panel.add(lenSlider, cLenSlider);
		row++;
		return row;
	}

	protected int addLabelComponents(int row) {
		// labels
		JLabel labels = new JLabel("labels");
		GridBagConstraints cLabels = new GridBagConstraints();
		cLabels.gridwidth = 1; cLabels.gridheight = 1;
		cLabels.gridx = 0; cLabels.gridy = row;
		cLabels.fill = GridBagConstraints.BOTH;
		cLabels.anchor = GridBagConstraints.LINE_START;
		cLabels.weighty = 0;//1.0; // results in even spacing
		panel.add(labels, cLabels);
		
		// labels toggle
		labelsToggle = new JCheckBox();
		labelsToggle.setSelected(true);
		GridBagConstraints cLabelsToggle = new GridBagConstraints();
		cLabelsToggle.gridwidth = 1; cLabelsToggle.gridheight = 1;
		cLabelsToggle.gridx = 1; cLabelsToggle.gridy = row;
		cLabelsToggle.fill = GridBagConstraints.BOTH;
		cLabelsToggle.anchor = GridBagConstraints.LINE_START;
		cLabelsToggle.weighty = 0;//1.0; // results in even spacing
		panel.add(labelsToggle, cLabelsToggle);
		row++;
		return row;
	}
	
	protected int addPePartnerListComponents(int row) {
		// paired-end partner label
		JLabel partnerLabel = new JLabel("paired-end partners");
		GridBagConstraints cPartnerLabel = new GridBagConstraints();
		cPartnerLabel.gridwidth = 1; cPartnerLabel.gridheight = 1;
		cPartnerLabel.gridx = 0; cPartnerLabel.gridy = row;
		cPartnerLabel.fill = GridBagConstraints.BOTH;
		cPartnerLabel.anchor = GridBagConstraints.LINE_START;
		cPartnerLabel.weighty = 0;//1.0; // results in even spacing
		panel.add(partnerLabel, cPartnerLabel);
		
		// paired-end partner toggle
		pePartnerToggle = new JCheckBox();
		pePartnerToggle.setSelected(true);
		GridBagConstraints cPartnerToggle = new GridBagConstraints();
		cPartnerToggle.gridwidth = 1; cPartnerToggle.gridheight = 1;
		cPartnerToggle.gridx = 1; cPartnerToggle.gridy = row;
		cPartnerToggle.fill = GridBagConstraints.BOTH;
		cPartnerToggle.anchor = GridBagConstraints.LINE_START;
		cPartnerToggle.weighty = 0;//1.0; // results in even spacing
		panel.add(pePartnerToggle, cPartnerToggle);
		row++;
		
		// paired-end inbound partner label
		inPartnerLabel = new JLabel("inbound");
		GridBagConstraints cInPartnerLabel = new GridBagConstraints();
		cInPartnerLabel.gridwidth = 1; cInPartnerLabel.gridheight = 1;
		cInPartnerLabel.gridx = 0; cInPartnerLabel.gridy = row;
		cInPartnerLabel.fill = GridBagConstraints.BOTH;
		cInPartnerLabel.anchor = GridBagConstraints.LINE_START;
		cInPartnerLabel.weighty = 0;//1.0; // results in even spacing
		panel.add(inPartnerLabel, cInPartnerLabel);
		row++;
		
		// paired-end inbound partner display
		peInPartnerListPane = new JScrollPane(peInPartnerList);
                peInPartnerListPane.setPreferredSize(new java.awt.Dimension(0,0));
		GridBagConstraints cInPartnerList = new GridBagConstraints();
		cInPartnerList.gridwidth = 4; cInPartnerList.gridheight = 2;
		cInPartnerList.gridx = 0; cInPartnerList.gridy = row;
		cInPartnerList.fill = GridBagConstraints.BOTH;
		cInPartnerList.anchor = GridBagConstraints.LINE_START;
		cInPartnerList.weighty = 1.0; // results in even spacing
        panel.add(peInPartnerListPane, cInPartnerList);
        row += 2;
        
		// paired-end outbound partner label
		outPartnerLabel = new JLabel("outbound");
		GridBagConstraints cOutPartnerLabel = new GridBagConstraints();
		cOutPartnerLabel.gridwidth = 1; cOutPartnerLabel.gridheight = 1;
		cOutPartnerLabel.gridx = 0; cOutPartnerLabel.gridy = row;
		cOutPartnerLabel.fill = GridBagConstraints.BOTH;
		cOutPartnerLabel.anchor = GridBagConstraints.LINE_START;
		cOutPartnerLabel.weighty = 0;//1.0; // results in even spacing
		panel.add(outPartnerLabel, cOutPartnerLabel);
		row++;
        
		// paired-end outbound partner display
    	peOutPartnerListPane = new JScrollPane(peOutPartnerList);
        peOutPartnerListPane.setPreferredSize(new java.awt.Dimension(0,0));
		// peInPartnerListPane.setPreferredSize(new Dimension(250,100));
		GridBagConstraints cOutPartnerList = new GridBagConstraints();
		cOutPartnerList.gridwidth = 4; cOutPartnerList.gridheight = 2;
		cOutPartnerList.gridx = 0; cOutPartnerList.gridy = row;
		cOutPartnerList.fill = GridBagConstraints.BOTH;
		cOutPartnerList.anchor = GridBagConstraints.LINE_START;
		cOutPartnerList.weighty = 1.0; // results in even spacing
        panel.add(peOutPartnerListPane, cOutPartnerList);
        row += 2;
        return row;
	}
	
	protected int addPeListComponents(int row) {
		// paired-end contig (path) label
		peListLabel = new JLabel("paired-end contigs");
		GridBagConstraints cPeListLabel = new GridBagConstraints();
		cPeListLabel.gridwidth = 1; cPeListLabel.gridheight = 1;
		cPeListLabel.gridx = 0; cPeListLabel.gridy = row;
		cPeListLabel.fill = GridBagConstraints.BOTH;
		cPeListLabel.anchor = GridBagConstraints.LINE_START;
		cPeListLabel.weighty = 0;//1.0; // results in even spacing
		panel.add(peListLabel, cPeListLabel);
        
		// paired-end contig (path) toggle
		peListToggle = new JCheckBox();
		peListToggle.setSelected(true);
		GridBagConstraints cPeListToggle = new GridBagConstraints();
		cPeListToggle.gridwidth = 1; cPeListToggle.gridheight = 1;
		cPeListToggle.gridx = 1; cPeListToggle.gridy = row;
		cPeListToggle.fill = GridBagConstraints.BOTH;
		cPeListToggle.anchor = GridBagConstraints.LINE_START;
		cPeListToggle.weighty = 0;//1.0; // results in even spacing
		panel.add(peListToggle, cPeListToggle);
		row++;
		
		// paired-end contig (path) list
		peListPane = new JScrollPane(peList);
                peListPane.setPreferredSize(new java.awt.Dimension(0,0));
		GridBagConstraints cPeList = new GridBagConstraints();
		cPeList.gridwidth = 4; cPeList.gridheight = 2;
		cPeList.gridx = 0; cPeList.gridy = row;
		cPeList.fill = GridBagConstraints.BOTH;
		cPeList.anchor = GridBagConstraints.LINE_START;
		cPeList.weighty = 1.0; // results in even spacing
        panel.add(peListPane, cPeList);
        row += 2;
        return row;
	}
	
	public void disableComponents() {
		//searchField.setEnabled(false);
		// graphSizeSlider.setEnabled(false);
		labelsToggle.setEnabled(false);
		lenToggle.setEnabled(false);
		lenSlider.setEnabled(false);
		pePartnerToggle.setEnabled(false);
		peListToggle.setEnabled(false);
	}
	
	public void enableComponenents() {
		//searchField.setEnabled(true);
		// graphSizeSlider.setEnabled(true);
		labelsToggle.setEnabled(true);
		lenToggle.setEnabled(true);
		lenSlider.setEnabled(true);
		pePartnerToggle.setEnabled(true);
		peListToggle.setEnabled(true);
	}
	
	protected void initializeLists() {
		
		peInPartnerListModel = new DefaultListModel();
		peInPartnerList = new JList(peInPartnerListModel);
		peInPartnerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		peInPartnerList.setFocusable(false); // prevents selection frame upon clicking a list member
		
		peOutPartnerListModel = new DefaultListModel();
		peOutPartnerList = new JList(peOutPartnerListModel);
        peOutPartnerList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        peOutPartnerList.setFocusable(false); // prevents selection frame upon clicking a list member
        
		peListModel = new DefaultListModel();
		peList = new JList(peListModel);
		peList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		peList.setFocusable(false);
	}
	
	public JPanel getControlPanel() {
		return panel;
	}
	
	protected void addToPeList(String s) {
		peListModel.addElement(s);
	}
	
	protected void addToInboundList(String s) {
		peInPartnerListModel.addElement(s);
	}
	
	protected void addToOutboundList(String s) {
		peOutPartnerListModel.addElement(s);
	}
	
	public void clear() {
		clearPeList();
		clearInboundList();
		clearOutboundList();
		clearSearch();
	}
	
	public void clearSearch() {
		searchField.setText("");
		searchField.setForeground(Color.black);
	}
	
	public void clearInboundList() {
		peInPartnerListModel.clear();
	}
	
	public void clearOutboundList() {
		peOutPartnerListModel.clear();
	}
	
	public void clearPeList() {
		peListModel.clear();
	}
	
	public void clearInSelectedPartner() {
		peInPartnerList.clearSelection();
	}

	public void clearOutSelectedPartner() {
		peOutPartnerList.clearSelection();
	}

	public int getLenSliderValue() {
		return lenSlider.getValue();
	}
	
	public String getQueryId() {
		String id = searchField.getText();
		if (id.trim().length() == 0) { return null; };
		if (id.contains("+") || id.contains("-")) {
			id = id.trim();
		} else {
			id = id.trim() + "+";
		}
		return id;
	}

        public void setQueryId(String id) {
            searchField.setForeground(Color.black);
            searchField.setText(id);
            searchField.postActionEvent();
        }
	
	public String getSelectedInPartner() {
		String peInfo = (String) peInPartnerList.getSelectedValue();
		if (peInfo == null) { return null; }
		return peInfo.split("\t")[0];
	}
	
	public String getSelectedOutPartner() {
		String peInfo = (String) peOutPartnerList.getSelectedValue();
		if (peInfo == null) { return null; }
		return peInfo.split("\t")[0];
	}
	
	public String getSelectedPeContig() {
		return (String) peList.getSelectedValue();
	}
	
	public boolean partnerListOn() {
		return pePartnerToggle.isSelected();
	}
	
	public boolean peListOn() {
		return peListToggle.isSelected();
	}
	
	public boolean setLenToggle(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			lenSlider.setEnabled(false);
			return false;
		} else {
			lenSlider.setEnabled(true);
			return true;
		}
	}
	
	public void setInboundList(List<String> items) {
		clearInboundList();
		for (String s: items) {
			addToInboundList(s);
		}
	}
	
	public void setOutboundList(List<String> items) {
		clearOutboundList();
		for (String s: items) {
			addToOutboundList(s);
		}
	}
	
	public boolean setPePartnerListToggle(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			inPartnerLabel.setForeground(Color.gray);
			peInPartnerListPane.setEnabled(false);
			clearInboundList();
			outPartnerLabel.setForeground(Color.gray);
			peOutPartnerListPane.setEnabled(false);
			clearOutboundList();
			return false;
		} else {
			inPartnerLabel.setForeground(Color.black);
			peInPartnerListPane.setEnabled(true);
			outPartnerLabel.setForeground(Color.black);
			peOutPartnerListPane.setEnabled(true);
			return true;
		}
	}
	
	public boolean setPeListToggle(ItemEvent e) {
		if (e.getStateChange() == ItemEvent.DESELECTED) {
			peListPane.setEnabled(false);
			clearPeList();
			return false;
		} else {
			peListPane.setEnabled(true);
			return true;
		}
	}
	
	public void setPeListToggleOn() {
		peListToggle.setSelected(true);
		peListPane.setEnabled(true);
	}
	
	public void setPeList(List<ContigLabel> labels) {
		clearPeList();
		for (ContigLabel l: labels) {
			addToPeList(l.getLabel());
		}
	}
	
	public void setSelectedPeId(String s) {
		for (int i=0; i<peList.getModel().getSize(); i++) {
			if (s.equals(peList.getModel().getElementAt(i))) {
				peList.setSelectedIndex(i);
				break;
			}
		}
	}
	
	public String selectPeListItem(MouseEvent e) {
		int index = peList.locationToIndex(e.getPoint());
		peList.setSelectedIndex(index);
		return getSelectedPeContig();
	}
	
	public String selectInPartnerListItem(MouseEvent e) {
		int index = peInPartnerList.locationToIndex(e.getPoint());
		peInPartnerList.setSelectedIndex(index);
		return getSelectedInPartner();
	}
	
	public String selectOutPartnerListItem(MouseEvent e) {
		int index = peOutPartnerList.locationToIndex(e.getPoint());
		peOutPartnerList.setSelectedIndex(index);
		return getSelectedOutPartner();
	}
	
	public void readyForSearch() {
		searchField.setText("Search");
		searchField.setForeground(Color.gray);
	}
	

}
