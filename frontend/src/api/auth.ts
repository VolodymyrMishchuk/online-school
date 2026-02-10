import { client } from './client';

export interface AuthResponse {
    accessToken: string;
    userId: string;
    role: string;
    firstName: string;
    lastName: string;
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

export interface MagicLoginRequest {
    token: string;
}

export const magicLogin = async (request: MagicLoginRequest): Promise<AuthResponse> => {
    const response = await client.post<AuthResponse>('/auth/magic-login', request, {
        withCredentials: true
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
    localStorage.removeItem('userId');
    localStorage.removeItem('userRole');
};

export interface ForgotPasswordRequest {
    email: string;
}

export const forgotPassword = async (request: ForgotPasswordRequest): Promise<void> => {
    await client.post('/auth/forgot-password', request);
};

export interface ResetPasswordRequest {
    token: string;
    newPassword: string;
}


export const resetPassword = async (request: ResetPasswordRequest): Promise<void> => {
    await client.post('/auth/reset-password', request);
};

export interface ChangePasswordRequest {
    oldPassword: string;
    newPassword: string;
}

export const changePassword = async (request: ChangePasswordRequest): Promise<void> => {
    await client.post('/auth/change-password', request);
};
