import axios from "axios";
import {
	loadJobsFailure,
	loadJobsInProgress,
	loadJobsSuccess,
} from "../actions/jobActions";
import { getURLParams } from "../utils/resourceUtils";
import { logger } from "../utils/logger";

// fetch jobs from server
export const fetchJobs = () => async (dispatch, getState) => {
	try {
		dispatch(loadJobsInProgress());

		const state = getState();
		let params = getURLParams(state);

		// /jobs.json?limit=0&offset=0&filter={filter}&sort={sort}
		let data = await axios.get("/admin-ng/job/jobs.json?", { params: params });

		const jobs = await data.data;
		dispatch(loadJobsSuccess(jobs));
	} catch (e) {
		logger.error(e);
		dispatch(loadJobsFailure());
	}
};
