<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue';
import { postsApi } from '@/api/posts';

const posts = ref<any[]>([]);
const loading = ref(false);
const searchKeyword = ref('');

// 请求令牌，用于避免旧请求覆盖新结果
let fetchToken = 0;
let searchTimer: number | null = null;

const fetchData = async () => {
  // 清除之前的防抖定时器
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }

  const keyword = searchKeyword.value.trim();

  // 未输入搜索关键词时，不加载任何数据（避免 Post 过多导致界面混乱）
  if (!keyword) {
    posts.value = [];
    loading.value = false;
    return;
  }

  // 生成新的请求令牌
  const token = ++fetchToken;
  loading.value = true;

  try {
    // 使用搜索接口（已存在于 posts.ts）
    const list = await postsApi.search(keyword, 100);
    // 如果请求令牌不匹配，说明有新请求发起，忽略本次结果
    if (token !== fetchToken) return;
    // 仍然过滤掉草稿
    posts.value = list.filter((p: any) => p.slug !== '00100000');
  } catch (e) {
    if (token !== fetchToken) return;
    console.error('Failed to search posts', e);
    posts.value = [];
  } finally {
    if (token === fetchToken) {
      loading.value = false;
    }
  }
};

// 防抖搜索：用户停止输入 300ms 后自动搜索
watch(searchKeyword, (newVal) => {
  // 清除之前的定时器
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }

  const keyword = newVal.trim();
  if (!keyword) {
    // 关键词为空时立即清空结果
    posts.value = [];
    loading.value = false;
    return;
  }

  // 设置新的防抖定时器
  searchTimer = setTimeout(() => {
    fetchData();
    searchTimer = null;
  }, 300);
});

const clearSearch = () => {
  // 清除防抖定时器
  if (searchTimer) {
    clearTimeout(searchTimer);
    searchTimer = null;
  }
  searchKeyword.value = '';
  posts.value = [];
  loading.value = false;
};

const handleDelete = async (slug: string) => {
  if (confirm(`Confirm deletion of ${slug}?`)) {
    await postsApi.delete(slug);
    // 删除后自动刷新当前搜索结果（直接调用，清除防抖）
    fetchData();
  }
};

// 组件卸载时清除定时器
onUnmounted(() => {
  if (searchTimer) {
    clearTimeout(searchTimer);
  }
});
</script>

<template>
  <div class="space-y-6">
    <!-- 标题栏 -->
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">
        POST_MANAGEMENT
      </h2>
      <router-link to="/admin/posts/new" class="btn-primary text-sm">+ NEW_POST</router-link>
    </div>

    <!-- 搜索栏（与 PageListView 风格一致） -->
    <div class="flex gap-2">
      <input
        v-model="searchKeyword"
        type="text"
        placeholder="Search posts by title or slug..."
        class="flex-1 bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
        @keyup.enter="fetchData"
      />
      <button
        v-if="searchKeyword.trim()"
        @click="clearSearch"
        class="px-4 py-2 border border-zinc-300 dark:border-zinc-700 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors text-sm font-bold uppercase tracking-tighter"
      >
        CLEAR
      </button>
    </div>

    <!-- 加载状态 / 内容区域 -->
    <div v-if="loading" class="italic opacity-50">Loading records...</div>

    <div v-else>
      <!-- 未搜索或搜索无结果时的提示 -->
      <div
        v-if="posts.length === 0"
        class="py-12 text-center italic opacity-50 border border-dashed border-zinc-300 dark:border-zinc-700 rounded-2xl"
      >
        {{
          searchKeyword.trim()
            ? 'No posts match your search query.'
            : 'Use the search bar above to find and manage posts.'
        }}
      </div>

      <!-- 有搜索结果时显示表格 -->
      <table v-else class="w-full text-left border-collapse">
        <thead>
          <tr class="border-b border-zinc-200 dark:border-zinc-700 text-xs uppercase tracking-widest text-zinc-400">
            <th class="py-3 font-normal">Title</th>
            <th class="py-3 font-normal w-32">Category</th>
            <th class="py-3 font-normal w-40">Date</th>
            <th class="py-3 font-normal w-24 text-right">Actions</th>
           </tr>
        </thead>
        <tbody class="text-sm">
          <tr v-for="post in posts" :key="post.id" class="border-b border-zinc-100 dark:border-zinc-700 group">
            <td class="py-4">
              <span class="font-bold group-hover:underline cursor-pointer">{{ post.title }}</span>
              <div class="text-xs text-zinc-400 font-mono">{{ post.slug }}</div>
            </td>
            <td class="py-4 font-mono">{{ post.categoryName }}</td>
            <td class="py-4 text-zinc-500">{{ new Date(post.createdAt).toLocaleDateString() }}</td>
            <td class="py-4 text-right space-x-4">
              <router-link :to="`/admin/posts/edit/${post.slug}`" class="hover:text-black dark:hover:text-white">EDIT</router-link>
              <button @click="handleDelete(post.slug)" class="text-red-500 hover:font-bold">DEL</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>