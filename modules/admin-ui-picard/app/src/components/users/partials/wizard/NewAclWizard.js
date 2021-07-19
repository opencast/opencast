import React, {useState} from "react";
import {connect} from "react-redux";
import {Formik} from "formik";
import {NewAclSchema} from "../../../shared/wizard/validate";
import WizardStepper from "../../../shared/wizard/WizardStepper";
import NewAclMetadataPage from "./NewAclMetadataPage";
import NewAclAccessPage from "./NewAclAccessPage";
import NewAclSummaryPage from "./NewAclSummaryPage";
import {postNewAcl} from "../../../../thunks/aclThunks";
import {initialFormValuesNewAcl} from "../../../../configs/wizardConfig";

const NewAclWizard = ({ close, postNewAcl }) => {
    const initialValues = initialFormValuesNewAcl;

    const [page, setPage] = useState(0);
    const [snapshot, setSnapshot] = useState(initialValues);

    const steps = [
        {
            name: 'metadata',
            translation: 'USERS.ACLS.NEW.TABS.METADATA'
        },
        {
            name: 'access',
            translation: 'USERS.ACLS.NEW.TABS.ACCESS'
        },
        {
            name: 'summary',
            translation: 'USERS.ACLS.NEW.TABS.SUMMARY'
        }
    ];

    const currentValidationSchema = NewAclSchema[page];

    const nextPage = values => {
        setSnapshot(values);
        setPage(page + 1);
    }

    const previousPage = values => {
        setSnapshot(values);
        setPage(page - 1);
    }

    const handleSubmit = values => {
        const response = postNewAcl(values);
        console.log(response);
        close();
    }

    return (
        <>
            {/* Stepper that shows each step of wizard as header */}
            <WizardStepper steps={steps} page={page}/>

            {/* Initialize overall form */}
            <Formik initialValues={snapshot}
                    validationSchema={currentValidationSchema}
                    onSubmit={values => handleSubmit(values)}>
                {/* Render wizard pages depending on current value of page variable */}
                {formik => (
                    <div>
                        {page === 0 && (
                            <NewAclMetadataPage formik={formik}
                                                nextPage={nextPage}/>
                        )}
                        {page === 1 && (
                            <NewAclAccessPage formik={formik}
                                              nextPage={nextPage}
                                              previousPage={previousPage}/>
                        )}
                        {page === 2 && (
                            <NewAclSummaryPage formik={formik}
                                               previousPage={previousPage}/>
                        )}
                    </div>
                )}
            </Formik>
        </>
    );
};

const mapDispatchToProps = dispatch => ({
    postNewAcl: values => dispatch(postNewAcl(values))
});


export default connect(null, mapDispatchToProps)(NewAclWizard);
