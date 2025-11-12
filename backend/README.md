# smart-to-do-mini-app
This repo for VK Hackathon

### Требования
- Docker
- Docker Compose

### Команда запуска
```bash
docker compose up -d
```

### Документация API
- Swagger UI:
http://localhost:8080/swagger-ui/index.html

### Принцип работы

- Контейнер ollama-pull выполняет загрузку модели (pull).
- После завершения pull модель доступна внутри ollama, а контейнер ollama-pul выключается 
- Первый запрос к LLM занимает больше времени (инициализация).
- Последующие запросы обрабатываются быстрее.