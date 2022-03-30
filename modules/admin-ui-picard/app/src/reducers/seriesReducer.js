import {seriesTableConfig} from "../configs/tableConfigs/seriesTableConfig";
import {
    LOAD_SERIES_FAILURE,
    LOAD_SERIES_IN_PROGRESS,
    LOAD_SERIES_METADATA_FAILURE,
    LOAD_SERIES_METADATA_IN_PROGRESS,
    LOAD_SERIES_METADATA_SUCCESS,
    LOAD_SERIES_SUCCESS, LOAD_SERIES_THEMES_FAILURE, LOAD_SERIES_THEMES_IN_PROGRESS, LOAD_SERIES_THEMES_SUCCESS,
    SHOW_ACTIONS_SERIES
} from "../actions/seriesActions";

/**
 * This file contains redux reducer for actions affecting the state of series
 */

// Fill columns initially with columns defined in seriesTableConfig
const initialColumns = seriesTableConfig.columns.map(column =>
    ({
        name: column.name,
        deactivated: false
    }));

// Initial state of series in redux store
const initialState = {
    isLoading: false,
    results: [],
    columns: initialColumns,
    showActions: false,
    total: 0,
    count: 0,
    offset: 0,
    limit: 0,
    metadata: {},
    themes: {}
}

// Reducer for series
const series = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_SERIES_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_SERIES_SUCCESS: {
            const { series } = payload;
            return {
                ...state,
                isLoading: false,
                total: series.total,
                count: series.count,
                limit: series.limit,
                offset: series.offset,
                results: series.results

            }
        }
        case LOAD_SERIES_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        case SHOW_ACTIONS_SERIES: {
            const { isShowing } = payload;
            return {
                ...state,
                showActions: isShowing
            }
        }
        case LOAD_SERIES_METADATA_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_SERIES_METADATA_SUCCESS: {
            const { metadata } = payload;
            return {
                ...state,
                isLoading: false,
                metadata: metadata
            }
        }
        case LOAD_SERIES_METADATA_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        case LOAD_SERIES_THEMES_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_SERIES_THEMES_SUCCESS: {
            const { themes } = payload;
            return {
                ...state,
                isLoading: false,
                themes: themes
            }
        }
        case LOAD_SERIES_THEMES_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        default:
            return state;
    }
}

export default series;
