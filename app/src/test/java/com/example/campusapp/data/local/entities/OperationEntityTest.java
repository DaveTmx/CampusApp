package com.example.campusapp.data.local.entities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OperationEntityTest {

    @Test
    public void newOperationStartsPendingWithItsIds() {
        OperationEntity op = OperationEntity.create("op-1", OperationEntity.ASSIGN_GROUP, "s-1", "G02");

        assertEquals("op-1", op.operationId);
        assertEquals(OperationEntity.ASSIGN_GROUP, op.opType);
        assertEquals("s-1", op.studentId);
        assertEquals("G02", op.groupCode);
        assertEquals(OperationEntity.PENDING, op.status);
        assertNull(op.errorCode);
        assertTrue(op.createdAt > 0);
    }

    @Test
    public void deleteCarriesNoGroup() {
        OperationEntity op = OperationEntity.create("op-2", OperationEntity.DELETE_STUDENT, "s-1", null);

        assertNull(op.groupCode);
        assertEquals(OperationEntity.DELETE_STUDENT, op.opType);
    }
}
