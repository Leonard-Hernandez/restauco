-- Multi-tenant SaaS: every restaurant is a tenant; data is scoped by tenant_id.

CREATE TABLE tenant (
    id            BIGSERIAL    PRIMARY KEY,
    slug          VARCHAR(60)  NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    timezone      VARCHAR(60)  NOT NULL DEFAULT 'America/Bogota',
    contact_phone VARCHAR(30),
    address       VARCHAR(255),
    branding      JSONB        NOT NULL DEFAULT '{}'::jsonb,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Staff allowed into the panel. Invited by email; authco_sub is linked on the first
-- authenticated visit (authco has no notion of tenants).
CREATE TABLE tenant_member (
    id         BIGSERIAL    PRIMARY KEY,
    tenant_id  BIGINT       NOT NULL REFERENCES tenant (id),
    email      VARCHAR(150) NOT NULL,
    authco_sub VARCHAR(64),
    name       VARCHAR(120),
    role       VARCHAR(10)  NOT NULL CHECK (role IN ('OWNER', 'WAITER', 'COOK')),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, email),
    UNIQUE (tenant_id, authco_sub)
);

-- Menu ---------------------------------------------------------------------------------

CREATE TABLE menu_category (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL REFERENCES tenant (id),
    name        VARCHAR(80)  NOT NULL,
    description VARCHAR(255),
    position    INT          NOT NULL DEFAULT 0,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_category_tenant ON menu_category (tenant_id, position);

CREATE TABLE ingredient (
    id        BIGSERIAL   PRIMARY KEY,
    tenant_id BIGINT      NOT NULL REFERENCES tenant (id),
    name      VARCHAR(80) NOT NULL,
    -- Shown as an allergy warning (dairy, gluten, nuts...)
    allergen  BOOLEAN     NOT NULL DEFAULT false,
    UNIQUE (tenant_id, name)
);

-- Dietary or style labels (vegetariano, picante...)
CREATE TABLE tag (
    id        BIGSERIAL   PRIMARY KEY,
    tenant_id BIGINT      NOT NULL REFERENCES tenant (id),
    name      VARCHAR(40) NOT NULL,
    UNIQUE (tenant_id, name)
);

-- price in whole pesos. modifiers: [{id, name, min, max, options: [{id, name, price}]}]
CREATE TABLE dish (
    id          BIGSERIAL    PRIMARY KEY,
    tenant_id   BIGINT       NOT NULL REFERENCES tenant (id),
    category_id BIGINT       NOT NULL REFERENCES menu_category (id),
    name        VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    price       BIGINT       NOT NULL CHECK (price >= 0),
    image_path  VARCHAR(500),
    -- false = "agotado" today (still listed); active = false hides it from the menu
    available   BOOLEAN      NOT NULL DEFAULT true,
    active      BOOLEAN      NOT NULL DEFAULT true,
    position    INT          NOT NULL DEFAULT 0,
    modifiers   JSONB        NOT NULL DEFAULT '[]'::jsonb,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_dish_tenant ON dish (tenant_id, category_id, position);

-- removable = the guest can ask for it without that ingredient ("sin cebolla")
CREATE TABLE dish_ingredient (
    dish_id       BIGINT  NOT NULL REFERENCES dish (id) ON DELETE CASCADE,
    ingredient_id BIGINT  NOT NULL REFERENCES ingredient (id) ON DELETE CASCADE,
    removable     BOOLEAN NOT NULL DEFAULT false,
    PRIMARY KEY (dish_id, ingredient_id)
);

CREATE TABLE dish_tag (
    dish_id BIGINT NOT NULL REFERENCES dish (id) ON DELETE CASCADE,
    tag_id  BIGINT NOT NULL REFERENCES tag (id) ON DELETE CASCADE,
    PRIMARY KEY (dish_id, tag_id)
);

-- Floor ----------------------------------------------------------------------------------

CREATE TABLE dining_table (
    id         BIGSERIAL   PRIMARY KEY,
    tenant_id  BIGINT      NOT NULL REFERENCES tenant (id),
    number     INT         NOT NULL CHECK (number > 0),
    label      VARCHAR(40),
    -- Printed in the table QR; regenerating it invalidates old prints
    qr_token   UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    active     BOOLEAN     NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (tenant_id, number)
);

CREATE TABLE payment_method (
    id        BIGSERIAL   PRIMARY KEY,
    tenant_id BIGINT      NOT NULL REFERENCES tenant (id),
    name      VARCHAR(40) NOT NULL,
    position  INT         NOT NULL DEFAULT 0,
    active    BOOLEAN     NOT NULL DEFAULT true,
    UNIQUE (tenant_id, name)
);

-- The bill of a table: opened by the first order, closed when paid.
CREATE TABLE table_session (
    id                  BIGSERIAL   PRIMARY KEY,
    tenant_id           BIGINT      NOT NULL REFERENCES tenant (id),
    table_id            BIGINT      NOT NULL REFERENCES dining_table (id),
    status              VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'CLOSED')),
    opened_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    closed_at           TIMESTAMPTZ,
    closed_by           VARCHAR(64),
    payment_method_id   BIGINT      REFERENCES payment_method (id),
    -- Copied at close, so renaming a method never rewrites past bills
    payment_method_name VARCHAR(40),
    total               BIGINT
);
CREATE UNIQUE INDEX uq_session_open_per_table ON table_session (table_id) WHERE status = 'OPEN';
CREATE INDEX idx_session_tenant ON table_session (tenant_id, status);

-- One round of orders sent to the kitchen. items are snapshots (name, price, modifiers)
-- taken when ordered: later menu edits never change a ticket or a bill.
CREATE TABLE order_ticket (
    id            BIGSERIAL   PRIMARY KEY,
    tenant_id     BIGINT      NOT NULL REFERENCES tenant (id),
    session_id    BIGINT      NOT NULL REFERENCES table_session (id),
    table_id      BIGINT      NOT NULL REFERENCES dining_table (id),
    table_number  INT         NOT NULL,
    status        VARCHAR(20) NOT NULL CHECK (status IN
                      ('PENDING_APPROVAL', 'SENT', 'PREPARING', 'READY', 'DELIVERED', 'CANCELLED')),
    source        VARCHAR(10) NOT NULL CHECK (source IN ('WAITER', 'QR')),
    waiter_sub    VARCHAR(64),
    items         JSONB       NOT NULL,
    total         BIGINT      NOT NULL CHECK (total >= 0),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at       TIMESTAMPTZ,
    started_at    TIMESTAMPTZ,
    ready_at      TIMESTAMPTZ,
    delivered_at  TIMESTAMPTZ,
    cancelled_at  TIMESTAMPTZ,
    cancel_reason VARCHAR(140),
    -- Optimistic lock: two cooks tapping "Listo" at once must not both win
    version       BIGINT      NOT NULL DEFAULT 0
);
CREATE INDEX idx_ticket_tenant_status ON order_ticket (tenant_id, status);
CREATE INDEX idx_ticket_session ON order_ticket (session_id);
