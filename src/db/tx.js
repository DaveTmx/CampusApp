'use strict';

const pool = require('./pool');

// Runs `work(conn)` inside one transaction and always releases the connection.
// - Returns the value from `work`. The transaction commits, even for business
//   failures that are returned as data (no changes were made in that case).
// - If `work` returns an object with `rollback: true`, the transaction is rolled back.
// - If `work` throws, the transaction is rolled back and the error is rethrown.
async function inTransaction(work) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const value = await work(conn);
    if (value && value.rollback) {
      await conn.rollback();
    } else {
      await conn.commit();
    }
    return value;
  } catch (err) {
    await conn.rollback().catch(() => {});
    throw err;
  } finally {
    conn.release();
  }
}

module.exports = { inTransaction };
