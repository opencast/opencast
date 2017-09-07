// Asset upload html UI directive
// Used in new event and existing event asset upload
angular.module('adminNg.directives')
.directive('adminNgUploadAsset', ['$filter', function ($filter) {
    return {
        restrict: 'A',
        scope: {
          assetoptions: "=",
          assets: "=filebindcontainer",
          onexitscope: "="
        },
        templateUrl: 'shared/partials/uploadAsset.html',
        link: function(scope, elem, attrs) {
           scope.removeFile = function(elemId) {
               delete scope.assets[elemId];
               angular.element(document.getElementById(elemId)).val('');
           }
           scope.addFile = function(file) {
               scope.assets[elem.id] = file;
           }
           // Allows sorting list on traslated title/description/caption
           scope.translatedTitle = function(asset) {
               return $filter('translate')(asset.title);
           }
           //The "onexitscope"'s oldValue acts as the callback when the scope of the directive is exited.
           //The callback allow the parent scope to do work (i.e. make a summary map) based on
           //  activies performed in this directive.
           scope.$watch('onexitscope', function(newValue, oldValue) {
                if (oldValue) {
                    oldValue();
                }
           });
        }
    };
}]);
