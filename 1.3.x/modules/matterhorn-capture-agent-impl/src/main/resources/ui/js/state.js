var agentState = agentState || { };

agentState.stateServiceUrl = "/state";

agentState.getCurrent = function(elm) {
  $.get(this.stateServiceUrl + "/state", function(data) { $(elm).text(data); });
}

$(document).ready(function() {
  agentState.getCurrent("state");
});
