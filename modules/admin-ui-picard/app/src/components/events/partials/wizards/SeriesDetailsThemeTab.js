import React from "react";
import {useTranslation} from "react-i18next";
import Notifications from "../../../shared/Notifications";

const SeriesDetailsThemeTab = ({ }) => {
    const { t } = useTranslation();

    return (
        <div className="modal-content">
            <div className="modal-body">
                <Notifications context="not-corner"/>
                <div className="full-col">
                    <div className="obj quick-actions">
                        <header>{t('CONFIGURATION.NAVIGATION.THEMES')}</header>
                        <div className="obj-container padded">
                            <ul>
                                <li>
                                    <p>{t('EVENTS.SERIES.NEW.THEME.DESCRIPTION.TEXT')}</p>
                                    <p>
                                        {/*todo: preselect theme and use themes available as options and
                                        empty if there are no themes */}
                                        <select style={{width: '100%'}}
                                                placeholder={t('')}>
                                            <option value=""/>
                                        </select>
                                    </p>
                                    {/*todo: show only if selected theme has description*/}
                                    <p>selectedTheme.description</p>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SeriesDetailsThemeTab;
