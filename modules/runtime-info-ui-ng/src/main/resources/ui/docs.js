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

/* TODO: We don't want alerts but we are stuck with them for now: */
/* eslint-disable no-alert */
/* global $ */

/**
 * Try to prettify JSON data. In case it fails, just return the original data.
 */
function tryPretty(data) {
  try {
    var obj = JSON.parse(data);
    return JSON.stringify(obj, undefined, 2).toString();
  } catch(err) {
    return data;
  }
}

/**
 * Takes a path and integrates the pathParams values into it
 * @param {String} path the path with keys (e.g. /my/{thing}/{stuff})
 * @param {Array} params the params to put into the path (e.g. {'thing':'apple'})
 */
function updatePath(path, params) {
  var newPath = path;
  for (var key in params) {
    if (Object.prototype.hasOwnProperty.call(params, key)) {
      var value = params[key];
      if (value !== undefined && value !== null && value !== '') {
        // The Regex here handles syntax like /episode.{format:xml|json}.
        // In this case, the {format:xml|json} part is extracted, then
        // the xml|json part is used as the second regex to verify user's input.
        // If the input is valid, the path /episode.{format:xml|json} would be
        // replaced by /episode.xml for example.
        var regex = new RegExp( '{' + key + '(:[^}]*|)}', '');
        if (regex.test(newPath))
        {
          var pat = regex.exec(newPath)[1].substring(1);
          var regex2 = new RegExp(pat, '');
          if (regex2.test(value))
          {
            newPath = newPath.replace(regex, value);
          } else
          {
            alert('The value for ' + key + ' is invalid.');
          }
        } else
        {
          alert('wrong syntax');
        }
      }
    }
  }
  return newPath;
}

function checkPath($form) {
  var params = [];
  $form.find('.form_param_path').each( function() {
    var $param = $(this);
    params[$param.attr('name')] = $param.val();
  });
  var form_path = $form.find('input.form_action_holder').val();
  var path = updatePath(form_path, params);
  // update form and display
  $form.attr('action', path);
  $form.find('.form_path').html(path);
  return path.indexOf('{') < 0 && path.indexOf('}') < 0;
}

function checkRequired($form) {
  // check all required items
  var $required = $form.find('.form_param_required');
  var total = $required.length;
  var counter = 0;
  $required.each( function() {
    var $this = $(this);
    if ($this.val() !== null && $this.val() !== '') {
      counter++;
    }
  });
  var $formInputs = $form.find('div.form_submit input');
  if (counter >= total) {
    // submit is ok
    $formInputs.removeAttr('disabled');
    return true;
  } else {
    // disable form submit until required options at set
    $formInputs.attr('disabled', 'disabled');
    return false;
  }
}

$(document).ready(function() {

  $('a.show_form_link').click(
    function() {
      var $this = $(this);
      var $form = $this.parent().find('div.hidden_form');
      var $hidectrl = $this.parent().find('a.hide_form_link');
      $form.toggle(400);
      $hidectrl.show();
      $this.parent().find('div.form_submit input[type=\'button\']').click(
        function() {
          $form.hide(200);
          $this.show();
        }
      );
      $this.hide();
      return false;
    }
  );

  $('a.hide_form_link').click(
    function() {
      var $this = $(this);
      var $form = $this.parent().find('div.hidden_form');
      var $show_form_ctrl = $this.parent().find('a.show_form_link');
      $form.hide(200);
      $show_form_ctrl.show();
      $this.hide();
      return false;
    }
  );

  // do operation on all testing forms
  $('form.form_test_form').each( function() {
    var $form = $(this);
    if ($form.find('input.form_action_holder').length >= 1) {
      // now find and put the event handler on all path params
      $form.find('.form_param_path').change( function() {
        checkPath($form);
      });
      // run the checkPath method
      checkPath($form);
      var $reqParams = $form.find('.form_param_required');
      if ($reqParams.length > 0) {
        // add required options checks
        $reqParams.change( function(){
          checkRequired($form);
        }).keyup( function(){
          checkRequired($form);
        });
      }
      // run the check required
      checkRequired($form);
      // handle the ajax submissions
      if ($form.find('input.form_ajax_submit').length >= 1) {
        // the form will use ajax submit
        //var form_key = $form.find("input.form_endpoint_name").val();
        // add an event handler to the form submit
        $form.bind('submit', function() {
          if ( checkRequired($form) ) {
            var submitParams = {};
            $form.find('.form_param_submit').each( function() {
              var $param = $(this);
              var $value = $param.val();
              if (this.type == 'checkbox' && !this.checked) {
                $value = 'false';
              }
              submitParams[$param.attr('name')] = $value;
            });
            var method = $form.find('.form_method').val();
            var url = $form.attr('action');
            $form.parent().find('.test_form_working').show();
            $form.parent().find('.test_form_response input').click(function(){
              $(this).parent.hide();
            });

            // clear previous responses
            var responseBody = $form.parent().find('.test_form_response');
            responseBody.hide();
            responseBody.find('pre').text('');

            // make the request
            $.ajax({
              type: method,
              url: url,
              processData: true,
              dataType: 'text',
              data: submitParams,
              success: function(data, textStatus, request) {
                $form.parent().find('.test_form_working').hide();
                var responseBody = $form.parent().find('.test_form_response');
                data = tryPretty(data);
                responseBody.show();
                var msg = 'Status: ' + request.status + ' (' + request.statusText + ')\n\n' + data;
                responseBody.find('pre').text(msg);
              },
              error: function(request) {
                $form.parent().find('.test_form_working').hide();
                var responseBody = $form.parent().find('.test_form_response');
                var msg = 'Status: ' + request.status + ' (' + request.statusText + ')';
                responseBody.show();
                responseBody.find('pre').text(msg);
              }
            });
          } else {
            alert('Fill out all required fields first');
          }
          return false;
        });
      }
    }
  });
});
