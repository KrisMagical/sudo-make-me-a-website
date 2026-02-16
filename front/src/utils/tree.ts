// src/utils/tree.ts
import type { PageDto } from '@/types/api'

export interface PageOption extends PageDto {
  level: number;
  children?: PageOption[];
}

export function buildIndentedList(pages: PageDto[], excludeId?: number): PageOption[] {
  const result: PageOption[] = [];

  const walk = (parentId: number | null, level: number) => {
    const children = pages.filter(p => p.parentId === parentId && p.id !== excludeId);

    children.sort((a, b) => a.orderIndex - b.orderIndex);

    children.forEach(p => {
      result.push({ ...p, level });
      walk(p.id, level + 1);
    });
  };

  walk(null, 0);
  return result;
}