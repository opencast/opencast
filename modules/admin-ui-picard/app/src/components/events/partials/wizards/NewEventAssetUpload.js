import React from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {uploadAssetOptions} from "../../../../configs/newEventConfigs/sourceConfig";

const NewEventAssetUpload = ({ onSubmit, previousPage, nextPage , formik }) => {
    const { t } = useTranslation();

    // Get upload assets that are not of type track
    const uploadAssets = uploadAssetOptions.filter(asset => asset.type !== 'track');

    if (formik.values.sourceMode !== 'UPLOAD') {
        nextPage(formik.values);
        return null;
    }
    return (
        <>
            <div className="modal-content">
                <div className="modal-body">
                    <div className="full-col">
                        <div className="obj tbl-details">
                            <header>{t('EVENTS.EVENTS.NEW.UPLOAD_ASSET.SELECT_TYPE')}</header>
                            <div className="obj-container">
                                <table className="main-tbl">
                                    {/*Todo: option if theres no further assets, only if source mode UPLOAD*/}
                                    {uploadAssets.length === 0 ? (
                                        <tr>
                                            <td>{t('EVENTS.EVENTS.NEW.UPLOAD_ASSET.NO_OPTIONS')}</td>
                                        </tr>
                                    ) : (
                                        uploadAssets.map((asset, key) => (
                                            <tr key={key}>
                                                <td> {asset.id}
                                                    <span
                                                        className="ui-helper-hidden">({asset.type} "{asset.flavorType}//{asset.flavorSubType}")</span>
                                                </td>
                                                <td>
                                                    <div className="file-upload">
                                                        <input id={asset.id}
                                                               className="blue-btn file-select-btn"
                                                               accept={asset.accept}
                                                               onChange={e => formik.setFieldValue(asset.id, e.target.files[0])}
                                                               type="file"
                                                               tabIndex=""/>
                                                    </div>
                                                </td>
                                                {/*Button to remove asset*/}
                                                <td className="fit">
                                                    <a className="remove"
                                                       onClick={e => {
                                                           formik.setFieldValue(asset.id, null);
                                                           document.getElementById(asset.id).value = '';
                                                       }}/>
                                                </td>
                                            </tr>
                                        ))
                                    )}
                                </table>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            {/* Button for navigation to next page */}
            <footer>
                <button type="submit"
                        className={cn("submit")}
                        onClick={() => {
                            nextPage(formik.values);
                            onSubmit();
                        }}
                        tabIndex="100">{t('WIZARD.NEXT_STEP')}</button>
                <button className="cancel"
                        onClick={() => previousPage(formik.values, false)}
                        tabIndex="101">{t('WIZARD.BACK')}</button>
            </footer>

            <div className="btm-spacer"/>
        </>
    );


}

export default NewEventAssetUpload;
