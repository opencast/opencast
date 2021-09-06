import {getFilters, getTextFilter} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";

/**
 * This file contains methods that are needed in more than one resource thunk
 */


// prepare URL params for getting resources
export const getURLParams = state => {
    // get filter map from state
    let filters = [];
    let filterMap = getFilters(state);
    let textFilter = getTextFilter(state);

    // check if textFilter has value and transform for use as URL param
    if (textFilter !== '') {
        filters.push('textFilter:' + textFilter);
    }
    // transform filters for use as URL param
    for (let key in filterMap) {
        if (!!filterMap[key].value) {
            filters.push(filterMap[key].name + ':' + filterMap[key].value.toString());
        }
    }

    let params = {
        limit: getPageLimit(state),
        offset: getPageOffset(state)
    }

    if (filters.length) {
        params = {
            ...params,
            filter: filters.join(','),
        };
    }

    if (getTableSorting(state) !== '') {
        params = {
            ...params,
            sort: getTableSorting(state) + ':' + getTableDirection(state),
        };
    }

    return params;
}

// used for create URLSearchParams for API requests used to create/update user
export const buildUserBody = values => {
    let data = new URLSearchParams();
    // fill form data with user inputs
    data.append('username', values.username);
    data.append('name', values.name);
    data.append('email', values.email);
    data.append('password', values.password);
    data.append('roles', JSON.stringify(values.roles));

    return data;
}

// used for create URLSearchParams for API requests used to create/update group
export const buildGroupBody = values => {
    let roles = [], users = [];

    // fill form data depending on user inputs
    let data = new URLSearchParams();
    data.append('name', values.name);
    data.append('description', values.description);

    for(let i = 0 ; i < values.roles.length; i++) {
        roles.push(values.roles[i].name);
    }
    for(let i = 0 ; i < values.users.length; i++) {
        users.push(values.users[i].id);
    }
    data.append('roles', roles.join(','));
    data.append('users', users.join(','));

    return data;
}

// transform collection of metadata into object with name and value
export const transformMetadataCollection = (metadata, noField) => {
    if (noField) {
        for (let i = 0; metadata.length > i; i++) {
            if (!!metadata[i].collection) {
                metadata[i].collection = Object.keys(metadata[i].collection).map(key => {
                    return {
                        name: key,
                        value: metadata[i].collection[key]
                    }
                })
            }
            metadata[i] = {
                ...metadata[i],
                selected: false
            }
        }
    } else {
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

export const prepareSeriesMetadataFieldsForPost = (metadataInfo, values) => {
    let metadataFields = [];

    // fill metadataField with field information sent by server previously and values provided by user
    // Todo: What is hashkey?
    for (let i = 0; metadataInfo.length > i; i++) {
        let fieldValue = {
            readOnly: metadataInfo[i].readOnly,
            id: metadataInfo[i].id,
            label: metadataInfo[i].label,
            type: metadataInfo[i].type,
            value: values[metadataInfo[i].id],
            tabindex: i + 1,
            $$hashKey: "object:123"
        };
        if (!!metadataInfo[i].translatable) {
            fieldValue = {
                ...fieldValue,
                translatable: metadataInfo[i].translatable
            };
        }
        if (!!metadataInfo[i].collection) {
            fieldValue = {
                ...fieldValue,
                collection: [],
            };
        }
        if (metadataInfo[i].type === 'mixed_text') {
            fieldValue = {
                ...fieldValue,
                presentableValue: values[metadataInfo[i].id].join()
            };
        } else {
            fieldValue = {
                ...fieldValue,
                presentableValue: values[metadataInfo[i].id]
            };
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

// build body for post/put request in theme context
export const buildThemeBody = values => {
    // fill form data depending on user inputs
    let data = new URLSearchParams();
    data.append('name', values.name);
    data.append('description', values.description);
    data.append('bumperActive', values.bumperActive);
    if (values.bumperActive) {
        data.append('bumperFile', values.bumperFile);
    }
    data.append('trailerActive', values.trailerActive);
    if (values.trailerActive) {
        data.append('trailerFile', values.trailerFile);
    }
    data.append('titleSlideActive', values.titleSlideActive);
    if (values.titleSlideActive && values.titleSlideMode === 'upload') {
        data.append('titleSlideBackground', values.titleSlideBackground);
    }
    data.append('licenseSlideActive', values.licenseSlideActive);
    data.append('watermarkActive', values.watermarkActive);
    if (values.watermarkActive) {
        data.append('watermarkFile', values.watermarkFile);
        data.append('watermarkPosition', values.watermarkPosition);
    }

    return data;
}

// creates an empty policy with the role from the argument
export const createPolicy = (role) => {
    return {
        role: role,
        read: false,
        write: false,
        actions: []
    };
};
