package com.uniquindio.bibliometria.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.uniquindio.bibliometria.modelo.ArticuloDocumental;

public final class JsonUtil {

    private JsonUtil() {
    }

    /**
     * Guarda una lista de artículos en un archivo JSON en disco con formato legible (indentado).
     */
    public static void guardarArticulosEnJson(List<ArticuloDocumental> articulos, Path destino) throws IOException {
        String json = serializarArticulos(articulos);
        if (destino.getParent() != null && !Files.exists(destino.getParent())) {
            Files.createDirectories(destino.getParent());
        }
        Files.writeString(destino, json, StandardCharsets.UTF_8);
    }

    /**
     * Carga y deserializa una lista de artículos desde un archivo JSON.
     */
    public static List<ArticuloDocumental> cargarArticulosDesdeJson(Path origen) throws IOException {
        String json = Files.readString(origen, StandardCharsets.UTF_8);
        return deserializarArticulos(json);
    }

    /**
     * Serializa la lista de artículos a una cadena en formato JSON legible.
     */
    public static String serializarArticulos(List<ArticuloDocumental> articulos) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < articulos.size(); i++) {
            ArticuloDocumental a = articulos.get(i);
            sb.append("  {\n");
            sb.append("    \"id\": ").append(a.id()).append(",\n");
            sb.append("    \"archivo\": \"").append(escapar(a.archivo())).append("\",\n");
            sb.append("    \"titulo\": \"").append(escapar(a.titulo())).append("\",\n");

            // Autores
            sb.append("    \"autores\": [\n");
            for (int j = 0; j < a.autores().size(); j++) {
                sb.append("      \"").append(escapar(a.autores().get(j))).append("\"");
                if (j < a.autores().size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("    ],\n");

            sb.append("    \"abstractOriginal\": \"").append(escapar(a.abstractOriginal())).append("\",\n");
            sb.append("    \"abstractProcesado\": \"").append(escapar(a.abstractProcesado())).append("\",\n");

            // Tokens
            sb.append("    \"tokens\": [");
            for (int k = 0; k < a.tokens().size(); k++) {
                if (k % 12 == 0) sb.append("\n      ");
                sb.append("\"").append(escapar(a.tokens().get(k))).append("\"");
                if (k < a.tokens().size() - 1) sb.append(", ");
            }
            sb.append("\n    ],\n");

            sb.append("    \"totalTokens\": ").append(a.totalTokens()).append("\n");
            sb.append("  }");
            if (i < articulos.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]\n");
        return sb.toString();
    }

    /**
     * Deserializa una cadena JSON que contiene una lista de artículos.
     */
    public static List<ArticuloDocumental> deserializarArticulos(String json) {
        List<ArticuloDocumental> lista = new ArrayList<>();
        SimpleJsonParser parser = new SimpleJsonParser(json);
        Object root = parser.parse();
        if (!(root instanceof List<?> array)) {
            throw new IllegalArgumentException("El JSON raíz debe ser un arreglo de objetos");
        }

        for (Object item : array) {
            if (!(item instanceof Map<?, ?> rawMap)) continue;
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;

            Object idVal = map.get("id");
            int id = idVal instanceof Number n ? n.intValue() : (lista.size() + 1);

            String archivo = map.get("archivo") instanceof String s ? s : "";
            String titulo = map.get("titulo") instanceof String s ? s : "";

            List<String> autores = new ArrayList<>();
            Object autoresObj = map.get("autores");
            if (autoresObj instanceof List<?> list) {
                for (Object o : list) autores.add(String.valueOf(o));
            }

            Object origObj = map.containsKey("abstractOriginal") ? map.get("abstractOriginal") : map.get("resumenOriginal");
            String abstractOriginal = origObj instanceof String s ? s : "";

            Object procObj = map.containsKey("abstractProcesado") ? map.get("abstractProcesado") : map.get("resumenProcesado");
            String abstractProcesado = procObj instanceof String s ? s : "";

            List<String> tokens = new ArrayList<>();
            Object tokensObj = map.containsKey("tokens") ? map.get("tokens") : map.get("palabras");
            if (tokensObj instanceof List<?> list) {
                for (Object o : list) tokens.add(String.valueOf(o));
            }

            Object totalVal = map.get("totalTokens");
            int totalTokens = totalVal instanceof Number n ? n.intValue() : tokens.size();

            lista.add(new ArticuloDocumental(
                    id, archivo, titulo, autores,
                    abstractOriginal, abstractProcesado, tokens, totalTokens
            ));
        }

        return lista;
    }

    private static String escapar(String texto) {
        if (texto == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < ' ') {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Parser JSON básico y recursivo para Mapas, Listas, Strings y Números.
     */
    private static class SimpleJsonParser {
        private final String src;
        private int pos = 0;

        SimpleJsonParser(String src) {
            this.src = src;
        }

        Object parse() {
            skipWhitespace();
            if (pos >= src.length()) return null;
            char c = src.charAt(pos);
            if (c == '[') return parseArray();
            if (c == '{') return parseObject();
            if (c == '"') return parseString();
            if (Character.isDigit(c) || c == '-') return parseNumber();
            if (src.startsWith("true", pos)) { pos += 4; return true; }
            if (src.startsWith("false", pos)) { pos += 5; return false; }
            if (src.startsWith("null", pos)) { pos += 4; return null; }
            throw new IllegalArgumentException("Carácter inesperado en JSON pos " + pos + ": " + c);
        }

        private List<Object> parseArray() {
            List<Object> list = new ArrayList<>();
            pos++; // Salta '['
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == ']') {
                pos++;
                return list;
            }
            while (pos < src.length()) {
                list.add(parse());
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                } else if (pos < src.length() && src.charAt(pos) == ']') {
                    pos++;
                    break;
                }
            }
            return list;
        }

        private Map<String, Object> parseObject() {
            Map<String, Object> map = new HashMap<>();
            pos++; // Salta '{'
            skipWhitespace();
            if (pos < src.length() && src.charAt(pos) == '}') {
                pos++;
                return map;
            }
            while (pos < src.length()) {
                skipWhitespace();
                if (pos >= src.length() || src.charAt(pos) != '"') {
                    throw new IllegalArgumentException("Se esperaba clave como cadena en pos " + pos);
                }
                String key = parseString();
                skipWhitespace();
                if (pos >= src.length() || src.charAt(pos) != ':') {
                    throw new IllegalArgumentException("Se esperaba ':' tras clave en pos " + pos);
                }
                pos++; // Salta ':'
                Object val = parse();
                map.put(key, val);
                skipWhitespace();
                if (pos < src.length() && src.charAt(pos) == ',') {
                    pos++;
                    skipWhitespace();
                } else if (pos < src.length() && src.charAt(pos) == '}') {
                    pos++;
                    break;
                }
            }
            return map;
        }

        private String parseString() {
            pos++; // Salta '"' inicial
            StringBuilder sb = new StringBuilder();
            while (pos < src.length()) {
                char c = src.charAt(pos++);
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\') {
                    if (pos >= src.length()) break;
                    char esc = src.charAt(pos++);
                    switch (esc) {
                        case '"' -> sb.append('"');
                        case '\\' -> sb.append('\\');
                        case '/' -> sb.append('/');
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        case 'u' -> {
                            if (pos + 4 <= src.length()) {
                                String hex = src.substring(pos, pos + 4);
                                sb.append((char) Integer.parseInt(hex, 16));
                                pos += 4;
                            }
                        }
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private Number parseNumber() {
            int start = pos;
            if (src.charAt(pos) == '-') pos++;
            while (pos < src.length() && (Character.isDigit(src.charAt(pos)) || src.charAt(pos) == '.')) {
                pos++;
            }
            String numStr = src.substring(start, pos);
            if (numStr.contains(".")) {
                return Double.valueOf(numStr);
            }
            return Long.valueOf(numStr);
        }

        private void skipWhitespace() {
            while (pos < src.length() && Character.isWhitespace(src.charAt(pos))) {
                pos++;
            }
        }
    }
}
