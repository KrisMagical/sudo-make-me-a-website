// src/components/Sidebar.tsx
import { useMemo, useState, useEffect } from "react";
import { NavLink, useLocation } from "react-router-dom";
import { getToken } from "@/services/auth";
import { getAllCategories, getAllPages, listSocials } from "@/services/api";
import type { CategoryDto, PageDto, SocialDto } from "@/types/dtos";

import avatarImage from "../Resources/喜多.png";
import magicHatImage from "../Resources/HAt.png";

type PageNode = PageDto & { children: PageNode[] };

const item = (to: string, text: string) => (
  <NavLink
    to={to}
    className={({ isActive }) =>
      `block rounded-xl px-3 py-2 text-sm hover:bg-white hover:shadow ${isActive ? "bg-white shadow" : ""}`
    }
  >
    {text}
  </NavLink>
);

function buildTree(pages: PageDto[]): PageNode[] {
  const byId = new Map<number, PageNode>();
  for (const p of pages) {
    if (!p.id) continue;
    byId.set(p.id, { ...p, children: [] });
  }

  const roots: PageNode[] = [];
  for (const node of byId.values()) {
    const pid = node.parentId ?? null;
    if (pid == null) {
      roots.push(node);
    } else {
      const parent = byId.get(pid);
      if (parent) parent.children.push(node);
      else roots.push(node);
    }
  }

  const sortRec = (nodes: PageNode[]) => {
    nodes.sort((a, b) => (a.orderIndex ?? 0) - (b.orderIndex ?? 0));
    nodes.forEach((n) => sortRec(n.children));
  };
  sortRec(roots);

  return roots;
}

function findPathIds(nodes: PageNode[], slug: string): number[] {
  const path: number[] = [];
  const dfs = (arr: PageNode[]): boolean => {
    for (const n of arr) {
      if (!n.id) continue;
      path.push(n.id);
      if (n.slug === slug) return true;
      if (dfs(n.children)) return true;
      path.pop();
    }
    return false;
  };
  dfs(nodes);
  return path;
}

export default function Sidebar() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [pages, setPages] = useState<PageDto[]>([]);
  const [socials, setSocials] = useState<SocialDto[]>([]);
  const [expanded, setExpanded] = useState<Set<number>>(new Set());

  const location = useLocation();

  const siteTitle = "Kris Magic";
  const siteSubtitle = "Blog & Notes";
  const avatarSrc = avatarImage as string;

  useEffect(() => {
    const token = getToken();
    setIsLoggedIn(!!token);
  }, []);

  useEffect(() => {
    getAllCategories()
      .then(setCategories)
      .catch((e) => console.error("Failed to load categories for sidebar", e));

    getAllPages()
      .then(setPages)
      .catch((e) => console.error("Failed to load pages for sidebar", e));

    listSocials()
      .then(setSocials)
      .catch((e) => console.error("Failed to load socials for sidebar", e));
  }, []);

  const isConsoleRoute = location.pathname.includes("/console");

  const renderSocialIcon = (s: SocialDto) => {
    const src = s.iconUrl && s.iconUrl.trim().length > 0 ? s.iconUrl : (magicHatImage as string);
    return <img src={src} alt={s.name} className="w-6 h-6 object-contain" />;
  };

  const pageTree = useMemo(() => buildTree(pages), [pages]);

  const currentPageSlug = useMemo(() => {
    // 你的页面路由是 /page/:slug
    const parts = location.pathname.split("/").filter(Boolean);
    const idx = parts.indexOf("page");
    if (idx >= 0 && parts[idx + 1]) return decodeURIComponent(parts[idx + 1]);
    return "";
  }, [location.pathname]);

  // 自动展开当前页路径
  useEffect(() => {
    if (!currentPageSlug) return;
    const pathIds = findPathIds(pageTree, currentPageSlug);
    if (pathIds.length === 0) return;
    setExpanded((prev) => {
      const next = new Set(prev);
      for (const id of pathIds) next.add(id);
      return next;
    });
  }, [pageTree, currentPageSlug]);

  const toggle = (id: number) => {
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  };

  const renderPageNode = (node: PageNode, depth: number) => {
    const hasChildren = node.children.length > 0;
    const isOpen = node.id ? expanded.has(node.id) : false;

    return (
      <div key={node.id ?? node.slug}>
        <div className="flex items-center gap-1">
          <div style={{ width: depth * 12 }} />
          {hasChildren ? (
            <button
              type="button"
              onClick={() => node.id && toggle(node.id)}
              className="w-5 h-5 inline-flex items-center justify-center rounded hover:bg-white hover:shadow"
              aria-label={isOpen ? "collapse" : "expand"}
              title={isOpen ? "collapse" : "expand"}
            >
              {isOpen ? "▾" : "▸"}
            </button>
          ) : (
            <div className="w-5 h-5" />
          )}

          <NavLink
            to={`/page/${encodeURIComponent(node.slug)}`}
            className={({ isActive }) =>
              `flex-1 rounded-xl px-3 py-2 text-sm truncate hover:bg-white hover:shadow ${
                isActive ? "bg-white shadow" : ""
              }`
            }
            title={node.title}
          >
            {node.title}
          </NavLink>
        </div>

        {hasChildren && isOpen && (
          <div className="mt-0.5 space-y-0.5">
            {node.children.map((c) => renderPageNode(c, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  return (
    <aside className="hidden md:flex md:flex-col md:w-72 bg-gray-100 min-h-screen p-6 justify-between sticky top-0">
      <div>
        <div className="flex items-center gap-3 mb-8">
          <div className="w-14 h-14 rounded-full bg-gray-200 overflow-hidden">
            <img src={avatarSrc} alt="Avatar" className="w-full h-full object-cover" />
          </div>
          <div>
            <div className="font-semibold text-lg">{siteTitle}</div>
            <div className="text-xs text-gray-500">{siteSubtitle}</div>
          </div>
        </div>

        <nav className="space-y-2">
          {item("/", "Home")}

          {/* Categories */}
          <div className="pt-2">
            <div className="px-2 text-[11px] uppercase text-gray-400 mb-1">Categories</div>
            {(categories ?? [])
              .filter((c) => !!c && !!c.name && !!c.slug)
              .map((c, idx) => (
                <NavLink
                  key={`${c.id ?? "idless"}-${c.slug}-${idx}`}
                  to={`/category/${encodeURIComponent(c.slug)}`}
                  className={({ isActive }) =>
                    `block rounded-xl px-3 py-2 text-sm hover:bg-white hover:shadow ${isActive ? "bg-white shadow" : ""}`
                  }
                >
                  {c.name}
                </NavLink>
              ))}
          </div>

          {/* Pages tree */}
          <div className="pt-2">
            <div className="px-2 text-[11px] uppercase text-gray-400 mb-1">Pages</div>
            <div className="space-y-0.5">
              {pageTree
                .filter((p) => !!p && !!p.slug && !!p.title)
                .map((p) => renderPageNode(p, 0))}
            </div>
          </div>

          {isLoggedIn && isConsoleRoute && (
            <div className="pt-4 border-t border-gray-200 mt-4">
              <div className="text-xs uppercase text-gray-400 mb-1">Console</div>
              {item("/console/login", "Login")}
              {item("/console/dashboard", "Dashboard")}
            </div>
          )}
        </nav>
      </div>

      {/* Social links */}
      <div className="flex items-center gap-3 text-gray-500">
        {(socials ?? [])
          .filter((s) => !!s && !!s.url && !!s.name)
          .map((s) => (
            <a
              key={s.id ?? `${s.name}-${s.url}`}
              href={s.url}
              aria-label={s.name}
              title={s.description ? `${s.name} - ${s.description}` : s.name}
              target="_blank"
              rel="noreferrer"
            >
              {renderSocialIcon(s)}
            </a>
          ))}
      </div>
    </aside>
  );
}
