#!/usr/bin/env bash
neededPacks="wget tar unzip lib32stdc++6 lib32z1"
for package in $neededPacks; do
    dpkg -s "$package" >/dev/null 2>&1 && {
        echo "$package is installed."
    } || {
        sudo apt-get --quiet install --yes $package
    }
done