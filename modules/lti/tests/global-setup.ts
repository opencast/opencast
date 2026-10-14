import { startMockServer } from "./mock-server";

export default async function globalSetup(): Promise<() => Promise<void>> {
    const server = await startMockServer();
    return async () => {
        await new Promise<void>((resolve, reject) => {
            server.close((err) => (err ? reject(err) : resolve()));
        });
    };
}
