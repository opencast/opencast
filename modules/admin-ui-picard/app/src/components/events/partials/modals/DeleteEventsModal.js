import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import cn from "classnames";
import { deleteMultipleEvent } from "../../../../thunks/eventThunks";
import { getSelectedRows } from "../../../../selectors/tableSelectors";
import { connect } from "react-redux";

/**
 * This component manages the delete bulk action
 */
const DeleteEventsModal = ({ close, selectedRows, deleteMultipleEvent }) => {
	const { t } = useTranslation();

	const [allChecked, setAllChecked] = useState(true);
	const [selectedEvents, setSelectedEvents] = useState(selectedRows);

	const deleteSelectedEvents = () => {
		deleteMultipleEvent(selectedEvents);
		close();
	};

	// Select or deselect all rows in table
	const onChangeAllSelected = (e) => {
		const selected = e.target.checked;
		setAllChecked(selected);
		let changedSelection = selectedEvents.map((event) => {
			return {
				...event,
				selected: selected,
			};
		});
		setSelectedEvents(changedSelection);
	};

	// Handle change of checkboxes indicating which events to consider further
	const onChangeSelected = (e, id) => {
		const selected = e.target.checked;
		let changedEvents = selectedEvents.map((event) => {
			if (event.id === id) {
				return {
					...event,
					selected: selected,
				};
			} else {
				return event;
			}
		});
		setSelectedEvents(changedEvents);

		if (!selected) {
			setAllChecked(false);
		}
		if (changedEvents.every((event) => event.selected === true)) {
			setAllChecked(true);
		}
	};
	return (
		<>
			<div className="modal-animation modal-overlay" />
			<section
				className="modal active modal-open"
				id="delete-events-status-modal"
				style={{ display: "block" }}
			>
				<header>
					<a onClick={close} className="fa fa-times close-modal" />
					<h2>{t("BULK_ACTIONS.DELETE.EVENTS.CAPTION")}</h2>
				</header>

				<div className="modal-content active">
					<div className="modal-body">
						<div className="full-col">
							<div className="list-obj">
								<div className="modal-alert danger obj">
									<p>{t("BULK_ACTIONS.DELETE_EVENTS_WARNING_LINE1")}</p>
									<p>{t("BULK_ACTIONS.DELETE_EVENTS_WARNING_LINE2")}</p>
								</div>
								{/*todo: only show if scheduling Authorized*/}
								<div>
									<p>{t("BULK_ACTIONS.DELETE.EVENTS.UNAUTHORIZED")}</p>
								</div>

								<div className="full-col">
									<div className="obj">
										<header>
											{t("BULK_ACTIONS.DELETE.EVENTS.DELETE_EVENTS")}
										</header>
										<table className="main-tbl">
											<thead>
												<tr>
													<th className="small">
														<input
															type="checkbox"
															checked={allChecked}
															onChange={(e) => onChangeAllSelected(e)}
															className="select-all-cbox"
														/>
													</th>
													<th>{t("EVENTS.EVENTS.TABLE.TITLE")}</th>
													<th>{t("EVENTS.EVENTS.TABLE.PRESENTERS")}</th>
												</tr>
											</thead>
											<tbody>
												{/* Repeat for each marked event*/}
												{selectedEvents.map((event, key) => (
													<tr key={key}>
														<td>
															<input
																className="child-cbox"
																name="selection"
																type="checkbox"
																checked={event.selected}
																onChange={(e) => onChangeSelected(e, event.id)}
															/>
														</td>
														<td>{event.title}</td>
														<td>
															{/* Repeat for each presenter*/}
															{event.presenters.map((presenter, key) => (
																<span className="metadata-entry" key={key}>
																	{presenter}
																</span>
															))}
														</td>
													</tr>
												))}
											</tbody>
										</table>
									</div>
								</div>
							</div>
						</div>
					</div>
				</div>

				<footer>
					<button
						onClick={() => deleteSelectedEvents()}
						disabled={!selectedEvents.some((event) => event.selected === true)}
						className={cn("danger", {
							active: selectedEvents.some((event) => event.selected === true),
							inactive: !selectedEvents.some(
								(event) => event.selected === true
							),
						})}
					>
						{t("WIZARD.DELETE")}
					</button>
					<button onClick={() => close()} className="cancel">
						{t("CANCEL")}
					</button>
				</footer>

				<div className="btm-spacer" />
			</section>
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	selectedRows: getSelectedRows(state),
});

const mapDispatchToProps = (dispatch) => ({
	deleteMultipleEvent: (selectedEvents) =>
		dispatch(deleteMultipleEvent(selectedEvents)),
});

export default connect(mapStateToProps, mapDispatchToProps)(DeleteEventsModal);
