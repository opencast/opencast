angular.module('adminNg.resources')
.factory('EmailTemplatesResource', ['$resource', 'Language', 'ResourceHelper',
function ($resource, Language, ResourceHelper) {
    return $resource('/email/templates.json', {}, {
        query: {method: 'GET', isArray: false, transformResponse: function (json) {
            return ResourceHelper.parseResponse(json, function (r) {
                var row = {};
                row.id = r.id;
                row.name = r.name;
                row.created = Language.formatDate('short', r.creationDate);
                row.creator = r.creator.username;
                return row;
            });
        }}
    });
}])
.factory('EmailTemplateResource', ['$resource',
function ($resource) {
    return $resource('/email/template/:id', {}, {
        get: {
            method: 'GET',
            transformResponse: function (data) {
                data = JSON.parse(data);
                return {
                    id: data.id,
                    name: data.name,
                    subject: data.subject,
                    message: data.body
                };
            }
        },
        update: {
            method: 'PUT',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
                    return data;
                }

                var request = {};

                request.name    = data.message.name;
                request.type    = 'INVITATION';
                request.subject = data.message.subject;
                request.body    = data.message.message;

                return $.param(request);
            }
        },
        save: {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            transformRequest: function (data) {
                if (angular.isUndefined(data) || angular.isUndefined(data.message)) {
                    return data;
                }

                var request = {};

                request.name    = data.message.name;
                request.type    = 'INVITATION';
                request.subject = data.message.subject;
                request.body    = data.message.message;

                return $.param(request);
            }
        }
    });
}])
.factory('EmailTemplateDemoResource', ['$resource', function ($resource) {
    return $resource('/email/demotemplate');
}])
.factory('EmailVariablesResource', ['$resource', function ($resource) {
    return $resource('/email/variables.json');
}]);
