"use client";

import { useTransactions, firestoreDeleteTransaction, firestoreAddTransaction } from "@/hooks/useFirestore";
import { useAuth } from "@/context/AuthContext";
import { formatCurrency, formatDate } from "@/lib/utils";
import { useState, useMemo } from "react";
import { Plus, Minus, ArrowUpRight, ArrowDownRight, CheckCircle2, ChevronDown, ChevronUp } from "lucide-react";

export default function DebtsPage() {
    const { transactions, loading } = useTransactions();
    const { user } = useAuth();

    // LOAN_GIVE (Cho vay) -> Mình đưa tiền cho người ta -> Tài sản giảm lúc đầu, trả lại thì tăng
    // LOAN_TAKE (Đi vay) -> Người ta đưa tiền cho mình -> Tài sản tăng lúc đầu, trả lại thì giảm
    const [activeTab, setActiveTab] = useState<"RECEIVABLE" | "PAYABLE">("RECEIVABLE");
    const [settlingId, setSettlingId] = useState<string | null>(null);

    // List logic (Matching Android DebtViewModel)
    const receivableTransactions = useMemo(() => {
        return transactions.filter(t => t.type === "LOAN_GIVE" && t.categoryName.toLowerCase() === "cho vay")
            .sort((a, b) => b.date - a.date);
    }, [transactions]);

    const payableTransactions = useMemo(() => {
        return transactions.filter(t => t.type === "LOAN_TAKE" && t.categoryName.toLowerCase() === "đi vay")
            .sort((a, b) => b.date - a.date);
    }, [transactions]);

    const totalReceivable = useMemo(() => receivableTransactions.reduce((acc, t) => acc + t.amount, 0), [receivableTransactions]);
    const totalPayable = useMemo(() => payableTransactions.reduce((acc, t) => acc + t.amount, 0), [payableTransactions]);

    const activeTransactions = activeTab === "RECEIVABLE" ? receivableTransactions : payableTransactions;

    const handleSettleDebt = async (transaction: any) => {
        if (!user) return;
        setSettlingId(transaction.id);

        try {
            // Logic match Android DebtViewModel.settleDebt
            const isGive = transaction.type === "LOAN_GIVE";
            const newType = isGive ? "LOAN_TAKE" : "LOAN_GIVE";
            const notePrefix = isGive ? "Thu nợ từ giao dịch ngày" : "Trả nợ cho giao dịch ngày";
            const dateStr = formatDate(transaction.date);

            // 1. Insert settling transaction
            await firestoreAddTransaction(user.uid, {
                amount: transaction.amount,
                type: newType,
                categoryId: transaction.categoryId, // Fallback
                categoryName: isGive ? "Thu nợ" : "Trả nợ", // Needs proper category mapping if we had the full list, but names work
                categoryIcon: "🤝",
                note: `${notePrefix} ${dateStr}: ${transaction.note || "Không ghi chú"}`,
                date: Date.now(),
                walletId: transaction.walletId,
                paymentMethod: transaction.paymentMethod,
            });

            // 2. Delete original
            await firestoreDeleteTransaction(user.uid, transaction.id);
        } catch (error) {
            console.error("Error settling debt:", error);
        } finally {
            setSettlingId(null);
        }
    };

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="animate-fade-in max-w-4xl relative">
            <h1 className="text-2xl font-bold mb-6">Sổ nợ</h1>

            {/* Summary */}
            <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="glass rounded-2xl p-5 relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-[var(--success)]/10 rounded-full blur-2xl -mr-10 -mt-10 transition-transform group-hover:scale-150" />
                    <div className="relative z-10 flex flex-col">
                        <span className="text-xs font-medium text-[var(--success)] uppercase tracking-wider mb-2 flex items-center gap-1">
                            <ArrowUpRight className="w-4 h-4" /> Khách nợ mình
                        </span>
                        <span className="text-2xl font-bold">{formatCurrency(totalReceivable)}</span>
                        <span className="text-xs text-[var(--text-muted)] mt-1">{receivableTransactions.length} khoản cho ra</span>
                    </div>
                </div>

                <div className="glass rounded-2xl p-5 relative overflow-hidden group">
                    <div className="absolute top-0 right-0 w-24 h-24 bg-[var(--danger)]/10 rounded-full blur-2xl -mr-10 -mt-10 transition-transform group-hover:scale-150" />
                    <div className="relative z-10 flex flex-col">
                        <span className="text-xs font-medium text-[var(--danger)] uppercase tracking-wider mb-2 flex items-center gap-1">
                            <ArrowDownRight className="w-4 h-4" /> Mình nợ khách
                        </span>
                        <span className="text-2xl font-bold">{formatCurrency(totalPayable)}</span>
                        <span className="text-xs text-[var(--text-muted)] mt-1">{payableTransactions.length} khoản nợ vào</span>
                    </div>
                </div>
            </div>

            {/* Tabs */}
            <div className="flex gap-2 mb-6 p-1 glass rounded-2xl">
                <button
                    onClick={() => setActiveTab("RECEIVABLE")}
                    className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "RECEIVABLE"
                            ? "bg-[var(--success)] text-white shadow-lg shadow-[var(--success)]/20"
                            : "text-[var(--text-secondary)] hover:bg-[var(--bg-card-hover)]"
                        }`}
                >
                    <ArrowUpRight className="w-4 h-4" /> Cho vay
                </button>
                <button
                    onClick={() => setActiveTab("PAYABLE")}
                    className={`flex-1 py-3 rounded-xl font-medium text-sm transition-all duration-200 flex items-center justify-center gap-2 ${activeTab === "PAYABLE"
                            ? "bg-[var(--danger)] text-white shadow-lg shadow-[var(--danger)]/20"
                            : "text-[var(--text-secondary)] hover:bg-[var(--bg-card-hover)]"
                        }`}
                >
                    <ArrowDownRight className="w-4 h-4" /> Đi vay
                </button>
            </div>

            {/* List */}
            {activeTransactions.length === 0 ? (
                <div className="glass rounded-2xl p-12 text-center text-[var(--text-muted)]">
                    <p className="text-lg mb-2">Không có khoản nợ nào</p>
                    <p className="text-sm">Khi nào có khoản {activeTab === "RECEIVABLE" ? "Cho vay" : "Đi vay"} thì nó sẽ hiện ở đây.</p>
                </div>
            ) : (
                <div className="glass rounded-2xl overflow-hidden">
                    {activeTransactions.map((t, i) => (
                        <div
                            key={t.id}
                            className={`flex flex-col sm:flex-row sm:items-center gap-4 p-4 hover:bg-[var(--bg-card-hover)] transition-colors duration-200 ${i < activeTransactions.length - 1 ? "border-b border-[var(--border)]" : ""
                                }`}
                        >
                            <div className="flex items-center gap-4 flex-1 min-w-0">
                                <div className={`w-12 h-12 rounded-xl flex items-center justify-center text-xl shrink-0 ${activeTab === "RECEIVABLE" ? "bg-[var(--success)]/10 text-[var(--success)]" : "bg-[var(--danger)]/10 text-[var(--danger)]"}`}>
                                    {t.categoryIcon || "🤝"}
                                </div>
                                <div className="flex-1 min-w-0">
                                    <p className="font-semibold text-lg truncate mb-0.5">
                                        {formatCurrency(t.amount)}
                                    </p>
                                    <p className="text-sm">
                                        {t.note || (activeTab === "RECEIVABLE" ? "Khách mượn" : "Mượn khách")}
                                    </p>
                                    <p className="text-xs text-[var(--text-muted)] mt-1">
                                        Ngày vay: {formatDate(t.date)}
                                    </p>
                                </div>
                            </div>

                            <div className="flex gap-2 shrink-0 sm:w-auto mt-3 sm:mt-0">
                                <button
                                    onClick={() => handleSettleDebt(t)}
                                    disabled={settlingId === t.id}
                                    className="flex-1 sm:flex-none px-4 py-2 bg-[var(--primary)] text-white rounded-xl font-medium disabled:opacity-50 flex items-center justify-center gap-2 hover:brightness-110 shadow-lg shadow-[var(--primary)]/20 transition-all"
                                >
                                    <CheckCircle2 className="w-4 h-4" />
                                    {settlingId === t.id ? "Đang xử lý..." : "Đã thanh toán"}
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
