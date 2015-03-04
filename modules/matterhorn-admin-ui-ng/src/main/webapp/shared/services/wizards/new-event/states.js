angular.module('adminNg.services')
.factory('NewEventStates', ['NewEventMetadata', 'NewEventMetadataExtended', 'NewEventSource', 'NewEventAccess', 'NewEventProcessing', 'NewEventSummary',
        function (NewEventMetadata, NewEventMetadataExtended, NewEventSource, NewEventAccess, NewEventProcessing, NewEventSummary) {
    return {
        get: function () {
            var states = [
                {
                    translation: 'EVENTS.EVENTS.NEW.METADATA.CAPTION',
                    name: 'metadata',
                    stateController: NewEventMetadata
                },
                {
                    translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
                    name: 'metadata-extended',
                    stateController: NewEventMetadataExtended
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.SOURCE.CAPTION',
                    name: 'source',
                    stateController: NewEventSource
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.PROCESSING.CAPTION',
                    name: 'processing',
                    // This allows us to reuse the processing functionality in schedule task
                    stateController: NewEventProcessing.get()
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.ACCESS.CAPTION',
                    name: 'access',
                    stateController: NewEventAccess
                },
                {
                    translation: 'EVENTS.EVENTS.NEW.SUMMARY.CAPTION',
                    name: 'summary',
                    stateController: NewEventSummary
                }
            ];
            return states;
        }
    };
}]);
