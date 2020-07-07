/**
 * This file contains all redux actions that can be executed on the table
 */

export const LOAD_RESOURCE_INTO_TABLE = 'LOAD_RESOURCE_INTO_TABLE';
export const CREATE_PAGE = 'CREATE_PAGE';
export const SORT_TABLE = 'SORT_TABLE';
export const SELECT_ROW = 'SELECT_ROW';
export const SELECT_ALL = 'SELECT_ALL';
export const DESELECT_ALL = 'DESELECT_ALL';
export const RESET_SORT_TABLE = 'RESET_SORT_TABLE';
export const REVERSE_TABLE = 'REVERSE_TABLE';
export const SET_MULTISELECT = 'SET_MULTISELECT';
export const LOAD_COLUMNS = 'LOAD_COLUMNS';
export const UPDATE_PAGESIZE = 'UPDATE_PAGESIZE';

export const loadResourceIntoTable = tableData => ({
   type: LOAD_RESOURCE_INTO_TABLE,
   payload: tableData
});

export const loadColumns = columnData => ({
    type: LOAD_COLUMNS,
    payload: columnData
});

export const createPage = page => ({
    type: CREATE_PAGE,
    payload: page
});

export const selectRow = (id, selected) => ({
   type: SELECT_ROW,
   payload: { id, selected }
});

export const selectAll = ()=> ({
    type: SELECT_ALL
});

export const deselectAll = () => ({
    type: DESELECT_ALL
});

export const updatePageSize = limit => ({
        type: UPDATE_PAGESIZE,
        payload: { limit }
});

//todo: maybe some adjustments necessary, when actually implementing this
export const sortTable = () => ({
    type: SORT_TABLE,
});

//todo: maybe some adjustments necessary, when actually implementing this
export const resetSortTable = () => ({
    type: RESET_SORT_TABLE
});

//todo: maybe some adjustments necessary, when actually implementing this
export const reverseTable = () => ({
   type: REVERSE_TABLE
});

//todo: maybe some adjustments necessary, when actually implementing this
export const setMultiselect = () => ({
   type: SET_MULTISELECT
});
