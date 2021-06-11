import React from "react";
import {useTranslation} from "react-i18next";
import Notifications from "../../../shared/Notifications";

const SeriesDetailsMetadataTab = ({ }) => {
    const { t } = useTranslation();

    return (
        <div className="modal-content">
            <div className="modal-body">
                <Notifications context="not-corner"/>
                <div className="full-col">
                    <div className="obj tbl-list">
                        <header className="no-expand">
                            {t('EVENTS.SERIES.DETAILS.TABS.METADATA')}
                        </header>
                        <div className="obj-container">
                            <table className="main-tbl">
                                {/* todo: repeat for each metadata entry*/}
                                <tr>
                                    <td>
                                        <span>row.label</span>
                                        <i className="required">*</i>
                                    </td>
                                    {/* todo: role: ROLE_UI_SERIES_DETAILS_METADATA_EDIT */}
                                    {/* todo: editable */}
                                    <td>
                                        row.value
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

export default SeriesDetailsMetadataTab;
