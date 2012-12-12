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
var ocUtils = ocUtils || {};

ocUtils.templateRoot = "jst/";

ocUtils.formatInt = function(value){
  var groupingSeparator= ',',       
  formatted = '';

  if (typeof value == 'number') value += '';	
  var count=0, i=value.length;  
  while (i--) {
    if (count !== 0 && count % 3 === 0) {
      formatted = groupingSeparator + formatted;    
    }
    formatted = value.substr(i, 1) + formatted;
    count++;
  }    
  return formatted;
}

ocUtils.internationalize = function(obj, prefix){
  for(var i in obj){
    if(typeof obj[i] == 'object'){
      ocUtils.internationalize(obj[i], prefix + '_' + i);
    }else if(typeof obj[i] == 'string'){
      var id = '#' + prefix + '_' + i;
      if($(id).length){
        $(id).text(obj[i]);
      }
    }
  }
}

ocUtils.getURLParam = function(name, url) {
  if(url == undefined) {
    url = window.location.href;
  }
  name = name.replace(/[\[]/,"\\\[").replace(/[\]]/,"\\\]");
  var regexS = "[\\?&]"+name+"=([^&#]*)";
  var regex = new RegExp( regexS );
  var results = regex.exec( url );
  if( results == null ) {
    return "";
  } else {
    return results[1];
  }
}

ocUtils.xmlToString = function(doc) {
  if(typeof XMLSerializer != 'undefined'){
    try {
      return (new XMLSerializer()).serializeToString(doc);
    } catch (e) { // IE9 supports XMLSerializer but causes an exception on some DOM types
      if(doc.xml) return doc.xml;
      else return '';
    }
  } else if(doc.xml) {
    return doc.xml;
  } else {
    return '';
  }
}

ocUtils.isChunkedUploadCompliable = function() {
  return window.File && window.FileReader && window.FileList && window.Blob && window.FormData != undefined;
}

ocUtils.createDoc = function(rootEl, rootNS){
  var doc = null;
  //Create a DOM Document, methods vary between browsers, e.g. IE and Firefox
  if(document.implementation && document.implementation.createDocument && !(navigator.userAgent.match(/MSIE\s(?!9.0)/))){ //Firefox, Opera, Safari, Chrome, etc.
    doc = document.implementation.createDocument(rootNS, rootEl, null);
  } else { // IE must use an XML specific doc even though IE9 supports createDocument
    doc = new ActiveXObject('MSXML2.DOMDocument');
    doc.loadXML('<' + rootEl + ' xmlns="' + rootNS + '"></' + rootEl + '>');
  }
  return doc;
}

ocUtils.toICalDate = function(d){
  if(d.constructor !== Date){
    d = new Date(0);
  }
  var month = UI.padstring(d.getUTCMonth() + 1, '0', 2);
  var hours = UI.padstring(d.getUTCHours(), '0', 2);
  var minutes = UI.padstring(d.getUTCMinutes(), '0', 2);
  var seconds = UI.padstring(d.getUTCSeconds(), '0', 2);
  return '' + d.getUTCFullYear() + month + d.getUTCDate() + 'T' + hours + minutes + seconds + 'Z';
}

/** convert timestamp to locale date string
 * @param timestamp
 * @return Strng localized String representation of timestamp
 */
ocUtils.makeLocaleDateString = function(timestamp) {
  var date = new Date();
  date.setTime(timestamp);
  return date.toLocaleString();
}

/** converts a date to a human readable date string
 *  @param date
 *  @param compact -- (boolean) without day name
 *  @return formatted date string
 */
ocUtils.getDateString = function(date, compact) {
  var days = [ "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" ];
  var months = [ "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" ];
  var daySeparator = ", ";
  var dateSeparator = " ";
  var yearSeparator = " ";
  var d = date;
  var datestring = "";
  if (compact == undefined || !compact) {
    datestring += days[d.getDay()];
    datestring += daySeparator;
  }
  datestring += months[d.getMonth() % 12];
  datestring += dateSeparator;
  datestring += (d.getDate() >= 10) ? d.getDate() : "0".concat(d.getDate());
  datestring += yearSeparator;
  datestring += d.getFullYear();
  return datestring;
}

/** converts a date to a human readable time string
 * @param date
 * @param withSeconds -- boolean
 * @return formatted time string
 */
ocUtils.getTimeString = function(date, withSeconds) {
  var timeSeparator = ":";
  var d = date;
  var h = (d.getHours() >= 10) ? d.getHours() : "0".concat(d.getHours());
  var m = (d.getMinutes() >= 10) ? d.getMinutes() : "0"
  .concat(d.getMinutes());
  var s = (d.getSeconds() >= 10) ? d.getSeconds() : "0"
  .concat(d.getSeconds());
  return (h + timeSeparator + m + (withSeconds ? timeSeparator + s : ""));
}

/** Converts a date to a human readable date and time string.
 *  @param date
 *  @param withSeconds -- boolean
 *  @return formatted time string
 */
ocUtils.getDateTimeStringCompact = function(date, withSeconds) {
  return ocUtils.getDateString(date, true) + " " + ocUtils.getTimeString(date, withSeconds);
}

ocUtils.fromUTCDateString = function(UTCDate) {
  var date = new Date(0);
  if(UTCDate[UTCDate.length - 1] == 'Z') {
    var dateTime = UTCDate.slice(0,-1).split("T");
    var ymd = dateTime[0].split("-");
    var hms = dateTime[1].split(":");
    date.setUTCFullYear(parseInt(ymd[0], 10));
    date.setUTCMonth(parseInt(ymd[1], 10) - 1);
    date.setUTCMilliseconds(0);
    date.setUTCSeconds(parseInt(hms[2], 10));
    date.setUTCMinutes(parseInt(hms[1], 10));
    date.setUTCHours(parseInt(hms[0], 10));
    date.setUTCDate(parseInt(ymd[2], 10));
  }
  return date;
}

/** converts a date to a human readable date and time string
 * @description Returns formatted Seconds
 * @param seconds Seconds to format
 * @return formatted Seconds
 */
ocUtils.formatSeconds = function(seconds) 
{
	var result="";
	seconds = Math.round(seconds)
	var hours = Math.floor(seconds/3600);
	seconds -= hours*3600;
	var minutes = Math.floor(seconds/60);
	seconds -= minutes *60;
	
	result += (hours < 10) ? "0" + hours : hours;
	result += ":";
	
	result += (minutes < 10) ? "0" + minutes : minutes;
	result += ":";
	
	result += (seconds < 10) ? "0" + seconds : seconds;
	
	return result;
}

/** converts a duration in ms to a human readable duration string
 * @param duration duration in ms
 * @return formatted duration string, '' is duration is null or < 0
 */
ocUtils.getDuration = function(duration) {
  var durationSeparator = "<br />Duration: ";
  if((duration !== null) && (duration >= 0)) {
    return durationSeparator + ocUtils.formatSeconds(duration / 1000);
  } else {
    return '';
  }
}

/** converts a date to a human readable date and time string
 * @param UTCDate
 * @return formatted date and time string
 */
ocUtils.fromUTCDateStringToFormattedTime = function(UTCDate, duration) {
  var dateTimeSeparator = " - ";
  var date = ocUtils.fromUTCDateString(UTCDate);
  return duration!=null ? (ocUtils.getDateString(date) + dateTimeSeparator + ocUtils.getTimeString(date) + ocUtils.getDuration(duration)) : (ocUtils.getDateString(date) + dateTimeSeparator + ocUtils.getTimeString(date)) ;
}

/* Convert Date object to yyyy-MM-dd'T'HH:mm:ss'Z' string.
 *
 */
ocUtils.toISODate = function(date, utc) {
  //align date format
  var date = new Date(date);
  var out;
  if(typeof utc == 'undefined') {
    utc = true;
  }
  if(utc) {
    out = date.getUTCFullYear() + '-' + 
    ocUtils.padString((date.getUTCMonth()+1) ,'0' , 2) + '-' +
    ocUtils.padString(date.getUTCDate() ,'0' , 2) + 'T' +
    ocUtils.padString(date.getUTCHours() ,'0' , 2) + ':' +
    ocUtils.padString(date.getUTCMinutes() ,'0' , 2) + ':' +
    ocUtils.padString(date.getUTCSeconds() ,'0' , 2) + 'Z';
  } else {
    out = date.getFullYear() + '-' + 
    ocUtils.padString((date.getMonth()+1) ,'0' , 2) + '-' +
    ocUtils.padString(date.getDate() ,'0' , 2) + 'T' +
    ocUtils.padString(date.getHours() ,'0' , 2) + ':' +
    ocUtils.padString(date.getMinutes() ,'0' , 2) + ':' +
    ocUtils.padString(date.getSeconds() ,'0' , 2);
  }
  return out;
}

ocUtils.padString = function(str, pad, padlen){
  if(typeof str !== 'string'){ 
    str = str.toString();
  }
  while(str.length < padlen && pad.length > 0){
    str = pad + str;
  }
  return str;
}

ocUtils.log = function(){
  if(window.console){
    try{
      window.console && console.log.apply(console,Array.prototype.slice.call(arguments));
    }catch(e){
      console.log(e);
    }
  }
}

/** loads and prepare a JST template
 *  @param name of tempalte
 *  @param callback 
 */
ocUtils.getTemplate = function(name, callback) {
  var reqUrl = ocUtils.templateRoot + name + '.jst';
  $.ajax( {
    url : reqUrl,
    type : 'get',
    dataType : 'text',
    error : function(xhr) {
      ocUtils.log('Error: Could not get template ' + name + ' from ' + reqUrl);
      ocUtils.log(xhr.status + ' ' + xhr.responseText);
    },
    success : function(data) {
      var template = TrimPath.parseTemplate(data);
      callback(template);
    }
  });
}

/** If obj is an array just returns obj else returns Array with obj as content.
 *  If obj === undefined returns empty Array.
 *
 */
ocUtils.ensureArray = function(obj) {
  if (obj === undefined) return [];
  if ($.isArray(obj)) {
    return obj;
  } else {
    return [obj];
  }
}

ocUtils.sizeOf = function(obj) {
  var length = 0;
  if (obj === undefined) {
    return 0;
  }
  if ($.isArray(obj) || typeof obj == 'string') {
    return obj.length;
  } else if (typeof obj == 'object') {
    for (i in obj) {
      length++;
    }
  }
  return length;
}

ocUtils.exists = function(obj) {
  if(typeof obj !== 'undefined') {
    return true;
  }
  return false;
};

ocUtils.contains = function(array, value) {
  return $.inArray(value, array) >= 0;
}

ocUtils.getDCJSONParam = function(dcJSON, param, namespace) {
  namespace = 'http://purl.org/dc/terms/' || namespace;
  for (var i in dcJSON) {
    var metadata = dcJSON[i];
    if (i === namespace) {
      for (var j in metadata) {
        if (j === param) {
          return metadata[param][0].value;
        }
      }
    }
  }
  return false;
}

/** Join all elements of an array with separator string "sep".
 *  @param as -- an array, a single value or "undefined"
 *  @param sep -- the separator string
 *  @return the array elements joined or "" if as is undefined
 */
ocUtils.joinArray = function(as, sep) {
  if (as !== undefined) {
    return ocUtils.ensureArray(as).join(sep);
  } else {
    return "";
  }
}

/** Return the first argument that is neither undefined nor null.
 *  @param a variable list of arguments
 */
ocUtils.dflt = function() {
  // arguments is an object _not_ an array so let's turn it into one
  return _.detect(_.toArray(arguments).concat(""), function(a) {
    return typeof a !== "undefined" && a != null;
  });
}

/** Return the first element of array a. If a is not an array return a. Return undefined if the array is empty.
 */
ocUtils.first = function(a) {
  return ocUtils.ensureArray(a)[0];
}

/** Return the last element of array a. If a is not an array return a. Return undefined if the array is empty.
 */
ocUtils.last = function(a) {
  var aa = ocUtils.ensureArray(a);
  return aa[aa.length - 1];
}
