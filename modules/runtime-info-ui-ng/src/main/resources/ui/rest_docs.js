function search() {
  var value = $('input').val();
  $('li').each(function() {
    $(this).toggle($(this).text().toLowerCase().indexOf(value.toLowerCase()) >= 0);
  });
}

$(document).ready(function($) {
  $('input').change(search);
  $('input').keyup(search);

  $.getJSON('/info/components.json', function(data) {
    $.each(data, function(section) {
      if ('rest' == section) {
        data.rest.sort((a,b) => a.path > b.path ? 1 : -1);
        $.each(data.rest, function(i) {
          $('#docs').append('<li><a href="/docs.html?path=' + data.rest[i].path + '">'
              + '<span>' + data.rest[i].path + '</span>'
              + data.rest[i].description + '</a></li>');
        });
        return;
      }
    });
    search();
  });
});
