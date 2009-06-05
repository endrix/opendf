package net.sf.opendf.plugin.causation;

import java.util.Vector;

import javax.swing.table.AbstractTableModel;

public class StatsResultTableModel extends AbstractTableModel {
	
	private Vector<Vector<String>> datas;
	
	private String[] columns={"Actors Name", "Action Name", "Number of Calls"};
	
	public int getColumnCount() {
		return columns.length;
	}

	public String getColumnName(int col) {
		return columns[col];
	}
	
	public Class<?> getColumnClass(int col) { 
		return String.class; 
	}

	public StatsResultTableModel(Vector<Vector<String>> datas){
		this.datas = datas;
	}

	public Object getValueAt(int parm1, int parm2) {
		return datas.elementAt(parm1).elementAt(parm2);
	}

	public int getRowCount() {
		return datas.size();
	}

}
