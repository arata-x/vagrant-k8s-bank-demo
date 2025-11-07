# 
kubectl apply -f https://raw.githubusercontent.com/metallb/metallb/v0.14.5/config/manifests/metallb-native.yaml
kubectl get pods -n metallb-system
kubectl apply -f /vagrant/provision/deployment/example/11-metallb-config.yaml
kubectl get svc ingress-nginx-controller -n ingress-nginx
