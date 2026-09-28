import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import CommentForm from './CommentForm.vue'
import { publicApi } from '@/api/public'

vi.mock('@/api/public', () => ({
  publicApi: {
    addComment: vi.fn()
  }
}))

describe('CommentForm', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows review message when backend returns pending status', async () => {
    vi.mocked(publicApi.addComment).mockResolvedValue({ status: 'PENDING' } as any)
    const wrapper = mount(CommentForm, { props: { postId: 1 } })

    await wrapper.find('input[type="text"]').setValue('Reader')
    await wrapper.find('input[type="email"]').setValue('reader@example.com')
    await wrapper.find('textarea').setValue('Looks good')
    await wrapper.find('form').trigger('submit.prevent')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Comment submitted. It will appear after review.')
  })

  it('shows backend validation message and field errors', async () => {
    vi.mocked(publicApi.addComment).mockRejectedValue({
      response: {
        data: {
          message: 'Validation failed',
          errors: { email: 'email must be valid' }
        }
      }
    })
    const wrapper = mount(CommentForm, { props: { postId: 1 } })

    await wrapper.find('input[type="text"]').setValue('Reader')
    await wrapper.find('input[type="email"]').setValue('reader@example.com')
    await wrapper.find('textarea').setValue('Looks good')
    await wrapper.find('form').trigger('submit.prevent')
    await wrapper.vm.$nextTick()

    expect(wrapper.text()).toContain('Validation failed')
    expect(wrapper.text()).toContain('email: email must be valid')
  })
})
