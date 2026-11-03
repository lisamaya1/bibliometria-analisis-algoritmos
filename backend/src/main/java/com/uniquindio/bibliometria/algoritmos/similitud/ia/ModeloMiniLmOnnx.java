package com.uniquindio.bibliometria.algoritmos.similitud.ia;

import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;

/** Una sesión CPU y un tokenizador locales compartidos; ninguna descarga en requests. */
@Component
public class ModeloMiniLmOnnx implements ModeloMiniLm, AutoCloseable {
    public static final String MODELO = "sentence-transformers/all-MiniLM-L6-v2";
    public static final String REVISION = "1110a243fdf4706b3f48f1d95db1a4f5529b4d41";
    private static final String SHA_MODELO = "6fd5d72fe4589f189f8ebc006442dbb529bb7ce38f8082112682524616046452";
    private static final Logger LOG = LoggerFactory.getLogger(ModeloMiniLmOnnx.class);
    private HuggingFaceTokenizer tokenizador;
    private OrtEnvironment entorno;
    private OrtSession sesion;
    private long cls;
    private long sep;
    private String problema;

    public ModeloMiniLmOnnx(@Value("${minilm.model-dir:}") String directorio) {
        if (directorio == null || directorio.isBlank()) {
            problema = "Configura MINILM_MODEL_DIR y reinicia el backend para habilitar MiniLM.";
            return;
        }
        long inicio = System.nanoTime();
        try {
            Path ruta = Path.of(directorio).toAbsolutePath().normalize();
            var mapper = new JsonMapper();
            var manifiesto = mapper.readTree(Files.readString(ruta.resolve("manifest.json")));
            if (!REVISION.equals(manifiesto.get("revision").asString()) || !MODELO.equals(manifiesto.get("modelo").asString()))
                throw new IllegalArgumentException("La revisión del modelo no es la prevista.");
            boolean modeloVerificado = false;
            boolean tokenizadorVerificado = false;
            for (var archivo : manifiesto.get("archivos")) {
                String nombre = archivo.get("archivo").asString();
                Path fichero = ruta.resolve(nombre).normalize();
                if (!fichero.startsWith(ruta)) throw new IllegalArgumentException("Ruta inválida en manifiesto.");
                var digest = MessageDigest.getInstance("SHA-256");
                try (var entrada = Files.newInputStream(fichero)) {
                    byte[] bloque = new byte[65536];
                    int leidos;
                    while ((leidos = entrada.read(bloque)) != -1) digest.update(bloque, 0, leidos);
                }
                String sha = HexFormat.of().formatHex(digest.digest());
                if (!sha.equals(archivo.get("sha256").asString())) throw new IllegalArgumentException("SHA-256 incorrecto: " + nombre);
                if (nombre.equals("onnx/model.onnx")) {
                    if (!SHA_MODELO.equals(sha)) throw new IllegalArgumentException("El modelo no coincide con el oficial.");
                    modeloVerificado = true;
                }
                if (nombre.equals("tokenizer.json")) tokenizadorVerificado = true;
            }
            if (!modeloVerificado || !tokenizadorVerificado) throw new IllegalArgumentException("Manifiesto incompleto.");
            var config = mapper.readTree(Files.readString(ruta.resolve("config.json")));
            var sentencia = mapper.readTree(Files.readString(ruta.resolve("sentence_bert_config.json")));
            if (config.get("hidden_size").asInt() != dimension() || sentencia.get("max_seq_length").asInt() != limiteEntrada())
                throw new IllegalArgumentException("Dimensión o límite de MiniLM inesperados.");
            var vocabulario = mapper.readTree(Files.readString(ruta.resolve("tokenizer.json"))).get("model").get("vocab");
            cls = vocabulario.get("[CLS]").asLong();
            sep = vocabulario.get("[SEP]").asLong();
            // Impide descargas y telemetría de DJL durante la inferencia.
            System.setProperty("ai.djl.offline", "true");
            System.setProperty("OPT_OUT_TRACKING", "true");
            tokenizador = HuggingFaceTokenizer.newInstance(ruta.resolve("tokenizer.json"),
                    Map.of("addSpecialTokens", "false", "truncation", "false", "padding", "false"));
            entorno = OrtEnvironment.getEnvironment();
            try (var opciones = new OrtSession.SessionOptions()) {
                opciones.setIntraOpNumThreads(2);
                opciones.setInterOpNumThreads(1);
                sesion = entorno.createSession(ruta.resolve("onnx/model.onnx").toString(), opciones);
            }
            if (!sesion.getInputNames().containsAll(Arrays.asList("input_ids", "attention_mask")) ||
                    !sesion.getOutputNames().contains("last_hidden_state")) throw new IllegalArgumentException("Contrato ONNX inesperado.");
            LOG.info("MiniLM local cargado una vez; revisión {}; carga y verificación: {} ms", REVISION, (System.nanoTime() - inicio) / 1_000_000);
        } catch (Exception | LinkageError error) {
            close();
            problema = "MiniLM no está disponible. Revisa MINILM_MODEL_DIR, los archivos y las bibliotecas nativas; después reinicia el backend.";
            LOG.warn("No se pudo cargar MiniLM: {}", error.toString());
        }
    }

    private void comprobarDisponible() {
        if (sesion == null || tokenizador == null) throw new IllegalStateException(problema == null ? "MiniLM está cerrado." : problema);
    }

    @Override public synchronized long[] tokenizar(String texto) {
        comprobarDisponible();
        return tokenizador.encode(texto, false, false).getIds();
    }

    @Override public synchronized Salida ejecutar(long[] contenido) {
        comprobarDisponible();
        if (contenido.length == 0 || contenido.length > limiteEntrada() - 2) throw new IllegalArgumentException("Fragmento MiniLM fuera del límite.");
        int longitud = contenido.length + 2;
        long[][] ids = new long[1][longitud];
        ids[0][0] = cls;
        System.arraycopy(contenido, 0, ids[0], 1, contenido.length);
        ids[0][longitud - 1] = sep;
        long[][] mascara = new long[1][longitud];
        Arrays.fill(mascara[0], 1);
        try (var entrada = OnnxTensor.createTensor(entorno, ids);
             var atencion = OnnxTensor.createTensor(entorno, mascara);
             var tipos = OnnxTensor.createTensor(entorno, new long[1][longitud])) {
            var entradas = new HashMap<String, OnnxTensor>();
            entradas.put("input_ids", entrada);
            entradas.put("attention_mask", atencion);
            if (sesion.getInputNames().contains("token_type_ids")) entradas.put("token_type_ids", tipos);
            try (var respuesta = sesion.run(entradas)) {
                float[][][] estados = (float[][][]) respuesta.get("last_hidden_state").orElseThrow().getValue();
                return new Salida(estados[0], mascara[0]);
            }
        } catch (OrtException error) {
            throw new IllegalStateException("No se pudo ejecutar el modelo MiniLM local.", error);
        }
    }

    @Override public int dimension() { return 384; }
    @Override public int limiteEntrada() { return 256; }

    @Override @PreDestroy public synchronized void close() {
        if (sesion != null) {
            try { sesion.close(); } catch (OrtException error) { LOG.warn("Error al cerrar MiniLM: {}", error.toString()); }
            sesion = null;
        }
        if (tokenizador != null) { tokenizador.close(); tokenizador = null; }
    }
}
