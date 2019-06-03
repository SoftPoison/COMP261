import java.util.*;

/**
 * A new instance of HuffmanCoding is created for every run. The constructor is
 * passed the full text to be encoded or decoded, so this is a good place to
 * construct the tree. You should store this tree in a field and then use it in
 * the encode and decode methods.
 */
public class HuffmanCoding {

    private class Node implements Comparable<Node> {
        public Node left = null, right = null;
        public ArrayList<Character> code = new ArrayList<>();
        public char value;
        public int frequency;

        public Node(char value, int frequency) {
            this.value = value;
            this.frequency = frequency;
        }

        public int compareTo(Node o) {
            return frequency - o.frequency;
        }
    }

    private HashMap<Character, String> encodingTable = new HashMap<>();
    private HashMap<String, Character> decodingTable = new HashMap<>();

    /**
     * This would be a good place to compute and store the tree.
     */
    public HuffmanCoding(String text) {
        if (text.length() <= 1)
            return;

        //Create a frequency table
        HashMap<Character, Node> counts = new HashMap<>();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (counts.containsKey(c))
                counts.get(c).frequency++;
            else
                counts.put(c, new Node(c, 1));
        }

        //Build the frequency tree
        PriorityQueue<Node> queue = new PriorityQueue<>(counts.values());
        while (queue.size() > 1) {
            Node left = queue.poll();
            Node right = queue.poll();
            Node parent = new Node('\0', left.frequency + right.frequency);
            parent.left = left;
            parent.right = right;

            queue.offer(parent);
        }

        Node root = queue.poll();

        //Assign the codes to each node in the tree
        Stack<Node> nodeStack = new Stack<>();
        nodeStack.add(root);
        while (!nodeStack.empty()) {
            Node node = nodeStack.pop();

            if (node.value != '\0') {
                StringBuilder sb = new StringBuilder();
                node.code.forEach(sb::append);
                String code = sb.toString();

                encodingTable.put(node.value, code);
                decodingTable.put(code, node.value);
            }
            else {
                node.left.code.addAll(node.code);
                node.left.code.add('0');
                node.right.code.addAll(node.code);
                node.right.code.add('1');
            }

            if (node.left != null)
                nodeStack.add(node.left);

            if (node.right != null)
                nodeStack.add(node.right);
        }
    }

    /**
     * Take an input string, text, and encode it with the stored tree. Should
     * return the encoded text as a binary string, that is, a string containing
     * only 1 and 0.
     */
    public String encode(String text) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            sb.append(encodingTable.get(text.charAt(i)));
        }

        return sb.toString();
    }

    /**
     * Take encoded input as a binary string, decode it using the stored tree,
     * and return the decoded text as a text string.
     */
    public String decode(String encoded) {
        StringBuilder output = new StringBuilder();

        int i = 0;
        while (i < encoded.length()) {
            StringBuilder charBuilder = new StringBuilder();

            while (!decodingTable.containsKey(charBuilder.toString()))
                charBuilder.append(encoded.charAt(i++));

            output.append(decodingTable.get(charBuilder.toString()));
        }

        return output.toString();
    }

    /**
     * The getInformation method is here for your convenience, you don't need to
     * fill it in if you don't wan to. It is called on every run and its return
     * value is displayed on-screen. You could use this, for example, to print
     * out the encoding tree.
     */
    public String getInformation() {
        StringBuilder sb = new StringBuilder();

        encodingTable.forEach((c, s) -> {
            sb.append(c);
            sb.append(" -> ");
            sb.append(s);
            sb.append('\n');
        });

        return sb.toString();
    }
}
