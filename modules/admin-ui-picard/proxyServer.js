const path = require("path");
const express = require("express");
const bodyParser = require('body-parser');
const { createProxyMiddleware } = require('http-proxy-middleware');
const requestDigest = require('request-digest');
const urlParser = require('url-parse');

const app = express();
const port = process.env.PORT || 5000;

// Get values of proxy host, username and password from npm command
let args = process.argv,
    host = args[2],
    username = args[3],
    password = args[4];

// Validate settings
if (host === undefined) {
    throw new Error("Runtime Parameter host is undefined!");
}
if (username === undefined) {
    throw new Error("Runtime Parameter username is undefined!");
}
if (password === undefined) {
    throw new Error("Runtime Parameter password is undefined!");
}



// Set up static files
// TODO: maybe future adjustments necessary when code is further implemented
app.use(
    "/styles",
    express.static(path.join(__dirname, "app/build/static/css"))
);
app.use(
    "/modules",
    express.static(path.join(__dirname, "app/src/components"))
);
app.use(
    "/shared",
    express.static(path.join(__dirname, "app/src/components"))
);
app.use(
    "/public",
    express.static(path.join(__dirname, "app/public/"))
);
app.use(
    "/img",
    express.static(path.join(__dirname, "app/src/img"))
);
app.use(
    "/info",
    express.static(path.join(__dirname, "test/app/GET/info"))
);
app.use(
    "/i18n",
    express.static(path.join(__dirname, "test/app/GET/i18n"))
);
app.use(
    "/sysinfo",
    express.static(path.join(__dirname, "test/app/GET/sysinfo"))
);


app.use('', (req, res, next) => {
    console.log('Proxy ' + req.method + ' ' + req.url + ' -> ' + host + req.url);

    let onReadFromBackend = function (error, response, body) {
        if (error && (typeof error != 'object' || !error.hasOwnProperty('statusCode') || !error.hasOwnProperty('body'))) {
            throw error;
        }

        // forward to client
        res.statusCode = (response || error).statusCode;
        body = body || (error ? error.body : '');
        res.write(body);
        res.end();
    };

    let parsed = urlParser(req.url);
    let parsedHost = urlParser(host);
    let escapedQuery = parsed.query.replace(',', '%2C');

    let onForwardToBackend = function (body) {
        let authConfig = {
            host: parsedHost.protocol + '//' + parsedHost.hostname,
            port: parsedHost.port,
            path: parsed.pathname + escapedQuery,
            method: req.method,
            headers: {
                'content-type': req.headers['content-type'],
                'X-Requested-Auth': 'Digest'
            },
            jar: true,
            body: body
        };

        requestDigest(username, password).request(authConfig, onReadFromBackend);
    };

    let buffer = [];
    req.on('data', function (chunk) {
        buffer.push(chunk);
    });
    req.on('end', function () {
        onForwardToBackend(Buffer.concat(buffer).toString());
    });
});

// todo: not sure if really needed
app.use(
    "/admin-ng",
    createProxyMiddleware({
        target: host,
        headers: {
            'X-Requested-Auth': 'Digest'
        },
        changeOrigin: true
    }));

app.use(
    "/services",
    createProxyMiddleware({
        target: host,
        headers: {
            'X-Requested-Auth': 'Digest'
        },
        changeOrigin: true
    }));

app.listen(port, () => console.log(`Listening on port ${port}`));
