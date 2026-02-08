import { client } from './client';

export interface AuthResponse {
    accessToken: string;
    userId: string;
    role: string;
}

export interface LoginRequest {
    email: string;
    password: string;
}

export const login = async (request: LoginRequest): Promise<AuthResponse> => {
    const response = await client.post<AuthResponse>('/auth/login', request, {
        withCredentials: true // Enable sending/receiving cookies
    });
    return response.data;
};

export interface RegisterRequest {
    firstName: string;
    lastName: string;
    email: string;
    phoneNumber: string;
    bornedAt: string; // ISO string
    password: string;
}

export const register = async (request: RegisterRequest): Promise<AuthResponse> => {
    const response = await client.post<AuthResponse>('/auth/register', request, {
        withCredentials: true // Enable sending/receiving cookies
    });
    return response.data;
};

export const refreshAccessToken = async (): Promise<AuthResponse> => {
    const response = await client.post<AuthResponse>('/auth/refresh', {}, {
        withCredentials: true // Send refresh token cookie
    });
    return response.data;
};

export const logout = async (): Promise<void> => {
    await client.post('/auth/logout', {}, {
        withCredentials: true
    });
    // Clear local storage
    localStorage.removeItem('token');
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
};
