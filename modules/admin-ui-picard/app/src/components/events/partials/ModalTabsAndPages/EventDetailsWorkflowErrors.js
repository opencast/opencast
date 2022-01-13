import {connect} from "react-redux";
import React from "react";
import Notifications from "../../../shared/Notifications";
import {
    getWorkflow,
    getWorkflowErrors,
    isFetchingWorkflowErrors
} from "../../../../selectors/eventDetailsSelectors";
import {fetchWorkflowErrorDetails} from "../../../../thunks/eventDetailsThunks";


/**
 * This component manages the workflow errors for the workflows tab of the event details modal
 */
const EventDetailsWorkflowErrors =  ({ eventId, t, setHierarchy,
                                       workflowId, errors, isFetching,
                                       fetchErrorDetails
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

    const severityColor = (severity) => {
      switch (severity.toUpperCase()) {
          case 'FAILURE':
            return 'red';
          case 'INFO':
            return 'green';
          case 'WARNING':
            return 'yellow';
          }
    };

    const openSubTab = (tabType, errorId) => {
        setHierarchy(tabType);
        if(tabType === "workflow-error-details"){
            fetchErrorDetails(eventId, workflowId, errorId).then(r => {});
        }
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
                   style={style_nav_hierarchy}
                   onClick={() => openSubTab('errors-and-warnings')}
                >
                    {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.TITLE") /* Errors & Warnings */}
                </a>
            </nav>

            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                {/* 'Errors & Warnings' table */}
                <div className="full-col">
                    <div className="obj tbl-container">
                        <header>
                            {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.HEADER") /* Errors & Warnings */ }
                        </header>

                        <div className="obj-container">
                            <table className="main-tbl">
                                {isFetching || (
                                    <>
                                        <thead>
                                        <tr>
                                            <th className="small"/>
                                            <th>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.DATE") /* Date */ }
                                                <i></i>
                                            </th>
                                            <th>
                                                {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.TITLE") /* Errors & Warnings */}
                                                <i></i>
                                            </th>
                                            <th className="medium"/>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        { /* error details */
                                            errors.entries.map((item, key) => (
                                                <tr key={key}>
                                                    <td>
                                                        {
                                                            !!item.severity && (
                                                                <div className={`circle ${ severityColor(item.severity) }`}/>
                                                            )
                                                        }

                                                    </td>
                                                    <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(item.timestamp)})}</td>
                                                    <td>{item.title}</td>

                                                    {/* link to 'Error Details'  sub-Tab */}
                                                    <td>
                                                        <a className="details-link"
                                                           onClick={() => openSubTab('workflow-error-details', item.id)}
                                                        >
                                                            {t("EVENTS.EVENTS.DETAILS.MEDIA.DETAILS") /*  Details */}
                                                        </a>
                                                    </td>
                                                </tr>
                                            ))
                                        }
                                        { /* No errors message */
                                            errors.entries.length == 0 && (
                                                <tr>
                                                    <td colSpan="4">
                                                        {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.EMPTY") /* No errors found. */}
                                                    </td>
                                                </tr>
                                            )
                                        }
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
    errors: getWorkflowErrors(state),
    isFetching: isFetchingWorkflowErrors(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    fetchErrorDetails: (eventId, workflowId, operationId) => dispatch(fetchWorkflowErrorDetails(eventId, workflowId, operationId))
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsWorkflowErrors);
