/**
 * @ngdoc directive
 * @name adminNg.directives.openModal
 * @description
 * Opens the modal with the name specified directive value.
 *
 * Optionally, a resource ID can be passed to the {@link adminNg.modal.ResourceModal service's show method}.
 * by setting the data attribute `resource-id` on the same element.
 *
 * @example
 * <a data-open-modal="recording-details" data-resource-id="82">details</a>
 */
angular.module('adminNg.directives')
.directive('openModal', ['ResourceModal', function (ResourceModal) {
    return {
        link: function ($scope, element, attr) {
            element.bind('click', function () {
                ResourceModal.show(attr.openModal, attr.resourceId, attr.tab, attr.action);
            });

            $scope.$on('$destroy', function () {
                element.unbind('click');
            });
        }
    };
}]);
