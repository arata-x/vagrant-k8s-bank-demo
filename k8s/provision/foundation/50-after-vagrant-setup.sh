#!/bin/bash
set -e

# Wait until both nodes are Ready before labeling
NODES=(k8s-node-01 k8s-node-02 k8s-node-03 k8s-node-04)

## Wait until both nodes are Ready (handles Ready or Ready,SchedulingDisabled)
echo "⏳ Waiting for nodes to become Ready..."
kubectl wait --for=condition=Ready --timeout=10m "node/${NODES[0]}" "node/${NODES[1]}"

echo "✅ Both nodes are Ready. Applying labels..."
kubectl label node "${NODES[0]}" node-role.kubernetes.io/worker-node="" tier=edge --overwrite
kubectl label node "${NODES[1]}" node-role.kubernetes.io/worker-node="" tier=backend --overwrite
kubectl label node "${NODES[2]}" node-role.kubernetes.io/worker-node="" tier=backend --overwrite
kubectl label node "${NODES[3]}" node-role.kubernetes.io/worker-node="" tier=database --overwrite

## Verify
kubectl get nodes "${NODES[@]}" -o custom-columns=NAME:.metadata.name,LABELS:.metadata.labels --no-headers
echo "🏷️ Labels applied successfully."

#---

# Install Local Path Provisioner for dynamic storage provisioning
kubectl apply -f https://raw.githubusercontent.com/rancher/local-path-provisioner/master/deploy/local-path-storage.yaml
kubectl patch deploy local-path-provisioner -n local-path-storage \
  --type='merge' \
  -p '{"spec":{"template":{"spec":{"nodeSelector":{"tier":"edge"}}}}}'
#---

# Install NGINX Ingress Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml
kubectl patch deploy ingress-nginx-controller -n ingress-nginx \
  --type='merge' \
  -p '{"spec":{"template":{"spec":{"nodeSelector":{"tier":"edge"}}}}}'
#---

# Install MetalLB
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.15.2/config/manifests/metallb-native.yaml
kubectl patch deploy controller -n metallb-system \
  --type='merge' \
  -p '{"spec":{"template":{"spec":{"nodeSelector":{"tier":"edge"}}}}}'
#---


# Install metrics-server
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# patch for lab clusters (insecure kubelet certs / non-default IPs)
kubectl -n kube-system patch deploy metrics-server --type='json' -p='[
  {"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-insecure-tls"},
  {"op":"add","path":"/spec/template/spec/containers/0/args/-","value":"--kubelet-preferred-address-types=InternalIP,Hostname,ExternalIP"},
  {"op":"add","path":"/spec/template/spec/nodeSelector","value":{"tier":"edge"}}
]'
