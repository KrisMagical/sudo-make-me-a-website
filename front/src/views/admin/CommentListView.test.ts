import { mount, flushPromises } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import CommentListView from './CommentListView.vue'
import { commentsApi } from '@/api/comments'
import { notify } from '@/utils/feedback'

vi.mock('@/api/comments', () => ({
  commentsApi: {
    list: vi.fn(),
    stats: vi.fn(),
    bulk: vi.fn(),
    delete: vi.fn(),
    updateStatus: vi.fn(),
    addAdminComment: vi.fn()
  }
}))

vi.mock('@/utils/feedback', () => ({
  notify: vi.fn()
}))

const pageResponse = {
  items: [],
  page: 0,
  size: 20,
  total: 0,
  totalPages: 0
}

describe('CommentListView', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.mocked(commentsApi.list).mockResolvedValue({ ...pageResponse })
    vi.mocked(commentsApi.stats).mockResolvedValue({ pending: 1, approved: 2, rejected: 3, total: 6 })
    vi.mocked(commentsApi.bulk).mockResolvedValue({ action: 'APPROVE', affected: 1 })
    vi.mocked(notify).mockClear()
  })

  it('loads comments with selected status filter', async () => {
    const wrapper = mount(CommentListView, {
      global: {
        stubs: {
          RouterLink: true,
          CommentNode: true
        }
      }
    })
    await flushPromises()

    await wrapper.findAll('button').find(button => button.text().startsWith('approved'))!.trigger('click')
    await flushPromises()

    expect(commentsApi.list).toHaveBeenLastCalledWith({
      status: 'APPROVED',
      keyword: undefined,
      page: 0,
      size: 20,
      sort: 'createdAt desc'
    })
  })

  it('shows api error message when search fails', async () => {
    vi.mocked(commentsApi.list).mockResolvedValueOnce({ ...pageResponse })
    vi.mocked(commentsApi.list).mockRejectedValueOnce({ response: { data: { message: 'Search failed' } } })

    const wrapper = mount(CommentListView, {
      global: {
        stubs: {
          RouterLink: true,
          CommentNode: true
        }
      }
    })
    await flushPromises()

    await wrapper.get('input[type="text"]').setValue('spam')
    await wrapper.get('input[type="text"]').trigger('keyup.enter')
    await flushPromises()

    expect(notify).toHaveBeenCalledWith('Search failed', 'error')
  })

  it('does not send bulk request without selected comments', async () => {
    const wrapper = mount(CommentListView, {
      global: {
        stubs: {
          RouterLink: true,
          CommentNode: true
        }
      }
    })
    await flushPromises()

    await wrapper.findAll('button').find(button => button.text() === 'approve selected')!.trigger('click')

    expect(commentsApi.bulk).not.toHaveBeenCalled()
    expect(notify).toHaveBeenCalledWith('No comments selected', 'info')
  })
})
