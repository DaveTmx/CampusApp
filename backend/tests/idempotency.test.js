// Challenge 2. Run: npx jest backend/tests/idempotency.test.js
const request = require('supertest');
const path = require('path');
const { randomUUID } = require('crypto');
const pool = require(path.join(__dirname, '../../src/db/pool'));

const express = require('express');
const cors = require('cors');
const operationsRoutes = require(path.join(__dirname, '../../src/routes/operationsRoutes'));
const app = express();
app.use(cors());
app.use(express.json());
app.use('/api/operations', operationsRoutes);

const jwt = require('jsonwebtoken');
const { JWT_SECRET } = require(path.join(__dirname, '../../src/middleware/auth'));
const lecturerToken = jwt.sign({ id: 1, email: 'lecturer@test', role: 'lecturer' }, JWT_SECRET);

describe('Challenge 2: response loss & operation ID idempotency', () => {
  const studentId = 'contender-a-0000-0000-0000-000000000001';
  const operationId = randomUUID();

  beforeAll(async () => {
    await pool.query('DELETE FROM operations');
    await pool.query('DELETE FROM students');
    await pool.query(
      `INSERT INTO students (student_id, student_name, student_number, programme_code, group_code, version)
       VALUES (?, 'Contender A', '900000001', 'CS', NULL, 1)`, [studentId]
    );
  });
  afterAll(async () => { await pool.end(); });

  test('original request applies the change (receipt committed with it)', async () => {
    const res = await request(app).post('/api/operations')
      .set('Authorization', `Bearer ${lecturerToken}`)
      .send({ operationId, opType: 'ASSIGN_GROUP', payload: { studentId, groupCode: 'G02' } });
    expect(res.status).toBe(200);
    expect(res.body.replayed).toBe(false);

    const [[row]] = await pool.query('SELECT group_code FROM students WHERE student_id = ?', [studentId]);
    expect(row.group_code).toBe('G02');
  });

  test('retry with the same operationId replays the receipt, does not reapply', async () => {
    // Simulates "the response disappeared and the client retries" --
    // and, because the receipt lives in MySQL rather than in the Node
    // process, this assertion holds even across a real server restart.
    const retry = await request(app).post('/api/operations')
      .set('Authorization', `Bearer ${lecturerToken}`)
      .send({ operationId, opType: 'ASSIGN_GROUP', payload: { studentId, groupCode: 'G02' } });

    expect(retry.status).toBe(200);
    expect(retry.body.replayed).toBe(true);

    const [rows] = await pool.query('SELECT * FROM operations WHERE operation_id = ?', [operationId]);
    expect(rows.length).toBe(1); // exactly one receipt, never duplicated
  });

  test('reusing the operationId with different content is rejected, not merged', async () => {
    const res = await request(app).post('/api/operations')
      .set('Authorization', `Bearer ${lecturerToken}`)
      .send({ operationId, opType: 'ASSIGN_GROUP', payload: { studentId, groupCode: 'G03' } }); // different group
    expect(res.status).toBe(409);
    expect(res.body.error).toBe('OPERATION_ID_REUSED_WITH_DIFFERENT_CONTENT');
  });
});
