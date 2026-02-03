
import React from 'react';
import { TouchableOpacity, Text, StyleSheet, View } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { COLORS, SIZES, FONTS } from '../constants/theme';

const GradientButton = ({
    title,
    onPress,
    icon: Icon,
    disabled
}) => {
    if (disabled) {
        return (
            <View style={[styles.container, styles.disabled]}>
                <Text style={[styles.text, { color: COLORS.textSecondary }]}>{title}</Text>
            </View>
        );
    }

    return (
        <TouchableOpacity
            onPress={onPress}
            activeOpacity={0.8}
            style={styles.touchable}
        >
            <View style={styles.shadowContainer} />
            <LinearGradient
                colors={[COLORS.accent, COLORS.accentDark]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.container}
            >
                {Icon && <Icon size={20} color={COLORS.white} style={styles.icon} />}
                <Text style={styles.text}>{title}</Text>
            </LinearGradient>
        </TouchableOpacity>
    );
};

const styles = StyleSheet.create({
    touchable: {
        marginVertical: 12,
        alignItems: 'center',
    },
    shadowContainer: {
        ...StyleSheet.absoluteFillObject,
        backgroundColor: COLORS.accent,
        borderRadius: SIZES.radius,
        opacity: 0.4,
        transform: [{ translateY: 4 }],
        // Blur simulation using simple positioning, real blur requires heavier lib or images
        shadowColor: COLORS.accent,
        shadowOffset: { width: 0, height: 8 },
        shadowOpacity: 0.6,
        shadowRadius: 16,
        elevation: 8,
    },
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        height: 56,
        width: '100%',
        borderRadius: SIZES.radius,
        paddingHorizontal: SIZES.padding,
    },
    disabled: {
        backgroundColor: COLORS.surfaceHighlight,
        height: 56,
        borderRadius: SIZES.radius,
        justifyContent: 'center',
        alignItems: 'center',
        width: '100%',
    },
    text: {
        fontFamily: FONTS.medium,
        fontSize: SIZES.medium,
        fontWeight: '700',
        color: COLORS.white,
        letterSpacing: 0.5,
    },
    icon: {
        marginRight: 8,
    }
});

export default GradientButton;
