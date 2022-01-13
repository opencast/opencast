import {connect} from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
    getWorkflow,
    getWorkflowOperations,
    isFetchingWorkflowOperations
} from "../../../../selectors/eventDetailsSelectors";
import {fetchWorkflowOperationDetails} from "../../../../thunks/eventDetailsThunks";


/**
 * This component manages the workflow operations for the workflows tab of the event details modal
 */
const EventDetailsWorkflowOperations =  ({ eventId, t, setHierarchy,
                                           workflowId, operations, isFetching,
                                           fetchOperationDetails
                                      }) => {

    const style_nav = {
        borderBottom: "1px solid #d6d6d6",
        lineHeight: "35px",
    }

    const style_nav_hierarchy_inactive = {
        marginLeft: "30px",
        color: "#92a0ab"
    }

    const style_nav_hierarchy = {
        marginLeft: "30px",
        marginRight: "30px",
        fontWeight: "600",
        color: "#5d7589"
    }

    const openSubTab = (tabType, operationId) => {
        setHierarchy(tabType);
        if(tabType === "workflow-operation-details"){
            fetchOperationDetails(eventId, workflowId, operationId).then(r => {});
        }
    }

    return (
        <div className="modal-content">
            {/* Hierarchy navigation */}
            <nav className="scope" style={style_nav}>
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy_inactive}
                   onClick={() => openSubTab('workflow-details', null)}
                >
                    {t("EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE") /* Workflow Details */}
                    <a style={style_nav_hierarchy_inactive}> > </a>
                </a>
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy}
                   onClick={() => openSubTab('workflow-operations', null)}
                >
                    {t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TITLE") /* Workflow Operations */}
                </a>
            </nav>

            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                {/* 'Workflow Operations' table */}
                <div className="full-col">
                    <div className="obj tbl-container">
                        <header>
                            {t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TITLE") /* Workflow Operations */}
                        </header>
                        <div className="obj-container">
                            <table className="main-tbl">
                                {isFetching || (
                                    <>
                                        <thead>
                                            <tr>
                                                <th>{t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TABLE_HEADERS.STATUS") /* Status */}</th>
                                                <th>{t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TABLE_HEADERS.TITLE") /* Title */}
                                                    <i></i>
                                                </th>
                                                <th>{t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TABLE_HEADERS.DESCRIPTION") /* Description */}
                                                    <i></i>
                                                </th>
                                                <th className="medium"></th>
                                            </tr>
                                        </thead>
                                        <tbody>

                                            {/* workflow operation details */}
                                            {operations.entries.map((item, key) => (
                                                <tr key={key}>
                                                    <td>{t(item.status)}</td>
                                                    <td>{item.title}</td>
                                                    <td>{item.description}</td>

                                                    {/* link to 'Operation Details'  sub-Tab */}
                                                    <td>
                                                        <a className="details-link"
                                                           onClick={() => openSubTab('workflow-operation-details', key)}
                                                        >
                                                            {t("EVENTS.EVENTS.DETAILS.MEDIA.DETAILS") /* Details */}
                                                        </a>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </>
                                )}
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    workflowId: getWorkflow(state).wiid,
    operations: getWorkflowOperations(state),
    isFetching: isFetchingWorkflowOperations(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    fetchOperationDetails: (eventId, workflowId, operationId) => dispatch(fetchWorkflowOperationDetails(eventId, workflowId, operationId))
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsWorkflowOperations);
