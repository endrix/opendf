import xlimAST.Start;

public class XlimPrettyPrint extends XlimParser {
        public static void main(String args[]) {
                Start ast = parse(args);

                // Dump the AST
                ast.prettyPrint("  ", System.out);
        }
}
