package net.sf.caltrop.xslt.nl;

import net.sf.caltrop.cal.interpreter.Context;
import net.sf.caltrop.cal.interpreter.util.DefaultContext;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.nl.Network;
import net.sf.caltrop.util.Logging;

import org.w3c.dom.Node;

import static net.sf.caltrop.util.Util.saxonify;
import static net.sf.caltrop.util.Util.xercify;

public class Elaborating {

	
	public static Node  elaborate(Node network) {

		try {
			Platform p = DefaultPlatform.thePlatform;
			Node n = xercify(network);
			Node res = Network.translate(n, p.createGlobalEnvironment(), p.context());  // FIXME: must respect imports
			return saxonify(res);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
