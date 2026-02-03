import React, { useState, useEffect, useCallback } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Switch, Modal, Alert, ActivityIndicator, RefreshControl, Image } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import ScreenWrapper from '../components/ScreenWrapper';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import { Shield, Lock, Smartphone, Search, Clock, Plus, Minus, X, Zap, Unlock, Trash2, AlertOctagon } from 'lucide-react-native';
import { useAuth } from '../context/AuthContext';
import { getManagedApps, addManagedApp, toggleAppBlock, removeManagedApp, unlockApp, getActiveUnlocks, triggerEmergencyUnlock, COMMON_APPS } from '../services/AppsService';
import { getBalance, COIN_RATES } from '../services/CoinsService';

const GuardianScreen = () => {
    const { user } = useAuth();

    // State
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);
    const [apps, setApps] = useState([]);
    const [activeUnlocks, setActiveUnlocks] = useState([]);
    const [coinBalance, setCoinBalance] = useState(0);

    // Modals
    const [showAddModal, setShowAddModal] = useState(false);
    const [showUnlockModal, setShowUnlockModal] = useState(false);
    const [selectedApp, setSelectedApp] = useState(null);
    const [unlockMinutes, setUnlockMinutes] = useState(30);
    const [actionLoading, setActionLoading] = useState(false);

    // Load data
    const loadData = async () => {
        if (!user) return;

        try {
            const [appsResult, unlocksResult, balanceResult] = await Promise.all([
                getManagedApps(user.id),
                getActiveUnlocks(user.id),
                getBalance(user.id),
            ]);

            if (appsResult.data) setApps(appsResult.data);
            if (unlocksResult.data) setActiveUnlocks(unlocksResult.data);
            if (balanceResult.data) setCoinBalance(balanceResult.data.balance || 0);
        } catch (error) {
            console.error('Error loading guardian data:', error);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        loadData();
    }, [user]);

    const onRefresh = useCallback(() => {
        setRefreshing(true);
        loadData();
    }, [user]);

    // Get unlock status for an app
    const getAppUnlockStatus = (appId) => {
        const unlock = activeUnlocks.find(u => u.app_id === appId);
        if (!unlock) return { isUnlocked: false, timeLeft: null };

        const now = new Date();
        const expires = new Date(unlock.expires_at);
        const minutesLeft = Math.max(0, Math.floor((expires - now) / 60000));

        return {
            isUnlocked: minutesLeft > 0,
            timeLeft: minutesLeft > 60
                ? `${Math.floor(minutesLeft / 60)}h ${minutesLeft % 60}m`
                : `${minutesLeft}m`,
        };
    };

    // Toggle block
    const handleToggleBlock = async (app) => {
        const { data, error } = await toggleAppBlock(app.id, !app.is_blocked);
        if (!error && data) {
            setApps(apps.map(a => a.id === app.id ? data : a));
        }
    };

    // Add app
    const handleAddApp = async (appInfo) => {
        if (!user) return;

        setActionLoading(true);
        // Pass appInfo.icon to saving function
        const { data, error } = await addManagedApp(user.id, appInfo.name, appInfo.package, appInfo.icon, true);
        setActionLoading(false);

        if (error) {
            Alert.alert('Error', 'Failed to add app');
        } else {
            setApps([...apps, data]);
            setShowAddModal(false);
        }
    };

    // Remove app
    const handleRemoveApp = async (appId) => {
        Alert.alert(
            'Remove App',
            'Are you sure you want to remove this app from the list?',
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Remove',
                    style: 'destructive',
                    onPress: async () => {
                        const { error } = await removeManagedApp(appId);
                        if (!error) {
                            setApps(apps.filter(a => a.id !== appId));
                        }
                    }
                }
            ]
        );
    };

    // Open unlock modal
    const openUnlockModal = (app) => {
        setSelectedApp(app);
        setUnlockMinutes(30);
        setShowUnlockModal(true);
    };

    // Unlock app
    const handleUnlock = async () => {
        if (!user || !selectedApp) return;

        const coinCost = Math.ceil((unlockMinutes / 60) * COIN_RATES.COINS_PER_HOUR);

        if (coinBalance < coinCost) {
            Alert.alert('Insufficient Coins', `You need ${coinCost} LC but only have ${coinBalance} LC`);
            return;
        }

        setActionLoading(true);
        const { data, error } = await unlockApp(user.id, selectedApp.id, selectedApp.app_name, unlockMinutes);
        setActionLoading(false);

        if (error) {
            Alert.alert('Error', error.message || 'Failed to unlock app');
        } else {
            setCoinBalance(prev => prev - data.coinsSpent);
            setActiveUnlocks([...activeUnlocks, data]);
            setShowUnlockModal(false);
            Alert.alert('Success! 🎉', `${selectedApp.app_name} unlocked for ${unlockMinutes} minutes`);
        }
    };

    // Emergency Unlock
    const handleEmergencyUnlock = () => {
        Alert.alert(
            'Emergency Unlock 🚨',
            'This will unlock ALL apps for 15 minutes.\n\n⚠️ COST: Your Streak will determine reset to 0.',
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'RESET STREAK & UNLOCK',
                    style: 'destructive',
                    onPress: async () => {
                        setActionLoading(true);
                        const result = await triggerEmergencyUnlock(user.id);
                        setActionLoading(false);

                        if (result.success) {
                            Alert.alert('Unlocked', 'All blocked apps unlocked for 15m.');
                            loadData();
                        } else {
                            Alert.alert('Error', 'Failed to trigger emergency unlock');
                        }
                    }
                }
            ]
        );
    };

    // Stats
    const blockedCount = apps.filter(a => a.is_blocked).length;
    const unlockedCount = activeUnlocks.length;

    if (loading) {
        return (
            <ScreenWrapper>
                <View style={styles.loadingContainer}>
                    <ActivityIndicator size="large" color={COLORS.accent} />
                </View>
            </ScreenWrapper>
        );
    }

    return (
        <ScreenWrapper>
            <ScrollView
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
                refreshControl={
                    <RefreshControl
                        refreshing={refreshing}
                        onRefresh={onRefresh}
                        tintColor={COLORS.accent}
                    />
                }
            >
                {/* Header */}
                <View style={styles.header}>
                    <Text style={styles.title}>Guardian</Text>
                    <Text style={styles.subtitle}>Manage Access & Focus</Text>
                </View>

                {/* Summary Card */}
                <GlassCard intensity={20} glowColor={COLORS.alert} contentContainerStyle={styles.summaryContent}>
                    <View style={styles.summaryRow}>
                        <View style={styles.summaryItem}>
                            <Text style={styles.summaryValue}>{blockedCount}</Text>
                            <Text style={styles.summaryLabel}>Apps Blocked</Text>
                        </View>
                        <View style={styles.dividerVertical} />
                        <View style={styles.summaryItem}>
                            <Text style={styles.summaryValue}>{unlockedCount}</Text>
                            <Text style={styles.summaryLabel}>Currently Unlocked</Text>
                        </View>
                    </View>
                    <TouchableOpacity style={styles.addButton} onPress={() => setShowAddModal(true)}>
                        <Text style={styles.addButtonText}>+ Add Apps to Block</Text>
                    </TouchableOpacity>
                </GlassCard>

                {/* Coin Balance */}
                <View style={styles.balanceRow}>
                    <Zap size={16} color={COLORS.accent} fill={COLORS.accent} />
                    <Text style={styles.balanceText}>{coinBalance} LC available</Text>
                </View>

                {/* App List */}
                <Text style={styles.sectionTitle}>Managed Apps</Text>
                {apps.length === 0 ? (
                    <GlassCard intensity={15}>
                        <Text style={styles.emptyText}>No apps added yet. Tap "Add Apps to Block" to get started.</Text>
                    </GlassCard>
                ) : (
                    <GlassCard intensity={15} contentContainerStyle={{ padding: 0 }}>
                        {apps.map((app, index) => {
                            const unlockStatus = getAppUnlockStatus(app.id);
                            return (
                                <View key={app.id} style={[
                                    styles.appRow,
                                    index === apps.length - 1 && { borderBottomWidth: 0 }
                                ]}>
                                    <TouchableOpacity
                                        style={[
                                            styles.appIcon,
                                            app.is_blocked && !unlockStatus.isUnlocked
                                                ? { backgroundColor: 'rgba(239, 68, 68, 0.1)' }
                                                : { backgroundColor: 'rgba(16, 185, 129, 0.1)' }
                                        ]}
                                        onLongPress={() => handleRemoveApp(app.id)}
                                    >
                                        {app.icon_url ? (
                                            <Image
                                                source={{ uri: app.icon_url }}
                                                style={{ width: 24, height: 24, borderRadius: 4 }}
                                                resizeMode="cover"
                                            />
                                        ) : (
                                            app.is_blocked && !unlockStatus.isUnlocked
                                                ? <Lock size={18} color={COLORS.alert} />
                                                : <Smartphone size={18} color={COLORS.success} />
                                        )}
                                    </TouchableOpacity>

                                    <View style={styles.appInfo}>
                                        <Text style={styles.appName}>{app.app_name}</Text>
                                        <View style={styles.statusRow}>
                                            <View style={[
                                                styles.statusDot,
                                                { backgroundColor: app.is_blocked && !unlockStatus.isUnlocked ? COLORS.alert : COLORS.success }
                                            ]} />
                                            <Text style={[
                                                styles.appStatus,
                                                { color: app.is_blocked && !unlockStatus.isUnlocked ? COLORS.alert : COLORS.success }
                                            ]}>
                                                {unlockStatus.isUnlocked
                                                    ? `Unlocked: ${unlockStatus.timeLeft} left`
                                                    : (app.is_blocked ? 'Blocked' : 'Allowed')
                                                }
                                            </Text>
                                        </View>
                                    </View>

                                    {app.is_blocked && !unlockStatus.isUnlocked && (
                                        <TouchableOpacity
                                            style={styles.unlockButton}
                                            onPress={() => openUnlockModal(app)}
                                        >
                                            <Unlock size={14} color={COLORS.accent} />
                                        </TouchableOpacity>
                                    )}

                                    <Switch
                                        trackColor={{ false: "#767577", true: "rgba(239, 68, 68, 0.5)" }}
                                        thumbColor={app.is_blocked ? COLORS.alert : "#f4f3f4"}
                                        ios_backgroundColor="#3e3e3e"
                                        onValueChange={() => handleToggleBlock(app)}
                                        value={app.is_blocked}
                                    />
                                </View>
                            );
                        })}
                    </GlassCard>
                )}

                {/* Info Note */}
                <View style={styles.infoNote}>
                    <Shield size={16} color={COLORS.textSecondary} />
                    <Text style={styles.infoText}>Long press app icon to remove from list.</Text>
                </View>

                {/* Emergency Unlock Button */}
                <TouchableOpacity
                    style={styles.emergencyKey}
                    onPress={handleEmergencyUnlock}
                    disabled={actionLoading}
                >
                    <AlertOctagon size={20} color={COLORS.alert} />
                    <Text style={styles.emergencyText}>Emergency Override</Text>
                </TouchableOpacity>

            </ScrollView>

            {/* Add App Modal */}
            <Modal visible={showAddModal} transparent animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Add App to Block</Text>
                            <TouchableOpacity onPress={() => setShowAddModal(false)}>
                                <X size={24} color={COLORS.textSecondary} />
                            </TouchableOpacity>
                        </View>

                        <ScrollView style={styles.appList}>
                            {COMMON_APPS.filter(ca => !apps.some(a => a.package_name === ca.package)).map((appInfo, idx) => (
                                <TouchableOpacity
                                    key={idx}
                                    style={styles.appOption}
                                    onPress={() => handleAddApp(appInfo)}
                                    disabled={actionLoading}
                                >
                                    {appInfo.icon ? (
                                        <Image
                                            source={{ uri: appInfo.icon }}
                                            style={{ width: 30, height: 30, borderRadius: 6, marginRight: SPACING.m }}
                                        />
                                    ) : (
                                        <Smartphone size={20} color={COLORS.textSecondary} style={{ marginRight: SPACING.m }} />
                                    )}
                                    <Text style={styles.appOptionText}>{appInfo.name}</Text>
                                </TouchableOpacity>
                            ))}
                        </ScrollView>
                    </View>
                </View>
            </Modal>

            {/* Unlock Modal */}
            <Modal visible={showUnlockModal} transparent animationType="fade">
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.modalHeader}>
                            <Text style={styles.modalTitle}>Unlock {selectedApp?.app_name}</Text>
                            <TouchableOpacity onPress={() => setShowUnlockModal(false)} disabled={actionLoading}>
                                <X size={24} color={COLORS.textSecondary} />
                            </TouchableOpacity>
                        </View>

                        <View style={styles.counterContainer}>
                            <TouchableOpacity
                                style={styles.counterButton}
                                onPress={() => setUnlockMinutes(prev => Math.max(15, prev - 15))}
                            >
                                <Minus size={24} color={COLORS.white} />
                            </TouchableOpacity>

                            <View style={styles.countDisplay}>
                                <Text style={styles.countNumber}>{unlockMinutes}</Text>
                                <Text style={styles.countLabel}>minutes</Text>
                            </View>

                            <TouchableOpacity
                                style={styles.counterButton}
                                onPress={() => setUnlockMinutes(prev => prev + 15)}
                            >
                                <Plus size={24} color={COLORS.white} />
                            </TouchableOpacity>
                        </View>

                        <View style={styles.costPreview}>
                            <Zap size={18} color={COLORS.accent} fill={COLORS.accent} />
                            <Text style={styles.costText}>
                                Cost: {Math.ceil((unlockMinutes / 60) * COIN_RATES.COINS_PER_HOUR)} LC
                            </Text>
                        </View>

                        <Text style={styles.balanceHint}>
                            Your balance: {coinBalance} LC
                        </Text>

                        <GradientButton
                            title={actionLoading ? '' : 'Unlock Now'}
                            onPress={handleUnlock}
                            disabled={actionLoading}
                            style={{ marginTop: SPACING.l }}
                        >
                            {actionLoading && <ActivityIndicator color={COLORS.white} />}
                        </GradientButton>
                    </View>
                </View>
            </Modal>
        </ScreenWrapper>
    );
};

const styles = StyleSheet.create({
    loadingContainer: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
    },
    scrollContent: {
        padding: SIZES.padding,
        paddingBottom: 100,
    },
    header: {
        marginBottom: SPACING.l,
        marginTop: SPACING.s,
    },
    title: {
        fontSize: SIZES.extraLarge,
        fontFamily: FONTS.bold,
        color: COLORS.textPrimary,
    },
    subtitle: {
        fontSize: SIZES.font,
        color: COLORS.textSecondary,
        marginTop: 4,
    },
    summaryContent: {
        padding: SPACING.l,
    },
    summaryRow: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        alignItems: 'center',
        marginBottom: SPACING.m,
    },
    summaryItem: {
        alignItems: 'center',
    },
    summaryValue: {
        fontSize: 24,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    summaryLabel: {
        fontSize: 12,
        color: COLORS.textSecondary,
        marginTop: 4,
    },
    dividerVertical: {
        width: 1,
        height: 40,
        backgroundColor: 'rgba(255,255,255,0.1)',
    },
    addButton: {
        backgroundColor: 'rgba(255,255,255,0.05)',
        paddingVertical: 12,
        borderRadius: SIZES.radius,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.1)',
    },
    addButtonText: {
        color: COLORS.accent,
        fontWeight: 'bold',
        fontSize: SIZES.font,
    },
    balanceRow: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginVertical: SPACING.m,
    },
    balanceText: {
        color: COLORS.accent,
        fontWeight: 'bold',
        marginLeft: SPACING.xs,
    },
    sectionTitle: {
        fontSize: SIZES.medium,
        fontWeight: 'bold',
        color: COLORS.textPrimary,
        marginBottom: SPACING.m,
    },
    emptyText: {
        color: COLORS.textSecondary,
        textAlign: 'center',
        padding: SPACING.l,
    },
    appRow: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: SPACING.m,
        borderBottomWidth: 1,
        borderBottomColor: 'rgba(255,255,255,0.05)',
    },
    appIcon: {
        width: 40,
        height: 40,
        borderRadius: 10,
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
    statusRow: {
        flexDirection: 'row',
        alignItems: 'center',
        marginTop: 4,
    },
    statusDot: {
        width: 6,
        height: 6,
        borderRadius: 3,
        marginRight: 6,
    },
    appStatus: {
        fontSize: 12,
        fontWeight: '500',
    },
    unlockButton: {
        width: 32,
        height: 32,
        borderRadius: 16,
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: SPACING.s,
    },
    infoNote: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginTop: SPACING.l,
        opacity: 0.7,
    },
    infoText: {
        color: COLORS.textSecondary,
        fontSize: 12,
        marginLeft: 8,
    },
    // Modal styles
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: SPACING.l,
    },
    modalContent: {
        width: '100%',
        maxHeight: '70%',
        backgroundColor: COLORS.cardDark || '#1a1a2e',
        borderRadius: SIZES.radius,
        padding: SPACING.l,
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.3)',
    },
    modalHeader: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: SPACING.l,
    },
    modalTitle: {
        fontSize: SIZES.large,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    appList: {
        maxHeight: 300,
    },
    appOption: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: SPACING.m,
        borderRadius: SIZES.radius,
        marginBottom: SPACING.s,
        backgroundColor: 'rgba(255,255,255,0.05)',
    },
    appOptionText: {
        color: COLORS.textPrimary,
        marginLeft: SPACING.m,
        fontSize: SIZES.font,
    },
    counterContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginVertical: SPACING.l,
    },
    counterButton: {
        width: 50,
        height: 50,
        borderRadius: 25,
        backgroundColor: 'rgba(59, 130, 246, 0.2)',
        justifyContent: 'center',
        alignItems: 'center',
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.3)',
    },
    countDisplay: {
        alignItems: 'center',
        marginHorizontal: SPACING.xl,
    },
    countNumber: {
        fontSize: 42,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    countLabel: {
        color: COLORS.textSecondary,
        fontSize: SIZES.font,
    },
    costPreview: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: SPACING.s,
    },
    costText: {
        color: COLORS.accent,
        fontSize: SIZES.medium,
        fontWeight: 'bold',
        marginLeft: SPACING.xs,
    },
    balanceHint: {
        textAlign: 'center',
        color: COLORS.textSecondary,
        fontSize: SIZES.small,
    },
    emergencyKey: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginTop: SPACING.xl,
        padding: SPACING.m,
        borderRadius: SIZES.radius,
        borderWidth: 1,
        borderColor: COLORS.alert,
        backgroundColor: 'rgba(239, 68, 68, 0.05)',
        opacity: 0.8,
    },
    emergencyText: {
        color: COLORS.alert,
        fontWeight: 'bold',
        marginLeft: SPACING.s,
    },
});

export default GuardianScreen;
