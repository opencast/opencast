function CaptureAgents(orgEntity) {
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
  var _oneshotFns = [];
  Object.defineProperty(this, 'oneshot', {
    get: function() {
      return _oneshotFns;
    },
    set: function(fn) {
      if (typeof fn == 'function') {
        _oneshotFns.push(fn);
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
      }
    }
  });

  var _authority = null;
  Object.defineProperty(this, 'authority', {
    //This property determines whether the capture-admin url or the agent naming convention
    //is used when determining whether an agent displays in the LTI ui.
    //lti.manage.captureagent.authority = 'agents' || 'names'
    get: function() {
      return _authority;
    },
    set: function(auth) {
      authority = auth;
    }
  });

  orgEntity.on('complete', function(org) {
    _authority = org['lti.manage.captureagent.authority'] || 'names';
    this.fetchAgents(org['lti.manage.captureagent.url'])
      .then(function() {
        this.fetchHumanReadableNames(org['lti.manage.captureagent.names'])
          .then(function(res) {
            if (this.authority === 'names') {
              var keepAgents = Object.keys(res);
              Object.keys(this).forEach(function(key) {
                if (!this.__proto__[key] && keepAgents.indexOf(key) === -1) {
                  delete this[key];
                }
              }.bind(this));
            }
            this.resolved = true;
            this.emit();
          }.bind(this));
      }.bind(this));
  }.bind(this));
}

CaptureAgents.prototype = {
  constructor: CaptureAgents,
  fetchAgents: function(url) {
    return $.Deferred(function(d) {
      $.ajax({
             url: url,
        dataType: 'json'
      })
      .done(function(res) {
        if (url.indexOf('/admin-ng') > -1) {
          this.setFromAdminUIAPI(res);
        }
        else {
          this.setFromExternalAPI(res);
        }
        d.resolve();
      }.bind(this))
      .fail(function(err) {
        d.reject(err);
      });
    }.bind(this)).promise();
  },
  setFromExternalAPI: function(agentResponse) {
    var agents = agentResponse.agents.agent;
    agents = Array.isArray(agents) ? agents : [agents];
    agents.forEach(function(agent) {
      if (agent && agent.capabilities && agent.capabilities.item) {
        var flavors = agent.capabilities.item
                      .filter(function(item) {
                        return item.key.indexOf('flavor') > -1;
                      })
                      .map(function(item) {
                        return item.value;
                      });
        var inputs = agent.capabilities.item
                     .filter(function(item) {
                       return item.key === 'capture.device.names';
                     })
                     .map(function(inputsObj) {
                       return inputsObj.value;
                     })
                     .reduce(function(result, current) {
                       results = current.split(',');
                       return results;
                     }, []);

        this[agent.name] = new CaptureAgent(agent.name, {
                              inputs: inputs,
                             flavors: flavors
                           });
      }
      else if (agent) {
        this[agent.name] = new CaptureAgent(agent.name);
      }
    }.bind(this));
  },
  setFromAdminUIAPI: function(agentResponse) {
    var agents = agentResponse.results;
    agents.forEach(function(agent) {
      if(agent.inputs) {
        var inputs = agent.inputs.map(function(input) {return input.id;});
        var flavors = agent.inputs.map(function(input) {
                        return input.id == 'audio' ? 'presenter/audio' : input.id + '/source'
                      });
        this[agent.Name] = new CaptureAgent(agent.Name, {inputs: inputs, flavors: flavors});
      } else {
        this[agent.Name] = new CaptureAgent(agent.Name);
      }
    }.bind(this));
  },
  fetchHumanReadableNames: function(url) {
    return $.Deferred(function(d) {
      if (!url) {
        this.authority = 'agents';
        for (var key in this) {
          this[key].name = key;
        }
        d.resolve();
        return;
      }

      var reply = {};

      $.ajax({
             url: url + '?' + (new Date((moment().format('YYYY-MM-DDTHH:00:00') + 'Z'))).getTime(),
        dataType: 'json'
      })
      .done(function(res) {
        for (var key in this) {
          if (res.hasOwnProperty(key)) {
            this[key].name = res[key];
          }
          else {
            this[key].name = key;
          }
        }
        reply = res;
      }.bind(this))
      .fail(function(err) {
        this.authority = 'agents';
        for (var key in this) {
          this[key].name = key;
        }
      })
      .always(function() {
        d.resolve(reply);
      })
    }.bind(this));
  },
  orderByName: function() {
    var agents = [];
    for (var key in this) {
      if (!this.__proto__[key]) {
        agents.push({
            id: this[key].id,
          name: this[key].name
        });
      }
    }
    return agents.sort(function(a, b) {
      return (a.name > b.name ? 1 : -1);
    })
    .reduce(function(all, agent) {
      all[agent.id] = agent;
      return all;
    }, {});
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
  once: function(event, fn) {
    if (typeof fn != 'function' || event !== 'complete') {
      return;
    }

    if (this.resolved) {
      fn(this);
      return;
    }

    this.oneshot = fn;
  },
  emit: function() {
    this.oncomplete.forEach(function(fn) {
      fn(this);
    }.bind(this));
    this.oneshot.forEach(function(fn) {
      fn(this);
    }.bind(this));
  }
}

function CaptureAgent(id, opts) {
  this.id = id;
  this.inputs = (opts || {}).inputs;
  this.flavors = (opts || {}).flavors;
}

CaptureAgent.prototype = {
  constructor: CaptureAgent,
  getCalendar: function(opts) {
    return $.Deferred(function(d) {
      $.ajax({
        url: '/recordings/calendars?agentid=' + this.id
      })
      .done(function(res) {
        if (!res) {
          d.resolve({});
          return;
        }

        var fieldMap = {
          DTSTART: 'start',
            DTEND: 'end',
          SUMMARY: 'title'
        };
        var reply = res.split("BEGIN:VEVENT")
                      .reduce(function(calObj, vevent) {
                        calObj.push(
                          vevent.split("\n")
                            .map(function(line) {
                              var fields = line.split(':');
                              return [fields[0], fields[1] || null];
                            })
                            .filter(function(fields) {
                              return Object.keys(fieldMap).indexOf(fields[0]) > -1;
                            })
                            .reduce(function(calEvent, fields) {
                              calEvent[fieldMap[fields[0]]] = fields[1];
                              return calEvent;
                            }, {})
                          );
                          return calObj.filter(function(cals) { return Object.keys(cals).length === Object.keys(fieldMap).length });
                        }, []);

        if (opts && opts.end) {
          if (typeof opts.end == 'string') {
            opts.end = moment(opts.end);
          }

          reply = reply.filter(function(vevent) {
                    return moment(vevent.end, 'YYYYMMDDTHHmmssZ')
                           .isBefore(opts.end);
                  });
        }

        d.resolve(reply);
      })
      .fail(function(err) {
        d.reject(arguments);
      });
    }.bind(this)).promise();
  },
  getSource: function(data) {
    var TYPE = data.multipleSchedule ? 'SCHEDULE_MULTIPLE' : 'SCHEDULE_SINGLE';
    var startTime = (data.startTime || data.start_time).split(':')
                     .reduce(function(total, unit, i) {
                       total += +unit * Math.pow(60, 1 - i);
                       return total;
                     }, 0);
    var endTime = (data.endTime || data.end_time).split(':')
                    .reduce(function(total, unit, i) {
                      total += +unit * Math.pow(60, 1 - i);
                      return total;
                    }, 0);

    if ((endTime - startTime) % 60 === 0) {
      endTime -= 5;
    }

    var startMoment = moment(moment(data.start_date).add(startTime, 'minute'));
    if (startMoment.isBefore(moment().add(10, 'minute'))) {
      startMoment = moment(moment().add(1, 'day').format('YYYY-MM-DD')).add(startTime, 'minute');
    }
    var endMoment = moment(moment(data.multipleSchedule ? data.end_date : data.start_date).add(endTime, 'minute'));

    var source = {
      type: TYPE,
      metadata: {
        device: this.id,
        inputs: (this.inputs || []).join(','),
        start: startMoment.utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z',
        end: endMoment.utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z',
        duration: (endTime - startTime) * 60 * 1000 + ''
      }
    }

    if (data.multipleSchedule) {
      source.metadata.rrule = 'FREQ=WEEKLY;BYDAY=' + data['repeatdays[]'].join(',') + 
                              ';BYHOUR=' + startMoment.utc().hours() +
                              ';BYMINUTE=' + startMoment.utc().minutes()
    }

    return source;
  }
}
