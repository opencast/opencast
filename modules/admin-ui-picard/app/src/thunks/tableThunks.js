import {eventsTableConfig} from "../configs/tableConfigs/eventsTableConfig";
import {seriesTableConfig} from "../configs/tableConfigs/seriesTableConfig";
import {recordingsTableConfig} from "../configs/tableConfigs/recordingsTableConfig";
import {jobsTableConfig} from "../configs/tableConfigs/jobsTableConfig";
import {serversTableConfig} from "../configs/tableConfigs/serversTableConfig";
import {servicesTableConfig} from "../configs/tableConfigs/servicesTableConfig";
import {usersTableConfig} from "../configs/tableConfigs/usersTableConfig";
import {groupsTableConfig} from "../configs/tableConfigs/groupsTableConfig";
import {aclsTableConfig} from "../configs/tableConfigs/aclsTableConfig";
import {themesTableConfig} from "../configs/tableConfigs/themesTableConfig";
import {
    deselectAll,
    loadResourceIntoTable,
    selectAll,
    selectRow,
    setOffset,
    setPageActive,
    setPages,
    loadColumns
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
import {fetchEvents} from "./eventThunks";
import {fetchSeries} from "./seriesThunks";
import {fetchRecordings} from "./recordingThunks";
import {fetchJobs} from "./jobThunks";
import {fetchServers} from "./serverThunks";
import {fetchServices} from "./serviceThunks";
import {fetchUsers} from "./userThunks";
import {fetchGroups} from "./groupThunks";
import {fetchAcls} from "./aclThunks";
import {fetchThemes} from "./themeThunks";

/**
 * This file contains methods/thunks used to manage the table in the main view and its state changes
 * */

// Method to load events into the table
export const loadEventsIntoTable = () => async (dispatch, getState) => {
    const { events, table } = getState();
    const total = events.total;

    console.log('in Load events into Table');
    const pagination = table.pagination;
    const resource = events.results.map((result) => {
        return {
            ...result,
            selected: false
        }
    });

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "events",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'events') {
        const c = eventsTableConfig.columns;
        const columns = c.map(column => {
            const col = events.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        })
        const multiSelect = eventsTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "title",
            multiSelect: multiSelect
        }
    }
    dispatch(loadResourceIntoTable(tableData));
    console.log('table data was dispatched');
}

// Method to load series into the table
export const loadSeriesIntoTable = () => (dispatch, getState) => {
    const { series, table } = getState();
    const total = series.total;
    const pagination = table.pagination;

    const resource = series.results.map((result) => {
        return {
            ...result,
            selected: false
        }
    });


    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "series",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'series') {
        const c = seriesTableConfig.columns;
        const columns = c.map(column => {
            const col = series.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });
        const multiSelect = seriesTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "title",
            multiSelect: multiSelect
        }
    }
    dispatch(loadResourceIntoTable(tableData));
};

export const loadRecordingsIntoTable = () => (dispatch, getState) => {
    const { recordings, table } = getState();
    const pagination = table.pagination;
    const resource = recordings.results;
    const total = recordings.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "recordings",
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        rows: resource,
        totalItems: total
    }

    if (table.resource !== "recordings") {
        const c = recordingsTableConfig.columns;
        const columns = c.map(column => {
            const col = recordings.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated : col.deactivated
            }
        });

        const multiSelect = recordingsTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "status",
            multiSelect: multiSelect
        }
    }

    dispatch(loadResourceIntoTable(tableData));
}

export const loadJobsIntoTable = () => (dispatch, getState) => {
    const { jobs, table } = getState();
    const pagination = table.pagination;
    const resource = jobs.results;
    const total = jobs.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "jobs",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'jobs') {
        const c = jobsTableConfig.columns;
        const columns = c.map(column => {
            const col = jobs.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = jobsTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "id",
            multiSelect: multiSelect
        };
    }
    dispatch(loadResourceIntoTable(tableData));
}

export const loadServersIntoTable = () => (dispatch, getState) => {
    const { servers, table } = getState();
    const pagination = table.pagination;
    const resource = servers.results;
    const total = servers.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "servers",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'servers') {
        const c = serversTableConfig.columns;
        const columns = c.map(column => {
            const col = servers.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = serversTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "online",
            multiSelect: multiSelect
        };
    }
    dispatch(loadResourceIntoTable(tableData));

}

export const loadServicesIntoTable = () => (dispatch, getState) => {
    const { services, table } = getState();
    const pagination = table.pagination;
    const resource = services.results;
    const total = services.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        rows: resource,
        pages: pages,
        totalItems: total,
        resource: "services",
        columns: table.columns,
        multiSelect: table.multiSelect,
        sortBy: table.sortBy
    }

    if (table.resource !== 'services') {
        const c = servicesTableConfig.columns;
        const columns = c.map(column => {
            const col = services.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = servicesTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "status",
            multiSelect: multiSelect
        };
    }

    dispatch(loadResourceIntoTable(tableData));
}

export const loadUsersIntoTable = () => (dispatch, getState) => {
    const { users, table} = getState();
    const pagination = getTablePagination(getState());
    const resource = users.results;
    const total = users.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "users",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'users') {
        const c = usersTableConfig.columns;
        const columns = c.map(column => {
            const col = users.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = usersTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "name",
            multiSelect: multiSelect
        };
    }
    dispatch(loadResourceIntoTable(tableData));
};

export const loadGroupsIntoTable = () => (dispatch, getState) => {
    const { groups, table } = getState();
    const pagination = table.pagination;
    const resource = groups.results;
    const total = groups.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "groups",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'groups') {
        const c = groupsTableConfig.columns;
        const columns = c.map(column => {
            const col = groups.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = groupsTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "name",
            multiSelect: multiSelect
        };
    }
    dispatch(loadResourceIntoTable(tableData));
}

export const loadAclsIntoTable = () => (dispatch, getState) => {
    const { acls, table } = getState();
    const pagination = table.pagination;
    const resource = acls.results;
    const total = acls.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "acls",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'acls') {
        const c = aclsTableConfig.columns;
        const columns = c.map(column => {
            const col = acls.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = aclsTableConfig.multiSelect;
        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "name",
            multiSelect: multiSelect
        };
    }
    dispatch(loadResourceIntoTable(tableData));
}

export const loadThemesIntoTable = () => (dispatch, getState) => {
    const { themes, table } = getState();
    const pagination = table.pagination;
    const resource = themes.results;
    const total = themes.total;

    const pages = calculatePages(total / pagination.limit, pagination.offset);

    let tableData = {
        resource: "themes",
        rows: resource,
        columns: table.columns,
        multiSelect: table.multiSelect,
        pages: pages,
        sortBy: table.sortBy,
        totalItems: total
    };

    if (table.resource !== 'themes') {
        const c = themesTableConfig.columns;
        const columns = c.map(column => {
            const col = themes.columns.find(co => co.name === column.name);
            return {
                ...column,
                deactivated: col.deactivated
            }
        });

        const multiSelect = themesTableConfig.multiSelect;

        tableData = {
            ...tableData,
            columns: columns,
            sortBy: "name",
            multiSelect: multiSelect
        };
    }
    dispatch(loadResourceIntoTable(tableData));
}


// Navigate between pages
export const goToPage = pageNumber => (dispatch, getState) => {

    dispatch(deselectAll());
    dispatch(setOffset(pageNumber));

    const state = getState();
    const offset = getPageOffset(state);
    const pages = getTablePages(state);


    dispatch(setPageActive(pages[offset].number));

    // Get resources of page and load them into table
    // eslint-disable-next-line default-case
    switch (getResourceType(state)) {
        case 'events': {
            dispatch(fetchEvents());
            dispatch(loadEventsIntoTable());
            break;
        }
        case 'series': {
            dispatch(fetchSeries());
            dispatch(loadSeriesIntoTable());
            break;
        }
        case 'recordings': {
            dispatch(fetchRecordings());
            dispatch(loadRecordingsIntoTable());
            break;
        }
        case 'jobs': {
            dispatch(fetchJobs());
            dispatch(loadJobsIntoTable());
            break;
        }
        case 'servers': {
            dispatch(fetchServers());
            dispatch(loadServersIntoTable());
            break;
        }
        case 'services': {
            dispatch(fetchServices());
            dispatch(loadServicesIntoTable());
            break;
        }
        case 'users': {
            dispatch(fetchUsers());
            dispatch(loadUsersIntoTable());
            break;
        }
        case 'groups': {
            dispatch(fetchGroups());
            dispatch(loadGroupsIntoTable());
            break;
        }
        case 'acls': {
            dispatch(fetchAcls());
            dispatch(loadAclsIntoTable());
            break;
        }
        case 'themes': {
            dispatch(fetchThemes());
            dispatch(loadThemesIntoTable());
            break;
        }
    }
}

// Update pages for example if page size was changed
export const updatePages = () => (dispatch,getState) => {
    const state = getState();

    const pagination = getTablePagination(state);

    const pages = calculatePages(pagination.totalItems / pagination.limit, pagination.offset);

    dispatch(setPages(pages));

    // Get resources of page and load them into table
    // eslint-disable-next-line default-case
    switch (getResourceType(state)) {
        case 'events': {
            dispatch(fetchEvents());
            dispatch(loadEventsIntoTable());
            break;
        }
        case 'series': {
            dispatch(fetchSeries());
            dispatch(loadSeriesIntoTable());
            break;
        }
        case 'recordings': {
            dispatch(fetchRecordings());
            dispatch(loadRecordingsIntoTable());
            break;
        }
        case 'jobs': {
            dispatch(fetchJobs());
            dispatch(loadJobsIntoTable());
            break;
        }
        case 'servers': {
            dispatch(fetchServers());
            dispatch(loadServersIntoTable());
            break;
        }
        case 'services': {
            dispatch(fetchServices());
            dispatch(loadServicesIntoTable());
            break;
        }
        case 'users': {
            dispatch(fetchUsers());
            dispatch(loadUsersIntoTable());
            break;
        }
        case 'groups': {
            dispatch(fetchGroups());
            dispatch(loadGroupsIntoTable());
            break;
        }
        case 'acls': {
            dispatch(fetchAcls());
            dispatch(loadAclsIntoTable());
            break;
        }
        case 'themes': {
            dispatch(fetchThemes());
            dispatch(loadThemesIntoTable());
            break;
        }
    }

}

// Select all rows on table page
export const changeAllSelected = selected => (dispatch, getState) => {
    const state = getState();

    if (selected) {
        // eslint-disable-next-line default-case
        switch (getResourceType(state)) {
            case 'events': {
                dispatch(showEventsActions(true));
                break;
            }
            case 'series': {
               dispatch(showSeriesActions(true));
               break;
            }
        }
        dispatch(selectAll());
    } else {
        // eslint-disable-next-line default-case
        switch (getResourceType(state)) {
            case 'events': {
                dispatch(showEventsActions(false));
                break;
            }
            case 'series': {
                dispatch(showSeriesActions(false));
                break;
            }
        }
        dispatch(deselectAll());
    }
}

// Select certain columns
export const changeColumnSelection = (id, selected) => (dispatch, getState) => {

    dispatch(loadColumns(id, selected));

    const state = getState();

    // eslint-disable-next-line default-case
    switch (getResourceType(state)) {
        case 'events': {
            if (getSelectedRows(state).length > 0) {
                dispatch(showEventsActions(true));
            } else {
                dispatch(showEventsActions(false));
            }
            break;
        }
        case 'series': {
            if (getSelectedRows(state).length > 0) {
                dispatch(showSeriesActions(true));
            } else {
                dispatch(showSeriesActions(false));
            }
            break;
        }
    }
}

// Select certain row
export const changeRowSelection = (id, selected) => (dispatch, getState) => {

    dispatch(selectRow(id, selected));

    const state = getState();

    // eslint-disable-next-line default-case
    switch (getResourceType(state)) {
        case 'events': {
            if (getSelectedRows(state).length > 0) {
                dispatch(showEventsActions(true));
            } else {
                dispatch(showEventsActions(false));
            }
            break;
        }
        case 'series': {
            if (getSelectedRows(state).length > 0) {
                dispatch(showSeriesActions(true));
            } else {
                dispatch(showSeriesActions(false));
            }
            break;
        }
    }
}

const calculatePages = (numberOfPages, offset) => {
    const pages = [];

    for (let i = 0; i < numberOfPages || (i === 0 && numberOfPages === 0); i++) {
        pages.push({
            number: i,
            label: (i + 1).toString(),
            active: i === offset
        });
    }

    return pages;
}
