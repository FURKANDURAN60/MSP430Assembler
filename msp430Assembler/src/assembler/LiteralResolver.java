package assembler;

public class LiteralResolver {

    public static int resolve(String literal) {
        literal = literal.trim();


        // 1. Binary: 0b1010 or 1010B
        if (literal.matches("0[bB][01]+")) {
            return Integer.parseInt(literal.substring(2), 2);
        }
        if (literal.matches("[01]+[bB]")) {
            return Integer.parseInt(literal.substring(0, literal.length() - 1), 2);
        }

        // 2. Octal: 0123 or 77Q
        if (literal.matches("0[0-7]+")) {
            return Integer.parseInt(literal.substring(1), 8);
        }
        if (literal.matches("[0-7]+[Qq]")) {
            return Integer.parseInt(literal.substring(0, literal.length() - 1), 8);
        }

        // 3. Decimal (varsayılan)
        if (literal.matches("[-+]?\\d+")) {
            return Integer.parseInt(literal);
        }

        // 4. Hexadecimal: 0x2A or 3Fh (ilk karakter rakam olmalı)
        if (literal.matches("0[xX][0-9a-fA-F]+")) {
            return Integer.parseInt(literal.substring(2), 16);
        }
        if (literal.matches("[0-9][0-9a-fA-F]*[Hh]")) {
            return Integer.parseInt(literal.substring(0, literal.length() - 1), 16);
        }

        // 6. Character literal: 'A'
        if (literal.matches("^'.'$")) {
            return literal.charAt(1);
        }


        throw new IllegalArgumentException("Geçersiz literal formatı: " + literal);
    }

    public static int resolve(String expr, SymbolTable symbolTable) {
        try {
            // Önce literal olarak parse etmeye çalış
            return resolve(expr);
        } catch (IllegalArgumentException e) {
            // Olmazsa sembolik ifade gibi değerlendir
            ExpressionEvaluator evaluator = new ExpressionEvaluator(symbolTable);
            return evaluator.evaluate(expr);
        }
    }



}
