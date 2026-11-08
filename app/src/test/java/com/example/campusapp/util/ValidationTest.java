package com.example.campusapp.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.campusapp.R;

import org.junit.Test;

/** Field rules shared by the registration, lecturer editor and profile screens. */
public class ValidationTest {

    // ---- student numbers: exactly 9 digits ----

    @Test
    public void nineDigitNumbersAreAccepted() {
        assertNull(Validation.numberError("202419080"));
    }

    @Test
    public void leadingZeroIsKept() {
        // A string, never an int, so 012345678 is valid and stays 9 characters.
        assertNull(Validation.numberError("012345678"));
    }

    @Test
    public void surroundingSpacesAreIgnored() {
        assertNull(Validation.numberError("  202419080 "));
    }

    @Test
    public void eightDigitsAreRejected() {
        assertEquals(Integer.valueOf(R.string.error_number_invalid), Validation.numberError("20241908"));
    }

    @Test
    public void tenDigitsAreRejected() {
        assertEquals(Integer.valueOf(R.string.error_number_invalid), Validation.numberError("2024190801"));
    }

    @Test
    public void spaceInTheMiddleIsRejected() {
        assertEquals(Integer.valueOf(R.string.error_number_invalid), Validation.numberError("202 419080"));
    }

    @Test
    public void lettersAreRejected() {
        assertEquals(Integer.valueOf(R.string.error_number_invalid), Validation.numberError("20241908A"));
    }

    @Test
    public void digitsFromOtherScriptsAreRejected() {
        // Arabic-Indic digits: \d would accept these on Android, [0-9] must not.
        assertEquals(Integer.valueOf(R.string.error_number_invalid),
                Validation.numberError("١٢٣٤٥٦٧٨٩"));
    }

    @Test
    public void emptyAndNullAreRejected() {
        assertEquals(Integer.valueOf(R.string.error_number_invalid), Validation.numberError(""));
        assertEquals(Integer.valueOf(R.string.error_number_invalid), Validation.numberError(null));
    }

    // ---- programmes: CS, IT, DS, CYB ----

    @Test
    public void allFourProgrammeCodesAreAccepted() {
        for (String code : new String[] {"CS", "IT", "DS", "CYB"}) {
            assertNull(code, Validation.programmeError(code));
        }
    }

    @Test
    public void unknownProgrammeCodeIsRejected() {
        assertEquals(Integer.valueOf(R.string.error_programme_required), Validation.programmeError("XX"));
    }

    @Test
    public void programmeLabelIsNotAcceptedInPlaceOfCode() {
        // The server and the rules use codes. Labels are for display only.
        assertEquals(Integer.valueOf(R.string.error_programme_required),
                Validation.programmeError("Cyber Security"));
    }

    @Test
    public void missingProgrammeIsRejected() {
        assertEquals(Integer.valueOf(R.string.error_programme_required), Validation.programmeError(null));
    }

    // ---- groups: G01-G14, or blank for Unassigned ----

    @Test
    public void groupsG01ToG14AndBlankAreAccepted() {
        assertNull(Validation.groupError(null));
        assertNull(Validation.groupError(""));
        assertNull(Validation.groupError("G01"));
        assertNull(Validation.groupError("G05"));
        assertNull(Validation.groupError("G14"));
    }

    @Test
    public void groupOutsideG01ToG14IsRejected() {
        assertEquals(Integer.valueOf(R.string.error_group_invalid), Validation.groupError("G15"));
        assertEquals(Integer.valueOf(R.string.error_group_invalid), Validation.groupError("G00"));
    }

    // ---- password ----

    @Test
    public void passwordNeedsEightCharacters() {
        assertEquals(Integer.valueOf(R.string.error_password_short), Validation.passwordError("1234567"));
        assertNull(Validation.passwordError("12345678"));
    }
}
