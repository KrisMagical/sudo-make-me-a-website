// src/pages/Dashboard.tsx
import React, { useState, useEffect, Suspense } from "react";
import {
  // Posts
  createPost,
  createPostFromMd,
  updatePost,
  updatePostFromMd,
  deletePostBySlug,
  getAllCategories,
  getPostDetail,
  uploadImage,
  uploadVideo,
  // Pages
  getPageBySlug,
  createPage,
  updatePageBySlug,
  createPageFromMd,
  updatePageFromMdBySlug,
  deletePageBySlug,
  type PageDto,
} from "@/services/api";
import type { CategoryDto, PostDetailDto } from "@/types/dtos";
import CategoryManager from "@/components/CategoryManager";
import SocialLinksManager from "@/components/SocialLinksManager";
import HomeManager from "@/components/HomeManager"; // ✅ 新增：Home 管理模块

const MDEditor = React.lazy(() => import("@uiw/react-md-editor"));

type PageField = "title" | "slug" | "content";
type Module = "home" | "page" | "post" | "categories" | "social"; // ✅ 新增 home

export default function Dashboard() {
  const [activeModule, setActiveModule] = useState<Module>("post");

  const [title, setTitle] = useState("");
  const [slug, setSlug] = useState("");
  const [categorySlug, setCategorySlug] = useState("blog");
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [content, setContent] = useState<string>("");
  const [submitting, setSubmitting] = useState(false);
  const [fileMode, setFileMode] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isUpdate, setIsUpdate] = useState(false);
  const [updateId, setUpdateId] = useState<number | null>(null);

  // 保留：用于复用你原有逻辑，但现在由 activeModule 控制
  const [isPageMode, setIsPageMode] = useState(false);

  // 按 slug 加载旧内容
  const [loadSlug, setLoadSlug] = useState("");

  // 保存“加载时的原始页面数据”，用于“只更新某一项”时补齐其它字段
  const [loadedPage, setLoadedPage] = useState<PageDto | null>(null);

  const slugify = (s: string) =>
    s
      .toLowerCase()
      .replace(/[^a-z0-9\s-]/g, "")
      .trim()
      .replace(/\s+/g, "-")
      .replace(/-+/g, "-");

  useEffect(() => {
    getAllCategories().then(setCategories).catch(() => {});
  }, []);

  const cancelUpdate = () => {
    setTitle("");
    setSlug("");
    setContent("");
    setCategorySlug("blog");
    setSelectedFile(null);
    setIsUpdate(false);
    setUpdateId(null);
    setLoadSlug("");
    setFileMode(false);
    setLoadedPage(null);
  };

  const switchModule = (m: Module) => {
    setActiveModule(m);
    cancelUpdate();

    if (m === "page") setIsPageMode(true);
    if (m === "post") setIsPageMode(false);
    if (m === "home") setIsPageMode(false); // ✅ home 不使用 page/post 编辑器
  };

  const handleUploadImage = async (file: File) => {
    try {
      const url = await uploadImage(file);
      setContent((c) => c + `\n\n![图片描述](${url})\n\n`);
    } catch (e) {
      alert("图片上传失败");
    }
  };

  const handleUploadVideo = async (file: File) => {
    try {
      const url = await uploadVideo(file);
      setContent((c) => c + `\n\n@[video](${url})\n\n`);
    } catch (e) {
      alert("视频上传失败");
    }
  };

  const handleLoadForUpdate = async () => {
    const isPage = activeModule === "page";
    const isPost = activeModule === "post";

    if (!isPage && !isPost) return;
    if (!loadSlug) return alert(`请先输入要加载的${isPage ? "页面" : "文章"} Slug`);

    try {
      if (isPage) {
        const data = await getPageBySlug(loadSlug);
        setTitle(data.title || "");
        setSlug(data.slug || "");
        setContent(data.content || "");
        setIsUpdate(true);
        setFileMode(false);
        setSelectedFile(null);
        setUpdateId(null);
        setLoadedPage(data);
        setIsPageMode(true);
      } else {
        const data = await getPostDetail(loadSlug);
        setTitle(data.title || "");
        setSlug(data.slug || "");
        setContent(data.content || "");
        setUpdateId(data.id ?? null);
        setIsUpdate(true);
        setFileMode(false);
        setSelectedFile(null);
        setIsPageMode(false);

        if (data.categoryName) {
          const matched = categories.find((c) => c.name === data.categoryName);
          if (matched) setCategorySlug(matched.slug);
        }
      }
    } catch (e) {
      alert(`未找到该 Slug 对应的${isPage ? "页面" : "文章"}`);
    }
  };

  const handleDelete = async () => {
    const isPage = activeModule === "page";
    const isPost = activeModule === "post";
    if (!isPage && !isPost) return;

    const delSlug = slug || loadSlug;
    if (!delSlug) return alert("当前没有可删除的 Slug");
    if (!confirm(`确定要删除${isPage ? "页面" : "文章"}「${delSlug}」吗？该操作不可恢复！`)) return;

    try {
      if (isPage) {
        const removed = await deletePageBySlug(delSlug);
        alert(`页面已删除：${removed}`);
      } else {
        const removed = await deletePostBySlug(delSlug);
        alert(`文章已删除：${removed}`);
      }
      cancelUpdate();
    } catch (e) {
      alert("删除失败");
    }
  };

  // 页面：按按钮更新指定字段
  const handleUpdatePageByFields = async (fields: PageField[]) => {
    const keySlug = loadedPage?.slug || loadSlug || slug;
    if (!keySlug) return alert("缺少用于定位的页面 Slug（请先加载页面）");

    if (fields.includes("title") && !title) return alert("标题不能为空");
    if (fields.includes("slug") && !slug) return alert("Slug 不能为空");
    if (fields.includes("content") && !content) return alert("内容不能为空");

    const payload = {
      title: fields.includes("title") ? title : loadedPage?.title || "",
      slug: fields.includes("slug") ? slug : loadedPage?.slug || keySlug,
      content: fields.includes("content") ? content : loadedPage?.content || "",
    };

    setSubmitting(true);
    try {
      const result = await updatePageBySlug(keySlug, payload);
      alert("页面已更新！");

      setLoadedPage(result);
      setTitle(result.title || "");
      setSlug(result.slug || "");
      setContent(result.content || "");
      setLoadSlug(result.slug || "");
    } catch (e) {
      alert("页面更新失败");
    } finally {
      setSubmitting(false);
    }
  };

  // 页面：Markdown 文件更新
  const handleUpdatePageFromMd = async () => {
    const keySlug = loadedPage?.slug || loadSlug || slug;
    if (!keySlug) return alert("缺少用于定位的页面 Slug（请先加载页面）");
    if (!selectedFile) return alert("请选择 Markdown 文件");

    setSubmitting(true);
    try {
      const result = await updatePageFromMdBySlug(keySlug, selectedFile);
      alert("页面已用 Markdown 文件更新！");

      setLoadedPage(result);
      setTitle(result.title || "");
      setSlug(result.slug || "");
      setContent(result.content || "");
      setLoadSlug(result.slug || keySlug);
      setSelectedFile(null);
      setFileMode(false);
    } catch (e) {
      alert("Markdown 更新失败");
    } finally {
      setSubmitting(false);
    }
  };

  const handleSubmit = async () => {
    const isPage = activeModule === "page";
    const isPost = activeModule === "post";

    if (!isPage && !isPost) return;

    if (!fileMode && (!title || !slug || !content)) {
      return alert(`请填写完整的${isPage ? "页面" : "文章"}信息`);
    }
    if (fileMode) {
      if (!selectedFile) return alert("请选择 Markdown 文件");
      if (!slug && !isUpdate) return alert("请填写 Slug");
    }

    setSubmitting(true);
    try {
      if (isPage) {
        let result: PageDto;
        if (isUpdate) {
          if (fileMode && selectedFile) {
            result = await updatePageFromMdBySlug(slug || loadSlug, selectedFile);
          } else {
            result = await updatePageBySlug(slug || loadSlug, { title, slug, content });
          }
          alert("页面已更新！");
        } else {
          if (fileMode && selectedFile) {
            result = await createPageFromMd(selectedFile, slug, title || undefined);
          } else {
            result = await createPage({ title, slug, content });
          }
          alert("页面已创建！");
        }
        cancelUpdate();
        return;
      }

      // 文章
      let result: PostDetailDto;
      if (isUpdate && updateId) {
        if (fileMode && selectedFile) {
          result = await updatePostFromMd(updateId, selectedFile, categorySlug);
        } else {
          result = await updatePost(updateId, { title, slug, content }, categorySlug);
        }
        alert("文章已更新！");
      } else {
        if (fileMode && selectedFile) {
          result = await createPostFromMd(selectedFile, categorySlug, slug, title || undefined);
        } else {
          result = await createPost({ title, slug, content }, categorySlug);
        }
        alert("文章已创建！");
      }

      cancelUpdate();
    } catch (e) {
      alert("操作失败");
    } finally {
      setSubmitting(false);
    }
  };

  const isEditingArea = activeModule === "page" || activeModule === "post";
  const isUpdatingPage = activeModule === "page" && isUpdate;

  return (
    <div className="p-6 space-y-6">
      <h1 className="text-2xl font-bold">
        控制台 -{" "}
        {activeModule === "home"
          ? "Home 管理"
          : activeModule === "page"
          ? isUpdate
            ? "更新页面"
            : "新建页面"
          : activeModule === "post"
          ? isUpdate
            ? "更新文章"
            : "新建文章"
          : activeModule === "categories"
          ? "分类管理"
          : "社交链接管理"}
      </h1>

      {/* ✅ 五个模块按钮 */}
      <div className="flex flex-wrap gap-3">
        <button
          onClick={() => switchModule("home")}
          className={`px-3 py-2 rounded-xl border ${activeModule === "home" ? "bg-black text-white" : ""}`}
        >
          Home 管理
        </button>

        <button
          onClick={() => switchModule("page")}
          className={`px-3 py-2 rounded-xl border ${activeModule === "page" ? "bg-black text-white" : ""}`}
        >
          页面管理
        </button>

        <button
          onClick={() => switchModule("post")}
          className={`px-3 py-2 rounded-xl border ${activeModule === "post" ? "bg-black text-white" : ""}`}
        >
          文章管理
        </button>

        <button
          onClick={() => switchModule("categories")}
          className={`px-3 py-2 rounded-xl border ${activeModule === "categories" ? "bg-black text-white" : ""}`}
        >
          分类管理
        </button>

        <button
          onClick={() => switchModule("social")}
          className={`px-3 py-2 rounded-xl border ${activeModule === "social" ? "bg-black text-white" : ""}`}
        >
          社交链接管理
        </button>
      </div>

      {/* ✅ Home 管理模块 */}
      {activeModule === "home" && (
        <div className="rounded-2xl border bg-white p-4">
          <HomeManager />
        </div>
      )}

      {/* ✅ 新增：页面互联/页面树功能提示（只在“页面管理”模块显示） */}
      {activeModule === "page" && (
        <div className="rounded-2xl border bg-white p-4">
          <div className="flex items-start gap-3">
            <div className="mt-0.5 text-lg">💡</div>
            <div className="space-y-2">
              <div className="font-semibold">页面互联（类似 Notion）已开启</div>
              <div className="text-sm text-gray-600 leading-relaxed">
                你可以在页面内容里使用 <span className="font-mono bg-gray-100 px-1 rounded">[[slug]]</span> 来创建内部链接。
                保存后，系统会自动建立引用关系：
                <span className="font-mono bg-gray-100 px-1 rounded ml-1">Outlinks</span>（本页引用）和
                <span className="font-mono bg-gray-100 px-1 rounded ml-1">Backlinks</span>（提及此页）。
                打开页面时会在页面顶部展示这些链接，点击即可跳转。
              </div>

              <div className="text-sm text-gray-600">
                示例：
                <div className="mt-2 grid grid-cols-1 md:grid-cols-2 gap-2">
                  <div className="rounded-xl bg-gray-50 p-3 font-mono text-xs whitespace-pre-wrap">
                    {`这是一个链接：[[getting-started]]
也支持普通链接：[介绍](/page/intro)`}
                  </div>
                  <div className="rounded-xl bg-gray-50 p-3 font-mono text-xs whitespace-pre-wrap">
                    {`插入图片：
![说明](https://example.com/a.png)

插入视频（你已有语法）：
@[video](https://example.com/a.mp4)`}
                  </div>
                </div>
              </div>

              <div className="text-sm text-gray-600">
                左侧 Sidebar 也支持页面树（parentId/orderIndex），会自动展开当前页面所在层级。
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ✅ 仅 Page/Post 显示编辑区域 */}
      {isEditingArea && (
        <>
          {/* 按 slug 加载旧内容 */}
          <div className="flex items-center gap-3">
            <input
              className="flex-1 border px-3 py-2 rounded-xl"
              placeholder={`输入现有${activeModule === "page" ? "页面" : "文章"}的 Slug 加载到编辑器以更新`}
              value={loadSlug}
              onChange={(e) => setLoadSlug(e.target.value)}
            />
            <button onClick={handleLoadForUpdate} className="px-3 py-2 rounded-xl border">
              加载到编辑器
            </button>
            {isUpdate && (
              <>
                <button onClick={cancelUpdate} className="px-3 py-2 rounded-xl border">
                  取消更新
                </button>
                <button onClick={handleDelete} className="px-3 py-2 rounded-xl border border-red-500 text-red-600">
                  删除{activeModule === "page" ? "页面" : "文章"}
                </button>
              </>
            )}
          </div>

          {!fileMode && (
            <input
              className="w-full border px-3 py-2 rounded-xl"
              placeholder="标题"
              value={title}
              onChange={(e) => {
                const v = e.target.value;
                setTitle(v);
                if (!slug) setSlug(slugify(v));
              }}
            />
          )}

          {!fileMode && (
            <input
              className="w-full border px-3 py-2 rounded-xl"
              placeholder="Slug（唯一标识）"
              value={slug}
              onChange={(e) => setSlug(e.target.value)}
            />
          )}

          {activeModule === "post" && (
            <div className="flex items-center gap-3">
              <label className="text-sm text-gray-600">分类</label>
              <select
                className="border px-3 py-2 rounded-xl"
                value={categorySlug}
                onChange={(e) => setCategorySlug(e.target.value)}
              >
                {categories.map((c) => (
                  <option key={c.slug} value={c.slug}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>
          )}

          {/* 编辑模式切换 */}
          <div className="flex gap-3">
            <button
              onClick={() => setFileMode(false)}
              className={`px-3 py-1 rounded-xl border ${!fileMode ? "bg-black text-white" : ""}`}
            >
              富文本编辑
            </button>
            <button
              onClick={() => setFileMode(true)}
              className={`px-3 py-1 rounded-xl border ${fileMode ? "bg-black text-white" : ""}`}
            >
              Markdown 文件
            </button>
          </div>

          {/* Markdown 编辑器 或 文件上传 */}
          {!fileMode ? (
            <div data-color-mode="light">
              <Suspense fallback={<div className="text-sm text-gray-600">编辑器加载中...</div>}>
                <MDEditor value={content} onChange={(v: string | undefined) => setContent(v || "")} height={500} />
              </Suspense>

              <div className="flex gap-3 mt-3">
                <label className="cursor-pointer border px-3 py-2 rounded-xl">
                  插入图片
                  <input
                    type="file"
                    accept="image/*"
                    hidden
                    onChange={(e) => e.target.files && handleUploadImage(e.target.files[0])}
                  />
                </label>
                <label className="cursor-pointer border px-3 py-2 rounded-xl">
                  插入视频
                  <input
                    type="file"
                    accept="video/*"
                    hidden
                    onChange={(e) => e.target.files && handleUploadVideo(e.target.files[0])}
                  />
                </label>
              </div>
            </div>
          ) : (
            <div className="flex flex-col gap-3">
              <input
                className="w-full border px-3 py-2 rounded-xl"
                placeholder="标题（可选）"
                value={title}
                onChange={(e) => {
                  const v = e.target.value;
                  setTitle(v);
                  if (!slug) setSlug(slugify(v));
                }}
              />
              <input
                className="w-full border px-3 py-2 rounded-xl"
                placeholder="Slug（必填，唯一标识）"
                value={slug}
                onChange={(e) => setSlug(e.target.value)}
              />

              <input
                type="file"
                accept=".md"
                onChange={(e) => setSelectedFile(e.target.files ? e.target.files[0] : null)}
              />
              {selectedFile && <span className="text-sm text-gray-600">已选择: {selectedFile.name}</span>}
            </div>
          )}

          {/* Page 更新：拆分按钮；Post/创建仍用提交按钮 */}
          {isUpdatingPage ? (
            <div className="flex flex-wrap gap-3">
              <button
                onClick={() => handleUpdatePageByFields(["title"])}
                disabled={submitting}
                className="px-4 py-2 rounded-xl border disabled:opacity-60"
              >
                只更新标题
              </button>

              <button
                onClick={() => handleUpdatePageByFields(["slug"])}
                disabled={submitting}
                className="px-4 py-2 rounded-xl border disabled:opacity-60"
              >
                只更新 Slug
              </button>

              <button
                onClick={() => handleUpdatePageByFields(["content"])}
                disabled={submitting}
                className="px-4 py-2 rounded-xl border disabled:opacity-60"
              >
                只更新内容
              </button>

              <button
                onClick={() => handleUpdatePageByFields(["title", "slug", "content"])}
                disabled={submitting}
                className="px-4 py-2 rounded-xl bg-black text-white disabled:opacity-60"
              >
                {submitting ? "更新中..." : "更新全部"}
              </button>

              {fileMode && (
                <button
                  onClick={handleUpdatePageFromMd}
                  disabled={submitting}
                  className="px-4 py-2 rounded-xl border disabled:opacity-60"
                >
                  {submitting ? "更新中..." : "用 MD 文件更新"}
                </button>
              )}
            </div>
          ) : (
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="px-4 py-2 rounded-xl bg-black text-white disabled:opacity-60"
            >
              {submitting
                ? "提交中..."
                : isUpdate
                ? activeModule === "page"
                  ? "更新页面"
                  : "更新文章"
                : activeModule === "page"
                ? "发布页面"
                : "发布文章"}
            </button>
          )}
        </>
      )}

      {activeModule === "categories" && <CategoryManager />}

      {activeModule === "social" && <SocialLinksManager />}
    </div>
  );
}
