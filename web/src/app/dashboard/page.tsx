"use client";

import { useTransactions, useWallets } from "@/hooks/useFirestore";
import { formatCurrency, getRelativeDate } from "@/lib/utils";
import { useAuth } from "@/context/AuthContext";
import { useState } from "react";
import {
    TrendingUp,
    TrendingDown,
    Wallet,
    ArrowUpRight,
    ArrowDownRight,
    Clock,
    ChevronLeft,
    ChevronRight,
} from "lucide-react";

export default function DashboardPage() {
    const { user } = useAuth();
    const { transactions, loading } = useTransactions();
    const { wallets } = useWallets();

    // Month navigation (same as Android)
    // Default to month of most recent transaction
    const latestDate = transactions.length > 0
        ? new Date(transactions[0].date)
        : new Date();
    const [viewDate, setViewDate] = useState<Date | null>(null);
    const activeDate = viewDate || latestDate;
    const viewMonth = activeDate.getMonth();
    const viewYear = activeDate.getFullYear();

    const prevMonth = () => {
        setViewDate(new Date(viewYear, viewMonth - 1, 1));
    };
    const nextMonth = () => {
        setViewDate(new Date(viewYear, viewMonth + 1, 1));
    };

    // Filter transactions for the selected month
    const startOfMonth = new Date(viewYear, viewMonth, 1).getTime();
    const endOfMonth = new Date(viewYear, viewMonth + 1, 0, 23, 59, 59, 999).getTime();
    const monthlyTransactions = transactions.filter(
        (t) => t.date >= startOfMonth && t.date <= endOfMonth
    );

    const totalIncome = monthlyTransactions
        .filter((t) => t.type === "INCOME" || t.type === "LOAN_TAKE")
        .reduce((sum, t) => sum + t.amount, 0);

    const totalExpense = monthlyTransactions
        .filter((t) => t.type === "EXPENSE" || t.type === "LOAN_GIVE")
        .reduce((sum, t) => sum + t.amount, 0);

    // Net balance = ALL income - ALL expense (across all time, same as Android)
    // INCOME & LOAN_TAKE increase balance
    // EXPENSE & LOAN_GIVE decrease balance
    // TRANSFER is neutral (moves between wallets)
    const allIncome = transactions
        .filter((t) => t.type === "INCOME" || t.type === "LOAN_TAKE")
        .reduce((sum, t) => sum + t.amount, 0);
    const allExpense = transactions
        .filter((t) => t.type === "EXPENSE" || t.type === "LOAN_GIVE")
        .reduce((sum, t) => sum + t.amount, 0);

    const walletBalance = wallets.reduce((sum, w) => sum + (w.initialBalance || 0), 0);
    const netBalance = walletBalance + allIncome - allExpense;

    // Monthly net
    const monthlyNet = totalIncome - totalExpense;

    const recentTransactions = monthlyTransactions.slice(0, 8);

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="animate-fade-in max-w-6xl">
            {/* Header with month navigation */}
            <div className="mb-8">
                <h1 className="text-2xl font-bold">
                    Xin chào,{" "}
                    <span className="gradient-text">
                        {user?.displayName?.split(" ").pop() || "bạn"}
                    </span>{" "}
                    👋
                </h1>
                <div className="flex items-center gap-3 mt-2">
                    <button
                        onClick={prevMonth}
                        className="p-1.5 rounded-lg hover:bg-[var(--bg-card-hover)] transition-colors"
                    >
                        <ChevronLeft className="w-4 h-4 text-[var(--text-muted)]" />
                    </button>
                    <p className="text-[var(--text-muted)] font-medium">
                        Tháng {viewMonth + 1} / {viewYear}
                    </p>
                    <button
                        onClick={nextMonth}
                        className="p-1.5 rounded-lg hover:bg-[var(--bg-card-hover)] transition-colors"
                    >
                        <ChevronRight className="w-4 h-4 text-[var(--text-muted)]" />
                    </button>
                </div>
            </div>

            {/* Summary Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
                {/* Total Balance (all time) */}
                <div className="glass rounded-2xl p-5 card-glow animate-slide-up">
                    <div className="flex items-center justify-between mb-3">
                        <span className="text-sm text-[var(--text-muted)]">Tổng tài sản</span>
                        <div className="w-9 h-9 rounded-xl bg-[var(--primary)]/10 flex items-center justify-center">
                            <Wallet className="w-4 h-4 text-[var(--primary-light)]" />
                        </div>
                    </div>
                    <p className={`text-2xl font-bold ${netBalance < 0 ? "text-[var(--danger)]" : ""}`}>
                        {formatCurrency(netBalance)}
                    </p>
                </div>

                {/* Monthly Income */}
                <div className="glass rounded-2xl p-5 animate-slide-up" style={{ animationDelay: "0.1s" }}>
                    <div className="flex items-center justify-between mb-3">
                        <span className="text-sm text-[var(--text-muted)]">Thu nhập tháng</span>
                        <div className="w-9 h-9 rounded-xl bg-[var(--success)]/10 flex items-center justify-center">
                            <TrendingUp className="w-4 h-4 text-[var(--success)]" />
                        </div>
                    </div>
                    <p className="text-2xl font-bold text-[var(--success)]">
                        +{formatCurrency(totalIncome)}
                    </p>
                </div>

                {/* Monthly Expense */}
                <div className="glass rounded-2xl p-5 animate-slide-up" style={{ animationDelay: "0.2s" }}>
                    <div className="flex items-center justify-between mb-3">
                        <span className="text-sm text-[var(--text-muted)]">Chi tiêu tháng</span>
                        <div className="w-9 h-9 rounded-xl bg-[var(--danger)]/10 flex items-center justify-center">
                            <TrendingDown className="w-4 h-4 text-[var(--danger)]" />
                        </div>
                    </div>
                    <p className="text-2xl font-bold text-[var(--danger)]">
                        -{formatCurrency(totalExpense)}
                    </p>
                </div>

                {/* Monthly net */}
                <div className="glass rounded-2xl p-5 animate-slide-up" style={{ animationDelay: "0.3s" }}>
                    <div className="flex items-center justify-between mb-3">
                        <span className="text-sm text-[var(--text-muted)]">Thu chi tháng</span>
                        <div className="w-9 h-9 rounded-xl bg-[var(--warning)]/10 flex items-center justify-center">
                            <Clock className="w-4 h-4 text-[var(--warning)]" />
                        </div>
                    </div>
                    <p className={`text-2xl font-bold ${monthlyNet < 0 ? "text-[var(--danger)]" : "text-[var(--success)]"}`}>
                        {formatCurrency(monthlyNet)}
                    </p>
                    <p className="text-xs text-[var(--text-muted)] mt-1">
                        {monthlyTransactions.length} giao dịch
                    </p>
                </div>
            </div>

            {/* Recent Transactions this month */}
            <div className="glass rounded-2xl p-6">
                <div className="flex items-center justify-between mb-5">
                    <h2 className="text-lg font-semibold">Giao dịch tháng {viewMonth + 1}</h2>
                    <a
                        href="/dashboard/transactions"
                        className="text-sm text-[var(--primary-light)] hover:underline"
                    >
                        Xem tất cả →
                    </a>
                </div>

                {recentTransactions.length === 0 ? (
                    <div className="text-center py-12 text-[var(--text-muted)]">
                        <p className="text-lg mb-2">Không có giao dịch trong tháng này</p>
                        <p className="text-sm">
                            Thử chuyển sang tháng khác hoặc thêm giao dịch từ ứng dụng Android
                        </p>
                    </div>
                ) : (
                    <div className="space-y-2">
                        {recentTransactions.map((t, i) => (
                            <div
                                key={t.id}
                                className="flex items-center gap-4 p-3 rounded-xl hover:bg-[var(--bg-card-hover)] transition-colors duration-200 animate-slide-up"
                                style={{ animationDelay: `${i * 0.05}s` }}
                            >
                                <div className="w-10 h-10 rounded-xl bg-[var(--bg-card-hover)] flex items-center justify-center text-lg shrink-0">
                                    {t.categoryIcon || "💰"}
                                </div>

                                <div className="flex-1 min-w-0">
                                    <p className="font-medium truncate">
                                        {t.note || t.categoryName}
                                    </p>
                                    <p className="text-xs text-[var(--text-muted)]">
                                        {t.categoryName} • {getRelativeDate(t.date)}
                                    </p>
                                </div>

                                <div className="text-right shrink-0">
                                    <p
                                        className={`font-semibold ${t.type === "INCOME"
                                            ? "text-[var(--success)]"
                                            : "text-[var(--danger)]"
                                            }`}
                                    >
                                        {t.type === "INCOME" ? "+" : "-"}
                                        {formatCurrency(Math.abs(t.amount))}
                                    </p>
                                </div>

                                <div className="shrink-0">
                                    {t.type === "INCOME" ? (
                                        <ArrowUpRight className="w-4 h-4 text-[var(--success)]" />
                                    ) : (
                                        <ArrowDownRight className="w-4 h-4 text-[var(--danger)]" />
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}
