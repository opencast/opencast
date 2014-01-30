<div>
  <span style="margin-top: 20px; float: left;"><%= data[j].totalCount %> Series</span>
  <table id="seriesTable" width="100%" style="float:left;">
    <thead>
      <tr>
        <th width="25%" class="ui-widget-header sortable"><div>Title<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></th>
    <th width="25%" class="ui-widget-header sortable"><div>Organizer<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></th>
    <th width="25%" class="ui-widget-header sortable"><div>Contributor<div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div></th>                        
    <th width="25%" class="ui-widget-header"><div>Action</div></th>
    <th width="25%" class="ui-widget-header"><div>ACL</div></th>
    </tr>
    </thead>
    <tbody>
      <% $.each(data[j].seriesView, function(key, series ) { %>
      <tr class="seriesEl" id="e-<%= series.id %>">
        <td class="ui-state-active"><%= series.title %></td>                       
        <td class="ui-state-active"><%= (series.creator) ? series.creator : '' %></td>
        <td class="ui-state-active"><%= (series.contributor) ? series.contributor : '' %></td>
        <td class="ui-state-active" align="center">
          <a href="index.html#/viewseries?seriesId=<%= series.id %>">View Info</a><br />
          <a href="index.html#/series?seriesId=<%= series.id %>&edit=true">Edit</a>
          <!--  | <a href="javascript:ocSeriesList.deleteSeries('${series.id}', '${series.title}');">Delete</a> -->
        </td>
        <td class="ui-state-active">
          <div class="toggle-scheduler ui-icon ui-icon-triangle-1-e"></div>
        </td>
      </tr>

      <!-- START series schedulers -->
      <tr class="series-scheduler" style="display:none">
        <td class="ui-state-active series-scheduler-header" colspan="1">
          <h2>ACL scheduling</h2>
          <button class="add"><div class="ui-icon ui-icon-plusthick"></div></button>
        </td>
        <td class="ui-state-active series-scheduler-content" colspan="4">

            <div class="series-schedule">
              <span><h3>Date</h3></span>
              <span><h3>Override</h3></span>
              <span><h3>ACL</h3></span>
              <span><h3>Workflow</h3></span>
              <span></span>
            </div>

            <div class="schedules-container">
              There is no ACL schedule for this series.
            </div>

        </td>
      </tr>
      <!-- END series schedulers -->
      <% }); %>
      
      <% if (data[j].seriesView.length == 0) { %>
      <tr>
        <td colspan="4" align="center" class="ui-state-active">No Series in the System</td>
      </tr>
      <% } %>
    </tbody>
  </table>
</div>