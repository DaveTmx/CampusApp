'use strict';

// Single source of truth for values shared by controllers, routes and the schema.
// If you change these, change the matching ENUM / INSERT rows in database/schema.sql.

// Lab groups G01..G14, each holding at most GROUP_CAPACITY active students.
// NOTE: the ICT361 lab brief says 15. This project uses 16; keep that agreed with the lecturer.
// To change either number: change it here, in database/schema.sql (default and INSERT), in the
// migration files, in database/seed-test-fixture.sql (LIMIT = capacity - 1) and in the Android
// constant GroupCount.CAPACITY.
const GROUP_COUNT = 14;
const GROUP_CAPACITY = 16;
const GROUPS = Object.freeze(
  Array.from({ length: GROUP_COUNT }, (_, i) => 'G' + String(i + 1).padStart(2, '0'))
);
const PROGRAMMES = Object.freeze(['CS', 'IT', 'DS', 'CYB']);
const ROLES = Object.freeze({ STUDENT: 'student', LECTURER: 'lecturer' });
const LOCKOUT = Object.freeze({ MAX_FAILED: 5, MINUTES: 15 });

module.exports = { GROUPS, GROUP_COUNT, GROUP_CAPACITY, PROGRAMMES, ROLES, LOCKOUT };
