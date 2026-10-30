-- Dictated orders: what the waiter said, what was understood and the round finally sent.
-- Kept to measure the parser and, later, to train a model on this restaurant's way of ordering.
CREATE TABLE voice_sample (
    id         BIGSERIAL   PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL REFERENCES tenant (id),
    transcript TEXT        NOT NULL,
    parsed     JSONB       NOT NULL,
    ticket_id  BIGINT      REFERENCES order_ticket (id) ON DELETE SET NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX voice_sample_tenant_created ON voice_sample (tenant_id, created_at);
