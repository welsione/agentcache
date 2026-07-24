import { defineStore } from 'pinia';
import { createSpace as createSpaceRequest, listSpaces } from '@/services/spaceService';
import type { SpaceResponse } from '@/types';

interface SpacesState {
  spaces: SpaceResponse[];
  currentSpaceId: number | null;
}

export const useSpacesStore = defineStore('spaces', {
  state: (): SpacesState => ({
    spaces: [],
    currentSpaceId: null,
  }),
  actions: {
    async fetchSpaces(): Promise<void> {
      this.spaces = await listSpaces();
    },
    setCurrentSpace(id: number): void {
      this.currentSpaceId = id;
    },
    async createSpace(name: string, description: string): Promise<SpaceResponse> {
      const space = await createSpaceRequest({ name, description });
      this.spaces.push(space);
      return space;
    },
  },
});
