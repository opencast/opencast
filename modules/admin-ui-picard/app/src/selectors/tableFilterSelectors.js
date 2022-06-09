/**
 * This file contains selectors regarding table filters
 */

export const getFilters = state => state.tableFilters.data;
export const getStats = state => state.tableFilters.stats;
export const getTextFilter = state => state.tableFilters.textFilter;
export const getSelectedFilter = state => state.tableFilters.selectedFilter;
export const getSecondFilter = state => state.tableFilters.secondFilter;
export const getCurrentFilterResource = state => state.tableFilters.currentResource;
