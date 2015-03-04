angular.module('adminNg.services')
.factory('BulkMessageRecipients', ['RecipientsResource', 'Table',
function (RecipientsResource, Table) {
    var Recipients = function () {
        var me = this;

        this.reset = function () {
            var resource = Table.resource || 'events',
                request = {
                    resource: resource
                };
            me.ud = { items: { recipients: [], recordings: [] } };

            request.category = request.resource === 'events' ? 'event':'series';
            request[request.category + 'Ids'] = Table.getSelected().
                map(function (item) { return item.id; }).join(',');

            RecipientsResource.get(request, function (data) {
                me.ud.items = data;
            });
        };
        this.reset();

        this.isValid = function () {
            return me.ud.items.recipients.length > 0;
        };
    };
    return new Recipients();
}]);
