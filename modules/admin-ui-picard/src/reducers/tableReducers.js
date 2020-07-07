import * as t from "../actions/tableActions";

/*
Overview of the structure of the data in arrays in state
const pages = [{
  active: false,
  label: "",
  number: 1
}, ...]

const rows = [{
    id: 1,
    data: [{for each column a value}]
}, ...]

const columns = [{
    style: "",
    deactivated: true,
    name: "",
    sortable: false,
    label: "",
    translate: false,
    template: ""
}, ...]
 */

const initialState = {
    loading: false,
    multiSelect: false,
    pages: [],
    columns: [],
    sortBy: "",
    predicate: "",
    reverse: false,
    rows: [],
    maxLabel: "",
    pagination: {
        limit: 10,
        offset: 0
    }
};



const table = (state=initialState, action) => {
    const { type, payload } = action;
    switch (type) {
        case t.LOAD_RESOURCE_INTO_TABLE: {
            const { multiSelect, columns, resource } = payload;
            return {
                ...state,
                multiSelect: multiSelect,
                columns: columns,
                rows: resource,
            }
        }
        case t.LOAD_COLUMNS: {
            return state;
        }
        case t.CREATE_PAGE: {
            const { page } = payload;
            return {
                ...state,
                pages: state.pages.concat(page)
            }
        }
        case t.SELECT_ROW: {
            const { id } = payload;
            return {
                ...state,
                rows: state.rows.map(row => {
                    if (row.id === id) {
                        return {
                            ...row,
                            selected: !row.selected
                        }
                    }
                    return row;
                })
            }
        }
        case t.SELECT_ALL: {
           return {
               ...state,
               rows: state.rows.map(row => {
                   return {
                       ...row,
                       selected: true
                   }
               })
           }
        }
        case t.DESELECT_ALL: {
            return {
                ...state,
                rows: state.rows.map(row => {
                    return {
                        ...row,
                        selected: false
                    }
                })
            }
        }
        case t.UPDATE_PAGESIZE: {
            const { limit } = payload;
            return {
                ...state,
                pagination: {
                    offset: state.pagination.offset,
                    limit: limit
                }
            }
        }
        case t.SORT_TABLE: {
            //todo: maybe some adjustments necessary, when actually implementing this
            return state;
        }
        case t.RESET_SORT_TABLE: {
            //todo: maybe some adjustments necessary, when actually implementing this
            return state;
        }
        case t.REVERSE_TABLE: {
            //todo: maybe some adjustments necessary, when actually implementing this
            return state;
        }
        case t.SET_MULTISELECT: {
            //todo: maybe some adjustments necessary, when actually implementing this
            return state;
        }
        default:
            return state;
    }
};

export default table;
