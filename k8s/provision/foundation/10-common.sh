#!/bin/bash
set -eux

# Disable swap
sudo swapoff -a
sudo sed -i '/ swap / s/^/#/' /etc/fstab

#---

# Install dependencies
sudo apt-get update -y
sudo apt-get install -y apt-transport-https ca-certificates curl

#---

# Add Kubernetes repo
curl -fsSL https://pkgs.k8s.io/core:/stable:/v1.34/deb/Release.key | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/kubernetes.gpg
echo "deb [signed-by=/etc/apt/trusted.gpg.d/kubernetes.gpg] https://pkgs.k8s.io/core:/stable:/v1.34/deb/ /" | sudo tee /etc/apt/sources.list.d/kubernetes.list

sudo apt-get update -y
sudo apt-get install -y kubelet kubeadm containerd

#---

# Configure containerd
sudo mkdir -p /etc/containerd
containerd config default | sudo tee /etc/containerd/config.toml > /dev/null  
## Update pause image version to 3.10.1
sudo sed -i.bak 's|"registry.k8s.io/pause:3.8"|"registry.k8s.io/pause:3.10.1"|' /etc/containerd/config.toml
sudo sed -i.bak -E 's/^( *SystemdCgroup *= *)false/\1true/' /etc/containerd/config.toml
sudo systemctl restart containerd

#---

# Load necessary kernel modules
## overlay: required for container storage layers (used by containerd, Docker)
## br_netfilter: enables iptables filtering on bridged traffic (used by CNI plugins like Calico)
cat <<EOF | sudo tee /etc/modules-load.d/k8s.conf
overlay
br_netfilter
EOF
sudo modprobe overlay
sudo modprobe br_netfilter

#---

# Configures essential kernel parameters to ensure Kubernetes networking works correctly—especially for container traffic routing and firewall rules
sudo cat <<'EOF' | sudo tee /etc/sysctl.d/99-kubernetes-cri.conf
net.bridge.bridge-nf-call-iptables = 1
net.bridge.bridge-nf-call-ip6tables = 1
net.ipv4.ip_forward = 1
EOF
sudo sysctl --system