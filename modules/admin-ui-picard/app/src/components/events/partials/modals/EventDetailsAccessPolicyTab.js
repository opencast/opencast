import React, {useState, useEffect} from "react";
import {connect} from "react-redux";
import {
    saveAccessPolicies
} from "../../../../thunks/eventDetailsThunks";
import {
    getPolicies,
} from "../../../../selectors/eventDetailsSelectors";
import {
    fetchAccessPolicies,
    fetchHasActiveTransactions
} from "../../../../thunks/eventDetailsThunks";
import ResourceDetailsAccessPolicyTab from "../../../shared/modals/ResourceDetailsAccessPolicyTab";

/**
 * This component manages the access policy tab of the event details modal
 */
const EventDetailsAccessPolicyTab = ({ eventId, header, t,
                                       policies,
                                       fetchAccessPolicies, fetchHasActiveTransactions, saveNewAccessPolicies}) => {
    return (
        <ResourceDetailsAccessPolicyTab resourceId={eventId}
                                        header={header}
                                        t={t}
                                        policies={policies}
                                        fetchAccessPolicies={fetchAccessPolicies}
                                        fetchHasActiveTransactions={fetchHasActiveTransactions}
                                        saveNewAccessPolicies={saveNewAccessPolicies} />
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    policies: getPolicies(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    fetchAccessPolicies: (eventId, test) => dispatch(fetchAccessPolicies(eventId)),
    fetchHasActiveTransactions: (eventId) => dispatch(fetchHasActiveTransactions(eventId)),
    saveNewAccessPolicies: (eventId, policies) => dispatch(saveAccessPolicies(eventId, policies)),
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsAccessPolicyTab);