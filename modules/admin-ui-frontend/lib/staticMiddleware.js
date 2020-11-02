'use strict';
var serveStatic = require('serve-static');

module.exports = function (grunt, appPath) {
    return function (connect) {
            var middlewares = [
      serveStatic('./target/grunt/.tmp'),
              connect().use(
                '/admin-ng',
        serveStatic('./test/app/GET/admin-ng')
              ),
              connect().use(
                '/app/styles',
                serveStatic('./app/styles')
              ),
              connect().use(
                '/blacklist',
        serveStatic('./test/app/GET/blacklist')
              ),
              connect().use(
                '/bower_components',
                serveStatic('./bower_components')
              ),
              connect().use(
                '/capture-agents',
        serveStatic('./test/app/GET/capture-agents')
              ),
              connect().use(
                '/email',
        serveStatic('./test/app/GET/email')
              ),
              connect().use(
                '/groups',
        serveStatic('./test/app/GET/groups')
              ),
              connect().use(
                '/i18n',
        serveStatic('./test/app/GET/i18n')
              ),
              connect().use(
                '/img',
        serveStatic('app/img/')
              ),
              connect().use(
                '/info',
        serveStatic('./test/app/GET/info')
              ),
              connect().use(
                '/lib',
        serveStatic('app/scripts/lib')
              ),
              connect().use(
                '/modules',
        serveStatic('app/scripts/modules')
              ),
              connect().use(
                '/public',
        serveStatic('resources/public/')
              ),
              connect().use(
                '/roles',
        serveStatic('./test/app/GET/roles')
              ),
              connect().use(
                '/services',
        serveStatic('./test/app/GET/services')
              ),
              connect().use(
                '/shared',
        serveStatic('app/scripts/shared')
              ),
              connect().use(
                '/sysinfo',
        serveStatic('./test/app/GET/sysinfo')
              ),
              connect().use(
                '/oc-version',
        serveStatic('./test/app/GET/oc-version')
              ),
              connect().use(
                '/workflow',
        serveStatic('./test/app/GET/workflow')
              ),
              serveStatic(appPath)
            ];

            var responseHeaders = {
              'POST': {
                '/staticfiles': {
                  'Location': 'http://localhost:9001/staticfiles/uuid1'
                }
              }
            }, resolveHeaders = function (method, url) {
              var headers = {}, object;

              if (typeof responseHeaders[method] !== 'undefined' && typeof responseHeaders[method][url] !== 'undefined') {
                // if there are configured response headers in the responseHeaders map
                object = responseHeaders[method][url];
                for (var key in object) {
                  // add all to the resulting headers
                  if (object.hasOwnProperty(key)) {
                    headers[key] = object[key];
                  }
                }
              }
              return headers;
            };

            middlewares.unshift(
              /*
       * This function serves POST / PUT / DELETE mock requests by getting the file content from test/app/<method>/<url>.
               */
              function (req, res, next) {
        var path = 'test/app/' + req.method + req.url;
                if ((req.method === 'POST' || req.method === 'PUT' || req.method === 'DELETE') &&
                  grunt.file.exists(path)) {
                  setTimeout(function () {
                    if (req.method === 'POST') {
                      res.writeHead(201, resolveHeaders(req.method, req.url));
                    }
                    res.end(grunt.file.read(path));
                  }, 1000);
                } else {
                  next();
                }
              }
            );
            return middlewares;
          }
}
