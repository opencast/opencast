import {loadSeriesFailure, loadSeriesInProgress, loadSeriesSuccess} from "../actions/seriesActions";
import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

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
       const data = await fetch('admin-ng/series/series.json');

       const response =  await data.json();

       const series = response;
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}
