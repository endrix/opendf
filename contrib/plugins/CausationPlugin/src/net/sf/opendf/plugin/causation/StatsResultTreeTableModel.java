package net.sf.opendf.plugin.causation;

import java.text.DecimalFormat;
import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

public class StatsResultTreeTableModel extends AbstractTreeTableModel {

	private String[] columns={"Actor (ID) / Action", "Number of Calls", "% of calls", "% of all calls"};
	
	private static DefaultMutableTreeNode translate(Vector<Vector<String>> datas){
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(new StatsResultTreeTableRowData("",0,"", "",true));
    	
		Map<String, Integer> tots = new TreeMap<String, Integer>();
		Map<String, Integer> results = new TreeMap<String, Integer>();
		Map<String, Vector<String>> actions = new TreeMap<String, Vector<String>>();

		Iterator<Vector<String>> it = datas.iterator();
		int globaltot = 0;
		while(it.hasNext()){
			Vector<String> el = it.next();
			String actor = el.elementAt(0);
			String action = el.elementAt(1);
			int value = Integer.parseInt(el.elementAt(2));
			Integer tot = tots.get(actor);
			if(tot==null)
				tot = value;
			else
				tot += value;
			tots.put(actor, tot);
			globaltot += value;
			
			Vector<String> listaction = actions.get(actor);
			if(listaction==null)
				listaction = new Vector<String>();
			listaction.add(action);
			actions.put(actor, listaction);
			results.put(actor+action,value);
		}
		
		for(String actor:tots.keySet()){
			int tot = tots.get(actor);
			DefaultMutableTreeNode actionNode = new DefaultMutableTreeNode(new StatsResultTreeTableRowData(actor,tot,""
					, new DecimalFormat("0.00").format((double)tot/(double)globaltot*100.0)+"%",true));
			for(String action:actions.get(actor)){
				int result = results.get(actor+action);
				actionNode.add(new DefaultMutableTreeNode(new StatsResultTreeTableRowData(action,result,
						new DecimalFormat("0.00").format((double)result/(double)tot*100.0)+"%",
						new DecimalFormat("0.00").format((double)result/(double)globaltot*100.0)+"%", false)));
			}
			rootNode.add(actionNode);
		}
		return rootNode;
	}
	
	public StatsResultTreeTableModel(Vector<Vector<String>> datas)
	{
		super(translate(datas));
	}
	
	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return columns.length;
	}
	
	public String getColumnName(int column) {
		if (column < columns.length)
			return (String) columns[column];
		else
			return "";
	}
	
	public Class<?> getColumnClass(int column)
	{
		/*if(column==1)
			return Integer.class;*/
		return String.class;
	}

	@Override
	public Object getValueAt(Object arg0, int arg1) {
		if(arg0 instanceof StatsResultTreeTableRowData)
		{
			StatsResultTreeTableRowData data = (StatsResultTreeTableRowData)arg0;
			if(data != null)
			{
				switch(arg1)
				{
				case 0: return data.getActionName();
				case 1: return data.getCount();
				case 2: return data.getProportion();
				case 3: return data.getGlobal();
				}
			}			
		}
		
		if(arg0 instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode dataNode = (DefaultMutableTreeNode)arg0;
			StatsResultTreeTableRowData data = (StatsResultTreeTableRowData)dataNode.getUserObject();
			if(data != null)
			{
				switch(arg1)
				{
				case 0: return data.getActionName();
				case 1: return data.getCount();
				case 2: return data.getProportion();
				case 3: return data.getGlobal();
				}
			}
			
		}
		return null;
	}

	public Object getChild(Object arg0, int arg1)
	{
		
		if(arg0 instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode nodes = (DefaultMutableTreeNode)arg0;
			return nodes.getChildAt(arg1);
		}
		return null;
	}

	public int getChildCount(Object arg0)
	{
		
		if(arg0 instanceof DefaultMutableTreeNode)
		{
			DefaultMutableTreeNode nodes = (DefaultMutableTreeNode)arg0;
			return nodes.getChildCount();
		}
		return 0;
	}

	public int getIndexOfChild(Object arg0, Object arg1)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	
	 public boolean isLeaf(Object node) 
	 {
	        return getChildCount(node) == 0;
	 }

}
