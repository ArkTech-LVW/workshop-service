CREATE TEMPORARY TABLE demo_service_order (
    public_id            UUID         PRIMARY KEY,
    sequence             INTEGER      NOT NULL,
    license_plate        VARCHAR(7)   NOT NULL,
    mechanic_email       VARCHAR(254),
    problem_description  TEXT         NOT NULL,
    status               VARCHAR(40)  NOT NULL,
    opened_at            TIMESTAMPTZ  NOT NULL
) ON COMMIT DROP;

CREATE TEMPORARY TABLE demo_status_change (
    public_id    UUID         NOT NULL,
    from_status  VARCHAR(40),
    to_status    VARCHAR(40)  NOT NULL,
    user_email   VARCHAR(254),
    description  TEXT,
    changed_at   TIMESTAMPTZ  NOT NULL
) ON COMMIT DROP;

CREATE TEMPORARY TABLE demo_service_item (
    public_id       UUID          NOT NULL,
    service_name    VARCHAR(150)  NOT NULL,
    quantity        INTEGER       NOT NULL,
    approved_at     TIMESTAMPTZ,
    started_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    mechanic_email  VARCHAR(254)
) ON COMMIT DROP;

CREATE TEMPORARY TABLE demo_part_item (
    public_id    UUID         NOT NULL,
    sku          VARCHAR(50)  NOT NULL,
    quantity     INTEGER      NOT NULL,
    approved_at  TIMESTAMPTZ
) ON COMMIT DROP;

INSERT INTO demo_service_order VALUES
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 1, 'ABC1D23', 'mecanico@workshop.local',
     'Troca de óleo no prazo e luz do óleo acendendo no painel', 'DELIVERED', now() - INTERVAL '20 days'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 2, 'RAP3C45', 'mecanico@workshop.local',
     'Barulho ao frear e pedal de freio baixo', 'DELIVERED', now() - INTERVAL '12 days'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 3, 'MOL2B34', 'mecanico@workshop.local',
     'Carro puxando para a direita e volante torto', 'COMPLETED', now() - INTERVAL '3 days'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 4, 'QWE4567', 'mecanico@workshop.local',
     'Revisão dos 60 mil km com troca da correia dentada', 'IN_PROGRESS', now() - INTERVAL '2 days'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05', 5, 'ABC1D23', 'mecanico@workshop.local',
     'Luz da injeção acesa e falha na aceleração', 'AWAITING_APPROVAL', now() - INTERVAL '1 day'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d06', 6, 'MOL2B34', 'mecanico@workshop.local',
     'Ar-condicionado com mau cheiro e pouco vento', 'IN_DIAGNOSIS', now() - INTERVAL '5 hours'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d07', 7, 'RAP3C45', NULL,
     'Vibração no volante acima de 80 km/h', 'RECEIVED', now() - INTERVAL '1 hour'),
    ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 8, 'QWE4567', 'mecanico@workshop.local',
     'Revisão dos freios antes de viagem', 'CANCELED', now() - INTERVAL '8 days');

INSERT INTO demo_status_change
SELECT order_status.public_id, order_status.from_status, order_status.to_status, order_status.user_email,
       order_status.description, demo_service_order.opened_at + order_status.elapsed
FROM (VALUES
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01'::UUID, NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '1 hour'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'mecanico@workshop.local', 'Óleo vencido e filtro saturado', INTERVAL '2 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', NULL, NULL, INTERVAL '4 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'QUOTE_APPROVED', 'IN_PROGRESS', 'mecanico@workshop.local', NULL, INTERVAL '5 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'IN_PROGRESS', 'COMPLETED', 'mecanico@workshop.local', NULL, INTERVAL '6 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'COMPLETED', 'DELIVERED', 'atendente@workshop.local', NULL, INTERVAL '1 day'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '2 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'mecanico@workshop.local', 'Pastilhas no limite e discos abaixo da espessura mínima', INTERVAL '4 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', 'atendente@workshop.local', 'Aprovado no balcão pelo responsável da frota', INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'QUOTE_APPROVED', 'IN_PROGRESS', 'mecanico@workshop.local', NULL, INTERVAL '1 day 1 hour'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'IN_PROGRESS', 'COMPLETED', 'mecanico@workshop.local', NULL, INTERVAL '2 days'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'COMPLETED', 'DELIVERED', 'atendente@workshop.local', NULL, INTERVAL '2 days 3 hours'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '1 hour'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'mecanico@workshop.local', 'Geometria fora do padrão', INTERVAL '2 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', NULL, NULL, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 'QUOTE_APPROVED', 'IN_PROGRESS', 'mecanico@workshop.local', NULL, INTERVAL '1 day 1 hour'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 'IN_PROGRESS', 'COMPLETED', 'mecanico@workshop.local', NULL, INTERVAL '1 day 2 hours'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '1 hour'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'mecanico@workshop.local', 'Correia no fim da vida útil', INTERVAL '3 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'AWAITING_APPROVAL', 'QUOTE_APPROVED', NULL, NULL, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'QUOTE_APPROVED', 'IN_PROGRESS', 'mecanico@workshop.local', NULL, INTERVAL '1 day 1 hour'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '30 minutes'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'mecanico@workshop.local', 'Sensor com leitura instável, filtro de ar obstruído', INTERVAL '2 hours'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d06', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d06', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '1 hour'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d07', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),

        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', NULL, 'RECEIVED', 'atendente@workshop.local', NULL, INTERVAL '0'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 'RECEIVED', 'IN_DIAGNOSIS', 'mecanico@workshop.local', NULL, INTERVAL '1 hour'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'mecanico@workshop.local', NULL, INTERVAL '2 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 'AWAITING_APPROVAL', 'QUOTE_REJECTED', NULL, 'Cliente achou o valor alto', INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 'QUOTE_REJECTED', 'CANCELED', 'atendente@workshop.local', 'Cliente desistiu do serviço', INTERVAL '1 day 1 hour')
    ) AS order_status (public_id, from_status, to_status, user_email, description, elapsed)
JOIN demo_service_order ON demo_service_order.public_id = order_status.public_id;

INSERT INTO demo_service_item
SELECT item.public_id, item.service_name, item.quantity,
       demo_service_order.opened_at + item.approved_after,
       demo_service_order.opened_at + item.started_after,
       demo_service_order.opened_at + item.completed_after,
       CASE WHEN item.started_after IS NULL THEN NULL ELSE demo_service_order.mechanic_email END
FROM (VALUES
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01'::UUID, 'Troca de óleo e filtro', 1,
         INTERVAL '4 hours', INTERVAL '5 hours', INTERVAL '6 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'Revisão do sistema de freios', 1,
         INTERVAL '1 day', INTERVAL '1 day 1 hour', INTERVAL '1 day 4 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'Alinhamento e balanceamento', 1,
         INTERVAL '1 day', INTERVAL '1 day 4 hours', INTERVAL '2 days'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d03', 'Alinhamento e balanceamento', 1,
         INTERVAL '1 day', INTERVAL '1 day 1 hour', INTERVAL '1 day 2 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'Troca de correia dentada', 1,
         INTERVAL '1 day', INTERVAL '1 day 1 hour', NULL),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'Troca de óleo e filtro', 1,
         INTERVAL '1 day', NULL, NULL),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05', 'Diagnóstico eletrônico', 1, NULL, NULL, NULL),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d06', 'Higienização do ar-condicionado', 1, NULL, NULL, NULL),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 'Revisão do sistema de freios', 1, NULL, NULL, NULL)
    ) AS item (public_id, service_name, quantity, approved_after, started_after, completed_after)
JOIN demo_service_order ON demo_service_order.public_id = item.public_id;

INSERT INTO demo_part_item
SELECT item.public_id, item.sku, item.quantity, demo_service_order.opened_at + item.approved_after
FROM (VALUES
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01'::UUID, 'OLEO-5W30', 4, INTERVAL '4 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d01', 'FLT-OLEO-01', 1, INTERVAL '4 hours'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'PAST-FREIO-D', 1, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'DISCO-FREIO-D', 2, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d02', 'FLUIDO-DOT4', 1, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'KIT-CORREIA-DENT', 1, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'ADIT-RAD', 2, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'OLEO-5W30', 4, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d04', 'FLT-OLEO-01', 1, INTERVAL '1 day'),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d05', 'FLT-AR-01', 1, NULL),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d06', 'FLT-CAB-01', 1, NULL),
        ('7d3f1a52-0c4e-4b8a-9f21-5e6a7b8c9d08', 'PAST-FREIO-D', 1, NULL)
    ) AS item (public_id, sku, quantity, approved_after)
JOIN demo_service_order ON demo_service_order.public_id = item.public_id;

WITH counter AS (
    INSERT INTO service_order_number_counter (year, last_value)
    VALUES (extract(YEAR FROM now() AT TIME ZONE 'America/Sao_Paulo')::INTEGER,
            (SELECT count(*) FROM demo_service_order))
    ON CONFLICT (year) DO UPDATE
    SET last_value = service_order_number_counter.last_value + EXCLUDED.last_value,
        last_updated = now(),
        version = service_order_number_counter.version + 1
    RETURNING year, last_value
)
INSERT INTO service_order (public_id, number, customer_id, vehicle_id, mechanic_id, problem_description, status,
                           total_price, date_created, last_updated, version)
SELECT demo_service_order.public_id,
       format('SO-%s-%s', counter.year,
              lpad((counter.last_value - (SELECT count(*) FROM demo_service_order) + demo_service_order.sequence)::TEXT,
                   6, '0')),
       vehicle.customer_id, vehicle.id, mechanic.id, demo_service_order.problem_description,
       demo_service_order.status, 0, demo_service_order.opened_at,
       (SELECT max(changed_at) FROM demo_status_change
        WHERE demo_status_change.public_id = demo_service_order.public_id),
       0
FROM demo_service_order
CROSS JOIN counter
JOIN vehicle ON vehicle.license_plate = demo_service_order.license_plate
LEFT JOIN "user" AS mechanic ON mechanic.email = demo_service_order.mechanic_email;

INSERT INTO status_change (service_order_id, from_status, to_status, changed_by_user_id, description,
                           date_created, last_updated, version)
SELECT service_order.id, demo_status_change.from_status, demo_status_change.to_status, changed_by.id,
       demo_status_change.description, demo_status_change.changed_at, demo_status_change.changed_at, 0
FROM demo_status_change
JOIN service_order ON service_order.public_id = demo_status_change.public_id
LEFT JOIN "user" AS changed_by ON changed_by.email = demo_status_change.user_email
ORDER BY demo_status_change.public_id, demo_status_change.changed_at;

INSERT INTO service_item (service_order_id, service_offering_id, quantity, unit_price, approved_at, started_at,
                          started_by_user_id, completed_at, completed_by_user_id, date_created, last_updated, version)
SELECT service_order.id, service_offering.id, demo_service_item.quantity, service_offering.labor_price,
       demo_service_item.approved_at, demo_service_item.started_at, started_by.id, demo_service_item.completed_at,
       CASE WHEN demo_service_item.completed_at IS NULL THEN NULL ELSE started_by.id END,
       service_order.date_created, service_order.last_updated, 0
FROM demo_service_item
JOIN service_order ON service_order.public_id = demo_service_item.public_id
JOIN LATERAL (
    SELECT id, labor_price FROM service_offering
    WHERE service_offering.name = demo_service_item.service_name
    ORDER BY id
    LIMIT 1
) AS service_offering ON TRUE
LEFT JOIN "user" AS started_by ON started_by.email = demo_service_item.mechanic_email;

INSERT INTO part_item (service_order_id, part_id, quantity, unit_price, approved_at, date_created, last_updated,
                       version)
SELECT service_order.id, part.id, demo_part_item.quantity, part.sale_price, demo_part_item.approved_at,
       service_order.date_created, service_order.last_updated, 0
FROM demo_part_item
JOIN service_order ON service_order.public_id = demo_part_item.public_id
JOIN part ON part.sku = demo_part_item.sku;

UPDATE service_order
SET total_price = (
        SELECT coalesce(sum(quantity * unit_price), 0) FROM service_item
        WHERE service_item.service_order_id = service_order.id)
    + (
        SELECT coalesce(sum(quantity * unit_price), 0) FROM part_item
        WHERE part_item.service_order_id = service_order.id)
WHERE public_id IN (SELECT public_id FROM demo_service_order);

INSERT INTO stock_movement (part_id, service_order_id, type, quantity, created_by_user_id, date_created,
                            last_updated, version)
SELECT part_item.part_id, part_item.service_order_id, 'OUT', part_item.quantity, approval.changed_by_user_id,
       part_item.approved_at, part_item.approved_at, 0
FROM part_item
JOIN service_order ON service_order.id = part_item.service_order_id
JOIN status_change AS approval
    ON approval.service_order_id = service_order.id AND approval.to_status = 'QUOTE_APPROVED'
WHERE service_order.public_id IN (SELECT public_id FROM demo_service_order)
  AND part_item.approved_at IS NOT NULL;

UPDATE part
SET stock_quantity = part.stock_quantity - withdrawn.quantity
FROM (
    SELECT stock_movement.part_id, sum(stock_movement.quantity) AS quantity
    FROM stock_movement
    JOIN service_order ON service_order.id = stock_movement.service_order_id
    WHERE service_order.public_id IN (SELECT public_id FROM demo_service_order)
    GROUP BY stock_movement.part_id
) AS withdrawn
WHERE part.id = withdrawn.part_id;

INSERT INTO stock_reservation (part_id, service_order_id, quantity, expires_at, date_created, last_updated, version)
SELECT part_item.part_id, part_item.service_order_id, part_item.quantity,
       quote_sent.date_created + INTERVAL '7 days', quote_sent.date_created, quote_sent.date_created, 0
FROM part_item
JOIN service_order ON service_order.id = part_item.service_order_id
JOIN status_change AS quote_sent
    ON quote_sent.service_order_id = service_order.id AND quote_sent.to_status = 'AWAITING_APPROVAL'
WHERE service_order.public_id IN (SELECT public_id FROM demo_service_order)
  AND service_order.status = 'AWAITING_APPROVAL';

UPDATE part
SET reserved_quantity = part.reserved_quantity + reserved.quantity
FROM (
    SELECT stock_reservation.part_id, sum(stock_reservation.quantity) AS quantity
    FROM stock_reservation
    JOIN service_order ON service_order.id = stock_reservation.service_order_id
    WHERE service_order.public_id IN (SELECT public_id FROM demo_service_order)
    GROUP BY stock_reservation.part_id
) AS reserved
WHERE part.id = reserved.part_id;

INSERT INTO stock_adjustment_request (part_id, quantity, reason, status, requested_by_user_id, date_created,
                                      last_updated, version)
SELECT part.id, 1, 'Pote danificado na prateleira', 'PENDING', requester.id,
       now() - INTERVAL '6 hours', now() - INTERVAL '6 hours', 0
FROM part
JOIN "user" AS requester ON requester.email = 'atendente@workshop.local'
WHERE part.sku = 'GRAXA-MP';
