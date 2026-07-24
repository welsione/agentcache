import type {
  PageResponse,
  ResetPasswordRequest,
  UpdateRoleRequest,
  UpdateStatusRequest,
  UserResponse,
} from '@/types';
import { del, get, put } from './httpClient';

export function getCurrentUser(): Promise<UserResponse> {
  return get<UserResponse>('/api/users/me');
}

export function listUsers(page: number, size: number): Promise<PageResponse<UserResponse>> {
  return get<PageResponse<UserResponse>>('/api/users', { page, size });
}

export function changeUserRole(id: number, request: UpdateRoleRequest): Promise<UserResponse> {
  return put<UserResponse>(`/api/users/${id}/role`, request);
}

export function changeUserStatus(id: number, request: UpdateStatusRequest): Promise<UserResponse> {
  return put<UserResponse>(`/api/users/${id}/status`, request);
}

export function resetUserPassword(id: number, request: ResetPasswordRequest): Promise<void> {
  return put<void>(`/api/users/${id}/password`, request);
}

export function deleteUser(id: number): Promise<void> {
  return del<void>(`/api/users/${id}`);
}
