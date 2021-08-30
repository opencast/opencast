import axios from "axios";
import {
    loadThemeDetailsFailure,
    loadThemeDetailsInProgress,
    loadThemeDetailsSuccess, loadThemeUsageSuccess
} from "../actions/themeDetailsActions";
import {buildThemeBody} from "../utils/resourceUtils";
import {addNotification} from "./notificationThunks";

// fetch details of certain theme from server
export const fetchThemeDetails = id => async dispatch => {
    try {
        dispatch(loadThemeDetailsInProgress());

        //fetch theme details
        let data = await axios.get(`/admin-ng/themes/${id}.json`);

        let themeDetails = await data.data;

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

// update a certain theme
export const updateThemeDetails = (id, values) => async dispatch => {

    let data = buildThemeBody(values);

    // request for updating
    axios.put(`/admin-ng/themes/${id}`, data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then(response => {
        console.log(response);
        dispatch(addNotification('success', 'THEME_CREATED'));
    }).catch(response => {
        console.log(response);
        dispatch(addNotification('error', 'THEME_NOT_CREATED'));
    });
};
