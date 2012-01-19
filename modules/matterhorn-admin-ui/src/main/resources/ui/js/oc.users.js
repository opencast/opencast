/**
 *  Copyright 2009 The Regents of the University of California
 *  Licensed under the Educational Community License, Version 2.0
 *  (the "License"); you may not use this file except in compliance
 *  with the License. You may obtain a copy of the License at
 *
 *  http://www.osedu.org/licenses/ECL-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an "AS IS"
 *  BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *  or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */
ocUsers = {} || ocUsers;
ocUsers.users = [];

ocUsers.init = function () {
  $.ajax( {
    url: '/users/users.json?limit=1000',
    type: 'GET',
    success: ocUsers.buildUsersView
  })
        
}

ocUsers.buildUsersView = function(users) {
  ocUsers.users = users;
  $.each(ocUsers.users, function (key, user) {
    user.roles = user.roles.join(', ');
  });
  $('#addHeader').jqotesubtpl('templates/users.tpl', {users: ocUsers.users});
  // Attach actions to the update buttons
  $(".roleButton").each(function (i) {
    $(this).click(function() {
      // POST the role array to /users/[username].json
      var row = $(this).parent().parent();
      var username = this.id.substring(7); // "button_" = 7 characters
      var url = "/users/" + username + ".json";
      var roleArray = $("#text_" + username).val().split(",");
      var roles = "[";
      for(i =0; i < roleArray.length; i++) {
        roles +="\"";
        roles += roleArray[i].trim();
        roles +="\"";
        if(i < roleArray.length -1) {
          roles += ",";
        }
      }
      roles +="]";
      $.ajax({
        url: url,
        type: 'PUT',
        dataType: 'text',
        data: {
          "roles": roles
        },
        success: function() {
          row.fadeOut('slow', function() {
            row.fadeIn();
          });
        }
      });
    });
  });
}