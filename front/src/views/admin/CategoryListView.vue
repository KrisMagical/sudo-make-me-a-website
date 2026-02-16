<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { categoriesApi } from '@/api/categories'
import type { CategoryDto } from '@/types/api'
import { notify } from '@/utils/feedback'

const categories = ref<CategoryDto[]>([])
const loading = ref(false)

// 新分类表单
const newCategory = ref({ name: '', slug: '' })

// Slug 编辑状态：用户是否手动修改过 slug
const slugManuallyEdited = ref(false)

// Slug 唯一性校验结果
const isSlugUnique = ref(true)

// 生成 slug 的工具函数
const slugify = (text: string): string => {
  return text
    .toLowerCase()
    .trim()
    .replace(/[^\w\s-]/g, '')   // 移除所有非单词字符、非空格、非连字符
    .replace(/\s+/g, '-')       // 空格转连字符
    .replace(/--+/g, '-')       // 多个连字符合并为一个
    .replace(/^-+|-+$/g, '')    // 去掉首尾连字符
}

// 监听分类名称变化，仅在未手动编辑 slug 时自动生成
watch(
  () => newCategory.value.name,
  (newName) => {
    if (!slugManuallyEdited.value && newName.trim()) {
      newCategory.value.slug = slugify(newName)
    }
  }
)

// 监听 slug 输入，判断是否为手动编辑
const onSlugInput = () => {
  if (!slugManuallyEdited.value) {
    slugManuallyEdited.value = true
  }
  validateSlugUnique(newCategory.value.slug)
}

// 校验 slug 是否唯一（基于当前已加载的分类列表）
const validateSlugUnique = (slug: string) => {
  if (!slug.trim()) {
    isSlugUnique.value = true // 空 slug 不进行唯一性校验（将由后端处理）
    return
  }
  const existing = categories.value.find(c => c.slug === slug)
  isSlugUnique.value = !existing
}

// 重置表单（包括手动编辑标记）
const resetForm = () => {
  newCategory.value = { name: '', slug: '' }
  slugManuallyEdited.value = false
  isSlugUnique.value = true
}

// 获取分类列表
const fetchCategories = async () => {
  loading.value = true
  try {
    categories.value = await categoriesApi.list()
  } finally {
    loading.value = false
  }
}

// 创建分类
const handleCreate = async () => {
  if (!newCategory.value.name.trim()) {
    notify('Category name is required', 'error')
    return
  }

  // 如果 slug 为空，根据名称生成一个（兜底）
  if (!newCategory.value.slug.trim()) {
    newCategory.value.slug = slugify(newCategory.value.name)
  }

  // 唯一性最终校验（防止并发或本地未刷新）
  const slugExists = categories.value.some(
    c => c.slug === newCategory.value.slug
  )
  if (slugExists) {
    notify(`Slug "${newCategory.value.slug}" already exists, please use another one`, 'error')
    return
  }

  try {
    await categoriesApi.create(newCategory.value)
    notify('Category created', 'success')
    resetForm()
    fetchCategories()
  } catch (error) {
    notify('Failed to create category', 'error')
  }
}

// 删除分类
const handleDelete = async (category: CategoryDto) => {
  if (!confirm(`Delete category "${category.name}"?`)) return

  try {
    await categoriesApi.delete(category.slug || category.name)
    notify('Category deleted', 'success')
    fetchCategories()
  } catch (error) {
    notify('Failed to delete category', 'error')
  }
}

onMounted(fetchCategories)
</script>

<template>
  <div class="space-y-6">
    <div class="flex justify-between items-end border-b-2 border-zinc-800 pb-2">
      <h2 class="text-2xl font-bold tracking-tighter">CATEGORIES</h2>
    </div>

    <!-- 创建分类表单 -->
    <div class="border border-zinc-200 dark:border-zinc-800 p-4">
      <h3 class="text-sm font-bold uppercase tracking-widest mb-4">New Category</h3>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div>
          <label class="block text-xs uppercase tracking-widest mb-1">Name</label>
          <input
            v-model="newCategory.name"
            type="text"
            class="w-full bg-transparent border border-zinc-300 dark:border-zinc-700 px-3 py-2 outline-none focus:border-zinc-500"
            placeholder="Technology"
            @input="slugManuallyEdited = false"
          />
        </div>
        <div>
          <label class="block text-xs uppercase tracking-widest mb-1">
            Slug
            <span class="text-zinc-400 font-normal ml-1">(optional)</span>
          </label>
          <input
            v-model="newCategory.slug"
            type="text"
            class="w-full bg-transparent border px-3 py-2 outline-none focus:border-zinc-500"
            :class="{
              'border-red-500 dark:border-red-500': !isSlugUnique && newCategory.slug,
              'border-zinc-300 dark:border-zinc-700': isSlugUnique || !newCategory.slug
            }"
            placeholder="tech"
            @input="onSlugInput"
          />
          <!-- Slug 唯一性提示 -->
          <p
            v-if="newCategory.slug && !isSlugUnique"
            class="text-xs text-red-500 mt-1"
          >
            ⚠️ Slug "{{ newCategory.slug }}" already exists
          </p>
        </div>
        <div class="flex items-end">
          <button
            @click="handleCreate"
            :disabled="!newCategory.name || !isSlugUnique"
            class="w-full px-4 py-2 bg-zinc-900 dark:bg-zinc-800 text-white hover:bg-zinc-800 dark:hover:bg-zinc-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed text-sm font-bold uppercase tracking-tighter"
          >
            Add Category
          </button>
        </div>
      </div>
    </div>

    <!-- 分类列表 -->
    <div v-if="loading" class="italic opacity-50">Loading categories...</div>

    <div v-else class="border border-zinc-200 dark:border-zinc-800">
      <table class="w-full text-left">
        <thead>
          <tr class="border-b border-zinc-200 dark:border-zinc-800 text-xs uppercase tracking-widest text-zinc-400">
            <th class="py-3 px-4 font-normal">Name</th>
            <th class="py-3 px-4 font-normal">Slug</th>
            <th class="py-3 px-4 font-normal text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="category in categories"
            :key="category.id"
            class="border-b border-zinc-100 dark:border-zinc-900 hover:bg-zinc-50 dark:hover:bg-zinc-900 transition-colors"
          >
            <td class="py-4 px-4 font-bold">{{ category.name }}</td>
            <td class="py-4 px-4 font-mono text-sm text-zinc-500">{{ category.slug }}</td>
            <td class="py-4 px-4 text-right">
              <button
                @click="handleDelete(category)"
                class="text-red-500 hover:text-red-700 hover:font-bold text-sm"
              >
                DELETE
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>