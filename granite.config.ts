import { defineConfig } from "@apps-in-toss/web-framework/config";
export default defineConfig({
  appName: "umbrella-app",
  brand: {
    displayName: "우산챙겨",
    primaryColor: "#3182F6",
    icon: "https://static.toss.im/appsintoss/55979/2e238c8e-01f0-4537-9a09-8312ec06c89e.png",
  },
  web: {
    host: "localhost",
    port: 5173,
    commands: {
      dev: "vite dev",
      build: "vite build",
    },
  },
  permissions: [],
  outdir: "dist",
});