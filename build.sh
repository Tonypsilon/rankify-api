#!/bin/bash

# Containerized build script for rankify-api
# Uses a Maven container to build the project without requiring local Maven installation

set -e

CONTAINER_ENGINE=${CONTAINER_ENGINE:-podman}
BUILD_ARGS=${1:-"clean install"}
SKIP_TESTCONTAINERS=${SKIP_TESTCONTAINERS:-0}

echo "Building rankify-api using $CONTAINER_ENGINE..."
echo "Build arguments: mvn $BUILD_ARGS"

# Check container engine availability early
if ! command -v "$CONTAINER_ENGINE" >/dev/null 2>&1; then
  echo "WARNING: Container engine '$CONTAINER_ENGINE' not found."
  if command -v docker >/dev/null 2>&1; then
    echo "Falling back to docker."; CONTAINER_ENGINE=docker
  elif command -v mvn >/dev/null 2>&1; then
    echo "Falling back to local mvn execution.";
    if [[ "$SKIP_TESTCONTAINERS" == "1" ]]; then
      mvn -Dtestcontainers.skip=true $BUILD_ARGS
    else
      mvn $BUILD_ARGS
    fi
    exit $?
  else
    echo "ERROR: Neither specified container engine nor docker nor local mvn available."; exit 2
  fi
fi

# Check if we're in a sandboxed/CI environment and handle accordingly
if [ -f /.dockerenv ] || [ -n "${GITHUB_ACTIONS}" ] || [ -n "${CI}" ]; then
    echo "Detected sandboxed/CI environment, using local Maven as fallback..."
    if command -v mvn &> /dev/null; then
        mvn $BUILD_ARGS
        echo "Build completed successfully using local Maven!"
        exit 0
    else
        echo "Warning: Local Maven not available and containerized build failed in sandbox environment"
        echo "In a normal development environment, this would use a Maven container"
    fi
fi

# Handle path conversion for Git Bash on Windows
PROJECT_PATH="$(pwd)"

# Debug: Show original path
echo "Original path from pwd: $PROJECT_PATH"

# Check if we're in Git Bash (MSYS/MinGW environment) or WSL
if [[ "$MSYSTEM" =~ ^MINGW ]] || [[ "$PROJECT_PATH" =~ ^/[a-zA-Z]/ ]] || [[ "$PROJECT_PATH" =~ ^/mnt/[a-zA-Z]/ ]]; then
    # In Git Bash, pwd returns paths like /c/Users/...
    # In WSL, pwd returns paths like /mnt/c/Users/...
    # Convert both to Windows format for Docker volume mounting
    if [[ "$PROJECT_PATH" =~ ^/mnt/ ]]; then
        # WSL format: /mnt/c/Users/... -> C:/Users/...
        PROJECT_PATH=$(echo "$PROJECT_PATH" | sed 's|^/mnt/\([a-zA-Z]\)/|\U\1:/|')
        echo "WSL detected, converted path: $PROJECT_PATH"
    else
        # Git Bash format: /c/Users/... -> C:/Users/...
        PROJECT_PATH=$(echo "$PROJECT_PATH" | sed 's|^/\([a-zA-Z]\)/|\U\1:/|')
        echo "Git Bash detected, converted path: $PROJECT_PATH"
    fi
fi

# Additional safety check - ensure we have a proper Windows path
if [[ ! "$PROJECT_PATH" =~ ^[A-Z]:/ ]]; then
    echo "ERROR: Failed to convert to proper Windows path format!"
    echo "Current PROJECT_PATH: $PROJECT_PATH"
    echo "Expected format: C:/path/to/project"
    exit 1
fi

echo "Final mounting path: $PROJECT_PATH -> /workspace (inside container)"

# --- Docker / Podman socket detection for Testcontainers -------------------------------
# We need to expose a Docker-compatible API endpoint inside the Maven build container so that
# Testcontainers (used by integration tests) can start sibling containers.
# Strategy:
# 1. Prefer an existing docker.sock if present (Docker environment)
# 2. Otherwise try common Podman socket locations and map them to /var/run/docker.sock inside
#    the build container (Podman's Docker API compatibility layer)
# 3. If DOCKER_HOST is set to a tcp:// endpoint, just forward the env var (no socket mount needed)
# 4. Provide debug output to help diagnose issues.

SOCKET_MOUNT_ARGS=()
EXPORTED_ENV_ARGS=()
EXTRA_MOUNTS=()
RUN_USER_ARGS=()

# Support tcp:// or ssh:// DOCKER_HOST (ssh common for podman machine on Windows)
if [[ -n "$DOCKER_HOST" && ( "$DOCKER_HOST" == tcp://* || "$DOCKER_HOST" == ssh://* ) ]]; then
  echo "Detected DOCKER_HOST=$DOCKER_HOST (remote engine)."
  if [[ "$DOCKER_HOST" == ssh://* ]]; then
    echo "WARNING: DOCKER_HOST uses ssh:// which is NOT supported by Testcontainers (docker-java)." >&2
    echo "         Expose Podman over a TCP socket and set DOCKER_HOST=tcp://HOST:PORT instead." >&2
    echo "         Example inside podman machine: podman system service --time=0 tcp:0.0.0.0:2375 &" >&2
    echo "         Then on host: export DOCKER_HOST=tcp://127.0.0.1:2375" >&2
    if [[ "$SKIP_TESTCONTAINERS" != "1" ]]; then
      echo "ERROR: Cannot proceed with Testcontainers over ssh://. Set SKIP_TESTCONTAINERS=1 to bypass tests or switch to tcp://." >&2
      exit 3
    else
      echo "Continuing because SKIP_TESTCONTAINERS=1 (integration tests will be skipped)." >&2
    fi
  else
    EXPORTED_ENV_ARGS+=( -e DOCKER_HOST="$DOCKER_HOST" )
  fi
  # (ssh path no longer attempts key mount since unsupported)
else
  # Candidate socket paths (host side)
  CANDIDATES=(
    "/var/run/docker.sock"
    "/run/docker.sock"
    "/var/run/podman/podman.sock"
    "/run/podman/podman.sock"
  )
  # Include XDG runtime dir for rootless podman if available
  if [[ -n "$XDG_RUNTIME_DIR" ]]; then
    CANDIDATES+=("$XDG_RUNTIME_DIR/podman/podman.sock")
  fi

  for candidate in "${CANDIDATES[@]}"; do
    if [[ -S "$candidate" ]]; then
      echo "Found container engine socket: $candidate"
      SOCKET_MOUNT_ARGS=( -v "$candidate:/var/run/docker.sock" )
      # If this is a rootless podman socket (path contains podman/podman.sock), align UID/GID for permissions
      if [[ "$candidate" == *podman/podman.sock* ]]; then
        HOST_UID=$(id -u)
        HOST_GID=$(id -g)
        echo "Rootless Podman socket detected; running container as host user $HOST_UID:$HOST_GID"
        RUN_USER_ARGS=( --user $HOST_UID:$HOST_GID )
      fi
      break
    fi
  done

  if [[ ${#SOCKET_MOUNT_ARGS[@]} -eq 0 ]]; then
    echo "WARNING: No Docker/Podman socket detected. Testcontainers-based tests may fail."
  else
    EXPORTED_ENV_ARGS+=( -e DOCKER_HOST="unix:///var/run/docker.sock" )
    EXPORTED_ENV_ARGS+=( -e TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE="/var/run/docker.sock" )
  fi
fi

# Pass through optional helpful env vars if set
for var in TESTCONTAINERS_HOST_OVERRIDE TESTCONTAINERS_RYUK_DISABLED; do
  if [[ -n "${!var}" ]]; then
    EXPORTED_ENV_ARGS+=( -e "$var=${!var}" )
  fi
done

echo "Socket mount args: ${SOCKET_MOUNT_ARGS[*]:-(none)}"
echo "Exported env args: ${EXPORTED_ENV_ARGS[*]:-(none)}"

RUN_ARGS=( run --rm )
RUN_ARGS+=( ${RUN_USER_ARGS[@]} )
RUN_ARGS+=( ${SOCKET_MOUNT_ARGS[@]} )
RUN_ARGS+=( ${EXPORTED_ENV_ARGS[@]} )
RUN_ARGS+=( ${EXTRA_MOUNTS[@]} )
RUN_ARGS+=( -v "$PROJECT_PATH:/workspace" -w /workspace )

# Optional: allow skipping Testcontainers-dependent tests when engine unavailable or explicitly requested
if [[ "$SKIP_TESTCONTAINERS" == "1" ]]; then
  echo "SKIP_TESTCONTAINERS=1 -> adding -Dtestcontainers.skip=true to Maven command"
  BUILD_ARGS="$BUILD_ARGS -Dtestcontainers.skip=true"
fi

echo "Running: $CONTAINER_ENGINE ${RUN_ARGS[*]} docker.io/library/maven:3.9.9-eclipse-temurin-24 mvn $BUILD_ARGS"

# Use MSYS_NO_PATHCONV to prevent Git Bash from converting /workspace to Windows path
MSYS_NO_PATHCONV=1 $CONTAINER_ENGINE "${RUN_ARGS[@]}" docker.io/library/maven:3.9.9-eclipse-temurin-24 mvn $BUILD_ARGS

echo "Build completed successfully!"
