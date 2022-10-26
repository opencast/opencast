import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { connect } from "react-redux";
import ResourceDetailsAccessPolicyTab from "../../../shared/modals/ResourceDetailsAccessPolicyTab";
import { getSeriesDetailsAcl } from "../../../../selectors/seriesDetailsSelectors";
import {
	fetchSeriesDetailsAcls,
	updateSeriesAccess,
} from "../../../../thunks/seriesDetailsThunks";
import { removeNotificationWizardForm } from "../../../../actions/notificationActions";

/**
 * This component manages the access policy tab of the series details modal
 */
const SeriesDetailsAccessTab = ({
	seriesId,
	header,
	policies,
	fetchAccessPolicies,
	saveNewAccessPolicies,
}) => {
	const { t } = useTranslation();

	useEffect(() => {
		removeNotificationWizardForm();
	}, []);

	return (
		<ResourceDetailsAccessPolicyTab
			resourceId={seriesId}
			header={header}
			t={t}
			buttonText={"EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.LABEL"}
			saveButtonText={"SAVE"}
			descriptionText={t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.DESCRIPTION")}
			policies={policies}
			fetchAccessPolicies={fetchAccessPolicies}
			saveNewAccessPolicies={saveNewAccessPolicies}
			editAccessRole={"ROLE_UI_SERIES_DETAILS_ACL_EDIT"}
		/>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	policies: getSeriesDetailsAcl(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	fetchAccessPolicies: (id) => dispatch(fetchSeriesDetailsAcls(id)),
	saveNewAccessPolicies: (id, policies) =>
		dispatch(updateSeriesAccess(id, policies)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(SeriesDetailsAccessTab);
