import * as t from "../actions/tableActions";
import * as e from "../actions/eventActions";
import * as s from "../selectors/tableSelectors";
import {eventsTableConfig}  from "../configs/tableConfigs/eventsTableConfig";

/**
 * This file contains methods/thunks used to manage the table in the main view and its state changes
 * */

// Method to load events into the table
export const loadEventsIntoTable = () => async (dispatch, getState) => {
    const { events, table } = getState();
    const resource = events.results.map((result) => {
        return {
            ...result,
            selected: false
        }
    });
    console.log('Load Events Results');
    console.log(resource);
    const c = eventsTableConfig.columns;
    const columns = c.map(column => {
        const col = events.columns.find(co => co.name === column.name);
        return {
            ...column,
            deactivated: col.deactivated
        }
    })
    const multiSelect = eventsTableConfig.multiSelect;

    let i, numberOfPages = table.pagination.totalItems / table.pagination.limit;
    const pages = [];
    for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
        pages.push({
            number: i,
            label: (i + 1).toString(),
            active: i === table.pagination.offset
        });
    }


    const tableData = {
        resource: "events",
        rows: resource,
        columns: columns,
        multiSelect: multiSelect,
        pages: pages
    };
    dispatch(t.loadResourceIntoTable(tableData));

}


// Navigate between pages
export const goToPage = pageNumber => (dispatch, getState) => {
    const firstState = getState();
    console.log(firstState.table.pages);
    console.log(pageNumber);

    dispatch(t.deselectAll());
    dispatch(t.setOffset(pageNumber));

    const secondState = getState();
    console.log(secondState.table.pagination.offset);
    console.log(secondState.table.pages[secondState.table.pagination.offset].number);

    dispatch(t.setPageActive(secondState.table.pages[secondState.table.pagination.offset].number));

}

// Update pages for example if page size was changed
export const updatePages = () => (dispatch,getState) => {
    const { table } = getState();

    let i, numberOfPages = table.pagination.totalItems / table.pagination.limit;

    const pages = [];

    for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
        pages.push({
            number: i,
            label: (i + 1).toString(),
            active: i === table.pagination.offset
        });
    }

    dispatch(t.updatePages(pages));
}

// Select all rows on table page
export const changeAllSelected = selected => (dispatch, getState) => {
    const state = getState();

    if (selected) {
        if(s.getResourceType(state) === "events") {
            dispatch(e.showActions(true));
        }
        dispatch(t.selectAll());
    } else {
        if(s.getResourceType(state) === "events") {
            dispatch(e.showActions(false));
        }
        dispatch(t.deselectAll());
    }
}

// Select certain row
export const changeRowSelection = (id, selected) => (dispatch, getState) => {
    dispatch(t.selectRow(id, selected));

    const state = getState();

    if (s.getResourceType(state) === "events"){
        if (s.getSelectedRows(state).length > 0) {
            dispatch(e.showActions(true));
        } else {
            dispatch(e.showActions(false));
        }
    }

}
