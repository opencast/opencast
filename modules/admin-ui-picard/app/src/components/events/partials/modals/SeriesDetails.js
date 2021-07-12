import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import SeriesDetailsMetadataTab from "../wizards/SeriesDetailsMetadataTab";
import SeriesDetailsExtendedMetadataTab from "../wizards/SeriesDetailsExtendedMetadataTab";
import SeriesDetailsAccessTab from "../wizards/SeriesDetailsAccessTab";
import SeriesDetailsThemeTab from "../wizards/SeriesDetailsThemeTab";
import SeriesDetailsStatisticTab from "../wizards/SeriesDetailsStatisticTab";
import SeriesDetailsFeedsTab from "../wizards/SeriesDetailsFeedsTab";
import {
    getSeriesDetailsFeeds,
    getSeriesDetailsMetadata,
    getSeriesDetailsTheme, getSeriesDetailsThemeNames
} from "../../../../selectors/seriesDetailsSelectors";
import {connect} from "react-redux";

const SeriesDetails = ({ seriesId, metadataFields, feeds, theme, themeNames }) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    // information about each tab
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
            {/* navigation for navigating between tabs */}
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
                {feeds.length > 0 && (
                    <a className={cn({active: page === 5})}
                       onClick={() => openTab(5)}>
                        {t(tabs[5].tabNameTranslation)}
                    </a>
                )}
            </nav>

            {/* render modal content depending on current page */}
            <div>
                {page === 0 && (
                    <SeriesDetailsMetadataTab metadataFields={metadataFields} seriesId={seriesId}/>
                )}
                {page === 1 && (
                    <SeriesDetailsExtendedMetadataTab />
                )}
                {page === 2 && (
                    <SeriesDetailsAccessTab />
                )}
                {page === 3 && (
                    <SeriesDetailsThemeTab theme={theme} themeNames={themeNames} seriesId={seriesId} />
                )}
                {page === 4 && (
                    <SeriesDetailsStatisticTab />
                )}
                {page === 5 && (
                    <SeriesDetailsFeedsTab feeds={feeds}/>
                )}
            </div>
        </>
    );
};

const mapStateToProps = state => ({
    metadataFields: getSeriesDetailsMetadata(state),
    feeds: getSeriesDetailsFeeds(state),
    theme: getSeriesDetailsTheme(state),
    themeNames: getSeriesDetailsThemeNames(state)
});

export default connect(mapStateToProps)(SeriesDetails);
