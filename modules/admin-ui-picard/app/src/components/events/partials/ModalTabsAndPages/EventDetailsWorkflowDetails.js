import React from "react";
import {connect} from "react-redux";
import {

} from "../../../../thunks/eventDetailsThunks";
import {} from "../../../../selectors/eventDetailsSelectors";
import Notifications from "../../../shared/Notifications";

/**
 * This component manages the workflow details for the workflows tab of the event details modal
 */
const EventDetailsWorkflowDetails =  ({ eventId, header, t,

                                       }) => {
    const subNavData = {
        creator: {
            name: "Some Name",
            email: "e@ma.il"
        },
        title: "Workflow Title",
        description: "Example description Text...",
        submittedAt: "2021-07-26 12:00:00",
        status: "ongoing",
        executionTime: "2h",
        wiid: 123,
        wdid: 456,
        configuration: [
            {
                key: "A",
                value: "Config A"
            },
            {
                key: "B",
                value: "Config B"
            }
        ],
        id: 42
    };

    const humanDuration = "5h"

    const openSubTab = (tabType, resourceType, id, someBool) => {
        console.log(`Open Sub Tab ${tabType}, res: ${resourceType}, id: ${id}, ${someBool}`);
    }

    return (
        <div className="modal-content"> {/* data-modal-tab-content="workflow-details" data-parent="workflows" data-level="2" data-label="EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE"*/}
            <div className="modal-body">
                <div className="full-col">
                    {/* Notifications */}
                    <Notifications context="not_corner"/>

                    <div className="obj tbl-details">
                        <header>
                            {t("EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE") /* Workflow Details */ }
                        </header>
                        <div className="obj-container">
                            <table className="main-tbl vertical-headers">
                                <tr>
                                    <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.TITLE") /* Title */ }</td>
                                    <td>{subNavData.title}</td>
                                </tr>
                                {subNavData.description && (
                                    <tr>
                                        <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.DESCRIPTION") /* Description */ }</td>
                                        <td>{subNavData.description}</td>
                                    </tr>
                                )}
                                <tr>
                                    <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.SUBMITTER") /* Submitter*/ }</td>
                                    <td>
                                        {subNavData.creator.name}
                                        {subNavData.creator.email && (
                                            <span>{'<' + subNavData.creator.email + '>'}</span>
                                        )}
                                    </td>
                                </tr>
                                <tr>
                                    <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.SUBMITTED") /* Submitted */ }</td>
                                    <td>{t('dateFormats.dateTime.medium', {dateTime: new Date(subNavData.submittedAt)})}</td>
                                </tr>
                                <tr>
                                    <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.STATUS") /* Status */ }</td>
                                    <td>{t(subNavData.status)}</td>
                                </tr>
                                { (subNavData.status !== 'EVENTS.EVENTS.DETAILS.WORKFLOWS.OPERATION_STATUS.RUNNING') && (
                                    <tr>
                                        <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.EXECUTION_TIME") /* Execution time */ }</td>
                                        <td>{subNavData.executionTime || humanDuration}</td>
                                    </tr>
                                )}
                                <tr with-role="ROLE_ADMIN">
                                    <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.ID") /* ID */ }</td>
                                    <td>{subNavData.wiid}</td>
                                </tr>
                                <tr with-role="ROLE_ADMIN">
                                    <td>{t("EVENTS.EVENTS.DETAILS.WORKFLOWS.WDID") /* Workflow definition */ }</td>
                                    <td>{subNavData.wdid}</td>
                                </tr>
                            </table>
                        </div>
                    </div>

                    <div className="obj tbl-details"> {/* with-role="ROLE_ADMIN" */}
                        <header>
                            {t("EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.CONFIGURATION") /* Workflow configuration */ }
                        </header>
                        <div className="obj-container">
                            <table className="main-tbl">
                                { subNavData.configuration.map((conf, key) => (
                                    <tr key={key}>
                                        <td>{conf.key}</td>
                                        <td>{conf.value}</td>
                                    </tr>
                                )) }
                            </table>
                        </div>
                    </div>

                    <div className="obj tbl-container more-info-actions">
                        <header>
                            {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.MORE_INFO") /* More Information */ }
                        </header>
                        <div className="obj-container">
                            <ul>
                                <li>
                <span>
                  {t("EVENTS.EVENTS.DETAILS.WORKFLOW_OPERATIONS.DETAILS_LINK") /* Operations */ }
                </span>
                                    <a className="details-link"
                                       onClick={() => openSubTab('workflow-operations', 'EventWorkflowOperationsResource', subNavData.id, true)}
                                    >
                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.DETAILS") /* Details */ }
                                    </a>
                                </li>
                                <li>
                <span>
                  {t("EVENTS.EVENTS.DETAILS.ERRORS_AND_WARNINGS.TITLE") /* Errors & Warnings */ }
                </span>
                                    <a className="details-link"
                                       onClick={() => openSubTab('errors-and-warnings', 'EventErrorsResource', subNavData.id, true)}
                                    >
                                        {t("EVENTS.EVENTS.DETAILS.WORKFLOWS.DETAILS") /* Details */ }
                                    </a>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({

});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({

});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetailsWorkflowDetails);