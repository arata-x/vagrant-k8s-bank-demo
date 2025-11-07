#!/bin/bash
set -e

# 🧭 Step 1: Extract IP from enp0s8
NODE_IP=$(ip -4 addr show enp0s8 | grep -oP '(?<=inet\s)\d+(\.\d+){3}')

if [[ -z "$NODE_IP" ]]; then
  echo "❌ Failed to detect IP from enp0s8. Is the interface up?"
  exit 1
fi

echo "✅ Detected node IP: $NODE_IP"

# 🧭 Step 2: Inject into kubelet systemd drop-in
DROPIN_FILE=/lib/systemd/system/kubelet.service.d/10-kubeadm.conf

if [[ ! -f "$DROPIN_FILE" ]]; then
  echo "❌ Drop-in file not found: $DROPIN_FILE"
  exit 1
fi

# Append --node-ip if not already present
if ! grep -q -- "--node-ip=$NODE_IP" "$DROPIN_FILE"; then
  sudo sed -i "0,/^Environment=\"KUBELET_KUBECONFIG_ARGS=/s|\"$| --node-ip=$NODE_IP\"|" "$DROPIN_FILE"
  echo "✅ Updated kubelet drop-in with node IP."
fi

sudo cat "$DROPIN_FILE"

# 🧭 Step 3: Reload and restart kubelet
sudo systemctl daemon-reexec
sudo systemctl daemon-reload
sudo systemctl restart kubelet

echo "✅ Kubelet restarted with node IP: $NODE_IP"
