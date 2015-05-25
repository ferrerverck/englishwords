SELECT word, bundle_name, complexity_name FROM words
JOIN bundles ON words.bundle_id = bundles.bundle_id
JOIN complexities ON words.complexity_id = complexities.complexity_id
LIMIT 10000;
