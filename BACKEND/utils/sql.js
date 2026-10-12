// Small helpers shared by the controllers.

/** Escape % _ \ so user input is matched literally inside a LIKE pattern. */
function escapeLike(value) {
  return String(value).replace(/[\\%_]/g, '\\$&');
}

/**
 * Read ?page=1&pageSize=20 from a query string. page is 1-based (the Android
 * RealStudentRepository sends offset = (page - 1) * pageSize on its own cache).
 */
function parsePaging(query, { defaultSize = 20, maxSize = 100 } = {}) {
  let page = parseInt(query.page, 10);
  let pageSize = parseInt(query.pageSize, 10);
  if (!Number.isInteger(page) || page < 1) page = 1;
  if (!Number.isInteger(pageSize) || pageSize < 1) pageSize = defaultSize;
  if (pageSize > maxSize) pageSize = maxSize;
  return { page, pageSize, offset: (page - 1) * pageSize };
}

/** Run fn(conn) inside one transaction; commit on success, roll back on any throw. */
async function withTransaction(pool, fn) {
  const conn = await pool.getConnection();
  try {
    await conn.beginTransaction();
    const result = await fn(conn);
    await conn.commit();
    return result;
  } catch (err) {
    try { await conn.rollback(); } catch (_) { /* connection already gone */ }
    throw err;
  } finally {
    conn.release();
  }
}

module.exports = { escapeLike, parsePaging, withTransaction };
