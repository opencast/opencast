<%
/* expects { episode: {}, workflows: {} } */
%>
<h2>Details for <%= this.episode.title %></h2>

<div class="section">
  <div class="section-title">Mediapackage</div>
  <div id="mediaPackageDetails"></div>
</div>

<div class="section">
  <div class="section-title">Workflows</div>
  <table id="workflows" class="ui-widget" cellspacing="0" width="100%">
  <thead>
  <tr>
    <th id="sortDate" width="18%" class="ui-widget-header">
      <div>Runtime</div>
    </th>
    <th id="sortTitle" width="25%" class="ui-widget-header">
      <div>Workflow</div>
    </th>
    <th id="sortPresenter" width="15%" class="ui-widget-header">
      <div>Status</div>
    </th>
    <th id="sortSeries" width="15%" class="ui-widget-header">
    </th>
  </tr>
  </thead>
  <tbody>
    <% _.each(this.workflows, function(a) { %>
      <tr valign="top">
        <td class="ui-state-active"><%= a.started %> - <%= a.completed %></td>
        <td class="ui-state-active"><%= a.title %></td>
        <td class="ui-state-active"><%= a.state %></td>
        <td class="ui-state-active"><a href="index.html#/inspect?id=<%= a.id %>">Details</a></td>
      </tr>
    <% }); %>
    <% if (this.workflows.length == 0) { %>
      <tr>
        <td colspan="6" align="center">No workflows found</td>
      </tr>
    <% } %>
  </tbody>
</table>
</div>
