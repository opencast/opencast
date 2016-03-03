'use strict';
var proxySnippet = require('grunt-connect-proxy/lib/utils').proxyRequest;
var requestDigest = require('request-digest');
var serveStatic = require('serve-static');
var httpModule = require('http');
var urlParser = require('url-parse');

module.exports = function (grunt) {
  var username = grunt.option('proxy.username'),
      password = grunt.option('proxy.password'),
      host = grunt.option('proxy.host');

  /**
   * This is the middleware function used by the proxy grunt-contrib-connect module.
   */
  return function (connect, serverOptions) {
    if (!Array.isArray(serverOptions.base)) {
      serverOptions.base = [serverOptions.base];
    }

    var middlewares = [];

    // Validate settings
    var params = ['proxy.host', 'proxy.username', 'proxy.password'];

    for (var id in params) {
      if (grunt.option(params[id]) === undefined) {
        grunt.fail.fatal('Expecting runtime parameter "' + params[id] + '". Use --' + params[id] + '=<value>');
      }
    }
    // --

    console.log('Proxying to ' + host);

    // wraps every proxy request with digest authentication
    httpModule.createServer(function (req, res) {
      console.log('Proxy ' + req.method + ' ' + req.url + ' -> ' + host + req.url);


      var onReadFromBackend = function (error, response, body) {
        if (error) {
          throw error;
        }
        // forward to client
        res.statusCode = response.statusCode;
        res.write(body);
        res.end();
      };

      var parsed = urlParser(req.url);
      var escapedQuery = parsed.query.replace(',','%2C');

      var onForwardToBackend = function (body) {
        var authConfig = {
          host: host,
          path: parsed.pathname + escapedQuery,
          method: req.method,
          headers: {
            'X-Requested-Auth': 'Digest'
          },
          jar: true,
          body: body
        };

        requestDigest(username, password).request(authConfig, onReadFromBackend);
      };

      // read request buffer
      var buffer = [];
      req.on('data', function (chunk) {
        buffer.push(chunk);
      });
      req.on('end', function () {
        onForwardToBackend(Buffer.concat(buffer).toString());
      });

    }).listen(serverOptions.proxyPort);

    //Setup the proxy routes
    middlewares.push(proxySnippet);

    //Serve static files
    serverOptions.base.forEach(function (base) {
      middlewares.push(serveStatic(base));
    });

    return middlewares;
  }
}
