import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

// 公共路由
import HomeView from '@/views/public/HomeView.vue'
import PostView from '@/views/public/PostView.vue'
import PageView from '@/views/public/PageView.vue'
import CategoryView from '@/views/public/CategoryView.vue'
import NotFoundView from '@/views/public/NotFoundView.vue'

// 后台路由组件
import AdminLayout from '@/views/admin/AdminLayout.vue'
import LoginPage from '@/views/admin/LoginPage.vue'
import PostListView from '@/views/admin/PostListView.vue'
import PostEditView from '@/views/admin/PostEditView.vue'
import PageListView from '@/views/admin/PageListView.vue'
import PageEditView from '@/views/admin/PageEditView.vue'
import CategoryListView from '@/views/admin/CategoryListView.vue'
import SocialListView from '@/views/admin/SocialListView.vue'
import HomeEditView from '@/views/admin/HomeEditView.vue'
import CommentListView from '@/views/admin/CommentListView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    // 公共路由
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/post/:slug',
      name: 'post',
      component: PostView
    },
    {
      path: '/page/:slug',
      name: 'page',
      component: PageView
    },
    {
      path: '/category/:slug',
      name: 'category',
      component: CategoryView
    },

    // 后台路由
    {
      path: '/admin/login',
      name: 'admin-login',
      component: LoginPage,
      meta: { requiresGuest: true }
    },
    {
      path: '/admin',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          redirect: '/admin/posts'
        },
        {
          path: 'posts',
          name: 'admin-posts',
          component: PostListView
        },
        {
          path: 'posts/new',
          name: 'admin-post-new',
          component: PostEditView
        },
        {
          path: 'posts/edit/:slug',
          name: 'admin-post-edit',
          component: PostEditView
        },
        {
          path: 'pages',
          name: 'admin-pages',
          component: PageListView
        },
        {
          path: 'pages/new',
          name: 'admin-page-new',
          component: PageEditView
        },
        {
          path: 'pages/edit/:slug',
          name: 'admin-page-edit',
          component: PageEditView
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: CategoryListView
        },
        {
          path: 'socials',
          name: 'admin-socials',
          component: SocialListView
        },
        {
          path: 'home',
          name: 'admin-home',
          component: HomeEditView
        },
        {
          path: 'comments',
          name: 'admin-comments',
          component: CommentListView
        },
        {
          path: 'sidebar',
          name: 'admin-sidebar',
          component: () => import('@/views/admin/SidebarEditView.vue')
        }
      ]
    },

    // 404 页面
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  // 检查是否需要认证
  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/admin/login')
    return
  }

  // 检查是否已登录但访问登录页
  if (to.meta.requiresGuest && authStore.isLoggedIn) {
    next('/admin/posts')
    return
  }

  next()
})

export default router