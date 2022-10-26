import React from "react";
import { connect } from "react-redux";
import {
	fetchAccessPolicies,
	fetchHasActiveTransactions,
	saveAccessPolicies,
} from "../../../../thunks/eventDetailsThunks";
import { getPolicies } from "../../../../selectors/eventDetailsSelectors";
import ResourceDetailsAccessPolicyTab from "../../../shared/modals/ResourceDetailsAccessPolicyTab";

/**
 * This component manages the access policy tab of the event details modal
 */
const EventDetailsAccessPolicyTab = ({
	eventId,
	header,
	t,
	policies,
	fetchAccessPolicies,
	fetchHasActiveTransactions,
	saveNewAccessPolicies,
}) => {
	return (
		<ResourceDetailsAccessPolicyTab
			resourceId={eventId}
			header={header}
			buttonText={"EVENTS.EVENTS.DETAILS.ACCESS.ACCESS_POLICY.LABEL"}
			saveButtonText={"SAVE"}
			t={t}
			policies={policies}
			fetchAccessPolicies={fetchAccessPolicies}
			fetchHasActiveTransactions={fetchHasActiveTransactions}
			saveNewAccessPolicies={saveNewAccessPolicies}
			descriptionText={t("EVENTS.SERIES.NEW.ACCESS.ACCESS_POLICY.DESCRIPTION")}
			editAccessRole={"ROLE_UI_EVENTS_DETAILS_ACL_EDIT"}
		/>
	);
};

// Getting state data out of redux store
const mapStateToProps = (state) => ({
	policies: getPolicies(state),
});

// Mapping actions to dispatch
const mapDispatchToProps = (dispatch) => ({
	fetchAccessPolicies: (eventId) => dispatch(fetchAccessPolicies(eventId)),
	fetchHasActiveTransactions: (eventId) =>
		dispatch(fetchHasActiveTransactions(eventId)),
	saveNewAccessPolicies: (eventId, policies) =>
		dispatch(saveAccessPolicies(eventId, policies)),
});

export default connect(
	mapStateToProps,
	mapDispatchToProps
)(EventDetailsAccessPolicyTab);
