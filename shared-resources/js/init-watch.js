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

	    // mh2go link
	    var usingMobileBrowser = (/iphone|ipad|ipod|android|blackberry|mini|windows\sce|palm/i.test(navigator.userAgent.toLowerCase()));
	    var mh2go_enabled = data.org.properties["engageui.link_mh2go.enable"];
	    var mh2go_url = data.org.properties["engageui.link_mh2go.url"];
	    if (usingMobileBrowser && mh2go_enabled && (mh2go_enabled == "true") && mh2go_url && (mh2go_url != ""))
	    {
		var mh2go_description = data.org.properties["engageui.link_mh2go.description"];
		mh2go_description = (mh2go_description && (mh2go_description != "")) ? mh2go_description : mh2go_url;
		$("#oc_mh2go-url").html('<a href="' + mh2go_url + '">' + mh2go_description + '</a>');
		$("#mh2go-close").click(function()
					{
					    $("#oc_mh2go").hide();
					});
                $("#oc_mh2go").show();
	    } else
	    {
                $("#oc_mh2go").detach();
	    }
        }
    });
});
