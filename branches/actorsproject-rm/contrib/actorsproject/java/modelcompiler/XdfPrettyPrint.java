import xdfAST.Start;

public class XdfPrettyPrint extends XdfParser {
        public static void main(String args[]) {
                Start ast = parse(args);

                // Dump the AST
                ast.prettyPrint("  ", System.out);
        }
}
