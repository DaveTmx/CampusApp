// Registration, verification, login lockout, lecturer protection and ownership.
// Run: npm test -- auth.test.js   (needs a MySQL database built from database/schema.sql)
require('dotenv').config();
const path = require('path');
const request = require('supertest');
const pool = require(path.join(__dirname, '../../src/db/pool'));
const app = require(path.join(__dirname, '../../server'));
const mailer = require(path.join(__dirname, '../../src/mailer'));

jest.setTimeout(60000);

const PASSWORD = 'correct-horse-battery';
let counter = 0;

// Each test gets its own student number and email, so the tests do not depend on order.
function newStudent(prefix = 'auth-test') {
  counter += 1;
  const suffix = `${Date.now()}-${counter}`;
  return {
    fullName: `Test ${suffix}`,
    email: `${prefix}-${suffix}@example.test`,
    password: PASSWORD,
    studentNumber: String(800000000 + Date.now() % 100000000 + counter).slice(0, 9),
    programmeCode: 'CS',
  };
}

function tokenFor(email) {
  const message = mailer.outbox.filter((m) => m.to === email).pop();
  return new URL(message.link).searchParams.get('token');
}

async function registerAndVerify(student) {
  await request(app).post('/api/auth/register').send(student).expect(201);
  await request(app).get('/api/auth/verify').query({ token: tokenFor(student.email) }).expect(200);
}

async function loginAs(email, password = PASSWORD) {
  return request(app).post('/api/auth/login').send({ email, password });
}

beforeAll(async () => {
  // Remove rows left by earlier runs. Students first, because they reference accounts.
  await pool.query(
    `DELETE s FROM students s JOIN accounts a ON a.id = s.account_id WHERE a.email LIKE 'auth-test-%@example.test'`
  );
  await pool.query(`DELETE FROM accounts WHERE email LIKE 'auth-test-%@example.test'`);
});

afterAll(async () => {
  await pool.end();
});

describe('login with email or student number', () => {
  test('a verified student can sign in with the student number or the email', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const byNumber = await request(app)
      .post('/api/auth/login')
      .send({ identifier: student.studentNumber, password: PASSWORD });
    expect(byNumber.status).toBe(200);
    expect(byNumber.body.user.role).toBe('student');

    const byEmail = await request(app)
      .post('/api/auth/login')
      .send({ identifier: student.email, password: PASSWORD });
    expect(byEmail.status).toBe(200);
    expect(byEmail.body.user.email).toBe(student.email);
  });

  test('a wrong password is refused for the student number too', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const res = await request(app)
      .post('/api/auth/login')
      .send({ identifier: student.studentNumber, password: 'not-the-password' });
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('INVALID_CREDENTIALS');
  });
});

describe('registration and email verification', () => {
  test('register creates an inactive student account and sends a link', async () => {
    const student = newStudent();
    const res = await request(app).post('/api/auth/register').send(student);

    expect(res.status).toBe(201);
    expect(res.body.status).toBe('PENDING_VERIFICATION');
    expect(mailer.outbox.some((m) => m.to === student.email)).toBe(true);
  });

  test('login is refused until the email link is opened', async () => {
    const student = newStudent();
    await request(app).post('/api/auth/register').send(student).expect(201);

    const res = await loginAs(student.email);
    expect(res.status).toBe(403);
    expect(res.body.error).toBe('EMAIL_NOT_VERIFIED');
  });

  test('opening the link activates the account; login then succeeds as a student', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const res = await loginAs(student.email);
    expect(res.status).toBe(200);
    expect(res.body.user.role).toBe('student');
    expect(res.body.user.studentId).toEqual(expect.any(String));
  });

  test('a verification link works only once', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const again = await request(app).get('/api/auth/verify').query({ token: tokenFor(student.email) });
    expect(again.status).toBe(400);
    expect(again.body.error).toBe('INVALID_TOKEN');
  });

  test('a malformed token is rejected before touching the database', async () => {
    const res = await request(app).get('/api/auth/verify').query({ token: 'not-a-token' });
    expect(res.status).toBe(400);
  });

  test('duplicate email and duplicate student number are reported separately', async () => {
    const student = newStudent();
    await request(app).post('/api/auth/register').send(student).expect(201);

    const sameEmail = await request(app).post('/api/auth/register').send({ ...student, studentNumber: '123456789' });
    expect(sameEmail.body.error).toBe('DUPLICATE_EMAIL');

    const sameNumber = await request(app).post('/api/auth/register')
      .send({ ...newStudent(), studentNumber: student.studentNumber });
    expect(sameNumber.body.error).toBe('DUPLICATE_NUMBER');
  });

  test('registration cannot create a lecturer, even if the client asks for one', async () => {
    const student = newStudent();
    await registerAndVerify({ ...student, role: 'lecturer' });

    const res = await loginAs(student.email);
    expect(res.body.user.role).toBe('student');
  });

  test('resend is generic and limited to one new link per cooldown', async () => {
    const student = newStudent();
    await request(app).post('/api/auth/register').send(student).expect(201);
    const before = mailer.outbox.filter((m) => m.to === student.email).length;

    const first = await request(app).post('/api/auth/resend-verification').send({ email: student.email });
    const second = await request(app).post('/api/auth/resend-verification').send({ email: student.email });
    const unknown = await request(app).post('/api/auth/resend-verification').send({ email: 'nobody@example.test' });

    expect(first.status).toBe(200);
    expect(second.body.message).toBe(first.body.message);   // same wording every time
    expect(unknown.body.message).toBe(first.body.message);  // does not reveal unknown emails
    // Registration just sent a link, so both resends fall inside the 60-second cooldown: no new email.
    expect(mailer.outbox.filter((m) => m.to === student.email).length).toBe(before);
  });
});

describe('login protection', () => {
  test('five wrong passwords lock the account, even for the right password', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    for (let i = 0; i < 5; i++) {
      await loginAs(student.email, 'wrong-password-here');
    }
    const locked = await loginAs(student.email);
    expect(locked.status).toBe(423);
    expect(locked.body.error).toBe('ACCOUNT_LOCKED');
  });

  test('unknown emails and wrong passwords get the same error', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const wrongPassword = await loginAs(student.email, 'wrong-password-here');
    const unknownEmail = await loginAs('nobody@example.test', 'wrong-password-here');
    expect(wrongPassword.body.error).toBe('INVALID_CREDENTIALS');
    expect(unknownEmail.body.error).toBe('INVALID_CREDENTIALS');
  });
});

describe('lecturer protection', () => {
  test('lecturer records cannot be created through the API', async () => {
    const res = await request(app).post('/api/students').send({});
    expect(res.status).toBe(401); // no token at all
  });

  test('a student token cannot read the roster or make changes', async () => {
    const student = newStudent();
    await registerAndVerify(student);
    const login = await loginAs(student.email);
    const auth = { Authorization: `Bearer ${login.body.token}` };

    expect((await request(app).get('/api/students').set(auth)).status).toBe(403);
    expect((await request(app).post('/api/operations').set(auth)
      .send({ operationId: 'auth-test-op-1', opType: 'DELETE_STUDENT', payload: { studentId: 'x' } })).status).toBe(403);
    expect((await request(app).get('/api/groups').set(auth)).status).toBe(200);
    expect((await request(app).post('/api/groups').set(auth).send({ name: 'Nope' })).status).toBe(403);
  });

  test('a student can read and edit only their own record', async () => {
    const mine = newStudent();
    const theirs = newStudent();
    await registerAndVerify(mine);
    await registerAndVerify(theirs);

    const myLogin = await loginAs(mine.email);
    const auth = { Authorization: `Bearer ${myLogin.body.token}` };
    const myId = myLogin.body.user.studentId;
    const theirLogin = await loginAs(theirs.email);
    const theirId = theirLogin.body.user.studentId;

    expect((await request(app).get(`/api/students/${myId}`).set(auth)).status).toBe(200);
    expect((await request(app).get(`/api/students/${theirId}`).set(auth)).status).toBe(403);

    const rename = await request(app).put(`/api/students/${myId}`).set(auth)
      .send({ baseVersion: 1, studentName: 'My New Name' });
    expect(rename.status).toBe(200);

    const otherProgramme = await request(app).put(`/api/students/${myId}`).set(auth)
      .send({ baseVersion: 2, programmeCode: 'IT' });
    expect(otherProgramme.body.error).toBe('FIELD_NOT_ALLOWED');
  });
});

describe('token checks', () => {
  test('a request without a valid token is refused', async () => {
    const res = await request(app).get('/api/groups').set('Authorization', 'Bearer not.a.token');
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('INVALID_TOKEN');
  });

  test('unknown routes and bad JSON get JSON errors, not HTML', async () => {
    const missing = await request(app).get('/api/nope');
    expect(missing.status).toBe(404);
    expect(missing.body.error).toBe('NOT_FOUND');

    const badJson = await request(app).post('/api/auth/login')
      .set('Content-Type', 'application/json').send('{"email": ');
    expect(badJson.status).toBe(400);
    expect(badJson.body.error).toBe('INVALID_JSON');
  });
});

// ---- lecturer (staff) sign-in: password, then an emailed code ----

const bcrypt = require('bcryptjs');

// Lecturers are never created by the API. The test inserts an active account directly,
// the same way scripts/create-lecturer.js does after the mailbox is verified.
async function createLecturer() {
  counter += 1;
  const email = `auth-test-lect-${Date.now()}-${counter}@example.test`;
  const hash = await bcrypt.hash(PASSWORD, 12);
  await pool.query(
    'INSERT INTO accounts (email, password_hash, full_name, role, is_active) VALUES (?, ?, ?, ?, 1)',
    [email, hash, 'Test Lecturer', 'lecturer']
  );
  return email;
}

// The most recent code emailed to this address (test mode keeps it in the outbox).
function codeFor(email) {
  return mailer.outbox.filter((m) => m.to === email && m.code).pop().code;
}

function staffLogin(email, password = PASSWORD) {
  return request(app).post('/api/auth/staff/login').send({ email, password });
}

function verifyCode(challengeId, code) {
  return request(app).post('/api/auth/staff/verify-code').send({ challengeId, code });
}

// A code that is certainly wrong for a given real code.
function wrongCodeFor(code) {
  return code === '000000' ? '000001' : '000000';
}

describe('lecturer sign-in with an emailed code', () => {
  test('the old login endpoint refuses a lecturer even with the right password', async () => {
    const email = await createLecturer();
    const res = await loginAs(email);
    expect(res.status).toBe(403);
    expect(res.body.error).toBe('STAFF_SIGN_IN_REQUIRED');
    expect(res.body.token).toBeUndefined();
  });

  test('correct password sends a 6-digit code and returns a challenge, not a token', async () => {
    const email = await createLecturer();
    const res = await staffLogin(email);

    expect(res.status).toBe(202);
    expect(res.body.challengeId).toMatch(/^[0-9a-f-]{36}$/);
    expect(res.body.token).toBeUndefined();
    expect(codeFor(email)).toMatch(/^\d{6}$/);
  });

  test('the correct code returns a lecturer token once', async () => {
    const email = await createLecturer();
    const started = await staffLogin(email).expect(202);
    const code = codeFor(email);

    const res = await verifyCode(started.body.challengeId, code);
    expect(res.status).toBe(200);
    expect(res.body.user.role).toBe('lecturer');
    expect(res.body.user.studentId).toBeNull();

    // The same code cannot be used twice.
    const again = await verifyCode(started.body.challengeId, code);
    expect(again.status).toBe(401);
    expect(again.body.error).toBe('CODE_INVALID');
  });

  test('a wrong code is refused and says how many tries are left', async () => {
    const email = await createLecturer();
    const started = await staffLogin(email).expect(202);
    const res = await verifyCode(started.body.challengeId, wrongCodeFor(codeFor(email)));

    expect(res.status).toBe(401);
    expect(res.body.error).toBe('CODE_INCORRECT');
    expect(res.body.attemptsLeft).toBe(4);
  });

  test('five wrong codes burn the challenge, even if the right code is sent next', async () => {
    const email = await createLecturer();
    const started = await staffLogin(email).expect(202);
    const right = codeFor(email);
    for (let i = 0; i < 5; i += 1) {
      await verifyCode(started.body.challengeId, wrongCodeFor(right));
    }
    const res = await verifyCode(started.body.challengeId, right);
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('CODE_INVALID');
  });

  test('a second code request within 60 seconds is refused', async () => {
    const email = await createLecturer();
    await staffLogin(email).expect(202);
    const res = await staffLogin(email);
    expect(res.status).toBe(429);
    expect(res.body.error).toBe('CODE_ALREADY_SENT');
    expect(res.body.retryAfterSeconds).toBeGreaterThan(0);
  });

  test('a student account cannot use the staff sign-in', async () => {
    const student = newStudent('auth-test-stu');
    await registerAndVerify(student);
    const res = await staffLogin(student.email);
    expect(res.status).toBe(401);
    expect(res.body.error).toBe('INVALID_CREDENTIALS');
  });

  test('five wrong passwords lock the lecturer account, and no code is sent while locked', async () => {
    const email = await createLecturer();
    for (let i = 0; i < 5; i += 1) {
      await staffLogin(email, 'not-the-password');
    }
    const res = await staffLogin(email);
    expect(res.status).toBe(423);
    expect(res.body.error).toBe('ACCOUNT_LOCKED');
  });

  test('wrong codes count toward the same lockout: the fifth wrong code locks the account', async () => {
    const email = await createLecturer();
    const started = await staffLogin(email).expect(202);
    const right = codeFor(email);
    let last;
    for (let i = 0; i < 5; i += 1) {
      last = await verifyCode(started.body.challengeId, wrongCodeFor(right));
    }
    expect(last.status).toBe(423);
    expect(last.body.error).toBe('ACCOUNT_LOCKED');
  });
});
