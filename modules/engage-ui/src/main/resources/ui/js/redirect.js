var defaultPlayerURL = "/engage/theodul/ui/core.html";
var infoMeURL = "/info/me.json";

function error() {
  $('body').show();
}

function redirect(player) {
  var params = window.location.search.split(/\?/);

  if (params.length > 1) {
    window.location.replace(player + "?" + params[1]);
  } else {
    error();
  }
}

$.ajax({
  url: infoMeURL,
  dataType: "json",
  success: function(data) {
    if (data && data.org && data.org.properties) {
      var player = data.org.properties.player || defaultPlayerURL;
      if (!player.startsWith('/')) {
        player = "/" + player;
      }
      var server = data.org.properties['org.opencastproject.engage.ui.url'] || '';
      redirect(server + player);
    } 
  },
  error: error
})
