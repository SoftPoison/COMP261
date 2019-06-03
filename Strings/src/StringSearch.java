/**
 * A new StringSearch instance is created for every substring search performed. Both the
 * pattern and the text are passed to the constructor and the search method. You
 * could, for example, use the constructor to create the match table and the
 * search method to perform the search itself.
 */
public class StringSearch {
    private static final int ALPHABET_SIZE = Character.MAX_VALUE + 1;

    /**
     * Constructs the jump table for KMP based on the given pattern
     */
    public static int[] buildKMPJumpTable(String pattern) {
        int[] table = new int[pattern.length()];

        //If the pattern is empty
        if (pattern.length() == 0)
            return table;

        //First index is always -1
        table[0] = -1;

        //Loop through the remaining characters
        for (int pos = 1, cursor = 0; pos < table.length; pos++, cursor++) {
            if (pattern.charAt(pos) == pattern.charAt(cursor)) {
                table[pos] = table[cursor];
            }
            else {
                table[pos] = cursor;
                cursor = table[cursor];

                while (cursor >= 0 && pattern.charAt(pos) != pattern.charAt(cursor))
                    cursor = table[cursor];
            }
        }

        return table;
    }

    /**
     * Searches the given text for the given pattern (with its jump table), returning the index of the first match
     */
    public static int searchKMP(String pattern, String text, int[] table) {
        final int pLen = pattern.length(), tLen = text.length();

        //Empty pattern
        if (table.length == 0)
            return 0;

        int pPos = 0; //Current position in pattern
        int tPos = 0; //Current position in text
        while (tPos + pPos < tLen) {
            if (pattern.charAt(pPos) == text.charAt(pPos + tPos)) { //Partial match
                pPos++;

                if (pPos == pLen) //Complete match
                    return tPos;
            }
            else if (table[pPos] == -1) { //Mismatch, no self overlap
                tPos += pPos + 1;
                pPos = 0;
            }
            else { //Mismatch, with self overlap
                tPos += pPos - table[pPos];
                pPos = table[pPos];
            }
        }

        //No match
        return -1;
    }

    /**
     * Searches the given text for the given pattern, returning the index of the first match
     */
    public static int searchKMP(String pattern, String text) {
        return searchKMP(pattern, text, buildKMPJumpTable(pattern));
    }

    /**
     * Creates a jump table based off of the mismatched character information
     */
    public static int[] buildBMCharTable(String pattern) {
        final int pLen = pattern.length();
        int[] table = new int[ALPHABET_SIZE];

        for (int i = 0; i < table.length; i++)
            table[i] = pLen;

        for (int i = 0; i < pLen - 1; i++)
            table[pattern.charAt(i)] = pLen - 1 - i;

        return table;
    }

    /**
     * Computes if pattern[pos, len) == pattern[0, len - pos)
     */
    private static boolean isPrefix(String pattern, int pos) {
        for (int i = pos, j = 0; i < pattern.length(); i++, j++)
            if (pattern.charAt(i) != pattern.charAt(j))
                return false;

        return true;
    }

    /**
     * Computes the number of characters that match sequentially, in reverse, between pattern[0, pos] and pattern[0, len)
     */
    private static int computeSuffixLength(String pattern, int pos) {
        int sLen = 0;

        for (int i = pos, j = pattern.length() - 1; i >= 0; i--, j--) {
            if (pattern.charAt(i) != pattern.charAt(j))
                break;

            sLen++;
        }

        return sLen;
    }

    /**
     * Builds a jump table based off of the position a mismatch occurs
     */
    public static int[] buildBMJumpTable(String pattern) {
        final int pLen = pattern.length();
        int[] table = new int[pLen];

        int lastPos = pLen;

        //Backwards prefix search
        for (int i = pLen; i > 0; i--) {
            if (isPrefix(pattern, i))
                lastPos = i;

            table[pLen - i] = lastPos - i + pLen;
        }

        for (int i = 0; i < pLen - 1; i++) {
            int sLen = computeSuffixLength(pattern, i);
            table[sLen] = pLen - 1 - i + sLen;
        }

        return table;
    }

    /**
     * Code based off:
     * https://en.wikipedia.org/wiki/Boyer%E2%80%93Moore_string-search_algorithm
     */
    public static int searchBM(String pattern, String text, int[] charTable, int[] jumpTable) {
        final int pLen = pattern.length();
        final int tLen = text.length();

        //Empty pattern
        if (pLen == 0)
            return 0;

        int tPos = pLen - 1;
        int pPos;

        while (tPos < tLen) {
            for (pPos = pLen - 1; pattern.charAt(pPos) == text.charAt(tPos); pPos--, tPos--)
                if (pPos == 0) //Complete match
                    return tPos;

            //Jump the largest, but safest, amount forwards
            tPos += Math.max(jumpTable[pLen - 1 - pPos], charTable[text.charAt(tPos)]);
        }

        return -1;
    }

    public static int searchBM(String pattern, String text) {
        //Empty pattern
        if (pattern.length() == 0)
            return 0;

        return searchBM(pattern, text, buildBMCharTable(pattern), buildBMJumpTable(pattern));
    }

    /**
     * Searches the given text for the given pattern (with its jump table), returning the index of the first match
     */
    public static int searchBruteforce(String pattern, String text) {
        final int plen = pattern.length(), tlen = text.length();

        //Empty pattern
        if (plen == 0)
            return 0;

        for (int i = 0; i < tlen - plen; i++) {
            for (int j = 0; j < plen; j++) {
                if (pattern.charAt(j) != text.charAt(i + j))
                    break;

                if (j == plen - 1)
                    return i;
            }
        }

        //No match
        return -1;
    }

    /**
     * Runs a comparison between KMP and brute force, printing the average time taken for each to run
     */
    public static void benchmark(String pattern, String text, int reps) {
        long bfTime = 0;
        for (int i = 0; i < reps; i++) {
            long start = System.nanoTime();
            searchBruteforce(pattern, text);
            bfTime += System.nanoTime() - start;
        }
        System.out.println("Brute force took " + bfTime / reps + "ns avg");

        int[] table = buildKMPJumpTable(pattern);
        long kmpTime = 0;
        for (int i = 0; i < reps; i++) {
            long start = System.nanoTime();
            searchKMP(pattern, text, table);
            kmpTime += System.nanoTime() - start;
        }
        System.out.println("KMP took " + kmpTime / reps + "ns avg");

        int[] charTable = buildBMCharTable(pattern);
        int[] jumpTable = buildBMJumpTable(pattern);
        long bmTime = 0;
        for (int i = 0; i < reps; i++) {
            long start = System.nanoTime();
            searchBM(pattern, text, charTable, jumpTable);
            bmTime += System.nanoTime() - start;
        }
        System.out.println("BM took " + bmTime / reps + "ns avg");
    }

}
