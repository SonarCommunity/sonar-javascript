#!/usr/bin/env node

const http = require('http');
const formData = require('form-data');
const port = process.argv[2];
const host = process.argv[3];

console.log(`allowTsParserJsFiles: ${process.argv[5]}`);
console.log(`sonarlint: ${process.argv[6]}`);
console.log(`debugMemory: ${process.argv[7]}`);
console.log(`additional rules: [${process.argv[8]}]`);

const requestHandler = (request, response) => {
  let data = '';
  request.on('data', chunk => (data += chunk));
  request.on('end', () => {
    console.log(data);

    if (request.url === '/status' || request.url === '/new-tsconfig') {
      response.writeHead(200, { 'Content-Type': 'text/plain' });
      response.end('OK!');
    } else if (request.url === '/tsconfig-files') {
      response.end("{files: ['abs/path/file1', 'abs/path/file2', 'abs/path/file3']}");
    } else if (request.url === '/init-linter') {
      response.end('OK!');
    } else if (request.url === '/load-rule-bundles') {
      response.end('OK!');
    } else if (request.url === '/close') {
      response.end();
      server.close();
    } else if (request.url === '/create-program' && data.includes('invalid')) {
      response.end("{ error: 'failed to create program'}");
    } else if (request.url === '/create-program') {
      response.end(
        "{programId: '42', projectReferences: [], files: ['abs/path/file1', 'abs/path/file2', 'abs/path/file3']}",
      );
    } else if (request.url === '/delete-program') {
      response.end('OK!');
    } else if (request.url === '/create-tsconfig-file') {
      response.end('{"filename":"/path/to/tsconfig.json"}');
    } else if (['/analyze-css', '/analyze-yaml', '/analyze-html'].includes(request.url)) {
      // objects are created to have test coverage
      response.end(`{ issues: [{line:0, column:0, endLine:0, endColumn:0, 
        quickFixes: [
          { 
            edits: [{
              loc: {}}]}]}], 
        highlights: [{location: {startLine: 0, startColumn: 0, endLine: 0, endColumn: 0}}], 
        metrics: {}, highlightedSymbols: [{}], cpdTokens: [{}] }`);
    } else {
      // /analyze-with-program
      // /analyze-js
      // /analyze-ts
      // objects are created to have test coverage
      const res = {
        issues: [
          {
            line: 0,
            column: 0,
            endLine: 0,
            endColumn: 0,
            quickFixes: [
              {
                edits: [
                  {
                    loc: {},
                  },
                ],
              },
            ],
          },
        ],
        highlights: [{ location: { startLine: 0, startColumn: 0, endLine: 0, endColumn: 0 } }],
        metrics: {},
        highlightedSymbols: [{}],
        cpdTokens: [{}],
      };
      const fd = new formData();
      fd.append('ast', 'plop');
      //delete message.result.ast;
      fd.append('json', JSON.stringify(res));
      // this adds the boundary string that will be
      response.writeHead(200, {
        'Content-Type': fd.getHeaders()['content-type'],
        'Content-Length': fd.getLengthSync(),
      });
      fd.pipe(response);
    }
  });
};

const server = http.createServer(requestHandler);
server.keepAliveTimeout = 100; // this is used so server disconnects faster

server.listen(port, host, err => {
  if (err) {
    return console.log('something bad happened', err);
  }

  console.log(`server is listening on ${host} ${port}`);
});

process.on('exit', () => {
  console.log(`
Rule                                 | Time (ms) | Relative
:------------------------------------|----------:|--------:
no-commented-code                    |   633.226 |    16.8%
arguments-order                      |   398.175 |    10.6%
deprecation                          |   335.577 |     8.9%
  `);
});
