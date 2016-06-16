<div>
  <% $.each(data[j].serversView, function(key, server) { %>
  <h2><img style="width: 16px; height: 16px;"
    src=<% if (server.online) { %><% if (server.maintenance) { %>"/admin/img/icons/maintenance.png"
    title="Maintenance Mode"<% } else { %>"/admin/img/icons/available.png"
    title="Online"<% } } else {%>"/admin/img/icons/offline.png"
    title="Offline"<% } %>/> <%! server.host %></h2>
  <input class="server-maintenance" value="<%! server.online %>" name="<%! server.host %>"
    type="checkbox" <% if (server.maintenance) { %> checked="checked" <% } %>> Maintenance</input>
  <table id="statsTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr>
        <th id="sortService" width="40%" class="ui-widget-header">Service<span></span></th>
        <th id="sortJobsCompleted" width="12%" class="ui-widget-header">Jobs Completed<span></span></th>
        <th id="sortJobsRunning" width="12%" class="ui-widget-header">Jobs Running<span></span></th>
        <th id="sortJobsQueued" width="12%" class="ui-widget-header">Jobs Queued<span></span></th>
        <th id="sortMeanRunTime" width="12%" class="ui-widget-header">Mean Run Time<span></span></th>
        <th id="sortMeanQueueTime" width="12%" class="ui-widget-header">Mean Queue Time<span></span></th>
      </tr>
    </thead>
    <tbody>
      <% $.each(server.services, function(key, service) { %>
      <tr valign="top">
        <td class="ui-state-active">
          <img style="vertical-align:middle; margin-right:5px; width: 16px; height: 16px;"
            src=<% if (service.online) { %><% if (service.maintenance) { %>"/admin/img/icons/maintenance.png"
            title="Maintenance Mode"<% } else { %> "/admin/img/icons/available.png"
            title="Online"<% } } else {%>"/admin/img/icons/offline.png" title="Offline"<% } %>/>
          <% if (service.state != "NORMAL") { %>
          <img style="vertical-align:middle; margin-right:5px; width: 16px; height: 16px;"
            src=<% if (service.state == "WARNING") { %>"/admin/img/icons/lightbulb.png"
            title="Warning State" <% } else { %> "/admin/img/icons/exclamation.png" title="Error State" <% } %>/>
          <a class="service-sanitize" title="Sanitize" style="vertical-align:middle; margin-right: 5px;"
            href="host=<%! server.host %>&serviceType=<%! service.type %>">Sanitize</a>
          <% } %>
          <span style="vertical-align:middle;"><%! ocStatistics.labelName(service.id) %></span>
        </td>
        <td class="ui-state-active center">
          <%! service.finished %>
        </td>
        <td class="ui-state-active center">
          <%! service.running %>
        </td>
        <td class="ui-state-active  center">
          <%! service.queued %>
        </td>
        <td class="ui-state-active  center">
          <%! service.meanRunTime %>
        </td>
        <td class="ui-state-active  center">
          <%! service.meanQueueTime %>
        </td>
      </tr>
      <% }); %>
      <tr>
        <td style=" text-align:right; padding-right:10px; font-weight:bold;">Total</td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! server.finishedTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! server.runningTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! server.queuedTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! server.meanRunTimeTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%! server.meanQueueTimeTotal %></td>
      </tr>
    </tbody>
  </table>
  <% }); %>
</div>
