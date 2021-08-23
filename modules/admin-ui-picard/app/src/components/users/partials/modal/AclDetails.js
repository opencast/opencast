import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import {Formik} from "formik";
import cn from "classnames";
import {NewAclSchema} from "../../../shared/wizard/validate";
import AclMetadataPage from "../wizard/AclMetadataPage";
import NewAclAccessPage from "../wizard/NewAclAccessPage";
import {getAclDetails} from "../../../../selectors/aclDetailsSelectors";
import {updateAclDetails} from "../../../../thunks/aclDetailsThunks";
import {connect} from "react-redux";

const AclDetails = ({close, aclDetails, updateAclDetails}) => {
    const { t } = useTranslation();

    const [page, setPage] = useState(0);

    const initialValues = {...aclDetails}

    const tabs = [
        {
            tabNameTranslation: 'USERS.ACLS.DETAILS.TABS.METADATA',
            name: 'metadata'
        },
        {
            tabNameTranslation: 'USERS.ACLS.DETAILS.TABS.ACCESS',
            name: 'access'
        }
    ];

    const openTab = tabNr => {
        setPage(tabNr);
    };

    const handleSubmit = values => {
        console.log("to be implemented")
        close();
    }

    return (
        <>
            {/* Navigation */}
            <nav className="modal-nav" id="modal-nav">
                <a className={cn({active: page === 0})}
                   onClick={() => openTab(0)}>
                    {t(tabs[0].tabNameTranslation)}
                </a>
                <a className={cn({active: page === 1})}
                   onClick={() => openTab(1)}>
                    {t(tabs[1].tabNameTranslation)}
                </a>
            </nav>

            <Formik initialValues={initialValues}
                    //validationSchema={NewAclSchema[0]}
                    onSubmit={values => handleSubmit(values)}>
                {formik => (
                    <>
                        {page === 0 && (
                            <AclMetadataPage formik={formik} isEdit/>
                        )}
                        {page === 1 && (
                            <NewAclAccessPage formik={formik} />
                        )}

                        {/* Navigation buttons and validation */}
                        <footer>
                            <button className={cn("submit", {
                                active: (formik.dirty && formik.isValid),
                                inactive: !(formik.dirty && formik.isValid)})
                            }
                                    disabled={!(formik.dirty && formik.isValid)}
                                    onClick={() => formik.handleSubmit()}
                                    type="submit">
                                {t('SUBMIT')}
                            </button>
                            <button className="cancel"
                                    onClick={() => close()}>
                                {t('CANCEL')}
                            </button>
                        </footer>
                    </>
                )}

            </Formik>
        </>
    );
}

const mapStateToProps = state => ({
    aclDetails: getAclDetails(state)
});

const mapDispatchToProps = dispatch => ({
    updateAclDetails: (values, aclId) => dispatch(updateAclDetails(values, aclId))
})

export default connect(mapStateToProps, mapDispatchToProps)(AclDetails);
