-- Allow createdBy to be null when uploads are performed via API Key.
ALTER TABLE fileRecord MODIFY createdBy BIGINT NULL;