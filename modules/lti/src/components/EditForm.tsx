import React from "react";
import { withTranslation, WithTranslation } from "react-i18next";
import {
    EventMetadataField,
    EventMetadataContainer,
    EventMetadataCollection,
    collectionToPairs
} from "../OpencastRest";
import Select from "react-select";
import i18next from "i18next";
import { ValueType } from "react-select/src/types"; // tslint:disable-line no-submodule-imports

import CreatableSelect from "react-select/creatable";

const allowedFields = ["title", "language", "license", "creator"];

interface OptionType {
    value: string;
    label: string;
}

interface EditFormProps extends WithTranslation {
    readonly data: EventMetadataContainer;
    readonly withUpload: boolean;
    readonly onDataChange: (newData: EventMetadataContainer) => void;
    readonly onPresenterFileChange: (file: Blob) => void;
    readonly onCaptionFileChange: (file: Blob) => void;
    readonly onSubmit: () => void;
    readonly pending: boolean;
    readonly hasSubmit: boolean;
}

interface MetadataFieldProps {
    readonly field: EventMetadataField;
    readonly valueChange: (id: string, newValue: string | string[]) => void;
    readonly t: i18next.TFunction;
}

interface MetadataCollectionKey {
    label: string;
    order?: number;
    selectable?: boolean;
}

function parseMetadataCollectionKey(s: string): MetadataCollectionKey {
    if (!s.startsWith("{"))
        return { label: s };
    return JSON.parse(s);
}

function collectionToOptions(collection: EventMetadataCollection, translatable: boolean, t: i18next.TFunction): OptionType[] {
    return collectionToPairs(collection)
        .map(([k, v]) => [parseMetadataCollectionKey(k).label, v])
        .map(([k, v]) => [translatable ? t(k) : k, v])
        .map(([k, v]) => ({ value: v, label: k }));
}

function MetadataFieldReadOnly(props: MetadataFieldProps) {
    const field = props.field;
    const t = props.t;
    const style = { fontStyle: "italics" };
    if (Array.isArray(field.value))
        return <div style={style}>{field.value.join(", ")}</div>
    if (field.value === "")
        return <div style={style}>{t("LTI.NO_OPTION_SELECTED")}</div>;
    if (field.collection !== undefined) {
        const options: OptionType[] = collectionToOptions(field.collection, field.translatable, t);
        const currentValue = options.find((o: OptionType) => o.value === field.value);
        if (currentValue === undefined)
            return <div style={style}>{t("LTI.NO_OPTION_SELECTED")}</div>;
        return <div style={style}>{currentValue.label}</div>;
    }
    return <div style={style}>{field.value}</div>;
}

function MetadataFieldInner(props: MetadataFieldProps) {
    const field = props.field;
    const t = props.t;
    const valueChange = props.valueChange;
    if (field.readOnly === true) {
        return <MetadataFieldReadOnly {...props} />;
    }
    if (field.type === "text" && field.collection === undefined)
        return <input
            type="text"
            id={field.id}
            className="form-control"
            value={field.value}
            onChange={(e) => valueChange(field.id, e.currentTarget.value)} />;

    if (field.collection !== undefined && field.type === "mixed_text") {
        const options: OptionType[] = collectionToOptions(field.collection, field.translatable, t);
        return <CreatableSelect
            isMulti={true}
            isClearable={true}
            id={field.id}
            value={(field.value as string[]).map((e) => ({ value: e, label: e }))}
            options={options}
            onChange={(value: ValueType<OptionType>) =>
                valueChange(
                    field.id,
                    value === undefined || value === null || !Array.isArray(value) ? [] : (value as OptionType[]).map((v) => v.value))} />;
    }
    if (field.collection !== undefined) {
        const options: OptionType[] = collectionToOptions(field.collection, field.translatable, t);
        const currentValue = options.find((o: OptionType) => o.value === field.value);
        return <Select
            id={field.id}
            onChange={(value: ValueType<OptionType>) =>
                valueChange(
                    field.id,
                    value === undefined || value === null || Array.isArray(value) ? "" : (value as OptionType).value)}
            value={currentValue}
            options={options}
            placeholder={t("LTI.SELECT_OPTION")} />
    }
    return <div>Cannot display control of type {field.type}</div>
}

function MetadataField(props: MetadataFieldProps) {
    return <div className="form-group">
        <label htmlFor={props.field.id}>{props.t(props.field.label)}</label>
        <MetadataFieldInner {...props} />
    </div>;
}

class TranslatedEditForm extends React.Component<EditFormProps> {
    onChangePresenterFile(e: React.FormEvent<HTMLInputElement>) {
        if (e.currentTarget.files === null)
            return;
        this.props.onPresenterFileChange(e.currentTarget.files[0]);
    }

    onChangeCaptionFile(e: React.FormEvent<HTMLInputElement>) {
        if (e.currentTarget.files === null)
            return;
        this.props.onCaptionFileChange(e.currentTarget.files[0]);
    }

    fieldValueChange(id: string, newValue: string | string[]) {
        this.props.onDataChange(
            {
                ...this.props.data,
                fields: this.props.data.fields.map((field) => field.id !== id ? field : {
                    ...field,
                    value: newValue
                })
            }
        );
    }

    render() {
        return <form>
            {this.props.data.fields
                .filter((field) => allowedFields.includes(field.id))
                .map((field) => <MetadataField
                    key={field.id}
                    t={this.props.t}
                    field={field}
                    valueChange={this.fieldValueChange.bind(this)} />)}
            {this.props.withUpload === true &&
                <div className="form-group">
                    <label htmlFor="presenter">{this.props.t("LTI.PRESENTER")}</label>
                    <input type="file" className="form-control-file" onChange={this.onChangePresenterFile.bind(this)} />
                    <small className="form-text text-muted">{this.props.t("LTI.PRESENTER_DESCRIPTION")}</small>
                </div>
            }
            {this.props.withUpload === true &&
                <>
                    <div className="form-group">
                        <label htmlFor="caption">{this.props.t("LTI.CAPTION")}</label>
                        <input type="file" className="form-control-file" onChange={this.onChangeCaptionFile.bind(this)} />
                        <small className="form-text text-muted">{this.props.t("LTI.CAPTION_DESCRIPTION")}</small>
                    </div>
                    { this.props.captionFormat === "vtt" && languageOptions.length > 0 &&
                        <div className="form-group">
                            <label htmlFor="caption_language">{this.props.t("LTI.CAPTION")} {this.props.t("EVENTS.EVENTS.DETAILS.METADATA.LANGUAGE")}</label>
                            <Select
                                id="caption_language"
                                onChange={(value) => this.props.onCaptionLanguageChange((value as OptionType).value)}
                                options={languageOptions}
                                placeholder={this.props.t("LTI.SELECT_OPTION")}
                            />
                        </div>
                    }
                </>
            }
            {this.props.hasSubmit === true &&
                <button
                    type="button"
                    className="btn btn-primary"
                    onClick={(_: any) => this.props.onSubmit()}
                    disabled={this.props.pending}>
                    {this.props.withUpload === true && this.props.t(this.props.pending ? "LTI.UPLOADING" : "LTI.UPLOAD")}
                    {this.props.withUpload === false && this.props.t(this.props.pending ? "LTI.EDITING" : "LTI.EDIT")}
                </button>}
        </form>
    }
}

export const EditForm = withTranslation()(TranslatedEditForm);
