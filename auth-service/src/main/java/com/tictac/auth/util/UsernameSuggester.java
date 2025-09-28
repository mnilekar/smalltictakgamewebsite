package com.tictac.auth.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UsernameSuggester {

    public static List<String> suggest(String first, String last) {
        String f = clean(first);
        String l = clean(last);
        List<String> out = new ArrayList<>();
        if (f.isEmpty() && l.isEmpty()) return out;

        out.add(f + l);
        out.add(f + "." + l);
        out.add(f.charAt(0) + l);
        out.add(l + f.charAt(0));
        out.add(f + l + "123");
        out.add(f + "_" + l);
        return out.stream().distinct().limit(6).toList();
    }

    private static String clean(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s, Normalizer.Form.NFD);
        n = n.replaceAll("\\p{M}+", ""); // remove accents
        n = n.toLowerCase(Locale.ROOT);
        n = n.replaceAll("[^a-z0-9]", "");
        return n;
    }
}