import { ActionMeta, ValueType } from "react-select/src/types"; // tslint:disable-line no-submodule-imports
import Select from "react-select";
import { Loading } from "./Loading";
import Helmet from "react-helmet";
import React from "react";
import { withTranslation, WithTranslation } from "react-i18next";
import {
    uploadFile,
    copyEventToSeries,
    getEventMetadata,
    EventMetadataContainer,
    findField,
    findFieldCollection,
    collectionToPairs
} from "../OpencastRest";
import { parsedQueryString } from "../utils";
import { EditForm } from "./EditForm";
import { JobList } from "./JobList";

interface OptionType {
    value: string;
    label: string;
}

interface MetadataResult {
    readonly initial: EventMetadataContainer;
    readonly edited: EventMetadataContainer;
    readonly seriesId: string;
}

interface UploadState {
    readonly eventId?: string;
    readonly uploadState: "success" | "error" | "pending" | "none";
    readonly metadata: MetadataResult | "error" | undefined;
    readonly presenterFile?: Blob;
    readonly captionFile?: Blob;
    readonly copyState: "success" | "error" | "pending" | "none";
    readonly copySeries?: OptionType;
    readonly refreshTimerId?: ReturnType<typeof setTimeout>;
}

function isMetadata(
    metadata: MetadataResult | "error" | undefined): metadata is MetadataResult {
    return metadata !== undefined && typeof metadata !== "string";
}

interface UploadProps extends WithTranslation {
}

class TranslatedUpload extends React.Component<UploadProps, UploadState> {
    constructor(props: UploadProps) {
        super(props);
        const qs = parsedQueryString();
        this.state = {
            eventId: typeof qs.episode_id === "string" ? qs.episode_id : undefined,
            uploadState: "none",
            copyState: "none",
            metadata: undefined,
        };
    }

    resolveSeries(metadata: EventMetadataContainer): string | undefined {
        const qs = parsedQueryString();
        // If we have a series ID in the query string, look no further.
        if (typeof qs.series === "string")
            return qs.series;
        // Do we have a series ID in the event metadata?
        const seriesField = findField("isPartOf", metadata);
        if (seriesField === undefined)
            return;
        const fieldValue = seriesField.value;
        if (typeof fieldValue === "string" && fieldValue !== "")
            return fieldValue;
        // Otherwise, assume and resolve a series name instead of an ID.
        if (typeof qs.series_name !== "string" || seriesField.collection === undefined)
            return;
        const pairs = collectionToPairs(seriesField.collection);
        return pairs
            .filter(([k, _]) => k === qs.series_name)
            .map(([_, v]) => v)
            .pop();
    }

    componentDidMount() {
        getEventMetadata(this.state.eventId).then((metadataCollection) => {
            if (metadataCollection.length > 0) {
                const metadata = metadataCollection[0];
                const seriesId = this.resolveSeries(metadata)
                if (seriesId === undefined) {
                    this.setState({
                        ...this.state,
                        metadata: "error"
                    });
                } else {
                    this.setState({
                        ...this.state,
                        metadata: {
                            initial: metadata,
                            edited: metadata,
                            seriesId: seriesId
                        },
                    });

                    const timerMillis = 1000;
                    this.setState({
                        ...this.state,
                        refreshTimerId: setInterval(this.refreshTimer.bind(this), timerMillis),
                    });
                }
            }
        }).catch((_) => this.setState({
            ...this.state,
            metadata: "error"
        }));
    }

    componentWillUnmount() {
        if (this.state.refreshTimerId !== undefined)
            clearInterval(this.state.refreshTimerId);
    }

    refreshTimer() {
        getEventMetadata(this.state.eventId).then((metadataCollection) => {
            if (this.state.metadata === undefined || this.state.metadata === "error" || metadataCollection.length === 0)
                return;

            const metadata = metadataCollection[0];
            const lockedBefore = this.state.metadata.edited.locked !== undefined;
            const lockedAfter = metadata.locked !== undefined;
            if (lockedBefore !== lockedAfter) {
                this.setState({
                    ...this.state,
                    metadata: {
                        ...this.state.metadata,
                        initial: metadata,
                        edited: metadata,
                    },
                });
            }
        }).catch((_) => {
            console.log("couldn't retrieve metadata at this time, ignoring...");
        });
    }

    onSubmit() {
        if (!isMetadata(this.state.metadata))
            return;
        if (this.state.eventId === undefined && this.state.presenterFile === undefined)
            return;
        this.setState({
            ...this.state,
            uploadState: "pending"
        });
        console.log("onSubmit, seriesId: " + this.state.metadata.seriesId);
        uploadFile(
            this.state.metadata.edited,
            this.state.metadata.seriesId,
            this.state.eventId,
            this.state.presenterFile,
            this.state.captionFile,
        ).then((_) => {
            if (!isMetadata(this.state.metadata))
                return;
            this.setState({
                ...this.state,
                uploadState: this.state.eventId === undefined ? "success" : "none"
            });
            if (this.state.eventId === undefined) {
                this.setState({
                    ...this.state,
                    metadata: {
                        ...this.state.metadata,
                        edited: this.state.metadata.initial
                    }
                });
            }
        }).catch((_) => {
            this.setState({
                ...this.state,
                uploadState: "error"
            });
        });
    }

    onCaptionFileChange(newFile: Blob) {
        this.setState({
            ...this.state,
            captionFile: newFile
        });
    }

    onPresenterFileChange(newFile: Blob) {
        this.setState({
            ...this.state,
            presenterFile: newFile
        });
    }

    onDataChange(newData: EventMetadataContainer) {
        if (!isMetadata(this.state.metadata))
            return;
        this.setState({
            ...this.state,
            metadata: {
                ...this.state.metadata,
                edited: newData
            }
        });
    }

    seriesItems(): OptionType[] {
        const metadata = this.state.metadata;
        if (!isMetadata(metadata))
            return [];
        const seriesCollection = findFieldCollection("isPartOf", metadata.edited);
        if (seriesCollection === undefined)
            return [];
        const pairs = collectionToPairs(seriesCollection);
        return pairs
            .filter(([_, v]) => v !== metadata.seriesId)
            .map(([k, v]) => ({ value: v, label: k }));
    }

    onMoveToSeries(_: any) {
        const eventId = this.state.eventId;
        if (eventId === undefined || this.state.copySeries === undefined)
            return;
        copyEventToSeries(eventId, this.state.copySeries.value).then((_) => {
            this.setState({
                ...this.state,
                copyState: "success"
            });
        }).catch((_: any) => {
            this.setState({
                ...this.state,
                copyState: "error"
            });
        });
    }

    onChangeCopyTarget(v: OptionType) {
        this.setState({
            ...this.state,
            copySeries: v
        });
    }

    render() {
        if (this.state.metadata === undefined)
            return <Loading t={this.props.t} />;
        if (this.state.metadata === "error")
            return <div>{this.props.t("LTI.ERROR_LOADING_METADATA")}</div>;
        const lockedString = this.state.metadata.edited.locked !== undefined ? "LTI.EVENT_LOCKED" : undefined;
        return <>
            <Helmet>
                <title>{this.props.t("LTI." + (this.state.eventId === undefined ? "UPLOAD_TITLE" : "EDIT_TITLE"))}</title>
            </Helmet>
            <h2>{this.props.t("LTI." + (this.state.eventId === undefined ? "NEW_UPLOAD" : "EDIT_UPLOAD"))}</h2>
            {lockedString !== undefined && <div className="alert alert-secondary">
                {this.props.t(lockedString)}<br />
            </div>}
            {this.state.uploadState === "success" && <div className="alert alert-success">
                {this.props.t(this.state.eventId === undefined ? "LTI.UPLOAD_SUCCESS" : "LTI.EDIT_SUCCESS")}<br />
            </div>}
            {this.state.uploadState === "error" && <div className="alert alert-danger">
                {this.props.t(this.state.eventId === undefined ? "LTI.UPLOAD_FAILURE" : "LTI.EDIT_FAILURE")}<br />
                <div className="text-muted">{this.props.t("LTI.UPLOAD_FAILURE_DESCRIPTION")}</div>
            </div>}
            {this.state.copyState === "success" && <div className="alert alert-success">
                {this.props.t("LTI.COPY_SUCCESS")}<br />
                <div className="text-muted">{this.props.t("LTI.COPY_SUCCESS_DESCRIPTION")}</div>
            </div>}
            {this.state.copyState === "error" && <div className="alert alert-danger">
                {this.props.t("LTI.COPY_FAILURE")}<br />
                <div className="text-muted">{this.props.t("LTI.COPY_FAILURE_DESCRIPTION")}</div>
            </div>}
            <EditForm
                withUpload={this.state.eventId === undefined}
                data={this.state.metadata.edited}
                onDataChange={this.onDataChange.bind(this)}
                onPresenterFileChange={this.onPresenterFileChange.bind(this)}
                onCaptionFileChange={this.onCaptionFileChange.bind(this)}
                onSubmit={this.onSubmit.bind(this)}
                hasSubmit={this.state.metadata.edited.locked === undefined}
                pending={this.state.uploadState === "pending"} />
            {this.state.eventId !== undefined && this.state.metadata.edited.locked === undefined &&
                <>
                    <h2>{this.props.t("LTI.COPY_TO_SERIES")}</h2>
                    <form>
                        <div className="form-group">
                            <Select
                                options={this.seriesItems()}
                                isSearchable={true}
                                value={this.state.copySeries}
                                onChange={(value: ValueType<OptionType>, _: ActionMeta) => this.onChangeCopyTarget(value as OptionType)}
                                placeholder={this.props.t("LTI.SELECT_COPY_TARGET")} />
                        </div>
                        <button
                            type="button"
                            className="btn btn-primary"
                            onClick={this.onMoveToSeries.bind(this)}
                            disabled={this.state.copyState === "pending" || this.state.copySeries === undefined}>
                            {this.props.t(this.state.copyState === "pending" ? "LTI.COPY_IN_PROGRESS" : "LTI.COPY")}
                        </button>
                    </form>
                </>
            }
            <h2>{this.props.t("LTI.CURRENT_JOBS")}</h2>
            <JobList seriesId={this.state.metadata.seriesId} />
        </>;
    }
}

export const Upload = withTranslation()(TranslatedUpload);
