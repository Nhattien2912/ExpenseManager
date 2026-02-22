"use client";

import { useTransactions } from "@/hooks/useFirestore";
import { formatCurrency, formatDate } from "@/lib/utils";
import { useMemo, useState } from "react";
import { PiggyBank, ArrowUpRight, ArrowDownRight, Target } from "lucide-react";

// Normalize text for simple matching (remove accents)
function normalizeString(str: string): string {
    return str
        .normalize("NFD")
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/đ/g, "d")
        .replace(/Đ/g, "D")
        .toLowerCase();
}

interface Bucket {
    name: string;
    deposited: number;
    withdrawn: number;
    transactions: any[];
}

export default function SavingsPage() {
    const { transactions, loading } = useTransactions();
    const [selectedBucket, setSelectedBucket] = useState<string | null>(null);

    // Grouping logic matching Android SavingsViewModel
    const { buckets, totalSaved, totalWithdrawn } = useMemo(() => {
        const bucketMap = new Map<string, Bucket>();
        let deposited = 0;
        let withdrawn = 0;

        // Filter transactions for savings
        transactions.forEach((t) => {
            const normalizedCat = normalizeString(t.categoryName);
            const isDeposit = normalizedCat.includes("gui tiet kiem") ||
                normalizedCat.includes("tiet kiem") && t.type === "EXPENSE"; // Sometimes custom categories
            const isWithdrawal = normalizedCat.includes("rut tiet kiem");

            if (!isDeposit && !isWithdrawal) return;

            // Extract bucket name from [BucketName] in note
            const match = t.note.match(/^\[(.*?)\]/);
            const bucketName = match ? match[1].trim() : "Chung";

            if (!bucketMap.has(bucketName)) {
                bucketMap.set(bucketName, { name: bucketName, deposited: 0, withdrawn: 0, transactions: [] });
            }

            const bucket = bucketMap.get(bucketName)!;
            bucket.transactions.push(t);

            if (isDeposit) {
                bucket.deposited += t.amount;
                deposited += t.amount;
            } else if (isWithdrawal) {
                bucket.withdrawn += t.amount;
                withdrawn += t.amount;
            }
        });

        // Sort transactions inside buckets
        bucketMap.forEach(b => {
            b.transactions.sort((x, y) => y.date - x.date);
        });

        return {
            buckets: Array.from(bucketMap.values()).sort((a, b) => b.deposited - a.deposited),
            totalSaved: deposited,
            totalWithdrawn: withdrawn
        };
    }, [transactions]);

    const activeBucket = selectedBucket ? buckets.find(b => b.name === selectedBucket) : null;
    const currentTotal = totalSaved - totalWithdrawn;

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="animate-fade-in max-w-4xl relative">
            <h1 className="text-2xl font-bold mb-6">Tích lũy & Tiết kiệm</h1>

            {/* Total summary */}
            <div className="glass rounded-3xl p-6 sm:p-8 mb-8 relative overflow-hidden bg-gradient-to-br from-[var(--primary)]/10 to-[var(--accent)]/10">
                <PiggyBank className="absolute -right-6 -bottom-6 w-40 h-40 text-[var(--primary)]/10" />

                <div className="relative z-10">
                    <p className="text-sm font-medium text-[var(--text-secondary)] uppercase tracking-wider mb-2">
                        Tổng quỹ tiết kiệm
                    </p>
                    <h2 className="text-4xl sm:text-5xl font-extrabold gradient-text mb-6">
                        {formatCurrency(currentTotal)}
                    </h2>

                    <div className="flex flex-wrap gap-6 sm:gap-12">
                        <div>
                            <p className="text-xs text-[var(--text-muted)] flex items-center gap-1 mb-1">
                                <ArrowUpRight className="w-3.5 h-3.5 text-[var(--success)]" /> Đã gửi vào
                            </p>
                            <p className="font-semibold text-lg">{formatCurrency(totalSaved)}</p>
                        </div>
                        <div>
                            <p className="text-xs text-[var(--text-muted)] flex items-center gap-1 mb-1">
                                <ArrowDownRight className="w-3.5 h-3.5 text-[var(--danger)]" /> Đã rút ra
                            </p>
                            <p className="font-semibold text-lg">{formatCurrency(totalWithdrawn)}</p>
                        </div>
                    </div>
                </div>
            </div>

            {buckets.length === 0 ? (
                <div className="glass rounded-2xl p-12 text-center text-[var(--text-muted)]">
                    <p className="text-lg mb-2">Chưa có khoản tiết kiệm nào</p>
                    <p className="text-sm">Hãy thêm các giao dịch vào danh mục "Gửi tiết kiệm" hoặc "Rút tiết kiệm" để theo dõi.</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                    {/* Bucket List */}
                    <div className="lg:col-span-1 space-y-3">
                        <h3 className="font-semibold mb-3">Mục tiêu / Quỹ</h3>
                        {buckets.map(b => (
                            <button
                                key={b.name}
                                onClick={() => setSelectedBucket(b.name)}
                                className={`w-full text-left p-4 rounded-2xl transition-all duration-200 group ${selectedBucket === b.name
                                        ? "bg-[var(--primary)] text-white shadow-lg shadow-[var(--primary)]/20"
                                        : "glass hover:bg-[var(--bg-card-hover)] text-[var(--text-primary)]"
                                    }`}
                            >
                                <div className="flex items-center gap-3 mb-2">
                                    <div className={`p-2 rounded-xl ${selectedBucket === b.name ? "bg-white/20 text-white" : "bg-[var(--primary)]/10 text-[var(--primary)]"}`}>
                                        <Target className="w-5 h-5" />
                                    </div>
                                    <span className="font-semibold flex-1 truncate">{b.name}</span>
                                </div>
                                <div className={`text-sm ${selectedBucket === b.name ? "text-white/80" : "text-[var(--text-muted)]"} flex justify-between`}>
                                    <span>Tích lũy:</span>
                                    <span className="font-medium text-[var(--success)]">{formatCurrency(b.deposited - b.withdrawn)}</span>
                                </div>
                            </button>
                        ))}
                    </div>

                    {/* Bucket Details */}
                    <div className="lg:col-span-2">
                        {activeBucket ? (
                            <div className="glass rounded-2xl p-6 h-full">
                                <div className="flex justify-between items-end mb-6 pb-6 border-b border-[var(--border)]">
                                    <div>
                                        <h3 className="text-xl font-bold mb-1">{activeBucket.name}</h3>
                                        <p className="text-sm text-[var(--text-muted)]">{activeBucket.transactions.length} giao dịch</p>
                                    </div>
                                    <div className="text-right">
                                        <p className="text-2xl font-bold text-[var(--primary)]">
                                            {formatCurrency(activeBucket.deposited - activeBucket.withdrawn)}
                                        </p>
                                    </div>
                                </div>

                                <div className="space-y-4">
                                    {activeBucket.transactions.map((t, i) => {
                                        const isDeposit = normalizeString(t.categoryName).includes("gui");
                                        return (
                                            <div key={i} className="flex items-center justify-between p-3 rounded-xl hover:bg-[var(--bg-card-hover)] transition-colors">
                                                <div className="flex items-center gap-3">
                                                    <div className={`w-10 h-10 rounded-full flex items-center justify-center ${isDeposit ? "bg-[var(--success)]/10 text-[var(--success)]" : "bg-[var(--danger)]/10 text-[var(--danger)]"}`}>
                                                        {isDeposit ? <ArrowUpRight className="w-4 h-4" /> : <ArrowDownRight className="w-4 h-4" />}
                                                    </div>
                                                    <div>
                                                        <p className="font-medium text-sm">
                                                            {t.note.replace(/^\[.*?\]\s*/, "") || "Tiết kiệm"}
                                                        </p>
                                                        <p className="text-xs text-[var(--text-muted)]">{formatDate(t.date)}</p>
                                                    </div>
                                                </div>
                                                <span className={`font-semibold ${isDeposit ? "text-[var(--success)]" : "text-[var(--danger)]"}`}>
                                                    {isDeposit ? "+" : "-"}{formatCurrency(t.amount)}
                                                </span>
                                            </div>
                                        );
                                    })}
                                </div>
                            </div>
                        ) : (
                            <div className="glass rounded-2xl h-full flex flex-col items-center justify-center text-[var(--text-muted)] p-8 text-center hidden lg:flex">
                                <Target className="w-12 h-12 mb-4 opacity-20" />
                                <p>Chọn một quỹ ở bên trái để xem chi tiết</p>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
