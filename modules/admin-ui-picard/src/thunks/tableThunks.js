import * as t from "../actions/tableActions";
import * as e from "../actions/eventActions";
import * as s from "../selectors/tableSelectors";
import {eventsTableConfig}  from "../configs/tableConfigs/eventsTableConfig";

/**
 * This file contains methods/thunks used to manage the table in the main view and its state changes
 * */

// Method to load events into the table
export const loadEventsIntoTable = () => async (dispatch, getState) => {
    const { events } = getState();
    const pagination = s.getTablePagination(getState());
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

    let i, numberOfPages = resource.length / pagination.limit;
    const pages = [];
    for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
        pages.push({
            number: i,
            label: (i + 1).toString(),
            active: i === pagination.offset
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
    // const firstState = getState();
    // console.log(firstState.table.pages);
    // console.log(pageNumber);

    dispatch(t.deselectAll());
    dispatch(t.setOffset(pageNumber));

    const state = getState();
    const offset = s.getPageOffset(state);
    const pages = s.getTablePages(state);

    console.log(offset);
    console.log(pages[offset].number);

    dispatch(t.setPageActive(pages[offset].number));

}

// Update pages for example if page size was changed
export const updatePages = () => (dispatch,getState) => {

    const pagination = s.getTablePagination(getState());

    let i, numberOfPages = pagination.totalItems / pagination.limit;

    const pages = [];

    for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
        pages.push({
            number: i,
            label: (i + 1).toString(),
            active: i === pagination.offset
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
