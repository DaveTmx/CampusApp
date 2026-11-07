# Database diagram

The schema lives in `database/schema.sql`. This diagram shows the tables and their foreign keys.
Dashed relationships are logical links with no database constraint (see the notes below).

```mermaid
erDiagram
  PROGRAMMES {
    CHAR4 programme_code PK
    VARCHAR programme_name
  }
  LAB_GROUPS {
    CHAR3 group_code PK
    INT capacity
  }
  ACCOUNTS {
    INT id PK
    VARCHAR email UK
    VARCHAR password_hash
    VARCHAR full_name
    ENUM role "student | lecturer"
    TINYINT is_active
    INT failed_logins
    DATETIME locked_until
    DATETIME created_at
  }
  STUDENTS {
    CHAR36 student_id PK
    VARCHAR student_name
    CHAR9 student_number UK
    CHAR4 programme_code FK
    CHAR3 group_code FK "NULL = unassigned"
    INT account_id FK, UK "NULL = no login"
    INT version
    DATETIME deleted_at "soft delete"
  }
  EMAIL_VERIFICATION_TOKENS {
    CHAR64 token_hash PK "SHA-256 of the token"
    INT account_id FK
    DATETIME expires_at
    DATETIME used_at
    DATETIME created_at
  }
  DELETION_MARKERS {
    CHAR36 student_id PK
    DATETIME deleted_at
    INT deleted_by
  }
  OPERATIONS {
    CHAR36 operation_id PK
    INT account_id
    CHAR36 student_id
    VARCHAR op_type
    JSON payload_json "fingerprint"
    SMALLINT http_status
    ENUM status "APPLIED | FAILED"
    JSON result_json
    DATETIME created_at
  }
  STUDY_GROUPS {
    INT id PK
    VARCHAR name
    VARCHAR description
    VARCHAR course_code
    INT created_by
    DATETIME created_at
  }

  PROGRAMMES ||--o{ STUDENTS : "enrols"
  LAB_GROUPS ||--o{ STUDENTS : "holds (max 16)"
  ACCOUNTS |o--o| STUDENTS : "logs in as"
  ACCOUNTS ||--o{ EMAIL_VERIFICATION_TOKENS : "is verified by"
```

## Notes

- **`deletion_markers`** has no foreign key to `students`. A marker must outlive its student row
  for sync, so the link is logical only.
- **`operations`** has no foreign keys. The receipt must survive even if the account or student
  is later removed, and its `account_id` may refer to a test account.
- **`study_groups`** is separate from `lab_groups`. It holds campus life groups, not the lab
  groups G01–G14.
- **Student numbers** stay reserved after a delete, because `student_number` is never cleared.
- **Group capacity** (16) is not stored as a constraint. It is enforced in
  `assignToGroupTx` under `SELECT … FOR UPDATE` on the `lab_groups` row.
