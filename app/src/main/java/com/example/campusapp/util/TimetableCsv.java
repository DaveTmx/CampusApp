package com.example.campusapp.util;

import com.example.campusapp.data.Timetable;
import com.example.campusapp.data.TimetableEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Reads a timetable from CSV text. One class per line:
 *
 *   day,name,time,room,type
 *   Mon,Database Systems,09:00 - 10:30,Room 102,Lecture
 *
 * day is Mon to Fri (or the full name). type is Lecture, Practical or Tutorial and may be left empty.
 * A header line is allowed. Nothing is accepted unless every line is valid.
 */
public final class TimetableCsv {

    public static final String[] TYPES = {"Lecture", "Practical", "Tutorial"};

    private static final Pattern TIME = Pattern.compile("[0-9]{1,2}:[0-9]{2}\\s*-\\s*[0-9]{1,2}:[0-9]{2}");

    private TimetableCsv() {
    }

    /** Thrown for a bad line; the message names the line so the lecturer can fix the file. */
    public static class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }
    }

    public static boolean isValidTime(String time) {
        return time != null && TIME.matcher(time.trim()).matches();
    }

    /** The day index for "Mon", "monday", "MON" and so on, or -1. */
    public static int dayIndex(String text) {
        if (text == null || text.trim().length() < 3) {
            return -1;
        }
        String key = text.trim().toLowerCase(Locale.ROOT).substring(0, 3);
        for (int i = 0; i < Timetable.DAYS.length; i++) {
            if (Timetable.DAYS[i].toLowerCase(Locale.ROOT).equals(key)) {
                return i;
            }
        }
        return -1;
    }

    /** "lecture" -> "Lecture"; empty -> "Lecture"; anything else -> null. */
    public static String normaliseType(String text) {
        String t = text == null ? "" : text.trim();
        if (t.isEmpty()) {
            return TYPES[0];
        }
        for (String type : TYPES) {
            if (type.equalsIgnoreCase(t)) {
                return type;
            }
        }
        return null;
    }

    public static List<TimetableEntry> parse(String csv) throws ParseException {
        List<TimetableEntry> entries = new ArrayList<>();
        if (csv == null) {
            throw new ParseException("The file is empty.");
        }
        if (csv.startsWith("\uFEFF")) {
            csv = csv.substring(1); // byte order mark that Excel adds
        }
        String[] lines = csv.split("\\r?\\n");
        long id = 1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            List<String> f = splitLine(line);
            if (entries.isEmpty() && id == 1 && "day".equalsIgnoreCase(f.get(0).trim())) {
                continue; // header
            }
            int lineNo = i + 1;
            if (f.size() < 4) {
                throw new ParseException("Line " + lineNo + ": expected day,name,time,room,type.");
            }
            int day = dayIndex(f.get(0));
            if (day < 0) {
                throw new ParseException("Line " + lineNo + ": day must be Mon, Tue, Wed, Thu or Fri.");
            }
            String name = f.get(1).trim();
            if (name.isEmpty()) {
                throw new ParseException("Line " + lineNo + ": the class name is empty.");
            }
            String time = f.get(2).trim();
            if (!isValidTime(time)) {
                throw new ParseException("Line " + lineNo + ": time must look like 09:00 - 10:30.");
            }
            String room = f.get(3).trim();
            if (room.isEmpty()) {
                throw new ParseException("Line " + lineNo + ": the room is empty.");
            }
            String type = normaliseType(f.size() > 4 ? f.get(4) : "");
            if (type == null) {
                throw new ParseException("Line " + lineNo + ": type must be Lecture, Practical or Tutorial.");
            }
            entries.add(new TimetableEntry(id++, day, name, time.replaceAll("\\s*-\\s*", " - "), room, type));
        }
        if (entries.isEmpty()) {
            throw new ParseException("No classes found in the file.");
        }
        return entries;
    }

    /** Splits one line on commas, honouring "quoted, fields". */
    private static List<String> splitLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cur.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
