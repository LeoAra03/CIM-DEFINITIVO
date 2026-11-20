#!/usr/bin/env bash
# Author-owned history consolidation. DESTRUCTIVE: rewrites every commit so
# git log --format='%an <%ae>' yields exactly one identity, force-pushes the
# result, and renames the branch to main.
#
# Prerequisites:
#   - git-filter-repo installed (python -m pip install git-filter-repo)
#   - a clean working tree
#   - push access to the origin repository
#
# Run this script manually from a FRESH MIRROR CLONE of the repository, not
# from a working checkout you care about.
set -euo pipefail

TARGET_NAME="Leonardo Araya"
TARGET_EMAIL="228963571+LeoAra03@users.noreply.github.com"

if ! command -v git-filter-repo >/dev/null 2>&1; then
  echo "git-filter-repo is required: python -m pip install git-filter-repo" >&2
  exit 1
fi

git clone --mirror git@github.com:LeoAra03/CIM-DEFINITIVO.git cim-history-rewrite
cd cim-history-rewrite

git filter-repo --use-base-name \
  --name-callback "print('${TARGET_NAME}'.encode('utf-8'))" \
  --email-callback "print('${TARGET_EMAIL}'.encode('utf-8'))"

git remote add origin git@github.com:LeoAra03/CIM-DEFINITIVO.git
git push --force origin --all
git push origin --tags

echo "History rewritten. Verify with:"
echo "  git log --format='%an <%ae>' | sort -u"
echo "Then set the default branch to main in the GitHub settings."
