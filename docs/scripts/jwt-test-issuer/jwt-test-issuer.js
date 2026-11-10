#!/usr/bin/env node
// JWT provider for testing Opencast's JWT support. See readme.md.

'use strict';

const crypto = require('node:crypto');
const fs = require('node:fs');
const http = require('node:http');
const path = require('node:path');

const port = Number(process.argv[2] ?? process.env.PORT ?? 3180);
const keyFile = path.join(__dirname, 'jwt-test-issuer-key.pem');

// Load or generate the key. ES256 (ECDSA with P-256) is what Opencast accepts
// by default and what e.g. Tobira uses.
let privateKey;
if (fs.existsSync(keyFile)) {
  privateKey = crypto.createPrivateKey(fs.readFileSync(keyFile));
} else {
  privateKey = crypto.generateKeyPairSync('ec', { namedCurve: 'P-256' }).privateKey;
  fs.writeFileSync(keyFile, privateKey.export({ type: 'pkcs8', format: 'pem' }), { mode: 0o600 });
  console.log(`Generated new key: ${keyFile}`);
}

const publicJwk = crypto.createPublicKey(privateKey).export({ format: 'jwk' });
// Key ID: the thumbprint (RFC 7638), so that it changes with the key.
const kid = crypto.createHash('sha256')
  .update(JSON.stringify({ crv: publicJwk.crv, kty: publicJwk.kty, x: publicJwk.x, y: publicJwk.y }))
  .digest('base64url');
const jwks = { keys: [{ ...publicJwk, kid, alg: 'ES256', use: 'sig' }] };

const base64url = (data) => Buffer.from(data).toString('base64url');

const sign = (payload) => {
  const header = { alg: 'ES256', typ: 'JWT', kid };
  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(payload))}`;
  // JWS wants the raw `r || s` signature, not the DER encoding.
  const signature = crypto.sign('sha256', Buffer.from(signingInput), { key: privateKey, dsaEncoding: 'ieee-p1363' });
  return `${signingInput}.${signature.toString('base64url')}`;
};

const readBody = (req) => new Promise((resolve, reject) => {
  const chunks = [];
  req.on('data', (chunk) => chunks.push(chunk));
  req.on('end', () => resolve(Buffer.concat(chunks).toString()));
  req.on('error', reject);
});

const server = http.createServer(async (req, res) => {
  const reply = (status, body, contentType = 'text/plain') => {
    res.writeHead(status, {
      'Content-Type': contentType,
      // Allow test pages from any origin to request JWTs.
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type',
    });
    res.end(body);
  };

  const url = new URL(req.url, `http://${req.headers.host}`);
  try {
    if (req.method === 'OPTIONS') {
      reply(204, '');
    } else if (req.method === 'GET' && url.pathname === '/.well-known/jwks.json') {
      reply(200, JSON.stringify(jwks), 'application/json');
    } else if (req.method === 'POST' && url.pathname === '/jwt') {
      let payload;
      try {
        payload = JSON.parse(await readBody(req));
      } catch (e) {
        return reply(400, `Request body is not valid JSON: ${e.message}\n`);
      }
      if (typeof payload !== 'object' || payload === null || Array.isArray(payload)) {
        return reply(400, 'Request body must be a JSON object (the JWT payload)\n');
      }
      reply(200, sign(payload));
    } else {
      reply(404, 'Endpoints: GET /.well-known/jwks.json, POST /jwt (body: JSON payload)\n');
    }
  } catch (e) {
    console.error(e);
    reply(500, `${e.message}\n`);
  }
});

server.listen(port, () => {
  console.log(`JWT test issuer listening on port ${port}, key ID ${kid}`);
  console.log(`JWKS: http://localhost:${port}/.well-known/jwks.json`);
  console.log(`JWTs: curl -d '{"exp": 2000000000, "sub": "jane", "name": "Jane Doe", "oc": {"e:EVENT-ID": ["read"]}}' http://localhost:${port}/jwt`);
});
