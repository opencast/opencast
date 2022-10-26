import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import ConfirmModal from "../../shared/ConfirmModal";
import { deleteUser } from "../../../thunks/userThunks";
import UserDetailsModal from "./modal/UserDetailsModal";
import { fetchUserDetails } from "../../../thunks/userDetailsThunks";
import { getUserInformation } from "../../../selectors/userInfoSelectors";
import { hasAccess } from "../../../utils/utils";

/**
 * This component renders the action cells of users in the table view
 */
const UsersActionCell = ({ row, deleteUser, fetchUserDetails, user }) => {
	const { t } = useTranslation();

	const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
	const [displayUserDetails, setUserDetails] = useState(false);

	const hideDeleteConfirmation = () => {
		setDeleteConfirmation(false);
	};

	const deletingUser = (id) => {
		deleteUser(id);
	};

	const showUserDetails = async () => {
		await fetchUserDetails(row.username);

		setUserDetails(true);
	};

	const hideUserDetails = () => {
		setUserDetails(false);
	};

	return (
		<>
			{/* edit/show user details */}
			{hasAccess("ROLE_UI_USERS_EDIT", user) && (
				<a
					onClick={() => showUserDetails()}
					className="more"
					title={t("USERS.USERS.TABLE.TOOLTIP.DETAILS")}
				/>
			)}

			{displayUserDetails && (
				<UserDetailsModal close={hideUserDetails} username={row.username} />
			)}

			{row.manageable && hasAccess("ROLE_UI_USERS_DELETE", user) && (
				<>
					<a
						onClick={() => setDeleteConfirmation(true)}
						className="remove"
						title={t("USERS.USERS.TABLE.TOOLTIP.DETAILS")}
					/>

					{/* Confirmation for deleting a user */}
					{displayDeleteConfirmation && (
						<ConfirmModal
							close={hideDeleteConfirmation}
							resourceName={row.name}
							resourceId={row.username}
							resourceType="USER"
							deleteMethod={deletingUser}
						/>
					)}
				</>
			)}
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	user: getUserInformation(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	deleteUser: (id) => dispatch(deleteUser(id)),
	fetchUserDetails: (username) => dispatch(fetchUserDetails(username)),
});

export default connect(mapStateToProps, mapDispatchToProps)(UsersActionCell);
