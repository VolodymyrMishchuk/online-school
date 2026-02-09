import axios from 'axios';

export const API_URL = 'http://localhost:8080'; // Should be in env

const apiClient = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true, // Enable sending cookies
});

// Request interceptor - add access token
apiClient.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Response interceptor - handle 401 and auto-refresh
let isRefreshing = false;
let failedQueue: any[] = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach((prom) => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // If error is 401 and we haven't tried to refresh yet
        if (error.response?.status === 401 && !originalRequest._retry) {
            if (isRefreshing) {
                // If already refreshing, queue this request
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        return apiClient(originalRequest);
                    })
                    .catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            try {
                // Try to refresh the token
                const response = await apiClient.post('/auth/refresh', {}, {
                    withCredentials: true,
                });

                const { accessToken, userId, role, firstName, lastName } = response.data;

                // Save new tokens
                localStorage.setItem('token', accessToken);
                localStorage.setItem('userId', userId);
                localStorage.setItem('userRole', role);
                // Sync user object
                const currentUser = localStorage.getItem('user');
                const userEmail = currentUser ? JSON.parse(currentUser).email : '';
                localStorage.setItem('user', JSON.stringify({ userId, role, firstName, lastName, email: userEmail }));

                // Update authorization header
                apiClient.defaults.headers.common.Authorization = `Bearer ${accessToken}`;
                originalRequest.headers.Authorization = `Bearer ${accessToken}`;

                processQueue(null, accessToken);

                // Retry original request
                return apiClient(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);

                // Refresh failed - clear storage and redirect to login
                localStorage.removeItem('token');
                localStorage.removeItem('userId');
                localStorage.removeItem('userRole');

                // Redirect to login page
                if (window.location.pathname !== '/login' && window.location.pathname !== '/register') {
                    window.location.pathname = '/login';
                }

                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

export { apiClient as client };
export default apiClient;
