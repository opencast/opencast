import React from "react";
import {useTranslation} from "react-i18next";

const SeriesDetailsFeedsTab = ({ }) => {
    const { t } = useTranslation();

    return (
        // todo: show if feed links
        <div className="modal-content">
            <div className="modal-body">
                <div className="full-col">
                    <div className="obj">
                        <div className="obj-container">
                            <table className="main-tbl">
                                <tr>
                                    <th>Type</th>
                                    <th>Version</th>
                                    <th>Link</th>
                                </tr>
                                {/*todo: repeat for each feed link*/}
                                <tr>
                                    <td>row.type</td>
                                    <td>row.version</td>
                                    <td><a href="row.link">row.link</a></td>
                                </tr>
                            </table>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default SeriesDetailsFeedsTab;
