import 'react-native-url-polyfill/auto';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { createClient } from '@supabase/supabase-js';

// ============================================
// ⚠️ REPLACE THESE WITH YOUR SUPABASE CREDENTIALS
// ============================================
// Find these in: Supabase Dashboard → Settings → API
const SUPABASE_URL = 'https://oyuwirtdnjofmtlsebno.supabase.co'; // e.g., https://xxxxx.supabase.co
const SUPABASE_ANON_KEY = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6Im95dXdpcnRkbmpvZm10bHNlYm5vIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjY2MzEyNzQsImV4cCI6MjA4MjIwNzI3NH0.T58zpPE8WTQj_Gd-Of_UsJXFiLjbmP8IGjlWIBpR9aI'; // Public anon key

// Create Supabase client with AsyncStorage for session persistence
export const supabase = createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
    auth: {
        storage: AsyncStorage,
        autoRefreshToken: true,
        persistSession: true,
        detectSessionInUrl: false, // Important for React Native
    },
});

export default supabase;
