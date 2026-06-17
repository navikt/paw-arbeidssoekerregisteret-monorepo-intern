#!/usr/bin/env bash
# Generates an EC P-256 (prime256v1) key pair for Kafka message signing.
#
# Private key  → uploaded to NAIS secret  (PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64)
# Public key   → committed to repo        (lib/kafka-signing/src/main/resources/paw-signing-public-keys/<key-id>.pub.b64)
#
# Keys are generated in a tmpfs directory and shredded on exit.
# File upload avoids copy/paste errors when updating console.nais.io.
#
# Usage:
#   ./scripts/generate-kafka-signing-keys.sh -e ENV -n SECRET_NAME [OPTIONS]
#
# Options:
#   -e, --env       dev|prod                (required)
#   -n, --name      nais-secret-name        (required, e.g. paw-api-inngang-kafka-signing-key)
#   -k, --key-id    key-id                  (required, e.g. paw-api-inngang-ecdsa-v1)
#   -p, --pub-dir   path/to/pub-keys-dir   (default: <repo>/lib/kafka-signing/src/main/resources/paw-signing-public-keys)
#   -t, --tmp-dir   tmpfs base directory    (default: /tmp)
#   -h, --help      Show this help

set -euo pipefail

# ── defaults ──────────────────────────────────────────────────────────────────
ENV=""
SECRET_NAME=""
KEY_ID=""
NAMESPACE="paw"
REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PUB_DIR=""
TMP_BASE="/tmp"
WORKDIR=""

# ── colours ───────────────────────────────────────────────────────────────────
RED='\033[0;31m'; YELLOW='\033[1;33m'; GREEN='\033[0;32m'; CYAN='\033[0;36m'; NC='\033[0m'
info()    { echo -e "${CYAN}ℹ ${NC}$*"; }
success() { echo -e "${GREEN}✔ ${NC}$*"; }
warn()    { echo -e "${YELLOW}⚠ ${NC}$*"; }
error()   { echo -e "${RED}✖ ${NC}$*" >&2; }

# ── cleanup ───────────────────────────────────────────────────────────────────
cleanup() {
    if [[ -n "${WORKDIR}" && -d "${WORKDIR}" ]]; then
        info "Makulerer sensitive filer i ${WORKDIR} …"
        if command -v shred &>/dev/null; then
            find "${WORKDIR}" -type f -exec shred -u {} \;
        else
            # macOS fallback: overwrite with zeros then remove
            find "${WORKDIR}" -type f -exec sh -c 'dd if=/dev/zero of="$1" bs=1 count=$(wc -c < "$1") 2>/dev/null; rm -f "$1"' _ {} \;
        fi
        rmdir "${WORKDIR}" 2>/dev/null || true
        success "Arbeidsmappen er fjernet."
    fi
}
trap cleanup EXIT

# ── argument parsing ───────────────────────────────────────────────────────────
usage() {
    sed -n '/^# Usage:/,/^[^#]/p' "$0" | grep '^#' | sed 's/^# \?//'
    exit 0
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        -e|--env)       ENV="$2";         shift 2 ;;
        -n|--name)      SECRET_NAME="$2"; shift 2 ;;
        -k|--key-id)    KEY_ID="$2";      shift 2 ;;
        -p|--pub-dir)   PUB_DIR="$2";     shift 2 ;;
        -t|--tmp-dir)   TMP_BASE="$2";    shift 2 ;;
        -h|--help)      usage ;;
        *) error "Ukjent argument: $1"; usage ;;
    esac
done

# ── validate ──────────────────────────────────────────────────────────────────
if [[ -z "${ENV}" ]]; then
    error "Mangler --env (dev eller prod)"
    echo ""
    usage
fi

if [[ -z "${SECRET_NAME}" ]]; then
    error "Mangler --name (nais secret-navn, f.eks. paw-api-inngang-kafka-signing-key)"
    echo ""
    usage
fi

# Derive PUB_DIR from repo root if not explicitly set
KEY_ID="${KEY_ID:-}"
PUB_DIR="${PUB_DIR:-${REPO_ROOT}/lib/kafka-signing/src/main/resources/paw-signing-public-keys}"

case "${ENV}" in
    dev)  NAIS_ENV="dev-gcp"  ;;
    prod) NAIS_ENV="prod-gcp" ;;
    *) error "--env må være 'dev' eller 'prod'"; exit 1 ;;
esac

if ! command -v openssl &>/dev/null; then
    error "openssl er ikke installert"
    exit 1
fi

# ── verify tmpfs ───────────────────────────────────────────────────────────────
FS_TYPE=$(stat -f -c %T "${TMP_BASE}" 2>/dev/null || stat -f "${TMP_BASE}" 2>/dev/null | awk '/Type:/{print $NF}' || echo "unknown")
if [[ "${FS_TYPE}" != "tmpfs" ]]; then
    warn "${TMP_BASE} er ikke tmpfs (type: ${FS_TYPE}). Vurder å bruke en tmpfs-mappe."
    read -r -p "Fortsett likevel? [j/N] " confirm
    [[ "${confirm}" =~ ^[jJ]$ ]] || exit 1
fi

# ── setup workdir ─────────────────────────────────────────────────────────────
WORKDIR=$(mktemp -d "${TMP_BASE}/paw-keys-XXXXXX")
chmod 700 "${WORKDIR}"
info "Arbeidsmappe (tmpfs): ${WORKDIR}"

PUB_FILE="${PUB_DIR}/${KEY_ID}.pub.b64"
PRIV_PEM="${WORKDIR}/ec-private.pem"
PRIV_B64_FILE="${WORKDIR}/PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64"
KEY_ID_FILE="${WORKDIR}/PAW_SIGNING_KEY_ID"

# ── generate key pair ─────────────────────────────────────────────────────────
echo ""
info "Genererer EC P-256 (prime256v1) nøkkelpar …"
openssl ecparam -name prime256v1 -genkey -noout -out "${PRIV_PEM}"
chmod 600 "${PRIV_PEM}"

# Private key → PKCS8 DER → base64 (ingen linjeskift)
openssl pkcs8 -topk8 -nocrypt -in "${PRIV_PEM}" -outform DER \
    | base64 -w0 > "${PRIV_B64_FILE}"
echo "" >> "${PRIV_B64_FILE}"   # trailing newline for cleaner uploads

# Key ID fil (nais secret expects one value per file)
echo -n "${KEY_ID}" > "${KEY_ID_FILE}"

# Public key → X.509 DER → base64 (ingen linjeskift) → pub.b64
mkdir -p "${PUB_DIR}"
openssl ec -in "${PRIV_PEM}" -pubout -outform DER \
    | base64 -w0 > "${PUB_FILE}"

success "Nøkkelpar generert."
echo ""
echo "  Privat nøkkelfil (upload til NAIS):  ${PRIV_B64_FILE}"
echo "  Key ID fil (upload til NAIS):         ${KEY_ID_FILE}"
echo "  Public key (commit til repo):         ${PUB_FILE}"
echo ""

# ── upload via nais CLI if available ──────────────────────────────────────────
if command -v nais &>/dev/null; then
    info "nais CLI funnet. Forsøker å oppdatere secret '${SECRET_NAME}' (--environment ${NAIS_ENV} --team ${NAMESPACE}) …"
    echo ""
    # Create the secret if it doesn't exist yet (ignore error if it already exists)
    nais secret create "${SECRET_NAME}" \
        --environment "${NAIS_ENV}" \
        --team "${NAMESPACE}" 2>/dev/null || true

    if nais secret set "${SECRET_NAME}" \
            --environment "${NAIS_ENV}" --team "${NAMESPACE}" \
            --key PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64 \
            --value-from-file "${PRIV_B64_FILE}" \
        && nais secret set "${SECRET_NAME}" \
            --environment "${NAIS_ENV}" --team "${NAMESPACE}" \
            --key PAW_SIGNING_KEY_ID \
            --value "${KEY_ID}"; then
        success "Secret oppdatert via nais CLI."
    else
        warn "nais CLI klarte ikke å oppdatere secret. Se manuell opplasting under."
        _show_manual_upload=true
    fi
else
    warn "nais CLI ikke funnet. Bruk manuell opplasting."
    _show_manual_upload=true
fi

# ── manual upload instructions ────────────────────────────────────────────────
if [[ "${_show_manual_upload:-false}" == "true" ]]; then
    echo ""
    echo -e "${CYAN}── Manuell opplasting via console.nais.io ──────────────────────────────${NC}"
    echo ""
    echo "  1. Gå til: https://console.nais.io/team/${NAMESPACE}/${NAIS_ENV}/secrets/${SECRET_NAME}"
    echo "     (opprett secreten om den ikke finnes)"
    echo ""
    echo "  2. Legg til / oppdater disse verdiene:"
    echo ""
    echo "     Nøkkel:   PAW_SIGNING_PRIVATE_KEY_PKCS8_BASE64"
    echo "     Last opp: ${PRIV_B64_FILE}"
    echo ""
    echo "     Nøkkel:   PAW_SIGNING_KEY_ID"
    echo "     Verdi:    ${KEY_ID}    (skriv inn direkte)"
    echo ""
    echo -e "${YELLOW}  ⚠  Bruk «Last opp fil» i stedet for å lime inn innholdet.${NC}"
    echo ""
    echo -e "${CYAN}────────────────────────────────────────────────────────────────────────${NC}"
fi

# ── next steps ────────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}── Neste steg ───────────────────────────────────────────────────────────${NC}"
echo ""
echo "  1. Last opp secret til NAIS (se over)."
echo ""
echo "  2. Legg til key-id i indeksfilen og commit:"
echo "       echo '${KEY_ID}' >> ${PUB_DIR}/index"
echo "       git add ${PUB_FILE} ${PUB_DIR}/index"
echo "       git commit -m 'feat(kafka-signing): legg til ${KEY_ID}.pub.b64'"
echo ""
echo "  3. Rull ut appen så den plukker opp ny nøkkel."
echo ""
echo "  4. Ved nøkkelrotasjon: behold gammel .pub.b64 — trengs for å verifisere"
echo "     meldinger signert med gammel nøkkel."
echo ""

# cleanup skjer via trap EXIT
