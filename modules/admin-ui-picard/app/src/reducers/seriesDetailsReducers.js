/**
 * This file contains redux reducer for actions affecting the state of a series
 */
import {
    LOAD_SERIES_DETAILS_ACLS_SUCCESS,
    LOAD_SERIES_DETAILS_FAILURE,
    LOAD_SERIES_DETAILS_FEEDS_SUCCESS,
    LOAD_SERIES_DETAILS_IN_PROGRESS,
    LOAD_SERIES_DETAILS_METADATA_SUCCESS,
    LOAD_SERIES_DETAILS_THEME_NAMES_FAILURE,
    LOAD_SERIES_DETAILS_THEME_NAMES_IN_PROGRESS,
    LOAD_SERIES_DETAILS_THEME_NAMES_SUCCESS,
    LOAD_SERIES_DETAILS_THEME_SUCCESS,
    SET_SERIES_DETAILS_EXTENDED_METADATA,
    SET_SERIES_DETAILS_METADATA,
    SET_SERIES_DETAILS_THEME,
} from "../actions/seriesDetailsActions";

// Initial state of series details in redux store
const initialState = {
    isLoading: false,
    metadata: {},
    extendedMetadata: [],
    feeds: {},
    acl: {},
    theme: '',
    themeNames: []
};

// Reducer for series details
const seriesDetails = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_SERIES_DETAILS_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_SERIES_DETAILS_METADATA_SUCCESS: {
            const { seriesMetadata, extendedMetadata } = payload;
            return {
                ...state,
                isLoading: false,
                metadata: seriesMetadata,
                extendedMetadata: extendedMetadata
            }
        }
        case LOAD_SERIES_DETAILS_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        case LOAD_SERIES_DETAILS_ACLS_SUCCESS: {
            const { seriesAcls } = payload;
            return {
                ...state,
                isLoading: false,
                acl: seriesAcls
            }
        }
        case LOAD_SERIES_DETAILS_FEEDS_SUCCESS: {
            const { seriesFeeds } = payload;
            return {
                ...state,
                isLoading: false,
                feeds: seriesFeeds
            }
        }
        case LOAD_SERIES_DETAILS_THEME_SUCCESS: {
            const { seriesTheme } = payload;
            return {
                ...state,
                isLoading: false,
                theme: seriesTheme
            }
        }
        case LOAD_SERIES_DETAILS_THEME_NAMES_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_SERIES_DETAILS_THEME_NAMES_SUCCESS: {
            const { themeNames } = payload;
            return {
                ...state,
                isLoading: false,
                themeNames: themeNames
            }
        }
        case LOAD_SERIES_DETAILS_THEME_NAMES_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        case SET_SERIES_DETAILS_THEME: {
            const { seriesTheme } = payload;
            return {
                ...state,
                theme: seriesTheme
            }
        }
        case SET_SERIES_DETAILS_METADATA: {
            const { seriesMetadata } = payload;
            return {
                ...state,
                metadata: seriesMetadata
            }
        }
        case SET_SERIES_DETAILS_EXTENDED_METADATA: {
            const { seriesMetadata } = payload;

            return {
                ...state,
                extendedMetadata: seriesMetadata
            }
        }
        default:
            return state;
    }
};

export default seriesDetails;
