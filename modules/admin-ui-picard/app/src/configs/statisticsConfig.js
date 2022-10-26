/* this file contains constants for available options and used date formats for statistics */

// available modes of choosing statistic timeframe
export const statisticTimeModes = [
	{
		value: "year",
		translation: "Year",
	},
	{
		value: "month",
		translation: "Month",
	},
	{
		value: "custom",
		translation: "Custom",
	},
];

// data resolutions (or time granularity) for statistics with year or month timeframe
export const fixedStatisticDataResolutions = {
	month: "daily",
	year: "monthly",
};

// available data resolutions (or time granularity) for statistics with custom timeframe
export const availableCustomStatisticDataResolutions = [
	{ label: "Yearly", value: "yearly" },
	{ label: "Monthly", value: "monthly" },
	{ label: "Daily", value: "daily" },
	{ label: "Hourly", value: "hourly" },
];

// date format strings
export const statisticDateFormatStrings = {
	month: "MMMM YYYY",
	year: "YYYY",
};
