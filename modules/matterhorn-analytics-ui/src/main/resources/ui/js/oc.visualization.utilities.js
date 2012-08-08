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

/** Adds a leading 0 to numbers that are less than ten. Used by getTime. **/
function addLeadingZeroes(number) {	
	if(number < 10) {
		return "0" + number;
	} 
	else {
		return number;
	}		
}
/** Replaces a variable in a string like {seriesID} with a dictionary entry [{ seriesID: "My Series ID"}]
str is the string to replace the variables in.
sub is the dictionary. 
**/
substitute = function(str, sub) {
	return str.replace(/\{(.+?)\}/g, function($0, $1) {
	    return $1 in sub ? sub[$1] : $0;
	});
};

//The number of seconds in a single week. 
var weekInSeconds = 604800;
var dayInSeconds = 86400;
var hourInSeconds = 3600;

/** Get a string representing a time in the format YYYYMMhhmm or 201212312359 **/
function getTime(date) {
	var fullYear = date.getFullYear();
	var month = addLeadingZeroes(date.getMonth() + 1);
	var day = addLeadingZeroes(date.getDate());
	var hours = addLeadingZeroes(date.getHours());
	var minutes = addLeadingZeroes(date.getMinutes());
	
	return "" + fullYear + month + day + hours + minutes;
}

function secondsToTime(secs)
{
	var hours = Math.floor(secs / (60 * 60));
		var hourString = hours + "hrs";
	if(hours < 10) {
		hourString = "0" + hours + "hrs";
	}

	var divisor_for_minutes = secs % (60 * 60);
	var minutes = Math.floor(divisor_for_minutes / 60);
	var minuteString = minutes + "mins";
	if(minutes < 10) {
		minuteString = "0" + minutes + "mins";
	}		

	var divisor_for_seconds = divisor_for_minutes % 60;
	var seconds = Math.ceil(divisor_for_seconds);
	var secondString = seconds + "secs";
	if(seconds < 10) {
		secondString = "0" + seconds + "secs";
	}   		

	var obj = {
		"h": hourString,
		"m": minuteString,
		"s": secondString
	};
	return obj;
}

function secondsToShortTime(secs)
{
	var hours = Math.floor(secs / (60 * 60));
		var hourString = hours;
	if(hours < 10) {
		hourString = "0" + hours;
	}

	var divisor_for_minutes = secs % (60 * 60);
	var minutes = Math.floor(divisor_for_minutes / 60);
	var minuteString = minutes;
	if(minutes < 10) {
		minuteString = "0" + minutes;
	}		

	var divisor_for_seconds = divisor_for_minutes % 60;
	var seconds = Math.ceil(divisor_for_seconds);
	var secondString = seconds;
	if(seconds < 10) {
		secondString = "0" + seconds;
	}   		

	var obj = {
		"h": hourString,
		"m": minuteString,
		"s": secondString
	};
	return obj;
}