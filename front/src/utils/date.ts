const pad = (value: number) => String(value).padStart(2, '0')

const toDate = (value: string | Date) => value instanceof Date ? value : new Date(value)

export const formatDate = (value: string | Date) => {
  const date = toDate(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export const formatDateTime = (value: string | Date) => {
  const date = toDate(value)
  if (Number.isNaN(date.getTime())) return ''
  return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}
