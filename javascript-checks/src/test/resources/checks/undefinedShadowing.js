// the tests in this file have already been reported to the test file for
// class BoundOrAssignedSpecialIdentifiersCheck (S1514), therefore this file
// can be safely deleted

function f() {
  var undefined = 1;  // Noncompliant {{Rename this variable.}}
  var undefined;      // Noncompliant [[sc=7;ec=16]]
  let undefined;      // Noncompliant
  var a = 1;          // OK
}

var undefined = 1;    // OK
var undefined;        // OK

undefined = 1;        // OK

function b() {
  const undefined;      // Noncompliant
}
