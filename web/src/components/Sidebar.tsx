"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useAuth } from "@/context/AuthContext";
import { useTheme } from "@/context/ThemeContext";
import {
    LayoutDashboard,
    ArrowLeftRight,
    PieChart,
    Wallet,
    LogOut,
    Menu,
    X,
    FolderClock,
    PiggyBank,
    Sun,
    Moon,
} from "lucide-react";
import { useState } from "react";
import { cn } from "@/lib/utils";

const navItems = [
    { href: "/dashboard", label: "Tổng quan", icon: LayoutDashboard },
    { href: "/dashboard/transactions", label: "Giao dịch", icon: ArrowLeftRight },
    { href: "/dashboard/categories", label: "Danh mục", icon: FolderClock },
    { href: "/dashboard/debts", label: "Sổ nợ", icon: ArrowLeftRight },
    { href: "/dashboard/savings", label: "Tiết kiệm", icon: PiggyBank },
    { href: "/dashboard/charts", label: "Biểu đồ", icon: PieChart },
    { href: "/dashboard/wallets", label: "Ví tiền", icon: Wallet },
];

export default function Sidebar() {
    const pathname = usePathname();
    const { user, logout } = useAuth();
    const { theme, toggleTheme } = useTheme();
    const [mobileOpen, setMobileOpen] = useState(false);

    return (
        <>
            {/* Mobile toggle */}
            <button
                onClick={() => setMobileOpen(!mobileOpen)}
                className="lg:hidden fixed top-4 left-4 z-50 p-2 rounded-xl glass"
            >
                {mobileOpen ? <X className="w-5 h-5" /> : <Menu className="w-5 h-5" />}
            </button>

            {/* Overlay */}
            {mobileOpen && (
                <div
                    className="lg:hidden fixed inset-0 bg-black/50 z-30"
                    onClick={() => setMobileOpen(false)}
                />
            )}

            {/* Sidebar */}
            <aside
                className={cn(
                    "fixed top-0 left-0 h-full w-[var(--sidebar-width)] glass-strong z-40 flex flex-col transition-transform duration-300",
                    mobileOpen ? "translate-x-0" : "-translate-x-full lg:translate-x-0"
                )}
            >
                {/* Logo */}
                <div className="p-6 border-b border-[var(--border)]">
                    <div className="flex items-center gap-3">
                        <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-[var(--primary)] to-[var(--accent)] flex items-center justify-center">
                            <Wallet className="w-5 h-5 text-white" />
                        </div>
                        <div>
                            <h1 className="font-bold text-lg gradient-text">Expense</h1>
                            <p className="text-xs text-[var(--text-muted)]">Manager</p>
                        </div>
                    </div>
                </div>

                {/* Navigation */}
                <nav className="flex-1 p-4 space-y-1">
                    {navItems.map((item) => {
                        const isActive = pathname === item.href;
                        return (
                            <Link
                                key={item.href}
                                href={item.href}
                                onClick={() => setMobileOpen(false)}
                                className={cn(
                                    "flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200",
                                    isActive
                                        ? "bg-[var(--primary)]/10 text-[var(--primary-light)] font-medium"
                                        : "text-[var(--text-secondary)] hover:text-[var(--text-primary)] hover:bg-[var(--bg-card-hover)]"
                                )}
                            >
                                <item.icon className="w-5 h-5" />
                                {item.label}
                            </Link>
                        );
                    })}
                </nav>

                {/* User info */}
                <div className="p-4 border-t border-[var(--border)]">
                    <div className="flex items-center gap-3 px-3 py-2">
                        {user?.photoURL ? (
                            <img
                                src={user.photoURL}
                                alt="Avatar"
                                className="w-9 h-9 rounded-full ring-2 ring-[var(--primary)]/30"
                            />
                        ) : (
                            <div className="w-9 h-9 rounded-full bg-[var(--primary)]/20 flex items-center justify-center text-sm font-bold text-[var(--primary-light)]">
                                {user?.displayName?.[0] || "?"}
                            </div>
                        )}
                        <div className="flex-1 min-w-0">
                            <p className="text-sm font-medium truncate">
                                {user?.displayName || "User"}
                            </p>
                            <p className="text-xs text-[var(--text-muted)] truncate">
                                {user?.email}
                            </p>
                        </div>
                    </div>
                    <button
                        onClick={logout}
                        className="w-full mt-2 flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm text-[var(--text-secondary)] hover:text-[var(--danger)] hover:bg-[var(--danger)]/10 transition-all duration-200"
                    >
                        <LogOut className="w-4 h-4" />
                        Đăng xuất
                    </button>
                    <button
                        onClick={toggleTheme}
                        className="w-full mt-1 flex items-center gap-2 px-4 py-2.5 rounded-xl text-sm text-[var(--text-secondary)] hover:text-[var(--warning)] hover:bg-[var(--warning)]/10 transition-all duration-200"
                    >
                        {theme === "dark" ? <Sun className="w-4 h-4" /> : <Moon className="w-4 h-4" />}
                        {theme === "dark" ? "Chế độ sáng" : "Chế độ tối"}
                    </button>
                </div>
            </aside>
        </>
    );
}
