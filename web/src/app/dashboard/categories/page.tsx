"use client";

import { useState } from "react";
import {
    useCategories,
    firestoreAddCategory,
    firestoreDeleteCategory,
    firestoreUpdateCategory,
    Category
} from "@/hooks/useFirestore";
import { useAuth } from "@/context/AuthContext";
import { Plus, Trash2, Pencil, Minus, Check, X } from "lucide-react";

export default function CategoriesPage() {
    const { categories } = useCategories();
    const { user } = useAuth();

    const [activeTab, setActiveTab] = useState<"EXPENSE" | "INCOME">("EXPENSE");
    const [isAdding, setIsAdding] = useState(false);
    const [editingId, setEditingId] = useState<string | null>(null);
    const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

    // Form inputs
    const [formName, setFormName] = useState("");
    const [formIcon, setFormIcon] = useState("🏷️");

    const filtered = categories.filter((c) => c.type === activeTab);

    const resetForm = () => {
        setFormName("");
        setFormIcon("🏷️");
        setIsAdding(false);
        setEditingId(null);
    };

    const handleAddClick = () => {
        resetForm();
        setIsAdding(true);
    };

    const handleEditClick = (cat: Category) => {
        setFormName(cat.name);
        setFormIcon(cat.icon);
        setEditingId(cat.id);
        setIsAdding(false);
    };

    const handleSave = async () => {
        if (!user || !formName.trim()) return;

        try {
            if (editingId) {
                await firestoreUpdateCategory(user.uid, editingId, {
                    name: formName.trim(),
                    icon: formIcon.trim(),
                });
            } else {
                await firestoreAddCategory(user.uid, {
                    name: formName.trim(),
                    icon: formIcon.trim() || "🏷️",
                    type: activeTab
                });
            }
            resetForm();
        } catch (error) {
            console.error("Error saving category:", error);
        }
    };

    const handleDelete = async (id: string) => {
        if (!user) return;
        try {
            await firestoreDeleteCategory(user.uid, id);
        } catch (error) {
            console.error("Error deleting category:", error);
        } finally {
            setConfirmDeleteId(null);
        }
    };

    if (!user) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="animate-fade-in max-w-4xl relative">
            <div className="flex justify-between items-center mb-6">
                <h1 className="text-2xl font-bold">Danh mục theo dõi</h1>
                {!isAdding && !editingId && (
                    <button
                        onClick={handleAddClick}
                        className="px-4 py-2 bg-[var(--primary)] text-white rounded-xl shadow-lg shadow-[var(--primary)]/20 font-medium flex items-center gap-2 hover:brightness-110 transition-all"
                    >
                        <Plus className="w-4 h-4" /> Thêm danh mục
                    </button>
                )}
            </div>

            {/* Type Toggle */}
            <div className="flex gap-2 mb-6">
                <button
                    onClick={() => setActiveTab("EXPENSE")}
                    className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "EXPENSE"
                        ? "bg-[var(--danger)] text-white shadow-lg shadow-[var(--danger)]/20"
                        : "glass text-[var(--text-secondary)]"
                        }`}
                >
                    <Minus className="w-4 h-4" /> Chi tiêu
                </button>
                <button
                    onClick={() => setActiveTab("INCOME")}
                    className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "INCOME"
                        ? "bg-[var(--success)] text-white shadow-lg shadow-[var(--success)]/20"
                        : "glass text-[var(--text-secondary)]"
                        }`}
                >
                    <Plus className="w-4 h-4" /> Thu nhập
                </button>
            </div>

            {/* Add / Edit Form */}
            {(isAdding || editingId) && (
                <div className="glass rounded-2xl p-4 mb-6 animate-slide-up flex flex-col sm:flex-row gap-3">
                    <div className="w-full sm:w-20">
                        <label className="text-xs text-[var(--text-muted)] mb-1.5 block">Icon</label>
                        <input
                            type="text"
                            value={formIcon}
                            onChange={(e) => setFormIcon(e.target.value)}
                            className="w-full px-4 py-3 rounded-xl glass text-center text-xl bg-transparent outline-none focus:ring-2 focus:ring-[var(--primary)]/30 transition-all"
                            placeholder="🍔"
                        />
                    </div>
                    <div className="flex-1">
                        <label className="text-xs text-[var(--text-muted)] mb-1.5 block">Tên danh mục</label>
                        <input
                            type="text"
                            value={formName}
                            onChange={(e) => setFormName(e.target.value)}
                            autoFocus
                            className="w-full px-4 py-3 rounded-xl glass text-sm bg-transparent outline-none focus:ring-2 focus:ring-[var(--primary)]/30 transition-all"
                            placeholder="Nhập tên..."
                        />
                    </div>
                    <div className="flex items-end gap-2 pb-1">
                        <button
                            onClick={handleSave}
                            disabled={!formName.trim()}
                            className="flex-1 sm:flex-none px-4 py-2.5 bg-[var(--primary)] text-white rounded-xl font-medium disabled:opacity-50 flex items-center justify-center"
                        >
                            <Check className="w-4 h-4 sm:mr-1" /> <span className="hidden sm:inline">Lưu</span>
                        </button>
                        <button
                            onClick={resetForm}
                            className="flex-1 sm:flex-none px-4 py-2.5 glass text-[var(--text-secondary)] rounded-xl font-medium flex items-center justify-center"
                        >
                            <X className="w-4 h-4 sm:mr-1" /> <span className="hidden sm:inline">Hủy</span>
                        </button>
                    </div>
                </div>
            )}

            {/* Category Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
                {filtered.map((cat) => (
                    <div
                        key={cat.id}
                        className="glass rounded-2xl p-4 flex flex-col items-center justify-center text-center gap-2 group relative transition-all hover:-translate-y-1 hover:shadow-lg"
                    >
                        <div className="w-14 h-14 rounded-full bg-[var(--bg-card-hover)] flex items-center justify-center text-3xl mb-1 shadow-inner">
                            {cat.icon}
                        </div>
                        <p className="font-medium text-sm truncate w-full px-2" title={cat.name}>
                            {cat.name}
                        </p>

                        {/* Actions overlay */}
                        <div className="absolute top-2 right-2 flex flex-col gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                            {confirmDeleteId === cat.id ? (
                                <div className="flex flex-col gap-1 bg-[var(--bg)] p-1.5 rounded-xl shadow-lg border border-[var(--border)] z-10">
                                    <span className="text-[10px] text-center mb-1 text-[var(--text-muted)]">Bạn chắc chứ?</span>
                                    <button
                                        onClick={() => handleDelete(cat.id)}
                                        className="w-8 h-8 rounded-lg bg-[var(--danger)] text-white flex items-center justify-center hover:brightness-110"
                                    >
                                        <Check className="w-4 h-4" />
                                    </button>
                                    <button
                                        onClick={() => setConfirmDeleteId(null)}
                                        className="w-8 h-8 rounded-lg glass flex items-center justify-center"
                                    >
                                        <X className="w-4 h-4" />
                                    </button>
                                </div>
                            ) : (
                                <>
                                    <button
                                        onClick={() => handleEditClick(cat)}
                                        className="w-7 h-7 rounded-lg glass flex items-center justify-center text-[var(--text-secondary)] hover:text-[var(--primary)] hover:bg-[var(--primary)]/10"
                                        title="Sửa"
                                    >
                                        <Pencil className="w-3.5 h-3.5" />
                                    </button>
                                    <button
                                        onClick={() => setConfirmDeleteId(cat.id)}
                                        className="w-7 h-7 rounded-lg glass flex items-center justify-center text-[var(--text-secondary)] hover:text-[var(--danger)] hover:bg-[var(--danger)]/10"
                                        title="Xóa"
                                    >
                                        <Trash2 className="w-3.5 h-3.5" />
                                    </button>
                                </>
                            )}
                        </div>
                    </div>
                ))}

                {filtered.length === 0 && !isAdding && (
                    <div className="col-span-full py-12 text-center text-[var(--text-muted)] glass rounded-2xl">
                        <p>Chưa có danh mục nào.</p>
                        <p className="text-sm mt-1">Hãy bấm "Thêm danh mục" để tạo mới.</p>
                    </div>
                )}
            </div>
        </div>
    );
}
