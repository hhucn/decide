CREATE TABLE IF NOT EXISTS users
(
  id       int PRIMARY KEY,
  nickname text UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS positions
(
  id   int PRIMARY KEY,
  text text NOT NULL,
  cost int NOT NULL
);

CREATE TABLE IF NOT EXISTS preferences
(
  user_id         int REFERENCES users (id) ON DELETE CASCADE,
  position_id     int REFERENCES positions (id) ON DELETE CASCADE,
  preferred_level int NOT NULL,
  CONSTRAINT preferences_pkey PRIMARY KEY (user_id, position_id),
  UNIQUE (user_id, preferred_level)
)