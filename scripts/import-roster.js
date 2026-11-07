#!/usr/bin/env node
'use strict';

// Loads a class roster from a CSV file, with a one-time claim code for every student.
//
// Usage:
//   node scripts/import-roster.js roster.csv [--dry-run] [--codes claim-codes.csv]
//
// CSV (header row required):  student_number,full_name,programme_code,group_code
//   group_code may be empty: the student stays Unassigned.
//   A group that is already full leaves the student Unassigned (the group-capacity rule always wins).
//
// For each NEW student the script prints nothing secret on screen. The plain claim codes are
// written to the --codes file (default claim-codes.csv) so you can hand them out. That file and
// the roster file are secrets/personal data: keep them out of Git, out of the submission
// package and out of AI tools. The database stores only a hash of each code.
//
// Students whose number already exists are skipped, so the script is safe to run twice.
require('dotenv').config();
const fs = require('fs');
const { randomInt, randomUUID } = require('crypto');
const pool = require('../src/db/pool');
const { inTransaction } = require('../src/db/tx');
const { hashToken } = require('../src/services/verification');
const { cleanName } = require('../src/util/validation');
const { assignToGroupTx } = require('../src/controllers/studentsController');
const { GROUPS, PROGRAMMES } = require('../src/constants');

// No 0/O/1/I so a code read aloud or from paper is hard to get wrong.
const ALPHABET = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
function newClaimCode() {
  const pick = () => Array.from({ length: 5 }, () => ALPHABET[randomInt(ALPHABET.length)]).join('');
  return `${pick()}-${pick()}`;
}

// Minimal CSV reader: quoted fields, commas and quotes inside quotes, CRLF or LF.
function parseCsv(text) {
  const rows = [];
  let row = [];
  let field = '';
  let quoted = false;
  for (let i = 0; i < text.length; i += 1) {
    const ch = text[i];
    if (quoted) {
      if (ch === '"' && text[i + 1] === '"') { field += '"'; i += 1; } else if (ch === '"') quoted = false;
      else field += ch;
    } else if (ch === '"') quoted = true;
    else if (ch === ',') { row.push(field); field = ''; }
    else if (ch === '\n' || ch === '\r') {
      if (ch === '\r' && text[i + 1] === '\n') i += 1;
      row.push(field); field = '';
      if (row.some((c) => c.trim() !== '')) rows.push(row);
      row = [];
    } else field += ch;
  }
  row.push(field);
  if (row.some((c) => c.trim() !== '')) rows.push(row);
  return rows;
}

function csvCell(value) {
  const s = String(value ?? '');
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

async function main() {
  const args = process.argv.slice(2);
  const dryRun = args.includes('--dry-run');
  const codesIdx = args.indexOf('--codes');
  const codesFile = codesIdx >= 0 ? args[codesIdx + 1] : 'claim-codes.csv';
  const file = args.find((a, i) => !a.startsWith('--') && args[i - 1] !== '--codes');
  if (!file || !fs.existsSync(file)) {
    console.error('Usage: node scripts/import-roster.js roster.csv [--dry-run] [--codes claim-codes.csv]');
    process.exitCode = 1;
    return;
  }

  const rows = parseCsv(fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, ''));
  const header = rows.shift().map((h) => h.trim().toLowerCase());
  const col = (name) => header.indexOf(name);
  const need = ['student_number', 'full_name', 'programme_code', 'group_code'];
  if (need.some((n) => col(n) < 0)) {
    console.error(`The header row must contain: ${need.join(', ')}`);
    process.exitCode = 1;
    return;
  }

  const summary = { added: 0, skippedExisting: 0, invalid: 0, assigned: 0, unassignedFull: 0 };
  const codes = [];
  const problems = [];
  const perGroup = {};

  for (let i = 0; i < rows.length; i += 1) {
    const line = i + 2; // +1 for the header, +1 because people count from 1
    const r = rows[i];
    const number = (r[col('student_number')] || '').trim();
    const name = cleanName(r[col('full_name')]);
    const programme = (r[col('programme_code')] || '').trim().toUpperCase();
    const group = (r[col('group_code')] || '').trim().toUpperCase();
    const bad = [];
    if (!/^\d{9}$/.test(number)) bad.push('student_number must be exactly 9 digits');
    if (!name) bad.push('full_name must be 2-100 characters');
    if (!PROGRAMMES.includes(programme)) bad.push(`programme_code must be one of ${PROGRAMMES.join(', ')}`);
    if (group && !GROUPS.includes(group)) bad.push(`group_code must be empty or ${GROUPS[0]}-${GROUPS[GROUPS.length - 1]}`);
    if (bad.length) {
      summary.invalid += 1;
      problems.push(`line ${line}: ${bad.join('; ')}`);
      continue;
    }

    const code = newClaimCode();
    const outcome = await inTransaction(async (conn) => {
      const [[exists]] = await conn.query('SELECT 1 AS x FROM students WHERE student_number = ?', [number]);
      if (exists) return { skipped: true };
      if (dryRun) return { dry: true };
      const studentId = randomUUID();
      await conn.query(
        'INSERT INTO students (student_id, student_name, student_number, programme_code, claim_code_hash) VALUES (?, ?, ?, ?, ?)',
        [studentId, name, number, programme, hashToken(code)]
      );
      let placed = null;
      if (group) {
        const result = await assignToGroupTx(conn, studentId, group);
        placed = result.ok ? group : null;
      }
      return { studentId, placed };
    });

    if (outcome.skipped) { summary.skippedExisting += 1; continue; }
    summary.added += 1;
    if (!dryRun) codes.push([number, name, group || '', outcome.placed || '', code]);
    if (group) {
      if (outcome.dry || outcome.placed) { summary.assigned += 1; perGroup[group] = (perGroup[group] || 0) + 1; }
      else { summary.unassignedFull += 1; problems.push(`line ${line}: ${group} is full, ${number} left Unassigned`); }
    }
  }

  if (!dryRun && codes.length) {
    const lines = ['student_number,full_name,requested_group,placed_group,claim_code']
      .concat(codes.map((c) => c.map(csvCell).join(',')));
    fs.writeFileSync(codesFile, `${lines.join('\n')}\n`, { mode: 0o600 });
  }

  if (dryRun) console.log('DRY RUN: nothing was written.');
  else if (codes.length) console.log(`Claim codes written to ${codesFile} (keep it private).`);
  else console.log('No new students, so no claim codes were written.');
  console.log(summary);
  if (Object.keys(perGroup).length) console.log('Students per group:', perGroup);
  if (problems.length) console.log(`\n${problems.length} problem(s):\n  ${problems.join('\n  ')}`);
}

main()
  .catch((err) => { console.error(err); process.exitCode = 1; })
  .finally(() => pool.end());
