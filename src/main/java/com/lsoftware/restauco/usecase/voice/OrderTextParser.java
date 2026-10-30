package com.lsoftware.restauco.usecase.voice;

import com.lsoftware.restauco.entity.menu.model.Dish;
import com.lsoftware.restauco.entity.menu.model.DishIngredient;
import com.lsoftware.restauco.entity.menu.model.ModifierGroup;
import com.lsoftware.restauco.entity.menu.model.ModifierOption;
import com.lsoftware.restauco.entity.voice.model.VoiceIssue;
import com.lsoftware.restauco.entity.voice.model.VoiceLine;
import com.lsoftware.restauco.entity.voice.model.VoiceOrder;

import java.text.Normalizer;
import java.util.*;

/**
 * Turns what a waiter dictated ("mesa 3, dos pelúas, una sifrina sin queso y una arepa de
 * chicharrón y pollo") into lines of the menu. Rules, not a model: it only knows the words of the
 * menu itself, so it works for any restaurant without training and answers instantly.
 *
 * <ul>
 *   <li>A quantity ("dos", "2", "otra") starts a new item; "dos rellenos" does not.</li>
 *   <li>A dish is recognized by the words only its name has ("pelúa", "jugo"), tolerating plurals
 *       and one wrong letter ("peluda").</li>
 *   <li>Options are recognized the same way within their group ("reina pepiada", "codorniz").
 *       Options without a dish pick the dish whose group takes that many ("arepa de chicharrón y
 *       pollo" is the arepa with 2 rellenos).</li>
 *   <li>"sin X" removes a removable ingredient; anything else left over becomes the note
 *       ("jugo de mora" -> note "mora").</li>
 * </ul>
 * What it is not sure about comes back as an issue, never as a guess the kitchen would cook.
 */
public final class OrderTextParser {

    private static final int MAX_QTY = 20;

    private static final Map<String, Integer> NUMBERS = Map.ofEntries(
            Map.entry("un", 1), Map.entry("una", 1), Map.entry("uno", 1),
            Map.entry("otra", 1), Map.entry("otro", 1),
            Map.entry("dos", 2), Map.entry("tres", 3), Map.entry("cuatro", 4), Map.entry("cinco", 5),
            Map.entry("seis", 6), Map.entry("siete", 7), Map.entry("ocho", 8), Map.entry("nueve", 9),
            Map.entry("diez", 10));

    /** Words that never name a dish or option, and are dropped from notes. */
    private static final Set<String> FILLER = Set.of(
            "y", "e", "de", "del", "con", "la", "el", "los", "las", "al", "a", "en", "para", "por", "favor",
            "porfa", "porfavor", "tambien", "mas", "ademas", "ni", "que", "sea", "me", "das", "regalas",
            "trae", "traeme", "quiero", "dame", "pon", "ponme", "otra", "otro", "un", "una", "uno",
            "arepa", "arepas", "relleno", "rellenos", "mesa", "pedido", "porcion");

    /** Words that end a "sin ..." list. */
    private static final Set<String> SIN_STOP = Set.of("con", "sin", "pero", "para");

    private final List<Dish> dishes;
    /** word -> dish, for words only one dish name has */
    private final Map<String, Dish> dishWords = new HashMap<>();
    /** word -> option id, for words only one option of its group has */
    private final Map<String, String> optionWords = new HashMap<>();
    /** every word the menu uses, so a known word is never "corrected" into another */
    private final Set<String> vocabulary = new HashSet<>();

    public OrderTextParser(List<Dish> menu) {
        this.dishes = menu.stream().filter(Dish::active).toList();
        indexDishes();
        indexOptions();
    }

    public VoiceOrder parse(String text) {
        List<Word> words = tokenize(text);
        Integer table = takeTable(words);
        List<VoiceLine> lines = new ArrayList<>();
        List<VoiceIssue> issues = new ArrayList<>();
        for (List<Word> segment : segments(words)) {
            for (Item item : items(segment)) {
                item.resolve(lines, issues);
            }
        }
        return new VoiceOrder(text, table, lines, issues);
    }

    // ---- reading the menu ------------------------------------------------------------------

    private void indexDishes() {
        Map<String, Set<Dish>> owners = new HashMap<>();
        for (Dish d : dishes) {
            for (String w : contentWords(d.name())) owners.computeIfAbsent(w, k -> new HashSet<>()).add(d);
            d.ingredients().forEach(i -> vocabulary.addAll(contentWords(i.name())));
        }
        vocabulary.addAll(owners.keySet());
        owners.forEach((w, ds) -> {
            if (ds.size() == 1) dishWords.put(w, ds.iterator().next());
        });
    }

    private void indexOptions() {
        Map<String, Set<String>> owners = new HashMap<>();
        for (Dish d : dishes) {
            for (ModifierGroup g : d.modifiers()) {
                Map<String, Set<String>> inGroup = new HashMap<>();
                for (ModifierOption o : g.options()) {
                    Set<String> ws = new LinkedHashSet<>(contentWords(o.name()));
                    vocabulary.addAll(ws);
                    ws.forEach(w -> inGroup.computeIfAbsent(w, k -> new HashSet<>()).add(o.id()));
                    // The id is usually the short name people say ("cerdo" for "Salteado de cerdo")
                    for (String w : contentWords(o.id().replace('-', ' ').replace('_', ' '))) {
                        owners.computeIfAbsent(w, k -> new HashSet<>()).add(o.id());
                        vocabulary.add(w);
                    }
                }
                inGroup.forEach((w, ids) -> {
                    if (ids.size() == 1) owners.computeIfAbsent(w, k -> new HashSet<>()).addAll(ids);
                });
            }
        }
        owners.forEach((w, ids) -> {
            if (ids.size() == 1) optionWords.put(w, ids.iterator().next());
        });
    }

    // ---- splitting what was said -----------------------------------------------------------

    private record Word(String text, String norm) {
        boolean is(String s) {
            return norm.equals(s);
        }
    }

    private static List<Word> tokenize(String text) {
        List<Word> words = new ArrayList<>();
        String spaced = text.replaceAll("[.,;:!?¿¡()]", " , ");
        for (String raw : spaced.trim().split("\\s+")) {
            if (raw.equals(",")) {
                words.add(new Word(",", ","));
                continue;
            }
            String norm = fold(raw);
            if (!norm.isEmpty()) words.add(new Word(raw, norm));
        }
        return words;
    }

    /** "mesa 3" / "mesa tres": takes the table number out of the words. */
    private static Integer takeTable(List<Word> words) {
        for (int i = 0; i + 1 < words.size(); i++) {
            if (!words.get(i).is("mesa")) continue;
            Integer n = number(words.get(i + 1).norm());
            if (n == null) continue;
            words.remove(i + 1);
            words.remove(i);
            return n;
        }
        return null;
    }

    /** A new segment starts at each quantity, unless it counts the fillings ("dos rellenos"). */
    private static List<List<Word>> segments(List<Word> words) {
        List<List<Word>> out = new ArrayList<>();
        List<Word> current = new ArrayList<>();
        for (int i = 0; i < words.size(); i++) {
            Word w = words.get(i);
            boolean startsItem = number(w.norm()) != null && !countsFillings(words, i);
            if (startsItem) {
                // "para la mesa 3, dos pelúas": words before the first quantity that say nothing are dropped
                if (hasContent(current)) out.add(current);
                current = new ArrayList<>();
            }
            current.add(w);
        }
        if (hasContent(current)) out.add(current);
        return out;
    }

    private static boolean countsFillings(List<Word> words, int i) {
        return i + 1 < words.size() && words.get(i + 1).norm().startsWith("relleno");
    }

    private static boolean hasContent(List<Word> ws) {
        return ws.stream().anyMatch(w -> !w.is(",") && !FILLER.contains(w.norm()));
    }

    /** One segment may still name several dishes ("una sifrina y una catira" was split, "sifrina y catira" not). */
    private List<Item> items(List<Word> segment) {
        List<Item> items = new ArrayList<>();
        Item current = new Item();
        int qty = 1;
        int start = 0;
        if (!segment.isEmpty() && number(segment.getFirst().norm()) != null && !countsFillings(segment, 0)) {
            qty = number(segment.getFirst().norm());
            start = 1;
        }
        current.qty = qty;
        boolean inSin = false;
        for (int i = start; i < segment.size(); i++) {
            Word w = segment.get(i);
            Dish dish = dishFor(w.norm());
            if (dish != null) inSin = false;
            if (dish != null && current.dish != null && current.dish != dish) {
                items.add(current);
                current = new Item();
                current.qty = 1;
            }
            if (w.is(",")) {
                inSin = false;
                continue;
            }
            if (w.is("sin")) {
                inSin = true;
                current.sin.add(new ArrayList<>());
                continue;
            }
            if (inSin && (SIN_STOP.contains(w.norm()) || number(w.norm()) != null)) inSin = false;
            if (inSin) {
                current.sin.getLast().add(w);
                continue;
            }
            current.heard.add(w);
            Integer n = number(w.norm());
            if (n != null && countsFillings(segment, i)) {
                current.fillingCount = n;
                continue;
            }
            if (dish != null) {
                current.dish = dish;
                continue;
            }
            String option = optionFor(w.norm());
            if (option != null) {
                if (!current.options.contains(option)) current.options.add(option);
                current.optionSaid.add(Map.entry(option, w));
                continue;
            }
            current.rest.add(w);
        }
        items.add(current);
        return items;
    }

    // ---- one item -------------------------------------------------------------------------

    private final class Item {
        int qty;
        Dish dish;
        Integer fillingCount;
        final List<String> options = new ArrayList<>();
        final List<List<Word>> sin = new ArrayList<>();
        final List<Word> heard = new ArrayList<>();
        final List<Word> rest = new ArrayList<>();
        /** each option word with the option it meant, to keep the words of options the dish has not */
        final List<Map.Entry<String, Word>> optionSaid = new ArrayList<>();

        void resolve(List<VoiceLine> lines, List<VoiceIssue> issues) {
            String said = said();
            Dish target = dish != null ? dish : dishForOptions();
            if (target == null) {
                if (!options.isEmpty() || fillingCount != null) {
                    Dish guess = closestForOptions();
                    issues.add(new VoiceIssue("NEEDS_CHOICES", guess != null ? guess.id() : null,
                            guess != null ? guess.name() : null, said));
                } else if (hasContent(heard) || !sin.isEmpty()) {
                    issues.add(new VoiceIssue("NOT_UNDERSTOOD", null, null, said));
                }
                return;
            }
            if (!target.orderable()) {
                issues.add(new VoiceIssue("UNAVAILABLE", target.id(), target.name(), said));
                return;
            }
            Map<String, List<String>> selections = new LinkedHashMap<>();
            for (ModifierGroup g : target.modifiers()) {
                List<String> picked = options.stream()
                        .filter(id -> g.options().stream().anyMatch(o -> o.id().equals(id)))
                        .toList();
                // A filling that ran out: say which, so the waiter offers another
                Optional<ModifierOption> soldOut = g.options().stream()
                        .filter(o -> !o.available() && picked.contains(o.id())).findFirst();
                if (soldOut.isPresent()) {
                    issues.add(new VoiceIssue("UNAVAILABLE", target.id(), soldOut.get().name(), said));
                    return;
                }
                if (picked.size() < g.min() || picked.size() > g.max()) {
                    issues.add(new VoiceIssue("NEEDS_CHOICES", target.id(), target.name(), said));
                    return;
                }
                if (!picked.isEmpty()) selections.put(g.id(), picked);
            }
            List<Long> removed = new ArrayList<>();
            List<String> notes = new ArrayList<>();
            Set<String> used = new HashSet<>();
            selections.values().forEach(used::addAll);
            List<Word> extra = new ArrayList<>(rest);
            optionSaid.stream().filter(e -> !used.contains(e.getKey())).forEach(e -> extra.add(e.getValue()));
            note(extra).ifPresent(n -> notes.add(extra.size() == rest.size() ? n : "con " + n));
            for (List<Word> span : sin) {
                List<Word> unknown = new ArrayList<>();
                for (Word w : span) {
                    if (FILLER.contains(w.norm())) continue;
                    Long ingredient = removableFor(target, w.norm());
                    if (ingredient == null) unknown.add(w);
                    else if (!removed.contains(ingredient)) removed.add(ingredient);
                }
                note(unknown).ifPresent(n -> notes.add("sin " + n));
            }
            String note = notes.isEmpty() ? null : String.join(", ", notes);
            lines.add(new VoiceLine(target.id(), target.name(), Math.min(qty, MAX_QTY), removed, selections, note));
        }

        /** Options alone: the dish whose group takes them all, as many as were said. */
        private Dish dishForOptions() {
            if (options.isEmpty()) return null;
            int count = fillingCount != null ? fillingCount : options.size();
            if (count != options.size()) return null;
            return dishes.stream()
                    .filter(d -> takes(d, count))
                    .min(Comparator.comparingInt(d -> d.modifiers().getFirst().max()))
                    .orElse(null);
        }

        /** For "arepa de 4 rellenos" with fewer said: the dish to finish on screen. */
        private Dish closestForOptions() {
            if (fillingCount != null) {
                return dishes.stream().filter(d -> takesCount(d, fillingCount)).findFirst().orElse(null);
            }
            return null;
        }

        private boolean takes(Dish d, int count) {
            if (!takesCount(d, count)) return false;
            ModifierGroup g = d.modifiers().getFirst();
            return options.stream().allMatch(id -> g.options().stream().anyMatch(o -> o.id().equals(id)));
        }

        private boolean takesCount(Dish d, int count) {
            return d.modifiers().size() == 1 && d.modifiers().getFirst().min() <= count
                    && count <= d.modifiers().getFirst().max() && d.modifiers().getFirst().min() > 0;
        }

        private String said() {
            List<Word> all = new ArrayList<>(heard);
            sin.forEach(all::addAll);
            return String.join(" ", all.stream().map(Word::text).toList());
        }
    }

    private static Optional<String> note(List<Word> words) {
        int from = 0;
        int to = words.size();
        while (from < to && FILLER.contains(words.get(from).norm())) from++;
        while (to > from && FILLER.contains(words.get(to - 1).norm())) to--;
        if (from >= to) return Optional.empty();
        return Optional.of(String.join(" ", words.subList(from, to).stream().map(Word::text).toList()));
    }

    // ---- matching words --------------------------------------------------------------------

    private Dish dishFor(String word) {
        String key = match(word, dishWords.keySet());
        return key != null ? dishWords.get(key) : null;
    }

    private String optionFor(String word) {
        String key = match(word, optionWords.keySet());
        return key != null ? optionWords.get(key) : null;
    }

    private Long removableFor(Dish dish, String word) {
        for (DishIngredient i : dish.ingredients()) {
            if (!i.removable()) continue;
            Set<String> names = new HashSet<>(contentWords(i.name()));
            if (match(word, names) != null) return i.ingredientId();
        }
        return null;
    }

    /**
     * The key the word means: the same word, its singular, or one letter off (only for longer
     * words, and only when the word is not itself another word of the menu).
     */
    private String match(String word, Set<String> keys) {
        if (FILLER.contains(word) || word.length() < 3) return null;
        if (keys.contains(word)) return word;
        for (String singular : singulars(word)) {
            if (keys.contains(singular)) return singular;
        }
        if (vocabulary.contains(word) || word.length() < 5) return null;
        String best = null;
        for (String key : keys) {
            if (key.length() < 5) continue;
            int limit = Math.min(word.length(), key.length()) >= 9 ? 2 : 1;
            if (distance(word, key) <= limit) {
                if (best != null) return null; // two candidates: do not guess
                best = key;
            }
        }
        return best;
    }

    private static List<String> singulars(String word) {
        List<String> out = new ArrayList<>();
        if (word.endsWith("es") && word.length() > 4) out.add(word.substring(0, word.length() - 2));
        if (word.endsWith("s") && word.length() > 3) out.add(word.substring(0, word.length() - 1));
        return out;
    }

    private static List<String> contentWords(String text) {
        List<String> out = new ArrayList<>();
        for (String raw : text.split("[^\\p{L}\\p{N}]+")) {
            String w = fold(raw);
            if (w.length() >= 3 && !FILLER.contains(w) && number(w) == null) out.add(w);
        }
        return out;
    }

    private static Integer number(String norm) {
        if (norm.matches("\\d{1,2}")) return Integer.parseInt(norm);
        return NUMBERS.get(norm);
    }

    static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9ñ]", "");
    }

    private static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] t = prev;
            prev = cur;
            cur = t;
        }
        return prev[b.length()];
    }
}
