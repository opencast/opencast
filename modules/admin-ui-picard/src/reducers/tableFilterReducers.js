import * as tf from "../actions/tableFilterActions";

/**
 * This file contains redux reducer for actions affecting the state of table filters
 */

// Initial state of table filters in redux store
const initialState = {
    isLoading: false,
    data: [],
    filterProfiles: [],
    textFilter: '',
    selectedFilter: '',
    secondFilter: '',
    startDate: '',
    endDate: ''
};

// Reducer for filter profiles
const tableFilters = (state = initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case tf.LOAD_FILTERS_IN_PROGRESS: {
            return {
                ...state,
                isLoading: true
            };
        }
        case tf.LOAD_FILTERS_SUCCESS: {
            const { filters } = payload;
            return {
                ...state,
                isLoading: false,
                data: filters
            };
        }
        case tf.LOAD_FILTERS_FAILURE: {
            return {
                ...state,
                isLoading: false
            };
        }
        case tf.EDIT_FILTER_VALUE: {
            const {filterName, value} = payload;
            return {
                ...state,
                data: state.data.map(filter => {
                    return filter.name === filterName ?
                        {...filter, value: value} : filter
                })
            };
        }
        case tf.RESET_FILTER_VALUES: {
            return {
                ...state,
                data: state.data.map(filter => {
                    return {...filter, value: ''}
                })
            };

        }
        case tf.EDIT_TEXT_FILTER: {
            const { textFilter } = payload;
            return {
                ...state,
                textFilter: textFilter
            };
        }
        case tf.REMOVE_TEXT_FILTER: {
            return {
                ...state,
                textFilter: ''
            };
        }
        case tf.LOAD_FILTER_PROFILE: {
            const { filterMap } = payload;
            return {
                ...state,
                data: filterMap
            }
        }
        case tf.EDIT_SELECTED_FILTER: {
            const { filter } = payload;
            return {
                ...state,
                selectedFilter: filter
            };
        }
        case tf.REMOVE_SELECTED_FILTER: {
            return {
                ...state,
                selectedFilter: ''
            };
        }
        case tf.EDIT_SECOND_FILTER: {
            const { filter } = payload;
            return {
                ...state,
                secondFilter: filter
            };
        }
        case tf.REMOVE_SECOND_FILTER: {
            return {
                ...state,
                secondFilter: ''
            };
        }
        case tf.SET_START_DATE: {
            const { date } = payload;
            return {
                ...state,
                startDate: date
            };
        }
        case tf.SET_END_DATE: {
            const { date } = payload;
            return {
                ...state,
                endDate: date
            };
        }
        case tf.RESET_START_DATE: {
            return {
                ...state,
                startDate: ''
            };
        }
        case tf.RESET_END_DATE: {
            return {
                ...state,
                endDate: ''
            };
        }
        default:
            return state;

    }
}

export default tableFilters;
