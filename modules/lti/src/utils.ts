import { parse as parseQuery } from "query-string";

export function parsedQueryString() {
    return parseQuery(window.location.search);
}
