#!/usr/bin/env python3
"""
Query the RabbitMQ management API for queue depths and write the results as a checkmk
"local check" spool file, for pickup by a checkmk agent on a host that can see OUTPUT_DIR.

Run by the queue-monitor CronJob (helm/templates/queue-monitor-cronjob.yaml). Uses only the
Python standard library. NOTE: this file is rendered by helm 'tpl' (via configmap.yaml), so it
must never contain two consecutive opening curly braces.

Environment variables:
  RMQ_HOST, RMQ_PORT, RMQ_VHOST, RMQ_USER, RMQ_PASS   management API connection
  OUTPUT_DIR            directory to write into (the checkmk agent's spool dir, or equivalent)
  OUTPUT_NAME           spool file name, e.g. "600_myrelease_rmq_queues". The leading number is
                        the max age (seconds) after which the agent ignores the file.
  SERVICE_PREFIX        prefix for checkmk service names
  MESSAGES_WARN, MESSAGES_CRIT   thresholds on total messages per queue
"""
import base64
import json
import os
import sys
import tempfile
import urllib.parse
import urllib.request

host = os.environ.get("RMQ_HOST", "localhost")
port = os.environ.get("RMQ_PORT", "15672")
vhost = os.environ.get("RMQ_VHOST", "/")
user = os.environ.get("RMQ_USER", "guest")
password = os.environ.get("RMQ_PASS", "guest")
output_dir = os.environ.get("OUTPUT_DIR", "/spool")
output_name = os.environ.get("OUTPUT_NAME", "600_rmq_queues")
prefix = os.environ.get("SERVICE_PREFIX", "DataONE Indexer RMQ")
warn = int(os.environ.get("MESSAGES_WARN", "200"))
crit = int(os.environ.get("MESSAGES_CRIT", "1000"))

COLUMNS = "name,messages,messages_ready,messages_unacknowledged,consumers,message_bytes"


def fetch_queues():
    url = "http://%s:%s/api/queues/%s?columns=%s" % (
        host, port, urllib.parse.quote(vhost, safe=""), COLUMNS)
    token = base64.b64encode(("%s:%s" % (user, password)).encode()).decode()
    req = urllib.request.Request(url, headers={"Authorization": "Basic " + token})
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.load(resp)


def clean(name):
    # checkmk service names are quoted; strip characters that would break the line
    return "".join(c if c.isprintable() and c != '"' else "_" for c in name)


def render(queues):
    lines = []
    for q in sorted(queues, key=lambda q: q.get("name", "")):
        name = clean(q.get("name", "?"))
        messages = int(q.get("messages") or 0)
        ready = int(q.get("messages_ready") or 0)
        unacked = int(q.get("messages_unacknowledged") or 0)
        consumers = int(q.get("consumers") or 0)
        nbytes = int(q.get("message_bytes") or 0)

        # 'P' lets checkmk compute the state from the warn;crit levels in the perfdata
        perf = "messages=%d;%d;%d|ready=%d|unacked=%d|consumers=%d|message_bytes=%d" % (
            messages, warn, crit, ready, unacked, consumers, nbytes)
        lines.append('P "%s queue %s" %s %d messages (%d ready, %d unacked), %d consumers' % (
            prefix, name, perf, messages, ready, unacked, consumers))

        if consumers == 0 and messages > 0:
            state, text = 2, "no consumers, but %d messages waiting" % messages
        else:
            state, text = 0, "%d consumers" % consumers
        lines.append('%d "%s consumers %s" consumers=%d %s' % (
            state, prefix, name, consumers, text))
    return lines


def write_atomic(content):
    os.makedirs(output_dir, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=".%s." % output_name, suffix=".tmp", dir=output_dir)
    try:
        with os.fdopen(fd, "w") as f:
            f.write(content)
        os.chmod(tmp, 0o644)
        os.replace(tmp, os.path.join(output_dir, output_name))
    except BaseException:
        if os.path.exists(tmp):
            os.unlink(tmp)
        raise


def main():
    try:
        lines = render(fetch_queues())
        if not lines:
            lines = ['1 "%s API" - No queues found in vhost %s' % (prefix, vhost)]
        else:
            lines.append('0 "%s API" - RabbitMQ management API OK at %s:%s' % (prefix, host, port))
        rc = 0
    except Exception as e:
        lines = ['2 "%s API" - RabbitMQ management API unreachable at %s:%s: %s' % (
            prefix, host, port, clean(str(e)))]
        rc = 1

    content = "<<<local:sep(0)>>>\n" + "\n".join(lines) + "\n"
    write_atomic(content)
    sys.stdout.write(content)
    return rc


if __name__ == "__main__":
    sys.exit(main())
