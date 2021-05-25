import {loadThemesFailure, loadThemesInProgress, loadThemesSuccess} from "../actions/themeActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";
import {addNotification} from "./notificationThunks";

// fetch themes from server
export const fetchThemes = () => async (dispatch, getState) => {
    try {
        dispatch(loadThemesInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /themes.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await axios.get('admin-ng/themes/themes.json', {params: params});

        const themes = await data.data;
        dispatch(loadThemesSuccess(themes));

    } catch (e) {
        dispatch(loadThemesFailure());
    }
}

// post new theme to backend
export const postNewTheme = async values => {

    // fill form data depending on user inputs
    let data = new URLSearchParams();
    data.append('name', values.name);
    data.append('description', values.description);
    data.append('bumperActive', values.bumperActive);
    if (values.bumperActive) {
        data.append('bumperFile', values.bumperFile.id);
    }
    data.append('trailerActive', values.trailerActive);
    if (values.trailerActive) {
        data.append('trailerFile', values.trailerFile.id);
    }
    data.append('titleSlideActive', values.titleSlideActive);
    if (values.titleSlideActive && values.titleSlideMode === 'upload') {
        data.append('titleSlideBackground', values.titleSlideBackground.id);
    }
    data.append('licenseSlideActive', values.licenseSlideActive);
    data.append('watermarkActive', values.watermarkActive);
    if (values.watermarkActive) {
        data.append('watermarkFile', values.watermarkFile.id);
        data.append('watermarkPosition', values.watermarkPosition);
    }

    // POST request
    axios.post('/admin-ng/themes', data, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }).then(response => console.log(response)).catch(response => console.log(response));
};

export const deleteTheme = id => async dispatch => {
    axios.delete(`/admin-ng/themes/${id}`).then(res => {
        console.log(res);
        // add success notification
        dispatch(addNotification('success', 'THEME_DELETED'));
    }).catch(res => {
        console.log(res);
        // add error notification
        dispatch(addNotification('error', 'THEME_NOT_DELETED'));
    })
}
