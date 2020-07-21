import * as e from '../actions/eventActions';
import {eventsTableConfig} from "../configs/tableConfigs/eventsTableConfig";

const initialColumns = eventsTableConfig.columns.map(column =>
    ({
    name: column.name,
        deactivated: false
    }));


const initialState = {
    isLoading: false,
    total: 0,
    count: 0,
    limit: 0,
    offset: 0,
    results: [],
    columns: initialColumns,
    showActions: false
}

const events = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case e.LOAD_EVENTS_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            }
        }
        case e.LOAD_EVENTS_SUCCESS: {
            const { events } = payload;
            return {
                ...state,
                isLoading: false,
                total: events.total,
                count: events.count,
                limit: events.limit,
                offset: events.offset,
                results: events.results
            }
        }
        case e.LOAD_EVENTS_FAILURE: {
            return {
                ...state,
                isLoading: false
            }
        }
        case e.SHOW_ACTIONS: {
            const { isShowing } = payload;
            return {
                ...state,
                showActions: isShowing
            }
        }
        default:
            return state;
    }
};

export default events;
