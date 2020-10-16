import {loadSeriesFailure, loadSeriesInProgress, loadSeriesSuccess} from "../actions/seriesActions";
import {getURLParams} from "../utils/resourceUtils";

// fetch series from server
export const fetchSeries = () => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       const state = getState();
       let params = getURLParams(state);

       // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
       let data = await fetch('admin-ng/series/series.json?' + params);


       const series = await data.json();
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}
