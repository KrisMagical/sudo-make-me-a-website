<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { publicApi } from '@/api/public';
import SmartContent from '@/components/public/SmartContent.vue';
import CommentForm from '@/components/public/CommentForm.vue';

const route = useRoute();
const post = ref<any>(null);
const comments = ref<any[]>([]);

const loadData = async () => {
  const slug = route.params.slug as string;
  post.value = await publicApi.getPost(slug);
  comments.value = await publicApi.getComments(post.value.id);
};

const handleLike = async (positive: boolean) => {
  try {
    const res = await publicApi.likePost(post.value.id, positive);
    // 更新成功：刷新点赞/点踩数
    post.value.likeCount = res.likes;
    post.value.dislikeCount = res.dislikes;
  } catch (error: any) {
    // 从后端返回的错误结构中提取 message 字段
    const message = error.response?.data?.message || 'An error occurred. Please try again.';
    alert(message); // 提示用户（可使用更友好的 toast 组件替换）
  }
};

onMounted(loadData);
</script>

<template>
  <article v-if="post" class="max-w-2xl mx-auto py-12 px-4">
    <header class="mb-8 border-b border-zinc-100 pb-8">
      <h1 class="text-3xl font-bold tracking-tighter mb-2">{{ post.title }}</h1>
      <div class="flex gap-4 text-xs text-zinc-400 font-mono">
        <span>{{ new Date(post.createdAt).toLocaleDateString() }}</span>
        <span class="uppercase">/ {{ post.categoryName }}</span>
        <span>VIEWS: {{ post.viewCount }}</span>
      </div>
    </header>

    <SmartContent :content="post.content" class="mb-12 text-lg" />

    <!-- 点赞/踩区域 - 增加颜色和文字标签 -->
    <div class="flex items-center gap-6 border-y border-zinc-100 py-6 mb-12">
      <button
        @click="handleLike(true)"
        class="flex items-center gap-2 text-green-600 hover:text-green-700 dark:text-green-400 dark:hover:text-green-300 transition-colors"
      >
        <div i-carbon-thumbs-up />
        <span>Like</span> {{ post.likeCount }}
      </button>
      <button
        @click="handleLike(false)"
        class="flex items-center gap-2 text-red-600 hover:text-red-700 dark:text-red-400 dark:hover:text-red-300 transition-colors"
      >
        <div i-carbon-thumbs-down />
        <span>Dislike</span> {{ post.dislikeCount }}
      </button>
    </div>

    <section>
      <h3 class="text-sm font-bold uppercase tracking-widest mb-6">Comments</h3>
      <div class="space-y-8 mb-12">
        <div v-for="c in comments" :key="c.id" class="border-l-2 border-zinc-100 pl-4">
          <div class="text-xs font-bold mb-1">{{ c.name }} <span class="font-normal text-zinc-400">— {{ new Date(c.createdAt).toLocaleDateString() }}</span></div>
          <p class="text-sm text-zinc-600 dark:text-zinc-400">{{ c.content }}</p>
        </div>
      </div>
      <CommentForm :post-id="post.id" @success="loadData" />
    </section>
  </article>
</template>