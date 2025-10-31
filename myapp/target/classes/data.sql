-- ============================
-- SEED: CATEGORIAS
-- ============================
INSERT INTO categoria (nome) VALUES
  ('Base'),
  ('Mistura'),
  ('Acompanhamento'),
  ('Salada');

-- ============================
-- SEED: ITENS FIXOS
-- ============================
INSERT INTO item_cardapio (nome, descricao, categoria_id, sempre_disponivel, substitui, ativo, imagem_url) VALUES
  ('Arroz', 'Arroz branco',
    (SELECT id FROM categoria WHERE nome='Base'),
    TRUE, 'NENHUM', TRUE, 'img/arroz.jpg'),

  ('Feijão', 'Feijão preto',
    (SELECT id FROM categoria WHERE nome='Base'),
    TRUE, 'NENHUM', TRUE, 'img/feijao.jpg'),

  ('Ovo frito', 'Ovo frito',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    TRUE, 'NENHUM', TRUE, 'img/ovo.jpg'),

  ('Bife frito', 'Bife bovino',
    (SELECT id FROM categoria WHERE nome='Mistura'),
    TRUE, 'NENHUM', TRUE, 'img/bife.jpg'),

  ('Filé de tilápia frita', 'Tilápia empanada',
    (SELECT id FROM categoria WHERE nome='Mistura'),
    TRUE, 'NENHUM', TRUE, 'img/tilapia.jpg'),

  ('Salada', 'Salada do dia',
    (SELECT id FROM categoria WHERE nome='Salada'),
    TRUE, 'NENHUM', TRUE, 'img/salada.jpg');

-- ============================
-- SEED: ITENS VARIÁVEIS E SUBSTITUTOS
-- ============================
INSERT INTO item_cardapio (nome, descricao, categoria_id, sempre_disponivel, substitui, ativo, imagem_url) VALUES
  ('Carne de panela', 'Carne cozida',
    (SELECT id FROM categoria WHERE nome='Mistura'),
    FALSE, 'NENHUM', TRUE, 'img/carnepanela.jpg'),

  ('Farofa', 'Farofa temperada',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    FALSE, 'NENHUM', TRUE, 'img/farofa.jpg'),

  ('Porco frito', 'Suíno frito',
    (SELECT id FROM categoria WHERE nome='Mistura'),
    FALSE, 'NENHUM', TRUE, 'img/porco.jpg'),

  ('Batata cozida com carne moída', 'Batata+carne',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    FALSE, 'NENHUM', TRUE, 'img/batata_carne.jpg'),

  ('Frango frito', 'Frango à milanesa',
    (SELECT id FROM categoria WHERE nome='Mistura'),
    FALSE, 'NENHUM', TRUE, 'img/frango.jpg'),

  ('Macarrão alho e óleo', 'Macarrão',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    FALSE, 'NENHUM', TRUE, 'img/macarrao.jpg'),

  ('Maionese temperada', 'Maionese',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    FALSE, 'NENHUM', TRUE, 'img/maionese.jpg'),

  ('Feijoada', 'Feijoada',
    (SELECT id FROM categoria WHERE nome='Base'),
    FALSE, 'FEIJAO', TRUE, 'img/feijoada.jpg'),

  ('Arroz carreteiro', 'Carreteiro',
    (SELECT id FROM categoria WHERE nome='Base'),
    FALSE, 'ARROZ', TRUE, 'img/carreteiro.jpg'),

  ('Purê de batata com carne moída', 'Purê com carne',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    FALSE, 'NENHUM', TRUE, 'img/pure.jpg'),

  ('Lasanha', 'Lasanha caseira',
    (SELECT id FROM categoria WHERE nome='Acompanhamento'),
    FALSE, 'NENHUM', TRUE, 'img/lasanha.jpg');

-- ============================
-- SEED: DISPONIBILIDADE POR DIA
-- (usa IDs buscados por nome do item)
-- ============================

-- Segunda: carne de panela + farofa
INSERT INTO disponibilidade (item_id, dia) VALUES
  ((SELECT id FROM item_cardapio WHERE nome='Carne de panela'), 'SEGUNDA'),
  ((SELECT id FROM item_cardapio WHERE nome='Farofa'),         'SEGUNDA');

-- Terça: porco frito + batata c/ carne
INSERT INTO disponibilidade (item_id, dia) VALUES
  ((SELECT id FROM item_cardapio WHERE nome='Porco frito'),                      'TERCA'),
  ((SELECT id FROM item_cardapio WHERE nome='Batata cozida com carne moída'),    'TERCA');

-- Quarta: frango frito + macarrão + maionese
INSERT INTO disponibilidade (item_id, dia) VALUES
  ((SELECT id FROM item_cardapio WHERE nome='Frango frito'),      'QUARTA'),
  ((SELECT id FROM item_cardapio WHERE nome='Macarrão alho e óleo'),'QUARTA'),
  ((SELECT id FROM item_cardapio WHERE nome='Maionese temperada'), 'QUARTA');

-- Quinta: porco + farofa + feijoada (substitui feijão)
INSERT INTO disponibilidade (item_id, dia) VALUES
  ((SELECT id FROM item_cardapio WHERE nome='Porco frito'), 'QUINTA'),
  ((SELECT id FROM item_cardapio WHERE nome='Farofa'),      'QUINTA'),
  ((SELECT id FROM item_cardapio WHERE nome='Feijoada'),    'QUINTA');

-- Sexta: carreteiro (substitui arroz) + purê c/ carne moída
INSERT INTO disponibilidade (item_id, dia) VALUES
  ((SELECT id FROM item_cardapio WHERE nome='Arroz carreteiro'),                 'SEXTA'),
  ((SELECT id FROM item_cardapio WHERE nome='Purê de batata com carne moída'),   'SEXTA');

-- Sábado: porco + lasanha
INSERT INTO disponibilidade (item_id, dia) VALUES
  ((SELECT id FROM item_cardapio WHERE nome='Porco frito'), 'SABADO'),
  ((SELECT id FROM item_cardapio WHERE nome='Lasanha'),     'SABADO');

-- ============================
-- SEED: CLIENTE DEMO (sem setar id)
-- ============================
INSERT INTO cliente (nome, telefone, email, endereco, latitude, longitude)
VALUES ('João Silva','44999999999','joao@email','Rua A, 123', -23.31, -51.16);

-- Domingo (placeholder): reutiliza frango e farofa
INSERT INTO disponibilidade (item_id, dia) VALUES (11,'DOMINGO'),(8,'DOMINGO');

