
export const COLORS = {
    // Primary Backgrounds
    primary: '#000000',
    primaryGradientStart: '#020410', // Darker Blue-Black
    primaryGradientEnd: '#0F172A', // Slate 900

    // Surfaces - Glass
    surface: 'rgba(30, 41, 59, 0.4)',
    surfaceHighlight: 'rgba(51, 65, 85, 0.5)',

    // Accents - True Blue (Not Cyan)
    accent: '#3B82F6', // Blue 500
    accentDark: '#2563EB', // Blue 600
    accentGlow: '#60A5FA', // Blue 400 (Bright Neon)
    borderGlow: 'rgba(59, 130, 246, 0.5)', // Blue Border

    alert: '#EF4444',
    success: '#10B981',

    // Text
    textPrimary: '#FFFFFF',
    textSecondary: '#94A3B8',

    // Utilities
    white: '#FFFFFF',
    black: '#000000',
    transparent: 'transparent',
    overlay: 'rgba(0, 0, 0, 0.8)',
};

export const SPACING = {
    xs: 4,
    s: 8,
    m: 16,
    l: 24,
    xl: 32,
    xxl: 48,
    xxxl: 64,
};

export const FONTS = {
    regular: 'System',
    medium: 'System-Medium',
    bold: 'System-Bold',
};

export const SIZES = {
    base: 8,
    small: 13,
    font: 16,
    medium: 18,
    large: 24,
    extraLarge: 32,
    xxl: 40,
    radius: 20,
    padding: 24,
};

export const SHADOWS = {
    glass: {
        shadowColor: COLORS.black,
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.3,
        shadowRadius: 20,
        elevation: 5,
    },
    neon: {
        shadowColor: COLORS.accent,
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.8,
        shadowRadius: 10,
        elevation: 10,
    },
};

const appTheme = { COLORS, SPACING, FONTS, SIZES, SHADOWS };

export default appTheme;
