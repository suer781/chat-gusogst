#!/bin/bash
cd ~/project/github.com/chat-gusogst
export CI=true DEBIAN_FRONTEND=noninteractive GIT_TERMINAL_PROMPT=0 GCM_INTERACTIVE=never HOMEBREW_NO_AUTO_UPDATE=1 GIT_EDITOR=: EDITOR=: VISUAL='' GIT_SEQUENCE_EDITOR=: GIT_MERGE_AUTOEDIT=no GIT_PAGER=cat PAGER=cat npm_config_yes=true PIP_NO_INPUT=1 YARN_ENABLE_IMMUTABLE_INSTALLS=false;

# Read token from .env file
if [ -f ~/mcp/mcpkeys/git_token.env ]; then
  source ~/mcp/mcpkeys/git_token.env
  TOKEN=${GIT_TOKEN:-$GITHUB_TOKEN}
fi

if [ -z "$TOKEN" ]; then
  echo 'ERROR: No GitHub token found'
  exit 1
fi

# Push with token via ghfast mirror
AUTH_URL="https://suer781:${TOKEN}@ghfast.top/https://github.com/suer781/chat-gusogst.git"
if timeout 40 git push "$AUTH_URL" main 2>&1; then
  echo 'SUCCESS'
else
  echo 'FAILED'
fi
