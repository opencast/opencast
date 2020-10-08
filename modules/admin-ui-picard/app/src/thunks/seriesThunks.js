import {loadSeriesFailure, loadSeriesInProgress, loadSeriesSuccess} from "../actions/seriesActions";
import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

// Todo: implement this
export const fetchSeries = () => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       const state = getState();

       // Todo: Check if empty values problem when using proxy backend
       // Get filter map from state
       let filters;
       let filterArray = [];
       let filterMap = getFilters(state);
       for (let key in filterMap) {
           if (!!filterMap[key].value) {
               filterArray.push(filterMap[key].name + ':' + filterMap[key].value);
           }
       }
       if (filterArray.length) {
           filters = filterArray.join(',');
       }
       console.log(filters);

       // Get sorting from state
       let sort = getTableSorting(state) + ':' + getTableDirection(state);

       // Get page info needed for fetching series from state
       let pageLimit = getPageLimit(state);
       let offset = getPageOffset(state);

       let data;

       // todo: maybe sortOrganizer needed
       if (typeof filters == "undefined") {
           // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
           data = await fetch('admin-ng/series/series.json?' + new URLSearchParams({
               sort: sort,
               limit: pageLimit,
               offset: offset
           }));
       } else {
           // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
           data = await fetch('admin-ng/series/series.json?' + new URLSearchParams({
               filter: filters,
               sort: sort,
               limit: pageLimit,
               offset: offset
           }));
       }


       const series = await data.json();
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}
