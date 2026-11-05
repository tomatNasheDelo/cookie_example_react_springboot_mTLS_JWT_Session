# Cookie, Session, JWT & mTLS Lab

Учебный проект: React/Vite 7 + Spring Boot/Kotlin с тремя отдельными темами аутентификации и транспорта:

- **Session** — серверная сессия через `HttpSession` и cookie `JSESSIONID`.
- **JWT** — подписанный токен, который frontend хранит только в памяти и передаёт как Bearer.
- **mTLS** — backend вызывает отдельный внутренний Spring Boot сервис по HTTPS, обе стороны предъявляют сертификаты.

Подробное объяснение компонентов, локального запуска, переменных Cloudflare/Render и ограничений безопасности — в [`start.txt`](start.txt).

Быстрый старт из корня: `./scripts/generate-certs.sh`, затем запустить `mtls-resource`, `backend` и `frontend` по инструкциям `start.txt`.

Это учебный стенд без базы данных. Не используйте демо-логин, ключи или значения cookies в production.
