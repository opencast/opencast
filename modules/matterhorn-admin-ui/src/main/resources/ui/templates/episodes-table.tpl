<table id="episodesTable" class="ui-widget" cellspacing="0" width="100%">
  <thead>
  <tr>
    <!--<th class="ui-state-default"><input type="checkbox" id="selectAllRecordings"-->
    <!--onclick="ocArchive.selectAll(this.checked);"/></th>-->
    <th id="sortTitle" width="25%" class="ui-widget-header sortable">
      <div>Title
        <div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div>
      </div>
    </th>
    <th id="sortPresenter" width="15%" class="ui-widget-header sortable">
      <div>Presenter
        <div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div>
      </div>
    </th>
    <th id="sortSeries" width="15%" class="ui-widget-header sortable">
      <div>Course/Series
        <div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div>
      </div>
    </th>
    <th id="sortDate" width="18%" class="ui-widget-header sortable">
      <div>Creation Date
        <div class="sort-icon ui-icon ui-icon-triangle-2-n-s"></div>
      </div>
    </th>
    <th class="ui-widget-header">Media</th>
    <th class="ui-widget-header">Process</th>
    <th class="ui-widget-header">Action</th>
  </tr>
  </thead>
  <tbody>
    <% _.each(this.episodes, function(e) { %>
      <tr valign="top">
        <!--<td class="ui-state-active"><input type="checkbox" value="${mp.id}" class="selectRecording"/></td>-->
        <td class="ui-state-active"><%= e.title %></td>
        <td class="ui-state-active"><%= e.creators %></td>
        <td class="ui-state-active"><%= ocUtils.emptyUndef(e.seriesTitle) %></td>
        <td class="ui-state-active"><%= e.date %></td>
        <td class="ui-state-active">
          <% _.each(e.media, function(m) { %>
            <a href="<%= m.url %>"><%= m.mimetype %></a>
          <% }); %>
        </td>
        <td class="ui-state-active">
          <% if (e.workflow) { %>
            <span class="active-workflow"><%= e.workflow %></span>
          <% } %>
        </td>
        <td class="ui-state-active">
          <% if (e.media.length > 0) { %>
            <a onclick="ocArchive.retract('<%= e.id %>'); return false;" href="#">Retract</a>
          <% } %>
        </td>
      </tr>
    <% }); %>
    <% if (this.episodes.length == 0) { %>
      <tr>
        <td colspan="6" align="center">No Episodes found</td>
      </tr>
    <% } %>
  </tbody>
</table>
