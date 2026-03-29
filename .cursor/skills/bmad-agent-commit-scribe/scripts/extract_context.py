#!/usr/bin/env python3
import subprocess
import re
import os
import sys
from pathlib import Path

def run_command(cmd):
    try:
        result = subprocess.run(cmd, shell=True, capture_output=True, text=True, check=True)
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        return ""

def get_branch_name():
    return run_command("git rev-parse --abbrev-ref HEAD")

def get_git_diff():
    # Get staged changes first, if none, get unstaged
    diff = run_command("git diff --cached")
    if not diff:
        diff = run_command("git diff")
    return diff

def parse_branch(branch):
    # Example: feat/lin-1-1-1-start-template
    # Pattern: type/issue_id-story_id-title
    pattern = r"^(?P<type>[^/]+)/(?P<issue_id>[a-zA-Z]+-\d+)-(?P<story_id>\d+-\d+)-?(?P<rest>.*)$"
    match = re.search(pattern, branch)
    if match:
        return match.groupdict()
    
    # Fallback pattern if no slash
    pattern_noslash = r"^(?P<issue_id>[a-zA-Z]+-\d+)-(?P<story_id>\d+-\d+)-?(?P<rest>.*)$"
    match = re.search(pattern_noslash, branch)
    if match:
        data = match.groupdict()
        data['type'] = 'feat' # Default
        return data
        
    return None

def find_user_story(story_id, project_root):
    search_path = Path(project_root) / "_bmad-output" / "implementation-artifacts"
    if not search_path.exists():
        return None
    
    # Search recursively in epic folders
    for root, dirs, files in os.walk(search_path):
        for file in files:
            if file.startswith(story_id) and file.endswith(".md"):
                return os.path.join(root, file)
    return None

def get_breaking_changes(project_root):
    breaks = []
    # 1. Check for deleted files
    deleted_files = run_command("git diff --name-only --diff-filter=D HEAD").splitlines()
    if not deleted_files: # Try cached
        deleted_files = run_command("git diff --cached --name-only --diff-filter=D").splitlines()
    
    if deleted_files:
        breaks.append(f"Deleted files: {', '.join(deleted_files)}")
    
    # 2. Check for database migrations
    migrations = [f for f in run_command("git diff --name-only HEAD").splitlines() if "db/migration" in f]
    if not migrations:
         migrations = [f for f in run_command("git diff --cached --name-only").splitlines() if "db/migration" in f]
    
    if migrations:
        breaks.append(f"Database migrations detected: {', '.join(migrations)}")
        
    return breaks

def main():
    project_root = os.getcwd() # Assume run from root
    branch = get_branch_name()
    if not branch:
        print("Error: Could not get branch name.")
        sys.exit(1)
        
    context = parse_branch(branch)
    if not context:
        print(f"Warning: Could not parse branch format for '{branch}'.")
        # Direct defaults
        context = {'type': 'chore', 'issue_id': 'UNKNOWN', 'story_id': 'UNKNOWN', 'rest': branch}

    story_path = find_user_story(context['story_id'], project_root)
    diff = get_git_diff()
    breaking_changes = get_breaking_changes(project_root)
    
    # Output for the agent (as a small JSON-like string or just text)
    print(f"--- CONTEXT ---")
    print(f"Branch: {branch}")
    print(f"Type: {context['type']}")
    print(f"Issue ID: {context['issue_id']}")
    print(f"Story ID: {context['story_id']}")
    print(f"Breaking Changes: {'; '.join(breaking_changes) if breaking_changes else 'NONE'}")
    print(f"Story File: {story_path if story_path else 'NOT FOUND'}")
    
    if story_path:
        with open(story_path, 'r') as f:
            print(f"\n--- USER STORY CONTENT ---\n{f.read()}")
            
    print(f"\n--- GIT DIFF ---\n{diff[:5000]}...") # Limit diff size
    if len(diff) > 5000:
        print("\n(Diff truncated...)")

if __name__ == "__main__":
    main()
