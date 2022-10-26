import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Formik } from "formik";
import cn from "classnames";
import { connect } from "react-redux";
import GroupMetadataPage from "../wizard/GroupMetadataPage";
import GroupRolesPage from "../wizard/GroupRolesPage";
import GroupUsersPage from "../wizard/GroupUsersPage";
import { EditGroupSchema } from "../../../../utils/validate";
import { getGroupDetails } from "../../../../selectors/groupDetailsSelectors";
import { updateGroupDetails } from "../../../../thunks/groupDetailsThunks";
import ModalNavigation from "../../../shared/modals/ModalNavigation";

/**
 * This component manages the pages of the group details
 */
const GroupDetails = ({ close, groupDetails, updateGroupDetails }) => {
	const { t } = useTranslation();

	const [page, setPage] = useState(0);

	// transform roles for use in SelectContainer
	let roleNames = [];
	for (let i = 0; i < groupDetails.roles.length; i++) {
		if (groupDetails.roles[i].type !== "GROUP") {
			roleNames.push({
				name: groupDetails.roles[i],
			});
		}
	}

	const initialValues = {
		...groupDetails,
		roles: roleNames,
	};

	// information about tabs
	const tabs = [
		{
			tabTranslation: "USERS.GROUPS.DETAILS.TABS.GROUP",
			accessRole: "ROLE_UI_GROUPS_EDIT",
			name: "group",
		},
		{
			tabTranslation: "USERS.GROUPS.DETAILS.TABS.ROLES",
			accessRole: "ROLE_UI_GROUPS_EDIT",
			name: "roles",
		},
		{
			tabTranslation: "USERS.GROUPS.DETAILS.TABS.USERS",
			accessRole: "ROLE_UI_GROUPS_EDIT",
			name: "users",
		},
	];

	const openTab = (tabNr) => {
		setPage(tabNr);
	};

	const handleSubmit = (values) => {
		updateGroupDetails(values, groupDetails.id);
		close();
	};

	return (
		<>
			{/* Navigation */}
			<ModalNavigation tabInformation={tabs} page={page} openTab={openTab} />

			{/* formik form used in entire modal */}
			<Formik
				initialValues={initialValues}
				validationSchema={EditGroupSchema}
				onSubmit={(values) => handleSubmit(values)}
			>
				{(formik) => (
					<>
						{page === 0 && <GroupMetadataPage formik={formik} isEdit />}
						{page === 1 && <GroupRolesPage formik={formik} isEdit />}
						{page === 2 && <GroupUsersPage formik={formik} isEdit />}

						{/* Navigation buttons and validation */}
						<footer>
							<button
								className={cn("submit", {
									active: formik.dirty && formik.isValid,
									inactive: !(formik.dirty && formik.isValid),
								})}
								disabled={!(formik.dirty && formik.isValid)}
								onClick={() => formik.handleSubmit()}
								type="submit"
							>
								{t("SUBMIT")}
							</button>
							<button className="cancel" onClick={() => close()}>
								{t("CANCEL")}
							</button>
						</footer>
					</>
				)}
			</Formik>
		</>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	groupDetails: getGroupDetails(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	updateGroupDetails: (values, groupName) =>
		dispatch(updateGroupDetails(values, groupName)),
});

export default connect(mapStateToProps, mapDispatchToProps)(GroupDetails);
