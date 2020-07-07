import React, {useEffect, useState} from "react";
import {useTranslation} from "react-i18next";
import * as ts from "../../selectors/tableSelectors";
import * as ta from "../../actions/tableActions";
import {connect} from "react-redux";
import cn from 'classnames';

const containerPageSize = React.createRef();

const Table = ({table, selectAll, deselectAll, rowSelectionChanged, updatePageSize, templateMap}) => {
    // Size options for pagination
    const sizeOptions = [10, 20, 50, 100];

    const { t } = useTranslation();

    // State of dropdown menu
    const [showPageSizes, setShowPageSizes] = useState(false);

    useEffect(() => {
        // Function for handling clicks outside of an open dropdown menu
        const handleClickOutside = e => {
            if (containerPageSize.current && !containerPageSize.current.contains(e.target)) {
                setShowPageSizes(false);
            }
        }

        // Event listener for handle a click outside of dropdown menu
        window.addEventListener('mousedown', handleClickOutside);

        return () => {
            window.removeEventListener('mousedown', handleClickOutside);
        }
    });

    const lengthDivStyle = {
        position: "absolute",
        visibility: "hidden",
        height: "auto",
        width: "auto",
        whiteSpace: "nowrap"
    };
    const loadingTdStyle = {
        textAlign: "center"
    };

    const onChangeAllSelected = e => {
        const selected = e.target.checked
        if (selected) {
            selectAll();
        } else {
            deselectAll();
        }

    };

    const changePageSize = size => {
        updatePageSize(size);
    }

    const applyColumnTemplate = (row, column) => {
        console.log("Row in Apply Template");
        console.log(row);
        console.log("column in Apply Template");
        console.log("Name: " + column.name + " Template: " + column.template);
        let Template = templateMap[column.template];
        console.log("Template in Apply Template");
        console.log(Template);
        return <Template row={row} />
    }

    return (
        <>
            <div className="action-bar">
              <ul>
                  {/* todo: look what data-open-modal and data-table is */}
                  <li><a>{t('TABLE_EDIT')}</a></li>
              </ul>
            </div>
            <div id="length-div" style={lengthDivStyle}>
            </div>
            <table className={'main-tbl highlight-hover'}>
                <thead>
                    <tr>
                        {/* Only show if multiple selection is possible */}
                        {table.multiSelect ? (
                            <th className="small">
                                {/*Checkbox to select all rows*/}
                                <input type="checkbox" onChange={e => onChangeAllSelected(e)}/>
                            </th>
                        ) : null}

                        {/* todo: repeat for each column in table.columns and if not column.deactivated*/}
                        {/* todo: react equivalent to ng-init (calculateStyle()), ng-style(column.style)
                         todo: ng-class (some style things)*/}
                        {/* todo: onClick for table.sortby(column) how??*/}
                        {table.columns.map((column, key) => (
                            <th key={key} className={cn({ 'col-sort': table.predicate === column.name, 'sortable': column.sortable })}>
                            <span>
                                {t(column.label)}
                                {/* Show only if column is sortable*/}
                                {column.sortable ? (
                                    <i className={cn("sort", { asc: table.predicate === column.name && !table.reverse, desc: table.predicate === column.name && table.reverse })}/>
                                ) : null}
                            </span>
                            </th>
                        )) }

                    </tr>
                </thead>
                <tbody>
                {(table.loading && table.rows.length === 0) ? (
                    //{/*todo: only show if table.rows.lenght === 0 and table loading TO CHECK*/}
                    // todo: put Loading in Redux state of table
                    <tr>
                        {/* todo: replace placeholder in colSpan */}
                        <td colSpan="70" style={loadingTdStyle}>
                            <i className="fa fa-spinner fa-spin fa-2x fa-fw"/>
                        </td>
                    </tr>
                ) : ((!table.loading && table.rows.length === 0) ? (
                    //Show if no results and table is not loading
                    <tr>
                        <td colSpan="70">{t('TABLE_NO_RESULT')}</td>
                    </tr>
                ) : (
                    //Repeat for each row in table.rows
                    table.rows.map((row) => (
                            <tr key={row.id}>
                                {/* Show if multi selection is possible */}
                                {/* Checkbox for selection of row */}
                                {table.multiSelect && (
                                    <td><input type="checkbox" checked={row.selected} onChange={() => rowSelectionChanged(row.id)}/></td>
                                )}
                                {/* todo: include template stuff */}
                                {table.columns.map((column, key) => (
                                    (!column.template && !column.translate && !column.deactivated) ?
                                        <td key={key}>
                                            {row[column.name]}
                                        </td>
                                     : (!column.template && column.translate && !column.deactivated) ?
                                        //Show only if column not template, translate, not deactivated
                                        <td key={key}>
                                            {t(row[column.name])}
                                        </td>
                                        : (!!column.template) ?
                                            <td key={key}>
                                                {applyColumnTemplate(row, column)}
                                            </td>
                                            : <td/>

                                ))}
                            </tr>
                        ))
                    ))}
                </tbody>
            </table>

            <div id="tbl-view-controls-container">
                <div className="drop-down-container small flipped" onClick={() => setShowPageSizes(!showPageSizes)} ref={containerPageSize}>
                    <span>{table.pagination.limit}</span>
                    {/* Drop down menu for selection of page size */}
                    {showPageSizes && (
                        <ul className="dropdown-ul">
                            {sizeOptions.map((size, key) => (
                                <li key={key}><a onClick={() => changePageSize(size)}>{size}</a></li>
                            ))}
                        </ul>
                    )}

                </div>

                {/* todo: add all the stuff that is missing*/}
                <div className="pagination">
                    <a className="prev"/>
                    <a className="active">page.label</a>
                    <a>page.label</a>
                    <a className="next"/>
                </div>
            </div>
        </>
    );
}

const mapStateToProps = state => ({
    table: state.table
});

const mapDispatchToProps = dispatch => ({
    selectAll: () => dispatch(ta.selectAll()),
    deselectAll: () => dispatch(ta.deselectAll()),
    rowSelectionChanged: (id, selected) => dispatch(ta.selectRow(id, selected)),
    updatePageSize: size => dispatch(ta.updatePageSize(size))
});

export default connect(mapStateToProps, mapDispatchToProps)(Table);
