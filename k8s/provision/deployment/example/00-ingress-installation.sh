# #!/bin/bash

# Install Ingress NGINX Controller
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# Confirm
kubectl get pods -n ingress-nginx
kubectl get svc  -n ingress-nginx

# Deploy a sample app
kubectl create deployment demo --image=nginxdemos/hello
kubectl expose deployment demo --port=80 --target-port=80 --name=demo-svc
kubectl get svc demo-svc

# Create ingress rule
kubectl apply -f /vagrant/provision/deployment/example/01-ingress-resource.yml

# Confirm installation
kubectl get pods -n ingress-nginx
kubectl get svc  -n ingress-nginx
kubectl get ingressclass

