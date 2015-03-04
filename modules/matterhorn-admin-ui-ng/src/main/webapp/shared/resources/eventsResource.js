angular.module('adminNg.resources')
.factory('EventsResource', ['$resource', 'Language', '$translate', 'ResourceHelper', function ($resource, Language, $translate, ResourceHelper) {

    /*
     * Here's an example for how we can fetch mock data from the server:
     * ...
     * return $resource('events/events.json', {}, {
     * ...,
     * this resource does not have a leading slash, hence the mock-data will be fetched from admin-ng/events/events.json
     *
     * In order to fetch real data, just add a leading slash:
     * ...
     * return $resource('/events/events.json', {}, {
     * ...,
     * then the real data will be fetched from /events/events.json
     */

    // We are live and are getting the real thing.
    return $resource('/admin-ng/event/events.json', {}, {
        query: {method: 'GET', isArray: false, transformResponse: function (data) {
            return ResourceHelper.parseResponse(data, function (r) {
                var row = {};
                row.id = r.id;
                row.title = r.title;
                row.presenter = r.presenters.join(', ');
                if (angular.isDefined(r.series)) {
                    row.series_name = r.series.title;
                }
                row.review_status = r.review_status;
                row.source = r.source;
                row.scheduling_status = r.scheduling_status;
                $translate(r.scheduling_status).then(function (translation) {
                    row.scheduling_status = translation;
                });
                row.workflow_state = r.workflow_state;
                row.date = Language.formatDate('short', r.start_date);
                row.publications = r.publications;
                row.start_date = Language.formatTime('short', r.start_date);
                row.end_date = Language.formatTime('short', r.end_date);
                row.has_comments = r.has_comments;
                row.has_open_comments = r.has_open_comments;
                row.has_preview = r.has_preview;
                row.location = r.location;
                row.managed_acl = r.managedAcl;
                return row;
            });

        }}
    });
}]);
