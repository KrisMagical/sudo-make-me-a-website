[file name]: MediaLibrary.vue
[file content begin]
<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import request from '@/utils/request'
import type { ImageDto } from '@/types/api'

interface Props {
  ownerType: 'POST' | 'PAGE' | 'HOME'
  ownerId?: string | number
  ownerSlug?: string
}

const props = withDefaults(defineProps<Props>(), {
  ownerId: 0,
  ownerSlug: ''
})

const images = ref<ImageDto[]>([])

const fetchImages = async () => {
  try {
    let url = ''
    
    if (props.ownerType === 'POST') {
      url = `/api/posts/${props.ownerId}/images`
    } else if (props.ownerType === 'PAGE') {
      if (props.ownerSlug && props.ownerSlug.length > 0) {
        url = `/api/pages/${props.ownerSlug}/images`
      } else if (props.ownerId && props.ownerId !== 0) {
        console.warn('PAGE media library should use slug instead of id')
        url = `/api/pages/${props.ownerId}/images`
      } else {
        console.error('PAGE media library requires either ownerSlug or ownerId')
        return
      }
    } else {
      url = `/api/home/images`
    }

    console.log('Fetching images from:', url)
    images.value = await request.get(url)
  } catch (error: any) {
    console.error('Failed to fetch images:', error)
    
    if (error.response?.status === 401) {
      console.warn('Authentication error when fetching images')
    }
  }
}

const deleteImage = async (imageId: number) => {
  if (!confirm('Delete this image?')) return
  
  try {
    // 删除图片的 API 使用 ID 而不是 slug
    await request.delete(`/api/images/${props.ownerType}/${props.ownerId}/${imageId}`)
    fetchImages()
  } catch (error: any) {
    console.error('Failed to delete image:', error)
    alert('Failed to delete image. Please try again.')
  }
}

watch(() => props.ownerSlug, (newSlug, oldSlug) => {
  if (newSlug !== oldSlug && props.ownerType === 'PAGE') {
    fetchImages()
  }
})

onMounted(fetchImages)
</script>

<template>
  <div class="mt-8">
    <h3 class="text-sm font-bold border-b border-zinc-800 mb-4 tracking-tighter">ATTACHED_MEDIA</h3>
    
    <div v-if="images.length === 0" class="text-sm text-zinc-400 italic">
      No images attached to this {{ ownerType.toLowerCase() }}.
    </div>
    
    <div v-else class="grid grid-cols-4 md:grid-cols-6 gap-4">
      <div 
        v-for="img in images" 
        :key="img.id" 
        class="group relative aspect-square border border-zinc-200 dark:border-zinc-800 p-1"
      >
        <img 
          :src="img.url" 
          :alt="img.originalFilename" 
          class="w-full h-full object-cover"
          loading="lazy"
        />
        <div 
          class="absolute inset-0 bg-black/60 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity"
        >
          <button 
            @click="deleteImage(img.id)" 
            class="text-white text-xs hover:underline px-2 py-1 bg-red-600/70 hover:bg-red-700/70"
          >
            REMOVE
          </button>
        </div>
        <div class="absolute bottom-0 left-0 right-0 bg-black/70 text-white text-[10px] p-1 truncate opacity-0 group-hover:opacity-100 transition-opacity">
          {{ img.originalFilename }}
        </div>
      </div>
    </div>
  </div>
</template>