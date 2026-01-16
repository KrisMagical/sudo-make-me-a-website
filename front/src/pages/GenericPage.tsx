import { useEffect, useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";

import { getPageBySlug, getPageBacklinks, getPageOutlinks } from "@/services/api";
import type { PageDto } from "@/types/dtos";

function preprocessWikiLinks(md: string): string {
  // 把 [[slug]] 转成 markdown link：[slug](/page/slug)
  return (md ?? "").replace(/\[\[\s*([a-zA-Z0-9-_./]+)\s*]]/g, (_m, slug) => {
    const s = String(slug).trim();
    return `[${s}](/page/${encodeURIComponent(s)})`;
  });
}

export default function GenericPage() {
  const { slug } = useParams<{ slug: string }>();
  const [page, setPage] = useState<PageDto | null>(null);
  const [backlinks, setBacklinks] = useState<PageDto[]>([]);
  const [outlinks, setOutlinks] = useState<PageDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    if (!slug) return;
    setLoading(true);
    setErr(null);

    Promise.all([
      getPageBySlug(slug),
      getPageBacklinks(slug).catch(() => [] as PageDto[]),
      getPageOutlinks(slug).catch(() => [] as PageDto[]),
    ])
      .then(([p, bl, ol]) => {
        setPage(p);
        setBacklinks(bl);
        setOutlinks(ol);
      })
      .catch(() => setErr("页面不存在或暂不可用"))
      .finally(() => setLoading(false));
  }, [slug]);

  const md = useMemo(() => preprocessWikiLinks(page?.content ?? ""), [page?.content]);

  if (loading) return <div className="p-6 text-sm text-gray-500">加载中...</div>;
  if (err) return <div className="p-6 text-sm text-red-600">{err}</div>;
  if (!page) return <div className="p-6 text-sm text-gray-500">未找到页面</div>;

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-3xl font-bold">{page.title}</h1>
        <div className="text-xs text-gray-500 mt-1">/{page.slug}</div>
      </div>

      {/* Links panels */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mb-6">
        <div className="rounded-xl border bg-white p-4">
          <div className="text-sm font-semibold mb-2">本页引用 (Outlinks)</div>
          {outlinks.length === 0 ? (
            <div className="text-sm text-gray-500">暂无</div>
          ) : (
            <ul className="space-y-1">
              {outlinks.map((p) => (
                <li key={p.id ?? p.slug}>
                  <Link className="text-sm underline" to={`/page/${encodeURIComponent(p.slug)}`}>
                    {p.title}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>

        <div className="rounded-xl border bg-white p-4">
          <div className="text-sm font-semibold mb-2">提及此页 (Backlinks)</div>
          {backlinks.length === 0 ? (
            <div className="text-sm text-gray-500">暂无</div>
          ) : (
            <ul className="space-y-1">
              {backlinks.map((p) => (
                <li key={p.id ?? p.slug}>
                  <Link className="text-sm underline" to={`/page/${encodeURIComponent(p.slug)}`}>
                    {p.title}
                  </Link>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>

      {/* Markdown */}
      <article className="prose max-w-none">
        <ReactMarkdown
          remarkPlugins={[remarkGfm]}
          components={{
            a: ({ href, children }) => {
              const h = href ?? "";
              // 内部 page 链接走 SPA Link
              if (h.startsWith("/page/")) {
                return (
                  <Link className="underline" to={h}>
                    {children}
                  </Link>
                );
              }
              return (
                <a className="underline" href={h} target="_blank" rel="noreferrer">
                  {children}
                </a>
              );
            },
            img: ({ src, alt }) => {
              // 让图片不会超出容器
              return <img src={src ?? ""} alt={alt ?? ""} className="max-w-full rounded-lg" />;
            },
          }}
        >
          {md}
        </ReactMarkdown>
      </article>
    </div>
  );
}
