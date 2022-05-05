import moment from "moment";
import {getCurrentLanguageInformation} from "./utils";

/**
 * This file contains functions that are needed for thunks for statistics
 */

/* creates callback function for formatting the labels of the xAxis in a statistics diagram */
const createXAxisTickCallback = (timeMode, dataResolution, language) => {

    return (value, index, ticks) => {
        let formatString = 'L';
        if (timeMode === 'year') {
            formatString = 'MMMM';
        } else if (timeMode === 'month') {
            formatString = 'dddd, Do';
        } else {
            if (dataResolution === 'yearly') {
                formatString = 'YYYY';
            } else if (dataResolution === 'monthly') {
                formatString = 'MMMM';
            } else if (dataResolution === 'daily') {
                formatString = 'MMMM Do, YYYY';
            } else if (dataResolution === 'hourly') {
                formatString = 'LLL';
            }
        }

        return moment(value).locale(language).format(formatString);
    }
}

/* creates callback function for the displayed label when hovering over a data point in a statistics diagram */
const createTooltipCallback = (chooseMode, dataResolution, language) => {
    return (tooltipItem) => {
        const date = tooltipItem.label;

        let formatString;
        if (chooseMode === 'year') {
            formatString = 'MMMM YYYY';
        } else if (chooseMode === 'month') {
            formatString = 'dddd, MMMM Do, YYYY';
        } else {
            if (dataResolution === 'yearly') {
                formatString = 'YYYY';
            } else if (dataResolution === 'monthly') {
                formatString = 'MMMM YYYY';
            } else if (dataResolution === 'daily') {
                formatString = 'dddd, MMMM Do, YYYY';
            } else {
                formatString = 'dddd, MMMM Do, YYYY HH:mm';
            }
        }
        const finalDate = moment(date).locale(language).format(formatString);
        return finalDate + ': ' + tooltipItem.value;
    }
}

/* creates options for statistics chart */
export const createChartOptions = (timeMode, dataResolution) => {

    // Get info about the current language and its date locale
    const currentLanguage = getCurrentLanguageInformation().dateLocale.code;

    return {
        responsive: true,
        legend: {
            display: false
        },
        layout: {
            padding: {
                top: 20,
                left: 20,
                right: 20
            }
        },
        scales: {
            xAxes: [{
                ticks: {
                    callback: createXAxisTickCallback(timeMode, dataResolution, currentLanguage)
                }
            }],
            y: {
                suggestedMin: 0
            }
        },
        tooltips: {
            callbacks: {
                label: createTooltipCallback(timeMode, dataResolution, currentLanguage)
            }
        }};
}

/* creates the url for downloading a csv file with current statistics */
export const createDownloadUrl = (eventId, providerId, from, to, dataResolution) => {
    const csvUrlSearchParams = new URLSearchParams({
        dataResolution: dataResolution,
        providerId: providerId,
        resourceId: eventId,
        resourceType: 'episode',
        from: moment(from).toJSON(),
        to: moment(to).endOf('day').toJSON()
    });

    return '/admin-ng/statistics/export.csv?' + csvUrlSearchParams;
}