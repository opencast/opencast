import axios from "axios";
import moment from "moment";
import {
	createChartOptions,
	createDownloadUrl,
} from "../utils/statisticsUtils";
import { getHttpHeaders } from "../utils/resourceUtils";
import { logger } from "../utils/logger";
import { getStatistics } from "../selectors/statisticsSelectors";
import {
	loadStatisticsFailure,
	loadStatisticsInProgress,
	loadStatisticsSuccess,
	updateStatisticsFailure,
	updateStatisticsSuccess,
} from "../actions/statisticsActions";

/* thunks for fetching statistics data */

export const fetchStatisticsPageStatistics = (organizationId) => async (
	dispatch
) => {
	dispatch(
		fetchStatistics(
			organizationId,
			"organization",
			getStatistics,
			loadStatisticsInProgress,
			loadStatisticsSuccess,
			loadStatisticsFailure
		)
	);
};

export const fetchStatisticsPageStatisticsValueUpdate = (
	organizationId,
	providerId,
	from,
	to,
	dataResolution,
	timeMode
) => async (dispatch) => {
	dispatch(
		fetchStatisticsValueUpdate(
			organizationId,
			"organization",
			providerId,
			from,
			to,
			dataResolution,
			timeMode,
			getStatistics,
			updateStatisticsSuccess,
			updateStatisticsFailure
		)
	);
};

export const fetchStatistics = (
	resourceId,
	resourceType,
	getStatistics,
	loadStatisticsInProgress,
	loadStatisticsSuccess,
	loadStatisticsFailure
) => async (dispatch, getState) => {
	dispatch(loadStatisticsInProgress());

	// get prior statistics
	const state = getState();
	const statistics = getStatistics(state);

	// create url params
	let params = new URLSearchParams();
	params.append("resourceType", resourceType);

	// get the available statistics providers from API
	axios
		.get("/admin-ng/statistics/providers.json", { params })
		.then((response) => {
			// default values to use, when statistics are viewed the first time
			const originalDataResolution = "monthly";
			const originalTimeMode = "year";
			const originalFrom = moment().startOf(originalTimeMode);
			const originalTo = moment().endOf(originalTimeMode);

			let newStatistics = [];
			const statisticsValueRequest = [];

			// iterate over statistics providers
			for (let i = 0; i < response.data.length; i++) {
				// currently, only time series data can be displayed, for other types, add data directly, then continue
				if (response.data[i].providerType !== "timeSeries") {
					newStatistics.push({
						...response.data[i],
					});
				} else {
					// case: provider is of type time series
					let from;
					let to;
					let timeMode;
					let dataResolution;

					/* if old values for this statistic exist, use old
                    from (date), to (date), timeMode and dataResolution values, otherwise use defaults */
					if (statistics.length > i) {
						from = statistics[i].from;
						to = statistics[i].to;
						timeMode = statistics[i].timeMode;
						dataResolution = statistics[i].dataResolution;
					} else {
						from = originalFrom.format("YYYY-MM-DD");
						to = originalTo.format("YYYY-MM-DD");
						timeMode = originalTimeMode;
						dataResolution = originalDataResolution;
					}

					// create chart options and download url
					const options = createChartOptions(timeMode, dataResolution);
					const csvUrl = createDownloadUrl(
						resourceId,
						resourceType,
						response.data[i].providerId,
						from,
						to,
						dataResolution
					);

					// add provider to statistics list and add statistic settings
					newStatistics.push({
						...response.data[i],
						from: from,
						to: to,
						timeMode: timeMode,
						dataResolution: dataResolution,
						options: options,
						csvUrl: csvUrl,
					});

					// add settings for this statistic of this resource to value request
					statisticsValueRequest.push({
						dataResolution: dataResolution,
						from: moment(from),
						to: moment(to).endOf("day"),
						resourceId: resourceId,
						providerId: response.data[i].providerId,
					});
				}
			}

			// prepare header and data for statistics values request
			const requestHeaders = getHttpHeaders();
			const requestData = new URLSearchParams({
				data: JSON.stringify(statisticsValueRequest),
			});

			// request statistics values from API
			axios
				.post("/admin-ng/statistics/data.json", requestData, requestHeaders)
				.then((dataResponse) => {
					// iterate over value responses
					for (const statisticsValue of dataResponse.data) {
						// get the statistic the response is meant for
						const stat = newStatistics.find(
							(element) => element.providerId === statisticsValue.providerId
						);

						// add values to statistic
						const statistic = {
							...stat,
							values: statisticsValue.values,
							labels: statisticsValue.labels,
							totalValue: statisticsValue.total,
						};

						// put updated statistic into statistics list
						newStatistics = newStatistics.map((oldStat) =>
							oldStat === stat ? statistic : oldStat
						);

						// put statistics list into redux store
						dispatch(loadStatisticsSuccess(newStatistics, false));
					}
				})
				.catch((response) => {
					// put unfinished statistics list into redux store but set flag that an error occurred
					dispatch(loadStatisticsSuccess(newStatistics, true));
					logger.error(response);
				});
		})
		.catch((response) => {
			// getting statistics from API failed
			dispatch(loadStatisticsFailure(true));
			logger.error(response);
		});
};

export const fetchStatisticsValueUpdate = (
	resourceId,
	resourceType,
	providerId,
	from,
	to,
	dataResolution,
	timeMode,
	getStatistics,
	updateStatisticsSuccess,
	updateStatisticsFailure
) => async (dispatch, getState) => {
	// get prior statistics
	const state = getState();
	const statistics = getStatistics(state);

	// settings for this statistic of this resource for value request
	const statisticsValueRequest = [
		{
			dataResolution: dataResolution,
			from: moment(from),
			to: moment(to).endOf("day"),
			resourceId: resourceId,
			providerId: providerId,
		},
	];

	// prepare header and data for statistic values request
	const requestHeaders = getHttpHeaders();
	const requestData = new URLSearchParams({
		data: JSON.stringify(statisticsValueRequest),
	});

	// request statistic values from API
	axios
		.post("/admin-ng/statistics/data.json", requestData, requestHeaders)
		.then((dataResponse) => {
			// if only one element is in the response (as expected), get the response
			if (dataResponse.data.length === 1) {
				const newStatisticData = dataResponse.data[0];

				// get the statistic the response is meant for out of the statistics list
				const stat = statistics.find(
					(element) => element.providerId === providerId
				);

				// get statistic options and download url for new statistic settings
				const options = createChartOptions(timeMode, dataResolution);
				const csvUrl = createDownloadUrl(
					resourceId,
					resourceType,
					providerId,
					from,
					to,
					dataResolution
				);

				// update statistic
				const statistic = {
					...stat,
					from: from,
					to: to,
					dataResolution: dataResolution,
					timeMode: timeMode,
					options: options,
					csvUrl: csvUrl,
					values: newStatisticData.values,
					labels: newStatisticData.labels,
					totalValue: newStatisticData.total,
				};

				// put updated statistic into statistics list
				const newStatistics = statistics.map((oldStat) =>
					oldStat === stat ? statistic : oldStat
				);

				// put updates statistics list into redux store
				dispatch(updateStatisticsSuccess(newStatistics));
			}
		})
		.catch((response) => {
			// getting new statistic values from API failed
			dispatch(updateStatisticsFailure());
			logger.error(response);
		});
};
