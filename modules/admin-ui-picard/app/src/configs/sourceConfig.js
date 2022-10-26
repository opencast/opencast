/* additional metadata that user should provide for new events
 * UPLOAD, SCHEDULE_SINGLE, SCHEDULE_MULTIPLE signal in which case the additional metadata is required/should be provided
 * A metadata field has following keys:
 * - id: identifies the metadata field
 * - label: translation key for the label of the metadata field
 * - value: indicates the kind of value that the field should have (e.g. [] for multiple Values)
 * - type: indicates the type of metadata field (see metadata field provided by backend)
 * - readOnly: flag indicating if metadata field can be changed
 * - required: flag indicating if metadata field is required
 * - tabindex: tabindex of the metadata field
 */
export const sourceMetadata = {
	UPLOAD: {
		metadata: [
			{
				id: "startDate",
				label: "EVENTS.EVENTS.DETAILS.METADATA.START_DATE",
				value: new Date(Date.now()).toISOString(),
				type: "date",
				readOnly: false,
				required: false,
				tabindex: 7,
			},
		],
	},
};
