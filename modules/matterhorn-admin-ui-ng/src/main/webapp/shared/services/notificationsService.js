angular.module('adminNg.services')
.provider('Notifications', function () {
    var notifications = {},
        keyList = {},
        defaultDuration = 5000,
        uniqueId = 0;
    this.$get = ['$rootScope', function ($rootScope) {
        var scope = $rootScope.$new(),
            initContext = function (context) {
                if (angular.isDefined(keyList[context])) {
                    return notifications[context];
                }

                // initialize the arrays the first time
                keyList[context] = [];
                notifications[context] = {};

                return notifications[context];
            };

        scope.get = function (context) {
            if (!context) {
                context = 'global';
            }
            return notifications[context] || initContext(context);
        };

        scope.remove = function (id, context) {
            var key;
            if (!context) {
                context = 'global';
            }

            if (notifications[context] && notifications[context][id]) {
                // remove from key list
                key = notifications[context][id].key;
                keyList[context].splice(keyList[context].indexOf(key), 1);

                notifications[context][id] = undefined;
                delete notifications[context][id];
                scope.$emit('deleted', context);
            }
        };

        scope.add = function (type, key, context, duration) {
            if (angular.isUndefined(duration)) {
                switch (type) {
                    case 'error':
                    case 'success':
                    case 'warning':
                        if (angular.isUndefined(duration)) {
                            duration = defaultDuration;
                        }
                        break;
                }
            }

            if (!context) {
                context = 'global';
            }

            initContext(context);

            if(keyList[context].indexOf(key) < 0) {
                // only add notification if not yet existent

                // add key to an array
                keyList[context].push(key);

                notifications[context][++uniqueId] = {
                    type: type,
                    key: key,
                    message: 'NOTIFICATIONS.' + key,
                    duration: duration,
                    id: uniqueId
                };

                scope.$emit('added', context);
            }
            return uniqueId;
        };

        return scope;
    }];
});
