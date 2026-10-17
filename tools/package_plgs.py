#!/usr/bin/env python3
"""Package classes.dex from a plugin debug APK into the SvBooster .plgs format."""
import argparse
import json
import zipfile
from pathlib import Path

MAX_DEX = 16 * 1024 * 1024
MAX_ICON = 512 * 1024


def main():
    parser = argparse.ArgumentParser(description="Cria um pacote .plgs para o SvBooster")
    parser.add_argument("--apk", required=True, type=Path, help="APK debug do módulo do plugin")
    parser.add_argument("--manifest", required=True, type=Path, help="manifest.json conforme a API .plgs")
    parser.add_argument("--icon", required=True, type=Path, help="ícone PNG quadrado, até 512x512")
    parser.add_argument("--output", required=True, type=Path, help="arquivo de saída com extensão .plgs")
    args = parser.parse_args()

    if args.output.suffix.lower() != ".plgs":
        raise SystemExit("Erro: o arquivo de saída precisa terminar em .plgs")
    manifest_bytes = args.manifest.read_bytes()
    manifest = json.loads(manifest_bytes.decode("utf-8"))
    for key in ("formatVersion", "apiVersion", "id", "name", "version", "category", "entryClass"):
        if key not in manifest:
            raise SystemExit(f"Erro: campo obrigatório ausente no manifest: {key}")
    if manifest["formatVersion"] != 1 or manifest["apiVersion"] != 1:
        raise SystemExit("Erro: formato/API esperados: formatVersion=1 e apiVersion=1")
    manifest["icon"] = "icon.png"
    manifest_bytes = (json.dumps(manifest, ensure_ascii=False, separators=(",", ":")) + "\n").encode("utf-8")

    icon = args.icon.read_bytes()
    if len(icon) > MAX_ICON or not icon.startswith(b"\x89PNG\r\n\x1a\n"):
        raise SystemExit("Erro: o ícone precisa ser um PNG válido de até 512 KiB")
    with zipfile.ZipFile(args.apk, "r") as apk:
        dex = apk.read("classes.dex")
    if len(dex) > MAX_DEX or not dex.startswith(b"dex\n"):
        raise SystemExit("Erro: classes.dex ausente, inválido ou maior que 16 MiB")

    args.output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(args.output, "w", compression=zipfile.ZIP_DEFLATED) as out:
        out.writestr("manifest.json", manifest_bytes)
        out.writestr("classes.dex", dex)
        out.writestr("icon.png", icon)
    print(f"Criado: {args.output} ({args.output.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
