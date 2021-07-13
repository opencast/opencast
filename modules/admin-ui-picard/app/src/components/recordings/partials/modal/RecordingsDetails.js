import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import {connect} from "react-redux";
import cn from 'classnames';
import GeneralDetailsTab from "../wizards/GeneralDetailsTab";
import ConfigurationDetailsTab from "../wizards/ConfigurationDetailsTab";
import {getRecordingDetails} from "../../../../selectors/recordingDetailsSelectors";
import CapabilitiesDetailsTab from "../wizards/CapabilitiesDetailsTab";

/**
 * This component manages the pages of the recording details
 */
const RecordingsDetails = ({ agent }) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    // information about tabs
    const tabs = [
        {
            tabNameTranslation: 'RECORDINGS.RECORDINGS.DETAILS.TAB.GENERAL',
            name: 'general'
        },
        {
            tabNameTranslation: 'RECORDINGS.RECORDINGS.DETAILS.TAB.CONFIGURATION',
            name: 'configuration'
        },
        {
            tabNameTranslation: 'RECORDINGS.RECORDINGS.DETAILS.TAB.CAPABILITIES',
            name: 'capabilities'
        }
    ];

    const openTab = tabNr => {
        setPage(tabNr);
    }

    return (
        <>
            <nav className="modal-nav" id="modal-nav">
                {/**/}
                <a className={cn({active: page === 0})}
                   onClick={() => openTab(0)}>
                    {t(tabs[0].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 1})}
                   onClick={() => openTab(1)}>
                    {t(tabs[1].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 2})}
                   onClick={() => openTab(2)}>
                    {t(tabs[2].tabNameTranslation)}
                </a>
            </nav>

            <div>
                {page === 0 && (
                    <GeneralDetailsTab agent={agent}/>
                )}
                {page === 1 && (
                    <ConfigurationDetailsTab agent={agent}/>
                )}
                {page === 2 && (
                    <CapabilitiesDetailsTab agent={agent}/>
                )}
            </div>
        </>
    );
};

// get current state out of redux store
const mapStateToProps = state => ({
    agent: getRecordingDetails(state)
});

export default connect(mapStateToProps)(RecordingsDetails);
