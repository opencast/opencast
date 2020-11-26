import React, {useState} from "react";
import {Formik} from "formik";
import NewEventMetadata from "./NewEventMetadata";
import NewEventMetadataExtended from "./NewEventMetadataExtended";
import NewEventSource from "./NewEventSource";
import NewEventProcessing from "./NewEventProcessing";
import NewEventAccess from "./NewEventAccess";
import NewEventSummary from "./NewEventSummary";
import {getEventMetadata} from "../../../../selectors/eventSelectors";
import {connect} from "react-redux";

/**
 * This component manages the pages of the new event wizard and the submission of values
 */
const NewEventWizard = ({ onSubmit, metadataFields }) => {

    // Transform all initial values needed from information provided by backend
    let initialValues = {};
    metadataFields.fields.forEach(field => {
        initialValues[field.id] = field.value;
    });

    const [page, setPage] = useState(1);
    const [snapshot, setSnapshot] = useState(initialValues);

    const nextPage = values => {
        setSnapshot(values);
        setPage(page + 1);
    }

    const previousPage = values => {
        setSnapshot(values);
        setPage(page - 1);
    }

    //todo: implement
    const handleSubmit = values => {
        console.log("To be implemented!!!");
        console.log(values);
    }

    return (
        // Initialize overall form
        <Formik initialValues={snapshot}
                onSubmit={handleSubmit}>
            {/* Render wizard pages depending on current value of page variable */}
            {formik => (
                <div>
                    {page === 1 && <NewEventMetadata nextPage={nextPage}
                                                     values={formik.values}
                                                     onSubmit={() => console.log(formik.values)}/>}
                    {page === 2 && (
                        <NewEventMetadataExtended previousPage={previousPage}
                                                  nextPage={nextPage}
                                                  values={formik.values}
                                                  onSubmit={() => console.log("Step 2 onSubmit")}/>
                    )}
                    {page === 3 && (
                        <NewEventSource previousPage={previousPage}
                                        nextPage={nextPage}
                                        values={formik.values}
                                        onSubmit={() => console.log("Step 3 onSubmit")}/>
                    )}
                    {page === 4 && (
                        <NewEventProcessing previousPage={previousPage}
                                            nextPage={nextPage}
                                            values={formik.values}
                                            onSubmit={() => console.log("Step 4 onSubmit")}/>
                    )}
                    {page === 5 && (
                        <NewEventAccess previousPage={previousPage}
                                        nextPage={nextPage}
                                        values={formik.values}
                                        onSubmit={() => console.log("Step 5 onSubmit")}/>
                    )}
                    {page === 6 && (
                        <NewEventSummary previousPage={previousPage}
                                         nextPage={handleSubmit}
                                         values={formik.values}
                                         onSubmit={() => console.log("Step 6 onSubmit")}/>
                    )}
                </div>
            )}
        </Formik>
    )
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    metadataFields: getEventMetadata(state)
});

export default connect(mapStateToProps)(NewEventWizard);
