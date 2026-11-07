package com.example.campusapp.data.remote;

import java.util.List;

/**
 * JSON shapes for the lecturer student endpoints (/api/students, /api/groups).
 * Single records are camelCase, as in docs/api-contract.md. List items are snake_case (StudentPageDto).
 */
public final class StudentDtos {

    private StudentDtos() {
    }

    /** One student record, as the server returns it. */
    public static class RecordDto {
        public String studentId;
        public String studentNumber;
        public String studentName;
        public String programme;
        public String groupCode;
        public int version;
    }

    /** GET /api/students/:id (200). */
    public static class RecordEnvelope {
        public boolean success;
        public RecordDto student;
    }

    /** POST /api/students (201). */
    public static class CreateResponse {
        public boolean success;
        public RecordDto student;
    }

    /** Any error reply. currentRecord is present on 409 CONFLICT. */
    public static class ErrorBody {
        public String error;
        public String message;
        public RecordDto currentRecord;
    }

    /** POST /api/students. A group is never sent here; it is assigned afterwards. */
    public static class CreateRequest {
        public final String studentName;
        public final String studentNumber;
        public final String programmeCode;

        public CreateRequest(String studentName, String studentNumber, String programmeCode) {
            this.studentName = studentName;
            this.studentNumber = studentNumber;
            this.programmeCode = programmeCode;
        }
    }

    /** PUT /api/students/:id. Only the fields that are sent are changed. */
    public static class EditRequest {
        public final int baseVersion;
        public final String studentName;
        public final String programmeCode;
        public final String studentNumber;

        public EditRequest(int baseVersion, String studentName, String programmeCode, String studentNumber) {
            this.baseVersion = baseVersion;
            this.studentName = studentName;
            this.programmeCode = programmeCode;
            this.studentNumber = studentNumber;
        }
    }

    /** PUT /api/students/:id (200). */
    public static class EditResponse {
        public boolean success;
        public int version;
    }

    /** POST /api/students/:id/assign. */
    public static class AssignRequest {
        public final String groupCode;

        public AssignRequest(String groupCode) {
            this.groupCode = groupCode;
        }
    }

    /** POST /api/students/:id/assign (200). */
    public static class AssignResponse {
        public boolean success;
        public String groupCode;
    }

    /** DELETE /api/students/:id (200). */
    public static class DeleteResponse {
        public boolean success;
        public boolean alreadyDeleted;
        public String releasedGroup;
    }

    /** GET /api/groups. */
    public static class GroupsResponse {
        public boolean success;
        public List<GroupRow> data;
    }

    public static class GroupRow {
        public String group_code;
        public int capacity;
        public int occupied;
    }

    /** The data inside an operation. groupCode is left out (null) for DELETE_STUDENT and UNASSIGN_GROUP. */
    public static class Payload {
        public final String studentId;
        public final String groupCode;

        public Payload(String studentId, String groupCode) {
            this.studentId = studentId;
            this.groupCode = groupCode;
        }
    }

    /** POST /api/operations. */
    public static class OperationRequest {
        public final String operationId;
        public final String opType;
        public final Payload payload;

        public OperationRequest(String operationId, String opType, Payload payload) {
            this.operationId = operationId;
            this.opType = opType;
            this.payload = payload;
        }
    }

    /** POST /api/operations reply. error is set when the server refused the operation. */
    public static class OperationResponse {
        public boolean success;
        public String status;
        public boolean replayed;
        public String operationId;
        public String error;
        public String message;
    }
}
