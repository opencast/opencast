import moment from "moment";

/**
 * This File contains methods concerning dates
 */

// transform relative date to an absolute date
export const relativeToAbsoluteDate = (relative, type, from) => {
    let localMoment = moment();

    let absolute;
    if (from === true) {
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

    return fromAbsoluteDate.toISOString() + '/' + toAbsoluteDate.toISOString();
};
