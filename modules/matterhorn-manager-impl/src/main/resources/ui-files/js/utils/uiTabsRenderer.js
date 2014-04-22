      var defaultPath = "/config", // the path to use when the base page directory is requested
          meJsonUrl = "/info/me.json", // path to the RuntimeInfo REST endpoint
          linkSelector = "#tabsWrapper ul:first a",
          matterhorn = " | Opencast Matterhorn",
          
          // define routing here
          
          routing = {
            "/config": {
              tab: "i18n_tab_manage_config",
              // initialization code to run
              init: function() {
                $('#content').load('/ui-files/templates/config_editor.tpl');	
                document.title = "Config Editor" + matterhorn;
              }
            },
            "/editor": {
              init: function() {
              	 $('#content').load('/ui-files/templates/workflow_editor.tpl');
                 document.title = "Workflow Editor" + matterhorn;
              },
              tab: "i18n_tab_workflow_editor"
            },
      };

      /** Run the init code for the tab corresponding to the current path.
       *  @param path -- current path
       *  @param enabledTabIds -- all enabled tabs
       *  @return the (html) id of the associated main tab
       */
      function setupTabFor(path, enabledTabIds) {
        var mapping = routing[path];
        if (mapping && ocUtils.contains(enabledTabIds, mapping.tab)) {
          mapping.init();
          return mapping.tab;
        }
      }
      
      /**
       * function load upload template.
       */
      function initUplodTemplate() {
      
      	    //$('#content').load('/template/upload.tpl');
      }

      /** 
       *  Get a list of all enabled tabs.
       *  @param $html -- tab html (jQuery object)
       *  @return deferred([id])
       */
      function getEnabledTabs($html) {
        var deferred = $6.Deferred();
        var allTabIds = $("a", $html).map(function() {return this.id}).toArray();
        $6.getJSON(meJsonUrl, function(data) {
          var enabledTabIds = _.filter(allTabIds, function(id) {
            // todo test codejquery remove 
            var enabled = data.org.properties["adminui." + id + ".enable"];
            return enabled == "true" || enabled == true || typeof enabled === "undefined";
          });
          deferred.resolve(enabledTabIds);
        });
        return deferred;
      }

      /** 
       *  Remove all tabs not in enabledTabIds from $html.
       *  @param $html -- tab html (jQuery object)
       *  @param enabledTabIds -- [id]
       *  @return html
       */
      function removeDisabledTabs($html, enabledTabIds) {
        $("a", $html).each(function() {
          if (!ocUtils.contains(enabledTabIds, $(this).attr("id"))) {
            $(this).remove();
          }
        });
        return $html;
      }

      /** 
       * DOM ready
       */
      $(document).ready(function() {
        var $tabsHtml = $($("#tabs-template").jqote());

        if ($.address.value() == '/') {
          $.address.value(defaultPath);
        }
        var path = $.address.value();

        // Address handler
        $.address
            .init(function(event) {
              getEnabledTabs($tabsHtml).done(function(enabledTabIds) {
                // inject tabs
                $("#tabsWrapper").append(removeDisabledTabs($tabsHtml, enabledTabIds));

                // Tabs setup
                var selectedTabId = setupTabFor($.address.path(), enabledTabIds);
                var selectedTabNr = $("a", "#tabs").map(
                    function(i) {
                      return this.id == selectedTabId ? i : -1
                    }).toArray().sort().pop();

                $('#tabsWrapper')
                    .tabs({
                      selected: selectedTabNr
                    })
                    .css('display', 'block');
                $('#' + selectedTabId).parent().removeClass('ui-tabs-selected');
                // Enables the plugin for all the tab links
                $(linkSelector).address();
              })
            })
            .change(function(event) {
              if ($.address.value() != path) {
                location.hash = "#" + $.address.value();
                location.reload();
              }
            })
            .externalChange(function(event) {
              if ($.address.value() != path) {
                location.hash = "#" + $.address.value();
                location.reload();
              }
            })
            .history(true);
      });
