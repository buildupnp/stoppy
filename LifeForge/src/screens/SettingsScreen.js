
import React, { useState, useEffect } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Switch, Alert, ActivityIndicator } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import ScreenWrapper from '../components/ScreenWrapper';
import GlassCard from '../components/GlassCard';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import {
    User, Settings, Shield, Bell, Smartphone, Trash2,
    LogOut, ChevronRight, CheckCircle, AlertCircle
} from 'lucide-react-native';
import { useAuth } from '../context/AuthContext';
import { getProfile, getSettings, updateSettings } from '../services/UserService';

const SettingsScreen = ({ navigation }) => {
    const { user, signOut } = useAuth();

    // State
    const [loading, setLoading] = useState(true);
    const [signingOut, setSigningOut] = useState(false);
    const [notifications, setNotifications] = useState(true);
    const [haptics, setHaptics] = useState(true);
    const [profile, setProfile] = useState({ full_name: 'User', email: '' });

    // Load user data
    useEffect(() => {
        const loadData = async () => {
            if (!user) return;

            try {
                const [profileResult, settingsResult] = await Promise.all([
                    getProfile(user.id),
                    getSettings(user.id),
                ]);

                if (profileResult.data) {
                    setProfile({
                        full_name: profileResult.data.full_name || 'User',
                        email: user.email || '',
                    });
                } else {
                    setProfile({ full_name: 'User', email: user.email || '' });
                }

                if (settingsResult.data) {
                    setNotifications(settingsResult.data.notifications_enabled ?? true);
                    setHaptics(settingsResult.data.haptic_enabled ?? true);
                }
            } catch (error) {
                console.error('Error loading settings:', error);
            } finally {
                setLoading(false);
            }
        };

        loadData();
    }, [user]);

    // Handle toggle changes
    const handleNotificationsChange = async (value) => {
        setNotifications(value);
        if (user) {
            await updateSettings(user.id, { notifications_enabled: value });
        }
    };

    const handleHapticsChange = async (value) => {
        setHaptics(value);
        if (user) {
            await updateSettings(user.id, { haptic_enabled: value });
        }
    };

    // Handle sign out
    const handleSignOut = () => {
        Alert.alert(
            'Sign Out',
            'Are you sure you want to sign out?',
            [
                { text: 'Cancel', style: 'cancel' },
                {
                    text: 'Sign Out',
                    style: 'destructive',
                    onPress: async () => {
                        setSigningOut(true);
                        await signOut();
                    }
                }
            ]
        );
    };

    const renderPermissionItem = (label, granted) => (
        <View style={styles.permissionItem}>
            <View style={styles.permissionInfo}>
                <Text style={styles.permissionLabel}>{label}</Text>
            </View>
            {granted ? (
                <CheckCircle size={20} color={COLORS.success} />
            ) : (
                <TouchableOpacity style={styles.grantButton}>
                    <Text style={styles.grantText}>Grant</Text>
                </TouchableOpacity>
            )}
        </View>
    );

    const renderSettingItem = (label, icon, value, onToggle) => (
        <View style={styles.settingItem}>
            <View style={styles.settingLeft}>
                {icon}
                <Text style={styles.settingLabel}>{label}</Text>
            </View>
            <Switch
                trackColor={{ false: "#767577", true: "rgba(59, 130, 246, 0.5)" }}
                thumbColor={value ? COLORS.accent : "#f4f3f4"}
                ios_backgroundColor="#3e3e3e"
                onValueChange={onToggle}
                value={value}
            />
        </View>
    );

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
            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                {/* Header */}
                <View style={styles.header}>
                    <Text style={styles.title}>Settings</Text>
                    <Text style={styles.subtitle}>Preferences & Controls</Text>
                </View>

                {/* 1. Profile Section */}
                <GlassCard intensity={20} contentContainerStyle={styles.profileCard}>
                    <View style={styles.avatarCircle}>
                        <User size={32} color={COLORS.white} />
                    </View>
                    <View style={styles.profileInfo}>
                        <Text style={styles.profileName}>{profile.full_name}</Text>
                        <Text style={styles.profileEmail}>{profile.email}</Text>
                    </View>
                    <TouchableOpacity
                        style={styles.signOutButton}
                        onPress={handleSignOut}
                        disabled={signingOut}
                    >
                        {signingOut ? (
                            <ActivityIndicator size="small" color={COLORS.alert} />
                        ) : (
                            <LogOut size={20} color={COLORS.alert} />
                        )}
                    </TouchableOpacity>
                </GlassCard>

                {/* 2. Permissions */}
                <Text style={styles.sectionTitle}>Required Permissions</Text>
                <GlassCard intensity={15} contentContainerStyle={{ padding: 0 }}>
                    <View style={styles.listContainer}>
                        {renderPermissionItem('Usage Access', true)}
                        <View style={styles.divider} />
                        {renderPermissionItem('Overlay Permission', false)}
                        <View style={styles.divider} />
                        {renderPermissionItem('Physical Activity', true)}
                    </View>
                </GlassCard>

                {/* 3. Preferences */}
                <Text style={styles.sectionTitle}>Preferences</Text>
                <GlassCard intensity={15} contentContainerStyle={{ padding: 0 }}>
                    <View style={styles.listContainer}>
                        {renderSettingItem(
                            'Notifications',
                            <Bell size={18} color={COLORS.textSecondary} style={{ marginRight: 10 }} />,
                            notifications,
                            handleNotificationsChange
                        )}
                        <View style={styles.divider} />
                        {renderSettingItem(
                            'Haptic Feedback',
                            <Smartphone size={18} color={COLORS.textSecondary} style={{ marginRight: 10 }} />,
                            haptics,
                            handleHapticsChange
                        )}
                    </View>
                </GlassCard>

                {/* 4. Data & Danger Zone */}
                <Text style={styles.sectionTitle}>Data Management</Text>
                <GlassCard intensity={10} contentContainerStyle={{ padding: 0 }}>
                    <TouchableOpacity style={styles.dangerItem}>
                        <View style={styles.settingLeft}>
                            <Trash2 size={18} color={COLORS.alert} style={{ marginRight: 10 }} />
                            <Text style={[styles.settingLabel, { color: COLORS.alert }]}>Reset All Data</Text>
                        </View>
                        <ChevronRight size={18} color={COLORS.alert} />
                    </TouchableOpacity>
                </GlassCard>

                <Text style={styles.versionText}>LifeForge v1.0.0 (Beta)</Text>

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
    scrollContent: {
        padding: SIZES.padding,
        paddingBottom: 100, // Tab bar clearance
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

    // Profile
    profileCard: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: SPACING.m,
    },
    avatarCircle: {
        width: 60,
        height: 60,
        borderRadius: 30,
        backgroundColor: COLORS.accent,
        justifyContent: 'center',
        alignItems: 'center',
        marginRight: SPACING.m,
    },
    profileInfo: {
        flex: 1,
    },
    profileName: {
        fontSize: SIZES.large,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    profileEmail: {
        fontSize: SIZES.font,
        color: COLORS.textSecondary,
    },
    signOutButton: {
        padding: SPACING.s,
        backgroundColor: 'rgba(239, 68, 68, 0.1)',
        borderRadius: 8,
    },

    // Lists
    sectionTitle: {
        fontSize: SIZES.medium,
        fontWeight: 'bold',
        color: COLORS.textPrimary,
        marginBottom: SPACING.m,
        marginTop: SPACING.m,
    },
    listContainer: {
        width: '100%',
    },
    divider: {
        height: 1,
        backgroundColor: 'rgba(255,255,255,0.1)',
        marginLeft: SPACING.m,
    },

    // Permissions
    permissionItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: SPACING.m,
    },
    permissionLabel: {
        color: COLORS.textPrimary,
        fontSize: SIZES.font,
        fontWeight: '500',
    },
    grantButton: {
        backgroundColor: COLORS.accent,
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 6,
    },
    grantText: {
        color: COLORS.white,
        fontSize: 12,
        fontWeight: 'bold',
    },

    // Settings
    settingItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: SPACING.m,
    },
    settingLeft: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    settingLabel: {
        color: COLORS.textPrimary,
        fontSize: SIZES.font,
    },

    // Danger
    dangerItem: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        alignItems: 'center',
        padding: SPACING.m,
    },

    versionText: {
        textAlign: 'center',
        color: COLORS.textSecondary,
        fontSize: 12,
        marginTop: SPACING.xl,
        opacity: 0.5,
    }
});

export default SettingsScreen;
