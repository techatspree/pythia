# shellcheck shell=bash
#
# Settings for the local minikube cluster (task-196), shared by
# minikube-setup.sh, minikube-deploy.sh and minikube-reset.sh, which SOURCE
# this file. It only assigns: no commands, no side effects.
#
# Every value can be overridden for one run without editing this file, e.g.
#   MINIKUBE_MEMORY=12288 ./scripts/minikube-setup.sh
#
# The variables are deliberately NOT exported, so sourcing this file cannot
# leak anything into Gradle, Docker or your shell. A script that hands a value
# to a child process (e.g. envsubst) passes it explicitly.
#
# No secrets and no organisation coordinates here: this file is public. The
# ENTRA_* ids stay in the workspace's EntraIdVariables.sh, outside the repo.
#
# One value has another owner that a shell file cannot reach. Keep the default
# in step with it:
#   - MINIKUBE_NAMESPACE  <-> `namespace:` in k8s/base/kustomization.yaml
# MINIKUBE_IMAGE_TAG used to mirror the Gradle version; since task-193 that
# version changes with every release, so the local cluster uses its own fixed
# tag instead. The deploy passes it explicitly to both image builds.

if [ "${BASH_SOURCE[0]}" = "$0" ]; then
    echo "minikube-env.sh holds settings; source it from the minikube scripts, do not run it." >&2
    exit 1
fi

# Cluster sizing, used by minikube-setup.sh.
: "${MINIKUBE_CPUS:=4}"
: "${MINIKUBE_MEMORY:=8192}"
: "${MINIKUBE_DRIVER:=docker}"

# The namespace the stack is deployed into.
: "${MINIKUBE_NAMESPACE:=estimation}"

# The registry addon, as seen from the HOST: the deploy port-forwards
# svc/registry to this port and pushes through it. Not 5000, because macOS's
# ControlCenter (AirPlay Receiver) listens on 5000.
: "${MINIKUBE_REGISTRY_HOST_PORT:=5005}"

# The same registry, as seen from INSIDE the node, where pods pull from it:
# the addon's registry-proxy serves it on the node's localhost:5000. Verified
# on minikube 1.39 (docker driver) with both node runtimes, Docker and the
# default containerd; the node pulls its native variant.
: "${MINIKUBE_IMAGE_REGISTRY:=localhost:5000}"

# The image tag pushed to and deployed from the local cluster's registry.
: "${MINIKUBE_IMAGE_TAG:=local}"
