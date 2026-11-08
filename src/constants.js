'use strict';

// Single source of truth for values shared by controllers, routes and the schema.
// If you change these, change the matching ENUM / INSERT rows in database/schema.sql.

const PROGRAMMES = Object.freeze(['CS', 'IT', 'DS', 'CYB']);
const ROLES = Object.freeze({ STUDENT: 'student', LECTURER: 'lecturer' });
const LOCKOUT = Object.freeze({ MAX_FAILED: 5, MINUTES: 15 });

