import json

path = r"C:\Users\Admin\.workbuddy-ai\projects\c-Users-Admin-Desktop-Angry-Birds\8be36690-f796-42d3-9aff-063dcd1c7e6d.jsonl"

calls = {}
order = []
results = {}

for line in open(path, encoding="utf-8", errors="replace"):
    try:
        o = json.loads(line)
    except Exception:
        continue
    t = o.get("type")
    if t == "function_call" and o.get("name") in ("Bash", "PowerShell"):
        args = o.get("arguments")
        if isinstance(args, str):
            try:
                args = json.loads(args)
            except Exception:
                args = {}
        cmd = (args or {}).get("command", "")
        calls[o.get("callId")] = cmd
        order.append(o.get("callId"))
    elif t and "result" in t:
        cid = o.get("callId")
        if cid:
            results[cid] = json.dumps(o.get("result") or o.get("output") or "")[:400]

adb = [c for c in order if "adb" in calls.get(c, "").lower()]
print("adb calls:", len(adb))
# show ones whose result does NOT contain Permission denied / Access is denied
ok = [c for c in adb if "enied" not in results.get(c, "")]
print("clean calls:", len(ok))
for cid in ok[:12]:
    print("=" * 30)
    print("CMD:", calls[cid][:300].replace("\n", " | "))
    print("OUT:", results.get(cid, "<none>")[:200].replace("\\r\\n", " "))
