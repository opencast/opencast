/**
 * This file contains all redux actions that can be executed on the table
 */

// Constants of of actions types concerning table and its management
export const LOAD_RESOURCE_INTO_TABLE = "LOAD_RESOURCE_INTO_TABLE";
export const SORT_TABLE = "SORT_TABLE";
export const SELECT_ROW = "SELECT_ROW";
export const SELECT_ALL = "SELECT_ALL";
export const DESELECT_ALL = "DESELECT_ALL";
export const RESET_SORT_TABLE = "RESET_SORT_TABLE";
export const REVERSE_TABLE = "REVERSE_TABLE";
export const SET_SORT_BY = "SET_SORT_BY";
export const LOAD_COLUMNS = "LOAD_COLUMNS";

// Constants of of actions types concerning pagination
export const CREATE_PAGE = "CREATE_PAGE";
export const UPDATE_PAGESIZE = "UPDATE_PAGESIZE";
export const SET_PAGES = "UPDATE_PAGES";
export const SET_TOTAL_ITEMS = "SET_TOTAL_ITEMS";
export const SET_OFFSET = "SET_OFFSET";
export const SET_DIRECT_ACCESSIBLE_PAGES = "SET_DIRECT_ACCESSIBLE_PAGES";
export const SET_PAGE_ACTIVE = "SET_PAGE_ACTIVE";

// Actions affecting table directly

export const loadResourceIntoTable = (tableData) => ({
	type: LOAD_RESOURCE_INTO_TABLE,
	payload: tableData,
});

export const loadColumns = (columnData) => ({
	type: LOAD_COLUMNS,
	payload: columnData,
});

export const selectRow = (id, selected) => ({
	type: SELECT_ROW,
	payload: { id, selected },
});

export const selectAll = () => ({
	type: SELECT_ALL,
});

export const deselectAll = () => ({
	type: DESELECT_ALL,
});

export const reverseTable = (order) => ({
	type: REVERSE_TABLE,
	payload: { order },
});

export const setSortBy = (column) => ({
	type: SET_SORT_BY,
	payload: { column },
});

// Actions affecting pagination of table

export const createPage = (page) => ({
	type: CREATE_PAGE,
	payload: page,
});

export const updatePageSize = (limit) => ({
	type: UPDATE_PAGESIZE,
	payload: { limit },
});

export const setPages = (pages) => ({
	type: SET_PAGES,
	payload: { pages },
});

export const setTotalItems = (totalItems) => ({
	type: SET_TOTAL_ITEMS,
	payload: { totalItems },
});

export const setOffset = (offset) => ({
	type: SET_OFFSET,
	payload: { offset },
});

export const setDirectAccessiblePages = (directAccessible) => ({
	type: SET_DIRECT_ACCESSIBLE_PAGES,
	payload: { directAccessible },
});

export const setPageActive = (pageNumber) => ({
	type: SET_PAGE_ACTIVE,
	payload: { pageNumber },
});
