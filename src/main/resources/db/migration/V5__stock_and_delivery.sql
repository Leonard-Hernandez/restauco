-- Sold-out ingredients --------------------------------------------------------------------

-- false = ran out today. A dish that needs it cannot be ordered; one that can go without it
-- is served without it; an option made with it (a filling) cannot be chosen.
ALTER TABLE ingredient ADD COLUMN available BOOLEAN NOT NULL DEFAULT true;

-- Options (fillings) now name the ingredients they are made with: modifiers[].options[].ingredientIds.
-- Link Toche's fillings to the catalog, adding the ones it did not have yet.
DO $$
DECLARE
    t BIGINT;
    links JSONB;
BEGIN
    SELECT id INTO t FROM tenant WHERE slug = 'toches-arepas';
    IF t IS NULL THEN RETURN; END IF;

    INSERT INTO ingredient (tenant_id, name, allergen) VALUES
        (t, 'Chicharrón', false), (t, 'Chorizo santarrosano', false), (t, 'Salteado de cerdo', false),
        (t, 'Huevo de codorniz', false), (t, 'Tocineta', false), (t, 'Costilla', false)
    ON CONFLICT (tenant_id, name) DO NOTHING;

    -- option id -> ingredient names
    SELECT jsonb_object_agg(o.option_id, (
               SELECT COALESCE(jsonb_agg(i.id ORDER BY i.id), '[]'::jsonb)
               FROM ingredient i WHERE i.tenant_id = t AND i.name = ANY (o.names)))
    INTO links
    FROM (VALUES
        ('chicharron', ARRAY['Chicharrón']),
        ('chorizo', ARRAY['Chorizo santarrosano']),
        ('maduritos', ARRAY['Tajadas de maduro']),
        ('carne', ARRAY['Carne desmechada']),
        ('cerdo', ARRAY['Salteado de cerdo']),
        ('pollo', ARRAY['Pollo desmechado']),
        ('codorniz', ARRAY['Huevo de codorniz']),
        ('reina', ARRAY['Pollo desmechado', 'Aguacate']),
        ('tocineta', ARRAY['Tocineta']),
        ('costilla', ARRAY['Costilla'])
    ) AS o(option_id, names);

    UPDATE dish d
    SET modifiers = (
        SELECT jsonb_agg(jsonb_set(g, '{options}', (
                   SELECT COALESCE(jsonb_agg(CASE WHEN links ? (opt ->> 'id') AND NOT opt ? 'ingredientIds'
                                                  THEN opt || jsonb_build_object('ingredientIds', links -> (opt ->> 'id'))
                                                  ELSE opt END ORDER BY n), '[]'::jsonb)
                   FROM jsonb_array_elements(g -> 'options') WITH ORDINALITY AS x(opt, n))) ORDER BY gn)
        FROM jsonb_array_elements(d.modifiers) WITH ORDINALITY AS y(g, gn))
    WHERE d.tenant_id = t AND jsonb_array_length(d.modifiers) > 0;
END $$;

-- Delivery ------------------------------------------------------------------------------

-- {enabled, lat, lng, radiusKm, fee, minOrder}: where the restaurant is and how far it delivers
ALTER TABLE tenant ADD COLUMN delivery JSONB NOT NULL DEFAULT '{}'::jsonb;
UPDATE tenant
SET delivery = '{"enabled": true, "lat": 3.4307, "lng": -76.5428, "radiusKm": 4, "fee": 0, "minOrder": 0}'
WHERE slug = 'toches-arepas';

-- A delivery is a bill with no table: same rounds, kitchen and payment as a table, so sales
-- reports count it with no changes. CANCELLED = rejected before anything was charged.
ALTER TABLE table_session ALTER COLUMN table_id DROP NOT NULL;
ALTER TABLE table_session ADD COLUMN delivery_fee BIGINT NOT NULL DEFAULT 0 CHECK (delivery_fee >= 0);
ALTER TABLE table_session DROP CONSTRAINT table_session_status_check;
ALTER TABLE table_session ADD CONSTRAINT table_session_status_check CHECK (status IN ('OPEN', 'CLOSED', 'CANCELLED'));

-- address is what the customer wrote; lat/lng the pin they confirmed on the map, and
-- place_label what the map says is there (checked by the API when ordering).
CREATE TABLE delivery_order (
    id                  BIGSERIAL    PRIMARY KEY,
    tenant_id           BIGINT       NOT NULL REFERENCES tenant (id),
    session_id          BIGINT       NOT NULL UNIQUE REFERENCES table_session (id),
    -- The customer follows the order with it; never shown to anyone else
    token               UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    customer_name       VARCHAR(80)  NOT NULL,
    phone               VARCHAR(20)  NOT NULL,
    address             VARCHAR(160) NOT NULL,
    address_detail      VARCHAR(120),
    lat                 DOUBLE PRECISION NOT NULL,
    lng                 DOUBLE PRECISION NOT NULL,
    place_label         VARCHAR(255) NOT NULL,
    -- The street the map found under the pin is in the written address
    matches_address     BOOLEAN      NOT NULL,
    distance_m          INT          NOT NULL,
    payment_method_id   BIGINT       REFERENCES payment_method (id),
    payment_method_name VARCHAR(40),
    -- Cash: the bill the customer pays with, so the courier brings change
    cash_for            BIGINT,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_delivery_tenant ON delivery_order (tenant_id, created_at);
CREATE INDEX idx_delivery_phone ON delivery_order (tenant_id, phone, created_at);

ALTER TABLE order_ticket ALTER COLUMN table_id DROP NOT NULL;
ALTER TABLE order_ticket ALTER COLUMN table_number DROP NOT NULL;
ALTER TABLE order_ticket ADD COLUMN delivery_id BIGINT REFERENCES delivery_order (id);
ALTER TABLE order_ticket ADD CONSTRAINT order_ticket_place_check CHECK (table_id IS NOT NULL OR delivery_id IS NOT NULL);
ALTER TABLE order_ticket DROP CONSTRAINT order_ticket_source_check;
ALTER TABLE order_ticket ADD CONSTRAINT order_ticket_source_check CHECK (source IN ('WAITER', 'QR', 'DELIVERY'));
