import { supabase } from '../lib/supabase';
import { spendCoins, COIN_RATES } from './CoinsService';

/**
 * Get all managed apps for a user
 */
export const getManagedApps = async (userId) => {
    try {
        const { data, error } = await supabase
            .from('managed_apps')
            .select('*')
            .eq('user_id', userId)
            .order('app_name', { ascending: true });

        if (error) throw error;
        return { data: data || [], error: null };
    } catch (error) {
        console.error('Error fetching managed apps:', error);
        return { data: [], error };
    }
};

/**
 * Add an app to the managed list
 */
export const addManagedApp = async (userId, appName, packageName = null, iconUrl = null, isBlocked = true) => {
    try {
        const { data, error } = await supabase
            .from('managed_apps')
            .insert({
                user_id: userId,
                app_name: appName,
                package_name: packageName,
                icon_url: iconUrl,
                is_blocked: isBlocked,
                always_allowed: false,
            })
            .select()
            .single();

        if (error) throw error;
        return { data, error: null };
    } catch (error) {
        console.error('Error adding managed app:', error);
        return { data: null, error };
    }
};

/**
 * Toggle app block status
 */
export const toggleAppBlock = async (appId, isBlocked) => {
    try {
        const { data, error } = await supabase
            .from('managed_apps')
            .update({ is_blocked: isBlocked })
            .eq('id', appId)
            .select()
            .single();

        if (error) throw error;
        return { data, error: null };
    } catch (error) {
        console.error('Error toggling app block:', error);
        return { data: null, error };
    }
};

/**
 * Remove app from managed list
 */
export const removeManagedApp = async (appId) => {
    try {
        const { error } = await supabase
            .from('managed_apps')
            .delete()
            .eq('id', appId);

        if (error) throw error;
        return { error: null };
    } catch (error) {
        console.error('Error removing managed app:', error);
        return { error };
    }
};

/**
 * Unlock an app by spending coins
 * @param {string} userId 
 * @param {string} appId 
 * @param {number} minutes - time to unlock
 */
export const unlockApp = async (userId, appId, appName, minutes) => {
    try {
        // Calculate coin cost (100 coins = 60 minutes)
        const coinCost = Math.ceil((minutes / 60) * COIN_RATES.COINS_PER_HOUR);

        // Spend coins first
        const { data: spendResult, error: spendError } = await spendCoins(userId, coinCost, appName, minutes);
        if (spendError) {
            return { data: null, error: spendError };
        }

        // Create unlock session
        const now = new Date();
        const expiresAt = new Date(now.getTime() + minutes * 60 * 1000);

        const { data, error } = await supabase
            .from('app_unlock_sessions')
            .insert({
                user_id: userId,
                app_id: appId,
                coins_spent: coinCost,
                minutes_granted: minutes,
                started_at: now.toISOString(),
                expires_at: expiresAt.toISOString(),
            })
            .select()
            .single();

        if (error) throw error;

        return {
            data: {
                ...data,
                coinsSpent: coinCost,
                expiresAt: expiresAt.toISOString(),
            },
            error: null
        };
    } catch (error) {
        console.error('Error unlocking app:', error);
        return { data: null, error };
    }
};

/**
 * Get active unlocks (not expired)
 */
export const getActiveUnlocks = async (userId) => {
    try {
        const now = new Date().toISOString();

        const { data, error } = await supabase
            .from('app_unlock_sessions')
            .select(`
                *,
                managed_apps (
                    id,
                    app_name,
                    package_name,
                    icon_url
                )
            `)
            .eq('user_id', userId)
            .gt('expires_at', now)
            .order('expires_at', { ascending: true });

        if (error) throw error;
        return { data: data || [], error: null };
    } catch (error) {
        console.error('Error fetching active unlocks:', error);
        return { data: [], error };
    }
};

/**
 * Check if an app is currently unlocked
 */
export const isAppUnlocked = async (userId, appId) => {
    try {
        const now = new Date().toISOString();

        const { data, error } = await supabase
            .from('app_unlock_sessions')
            .select('*')
            .eq('user_id', userId)
            .eq('app_id', appId)
            .gt('expires_at', now)
            .limit(1)
            .single();

        if (error && error.code !== 'PGRST116') throw error;

        return {
            data: {
                isUnlocked: !!data,
                expiresAt: data?.expires_at || null,
                minutesRemaining: data ? Math.max(0, Math.floor((new Date(data.expires_at) - new Date()) / 60000)) : 0,
            },
            error: null
        };
    } catch (error) {
        console.error('Error checking app unlock:', error);
        return { data: { isUnlocked: false, expiresAt: null, minutesRemaining: 0 }, error };
    }
};

/**
 * Trigger Emergency Unlock (All apps, 15 mins)
 * Logs the event for streak penalty calculation
 */
export const triggerEmergencyUnlock = async (userId) => {
    try {
        const now = new Date();
        const expiresAt = new Date(now.getTime() + 15 * 60 * 1000); // 15 mins

        // 1. Log Emergency
        const { error: logError } = await supabase
            .from('emergency_logs')
            .insert({
                user_id: userId,
                streak_lost: 1
            });

        if (logError) throw logError;

        // 2. Get all blocked apps
        const { data: apps } = await getManagedApps(userId);
        const blockedApps = apps.filter(a => a.is_blocked);

        // 3. Unlock them all
        if (blockedApps.length > 0) {
            const unlocks = blockedApps.map(app => ({
                user_id: userId,
                app_id: app.id,
                coins_spent: 0,
                minutes_granted: 15,
                started_at: now.toISOString(),
                expires_at: expiresAt.toISOString(),
            }));

            const { error: unlockError } = await supabase
                .from('app_unlock_sessions')
                .insert(unlocks);

            if (unlockError) throw unlockError;
        }

        return { success: true, count: blockedApps.length, error: null };
    } catch (error) {
        console.error('Error in emergency unlock:', error);
        return { success: false, error };
    }
};

// Common apps list with icons for quick add
export const COMMON_APPS = [
    { name: 'Instagram', package: 'com.instagram.android', icon: 'https://play-lh.googleusercontent.com/VRMWkE5p3CkWhJs6nv-9ZsLAs1QOg5ob1_3qg-rckwYW7yp1fMrYZqnEFpk0IoVP4LM=w240-h480-rw' },
    { name: 'TikTok', package: 'com.zhiliaoapp.musically', icon: 'https://play-lh.googleusercontent.com/Aw3rHJfMJOzOIKMx81GHiNXY57ObmLs9xqHVTkuqbBpZSIYCGWRl9VmchHPtpDMQ8wE=w240-h480-rw' },
    { name: 'YouTube', package: 'com.google.android.youtube', icon: 'https://play-lh.googleusercontent.com/lMoItBgdPPVDJsNOVtP26EKHePkwBg-PkuY9NOrc-fumRtTFP4XhpUNk_22syN4Datc=w240-h480-rw' },
    { name: 'Facebook', package: 'com.facebook.katana', icon: 'https://play-lh.googleusercontent.com/ccWDU4A7fX1R24v-vvT480ySh26AYp97g1VrIB_FIdjRcuQB2JP2WdY7h_wVVAeSpg=w240-h480-rw' },
    { name: 'Twitter/X', package: 'com.twitter.android', icon: 'https://play-lh.googleusercontent.com/A-Rnrh0J7iKmABskTonqFKyI-V4IZK3LxmHXqGb0iMD4E4CpyLwAL2yJwyGSBzTrfOI=w240-h480-rw' },
    { name: 'Snapchat', package: 'com.snapchat.android', icon: 'https://play-lh.googleusercontent.com/KxeSAjPTKliCErbivNiXrd6cTwfbqUJcbSRPe_IBVK_YmwckfMRS1VIHz-5cgT09yMo=w240-h480-rw' },
    { name: 'Reddit', package: 'com.reddit.frontpage', icon: 'https://play-lh.googleusercontent.com/nlwrrLeBakZ3Vm0_JukBy2P0MOTrWECIYCrQksNB_2DVtbnmC8wXs5oXyNrzq6yXVMU=w240-h480-rw' },
    { name: 'Netflix', package: 'com.netflix.mediaclient', icon: 'https://play-lh.googleusercontent.com/TBRwjS_qfJCSj1m7zZB93FnpJM5fSpMA_wUlFDLxWAb45T9RmwBvQd5cWR5viJJOhkI=w240-h480-rw' },
    { name: 'WhatsApp', package: 'com.whatsapp', icon: 'https://play-lh.googleusercontent.com/bYtqbOcTYOlgc6gqZ2rwb8lptHuwlNE75zYJu6Bn076-hTmvd96HH-6v7S0YUAAJXoJN=w240-h480-rw' },
    { name: 'Messenger', package: 'com.facebook.orca', icon: 'https://play-lh.googleusercontent.com/ldcQMpP7OaVmglCF6kGas9cY_K0PsJzSSosx2saw9KF1m3RHaEXpuCzpnAWaFDLOsw=w240-h480-rw' },
    { name: 'Discord', package: 'com.discord', icon: 'https://play-lh.googleusercontent.com/0oO5sAneb9lJP6l8c6DH4-uccBhPq5A9yNUjCwoSqre7bM1W1NrnlOjQ0qDwW4XWM8M=w240-h480-rw' },
    { name: 'Twitch', package: 'tv.twitch.android.app', icon: 'https://play-lh.googleusercontent.com/QLQzL-MXtxKEDlbhrQCDw-REiDsA9glUH4m16LHBvXdqxN2v2hXTAVTF5X6mUn12dho=w240-h480-rw' },
    { name: 'Pinterest', package: 'com.pinterest', icon: 'https://play-lh.googleusercontent.com/X9pWDW88eoyFEq5NwZz29hqF-5gGpIE4qdyv3DKwEDrDuwV7fLgM1lcWXpZxGCowsQ=w240-h480-rw' },
    { name: 'Spotify', package: 'com.spotify.music', icon: 'https://play-lh.googleusercontent.com/cShys-AmJ93dB0SV8kE6Fl5eSaf4-qMMZdwEDKI5VEmKAXfzOqbiaeAsqqrEBCTdIEs=w240-h480-rw' },
];

export default {
    getManagedApps,
    addManagedApp,
    toggleAppBlock,
    removeManagedApp,
    unlockApp,
    getActiveUnlocks,
    isAppUnlocked,
    triggerEmergencyUnlock,
    COMMON_APPS,
};
