import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(() => {
  return {
    base: process.env.PUBLIC_URL || "",
    build: {
      outDir: 'build',
    },
    plugins: [react()],
    server: {
      open: true,
      port: Number(process.env.PORT) || 3000,
    },
  };
});
