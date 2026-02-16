<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { postsApi } from '@/api/posts';

const posts = ref([]);
const loading = ref(false);

const fetchData = async () => {
  loading.value = true;
  posts.value = await postsApi.listRecent(50);
  loading.value = false;
};

const handleDelete = async (slug: string) => {
  if (confirm(`Confirm deletion of ${slug}?`)) {
    await postsApi.delete(slug);
    fetchData();
  }
};

onMounted(fetchData);
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">ALL_POSTS</h2>
      <router-link to="/admin/posts/new" class="btn-primary text-sm">+ NEW_POST</router-link>
    </div>

    <div v-if="loading" class="italic opacity-50">Loading records...</div>

    <table v-else class="w-full text-left border-collapse">
      <thead>
        <tr class="border-b border-zinc-200 dark:border-zinc-800 text-xs uppercase tracking-widest text-zinc-400">
          <th class="py-3 font-normal">Title</th>
          <th class="py-3 font-normal w-32">Category</th>
          <th class="py-3 font-normal w-40">Date</th>
          <th class="py-3 font-normal w-24 text-right">Actions</th>
        </tr>
      </thead>
      <tbody class="text-sm">
        <tr v-for="post in posts" :key="post.id" class="border-b border-zinc-100 dark:border-zinc-900 group">
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
</template>