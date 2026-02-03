
import React from 'react';
import { View, TouchableOpacity, StyleSheet, Platform, Dimensions } from 'react-native';
import { BlurView } from 'expo-blur';
import { LinearGradient } from 'expo-linear-gradient';
import { COLORS, SIZES, SHADOWS } from '../constants/theme';
import { Home, Dumbbell, Shield, Settings } from 'lucide-react-native';

const { width } = Dimensions.get('window');

const GlassTabBar = ({ state, descriptors, navigation }) => {
    return (
        <View style={styles.container}>
            {/* Glow Layer */}
            <View style={styles.glowLayer} />

            {/* Gradient Border for Glass Effect */}
            <LinearGradient
                colors={[
                    COLORS.borderGlow,
                    'rgba(59, 130, 246, 0.1)',
                    COLORS.borderGlow // Loop back to glow
                ]}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.borderGradient}
            >
                <BlurView intensity={Platform.OS === 'ios' ? 30 : 50} tint="dark" style={styles.glassContent}>
                    <View style={styles.tabBar}>
                        {state.routes.map((route, index) => {
                            const { options } = descriptors[route.key];
                            const isFocused = state.index === index;

                            const onPress = () => {
                                const event = navigation.emit({
                                    type: 'tabPress',
                                    target: route.key,
                                    canPreventDefault: true,
                                });

                                if (!isFocused && !event.defaultPrevented) {
                                    navigation.navigate(route.name);
                                }
                            };

                            let IconComponent;
                            let label;

                            // Match routes to icons
                            switch (route.name) {
                                case 'Home':
                                    IconComponent = Home;
                                    label = 'Home';
                                    break;
                                case 'Forge':
                                    IconComponent = Dumbbell;
                                    label = 'Forge';
                                    break;
                                case 'Guardian':
                                    IconComponent = Shield;
                                    label = 'Guardian';
                                    break;
                                case 'Settings':
                                    IconComponent = Settings;
                                    label = 'Settings';
                                    break;
                                default:
                                    IconComponent = Home;
                                    label = 'Home';
                            }

                            return (
                                <TouchableOpacity
                                    key={index}
                                    onPress={onPress}
                                    style={styles.tabItem}
                                    activeOpacity={0.7}
                                >
                                    <View style={[
                                        styles.iconContainer,
                                        isFocused && styles.activeIconContainer
                                    ]}>
                                        <IconComponent
                                            size={24}
                                            color={isFocused ? COLORS.white : COLORS.textSecondary}
                                        />
                                        {isFocused && <View style={styles.activeDot} />}
                                    </View>
                                </TouchableOpacity>
                            );
                        })}
                    </View>
                </BlurView>
            </LinearGradient>
        </View>
    );
};

const styles = StyleSheet.create({
    container: {
        position: 'absolute',
        bottom: 24, // Floating
        left: 16,
        right: 16,
        borderRadius: 32,
        height: 72,
        shadowColor: COLORS.black,
        shadowOffset: { width: 0, height: 10 },
        shadowOpacity: 0.5,
        shadowRadius: 20,
        elevation: 10,
    },
    glowLayer: {
        position: 'absolute',
        bottom: 0,
        left: 20,
        right: 20,
        height: 60,
        borderRadius: 32,
        backgroundColor: COLORS.accent,
        opacity: 0.15,
        transform: [{ scaleX: 0.9 }],

    },
    borderGradient: {
        borderRadius: 32,
        padding: 1, // Border thickness
        flex: 1,
    },
    glassContent: {
        flex: 1,
        borderRadius: 31,
        overflow: 'hidden',
        backgroundColor: 'rgba(15, 23, 42, 0.65)',
    },
    tabBar: {
        flexDirection: 'row',
        justifyContent: 'space-between', // Spread evenly
        alignItems: 'center',
        flex: 1,
        paddingHorizontal: 16,
    },
    tabItem: {
        flex: 1,
        alignItems: 'center',
        justifyContent: 'center',
        height: '100%',
    },
    iconContainer: {
        alignItems: 'center',
        justifyContent: 'center',
        width: 48,
        height: 48,
        borderRadius: 24,
    },
    activeIconContainer: {
        backgroundColor: 'rgba(59, 130, 246, 0.2)', // Subtle highlight
        shadowColor: COLORS.accent,
        shadowOffset: { width: 0, height: 0 },
        shadowOpacity: 0.5,
        shadowRadius: 10,
    },
    activeDot: {
        position: 'absolute',
        bottom: 8,
        width: 4,
        height: 4,
        borderRadius: 2,
        backgroundColor: COLORS.accent,
    }
});

export default GlassTabBar;
