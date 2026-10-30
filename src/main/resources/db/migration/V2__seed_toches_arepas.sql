-- Demo tenant: Toche's Arepas (venezuelan street food, San Fernando, Cali).
-- Menu transcribed from the menu board published on its Google Maps profile.

DO $$
DECLARE
    t        BIGINT;
    c_armala BIGINT;
    c_casa   BIGINT;
    c_bebida BIGINT;
    d        BIGINT;
    rellenos JSONB := '[
        {"id": "chicharron", "name": "Chicharrón", "price": 0},
        {"id": "chorizo", "name": "Chorizo santarrosano", "price": 0},
        {"id": "maduritos", "name": "Maduritos", "price": 0},
        {"id": "carne", "name": "Carne desmechada", "price": 0},
        {"id": "cerdo", "name": "Salteado de cerdo", "price": 0},
        {"id": "pollo", "name": "Pollo desmechado", "price": 0},
        {"id": "codorniz", "name": "Huevo de codorniz", "price": 0},
        {"id": "reina", "name": "Reina pepiada (pollo desmechado y aguacate)", "price": 0},
        {"id": "tocineta", "name": "Queso con tocineta", "price": 0},
        {"id": "costilla", "name": "Queso con costilla", "price": 0}
    ]';
BEGIN
    INSERT INTO tenant (slug, name, contact_phone, address, branding)
    VALUES ('toches-arepas', 'Toche''s Arepas', '+573148094166', 'Cl. 5B #36, San Fernando, Cali',
            '{
               "tagline": "Comida rápida venezolana",
               "hours": "Abrimos desde las 6:00 p. m.",
               "phones": ["+573148094166", "+573024665548"],
               "delivery": true,
               "paymentNote": "Paga aquí con Nequi",
               "mapsUrl": "https://maps.app.goo.gl/w2CNvdtMfphZfoQdA"
             }'::jsonb)
    RETURNING id INTO t;

    INSERT INTO dining_table (tenant_id, number) SELECT t, n FROM generate_series(1, 6) AS n;

    INSERT INTO payment_method (tenant_id, name, position) VALUES
        (t, 'Efectivo', 1), (t, 'Nequi', 2), (t, 'Transferencia', 3);

    INSERT INTO tag (tenant_id, name) VALUES (t, 'Vegetariana'), (t, 'De la casa');

    INSERT INTO ingredient (tenant_id, name, allergen) VALUES
        (t, 'Masa de maíz', false),
        (t, 'Queso mozzarella', true),
        (t, 'Pollo desmechado', false),
        (t, 'Carne desmechada', false),
        (t, 'Aguacate', false),
        (t, 'Caraotas', false),
        (t, 'Tajadas de maduro', false),
        (t, 'Fruta natural', false);

    INSERT INTO menu_category (tenant_id, name, description, position) VALUES
        (t, 'Ármala a tu gusto', 'Arepa rellena con los ingredientes que quieras', 1)
        RETURNING id INTO c_armala;
    INSERT INTO menu_category (tenant_id, name, description, position) VALUES
        (t, 'Arepas de la casa', 'Las clásicas venezolanas', 2)
        RETURNING id INTO c_casa;
    INSERT INTO menu_category (tenant_id, name, description, position) VALUES
        (t, 'Bebidas', NULL, 3)
        RETURNING id INTO c_bebida;

    -- Build your own: the price depends on how many fillings
    INSERT INTO dish (tenant_id, category_id, name, description, price, position, modifiers) VALUES
        (t, c_armala, 'Arepa con 1 relleno', 'Escoge un relleno.', 10000, 1,
         jsonb_build_array(jsonb_build_object('id', 'rellenos', 'name', 'Relleno', 'min', 1, 'max', 1, 'options', rellenos)))
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, false FROM ingredient WHERE tenant_id = t AND name = 'Masa de maíz';

    INSERT INTO dish (tenant_id, category_id, name, description, price, position, modifiers) VALUES
        (t, c_armala, 'Arepa con 2 rellenos', 'Escoge dos rellenos.', 15000, 2,
         jsonb_build_array(jsonb_build_object('id', 'rellenos', 'name', 'Rellenos', 'min', 2, 'max', 2, 'options', rellenos)))
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, false FROM ingredient WHERE tenant_id = t AND name = 'Masa de maíz';

    INSERT INTO dish (tenant_id, category_id, name, description, price, position, modifiers) VALUES
        (t, c_armala, 'Arepa con 4 rellenos', 'Escoge cuatro rellenos.', 20000, 3,
         jsonb_build_array(jsonb_build_object('id', 'rellenos', 'name', 'Rellenos', 'min', 4, 'max', 4, 'options', rellenos)))
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, false FROM ingredient WHERE tenant_id = t AND name = 'Masa de maíz';

    -- House arepas
    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_casa, 'Arepa Sifrina', 'Reina pepiada con queso mozzarella rallado.', 15000, 1)
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, name IN ('Aguacate', 'Queso mozzarella') FROM ingredient
        WHERE tenant_id = t AND name IN ('Masa de maíz', 'Pollo desmechado', 'Aguacate', 'Queso mozzarella');

    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_casa, 'Arepa Pelúa', 'Carne desmechada con queso mozzarella rallado.', 15000, 2)
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, name = 'Queso mozzarella' FROM ingredient
        WHERE tenant_id = t AND name IN ('Masa de maíz', 'Carne desmechada', 'Queso mozzarella');

    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_casa, 'Arepa de Pabellón', 'Caraotas, tajadas de maduro, carne desmechada y queso mozzarella.', 20000, 3)
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, name <> 'Masa de maíz' AND name <> 'Carne desmechada' FROM ingredient
        WHERE tenant_id = t AND name IN ('Masa de maíz', 'Caraotas', 'Tajadas de maduro', 'Carne desmechada', 'Queso mozzarella');
    INSERT INTO dish_tag (dish_id, tag_id) SELECT d, id FROM tag WHERE tenant_id = t AND name = 'De la casa';

    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_casa, 'Arepa Dominó', 'Caraotas con queso mozzarella rallado.', 15000, 4)
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, name = 'Queso mozzarella' FROM ingredient
        WHERE tenant_id = t AND name IN ('Masa de maíz', 'Caraotas', 'Queso mozzarella');
    INSERT INTO dish_tag (dish_id, tag_id) SELECT d, id FROM tag WHERE tenant_id = t AND name = 'Vegetariana';

    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_casa, 'Arepa Catira', 'Pollo desmechado con queso mozzarella rallado.', 15000, 5)
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, name = 'Queso mozzarella' FROM ingredient
        WHERE tenant_id = t AND name IN ('Masa de maíz', 'Pollo desmechado', 'Queso mozzarella');

    -- Drinks
    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_bebida, 'Jugo natural', 'Pregunta por los sabores del día.', 3000, 1)
        RETURNING id INTO d;
    INSERT INTO dish_ingredient (dish_id, ingredient_id, removable)
        SELECT d, id, false FROM ingredient WHERE tenant_id = t AND name = 'Fruta natural';
    INSERT INTO dish_tag (dish_id, tag_id) SELECT d, id FROM tag WHERE tenant_id = t AND name = 'Vegetariana';

    INSERT INTO dish (tenant_id, category_id, name, description, price, position) VALUES
        (t, c_bebida, 'Gaseosa', NULL, 4000, 2);
END $$;
