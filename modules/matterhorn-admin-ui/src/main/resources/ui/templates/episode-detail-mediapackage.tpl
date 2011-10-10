<div id="mediapackage">
  <h3>General</h3>
  <table class="kvtable">
    <% _.each(this, function(value, key) { %>
      <% if (opencast.episode.Utils.isStringOrNumber(value)) { %>
        <tr>
          <td class="td-key">
            <%= key %>
          </td>
          <td class="td-value">
            <% if (key == 'url') { %>
              <a href="<%= value %>"><%= value %></a>
            <% } else { %>
              <%= value %>
            <% } %>
          </td>
        </tr>
      <% } %>
    <% }); %>
  </table>

  <% __(this.media.track).whole(opencast.episode.Utils.full(function(tracks) { %>
    <br>
    <h3>Media</h3>
    <table class="kvtable">
      <% _.each(tracks, function(track) { %>
        <tr class="unfoldable-tr">
        <td class="td-key">
          <%= track.type %>
        </td>
        <td class="td-value">
          <%= track.mimetype %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% _.each(track, function(value, key) { %>
                <% if (opencast.episode.Utils.isStringOrNumber(value)) { %>
                  <tr>
                    <td class="td-key">
                      <%= key %>
                    </td>
                    <td class="td-value">
                        <% if (key == 'url') { %>
                      <a href="<%= value %>"><%= value %></a>
                      <% } else { %>
                        <%= value %>
                      <% } %>
                    </td>
                  </tr>
                <% } %>
              <% }); %>
            </table>
            <% if (track.audio || track.video) { %>
              <div>
                <% if (track.audio) { %>
                  <div class="subdetails">
                    <h4>Audio</h4>
                    <table class="subtable">
                      <% _.each(track.audio, function(value, key) { %>
                        <% if (opencast.episode.Utils.isStringOrNumber(value)) { %>
                          <tr>
                            <td class="td-key">
                              <%= key %>
                            </td>
                            <td class="td-value">
                              <% if (key == 'url') { %>
                                <a href="<%= value %>"><%= value %></a>
                              <% } else { %>
                                <%= value %>
                              <% } %>
                            </td>
                          </tr>
                        <% } %>
                      <% }); %>
                    </table>
                  </div>
                <% } %>
                <% if (track.video) { %>
                  <div class="subdetails">
                    <h4>Video</h4>
                    <table class="subtable">
                      <% _.each(track.video, function(value, key) { %>
                        <% if (opencast.episode.Utils.isStringOrNumber(value)) { %>
                          <tr>
                            <td class="td-key">
                              <%= key %>
                            </td>
                            <td class="td-value">
                              <% if (key == 'url') { %>
                                <a href="<%= value %>"><%= value %></a>
                              <% } else { %>
                                <%= value %>
                              <% } %>
                            </td>
                          </tr>
                        <% } %>
                      <% }); %>
                    </table>
                  </div>
                <% } %>
              </div>
            <% } %>
          </td>
        </tr>
      <% }); %>
    </table>
  <% })); %>

  <% __(this.metadata.catalog).whole(opencast.episode.Utils.full(function(catalogs) { %>
    <br>
    <h3>Metadata</h3>
    <table class="kvtable">
      <% _.each(catalogs, function(catalog) { %>
        <tr class="unfoldable-tr">
        <td class="td-key">
          <%= catalog.type %>
        </td>
        <td class="td-value">
          <%= catalog.id %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% _.each(catalog, function(value, key) { %>
                <% if (opencast.episode.Utils.isStringOrNumber(value)) { %>
                  <tr>
                    <td class="td-key">
                      <%= key %>
                    </td>
                    <td class="td-value">
                      <% if (key == 'url') { %>
                        <a href="<%= value %>"><%= value %></a>
                      <% } else { %>
                        <%= value %>
                      <% } %>
                    </td>
                  </tr>
                <% } %>
              <% }); %>
            </table>
          </div>
        </td>
      </tr>
      <% }); %>
    </table>
  <% })); %>


  <% __(this.attachments.attachment).whole(opencast.episode.Utils.full(function(attachments) { %>
    <br>
    <h3>Attachments</h3>
    <table class="kvtable">
      <% _.each(attachments, function(item) { %>
        <tr class="unfoldable-tr">
        <td class="td-key">
          <%= item.type %>
        </td>
        <td class="td-value">
          <%= item.id %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% _.each(item, function(value, key) { %>
                <% if (opencast.episode.Utils.isStringOrNumber(value)) { %>
                  <tr>
                    <td class="td-key">
                      <%= key %>
                    </td>
                    <td class="td-value">
                      <% if (key == 'url') { %>
                        <a href="<%= value %>"><%= value %></a>
                      <% } else { %>
                        <%= value %>
                      <% } %>
                    </td>
                  </tr>
                <% } %>
              <% }); %>
            </table>
          </div>
        </td>
      </tr>
      <% }); %>
    </table>
  <% })); %>
</div>