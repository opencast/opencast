import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the metadata extended table containing access rules provided by user
 * before in wizard summary pages
 */
const MetadataExtendedSummaryTable = ({ header }) => {
    const { t } = useTranslation();

    // todo: remove placeholder when metadata extended is implemented

    return (
        <div className="obj tbl-list">
            <header className="no-expand">{t(header)}</header>
            <div className="obj-container">
                <table className="main-tbl">
                    <tbody>
                    <tr>
                        <td>Placeholder Label</td>
                        <td>Placeholder Value</td>
                    </tr>
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default MetadataExtendedSummaryTable;
