/**
 * This file contains selectors regarding table filters
 */

export const getFilters = state => state.tableFilters.data;
export const getStats = state => state.tableFilters.stats;
export const getTextFilter = state => state.tableFilters.textFilter;
export const getSelectedFilter = state => state.tableFilters.selectedFilter;
export const getSecondFilter = state => state.tableFilters.secondFilter;
export const getStartDate = state => state.tableFilters.startDate;
export const getEndDate = state => state.tableFilters.endDate;
