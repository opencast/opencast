import React from "react";
import { connect } from "react-redux";
import EventDetailsTabHierarchyNavigation from "./EventDetailsTabHierarchyNavigation";
import Notifications from "../../../shared/Notifications";
import {
	getAssetAttachmentDetails,
	isFetchingAssets,
} from "../../../../selectors/eventDetailsSelectors";
import { humanReadableBytesFilter } from "../../../../utils/eventDetailsUtils";

/**
 * This component manages the attachment details sub-tab for assets tab of event details modal
 */
const EventDetailsAssetAttachmentDetails = ({
	eventId,
	t,
	setHierarchy,
	attachment,
	isFetching,
}) => {
	const openSubTab = (subTabName) => {
		setHierarchy(subTabName);
	};

	return (
		<div className="modal-content">
			{/* Hierarchy navigation */}
			<EventDetailsTabHierarchyNavigation
				openSubTab={openSubTab}
				hierarchyDepth={1}
				translationKey0={"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.TITLE"}
				subTabArgument0={"asset-attachments"}
				translationKey1={
					"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.TITLE"
				}
				subTabArgument1={"attachment-details"}
			/>

			<div className="modal-body">
				{/* Notifications */}
				<Notifications context="not_corner" />

				{/* table with details for the attachment */}
				<div className="full-col">
					<div className="obj tbl-container operations-tbl">
						<header>
							{
								t(
									"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.CAPTION"
								) /* Attachment Details */
							}
						</header>
						<div className="obj-container">
							<table className="main-tbl">
								{isFetching || (
									<tbody>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.ID"
													) /* Id */
												}
											</td>
											<td>{attachment.id}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.TYPE"
													) /* Type */
												}
											</td>
											<td>{attachment.type}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.MIMETYPE"
													) /* Mimetype */
												}
											</td>
											<td>{attachment.mimetype}</td>
										</tr>
										{!!attachment.size && attachment.size > 0 && (
											<tr>
												<td>
													{
														t(
															"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.SIZE"
														) /* Size */
													}
												</td>
												<td>{humanReadableBytesFilter(attachment.size)}</td>
											</tr>
										)}
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.CHECKSUM"
													) /* Checksum */
												}
											</td>
											<td>{attachment.checksum}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.REFERENCE"
													) /* Reference */
												}
											</td>
											<td>{attachment.reference}</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.TAGS"
													) /* Tags */
												}
											</td>
											<td>
												{!!attachment.tags && attachment.tags.length > 0
													? attachment.tags.join(", ")
													: null}
											</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.DETAILS.URL"
													) /* Link */
												}
											</td>
											<td>
												<a
													className="fa fa-external-link"
													href={attachment.url}
												/>
											</td>
										</tr>
									</tbody>
								)}
							</table>
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	isFetching: isFetchingAssets(state),
	attachment: getAssetAttachmentDetails(state),
});

export default connect(mapStateToProps)(EventDetailsAssetAttachmentDetails);
