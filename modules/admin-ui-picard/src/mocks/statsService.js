/*
* This Service is used for counting the number events of each status that a event can have. Furthermore events can be
* filtered depending on their status (returning these events in a json).
* The stats-json is also returned containing all necessary filter information and the counter of each status.
*
* Currently only a mock json is returned.
*
* */

const mockStats= {
    "RECORDING": {
        filters: [
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.RECORDING"
            }
        ],
        description: "DASHBOARD.RECORDING",
        order: 7,
        counter: 5
    },
    "PAUSED": {
        filters: [
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.PAUSED"
            }
        ],
        description: "DASHBOARD.PAUSED",
        order: 9,
        counter: 3
    },
    "FINISHED_WITH_COMMENTS": {
        filters: [
            {
                name: "comments",
                filter: "FILTERS.EVENTS.COMMENTS.LABEL",
                value: "OPEN"
            },
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.PROCESSED"
            }
        ],
        description: "DASHBOARD.FINISHED_WITH_COMMENTS",
        order: 11,
        counter: 13
    },
    "FAILED": {
        filters: [
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.PROCESSING_FAILURE"
            }
        ],
        description: "DASHBOARD.FAILED",
        order: 10,
        counter: 6
    },
    "RUNNING": {
        filters: [
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.PROCESSING"
            }
        ],
        description: "DASHBOARD.RUNNING",
        order: 8,
        counter: 66
    },
    "TODAY": {
        filters: [
            {
                name: "startDate",
                filter: "FILTERS.EVENTS.START_DATE",
                value: {
                    relativeDateSpan: {
                        from: "0",
                        to: "0",
                        unit: "day"
                    }
                }
            }
        ],
        description: "DATES.TODAY",
        order: 1,
        counter: 15
    },
    "TOMORROW": {
        filters: [
            {
                name: "startDate",
                filter: "FILTERS.EVENTS.START_DATE",
                value: {
                    relativeDateSpan: {
                        from: "1",
                        to: "1",
                        unit: "day"
                    }
                }
            }
        ],
        description: "DATES.TOMORROW",
        order: 2,
        counter: 22
    },
    "YESTERDAY": {
        filters: [
            {
                name: "startDate",
                filter: "FILTERS.EVENTS.START_DATE",
                value: {
                    relativeDateSpan: {
                        from: "-1",
                        to: "-1",
                        unit: "day"
                    }
                }
            }
        ],
        description: "DATES.YESTERDAY",
        order: 0,
        counter: 15
    },
    "FINISHED": {
        filters: [
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.PROCESSED"
            }
        ],
        description: "DASHBOARD.FINISHED",
        order: 12,
        counter: 666
    },
    "SCHEDULED": {
        filters: [
            {
                name: "status",
                filter: "FILTERS.EVENTS.STATUS.LABEL",
                value: "EVENTS.EVENTS.STATUS.SCHEDULED"
            }
        ],
        description: "DASHBOARD.SCHEDULED",
        order: 6,
        counter: 42
    }
};

const stats = Object.keys(mockStats).map(key => {
    let stat = mockStats[key];
    stat.name = key;
    return stat;
});

export default stats;

