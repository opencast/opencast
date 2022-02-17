import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import cn from 'classnames';
import {
    getSeriesDetailsFeeds,
    getSeriesDetailsMetadata,
    getSeriesDetailsTheme, getSeriesDetailsThemeNames
} from "../../../../selectors/seriesDetailsSelectors";
import {connect} from "react-redux";
import {updateSeriesMetadata} from "../../../../thunks/seriesDetailsThunks";
import SeriesDetailsExtendedMetadataTab from "../ModalTabsAndPages/SeriesDetailsExtendedMetadataTab";
import SeriesDetailsAccessTab from "../ModalTabsAndPages/SeriesDetailsAccessTab";
import SeriesDetailsThemeTab from "../ModalTabsAndPages/SeriesDetailsThemeTab";
import SeriesDetailsStatisticTab from "../ModalTabsAndPages/SeriesDetailsStatisticTab";
import SeriesDetailsFeedsTab from "../ModalTabsAndPages/SeriesDetailsFeedsTab";
import DetailsMetadataTab from "../ModalTabsAndPages/DetailsMetadataTab";
import {hasAccess} from "../../../../utils/utils";
import {getUserInformation} from "../../../../selectors/userInfoSelectors";

/**
 * This component manages the tabs of the series details modal
 */
const SeriesDetails = ({ seriesId, metadataFields, feeds, theme, themeNames, updateSeries, user }) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    // information about each tab
    const tabs = [
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.METADATA',
            accessRole: 'ROLE_UI_SERIES_DETAILS_METADATA_VIEW',
            name: 'metadata'
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.EXTENDED_METADATA',
            accessRole: 'ROLE_UI_SERIES_DETAILS_METADATA_VIEW',
            name: 'extended-metadata',
            hidden: true
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.PERMISSIONS',
            accessRole: 'ROLE_UI_SERIES_DETAILS_ACL_VIEW',
            name: 'permissions'
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.THEME',
            accessRole: 'ROLE_UI_SERIES_DETAILS_THEMES_VIEW',
            name: 'theme'
        },
        {
            tabNameTranslation: 'EVENTS.SERIES.DETAILS.TABS.STATISTICS',
            accessRole: 'ROLE_UI_SERIES_DETAILS_STATISTICS_VIEW',
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
            {/* navigation for navigating between tabs */}
            <nav className="modal-nav" id="modal-nav">
                {hasAccess(tabs[0].accessRole, user) && (
                    <a className={cn({active: page === 0})}
                       onClick={() => openTab(0)}>
                        {t(tabs[0].tabNameTranslation)}
                    </a>
                )}
                {(!tabs[1].hidden && hasAccess(tabs[1].accessRole, user)) &&
                    <a className={cn({active: page === 1})}
                       onClick={() => openTab(1)}>
                        {t(tabs[1].tabNameTranslation)}
                    </a>
                }
                {hasAccess(tabs[2].accessRole, user) && (
                    <a className={cn({active: page === 2})}
                       onClick={() => openTab(2)}>
                        {t(tabs[2].tabNameTranslation)}
                    </a>
                )}
                {hasAccess(tabs[3].accessRole, user) && (
                    <a className={cn({active: page === 3})}
                       onClick={() => openTab(3)}>
                        {t(tabs[3].tabNameTranslation)}
                    </a>
                )}
                {(!tabs[4].hidden && hasAccess(tabs[4].accessRole, user)) &&
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
                    <DetailsMetadataTab metadataFields={metadataFields}
                                        resourceId={seriesId}
                                        header={tabs[page].tabNameTranslation}
                                        buttonLabel='EVENTS.SERIES.DETAILS.METADATA.REPLACE_SERIES_METADATA'
                                        updateResource={updateSeries}
                                        editAccessRole='ROLE_UI_SERIES_DETAILS_METADATA_EDIT'/>
                )}
                {page === 1 && (
                    <SeriesDetailsExtendedMetadataTab />
                )}
                {page === 2 && (
                    <SeriesDetailsAccessTab seriesId={seriesId}
                                            header={tabs[page].tabNameTranslation}/>
                )}
                {page === 3 && (
                    <SeriesDetailsThemeTab theme={theme}
                                           themeNames={themeNames}
                                           seriesId={seriesId} />
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
    themeNames: getSeriesDetailsThemeNames(state),
    user: getUserInformation(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    updateSeries: (id, values) => dispatch(updateSeriesMetadata(id, values))
});

export default connect(mapStateToProps, mapDispatchToProps)(SeriesDetails);
