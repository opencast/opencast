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
| Recording | All events with status _Recording_. |
| Running   | All events with status _Running_. |
| Paused    | All events with status _Paused_. |
| Failed    | All events with status _Failed_. |
| Todo      | All events with status _Finished_ and open comments. |
| Finished  | All events with status _Finished_. |

Filters can be added or removed by editing the file `etc/listproviders/adminui.stats.properties`. For example, the
_Finished_ filter is defined as follows:

    FINISHED=\
    {"filters": [{"name": "status", "filter": "FILTERS.EVENTS.STATUS.LABEL", \
    "value": "EVENTS.EVENTS.STATUS.PROCESSED"}],\
    "description": "DASHBOARD.FINISHED",\
    "order":12}

* `filters` defines a list containing at least one filter. Each filter is defined with
    *   a `name`  that defines the event property to filter on for the backend
    *   a `filter` that defines the event property to filter on for the frontend
    *   and the `value` that property is supposed to have
*   `description` contains the (possibly translated) description displayed in the UI
*   `order` controls the order the filters are shown in the UI

### Filters with relative time spans

For defining filters that contain a relative time span like _yesterday_ or _this week_ `value` can contain an object
instead of a string. This object has to contain a `relativeDateSpan` property which itself contains the fields `from`,
`to` and `unit`. The `unit` defines the unit of time that is being considered, e.g. _hour_, _day_, _week_, _month_ or
_year_, while `from` and `to` specify the beginning and end of the time span by defining an integer offset relative to
the current hour, day, ... depending on the unit. So if the `unit` is defined as _day_, **0** is the current day while
**-1** is yesterday and **1** is tomorrow. If the unit is _week_ instead, **0** is the current week while **-1** is the
last and **1** the next week, and so on.

Every date/time unit below the one defined by `unit` depends on whether the offset is defined by `to` or `from`. So if
the unit is _day_, **from: -1** would be the beginning of yesterday (so the time is 00:00:00 in the user's timezone)
while **to: -1** would be the end of yesterday (23:59:59). If the unit is *week*, **from: -1** is the beginning of last
week (which day is the first day of the week is defined by the user's locale) and **to: 0** would be the end of this
week, so a filter defined as

    LAST_TWO_WEEKS=\
    {"filters": [{"name": "startDate", "filter":"FILTERS.EVENTS.START_DATE",
                  "value": {"relativeDateSpan": {"from": "-2", "to": "0", "unit": "week"}}}],\
     "description": "DATES.LAST_TWO_WEEKS",\
     "order":15}

would cover all events whose start dates occur sometime during the last or current week.

This functionality is implemented with the library [Moment.js](https://momentjs.com) by adding the values of `to` or
`from` to the current date and time while considering the defined unit. A list of valid unit strings can be found in
the [documentation](https://momentjs.com).

##### To be considered
Since only one unit can be defined per filter, time spans like *the beginning of this month until tomorrow* are
currently not possible.

Be advised that a too big amount of filters can lead to filters disappearing from view depending on the width of the
user's screen.


Available Language Configuration
--------------------------------

The admin UI is translated into a number of languages by default.  If you wish to restrict the languages available to
your users, add the relevant locale code to `etc/org.opencastproject.adminui.endpoint.LanguageServiceEndpoint.cfg`.
