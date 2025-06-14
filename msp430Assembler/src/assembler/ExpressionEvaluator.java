package assembler;

import java.util.*;

public class ExpressionEvaluator {

    private final SymbolTable symbolTable;
    private int pos;
    private String input;

    public ExpressionEvaluator(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    public int evaluate(String expr) {
        this.input = expr.replaceAll("\\s+", ""); // boşlukları sil
        this.pos = 0;
        return parseExpression();
    }

    private int parseExpression() {
        int value = parseTerm();

        while (pos < input.length()) {
            char op = input.charAt(pos);

            if (op == '+') {
                pos++;
                value += parseTerm();
            } else if (op == '-') {
                pos++;
                value -= parseTerm();
            } else {
                break;
            }
        }

        return value;
    }

    private int parseTerm() {
        int value = parseFactor();

        while (pos < input.length()) {
            char op = input.charAt(pos);

            if (op == '*') {
                pos++;
                value *= parseFactor();
            } else if (op == '/') {
                pos++;
                int divisor = parseFactor();
                if (divisor == 0) throw new RuntimeException("Sıfıra bölme hatası");
                value /= divisor;
            } else if (op == '%') {
                pos++;
                value %= parseFactor();
            } else {
                break;
            }
        }

        return value;
    }

    private int parseFactor() {
        if (pos >= input.length()) throw new RuntimeException("Eksik ifade");

        // Char literal: 'A'
        if (input.charAt(pos) == '\'' && pos + 2 < input.length() && input.charAt(pos + 2) == '\'') {
            int ascii = input.charAt(pos + 1);
            pos += 3;
            return ascii;
        }

        // Parantez
        if (input.charAt(pos) == '(') {
            pos++;
            int val = parseExpression();
            if (pos >= input.length() || input.charAt(pos) != ')') {
                throw new RuntimeException("Parantez kapanmadı");
            }
            pos++;
            return val;
        }

        // Token alma (sembol, sayı)
        int start = pos;
        while (pos < input.length() &&
                (Character.isLetterOrDigit(input.charAt(pos)) || input.charAt(pos) == '_')) {
            pos++;
        }

        String token = input.substring(start, pos);

        if (token.matches("0[xX][0-9a-fA-F]+")) {
            return Integer.parseInt(token.substring(2), 16);
        }

        if (token.matches("\\d+")) {
            return Integer.parseInt(token);
        }

        if (symbolTable.contains(token)) {
            return symbolTable.getAddress(token);
        }

        throw new RuntimeException("Geçersiz ifade veya sembol: " + token);
    }


}
