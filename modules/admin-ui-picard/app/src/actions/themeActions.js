/**
 * This file contains all redux actions that can be executed on themes
 */

// Constants of action types for fetching themes from server
export const LOAD_THEMES_IN_PROGRESS = "LOAD_THEMES_IN_PROGRESS";
export const LOAD_THEMES_SUCCESS = "LOAD_THEMES_SUCCESS";
export const LOAD_THEMES_FAILURE = "LOAD_THEMES_FAILURE";

// Constants of action types affecting UI
export const SET_THEME_COLUMNS = "SET_THEME_COLUMNS";

// Actions affecting fetching themes from server

export const loadThemesInProgress = () => ({
	type: LOAD_THEMES_IN_PROGRESS,
});

export const loadThemesSuccess = (themes) => ({
	type: LOAD_THEMES_SUCCESS,
	payload: { themes },
});

export const loadThemesFailure = () => ({
	type: LOAD_THEMES_FAILURE,
});

// Actions affecting UI

export const setThemeColumns = (updatedColumns) => ({
	type: SET_THEME_COLUMNS,
	payload: { updatedColumns },
});
