import { createSelector } from "reselect";

/**
 * This file contains selectors regarding the table view
 */

export const getTableRows = (state) => state.table.rows;
export const getTableColumns = (state) => state.table.columns;
export const getTablePagination = (state) => state.table.pagination;
export const getTablePages = (state) => state.table.pages;
export const getTotalItems = (state) => state.table.pagination.totalItems;
export const getPageLimit = (state) => state.table.pagination.limit;
export const getPageOffset = (state) => state.table.pagination.offset;
export const getNumberDirectAccessiblePages = (state) =>
	state.table.pagination.directAccessibleNo;
export const getResourceType = (state) => state.table.resource;
export const getTableSorting = (state) => state.table.sortBy;
export const getTableDirection = (state) => state.table.reverse;
export const getTable = (state) => state.table;
export const getDeactivatedColumns = (state) =>
	state.table.columns.filter((column) => column.deactivated);
export const getActivatedColumns = (state) =>
	state.table.columns.filter((column) => !column.deactivated);

export const getSelectedRows = createSelector(getTableRows, (rows) =>
	rows.filter((row) => row.selected)
);
