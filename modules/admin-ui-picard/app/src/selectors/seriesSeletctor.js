/**
 * This file contains selectors regarding series
 */

export const getSeries = (state) => state.series.results;
export const getVisibilitySeriesColumns = (state) => state.series.columns;
export const isShowActions = (state) => state.series.showActions;
export const isSeriesDeleteAllowed = (state) => state.series.deletionAllowed;
export const getSeriesHasEvents = (state) => state.series.hasEvents;
export const getSeriesMetadata = (state) => state.series.metadata;
export const getSeriesExtendedMetadata = (state) =>
	state.series.extendedMetadata;
export const getSeriesThemes = (state) => state.series.themes;
export const getTotalSeries = (state) => state.series.total;
