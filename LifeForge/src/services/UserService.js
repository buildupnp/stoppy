import { supabase } from '../lib/supabase';

/**
 * Get user profile
 */
export const getProfile = async (userId) => {
    try {
        const { data, error } = await supabase
            .from('profiles')
            .select('*')
            .eq('id', userId)
            .single();

        if (error && error.code !== 'PGRST116') throw error;

        if (!data) {
            return {
                data: {
                    id: userId,
                    full_name: 'User',
                },
                error: null
            };
        }
        return { data, error: null };
    } catch (error) {
        console.error('Error fetching profile:', error);
        return { data: null, error };
    }
};

/**
 * Update user profile
 */
export const updateProfile = async (userId, updates) => {
    try {
        const { data, error } = await supabase
            .from('profiles')
            .update({
                ...updates,
                updated_at: new Date().toISOString(),
            })
            .eq('id', userId)
            .select()
            .single();

        if (error) throw error;
        return { data, error: null };
    } catch (error) {
        console.error('Error updating profile:', error);
        return { data: null, error };
    }
};

/**
 * Get user settings
 */
export const getSettings = async (userId) => {
    try {
        const { data, error } = await supabase
            .from('user_settings')
            .select('*')
            .eq('user_id', userId)
            .single();

        if (error && error.code !== 'PGRST116') throw error;

        return {
            data: data || {
                notifications_enabled: true,
                haptic_enabled: true,
                daily_step_goal: 10000,
            },
            error: null
        };
    } catch (error) {
        console.error('Error fetching settings:', error);
        return { data: null, error };
    }
};

/**
 * Update user settings
 */
export const updateSettings = async (userId, settings) => {
    try {
        const { data, error } = await supabase
            .from('user_settings')
            .upsert({
                user_id: userId,
                ...settings,
                updated_at: new Date().toISOString(),
            })
            .select()
            .single();

        if (error) throw error;
        return { data, error: null };
    } catch (error) {
        console.error('Error updating settings:', error);
        return { data: null, error };
    }
};

export default {
    getProfile,
    updateProfile,
    getSettings,
    updateSettings,
};
