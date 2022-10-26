import axios from "axios";
import {
	loadGroupsFailure,
	loadGroupsInProgress,
	loadGroupsSuccess,
} from "../actions/groupActions";
import { buildGroupBody, getURLParams } from "../utils/resourceUtils";
import { addNotification } from "./notificationThunks";
import { logger } from "../utils/logger";

// fetch groups from server
export const fetchGroups = () => async (dispatch, getState) => {
	try {
		dispatch(loadGroupsInProgress());

		const state = getState();
		let params = getURLParams(state);

		// /groups.json?limit=0&offset=0&filter={filter}&sort={sort}
		let data = await axios.get("/admin-ng/groups/groups.json", {
			params: params,
		});

		const groups = await data.data;
		dispatch(loadGroupsSuccess(groups));
	} catch (e) {
		dispatch(loadGroupsFailure());
	}
};

// post new group to backend
export const postNewGroup = (values) => async (dispatch) => {
	// get URL params used for post request
	let data = buildGroupBody(values);

	// POST request
	axios
		.post("/admin-ng/groups", data, {
			headers: {
				"Content-Type": "application/x-www-form-urlencoded",
			},
		})
		.then((response) => {
			logger.info(response);
			dispatch(addNotification("success", "GROUP_ADDED"));
		})
		.catch((response) => {
			logger.error(response);
			if (response.status === 409) {
				dispatch(addNotification("error", "GROUP_CONFLICT"));
			} else {
				dispatch(addNotification("error", "GROUP_NOT_SAVED"));
			}
		});
};

export const deleteGroup = (id) => async (dispatch) => {
	// API call for deleting a group
	axios
		.delete(`/admin-ng/groups/${id}`)
		.then((res) => {
			logger.info(res);
			// add success notification
			dispatch(addNotification("success", "GROUP_DELETED"));
		})
		.catch((res) => {
			logger.error(res);
			// add error notification
			dispatch(addNotification("error", "GROUP_NOT_DELETED"));
		});
};
