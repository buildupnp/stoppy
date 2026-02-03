
import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView, Dimensions, TouchableOpacity, RefreshControl, ActivityIndicator, Image } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import ScreenWrapper from '../components/ScreenWrapper';
import GradientButton from '../components/GradientButton';
import GlassCard from '../components/GlassCard';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import { Play, Activity, Shield, Zap, TrendingUp, Lock, CheckCircle, Smartphone, Unlock } from 'lucide-react-native';
import { useAuth } from '../context/AuthContext';
import { getBalance } from '../services/CoinsService';
import { getTodayStats, calculateStreak, syncDeviceSteps } from '../services/ActivityService';
import { getProfile, getSettings } from '../services/UserService';
import { getActiveUnlocks } from '../services/AppsService';
import { Pedometer } from 'expo-sensors';

const { width } = Dimensions.get('window');

const DashboardScreen = ({ navigation }) => {
    const { user } = useAuth();

    // State for data
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [coinBalance, setCoinBalance] = useState(0);
    const [todayStats, setTodayStats] = useState({ total_steps: 0, total_pushups: 0 });
    const [streak, setStreak] = useState(0);
    const [userName, setUserName] = useState('User');
    const [stepGoal, setStepGoal] = useState(10000);
    const [activeUnlocks, setActiveUnlocks] = useState([]);

    // Get time-based greeting
    const getGreeting = () => {
        const hour = new Date().getHours();
        if (hour < 12) return 'Good Morning,';
        if (hour < 18) return 'Good Afternoon,';
        return 'Good Evening,';
    };

    // Calculate screen time from coins
    const getScreenTime = (coins) => {
        const hours = Math.floor(coins / 100);
        const minutes = Math.round((coins % 100) * 0.6);
        if (hours > 0) {
            return `${hours}h ${minutes}m`;
        }
        return `${minutes}m`;
    };

    // Fetch all dashboard data
    const fetchDashboardData = async () => {
        if (!user) return;

        try {
            // 0. Sync Pedometer Data first
            const isAvailable = await Pedometer.isAvailableAsync();
            if (isAvailable) {
                const perm = await Pedometer.requestPermissionsAsync();
                if (perm.granted) {
                    const end = new Date();
                    const start = new Date();
                    start.setHours(0, 0, 0, 0);
                    const { steps } = await Pedometer.getStepCountAsync(start, end);
                    if (steps > 0) {
                        await syncDeviceSteps(user.id, steps);
                    }
                }
            }

            // Fetch all data in parallel
            const [balanceResult, statsResult, streakResult, profileResult, settingsResult, unlocksResult] = await Promise.all([
                getBalance(user.id),
                getTodayStats(user.id),
                calculateStreak(user.id),
                getProfile(user.id),
                getSettings(user.id),
                getActiveUnlocks(user.id),
            ]);

            if (balanceResult.data) {
                setCoinBalance(balanceResult.data.balance || 0);
            }
            if (statsResult.data) {
                setTodayStats(statsResult.data);
            }
            if (streakResult.data !== undefined) {
                setStreak(streakResult.data);
            }
            if (profileResult.data?.full_name) {
                setUserName(profileResult.data.full_name.split(' ')[0]); // First name only
            }
            if (settingsResult.data?.daily_step_goal) {
                setStepGoal(settingsResult.data.daily_step_goal);
            }
            if (unlocksResult.data) {
                setActiveUnlocks(unlocksResult.data);
            }
        } catch (error) {
            console.error('Error fetching dashboard data:', error);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    // Initial load
    useEffect(() => {
        fetchDashboardData();
    }, [user]);

    // Pull to refresh
    const onRefresh = useCallback(() => {
        setRefreshing(true);
        fetchDashboardData();
    }, [user]);

    // Calculate progress percentage
    const stepsProgress = Math.min((todayStats.total_steps / stepGoal) * 100, 100);
    const coinsFromSteps = Math.floor(todayStats.total_steps / 100);

    if (loading) {
        return (
            <ScreenWrapper>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="large" color={COLORS.accent} />
                    <Text style={styles.loadingText}>Loading your forge...</Text>
                </View>
            </ScreenWrapper>
        );
    }

    return (
        <ScreenWrapper>
            {/* Background Mesh */}
            <View style={styles.backgroundMesh}>
                <LinearGradient
                    colors={[COLORS.primaryGradientStart, COLORS.primaryGradientEnd]}
                    style={StyleSheet.absoluteFillObject}
                />
                <View style={[styles.orb, { top: 50, left: -50, backgroundColor: COLORS.accentDark }]} />
                <View style={[styles.orb, { top: 300, right: -50, backgroundColor: 'rgba(59, 130, 246, 0.2)' }]} />
            </View>

            <ScrollView
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={onRefresh}
                        tintColor={COLORS.accent}
                        colors={[COLORS.accent]}
                    />
                }
            >

                {/* 1. Header & Status */}
                <View style={styles.header}>
                    <View>
                        <Text style={styles.greeting}>{getGreeting()}</Text>
                        <Text style={styles.username}>{userName}</Text>
                    </View>
                    <View style={styles.statusBadge}>
                        <View style={styles.statusDot} />
                        <Text style={styles.statusText}>Active</Text>
                    </View>
                </View>

                {/* 2. Primary Balance Display (Hero) */}
                <View style={styles.heroSection}>
                    <Text style={styles.balanceLabel}>CURRENT BALANCE</Text>
                    <View style={styles.balanceRow}>
                        <Text style={styles.heroBalance}>{coinBalance.toLocaleString()}</Text>
                        <Text style={styles.heroCurrency}>LC</Text>
                    </View>
                    <Text style={styles.heroSubtext}>≈ {getScreenTime(coinBalance)} screen time available</Text>
                </View>

                {/* 3. Daily Progress & Quick Stats */}
                <Text style={styles.sectionTitle}>Daily Progress</Text>
                <GlassCard intensity={20} contentContainerStyle={styles.progressCardContent}>
                    <View style={styles.progressHeader}>
                        <View style={styles.progressInfo}>
                            <Text style={styles.progressValue}>{todayStats.total_steps.toLocaleString()}</Text>
                            <Text style={styles.progressLabel}>steps today</Text>
                        </View>
                        <View style={styles.coinBadge}>
                            <Zap size={14} color={COLORS.accent} fill={COLORS.accent} />
                            <Text style={styles.coinBadgeText}>+{coinsFromSteps}</Text>
                        </View>
                    </View>
                    {/* Progress Bar */}
                    <View style={styles.progressBarBg}>
                        <LinearGradient
                            colors={[COLORS.accent, COLORS.accentGlow]}
                            start={{ x: 0, y: 0 }} end={{ x: 1, y: 0 }}
                            style={[styles.progressBarFill, { width: `${stepsProgress}%` }]}
                        />
                    </View>
                    <Text style={styles.goalText}>Goal: {stepGoal.toLocaleString()} steps</Text>

                    <View style={styles.divider} />

                    <View style={styles.quickStatsRow}>
                        <View style={styles.quickStatItem}>
                            <Activity size={16} color={COLORS.textSecondary} />
                            <Text style={styles.quickStatText}>{todayStats.total_pushups} Push-ups</Text>
                        </View>
                        <View style={styles.quickStatItem}>
                            <Text style={styles.fireIcon}>🔥</Text>
                            <Text style={styles.quickStatText}>{streak} day streak</Text>
                        </View>
                    </View>
                </GlassCard>

                {/* 4. Active Apps (Coin Spending) */}
                <Text style={styles.sectionTitle}>Unlocked Apps</Text>
                <GlassCard intensity={15} contentContainerStyle={{ padding: 0 }}>
                    {activeUnlocks.length === 0 ? (
                        <View style={styles.emptyAppsContainer}>
                            <Lock size={24} color={COLORS.textSecondary} />
                            <Text style={styles.emptyAppsText}>No apps currently unlocked</Text>
                            <TouchableOpacity
                                style={styles.goToGuardianButton}
                                onPress={() => navigation.navigate('Guardian')}
                            >
                                <Text style={styles.goToGuardianText}>Manage Apps →</Text>
                            </TouchableOpacity>
                        </View>
                    ) : (
                        activeUnlocks.map((unlock, index) => {
                            const now = new Date();
                            const expires = new Date(unlock.expires_at);
                            const minutesLeft = Math.max(0, Math.floor((expires - now) / 60000));
                            const timeDisplay = minutesLeft > 60
                                ? `${Math.floor(minutesLeft / 60)}h ${minutesLeft % 60}m left`
                                : `${minutesLeft}m left`;

                            return (
                                <View key={unlock.id} style={[
                                    styles.appRow,
                                    index === activeUnlocks.length - 1 && { borderBottomWidth: 0 }
                                ]}>
                                    <View style={styles.appIconPlaceholder}>
                                        {unlock.managed_apps?.icon_url ? (
                                            <Image
                                                source={{ uri: unlock.managed_apps.icon_url }}
                                                style={{ width: 20, height: 20, borderRadius: 4 }}
                                                resizeMode="cover"
                                            />
                                        ) : (
                                            <Unlock size={20} color={COLORS.success} />
                                        )}
                                    </View>
                                    <View style={styles.appInfo}>
                                        <Text style={styles.appName}>{unlock.managed_apps?.app_name || 'App'}</Text>
                                        <Text style={styles.appTimer}>{timeDisplay}</Text>
                                    </View>
                                    <TouchableOpacity
                                        style={styles.extendButton}
                                        onPress={() => navigation.navigate('Guardian')}
                                    >
                                        <Text style={styles.extendButtonText}>Extend</Text>
                                    </TouchableOpacity>
                                </View>
                            );
                        })
                    )}
                </GlassCard>


                {/* 5. Footer Actions */}
                <View style={styles.footerActions}>
                    <GradientButton
                        title="Start Exercise"
                        icon={Play}
                        style={{ marginBottom: SPACING.m }}
                        onPress={() => navigation.navigate('Forge')}
                    />
                    <TouchableOpacity
                        style={styles.secondaryButton}
                        onPress={() => navigation.navigate('Guardian')}
                    >
                        <Shield size={20} color={COLORS.textSecondary} style={{ marginRight: 8 }} />
                        <Text style={styles.secondaryButtonText}>View Blocked Apps</Text>
                    </TouchableOpacity>
                </View>

                {/* Bottom Padding for Tab Bar */}
                <View style={{ height: 100 }} />

            </ScrollView>
        </ScreenWrapper>
    );
};

const styles = StyleSheet.create({
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    loadingText: {
        color: COLORS.textSecondary,
        marginTop: SPACING.m,
        fontSize: SIZES.font,
    },
    backgroundMesh: {
        position: 'absolute',
        width: width,
        height: 900,
        top: 0,
        left: 0,
        zIndex: -1,
    },
    orb: {
        position: 'absolute',
        width: 300,
        height: 300,
        borderRadius: 150,
        opacity: 0.2,

    },
    scrollContent: {
        padding: SIZES.padding,
    },

    // Header
    header: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: SPACING.l,
        marginTop: SPACING.s,
    },
    greeting: {
        fontSize: SIZES.small,
        color: COLORS.textSecondary,
        fontFamily: FONTS.regular,
    },
    username: {
        fontSize: SIZES.medium,
        color: COLORS.textPrimary,
        fontFamily: FONTS.bold,
    },
    statusBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 12,
        borderWidth: 1,
        borderColor: 'rgba(16, 185, 129, 0.2)',
    },
    statusDot: {
        width: 6,
        height: 6,
        borderRadius: 3,
        backgroundColor: COLORS.success,
        marginRight: 6,
    },
    statusText: {
        color: COLORS.success,
        fontSize: 10,
        fontWeight: 'bold',
        textTransform: 'uppercase',
    },

    // Hero Balance
    heroSection: {
        alignItems: 'center',
        marginBottom: SPACING.xl,
    },
    balanceLabel: {
        color: COLORS.textSecondary,
        fontSize: 11,
        letterSpacing: 1.5,
        marginBottom: SPACING.xs,
    },
    balanceRow: {
        flexDirection: 'row',
        alignItems: 'baseline',
    },
    heroBalance: {
        fontSize: 64,
        color: COLORS.white,
        fontWeight: '800',
        letterSpacing: -2,
        textShadowColor: COLORS.accent,
        textShadowOffset: { width: 0, height: 0 },
        textShadowRadius: 20,
    },
    heroCurrency: {
        fontSize: 24,
        color: COLORS.accent,
        marginLeft: 8,
        fontWeight: '600',
    },
    heroSubtext: {
        color: COLORS.textSecondary,
        fontSize: SIZES.font,
        marginTop: SPACING.xs,
    },

    // Section Headers
    sectionTitle: {
        fontSize: SIZES.medium,
        fontWeight: 'bold',
        color: COLORS.textPrimary,
        marginBottom: SPACING.m,
        marginTop: SPACING.l,
    },

    // Progress Card
    progressCardContent: {
        padding: SPACING.l,
    },
    progressHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: SPACING.m,
    },
    progressValue: {
        fontSize: 28,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    progressLabel: {
        fontSize: SIZES.font,
        color: COLORS.textSecondary,
    },
    coinBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        paddingHorizontal: 8,
        paddingVertical: 4,
        borderRadius: 100,
    },
    coinBadgeText: {
        color: COLORS.accent,
        fontWeight: 'bold',
        fontSize: 12,
        marginLeft: 4,
    },
    progressBarBg: {
        height: 8,
        backgroundColor: 'rgba(255,255,255,0.1)',
        borderRadius: 4,
        marginBottom: 8,
        overflow: 'hidden',
    },
    progressBarFill: {
        height: '100%',
        borderRadius: 4,
    },
    goalText: {
        color: COLORS.textSecondary,
        fontSize: 12,
        textAlign: 'right',
    },
    divider: {
        height: 1,
        backgroundColor: 'rgba(255,255,255,0.1)',
        marginVertical: SPACING.m,
    },
    quickStatsRow: {
        flexDirection: 'row',
        justifyContent: 'space-around',
    },
    quickStatItem: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    quickStatText: {
        color: COLORS.textPrimary,
        marginLeft: 6,
        fontSize: 13,
        fontWeight: '500',
    },
    fireIcon: {
        fontSize: 14,
    },

    // Active Apps
    appRow: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: SPACING.m,
        borderBottomWidth: 1,
        borderBottomColor: 'rgba(255,255,255,0.05)',
    },
    appIconPlaceholder: {
        width: 40,
        height: 40,
        borderRadius: 10,
        backgroundColor: 'rgba(255,255,255,0.05)',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: SPACING.m,
    },
    appInfo: {
        flex: 1,
    },
    appName: {
        color: COLORS.textPrimary,
        fontWeight: '600',
        fontSize: SIZES.font,
    },
    appTimer: {
        color: COLORS.success, // Green for active time
        fontSize: 12,
        marginTop: 2,
        fontWeight: '500',
    },
    extendButton: {
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 8,
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.3)',
    },
    extendButtonText: {
        color: COLORS.accent,
        fontSize: 12,
        fontWeight: 'bold',
    },

    // Quick Unlock
    quickUnlockRow: {
        paddingBottom: SPACING.xl,
        marginBottom: SPACING.l,
    },
    quickUnlockItem: {
        alignItems: 'center',
        marginRight: SPACING.m,
        width: 60,
    },
    quickUnlockIcon: {
        width: 50,
        height: 50,
        borderRadius: 16,
        backgroundColor: 'rgba(30, 41, 59, 0.6)',
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.1)',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: 6,
    },
    quickUnlockText: {
        color: COLORS.textSecondary,
        fontSize: 10,
    },

    // Footer Actions
    footerActions: {
        marginTop: SPACING.s,
    },
    secondaryButton: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        paddingVertical: 12,
    },
    secondaryButtonText: {
        color: COLORS.textSecondary,
        fontSize: SIZES.font,
        fontWeight: '600',
    },

    // Empty Apps State
    emptyAppsContainer: {
        padding: SPACING.l,
        alignItems: 'center',
    },
    emptyAppsText: {
        color: COLORS.textSecondary,
        marginTop: SPACING.s,
        fontSize: SIZES.font,
    },
    goToGuardianButton: {
        marginTop: SPACING.m,
        padding: SPACING.s,
    },
    goToGuardianText: {
        color: COLORS.accent,
        fontWeight: 'bold',
    },

});

export default DashboardScreen;
