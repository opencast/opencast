angular.module('adminNg.services')
.factory('PlayerAdapterRepository', ['$injector', function ($injector) {


    /**
     * A repository containing the adapter instances for given adapter type (may be html5, videojs) and given element.
     * The sole purpose of this implementation is to be able to manage adapters on a per instance basis - if there is
     * a better solution for this problem, this class becomes obvious.
     *
     * @constructor
     */
    var PlayerAdapterRepository = function () {
        var adapters = {};

        /**
         * Returns the given adapter instance per adapterType and elementId. If the adapter does not exist,
         * it will be created.
         *
         * @param adapterType
         * @param element of the player
         * @returns {*}
         */
        this.findByAdapterTypeAndElementId = function (adapterType, element) {
            var factory, adapter;

            if (typeof adapters[adapterType] === 'undefined') {

                // create entry for adapterType if not existent
                adapters[adapterType] = {};
            }

            if (typeof adapters[adapterType][element.id] === 'undefined') {
                // lazy create adapter on demand over factory if not existent
                factory = $injector.get('PlayerAdapterFactory' + adapterType);
                adapter = factory.create(element);
                adapters[adapterType][element.id] = adapter;
            }
            return adapters[adapterType][element.id];
        };

    };

    return new PlayerAdapterRepository();
}]);
