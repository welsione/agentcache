import type {
  ApiKeyInfo,
  CreateApiKeyRequest,
  CreateApiKeyResponse,
  CreateSpaceRequest,
  SpaceResponse,
  UpdateSpaceRequest,
} from '@/types';
import { get, post, put } from './httpClient';

export function listSpaces(): Promise<SpaceResponse[]> {
  return get<SpaceResponse[]>('/api/spaces');
}

export function getSpace(id: number): Promise<SpaceResponse> {
  return get<SpaceResponse>(`/api/spaces/${id}`);
}

export function createSpace(request: CreateSpaceRequest): Promise<SpaceResponse> {
  return post<SpaceResponse>('/api/spaces', request);
}

export function updateSpace(id: number, request: UpdateSpaceRequest): Promise<SpaceResponse> {
  return put<SpaceResponse>(`/api/spaces/${id}`, request);
}

export function listApiKeys(spaceId: number): Promise<ApiKeyInfo[]> {
  return get<ApiKeyInfo[]>(`/api/spaces/${spaceId}/api-keys`);
}

export function createApiKey(spaceId: number, request: CreateApiKeyRequest): Promise<CreateApiKeyResponse> {
  return post<CreateApiKeyResponse>(`/api/spaces/${spaceId}/api-keys`, request);
}
