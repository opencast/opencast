import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import ConfirmModal from "../../shared/ConfirmModal";
import {deleteSeries} from "../../../thunks/seriesThunks";
import {connect} from "react-redux";
import SeriesDetailsModal from "./modals/SeriesDetailsModal";
import {fetchSeriesDetails} from "../../../thunks/seriesDetailsThunks";

/**
 * This component renders the action cells of series in the table view
 */
const SeriesActionsCell = ({ row, deleteSeries, fetchSeriesDetails }) => {
    const { t } = useTranslation();

    const [displayDeleteConfirmation, setDeleteConfirmation] = useState(false);
    const [displaySeriesDetailsModal, setSeriesDetailsModal] = useState(false);

    const hideDeleteConfirmation = () => {
        setDeleteConfirmation(false);
    };

    const deletingSeries = id => {
        deleteSeries(id)
    };

    const hideSeriesDetailsModal = () => {
        setSeriesDetailsModal(false);
    }

    const showSeriesDetailsModal = async () => {
        await fetchSeriesDetails(row.id);

        setSeriesDetailsModal(true);
    }

    return (
        <>

            {/*TODO: When series details are implemented, remove placeholder
            {/*TODO: with-Role ROLE_UI_SERIES_DETAILS_VIEW*/}
            <a onClick={() => showSeriesDetailsModal()}
               className="more-series"
               title={t('EVENTS.SERIES.TABLE.TOOLTIP.DETAILS')}/>


            {displaySeriesDetailsModal && (
                <SeriesDetailsModal handleClose={hideSeriesDetailsModal}
                                    seriesId={row.id}
                                    seriesTitle={row.title}/>
            )}

            {/*TODO: with-Role ROLE_UI_SERIES_DELETE*/}
            <a onClick={() => setDeleteConfirmation(true)}
               className="remove"
               title={t('EVENTS.SERIES.TABLE.TOOLTIP.DELETE')}/>

            {displayDeleteConfirmation && (
                <ConfirmModal close={hideDeleteConfirmation}
                              resourceName={row.title}
                              resourceType="SERIES"
                              resourceId={row.id}
                              deleteMethod={deletingSeries}/>
            )}
        </>
    )
}


//todo: remove if not needed anymore
const onClickPlaceholder = row => {
    console.log("In the Future here opens an other component, which is not implemented yet");
};

const mapDispatchToProps = dispatch => ({
    deleteSeries: id => dispatch(deleteSeries(id)),
    fetchSeriesDetails: id => dispatch(fetchSeriesDetails(id))
})

export default connect(null, mapDispatchToProps)(SeriesActionsCell);
