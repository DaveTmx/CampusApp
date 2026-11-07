package com.example.campusapp.util;

import com.example.campusapp.data.Timetable;
import com.example.campusapp.data.TimetableEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Groups timetable classes by the venue they are held in, for the campus map.
 * Spellings that differ only in case, spacing or "Center"/"Centre" count as one venue,
 * so a lecturer typing "new dinning hall" still lands on the same pin.
 */
public final class VenueIndex {

    /** One venue and what happens there. */
    public static final class Venue {
        public final String name;
        /** "ICT361 (Nyirenda E.) · Mon 15:00 - 17:00", in timetable order. */
        public final List<String> classes = new ArrayList<>();

        Venue(String name) {
            this.name = name;
        }

        /** True when the search text appears in the venue name or any of its classes. */
        public boolean matches(String query) {
            String q = normalise(query);
            if (q.isEmpty() || normalise(name).contains(q)) {
                return true;
            }
            for (String c : classes) {
                if (normalise(c).contains(q)) return true;
            }
            return false;
        }
    }

    private VenueIndex() {
    }

    /**
     * Venues in the order they first appear in the timetable (Monday first).
     * withProgramme starts each line with the programmes that share that class
     * ("CS, IT · ICT361 ..."), for the lecturer's all-programme view.
     */
    public static List<Venue> build(List<TimetableEntry> entries, boolean withProgramme) {
        Map<String, Venue> byKey = new LinkedHashMap<>();
        // venue key -> class line -> programmes attending it, in first-seen order
        Map<String, Map<String, List<String>>> programmes = new LinkedHashMap<>();
        for (TimetableEntry e : entries) {
            if (e.room == null || e.room.trim().isEmpty()) continue;
            String key = normalise(e.room);
            if (!byKey.containsKey(key)) {
                byKey.put(key, new Venue(e.room.trim()));
                programmes.put(key, new LinkedHashMap<>());
            }
            String line = e.name + " · " + Timetable.DAYS[e.day] + " " + e.time;
            List<String> codes = programmes.get(key).get(line);
            if (codes == null) {
                codes = new ArrayList<>();
                programmes.get(key).put(line, codes);
            }
            if (e.program != null && !e.program.isEmpty() && !codes.contains(e.program)) {
                codes.add(e.program);
            }
        }
        for (Map.Entry<String, Venue> v : byKey.entrySet()) {
            for (Map.Entry<String, List<String>> c : programmes.get(v.getKey()).entrySet()) {
                boolean label = withProgramme && !c.getValue().isEmpty();
                v.getValue().classes.add(label
                        ? joinCodes(c.getValue()) + " · " + c.getKey()
                        : c.getKey());
            }
        }
        return new ArrayList<>(byKey.values());
    }

    /** ["CS", "IT"] -> "CS, IT" (String.join needs Android 8; the app supports 7). */
    private static String joinCodes(List<String> codes) {
        StringBuilder out = new StringBuilder();
        for (String code : codes) {
            if (out.length() > 0) out.append(", ");
            out.append(code);
        }
        return out.toString();
    }

    /** True when two venue names mean the same place. */
    public static boolean sameVenue(String a, String b) {
        return normalise(a).equals(normalise(b));
    }

    /** "ICT Center Lab ( Computer Room 2)" -> "ict centre lab (computer room 2)". */
    static String normalise(String text) {
        if (text == null) return "";
        return text.toLowerCase(Locale.ROOT)
                .replace("center", "centre")
                .replaceAll("\\(\\s+", "(")
                .replaceAll("\\s+\\)", ")")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
