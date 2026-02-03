
import React from 'react';
import { View, StyleSheet } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { COLORS, SIZES, SPACING } from '../constants/theme';

const GlowCard = ({ children, style, glowColor = COLORS.accent }) => {
    return (
        <View style={[styles.wrapper, style]}>
            {/* Background Glow */}
            <View style={[styles.glow, { backgroundColor: glowColor }]} />

            {/* Border Gradient (Optional, here we just use surface) */}
            <View style={styles.card}>
                {children}
            </View>
        </View>
    );
};

const styles = StyleSheet.create({
    wrapper: {
        marginBottom: SPACING.l,
        position: 'relative',
    },
    glow: {
        position: 'absolute',
        top: 10,
        left: '10%',
        right: '10%',
        height: '100%',
        borderRadius: SIZES.radius,
        opacity: 0.15,
        transform: [{ scaleX: 0.95 }],
        shadowColor: COLORS.accent,
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.5,
        shadowRadius: 20,
        elevation: 5,
    },
    card: {
        backgroundColor: COLORS.surface,
        borderRadius: SIZES.radius,
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.05)',
        overflow: 'hidden',
    }
});

export default GlowCard;
