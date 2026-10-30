package com.lsoftware.restauco.usecase.delivery;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Colombian addresses as people write them ("Cl. 5B # 36-20", "Kra 39 No 5-30"): the street
 * type and number come first, then the cross street and the meters from the corner.
 */
final class AddressText {

    private AddressText() {}

    private static final Map<String, String> TYPES = Map.ofEntries(
            Map.entry("cl", "calle"), Map.entry("cll", "calle"), Map.entry("cal", "calle"), Map.entry("calle", "calle"),
            Map.entry("cra", "carrera"), Map.entry("kra", "carrera"), Map.entry("kr", "carrera"), Map.entry("cr", "carrera"),
            Map.entry("crr", "carrera"), Map.entry("carrera", "carrera"),
            Map.entry("av", "avenida"), Map.entry("avda", "avenida"), Map.entry("avenida", "avenida"),
            Map.entry("dg", "diagonal"), Map.entry("diag", "diagonal"), Map.entry("diagonal", "diagonal"),
            Map.entry("tv", "transversal"), Map.entry("tr", "transversal"), Map.entry("transv", "transversal"),
            Map.entry("transversal", "transversal"));

    /** "No", "#": markers, not part of any name */
    private static final Set<String> MARKERS = Set.of("no", "n", "nro", "num", "numero");
    private static final Set<String> SMALL = Set.of("de", "del", "la", "las", "los", "el", "y");

    /** Lower case, no accents, street types spelled out, "5 b" joined: [calle, 5b, 36, 20]. */
    static List<String> tokens(String text) {
        String folded = Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("(\\d)([a-z])", "$1 $2").replaceAll("[^a-z0-9]+", " ").strip();
        List<String> out = new ArrayList<>();
        for (String word : folded.isEmpty() ? new String[0] : folded.split(" ")) {
            if (MARKERS.contains(word)) continue;
            // 5 b -> 5b, 5 bis -> 5bis
            if (!out.isEmpty() && word.matches("[a-z]|bis") && out.getLast().matches("\\d+")) {
                out.set(out.size() - 1, out.getLast() + word);
                continue;
            }
            out.add(TYPES.getOrDefault(word, word));
        }
        return out;
    }

    /** What to ask the map for: the written address with the street type spelled out. */
    static String forSearch(String address) {
        return String.join(" ", tokens(address));
    }

    /**
     * Whether the street the map found under the pin is the one the customer wrote. A numbered
     * street ("Calle 5B") needs its type and number; a named one ("Avenida Roosevelt") its words.
     */
    static boolean sameStreet(String written, String road) {
        if (road == null || written == null) return false;
        List<String> said = tokens(written);
        List<String> street = tokens(road);
        String number = street.stream().filter(t -> t.matches("\\d.*")).findFirst().orElse(null);
        if (number != null) {
            String type = TYPES.containsValue(street.getFirst()) ? street.getFirst() : null;
            int at = said.indexOf(number);
            // The number right after the type: "calle 5b" (not "carrera 36 # 5b-20")
            return at > 0 && (type == null || said.get(at - 1).equals(type));
        }
        List<String> words = street.stream().filter(t -> t.length() > 2 && !SMALL.contains(t) && !TYPES.containsValue(t)).toList();
        return !words.isEmpty() && said.containsAll(words);
    }
}
