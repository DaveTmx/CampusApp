package com.example.campusapp.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ResultTest {

    @Test
    public void successAndQueuedAreBothSaved() {
        assertTrue(Result.of(Result.Status.SUCCESS, null).isSaved());
        assertTrue(Result.of(Result.Status.PENDING, null).isSaved());
    }

    @Test
    public void refusalsAreNotSaved() {
        assertFalse(Result.of(Result.Status.GROUP_FULL, null).isSaved());
        assertFalse(Result.of(Result.Status.OFFLINE, null).isSaved());
        assertFalse(Result.of(Result.Status.DUPLICATE_NUMBER, null).isSaved());
    }

    @Test
    public void groupIsFullAtCapacity() {
        assertFalse(new GroupCount("G01", GroupCount.CAPACITY - 1).isFull());
        assertTrue(new GroupCount("G01", GroupCount.CAPACITY).isFull());
    }
}
