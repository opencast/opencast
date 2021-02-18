import React from "react";
import {useTranslation} from "react-i18next";
import cn from "classnames";
import {uploadAssetOptions} from "../../../../configs/wizard/sourceConfig";

/**
 * This component renders the asset upload page of the new event wizard
 * (only if its not set hidden (see newEventWizardConfig) or user chose UPLOAD as source mode)
 */
const NewAssetUploadPage = ({ previousPage, nextPage , formik }) => {
    const { t } = useTranslation();

    // Get upload assets that are not of type track
    const uploadAssets = uploadAssetOptions.filter(asset => asset.type !== 'track');

    // if user not chose upload in step before, the skip this step
    if (formik.values.sourceMode !== 'UPLOAD') {
        nextPage(formik.values);
        return null;
    }

    const handleChange = (e, assetId) => {
        if (e.target.files.length === 0) {
            formik.setFieldValue(assetId, null);
        } else {
            formik.setFieldValue(assetId, e.target.files[0]);
        }
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
                                    <tbody>
                                        {uploadAssets.length === 0 ? (
                                            <tr>
                                                <td>{t('EVENTS.EVENTS.NEW.UPLOAD_ASSET.NO_OPTIONS')}</td>
                                            </tr>
                                        ) : (
                                            uploadAssets.map((asset, key) => (
                                                <tr key={key}>
                                                    <td> {asset.id}
                                                        <span className="ui-helper-hidden">
                                                            ({asset.type} "{asset.flavorType}//{asset.flavorSubType}")
                                                        </span>
                                                    </td>
                                                    <td>
                                                        <div className="file-upload">
                                                            <input id={asset.id}
                                                                   className="blue-btn file-select-btn"
                                                                   accept={asset.accept}
                                                                   onChange={e => handleChange(e, asset.id)}
                                                                   type="file"
                                                                   tabIndex=""/>
                                                            {formik.values[asset.id] && (
                                                                <span className="ui-helper">
                                                                    {formik.values[asset.id].name.substr(0, 50)}
                                                                </span>
                                                            )}
                                                        </div>
                                                    </td>
                                                    {/*Button to remove asset*/}
                                                    <td className="fit">
                                                        <a className="remove"
                                                           onClick={() => {
                                                               formik.setFieldValue(asset.id, null);
                                                               document.getElementById(asset.id).value = '';
                                                           }}/>
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
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

export default NewAssetUploadPage;
