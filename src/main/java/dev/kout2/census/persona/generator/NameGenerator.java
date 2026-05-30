package dev.kout2.census.persona.generator;

import dev.kout2.census.persona.Gender;
import net.minecraft.util.RandomSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Character-level order-2 Markov chain name generator.
 *
 * Trained at class-load time on small embedded corpora, it produces plausible
 * novel names rather than picking from a fixed list — so two villagers rarely
 * share a name, and names "feel" like they belong to the same culture without
 * being real-world specific.
 *
 * Phase 8 will move the corpora into data-driven JSON (data/census/names/) with
 * per-language phoneme tables; this in-code version keeps Phase 1 self-contained.
 */
public final class NameGenerator {
    private static final char START = '^';
    private static final char END = '$';
    private static final int MAX_LEN = 11;
    private static final int MIN_LEN = 3;

    private final Map<String, List<Character>> chain = new HashMap<>();

    private NameGenerator(List<String> corpus) {
        for (String raw : corpus) {
            train(raw.toLowerCase());
        }
    }

    private void train(String word) {
        // NB: lead with "" so this is string concatenation — `START + START`
        // alone would be char arithmetic (94 + 94 = 188), corrupting the keys.
        String padded = "" + START + START + word + END;
        for (int i = 2; i < padded.length(); i++) {
            String key = padded.substring(i - 2, i);
            chain.computeIfAbsent(key, k -> new ArrayList<>()).add(padded.charAt(i));
        }
    }

    /** Generates one name, retrying until it lands within length bounds. */
    public String generate(RandomSource random) {
        for (int attempt = 0; attempt < 24; attempt++) {
            String candidate = roll(random);
            if (candidate.length() >= MIN_LEN && candidate.length() <= MAX_LEN) {
                return capitalize(candidate);
            }
        }
        // Fallback: accept whatever the last roll produced, trimmed.
        return capitalize(roll(random));
    }

    private String roll(RandomSource random) {
        StringBuilder sb = new StringBuilder();
        String key = "" + START + START;
        for (int i = 0; i < MAX_LEN + 4; i++) {
            List<Character> next = chain.get(key);
            if (next == null || next.isEmpty()) {
                break;
            }
            char c = next.get(random.nextInt(next.size()));
            if (c == END) {
                break;
            }
            sb.append(c);
            key = "" + key.charAt(1) + c;
        }
        return sb.toString();
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return "Anon";
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // ---- trained instances -------------------------------------------------

    private static final NameGenerator MALE_GIVEN = new NameGenerator(List.of(
            "alden", "bram", "cedric", "doran", "edmund", "finnian", "gareth",
            "harlon", "ivor", "joren", "kestrel", "lucan", "marrek", "nolan",
            "orrin", "perrin", "quill", "roderic", "soren", "tolbert", "ulric",
            "varian", "wendel", "yorrick", "alaric", "branok", "corwin", "delwin"
    ));

    private static final NameGenerator FEMALE_GIVEN = new NameGenerator(List.of(
            "aria", "brenna", "cerys", "delia", "elara", "fenna", "gwyneth",
            "hazel", "isolde", "junia", "kara", "linnea", "maren", "norah",
            "odette", "phaedra", "rowena", "saira", "tamsin", "ulla", "vespera",
            "wyndra", "yara", "alessa", "briony", "callista", "dlinah", "elowen"
    ));

    private static final NameGenerator FAMILY = new NameGenerator(List.of(
            "ashdown", "blackwood", "carrow", "denholm", "evermere", "fenwick",
            "grimsby", "hollis", "ironwood", "kettering", "larkspur", "marsh",
            "northgate", "oakhart", "pendleton", "quarry", "ravenscar", "stonefield",
            "thornbury", "underhill", "vance", "westfall", "wycombe", "yarrow",
            "amberly", "brightwater", "coldspring", "dunmore"
    ));

    public static String givenName(RandomSource random, Gender gender) {
        return (gender == Gender.FEMALE ? FEMALE_GIVEN : MALE_GIVEN).generate(random);
    }

    public static String familyName(RandomSource random) {
        return FAMILY.generate(random);
    }
}
