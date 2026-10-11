// Challenge 3. Run: npx jest backend/tests/conflict.test.js
const request = require('supertest');
const path = require('path');
const pool = require(path.join(__dirname, '../../src/db/pool'));

const express = require('express');
const cors = require('cors');
const studentRoutes = require(path.join(__dirname, '../../src/routes/studentRoutes'));
const app = express();
app.use(cors());
app.use(express.json());
app.use('/api/students', studentRoutes);

const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require(path.join(__dirname, '../../src/middleware/auth'));
const lecturerToken = jwt.sign({ id: 1, email: 'lecturer@test', role: 'lecturer' }, JWT_SECRET);
const auth = { Authorization: `Bearer ${lecturerToken}` };

describe('Challenge 3: offline stale conflict resolution & soft deletes', () => {
  afterAll(async () => { await pool.end(); });

  test('scenario A: two devices cache the same student; stale edit is rejected', async () => {
    await pool.query('DELETE FROM students');
    await pool.query(
      `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
       VALUES ('student-a', 'Original Name', '900000003', 'CS', 'G01', 1)`
    );

    const device1 = await request(app).put('/api/students/student-a').set(auth)
      .send({ baseVersion: 1, studentName: 'Edited by Device 1' });
    expect(device1.status).toBe(200);

    const device2 = await request(app).put('/api/students/student-a').set(auth)
      .send({ baseVersion: 1, studentName: 'Edited by Device 2 (stale)' }); // same cached version as device1 started from
    expect(device2.status).toBe(409);
    expect(device2.body.error).toBe('CONFLICT');
    expect(device2.body.currentRecord.studentName).toBe('Edited by Device 1');
  });

  test('scenario B: group fills up while a device is offline', async () => {
    await pool.query('DELETE FROM students');
    for (let i = 0; i < 15; i++) {
      await pool.query(
        `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
         VALUES (?, ?, ?, 'CS', 'G02', 1)`, [`fill-${i}`, `Fill ${i}`, String(910000000 + i)]
      );
    }
    await pool.query(
      `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
       VALUES ('student-b', 'Student B', '900000004', 'IT', NULL, 1)`
    );

    const res = await request(app).post('/api/students/student-b/assign').set(auth).send({ groupCode: 'G02' });
    expect(res.status).toBe(409);
    expect(res.body.error).toBe('GROUP_FULL');

    const [[row]] = await pool.query('SELECT group_code FROM students WHERE student_id = ?', ['student-b']);
    expect(row.group_code).toBeNull(); // kept its previous (unassigned) state, not left in limbo
  });

  test('scenario C: remote deletion vs. a stale offline edit, and repeated deletion', async () => {
    await pool.query('DELETE FROM students');
    await pool.query(
      `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
       VALUES ('student-c', 'Student C', '900000005', 'DS', 'G03', 1)`
    );

    const deletion = await request(app).delete('/api/students/student-c').set(auth);
    expect(deletion.status).toBe(200);

    const staleEdit = await request(app).put('/api/students/student-c').set(auth)
      .send({ baseVersion: 1, studentName: 'Should Not Resurrect' });
    expect(staleEdit.status).toBe(410);
    expect(staleEdit.body.error).toBe('STUDENT_DELETED');

    const repeat = await request(app).delete('/api/students/student-c').set(auth);
    expect(repeat.status).toBe(200);
    expect(repeat.body.alreadyDeleted).toBe(true); // idempotent: group place not released twice
  });
});
