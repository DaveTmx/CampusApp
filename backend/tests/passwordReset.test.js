// Forgot password and reset password: generic replies, single-use 30-minute links,
// and every existing session ends once the password changes.
// Run: npm test -- passwordReset.test.js   (needs a MySQL database built from database/schema.sql)
require('dotenv').config();
const path = require('path');
const request = require('supertest');
const pool = require(path.join(__dirname, '../../src/db/pool'));
const app = require(path.join(__dirname, '../../server'));
const mailer = require(path.join(__dirname, '../../src/mailer'));

jest.setTimeout(60000);

// The reply is sent before the email, so give the background send a moment to finish.
async function waitForMail(predicate) {
  for (let i = 0; i < 50; i++) {
    if (predicate()) return true;
    await new Promise((r) => setTimeout(r, 40));
  }
  return predicate();
}

const PASSWORD = 'correct-horse-battery';
const NEW_PASSWORD = 'new-horse-battery-2';
let counter = 0;

function newStudent() {
  counter += 1;
  const suffix = `${Date.now()}-${counter}`;
  return {
    fullName: `Reset ${suffix}`,
    email: `auth-test-reset-${suffix}@example.test`,
    password: PASSWORD,
    studentNumber: String(900000000 + (Date.now() % 90000000) + counter).slice(0, 9),
    programmeCode: 'CS',
  };
}

function verifyTokenFor(email) {
  const message = mailer.outbox.filter((m) => m.to === email && m.link && m.link.includes('/verify')).pop();
  return new URL(message.link).searchParams.get('token');
}

function resetTokenFor(email) {
  const message = mailer.outbox.filter((m) => m.to === email && m.link && m.link.includes('reset-password')).pop();
  return new URL(message.link).searchParams.get('token');
}

async function registerAndVerify(student) {
  await request(app).post('/api/auth/register').send(student).expect(201);
  await request(app).get('/api/auth/verify').query({ token: verifyTokenFor(student.email) }).expect(200);
}

beforeAll(async () => {
  await pool.query(
    `DELETE s FROM students s JOIN accounts a ON a.id = s.account_id WHERE a.email LIKE 'auth-test-reset-%@example.test'`
  );
  await pool.query(`DELETE FROM accounts WHERE email LIKE 'auth-test-reset-%@example.test'`);
});

afterAll(async () => {
  await pool.end();
});

describe('forgot password', () => {
  test('gives the same reply for an unknown email and for a real one', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const unknown = await request(app)
      .post('/api/auth/forgot-password')
      .send({ email: 'nobody-here@example.test' });
    const known = await request(app)
      .post('/api/auth/forgot-password')
      .send({ email: student.email });

    expect(unknown.status).toBe(200);
    expect(known.status).toBe(200);
    expect(known.body).toEqual(unknown.body);
    expect(await waitForMail(() => mailer.outbox.some((m) => m.to === student.email && m.link.includes('reset-password')))).toBe(true);
    await new Promise((r) => setTimeout(r, 200));
    expect(mailer.outbox.some((m) => m.to === 'nobody-here@example.test')).toBe(false);
  });

  test('does not send a link to an account that has not verified its email', async () => {
    const student = newStudent();
    await request(app).post('/api/auth/register').send(student).expect(201);

    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    await new Promise((r) => setTimeout(r, 200));
    expect(mailer.outbox.some((m) => m.to === student.email && m.link.includes('reset-password'))).toBe(false);
  });
});

describe('reset password', () => {
  test('a valid link sets the new password and ends every existing session', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    const before = await request(app)
      .post('/api/auth/login')
      .send({ email: student.email, password: PASSWORD })
      .expect(200);

    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const token = resetTokenFor(student.email);

    await request(app)
      .post('/api/auth/reset-password')
      .send({ token, password: NEW_PASSWORD })
      .expect(200);

    // The old password no longer works, and the new one does.
    const oldLogin = await request(app).post('/api/auth/login').send({ email: student.email, password: PASSWORD });
    expect(oldLogin.status).toBe(401);
    await request(app)
      .post('/api/auth/login')
      .send({ email: student.email, password: NEW_PASSWORD })
      .expect(200);

    // The session that existed before the reset is refused.
    const revoked = await request(app)
      .post('/api/auth/logout')
      .set('Authorization', `Bearer ${before.body.token}`);
    expect(revoked.status).toBe(401);
    expect(revoked.body.error).toBe('TOKEN_REVOKED');
  });

  test('a link can be used only once', async () => {
    const student = newStudent();
    await registerAndVerify(student);
    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const token = resetTokenFor(student.email);

    await request(app).post('/api/auth/reset-password').send({ token, password: NEW_PASSWORD }).expect(200);
    const again = await request(app).post('/api/auth/reset-password').send({ token, password: 'another-password-3' });
    expect(again.status).toBe(400);
    expect(again.body.error).toBe('INVALID_TOKEN');
  });

  test('asking for a second link makes the first one stop working', async () => {
    const student = newStudent();
    await registerAndVerify(student);

    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const first = resetTokenFor(student.email);
    await new Promise((resolve) => setTimeout(resolve, 10));
    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const second = resetTokenFor(student.email);
    expect(second).not.toBe(first);

    const stale = await request(app).post('/api/auth/reset-password').send({ token: first, password: NEW_PASSWORD });
    expect(stale.status).toBe(400);
    await request(app).post('/api/auth/reset-password').send({ token: second, password: NEW_PASSWORD }).expect(200);
  });

  test('an expired link is refused', async () => {
    const student = newStudent();
    await registerAndVerify(student);
    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const token = resetTokenFor(student.email);

    // Move the link's expiry into the past, as if 30 minutes had passed.
    await pool.query(
      'UPDATE password_reset_tokens SET expires_at = DATE_SUB(NOW(), INTERVAL 1 MINUTE) WHERE token_hash = SHA2(?, 256)',
      [token]
    );

    const res = await request(app).post('/api/auth/reset-password').send({ token, password: NEW_PASSWORD });
    expect(res.status).toBe(400);
    expect(res.body.error).toBe('INVALID_TOKEN');
  });

  test('a short password is refused and the link is kept', async () => {
    const student = newStudent();
    await registerAndVerify(student);
    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const token = resetTokenFor(student.email);

    const short = await request(app).post('/api/auth/reset-password').send({ token, password: 'short' });
    expect(short.status).toBe(400);
    expect(short.body.error).toBe('VALIDATION_FAILED');

    await request(app).post('/api/auth/reset-password').send({ token, password: NEW_PASSWORD }).expect(200);
  });

  test('the reset page shows a form for a valid link and a message for a bad one', async () => {
    const student = newStudent();
    await registerAndVerify(student);
    await request(app).post('/api/auth/forgot-password').send({ email: student.email }).expect(200);
    const token = resetTokenFor(student.email);

    const good = await request(app).get('/api/auth/reset-password').query({ token });
    expect(good.status).toBe(200);
    expect(good.text).toContain('id="f"');

    const bad = await request(app).get('/api/auth/reset-password').query({ token: '<script>x</script>' });
    expect(bad.status).toBe(200);
    expect(bad.text).not.toContain('<script>x</script>');
    expect(bad.text).toContain('not valid');
  });
});
