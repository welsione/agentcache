import axios, {
  AxiosError,
  type AxiosInstance,
  type AxiosRequestConfig,
  type AxiosResponse,
  type InternalAxiosRequestConfig,
} from 'axios';
import type { ApiResult } from '@/types';

const TOKEN_KEY = 'agentcache-token';
const USERNAME_KEY = 'agentcache-username';

function isApiResult(value: unknown): value is ApiResult<unknown> {
  if (typeof value !== 'object' || value === null) {
    return false;
  }
  const candidate = value as Partial<ApiResult<unknown>>;
  return typeof candidate.code === 'number' && typeof candidate.message === 'string' && 'data' in candidate;
}

function handleUnauthorized(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USERNAME_KEY);
  localStorage.removeItem('agentcache-role');
  localStorage.removeItem('agentcache-user-id');
  localStorage.removeItem('agentcache-must-change-password');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

const httpClient: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000,
});

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

httpClient.interceptors.response.use(
  (response: AxiosResponse<unknown>) => {
    const body = response.data;
    if (isApiResult(body) && body.code !== 200) {
      if (body.code === 401) {
        handleUnauthorized();
      }
      return Promise.reject(new Error(body.message));
    }
    return response;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    if (error.response?.status === 401) {
      handleUnauthorized();
    }
    const responseBody: unknown = error.response?.data;
    if (typeof responseBody === 'object' && responseBody !== null && 'message' in responseBody) {
      const message = (responseBody as { message?: unknown }).message;
      if (typeof message === 'string') {
        return Promise.reject(new Error(message));
      }
    }
    return Promise.reject(new Error(error.message));
  },
);

export default httpClient;

export async function get<T>(url: string, params?: Record<string, unknown>): Promise<T> {
  const response = await httpClient.get<ApiResult<T>>(url, { params });
  return response.data.data;
}

export async function post<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.post<ApiResult<T>>(url, body, config);
  return response.data.data;
}

export async function put<T>(url: string, body?: unknown, config?: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.put<ApiResult<T>>(url, body, config);
  return response.data.data;
}

export async function del<T = void>(url: string, config?: AxiosRequestConfig): Promise<T> {
  const response = await httpClient.delete<ApiResult<T>>(url, config);
  return response.data.data;
}
