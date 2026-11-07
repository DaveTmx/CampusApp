// Claim codes: a student whose profile a lecturer already entered proves ownership with a code.
// Run: npm test -- claim.test.js   (needs a MySQL database built from database/schema.sql;
// use a separate test database, never the real one)
require('dotenv').config();
const path = require('path');
const request = require('supertest');
const pool = require(path.join(__dirname, '../../src/db/pool'));
const app = require(path.join(__dirname, '../../server'));
const mailer = require(path.join(__dirname, '../../src/mailer'));

const PASSWORD = 'correct-horse-battery';
const PREFIX = 'claim-test-';
let counter = 0;

// Invented data only. Numbers start with 6000 so cleanup cannot touch other rows.
function next() {
  counter += 1;
  const n = String(600000000 + (Date.now() % 1000000) * 10 + counter).slice(0, 9);
  return { number: n, email: `${PREFIX}${Date.now()}-${counter}@example.test` };
}

async function seedProfile(code, { number = next().number, name = 'Seeded Student', deleted = false } = {}) {
  await pool.query(
    `INSERT INTO students (student_id, student_name, student_number, programme_code, claim_code_hash, deleted_at)
     VALUES (UUID(), ?, ?, 'IT', SHA2(?, 256), ${deleted ? 'NOW()' : 'NULL'})`,
    [name, number, code]
  );
  return number;
}

function register(fields) {
  return request(app).post('/api/auth/register').send({
    fullName: 'Claiming Student', password: PASSWORD, programmeCode: 'CS', ...fields,
  });
}

async function profileOf(number) {
  const [rows] = await pool.query('SELECT * FROM students WHERE student_number = ?', [number]);
  return rows;
}

async function accountCount(email) {
  const [[row]] = await pool.query('SELECT COUNT(*) AS n FROM accounts WHERE email = ?', [email]);
  return row.n;
}

beforeAll(async () => {
  await pool.query("DELETE FROM students WHERE student_number LIKE '6%' AND (student_name = 'Seeded Student' OR account_id IN (SELECT id FROM accounts WHERE email LIKE ?))", [`${PREFIX}%`]);
  await pool.query('DELETE FROM accounts WHERE email LIKE ?', [`${PREFIX}%`]);
});

afterAll(async () => {
  await pool.query("DELETE FROM students WHERE student_name = 'Seeded Student' OR account_id IN (SELECT id FROM accounts WHERE email LIKE ?)", [`${PREFIX}%`]);
  await pool.query('DELETE FROM accounts WHERE email LIKE ?', [`${PREFIX}%`]);
  await pool.end();
});

describe('claim codes at registration', () => {
  test('the right code links the account to the existing profile, with no second profile', async () => {
    const { email } = next();
    const number = await seedProfile('CLAIM-A1');
    const before = (await profileOf(number))[0];

    const res = await register({ email, studentNumber: number, claimCode: 'CLAIM-A1' });
    expect(res.status).toBe(201);
    expect(res.body.linked).toBe(true);

    const rows = await profileOf(number);
    expect(rows).toHaveLength(1);                                   // never a second profile
    expect(rows[0].student_id).toBe(before.student_id);              // the same profile
    expect(rows[0].student_name).toBe('Seeded Student');             // the lecturer's data is kept
    expect(rows[0].account_id).not.toBeNull();
    expect(rows[0].claim_code_hash).toBeNull();                      // the code is used up
    expect(rows[0].claimed_at).not.toBeNull();
    expect(rows[0].version).toBe(before.version + 1);

    // After verification the account logs in as that same student.
    const link = mailer.outbox.filter((m) => m.to === email).pop().link;
    const token = new URL(link).searchParams.get('token');
    await request(app).get('/api/auth/verify').query({ token }).expect(200);
    const login = await request(app).post('/api/auth/login').send({ email, password: PASSWORD });
    expect(login.status).toBe(200);
    expect(login.body.user.studentId).toBe(before.student_id);
  });

  test('a claim code is accepted in any letter case and with outer spaces', async () => {
    const { email } = next();
    const number = await seedProfile('CLAIM-B2');
    const res = await register({ email, studentNumber: number, claimCode: '  claim-b2 ' });
    expect(res.status).toBe(201);
    expect(res.body.linked).toBe(true);
  });

  test('a profile that exists cannot be taken without its code', async () => {
    const { email } = next();
    const number = await seedProfile('CLAIM-C3');
    const res = await register({ email, studentNumber: number });
    expect(res.status).toBe(403);
    expect(res.body.error).toBe('CLAIM_REQUIRED');
    expect(await accountCount(email)).toBe(0);                       // nothing was created
    expect((await profileOf(number))[0].account_id).toBeNull();
  });

  test('a wrong code is refused and nothing changes', async () => {
    const { email } = next();
    const number = await seedProfile('CLAIM-D4');
    const res = await register({ email, studentNumber: number, claimCode: 'CLAIM-WRONG' });
    expect(res.status).toBe(403);
    expect(res.body.error).toBe('CLAIM_INVALID');
    expect(await accountCount(email)).toBe(0);
    const row = (await profileOf(number))[0];
    expect(row.account_id).toBeNull();
    expect(row.claim_code_hash).not.toBeNull();                      // the code still works for its owner
  });

  test('a code cannot be used a second time', async () => {
    const first = next();
    const number = await seedProfile('CLAIM-E5');
    await register({ email: first.email, studentNumber: number, claimCode: 'CLAIM-E5' }).expect(201);
    const second = next();
    const res = await register({ email: second.email, studentNumber: number, claimCode: 'CLAIM-E5' });
    expect(res.status).toBe(409);
    expect(res.body.error).toBe('DUPLICATE_NUMBER');
    expect(await accountCount(second.email)).toBe(0);
  });

  test('a code for a number with no profile is refused, not turned into a new profile', async () => {
    const { email, number } = next();
    const res = await register({ email, studentNumber: number, claimCode: 'CLAIM-NONE' });
    expect(res.status).toBe(403);
    expect(res.body.error).toBe('CLAIM_INVALID');
    expect(await profileOf(number)).toHaveLength(0);
  });

  test('a deleted profile stays reserved and cannot be claimed', async () => {
    const { email } = next();
    const number = await seedProfile('CLAIM-F6', { deleted: true });
    const res = await register({ email, studentNumber: number, claimCode: 'CLAIM-F6' });
    expect(res.status).toBe(409);
    expect(res.body.error).toBe('DUPLICATE_NUMBER');
  });

  test('registering with no existing profile still creates one', async () => {
    const { email, number } = next();
    const res = await register({ email, studentNumber: number });
    expect(res.status).toBe(201);
    expect(res.body.linked).toBe(false);
    expect(await profileOf(number)).toHaveLength(1);
  });

  test('two people claiming the same profile at once: exactly one wins', async () => {
    const a = next();
    const b = next();
    const number = await seedProfile('CLAIM-G7');
    const [r1, r2] = await Promise.all([
      register({ email: a.email, studentNumber: number, claimCode: 'CLAIM-G7' }),
      register({ email: b.email, studentNumber: number, claimCode: 'CLAIM-G7' }),
    ]);
    expect([r1.status, r2.status].sort()).toEqual([201, 409]);
    expect(await profileOf(number)).toHaveLength(1);
    expect((await accountCount(a.email)) + (await accountCount(b.email))).toBe(1);
  });

  test('a claim code of the wrong type is a validation error', async () => {
    const { email, number } = next();
    const res = await register({ email, studentNumber: number, claimCode: 12345 });
    expect(res.status).toBe(400);
    expect(res.body.fields).toContain('claimCode');
  });
});
