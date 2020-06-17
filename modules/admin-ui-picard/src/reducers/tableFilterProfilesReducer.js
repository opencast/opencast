import * as tfp from "../actions/tableFilterProfilesActions";

/**
 * This file contains redux reducer for actions affecting the state of table filter profiles
 */

// Initial state of filter profiles in redux store
const initialState = {
    profiles: []
};

// Reducer for filter profiles
const tableFilterProfiles = (state = initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case tfp.CREATE_FILTER_PROFILE: {
            const { filterProfile } = payload;
            return {
                ...state,
                profiles: state.profiles.concat(filterProfile)
            };
        }
        case tfp.EDIT_FILTER_PROFILE: {
            const { filterProfile: updatedFilterProfile } = payload;
            return {
                ...state,
                profiles: state.profiles.map(filterProfile => {
                    if (filterProfile.name === updatedFilterProfile.name) {
                        return updatedFilterProfile;
                    }
                    return filterProfile;
                })
            };
        }
        case tfp.REMOVE_FILTER_PROFILE: {
            const { filterProfile: filterProfileToRemove } = payload;
            return {
                ...state,
                profiles: state.profiles.filter(
                    filterProfile => filterProfile.name !== filterProfileToRemove.name
                )
            };
        }
        case tfp.CANCEL_EDITING_FILTER_PROFILE: {
            return state;
        }
        default:
            return state;

    }
}

export default tableFilterProfiles;
