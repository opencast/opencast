import React from "react";
import {useTranslation} from "react-i18next";
import {style_nav, style_nav_hierarchy, style_nav_hierarchy_inactive, error_detail_style} from "../../../../utils/workflowDetailsUtils";


/**
 * This component renders the navigation hierarchy for the workflow details sub-tabs of event details modal
 */
const EventDetailsWorkflowDetailsHierarchyNavigation = ({openSubTab, hierarchyDepth,
                                                            translationKey1 = '', subTabArgument1,
                                                            translationKey2 = '', subTabArgument2}) => {
    const { t } = useTranslation();

    /* Hierarchy navigation */
    return (
        <nav className="scope" style={style_nav}>
            <a className="breadcrumb-link scope"
               style={(hierarchyDepth === 0)? style_nav_hierarchy : style_nav_hierarchy_inactive}
               onClick={() => openSubTab('workflow-details')}
            >
                {t("EVENTS.EVENTS.DETAILS.WORKFLOW_DETAILS.TITLE") /* Workflow Details */}
                {(hierarchyDepth > 0) && (<a style={style_nav_hierarchy_inactive}> > </a>)}
            </a>
            {(hierarchyDepth > 0) && (
                <a className="breadcrumb-link scope"
                   style={(hierarchyDepth === 1)? style_nav_hierarchy : style_nav_hierarchy_inactive}
                   onClick={() => openSubTab(subTabArgument1)}
                >
                    {t(translationKey1) /* Errors & Warnings   or   Workflow Operations */}
                    {(hierarchyDepth > 1) && (<a style={style_nav_hierarchy_inactive}> > </a>)}
                </a>
            )}
            {(hierarchyDepth > 1) && (
                <a className="breadcrumb-link scope"
                   style={style_nav_hierarchy}
                   onClick={() => openSubTab(subTabArgument2)}
                >
                    {t(translationKey2) /* Error Details   or   Operation Details */}
                </a>
            )}
        </nav>
    )
}

export default EventDetailsWorkflowDetailsHierarchyNavigation;