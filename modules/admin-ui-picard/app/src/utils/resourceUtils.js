import {getFilters} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

/**
 * This file contains methods that are needed in more than one resource thunk
 */


// prepare URL params for getting resources
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

// transform collection of metadata into object with name and value
export const transformMetadataCollection = metadata => {

    for (let i = 0; metadata.fields.length > i; i++) {
        if (!!metadata.fields[i].collection) {
            metadata.fields[i].collection = Object.keys(metadata.fields[i].collection).map(key => {
                return {
                    name: key,
                    value: metadata.fields[i].collection[key]
                }
            })
        }
    }

    return metadata;
}

// Prepare metadata for post of new events or series
export const prepareMetadataFieldsForPost = (metadataInfo, values) => {
    let metadataFields = [];

    // fill metadataField with field information send by server previously and values provided by user
    // Todo: What is hashkey?
    for (let i = 0; metadataInfo.length > i; i++) {
        let fieldValue = {
            id: metadataInfo[i].id,
            type: metadataInfo[i].type,
            value: values[metadataInfo[i].id],
            tabindex: i + 1,
            $$hashKey: "object:123"
        };
        if (!!metadataInfo[i].translatable) {
            fieldValue = {
                ...fieldValue,
                translatable: metadataInfo[i].translatable
            }
        }
        metadataFields = metadataFields.concat(fieldValue);
    }

    return metadataFields;
}

// Prepare rules of access policies for post of new events or series
export const prepareAccessPolicyRulesForPost = policies => {

    // access policies for post request
    let access = {
        acl: {
            ace: []
        }
    };

    // iterate through all policies provided by user and transform them into form required for request
    for (let i = 0; policies.length > i; i++) {
        access.acl.ace = access.acl.ace.concat({
            action: 'read',
            allow: policies[i].read,
            role: policies[i].role
        },{
            action: 'write',
            allow: policies[i].write,
            role: policies[i].role
        });
        if (policies[i].actions.length > 0) {
            for (let j = 0; policies[i].actions.length > j; j++) {
                access.acl.ace = access.acl.ace.concat({
                    action: policies[i].actions[j],
                    allow: true,
                    role: policies[i].role
                })
            }
        }
    }

    return access;
}
