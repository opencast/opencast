import moment from 'moment';

/**
 * This File contains methods concerning dates
 */

// transform relative date to an absolute date
export const relativeToAbsoluteDate = (relative, type, from) => {
    let localMoment = moment();

    let absolute;
    if (from) {
        absolute = localMoment.startOf(type);
    } else {
        absolute = localMoment.endOf(type);
    }

    absolute = absolute.add(relative, type);

    return absolute.toDate();
};

// transform from relative date span to filter value containing absolute dates
export const relativeDateSpanToFilterValue = (fromRelativeDate, toRelativeDate, type) => {
    let fromAbsoluteDate = relativeToAbsoluteDate(fromRelativeDate, type, true);
    let toAbsoluteDate = relativeToAbsoluteDate(toRelativeDate, type, false);

    return (fromAbsoluteDate.toISOString() + '/' + toAbsoluteDate.toISOString()).toString();
};

// creates a date object from a date, hour and minute
export const makeDate = (date, hour, minute) => {
    const madeDate = new Date(date);
    madeDate.setHours(hour);
    madeDate.setMinutes(minute);

    return madeDate;
}

// calculates the duration between a start and end date in hours and minutes
export const calculateDuration = (startDate, endDate) => {
    const duration = (endDate - startDate) / 1000;
    const durationHours = (duration - (duration % 3600)) / 3600;
    const durationMinutes = (duration % 3600) / 60;

    return {durationHours, durationMinutes};
}

// get localized time
export const localizedMoment = (m,  currentLanguage) => {
    return moment(m).locale(currentLanguage.dateLocale.code);
};
