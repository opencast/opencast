function Series(id, isPersonal) {
  this.id = id;

  var _acl = {acl: {ace: [{action: 'read', allow: true, role: 'ROLE_ADMIN'}, {action: 'write', allow: true, role: 'ROLE_ADMIN'}, {action: 'read', allow: true, role: 'ROLE_ANONYMOUS'}]}};
  Object.defineProperty(this, 'acl', {
    get: function() {
      return _acl;
    },
    set: function(acl) {
      _acl = acl;
      _aclResolved = true;
      if (_onFuncs.acl) {
        _onFuncs.acl.forEach(function(fn) {
          fn(acl);
        });
      }
    }
  });

  Object.defineProperty(this, 'notifyDetails', {
    set: function(placebo) {
      _detailsResolved = true;
    }
  });

  var _onFuncs = {};
  Object.defineProperty(this, 'listeners', {
    get: function() {
      return _onFuncs;
    }
  });

  var _detailsResolved = false;
  Object.defineProperty(this, 'resolved', {
    get: function() {
      return _detailsResolved;
    },
    set: function(bool) {
      _detailsResolved = bool;
      if (_onFuncs.details) {
        _onFuncs.details.forEach(function(fn) {
          fn(this);
        });
      }
    }
  });

  var _aclResolved = false;
  Object.defineProperty(this, 'aclResolved', {
    get: function() {
      return _aclResolved;
    },
    set: function(bool) {
      _aclResolved = bool;
    }
  });

  this.setDetails();
  this.setACL();
}

Series.prototype = {
  constructor: Series,
  setACL: function() {
    $.ajax({
           url: '/series/' + this.id + '/acl.json',
      dataType: 'json'
    })
    .then(function(acl) {
      this.acl = acl;
    }.bind(this))
  },
  setDetails: function() {
    $.ajax({
           url: '/series/series.json?seriesId=' + this.id,
      dataType: 'json'
    })
    .then(function(res) {
      if (res.catalogs[0]) {
        var details = res.catalogs[0]['http://purl.org/dc/terms/'];
        for (var key in details) {
          this[key] = details[key][0].value;
        }
      }
      this.resolved = true;
    }.bind(this))
  },
  on: function(prop, fn) {
    if (typeof fn != 'function') {
      return;
    }

    if (prop == 'acl' && this.aclResolved) {
      fn(this.acl);
      return;
    }
    else if (prop == 'details' && this.resolved) {
      fn(this);
      return;
    }

    if (!this.listeners.hasOwnProperty(prop)) {
      this.listeners[prop] = [];
    }
    this.listeners[prop].push(fn);
  }
}
