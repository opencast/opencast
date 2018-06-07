Admin UI Configuration
===================

Configuring the events filters
--------------------------------

At the top right of the admin UI a set of predefined filters for events are available, displayed with a description and
the amount of events currently matching that filter. By default, the following filters are visible:

| Statistic   | Description |
|-------------|----------------|
| Yesterday | All events with a start date sometime yesterday. |
| Today     | All events with a start date sometime today. |
| Tomorrow  | All events with a start date sometime tomorrow. |
| Scheduled | All events with status _Scheduled_. |
| Recording| All events with status _Recording_. |
| Running  | All events with status _Running_. |
| Paused   | All events with status _Paused_. |
| Failed   | All events with status _Failed_. |
| Todo     | All events with status _Finished_ and open comments. |
| Finished | All events with status _Finished_. |

Filters can be added or removed by editing the file `etc/listproviders/adminui.stats.properties`. For example, the
_Finished_ filter is defined as follows:

    FINISHED=\
    {"filters": [{"name": "status", "filter": "FILTERS.EVENTS.STATUS.LABEL", "value": "EVENTS.EVENTS.STATUS.PROCESSED"}],\
    "description": "DASHBOARD.FINISHED",\
    "order":12}


* `filters` defines a list containing at least one filter. Each filter is defined with:
    *   a `name`
    *   a `filter` that defines an event property
    *   and the `value` that property is supposed to have
*   `description` contains the (possibly translated) description displayed in the UI
*   `order`controls the order the filters are shown in the UI

For defining filters that contain a relative time span like _this week_ the `value` can be left empty and instead a
`relativeDateSpan` property containing two keywords `from`and `to` are added. An example:

    THIS_WEEK=\
    {"filters": [{"name": "startDate", "filter":"FILTERS.EVENTS.START_DATE", "value": "", "relativeDateSpan": \
    {"from": "this monday", "to": "next monday"}}], "description": "DATES.THIS_WEEK", "order":3}

The strings defining the relative dates are translated by the library [SugarJs](https://sugarjs.com), their
[website](https://sugarjs.com/dates/#/Parsing) can be used to check if a string can be parsed.
(English language support only!)