import { create } from 'zustand'

export const useAuthStore = create((set) => ({
  member: null,
  accessToken: null,
  ready: false,

  setAuth: (member, accessToken) => set({ member, accessToken }),
  clear: () => set({ member: null, accessToken: null }),
  setReady: () => set({ ready: true })
}))