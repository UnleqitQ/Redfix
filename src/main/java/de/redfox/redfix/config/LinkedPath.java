package de.redfox.redfix.config;

import java.util.function.Predicate;
import java.util.stream.Stream;

public class LinkedPath {
    private String key;
    private LinkedPath next;

    public LinkedPath(String key) {
        this.key = key;
    }

    public static LinkedPath translate(String path) {
        String[] elements = path.split("\\.");

        LinkedPath parent = new LinkedPath(elements[0]);
        LinkedPath current = parent;
        for (int i = 1; i < elements.length; i++) {
            current.next = new LinkedPath(elements[i]);
            current = current.next;
        }

        return parent;
    }

    public static String translate(LinkedPath path) {
        StringBuilder res = new StringBuilder();

        LinkedPath current = path;
        for (;;) {
            res.append(current.key + ".");
            LinkedPath next = current.next;
            if (next == null)
                break;

            current = next;
        }

        res.deleteCharAt(res.length() - 1);
        return res.toString();
    }
}
