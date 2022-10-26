import { themesTableConfig } from "../configs/tableConfigs/themesTableConfig";
import {
	LOAD_THEMES_FAILURE,
	LOAD_THEMES_IN_PROGRESS,
	LOAD_THEMES_SUCCESS,
	SET_THEME_COLUMNS,
} from "../actions/themeActions";

/**
 * This file contains redux reducer for actions affecting the state of themes
 */

// Fill columns initially with columns defined in themesTableConfig
const initialColumns = themesTableConfig.columns.map((column) => ({
	...column,
	deactivated: false,
}));

// Initial state of themes in redux store
const initialState = {
	isLoading: false,
	results: [],
	columns: initialColumns,
	total: 0,
	count: 0,
	offset: 0,
	limit: 0,
};

// Reducer for themes
const themes = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_THEMES_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_THEMES_SUCCESS: {
			const { themes } = payload;
			return {
				...state,
				isLoading: false,
				total: themes.total,
				count: themes.count,
				limit: themes.limit,
				offset: themes.offset,
				results: themes.results,
			};
		}
		case LOAD_THEMES_FAILURE: {
			return {
				...state,
				isLoading: false,
			};
		}
		case SET_THEME_COLUMNS: {
			const { updatedColumns } = payload;
			return {
				...state,
				columns: updatedColumns,
			};
		}
		default:
			return state;
	}
};

export default themes;
