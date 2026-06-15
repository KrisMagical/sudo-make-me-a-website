import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useMaintenanceStore } from '@/stores/maintenanceStore'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: () => import('@/views/public/HomeView.vue')
    },
    {
      path: '/post/:slug',
      name: 'post',
      component: () => import('@/views/public/PostView.vue')
    },
    {
      path: '/maintenance',
      name: 'maintenance',
      component: () => import('@/views/public/MaintenanceView.vue')
    },
    {
      path: '/category/:slug',
      name: 'category',
      component: () => import('@/views/public/CategoryView.vue')
    },
    {
      path: '/collection/:slug',
      name: 'collection',
      component: () => import('@/views/public/CollectionView.vue')
    },
    {
      path: '/admin/login',
      name: 'admin-login',
      component: () => import('@/views/admin/LoginPage.vue'),
      meta: { requiresGuest: true }
    },
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/admin/posts'
        },
        {
          path: 'posts',
          name: 'admin-posts',
          component: () => import('@/views/admin/PostListView.vue')
        },
        {
          path: 'posts/new',
          name: 'admin-post-new',
          component: () => import('@/views/admin/PostEditView.vue')
        },
        {
          path: 'posts/edit/:slug',
          name: 'admin-post-edit',
          component: () => import('@/views/admin/PostEditView.vue')
        },
        {
          path: 'collections',
          name: 'admin-collections',
          component: () => import('@/views/admin/CollectionListView.vue')
        },
        {
          path: 'collections/new',
          name: 'admin-collection-new',
          component: () => import('@/views/admin/CollectionEditView.vue')
        },
        {
          path: 'collections/edit/:slug',
          name: 'admin-collection-edit',
          component: () => import('@/views/admin/CollectionEditView.vue')
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/views/admin/CategoryListView.vue')
        },
        {
          path: 'socials',
          name: 'admin-socials',
          component: () => import('@/views/admin/SocialListView.vue')
        },
        {
          path: 'home',
          name: 'admin-home',
          component: () => import('@/views/admin/HomeEditView.vue')
        },
        {
          path: 'comments',
          name: 'admin-comments',
          component: () => import('@/views/admin/CommentListView.vue')
        },
        {
          path: 'sidebar',
          name: 'admin-sidebar',
          component: () => import('@/views/admin/SidebarEditView.vue')
        },
        {
          path: 'maintenance',
          name: 'admin-maintenance',
          component: () => import('@/views/admin/MaintenanceEditView.vue')
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('@/views/public/NotFoundView.vue')
    }
  ]
})

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth) {
    if (!authStore.isLoggedIn) {
      next('/admin/login')
      return
    }
    const valid = await authStore.verifySession()
    if (!valid) {
      next('/admin/login')
      return
    }
  }

  if (to.meta.requiresGuest && authStore.isLoggedIn) {
    next('/admin/posts')
    return
  }

  const maintenanceStore = useMaintenanceStore()
  if (!maintenanceStore.config) {
    await maintenanceStore.fetchStatus()
  }
  const maintenance = maintenanceStore.config

  if (maintenance && !maintenance.enabled && to.path === '/maintenance') {
    next('/')
    return
  }

  if (maintenance && maintenance.enabled && !to.path.startsWith('/admin') && to.path !== '/maintenance') {
    next('/maintenance')
    return
  }
  next()
})

export default router
