import { supabase } from '../lib/supabase';

// Coin conversion rates
export const COIN_RATES = {
    PUSHUP_TO_COINS: 1,        // 1 push-up = 1 coin
    STEPS_TO_COINS: 100,       // 100 steps = 1 coin
    COINS_PER_HOUR: 100,       // 100 coins = 1 hour screen time
};

/**
 * Get user's current coin balance
 */
export const getBalance = async (userId) => {
    try {
        const { data, error } = await supabase
            .from('coin_balances')
            .select('balance, total_earned, total_spent')
            .eq('user_id', userId)
            .single();

        if (error) throw error;
        return { data, error: null };
    } catch (error) {
        console.error('Error fetching balance:', error);
        return { data: null, error };
    }
};

/**
 * Earn coins from activity
 * @param {string} userId 
 * @param {number} amount - coins to add
 * @param {string} source - 'pushups' | 'steps' | 'bonus'
 * @param {string} description - optional description
 */
export const earnCoins = async (userId, amount, source, description = '') => {
    try {
        // 1. Update balance
        const { data: currentBalance } = await getBalance(userId);
        const newBalance = (currentBalance?.balance || 0) + amount;
        const newTotalEarned = (currentBalance?.total_earned || 0) + amount;

        const { error: balanceError } = await supabase
            .from('coin_balances')
            .upsert({
                user_id: userId,
                balance: newBalance,
                total_earned: newTotalEarned,
                updated_at: new Date().toISOString(),
            });

        if (balanceError) throw balanceError;

        // 2. Log transaction
        const { error: txError } = await supabase
            .from('coin_transactions')
            .insert({
                user_id: userId,
                amount: amount,
                type: 'earn',
                source: source,
                description: description || `Earned ${amount} coins from ${source}`,
            });

        if (txError) throw txError;

        return { data: { newBalance }, error: null };
    } catch (error) {
        console.error('Error earning coins:', error);
        return { data: null, error };
    }
};

/**
 * Spend coins to unlock app
 * @param {string} userId 
 * @param {number} amount - coins to spend
 * @param {string} appName - app being unlocked
 * @param {number} minutes - time granted
 */
export const spendCoins = async (userId, amount, appName, minutes) => {
    try {
        // 1. Check sufficient balance
        const { data: currentBalance } = await getBalance(userId);
        if (!currentBalance || currentBalance.balance < amount) {
            return { data: null, error: { message: 'Insufficient coins' } };
        }

        // 2. Update balance
        const newBalance = currentBalance.balance - amount;
        const newTotalSpent = (currentBalance.total_spent || 0) + amount;

        const { error: balanceError } = await supabase
            .from('coin_balances')
            .update({
                balance: newBalance,
                total_spent: newTotalSpent,
                updated_at: new Date().toISOString(),
            })
            .eq('user_id', userId);

        if (balanceError) throw balanceError;

        // 3. Log transaction
        const { error: txError } = await supabase
            .from('coin_transactions')
            .insert({
                user_id: userId,
                amount: -amount,
                type: 'spend',
                source: 'app_unlock',
                description: `Unlocked ${appName} for ${minutes} minutes`,
            });

        if (txError) throw txError;

        return { data: { newBalance, minutesGranted: minutes }, error: null };
    } catch (error) {
        console.error('Error spending coins:', error);
        return { data: null, error };
    }
};

/**
 * Get transaction history
 */
export const getTransactionHistory = async (userId, limit = 20) => {
    try {
        const { data, error } = await supabase
            .from('coin_transactions')
            .select('*')
            .eq('user_id', userId)
            .order('created_at', { ascending: false })
            .limit(limit);

        if (error) throw error;
        return { data, error: null };
    } catch (error) {
        console.error('Error fetching transactions:', error);
        return { data: null, error };
    }
};

export default {
    COIN_RATES,
    getBalance,
    earnCoins,
    spendCoins,
    getTransactionHistory,
};
