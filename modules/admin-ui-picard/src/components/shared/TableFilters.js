import React, {Component} from "react";
import {withTranslation} from "react-i18next";
import DatePicker from "react-datepicker/es";
import filters from "../../resources/resourcesFilterResource";

const filtersList = Object.keys(filters.filters).map(key => {
    let filter = filters.filters[key];
    filter.name = key;
    return filter;
});

/**
 * This component renders the table filters in the upper right corner of the table
 */
class TableFilters extends Component {

    constructor(props) {
        super(props);

        // State of the individual filters and
        // variables for showing different dialogs depending on what was clicked
        this.state = {
            textFilter: "",
            showFilterSelector: false,
            showFilterSettings: false,
            filterMap: filtersList,
            selectedFilter: "",
            secondFilter: "",
            startDate: new Date(),
            endDate: new Date(),
            settingsMode: false
        }
        console.log(filters);

        // todo: which of these are actually needed, maybe some things do the same or quite similar stuff
        this.displayFilterSelector = this.displayFilterSelector.bind(this);
        this.selectFilterPeriodValue = this.selectFilterPeriodValue.bind(this);
        this.removeFilters = this.removeFilters.bind(this);
        this.removeFilter = this.removeFilter.bind(this);
        this.toggleFilterSettings = this.toggleFilterSettings.bind(this);
        this.loadFilterProfile = this.loadFilterProfile.bind(this);
        this.editFilterProfile = this.editFilterProfile.bind(this);
        this.removeFilterProfile = this.removeFilterProfile.bind(this);
        this.saveProfile = this.saveProfile.bind(this);

        this.handleChange = this.handleChange.bind(this);
        this.onChangeSelectMainFilter = this.onChangeSelectMainFilter.bind(this);
        this.renderFilterSwitch = this.renderFilterSwitch.bind(this);
        this.changeMode = this.changeMode.bind(this);

    }

    displayFilterSelector(e) {
        e.preventDefault();
        this.setState(state => ({
            showFilterSelector: !state.showFilterSelector
        }));
        console.log ("display filter selectors");
    }

    //todo: implement/look if really needed (handleEnddatePicker is quite similar)
    selectFilterPeriodValue() {
        console.log("select filter period value");
    }

    // Remove all selected filters, no filter should be "active" anymore
    removeFilters() {
        this.setState({
            textFilter: "",
            showFilterSelector: false,
            selectedFilter: "",
            secondFilter: "",
            startDate: new Date(),
            endDate: new Date()

        });

        // Iterate though filterMap and set all values to "" again
        for(let filter in this.state.filterMap) {
            this.removeFilter(this.state.filterMap[filter]);
        }


        console.log("remove filters");
        console.log(this.state.filterMap);
    }

    // Remove a certain filter
    removeFilter(filter) {
        console.log("filter to be removed:");
        console.log(filter);
        filter.value = "";
        console.log("remove certain filter");
    }

    // Toggle the dialog where filter profiles can be saved and edited
    toggleFilterSettings(e) {
        e.preventDefault();
        this.setState(state => ({
            showFilterSettings: !state.showFilterSettings
        }));
        console.log("toggle filter settings");
    }

    // todo: implement
    loadFilterProfile() {
        console.log("load filter profile");
    }

    // todo: implement
    editFilterProfile() {
        console.log("edit filter profile");
    }

    // todo: implement
    removeFilterProfile() {
        console.log("remove filter profile");
    }

    // todo: implement
    saveProfile() {
        console.log("save profile");
    }

    // Change between the view of filter profile list and editing/saving them
    changeMode(e) {
        e.preventDefault();
        this.setState(state => ({
            settingsMode: !state.settingsMode
        }));
    }

    // Set the sate of startDate picked with datepicker
    handleStartDatepickerChange = date => {
        this.setState({
            startDate: date
        });
        console.log(this.state.startDate);
    };

    // Set the sate of startDate picked with datepicker
    // todo: use one method to capture the change in the datepicker (if-cases)
    handleEndDatepickerChange = date => {
        this.setState({
            endDate: date
        });
        console.log(this.state.endDate);
    };

    // Handle changes when a item of the component is clicked
    handleChange(e) {
        const itemName = e.target.name;
        const itemValue = e.target.value;

        this.setState({ [itemName]: itemValue });

        // if the change is in secondFilter (filter is picked) then the selected value is saved in filterMap
        // and the filter selections are cleared
        if(itemName === "secondFilter") {
            let filter = this.state.filterMap.find(({ name }) => name === this.state.selectedFilter);
            filter.value = itemValue;
            console.log(this.state.filterMap);
            this.setState({
                showFilterSelector: false,
                selectedFilter: "",
                secondFilter:""
            });
        }
    }

    // todo: implement
    onChangeSelectMainFilter(e) {

    }

    /*
    * Method for rendering the selection of options for the secondary filter
    * depending on the the type of the main filter. These types can be select or period.
    * In case of select, a second selection is shown. In case of period, datepicker are shown.
    */
    renderFilterSwitch() {
        let filter = this.state.filterMap.find(({ name }) => name === this.state.selectedFilter);
        const { t } = this.props;
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
                                            onChange={this.handleChange}
                                            value={this.state.secondFilter}
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
                                        onChange={this.handleChange}
                                        value={this.state.secondFilter}
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
                        {/* todo: ui is still not working right, revisit this */}
                        <DatePicker selected={this.state.startDate}
                                    onChange={this.handleStartDatepickerChange}
                                    placeholderText={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.START_DATE')}
                                    className="small-search start-date"/>
                        {/*<input type="date"
                                   className="small-search start-date"
                                   onSelect={this.selectFilterPeriodValue}
                                   placeholder={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.START_DATE')}/>*/}

                        {/* Show datepicker for end date*/}
                        <DatePicker selected={this.state.endDate}
                                    onChange={this.handleEndDatepickerChange}
                                    placeholderText={t('EVENTS.EVENTS.NEW.SOURCE.PLACEHOLDER.END_DATE')}
                                    className="small-search end-date"/>
                    </div>
                );
        }
    }


    render() {
        // t is translation function of i18next
        const { t } = this.props;
        return (
          <div className="filters-container">
              {/* Text filter - Search Query */}
              {/* todo: Search icon is not showing yet*/}
              <input type="text"
                     className="search expand"
                     placeholder={t('TABLE_FILTERS.PLACEHOLDER')}
                     onChange={this.handleChange}
                     name="textFilter"
                     value={this.state.textFilter}/>

              {/* Selection of filters and management of filter profiles*/}
              {/*show only if filters.filters contains filters*/}
              {!!filters.filters && (
                  <div className="table-filter">
                      <div className="filters">
                          <i title={t('TABLE_FILTERS.ADD')}
                             className="fa fa-filter"
                             onClick={this.displayFilterSelector}/>

                          {/*show if icon is clicked*/}
                          {this.state.showFilterSelector && (
                              <div>
                                  {/*Check if filters in filtersMap and show corresponding selection*/}
                                  {(!this.state.filterMap || false) ? (
                                      // Show if no filters in filtersList
                                      <select defaultValue={t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}
                                              className="main-filter">
                                          <option disabled>{t('TABLE_FILTERS.FILTER_SELECTION.NO_OPTIONS')}</option>
                                      </select>

                                  ) : (
                                      // Show all filtersMap as selectable options
                                      <select chosen
                                              disable_search_threshold="10"
                                              onChange={this.handleChange}
                                              value={this.state.selectedFilter}
                                              name="selectedFilter"
                                              className="main-filter">
                                          <option value="" disabled>{t('TABLE_FILTERS.FILTER_SELECTION.PLACEHOLDER')}</option>
                                          {
                                              filtersList.map((filter, key) => (
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
                          {!!this.state.selectedFilter && (
                              <div>
                                  {/*Show the secondary filter depending on the type of main filter chosen (select or period)*/}
                                  {this.renderFilterSwitch()}
                              </div>
                          )
                          }

                          {/* Show for each selected filter a blue label containing its name and option */}
                          {
                              this.state.filterMap.map((filter, key) => {
                                  if(!!filter.value) { return (
                                      <span className="ng-multi-value" key={key}>
                                        <span>
                                            {
                                                // Use different representation of name and value depending on type of filter
                                                {
                                                    'select': (
                                                        <span>
                                                            {t(filter.label).substr(0,40)}:
                                                            {(filter.translatable) ? (
                                                                t(filter.value).substr(0, 40)
                                                                ) : (
                                                                filter.value.substr(0, 40)
                                                                )}
                                                        </span>
                                                    ),
                                                    'period': (
                                                        <span>
                                                            <span>
                                                                {/*todo: format date range*/}
                                                                {t(filter.label).substr(0,40)} : Placeholder
                                                            </span>
                                                        </span>
                                                    )
                                                }[filter.type]
                                            }
                                        </span>
                                          {/* Remove icon in blue area around filter */}
                                          <a title={t('TABLE_FILTERS.REMOVE')} onClick={() => this.removeFilter(filter)}>
                                            <i className="fa fa-times"/>
                                          </a>
                                    </span>
                                  )

                              }})
                          }

                      </div>

                      {/* Remove icon to clear all filters */}
                      <i onClick={this.removeFilters}
                         title={t('TABLE_FILTERS.CLEAR')}
                         className="clear fa fa-times" />
                      {/* Settings icon to open filters profile dialog (save and editing filter profiles)*/}
                      <i onClick={this.toggleFilterSettings}
                         title={t('TABLE_FILTERS.PROFILES.FILTERS_HEADER')}
                         className="settings fa fa-cog fa-times" />

                      {/* Filter profile dialog for saving and editing filter profiles */}
                      {this.state.showFilterSettings && (
                          <div className="btn-dd filter-settings-dd df-profile-filters">
                              {/* depending on settingsMode show list of all saved profiles or the chosen profile to edit*/}
                              {this.state.settingsMode ? (
                                  // if settingsMode is true the list with all saved profiles is shown
                                  <div className="filters-list">
                                      <header>
                                          <a className="icon close" onClick={this.toggleFilterSettings}/>
                                          <h4>{t('TABLE_FILTERS.PROFILES.FILTERS_HEADER')}</h4>
                                      </header>
                                      <ul>
                                          {/*todo: if no profiles saved yet (profiles.length == 0)*/}
                                          <li>{t('TABLE_FILTERS.PROFILES.EMPTY')}</li>
                                          {/*todo: repeat for each profile in profiles (else-case)*/}
                                          <li>
                                              <a title="profile.description"
                                                 onClick={this.loadFilterProfile}>
                                                  {/*todo: just a placeholder*/}
                                                  profile.name limit 70
                                              </a>
                                              {/* Settings icon to edit profile */}
                                              <a onClick={this.editFilterProfile}
                                                 title={t('TABLE_FILTERS.PROFILES.EDIT')}
                                                 className="icon edit"/>
                                              {/* Remove icon to remove profile */}
                                              <a onClick={this.removeFilterProfile}
                                                 title={t('TABLE_FILTERS.PROFILES.REMOVE')}
                                                 className="icon remove"/>
                                          </li>
                                      </ul>

                                      {/* Save the currently selected filter options as new profile */}
                                      {/* settingsMode is switched and save dialog is opened*/}
                                      <div className="input-container">
                                          <div className="btn-container">
                                              <a className="save" onClick={this.changeMode}>
                                                  {/*todo: limit to 70*/}
                                                  {t('TABLE_FILTERS.PROFILES.SAVE_FILTERS').substr(0,40)}
                                              </a>
                                          </div>
                                      </div>
                                  </div>
                              ) : (
                                  // if settingsMode is false then show editing dialog of selected filter profile
                                  <div className="filter-details">
                                      <header>
                                          <a className="icon close" onClick={this.toggleFilterSettings} />
                                          <h4>{t('TABLE_FILTERS.PROFILES.FILTER_HEADER')}</h4>
                                      </header>
                                      {/* Input form for save/editing profile*/}
                                      <div>
                                          <label>{t('TABLE_FILTERS.PROFILES.NAME')} <i className="required">*</i></label>
                                          {/*todo: find react equivalent of ng-model (profile.name), ng-change*/}
                                          <input required
                                                 name="name"
                                                 type="text"
                                                 placeholder={t('TABLE_FILTERS.PROFILES.NAME_PLACEHOLDER')}/>

                                          <label>{t('TABLE_FILTERS.PROFILES.DESCRIPTION')}</label>
                                          {/*todo: find react equivalent of ng-model (profile.description)*/}
                                          <textarea placeholder={t('TABLE_FILTERS.PROFILES.DESCRIPTION_PLACEHOLDER')} />
                                      </div>
                                      <div className="input-container">
                                          {/* Buttons for saving and canceling editing */}
                                          <div className="btn-container">
                                              <a onClick={this.toggleFilterSettings} className="cancel">{t('CANCEL')}</a>
                                              <a onClick={this.saveProfile} className="save">{t('SAVE')}</a>
                                          </div>
                                      </div>
                                  </div>

                              )
                              }

                          </div>



                      )}


                  </div>
              )}

          </div>
        );
    }
}

export default withTranslation()(TableFilters);
