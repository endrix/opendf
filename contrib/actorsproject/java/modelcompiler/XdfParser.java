import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.Reader;

import xdfAST.XmlParser;
import xdfAST.ParseException;
import xdfAST.Start;

public class XdfParser {

	public static Start parse(String path) {
		String[] a = {path,};
		return parse(a);
	}

	public static Start parse(String args[]) {
		Reader r = getReader(args);
		Start ast = null;
		try {
			XmlParser parser = new XmlParser(r);

			ast = parser.Start();
			ast.setCWD(args[0]);
		} catch (ParseException e) {
			System.out.println(e.getMessage());
		}
		return ast;
	}

	private static Reader getReader(String[] args) {
		Reader r = null;
		if (args.length != 1) {
			r = new InputStreamReader(System.in);
		} else {
			try {
				r = new FileReader(args[0]);
			} catch (FileNotFoundException e1) {
				System.err.println("Dumper: file " + args[0] + " not found");
			}
		}
		return r;
	}
}
