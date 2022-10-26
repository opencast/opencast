import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import { deleteAcl } from "../../../thunks/aclThunks";
import AclDetailsModal from "./modal/AclDetailsModal";
import { fetchAclDetails } from "../../../thunks/aclDetailsThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the action cells of acls in the table view
 */
const AclsActionsCell = ({ row, deleteAcl, fetchAclDetails, user }) => {
	const { t } = useTranslation();

	const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
	const [displayAclDetails, setAclDetails] = useState(false);

	const hideDeleteConfirmation = () => {
		setDeleteConfirmation(false);
	};

	const deletingAcl = (id) => {
		deleteAcl(id);
	};

	const hideAclDetails = () => {
		setAclDetails(false);
	};

	const showAclDetails = async () => {
		await fetchAclDetails(row.id);

		setAclDetails(true);
	};

	return (
		<>
			{/* edit/show ACL details */}
			{hasAccess("ROLE_UI_ACLS_EDIT", user) && (
				<a
					onClick={() => showAclDetails()}
					className="more"
					title={t("USERS.ACLS.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{displayAclDetails && (
				<AclDetailsModal close={hideAclDetails} aclName={row.name} />
			)}

			{/* delete ACL */}
			{hasAccess("ROLE_UI_ACLS_DELETE", user) && (
				<a
					onClick={() => setDeleteConfirmation(true)}
					className="remove"
					title={t("USERS.ACLS.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{/* Confirmation for deleting an ACL */}
			{displayDeleteConfirmation && (
				<ConfirmModal
					close={hideDeleteConfirmation}
					resourceName={row.name}
					resourceId={row.id}
					resourceType="ACL"
					deleteMethod={deletingAcl}
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
	deleteAcl: (id) => dispatch(deleteAcl(id)),
	fetchAclDetails: (aclId) => dispatch(fetchAclDetails(aclId)),
});

export default connect(mapStateToProps, mapDispatchToProps)(AclsActionsCell);
