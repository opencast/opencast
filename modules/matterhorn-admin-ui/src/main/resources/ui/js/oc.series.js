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

/*    NAMSPACES    */
var ocSeries        = ocSeries || {};
ocSeries.components = {};
ocSeries.additionalComponents = {};
ocSeries.roles = [];
ocSeries.seriesRolesList = {};
ocSeries.anonymous_role = "";

/*    PAGE CONFIGURATION    */
var SERIES_SERVICE_URL = "/series";
var SERIES_LIST_URL = "/admin/index.html#/series_list";
var ANOYMOUS_URL = "/info/me.json";
var DUBLINCORE_NS_URI     = 'http://purl.org/dc/terms/';
var OC_NS_URI = 'http://www.opencastproject.org/matterhorn/';
var CREATE_MODE = 1;
var EDIT_MODE   = 2;

ocSeries.mode = CREATE_MODE;

/*    UI FUNCTIONS    */
ocSeries.init = function(){
  
  $('#addHeader').jqotesubtpl('templates/series.tpl', {});
  //Load i18n strings and replace default english
  // disabled temporarily - see MH-6510
  ocSeries.Internationalize();
  $.ajax({
    url: ANOYMOUS_URL,
    type: 'GET',
    dataType: 'json',
    async: false,
    error: function () {
      if (ocUtils !== undefined) {
        ocUtils.log("Could not retrieve anonymous role " + ANOYMOUS_URL);
      }
    },
    success: function(data) {
      ocSeries.anonymous_role = data.org.anonymousRole;
    }
  });

  
  ocSeries.roles = $.map(ocUtils.ensureArray(ocSecurity.loadRoles().roles.role), function(role) {
      return role.name;
  });
  
  //Add folding action for hidden sections.
  $('.oc-ui-collapsible-widget .form-box-head').click(
    function() {
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-e');
      $(this).children('.ui-icon').toggleClass('ui-icon-triangle-1-s');
      $(this).next().toggle();
      return false;
    });
    
  $('#additionalContentTabs').tabs();
  
  ocSeries.RegisterComponents();
  //ocSeries.FormManager = new ocAdmin.Manager('series', '', ocSeries.components, ocSeries.additionalComponents);
  $('#submitButton').button().click(ocSeries.SubmitForm);
  $('#cancelButton').click(function() {
    document.location = SERIES_LIST_URL;
  });
  
  if(ocUtils.getURLParam('edit') === 'true'){
    ocSeries.mode = EDIT_MODE;
    $('#submitButton').val('Update Series');
    $('#i18n_page_title').text(i18n.page.title.edit);
    document.title = i18n.page.title.edit + " " + i18n.window.title.suffix;
    var seriesId = ocUtils.getURLParam('seriesId');
    if(seriesId !== '') {
      $('#seriesId').val(seriesId);
      $.getJSON(SERIES_SERVICE_URL + "/" + seriesId + ".json", ocSeries.loadSeries);
    }
  }

  var privilegeRow = '<tr>';
  privilegeRow += '<td><input type="text" class="role_search"/></td>';
  privilegeRow += '<td class="privilege_edit"><input type="checkbox" name="priv_view" class="privilege_edit" /></td>';
  privilegeRow += '<td class="privilege_edit"><input type="checkbox" name="priv_edit" class="privilege_edit" /></td>';
  privilegeRow += '<td class="privilege_edit"><img src="/admin/img/icons/delete.png" alt="delete" title="Delete Role"></td>';
  privilegeRow += '</tr>';
  var $row;
  
  var sourceFunction = function(request, response) {
    var matcher = new RegExp( $.ui.autocomplete.escapeRegex(request.term), "i" );
    var matched = $.grep( ocSeries.roles, function(value) {
      return matcher.test(value);
    });
    if($(matched).size() != 0) {
      response(matched);
    } else {
      if(this.element.parent().find('p').size() == 0) {
        this.element.parent().prepend('<p class="no_valid_rule" style="color: red;">Not a valid role</p>');
      }
      response(['No Match']);
    }
  };
  
  var closeFunction = function(event, ui) {
    if($.inArray(ocSeries.roles, $(this).val()) == -1 && $(this).parent().find('p').size() == 0 && event.originalEvent.type != "menuselected") {
      $(this).parent().prepend('<p class="no_valid_rule" style="color: red;">Not a valid role</p>');
    }
  }

  var append = function(event, ui) {
    if(ui.item.value == 'No Match') {
      return false;
    }
    if($(this).attr('id') != "") {
      $row = $(privilegeRow);
      $row.find('[name|="priv_view"]').attr('checked', 'checked');
      $row.find('[name|="priv_view"]').attr('disabled', 'disabled');
      $row.find('[name|="priv_edit"]').attr('disabled', 'disabled');
      $row.find('[name|="priv_analyze"]').attr('disabled', 'disabled');
      $row.find('img').hide();
      $row.find('img').click(removeRole)
      $row.find('.role_search').autocomplete({
        source: sourceFunction,
        select: append,
        close: closeFunction
      });
      $('#rolePrivilegeTable > tbody').append($row);
      var $tr = $(this).parent().parent();
      $tr.children().find('[name|="priv_edit"]').removeAttr('disabled');
      $tr.children().find('[name|="priv_view"]').removeAttr('disabled');
      $tr.children().find('[name|="priv_analyze"]').removeAttr('disabled');
      $tr.children().find('img').show();
    }
    $(this).attr('id', ui.item.value);
    $(this).parent().find('p').remove();
  };

  var removeRole = function() {
    $(this).parent().parent().remove();
  }

  if (ocSeries.mode == CREATE_MODE) {
    $row = $(privilegeRow);
    $row.find('[name|="priv_view"]').attr('checked', 'checked');
    $row.find('[name|="priv_view"]').attr('disabled', 'disabled');
    $row.find('[name|="priv_edit"]').attr('disabled', 'disabled');
    $row.find('[name|="priv_analyze"]').attr('disabled', 'disabled');
    $row.find('img').hide();
    $row.find('img').click(removeRole);

    $row.find('.role_search').autocomplete({
      source: sourceFunction,
      select: append,
      close: closeFunction
    });

    $('#rolePrivilegeTable > tbody').append($row);
    //activate public view by default
    $('#anonymous_view').attr('checked', 'checked');

  } else if (ocSeries.mode == EDIT_MODE) {
    roles = false;
    $.ajax({
      url: SERIES_SERVICE_URL + '/' + seriesId  + '/acl.json',
      type: 'GET',
      dataType: 'json',
      async: false,
      success: function(data) {
        roles = data;
        if(!$.isArray(roles.acl.ace)) {
          roles.acl.ace = [roles.acl.ace];
        }
        $.each(roles.acl.ace, function () {
          if(ocSeries.seriesRolesList[this.role] !== undefined) {
            ocSeries.seriesRolesList[this.role][this.action] = this.allow;
          }
          else {
            ocSeries.seriesRolesList[this.role] = new Object();
            ocSeries.seriesRolesList[this.role][this.action] = this.allow;
          }
        });
      },
      error: function() {
        if (ocUtils !== undefined) {
          ocUtils.log("Could not retrieve roles from " + SERIES_SERVICE_URL + '/' + seriesId  + '/acl.json');
        }
      }
    });
    $.each(ocSeries.seriesRolesList, function(index, value) {
      if(index == ocSeries.anonymous_role) {
        $('#anonymous_view').attr('checked', 'checked');
      }
      else {
        $row = $(privilegeRow);
        $row.find('.role_search').attr('value', index);
        $row.find('.role_search').attr('id', index);
        $row.find('img').click(removeRole)
        $row.find('.role_search').autocomplete({
          source: sourceFunction,
          select: append,
          close: closeFunction
        });
        if(value.write) {
          $row.find('[name|="priv_edit"]').attr('checked', 'checked');
        }
        if(value.read) {
          $row.find('[name|="priv_view"]').attr('checked', 'checked');
        }
        if(value.analyze) {
          $row.find('[name|="priv_analyze"]').attr('checked', 'checked');
        }
        $('#rolePrivilegeTable > tbody').append($row);
      }
    });

    $row = $(privilegeRow);
    $row.find('[name|="priv_view"]').attr('checked', 'checked');
    $row.find('[name|="priv_view"]').attr('disabled', 'disabled');
    $row.find('[name|="priv_edit"]').attr('disabled', 'disabled');
    $row.find('[name|="priv_analyze"]').attr('disabled', 'disabled');
    $row.find('img').hide();
    $row.find('img').click(removeRole)

    $row.find('.role_search').autocomplete({
      source: sourceFunction,
      select: append,
      close: closeFunction
    });
    $('#rolePrivilegeTable > tbody').append($row);
  }
}

ocSeries.Internationalize = function(){
  //Do internationalization of text
  jQuery.i18n.properties({
    name:'series',
    path:'i18n/'
  });
  ocUtils.internationalize(i18n, 'i18n');
  //Handle special cases like the window title.
  document.title = i18n.page.title.add + " " + i18n.window.title.suffix;
}

ocSeries.loadSeries = function(data) {
  data_save = data;
  data = data[DUBLINCORE_NS_URI]
  $("#id").val(data['identifier'][0].value);
  for(var key in data) {
    if(key == "title") {
      ocSeries.seriesTitle = data[key][0].value;
    }
    $('#' + key).attr('value', data[key][0].value);
  }
  data = data_save[OC_NS_URI];
  for(key in data) {
    if($('#' + key).attr('type') == "checkbox") {
      if(data[key][0].value == "true") {
        $('#' + key).attr('checked', 'checked');
      }
    } else {
      $('#' + key).attr('value', data[key][0].value);
    }
  }
}

ocSeries.RegisterComponents = function(){
  //Core Metadata
  ocSeries.additionalComponents.title = new ocAdmin.Component(
    ['title'],
    {
      label:'seriesLabel',
      required:true
    }
    );
  
  ocSeries.additionalComponents.contributor = new ocAdmin.Component(
    ['contributor'],
    {
      label:'contributorLabel'
    }
    );
  
  ocSeries.additionalComponents.creator = new ocAdmin.Component(
    ['creator'],
    {
      label: 'creatorLabel'
    }
    );
  
  //Additional Metadata
  ocSeries.additionalComponents.subject = new ocAdmin.Component(
    ['subject'],
    {
      label: 'subjectLabel'
    }
    )
  
  ocSeries.additionalComponents.language = new ocAdmin.Component(
    ['language'],
    {
      label: 'languageLabel'
    }
    )
  
  ocSeries.additionalComponents.license = new ocAdmin.Component(
    ['license'],
    {
      label: 'licenseLabel'
    }
    )
  
  ocSeries.components.description = new ocAdmin.Component(
    ['description'],
    {
      label: 'descriptionLabel'
    }
    )
  
/*
  //Extended Metadata
  ocAdmin.additionalComponents.type
  //ocAdmin.additionalComponents.subtype
  ocAdmin.additionalComponents.publisher
  ocAdmin.additionalComponents.audience
  //ocAdmin.additionalComponents.duration
  //ocAdmin.additionalComponents.startdate
  //ocAdmin.additionalComponents.enddate
  ocAdmin.additionalComponents.spatial
  ocAdmin.additionalComponents.temporal
  ocAdmin.additionalComponents.rights
   */
}

ocSeries.createDublinCoreDocument = function() {
  dcDoc = ocUtils.createDoc('dublincore', 'http://www.opencastproject.org/xsd/1.0/dublincore/');
  $(dcDoc.documentElement).attr('xmlns:dcterms', 'http://purl.org/dc/terms/');
  $(dcDoc.documentElement).attr('xmlns:dc', 'http://purl.org/dc/elements/1.1/');
  $(dcDoc.documentElement).attr('xmlns:oc', 'http://www.opencastproject.org/matterhorn/');
  $('.dc-metadata-field').each(function() {
    $field = $(this);
    var $newElm = $(dcDoc.createElement('dcterms:' + $field.attr('id')));
    $newElm.text($field.val());
    $(dcDoc.documentElement).append($newElm);
  });
  if($('#id').val() != "") {
    var $newElm = $(dcDoc.createElement('dcterms:identifier'));
    $newElm.text($('#id').val());
    $(dcDoc.documentElement).append($newElm);
  }
  $('.oc-metadata-field').each(function() {
    $field = $(this);
    var $newElm = $(dcDoc.createElement('oc:' + $field.attr('id')));
    if($field.attr('type') == "checkbox") {
      $newElm.text($field.attr('checked') == "checked");
    } else {
      $newElm.text($field.val());
    }
    $(dcDoc.documentElement).append($newElm)
  });
  var out = ocUtils.xmlToString(dcDoc);
  return out;
}

ocSeries.createACLDocument = function() {
  var out = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><acl xmlns="http://org.opencastproject.security">';
  $('.role_search').each(function () {
    var $field = $(this);

    //check whether there is a value and entered value is a valid role
    if ($field.attr('value') != "") {
        out += '<ace>';
        out += '<role>' + $field.attr('value') + '</role>';
        out += '<action>read</action>';
        out += '<allow>' + $field.parent().parent().children().find('[name|="priv_view"]').is(':checked') + '</allow>';
        out += '</ace>';

        out += '<ace>';
        out += '<role>' + $field.attr('value') + '</role>';
        out += '<action>write</action>';
        out += '<allow>' + $field.parent().parent().children().find('[name|="priv_edit"]').is(':checked') + '</allow>';
        out += '</ace>';

        out += '<ace>';
        out += '<role>' + $field.attr('value') + '</role>';
        out += '<action>analyze</action>';
        out += '<allow>' + $field.parent().parent().children().find('[name|="priv_analyze"]').is(':checked') + '</allow>';
        out += '</ace>';
    }
  });

  out += '<ace>';
  out += '<role>' + ocSeries.anonymous_role + '</role>';
  out += '<action>read</action>';
  out += '<allow>' + $('#anonymous_view').is(':checked') + '</allow>';
  out += '</ace>';

  out += '</acl>';
  return out;
}

ocSeries.SubmitForm = function(){
  if(ocSeries.checkFields()) {
    return;
  }
  var dcDoc = ocSeries.createDublinCoreDocument();
  var acl = ocSeries.createACLDocument();
  if(dcDoc && ocSeries.additionalComponents.title.validate()){
    $.ajax({
      type: 'POST',
      url: SERIES_SERVICE_URL + '/',
      data: {
        series: dcDoc,
        acl: acl
      },
      complete: ocSeries.SeriesSubmitComplete
    });
  }
}

ocSeries.checkFields = function() {
  var error = false;
  var title = $.trim($('#title').val())
  if(title == "") {
    error = true;
    $('#item-title').show();
  } else {
    $('#item-title').hide();
  }
  
  if(!error) {
    $.ajax({
      url: SERIES_SERVICE_URL + "/series.json",
      async: false,
      data: {
        seriesTitle: title
      },
      success: function(data) {
        if(data.totalCount != 0) {
          $.each(data.catalogs, function(key, value) {
            if(value[DUBLINCORE_NS_URI].title[0].value == title
              && ocSeries.seriesTitle != title) {
              error = true;
              $('#item-title-existing').show();
            } else {
              $('#item-title-existing').hide();
            }
          });
        }
      }
    });
  }
  ocSeries.showMissingFieldsContainer(error);
  
  return error;
}

ocSeries.showMissingFieldsContainer = function(show) {
  if(show) {
    $('#missingFieldsContainer').show();
  } else {
    $('#missingFieldsContainer').hide();
  }
}

ocSeries.SeriesSubmitComplete = function(xhr, status){
  if(xhr.status == 201 || xhr.status == 204){
    document.location = SERIES_LIST_URL;
  }
/*for(var k in ocSeries.components){
    if(i18n[k]){
      $("#data-" + k).show();
      $("#data-" + k + " > .data-label").text(i18n[k].label + ":");
      $("#data-" + k + " > .data-value").text(ocSeries.components[k].asString());
    }
  }
  $("#schedulerLink").attr('href',$("#schedulerLink").attr('href') + '?seriesId=' + ocSeries.components.seriesId.getValue());
  $("#submissionSuccess").siblings().hide();
  $("#submissionSuccess").show();*/
}
