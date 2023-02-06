import React from "react";
import moment from "moment";
import { getCurrentLanguageInformation } from "../../utils/utils";
import { MuiPickersUtilsProvider } from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import { DatePicker } from "@material-ui/pickers";
import { createTheme, ThemeProvider } from "@material-ui/core";
import { Field, Formik } from "formik";
import BarChart from "./BarChart";
import {
	availableCustomStatisticDataResolutions,
	fixedStatisticDataResolutions,
	statisticDateFormatStrings,
	statisticTimeModes,
} from "../../configs/statisticsConfig";
import { localizedMoment } from "../../utils/dateUtils";

/**
 * This component visualizes statistics with data of type time series
 */
const TimeSeriesStatistics = ({
	t,
	resourceId,
	statTitle,
	providerId,
	fromDate,
	toDate,
	timeMode,
	dataResolution,
	statDescription,
	onChange,
	exportUrl,
	exportFileName,
	totalValue,
	sourceData,
	chartLabels,
	chartOptions,
}) => {
	// Style to bring date picker pop up to front
	const theme = createTheme({
		props: {
			MuiDialog: {
				style: {
					zIndex: "2147483550",
				},
			},
		},
	});

	// Style for date picker
	const datePickerStyle = {
		border: "1px solid #dedddd",
		borderRadius: "4px",
		marginLeft: "3px",
		marginRight: "5px",
	};

	// Style for radio buttons
	const radioButtonStyle = {
		backgroundColor: "whitesmoke",
		backgroundImage: "linear-gradient(whitesmoke, #dedddd)",
		color: "#666666",
	};

	// available modes of choosing statistic timeframe
	const timeModes = statisticTimeModes;

	// data resolutions (or time granularity) for statistics with year or month timeframe
	const fixedDataResolutions = fixedStatisticDataResolutions;

	// available data resolutions (or time granularity) for statistics with custom timeframe
	const availableCustomDataResolutions = availableCustomStatisticDataResolutions;

	// date format strings
	const formatStrings = statisticDateFormatStrings;

	// Get info about the current language and its date locale
	const currentLanguage = getCurrentLanguageInformation();

	// change formik values and get new statistic values from API
	const change = (setFormikValue, timeMode, from, to, dataResolution) => {
		if (timeMode === "year" || timeMode === "month") {
			from = moment(from).clone().startOf(timeMode).format("YYYY-MM-DD");
			to = moment(from).clone().endOf(timeMode).format("YYYY-MM-DD");
			setFormikValue("fromDate", from);
			setFormikValue("toDate", to);
			setFormikValue("dataResolution", fixedDataResolutions[timeMode]);
			dataResolution = fixedDataResolutions[timeMode];
		}

		onChange(resourceId, providerId, from, to, dataResolution, timeMode);
	};

	// change time mode in formik and get new values from API
	const changeTimeMode = async (
		newTimeMode,
		setFormikValue,
		from,
		to,
		dataResolution
	) => {
		setFormikValue("timeMode", newTimeMode);
		change(setFormikValue, newTimeMode, from, to, dataResolution);
	};

	// change custom from date in formik and get new values from API
	const changeFrom = async (
		newFrom,
		setFormikValue,
		timeMode,
		to,
		dataResolution
	) => {
		setFormikValue("fromDate", newFrom);
		change(setFormikValue, timeMode, newFrom, to, dataResolution);
	};

	// change custom to date in formik and get new values from API
	const changeTo = async (
		newTo,
		setFormikValue,
		timeMode,
		from,
		dataResolution
	) => {
		setFormikValue("toDate", newTo);
		change(setFormikValue, timeMode, from, newTo, dataResolution);
	};

	// change custom time granularity in formik and get new values from API
	const changeGranularity = async (
		granularity,
		setFormikValue,
		timeMode,
		from,
		to
	) => {
		setFormikValue("dataResolution", granularity);
		change(setFormikValue, timeMode, from, to, granularity);
	};

	// format selected time to display as name of timeframe
	const formatSelectedTimeframeName = (from, timeMode) => {
		return localizedMoment(from, currentLanguage).format(
			formatStrings[timeMode]
		);
	};

	// change to and from dates in formik to previous timeframe and get new values from API
	const selectPrevious = (setFormikValue, from, timeMode, dataResolution) => {
		const newFrom = moment(from)
			.subtract(1, timeMode + "s")
			.format("YYYY-MM-DD");
		const to = newFrom;
		change(setFormikValue, timeMode, newFrom, to, dataResolution);
	};

	// change to and from dates in formik to next timeframe and get new values from API
	const selectNext = (setFormikValue, from, timeMode, dataResolution) => {
		const newFrom = moment(from)
			.add(1, timeMode + "s")
			.format("YYYY-MM-DD");
		const to = newFrom;
		change(setFormikValue, timeMode, newFrom, to, dataResolution);
	};

	return (
		/* Initialize form */
		<MuiPickersUtilsProvider
			utils={DateFnsUtils}
			locale={currentLanguage.dateLocale}
		>
			<Formik
				enableReinitialize
				initialValues={{
					timeMode: timeMode,
					dataResolution: dataResolution,
					fromDate: moment(fromDate).startOf(timeMode).format("YYYY-MM-DD"),
					toDate: moment(toDate).endOf(timeMode).format("YYYY-MM-DD"),
				}}
				onSubmit={(values) => {}}
			>
				{(formik) => (
					<div className="statistics-graph">
						{/* download link for a statistic file */}
						<div className="download">
							<a
								className="download-icon"
								href={exportUrl}
								download={exportFileName(statTitle)}
							/>
						</div>

						{/* radio buttons for selecting the mode of choosing the timeframe of statistic */}
						<div className="mode">
							{timeModes.map((mode, key) => (
								<label
									htmlFor={providerId + "-mode-" + key}
									style={
										formik.values.timeMode === mode.value
											? radioButtonStyle
											: {}
									}
									key={key}
								>
									<Field
										type="radio"
										style={{ display: "none" }}
										name="timeMode"
										value={mode.value}
										id={providerId + "-mode-" + key}
										onChange={(event) =>
											changeTimeMode(
												event.target.value,
												formik.setFieldValue,
												formik.values.fromDate,
												formik.values.toDate,
												formik.values.dataResolution
											)
										}
									/>
									{t("STATISTICS.TIME_MODES." + mode.translation)}
								</label>
							))}
						</div>

						{/* statistics total value */}
						<div className="total">
							<span>{t("STATISTICS.TOTAL") /* Total */}</span>
							<span>{": " + totalValue}</span>
						</div>

						{/* timeframe selection */}

						{(formik.values.timeMode === "year" ||
							formik.values.timeMode === "month") && (
							/* year/month selection for statistic via previous and next buttons */
							<span className="preset">
								<a
									className="navigation prev"
									onClick={() =>
										selectPrevious(
											formik.setFieldValue,
											formik.values.fromDate,
											formik.values.timeMode,
											formik.values.dataResolution
										)
									}
								/>
								<div>
									{formatSelectedTimeframeName(
										formik.values.fromDate,
										formik.values.timeMode
									)}
								</div>
								<a
									className="navigation next"
									onClick={() =>
										selectNext(
											formik.setFieldValue,
											formik.values.fromDate,
											formik.values.timeMode,
											formik.values.dataResolution
										)
									}
								/>
							</span>
						)}

						{formik.values.timeMode === "custom" && (
							/* custom timeframe selection for statistic */
							<span className="custom">
								{/* time range selection */}
								<div className="range">
									{/* date picker for selecting start date of the statistic */}
									<span>{t("STATISTICS.FROM") /* From */}</span>
									<div className="chosen-container">
										<ThemeProvider theme={theme}>
											<DatePicker
												name="fromDate"
												style={datePickerStyle}
												value={formik.values.fromDate}
												placeholder={t(
													"EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.START_DATE"
												)}
												onChange={(value) =>
													changeFrom(
														value,
														formik.setFieldValue,
														formik.values.timeMode,
														formik.values.toDate,
														formik.values.dataResolution
													)
												}
											/>
										</ThemeProvider>
									</div>

									{/* date picker for selecting end date of the statistic */}
									<span>{t("STATISTICS.TO") /* To */}</span>
									<div className="chosen-container">
										<ThemeProvider theme={theme}>
											<DatePicker
												name="toDate"
												style={datePickerStyle}
												value={formik.values.toDate}
												placeholder={t(
													"EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.END_DATE"
												)}
												onChange={(value) =>
													changeTo(
														value,
														formik.setFieldValue,
														formik.values.timeMode,
														formik.values.fromDate,
														formik.values.dataResolution
													)
												}
											/>
										</ThemeProvider>
									</div>
								</div>

								{/* time granularity selection */}
								<div>
									<span>
										{t("STATISTICS.GRANULARITY") + " " /* Granularity */}
									</span>
									<div className="chosen-container chosen-container-single">
										{/* drop-down for selecting the time granularity of the statistic */}
										<Field
											className="chosen-single"
											name="dataResolution"
											as="select"
											data-width="'100px'"
											onChange={(event) =>
												changeGranularity(
													event.target.value,
													formik.setFieldValue,
													formik.values.timeMode,
													formik.values.fromDate,
													formik.values.toDate
												)
											}
											placeholder={t(
												"EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.MINUTE"
											)}
										>
											<option value="" hidden />
											{availableCustomDataResolutions.map((option, key) => (
												<option value={option.value} key={key}>
													{t("STATISTICS.TIME_GRANULARITIES." + option.label)}
												</option>
											))}
										</Field>
									</div>
								</div>
							</span>
						)}

						<br />
						{/* bar chart with visualization of statistic data */}
						<BarChart
							values={sourceData}
							axisLabels={chartLabels}
							options={chartOptions}
						/>

						{/* statistic description */}
						<p>{t(statDescription)}</p>
					</div>
				)}
			</Formik>
		</MuiPickersUtilsProvider>
	);
};

export default TimeSeriesStatistics;
