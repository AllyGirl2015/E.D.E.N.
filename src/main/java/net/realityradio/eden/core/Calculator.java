package net.realityradio.eden.core;

/** Small bounded arithmetic parser, independent of scripts/JS engines. */
public final class Calculator {
    private final String input;
    private int index;
    private int depth;
    private Calculator(String input) { this.input = input; }
    public static double evaluate(String input) {
        if (input == null || input.isBlank() || input.length() > 128)
            throw new IllegalArgumentException("Enter an expression of up to 128 characters");
        var parser = new Calculator(input);
        double result = parser.expression();
        parser.space();
        if (parser.index != input.length() || !Double.isFinite(result))
            throw new IllegalArgumentException("Invalid expression or non-finite result");
        return result;
    }
    private void space() { while (index < input.length() && Character.isWhitespace(input.charAt(index))) index++; }
    private boolean eat(char c) { space(); if (index < input.length() && input.charAt(index) == c) { index++; return true; } return false; }
    private double expression() {
        double x = term();
        while (true) { if (eat('+')) x += term(); else if (eat('-')) x -= term(); else return x; }
    }
    private double term() {
        double x = factor();
        while (true) { if (eat('*')) x *= factor(); else if (eat('/')) x /= factor(); else return x; }
    }
    private double factor() {
        if (++depth > 32) throw new IllegalArgumentException("Expression nesting is too deep");
        try {
            if (eat('+')) return factor();
            if (eat('-')) return -factor();
            if (eat('(')) { double x = expression(); if (!eat(')')) throw new IllegalArgumentException("Missing )"); return x; }
            space(); int start = index;
            while (index < input.length() && (Character.isDigit(input.charAt(index)) || input.charAt(index) == '.')) index++;
            if (start == index) throw new IllegalArgumentException("Expected a number");
            return Double.parseDouble(input.substring(start, index));
        } finally { depth--; }
    }
}
