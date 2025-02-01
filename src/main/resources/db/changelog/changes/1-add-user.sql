CREATE TABLE users
(
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(255)  NOT NULL UNIQUE,
    expire_date   DATE          NOT NULL,
    user_data      varchar(1000) NOT NULL,
    full_name     VARCHAR(255)  NOT NULL
);