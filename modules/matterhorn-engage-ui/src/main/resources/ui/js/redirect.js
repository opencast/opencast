$(document).ready(function() {
    var defaultPlayerURL = "/engage/ui/player.html";
    var infoMeURL = "/info/me.json";

    function initialize() {
        getInfo();
    }

    function getInfo() {
        $.ajax({
            url: infoMeURL,
            dataType: "json",
            success: function(data) {
                if (data && data.org && data.org.properties) {
                    var player = data.org.properties.player ? data.org.properties.player : defaultPlayerURL;
                    if (player.charAt(0) != "/")
                        player = "/" + player;
                    var server = "";
                    if (data.org.properties.org && data.org.properties.org.opencastproject && 
                            data.org.properties.org.opencastproject.engage &&
                            data.org.properties.org.opencastproject.engage.ui && 
                            data.org.properties.org.opencastproject.engage.ui.url) {
                        server = data.org.properties.org.opencastproject.engage.ui.url ? data.org.properties.org.opencastproject.engage.ui.url : "";
                    }
                    redirect(server + player);
                } 
            }
        })
    }
    
    function redirect(player) {
        var params = window.location.search.split(/\?/);
        
        if (params.length > 1) {
           window.location = player + "?" + params[1];
        }
    }

    $(window).load(function() {
        initialize();
    });       
});
