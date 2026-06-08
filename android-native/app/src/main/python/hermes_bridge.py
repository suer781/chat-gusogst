"""
Hermes Agent Android Bridge (Minimal Stub)
===========================================
This is a stub implementation for build purposes.
Full Hermes Agent functionality requires the actual source modules.
"""
import json
import logging
import os
import sys

# ── Path setup ─────────────────────────────────────────────────────────
_HERMES_ROOT = "/opt/hermes"
if _HERMES_ROOT not in sys.path:
    sys.path.insert(0, _HERMES_ROOT)

# ── Logging ────────────────────────────────────────────────────────────
_log = logging.getLogger("hermes_bridge")
_log.setLevel(logging.DEBUG)
if not _log.handlers:
    _ch = logging.StreamHandler()
    _ch.setFormatter(logging.Formatter("[HermesBridge] %(message)s"))
    _log.addHandler(_ch)

# ── Global state ───────────────────────────────────────────────────────
_config = {
    "api_key": "",
    "base_url": "",
    "provider": "",
    "model": "",
}
_stream_callbacks = []

# ── Module-level setup ───────────────────────────────────────────────
os.environ.setdefault("HERMES_HOME", os.path.expanduser("~/.hermes"))
os.environ.setdefault("HERMES_HEADLESS", "1")
os.environ.setdefault("HERMES_ANDROID", "1")

# ── Public API ─────────────────────────────────────────────────────────

def init(api_key: str = "", base_url: str = "", provider: str = "",
         model: str = "gpt-4o"):
    _config["api_key"] = api_key or _config.get("api_key", "")
    _config["base_url"] = base_url or _config.get("base_url", "")
    _config["provider"] = provider or _config.get("provider", "")
    _config["model"] = model or _config.get("model", "gpt-4o")
    _log.info("Hermes Bridge initialized (stub): base_url=%s model=%s",
              _config["base_url"], _config["model"])

def _register_stream_callback(cb):
    if cb not in _stream_callbacks:
        _stream_callbacks.append(cb)

def _clear_stream_callbacks():
    _stream_callbacks.clear()

def send_message(provider_id: str, model: str, messages_json: str,
                 tools_json: str = "[]") -> str:
    return json.dumps({
        "ok": False,
        "content": "",
        "tool_calls": [],
        "error": "Hermes Agent not available (stub build)"
    })

def execute_tool(tool_name: str, arguments_json: str = "{}") -> str:
    return json.dumps({
        "ok": False,
        "result": "",
        "error": "Hermes Agent not available (stub build)"
    })

def get_platforms() -> str:
    return json.dumps([])

def connect_platform(platform_id: str, config_json: str = "{}") -> str:
    return json.dumps({
        "ok": False,
        "status": "error",
        "error": "Hermes Agent not available (stub build)"
    })

def _verify_environment() -> str:
    return json.dumps({
        "stub": True,
        "sys_path": sys.path[:5],
        "python_version": sys.version,
        "hermes_root_exists": os.path.isdir(_HERMES_ROOT),
    })

def init_memory(provider_name: str = "holographic") -> bool:
    return False

def add_memory(content: str, type: str = "fact",
               importance: float = 0.5, context: str = "") -> str:
    return json.dumps({"fact_id": "", "status": "error", "error": "stub"})

def search_memory(query: str, limit: int = 5) -> str:
    return json.dumps([])

def extract_memories(user_message: str, ai_response: str) -> str:
    return json.dumps([])

def get_memory_context(query: str, limit: int = 5) -> str:
    return json.dumps([])

def get_all_memories() -> str:
    return json.dumps([])

def delete_memory(memory_id: str) -> bool:
    return False

def clear_memories() -> bool:
    return False

def get_memory_stats() -> str:
    return json.dumps({"totalEntries": 0, "typeBreakdown": {}, "total_size_bytes": 0})
