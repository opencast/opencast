/*
 * Licensed to The Apereo Foundation under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 *
 * The Apereo Foundation licenses this file to you under the Educational
 * Community License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at:
 *
 *   http://opensource.org/licenses/ecl2.txt
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */

/* global $ */

$(document).ready(function () {
  $.ajax({
    url: '/ui/config/runtime-info-ui/settings.json',
    type: 'GET',
    error: function () {
      window.location.replace('/admin-ui/index.html');
    },
    success: function (data) {
      const adminUIUrl = data.adminUIUrl;
      $.ajax({
        url: adminUIUrl,
        type: 'HEAD',
        error: function (xhr){
          if (xhr.status == 404) {
            // Go to rest docs instead
            $.ajax({
              url: '/rest_docs.html',
              type: 'HEAD',
              error: function () {
                // Give up
              },
              success: function () {
                window.location.replace('/rest_docs.html');
              }
            });
          }
        },
        success: function () {
          window.location.replace(adminUIUrl);
        }
      });
    }
  });
});
