import { defineStore } from 'pinia';
import { login as loginRequest, logout } from '@/services/authService';

const TOKEN_KEY = 'agentcache-token';
const USERNAME_KEY = 'agentcache-username';
const ROLE_KEY = 'agentcache-role';
const USER_ID_KEY = 'agentcache-user-id';
const MUST_CHANGE_PASSWORD_KEY = 'agentcache-must-change-password';

interface AuthState {
  token: string | null;
  username: string | null;
  role: string | null;
  userId: number | null;
  mustChangePassword: boolean;
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: null,
    username: null,
    role: null,
    userId: null,
    mustChangePassword: false,
  }),
  getters: {
    isAuthenticated: (state): boolean => Boolean(state.token),
    isAdmin: (state): boolean => state.role === 'ADMIN',
  },
  actions: {
    setSession(payload: {
      accessToken: string;
      username: string;
      role: string;
      userId: number;
      mustChangePassword: boolean;
    }): void {
      this.token = payload.accessToken;
      this.username = payload.username;
      this.role = payload.role;
      this.userId = payload.userId;
      this.mustChangePassword = payload.mustChangePassword;
      localStorage.setItem(TOKEN_KEY, payload.accessToken);
      localStorage.setItem(USERNAME_KEY, payload.username);
      localStorage.setItem(ROLE_KEY, payload.role);
      localStorage.setItem(USER_ID_KEY, String(payload.userId));
      localStorage.setItem(MUST_CHANGE_PASSWORD_KEY, String(payload.mustChangePassword));
    },
    setMustChangePassword(value: boolean): void {
      this.mustChangePassword = value;
      localStorage.setItem(MUST_CHANGE_PASSWORD_KEY, String(value));
    },
    clear(): void {
      this.token = null;
      this.username = null;
      this.role = null;
      this.userId = null;
      this.mustChangePassword = false;
      logout();
    },
    initFromStorage(): void {
      this.token = localStorage.getItem(TOKEN_KEY);
      this.username = localStorage.getItem(USERNAME_KEY);
      this.role = localStorage.getItem(ROLE_KEY);
      const userId = localStorage.getItem(USER_ID_KEY);
      this.userId = userId ? Number(userId) : null;
      this.mustChangePassword = localStorage.getItem(MUST_CHANGE_PASSWORD_KEY) === 'true';
    },
    async login(username: string, password: string): Promise<void> {
      const response = await loginRequest({ username, password });
      this.setSession({
        accessToken: response.accessToken,
        username: response.username,
        role: response.role,
        userId: response.userId,
        mustChangePassword: response.mustChangePassword,
      });
    },
  },
});
