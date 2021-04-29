import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import {connect} from "react-redux";
import {getCurrentLanguageInformation} from "../../../../utils/utils";
import EventDetailsCommentsTab from "./EventDetailsCommentsTab";
import EventDetailsAccessPolicyTab from "./EventDetailsAccessPolicyTab";


// Get info about the current language and its date locale
const currentLanguage = getCurrentLanguageInformation();

/**
 * This component manages the pages of the event details
 */
const EventDetails = ({ tabIndex, eventId }) => {
    const { t } = useTranslation();


    const [page, setPage] = useState(tabIndex);

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

            {/* Initialize overall form */}
                        <div>
                            {page === 0 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 1 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 2 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 3  && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 4 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
                            )}
                            {page === 5 && (
                                <MockDataPage header={tabs[page].bodyHeaderTranslation}
                                                 t={t}/>
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
                        {/* Table view containing input fields for metadata */}
                        <div className="obj-container">
                            <table class="main-tbl">
                                <tr>
                                    <td>
                                        <span>Content coming soon!</span>
                                    </td>
                                </tr>
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
});



export default connect(mapStateToProps)(EventDetails);
