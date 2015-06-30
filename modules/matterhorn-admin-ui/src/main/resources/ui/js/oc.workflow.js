/**
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
var ocWorkflow = ocWorkflow || {};

ocWorkflow.init = function(selectElm, configContainer, tags) {
  ocWorkflow.container = configContainer;
  ocWorkflow.selector = selectElm;
  $(ocWorkflow.selector).change( function() {
    ocWorkflow.definitionSelected($(this).val(), configContainer);
  });
  ocWorkflow.loadDefinitions(selectElm, configContainer, tags);
}

ocWorkflow.loadDefinitions = function(selector, container, tags) {
  $.ajax({
    async: false,
    method: 'GET',
    url: '/workflow/definitions.json',
    dataType: 'json',
    success: function(data) {
      var wfDefinitions = ocUtils.ensureArray(data.definitions.definition);
      outerloop:
      for (i in wfDefinitions) {
        if (wfDefinitions[i].id == 'error')
        	continue;
        
        if(tags != undefined && $.isArray(tags)) {
        	var definitionTags = ocUtils.ensureArray(wfDefinitions[i].tags.tag);
        	for(y in tags) {
        		var include = _.contains(definitionTags, tags[y]);
        		if(!include) continue outerloop;
        	}
        }
        
        var option = document.createElement("option");
        option.setAttribute("value", wfDefinitions[i].id);
        option.innerHTML = wfDefinitions[i].title || wfDefinitions[i].id;
        if (wfDefinitions[i].id == "full") {
        	option.setAttribute("selected", "true");
        }
        $(selector).append(option);
      }
      ocWorkflow.definitionSelected($(selector).val(), container);
    }
  });
}

ocWorkflow.definitionSelected = function(defId, container, callback) {
  if(typeof ocWorkflowPanel != 'undefined')
    ocWorkflowPanel = null;
  $(container).load('/workflow/configurationPanel?definitionId=' + defId,
    function() {
      $(container).show('fast');
      if (callback) {
        callback();
      }
      if(ocWorkflowPanel && ocWorkflowPanel.registerComponents && typeof ocScheduler != 'undefined'){
        ocScheduler.workflowComponents = {}; //Clear the previously selected panel's components
        ocWorkflowPanel.registerComponents(ocScheduler.capture.components);
      }else{
        ocUtils.log("component registration handler not found in workflow.", defId);
      }
    }
  );
}

ocWorkflow.getConfiguration = function(container) {
  var out = new Object();
  $(container).find('.configField').each( function(idx, elm) {
    if ($(elm).is('[type=checkbox]')) {
      if ($(elm).is(':checked')) {
        out[$(elm).attr('id')] = $(elm).val();
      }
    } else {
      out[$(elm).attr('id')] = $(elm).val();
    }
  });
  return out;
}

