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

  // -- Service calls

  /** Query the workflows that are currently being active, i.e. they do have either of the states
   *  SUCCEEDED or FAILED.
   *  @return jqXHR
   */
  function getAllActiveWorkflows() {
    return $.getJSON("../workflow/instances.json?compact=true&state=-SUCCEEDED&state=-FAILED");
  }

  /** @param id -- the media package id
   *  @return jqXHR containing raw json
   */
  function getWorkflowsOfMediaPackage(id) {
    return $.getJSON("../workflow/instances.json", { mp:id, startPage:0, count:999999, compact:false });
  }

  /** @param id -- the media package id
   *  @return jqXHR containing raw json
   */
  function getMediaPackage(id) {
    return $.getJSON("../episode/episode.json", { id:id });
  }

  /** @param id -- the media package id
   *  @return jqXHR containing raw xml
   */
  function getMediaPackageXml(id) {
    return $.get("../episode/episode.xml", { id:id });
  }

  /** @param params -- parameter object for the episode rest endpoint
   *  @return jqXHR containing raw json
   */
  function getEpisodes(params) {
    return $.getJSON("../episode/episode.json", params);
  }

  /** @return jqXHR containing raw json
   */
  function getAvailableWorkflowDefinitions() {
    return $.getJSON("../workflow/definitions.json");
  }

  /** Open the metadata editor with mediapackage xml.
   *  @param mediapackageXml -- xml representation of the mediapackage
   */
  function openMetadataEditor(mediapackageXml) {
    var mpe = $("#mpe-editor").mediaPackageEditor({
              additionalDC: {
                enable: true,
                required: false
              },
              // Catalogs available for the plugin
              catalogs: {
                youtube: {
                  flavor: "catalog/youtube"
                }
              },
              requirement: {
                title: true,
                creator: true
              },
              
              addCatalog: function(mp, catalog, catalogDCXML) {
            	   var doc = $.parseXML(mp);
	          	   var mpId = $(doc).find('mp\\:mediapackage').attr("id");
            	   var uuid = generateUUID();
            	  
            	   var response = false;
	          		$.ajax({
	          			async: false,
	          			url: "../files/mediapackage/"+mpId+"/"+uuid+"/dublincore.xml",
	          			data: {
	          				content : catalogDCXML
	          			},
	          			type: 'post',
	          			success: function(url){
	          				catalog.url = url;
	          				catalog.id = uuid.toString();
	          				response = true;
	          			}
	          			
	          		});	
	          		
	          		return response;

	          },
	          changeCatalog: function(mp, catalog, catalogDCXML) {
	        	    var doc = $.parseXML(mp);
	          		var mpId = $(doc).find('mp\\:mediapackage').attr("id");
         	  
	          		var response = false;
	         		$.ajax({
	         			async: false,
	         			url: "../files/mediapackage/"+mpId+"/"+catalog.id+"/dublincore.xml",
	         			data: {
	         				content : catalogDCXML
	         			},
	         			type: 'post',
	         			success: function(url){
	         				catalog.url = url;
	         				response = true;
	         			}
	         			
	         		});	
	         		
	         		return response;
	          },
	          deleteCatalog: function(catalog) {
	          		var mp = this.getMediaPackage();
	          		var doc = $.parseXML(mp);
	          		var mpId = $(doc).find('mp\\:mediapackage').attr("id");
	          		
	          		var response = false;
	          		$.ajax({
	          			async: false,
	          			url: "../files/mediapackage/"+mpId+"/"+catalog.id,
	          			type: 'delete',
	          			success: function(){
	          				response = true;
	          			}
	          			
	          		});	 
	          		
	          		return response;
	          }
            },
            mediapackageXml);

    var $window = openWindow($("#mpe-window"), {
      close: function() {
        $("#mpe-editor, #mpe-cancel, #mpe-submit").unbind();
      }
    });
    $("#mpe-submit").click(function () {
      $("div#mpe-errors").empty();
      mpe.submit();
    });
    $("#mpe-cancel").click(function () {
      $window.dialog("close");
    });
    
	$("#mpe-editor").bind('succeeded', function(ev, mp) {
 		$.ajax({
 			async: false,
 			url: "../episode/add",
 			dataType: "text",
 			data: {
 				mediapackage : mp
 			},
 			type: 'post',
 			success: function(){
 			  $("#mpe-editor").unbind();
 			  $window.dialog("close");
 			},
 			error: function(request, errorText, thrownError){
 			  $("div#mpe-errors").append("Error during update: "+thrownError.message+" "+errorText+"("+thrownError.status + thrownError.statusText+")");
 			}
 		});	
	});
  }
  
  /**
   * @return generated UUID for catalog
   */
  function generateUUID() {
      var uuid = (function () {
          var i,
              c = "89ab",
              u = [];
          for (i = 0; i < 36; i += 1) {
              u[i] = (Math.random() * 16 | 0).toString(16);
          }
          u[8] = u[13] = u[18] = u[23] = "-";
          u[14] = "4";
          u[19] = c.charAt(Math.random() * 4 | 0);
          return u.join("");
      })();
      return {
          toString: function () {
              return uuid;
          },
          valueOf: function () {
              return uuid;
          }
      };
  }

  /** Open a window using the jquery.dialog plugin.
   *  @param opt -- [optional] options, as passed to .dialog("option")
   *  @return $win
   */
  function openWindow($win, opt) {
    $win.dialog("option", _.extend({
      width: $(window).width() - 40,
      height: $(window).height() - 40,
      position: ["center", "center"],
      show: "scale"
    }, opt || {})).dialog("open");
    return $win;
  }

  // --

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
      date: json.mediapackage.start ? ocUtils.fromUTCDateStringToFormattedTime(json.mediapackage.start) : "?",
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
    Archive: (function() {

      var SORT_FIELDS = {
        "Title": "TITLE",
        "Presenter": "CREATOR",
        "Series": "SERIES_TITLE",
        "Date": "DATE_CREATED"
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

      var defaultState = {
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

      var state = stateFromUrl();

      var refreshId = null;

      var totalEpisodes = 0;
      var currentShownEpisodes = 0;
      var selectedEpisodes = {};

      var refreshing = false;      // indicates if JSONP requesting recording data is in progress

      /** Setup the page on first load. Call refresh afterwards to initialize the content area.
       */
      function init() {
        $("#addHeader").jqotesubtpl("templates/episodes-header.tpl", {});

        // each initializer function may return an array of parameterless functions that shall be applied
        // after refreshing has finished
        var applyAfterRefresh = [
          // log
          function () {
            console.log("done")
          },
          // initialize search box
          (function () {
            $("#searchBox")
                    .css("width", $("#addButtonsContainer")
                    .outerWidth(false) - 10)// make searchbox being aligned with upload/schedule buttons (MH-6519)
                    .searchbox({
                      search: function (text, field) {
                        if ($.trim(text) != "") {
                          state.filterField = field;
                          state.filterText = text;
                          state.page = 0;
                        }
                        refresh();
                      },
                      clear: function () {
                        state.filterField = "";
                        state.filterText = "";
                        state.page = 0;
                        refresh();
                      },
                      searchText: state.filterText,
                      options: FILTER_FIELDS,
                      selectedOption: state.filterField
                    });
          })(),
          // initialize pager
          (function () {
            $("#pageSize").val(state.pageSize);
            $("#pageSize").change(function () {
              state.pageSize = $(this).val();
              state.page = 0;
            });
            $("#page").val(state.page + 1);
            $("#page").blur(function () {
              gotoPage($(this).val() - 1);
            });
            $("#page").keypress(function (event) {
              if (event.keyCode == "13") {
                event.preventDefault();
                gotoPage($(this).val() - 1);
              }
            });
          })(),
          // initialize refresh controls
          (function () {
            updateRefreshInterval(state.doRefresh, state.refresh);

            // Refresh Controls
            // set values according to config
            if (state.doRefresh) {
              $("#refreshEnabled").attr("checked", "checked");
              $("#refreshInterval").removeAttr("disabled");
              $("#refreshControlsContainer span").removeAttr("style");
            } else {
              $("#refreshEnabled").removeAttr("checked");
              $("#refreshInterval").attr("disabled", "true");
              $("#refreshControlsContainer span").css("color", "silver");
            }
            $("#refreshInterval").val(state.refresh);
            // attatch event handlers
            $("#refreshEnabled").change(function () {
              if ($(this).is(":checked")) {
                $("#refreshInterval").removeAttr("disabled");
                $("#refreshControlsContainer span").removeAttr("style");
              } else {
                $("#refreshInterval").attr("disabled", "true");
                $("#refreshControlsContainer span").css("color", "silver");
              }
              updateRefreshInterval($(this).is(":checked"), $("#refreshInterval").val());
            });
            $("#refreshInterval").change(function () {
              updateRefreshInterval($("#refreshEnabled").is(":checked"), $(this).val());
            });
          })(),
          // initialize episode selection checkboxes
          (function () {
            $("body")
                    .delegate(".selectEpisode", "change", function () {
                      check(this, $(this).is(":checked"));
                      showSelectedEpisodesCount();
                    })
                    .delegate("#selectAllEpisodes", "change", function () {
                      var checked = $(this).is(":checked");
                      $(".selectEpisode").each(function () {
                        check(this, checked);
                      });
                      showSelectedEpisodesCount();
                    });

            showSelectedEpisodesCount();
            return [restoreCheckboxes];
            // -- where
            function check(box, checked) {
              $(box).attr("checked", checked);
              var eid = $(box).val();
              selectedEpisodes[eid] = checked;
              checkSelectAllEpisodes();
            }

            function allChecked() {
              var $e = $(".selectEpisode");
              return $e.size() == $e.filter(":checked").size();
            }

            function restoreCheckboxes() {
              $(".selectEpisode").each(function () {
                var eid = $(this).val();
                $(this).attr("checked", selectedEpisodes[eid] == true);
              });
              checkSelectAllEpisodes();
            }

            function checkSelectAllEpisodes() {
              $("#selectAllEpisodes").attr("checked", allChecked());
            }
          })(),
          // pager
          (function () {
            var $pageSize = $("#pageSize");
            $pageSize.val(state.pageSize);
            $pageSize.change(function () {
              state.pageSize = $(this).val();
              state.page = 0;
              refresh();
            });
          })(),
          // apply workflow widget
          (function() {
            var $window = $("#awf-window").dialog({autoOpen: false});
            var $selectWorkflow = $("#awf-select");
            var $configContainer = $("#awf-config-container");
            ocWorkflow.init($selectWorkflow, $configContainer);
            $("#awf-start").click(function () {
              openWindow($window, {height: 250});
            });
            $("#awf-submit").click(function() {
              applyWorkflowToSelectedEpisodes($selectWorkflow.val(), ocWorkflow.getConfiguration($configContainer));
              showSelectedEpisodesCount();
              $window.dialog("close");
            });
            $("#awf-cancel").click(function() { $window.dialog("close"); });
          })(),
          // attach edit handler
          (function() {
            $("#mpe-window").dialog({autoOpen: false});
            $("#tableContainer").delegate(".edit", "click", function() {
              var eid = $(this).attr("data-eid");
              getMediaPackageXml(eid).done(function(xml) {
                $(xml).find("mediapackage").each(function(_, mp) {
                  openMetadataEditor(mp);
                });
              });
              return false;
            })
          }())
        ];
        // flatten and filter out undefined values, refresh and apply functions afterwards
        var f = _(applyAfterRefresh).chain().flatten().compact().value();
        _theRefreshFunc = function () {
          return _refresh().done(f);
        };
        refresh();
      }

      function showSelectedEpisodesCount() {
        var c = _(selectedEpisodes).chain().keys().filter(
                function (a) {
                  return selectedEpisodes[a]
                }).value().length;
        $("#selectedEpisodesCount").html(c + " episode(s) selected");
      }

      /** Create a state object from the current URL.
       */
      function stateFromUrl() {
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
        var state = _(A(document.location.href.split("?", 2)[1])).chain()
            .map(function(a) { return a.split("&") })
            .flatten()
            .foldl(function(a, b) {
              var kv = b.split("=");
              if (kv.length == 2)
                a[kv[0]] = (conv[kv[0]] || _.identity)(kv[1]);
              return a;
            }, {})
            .value();

        // merge them with defaults
        return _.defaults(state, defaultState);
      }

      /** Transform a state object into a URL.
       */
      function stateToUrl(state) {
        var query = _.map(state, function(v, k) { return k + "=" + escape(v) }).join("&");
        return document.location.href.split("?", 2)[0] + (query.length > 0 ? "?" + query : "");
      }

      /** Make the page reload with the currently set configuration
       *  @param newState -- object with new state parameters, see the main state object
       */
      function reload(newState) {
        state = _.defaults(newState || {}, state);
        refresh();
      }

      // this var holds the actual refresh function and gets initialized lazily
      var _theRefreshFunc = function () {return $.Deferred()};

      function refresh() {
        return _theRefreshFunc();
      }

      /** Initiate new ajax call to workflow instances list endpoint
       *  @return deferred object
       */
      function _refresh() {
        if (!refreshing) {
          refreshing = true;
          // # LET
          /** Extract data from the json response suitable for the render function.
           */
          function extractData(json) {
            return {
              raw: json,
              totalCount: parseInt(json["search-results"].total),
              count: parseInt(json["search-results"].limit),
              mkRenderData: function () {
                return mkRenderDataEpisodes(json);
              }
            }
          }
          // # IN
          // define query parameters
          var params = _.extend(
                  {
                    limit: state.pageSize,
                    offset: (state.page * state.pageSize)
                  },
                  // sorting if specified
                  (function () {
                    if (state.sortField != null) {
                      var sort = SORT_FIELDS[state.sortField];
                      if (state.sortOrder == "DESC") sort += "_DESC";
                      return {sort: sort};
                    }
                  })(),
                  // filtering if specified
                  (function () {
                    if (state.filterText != "") {
                      var a = {};
                      a[state.filterField] = encodeURI(state.filterText);
                      return a;
                    }
                  })());
          // issue the ajax request
          return $.Deferred(function (d) {
            getEpisodes(params).success(function (data) {
              console.log("success");
              // ensure rendering has finished before any further functions apply
              render(extractData(data))
                      .done(function () {d.resolve()})
                      .fail(function () {d.fail()});
            })
          });
        } else {
          // return an empty deferred
          return $.Deferred();
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

      /** Callback for calls to the workflow instances list endpoint.
       *  @param pdata -- "parsed data"
       *    {
       *      raw: json,
       *      totalCount: <INT>
       *      count: <INT>
       *      mkRenderData: function: {episodes: [episode]}
       *    }
       *  @return deferred render object
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

        // display number of matches if filtered
        if (state.filterText) {
          if (pdata.totalCount == "0") {
            $("#filterRecordingCount").css("color", "red");
          } else {
            $("#filterRecordingCount").css("color", "black");
          }
          $("#filterRecordingCount").text(pdata.totalCount + " found").show();
        } else {
          $("#filterRecordingCount").hide();
        }

        var page = state.page + 1;
        var pageCount = Math.ceil(pdata.totalCount / state.pageSize);
        pageCount = pageCount == 0 ? 1 : pageCount;
        $("#pageList").text(page + " of " + pageCount);
        if (page == 1) {
          $("#prevButtons").hide();
          $("#prevText").show();
        } else {
          $("#prevButtons").show();
          $("#prevText").hide();
        }
        if (page == pageCount) {
          $("#nextButtons").hide();
          $("#nextText").show();
        } else {
          $("#nextButtons").show();
          $("#nextText").hide();
        }

        return getAllActiveWorkflows()
                .done(function (workflowJson) {
                  var episodes = pdata.mkRenderData();
                  addCurrentWorkflow(episodes.episodes, A(workflowJson.workflows.workflow));
                  renderTable(episodes);
                  attachSortHandlers();
                  console.log("done workflows");
                });

        // -- where

        /** @param episodes -- {episodes: [Episode]}
         */
        function renderTable(episodes) {
          var $table = $("#tableContainer");
          $table.jqotesubtpl("templates/episodes-table.tpl", episodes);
          // do some postprocessing
          // disable episodes currently being processed, highlight them
          $table.find("tr:has(span.active-workflow)")
                  .find(".selectEpisode").each(function() {this.disabled = true})
                  .end()
                  .addClass("highlight");
        }

        /** If an episode is currently being processed by a workflow, add the workflow information
         *  to the episode.
         *  @param episodes -- [Episode]
         *  @param workflows -- [workflow_json]
         */
        function addCurrentWorkflow(episodes, workflows) {
          _(episodes).each(function(episode) {
            _.detect(workflows, function(workflow) {
              if (workflow.mediapackage.id === episode.id) {
                var lastOperation = A(workflow.operations.operation).pop();
                if (lastOperation) {
                  episode.workflow = workflowDisplayName(workflow) + " : "
                      + ocUtils.dflt(lastOperation.description, lastOperation.id);
                }
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
            var sortDesc = $(this).find(".sort-icon").hasClass("ui-icon-circle-triangle-s");
            var sortField = ($(this).attr("id")).substr(4);
            $("#episodesTable th .sort-icon")
                .removeClass("ui-icon-circle-triangle-s")
                .removeClass("ui-icon-circle-triangle-n")
                .addClass("ui-icon-triangle-2-n-s");
            if (sortDesc) {
              state.sortField = sortField;
              state.sortOrder = "ASC";
              state.page = 0;
              reload();
            } else {
              state.sortField = sortField;
              state.sortOrder = "DESC";
              state.page = 0;
              reload();
            }
          });
          // if results are sorted, display icon indicating sort order in respective table header cell
          if (state.sortField != null) {
            var th = $("#sort" + state.sortField);
            $(th).find(".sort-icon").removeClass("ui-icon-triangle-2-n-s");
            if (state.sortOrder == "ASC") {
              $(th).find(".sort-icon").addClass("ui-icon-circle-triangle-n");
            } else if (state.sortOrder == "DESC") {
              $(th).find(".sort-icon").addClass("ui-icon-circle-triangle-s");
            }
          }
        }
      }

      /** Start the retract workflow for a certain media package.
       *  @param mediaPackageId -- the id of the package to retract
       */
      function retract(mediaPackageId) {
        if (confirm("Start retraction of episode?")) {
          $.ajax({
            type: "POST",
            url: "../episode/applyworkflow",
            data: {
              id: mediaPackageId,
              definitionId: "retract"
            },
            complete: function(xhr) {
              if (xhr.status == 204) {
                // 204: NO_CONTENT -> ok, expected response
                refresh();
              } else {
                alert("Unexpected response " + xhr.status);
              }
            }
          });
        }
      }

      /** @param workflowDefinitionId -- id of the workflow to apply
       *  @param workflowParams -- workflow parameter object
       *  @param mediaPackageId -- String: Id of a single media package | Array: List of ids.
       */
      function applyWorkflow(workflowDefinitionId, workflowParams, mediaPackageId) {
        var mids = _.isArray(mediaPackageId) ? mediaPackageId : [mediaPackageId];
        $.ajax({
          type: "POST",
          url: "../episode/apply/" + workflowDefinitionId,
          data: _.extend({}, workflowParams, {mediaPackageIds: mids}),
          // IMPORTANT! Must be true otherwise the id array gets serialized like this "id%5B%5D=1&id%5B%5D=2"
          // which the server does not understand
          traditional: true,
          complete: function(xhr) {
            if (xhr.status == 204) {
              // 204: NO_CONTENT -> ok, expected response
              refresh();
            } else {
              alert("Unexpected response " + xhr.status);
            }
          }
        });
      }

      function applyWorkflowToSelectedEpisodes(workflowDefinitionId, workflowParams) {
        var selected = _(selectedEpisodes).chain().keys().filter(function (a) {return selectedEpisodes[a]}).value();
        if (selected.length > 0) {
          selectedEpisodes = {};
          applyWorkflow(workflowDefinitionId, workflowParams, selected);
        }
      }

      function nextPage() {
        var numPages = Math.floor(totalEpisodes / state.pageSize);
        // make sure we have an int
        var currentPage = state.page;
        reload({ page: currentPage < numPages ? currentPage + 1 : currentPage });
      }

      function previousPage() {
        var currentPage = state.page;
        reload({ page: currentPage > 0 ? currentPage - 1 : currentPage });
      }

      function lastPage() {
        reload({ page: Math.floor(totalEpisodes / state.pageSize) });
      }

      function firstPage() {
        reload({ page: 0 });
      }

      function gotoPage(page) {
        if (page > (totalEpisodes / state.pageSize)) {
          lastPage();
        } else {
          if (page < 0) {
            page = 0;
          }
          reload({ page: page });
        }
      }

      function disableRefresh() {
        if (refreshId) {
          window.clearInterval(refreshId);
        }
      }

      function updateRefreshInterval(enable, delay) {
        delay = delay < 5 ? 5 : delay;
        state.refresh = delay;
        ocUtils.log("Setting Refresh to " + enable + " - " + delay + " sec");
        state.doRefresh = enable;
        disableRefresh();
        if (enable) {
          refreshId = window.setInterval(refresh, delay * 1000);
        }
      }
      ////
      // exports
      //
      return {
        init: init,
        retract: retract,
        nextPage: nextPage,
        previousPage: previousPage,
        lastPage: lastPage,
        firstPage: firstPage,
        gotoPage: gotoPage
      };
    })(),

    /** Details page.
     */
    Details: (function() {

      function init() {
        var mpId = ocUtils.getURLParam("id");
        // load header and inject it
        $("#addHeader").jqotesubtpl("templates/episode-detail-header.tpl", {});
        $.when(getWorkflowsOfMediaPackage(mpId),
                getMediaPackage(mpId))
          .done(function (ajaxWorkflow, ajaxMpSearchResult) {
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
        $(".unfoldable-tr").click(function() {
          var $content = $(this).find(".unfoldable-content");
          var unfolded = $content.is(":visible");
          $(".unfoldable-content").hide("fast");
          if (!unfolded) {
            $content.show("fast");
          }
        });
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
