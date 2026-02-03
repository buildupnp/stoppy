
import React from 'react';
import { View, Text, StyleSheet, Image, Dimensions } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import ScreenWrapper from '../components/ScreenWrapper';
import GradientButton from '../components/GradientButton';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import { ArrowRight, Zap } from 'lucide-react-native';

const { width } = Dimensions.get('window');

const OnboardingScreen = ({ navigation }) => {
    return (
        <ScreenWrapper>
            <LinearGradient
                colors={[COLORS.primary, '#18181b', COLORS.primary]}
                style={StyleSheet.absoluteFillObject}
            />

            <View style={styles.container}>
                {/* Hero Section */}
                <View style={styles.heroSection}>
                    <View style={styles.glowContainer}>
                        <View style={styles.glow} />
                        <Zap size={80} color={COLORS.accent} />
                    </View>
                </View>

                <View style={styles.textSection}>
                    <Text style={styles.tagline}>LIFEFORGE</Text>
                    <Text style={styles.title}>
                        Master Your Time.{'\n'}Earn Your Access.
                    </Text>
                    <Text style={styles.subtitle}>
                        LifeForge turns your physical effort into digital freedom. Walk or workout to unlock your favorite apps.
                    </Text>
                </View>

                <View style={styles.actionSection}>
                    <GradientButton
                        title="Start Forging"
                        icon={ArrowRight}
                        onPress={() => navigation.navigate('Auth')}
                    />
                </View>
            </View>
        </ScreenWrapper>
    );
};

const styles = StyleSheet.create({
    container: {
        flex: 1,
        padding: SIZES.padding,
        justifyContent: 'space-between',
    },
    heroSection: {
        flex: 0.5,
        justifyContent: 'center',
        alignItems: 'center',
    },
    glowContainer: {
        width: 160,
        height: 160,
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
    },
    glow: {
        position: 'absolute',
        width: 200,
        height: 200,
        borderRadius: 100,
        backgroundColor: COLORS.accent,
        opacity: 0.2,
        shadowColor: COLORS.accent,
        shadowRadius: 50,
        elevation: 20,
    },
    textSection: {
        flex: 0.35,
        justifyContent: 'center',
    },
    tagline: {
        color: COLORS.accent,
        fontSize: SIZES.small,
        fontFamily: FONTS.bold,
        letterSpacing: 2,
        marginBottom: SPACING.s,
        textTransform: 'uppercase',
    },
    title: {
        fontSize: 40,
        color: COLORS.textPrimary,
        fontFamily: FONTS.bold,
        fontWeight: '800', // Extra bold
        marginBottom: SPACING.m,
        lineHeight: 46,
    },
    subtitle: {
        fontSize: SIZES.medium,
        color: COLORS.textSecondary,
        fontFamily: FONTS.regular,
        lineHeight: 26,
    },
    actionSection: {
        flex: 0.15,
        justifyContent: 'flex-end',
        marginBottom: SIZES.padding,
    }
});

export default OnboardingScreen;
