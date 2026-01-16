import { useEffect, useMemo, useState } from "react";
import ReactMarkdown from "react-markdown";

import { getAllCategories, getPostsByCategory, getHome } from "@/services/api";
import type { CategoryDto, PostSummaryDto, HomeProfileDto, HomeMediaDto } from "@/types/dtos";
import PostCard from "@/components/PostCard";
import { Empty, Skeleton } from "@/components/ui";

export default function Home() {
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [recent, setRecent] = useState<PostSummaryDto[]>([]);
  const [home, setHome] = useState<HomeProfileDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const cats = await getAllCategories();
        if (!mounted) return;
        const validCats = (cats ?? []).filter((c) => !!c?.name && !!c?.slug);
        setCategories(validCats);

        if (validCats.length) {
          const posts = await getPostsByCategory(validCats[0].slug);
          if (!mounted) return;
          setRecent(posts);
        } else {
          setRecent([]);
        }

        // ✅ 改为 /api/home
        try {
          const h = await getHome();
          if (mounted) setHome(h);
        } catch {
          if (mounted) setHome(null);
        }
      } finally {
        if (mounted) setLoading(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, []);

  const mediaList = useMemo<HomeMediaDto[]>(() => {
    const list = home?.mediaDtoList ?? [];
    return [...list].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  }, [home]);

  return (
    <div>
      {home ? (
        <div className="mb-8 space-y-4">
          <h1 className="text-3xl font-bold">{home.title}</h1>

          {/* cover（可选） */}
          {home.coverVideoUrl ? (
            <video className="w-full rounded-xl" controls src={home.coverVideoUrl} />
          ) : home.coverImageUrl ? (
            <img className="w-full rounded-xl object-cover" src={home.coverImageUrl} alt="cover" />
          ) : null}

          <div className="prose max-w-none">
            <ReactMarkdown>{home.content ?? ""}</ReactMarkdown>
          </div>

          {/* 媒体列表 */}
          {mediaList.length > 0 && (
            <section className="space-y-3">
              <h2 className="text-xl font-semibold">个人风采</h2>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
                {mediaList.map((m) => (
                  <div key={m.id ?? m.url} className="rounded-xl border p-3 space-y-2">
                    {m.type === "VIDEO" ? (
                      <video className="w-full rounded-lg" controls src={m.url} />
                    ) : (
                      <img className="w-full rounded-lg object-cover" src={m.url} alt={m.caption ?? "image"} />
                    )}
                    {m.caption ? <div className="text-sm text-gray-600">{m.caption}</div> : null}
                  </div>
                ))}
              </div>
            </section>
          )}
        </div>
      ) : (
        <div className="mb-8">
          <h1 className="text-3xl font-bold mb-2">欢迎 👋</h1>
          <p className="text-gray-600">这是一个最小可用界面（MVP），对接 Blog Service API。</p>
        </div>
      )}

      <section className="mb-10">
        <h2 className="text-xl font-semibold mb-3">分类</h2>
        {loading ? (
          <Skeleton lines={2} />
        ) : categories.length ? (
          <div className="flex flex-wrap gap-2">
            {categories.map((c) => (
              <span key={c.id ?? c.slug} className="px-3 py-1 rounded-full bg-gray-100 text-gray-700 text-sm">
                {c.name}
              </span>
            ))}
          </div>
        ) : (
          <Empty text="暂无分类" />
        )}
      </section>

      <section>
        <h2 className="text-xl font-semibold mb-3">近期文章</h2>
        {loading ? (
          <Skeleton lines={4} />
        ) : recent.length ? (
          <div className="space-y-3">
            {recent.slice(0, 5).map((p) => (
              <PostCard key={p.slug} post={p} />
            ))}
          </div>
        ) : (
          <Empty text="暂无文章" />
        )}
      </section>
    </div>
  );
}
