import axios from "axios";
import {
	loadUsersFailure,
	loadUsersInProgress,
	loadUsersSuccess,
} from "../actions/userActions";
import { buildUserBody, getURLParams } from "../utils/resourceUtils";
import { transformToIdValueArray } from "../utils/utils";
import { addNotification } from "./notificationThunks";
import { logger } from "../utils/logger";

// fetch users from server
export const fetchUsers = () => async (dispatch, getState) => {
	try {
		dispatch(loadUsersInProgress());

		const state = getState();
		let params = getURLParams(state);

		// /users.json?limit=0&offset=0&filter={filter}&sort={sort}
		let data = await axios.get("/admin-ng/users/users.json", {
			params: params,
		});

		const users = await data.data;
		dispatch(loadUsersSuccess(users));
	} catch (e) {
		dispatch(loadUsersFailure());
	}
};

// get users and their user names
export const fetchUsersAndUsernames = async () => {
	let data = await axios.get(
		"/admin-ng/resources/USERS.NAME.AND.USERNAME.json"
	);

	const response = await data.data;

	return transformToIdValueArray(response);
};

// new user to backend
export const postNewUser = (values) => async (dispatch) => {
	// get URL params used for post request
	let data = buildUserBody(values);

	// POST request
	axios
		.post("/admin-ng/users", data, {
			headers: {
				"Content-Type": "application/x-www-form-urlencoded",
			},
		})
		.then((response) => {
			logger.info(response);
			dispatch(addNotification("success", "USER_ADDED"));
		})
		.catch((response) => {
			logger.error(response);
			dispatch(addNotification("error", "USER_NOT_SAVED"));
		});
};

// delete user with provided id
export const deleteUser = (id) => async (dispatch) => {
	// API call for deleting an user
	axios
		.delete(`/admin-ng/users/${id}.json`)
		.then((res) => {
			logger.info(res);
			// add success notification
			dispatch(addNotification("success", "USER_DELETED"));
		})
		.catch((res) => {
			logger.error(res);
			// add error notification
			dispatch(addNotification("error", "USER_NOT_DELETED"));
		});
};
