import tools.jackson.dataformat.toon.*;
import java.io.*;

public class SimpleNestedTest {
    public static void main(String[] args) {
        try {
            String input = "user:\n  id: 123\n  name: Alice";
            System.out.println("Input:");
            System.out.println(input);
            System.out.println("\nTokens from lexer:");

            ToonLexer lexer = new ToonLexer(new StringReader(input));
            ToonToken token;
            int count = 0;
            while (count < 20 && (token = lexer.nextToken()) != ToonToken.EOF) {
                System.out.printf("  %2d. %-15s", ++count, token);
                if (token.isValue()) {
                    System.out.print(" = " + lexer.getTokenValue());
                }
                System.out.println();
            }
            System.out.println("  " + (++count) + ". EOF");

            System.out.println("\nEvents from parser:");
            ToonParser parser = new ToonParser(new StringReader(input));
            ToonParser.Event event;
            count = 0;
            while (count < 20 && (event = parser.nextEvent()) != ToonParser.Event.EOF) {
                System.out.printf("  %2d. %-20s", ++count, event);
                if (event == ToonParser.Event.FIELD_NAME || event == ToonParser.Event.VALUE_STRING) {
                    System.out.print(" (" + parser.getTextValue() + ")");
                } else if (event == ToonParser.Event.VALUE_NUMBER_INT) {
                    System.out.print(" (" + parser.getNumberValue() + ")");
                }
                System.out.println();
            }

            if (count >= 20) {
                System.err.println("\nERROR: Too many events!");
                System.exit(1);
            }

            System.out.println("\n✓ Test completed successfully!");

        } catch (Exception e) {
            System.err.println("\nERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
