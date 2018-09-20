/**
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

function search() {
  var value = $('input').val();
  $('li').each(function() {
    $(this).toggle($(this).text().toLowerCase().indexOf(value.toLowerCase()) >= 0);
  });
}

$(document).ready(function($) {
  $('input').change(search);
  $('input').keyup(search);

  $.getJSON('/info/components.json', function(data) {
    $.each(data, function(section) {
      if ('rest' == section) {
        data.rest.sort((a,b) => a.path > b.path ? 1 : -1);
        $.each(data.rest, function(i) {
          $('#docs').append('<li><a href="/docs.html?path=' + data.rest[i].path + '">'
              + '<span>' + data.rest[i].path + '</span>'
              + data.rest[i].description + '</a></li>');
        });
        return;
      }
    });
    search();
  });
});
