angular.module('adminNg.resources')
.factory('RecipientsResource', ['$resource', function ($resource) {
    return $resource('/admin-ng/:category/:resource/recipients');
}])
.factory('EmailPreviewResource', ['$resource', function ($resource) {
    return $resource('/email/preview/:templateId', {}, {
        save: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
                    return data;
                }

                var request = {};

                request.eventIds = data.recipients.items.recordings
                    .map(function (item) { return item.id; }).join(',');
                request.personIds    = data.recipients.items.recipients
                    .map(function (item) { return item.id; }).join(',');
                request.signature    = data.message.include_signature ? true:false;
                request.body         = data.message.message;

                return $.param(request);
            }
        }
    });
}])
.factory('EmailResource', ['$resource', function ($resource) {
    return $resource('/email/send/:templateId', {}, {
        save: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
                    return data;
                }

                var request = {};

                request.eventIds = data.recipients.items.recordings
                    .map(function (item) { return item.id; }).join(',');
                request.personIds    = data.recipients.items.recipients
                    .map(function (item) { return item.id; }).join(',');
                request.signature    = data.message.include_signature ? true:false;
                request.subject      = data.message.subject;
                request.body         = data.message.message;
                request.store        = data.recipients.audit_trail ? true:false;

                return $.param(request);
            }
        }
    });
}]);
