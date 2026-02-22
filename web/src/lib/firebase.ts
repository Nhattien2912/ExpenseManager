import { initializeApp, getApps, getApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";

const firebaseConfig = {
    apiKey: "AIzaSyAxhR0Y_UNmDAfisOHPWZNjrITK5YMmk48",
    authDomain: "expensemanager-69017.firebaseapp.com",
    projectId: "expensemanager-69017",
    storageBucket: "expensemanager-69017.firebasestorage.app",
    messagingSenderId: "345037083240",
    appId: "1:345037083240:web:placeholder"
};

const app = !getApps().length ? initializeApp(firebaseConfig) : getApp();
const auth = getAuth(app);
const db = getFirestore(app);
const googleProvider = new GoogleAuthProvider();

export { app, auth, db, googleProvider };
