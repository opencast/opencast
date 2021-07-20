import {useTranslation} from "react-i18next";
import React, {useRef, useState} from "react";
import axios from "axios";
import {connect} from "react-redux";
import {useField} from "formik";
import {addNotification} from "../../../thunks/notificationThunks";
import {NOTIFICATION_CONTEXT} from "../../../configs/modalConfig";

/**
 * This component renders a custom file upload button in wizards.
 */
const FileUpload = ({ descriptionKey, labelKey, buttonKey, acceptableTypes, formikField, addNotification }) => {
    const { t } = useTranslation();

    // Formik hook for getting data of specific form field
    // DON'T delete meta, hook works with indices not variable names
    const [field, meta, helpers] = useField(formikField);

    // Temporary storage for uploaded file
    const [file, setFile] = useState({});
    // how much is uploaded; used for progress bar
    const [loaded, setLoaded] = useState(0);

    // reference used for activating file input when button is clicked
    const hiddenFileInput = useRef(null);

    const handleDelete = () => {
        setFile({});
        setLoaded(0);
        helpers.setValue({});
    }

    // upload file to backend
    const upload = file => {
        const data = new FormData();
        data.append('BODY', file, file.name);
        console.log(data);
        axios.post('/staticfiles', data, {
            headers: {
                'Content-Type': 'multipart/form-data'
            },
            onUploadProgress: ProgressEvent => {
                // update loaded with current progress
                setLoaded((ProgressEvent.loaded / ProgressEvent.total * 100))
            }
        }).then(res => {
            if (res.status === 201) {
                // set information about file later needed for POST request and summary
                helpers.setValue({
                    id: res.data,
                    name: file.name
                })

            }
        }).catch(res => {
            addNotification('error', 'NOTIFICATIONS.BUMPER_UPLOAD_ERROR', -1, null, NOTIFICATION_CONTEXT);
        })
    }

    const handleClick = () => {
        hiddenFileInput.current.click();
    }

    const handleChange = e => {
        setFile(e.target.files[0]);
        upload(e.target.files[0]);
    };

    return (
        <div className="list-row file-upload">

            {!!descriptionKey && (
                <p className="description">{t(descriptionKey)}</p>
            )}

            <div className="list-sub-row">
                <div className="header-column">
                    <label className="large">{t(labelKey)}</label>
                </div>
                <div className="content-column">
                    <div className="content-container">
                        {/* If user already uploaded a file, its name and a delete button is rendered */}
                        {/* else render button for upload */}
                        {!!field.value.id ? (
                            <div className="upload-file-info">
                                <p><a href={file.url} target="_blank">{field.value.name}</a></p>
                                <div className="button-container">
                                    <button id="remove-file-1" className="remove-file-button" onClick={() => handleDelete()}>
                                        <div className="remove-icon" />
                                    </button>
                                </div>
                            </div>
                        ) : (
                            <>
                                <button className="upload-button" onClick={() => handleClick()}>
                                    {t(buttonKey)}
                                </button>
                                <input type="file"
                                       style={{display: 'none'}}
                                       accept={acceptableTypes}
                                       ref={hiddenFileInput}
                                       onChange={e => {
                                           handleChange(e);
                                       }}/>
                            </>
                        )}

                    </div>
                </div>
            </div>

            {/* render progress bar while loaded is under 100 and a file is in the upload */}
            {!!file.name && loaded < 100 && (
                <div className="list-sub-row file-management">
                    <div className="progress-container">
                        <div className="progress compact">
                            <div className="progress-bar" role="progressbar" aria-valuenow={loaded} aria-valuemin="0"
                                 aria-valuemax="100" style={{width: loaded + '%'}}>
                                <span className="sr-only">{loaded}% Complete</span>
                            </div>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
};

const mapDispatchToProps = dispatch => ({
    addNotification: (type, key, duration, parameter, context) => dispatch(addNotification(type, key, duration, parameter, context))
})


export default connect(null, mapDispatchToProps)(FileUpload);
