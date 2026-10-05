CREATE INDEX idx_message_conversation_id
    ON messages (conversation_id, id);
