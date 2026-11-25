-- task-218: the read-only MCP interface (/api/mcp) is switched on PER TENANT,
-- i.e. per installation: the singleton system_settings row, edited by an
-- administrator on /admin/system. OFF by default, so an existing installation
-- exposes nothing new until someone decides to.
--
-- Also declared on the SystemSettings entity: %test and %dev build the schema
-- from Hibernate with Flyway off (src/backend/CLAUDE.md, migration parity).
ALTER TABLE system_settings ADD COLUMN mcp_enabled BOOLEAN NOT NULL DEFAULT FALSE;
