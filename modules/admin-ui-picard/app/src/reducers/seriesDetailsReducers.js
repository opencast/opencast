/**
 * This file contains redux reducer for actions affecting the state of a series
 */
import {
    LOAD_SERIES_DETAILS_FAILURE,
    LOAD_SERIES_DETAILS_IN_PROGRESS,
    LOAD_SERIES_DETAILS_SUCCESS
} from "../actions/seriesDetailsActions";

const initialState = {
    isLoading: false,
    metadata: {},
    feeds: {},
    acl: {}
};

const seriesDetails = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_SERIES_DETAILS_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_SERIES_DETAILS_SUCCESS: {
            const { seriesDetails } = payload;
            return {
                ...state,
                isLoading: false,
                metadata: seriesDetails.metadata,
                feeds: seriesDetails.feeds,
                acl: seriesDetails.acl
            }
        }
        case LOAD_SERIES_DETAILS_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        default:
            return state;
    }
};

export default seriesDetails;
