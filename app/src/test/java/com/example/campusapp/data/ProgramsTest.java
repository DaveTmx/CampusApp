package com.example.campusapp.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Programme labels (shown on screen) and codes (sent to the server and used in rules). */
public class ProgramsTest {

    @Test
    public void cyberSecurityMapsToCYB() {
        assertEquals("CYB", Programs.toCode("Cyber Security"));
        assertEquals("Cyber Security", Programs.toLabel("CYB"));
    }

    @Test
    public void everyLabelRoundTripsThroughItsCode() {
        for (String label : Programs.ALL) {
            assertEquals(label, Programs.toLabel(Programs.toCode(label)));
        }
    }

    @Test
    public void labelsAndCodesAreInTheSameOrder() {
        assertEquals(Programs.ALL.length, Programs.CODES.length);
        for (int i = 0; i < Programs.ALL.length; i++) {
            assertEquals(Programs.CODES[i], Programs.toCode(Programs.ALL[i]));
        }
    }

    @Test
    public void allFourProgrammesArePresent() {
        assertEquals("CS", Programs.toCode("Computer Science"));
        assertEquals("IT", Programs.toCode("Information Technology"));
        assertEquals("DS", Programs.toCode("Data Science"));
        assertEquals("CYB", Programs.toCode("Cyber Security"));
    }

    @Test
    public void unknownLabelIsKeptUnchangedSoItIsNeverLost() {
        assertEquals("Nursing", Programs.toCode("Nursing"));
    }

    @Test
    public void unknownCodeIsKeptUnchanged() {
        assertEquals("XX", Programs.toLabel("XX"));
    }
}
