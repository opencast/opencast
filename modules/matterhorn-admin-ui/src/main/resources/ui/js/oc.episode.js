/** Attention: underscore.js is needed to operate this script!
 */

//
// underscore related
function __(a) { return _(ocUtils.ensureArray(a)) }

// invoke the whole list with function f
_.mixin({whole: function(xs, f) {
  return f(xs)
}});

var opencast = opencast || {};

opencast.episode = (function() {

  var A = ocUtils.ensureArray;

  /** @param track -- track object from media package json
   */
  function isDistributedTrack(track) {
    // todo do not have "delivery" and path segment "files/mediapackage" hard coded here!
    // tracks with a flavor of */delivery _not_ residing in the mediapackage are recognized as being distributed
    return /[^/]\/delivery$/.test(track.type) && !/\/files\/mediapackage\//.test(track.url);
  }

  /** Make episode object from json returned by the episode service.
   */
  function mkEpisode(json) {
    return {
      id: json.id,
      title: json.dcTitle,
      seriesTitle: json.mediapackage.seriestitle,
      creators: _.pluck(A(json.mediapackage.creators), "creator").join(", "),
      date: ocUtils.fromUTCDateStringToFormattedTime(json.mediapackage.start),
      // the name of the workflow that is currently being applied to this episode
      workflow: "",
      media: _(A(json.mediapackage.media.track)).chain()
          .filter(isDistributedTrack)
          .map(function(track) {
            return {
              url: track.url,
              mimetype: track.mimetype
            };
          })
          .value()
    }
  }

  /** Return a string to display as the workflow name.
   */
  function workflowDisplayName(workflow) {
    return ocUtils.dflt(workflow.title, workflow.template, "-UNTITLED WORKFLOW-");
  }

  ////
  // exports
  //
  return {
    /** Archive page.
     */
    Archive: new (function() {

      var URL_EPISODE_LIST = "../episode/episode.json";
      var URL_WORKFLOW_INSTANCES = "../workflow/instances.json";

      var SORT_FIELDS = {
        'Title': 'TITLE',
        'Presenter': 'CREATOR',
        'Series': 'SERIES_TITLE',
        'Date': 'DATE_CREATED'
      };

      var FILTER_FIELDS = [
        {
          q: "Any fields",
          title: "Title",
          creator: "Presenter"
        },
        {
          contributor: "Contributor",
          language: "Language",
          license: "License"
        }
      ];

      var refreshId = null;

      var totalEpisodes = 0;
      var currentShownEpisodes = 0;
      var numSelectedEpisodes = 0;

      // components
      this.searchbox = null;
      this.pager = null;

      this.statistics = null;

      var refreshing = false;      // indicates if JSONP requesting recording data is in progress
      this.refreshingStats = false; // indicates if JSONP requesting statistics data is in progress
      this.statsInterval = null;

      /** Create a state object from the current URL.
       */
      function stateFromUrl() {
        var defaults = {
          state: "all",
          pageSize: 10,
          page: 0,
          refresh: 5,
          doRefresh: true,
          sortField: "Date",
          sortOrder: "ASC",
          filterField: null,
          filterText: "",
          lastState: "all",
          lastPageSize: 10,
          lastPage: 0
        };

        // define converter functions for certain state properties
        var conv = {
          pageSize: parseInt,
          page: parseInt,
          refresh: parseInt,
          lastPageSize: parseInt,
          lastPage: parseInt,
          doRefresh: function(a) {
            return (/^true$/i).test(a);
          }
        };

        // parse url parameters
        var state = _(A(document.location.href.split('?', 2)[1])).chain()
            .map(function(a) { return a.split("&") })
            .flatten()
            .foldl(function(a, b) {
              var kv = b.split("=");
              if (kv.length == 2)
                a[kv[0]] = (conv[kv[0]] || _.identity)(kv[1]);
              return a;
            }, {})
            .value();

        // merge them into one
        return _.defaults(state, defaults);
      }

      /** Transform a state object into a URL.
       */
      function stateToUrl(state) {
        var query = _.map(state, function(v, k) { return k + "=" + escape(v) }).join("&");
        return document.location.href.split('?', 2)[0] + (query.length > 0 ? "?" + query : "");
      }

      var state = stateFromUrl();

      /** Make the page reload with the currently set configuration
       *  @param newState -- object with new state parameters, see the main state object
       */
      function reload(newState) {
        document.location.href = stateToUrl(_.defaults(newState || {}, state));
      }

      /** Initiate new JSONP call to workflow instances list endpoint
       */
      function refresh() {
        if (!refreshing) {
          refreshing = true;

          /** Extract data from the json response suitable for the render function.
           */
          function extractData(json) {
            return {
              raw: json,
              totalCount: parseInt(json["search-results"].total),
              count: parseInt(json["search-results"].limit),
              mkRenderData: function() {
                return mkRenderDataEpisodes(json);
              }
            }
          }

          // define query parameters
          var params = _([
            // sorting if specified
            (function() {
              if (state.sortField != null) {
                var sort = SORT_FIELDS[state.sortField];
                if (state.sortOrder == 'DESC') sort += "_DESC";
                return "sort=" + sort;
              }
            })(),
            // filtering if specified
            (function() {
              if (state.filterText != '') {
                return state.filterField + '=' + encodeURI(state.filterText);
              }
            })(),
            "limit=" + state.pageSize,
            "offset=" + (state.page * state.pageSize)
          ]).compact();

          // issue the ajax request
          $.ajax({
            url: URL_EPISODE_LIST + "?" + params.join("&"),
            dataType: 'jsonp',
            jsonp: 'jsonp',
            success: function(data) {
              render(extractData(data));
            }
          });
        }
      }

      /** Prepare json data delivered by episode service endpoint for template rendering.
       *  @param json -- the json data as it is returned by the server
       *  @return { episodes: [] }
       */
      function mkRenderDataEpisodes(json) {
        return {
          episodes: _.map(A(json["search-results"].result), mkEpisode)
        }
      }

      /** Query the workflows that are currently being active, i.e. they do have either of the states
       *  SUCCEEDED or FAILED.
       *  @return jqXHR
       */
      function getAllActiveWorkflows() {
        return $6.getJSON(URL_WORKFLOW_INSTANCES + "?compact=true&state=-SUCCEEDED&state=-FAILED");
      }

      /** JSONP callback for calls to the workflow instances list endpoint.
       *  @param pdata -- "parsed data"
       *    {
       *      raw: json,
       *      totalCount: <INT>
       *      count: <INT>
       *      mkRenderData: function: {episodes: [episode]}
       *    }
       */
      function render(pdata) {
        // select template
        $("#controlsFoot").show();
        refreshing = false;

        totalEpisodes = pdata.totalCount;
        if (totalEpisodes >= pdata.count) {
          currentShownEpisodes = pdata.count;
        } else {
          currentShownEpisodes = totalEpisodes;
        }

        /** @param episodes -- {episodes: [Episode]}
         */
        function renderTable(episodes) {
          $('#tableContainer').jqotesubtpl("templates/episodes-table.tpl", episodes);
        }

        /** Adds the current workflow to each episode in the list.
         *  @param episodes -- [Episode]
         *  @param workflows -- [workflow_json]
         */
        function addCurrentWorkflow(episodes, workflows) {
          _(episodes).each(function(episode) {
            _.detect(workflows, function(workflow) {
              if (workflow.mediapackage.id === episode.id) {
                var lastOperation = A(workflow.operations.operation).pop()
                episode.workflow = workflowDisplayName(workflow) + " : "
                    + ocUtils.dflt(lastOperation.description, lastOperation.id);
                return true;
              } else {
                return false;
              }
            });
          });
        }

        function attachSortHandlers() {
          // When table is ready, attach event handlers
          $(".sortable").click(function() {
            var sortDesc = $(this).find('.sort-icon').hasClass('ui-icon-circle-triangle-s');
            var sortField = ($(this).attr('id')).substr(4);
            $('#episodesTable th .sort-icon')
                .removeClass('ui-icon-circle-triangle-s')
                .removeClass('ui-icon-circle-triangle-n')
                .addClass('ui-icon-triangle-2-n-s');
            if (sortDesc) {
              state.sortField = sortField;
              state.sortOrder = 'ASC';
              state.page = 0;
              reload();
            } else {
              state.sortField = sortField;
              state.sortOrder = 'DESC';
              state.page = 0;
              reload();
            }
          });
          // if results are sorted, display icon indicating sort order in respective table header cell
          if (state.sortField != null) {
            var th = $('#sort' + state.sortField);
            $(th).find('.sort-icon').removeClass('ui-icon-triangle-2-n-s');
            if (state.sortOrder == 'ASC') {
              $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-n');
            } else if (state.sortOrder == 'DESC') {
              $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-s');
            }
          }
        }

        getAllActiveWorkflows()
            .done(function(workflowJson) {
              var episodes = pdata.mkRenderData();
              addCurrentWorkflow(episodes.episodes, A(workflowJson.workflows.workflow));
              renderTable(episodes);
              attachSortHandlers();
            });

        // display number of matches if filtered
        if (state.filterText) {
          if (pdata.totalCount == '0') {
            $('#filterRecordingCount').css('color', 'red');
          } else {
            $('#filterRecordingCount').css('color', 'black');
          }
          $('#filterRecordingCount').text(pdata.totalCount + ' found').show();
        } else {
          $('#filterRecordingCount').hide();
        }

        var page = state.page + 1;
        var pageCount = Math.ceil(pdata.totalCount / state.pageSize);
        pageCount = pageCount == 0 ? 1 : pageCount;
        $('#pageList').text(page + " of " + pageCount);
        if (page == 1) {
          $('#prevButtons').hide();
          $('#prevText').show();
        } else {
          $('#prevButtons').show();
          $('#prevText').hide();
        }
        if (page == pageCount) {
          $('#nextButtons').hide();
          $('#nextText').show();
        } else {
          $('#nextButtons').show();
          $('#nextText').hide();
        }
      }

      /** Setup the page on first load. Call refresh afterwards to initialize the content area.
       */
      this.init = function() {
        $('#addHeader').jqotesubtpl('templates/episodes-header.tpl', {});

        // initialize search box
        (function(controller) {
          $('#searchBox').css('width', $('#addButtonsContainer').outerWidth(false) - 10);   // make searchbox beeing aligned with upload/schedule buttons (MH-6519)
          controller.searchbox = $("#searchBox").searchbox({
            search: function(text, field) {
              if ($.trim(text) != '') {
                state.filterField = field;
                state.filterText = text;
                state.page = 0;
              }
              refresh();
            },
            clear: function() {
              state.filterField = '';
              state.filterText = '';
              state.page = 0;
              refresh();
            },
            searchText: state.filterText,
            options: FILTER_FIELDS,
            selectedOption: state.filterField
          });
        })(this);

        // initialize pager
        (function(controller) {
          $('#pageSize').val(state.pageSize);

          $('#pageSize').change(function() {
            state.pageSize = $(this).val();
            state.page = 0;
          });

          $('#page').val(state.page + 1);

          $('#page').blur(function() {
            controller.gotoPage($(this).val() - 1);
          });

          $('#page').keypress(function(event) {
            if (event.keyCode == '13') {
              event.preventDefault();
              controller.gotoPage($(this).val() - 1);
            }
          });
        })(this);

        // initialize refresh controls
        (function(controller) {
          controller.updateRefreshInterval(state.doRefresh, state.refresh);

          // Refresh Controls
          // set values according to config
          if (state.doRefresh) {
            $('#refreshEnabled').attr('checked', 'checked');
            $('#refreshInterval').removeAttr('disabled');
            $('#refreshControlsContainer span').removeAttr('style');
          } else {
            $('#refreshEnabled').removeAttr('checked');
            $('#refreshInterval').attr('disabled', 'true');
            $('#refreshControlsContainer span').css('color', 'silver');
          }
          $('#refreshInterval').val(state.refresh);
          // attatch event handlers
          $('#refreshEnabled').change(function() {
            if ($(this).is(':checked')) {
              $('#refreshInterval').removeAttr('disabled');
              $('#refreshControlsContainer span').removeAttr('style');
            } else {
              $('#refreshInterval').attr('disabled', 'true');
              $('#refreshControlsContainer span').css('color', 'silver');
            }
            controller.updateRefreshInterval($(this).is(':checked'), $('#refreshInterval').val());
          });
          $('#refreshInterval').change(function() {
            controller.updateRefreshInterval($('#refreshEnabled').is(':checked'), $(this).val());
          });
        })(this);

        //
        refresh();
      };

      /** Start the retract workflow for a certain media package.
       *  @param mediaPackageId -- the id of the package to retract
       */
      this.retract = function(mediaPackageId) {
        if (confirm("Start retraction of episode?")) {
          $.ajax({
            type: "POST",
            url: "../episode/applyworkflow",
            data: {
              id: mediaPackageId,
              definitionId: "retract"
            },
            complete: function(xhr, status) {
              if (xhr.status == 204) {
                // 204: NO_CONTENT -> ok, expected response
                refresh();
              } else {
                alert("Unexpected response " + xhr.status);
              }
            }
          });
        }
      };

      this.nextPage = function() {
        var numPages = Math.floor(totalEpisodes / state.pageSize);
        // make sure we have an int
        var currentPage = state.page;
        reload({ page: currentPage < numPages ? currentPage + 1 : currentPage });
      };

      this.previousPage = function() {
        var currentPage = state.page;
        reload({ page: currentPage > 0 ? currentPage - 1 : currentPage });
      };

      this.lastPage = function() {
        reload({ page: Math.floor(totalEpisodes / state.pageSize) });
      };

      this.firstPage = function() {
        reload({ page: 0 });
      };

      this.gotoPage = function(page) {
        if (page > (totalEpisodes / state.pageSize)) {
          this.lastPage();
        } else {
          if (page < 0) {
            page = 0;
          }
          reload({ page: page });
        }
      };

      this.disableRefresh = function() {
        if (refreshId) {
          window.clearInterval(refreshId);
        }
      };

      this.updateRefreshInterval = function(enable, delay) {
        delay = delay < 5 ? 5 : delay;
        state.refresh = delay;
        ocUtils.log('Setting Refresh to ' + enable + " - " + delay + " sec");
        state.doRefresh = enable;
        this.disableRefresh();
        if (enable) {
          refreshId = window.setInterval(refresh, delay * 1000);
        }
      };

      return this;
    })(),

    /** Details page.
     */
    Details: (function() {

      function init() {
        var mpId = ocUtils.getURLParam("id");
        // load header and inject it
        $("#addHeader").jqotesubtpl("templates/episode-detail-header.tpl", {});
        $6.when(queryWorkflowsOfMediaPackage(mpId), queryMediaPackage(mpId))
            .done(function(ajaxWorkflow, ajaxMpSearchResult) {
              // both args are arrays where [0] contains the response data
              injectDetailOverview($("#tableContainer"), {
                episode: mkEpisode(ajaxMpSearchResult[0]["search-results"].result),
                workflows: _.map(A(ajaxWorkflow[0].workflows.workflow), mkWorkflow)
              });
              injectMediaPackageDetails($("#mediaPackageDetails"), ajaxMpSearchResult[0]["search-results"].result.mediapackage);
            });
      }

      function injectDetailOverview($container, data) {
        $container.jqotesubtpl("templates/episode-detail-overview.tpl", data);
      }

      function injectMediaPackageDetails($container, mediapackage) {
        $container.jqotesubtpl("templates/episode-detail-mediapackage.tpl", mediapackage);
        $('.unfoldable-tr').click(function() {
          var $content = $(this).find('.unfoldable-content');
          var unfolded = $content.is(':visible');
          $('.unfoldable-content').hide('fast');
          if (!unfolded) {
            $content.show('fast');
          }
        });
      }

      /** @param id -- the media package id
       *  @return jqXHR containing raw json
       */
      function queryWorkflowsOfMediaPackage(id) {
        return $6.getJSON("../workflow/instances.json", { mp: id, startPage: 0, count: 999999, compact: false });
      }

      /** @param id -- the media package id
       *  @return jqXHR containing raw json
       */
      function queryMediaPackage(id) {
        return $6.getJSON("../episode/episode.json", { id: id })
      }

      function mkWorkflow(json) {
        var started = new Date(ocUtils.first(json.operations.operation).started);
        var completed = new Date(ocUtils.last(json.operations.operation).completed);
        return {
          id: json.id,
          title: workflowDisplayName(json),
          state: json.state,
          started: ocUtils.getDateTimeStringCompact(started),
          completed: ocUtils.getDateTimeStringCompact(completed)
        }
      }

      ////
      // exports
      //
      return {
        init: init
      };
    })(),

    /** Utility functions.
     */
    Utils: {
      /** Return a function that applies f to its argument if it is not empty, i.e. an empty array.
       */
      full: function(f) {
        return function(as) {
          if (ocUtils.ensureArray(as).length > 0)
            f(as);
          return as;
        }
      },
      isStringOrNumber: function(a) {
        return _.isString(a) || _.isNumber(a);
      }
    }
  };
})();

// temporary symbol import
// todo: remove
var ocArchive = opencast.episode.Archive;
