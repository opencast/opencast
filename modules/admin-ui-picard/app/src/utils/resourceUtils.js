import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

/**
 * This file contains methods that are needed in more than one resource thunk
 */


export const getURLParams = state => {
    // get filter map from state
    let filters = [];
    let filterMap = getFilters(state);
    // transform filters for use as URL param
    for (let key in filterMap) {
        if (!!filterMap[key].value) {
            filters.push(filterMap[key].name + ':' + filterMap[key].value);
        }
    }

    if (filters.length) {
        return {
            filters: filters.join(','),
            sort: getTableSorting(state) + ':' + getTableDirection(state),
            limit: getPageLimit(state),
            offset: getPageOffset(state)
        };
    } else {
        return {
            sort: getTableSorting(state) + ':' + getTableDirection(state),
            limit: getPageLimit(state),
            offset: getPageOffset(state)
        };
    }
}
