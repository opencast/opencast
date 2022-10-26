/**
 * This file contains selectors regarding details of a certain series
 */
export const getSeriesDetailsMetadata = (state) => state.seriesDetails.metadata;
export const getSeriesDetailsExtendedMetadata = (state) =>
	state.seriesDetails.extendedMetadata;
export const getSeriesDetailsAcl = (state) => state.seriesDetails.acl;
export const getSeriesDetailsFeeds = (state) => state.seriesDetails.feeds;
export const getSeriesDetailsTheme = (state) => state.seriesDetails.theme;
export const getSeriesDetailsThemeNames = (state) =>
	state.seriesDetails.themeNames;

/* selectors for statistics */
export const hasStatistics = (state) =>
	state.seriesDetails.statistics.length > 0;
export const getStatistics = (state) => state.seriesDetails.statistics;
export const hasStatisticsError = (state) =>
	state.seriesDetails.hasStatisticsError;
export const isFetchingStatistics = (state) =>
	state.seriesDetails.fetchingStatisticsInProgress;
