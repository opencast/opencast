/** Attention: underscore.js is needed to operate this script!
 */
var ocArchive = ocArchive || new (function() {

  var self = this;

  var URL_EPISODE_LIST = "../episode/episode.json";
  var URL_WORKFLOW_INSTANCES = "../workflow/instances.json";

  var SORT_FIELDS = {
    'Title': 'TITLE',
    'Presenter': 'CREATOR',
    'Series': 'SERIES_TITLE',
    'Date': 'DATE_CREATED'
  };

  var SEARCH_FILTER_FIELDS = [{
    q: "Full text"
  }];

  var _A = ocUtils.ensureArray;

  var refreshId = null;

  this.totalEpisodes = 0;
  this.currentShownEpisodes = 0;
  this.numSelectedEpisodes = 0;

  // components
  this.searchbox = null;
  this.pager = null;

  this.statistics = null;

  var refreshing = false;      // indicates if JSONP requesting recording data is in progress
  this.refreshingStats = false; // indicates if JSONP requesting statistics data is in progress
  this.statsInterval = null;

  /** Executed directly when script is loaded: parses url parameters and
   *  returns the configuration object.
   */
  var conf = new (function() {

    // default configuartion
    this.state = 'all';
    this.pageSize = 10;
    this.page = 0;
    this.refresh = 5;
    this.doRefresh = true;
    this.sortField = 'Date';
    this.sortOrder = 'ASC';
    this.filterField = null;
    this.filterText = '';

    this.lastState = 'all'
    this.lastPageSize = 10;
    this.lastPage = 0;

    // parse url parameters
    try {
      var p = document.location.href.split('?', 2)[1] || false;
      if (p !== false) {
        p = p.split('&');
        for (i in p) {
          var param = p[i].split('=');
          if (this[param[0]] !== undefined) {
            this[param[0]] = unescape(param[1]);
          }
        }
      }
    }
    catch (e) {
      alert('Unable to parse url parameters:\n' + e.toString());
    }

    return this;
  })();

  /** Initiate new JSONP call to workflow instances list endpoint
   */
  function refresh() {
    if (!refreshing) {
      refreshing = true;
      var params = [];

      // generic function to create the json url
      function mkUrl(baseUrl, startPageParam, countParam, params) {
        var p = $.merge([], params);
        p.push(countParam + "=" + conf.pageSize);
        p.push(startPageParam + "=" + conf.page);
        return baseUrl + "?" + p.join("&");
      }

      // a URL handler is an object containing the two functions
      // createUrl(params) and extractData(json).
      var urlHandler = {
        createUrl: function(params) {
          return mkUrl(URL_EPISODE_LIST, "offset", "limit", params);
        },
        extractData: function(json) {
          return {
            raw: json,
            totalCount: parseInt(json["search-results"].total),
            count: parseInt(json["search-results"].limit),
            mkRenderData: function() {
              return makeRenderDataEpisodes(json);
            }
          }
        }
      };

      // sorting if specified
      if (conf.sortField != null) {
        var sort = SORT_FIELDS[conf.sortField];
        if (conf.sortOrder == 'DESC') {
          sort += "_DESC";
        }
        params.push('sort=' + sort);
      }
      // filtering if specified
      if (conf.filterText != '') {
        params.push(conf.filterField + '=' + encodeURI(conf.filterText));
      }

      // issue the ajax request
      $.ajax({
        url: urlHandler.createUrl(params),
        dataType: 'jsonp',
        jsonp: 'jsonp',
        success: function(data) {
          // todo
          self.render(urlHandler.extractData(data));
        }
      });
    }
  }

  /** Create an object representing an episode from json.
   *  @param json -- json data from the episode service
   */
  function Episode(json) {
    this.id = json.id;
    this.title = json.dcTitle;
    this.seriesTitle = json.mediapackage.seriestitle;
    this.creators = _.pluck(_A(json.mediapackage.creators), "creator").join(", ");
    this.date = ocUtils.fromUTCDateStringToFormattedTime(json.mediapackage.start);
    // the name of the workflow that is currently being applied to this episode
    this.workflow = "";
    this.media = _(_A(json.mediapackage.media.track)).chain()
        .filter(function(track) {
          return /[^/]\/delivery$/.test(track.type);
        })
        .map(function(track) {
          return {
            url: track.url,
            mimetype: track.mimetype
          };
        })
        .value()
  }

  /** Prepare json data delivered by episode service endpoint for template rendering.
   *  @param json -- the json data as it is returned by the server
   *  @return { episodes: [Episode] }
   */
  function makeRenderDataEpisodes(json) {
    return {
      episodes: _.map(_A(json["search-results"].result), function(a) { return new Episode(a) })
    }
  }

  /** Query the workflows that are currently being active, i.e. they do have either of the states
   *  SUCCEEDED or FAILED.
   *  @return deferred ajax object
   */
  function getAllActiveWorkflows() {
    return $6.getJSON(URL_WORKFLOW_INSTANCES + "?compact=true&state=-SUCCEEDED&state=-FAILED");
  }

  /** JSONP callback for calls to the workflow instances list endpoint.
   *  @param pdata -- "parsed data", the data structure as it is created by the url handler
   */
  this.render = function(pdata) {
    // select template
    $("#controlsFoot").show();
    refreshing = false;

    this.totalEpisodes = pdata.totalCount;
    if (this.totalEpisodes >= pdata.count) {
      this.currentShownEpisodes = pdata.count;
    } else {
      this.currentShownEpisodes = this.totalEpisodes;
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
            episode.workflow = workflow.template + ":" + _A(workflow.operations.operation).pop().id;
            return true;
          } else {
            return false;
          }
        });
      });
    }

    getAllActiveWorkflows()
      .done(function(workflowJson) {
          var episodes = pdata.mkRenderData();
          addCurrentWorkflow(episodes.episodes, _A(workflowJson.workflows.workflow));
          renderTable(episodes);
      });

    // display number of matches if filtered
    if (conf.filterText) {
      if (pdata.totalCount == '0') {
        $('#filterRecordingCount').css('color', 'red');
      } else {
        $('#filterRecordingCount').css('color', 'black');
      }
      $('#filterRecordingCount').text(pdata.totalCount + ' found').show();
    } else {
      $('#filterRecordingCount').hide();
    }

    var page = conf.page + 1;
    var pageCount = Math.ceil(pdata.totalCount / conf.pageSize);
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

    // When table is ready, attach event handlers
    $('.sortable')
        .click(function() {
          var sortDesc = $(this).find('.sort-icon').hasClass('ui-icon-circle-triangle-s');
          var sortField = ($(this).attr('id')).substr(4);
          $('#ocRecordingsTable th .sort-icon')
              .removeClass('ui-icon-circle-triangle-s')
              .removeClass('ui-icon-circle-triangle-n')
              .addClass('ui-icon-triangle-2-n-s');
          if (sortDesc) {
            conf.sortField = sortField;
            conf.sortOrder = 'ASC';
            conf.page = 0;
            this.reload();
          } else {
            conf.sortField = sortField;
            conf.sortOrder = 'DESC';
            conf.page = 0;
            this.reload();
          }
        });
    // if results are sorted, display icon indicating sort order in respective table header cell
    if (conf.sortField != null) {
      var th = $('#sort' + conf.sortField);
      $(th).find('.sort-icon').removeClass('ui-icon-triangle-2-n-s');
      if (conf.sortOrder == 'ASC') {
        $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-n');
      } else if (conf.sortOrder == 'DESC') {
        $(th).find('.sort-icon').addClass('ui-icon-circle-triangle-s');
      }
    }
  };

  this.buildURLparams = function() {
    var pa = [];
    for (p in this.conf) {
      if (this.conf[p] != null) {
        pa.push(p + '=' + escape(this.conf[p]));
      }
    }
    return pa.join('&');
  }

  /** Make the page reload with the currently set configuration
   */
  this.reload = function() {
    var url = document.location.href.split('?', 2)[0];
    url += '?' + this.buildURLparams();
    document.location.href = url;
  }

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

  this.init = function() {
    $('#addHeader').jqotesubtpl('templates/episodes-header.tpl', {});

    // initialize search box
    (function(controller) {
      $('#searchBox').css('width', $('#addButtonsContainer').outerWidth(false) - 10);   // make searchbox beeing aligned with upload/schedule buttons (MH-6519)
      controller.searchbox = $("#searchBox").searchbox({
        search: function(text, field) {
          if ($.trim(text) != '') {
            conf.filterField = field;
            conf.filterText = text;
            conf.page = 0;
          }
          refresh();
        },
        clear: function() {
          conf.filterField = '';
          conf.filterText = '';
          conf.page = 0;
          refresh();
        },
        searchText: conf.filterText,
        options: SEARCH_FILTER_FIELDS,
        selectedOption: conf.filterField
      });
    })(this);

    // initialize pager
    (function(controller) {
      $('#pageSize').val(conf.pageSize);

      $('#pageSize').change(function() {
        conf.pageSize = $(this).val();
        conf.page = 0;
      });

      $('#page').val(parseInt(conf.page) + 1);

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
      controller.updateRefreshInterval(conf.doRefresh, conf.refresh);

      // Refresh Controls
      // set values according to config
      if (conf.doRefresh) {
        $('#refreshEnabled').attr('checked', 'checked');
        $('#refreshInterval').removeAttr('disabled');
        $('#refreshControlsContainer span').removeAttr('style');
      } else {
        $('#refreshEnabled').removeAttr('checked');
        $('#refreshInterval').attr('disabled', 'true');
        $('#refreshControlsContainer span').css('color', 'silver');
      }
      $('#refreshInterval').val(conf.refresh);
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

  //TEMPORARY (quick'n'dirty) PAGING
  this.nextPage = function() {
    numPages = Math.floor(this.totalEpisodes / conf.pageSize);
    if (conf.page < numPages) {
      conf.page++;
    }
    this.reload();
  }

  this.previousPage = function() {
    if (conf.page > 0) {
      conf.page--;
    }
    this.reload();
  };

  this.lastPage = function() {
    conf.page = Math.floor(this.totalEpisodes / conf.pageSize);
    this.reload();
  }

  this.gotoPage = function(page) {
    if (page > (this.totalEpisodes / conf.pageSize)) {
      this.lastPage();
    } else {
      if (page < 0) {
        page = 0;
      }
      conf.page = page;
      this.reload();
    }
  };

  this.disableRefresh = function() {
    if (refreshId) {
      window.clearInterval(refreshId);
    }
  };

  this.updateRefreshInterval = function(enable, delay) {
    delay = delay < 5 ? 5 : delay;
    conf.refresh = delay;
    ocUtils.log('Setting Refresh to ' + enable + " - " + delay + " sec");
    conf.doRefresh = enable;
    this.disableRefresh();
    if (enable) {
      refreshId = window.setInterval(refresh, delay * 1000);
    }
  };

  return this;
})();
