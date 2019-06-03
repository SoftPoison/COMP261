import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * A new instance of LempelZiv is created for every run.
 */
public class LempelZiv {
    private static final int WINDOW_SIZE = 20000;
    private static final Pattern TUPLE_PAT = Pattern.compile("(?=\\[\\d+\\|\\d+\\|[\\S\\s]])");

    /**
     * Take uncompressed input as a text string, compress it, and return it as a
     * text string.
     */
    public String compress(String input) {
        StringBuilder output = new StringBuilder();

        final int inputSize = input.length();
        int cursor = 0;

        //Benchmarking purposes only
//        long time = System.currentTimeMillis();

        while (cursor < inputSize) {
            int length = 0;
            int position = 0;
            int start = Math.max(cursor - WINDOW_SIZE, 0);

            String window = input.substring(start, cursor);

            int currentPos;
            while (cursor + length < inputSize - 1 && (currentPos = window.indexOf(input.substring(cursor, cursor + length + 1))) >= 0) {
                position = currentPos;
                length++;
            }

            if (length > 0) { //Match found
                output.append('[');
                output.append(window.length() - position);
                output.append('|');
                output.append(length);
                output.append('|');
                output.append(input.charAt(cursor + length));
                output.append(']');
            }
            else {
                output.append("[0|0|");
                output.append(input.charAt(cursor));
                output.append(']');
            }

            cursor += length + 1;
        }

//        System.out.printf("Took %dms\n", System.currentTimeMillis() - time);

        return output.toString();
    }

    /**
     * Take compressed input as a text string, decompress it, and return it as a
     * text string.
     */
    public String decompress(String compressed) {
        Scanner scanner = new Scanner(compressed);
        scanner.useDelimiter(TUPLE_PAT);

        StringBuilder output = new StringBuilder();

        while (scanner.hasNext()) {
            //Split the tuple up into its tokens
            String tuple = scanner.next();
            tuple = tuple.substring(1, tuple.length() - 1); //Remove brackets

            char c = tuple.charAt(tuple.length() - 1);
            tuple = tuple.substring(0, tuple.length() - 2);

            int sepPos = tuple.indexOf('|');

            int offset = Integer.parseInt(tuple.substring(0, sepPos));
            int length = Integer.parseInt(tuple.substring(sepPos + 1));

            for (int i = 0; i < length; i++) {
                output.append(output.charAt(output.length() - offset));
            }
            output.append(c);
        }

        return output.toString();
    }

    /**
     * The getInformation method is here for your convenience, you don't need to
     * fill it in if you don't want to. It is called on every run and its return
     * value is displayed on-screen. You can use this to print out any relevant
     * information from your compression.
     */
    public String getInformation() {
        return "";
    }
}
