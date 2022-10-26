import {
	LOAD_THEME_DETAILS_FAILURE,
	LOAD_THEME_DETAILS_IN_PROGRESS,
	LOAD_THEME_DETAILS_SUCCESS,
	LOAD_THEME_USAGE_SUCCESS,
} from "../actions/themeDetailsActions";

/**
 * This file contains redux reducer for actions affecting the state of a theme
 */

// Initial state of theme details in redux store
const initialState = {
	isLoading: false,
	details: {},
	usage: {},
};

// Reducer for theme details
const themeDetails = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_THEME_DETAILS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_THEME_DETAILS_SUCCESS: {
			const { themeDetails } = payload;
			return {
				...state,
				isLoading: false,
				details: themeDetails,
			};
		}
		case LOAD_THEME_DETAILS_FAILURE: {
			return {
				...state,
				isLoading: false,
				details: {},
			};
		}
		case LOAD_THEME_USAGE_SUCCESS: {
			const { themeUsage } = payload;
			return {
				...state,
				isLoading: false,
				usage: themeUsage,
			};
		}
		default:
			return state;
	}
};

export default themeDetails;
