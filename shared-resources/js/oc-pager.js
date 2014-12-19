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
 * @namespace the global Opencast namespace pager
 */
Opencast.pager = (function ()
{
    /**
     * @memberOf Opencast.pager
     * @description Renders the pager
     */
    function renderPager()
    {
        // constants
        var PREVIOUS_TEXT = "Previous";
        var NEXT_TEXT = "Next";
        var OFFSET = 2;
        // variables
        var LINK_PREFIX = window.location.origin + window.location.pathname;
        var currentPageId, lowPageId, highPageId, maxPageId;
        var li;
	var pages = [];

        $('.navigation').empty();

        // display the pager only if necessary
        if ($('#oc-episodes-total').html() < 10)
        {
            $('ul.navigation').css('visibility', 'hidden');
	    return;
        }

	var query = $.parseURL();

	// get the current page id
	if ((query.hasOwnProperty('page')) && (parseInt(query['page']))) {
	    currentPageId = parseInt(query['page']);
	} else {
	    currentPageId = 1;
	}

        // get the max page id
        maxPageId = getMaxPageID();

        // take care for the previous page button
        if (currentPageId <= 1) {
            pages.push('<span style="color:#CCCCCC">' + PREVIOUS_TEXT + "</span>");
        } else {
	    query['page'] = currentPageId - 1;
            pages.push("<a href='" + LINK_PREFIX + $.urlMapToString(query) + "'>" + PREVIOUS_TEXT + "</a>");
        }

        // Pipe before page numbers
	pages.push("<span>|</span>");

        // "Page:"
        pages.push("<span id='currentPage'>Page:</span>");

        // take care for the page buttons
	lowPageId = currentPageId - OFFSET;
	highPageId = currentPageId + OFFSET;
	for (query['page'] = 1; query['page'] <= maxPageId; query['page']++) {
	    if (query['page'] == currentPageId) {
		// Insert the current page number (no link)
		pages.push('<span id="currentPage">' + query['page'] + '</span>');
	    } else {
		// Insert a link to a page within the range [currentPageId-OFFSET, currentPageId+OFFSET]
		pages.push("<a href='"+LINK_PREFIX+$.urlMapToString(query)+"'>"+query['page']+"</a>");
	    }

	    if ((query['page'] == 1) && (lowPageId > 2)) {
		// Insert a '...' span if the first page was included in this iteration and 
		// currentPageId-OFFSET is greater than two (otherwise no pages would be skipped and
		// the ellipsis "..." is unnecesary). Then jump to the page currentPageId-OFFSET
		// (i.e. modify the index so that the next iteration will be currentPageId-OFFSET)
                pages.push("<span>...</span>");
                query['page'] = lowPageId - 1;
		continue;
	    }

	    if ((query['page'] == highPageId) && (highPageId < maxPageId - 1)) {
		// Insert a '...' span if the page currentPage+OFFSET was included in this iteration
		// and such page is not the second-last page (otherwise no pages would be skipped and
		// the ellipsis "..." is unnecessary). Then jump to the last page (i.e. modify the
		// index so that the next interation will be maxPageId)
                pages.push("<span>...</span>");
                query['page'] = maxPageId - 1;
		continue;
	    }
	}

        // Pipe after page numbers
	pages.push("<span>|</span>");

        // take care for the next page button
        if (currentPageId >= maxPageId) {
            pages.push("<span style='color:#CCCCCC'>" + NEXT_TEXT + "</span>");
        } else {
	    query['page'] = currentPageId + 1;
	    pages.push("<a href='" + LINK_PREFIX + $.urlMapToString(query) + "'>" + NEXT_TEXT + "</a>");
        }

	for (var i = 0; i < pages.length; i++) {
	    li = document.createElement('li');
	    li.innerHTML = pages[i];
	    $('.navigation').append(li);
	}
    }
    
    /**
     * @memberOf Opencast.pager
     * @description Test the function getCurrentPage
     * @param number time
     */
    function testGetCurrentPage()
    {
        $('#log').empty().append("Current Page: " + getCurrentPageID());
    }
    
    /**
     * @memberOf Opencast.pager
     * @description Gets the current page ID
     */
    function getCurrentPageID()
    {
        var value = $.getURLParameter("page");
        /* if the GET parameter page is not there
         * Assume that we are on page 1
         * otherwise return the page value from the get parameter
         */
        if (value == null) return 1;
        else
        return value;
    }
    
    /**
     * @memberOf Opencast.pager
     * @description Gets the current search query
     * @return The current search query
     */
    function getCurrentSearchQuery()
    {
        var value = $.getURLParameter("q");
        return value;
    }
    
    /**
     * @memberOf Opencast.pager
     * @description Gets the current series query
     * @return The current series query
     */
    function getCurrentSeriesQuery()
    {
        var value = $.getURLParameter("seriesId");
        return value;
    }

    /**
     * @memberOf Opencast.pager
     * @description Gets the current sorting
     * @return The current sorting
     */
    function getCurrentSorting()
    {
        var value = $.getURLParameter("sort");
        return value;
    }

    /**
     * @memberOf Opencast.pager
     * @description Gets the max page ID
     * @return The maximum number of pages
     */
    function getMaxPageID()
    {
        var total = $('#oc-episodes-total').html()
        var maxPage = parseInt(total / 10);
        if (total % 10 != 0) maxPage += 1;
        return Math.max(1, maxPage);
    }

    return {
        testGetCurrentPage: testGetCurrentPage,
        getCurrentPageID: getCurrentPageID,
        getCurrentSearchQuery: getCurrentSearchQuery,
        getCurrentSeriesQuery: getCurrentSeriesQuery,
        getCurrentSorting: getCurrentSorting,
        renderPager: renderPager
    };
}());
