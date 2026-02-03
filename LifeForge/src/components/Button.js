
import React from 'react';
import { TouchableOpacity, Text, StyleSheet, View } from 'react-native';
import { COLORS, SIZES, FONTS, SHADOWS } from '../constants/theme';

const Button = ({
    title,
    onPress,
    variant = 'primary', // primary, secondary, outline, ghost
    style,
    textStyle,
    icon: Icon,
    disabled
}) => {

    const getBackgroundColor = () => {
        if (disabled) return COLORS.surface;
        switch (variant) {
            case 'primary': return COLORS.accent;
            case 'secondary': return COLORS.surface;
            case 'outline': return 'transparent';
            case 'ghost': return 'transparent';
            default: return COLORS.accent;
        }
    };

    const getTextColor = () => {
        if (disabled) return COLORS.textSecondary;
        switch (variant) {
            case 'primary': return COLORS.textPrimary; // or white
            case 'secondary': return COLORS.textPrimary;
            case 'outline': return COLORS.textPrimary; // or accent
            case 'ghost': return COLORS.textSecondary;
            default: return COLORS.textPrimary;
        }
    };

    const getBorder = () => {
        if (variant === 'outline') {
            return { borderWidth: 1, borderColor: COLORS.textSecondary };
        }
        return {};
    };

    const getShadow = () => {
        if (variant === 'primary' && !disabled) {
            return {
                shadowColor: COLORS.accent,
                shadowOffset: { width: 0, height: 4 },
                shadowOpacity: 0.3, // Glow effect
                shadowRadius: 8,
                elevation: 6,
            };
        }
        return {};
    };

    return (
        <TouchableOpacity
            onPress={onPress}
            disabled={disabled}
            activeOpacity={0.8}
            style={[
                styles.container,
                { backgroundColor: getBackgroundColor() },
                getBorder(),
                getShadow(),
                style
            ]}
        >
            {Icon && <View style={styles.iconContainer}><Icon size={20} color={getTextColor()} /></View>}
            <Text style={[
                styles.text,
                { color: getTextColor() },
                textStyle
            ]}>
                {title}
            </Text>
        </TouchableOpacity>
    );
};

const styles = StyleSheet.create({
    container: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        height: 56, // Tall tap targets
        borderRadius: SIZES.radius,
        paddingHorizontal: SIZES.padding,
        marginVertical: 8,
    },
    text: {
        fontFamily: FONTS.medium,
        fontSize: SIZES.medium,
        fontWeight: '600',
        letterSpacing: 0.5,
    },
    iconContainer: {
        marginRight: 8,
    }
});

export default Button;
