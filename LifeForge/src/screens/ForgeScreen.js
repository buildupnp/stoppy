import React, { useState, useEffect, useCallback, useRef } from 'react';
import { View, Text, StyleSheet, ScrollView, TouchableOpacity, Alert, Modal, ActivityIndicator, RefreshControl } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import ScreenWrapper from '../components/ScreenWrapper';
import GlassCard from '../components/GlassCard';
import GradientButton from '../components/GradientButton';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import { Camera as CameraIcon, Footprints, Info, Plus, Minus, X, Check, Zap, RotateCcw } from 'lucide-react-native';
import { useAuth } from '../context/AuthContext';
import { logPushupSession, logSteps, getTodayStats, syncDeviceSteps } from '../services/ActivityService';
import { COIN_RATES } from '../services/CoinsService';
import { Pedometer } from 'expo-sensors';
// Note: expo-face-detector requires a development build and doesn't work in Expo Go
// Camera AI mode will show a message to users about this limitation
const FACE_DETECTOR_AVAILABLE = false;

const ForgeScreen = ({ navigation }) => {
    const { user } = useAuth();
    const [selectedType, setSelectedType] = useState('pushups'); // 'pushups' | 'walking'
    const [showModal, setShowModal] = useState(false);
    const [count, setCount] = useState(10);
    const [loading, setLoading] = useState(false);
    const [refreshing, setRefreshing] = useState(false);
    const [result, setResult] = useState(null); // { count, coins }
    const [todaySteps, setTodaySteps] = useState(0);
    const [pushupMode, setPushupMode] = useState('manual'); // 'manual' | 'camera'
    // Camera State - Disabled in Expo Go (requires development build)
    const [permission, setPermission] = useState(null);
    const requestPermission = () => Alert.alert('Development Build Required', 'Camera AI mode requires a development build. Please use Manual mode instead.');
    const [calibratedWidth, setCalibratedWidth] = useState(null);
    const [isDown, setIsDown] = useState(false);
    const [debugFaceInfo, setDebugFaceInfo] = useState('');
    const lastWidthRef = useRef(0);

    // Light Sensor Logic (Shadow Mode)
    useEffect(() => {
        let subscription;
        if (showModal && pushupMode === 'camera') {
            // Reset camera
            setCalibratedWidth(null); setIsDown(false); setCount(0);

            /* (async () => {
                const available = await LightSensor.isAvailableAsync();
                if (available) {
                    LightSensor.setUpdateInterval(100);
                    subscription = LightSensor.addListener(({ illuminance: lux }) => {
                        setIlluminance(lux);
                        const state = sensorState.current;

                        // Auto-calibrate (Track max brightness as 'open' state)
                        // Ignore initial 0s or very low values if just starting
                        if (lux > state.base) {
                            state.base = lux;
                        }

                        // Thresholds: 
                        // DOWN: < 40% of base light (Shadow covers)
                        // UP: > 70% of base light (Shadow removed)
                        if (state.base > 10) { // Only run if we have some light
                            if (lux < state.base * 0.4 && !state.covered) {
                                state.covered = true;
                            } else if (state.covered && lux > state.base * 0.7) {
                                state.covered = false;
                                adjustCount(1);
                            }
                        }
                    });
                } else {
                    Alert.alert('Sensor Error', 'idk Light sensor not available.');
                    setPushupMode('manual');
                }
            })(); */
        }
        return () => subscription && subscription.remove();
    }, [showModal, pushupMode]);

    const handleFacesDetected = ({ faces }) => {
        if (faces.length === 0) {
            setDebugFaceInfo('No face');
            return;
        }

        const face = faces[0];
        const width = face.bounds.size.width;
        lastWidthRef.current = width;
        setDebugFaceInfo(`W:${Math.round(width)} | Cal:${calibratedWidth ? Math.round(calibratedWidth) : 'None'}`);

        if (!calibratedWidth) {
            return;
        }

        const DOWN_THRESHOLD = calibratedWidth * 1.35; // 35% larger (closer)
        const UP_THRESHOLD = calibratedWidth * 1.15;   // 15% larger

        if (width > DOWN_THRESHOLD && !isDown) {
            setIsDown(true);
        } else if (width < UP_THRESHOLD && isDown) {
            setIsDown(false);
            adjustCount(1);
        }
    };

    const calibrate = () => {
        if (lastWidthRef.current > 0) {
            setCalibratedWidth(lastWidthRef.current);
        } else {
            Alert.alert("No Face", "Ensure your face is visible in the camera.");
        }
    };

    // Initial load
    useEffect(() => {
        loadStats();
    }, [user, selectedType]);

    // Load today's stats
    const loadStats = async () => {
        if (!user) return;
        const { data } = await getTodayStats(user.id);
        if (data) {
            setTodaySteps(data.total_steps || 0);
        }
    };

    const onRefresh = useCallback(async () => {
        setRefreshing(true);
        if (selectedType === 'walking') {
            await handleSyncSteps();
        } else {
            await loadStats();
        }
        setRefreshing(false);
    }, [user, selectedType]);

    // Sync Pedometer
    const handleSyncSteps = async () => {
        setLoading(true);
        try {
            const isAvailable = await Pedometer.isAvailableAsync();
            if (!isAvailable) {
                Alert.alert('Not Available', 'Pedometer not available on this device');
                setLoading(false);
                return;
            }

            const perm = await Pedometer.requestPermissionsAsync();
            if (!perm.granted) {
                Alert.alert('Permission Denied', 'Step tracking permission is required');
                setLoading(false);
                return;
            }

            const end = new Date();
            const start = new Date();
            start.setHours(0, 0, 0, 0);

            // Get device count
            const { steps } = await Pedometer.getStepCountAsync(start, end);

            // Sync with backend
            const syncResult = await syncDeviceSteps(user.id, steps);

            if (syncResult && syncResult.error) {
                console.error(syncResult.error);
                Alert.alert('Error', 'Failed to sync steps');
            } else if (syncResult && syncResult.data) {
                // If coins were earned, show success
                if (syncResult.data.coinsEarned > 0) {
                    setResult({
                        count: syncResult.data.steps,
                        coins: syncResult.data.coinsEarned,
                        isSync: true
                    });
                    setShowModal(true);
                    setTimeout(() => {
                        setShowModal(false);
                        setResult(null);
                    }, 2500);
                }
            }

            // Reload stats to update display
            loadStats();

        } catch (error) {
            console.log('Sync err', error);
            Alert.alert('Error', 'Failed to sync steps');
        } finally {
            setLoading(false);
        }
    };

    // Increment/decrement count (Pushups only)
    const adjustCount = (delta) => {
        setCount(prev => Math.max(0, prev + delta));
    };

    // Submit pushup session
    const handleSubmitPushups = async () => {
        if (!user) return;

        setLoading(true);
        try {
            const result = await logPushupSession(user.id, count, 0);

            if (result.error) {
                Alert.alert('Error', result.error.message || 'Failed to log activity');
            } else {
                setResult({
                    count: result.data.count,
                    coins: result.data.coinsEarned,
                    isSync: false
                });
                // Close modal after showing result briefly
                setTimeout(() => {
                    setShowModal(false);
                    setResult(null);
                    setCount(10);
                }, 2000);
                loadStats();
            }
        } catch (error) {
            Alert.alert('Error', 'Failed to log activity');
        } finally {
            setLoading(false);
        }
    };

    // Open pushup modal
    const openModal = () => {
        if (selectedType === 'walking') {
            handleSyncSteps();
        } else {
            setCount(pushupMode === 'camera' ? 0 : 10);
            setResult(null);
            setShowModal(true);
        }
    };

    return (
        <ScreenWrapper>
            <ScrollView
                contentContainerStyle={styles.scrollContent}
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
                    <Text style={styles.title}>The Forge</Text>
                    <Text style={styles.subtitle}>Transform Effort into Value</Text>
                </View>

                {/* 1. Exercise Type Selector */}
                <GlassCard intensity={20} contentContainerStyle={{ padding: SPACING.s }}>
                    <View style={styles.selectorContainer}>
                        <TouchableOpacity
                            style={[
                                styles.selectorButton,
                                selectedType === 'pushups' && styles.selectorButtonActive
                            ]}
                            onPress={() => setSelectedType('pushups')}
                        >
                            <CameraIcon size={20} color={selectedType === 'pushups' ? COLORS.white : COLORS.textSecondary} />
                            <Text style={[
                                styles.selectorText,
                                selectedType === 'pushups' && styles.selectorTextActive
                            ]}>Push-ups</Text>
                        </TouchableOpacity>

                        <TouchableOpacity
                            style={[
                                styles.selectorButton,
                                selectedType === 'walking' && styles.selectorButtonActive
                            ]}
                            onPress={() => setSelectedType('walking')}
                        >
                            <Footprints size={20} color={selectedType === 'walking' ? COLORS.white : COLORS.textSecondary} />
                            <Text style={[
                                styles.selectorText,
                                selectedType === 'walking' && styles.selectorTextActive
                            ]}>Walking</Text>
                        </TouchableOpacity>
                    </View>
                </GlassCard>

                {/* 2. Activity Info Card */}
                <GlassCard intensity={15} style={{ marginTop: SPACING.m }}>
                    <View style={styles.infoContent}>
                        <View style={styles.iconCircle}>
                            {selectedType === 'pushups' ? (
                                <CameraIcon size={32} color={COLORS.accent} />
                            ) : (
                                <Footprints size={32} color={COLORS.accent} />
                            )}
                        </View>

                        <Text style={styles.activityTitle}>
                            {selectedType === 'pushups' ? 'Manual Entry' : 'Auto Tracker'}
                        </Text>

                        {selectedType === 'walking' ? (
                            <View style={styles.walkingStats}>
                                <Text style={styles.stepCountLarge}>{todaySteps.toLocaleString()}</Text>
                                <Text style={styles.stepLabel}>steps today</Text>
                                <View style={styles.coinBadge}>
                                    <Zap size={14} color={COLORS.accent} fill={COLORS.accent} />
                                    <Text style={styles.coinBadgeText}>
                                        {Math.floor(todaySteps / COIN_RATES.STEPS_TO_COINS)} LC Earned
                                    </Text>
                                </View>
                            </View>
                        ) : (
                            <Text style={styles.rateText}>
                                {`${COIN_RATES.PUSHUP_TO_COINS} Coin per 1 Push-up`}
                            </Text>
                        )}

                        <View style={styles.divider} />

                        <View style={styles.tipRow}>
                            <Info size={16} color={COLORS.textSecondary} style={{ marginRight: 8 }} />
                            <Text style={styles.tipText}>
                                {selectedType === 'pushups'
                                    ? 'Enter your push-up count manually.'
                                    : 'Steps are tracked automatically from your phone.'
                                }
                            </Text>
                        </View>
                    </View>
                </GlassCard>

                {/* 3. Action Button */}
                <View style={styles.actionContainer}>
                    <GradientButton
                        title={selectedType === 'pushups' ? 'Log Push-ups' : 'Sync Steps Now'}
                        onPress={openModal}
                        icon={selectedType === 'walking' ? RotateCcw : null}
                        disabled={loading}
                    >
                        {loading && selectedType === 'walking' && <ActivityIndicator color={COLORS.white} style={{ marginLeft: 10 }} />}
                    </GradientButton>
                </View>

                {/* Sub-Selector for Pushups */}
                {selectedType === 'pushups' && (
                    <View style={styles.subSelector}>
                        <TouchableOpacity
                            style={[styles.subOption, pushupMode === 'manual' && styles.subOptionActive]}
                            onPress={() => setPushupMode('manual')}
                        >
                            <Text style={[styles.subOptionText, pushupMode === 'manual' && styles.activeText]}>Manual Input</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={[styles.subOption, pushupMode === 'camera' && styles.subOptionActive]}
                            onPress={() => setPushupMode('camera')}
                        >
                            <Text style={[styles.subOptionText, pushupMode === 'camera' && styles.activeText]}>Camera AI</Text>
                        </TouchableOpacity>
                    </View>
                )}

            </ScrollView>

            {/* Modal (Pushups Entry or Success Message) */}
            <Modal
                visible={showModal}
                transparent
                animationType="fade"
                onRequestClose={() => !loading && setShowModal(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        {result ? (
                            // Success state
                            <View style={styles.successContent}>
                                <View style={styles.successIcon}>
                                    <Check size={40} color={COLORS.success} />
                                </View>
                                <Text style={styles.successTitle}>
                                    {result.isSync ? 'Steps Synced!' : 'Great Work! 💪'}
                                </Text>
                                <Text style={styles.successText}>
                                    {result.isSync
                                        ? `You've walked ${result.count} new steps`
                                        : `${result.count} Push-ups logged`
                                    }
                                </Text>
                                <View style={styles.coinsEarned}>
                                    <Zap size={24} color={COLORS.accent} fill={COLORS.accent} />
                                    <Text style={styles.coinsEarnedText}>+{result.coins} LC</Text>
                                </View>
                            </View>
                        ) : (
                            // Entry state (Only for Pushups)
                            selectedType === 'pushups' && (
                                <>
                                    <View style={styles.modalHeader}>
                                        <Text style={styles.modalTitle}>Enter Push-ups</Text>
                                        <TouchableOpacity
                                            style={styles.closeButton}
                                            onPress={() => setShowModal(false)}
                                            disabled={loading}
                                        >
                                            <X size={24} color={COLORS.textSecondary} />
                                        </TouchableOpacity>
                                    </View>

                                    {pushupMode === 'manual' ? (
                                        // Manual Counter
                                        <View style={styles.counterContainer}>
                                            <TouchableOpacity
                                                style={styles.counterButton}
                                                onPress={() => adjustCount(-5)}
                                                disabled={loading}
                                            >
                                                <Minus size={24} color={COLORS.white} />
                                            </TouchableOpacity>

                                            <View style={styles.countDisplay}>
                                                <Text style={styles.countNumber}>{count}</Text>
                                                <Text style={styles.countLabel}>push-ups</Text>
                                            </View>

                                            <TouchableOpacity
                                                style={styles.counterButton}
                                                onPress={() => adjustCount(5)}
                                                disabled={loading}
                                            >
                                                <Plus size={24} color={COLORS.white} />
                                            </TouchableOpacity>
                                        </View>
                                    ) : (
                                        // Camera AI Counter - Requires Development Build
                                        <View style={styles.cameraContainer}>
                                            <View style={styles.permContainer}>
                                                <View style={styles.devBuildIcon}>
                                                    <CameraIcon size={40} color={COLORS.textSecondary} />
                                                </View>
                                                <Text style={styles.permText}>
                                                    Camera AI mode requires a{'\n'}development build.
                                                </Text>
                                                <Text style={[styles.permText, { fontSize: 12, marginTop: 8 }]}>
                                                    Use Manual Input mode for now, or build the app with EAS Build to enable Camera AI.
                                                </Text>
                                                <GradientButton
                                                    title="Switch to Manual"
                                                    onPress={() => setPushupMode('manual')}
                                                    style={{ width: 200, marginTop: SPACING.m }}
                                                />
                                            </View>
                                        </View>
                                    )}

                                    <View style={styles.coinPreview}>
                                        <Zap size={18} color={COLORS.accent} fill={COLORS.accent} />
                                        <Text style={styles.coinPreviewText}>
                                            = {count * COIN_RATES.PUSHUP_TO_COINS} LC
                                        </Text>
                                    </View>

                                    <GradientButton
                                        title={loading ? '' : ((pushupMode === 'camera' && count > 0) ? 'Finish & Save' : 'Confirm')}
                                        onPress={handleSubmitPushups}
                                        disabled={loading || (pushupMode === 'camera' && count === 0)}
                                        style={{ marginTop: SPACING.l }}
                                    >
                                        {loading && <ActivityIndicator color={COLORS.white} />}
                                    </GradientButton>
                                </>
                            )
                        )}
                    </View>
                </View>
            </Modal>
        </ScreenWrapper>
    );
};

const styles = StyleSheet.create({
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

    // Selector
    selectorContainer: {
        flexDirection: 'row',
        backgroundColor: 'rgba(0,0,0,0.2)',
        borderRadius: SIZES.radius - 4,
        padding: 4,
    },
    selectorButton: {
        flex: 1,
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        paddingVertical: 12,
        borderRadius: SIZES.radius - 8,
    },
    selectorButtonActive: {
        backgroundColor: 'rgba(59, 130, 246, 0.2)',
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.3)',
    },
    selectorText: {
        marginLeft: 8,
        color: COLORS.textSecondary,
        fontFamily: FONTS.medium,
        fontSize: SIZES.font,
    },
    selectorTextActive: {
        color: COLORS.white,
        fontWeight: 'bold',
    },

    // Info Card
    infoContent: {
        alignItems: 'center',
        padding: SPACING.s,
    },
    iconCircle: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: SPACING.m,
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.2)',
        shadowColor: COLORS.accent,
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.3,
        shadowRadius: 10,
    },
    activityTitle: {
        fontSize: SIZES.large,
        color: COLORS.white,
        fontFamily: FONTS.bold,
        marginBottom: 4,
    },
    rateText: {
        fontSize: SIZES.medium,
        color: COLORS.accent,
        fontFamily: FONTS.medium,
        marginBottom: SPACING.m,
    },
    divider: {
        height: 1,
        width: '100%',
        backgroundColor: 'rgba(255,255,255,0.1)',
        marginVertical: SPACING.m,
    },
    tipRow: {
        flexDirection: 'row',
        alignItems: 'center',
    },
    tipText: {
        color: COLORS.textSecondary,
        fontSize: SIZES.small,
        fontStyle: 'italic',
    },

    // Walking Stats in Card
    walkingStats: {
        alignItems: 'center',
        marginBottom: SPACING.m,
    },
    stepCountLarge: {
        fontSize: 36,
        fontWeight: 'bold',
        color: COLORS.white,
        fontFamily: FONTS.bold,
    },
    stepLabel: {
        color: COLORS.textSecondary,
        fontSize: SIZES.font,
        marginBottom: SPACING.s,
    },
    coinBadge: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 100,
    },
    coinBadgeText: {
        color: COLORS.accent,
        fontWeight: 'bold',
        marginLeft: 4,
    },


    // Action
    actionContainer: {
        marginTop: SPACING.l,
    },

    // Modal
    modalOverlay: {
        flex: 1,
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: SPACING.l,
    },
    modalContent: {
        width: '100%',
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
    closeButton: {
        padding: SPACING.xs,
    },
    counterContainer: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        marginVertical: SPACING.l,
    },
    counterButton: {
        width: 56,
        height: 56,
        borderRadius: 28,
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
        fontSize: 48,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    countLabel: {
        color: COLORS.textSecondary,
        fontSize: SIZES.font,
    },
    coinPreview: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        paddingVertical: SPACING.s,
        paddingHorizontal: SPACING.m,
        borderRadius: SIZES.radius,
        alignSelf: 'center',
    },
    coinPreviewText: {
        color: COLORS.accent,
        fontSize: SIZES.medium,
        fontWeight: 'bold',
        marginLeft: SPACING.xs,
    },
    successContent: {
        alignItems: 'center',
        paddingVertical: SPACING.l,
    },
    successIcon: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: SPACING.m,
    },
    successTitle: {
        fontSize: SIZES.extraLarge,
        fontWeight: 'bold',
        color: COLORS.white,
        marginBottom: SPACING.xs,
    },
    successText: {
        color: COLORS.textSecondary,
        fontSize: SIZES.medium,
        marginBottom: SPACING.m,
        textAlign: 'center',
    },
    coinsEarned: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        paddingVertical: SPACING.s,
        paddingHorizontal: SPACING.l,
        borderRadius: 100,
    },
    coinsEarnedText: {
        color: COLORS.accent,
        fontSize: SIZES.large,
        fontWeight: 'bold',
        marginLeft: SPACING.xs,
    },

    // Sub Selector
    subSelector: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginTop: SPACING.m,
        backgroundColor: 'rgba(0,0,0,0.2)',
        borderRadius: 20,
        alignSelf: 'center',
        padding: 2,
    },
    subOption: {
        paddingVertical: 6,
        paddingHorizontal: 16,
        borderRadius: 18,
    },
    subOptionActive: {
        backgroundColor: COLORS.accent,
    },
    subOptionText: {
        color: COLORS.textSecondary,
        fontSize: 12,
        fontWeight: '500',
    },
    activeText: {
        color: COLORS.white,
        fontWeight: 'bold',
    },

    // Sensor Target
    sensorTarget: {
        width: 200,
        height: 200,
        borderRadius: 100,
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        borderWidth: 2,
        borderColor: COLORS.accent,
        borderStyle: 'dashed',
        justifyContent: 'center',
        alignItems: 'center',
        marginVertical: SPACING.l,
        alignSelf: 'center',
    },
    sensorInner: {
        width: 180,
        height: 180,
        borderRadius: 90,
        backgroundColor: 'rgba(59, 130, 246, 0.2)',
        justifyContent: 'center',
        alignItems: 'center',
    },
    sensorCount: {
        fontSize: 64,
        fontWeight: 'bold',
        color: COLORS.white,
    },
    sensorLabel: {
        color: COLORS.accent,
        marginTop: 4,
        fontWeight: 'bold',
        textTransform: 'uppercase',
        fontSize: 12,
    },
    debugText: {
        color: COLORS.textSecondary,
        fontSize: 10,
        textAlign: 'center',
        marginTop: 8,
    },
    sensorContainer: {
        alignItems: 'center',
    },
    // Camera Styles
    cameraContainer: {
        width: '100%',
        alignItems: 'center',
        marginVertical: 20,
    },
    camera: {
        width: 250,
        height: 250,
        borderRadius: 125,
        overflow: 'hidden',
        borderWidth: 2,
        borderColor: COLORS.accent,
    },
    cameraOverlay: {
        flex: 1,
        justifyContent: 'center',
        alignItems: 'center',
        backgroundColor: 'rgba(0,0,0,0.1)',
    },
    calibBtn: {
        backgroundColor: 'rgba(0,0,0,0.6)',
        padding: 10,
        borderRadius: 20,
    },
    calibText: {
        color: COLORS.white,
        fontWeight: 'bold',
    },
    statusBadge: {
        paddingVertical: 5,
        paddingHorizontal: 15,
        borderRadius: 20,
    },
    statusDown: {
        backgroundColor: COLORS.success,
    },
    statusUp: {
        backgroundColor: 'rgba(255,255,255,0.3)',
    },
    statusText: {
        color: COLORS.white,
        fontWeight: 'bold',
        fontSize: 18,
    },
    permContainer: {
        padding: 20,
        alignItems: 'center',
    },
    permText: {
        color: COLORS.textPrimary,
        marginBottom: 20,
        textAlign: 'center',
    },
    devBuildIcon: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: 'rgba(59, 130, 246, 0.1)',
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: SPACING.m,
        borderWidth: 1,
        borderColor: 'rgba(59, 130, 246, 0.2)',
    },
});

export default ForgeScreen;
