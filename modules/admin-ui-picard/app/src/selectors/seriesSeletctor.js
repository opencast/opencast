/**
 * This file contains selectors regarding series
 */

export const getSeries = state => state.series.results;
export const getVisibilitySeriesColumns = state => state.series.columns;
export const isShowActions = state => state.series.showActions;
