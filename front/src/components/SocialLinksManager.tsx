// src/components/SocialLinksManager.tsx
import { useEffect, useMemo, useState } from "react";
import type { SocialDto } from "@/types/dtos";
import { createSocial, deleteSocial, listSocials, updateSocial } from "@/services/api";

type Mode = "create" | "edit";

const emptyDraft: SocialDto = {
  name: "",
  url: "",
  description: "",
};

export default function SocialLinksManager() {
  const [socials, setSocials] = useState<SocialDto[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [mode, setMode] = useState<Mode>("create");
  const [editingId, setEditingId] = useState<number | null>(null);

  const [draft, setDraft] = useState<SocialDto>({ ...emptyDraft });
  const [iconFile, setIconFile] = useState<File | null>(null);
  const [externalIconUrl, setExternalIconUrl] = useState("");

  const canSubmit = useMemo(() => {
    return draft.name.trim().length > 0 && draft.url.trim().length > 0;
  }, [draft.name, draft.url]);

  async function refresh() {
    setLoading(true);
    try {
      const data = await listSocials();
      setSocials(data);
    } catch (e) {
      console.error(e);
      alert("加载 Social 列表失败：请检查后端是否启动/CORS/权限");
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => {
    refresh();
  }, []);

  function resetForm() {
    setMode("create");
    setEditingId(null);
    setDraft({ ...emptyDraft });
    setIconFile(null);
    setExternalIconUrl("");
  }

  function startEdit(s: SocialDto) {
    setMode("edit");
    setEditingId(s.id ?? null);
    setDraft({
      name: s.name ?? "",
      url: s.url ?? "",
      description: s.description ?? "",
    });
    setIconFile(null);
    setExternalIconUrl("");
  }

  async function onSubmit() {
    if (!canSubmit) return;

    // icon 二选一：建议你只填一个；这里不强制，交给后端决定
    setSaving(true);
    try {
      if (mode === "create") {
        await createSocial({
          data: {
            name: draft.name.trim(),
            url: draft.url.trim(),
            description: draft.description?.trim() ?? "",
          },
          iconFile,
          externalIconUrl: externalIconUrl.trim() || null,
        });
        alert("Social 已创建！");
      } else {
        if (!editingId) {
          alert("编辑状态缺少 id");
          return;
        }
        await updateSocial(editingId, {
          data: {
            id: editingId,
            name: draft.name.trim(),
            url: draft.url.trim(),
            description: draft.description?.trim() ?? "",
          },
          iconFile,
          externalIconUrl: externalIconUrl.trim() || null,
        });
        alert("Social 已更新！");
      }

      await refresh();
      resetForm();
    } catch (e) {
      console.error(e);
      alert("保存失败：请检查后端是否需要登录/参数是否符合要求");
    } finally {
      setSaving(false);
    }
  }

  async function onDelete(id?: number) {
    if (!id) return;
    if (!confirm("确定删除该 Social 吗？该操作不可恢复。")) return;

    try {
      await deleteSocial(id);
      await refresh();
      if (editingId === id) resetForm();
      alert("已删除！");
    } catch (e) {
      console.error(e);
      alert("删除失败：请检查权限/后端状态");
    }
  }

  return (
    <div className="p-4 rounded-xl border space-y-4">
      <div className="flex items-center justify-between">
        <h2 className="text-lg font-semibold">社交链接配置</h2>
        <button className="px-3 py-1 rounded-xl border" onClick={refresh} disabled={loading}>
          {loading ? "刷新中..." : "刷新"}
        </button>
      </div>

      {/* 表单 */}
      <div className="rounded-xl border p-4 space-y-3">
        <div className="font-medium">
          {mode === "create" ? "新增 Social" : `编辑 Social #${editingId ?? ""}`}
        </div>

        <div className="grid gap-3">
          <label className="grid gap-1">
            <span className="text-sm text-gray-600">Name *</span>
            <input
              className="border px-3 py-2 rounded-xl"
              value={draft.name}
              onChange={(e) => setDraft((d) => ({ ...d, name: e.target.value }))}
              placeholder="GitHub / Twitter / Bilibili ..."
            />
          </label>

          <label className="grid gap-1">
            <span className="text-sm text-gray-600">URL *</span>
            <input
              className="border px-3 py-2 rounded-xl"
              value={draft.url}
              onChange={(e) => setDraft((d) => ({ ...d, url: e.target.value }))}
              placeholder="https://..."
            />
          </label>

          <label className="grid gap-1">
            <span className="text-sm text-gray-600">Description</span>
            <input
              className="border px-3 py-2 rounded-xl"
              value={draft.description ?? ""}
              onChange={(e) => setDraft((d) => ({ ...d, description: e.target.value }))}
              placeholder="可选"
            />
          </label>

          <div className="grid gap-2">
            <div className="text-sm text-gray-600">Icon（二选一）</div>

            <label className="grid gap-1">
              <span className="text-xs text-gray-500">上传 iconFile</span>
              <input
                type="file"
                accept="image/*"
                onChange={(e) => setIconFile(e.target.files?.[0] ?? null)}
              />
            </label>

            <label className="grid gap-1">
              <span className="text-xs text-gray-500">或 externalIconUrl</span>
              <input
                className="border px-3 py-2 rounded-xl"
                value={externalIconUrl}
                onChange={(e) => setExternalIconUrl(e.target.value)}
                placeholder="https://.../icon.png"
              />
            </label>

            <div className="text-xs text-gray-500">
              提示：如果同时提供 file 和 externalIconUrl，哪个生效取决于后端实现；建议只填一个。
            </div>
          </div>

          <div className="flex gap-2">
            <button
              onClick={onSubmit}
              disabled={!canSubmit || saving}
              className="px-4 py-2 rounded-xl bg-black text-white disabled:opacity-60"
            >
              {saving ? "提交中..." : mode === "create" ? "创建" : "更新"}
            </button>
            <button onClick={resetForm} className="px-4 py-2 rounded-xl border">
              重置
            </button>
          </div>
        </div>
      </div>

      {/* 列表 */}
      <div className="space-y-2">
        {socials.length === 0 && !loading ? (
          <div className="text-sm text-gray-600">暂无 Social 数据</div>
        ) : null}

        {socials.map((s) => (
          <div
            key={s.id ?? s.url}
            className="flex items-center justify-between gap-3 border rounded-xl px-3 py-2"
          >
            <div className="min-w-0">
              <div className="flex items-center gap-2">
                {s.iconUrl ? (
                  <img src={s.iconUrl} alt={s.name} className="w-5 h-5 object-contain" />
                ) : (
                  <span className="w-5 h-5 inline-flex items-center justify-center rounded bg-gray-200 text-xs">
                    @
                  </span>
                )}
                <div className="font-medium truncate">{s.name}</div>
              </div>
              <div className="text-sm text-gray-600 truncate">{s.url}</div>
              {s.description ? <div className="text-xs text-gray-500 truncate">{s.description}</div> : null}
            </div>

            <div className="flex gap-2 shrink-0">
              <button className="px-3 py-1 rounded-xl border" onClick={() => startEdit(s)}>
                编辑
              </button>
              <button className="px-3 py-1 rounded-xl border border-red-500 text-red-600" onClick={() => onDelete(s.id)}>
                删除
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
