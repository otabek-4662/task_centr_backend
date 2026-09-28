import os
import re

src_dir = r"c:\Users\Bekmurod\Desktop\task_center_backend\src\main\java\com\taskcenter"
dto_dir = os.path.join(src_dir, "dto")
mapper_dir = os.path.join(src_dir, "mapper")

if not os.path.exists(mapper_dir):
    os.makedirs(mapper_dir)

renames = {
    "TaskDto": "TaskResponseDto",
    "WorkspaceDto": "WorkspaceResponseDto",
    "SprintDto": "SprintResponseDto",
    "ColumnDto": "ColumnResponseDto",
    "UserDto": "UserResponseDto",
    "CommentDto": "CommentResponseDto",
    "LabelDto": "LabelResponseDto",
    "AttachmentDto": "AttachmentResponseDto",
    "WorkspaceListDto": "WorkspaceListResponseDto",
    "TaskActivityDto": "TaskActivityResponseDto",
    "WorkspaceMemberResponseDto": "WorkspaceMemberResponseDto"
}

mapper_methods = []

for old in renames.keys():
    filepath = os.path.join(dto_dir, old + ".java")
    if not os.path.exists(filepath):
        continue
    with open(filepath, "r", encoding="utf-8") as f:
        content = f.read()

    while True:
        pattern = r'(public\s+static\s+[\w<>]+\s+fromEntity\s*\([^)]*\)\s*\{)'
        match = re.search(pattern, content)
        if not match:
            break
        
        start_idx = match.start()
        brace_count = 0
        end_idx = start_idx
        for i in range(match.end() - 1, len(content)):
            char = content[i]
            if char == '{':
                brace_count += 1
            elif char == '}':
                brace_count -= 1
                if brace_count == 0:
                    end_idx = i + 1
                    break
        
        method_body = content[start_idx:end_idx]
        content = content[:start_idx] + content[end_idx:]
        mapper_methods.append(method_body)

    with open(filepath, "w", encoding="utf-8") as f:
        f.write(content)

fixed_methods = []
for m in mapper_methods:
    m_match = re.match(r'public\s+static\s+(\w+)\s+fromEntity', m)
    if m_match:
        ret_type = m_match.group(1)
        new_name = "to" + renames.get(ret_type, ret_type)
        m = re.sub(r'public\s+static\s+' + ret_type + r'\s+fromEntity', 
                   r'public static ' + ret_type + r' ' + new_name, m, count=1)
    
    for old, new in renames.items():
        if old != new:
            m = re.sub(r'\b' + old + r'\b', new, m)
    
    for old, new in renames.items():
        m = re.sub(new + r'::fromEntity', 'DtoMapper::to' + new, m)
        m = re.sub(new + r'\.fromEntity', 'DtoMapper.to' + new, m)
    fixed_methods.append(m)

mapper_content = """package com.taskcenter.mapper;

import com.taskcenter.dto.*;
import com.taskcenter.model.*;
import java.util.List;
import java.util.stream.Collectors;

public class DtoMapper {
"""
for m in fixed_methods:
    mapper_content += "\n    " + m.replace("\n", "\n    ") + "\n"
mapper_content += "}\n"

with open(os.path.join(mapper_dir, "DtoMapper.java"), "w", encoding="utf-8") as f:
    f.write(mapper_content)

for root, dirs, files in os.walk(src_dir):
    for file in files:
        if file.endswith(".java"):
            filepath = os.path.join(root, file)
            with open(filepath, "r", encoding="utf-8") as f:
                content = f.read()
            
            new_content = content
            for old, new in renames.items():
                new_content = re.sub(r'([A-Za-z0-9_]+\.)*\b' + old + r'::fromEntity', 'com.taskcenter.mapper.DtoMapper::to' + new, new_content)
                new_content = re.sub(r'([A-Za-z0-9_]+\.)*\b' + old + r'\.fromEntity', 'com.taskcenter.mapper.DtoMapper.to' + new, new_content)
            
            for old, new in renames.items():
                if old != new:
                    new_content = re.sub(r'(?<!AuthResponse\.)\b' + old + r'\b', new, new_content)
                
            if new_content != content:
                with open(filepath, "w", encoding="utf-8") as f:
                    f.write(new_content)

for old, new in renames.items():
    if old != new:
        old_path = os.path.join(dto_dir, old + ".java")
        new_path = os.path.join(dto_dir, new + ".java")
        if os.path.exists(old_path):
            os.rename(old_path, new_path)

print("Refactoring complete.")
