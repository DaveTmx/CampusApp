Add to package.json's devDependencies and scripts (jest/supertest are not currently installed):

```json
"scripts": {
  "test": "jest backend/tests --runInBand"
},
"devDependencies": {
  "nodemon": "^3.1.14",
  "jest": "^29.7.0",
  "supertest": "^6.3.4"
}
```
`--runInBand` matters here: the concurrency test resets the shared MySQL fixture between runs, so tests must not run in parallel against the same database.

Also create a `.env` (gitignored already) with:
```
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=
DB_NAME=ict361_lab
JWT_SECRET=<a real random string>
```
