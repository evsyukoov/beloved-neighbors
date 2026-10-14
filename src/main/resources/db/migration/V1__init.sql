create table participant
(
    telegram_id bigint       primary key,
    chat_id     bigint       not null,
    first_name  varchar(128) not null,
    username    varchar(64),
    created_at  timestamptz  not null default now()
);

create table proposal
(
    id             bigserial    primary key,
    participant_id bigint       not null references participant (telegram_id),
    day            date         not null,
    activity       varchar(32)  not null,
    status         varchar(16)  not null,
    created_at     timestamptz  not null default now()
);

-- одно активное предложение на человека в день: повторный выбор обновляет активность
create unique index ux_proposal_active on proposal (participant_id, day) where status = 'ACTIVE';
create index ix_proposal_day on proposal (day);

create table activity_match
(
    id         bigserial   primary key,
    day        date        not null,
    activity   varchar(32) not null,
    created_at timestamptz not null default now()
);

-- один матч на день — заодно защита от повторной отправки стикеров
create unique index ux_activity_match_day on activity_match (day);
