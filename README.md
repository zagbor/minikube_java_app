# Проект контейнеризации и развертывания ИИ-приложений в Kubernetes
## Структура:

* ToneAnalysisServer.java - Основное Java приложение с HTTP сервером
* Dockerfile - Сборка Docker образа с multi-stage build
* docker-compose.yml - Локальный запуск приложения в Docker
* deployment.yaml - Kubernetes деплоймент с 3 репликами
* service.yaml - Kubernetes сервис типа LoadBalancer
* ingress.yaml - Ingress для маршрутизации внешнего трафика
* hpa.yaml - Horizontal Pod Autoscaler для автоскейлинга
* prometheus-deployment.yaml - Развертывание Prometheus в кластере
* prometheus-config.yaml - Конфигурация Prometheus для сбора метрик
* grafana-compose.yml - Docker Compose для запуска Grafana
* test_load.ps1 - Скрипт нагрузочного тестирования с Windows

# Команды проекта
## 1. Установка и настройка Minikube
```bash
# Установка Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# Запуск кластера
minikube start --cpus=6 --memory=10000mb --nodes=2

# Включение аддонов
minikube addons enable ingress
minikube addons enable metrics-server
```
2. Сборка и тестирование приложения
```bash
# Сборка Docker образа
docker-compose up --build -d

# Тестирование эндпоинтов
curl http://localhost:8080/health
curl -X POST http://localhost:8080/text-tone -H "Content-Type: application/json" -d '{"text": "Test"}'
curl http://localhost:8080/metrics
```
3. Загрузка в Minikube
```bash
# Загрузка образа
minikube image load proj-app:latest

# Проверка
minikube ssh docker images | grep proj-app
```
4. Развертывание в Kubernetes
```bash
# Применение манифестов
kubectl apply -f deployment.yaml -f service.yaml -f ingress.yaml -f hpa.yaml

# Проверка
kubectl get deployments,pods,services,ingress,hpa
```
5. Доступ к приложению
```bash
# Проброс портов
kubectl port-forward svc/proj-service 8080:80 --address 0.0.0.0
```
6. Мониторинг
```bash
# Создание namespace
kubectl create namespace monitoring

# Установка Prometheus
kubectl apply -f prometheus-config.yaml
kubectl apply -f prometheus-deployment.yaml

# Доступ к Prometheus
kubectl port-forward -n monitoring svc/prometheus 9090:9090 --address 0.0.0.0
```
7. Тестирование нагрузки
```bash
# Запуск нагрузочного теста
./load-test.sh

# Мониторинг автоскейлинга
kubectl get hpa -w
```