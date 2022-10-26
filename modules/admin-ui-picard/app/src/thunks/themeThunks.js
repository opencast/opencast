import {
	loadThemesFailure,
	loadThemesInProgress,
	loadThemesSuccess,
} from "../actions/themeActions";
import { buildThemeBody, getURLParams } from "../utils/resourceUtils";
import axios from "axios";
import { addNotification } from "./notificationThunks";
import { logger } from "../utils/logger";

// fetch themes from server
export const fetchThemes = () => async (dispatch, getState) => {
	try {
		dispatch(loadThemesInProgress());

		const state = getState();
		let params = getURLParams(state);

		// /themes.json?limit=0&offset=0&filter={filter}&sort={sort}
		let data = await axios.get("/admin-ng/themes/themes.json", {
			params: params,
		});

		const themes = await data.data;
		dispatch(loadThemesSuccess(themes));
	} catch (e) {
		dispatch(loadThemesFailure());
	}
};

// post new theme to backend
export const postNewTheme = (values) => async (dispatch) => {
	let data = buildThemeBody(values);

	// POST request
	axios
		.post("/admin-ng/themes", data, {
			headers: {
				"Content-Type": "application/x-www-form-urlencoded",
			},
		})
		.then((response) => {
			logger.info(response);
			dispatch(addNotification("success", "THEME_CREATED"));
		})
		.catch((response) => {
			logger.error(response);
			dispatch(addNotification("error", "THEME_NOT_CREATED"));
		});
};

export const deleteTheme = (id) => async (dispatch) => {
	axios
		.delete(`/admin-ng/themes/${id}`)
		.then((res) => {
			logger.info(res);
			// add success notification
			dispatch(addNotification("success", "THEME_DELETED"));
		})
		.catch((res) => {
			logger.error(res);
			// add error notification
			dispatch(addNotification("error", "THEME_NOT_DELETED"));
		});
};
