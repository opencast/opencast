import React, { useEffect } from "react";
import Notifications from "../../../shared/Notifications";
import { connect } from "react-redux";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";
import {
	fetchAssetAttachments,
	fetchAssetCatalogs,
	fetchAssetMedia,
	fetchAssetPublications,
	fetchAssets,
	fetchWorkflows,
} from "../../../../thunks/eventDetailsThunks";
import {
	getAssets,
	getUploadAssetOptions,
	isFetchingAssets,
	isTransactionReadOnly,
} from "../../../../selectors/eventDetailsSelectors";
import { getWorkflow } from "../../../../selectors/eventDetailsSelectors";
import { getUserInformation } from "../../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../../utils/utils";
import { isFetchingAssetUploadOptions } from "../../../../selectors/eventSelectors";

/**
 * This component manages the main assets tab of event details modal
 */
const EventDetailsAssetsTab = ({
	eventId,
	t,
	setHierarchy,
	fetchAssets,
	fetchAttachments,
	fetchCatalogs,
	fetchMedia,
	fetchPublications,
	assets,
	transactionsReadOnly,
	uploadAssetOptions,
	isFetching,
	isFetchingAssetUploadOptions,
	user,
}) => {
	useEffect(() => {
		removeNotificationWizardForm();
		fetchAssets(eventId).then((r) => {});
	}, []);

	const openSubTab = (
		subTabName,
		newassetupload,
		bool1 = false,
		bool2 = true
	) => {
		removeNotificationWizardForm();
		if (subTabName === "asset-attachments") {
			fetchAttachments(eventId).then((r) => {});
		} else if (subTabName === "asset-attachments") {
			fetchAttachments(eventId).then((r) => {});
		} else if (subTabName === "asset-catalogs") {
			fetchCatalogs(eventId).then((r) => {});
		} else if (subTabName === "asset-media") {
			fetchMedia(eventId).then((r) => {});
		} else if (subTabName === "asset-publications") {
			fetchPublications(eventId).then((r) => {});
		}
		setHierarchy(subTabName);
	};

	return (
		<div className="modal-content">
			<div className="modal-body">
				{/* Notifications */}
				<Notifications context="not_corner" />

				{/* table with types of assets */}
				<div className="full-col">
					<div className="obj tbl-container operations-tbl">
						{" "}
						{/* Assets */}
						<header>{t("EVENTS.EVENTS.DETAILS.ASSETS.CAPTION")}</header>
						<div className="obj-container">
							{isFetching || (
								<table cellPadding="0" cellSpacing="0" className="main-tbl">
									<thead>
										<tr>
											<th>
												{" "}
												{t("EVENTS.EVENTS.DETAILS.ASSETS.TYPE") /* Type */}
											</th>
											<th>
												{" "}
												{t("EVENTS.EVENTS.DETAILS.ASSETS.SIZE") /* Size */}
											</th>
											<th className="medium">
												{!isFetchingAssetUploadOptions &&
													!!uploadAssetOptions &&
													uploadAssetOptions.filter(
														(asset) => asset.type !== "track"
													).length > 0 &&
													!transactionsReadOnly &&
													hasAccess(
														"ROLE_UI_EVENTS_DETAILS_ASSETS_EDIT",
														user
													) && (
														<a
															className="details-link"
															onClick={() =>
																openSubTab(
																	"add-asset",
																	"newassetupload",
																	false,
																	true
																)
															}
														>
															{t("EVENTS.EVENTS.NEW.UPLOAD_ASSET.ADD")}
														</a>
													)}
											</th>
										</tr>
									</thead>
									<tbody>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.ATTACHMENTS.CAPTION"
													) /* Attachments */
												}
											</td>
											<td>{assets.attachments}</td>
											<td>
												{assets.attachments > 0 && (
													<a
														className="details-link"
														onClick={() =>
															openSubTab("asset-attachments", "attachment")
														}
													>
														{
															t(
																"EVENTS.EVENTS.DETAILS.ASSETS.DETAILS"
															) /* Details */
														}
													</a>
												)}
											</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.CATALOGS.CAPTION"
													) /* Catalogs */
												}
											</td>
											<td>{assets.catalogs}</td>
											<td>
												{assets.catalogs > 0 && (
													<a
														className="details-link"
														onClick={() =>
															openSubTab("asset-catalogs", "catalog")
														}
													>
														{
															t(
																"EVENTS.EVENTS.DETAILS.ASSETS.DETAILS"
															) /* Details */
														}
													</a>
												)}
											</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.MEDIA.CAPTION"
													) /* Media */
												}
											</td>
											<td>{assets.media}</td>
											<td>
												{assets.media > 0 && (
													<a
														className="details-link"
														onClick={() => openSubTab("asset-media", "media")}
													>
														{
															t(
																"EVENTS.EVENTS.DETAILS.ASSETS.DETAILS"
															) /* Details */
														}
													</a>
												)}
											</td>
										</tr>
										<tr>
											<td>
												{
													t(
														"EVENTS.EVENTS.DETAILS.ASSETS.PUBLICATIONS.CAPTION"
													) /* Publications */
												}
											</td>
											<td>{assets.publications}</td>
											<td>
												{assets.publications > 0 && (
													<a
														className="details-link"
														onClick={() =>
															openSubTab("asset-publications", "publication")
														}
													>
														{
															t(
																"EVENTS.EVENTS.DETAILS.ASSETS.DETAILS"
															) /* Details */
														}
													</a>
												)}
											</td>
										</tr>
									</tbody>
								</table>
							)}
						</div>
					</div>
				</div>
			</div>
		</div>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	assets: getAssets(state),
	isFetching: isFetchingAssets(state),
	transactionsReadOnly: isTransactionReadOnly(state),
	uploadAssetOptions: getUploadAssetOptions(state),
	assetUploadWorkflowDefId: getWorkflow(state).id,
	user: getUserInformation(state),
	isFetchingAssetUploadOptions: isFetchingAssetUploadOptions(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	fetchAssets: (eventId) => dispatch(fetchAssets(eventId)),
	fetchAttachments: (eventId) => dispatch(fetchAssetAttachments(eventId)),
	fetchCatalogs: (eventId) => dispatch(fetchAssetCatalogs(eventId)),
	fetchMedia: (eventId) => dispatch(fetchAssetMedia(eventId)),
	fetchPublications: (eventId) => dispatch(fetchAssetPublications(eventId)),
	fetchWorkflows: (eventId) => dispatch(fetchWorkflows(eventId)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsAssetsTab);
