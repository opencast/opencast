import { type Connect, defineConfig, type ViteDevServer } from 'vite';
import tailwindcss from '@tailwindcss/vite'
import express from 'express';
import { resolve } from 'path';

const ocProxyServer = process.env.OC_SERVER_URL ?? process.env.OC_PRESENTATION_URL ?? 'https://stable.opencast.org';
const baseURL = process.env.OC_PAELLA8_BASE_URL || '/paella8/ui';

const app = express();
app.use('/ui/config/paella8', express.static('../../etc/ui-config/mh_default_org/paella8/'));
app.use('/play/:id', (req, res) => {
  const id = req.params.id;
  res.redirect(`/paella8/ui/watch.html?id=${id}`);
});


function expressPlugin() {
  return {
    name: 'express-plugin',
    configureServer(server: ViteDevServer) {
      server.middlewares.use(app as Connect.NextHandleFunction);
    }
  };
}

export default defineConfig({
  base: baseURL,
  build: {
    outDir: 'target/paella-build',
    sourcemap: true,
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      input: {
        index: resolve(__dirname, 'index.html'),
        watch: resolve(__dirname, 'watch.html'),
        auth: resolve(__dirname, 'auth.html'),
      },
      output: {
        manualChunks: {
          // 'paella-core': ['@asicupv/paella-core'],
          'paella-opencast-core': ['@asicupv/paella-opencast-core', '@asicupv/paella-core'],          
          'paella-plugins': [
            '@asicupv/paella-basic-plugins',
            '@asicupv/paella-slide-plugins',
            '@asicupv/paella-zoom-plugin',
            '@asicupv/paella-webgl-plugins',
            '@asicupv/paella-video-plugins',
            '@asicupv/paella-extra-plugins',
            '@asicupv/paella-opencast-plugins'
          ],
        }
      }
    },    
  },  
  server: {
    port: 7070,
    proxy: {
      '/search': ocProxyServer,
      '/series': ocProxyServer,
      '/info': ocProxyServer,
      '/engage': ocProxyServer,
      '/editor-ui': ocProxyServer,
      // '/play': ocProxyServer,
      // '/ui': ocServer,
      // '/paella7/ui/default_theme': ocServer
    }
  },
  plugins: [    
    tailwindcss(),
    expressPlugin()
  ],
  define: {    
    "import.meta.env.OC_PRESENTATION_URL": JSON.stringify(process.env.OC_PRESENTATION_URL || '/'),
    "import.meta.env.OC_PAELLA8_BASE_URL": JSON.stringify(baseURL),    
    "import.meta.env.CONFIG_FOLDER": JSON.stringify(process.env.CONFIG_FOLDER || '/ui/config/paella8/'),    
    "import.meta.env.OC_PAELLA8_URL_TEMPLATE": JSON.stringify(process.env.OC_PAELLA8_URL_TEMPLATE),
    "import.meta.env.USE_OC_SERVER_FROM_URL": JSON.stringify(process.env.USE_OC_SERVER_FROM_URL || 'false'),
  }
});