-- Test insert with minimal required fields to identify the exact column mismatch
INSERT INTO posts (
    id,
    author_uid,
    post_text,
    post_type,
    timestamp
) VALUES (
    'test-post-123',
    '3f1ea8ce-22b7-4621-99d7-3df341989205', -- Using existing user uid
    'Test post content',
    'TEXT',
    1734343936276
);
