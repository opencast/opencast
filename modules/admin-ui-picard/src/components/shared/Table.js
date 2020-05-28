import React, {Component} from "react";
import {useTranslation, withTranslation} from "react-i18next";

function allSelectedChanged() {
    console.log("all selected changed");
}

const Table = () => {
    const { t } = useTranslation();
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
            {/* todo: react equivalent to ng-class */}
            <table>
                <thead>
                    <tr>
                        {/* todo: only show if table.multiSelect true*/}
                        <th className="small">
                            {/* todo: react equivalent to ng-model (table.allSelected)*/}
                            <input type="checkbox" onChange={() => allSelectedChanged()}/>
                        </th>

                        {/* todo: repeat for each column in table.columns and if not column.deactivated*/}
                        {/* todo: react equivalent to ng-init (calculateStyle()), ng-style(column.style)
                         todo: ng-class (some style things)*/}
                        {/* todo: onClick for table.sortby(column) how??*/}
                        <th>
                            <span>
                                {/* todo: replace placeholder */}
                                column.label
                                {/* todo: show only if column.sortable = true */}
                                {/* todo: react equivalent for ng-class (sorting stuff) */}
                                <i className="sort"/>
                            </span>
                        </th>
                    </tr>
                </thead>
                <tbody>
                    {/* todo: Repeat for each row in table.rows (track by index??)*/}
                    {/* todo: what is data-row-id*/}
                    <tr>
                        {/* todo: show if table.multiSelect */}
                        {/* todo: react equivalent to ng-model (row.selected), ng-change (table.rowSelectionChanged())*/}
                        <td><input type="checkbox" /></td>
                        {/* todo: repeat for each column in table.column and show only if not template, not translate, not deactivated */}
                        <td>
                            {/* todo: replace placeholder */}
                            row[column.name]
                        </td>
                        {/* todo: show only if column not template, translate, not deactivated*/}
                        <td>
                            {/* todo: replace placeholder */}
                            row[column.name] trans
                        </td>
                        {/* todo: show only if column has template and include template here*/}
                        <td />
                    </tr>
                    {/*todo: only show if table.rows.lenght === 0 and table not loading */}
                    <tr>
                        {/* todo: replace placeholder in colSpan */}
                        <td colSpan="70">{t('TABLE_NO_RESULT')}</td>
                    </tr>
                    {/*todo: only show if table.rows.lenght === 0 and table loading */}
                    <tr>
                        {/* todo: replace placeholder in colSpan */}
                        <td colSpan="70" style={loadingTdStyle}>
                            <i className="fa fa-spinner fa-spin fa-2x fa-fw"/>
                        </td>
                    </tr>
                </tbody>
            </table>

            <div id="tbl-view-controls-container">
                <div className="drop-down-container small flipped">
                    <span>table.pagination.limit</span>
                    <ul className="dropdown-ul">
                        {[10, 20, 50, 100].map((x, key) => (
                            //todo: onClick: table.updatePageSize(x)
                            <li key={key}><a>{x}</a></li>
                        ))}
                    </ul>
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

export default Table;
