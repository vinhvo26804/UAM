"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Space } from "@/types/issue";
import { spaceService } from "@/services/space.service";
import { extractErrorMessage } from "@/lib/api-client";
import { SprintIcon } from "@/components/jira/JiraIcons";

export default function SpacesPage() {
  const [spaces, setSpaces] = useState<Space[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Modal create
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [key, setKey] = useState("");
  const [name, setName] = useState("");
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    async function loadSpaces() {
      try {
        const data = await spaceService.getMySpaces();
        if (isMounted) {
          setSpaces(data.content);
        }
      } catch (err) {
        if (isMounted) {
          setError(extractErrorMessage(err));
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    }

    loadSpaces();

    return () => {
      isMounted = false;
    };
  }, []);

  const handleCreateSpace = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!key.trim() || !name.trim()) return;

    setCreating(true);
    setCreateError(null);
    try {
      const newSpace = await spaceService.createSpace({
        key: key.trim().toUpperCase(),
        name: name.trim(),
      });
      setShowCreateModal(false);
      setKey("");
      setName("");
      setSpaces((prev) => [newSpace, ...prev]);
    } catch (err) {
      setCreateError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-5 animate-jira-fade">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between pb-4 border-b border-[#091E4224] gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-bold text-[#172B4D] tracking-tight">
            Không gian làm việc (Spaces)
          </h1>
          <p className="text-xs sm:text-sm text-[#44546F] mt-1">
            Quản lý các dự án, công việc (Epic, Story, Task, Bug) và chu kỳ phát triển Sprint của bạn.
          </p>
        </div>
        <button
          onClick={() => {
            setShowCreateModal(true);
            setCreateError(null);
          }}
          className="inline-flex items-center gap-1.5 px-4 py-2 bg-[#0C66E4] hover:bg-[#0055CC] text-white text-xs font-semibold rounded shadow-xs transition-colors self-start sm:self-auto cursor-pointer jira-interactive-btn"
        >
          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 4v16m8-8H4" />
          </svg>
          Tạo Không gian mới
        </button>
      </div>

      {error && (
        <div className="p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
          {error}
        </div>
      )}

      {loading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {[1, 2, 3].map((i) => (
            <div key={i} className="bg-white border border-[#091E4224] rounded-lg p-5 animate-pulse space-y-3">
              <div className="flex justify-between items-center">
                <div className="w-12 h-6 bg-[#091E420F] rounded" />
                <div className="w-16 h-5 bg-[#091E420F] rounded" />
              </div>
              <div className="w-3/4 h-5 bg-[#091E420F] rounded" />
              <div className="w-1/2 h-4 bg-[#091E420F] rounded" />
            </div>
          ))}
        </div>
      ) : spaces.length === 0 ? (
        <div className="py-16 text-center bg-white rounded-lg border border-[#091E4224] p-8 shadow-2xs">
          <div className="w-12 h-12 rounded-full bg-[#E9F2FF] text-[#0C66E4] flex items-center justify-center mx-auto mb-3">
            <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <h3 className="text-base font-bold text-[#172B4D]">Bạn chưa tham gia Không gian nào</h3>
          <p className="text-xs text-[#626F86] mt-1 max-w-sm mx-auto">
            Bắt đầu bằng cách tạo Không gian làm việc đầu tiên để lập kế hoạch Sprint và quản lý Issues.
          </p>
          <button
            onClick={() => setShowCreateModal(true)}
            className="mt-4 px-4 py-2 bg-[#0C66E4] hover:bg-[#0055CC] text-white text-xs font-semibold rounded cursor-pointer transition-colors shadow-xs jira-interactive-btn inline-flex items-center gap-1.5"
          >
            + Tạo Không gian đầu tiên
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {spaces.map((space) => (
            <div
              key={space.id}
              className="bg-white border border-[#091E4224] rounded-lg p-5 hover:shadow-md hover:border-[#85B8FF] transition-all flex flex-col justify-between group"
            >
              <div>
                <div className="flex items-start justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <div className="w-7 h-7 rounded bg-[#0C66E4] text-white flex items-center justify-center font-bold text-xs shadow-xs group-hover:scale-105 transition-transform">
                      {space.key.slice(0, 2).toUpperCase()}
                    </div>
                    <span className="px-2 py-0.5 bg-[#E9F2FF] text-[#0C66E4] font-bold text-xs rounded tracking-wider">
                      {space.key}
                    </span>
                  </div>
                  <span className="px-2 py-0.5 bg-[#F1F2F4] text-[#44546F] text-[11px] rounded font-semibold">
                    {space.currentUserRole}
                  </span>
                </div>
                <h3 className="text-base font-bold text-[#172B4D] mt-3 line-clamp-1 group-hover:text-[#0C66E4] transition-colors">
                  {space.name}
                </h3>
                <p className="text-xs text-[#626F86] mt-1 font-mono">
                  Tạo ngày: {new Date(space.createdAt).toLocaleDateString("vi-VN")}
                </p>
              </div>

              <div className="mt-5 pt-3 border-t border-[#091E4214] flex items-center justify-between gap-2">
                <Link
                  href={`/spaces/${space.id}/issues`}
                  className="text-xs font-semibold text-[#0C66E4] hover:underline"
                >
                  Danh sách công việc &rarr;
                </Link>
                <Link
                  href={`/spaces/${space.id}/sprints`}
                  className="inline-flex items-center gap-1 text-xs font-medium text-[#44546F] hover:text-[#0C66E4] transition-colors"
                >
                  <SprintIcon className="w-3.5 h-3.5" />
                  <span>Sprints</span>
                </Link>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Modal Tạo Không Gian */}
      {showCreateModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-xs p-4 animate-jira-fade">
          <div className="bg-white rounded-xl shadow-2xl max-w-md w-full p-6 border border-[#091E4224] animate-jira-modal">
            <div className="flex items-center justify-between pb-3 border-b border-[#091E4224]">
              <h2 className="text-base font-bold text-[#172B4D]">Tạo Không gian làm việc mới</h2>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="text-[#626F86] hover:text-[#172B4D] text-sm p-1 rounded hover:bg-[#091E420F] transition-colors cursor-pointer"
              >
                ✕
              </button>
            </div>
            <p className="text-xs text-[#626F86] mt-2">
              Mã Không gian sẽ là tiền tố đại diện cho tất cả mã Issue (Ví dụ: PROJ-1, PROJ-2).
            </p>

            {createError && (
              <div className="mt-3 p-3 bg-[#FFEBE6] border border-[#FF8F73] text-[#DE350B] rounded-md text-xs animate-jira-slide-down">
                {createError}
              </div>
            )}

            <form onSubmit={handleCreateSpace} className="mt-4 space-y-4">
              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Mã Không gian (Key) <span className="text-[#DE350B]">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="PROJ, UAM, APP..."
                  value={key}
                  onChange={(e) => setKey(e.target.value.toUpperCase())}
                  maxLength={10}
                  className="w-full text-sm font-mono uppercase font-bold border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-[#44546F] uppercase mb-1">
                  Tên Không gian <span className="text-[#DE350B]">*</span>
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ví dụ: Dự án Cổng dịch vụ trực tuyến"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full text-sm border border-[#091E4224] rounded-md px-3 py-2 focus:outline-hidden focus:border-[#0C66E4]"
                />
              </div>

              <div className="flex justify-end gap-2.5 pt-3 border-t border-[#091E4224]">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="px-4 py-2 text-xs text-[#44546F] hover:bg-[#091E420F] rounded-md font-medium cursor-pointer"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-5 py-2 text-xs bg-[#0C66E4] hover:bg-[#0055CC] text-white rounded-md font-semibold disabled:opacity-50 cursor-pointer shadow-xs jira-interactive-btn"
                >
                  {creating ? "Đang tạo..." : "Tạo Không gian"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
