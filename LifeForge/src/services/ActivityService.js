import { supabase } from '../lib/supabase';
import { earnCoins, COIN_RATES } from './CoinsService';

/**
 * Log a push-up session and earn coins
 * @param {string} userId 
 * @param {number} count - number of push-ups
 * @param {number} durationSeconds - session duration
 */
export const logPushupSession = async (userId, count, durationSeconds = 0) => {
    try {
        const coinsEarned = count * COIN_RATES.PUSHUP_TO_COINS;
        const now = new Date();

        // 1. Log the activity session
        const { error: sessionError } = await supabase
            .from('activity_sessions')
            .insert({
                user_id: userId,
                type: 'pushups',
                count: count,
                coins_earned: coinsEarned,
                duration_seconds: durationSeconds,
                started_at: new Date(now.getTime() - durationSeconds * 1000).toISOString(),
                ended_at: now.toISOString(),
            });

        if (sessionError) throw sessionError;

        // 2. Update daily stats
        await updateDailyStats(userId, { pushups: count, coinsEarned });

        // 3. Earn the coins
        await earnCoins(userId, coinsEarned, 'pushups', `Completed ${count} push-ups`);

        return { data: { count, coinsEarned }, error: null };
    } catch (error) {
        console.error('Error logging pushup session:', error);
        return { data: null, error };
    }
};

/**
 * Log steps and earn coins
 * @param {string} userId 
 * @param {number} steps - number of steps
 */
export const logSteps = async (userId, steps) => {
    try {
        const coinsEarned = Math.floor(steps / COIN_RATES.STEPS_TO_COINS);
        const now = new Date();

        // 1. Log the activity session
        const { error: sessionError } = await supabase
            .from('activity_sessions')
            .insert({
                user_id: userId,
                type: 'walking',
                count: steps,
                coins_earned: coinsEarned,
                started_at: now.toISOString(),
                ended_at: now.toISOString(),
            });

        if (sessionError) throw sessionError;

        // 2. Update daily stats
        await updateDailyStats(userId, { steps, coinsEarned });

        // 3. Earn the coins (only if coins > 0)
        if (coinsEarned > 0) {
            await earnCoins(userId, coinsEarned, 'steps', `Walked ${steps} steps`);
        }

        return { data: { steps, coinsEarned }, error: null };
    } catch (error) {
        console.error('Error logging steps:', error);
        return { data: null, error };
    }
};

/**
 * Update daily stats (upsert)
 */
const updateDailyStats = async (userId, { steps = 0, pushups = 0, coinsEarned = 0, coinsSpent = 0 }) => {
    const today = new Date().toISOString().split('T')[0]; // YYYY-MM-DD

    try {
        // Get existing stats for today
        const { data: existing } = await supabase
            .from('daily_stats')
            .select('*')
            .eq('user_id', userId)
            .eq('date', today)
            .single();

        if (existing) {
            // Update existing
            await supabase
                .from('daily_stats')
                .update({
                    total_steps: (existing.total_steps || 0) + steps,
                    total_pushups: (existing.total_pushups || 0) + pushups,
                    coins_earned: (existing.coins_earned || 0) + coinsEarned,
                    coins_spent: (existing.coins_spent || 0) + coinsSpent,
                })
                .eq('id', existing.id);
        } else {
            // Insert new
            await supabase
                .from('daily_stats')
                .insert({
                    user_id: userId,
                    date: today,
                    total_steps: steps,
                    total_pushups: pushups,
                    coins_earned: coinsEarned,
                    coins_spent: coinsSpent,
                });
        }
    } catch (error) {
        console.error('Error updating daily stats:', error);
    }
};

/**
 * Get today's stats
 */
export const getTodayStats = async (userId) => {
    const today = new Date().toISOString().split('T')[0];

    try {
        const { data, error } = await supabase
            .from('daily_stats')
            .select('*')
            .eq('user_id', userId)
            .eq('date', today)
            .single();

        if (error && error.code !== 'PGRST116') throw error; // PGRST116 = no rows

        return {
            data: data || {
                total_steps: 0,
                total_pushups: 0,
                coins_earned: 0,
                coins_spent: 0,
            },
            error: null
        };
    } catch (error) {
        console.error('Error fetching today stats:', error);
        return { data: null, error };
    }
};

/**
 * Sync device steps with backend (Passive Sync)
 * @param {string} userId
 * @param {number} deviceSteps - total steps from device for today
 */
export const syncDeviceSteps = async (userId, deviceSteps) => {
    try {
        // 1. Get current stats from DB
        const { data: currentStats } = await getTodayStats(userId);
        const currentDbSteps = currentStats?.total_steps || 0;

        // 2. Calculate difference
        // If device has more steps than DB, we need to add the difference
        // If device has fewer (e.g. phone restart?), we keep DB (max strategy)
        if (deviceSteps > currentDbSteps) {
            const stepsToAdd = deviceSteps - currentDbSteps;
            console.log(`Syncing steps: Device=${deviceSteps}, DB=${currentDbSteps}, Adding=${stepsToAdd}`);

            // 3. Log the difference
            return await logSteps(userId, stepsToAdd);
        }

        return { data: { steps: 0, coinsEarned: 0 }, error: null };
    } catch (error) {
        console.error('Error syncing device steps:', error);
        return { data: null, error };
    }
};

/**
 * Calculate current streak (consecutive days with activity)
 */
export const calculateStreak = async (userId) => {
    try {
        // Check for recent emergency unlock (Penalty: Reset streak to 0)
        const twentyFourHoursAgo = new Date(Date.now() - 24 * 60 * 60 * 1000).toISOString();
        const { data: emergencyLogs, error: emergencyError } = await supabase
            .from('emergency_logs')
            .select('id')
            .eq('user_id', userId)
            .gt('created_at', twentyFourHoursAgo)
            .limit(1);

        if (emergencyLogs && emergencyLogs.length > 0) {
            return { data: 0, error: null };
        }

        const { data, error } = await supabase
            .from('daily_stats')
            .select('date')
            .eq('user_id', userId)
            .order('date', { ascending: false })
            .limit(30); // Check last 30 days

        if (error) throw error;
        if (!data || data.length === 0) return { data: 0, error: null };

        let streak = 0;
        const today = new Date();
        today.setHours(0, 0, 0, 0);

        for (let i = 0; i < data.length; i++) {
            const entryDate = new Date(data[i].date);
            entryDate.setHours(0, 0, 0, 0);

            const expectedDate = new Date(today);
            expectedDate.setDate(expectedDate.getDate() - i);

            // Allow for 1 day gap? No, strict streak.
            // Check if entry matches today (streak starts today) or previous days
            // Note: If user hasn't synced today yet, streak might look broken until they do.
            // Logic: Is this date equal to (Today - i days)?
            if (entryDate.toISOString().split('T')[0] === expectedDate.toISOString().split('T')[0]) {
                streak++;
            } else if (i === 0) {
                // If the first entry is NOT today, check if it was yesterday
                // If it was yesterday, streak is still valid but incomplete for today. 
                // If it was older than yesterday, streak is broken.
                const yesterday = new Date(today);
                yesterday.setDate(yesterday.getDate() - 1);

                if (entryDate.toISOString().split('T')[0] === yesterday.toISOString().split('T')[0]) {
                    streak++;
                    // Adjust index to skip today in expectation loop
                    // This simple loop assumes contiguous dates. Better logic needed for gaps? 
                    // For MVP, simple consecutive checks are fine.
                } else {
                    break;
                }
            } else {
                break;
            }
        }

        return { data: streak, error: null };
    } catch (error) {
        console.error('Error calculating streak:', error);
        return { data: 0, error };
    }
};

export default {
    logPushupSession,
    logSteps,
    getTodayStats,
    calculateStreak,
    syncDeviceSteps,
};
