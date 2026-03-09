#!/usr/bin/env python3
"""
Travelmate Claude Code Hooks Script

Handles lifecycle events from Claude Code sessions.
Events are received via environment variables set by Claude Code.

Environment Variables:
  CLAUDE_HOOK_EVENT    - The hook event type (PreToolUse, PostToolUse, etc.)
  CLAUDE_TOOL_NAME     - The tool being used (for tool-related hooks)
  CLAUDE_PROJECT_DIR   - The project root directory
  CLAUDE_SESSION_ID    - Current session identifier
"""

import json
import os
import sys
from datetime import datetime
from pathlib import Path


def get_project_dir() -> Path:
    """Get the project directory from environment."""
    return Path(os.environ.get("CLAUDE_PROJECT_DIR", os.getcwd()))


def get_hook_event() -> str:
    """Get the current hook event type."""
    return os.environ.get("CLAUDE_HOOK_EVENT", "Unknown")


def get_tool_name() -> str:
    """Get the tool name for tool-related hooks."""
    return os.environ.get("CLAUDE_TOOL_NAME", "")


def log_event(event: str, details: str = "") -> None:
    """Log hook events to a session log file."""
    log_dir = get_project_dir() / ".claude" / "hooks" / "logs"
    log_dir.mkdir(parents=True, exist_ok=True)

    timestamp = datetime.now().isoformat()
    session_id = os.environ.get("CLAUDE_SESSION_ID", "unknown")
    log_file = log_dir / f"session-{session_id[:8]}.log"

    entry = f"[{timestamp}] {event}"
    if details:
        entry += f" | {details}"
    entry += "\n"

    with open(log_file, "a") as f:
        f.write(entry)


def check_security_rules(tool_name: str, tool_input: str = "") -> dict:
    """
    Pre-tool-use security checks.
    Returns a dict with 'allow' (bool) and 'message' (str).
    """
    # Block dangerous patterns in Bash commands
    if tool_name == "Bash":
        dangerous_patterns = [
            "rm -rf /",
            "DROP DATABASE",
            "DROP TABLE",
            "TRUNCATE",
            "--no-verify",
            "force-push",
        ]
        for pattern in dangerous_patterns:
            if pattern.lower() in tool_input.lower():
                return {
                    "allow": False,
                    "message": f"Blocked: dangerous pattern '{pattern}' detected in Bash command",
                }

    # Warn on file writes to security-sensitive paths
    if tool_name in ("Write", "Edit"):
        sensitive_paths = [
            "SecurityConfig",
            "application.yml",
            "application-prod.yml",
            "docker-compose.yml",
            ".env",
        ]
        for path in sensitive_paths:
            if path in tool_input:
                log_event("SECURITY_WARN", f"Modification to sensitive file: {path}")

    return {"allow": True, "message": ""}


def on_pre_tool_use() -> None:
    """Handle PreToolUse events."""
    tool_name = get_tool_name()
    tool_input = os.environ.get("CLAUDE_TOOL_INPUT", "")
    log_event("PreToolUse", f"tool={tool_name}")

    result = check_security_rules(tool_name, tool_input)
    if not result["allow"]:
        log_event("BLOCKED", result["message"])
        print(json.dumps({"blocked": True, "message": result["message"]}))
        sys.exit(1)


def on_post_tool_use() -> None:
    """Handle PostToolUse events."""
    tool_name = get_tool_name()
    log_event("PostToolUse", f"tool={tool_name}")


def on_session_start() -> None:
    """Handle SessionStart events."""
    log_event("SessionStart", f"project={get_project_dir()}")


def on_session_end() -> None:
    """Handle SessionEnd events."""
    log_event("SessionEnd")


def on_stop() -> None:
    """Handle Stop events — agent has finished."""
    log_event("Stop")


def main() -> None:
    """Main entry point — route to appropriate handler."""
    event = get_hook_event()

    handlers = {
        "PreToolUse": on_pre_tool_use,
        "PostToolUse": on_post_tool_use,
        "PostToolUseFailure": lambda: log_event("PostToolUseFailure", get_tool_name()),
        "PermissionRequest": lambda: log_event("PermissionRequest", get_tool_name()),
        "UserPromptSubmit": lambda: log_event("UserPromptSubmit"),
        "Notification": lambda: log_event("Notification"),
        "Stop": on_stop,
        "SubagentStart": lambda: log_event("SubagentStart"),
        "SubagentStop": lambda: log_event("SubagentStop"),
        "PreCompact": lambda: log_event("PreCompact"),
        "SessionStart": on_session_start,
        "SessionEnd": on_session_end,
        "Setup": lambda: log_event("Setup"),
        "TeammateIdle": lambda: log_event("TeammateIdle"),
        "TaskCompleted": lambda: log_event("TaskCompleted"),
        "ConfigChange": lambda: log_event("ConfigChange"),
        "WorktreeCreate": lambda: log_event("WorktreeCreate"),
        "WorktreeRemove": lambda: log_event("WorktreeRemove"),
        "InstructionsLoaded": lambda: log_event("InstructionsLoaded"),
    }

    handler = handlers.get(event, lambda: log_event("Unknown", event))

    # Parse --agent flag for agent-specific hooks
    if "--agent=" in " ".join(sys.argv):
        agent_name = next(
            (arg.split("=")[1] for arg in sys.argv if arg.startswith("--agent=")),
            "unknown",
        )
        log_event(event, f"agent={agent_name}")
    else:
        handler()


if __name__ == "__main__":
    main()
