const path = require('path');
const fs = require('fs');
const semver = require('semver');

const version = process.versions.node;
if (semver.lt(version, '20.10.0')) {
  console.error('Node.js 20.10.0 or higher is required');
  process.exit(1);
}

const TARGET = path.join(__dirname, '..', 'its', 'sources');
const LINK = path.join(__dirname, '..', '..', 'sonarjs-ruling-sources');

if (fs.existsSync(LINK)) {
  fs.unlinkSync(LINK);
}
fs.symlinkSync(TARGET, LINK);
