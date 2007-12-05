package net.sf.opendf.xslt.nl;

import static net.sf.opendf.util.xml.Util.saxonify;
import static net.sf.opendf.util.xml.Util.xercify;
import net.sf.opendf.cal.interpreter.util.DefaultPlatform;
import net.sf.opendf.cal.interpreter.util.Platform;
import net.sf.opendf.nl.Network;

import org.w3c.dom.Node;

public class Elaborating {

	
	public static Node  elaborate(Node network) {

			Platform p = DefaultPlatform.thePlatform;
			Node n = xercify(network);
			Node res = Network.translate(n, p.createGlobalEnvironment(), p.context());  // FIXME: must respect imports
			return saxonify(res);
	}

}
