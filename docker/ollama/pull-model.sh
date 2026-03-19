#!/bin/sh
# Wait for Ollama to be ready, then pull the model if not already present
set -e

MODEL="${LLM_MODEL:-qwen3-vl:8b}"
OLLAMA_URL="${OLLAMA_BASE_URL:-http://ollama:11434}"

echo "Waiting for Ollama at $OLLAMA_URL..."
for i in $(seq 1 30); do
  if wget -q --spider "$OLLAMA_URL/api/tags" 2>/dev/null; then
    echo "Ollama is ready."
    break
  fi
  echo "  Attempt $i/30 - not ready yet..."
  sleep 2
done

# Check if model is already pulled
if wget -qO- "$OLLAMA_URL/api/tags" 2>/dev/null | grep -q "$MODEL"; then
  echo "Model $MODEL already available."
else
  echo "Pulling model $MODEL (this may take a few minutes on first run)..."
  wget --post-data="{\"name\":\"$MODEL\"}" \
    --header="Content-Type: application/json" \
    -qO- "$OLLAMA_URL/api/pull" || echo "Model pull failed — LLM features will use fallback."
fi

echo "Done."
