create table auth_user(
    id UUID,
    username varchar,
    password varchar,
    name: varchar,
    admin: boolean not null default false
);
