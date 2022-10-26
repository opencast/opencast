import React, { useState, useEffect } from "react";
import { Container, Draggable } from "react-smooth-dnd";
import arrayMove from "array-move";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import { changeColumnSelection } from "../../thunks/tableThunks";
import {
	getActivatedColumns,
	getDeactivatedColumns,
	getResourceType,
} from "../../selectors/tableSelectors";

/**
 * This component renders the modal for editing which columns are shown in the table
 */
const EditTableViewModal = ({
	showModal,
	handleClose,
	resource,
	activeColumns,
	deactivatedColumns,
	changeSelectedColumns,
}) => {
	const { t } = useTranslation();

	const originalActiveColumns = activeColumns;
	const originalDeactivatedColumns = deactivatedColumns;

	const [deactivatedCols, setDeactivatedColumns] = useState(deactivatedColumns);
	const [activeCols, setActiveColumns] = useState(activeColumns);

	useEffect(() => {
		setActiveColumns(activeColumns);
		setDeactivatedColumns(deactivatedColumns);
	}, [activeColumns, deactivatedColumns]);

	// closes this modal
	const close = () => {
		handleClose();
	};

	// set deactivated property of column to true (deactivate = true) or false (deactivate = false) and move to corresponding list
	const changeColumn = (column, deactivate) => {
		if (deactivate) {
			setActiveColumns(activeCols.filter((col) => col !== column));
			column = { ...column, deactivated: deactivate };
			setDeactivatedColumns(deactivatedCols.concat(column));
		} else {
			setDeactivatedColumns(deactivatedCols.filter((col) => col !== column));
			column = { ...column, deactivated: deactivate };
			setActiveColumns(activeCols.concat(column));
		}
	};

	// save new values of which columns are active or deactivated and apply changes to table
	const save = () => {
		const settings = activeCols.concat(deactivatedCols);
		changeSelectedColumns(settings);
		close();
	};

	// reset active and deactivated columns to how they were when the dialogue was opened (used when closing without saving)
	const clearData = () => {
		setActiveColumns(originalActiveColumns);
		setDeactivatedColumns(originalDeactivatedColumns);
		close();
	};

	// change column order based on where column was dragged and dropped
	const onDrop = ({ removedIndex, addedIndex }) => {
		setActiveColumns((columns) => arrayMove(columns, removedIndex, addedIndex));
	};

	return (
		<>
			{showModal && (
				<>
					<div className="modal-animation modal-overlay" />
					<section
						className="modal active modal-animation"
						id="edit-table-view-modal"
					>
						<header>
							<a
								className="fa fa-times close-modal"
								onClick={() => {
									clearData();
									close();
								}}
							/>
							<h2>{t("PREFERENCES.TABLE.CAPTION") /* Edit Table View */}</h2>
						</header>

						<div className="modal-content">
							<div className="modal-body">
								<div className="tab-description for-header">
									<p>
										{t("PREFERENCES.TABLE.SUBHEADING", {
											tableName: t(
												"EVENTS." + resource.toUpperCase() + ".TABLE.CAPTION"
											),
										})}
									</p>
								</div>

								<div className="row">
									<div className="col">
										<div className="obj drag-available-column">
											<header>
												<h2>
													{
														t(
															"PREFERENCES.TABLE.AVAILABLE_COLUMNS"
														) /* Available Columns */
													}
												</h2>
											</header>
											<ul className="drag-drop-items">
												{deactivatedCols.map((column, key) =>
													column ? (
														<li className="drag-item" key={key}>
															<div className="title">{t(column.label)}</div>
															<a
																className="move-item add"
																onClick={() => changeColumn(column, false)}
															/>
														</li>
													) : null
												)}
											</ul>
										</div>
									</div>

									<div className="col">
										<div className="obj drag-selected-column">
											<header>
												<h2>
													{
														t(
															"PREFERENCES.TABLE.SELECTED_COLUMNS"
														) /* Selected Columns */
													}
												</h2>
											</header>
											<ul className="drag-drop-items">
												<li>
													<Container
														dragHandleSelector=".drag-handle"
														lockAxis="y"
														onDrop={onDrop}
													>
														{activeCols.map((column, key) =>
															column ? (
																<Draggable className="drag-item" key={key}>
																	<div className="drag-handle">
																		<div className="title">
																			{t(column.label)}
																		</div>
																		<a
																			className="move-item remove"
																			onClick={() => changeColumn(column, true)}
																		/>
																	</div>
																</Draggable>
															) : null
														)}
													</Container>
												</li>
											</ul>
										</div>
									</div>
								</div>

								<div className="tab-description for-footer">
									<p>
										{/* The order and selection will be saved automatically.
                                Press "Reset" to restore the default view. */}
										{t("PREFERENCES.TABLE.FOOTER_TEXT", {
											resetTranslation: t("RESET"),
										})}
									</p>
								</div>
							</div>
						</div>

						<footer>
							<div className="pull-left">
								<button onClick={() => clearData()} className="cancel active">
									{t("CANCEL") /*Cancel*/}
								</button>
							</div>
							<div className="pull-right">
								<button onClick={() => save()} className="submit active">
									{t("SAVE") /* Save As Default */}
								</button>
							</div>
						</footer>
					</section>
				</>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	resource: getResourceType(state),
	deactivatedColumns: getDeactivatedColumns(state),
	activeColumns: getActivatedColumns(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	changeSelectedColumns: (updatedColumns) =>
		dispatch(changeColumnSelection(updatedColumns)),
});

export default connect(mapStateToProps, mapDispatchToProps)(EditTableViewModal);
