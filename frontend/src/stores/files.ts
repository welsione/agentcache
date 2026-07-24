import { defineStore } from 'pinia';
import {
  deleteFile,
  listFiles,
  updateVisibility,
  uploadFile,
} from '@/services/fileService';
import type { FileResponse, FileVisibility } from '@/types';

interface FilesState {
  files: FileResponse[];
  total: number;
  loading: boolean;
  currentRequestId: number;
}

export const useFilesStore = defineStore('files', {
  state: (): FilesState => ({
    files: [],
    total: 0,
    loading: false,
    currentRequestId: 0,
  }),
  actions: {
    async fetchFiles(spaceId: number, query = '', page = 0): Promise<void> {
      const requestId = ++this.currentRequestId;
      this.loading = true;
      try {
        const response = await listFiles(spaceId, query || undefined, page);
        if (requestId === this.currentRequestId) {
          this.files = response.content;
          this.total = response.total;
        }
      } finally {
        if (requestId === this.currentRequestId) {
          this.loading = false;
        }
      }
    },
    async upload(spaceId: number, file: File, options?: {
      description?: string;
      visibility?: FileVisibility;
      expiresInHours?: number;
    }): Promise<FileResponse> {
      const uploaded = await uploadFile(spaceId, file, options);
      this.files.unshift(uploaded);
      this.total += 1;
      return uploaded;
    },
    async remove(spaceId: number, fileId: number): Promise<void> {
      await deleteFile(fileId, spaceId);
      this.files = this.files.filter((file) => file.id !== fileId);
      this.total = Math.max(0, this.total - 1);
    },
    async setVisibility(
      spaceId: number,
      fileId: number,
      visibility: FileVisibility,
    ): Promise<FileResponse> {
      const updated = await updateVisibility(fileId, spaceId, visibility);
      const index = this.files.findIndex((file) => file.id === fileId);
      if (index >= 0) {
        this.files[index] = updated;
      }
      return updated;
    },
  },
});
