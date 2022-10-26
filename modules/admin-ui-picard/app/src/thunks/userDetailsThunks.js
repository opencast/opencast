import axios from "axios";
import {
	loadUserDetailsFailure,
	loadUserDetailsInProgress,
	loadUserDetailsSuccess,
} from "../actions/userDetailsActions";
import { addNotification } from "./notificationThunks";
import { buildUserBody } from "../utils/resourceUtils";
import { logger } from "../utils/logger";

// fetch details about certain user from server
export const fetchUserDetails = (username) => async (dispatch) => {
	try {
		dispatch(loadUserDetailsInProgress());

		let data = await axios.get(`/admin-ng/users/${username}.json`);

		const response = await data.data;

		const userDetails = {
			...response,
			email: !!response.email ? response.email : "",
		};

		dispatch(loadUserDetailsSuccess(userDetails));
	} catch (e) {
		logger.error(e);
		dispatch(loadUserDetailsFailure());
	}
};

// update existing user with changed values
export const updateUserDetails = (values, username) => async (dispatch) => {
	// get URL params used for put request
	let data = buildUserBody(values);

	// PUT request
	axios
		.put(`/admin-ng/users/${username}.json`, data)
		.then((response) => {
			logger.info(response);
			dispatch(addNotification("success", "USER_UPDATED"));
		})
		.catch((response) => {
			logger.error(response);
			dispatch(addNotification("error", "USER_NOT_SAVED"));
		});
};
