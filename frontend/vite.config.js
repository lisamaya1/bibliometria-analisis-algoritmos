import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    // Proxy: reenvía /api/* al backend Spring Boot en localhost:8080
    // Así el frontend llama a /api/bucket-sort sin problemas de CORS
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
