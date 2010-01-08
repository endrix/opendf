import xlimAST.Start;

public class PrettyPrint extends Parser {
        public static void main(String args[]) {
                Start ast = parse(args);

                // Dump the AST
                ast.prettyPrint("  ", System.out);
        }
}
