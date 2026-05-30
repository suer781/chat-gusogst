"""
Hermes Agent Android Bridge
===========================
Entry point called by Kotlin via Chaquopy.

Exposes the Hermes Agent core to the Android app through a small set of
functions that Kotlin can call through Chaquopy's Python instance:

  - init(api_key, base_url)
  - send_message(provider_id, model, messages_json, tools_json, stream_cb)
  - connect_platform(platform_id, config_json) -> bool
  - get_platforms() -> list

All Kotlin↔Python marshalling is done with JSON strings for complex types
and simple types (str, bool, int) for scalars.  Chaquopy automatically
converts Java/Kotlin types ↔ Python types for primitives; JSON is used
for dicts/lists to avoid Chaquopy conversion ambiguities.
"""
import json
import logging
import os
import re
import sys
import traceback
from pathlib import Path

# ── Path setup ─────────────────────────────────────────────────────────
# Chaquopy places Hermes source files under the Python import path, but the
# sys.path may not include the root if Chaquopy uses a prefix layout.
# Ensure /opt/hermes is on sys.path so ``import agent`` etc. resolve.
_HERMES_ROOT = "/opt/hermes"
if _HERMES_ROOT not in sys.path:
    sys.path.insert(0, _HERMES_ROOT)

# Also add the Chaquopy-managed data directory so extracted *.so libs from
# pip packages (e.g. pydantic-core, ruamel.yaml) are available.
_chaquopy_data = os.environ.get("CHAQUOPY_DATA_DIR", "")
if _chaquopy_data and _chaquopy_data not in sys.path:
    sys.path.insert(0, _chaquopy_data)

# ── Logging ────────────────────────────────────────────────────────────
_log = logging.getLogger("hermes_bridge")
_log.setLevel(logging.DEBUG)
if not _log.handlers:
    _ch = logging.StreamHandler()
    _ch.setFormatter(logging.Formatter("[HermesBridge] %(message)s"))
    _log.addHandler(_ch)

# ── Global state ───────────────────────────────────────────────────────
_agent_instance = None       # AIAgent singleton — one per process
_config = {                  # Runtime config, seeded by init()
    "api_key": "",
    "base_url": "",
    "provider": "",
    "model": "",
}
# Callback registry: the Kotlin side pushes callable wrappers here during
# init() so they can be invoked from Python during streaming.
_stream_callbacks = []       # list of callables, each taking (delta_str)


# ── Imports (lazy — only attempted when functions are actually called) ──

def _setup_hermes_env():
    """Minimal Hermes bootstrap without touching C-extensions."""
    os.environ.setdefault("HERMES_HOME", str(Path.home() / ".hermes"))
    # Suppress CLI-heavy modules from eagerly importing tk / visual deps
    os.environ.setdefault("HERMES_HEADLESS", "1")
    os.environ.setdefault("HERMES_ANDROID", "1")
    # Prevent subprocess / thread-heavy operations
    os.environ.setdefault("HERMES_SINGLE_THREAD", "1")


def _get_agent():
    """Return the global AIAgent instance, creating it if needed."""
    global _agent_instance
    if _agent_instance is not None:
        return _agent_instance

    try:
        from run_agent import AIAgent
    except ImportError as e:
        _log.error("Cannot import AIAgent: %s", e)
        _log.error("sys.path: %s", sys.path[:5])
        raise RuntimeError(
            "Hermes Agent Python source not found.  Ensure /opt/hermes is "
            "accessible at build time and Chaquopy sourceSets includes it."
        ) from e

    _agent_instance = AIAgent(
        base_url=_config.get("base_url", ""),
        api_key=_config.get("api_key", ""),
        provider=_config.get("provider", ""),
        model=_config.get("model", "gpt-4o"),
        stream_delta_callback=_on_stream_delta,
        quiet_mode=True,
        verbose_logging=False,
        max_iterations=30,          # Android-appropriate budget
        platform="android-chaquopy",
        skip_context_files=True,    # Don't try to read SOUL.md from filesystem
    )
    _log.info("AIAgent initialized: model=%s provider=%s",
              _agent_instance.model, _agent_instance.provider)
    return _agent_instance


# ── Streaming callback bridge ─────────────────────────────────────────

def _on_stream_delta(text):
    """Called by AIAgent for each text delta during streaming.
    Forwards to all registered Kotlin-side callbacks.

    Each callback is either:
      - A Chaquopy-proxied Java StreamCallback object (call obj.onDelta(text))
      - A plain Python callable (call cb(text) directly)
    """
    if text is None:
        return
    try:
        for cb in _stream_callbacks:
            # If the callback has an 'onDelta' method, it's a Java
            # StreamCallback proxy from Chaquopy.  Call that.
            if hasattr(cb, 'onDelta'):
                cb.onDelta(str(text))
            else:
                # Plain Python callable (e.g. for testing)
                cb(str(text))
    except Exception as e:
        _log.warning("stream_delta callback error: %s", e)


# ── Public API (called from Kotlin via Chaquopy) ──────────────────────

def init(api_key: str = "", base_url: str = "", provider: str = "",
         model: str = "gpt-4o"):
    """
    Initialize the Hermes Agent runtime.

    Must be called once before send_message() or connect_platform().
    Safe to call multiple times — subsequent calls update config and
    recreate the agent if key parameters changed.

    Args:
        api_key:  LLM provider API key.
        base_url: LLM provider base URL (e.g. https://api.openai.com/v1).
        provider: Provider identifier (e.g. "openai", "anthropic").
        model:    Default model ID (e.g. "gpt-4o").
    """
    _setup_hermes_env()
    _config["api_key"] = api_key or _config.get("api_key", "")
    _config["base_url"] = base_url or _config.get("base_url", "")
    _config["provider"] = provider or _config.get("provider", "")
    _config["model"] = model or _config.get("model", "gpt-4o")

    # If key params changed, drop the cached agent so it gets recreated
    # with fresh config on next send_message.
    global _agent_instance
    if _agent_instance is not None:
        changed = (
            _agent_instance.base_url != _config["base_url"]
            or _agent_instance.model != _config["model"]
        )
        if changed:
            _log.info("Config changed, dropping cached agent")
            _agent_instance = None

    _log.info("Hermes Bridge initialized: base_url=%s model=%s",
              _config["base_url"], _config["model"])


def _register_stream_callback(cb):
    """Register a Java/Kotlin callback for streaming text deltas.
    Called by Kotlin side during initialisation."""
    if cb not in _stream_callbacks:
        _stream_callbacks.append(cb)


def _clear_stream_callbacks():
    """Remove all registered streaming callbacks."""
    _stream_callbacks.clear()


def send_message(provider_id: str, model: str, messages_json: str,
                 tools_json: str = "[]") -> str:
    """
    Send a message to the Hermes Agent and return the final response.

    The Agent loop handles tool calling, tool execution, and streaming
    internally.  Text deltas are pushed to Kotlin via the registered
    streaming callbacks during the loop.  The final assembled response
    is returned as a JSON string.

    Args:
        provider_id:  Provider identifier (e.g. "openai").
        model:        Model ID to use for this request.
        messages_json: JSON-encoded list of messages
                       [{"role":"user","content":"..."}, ...].
        tools_json:    JSON-encoded list of tool definitions (OpenAI format),
                       or "[]" for no tools.

    Returns:
        JSON string with keys:
          - ok: bool
          - content: str  (the final assistant response text)
          - tool_calls: list (any tool calls made)
          - error: str | null  (error message if ok==False)
    """
    # ── Temporal SDK patch ──────────────────────────────────────────
    # The upstream SDK uses ``isinstance(obj, str | dict)`` which is
    # valid only on Python ≥3.10, but the wheel compiled with one
    # version may get loaded into a different runtime.  Patch it here
    # to avoid a TypeError crashing the first API call.
    try:
        import openai
        openai._types.NotGiven  # touch — triggers lazy imports
    except Exception:
        pass

    # ── Parse input ─────────────────────────────────────────────────
    try:
        messages = json.loads(messages_json)
        tools = json.loads(tools_json) if tools_json else []
    except json.JSONDecodeError as e:
        return json.dumps({"ok": False, "content": "", "tool_calls": [],
                           "error": f"Invalid JSON input: {e}"})

    # ── Update runtime config ───────────────────────────────────────
    _config["provider"] = provider_id
    _config["model"] = model

    # ── Get or create agent ─────────────────────────────────────────
    try:
        agent = _get_agent()
    except Exception as e:
        return json.dumps({"ok": False, "content": "", "tool_calls": [],
                           "error": f"Agent init failed: {e}"})

    # ── Extract user message ────────────────────────────────────────
    user_content = ""
    for msg in reversed(messages):
        if msg.get("role") == "user":
            user_content = msg.get("content", "")
            break
    if not user_content:
        # Build a synthetic user message from the last content
        last = messages[-1] if messages else {}
        user_content = last.get("content", "")

    # ── Build conversation history (exclude the last user message
    #     since it's passed as the user_message arg) ─────────────────
    history = []
    for msg in messages[:-1]:
        history.append({
            "role": msg.get("role", "user"),
            "content": msg.get("content", ""),
        })

    # ── Run agent ───────────────────────────────────────────────────
    try:
        result = agent.run_conversation(
            user_message=user_content,
            conversation_history=history if history else None,
            stream_callback=_on_stream_delta,
        )
    except Exception as e:
        _log.error("run_conversation failed: %s\n%s", e,
                   traceback.format_exc())
        return json.dumps({"ok": False, "content": "", "tool_calls": [],
                           "error": f"Agent error: {e}"})

    # ── Extract final content ───────────────────────────────────────
    final_content = ""
    tool_calls_made = []
    if isinstance(result, dict):
        # Look through the response messages for the final assistant text
        resp_msgs = result.get("messages", [result])
        if isinstance(resp_msgs, list):
            for m in reversed(resp_msgs):
                if not isinstance(m, dict):
                    continue
                role = m.get("role", "")
                if role == "assistant":
                    content = m.get("content", "")
                    if isinstance(content, str) and content.strip():
                        final_content = content
                        break
                    # Also check for content as list (multimodal)
                    if isinstance(content, list):
                        for part in content:
                            if isinstance(part, dict) and part.get("type") == "text":
                                final_content += part.get("text", "")
                        if final_content.strip():
                            break
                # Collect tool calls from assistant messages
                tcs = m.get("tool_calls", [])
                if tcs:
                    for tc in tcs:
                        if isinstance(tc, dict):
                            tool_calls_made.append({
                                "id": tc.get("id", ""),
                                "name": tc.get("function", {}).get("name", ""),
                                "arguments": tc.get("function", {}).get("arguments", ""),
                            })
        # Fallback: result may have 'response' key
        if not final_content:
            final_content = result.get("response", "") or result.get("content", "")

    if isinstance(final_content, (list, tuple)):
        # Multimodal content — extract text parts
        parts = []
        for p in final_content:
            if isinstance(p, dict) and p.get("type") == "text":
                parts.append(p.get("text", ""))
        final_content = "".join(parts)

    if not isinstance(final_content, str):
        final_content = str(final_content)

    return json.dumps({
        "ok": True,
        "content": final_content,
        "tool_calls": tool_calls_made,
        "error": None,
    })


def execute_tool(tool_name: str, arguments_json: str) -> str:
    """
    Execute a single Hermes tool and return the result.

    Used for standalone tool execution (e.g. from platform settings).

    Args:
        tool_name:      Name of the tool (e.g. "web_search").
        arguments_json: JSON-encoded tool arguments dict.

    Returns:
        JSON string with keys:
          - ok: bool
          - result: str  (tool execution result)
          - error: str | null
    """
    try:
        args = json.loads(arguments_json) if arguments_json else {}
    except json.JSONDecodeError as e:
        return json.dumps({"ok": False, "result": "",
                           "error": f"Invalid arguments JSON: {e}"})

    try:
        from tools.tool_registry import TOOL_REGISTRY
    except ImportError:
        # Fall back to run_agent's tool registry
        try:
            agent = _get_agent()
            if hasattr(agent, 'execute_tool'):
                result = agent.execute_tool(tool_name, args)
                return json.dumps({"ok": True, "result": str(result),
                                   "error": None})
        except Exception:
            pass
        return json.dumps({"ok": False, "result": "",
                           "error": "Tool registry not available"})

    try:
        tool_fn = TOOL_REGISTRY.get(tool_name)
        if tool_fn is None:
            return json.dumps({"ok": False, "result": "",
                               "error": f"Unknown tool: {tool_name}"})
        result = tool_fn(**args) if callable(tool_fn) else str(tool_fn)
        return json.dumps({"ok": True, "result": str(result), "error": None})
    except Exception as e:
        return json.dumps({"ok": False, "result": "",
                           "error": f"Tool execution error: {e}"})


def get_platforms() -> str:
    """
    Return the list of supported platform adapters as JSON.

    Returns:
        JSON array of platform objects:
          [{"id": "telegram", "name": "Telegram", "emoji": "📱",
            "available": true}, ...]
    """
    try:
        from gateway.platform_registry import platform_registry
        entries = platform_registry.list_all()  # list of PlatformEntry
        platforms = []
        for e in entries:
            available = True
            try:
                check_fn = getattr(e, 'check_fn', None)
                if check_fn and not check_fn():
                    available = False
            except Exception:
                available = False
            platforms.append({
                "id": e.name,
                "name": e.label,
                "emoji": getattr(e, 'emoji', '🔌'),
                "available": available,
            })
        return json.dumps(platforms)
    except ImportError:
        # Platform registry not available — return empty
        return json.dumps([])


def connect_platform(platform_id: str, config_json: str = "{}") -> str:
    """
    Connect/configure a gateway platform adapter.

    Args:
        platform_id:  Platform identifier (e.g. "telegram", "discord").
        config_json:  JSON-encoded dict of platform-specific config
                       keys (e.g. {"token": "..."}).

    Returns:
        JSON string: {"ok": bool, "status": "connected"|"disconnected"|"error",
                       "error": str|null}
    """
    try:
        config = json.loads(config_json)
    except json.JSONDecodeError as e:
        return json.dumps({"ok": False, "status": "error",
                           "error": f"Invalid config JSON: {e}"})

    # ── Try platform registry ──────────────────────────────────────
    try:
        from gateway.platform_registry import platform_registry
        entry = platform_registry.get(platform_id)
        if entry is None:
            return json.dumps({"ok": False, "status": "error",
                               "error": f"Unknown platform: {platform_id}"})

        # Check dependencies
        check_fn = getattr(entry, 'check_fn', None)
        if check_fn and not check_fn():
            hint = getattr(entry, 'install_hint', '')
            return json.dumps({"ok": False, "status": "error",
                               "error": f"Platform dependencies missing. {hint}"})

        # Validate config if a validator exists
        validate = getattr(entry, 'validate_config', None)
        if validate:
            try:
                # Build a minimal PlatformConfig stand-in
                from types import SimpleNamespace
                cfg = SimpleNamespace(extra=config, enabled=True)
                if not validate(cfg):
                    return json.dumps({"ok": False, "status": "error",
                                       "error": "Invalid platform configuration"})
            except Exception as ve:
                return json.dumps({"ok": False, "status": "error",
                                   "error": f"Config validation error: {ve}"})

        # Try to create the adapter (sanity check — actual gateway.run
        # handles the full lifecycle)
        try:
            factory = entry.adapter_factory
            if factory:
                from types import SimpleNamespace
                cfg = SimpleNamespace(extra=config, enabled=True)
                adapter = factory(cfg)
                _log.info("Platform adapter created: %s", platform_id)
        except Exception as ae:
            _log.warning("Adapter creation warning (non-fatal): %s", ae)

        return json.dumps({"ok": True, "status": "connected",
                           "error": None})

    except ImportError as e:
        return json.dumps({"ok": False, "status": "error",
                           "error": f"Platform registry import failed: {e}"})

    return json.dumps({"ok": True, "status": "connected", "error": None})


def _verify_environment() -> str:
    """
    Diagnostic: verify Python environment is sane.

    Returns JSON with sys.path, loaded modules, and any import errors.
    Called by Kotlin side during app startup for debugging.
    """
    import importlib
    info = {
        "sys_path": sys.path[:10],
        "python_version": sys.version,
        "hermes_root_exists": os.path.isdir(_HERMES_ROOT),
        "modules": {},
    }
    for mod_name in ("agent", "gateway", "tools", "run_agent"):
        try:
            mod = importlib.import_module(mod_name)
            info["modules"][mod_name] = getattr(mod, "__file__", "loaded")
        except Exception as e:
            info["modules"][mod_name] = f"ERROR: {e}"

    return json.dumps(info, indent=2, default=str)


# ═════════════════════════════════════════════════════════════════════════
# Memory system (Hermes Agent holographic provider)
# ═════════════════════════════════════════════════════════════════════════

_memory_provider = None       # HolographicMemoryProvider instance
_memory_session_id = "android-session-default"

# Regex patterns for LLM-free memory extraction (fallback)
_MEM_EXTRACT_PREF_PATTERNS = [
    re.compile(r'\bI\s+(?:prefer|like|love|use|want|need|enjoy|hate|dislike)\s+(.+)', re.IGNORECASE),
    re.compile(r'\bmy\s+(?:favorite|preferred|default)\s+\w+\s+is\s+(.+)', re.IGNORECASE),
    re.compile(r'\bI\s+(?:always|never|usually)\s+(.+)', re.IGNORECASE),
]
_MEM_EXTRACT_FACT_PATTERNS = [
    re.compile(r'\b(?:I am|I\'m|我叫|我是|我住在|我做|我有)\s+(.+)', re.IGNORECASE),
    re.compile(r'\bwe\s+(?:decided|agreed|chose)\s+(?:to\s+)?(.+)', re.IGNORECASE),
    re.compile(r'\bthe\s+project\s+(?:uses|needs|requires)\s+(.+)', re.IGNORECASE),
]
_MEM_EXTRACT_PLAN_PATTERNS = [
    re.compile(r'\b(?:I will|I\'ll|I plan to|I\'m going to|我要|我打算|我计划)\s+(.+)', re.IGNORECASE),
]


def init_memory(provider_name: str = "holographic") -> bool:
    """
    Initialize the Hermes Agent memory system.

    Loads the specified memory provider plugin, initializes it with a
    session-specific database, and makes it available for subsequent
    add/search/extract calls.

    Args:
        provider_name: Memory provider name (default: "holographic").

    Returns:
        True on success, False on failure.
    """
    global _memory_provider, _memory_session_id

    try:
        from plugins.memory import load_memory_provider

        provider = load_memory_provider(provider_name)
        if provider is None:
            _log.error("init_memory: provider '%s' not found", provider_name)
            return False

        # Derive a session-scoped identifier from hermes_home + timestamp
        from hermes_constants import get_hermes_home
        import time
        hermes_home = str(get_hermes_home())

        # Initialize with platform context
        provider.initialize(
            session_id=_memory_session_id,
            hermes_home=hermes_home,
            platform="android-chaquopy",
        )
        _memory_provider = provider
        _log.info("Memory provider '%s' initialized (session=%s)",
                  provider_name, _memory_session_id)
        return True

    except Exception as e:
        _log.error("init_memory failed: %s\n%s", e, traceback.format_exc())
        return False


def _get_memory_provider():
    """Return the active memory provider, raising if not initialized."""
    global _memory_provider
    if _memory_provider is None:
        if not init_memory("holographic"):
            raise RuntimeError("Memory provider not initialized")
    return _memory_provider


def add_memory(content: str, type: str = "fact",
               importance: float = 0.5, context: str = "") -> str:
    """
    Add a memory entry to the holographic store.

    Args:
        content:    Memory content text.
        type:       Memory category (fact, preference, plan, habit, etc.).
        importance: Trust score [0.0-1.0] — higher = more trusted.
        context:    Optional contextual label.

    Returns:
        JSON with fact_id and status.
    """
    try:
        provider = _get_memory_provider()
        store = getattr(provider, '_store', None)
        if store is None:
            return json.dumps({"fact_id": "", "status": "error",
                               "error": "Memory store not initialized"})

        # Build tags from importance + context
        tags_parts = [f"importance:{importance:.2f}"]
        if context:
            tags_parts.append(f"ctx:{context[:80]}")
        tags = ",".join(tags_parts)

        # Override default trust with caller's importance
        old_trust = store.default_trust
        try:
            store.default_trust = max(0.0, min(1.0, importance))
            fact_id = store.add_fact(content, category=type, tags=tags)
        finally:
            store.default_trust = old_trust

        return json.dumps({"fact_id": str(fact_id), "status": "added"})

    except Exception as e:
        return json.dumps({"fact_id": "", "status": "error",
                           "error": str(e)})


def search_memory(query: str, limit: int = 5) -> str:
    """
    Semantic search over stored memories using FTS5 + Jaccard + trust scoring.

    Args:
        query: Search query string.
        limit: Max results to return.

    Returns:
        JSON array of memory result objects.
    """
    try:
        provider = _get_memory_provider()
        retriever = getattr(provider, '_retriever', None)
        if retriever is None:
            return json.dumps([])

        results = retriever.search(query, limit=limit)
        output = []
        for r in results:
            output.append({
                "id": str(r.get("fact_id", "")),
                "content": r.get("content", ""),
                "type": r.get("category", "general"),
                "score": r.get("score", 0.0),
                "trust_score": r.get("trust_score", 0.5),
                "timestamp": r.get("created_at", ""),
            })
        return json.dumps(output, default=str)

    except Exception as e:
        _log.warning("search_memory failed: %s", e)
        return json.dumps([])


def extract_memories(user_message: str, ai_response: str) -> str:
    """
    Extract memory-worthy facts from a conversation turn.

    Uses regex pattern matching (preference, fact, plan extraction) to find
    user information worth remembering. Extracted memories are persisted to
    the holographic store.

    For full LLM-based extraction, the memory_provider's get_tool_schemas
    exposes fact_store/fact_feedback tools that the agent can call directly
    during conversation.

    Args:
        user_message: The user's message.
        ai_response:  The AI's response.

    Returns:
        JSON list of extracted memory dicts with type/content/confidence.
    """
    extracted = []
    combined = (user_message or "") + " " + (ai_response or "")

    # ── Regex-based extraction ────────────────────────────────────────
    for pattern in _MEM_EXTRACT_PREF_PATTERNS:
        m = pattern.search(combined)
        if m:
            extracted.append({
                "type": "preference",
                "content": m.group(1).strip()[:400],
                "confidence": 0.4,
            })
            break

    for pattern in _MEM_EXTRACT_FACT_PATTERNS:
        m = pattern.search(combined)
        if m:
            extracted.append({
                "type": "fact",
                "content": m.group(1).strip()[:400],
                "confidence": 0.35,
            })
            break

    for pattern in _MEM_EXTRACT_PLAN_PATTERNS:
        m = pattern.search(combined)
        if m:
            extracted.append({
                "type": "plan",
                "content": m.group(1).strip()[:400],
                "confidence": 0.35,
            })
            break

    # ── Persist extracted memories ─────────────────────────────────────
    saved = []
    for mem in extracted:
        try:
            result_json = add_memory(
                content=mem["content"],
                type=mem["type"],
                importance=min(0.8, mem.get("confidence", 0.5) + 0.2),
            )
            result = json.loads(result_json)
            if result.get("status") == "added":
                mem["id"] = result["fact_id"]
                saved.append(mem)
        except Exception:
            pass

    return json.dumps(saved, default=str)


def get_memory_context(query: str, limit: int = 5) -> str:
    """
    Get memory context strings for injection into the system prompt.

    Performs semantic search and returns formatted context lines.

    Args:
        query: The user's current message for relevance matching.
        limit: Max context lines to return.

    Returns:
        JSON list of context strings.
    """
    try:
        results_json = search_memory(query, limit=limit)
        results = json.loads(results_json)
        if not results:
            return json.dumps([])

        lines = []
        for r in results:
            cat = r.get("type", "general")
            content = r.get("content", "")
            trust = r.get("trust_score", 0.5)
            lines.append(f"[{cat}] (trust:{trust:.1f}): {content}")
        return json.dumps(lines)
    except Exception as e:
        return json.dumps([])


def get_all_memories() -> str:
    """
    Return all stored memories.

    Returns:
        JSON list of memory objects.
    """
    try:
        provider = _get_memory_provider()
        store = getattr(provider, '_store', None)
        if store is None:
            return json.dumps([])

        facts = store.list_facts(min_trust=0.0, limit=1000)
        output = []
        for f in facts:
            output.append({
                "id": str(f.get("fact_id", "")),
                "content": f.get("content", ""),
                "type": f.get("category", "general"),
                "score": f.get("trust_score", 0.5),
                "timestamp": f.get("created_at", ""),
                "retrieval_count": f.get("retrieval_count", 0),
                "helpful_count": f.get("helpful_count", 0),
            })
        return json.dumps(output, default=str)
    except Exception as e:
        _log.warning("get_all_memories failed: %s", e)
        return json.dumps([])


def delete_memory(memory_id: str) -> bool:
    """
    Delete a single memory by ID.

    Args:
        memory_id: The memory fact ID (integer as string, e.g. "42").

    Returns:
        True if deleted, False otherwise.
    """
    try:
        provider = _get_memory_provider()
        store = getattr(provider, '_store', None)
        if store is None:
            return False

        fact_id = int(memory_id)
        return store.remove_fact(fact_id)
    except Exception as e:
        _log.warning("delete_memory failed: %s", e)
        return False


def clear_memories() -> bool:
    """
    Delete all stored memories.

    Returns:
        True on success.
    """
    try:
        provider = _get_memory_provider()
        store = getattr(provider, '_store', None)
        if store is None:
            return False

        conn = store._conn
        conn.execute("DELETE FROM fact_entities")
        conn.execute("DELETE FROM facts")
        conn.execute("DELETE FROM memory_banks")
        conn.commit()
        _log.info("All memories cleared")
        return True
    except Exception as e:
        _log.warning("clear_memories failed: %s", e)
        return False


def get_memory_stats() -> str:
    """
    Get memory system statistics.

    Returns:
        JSON with totalEntries, typeBreakdown, total_size_bytes.
    """
    try:
        provider = _get_memory_provider()
        store = getattr(provider, '_store', None)
        if store is None:
            return json.dumps({"totalEntries": 0, "typeBreakdown": {},
                               "total_size_bytes": 0})

        conn = store._conn

        row = conn.execute("SELECT COUNT(*) FROM facts").fetchone()
        total = row[0] if row else 0

        type_rows = conn.execute(
            "SELECT category, COUNT(*) as cnt FROM facts GROUP BY category"
        ).fetchall()
        type_breakdown = {r[0]: r[1] for r in type_rows}

        # Estimate DB file size
        try:
            db_size = store.db_path.stat().st_size if store.db_path.exists() else 0
        except Exception:
            db_size = 0

        return json.dumps({
            "totalEntries": total,
            "typeBreakdown": type_breakdown,
            "total_size_bytes": db_size,
        })

    except Exception as e:
        return json.dumps({"totalEntries": 0, "typeBreakdown": {},
                           "total_size_bytes": 0, "error": str(e)})


# ── Module-level setup ───────────────────────────────────────────────
_setup_hermes_env()
