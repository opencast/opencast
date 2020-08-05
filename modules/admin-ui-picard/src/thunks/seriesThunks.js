import {loadSeriesFailure, loadSeriesInProgress, loadSeriesSuccess} from "../actions/seriesActions";
import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

const data = {
    "total": 2,
    "offset": 0,
    "count": 2,
    "limit": 100,
    "results": [
    {
        "license": "CC0",
        "createdBy": "Opencast Project Administrator",
        "organizers": [
            "Ophelia Organizer"
        ],
        "language": "eng",
        "id": "edf05757-34b1-42b8-8821-e3302b663972",
        "contributors": [
            "Carl Contributor",
            "Carmen Contributor"
        ],
        "creation_date": "2018-08-13T07:20:41Z",
        "title": "Mock Series"
    },
    {
        "license": "CC-BY-ND",
        "createdBy": "Opencast Project Administrator",
        "organizers": [
            "Olaf Organizer"
        ],
        "language": "nld",
        "id": "73f9b7ab-1d8f-4c75-9da1-ceb06736d82c",
        "contributors": [
            "Carmen Contributor"
        ],
        "creation_date": "2018-08-13T08:58:18Z",
        "title": "Mock Series 2"
    }
]};


// Todo: implement this
export const fetchSeries = (filter, sort) => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       console.log('Filters in series thunk: ');
       console.log(filter);

       const state = getState();

       // Get filter map from state if filter flag is true
       let filterMap = null;
       if (filter) {
           filterMap = getFilters(state);
       }

       // Get sorting from state if sort flag is true
       let sortBy, direction = null;
       if (sort) {
           sortBy = getTableSorting(state);
           direction = getTableDirection(state);
       }

       // Get page info needed for fetching events from state
       let pageLimit = getPageLimit(state);
       let offset = getPageOffset(state);

       //TODO: Fetch actual data from server
       //Todo: maybe some Transfromations for publication needed
       //const response = JSON.parse(data);
       const response = data;

       const series = response;
       console.log("Series in Thunk:");
       console.log(series);
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}
