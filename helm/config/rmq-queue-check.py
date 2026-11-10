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
    """Fetch per-queue statistics from the RabbitMQ management HTTP API.

    Calls GET /api/queues/<vhost> on RMQ_HOST:RMQ_PORT using HTTP basic auth, and requests only
    the fields listed in COLUMNS, to keep the response small.

    Returns:
        list[dict]: One dict per queue in the vhost, containing the COLUMNS fields (e.g.
        "name", "messages", "consumers"). Fields may be missing or None for a queue that
        RabbitMQ has not yet collected stats for.

    Raises:
        urllib.error.URLError: If the API cannot be reached, or returns an HTTP error status
            (urllib.error.HTTPError is a subclass), e.g. 401 for bad credentials.
        TimeoutError: If the API does not respond within 20 seconds.
        json.JSONDecodeError: If the response body is not valid JSON.
    """
    url ="http://%s:%s/api/queues/%s?columns=%s" % (
        host, port, urllib.parse.quote(vhost, safe=""), COLUMNS)
    token = base64.b64encode(("%s:%s" % (user, password)).encode()).decode()
    req = urllib.request.Request(url, headers={"Authorization": "Basic " + token})
    with urllib.request.urlopen(req, timeout=20) as resp:
        return json.load(resp)


def clean(name):
    """Make a string safe to embed in a checkmk local-check line.

    checkmk service names are enclosed in double quotes, and each check must fit on one line, so
    double quotes and non-printable characters (including newlines) are replaced.

    Args:
        name (str): The text to sanitize, e.g. a queue name or an error message.

    Returns:
        str: ``name`` with each double quote and non-printable character replaced by "_".
    """
    return "".join(c if c.isprintable() and c != '"' else "_" for c in name)


def render(queues):
    """Convert queue statistics into checkmk local-check lines.

    Produces two services per queue, sorted by queue name:

    * "<prefix> queue <name>": state "P", so that checkmk computes OK/WARN/CRIT from the
      MESSAGES_WARN/MESSAGES_CRIT levels on the "messages" metric. Also reports ready, unacked,
      consumers and message_bytes as metrics, for graphing.
    * "<prefix> consumers <name>": CRIT (2) if messages are waiting but no consumers are
      attached, otherwise OK (0).

    Args:
        queues (list[dict]): Queue statistics, as returned by fetch_queues(). Missing or None
            numeric fields are treated as 0.

    Returns:
        list[str]: The local-check lines, without the section header or trailing newlines.
        Empty if ``queues`` is empty.
    """
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
    """Atomically write the spool file OUTPUT_DIR/OUTPUT_NAME.

    Writes to a hidden temporary file in the same directory, then renames it over the target, so
    the checkmk agent never reads a partially written file. The file is made world-readable
    (0644) so the agent can read it regardless of its uid. The temporary file is removed if
    anything fails.

    Args:
        content (str): The complete file content, including the <<<local>>> section header.

    Raises:
        OSError: If OUTPUT_DIR cannot be created, or the file cannot be written or renamed,
            e.g. because the directory is not writable by this process.
    """
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
    """Query RabbitMQ, write the checkmk spool file, and echo it to stdout (for the job log).

    In addition to the per-queue services from render(), always reports a "<prefix> API"
    service, which is:

    * OK (0) if the management API responded and at least one queue exists,
    * WARN (1) if it responded but the vhost has no queues,
    * CRIT (2) if the API could not be queried. In this case, only the API service is written,
      so the per-queue services go stale in checkmk instead of reporting old values.

    Returns:
        int: Process exit code; 0 on success, 1 if the API query failed (so that the k8s Job is
        marked as failed). A failure to write the spool file is not caught, and propagates as
        an exception (also failing the Job).
    """
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
