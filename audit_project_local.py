import json
import os
import subprocess
import sys
import time

AUDIT_DIR = "/home/user/jpom-project/Jpom/.tmp/audit"
sys.path.insert(0, AUDIT_DIR)
import audit_codes as ac

ac.GO_BASE = "http://127.0.0.1:2131"
cached = {r["case"]: r for r in json.load(open(os.path.join(AUDIT_DIR, "audit_codes.json")))}
wanted = [c for c in ac.CASES if any(k in c[0] for k in ["getProjectStatus", "operate", "saveProject", "getProjectPort"])]

only = "--start" not in sys.argv
total = 0
bad = 0
if only:
    for case in wanted:
        name = case[0]
        status, body = ac.go_req(case)
        gc, gm = ac.parse(body)
        java = cached[name]["java"]
        match = java["code"] == gc and java["msg"] == gm
        total += 1
        if not match:
            bad += 1
        print("%-32s java=%-4s go=%-4s %s" % (name, java["code"], gc, "OK" if match else "DIFF"))
        if not match:
            print("   java.msg=%r go.msg=%r" % (java["msg"], gm))
    print("PROJECT AUDIT: total %d, diff %d" % (total, bad))
    sys.exit(1 if bad else 0)

