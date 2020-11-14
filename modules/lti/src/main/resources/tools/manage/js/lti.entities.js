function EntityManager() {
  var _info = {};
  var _onFuncs = {};
  var _entityResolved = false;
  var _entityRequested = false;

  Object.defineProperty(this, 'details', {
    get: function() {
      return _info;
    },
    set: function(details) {
      _info = details;
      for (var key in _onFuncs) {
        var fn = _info[key] || {};
        _onFuncs[key].forEach(function(fn) {
          fn(_info[key]);
        });
      }
    }
  });

  this.on = function(prop, fn) {
    if (typeof fn != 'function') {
      return;
    }
    if (_info[prop]) {
      fn(_info[prop]);
      return;
    }
    if (_entityResolved) {
      fn({});
      return;
    }
    if (!_entityRequested) {
      _entityRequested = true;
      this.requestEntity()
        .then(function(entities) {
          this.details = entities;
          _entityResolved = true;
        }.bind(this));
    }
    if (!_onFuncs.hasOwnProperty(prop)) {
      _onFuncs[prop] = [];
    }
    _onFuncs[prop].push(fn);
  }
}

EntityManager.prototype = {
  constructor: EntityManager,
  requestEntity: function() {
    return $.Deferred(function(d) {
      $.ajax({
             url: '/info/me.json',
        dataType: 'json'
      })
      .done(function(res) {
        d.resolve(res);
      }.bind(this))
      .fail(function(err) {
        console.log('failed getting org defaults', err);
        d.reject(err);
      });
     }.bind(this)).promise();
  }
}

var em = new EntityManager();

function Organization() {
  this.setDefaults();
  this.resolved = false;
  var _oncomplete = [];
  Object.defineProperty(this, 'oncomplete', {
    get: function() {
      return _oncomplete;
    },
    set: function(fn) {
      if (typeof fn == 'function') {
        _oncomplete.push(fn);
      }
    }
  });
}

Organization.prototype = {
  constructor: Organization,
  setDefaults: function() {
    em.on('org', function(org) {
      for (var key in org) {
        this[key] = org[key];
      }

      /*** Set fallback values where necessary ***/
      this.properties['admin.event.new.start_time'] = this.properties['admin.event.new.start_time'] || '07:00';
      this.properties['admin.event.new.end_time'] = this.properties['admin.event.new.end_time'] || '23:00';
      this.properties['admin.event.new.max_duration'] = this.properties['admin.event.new.max_duration'] || '240';
      this.properties['lti.manage.captureagent.url'] = this.properties['lti.manage.captureagent.url'] || '/admin-ng/capture-agents/agents.json?inputs=true';
      this.properties['lti.manage.captureagent.names'] = this.properties['lti.manage.captureagent.names'] || '/mrtg/dashboard/cainfo.json';
      this.properties['lti.manage.downloads'] = this.properties['lti.manage.downloads'] ? this.properties['lti.manage.downloads'] === 'true' : false;
      this.properties['lti.manage.workflow.schedule'] = this.properties['lti.manage.workflow.schedule'] || 'uct-process-for-editing';
      this.properties['lti.manage.workflow.upload'] = this.properties['lti.manage.workflow.upload'] || 'uct-process-upload';
      this.properties['lti.manage.scheduling.api'] = this.properties['lti.manage.scheduling.api'] || 'admin';

      this.resolved = true;
      this.emit();
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
      fn(this.properties);
    }.bind(this));
  }
}

function User(opts) {
  opts = opts || {};
  this.roles = [];
  this.setUser();
  this.setRoles();

  if (opts.isPersonalSeries) {
    this.getAvailableSeries()
      .then(function(series) {
        this.availableSeries = series;
      }.bind(this))
      .fail(function() {
        this.availableSeries = [];
      }.bind(this));
  }
}

User.prototype = {
  constructor: User,
  setUser: function() {
    em.on('user', function(user) {
      for (var key in user) {
        this[key] = user[key];
      }
    }.bind(this));
  },
  setRoles: function() {
    em.on('roles', function(roles) {
      this.roles = roles;
    }.bind(this));
  },
  getAvailableSeries: function() {
    return $.Deferred(function(d) {
      $.ajax({
             url: '/series/series.json?edit=true&fuzzyMatch=false&count=100',
        dataType: 'json'
      }).done(function(json) {
        d.resolve(
          json.catalogs
            .map(function(details) {
              return details['http://purl.org/dc/terms/'];
            })
            .filter(function(isValid) {
              return !!isValid;
            })
            .map(function(series) {
              return {
                   id: series.identifier[0].value,
                title: series.title[0].value
              };
            })
            .sort(function(a, b) {
              if (a.title < b.title) return -1;
              if (a.title > b.title) return 1;
              return 0;
            })
        );
      }).fail(function() {
        d.resolve([]);
      });
    });
  }

}
