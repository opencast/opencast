/*
 * this file contains functions, which are needed for the searchable drop-down selections
 */

import {makeTwoDigits} from "./utils";

const filterBySearch = (filterText, type, options, t) => {
    if (type === 'language') {
        return options.filter(item => t(item.name).toLowerCase().includes(filterText));
    } else if (type === 'isPartOf' || type === 'captureAgent') {
        return options.filter(item => item.name.toLowerCase().includes(filterText));
    } else {
        return options.filter(item => item.value.toLowerCase().includes(filterText));
    }
}

export const handleSearch = async (searchText, type, options, setSearch, t) => {
    setSearch({
        text: searchText,
        filteredCollection: filterBySearch(searchText.toLowerCase(), type, options, t)
    });
}

/*
 * the Select component needs options to have an internal value and a displayed label
 * this function formats selection options as provided by the backend into that scheme
 * it takes the options and provides the correct label to display for this kind of metadata,
 * as well as adding an empty option, if available
 */
export const formatDropDownOptions = (unformattedOptions, type, currentValue, required, t) => {
    const formattedOptions = [];
    if (currentValue === '' || !required) {
        formattedOptions.push({
            value: '',
            label: `-- ${t('SELECT_NO_OPTION_SELECTED')} --`
        });
    }
    if (type === 'language') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.value,
                label: t(item.name)
            });
        }
    } else if (type === 'isPartOf') {
            for (const item of unformattedOptions) {
                formattedOptions.push({
                    value: item.value,
                    label: item.name
                });
            }
    } else if (type === 'captureAgent') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.name,
                label: item.name
            });
        }
    } else if (type === 'time') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: makeTwoDigits(item.index),
                label: item.value
            });
        }
    } else {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.value,
                label: item.value
            });
        }
    }

    return formattedOptions;
}