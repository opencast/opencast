/**
 * @ngdoc directive
 * @name adminNg.modal.confirmationModal
 * @description
 * Opens the modal with the name specified in the `confirmation-modal` attribute.
 *
 * The success callback name- and id defined in the `callback` and `id` data
 * attributes will be sent to the {@link adminNg.modal.ConfirmationModal service's show method}.
 *
 * @example
 * <a data-confirmation-modal="confirm-deletion-modal"
 *    data-success-callback="delete" data-success-id="82">delete
 * </a>
 */
angular.module('adminNg.directives')
.directive('confirmationModal', ['ConfirmationModal', function (ConfirmationModal) {
    return {
        scope: {
            callback: '=',
            object:   '='
        },
        link: function ($scope, element, attr) {
            element.bind('click', function () {
                ConfirmationModal.show(attr.confirmationModal, $scope.callback, $scope.object);
            });

            $scope.$on('$destroy', function () {
                element.unbind('click');
            });
        }
    };
}]);
