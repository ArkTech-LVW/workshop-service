WITH seeded_user AS (
    INSERT INTO "user" (name, email, password_hash, active, date_created, last_updated, version)
    VALUES
        ('Carla Mendes', 'gerente@workshop.local',
         '{argon2}$argon2id$v=19$m=16384,t=2,p=1$9mT26OzLgkbw8nQ7g5zkjA$JNRaCeEsHynBQJiWX8GewP51acKeq9uDeJTNozO51L0',
         TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('Ana Souza', 'atendente@workshop.local',
         '{argon2}$argon2id$v=19$m=16384,t=2,p=1$9mT26OzLgkbw8nQ7g5zkjA$JNRaCeEsHynBQJiWX8GewP51acKeq9uDeJTNozO51L0',
         TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('Bruno Lima', 'mecanico@workshop.local',
         '{argon2}$argon2id$v=19$m=16384,t=2,p=1$9mT26OzLgkbw8nQ7g5zkjA$JNRaCeEsHynBQJiWX8GewP51acKeq9uDeJTNozO51L0',
         TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0)
    ON CONFLICT (email) DO NOTHING
    RETURNING id, email
)
INSERT INTO user_role (user_id, role, date_created, last_updated, version)
SELECT seeded_user.id, seeded_role.role, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0
FROM seeded_user
JOIN (VALUES
        ('gerente@workshop.local', 'ADMIN'),
        ('atendente@workshop.local', 'ATTENDANT'),
        ('mecanico@workshop.local', 'MECHANIC')
    ) AS seeded_role (email, role) ON seeded_role.email = seeded_user.email;

INSERT INTO customer (name, document, person_type, email, phone, date_created, last_updated, version)
VALUES
    ('João da Silva', '52998224725', 'NATURAL_PERSON', 'joao.silva@example.com', '85988887777', now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Maria Oliveira', '12345678909', 'NATURAL_PERSON', 'maria.oliveira@example.com', '11987654321', now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Transportes Rápido Ltda', '11222333000181', 'LEGAL_PERSON', 'frota@example.com', '8532345678', now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0)
ON CONFLICT (document) DO NOTHING;

INSERT INTO vehicle (customer_id, license_plate, chassis, brand, model, year, color, date_created, last_updated, version)
SELECT customer.id, seeded_vehicle.license_plate, seeded_vehicle.chassis, seeded_vehicle.brand,
       seeded_vehicle.model, seeded_vehicle.year, seeded_vehicle.color, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0
FROM (VALUES
        ('52998224725', 'ABC1D23', '9BWZZZ370RT004321', 'Volkswagen', 'Gol 1.0', 2019, 'Prata'),
        ('52998224725', 'QWE4567', NULL, 'Honda', 'Fit EX', 2016, 'Preto'),
        ('12345678909', 'MOL2B34', '9BD197161ST123456', 'Fiat', 'Argo Drive', 2022, 'Branco'),
        ('11222333000181', 'RAP3C45', '9BG148HK9TC112233', 'Chevrolet', 'S10 LTZ', 2023, 'Vermelho')
    ) AS seeded_vehicle (document, license_plate, chassis, brand, model, year, color)
JOIN customer ON customer.document = seeded_vehicle.document
ON CONFLICT DO NOTHING;

INSERT INTO service_offering (name, description, labor_price, estimated_minutes, active, date_created, last_updated, version)
VALUES
    ('Troca de óleo e filtro', 'Troca do óleo do motor e do filtro de óleo', 89.90, 40, TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Alinhamento e balanceamento', 'Alinhamento da direção e balanceamento das quatro rodas', 149.90, 60, TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Revisão do sistema de freios', 'Inspeção e troca de pastilhas, discos e fluido de freio', 199.90, 90, TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Diagnóstico eletrônico', 'Leitura de falhas da central eletrônica com scanner', 120.00, 45, TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Troca de correia dentada', 'Substituição do kit de correia dentada e tensor', 350.00, 180, TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
    ('Higienização do ar-condicionado', 'Limpeza do sistema e troca do filtro de cabine', 129.90, 50, TRUE, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0);

WITH seeded_part AS (
    INSERT INTO part (sku, name, type, unit, sale_price, stock_quantity, minimum_stock, date_created, last_updated, version)
    VALUES
        ('OLEO-5W30', 'Óleo de motor 5W30 sintético', 'SUPPLY', 'LITER', 45.90, 40, 10, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('FLT-OLEO-01', 'Filtro de óleo', 'PART', 'UNIT', 32.50, 15, 5, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('FLT-AR-01', 'Filtro de ar do motor', 'PART', 'UNIT', 48.00, 10, 4, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('FLT-CAB-01', 'Filtro de cabine', 'PART', 'UNIT', 39.90, 8, 4, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('PAST-FREIO-D', 'Pastilha de freio dianteira (jogo)', 'PART', 'UNIT', 159.90, 6, 2, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('DISCO-FREIO-D', 'Disco de freio dianteiro', 'PART', 'UNIT', 219.00, 4, 2, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('FLUIDO-DOT4', 'Fluido de freio DOT 4', 'SUPPLY', 'LITER', 38.50, 12, 3, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('ADIT-RAD', 'Aditivo para radiador', 'SUPPLY', 'LITER', 29.90, 20, 5, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('KIT-CORREIA-DENT', 'Kit de correia dentada', 'PART', 'UNIT', 289.00, 2, 3, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0),
        ('GRAXA-MP', 'Graxa multiuso', 'SUPPLY', 'KG', 42.00, 5, 2, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0)
    ON CONFLICT (sku) DO NOTHING
    RETURNING id, stock_quantity
)
INSERT INTO stock_movement (part_id, type, quantity, reason, created_by_user_id, date_created, last_updated, version)
SELECT seeded_part.id, 'IN', seeded_part.stock_quantity, 'Estoque inicial de demonstração', "user".id, now() - INTERVAL '30 days', now() - INTERVAL '30 days', 0
FROM seeded_part
JOIN "user" ON "user".email = 'gerente@workshop.local';
