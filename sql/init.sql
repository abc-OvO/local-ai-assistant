CREATE TABLE IF NOT EXISTS documents (
  id varchar(64) PRIMARY KEY,
  file_name varchar(255) NOT NULL,
  file_type varchar(50) NOT NULL,
  file_path varchar(500) NOT NULL,
  content_text longtext NOT NULL,
  created_at datetime NOT NULL,
  updated_at datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_chunks (
  id varchar(64) PRIMARY KEY,
  document_id varchar(64) NOT NULL,
  file_name varchar(255) NOT NULL,
  chunk_index int NOT NULL,
  content text NOT NULL,
  embedding_json longtext NOT NULL,
  created_at datetime NOT NULL,
  INDEX idx_document_chunks_document_id (document_id),
  CONSTRAINT fk_document_chunks_document_id
    FOREIGN KEY (document_id) REFERENCES documents(id)
    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_history (
  id bigint AUTO_INCREMENT PRIMARY KEY,
  session_id varchar(128) NOT NULL,
  role varchar(32) NOT NULL,
  content text NOT NULL,
  created_at datetime NOT NULL,
  INDEX idx_chat_history_session_created_at (session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
