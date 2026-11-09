import { ref } from 'vue'

const toastMessage = ref('')
let timer: ReturnType<typeof setTimeout> | undefined

export function useToast() {
  function show(message: string) {
    toastMessage.value = message
    if (timer) {
      clearTimeout(timer)
    }
    timer = setTimeout(() => {
      toastMessage.value = ''
    }, 2200)
  }

  return { toast: toastMessage, show }
}