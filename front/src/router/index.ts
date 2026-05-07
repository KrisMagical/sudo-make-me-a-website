import {createRouter, createWebHistory} from 'vue-router'
import {useAuthStore} from '@/stores/authStore'
import CollectionListView from '@/views/admin/CollectionListView.vue'
import CollectionEditView from '@/views/admin/CollectionEditView.vue'
import { useMaintenanceStore } from '@/stores/maintenanceStore';

// 公共路由
import HomeView from '@/views/public/HomeView.vue'
import PostView from '@/views/public/PostView.vue'
import CategoryView from '@/views/public/CategoryView.vue'
import NotFoundView from '@/views/public/NotFoundView.vue'

// 后台路由组件
import AdminLayout from '@/views/admin/AdminLayout.vue'
import LoginPage from '@/views/admin/LoginPage.vue'
import PostListView from '@/views/admin/PostListView.vue'
import PostEditView from '@/views/admin/PostEditView.vue'
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
            path: '/maintenance',
            name: 'maintenance',
            component: () => import('@/views/public/MaintenanceView.vue')
        },
        {
            path: '/category/:slug',
            name: 'category',
            component: CategoryView
        },
        {
            path: '/collection/:slug',
            name: 'collection',
            component: () => import('@/views/public/CollectionView.vue')
        },

        // 后台路由
        {
            path: '/admin/login',
            name: 'admin-login',
            component: LoginPage,
            meta: {requiresGuest: true}
        },
        {
            path: '/admin',
            component: AdminLayout,
            meta: {requiresAuth: true},
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
                    path: 'collections',
                    name: 'admin-collections',
                    component: CollectionListView
                },
                {
                    path: 'collections/new',
                    name: 'admin-collection-new',
                    component: CollectionEditView
                },
                {
                    path: 'collections/edit/:slug',
                    name: 'admin-collection-edit',
                    component: CollectionEditView
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
                },
                {
                    path: 'maintenance',
                    name: 'admin-maintenance',
                    component: () => import('@/views/admin/MaintenanceEditView.vue')
                }
            ]
        },

        // 404
        {
            path: '/:pathMatch(.*)*',
            name: 'not-found',
            component: NotFoundView
        }
    ]
})

router.beforeEach(async(to, _from, next) => {
    const authStore = useAuthStore()

    if (to.meta.requiresAuth && !authStore.isLoggedIn) {
        next('/admin/login')
        return;
    }

    if (to.meta.requiresGuest && authStore.isLoggedIn) {
        next('/admin/posts')
        return;
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