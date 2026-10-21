#!/bin/sh
# Mechanical checks of Ballarch author rules for a C decider translation unit.
#
#   R2 (pure decider): the object file references no external symbol. A decider
#      that reads a clock, performs I/O, allocates, or calls into the runtime
#      needs a symbol from outside. memcpy, memset and memmove are allowed:
#      compilers emit them for plain struct assignment.
#   R1 (single owner): the object file defines no writable data. Global and
#      static variables are state that no runtime commits.
#
# Usage: tools/check_purity.sh decider.c [more cc flags]   (run from reference/c)
set -eu
src=$1
shift
cc=${CC:-cc}
obj=$(mktemp "${TMPDIR:-/tmp}/ballarch-purity.XXXXXX")
trap 'rm -f "$obj"' EXIT
"$cc" -std=c11 -O2 -c -Iinclude -I"$(dirname "$src")" "$@" -o "$obj" "$src"
undefined=$(nm -u "$obj" | awk '{print $NF}' | grep -vxE 'memcpy|memset|memmove' | tr '\n' ' ' || true)
writable=$(nm "$obj" | awk 'NF == 3 && $2 ~ /^[BbDdCGgSs]$/ {print $3}' | tr '\n' ' ' || true)
status=0
if [ -n "$undefined" ]; then
    echo "R2 violated in $src: references external symbols: $undefined"
    status=1
fi
if [ -n "$writable" ]; then
    echo "R1 violated in $src: defines writable data: $writable"
    status=1
fi
[ "$status" -eq 0 ] && echo "R1/R2 object checks passed for $src"
exit "$status"
