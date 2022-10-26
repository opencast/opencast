import moment from "moment";
import { makeTwoDigits } from "./utils";

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
export const relativeDateSpanToFilterValue = (
	fromRelativeDate,
	toRelativeDate,
	type
) => {
	let fromAbsoluteDate = relativeToAbsoluteDate(fromRelativeDate, type, true);
	let toAbsoluteDate = relativeToAbsoluteDate(toRelativeDate, type, false);

	return (
		fromAbsoluteDate.toISOString() +
		"/" +
		toAbsoluteDate.toISOString()
	).toString();
};

// creates a date object from a date, hour and minute
export const makeDate = (date, hour, minute) => {
	const madeDate = new Date(date);
	madeDate.setHours(hour);
	madeDate.setMinutes(minute);

	return madeDate;
};

// calculates the duration between a start and end date in hours and minutes
export const calculateDuration = (startDate, endDate) => {
	const duration = (endDate - startDate) / 1000;
	const durationHours = (duration - (duration % 3600)) / 3600;
	const durationMinutes = (duration % 3600) / 60;

	return { durationHours, durationMinutes };
};

// sets the duration in the formik
const setDuration = (startDate, endDate, setFieldValue) => {
	const { durationHours, durationMinutes } = calculateDuration(
		startDate,
		endDate
	);

	setFieldValue("scheduleDurationHours", makeTwoDigits(durationHours));
	setFieldValue("scheduleDurationMinutes", makeTwoDigits(durationMinutes));
};

// checks if the time of the endDate is before the time of the startDate
const isEndBeforeStart = (startDate, endDate) => {
	return startDate > endDate;
};

// changes the start in the formik
const changeStart = (
	eventId,
	start,
	formikValues,
	setFieldValue,
	checkConflicts
) => {
	const startDate = makeDate(start.date, start.hour, start.minute);
	let endDate = makeDate(
		start.date,
		formikValues.scheduleEndHour,
		formikValues.scheduleEndMinute
	);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
	}

	setDuration(startDate, endDate, setFieldValue);
	setFieldValue(
		"scheduleEndDate",
		new Date(endDate.setHours(0, 0, 0)).toISOString()
	);
	setFieldValue(
		"scheduleStartDate",
		new Date(startDate.setHours(0, 0, 0)).toISOString()
	);

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

export const changeStartDate = (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeStart(
		eventId,
		{
			date: value,
			hour: formikValues.scheduleStartHour,
			minute: formikValues.scheduleStartMinute,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);
};

export const changeStartHour = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeStart(
		eventId,
		{
			date: formikValues.scheduleStartDate,
			hour: value,
			minute: formikValues.scheduleStartMinute,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleStartHour", value);
};

export const changeStartMinute = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeStart(
		eventId,
		{
			date: formikValues.scheduleStartDate,
			hour: formikValues.scheduleStartHour,
			minute: value,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleStartMinute", value);
};

// changes the end in the formik
const changeEnd = (
	eventId,
	end,
	formikValues,
	setFieldValue,
	checkConflicts
) => {
	const endDate = makeDate(
		formikValues.scheduleStartDate,
		end.hour,
		end.minute
	);
	const startDate = makeDate(
		formikValues.scheduleStartDate,
		formikValues.scheduleStartHour,
		formikValues.scheduleStartMinute
	);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
	}

	setDuration(startDate, endDate, setFieldValue);
	setFieldValue(
		"scheduleEndDate",
		new Date(endDate.setHours(0, 0, 0)).toISOString()
	);

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

export const changeEndHour = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeEnd(
		eventId,
		{
			hour: value,
			minute: formikValues.scheduleEndMinute,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleEndHour", value);
};

export const changeEndMinute = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeEnd(
		eventId,
		{
			hour: formikValues.scheduleEndHour,
			minute: value,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleEndMinute", value);
};

// changes the duration in the formik
const changeDuration = (
	eventId,
	duration,
	formikValues,
	setFieldValue,
	checkConflicts
) => {
	const startDate = makeDate(
		formikValues.scheduleStartDate,
		formikValues.scheduleStartHour,
		formikValues.scheduleStartMinute
	);
	const endDate = new Date(startDate.toISOString());

	endDate.setHours(endDate.getHours() + parseInt(duration.hours));
	endDate.setMinutes(endDate.getMinutes() + parseInt(duration.minutes));

	setFieldValue("scheduleEndHour", makeTwoDigits(endDate.getHours()));
	setFieldValue("scheduleEndMinute", makeTwoDigits(endDate.getMinutes()));
	setFieldValue(
		"scheduleEndDate",
		new Date(endDate.setHours(0, 0, 0)).toISOString()
	);

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

export const changeDurationHour = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeDuration(
		eventId,
		{
			hours: value,
			minutes: formikValues.scheduleDurationMinutes,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleDurationHours", value);
};

export const changeDurationMinute = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeDuration(
		eventId,
		{
			hours: formikValues.scheduleDurationHours,
			minutes: value,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleDurationMinutes", value);
};

// changes the start in the formik
const changeStartMultiple = (
	eventId,
	start,
	formikValues,
	setFieldValue,
	checkConflicts
) => {
	const startDate = makeDate(start.date, start.hour, start.minute);
	let endDate = makeDate(
		start.date,
		formikValues.scheduleEndHour,
		formikValues.scheduleEndMinute
	);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
	}

	setDuration(startDate, endDate, setFieldValue);

	endDate = makeDate(
		formikValues.scheduleEndDate,
		formikValues.scheduleEndHour,
		formikValues.scheduleEndMinute
	);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
	}

	setFieldValue(
		"scheduleEndDate",
		new Date(endDate.setHours(0, 0, 0)).toISOString()
	);
	setFieldValue(
		"scheduleStartDate",
		new Date(startDate.setHours(0, 0, 0)).toISOString()
	);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
	}

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

export const changeStartDateMultiple = (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeStartMultiple(
		eventId,
		{
			date: value,
			hour: formikValues.scheduleStartHour,
			minute: formikValues.scheduleStartMinute,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);
};

export const changeStartHourMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeStartMultiple(
		eventId,
		{
			date: formikValues.scheduleStartDate,
			hour: value,
			minute: formikValues.scheduleStartMinute,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleStartHour", value);
};

export const changeStartMinuteMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeStartMultiple(
		eventId,
		{
			date: formikValues.scheduleStartDate,
			hour: formikValues.scheduleStartHour,
			minute: value,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleStartMinute", value);
};

// changes the end in the formik
export const changeEndDateMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	const endDate = makeDate(
		value,
		formikValues.scheduleEndHour,
		formikValues.scheduleEndMinute
	);
	const startDate = makeDate(
		formikValues.scheduleStartDate,
		formikValues.scheduleStartHour,
		formikValues.scheduleStartMinute
	);

	if (isEndBeforeStart(startDate, endDate)) {
		startDate.setDate(endDate.getDate());
		if (isEndBeforeStart(startDate, endDate)) {
			startDate.setDate(endDate.getDate() - 1);
		}
	}

	setFieldValue(
		"scheduleEndDate",
		new Date(endDate.setHours(0, 0, 0)).toISOString()
	);
	setFieldValue(
		"scheduleStartDate",
		new Date(startDate.setHours(0, 0, 0)).toISOString()
	);

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

// changes the end in the formik
const changeEndMultiple = (
	eventId,
	end,
	formikValues,
	setFieldValue,
	checkConflicts
) => {
	let endDate = makeDate(formikValues.scheduleStartDate, end.hour, end.minute);
	const startDate = makeDate(
		formikValues.scheduleStartDate,
		formikValues.scheduleStartHour,
		formikValues.scheduleStartMinute
	);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
	}

	setDuration(startDate, endDate, setFieldValue);

	endDate = makeDate(formikValues.scheduleEndDate, end.hour, end.minute);

	if (isEndBeforeStart(startDate, endDate)) {
		endDate.setDate(startDate.getDate() + 1);
		setFieldValue(
			"scheduleEndDate",
			new Date(endDate.setHours(0, 0, 0)).toISOString()
		);
	}

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

export const changeEndHourMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeEndMultiple(
		eventId,
		{
			hour: value,
			minute: formikValues.scheduleEndMinute,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleEndHour", value);
};

export const changeEndMinuteMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeEndMultiple(
		eventId,
		{
			hour: formikValues.scheduleEndHour,
			minute: value,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleEndMinute", value);
};

// changes the duration in the formik
const changeDurationMultiple = (
	eventId,
	duration,
	formikValues,
	setFieldValue,
	checkConflicts
) => {
	const startDate = makeDate(
		formikValues.scheduleStartDate,
		formikValues.scheduleStartHour,
		formikValues.scheduleStartMinute
	);
	const endDate = makeDate(
		formikValues.scheduleEndDate,
		formikValues.scheduleStartHour,
		formikValues.scheduleStartMinute
	);

	endDate.setHours(endDate.getHours() + parseInt(duration.hours));
	endDate.setMinutes(endDate.getMinutes() + parseInt(duration.minutes));

	setFieldValue("scheduleEndHour", makeTwoDigits(endDate.getHours()));
	setFieldValue("scheduleEndMinute", makeTwoDigits(endDate.getMinutes()));
	setFieldValue(
		"scheduleEndDate",
		new Date(endDate.setHours(0, 0, 0)).toISOString()
	);

	if (!!checkConflicts) {
		checkConflicts(
			eventId,
			startDate,
			endDate,
			formikValues.captureAgent
		).then((r) => {});
	}
};

export const changeDurationHourMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeDurationMultiple(
		eventId,
		{
			hours: value,
			minutes: formikValues.scheduleDurationMinutes,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleDurationHours", value);
};

export const changeDurationMinuteMultiple = async (
	value,
	formikValues,
	setFieldValue,
	eventId = "",
	checkConflicts = null
) => {
	changeDurationMultiple(
		eventId,
		{
			hours: formikValues.scheduleDurationHours,
			minutes: value,
		},
		formikValues,
		setFieldValue,
		checkConflicts
	);

	setFieldValue("scheduleDurationMinutes", value);
};

// get localized time
export const localizedMoment = (m, currentLanguage) => {
	return moment(m).locale(currentLanguage.dateLocale.code);
};
