import type { CreateInvitationRequest, InvitationResponse } from '@/types';
import { del, get, post } from './httpClient';

export function createInvitation(request?: CreateInvitationRequest): Promise<InvitationResponse> {
  return post<InvitationResponse>('/api/auth/invitations', request ?? {});
}

export function listInvitations(page: number, size: number): Promise<{ content: InvitationResponse[]; total: number }> {
  return get<{ content: InvitationResponse[]; total: number }>('/api/auth/invitations', { page, size });
}

export function revokeInvitation(id: number): Promise<void> {
  return del<void>(`/api/auth/invitations/${id}`);
}
