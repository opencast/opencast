/*
 * this file contains functions, which are needed for the searchable drop-down selections
 */

export const filterBySearch = (filterText, type, options, t) => {
    if (type === 'language') {
        return options.filter(item => t(item.name).toLowerCase().includes(filterText));
    } else if (type === 'isPartOf' || type === 'captureAgent' || type === 'aclRole'|| type === 'newTheme') {
        return options.filter(item => item.name.toLowerCase().includes(filterText));
    } else if (type === 'workflow') {
        return options.filter(item => item.title.toLowerCase().includes(filterText));
    } else if (type === 'comment') {
        return options.filter(item => t(item[0]).toLowerCase().includes(filterText));
    } else {
        return options.filter(item => item.value.toLowerCase().includes(filterText));
    }
}

/*
 * the Select component needs options to have an internal value and a displayed label
 * this function formats selection options as provided by the backend into that scheme
 * it takes the options and provides the correct label to display for this kind of metadata,
 * as well as adding an empty option, if available
 */
export const formatDropDownOptions = (unformattedOptions, type, currentValue, required, t) => {
    const formattedOptions = [];
    if (!required) {
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
    } else if (type === 'captureAgent' || type === 'aclRole') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.name,
                label: item.name
            });
        }
    } else if (type === 'workflow') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.id,
                label: item.title
            });
        }
    } else if (type === 'aclTemplate') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.id,
                label: item.value
            });
        }
    } else if (type === 'newTheme') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item.id,
                label: item.name
            });
        }
    } else if (type === 'comment') {
        for (const item of unformattedOptions) {
            formattedOptions.push({
                value: item[0],
                label: t(item[1])
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