DROP TABLE IF EXISTS bundles;
CREATE TABLE bundles (
    bundle_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    bundle_date DATE NOT NULL UNIQUE
);

DROP TABLE IF EXISTS complexities;
CREATE TABLE complexities (
    complexity_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    complexity_name VARCHAR(50) NOT NULL UNIQUE,
    weight INT NOT NULL,
    privileged BOOLEAN NOT NULL DEFAULT false
);

DROP TABLE IF EXISTS words;
CREATE TABLE words (
    word_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    word VARCHAR(100) NOT NULL UNIQUE,
    translation VARCHAR(100) NOT NULL,
    synonyms VARCHAR(100) NOT NULL,
    bundle_id INT NOT NULL,
    times_picked INT DEFAULT 0,
    last_picked_timestamp BIGINT DEFAULT 1355097600000,
    complexity_id INT NOT NULL,
    definition TEXT,
    FOREIGN KEY (complexity_id) REFERENCES complexities(complexity_id) ON DELETE CASCADE,
    FOREIGN KEY (bundle_id) REFERENCES bundles(bundle_id) ON DELETE CASCADE
);

DROP TABLE IF EXISTS future_words;
CREATE TABLE future_words (
    future_word_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    future_word VARCHAR(255) NOT NULL UNIQUE,
    priority INT NOT NULL DEFAULT 0,
    date_added DATE,
    date_changed DATE
);

DROP TABLE IF EXISTS repeat_words;
CREATE TABLE repeat_words (
    repeat_word_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    word_id INT NOT NULL,
    date_added DATE NOT NULL,
    FOREIGN KEY (word_id) REFERENCES words(word_id) ON DELETE CASCADE,
    UNIQUE KEY unique_repeat_entry (word_id, date_added)
);

DROP TABLE IF EXISTS daily_iterations;
CREATE TABLE daily_iterations (
    daily_id INT NOT NULL PRIMARY KEY AUTO_INCREMENT,
    local_date DATE NOT NULL UNIQUE,
    iterations INT DEFAULT 0
);