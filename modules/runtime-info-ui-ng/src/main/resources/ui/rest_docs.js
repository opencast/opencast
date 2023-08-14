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
    var docs = $('#docs'),
        tpl = $('#template').html();
    $.each(data, function(section) {
      if ('rest' == section) {
        data.rest.sort((a,b) => a.path > b.path ? 1 : -1);
        $.each(data.rest, function(i) {
          let path = data.rest[i].path,
              service = $(tpl);
          $('a', service).attr('href', '/docs.html?path=' + path);
          $('.path', service).text(path);
          $('.desc', service).text(data.rest[i].description);
          docs.append(service);
        });
        return;
      }
    });
    search();
  });
});
