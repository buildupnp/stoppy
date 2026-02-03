
import React, { useState } from 'react';
import { View, TextInput, StyleSheet, Text } from 'react-native';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';

const Input = ({
    label,
    icon: Icon,
    error,
    placeholder,
    secureTextEntry,
    keyboardType = 'default',
    onChangeText,
    value
}) => {
    const [isFocused, setIsFocused] = useState(false);

    return (
        <View style={styles.container}>
            {label && <Text style={styles.label}>{label}</Text>}
            <View style={[
                styles.inputContainer,
                isFocused && styles.focused,
                error && styles.errorBorder
            ]}>
                {Icon && <Icon size={20} color={isFocused ? COLORS.accent : COLORS.textSecondary} style={styles.icon} />}
                <TextInput
                    style={styles.input}
                    placeholder={placeholder}
                    placeholderTextColor={COLORS.textSecondary}
                    secureTextEntry={secureTextEntry}
                    keyboardType={keyboardType}
                    value={value}
                    onChangeText={onChangeText}
                    onFocus={() => setIsFocused(true)}
                    onBlur={() => setIsFocused(false)}
                />
            </View>
            {error && <Text style={styles.errorText}>{error}</Text>}
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        marginBottom: SPACING.m,
    },
    label: {
        color: COLORS.textSecondary,
        fontSize: SIZES.small,
        marginBottom: 6,
        fontFamily: FONTS.medium,
    },
    inputContainer: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: COLORS.surface,
        borderRadius: SIZES.radius,
        borderWidth: 1,
        borderColor: 'rgba(255,255,255,0.05)',
        height: 56,
        paddingHorizontal: SPACING.m,
    },
    focused: {
        borderColor: COLORS.accent,
        // Add subtle glow or stronger border
    },
    errorBorder: {
        borderColor: COLORS.alert,
    },
    icon: {
        marginRight: 10,
    },
    input: {
        flex: 1,
        color: COLORS.textPrimary,
        fontSize: SIZES.font,
        fontFamily: FONTS.regular,
        height: '100%',
    },
    errorText: {
        color: COLORS.alert,
        fontSize: SIZES.small,
        marginTop: 4,
    }
});

export default Input;
