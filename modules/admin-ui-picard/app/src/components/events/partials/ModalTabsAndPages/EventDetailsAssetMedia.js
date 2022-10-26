import React from "react";
import { connect } from "react-redux";
import EventDetailsTabHierarchyNavigation from "./EventDetailsTabHierarchyNavigation";
import Notifications from "../../../shared/Notifications";
import {
	getAssetMedia,
	isFetchingAssets,
} from "../../../../selectors/eventDetailsSelectors";
import { fetchAssetMediaDetails } from "../../../../thunks/eventDetailsThunks";

/**
 * This component manages the media sub-tab for assets tab of event details modal
 */
const EventDetailsAssetMedia = ({
	eventId,
	t,
	setHierarchy,
	media,
	isFetching,
	loadMediaDetails,
}) => {
	const openSubTab = (subTabName, mediaId = "") => {
		if (subTabName === "media-details") {
			loadMediaDetails(eventId, mediaId).then((r) => {});
		}
		setHierarchy(subTabName);
	};

	return (
		<div className="modal-content">
			{/* Hierarchy navigation */}
			<EventDetailsTabHierarchyNavigation
				openSubTab={openSubTab}
				hierarchyDepth={0}
				translationKey0={"EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.TITLE"}
				subTabArgument0={"asset-media"}
			/>

			<div className="modal-body">
				{/* Notifications */}
				<Notifications context="not_corner" />

				{/* table with list of media */}
				<div className="full-col">
					<div className="obj tbl-container operations-tbl">
						<header>
							{t("EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.CAPTION") /* Media */}
						</header>
						<div className="obj-container">
							<table cellPadding="0" cellSpacing="0" className="main-tbl">
								<thead>
									<tr>
										<th>
											{t("EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.ID") /* ID */}
										</th>
										<th>
											{t("EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.TYPE") /* Type */}
										</th>
										<th>
											{
												t(
													"EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.MIMETYPE"
												) /* Mimetype */
											}
										</th>
										<th>
											{t("EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.TAGS") /* Tags */}
										</th>
										<th className="medium" />
									</tr>
								</thead>
								<tbody>
									{isFetching ||
										media.map((item, key) => (
											<tr key={key}>
												<td>
													<a href={item.url}>{item.id}</a>
												</td>
												<td>{item.type}</td>
												<td>{item.mimetype}</td>
												<td>
													{!!item.tags && item.tags.length > 0
														? item.tags.join(", ")
														: null}
												</td>
												<td>
													<a
														className="details-link"
														onClick={() => openSubTab("media-details", item.id)}
													>
														{
															t(
																"EVENTS.EVENTS.DETAILS.ASSETS.DETAILS"
															) /* Details */
														}
													</a>
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
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	isFetching: isFetchingAssets(state),
	media: getAssetMedia(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	loadMediaDetails: (eventId, mediaId) =>
		dispatch(fetchAssetMediaDetails(eventId, mediaId)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsAssetMedia);
