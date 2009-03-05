package net.sf.opendf.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Misc {

	static public Object deepCopy(Object oldObj) 
	{
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try
		{
			ByteArrayOutputStream bos = 
				new ByteArrayOutputStream(); 
				oos = new ObjectOutputStream(bos); 
				// serialize and pass the object
				oos.writeObject(oldObj);   
				oos.flush();               
				ByteArrayInputStream bin = 
					new ByteArrayInputStream(bos.toByteArray()); 
				ois = new ObjectInputStream(bin);                  
				// return the new object
				return ois.readObject(); 
		}
		catch(Exception e)
		{
			throw new RuntimeException("Exception during object deepCopy.", e);
		}
		finally
		{
			try {
				oos.close();
				ois.close();
			}
			catch (Exception e) {
				throw new RuntimeException("Exception during cleanup (in object deepCopy).", e);
			}
		}
	}

	
	private Misc() {}
}
