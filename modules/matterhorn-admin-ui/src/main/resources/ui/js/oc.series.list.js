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
var ocSeriesList = ocSeriesList || {};
ocSeriesList.views = ocSeriesList.views || {};
ocSeriesList.views.seriesView = ocSeriesList.seriesView || {};

ocSeriesList.Configuration = new (function(){
  //default configuration
  this.count = $.cookie('series_count') == null ? 10 : $.cookie('series_count');
  this.total = 10;
  this.startPage = 0;  
  this.lastPage = 0;
  this.sort = 'TITLE';  
  this.edit = 'true';
});

ocSeriesList.SortColumns = [
{
  0: 'TITLE', 
  1: 'TITLE_DESC'
},
{
  0: 'CREATOR', 
  1: 'CREATOR_DESC'
},
{
  0: 'CONTRIBUTOR', 
  1: 'CONTRIBUTOR_DESC'
}
];

ocSeriesList.init = function(){
  ocSeriesList.askForSeries();
  $('#addHeader').jqotesubtpl('templates/series_list-header.tpl', {});
  if ($.cookie('series_count') != null) {
    $('#pageSize').children()
    .each(function() {
      this.selected = (this.text == $.cookie('series_count'));
    });
  }
  $('#pageSize').change(function(event, ui)
  {
    ocSeriesList.Configuration.startPage = 0;
    ocSeriesList.Configuration.count = parseInt(event.target.value);
    $.cookie('series_count', ocSeriesList.Configuration.count);
    ocSeriesList.askForSeries();
  })
  
  ocSeriesList.askForSeries();
  
  $("#addSeriesButton").button({
    icons:{
      primary:"ui-icon-circle-plus"
    }
  });
}

ocSeriesList.askForSeries = function()
{
  $.ajax({
    url : "/series/series.json?"+ocSeriesList.buildURLparams(),
    type: "GET",
    success: function(data)
    {
      ocSeriesList.buildSeriesView(data);
      ocSeriesList.Configuration.total = parseInt(data.totalCount);
      ocSeriesList.Configuration.lastPage = Math.ceil(ocSeriesList.Configuration.total / ocSeriesList.Configuration.count);
      if(ocSeriesList.Configuration.total <= ocSeriesList.Configuration.count){
        $('#prevText').show();
        $('#prevButtons').hide();

        $('#nextText').show();
        $('#nextButtons').hide();
      } else if(ocSeriesList.Configuration.startPage == 0) {
        $('#prevText').show();
        $('#prevButtons').hide();

        $('#nextText').hide();
        $('#nextButtons').show();
      }else if(ocSeriesList.Configuration.startPage + 1 == ocSeriesList.Configuration.lastPage) {
        $('#nextText').show();
        $('#nextButtons').hide();

        $('#prevText').hide();
        $('#prevButtons').show();
      } else {
        $('#prevText').hide();
        $('#prevButtons').show();

        $('#nextText').hide();
        $('#nextButtons').show();  
      }
      $('#curPage').text(ocSeriesList.Configuration.startPage + 1);
      $('#numPage').text(ocSeriesList.Configuration.lastPage);
      //
      updateSortIcons();
    }
  });

  function updateSortIcons() {
    // find the index of the sort column
    var sortColumn = _.chain(ocSeriesList.SortColumns).map(function (a) {
      return _.chain(a).values().find(function (v) {
        return v == ocSeriesList.Configuration.sort;
      }).value() != undefined
    }).indexOf(true).value();
    // find the sort icon and remove all triangle related styles
    var sortIcon = $("#seriesTable th:eq(" + sortColumn + ")").find(".sort-icon")
            .removeClass("ui-icon-triangle-2-n-s")
            .removeClass("ui-icon-circle-triangle-n")
            .removeClass("ui-icon-circle-triangle-s");
    // set the correct triangle style
    if (ocSeriesList.Configuration.sort.indexOf("_DESC") > 0) {
      sortIcon.addClass("ui-icon-circle-triangle-s")
    } else {
      sortIcon.addClass("ui-icon-circle-triangle-n")
    }
  }
}

ocSeriesList.previousPage = function(){
  if(ocSeriesList.Configuration.startPage > 0) {
    ocSeriesList.Configuration.startPage--;
    ocSeriesList.askForSeries();
  }
}

ocSeriesList.nextPage = function(){
  numPages = Math.floor(ocSeriesList.Configuration.total / ocSeriesList.Configuration.count);
  if( ocSeriesList.Configuration.startPage < numPages ) {
    ocSeriesList.Configuration.startPage++;
    ocSeriesList.askForSeries();
  }
}

ocSeriesList.buildURLparams = function() {
  var pa = [];
  for (p in ocSeriesList.Configuration) {
    if (ocSeriesList.Configuration[p] != null) {	
      pa.push(p + '=' + escape(this.Configuration[p]));
    }
  }
  return pa.join('&');
}
 

ocSeriesList.buildSeriesView = function(data) {
  var PURL = "http://purl.org/dc/terms/";
  var sorting;
  ocSeriesList.views = {};
  ocSeriesList.views.seriesView = {};
  for(var i = 0; i < data.catalogs.length; i++) {
    var s = ocSeriesList.views.seriesView[data.catalogs[i][PURL]['identifier'][0].value] = {};
    s.id = data.catalogs[i][PURL]['identifier'][0].value;
    for(var key in data.catalogs[i][PURL]) {
      if(key === 'title'){
        s.title = data.catalogs[i][PURL][key][0].value
      } else if(key === 'creator') {
        s.creator = data.catalogs[i][PURL][key][0].value
      } else if(key  === 'contributor') {
        s.contributor = data.catalogs[i][PURL][key][0].value
      }
    }
  }
  if($.cookie('column') == null) 
  {
    $.cookie('column', 0)
  }
  if($.cookie('direction') == null) 
  {
    $.cookie('direction', 0) //standard is ASC
  }
  ocSeriesList.views.totalCount = ocSeriesList.Configuration.total;
  $('#seriesTableContainer').jqotesubtpl("templates/series_list-table.tpl", ocSeriesList.views);
  $('#seriesTable th').click(function(){
    var index = $(this).parent().children().index($(this));
    if(index != 3)
    {
      if(index == $.cookie('column'))
      {
        $.cookie('direction', $.cookie('direction') == 0 ? 1 : 0);
      }
      $.cookie('column', index)
      ocSeriesList.Configuration.sort = ocSeriesList.SortColumns[$.cookie('column')][$.cookie('direction')];
      ocSeriesList.askForSeries();
    }
  });
}

ocSeriesList.deleteSeries = function(seriesId, title) {
  if(confirm('Are you sure you want to delete the series "' + title + '"?')){
    $.ajax({
      type: 'DELETE',
      url: '/series/' + seriesId,
      error: function(XHR,status,e){
        alert('Could not remove series "' + title + '"');
      },
      success: function(data) {
        location.reload();
      }
    });
  }
}
