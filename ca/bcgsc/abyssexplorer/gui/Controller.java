package ca.bcgsc.abyssexplorer.gui;

import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.ChangeListener;

import ca.bcgsc.abyssexplorer.graph.ContigLabel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author kmnip
 */
public class Controller {
	protected JPanel panel;

	protected JCheckBox labelsToggle;
	protected JCheckBox lenToggle;
	protected JSlider lenSlider;
//	protected JCheckBox pePartnerToggle;
	protected JLabel inPartnerLabel;
	protected JLabel outPartnerLabel;

        protected JTable outPartnerTable;
        protected JTable inPartnerTable;

        protected DefaultTableModel outPartnerTableModel;
        protected DefaultTableModel inPartnerTableModel;

//	protected JCheckBox peListToggle;
	protected DefaultComboBoxModel peListModel;
	protected JComboBox peComboBox;
        protected JLabel partnerListTitle;
        protected JLabel peLabel;
        protected JRadioButton peRadioButton;
        protected JRadioButton seRadioButton;
        protected JRadioButton scaffoldRadioButton;

        public Controller(
            JCheckBox labelsToggle,
            JCheckBox lenToggle,
            JSlider lenSlider,
//            JCheckBox pePartnerToggle,
            JLabel inPartnerLabel,	
            JTable peInPartnerTable,
            JLabel outPartnerLabel,
            JTable peOutPartnerTable,
//            JCheckBox peListToggle,
            JComboBox peComboBox,
            JLabel partnerListTitle,
            JLabel peLabel,
            JRadioButton peRadioButton,
            JRadioButton seRadioButton,
            JRadioButton scaffoldRadioButton
            ){

            this.labelsToggle = labelsToggle;
            this.lenToggle = lenToggle;
            this.lenSlider = lenSlider;
//            this.pePartnerToggle = pePartnerToggle;
            this.inPartnerLabel = inPartnerLabel;
            this.inPartnerTable = peInPartnerTable;
            this.outPartnerLabel = outPartnerLabel;
            this.outPartnerTable = peOutPartnerTable;
//            this.peListToggle = peListToggle;
            this.peComboBox = peComboBox;
            this.partnerListTitle = partnerListTitle;
            this.peLabel = peLabel;
            this.peRadioButton = peRadioButton;
            this.seRadioButton = seRadioButton;
            this.scaffoldRadioButton = scaffoldRadioButton;
            
            initializeLists();
        }

	protected void initializeLists() {
            outPartnerTableModel = new AbyssExplorer.PartnerTableModel();
            outPartnerTable.setModel(outPartnerTableModel);
            outPartnerTable.getRowSorter().toggleSortOrder(1); // sort by distance by default
            outPartnerTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            outPartnerTable.setFocusable(false);

            inPartnerTableModel = new AbyssExplorer.PartnerTableModel();
            inPartnerTable.setModel(inPartnerTableModel);
            inPartnerTable.getRowSorter().toggleSortOrder(1); // sort by distance by default
            inPartnerTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
            inPartnerTable.setFocusable(false);

            peListModel = new DefaultComboBoxModel();
            peComboBox.setModel(peListModel);
            peLabel.setText("Contigs: (0)");
	}

//	public void disableComponents() {
//		labelsToggle.setEnabled(false);
//		lenToggle.setEnabled(false);
//		lenSlider.setEnabled(false);
//		pePartnerToggle.setEnabled(false);
//		peListToggle.setEnabled(false);
//
//                peComboBox.setEnabled(false);
//                inPartnerLabel.setEnabled(false);
//                outPartnerLabel.setEnabled(false);
//                partnerListTitle.setEnabled(false);
//                peLabel.setEnabled(false);
//                peRadioButton.setEnabled(false);
//                seRadioButton.setEnabled(false);
//	}
//
//	public void enableComponenents() {
//		labelsToggle.setEnabled(true);
//		lenToggle.setEnabled(true);
//		lenSlider.setEnabled(true);
//                boolean se = seRadioButton.isSelected();
//		pePartnerToggle.setEnabled(se);
//		peListToggle.setEnabled(se);
//
//                peComboBox.setEnabled(true);
//                inPartnerLabel.setEnabled(true);
//                outPartnerLabel.setEnabled(true);
//                partnerListTitle.setEnabled(true);
//                peLabel.setEnabled(true);
//                peRadioButton.setEnabled(true);
//                seRadioButton.setEnabled(true);
//	}

//        public void addPeModeListener(ItemListener l){
//            peRadioButton.addItemListener(l);
//        }

	public void addPeListListener(ActionListener l) {
		peComboBox.addActionListener(l);
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

	public void addInPartnerListener(MouseListener l) {
		inPartnerTable.addMouseListener(l);
	}

	public void addInPartnerMotionListener(MouseMotionListener l) {
		inPartnerTable.addMouseMotionListener(l);
	}

	public void addOutPartnerListener(MouseListener l) {
		outPartnerTable.addMouseListener(l);
	}

	public void addOutPartnerMotionListener(MouseMotionListener l) {
		outPartnerTable.addMouseMotionListener(l);
	}

//	public void addPartnerToggleListener(ItemListener l) {
//		pePartnerToggle.addItemListener(l);
//	}
//
//	public void addPeListToggleListener(ItemListener l) {
//		peListToggle.addItemListener(l);
//	}

	protected void addToPeList(String s) {
		peListModel.addElement(s);
                peLabel.setText("Contigs: (" + peListModel.getSize() + ")");
	}
	
//	protected void addToInboundList(String s) {
//		peInPartnerListModel.addElement(s);
//	}
//
//	protected void addToOutboundList(String s) {
//		peOutPartnerListModel.addElement(s);
//	}
	
        protected void addToInboundTable(Object[] rowData){
            inPartnerTableModel.addRow(rowData);
        }

        protected void addToOutboundTable(Object[] rowData){
            outPartnerTableModel.addRow(rowData);
        }

	public void clear() {
		clearPeList();
		clearInboundList();
		clearOutboundList();
	}

	public void clearInboundList() {
            inPartnerTable.clearSelection();
            for(int i=inPartnerTableModel.getRowCount()-1; i>=0; i--){
                inPartnerTableModel.removeRow(i);
            }
	}

	public void clearOutboundList() {
            outPartnerTable.clearSelection();
            for(int i=outPartnerTableModel.getRowCount()-1; i>=0; i--){
                outPartnerTableModel.removeRow(i);
            }
	}

	public void clearPeList() {
		peListModel.removeAllElements();
                peLabel.setText("Contigs: (0)");
	}

	public void clearInSelectedPartner() {
		inPartnerTable.clearSelection();
	}

	public void clearOutSelectedPartner() {
		outPartnerTable.clearSelection();
	}

	public int getLenSliderValue() {
		return lenSlider.getValue();
	}

	public String getSelectedInPartner() {
            int s = inPartnerTable.getSelectedRow();
            if(s < 0){
                return null;
            }

            return (String) inPartnerTable.getValueAt(s, 0);
	}

	public String getSelectedOutPartner() {
            int s = outPartnerTable.getSelectedRow();
            if(s < 0){
                return null;
            }

            return (String) outPartnerTable.getValueAt(s, 0);
	}

	public String getSelectedPeContig() {
		return (String) peListModel.getSelectedItem();
	}

//	public boolean partnerListOn() {
//		return pePartnerToggle.isSelected();
//	}
//
//	public boolean peListOn() {
//		return peListToggle.isSelected();
//	}

        public boolean getLenToggle(){
            return lenToggle.isSelected();
        }

//	public boolean setLenToggle(ItemEvent e) {
//		if (e.getStateChange() == ItemEvent.DESELECTED) {
//			lenSlider.setEnabled(false);
//			return false;
//		} else {
//			lenSlider.setEnabled(true);
//			return true;
//		}
//	}

//	public void setInboundList(List<String> items) {
//		clearInboundList();
//		for (String s: items) {
//			addToInboundList(s);
//		}
//	}
//
//	public void setOutboundList(List<String> items) {
//		clearOutboundList();
//		for (String s: items) {
//			addToOutboundList(s);
//		}
//	}

        public void setInboundTable(List<Object[]> data){
            clearInboundList();
            for(Object[] rowData : data){
                addToInboundTable(rowData);
            }
        }

        public void setOutboundTable(List<Object[]> data){
            clearOutboundList();
            for(Object[] rowData : data){
                addToOutboundTable(rowData);
            }
        }

//        public boolean getPePartnerListToggle(){
//            return pePartnerToggle.isSelected();
//        }

//	public boolean setPePartnerListToggle(ItemEvent e) {
//		if (e.getStateChange() == ItemEvent.DESELECTED) {
//			inPartnerLabel.setEnabled(false);//setForeground(Color.gray);
//			//peInPartnerListPane.setEnabled(false);
//                        peInPartnerList.setEnabled(false);
//			clearInboundList();
//			outPartnerLabel.setEnabled(false);//.setForeground(Color.gray);
//			//peOutPartnerListPane.setEnabled(false);
//                        peOutPartnerList.setEnabled(false);
//                        partnerListTitle.setEnabled(false);
//			clearOutboundList();
//			return false;
//		} else {
//			inPartnerLabel.setEnabled(true);//.setForeground(Color.black);
//			//peInPartnerListPane.setEnabled(true);
//                        peInPartnerList.setEnabled(true);
//			outPartnerLabel.setEnabled(true);//.setForeground(Color.black);
//			//peOutPartnerListPane.setEnabled(true);
//                        peOutPartnerList.setEnabled(true);
//                        partnerListTitle.setEnabled(true);
//			return true;
//		}
//	}

        public void setPePartnerList(boolean on){
            inPartnerLabel.setEnabled(on);
            inPartnerTable.setEnabled(on);
            outPartnerLabel.setEnabled(on);
            outPartnerTable.setEnabled(on);
            partnerListTitle.setEnabled(on);

            if(!on){
                clearInboundList();
                clearOutboundList();
            }
        }

//        public boolean getPeListToggle(){
//            return peListToggle.isSelected();
//        }

//	public boolean setPeListToggle(ItemEvent e) {
//		if (e.getStateChange() == ItemEvent.DESELECTED) {
//                        peLabel.setEnabled(false);
//			peComboBox.setEnabled(false);
//			clearPeList();
//			return false;
//		} else {
//                        peLabel.setEnabled(true);
//			peComboBox.setEnabled(true);
//			return true;
//		}
//	}

        public void setPeList(boolean on){
//            peLabel.setEnabled(on);
//            peComboBox.setEnabled(on);
            peLabel.setVisible(on);
            peComboBox.setVisible(on);
            
            if(!on){
                clearPeList();
            }
        }

//	public void setPeListToggleOn() {
//		peListToggle.setSelected(true);
////		peComboBox.setEnabled(true);
//	}

	public void setPeList(List<ContigLabel> labels) {
                peListModel = new DefaultComboBoxModel();
		for (ContigLabel l: labels) {
			addToPeList(l.getLabel());
		}
                peComboBox.setModel(peListModel);
                peLabel.setText("Contigs: (" + peListModel.getSize() + ")");
	}

	public void setPeList(List<ContigLabel> labels, String s) {
                peListModel = new DefaultComboBoxModel();
		for (ContigLabel l: labels) {
			addToPeList(l.getLabel());
		}
                peListModel.setSelectedItem(s);
                peComboBox.setModel(peListModel);
                peLabel.setText("Contigs: (" + peListModel.getSize() + ")");
	}

	public void setSelectedPeId(String s) {
            // if 's' has already been selected, don't select 's' again because it will fire a statechanged event, which is not necessary
            if(!((String)peListModel.getSelectedItem()).equals(s)){
                peListModel.setSelectedItem(s);
            }
//		for (int i=0; i<peListModel.getSize(); i++) {
//			if (s.equals(peListModel.getElementAt(i))) {
//				peComboBox.setSelectedIndex(i);
//				break;
//			}
//		}
	}

	public String selectPeListItem() {
		return (String) peComboBox.getSelectedItem();
	}

	public String selectInPartnerListItem(MouseEvent e) {
            int index = inPartnerTable.rowAtPoint(e.getPoint());
            inPartnerTable.clearSelection();
            inPartnerTable.addRowSelectionInterval(index, index);
		return getSelectedInPartner();
	}

	public String selectOutPartnerListItem(MouseEvent e) {
            int index = outPartnerTable.rowAtPoint(e.getPoint());
            outPartnerTable.clearSelection();
            outPartnerTable.addRowSelectionInterval(index, index);
		return getSelectedOutPartner();
	}
}
