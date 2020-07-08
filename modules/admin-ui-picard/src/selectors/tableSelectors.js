/**
 * This file contains selectors regarding the table view
 */

export const getTableRows = state => state.table.rows;
export const getTableColumns = state => state.table.columns;
export const getTablePagination = state => state.table.pagination;
export const getTablePages = state => state.table.pages;
export const getTotalItems = state => state.table.pagination.totalItems;
export const getPageLimit = state => state.table.pagination.limit;
export const getPageOffset = state => state.table.pagination.offset;
export const getNumberDirectAccessiblePages = state => state.table.pagination.directAccessibleNo;
