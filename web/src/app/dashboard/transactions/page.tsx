"use client";

import { useTransactions, firestoreDeleteTransaction, Transaction } from "@/hooks/useFirestore";
import { useAuth } from "@/context/AuthContext";
import { formatCurrency, formatDate, formatTime } from "@/lib/utils";
import {
    ArrowUpRight,
    ArrowDownRight,
    Search,
    Trash2,
    Pencil,
} from "lucide-react";
import { useState, useMemo } from "react";
import AddTransactionModal from "@/components/AddTransactionModal";

export default function TransactionsPage() {
    const { transactions, loading } = useTransactions();
    const { user } = useAuth();
    const [search, setSearch] = useState("");
    const [filter, setFilter] = useState<"ALL" | "INCOME" | "EXPENSE">("ALL");
    const [deletingId, setDeletingId] = useState<string | null>(null);
    const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
    const [editingTransaction, setEditingTransaction] = useState<Transaction | null>(null);
    const [showEditModal, setShowEditModal] = useState(false);

    const filtered = useMemo(() => {
        return transactions.filter((t) => {
            const matchSearch =
                t.note?.toLowerCase().includes(search.toLowerCase()) ||
                t.categoryName?.toLowerCase().includes(search.toLowerCase());
            const matchFilter = filter === "ALL" || t.type === filter;
            return matchSearch && matchFilter;
        });
    }, [transactions, search, filter]);

    // Group by date
    const grouped = useMemo(() => {
        const groups: Record<string, typeof filtered> = {};
        filtered.forEach((t) => {
            const dateKey = formatDate(t.date);
            if (!groups[dateKey]) groups[dateKey] = [];
            groups[dateKey].push(t);
        });
        return groups;
    }, [filtered]);

    const handleDelete = async (id: string) => {
        if (!user) return;
        setDeletingId(id);
        try {
            await firestoreDeleteTransaction(user.uid, id);
        } catch (error) {
            console.error("Error deleting:", error);
        } finally {
            setDeletingId(null);
            setConfirmDeleteId(null);
        }
    };

    const handleEdit = (transaction: Transaction) => {
        setEditingTransaction(transaction);
        setShowEditModal(true);
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="animate-fade-in max-w-4xl">
            <h1 className="text-2xl font-bold mb-6">Lịch sử giao dịch</h1>

            {/* Search & Filter */}
            <div className="flex flex-col sm:flex-row gap-3 mb-6">
                <div className="relative flex-1">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[var(--text-muted)]" />
                    <input
                        type="text"
                        placeholder="Tìm kiếm giao dịch..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="w-full pl-10 pr-4 py-3 rounded-xl glass text-sm bg-transparent outline-none focus:ring-2 focus:ring-[var(--primary)]/30 transition-all"
                    />
                </div>
                <div className="flex gap-2">
                    {(["ALL", "INCOME", "EXPENSE"] as const).map((f) => (
                        <button
                            key={f}
                            onClick={() => setFilter(f)}
                            className={`px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 ${filter === f
                                ? "bg-[var(--primary)] text-white shadow-lg shadow-[var(--primary)]/20"
                                : "glass text-[var(--text-secondary)] hover:text-[var(--text-primary)]"
                                }`}
                        >
                            {f === "ALL" ? "Tất cả" : f === "INCOME" ? "Thu" : "Chi"}
                        </button>
                    ))}
                </div>
            </div>

            {/* Stats bar */}
            <div className="flex gap-4 mb-6 text-sm">
                <span className="text-[var(--text-muted)]">
                    {filtered.length} giao dịch
                </span>
                <span className="text-[var(--success)]">
                    +{formatCurrency(filtered.filter((t) => t.type === "INCOME" || t.type === "LOAN_TAKE").reduce((s, t) => s + t.amount, 0))}
                </span>
                <span className="text-[var(--danger)]">
                    -{formatCurrency(filtered.filter((t) => t.type === "EXPENSE" || t.type === "LOAN_GIVE").reduce((s, t) => s + t.amount, 0))}
                </span>
            </div>

            {/* Transaction list */}
            {Object.keys(grouped).length === 0 ? (
                <div className="glass rounded-2xl p-12 text-center text-[var(--text-muted)]">
                    <p className="text-lg mb-2">Không tìm thấy giao dịch</p>
                    <p className="text-sm">Nhấn nút + để thêm giao dịch mới</p>
                </div>
            ) : (
                <div className="space-y-6">
                    {Object.entries(grouped).map(([date, items]) => (
                        <div key={date}>
                            {/* Date header */}
                            <div className="flex items-center gap-3 mb-3">
                                <span className="text-sm font-medium text-[var(--text-muted)]">
                                    {date}
                                </span>
                                <div className="flex-1 h-[1px] bg-[var(--border)]" />
                                <span className="text-xs text-[var(--text-muted)]">
                                    {formatCurrency(
                                        items.reduce(
                                            (s, t) =>
                                                s + (t.type === "INCOME" || t.type === "LOAN_TAKE" ? t.amount : -t.amount),
                                            0
                                        )
                                    )}
                                </span>
                            </div>

                            {/* Items */}
                            <div className="glass rounded-2xl overflow-hidden">
                                {items.map((t, i) => (
                                    <div
                                        key={t.id}
                                        className={`flex items-center gap-4 p-4 hover:bg-[var(--bg-card-hover)] transition-colors duration-200 group ${i < items.length - 1
                                            ? "border-b border-[var(--border)]"
                                            : ""
                                            }`}
                                    >
                                        <div className="w-10 h-10 rounded-xl bg-[var(--bg-card-hover)] flex items-center justify-center text-lg shrink-0">
                                            {t.categoryIcon || "💰"}
                                        </div>

                                        <div className="flex-1 min-w-0">
                                            <p className="font-medium truncate">
                                                {t.note || t.categoryName}
                                            </p>
                                            <p className="text-xs text-[var(--text-muted)]">
                                                {t.categoryName} • {formatTime(t.date)}
                                            </p>
                                        </div>

                                        <div className="text-right shrink-0 flex items-center gap-2">
                                            <p
                                                className={`font-semibold ${t.type === "INCOME" || t.type === "LOAN_TAKE"
                                                    ? "text-[var(--success)]"
                                                    : "text-[var(--danger)]"
                                                    }`}
                                            >
                                                {t.type === "INCOME" || t.type === "LOAN_TAKE" ? "+" : "-"}
                                                {formatCurrency(Math.abs(t.amount))}
                                            </p>
                                            {t.type === "INCOME" || t.type === "LOAN_TAKE" ? (
                                                <ArrowUpRight className="w-4 h-4 text-[var(--success)]" />
                                            ) : (
                                                <ArrowDownRight className="w-4 h-4 text-[var(--danger)]" />
                                            )}
                                        </div>

                                        {/* Action buttons (appear on hover) */}
                                        {confirmDeleteId === t.id ? (
                                            <div className="flex gap-1 shrink-0 ml-2">
                                                <button
                                                    onClick={() => handleDelete(t.id)}
                                                    disabled={deletingId === t.id}
                                                    className="px-2 py-1 rounded-lg bg-[var(--danger)] text-white text-xs font-medium"
                                                >
                                                    {deletingId === t.id ? "..." : "Xóa"}
                                                </button>
                                                <button
                                                    onClick={() => setConfirmDeleteId(null)}
                                                    className="px-2 py-1 rounded-lg glass text-xs"
                                                >
                                                    Hủy
                                                </button>
                                            </div>
                                        ) : (
                                            <div className="opacity-0 group-hover:opacity-100 shrink-0 flex items-center gap-1 ml-2 transition-all">
                                                <button
                                                    onClick={() => handleEdit(t)}
                                                    className="w-8 h-8 rounded-lg hover:bg-[var(--primary)]/10 flex items-center justify-center transition-colors"
                                                    title="Sửa giao dịch"
                                                >
                                                    <Pencil className="w-3.5 h-3.5 text-[var(--primary)]" />
                                                </button>
                                                <button
                                                    onClick={() => setConfirmDeleteId(t.id)}
                                                    className="w-8 h-8 rounded-lg hover:bg-[var(--danger)]/10 flex items-center justify-center transition-colors"
                                                    title="Xóa giao dịch"
                                                >
                                                    <Trash2 className="w-3.5 h-3.5 text-[var(--danger)]" />
                                                </button>
                                            </div>
                                        )}
                                    </div>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* Edit Modal */}
            <AddTransactionModal
                isOpen={showEditModal}
                onClose={() => {
                    setShowEditModal(false);
                    setEditingTransaction(null);
                }}
                editingTransaction={editingTransaction}
            />
        </div>
    );
}
