import java.util.*;

public class SearchTrie<T> {
    private Map<Character, SearchTrie<T>> children;
    private List<T> list;

    public SearchTrie() {
        children = new HashMap<>();
        list = new ArrayList<>();
    }

    /**
     * Navigates through the trie and collates all of the children matching the given prefix
     *
     * @param prefix the prefix to match
     * @return all of the children matching the given prefix
     */
    public List<T> findAll(String prefix) {
        SearchTrie<T> currentTrie = this;
        List<T> result = new ArrayList<>();

        //Iterate through the characters of the prefix to navigate down the trie
        for (char c : prefix.toCharArray()) {
            //If nothing matches the prefix, return
            if (!currentTrie.children.containsKey(c))
                return result;

            currentTrie = currentTrie.children.get(c);
        }

        if (currentTrie.list.size() != 0) {
            result.addAll(currentTrie.list); //Don't return currentTrie.list because then it could be modified
            return result;
        }

        //Look for children using a depth-first search
        Stack<SearchTrie<T>> toCheck = new Stack<>();
        toCheck.push(currentTrie);

        while (!toCheck.isEmpty()) {
            currentTrie = toCheck.pop();
            currentTrie.children.values().forEach(toCheck::push);

            result.addAll(currentTrie.list);
        }

        return result;
    }

    /**
     * Inserts a child into the trie
     *
     * @param key   the key to index by
     * @param value the child to add
     */
    public void insert(String key, T value) {
        SearchTrie<T> currentTrie = this;

        for (char c : key.toCharArray())
            currentTrie = currentTrie.children.computeIfAbsent(c, v -> new SearchTrie<>());

        currentTrie.list.add(value);
    }
}
