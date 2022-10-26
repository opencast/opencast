import winston from "winston";
import BrowserConsole from "winston-transport-browserconsole";

export const logger = winston.createLogger({
	format: winston.format.json(),
	transports: [
		new BrowserConsole({
			level: "info",
		}),
	],
});
