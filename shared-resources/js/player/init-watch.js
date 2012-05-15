$(document).ready( function() {
    $.ajax({
        url: '/info/me.json',
        type: 'GET',
        dataType: 'json',
        success: function(data)
        {
	    // login link
            if(data.username != "anonymous")
            {
		$('#logout').show();
            } else
	    {
		$('#logout').detach();
	    }

	    // download link
            var download_enabled = data.org.properties["engageui.link_download.enable"];
            if (download_enabled && (download_enabled == "true"))
	    {
                $("#oc_download-button").show();
            }
            else
            {
                $("#oc_download-button").detach();
            }

	    // media module links
            var media_module_enabled = data.org.properties["engageui.links_media_module.enable"];
            if (media_module_enabled && (media_module_enabled == "true"))
            {
                $("#oc_search").show();
                $("#oc_title-bar-gallery-link").show();
            } else
            {
                $("#oc_search").detach();
                $("#oc_title-bar-gallery-link").detach();
            }

	    // no comments plugin when disabled in config
            var annotations_module_enabled = data.org.properties["engageui.annotations.enable"];
	    if(annotations_module_enabled)
	    {
		annotations_module_enabled = (annotations_module_enabled == "true") ? true : false;
	    }
            if (!annotations_module_enabled)
	    {
                // Detach "Comment" Tab
		$('#oc_btn-comments').detach();
		$('#oc_btn-add-comment').detach();
		$('#oc_checkbox-annotation-comment').detach();
		$('#oc_label-annotation-comment').detach();
	    }

	    // no comments plugin support for IE 8
            if (annotations_module_enabled && $.browser.msie && (parseInt($.browser.version, 10) < 9))
	    {
                // Detach "Comment" Tab
		$('#oc_btn-comments').detach();
		$('#oc_btn-add-comment').detach();
		$('#oc_checkbox-annotation-comment').detach();
		$('#oc_label-annotation-comment').detach();
		$("#ie8comments-browser-version").html($.browser.version);
		$("#ie8comments-close").click(function()
					{
					    $("#oc_ie8comments").hide();
					});
                $("#oc_ie8comments").show();
	    } else
	    {
                $("#oc_ie8comments").detach();
	    }

	    // mobile_redirect link
	    var usingMobileBrowser = (/iphone|ipad|ipod|android|blackberry|mini|windows\sce|palm/i.test(navigator.userAgent.toLowerCase()));
	    var mobile_redirect_enabled = data.org.properties["engageui.link_mobile_redirect.enable"];
	    var mobile_redirect_url = data.org.properties["engageui.link_mobile_redirect.url"];
	    if (usingMobileBrowser && mobile_redirect_enabled && (mobile_redirect_enabled == "true") && mobile_redirect_url && (mobile_redirect_url != ""))
	    {
		var mobile_redirect_description = data.org.properties["engageui.link_mobile_redirect.description"];
		mobile_redirect_description = (mobile_redirect_description && (mobile_redirect_description != "")) ? mobile_redirect_description : mobile_redirect_url;
		$("#oc_mobile_redirect-url").html('<a href="' + mobile_redirect_url + '">' + mobile_redirect_description + '</a>');
		$("#mobile_redirect-close").click(function()
					{
					    $("#oc_mobile_redirect").hide();
					});
                $("#oc_mobile_redirect").show();
	    } else
	    {
                $("#oc_mobile_redirect").detach();
	    }
        }
    });
});
