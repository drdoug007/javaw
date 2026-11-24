"""Live progress for an unattended kit session, read from Claude Code's stream-json output.

The session is a fixed loop (AGENT.md): read the directions, the task and its tests; implement; run
`kit check` until it prints PASS; write the report; run `kit finish`. Progress follows those phases:

    reading      0-15%   Read of AGENT.md, RULES.md, the task file and its test classes
    implementing 15-40%  edits before the first `kit check` (grows with each edit)
    checking     40-88%  share of the task's test methods that passed in the latest `kit check`
    report       88%     the report under javaw-agent-kit/reports/ is written
    finishing    90-100% `kit finish` stages (== k/7); done at PASS, or "blocked"

The percentage is an estimate of where the session is in that loop, not of time left. Each event is printed
as one line, and a heartbeat line appears when the model has been quiet for a while (local models think slowly).
"""
import json
import re
import time

HEARTBEAT_SECONDS = 120


def server_error(event):
    """The error text if a stream-json result event reports a failed API call rather than the agent's own ending.

    A request the model server refuses (for example oMLX's 507 when the model does not fit in memory) ends the
    session after one turn with a result such as `API Error: 507 ...`, which Claude Code may still label
    `success`."""
    text = str(event.get("result") or "").strip()
    if event.get("api_error_status") or text.startswith("API Error"):
        return text or "API error %s" % event.get("api_error_status")
    if event.get("is_error") and event.get("subtype") == "success":
        return text or "is_error result"
    return None


def count_tests(paths):
    """Number of @Test methods in the given test sources (0 if none can be read)."""
    total = 0
    for path in paths:
        try:
            total += len(re.findall(r"@Test\b", path.read_text()))
        except OSError:
            pass
    return total


class Progress:
    def __init__(self, task_id, total_tests, timeout_minutes, out, clock=time.monotonic):
        self.task_id = task_id
        self.total = total_tests
        self.timeout = timeout_minutes * 60
        self.out = out
        self.clock = clock
        self.started = clock()
        self.last_output = self.started
        self.percent = 0
        self.phase = "starting"
        self.edits = 0
        self.checks = 0
        self.passing = None
        self.pending = {}      # tool_use_id -> (tool name, input)
        self.finished = None   # "done" or "blocked" once known
        self.server_error = None  # the error text when the model server failed the session (not the agent)

    # -- formatting -------------------------------------------------------------------------------------------
    def elapsed(self):
        seconds = int(self.clock() - self.started)
        return "%d:%02d" % (seconds // 60, seconds % 60)

    def emit(self, message):
        line = "[%s +%s %3d%% %s] %s" % (self.task_id, self.elapsed(), self.percent, self.phase, message)
        self.out.write(line + "\n")
        self.out.flush()
        self.last_output = self.clock()

    def advance(self, phase, percent):
        self.phase = phase
        self.percent = max(self.percent, min(100, int(percent)))

    # -- stream events ----------------------------------------------------------------------------------------
    def handle_line(self, line):
        """Handles one stream-json line; returns a human-readable transcript line for the session log, or None."""
        line = line.strip()
        if not line:
            return None
        try:
            event = json.loads(line)
        except ValueError:
            return line
        kind = event.get("type")
        if kind == "system" and event.get("subtype") == "init":
            self.advance("reading", 1)
            self.emit("session started (model %s)" % event.get("model", "?"))
            return "[init] model %s" % event.get("model", "?")
        if kind == "assistant":
            parts = []
            for block in event.get("message", {}).get("content", []) or []:
                if block.get("type") == "text" and block.get("text", "").strip():
                    parts.append(block["text"].strip())
                elif block.get("type") == "tool_use":
                    self.pending[block.get("id")] = (block.get("name"), block.get("input") or {})
                    summary = self.tool_started(block.get("name"), block.get("input") or {})
                    parts.append("[tool] " + summary)
            return "\n".join(parts) or None
        if kind == "user":
            parts = []
            for block in event.get("message", {}).get("content", []) or []:
                if isinstance(block, dict) and block.get("type") == "tool_result":
                    name, tool_input = self.pending.pop(block.get("tool_use_id"), (None, {}))
                    text = result_text(block.get("content"))
                    self.tool_finished(name, tool_input, text, bool(block.get("is_error")))
                    parts.append("[result] " + text[:4000])
            return "\n".join(parts) or None
        if kind == "result":
            turns = event.get("num_turns")
            error = server_error(event)
            if error:
                self.server_error = error
                self.emit("session failed: model server error (%s)" % error[:160])
            else:
                self.emit("session ended (%s%s)" % (event.get("subtype", "?"), ", %s turns" % turns if turns else ""))
            return "[result] %s" % (event.get("result") or event.get("subtype"))
        return None

    def heartbeat(self):
        """Prints a 'still working' line when nothing was printed for a while."""
        now = self.clock()
        if now - self.last_output >= HEARTBEAT_SECONDS:
            left = max(0, self.timeout - (now - self.started))
            self.emit("still working (%d min left before the session times out)" % (left // 60))

    # -- tools ------------------------------------------------------------------------------------------------
    def tool_started(self, name, tool_input):
        path = tool_input.get("file_path") or tool_input.get("path") or ""
        short = path.split("/")[-1] if path else ""
        if name == "Read":
            if self.phase in ("starting", "reading"):
                self.advance("reading", min(15, self.percent + 3))
            self.emit("read " + short)
            return "Read " + path
        if name in ("Edit", "Write", "MultiEdit", "NotebookEdit"):
            if "/reports/" in path:
                self.advance("report", 88)
                self.emit("write report " + short)
            else:
                self.edits += 1
                if self.checks == 0:
                    self.advance("implementing", min(40, 15 + 3 * self.edits))
                self.emit("edit " + short)
            return "%s %s" % (name, path)
        if name == "Bash":
            command = tool_input.get("command", "")
            kit = re.search(r"bin/kit\s+(\w+)", command)
            if kit:
                sub = kit.group(1)
                if sub == "finish":
                    self.advance("finishing", 90)
                    self.emit("kit finish (tests, full suite, audit, commit); this takes several minutes")
                elif sub == "check":
                    self.emit("kit check (running the task tests)")
                elif sub == "block":
                    self.emit("kit block")
                else:
                    self.emit("kit " + sub)
            else:
                self.emit("run " + command.splitlines()[0][:100] if command else "run")
            return "Bash " + command
        if name in ("Grep", "Glob"):
            self.emit("%s %s" % (name.lower(), tool_input.get("pattern", "")))
            return "%s %s" % (name, tool_input.get("pattern", ""))
        if name == "TodoWrite":
            return "TodoWrite"
        self.emit(name or "tool")
        return name or "tool"

    def tool_finished(self, name, tool_input, text, error):
        if name != "Bash":
            return
        command = tool_input.get("command", "")
        if re.search(r"bin/kit\s+check", command):
            self.checks += 1
            self.check_result(text)
        elif re.search(r"bin/kit\s+finish", command):
            stages = re.findall(r"== (\d+)/(\d+)", text)
            if stages:
                done, total = map(int, stages[-1])
                self.advance("finishing", 90 + 10 * done / max(1, total) - 1)
            if re.search(r"^DONE ", text, re.M):
                self.finished = "done"
                self.advance("done", 100)
                self.emit("finished")
            else:
                self.emit("kit finish did not complete: " + last_line(text))
        elif re.search(r"bin/kit\s+block", command):
            self.finished = "blocked"
            self.phase = "blocked"
            self.emit("blocked: " + last_line(text))

    def check_result(self, text):
        if re.search(r"^PASS", text, re.M):
            self.passing = self.total
            self.advance("checking", 88)
            self.emit("check %d: PASS (all %s tests)" % (self.checks, self.total or "the"))
            return
        failing = []
        for line in text.splitlines():
            if line.startswith("FAIL"):
                continue
            match = re.match(r"\s*\d+: FAIL (.*)", line)
            if match:
                failing = [name.strip() for name in match.group(1).split(",") if name.strip()]
        if self.total:
            self.passing = max(0, self.total - len(failing)) if failing else None
        if self.passing is not None and self.total:
            self.advance("checking", 40 + 48 * self.passing / self.total)
            names = ", ".join(failing[:3]) + (" ..." if len(failing) > 3 else "")
            self.emit("check %d: %d/%d tests pass; failing %s" % (self.checks, self.passing, self.total, names))
        else:
            self.advance("checking", 40)
            self.emit("check %d: FAIL (%s)" % (self.checks, last_line(text)))


def result_text(content):
    if isinstance(content, str):
        return content
    if isinstance(content, list):
        return "\n".join(item.get("text", "") for item in content if isinstance(item, dict))
    return ""


def last_line(text):
    lines = [line.strip() for line in text.splitlines() if line.strip()]
    return lines[-1][:160] if lines else "no output"
