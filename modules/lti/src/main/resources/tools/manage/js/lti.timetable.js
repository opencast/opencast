function Timetable(params, isPersonal) {
  var _ltiData = (params || {}).lti;
  var _eventMgr = (params || {}).eventManager;

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

  Object.defineProperty(this, 'isPersonal', {
    get: function() {
      return isPersonal;
    },
    enumerable: false
  });

  _ltiData.on('complete', function() {
    if (this.isPersonal) {
      return;
    }

    var _ttArr = [];

    (_ltiData['lis_course_offering_sourcedid'] || '').split('+')
      .forEach(function(course) {
        _ttArr.push(
          this.setCourseTimetable(course)
        )
      }.bind(this));

    $.when.apply($, _ttArr).done(function() {
      this.resolved = true;
    }.bind(this));

  }.bind(this));
}

Timetable.prototype = {
  constructor: Timetable,
  fetchTimetable: function(course) {
    return $.Deferred(function(d) {
      var timetable = null;

      course = course.split(",");
      if(course.length === 2 || !isNaN(course[0])){
        $.ajax({
              url: 'https://srvslscet001.uct.ac.za/timetable/?course=' + course,       
        })
        .done(function(tt) {
          timetable = tt;
        })
        .always(function() {
          d.resolve(timetable);
        });
      }
      }).promise();
  },
  setCourseTimetable: function(course) {
    return $.Deferred(function(d) {
      this.fetchTimetable(course)
        .then(function(tt) {
          if (tt && tt.course) {
            this[tt.course + ',' + tt.term] = tt;
          }
          d.resolve();
      }.bind(this));
    }.bind(this)).promise();
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
