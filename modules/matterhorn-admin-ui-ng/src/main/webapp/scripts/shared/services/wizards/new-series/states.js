angular.module('adminNg.services')
.factory('NewSeriesStates', ['NewSeriesMetadata', 'NewSeriesMetadataExtended', 'NewSeriesAccess', 'NewSeriesTheme', 'NewSeriesSummary',
        function (NewSeriesMetadata, NewSeriesMetadataExtended, NewSeriesAccess, NewSeriesTheme, NewSeriesSummary) {
    return {
        get: function () {
            return [{
                translation: 'EVENTS.SERIES.NEW.METADATA.CAPTION',
                name: 'metadata',
                stateController: NewSeriesMetadata
            }, {
                translation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
                name: 'metadata-extended',
                stateController: NewSeriesMetadataExtended
            }, {
                translation: 'EVENTS.SERIES.NEW.ACCESS.CAPTION',
                name: 'access',
                stateController: NewSeriesAccess
            }, {
               translation: 'EVENTS.SERIES.NEW.THEME.CAPTION',
                name: 'theme',
                stateController: NewSeriesTheme
            },
            {
                translation: 'EVENTS.SERIES.NEW.SUMMARY.CAPTION',
                name: 'summary',
                stateController: NewSeriesSummary
            }];
        }
    };
}]);
