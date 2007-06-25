package net.sf.caltrop.xslt.util;

import static net.sf.caltrop.util.Util.createXML;

import java.io.InputStream;

import net.sf.caltrop.cli.ModelClassLocator;
import net.sf.caltrop.cli.lib.ClassLoaderModelClassLocator;
import net.sf.caltrop.util.Logging;
import net.sf.caltrop.xslt.util.loader.CalLoader;
import net.sf.caltrop.xslt.util.loader.CalMLLoader;
import net.sf.caltrop.xslt.util.loader.NLLoader;
import net.sf.caltrop.xslt.util.loader.XDFLoader;
import net.sf.caltrop.xslt.util.loader.XNLLoader;

import org.w3c.dom.Node;

public class Loading {
	
	public static Node  loadActorSource(String qname) {
		
		String baseName = "/" + qname.replace('.', '/');
		
		for (SourceLoader l : loaders) {
			try {
				String name = baseName + "." + l.extension();
				InputStream s = getSourceAsStream(name);
				if (s != null) {
					Node res = l.load(s);
					String xml = createXML(res);
					Logging.dbg().info("Loaded: " + xml);
					return res;
				}
			}
			catch (Exception e) {}
		}		
		return null;
	}

	
	public static void setLocators(ModelClassLocator [] mcl) {
		locators = mcl;
	}
	
	public static ModelClassLocator[] getLocators() {
		if (locators == null) {
			locators = new ModelClassLocator [] { new ClassLoaderModelClassLocator(Loading.class.getClassLoader()) };
		}
		return locators;
	}

	private static InputStream  getSourceAsStream(String name) {
		for (ModelClassLocator mcl : getLocators()) {
			InputStream s = mcl.getAsStream(name);
			if (s != null)
				return s;
		}
		return null;
	}
		
	private static SourceLoader [] loaders = {
		new CalLoader(),
		new NLLoader(),
		new XNLLoader(),
		new XDFLoader(),
		new CalMLLoader()
	};
	
	private static ModelClassLocator [] locators;

}
