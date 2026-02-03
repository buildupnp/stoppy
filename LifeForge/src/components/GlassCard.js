import React from 'react';
import { View, StyleSheet, Platform } from 'react-native';
import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';
import { COLORS, SIZES, SPACING } from '../constants/theme';

const GlassCard = ({
    children,
    style,
    intensity = 20,
    glowColor = COLORS.borderGlow,
    contentContainerStyle
}) => {
    return (
        <View style={[styles.container, style]}>
            {/* Neon Glow */}
            <View style={[styles.glowLayer, { shadowColor: glowColor }]} />

            {/* Gradient Border */}
            <LinearGradient
                colors={[
                    COLORS.borderGlow,
                    'rgba(59, 130, 246, 0.15)',
                    'rgba(59, 130, 246, 0.05)',
                    COLORS.borderGlow
                ]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.borderGradient}
            >
                {/* Glass Content */}
                <BlurView
                    intensity={Platform.OS === 'ios' ? intensity : 50}
                    tint="dark"
                    style={styles.glassView}
                >
                    <View style={[styles.tintLayer, contentContainerStyle]}>
                        {children}
                    </View>
                </BlurView>
            </LinearGradient>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        marginBottom: SPACING.l,
        borderRadius: SIZES.radius,
    },
    glowLayer: {
        ...StyleSheet.absoluteFillObject,
        borderRadius: SIZES.radius,
        backgroundColor: 'transparent',
        shadowOffset: { width: 0, height: 4 },
        shadowOpacity: 0.6,
        shadowRadius: 16,
        elevation: 8,
    },
    borderGradient: {
        padding: 1,
        borderRadius: SIZES.radius,
    },
    glassView: {
        borderRadius: SIZES.radius - 1,
        overflow: 'hidden',
        backgroundColor: 'rgba(15, 23, 42, 0.6)',
    },
    tintLayer: {
        padding: SPACING.m,
        backgroundColor: 'rgba(59, 130, 246, 0.03)',
    }
});

export default GlassCard;
