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
 
var Opencast = Opencast || {};

/**
 * @namespace the global Opencast namespace segments_text
 */
Opencast.segments_text = (function ()
{
    var mediaPackageId;
    var staticBool_hide = true,
    SEGMENTS_TEXT = "Segment Text",
    SEGMENTS_TEXT_HIDE = "Hide Segment Text",
    isOpening = false,
    isOpen = false;
        
    /**
     * @memberOf Opencast.segments_text
     * @description Initializes the Segments Text Tab
     */
    function initialize()
    {
	var reg = Opencast.Plugin_Controller.registerPlugin(Opencast.segments_text);
	$.log("Opencast.segments_text registered: " + reg);
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Shows the Segments Text Tab
     */
    function show()
    {
	if(!isOpen && !isOpening)
	{
	    isOpening = true;
            Opencast.Player.addEvent(Opencast.logging.SHOW_TEXT_SEGMENTS);
            // Hide other Tabs
	    Opencast.Plugin_Controller.hideAll(Opencast.segments_text);
            // Change Tab Caption
            $('#oc_btn-slidetext').attr(
		{
		    title: SEGMENTS_TEXT_HIDE
		});
            $('#oc_btn-slidetext').html(SEGMENTS_TEXT_HIDE);
            $("#oc_btn-slidetext").attr('aria-pressed', 'true');
			$("#oc_btn-slidetext").parent().addClass('ui-tabs-selected').addClass('ui-state-active');
            // Show a loading Image
            $('#oc_slidetext').show();
            $('#segments_text-loading').show();
            $('#oc-segments_text').hide();
            $('.oc-segments-preview').css('display', 'block');
            // If cashed data are available
            if (Opencast.segments_text_Plugin.createSegmentsTextFromCashe())
            {
		$.log("Cashing segments text plugin: yes");
		// Make visible
		$('#oc_slidetext').show();
		$('#segments_text-loading').hide();
		$('#oc-segments_text').show();
		$('.oc-segments-preview').css('display', 'block');
		isOpening = false;
		isOpen = true;
            }
            else
            {
		$.log("Cashing segments text plugin: no");
		// Request JSONP data
		$.ajax(
		    {
			url: Opencast.Watch.getSegmentsTextURL(),
			data: 'id=' + mediaPackageId,
			dataType: 'jsonp',
			jsonp: 'jsonp',
			success: function (data)
			{
			    $.log("Segments Text AJAX call: Requesting data succeeded");
			    if ((data === undefined) || (data['search-results'] === undefined) || (data['search-results'].result === undefined) || (data['search-results'].result.segments === undefined))
			    {
				$.log("Segments Text AJAX call: Data not available");
				isOpening = false;
				isOpen = false;
			    } else
			    {
				$.log("Segments Text AJAX call: Data available");
				data['search-results'].result.segments.currentTime = $.getTimeInMilliseconds(Opencast.Player.getCurrentTime());
				// Set Duration until this Segment ends
				var completeDuration = 0;
				$.each(data['search-results'].result.segments.segment, function (i, value)
				       {
					   // Set a Duration until the Beginning of this Segment
					   data['search-results'].result.segments.segment[i].durationExcludingSegment = completeDuration;
					   completeDuration += parseInt(data['search-results'].result.segments.segment[i].duration);
					   // Set a Duration until the End of this Segment
					   data['search-results'].result.segments.segment[i].durationIncludingSegment = completeDuration;
				       });
				// Create Trimpath Template
				Opencast.segments_text_Plugin.addAsPlugin($('#oc-segments_text'), data['search-results'].result.segments);
				// Make visible
				$('#oc_slidetext').show();
				$('#segments_text-loading').hide();
				$('#oc-segments_text').show();
				$('.oc-segments-preview').css('display', 'block');
				isOpening = false;
				isOpen = true;
			    }
			},
			// If no data comes back
			error: function (xhr, ajaxOptions, thrownError)
			{
			    $.log("Segments Text Ajax call: Requesting data failed");
			    Opencast.Player.addEvent(Opencast.logging.SEGMENTS_TEXT_AJAX_FAILED);
			    $('#oc-segments_text').html('No Segment Text available');
			    $('#oc-segments_text').hide();
			    isOpening = false;
			    isOpen = false;
			}
		    });
            }
	    isOpening = false;
	}
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Hides the Segments Text Tab
     */
    function hide()
    {
	if(isOpen)
	{
            // Change Tab Caption
            $('#oc_btn-slidetext').attr(
		{
		    title: SEGMENTS_TEXT
		});
            $('#oc_btn-slidetext').html(SEGMENTS_TEXT);
            $("#oc_btn-slidetext").attr('aria-pressed', 'false');
            $('#oc_slidetext').hide();
	    isOpen = false;
	}
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Toggles the Segments Text Tab
     */
    function doToggle()
    {
        if (!isOpen)
        {
	    Opencast.Plugin_Controller.hideAll(Opencast.segments_text);
            show();
        }
        else if (staticBool_hide)
        {
            hide();
        }
    }
    
    /**
     * @memberOf Opencast.segments_text
     * @description Sets the mediaPackageId
     * @param String mediaPackageId
     */
    function setMediaPackageId(id)
    {
        mediaPackageId = id;
    }
    
    return {
        initialize: initialize,
        show: show,
        hide: hide,
        setMediaPackageId: setMediaPackageId,
        doToggle: doToggle
    };
}());
