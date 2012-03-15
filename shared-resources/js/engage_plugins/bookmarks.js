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
 * @namespace the global Opencast namespace Bookmarks
 */
Opencast.Bookmarks = (function ()
{
    var BOOKMARKS = "Bookmarks",
        BOOKMARKSHIDE = "Hide Bookmarks";
    
    /**
     * @memberOf Opencast.Bookmarks
     * @description Initializes the segments view
     */
    function initialize()
    {
        $('#oc_btn-addBookmark').click(function ()
        {
            var value = $('#oc_addBookmark').attr('value');
            Opencast.Bookmarks.addBookmark(Opencast.Player.getMediaPackageId(), Opencast.Player.getSessionId(), parseInt(Opencast.Player.getCurrentPosition()), value);
        });
        $('#oc_btn-removeBookmark').click(function ()
        {
            Opencast.Bookmarks.removeBookmark();
        });
        $('#oc_bookmarkSelect').click(function ()
        {
            var className = '';
            $('#oc_bookmarkSelect option:selected').each(function ()
            {
                className = $(this).attr('class');
            });
            Opencast.Bookmarks.playBookmark($('#oc_bookmarkSelect').val());
            if (className === OPTIONMYCLASSNAME)
            {
                $('#oc_btn-removeBookmark').css('display', 'inline');
            }
            else
            {
                $('#oc_btn-removeBookmark').css('display', 'none');
            }
        });
        $('#oc_myBookmarks-checkbox').click(function ()
        {
            if ($("#oc_myBookmarks-checkbox").attr('aria-checked') === 'true')
            {
                $("#oc_myBookmarks-checkbox").attr('aria-checked', 'false');
                $('.oc_option-myBookmark').css('display', 'none');
                $('.oc_option-myBookmark').css('visibility', 'hidden');
                $('#oc_btn-removeBookmark').css('display', 'none');
                $('.oc_boomarkPoint').css('display', 'none');
            }
            else
            {
                $("#oc_myBookmarks-checkbox").attr('aria-checked', 'true');
                $('.oc_option-myBookmark').css('display', 'block');
                $('.oc_option-myBookmark').css('visibility', 'visible');
                $('.oc_boomarkPoint').css('display', 'inline');
            }
        });
        $('#oc_publicBookmarks-checkbox').click(function ()
        {
            if ($("#oc_publicBookmarks-checkbox").attr('aria-checked') === 'true')
            {
                $("#oc_publicBookmarks-checkbox").attr('aria-checked', 'false');
                $('.oc_option-publicBookmark').css('display', 'none');
                $('.oc_option-publicBookmark').css('visibility', 'hidden');
            }
            else
            {
                $("#oc_publicBookmarks-checkbox").attr('aria-checked', 'true');
                $('.oc_option-publicBookmark').css('display', 'block');
                $('.oc_option-publicBookmark').css('visibility', 'visible');
            }
        });
        // Handler keypress
        $('#oc_addBookmark').keypress(function (event)
        {
            if (event.keyCode === 13)
            {
                var value = $('#oc_current-time').attr('value');
                var text = $('#oc_addBookmark').attr('value');
                var name = 'Stefan Altevogt';
                Opencast.Player.addBookmark(value, name, text);
            }
        });
        var content = '';
        content = content + '<li class="oc_bookmarks-list-myBookmarks">';
        content = content + '<div class="oc_bookmarks-list-myBookmarks-div-left" type="text">Funny joke! He said: Lorem ipsum dolor sit amet...</div>';
        content = content + '<div class="oc_bookmarks-list-myBookmarks-div-right" type="text">00:15:20 Alicia Valls</div>';
        content = content + '</li>';
        content = content + '<li class="oc_bookmarks-list-publicBookmarks">';
        content = content + '<div class="oc_bookmarks-list-publicBookmarks-div-left" type="text">Multicelled organisms definition</div>';
        content = content + '<div class="oc_bookmarks-list-publicBookmarks-div-right" type="text">00:20:02 Justin Ratcliffe</div>';
        content = content + '</li>';
        content = content + '<li class="oc_bookmarks-list-myBookmarks">';
        content = content + '<div class="oc_bookmarks-list-myBookmarks-div-left" type="text">Funny joke! He said: Lorem ipsum dolor sit amet...</div>';
        content = content + '<div class="oc_bookmarks-list-myBookmarks-div-right" type="text">00:15:20 Alicia Valls</div>';
        content = content + '</li>';
        content = content + '<li class="oc_bookmarks-list-publicBookmarks">';
        content = content + '<div class="oc_bookmarks-list-publicBookmarks-div-left" type="text">Multicelled organisms definition</div>';
        content = content + '<div class="oc_bookmarks-list-publicBookmarks-div-right" type="text">00:20:02 Justin Ratcliffe</div>';
        content = content + '</li>';
        $('#oc_bookmarks-list').prepend(content);
    }
    
    /**
     @memberOf Opencast.Bookmarks
     @description Show the bookmarks
     */
    function showBookmarks()
    {
        $("#oc_bookmarks").attr("className", "oc_DisplayBlock-textleft");
        $("#oc_btn-bookmarks").attr(
        {
            alt: BOOKMARKSHIDE,
            title: BOOKMARKSHIDE,
            value: BOOKMARKSHIDE
        });
        $("#oc_btn-bookmarks").attr('aria-pressed', 'true');
        $("#oc_bookmarksPoints").css('display', 'block');
    }
    
    /**
     @memberOf Opencast.Bookmarks
     @description Hide the bookmarks
     */
    function hideBookmarks()
    {
        $("#oc_bookmarks").attr("className", "oc_DisplayNone");
        $("#oc_btn-bookmarks").attr(
        {
            alt: BOOKMARKS,
            title: BOOKMARKS,
            value: BOOKMARKS
        });
        $("#oc_btn-bookmarks").attr('aria-pressed', 'false');
        $("#oc_bookmarksPoints").css('display', 'none');
    }
    
    /**
     @memberOf Opencast.Player
     @description Toggle the bookmarks
     */
    function doToggleBookmarks()
    {
        if ($("#oc_btn-bookmarks").attr("title") === BOOKMARKS)
        {
            showBookmarks();
            hideEmbed();
            hideSlides();
            setShowSections(true);
        }
        else
        {
            hideBookmarks();
            setShowSections(false);
        }
        // Opencast.Initialize.doResize();
    }
    
    /**
     * @memberOf Opencast.Bookmarks
     * @description Add a bookmark
     * @param String value, String name, String text
     */
    function addBookmark(mediaPackageId, sessionId, curPosition, value)
    {
        $.ajax(
        {
            type: 'PUT',
            contentType: 'text/xml',
            url: "../../annotation/",
            data: "id=" + mediaPackageId + "&session=" + sessionId + "&in=" + curPosition + "&out=" + curPosition + "&type=BOOKMARK&value=" + value,
            dataType: 'xml',
            success: function (xml)
            {
                // The BOOKMARK has been saved
                var unencoded = $.formatSeconds(curPosition) + " " + value;
                encoded = $('<div/>').text(unencoded).html();
                var option = $('<option/>').val(encoded).addClass("oc_option-myBookmark").attr("title", encoded).text(unencoded);
                $('#oc_bookmarkSelect').prepend(option);
                if ($("#oc_myBookmarks-checkbox").attr('aria-checked') === 'false')
                {
                    $("#oc_myBookmarks-checkbox").attr('aria-checked', 'true');
                    $('#oc_myBookmarks-checkbox').attr('checked', 'true');
                    $('option.oc_option-myBookmark').css('display', 'block');
                    $('.oc_option-myBookmark').css('visibility', 'visible');
                }
                var unencoded = value;
                encoded = $('<div/>').text(unencoded).html();
                //
                var currentPosition = Opencast.Player.getCurrentPosition();
                var btn = $('<input/>').addClass("oc_boomarkPoint").attr(
                {
                    onClick: "Opencast.Player.playBookmark(this.name)",
                    style: 'left:' + currentPosition + '%; width: 5px; height: 10px; margin-left: 5px; position: absolute; background-color: #90ee90 !important;',
                    name: value,
                    alt: encoded,
                    title: encoded
                });
                $('#oc_bookmarksPoints').append(btn);
            },
            error: function (a, b, c)
            {
                // Some error while adding the BOOKMARK
            }
        });
    }
    
    /**
     * @memberOf Opencast.Bookmarks
     * @description remove a bookmark
     */
    function removeBookmark()
    {
        $('#oc_bookmarkSelect option:selected').remove();
        $('#oc_btn-removeBookmark').css('display', 'none');
    }
    
    /**
     * @memberOf Opencast.Bookmarks
     * @description play a bookmark
     * @param String playheadString
     */
    function playBookmark(playheadString)
    {
        var newPlayhead = getPlayhead(playheadString);
        Videodisplay.seek(newPlayhead);
    }
    
    return {
        initialize: initialize,
        showBookmarks: showBookmarks,
        hideBookmarks: hideBookmarks,
        doToggleBookmarks: doToggleBookmarks,
        addBookmark: addBookmark,
        removeBookmark: removeBookmark,
        playBookmark: playBookmark
    };
}());
