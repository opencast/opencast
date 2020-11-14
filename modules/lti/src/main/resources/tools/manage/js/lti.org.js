function Organization() {
  var _listeners = {};
  this.getDefaults();
}

Organization.prototype = {
  constructor: Organization,
  getDefaults: function() {
    $.ajax({
           url: '/info/me.json',
      dataType: 'json'
    })
    .done(function(res) {
      for (var key in res) {
        this[key] = res[key];
      }
    }.bind(this))
    .fail(function(err) {
      console.log('failed getting org defaults', err);
    });
  }
}
