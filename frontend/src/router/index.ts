import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { guest: true }
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { guest: true }
    },
    {
      path: '/onboarding',
      name: 'onboarding',
      component: () => import('@/views/OnboardingView.vue')
    },
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/HomeView.vue')
    },
    {
      path: '/foods',
      name: 'foods',
      component: () => import('@/views/FoodsView.vue')
    },
    {
      path: '/favorites',
      name: 'favorites',
      component: () => import('@/views/FavoritesView.vue')
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue')
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/'
    }
  ],
  scrollBehavior: () => ({ top: 0 })
})

router.beforeEach(async (to) => {
  const store = useUserStore()

  if (to.meta.guest) {
    if (!store.isLoggedIn) {
      return true
    }
    return store.needsOnboarding ? '/onboarding' : '/'
  }

  if (!store.isLoggedIn) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }

  if (!store.user) {
    try {
      await store.fetchProfile()
    } catch {
      store.clearAuth()
      return '/login'
    }
  }

  if (store.needsOnboarding && to.name !== 'onboarding') {
    return '/onboarding'
  }
  if (to.name === 'onboarding' && store.user?.onboardingDone === 1) {
    return '/'
  }
  return true
})

export default router