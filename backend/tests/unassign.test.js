// Run: npx jest --runInBand backend/tests/unassign.test.js
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

const { makeLecturerToken } = require('./helpers/lecturerToken');
let auth;
beforeAll(async () => {
  auth = { Authorization: `Bearer ${await makeLecturerToken()}` };
});

describe('unassign from a group', () => {
  afterAll(async () => { await pool.end(); });

  beforeEach(async () => {
    await pool.query('DELETE FROM students');
    await pool.query(
      `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
       VALUES ('unassign-a', 'Placed Student', '900000010', 'CS', 'G03', 1)`
    );
  });

  test('clears the group and bumps the version', async () => {
    const res = await request(app).post('/api/students/unassign-a/unassign').set(auth);
    expect(res.status).toBe(200);
    expect(res.body.groupCode).toBeNull();

    const [[row]] = await pool.query(
      'SELECT group_code, version FROM students WHERE student_id = ?', ['unassign-a']
    );
    expect(row.group_code).toBeNull();
    expect(row.version).toBe(2);
  });

  test('unassigning twice is harmless and does not bump the version again', async () => {
    await request(app).post('/api/students/unassign-a/unassign').set(auth);
    const again = await request(app).post('/api/students/unassign-a/unassign').set(auth);
    expect(again.status).toBe(200);

    const [[row]] = await pool.query('SELECT version FROM students WHERE student_id = ?', ['unassign-a']);
    expect(row.version).toBe(2);
  });

  test('unknown student gets 404', async () => {
    const res = await request(app).post('/api/students/no-such-student/unassign').set(auth);
    expect(res.status).toBe(404);
    expect(res.body.error).toBe('STUDENT_NOT_FOUND');
  });

  test('deleted student gets 410', async () => {
    await pool.query('UPDATE students SET deleted_at = NOW() WHERE student_id = ?', ['unassign-a']);
    const res = await request(app).post('/api/students/unassign-a/unassign').set(auth);
    expect(res.status).toBe(410);
    expect(res.body.error).toBe('STUDENT_DELETED');
  });
});
