import { defineConfig } from 'vite';

// Builds the service worker for JWT static file auth (see `src/sw.ts`). It
// needs a fixed file name, as the page registers it by URL, and has to be a
// classic script (no `import`s), as it's not registered as module worker.
export default defineConfig({
    build: {
        sourcemap: true,
        outDir: 'target/paella-build',
        copyPublicDir: false,
        emptyOutDir: false,
        lib: {
            entry: './src/sw.ts',
            formats: ['iife'],
            name: 'OpencastJwtServiceWorker',
            fileName: () => 'sw.js',
        },
    },
});
