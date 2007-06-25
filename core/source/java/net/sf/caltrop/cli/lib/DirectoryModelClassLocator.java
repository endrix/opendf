package net.sf.caltrop.cli.lib;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import net.sf.caltrop.cli.ModelClassLocator;
import net.sf.caltrop.util.Logging;

public class DirectoryModelClassLocator implements ModelClassLocator {

	public InputStream getAsStream(String name) {
		File f = new File(dirpath + name);
		try {
			InputStream is = new FileInputStream(f);
			
			Logging.dbg().info("DirectoryModelClassLocator: " + dirpath + "::" + name + " -- " + ((is == null) ? "failed" : "succeeded") + ".");
			return is;
		}
		catch (Exception e) {
			Logging.dbg().info("DirectoryModelClassLocator: " + dirpath + "::" + name + " -- failed.");
			return null;
		}
	}
	
	public DirectoryModelClassLocator(String dirpath) {
		this.dirpath = dirpath + File.separator;
	}

	private String dirpath;
}
