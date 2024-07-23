const { test } = require('node:test');
const assert = require('node:assert');
const spawn = require('cross-spawn');

test('should work with CommonJS config', async t => {
  const result = spawn.sync('npx', ['eslint', '-c', 'eslint.config.cjs', 'file.js'], {
    cwd: __dirname,
    encoding: 'utf-8',
  });
  const output = result.stdout;
  console.log(output);
  const errorLines = output.split('\n').filter(line => line.includes('error'));
  assert(errorLines.length > 4);
});

test('should work with ECMAScript modules config', async t => {
  const result = spawn.sync('npx', ['eslint', '-c', 'eslint.config.mjs', 'file.js'], {
    cwd: __dirname,
    encoding: 'utf-8',
  });
  const output = result.stdout;
  console.log(output);
  const errorLines = output.split('\n').filter(line => line.includes('error'));
  assert(errorLines.length > 4);
});

test('should work with TSESLint config', async t => {
  const result = spawn.sync('npx', ['eslint', '-c', 'tseslint.config.mjs', 'file.js'], {
    cwd: __dirname,
    encoding: 'utf-8',
  });
  const output = result.stdout;
  console.log(output);
  const errorLines = output.split('\n').filter(line => line.includes('error'));
  assert(errorLines.length > 4);
});
