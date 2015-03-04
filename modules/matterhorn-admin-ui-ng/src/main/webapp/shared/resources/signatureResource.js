angular.module('adminNg.resources')
.factory('SignatureResource', ['$resource', function ($resource) {
    var transformRequest = function (data) {
        var request = {};
        request.username = data.username;
        request.from_name = request.reply_name = data.name;
        request.from_address = data.replyAddress = data.sender.address;
        request.text = data.signature;
        request.name = data.name;
        return $.param(request);
    };

    return $resource('/admin-ng/user-settings/signature/:id', {id: '@id'}, {
        get: {
            method: 'GET',
            transformResponse: function (data) {
                try {
                    data = JSON.parse(data);
                } catch (e) {
                    return {
                        replyTo: {},
                        existsOnServer: false
                    };
                }
                data.existsOnServer = true;
                return data;
            }
        },
        update: {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            params: {
                id: '@id'
            },
            transformRequest: transformRequest
        },
        save: {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'
            },
            transformRequest: transformRequest
        }
    });
}]);
