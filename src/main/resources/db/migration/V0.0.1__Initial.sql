CREATE TABLE item_eiv
(
    item_id bigint NOT NULL PRIMARY KEY,
    adjusted_price decimal,
    average_price decimal,
    updated_at timestamp without time zone
);