import type {
  ApiResult,
  FileResponse,
  FileVisibility,
  PageResponse,
  VisibilityUpdateRequest,
  FileAccessLogResponse,
} from '@/types';
import httpClient, { del, get, put } from './httpClient';

export function listFiles(
  spaceId: number,
  query?: string,
  page = 0,
  size = 10,
): Promise<PageResponse<FileResponse>> {
  return get<PageResponse<FileResponse>>(`/api/spaces/${spaceId}/files`, {
    q: query,
    page,
    size,
  });
}

export async function uploadFile(
  spaceId: number,
  file: File,
  options?: {
    description?: string;
    visibility?: FileVisibility;
    expiresInHours?: number;
  },
): Promise<FileResponse> {
  const formData = new FormData();
  formData.append('file', file);
  if (options?.description) formData.append('description', options.description);
  if (options?.visibility) formData.append('visibility', options.visibility);
  if (options?.expiresInHours) formData.append('expiresInHours', String(options.expiresInHours));
  const response = await httpClient.post<ApiResult<FileResponse>>(`/api/spaces/${spaceId}/files`, formData);
  return response.data.data;
}

export function getFile(fileId: number, spaceId: number): Promise<FileResponse> {
  return get<FileResponse>(`/api/files/${fileId}`, { spaceId });
}

export function getPublicFile(fileId: number): Promise<FileResponse> {
  return get<FileResponse>(`/public/files/${fileId}`);
}

export function updateVisibility(
  fileId: number,
  spaceId: number,
  visibility: FileVisibility,
): Promise<FileResponse> {
  const request: VisibilityUpdateRequest = { visibility };
  return put<FileResponse>(`/api/files/${fileId}/visibility`, request, {
    params: { spaceId },
  });
}

export function updateFileDescription(
  fileId: number,
  spaceId: number,
  description: string,
): Promise<FileResponse> {
  return put<FileResponse>(`/api/files/${fileId}/description`, { description }, { params: { spaceId } });
}

export function updateFileExpiry(
  fileId: number,
  spaceId: number,
  expiresInHours: number | null,
): Promise<FileResponse> {
  return put<FileResponse>(`/api/files/${fileId}/expiry`, { expiresInHours }, { params: { spaceId } });
}

export function deleteFile(fileId: number, spaceId: number): Promise<void> {
  return del<void>(`/api/files/${fileId}`, { params: { spaceId } });
}

export async function downloadFile(fileId: number, spaceId: number): Promise<Blob> {
  const response = await httpClient.get<Blob>(privateDownloadUrl(fileId, spaceId), {
    responseType: 'blob',
  });
  return response.data;
}

export function privateDownloadUrl(fileId: number, spaceId: number): string {
  return `/api/files/${fileId}/content?spaceId=${encodeURIComponent(spaceId)}`;
}

export function publicDownloadUrl(fileId: number): string {
  return `/public/files/${fileId}/content`;
}

export function listAccessLogs(
  spaceId: number,
  page = 0,
  size = 10,
): Promise<PageResponse<FileAccessLogResponse>> {
  return get<PageResponse<FileAccessLogResponse>>(`/api/spaces/${spaceId}/access-logs`, { page, size });
}
