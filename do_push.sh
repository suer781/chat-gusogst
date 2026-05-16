#!/bin/bash
set -e
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

# Extract token value from .env (skip comments)
TOKEN=$(grep -v '^#' ~/mcp/mcpkeys/git_token.env | grep GIT_TOKEN | cut -d'=' -f2 | tr -d '\n')

if [ -z "$TOKEN" ]; then
  echo "[push] ERROR: no token found"
  exit 1
fi

REMOTE_URL="https://${TOKEN}@ghfast.top/https://github.com/suer781/chat-gusogst.git"
BRANCH=$(git branch --show-current)
echo "[push] branch=$BRANCH"
git remote set-url upstream "$REMOTE_URL"
echo "[push] pushing..."
git push upstream "$BRANCH" 2>&1
echo "[push] done!"
git remote set-url upstream "https://ghfast.top/https://github.com/suer781/chat-gusogst.git"
echo "[push] URL cleaned"
