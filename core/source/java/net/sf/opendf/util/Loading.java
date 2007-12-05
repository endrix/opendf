package net.sf.opendf.util;

import static net.sf.opendf.util.xml.Util.createXML;

import java.io.InputStream;

import net.sf.opendf.cal.util.CalLoader;
import net.sf.opendf.nl.util.NLLoader;
import net.sf.opendf.util.io.ClassLoaderStreamLocator;
import net.sf.opendf.util.io.StreamLocator;
import net.sf.opendf.util.logging.Logging;
import net.sf.opendf.util.source.CalMLLoader;
import net.sf.opendf.util.source.SourceLoader;
import net.sf.opendf.util.source.XDFLoader;
import net.sf.opendf.util.source.XNLLoader;
import net.sf.opendf.util.exception.LocatableException;

import org.w3c.dom.Node;

public class Loading {
	
	public static Node  loadActorSource(String qname)
    {
		String baseName = "/" + qname.replace('.', '/');

        for (SourceLoader l : loaders)
        {
            String name = baseName + "." + l.extension();
            try
            {
                InputStream s = getSourceAsStream(name);
                if (s != null) {
                    Node res = l.load(s);
                    return res;
                }
            }
            catch (RuntimeException e)
            {
                throw new LocatableException(e, qname);
            }
        }
        
		return null;
	}

	
	public static void setLocators(StreamLocator [] mcl) {
		locators = mcl;
	}
	
	public static StreamLocator[] getLocators() {
		if (locators == null) {
			locators = new StreamLocator [] { new ClassLoaderStreamLocator(Loading.class.getClassLoader()) };
		}
		return locators;
	}

	private static InputStream  getSourceAsStream(String name) {
		for (StreamLocator mcl : getLocators()) {
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
	
	private static StreamLocator [] locators;

}
