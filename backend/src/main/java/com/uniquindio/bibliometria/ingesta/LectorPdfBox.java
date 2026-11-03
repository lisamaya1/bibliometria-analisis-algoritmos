package com.uniquindio.bibliometria.ingesta;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** PDFBox lee el PDF; las reglas de titulo, autores y resumen se aplican por separado. */
public final class LectorPdfBox implements LectorPdf {
    @Override
    public DocumentoPdf leer(String archivo, byte[] pdf) throws IOException {
        Objects.requireNonNull(pdf, "pdf");
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            if (!documento.getCurrentAccessPermission().canExtractContent()) {
                throw new IOException("El PDF no permite extraer texto: " + archivo);
            }
            List<PaginaPdf> paginas = new ArrayList<>();
            for (int i = 0; i < documento.getNumberOfPages(); i++) {
                ExtractorPosiciones extractorPosiciones = new ExtractorPosiciones();
                extractorPosiciones.setStartPage(i + 1);
                extractorPosiciones.setEndPage(i + 1);
                extractorPosiciones.setSortByPosition(true);
                String texto = extractorPosiciones.getText(documento);
                var box = documento.getPage(i).getCropBox();
                paginas.add(new PaginaPdf(i + 1, box.getWidth(), box.getHeight(), texto,
                        extractorPosiciones.lineas()));
            }
            return new DocumentoPdf(archivo, documento.getDocumentInformation().getTitle(), paginas);
        }
    }

    private static final class ExtractorPosiciones extends PDFTextStripper {
        private final List<TextPosition> posiciones = new ArrayList<>();
        private final Set<TextPosition> iniciosPalabra = Collections.newSetFromMap(new IdentityHashMap<>());

        private ExtractorPosiciones() throws IOException { }

        @Override
        public void beginMarkedContentSequence(COSName tag, COSDictionary properties) {
            // Conserva los guiones visibles que PDFBox elimina de ActualText.
            // Usa una copia del diccionario para no modificar el PDF original.
            if (properties != null) {
                String actualText = properties.getString(COSName.ACTUAL_TEXT);
                if (actualText != null && actualText.indexOf('\u00ad') >= 0) {
                    properties = new COSDictionary(properties);
                    properties.setString(COSName.ACTUAL_TEXT, actualText.replace('\u00ad', '-'));
                }
            }
            super.beginMarkedContentSequence(tag, properties);
        }

        @Override
        protected void writeString(String texto, List<TextPosition> textPositions) throws IOException {
            // PDFTextStripper entrega las palabras por separado. Conserva esos limites
            // para reconstruir espacios sin modificar el texto original.
            if (!textPositions.isEmpty()) iniciosPalabra.add(textPositions.get(0));
            posiciones.addAll(textPositions);
            super.writeString(texto, textPositions);
        }

        private List<PaginaPdf.Linea> lineas() {
            posiciones.sort(Comparator.comparing(TextPosition::getYDirAdj)
                    .thenComparing(TextPosition::getXDirAdj));
            List<List<TextPosition>> filas = new ArrayList<>();
            for (TextPosition posicion : posiciones) {
                if (filas.isEmpty() || Math.abs(posicion.getYDirAdj()
                        - filas.get(filas.size() - 1).get(0).getYDirAdj()) > 2.5f) {
                    filas.add(new ArrayList<>());
                }
                filas.get(filas.size() - 1).add(posicion);
            }
            List<PaginaPdf.Linea> resultado = new ArrayList<>();
            for (List<TextPosition> fila : filas) {
                fila.sort(Comparator.comparing(TextPosition::getXDirAdj));
                List<TextPosition> segmento = new ArrayList<>();
                float finX = -1;
                for (TextPosition p : fila) {
                    // Un espacio ancho separa columnas; los espacios normales se conservan en la linea.
                    if (!segmento.isEmpty() && p.getXDirAdj() - finX > 18) {
                        resultado.add(linea(segmento));
                        segmento.clear();
                    }
                    segmento.add(p);
                    finX = Math.max(finX, p.getXDirAdj() + p.getWidthDirAdj());
                }
                if (!segmento.isEmpty()) resultado.add(linea(segmento));
            }
            resultado.sort(Comparator.comparing(PaginaPdf.Linea::y).thenComparing(PaginaPdf.Linea::x));
            return resultado;
        }

        private PaginaPdf.Linea linea(List<TextPosition> caracteres) {
            StringBuilder texto = new StringBuilder();
            StringBuilder reconstruido = new StringBuilder();
            float fin = caracteres.get(0).getXDirAdj();
            float fuente = 0;
            for (TextPosition p : caracteres) {
                boolean hayEspacio = p.getXDirAdj() - fin > Math.max(.3f, p.getWidthOfSpace() * .3f);
                if (texto.length() > 0 && hayEspacio) {
                    texto.append(' ');
                }
                if (reconstruido.length() > 0 && (hayEspacio || iniciosPalabra.contains(p))
                        && !terminaConEspacio(reconstruido) && !p.getUnicode().isBlank()) {
                    reconstruido.append(' ');
                }
                texto.append(p.getUnicode());
                reconstruido.append(p.getUnicode());
                fin = Math.max(fin, p.getXDirAdj() + p.getWidthDirAdj());
                // La altura permite reconocer el tamano visible aunque el PDF use una escala.
                fuente = Math.max(fuente, p.getHeightDir());
            }
            var primero = caracteres.get(0);
            return new PaginaPdf.Linea(texto.toString().strip(), primero.getXDirAdj(),
                    primero.getYDirAdj(), fin, fuente, reconstruido.toString().strip());
        }

        private boolean terminaConEspacio(StringBuilder texto) {
            int codigo = texto.codePointBefore(texto.length());
            return Character.isWhitespace(codigo) || Character.isSpaceChar(codigo);
        }
    }
}
