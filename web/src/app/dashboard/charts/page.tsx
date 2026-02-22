"use client";

import { useTransactions } from "@/hooks/useFirestore";
import { formatCurrency } from "@/lib/utils";
import { useMemo } from "react";
import { PieChart as PieChartIcon, BarChart3 } from "lucide-react";

export default function ChartsPage() {
    const { transactions, loading } = useTransactions();

    const now = new Date();
    const startOfMonth = new Date(now.getFullYear(), now.getMonth(), 1).getTime();
    const monthlyTransactions = transactions.filter((t) => t.date >= startOfMonth);

    // Category breakdown for expenses
    const categoryData = useMemo(() => {
        const map = new Map<string, { amount: number; icon: string }>();
        monthlyTransactions
            .filter((t) => t.type === "EXPENSE")
            .forEach((t) => {
                const existing = map.get(t.categoryName) || { amount: 0, icon: t.categoryIcon };
                map.set(t.categoryName, {
                    amount: existing.amount + t.amount,
                    icon: t.categoryIcon || existing.icon,
                });
            });
        return Array.from(map.entries())
            .map(([name, data]) => ({ name, ...data }))
            .sort((a, b) => b.amount - a.amount);
    }, [monthlyTransactions]);

    const totalExpense = categoryData.reduce((s, c) => s + c.amount, 0);

    // Daily spending for the current month
    const dailyData = useMemo(() => {
        const map = new Map<number, { income: number; expense: number }>();
        monthlyTransactions.forEach((t) => {
            const day = new Date(t.date).getDate();
            const existing = map.get(day) || { income: 0, expense: 0 };
            if (t.type === "INCOME") existing.income += t.amount;
            else existing.expense += t.amount;
            map.set(day, existing);
        });
        return Array.from(map.entries())
            .map(([day, data]) => ({ day, ...data }))
            .sort((a, b) => a.day - b.day);
    }, [monthlyTransactions]);

    const maxDailyAmount = Math.max(
        ...dailyData.map((d) => Math.max(d.income, d.expense)),
        1
    );

    const colors = [
        "#6366f1", "#06b6d4", "#10b981", "#f59e0b", "#ef4444",
        "#8b5cf6", "#ec4899", "#14b8a6", "#f97316", "#64748b",
    ];

    if (loading) {
        return (
            <div className="flex items-center justify-center h-[60vh]">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return (
        <div className="animate-fade-in max-w-5xl">
            <h1 className="text-2xl font-bold mb-6">
                Biểu đồ tháng {now.getMonth() + 1}/{now.getFullYear()}
            </h1>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Category Donut Chart - Premium */}
                <div className="glass rounded-2xl p-6">
                    <div className="flex items-center gap-2 mb-5">
                        <PieChartIcon className="w-5 h-5 text-[var(--primary-light)]" />
                        <h2 className="font-semibold">Chi tiêu theo danh mục</h2>
                    </div>

                    {categoryData.length === 0 ? (
                        <div className="text-center py-8 text-[var(--text-muted)]">
                            Chưa có dữ liệu chi tiêu
                        </div>
                    ) : (
                        <>
                            {/* Donut chart with outside labels */}
                            <div className="flex justify-center mb-4">
                                <div className="relative" style={{ width: 420, height: 420 }}>
                                    <svg viewBox="0 0 420 420" width="420" height="420" overflow="visible">
                                        {(() => {
                                            const cx = 210, cy = 210, r = 100;
                                            const strokeW = 40;
                                            const circumference = 2 * Math.PI * r;
                                            let offsetAngle = -90; // start from top

                                            return categoryData.map((cat, i) => {
                                                const pct = cat.amount / totalExpense;
                                                const pctDisplay = Math.round(pct * 100);
                                                const sliceAngle = pct * 360;
                                                const gap = categoryData.length > 1 ? 3 : 0;
                                                const dashLen = (pct * circumference) - gap;
                                                const dashGap = circumference - dashLen;

                                                // Calculate midpoint angle for label positioning
                                                const midAngle = offsetAngle + sliceAngle / 2;
                                                const midRad = (midAngle * Math.PI) / 180;

                                                // Label position (outside the donut) - pushed further out
                                                const labelR = r + strokeW / 2 + 60;
                                                const labelX = cx + labelR * Math.cos(midRad);
                                                const labelY = cy + labelR * Math.sin(midRad);

                                                // Connector line: from edge of donut to label
                                                const lineStartR = r + strokeW / 2 + 4;
                                                const lineStartX = cx + lineStartR * Math.cos(midRad);
                                                const lineStartY = cy + lineStartR * Math.sin(midRad);
                                                const lineEndR = r + strokeW / 2 + 30;
                                                const lineEndX = cx + lineEndR * Math.cos(midRad);
                                                const lineEndY = cy + lineEndR * Math.sin(midRad);

                                                const strokeOffset = -(offsetAngle / 360) * circumference;
                                                const color = colors[i % colors.length];

                                                const el = (
                                                    <g key={cat.name}>
                                                        {/* Donut slice */}
                                                        <circle
                                                            cx={cx}
                                                            cy={cy}
                                                            r={r}
                                                            fill="none"
                                                            stroke={color}
                                                            strokeWidth={strokeW}
                                                            strokeDasharray={`${Math.max(dashLen, 0)} ${Math.max(dashGap, 0)}`}
                                                            strokeDashoffset={strokeOffset}
                                                            strokeLinecap="butt"
                                                            style={{ transition: "stroke-dasharray 0.8s ease" }}
                                                        />
                                                        {/* Dashed connector line */}
                                                        {pctDisplay >= 3 && (
                                                            <line
                                                                x1={lineStartX}
                                                                y1={lineStartY}
                                                                x2={lineEndX}
                                                                y2={lineEndY}
                                                                stroke={color}
                                                                strokeWidth="1.5"
                                                                strokeDasharray="4 3"
                                                                opacity="0.7"
                                                            />
                                                        )}
                                                        {/* Outside label: icon + percentage + name */}
                                                        {pctDisplay >= 3 && (
                                                            <g>
                                                                <text
                                                                    x={labelX}
                                                                    y={labelY - 6}
                                                                    textAnchor="middle"
                                                                    fill="var(--text-primary)"
                                                                    fontSize="13"
                                                                    fontWeight="700"
                                                                >
                                                                    {cat.icon} {pctDisplay}%
                                                                </text>
                                                                <text
                                                                    x={labelX}
                                                                    y={labelY + 10}
                                                                    textAnchor="middle"
                                                                    fill="var(--text-secondary)"
                                                                    fontSize="10"
                                                                >
                                                                    {cat.name.length > 12 ? cat.name.slice(0, 12) + "…" : cat.name}
                                                                </text>
                                                            </g>
                                                        )}
                                                    </g>
                                                );
                                                offsetAngle += sliceAngle;
                                                return el;
                                            });
                                        })()}
                                    </svg>
                                    {/* Center text */}
                                    <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                                        <p className="text-xs text-[var(--text-muted)]">Tổng chi</p>
                                        <p className="text-lg font-bold">{formatCurrency(totalExpense)}</p>
                                    </div>
                                </div>
                            </div>

                            {/* Detail legend list */}
                            <div className="space-y-2 mt-2">
                                {categoryData.map((cat, i) => (
                                    <div key={cat.name} className="flex items-center gap-3 px-2 py-1.5 rounded-lg hover:bg-[var(--bg-card-hover)] transition-colors">
                                        <div
                                            className="w-3 h-3 rounded-full shrink-0"
                                            style={{ backgroundColor: colors[i % colors.length] }}
                                        />
                                        <span className="text-sm flex-1 truncate">
                                            {cat.icon} {cat.name}
                                        </span>
                                        <span className="text-sm font-medium">
                                            {formatCurrency(cat.amount)}
                                        </span>
                                        <span className="text-xs text-[var(--text-muted)] w-10 text-right">
                                            {((cat.amount / totalExpense) * 100).toFixed(0)}%
                                        </span>
                                    </div>
                                ))}
                            </div>
                        </>
                    )}
                </div>

                {/* Daily bar chart */}
                <div className="glass rounded-2xl p-6">
                    <div className="flex items-center gap-2 mb-5">
                        <BarChart3 className="w-5 h-5 text-[var(--accent)]" />
                        <h2 className="font-semibold">Chi tiêu theo ngày</h2>
                    </div>

                    {dailyData.length === 0 ? (
                        <div className="text-center py-8 text-[var(--text-muted)]">
                            Chưa có dữ liệu
                        </div>
                    ) : (
                        <div className="space-y-2">
                            {dailyData.map((d) => (
                                <div key={d.day} className="flex items-center gap-3">
                                    <span className="text-xs text-[var(--text-muted)] w-8 text-right shrink-0">
                                        {d.day}
                                    </span>
                                    <div className="flex-1 flex flex-col gap-1">
                                        {d.income > 0 && (
                                            <div className="flex items-center gap-2">
                                                <div
                                                    className="h-4 rounded-r-md bg-[var(--success)] transition-all duration-500"
                                                    style={{
                                                        width: `${(d.income / maxDailyAmount) * 100}%`,
                                                        minWidth: "4px",
                                                    }}
                                                />
                                                <span className="text-xs text-[var(--success)]">
                                                    {formatCurrency(d.income)}
                                                </span>
                                            </div>
                                        )}
                                        {d.expense > 0 && (
                                            <div className="flex items-center gap-2">
                                                <div
                                                    className="h-4 rounded-r-md bg-[var(--danger)] transition-all duration-500"
                                                    style={{
                                                        width: `${(d.expense / maxDailyAmount) * 100}%`,
                                                        minWidth: "4px",
                                                    }}
                                                />
                                                <span className="text-xs text-[var(--danger)]">
                                                    {formatCurrency(d.expense)}
                                                </span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}
