const chai = require('chai');
const { assert, expect, should } = chai;
const sinon = require('sinon');

should();

describe('test cases', () => {
  it('no assertion', () => { // Noncompliant {{Add at least one assertion to this test case.}}
    alert('msg');
  });

  it('no transitive assertion', () => { // Noncompliant
    nocheck();
  });

  it.skip('non compliant', () => { // Noncompliant
    alert('msg');
  });

  it.only('non compliant', () => { // Noncompliant
    alert('msg');
  });

  function nocheck() {
    alert('msg');
  }

  it('foo') // missing callback
});

describe('chai test cases', () => {
  it('expect', () => { // Compliant
    expect(1).to.equal(2);
  });

  it('chai.expect', () => { // Compliant
    chai.expect(1).to.equal(2);
  });

  it('assert', () => { // Compliant
    assert([] !== [2]);
  });

  it('chai.assert', () => { // Compliant
    chai.assert([] !== [2]);
  });

  it('assert.<method>', () => { // Compliant
    assert.equal(1, 2);
  });

  it('chai.assert.<method>', () => { // Compliant
    chai.assert.equal(1, 2);
  });

  it('should', () => { // Compliant
    'foo'.should.equal('bar');
  });

  it('transitive assertion', () => { // Compliant
    check();
  });

  function check() {
    expect(1).to.equal(2);
  }
});

describe('sinon test cases', () => {
  const { assert } = sinon;

  it('assert.<method>', () => { // Compliant
    assert.called(spy);
  });

  it('sinon.assert.<method>', () => { // Compliant
    sinon.assert.calledOnce(spy);
  });

  it('transitive assertion', () => { // Compliant
    check();
  });

  function check() {
    assert.called(spy);
  }
});
