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
        }
    });
});
