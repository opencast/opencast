import {recordingsTableConfig} from "../configs/tableConfigs/recordingsTableConfig";
import {LOAD_RECORDINGS_IN_PROGRESS, LOAD_RECORDINGS_SUCCESS} from "../actions/recordingActions";
import {LOAD_SERIES_FAILURE} from "../actions/seriesActions";

const initialColumns = recordingsTableConfig.columns.map(column =>
    ({
    name: column.name,
    deactivated: false
    }));

const initialState = {
    isLoading: false,
    results: [],
    columns: initialColumns,
    total: 0,
    count: 0,
    offset: 0,
    limit: 0
}

const recordings = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case LOAD_RECORDINGS_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case LOAD_RECORDINGS_SUCCESS: {
            const { recordings } = payload;
            return {
                ...state,
                isLoading: false,
                total: recordings.total,
                count: recordings.count,
                limit: recordings.limit,
                offset: recordings.offset,
                results: recordings.results
            }
        }
        case LOAD_SERIES_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        default:
            return state;
    }
}

export default recordings;
