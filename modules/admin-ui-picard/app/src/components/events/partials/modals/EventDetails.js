import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import {connect} from "react-redux";
import {getCurrentLanguageInformation} from "../../../../utils/utils";
import EventDetailsCommentsTab from "../ModalTabsAndPages/EventDetailsCommentsTab";
import EventDetailsAccessPolicyTab from "../ModalTabsAndPages/EventDetailsAccessPolicyTab";
import EventDetailsWorkflowTab from "../ModalTabsAndPages/EventDetailsWorkflowTab";
import EventDetailsWorkflowDetails from "../ModalTabsAndPages/EventDetailsWorkflowDetails";
import EventDetailsPublicationTab from "../ModalTabsAndPages/EventDetailsPublicationTab";
import EventDetailsWorkflowOperations from "../ModalTabsAndPages/EventDetailsWorkflowOperations";
import EventDetailsWorkflowOperationDetails from "../ModalTabsAndPages/EventDetailsWorkflowOperationDetails";
import EventDetailsWorkflowErrors from "../ModalTabsAndPages/EventDetailsWorkflowErrors";
import EventDetailsWorkflowErrorDetails from "../ModalTabsAndPages/EventDetailsWorkflowErrorDetails";
import {getMetadata, isFetchingMetadata} from "../../../../selectors/eventDetailsSelectors";
import {fetchMetadata, updateMetadata} from "../../../../thunks/eventDetailsThunks";
import DetailsMetadataTab from "../ModalTabsAndPages/DetailsMetadataTab";
import {removeNotificationWizardForm} from "../../../../actions/notificationActions";
import EventDetailsAssetsTab from "../ModalTabsAndPages/EventDetailsAssetsTab";
import EventDetailsAssetAttachments from "../ModalTabsAndPages/EventDetailsAssetAttachments";
import EventDetailsAssetCatalogs from "../ModalTabsAndPages/EventDetailsAssetCatalogs";
import EventDetailsAssetMedia from "../ModalTabsAndPages/EventDetailsAssetMedia";
import EventDetailsAssetPublications from "../ModalTabsAndPages/EventDetailsAssetPublications";
import EventDetailsAssetAttachmentDetails from "../ModalTabsAndPages/EventDetailsAssetAttachmentDetails";
import EventDetailsAssetCatalogDetails from "../ModalTabsAndPages/EventDetailsAssetCatalogDetails";
import EventDetailsAssetMediaDetails from "../ModalTabsAndPages/EventDetailsAssetMediaDetails";
import EventDetailsAssetPublicationDetails from "../ModalTabsAndPages/EventDetailsAssetPublicationDetails";
import EventDetailsAssetsAddAsset from "../ModalTabsAndPages/EventDetailsAssetsAddAsset";


// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manages the pages of the event details
 */
const EventDetails = ({ tabIndex, eventId, close,
                          metadata, isLoadingMetadata,
                          loadMetadata, updateMetadata, removeNotificationWizardForm }) => {
    const { t } = useTranslation();

    useEffect(() => {
        removeNotificationWizardForm();
        loadMetadata(eventId).then(r => {});

    }, []);

    const [page, setPage] = useState(tabIndex);
    const [workflowTabHierarchy, setWorkflowTabHierarchy] = useState("entry")
    const [assetsTabHierarchy, setAssetsTabHierarchy] = useState("entry")

    const tabs = [
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.METADATA',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTION',
            name: 'metadata'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.EXTENDED-METADATA',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTION', //todo: here {{ catalog.title | translate }}
            name: 'metadata-extended',
            hidden: true
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.PUBLICATIONS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.PUBLICATIONS.CAPTION',
            name: 'publications'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.ASSETS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.ASSETS.CAPTION',
            name: 'assets',
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.SCHEDULING',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.SCHEDULING.CAPTION',
            name: 'scheduling',
            hidden: true
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.WORKFLOWS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.WORKFLOW_INSTANCES.TITLE', // todo: not quite right, has 2 top-level captions
            name: 'workflows'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.ACCESS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.TABS.ACCESS',
            name: 'access'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.COMMENTS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.COMMENTS.CAPTION',
            name: 'comments'
        },
        {
            tabNameTranslation: 'EVENTS.EVENTS.DETAILS.TABS.STATISTICS',
            bodyHeaderTranslation: 'EVENTS.EVENTS.DETAILS.METADATA.CAPTION',
            name: 'statistics',
            hidden: true
        }
    ];

    const openTab = (tabNr) => {
        removeNotificationWizardForm();
        setWorkflowTabHierarchy("entry")
        setAssetsTabHierarchy("entry")
        setPage(tabNr);
    }

    return (
        <>
            <nav className='modal-nav' id='modal-nav'>
                <a className={cn({active: page === 0})}
                      onClick={() => openTab(0)}>
                    {t(tabs[0].tabNameTranslation)}
                </a>
                {
                    tabs[1].hidden ?
                        null :
                        <a className={cn({active: page === 1})}
                              onClick={() => openTab(1)}>
                            {t(tabs[1].tabNameTranslation)}
                        </a>
                }
                <a className={cn({active: page === 2})}
                      onClick={() => openTab(2)}>
                    {t(tabs[2].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 3})}
                      onClick={() => openTab(3)}>
                    {t(tabs[3].tabNameTranslation)}
                </a>
                {
                    tabs[4].hidden ?
                        null :
                        <a className={cn({active: page === 4})}
                              onClick={() => openTab(4)}>
                            {t(tabs[4].tabNameTranslation)}
                </a>
                }
                <a className={cn({active: page === 5})}
                      onClick={() => openTab(5)}>
                    {t(tabs[5].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 6})}
                      onClick={() => openTab(6)}>
                    {t(tabs[6].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 7})}
                      onClick={() => openTab(7)}>
                    {t(tabs[7].tabNameTranslation)}
                </a>
                {
                    tabs[8].hidden ?
                        null :
                        <a className={cn({active: page === 8})}
                              onClick={() => openTab(8)}>
                            {t(tabs[8].tabNameTranslation)}
                        </a>
                }
            </nav>
            {/* Initialize overall modal */}
                <div>
                    {page === 0 && (!isLoadingMetadata) && (
                        <DetailsMetadataTab metadataFields={metadata}
                            resourceId={eventId}
                            header={tabs[page].bodyHeaderTranslation}
                            buttonLabel='SAVE'
                            updateResource={updateMetadata}/>
                    )}
                    {page === 1 && (
                        <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                         t={t}/>
                    )}
                    {page === 2 && (
                        <EventDetailsPublicationTab eventId={eventId} />
                    )}
                    {page === 3  && (
                        (assetsTabHierarchy === "entry" && (
                            <EventDetailsAssetsTab
                                 eventId={eventId}
                                 t={t}
                                 setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "add-asset" && (
                            <EventDetailsAssetsAddAsset
                                 eventId={eventId}
                                 t={t}
                                 setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "asset-attachments" && (
                            <EventDetailsAssetAttachments
                                 eventId={eventId}
                                 t={t}
                                 setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "attachment-details" && (
                            <EventDetailsAssetAttachmentDetails
                                 eventId={eventId}
                                 t={t}
                                 setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "asset-catalogs" && (
                            <EventDetailsAssetCatalogs
                                eventId={eventId}
                                t={t}
                                setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "catalog-details" && (
                            <EventDetailsAssetCatalogDetails
                                eventId={eventId}
                                t={t}
                                setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "asset-media" && (
                            <EventDetailsAssetMedia
                                eventId={eventId}
                                t={t}
                                setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "media-details" && (
                            <EventDetailsAssetMediaDetails
                                eventId={eventId}
                                t={t}
                                setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "asset-publications" && (
                            <EventDetailsAssetPublications
                                eventId={eventId}
                                t={t}
                                setHierarchy={setAssetsTabHierarchy}/>
                        )) || (
                        assetsTabHierarchy === "publication-details" && (
                            <EventDetailsAssetPublicationDetails
                                eventId={eventId}
                                t={t}
                                setHierarchy={setAssetsTabHierarchy}/>
                        ))
                    )}
                    {page === 4 && (
                        <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                         t={t}/>
                    )}
                    {page === 5 && (
                        (workflowTabHierarchy === "entry" && (
                            <EventDetailsWorkflowTab
                                eventId={eventId}
                                header={tabs[page].bodyHeaderTranslation}
                                t={t}
                                close={close}
                                setHierarchy={setWorkflowTabHierarchy}/>
                        )) || (
                        workflowTabHierarchy === "workflow-details" && (
                            <EventDetailsWorkflowDetails
                                eventId={eventId}
                                t={t}
                                setHierarchy={setWorkflowTabHierarchy}/>
                        )) || (
                        workflowTabHierarchy === "workflow-operations" && (
                            <EventDetailsWorkflowOperations
                                eventId={eventId}
                                t={t}
                                setHierarchy={setWorkflowTabHierarchy}/>
                        )) || (
                        workflowTabHierarchy === "workflow-operation-details" && (
                            <EventDetailsWorkflowOperationDetails
                                eventId={eventId}
                                t={t}
                                setHierarchy={setWorkflowTabHierarchy}/>
                        )) || (
                        workflowTabHierarchy === "errors-and-warnings" && (
                            <EventDetailsWorkflowErrors
                                eventId={eventId}
                                t={t}
                                setHierarchy={setWorkflowTabHierarchy}/>
                        )) || (
                        workflowTabHierarchy === "workflow-error-details" && (
                            <EventDetailsWorkflowErrorDetails
                                eventId={eventId}
                                t={t}
                                setHierarchy={setWorkflowTabHierarchy}/>
                        ))
                    )}
                    {page === 6 && (
                        <EventDetailsAccessPolicyTab
                            eventId={eventId}
                            header={tabs[page].bodyHeaderTranslation}
                            t={t}/>
                    )}
                    {page === 7 && (
                        <EventDetailsCommentsTab
                            eventId={eventId}
                            header={tabs[page].bodyHeaderTranslation}
                            t={t}/>
                    )}
                    {page === 8 && (
                        <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                         t={t}/>
                    )}
                </div>
        </>

    );
};

const MockDataPage = ({ header, t }) => {

    return (
        <div className="modal-content">
            <div className="modal-body">
                <div className="full-col">
                    <div className="obj tbl-details">
                        <header>{t(header)}</header>
                        <div className="obj-container">
                            <table className="main-tbl">
                                <tbody>
                                    <tr>
                                        <td>
                                            <span>Content coming soon!</span>
                                        </td>
                                    </tr>
                                </tbody>
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
    metadata: getMetadata(state),
    isLoadingMetadata: isFetchingMetadata(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    loadMetadata: (id) => dispatch(fetchMetadata(id)),
    updateMetadata: (id, values) => dispatch(updateMetadata(id, values)),
    removeNotificationWizardForm: () => dispatch(removeNotificationWizardForm())
});

export default connect(mapStateToProps, mapDispatchToProps)(EventDetails);
