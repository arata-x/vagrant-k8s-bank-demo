# Spring Boot Account Concurrency Demo: Kubernetes Deployment via Vagrant

A Spring Boot application demonstrating account transaction concurrency patterns (Optimistic & Pessimistic locking) deployed on a multi-node Kubernetes cluster using Vagrant.

## 📋 Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Testing](#testing)
- [Monitoring](#monitoring)
- [Troubleshooting](#troubleshooting)
- [Cleanup](#cleanup)

## 🎯 Overview

This project demonstrates:
- **Multi-node Kubernetes cluster** setup using Vagrant and VirtualBox
- **Spring Boot application** with PostgreSQL database
- **Concurrency control** patterns (Optimistic vs Pessimistic locking)
- **Production-grade infrastructure**: Ingress, LoadBalancer, Storage provisioning
- **Load testing** with k6 for performance validation

## 🏗️ Architecture

### Cluster Topology

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Control Plane (k8s-cp-01)                                  │
│  └─ IP: 192.168.56.10                                       │
│  └─ Role: Master node, API server, scheduler, controller    │
│                                                             │
│  Worker Nodes:                                              │
│  ├─ k8s-node-01 (192.168.56.11) - tier: edge                │
│  │  └─ Ingress Controller, Local Path Provisioner, MetalLB  │
│  ├─ k8s-node-02 (192.168.56.12) - tier: backend             │
│  │  └─ Spring Boot Application Pods                         │
│  ├─ k8s-node-03 (192.168.56.13) - tier: backend             │
│  │  └─ Spring Boot Application Pods                         │
│  └─ k8s-node-04 (192.168.56.14) - tier: database            │
│     └─ PostgreSQL Database                                  │
│                                                             │
│  LoadBalancer IP Pool: 192.168.56.240-250                   │
└─────────────────────────────────────────────────────────────┘
```

## 📦 Prerequisites

### Required Software

1. **VirtualBox** (7.1.6 or later)
   ```bash
   # macOS
   brew install --cask virtualbox
   
   # Windows
   choco install virtualbox
   
   # Linux
   sudo apt-get install virtualbox
   ```

2. **Vagrant** (2.4.9 or later)
   ```bash
   # macOS
   brew install vagrant
   
   # Windows
   choco install vagrant
   
   # Linux
   sudo apt-get install vagrant
   ```

### System Requirements

- **RAM**: Minimum 12GB (2GB per VM × 5 VMs + host overhead)
- **CPU**: 4+ cores recommended
- **Disk**: 20GB free space
- **Network**: Private network support (192.168.56.0/24)

## 🚀 Quick Start

```bash
# 1. Navigate to k8s directory
cd k8s

# 2. Start all VMs (this will take 10-15 minutes)
vagrant up

# 3. SSH into control plane
vagrant ssh k8s-cp-01

# 4. Run post-initialization setup
sudo /vagrant/provision/foundation/setup-after-init.sh

# 5. Deploy infrastructure components
kubectl apply -f /vagrant/provision/deployment/standard/resource/infra

# 6. Deploy application
kubectl apply -f /vagrant/provision/deployment/standard/resource/app

# 7. Monitor with k9s
k9s

# 8. Test the application (see Testing section)
```

## 🧪 Testing

### Option 1: Using k9s (Interactive Monitoring)

```bash
k9s
```

**Navigation:**
- Type `:namespace` and select `demo`
- Type `:pods` to view pods
- Type `:svc` to view services
- Type `:ingress` to view ingress
- Press `<Enter>` on a pod to view logs
- Press `<Ctrl-C>` to exit

### Option 2: Get Ingress IP

```bash
kubectl get ingress -n demo
```

### Option 4: Load Testing with k6

For comprehensive load testing, see [test/README.md](test/README.md)

### Option 5: Port Forwarding (Alternative Access)

If you prefer to access via localhost:

```bash
# Forward application port
kubectl port-forward -n demo svc/api-svc 8080:80

# In another terminal
curl http://localhost:8080/actuator/health
```

## 📊 Monitoring

### Using k9s

```bash
k9s
```

**Useful views:**
- `:pods` - View all pods
- `:deployments` - View deployments
- `:services` - View services
- `:ingress` - View ingress rules
- `:pv` - View persistent volumes
- `:pvc` - View persistent volume claims
- `:events` - View cluster events

### Using kubectl

```bash
# View all resources in demo namespace
kubectl get all -n demo

# View pod logs
kubectl logs -n demo -l app=springboot-app --tail=100 -f

# View database logs
kubectl logs -n demo -l app=postgres --tail=100 -f

# View ingress controller logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller --tail=100 -f

# View resource usage
kubectl top nodes
kubectl top pods -n demo

# View events
kubectl get events -n demo --sort-by='.lastTimestamp'
```

### Accessing PostgreSQL

```bash
# From control plane node
kubectl exec -it -n demo <postgres-pod-name> -- psql -U postgres -d accountdb

# Or via NodePort from host machine
psql -h 192.168.56.10 -p 30000 -U postgres -d accountdb
```

## 🔧 Troubleshooting

### VMs Not Starting

```bash
# Check VM status
vagrant status

# Restart a specific VM
vagrant reload k8s-cp-01

# Destroy and recreate
vagrant destroy -f
vagrant up
```

### Nodes Not Ready

```bash
# Check node status
kubectl get nodes

# Describe node for details
kubectl describe node k8s-node-01

# Check kubelet logs
sudo journalctl -u kubelet -f
```

### Pods Not Starting

```bash
# Check pod status
kubectl get pods -n demo

# Describe pod for events
kubectl describe pod <pod-name> -n demo

# View pod logs
kubectl logs <pod-name> -n demo

# Check if images are pulled
kubectl get events -n demo | grep -i pull
```

### Ingress Not Working

```bash
# Check ingress status
kubectl get ingress -n demo
kubectl describe ingress webapp-ingress -n demo

# Check ingress controller
kubectl get pods -n ingress-nginx
kubectl logs -n ingress-nginx -l app.kubernetes.io/component=controller

# Verify MetalLB
kubectl get pods -n metallb-system
kubectl get ipaddresspool -n metallb-system
```

### Database Connection Issues

```bash
# Check database pod
kubectl get pods -n demo -l app=postgres

# Check database logs
kubectl logs -n demo -l app=postgres

# Test connection from app pod
kubectl exec -it -n demo <app-pod-name> -- nc -zv database-svc 5432
```

### Performance Issues

```bash
# Check resource usage
kubectl top nodes
kubectl top pods -n demo

# Increase VM resources in Vagrantfile
# Edit k8s/Vagrantfile and change:
vb.memory = "4096"  # Increase from 2048
vb.cpus = 4         # Increase from 2

# Then reload
vagrant reload
```

## 🧹 Cleanup

### Stop VMs (Preserve State)

```bash
cd k8s
vagrant halt
```

### Destroy VMs (Complete Cleanup)

```bash
cd k8s
vagrant destroy -f
```

### Vagrant Commands

```bash
vagrant up                 # boot and provision
vagrant ssh                # SSH into the default VM
vagrant ssh <name>         # SSH into a named machine
vagrant halt               # graceful shutdown
vagrant reload             # reboot + re-run network/some provider settings
vagrant provision          # re-run provisioners
vagrant suspend            # save VM state (RAM to disk)
vagrant resume             # resume from suspend
vagrant destroy -f         # destroy the VM
vagrant status             # list machines & states
vagrant global-status      # list all Vagrant-managed VMs system-wide
vagrant box list           # list cached boxes
vagrant box prune          # clean old versions
vagrant snapshot save dev  # snapshot current state
vagrant snapshot restore dev
```

## 📚 Additional Resources

- **Application Code**: `app/` directory
- **Kubernetes Manifests**: `k8s/provision/deployment/standard/`
- **Provision Scripts**: `k8s/provision/foundation/`
- **Load Testing Guide**: `test/README.md`
- **Vagrantfile**: `k8s/Vagrantfile`

## 📝 Notes

- The cluster uses **Calico** for pod networking
- **MetalLB** operates in Layer 2 mode
- **Ingress** is accessible via LoadBalancer IP (192.168.56.240-250)
- **Database** is accessible via NodePort 30000 for debugging
- **Node labels** control pod placement (edge, backend, database tiers)
- All VMs use **Ubuntu 22.04 (Jammy)** base image
