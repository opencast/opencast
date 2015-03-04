/**
 * @ngdoc service
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying a confirmation dialog.
 * partial.
 */
angular.module('adminNg.services.modal')
.factory('ConfirmationModal', ['$location', 'Modal', function ($location, Modal) {
    var ConfirmationModal = function () {
        var me = this;

        /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @param {string} callback Name of the function to call when confirmed
         * @param {string} object Hash for the success callback function
         */
        this.show = function (modalId, callback, object) {
            Modal.show(modalId);
            me.$scope = Modal.$scope;

            me.$scope.confirm  = me.confirm;
            me.$scope.callback = callback;
            me.$scope.object   = object;
        };

        this.confirm = function () {
            me.$scope.callback(me.$scope.object);
            me.$scope.close();
        };
    };

    return new ConfirmationModal();
}]);
