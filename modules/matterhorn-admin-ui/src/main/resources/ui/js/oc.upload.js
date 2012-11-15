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
var ocUpload = ocUpload || {};

ocUpload.lastParams = '';
ocUpload.metadata = {};
ocUpload.retryId = "";

/** Init the upload page. Init events.
 *
 */
ocUpload.init = function() {

  ocUpload.lastParams = document.location.href.split('?', 2)[1];

  // are we in debug mode?
  if (ocUtils.getURLParam("debug")) {
    $('#console').show();
  }

  // Event: Retry: use already uploaded file clicked
  $('#useFilePrevious').click(function() {
    if ($(this).val()) {
      $('#regularFileChooser').fadeOut('fast');
      $('#regularFileChooserFlavor').fadeOut('fast');
      $('#track').val($('#previousFileLink').attr('href'));
      ocUpload.checkRequiredFields(false);
    }
  });

  // Event: Retry: use replacement file clicked
  $('#useFileReplace').click(function() {
    if ($(this).val()) {
      $('#regularFileChooser').fadeIn('fast');
      $('#regularFileChooserFlavor').fadeIn('fast');
      $('#track').val($('#fileChooserAjax'));
    }
  });

  // Event: File location selector clicked
  $(".file-location").change(function() {
    var location = $(this).val();
    var $container = $(this).parent().next('li').find('iframe');
    $container.attr('src', '../ingest/filechooser-' + location + '.html');
  });

  // Event: Add form filed button clicked
  $('.addFormFieldBtn').click(function() {
    var toClone = $(".creatorRow").children('.formField');
    var clone = $(toClone).clone(true);
    $(clone).removeAttr('id').val('');
    $(this).parent().children('.additionalFieldsContainer').append(clone);   // .insertBefore() yields exception
    var remove = document.createElement("span");
    $(remove).addClass("deleteIcon");
    $(remove).bind('click',function() {
      $(this).prev().remove();
      $(this).remove();
    });
    $(this).parent().children('.additionalFieldsContainer').append(remove);
  });

  $('#containsSlides').change(function() {
    if ($(this).is(':checked')) {
      $('#flavor').val('presentation/source');
    } else {
      $('#flavor').val('presenter/source');
    }
  });

  // Event: Submit button, submit form if no missing inputs
  $('#BtnSubmit').click( function() {
    if (ocUpload.checkRequiredFields(true))  {
      ocUtils.log("Collecting metadata");
      $('.oc-ui-form-field').each( function() {
        if (($(this).attr('id') != 'flavor') && ($(this).attr('id') != 'distribution')) {
          //log("adding metadata " + $(this).attr('id') + ' ' + $(this).val());
          if ($(this).hasClass('multiValueField')) {
            if (ocUpload.metadata[$(this).attr('name')] == undefined) {
              ocUpload.metadata[$(this).attr('name')] = new Array($(this).val());
            } else {
              ocUpload.metadata[$(this).attr('name')].push($(this).val());
            }
          } else {
            if ($(this).attr('id') === 'isPartOf') {
              /*if($('#series').val() !== '' && $('#isPartOf').val() === ''){ //have text and no id
                ocUpload.createSeriesFromSearchText();
              }*/
            }
            ocUpload.metadata[$(this).attr('name')] = $(this).val();
          }
        }
      });
      ocIngest.metadata = ocUpload.metadata;
      ocUpload.showProgressStage();
      ocIngest.createMediaPackage();
    } else {
      $('#containerMissingFields').show('fast');
      $(window).scrollTop(0);
    }
  });

  // Event: collapsable title clicked, de-/collapse collapsables
  $('.collapse-control2').click(function() {
    $('#ui-icon').toggleClass('ui-icon-triangle-1-e');
    $('#ui-icon').toggleClass('ui-icon-triangle-1-s');
    $(this).next('.collapsable').toggle('fast');
  });

  // Event: form field changed, check if data for required fields is missing when a input value changes
  $('.requiredField').change( function() {
    ocUpload.checkRequiredFields();
  });

  // Event: Help icon clicked, display help
  $('.helpIcon').click( function() {
    var help = $(this).prev().attr('id');
    //alert("Displaying help for: " + help);
    ocUpload.showHelpBox(help,$(this).offset().top,$(this).offset().left);
    return false;
  });

  // Event: clicked somewhere
  $('body').click( function() {
    $('#helpBox').fadeOut('fast');
  });
  
  $('#series').autocomplete({
    source: '/series/search',
    select: function(event, ui){
      $('#isPartOf').val(ui.item.id);
    },
    search: function(){
      $('#isPartOf').val('');
    }
  });

  $('#singleUploadRadio').change(function() {
    if ($(this).is(':checked')) {
      $('#multiUploadContainer').hide();
      $('#singleUploadContainer').show();
    }
  });

  $('#multiUploadRadio').change(function() {
    if ($(this).is(':checked')) {
      $('#singleUploadContainer').hide();
      $('#multiUploadContainer').show();
    }
  });
  
  ocWorkflow.init($('#workflowSelector'), $('#workflowConfigContainer'));

  // test if we upload a new recording or want to retry a workflow
  ocUpload.retryId = ocUtils.getURLParam("retry");
  if (ocUpload.retryId != '') {
    $('#i18n_page_title').text("Edit Recording Before Continuing");
    $('#BtnSubmit').text("Continue Processing");
    $('#i18n_submit_instr').css('display','none');
    ocUpload.initRetry(ocUpload.retryId);
  } else {                                             // FIXME well this has to be cleaned up, agile...
    ocUpload.retryId = ocUtils.getURLParam("edit");
    if (ocUpload.retryId != '') {
      $('#i18n_page_title').text("Edit Recording Before Continuing");
      $('#BtnSubmit').text("Continue Processing");
      $('#i18n_submit_instr').css('display','none');
      ocUpload.initRetry(ocUpload.retryId);
    }
  }
}

ocUpload.initRetry = function(wfId) {
  // display current file element / hide file chooser
  $('#retryFile').css('display', 'block');
  $('#regularFileSelection').css('display', 'none');
  $('#regularFileChooser').css('display', 'none');
  $('#regularFileChooserFlavor').css('display', 'none');
  $('#track').val('reingest');
  // get failed Workflow
  $.ajax({
    method: 'GET',
    url: '../workflow/instance/'+ wfId +'.xml',
    dataType: 'xml',
    success: function(data) {
      ocIngest.previousMediaPackage = data;
      var catalogUrl = $(data.documentElement).find("mediapackage > metadata > catalog[type='dublincore/episode'] > url").text();
      ocUpload.loadDublinCore(catalogUrl);
      // previous file
      var files = new Array();
      $(data.documentElement).find("mediapackage > media > track").each(function(index, elm) {
        var type = $(elm).attr('type');
        if (type.split(/\//)[1] == 'source') {
          var filename = $(elm).find('url').text();
          var fileItem = {url: filename, flavor: type};
          ocIngest.previousFiles.push(fileItem);
          //$('#previous-file-url').val(filename);
          filename = filename.split(/\//);
          filename = filename[filename.length-1];
          files.push(filename);
        }
      });
      $('#previousFileList').text(files.join(', '));
      // previous workflow definition
      var defId = $(data.documentElement).find('template').text();
      $('#workflowSelector').val(defId);
      ocWorkflow.definitionSelected(defId, $('#workflowConfigContainer'), function() {
        $(data.documentElement).find("> configurations > configuration").each(function(index, elm) {
          var fieldname = '#' + $(elm).attr('key').replace(/\./g, '\\\\.');
          var field = $(fieldname);
          if (field) {
            if ($(field).is('input[type=checkbox]')) {
              $(field).attr('checked',$(elm).text());
            } else {
              $(field).val($(elm).text());
            }
          }
        });
      });
    }
  });
}

ocUpload.loadDublinCore = function(url) {
  $.ajax({
    method: 'GET',
    url: url,
    dataType: 'xml',
    success: function(data) {
      var url = $(data.documentElement).children().each(function(index, elm) {
        var tagName = elm.tagName.split(/:/)[1];
        if ($('#'+tagName).val() != '') {   // multi value? --> clone the field
          var toClone = $('#'+tagName);
          var clone = $(toClone).clone(true);
          $(clone).removeAttr('id').val('');
          $(toClone).parent().children('.additionalFieldsContainer').append(clone);   // .insertBefore() yields exception
          var remove = document.createElement("span");
          $(remove).addClass("deleteIcon");
          $(remove).bind('click',function() {
            $(this).prev().remove();
            $(this).remove();
          });
          $(toClone).parent().children('.additionalFieldsContainer').append(remove);
          $(clone).val($(elm).text());
        } else {
          $('#'+tagName).val($(elm).text());
        }
      });
    }
  });
}

/** collect data from workflow configuration panel
 *
 */
ocUpload.collectWorkflowConfig = function() {
  var out = {};
  $('.configField').each(function() {
    out[$(this).attr('name')] = $(this).val();
  });
  return out;
}

/** check if data for required fields is missing
 *
 */
ocUpload.checkRequiredFields = function(submit) {
  var missing = false;
  $('.requiredField:visible, .requiredField[type|=hidden]').each( function() {
    if (!$(this).val()) {
      $('#notification' + $(this).attr('id')).show();
      if ((submit) || ($('#containerMissingFields').is(':visible'))) {
        $(this).prev('.scheduler-label').css('color','red');
      }
      if ((submit) && $('#track').val() == '') {
        $('#i18n_upload_file').css('color','red');
      }
      missing = true;
    } else {
      $('#notification' + $(this).attr('id')).hide();
      $(this).prev('.scheduler-label').css('color','black');
      if ($('#track').val() != '') {
        $('#i18n_upload_file').css('color','black');
      }
    }
  });

  if (!missing) {
    $('#containerMissingFields').hide('fast');
  }
  return !missing;
}

ocUpload.showHelpBox = function(help,top,left) {
  $('#helpTitle').text(helpTexts[help][0]);
  $('#helpText').text(helpTexts[help][1]);
  $('#helpBox').css({
    top:top,
    left:left
  }).fadeIn('fast');
}

/** gray-out whole page and display progress popup
 *
 */
ocUpload.showProgressStage = function() {
  //$('#gray-out').fadeIn('fast');
  $('#grayOut').css('display','block');
  $('#progressStage').dialog(
  {
    modal: true,
    width: 450,
    height: 'auto',
    position: ['center', 'center'],
    title: 'Uploading File',
    create: function (event, ui)
    {
      $('.ui-dialog-titlebar-close').hide();
    },
    resizable: false,
    draggable: false,
    disabled: true
  });

}

/** Restore view
 *
 */
ocUpload.hideProgressStage = function() {
  $('#grayOut').hide();
  $('#progressStage').dialog('close');
}

/** Set the progress view to a certain state
 *
 */
ocUpload.setProgress = function(width, text, total, transfered) {
  $('#progressBarIndicator').css('width',width);
  $('#progressBarLabel').text(text);
  $('#labelFilesize').text(total);
  $('#labelBytesTrasfered').text(transfered);
}

/** Load success screen into stage
 *
 */
ocUpload.showSuccessScreen = function() {
  $('#stage').load('complete.html', function() {
    for (key in ocUpload.metadata) {
      if (ocUpload.metadata[key] != "") {
        $('#field-'+key).css('display','block');
        if (ocUpload.metadata[key] instanceof Array) {
          $('#field-'+key).children('.fieldValue').text(ocUpload.metadata[key].join(', '));
        } else {
          $('#field-'+key).children('.fieldValue').text(ocUpload.metadata[key]);
        }
      }
      if (ocUpload.retryId != "") {
        $('#heading-metadata').text('Your recording with the following information has been resubmitted');
      }
    }
    $('#field-filename').children('.fieldValue').text(ocUploadListener.shortFilename);
    
  });
}

/** Load failed screen into stage and display error message
 *
 */
ocUpload.showFailedScreen = function(message) {
  ocUpload.hideProgressStage();
  $('#stage').load('error.html', function() {
    if (message) {
      $('#errorMessage').text(message).show();
    }
    $('#fieldFilename').children('.fieldValue').text(ocUploadListener.shortFilename);
    
  });
}

ocUpload.createSeriesFromSearchText = function(){
  var seriesXml = '<series><additionalMetadata><metadata><key>title</key><value>' + $('#series').val() + '</value></metadata></additionalMetadata></series>';
  var creationSucceeded = false;
  $.ajax({
    async: false,
    type: 'PUT',
    url: '/series',
    data: { series: seriesXml },
    dataType : 'json',
    success: function(data){
      creationSucceeded = true;
      $('#isPartOf').val(data.series.id);
    }
  });
  return creationSucceeded;
}

ocUpload.backToRecordings = function() {
  var url = 'recordings.html';
  if (ocUpload.lastParams !== '') {
    url += '?' + ocUpload.lastParams;
  }
  window.location.href = url;
}
