# Как отправить проект на GitHub

Репозиторий уже инициализирован локально, сделан первый коммит, ветка — `main`.

## Шаги

### 1. Создайте новый репозиторий на GitHub

1. Откройте [github.com](https://github.com) и войдите в аккаунт.
2. Нажмите **«+» → «New repository»**.
3. Укажите имя, например **RWBOTANDROID** (или любое другое).
4. Оставьте репозиторий **пустым**: не добавляйте README, .gitignore и лицензию — они уже есть в проекте.
5. Нажмите **«Create repository»**.

### 2. Подключите удалённый репозиторий и отправьте код

В терминале в каталоге проекта выполните (подставьте **ВАШ_ЛОГИН** на GitHub):

```bash
git remote add origin https://github.com/ВАШ_ЛОГИН/RWBOTANDROID.git
git push -u origin main
```

Если репозиторий вы назвали по-другому — замените `RWBOTANDROID` в URL на это имя.

### 3. Авторизация

- При первом `git push` браузер или Git могут запросить вход в GitHub.
- Если используете **HTTPS**, можно настроить [Personal Access Token](https://github.com/settings/tokens) вместо пароля.
- Альтернатива: **SSH** — тогда добавьте remote так:  
  `git remote add origin git@github.com:ВАШ_ЛОГИН/RWBOTANDROID.git`

После успешного `git push` проект будет на GitHub.
