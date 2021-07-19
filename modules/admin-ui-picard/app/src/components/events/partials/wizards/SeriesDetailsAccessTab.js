import React from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import ResourceDetailsAccessPolicyTab from "../../../shared/modals/ResourceDetailsAccessPolicyTab";
import {getSeriesDetailsAcl} from "../../../../selectors/seriesDetailsSelectors";
import {fetchSeriesDetailsAcls, updateSeriesAccess} from "../../../../thunks/seriesDetailsThunks";

/**
 * This component manages the access policy tab of the series details modal
 */
const SeriesDetailsAccessTab = ({ seriesId, header, policies, fetchAccessPolicies, fetchHasActiveTransactions,
                                    saveNewAccessPolicies }) => {
    const { t } = useTranslation();

    return (
        <ResourceDetailsAccessPolicyTab resourceId={seriesId}
                                        header={header}
                                        t={t}
                                        buttonText={'EVENTS.SERIES.DETAILS.ACCESS.ACCESS_POLICY.LABEL'}
                                        policies={policies}
                                        fetchAccessPolicies={fetchAccessPolicies}
                                        saveNewAccessPolicies={saveNewAccessPolicies} />
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    policies: getSeriesDetailsAcl(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    fetchAccessPolicies: id => dispatch(fetchSeriesDetailsAcls(id)),
    saveNewAccessPolicies: (id, policies) => dispatch(updateSeriesAccess(id, policies))

});

export default connect(mapStateToProps, mapDispatchToProps)(SeriesDetailsAccessTab);
