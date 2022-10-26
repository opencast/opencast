import {
	LOAD_USER_DETAILS_FAILURE,
	LOAD_USER_DETAILS_IN_PROGRESS,
	LOAD_USER_DETAILS_SUCCESS,
} from "../actions/userDetailsActions";

/**
 * This file contains redux reducer for actions affecting the state of details of a user
 */

const initialState = {
	isLoading: false,
	provider: "",
	roles: [],
	name: "",
	username: "",
	email: "",
	manageable: false,
};

// reducer
const userDetails = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_USER_DETAILS_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_USER_DETAILS_SUCCESS: {
			const { userDetails } = payload;
			return {
				...state,
				isLoading: false,
				provider: userDetails.provider,
				roles: userDetails.roles,
				name: userDetails.name,
				username: userDetails.username,
				email: userDetails.email,
				manageable: userDetails.manageable,
			};
		}
		case LOAD_USER_DETAILS_FAILURE: {
			return {
				...state,
				isLoading: false,
				provider: "",
				roles: [],
				name: "",
				username: "",
				email: "",
				manageable: false,
			};
		}
		default:
			return state;
	}
};

export default userDetails;
