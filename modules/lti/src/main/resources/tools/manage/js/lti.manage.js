const maxEventDuration = 18000000;      // 5 hours in milliseconds

var OCManager = (function($) {
  function getURLParameter(name) {
      return decodeURIComponent((new RegExp('[?|&]' + name + '=' + '([^&;]+?)(&|#|;|$)').exec(location.search)||[,""])[1].replace(/\+/g, '%20')) || '';
  }

  function Manager() {
    /****GET Parameters****/
    this.seriesId = getURLParameter('sid');
    this.uploadSetting = getURLParameter('upload');
    this.isPersonalSeries = getURLParameter('type') === 'personal';

    this.org = new Organization();
    this.user = new User({isPersonalSeries: this.isPersonalSeries});
    this.LTIData = new LTIData();

    this.series = new Series(getURLParameter('sid'), this.isPersonalSeries);
    this.captureAgents = new CaptureAgents(this.org);
    this.eventMgr = new EventManager({
             series: this.series,
                org: this.org,
      captureAgents: this.captureAgents,
         isPersonal: this.isPersonalSeries
    });
    this.timeTable = new Timetable({lti: this.LTIData, eventManager: this.eventMgr}, this.isPersonalSeries);
    this.tableView = new TableView({eventManager: this.eventMgr, isPersonal: this.isPersonalSeries});

    this.progressBar = $('#uploadModal progress')[0];

    this.lectureNotesSeries = [
      '2458110f-c183-4925-bd04-1351256b97a3',
      '55137340-8c3f-4c90-80c0-10163223039c',
    ];

    this.isAttachmentAllowed = getURLParameter('lecturenotes') === 'true' || this.lectureNotesSeries.indexOf(this.eventMgr.series.id) > -1;
    this.recorderAllowed = getURLParameter('recorder') === 'true';

    this.aclUpdateQueue = [];
  }

  Manager.prototype = {
    constructor: OCManager,
    init: function() {
      if (!this.eventMgr.resolved || !this.LTIData.resolved) {
        return;
      }

      if (this.eventMgr.events.length === 0 && this.LTIData.lis_course_offering_sourcedid &&
          this.LTIData.lis_course_offering_sourcedid.indexOf(',') > -1) {
        var reminder = localStorage.getItem('timetableNoRemind');
        if (!reminder || reminder == 'false') {
          $('#ttNotificationModal').modal('show');
        }
      }

      if (this.uploadSetting === 'true') {
        $('span[data-target="#uploadModal"]')[0].style.display = '';
      }

      var uniqueStatuses = this.eventMgr.events.map(function(event) {
                             return event.status;
                           })
                           .filter(function(status, i, arr) {
                             return arr.indexOf(status) === i;
                           });

      this.setStatuses(uniqueStatuses);

      if (this.recorderAllowed) {
        $('.lti-oc-title a').show();
      }

      if (this.isPersonalSeries) {
        $('.lti_links').hide();
        $('span[data-target="#sheduleModal"]').hide();
        $('span[data-target="#uploadModal"]').css({
          marginRight: '17px'
        });
        $('th:last-of-type').css('width', '10em');
        $('th:nth-of-type(6)').css('width', '8em');
        $('th:nth-of-type(3)')
          .css('width', 'calc((100% - 30rem)/3 - 1rem)')
          .attr('data-column', 'series')
          .text('Series')
        $('#grid-body').addClass('personal');
        $('span[data-target="#scheduleModal"]').hide();
        $('span[data-target="#ttScheduleModal"]').hide();
      }

      var recorderButton = document.getElementById('recorder');
      recorderButton.href = "https://"+window.location.hostname+"/studio/index.html?upload.seriesId="+this.seriesId;
    },
  setTimetableInputs: function() {
                        if (Object.keys(ocManager.timeTable).length === 0) {
                          $('.btn[data-target="#ttScheduleModal"]').addClass('btn-disabled');
                          return;
                        }
                        var first = null;
                        for (var key in this.timeTable) {
                          var $el;
                          if (moment(this.timeTable[key].end_date).isBefore(moment())) {
                            $('#ttCourses').append(
                              tmpl('tmpl-ttcourse', this.timeTable[key])
                            );
                            $el = $('#ttCourses li[data-course="' + key + '"]');
                            $el.attr('data-timeframe', moment(this.timeTable[key].end_date).format('Do MMM YYYY'));
                            $el.addClass('inactive');
                          }
                          else if (this.timeTable[key].end_date) {
                            $('#ttCourses').prepend(
                              tmpl('tmpl-ttcourse', this.timeTable[key])
                            );
                            $el = $('#ttCourses li[data-course="' + key + '"]');
                          }
                          first = first || $el[0];
                        }
                        first.click();
                      },
  setAvailableTimeslots: function(orgDefaults) {
                           orgDefaults = orgDefaults || this.org.properties;

                           var minTime = orgDefaults['admin.event.new.start_time']
                                           .split(':').reduce(function(total, unit, i) {
                                               if (i < 2) {
                                                 total += +unit * Math.pow(60, 1-i);
                                               }
                                               return total;
                                             }, 0);
                             var maxTime = orgDefaults['admin.event.new.end_time']
                                             .split(':').reduce(function(total, unit, i) {
                                               if (i < 2) {
                                                 total += +unit * Math.pow(60, 1-i);
                                               }
                                               return total;
                                             }, 0);

                             $('.timeSelector li').each(function() {
                               var timeInMins = $(this).html().split(':').reduce(function(total, unit, i) {
                                                  if (i < 2) {
                                                    total += +unit * Math.pow(60, 1-i);
                                                  }
                                                  return total;
                                                }, 0);
                               if (timeInMins < minTime || timeInMins > maxTime) {
                                 $(this).addClass('orgHide');
                               }
                             });
                         },
     getCommonSelections: function() {
                            var self = this;
                            var common = this.events
                                           .filter(function(event) {
                                             return self.selected.indexOf(event.id) > -1;
                                           })
                                           .map(function(event) {
                                             var tmpObj = JSON.parse(JSON.stringify(event));
                                             tmpObj.startTime = moment(tmpObj.start_date).format('HH:mm');
                                             tmpObj.endTime = moment(tmpObj.end_date).format('HH:mm');
                                             tmpObj.startDate = moment(tmpObj.start_date).format('ddd DD MMM, YYYY');
                                             tmpObj.endDate = moment(tmpObj.end_date).format('HH:mm');
                                             tmpObj.title = !isNaN(tmpObj.title.substring(tmpObj.title.lastIndexOf(' ') + 1)) ?
                                                            tmpObj.title.substring(0, tmpObj.title.lastIndexOf(' ')) :
                                                            tmpObj.title;

                                             return tmpObj;
                                           });

                            var fields = ['presenters', 'startTime', 'endTime', 'startDate', 'endDate', 'location'];

                            if (common.length === 0) {
                              var obj = {};
                              fields.forEach(function(field) {
                                obj[field] = 'Multiple';
                              });
                              return obj;
                            }

                            var base = JSON.parse(JSON.stringify(common[0]));

                            common.forEach(function(event) {
                              for (var key in event) {
                                if (base[key]) {
                                  if ((Array.isArray(base[key]) && JSON.stringify(base[key]) !== JSON.stringify(event[key]))
                                       || (!Array.isArray(base[key]) && event[key] !== base[key])) {
                                    delete base[key];
                                  }
                                }
                              }
                            });

                            fields.forEach(function(field) {
                              if (!base[field]) base[field] = 'Multiple';
                            });

                            return base;
                          },
        setVenues: function(agents) {
                     agents = agents || this.captureAgents.orderByName();
                     var _venueCache = $('<div/>');
                     for (var key in agents) {
                       if (!this.captureAgents.__proto__[key]) {
                         _venueCache.append(
                           $('<li/>', {'data-ref': key}).html(agents[key].name)
                         );
                       }
                     }
                     $('[data-venues=true]').each(function() {
                       $(this).append(_venueCache.append(true).children())
                     });
                     $('#grid-body tr td:nth-child(5)').each(function() {
                       var agent = $(this).text();
                       if (agents[agent]) {
                         $(this).text(agents[agent].name);
                       }
                     });
                   },
      setStatuses: function(statuses) {
                     var remove = true;
                     $('.dropdown-menu[aria-describedby="statusDropdown"] li').each(function() {
                       if ($(this).hasClass('divider')) {
                         return false;
                       }

                       $(this).remove();
                     });

                     statuses.forEach(function(status) {
                       var statusEl = $('<li/>', {
                                        'data-ref': status,
                                        role: 'presentation'
                                      }).append($('<a/>', {
                                                  text: status
                                                })
                                               );

                       $('.dropdown-menu[aria-describedby="statusDropdown"]').prepend(statusEl);
                     });
                   },
   getInputs: function(_form) {
      _form = _form || $('.modal.in form')[0];
      if (!_form) {
        return null;
      }

      var changes = $(_form).find('input[name],select[name],textarea[name]').toArray().reduce(function(result, input) {
                      if (input.name.indexOf('[]') > -1) {
                        if (!result[input.name]) {
                          result[input.name] = [];
                        }
                        if ((input.type === 'checkbox' && input.checked) || input.type !== 'checkbox') {
                          result[input.name].push(input.value);
                        }
                      }
                      else if (['checkbox', 'radio'].indexOf(input.type) > -1) {
                        if ($(_form).find('input[name="' + input.name + '"]').length === 1) {
                          result[input.name] = input.checked;
                        }
                      }
                      else if (input.type === 'file') {
                        result[input.name] = input.files[0];
                      }
                      else {
                        result[input.name] = input.value;
                      }
                      return result;
                    }, {});

      return changes;
   },
   checkForCurrentConflicts: function(form) {
    form = (form.hasOwnProperty('length') ? form[0] : form) || $('.modal.in .modal-content')[0];
    if (!form) {
      return;
    }
    var _modal = $(form).hasClass('modal') ? form : $(form).parents('.modal')[0];
    $(form).find('.btn-success').prop('disabled', true);
    $(form).find('li.conflicts').empty();

    var changes = this.getInputs(form);
    if (!changes.startTime) {
      if (changes.duration && changes.hour && changes.minute) {
        var endTime = +changes.hour * 60 + +changes.minute + +changes.duration;
        changes.startTime = changes.hour + ':' + changes.minute;
        changes.endTime = [(endTime/60 >> 0) % 24, endTime % 60]
                            .map(function(unit) {
                              return (unit < 10 ? '0' : '') + unit;
                            })
                            .join(':');
      }
    }

    if (changes.multipleSchedule && changes["repeatdays[]"].length === 0) {
      return;
    }

    if ($(form).parents('.modal').find('.numConflicts').length > 0) {
      $(form).find('.numConflicts').html('')
      $(form).parents('.modal').removeClass('conflicts all');
    }

    if (_modal) {
      $(_modal).addClass('checkConflicts');
    }

    this.eventMgr.checkConflicts(changes, true)
      .then(function(conflicts) {
        if (!conflicts) {
          return;
        }

        if (conflicts.length === 0) {
          $(form).find('.btn-success').prop('disabled', false);
          ocManager.clearConflicts(form);
        }
      })
      .fail(function(error) {
        if ($(form).hasClass('in') || $(form).parents('.modal').hasClass('in')) {
          this.displayScheduleConflicts(error, changes);
        }
      }.bind(this))
      .always(function() {
        $(_modal).removeClass('checkConflicts');
      });
   },
  checkConflictsForUpdate: function(eventsArr, changes, isOnTheFly) {
    return $.Deferred(function(d) {
      var promArr = [];

      eventsArr.forEach(function(event) {
        promArr.push(
          $.Deferred(function(p) {
            for (var key in changes) {
              event[key] = changes[key] || event[key];
            }
            this.eventMgr.checkConflicts(event, isOnTheFly, true)
              .then(function() {
                p.resolve();
              })
              .fail(function(conflicts) {
                p.resolve(conflicts);
              });
          }.bind(this)).promise()
        )
      }.bind(this));

      $.when.apply($, promArr)
        .done(function() {
          var results = Array.prototype.slice.call(arguments)
                          .filter(function(conflict) {
                            return !!conflict;
                          });

          if (results.length === 0) {
            d.resolve();
          }
          else {
            d.reject(results);
          }
        });
    }.bind(this)).promise();
  },
  clearConflicts: function(_modal) {
    _modal = _modal || $('.modal.in')[0];
    if (!$(_modal).hasClass('modal')) {
      _modal = $(_modal).parents('.modal')[0];
    }

    $(_modal).find('li.conflicts').empty();
    $(_modal).removeClass('conflicts').removeClass('all').removeClass('error').removeClass('checkConflicts');
  },
  displayCreationError: function(error, _modal) {
    if (!error || (!error.error && error.error !== '')) {
      return;
    }

    _modal = _modal || $('.modal.in')[0];
    if (!_modal) {
      return;
    }

    $(_modal).find('.errors').text(error.error);
  },
    displayScheduleConflicts: function(conflicts, proposedDates, _modal) {
                                _modal = _modal || $('.modal.in')[0];
                                if (!_modal) {
                                  return;
                                }

                                if (!conflicts) {
                                  $(_modal).addClass('error');
                                }


                                $(_modal).find('li.conflicts').empty();

                                if (!Array.isArray(conflicts)) {
                                  if (conflicts && conflicts.error) {
                                  }

                                  return;
                                }

                                var startTimeMins = proposedDates.startTime
                                                      .split(':').reduce(function(mins, unit, i) {
                                                        mins += +unit * Math.pow(60, 1-i);
                                                        return mins;
                                                      }, 0);
                                var endTimeMins = proposedDates.endTime
                                                      .split(':').reduce(function(mins, unit, i) {
                                                        mins += +unit * Math.pow(60, 1-i);
                                                        return mins;
                                                      }, 0);

                                var conflictValues = {
                                  start: moment(moment(proposedDates.start_date).add(startTimeMins, 'minute')),
                                    end: moment(moment(proposedDates.end_date).add(endTimeMins, 'minute'))
                                };
                                var _listConflicts = $('<ul/>');
                                var allowTimeAdjust = true;
                                var timeAdjustDelta = 1000000;

                                var chosenEndTimeInUTCMins = getDayTimeInMinutes(moment(conflictValues.end).utc());
                                var chosenStartTimeInUTCMins = getDayTimeInMinutes(moment(conflictValues.start).utc());
                                conflicts.forEach(function(conflict) {
                                  var conflictStartTimeInUTCMins = getDayTimeInMinutes(moment(conflict.start, 'YYYYMMDDTHHmmssZ').utc());
                                  var conflictEndTimeInUTCMins = getDayTimeInMinutes(moment(conflict.end, 'YYYYMMDDTHHmmssZ').utc());
                                  if (conflictStartTimeInUTCMins <= chosenStartTimeInUTCMins) {
                                    allowTimeAdjust = false;
                                  }
                                  else {
                                    timeAdjustDelta = Math.min(timeAdjustDelta, Math.abs(conflictStartTimeInUTCMins - chosenStartTimeInUTCMins));
                                  }
                                  _listConflicts.append(
                                    $('<li/>', {
                                      text: moment(conflict.start, 'YYYYMMDDTHHmmssZ').format('ddd') + ' ' + moment(conflict.start, 'YYYYMMDDTHHmmssZ').format('DD MMM HH:mm') + ' - ' + moment(conflict.end, 'YYYYMMDDTHHmmssZ').format('HH:mm') + ': ' + conflict.title
                                    })
                                  );
                                });
                                $(_modal).find('li.conflicts').append(_listConflicts);
                                $(_modal).find('.btn-success').prop('disabled', 'true');
                                if (allowTimeAdjust && timeAdjustDelta >= 15) {
                                  timeAdjustDelta = parseInt(timeAdjustDelta/5) * 5;
                                  timeAdjustDelta = timeAdjustDelta % 5 === 0 ? timeAdjustDelta - 5 : timeAdjustDelta;
                                  var _promptAdjust = $('<p/>', {text: 'Automatically adjust your selected start and end time to resolve conflicts by clicking the button below.'});
                                  _promptAdjust.css({
                                    marginTop: '1rem',
                                    lineHeight: '1.5rem'
                                  });
                                  var _adjustButton = $('<button/>', {text: 'Adjust event duration(s) to ' + (timeAdjustDelta) + ' minutes', 'data-adjust': timeAdjustDelta, class: 'btn btn-default', type: 'button'});
                                  _adjustButton.css({
                                    display: 'block',
                                    textAlign: 'right'
                                  });
                                  _promptAdjust.append(_adjustButton);
                                  $(_modal).find('li.conflicts').append(_promptAdjust);
                                  _adjustButton.on('click', function(e) {
                                    var mins = +$(this).data('adjust');
                                    var proposedEndTime = getDayTimeFromMinutes(getDayTimeInMinutes(moment(conflictValues.start)) + mins);
                                    $(_modal).find('input[name=duration]').val(mins);
                                    $(_modal).find('input[name=endTime]').val(proposedEndTime);
                                    $(_modal).find('input[name=startTime]').trigger('change');
                                  });
                                }
                              },
    displayUpdateConflicts: function(conflicts) {
      var _modal = $('.modal.in');
      if ($(_modal).find('.numConflicts').length > 0) {
        $(_modal).addClass('conflicts');
        $(_modal).find('.numConflicts').attr('data-numconflicts', conflicts.length);
      }
    },
    displayUpdateErrors: function(ids, proposedChanges) {
      $('#updateProgress ul').empty();
      var _list = $('<div/>', {class: 'list'});
      var errorEvents = ocManager.eventMgr.getEventsFromSelections(ids);
      errorEvents.forEach(function(event) {
        var _item = $('<li/>');
        var _title = $('<span/>', {text: event.title, class: 'thirdWidth'});
        var _venue = $('<span/>', {
                       text: (ocManager.captureAgents[proposedChanges.location || event.location] || {}).name ||
                             proposedChanges.location || event.location,
                       class: 'thirdWidth'
                     });
        var dateConflictText = moment(event.technical_start).format('ddd DD MMM, hh:mma');
        if (proposedChanges.start_date || proposedChanges.startTime) {
          dateConflictText = moment(
                               moment(proposedChanges.start_date || event.technical_start).format('YYYY-MM-DDT') +
                               proposedChanges.startTime || moment(event.technical_start).format('HH:mm') + ':00'
                             ).format('ddd DD MMM, hh:mma');
        }
        var _date = $('<span/>', {text: dateConflictText, class: 'thirdWidth'});
        _item.append(_title).append(_venue).append(_date);
        _list.append(_item);
      });
      $('#updateProgress ul').append(_list);
    },
     logSelections: function(selectedEvents) {
                     var totalSelected = selectedEvents.length;
                     var selectedIds = selectedEvents.map(function(event) { return event.id });

                     $('#selectionList li').toArray()
                       .forEach(function(item) {
                         var eventId = item.getAttribute('data-eventid');
                         var eventIndex = selectedIds.indexOf(eventId);
                         if (eventIndex === -1) {
                           this.removeSelection(eventId);
                         }
                         else {
                           selectedIds.splice(eventIndex, 1);
                         }
                       }.bind(this));

                     selectedEvents.filter(function(selectedEvent) {
                       return selectedIds.indexOf(selectedEvent.id) > -1;
                     })
                     .forEach(function(selectedEvent) {
                       $('#selectionList')
                         .append(
                           $.parseHTML(tmpl('tmpl-selection', selectedEvent))
                         )
                       $('#selectedInfo')
                         .append(
                           $.parseHTML(tmpl('tmpl-selected', selectedEvent))
                         )
                     });

                     if (totalSelected > 0) {
                       $('#grid-header').addClass('selections');
                       $('#selections').html(totalSelected);
                     }
                     else {
                       $('#grid-header').removeClass('selections');
                       $('#selectDropdown').removeClass('open');
                       setTimeout(function() {
                         $('#selections').html('');
                       }, 500);
                     }
                     return this;
                   },
 promptEventRemoval: function(eventList) {
                       var text = eventList.length > 1 ? eventList.length + ' events' : eventList[0];
                       $('#delModal .modal-body').html(tmpl('tmpl-delete', {text: text}));
                       $('#delModal').modal('show');
                     },
    removeSelection: function(eventId) {
                       var item = $('#selectionList li[data-eventid="' + eventId + '"]')[0];
                       if (item && !item.classList.contains('remove')) {
                         item.className = 'remove';
                         setTimeout(function() {
                            item.parentNode.removeChild(item);
                           $('#selectedInfo li[data-eventid="' + eventId + '"]').remove();
                         }, 320);
                       }
                     },
   refreshSelection: function(id) {
                       this
                         .removeSelection(id)
                         .addSelection(id);
                     },
          isSelected: function(id) {
                        return ocManager.selected.indexOf(id) > -1;
                      },
     showSelectedNum: function(id) {
                        if ($('#chk-' + id).length > 0) {
                          var numChecked = Array.prototype.slice.call(
                                               document.querySelectorAll('#grid tr[data-status="Upcoming"] input[type=checkbox]')
                                             )
                                             .reduce(function(collect, input) {
                                               return collect + input.checked ? 1 : 0;
                                             }, 0);

                          $('#chk-head')[0].className = 'checkbox ' + (numChecked ? 'partial' : 'none');

                          if (this.selected.length === 0) {
                            $('#grid-header').removeClass('selections');
                          }
                        }
                      },
    prepareSchedules: function(selections, names) {
                        if (!Array.isArray(selections)) {
                          selections = [selections];
                        }

                        return selections
                               .map(function(schedule, i) {
                                 //Put into nice format for scheduling

                                 var endTimeInMinutes = schedule.end_time.split(':')
                                                         .filter(function(unit, i) {                      //filter out anything but hours and minutes
                                                           return i < 2;
                                                         })
                                                         .reduce(function(minutes, unit, i) {
                                                           minutes += +unit * Math.pow(60, 1 - i);
                                                           return minutes;
                                                         }, -10)                                          //-10 is subtraction of 10 minutes from scheduled end time

                                 schedule.end_time = '' + parseInt(endTimeInMinutes/60) + ':' + (endTimeInMinutes%60);

                                 schedule.start_time = schedule.start_time.replace(/ /g, '');
                                 schedule.end_time = schedule.end_time.replace(/ /g, '');
                                 schedule.start_time = (schedule.start_time.length === 4 ? "0" : '') + schedule.start_time;
                                 schedule.end_time = (schedule.end_time.length === 4 ? "0" : '') + schedule.end_time;

                                 var startArr = schedule.start_time.split(':').map(function(unit) { return +unit });
                                 var endArr = schedule.end_time.split(':').map(function(unit) { return +unit });
                                 schedule.duration = ((endArr[0] - startArr[0]) * 60 + endArr[1] - startArr[1]) * 60 * 1000;

                                 var todayTime = moment().format('YYYY-MM-DDT') + schedule.start_time;

                                 schedule.start_date = moment(schedule.start_date + 'T' + schedule.start_time).isBefore(moment().add(10, 'minutes')) ?
                                                         (
                                                            moment(todayTime).isBefore(moment().add(10, 'minutes')) ? moment().add(1, 'day').format('YYYY-MM-DD') : todayTime
                                                         ) :
                                                         schedule.start_date;
                                 schedule.start_date = moment(moment(schedule.start_date).format('YYYY-MM-DD') + ' ' +
                                                       (schedule.start_time.length === 4 ? '0' : '') + schedule.start_time).utc().format('YYYY-MM-DDTHH:mm:ssZ');
                                 schedule.end_date = moment(moment(schedule.end_date).format('YYYY-MM-DD') + ' ' +
                                                     (schedule.end_time.length === 4 ? '0' : '') + schedule.end_time).utc().format('YYYY-MM-DDTHH:mm:ssZ');

                                 var BYHOUR = 'BYHOUR=' + moment(schedule.start_date).utc().format('HH') + ';';
                                 var BYMINUTE = 'BYMINUTE=' + moment(schedule.start_date).utc().format('mm');
                                 var BYDAY = 'BYDAY=' + Object.keys(schedule.days)
                                               .filter(function(day) {
                                                 return schedule.days[day];
                                               })
                                                 .map(function(day) {
                                                   return day.substring(0, 2).toUpperCase();
                                               })
                                               .join(',') + ';';
                                 schedule.rrule = 'FREQ=WEEKLY;' + BYDAY + BYHOUR + BYMINUTE;

                                 var payload = {
                                   processing: this.processing,
                                       access: this.access,
                                       source: {
                                                 type: 'SCHEDULE_MULTIPLE',
                                                 metadata: {
                                                   start: schedule.start_date,
                                                   device: schedule.ca_name,
                                                   inputs: 'presenter,presentation,presentation2,audio',
                                                   end: schedule.end_date,
                                                   duration: schedule.duration + '',
                                                      rrule: schedule.rrule
                                                 }
                                               },
                                     metadata: [
                                                 {
                                                   flavor: "dublincore/episode",
                                                    title: "EVENTS.EVENTS.DETAILS.CATALOG.EPISODE",
                                                   fields: [
                                                             {
                                                               id: 'isPartOf',
                                                               type: 'text',
                                                               value: this.seriesId
                                                             },
                                                             {
                                                               id: 'title',
                                                               type: 'text',
                                                               value: schedule.course + ' ' + schedule.class_section + ' ' + schedule.start_time
                                                             },
                                                             {
                                                               id: 'creator',
                                                               type: 'mixed_text',
                                                               value: []
                                                             },

                                                           ]
                                                 }
                                               ],
                                   scheduleId: names ? names[i] : null
                                 };
                                 if (this.series.hasOwnProperty('contributor')) {
                                   try {
                                     payload.metadata[0].fields[2].value = this.series.contributor;
                                   } catch(e) {
                                   }
                                 }
                                 return payload;
                               }.bind(this));
                      },
          suggestion: function(msg) {
                        $('#suggestion').html(msg);
                        $('#suggestion').addClass('display');
                        setTimeout(function() {
                          $('#suggestion').removeClass('display');
                        }, 4000);
                      },
      createSchedule: function(sched) {
                        var fd = new FormData();
                        fd.append('metadata', JSON.stringify(sched));
                        return $.Deferred(function(d) {
                          $.ajax({
                                    url: '/admin-ng/event/new',
                                   type: 'post',
                            contentType: false,
                            processData: false,
                                  cache: false,
                                   data: fd
                          }).done(function(res) {
                            d.resolve(res);
                          }).fail(function() {
                            d.reject(sched);
                          });
                          d.resolve();
                        }).promise();
                      },
    displayTimetableConflict: function(scheduleId) {
      $('input[name="' + scheduleId + '"]').each(function() {
        var parent = $(this).parents('li')[0];
        $(parent).addClass('conflict');
        if ($(parent).is(':visible')) {
          $(parent).addClass('conflictAnim');
          setTimeout(function() {
            $(parent).removeClass('conflictAnim');
          }.bind(this), 1000);
        }
        $(this).parents('li[data-course]:not(.selected)').addClass('issue');
        this.checked = false;
      });
      $('#ttScheduleModal .instruction').html('Timetabling conflicts detected. Please review before proceeding.');
    },
  sequentialEventConflictCheck: function(data, events) {
      return $.Deferred(function(d) {
        var tzTotalMins = new Date().getTimezoneOffset();
        var tzHours = Math.abs(parseInt(tzTotalMins/60));
        var tzMins = Math.abs(tzTotalMins) - tzHours * 60;
        tzHours = (tzHours < 10 ? '0' : '') + tzHours;
        tzMins = (tzMins < 10 ? '0' : '') + tzMins;

        var conflictingList = [];

        var runCheck = function(index) {
          if (index < events.length) {
            var event = events[index];
            var start = (data.start_date || moment(event.start_date).format('YYYY-MM-DD')) + 'T' +
                        (data.startTime || moment(event.start_date).format('HH:mm')) + ':00' + (tzTotalMins >= 0 ? '-' : '+') + tzHours + ':' + tzMins;

            var req = {
                  id: event.id,
                  device: data.location || event.location,
                  start: moment(start).utc().format('YYYY-MM-DDTHH:mm:ss') + 'Z',
                  duration: (data.duration || (moment.duration(moment(event.end_date).diff(moment(event.start_date)))).asMinutes()) * 60 * 1000 + ''
            };
            req.end = moment(req.start).add(parseInt(req.duration / 60000), 'minutes');

            $.ajax({
              url: this.endpoints.checkConflict,
              type: 'POST',
              data: {metadata: JSON.stringify(req)}
            }).done(function(res) {
            }).fail(function(err) {
              if (err.status === 409) {
                conflictingList.push(event.id);
              }
            }).always(function() {
              runCheck(++index);
            })
          }
          else {
            d.resolve(conflictingList);
          }
        }.bind(this);
        runCheck(0);
      }.bind(this)).promise();
  },
  sequentialConflictCheck: function(scheds) {
      return $.Deferred(function(d) {
        var runCheck = function(index) {
          if (index < scheds.length) {
            this.checkTimetableConflict(scheds[index].source.metadata)
              .then(function() {
                runCheck(++index);
              })
              .fail(function(err) {
                d.reject(scheds[index]);
              });
          }
          else {
            d.resolve();
          }
        }.bind(this);
        runCheck(0);
      }.bind(this)).promise();
  }
  }

  return Manager;

})(jQuery);

var ocManager = new OCManager();

ocManager.org.on('complete', function() {
  ocManager.setAvailableTimeslots();
});

ocManager.captureAgents.on('complete', function() {
  ocManager.setVenues();
});

ocManager.timeTable.on('complete', function() {
  ocManager.setTimetableInputs();
});

ocManager.series.on('acl', function(acl) {
  ocManager.access = acl;
});

ocManager.eventMgr.once('complete', function(events) {
  ocManager.init(events);
});

ocManager.LTIData.on('complete', function() {
  ocManager.init();
});

ocManager.eventMgr.on('filter.startDate', function(d) {
  $('li[data-ref="date_range"] .selectedValue span:nth-child(1)')
    .html(d ? moment(d).format('DD MMM, YYYY') : '');
});

ocManager.eventMgr.on('filter.endDate', function(d) {
  $('li[data-ref="date_range"] .selectedValue span:nth-child(2)')
    .html(d ? moment(d).format('DD MMM, YYYY') : '');
});

ocManager.eventMgr.on('filter.startTime', function(d) {
  if (!d) {
    $('li[data-ref="time_range"] .selectedValue span:nth-child(1)').empty()
      .parent().removeClass('hasValue');
    return;
  }

  $('li[data-ref="time_range"] .selectedValue span:nth-child(1)')
    .html(d.map(function(unit) { return (unit < 10 ? '0' : '') + unit;}).join(':'))
    .parent().addClass('hasValue');
});

ocManager.eventMgr.on('filter.endTime', function(d) {
  if (!d) {
    $('li[data-ref="time_range"] .selectedValue span:nth-child(2)').empty();
    return;
  }

  $('li[data-ref="time_range"] .selectedValue span:nth-child(2)')
    .html(d.map(function(unit) { return (unit < 10 ? '0' : '') + unit;}).join(':'))
    .parent().addClass('hasValue');
});

ocManager.eventMgr.on('isfiltered', function(isFiltered) {
  if (isFiltered) {
    $('#grid-header').addClass('filtered');
  }
  else {
    $('#grid-header').removeClass('filtered');
    $('input[name=freeform]').val('');
  }
});

ocManager.eventMgr.on('filter.status,filter.day', function(filterArr, filterField) {
  var buttonText = '';
  var filterTotal = (filterArr || []).length;
  var dropDown = $('[aria-labelledby="' + filterField + 'Dropdown"]');

  dropDown.find('li')
    .each(function() {
      $(this).removeClass('active');
    });

  if (filterTotal > 0) {
    buttonText = filterArr[0] + (filterTotal > 1 ? ' + ' + (filterTotal - 1) + ' other' : '') + (filterTotal > 2 ? 's' : '');
    dropDown.parent().attr('title', filterArr.join(', '));
    filterArr.forEach(function(val) {
      dropDown.find('li[data-ref="' + val + '"]').addClass('active');
    });
  }
  else {
    dropDown.find('li[data-ref=All]')
      .addClass('active');

    $('#'+ filterField+ 'Dropdown').parent().removeAttr('title');
  }

  $('#' + filterField + 'Dropdown').html(buttonText);
});

ocManager.eventMgr.on('selection', function(selections) {
  ocManager.logSelections(selections);
});

ocManager.eventMgr.on('promptremoval', function(events) {
  ocManager.promptEventRemoval(events);
});

ocManager.eventMgr.on('event.delete.success', function(deletedEvents) {
  $('#delModal').removeClass('committing').modal('hide');
  $('p[data-instruction=info]')
    .html(deletedEvents.length + ' event(s) deleted')
    .addClass('show');

  setTimeout(function() {
    $('p[data-instruction=info]').removeClass('show');
  }, 5000);
});

ocManager.eventMgr.on('event.create.success', function(createdEvents) {
  $('#scheduleModal').find('button[type=reset].btn-default')[0].click();
  $('p[data-instruction=warning]').removeClass('show');
  var numEvents = createdEvents.length;
  $('p[data-instruction=info]')
    .html(numEvents ? numEvents + ' event(s) created' : 'Events created')
    .addClass('show');

  setTimeout(function() {
    $('p[data-instruction=info]').removeClass('show');
  }, 5000);
});

ocManager.eventMgr.on('event.create.partial', function(createdEvents) {
  $('p[data-instruction=warning]')
    .html(createdEvents.length + ' event(s) altogether so far...');
});

ocManager.eventMgr.on('event.create.failed', function(deletedEvents) {
  $('#scheduleModal').addClass('error');
});

ocManager.eventMgr.on('event.delete.fail', function(events) {
  $('#delModal').removeClass('committing').modal('hide');
});

ocManager.eventMgr.on('event.upload.progress', function(progress) {
  if($('#populatedPresVid').prop('checked')) {
    ocManager.progressBar.value = progress.loaded / progress.total * 100;
  } else if($('#populatedPresentationVid').prop('checked')) {
    $('#uploadProgress progress')[0].value = progress.loaded / progress.total * 100;
  }

});

ocManager.eventMgr.on('event.update.progress', function(progress) {
  $('#updateProgress progress')[0].value = progress.current / progress.total * 100 >> 0;
});

ocManager.eventMgr.on('event.update.complete', function(details) {
  console.log('event.update.complete: ' + details.id + ' ' + details.isPersonal);
  console.log(details.metadataChanges);
  if (details.isPersonal) {
    if (details.metadataChanges.isPartOf) {
      //Update ACLs since series was changed
      this.addAccessRoles(details.id, ['ROLE_CILT_OBS'], ocManager.series.identifier, details.metadataChanges.isPartOf);
      $('.modal.in').addClass('updateAcl');
    }
    else if (details.metadataChanges.title || details.metadataChanges.presenters) {
      this.republish(details.id);
      removeModal($('.modal.in')[0], details.title);
    }
  } else {
    removeModal($('.modal.in')[0], details.title);
  }
}.bind(ocManager.eventMgr));

ocManager.eventMgr.on('event.update.acl', function(details) {
  //ACLs were updated. Now republish event to make ACLs apply for other Vula sites
  ocManager.eventMgr.republish(details.id);
  $('.modal.in').addClass('republish').removeClass('updateAcl');
});

ocManager.eventMgr.on('event.republish.complete', function(details) {
  $('.modal.in').removeClass('republish');
  removeModal($('.modal.in')[0], '');
});

ocManager.eventMgr.on('event.update.failed', function(progress) {
  if ($('#updateProgress').length) {
    $('#updateProgress progress')[0].value = progress.current / progress.total * 100 >> 0;
  }
});

ocManager.eventMgr.on('event.schedule.submitted', function() {
});

ocManager.eventMgr.on('event.statuses', function(statuses) {
  ocManager.setStatuses(statuses);
});

ocManager.eventMgr.on('event.media', function(details) {
  if (details.target && $(details.target).data('eventid') === details.id) {
    var event = this.getEventDetails(details.id);
    var videoSrc = details.media
                  .filter(function(source) {
                    return source.type.indexOf("delivery") > -1;
                  })
                  .reduce(function(finalised, source) {
                    finalised = finalised || source.url;
                    return finalised;
                  }, null);

    if (videoSrc) {
      $(details.target).find('video').attr('src', '/' + videoSrc.split('/').slice(3).join('/'));
    }

    var notes = details.media
                  .filter(function(lectureNote) {
                    return lectureNote.type === 'attachment/notes' || lectureNote.type === 'attachment/part+notes';
                  })
                  .reduce(function(finalised, lectureNote) {
                    finalised = finalised || lectureNote.url;
                    return finalised;
                  }, '');

    $(details.target).find('#notes')
      .text(event.title + notes.substring(notes.lastIndexOf('.')))
      .attr('title', event.title + notes.substring(notes.lastIndexOf('.')));

    if (notes) {
      $(details.target).find('#notes')
        .text(event.title + notes.substring(notes.lastIndexOf('.')))
        .attr('title', event.title + notes.substring(notes.lastIndexOf('.')))
        .attr('href', notes);
    }
    else {
      $(details.target).find('#notes')
        .text(notes)
        .removeAttr('title')
        .removeAttr('href');
    }

    if (ocManager.isPersonalSeries) {
      $('#mediaList').empty();
      details.media
        .filter(function(file) {
          return file.url.indexOf('http') > -1;
        })
        .forEach(function(file) {
          var filename = file.name;
          var $li = $('<li/>').css('margin-bottom', '0');
          var $a = $('<a/>', {
            href: file.url,
            target: '_blank',
            download: filename,
          });
          $a.append($('<i/>', { class: 'fa fa-download' }).css('margin-right', '0.5em'));
          $a.append($('<span/>', { text: filename }));
          $li.append($a);
          $('#mediaList').append($li);
        });
    }
  }
}.bind(ocManager.eventMgr));

ocManager.eventMgr.on('event.transaction.active', function(details) {
  if (details.active && details.target && $(details.target).data('eventid') === details.id) {
    $(details.target).addClass('activeTransaction');
  }
});

ocManager.eventMgr.on('event.transaction.started', function(details) {
  if (details.target && $(details.target).data('eventid') === details.id) {
    $(details.target).addClass('committing');
  }
});

ocManager.eventMgr.on('event.transaction.complete', function(details) {
  if (details.target && $(details.target).data('eventid') === details.id) {
    $(details.target)
      .removeClass('committing')
      .modal('hide')
      .data('eventid', '');
  }
});

function getEditControls(ev) {
  if (!ev.status) {
    return '';
  }

  switch (ev.status) {
    case 'Upcoming':
      return eventEditable(ev.id);
      break;

    case 'Published':
      return ocManager.isPersonalSeries ? personalEventEditable(ev.id, ev.has_preview) : editPublishedControls(ev.id);
      break;
  }
}

function eventEditable(id) {
  var str = '<button type="button" data-toggle="modal" data-event="' + id + '" data-target="#editModal">' +
            '  <i class="fa fa-pencil"></i></button>' +
            '<button type="button" data-event="' + id + '"  data-target="#delModal">' +
            '  <i class="fa fa-times-circle"></i></button>';
  return str;
}

function editPublishedControls(id) {
  var str = '<button type="button" id="btnDetails" data-toggle="modal" data-event="' + id + '" data-target="#editPublishedModal">' +
            '  <i class="fa fa-pencil"></i></button>' +
            '&nbsp;&nbsp;<button type="button" data-toggle="modal" data-event="' + id + '"  data-target="#retractModal">' +
            '  <i class="fa fa-times-circle"></i></button>'+ 
            '&nbsp;&nbsp;<button type="button" id="btnCaptions_' + id + '"  style="display:none;" data-toggle="modal" data-event="' + id + '" data-target="#editPublishedModal">' +
            '  <i class="fa fa-cc"></i></button>';
  checkCaptions(id);
  return str;
}

function retractedControls(id) {
  var str = '<button type="button" data-toggle="modal" data-event="' + id + '" data-target="#editPublishedModal">' +
            '  <i class="fa fa-pencil"></i></button>' +
            '<button type="button" data-toggle="modal" data-event="' + id + '"  data-target="#publishModal" class="publicationState" title="Publish recording"></button>';
  return str;
}

function personalEventEditable(id, has_preview) {
    var str = '<a type="button" style="padding: 0.5rem;" href="/engage/theodul/ui/core.html?id=' + id + '" target="_blank" title="Watch in player"><i class="fa fa-play-circle-o" style="font-size: 1.25em"></i></a>';

    if (has_preview) {
        str += '<a type="button" style="padding: 0.5rem;" href="/admin-ng/index.html#!/events/events/' + id + '/tools/editor' +
                '?ltimode=true&callback_url=' + encodeURIComponent('/ltitools/manage?sid=' + ocManager.series.id + '&type=personal') + '" title="Edit recording">' +
                '  <i class="fa fa-scissors"></i></a>'
    }

    str += '<button type="button" id="btnDetails" data-toggle="modal" data-event="' + id + '" data-target="#editPublishedModal" title="Edit recording details">' +
            '  <i class="fa fa-pencil"></i></button>';
    str += '&nbsp;&nbsp;<button type="button" data-event="' + id + '"  data-target="#delModal" title="Remove recording">' +
            '  <i class="fa fa-times-circle"></i></button>';
    str += '&nbsp;&nbsp;<button type="button" id="btnCaptions_' + id + '" style="display:none;" data-toggle="modal" data-event="' + id + '" data-target="#editPublishedModal">' +
           '  <i class="fa fa-cc"></i></button>';
    return '<div style="display:flex; justify-content: space-between;">'+ str + '</div>';
}

function getTooltip(details) {
    var status = getStatus(details);

    if (status == "Processing") {
        return "Processing: please check back later";
    }
    else if (status == "Unwanted") {
        return "No event, no consent provided, or recording was published and later retracted.";
    }
    else if(status == "Awaiting Review") {
        return "Queued for editing, or waiting for consent to be provided (if requested)";
    }
    else if(status == "Failed") {
        return "Technical failure: event not recorded successfully";
    }
}

function getStatus(details) {
  var evStatus = 'Processing';

  var isFuture = (new Date()).getTime() < (new Date(details.start_date)).getTime();
  switch(details.event_status) {

    case 'EVENTS.EVENTS.STATUS.SCHEDULED':
      evStatus = isFuture ? 'Upcoming' : 'Expired';
      break;

    case 'EVENTS.EVENTS.STATUS.RECORDING':
      evStatus ='Capturing';
      break;

    case 'EVENTS.EVENTS.STATUS.PROCESSING_FAILURE':
    case 'EVENTS.EVENTS.STATUS.RECORDING_FAILURE':
      evStatus ='Failed'; //TODO: possible different term
      break;

    case 'EVENTS.EVENTS.STATUS.INGESTING':
    case 'EVENTS.EVENTS.STATUS.PROCESSING':
    case 'EVENTS.EVENTS.STATUS.PENDING':
      evStatus = 'Processing';
      break;

    case 'EVENTS.EVENTS.STATUS.PROCESSED':
      if (isFuture) {
          evStatus = 'Upcoming';
      } else {
          if (details.publications && details.publications.length > 0) {
              evStatus = 'Published';
          } else {
              if (!details.has_open_comments){
                  evStatus = 'Unwanted';
              } else {
                  evStatus = 'Awaiting Review';
              }
          }
      }
      break;
    }
  return evStatus;
}

function getVenues(event, expectObj) {
  var res;

  if (!expectObj) {
    res = '';

    for (var key in ocManager.captureAgents) {
      if (!ocManager.captureAgents.__proto__[key]) {
        res += '<li data-ref="' + key + '">' + ocManager.captureAgents[key].name + '</li>';
      }
    }
  }
  return res;
}

function getVenueName(loc) {
  return (ocManager.captureAgents[loc] || {}).name || loc;
}

function getDayTimeInMinutes(momentObj) {
  if (!momentObj) {
    momentObj = moment();
  }
  else if (typeof momentObj == 'string') {
    momentObj = moment(momentObj);
  }
  return momentObj.hours()*60 + momentObj.minutes();
}

function getDayTimeFromMinutes(mins) {
  mins = +mins;
  var timeText = [mins/60 >> 0, mins % 60];
  timeText.map(function(unit) {
    return (unit < 10 ? '0' + unit: '') + unit + '';
  });
  return timeText.join(':');
}

$(document).ready(function() {
  closeSeries();
  $('li[data-ref="date_range"]').daterangepicker({
    opens: 'left',
    autoApply: true
  }, function(start, end, label) {
    if (start) {
      ocManager.eventMgr.addFilter('startDate', moment(start).format('YYYY-MM-DD'));
    }
    if (end) {
      ocManager.eventMgr.addFilter('endDate', moment(end).format('YYYY-MM-DD'));
    }
  });

  $('li[data-ref="time_range"]').timerangepicker({
    onpopup: function() {
               function populateTimes(arr, which) {
                 var timeWrapper = $('.timepicker .timeWrapper')[which];
                 if (timeWrapper) {
                   $(timeWrapper)
                     .find('.timeDisplaySelected span[data-timetype="Hour"]').html(arr[0])
                     .siblings('span[data-timetype="Minute"]').html(arr[1])
                     .siblings('span[data-meridian]').html(arr[0] < 12 ? 'AM' : 'PM')
                   .end()
                     .find('.timeSelect[data-timetype="Hour"] .btn[value="' + ((arr[0]%12 < 10 ? '0' : '') + arr[0] % 12) + '"]')
                       .addClass('active')
                     .siblings('.timeSelect[data-timetype="Minute"] .btn[value="' + ((arr[1] < 10 ? '0' : '') + arr[1]) + '"]')
                       .addClass('active')
                   .end()
                     .find('.timeSelect[data-timetype="Meridian"] .btn[value="' + (arr[0] < 12 ? 'AM' : 'PM') + '"]')
                       .addClass('active');
                 }
               }
             }
  }, function(start, end) {
    if (start) {
      ocManager.eventMgr.addFilter('startTime', start.split(':').map(function(val) { return parseInt(val); }));
    }
    if (end) {
      ocManager.eventMgr.addFilter('endTime', end.split(':').map(function(val) { return parseInt(val); }));
    }
  });

  $('[aria-describedby="statusDropdown"],[aria-describedby="dayDropdown"]').on('click', 'li', function(e) {
    var filterVal = $(this).data('ref');
    var filterField = ($(this).parent().attr('aria-describedby').split('D'))[0];

    if (filterVal == 'All') {
      ocManager.eventMgr.removeFilter(filterField);
      return;
    }

    if (!$(this).hasClass('active')) {
      ocManager.eventMgr.addFilter(filterField, filterVal);
    }
    else {
      ocManager.eventMgr.removeFilter(filterField, filterVal);
    }
  });

  $('input[name=freeform]').on('keyup', function(e) {
    if ($(this).val()) {
      ocManager.eventMgr.addFilter('freeform', $(this).val());
    }
    else {
      ocManager.eventMgr.removeFilter('freeform');
    }
  });

  var tbl = $('#grid');
  tbl.selectRows = function(event) {
    var checked = $('#grid-body td input[type="checkbox"]:checked').length,
            all = $('#grid-body td input[type="checkbox"]').length,
            chk = $('#chk-head').data('customCheckbox');

    $('#btns-select').addClass('visible');

    if (all === checked) {
      chk.selectAll(false);
    } else if (checked > 0) {
      chk.selectPartial(false);
    } else {
     chk.selectNone(false);
     $('#btns-select').removeClass('visible');
    }
  };

  $('#chk-head').customCheckbox({
    partial: false
  }).on('selected.all', function() {
    ocManager.eventMgr.addSelection(
      $('#grid-body tr[data-status="Upcoming"] input[type="checkbox"]')
        .toArray()
        .map(function(input) {
          $(input).prop('checked', true);
          return input.id.replace(/chk-/g, '');
        })
    );
  })
  .on('selected.none', function() {
    ocManager.eventMgr.removeSelection(
      $('#grid-body tr[data-status="Upcoming"] input[type="checkbox"]')
        .toArray()
        .map(function(input) {
          $(input).prop('checked', false);
          return input.id.replace(/chk-/g, '');
        })
    );
  });

  $('#clearFilters').on('click', function() {
    ocManager.eventMgr.removeFilters();
  });

  $('#clearSelections').on('click', function(e) {
    ocManager.eventMgr.removeAllSelections();
  });

  $('.timeInput').on('keyup', function(e) {
    e.keyCode = e.keyCode || e.which;
    if (e.keyCode === 13) {
      e.preventDefault();
      e.stopImmediatePropagation();
    }
  });

  $('.container').on('click',
        'button[data-target="#editModal"], button[data-target="#bulkModal"], button[data-target="#editPublishedModal"]',
    function(e) {
      var target = $(this).data('target');
      var event = (target == '#editModal' || target == '#editPublishedModal' ? ocManager.eventMgr.getEventDetails($(this).data('event')) : ocManager.eventMgr.getCommonSelections());

      if (!event || (Array.isArray(event) && event.length === 0)) return;

      event.target = target;
      event.isAttachmentAllowed = ocManager.isAttachmentAllowed;
      event.isPersonal = ocManager.isPersonalSeries;

      $(target).data('eventid', event.id);

      if (!event.duration) {
        if (event.startTime) {
          var startArr = event.startTime.split(':').map(function(unit) { return +unit });
          var endArr = event.endTime.split(':').map(function(unit) { return +unit });
          event.duration = (endArr[0] - startArr[0]) * 60 + endArr[1] - startArr[1];
        }
        else {
          event.duration = moment.duration(moment(event.end_date).diff(moment(event.start_date))).asMinutes()
        }
      }

      $(target + ' h4').html('<span style="color:#555">Edit:</span> ' + ((event.title !== 'Multiple' ? event.title : '') || 'event(s)'));

      var timeOpts = {
      }

      $(target + ' .modal-body').html(tmpl('tmpl-editormodal', event));

      if (target == '#editPublishedModal') {
        ocManager.eventMgr.checkActiveTransaction(event.id, {target: target});
        if (ocManager.isPersonalSeries) {
          var $seriesList = $(target).find('.seriesList .filterList');
          $seriesList.empty();
          ocManager.user.availableSeries
            .map(function(series) {
              var $item = $('<li/>', {
                            'data-ref': series.id,
                            text: series.title
                          });
              return $item;
          })
          .forEach(function($seriesItem) {
            $seriesList.append($seriesItem);
          });
        }
        $('#hiddenEvent').attr('data-event', event.id);
        $('#hiddenEvent').attr('data-title', event.title);
        return ocManager.eventMgr.getEventAssets(event.id, {target: target});
      }

      $(target + ' .modal-body')
        .find('input[name=duration]').timesetter({
          setTime: event.startTime || moment(event.start_date).format('HH:mm'),
          setDuration: event.duration,
          opens: (window.innerHeight < 750 ? 'right side' : 'left')
        }, function(start, end, self) {
          var startMinutes = start.split(':').reduce(function(total, current, index) {
                              return total + current * Math.pow(60, (1 - index));
                            }, 0);

          var endMinutes = end.split(':').reduce(function(total, current, index) {
                              return total + current * Math.pow(60, (1 - index));
                            }, 0);

          var durationMinutes = endMinutes - startMinutes;

          $(self).find('.selectedValue').html(start + ' - ' + end);
          $(self).find('input[name=duration]').val(durationMinutes);
          $(self).find('input[name=startTime]').val(start);

          var changes = ocManager.getInputs(target);
          var endTime = +changes.hour * 60 + +changes.minute + +changes.duration;
          changes.startTime = changes.hour + ':' + changes.minute;
          changes.endTime = [(endTime/60 >> 0) % 24, endTime % 60]
                              .map(function(unit) {
                                return (unit < 10 ? '0' : '') + unit;
                              })
                              .join(':');

          $(target).addClass('checkConflicts')
            .find('.modal-footer .btn-success').prop('disabled', true);
          ocManager.checkConflictsForUpdate(ocManager.eventMgr.getEventsFromSelections(event.id.split(',')), changes)
            .then(function() {
              ocManager.clearConflicts(target);
            })
            .fail(function(conflicts) {
              ocManager.displayUpdateConflicts(conflicts, changes);
            }.bind(this))
            .always(function() {
              $(target).removeClass('checkConflicts')
                .find('.btn-success').prop('disabled', false);
            });
        });

      var datePickerOpts = {
        singleDatePicker: true
      }
      if (event.start_date !== 'Multiple') {
        datePickerOpts.setStartdate = moment(event.start_date).format('YYYY-MM-DD');
      }

      $(target + ' .modal-body').find('[data-ref="start_date"]')
        .daterangepicker(datePickerOpts, function(start) {
          var parent = this.element[0];
          $(parent).find('input[type=hidden]').val(moment(start).format('YYYY-MM-DD'));
          $(parent).find('.selectedValue').html(moment(start).format('DD MMM, YYYY'));

          $(target).find('button.btn-success').attr('disabled', 'true');
          var changes = ocManager.getInputs(target);
          var endTime = +changes.hour * 60 + +changes.minute + +changes.duration;
          changes.startTime = changes.hour + ':' + changes.minute;
          changes.endTime = [(endTime/60 >> 0) % 24, endTime % 60]
                              .map(function(unit) {
                                return (unit < 10 ? '0' : '') + unit;
                              })
                              .join(':');

          $(target).addClass('checkConflicts')
            .find('.modal-footer .btn-success').prop('disabled', true);
          ocManager.checkConflictsForUpdate(ocManager.eventMgr.getEventsFromSelections(event.id.split(',')), changes)
            .then(function() {
              ocManager.clearConflicts(target);
            })
            .fail(function(conflicts) {
              ocManager.displayUpdateConflicts(error, changes);
            }.bind(this))
            .always(function() {
              $(target).removeClass('checkConflicts')
                .find('.btn-success').prop('disabled', false);
            });
      });
  });

  $('#grid-body').on('click', 'button.publicationState', function(e) {
    var target = $(this).data('target');
    $(target).data('eventid', $(this).data('event'));
  });

    function statusCellInner(text, eventId, edit) {
      var container = $('<span/>');
      if (edit && text == 'Upcoming') {
        var editImg = $('<img/>', {src: '/ltitools/shared/img/icons/edit.png'});
        var deleteImg = $('<img/>', {src: '/ltitools/shared/img/icons/cross.png'});
        var editAnchor = $('<a/>', {
                           title: "Edit this recording",
                           href: '../schedule/index.html?sid='+ seriesId + '&upload=' + uploadSetting +  '&eventId=' + eventId + '&edit=true'
                         });
        var deleteAnchor = $('<a/>', {
                           title: "Delete this recording",
                         });
        $(deleteAnchor).on('click', function(e) {
          var allowEdit = ($(this).parents('span').hasClass('noEdit') ? false : true);
          if (!allowEdit) return;
          e.preventDefault();
          $('#deleteModal').addClass('display');
          var parent = $(this).parents('td');
          $('#deleteModal form').attr('action', '/admin-ng/event/' + $(parent).data('eventid'));
          $('#deleteModal .focus').html($(parent).data('title'));
        });

        $('body').on('click', '.timeSelect .btn', function(e) {
          $(this).siblings('.btn.active')
            .removeClass('active');
          $(this).addClass('active');

          var timeValue = $(this).val();
          var timeType = $(this).parents('.timeSelect').data('timetype');

          var timeEl = $(this).parents('.timeWrapper')
                          .find('.timeDisplaySelected span[data-timetype="' + timeType + '"]')

          timeEl.html(timeValue);

          if (timeEl.parents('.pristine').length > 0) {
            if (timeEl.siblings('span')[0].innerHTML !== '') {
              timeEl.parents('.pristine')
                .removeClass('pristine');
            }
          }
        });
      }
    }

  $('body').on('click', '.timepicker', function(e) {
    e.stopImmediatePropagation();
  });

  $('body').on('click', '.timetrigger', function(e) {
    e.stopImmediatePropagation();
    $(this).find('.timepicker').toggleClass('active');
  });


  $('body').on('click', function(e) {
    $('.timepicker').each(function() {
      $(this).removeClass('active');
    });
  });

  $('body').on('keyup', '.dropdown input[name=filterlist]', function(e) {
    e.stopImmediatePropagation();
    var search = $(this).val();

    $(this).parents('.dropdown')
      .find('.filterList li').each(function() {
        if ($(this).data('ref').toLowerCase().indexOf(search) > -1 ||
            $(this).html().toLowerCase().indexOf(search) > -1) {
          $(this).show();
        }
        else $(this).hide();
      });
  });

  $('#neverRemindTimetable').on('change', function(e) {
    localStorage.setItem('timetableNoRemind', this.checked);
  });

  $('body').on('keyup', function(e) {
    if ($('#selectDropdown.open').length > 0) {
      e.keyCode = e.keyCode || e.which;

      if (e.keyCode === 110 || e.keyCode === 46) {
        $('#selectionList li.active .remove').trigger('click');
      }
      else if (e.keyCode === 27) {
        $('body').click();
      }
    }
  });
  $(window).on('keydown', function(e) {
    if ($('#selectDropdown.open').length > 0) {
      e.keyCode = e.keyCode || e.which;
      switch (e.keyCode) {
        case 38:
        case 40:
          e.preventDefault();
          break;
      }
      var scrolled = $('#selectionList').scrollTop();
      var height =  $('#selectionList').height();

      if (e.keyCode === 40) {
        var curActive = document.querySelector('#selectionList li.active');
        var newActive = curActive ? (curActive.nextElementSibling || curActive) : document.querySelector('#selectionList li');
        $(newActive).click();

        var elBottom = $(newActive).offset().top - $(newActive).offsetParent().offset().top + Math.ceil($(newActive).height()) + scrolled;
        if (elBottom > scrolled + height) {
          $('#selectionList').scrollTop($('#selectionList').scrollTop() + Math.ceil($(newActive).height()));
        }
      }
      else if (e.keyCode === 38) {
        var curActive = document.querySelector('#selectionList li.active');
        var newActive = curActive ? (curActive.previousElementSibling || curActive) : document.querySelector('#selectionList li');
        $(newActive).click();

        var elBottom = $(newActive).offset().top - $(newActive).offsetParent().offset().top + scrolled;
        if (elBottom < scrolled) {
          $('#selectionList').scrollTop($('#selectionList').scrollTop() - Math.ceil($(newActive).height()));
        }
      }
    }
  });

  $('.modal').on('click', '.dropdown-menu .filterList li', function(e) {
    var val = $(this).data('ref');
    var text = $(this).html();

    $(this).addClass('active');
    $(this).parents('[data-field]')
      .find('input.setField').val(val)
        .end()
      .find('.dropdown-toggle').html(text);

    if (ocManager.isPersonalSeries) {
      return;
    }

    var isNewEvent = $('.modal.in').attr('id') === 'scheduleModal';

    if (isNewEvent) {
      ocManager.checkForCurrentConflicts($(this).parents('form'));
    }
    else {
      var _modal = $(this).parents('.modal');
      var eventsArr = _modal.data('eventid').split(',');
      var changes = ocManager.getInputs(_modal);
      $(_modal).addClass('checkConflicts').find('.modal-footer .btn-success').attr('disabled', true);
      ocManager.checkConflictsForUpdate(ocManager.eventMgr.getEventsFromSelections(eventsArr), changes)
        .then(function() {
          ocManager.clearConflicts(_modal);
        })
        .fail(function(conflicts) {
          ocManager.displayUpdateConflicts(conflicts, changes);
        })
        .always(function() {
          $(_modal).find('.modal-footer .btn-success').attr('disabled', false);
        });
    }
  });

  $('.modal').on('click', '.modal-footer .btn-default', function(e) {
    $modal = $(this).parents('.modal');
    $modal.modal('hide');
    $modal.removeClass('conflicts').removeClass('all')
           .removeClass('noproceed').removeClass('committing')
           .removeClass('committed').removeClass('activeTransaction');
    $modal.find('.scheduleDuration').text('');
    $modal.find('.conflicts').text('');
    $modal.find('button[data-toggle=dropdown]').text('');
    $modal.find('video').attr('src', '');
    if ($(this).parents('#scheduleModal').length > 0) {
      $(this).siblings('.btn-success').prop('disabled', 'true');
    }
  });

  $('#editPublishedModal').on('click', '.close', function(e) {
    $modal = $(this).parents('.modal');
    $modal.modal('hide');
    $modal.removeClass('conflicts').removeClass('all')
           .removeClass('noproceed').removeClass('committing')
           .removeClass('committed').removeClass('activeTransaction');
    $modal.find('video').attr('src', '');
  });

  $('body').on('change', '#proceedNonConflicts', function(e) {
    if ($(this).is(':checked')) {
      $(this).parents('.modal').removeClass('all');
    }
    else {
      $(this).parents('.modal').addClass('all');
    }
  });

  $('body').on('change', 'input[type=range][name=duration_range]', function(e) {
    var val = parseInt($(this).val());
    var hours = parseInt(val/60);
    var mins = val - hours*60;
    $(this).next().html('<span>0' + hours + '</span><span>' + (mins < 10 ? '0' : '') + mins + '</span>');
  });
  $('body').on('change, input', 'input[type=range][name=duration_range]', function(e) {
    var val = parseInt($(this).val());
    var hours = parseInt(val/60);
    var mins = val - hours*60;
    $(this).next().html('<span>0' + hours + '</span><span>' + (mins < 10 ? '0' : '') + mins + '</span>');
  });

  $('body').on('submit', '.modal:not(#uploadModal):not(#scheduleModal):not(#ttScheduleModal) form', function(e) {
    e.preventDefault();
    var _modal = $(this).parents('.modal')[0];
    var isBulk = $(_modal).attr('id') === 'bulkModal';
    var events = isBulk ? ocManager.eventMgr.selections : [$(_modal).data('eventid')];
    var changes = ocManager.getInputs(_modal);

    if (!isBulk) {
      $(_modal).addClass('committing');

      var event = events[0];
      if (typeof event == 'string') {
        event = ocManager.eventMgr.getEventDetails(event);
      }
      var eventId = event.id;
      var title = event.title;

      opts = {};
      if (changes["attachment/notes"]) {
        opts.hasFile = true;
      }

      $.Deferred(function(d) {
        if (!opts.hasFile) return d.resolve();

        ocManager.eventMgr.addLectureNotes(eventId, changes)
          .then(function() {
            d.resolve();
          })
          .fail(function() {
            console.log('failed uploading lecture notes');
          });
      }).promise()
      .then(function() {

        var runUpdate = function() {
          /** Delay republish if this is for a personal series. The following events will be observed later
          *   1. event.update.complete => after this event, if series was changed, update ACLs.
          *   2. event.update.acl => after this event, republish event metadata.
          */
          opts.delayPublish = ocManager.isPersonalSeries;
          ocManager.eventMgr.updateEvent(eventId, changes, opts)
            .then(function() {
              if (!ocManager.isPersonalSeries) {
                removeModal(_modal, title);
              }
            });
        };

        if (!opts.hasFile) return runUpdate();
        else return removeModal();
      })
      .fail(function(err) {
        ocManager.displayUpdateConflicts(events);
      });
      return;
    }

    $(_modal).addClass('ready');

    setTimeout(function() {
      $(_modal).addClass('committing');
      ocManager.eventMgr.updateEvents(events, changes)
        .then(function(errors) {
          $(_modal).addClass('committed').removeClass('committing');

          var completedIds = Object.keys(errors).filter(function(errorId) {
                               var error = errors[errorId];
                               return !error[0] && !error[1];
                             });
          ocManager.eventMgr.removeSelection(completedIds);

          var errorIds = Object.keys(errors).filter(function(errorId) {
                           var error = errors[errorId];
                           return !!error[0] || !!error[1];
                         });
          $(_modal).data('eventid', errorIds.join(','));
          if (errorIds.length > 0) {
            ocManager.displayUpdateErrors(errorIds, changes);
          }
          else {
            $(_modal).removeClass('committed')
              .find('.btn-default[type=reset]').trigger('click');
            $('p[data-instruction=info]')
              .html(completedIds.length + ' event(s) updated')
              .addClass('show');

               setTimeout(function() {
                 $('p[data-instruction=info]').removeClass('show');
                 $(_modal).removeClass('ready');
               }, 5000);
          }
        });
    }.bind(this), 16);
  });

  $('#bulkModal').on('reset', 'form', function() {
    $(this).find('input,textarea,select').each(function() {
      $(this).val('');
    });
  });
  $('#bulkModal').on('click', '#updateProgress .btn', function() {
    $('#bulkModal').removeClass('committed');
    setTimeout(function() {
      $('#bulkModal').removeClass('ready');
      $('#updateProgress ul').empty();
    }, 500);
  });
  $('#retractModal').on('submit', function(e) {
    e.preventDefault();
    var eventId = $(this).data('eventid');
    var changes = ocManager.getInputs(this);
    if (!changes.comment) {
      ocManager.eventMgr.retract(eventId, {target: this});
    }
    else {
      ocManager.eventMgr.comment(eventId, changes.comment, {target: this})
        .then(function() {
          ocManager.eventMgr.retract(eventId, {target: this});
        }.bind(this));
    }
    $(this).addClass('committing');
  });
  $('#publishModal').on('submit', function(e) {
    e.preventDefault();
    var eventId = $(this).data('eventid');
    ocManager.eventMgr.publish(eventId, {target: this});
    $(this).addClass('committing');
  });

  $('#grid-body').on('click', 'button[data-target="#retractModal"], button[data-target="#publishModal"]', function(e) {
    var target = $(this).data('target');
    try {
      var eventId = $(this).data('event');
      var event = ocManager.eventMgr.getEventDetails(eventId);
      $(target)
        .data('eventid', eventId)
        .find('p.focus').text(event.title);
    } catch(e) {
      $(target).find('p.focus').text('');
    }
  });

  $('body').on('click', '#delModal .modal-footer .btn-success', function(e) {
    $('#delModal').addClass('committing');
    ocManager.eventMgr.deleteEvents();
  });

  $('.container').on('click', 'button[data-target="#delModal"]', function(e) {
    var eventId = $(this).data('event');
    if (eventId) {
      ocManager.eventMgr.logEventRemoval(eventId);
    }
    else if ($(this).parents('#grid-header').length > 0) {
      ocManager.eventMgr.once('selection', function(selections) {
        ocManager.eventMgr.logEventRemoval(selections.map(function(event) { return event.id }));
      });
    }
  });

  $('#selectionList').on('click', 'li', function(e) {
    var id = $(this).data('eventid');

    if (!$(this).hasClass('active')) {
      $(this).siblings('.active').removeClass('active');
      $('#selectedInfo li.active').removeClass('active');

      $(this).addClass('active');
      $('#selectedInfo li[data-eventid="' + id + '"]').addClass('active');
    }
  });
  $('#selectionList').on('click', '.remove', function(e) {
    e.stopImmediatePropagation();
    var id = $(this).parent().data('eventid');
    ocManager.eventMgr.removeSelection(id);
  });
  $('body').on('click', '[aria-labelledby="selections"]', function(e) {
    e.stopImmediatePropagation();
  });

  $('.timeInput').on('keyup', function(e) {
    var timeText = $(this).val();
    $(this).next().find('li')
      .each(function() {
        if ($(this).html().indexOf(timeText) > -1) {
          $(this).addClass('keyShow');
        }
        else {
          $(this).removeClass('keyShow');
        }
      });
  });

  $('.timeInput').on('change', function(e) {
    var timeText = $(this).val();

    var isStart = $(this).attr('name') == 'startTime' ? 1 : -1;
    var timeArr = timeText.split(':')
                    .map(function(unit) { return +unit });

    var _otherTimeInput = (isStart === 1 ? $(this).siblings('input[name=endTime]') : $(this).siblings('input[name=startTime]'));
    var otherTimeArr = _otherTimeInput.val().split(':')
                         .map(function(unit) { return +unit; });

    if (!timeText) {
      _otherTimeInput.next().find('li')
        .each(function() {
          $(this).removeClass('hidden');
        });
      return;
    }

    $(this).siblings('.scheduleDuration').html('');

    if (timeArr.length == 2 && otherTimeArr.length == 2) {
      var duration = ((otherTimeArr[0] - timeArr[0]) * 60 + otherTimeArr[1] - timeArr[1]) * isStart;
      if (duration <= 0) {
        $(this).val('');
        _otherTimeInput.next().find('li')
          .each(function() {
            $(this).removeClass('hidden');
          });
        return;
      }

      $(this).siblings('input[name=duration]').val(duration);
      var durationDisplay = parseInt(duration/60) + 'h' + (duration%60 < 10 ? '0' : '') + duration%60 + 'm';
      $(this).siblings('.scheduleDuration').html(durationDisplay);

      ocManager.checkForCurrentConflicts($('.modal.in'));
    }

    _otherTimeInput.next().find('li')
     .each(function() {
        var thisTimeArr = $(this).html().split(':')
                             .map(function(unit) { return +unit });

        if ((thisTimeArr[0] - timeArr[0]) * isStart > 0) {
          $(this).removeClass('hidden');
        }
        else if (timeArr.length > 1 && timeArr[0] === thisTimeArr[0] && (thisTimeArr[1] - timeArr[1]) * isStart > 0) {
          $(this).removeClass('hidden');
        }
        else {
          $(this).addClass('hidden');
        }
      });

    $(this).next().find('li')
      .each(function() {
        $(this).addClass('keyShow');
      });
  });

  $('#scheduleModal').on('change', 'input[name="repeatdays[]"]', function(e) {
    ocManager.checkForCurrentConflicts($('#scheduleModal'));
  });

  $('#choose_start,#choose_end,span[data-target="#upload_start"]').singleDatePicker(
    function(moment_date, picker) {
      var _this = picker.element;
      if (_this.attr('id') && moment_date.isBefore(moment().subtract(1, 'day'))) {
        $(_this).addClass('invalid');
        setTimeout(function() {
          $(_this).removeClass('invalid');
        }, 3000);
        return;
      }

      var targetId = _this.data('target');
      var valid = true;

      if ($(_this).prev().length > 0 && $(_this).prev().find('input').val()) {
        valid = moment_date.isAfter($(_this).prev().find('input').val());
      }
      else if ($(_this).next().length > 0 && $(_this).next().find('input').val()) {
        valid = moment_date.isBefore($(_this).next().find('input').val());
      }

      if ($(_this).attr('id') && moment_date.isBefore(moment())) {
        var minTime = +moment().add(5, 'minute').format('HH')*60 + +moment().add(5, 'minute').format('mm');
        $('#scheduleModal .timeSelector li').each(function() {
          var timeInDay = $(this).html().split(':')
                            .reduce(function(total, unit, i) {
                              if (i < 2) {
                                total += +unit * Math.pow(60, 1 - i);
                              }
                              return total;
                            }, 0);

          if (timeInDay < minTime) {
            $(this).addClass('hideOnChosenDate');
          }
        });
        $('#scheduleTime .timeInput').each(function() {
          var thisTime = $(this).val().split(':')
                            .reduce(function(total, unit, i) {
                              if (i < 2) {
                                total += +unit * Math.pow(60, 1 - i);
                              }
                              return total;
                            }, 0);

          if (thisTime < minTime) {
            $(this).val('');
            $(this).next().find('li').each(function() {
              $(this).removeClass('hidden');
            });
            $('.scheduleDuration').html('');
          }
        });
      }
      else {
        $('#scheduleModal .timeSelector li').each(function() {
            $(this).removeClass('hideOnChosenDate');
        });
      }

      if (!valid) {
        $(_this).addClass('invalid');
        setTimeout(function() {
          $(_this).removeClass('invalid');
        }, 3000);
        return;
      }

      $(targetId).val(moment_date.format('YYYY-MM-DD'));
      _this.find('.selectedValue').html(moment_date.format('DD MMM YYYY'));
      ocManager.checkForCurrentConflicts($('#scheduleModal'));
    }
  );

  $('#scheduleModal').on('submit', 'form', function(e) {
    try {
      e.preventDefault();

      $('#scheduleModal').removeClass('error');
      $(this).addClass('committing');
      var changes = ocManager.getInputs(this);

      var success = true;
      ocManager.eventMgr.createEvent(changes, false)
        .fail(function(error) {
          success = false;
          if (Array.isArray(error)) {
            ocManager.displayScheduleConflicts(error, changes);
          }
          else {
            ocManager.displayCreationError(error);
          }
        })
        .always(function() {
          $(this).removeClass('committing');
          if (success) {
            $(this).find('button[type=reset].btn-default')[0].click();
          }
        }.bind(this));

    } catch(err) {
    }
  });
  $('#uploadModal').on('submit', 'form', function(e) {
    try {
      e.preventDefault();

      $(this).addClass('uploading');
      var changes = ocManager.getInputs(this);

      var success = true;

      ocManager.eventMgr.createEvent(changes)
        .fail(function(err) {
          $('#uploadModal').find('li.errors').text(err.error);
          success = false;
        })
        .always(function() {
          $(this).removeClass('uploading');
          if (success) {
            $(this).find('button[type=reset].btn-default')[0].click();
          }
        }.bind(this));

    } catch(err) {
    }
  });
  $('#scheduleModal').on('reset', 'form', function(e) {
    $(this).find('.selectedValue,.errors').each(function() {
      $(this).html('');
    });
    $(this).find('input[type=hidden]').each(function() {
      $(this).val('');
    });
    $(this).find('li').each(function() {
      $(this).removeClass('hidden');
    });
    $('#scheduleModal').removeClass('error');
  });
  $('#uploadModal').on('change', '.fileContainer input[type=file]', function(e) {
    if (this.files.length === 0) {
      return;
    }

    var mediaType = $(this).data('mediatype');
    var file = this.files[0];
    if (file.type.indexOf("video") === -1 && file.type.indexOf("audio") === -1) {
      $(this).parent().attr('data-title', 'Please provide a file of video or audio type');
      $(this).val('');
      $(this).parent().prev()[0].checked = false;
      return;
    }

    var fileName = this.files[0].name;
    $(this).parent().prev()[0].checked = true;
    $(this).parent().attr('data-title', fileName);
    $(this).parent().siblings('.videoPreview').find('img').removeAttr('src');

    var self = this;

    if (file.type.indexOf('video') > -1) {
      var vidPreview = document.createElement('video');
      var canvas = document.createElement('canvas');
      vidPreview.muted = true;
      var videoReader = new FileReader();

      videoReader.onload = function() {
        var previewBlob = new Blob([videoReader.result],  {type: file.type});
        $(self).parent().siblings('.videoPreview').attr('data-previewerror', 'Preview Unavailable');
        vidPreview.onloadeddata = function() {
          this.currentTime = this.seekable.end(0) === Infinity ? 10 : this.seekable.end(0);
        };
        vidPreview.ontimeupdate = function() {
          canvas.width = this.videoWidth;
          canvas.height = this.videoHeight;
          canvas.getContext('2d').drawImage(vidPreview, 0, 0, canvas.width, canvas.height);
          var img = new Image();
          img.onload = function() {
            $(self).parent().siblings('.videoPreview').find('img').attr('src', img.src);
            $(self).parent().siblings('.videoPreview').removeAttr('data-previewerror');
            img = null;
            canvas = null;
            videoPreview = null;
            videoReader = null;
            previewBlob = null;
          }
          img.src = canvas.toDataURL();
        }
        vidPreview.src = URL.createObjectURL(previewBlob);
      };

      var vidSlice = file.slice(0, Math.min(file.size, 10000000));
      videoReader.readAsArrayBuffer(vidSlice);
    }
  });

  $('#uploadModal').on('change', '.presentationContainer input[type=file]', function(e) {
    if(this.files.length === 0) {
       return;
    }

    var file = this.files[0];
    if (file.type.indexOf("video") === -1) {
       $(this).parent().attr('data-title', 'Please provide a file of video type');
       $(this).val('');
       $(this).parent().prev()[0].checked = false;
       return;
    }

    var fileName = this.files[0].name;
    $(this).parent().prev()[0].checked = true;
    $(this).parent().attr('data-title', fileName);
    $(this).parent().siblings('.presentationPreview').find('img').removeAttr('src');

    var self = this;
    if (file.type.indexOf('video') > -1) {
         var vidPreview = document.createElement('video');
         var canvas = document.createElement('canvas');
         vidPreview.muted = true;
         var videoReader = new FileReader();

         videoReader.onload = function() {
         var previewBlob = new Blob([videoReader.result],  {type: file.type});
         $(self).parent().siblings('.presentationPreview').attr('data-previewerror', 'Preview Unavailable');
         vidPreview.onloadeddata = function() {
            this.currentTime = this.seekable.end(0) === Infinity ? 10 : this.seekable.end(0);
         };
         vidPreview.ontimeupdate = function() {
             canvas.width = this.videoWidth;
             canvas.height = this.videoHeight;
             canvas.getContext('2d').drawImage(vidPreview, 0, 0, canvas.width, canvas.height);
             var img = new Image();
             img.onload = function() {
                 $(self).parent().siblings('.presentationPreview').find('img').attr('src', img.src);
                 $(self).parent().siblings('.presentationPreview').removeAttr('data-previewerror');
                 img = null;
                 canvas = null;
                 videoPreview = null;
                 videoReader = null;
                 previewBlob = null;
             }
             img.src = canvas.toDataURL();
           }
           vidPreview.src = URL.createObjectURL(previewBlob);
         };

         var vidSlice = file.slice(0, Math.min(file.size, 10000000));
         videoReader.readAsArrayBuffer(vidSlice);
    }

  });

  $('.filePopulated').on('change', function(e) {
    if (!this.checked) {
      $(this).next().find('input[type=file]').val('');
      $(this).next()[0].removeAttribute('data-title');
    }
  });
  $('#editPublishedModal').on('change', 'input[type=file]', function(e) {
    $('#editPublishedModal #errorLi').html('');
    var mediaType = $(this).data('mediatype'),
        file = this.files[0],
        fileContents = "";
    
    if (file) {
      var fileName = file.name;
      var fileNameExt = fileName.substr(fileName.lastIndexOf('.') +1);
      if (fileNameExt != mediaType) {
          $(this).parent().attr('data-title', 'Please provide a file of *.vtt type');
          $(this).val('');
          $(this).parent().prev()[0].checked = false;
          $('.uploadCaptions').hide();
          return;
      } else {
         var reader = new FileReader();
         reader.readAsText(file, "UTF-8");
         reader.onload = function (evt) {
            $('#uploadedVttText').val(evt.target.result);
            fileContents = $('#uploadedVttText').val();
            validateVTT(fileName, fileContents);
         }
         reader.onerror = function (evt) {
            console.log("error reading file");
         } 
      }
    }
  });

  $('.timeSelector').on('mousedown', function(e) {
    var self = this;
    setTimeout(function() {
      $(self).prev().focus();
    }, 10);
  });
  $('.timeSelector').on('click', 'li', function(e) {
    var isStart = $(this).parent().data('timetype') === 'start' ? 1 : -1;
    var $otherTime = $(this).parent().siblings('.timeSelector');

    var thisTime = $(this).html().split(':').reduce(function(total, unit, i) {
                     total += parseInt(Math.pow(60, 1 - i) * +unit);
                     return total;
                   }, 0);

    $otherTime.find('li').each(function() {
      var thatTime = $(this).html().split(':').reduce(function(total, unit, i) {
                       total += parseInt(Math.pow(60, 1 - i) * +unit);
                       return total;
                     }, 0);

      if ((thatTime - thisTime) * isStart > 0 && (thatTime - thisTime) * isStart < +ocManager.org.properties["admin.event.new.max_duration"]) {
        $(this).removeClass('hidden');
      }
      else {
        $(this).addClass('hidden');
      }
    });
  });
  $('.timeSelector').each(function() {
    for (var i = 0; i < 24; i++) {
      for (var j = 0; j < 60; j+=5) {
        var timeText = (i < 10 ? '0' : '') + i + ':' + (j < 10 ? '0' : '') + j;
        var _time = $('<li>', {text: timeText, class: 'keyShow'});
        _time.on('mousedown', function(e) {
          e.stopImmediatePropagation();
          var chosenTime = $(this).html();
          $(this).parent().prev().val(chosenTime);
          $(this).parent().prev().trigger('change');
        });
        $(this).append(_time);
      }
    }
  });
  $('.timeInput').on('change', function(e) {
    var timeArr = $(this).val().split(':');
    if (timeArr.length != 2) {
      $(this).val('');
      $(this).siblings('.scheduleDuration').html('');
      return;
    }


    var isValid = true;
    timeArr.some(function(unit, i) {
      if (isNaN(unit) || +unit < 0 || unit > 59 || (i === 0 && unit > 23)) {
        isValid = false;
        return;
      }
    });

    var allowedValues = [];
    $(this).next().find('.keyShow:not(.hidden):not(.orgHide)').each(function(el) {
      allowedValues.push($(this).text());
    });
    var invalidVal = allowedValues.indexOf($(this).val()) === -1;
    if (!isValid || invalidVal) {
      $(this).val('');
      if (invalidVal) {
        var textDisplay = $(this).siblings('.scheduleDuration');
        textDisplay.addClass('error').text('Select from dropdown');
        setTimeout(function() {
          textDisplay.removeClass('error');
          if (textDisplay.text() === 'Select from dropdown') {
            textDisplay.text('');
          }
        }.bind(this), 5000);
      }
      else {
       $(this).siblings('.scheduleDuration').html('');
      }
      return;
    }

    var otherTimeArr = $(this).siblings('input').val().split(':');
    if (otherTimeArr.length === 2) {
      var isInDuration = Math.abs((+timeArr[0] - +otherTimeArr[0]) * 60 + +timeArr[1] - +otherTimeArr[1]) < +ocManager.org.properties["admin.event.new.max_duration"];

      if (!isInDuration) {
        $(this).val('');
        $(this).siblings('.scheduleDuration').html('');
        return;
      }
    }
  });
  $('.timeSelector').on('mousedown', function(e) {
    var self = this;
    setTimeout(function() {
      $(self).prev().focus();
    }, 10);
  });
  $('.timeContainer').on('change', '> input[type=checkbox]', function(e) {
    var $otherInput = $(this).parent().siblings('.timeContainer').find('input[type=checkbox]');
    if ($otherInput[0] && $otherInput[0].checked) {
      $otherInput[0].checked = false;
      $otherInput.siblings('.showMinute').removeClass('showMinute');
    }

    if (!this.checked) {
      $(this).siblings('.showMinute').removeClass('showMinute');
    }
  });
  $('#scheduleMultiple').on('change', function(e) {
    if (this.checked) {
      $('#sched_end').attr('required', 'true');
    }
    else {
      $('#sched_end').removeAttr('required');
    }
  });
  $('#ttNotificationModal').on('submit', function(e) {
    e.preventDefault();
    e.stopImmediatePropagation();
    $(this).modal('hide');
    setTimeout(function() {
      $('#ttScheduleModal').modal('show');
    }, 100);
  });
  $('#ttScheduleModal').on('click', 'li[data-course]', function(e) {
    if (!$(this).hasClass('heightSet')) {
      $(this).addClass('heightSet');
    }
    else {
    }

  });
  $('#ttScheduleModal').on('click', 'li[data-course] ul, li[data-course] span[data-field=header]', function(e) {
    e.stopImmediatePropagation();
  });
  $('#ttScheduleModal').on('click', 'ul[data-instr-type] > li:not(.conflict)', function(e) {
    if (e.target.tagName.toLowerCase() !== 'input') {
      var checkbox = $(this).find('input[type=checkbox]')[0];
      checkbox.checked = !checkbox.checked;
      $(checkbox).trigger('change');
    }
  });
  $('#ttCourses').on('click', 'li[data-course]:not(.selected):not(.conflict)', function(e) {
    if ($('#ttCourses .selected').length > 0) {
      $('#ttScheduleCourses input').each(function() {
        $('#ttCourses .selected input[name="' + $(this).attr('name') + '"]')[0].checked = this.checked;
      });
      $('#ttCourses .selected').removeClass('selected');
    }
    $('#ttScheduleCourses').html($(this)[0].outerHTML);
    $(this).find('input').each(function() {
      $('#ttScheduleCourses input[name="' + $(this).attr('name') + '"]')[0].checked = this.checked;
    });
    $(this).addClass('selected').removeClass('issue');
  });
  $('#ttScheduleCourses').on('change', 'input[type=checkbox]', function(e) {
    $('#ttCourses input[name="' + this.name + '"]')[0].checked = this.checked;
    var scheduleId = this.name;
    if (this.checked) {
      $('#ttScheduleModal').addClass('checkConflicts');
      var currentChecks = $('#ttScheduleModal').data('checking') || [];
      if (currentChecks.indexOf(scheduleId) === -1) {
        currentChecks.push(scheduleId);
        $('#ttScheduleModal').data('checking', currentChecks);
      }
      var schedule = ocManager.prepareSchedules(JSON.parse(this.value));
      ocManager.eventMgr.checkConflicts(schedule[0].source.metadata)
        .fail(function(err) {
          ocManager.displayTimetableConflict(scheduleId);
        })
        .always(function() {
          currentChecks = $('#ttScheduleModal').data('checking') || [];
          var checkIndex = currentChecks.indexOf(scheduleId);
          if (checkIndex > -1) {
            currentChecks.splice(checkIndex, 1);
            $('#ttScheduleModal').data('checking', currentChecks);
            if (currentChecks.length === 0) {
              $('#ttScheduleModal').removeClass('checkConflicts');
            }
          }
        });
    }
  });

  $('#ttScheduleModal form').on('submit', function(e) {
    e.preventDefault();
    var schedules = $('#ttCourses li[data-course] li:not(.conflict) input:checked')
                     .toArray()
                     .map(function(input) {
                       var schedule = JSON.parse(input.value);
                       schedule.creator = ocManager.series.creator;
                       return schedule;
                     });

    ocManager.eventMgr.createSchedule(schedules);

    $('#ttScheduleModal').removeClass('committing').modal('hide');
    $('p[data-instruction=info]')
      .html('Your chosen timetabled events have been scheduled for recording. The events will display in due course.')
      .addClass('show');

    setTimeout(function() {
      $('p[data-instruction=info]')
        .html('')
        .removeClass('show');
      $('p[data-instruction=warning]')
        .html('')
        .addClass('show');
    }, 10000);

    $('#ttScheduleModal input:checked').each(function() {
      this.checked = false;
    });
  });
  $('#uploadModal').on('shown.bs.modal', function(e) {
    if (!$('#uploadModal input[type=file]')) {
      $('.fileContainer').attr('data-title', 'Choose a presenter video or audio');
      $('.presentationContainer').attr('data-title', 'Choose a presentation video');
    }
    if (!$('#uploadModal input[name=start_date]').val()) {
      $('#uploadModal input[name=start_date]').val(moment().format('YYYY-MM-DD'));
      $('#uploadModal .selectedValue').text(moment().format('DD MMM YYYY'));
    }
    if (!$('#uploadModal textarea[name=presenters]').val() && !$('#uploadModal textarea[name=presenters]').html()) {
      $('#uploadModal textarea[name=presenters]').val(
        ocManager.series.contributor
      );
    }
  });
  $('#uploadModal').on('hidden.bs.modal', function () {
    $('.fileContainer').attr('data-title', 'Choose a presenter video or audio');
    $('.presentationContainer').attr('data-title', 'Choose a presentation video');
    $('.videoPreview').find('img').removeAttr('src');
    $('.presentationPreview').find('img').removeAttr('src');
  });
  $('#editPublishedModal').on('show.bs.modal', function(e) {
    var triggerElement = $(e.relatedTarget),
        id = $('#hiddenEvent').attr('data-event');

    if(triggerElement[0].id === 'btnCaptions_'+ id) {
      getCaptions(id);
      $('#editPublished').hide();
      $('#editPublishedCancel').text("Close");
      $('#detailsLink, #details').removeClass('active');
      $('#captions, #captionsLink').addClass('active');
      $('#editCaptions, #dlNibityCaptions, #dlGoogleCaptions, #dlUploadedCaptions, .uploadCaptions, #rmNibityCaptions, #rmGoogleCaptions, #rmUploadedCaptions').attr('data-event', id);
    }else{
      $('#editPublished').show();
      $('#editCaptionsGroup').hide();
      $('#downloadCaptionsGroup').hide();
      $('#removeCaptionsGroup').hide();
      $('#editPublishedCancel').text("Cancel");
    }
  });
  $('#editPublishedModal').on('click', '.detailsLink', function(){
      $('#editPublished').show();
      $('#editPublishedCancel').text("Cancel");
  });
  $('#editPublishedModal').on('click', '.captionsLink', function(){
      var id = $('#hiddenEvent').attr('data-event');
      $('#editPublished').hide();
      $('#editPublishedCancel').text("Close");
      $('#btnUploadCaptions, #rqCaptions').attr('data-event', id);
  })
  $('#editPublishedModal').on('hidden.bs.modal', function () {
      $('#uploadModal .fileContainer').attr('data-title', 'Choose video');
      $('#editPublishedModal .fileContainer').attr('data-title', 'Choose *.vtt...');
      $('#editPublishedModal #errorLi').html('');
      $('#btnUploadCaptions').attr('data-event', '');
      $('#editCaptions, #dlNibityCaptions, #dlGoogleCaptions, #dlUploadedCaptions, #rmNibityCaptions, #rmGoogleCaptions, #rmUploadedCaptions').attr('data-provider','');
      $('#editCaptions, #dlNibityCaptions, #dlGoogleCaptions, #dlUploadedCaptions, #rmNibityCaptions, #rmGoogleCaptions, #rmUploadedCaptions').attr('data-url','');
      $('#editCaptions, #dlNibityCaptions, #dlGoogleCaptions, #dlUploadedCaptions, #rmNibityCaptions, #rmGoogleCaptions, #rmUploadedCaptions').attr('href','');
      $('#editCaptions').text('');
  });
  $('#editCaptionsModal').on('show.bs.modal', function(e) {
      var title = $('#hiddenEvent').attr('data-title'),
          id = $('#hiddenEvent').attr('data-event'),
          vttProvider =$('#hiddenEvent').attr('data-provider'),
          vttURL = $('#hiddenEvent').attr('data-url'),
          mediaType = $('#hiddenEvent').attr('data-mediatype'),
          label = "Edit " + vttProvider + " VTT";
  
      $('#ecModal').html('<span style="color:#555">Edit:</span> ' + title);   
      $('#vttInfo').attr('data-event', id);
      $('#vttInfo').attr('data-url', vttURL);
      $('#vttInfo').attr('data-mediaType',mediaType); 
      $('#vttLabel').text(label);
      $('.newVtt').load(vttURL);
  });
  $('#editPublishedModal').on('click', '.uploadCaptions', function(e) {
      try {
        e.preventDefault();

        $('.uploadCaptions').addClass('uploading');
        var eventId = $('.uploadCaptions').attr('data-event'),
            mediaType = $('#editPublishedModal .fileContainer').attr('data-mediatype'),
            fileName = $('#editPublishedModal .fileContainer').attr('data-title'),
            newFile = $('#uploadedVttText').val(),
            success = true;
        
        if(newFile) {
            var parts = new Blob([newFile], {type:"text/plain"}),
                f = new File([parts], fileName, {type: mediaType, lastModified: new Date()});
                  
            var changes = {"text/vtt":f};
            ocManager.eventMgr.addCaptions(eventId, changes)
            .fail(function(err) {
                console.log("Failed to upload captions");
                $('#editPublishedModal').find('span.errors').text(err);
                success = false;
            })
            .always(function() {
                $('.uploadCaptions').removeClass('uploading');
                if (success) {
                    $('#editPublishedModal').find('button[type=reset].btn-default')[0].click();
                }
            }.bind(this));
          }
      } catch(err) {
        console.log(err);
      }   
  });
  $('#editCaptionsModal').on('click', '.updateCaptions', function(e) {
      try {
        e.preventDefault();

        $(this).addClass('uploading');
        var eventId = $('#vttInfo').attr('data-event'),
            vttURL = $('#vttInfo').attr('data-url'),
            mediaType = $('#vttInfo').attr('data-mediaType'),
            fileName = vttURL.substring(vttURL.lastIndexOf('/') + 1),
            change = $('#vttText').val(),
            success = true;

        var parts = new Blob([change], {type:"text/plain"}),
            f = new File([parts], fileName, {type: mediaType, lastModified: new Date()});
                
        var changes = {"text/vtt":f};
        ocManager.eventMgr.updateCaptions(eventId, changes)
        .fail(function(err) {
            console.log("Failed to upload edited captions");
            $('#editCaptionsModal').find('span.errors').text(err.error);
            success = false;
        })
        .always(function() {
            $('.updateCaptions').removeClass('uploading');
            if (success) {
                $('#editCaptionsModal').find('button[type=reset].btn-default')[0].click();
            }
        }.bind(this));
      } catch(err) {
        console.log(err);
      }
  });
  $('#editCaptionsModal').on('hidden.bs.modal', function () {
    $('.newVtt').empty();
    $('#ecModal, #vttLabel').text('');
    $('#vttInfo').attr('data-url','');
    $('#vttInfo').attr('data-event','');
    $('#editCaptionsModal #errorLi').html('');
    $('#editCaptionsModal .note').show();
    $('.updateCaptions').hide();
  });
  $('#vttText').on('change keyup paste', function(e) {
    var changes = $(this)[0].value;
    changes = changes.replace(/&/g, "&amp;");
    var Parser = new WebVTTParser();
    var vttText = Parser.parse(changes, 'subtitles/captions/descriptions');
    var ol = $('#editCaptionsModal #errorLi')[0];
    ol.textContent = "";
                             
    if(vttText.errors.length > 0) {
      $('#editCaptionsModal #errorLi').show();
      for(var i = 0; i < vttText.errors.length; i++) {
          var error = vttText.errors[i],
              message = "Line " + error.line,
              li = document.createElement("li");
            
          li.textContent = message + ": " + error.message;
          ol.appendChild(li);
          $('#editCaptionsError').show();        
          $('#editCaptionsModal .updateCaptions').hide();
        }
    } else {  
        $('#editCaptionsModal .updateCaptions').show();
    }
     var s = new WebVTTSerializer()
     s.serialize(vttText.cues);
  });
   $('#editPublishedModal').on('click', '#rqCaptions', function(e) {
     $.ajax({
         url:"/api/workflows",
         method:"POST",
         data:{
             event_identifier: $(this).attr('data-event'),
             workflow_definition_identifier: "uct-request-transcript",
             withoperations: false,
             withconfiguration: false,
          },
     }).done(function(response) {
         console.log(response.description);
     }).fail(function( jqXHR, textStatus ) {
         console.log(textStatus);
     });
   });
   $('#editPublishedModal').on('click', '#rmGoogleCaptions, #rmNibityCaptions, #rmUploadedCaptions', function(e) {
    var eventId = $(this).attr('data-event'),
        modalTitle = "",
        captionsProvider = $(this).attr('data-provider');

    $('#removeCaptionsModal #eventDetails').attr('data-event', eventId);
    $('#removeCaptionsModal #eventDetails').attr('data-provider', captionsProvider);

    if(captionsProvider === "googleTranscript") {
       modalTitle = "Remove Automated Captions";
    }
    if(captionsProvider === "nibityTranscript") {
       modalTitle = "Remove Way with Words Captions";
    }
    if(captionsProvider === "uploadedTranscript") {
       modalTitle = "Remove Uploaded Captions";
    }
    $('#removeCaptionsModal #rcModalTitle').text(modalTitle);
   });
   $('#removeCaptionsModal').on('click', '#cancelRemoveCaptions', function(e) {
        $('#removeCaptionsModal').modal('hide');
   });
   $('#removeCaptionsModal').on('click', '#confirmRemoveCaptions', function(e) {
           var eventId = $('#eventDetails').attr('data-event'),
               captionsProvider = $('#eventDetails').attr('data-provider');

           removeCaptions(eventId, captionsProvider);
           $('#removeCaptionsModal').modal('hide');
           $('#editPublishedModal').modal('hide');
   });
});

function removeCaptions(eventId, captionsProvider) {
    var fd = new FormData(),
        payload = {},
        workflow = "uct-remove-transcripts";

    payload[captionsProvider] = "true";
    fd.append('workflow_definition_identifier', workflow);
    fd.append('event_identifier', eventId);
    fd.append('configuration', JSON.stringify(payload));

    $.ajax({
        url: '/api/workflows',
        type: 'post',
        data: fd,
        processData: false,
        contentType: false,
        cache: false
    }).done(function(response) {
        console.log(response.description);
    }).fail(function( jqXHR, textStatus ) {
        console.log(textStatus);
    });
}

function removeModal(_modal, title) {
  $(_modal).removeClass('committing')
    .find('.btn[type=reset]').trigger('click');

  $('p[data-instruction=info]')
    .html('Changes to <b>' + title + '</b> submitted. Please note that additional processing may take place before changes reflect fully.')
    .addClass('show');

  setTimeout(function() {
    $('p[data-instruction=info]').removeClass('show');
  }, 15000);
}

function validateVTT(fileName, fileContents) {
  var Parser = new WebVTTParser(),
      vttText = Parser.parse(fileContents, 'subtitles/captions/descriptions');

  if(vttText.errors.length > 0) {
    for(var i = 0; i < vttText.errors.length; i++) {
        $('#editPublishedModal .errors').show();
    }
  } else {
      $('#btnUploadCaptions').show();
      $('#editPublishedModal .errors').hide();
      $('#editPublishedModal #populatedPresVtt').checked = true;
      $('#editPublishedModal #populatedPresVtt').text(fileName).attr('title', fileName);
      $('#editPublishedModal .fileContainer').attr('data-title', fileName);
  }
  var s = new WebVTTSerializer()
  s.serialize(vttText.cues);
}

function checkCaptions(id) {
  var url = '/search/episode.json?limit1&id=' + id;
  $.get({url: url},
    function(response) {
        var attachments = response["search-results"]["result"]["mediapackage"]["attachments"]["attachment"];
        for(var i = 0; i < attachments.length; i++) {
            if(attachments[i].mimetype === "text/vtt" && attachments[i].tags["tag"].indexOf("engage-download") >= 0) {
              if($('#btnCaptions_' + id).hide()) {
                 $('#btnCaptions_' + id).show();
              }
            } 
        }
   })
}

function getCaptions(id) {
  var url = '/search/episode.json?limit1&id=' + id;
  var provider, mediaType, vttURL;
  var providerArray = [];

  $.get({url: url},
    function(response) {
        var attachments = response["search-results"]["result"]["mediapackage"]["attachments"]["attachment"];
        for(var i = 0; i < attachments.length; i++) {
            if(attachments[i].mimetype === "text/vtt" && attachments[i].tags["tag"].indexOf("engage-download") >= 0) {
              if(attachments[i].type == "captions/timedtext") {
                  providerArray.push({"id" : id, "mediatype" : attachments[i].type, "url" : attachments[i].url});
                  $('#dlGoogleCaptions').attr('href', attachments[i].url + "/download/" + attachments[i].url.substring(attachments[i].url.lastIndexOf('/') + 1));
                  $('#rmGoogleCaptions').attr('data-provider', "googleTranscript");
                  $('#dlGoogleCaptions').attr('data-mediatype', attachments[i].type);
                  $('#downloadGoogleCaptions').show();
                  $("#removeGoogleCaptions").show();
                  $("#removeCaptionsList").show();
              }else if(attachments[i].type == "captions/vtt") {
                  providerArray.push({"id" : id, "mediatype" : attachments[i].type, "url" : attachments[i].url});
                  $('#dlNibityCaptions').attr('href', attachments[i].url + "/download/" + attachments[i].url.substring(attachments[i].url.lastIndexOf('/') + 1));
                  $('#rmNibityCaptions').attr('data-provider',"nibityTranscript");
                  $('#dlNibityCaptions').attr('data-mediatype', attachments[i].type);
                  $('#downloadNibityCaptions').show();
                  $("#removeNibityCaptions").show();
                  $("#removeCaptionsList").show();
              }else if(attachments[i].type == "text/vtt") {
                  providerArray.push({"id" : id, "mediatype" : attachments[i].type, "url" : attachments[i].url});
                  $('#dlUploadedCaptions').attr('href', attachments[i].url + "/download/" + attachments[i].url.substring(attachments[i].url.lastIndexOf('/') + 1));
                  $('#rmUploadedCaptions').attr('data-provider', "uploadedTranscript");
                  $('#dlUploadedCaptions').attr('data-mediatype', attachments[i].type);
                  $('#downloadUploadedCaptions').show();
                  $("#removeUploadedCaptions").show();
                  $("#removeCaptionsList").show();
              }
           }
        }
        for(var i=0;i<providerArray.length;i++) {
            if(providerArray[i].mediatype == "text/vtt") {
              provider = "Uploaded";
              vttURL = providerArray[i].url;
              mediaType = providerArray[i].mediatype;
            } else if(providerArray[i].mediatype == "captions/vtt" || providerArray[i].mediatype == "captions/timedtext") {
              if(providerArray[i].mediatype == "captions/vtt") {
                provider = "Way with Words";
                vttURL = providerArray[i].url;
                mediaType = providerArray[i].mediatype;
              } else {
                provider = "Automated";
                vttURL = providerArray[i].url;
                mediaType = providerArray[i].mediatype;
              }
            }
          }
      $("#editCaptions").html("<i class='fa fa-pencil' id='edCaptions'></i>Edit Captions");
      $("#editCaptions").attr('title', provider + ' Captions');
      $("#editCaptions, #hiddenEvent").attr('data-url', vttURL);
      $("#editCaptions, #hiddenEvent").attr('data-provider', provider);
      $("#editCaptions, #hiddenEvent").attr('data-mediatype', mediaType);
      $("#rqCaptions").attr('data-event',id);
    }
  )
}

function closeSeries() {
    var urlParams = new URLSearchParams(window.location.search),
        seriesID = urlParams.get('sid'),
        url = "/api/series/" + seriesID + "/metadata?type=ext/series";

    $.get({url: url, responseType: 'json'},
        function(response) {
            var seriesStatus = "";
            for(var i = 0; i<response.length;i++){
                if(response[i].id == 'series-locked') {
                    seriesStatus = response[i].value;
                }
            }
            if(seriesStatus === true) {
                $('#btnTtScheduler').hide();
                $('#btnSchedule').hide();
                $('#btnUpload').hide();
                $("#grid th:nth-child(7)").hide();
                $("#grid td:nth-child(7)").hide();
            }
        }
    )
}

function blockLongTtEvents(starttime, endtime) {
    var startTime = moment(starttime, "hh:mm"),
        endTime = moment(endtime, "hh:mm"),
        duration = moment.duration(endTime.diff(startTime)).asMilliseconds();

    if(duration > maxEventDuration) {
        return "disabled";
    }
}

var pollSession = setInterval(function() {
  $.ajax({
    url: '/lti'
  }).fail(function(res) {
    clearInterval(pollSession);
    $('#refreshModal').modal('show');
  });
}, 30000)