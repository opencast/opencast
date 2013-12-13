<table id="episodesTable" class="ui-widget" cellspacing="0" width="100%">
  <thead>
  <tr>
    <th class="ui-state-default">
      <input type="checkbox" id="selectAllEpisodes" title="Select all archives on this page"/>
    </th>
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
    <th class="ui-widget-header">Process</th>
    <th class="ui-widget-header">Published</th>
    <th class="ui-widget-header"></th>
    <th class="ui-widget-header"></th>
    <th class="ui-widget-header">ACL</th>
  </tr>
  </thead>
  <tbody>
    <% _.each(this.episodes, function(e) { %>
      <tr valign="top" class="episodeEl" id="e-<%= e.id %>">
        <td class="ui-state-active">
          <input type="checkbox" value="<%= e.id %>" class="selectEpisode"/>
        </td>
        <td class="ui-state-active"><%= e.title %></td>
        <td class="ui-state-active"><%= e.creators %></td>
        <td class="ui-state-active"><%= ocUtils.dflt(e.seriesTitle) %>
          <% if (e.series) { %>
            <input type="hidden" class="seriesId" value="<%= e.series %>"/>
          <% } %>
        </td>
        <td class="ui-state-active" style="text-align: right"><%= e.date %></td>
        <td class="ui-state-active">
          <% if (e.workflow) { %>
            <span class="active-workflow"><%= e.workflow %></span>
          <% } %>
        </td>
        <td class="ui-state-active">
         <ul>
         <% _.each(e.publications, function(publication) { %>
            <li><%= publication.channel %></li>
         <% }); %>
         </ul>
        </td>
        <td class="ui-state-active">
          <a href="#" class="edit" data-eid="<%= e.id %>">Edit</a>
        </td>
        <td class="ui-state-active" style="text-align: center">
          <a href="#/episodedetails?id=<%= e.id %>">Details</a>
          <!--<% _.each(e.media, function(m) { %>-->
          <!--<a href="<%= m.url %>"><%= m.mimetype %></a>-->
          <!--<% }); %>-->
        </td>
        <td class="ui-state-active">
          <div class="toggle-scheduler ui-icon ui-icon-triangle-1-e"></div>
        </td>
      </tr>

      <!-- START Episode schedulers -->
      <tr colspan="10" class="episode-scheduler" style="display:none">
        <td class="ui-state-active episode-scheduler-header" colspan="2">
          <h2>ACL scheduling</h2>
          <button class="add"><div class="ui-icon ui-icon-plusthick"></div></button>
        </td>
        <td class="ui-state-active episode-scheduler-content" colspan="8">

            <div class="episode-schedule">
              <span><h3>Date</h3></span>
              <span><h3>Acl</h3></span>
              <span><h3>Workflow</h3></span>
              <span></span>
            </div>

            <div class="schedules-container">
              There is no ACL schedule for this episode.
            </div>

        </td>
      </tr>
      <!-- END Episode schedulers -->

    <% }); %>
    <% if (this.episodes.length == 0) { %>
      <tr>
        <td colspan="6" align="center">No Archives found</td>
      </tr>
    <% } %>
  </tbody>
</table>
