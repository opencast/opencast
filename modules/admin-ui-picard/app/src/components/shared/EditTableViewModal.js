import React, {useState, useEffect} from "react";
import {useTranslation} from "react-i18next";
import {changeColumnSelection} from "../../thunks/tableThunks";
import {connect} from "react-redux";


/**
 * This component renders the modal for adding new resources
 */
const EditTableViewModal = ({  showModal, handleClose, resource, activeColumns, deactivatedColumns, changeSelectedColumns }) => {
    const { t } = useTranslation();

    const originalActiveColumns = activeColumns;
    const originalDeactivatedColumns = deactivatedColumns;

    const [deactivatedCols, setDeactivatedColumns] = useState(deactivatedColumns);
    const [activeCols, setActiveColumns] = useState(activeColumns);

    useEffect(() => {
        setActiveColumns(activeColumns);
        setDeactivatedColumns(deactivatedColumns);
    }, [activeColumns, deactivatedColumns]);

    const close = () => {
        handleClose();
    };

    const changeColumn = (column, deactivate) => {
        if (deactivate) {
            setActiveColumns(activeCols.filter(col => col !== column));
            column = { ...column,
                deactivated: deactivate};
            setDeactivatedColumns(deactivatedCols.concat(column));
        } else {
            setDeactivatedColumns(deactivatedCols.filter(col => col !== column));
            column = { ...column,
                deactivated: deactivate};
            setActiveColumns(activeCols.concat(column));
        }
    };

    const save = () => {
        const settings = activeCols.concat(deactivatedCols);
        console.log(settings);
        changeSelectedColumns({columns: settings});
    };

    const clearData = () => {
        setActiveColumns(originalActiveColumns);
        setDeactivatedColumns(originalDeactivatedColumns);
    };

    return (
        // todo: add hotkeys
        <>
        {showModal && (
            <section className="modal active modal-animation" id="edit-table-view-modal">{/*ng-keyup="keyUp($event)"  */}
            {/*scheinbar hier nicht class="" direkt in*/}
                <header>
                    <a className="fa fa-times close-modal" onClick={() => {clearData(); close();}}/>
                    <h2>{t('PREFERENCES.TABLE.CAPTION') /* Edit Table View */}</h2>
                </header>

                <div className="modal-content">
                    <div className="modal-body">

                        <div className="tab-description for-header">
                            <p>{t('PREFERENCES.TABLE.SUBHEADING', { tableName: t('EVENTS.' + resource.toUpperCase() + '.TABLE.CAPTION') })}</p>
                        </div>

                        <div className="row">

                            <div className="col">
                                <div className="obj drag-available-column">
                                    <header>
                                        <h2>{t('PREFERENCES.TABLE.AVAILABLE_COLUMNS') /* Available Columns */}</h2>
                                    </header>
                                    <ul className="drag-drop-items">
                                        {
                                            deactivatedCols.map( (column, key) =>
                                            //table.columns.filter(column => column.deactivated).map( (column, key) =>
                                                column ?
                                                    <li className="drag-item" key={key}>
                                                        <div
                                                            className="title">{t(column.label)}</div>
                                                        <a className="move-item add"
                                                           onClick={() => changeColumn(column, false)}></a>
                                                    </li> :
                                                    null
                                            )
                                        }

                                    </ul>
                                </div>
                            </div>

                            <div className="col">
                                <div className="obj drag-selected-column">
                                    <header>
                                        <h2>{t('PREFERENCES.TABLE.SELECTED_COLUMNS') /* Selected Columns */}</h2>
                                    </header>
                                    <ul className="drag-drop-items">
                                        <li>{/*ui-sortable ng-model="activeColumns"*/}
                                            {
                                                activeCols.map( (column, key) =>
                                                //table.columns.filter(column => !column.deactivated).map( (column, key) =>
                                                    column ?
                                                        <div className="drag-item"
                                                             key={key}> {/*ng-repeat="column in activeColumns"*/}
                                                             <div
                                                                 className="title">{t(column.label)}</div>
                                                             <a className="move-item remove"
                                                                 onClick={() => changeColumn(column, true)}></a>
                                                        </div> :
                                                        null
                                                )
                                            }
                                        </li>
                                    </ul>
                                </div>
                            </div>

                        </div>

                        <div className="tab-description for-footer">
                            <p>
                                {/*<!-- The order and selection will be saved automatically.
                                Press "Reset" to restore the default view. -->*/}
                                {t('PREFERENCES.TABLE.FOOTER_TEXT', { resetTranslation: t('RESET') })}
                            </p>
                        </div>

                    </div>
                </div>

                <footer>
                    <div className="pull-left">
                        <button onClick={() => {
                            clearData();
                            close();
                        }} className="cancel active">{t('CANCEL')/*<!--Cancel-->*/}</button>
                    </div>
                    <div className="pull-right">
                        {/*<!-- <a ng-click="initialize()" class="cancel" translate="RESET">Reset</a> -->*/}
                        <button onClick={() => {
                            save();
                            close();
                        }} className="submit active">{t('SAVE')/*<!-- Save As Default -->*/}</button>
                    </div>
                </footer>
            </section>

        )}
        </>
    );
};

// Getting state data out of redux store
const mapStateToProps = state => ({
    resource: state.table.resource,
    deactivatedColumns: state.table.columns.filter(column => column.deactivated),
    activeColumns: state.table.columns.filter(column => !column.deactivated)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    changeSelectedColumns: selectedColumns => dispatch(changeColumnSelection(selectedColumns))
});

export default connect(mapStateToProps, mapDispatchToProps)(EditTableViewModal);