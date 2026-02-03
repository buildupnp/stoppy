import React, { createContext, useState, useEffect, useContext } from 'react';
import { supabase } from '../lib/supabase';

const AuthContext = createContext({});

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [session, setSession] = useState(null);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        // Check for existing session
        supabase.auth.getSession().then(({ data: { session } }) => {
            setSession(session);
            setUser(session?.user ?? null);
            setLoading(false);
        });

        // Listen for auth changes
        const { data: { subscription } } = supabase.auth.onAuthStateChange(
            async (event, session) => {
                setSession(session);
                setUser(session?.user ?? null);
                setLoading(false);
            }
        );

        return () => subscription.unsubscribe();
    }, []);

    // Sign Up with email/password
    const signUp = async (email, password, fullName) => {
        try {
            const { data, error } = await supabase.auth.signUp({
                email,
                password,
                options: {
                    data: {
                        full_name: fullName,
                    }
                }
            });
            if (error) throw error;

            // If signup successful and we have a user, create their profile
            if (data.user) {
                // Create profile
                await supabase.from('profiles').upsert({
                    id: data.user.id,
                    full_name: fullName,
                    created_at: new Date().toISOString(),
                });

                // Create initial coin balance (100 LC starting bonus)
                await supabase.from('coin_balances').upsert({
                    user_id: data.user.id,
                    balance: 100,
                    total_earned: 100,
                    total_spent: 0,
                });

                // Create default settings
                await supabase.from('user_settings').upsert({
                    user_id: data.user.id,
                    notifications_enabled: true,
                    haptic_enabled: true,
                    daily_step_goal: 10000,
                });
            }

            return { data, error: null };
        } catch (error) {
            return { data: null, error };
        }
    };

    // Sign In with email/password
    const signIn = async (email, password) => {
        try {
            const { data, error } = await supabase.auth.signInWithPassword({
                email,
                password,
            });
            if (error) throw error;
            return { data, error: null };
        } catch (error) {
            return { data: null, error };
        }
    };

    // Sign Out
    const signOut = async () => {
        try {
            const { error } = await supabase.auth.signOut();
            if (error) throw error;
        } catch (error) {
            console.error('Error signing out:', error);
        }
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                session,
                loading,
                signUp,
                signIn,
                signOut,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

// Custom hook to use auth context
export const useAuth = () => {
    const context = useContext(AuthContext);
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export default AuthContext;
