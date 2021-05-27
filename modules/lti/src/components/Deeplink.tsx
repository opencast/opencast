import React from "react";
import { Loading } from "./Loading";
import Helmet from "react-helmet";
import { searchEpisode, getLti, SearchEpisodeResults, postDeeplinkData } from "../OpencastRest";
import { parsedQueryString } from "../utils";
import { withTranslation, WithTranslation } from "react-i18next";
import Pagination from "react-js-pagination";
import i18next from "i18next";
import { Container, Tabs, Tab, Form, Button, Col } from 'react-bootstrap';
import InnerHTML from 'dangerously-set-html-content'
import "../App.css";
import './Deeplink.css';
import 'engage-ui/src/main/resources/ui/css/engage-ui.css';
import 'bootstrap/dist/css/bootstrap.css';

const pagingLimit: number = 15;

interface DeeplinkPagingPagingProps{
    readonly currentPage: number;
    readonly handlePageChange: (pageNumber: number) => void;
    readonly results?: SearchEpisodeResults;
}

const DeeplinkPaging: React.FC<DeeplinkPagingPagingProps> = ({ currentPage, handlePageChange, results }) => {
    return results !== undefined ? <Pagination
        activePage={currentPage}
        itemsCountPerPage={pagingLimit}
        totalItemsCount={results.total}
        pageRangeDisplayed={5}
        itemClass="page-item"
        linkClass="page-link"
        innerClass="pagination justify-content-center w-100"
        onChange={handlePageChange}
    /> : null
}

interface DeeplinkState {
    readonly httpErrors: string[];
    readonly currentPage: number;
    readonly episodesFilter?: string;
    readonly seriesFilter?: string;
    readonly searchEpisodeResults?: SearchEpisodeResults;
    readonly searchSeriesResults?: SearchEpisodeResults;
    readonly showEpisodeTemplate: boolean;
    readonly populatedData?: string;
}

interface DeeplinkProps extends WithTranslation {
    readonly t: i18next.TFunction;
}

class TranslatedDeeplink extends React.Component<DeeplinkProps, DeeplinkState> {
    constructor(props: DeeplinkProps) {
        super(props);
        this.state = {
            httpErrors: [],
            currentPage: 1,
            showEpisodeTemplate: true
        };
    }

    loadEpisodesTab(page: number, q?: string) {
        const qs = parsedQueryString();
        const offset: number = (page - 1) * pagingLimit;

        this.setState({
            ...this.state,
            currentPage: page,
            episodesFilter: q,
            showEpisodeTemplate: true
        });

        searchEpisode(
            pagingLimit,
            offset,
            undefined,
            typeof qs.series === "string" ? qs.series : undefined,
            typeof qs.series_name === "string" ? qs.series_name : undefined,
            q
        ).then((results) => this.setState({
            ...this.state,
            searchEpisodeResults: results
        })).catch((error) => this.setState({
            ...this.state,
            httpErrors: this.state.httpErrors.concat([error.message])
        }));


    }

    loadSeriesTab(page: number, q?: string) {
        const qs = parsedQueryString();
        const offset: number = (page - 1) * pagingLimit;

        this.setState({
            ...this.state,
            currentPage: page,
            seriesFilter: q,
            showEpisodeTemplate: false
        });

        searchEpisode(
            pagingLimit,
            offset,
            undefined,
            typeof qs.series === "string" ? qs.series : undefined,
            typeof qs.series_name === "string" ? qs.series_name : undefined,
            q,
            true
        ).then((results) => this.setState({
            ...this.state,
            searchSeriesResults: results
        })).catch((error) => this.setState({
            ...this.state,
            httpErrors: this.state.httpErrors.concat([error.message])
        }));
    }

    componentDidMount() {
        getLti().then((lti) => {
            this.setState({
                ...this.state,
                episodesFilter: lti.context_label,
                seriesFilter: lti.context_label
            })
            this.loadEpisodesTab(1, lti.context_label);
            this.loadSeriesTab(1, lti.context_label);
        }).catch((error) => this.setState({
            ...this.state,
            httpErrors: this.state.httpErrors.concat([`LTI: ${error.message}`])
        }))
    }

    handlePageChange(pageNumber: number) {
        this.setState({
            ...this.state,
            currentPage: pageNumber
        });

        if(this.state.showEpisodeTemplate){
            this.loadEpisodesTab(pageNumber, this.state.episodesFilter);
        } else {
            this.loadSeriesTab(pageNumber, this.state.seriesFilter);
        }
    }

    formatDuration(duration: number) {
        const round: number = 1000, max: number = 60, numOfInts: number = 2;
        const seconds = ("0" + Math.floor(duration / round % max).toString()).slice(-numOfInts),
            minutes = ("0" + Math.floor(duration / (round * max) % max).toString()).slice(-numOfInts),
            hours = ("0" + Math.floor(duration / (round * max * max) % max).toString()).slice(-numOfInts);

        return hours + ':' + minutes + ':' + seconds;
    }

    populateData(title: string, image: string, created: string, tool: string) {
        const qs = parsedQueryString();

        // generate content_items
        const contentItemsObject = {
          '@context': 'http://purl.imsglobal.org/ctx/lti/v1/ContentItem',
          '@graph': [{
            '@type': 'LtiLinkItem',
            mediaType: 'application/vnd.ims.lti.v1.ltilink',
            title: title,
            text: created,
            thumbnail: {'@id': image},
            custom: {
              tool: tool
            }
          }]
        };

        const contentItems: string = JSON.stringify(contentItemsObject).replace(/"/g, '"');

        postDeeplinkData(
            contentItems,
            typeof qs.content_item_return_url === 'string' ? qs.content_item_return_url : undefined,
            typeof qs.consumer_key === 'string' ? qs.consumer_key : undefined,
            typeof qs.data === 'string' ? qs.data : undefined,
            typeof qs.test === 'string' ? qs.test : undefined
        ).then((response) => {
            this.setState({
                ...this.state,
                populatedData: response.data
            });
        }).catch((error) => {
            this.setState({
                ...this.state,
                httpErrors: this.state.httpErrors.concat([`Data: ${error.message}`])
            });
        });
    }

    generateSeriesColor(id: string) {
        if (id == null) {
            return '#fff';
        }

        const rgbMax: number = 220, rgbOffset: number = 20;
        const seriesRgbMax = [rgbMax, rgbMax, rgbMax],
            seriesRgbOffset = [rgbOffset, rgbOffset, rgbOffset],
            stringRadix = 16
        const rgb: any[] = [0, 0, 0];
        let color: string = '#';

        for (let i = 0; i < id.length; ++i) {
            rgb[i % seriesRgbMax.length] += id.charCodeAt(i);
        }

        for (let i = 0; i < seriesRgbMax.length; ++i) {
            rgb[i] = (rgb[i] % seriesRgbMax[i] + seriesRgbOffset[i]).toString(stringRadix);
            if (rgb[i].length < 1) {
                rgb[i] = "0".concat(rgb[i]);
            }
            color = color.concat(rgb[i]);
        }

        return color;
    }

    formatDate(date: string) {
        const d: Date = new Date(date);
        const padding: number = -2;
        const day: string = ("0" + d.getDate().toString()).slice(padding),
            month: string = ("0"+(d.getMonth()+1).toString()).slice(padding),
            year: string = d.getFullYear().toString(),
            hours: string = ("0" + d.getHours().toString()).slice(padding),
            minutes: string = ("0" + d.getMinutes().toString()).slice(padding);
        return  day + "/" + month + "/" + year + " " + hours + ":" + minutes;
    }

    render() {
        if (this.state.httpErrors.length > 0) {
            return <div>{this.props.t("LTI.GENERIC_ERROR", { message: this.state.httpErrors[0] })}</div>;
        }
        return <>
            <Helmet>
                <title>Opencast: Deep Linking</title>
            </Helmet>
            <h2>Deep Linking</h2>
            {/* Use InnerHTML to render dynamic html and execute any scripts tag within it. */}
            {this.state.populatedData !== undefined && <InnerHTML html={this.state.populatedData} />}
            <Tabs defaultActiveKey="episodes">
                <Tab eventKey="episodes" title="Episodes">
                    <Form.Row id="episodes-searchfield" className="searchfield">
                        <Col>
                            <Form.Control type="text" placeholder="Filter" value={this.state.episodesFilter} onChange={(e) => {
                                this.setState({
                                    ...this.state,
                                    episodesFilter: e.target.value
                                })
                            }}/>
                        </Col>
                        <Col>
                            <Button variant="primary" onClick={() => {this.loadEpisodesTab(1, this.state.episodesFilter)}}>
                                Refresh list
                            </Button>
                        </Col>
                    </ Form.Row>
                    <Container fluid id="episodes-results" className="p-0">
                    { this.state.searchEpisodeResults !== undefined ? this.state.searchEpisodeResults.total !== 0 ? this.state.searchEpisodeResults.results.map((episode) => {
                        return (
                            <div className="col-xs-12 col-sm-6 col-md-4 col-lg-4 float-left">
                                <div className="tile">
                                    <div className="seriesindicator" style={{backgroundColor: this.generateSeriesColor(episode.id)}} />
                                    <div className="tilecontent">
                                        <h4 className="title">{episode.dcTitle}</h4>
                                        <div>
                                            {episode.mediapackage.attachments.map((attachment) => {
                                                return (
                                                    attachment.type.endsWith('/search+preview') ? <img className="thumbnail img-fluid img-thumbnail" alt="Preview" src={attachment.url} /> : null
                                                )
                                            })}
                                        </div>
                                        <div className="infos">
                                            {episode.dcCreator !== undefined && <div className="creator">
                                                {this.props.t("LTI.CREATOR", { creator: episode.dcCreator })}
                                            </div>}
                                            {episode.mediapackage.seriestitle !== undefined && <div className="seriestitle">{episode.mediapackage.seriestitle}</div>}
                                            <div className="date">{this.formatDate(episode.dcCreated)}</div>
                                            {episode.mediapackage.duration !== undefined && <div className="duration">{this.formatDuration(episode.mediapackage.duration)}</div>}
                                            <Button variant="primary" className="card-text selectitem" onClick={() => {this.populateData(episode.dcTitle, episode.mediapackage.attachments[0].url, this.formatDate(episode.dcCreated), '/play/' + episode.id)}}>Select</Button>
                                        </div>
                                    </div>
                                </div>
                            </div>
                    )}) : <p className="ml-2 pl-1">No episodes found.</p> : <Loading t={this.props.t} />}
                    </Container>
                    <div className="clearfix" />
                    <DeeplinkPaging
                        currentPage={this.state.currentPage}
                        results={this.state.searchEpisodeResults}
                        handlePageChange={this.handlePageChange.bind(this)}
                    />
                </Tab>
                <Tab eventKey="series" title="Series">
                    <Form.Row id="series-searchfield" className="searchfield">
                        <Col>
                            <Form.Control type="text" placeholder="Filter" value={this.state.seriesFilter} onChange={(e) => {
                                this.setState({
                                    ...this.state,
                                    seriesFilter: e.target.value
                                })
                            }}/>
                        </Col>
                        <Col>
                            <Button variant="primary" onClick={() => {this.loadSeriesTab(1, this.state.seriesFilter)}}>
                                Refresh list
                            </Button>
                        </Col>
                    </Form.Row>
                    <Container fluid id="series-results" className="p-0">
                    { this.state.searchSeriesResults !== undefined ? this.state.searchSeriesResults.total !== 0 ? this.state.searchSeriesResults.results.map((serie) => {
                        return (
                        <>
                            <div className="col-xs-12 col-sm-6 col-md-4 col-lg-4 float-left">
                                <div className="tile">
                                    <div className="seriesindicator" style={{backgroundColor: this.generateSeriesColor(serie.id)}} />
                                    <div className="tilecontent">
                                        <h4 className="title">{serie.dcTitle}</h4>
                                    </div>
                                    <Button variant="primary" className="selectitem" onClick={() => { this.populateData(serie.dcTitle, 'engage/ui/img/logo/opencast-icon.svg', this.formatDate(serie.dcCreated), 'ltitools/series/index.html?series=' + serie.id) }}>Select</Button>
                                </div>
                            </div>
                        </>
                    )}) : <p className="ml-2 pl-1">No series found.</p> : <Loading t={this.props.t} />}
                    </Container>
                    <div className="clearfix" />
                    <DeeplinkPaging
                        currentPage={this.state.currentPage}
                        results={this.state.searchSeriesResults}
                        handlePageChange={this.handlePageChange.bind(this)}
                    />
                </Tab>
            </Tabs>
        </>
    }
}

export const Deeplink = withTranslation()(TranslatedDeeplink);
