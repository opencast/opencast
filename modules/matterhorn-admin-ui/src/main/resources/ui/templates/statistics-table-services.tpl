<div>
  <% $.each(data[j].servicesView, function(key, service) { %>
  <h2><%! ocStatistics.labelName(service.id) %></h2>
  <table id="statsTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr>
        <th id="sortService" width="40%" class="ui-widget-header">Host<span></span></th>
        <th id="sortJobsCompleted" width="12%" class="ui-widget-header">Jobs Completed<span></span></th>
        <th id="sortJobsRunning" width="12%" class="ui-widget-header">Jobs Running<span></span></th>
        <th id="sortJobsQueued" width="12%" class="ui-widget-header">Jobs Queued<span></span></th>
        <th id="sortMeanRunTime" width="12%" class="ui-widget-header">Mean Run Time<span></span></th>
        <th id="sortMeanQueueTime" width="12%" class="ui-widget-header">Mean Queue Time<span></span></th>
      </tr>
    </thead>
    <tbody>
      <% $.each(service.servers, function (key, server) { %>
      <tr valign="top">
        <td class="ui-state-active">
            <img style="vertical-align:middle; margin-right:5px;" src=<% if (server.online) { %><% if(server.maintenance) { %>"/admin/img/icons/maintenance.png" title="Maintenance Mode"<% } else { %> "/admin/img/icons/available.png" title="Online"<% } } else {%>"/admin/img/icons/offline.png" title="Offline"<% } %>/>
		  	<% if (server.state != "NORMAL") { %>
		  	<img style="vertical-align:middle; margin-right:5px;" src=<% if (server.state == "WARNING") { %>"/admin/img/icons/lightbulb.png" title="Warning State" <% } else { %> "/admin/img/icons/exclamation.png" title="Error State" <% } %>/>
		  	<a class="service-sanitize" title="Sanitize" style="vertical-align:middle; margin-right:5px;" href="host=<%! server.host %>&serviceType=<%! server.type %>">Sanitize</a>
		  	<% } %>
			<span style="vertical-align:middle;"><%! server.host %></span>
        </td>
        <td class="ui-state-active center">
          <%! server.finished %>
        </td>
        <td class="ui-state-active center">
          <%! server.running %>
        </td>
        <td class="ui-state-active center">
          <%! server.queued %>
        </td>
        <td class="ui-state-active center">
          <%! server.meanRunTime %>
        </td>
        <td class="ui-state-active center">
          <%! server.meanQueueTime %>
        </td>
      </tr>
      <% }); %>
      <tr>
        <td style="text-align:right; padding-right:10px; font-weight:bold;">Total</td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! service.finishedTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! service.runningTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! service.queuedTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! service.meanRunTimeTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! service.meanQueueTimeTotal %></td>
      </tr>
    </tbody>
  </table>
  <% }); %>
</div>
