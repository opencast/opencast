import {loadSeriesFailure, loadSeriesInProgress, loadSeriesSuccess} from "../actions/seriesActions";
import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

// Todo: implement this
export const fetchSeries = () => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       const state = getState();

       // Todo: Check if empty values problem when using proxy backend
       // Get filter map from state if filter flag is true
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

       // Get sorting from state if sort flag is true
       let sortBy = getTableSorting(state);
       let direction = getTableDirection(state);

       // Get page info needed for fetching events from state
       let pageLimit = getPageLimit(state);
       let offset = getPageOffset(state);

       // todo: maybe sortOrganizer needed
       // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
       const data = await fetch(`admin-ng/series/series.json?sort=${sortBy}:${direction}&filter=${filters}&offset=${offset}&limit=${pageLimit}`);

       const series = await data.json();
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}
