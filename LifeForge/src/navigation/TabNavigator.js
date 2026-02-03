
import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import DashboardScreen from '../screens/DashboardScreen';
import ForgeScreen from '../screens/ForgeScreen';
import GuardianScreen from '../screens/GuardianScreen';
import SettingsScreen from '../screens/SettingsScreen';
import GlassTabBar from '../components/GlassTabBar';

const Tab = createBottomTabNavigator();

const TabNavigator = () => {
    return (
        <Tab.Navigator
            tabBar={props => <GlassTabBar {...props} />}
            screenOptions={{
                headerShown: false,
                // Ensure content is visible behind the floating tab bar
                tabBarHideOnKeyboard: true,
            }}
            sceneContainerStyle={{
                backgroundColor: 'transparent' // Let background show through if needed
            }}
        >
            <Tab.Screen name="Home" component={DashboardScreen} />
            <Tab.Screen name="Forge" component={ForgeScreen} />
            <Tab.Screen name="Guardian" component={GuardianScreen} />
            <Tab.Screen name="Settings" component={SettingsScreen} />
        </Tab.Navigator>
    );
};

export default TabNavigator;
