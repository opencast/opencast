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

$(document).ready(function () {
  $.getJSON('/info/components.json', function (data) {
    var capt = false;
    var confMon = false;
    $.each(data, function (section) {
      if ('rest' == section) {
        $.each(data.rest, function (i) {
          var col = $('#docs1');
          var pathToDocs = data.rest[i].docs.replace(/^.*\/\/[^/]+/, '');
          col.append('<li>' + data.rest[i].description + ' <a href="' + pathToDocs + '">Docs</a></li>');
          if (data.rest[i].description.toLowerCase() == 'Capture REST Endpoint'.toLowerCase()) {
            $('#col3m').append('<br /><a class="redbutton" id="captureLink" href="/capture">Go to Capture</a>');
            capt = true;
          } else if (data.rest[i].description.toLowerCase() == 'Confidence Monitoring REST Endpoint'.toLowerCase()) {
            $('#col4m').append('<br /><a class="redbutton" id="confidenceMonitoringLink" '
              + 'href="/confidence-monitoring">Go to Confidence Monitoring</a>');
            confMon = true;
          }
        });
      } else if ('engage' == section) {
        $('#engagelink').attr('href', data.engage + '/engage/ui');
      } else if ('admin' == section) {
        $('#adminlink').attr('href', data.admin + '/admin-ng/index.html');
      }
    });
    if (capt) {
      $('#col3m').show();
    }
    if (confMon) {
      $('#col4m').show();
    }
  });

  var $sysInfoContainer = $('div#system-info'),
      checkAndDisplay = function (field, $container) {
        var value = field;
        if (typeof field !== 'undefined' && field !== null) {
          if (field instanceof Array) {
            value = field.toString();
          } else if (field instanceof Date) {
            value = field.toLocaleDateString() + ', ' + field.toLocaleTimeString();
          }
          $container.html(value);
          $container.parent().show();
        } else {
          $container.parent().hide();
        }
      };

  $.getJSON('/sysinfo/bundles/version', function (data) {
    var $message = $sysInfoContainer.find('div#build span.message'),
        $messageDiv = $sysInfoContainer.find('div#build'),
        $version = $sysInfoContainer.find('div#version span'),
        $buildNumber = $sysInfoContainer.find('div#build-number span'),
        versions = [],
        buildNumbers = [];


    if (data.consistent) {
      $message.html('Opencast is running with consistent bundles version.');
      $messageDiv.addClass('ok');

      checkAndDisplay(data.version, $version);
      checkAndDisplay(data.buildNumber, $buildNumber);
    } else {
      $message.html('Opencast is running with inconsistent bundles version. Update the deprecated bundles to avoid loss or corruption of data.');
      $buildNumber.addClass('error');

      if (typeof data.versions !== 'undefined') {
        $.each(data.versions, function (index, element) {
          if ($.inArray(element.version, versions)) {
            versions.push(element.version);
          }

          if ($.inArray(element.buildNumber, buildNumbers)) {
            buildNumbers.push(element.buildNumber);
          }
        });
      }

      if (versions.length > 0) {
        $version.before('Version' + (versions.length > 1 ? 's: ' : ': '));
        checkAndDisplay(versions, $version);
      }

      if (buildNumbers.length > 0) {
        $buildNumber.before('Build number' + (buildNumbers.length > 1 ? 's: ' : ': '));
        checkAndDisplay(buildNumbers, $buildNumber);
      }
    }
  });
});
