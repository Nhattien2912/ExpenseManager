"use client";

import { useState, useEffect } from "react";
import {
    collection,
    query,
    orderBy,
    onSnapshot,
    DocumentData,
    addDoc,
    deleteDoc,
    updateDoc,
    doc,
    serverTimestamp,
} from "firebase/firestore";
import { db } from "@/lib/firebase";
import { useAuth } from "@/context/AuthContext";

export interface Transaction {
    id: string;
    amount: number;
    note: string;
    date: number;
    type: string;
    categoryName: string;
    categoryIcon: string;
    walletId: number;
    paymentMethod: string;
}

export interface Category {
    id: string;
    name: string;
    icon: string;
    type: string;
}

export interface Wallet {
    id: string;
    name: string;
    initialBalance: number;
    icon: string;
    color: number;
}

function resolveType(raw: any): string {
    // Handle multiple formats:
    // 1. String: "EXPENSE" or "INCOME"
    // 2. Enum object: { name: "EXPENSE" } or just the enum name
    if (typeof raw === "string") return raw;
    if (raw && typeof raw === "object" && raw.name) return raw.name;
    return "EXPENSE";
}

function mapTransaction(doc: DocumentData, id: string): Transaction {
    return {
        id,
        amount: doc.amount || 0,
        note: doc.note || "",
        date: doc.date || 0,
        type: resolveType(doc.type),
        categoryName: doc.categoryName || doc.categoryId?.toString() || "Khác",
        categoryIcon: doc.categoryIcon || "💰",
        walletId: doc.walletId || 1,
        paymentMethod: doc.paymentMethod || "CASH",
    };
}

export function useTransactions() {
    const { user } = useAuth();
    const [transactions, setTransactions] = useState<Transaction[]>([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        if (!user) {
            setTransactions([]);
            setLoading(false);
            return;
        }

        const q = query(
            collection(db, `users/${user.uid}/transactions`),
            orderBy("date", "desc")
        );

        const unsubscribe = onSnapshot(
            q,
            (snapshot) => {
                const data = snapshot.docs.map((doc) =>
                    mapTransaction(doc.data(), doc.id)
                );
                setTransactions(data);
                setLoading(false);
            },
            (error) => {
                console.error("Firestore error:", error);
                setLoading(false);
            }
        );

        return () => unsubscribe();
    }, [user]);

    return { transactions, loading };
}

export function useCategories() {
    const { user } = useAuth();
    const [categories, setCategories] = useState<Category[]>([]);

    useEffect(() => {
        if (!user) return;

        const q = collection(db, `users/${user.uid}/categories`);
        const unsubscribe = onSnapshot(q, (snapshot) => {
            setCategories(
                snapshot.docs.map((doc) => {
                    const data = doc.data();
                    return {
                        id: doc.id,
                        name: data.name || "",
                        icon: data.icon || "💰",
                        type: resolveType(data.type),
                    };
                })
            );
        });

        return () => unsubscribe();
    }, [user]);

    return { categories };
}

export function useWallets() {
    const { user } = useAuth();
    const [wallets, setWallets] = useState<Wallet[]>([]);

    useEffect(() => {
        if (!user) return;

        const q = collection(db, `users/${user.uid}/wallets`);
        const unsubscribe = onSnapshot(q, (snapshot) => {
            setWallets(
                snapshot.docs.map((doc) => ({
                    id: doc.id,
                    ...(doc.data() as Omit<Wallet, "id">),
                }))
            );
        });

        return () => unsubscribe();
    }, [user]);

    return { wallets };
}

// --- WRITE FUNCTIONS ---

export async function firestoreAddTransaction(
    userId: string,
    data: {
        amount: number;
        type: string;
        categoryName: string;
        categoryIcon: string;
        categoryId: number;
        note: string;
        date: number;
        walletId: number;
        paymentMethod: string;
    }
) {
    const docRef = await addDoc(
        collection(db, `users/${userId}/transactions`),
        {
            ...data,
            isRecurring: false,
            debtId: null,
            targetWalletId: null,
        }
    );
    return docRef.id;
}

export async function firestoreDeleteTransaction(
    userId: string,
    transactionId: string
) {
    await deleteDoc(doc(db, `users/${userId}/transactions`, transactionId));
}

export async function firestoreUpdateTransaction(
    userId: string,
    transactionId: string,
    data: Partial<Transaction>
) {
    const { id, ...updateData } = data;
    await updateDoc(
        doc(db, `users/${userId}/transactions`, transactionId),
        updateData
    );
}

export async function firestoreAddCategory(
    userId: string,
    data: Omit<Category, "id">
) {
    const docRef = await addDoc(
        collection(db, `users/${userId}/categories`),
        data
    );
    return docRef.id;
}

export async function firestoreUpdateCategory(
    userId: string,
    categoryId: string,
    data: Partial<Category>
) {
    const { id, ...updateData } = data;
    await updateDoc(
        doc(db, `users/${userId}/categories`, categoryId),
        updateData
    );
}

export async function firestoreDeleteCategory(
    userId: string,
    categoryId: string
) {
    await deleteDoc(doc(db, `users/${userId}/categories`, categoryId));
}
