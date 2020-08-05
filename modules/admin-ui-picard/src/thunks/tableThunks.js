import {
    deselectAll,
    loadResourceIntoTable,
    selectAll,
    selectRow,
    setOffset,
    setPageActive, setPages
} from "../actions/tableActions";
import {showActions as showEventsActions} from "../actions/eventActions";
import {showActions as showSeriesActions} from "../actions/seriesActions";
import {
    getPageOffset,
    getResourceType,
    getSelectedRows,
    getTablePages,
    getTablePagination
} from "../selectors/tableSelectors";
import {eventsTableConfig}  from "../configs/tableConfigs/eventsTableConfig";
import {seriesTableConfig} from "../configs/tableConfigs/seriesTableConfig";
import {fetchEvents} from "./eventThunks";
import {fetchSeries} from "./seriesThunks";

/**
 * This file contains methods/thunks used to manage the table in the main view and its state changes
 * */

// Method to load events into the table
export const loadEventsIntoTable = () => async (dispatch, getState) => {
    const { events } = getState();
    const pagination = getTablePagination(getState());
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
    dispatch(loadResourceIntoTable(tableData));

}

// Method to load series into the table
export const loadSeriesIntoTable = () => (dispatch, getState) => {
    const { series } = getState();
    const pagination = getTablePagination(getState());
    const resource = series.results.map((result) => {
        return {
            ...result,
            selected: false
        }
    });
    console.log('Load Series Results: ');
    console.log(resource);
    const c = seriesTableConfig.columns;
    const columns = c.map(column => {
        const col = series.columns.find(co => co.name === column.name);
        return {
            ...column,
            deactivated: col.deactivated
        }
    });
    const multiSelect = seriesTableConfig.multiSelect;

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
        resource: "series",
        rows: resource,
        columns: columns,
        multiSelect: multiSelect,
        pages: pages
    };
    dispatch(loadResourceIntoTable(tableData));
};

// Navigate between pages
export const goToPage = pageNumber => (dispatch, getState) => {
    // const firstState = getState();
    // console.log(firstState.table.pages);
    // console.log(pageNumber);

    dispatch(deselectAll());
    dispatch(setOffset(pageNumber));

    const state = getState();
    const offset = getPageOffset(state);
    const pages = getTablePages(state);

    console.log(offset);
    console.log(pages[offset].number);

    dispatch(setPageActive(pages[offset].number));

    // Get resources of page and load them into table
    // Load events if resource is events
    if (getResourceType(state) === "events") {
        dispatch(fetchEvents(false, false));
        dispatch(loadEventsIntoTable());
    }
    // Load series if resource is series
    if(getResourceType(state) === "series") {
        dispatch(fetchSeries(false, false));
        dispatch(loadSeriesIntoTable());
    }
    // todo: Check for all other type of resource

}

// Update pages for example if page size was changed
export const updatePages = () => (dispatch,getState) => {
    const state = getState();

    const pagination = getTablePagination(state);

    let i, numberOfPages = pagination.totalItems / pagination.limit;

    const pages = [];

    for (i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
        pages.push({
            number: i,
            label: (i + 1).toString(),
            active: i === pagination.offset
        });
    }

    dispatch(setPages(pages));

    // Get resources of page and load them into table
    // Load events if resource is events
    if (getResourceType(state) === "events") {
        dispatch(fetchEvents(false, false));
        dispatch(loadEventsIntoTable());
    }
    // Load series if resource is series
    if(getResourceType(state) === "series") {
        dispatch(fetchSeries(false, false));
        dispatch(loadSeriesIntoTable());
    }
    // todo: Check for all other type of resource
}

// Select all rows on table page
export const changeAllSelected = selected => (dispatch, getState) => {
    const state = getState();

    if (selected) {
        if(getResourceType(state) === "events") {
            dispatch(showEventsActions(true));
        }
        if(getResourceType(state) === "series") {
            dispatch(showSeriesActions(true));
        }
        dispatch(selectAll());
    } else {
        if(getResourceType(state) === "events") {
            dispatch(showEventsActions(false));
        }
        if(getResourceType(state) === "series") {
            dispatch(showSeriesActions(false));
        }
        dispatch(deselectAll());
    }
}

// Select certain row
export const changeRowSelection = (id, selected) => (dispatch, getState) => {

    dispatch(selectRow(id, selected));

    const state = getState();

    if (getResourceType(state) === "events"){
        if (getSelectedRows(state).length > 0) {
            dispatch(showEventsActions(true));
        } else {
            dispatch(showEventsActions(false));
        }
    }

    if (getResourceType(state) === "series"){
        if (getSelectedRows(state).length > 0) {
            dispatch(showSeriesActions(true));
        } else {
            dispatch(showSeriesActions(false));
        }
    }

}
