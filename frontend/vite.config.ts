import { fileURLToPath, URL } from 'node:url';
import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/public': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        bypass: (request) => {
          const acceptsHtml = request.headers.accept?.includes('text/html') ?? false;
          return request.method === 'GET' && acceptsHtml ? request.url : undefined;
        },
      },
    },
  },
});
