/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
var ocViewSeries = (function(){
  var SERIES_URL2 = '/series';
  
  var PURL = "http://purl.org/dc/terms/";
  
  var anonymous_role = "ROLE_ANONYMOUS";
  
  var trans = {
    read: "View", 
    contribute: "Contribute", 
    write: "Administer",
    analyze: "Analyze"
  };

  this.initViewSeries = function() {
    $('#addHeader').jqotesubtpl('templates/viewseries.tpl', {});
    
    var id = ocUtils.getURLParam('seriesId');

    $('.oc-ui-collapsible-widget .ui-widget-header').click( function() {
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
      $(this).next().toggle();
      return false;
    });
    $('#id').text(id);
    $.ajax({
      url: "/info/me.json",
      dataType: "json",
      async: false,
      success: function(data)
      {
        anonymous_role = data.org.anonymousRole;
      }
    });
    loadSeries(id);
  };
    
  this.loadSeries = function(seriesId) {
    var i, mdList, metadata, series;
    if(seriesId !== '') {
      $.get(SERIES_URL2 + '/' + seriesId + '.json', function(data) {
        $.each(data[PURL], function(key, value) 
        {
          $('#' + key).text(value[0].value);
        });
      });
      
      $.get(SERIES_URL2 + "/" + seriesId + "/acl.json", function (data)
      {
        var roles = {};

        if (!$.isArray(data.acl.ace) && data.acl.ace.role == anonymous_role && data.acl.ace.action == "read" && data.acl.ace.allow == true) {
            roles["Public"] = ["View"];
        } else { 
          data.acl.ace = ocUtils.ensureArray(data.acl.ace);

          $.each(data.acl.ace, function (key, value) {
            
            if (value.action != "contribute" && value.role != anonymous_role && value.allow) {
              if (!$.isArray(roles[value.role])) {
                roles[value.role] = [];
              }
              roles[value.role].push(trans[value.action]);
            }
          
            if (value.role == anonymous_role && value.allow) {
              roles["Public"] = ["View"];
            }

          });
        }
        
        if (roles["Public"] == undefined) {
          roles["Public"] = ["No access"];
        }
        
        $.each(roles, function (key, value) {
          roles[key] = value.join(', ');
        });
        
        $("#privileges-list").jqotesubtpl('templates/viewseries-privileges.tpl', {
          roles: roles
        });
      });
    }
  }
  
  return this;
  
})();