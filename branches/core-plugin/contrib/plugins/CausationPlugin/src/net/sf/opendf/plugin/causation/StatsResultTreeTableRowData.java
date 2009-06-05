package net.sf.opendf.plugin.causation;

public class StatsResultTreeTableRowData {
	private String actionName;
	private Integer count;
	private String proportion;
	private String global;
	private boolean isRoot;
	
	public StatsResultTreeTableRowData(String actionName, Integer count, String proportion, String global, boolean isRoot)
	{
		this.actionName = actionName;
		this.count = count;
		this.isRoot = isRoot;
		this.proportion = proportion;
		this.global = global;
	}
	
	public String getActionName(){
		return actionName;
	}
	
	public void setActionName(String actionName){
		this.actionName = actionName;
	}
	
	public Integer getCount(){
		return count;
	}
	
	public void setCount(Integer count){
		this.count = count;
	}
	
	public String getProportion(){
		return proportion;
	}
	
	public void setProportion(String proportion){
		this.proportion = proportion;
	}
	
	public String getGlobal(){
		return global;
	}
	
	public void setGlobal(String global){
		this.global = global;
	}
	
	public boolean isRoot(){
		return isRoot;
	}
	
	public void setRoot(boolean isRoot){
		this.isRoot = isRoot;
	}
}
