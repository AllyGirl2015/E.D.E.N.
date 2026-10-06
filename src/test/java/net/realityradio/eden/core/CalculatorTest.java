package net.realityradio.eden.core;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CalculatorTest {
    @Test void precedenceParenthesesAndUnary() {
        assertEquals(14, Calculator.evaluate("2 + 3 * 4"));
        assertEquals(5, Calculator.evaluate("(12 + 8) / 4"));
        assertEquals(-7.5, Calculator.evaluate("-2.5 * 3"));
    }
    @Test void rejectsDivisionByZeroScriptsAndTrailingGarbage() {
        for (var input : new String[]{"1/0", "0/0", "1 +", "1.2.3", "(2", "2 abc", "eval(1)", ""})
            assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate(input), input);
    }
    @Test void rejectsDeepExpressionsAndLongInputs() {
        assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate("(".repeat(40) + "1" + ")".repeat(40)));
        assertThrows(IllegalArgumentException.class, () -> Calculator.evaluate("1".repeat(129)));
    }
}
