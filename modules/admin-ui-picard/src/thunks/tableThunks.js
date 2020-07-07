import * as t from "../actions/tableActions";
import * as es from "../selectors/eventSelectors";
import {eventsTableConfig}  from "../configs/tableConfigs/eventsTableConfig";
import {loadResourceIntoTable} from "../actions/tableActions";


export const loadEventsIntoTable = () => async (dispatch, getState) => {
    const { events } = getState();
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
    const tableData = {
        resource: resource,
        columns: columns,
        multiSelect: multiSelect
    };
    dispatch(loadResourceIntoTable(tableData));




}
