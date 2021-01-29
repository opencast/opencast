import {loadSeriesFailure, loadSeriesInProgress, loadSeriesSuccess} from "../actions/seriesActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

// fetch series from server
export const fetchSeries = () => async (dispatch, getState) => {
   try {
       dispatch(loadSeriesInProgress());

       const state = getState();
       let params = getURLParams(state);

       // /series.json?sortorganizer={sortorganizer}&sort={sort}&filter={filter}&offset=0&limit=100
       let data = await axios.get('admin-ng/series/series.json', { params: params });


       const series = await data.data;
       dispatch(loadSeriesSuccess(series));

   } catch (e) {
       dispatch(loadSeriesFailure());
       console.log(e);
   }
}
