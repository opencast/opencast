import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import SeriesDetailsMetadataTab from "../wizards/SeriesDetailsMetadataTab";
import SeriesDetailsExtendedMetadataTab from "../wizards/SeriesDetailsExtendedMetadataTab";
import SeriesDetailsAccessTab from "../wizards/SeriesDetailsAccessTab";
import SeriesDetailsThemeTab from "../wizards/SeriesDetailsThemeTab";
import SeriesDetailsStatisticTab from "../wizards/SeriesDetailsStatisticTab";
import SeriesDetailsFeedsTab from "../wizards/SeriesDetailsFeedsTab";
import {getSeriesMetadata} from "../../../../selectors/seriesDetailsSelectors";
import {connect} from "react-redux";

const SeriesDetails = ({ seriesId, metadataFields }) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    const tabs = [
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.METADATA',
            name: 'metadata'
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.EXTENDED_METADATA',
            name: 'extended-metadata',
            hidden: true
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.PERMISSIONS',
            name: 'permissions'
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.THEME',
            name: 'theme'
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.STATISTICS',
            name: 'statistics',
            hidden: true
        },
        {
            tabNameTranslation: 'Feeds',
            name: 'feeds'
        },
    ]

    const openTab = tabNr => {
        setPage(tabNr);
    }

    return (
        <>
            {/* todo: role management */}
            <nav className="modal-nav" id="modal-nav">
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
            </nav>
            <div>
                {page === 0 && (
                    <SeriesDetailsMetadataTab metadataFields={metadataFields}/>
                )}
                {page === 1 && (
                    <SeriesDetailsExtendedMetadataTab />
                )}
                {page === 2 && (
                    <SeriesDetailsAccessTab />
                )}
                {page === 3 && (
                    <SeriesDetailsThemeTab />
                )}
                {page === 4 && (
                    <SeriesDetailsStatisticTab />
                )}
                {page === 5 && (
                    <SeriesDetailsFeedsTab />
                )}
            </div>
        </>
    );
};

const mapStateToProps = state => ({
    metadataFields: getSeriesMetadata(state)
});

export default connect(mapStateToProps)(SeriesDetails);
