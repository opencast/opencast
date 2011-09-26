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
ocSeriesList = {} || ocSeriesList;
ocSeriesList.views = {} || ocSeriesList.views;
ocSeriesList.views.seriesView = {} || ocSeriesList.seriesView;

ocSeriesList.Configuration = new (function(){
  //default configuration
  this.count = 10;
  this.total = 10;
  this.startPage = 0;  
  this.lastPage = 0;
  this.sort = 'TITLE_ASC';  
});

ocSeriesList.init = function(){
  $('#addHeader').jqotesubtpl('templates/series_list-header.tpl', {});
  
  $.ajax({
    url: "/series/series.json?edit=true",
    type: "GET",
    success: function(data)
    {
      ocSeriesList.buildSeriesView(data);
    }
  });
  
  $("#addSeriesButton").button({
    icons:{
      primary:"ui-icon-circle-plus"
    }
  });
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
  sorting = [[$.cookie('column'), $.cookie('direction')]];
  $('#seriesTableContainer').jqotesubtpl("templates/series_list-table.tpl", ocSeriesList.views);
  $('#seriesTable').tablesorter({
    cssHeader: 'oc-ui-sortable',
    cssAsc: 'oc-ui-sortable-Ascending',
    cssDesc: 'oc-ui-sortable-Descending' ,
    headers: {
      3: {
        sorter: false
      }
    },
    sortList: sorting
  });
  $('#seriesTable th').click(function(){
    var index = $(this).parent().children().index($(this));
    if(index != 3)
    {
      if(index == $.cookie('column'))
      {
          $.cookie('direction', $.cookie('direction') == 0 ? 1 : 0);
      }
      $.cookie('column', index)
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
