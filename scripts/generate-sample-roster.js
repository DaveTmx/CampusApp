#!/usr/bin/env node
'use strict';

// Makes a FICTITIOUS roster (invented names, made-up student numbers) for demos and testing.
//
// Usage:
//   node scripts/generate-sample-roster.js [--per-group 12] [--out sample-roster.csv]
//
// Then load it with:  node scripts/import-roster.js sample-roster.csv
// Student numbers run 700000001, 700000002, ... so they never clash with your own test data.
const fs = require('fs');
const { GROUPS, GROUP_CAPACITY, PROGRAMMES } = require('../src/constants');

const args = process.argv.slice(2);
const opt = (name, fallback) => {
  const i = args.indexOf(name);
  return i >= 0 ? args[i + 1] : fallback;
};
const perGroup = Number(opt('--per-group', 12));
const out = opt('--out', 'sample-roster.csv');
if (!Number.isInteger(perGroup) || perGroup < 0 || perGroup > GROUP_CAPACITY) {
  console.error(`--per-group must be a whole number from 0 to ${GROUP_CAPACITY}`);
  process.exit(1);
}

const first = ['Mwila', 'Chanda', 'Natasha', 'Joseph', 'Mercy', 'Brian', 'Temwani', 'Kondwani', 'Lubasi', 'Mutinta',
  'Bupe', 'Thandiwe', 'Kelvin', 'Precious', 'Given', 'Esther', 'Moses', 'Ruth', 'Daniel', 'Grace',
  'Patrick', 'Charity', 'Isaac', 'Naomi', 'Felix', 'Doreen', 'Collins', 'Alice', 'Victor', 'Sarah'];
const last = ['Mulenga', 'Banda', 'Phiri', 'Tembo', 'Zulu', 'Mwansa', 'Chileshe', 'Lungu', 'Sakala', 'Daka',
  'Mumba', 'Kapata', 'Chola', 'Siame', 'Nkonde', 'Musonda', 'Bwalya', 'Kasonde', 'Ngoma', 'Simukonda'];

const lines = ['student_number,full_name,programme_code,group_code'];
let n = 0;
for (const group of GROUPS) {
  for (let k = 0; k < perGroup; k += 1) {
    n += 1;
    const name = `${first[n % first.length]} ${last[(n * 7) % last.length]}`;
    const number = String(700000000 + n);
    lines.push([number, name, PROGRAMMES[n % PROGRAMMES.length], group].join(','));
  }
}
fs.writeFileSync(out, `${lines.join('\n')}\n`);
console.log(`Wrote ${n} fictitious students in ${GROUPS.length} groups to ${out}`);
