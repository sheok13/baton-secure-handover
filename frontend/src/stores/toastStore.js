import { create } from 'zustand'

export const useToastStore = create((set) => ({
  message: '',
  show: (message) => set({ message }),
  hide: () => set({ message: '' }),
}))