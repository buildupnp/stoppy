import React, { useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView, Alert, ActivityIndicator } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import ScreenWrapper from '../components/ScreenWrapper';
import GradientButton from '../components/GradientButton';
import Input from '../components/Input';
import { COLORS, SIZES, FONTS, SPACING } from '../constants/theme';
import { Mail, Lock, User, ArrowRight } from 'lucide-react-native';
import { useAuth } from '../context/AuthContext';

const AuthScreen = ({ navigation }) => {
    const { signUp, signIn } = useAuth();

    const [isLogin, setIsLogin] = useState(false);
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [name, setName] = useState('');
    const [loading, setLoading] = useState(false);

    const handleAuth = async () => {
        // Validation
        if (!email || !password) {
            Alert.alert('Error', 'Please fill in all fields');
            return;
        }
        if (!isLogin && !name) {
            Alert.alert('Error', 'Please enter your name');
            return;
        }

        setLoading(true);

        try {
            if (isLogin) {
                // Login
                const { data, error } = await signIn(email, password);
                if (error) {
                    Alert.alert('Login Failed', error.message);
                } else {
                    navigation.replace('MainTabs');
                }
            } else {
                // Sign Up
                const { data, error } = await signUp(email, password, name);
                if (error) {
                    Alert.alert('Sign Up Failed', error.message);
                } else {
                    // Supabase sends confirmation email by default
                    Alert.alert(
                        'Account Created!',
                        'Please check your email to confirm your account, then log in.',
                        [{ text: 'OK', onPress: () => setIsLogin(true) }]
                    );
                }
            }
        } catch (error) {
            Alert.alert('Error', error.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <ScreenWrapper>
            {/* Background Ambience */}
            <View style={styles.ambienceContainer}>
                <View style={styles.glowSpot} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.header}>
                    <Text style={styles.superTitle}>LifeForge</Text>
                    <Text style={styles.title}>{isLogin ? 'Welcome Back.' : 'Start Your Journey.'}</Text>
                    <Text style={styles.subtitle}>
                        {isLogin
                            ? 'Enter your credentials to access your forge.'
                            : 'Create an account to turn movement into time.'}
                    </Text>
                </View>

                <View style={styles.form}>
                    {!isLogin && (
                        <View style={styles.inputGroup}>
                            <Input
                                label="Full Name"
                                placeholder="John Doe"
                                icon={User}
                                value={name}
                                onChangeText={setName}
                            />
                        </View>
                    )}

                    <View style={styles.inputGroup}>
                        <Input
                            label="Email Address"
                            placeholder="you@example.com"
                            icon={Mail}
                            keyboardType="email-address"
                            autoCapitalize="none"
                            value={email}
                            onChangeText={setEmail}
                        />
                    </View>

                    <View style={styles.inputGroup}>
                        <Input
                            label="Password"
                            placeholder="••••••••"
                            icon={Lock}
                            secureTextEntry
                            value={password}
                            onChangeText={setPassword}
                        />
                    </View>

                    {isLogin && (
                        <TouchableOpacity style={styles.forgotPass}>
                            <Text style={styles.forgotLink}>Recover Password</Text>
                        </TouchableOpacity>
                    )}

                    <GradientButton
                        title={loading ? '' : (isLogin ? "Enter Forge" : "Create Account")}
                        icon={loading ? null : ArrowRight}
                        onPress={handleAuth}
                        disabled={loading}
                    >
                        {loading && <ActivityIndicator color={COLORS.white} />}
                    </GradientButton>

                    <View style={styles.footer}>
                        <Text style={styles.footerText}>
                            {isLogin ? "New to LifeForge? " : "Already have an account? "}
                        </Text>
                        <TouchableOpacity onPress={() => setIsLogin(!isLogin)} style={styles.switchTouch}>
                            <Text style={styles.footerLink}>
                                {isLogin ? "Create Account" : "Login"}
                            </Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </ScrollView>
        </ScreenWrapper>
    );
};

const styles = StyleSheet.create({
    ambienceContainer: {
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        height: 400,
        overflow: 'hidden',
        zIndex: -1,
    },
    glowSpot: {
        position: 'absolute',
        top: -100,
        right: -100,
        width: 300,
        height: 300,
        borderRadius: 150,
        backgroundColor: COLORS.accent,
        opacity: 0.08,
        transform: [{ scale: 1.5 }],
        shadowColor: COLORS.accent,
        shadowRadius: 50,
        elevation: 0,
    },
    scrollContent: {
        flexGrow: 1,
        padding: SIZES.padding,
        justifyContent: 'center',
        paddingBottom: SPACING.xxl,
    },
    header: {
        marginBottom: SPACING.xl,
    },
    superTitle: {
        fontSize: SIZES.small,
        color: COLORS.accent,
        fontFamily: FONTS.medium,
        textTransform: 'uppercase',
        letterSpacing: 1.5,
        marginBottom: SPACING.xs,
    },
    title: {
        fontSize: 42,
        color: COLORS.textPrimary,
        fontFamily: FONTS.bold,
        fontWeight: '800',
        marginBottom: SPACING.s,
        lineHeight: 48,
    },
    subtitle: {
        fontSize: SIZES.medium,
        color: COLORS.textSecondary,
        fontFamily: FONTS.regular,
        lineHeight: 24,
        maxWidth: '90%',
    },
    form: {
        width: '100%',
    },
    inputGroup: {
        marginBottom: SPACING.m,
    },
    forgotPass: {
        alignSelf: 'flex-end',
        marginBottom: SPACING.l,
        marginTop: -SPACING.s,
    },
    forgotLink: {
        color: COLORS.textSecondary,
        fontSize: SIZES.small,
        fontFamily: FONTS.medium,
    },
    footer: {
        flexDirection: 'row',
        justifyContent: 'center',
        marginTop: SPACING.xl,
        alignItems: 'center',
    },
    footerText: {
        color: COLORS.textSecondary,
        fontSize: SIZES.font,
    },
    switchTouch: {
        padding: 4,
    },
    footerLink: {
        color: COLORS.accent,
        fontWeight: '700',
        fontSize: SIZES.font,
    }
});

export default AuthScreen;
