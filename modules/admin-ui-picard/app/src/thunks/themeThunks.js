import {loadThemesFailure, loadThemesInProgress, loadThemesSuccess} from "../actions/themeActions";
import {getURLParams} from "../utils/resourceUtils";

// fetch themes from server
export const fetchThemes = () => async (dispatch, getState) => {
    try {
        dispatch(loadThemesInProgress());

        const state = getState();
        let params = getURLParams(state);

        // /themes.json?limit=0&offset=0&filter={filter}&sort={sort}
        let data = await fetch('admin-ng/themes/themes.json?' + params);

        const themes = await data.json();
        dispatch(loadThemesSuccess(themes));

    } catch (e) {
        dispatch(loadThemesFailure());
    }
}
