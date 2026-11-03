# Modelos distribuidos en la imagen Docker

Los pesos originales no se modifican ni se guardan en Git. Se descargan durante
el build desde las revisiones fijadas y se verifican antes de generar la imagen.
Las operaciones posteriores de pooling, fragmentación y comparación corresponden
al código existente del proyecto.

## MiniLM

- Modelo: `sentence-transformers/all-MiniLM-L6-v2`.
- Autores y procedencia: Sentence Transformers y los autores citados en su model card.
- Revisión: `1110a243fdf4706b3f48f1d95db1a4f5529b4d41`.
- Origen: https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2/tree/1110a243fdf4706b3f48f1d95db1a4f5529b4d41
- Licencia declarada por el modelo: Apache-2.0.
- Texto íntegro de la licencia: `licenses/Apache-2.0.txt`, obtenido de
  https://www.apache.org/licenses/LICENSE-2.0.txt.

## BGE

- Modelo: `BAAI/bge-small-en-v1.5`.
- Procedencia: Beijing Academy of Artificial Intelligence (BAAI), FlagEmbedding.
- Revisión: `5c38ec7c405ec4b44b94cc5a9bb96e735b38267a`.
- Origen: https://huggingface.co/BAAI/bge-small-en-v1.5/tree/5c38ec7c405ec4b44b94cc5a9bb96e735b38267a
- Licencia declarada por el modelo: MIT.
- Texto íntegro: `licenses/MIT-BGE.txt`, con su aviso de copyright original,
  obtenido de https://raw.githubusercontent.com/FlagOpen/FlagEmbedding/897baa67c0ffddbfccf4e6776ff55310d67809d4/LICENSE.

Cada carpeta de modelo de la imagen conserva su `README.md` original, su `LICENSE`
y un `manifest.json` con SHA-256 y tamaños. Los `.sha256` y manifiestos de esta
carpeta fijan los archivos esperados: no aceptan automáticamente hashes nuevos.
Se contrastaron los ocho archivos auxiliares de cada modelo con su revisión
oficial; los SHA-256 ONNX coinciden con la documentación y los metadatos LFS
oficiales. Las model cards se conservan completas para mantener citas y atribuciones.
