import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  root: "src/main/webapp",
  base: "./",
  plugins: [react()],
  build: {
    outDir: "../../../target/classes",
    emptyOutDir: true,
    assetsDir: "",
  },
  server: {
    proxy: {
      "/graphql": "http://localhost:8080",
    },
  },
});
