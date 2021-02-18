import React from "react";
import {useTranslation} from "react-i18next";

/**
 * This component renders the metadata table containing access rules provided by user before in wizard summary pages
 */
const MetadataSummaryTable = ({ metadataFields, formikValues, header }) => {
    const { t } = useTranslation();

    // metadata that user has provided
    let metadata = [];
    for (let i = 0; metadataFields.length > i; i++) {
        let fieldValue = formikValues[metadataFields[i].id];
        if (!!fieldValue && fieldValue.length > 0) {
            metadata = metadata.concat({
                name: metadataFields[i].id,
                label: metadataFields[i].label,
                value: fieldValue
            });
        }
    }

    return (
        <div className="obj tbl-list">
            <header className="no-expand">{t(header)}</header>
            <div className="obj-container">
                <table className="main-tbl">
                    <tbody>
                    {/*Insert row for each metadata entry user has provided*/}
                    {metadata.map((entry, key) => (
                        <tr key={key}>
                            <td>{t(entry.label)}</td>
                            <td>{Array.isArray(entry.value) ?
                                entry.value.join(', ')
                                : entry.value}</td>
                        </tr>
                    ))}
                    </tbody>
                </table>
            </div>
        </div>
    )
}

export default MetadataSummaryTable;
