import { createServer, IncomingMessage, Server, ServerResponse } from "http";
import { readFile } from "fs/promises";
import { join } from "path";

// Paths served directly from mock-server-api-data/*.json fixtures. Everything
// else is proxied through to the real Vite dev server, so the app (and its
// static assets) can be loaded from this same origin.
const MOCK_FILES = [
    "lti",
    "lti-service-gui/existing-event/metadata",
    "lti-service-gui/jobs",
    "lti-service-gui/new/metadata",
    "search/episode.json",
];

const MOCK_DATA_ROOT = join(__dirname, "..", "mock-server-api-data");

async function serveMockFile(path: string, res: ServerResponse): Promise<void> {
    const data = await readFile(join(MOCK_DATA_ROOT, path));
    res.writeHead(200, {
        "Content-Type": "application/json",
        "Content-Length": data.length,
    });
    res.end(data);
}

async function proxyToDevServer(req: IncomingMessage, res: ServerResponse, proxyTarget: string): Promise<void> {
    const upstream = await fetch(proxyTarget + req.url, { method: req.method });
    const body = Buffer.from(await upstream.arrayBuffer());
    const headers: Record<string, string> = {};
    upstream.headers.forEach((value, key) => {
        if (!["transfer-encoding", "connection"].includes(key.toLowerCase()))
            headers[key] = value;
    });
    res.writeHead(upstream.status, headers);
    res.end(body);
}

export interface MockServerOptions {
    readonly port?: number;
    readonly host?: string;
    readonly proxyTarget?: string;
}

/**
 * Mocks the LTI REST endpoints used by the app from mock-server-api-data/*,
 * proxying every other request through to the real Vite dev server. This is
 * the TS equivalent of the mock/proxy server from the old selenium-tests script.
 */
export function startMockServer(options: MockServerOptions = {}): Promise<Server> {
    const port = options.port ?? 7878;
    const host = options.host ?? "127.0.0.1";
    const proxyTarget = options.proxyTarget ?? "http://localhost:3000";

    const server = createServer((req, res) => {
        const path = (req.url ?? "/").slice(1).split("?")[0];
        const handled = MOCK_FILES.includes(path)
            ? serveMockFile(path, res)
            : proxyToDevServer(req, res, proxyTarget);
        handled.catch((err: unknown) => {
            res.writeHead(500);
            res.end(String(err));
        });
    });

    return new Promise((resolve) => {
        server.listen(port, host, () => resolve(server));
    });
}
