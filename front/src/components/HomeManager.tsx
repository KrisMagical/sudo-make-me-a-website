// src/components/HomeManager.tsx
import React, { useEffect, useMemo, useState, Suspense } from "react";
import {
  getHome,
  updateHome,
  uploadHomeImage,
  uploadHomeVideo,
  updateHomeMedia,
  deleteHomeMedia,
} from "@/services/api";
import type { HomeMediaDto, HomeProfileDto } from "@/types/dtos";
import { Empty, Skeleton } from "@/components/ui";

const MDEditor = React.lazy(() => import("@uiw/react-md-editor"));

export default function HomeManager() {
  const [home, setHome] = useState<HomeProfileDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  async function reload() {
    const h = await getHome();
    setHome(h);
  }

  useEffect(() => {
    (async () => {
      try {
        await reload();
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  const mediaList = useMemo<HomeMediaDto[]>(() => {
    const list = home?.mediaDtoList ?? [];
    return [...list].sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
  }, [home]);

  async function onSave() {
    if (!home) return;
    setSaving(true);
    setMsg(null);
    try {
      const updated = await updateHome({
        title: home.title,
        content: home.content,
        coverImageUrl: home.coverImageUrl,
        coverVideoUrl: home.coverVideoUrl,
      });
      setHome(updated);
      setMsg("保存成功");
    } catch (e: any) {
      setMsg(e?.message ?? "保存失败");
    } finally {
      setSaving(false);
    }
  }

  // ====== 你喜欢的“编辑器插入”逻辑：上传成功后把 url 直接写到 markdown ======

  async function insertImageToContent(file: File) {
    if (!home) return;
    setMsg(null);
    try {
      const created = await uploadHomeImage(file, "content image", (mediaList.length + 1) * 10);
      const url = created?.url;
      if (url) {
        setHome((prev) =>
          prev
            ? {
                ...prev,
                content: `${prev.content ?? ""}\n\n![image](${url})\n\n`,
              }
            : prev
        );
      }
      setMsg("图片已上传并插入内容（记得点保存）");
      await reload(); // 同步媒体列表
    } catch (e: any) {
      setMsg(e?.message ?? "图片插入失败");
    }
  }

  async function insertVideoToContent(file: File) {
    if (!home) return;
    setMsg(null);
    try {
      const created = await uploadHomeVideo(file, "content video", (mediaList.length + 1) * 10);
      const url = created?.url;
      if (url) {
        setHome((prev) =>
          prev
            ? {
                ...prev,
                content: `${prev.content ?? ""}\n\n@[video](${url})\n\n`,
              }
            : prev
        );
      }
      setMsg("视频已上传并插入内容（记得点保存）");
      await reload();
    } catch (e: any) {
      setMsg(e?.message ?? "视频插入失败");
    }
  }

  // ====== 封面：上传成功后自动写入 cover url（仍建议点保存落库） ======

  async function uploadCoverImage(file: File) {
    if (!home) return;
    setMsg(null);
    try {
      const created = await uploadHomeImage(file, "cover image", (mediaList.length + 1) * 10);
      const url = created?.url;
      if (url) setHome((prev) => (prev ? { ...prev, coverImageUrl: url } : prev));
      setMsg("封面图片已上传并填入 URL（记得点保存）");
      await reload();
    } catch (e: any) {
      setMsg(e?.message ?? "封面图片上传失败");
    }
  }

  async function uploadCoverVideo(file: File) {
    if (!home) return;
    setMsg(null);
    try {
      const created = await uploadHomeVideo(file, "cover video", (mediaList.length + 1) * 10);
      const url = created?.url;
      if (url) setHome((prev) => (prev ? { ...prev, coverVideoUrl: url } : prev));
      setMsg("封面视频已上传并填入 URL（记得点保存）");
      await reload();
    } catch (e: any) {
      setMsg(e?.message ?? "封面视频上传失败");
    }
  }

  async function onPatchMedia(m: HomeMediaDto, patch: Partial<HomeMediaDto>) {
    if (!m.id) return;
    await updateHomeMedia(m.id, patch);
    await reload();
  }

  async function onDeleteMedia(m: HomeMediaDto) {
    if (!m.id) return;
    await deleteHomeMedia(m.id);
    await reload();
  }

  if (loading)
    return (
      <div className="p-6">
        <Skeleton lines={6} />
      </div>
    );
  if (!home) return <div className="p-6">Home not found</div>;

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-6">
      <h1 className="text-2xl font-bold">Home 管理</h1>
      {msg ? <div className="text-sm text-gray-700">{msg}</div> : null}

      {/* 基础信息 */}
      <section className="border rounded-xl p-4 space-y-4 bg-white">
        <h2 className="text-lg font-semibold">基础信息</h2>

        <div className="space-y-1">
          <div className="text-sm text-gray-600">标题</div>
          <input
            className="w-full border rounded-lg p-2"
            value={home.title ?? ""}
            onChange={(e) => setHome({ ...home, title: e.target.value })}
          />
        </div>

        {/* 封面图片区 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <div className="text-sm text-gray-600">封面图片 URL</div>
            <input
              className="w-full border rounded-lg p-2"
              value={home.coverImageUrl ?? ""}
              onChange={(e) => setHome({ ...home, coverImageUrl: e.target.value })}
            />

            <div className="flex flex-wrap gap-3">
              <label className="border rounded-lg px-3 py-2 cursor-pointer">
                上传封面图片（自动填 URL）
                <input
                  className="hidden"
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) uploadCoverImage(f);
                    e.currentTarget.value = "";
                  }}
                />
              </label>
            </div>

            {home.coverImageUrl ? (
              <img className="w-full rounded-lg object-cover border" src={home.coverImageUrl} alt="cover" />
            ) : null}
          </div>

          <div className="space-y-2">
            <div className="text-sm text-gray-600">封面视频 URL</div>
            <input
              className="w-full border rounded-lg p-2"
              value={home.coverVideoUrl ?? ""}
              onChange={(e) => setHome({ ...home, coverVideoUrl: e.target.value })}
            />

            <div className="flex flex-wrap gap-3">
              <label className="border rounded-lg px-3 py-2 cursor-pointer">
                上传封面视频（自动填 URL）
                <input
                  className="hidden"
                  type="file"
                  accept="video/mp4,video/webm,video/ogg"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) uploadCoverVideo(f);
                    e.currentTarget.value = "";
                  }}
                />
              </label>
            </div>

            {home.coverVideoUrl ? (
              <video className="w-full rounded-lg border" controls src={home.coverVideoUrl} />
            ) : null}
          </div>
        </div>

        {/* Markdown 编辑器 */}
        <div className="space-y-2">
          <div className="text-sm text-gray-600">内容（Markdown）</div>

          <div data-color-mode="light">
            <Suspense fallback={<div className="text-sm text-gray-600">编辑器加载中...</div>}>
              <MDEditor
                value={home.content ?? ""}
                onChange={(v: string | undefined) => setHome({ ...home, content: v || "" })}
                height={420}
              />
            </Suspense>

            <div className="flex flex-wrap gap-3 mt-3">
              <label className="border rounded-lg px-3 py-2 cursor-pointer">
                插入图片（上传后写入 Markdown）
                <input
                  className="hidden"
                  type="file"
                  accept="image/*"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) insertImageToContent(f);
                    e.currentTarget.value = "";
                  }}
                />
              </label>

              <label className="border rounded-lg px-3 py-2 cursor-pointer">
                插入视频（上传后写入 Markdown）
                <input
                  className="hidden"
                  type="file"
                  accept="video/mp4,video/webm,video/ogg"
                  onChange={(e) => {
                    const f = e.target.files?.[0];
                    if (f) insertVideoToContent(f);
                    e.currentTarget.value = "";
                  }}
                />
              </label>
            </div>
          </div>
        </div>

        <button className="px-4 py-2 rounded-lg border" disabled={saving} onClick={onSave}>
          {saving ? "保存中..." : "保存"}
        </button>
      </section>

      {/* 媒体列表 */}
      <section className="border rounded-xl p-4 space-y-3 bg-white">
        <h2 className="text-lg font-semibold">媒体列表</h2>

        {mediaList.length === 0 ? (
          <Empty text="暂无媒体" />
        ) : (
          <div className="space-y-4">
            {mediaList.map((m) => (
              <div key={m.id ?? m.url} className="border rounded-xl p-3 space-y-2">
                <div className="text-sm text-gray-500">
                  #{m.id} · {m.type} · orderIndex={m.orderIndex ?? 0}
                </div>

                {m.type === "VIDEO" ? (
                  <video className="w-full rounded-lg" controls src={m.url} />
                ) : (
                  <img className="w-full rounded-lg object-cover" src={m.url} alt={m.caption ?? ""} />
                )}

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
                  <input
                    className="border rounded-lg p-2 sm:col-span-2"
                    placeholder="caption"
                    value={m.caption ?? ""}
                    onChange={(e) => onPatchMedia(m, { caption: e.target.value })}
                  />
                  <input
                    className="border rounded-lg p-2"
                    type="number"
                    value={m.orderIndex ?? 0}
                    onChange={(e) => onPatchMedia(m, { orderIndex: Number(e.target.value) })}
                  />
                </div>

                <div className="flex flex-wrap gap-2">
                  <button className="px-3 py-1 rounded-lg border" onClick={() => setHome({ ...home, coverImageUrl: m.url })}>
                    设为封面图片（仅填入，记得保存）
                  </button>
                  <button className="px-3 py-1 rounded-lg border" onClick={() => setHome({ ...home, coverVideoUrl: m.url })}>
                    设为封面视频（仅填入，记得保存）
                  </button>
                  <button className="px-3 py-1 rounded-lg border" onClick={() => onDeleteMedia(m)}>
                    删除
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
}
