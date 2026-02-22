"use client";

import { useWallets } from "@/hooks/useFirestore";
import { formatCurrency } from "@/lib/utils";
import { Wallet, CreditCard } from "lucide-react";

export default function WalletsPage() {
    const { wallets } = useWallets();

    return (
        <div className="animate-fade-in max-w-4xl">
            <h1 className="text-2xl font-bold mb-6">Ví tiền</h1>

            {wallets.length === 0 ? (
                <div className="glass rounded-2xl p-12 text-center text-[var(--text-muted)]">
                    <Wallet className="w-12 h-12 mx-auto mb-4 opacity-30" />
                    <p className="text-lg mb-2">Chưa có ví nào</p>
                    <p className="text-sm">Tạo ví trên ứng dụng Android để đồng bộ tại đây</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    {wallets.map((w, i) => (
                        <div
                            key={w.id}
                            className="glass rounded-2xl p-6 hover:card-glow transition-all duration-300 animate-slide-up"
                            style={{ animationDelay: `${i * 0.1}s` }}
                        >
                            <div className="flex items-center gap-4">
                                <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-[var(--primary)] to-[var(--accent)] flex items-center justify-center">
                                    <CreditCard className="w-6 h-6 text-white" />
                                </div>
                                <div>
                                    <p className="font-semibold text-lg">{w.name}</p>
                                    <p className="text-[var(--text-muted)] text-sm">Số dư ban đầu</p>
                                </div>
                            </div>
                            <p className="text-2xl font-bold mt-4 gradient-text">
                                {formatCurrency(w.initialBalance)}
                            </p>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
