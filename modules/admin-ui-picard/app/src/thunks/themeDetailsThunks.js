import axios from "axios";
import {
    loadThemeDetailsFailure,
    loadThemeDetailsInProgress,
    loadThemeDetailsSuccess, loadThemeUsageSuccess
} from "../actions/themeDetailsActions";

// fetch details of certain theme from server
export const fetchThemeDetails = id => async dispatch => {
    try {
        dispatch(loadThemeDetailsInProgress());

        //fetch theme details
        let data = await axios.get(`/admin-ng/themes/${id}`);

        const themeDetails = await data.data;

        dispatch(loadThemeDetailsSuccess(themeDetails));

    } catch (e) {
        dispatch(loadThemeDetailsFailure());
    }
}

// fetch usage of a certain theme
export const fetchUsage = id => async dispatch => {
    try {
        dispatch(loadThemeDetailsInProgress());

        let data = await axios.get(`admin-ng/themes/${id}/usage.json`);

        const themeUsage = await data.data;

        dispatch(loadThemeUsageSuccess(themeUsage));
    } catch (e) {
        dispatch(loadThemeDetailsFailure());
    }
}
