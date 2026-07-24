import type {
  AcceptInvitationRequest,
  ChangePasswordRequest,
  LoginRequest,
  LoginResponse,
} from '@/types';
import { post } from './httpClient';

export function login(request: LoginRequest): Promise<LoginResponse> {
  return post<LoginResponse>('/api/auth/login', request);
}

export function changePassword(request: ChangePasswordRequest): Promise<void> {
  return post<void>('/api/auth/change-password', request);
}

export function acceptInvitation(request: AcceptInvitationRequest): Promise<void> {
  return post<void>('/api/auth/invite-accept', request);
}

export function logout(): void {
  localStorage.removeItem('agentcache-token');
  localStorage.removeItem('agentcache-username');
  localStorage.removeItem('agentcache-role');
  localStorage.removeItem('agentcache-user-id');
  localStorage.removeItem('agentcache-must-change-password');
}
