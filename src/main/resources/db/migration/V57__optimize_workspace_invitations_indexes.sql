-- V57: Add composite performance indexes for workspace invitations
-- Optimizes:
-- 1. GET /api/invitations/me: findByReceiverIdAndStatusOrderByCreatedAtDesc
-- 2. GET /api/workspaces/{id}/invites & countByWorkspaceIdAndStatus: findByWorkspaceIdAndStatus, findByWorkspaceIdOrderByCreatedAtDesc
-- 3. createOrGetLinkInvite: findByWorkspaceIdAndTypeAndStatus
-- 4. InvitationCleanupService daily purge: (status, expires_at)
--
-- Rollback note:
-- DROP INDEX IF EXISTS idx_workspace_invitations_receiver_status_created;
-- DROP INDEX IF EXISTS idx_workspace_invitations_workspace_status_created;
-- DROP INDEX IF EXISTS idx_workspace_invitations_workspace_created;
-- DROP INDEX IF EXISTS idx_workspace_invitations_workspace_type_status;
-- DROP INDEX IF EXISTS idx_workspace_invitations_status_expires;

CREATE INDEX IF NOT EXISTS idx_workspace_invitations_receiver_status_created
    ON workspace_invitations (receiver_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_workspace_invitations_workspace_status_created
    ON workspace_invitations (workspace_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_workspace_invitations_workspace_created
    ON workspace_invitations (workspace_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_workspace_invitations_workspace_type_status
    ON workspace_invitations (workspace_id, type, status);

CREATE INDEX IF NOT EXISTS idx_workspace_invitations_status_expires
    ON workspace_invitations (status, expires_at);
