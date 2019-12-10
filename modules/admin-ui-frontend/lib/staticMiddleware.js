'use strict';
var serveStatic = require('serve-static');

module.exports = function (grunt, appPath) {
    return function (connect) {
            var middlewares = [
              serveStatic('target/grunt/.tmp'),
              connect().use(
                '/admin-ng',
                serveStatic('./src/test/resources/app/GET/admin-ng')
              ),
              connect().use(
                '/app/styles',
                serveStatic('./app/styles')
              ),
              connect().use(
                '/blacklist',
                serveStatic('./src/test/resources/app/GET/blacklist')
              ),
              connect().use(
                '/bower_components',
                serveStatic('./bower_components')
              ),
              connect().use(
                '/capture-agents',
                serveStatic('./src/test/resources/app/GET/capture-agents')
              ),
              connect().use(
                '/email',
                serveStatic('./src/test/resources/app/GET/email')
              ),
              connect().use(
                '/groups',
                serveStatic('./src/test/resources/app/GET/groups')
              ),
              connect().use(
                '/i18n',
                serveStatic('./src/test/resources/app/GET/i18n')
              ),
              connect().use(
                '/img',
                serveStatic('src/main/webapp/img/')
              ),
              connect().use(
                '/info',
                serveStatic('./src/test/resources/app/GET/info')
              ),
              connect().use(
                '/lib',
                serveStatic('src/main/webapp/scripts/lib')
              ),
              connect().use(
                '/modules',
                serveStatic('src/main/webapp/scripts/modules')
              ),
              connect().use(
                '/public',
                serveStatic('src/main/resources/public/')
              ),
              connect().use(
                '/roles',
                serveStatic('./src/test/resources/app/GET/roles')
              ),
              connect().use(
                '/services',
                serveStatic('./src/test/resources/app/GET/services')
              ),
              connect().use(
                '/shared',
                serveStatic('src/main/webapp/scripts/shared')
              ),
              connect().use(
                '/sysinfo',
                serveStatic('./src/test/resources/app/GET/sysinfo')
              ),
              connect().use(
                '/workflow',
                serveStatic('./src/test/resources/app/GET/workflow')
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
               * This function serves POST / PUT / DELETE mock requests by getting the file content from src/test/resources/app/<method>/<url>.
               */
              function (req, res, next) {
                var path = 'src/test/resources/app/' + req.method + req.url;

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
