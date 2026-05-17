#!/bin/bash
cd ~/project/github.com/chat-gusogst

# 读 token
GIT_TOKEN=$(cat ~/mcp/mcpkeys/git_token.env 2>/dev/null | grep -oP '(?<=GIT_TOKEN=).*' | tr -d '"\n')
if [ -z "$GIT_TOKEN" ]; then
  echo 'NO_TOKEN' > push_result.txt
  exit 1
fi

# 试多个镜像
for mirror in \
  "https://ghfast.top/https://github.com/suer781/chat-gusogst.git" \
  "https://ghproxy.net/https://github.com/suer781/chat-gusogst.git" \
  "https://gh-proxy.com/https://github.com/suer781/chat-gusogst.git" \
  "https://github.com/suer781/chat-gusogst.git"; do
  echo "Trying: $mirror" >> push_result.txt
  URL=$(echo "$mirror" | sed "s|https://|https://suer781:${GIT_TOKEN}@|1")
  git remote set-url origin "$URL"
  timeout 60 git push origin main >> push_result.txt 2>&1
  if [ $? -eq 0 ]; then
    echo "SUCCESS: $mirror" >> push_result.txt
    break
  fi
done
echo 'DONE' >> push_result.txt
