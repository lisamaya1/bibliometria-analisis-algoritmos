# Proyecto de Análisis de Algoritmos

Autora: Elizabeth Maya.

## Organización

```text
backend/        Spring Boot, pruebas, datos del corpus y scripts
frontend/       React y Vite
documentacion/  Documentación técnica de los algoritmos
```

El backend y el frontend se compilan y ejecutan por separado. Se requieren Java 21, Maven y Node.js con npm.

## Backend

Desde la raíz del repositorio, en PowerShell:

```powershell
cd backend
$env:MINILM_MODEL_DIR = 'C:\Users\HP\modelos\all-MiniLM-L6-v2'
$env:BGE_MODEL_DIR = 'C:\Users\HP\modelos\bge-small-en-v1.5'
mvn spring-boot:run
```

Las rutas de los modelos son externas y deben ajustarse a cada equipo. Su preparación se explica en `documentacion/minilm.md` y `documentacion/bge.md`. El servidor escucha en `http://localhost:8080`.

Ejecutar Maven desde `backend` conserva las rutas relativas del corpus: `datos/procesados/corpus_procesado.json` y `datos/pdfs_originales`. Los scripts están en `backend/scripts`.

## Frontend

En otra terminal, desde la raíz del repositorio:

```powershell
cd frontend
npm install
npm run dev
```

Abrir la dirección que indique Vite, normalmente `http://localhost:5173`. Durante el desarrollo, Vite reenvía `/api` al backend en el puerto 8080.

## Verificación y compilación

En `backend`, con los modelos configurados: `mvn test` y `mvn package`.

En `frontend`: `npm run build`. Los archivos estáticos se generan en `frontend/dist`.

Para desplegar por separado, el servidor que publique el frontend debe reenviar `/api` al backend. El proxy de Vite solo corresponde al servidor de desarrollo; la publicación requiere configurar ese enrutamiento en el alojamiento elegido.
