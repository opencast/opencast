var AnimationLoop = function() {
  this.subscriptions = {};
  this.loop();
}

AnimationLoop.prototype = {
  constructor: AnimationLoop,
  loop: function(timestamp) {
    for (var key in this.subscriptions) {
      let fn = this.subscriptions[key].fn;
      let scope = this.subscriptions[key].scope;
      fn.apply(scope);
    }
    requestAnimationFrame(timestamp => this.loop(timestamp));
  },
  subscribe: function(fnObj) {
    if (fnObj.fn && typeof fnObj.fn == 'function') {
      let randString = null;
      do {
        randString = (Math.random() + 1).toString(36).substring(2, 10);
      } while (this.subscriptions.hasOwnProperty(randString));

      this.subscriptions[randString] = {
        fn: fnObj.fn,
        scope: fnObj.scope
      };

      if (fnObj.cb && typeof fnObj.cb == 'function') {
        fnObj.cb(randString);
      }

      return randString;
    }

    return null;
  },
  unsubscribe: function(key, cb) {
    let result = false;
    if (this.subscriptions.hasOwnProperty(key)) {
      delete this.subscriptions[key];
      result = true;
    }
    if (cb && typeof cb == 'function') {
      cb(result);
    }

    return result;
  }
}
