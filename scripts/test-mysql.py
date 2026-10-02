#!/usr/bin/env python3
"""Run the full suite on a fresh local MySQL container; never touches chat_db.

Requires Docker, Java 21 and Maven dependencies. Extra arguments go to `mvnw test`.
Container credentials are random, retained in process memory, and removed with
the disposable container in a finally block. No existing credentials are read.
"""
import os
from pathlib import Path
import secrets
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[1]
os.chdir(ROOT)
env = os.environ.copy()
name = "chat-verification-" + uuid.uuid4().hex[:12]
env["MYSQL_ROOT_PASSWORD"] = secrets.token_urlsafe(32)
env["DB_PASSWORD"] = env["MYSQL_ROOT_PASSWORD"]
env["DB_USERNAME"] = "root"
env["CHAT_WRITE_TESTS"] = "true"
env["CHAT_DISPOSABLE_DATABASE"] = "true"
env["FLYWAY_ENABLED"] = "true"

def run(args, **kwargs):
    return subprocess.run(args, env=env, check=True, **kwargs)

try:
    run(["docker", "run", "--detach", "--rm", "--name", name,
         "--publish", "127.0.0.1::3306", "--env", "MYSQL_ROOT_PASSWORD",
         "--env", "MYSQL_DATABASE=chat_verification", "mysql:lts"], stdout=subprocess.DEVNULL)
    port = run(["docker", "port", name, "3306"], capture_output=True, text=True).stdout.strip().rsplit(":", 1)[1]
    env["DB_URL"] = (f"jdbc:mysql://127.0.0.1:{port}/chat_verification?sslMode=DISABLED"
                     "&allowPublicKeyRetrieval=true&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true")
    for attempt in range(90):
        probe = subprocess.run(["docker", "exec", name, "mysqladmin", "ping", "--silent"],
                               env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
        if probe.returncode == 0:
            # Initialization starts a temporary socket-only server; wait for TCP as well.
            tcp = subprocess.run(["docker", "exec", name, "mysqladmin", "ping", "--host=127.0.0.1", "--silent"],
                                 env=env, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            if tcp.returncode == 0:
                break
        time.sleep(1)
    else:
        raise RuntimeError("Disposable MySQL did not become ready")
    run(["./mvnw", "--no-transfer-progress", "clean", "compile"])
    run(["./mvnw", "--no-transfer-progress", "dependency:build-classpath",
         "-Dmdep.outputFile=target/test-classpath.txt", "-Dmdep.includeScope=test"])
    java_home = env.get("JAVA_HOME")
    java = str(Path(java_home) / "bin/java") if java_home else "java"
    run([java, "--class-path", Path("target/test-classpath.txt").read_text().strip(),
         "scripts/PrepareTestDatabase.java"])
    run(["./mvnw", "--no-transfer-progress", "test", *sys.argv[1:]])
finally:
    subprocess.run(["docker", "rm", "--force", name], env=env,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
