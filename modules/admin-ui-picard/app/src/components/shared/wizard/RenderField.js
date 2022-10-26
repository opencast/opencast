import React, { useState } from "react";
import Select from "react-select";
import { useTranslation } from "react-i18next";
import { DateTimePicker } from "@material-ui/pickers";
import { createMuiTheme, ThemeProvider } from "@material-ui/core";
import cn from "classnames";
import { useClickOutsideField } from "../../../hooks/wizardHooks";
import { isJson } from "../../../utils/utils";
import { getMetadataCollectionFieldName } from "../../../utils/resourceUtils";
import DropDown from "../DropDown";

const childRef = React.createRef();
/**
 * This component renders an editable field for single values depending on the type of the corresponding metadata
 */
const RenderField = ({
	field,
	metadataField,
	form,
	showCheck = false,
	isFirstField = false,
}) => {
	const { t } = useTranslation();

	// Indicator if currently edit mode is activated
	const [editMode, setEditMode] = useClickOutsideField(childRef, isFirstField);

	// Handle key down event and check if pressed key leads to leaving edit mode
	const handleKeyDown = (event, type) => {
		const { key } = event;
		// keys pressable for leaving edit mode
		const keys = ["Escape", "Tab", "Enter"];

		if (type !== "textarea" && keys.indexOf(key) > -1) {
			setEditMode(false);
		}
	};

	return (
		// Render editable field depending on type of metadata field
		// (types: see metadata.json retrieved from backend)
		<>
			{metadataField.type === "time" && (
				<EditableSingleValueTime
					field={field}
					text={field.value}
					editMode={editMode}
					setEditMode={setEditMode}
					form={form}
					showCheck={showCheck}
				/>
			)}
			{metadataField.type === "text" &&
				!!metadataField.collection &&
				metadataField.collection.length > 0 && (
					<EditableSingleSelect
						metadataField={metadataField}
						field={field}
						form={form}
						text={
							isJson(getMetadataCollectionFieldName(metadataField, field))
								? t(
										JSON.parse(
											getMetadataCollectionFieldName(metadataField, field)
										).label
								  )
								: t(getMetadataCollectionFieldName(metadataField, field))
						}
						editMode={editMode}
						setEditMode={setEditMode}
						showCheck={showCheck}
						handleKeyDown={handleKeyDown}
					/>
				)}
			{metadataField.type === "ordered_text" && (
				<EditableSingleSelect
					metadataField={metadataField}
					field={field}
					form={form}
					text={field.value}
					editMode={editMode}
					setEditMode={setEditMode}
					showCheck={showCheck}
					handleKeyDown={handleKeyDown}
				/>
			)}
			{metadataField.type === "text" &&
				!(
					!!metadataField.collection && metadataField.collection.length !== 0
				) && (
					<EditableSingleValue
						field={field}
						form={form}
						text={field.value}
						editMode={editMode}
						setEditMode={setEditMode}
						isFirst={isFirstField}
						showCheck={showCheck}
						handleKeyDown={handleKeyDown}
					/>
				)}
			{metadataField.type === "text_long" && (
				<EditableSingleValueTextArea
					field={field}
					text={field.value}
					form={form}
					editMode={editMode}
					setEditMode={setEditMode}
					isFirst={isFirstField}
					showCheck={showCheck}
					handleKeyDown={handleKeyDown}
				/>
			)}
			{metadataField.type === "date" && (
				<EditableDateValue
					field={field}
					text={field.value}
					form={form}
					editMode={editMode}
					setEditMode={setEditMode}
					showCheck={showCheck}
				/>
			)}
			{metadataField.type === "boolean" && (
				<EditableBooleanValue
					field={field}
					form={form}
					showCheck={showCheck}
					handleKeyDown={handleKeyDown}
				/>
			)}
		</>
	);
};

// Renders editable field for a boolean value
const EditableBooleanValue = ({
	field,
	handleKeyDown,
	form: { initialValues },
	showCheck,
}) => {
	return (
		<div onKeyDown={(e) => handleKeyDown(e, "input")} ref={childRef}>
			<input type="checkbox" checked={field.value} {...field} />
			<i className="edit fa fa-pencil-square" />
			{showCheck && (
				<i
					className={cn("saved fa fa-check", {
						active: initialValues[field.name] !== field.value,
					})}
				/>
			)}
		</div>
	);
};

// Renders editable field for a data value
const EditableDateValue = ({
	field,
	text,
	form: { setFieldValue, initialValues },
	editMode,
	setEditMode,
	showCheck,
}) => {
	const { t } = useTranslation();

	const theme = createMuiTheme({
		props: {
			MuiDialog: {
				style: {
					zIndex: "2147483550",
				},
			},
		},
	});

	return editMode ? (
		<div>
			<ThemeProvider theme={theme}>
				<DateTimePicker
					name={field.name}
					value={field.value}
					onChange={(value) => setFieldValue(field.name, value)}
					onClose={() => setEditMode(false)}
					fullWidth
					format="MM/dd/yyyy"
				/>
			</ThemeProvider>
		</div>
	) : (
		<div onClick={() => setEditMode(true)}>
			<span className="editable preserve-newlines">
				{t("dateFormats.dateTime.short", { dateTime: new Date(text) }) || ""}
			</span>
			<i className="edit fa fa-pencil-square" />
			{showCheck && (
				<i
					className={cn("saved fa fa-check", {
						active: initialValues[field.name] !== field.value,
					})}
				/>
			)}
		</div>
	);
};

// renders editable field for selecting value via dropdown
const EditableSingleSelect = ({
	field,
	metadataField,
	text,
	editMode,
	setEditMode,
	handleKeyDown,
	form: { setFieldValue, initialValues },
	showCheck,
}) => {
	const { t } = useTranslation();

	return editMode ? (
		<div
			onBlur={() => setEditMode(false)}
			onKeyDown={(e) => handleKeyDown(e, "select")}
			ref={childRef}
		>
			<DropDown
				value={field.value}
				text={text}
				options={metadataField.collection}
				type={metadataField.id}
				required={metadataField.required}
				handleChange={(element) => setFieldValue(field.name, element.value)}
				placeholder={`-- ${t("SELECT_NO_OPTION_SELECTED")} --`}
				tabIndex={"10"}
				autoFocus={true}
				defaultOpen={true}
			/>
		</div>
	) : (
		<div onClick={() => setEditMode(true)}>
			<span className="editable preserve-newlines">
				{text || t("SELECT_NO_OPTION_SELECTED")}
			</span>
			<i className="edit fa fa-pencil-square" />
			{showCheck && (
				<i
					className={cn("saved fa fa-check", {
						active: initialValues[field.name] !== field.value,
					})}
				/>
			)}
		</div>
	);
};

// Renders editable text area
const EditableSingleValueTextArea = ({
	field,
	text,
	editMode,
	setEditMode,
	handleKeyDown,
	form: { initialValues },
	showCheck,
	isFirst,
}) => {
	return editMode ? (
		<div
			onBlur={() => setEditMode(false)}
			onKeyDown={(e) => handleKeyDown(e, "textarea")}
			ref={childRef}
		>
			<textarea
				{...field}
				autoFocus={isFirst}
				className="editable vertical-resize"
			/>
		</div>
	) : (
		<div onClick={() => setEditMode(true)}>
			<span className="editable preserve-newlines">{text || ""}</span>
			<i className="edit fa fa-pencil-square" />
			{showCheck && (
				<i
					className={cn("saved fa fa-check", {
						active: initialValues[field.name] !== field.value,
					})}
				/>
			)}
		</div>
	);
};

// Renders editable input for single value
const EditableSingleValue = ({
	field,
	form: { initialValues },
	text,
	editMode,
	setEditMode,
	handleKeyDown,
	showCheck,
	isFirst,
}) => {
	return editMode ? (
		<div
			onBlur={() => setEditMode(false)}
			onKeyDown={(e) => handleKeyDown(e, "input")}
			ref={childRef}
		>
			<input {...field} autoFocus={isFirst} type="text" />
		</div>
	) : (
		<div onClick={() => setEditMode(true)}>
			<span className="editable preserve-newlines">{text || ""}</span>
			<i className="edit fa fa-pencil-square" />
			{showCheck && (
				<i
					className={cn("saved fa fa-check", {
						active: initialValues[field.name] !== field.value,
					})}
				/>
			)}
		</div>
	);
};

// Renders editable field for time value
const EditableSingleValueTime = ({
	field,
	text,
	form: { setFieldValue, initialValues },
	editMode,
	setEditMode,
	showCheck,
}) => {
	const { t } = useTranslation();

	const theme = createMuiTheme({
		props: {
			MuiDialog: {
				style: {
					zIndex: "2147483550",
				},
			},
		},
	});

	return editMode ? (
		<div>
			<ThemeProvider theme={theme}>
				<DateTimePicker
					name={field.name}
					value={field.value}
					onChange={(value) => setFieldValue(field.name, value)}
					onClose={() => setEditMode(false)}
					fullWidth
				/>
			</ThemeProvider>
		</div>
	) : (
		<div onClick={() => setEditMode(true)}>
			<span className="editable preserve-newlines">
				{t("dateFormats.dateTime.short", { dateTime: new Date(text) }) || ""}
			</span>
			<i className="edit fa fa-pencil-square" />
			{showCheck && (
				<i
					className={cn("saved fa fa-check", {
						active: initialValues[field.name] !== field.value,
					})}
				/>
			)}
		</div>
	);
};

export default RenderField;
