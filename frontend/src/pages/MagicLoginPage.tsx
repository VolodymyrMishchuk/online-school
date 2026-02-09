import { useEffect, useState, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { magicLogin } from '../api/auth';

export default function MagicLoginPage() {
    const [searchParams] = useSearchParams();
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const dataFetchedRef = useRef(false);

    useEffect(() => {
        if (dataFetchedRef.current) return;
        dataFetchedRef.current = true;

        const token = searchParams.get('token');
        const redirectUrl = searchParams.get('redirect') || '/dashboard/my-courses';

        if (!token) {
            setError('No token provided');
            return;
        }

        const doLogin = async () => {
            try {
                const response = await magicLogin({ token });
                const { accessToken, userId, role, firstName, lastName } = response;
                localStorage.clear(); // Clear previous session
                localStorage.setItem('token', accessToken);
                localStorage.setItem('userId', userId);
                localStorage.setItem('userRole', role);
                localStorage.setItem('user', JSON.stringify({ userId, role, firstName, lastName }));
                navigate(redirectUrl);
            } catch (err: any) {
                console.error(err);
                const status = err.response?.status;
                const message = err.response?.data?.message || err.message;
                setError(`Login Failed (${status}): ${message || 'Invalid or expired Magic Link'}. Please check backend logs.`);
                // setTimeout(() => navigate('/login'), 5000); // Keep it longer to read error
            }
        };

        doLogin();
    }, [searchParams, navigate]);

    if (error) {
        return (
            <div className="flex h-screen items-center justify-center bg-gray-50">
                <div className="text-center p-8 bg-white rounded-xl shadow-lg">
                    <h2 className="text-2xl font-bold text-red-600 mb-2">Login Failed</h2>
                    <p className="text-gray-600 mb-4">{error}</p>
                    <p className="text-sm text-gray-400">Redirecting to login page...</p>
                </div>
            </div>
        );
    }

    return (
        <div className="flex h-screen items-center justify-center bg-gray-50">
            <div className="text-center">
                <h2 className="text-2xl font-bold text-gray-800 mb-4">Logging you in...</h2>
                <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-brand-primary mx-auto"></div>
            </div>
        </div>
    );
}
