<div>
  <table id="seriesTable" width="100%" style="float:left;">
    <thead>
      <tr>
        <th width="25%" class="ui-widget-header"><div>Title</div></th>
    <th width="25%" class="ui-widget-header"><div>Organizer</div></th>
    <th width="25%" class="ui-widget-header"><div>Contributor</div></th>                        
    <th width="25%" class="ui-widget-header"><div>Action</div></th>
    </tr>
    </thead>
    <tbody>
      <% $.each(data[j].seriesView, function(key, series ) { %>
      <tr>
        <td class="ui-state-active"><%= series.title %></td>                       
        <td class="ui-state-active"><%= series.creator %></td>
        <td class="ui-state-active"><%= series.contributor %></td>
        <td class="ui-state-active" align="center">
          <a href="index.html#/viewseries?seriesId=<%= series.id %>">View Info</a><br />
          <a href="index.html#/series?seriesId=<%= series.id %>&edit=true">Edit</a>
          <!--  | <a href="javascript:ocSeriesList.deleteSeries('${series.id}', '${series.title}');">Delete</a> -->
        </td>
      </tr>
      <% }); %>
      
      <% if (data[j].seriesView.length == 0) { %>
      <tr>
        <td colspan="4" align="center" class="ui-state-active">No Series in the System</td>
      </tr>
      <% } %>
    </tbody>
  </table>
</div>