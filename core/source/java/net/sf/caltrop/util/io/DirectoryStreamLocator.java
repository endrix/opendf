package net.sf.caltrop.util.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.caltrop.util.logging.Logging;

public class DirectoryStreamLocator implements StreamLocator {

	public InputStream getAsStream(String name) {
		File f = new File(dirpath + name);
		try {
			InputStream is = new FileInputStream(f);
			
			Logging.dbg().info("DirectoryStreamLocator: " + dirpath + "::" + name + " -- " + ((is == null) ? "failed" : "succeeded") + ".");
			return is;
		}
		catch (Exception e) {
			Logging.dbg().info("DirectoryStreamLocator: " + dirpath + "::" + name + " -- failed.");
			return null;
		}
	}
	
	public DirectoryStreamLocator(String dirpath) {
		this.dirpath = dirpath + File.separator;
	}

	private String dirpath;
}
