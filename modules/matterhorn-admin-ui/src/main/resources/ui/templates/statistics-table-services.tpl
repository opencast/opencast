<div>
  <% $.each(data[j].servicesView, function(key, service) { %>
  <h2><% if (ocStatistics.labels[service.id]) { %><%= ocStatistics.labels[service.id]%> <% } else { %><%= service.id %><% } %></h2>
  <table id="statsTable" class="ui-widget" cellspacing="0" width="100%">
    <thead>
      <tr>
        <th id="sortService" width="40%" class="ui-widget-header">Host<span></span></th>    
        <th id="sortJobsRunning" width="15%" class="ui-widget-header">Jobs Running<span></span></th>
        <th id="sortJobsQueued" width="15%" class="ui-widget-header">Jobs Queued<span></span></th>
        <th id="sortMeanRunTime" width="15%" class="ui-widget-header">Mean Run Time<span></span></th>
        <th id="sortMeanQueueTime" width="15%" class="ui-widget-header">Mean Queue Time<span></span></th>
      </tr>
    </thead>
    <tbody>
      <% $.each(service.servers, function (key, server) { %>
      <tr valign="top">
        <td class="ui-state-active"">
            <img style="vertical-align:middle; margin-right:5px;" src=<% if (server.online) { %>"/admin/img/icons/available.png" title="Online"<% } else if (server.maintenance) { %>"/admin/img/icons/maintenance.png" title="Maintenance"<% } else { %>"/admin/img/icons/offline.png" title="Offline"<% } %>/>
             <%= server.host %>
        </td>        
        <td class="ui-state-active  center">
          <%= server.running %>
        </td>
        <td class="ui-state-active  center">
          <%= server.queued %>
        </td>
        <td class="ui-state-active  center">
          <%= server.meanRunTime %>
        </td>
        <td class="ui-state-active  center">
          <%= server.meanQueueTime %>
        </td>
      </tr>
      <% }); %>
      <tr>
        <td style="text-align:right; padding-right:10px; font-weight:bold;">Total</td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%= service.runningTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%= service.queuedTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%= service.meanRunTimeTotal %></td>
        <td class="ui-state-active center" style="font-weight:bold;"> <%= service.meanQueueTimeTotal %></td>
      </tr>
    </tbody>
  </table>
  <% }); %>
</div>