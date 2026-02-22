"use client";

import { useAuth } from "@/context/AuthContext";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import Sidebar from "@/components/Sidebar";
import AddTransactionModal from "@/components/AddTransactionModal";
import { Plus } from "lucide-react";

export default function DashboardLayout({
    children,
}: {
    children: React.ReactNode;
}) {
    const { user, loading } = useAuth();
    const router = useRouter();
    const [showAddModal, setShowAddModal] = useState(false);

    useEffect(() => {
        if (!loading && !user) {
            router.push("/");
        }
    }, [user, loading, router]);

    if (loading) {
        return (
            <div className="min-h-screen flex items-center justify-center">
                <div className="w-8 h-8 border-2 border-[var(--primary)] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    if (!user) return null;

    return (
        <div className="min-h-screen">
            <Sidebar />
            <main className="lg:ml-[var(--sidebar-width)] p-6 lg:p-8 pb-24">
                {children}
            </main>

            {/* FAB - Add Transaction */}
            <button
                onClick={() => setShowAddModal(true)}
                className="fixed bottom-6 right-6 lg:bottom-8 lg:right-8 w-14 h-14 rounded-full bg-[var(--primary)] text-white shadow-xl shadow-[var(--primary)]/30 flex items-center justify-center hover:scale-110 hover:shadow-2xl hover:shadow-[var(--primary)]/40 transition-all duration-200 z-40"
            >
                <Plus className="w-6 h-6" />
            </button>

            {/* Add Transaction Modal */}
            <AddTransactionModal
                isOpen={showAddModal}
                onClose={() => setShowAddModal(false)}
            />
        </div>
    );
}
