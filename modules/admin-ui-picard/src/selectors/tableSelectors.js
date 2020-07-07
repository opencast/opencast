/**
 * This file contains selectors regarding the table view
 */

export const getTableRows = state => state.table.rows;
export const getTableColumns = state => state.table.columns;
export const getTablePagination = state => state.table.pagination;
export const getTablePages = state => state.table.pages;
