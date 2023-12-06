function LTIData() {

  var _completeFns = [];

  Object.defineProperty(this, 'oncomplete', {
    get: function() {
      return _completeFns;
    },
    set: function(fn) {
      if (typeof fn == 'function') {
        _completeFns.push(fn);
      }
    }
  });

  var _resolved = false;
  Object.defineProperty(this, 'resolved', {
    get: function() {
      return _resolved;
    },
    set: function(bool) {
      if (typeof bool == 'boolean') {
        _resolved = bool;
        this.emit();
      }
    }
  });

  this.fetchLTIData();
}

LTIData.prototype = {
  constructor: LTIData,
  fetchLTIData: function(url) {
    url = url || '/lti';
    $.ajax({
      url: url
    })
    .done(function(res) {
      for (var key in res) {
        this[key] = res[key];
      }
    }.bind(this))
    .always(function() {
      this.resolved = true;
    }.bind(this));
  },
  on: function(event, fn) {
    if (typeof fn != 'function' || event !== 'complete') {
      return;
    }

    if (this.resolved) {
      fn(this);
      return;
    }

    this.oncomplete = fn;
  },
  emit: function() {
    this.oncomplete.forEach(function(fn) {
      fn(this);
    }.bind(this));
  }
}
