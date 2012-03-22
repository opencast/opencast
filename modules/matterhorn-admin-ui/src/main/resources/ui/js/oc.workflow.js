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
var ocWorkflow = {} || ocWorkflow;

ocWorkflow.init = function(selectElm, configContainer) {
  ocWorkflow.container = configContainer;
  ocWorkflow.selector = selectElm;
  $(ocWorkflow.selector).change( function() {
    ocWorkflow.definitionSelected($(this).val(), configContainer);
  });
  ocWorkflow.loadDefinitions(selectElm, configContainer);
}

ocWorkflow.loadDefinitions = function(selector, container) {
  $.ajax({
    async: false,
    method: 'GET',
    url: '/workflow/definitions.json',
    dataType: 'json',
    success: function(data) {
      for (i in data.workflow_definitions) {
        if (data.workflow_definitions[i].id != 'error') {
          var option = document.createElement("option");
          option.setAttribute("value", data.workflow_definitions[i].id);
          option.innerHTML = data.workflow_definitions[i].title || data.workflow_definitions[i].id;
          if (data.workflow_definitions[i].id == "full") {
            option.setAttribute("selected", "true");
          }
          $(selector).append(option);
        }
      }
      ocWorkflow.definitionSelected($(selector).val(), container);
    }
  });
}

ocWorkflow.definitionSelected = function(defId, container, callback) {
  $(container).load('/workflow/configurationPanel?definitionId=' + defId,
    function() {
      $('.holdCheckbox').attr('checked', false);
      $(container).show('fast');
      if (callback) {
        callback();
      }
      if(ocWorkflowPanel && ocWorkflowPanel.registerComponents && typeof ocScheduler != 'undefined'){
        ocScheduler.workflowComponents = {}; //Clear the previously selected panel's components
        ocWorkflowPanel.registerComponents(ocScheduler.capture.components);
      }else{
        ocUtils.log("component registration handler not found.", ocWorkflowPanel, ocWorkflowPanel.registerComponents);
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

