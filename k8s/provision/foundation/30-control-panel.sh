#!/bin/bash
set -eux

#
sudo apt-get install -y kubectl

# Initialize cluster
sudo kubeadm init --apiserver-advertise-address=192.168.56.10 --pod-network-cidr=10.224.0.0/16

while [ ! -f /etc/kubernetes/admin.conf ]; do
  echo "Waiting for kubeadm init to complete..."
  sleep 5
done

# Setup kubeconfig
mkdir -p /home/vagrant/.kube
cp /etc/kubernetes/admin.conf /home/vagrant/.kube/config
chown vagrant:vagrant /home/vagrant/.kube/config
mkdir -p ~/.kube
cp /etc/kubernetes/admin.conf ~/.kube/config
#---

# Create join command
sudo kubeadm token create --print-join-command > /vagrant/provision/foundation/join-command.sh

#---
# Install Calico network plugin
## Define autodetection method
AUTO_METHOD="cidr=192.168.56.0/24"

## Download Calico manifest
curl -O https://raw.githubusercontent.com/projectcalico/calico/v3.30.3/manifests/calico.yaml

## Apply to cluster
kubectl apply -f calico.yaml

kubectl set env daemonset/calico-node -n kube-system IP_AUTODETECTION_METHOD="$AUTO_METHOD"

sudo systemctl restart kubelet

#install calicoctl
curl -L https://github.com/projectcalico/calico/releases/download/v3.30.4/calicoctl-linux-amd64 -o calicoctl
chmod +x calicoctl
sudo mv calicoctl /usr/local/bin/

# Install k9s
wget https://github.com/derailed/k9s/releases/latest/download/k9s_linux_amd64.deb
sudo apt install ./k9s_linux_amd64.deb
sudo rm k9s_linux_amd64.deb
echo "✅ k9s installed"

#---
