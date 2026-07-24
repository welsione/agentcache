export interface ApiResult<T> {
  code: number;
  message: string;
  data: T;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  mustChangePassword: boolean;
  userId: number;
  username: string;
  role: UserRole;
}

export interface ChangePasswordRequest {
  oldPassword: string;
  newPassword: string;
}

export interface AcceptInvitationRequest {
  token: string;
  username: string;
  email: string;
  password: string;
}

export type UserRole = 'ADMIN' | 'USER';
export type UserStatus = 'ACTIVE' | 'DELETED';
export type StorageType = 'LOCAL' | 'COS';
export type FileAccessAction = 'VIEW' | 'DOWNLOAD' | 'UPLOAD' | 'DELETE' | 'VISIBILITY_CHANGE';
export type ActorType = 'USER' | 'API_KEY' | 'ANONYMOUS';

export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: UserRole;
  status: UserStatus;
  mustChangePassword: boolean;
  createdAt: string;
}

export interface UpdateRoleRequest {
  role: UserRole;
}

export interface UpdateStatusRequest {
  status: UserStatus;
}

export interface ResetPasswordRequest {
  newPassword: string;
}

export interface SpaceMemberResponse {
  id: number;
  spaceId: number;
  userId: number;
  role: SpaceMemberRole;
  createdAt: string;
}

export interface SpaceMemberRequest {
  userId: number;
  role: SpaceMemberRole;
}

export interface UpdateMemberRoleRequest {
  role: SpaceMemberRole;
}

export interface InvitationResponse {
  id: number;
  token: string;
  inviteUrl: string;
  createdBy: number;
  usedAt: string | null;
  expiresAt: string;
  createdAt: string;
}

export interface CreateInvitationRequest {
  expiresInHours?: number;
}

export interface SpaceResponse {
  id: number;
  name: string;
  description: string | null;
  ownerId: number;
  storageType: StorageType;
  defaultVisibility: FileVisibility;
  createdAt: string;
}

export interface CreateSpaceRequest {
  name: string;
  description: string;
  storageType?: StorageType;
  defaultVisibility?: FileVisibility;
}

export interface UpdateSpaceRequest {
  name?: string;
  description?: string;
  storageType?: StorageType;
  defaultVisibility?: FileVisibility;
}

export type FileVisibility = 'PRIVATE' | 'PUBLIC';

export interface FileResponse {
  id: number;
  originalName: string;
  contentType: string | null;
  size: number;
  visibility: FileVisibility;
  description: string | null;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: number | null;
  accessUrl: string;
}

export interface CreateFileRequest {
  file: File;
}

export interface PageResponse<T> {
  content: T[];
  total: number;
  page: number;
  size: number;
}

export type SpaceMemberRole = 'MANAGER' | 'MEMBER' | 'READER';

export interface ApiKeyInfo {
  id: number;
  name: string;
  role: SpaceMemberRole;
  createdAt: string;
}

export interface CreateApiKeyRequest {
  name: string;
  role: SpaceMemberRole;
}

export interface CreateApiKeyResponse extends ApiKeyInfo {
  apiKey: string;
}

export interface VisibilityUpdateRequest {
  visibility: FileVisibility;
}

export interface UpdateFileDescriptionRequest {
  description: string;
}

export interface UpdateFileExpiryRequest {
  expiresInHours: number | null;
}

export interface FileAccessLogResponse {
  id: number;
  fileId: number;
  spaceId: number;
  action: FileAccessAction;
  actorType: ActorType;
  actorId: number | null;
  actorName: string | null;
  ip: string | null;
  details: string | null;
  createdAt: string;
}
