package net.sf.opendf.profiler.schedule.data;

/**
 * 
 * @author jornj
 */

public class Resource {
	public Object  classID;
	public int     instanceID;
	
	public  int hashCode() {
		return 13 * classID.hashCode() + 17 * instanceID;
	}
	
	public boolean equals(Object a) {
		if (!(a instanceof Resource))
			return false;
		Resource r = (Resource)a;
		return (classID == r.classID) && (instanceID == r.instanceID);
	}
	
	public Resource(Object classID, int instanceID) {
		this.classID = classID;
		this.instanceID = instanceID;
	}
	
	public String toString() {
		return "(R: " + classID + ":" + instanceID + ")";
	}
}



