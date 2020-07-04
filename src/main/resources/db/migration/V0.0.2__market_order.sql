CREATE TABLE market_order
(
    order_id      bigint                   not null primary key,
    duration      int                      not null,
    is_buy_order  boolean                  not null,
    issued        timestamp with time zone not null,
    location_id   bigint                   not null,
    min_volume    int                      not null,
    price         decimal                  not null,
    range         varchar(15)              not null,
    system_id     bigint                   not null,
    type_id       bigint                   not null,
    volume_remain int                      not null,
    volume_total  int                      not null
);