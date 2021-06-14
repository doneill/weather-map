#!/usr/bin/env bash
set -xeuo pipefail

docker run -it --rm -v "$PWD":/apps jdoneill/android-sdk:29-java11 sh -c "$@"
