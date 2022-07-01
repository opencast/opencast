import {getFilters, getTextFilter} from "../selectors/tableFilterSelectors";
import {getPageLimit, getPageOffset, getTableDirection, getTableSorting} from "../selectors/tableSelectors";
import {hasAccess, hasOrgAdminAccess} from "./utils";

/**
 * This file contains methods that are needed in more than one resource thunk
 */

// prepare http headers for posting to resources
export const getHttpHeaders = () => {
    return {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    };
}

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

// get initial metadata field values for formik in create resources wizards
export const getInitialMetadataFieldValues = (metadataFields, extendedMetadata) => {
    let initialValues = {};

    if (!!metadataFields.fields && metadataFields.fields.length > 0) {
        metadataFields.fields.forEach(field => {
            initialValues[field.id] = field.value;
        });
    }

    if(extendedMetadata.length > 0){
        for(const metadataCatalog of extendedMetadata){
            if (!!metadataCatalog.fields && metadataCatalog.fields.length > 0) {
                metadataCatalog.fields.forEach(field => {
                    let value = field.value
                    if (value === 'true') {
                        value = true;
                    } else if (value === 'false') {
                        value = false;
                    }

                    initialValues[metadataCatalog.flavor + '_' + field.id] = value;
                });
            }
        }
    }

    return initialValues;
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

// transform metadata catalog for update via post request
export const transformMetadataForUpdate = (catalog, values) => {
    let fields = [];
    let updatedFields = [];

    catalog.fields.forEach(field => {
        if (field.value !== values[field.id]) {
            let updatedField = {
                ...field,
                value: values[field.id]
            }
            updatedFields.push(updatedField);
            fields.push(updatedField);
        } else {
            fields.push({...field});
        }
    });
    let data = new URLSearchParams();
    data.append("metadata", JSON.stringify([{
        flavor: catalog.flavor,
        title: catalog.title,
        fields: updatedFields
    }]));
    const headers = getHttpHeaders();

    return {fields, data, headers};
}

// Prepare metadata for post of new events or series
export const prepareMetadataFieldsForPost = (metadataInfo, values, formikIdPrefix = '') => {
    let metadataFields = [];

    // fill metadataField with field information send by server previously and values provided by user
    // Todo: What is hashkey?
    for (let i = 0; metadataInfo.length > i; i++) {
        let fieldValue = {
            id: metadataInfo[i].id,
            type: metadataInfo[i].type,
            value: values[formikIdPrefix + metadataInfo[i].id],
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

// Prepare extended metadata for post of new events or series
export const prepareExtendedMetadataFieldsForPost = (extendedMetadata, values) => {
    const extendedMetadataFields = [];

    for(const catalog of extendedMetadata){
        const catalogPrefix = catalog.flavor + '_';
        const metadataFields = prepareMetadataFieldsForPost(catalog.fields, values, catalogPrefix);

        // Todo: What is hashkey?
        const metadataCatalog = {
            flavor: catalog.flavor,
            title: catalog.title,
            fields: metadataFields,
            $$hashKey: "object:123"
        };

        extendedMetadataFields.push(metadataCatalog);
    }

    return extendedMetadataFields;
}

export const prepareSeriesMetadataFieldsForPost = (metadataInfo, values, formikIdPrefix = '') => {
    let metadataFields = [];

    // fill metadataField with field information sent by server previously and values provided by user
    for (let i = 0; metadataInfo.length > i; i++) {
        let fieldValue = {
            readOnly: metadataInfo[i].readOnly,
            id: metadataInfo[i].id,
            label: metadataInfo[i].label,
            type: metadataInfo[i].type,
            value: values[formikIdPrefix + metadataInfo[i].id],
            tabindex: i + 1
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
        if (!!metadataInfo[i].required) {
            fieldValue = {
                ...fieldValue,
                required: metadataInfo[i].required,
            };
        }
        if (metadataInfo[i].type === 'mixed_text') {
            fieldValue = {
                ...fieldValue,
                presentableValue: values[formikIdPrefix + metadataInfo[i].id].join()
            };
        } else {
            fieldValue = {
                ...fieldValue,
                presentableValue: values[formikIdPrefix + metadataInfo[i].id]
            };
        }
        metadataFields = metadataFields.concat(fieldValue);
    }

    return metadataFields;
}

// Prepare extended metadata for post of new events or series
export const prepareSeriesExtendedMetadataFieldsForPost = (extendedMetadata, values) => {
    const extendedMetadataFields = [];

    for(const catalog of extendedMetadata){
        const catalogPrefix = catalog.flavor + '_';
        const metadataFields = prepareSeriesMetadataFieldsForPost(catalog.fields, values, catalogPrefix);

        // Todo: What is hashkey?
        const metadataCatalog = {
            flavor: catalog.flavor,
            title: catalog.title,
            fields: metadataFields
        };

        extendedMetadataFields.push(metadataCatalog);
    }

    return extendedMetadataFields;
}

// returns the name for a field value from the collection
export const getMetadataCollectionFieldName = (metadataField, field) => {
    try {
        const collectionField = metadataField.collection.find(element => element.value === field.value);
        return collectionField.name;
    } catch (e) {
        return '';
    }
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

// transform response data in form that is used in wizards and modals for policies (for each role one entry)
export const transformAclTemplatesResponse = acl => {
    let template = [];

    for (let i = 0; acl.ace.length > i; i++) {
        if (template.find(rule => rule.role === acl.ace[i].role)) {
            for (let j = 0; template.length > j; j++) {
                // Only update entry for policy if already added with other action
                if(template[j].role === acl.ace[i].role) {
                    if (acl.ace[i].action === "read") {
                        template[j] = {
                            ...template[j],
                            read: acl.ace[i].allow
                        }
                        break;
                    }
                    if (acl.ace[i].action === "write") {
                        template[j] = {
                            ...template[j],
                            write: acl.ace[i].allow
                        }
                        break;
                    }
                    if (acl.ace[i].action !== "read" && acl.ace[i].action !== "write"
                        && acl.ace[i].allow === true) {
                        template[j] = {
                            ...template[j],
                            actions: template[j].actions.concat(acl.ace[i].action)
                        }
                        break;
                    }
                }
            }
        } else {
            // add policy if role not seen before
            if (acl.ace[i].action === "read") {
                template = template.concat({
                    role: acl.ace[i].role,
                    read: acl.ace[i].allow,
                    write: false,
                    actions: []
                });
            }
            if (acl.ace[i].action === "write") {
                template = template.concat({
                    role: acl.ace[i].role,
                    read: false,
                    write: acl.ace[i].allow,
                    actions: []
                });
            }
            if (acl.ace[i].action !== "read" && acl.ace[i].action !== "write"
                && acl.ace[i].allow === true) {
                template = template.concat({
                    role: acl.ace[i].role,
                    read: false,
                    write: false,
                    actions: [acl.ace[i].action]
                })
            }
        }
    }

    return template;
}

// filter devices, so that only devices for which the user has access rights are left
export const filterDevicesForAccess = (user, inputDevices) => {
    if(user.isOrgAdmin){
            return inputDevices;
    } else {
        const devicesWithAccessRights = [];
        for(const device of inputDevices){
            const inputDeviceAccessRole = 'ROLE_CAPTURE_AGENT_' + device.id.replace(/[^a-zA-Z0-9_]/g, '').toUpperCase();
            if(hasAccess(inputDeviceAccessRole, user)){
                devicesWithAccessRights.push(device);
            }
        }

        return devicesWithAccessRights;
    }
}

// returns, whether user has access rights for any inputDevices
export const hasAnyDeviceAccess = (user, inputDevices) => {
    return filterDevicesForAccess(user, inputDevices).length > 0;
}

// returns, whether user has access rights for a specific inputDevice
export const hasDeviceAccess = (user, deviceId) => {
    if(user.isOrgAdmin){
            return true;
    } else {
        const inputDeviceAccessRole = 'ROLE_CAPTURE_AGENT_' + deviceId.replace(/[^a-zA-Z0-9_]/g, '').toUpperCase();
        return hasAccess(inputDeviceAccessRole, user);
    }
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
