#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Feil: oppgi banen til Trivy-rapporten." >&2
  exit 2
fi

report=$1

if ! jq -e '
  (.Metadata.OS.Family | type == "string" and length > 0) and
  (.Results | type == "array") and
  (any(.Results[]; .Class == "os-pkgs" and (.Packages | type == "array" and length > 0)))
' "$report" >/dev/null 2>&1; then
  echo "Feil: Trivy-rapporten mangler kjent OS eller komplett OS-pakkeinventar." >&2
  exit 1
fi

if ! jq -e '
  [.Results[] | select(.Class == "os-pkgs") | .Vulnerabilities[]? |
    select(.VulnerabilityID == "CVE-2026-115506")] | length == 0
' "$report" >/dev/null; then
  echo "Feil: containerimaget inneholder CVE-2026-115506." >&2
  exit 1
fi

echo "OK: OS-pakkeinventaret er komplett, og CVE-2026-115506 ble ikke funnet."
