#!/bin/bash

set -e

if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Uncommitted changes detected. Please commit or stash them before running this script."
    exit 1
fi

current_branch=$(git rev-parse --abbrev-ref HEAD)
echo "Current branch: $current_branch"

git checkout main
git pull origin main

current_version=$(sed -En 's/.*version = "([0-9]+\.[0-9]+\.[0-9]+)"/\1/p' waveform/build.gradle.kts)
echo "Current version: $current_version"

IFS='.' read -r major minor patch <<< "$current_version"

if (( patch < 9 )); then
    patch=$((patch + 1))
else
    patch=0
    if (( minor < 9 )); then
        minor=$((minor + 1))
    else
        minor=0
        major=$((major + 1))
    fi
fi

new_version="${major}.${minor}.${patch}"
echo "New version: $new_version"

sed -i.bak "s/version = \"$current_version\"/version = \"$new_version\"/g" waveform/build.gradle.kts
rm waveform/build.gradle.kts.bak

git add waveform/build.gradle.kts
git commit -m "release: bump up version"

git tag "v$new_version"

git push -u origin main
git push origin "v$new_version"

echo "Version bumped to $new_version and pushed with tag v$new_version"

git checkout "$current_branch"
