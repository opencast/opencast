var express = require('express');
var httpProxy = require('http-proxy');
var createError = require('http-errors');

var app = express();


var proxy = httpProxy.createProxyServer({
  secure:false,
  changeOrigin: true,
  target: 'https://develop.opencast.org'
});
 
function proxyFunc(req, res, next) {
    proxy.web(req, res,
    function(err){
        next(createError(502, err));
    });
}

app.use('/paella/ui', express.static('target/gulp/paella-opencast'));
app.use('/ui/config/paella', express.static('../../etc/ui-config/mh_default_org/paella/'));
app.use(proxyFunc);


app.listen(4000, function () {
  console.log('Example app listening on port 4000!');
});
