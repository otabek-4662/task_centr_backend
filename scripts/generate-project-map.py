#!/usr/bin/env python3
import os
import re

ROOT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC_JAVA = os.path.join(ROOT_DIR, 'src', 'main', 'java', 'com', 'taskcenter')
MIGRATIONS_DIR = os.path.join(ROOT_DIR, 'src', 'main', 'resources', 'db', 'migration')
DOCS_DIR = os.path.join(ROOT_DIR, 'docs')
OUTPUT_FILE = os.path.join(DOCS_DIR, 'PROJECT_MAP.md')

def get_files_in(subfolder):
    p = os.path.join(SRC_JAVA, subfolder)
    if not os.path.exists(p):
        return []
    return sorted([f for f in os.listdir(p) if f.endswith('.java')])

def get_migrations():
    if not os.path.exists(MIGRATIONS_DIR):
        return []
    return sorted([f for f in os.listdir(MIGRATIONS_DIR) if f.endswith('.sql')])

def main():
    entities = get_files_in('model')
    controllers = get_files_in('controller')
    services = [f for f in get_files_in('service') if not f.endswith('Impl.java')]
    repositories = get_files_in('repository')
    migrations = get_migrations()

    lines = [
        "# Task Center Backend — Loyiha Xaritasi (Project Map)",
        "",
        "> Ushbu hujjat loyihadagi barcha JPA Entitylar, Controllerlar, Servislar, Repositoriylar va Flyway migratsiyalarining to'liq xaritasini o'z ichiga oladi.",
        "",
        f"## 1. JPA Entitylar (`model/*.java`) — Jami: {len(entities)} ta",
        ""
    ]
    for e in entities:
        lines.append(f"- `{e}`")

    lines.extend([
        "",
        f"## 2. REST & WebSocket Controllerlar (`controller/*.java`) — Jami: {len(controllers)} ta",
        ""
    ])
    for c in controllers:
        lines.append(f"- `{c}`")

    lines.extend([
        "",
        f"## 3. Asosiy Servislar (`service/*.java`) — Jami: {len(services)} ta",
        ""
    ])
    for s in services:
        lines.append(f"- `{s}`")

    lines.extend([
        "",
        f"## 4. Repositoriylar (`repository/*.java`) — Jami: {len(repositories)} ta",
        ""
    ])
    for r in repositories:
        lines.append(f"- `{r}`")

    lines.extend([
        "",
        f"## 5. Flyway DB Migratsiyalari (`db/migration/*.sql`) — Jami: {len(migrations)} ta",
        ""
    ])
    for m in migrations:
        lines.append(f"- `{m}`")

    os.makedirs(DOCS_DIR, exist_ok=True)
    with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')

    print(f"Project map successfully generated at: {OUTPUT_FILE}")

if __name__ == '__main__':
    main()
