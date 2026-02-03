
import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, Image, Dimensions } from 'react-native';
import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';
import GradientButton from '../components/GradientButton';
import GlassCard from '../components/GlassCard';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import { Lock, Smartphone, AlertOctagon, ArrowRight } from 'lucide-react-native';

const { width, height } = Dimensions.get('window');

// Mock content for preview - usually passed via route params
const MOCK_APP = { name: 'Instagram', icon: null };
const USER_BALANCE = 45;

const LockOverlayScreen = ({ navigation }) => {
    return (
        <View style={styles.container}>
            {/* 1. Background Blur Layer */}
            {/* In a real app, this would overlay the blocked app screenshot */}
            <LinearGradient
                colors={[COLORS.primaryGradientStart, COLORS.primaryGradientEnd]}
                style={StyleSheet.absoluteFillObject}
            />
            <View style={styles.glowSpot} />

            {/* Blur Intensity High for Lock Screen */}
            <BlurView intensity={80} tint="dark" style={StyleSheet.absoluteFill} />

            <View style={styles.content}>

                {/* 2. Top Lock Header */}
                <View style={styles.topSection}>
                    <View style={styles.lockIconCircle}>
                        <Lock size={32} color={COLORS.alert} />
                    </View>
                    <Text style={styles.appTitle}>{MOCK_APP.name}</Text>
                    <Text style={styles.lockedText}>is currently locked</Text>
                </View>

                {/* 3. Center Balance & Exchange */}
                <View style={styles.centerSection}>
                    <Text style={styles.balanceLabel}>YOUR BALANCE</Text>
                    <View style={styles.balanceRow}>
                        <Text style={styles.balanceValue}>{USER_BALANCE}</Text>
                        <Text style={styles.currency}>LC</Text>
                    </View>

                    <Text style={styles.instruction}>Spend coins to unlock time:</Text>

                    <View style={styles.unlockOptions}>
                        <TouchableOpacity style={styles.optionButton}>
                            <Text style={styles.optionTime}>15 min</Text>
                            <Text style={styles.optionCost}>10 LC</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={[styles.optionButton, styles.optionButtonPrimary]}>
                            <View style={styles.popularBadge}>
                                <Text style={styles.popularText}>BEST</Text>
                            </View>
                            <Text style={styles.optionTime}>30 min</Text>
                            <Text style={styles.optionCost}>18 LC</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={[styles.optionButton, styles.optionDisabled]}>
                            <Text style={styles.optionTime}>1 hour</Text>
                            <Text style={styles.optionCost}>30 LC</Text>
                        </TouchableOpacity>
                    </View>
                </View>

                {/* 4. Bottom Actions */}
                <View style={styles.bottomSection}>
                    <GradientButton
                        title="Earn More Coins"
                        onPress={() => navigation.navigate('MainTabs', { screen: 'Forge' })} // Go to Forge
                        style={{ marginBottom: SPACING.l }}
                    />

                    <TouchableOpacity style={styles.emergencyLink}>
                        <AlertOctagon size={16} color={COLORS.textSecondary} style={{ marginRight: 6 }} />
                        <Text style={styles.emergencyText}>Emergency Unlock</Text>
                    </TouchableOpacity>
                </View>

            </View>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: COLORS.primary, // Fallback
    },
    glowSpot: {
        position: 'absolute',
        top: 100,
        left: 50,
        width: 300,
        height: 300,
        borderRadius: 150,
        backgroundColor: COLORS.alert, // Red glow for lock
        opacity: 0.15,
        transform: [{ scale: 1.5 }],
    },
    content: {
        flex: 1,
        padding: SIZES.padding,
        justifyContent: 'space-between',
        paddingVertical: 60,
    },

    // Top
    topSection: {
        alignItems: 'center',
    },
    lockIconCircle: {
        width: 80,
        height: 80,
        borderRadius: 40,
        backgroundColor: 'rgba(239, 68, 68, 0.1)', // Red tint
        justifyContent: 'center',
        alignItems: 'center',
        marginBottom: SPACING.m,
        borderWidth: 1,
        borderColor: 'rgba(239, 68, 68, 0.3)',
    },
    appTitle: {
        fontSize: 32,
        fontWeight: 'bold',
        color: COLORS.white,
        marginBottom: 4,
    },
    lockedText: {
        fontSize: SIZES.font,
        color: COLORS.textSecondary,
        textTransform: 'uppercase',
        letterSpacing: 2,
    },

    // Center
    centerSection: {
        alignItems: 'center',
        width: '100%',
    },
    balanceLabel: {
        fontSize: 12,
        color: COLORS.textSecondary,
        letterSpacing: 1,
        marginBottom: 8,
    },
    balanceRow: {
        flexDirection: 'row',
        alignItems: 'baseline',
        marginBottom: SPACING.xl,
    },
    balanceValue: {
        fontSize: 56,
        fontWeight: '800',
        color: COLORS.white,
    },
    currency: {
        fontSize: 24,
        color: COLORS.accent,
        marginLeft: 8,
        fontWeight: '600',
    },
    instruction: {
        fontSize: SIZES.font,
        color: COLORS.textPrimary,
        marginBottom: SPACING.m,
    },
    unlockOptions: {
        flexDirection: 'row',
        justifyContent: 'space-between',
        width: '100%',
        gap: 12,
    },
    optionButton: {
        flex: 1,
        backgroundColor: 'rgba(255,255,255,0.05)',
        borderRadius: SIZES.radius,
        paddingVertical: SPACING.m,
        alignItems: 'center',
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.1)',
    },
    optionButtonPrimary: {
        backgroundColor: 'rgba(59, 130, 246, 0.15)',
        borderColor: COLORS.accent,
        transform: [{ scale: 1.05 }], // Slightly bigger
    },
    optionDisabled: {
        opacity: 0.5,
        backgroundColor: 'rgba(0,0,0,0.2)',
    },
    optionTime: {
        fontSize: SIZES.medium,
        fontWeight: 'bold',
        color: COLORS.white,
        marginBottom: 4,
    },
    optionCost: {
        fontSize: SIZES.small,
        color: COLORS.accent,
        fontWeight: '600',
    },
    popularBadge: {
        position: 'absolute',
        top: -10,
        backgroundColor: COLORS.accent,
        paddingHorizontal: 8,
        paddingVertical: 2,
        borderRadius: 4,
    },
    popularText: {
        fontSize: 8,
        fontWeight: 'bold',
        color: COLORS.white,
    },

    // Bottom
    bottomSection: {
        width: '100%',
    },
    emergencyLink: {
        flexDirection: 'row',
        justifyContent: 'center',
        alignItems: 'center',
    },
    emergencyText: {
        color: COLORS.textSecondary,
        fontSize: SIZES.small,
        textDecorationLine: 'underline',
    }
});

export default LockOverlayScreen;
