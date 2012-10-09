<div>
  <ul>
    <li><a href="#instance">Workflow Instance</a></li>
    <li><a href="#mediapackage">Mediapackage</a></li>
    <li><a href="#operations">Operations</a></li>
    <li><a href="#performance">Performance</a></li>
  </ul>

  <div id="instance">
    <h3>Instance</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow, function(key, value) { %>
      <% if (typeof value == 'string' || typeof value == 'number') { %>
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
    <br>
    <h3>Configuration</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow.config, function(key, value) { %>
      <% if (typeof value == 'string' || typeof value == 'number') { %>
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

  <div id="mediapackage">
    <h3>General</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow.mediapackage, function(key, value) { %>
      <% if (typeof value == 'string' || typeof value == 'number') { %>
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
    <% if ($(data[j].workflow.mediapackage.media.track).size() > 0) { %>
    <br>
    <h3>Media</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow.mediapackage.media.track, function(key, track) { %>
      <tr class="unfoldable-tr">
        <td class="td-key">
          <%= track.type %>
        </td>
        <td class="td-value">
          <%= track.mimetype %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% $.each(track, function(key, value) { %>
              <% if (typeof value == 'string' || typeof value == 'number') { %>
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
                  <% $.each(track.audio, function(key, value) { %>
                  <% if (typeof value == 'string' || typeof value == 'number') { %>
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
                  <% $.each(track.video, function(key, value) { %>
                  <% if (typeof value == 'string' || typeof value == 'number') { %>
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
    <% } %>
    <% if ($(data[j].workflow.mediapackage.metadata.catalog).size() > 0) { %>
    <br>
    <h3>Metadata</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow.mediapackage.metadata.catalog, function(key, catalog) { %>
      <tr class="unfoldable-tr">
        <td class="td-key">
          <%= catalog.type %>
        </td>
        <td class="td-value">
          <%= catalog.id %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% $.each(catalog, function(key, value) { %>
              <% if (typeof value == 'string' || typeof value == 'number') { %>
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
    <% } %> <!-- endif -->
    <% if ($(data[j].workflow.mediapackage.attachments).size() > 0) { %>
    <br>
    <h3>Attachments</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow.mediapackage.attachments, function(key, item) { %>
      <tr class="unfoldable-tr">
        <td class="td-key">
          <%= item.type %>
        </td>
        <td class="td-value">
          <%= item.id %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% $.each(item, function(key, value) { %>
              <% if (typeof value == 'string' || typeof value == 'number') { %>
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
    <% } %>
  </div>


  <div id="operations">
    <h3>Operations</h3>
    <table class="kvtable">
      <% $.each(data[j].workflow.operations, function(key, op) { %>
      <tr class="unfoldable-tr">
        <td class="td-value" style="font-size: 70%; vertical-align: top;
            <% if (op.state=='SUCCEEDED') { %>color:green;<% } %>
            <% if (op.state=='FAILED') { %>color:red;<% } %>
            <% if (op.state=='PAUSED') { %>color:orange;<% } %>
            <% if (op.state=='INSTANTIATED') { %>color:blue;<% } %>
            ">
          <%= op.state %>
        </td>
        <td class="td-key">
          <%= op.id %>
        </td>
        <td class="td-value">
          <%= op.description %>
          <div class="unfoldable-content">
            <table class="subtable">
              <% $.each(op, function(key, value) { %>
              <% if (typeof value == 'string' || typeof value == 'number') { %>
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
            <% if ($(op.configurations).size() > 0) { %>
            <h4>Parameter</h4>
            <table class="subtable">
              <% $.each(op.configurations, function(key, value) { %>
              <% if (typeof value == 'string' || typeof value == 'number') { %>
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
            <% } %>
          </div>
        </td>
      </tr>
      <% }); %>
    </table>
  </div>     








  <div id="performance">
    <div id="graph" style="width: 760px; height: 400px"></div>  </div>
</div>