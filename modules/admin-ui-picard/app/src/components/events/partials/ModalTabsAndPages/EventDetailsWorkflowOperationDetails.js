import {connect} from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
    getWorkflow, getWorkflowOperationDetails,
    getWorkflowOperations, isFetchingWorkflowOperationDetails,
    isFetchingWorkflowOperations
} from "../../../../selectors/eventDetailsSelectors";


/**
 * This component manages the workflow operation details for the workflows tab of the event details modal
 */
const EventDetailsWorkflowOperationDetails =  ({ eventId, t, setHierarchy,
                                                 operationDetails, isFetching
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

    const openSubTab = (tabType) => {
        setHierarchy(tabType);
    }

    return (
        <div className="modal-content">
            {/* Hierarchy navigation */}
            <nav className="scope" style={style_nav}>
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy_inactive}
                   onClick={() => openSubTab('workflow-details')}
                >
                    {t("EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE") /* Workflow Details */}
                    <a style={style_nav_hierarchy_inactive}> > </a>
                </a>
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy_inactive}
                   onClick={() => openSubTab('workflow-operations')}
                >
                    {t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.TITLE") /* Workflow Operations */}
                    <a style={style_nav_hierarchy_inactive}> > </a>
                </a>
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy}
                   onClick={() => openSubTab('workflow-operation-details')}
                >
                    {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TITLE") /* Operation Details */}
                </a>
            </nav>

            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                {/* 'Operation Details' table */}
                <div className="full-col">
                    <div className="obj tbl-details">
                        <header>
                            {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TITLE") /* Operation Details */}
                        </header>
                        <div className="obj-container">
                            <table className="main-tbl">
                                {isFetching || (
                                    <>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.TITLE") /* Title */}
                                            </td>
                                            <td>{operationDetails.name}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.DESCRIPTION") /* Description */ }
                                            </td>
                                            <td>{operationDetails.description}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.STATE") /* State */ }
                                            </td>
                                            <td>{t(operationDetails.state)}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.EXECUTION_HOST") /* Execution Host */ }
                                            </td>
                                            <td>{operationDetails.execution_host}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.JOB") /* Job */ }
                                            </td>
                                            <td>{operationDetails.job}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.TIME_IN_QUEUE") /* Time in Queue */}
                                            </td>
                                            <td>{operationDetails.time_in_queue}ms</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.STARTED") /* Started */ }
                                            </td>
                                            <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(operationDetails.started)})}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.FINISHED") /* Finished */ }
                                            </td>
                                            <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(operationDetails.completed)})}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.RETRY_STRATEGY") /* Retry Strategy */ }
                                            </td>
                                            <td>{operationDetails.retry_strategy}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.FAILED_ATTEMPTS") /* Failed Attempts */ }
                                            </td>
                                            <td>{operationDetails.failed_attempts}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.MAX_ATTEMPTS") /* Max */ }
                                            </td>
                                            <td>{operationDetails.max_attempts}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.EXCEPTION_HANDLER_WORKFLOW") /* Exception Handler Workflow */ }
                                            </td>
                                            <td>{operationDetails.exception_handler_workflow}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.OPERATION_DETAILS.TABLE_HEADERS.FAIL_ON_ERROR") /* Fail on Error */ }
                                            </td>
                                            <td>{operationDetails.fail_on_error}</td>
                                        </tr>
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
    isFetching: isFetchingWorkflowOperationDetails(state),
    operationDetails: getWorkflowOperationDetails(state)
});

export default connect(mapStateToProps)(EventDetailsWorkflowOperationDetails);