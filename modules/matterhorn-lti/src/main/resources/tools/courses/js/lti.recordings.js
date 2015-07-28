/**
 *  Copyright 2009-2011 The Regents of the University of California
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

$(function(){
    var courseID = $.getURLParameter("sid"),
        limit = 100,
	lti_player="",
	extra_params="",
        url = "/search/episode.json?sid=" + courseID + "&limit=" + limit;

    $.ajax({
	    url: "/info/me.json",
		dataType: "json",
		async: false,
		success: function(data) {
		var props = data.org.properties;
		if(props){
		    lti_player = props["lti.player.url"] ? props["lti.player.url"].trim() : "../player/index.html?id=";
		    extra_params = props["lti.player.extra_params"] ? props["lti.player.extra_params"].trim() : "";
		}
	    }
    });

    $.ajax(
        {
        url: url,
        dataType: 'json',
        success: function(data)
        {
            var media = data["search-results"] ? data["search-results"]["result"] : [],
            recent_media = [],
            previous_media = [],
            message = "Sorry no broadcasts present",
            courseName = "",
            sorted_media;

            //Get and Set the Course Title
            if(courseID && media){
                courseName = media.length > 0 ? media[0]["mediapackage"].seriestitle : media["mediapackage"].seriestitle;
                $(".lti-oc-title h2").text(courseName + " Recordings");
            }

            var build_recent_content = function(media, klass){
                var media_title,
                media_presenter,
                media_date,
                media_duration,
                image_attachments,
                image_markup,
                title_markup,
                presenter_markup,
                duration_markup,
                media_id,
                watch_media_url,
                formatted_duration;

                $(klass).append('<div><ul></ul></div>');
                $(klass + '> div').attr('class', 'wrapper');

                for(var i = 0; i < media.length ; i+=1){
                    media_id = media[i].id;
                    media_title = media[i].dcTitle;
                    media_presenter = media[i].dcCreator;
                    media_presenter = media[i].dcCreator ? media[i].dcCreator : 'ANONYMOUS';
                    media_duration = media[i]["mediapackage"].duration;
                    media_date = media[i].dcCreated;
                    media_date = media_date ? $.dateStringToDate(media_date).toLocaleDateString() : "";
                    image_attachments = media[i]["mediapackage"]["attachments"]["attachment"];

                    thumbnail_url = getThumbnailURL( image_attachments );
                    watch_media_url = lti_player+media_id+extra_params;
                    formatted_duration = Opencast.Date_Helper.formatSeconds(media_duration/1000);
                    duration_markup = "<span class='duration'>"+ formatted_duration +"</span><br>";
                    image_markup = "<a class='preview' href="+watch_media_url+"><span class='preview_image'><img src="+thumbnail_url+">"+duration_markup+"</span><span class='preview_overlay'><img src='../shared/img/icons/play_icon.png'></span></a><br>";
                    title_markup = "<a href="+watch_media_url+">"+media_title+'</a><br>';
                    presenter_markup = '<span>by '+ media_presenter+'</span><br>';
                    date_markup = '<span>'+media_date+'</span><br>';
                    $(klass +'> div > ul')
                    .append('<li>'+image_markup+title_markup+presenter_markup+date_markup+'</li>');
                }
            };

            var build_previous_content = function(media, klass){
                var media_title,
                media_presenter,
                media_date,
                media_duration,
                image_attachments,
                image_markup,
                title_markup,
                presenter_markup,
                duration_markup,
                media_id,
                watch_media_url,
                totalImages,
                imageWidth,
                totalWidth,
                formatted_duration;

                $(klass).append("<button id='gallery-prev'><img src='../shared/img/icons/prev.png'></button><div><ul></ul></div>");
                $(klass + '> div').attr('id', 'gallery-wrap');
                $(klass + '> div > ul').attr('id', 'gallery');

                for(var i = 0; i < media.length ; i+=1){
                    media_id = media[i].id;
                    media_title = media[i].dcTitle;
                    media_presenter = media[i].dcCreator ? media[i].dcCreator : 'ANONYMOUS';
                    media_date = media[i].dcCreated;
                    media_date = media_date ? $.dateStringToDate(media_date).toLocaleDateString() : "";
                    media_duration = media[i]["mediapackage"].duration;
                    image_attachments = media[i]["mediapackage"]["attachments"]["attachment"];

                    thumbnail_url = getThumbnailURL( image_attachments );
                    watch_media_url = lti_player+media_id+extra_params;
                    formatted_duration = Opencast.Date_Helper.formatSeconds(media_duration/1000);
                    duration_markup = "<span class='duration'>"+ formatted_duration +"</span><br>";
                    image_markup = "<a class='preview' href="+watch_media_url+"><span class='preview_image'><img src="+thumbnail_url+">"+duration_markup+"</span><span class='preview_overlay'><img src='../shared/img/icons/play_icon.png'></span></a><br>";
                    title_markup = "<a href="+watch_media_url+">"+media_title+'</a><br>';
                    presenter_markup = '<span>by '+ media_presenter+'</span><br>';
                    date_markup = '<span>'+media_date+'</span><br>';
                    $(klass +'> div > ul')
                    .append('<li>'+image_markup+title_markup+presenter_markup+date_markup+'</li>');
                }

                $(klass).append("<button id='gallery-next'><img src='../shared/img/icons/next.png'></button>");
                $(klass).append("<div class='clear'>");

                // Set the width for the gallery
                totalImages = $("#gallery > li").length;
                imageWidth = $("#gallery > li:first").outerWidth(true);
                totalWidth = imageWidth * totalImages;
                $("#gallery").width(totalWidth);

                // Disable the navigation buttons if less than 4 images exist in slider
                if(totalImages <= 3){
                    $("#gallery-prev").attr("disabled","disabled");
                    $("#gallery-prev > img").css("opacity", 0.4);
                    $("#gallery-next").attr("disabled","disabled");
                    $("#gallery-next > img").css("opacity", 0.4);
                }else{
                    $("#gallery-prev").attr("disabled","disabled");
                    $("#gallery-prev > img").css("opacity", 0.4);
                }
            };

            var sort_by_date = function(my_media){
                my_media.sort(function(a, b){
                    var dateA = (typeof a.dcCreated === "string") ? a.dcCreated : "",
                    dateB = (typeof b.dcCreated === "string") ? b.dcCreated : "";

                    if (dateA < dateB) return 1;
                    if (dateA > dateB) return -1;
                    return 0;
                });
                return my_media;
            };

            var getThumbnailURL = function(image_attachments){
                var search_image_url,
                    presenter_present = false,
                    presentation_present = false;

		if (image_attachments) {
		    // Check for available media
		    for(var j = 0; j < image_attachments.length ; j+=1){
			if(image_attachments[j].type ===  "presenter/search+preview"){
			    presenter_present = true;
			}
			if(image_attachments[j].type ===  "presentation/search+preview"){
			    presentation_present = true;
			}
		    }

		    //Get the thumbnail
		    for(var j = 0; j < image_attachments.length ; j+=1){
			if(presenter_present === true && image_attachments[j].type ===  "presenter/search+preview"){
			    //get the presenter thumbnail
			    search_image_url = image_attachments[j].url;
			    search_image_url = search_image_url.slice(search_image_url.indexOf("/static"));
			    break;
			}
			if(presenter_present === false && presentation_present === true && image_attachments[j].type ===  "presentation/search+preview"){
			    //get the presentation thumbnail
			    search_image_url = image_attachments[j].url;
			    search_image_url = search_image_url.slice(search_image_url.indexOf("/static"));
			    break;
			}
		    }
		}
                //If you do not find a thumbnail -  show the default one
                search_image_url = search_image_url ? search_image_url :  "/ltitools/shared/img/audio.png";
                return search_image_url;
            };

            if(media){
                if (!media[0]){
                    recent_media.push(media);
                }
                else
                    {
                        // Sort the media
                        sorted_media = sort_by_date(media);
                        // Separate recent and not so recent broadcasts
                        for( var i = 0; i < sorted_media.length; i+=1 ){
                            i < 3 ? recent_media.push(sorted_media[i]) : previous_media.push(sorted_media[i]);
                        }
                    }
            }
            recent_media.length > 0 ? build_recent_content(recent_media, 'div.lti-oc-recent') : $('div.lti-oc-recent').append(message);
            previous_media.length > 0 ? build_previous_content(previous_media, 'div.lti-oc-slider-holder') : $('div.lti-oc-slider-holder').append(message);
        }
    });
});

