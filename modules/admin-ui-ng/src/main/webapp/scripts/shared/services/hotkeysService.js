angular.module('adminNg.services')
.factory('HotkeysService', ['$q', 'IdentityResource', 'hotkeys',
        function ($q, IdentityResource, hotkeys) {
    var HotkeysService = function () {
      var me = this,
          identity,
          keyBindings,
          loading;

      this.keyBindings = {};

      this.loadHotkeys = function () {
        return $q(function (resolve, reject) {
          identity = IdentityResource.get();
          identity.$promise.then(function (info) {
            if (info && info.org && info.org.properties && info.org.properties) {
              var properties = info.org.properties;
              angular.forEach(Object.keys(properties), function (key) {
                if (key.indexOf("admin.shortcut.") >= 0) {
                  var keyIdentifier = key.substring(15),
                      value = properties[key];
                  me.keyBindings[keyIdentifier] = value;
                }
              });
              resolve();
            } else {
              reject(); // as no hotkeys have been loaded
            }
          });
        });
      };

      this.activateHotkey = function (scope, keyIdentifier, description, callback) {
        me.loading.then(function () {
          var key = me.keyBindings[keyIdentifier];
          if (key !== undefined) {
            hotkeys.bindTo(scope).add({
              combo: key,
              description: description,
              callback: callback
            });
          }
        });
      }

      this.activateUniversalHotkey = function (keyIdentifier, description, callback) {
        me.loading.then(function () {
          var key = me.keyBindings[keyIdentifier];
          if (key !== undefined) {
            hotkeys.add({
              combo: key,
              description: description,
              callback: callback
            });
          }
        });
      }
      this.loading = this.loadHotkeys();
    };

    return new HotkeysService();
}]);
