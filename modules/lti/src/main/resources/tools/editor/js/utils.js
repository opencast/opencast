function Utils() {
}

Utils.prototype = {
  constructor: Utils,
  hash: function(s) {
    let hash = 0;

    if (!s.length) return 's';
    for (let i = 0, len = s.length; i < len; i++) {
      let char = s.charCodeAt(i);
      hash = ((hash << 5) - hash) + char;
      hash |= 0;
    }

    return Math.abs(hash).toString(36);
  },
  xhr: function(url, opts) {
    return new Promise((resolve, reject) => {
      if (!url) {
        reject('no url');
      }

      const req = new XMLHttpRequest();
      const reqType = (opts ? opts.type || 'GET' : 'GET').toUpperCase();
      req.open(reqType, url, true);
      if (opts && opts.progress) {
        req.upload.onprogress = function(e) {
          if (e.lengthComputable) {
            opts.progress.value = (e.loaded * 100 / e.total) >> 0;
          }
        };
      }
      if (opts && opts.hasOwnProperty('attributes')) {
        for (let key in opts.attributes) {
          req.setAttribute(key, opts.attributes[key]);
        }
      }
      req.onload = function() {
        resolve(req);
      };
      req.onerror = function(e) {
        reject(e);
      };
      req.send(opts ? opts.data || null : null);
    });
  }
}
