#!/bin/sh
set -eu

fail() {
  printf '%s\n' "ERROR: $1" >&2
  exit 1
}

test -d parent || fail "missing parent module"
test -d util || fail "missing util module"
test -d magic || fail "missing magic module"
test -d dbtools || fail "missing dbtools module"
test -d platform || fail "missing platform module"

test ! -d package || fail "package module must not be migrated"
test ! -d tools || fail "tools module must not be migrated"
test ! -d support || fail "support module must not be migrated"

tracked_paths="$(mktemp "${TMPDIR:-/tmp}/bpmt-verify.XXXXXX")"
trap 'rm -f "$tracked_paths"' EXIT HUP INT TERM

git ls-files --cached --others --exclude-standard > "$tracked_paths"

forbidden_path="$(
  awk '
    function base(path) {
      sub(/^.*\//, "", path)
      return path
    }

    {
      name = base($0)

      if ($0 == ".svn" || $0 ~ /^\.svn\// || $0 ~ /\/\.svn\//) {
        print $0
        exit
      }
      if ($0 == "target" || $0 ~ /^target\// || $0 ~ /\/target\//) {
        print $0
        exit
      }
      if (name == "kyq.sql" ||
          name ~ /^aspose-.*\.jar$/ ||
          name ~ /^jpedal.*\.jar$/ ||
          name ~ /^patch-implementation.*\.jar$/ ||
          name ~ /^ueditor.*\.war$/ ||
          name ~ /-license\.xml$/ ||
          name ~ /\.h2\.db$/ ||
          name ~ /\.h2\.db\.bak$/ ||
          name == "Thumbs.db" ||
          name == "settings.local.xml" ||
          name == "settings.xml" ||
          $0 ~ /^db\/data\/.+/ ||
          $0 ~ /^db\/logs\/.+/ ||
          ($0 ~ /^runtime\/.+/ && $0 != "runtime/.gitkeep")) {
        print $0
        exit
      }
    }
  ' "$tracked_paths"
)"

test -z "$forbidden_path" || fail "forbidden/local runtime file is tracked or pending commit: $forbidden_path"

origin_url="$(git remote get-url origin)"
test "$origin_url" = "https://github.com/wodenwang/bpmt-lite.git" || fail "unexpected origin: $origin_url"

printf '%s\n' "OK: repository hygiene checks passed"
