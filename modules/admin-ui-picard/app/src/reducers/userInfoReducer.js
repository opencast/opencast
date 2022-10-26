import {
	LOAD_OC_VERSION_FAILURE,
	LOAD_OC_VERSION_IN_PROGRESS,
	LOAD_OC_VERSION_SUCCESS,
	LOAD_USER_INFO_FAILURE,
	LOAD_USER_INFO_IN_PROGRESS,
	LOAD_USER_INFO_SUCCESS,
} from "../actions/userInfoActions";

/**
 * This file contains redux reducer for actions affecting the state of information about current user
 */

const initialState = {
	isLoading: false,
	isAdmin: false,
	isOrgAdmin: false,
	org: {},
	roles: [],
	userRole: "",
	user: {},
	ocVersion: {},
};

// reducer
const userInfo = (state = initialState, action) => {
	const { type, payload } = action;
	switch (type) {
		case LOAD_USER_INFO_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_USER_INFO_SUCCESS: {
			const { userInfo } = payload;
			return {
				...state,
				isLoading: false,
				isAdmin: userInfo.isAdmin,
				isOrgAdmin: userInfo.isOrgAdmin,
				org: userInfo.org,
				roles: userInfo.roles,
				userRole: userInfo.userRole,
				user: userInfo.user,
			};
		}
		case LOAD_USER_INFO_FAILURE: {
			return {
				...state,
				isLoading: false,
				org: {},
				roles: [],
				userRole: "",
				user: {},
			};
		}
		case LOAD_OC_VERSION_IN_PROGRESS: {
			return {
				...state,
				isLoading: true,
			};
		}
		case LOAD_OC_VERSION_SUCCESS: {
			const { ocVersion } = payload;
			return {
				...state,
				isLoading: false,
				ocVersion: ocVersion,
			};
		}
		case LOAD_OC_VERSION_FAILURE: {
			return {
				...state,
				isLoading: false,
				ocVersion: {},
			};
		}
		default:
			return state;
	}
};

export default userInfo;
