import { runTest } from '../test';

runTest(
  'scopes',
  `const foo = null;

function a() {
  function b() {
    foo.toString;
  }
  
  b();
}

a();`,
);
