import React, { useState } from "react";
import { useTranslation } from "react-i18next";
import { Formik } from "formik";
import cn from "classnames";
import { connect } from "react-redux";
import AclAccessPage from "../wizard/AclAccessPage";
import AclMetadataPage from "../wizard/AclMetadataPage";
import { getAclDetails } from "../../../../selectors/aclDetailsSelectors";
import { updateAclDetails } from "../../../../thunks/aclDetailsThunks";
import { NewAclSchema } from "../../../../utils/validate";
import ModalNavigation from "../../../shared/modals/ModalNavigation";
import { checkAcls } from "../../../../thunks/aclThunks";

/**
 * This component manages the pages of the acl details modal
 */
const AclDetails = ({ close, aclDetails, updateAclDetails, checkAcls }) => {
	const { t } = useTranslation();

	const [page, setPage] = useState(0);

	// set initial values
	const initialValues = {
		name: aclDetails.name,
		aclTemplate: "",
		acls: aclDetails.acl,
	};

	// information about tabs
	const tabs = [
		{
			tabTranslation: "USERS.ACLS.DETAILS.TABS.METADATA",
			accessRole: "ROLE_UI_ACLS_EDIT",
			name: "metadata",
		},
		{
			tabTranslation: "USERS.ACLS.DETAILS.TABS.ACCESS",
			accessRole: "ROLE_UI_ACLS_EDIT",
			name: "access",
		},
	];

	const openTab = (tabNr) => {
		setPage(tabNr);
	};

	const handleSubmit = (values) => {
		updateAclDetails(values, aclDetails.id);
		close();
	};

	return (
		<>
			{/* Navigation */}
			<ModalNavigation tabInformation={tabs} openTab={openTab} page={page} />

			{/* formik form used in entire modal */}
			<Formik
				initialValues={initialValues}
				validationSchema={NewAclSchema[0]}
				onSubmit={(values) => handleSubmit(values)}
			>
				{(formik) => (
					<>
						{page === 0 && <AclMetadataPage formik={formik} isEdit />}
						{page === 1 && (
							<AclAccessPage
								formik={formik}
								isEdit
								accessRole={"ROLE_UI_SERIES_DETAILS_ACL_EDIT"}
							/>
						)}

						{/* Navigation buttons and validation */}
						<footer>
							<button
								className={cn("submit", {
									active: formik.dirty && formik.isValid,
									inactive: !(formik.dirty && formik.isValid),
								})}
								disabled={!(formik.dirty && formik.isValid)}
								onClick={async () => {
									if (await checkAcls(formik.values.acls)) {
										formik.handleSubmit();
									}
								}}
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

// getting state data out of redux store
const mapStateToProps = (state) => ({
	aclDetails: getAclDetails(state),
});

// mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	updateAclDetails: (values, aclId) =>
		dispatch(updateAclDetails(values, aclId)),
	checkAcls: (acls) => dispatch(checkAcls(acls)),
});

export default connect(mapStateToProps, mapDispatchToProps)(AclDetails);
