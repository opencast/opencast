import React, {useState} from "react";
import {useTranslation} from "react-i18next";
import {connect} from 'react-redux';
import {DatePicker, MuiPickersUtilsProvider} from "@material-ui/pickers";
import DateFnsUtils from "@date-io/date-fns";
import {
    getCurrentFilterResource,
    getFilters,
    getSecondFilter,
    getSelectedFilter,
    getTextFilter
} from '../../selectors/tableFilterSelectors';
import {
    editFilterValue,
    editSecondFilter,
    editSelectedFilter,
    editTextFilter,
    removeSecondFilter,
    removeSelectedFilter,
    removeTextFilter,
    resetFilterValues
} from '../../actions/tableFilterActions';
import TableFilterProfiles from "./TableFilterProfiles";
import {getCurrentLanguageInformation} from "../../utils/utils";
import {availableHotkeys} from "../../configs/hotkeysConfig";
import {GlobalHotKeys} from "react-hotkeys";
import { getResourceType } from '../../selectors/tableSelectors';
import { fetchFilters } from '../../thunks/tableFilterThunks';


/**
 * This component renders the table filters in the upper right corner of the table
 */
const TableFilters = ({filterMap, textFilter, selectedFilter, secondFilter, onChangeTextFilter,
    removeTextFilter, editSelectedFilter, removeSelectedFilter, removeSecondFilter, resetFilterMap,
    editFilterValue, loadResource, loadResourceIntoTable, resource}) => {
    const { t } = useTranslation();

    // Variables for showing different dialogs depending on what was clicked
    const [showFilterSelector, setFilterSelector] = useState(false);
    const [showFilterSettings, setFilterSettings] = useState(false);

    // Variables containing selected start date and end date for date filter
    const [startDate, setStartDate] = useState(new Date());
    const [endDate, setEndDate] = useState(new Date());

    // Remove all selected filters, no filter should be "active" anymore
    const removeFilters = async () => {
        removeTextFilter();
        removeSelectedFilter();
        removeSelectedFilter();

        // Set all values of the filters in filterMap back to ""
        resetFilterMap();

        // Reload resources when filters are removed
        await loadResource();
        loadResourceIntoTable();

    }

    // Remove a certain filter
    const removeFilter = async filter => {
        editFilterValue(filter.name, "");

        // Reload resources when filter is removed
        await loadResource();
        loadResourceIntoTable();
    }

    // Handle changes when a item of the component is clicked
    const handleChange = async e => {
        const itemName = e.target.name;
        const itemValue = e.target.value;

        if (itemName === "textFilter") {
            onChangeTextFilter(itemValue);
        }

        if (itemName === "selectedFilter") {
            editSelectedFilter(itemValue);
        }

        // If the change is in secondFilter (filter is picked) then the selected value is saved in filterMap
        // and the filter selections are cleared
        if(itemName === "secondFilter") {
            let filter = filterMap.find(({ name }) => name === selectedFilter);
            editFilterValue(filter.name, itemValue);
            setFilterSelector(false);
            removeSelectedFilter();
            removeSecondFilter();

        }
        // Reload of resource
        await loadResource();
        loadResourceIntoTable();
    }

    // Set the sate of startDate and endDate picked with datepicker
    const handleDatepickerChange = async (date, isStart=false) => {
        if (isStart) {
            setStartDate(date);
        } else {
            setEndDate(date);
        }

        // When both dates set, then set the value for this filter
        if (!isStart) {
            let filter = filterMap.find(({ name }) => name === selectedFilter);
            editFilterValue(filter.name, startDate.toISOString() + '/' + endDate.toISOString());
            setFilterSelector(false);
            removeSelectedFilter();
            // Reload of resource
            await loadResource();
            loadResourceIntoTable();
        }

    };

    const hotKeyHandlers = {
        REMOVE_FILTERS: removeFilters
    }

    const renderBlueBox = filter => {
        let valueLabel = filter.options.find(opt => opt.value === filter.value).label;
        return (
            <span>
                {t(filter.label).substr(0, 40)}:
                {(filter.translatable) ? (
                    t(valueLabel).substr(0, 40)
                ) : (
                    valueLabel.substr(0, 40)
                )}
            </span>
        )
    }

    return (
        <>
            <GlobalHotKeys keyMap={availableHotkeys.general} handlers={hotKeyHandlers}/>
            <div className="filters-container">
                {/* Text filter - Search Query */}
                <input type="text"
                       className="search expand"
                       placeholder={t('TABLE_FILTERS.PLACEHOLDER')}
                       onChange={e => handleChange(e)}
                       name="textFilter"
                       value={textFilter}/>

                {/* Selection of filters and management of filter profiles*/}
                {/*show only if filters.filters contains filters*/}
                {!!filterMap && (
                    <div className="table-filter">
                        <div className="filters">
                            <i title={t('TABLE_FILTERS.ADD')}
                               className="fa fa-filter"
                               onClick={() => setFilterSelector(!showFilterSelector)}/>

                            {/*show if icon is clicked*/}
                            {showFilterSelector && (
                                <div>
                                    {/*Check if filters in filtersMap and show corresponding selection*/}
                                    {(!filterMap || false) ? (
                                        // Show if no filters in filtersList
                                        <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                                className="main-filter">
                                            <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                                        </select>

                                    ) : (
                                        // Show all filtersMap as selectable options
                                        <select disable_search_threshold="10"
                                                onChange={e => handleChange(e)}
                                                value={selectedFilter}
                                                name="selectedFilter"
                                                className="main-filter">
                                            <option value=""
                                                    disabled>{t('TABLE_FILTERS.FILTER_SELECTION.PLACEHOLDER')}</option>
                                            {
                                                filterMap.map((filter, key) => (
                                                    <option
                                                        key={key}
                                                        value={filter.name}>
                                                        {t(filter.label).substr(0, 40)}
                                                    </option>
                                                ))
                                            }
                                        </select>
                                    )}

                                </div>
                            )}

                            {/*Show selection of secondary filter if a main filter is chosen*/}
                            {!!selectedFilter && (
                                <div>
                                    {/*Show the secondary filter depending on the type of main filter chosen (select or period)*/}
                                    <FilterSwitch filterMap={filterMap}
                                                  selectedFilter={selectedFilter}
                                                  secondFilter={secondFilter}
                                                  startDate={startDate}
                                                  endDate={endDate}
                                                  handleDate={handleDatepickerChange}
                                                  handleChange={handleChange}/>
                                </div>
                            )
                            }

                            {/* Show for each selected filter a blue label containing its name and option */}
                            {
                                filterMap.map((filter, key) => {
                                    if (!!filter.value) {
                                        return (
                                            <span className="ng-multi-value" key={key}>
                                                <span>
                                                {
                                                    // Use different representation of name and value depending on type of filter
                                                    filter.type === 'select' ?
                                                            renderBlueBox(filter)
                                                        :
                                                        filter.type === 'period' ?
                                                            (
                                                                <span>
                                                                    <span>
                                                                        {t(filter.label).substr(0, 40)}:
                                                                        {t('dateFormats.date.short', {date: new Date(filter.value.split('/')[0])})}-
                                                                        {t('dateFormats.date.short', {date: new Date(filter.value.split('/')[1])})}
                                                                    </span>
                                                                </span>
                                                            ) : null
                                                }
                                                </span>
                                                {/* Remove icon in blue area around filter */}
                                                <a title={t('TABLE_FILTERS.REMOVE')}
                                                   onClick={() => removeFilter(filter)}>
                                                    <i className="fa fa-times"/>
                                                </a>
                                            </span>
                                        )
                                    }
                                })
                            }
                        </div>

                        {/* Remove icon to clear all filters */}
                        <i onClick={removeFilters}
                           title={t('TABLE_FILTERS.CLEAR')}
                           className="clear fa fa-times"/>
                        {/* Settings icon to open filters profile dialog (save and editing filter profiles)*/}
                        <i onClick={() => setFilterSettings(!showFilterSettings)}
                           title={t('TABLE_FILTERS.PROFILES.FILTERS_HEADER')}
                           className="settings fa fa-cog fa-times"/>

                        {/* Filter profile dialog for saving and editing filter profiles */}
                        <TableFilterProfiles showFilterSettings={showFilterSettings}
                                             setFilterSettings={setFilterSettings}
                                             resource={resource}
                                             loadResource={loadResource}
                                             loadResourceIntoTable={loadResourceIntoTable}/>


                    </div>
                )}

            </div>
        </>

    );


}

/*
 * Component for rendering the selection of options for the secondary filter
 * depending on the the type of the main filter. These types can be select or period.
 * In case of select, a second selection is shown. In case of period, datepicker are shown.
 */
const FilterSwitch = ({filterMap, selectedFilter, handleChange, startDate, endDate,
                          handleDate, secondFilter }) => {
    const {t} = useTranslation();

    const currentLanguage = getCurrentLanguageInformation();

    let filter = filterMap.find(({ name }) => name === selectedFilter);
    // eslint-disable-next-line default-case
    switch(filter.type) {
        case 'select':
            return (
                <div>
                    {/*Show only if selected main filter has translatable options*/}
                    {filter.translatable ? (
                        // Show if the selected main filter has no further options
                        (!filter.options || false) ? (
                            <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                    className="second-filter">
                                <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                            </select>
                        ) : (
                            // Show further options for a secondary filter
                            <select className="second-filter"
                                    onChange={e => handleChange(e)}
                                    value={secondFilter}
                                    name="secondFilter">
                                <option value="" disabled>{t('TABLE_FILTERS.FILTER_VALUE_SELECTION.PLACEHOLDER')}</option>
                                {
                                    filter.options.map((option, key) => (
                                        <option
                                            key={key}
                                            value={option.value}>
                                            {t(option.label).substr(0,40)}
                                        </option>
                                    ))
                                }
                            </select>
                        )

                    ) : (
                        // Show only if the selected main filter has options that are not translatable (else case from above)
                        (!filter.options || false) ? (
                            // Show if the selected main filter has no further options
                            <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                    className="second-filter">
                                <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                            </select>
                        ) : (
                            // Show further options for a secondary filter
                            <select className="second-filter"
                                    onChange={e => handleChange(e)}
                                    value={secondFilter}
                                    name="secondFilter">
                                <option value="" disabled>{t('TABLE_FILTERS.FILTER_VALUE_SELECTION.PLACEHOLDER')}</option>
                                {
                                    filter.options.map((option, key) => (
                                        <option
                                            key={key}
                                            value={option.value}>
                                            {option.label.substr(0,40)}
                                        </option>
                                    ))
                                }
                            </select>
                        )

                    )
                    }

                </div>
            );
        case 'period':
            return (
                <div>
                    {/* Show datepicker for start date */}
                    <MuiPickersUtilsProvider utils={DateFnsUtils} locale={currentLanguage.dateLocale}>
                        <DatePicker
                            className="small-search start-date"
                            value={startDate}
                            disableToolbar
                            format="dd/MM/yyyy"
                            onChange={(date) => handleDate(date, true)}
                        />
                        <DatePicker
                            className="small-search end-date"
                            value={endDate}
                            disableToolbar
                            format="dd/MM/yyyy"
                            onChange={(date) => handleDate(date)}
                        />
                    </MuiPickersUtilsProvider>
                </div>
            );
    }
}

// Getting state data out of redux store
const mapStateToProps = state => ({
    textFilter: getTextFilter(state),
    filterMap: getFilters(state),
    selectedFilter: getSelectedFilter(state),
    secondFilter: getSecondFilter(state),
    resourceType: getResourceType(state),
    filterResourceType: getCurrentFilterResource(state)
});

// Mapping actions to dispatch
const mapDispatchToProps = dispatch => ({
    onChangeTextFilter: textFilter => dispatch(editTextFilter(textFilter)),
    removeTextFilter: () => dispatch(removeTextFilter()),
    editSelectedFilter: filter => dispatch(editSelectedFilter(filter)),
    removeSelectedFilter: () => dispatch(removeSelectedFilter()),
    editSecondFilter: filter => dispatch(editSecondFilter(filter)),
    removeSecondFilter: () => dispatch(removeSecondFilter()),
    resetFilterMap: () => dispatch(resetFilterValues()),
    editFilterValue: (filterName, value) => dispatch(editFilterValue(filterName, value)),
    loadingFilters: resource => dispatch(fetchFilters(resource)),
});


export default connect(mapStateToProps, mapDispatchToProps)(TableFilters);
