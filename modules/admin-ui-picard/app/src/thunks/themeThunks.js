import {loadThemesFailure, loadThemesInProgress, loadThemesSuccess} from "../actions/themeActions";
import {getURLParams} from "../utils/resourceUtils";
import axios from "axios";

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
