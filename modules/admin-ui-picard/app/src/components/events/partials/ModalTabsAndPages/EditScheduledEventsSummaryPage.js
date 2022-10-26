import React, { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import WizardNavigationButtons from "../../../shared/wizard/WizardNavigationButtons";
import { getMetadataCollectionFieldName } from "../../../../utils/resourceUtils";
import { connect } from "react-redux";
import { getSchedulingSeriesOptions } from "../../../../selectors/eventSelectors";

/**
 * This component renders the summary page of the edit scheduled bulk action
 */
const EditScheduledEventsSummaryPage = ({
	previousPage,
	formik,
	seriesOptions,
}) => {
	const { t } = useTranslation();

	// Changes applied to events
	const [changes, setChanges] = useState([]);

	useEffect(() => {
		// Check on mount if changes on events where applied on page before
		checkForChanges();
	}, []);

	const checkForChanges = () => {
		let changed = [];

		// Loop through each event selected for editing and compare original values and changed values
		for (let i = 0; i < formik.values.editedEvents.length; i++) {
			let event = formik.values.editedEvents[i];
			let eventChanges = {
				eventId: event.eventId,
				title: event.title,
				changes: [],
			};
			if (isChanged(event.title, event.changedTitle)) {
				eventChanges.changes.push({
					type: "EVENTS.EVENTS.DETAILS.METADATA.TITLE",
					previous: event.title,
					next: event.changedTitle,
				});
			}
			if (isChanged(event.series, event.changedSeries)) {
				eventChanges.changes.push({
					type: "EVENTS.EVENTS.DETAILS.METADATA.SERIES",
					previous: getMetadataCollectionFieldName(
						{ collection: seriesOptions },
						{ value: event.series }
					),
					next: getMetadataCollectionFieldName(
						{ collection: seriesOptions },
						{ value: event.changedSeries }
					),
				});
			}
			if (
				isChanged(
					event.startTimeHour + ":" + event.startTimeMinutes,
					event.changedStartTimeHour + ":" + event.changedStartTimeMinutes
				)
			) {
				eventChanges.changes.push({
					type: "EVENTS.EVENTS.TABLE.START",
					previous: event.startTimeHour + ":" + event.startTimeMinutes,
					next:
						event.changedStartTimeHour + ":" + event.changedStartTimeMinutes,
				});
			}
			if (
				isChanged(
					event.endTimeHour + ":" + event.endTimeMinutes,
					event.changedEndTimeHour + ":" + event.changedEndTimeMinutes
				)
			) {
				eventChanges.changes.push({
					type: "EVENTS.EVENTS.TABLE.END",
					previous: event.endTimeHour + ":" + event.endTimeMinutes,
					next: event.changedEndTimeHour + ":" + event.changedEndTimeMinutes,
				});
			}
			if (isChanged(event.location, event.changedLocation)) {
				eventChanges.changes.push({
					type: "EVENTS.EVENTS.TABLE.LOCATION",
					previous: event.location,
					next: event.changedLocation,
				});
			}
			/* the following six lines can be commented in, when the possibility of a selection of individual inputs is desired and the backend has been adapted to support it
            if (isArrayChanged(event.deviceInputs.split(','), event.changedDeviceInputs)){
                eventChanges.changes.push({
                    type: 'EVENTS.EVENTS.DETAILS.SOURCE.PLACEHOLDER.INPUTS',
                    previous: event.deviceInputs,
                    next: event.changedDeviceInputs.join(',')
                });
            }*/
			if (isChanged(event.weekday, event.changedWeekday)) {
				eventChanges.changes.push({
					type: "EVENTS.EVENTS.TABLE.WEEKDAY",
					previous: t("EVENTS.EVENTS.NEW.WEEKDAYSLONG." + event.weekday),
					next: t("EVENTS.EVENTS.NEW.WEEKDAYSLONG." + event.changedWeekday),
				});
			}

			// If there were changes push them to changed array
			if (eventChanges.changes.length > 0) {
				changed.push(eventChanges);
				// Keep ids of changed events (used later)
				formik.setFieldValue(
					"changedEvent",
					formik.values.changedEvents.push(event.eventId)
				);
			}
		}

		// Set changes state
		setChanges(changed);
	};

	// Compare two values
	const isChanged = (oldValue, newValue) => {
		return oldValue !== newValue;
	};

	return (
		<>
			<div className="modal-content active">
				<div className="modal-body">
					{changes.length > 0 ? (
						<div className="full-col">
							{/*Repeat for each changed event*/}
							{changes.map((event, key) => (
								<div key={key} className="obj tbl-list">
									<header>
										{t(
											"BULK_ACTIONS.EDIT_EVENTS.SUMMARY.SINGLE_EVENT_CAPTION",
											{ title: event.title }
										)}
									</header>
									<div className="obj-container">
										<table className="main-tbl">
											<thead>
												<tr>
													<th className="fit">
														{t("BULK_ACTIONS.EDIT_EVENTS.SUMMARY.TYPE")}
													</th>
													<th className="fit">
														{t("BULK_ACTIONS.EDIT_EVENTS.SUMMARY.PREVIOUS")}
													</th>
													<th className="fit">
														{t("BULK_ACTIONS.EDIT_EVENTS.SUMMARY.NEXT")}
													</th>
												</tr>
											</thead>
											<tbody>
												{/* Add table row with old value and new one if something has changed */}
												{event.changes.map((row, key) => (
													<tr key={key}>
														<td>{t(row.type)}</td>
														<td>{row.previous}</td>
														<td className="highlighted-cell">{row.next}</td>
													</tr>
												))}
											</tbody>
										</table>
									</div>
								</div>
							))}
						</div>
					) : (
						<div className="row">
							{/* Show only if there no changes*/}
							<div className="alert sticky warning">
								<p>{t("BULK_ACTIONS.EDIT_EVENTS.GENERAL.NOCHANGES")}</p>
							</div>
						</div>
					)}
				</div>
			</div>

			{/* Navigation buttons */}
			<WizardNavigationButtons
				isLast
				formik={formik}
				previousPage={previousPage}
			/>
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	seriesOptions: getSchedulingSeriesOptions(state),
});

export default connect(mapStateToProps, null)(EditScheduledEventsSummaryPage);
