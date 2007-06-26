package net.sf.caltrop.xslt.nl;

import static net.sf.caltrop.util.Util.saxonify;
import static net.sf.caltrop.util.Util.xercify;
import net.sf.caltrop.cal.interpreter.util.DefaultPlatform;
import net.sf.caltrop.cal.interpreter.util.Platform;
import net.sf.caltrop.nl.Network;

import org.w3c.dom.Node;

public class Elaborating {

	
	public static Node  elaborate(Node network) {

			Platform p = DefaultPlatform.thePlatform;
			Node n = xercify(network);
			Node res = Network.translate(n, p.createGlobalEnvironment(), p.context());  // FIXME: must respect imports
			return saxonify(res);
	}

}
