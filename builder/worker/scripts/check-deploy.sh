#!/usr/bin/env bash
# Checks a deployed builder the way a browser sees it. The wasm has to arrive brotli-compressed
# and cached for good, and next has to carry noindex while production must not. Each request is a
# GET rather than a HEAD, since the edge only compresses a response that has a body, and each one
# retries, since the first request to a new custom domain can fail while its certificate is issued.
#
# Run it after `./gradlew :builder:apps:web:assembleSite` built the site that was deployed:
#   builder/worker/scripts/check-deploy.sh https://next.materialkolor.com next
#   builder/worker/scripts/check-deploy.sh https://materialkolor.com production
set -euo pipefail
shopt -s nullglob

origin=${1:?usage: check-deploy.sh <origin> <next|production>}
environment=${2:?usage: check-deploy.sh <origin> <next|production>}
site="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../apps/web/build/site" && pwd)"

case "$environment" in
  next) worker=materialkolor-builder-next ;;
  production) worker=materialkolor-builder ;;
  *)
    echo "The environment is '$environment', expected next or production." >&2
    exit 2
    ;;
esac

fetch() {
  curl -sS --fail --retry 6 --retry-delay 20 --retry-all-errors -o /dev/null -D - \
    -H 'Accept-Encoding: br' "$1" | tr -d '\r'
}

unreachable() {
  echo "::error title=Builder unreachable::$1 did not answer. Check the custom domain of the Worker $worker."
  exit 1
}

wasms=("$site"/assets/*.wasm)
if [ ${#wasms[@]} -eq 0 ]; then
  echo "::error title=No site::$site/assets has no wasm. Run ./gradlew :builder:apps:web:assembleSite first."
  exit 1
fi

failed=0
for wasm in "${wasms[@]}"; do
  url="$origin/assets/$(basename "$wasm")"
  headers=$(fetch "$url") || unreachable "$url"
  echo "$url"
  echo "$headers"
  if ! grep -qix 'content-encoding: br' <<< "$headers"; then
    echo "::error title=Wasm not brotli-compressed::$url came back without content-encoding br. Check that the materialkolor.com zone compresses application/wasm with brotli, under Rules, Compression Rules."
    failed=1
  fi
  if ! grep -qix 'cache-control: public, max-age=31536000, immutable' <<< "$headers"; then
    echo "::error title=Wasm not immutable::$url came back without the immutable Cache-Control. Check the /assets/* rule in the _headers that writeHeaders writes."
    failed=1
  fi
done

# The page comes from the site's _headers and a theme page from the Worker.
for path in / /t/AdllOwAAAAAT; do
  url="$origin$path"
  headers=$(fetch "$url") || unreachable "$url"
  if grep -qix 'x-robots-tag: noindex' <<< "$headers"; then
    if [ "$environment" = production ]; then
      echo "::error title=Production is not indexable::$url came back with X-Robots-Tag noindex. Check that the site was built without -Psite.env=next."
      failed=1
    fi
  elif [ "$environment" = next ]; then
    echo "::error title=Next is indexable::$url came back without X-Robots-Tag noindex. Check the next _headers and ROBOTS_TAG in wrangler.jsonc."
    failed=1
  fi
done

exit "$failed"
