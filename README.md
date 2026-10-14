# Beloved Neighbors 🔥🍿

Шуточный телеграм-бот на двоих. Каждый отмечает день и желаемую активность; когда желания
на один день пересекаются — обоим прилетает стикер.

Активности: **Секс**, **Фильм**, **Секс + фильм**. Матчинг по пересечению: если один хочет
«секс + фильм», а второй «фильм» — совпадение на фильме.

## Стек

Kotlin 2.4 / Java 25 · Spring Boot 3.5 · Postgres 17 + Flyway + data-jpa ·
[telegrambots](https://github.com/rubenlagus/TelegramBots) 10.3 (long polling)

## Запуск

1. Создай бота у [@BotFather](https://t.me/BotFather) и забери токен.
2. Поднять базу (снаружи она на порту **5433**, чтобы не мешать локальному Postgres):

   ```sh
   docker compose up -d
   ```

3. Запустить приложение:

   ```sh
   TELEGRAM_BOT_TOKEN=123456:AA... TELEGRAM_BOT_USERNAME=beloved_neighbors_bot ./gradlew bootRun
   ```

4. Нажать `/start` у бота с двух аккаунтов — первые двое занимают места, третьему бот откажет.

### Переменные окружения

| Переменная | Обязательна | Назначение |
|---|---|---|
| `TELEGRAM_BOT_TOKEN` | да | токен от @BotFather |
| `TELEGRAM_BOT_USERNAME` | да | username бота |
| `STICKER_SEX`, `STICKER_MOVIE`, `STICKER_SEX_AND_MOVIE` | нет | `file_id` стикеров |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | нет | если база не из docker-compose |

## Стикеры

Пришли боту любой стикер — он ответит его `file_id`. Вписать в переменные окружения
(`STICKER_SEX` и т.д.) и перезапустить. Пока стикеры не заданы, бот отправляет текст с эмодзи,
так что работает и без настройки.

## Команды

| Команда | Кнопка | Что делает |
|---|---|---|
| `/start` | — | регистрация |
| `/propose` | 🗓 Предложить | выбрать день → выбрать активность |
| `/list` | 📋 Предложения | свои и чужие предложения; своё можно скинуть |
| `/matches` | 💘 Матчи | что уже совпало |

Когда кто-то предлагает активность, второму приходит уведомление с кнопками «🙌 Я тоже» и
«🙈 Не сегодня». Если предложения совпали — вместо уведомления обоим летит стикер.
Скинутое предложение снимает матч и об этом тоже сообщают обоим.

## Как добавить активность

В `domain/Activity.kt` добавить строку в enum — при необходимости с новым тегом:

```kotlin
WALK("Прогулка", setOf(ActivityTag.WALK)),
```

Пересечение считается по тегам, так что ни матчинг, ни клавиатуры править не нужно —
разве что добавить стикер в конфиг.

## Деплой на сервер

На сервере нужен только Docker и compose. Бот работает на long polling, поэтому
ни домена, ни открытых портов, ни TLS не требуется — он сам ходит в Telegram.

`docker-compose.prod.yml` написан в формате `2.4` и работает и со старым
`docker-compose` (v1), и с современным плагином `docker compose` (v2). Разница только в
имени команды: везде ниже вместо `docker compose` подставляй `docker-compose`, если на
сервере v1. Формат 3.x здесь не годится: в нём нет `depends_on.condition`, то есть
приложение полезет в базу, не дождавшись её готовности.

Compose v1 больше не поддерживается (EOL с 2023), так что плагин v2 рано или поздно поставить
стоит — но этот файл переписывать для него не придётся.

Положи рядом с `docker-compose.prod.yml` файл `.env` (в git он не попадает):

```sh
TELEGRAM_BOT_TOKEN=123456:AA...
TELEGRAM_BOT_USERNAME=beloved_neighbors_bot
DB_PASSWORD=придумай-что-нибудь-длинное
# необязательно
STICKER_SEX=
STICKER_MOVIE=
STICKER_SEX_AND_MOVIE=
DB_PORT=5433       # порт базы на сервере
DB_BIND=127.0.0.1  # кого слушать; по умолчанию только loopback
COMPOSE_PROJECT_NAME=beloved-neighbors
```

`COMPOSE_PROJECT_NAME` стоит задать явно: иначе имя проекта (а с ним и имя volume с базой)
compose берёт из имени каталога, и после переименования или переноса папки стек поедет
с пустым новым volume вместо твоих данных.

Если какой-то обязательной переменной нет, compose откажется стартовать и скажет, какой именно.

### Вариант 1: собрать прямо на сервере

Нужно ~2 ГБ RAM на время сборки (Gradle качает зависимости внутрь образа).

```sh
git clone <repo> && cd beloved_neighbors
# создать .env
docker compose -f docker-compose.prod.yml up -d --build
```

### Вариант 2: собрать локально и перенести образ

Полезно, если сервер слабый. Mac на arm64, сервер почти наверняка amd64 — платформу надо
указать явно, иначе образ не запустится:

```sh
docker buildx build --platform linux/amd64 -t beloved-neighbors:latest .
docker save beloved-neighbors:latest | gzip | ssh user@server 'gunzip | docker load'
```

Дальше на сервере достаточно `docker-compose.prod.yml` и `.env`:

```sh
docker compose -f docker-compose.prod.yml up -d
```

Образ уже загружен, так что собирать ничего не будет (пересборка только с `--build`).

### Подключиться к базе на сервере

База публикуется на `127.0.0.1:5433` сервера — то есть доступна только с самого сервера.
Снаружи заходим через SSH-туннель.

**С самого сервера** (после `ssh`), туннель не нужен:

```sh
docker compose -f docker-compose.prod.yml exec db psql -U neighbors -d neighbors
# или, раз порт опубликован на loopback:
psql -h 127.0.0.1 -p 5433 -U neighbors -d neighbors
```

**С ноутбука** — пробрасываем порт и подключаемся как к локальной базе:

```sh
ssh -N -L 5433:127.0.0.1:5433 user@server
# в другом терминале:
psql -h 127.0.0.1 -p 5433 -U neighbors -d neighbors
```

Если локально поднят дев-постгрес на том же 5433, возьми другой локальный порт:
`ssh -N -L 5434:127.0.0.1:5433 user@server`.

В DataGrip/pgAdmin проще не поднимать туннель руками: хост `127.0.0.1`, порт `5433`,
база и юзер `neighbors`, а на вкладке SSH/SSL включить SSH-туннель до сервера.

Если порт нужно сменить или (сознательно) открыть базу наружу — в `.env`:

```sh
DB_PORT=15432      # другой порт на сервере
DB_BIND=0.0.0.0    # открыть всем — так лучше не делать
```

Про `DB_BIND=0.0.0.0` стоит знать две вещи: Postgres окажется доступен из интернета с
паролем как единственной защитой, и правила Docker в iptables идут **раньше** UFW — то есть
`ufw deny 15432` такой порт не прикроет. Через SSH-туннель получаешь то же удобство
без этих проблем.

### Эксплуатация

```sh
docker compose -f docker-compose.prod.yml logs -f app     # логи бота
docker compose -f docker-compose.prod.yml ps              # что живо
docker compose -f docker-compose.prod.yml restart app     # перезапуск
docker compose -f docker-compose.prod.yml down            # остановить (данные в volume целы)
```

`.env` нужен **каждой** compose-команде, а не только `up`: обязательные переменные
подставляются при чтении файла, так что без них не сработают даже `logs` и `ps`.

Бэкап и восстановление базы:

```sh
docker compose -f docker-compose.prod.yml exec db pg_dump -U neighbors neighbors | gzip > backup.sql.gz
gunzip -c backup.sql.gz | docker compose -f docker-compose.prod.yml exec -T db psql -U neighbors -d neighbors
```

Что стоит знать:

- Порт базы публикуется только на `127.0.0.1` сервера — снаружи нужен SSH-туннель.
- `DB_PASSWORD` применяется при **первом** создании volume. Поменять его потом через `.env`
  не получится: нужно либо `ALTER USER neighbors PASSWORD '...'` в psql, либо удалить volume
  (`docker compose -f docker-compose.prod.yml down -v`) и потерять данные.
- Миграции Flyway накатываются сами при старте приложения.
- В образе выполняется только `bootJar`, без тестов: интеграционным нужен Docker внутри Docker.
  Тесты гоняем локально (`./gradlew build`) до деплоя.
- Логи ограничены (10 МБ × 3 файла на сервис), диск не забьётся.
- Оба контейнера с `restart: unless-stopped` — поднимутся после перезагрузки сервера.
- Памяти хватит 1 ГБ: JVM ограничена 75% доступной (`-XX:MaxRAMPercentage=75.0`).

## Тесты

```sh
./gradlew test
```

Интеграционные тесты поднимают Postgres через Testcontainers (нужен запущенный Docker),
Telegram при этом не вызывается.
