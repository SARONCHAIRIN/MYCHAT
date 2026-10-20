#!/usr/bin/env python3
"""Run from this patch directory: python3 apply_patch.py /path/to/MYCHAT"""
from pathlib import Path
import shutil
import sys

if len(sys.argv) != 2:
    raise SystemExit('Usage: python3 apply_patch.py /path/to/MYCHAT')
repo = Path(sys.argv[1]).resolve()
patch = Path(__file__).resolve().parent
pom = repo / 'pom.xml'
if not pom.is_file():
    raise SystemExit('Target does not look like MYCHAT repo (missing pom.xml)')

# Safe to re-run: use markers and avoid duplicate inserts.
s = pom.read_text()
if 'software.amazon.awssdk' not in s:
    needle = '    </dependencies>'
    assert s.count(needle) == 1
    sdk = '''        <!-- Cloudflare R2 via S3-compatible AWS SDK -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>2.25.70</version>
        </dependency>

'''
    s = s.replace(needle, sdk + needle)
    pom.write_text(s)

for source in (patch / 'src').rglob('*.java'):
    dest = repo / source.relative_to(patch)
    dest.parent.mkdir(parents=True, exist_ok=True)
    if dest.exists():
        backup = dest.with_suffix(dest.suffix + '.before-r2')
        if not backup.exists():
            shutil.copy2(dest, backup)
    shutil.copy2(source, dest)

repo_file = repo / 'src/main/java/com/rindev/chat/repository/MessageAttachmentRepository.java'
s = repo_file.read_text()
if 'findByFileUrl(' not in s:
    s = s.replace('import java.util.List;', 'import java.util.List;\nimport java.util.Optional;\nimport org.springframework.data.jpa.repository.EntityGraph;')
    idx = s.rfind('}')
    s = s[:idx] + '''    @EntityGraph(attributePaths = { "message", "message.conversation" })
    Optional<MessageAttachment> findByFileUrl(String fileUrl);
''' + s[idx:]
    repo_file.write_text(s)

user_repo = repo / 'src/main/java/com/rindev/chat/repository/UserRepository.java'
s = user_repo.read_text()
if 'existsByAvatarUrl(' not in s:
    idx = s.rfind('}')
    s = s[:idx] + '    boolean existsByAvatarUrl(String avatarUrl);\n' + s[idx:]
    user_repo.write_text(s)

config = repo / 'src/main/resources/application.yml'
s = config.read_text()
if 'provider: ${APP_STORAGE_PROVIDER:local}' not in s:
    needle = 'app:\n'
    assert needle in s
    s = s.replace(needle, '''app:
  storage:
    provider: ${APP_STORAGE_PROVIDER:local}
    r2:
      endpoint: ${R2_ENDPOINT:}
      bucket: ${R2_BUCKET:}
      access-key-id: ${R2_ACCESS_KEY_ID:}
      secret-access-key: ${R2_SECRET_ACCESS_KEY:}
''', 1)
    config.write_text(s)
print('Patch applied. Review git diff, run ./mvnw clean test, then commit after reviewing.')
