/**
 * @ngdoc service
 * @name adminNg.modal.Modal
 * @description
 * Provides a service for displaying a modal filled with contents from a
 * partial.
 */
angular.module('adminNg.services.modal')
.factory('Modal', ['$http', '$compile', '$rootScope', '$location', '$timeout', 'Table', function ($http, $compile, $rootScope, $location, $timeout, Table) {
    var Modal = function () {
        var me = this;

        /**
         * @ngdoc function
         * @name Modal.show
         * @methodOf adminNg.modal.Modal
         * @description
         * Displays a modal and its overlay.
         *
         * Loads markup via AJAX from 'shared/partials/modals/{{ modalId }}.html'.
         *
         * @param {string} modalId Identifier for the modal.
         * @returns {HttpPromise} a promise object for the template HTTP request
         */
        this.show = function (modalId) {
            var $scope, http, params;

            // When opening a modal from another modal, remove the latter from the DOM
            // first.
            if (this.$scope) {
                this.$scope.$destroy();
            }
            if (me.modal) {
                me.modal.remove();
            }
            if (me.overlay) {
                me.overlay.remove();
            }
            $scope = $rootScope.$new();
            this.$scope = $scope;

            /**
             * @ngdoc function
             * @name Modal.close
             * @methodOf adminNg.modal.Modal
             * @description
             * Close the currently open modal.
             *
             * Fades out the overlay and the tab content and updates the URL by
             * removing all search parameters.
             */
            $scope.close = function (fetch) {
                $scope.open = false;
                $location.search({});
                if (!angular.isUndefined(me.modal)) {
                    me.modal.remove();
                    me.overlay.remove();
                    delete me.modal;
                    delete me.overlay;
                }
                if(angular.isUndefined(fetch) || true === fetch) {
                    Table.fetch();
                }
                $scope.$destroy();
            };

            /**
             * @ngdoc function
             * @name Modal.keyUp
             * @methodOf adminNg.modal.Modal
             * @description
             * Closes the modal when pressing ESC.
             *
             * @param {event} event Event that triggered this function.
             */
            $scope.keyUp = function (event) {
                switch (event.keyCode) {
                case 27:
                    $scope.close();
                    break;
                }
            };

            // Guard against concurrent calls
            if (me.opening) { return; }
            me.opening = true;
            $scope.open = false;

            // Fetch the modal markup from the partial named after its ID
            http = $http.get('shared/partials/modals/' + modalId + '.html', {});

            http.then(function (html) {
                // Compile modal and overlay and attach them to the DOM.
                me.overlay = angular.element('<div ng-show="open" class="modal-animation modal-overlay"></div>');
                angular.element(document.body).append(me.overlay);
                $compile(me.overlay)($scope);

                me.modal = angular.element(html.data);
                angular.element(document.body).prepend(me.modal);
                $compile(me.modal)($scope);

                // Signal animation start to overlay and modal
                $scope.open = true;

                // Set location
                params = $location.search();
                params.modal = modalId;
                $location.search(params);

                delete me.opening;

                // Focus the modal so it can be closed by key press
                $timeout(function () {
                    me.modal.focus();
                }, 100);
            });

            return http;
        };
    };

    return new Modal();
}]);
