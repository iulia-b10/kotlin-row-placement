#!/usr/bin/env bash
set -euo pipefail
component_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if [[ -x "$component_dir/gradlew" ]]; then
  runner="$component_dir/gradlew"
elif [[ -x "$component_dir/../../gradlew" ]]; then
  runner="$component_dir/../../gradlew"
else
  runner="$(command -v gradle)"
fi
exec "$runner" -p "$component_dir" jvmTest wasmJsNodeTest macosArm64Test \
  compileKotlinIosArm64 compileKotlinIosSimulatorArm64 :example:run --console=plain "$@"
