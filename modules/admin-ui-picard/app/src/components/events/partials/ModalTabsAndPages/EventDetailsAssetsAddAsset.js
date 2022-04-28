import React from "react";
import {connect} from "react-redux";
import EventDetailsTabHierarchyNavigation from "./EventDetailsTabHierarchyNavigation";
import Notifications from "../../../shared/Notifications";
import {style_button_spacing} from "../../../../utils/eventDetailsUtils";
import {uploadAssetOptions} from "../../../../configs/sourceConfig";
import {Formik} from "formik";
import { updateAssets } from '../../../../thunks/eventDetailsThunks';

/**
 * This component manages the add asset sub-tab for assets tab of event details modal
 */
const EventDetailsAssetsAddAsset = ({ eventId, t, setHierarchy, updateAssets }) => {

    const openSubTab = (subTabName) => {
        setHierarchy(subTabName);
    }


    function saveAssets (values) {
        updateAssets(values, eventId);
    }

    const handleChange = (e, formik, assetId) => {
        if (e.target.files.length === 0) {
            formik.setFieldValue(assetId, null);
        } else {
            formik.setFieldValue(assetId, e.target.files[0]);
        }
    }

    return (
        <div className="modal-content">
            {/* Hierarchy navigation */}
            <EventDetailsTabHierarchyNavigation
                openSubTab={openSubTab}
                hierarchyDepth={0}
                translationKey0={"EVENTS.EVENTS.NEW.UPLOAD_ASSET.ADD"}
                subTabArgument0={'add-asset'}
            />

            <div className="modal-body">
                {/* Notifications */}
                <Notifications context="not_corner"/>

                {/* section for adding assets */}
                <div className="full-col">
                    <div className="obj tbl-container operations-tbl">
                        <header>
                            {t("EVENTS.EVENTS.NEW.UPLOAD_ASSET.ADD") /* Attachments */}
                        </header>
                        <div className="obj-container">
                            <Formik initialValues={{newAssets: {}}}
                                    onSubmit={values => saveAssets(values)}>
                                {formik => (
                                    <div>

                                        {/* file select for upload for different types of assets */}
                                        <table className="main-tbl">
                                            <tbody>
                                                {uploadAssetOptions.length === 0 ? (
                                                    <tr>
                                                        <td>{t('EVENTS.EVENTS.NEW.UPLOAD_ASSET.NO_OPTIONS')}</td>
                                                    </tr>
                                                ) : (
                                                    uploadAssetOptions.map((asset, key) => (
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
                                                                           onChange={e => handleChange(e, formik, asset.id)}
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

                                        {/* add asset button */}
                                        <footer>
                                            <button className="submit"
                                                    style={style_button_spacing}
                                                    type="submit"
                                                    onClick={() => formik.handleSubmit()}
                                            >
                                                {t("EVENTS.EVENTS.NEW.UPLOAD_ASSET.ADD")}
                                            </button>
                                        </footer>
                                    </div>
                                )}
                            </Formik>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    updateAssets: (values, eventId) => dispatch(updateAssets(values, eventId))
});

export default connect(null, mapDispatchToProps)(EventDetailsAssetsAddAsset);
