package com.example.campusapp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.campusapp.util.TimetableCsv;
import com.example.campusapp.util.VenueIndex;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/** The starting timetables match the four programme timetables from EduRole. */
public class TimetableTest {

    private static List<TimetableEntry> all(String programme) {
        List<TimetableEntry> out = new ArrayList<>();
        long id = 1;
        for (int day = 0; day < Timetable.DAYS.length; day++) {
            for (ClassSession s : Timetable.forProgram(programme, day)) {
                out.add(new TimetableEntry(id++, programme, day, s.name, s.time, s.room, s.type));
            }
        }
        return out;
    }

    @Test
    public void everyProgrammeHasItsOwnTimetable() {
        assertEquals(8, all("CS").size());
        assertEquals(7, all("IT").size());
        assertEquals(8, all("DS").size());
        assertEquals(9, all("CYB").size());
        assertTrue(all("XX").isEmpty());
    }

    @Test
    public void programmeCodesMatchTheRegistrationList() {
        for (String code : Timetable.PROGRAMS) {
            assertFalse(Programs.toLabel(code).equals(code)); // every code has a programme name
        }
        assertEquals(Programs.CODES.length, Timetable.PROGRAMS.length);
    }

    @Test
    public void samplesFromEachPdf() {
        // IT, Monday: BMG241 11:00-13:00 in Chalabesa hall.
        ClassSession bmg = Timetable.forProgram("IT", 0).get(0);
        assertEquals("BMG241 (Mwaanga C.)", bmg.name);
        assertEquals("11:00 - 13:00", bmg.time);
        assertEquals(Timetable.CHALABESA, bmg.room);

        // Data Science has no Friday classes; Tuesday ends with ICD331 15:00-17:00 in F 60.
        assertTrue(Timetable.forProgram("DS", 4).isEmpty());
        ClassSession icd331 = Timetable.forProgram("DS", 1).get(1);
        assertEquals("15:00 - 17:00", icd331.time);
        assertEquals(Timetable.F60, icd331.room);

        // Cyber Security, Wednesday: ICS331 07:00-09:00 in F 60, then ICS351 in Computer Room 1.
        List<ClassSession> cybWed = Timetable.forProgram("CYB", 2);
        assertEquals("ICS331 (Kalunga J.)", cybWed.get(0).name);
        assertEquals("07:00 - 09:00", cybWed.get(0).time);
        assertEquals(Timetable.ICT_LAB_1, cybWed.get(1).room);

        // Computer Science, Friday 15:00: ICT351 in SSS Block-A Rm 2.
        assertEquals(Timetable.SSS_A2, Timetable.forProgram("CS", 4).get(2).room);
    }

    @Test
    public void allTimesAreReadable() {
        for (String code : Timetable.PROGRAMS) {
            for (TimetableEntry e : all(code)) {
                assertTrue(code + " " + e.name + " " + e.time, TimetableCsv.isValidTime(e.time));
            }
        }
    }

    @Test
    public void venuesGroupClassesHeldInTheSameRoom() {
        List<VenueIndex.Venue> venues = VenueIndex.build(all("CYB"), false);
        VenueIndex.Venue lab2 = null;
        for (VenueIndex.Venue v : venues) {
            if (v.name.equals(Timetable.ICT_LAB_2)) lab2 = v;
        }
        assertTrue(lab2 != null);
        assertEquals(4, lab2.classes.size()); // ICS321 Mon + Fri, ICS331 Mon, ICS351 Thu
        assertEquals("ICS321 (Bwalya M.) · Mon 11:00 - 13:00", lab2.classes.get(0));
        assertTrue(lab2.matches("ics351"));
        assertFalse(lab2.matches("ICT361"));
    }

    @Test
    public void venueSpellingsAreTreatedAsOnePlace() {
        assertTrue(VenueIndex.sameVenue("ICT Center Lab ( Computer Room 2)", "ICT Centre Lab (Computer Room 2)"));
        assertTrue(VenueIndex.sameVenue("NEW DINNING HALL", "New Dinning Hall"));
        assertFalse(VenueIndex.sameVenue("F 60", "F 62"));
    }

    @Test
    public void lecturerViewLabelsTheProgramme() {
        List<TimetableEntry> both = new ArrayList<>(all("IT"));
        both.addAll(all("CS"));
        List<VenueIndex.Venue> venues = VenueIndex.build(both, true);
        // ICT361 on Monday is the same class for both programmes: one line, both codes.
        assertEquals("IT, CS · ICT361 (Nyirenda E.) · Mon 15:00 - 17:00", venues.get(1).classes.get(0));
    }
}
