// MediaPackage Editor
//=require <jquery>

(function ($) {

    var SERIES_SEARCH_URL = '/series/series.json',
        SERIES_URL = '/series',
        BASE_URL = window.location.protocol + '//' + window.location.hostname,
        // Default properties
        defProperties = {
            additionalDC: {
                enable  : true,
                required: false
            },
            // Catalogs available for the plugin
            catalogs: {
                youtube: {
                    flavor: "catalog/youtube"
                },
                itunes: {
                    flavor: "catalog/itunes"
                }
            },
            // Edition properties
            edition: {
                readOnly: false,
                metadata: true,
                tracks  : true
            },
            // Required fields
            requirement: {
                titleField: true,
                creator   : false
            },
            addCatalog: function (mp, catalog, catalogDCXML) {
                return false;
            },
            changeCatalog: function (mp, catalog, catalogDCXML) {
                return false;
            },
            deleteCatalog: function (catalog) {
                return false;
            },
            baseUrl: BASE_URL
        };

    if (window.location.port !== "") {
        BASE_URL += ":" + window.location.port;
        defProperties.baseUrl = BASE_URL;
    }

    $.fn.mediaPackageEditor = function (options, mp) {
        var self = this,
            properties = defProperties,
            originalMediaPackage = new MediaPackage(mp || {}),
            inMemoryMediaPackage = originalMediaPackage.clone(),
            finalMediaPackage;

        this.finishEditing = function() {

            // Show loading spinner
            self.find(".loading").show();
            self.parent().find("button#mpe-submit, button#mpe-cancel").attr("disabled","disabled");

            // Remove additional values
            if(inMemoryMediaPackage.episodeCatalog.disable) {
                self.find('#dublincore_episode_tab :input').not('#enable_button').each(function(i,element){
                    var key = $(element).attr('name');
                    inMemoryMediaPackage.episodeCatalog.deleteValue(key);
                });
            }

            // enqueue Series Dublin Core
            var series = self.find('#series').val();

            // Check if episode is part of a series
            if (series !== '') {

              var seriesId = self.find('#isPartOf').val();

              // If series does not exist, we create it
              if (seriesId === '') {
                seriesId = self.createSeries(series);
                self.find('#isPartOf').val(seriesId);
              }

              // Add or change series dublin core
              inMemoryMediaPackage.updateDCSeries(seriesId, series, self.getSeriesCatalog(seriesId));

              // Add or change episode dublin core
              inMemoryMediaPackage.episodeCatalog.update('isPartOf', seriesId);

              if($.isEmptyObject(originalMediaPackage.seriesCatalog.values) && !$.isEmptyObject(inMemoryMediaPackage.seriesCatalog.values)) {
                  self.addDCSeries(seriesId, series, inMemoryMediaPackage.seriesCatalog);
              } else if (!$.isEmptyObject(originalMediaPackage.seriesCatalog.values) && !originalMediaPackage.seriesCatalog.equals(inMemoryMediaPackage.seriesCatalog)) {
                  self.changeDCSeries(seriesId, series, inMemoryMediaPackage.seriesCatalog);
              }

            } else {
                // Remove series if not part of series
                inMemoryMediaPackage.deleteDCSeries();
                inMemoryMediaPackage.episodeCatalog.deleteValue('isPartOf');
            }

            // Update episode dublin core created field
            var date = self.find('#recordDate').datepicker('getDate').getTime();
            date += self.find('#startTimeHour').val() * 60 * 60 * 1000;
            date += self.find('#startTimeMin').val() * 60 * 1000;
            inMemoryMediaPackage.episodeCatalog.update('created', toISODate(new Date(date)));

            // Delete series catalog if removed
            if(inMemoryMediaPackage.seriesId === "" && originalMediaPackage.seriesId !== "") {
                self.deleteDCSeries(inMemoryMediaPackage.seriesCatalog);
            }

            // add or change
            if($.isEmptyObject(originalMediaPackage.episodeCatalog.values) && !$.isEmptyObject(inMemoryMediaPackage.episodeCatalog.values)) {
                self.addDCEpisode(inMemoryMediaPackage.episodeCatalog);
            } else if(!inMemoryMediaPackage.episodeCatalog.equals(originalMediaPackage.episodeCatalog)) {
                self.changeDCEpisode(inMemoryMediaPackage.episodeCatalog);
            }

            // Add, update or remove other dublin core catalogs
            $.each(inMemoryMediaPackage.catalogs, function(index, catalog) {

                var found = false;
                $.each(properties.catalogs, function(index, cat) {
                    if(catalog.flavor == cat.flavor)
                        found = true;
                });

                var compareCatalog = originalMediaPackage.getCatalogById(catalog.id);
                if(!found || catalog.disable) {
                    if(compareCatalog != null)
                        self.deleteCatalog(catalog);
                } else {
                    if(compareCatalog == null && !$.isEmptyObject(catalog.values)) {
                        self.addCatalog(catalog);
                    } else if(compareCatalog != null && !catalog.equals(compareCatalog)) {
                        self.changeCatalog(catalog);
                    }
                }
            });

            self.trigger('succeeded', finalMediaPackage.asString());
        }

        this.addDCEpisode = function (dublincoreEpisodeCatalog) {
            var oldId = dublincoreEpisodeCatalog.id;
            var add = $.proxy(properties.addCatalog, self);
            if(add(finalMediaPackage.asString(), dublincoreEpisodeCatalog, dublincoreEpisodeCatalog.generateCatalog())) {
                self.changeTabId(dublincoreEpisodeCatalog, oldId);
                finalMediaPackage.updateDCEpisode(dublincoreEpisodeCatalog);
            } else {
                dublincoreEpisodeCatalog.error = "Add DC Episode failed";
            }
        }

        this.changeDCEpisode = function (dublincoreEpisodeCatalog) {
            var change = $.proxy(properties.changeCatalog, self);
            if(change(finalMediaPackage.asString(), dublincoreEpisodeCatalog, dublincoreEpisodeCatalog.generateCatalog())) {
                finalMediaPackage.updateDCEpisode(dublincoreEpisodeCatalog);
            } else {
                dublincoreEpisodeCatalog.error = "Update DC Episode failed";
            }
        }

        this.addDCSeries = function (it, title, seriesCatalog) {
            var oldId = seriesCatalog.id;
            var add = $.proxy(properties.addCatalog, self);
            if(add(finalMediaPackage.asString(), seriesCatalog, seriesCatalog.generateCatalog())) {
                self.changeTabId(seriesCatalog, oldId);
                finalMediaPackage.updateSeriesCatalog(it, title, seriesCatalog);
            } else {
                seriesCatalog.error = "Add DC Series failed";
            }
        }

        this.changeDCSeries = function (it, title, seriesCatalog) {
            var change = $.proxy(properties.changeCatalog, self);
            if(change(finalMediaPackage.asString(), seriesCatalog, seriesCatalog.generateCatalog())) {
                finalMediaPackage.updateSeriesCatalog(it, title, seriesCatalog);
            } else {
                seriesCatalog.error = "Update DC Series failed";
            }
        }

        this.deleteDCSeries = function(seriesCatalog) {
            var delCatalog = $.proxy(properties.deleteCatalog, self);
            if(delCatalog(seriesCatalog)) {
                finalMediaPackage.deleteSeriesCatalog(seriesCatalog);
            } else {
                finalMediaPackage.deleteDCSeries();
                seriesCatalog.error = "Deletion DC Series failed";
            }
        }

        this.addCatalog = function (catalog) {
            var oldId = catalog.id;
            var add = $.proxy(properties.addCatalog, self);
            if(add(finalMediaPackage.asString(), catalog, catalog.generateCatalog())) {
                self.changeTabId(catalog, oldId);
                finalMediaPackage.addCatalog(catalog);
            } else {
                catalog.error = "Add failed";
            }
        }

        this.changeCatalog = function (catalog) {
            var change = $.proxy(properties.changeCatalog, self);
            if(change(finalMediaPackage.asString(), catalog, catalog.generateCatalog())) {
                finalMediaPackage.changeCatalog(catalog);
            } else {
                catalog.error = "Update failed";
            }
        }

        this.deleteCatalog = function (catalog) {
            var delCatalog = $.proxy(properties.deleteCatalog, self);
            if(delCatalog(catalog)) {
                finalMediaPackage.deleteCatalog(catalog);
            } else {
                catalog.error = "Deletion failed";
            }
        }

        this.changeTabId = function (catalog, oldId) {
            var tab = self.find('#' + catalog.flavor.replace('/', '_') + '_' + oldId + '_tab');
            var newId = catalog.flavor.replace('/', '_') + '_' + catalog.id + '_tab';
            tab.attr('id', newId);
        }

        this.getMediaPackage = function() {
            return finalMediaPackage.asString();
        }

        this.getSeriesCatalog = function (id) {
            var catalog = null;
            $.ajax({
              url : '/series/' + id + '.xml',
              type : 'get',
              async : false,
              dataType : 'xml',
              success : function(data) {
                catalog = data;
              }
            });
            return catalog;
        }

        this.validateTab = function (element) {
           var errorField = 0;

           if($(element).find('input#enable_button').attr('checked') == 'checked') return true;

           // Go through each input elements of the tabs to check them
           $(element).find(':input').not('#enable_button').each(function(index,value){
               if(!$(this).valid())
                   errorField ++;
           });

           // Remove previous messages
           $(element).find('div.errors').remove();

           // Add new errors message if needed
           if(errorField){
               var errorMsg = "There are still "+errorField+ " incorrectly filled inputs left in this catalog!";
               if(errorMsg == 1)
                   errorMsg = "There is still "+errorField+ " incorrectly filled input left in this catalog!";
               $(element).find('.collapse').after('<div class="errors">'+errorMsg+'</div>');
               return false;
           }
           return true;
        }

        this.getIdFromElement = function(element) {
            var id = $(element).attr("id");
            id = id.replace("_tab", "");
            var index = id.indexOf('_');
            if(index == -1) return '';
            index = id.indexOf('_', index + 1);
            if(index == -1) return '';
            return id.substring(index + 1, id.length);
        }

        this.enableCatalog = function(catalogElement, disable) {
           var catalog;
           if($.isEmptyObject(inMemoryMediaPackage) || (catalog = inMemoryMediaPackage.getCatalogById(self.getIdFromElement(catalogElement))) == null)
            return false;

           catalog.disable = !disable;
           catalogElement.toggleClass("disable", !disable);

           if(catalog.disable){
               catalogElement.find('.ui-icon.unfoldable-icon').removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
               catalogElement.find('.form-box-content').hide();
           }
        };

        this.createSeries = function (name) {
            var id = false;
            var seriesXml = '<dublincore xmlns="http://www.opencastproject.org/xsd/1.0/dublincore/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:oc="http://www.opencastproject.org/matterhorn"><dcterms:title xmlns="">' + name + '</dcterms:title></dublincore>'
            $.ajax({
              async: false,
              type: 'POST',
              url: SERIES_URL,
              data: {
                series: seriesXml,
                acl: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><acl xmlns="http://org.opencastproject.security"><ace><role>anonymous</role><action>read</action><allow>true</allow></ace></acl>'
              },
              dataType : 'xml',
              success: function(data){
                window.debug = data;
                id = $(data).find('[nodeName="dcterms:identifier"]').text();
              }
            });
            return id;
        }

        // Init function
        this.init = function () {
            var element = self;// MediaPackage editor element

            // Set the properties with the one given
            if(options){
                if(!$.isEmptyObject(options.catalogs))
                    properties.catalogs = options.catalogs;
                if(!$.isEmptyObject(options.additionalDC))
                    properties.additionalDC = options.additionalDC;
                if($.isFunction(options.addCatalog))
                    properties.addCatalog = options.addCatalog;
                if($.isFunction(options.changeCatalog))
                    properties.changeCatalog = options.changeCatalog;
                if($.isFunction(options.addCatalog))
                    properties.deleteCatalog = options.deleteCatalog;
                if(!$.isEmptyObject(options.baseUrl))
                    properties.baseUrl = options.baseUrl;

                $.extend(properties.edition, options.edition || {});
                $.extend(properties.requirement, options.requirement || {});
            }

            // Collecting flavor data
            var tabs = new Array();
            $.each(properties.catalogs, function(key, catalog) {
                var catalogs = inMemoryMediaPackage.getCatalogsByFlavor(catalog.flavor);
                if(catalogs == null) {
                    var newCatalog = new Catalog();
                    newCatalog.flavor = catalog.flavor;
                    newCatalog.disable = true;
                    inMemoryMediaPackage.catalogs.push(newCatalog);
                    catalogs = new Array();
                    catalogs.push(newCatalog);
                }
                $.each(catalogs, function(i, cat) {
                    tabs.push(cat.flavor.replace('/', '_') + '_' + cat.id);
                });
            });

            // Insert base template
            $.ajax({url: properties.baseUrl+'/mediapackage-editor/templates/editor.tmpl', dataType: 'text', async: false})
                .success(function(template) {
                self.html($.tmpl(template, {
                    catalog: inMemoryMediaPackage.episodeCatalog,
                    flavors: tabs,
                    seriesId: inMemoryMediaPackage.seriesId,
                    seriesTitle: inMemoryMediaPackage.seriesTitle
                }));
            });

            // Show additional dc catalog
            if(properties.additionalDC.enable) {
                var catalog = inMemoryMediaPackage.episodeCatalog;
                var tab = element.find('#dublincore_episode_tab');
                $.ajax({url: properties.baseUrl+'/mediapackage-editor/templates/dublincore.tmpl', dataType: 'text', async: false})
                .success(function(template) {
                    $.tmpl(template, {catalog: catalog}).appendTo(tab);
                });
                tab.show();
                tab.find(".form-box-head input[type='checkbox']").click(function(event) {
                    catalog.disable = !catalog.disable;
                    tab.toggleClass("disable", catalog.disable);

                    if(catalog.disable){
                        tab.find('.ui-icon.unfoldable-icon').removeClass('ui-icon-triangle-1-s').addClass('ui-icon-triangle-1-e');
                        tab.find('.form-box-content').hide();
                    }
                })

                if(properties.additionalDC.required) {
                    tab.find(".form-box-head input[type='checkbox'],.form-box-head label").remove();
                }
            }

            // Show the tabs set visible
            $.each(properties.catalogs, function(key, value) {
                var catalogs = inMemoryMediaPackage.getCatalogsByFlavor(value.flavor);

                $.each(catalogs, function(i, cat) {
                    var flavor = value.flavor.replace('/', '_'),
                        tab = element.find('#' + flavor + '_' + cat.id + '_tab');

                    $.ajax({url: properties.baseUrl+'/mediapackage-editor/templates/'+key+'.tmpl', dataType: 'text', async: false})
                    .success(function(template) {
                        $.tmpl(template, {catalog: cat}).appendTo(tab);
                    });
                    tab.show();
                    tab.find(".form-box-head input[type='checkbox']").click(function(event){
                        self.enableCatalog(tab, cat.disable);
                    })

                    // Init enable checkbox
                    if (cat.disable) {
                        self.enableCatalog(tab, !cat.disable);
                        tab.find(".form-box-head input[type='checkbox']").attr("checked","checked");
                    }

                    if (value.required) {
                        tab.find(".form-box-head input[type='checkbox'],.form-box-head label").remove();
                    }
                });
            });

            self.find('.oc-ui-collapsible-widget :input').not('#enable_button').bind('keyup change', function(){
                var tab = $(this).parents('div.oc-ui-collapsible-widget');
                var catalog;
                if(tab.attr('id') == 'dublincore_episode_tab') {
                    catalog = inMemoryMediaPackage.episodeCatalog;
                } else {
                    var id = self.getIdFromElement(tab);
                    if(id == "" || (catalog = inMemoryMediaPackage.getCatalogById(id)) == null)
                        return;
                }
                catalog.update($(this).attr("name"),$(this).val());
            });

            self.find('.dc-metadata-field').bind('keyup change', function() {
                inMemoryMediaPackage.episodeCatalog.update($(this).attr("name"), $(this).val());
            });

            // No mediaPackage given
            if(properties.edition.readOnly) {
                element.find('h2 span').html("MediaPackage");
            } else if($.isEmptyObject(mp)) {
                element.find('h2 span').html("Upload MediaPackage");
            }

            // Bulk Actions
            element.find('div.collapse, div.ui-icon').click(
              function() {
                if($(this).parents('div.oc-ui-collapsible-widget.disable').length!=0)
                    return false;

                $(this).parent().children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
                $(this).parent().children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
                $(this).parent().next().toggle();

                return false;
             });

            // Disabled field if readOnly
            if(properties.edition.readOnly){
                element.find(':input').attr('disabled','true');
            }

            // Define required fields
            $.each(properties.requirement, function (key, value) {
                  if(value){
                      var field = element.find('#'+key);
                      field.addClass('required');
                      if(field.prev().find('.redStar').length == 0)
                          field.prev().prepend('<span class="redStar">* </span>');
                  }
            });

            // Add date picker
            self.find('#recordDate').datepicker({
                showOn: 'both',
                buttonImage: '/mediapackage-editor/style/images/calendar.gif',
                buttonImageOnly: true,
                dateFormat: 'yy-mm-dd'
            });

            // Fill start date time
            var date;
            if(!$.isEmptyObject(inMemoryMediaPackage.episodeCatalog.getValue('created'))) {
                date = fromUTCDateString(inMemoryMediaPackage.episodeCatalog.getValue('created'));
            } else {
                date = new Date();
            }
            var hours = date.getHours();
            var minutes = date.getMinutes();
            self.find('#recordDate').datepicker('setDate', date);
            self.find('#startTimeHour').val(hours);
            self.find('#startTimeMin').val(minutes);
            self.find('#titleField').boundinput('input#title');
            self.find('div#dublincore_episode_tab #description').boundinput('.description');
            self.find('div#dublincore_episode_tab #license').boundinput('.license');
            self.find('div#dublincore_episode_tab #copyright').boundinput('.copyright');
            self.find('div#dublincore_episode_tab #language').boundinput('.language');


        }

        this.submit = function() {
            var editForm = self.find('form#editForm'), // Edition form element
                formValid = editForm.valid();

            // Form validation
            self.find('.oc-ui-collapsible-widget').each(function () {
                if (!self.validateTab(this)) {
                    formValid = false;
                }
            });

            if (!formValid) {
                return;
            }

            finalMediaPackage = originalMediaPackage.clone();
            self.finishEditing();
        }

        this.initSeriesAutocomplete = function () {
            self.find('#series').autocomplete({
              source: function(request, response) {
                $.ajax({
                  url: SERIES_SEARCH_URL + '?q=' + request.term,
                  dataType: 'json',
                  type: 'GET',
                  success: function(data) {
                    data = data.catalogs;
                    var series_list = [];
                    $.each(data, function(){
                      series_list.push({value: this['http://purl.org/dc/terms/']['title'][0].value,
                                        id: this['http://purl.org/dc/terms/']['identifier'][0].value});
                    });
                    series_list.sort(function stringComparison(a, b)    {
                      a = a.value;
                      a = a.toLowerCase();
                      a = a.replace(/ä/g,"a");
                      a = a.replace(/ö/g,"o");
                      a = a.replace(/ü/g,"u");
                      a = a.replace(/ß/g,"s");

                      b = b.value;
                      b = b.toLowerCase();
                      b = b.replace(/ä/g,"a");
                      b = b.replace(/ö/g,"o");
                      b = b.replace(/ü/g,"u");
                      b = b.replace(/ß/g,"s");

                      return(a==b)?0:(a>b)?1:-1;
                    });
                    response(series_list);
                  }, 
                  error: function() {
                    console.log('could not retrieve series_data');
                  }
                });
              },
              select: function(event, ui){
                self.find('#isPartOf').val(ui.item.id);
              },
              search: function(){
                self.find('#isPartOf').val('');
              }
            });
        }

        this.init();
        this.initSeriesAutocomplete();

        return this;
    };

    function padString (str, pad, padlen) {
        if (typeof str !== 'string') {
            str = str.toString();
        }
        while(str.length < padlen && pad.length > 0){
            str = pad + str;
        }
        return str;
    }

    function fromUTCDateString (UTCDate) {
      var date = new Date(0);
      if(UTCDate[UTCDate.length - 1] == 'Z') {
        var dateTime = UTCDate.slice(0,-1).split("T");
        var ymd = dateTime[0].split("-");
        var hms = dateTime[1].split(":");
        date.setUTCMilliseconds(0);
        date.setUTCSeconds(parseInt(hms[2], 10));
        date.setUTCMinutes(parseInt(hms[1], 10));
        date.setUTCHours(parseInt(hms[0], 10));
        date.setUTCDate(parseInt(ymd[2], 10));
        date.setUTCMonth(parseInt(ymd[1], 10) - 1);
        date.setUTCFullYear(parseInt(ymd[0], 10));
      }
      return date;
    }

    /**
     * Convert Date object to yyyy-MM-dd'T'HH:mm:ss'Z' string.
     */
    function toISODate (date, utc) {
        //align date format
        var date = new Date(date),
            out;

        if (typeof utc === "undefined") {
            utc = true;
        }
        if(utc) {
            out = date.getUTCFullYear() + '-' +
            padString((date.getUTCMonth()+1) ,'0' , 2) + '-' +
            padString(date.getUTCDate() ,'0' , 2) + 'T' +
            padString(date.getUTCHours() ,'0' , 2) + ':' +
            padString(date.getUTCMinutes() ,'0' , 2) + ':' +
            padString(date.getUTCSeconds() ,'0' , 2) + 'Z';
        } else {
            out = date.getFullYear() + '-' +
            padString((date.getMonth()+1) ,'0' , 2) + '-' +
            padString(date.getDate() ,'0' , 2) + 'T' +
            padString(date.getHours() ,'0' , 2) + ':' +
            padString(date.getMinutes() ,'0' , 2) + ':' +
            padString(date.getSeconds() ,'0' , 2);
        }
        return out;
    }
})(jQuery);