/* selectors for statistics page */
export const hasStatistics = (state) => state.statistics.statistics.length > 0;
export const getStatistics = (state) => state.statistics.statistics;
export const hasStatisticsError = (state) =>
	state.statistics.hasStatisticsError;
export const isFetchingStatistics = (state) =>
	state.statistics.fetchingStatisticsInProgress;
