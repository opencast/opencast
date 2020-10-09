import { loadFiltersSuccess, loadFiltersFailure, loadFiltersInProgress} from '../actions/tableFilterActions';
/**
* This file contains methods/thunks used to query the REST-API of Opencast to get the filters of a certain resource type.
* This information is used to filter the entries of the table in the main view.
*
* Currently only a mock json containing filters of events is returned.
*
* */
// Fetch table filters from opencast instance and transform them for further use
export const fetchFilters = resource => async dispatch => {
    try {
        dispatch(loadFiltersInProgress());
        //TODO: Fetch the actual data from server
        let response;
        if (resource === 'events') {
            const data = await fetch('admin-ng/resources/events/filters.json');
            const eventsData =  await data.json();

            response = transformResponse(eventsData);
        }
        if (resource === 'series') {
            const data = await fetch('admin-ng/resources/series/filters.json');
            const seriesData =  await data.json();

            response = transformResponse(seriesData);
        }
        if (resource === 'recordings') {
            const data = await fetch('admin-ng/resources/recordings/filters.json');
            const recordingsData = await data.json();

            response = transformResponse(recordingsData);
        }

        if (resource === 'jobs') {
            const data = await fetch('admin-ng/resources/jobs/filters.json');
            const jobsData = await data.json();

            response = transformResponse(jobsData);
        }
        const filters = response
        const filtersList = Object.keys(filters.filters).map(key => {
            let filter = filters.filters[key];
            filter.name = key;
            return filter;
        });
        dispatch(loadFiltersSuccess(filtersList));
    } catch (e) {
        dispatch(loadFiltersFailure());
        console.log(e);
    }
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
    } catch (e) {  console.log(e.message);}
    console.log(filters);
    return {filters: filters};
}

