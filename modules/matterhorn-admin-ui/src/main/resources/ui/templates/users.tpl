<div>
  <div id="usersDiv">
    <table id="usersTable" width="100%" style="float:left;">
      <thead>
        <tr>
          <th width="25%" class="ui-widget-header"><div>User Name</div></th>
      <th width="25%" class="ui-widget-header"><div>Roles</div></th>
      </tr>
      </thead>
      <tbody>
      <form>
        <% $.each(data[j].users, function(key, user) { %>
        <tr>
          <td class="ui-state-active">
            <%= user.username %>
          </td>
          <td class="ui-state-active">
            <input type="text" disabled="disabled" id="text_<%= user.username %>" size="50" value="<%= user.roles %>"/>
            <!-- <input type="button" class="roleButton" id="button_<%= user.username %>" value="update"/> -->
          </td>
        </tr>
        <% }); %>
      </form>
      </tbody>
  </div>
</div>