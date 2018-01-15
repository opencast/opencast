angular.module('adminNg.services')
.factory('SchedulingHelperService', [function () {
    var SchedulingHelperService = function () {

        var me = this;

        this.parseDate = function(dateStr, hour, minute) {
            var dateArray = dateStr.split("-");
            return new Date(dateArray[0], parseInt(dateArray[1]) - 1, dateArray[2], hour, minute);
        }

        this.getUpdatedEndDateSingle = function(start, end) {
            var startDate = me.parseDate(start.date, start.hour, start.minute);
            if (end.hour < start.hour) {
                startDate.setHours(24);
            }
            return moment(startDate).format('YYYY-MM-DD');
        };

        this.applyTemporalValueChange = function(temporalValues, change, single){
            var startMins = temporalValues.start.hour * 60 + temporalValues.start.minute;
            switch(change) {
                case "duration":
                    // Update end time
                    var durationMins = temporalValues.duration.hour * 60 + temporalValues.duration.minute;
                    temporalValues.end.hour = (Math.floor((startMins + durationMins) / 60)) % 24;
                    temporalValues.end.minute = (startMins + durationMins) % 60;
                    break;
                case "start":
                case "end":
                    // Update duration
                    var endMins = temporalValues.end.hour * 60 + temporalValues.end.minute;
                    if (endMins < startMins) endMins += 24 * 60; // end is on the next day
                    temporalValues.duration.hour = Math.floor((endMins - startMins) / 60);
                    temporalValues.duration.minute = (endMins - startMins) % 60;
                    break;
                default:
                    return;
            }
            if (single) {
                temporalValues.end.date = me.getUpdatedEndDateSingle(temporalValues.start, temporalValues.end);
            }
        };
    };
    return new SchedulingHelperService();
}]);
