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
            <%! user.username %>
          </td>
          <td class="ui-state-active">
            <%! user.roles %>
          </td>
        </tr>
        <% }); %>
      </form>
      </tbody>
  </div>
</div>
