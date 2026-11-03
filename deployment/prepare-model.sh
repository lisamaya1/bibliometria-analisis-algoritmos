#!/bin/sh
# Solo preparación de archivos: no ejecuta inferencia ni modifica los modelos.
set -eu

if [ "$#" -ne 2 ]; then
    echo "Uso: sh prepare-model.sh minilm|bge directorio" >&2
    exit 1
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
model=$1
destination=$2
case "$model" in
    minilm)
        repository=sentence-transformers/all-MiniLM-L6-v2
        revision=1110a243fdf4706b3f48f1d95db1a4f5529b4d41
        license=Apache-2.0.txt
        ;;
    bge)
        repository=BAAI/bge-small-en-v1.5
        revision=5c38ec7c405ec4b44b94cc5a9bb96e735b38267a
        license=MIT-BGE.txt
        ;;
    *) echo "Modelo no admitido: $model" >&2; exit 1 ;;
esac

mkdir -p "$destination"
cd "$destination"
while read -r expected file; do
    [ -n "$file" ] || continue
    mkdir -p "$(dirname -- "$file")"
    if [ ! -f "$file" ]; then
        curl --fail --location --silent --show-error --retry 3 \
            --connect-timeout 30 --max-time 900 \
            "https://huggingface.co/$repository/resolve/$revision/$file" \
            --output "$file.partial"
        printf '%s  %s\n' "$expected" "$file.partial" | sha256sum --check --status
        mv "$file.partial" "$file"
    fi
    # Los archivos ya existentes también se verifican; un error detiene el build.
    printf '%s  %s\n' "$expected" "$file" | sha256sum --check --status
done < "$script_dir/models/$model.sha256"

# Este manifiesto contiene hashes esperados, no hashes aceptados de una descarga.
cp "$script_dir/models/$model.manifest.json" manifest.json
cp "$script_dir/licenses/$license" LICENSE
printf 'Modelo %s verificado: %s\n' "$repository" "$revision"
