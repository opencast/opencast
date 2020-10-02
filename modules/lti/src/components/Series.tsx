import React from "react";
import { SearchEpisodeResults, searchEpisode, getLti, SearchEpisodeResult, deleteEvent } from "../OpencastRest";
import { Loading } from "./Loading";
import { withTranslation, WithTranslation } from "react-i18next";
import "../App.css";
import 'bootstrap/dist/css/bootstrap.css';
import Pagination from "react-js-pagination";
import Helmet from "react-helmet";
import { FontAwesomeIcon } from "@fortawesome/react-fontawesome";
import { faTrash, faEdit } from "@fortawesome/free-solid-svg-icons";
import i18next from "i18next";
import { parsedQueryString } from "../utils";

interface SeriesState {
    readonly searchResults?: SearchEpisodeResults;
    readonly ltiRoles?: string[];
    readonly httpErrors: string[];
    readonly currentPage: number;
    readonly deleteSuccess?: boolean;
}

interface SeriesProps extends WithTranslation {
}

interface EpisodeProps {
    readonly episode: SearchEpisodeResult;
    readonly deleteCallback?: (episodeId: string) => void;
    readonly editCallback?: (episodeId: string) => void;
    readonly t: i18next.TFunction;
}

const SeriesEpisode: React.StatelessComponent<EpisodeProps> = ({ episode, deleteCallback, editCallback, t }) => {
    const attachments = episode.mediapackage.attachments;
    const imageAttachment = attachments.find((a) => a.type.endsWith("/search+preview"));
    const image = imageAttachment !== undefined ? imageAttachment.url : "";
    return <div
        className="list-group-item list-group-item-action d-flex justify-content-start align-items-center episode-item"
        onClick={(_) => { window.location.href = "/play/" + episode.id; }}>
        <div>
            <img alt="Preview" className="img-fluid" src={image} />
        </div>
        <div className="ml-3">
            <h4>{episode.dcTitle}</h4>
            {episode.dcCreator !== undefined && <p className="text-muted">
                {t("LTI.CREATOR", { creator: episode.dcCreator })}
            </p>}
            <p className="text-muted">{new Date(episode.dcCreated).toLocaleString()}</p>
        </div>
        {(deleteCallback !== undefined || editCallback !== undefined) &&
            <div className="ml-auto">
                {deleteCallback !== undefined &&
                    <button onClick={(e) => { deleteCallback(episode.id); e.stopPropagation(); }}>
                        <FontAwesomeIcon icon={faTrash} />
                    </button>}
                {editCallback !== undefined &&
                    <button onClick={(e) => { editCallback(episode.id); e.stopPropagation(); }}>
                        <FontAwesomeIcon icon={faEdit} />
                    </button>}
            </div>}
    </div>;
}

class TranslatedSeries extends React.Component<SeriesProps, SeriesState> {
    constructor(props: SeriesProps) {
        super(props);
        this.state = {
            httpErrors: [],
            currentPage: 1,
        };
    }

    handlePageChange(pageNumber: number) {
        this.unsetDeletionState();
        this.setState({
            ...this.state,
            currentPage: pageNumber
        });
        this.loadCurrentPage();
    }

    loadCurrentPage() {
        const qs = parsedQueryString();
        const episodesPerPage = 15;
        searchEpisode(
            episodesPerPage,
            this.state.currentPage - 1,
            undefined,
            typeof qs.series === "string" ? qs.series : undefined,
            typeof qs.series_name === "string" ? qs.series_name : undefined
        ).then((results) => this.setState({
            ...this.state,
            searchResults: results
        })).catch((error) => this.setState({
            ...this.state,
            httpErrors: this.state.httpErrors.concat([error.message])
        }));
    }

    unsetDeletionState() {
        this.setState({
            ...this.state,
            deleteSuccess: undefined
        });
    }

    editEpisodeCallback(id: string) {
        const qs = parsedQueryString();
        let seriesSuffix = typeof qs.series === "string" ? "&series=" + qs.series : "";
        if (typeof qs.series_name === "string")
            seriesSuffix = "&series_name=" + qs.series_name;
        const debugSuffix = typeof qs.debug === "string" ? "&debug=" + qs.debug : "";
        window.location.href = "/ltitools/index.html?subtool=upload&episode_id=" + id + seriesSuffix + debugSuffix;
    }

    deleteEventCallback(id: string) {
        this.unsetDeletionState();
        if (window.confirm(this.props.t("LTI.CONFIRM_DELETION")) === false)
            return;
        deleteEvent(id).then((_) => {
            this.setState({
                ...this.state,
                deleteSuccess: true
            });
        }).catch((_) => {
            this.setState({
                ...this.state,
                deleteSuccess: false
            });
        });
    }

    hasDeletion() {
        return parsedQueryString().deletion === "true";
    }

    hasEdit() {
        return parsedQueryString().edit === "true";
    }

    componentDidMount() {
        this.loadCurrentPage();
        getLti().then((lti) => this.setState({
            ...this.state,
            ltiRoles: lti.roles
        })).catch((error) => this.setState({
            ...this.state,
            httpErrors: this.state.httpErrors.concat([`LTI: ${error.message}`])
        }))
    }

    isInstructor() {
        return this.state.ltiRoles !== undefined && this.state.ltiRoles.includes("Instructor");
    }

    render() {
        if (this.state.httpErrors.length > 0)
            return <div>{this.props.t("LTI.GENERIC_ERROR", { message: this.state.httpErrors[0] })}</div>;
        if (this.state.searchResults !== undefined && this.state.ltiRoles !== undefined) {
            const sr = this.state.searchResults;
            const headingOpts = {
                range: {
                    begin: Math.min(sr.offset + 1, sr.total),
                    end: sr.offset + sr.limit
                },
                total: sr.total
            };
            return <>
                <header>
                    {this.state.deleteSuccess === true && <div className="alert alert-success">
                        {this.props.t("LTI.DELETION_SUCCESS")}<br />
                        <div className="text-muted">{this.props.t("LTI.DELETION_SUCCESS_DESCRIPTION")}</div>
                    </div>}
                    {this.state.deleteSuccess === false && <div className="alert alert-danger">
                        {this.props.t("LTI.DELETION_FAILURE")}<br />
                        <div className="text-muted">{this.props.t("LTI.DELETION_FAILURE_DESCRIPTION")}</div>
                    </div>}
                    {this.props.t("LTI.RESULT_HEADING", headingOpts)}
                </header>
                <Helmet>
                    <title>{this.props.t("LTI.SERIES_TITLE")}</title>
                </Helmet>
                <div className="list-group">
                    {sr.results.map((episode) => <SeriesEpisode
                        key={episode.id}
                        episode={episode}
                        deleteCallback={this.isInstructor() && this.hasDeletion() ? this.deleteEventCallback.bind(this) : undefined}
                        editCallback={this.isInstructor() && this.hasEdit() ? this.editEpisodeCallback.bind(this) : undefined}
                        t={this.props.t} />)}
                </div>
                <footer className="mt-3">
                    <Pagination
                        activePage={this.state.currentPage}
                        itemsCountPerPage={sr.limit}
                        totalItemsCount={sr.total}
                        pageRangeDisplayed={5}
                        itemClass="page-item"
                        linkClass="page-link"
                        innerClass="pagination justify-content-center"
                        onChange={this.handlePageChange.bind(this)}
                    />
                </footer>
            </>
        }
        return <Loading t={this.props.t} />;
    }
}

export const Series = withTranslation()(TranslatedSeries);
