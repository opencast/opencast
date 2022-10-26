import React, { useState } from "react";
import { connect } from "react-redux";
import { useTranslation } from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import { deleteGroup } from "../../../thunks/groupThunks";
import GroupDetailsModal from "./modal/GroupDetailsModal";
import { fetchGroupDetails } from "../../../thunks/groupDetailsThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the action cells of groups in the table view
 */
const GroupsActionsCell = ({ row, deleteGroup, fetchGroupDetails, user }) => {
	const { t } = useTranslation();

	const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
	const [displayGroupDetails, setGroupDetails] = useState(false);

	const hideDeleteConfirmation = () => {
		setDeleteConfirmation(false);
	};

	const deletingGroup = (id) => {
		deleteGroup(id);
	};

	const hideGroupDetails = () => {
		setGroupDetails(false);
	};

	const showGroupDetails = async () => {
		await fetchGroupDetails(row.id);

		setGroupDetails(true);
	};

	return (
		<>
			{/*edit/show group */}
			{hasAccess("ROLE_UI_GROUPS_EDIT", user) && (
				<a
					onClick={() => showGroupDetails()}
					className="more"
					title={t("USERS.GROUPS.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{/*modal displaying details about group*/}
			{displayGroupDetails && (
				<GroupDetailsModal close={hideGroupDetails} groupName={row.name} />
			)}

			{/* delete group */}
			{hasAccess("ROLE_UI_GROUPS_DELETE", user) && (
				<a
					onClick={() => setDeleteConfirmation(true)}
					className="remove"
					title={t("USERS.GROUPS.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{/*Confirmation for deleting a group*/}
			{displayDeleteConfirmation && (
				<ConfirmModal
					close={hideDeleteConfirmation}
					resourceId={row.id}
					resourceName={row.name}
					deleteMethod={deletingGroup}
					resourceType="GROUP"
				/>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

const mapDispatchToProps = (dispatch) => ({
	deleteGroup: (id) => dispatch(deleteGroup(id)),
	fetchGroupDetails: (groupName) => dispatch(fetchGroupDetails(groupName)),
});

export default connect(mapStateToProps, mapDispatchToProps)(GroupsActionsCell);
