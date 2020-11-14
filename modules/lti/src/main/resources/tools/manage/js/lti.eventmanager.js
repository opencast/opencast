function EventManager(params) {
  this.events = [];
  this.filteredEvents = [];
  this.processingEvents = [];
  this.isProcessing = null;

  this.endpoints = {
        create: null,
        delete: null,
        update: null,
    scheduling: null
  };

  this.selections = [];
  this.eventRemovalCache = [];

  var _metadata,
      _processing,
      _source,
      _assets,
      _defaultWorkflows,
      _org,
      _series,
      _api,
      _filters,
      _captureAgents;

  Object.defineProperty(this, 'externalEndpoints', {
    value: {
      create: '/api/events/',
      delete: '/api/events/%ID%',
      update: '/api/events/%ID%/metadata',
      conflictCheck: '/recordings/conflicts.xml',
      republish: '',
    }
  });
  Object.defineProperty(this, 'adminEndpoints', {
    value: {
               create: '/admin-ng/event/new',
               delete: '/admin-ng/event/%ID%',
               update: '/admin-ng/event/%ID%/metadata',
           scheduling: '/admin-ng/event/%ID%/scheduling',
        conflictCheck: '/admin-ng/event/new/conflicts',
            startTask: '/admin-ng/tasks/new',
              comment: '/admin-ng/event/%ID%/comment',
      addLectureNotes: '/admin-ng/event/%ID%/assets',
          addCaptions: '/admin-ng/event/%ID%/assets',
       updateCaptions: '/admin-ng/event/%ID%/assets',
            seriesACL: '/admin-ng/series/%ID%/access.json',
             eventACL: '/admin-ng/event/%ID%/access'
    }
  });

  Object.defineProperty(this, 'series', {
    get: function() {
      return _series;
    }
  });

  Object.defineProperty(this, 'captureAgents', {
    get: function() {
      return _captureAgents;
    }
  });

  Object.defineProperty(this, 'workflows', {
    get: function() {
      return _defaultWorkflows;
    }
  });

  if (params) {
    _series = params.series;

    if (params.org) {
      params.org.on('complete', function() {
        _defaultWorkflows = {
          schedule: params.org.properties['lti.manage.workflow.schedule'],
            upload: params.org.properties['lti.manage.workflow.upload']
        };

        var api = ['external', 'admin'].filter(function(apiCandidate) {
                    return params.org.properties['lti.manage.scheduling.api'] === apiCandidate
                  })
                  .reduce(function(chosen, current) {
                    return current;
                  }, '') || 'admin';

        this.setApi(api);
        this.endpoints = this[api + 'Endpoints'];
      }.bind(this));
    }
    _captureAgents = (params || {}).captureAgents;

    this.isPersonal = !!params.isPersonal;
  }

  var _onFuncs = {};
  Object.defineProperty(this, 'listeners', {
    get: function() {
      return _onFuncs;
    },
  });

  var _oneShotFuncs = {};
  Object.defineProperty(this, 'oneshot', {
    get: function() {
      return _oneShotFuncs;
    },
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

  _filters = {};
  Object.defineProperty(this, 'filters', {
    get: function() {
      return _filters;
    }
  });

  _filterTimeout = null
  Object.defineProperty(this, 'notifyDelay', {
    get: function() {
      return _filterTimeout;
    },
    set: function(fn) {
      if (typeof fn == 'number') {
        _filterTimeout = fn;
      }
    }
  });

  this.metadataFields = [
    {
      readOnly: false,
            id: 'title',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.TITLE',
          type: 'text',
      required: true,
      fieldMap: ['title']
    },
    {
      readOnly: false,
            id: 'startDate',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.START_DATE',
          type: 'date',
      required: true,
      fieldMap: ['startDate', 'start_date']
    },
    {
      readOnly: false,
            id: 'duration',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.DURATION',
          type: 'text',
      required: false,
      fieldMap: ['duration']
    },
    {
      readOnly: false,
            id: 'location',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.LOCATION',
          type: 'text',
      required: true,
      fieldMap: ['location', 'agent', 'agentid', 'agentId']
    },
    {
      readOnly: false,
            id: 'creator',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.PRESENTERS',
          type: 'mixed_text',
      required: true,
      fieldMap: ['creator', 'presenters']
    },
    {
      readOnly: false,
            id: 'startTime',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.START_TIME',
          type: 'text',
      required: true,
      fieldMap: ['startTime']
    },
    {
      readOnly: false,
            id: 'source',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.SOURCE',
          type: 'text',
      required: true,
      fieldMap: ['source', 'activity_id']
    },
    {
      readOnly: false,
            id: 'captions',
         label: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTIONS',
          type: 'text',
      required: true,
      fieldMap: ['doCaptioning']
    },
  ];

  this.processing = {workflow: 'fast', configuration: {comment: "false", publish: "true"}};

  this.getEvents()
    .then(function() {
      this.resolved = true;
    }.bind(this));

  var _runQueue = {};
  var _queue = {
    add: function() {
      var args = Array.prototype.slice.call(arguments);
      var key = (args[1] || {}).id || Math.toString(36).substring(2,10);
      if (!_runQueue.hasOwnProperty(key)) {
        _runQueue[key] = [];
      }
      _runQueue[key].push({
        fn: args[0],
        args: args.slice(1)
      });
    },
    start: function(opts) {
      //Doing this synchronously, so may be slow
      //Swap it out if it takes too long. So stress test media first chance!!!

      //Set up events
      var notify = {
        success: function() {},
        fail: function() {},
        complete: function() {}
      };
      var errorAlteration = opts.errorAlteration || function(data) { return data };
      if (opts.notify) {
        if (typeof opts.notify == 'string') {
          notify.success = function() {
            var args = [opts.notify].concat(Array.prototype.slice.call(arguments));
            this.emit.apply(this, args);
          }.bind(this);
        }
        else {
          for (var key in opts.notify) {
            var args = [opts.notify[key]].concat(Array.prototype.slice.call(arguments));
            notify[key] = function() {
              var args = [opts.notify[key]].concat(Array.prototype.slice.call(arguments));
              this.emit.apply(this, args);
            }.bind(this);
          }
        }
      }

      return $.Deferred(function(d) {
        var results = {};
        if (Object.keys(_runQueue).length === 0) {
          d.resolve();
          return;
        }

        var errorFlag = false;

        var stepThruQueue = function(memberIndex) {
          var nextKey = (Object.keys(_runQueue))[memberIndex];
          var member = _runQueue[nextKey];
          var totalMemberFns = member.length;

          var runMemberFns = function(fnIndex) {
            var fn = member[fnIndex].fn;
            var args = member[fnIndex].args;

            if (errorFlag) {
              args[1] = errorAlteration(args[1]);
            }

            fn.apply(this, args)
              .then(function(res) {
                errorFlag = false;
                if (!results.hasOwnProperty(nextKey)) {
                  results[nextKey] = [];
                }
                results[nextKey].push(res);
                if (fnIndex + 1 < totalMemberFns) {
                  runMemberFns(++fnIndex);
                }
                else if (memberIndex + 1 < Object.keys(_runQueue).length) {
                  notify.success({current: memberIndex + 1, total: Object.keys(_runQueue).length});
                  stepThruQueue(++memberIndex);
                }
                else {
                  notify.success({current: memberIndex + 1, total: Object.keys(_runQueue).length});
                  _runQueue = {};
                  d.resolve(results);
                }
              }.bind(this))
              .fail(function(err) {
                if (!results.hasOwnProperty(nextKey)) {
                  results[nextKey] = [];
                }
                results[nextKey].push({
                  fn: fnIndex,
                  err: err
                });

                if (opts.proceedWithErrors && fnIndex + 1 < totalMemberFns) {
                  errorFlag = true;
                  runMemberFns(++fnIndex);
                }
                else if (memberIndex + 1 < Object.keys(_runQueue).length) {
                  notify.fail({current: memberIndex + 1, total: Object.keys(_runQueue).length});
                  errorFlag = false;
                  stepThruQueue(++memberIndex);
                }
                else {
                  notify.success({current: memberIndex + 1, total: Object.keys(_runQueue).length});
                  _runQueue = {};
                  d.resolve(results);
                }
              }.bind(this))
          }.bind(this);
          runMemberFns(0);

        }.bind(this);
        stepThruQueue(0);

      }.bind(this)).promise();
    }.bind(this)
  }
  Object.defineProperty(this, 'queue', {
    get: function() {
      return _queue;
    }
  });

  var _refreshInterval = null
  Object.defineProperty(this, 'refreshInterval', {
    get: function() {
      return _refreshInterval;
    },
    set: function(fn) {
      if (typeof fn == 'number') {
        _refreshInterval = fn;
      }
    }
  });

  this.runAfterProcessed = {};
  this.on('event.processing.complete', function(eventId) {
    if (this.runAfterProcessed[eventId]) {
      this.runAfterProcessed[eventId].fn();
      delete this.runAfterProcessed[eventId];
    }
  }.bind(this));
}

EventManager.prototype = {
  constructor: EventManager,
  fetchEvents: function() {
    return this.fetchEventsViaAdmin();
  },
  fetchEventsViaExternal: function() {
    return $.Deferred(function(d) {
      $.ajax({
        url: '/api/event/events.json?filter=series:' + this.series.id
      })
      .done(function(res) {
        d.resolve(res);
      })
      .fail(function() {
        d.resolve([]);
      });
    }.bind(this)).promise();
  },
  fetchEventsViaAdmin: function() {
    return $.Deferred(function(d) {
      var url = '/admin-ng/event/events.json?filter=' + (!this.isPersonal ? 'series:' : 'textFilter:') + this.series.id;
      $.ajax({
        url: url
      })
      .done(function(res) {
        d.resolve(res.results);
      })
      .fail(function() {
        d.resolve([]);
      });
    }.bind(this)).promise();
  },
  getEvents: function() {
    return this.getEventsViaAdmin();
  },
  getEventsViaExternal: function() {
    return $.Deferred(function(d) {
      this.fetchEventViaExternal()
        .then(function(events) {
          this.events = this.filteredEvents = events.map(function(event) {
                                                event.agent_id = event.location || event.agent_id;
                                                return event;
                                              });
          d.resolve(events);
        }.bind(this));
    }.bind(this)).promise();
  },
  getEventsViaAdmin: function() {
    return $.Deferred(function(d) {
      this.fetchEventsViaAdmin()
        .then(function(events) {
          this.events = this.filteredEvents = events.map(function(event) {
                                                event.agent_id = event.location || event.agent_id;
                                                event.status = getStatus(event);

                                                return event;
                                              });
          var eventStatuses = this.events
                                .map(function(event) {
                                  return event.status;
                                })
                                .filter(function(status, i, arr) {
                                  return arr.indexOf(status) === i;
                                });
          this.emit('event.statuses', eventStatuses);
          this.checkProcessingStatus(
            this.events
              .filter(function(event) {
                return event.status === 'Processing';
              })
              .map(function(event) {
                return event.id;
              })
          );
          d.resolve(events);
        }.bind(this));
    }.bind(this)).promise();
  },
  fetchEvent: function(id) {
    return this.fetchEventViaAdmin(id);
  },
  fetchEventViaAdmin: function(id) {
    return $.Deferred(function(d) {
      $.ajax({
        url: '/admin-ng/event/' + id + '?_' + (new Date()).getTime()
      })
      .done(function(res) {
        d.resolve(res);
      })
      .fail(function() {
        d.resolve(null);
      });
    }).promise();
  },
  getEvent: function(id) {
    return this.getEventViaAdmin(id);
  },
  getEventViaAdmin: function(id) {
    return $.Deferred(function(d) {
      this.fetchEvent(id)
        .then(function(event) {
          if (!event) {
            return;
          }

          var currentEvents = this.events.map(function(event) { return event.id });
          var currentStatuses = this.events
                                  .map(function(event) { return getStatus(event) })
                                  .filter(function(status, i, arr) { return arr.indexOf(status) === i });
          var currentEventIndex = currentEvents.indexOf(event.id);
          if (currentEventIndex === -1) {
            event.agent_id = event.location || event.agent_id;
            event.status = getStatus(event);
            this.events.push(event);
          }
          else {
            var updatedEvent = this.events[currentEventIndex];
            for (var key in updatedEvent) {
              updatedEvent[key] = event[key];
            }
            updatedEvent.status = getStatus(event);
          }
          var updatedStatuses = this.events
                                  .map(function(event) { return getStatus(event) })
                                  .filter(function(status, i, arr) { return arr.indexOf(status) === i });

          if (updatedStatuses.length !== currentStatuses.length) {
            this.emit('event.statuses', updatedStatuses);
          }

          this.filterEvents();
          d.resolve(event);
        }.bind(this))
    }.bind(this)).promise();
  },
  checkConflicts: function(data) {
    return this.checkConflictViaAdmin(data);
  },
  checkConflictsViaAdmin: function(data, isOnTheFly, doReturnId) {
    return $.Deferred(function(d) {
      if ((data.hasOwnProperty('presenter') && data.presenter instanceof File) || (data.hasOwnProperty('presentation') && data.presentation instanceof File)) {
        d.resolve();
        return;
      }
      if (!data || (!data.location && !data.device && !data.ca_name) || (!data.start_date && !data.start)) {
        if (!isOnTheFly) {
          d.reject({error: "Location or start date not set, or no file uploaded"});
        }
        else {
          d.resolve();
        }
        return;
      }

      var req = {};

      if (data.device) {
        req = data;
      }
      else {
        var startTime = (data.startTime || data.start_time || moment(data.technical_start).format('HH:mm')).split(':')
                         .reduce(function(total, unit, i) {
                           total += +unit * Math.pow(60, 1 - i);
                           return total;
                         }, 0);
        var endTime = (data.endTime || data.end_time || moment(data.technical_end).format('HH:mm')).split(':')
                        .reduce(function(total, unit, i) {
                          total += +unit * Math.pow(60, 1 - i);
                          return total;
                        }, 0);
        var startMoment = moment(moment(data.start_date, 'YYYY-MM-DD').add(startTime, 'minute'));
        var endMoment = moment(moment(data.multipleSchedule ? data.end_date : data.start_date, 'YYYY-MM-DD').add(endTime, 'minute'));
        req = {
          device: data.location || data.ca_name,
          start: startMoment.utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z',
          end: endMoment.utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z',
          duration: (endTime - startTime) * 60 * 1000 + ''
        };

        if (data.id) {
          req.id = data.id;
        }

        if (data.multipleSchedule) {
          if (!data.end_date || data['repeatdays[]'].length === 0) {
            if (isOnTheFly) {
              d.resolve();
            }
            else {
              d.reject({error: 'Please set end date and/or repeated days'});
            }
            return;
          }
          req.rrule = 'FREQ=WEEKLY;BYDAY=' + data['repeatdays[]'].join(',') +
                      ';BYHOUR=' + startMoment.utc().hours() + ';BYMINUTE=' + startMoment.utc().minutes();
        }
      }
      $.ajax({
        url: this.endpoints.conflictCheck,
        type: 'POST',
        data: {metadata: JSON.stringify(req)},
      }).done(function(res) {
        d.resolve([]);
      }).fail(function(err, statusText) {
        if (statusText === 'timeout') {
          d.resolve();
          return;
        }
        try {
          if (!doReturnId) {
            d.reject(err.responseJSON);
          }
          else {
            var result = {};
            result[data.id] = err.responseJSON;
            d.reject(result);
          }
        } catch(e) {
          d.reject({error: statusText});
        }
      });
    }.bind(this)).promise();
  },
  checkConflictsViaExternal: function(data, isOnTheFly) {
    return $.Deferred(function(d) {
      if (!data || !data.location || !data.start_date) {/* || !data.end_date ||
          !data.startTime || !data.endTime || (data.multipleSchedule && data['repeatdays[]'].length === 0)) {*/
        if (!isOnTheFly) {
          d.reject({error: "Location or start date not set"});
        }
        else {
          d.resolve();
        }
        return;
      }

      var startTime = data.startTime.split(':')
                       .reduce(function(total, unit, i) {
                         total += +unit * Math.pow(60, 1 - i);
                         return total;
                       }, 0);
      var endTime = data.endTime.split(':')
                      .reduce(function(total, unit, i) {
                        total += +unit * Math.pow(60, 1 - i);
                        return total;
                      }, 0);

      var startMoment = moment(moment(data.start_date).add(startTime, 'minute'));
      var endMoment = moment(moment(data.multipleSchedule ? data.end_date : data.start_date).add(endTime, 'minute'));

      var url = this.endpoints.conflictCheck + '?agent=' + data.location +
                '&start=' + startMoment.format('x') + '&end=' + endMoment.format('x');

      if (data.multipleSchedule) {
        url += '&rrule=' + encodeURIComponent('FREQ=WEEKLY;BYDAY=' + data['repeatdays[]'].join(',')) +
               ';BYHOUR=' + startMoment.utc().hours() + ';BYMINUTE=' + startMoment.utc().minutes() +
               '&duration=' + (endTime - startTime) * 60 * 1000 +
               '&timezone=UTC'
      }

      $.ajax({
        url: url,
        dataType: 'text'     //multiple mediapackages in response are joined with '###', breaking xml parsing
      })
      .done(function(res, statusText, jqXhr) {
        if (jqXhr.status === 204) {
          d.resolve([]);
          return;
        }
        else if (jqXhr.status === 200) {
          //1. WHY OH WHY IS A CONFLICT'S RESPONSE 200 INSTEAD OF 409?!?!?!?!?
          //Note to past self: its 200 because when you ASK for conflicts, and there ARE conflicts, you GET the conflicts.
          //                   more importantly, don't conflate opencast conflicts with HTTP processing type conflicts, they are different!
          //2. DC does not always contain proper event times
   /*       var conflicts = res.split('###');
          var conflictArrResponse = [];
          conflicts.forEach(function(conflict) {
            conflictArrResponse.push(
              $.Deferred(function(e) {
                this.parseConflictViaExternal(conflict)
                  .then(function(conflictObj) {
                    e.resolve(conflictObj);
                  });
              }.bind(this)).promise()
            );
          }.bind(this));

          $.when.apply($, conflictArrResponse)
            .done(function() {
              var conflictResults = Array.prototype.slice.call(arguments);
              this.emit('conflict', conflictResults);
              d.reject(conflictResults);
            }.bind(this));*/

          this.captureAgents.once('complete', function(CAs) {
            CAs[data.location].getCalendar({
                 end: endMoment.add(1, 'minute')
              })
              .then(function(events) {
                d.reject(events);
              });
          });
        }
        else if (jqXhr.status === 400) {
          d.reject({error: 'Please complete the form fully prior to submission'});
        }
        else if (jqXhr.status === 419) {
          d.reject({error: 'Please login to perform this action'});
        }
      }.bind(this))
      .fail(function(err, errorText) {
        d.reject(err.status);
      });
    }.bind(this)).promise();
  },
  parseConflictViaExternal: function(conflictXml) {
    return $.Deferred(function(d) {
      if (typeof conflictXml == 'string') {
        conflictXml = (new DOMParser()).parseFromString(conflictXml, 'text/xml');
      }
      var dcUrl = (conflictXml.querySelector('metadata url') || {}).textContent;
      if (!dcUrl) {
        d.resolve();
        return;
      }

      $.ajax({
        url: dcUrl.replace('http:', 'https:')
      })
      .done(function(res) {

        var conflictObj = (res.evaluate('//dcterms:temporal', res, this.nsResolver('dcterms'),
                             XPathResult.ANY_TYPE, null).iterateNext() || {textContent: ''}).textContent
                         .split('; ')
                         .map(function(temporalVal) {
                           return temporalVal.split('=');
                         })
                         .filter(function(temporals) {
                           return ['start', 'end'].indexOf(temporals[0]) > -1;
                         })
                         .reduce(function(obj, temporals) {
                           obj[temporals[0]] = temporals[1];
                           return obj;
                         }, {});

        conflictObj.title = (res.evaluate('//dcterms:title', res, this.nsResolver('dcterms'),
                               XPathResult.ANY_TYPE, null).iterateNext() || {textContent: ''}).textContent;

        d.resolve(conflictObj);
      }.bind(this))
      .fail(function(err) {
        d.resolve();
      });
    }.bind(this)).promise();
  },
  nsResolver: function(prefix) {
    var NSes = {
      mediapackage: 'http://mediapackage.opencastproject.org',
      dcterms: 'http://purl.org/dc/terms/'
    };

    if (Object.keys(NSes).indexOf(prefix) > -1) {
      prefix = NSes[prefix];
    }

    return function() {
      return prefix || null;
    }
  },
  createEvent: function(data) {
    return this.createEventViaAdmin(data);
  },
  createEventViaAdmin: function(data, delayNotify) {
    return $.Deferred(function(d) {
      this.checkConflicts(data)
        .then(function() {
          try {
            var ajaxOpts = {
                      url: this.endpoints.create,
                     type: 'post',
              contentType: false,
              processData: false,
                    cache: false,
            };
            var payload;
            var isUpload = ((!!data.presenter && data.presenter instanceof File) || (!!data.presentation && data.presentation instanceof File));

            try {
              payload = this.createPayload(data, isUpload);
            } catch (e) {
              return d.reject({error: e.message});
            }
            var fd = new FormData();
            fd.append('metadata', JSON.stringify(payload));
            ajaxOpts.data = fd;

            if (isUpload) {
              fd.append('track_presenter.0', data.presenter);
              if (data.presentation && data.presentation instanceof File) {
                fd.append('track_presentation.0', data.presentation);
              }
              ajaxOpts.xhr = this.uploadProgress.bind(this);
            }
            $.ajax(ajaxOpts)
            .done(function(res) {
              res.split(',').forEach(function(id) {
                if (id) {
                  try {
                    this.getEvent(id);
                  } catch(e) {
                    console.log(e);
                  }
                }
              }.bind(this));
              if (!delayNotify) {
                this.emit('event.create.success', res.split(','));
                this.updateNotify = true;
              }
              d.resolve(res);
            }.bind(this))
            .fail(function(err) {
              this.emit('event.create.failed');
              d.reject({error: err});
            }.bind(this));
          } catch(e) {
            d.reject({error: e});
          }
        }.bind(this))
        .fail(function(err, status) {
          if (status === 504) {
            d.resolve();
          }
          else {
            d.reject(err);
          }
        });
    }.bind(this)).promise();
  },
  createEventViaExternal: function(data, skipCheck) {
    return $.Deferred(function(d) {
      if (!data || !data.start_date || !data.location) {
        d.reject({error: 'Please complete form fully before submitting'});
        return;
      }
      var startTimeMins = data.startTime.split(':').reduce(function(mins, unit, i) { mins += +unit * Math.pow(60, 1-i); return mins}, 0);
      var endTimeMins = data.endTime.split(':').reduce(function(mins, unit, i) { mins += +unit * Math.pow(60, 1-i); return mins}, 0);
      var durationMins = endTimeMins - startTimeMins;
      data.startDate = moment(moment(data.start_date).add(startTimeMins, 'minute')).utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z';
      data.startTime = moment(data.startDate).utc().format('HH:mm:ss') + 'Z';
      data.duration = [durationMins / 60 >> 0, durationMins % 60]
                        .map(function(unit) {
                          return (unit < 10 ? '0' : '') + unit;
                        })
                        .join(':') + ':00';

      if (skipCheck) {
          var fd = new FormData();
          fd.append('acl', JSON.stringify(this.series.acl.acl.ace));
          fd.append('metadata', JSON.stringify(this.setEventMetadata(data)));
          fd.append('processing', JSON.stringify(this.processing));
          $.ajax({
            url: this.endpoints.create,
            type: 'POST',
            data: fd,
            processData: false,
            contentType: false
          })
          .done(function(res) {

          })
          .fail(function(err) {
          });
      }
      else
      this.checkConflicts(data)
        .then(function(stuff) {
          var fd = new FormData();
          fd.append('acl', this.series.acl);
          $.ajax({
            url: this.endpoints.create,
            type: post,
            data: fd
          })
          .done(function(res) {
          })
          .fail(function(err) {
          });
        })
        .fail(function(err) {
          d.reject(err);
        }.bind(this));
    }.bind(this)).promise();
  },
  updateEvents: function(eventIds, changes) {
    return $.Deferred(function(d) {
      eventIds.forEach(function(id, idx) {
        var eventChanges = Object.keys(changes)
                             .reduce(function(newEv, key) {
                               newEv[key] = changes[key];
                               return newEv;
                             }, {});

        if (eventChanges.title) {
          eventChanges.title += ' ' + (idx + 1);
        }

        var opts = {
          dropDateOnScheduleFailed: true,
          multipleUpdates: true
        };

        this.updateEvent(id, eventChanges, opts);
      }.bind(this));

      this.queue.start({
          notify: {
            success: 'event.update.progress',
            fail: 'event.update.failed',
          },
          proceedWithErrors: true,
          errorAlteration: function(data) {
            var dropKeys = ['start_date', 'startTime', 'location', 'duration'];
            return Object.keys(data)
                     .filter(function(key) {
                       return dropKeys.indexOf(key) === -1;
                     })
                     .reduce(function(retObj, key) {
                       retObj[key] = data[key];
                       return retObj;
                     }, {});
          }
        })
        .then(function(res) {
          d.resolve(res);
        });
    }.bind(this)).promise();
  },
  updateEvent: function(id, data, opts) {
    return this.updateEventViaAdmin(id, data, opts);
  },
  updateEventViaAdmin: function(id, data, opts) {
    var event = this.getEventDetails(id);
    opts = opts || {};

    if (!event) {
      throw new Error('No such event: ' + id);
      return;
    }

    var bibliographic = {};
    var biblioFields = ['title', 'presenters', 'start_date', 'startTime', 'location', 'duration', 'isPartOf'];

    biblioFields.forEach(function(field) {
      if (data[field]) {
        bibliographic[field] = data[field];
      }
    });

    if (bibliographic.duration) {
      bibliographic.duration = [+bibliographic.duration / 60 >> 0, +bibliographic.duration % 60]
                                .map(function(unit) {
                                  return (unit < 10 ? '0' : '') + unit;
                                })
                                .join(':') + ':00';
    }

    var scheduling = {};
    var schedFields = ['start_date', 'startTime', 'location', 'duration'];
    schedFields.forEach(function(field) {
      if (data[field]) {
        scheduling[field] = data[field];
      }
    });

    if (!opts.multipleUpdates) {
      return $.Deferred(function(d) {
        if (Object.keys(scheduling).length > 0) {
          this.updateScheduling(event, scheduling)
            .then(function() {
              this.updateMetadata(event, bibliographic)
                .then(function() {
                  d.resolve(event.title);
                }.bind(this))
                .fail(function(err) {
                  d.reject({id: event.id, error: err, completed: 'schedule'});
                })
            }.bind(this))
            .fail(function(err) {
              d.reject({id: event.id, error: err});
            });
        }
        else if (event.status !== 'Upcoming') {
          if (opts && opts.hasFile && Object.keys(bibliographic) === 0) return d.resolve(event.title);

          this.updateMetadata(event, bibliographic)
            .then(function() {
              if (!opts || !opts.delayPublish) {
                this.republish(event.id)
                  .then(function() {
                    return d.resolve(bibliographic.title || event.title);
                  }.bind(this))
                  .fail(function(err) {
                    return d.reject(err);
                  });

                return;
              }

              this.emit('event.update.complete', {id: event.id, title: bibliographic.title || event.title, delayPublish: true, isPersonal: this.isPersonal, metadataChanges: bibliographic});
              return d.resolve();
            }.bind(this))
            .fail(function(err) {
              return d.reject(err);
            });
        }
      }.bind(this)).promise();
    }
    else {
      if (Object.keys(scheduling).length > 0) {
        this.queue.add(this.updateScheduling, event, scheduling);
      }
      if (Object.keys(bibliographic).length > 0) {
        this.queue.add(this.updateMetadata, event, bibliographic);
      }
    }
  },
  checkActiveTransaction: function(ev, opts) {
    return $.Deferred(function(d) {
      $.ajax({
        url: '/admin-ng/event/' + (ev.id || ev) + '/hasActiveTransaction',
        dataType: 'json'
      })
      .done(function(res) {
        res.id = ev.id || ev;
        if (opts && opts.target) {
          res.target = opts.target;
        }
        this.emit('event.transaction.active', res);
        return d.resolve(res);
      }.bind(this))
    }.bind(this));
  },
  updateScheduling: function(ev, schedule) {
    return $.Deferred(function(d) {
      var payload = {
        agentId: schedule.location || ev.agent_id,
        start: moment(schedule.start_date || ev.start_date).format('YYYY-MM-DD')
      };

      payload.start = moment(payload.start + 'T' + (schedule.startTime || moment(ev.start_date).format('HH:mm'))).utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z';

      var duration = +(schedule.duration || moment.duration(moment(ev.end_date).diff(moment(ev.start_date))).asMinutes());
      payload.end = moment(payload.start).utc().add(duration, 'minute').format('YYYY-MM-DDTHH:mm:ss') + 'Z';

      $.ajax({
                url: this.endpoints.scheduling.replace('%ID%', ev.id),
               type: 'put',
               data: {scheduling: JSON.stringify(payload)}
      }).done(function(res) {
        d.resolve();
      }).fail(function(err) {
        d.reject(err);
      });
    }.bind(this)).promise()
  },
  updateMetadata: function(ev, metadata) {
    return $.Deferred(function(d) {
      //TODO: setEventMetadata may be assuming creation more than updates
      var payload = this.setEventMetadata(metadata, ev);
      $.ajax({
                url: this.endpoints.update.replace('%ID%', ev.id),
               type: 'put',
               data: {metadata: JSON.stringify(payload)}
      }).done(function(res) {
        setTimeout(function() {
          //TODO: resorting to a delay (server needs to redo DC))?!?! so bad, wheres websockets?
          this.getEvent(ev.id);
        }.bind(this), 3000);
        d.resolve();
      }.bind(this)).fail(function(err) {
        d.reject(err);
      });
    }.bind(this)).promise();
  },
  createPayload: function(data, isUpload) {
    try {
      if ((data.presenter && !(data.presenter instanceof File)) || (data.presentation && !(data.presentation instanceof File))) {
        throw new Error('Please add a file for upload prior to submission');
      }
      else if (!data.presenter && !data.presentation && ((!data.location && !data.ca_name) || !data.start_date)) {
        throw new Error('Please complete form fully prior to submission');
      }

      var isUpload = !!data.presenter || !!data.presentation;

      var payload = {
        processing: this.getProcessing(data, isUpload),
            access: this.series.acl,
            source: isUpload ? {type: 'UPLOAD'} : this.captureAgents[data.location || data.ca_name].getSource(data),
          metadata: this.setEventMetadata(data, null, isUpload),
            assets: {}
      };

      return payload;
    } catch(e) {
       throw new Error(e.message);
    }
  },
  getProcessing: function(data, isUpload) {
    var workflow = this.workflows[isUpload ? 'upload' : 'schedule'];
    var processing = {
      workflow: workflow,
      configuration: {
        comment: 'false',
        publish: 'true',
      }
    };
    if (isUpload && data.hasSlides) {
      processing.configuration.hasSlides = "true";
    }
    if (data.presentation && data.presentation instanceof File) {
      processing.configuration.hasPresentation = "true";
    }
    if (isUpload && !data.doCaptioning) {
      processing.configuration.doCaptioning = "false";
    }
    return processing;
  },
  setEventMetadata: function(data, event, isUpload) {
    var metadata = [{
      flavor: 'dublincore/episode',
       title: 'EVENTS.EVENTS.DETAILS.CATALOG.EPISODE',
      fields: [
        {
             id: 'isPartOf',
           type: 'text',
          value: this.series.id
        }
      ]
    }];
    var hasTime = false;
    var hasDate = false;
    var hasLicense = false;

    if (data.isPartOf && this.isPersonal) {
      metadata[0].fields[0].value = data.isPartOf;
    }
    for (var key in data) {
      var field = this.metadataFields
                    .filter(function(testField) {
                      return testField.fieldMap.indexOf(key) > -1;
                    })
                    .reduce(function(obj, currentField) {
                      return currentField;
                    }, null);
      if (field) {
        field.value = field.type === 'mixed_text' ? data[key].split("\n") : data[key];
        if (key === 'start_date') {
          field.value = moment(data[key] + 'T' + (data.startTime || moment(event ? event.technical_start : data[key]).format('HH:mm')))
                          .utc().format('YYYY-MM-DDTHH:mm:ss') + '.000Z';

          if (!event && moment(field.value).isBefore(moment().add(5, 'minute'))) {
            if (data.multipleSchedule) {
              field.value = moment(moment().add(1, 'day').format('YYYY-MM-DD') + 'T' +
                                   (data.startTime || moment(event ? event.technical_start : data[key]).format('HH:mm'))
                            ).utc().format('YYYY-MM-DDTHH:mm:ss') + '.000Z';
            }
            else if (!data.presenter && data.presenter instanceof File) {
              throw new Error("Start time before/too close to current time");
            }
          }
        }
        if (key === 'duration' && +(data[key])%60 === 0) {
          field.value = +(data[key]) - 5 + "";
        }
        metadata[0].fields.push(field);
      }
      if (key === 'startTime') {
        hasTime = true;
      }
      if (key === 'license') {
        hasLicense = true;
      }
      if (key === 'start_date') {
        hasDate = true;
      }
    }
    if (hasTime && !hasDate) {
      metadata[0].fields.push({
        id: 'startDate',
        value: moment(moment(event.technical_start).format('YYYY-MM-DD') + 'T' + data.startTime).utc().format('YYYY-MM-DDTHH:mm:ss') + '.000Z',
        label: 'EVENTS.EVENTS.DETAILS.METADATA.START_DATE',
        type: 'date'
      })
    }

    if (isUpload) {
        metadata[0].fields.push({
            id: "location",
            value: "upload",
            label: "EVENTS.EVENTS.DETAILS.METADATA.LOCATION",
            type: "text",
        })
    }

    if (!hasLicense) {
      metadata[0].fields.push({
        id: 'license',
        value: this.series.license,
        label: 'EVENTS.EVENTS.DETAILS.METADATA.LICENSE',
        type: 'text'
      })
    }

    return metadata;
  },
  createSchedule: function(schedules) {
    var schedProm = [];

    schedules.forEach(function(schedule, i) {
      schedProm.push(
        $.Deferred(function(d) {
          schedule.title = [schedule.course, schedule.class_section, schedule.start_time].join(' ');
          schedule.multipleSchedule = true;

          if (moment(schedule.start_date + 'T' + schedule.start_time).add(30, 'minute').isBefore(moment())) {
            schedule.start_date = moment().add(1, 'day').format('YYYY-MM-DD');
          }

          schedule['repeatdays[]'] = Object.keys(schedule.days)
                                       .filter(function(day) {
                                         return schedule.days[day];
                                       })
                                       .map(function(day) {
                                         return day.toUpperCase().substring(0, 2);
                                       });

          schedule.activity_id = 'SP[' + schedule.activity_id + ']';
          schedule.end_time = moment(schedule.end_date + 'T' + schedule.end_time).subtract(5, 'minute').format('HH:mm');

          this.createEvent(schedule)
            .then(function(res) {
              d.resolve(res);
            })
            .fail(function(err) {
              d.resolve({error: err, schedule: schedule});
            });
        }.bind(this)).promise()
      );
    }.bind(this));
    this.emit('event.schedule.submitted');
    this.pollEvents();
  },
  pollEvents: function() {
    if (this.refreshInterval) {
      clearTimeout(this.refreshInterval);
      this.refreshInterval = null;
    }

    this.refreshCount = this.events.length;
    this.refreshInterval = setInterval(function() {
      this.getEvents()
        .then(function() {
          if (this.refreshCount === this.events.length) {
            clearInterval(this.refreshInterval);
            if (!this.updateNotify) {
              this.emit('event.create.success', []);
            }
            this.updateNotify = null;
            return;
          }
          this.refreshCount = this.events.length;
          this.emit('event.create.partial', this.events);
          this.filterEvents();
        }.bind(this))
        .fail(function() {
          clearInterval(this.refreshInterval);
        }.bind(this))
    }.bind(this), 10000);
  },
  uploadProgress: function() {
    var xhr = new XMLHttpRequest();

    xhr.upload.addEventListener('progress', function(e) {
      if (e.lengthComputable) {
        this.emit('event.upload.progress', {loaded: e.loaded, total: e.total});
        if (e.loaded === e.total) {
          this.emit('event.upload.complete', {loaded: e.loaded, total: e.total});
        }
      }
    }.bind(this), false);

    return xhr;
  },
  retract: function(id, opts) {
    return $.Deferred(function(d) {
      opts = opts || {};
      opts.id = id;
      var retractStatus = "event.retract.complete";

      this.startTask(id, 'retract', {
        retractFromEngage: "true",
        retractFromOaiPmh: "true",
        retractFromAws: "false",
        retractFromApi: "true",
        retractPreview: "true",
        retractFromYoutube:"false"
      })
      .done(function() {
        this.emit('event.transaction.complete', opts);
      }.bind(this))
      .fail(function() {
        retractStatus = 'event.retract.failed';
        this.emit('event.transaction.failed', opts);
      }.bind(this))
      .always(function() {
        this.emit(retractStatus, opts);
        this.checkProcessingStatus(id);
        opts.retract = retractStatus;
        d.resolve(opts);
      }.bind(this));

      this.emit('event.transaction.started', opts);
    }.bind(this)).promise();
  },
  republish: function(id, opts) {
    return $.Deferred(function(d) {
      opts = opts || {};
      opts.id = id;
      var republishStatus = "event.republish.complete";

      this.startTask(id, 'republish-metadata', {
        publishToEngage: "true",
        publishToOaiPmh: "false"
      })
      .done(function() {
        this.emit('event.transaction.complete', opts);
      }.bind(this))
      .fail(function() {
        republishStatus = 'event.republish.failed';
        this.emit('event.transaction.failed', opts);
      }.bind(this))
      .always(function() {
        this.emit(republishStatus, opts);
        this.checkProcessingStatus(id);
        opts.republish = republishStatus;
        d.resolve(opts);
      }.bind(this));

      this.emit('event.transaction.started', opts);
    }.bind(this)).promise();
  },
  publish: function(id, opts) {
    return $.Deferred(function(d) {
      opts = opts || {id: id};
      var republishStatus = "event.publish.complete";

      this.startTask(id, 'uct-publish-after-edit', {
        publishToEngage: "true",
        publishToOaiPmh: "true"
      })
      .done(function() {
        this.emit('event.transaction.complete', opts);
      }.bind(this))
      .fail(function() {
        republishStatus = 'event.publish.failed';
        this.emit('event.transaction.failed', opts);
      }.bind(this))
      .always(function() {
        this.emit(republishStatus, opts);
        this.checkProcessingStatus(id);
        opts.republish = republishStatus;
        d.resolve(opts);
      }.bind(this));

      this.emit('event.transaction.started', opts);
    }.bind(this)).promise();
  },
  comment: function(id, comment, opts) {
    return $.Deferred(function(d) {
      var fd = new FormData();
      fd.append('text', comment);
      fd.append('reason', 'EVENTS.EVENTS.DETAILS.COMMENTS.REASONS.CUTTING');

      $.ajax({
        url: this.endpoints.comment.replace(/%ID%/g, id),
        type: 'post',
        data: fd,
        processData: false,
        contentType: false
      })
      .done(function() {
        d.resolve();
      })
      .fail(function() {
        d.reject();
      });
    }.bind(this)).promise();
  },
  startTask: function(id, workflow, config) {
    return $.Deferred(function(d) {
      var taskPayload = {
        workflow: workflow,
        configuration: {}
      };
      taskPayload.configuration[id] = config;
      $.ajax({
        url: this.endpoints.startTask,
        type: 'post',
        data: {metadata: JSON.stringify(taskPayload)}
      })
      .done(function() {
        d.resolve();
      })
      .fail(function() {
        d.reject();
      })
    }.bind(this)).promise();
  },
  addLectureNotes: function(id, changes, opts) {
    var payload = {"assets":{"options":[{"id":"attachment_class_handout_notes","type":"attachment","flavorType":"attachment","flavorSubType":"notes","title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.CLASS_HANDOUT_NOTES"}]},"processing":{"workflow":"publish-uploaded-assets","configuration":{"downloadSourceflavorsExist":"true","download-source-flavors":"attachment/notes"}}};

    return $.Deferred(function(d) {
      this.updateEvent(id, changes, {delayPublish: true})
        .then(function() {
          var fd = new FormData();
          fd.append('metadata', JSON.stringify(payload));
          fd.append('attachment_class_handout_notes.0', changes['attachment/notes']);

          $.ajax({
            url: this.endpoints.addLectureNotes.replace(/%ID%/g, id),
            type: 'post',
            data: fd,
            processData: false,
            contentType: false,
                  cache: false,
          })
          .done(function() {
            d.resolve();
          }.bind(this))
          .fail(function() {
            d.reject();
          })
          .always(function() {
            if (this.processingEvents.indexOf(id) === -1) {
              this.processingEvents.push(id);
            }
          }.bind(this));
        }.bind(this))
        .fail(function() {
          d.reject();
        });
    }.bind(this)).promise();
  },
  addCaptions: function(id, changes, opts) {
    var payload = {"assets":{"options":[{"id":"attachment_captions_webvtt","type":"attachment","flavorType":"text","flavorSubType":"vtt","displayOrder":3,"title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.CAPTIONS_WEBVTT"}]},"processing":{"workflow":"uct-publish-updated-transcripts","configuration":{"downloadSourceflavorsExist":"true","download-source-flavors":"text/vtt"}}};

    return $.Deferred(function(d) {
      this.updateEvent(id, changes, {delayPublish: true})
        .then(function() {
          var fd = new FormData();
          fd.append('metadata', JSON.stringify(payload));
          fd.append('attachment_captions_webvtt.0', changes['text/vtt']);

          $.ajax({
            url: this.endpoints.addCaptions.replace(/%ID%/g, id),
            type: 'post',
            data: fd,
            processData: false,
            contentType: false,
                  cache: false,
          })
          .done(function() {
            d.resolve();
          }.bind(this))
          .fail(function() {
            d.reject();
          })
          .always(function() {
            if (this.processingEvents.indexOf(id) === -1) {
              this.processingEvents.push(id);
            }
          }.bind(this));
        }.bind(this))
        .fail(function() {
          d.reject();
        });
    }.bind(this)).promise();
  },
  updateCaptions: function(id, changes) {
    var mType = changes['text/vtt'].type,
        fType = mType.substring(0, mType.indexOf('/')),
        fSubType = mType.substring(mType.lastIndexOf('/') + 1);
    var payload = {"assets":{"options":[{"id":"attachment_captions_webvtt","type":"attachment","flavorType":fType,"flavorSubType":fSubType,"displayOrder":3,"title":"EVENTS.EVENTS.NEW.UPLOAD_ASSET.OPTION.CAPTIONS_WEBVTT"}]},"processing":{"workflow":"uct-publish-updated-transcripts","configuration":{"downloadSourceflavorsExist":"true","download-source-flavors":mType}}};
  
    return $.Deferred(function(d) {
      this.updateEvent(id, changes, {delayPublish: true})
        .then(function() {
          var fd = new FormData();
          fd.append('metadata', JSON.stringify(payload));
          fd.append('attachment_captions_webvtt.0', changes['text/vtt']);

          $.ajax({
            url: this.endpoints.updateCaptions.replace(/%ID%/g, id),
            type: 'post',
            data: fd,
            processData: false,
            contentType: false,
                  cache: false,
          })
          .done(function() {
            d.resolve();
          }.bind(this))
          .fail(function() {
            d.reject();
          })
          .always(function() {
            if (this.processingEvents.indexOf(id) === -1) {
              this.processingEvents.push(id);
            }
          }.bind(this));
        }.bind(this))
        .fail(function() {
          d.reject();
        });
    }.bind(this)).promise();
  },
  getEventDetails: function(id) {
    return JSON.parse(JSON.stringify(this.events))
             .filter(function(event) {
               return event.id === id;
             })
             .reduce(function(collect, event) {
               return event;
             }, null);
  },
  getEventsFromSelections: function(events) {
    events = events || this.selections;
    return JSON.parse(JSON.stringify(this.events))
             .filter(function(event) {
               return events.indexOf(event.id) > -1;
             });
  },
  getCommonSelections: function() {
     var fields = ['presenters', 'startTime', 'endTime', 'startDate', 'endDate', 'location'];
     var selectedEvents = this.events
                            .filter(function(event) {
                              return this.selections.indexOf(event.id) > -1;
                            }.bind(this));
     var common = selectedEvents
                    .map(function(ev) {
                       var tmpObj = JSON.parse(JSON.stringify(ev));
                      tmpObj.startTime = moment(tmpObj.start_date).format('HH:mm');
                      tmpObj.endTime = moment(tmpObj.end_date).format('HH:mm');
                      tmpObj.startDate = moment(tmpObj.start_date).format('ddd DD MMM, YYYY');
                      tmpObj.endDate = moment(tmpObj.end_date).format('HH:mm');
                      tmpObj.title = !isNaN(tmpObj.title.substring(tmpObj.title.lastIndexOf(' ') + 1)) ?
                                       tmpObj.title.substring(0, tmpObj.title.lastIndexOf(' ')) :
                                       tmpObj.title;

                      return tmpObj;
                    })
                    .reduce(function(base, ev) {
                      if (Object.keys(base) === 0) {
                        return ev;
                      }

                      for (var key in base) {
                        base[key] = base[key] === ev[key] ? base[key] : 'Multiple';
                      }

                      return base;
                    }) || {};

     common.id = this.selections.join(',');

     if (Object.keys(common) === 0) {
       fields.forEach(function(field) {
         common[field] = 'Multiple';
       });
     }

     return common;
  },
  getEventAssets: function(id, opts) {
    return $.Deferred(function(d) {
      var msg = {
        id: id
      };

      if (opts && opts.target) {
        msg.target = opts.target;
      }

      var event = this.events
                    .filter(function(event) {
                      return event.id === id
                    })
                    .reduce(function(collect, event) {
                      collect = event;
                      return collect;
                    }, null);

      if (!event) {
        return this.emit('event.media.notfound', msg);
     }

      if (event.media) {
        msg.media = event.media;
        return this.emit('event.media', msg);
      }

      this.getEventSearchInfo(id)
        .then(function(media) {
          this.events
            .filter(function(event) {
              return event.id === id
            })
            .forEach(function(event) {
              event.media = media;
            });

          msg.media = event.media;
          this.emit('event.media', msg);
          d.resolve(msg);
        }.bind(this))
        .fail(function(err) {
          msg.error = err;
          d.reject(msg);
          return this.emit('event.media.error', msg);
        });
    }.bind(this)).promise();
  },
  getEventSearchInfo: function(id) {
    return $.Deferred(function(d) {
      $.ajax({
        url: '/search/episode.json?limit1&id=' + id,
        dataType: 'json'
      }).done(function(res) {
        var query = res['search-results'];
        var response = [];

        if (query.total === 0) {
          return d.reject("cannot find");
        }
        if (!Array.isArray(query.result.mediapackage.media.track)) {
          if (!query.result.mediapackage.media.track.video) {
            return d.reject("no video");
          }

          response.push({
            type: query.result.mediapackage.media.track.type,
             url: query.result.mediapackage.media.track.url
          });
          return d.resolve(response);
        }

        response = query.result.mediapackage.media.track
                     .filter(function(track) {
                       return track.video;
                     })
                     .map(function(track) {
                       let frameArea = track.video.resolution.split('x')
                                         .map(function(dim) { return +dim })
                                         .reduce(function(product, dim) { product *= dim; return product });

                       let quality = '(Low quality)';
                       if (frameArea >= 2073600) {
                         quality = '(High quality)';
                       }
                       else if (frameArea >= 800000) {
                         quality = '(Medium quality)';
                       }

                       let videoName = 'Presenter';
                       if (track.type.indexOf('presentation2') > -1) {
                         videoName = 'Presentation 2';
                       }
                       else if (track.type.indexOf('presentation') > -1) {
                         videoName = 'Presentation';
                       }
                       else if (track.type.indexOf('composite') > -1) {
                         videoName = 'Side by side';
                       }
                       else if (track.type.indexOf('pic-in-pic') > -1) {
                         videoName = 'Picture-in-Picture';
                       }

                       return {
                            type: track.type,
                            name: videoName + ' ' + quality,
                             url: track.url
                       };
                     })
                     .reduce(function(arr, track) {
                       arr.push(track);
                       return arr;
                     }, []);

        var notes = query.result.mediapackage.attachments.attachment
                      .filter(function(attachment) {
                        return attachment.type === 'attachment/notes' || attachment.type === 'attachment/part+notes';
                      })
                      .map(function(attachment) {
                        return {
                          mimeType: attachment.mimetype,
                              type: attachment.type,
                               url: attachment.url
                        };
                      })

        response = response.concat(notes);
        d.resolve(response);
      }).fail(function(err) {
        d.reject(err);
      })
    }).promise();
  },
  setApi: function(chosenApi) {
    if (!chosenApi) {
      return;
    }

    chosenApi = chosenApi.charAt(0).toUpperCase() + chosenApi.substring(1);
    var availableApis = [
      'fetchEvents',
      'getEvents',
      'checkConflicts',
      'createEvent',
      'updateEvent'
    ];

    availableApis.forEach(function(api) {
      if (this.__proto__.hasOwnProperty(api + 'Via' + chosenApi)) {
        this.__proto__[api] = this.__proto__[api + 'Via' + chosenApi];
      }
    }.bind(this));
  },
  addFilter: function(filter, val) {
    if (filter === 'day') {
      if (!this.filters.day) {
        this.filters.day = [];
      }
      if (this.filters.day.indexOf(val) === -1) {
        this.filters.day.push(val);
      }
    }
    else if (filter === 'status') {
      var allUniqueStatuses = this.events
                                .map(function(event) {
                                  return event.status;
                                })
                                .filter(function(status, index, arr) {
                                  return arr.indexOf(status) === index;
                                });

      if (!this.filters.status) {
        this.filters.status = [];
      }
      if (this.filters.status.indexOf(val) === -1) {
        this.filters.status.push(val);
      }
      if (this.filters.status.length === allUniqueStatuses.length) {
        this.removeFilter('status');
        return;
      }
    }
    else {
      this.filters[filter] = val;
    }
    this.filterEvents();
    this.emit('filter.' + filter, this.filters[filter], filter);
    this.emit('isfiltered', Object.keys(this.filters).length > 0);
  },
  removeFilter: function(filter, val) {
    var isArrayType = ['day', 'status'].indexOf(filter) > -1;
    if (isArrayType && val) {
      this.filters[filter] = this.filters[filter].filter(function(filterVal) {
                               return filterVal !== val;
                             });

      if (this.filters[filter].length === 0) {
        delete this.filters[filter];
      }
    }
    else if (!val) {
      delete this.filters[filter];
    }

    this.filterEvents();
    this.emit('filter.' + filter, this.filters[filter] || null, filter);
    this.delayFilterNotify();
  },
  removeFilters: function() {
    for (var key in this.filters) {
      this.removeFilter(key);
    }
  },
  delayFilterNotify: function() {
    if (this.notifyDelay) {
      clearTimeout(this.notifyDelay);
      this.notifyDelay = null;
    }

    this.notifyDelay = setTimeout(function() {
      this.emit('isfiltered', Object.keys(this.filters).length > 0);
      this.notifyDelay = null;
    }.bind(this), 0);
  },
  filterEvents: function() {
    this.filteredEvents = this.events;

    for (var key in this.filters) {
      var filterVal = this.filters[key];

      switch(key) {
        case 'startTime':
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  var localTime = moment(event.start_date).local();

                                  return localTime.hours() > filterVal[0] || (localTime.hours() === filterVal[0] && localTime.minutes() >= filterVal[1]);
                                });
          break;

        case 'endTime':
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  var localTime = moment(event.start_date).local();

                                  return localTime.hours() < filterVal[0] || (localTime.hours() === filterVal[0] && localTime.minutes() <= filterVal[1]);
                                });
          break;

        case 'startDate':
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  var timestamp = new Date((event.start_date.split('T'))[0]);
                                  var filterDate = new Date(filterVal);

                                  return timestamp >= filterDate;
                                });
          break;

        case 'endDate':
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  var timestamp = new Date((event.start_date.split('T'))[0]);
                                  var filterDate = new Date(filterVal);

                                  return timestamp <= filterDate;
                                });
          break;

        case 'day':
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  var eventDay = moment(event.start_date).format('ddd');

                                  return this.filters.day.indexOf(eventDay) > -1;
                                }.bind(this));
          break;

        case 'status':
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  if (this.filters.hasOwnProperty('status')) {
                                      return this.filters.status.indexOf(event.status) > -1;
                                  }

                                  return true;
                                }.bind(this));
          break;

        default:
          this.filteredEvents = this.filteredEvents.filter(function(event) {
                                  var text = JSON.stringify(event).toLowerCase();
                                  return text.indexOf(filterVal.toLowerCase()) > -1;
                                });
          break;
      }
    }
    this.emit('filtered', this.filteredEvents);
  },
  addSelection: function(eventStrOrArr) {
    var eventArr = Array.isArray(eventStrOrArr) ? eventStrOrArr : [eventStrOrArr];

    this.selections.push.apply(
      this.selections,
      this.events.filter(function(event) {
        return eventArr.indexOf(event.id) > -1;
      })
      .map(function(event) {
        return event.id;
      })
    );

    this.emit('selection',
      this.events.filter(function(event) {
        return this.selections.indexOf(event.id) > -1;
      }.bind(this))
    );
  },
  removeSelection: function(eventStrOrArr) {
    var eventArr = Array.isArray(eventStrOrArr) ? eventStrOrArr : [eventStrOrArr];

    this.selections = this.selections.filter(function(id) {
                        return eventArr.indexOf(id) === -1;
                      });

    this.emit('selection',
      this.events.filter(function(event) {
        return this.selections.indexOf(event.id) > -1;
      }.bind(this))
    );
  },
  removeAllSelections: function() {
    this.selections = [];
    this.emit('selection', []);
  },
  logEventRemoval: function(eventStrOrArr) {
    this.eventRemovalCache = Array.isArray(eventStrOrArr) ? eventStrOrArr : [eventStrOrArr];

    this.emit('promptremoval',
      this.events.filter(function(event) {
        return this.eventRemovalCache.indexOf(event.id) > -1
      }.bind(this))
      .map(function(event) {
        return event.title;
      })
    );
  },
  deleteEvent: function(eventId) {
    return $.Deferred(function(d) {
      var result = {id: eventId};
      $.ajax({
        url: this.endpoints.delete.replace(/%ID%/g, eventId),
        type: 'DELETE'
      }).done(function(body, statusText, xhr) {
        result.success = xhr.status < 300;
      }).fail(function(err) {
        result.success = false;
      }).always(function() {
        d.resolve(result);
      });
    }.bind(this)).promise();
  },
  deleteEvents: function(eventIdsArr) {
    eventIdsArr = eventIdsArr || this.eventRemovalCache;
    var deleteReqs = [];

    eventIdsArr.forEach(function(eventId) {
      deleteReqs.push(this.deleteEvent(eventId));
    }.bind(this));

    $.when.apply($, deleteReqs).done(function() {
      //function arguments contains the results of the $.when.apply
      var results = Array.prototype.slice.call(arguments);
      var successes = [];

      this.eventRemovalCache = results.filter(function(result) {
                                 if (result.success) {
                                   successes.push(result.id);
                                   return false;
                                 }
                                 else {
                                   return true;
                                 }
                               })
                               .map(function(result) {
                                 return result.id;
                               });

      this.emit('event.delete.fail',this.eventRemovalCache);
      this.emit('event.delete.success', successes)

      this.getEvents()
        .then(function() {
          this.filterEvents();
        }.bind(this));

      this.removeSelection(successes);

    }.bind(this));
  },
  deleteEventsSynchronously: function(eventIdsArr) {
    var success = [];
    var fail = [];
    var totalEvents = eventIdsArr.length;

    var performDeleteOnIndex = function(i) {
      this.deleteEvent(eventIdsArr[i])
        .then(function(result) {
          if (result.success) {
            success.push(result.id);
          }
          else {
            fail.push(result.id);
          }

          if (i + 1 < totalEvents) {
            performDeleteOnIndex(++i);
          }
          else {
            this.emit('event.delete.success', success);
            this.emit('event.delete.fail', fail);
          }
        }.bind(this));
    }.bind(this);
    performDeleteOnIndex(0);
  },
  checkProcessingStatus: function(ids) {
    ids = Array.isArray(ids) ? ids : [ids];
    ids.forEach(function(id) {
      if (this.processingEvents.indexOf(id) === -1) {
        this.processingEvents.push(id);
      }
      if (!this.isProcessing) {
        this.isProcessing = setInterval(function() {
          this.processingEvents.forEach(function(eventId) {
            this.getEvent(eventId)
              .then(function(updatedEvent) {
                if (!updatedEvent || updatedEvent.workflow_state !== 'RUNNING') {
                  this.processingEvents.splice(this.processingEvents.indexOf(updatedEvent.id), 1);
                  this.emit('event.processing.complete', updatedEvent.id);
                }

                if (this.processingEvents.length === 0) {
                  clearInterval(this.isProcessing);
                  this.isProcessing = null;
                }
              }.bind(this));
          }.bind(this));
        }.bind(this), 10000);
      }
    }.bind(this));
  },
  addAccessRoles: function() {
    /**
    * Accepts a list of arguments where:
    * 1. first argument is the event id
    * 1. if an argument is an array, it contains a list of roles for rw access,
    * 2. if an argument is a string, its a series id (i.e. fetch and append those series acls)
    */
    var args = Array.prototype.slice.call(arguments);
    var eventId = args.shift();
    var rolesArr = args.filter(function(arg) { return Array.isArray(arg); }).reduce(function(arr, roles) { return arr.length ? arr : roles; }, []);
    var seriesArr = args.filter(function(arg) { return typeof arg == 'string' });
    seriesArr = seriesArr.filter(function(seriesId, index) {
                  return seriesArr.indexOf(seriesId) === index;
                });

    var access = {acl: {ace: []}};
    //Get ACLs for series in seriesArr
    var aclProms = [];

    seriesArr.forEach(function(seriesId) {
      aclProms.push(
        this.getSeriesAcl(seriesId)
      );
    }.bind(this));

    $.when.apply($, aclProms)
      .done(function() {
        var resolvedAcls = Array.prototype.slice.call(arguments);
        resolvedAcls.forEach(function(seriesAcl) {
          if (seriesAcl && JSON.stringify(access.acl.ace).indexOf(JSON.stringify(seriesAcl.acl.ace)) === -1) {
            access.acl.ace = [].concat(access.acl.ace, seriesAcl.acl.ace);
          }
        });

        rolesArr.forEach(function(role) {
          ['read', 'write'].forEach(function(action) {
            access.acl.ace.push({
               allow: true,
                role: role,
              action: action
            });
          });
        });

        this.updateEventAcl(eventId, access)
          .then(function() {
            this.emit('event.update.acl', {id: eventId});
          }.bind(this))
          .fail(function() {
            console.log('failed to update acl');
          });
      }.bind(this));
  },
  getSeriesAcl: function(seriesId) {
    return $.Deferred(function(d) {
      $.ajax({
             url: this.endpoints.seriesACL.replace(/%ID%/g, seriesId),
        dataType: 'json'
      })
      .done(function(access) {
         d.resolve(JSON.parse(access.series_access.acl));
      })
      .fail(function() {
        d.resolve(null);
      });
    }.bind(this)).promise();
  },
  updateEventAcl: function(eventId, access) {
    return $.Deferred(function(d) {
      var fd = new FormData();
      fd.append('acl', JSON.stringify(access));
      $.ajax({
                url: this.endpoints.eventACL.replace(/%ID%/g, eventId),
               type: 'post',
               data: fd,
        contentType: false,
        processData: false,
              cache: false,
      }).done(function() {
        d.resolve();
      }).fail(function() {
        d.reject();
      });
    }.bind(this)).promise();
  },
  on: function(event, opts, fn) {
    if (typeof opts == 'function') {
      fn = opts;
    }

    if (typeof fn != 'function') {
      return this;
    }

    var target = typeof opts == 'object' ? (opts.target ? this[opts.target] : this) : this;
    if (event === 'complete' && this.resolved) {
      fn(target);
    }

    var events = event.split(',');

    if (typeof fn == 'function') {
      events.map(function(ev) { return ev.trim() }).forEach(function(ev) {
        if (!this.listeners.hasOwnProperty(ev)) {
          this.listeners[ev] = [];
        }
        this.listeners[ev].push({fn: fn, opts: opts});
      }.bind(this));
    }

    return this;
  },
  once: function(event, opts, fn) {
    if (typeof opts == 'function') {
      fn = opts;
    }

    if (typeof fn != 'function') {
      return this;
    }

    var target = typeof opts == 'object' ? (opts.target ? this[opts.target] : this) : this;
    if (event === 'complete' && this.resolved) {
      fn(target);
      return;
    }

    if (event === 'selection') {
      fn(this.events.filter(function(ev) { return this.selections.indexOf(ev.id) > -1}.bind(this)));
      return;
    }

    if (event === 'filtered' && this.resolved) {
      fn(this.filteredEvents);
      return;
    }

    if (opts && opts.target && this[opts.target] && opts.immediate) {
      fn(this[opts.target]);
      return;
    }

    var events = event.split(',');

    if (typeof fn == 'function') {
      events.map(function(ev) { return ev.trim() }).forEach(function(ev) {
        if (!this.oneshot.hasOwnProperty(ev)) {
          this.oneshot[ev] = [];
        }
        this.oneshot[ev].push({fn: fn, opts: opts});
      }.bind(this));
    }

    return this;
  },
  emit: function(event, target, field) {
    event = event || 'complete';
    target = typeof target != 'undefined' ? target : this.events;

    if (this.listeners.hasOwnProperty(event)) {
      this.listeners[event].forEach(function(fnObj) {
        var fnTarget = (fnObj.opts || {}).target || target;
        fnObj.fn(fnTarget, field);
      });
    }
    if (this.oneshot.hasOwnProperty(event)) {
      this.oneshot[event].forEach(function(fnObj) {
        var fnTarget = (fnObj.opts || {}).target || target;
        fnObj.fn(fnTarget, field);
      });
      delete this.oneshot[event];
    }
  }
}
