"use client";

import { useState, useEffect } from "react";
import { X, Plus, Minus } from "lucide-react";
import {
    useCategories,
    useWallets,
    firestoreAddTransaction,
    firestoreUpdateTransaction,
    Transaction,
} from "@/hooks/useFirestore";
import { useAuth } from "@/context/AuthContext";

interface Props {
    isOpen: boolean;
    onClose: () => void;
    editingTransaction?: Transaction | null;
}

export default function AddTransactionModal({ isOpen, onClose, editingTransaction }: Props) {
    const { user } = useAuth();
    const { categories } = useCategories();
    const { wallets } = useWallets();

    const [type, setType] = useState<"EXPENSE" | "INCOME">("EXPENSE");
    const [amount, setAmount] = useState("");
    const [note, setNote] = useState("");
    const [selectedCategoryId, setSelectedCategoryId] = useState<string>("");
    const [selectedWalletId, setSelectedWalletId] = useState<string>("");
    const [date, setDate] = useState(
        new Date().toISOString().split("T")[0]
    );
    const [saving, setSaving] = useState(false);

    // Effect to populate form when editing
    useEffect(() => {
        if (isOpen && editingTransaction) {
            setType(editingTransaction.type as "EXPENSE" | "INCOME");
            setAmount(editingTransaction.amount.toString());
            setNote(editingTransaction.note);
            // Default to first matching category if name matches, or reset
            const cat = categories.find(c => c.name === editingTransaction.categoryName);
            setSelectedCategoryId(cat?.id || "");
            setSelectedWalletId(editingTransaction.walletId.toString());
            // Convert timestamp to YYYY-MM-DD
            const d = new Date(editingTransaction.date);
            const dateStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
            setDate(dateStr);
        } else if (isOpen && !editingTransaction) {
            // Reset for new
            setType("EXPENSE");
            setAmount("");
            setNote("");
            setSelectedCategoryId("");
            setSelectedWalletId("");
            setDate(new Date().toISOString().split("T")[0]);
        }
    }, [isOpen, editingTransaction, categories]);

    const filteredCategories = categories.filter((c) => c.type === type);

    const selectedCategory = categories.find((c) => c.id === selectedCategoryId);

    const handleSubmit = async () => {
        if (!user || !amount || !selectedCategoryId) return;

        const cat = categories.find((c) => c.id === selectedCategoryId);
        if (!cat) return;

        setSaving(true);
        try {
            const dateTimestamp = new Date(date).getTime();
            const dataToSave = {
                amount: parseFloat(amount),
                type,
                categoryName: cat.name,
                categoryIcon: cat.icon,
                categoryId: parseInt(cat.id) || 0,
                note: note.trim(),
                date: dateTimestamp,
                walletId: selectedWalletId ? parseInt(selectedWalletId) : 1,
                paymentMethod: "CASH",
            };

            if (editingTransaction) {
                await firestoreUpdateTransaction(user.uid, editingTransaction.id, dataToSave);
            } else {
                await firestoreAddTransaction(user.uid, dataToSave);
            }
            onClose();
        } catch (error) {
            console.error("Error saving transaction:", error);
        } finally {
            setSaving(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center">
            {/* Backdrop */}
            <div
                className="absolute inset-0 bg-black/60 backdrop-blur-sm"
                onClick={onClose}
            />

            {/* Modal */}
            <div className="relative w-full sm:max-w-md mx-auto glass rounded-t-3xl sm:rounded-3xl p-6 animate-slide-up max-h-[90vh] overflow-y-auto">
                {/* Header */}
                <div className="flex items-center justify-between mb-6">
                    <h2 className="text-xl font-bold">Thêm giao dịch</h2>
                    <button
                        onClick={onClose}
                        className="w-8 h-8 rounded-full bg-[var(--bg-card-hover)] flex items-center justify-center hover:bg-[var(--border)] transition-colors"
                    >
                        <X className="w-4 h-4" />
                    </button>
                </div>

                {/* Type Toggle */}
                <div className="flex gap-2 mb-6">
                    <button
                        onClick={() => { setType("EXPENSE"); setSelectedCategoryId(""); }}
                        className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all duration-200 flex items-center justify-center gap-2 ${type === "EXPENSE"
                            ? "bg-[var(--danger)] text-white shadow-lg shadow-[var(--danger)]/20"
                            : "glass text-[var(--text-secondary)]"
                            }`}
                    >
                        <Minus className="w-4 h-4" /> Chi tiêu
                    </button>
                    <button
                        onClick={() => { setType("INCOME"); setSelectedCategoryId(""); }}
                        className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all duration-200 flex items-center justify-center gap-2 ${type === "INCOME"
                            ? "bg-[var(--success)] text-white shadow-lg shadow-[var(--success)]/20"
                            : "glass text-[var(--text-secondary)]"
                            }`}
                    >
                        <Plus className="w-4 h-4" /> Thu nhập
                    </button>
                </div>

                {/* Amount */}
                <div className="mb-5">
                    <label className="text-xs text-[var(--text-muted)] mb-1.5 block">
                        Số tiền (VNĐ)
                    </label>
                    <input
                        type="number"
                        value={amount}
                        onChange={(e) => setAmount(e.target.value)}
                        placeholder="0"
                        className="w-full px-4 py-3.5 rounded-xl glass text-2xl font-bold bg-transparent outline-none focus:ring-2 focus:ring-[var(--primary)]/30 transition-all text-center"
                        autoFocus
                    />
                </div>

                {/* Category Picker */}
                <div className="mb-5">
                    <label className="text-xs text-[var(--text-muted)] mb-2 block">
                        Danh mục
                    </label>
                    <div className="grid grid-cols-4 gap-2 max-h-[160px] overflow-y-auto pr-1">
                        {filteredCategories.map((cat) => (
                            <button
                                key={cat.id}
                                onClick={() => setSelectedCategoryId(cat.id)}
                                className={`flex flex-col items-center gap-1 p-2.5 rounded-xl text-xs transition-all duration-200 ${selectedCategoryId === cat.id
                                    ? "bg-[var(--primary)] text-white shadow-lg"
                                    : "glass hover:bg-[var(--bg-card-hover)]"
                                    }`}
                            >
                                <span className="text-lg">{cat.icon}</span>
                                <span className="truncate w-full text-center text-[10px]">
                                    {cat.name}
                                </span>
                            </button>
                        ))}
                        {filteredCategories.length === 0 && (
                            <p className="col-span-4 text-center text-[var(--text-muted)] text-sm py-4">
                                Chưa có danh mục. Thêm trên Android trước.
                            </p>
                        )}
                    </div>
                </div>

                {/* Note */}
                <div className="mb-5">
                    <label className="text-xs text-[var(--text-muted)] mb-1.5 block">
                        Ghi chú
                    </label>
                    <input
                        type="text"
                        value={note}
                        onChange={(e) => setNote(e.target.value)}
                        placeholder="Nhập ghi chú..."
                        className="w-full px-4 py-3 rounded-xl glass text-sm bg-transparent outline-none focus:ring-2 focus:ring-[var(--primary)]/30 transition-all"
                    />
                </div>

                {/* Date */}
                <div className="mb-5">
                    <label className="text-xs text-[var(--text-muted)] mb-1.5 block">
                        Ngày
                    </label>
                    <input
                        type="date"
                        value={date}
                        onChange={(e) => setDate(e.target.value)}
                        className="w-full px-4 py-3 rounded-xl glass text-sm bg-transparent outline-none focus:ring-2 focus:ring-[var(--primary)]/30 transition-all"
                    />
                </div>

                {/* Wallet Picker */}
                {wallets.length > 0 && (
                    <div className="mb-6">
                        <label className="text-xs text-[var(--text-muted)] mb-1.5 block">
                            Ví
                        </label>
                        <div className="flex gap-2 overflow-x-auto">
                            {wallets.map((w) => (
                                <button
                                    key={w.id}
                                    onClick={() => setSelectedWalletId(w.id)}
                                    className={`shrink-0 px-4 py-2.5 rounded-xl text-sm transition-all duration-200 ${selectedWalletId === w.id
                                        ? "bg-[var(--primary)] text-white shadow-lg"
                                        : "glass hover:bg-[var(--bg-card-hover)]"
                                        }`}
                                >
                                    {w.icon || "💰"} {w.name}
                                </button>
                            ))}
                        </div>
                    </div>
                )}

                {/* Submit */}
                <button
                    onClick={handleSubmit}
                    disabled={!amount || !selectedCategoryId || saving}
                    className={`w-full py-4 rounded-xl font-bold text-white text-sm transition-all duration-200 ${!amount || !selectedCategoryId || saving
                        ? "bg-gray-600 opacity-50 cursor-not-allowed"
                        : type === "EXPENSE"
                            ? "bg-[var(--danger)] hover:brightness-110 shadow-lg shadow-[var(--danger)]/30"
                            : "bg-[var(--success)] hover:brightness-110 shadow-lg shadow-[var(--success)]/30"
                        }`}
                >
                    {saving ? "Đang lưu..." : `Thêm ${type === "EXPENSE" ? "Chi tiêu" : "Thu nhập"}`}
                </button>
            </div>
        </div>
    );
}
