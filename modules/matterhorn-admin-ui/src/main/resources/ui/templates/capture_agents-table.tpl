<div id="stage" class="ui-widget ui-helper-clearfix">
  <table id="captureTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr>
        <th id="sortAgent" width="25%" class="ui-widget-header sortable"><div>Agent Name<div class="sort-icon ui-icon-triangle-2-n-s"></div></div></th>
        <th id="sortCapabilities" width="50%" class="ui-widget-header sortable"><div>Capabilities<div class="sort-icon ui-icon-triangle-2-n-s"></div></div></th>
        <th id="sortStatus" width="25%" class="ui-widget-header sortable"><div>Status<div class="sort-icon ui-icon-triangle-2-n-s"></div></div></th>
      </tr>
    </thead>
    <tbody>
      <% if (data[j].agents.length > 0) { %>
	    <% $.each(data[j].agents, function(key, agent) { %>	
	      <tr>
	        <td class="ui-state-active">
	          <div style="margin-left:10px;"><a title="<%! agent.name %>" href="<%= agent.url %>"><%! agent.name %></a></div>
	        </td>
	        <td class="ui-state-active" align="center">
	          <% if (agent.devices) { %>
	          <ul class="propnav">
	            <% $.each(agent.devices, function(key, capability) { %>
	            <li>
	              <span class="subnav"></span>
	              <span class="device"><%! capability.device %></span>
	              <% if (capability.properties) { %>
	              <ul class="itemnav">
	                <% $.each(capability.properties, function(t, property) { %>
	                <li><span class="dev-prop"><%! property.key %> :</span><span class="dev-prop-val"><%! property.value %></span>
	                </li>
	                <% }); %>
	              </ul>
	              <% } %>
	            </li>
	            <% }); %>
	          </ul>
	          <% } else {%>
	          <span style="text-align:center;">No capabilities found</span>
	          <% } %>
	        </td>
	        <% if (agent.state) { %>
	        <td class="ui-state-active">
	          <span class="icon icon-<%= agent.state %>"></span>
	          <span><%= agent.state %></span>
	        </td>

	        <% } else {%>
	        <td class="ui-state-active"><td>
	        <% } %>
	      </tr>
	    <% }); %>
	  <% } else { %>
        <tr>
          <td class="ui-state-active" colspan="3" align="center">
            No Capture Agents found
          </td>
        </tr>
      <% } %>
    </tbody>
  </table>
</div>
