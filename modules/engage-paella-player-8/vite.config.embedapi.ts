
import { defineConfig, normalizePath } from 'vite';
import path from 'node:path';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import { createRequire } from 'node:module'


const require = createRequire(import.meta.url)
const pkgPaellaOpencastComponentPath = path.dirname(require.resolve('@asicupv/paella-opencast-component')) 

export default defineConfig({
  resolve: { alias: { src: path.resolve('src/') } },
  build: {
    sourcemap: true,
    outDir: 'target/paella-build',
    copyPublicDir: false,
    emptyOutDir: false,
    lib: {
      entry: './src/paella-embedapi.ts',
      formats: ['iife'],
      name: 'PaellaEmbedApi',
      fileName: (format) => `paella-embedapi.js`
    }
  },
  plugins: [
    viteStaticCopy({
      targets: [
        {
          src: normalizePath(path.resolve(pkgPaellaOpencastComponentPath, 'paella-opencast-component.iife.*')),
          dest: '.'
        },
        {
          src: normalizePath(path.resolve(pkgPaellaOpencastComponentPath, 'paella-opencast-component.css')),
          dest: '.'
        }
      ]
    })
  ]
});