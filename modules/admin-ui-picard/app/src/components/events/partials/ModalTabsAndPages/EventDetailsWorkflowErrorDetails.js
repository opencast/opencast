import {connect} from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
    getWorkflow, getWorkflowErrorDetails,
    isFetchingWorkflowErrorDetails
} from "../../../../selectors/eventDetailsSelectors";
import {fetchWorkflowErrorDetails} from "../../../../thunks/eventDetailsThunks";


/**
 * This component manages the workflow error details for the workflows tab of the event details modal
 */
const EventDetailsWorkflowErrorDetails =  ({ eventId, t, setHierarchy,
                                         errorDetails, isFetching
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

    const error_detail_style = {
        overflow: "auto",
        width: "750px"
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
                   onClick={() => openSubTab('errors-and-warnings')}
                >
                    {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.TITLE") /* Errors & Warnings */}
                    <a style={style_nav_hierarchy_inactive}> > </a>
                </a>
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy}
                   onClick={() => openSubTab('workflow-error-details')}
                >
                    {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.HEADER") /* Error Details */}
                </a>
            </nav>

            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                {/* 'Error Details' table */}
                <div className="full-col">
                    <div className="obj tbl-details">
                        <header>
                          {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.HEADER") /* Error Details */ }
                        </header>
                        <div className="obj-container">
                            <table className="main-tbl">
                                {isFetching || (
                                    <tbody>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.SEVERITY") /* Severity */ }
                                            </td>
                                            <td>{ errorDetails.severity }</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.TITLE") /* Title */ }
                                            </td>
                                            <td>{ errorDetails.title }</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.DESCRIPTION") /* Description */ }
                                            </td>
                                            <td>{ errorDetails.description }</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.JOB_ID") /* Job ID */ }
                                            </td>
                                            <td>{ errorDetails.job_id }</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.DATE") /* Date */ }
                                            </td>
                                            <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(errorDetails.timestamp)})}</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.HOST") /* Host */ }
                                            </td>
                                            <td>{ errorDetails.processing_host }</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.TYPE") /* Type */ }
                                            </td>
                                            <td>{ errorDetails.service_type }</td>
                                        </tr>
                                        <tr>
                                            <td>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DETAILS.TECHNICAL_DETAILS") /* Technical Details */ }
                                            </td>

                                            {/* list of technical error details */}
                                            <td>
                                                {errorDetails.details.map((item, key) => (
                                                    <div key={key}>
                                                        <h3>{ item.name }</h3>
                                                        <div style={error_detail_style}>
                                                            <pre>{ item.value }</pre>
                                                        </div>
                                                    </div>
                                                ))}
                                            </td>
                                        </tr>
                                    </tbody>
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
    errorDetails: getWorkflowErrorDetails(state),
    isFetching: isFetchingWorkflowErrorDetails(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    fetchErrorDetails: (eventId, workflowId, operationId) => dispatch(fetchWorkflowErrorDetails(eventId, workflowId, operationId))
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsWorkflowErrorDetails);