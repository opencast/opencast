import {
    loadFiltersSuccess,
    loadFiltersFailure,
    loadFiltersInProgress,
    loadStats,
    editFilterValue
} from '../actions/tableFilterActions';
import axios from "axios";
import {relativeDateSpanToFilterValue} from "../utils/dateUtils";
import {logger} from "../utils/logger";
import { setOffset } from '../actions/tableActions';
import { fetchEvents } from './eventThunks';
import { fetchServices } from './serviceThunks';
/**
* This file contains methods/thunks used to query the REST-API of Opencast to get the filters of a certain resource type.
* This information is used to filter the entries of the table in the main view.
*
* */
// Fetch table filters from opencast instance and transform them for further use
export const fetchFilters = resource => async (dispatch, getState)=> {
    try {
        const { tableFilters } = getState();

        if (tableFilters.currentResource === resource) {
            return;
        }

        dispatch(loadFiltersInProgress());

        const data = await axios.get(`/admin-ng/resources/${resource}/filters.json`);
        const resourceData = await data.data;

        let response = transformResponse(resourceData);

        const filters = response
        const filtersList = Object.keys(filters.filters).map(key => {
            let filter = filters.filters[key];
            filter.name = key;
            return filter;
        });
        dispatch(loadFiltersSuccess(filtersList, resource));
    } catch (e) {
        dispatch(loadFiltersFailure());
        logger.error(e);
    }
}

export const fetchStats = () => async dispatch => {
    try {
        // fetch information about possible status an event can have
        let data =  await axios.get('/admin-ng/resources/STATS.json');
        let response = await data.data;

        // transform response
        const statsResponse = Object.keys(response).map(key => {
            let stat = JSON.parse(response[key]);
            stat.name = key;
            return stat;
        });

        let stats = [];

        // fetch for each status the corresponding count of events having this status
        for (let i in statsResponse) {
            let filter = [];
            for (let j in statsResponse[i].filters) {
                let value = statsResponse[i].filters[j].value;
                let name = statsResponse[i].filters[j].name;

                if (Object.prototype.hasOwnProperty.call(value, 'relativeDateSpan')) {
                    value = relativeDateSpanToFilterValue(value.relativeDateSpan.from, value.relativeDateSpan.to, value.relativeDateSpan.unit);
                    // set date span as filter value
                    statsResponse[i].filters[j].value = value;
                }
                filter.push(name + ':' + value);
            }
            let data = await axios.get('/admin-ng/event/events.json', {
                params: {
                    filter: filter.join(','),
                    limit: 1
                }
            });

            let response = await data.data;

            // add count to status information fetched before
            statsResponse[i] = {
                ...statsResponse[i],
                count: response.total
            };

            // fill stats array for redux state
            stats.push(statsResponse[i]);
        }

        stats.sort(compareOrder);

        dispatch(loadStats(stats));

    } catch (e) {
        logger.error(e);
    }
}

export const setSpecificEventFilter = (filter, filterValue) => async (dispatch, getState) => {
    await dispatch(fetchFilters("events"));

    const { tableFilters } = getState();

    let filterToChange = tableFilters.data.find(({ name }) => name === filter);

    if (!!filterToChange) {
        await dispatch(editFilterValue(filterToChange.name, filterValue));
    }

    dispatch(setOffset(0));

    dispatch(fetchStats());

    dispatch(fetchEvents());

}

export const setSpecificServiceFilter = (filter, filterValue) => async (dispatch, getState) => {
    await dispatch(fetchFilters("services"));

    const { tableFilters } = getState();

    let filterToChange = tableFilters.data.find(({ name }) => name === filter);

    if (!!filterToChange) {
        await dispatch(editFilterValue(filterToChange.name, filterValue));
    }

    dispatch(setOffset(0));

    dispatch(fetchServices());
}

// Transform received filter.json to a structure that can be used for filtering
function transformResponse(data) {
    let filters = {};
    try {
        filters = data;

        for (let key in filters) {
            filters[key].value = "";
            if (!filters[key].options) {
                continue;
            }
            let filterArr = [];
            let options = filters[key].options;
            for (let subKey in options) {
                filterArr.push({value: subKey, label: options[subKey]});
            }
            filterArr = filterArr.sort(function(a,b) {
                if (a.label.toLowerCase() < b.label.toLowerCase()) {
                    return -1;
                }
                if (a.label.toLowerCase() > b.label.toLowerCase()) {
                    return 1;
                }
                return 0;
            });
            filters[key].options = filterArr;
        }
    } catch (e) {  logger.error(e.message);}

    return {filters: filters};
}

// compare function for sort stats array by order property
const compareOrder = (a, b) => {
    if (a.order < b.order) {
        return -1;
    }
    if (a.order > b.order) {
        return 1;
    }
    return 0;
}

