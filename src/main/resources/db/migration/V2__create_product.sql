create table product (
    id UUID NOT NULL PRIMARY KEY,
    product_type varchar not null,
    title varchar not null,
    description jsonb,
    UNIQUE (title, product_type)
);
